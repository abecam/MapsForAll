package com.tgb.gfx;

import javax.swing.*;  // For JPanel, etc.

import com.jhlabs.awt.TextStroke;
import com.tgb.controller.GpxProcessor;
import com.tgb.controller.TypeOfSelect;
import com.tgb.model.MapElement;
import com.tgb.model.MarkablePoint;
import com.tgb.model.StackOfJonctionsUsed;

import java.awt.*;   // For Graphics, etc.
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** 
* Simplified map viewer
 */

public class DrawMap4 extends JLabel implements MouseListener, MouseWheelListener, MouseMotionListener {
	private static final int SHIFT_Y_MOUSE = 0; // 32;
	private static final Color BROWN = new Color(200, 150, 20);
	private static final Color LIGHTER_GREEN_TRSP = new Color(10, 250, 10, 50);
	private static final Color GREEN_TRSP = new Color(0, 255, 0, 200);
	private static final Color GREY_TRSP = new Color(50, 50, 50, 200);
	private static final Color ORANGE_TRSP = new Color(255, 165, 0, 50);
	private static final Color DK_GREY = new Color(50, 50, 50);
	private static final Color BLUE_TRSP = new Color(10, 10, 250, 150);
	
	private static final Color SELECTING_CIRCLE_COLOR = new Color(10, 250, 10, 150);
	
	private static final float SIZE_OF_MAP_FONT = 14.0f;
	private static final BasicStroke Steps_stroke = new BasicStroke(2.0f,                      // Width
	           BasicStroke.CAP_BUTT,    // End cap
	           BasicStroke.JOIN_ROUND,    // Join style
	           10.0f,                     // Miter limit
	           new float[] {2.0f,2.0f}, // Dash pattern
	           0.0f);
	
	private static final BasicStroke Process_stroke = new BasicStroke(2.0f,                      // Width
	           BasicStroke.CAP_BUTT,    // End cap
	           BasicStroke.JOIN_ROUND,    // Join style
	           10.0f,                     // Miter limit
	           new float[] {2.0f,1.0f}, // Dash pattern
	           0.0f);
	
	private static final int IS_AREA = 4;
	private static final double SHIFTX_INITIAL = 600;
	private static final double SHIFTY_INITIAL = 400;
	
	public static final int BARRIER = 2;
	public static final int BUILDING = 8;
	public static final int NATURAL = 16;
	public static final int LANDUSE = 32;
	public static final int BIG_ROAD = 64;
	public static final int SMALL_ROAD = 128;
	public static final int PEDESTRIAN = 256;
	public static final int STEPS = 512;
	public static final int LEISURE = 1024;
	
	int initLon = 0 ; // 8.709666;
    int initLat =  0 ; // 49.383443;

    float zoomFixed = 20000.0f; // Fixed zoom to get things reasonable ok for screen
    float zoom = 1.0f;
    
    double shiftX = SHIFTX_INITIAL ; // 8.709666;
    double shiftY =  SHIFTY_INITIAL ; // 49.383443;
    
    int shiftFrameX = 0;
    int shiftFrameY = 700;

	ArrayList<MapElement> allInfo;
	
	MarkablePoint[] currentProcess; // What the other renderer is currently processing
	
	private boolean showNames = false;
	
	public FloatingInfo myFloatingInfo = new FloatingInfo();
	public InfoText myInfoText = new InfoText();
	public InfoPoints myInfoPoints = new InfoPoints();
	private GpxProcessor myExporter;
	public StackOfJonctionsUsed myStackOfPoints;
	private int mouseSelecting = -1;
	private int zoomCircle = 4;
	private int mouseX = 0;
	private int mouseY = 0;
	
	TypeOfSelect currentTypeOfSelection = TypeOfSelect.POINT_SELECT;
	
	private boolean areMPShifter = true;
	
	JLabel theFrame = null;
	
	public DrawMap4(GpxProcessor exporterGpxMulti) {
		
		// To allow pauses
		myExporter = exporterGpxMulti;
		
		theFrame = this;
	}

