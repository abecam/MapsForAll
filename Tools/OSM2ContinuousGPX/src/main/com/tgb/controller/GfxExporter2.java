/*
	Copyright 2014 Adrien PAVIE
	
	This file is part of BasicOSMParser.
	
	BasicOSMParser is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	BasicOSMParser is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with BasicOSMParser. If not, see <http://www.gnu.org/licenses/>.
 */

package com.tgb.controller;

import info.pavie.basicosmparser.model.Element;
import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Relation;
import info.pavie.basicosmparser.model.Way;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import com.tgb.gfx.DrawMap3;
import com.tgb.model.MapElement;
import com.tgb.model.MarkablePoint;

/**
 * This class allows you to export a Map of {@link Element}s as several CSV files.
 * Three CSV files are created :  nodes.csv, ways.csv, relations.csv
 * Those three CSV all contain generic informations about all elements (ID, last user ID, timestamp, ..., and tags).
 * Nodes.csv contains nodes coordinates.
 * Ways.csv contains the nodes list for each way.
 * Relations.csv contains the members list for each relation, and members roles.
 * @author Adrien PAVIE
 */
public class GfxExporter2 {
	
	public static final int BARRIER = 2;
	public static final int BUILDING = 8;
	public static final int NATURAL = 16;
	public static final int LANDUSE = 32;
	public static final int BIG_ROAD = 64;
	public static final int SMALL_ROAD = 128;
	public static final int PEDESTRIAN = 256;
	public static final int STEPS = 512;
	public static final int LEISURE = 1024;

    boolean onlyRoads = true; // Save only the roads or all
    
//OTHER METHODS
	/**
	 * Exports a map of Elements as CSV files.
	 * @param elements The element objects to export
	 * @param outputFolder The folder where CSV files will be written
	 * @throws IOException If an error occurs during CSV writing
	 */
	public void export(Map<String,Element> elements, DrawMap3 onDrawMap) throws IOException {
		
		Element currentElem = null;		 
		
		ArrayList<MapElement> allElements = new ArrayList<MapElement>();
		HashSet<String> nameAlreadyIn = new HashSet<>(); // We keep a name only once (not great but should work on small scale :), at least for a demo)
		
		double minLat = 200;
		double minLon = 200;
		
		for(String id : elements.keySet()) {
			currentElem = elements.get(id);
			
			/*
			 * CSV content (depends of object type)
			 */
			if(currentElem instanceof Node) {
				;
			}
			else if(currentElem instanceof Way) {
				//Way element (list nodes)
				Way currentWay = (Way) currentElem;

				
				if (currentElem.isVisible())
				{
					Node firstNode = currentWay.getNodes().get(0);

					// FLAGS -> One way, Car road, Bike road, Pedestrian way, ...
					Map<String, String> allTags = currentElem.getTags();

					long flag = 0;
					boolean isRoad = false;
					boolean isKnown = false; // Remove all unknown polygons

					
					if (allTags.containsKey("barrier"))
					{
						isKnown = true;
						flag = flag | BARRIER;
					}
					if (allTags.containsKey("area"))
					{
						String value = allTags.get("area");
						if (value.equals("yes"))
							flag = flag | 4;
					}
					if (allTags.containsKey("leisure"))
					{
						isKnown = true;
						flag = flag | LEISURE;
					}
					 
					if (allTags.containsKey("building"))
					{
						isKnown = true;
						flag = flag | BUILDING;
					}
					if (allTags.containsKey("natural"))
					{
						isKnown = true;
						flag = flag | NATURAL;
					}
					if (allTags.containsKey("landuse"))
					{
						isKnown = true;
						flag = flag | LANDUSE;
					}
					if (allTags.containsKey("highway"))
					{
						isRoad = true; // always export
						isKnown = true;

						flag = flag | 1;

						String value = allTags.get("highway");
						// Major roads
						if (value.equals("motorway") || value.equals("trunk") || value.equals("primary") || value.equals("secondary")
								||	value.equals("motorway_link") || value.equals("trunk_link") || value.equals("primary_link") || value.equals("secondary_link"))
						{
							flag = flag | BIG_ROAD;
						}
						// Small car roads
						if (value.equals("tertiary") || value.equals("unclassified") || value.equals("road") || value.equals("residential") || value.equals("service")
								||	value.equals("tertiary_link"))
						{
							flag = flag | SMALL_ROAD;
						}
						// Special places for pedestrian
						if (value.equals("living_street") || value.equals("pedestrian") || value.equals("road") || value.equals("track") || value.equals("footway")
								||	value.equals("path"))
						{
							flag = flag | PEDESTRIAN;
						}

						if (value.equals("steps"))
						{
							flag = flag | STEPS; // 512
						}
					}

					if ((isRoad || !onlyRoads) && isKnown )
					{
						// Element to show
						MapElement aNewElement = new MapElement();
						
						aNewElement.flags = flag;
						
						allElements.add(aNewElement);
						
						if (allTags.containsKey("name"))
						{
							// We save the name in another file
							String value = allTags.get("name");
							if (!nameAlreadyIn.contains(value))
							{
								nameAlreadyIn.add(value);
								aNewElement.name = value;
							}
						}
						// 12 bit max each ->
						int nbOfNodes = currentWay.getNodes().size();
						aNewElement.polyOrWay = new ArrayList<MarkablePoint>(nbOfNodes);


						for(int i=0; i < nbOfNodes; i++) {
							Node currentNode = currentWay.getNodes().get(i);

							double lonCur = currentNode.getLon();
							double latCur = currentNode.getLat();

							MarkablePoint onePoint = new MarkablePoint();
							
							onePoint.longitude = lonCur;
							onePoint.latitude = latCur;

							aNewElement.polyOrWay.add(onePoint);
							
							if (lonCur < minLon)
							{
								minLon = lonCur;
							}
							if (latCur < minLat)
							{
								minLat = latCur;
							}
						}
					}
				}
			}
			else if(currentElem instanceof Relation) {
				//
			}
			else {
				throw new RuntimeException("Unexpected kind of Element: "+currentElem.getClass().toString());
			}
		}
		
		onDrawMap.feedInfosAndNames(allElements, minLon, minLat);
	}
}
