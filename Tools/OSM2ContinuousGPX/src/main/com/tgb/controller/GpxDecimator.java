package com.tgb.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.tgb.gfx.DrawMap4;
import com.tgb.model.MarkablePoint;
import com.tgb.model.MarkableWay;
import com.tgb.model.NodeWithPoints;
import com.tgb.utils.MyGPXCreator;

public class GpxDecimator {
	
	/**
	 * Decimate the points prior to save a GPX following simple rules (that need informations from the OSM file that will be removed from the GPX output):
	 * 
	 * - Does not remove intersecting points,
	 * - If a point is removed, remove all points of the same node (so points at the same place),
	 * - remove point only if the angle of the 2 segments are smaller than a certain value (and we iterate so the angle might get bigger if the curve has many points),
	 * - never remove points at beginning/end of way
	 * 
	 * And with (later ? current interaction with the map is a dirty trick) an interactive interaction:
	 * - with sizable circle, left button to decimate the ways in the circle (following previous rules), right button to restore.
	 * @param allFoundPoint the new list of points
	 * @param allFoundWays the extracted list of points in a way
	 * @param allNodesId all the node with their points, with atPos the position in allFoundPoint.
	 * @param myDrawer
	 */
	ArrayList<MarkablePoint> decimatePoints(int nbOfIteration, ArrayList<MarkablePoint> allFoundPoint, ArrayList<MarkableWay> allFoundWays, DrawMap4 myDrawer, int maxAngle, int incrAngle)
	{
		HashSet<Long> nodesSelected = new HashSet<>();
		
		for (MarkablePoint onePoint : allFoundPoint)
		{
			nodesSelected.add(onePoint.forNodeId);
		}
		
		ArrayList<MarkableWay> currentWays = allFoundWays;
		HashSet<Long> nodesToRemove = new HashSet<>();
		
		for (int iDecimation = 0; iDecimation < nbOfIteration ; iDecimation++)
		{	
			ArrayList<MarkableWay> nextWays = new ArrayList<>();
			
			// For each way, decimate iteratively
			for (MarkableWay oneWay : currentWays)
			{
				// We never remove the first point or the last, nor the intersecting ones
				if (oneWay.myPoints.size() > 2)
				{
					// First eliminate the eventual points not selected
					boolean isPointSelected =  nodesSelected.contains(oneWay.myPoints.get(0));
					boolean waySelected = false;
					int iPoints = 1;
					
					while (!isPointSelected && iPoints < oneWay.myPoints.size() - 1)
					{
						isPointSelected =  nodesSelected.contains(oneWay.myPoints.get(iPoints++).forNodeId);
					}
					System.out.println("We are at "+iPoints+" on "+oneWay.myPoints.size());
					
					// We simply ignore all non-selected ways
					if (iPoints < oneWay.myPoints.size() - 1)
					{
						System.out.println("Starting at "+iPoints);
						boolean endOfWay = false;

						for ( ; !endOfWay; iPoints++)
						{
							System.out.println("Being at "+iPoints+" on "+oneWay.myPoints.size());
							
							MarkablePoint currentPoint = oneWay.myPoints.get(iPoints);
							MarkablePoint nextPoint = oneWay.myPoints.get(iPoints + 1);
							
							if (!nodesSelected.contains(nextPoint.forNodeId))
							{
								// End of the way
								endOfWay = true;
								
								System.out.println("Stoping at "+iPoints+" on "+oneWay.myPoints.size());
							} // And keep it
							// Be sure it is not an intersection and not already removed
							else if (currentPoint.nbIntersect == 0 && !nodesToRemove.contains(currentPoint.forNodeId))
							{
								// Check the angle between the previous and the next
								MarkablePoint previousPoint = oneWay.myPoints.get(iPoints - 1);
								

								if (Math.abs(angleBetween3Points(previousPoint, currentPoint, nextPoint)) < maxAngle)
								{
//									System.out.println("Removing points: "+previousPoint.latitude+ " , "+previousPoint.longitude+
//											" : "+currentPoint.latitude+ " , "+currentPoint.longitude+
//											" : "+nextPoint.latitude+ " , "+nextPoint.longitude+
//											" --- Angle = "+angleBetween3Points(previousPoint, currentPoint, nextPoint));
									nodesToRemove.add(currentPoint.forNodeId);
									// And skip the next point for now
									if (iPoints < oneWay.myPoints.size() - 1)
									{
										iPoints++;
									}
								}
							}
							
							endOfWay = iPoints >= oneWay.myPoints.size() - 2;
						}
						MarkableWay oneNewWayOnlyPoints = new MarkableWay();

						// Only the points are used yet.

						// Then copy the way to a new way for next time...
						for (MarkablePoint onePoint: oneWay.myPoints)
						{
							if (!nodesToRemove.contains(onePoint.forNodeId))
							{
								oneNewWayOnlyPoints.myPoints.add(onePoint);
							}
						}

						nextWays.add(oneNewWayOnlyPoints);
					}
					// Non-iterative version, degrade more the first segments
//					while (somethingToRemove && currentNbOfIteration < nbOfIteration)
//					{
//						MarkablePoint currentPoint = oneWay.myPoints.get(iPoints);
//						int firstPointIndex = iPoints - 1;
//						int nextPointIndex = iPoints + 1;
//						
//						// Be sure it is not an intersection
//						if (currentPoint.nbIntersect == 0)
//						{
//							// Check the angle between the previous and the next
//							
//							MarkablePoint previousPoint = oneWay.myPoints.get(firstPointIndex);
//							
//							MarkablePoint nextPoint = oneWay.myPoints.get(nextPointIndex);
//
//							if (angleBetween3Points(previousPoint, currentPoint, nextPoint) < mayAngle)
//							{
//								nodesToRemove.add(currentPoint.forNodeId);
//								
//								// Still check the next point
//								somethingToRemove = true;
//								currentNbOfIteration++;
//								
//								System.out.println(currentNbOfIteration+" - Next points to check "+firstPointIndex+" : "+iPoints+" : "+nextPointIndex);
//								
//								// See if we can still remove
//								if (iPoints < oneWay.myPoints.size() - 2)
//								{
//									iPoints++;
//									nextPointIndex = iPoints+1;
//								}
//								else if (iPoints < oneWay.myPoints.size() - 1)
//								{
//									iPoints++;
//									somethingToRemove = false;
//								}
//								else
//								{
//									somethingToRemove = false;
//								}
//							}
//							else
//							{
//								somethingToRemove = false;
//							}
//						}
//						else
//						{
//							somethingToRemove = false;
//						}
//					}
				}
			}
			
			currentWays = nextWays;
			maxAngle+=incrAngle;
		}
		
		// Now go through all nodes and remove them.
		// So all point at a node will be removed, and will always be on the same segment, as we ignored intersecting points

		ArrayList<MarkablePoint> decimatedPoints = new ArrayList<MarkablePoint>();

		for (MarkablePoint onePoint : allFoundPoint)
		{
			if (!nodesToRemove.contains(onePoint.forNodeId))
			{
				decimatedPoints.add(onePoint);
			}
		}

		allFoundPoint = decimatedPoints;
		
		return allFoundPoint;
	}
	
	/**
	 * From https://stackoverflow.com/questions/26829161/the-angle-between-3-points-in-c
	 * @param A
	 * @param B
	 * @param C
	 * @return
	 */
	double angleBetween3Points(MarkablePoint A, MarkablePoint B, MarkablePoint C) {
		  double atanA = Math.atan2(A.longitude - B.longitude, A.latitude - B.latitude);
		  double atanC = Math.atan2(C.longitude - B.longitude, C.latitude - B.latitude);
		  double diff = atanC - atanA;

		  if (diff > Math.PI) diff -= Math.PI;
		  else if (diff < -Math.PI) diff += Math.PI;

		  // Convert to degrees if desired.
		  diff *= 180 / Math.PI;

		  return diff;
		} 
}
