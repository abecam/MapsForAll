package com.tgb.model;

import java.util.ArrayList;

public class NodeWithPoints {
	public long nodeID = -1;
	public ArrayList<Integer> forMarkablePoint = new ArrayList<>();
	
	@Override
	public int hashCode() {
		return new Long(nodeID).hashCode();
	}
}
