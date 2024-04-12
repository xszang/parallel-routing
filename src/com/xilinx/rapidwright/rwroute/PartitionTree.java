package com.xilinx.rapidwright.rwroute;

import java.util.List;
import java.util.ArrayList;

import com.xilinx.rapidwright.rwroute.Connection;
import com.xilinx.rapidwright.rwroute.PartitionAxis;
import com.xilinx.rapidwright.rwroute.PartitionBBox;
import com.xilinx.rapidwright.rwroute.PartitionTreeNode;

public class PartitionTree {
	private PartitionTreeNode root;
	private PartitionBBox bbox;
	public PartitionTreeNode root() {return root;}

	public PartitionTree(List<Connection> connections, int xMax, int yMax) {
		bbox = new PartitionBBox(0, xMax, 0, yMax);
		root = new PartitionTreeNode();
		root.bbox = bbox;
		root.connections = connections;
		build(root);
	}

	private void build(PartitionTreeNode cur) {
		// sort the connections for routing
		cur.sortConnections();

		// find the best cutline
		int W = cur.bbox.xMax - cur.bbox.xMin + 1;
		int H = cur.bbox.yMax - cur.bbox.yMin + 1;

		int[] xTotalBefore = new int[W - 1];
		int[] xTotalAfter = new int[W - 1];
		int[] yTotalBefore = new int[H - 1];
		int[] yTotalAfter = new int[H - 1];

		for (Connection connection: cur.connections) {
			int xStart = Math.max(cur.bbox.xMin, clampX(connection.getXMinBB())) - cur.bbox.xMin;
			int xEnd   = Math.min(cur.bbox.xMax, clampX(connection.getXMaxBB())) - cur.bbox.xMin;
			assert(xStart >= 0);
			for (int x = xStart; x < W - 1; x++) {
				xTotalBefore[x] ++;
			}
			for (int x = 0; x < xEnd; x++) {
				xTotalAfter[x] ++;
			}

			int yStart = Math.max(cur.bbox.yMin, clampY(connection.getYMinBB())) - cur.bbox.yMin;
			int yEnd   = Math.min(cur.bbox.yMax, clampY(connection.getYMaxBB())) - cur.bbox.yMin;
			assert(yStart >= 0);
			for (int y = yStart; y < H - 1; y++) {
				yTotalBefore[y] ++;
			}
			for (int y = 0; y < yEnd; y++) {
				yTotalAfter[y] ++;
			}
		}

		double bestScore = Double.MAX_VALUE;
		double bestPos = Double.NaN;
		PartitionAxis bestAxis = PartitionAxis.X;

		int maxXBefore = xTotalBefore[W - 2];
		int maxXAfter = xTotalAfter[0];
		for (int x = 0; x < W - 1; x++) {
			int before = xTotalBefore[x];
			int after = xTotalAfter[x];
			if (before == maxXBefore || after == maxXAfter)
				continue;
			double score = (double)Math.abs(xTotalBefore[x] - xTotalAfter[x]) / Math.max(xTotalBefore[x], xTotalAfter[x]);
			if (score < bestScore) {
				bestScore = score;
				bestPos = cur.bbox.xMin + x + 0.5;
				bestAxis = PartitionAxis.X;
			}
		}

		int maxYBefore = yTotalBefore[H - 2];
		int maxYAfter = yTotalAfter[0];
		for (int y = 0; y < H - 1; y++) {
			int before = yTotalBefore[y];
			int after = yTotalAfter[y];
			if (before == maxYBefore || after == maxYAfter)
				continue;
			double score = (double)Math.abs(yTotalBefore[y] - yTotalAfter[y]) / Math.max(yTotalBefore[y], yTotalAfter[y]);
			if (score < bestScore) {
				bestScore = score;
				bestPos = cur.bbox.yMin + y + 0.5;
				bestAxis = PartitionAxis.Y;
			}
		}

		if (Double.isNaN(bestPos))
			return;
		
		// recursively build tree
		cur.left = new PartitionTreeNode();
		cur.left.connections = new ArrayList<>();
		cur.middle = new PartitionTreeNode();
		cur.middle.connections = new ArrayList<>();
		cur.right = new PartitionTreeNode();
		cur.right.connections = new ArrayList<>();

		if (bestAxis == PartitionAxis.X) {
			for (Connection connection: cur.connections) {
				if (clampX(connection.getXMaxBB()) < bestPos) {
					cur.left.connections.add(connection);
				}
				else if (clampX(connection.getXMinBB()) > bestPos) {
					cur.right.connections.add(connection);
				}
				else {
					cur.middle.connections.add(connection);
				}
			}
			cur.left.bbox = new PartitionBBox(cur.bbox.xMin, (int)Math.floor(bestPos), cur.bbox.yMin, cur.bbox.yMax);
			cur.right.bbox = new PartitionBBox((int)Math.floor(bestPos) + 1, cur.bbox.xMax, cur.bbox.yMin, cur.bbox.yMax);
			cur.middle.bbox = cur.bbox;
		} else {
			assert(bestAxis == PartitionAxis.Y);
			for (Connection connection: cur.connections) {
				if (clampY(connection.getYMaxBB()) < bestPos) {
					cur.left.connections.add(connection);
				}
				else if (clampY(connection.getYMinBB()) > bestPos) {
					cur.right.connections.add(connection);
				}
				else {
					cur.middle.connections.add(connection);
				}
			}
			cur.left.bbox = new PartitionBBox(cur.bbox.xMin, cur.bbox.xMax, cur.bbox.yMin, (int)Math.floor(bestPos));
			cur.right.bbox = new PartitionBBox(cur.bbox.xMin, cur.bbox.xMax, (int)Math.floor(bestPos) + 1, cur.bbox.yMax);
			cur.middle.bbox = cur.bbox;
		}
		assert(cur.left.connections.size() > 0 && cur.right.connections.size() > 0);
		build(cur.left);
		build(cur.right);
		if (cur.middle.connections.size() > 0)
			build(cur.middle);
		else
			cur.middle = null;
	}

	private int clampX(int x) { 
		return Math.min(Math.max(x, bbox.xMin), bbox.xMax); 
	}

	private int clampY(int y) { 
		return Math.min(Math.max(y, bbox.yMin), bbox.yMax); 
	}
}

