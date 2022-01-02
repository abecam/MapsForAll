package com.tgb.model;

import java.util.ArrayList;

public class GeneratedPath implements Comparable<GeneratedPath>{
	public float score; // in nbOfTime the original OSM points: 1 would be perfect, 
				 // normal calculations arrives around 3, iterating can go as low as 1.6
	
	public ArrayList<MarkablePoint> allPoint = new ArrayList<>();

	@Override
	public int compareTo(GeneratedPath o) {
		// TODO Auto-generated method stub
		return Float.compare(score, o.score);
	}
}
