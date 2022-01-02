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
public class TiledVectorExporter {
	public static final int BARRIER = 2;
	public static final int BUILDING = 8;
	public static final int NATURAL = 16;
	public static final int LANDUSE = 32;
	public static final int BIG_ROAD = 64;
	public static final int SMALL_ROAD = 128;
	public static final int PEDESTRIAN = 256;
	public static final int STEPS = 512;
	public static final int LEISURE = 1024;
	
	int initLon = 870966 ; // 8.709666;
    int initLat =  4938344 ; // 49.383443;
    
    // We need to calculate the distance in degree at this specific location
    
    // For latitude
    double latMetersToDecimalDegrees(double meters, double latitude)
    {
        return meters * 0.11054;
    }

    // For longitude
    double lonMetersToDecimalDegrees(double meters, double latitude)
    {
        return meters / (111.32 * 1000 * Math.cos(latitude * (Math.PI / 180)));
    }
    
//OTHER METHODS
    
	/**
	 * Exports a map of Elements as CSV files.
	 * @param elements The element objects to export
	 * @param outputFolder The folder where CSV files will be written
	 * @throws IOException If an error occurs during CSV writing
	 */
	public void export(Map<String,Element> elements, File outputFolder) throws IOException {
		int iMap = 0;
		
		initLon = initLon / 2;
		initLat = initLat /2;
		
		Element currentElem = null;
		
		//Create output
		StringBuilder csvNodesBuild = new StringBuilder("ID;UserID;timestamp;isVisible;version;changesetID;latitude;longitude;tags");
		
		
		StringBuilder waysAllNextLon = new StringBuilder("<drawable-list id=\"mapEMBL"+iMap+"\" background=\"Gfx.COLOR_WHITE\"><shape type=\"polygon\" points=\"["); 
				
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
					
					double lonPrev = firstNode.getLon();
					double latPrev = firstNode.getLat();

					int apprLonPrev =new Double(lonPrev*50000).intValue();
					int apprLatPrev =new Double(latPrev*50000).intValue();
					int apprLonCur = apprLonPrev;
					int apprLatCur = apprLatPrev;
					
					//[[120,120],[121,121],[122,122]]
							
					waysAllNextLon.append("["+(initLon-apprLonPrev)+",");
					waysAllNextLon.append(initLat-apprLatPrev+"],");

					int nbOfNodes = currentWay.getNodes().size();
					
					System.out.println("With "+nbOfNodes+" elements");
					for(int i=1; i < nbOfNodes; i++) {
						Node currentNode = currentWay.getNodes().get(i);

						double lonCur = currentNode.getLon();
						double latCur = currentNode.getLat();

						// 1000000 will be full precision. We remove one 0
						apprLonCur =(new Double(lonCur*50000)).intValue();
						apprLatCur =(new Double(latCur*50000)).intValue();

						waysAllNextLon.append("["+(initLon-apprLonCur)+",");
						waysAllNextLon.append(initLat-apprLatCur+"],");

						lonPrev = lonCur;
						latPrev = latCur;
						
						if (i % 50 == 0)
						{
							System.out.println("Cutting at "+i+" elements");
							
							waysAllNextLon.deleteCharAt(waysAllNextLon.length()-1);
							waysAllNextLon.append("]\" x=\"120\" y=\"120\" color=\"Gfx.COLOR_BLACK\" /><!-- [120,120] is the center of the screen--></drawable-list>\n");
							iMap++;
							waysAllNextLon.append("<drawable-list id=\"mapEMBL"+iMap+"\" background=\"Gfx.COLOR_WHITE\"><shape type=\"polygon\" points=\"["); 
						}
					}
					if (nbOfNodes == 2)
					{
						// We need to add another point so Connect IQ thinks it's a polygon
						waysAllNextLon.append("["+(initLon-apprLonCur)+",");
						waysAllNextLon.append(initLat-apprLatCur+"],");
					}
					waysAllNextLon.deleteCharAt(waysAllNextLon.length()-1);
					waysAllNextLon.append("]\" x=\"120\" y=\"120\" color=\"Gfx.COLOR_BLACK\" /><!-- [120,120] is the center of the screen--></drawable-list>\n");
					iMap++;
					waysAllNextLon.append("<drawable-list id=\"mapEMBL"+iMap+"\" background=\"Gfx.COLOR_WHITE\"><shape type=\"polygon\" points=\"["); 

					addTags(waysTags, currentElem);
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
		File csvWays = new File(outputFolder.getPath()+File.separator+"Ways"+date+".txt");
		//File csvRels = new File(outputFolder.getPath()+File.separator+"relations.csv");
		//writeTextFile(csvNodes, csvNodesBuild.toString());
		writeTextFile(csvWays, waysAllNextLon.toString()+"]\" x=\"120\" y=\"120\" color=\"Gfx.COLOR_BLACK\" /><!-- [120,120] is the center of the screen--></drawable-list>\n" );
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
