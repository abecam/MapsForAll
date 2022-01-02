package com.tgb.model;

public class MarkablePoint
{
	public double latitude;
	public double longitude;
	public int nbOfTimePassed = 0;
	public int directionOfPass = 1; // 1 or -1
	public MarkableWay belongingTo; // If stacked or stored, we need to find to which way they belong
	public int atPos; // and found where on that way?
	
	public long forNodeId = -1; // OSM node ID
	public int nbIntersect = 0; // If other nodes share the same ID !

	/**
	 * Unique for the Node!
	 */
	@Override
	public int hashCode() {
		return new Long(forNodeId).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		try
		{
			MarkablePoint theOtherPoint = ((MarkablePoint )obj);
			return (forNodeId == theOtherPoint.forNodeId) && (belongingTo.ID == theOtherPoint.belongingTo.ID);
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}
}
