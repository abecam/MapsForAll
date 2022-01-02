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

package info.pavie.basicosmparser.controller;

import info.pavie.basicosmparser.model.Element;
import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Relation;
import info.pavie.basicosmparser.model.Way;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import com.tgb.utils.MyGPXCreator;

/**
 * This class allows you to export a Map of {@link Element}s as several CSV files.
 * Three CSV files are created :  nodes.csv, ways.csv, relations.csv
 * Those three CSV all contain generic informations about all elements (ID, last user ID, timestamp, ..., and tags).
 * Nodes.csv contains nodes coordinates.
 * Ways.csv contains the nodes list for each way.
 * Relations.csv contains the members list for each relation, and members roles.
 * @author Adrien PAVIE
 */
public class GpxExporter {
	int initLon = 870966 ; // 8.709666;
    int initLat =  4938344 ; // 49.383443;
    int shiftX = 120;
    int shiftY = 160;
//OTHER METHODS
	/**
	 * Exports a map of Elements as CSV files.
	 * @param elements The element objects to export
	 * @param outputFolder The folder where CSV files will be written
	 * @throws IOException If an error occurs during CSV writing
	 */
	public void export(Map<String,Element> elements, File outputFolder) throws IOException {
		int iMap = 0;
		
		initLon = initLon / 5;
		initLat = (initLat * 2) / 5;
		
		MyGPXCreator myGPXCreator = new MyGPXCreator();
		
		myGPXCreator.createDocument();
		
		Element currentElem = null;
				
      // Firsts are always lat+lon. Next will be diffs if not too big
		StringBuilder waysTags = new StringBuilder("var allTags = ["); // Same order as prev. If full value, read the tags

		//Create CSV entries for each element
		for(String id : elements.keySet()) {
			currentElem = elements.get(id);
			
			/*
			 * CSV content (depends of object type)
			 */
			if(currentElem instanceof Node) {
				//Node element (define latitude, longitude)
				Node currentNode = (Node) currentElem;
				
				// Would be POI
//				addInformations(csvNodesBuild, currentElem);
//				
//				csvNodesBuild.append(";"+currentNode.getLat()+";"+currentNode.getLon()+";");
//				
//				addTags(csvNodesBuild, currentElem);
			}
			else if(currentElem instanceof Way) {
				//Way element (list nodes)
				Way currentWay = (Way) currentElem;

				if (currentElem.isVisible())
				{
					Node firstNode = currentWay.getNodes().get(0);
					
					int sizeRoad = -1;
					
					// FLAGS -> One way, Car road, Bike road, Pedestrian way, ...
					Map<String, String> allTags = currentElem.getTags();
					
				
					if (allTags.containsKey("highway"))
					{
						String value = allTags.get("highway");
						// Major roads
						if (value.equals("motorway") || value.equals("trunk") || value.equals("primary") || value.equals("secondary")
								||	value.equals("motorway_link") || value.equals("trunk_link") || value.equals("primary_link") || value.equals("secondary_link"))
						{
							//sizeRoad = 4;
						}
						// Small car roads
						if (value.equals("tertiary") || value.equals("unclassified") || value.equals("road") || value.equals("residential") || value.equals("service")
								||	value.equals("tertiary_link"))
						{
							sizeRoad = 3; 
						}
						// Special places for pedestrian
						if (value.equals("living_street") || value.equals("pedestrian") || value.equals("road") || value.equals("track") || value.equals("footway")
								||	value.equals("path"))
						{
							sizeRoad = 2;
						}

						if (value.equals("steps"))
						{
							sizeRoad = 20; // To recognize it :)
						}
					}
					
					if (sizeRoad > 0)
					{	
						int nbOfNodes = currentWay.getNodes().size();

						System.out.println("With "+nbOfNodes+" elements");
						for(int i=0; i < nbOfNodes; i++) {
							
							iMap++;
							
							Node currentNode = currentWay.getNodes().get(i);

							double lonCur = currentNode.getLon();
							double latCur = currentNode.getLat();
							long elevation = 0; // Where ?
							
							myGPXCreator.addLatLonEle(latCur, lonCur, elevation);
							
//							if (iMap > 1000)
//							{
//								break;
//							}
						}			

						addTags(waysTags, currentElem);
					}
				}
			}
			else if(currentElem instanceof Relation) {
				//Relation element (list members and roles)
				Relation currentRel = (Relation) currentElem;
				
				// addTags(csvRelsBuild, currentElem);
			}
			else {
				throw new RuntimeException("Unexpected kind of Element: "+currentElem.getClass().toString());
			}
			if (iMap > 9000)
			{
				break;
			}
		}
		
		GregorianCalendar today = new GregorianCalendar();
		SimpleDateFormat myFormat = new SimpleDateFormat("hh-mm-ss");
		
		String date = myFormat.format(today.getTime());
		
		myGPXCreator.closeAndSave(outputFolder.getPath()+File.separator+"Ways"+date+".gpx");
		//writeTextFile(csvRels, csvRelsBuild.toString());
	}
	
	/**
	 * Adds the tags of an Element in the given StringBuilder
	 * @param sb The string builder
	 * @param elem The element
	 */
	private void addTags(StringBuilder sb, Element elem) {
		//Start tags array
		boolean firstTag = true;
		
		//Add each tag
		for(String key : elem.getTags().keySet()) {
			if(!firstTag) {
				sb.append("+");
			} else {
				firstTag = false;
			}
			
			sb.append(key+"="+elem.getTags().get(key));
		}
	}
	
	/**
	 * Adds common informations about a Element in a StringBuilder
	 * @param sb The string builder
	 * @param elem The element
	 */
	private void addInformations(StringBuilder sb, Element elem) {
		sb.append('\n'+elem.getId()+';'
				+elem.getUid()+';'
				+elem.getTimestamp()+';'
				+elem.isVisible()+';'
				+elem.getVersion()+';'
				+elem.getChangeset());
	}
	
	/**
	 * Writes a text file.
	 * @param output The file to write in
	 * @param text The text to write
	 * @throws IOException If an error occurs during writing
	 */
	private void writeTextFile(File output, String text) throws IOException {
		Writer w = new OutputStreamWriter(new FileOutputStream(output));
		w.write(text);
		w.close();
	}
}
