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
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.compress.utils.ByteUtils;
import org.apache.commons.io.FileUtils;

/**
 * This class allows you to export a Map of {@link Element}s as several CSV files.
 * Three CSV files are created :  nodes.csv, ways.csv, relations.csv
 * Those three CSV all contain generic informations about all elements (ID, last user ID, timestamp, ..., and tags).
 * Nodes.csv contains nodes coordinates.
 * Ways.csv contains the nodes list for each way.
 * Relations.csv contains the members list for each relation, and members roles.
 * @author Adrien PAVIE
 */
public class FileBinSourceExporter {
	
	public static final int BARRIER = 2;
	public static final int BUILDING = 8;
	public static final int NATURAL = 16;
	public static final int LANDUSE = 32;
	public static final int BIG_ROAD = 64;
	public static final int SMALL_ROAD = 128;
	public static final int PEDESTRIAN = 256;
	public static final int STEPS = 512;
	
	int initLon = 870966 ; // 8.709666;
    int initLat =  4938344 ; // 49.383443;

    boolean onlyRoads = false; // Save only the roads or all
    
//OTHER METHODS
	/**
	 * Exports a map of Elements as CSV files.
	 * @param elements The element objects to export
	 * @param outputFolder The folder where CSV files will be written
	 * @throws IOException If an error occurs during CSV writing
	 */
	public void export(Map<String,Element> elements, File outputFolder) throws IOException {
		
		GregorianCalendar today = new GregorianCalendar();
		SimpleDateFormat myFormat = new SimpleDateFormat("hh-mm-ss");
		
		String date = myFormat.format(today.getTime());
		
		int iMap = 0;
		
		Element currentElem = null;
		
		//Create output
		StringBuilder csvNodesBuild = new StringBuilder("ID;UserID;timestamp;isVisible;version;changesetID;latitude;longitude;tags");
		
		byte waysAllNextLon[] = new byte[1000];		
		File mapInBytes = new File(outputFolder.getPath()+File.separator+"OsmMap"+date+".bin");
		File nameOfWays = new File(outputFolder.getPath()+File.separator+"OsmMapNames"+date+".txt");
		
		byte[] latLonInBytes;
      // Firsts are always lat+lon. Next will be diffs if not too big
		 
		boolean firstValues = true;
		

		StringBuilder csvRelsBuild = new StringBuilder("ID;UserID;timestamp;isVisible;version;changesetID;members;tags");

		int maxBit = 0;
		boolean notDone = true;
		
		int iByteArray=0;
		
		HashSet<String> nameAlreadyIn = new HashSet<>(); // We keep a name only once (not great but should work on small scale :), at least for a demo)
		
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
				
				//addTags(csvNodesBuild, currentElem);
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

					if (allTags.containsKey("name"))
					{
						// We save the name in another file
						String value = allTags.get("name");
						if (!nameAlreadyIn.contains(value))
						{
							nameAlreadyIn.add(value);
							FileUtils.writeStringToFile(nameOfWays, value+"\n", "UTF-8", true);
						}
						else
						{
							FileUtils.writeStringToFile(nameOfWays, "\n", "UTF-8", true);
						}
					}
					else
					{
						FileUtils.writeStringToFile(nameOfWays, "\n", "UTF-8", true);
					}
					if (allTags.containsKey("barrier"))
					{
						flag = flag | BARRIER;
					}
					if (allTags.containsKey("area"))
					{
						String value = allTags.get("area");
						if (value.equals("yes"))
							flag = flag | 4;
					}
					if (allTags.containsKey("building"))
					{
						flag = flag | BUILDING;
					}
					if (allTags.containsKey("natural"))
					{
						flag = flag | NATURAL;
					}
					if (allTags.containsKey("landuse"))
					{
						flag = flag | LANDUSE;
					}
					if (allTags.containsKey("highway"))
					{
						isRoad = true; // always export

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

					if (isRoad || !onlyRoads)
					{
						double lonPrev = firstNode.getLon();
						double latPrev = firstNode.getLat();

						// 1000000 will be full precision. We remove one 0 and divide by 2
						int apprLonPrev =new Double(lonPrev*50000).intValue();
						int apprLatPrev =new Double(latPrev*50000).intValue();


						// First the first lat & lon found, absolute. Everything else will be relative to that
						ByteUtils.toLittleEndian(waysAllNextLon, apprLonPrev, iByteArray, 4);

						iByteArray+=4;

						ByteUtils.toLittleEndian(waysAllNextLon, apprLatPrev, iByteArray, 4);

						iByteArray+=4;

						int apprLonCur = apprLonPrev;
						int apprLatCur = apprLatPrev;


						ByteUtils.toLittleEndian(waysAllNextLon, flag, iByteArray, 4);

						iByteArray+=4;


						// 12 bit max each ->
						int nbOfNodes = currentWay.getNodes().size();

						// nb of nodes next, max 2000, could be saved with 11bits

						ByteUtils.toLittleEndian(waysAllNextLon, nbOfNodes, iByteArray, 4);

						iByteArray+=4;

						for(int i=1; i < nbOfNodes; i++) {
							Node currentNode = currentWay.getNodes().get(i);

							double lonCur = currentNode.getLon();
							double latCur = currentNode.getLat();

							// 1000000 will be full precision. We remove one 0 and divide by 2
							apprLonCur =(new Double(lonCur*50000)).intValue();
							apprLatCur =(new Double(latCur*50000)).intValue();	

							int diffLonCur = apprLonCur - apprLonPrev;
							int diffLatCur = apprLatCur - apprLatPrev;

							int tmpLon = diffLonCur;
							int tmpLat = diffLatCur;

							// How many bits ???
							for (int shift = 0; (shift < 32) && notDone; shift++)
							{
								tmpLon = tmpLon >> 1;
							tmpLat = tmpLat >> 1;


							if (tmpLon == 0 && tmpLat == 0)
							{
								if (shift > maxBit)
								{
									maxBit = shift;
									System.out.println("Last max bit : "+(maxBit+1)+" : "+diffLonCur+" - "+diffLatCur);

									if (maxBit == 31)
									{
										notDone = false;
									}
									break;
								}
								else
								{
									break;
								}
							}
							}

							ByteUtils.toLittleEndian(waysAllNextLon, apprLonCur, iByteArray, 3);

							iByteArray+=3;

							ByteUtils.toLittleEndian(waysAllNextLon, apprLatCur, iByteArray, 3);

							iByteArray+=3;

							//byte lonDiffFromBytes[] = Arrays.copyOfRange(waysAllNextLon,iByteArray-2 , iByteArray+2);

							//						System.out.println("DiffLonLat: "+diffLonCur+" - "+diffLatCur+" - Together:"+latLonInBytes+" In bytes:"+latLonInBytes[0]+" : "+latLonInBytes[1]+" : "+byteArrayToInt(lonDiffFromBytes));
							//						System.out.println(((0xFF & (latLonInBytes[0]))<<8)|(0xFF & (latLonInBytes[1])));


							//waysAllNextLon.append("dc.drawLine("+apprLonPrev+"+ x, "+apprLatPrev+" + y, "+apprLonCur+" + x, "+ apprLatCur+ "+ y);\n");

							apprLonPrev = apprLonCur;
							apprLatPrev = apprLatCur;

							if (iByteArray > 900)
							{
								//System.out.println("Writting "+iByteArray+" bytes");

								byte wayToWrite[] = Arrays.copyOfRange(waysAllNextLon, 0, iByteArray);

								FileUtils.writeByteArrayToFile(mapInBytes, wayToWrite,true);

								waysAllNextLon = new byte[1000];
								iByteArray = 0;
								iMap++; 
							}
						}
						byte wayToWrite[] = Arrays.copyOfRange(waysAllNextLon,0 , iByteArray);

						//System.out.println("Writting "+iByteArray+" bytes");

						FileUtils.writeByteArrayToFile(mapInBytes, wayToWrite,true);
						waysAllNextLon = new byte[1000];
						iByteArray = 0;
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
				
				//addTags(csvRelsBuild, currentElem);
			}
			else {
				throw new RuntimeException("Unexpected kind of Element: "+currentElem.getClass().toString());
			}
		}
		
		
		//Write CSV
		//File csvNodes = new File(outputFolder.getPath()+File.separator+"nodes.csv");
		File csvWays = new File(outputFolder.getPath()+File.separator+"Ways"+date+".txt");
		//File csvRels = new File(outputFolder.getPath()+File.separator+"relations.csv");
		//writeTextFile(csvNodes, csvNodesBuild.toString());
		writeTextFile(csvWays, waysAllNextLon.toString());
		//writeTextFile(csvRels, csvRelsBuild.toString());
	}
	
	public static int byteArrayToInt(byte[] b) 
	{
	    int value = 0;
	    for (int i = 0; i < 4; i++) {
	        int shift = (4 - 1 - i) * 8;
	        value += (b[i] & 0x000000FF) << shift;
	    }
	    return value;
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
