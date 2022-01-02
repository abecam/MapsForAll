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

import info.pavie.basicosmparser.controller.OSMParser;
import info.pavie.basicosmparser.model.Element;
import info.pavie.basicosmparser.model.Node;
import info.pavie.basicosmparser.model.Relation;
import info.pavie.basicosmparser.model.Way;

import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.xml.sax.SAXException;

import com.tgb.gfx.CoordInfo;
import com.tgb.gfx.DrawMap4;
import com.tgb.mapextractor.view.MainWindow;
import com.tgb.model.AllUsedNodes;
import com.tgb.model.GeneratedPath;
import com.tgb.model.MarkablePoint;
import com.tgb.model.MarkableWay;
import com.tgb.model.NodeWithPoints;
import com.tgb.model.StackOfJonctionsUsed;
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
public class GpxProcessor {
	private static final int MS_FOR_NO_PAUSE =0;
	private static final boolean inPauseMode = false;
	int initLon = 870966 ; // 8.709666;
    int initLat =  4938344 ; // 49.383443;
    int shiftX = 120;
    int shiftY = 160;
	private boolean paused = false;
	
	ArrayList<MarkablePoint> allFoundPoint = new ArrayList<>();
	ArrayList<MarkableWay> allFoundWays = new ArrayList<>();
	HashMap<Long, NodeWithPoints> allNodesId = new HashMap<>(); // See if a node has already be used -> intersection
	
	ArrayList<GeneratedPath> allGeneratedPath = new ArrayList<>(); // All the generated path. From them we will keep only the N best
	GeneratedPath currentPathSelected;
	
	DrawMap4 myDrawer;
	MainWindow ourMainWindow;
	
	private boolean isProcessing = false;
	
	HashSet<MarkablePoint> selectedPoints = new HashSet<MarkablePoint>();
	
	File osmFileToOpen = null;
	
	MyGPXCreator myGPXCreator = new MyGPXCreator();
	
	public GpxProcessor(MainWindow mainWindow)
	{
		ourMainWindow = mainWindow;
	}
	
