package com.xilinx.rapidwright.rwroute;

import java.util.List;

import com.xilinx.rapidwright.rwroute.Connection;
import com.xilinx.rapidwright.rwroute.PartitionBBox;

public class PartitionTreeNode {
	/* Connections contained in all subtrees */
	public List<Connection> connections;
	/* Left subtree */
	public PartitionTreeNode left;
	/* Middle subtree */
	public PartitionTreeNode middle;
	/* Right subtree */
	public PartitionTreeNode right;
	/* Bounding box of this node. */
	public PartitionBBox bbox;

	public PartitionTreeNode() {
		connections = null;
		left = null;
		right = null;
		bbox = null;
	}
	public void sortConnections() {
        connections.sort((connection1, connection2) -> {
            int comp = connection2.getNetWrapper().getConnections().size() - connection1.getNetWrapper().getConnections().size();
            if (comp == 0) {
                return Short.compare(connection1.getHpwl(), connection2.getHpwl());
            } else {
                return comp;
            }
        });
	}
}
