package com.tgb.gfx;

import java.awt.Color;

public class CoordInfo {
	public double longitude;
	public double latitude;
	public Color internalColor;

	public CoordInfo() {
	}
	
	public void setColor(int r, int g, int b)
	{
		internalColor = new Color(r, g, b);
	}
}