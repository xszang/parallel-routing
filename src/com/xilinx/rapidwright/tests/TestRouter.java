package com.xilinx.rapidwright.tests;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.PriorityQueue;

import com.xilinx.rapidwright.rwroute.PartialRouter;
import com.xilinx.rapidwright.rwroute.RWRoute;
import com.xilinx.rapidwright.rwroute.RWRouteConfig;
import com.xilinx.rapidwright.util.RuntimeTracker;
import com.xilinx.rapidwright.util.RuntimeTrackerTree;
import com.xilinx.rapidwright.design.Design;
import com.xilinx.rapidwright.design.Net;
import com.xilinx.rapidwright.design.NetType;
import com.xilinx.rapidwright.design.SitePinInst;
import com.xilinx.rapidwright.device.Node;
import com.xilinx.rapidwright.router.RouteThruHelper;
import com.xilinx.rapidwright.rwroute.Connection;
import com.xilinx.rapidwright.rwroute.RouteNode;
import com.xilinx.rapidwright.rwroute.RouterHelper;

public class TestRouter extends PartialRouter {
    public TestRouter(Design design, RWRouteConfig config, Collection<SitePinInst> pinsToRoute, boolean softPreserve) {
        super(design, config, pinsToRoute, softPreserve);
    }
    public static ArrayList<Connection> routeDesignWithUserDefinedArguments_(Design design,
    String[] args,
    Collection<SitePinInst> pinsToRoute,
    boolean softPreserve) {
        RWRouteConfig config = new RWRouteConfig(args);
        if (pinsToRoute == null) {
            preprocess(design);
            pinsToRoute = getUnroutedPins(design);
        }
        if (config.isMaskNodesCrossRCLK()) {
            System.out.println("WARNING: Masking nodes across RCLK for partial routing could result in routability problems.");
        }
        return routeDesign_(design, new TestRouter(design, config, pinsToRoute, softPreserve));
    }

    protected static ArrayList<Connection> routeDesign_(Design design, TestRouter router) {
        router.preprocess();
        return router.initialize_();
    }

    protected ArrayList<Connection> initialize_() {
        routerTimer = new RuntimeTrackerTree("Route design", config.isVerbose());
        rnodesTimer = routerTimer.createStandAloneRuntimeTracker("rnodes creation");
        RuntimeTracker updateTimingTimer = routerTimer.createStandAloneRuntimeTracker("update timing");
        RuntimeTracker updateCongestionCosts = routerTimer.createStandAloneRuntimeTracker("update congestion costs");
        routerTimer.createRuntimeTracker("Initialization", routerTimer.getRootRuntimeTracker()).start();

        minRerouteCriticality = config.getMinRerouteCriticality();
        ArrayList<Connection> criticalConnections = new ArrayList<>();

        PriorityQueue<RouteNode> queue = new PriorityQueue<>();
        routingGraph = createRouteNodeGraph();
        if (config.isTimingDriven()) {
            Map<Node, Float> nodesDelays = new HashMap<>();
        }
        rnodesCreatedThisIteration = 0;
        routethruHelper = new RouteThruHelper(design.getDevice());
        presentCongestionFactor = config.getInitialPresentCongestionFactor();
        lutPinSwapping = config.isLutPinSwapping();
        lutRoutethru = config.isLutRoutethru();

        routerTimer.createRuntimeTracker("determine route targets", "Initialization").start();
        System.out.println("Shape of Tiles: " + design.getDevice().getColumns() + "x" + + design.getDevice().getRows());
        return determineRoutingTargets_();
    }

    protected ArrayList<Connection> determineRoutingTargets_() {
        return categorizeNets_();
    }

    private ArrayList<Connection> categorizeNets_() {
        int numWireNetsToRoute = 0;
        int numConnectionsToRoute = 0;
        numPreservedRoutableNets = 0;
        numNotNeedingRoutingNets = 0;
        int numUnrecognizedNets = 0;

        nets = new IdentityHashMap<>();
        indirectConnections = new ArrayList<>();
        directConnections = new ArrayList<>();
        clkNets = new ArrayList<>();
        staticNetAndRoutingTargets = new HashMap<>();

        for (Net net : design.getNets()) {
            if (net.isClockNet()) {
                addGlobalClkRoutingTargets(net);

            } else if (net.isStaticNet()) {
                addStaticNetRoutingTargets(net);

            } else if (net.getType().equals(NetType.WIRE)) {
                if (RouterHelper.isDriverLessOrLoadLessNet(net) ||
                        RouterHelper.isInternallyRoutedNet(net) ||
                        net.getName().equals(Net.Z_NET)) {
                    preserveNet(net, true);
                    numNotNeedingRoutingNets++;
                } else if (RouterHelper.isRoutableNetWithSourceSinks(net)) {
                    addNetConnectionToRoutingTargets(net);
                } else {
                    numNotNeedingRoutingNets++;
                }
            } else {
                numUnrecognizedNets++;
                System.err.println("ERROR: Unknown net " + net);
            }
        }

        ArrayList<Connection> indirectConnections_ = new ArrayList<>();
        indirectConnections_.addAll(indirectConnections);

        return indirectConnections_;
    }
}
