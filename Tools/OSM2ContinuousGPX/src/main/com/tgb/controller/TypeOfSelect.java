package com.tgb.controller;

public enum TypeOfSelect {
	
	
	POINT_SELECT ("Point Selection"), // Only the points below the circle
	POINT_UNSELECT ("Point removal"),
	WAY_SELECT ("Full way selection"), // The full way of the points selected
	WAY_UNSELECT ("Full way removal"),
	SEGMENT_SELECT("Segment selection"), // Until the first intersection after the selection
	SEGMENT_UNSELECT("Segment removal");
	
	private String name;
	
	private TypeOfSelect(String nameOfType)
	{
		name = nameOfType;
	}
}
