package com.xilinx.rapidwright.rwroute;

public class PartitionBBox {
	public int xMin;
	public int xMax;
	public int yMin;
	public int yMax;

	public PartitionBBox(int xMin_, int xMax_, int yMin_, int yMax_) {
		xMin = xMin_;
		xMax = xMax_;
		yMin = yMin_;
		yMax = yMax_;
	}

	@Override
    public String toString() {
		return "bbox: [( " + xMin + ", " + yMin + " ), -> ( " + xMax + ", " + yMax + " )]";
	}
}
