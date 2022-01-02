package com.tgb.gfx;

import java.awt.Color;

public class FloatingInfo {
	Color internalColor = Color.BLACK;
	public String text = "";
	
	public void setColor(int r, int g, int b)
	{
		internalColor = new Color(r, g, b);
	}
}
