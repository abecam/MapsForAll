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

/**
 * This class allows you to export a Map of {@link Element}s as several CSV files.
 * Three CSV files are created :  nodes.csv, ways.csv, relations.csv
 * Those three CSV all contain generic informations about all elements (ID, last user ID, timestamp, ..., and tags).
 * Nodes.csv contains nodes coordinates.
 * Ways.csv contains the nodes list for each way.
 * Relations.csv contains the members list for each relation, and members roles.
 * @author Adrien PAVIE
 */
public class JsonGarminExporter {
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
		
		Element currentElem = null;
		
		//Create output
		StringBuilder csvNodesBuild = new StringBuilder("ID;UserID;timestamp;isVisible;version;changesetID;latitude;longitude;tags");
		
		
		StringBuilder waysAllNextLon = new StringBuilder("<resources>\n");
				
      // Firsts are always lat+lon. Next will be diffs if not too big
		StringBuilder waysTags = new StringBuilder("var allTags = ["); // Same order as prev. If full value, read the tags
		StringBuilder csvRelsBuild = new StringBuilder("ID;UserID;timestamp;isVisible;version;changesetID;members;tags");

		//Create CSV entries for each element
		for(String id : elements.keySet()) {
			currentElem = elements.get(id);
			
			/*
			 * CSV content (depends of object type)
			 */
			if(currentElem instanceof Node) {
				//Node element (define latitude, longitude)
				Node currentNode = (Node) currentElem;
				addInformations(csvNodesBuild, currentElem);
				
				csvNodesBuild.append(";"+currentNode.getLat()+";"+currentNode.getLon()+";");
				
				addTags(csvNodesBuild, currentElem);
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
							sizeRoad = 4;
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

					boolean jsonLineEnded = false; // Did we close the line with the modulo ? If yes, we don't close it again
					
					if (sizeRoad > 0)
					{
						waysAllNextLon.append("<jsonData id=\"jsonDraw"+iMap+"\">["+sizeRoad+",");
						iMap++;
						
						double lonPrev = firstNode.getLon();
						double latPrev = firstNode.getLat();

						int apprLonPrev =new Double(lonPrev*20000).intValue();
						int apprLatPrev =new Double(latPrev*40000).intValue();
						apprLonPrev = (-(initLon-apprLonPrev))+ shiftX;
						apprLatPrev = initLat-apprLatPrev+ shiftY;

						int apprLonCur = apprLonPrev;
						int apprLatCur = apprLatPrev;

						int nbOfNodes = currentWay.getNodes().size();

						System.out.println("With "+nbOfNodes+" elements");
						for(int i=1; i < nbOfNodes; i++) {
							Node currentNode = currentWay.getNodes().get(i);

							double lonCur = currentNode.getLon();
							double latCur = currentNode.getLat();

							// 1000000 will be full precision. We remove one 0
							apprLonCur =(new Double(lonCur*20000)).intValue();
							apprLatCur =(new Double(latCur*40000)).intValue();

							apprLonCur = (-(initLon-apprLonCur))+ shiftX;
							apprLatCur = initLat-apprLatCur+ shiftY;

							int newLon = apprLonPrev+500; // Remove negative
							int newLat = apprLatPrev+500;
							newLat = newLat << 16;

							newLon = newLon | newLat;

							int prevLon = apprLonCur+500; // Remove negative
							int prevLat = apprLatCur+500;
							prevLat = prevLat << 16;

							prevLon = prevLon | prevLat;

							
							{
								waysAllNextLon.append(newLon+", "+prevLon+", ");

								if (i % 200 == 0)
								{
									waysAllNextLon.deleteCharAt(waysAllNextLon.length()-1);
									waysAllNextLon.append("]</jsonData>\n<jsonData id=\"jsonDraw"+iMap+"\">["+"1000,"); // 1000 means we continue
									iMap++;
									jsonLineEnded = true;
								}
							}
							
							apprLonPrev = apprLonCur;
							apprLatPrev = apprLatCur;
						}
						
						if (!jsonLineEnded)
						{
							waysAllNextLon.deleteCharAt(waysAllNextLon.length()-1);
							waysAllNextLon.append("]</jsonData>\n");
						}

						addTags(waysTags, currentElem);
					}
				}
			}
			else if(currentElem instanceof Relation) {
				//Relation element (list members and roles)
				Relation currentRel = (Relation) currentElem;
				addInformations(csvRelsBuild, currentElem);
				
				csvRelsBuild.append(";\"[");
				
				//List members and roles
				for(int i=0; i < currentRel.getMembers().size(); i++) {
					if(i > 0) {
						csvRelsBuild.append(",");
					}
					
					//Member
					csvRelsBuild.append(currentRel.getMembers().get(i).getId()
							+"=");
					
					//Role
					String role = currentRel.getMemberRole(currentRel.getMembers().get(i));
					if(role.equals("")) { role = "null"; }
					csvRelsBuild.append(role);
				}
				csvRelsBuild.append("]\";");
				
				addTags(csvRelsBuild, currentElem);
			}
			else {
				throw new RuntimeException("Unexpected kind of Element: "+currentElem.getClass().toString());
			}
		}
		
		GregorianCalendar today = new GregorianCalendar();
		SimpleDateFormat myFormat = new SimpleDateFormat("hh-mm-ss");
		
		String date = myFormat.format(today.getTime());
		//Write CSV
		//File csvNodes = new File(outputFolder.getPath()+File.separator+"nodes.csv");
		waysAllNextLon.deleteCharAt(waysAllNextLon.length()-1);
		File csvWays = new File(outputFolder.getPath()+File.separator+"Ways"+date+".txt");
		//File csvRels = new File(outputFolder.getPath()+File.separator+"relations.csv");
		//writeTextFile(csvNodes, csvNodesBuild.toString());
		writeTextFile(csvWays, waysAllNextLon.toString()+"\n</resources>\n");
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
