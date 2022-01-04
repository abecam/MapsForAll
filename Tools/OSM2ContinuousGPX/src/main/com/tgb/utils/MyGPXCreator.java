package com.tgb.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyGPXCreator {
	private static Logger logger = LoggerFactory.getLogger(MyGPXCreator.class);
	
	Document document;
	Element root;
	Element track;
	Element trackSeg;
	
	public void createDocument()
	{
		document = DocumentHelper.createDocument();
		root = document.addElement("gpx");
		root.addAttribute("version", "1.1");
		root.addAttribute("creator", "Map2GPX");
		
		// Must be a continuous track, we will need to push all close paths together!
		addNewTrack();
	}
	
	public void addNewTrack()
	{
		track = root.addElement("trk");
		trackSeg = track.addElement("trkseg");
	}
	
	public void addLatLonEle(double lat, double lon, long ele)
	{
		Element oneTrackPoint = trackSeg.addElement("trkpt");

		DecimalFormat df = new DecimalFormat("0.000000");
		// Stupid lines of code to force using "." instead of "," in some locals...
		DecimalFormatSymbols myDecimalFormatSymbols = new DecimalFormatSymbols();
		myDecimalFormatSymbols.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(myDecimalFormatSymbols);
		
		String latTxt = df.format(lat);
		String lonTxt = df.format(lon);
		
		oneTrackPoint.addAttribute("lat", latTxt);
		oneTrackPoint.addAttribute("lon", lonTxt);
	}
	
	public void closeAndSave(String documentName)
	{
		FileWriter out;
		try {
			final OutputFormat format = OutputFormat.createPrettyPrint();
			out = new FileWriter(documentName);
			
			final XMLWriter writer = new XMLWriter(out, format);
			writer.write(document);
			out.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			logger.error("Could not save: ", e);
		}
	}

	public void closeAndSave(File selectedFile) {
		FileWriter out;
		try {
			final OutputFormat format = OutputFormat.createPrettyPrint();
			out = new FileWriter(selectedFile);
			
			final XMLWriter writer = new XMLWriter(out, format);
			writer.write(document);
			out.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			logger.error("Could not save: ", e);
		}
	}
}