	public void paintComponent(Graphics g) 
	{
		clear(g);
		
		this.setSize(this.getParent().getWidth(), this.getParent().getHeight());
		
		int currentWidth = this.getWidth();
		int currentHeight = this.getHeight();
		
		System.out.println("Height "+currentHeight+" and width "+currentWidth);
		
		BufferedImage UnknownImage = new BufferedImage(currentWidth, currentHeight,
				BufferedImage.TYPE_INT_ARGB);
		
		BufferedImage buildingImage = new BufferedImage(currentWidth, currentHeight,
		        BufferedImage.TYPE_INT_ARGB);
		
		BufferedImage roadsImage = new BufferedImage(currentWidth, currentHeight,
		        BufferedImage.TYPE_INT_ARGB);
		
		BufferedImage textImage = new BufferedImage(currentWidth, currentHeight,
		        BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2dUnknown = UnknownImage.createGraphics();
		    
		setGoodQuality(g2dUnknown);
		
        Graphics2D g2dBuilding = buildingImage.createGraphics();
        
        setGoodQuality(g2dBuilding);
        
        Graphics2D g2dRoads = roadsImage.createGraphics();
        
        setGoodQuality(g2dRoads);
        
        Graphics2D g2dText = textImage.createGraphics();
        
        setGoodQuality(g2dText);
        
        Graphics2D g2d = g2dUnknown;

		try {
			// System.out.println("Nb of elements "+allInfo.size());
			// System.out.println("Shifted by "+shiftX+", "+shiftY+" : zoom "+zoom);
			
			long currFlags;
			
			Color polyColor = new Color(10, 10, 10);
			int red = 0;
			int blue = 100;
			int green = 50;

			for (MapElement someElements : allInfo)
			{
				int xPoly[] = new int[someElements.polyOrWay.size()];
				int yPoly[] = new int[someElements.polyOrWay.size()];
				
				String nameOfPart = someElements.name;

				// Flags
				currFlags = someElements.flags;

				int iNodes = 0;
				
				for (MarkablePoint onePoint : someElements.polyOrWay)
				{
					//System.out.println("Found point "+onePoint.longitude+", "+onePoint.latitude);
					//System.out.println("Found point "+(shiftX + onePoint.longitude)+", "+(-shiftY - onePoint.latitude));
					
					xPoly[iNodes] = shiftFrameX + (int )((shiftX + onePoint.longitude)*zoomFixed*zoom);
					yPoly[iNodes] = shiftFrameY + (int )((shiftY - onePoint.latitude)*zoomFixed*zoom);
					
					//System.out.println("Found point "+xPoly[iNodes]+", "+yPoly[iNodes]);
					iNodes++;
				}
				Polygon newPolygon = new Polygon(xPoly, yPoly, iNodes);
				
				boolean isArea = false;
				
				if ((currFlags & IS_AREA) != 0)
				{
					isArea = true;
				}
				// 1 -> highway
				if ((currFlags & 1) != 0)
				{
					g2d = g2dRoads;
					
					g2d.setColor(Color.DARK_GRAY);
					g2d.setStroke(new BasicStroke(4));
					if ((currFlags & BIG_ROAD) != 0)
					{
						g2d.setStroke(new BasicStroke(6));
					}
					else if ((currFlags & SMALL_ROAD) != 0)
					{
						g2d.setStroke(new BasicStroke(4));
					}
					else if ((currFlags & PEDESTRIAN) != 0)
					{
						g2d.setColor(BROWN);
						g2d.setStroke(new BasicStroke(2));
					}
					else if ((currFlags & STEPS) != 0)
					{
						g2d.setColor(BROWN);
						g2d.setStroke(Steps_stroke);
					}
					g2d.drawPolyline(xPoly, yPoly, iNodes);
				}
				else if ((currFlags & BARRIER) != 0)
				{
					g2d = g2dRoads;
					
					g2d.setColor(Color.MAGENTA);
					g2d.setStroke(new BasicStroke(1));
					g2d.drawPolygon(newPolygon);
				}
				else if ((currFlags & BUILDING) != 0)
				{
					g2d = g2dBuilding;
					
					isArea = true;
					
					g2d.setColor(Color.BLACK);
					g2d.setStroke(new BasicStroke(1));
					g2d.drawPolygon(newPolygon);
					
					polyColor = GREY_TRSP;
				}
				else if ((currFlags & NATURAL) != 0)
				{
					g2d = g2dBuilding;
					
					isArea = true;
					
					g2d.setColor(Color.BLACK);
					g2d.setStroke(new BasicStroke(1));
					g2d.drawPolygon(newPolygon);
					
					polyColor = GREEN_TRSP;
				}
				else if ((currFlags & LANDUSE) != 0)
				{
					g2d = g2dBuilding;
					
					isArea = true;
					
					g2d.setColor(Color.BLACK);
					g2d.setStroke(new BasicStroke(1));
					g2d.drawPolygon(newPolygon);
					
					polyColor = LIGHTER_GREEN_TRSP;
				}
				else if ((currFlags & LEISURE) != 0)
				{
					g2d = g2dBuilding;
					
					isArea = true;
					
					g2d.setColor(Color.BLACK);
					g2d.setStroke(new BasicStroke(1));
					g2d.drawPolygon(newPolygon);
					
					polyColor = ORANGE_TRSP;
				}
				else
				{
					g2d = g2dUnknown;
					//isArea = true;
					
					g2d.setColor(Color.BLACK);
					g2d.setStroke(new BasicStroke(1));
					g2d.drawPolygon(newPolygon);	
					polyColor = new Color(red, green, blue, 50);
				}
				
				if (isArea)
				{
					g2d.setColor(polyColor);
		
					g2d.fillPolygon(newPolygon);
				}
				
				if (showNames && nameOfPart != null && !nameOfPart.isEmpty())
				{
					g2d = g2dText;
					
					if (isArea)
					{
						g2d.setColor(Color.BLACK);
						g2d.drawString(nameOfPart, xPoly[0], yPoly[0]);
					}
					else
					{
						g2d.setColor(DK_GREY);
						g2d.setStroke(new TextStroke(nameOfPart, this.getFont(), false, false));
					
						g2d.drawPolyline(xPoly, yPoly, iNodes);
					}
				}
				
				red++;
				if (red > 255)
				{
					red = 0;
					blue++;
					if (blue > 255)
					{
						blue = 0;
						green++;
						if (green > 255)
						{
							green = 0;
						}
					}
				}
			}
			
			if (currentProcess != null)
			{
				int iProcess = currentProcess.length;

				int xProcess[] = new int[iProcess];
				int yProcess[] = new int[iProcess];

				if (iProcess > 0)
				{
					// Draw the processing of the ways
					g2d = g2dRoads;

					g2d.setColor(BLUE_TRSP);
					
					for (int iNodes = 0 ; iNodes < iProcess; iNodes++)
					{
						try
						{
							MarkablePoint onePoint = currentProcess[iNodes];
							//System.out.println("Found point "+onePoint.longitude+", "+onePoint.latitude);
							//System.out.println("Found point "+(shiftX + onePoint.longitude)+", "+(-shiftY - onePoint.latitude));
							int shiftXY = areMPShifter?4:0;

							xProcess[iNodes] = shiftXY + shiftFrameX + (int )((shiftX + onePoint.longitude)*zoomFixed*zoom);
							yProcess[iNodes] = shiftXY + shiftFrameY + (int )((shiftY - onePoint.latitude)*zoomFixed*zoom);

							//System.out.println("Found point "+xProcess[iNodes]+", "+yProcess[iNodes]);
							g2d.drawRect(xProcess[iNodes], yProcess[iNodes], 4, 4);
						}
						catch (Exception e)
						{
							e.printStackTrace();
							// Probably the array list has been changed concurrently...
						}
					}
					
					g2d.setStroke(Process_stroke);
					
					if (myExporter.isProcessing())
					{
						g2d.drawPolyline(xProcess, yProcess, iProcess);
					}
					
					polyColor = ORANGE_TRSP;
				}
				
				// Now show the different info
				g2d.drawString(myInfoText.line1, currentWidth-300, 100);
				g2d.drawString(myInfoText.line2, currentWidth-300, 120);
				g2d.drawString(myInfoText.line3, currentWidth-300, 140);
				g2d.drawString(myInfoText.line4, currentWidth-300, 160);
				
				int iPos = 0;
				
				for (String oneVarLine : myInfoText.variableText)
				{
					iPos++;
					g2d.drawString(oneVarLine, currentWidth-300, 160+iPos*20);
				}
				
				for (CoordInfo oneCoord : myInfoPoints.pointsToShow)
				{
					g2d.setColor(oneCoord.internalColor);
					
					int xInfo = shiftFrameX + (int )((shiftX + oneCoord.longitude)*zoomFixed*zoom);
					int yInfo = shiftFrameY + (int )((shiftY - oneCoord.latitude)*zoomFixed*zoom);
					
					g2d.setStroke(new BasicStroke(2));
					g2d.drawLine(xInfo - 15, yInfo - 15, xInfo + 15, yInfo + 15);
					g2d.drawLine(xInfo - 15, yInfo + 15, xInfo + 15, yInfo - 15);
				}
				
				int xFloatingInfo = 0;
				int yFloatingInfo = 0;
				
				if (currentProcess.length > 0)
				{
					MarkablePoint lastPoint = currentProcess[currentProcess.length - 1];

					xFloatingInfo = 4 + shiftFrameX + (int )((shiftX + lastPoint.longitude)*zoomFixed*zoom);
					yFloatingInfo = 4 + shiftFrameY + (int )((shiftY - lastPoint.latitude)*zoomFixed*zoom);

					g2d.setColor(myFloatingInfo.internalColor);
					g2d.drawString(myFloatingInfo.text, xFloatingInfo + 10, yFloatingInfo);
				}
				// And show the current stack
				if (myStackOfPoints != null)
				{
					int iPoint = 0;

					for (iPoint = 0; iPoint < myStackOfPoints.myPoints.size(); iPoint++)
					{
						MarkablePoint oneMPoint = myStackOfPoints.myPoints.elementAt(iPoint);

						xFloatingInfo = 4 + shiftFrameX + (int )((shiftX + oneMPoint.longitude)*zoomFixed*zoom);
						yFloatingInfo = 4 + shiftFrameY + (int )((shiftY - oneMPoint.latitude)*zoomFixed*zoom);

						g2d.setColor(Color.black);
						g2d.drawString(" - "+iPoint+" - ", xFloatingInfo + 10, yFloatingInfo);
					}
				}
			}
			
			// Finally draw the selecting circle
			g2d = g2dText;
			
			PointerInfo pointerInfo = MouseInfo.getPointerInfo();
			
			g2d.setColor(SELECTING_CIRCLE_COLOR);

			// TODO: Why is it not aligned on the Y axe !?!
			g2d.fillOval(mouseX-zoomCircle/2, mouseY-zoomCircle/2 - SHIFT_Y_MOUSE, zoomCircle, zoomCircle);
			
			// Now draw the layers
			g.drawImage(UnknownImage, 0, 0, currentWidth,currentHeight, null);
			g.drawImage(buildingImage, 0, 0, currentWidth,currentHeight, null);
			g.drawImage(roadsImage, 0, 0, currentWidth,currentHeight, null);
			g.drawImage(textImage, 0, 0, currentWidth,currentHeight, null);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setGoodQuality(Graphics2D g2d) {
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
	}
	
	// super.paintComponent clears off screen pixmap,
	// since we're using double buffering by default.
	protected void clear(Graphics g) {
		super.paintComponent(g);
	}

	public void feedInfosAndNames(ArrayList<MapElement> newInfo, double minLon, double minLat)
	{
		allInfo = newInfo;
		
		System.out.println("Received "+allInfo.size()+" elements");
		shiftX = -minLon;
		shiftY = minLat; // 700 should be frame height ?
	}
	
	public void shiftMarkedPoint(boolean doTheShift)
	{
		areMPShifter  = doTheShift;
	}
	
	public void updateCurrentProcess(MarkablePoint[] allCurrentPoints)
	{
		currentProcess = allCurrentPoints;
		
		this.repaint();
	}
	
	public void createAndShowMap()
	{
		super.setFont(this.getFont().deriveFont(SIZE_OF_MAP_FONT));
		
//		JFrame.setDefaultLookAndFeelDecorated(true);
//		frame = new JFrame("Draw map");
//		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
//		frame.getContentPane().add(this);
//		frame.setSize(800, 800);
//		frame.setLocationRelativeTo( null );
//		frame.setVisible(true);
		this.setSize(800, 700);
		this.setMaximumSize(new Dimension(2000, 2000));
		
		
		//myExporter.getMainWindow.addKeyListener(this);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.addMouseWheelListener(this);
		//this.addKeyListener(this);
		this.setVisible(true);
		
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		
		addKeyBinding();
	}

	public void addKeyBinding()
	{
		InputMap inputMap = this.getInputMap(WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = this.getActionMap();
		
		System.out.println("Current input map: "+inputMap.toString());
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                "scrollLeft");
		
		actionMap.put("scrollLeft",
                new ScrollAction(10, 0));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                "scrollRight");
		actionMap.put("scrollRight",
				new ScrollAction(-10, 0));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                "scrollUp");
		actionMap.put("scrollUp",
				new ScrollAction(0, 10));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                "scrollDown");
		actionMap.put("scrollDown",
				new ScrollAction(0, -10));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0),
                "zoomIn");
		actionMap.put("zoomIn",
                new ZoomAction(0.1f));
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0),
                "zoomIn");
		actionMap.put("zoomIn",
				new ZoomAction(0.1f));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0),
                "zoomOut");
		actionMap.put("zoomOut",
				new ZoomAction(-0.1f));
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0),
                "zoomOut");
		actionMap.put("zoomOut",
				new ZoomAction(-0.1f));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0),
                "showName");
		actionMap.put("showName",
                new ShowHideNameAction(true));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0),
                "hideName");
		actionMap.put("hideName",
				new ShowHideNameAction(false));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                "unpause");
		actionMap.put("unpause",
				new UnpauseAction());
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0),
                "selectionPoint");
		actionMap.put("selectionPoint",
				new ChooseSelectAction(TypeOfSelect.POINT_SELECT));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0),
                "selectionSegment");
		actionMap.put("selectionSegment",
				new ChooseSelectAction(TypeOfSelect.SEGMENT_SELECT));
		
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0),
                "selectionWay");
		actionMap.put("selectionWay",
				new ChooseSelectAction(TypeOfSelect.WAY_SELECT));
		
		System.out.println("2 - Current input map: "+inputMap.toString());
		
		// Not using keyboard?