	public void setOSMFile(File osmToOpen)
	{
		// Clean-up everything first
		cleanAll();
		
		myDrawer = ourMainWindow.myDrawer;
		
		osmFileToOpen = osmToOpen;
		
		OSMParser parser = new OSMParser();
		
		GfxExporter3 exporterGfx = new GfxExporter3();	
		
		try {
			Map<String, Element> parsedOSM = parser.parse(osmToOpen);
			
			exporterGfx.export(parsedOSM, myDrawer);
			
			myDrawer.createAndShowMap();
			
			Container container = ourMainWindow.getContentPane();
			
			//ourMainWindow.addKeyListener(myDrawer);
			
			container.add(myDrawer);
			
			//ourMainWindow.add(myDrawer);
			
			//ourMainWindow.pack();
			
			export(parsedOSM);
			
		} catch (IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void cleanAll()
	{
		allFoundPoint = new ArrayList<>();
		allFoundWays = new ArrayList<>();
		allNodesId = new HashMap<>(); // See if a node has already be used -> intersection
		
		cleanGeneratedAndSelected();
	}

	private void cleanGeneratedAndSelected() {
		allGeneratedPath = new ArrayList<>(); // All the generated path. From them we will keep only the N best
		GeneratedPath currentPathSelected = null;
		selectedPoints = new HashSet<>();
	}
	
//OTHER METHODS
	/**
	 * Exports a map of Elements as CSV files.
	 * @param elements The element objects to export
	 * @param outputFolder The folder where CSV files will be written
	 * @throws IOException If an error occurs during CSV writing
	 */
	public void export(Map<String,Element> elements) throws IOException {
		
		int iTrack = 0;
		
		initLon = initLon / 5;
		initLat = (initLat * 2) / 5;
		

		
		Element currentElem = null;
				
  		//Create CSV entries for each element
		for(String id : elements.keySet()) {
			currentElem = elements.get(id);
			
			/*
			 * CSV content (depends of object type)
			 */
			if(currentElem instanceof Node) {
				//
			}
			else if(currentElem instanceof Way) {
				//Way element (list nodes)
				Way currentWay = (Way) currentElem;

				if (currentElem.isVisible())
				{
					int sizeRoad = -1;
					
					// FLAGS -> One way, Car road, Bike road, Pedestrian way, ...
					Map<String, String> allTags = currentElem.getTags();
					
				
					if (allTags.containsKey("highway"))
					{
						// We take it anyway
						sizeRoad = 1;
						
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
					
					if (sizeRoad > 0)
					{	
						int nbOfNodes = currentWay.getNodes().size();

						System.out.println("With "+nbOfNodes+" elements");
						MarkableWay oneNewWay = new MarkableWay();
						
						oneNewWay.ID = iTrack++;
						allFoundWays.add(oneNewWay);
						
						for(int i=0; i < nbOfNodes; i++) {
							Node currentNode = currentWay.getNodes().get(i);

							double lonCur = currentNode.getLon();
							double latCur = currentNode.getLat();
							long elevation = 0; // Where ?
							
							MarkablePoint oneNewPoint = new MarkablePoint();
							oneNewPoint.longitude = lonCur;
							oneNewPoint.latitude = latCur;
							oneNewPoint.belongingTo = oneNewWay;
							oneNewPoint.forNodeId = currentNode.getNbID();
							
							allFoundPoint.add(oneNewPoint);
										
							if (allNodesId.containsKey(oneNewPoint.forNodeId))
							{
								NodeWithPoints oneNode = allNodesId.get(oneNewPoint.forNodeId);
								int nbOfIntersect = 0;
								
								// All point at that node get the same number of intersection,
								// we need to be careful if we use the count (we don't as of now)
								for (int onePointId : oneNode.forMarkablePoint)
								{
									MarkablePoint onePoint = allFoundPoint.get(onePointId);
									nbOfIntersect = onePoint.nbIntersect;
									onePoint.nbIntersect = nbOfIntersect+1;
								}
								System.out.println("Node "+oneNewPoint.forNodeId+" intersecting "+(nbOfIntersect+1)+ " times");
								oneNewPoint.nbIntersect = (nbOfIntersect+1);
								
								// And add the last added point of all points -> last index
								oneNode.forMarkablePoint.add(allFoundPoint.size() - 1);
							}
							else
							{
								NodeWithPoints oneNewNode = new NodeWithPoints();
								oneNewNode.nodeID = oneNewPoint.forNodeId;
								
								// Last added point -> last index
								oneNewNode.forMarkablePoint.add(allFoundPoint.size() -1);
								
								allNodesId.put(oneNewNode.nodeID, oneNewNode);
							}
							
							oneNewWay.myPoints.add(oneNewPoint);
							oneNewPoint.atPos = oneNewWay.myPoints.size() -1;
							
//							if (iMap > 1000)
//							{
//								break;
//							}
						}			
						
//						myDrawer.updateCurrentProcess(allFoundPoint);
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
//			if (iMap > 1000)
//			{
//				break;
//			}
		}
	}
	
	MarkablePoint centerToCheck = new MarkablePoint();
	/**
	 * Call be the drawer to select/unselect point following the given type. The drawer ensure to use the correct lon/lat and size
	 * @param lonSelection
	 * @param latSelection
	 * @param withRadius
	 * @param typeOfSelect
	 * @param inSelectionMode 
	 */
	public void selectUnselectPoints(double lonSelection, double latSelection, double withRadius, TypeOfSelect typeOfSelect, boolean inSelectionMode)
	{
		myDrawer.myInfoText.variableText.clear();
		isProcessing  = false;
		myDrawer.shiftMarkedPoint(false);
		
//		centerToCheck.longitude = lonSelection;
//		centerToCheck.latitude = latSelection;
//		centerToCheck.forNodeId = -1000; // Must be there so we don't add a new one each time
//		
//		selectedPoints.add(centerToCheck);

		if (typeOfSelect == TypeOfSelect.POINT_SELECT)
		{
			System.out.println("A- Selection type "+typeOfSelect.name());
			if (inSelectionMode)
			{

				for (MarkablePoint onePoint : allFoundPoint)
				{
					double distanceToSelectionCenter = Math.sqrt((Math.pow((onePoint.longitude - lonSelection),2)+Math.pow((onePoint.latitude - latSelection),2)));

					//System.out.println("Distance to center = "+distanceToSelectionCenter+" compared to "+withRadius);
					if (distanceToSelectionCenter < withRadius)
					{
						selectedPoints.add(onePoint);
					}
				}
			}
			else
			{
				for (MarkablePoint onePoint : allFoundPoint)
				{
					double distanceToSelectionCenter = Math.sqrt((Math.pow((onePoint.longitude - lonSelection),2)+Math.pow((onePoint.latitude - latSelection),2)));

					//System.out.println("Distance to center = "+distanceToSelectionCenter+" compared to "+withRadius);
					if (distanceToSelectionCenter < withRadius)
					{
						selectedPoints.remove(onePoint);
					}
				}
			}
		}
		else if (typeOfSelect == TypeOfSelect.SEGMENT_SELECT)
		{
			if (inSelectionMode)
			{

				for (MarkablePoint onePoint : allFoundPoint)
				{
					double distanceToSelectionCenter = Math.sqrt((Math.pow((onePoint.longitude - lonSelection),2)+Math.pow((onePoint.latitude - latSelection),2)));

					//System.out.println("Distance to center = "+distanceToSelectionCenter+" compared to "+withRadius);
					if (distanceToSelectionCenter < withRadius)
					{
						selectedPoints.add(onePoint);
						
						MarkableWay onWay = onePoint.belongingTo;
						boolean checking = true;
						
						int nextPos = onePoint.atPos;
						
						// Check all the point belonging to that way, until the first intersection *out* of selection
						
						// Forward
						while (checking)
						{
							nextPos = nextPos + 1;
							if (nextPos < onWay.myPoints.size())
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								// We don't need to check if the point is inside the selection, as it will be added anyway if it does.
								// But we need to check if it intersect!
								if (oneNextPoint.nbIntersect == 0)
								{
									selectedPoints.add(oneNextPoint);
								}
								else
								{
									checking = false;
								}
							}
							else
							{
								checking = false;
							}
						}
						
						checking = true;
						
						// Back
						while (checking)
						{
							nextPos = nextPos - 1;
							if (nextPos > 0)
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								// We don't need to check if the point is inside the selection, as it will be added anyway if it does.
								// But we need to check if it intersect!
								if (oneNextPoint.nbIntersect == 0)
								{
									selectedPoints.add(oneNextPoint);
								}
								else
								{
									checking = false;
								}
							}
							else
							{
								checking = false;
							}
						}
					}
				}
			}
			else
			{
				for (MarkablePoint onePoint : allFoundPoint)
				{
					double distanceToSelectionCenter = Math.sqrt((Math.pow((onePoint.longitude - lonSelection),2)+Math.pow((onePoint.latitude - latSelection),2)));

					//System.out.println("Distance to center = "+distanceToSelectionCenter+" compared to "+withRadius);
					if (distanceToSelectionCenter < withRadius)
					{
						selectedPoints.remove(onePoint);
						
						MarkableWay onWay = onePoint.belongingTo;
						boolean checking = true;
						
						int nextPos = onePoint.atPos;
						
						// Check all the point belonging to that way, until the first intersection *out* of selection
						
						// Forward
						while (checking)
						{
							nextPos = nextPos + 1;
							if (nextPos < onWay.myPoints.size())
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								// We don't need to check if the point is inside the selection, as it will be added anyway if it does.
								// But we need to check if it intersect!
								if (oneNextPoint.nbIntersect == 0)
								{
									selectedPoints.remove(oneNextPoint);
								}
								else
								{
									checking = false;
								}
							}
							else
							{
								checking = false;
							}
						}
						
						checking = true;
						
						// Back
						while (checking)
						{
							nextPos = nextPos - 1;
							if (nextPos > 0)
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								// We don't need to check if the point is inside the selection, as it will be added anyway if it does.
								// But we need to check if it intersect!
								if (oneNextPoint.nbIntersect == 0)
								{
									selectedPoints.remove(oneNextPoint);
								}
								else
								{
									checking = false;
								}
							}
							else
							{
								checking = false;
							}
						}
					}
				}
			}
		}
		else if (typeOfSelect == TypeOfSelect.WAY_SELECT)
		{
			if (inSelectionMode)
			{
				HashSet<Integer> doneWays = new HashSet<>();
				
				for (MarkablePoint onePoint : allFoundPoint)
				{
					double distanceToSelectionCenter = Math.sqrt((Math.pow((onePoint.longitude - lonSelection),2)+Math.pow((onePoint.latitude - latSelection),2)));

					//System.out.println("Distance to center = "+distanceToSelectionCenter+" compared to "+withRadius);
					if (distanceToSelectionCenter < withRadius)
					{
						selectedPoints.add(onePoint);
						
						MarkableWay onWay = onePoint.belongingTo;
						boolean checking = true;
						
						int nextPos = onePoint.atPos;
						
						// Check all the point belonging to that way, until the first intersection *out* of selection
						
						// Forward
						while (checking)
						{
							nextPos = nextPos + 1;
							if (nextPos < onWay.myPoints.size())
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								if (selectedPoints.contains(oneNextPoint) && !doneWays.contains(onWay.ID))
								{
									myDrawer.myInfoText.variableText.add("Already Here !! "+oneNextPoint.belongingTo.ID+" - "+oneNextPoint.forNodeId);
								}
								// All point of the way
								selectedPoints.add(oneNextPoint);
							}
							else
							{
								checking = false;
							}
						}
						
						checking = true;
						
						// Back
						while (checking)
						{
							nextPos = nextPos - 1;
							if (nextPos > 0)
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								// All point of the way
								selectedPoints.add(oneNextPoint);
							}
							else
							{
								checking = false;
							}
						}
						
						doneWays.add(onWay.ID);
					}
				}
			}
			else
			{
				for (MarkablePoint onePoint : allFoundPoint)
				{
					double distanceToSelectionCenter = Math.sqrt((Math.pow((onePoint.longitude - lonSelection),2)+Math.pow((onePoint.latitude - latSelection),2)));

					//System.out.println("Distance to center = "+distanceToSelectionCenter+" compared to "+withRadius);
					if (distanceToSelectionCenter < withRadius)
					{
						selectedPoints.remove(onePoint);
						
						MarkableWay onWay = onePoint.belongingTo;
						boolean checking = true;
						
						int nextPos = onePoint.atPos;
						
						// Check all the point belonging to that way, until the first intersection *out* of selection
						
						// Forward
						while (checking)
						{
							nextPos = nextPos + 1;
							if (nextPos < onWay.myPoints.size())
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								// All point of the way
								selectedPoints.remove(oneNextPoint);
							}
							else
							{
								checking = false;
							}
						}
						
						checking = true;
						
						// Back
						while (checking)
						{
							nextPos = nextPos - 1;
							if (nextPos > 0)
							{
								MarkablePoint oneNextPoint = onWay.myPoints.get(nextPos);
								
								// All point of the way
								selectedPoints.remove(oneNextPoint);
							}
							else
							{
								checking = false;
							}
						}
					}
				}
			}
		}
		myDrawer.updateCurrentProcess((MarkablePoint[]) selectedPoints.toArray(new MarkablePoint[selectedPoints.size()]));
	}
	
	ArrayList<MarkablePoint> foundPointsToProcess = new ArrayList<>();
	ArrayList<MarkableWay> foundWaysToProcess = new ArrayList<>();
	HashMap<Long, NodeWithPoints> foundNodesIdToProcess = new HashMap<>(); // See if a node has already be used -> intersection
	
	public void transformSelectionToElementsToProcess()
	{
		isProcessing  = false;
		
		foundPointsToProcess = new ArrayList<>();
		foundWaysToProcess = new ArrayList<>();
		foundNodesIdToProcess = new HashMap<>(); // See if a node has already be used -> intersection
		
		myDrawer.shiftMarkedPoint(true);
		
		// Go through each existing ways to rebuild the collection
		// 2 detached segments of a way creates one new way!
		
		//allFoundPoint, allFoundWays, allNodesId
		int iTrack = 0; // Ids for ways
		
		for (MarkableWay oneWay : allFoundWays)
		{			
			boolean wayCreated = false;
			MarkableWay oneNewWay = null;
			
			for (MarkablePoint onePoint : oneWay.myPoints)
			{
				if (selectedPoints.contains(onePoint))
				{
					if (!wayCreated)
					{
						oneNewWay = new MarkableWay();
						oneNewWay.ID = iTrack++;
						foundWaysToProcess.add(oneNewWay);
						
						// Do not create a new way until a new one OR a gap
						wayCreated = true;
						
						drawOnePoint(myDrawer, onePoint, 25, 20, 250);
					}
					// Now add the point
					
					MarkablePoint oneNewPoint = new MarkablePoint();
					oneNewPoint.longitude = onePoint.longitude;
					oneNewPoint.latitude = onePoint.latitude;
					oneNewPoint.belongingTo = oneNewWay;
					oneNewPoint.forNodeId = onePoint.forNodeId;
					
					foundPointsToProcess.add(oneNewPoint);
								
					if (foundNodesIdToProcess.containsKey(oneNewPoint.forNodeId))
					{
						NodeWithPoints oneNode = foundNodesIdToProcess.get(oneNewPoint.forNodeId);
						int nbOfIntersect = 0;
						
						// All point at that node get the same number of intersection,
						// we need to be careful if we use the count (we don't as of now)
						for (int onePointId : oneNode.forMarkablePoint)
						{
							MarkablePoint oneIntersectingPoint = foundPointsToProcess.get(onePointId);
							nbOfIntersect = oneIntersectingPoint.nbIntersect;
							oneIntersectingPoint.nbIntersect = nbOfIntersect+1;
							
							drawOnePoint(myDrawer, onePoint, 255, 20+nbOfIntersect*50, 250);
						}
						System.out.println("Node "+oneNewPoint.forNodeId+" intersecting "+(nbOfIntersect+1)+ " times");
						oneNewPoint.nbIntersect = (nbOfIntersect+1);
						
						// And add the last added point of all points -> last index
						oneNode.forMarkablePoint.add(foundPointsToProcess.size() - 1);
					}
					else
					{
						NodeWithPoints oneNewNode = new NodeWithPoints();
						oneNewNode.nodeID = oneNewPoint.forNodeId;
						
						// Last added point -> last index
						oneNewNode.forMarkablePoint.add(foundPointsToProcess.size() -1);
						
						foundNodesIdToProcess.put(oneNewNode.nodeID, oneNewNode);
					}
					
					oneNewWay.myPoints.add(oneNewPoint);
					oneNewPoint.atPos = oneNewWay.myPoints.size() -1;
					
					try {
						if (inPauseMode)
						{
							while (paused)
							{
								Thread.sleep(1000);
							}
							paused = true;
						}
						else
						{
							Thread.sleep(MS_FOR_NO_PAUSE);
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{
					wayCreated = false;
					drawOnePoint(myDrawer, onePoint, 0, 20, 20);
				}
							
				//myDrawer.updateCurrentProcess((MarkablePoint[]) foundPointsToProcess.toArray(new MarkablePoint[foundPointsToProcess.size()]));
				
			}
		}	
	}
	
	// Check that there aren't too many detached road, or the processing will be a mess
	public int countOrphans()
	{
		return 0;
	}
	
	// Remove small orphans and isolated points.
	public void cleanUp()
	{
		
	}
	
	public void removeWorstCases()
	{
		Collections.sort(allGeneratedPath);
		
		System.out.println("Lowest score is "+allGeneratedPath.get(0).score+", worst score is "+allGeneratedPath.get(allGeneratedPath.size() - 1).score);
		// Now find the 20 best...
		ArrayList<GeneratedPath> curratedList = new ArrayList<>();
		
		for (int iGPath = 0; iGPath < 20; iGPath++)
		{
			if (iGPath >= allGeneratedPath.size())
			{
				System.out.println("Less than 20 generated paths ("+allGeneratedPath.size()+")");
				break; // Shouldn't happen but better safe than sorry
			}
			curratedList.add(allGeneratedPath.get(iGPath));
		}
		
		allGeneratedPath = curratedList;
		selectOnePath(0);
	}
	
	public int getNbOfBestPaths()
	{
		return allGeneratedPath.size();
	}
	/**
	 * Select from the 20 best paths
	 * @param iPath
	 */
	public void selectOnePath(int iPath)
	{
		if (iPath >= 0 && iPath < allGeneratedPath.size())
		{
			currentPathSelected = allGeneratedPath.get(iPath);
			
			// Show it as well
			myDrawer.updateCurrentProcess((MarkablePoint[]) currentPathSelected.allPoint.toArray(new MarkablePoint[currentPathSelected.allPoint.size()]));
		}
	}
	
	public void doTheDegradation(int maxAngle, int nbIter, int incrAngle)
	{
		// allGeneratedPath
		GpxDecimator myDecimator = new GpxDecimator();
		
		decimatedPath = myDecimator.decimatePoints(nbIter, currentPathSelected.allPoint, allFoundWays, myDrawer, maxAngle, incrAngle);
		
		// Show it as well
		myDrawer.updateCurrentProcess((MarkablePoint[]) decimatedPath.toArray(new MarkablePoint[decimatedPath.size()]));
	}
	
	public void doTheProcessing()
	{
		// First clean-up previous results
		cleanGeneratedAndSelected();
		
		isProcessing = true;
		// Now process the connected road to try build a continous GPX
		//float scoreDeepSearch = processWays(allFoundPoint, allFoundWays, allNodesId, myGPXCreator, myDrawer);

		processWaysDeepDepthWRandom(foundPointsToProcess, foundWaysToProcess, foundNodesIdToProcess, myDrawer);

		GregorianCalendar today = new GregorianCalendar();
		SimpleDateFormat myFormat = new SimpleDateFormat("hh-mm-ss");

		String date = myFormat.format(today.getTime());
		for (int iRandom = 0; iRandom < 400; iRandom++)
		{
			processWaysDeepDepthWRandom(foundPointsToProcess, foundWaysToProcess, foundNodesIdToProcess, myDrawer);
		}
	}
	
	private float processWays(ArrayList<MarkablePoint> allFoundPoint, ArrayList<MarkableWay> allFoundWays, HashMap<Long,NodeWithPoints> allNodesId, DrawMap4 myDrawer) {
		int maxDepthOfStack = 0;
		
		boolean canContinue = true;
		MarkableWay currentWay = allFoundWays.get(0);
		currentWay.partiallyPassed = 1;
		
		int posOnWay = 0;
		int currentDirection = 1; // Increasing
		
		// Utility collections
		AllUsedNodes allNodesAlreadyUsed = new AllUsedNodes();
		StackOfJonctionsUsed stackOfJonctions = new StackOfJonctionsUsed();
		MarkablePoint pointFromStack = null; // If it exist, when on it we need to go back there, even if already used
		int nbOfJonctionsAvailable = 0;
		
		resetAllPoints(allFoundPoint);
		
		while (canContinue)
		{
			System.out.println("Point  at "+posOnWay);
			MarkablePoint currentPoint = currentWay.myPoints.get(posOnWay);
			
			myDrawer.myInfoText.line1 = "Point "+currentPoint.forNodeId+" at "+posOnWay+" with at pos "+currentPoint.atPos;
			
			allNodesAlreadyUsed.myPoints.add(currentPoint);
			
			currentPoint.nbOfTimePassed++;
			
			myDrawer.myStackOfPoints = stackOfJonctions;
			
			// Check if we are going back to a stacked point
			if (pointFromStack != null && (currentPoint.forNodeId == pointFromStack.forNodeId))
			{
				System.out.println("Back on previous track "+posOnWay);
				
				myDrawer.myInfoText.line3 = "Back on previous track "+posOnWay;
				
				currentDirection = pointFromStack.directionOfPass;
				
				currentWay = pointFromStack.belongingTo;
				currentWay.partiallyPassed++;
				currentPoint = pointFromStack;
				currentPoint.nbOfTimePassed++;
				posOnWay=currentPoint.atPos;
				// Don't use it anymore
				pointFromStack = null;
			}
			
			myDrawer.myInfoText.variableText.clear();
			myDrawer.myInfoPoints.pointsToShow.clear();
			
			// Check if there are points from the current point, no already used, and follow them if in the right direction 
			if (currentPoint.nbIntersect > 0)
			{		
				myDrawer.myInfoText.line2 = "Intersecting "+currentPoint.nbIntersect;
				
				boolean newWay = false;
				// Find the other intersecting points!
				NodeWithPoints currentNode = allNodesId.get(currentPoint.forNodeId);
				// Now check all others points sharing the same node
				
				// 2nd pass, find the non-used intersections
				boolean isWay = false;
				// Save the direction in case we go somewhere else, it will be stacked
				int lastDirection = currentDirection;
				
				MarkableWay chosenWay = currentWay;
				MarkablePoint chosenPoint = currentPoint;
				
				for (int onePos : currentNode.forMarkablePoint)
				{
					// Check if already used
					MarkablePoint intersectingPoint = allFoundPoint.get(onePos);
					
					// For all *other* nodes
					// NEEDED ????
					if (intersectingPoint.belongingTo.ID != currentPoint.belongingTo.ID)
					{
						
						MarkableWay intersectingWay = intersectingPoint.belongingTo;
						
						boolean canGoThisWay = false;
						
						{
							myDrawer.myInfoText.variableText.add("Intersection "+onePos+" might be valid, used "+intersectingPoint.nbOfTimePassed+" time");
							
							// Check if the point is not the last in its way and if the next point has been used or not
							if ((intersectingWay.myPoints.size() - 1) > intersectingPoint.atPos)
							{
								System.out.println("A");
								// Find the next point in that direction
								MarkablePoint nextPointThere = allFoundPoint.get(onePos + 1);

								if (nextPointThere.nbOfTimePassed == 0)
								{
									System.out.println("A OK");
									if (!isWay)
									{
										isWay = true;
										currentDirection=1;
										chosenWay = intersectingWay;
										chosenPoint = intersectingPoint;
										
										CoordInfo myNewCoord = new CoordInfo();
										myNewCoord.longitude = nextPointThere.longitude;
										myNewCoord.latitude = nextPointThere.latitude;
										myNewCoord.setColor(0, 250, 0);
										
										myDrawer.myInfoPoints.pointsToShow.add(myNewCoord);
										drawInvestigatedWay(myDrawer, intersectingWay.myPoints, 10, 250, 10);
										break;
									}
									else
									{
										// Available for next time
										nbOfJonctionsAvailable+= 1;
									}
								}
							}
							else
							{
								// Dead end
								System.out.println("A NOK");
								intersectingPoint.nbOfTimePassed++;
							}
							// Did not work in one direction, check the other
							if ( intersectingPoint.atPos > 0)
							{
								System.out.println("B");
								MarkablePoint nextPointThere = allFoundPoint.get(onePos - 1);

								if (nextPointThere.nbOfTimePassed == 0)
								{
									System.out.println("B OK");
									if (!isWay)
									{
										isWay = true;
										currentDirection=-1;
										chosenWay = intersectingWay;
										chosenPoint = intersectingPoint;
										
										CoordInfo myNewCoord = new CoordInfo();
										myNewCoord.longitude = nextPointThere.longitude;
										myNewCoord.latitude = nextPointThere.latitude;
										myNewCoord.setColor(0, 0, 250);
										
										myDrawer.myInfoPoints.pointsToShow.add(myNewCoord );
										drawInvestigatedWay(myDrawer, intersectingWay.myPoints, 10, 10, 250);
										break;
									}
									else
									{
										// Available for next time
										nbOfJonctionsAvailable+= 1;
									}
								}
							}
							else
							{
								// Dead end
								System.out.println("B NOK");
								intersectingPoint.nbOfTimePassed++;								
							}
							if (!isWay)
							{
								System.out.println("Intersection already fully used, point "+intersectingPoint.forNodeId);
								myDrawer.myFloatingInfo.setColor(40, 20, 10);
								myDrawer.myInfoText.variableText.add("Intersection already fully used, point "+intersectingPoint.forNodeId);

								CoordInfo myNewCoord = new CoordInfo();
								myNewCoord.longitude = intersectingPoint.longitude;
								myNewCoord.latitude = intersectingPoint.latitude;
								myNewCoord.setColor(0, 0, 0);

								myDrawer.myInfoPoints.pointsToShow.add(myNewCoord );
								drawInvestigatedWay(myDrawer, intersectingWay.myPoints, 0, 0 ,0);
							}
						}
					}				
				}
				if (isWay)
				{
					myDrawer.myFloatingInfo.text = "New way";
					myDrawer.myFloatingInfo.setColor(250, 20, 10);
					
					currentPoint.directionOfPass = lastDirection;
					
					// Check if we can go back using the source way as well,
					// in that case we need to increase the nbOfJunctionsAvailable
					// The navigation using nbOfTimePassed should assure we come back
					
					// Needed ??? When back from a dead-end, we might simply continue forward...
					if (lastDirection == 1)
					{
						// What about the next point
						if ((currentWay.myPoints.size() - 1) > currentPoint.atPos)
						{
							System.out.println("A++");
							// Find the next point in that direction
							MarkablePoint nextPointThere = currentWay.myPoints.get(currentPoint.atPos + 1);

							if (nextPointThere.nbOfTimePassed == 0)
							{
								nbOfJonctionsAvailable+= 1;
								System.out.println("Can go forward next time!");
								myDrawer.myInfoText.line3 = "Can go forward when back";
							}
						}
					}
					else
					{
						if ( currentPoint.atPos > 0)
						{
							System.out.println("B++");
							MarkablePoint nextPointThere = currentWay.myPoints.get(currentPoint.atPos -1);

							if (nextPointThere.nbOfTimePassed == 0)
							{
								// Available for next time
								nbOfJonctionsAvailable+= 1;
								System.out.println("Can go backward next time!");
								myDrawer.myInfoText.line3 = "Can go backward when back";
							}
						}
					}
					stackOfJonctions.myPoints.push(currentPoint);
					maxDepthOfStack = checkIfMaxDepth(maxDepthOfStack, stackOfJonctions);
					// Ok to go there
					currentWay = chosenWay;
					currentWay.partiallyPassed++;
					currentPoint = chosenPoint;
					currentPoint.nbOfTimePassed++;
					// And at a new position
					posOnWay=currentPoint.atPos;
					newWay = true;
				}
				if (!newWay)
				{
					System.out.println("No intersection valid anymore, continuing");
					
					myDrawer.myFloatingInfo.text = "No new way";
					myDrawer.myFloatingInfo.setColor(0, 0, 10);
					
					myDrawer.myInfoText.line3 = "No intersection valid anymore, continuing";
				}
			}
			else
			{
				myDrawer.myFloatingInfo.text = "Same way";
				myDrawer.myFloatingInfo.setColor(20, 20, 250);
			}
			
			// Check if we can continue.
			boolean wentFurther = false;
			
			if (currentDirection > 0)
			{
				System.out.println("C on "+currentPoint.atPos +" at " + currentWay.myPoints.size()+ "("+posOnWay+")");
				if (currentPoint.atPos < currentWay.myPoints.size() - 1)
				{
					posOnWay = currentPoint.atPos + 1;
					wentFurther = true;
					System.out.println("On our way +1 " + posOnWay);
					myDrawer.myInfoText.line4 = "Forward";
				}
			}
			else if ( currentPoint.atPos > 0)
			{
				posOnWay = currentPoint.atPos - 1;
				wentFurther = true;
				System.out.println("On our way -1 " + posOnWay);
				myDrawer.myInfoText.line4 = "Back";
			}
			// If not, we un-stack if there is something on the stack
			
			if (!wentFurther)
			{
				// Not matching !!! isEmty works better than >0...
				if (!stackOfJonctions.myPoints.isEmpty()) // (nbOfJonctionsAvailable > 0)//(!stackOfJonctions.myPoints.isEmpty())
				{
					System.out.println("Still one way " + nbOfJonctionsAvailable);
					// Try to backtrack
					pointFromStack = stackOfJonctions.myPoints.pop();
					
					nbOfJonctionsAvailable--;
					//currentPoint = stackOfJonctions.myPoints.pop();
					currentDirection = -currentDirection; // On the other direction
//					posOnWay = currentPoint.atPos;
//					currentWay = currentPoint.belongingTo;
//					
//					System.out.println("Poped an intersection " + currentPoint.forNodeId+" at "+posOnWay);
				}
				else
				{
					// Stuck???
					canContinue = false;
					
					// Find the next unused way
					for (MarkableWay oneWay : allFoundWays)
					{
						if (oneWay.partiallyPassed == 0) // Should that be on fullyPassed, and check otherwise for the first unpassed point?
						{
							currentWay = oneWay;
							currentWay.partiallyPassed = 1;
							currentPoint = oneWay.myPoints.get(0);
							posOnWay = 0;
							currentDirection = 1;
							
							canContinue = true;
							
							System.out.println("Jumped to new way " + currentPoint.forNodeId);
							
							break;
						}
					}
				}
				
				// If enough done, stop
				canContinue = checkIfEnoughCovered(allFoundPoint, 98);
			}
			try {
				if (inPauseMode)
				{
					while (paused)
					{
						Thread.sleep(1000);
					}
					paused = true;
				}
				else
				{
					Thread.sleep(MS_FOR_NO_PAUSE);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myDrawer.updateCurrentProcess((MarkablePoint[]) allNodesAlreadyUsed.myPoints.toArray(new MarkablePoint[allNodesAlreadyUsed.myPoints.size()]));
		}
		
		int maxNbOfPassage = 0;
		int totalNbOfPassage = 0;
		float totalNbOfPassageByWay = 0;
		float avgNbOfPassage = 0;
		int nbOfWay = allFoundWays.size();
		int nbOfPointInWays = 0;
		float ratioTotalNbOfPointOnWayPoint;
		
		for (MarkableWay oneWay : allFoundWays)
		{
			for (MarkablePoint onePoint : oneWay.myPoints)
			{
				if (onePoint.nbOfTimePassed > maxNbOfPassage)
				{
					maxNbOfPassage = onePoint.nbOfTimePassed;
				}
				totalNbOfPassage += onePoint.nbOfTimePassed;
				nbOfPointInWays++;
			}
		}
		totalNbOfPassageByWay = ((float )totalNbOfPassage) / ((float )nbOfWay);
		avgNbOfPassage = ((float )totalNbOfPassage) / ((float )nbOfPointInWays);
		ratioTotalNbOfPointOnWayPoint = ((float )allNodesAlreadyUsed.myPoints.size()) / ((float )nbOfPointInWays);
		
		System.out.println("Max depth of stack "+maxDepthOfStack);
		System.out.println("Max nb of passage "+maxNbOfPassage);
		System.out.println("Total nb of passage "+totalNbOfPassage);
		System.out.println("Total nb of passage by way "+totalNbOfPassageByWay);
		System.out.println("Average nb of passage "+avgNbOfPassage);
		System.out.println("Used "+ratioTotalNbOfPointOnWayPoint+" time as many points as they were");
		
		// Now we save it
		GeneratedPath oneNewPath = new GeneratedPath();
		oneNewPath.score = ratioTotalNbOfPointOnWayPoint;
		
		allGeneratedPath.add(oneNewPath);
		
		for (MarkablePoint onePoint: allNodesAlreadyUsed.myPoints)
		{
			oneNewPath.allPoint.add(onePoint);
		}
		
		return ratioTotalNbOfPointOnWayPoint;
	}
	
	private float processWaysDeepDepthWRandom(ArrayList<MarkablePoint> allFoundPoint, ArrayList<MarkableWay> allFoundWays, HashMap<Long,NodeWithPoints> allNodesId, DrawMap4 myDrawer) {
		int maxDepthOfStack = 0;
		
		boolean canContinue = true;
		MarkableWay currentWay = allFoundWays.get(0);
		currentWay.partiallyPassed = 1;
		
		int posOnWay = 0;
		int currentDirection = 1; // Increasing
		
		// Utility collections
		AllUsedNodes allNodesAlreadyUsed = new AllUsedNodes();
		StackOfJonctionsUsed stackOfJonctions = new StackOfJonctionsUsed();
		MarkablePoint pointFromStack = null; // If it exist, when on it we need to go back there, even if already used
		int nbOfJonctionsAvailable = 0;
		
		resetAllPoints(allFoundPoint);
		
		while (canContinue)
		{
			System.out.println("Point  at "+posOnWay);
			MarkablePoint currentPoint = currentWay.myPoints.get(posOnWay);
			
			myDrawer.myInfoText.line1 = "Point "+currentPoint.forNodeId+" at "+posOnWay+" with at pos "+currentPoint.atPos;
			
			allNodesAlreadyUsed.myPoints.add(currentPoint);
			
			currentPoint.nbOfTimePassed++;
			
			myDrawer.myStackOfPoints = stackOfJonctions;
			
			// Check if we are going back to a stacked point
			if (pointFromStack != null && (currentPoint.forNodeId == pointFromStack.forNodeId))
			{
				System.out.println("Back on previous track "+posOnWay);
				
				myDrawer.myInfoText.line3 = "Back on previous track "+posOnWay;
				
				currentDirection = pointFromStack.directionOfPass;
				
				currentWay = pointFromStack.belongingTo;
				currentWay.partiallyPassed++;
				currentPoint = pointFromStack;
				currentPoint.nbOfTimePassed++;
				posOnWay=currentPoint.atPos;
				// Don't use it anymore
				pointFromStack = null;
			}
			
			myDrawer.myInfoText.variableText.clear();
			myDrawer.myInfoPoints.pointsToShow.clear();
			
			// Check if there are points from the current point, no already used, and follow them if in the right direction 
			if (currentPoint.nbIntersect > 0)
			{		
				myDrawer.myInfoText.line2 = "Intersecting "+currentPoint.nbIntersect;
				
				boolean newWay = false;
				// Find the other intersecting points!
				NodeWithPoints currentNode = allNodesId.get(currentPoint.forNodeId);
				// Now check all others points sharing the same node
				
				// 2nd pass, find the non-used intersections
				boolean isWay = false;
				// Save the direction in case we go somewhere else, it will be stacked
				int lastDirection = currentDirection;
				
				MarkableWay chosenWay = currentWay;
				MarkablePoint chosenPoint = currentPoint;
				
				// Push all intersecting point in a pool, together with their direction
				ArrayList<MarkablePoint> poolOfPoints = new ArrayList<MarkablePoint>();
				
				for (int onePos : currentNode.forMarkablePoint)
				{
					// Check if already used
					MarkablePoint intersectingPoint = allFoundPoint.get(onePos);
					
					// For all *other* nodes
					// NEEDED ????
					if (intersectingPoint.belongingTo.ID != currentPoint.belongingTo.ID)
					{
						
						MarkableWay intersectingWay = intersectingPoint.belongingTo;
						
						{
							myDrawer.myInfoText.variableText.add("Intersection "+onePos+" might be valid, used "+intersectingPoint.nbOfTimePassed+" time");
							
							// Check if the point is not the last in its way and if the next point has been used or not
							if ((intersectingWay.myPoints.size() - 1) > intersectingPoint.atPos)
							{
								System.out.println("A");
								// Find the next point in that direction
								MarkablePoint nextPointThere = allFoundPoint.get(onePos + 1);

								if (nextPointThere.nbOfTimePassed == 0)
								{
									System.out.println("A OK");

									isWay = true;
									currentDirection=1;
									chosenWay = intersectingWay;
									chosenPoint = intersectingPoint;

									CoordInfo myNewCoord = new CoordInfo();
									myNewCoord.longitude = nextPointThere.longitude;
									myNewCoord.latitude = nextPointThere.latitude;
									myNewCoord.setColor(0, 250, 0);

									myDrawer.myInfoPoints.pointsToShow.add(myNewCoord);
									drawInvestigatedWay(myDrawer, intersectingWay.myPoints, 10, 250, 10);

									MarkablePoint tmpPoint = new MarkablePoint();
									tmpPoint.atPos = intersectingPoint.atPos;
									tmpPoint.directionOfPass = 1;
									tmpPoint.belongingTo = intersectingWay;

									poolOfPoints.add(tmpPoint);

								}
							}
							else
							{
								// Dead end
								System.out.println("A NOK");
								intersectingPoint.nbOfTimePassed++;
							}
							// Did not work in one direction, check the other
							if ( intersectingPoint.atPos > 0)
							{
								System.out.println("B");
								MarkablePoint nextPointThere = allFoundPoint.get(onePos - 1);

								if (nextPointThere.nbOfTimePassed == 0)
								{
									System.out.println("B OK");

									isWay = true;
									currentDirection=-1;
									chosenWay = intersectingWay;
									chosenPoint = intersectingPoint;

									CoordInfo myNewCoord = new CoordInfo();
									myNewCoord.longitude = nextPointThere.longitude;
									myNewCoord.latitude = nextPointThere.latitude;
									myNewCoord.setColor(0, 0, 250);

									myDrawer.myInfoPoints.pointsToShow.add(myNewCoord );
									drawInvestigatedWay(myDrawer, intersectingWay.myPoints, 10, 10, 250);

									MarkablePoint tmpPoint = new MarkablePoint();
									tmpPoint.atPos = intersectingPoint.atPos;
									tmpPoint.directionOfPass = -1;
									tmpPoint.belongingTo = intersectingWay;

									poolOfPoints.add(tmpPoint);

								}
							}
							else
							{
								// Dead end
								System.out.println("B NOK");
								intersectingPoint.nbOfTimePassed++;								
							}
							if (!isWay)
							{
								System.out.println("Intersection already fully used, point "+intersectingPoint.forNodeId);
								myDrawer.myFloatingInfo.setColor(40, 20, 10);
								myDrawer.myInfoText.variableText.add("Intersection already fully used, point "+intersectingPoint.forNodeId);

								CoordInfo myNewCoord = new CoordInfo();
								myNewCoord.longitude = intersectingPoint.longitude;
								myNewCoord.latitude = intersectingPoint.latitude;
								myNewCoord.setColor(0, 0, 0);

								myDrawer.myInfoPoints.pointsToShow.add(myNewCoord );
								drawInvestigatedWay(myDrawer, intersectingWay.myPoints, 0, 0 ,0);
							}
						}
					}				
				}
				if (isWay)
				{
					myDrawer.myFloatingInfo.text = "New way";
					myDrawer.myFloatingInfo.setColor(250, 20, 10);
					
					currentPoint.directionOfPass = lastDirection;
					
					// Check if we can go back using the source way as well,
					// in that case we need to increase the nbOfJunctionsAvailable
					// The navigation using nbOfTimePassed should assure we come back
					
					// Needed ??? When back from a dead-end, we might simply continue forward...
					if (lastDirection == 1)
					{
						// What about the next point
						if ((currentWay.myPoints.size() - 1) > currentPoint.atPos)
						{
							System.out.println("A++");
							// Find the next point in that direction
							MarkablePoint nextPointThere = currentWay.myPoints.get(currentPoint.atPos + 1);

							if (nextPointThere.nbOfTimePassed == 0)
							{
								nbOfJonctionsAvailable+= 1;
								System.out.println("Can go forward next time!");
								myDrawer.myInfoText.line3 = "Can go forward when back";
							}
						}
					}
					else
					{
						if ( currentPoint.atPos > 0)
						{
							System.out.println("B++");
							MarkablePoint nextPointThere = currentWay.myPoints.get(currentPoint.atPos -1);

							if (nextPointThere.nbOfTimePassed == 0)
							{
								// Available for next time
								nbOfJonctionsAvailable+= 1;
								System.out.println("Can go backward next time!");
								myDrawer.myInfoText.line3 = "Can go backward when back";
							}
						}
					}
					stackOfJonctions.myPoints.push(currentPoint);
					maxDepthOfStack = checkIfMaxDepth(maxDepthOfStack, stackOfJonctions);
					
					// From all the possible direction, chose one.
					// Now from the pool select one point
					Random oneRandomNumber = new Random();
					int selectedPointPos = oneRandomNumber.nextInt(poolOfPoints.size());
					
					myDrawer.myInfoText.variableText.add("Got "+poolOfPoints.size()+" choices");

					MarkablePoint tmpPoint = poolOfPoints.get(selectedPointPos);
					currentDirection=tmpPoint.directionOfPass;
					chosenWay = tmpPoint.belongingTo;
					chosenPoint = chosenWay.myPoints.get(tmpPoint.atPos);
					
					// Ok to go there
					currentWay = chosenWay;
					currentWay.partiallyPassed++;
					currentPoint = chosenPoint;
					currentPoint.nbOfTimePassed++;
					// And at a new position
					posOnWay=currentPoint.atPos;
					newWay = true;
				}
				if (!newWay)
				{
					System.out.println("No intersection valid anymore, continuing");
					
					myDrawer.myFloatingInfo.text = "No new way";
					myDrawer.myFloatingInfo.setColor(0, 0, 10);
					
					myDrawer.myInfoText.line3 = "No intersection valid anymore, continuing";
				}
			}
			else
			{
				myDrawer.myFloatingInfo.text = "Same way";
				myDrawer.myFloatingInfo.setColor(20, 20, 250);
			}
			
			// Check if we can continue.
			boolean wentFurther = false;
			
			if (currentDirection > 0)
			{
				System.out.println("C on "+currentPoint.atPos +" at " + currentWay.myPoints.size()+ "("+posOnWay+")");
				if (currentPoint.atPos < currentWay.myPoints.size() - 1)
				{
					posOnWay = currentPoint.atPos + 1;
					wentFurther = true;
					System.out.println("On our way +1 " + posOnWay);
					myDrawer.myInfoText.line4 = "Forward";
				}
			}
			else if ( currentPoint.atPos > 0)
			{
				posOnWay = currentPoint.atPos - 1;
				wentFurther = true;
				System.out.println("On our way -1 " + posOnWay);
				myDrawer.myInfoText.line4 = "Back";
			}
			// If not, we un-stack if there is something on the stack
			
			if (!wentFurther)
			{
				// Not matching !!! isEmty works better than >0...
				if (!stackOfJonctions.myPoints.isEmpty()) // (nbOfJonctionsAvailable > 0)//(!stackOfJonctions.myPoints.isEmpty())
				{
					System.out.println("Still one way " + nbOfJonctionsAvailable);
					// Try to backtrack
					pointFromStack = stackOfJonctions.myPoints.pop();
					
					nbOfJonctionsAvailable--;
					//currentPoint = stackOfJonctions.myPoints.pop();
					currentDirection = -currentDirection; // On the other direction
//					posOnWay = currentPoint.atPos;
//					currentWay = currentPoint.belongingTo;
//					
//					System.out.println("Poped an intersection " + currentPoint.forNodeId+" at "+posOnWay);
				}
				else
				{
					// Stuck???
					canContinue = false;
					
					// Find the next unused way
//					for (MarkableWay oneWay : allFoundWays)
//					{
//						if (oneWay.partiallyPassed == 0) // Should that be on fullyPassed, and check otherwise for the first unpassed point?
//						{
//							currentWay = oneWay;
//							currentWay.partiallyPassed = 1;
//							currentPoint = oneWay.myPoints.get(0);
//							posOnWay = 0;
//							currentDirection = 1;
//							
//							canContinue = true;
//							
//							System.out.println("Jumped to new way " + currentPoint.forNodeId);
//							
//							break;
//						}
//					}
				}
				
				// If enough done, stop
				// Using this condition has a *huge* influence of the size...
				// As opposed as trying to be perfect as above.
				// But if the percentage is too high, we might be stuck forever (so that should be prevented)
				// -> stop if no change of nb of point.
				canContinue = checkIfEnoughCovered(allFoundPoint, 99);
			}
			try {
				if (inPauseMode)
				{
					while (paused)
					{
						Thread.sleep(1000);
					}
					paused = true;
				}
				else
				{
					Thread.sleep(MS_FOR_NO_PAUSE);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myDrawer.updateCurrentProcess((MarkablePoint[]) allNodesAlreadyUsed.myPoints.toArray(new MarkablePoint[allNodesAlreadyUsed.myPoints.size()]));
		}
		
		int maxNbOfPassage = 0;
		int totalNbOfPassage = 0;
		float totalNbOfPassageByWay = 0;
		float avgNbOfPassage = 0;
		int nbOfWay = allFoundWays.size();
		int nbOfPointInWays = 0;
		float ratioTotalNbOfPointOnWayPoint;
		
		for (MarkableWay oneWay : allFoundWays)
		{
			for (MarkablePoint onePoint : oneWay.myPoints)
			{
				if (onePoint.nbOfTimePassed > maxNbOfPassage)
				{
					maxNbOfPassage = onePoint.nbOfTimePassed;
				}
				totalNbOfPassage += onePoint.nbOfTimePassed;
				nbOfPointInWays++;
			}
		}
		totalNbOfPassageByWay = ((float )totalNbOfPassage) / ((float )nbOfWay);
		avgNbOfPassage = ((float )totalNbOfPassage) / ((float )nbOfPointInWays);
		ratioTotalNbOfPointOnWayPoint = ((float )allNodesAlreadyUsed.myPoints.size()) / ((float )nbOfPointInWays);
		
		System.out.println("Max depth of stack "+maxDepthOfStack);
		System.out.println("Max nb of passage "+maxNbOfPassage);
		System.out.println("Total nb of passage "+totalNbOfPassage);
		System.out.println("Total nb of passage by way "+totalNbOfPassageByWay);
		System.out.println("Average nb of passage "+avgNbOfPassage);
		System.out.println("Used "+ratioTotalNbOfPointOnWayPoint+" time as many points as they were");
		
		// Now we save it
		GeneratedPath oneNewPath = new GeneratedPath();
		oneNewPath.score = ratioTotalNbOfPointOnWayPoint;

		allGeneratedPath.add(oneNewPath);

		for (MarkablePoint onePoint: allNodesAlreadyUsed.myPoints)
		{
			oneNewPath.allPoint.add(onePoint);
		}
		
		return ratioTotalNbOfPointOnWayPoint;
	}
	/**
	 * Go randomly through the intersections. Used one are still used, but with penality. Process end when most (all ?) points are used.
	 * @param allFoundPoint
	 * @param allFoundWays
	 * @param allNodesId
	 * @param myGPXCreator
	 * @param myDrawer
	 * @return 
	 */
	private float processWaysRandom(ArrayList<MarkablePoint> allFoundPoint, ArrayList<MarkableWay> allFoundWays, HashMap<Long,NodeWithPoints> allNodesId, DrawMap4 myDrawer) {
		int maxDepthOfStack = 0;
		
		boolean canContinue = true;
		MarkableWay currentWay = allFoundWays.get(0);
		currentWay.partiallyPassed = 1;
		
		int posOnWay = 0;
		int currentDirection = 1; // Increasing
		
		// Utility collections
		AllUsedNodes allNodesAlreadyUsed = new AllUsedNodes();
		StackOfJonctionsUsed stackOfJonctions = new StackOfJonctionsUsed();
		MarkablePoint pointFromStack = null; // If it exist, when on it we need to go back there, even if already used
		int nbOfJonctionsAvailable = 0;
		
		resetAllPoints(allFoundPoint);
		
		while (canContinue)
		{
			System.out.println("Point  at "+posOnWay);
			MarkablePoint currentPoint = currentWay.myPoints.get(posOnWay);
			
			myDrawer.myInfoText.line1 = "Point "+currentPoint.forNodeId+" at "+posOnWay+" with at pos "+currentPoint.atPos;
			
			allNodesAlreadyUsed.myPoints.add(currentPoint);
			
			currentPoint.nbOfTimePassed++;
			
			myDrawer.myStackOfPoints = stackOfJonctions;
			
			
			myDrawer.myInfoText.variableText.clear();
			myDrawer.myInfoPoints.pointsToShow.clear();

			// Check if we are going back to a stacked point
			if (pointFromStack != null && (currentPoint.forNodeId == pointFromStack.forNodeId))
			{
				System.out.println("Back on previous track "+posOnWay);
				
				myDrawer.myInfoText.line3 = "Back on previous track "+posOnWay;
				
				currentDirection = pointFromStack.directionOfPass;
				
				currentWay = pointFromStack.belongingTo;
				currentWay.partiallyPassed++;
				currentPoint = pointFromStack;
				currentPoint.nbOfTimePassed++;
				posOnWay=currentPoint.atPos;
				// Don't use it anymore
				pointFromStack = null;
			}
			// Check if there are points from the current point, no already used, and follow them if in the right direction 
			else if (currentPoint.nbIntersect > 0)
			{		
				myDrawer.myInfoText.line2 = "Intersecting "+currentPoint.nbIntersect;
				
				int lastDirection = currentDirection;
				
				boolean newWay = false;
				// Find the other intersecting points!
				NodeWithPoints currentNode = allNodesId.get(currentPoint.forNodeId);
				// Now check all others points sharing the same node
				
				// Push all intersecting point in a pool, together with their direction
				ArrayList<MarkablePoint> poolOfPoints = new ArrayList<MarkablePoint>();
				
				myDrawer.myInfoText.variableText.add("Having "+currentNode.forMarkablePoint.size()+" intersections");
				
				// Give them a chance to be found inverse to the nb of passage already done
				for (int onePos : currentNode.forMarkablePoint)
				{
					// Check if already used
					MarkablePoint intersectingPoint = allFoundPoint.get(onePos);
					
					// For all *other* nodes
					// NEEDED ????
					if (intersectingPoint.belongingTo.ID != currentPoint.belongingTo.ID)
					{
						
						MarkableWay intersectingWay = intersectingPoint.belongingTo;
						
						boolean canGoThisWay = false;
						
						{
							myDrawer.myInfoText.variableText.add("Intersection "+onePos+" might be valid, used "+intersectingPoint.nbOfTimePassed+" time");
							
							// Check if the point is not the last in its way and if the next point has been used or not
							if ((intersectingWay.myPoints.size() - 1) > intersectingPoint.atPos)
							{		
								System.out.println("A");
								// Find the next point in that direction
								MarkablePoint nextPointThere = allFoundPoint.get(onePos + 1);
								myDrawer.myInfoText.variableText.add("Forward ? "+ nextPointThere.nbOfTimePassed);	
								
								int nbOfAdd = 1000 - nextPointThere.nbOfTimePassed*nextPointThere.nbOfTimePassed;
								
								MarkablePoint tmpPoint = new MarkablePoint();
								tmpPoint.atPos = intersectingPoint.atPos;
								tmpPoint.directionOfPass = 1;
								tmpPoint.belongingTo = intersectingWay;

								for (int iAdd = 0 ; iAdd < nbOfAdd; ++iAdd)
								{
									poolOfPoints.add(tmpPoint);
								}
								myDrawer.myInfoText.variableText.add("Added "+nbOfAdd+" times");
							}
							// Did not work in one direction, check the other
							if ( intersectingPoint.atPos > 0)
							{
								System.out.println("B");
								MarkablePoint nextPointThere = allFoundPoint.get(onePos - 1);

								myDrawer.myInfoText.variableText.add("Back ? "+ nextPointThere.nbOfTimePassed);	
								
								int nbOfAdd = 1000 - nextPointThere.nbOfTimePassed*nextPointThere.nbOfTimePassed;
								
								MarkablePoint tmpPoint = new MarkablePoint();
								tmpPoint.atPos = intersectingPoint.atPos;
								tmpPoint.directionOfPass = -1;
								tmpPoint.belongingTo = intersectingWay;

								for (int iAdd = 0 ; iAdd < nbOfAdd; ++iAdd)
								{
									poolOfPoints.add(tmpPoint);
								}
								myDrawer.myInfoText.variableText.add("- Added "+nbOfAdd+" times");
							}
						}
					}
				}
						
				// 2nd pass, find the non-used intersections
				boolean isWay = false;
				
				if (!poolOfPoints.isEmpty())
				{

					// Now from the pool select one point
					Random oneRandomNumber = new Random();
					int selectedPointPos = oneRandomNumber.nextInt(poolOfPoints.size());

					MarkableWay chosenWay = currentWay;
					MarkablePoint chosenPoint = currentPoint;

					MarkablePoint tmpPoint = poolOfPoints.get(selectedPointPos);
					currentDirection=tmpPoint.directionOfPass;
					chosenWay = tmpPoint.belongingTo;
					chosenPoint = chosenWay.myPoints.get(tmpPoint.atPos);
					
					CoordInfo myNewCoord = new CoordInfo();
					myNewCoord.longitude = chosenPoint.longitude;
					myNewCoord.latitude = chosenPoint.latitude;
					myNewCoord.setColor(0, 0, 250);
					
					myDrawer.myInfoPoints.pointsToShow.add(myNewCoord );
					drawInvestigatedWay(myDrawer, chosenWay.myPoints, 10, 10, 250);
			
					
					myDrawer.myFloatingInfo.text = "New way";
					myDrawer.myFloatingInfo.setColor(250, 20, 10);
					
					currentPoint.directionOfPass = lastDirection;
					
					// Check if we can go back using the source way as well,
					// in that case we need to increase the nbOfJunctionsAvailable
					// The navigation using nbOfTimePassed should assure we come back
					
					// Needed ??? When back from a dead-end, we might simply continue forward...
					if (lastDirection == 1)
					{
						// What about the next point
						if ((currentWay.myPoints.size() - 1) > currentPoint.atPos)
						{
							System.out.println("A++");
							// Find the next point in that direction
							MarkablePoint nextPointThere = currentWay.myPoints.get(currentPoint.atPos + 1);

							if (nextPointThere.nbOfTimePassed == 0)
							{
								nbOfJonctionsAvailable+= 1;
								System.out.println("Can go forward next time!");
								myDrawer.myInfoText.line3 = "Can go forward when back";
							}
						}
					}
					else
					{
						if ( currentPoint.atPos > 0)
						{
							System.out.println("B++");
							MarkablePoint nextPointThere = currentWay.myPoints.get(currentPoint.atPos -1);

							if (nextPointThere.nbOfTimePassed == 0)
							{
								// Available for next time
								nbOfJonctionsAvailable+= 1;
								System.out.println("Can go backward next time!");
								myDrawer.myInfoText.line3 = "Can go backward when back";
							}
						}
					}
					stackOfJonctions.myPoints.push(currentPoint);
					maxDepthOfStack = checkIfMaxDepth(maxDepthOfStack, stackOfJonctions);
					// Ok to go there
					currentWay = chosenWay;
					currentWay.partiallyPassed++;
					currentPoint = chosenPoint;
					currentPoint.nbOfTimePassed++;
					// And at a new position
					posOnWay=currentPoint.atPos;
					newWay = true;
				}
				if (!newWay)
				{
					System.out.println("No intersection valid anymore, continuing");
					
					myDrawer.myFloatingInfo.text = "No new way";
					myDrawer.myFloatingInfo.setColor(0, 0, 10);
					
					myDrawer.myInfoText.line3 = "No intersection valid anymore, continuing";
				}
			}
			else
			{
				myDrawer.myFloatingInfo.text = "Same way";
				myDrawer.myFloatingInfo.setColor(20, 20, 250);
			}
			
			// Check if we can continue.
			boolean wentFurther = false;
			
			if (currentDirection > 0)
			{
				System.out.println("C on "+currentPoint.atPos +" at " + currentWay.myPoints.size()+ "("+posOnWay+")");
				if (currentPoint.atPos < currentWay.myPoints.size() - 1)
				{
					posOnWay = currentPoint.atPos + 1;
					wentFurther = true;
					System.out.println("On our way +1 " + posOnWay);
					myDrawer.myInfoText.line4 = "Forward";
				}
			}
			else if ( currentPoint.atPos > 0)
			{
				posOnWay = currentPoint.atPos - 1;
				wentFurther = true;
				System.out.println("On our way -1 " + posOnWay);
				myDrawer.myInfoText.line4 = "Back";
			}
			// If not, we un-stack if there is something on the stack
			
			if (!wentFurther)
			{
				// Not matching !!! isEmpty works better than >0...
				if (!stackOfJonctions.myPoints.isEmpty()) // (nbOfJonctionsAvailable > 0)//(!stackOfJonctions.myPoints.isEmpty())
				{
					System.out.println("Still one way " + nbOfJonctionsAvailable);
					// Try to backtrack
					pointFromStack = stackOfJonctions.myPoints.pop();
					
					nbOfJonctionsAvailable--;
					//currentPoint = stackOfJonctions.myPoints.pop();
					currentDirection = -currentDirection; // On the other direction
//					posOnWay = currentPoint.atPos;
//					currentWay = currentPoint.belongingTo;
//					
//					System.out.println("Poped an intersection " + currentPoint.forNodeId+" at "+posOnWay);
				}
				else
				{
					// Stuck???
					canContinue = false;
					
					// Find the next unused way
					for (MarkableWay oneWay : allFoundWays)
					{
						if (oneWay.partiallyPassed == 0) // Should that be on fullyPassed, and check otherwise for the first unpassed point?
						{
							currentWay = oneWay;
							currentWay.partiallyPassed = 1;
							currentPoint = oneWay.myPoints.get(0);
							posOnWay = 0;
							currentDirection = 1;
							
							canContinue = true;
							
							System.out.println("Jumped to new way " + currentPoint.forNodeId);
							
							break;
						}
					}
				}
				canContinue = checkIfEnoughCovered(allFoundPoint, 95);
				
			}
			try {
				if (inPauseMode)
				{
					while (paused)
					{
						Thread.sleep(1000);
					}
					paused = true;
				}
				else
				{
					Thread.sleep(MS_FOR_NO_PAUSE);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myDrawer.updateCurrentProcess((MarkablePoint[]) allNodesAlreadyUsed.myPoints.toArray(new MarkablePoint[allNodesAlreadyUsed.myPoints.size()]));
		}
		
		int maxNbOfPassage = 0;
		int totalNbOfPassage = 0;
		float totalNbOfPassageByWay = 0;
		float avgNbOfPassage = 0;
		int nbOfWay = allFoundWays.size();
		int nbOfPointInWays = 0;
		float ratioTotalNbOfPointOnWayPoint;
		
		for (MarkableWay oneWay : allFoundWays)
		{
			for (MarkablePoint onePoint : oneWay.myPoints)
			{
				if (onePoint.nbOfTimePassed > maxNbOfPassage)
				{
					maxNbOfPassage = onePoint.nbOfTimePassed;
				}
				totalNbOfPassage += onePoint.nbOfTimePassed;
				nbOfPointInWays++;
			}
		}
		totalNbOfPassageByWay = ((float )totalNbOfPassage) / ((float )nbOfWay);
		avgNbOfPassage = ((float )totalNbOfPassage) / ((float )nbOfPointInWays);
		ratioTotalNbOfPointOnWayPoint = ((float )allNodesAlreadyUsed.myPoints.size()) / ((float )nbOfPointInWays);
		
		System.out.println("Max depth of stack "+maxDepthOfStack);
		System.out.println("Max nb of passage "+maxNbOfPassage);
		System.out.println("Total nb of passage "+totalNbOfPassage);
		System.out.println("Total nb of passage by way "+totalNbOfPassageByWay);
		System.out.println("Average nb of passage "+avgNbOfPassage);
		System.out.println("Used "+ratioTotalNbOfPointOnWayPoint+" time as many points as they were");
		
		// Now we save it
		GeneratedPath oneNewPath = new GeneratedPath();
		oneNewPath.score = ratioTotalNbOfPointOnWayPoint;

		allGeneratedPath.add(oneNewPath);

		for (MarkablePoint onePoint: allNodesAlreadyUsed.myPoints)
		{
			oneNewPath.allPoint.add(onePoint);
		}
		
		return ratioTotalNbOfPointOnWayPoint;
	}

	int lastPointUsed = 0;
	int stuckFor = 0;
	
	static final int MAX_TIME_STUCK = 100;
	private ArrayList<MarkablePoint> decimatedPath;
	
	private boolean checkIfEnoughCovered(ArrayList<MarkablePoint> allFoundPoint, int afterWhichPercentage) {
		boolean canContinue;
		int nbOfPointUsed = 0;
		// Check if most points have been used
		for (MarkablePoint onePoint : allFoundPoint)
		{
			if (onePoint.nbOfTimePassed > 0)
			{
				nbOfPointUsed++;
			}
		}
		// No more progress, we stop
		if (lastPointUsed == nbOfPointUsed)
		{
			stuckFor++;
			
			if (stuckFor > MAX_TIME_STUCK)
			{
				return false;
			}
		}
		else
		{
			lastPointUsed = nbOfPointUsed;
			stuckFor = 0;
		}
		System.out.println(nbOfPointUsed +" on " + allFoundPoint.size());
		int percUsed = (100 * nbOfPointUsed) / allFoundPoint.size();
		System.out.println(percUsed +"% of all points used");
		if (percUsed > afterWhichPercentage)
		{
			canContinue = false;
		}
		else
		{
			canContinue = true;
		}
		return canContinue;
	}

	private void resetAllPoints(ArrayList<MarkablePoint> allFoundPoint) {
		// Reset nb of time passed for all points
		for (MarkablePoint onePoint : allFoundPoint)
		{
			onePoint.nbOfTimePassed = 0;
		}
	}

	private int checkIfMaxDepth(int maxDepthOfStack, StackOfJonctionsUsed stackOfJonctions) {
		if (stackOfJonctions.myPoints.size() > maxDepthOfStack)
		{
			maxDepthOfStack = stackOfJonctions.myPoints.size();
		}
		return maxDepthOfStack;
	}
	
	public synchronized void unpause()
	{
		paused = false;
	}

	private void drawInvestigatedWay(DrawMap4 myDrawer, ArrayList<MarkablePoint> myPoints, int r, int g, int b) {
		for (MarkablePoint onePoint : myPoints)
		{
			CoordInfo myNewCoord = new CoordInfo();
			myNewCoord.longitude = onePoint.longitude;
			myNewCoord.latitude = onePoint.latitude;
			myNewCoord.setColor(r, g, b);
			
			myDrawer.myInfoPoints.pointsToShow.add(myNewCoord );
		}
	}
	
	private void drawOnePoint(DrawMap4 myDrawer, MarkablePoint myPoint, int r, int g, int b) {
		CoordInfo myNewCoord = new CoordInfo();
		myNewCoord.longitude = myPoint.longitude;
		myNewCoord.latitude = myPoint.latitude;
		myNewCoord.setColor(r, g, b);

		myDrawer.myInfoPoints.pointsToShow.add(myNewCoord);
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

	public boolean isProcessing() {
		// TODO Auto-generated method stub
		return isProcessing ;
	}

	public void saveSelectedGPX(File selectedFile) {
		myGPXCreator.createDocument();
		
		for (MarkablePoint onePoint: decimatedPath)
		{
			myGPXCreator.addLatLonEle(onePoint.latitude, onePoint.longitude, 0);
		}
		
		myGPXCreator.closeAndSave(selectedFile);
	}
}
