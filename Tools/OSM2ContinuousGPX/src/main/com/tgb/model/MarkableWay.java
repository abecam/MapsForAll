package com.tgb.model;

import java.util.ArrayList;

/**
 * Representation of an way (set of lon/lat) allowing to mark the use of it
 * @author Alain
 *
 */
public class MarkableWay {
	public ArrayList<MarkablePoint> myPoints  = new ArrayList<>();
	public int lastDirOfPass = 1; // Last passed in which direction (keep the passing in the same direction)
	public int partiallyPassed = 0; // 0-> none, 1-> partial, 2 -> fully
	public int nbOfPass = 0;
	
	public int ID = -1; // Will be mostly used for debugging
	public String name;
}