//		else if (e.getKeyCode() == KeyEvent.VK_R)
//		{
//			Thread processThread = new Thread("Running process")
//			{
//				public void run()
//				{
//					myExporter.transformSelectionToElementsToProcess();
//					myExporter.doTheProcessing();
//				}
//			};
//			
//			processThread.start();
//		}
	}

	private class ChooseSelectAction  extends AbstractAction
	{
		TypeOfSelect localTypeOfSelection = TypeOfSelect.POINT_SELECT;
		
		ChooseSelectAction(TypeOfSelect newType)
		{
			localTypeOfSelection = newType;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			currentTypeOfSelection = localTypeOfSelection;
		}
	}

	private class UnpauseAction extends AbstractAction
	{
		UnpauseAction()
		{
			
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("unpauseAction");
			myExporter.unpause();
		}

	}

	private class ShowHideNameAction extends AbstractAction
	{
		boolean localShowName = false;
		
		ShowHideNameAction(boolean showName)
		{
			localShowName = showName;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			showNames  = localShowName;
			theFrame.repaint();
		}
	}
	
	private class ZoomAction extends AbstractAction
	{
		float localZoom = 0.0f;
		
		ZoomAction(float newZoom)
		{
			localZoom = newZoom;		
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("ZoomAction: zooming by " + zoom);
			zoom+=localZoom;
			theFrame.repaint();
		}

	}

	private class ScrollAction extends AbstractAction
	{
		int localShiftX = 0;
		int localShiftY = 0;
		
		ScrollAction(int xScroll, int yScroll)
		{
			localShiftX = xScroll;
			localShiftY = yScroll;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			System.out.println("scrollAction: scrolling by " + shiftFrameX +" , "+ shiftFrameY);
			shiftFrameX+=localShiftX;
			shiftFrameY+=localShiftY;
			theFrame.repaint();
		}

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
		boolean somethingToDo = false;
		
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			setCursor(new Cursor(Cursor.HAND_CURSOR));
			
			mouseSelecting = 1;
			somethingToDo = true;
		}
		else if (e.getButton() == MouseEvent.BUTTON3)
		{
			setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			
			mouseSelecting = 2;
			somethingToDo = true;
		}
		
		if (somethingToDo)
		{
			double lonSelection = ((double )(mouseX - shiftFrameX))/(zoomFixed*zoom) - shiftX;
			double latSelection = shiftY - ((double )(mouseY - SHIFT_Y_MOUSE - shiftFrameY))/(zoomFixed*zoom);
			double withRadius = ((double )zoomCircle/2)/(zoomFixed*zoom);

			System.out.println("Selection type "+currentTypeOfSelection.name());
			
			if (mouseSelecting == 1)
			{
				myExporter.selectUnselectPoints(lonSelection, latSelection, withRadius, currentTypeOfSelection, true);
			}
			else if (mouseSelecting == 2)
			{
				myExporter.selectUnselectPoints(lonSelection, latSelection, withRadius, currentTypeOfSelection, false);
			}
		}
		
		this.repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouseSelecting = -1;
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		zoomCircle  = zoomCircle - notches;
		if (zoomCircle < 1)
		{
			zoomCircle = 0;
		}
		else if (zoomCircle > 4000)
		{
			zoomCircle = 4000;
		}
		this.repaint();
		System.out.println("Size of zoom "+zoomCircle);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();		
		
		/*
		 * int xInfo = shiftFrameX + (int )((shiftX + oneCoord.longitude)*zoomFixed*zoom);
		 * int yInfo = shiftFrameY + (int )((shiftY - oneCoord.latitude)*zoomFixed*zoom);
		 */
		double lonSelection = ((double )(mouseX - shiftFrameX))/(zoomFixed*zoom) - shiftX;
		double latSelection = shiftY - ((double )(mouseY - SHIFT_Y_MOUSE - shiftFrameY))/(zoomFixed*zoom);
		double withRadius = ((double )zoomCircle/2)/(zoomFixed*zoom);
		
		if (mouseSelecting == 1)
		{
			myExporter.selectUnselectPoints(lonSelection, latSelection, withRadius, currentTypeOfSelection, true);
		}
		else if (mouseSelecting == 2)
		{
			myExporter.selectUnselectPoints(lonSelection, latSelection, withRadius, currentTypeOfSelection, false);
		}
			
		
		this.repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();

		this.repaint();
	}

	public void shift(int shiftX, int shiftY) {
		shiftFrameX+=shiftX;
		shiftFrameY+=shiftY;
		
		this.repaint();
	}

	public void zoom(double zoomCoef) {
		zoom+=zoomCoef;
		this.repaint();
	}

	public void showNames(boolean newShowNames) {
		showNames  = newShowNames;
		this.repaint();
	}

	public void unpause() {
		myExporter.unpause();
	}

	public void setCurrentTypeOfSelection(TypeOfSelect pointSelect) {
		currentTypeOfSelection = pointSelect;
	}

	public void startProcessing() {
		Thread processThread = new Thread("Running process")
		{
			public void run()
			{
				myExporter.transformSelectionToElementsToProcess();
				myExporter.doTheProcessing();
			}
		};
		
		processThread.start();
	}
} 