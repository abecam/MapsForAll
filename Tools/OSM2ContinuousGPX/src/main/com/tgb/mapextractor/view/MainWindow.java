package com.tgb.mapextractor.view;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.tgb.controller.GpxProcessor;
import com.tgb.controller.TypeOfSelect;
import com.tgb.gfx.DrawMap4;

public class MainWindow extends JFrame implements MouseListener, MouseWheelListener, MouseMotionListener, KeyListener {
	
	
	private static final float SIZE_OF_MAP_FONT = 14.0f;
	public DrawMap4 myDrawer;
	private GpxProcessor ourGpxProcessor;
	private int iPath = 0;
	public JPanel myMainPanel;
	protected boolean nameHidden = true;
	JTextField maxAngle;
	JTextField nbIter;
	JTextField incrAngle;
	
	public MainWindow()
	{
		super();
		
		//myMainPanel
		//this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}
	
	public void createAndShowMap()
	{
		//super.setFont(this.getFont().deriveFont(SIZE_OF_MAP_FONT));
		
		JFrame.setDefaultLookAndFeelDecorated(true);
		this.setName("Draw map");
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		this.setSize(1200, 800);
		this.setLocationRelativeTo( null );
		//this.addKeyListener(this);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.addMouseWheelListener(this);
		this.setVisible(true);
		
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		
//		FlowLayout mainLayout = new FlowLayout();
//		this.setLayout(mainLayout);
		
		addDrawer(this);
		
		addGUI();
	}

	private void addDrawer(JFrame mainFrame) {
		ourGpxProcessor = new GpxProcessor(this);
		
		myDrawer = new DrawMap4(ourGpxProcessor);
		//this.addKeyListener(myDrawer);
	}

	private void addGUI() {
		JPanel ourPanel = new JPanel();
		//mainFrame.add(ourPanel);
		
		// Menu Load OSM/Save GPX
		JMenuBar menuBar = new JMenuBar();
		JMenu mainMenuFile = new JMenu("FILE");
		JMenu menuHelp = new JMenu("Help");
		menuBar.add(mainMenuFile);
		menuBar.add(menuHelp);
		JMenuItem subMenuOpenFile = new JMenuItem("Open OSM map");
		subMenuOpenFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadOSM();
			}
		});
		JMenuItem subMenuSaveFile = new JMenuItem("Save result as");
		subMenuSaveFile.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				saveGPX();
			}
		});
		mainMenuFile.add(subMenuOpenFile);
		mainMenuFile.add(subMenuSaveFile);

		//Creating the panel at bottom and adding components
		JPanel panel = new JPanel(); // the panel is not visible in output
		
		/*
		 * if (e.getKeyCode() == KeyEvent.VK_N)
		{
			myDrawer.showNames(true);
		}
		else if (e.getKeyCode() == KeyEvent.VK_B)
		{
			myDrawer.showNames(false);
		}
		else if (e.getKeyCode() == KeyEvent.VK_Q)
		{
			myDrawer.setCurrentTypeOfSelection(TypeOfSelect.POINT_SELECT);
		}
		else if (e.getKeyCode() == KeyEvent.VK_W)
		{
			myDrawer.setCurrentTypeOfSelection(TypeOfSelect.SEGMENT_SELECT);
		}
		else if (e.getKeyCode() == KeyEvent.VK_E)
		{
			myDrawer.setCurrentTypeOfSelection(TypeOfSelect.WAY_SELECT);
		}
		else if (e.getKeyCode() == KeyEvent.VK_R)
		{
			myDrawer.startProcessing();
		}
		else if (e.getKeyCode() == KeyEvent.VK_A)
		{
			ourGpxProcessor.removeWorstCases();
		}
		else if (e.getKeyCode() == KeyEvent.VK_S)
		{
			ourGpxProcessor.selectOnePath(iPath++);
			if (iPath >= ourGpxProcessor.getNbOfBestPaths())
			{
				iPath = 0;
			}
		}
		else if (e.getKeyCode() == KeyEvent.VK_D)
		{
			ourGpxProcessor.doTheDegradation();
		}
		 */
		JButton showName = new JButton("Show Names");
		showName.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				nameHidden  = !nameHidden;
				myDrawer.showNames(!nameHidden);
				
				showName.setText(nameHidden?"Show Names":"Hide Names");
			}
		});
		JButton selectByPoint = new JButton("Select by point");
		selectByPoint.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				myDrawer.setCurrentTypeOfSelection(TypeOfSelect.POINT_SELECT);
			}
		});
		JButton selectBySegment = new JButton("Select by segment");
		selectBySegment.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				myDrawer.setCurrentTypeOfSelection(TypeOfSelect.SEGMENT_SELECT);
			}
		});
		JButton selectByWay = new JButton("Select by way");
		selectByWay.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				myDrawer.setCurrentTypeOfSelection(TypeOfSelect.WAY_SELECT);
			}
		});
		JButton transformToGPX = new JButton("Transform to GPX");
		transformToGPX.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				myDrawer.startProcessing();
				ourGpxProcessor.removeWorstCases();
			}
		});
		
		JButton selectNextSol = new JButton("Select next solution");
		selectNextSol.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ourGpxProcessor.selectOnePath(iPath++);
				if (iPath >= ourGpxProcessor.getNbOfBestPaths())
				{
					iPath = 0;
				}
			}
		});
		JButton doTheDegradation = new JButton("Do the degradation");
		doTheDegradation.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ourGpxProcessor.doTheDegradation(new Integer(maxAngle.getText()), new Integer(nbIter.getText()), new Integer(incrAngle.getText()));
			}
		});

		JButton reset = new JButton("Reset to original");
		reset.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ourGpxProcessor.selectOnePath(iPath-1);	
			}
		});
		panel.add(showName);
		panel.add(selectByPoint);
		panel.add(selectBySegment);
		panel.add(selectByWay);
		panel.add(transformToGPX);
		panel.add(selectNextSol);
		panel.add(doTheDegradation);
		panel.add(reset);
		
		maxAngle = new JTextField("Max angle for degradation");
		panel.add(maxAngle);
		maxAngle.setText("15");
		
		incrAngle = new JTextField("Incr. of angle");
		panel.add(incrAngle);
		incrAngle.setText("3");
		
		nbIter = new JTextField("Nb of iterations");
		panel.add(nbIter);
		nbIter.setText("15");

		//Adding Components to the frame.
		ourPanel.setLayout(new GridLayout(2, 1));
		
		ourPanel.add(menuBar);
		menuBar.setSize(getMaximumSize());
		ourPanel.add(panel);	
		ourPanel.setSize(getMaximumSize());
		
		Container container = getContentPane();
		
		container.add(BorderLayout.NORTH, ourPanel);
		this.setVisible(true);
		// Left Panel map
		
		// Right panel 
		//  Information -> nb of point, best score default solution, best score all solutions, nb of points/score after decimation
		
		//  buttons (Generate/Decimate/Restore)
		
		//  sliders (Which solution)
		
		//  radio buttons (which selection mode)
	}

	protected void saveGPX() {
		// JFileChooser-Objekt erstellen
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filterGPX = new FileNameExtensionFilter("GPX trace", "gpx");
		chooser.addChoosableFileFilter(filterGPX);
        chooser.setFileFilter(filterGPX);
        
        // Dialog zum Speichern von Dateien anzeigen

        int returnVal = chooser.showSaveDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
           System.out.println("You chose to save this file: " +
                chooser.getSelectedFile().getName());
           
           File fileToBeSaved = chooser.getSelectedFile();
           
           if(!fileToBeSaved.getAbsolutePath().endsWith(".gpx")){
        	    fileToBeSaved = new File(chooser.getSelectedFile() + ".gpx");
        	}
           ourGpxProcessor.saveSelectedGPX(fileToBeSaved);
        }
	}

	protected void loadOSM() {
		// JFileChooser-Objekt erstellen
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filterOSM = new FileNameExtensionFilter("OSM maps", "osm");
		chooser.addChoosableFileFilter(filterOSM);
        chooser.setFileFilter(filterOSM);
        
        // Dialog zum Speichern von Dateien anzeigen
        chooser.showOpenDialog(null);
        int returnVal = chooser.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
           System.out.println("You chose to open this file: " +
                chooser.getSelectedFile().getName());
           
           ourGpxProcessor.setOSMFile(chooser.getSelectedFile());
        }
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("Pressed "+e);
		if (e.getKeyCode() == KeyEvent.VK_LEFT)
		{
			myDrawer.shift(10,0);
		}
		else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
		{
			myDrawer.shift(-10,0);
		}
		else if (e.getKeyCode() == KeyEvent.VK_UP)
		{
			myDrawer.shift(0,10);
		}
		else if (e.getKeyCode() == KeyEvent.VK_DOWN)
		{
			myDrawer.shift(0,-10);
		}
		else if (e.getKeyCode() == KeyEvent.VK_PLUS)
		{
			myDrawer.zoom(0.1);
		}
		else if (e.getKeyCode() == KeyEvent.VK_MINUS)
		{
			myDrawer.zoom(-0.1);
		}
		else if (e.getKeyCode() == KeyEvent.VK_N)
		{
			myDrawer.showNames(true);
		}
		else if (e.getKeyCode() == KeyEvent.VK_B)
		{
			myDrawer.showNames(false);
		}
		else if (e.getKeyCode() == KeyEvent.VK_SPACE)
		{
			myDrawer.unpause();
		}
		else if (e.getKeyCode() == KeyEvent.VK_Q)
		{
			myDrawer.setCurrentTypeOfSelection(TypeOfSelect.POINT_SELECT);
		}
		else if (e.getKeyCode() == KeyEvent.VK_W)
		{
			myDrawer.setCurrentTypeOfSelection(TypeOfSelect.SEGMENT_SELECT);
		}
		else if (e.getKeyCode() == KeyEvent.VK_E)
		{
			myDrawer.setCurrentTypeOfSelection(TypeOfSelect.WAY_SELECT);
		}
		else if (e.getKeyCode() == KeyEvent.VK_R)
		{
			myDrawer.startProcessing();
		}
		else if (e.getKeyCode() == KeyEvent.VK_A)
		{
			ourGpxProcessor.removeWorstCases();
		}
		else if (e.getKeyCode() == KeyEvent.VK_S)
		{
			ourGpxProcessor.selectOnePath(iPath++);
			if (iPath >= ourGpxProcessor.getNbOfBestPaths())
			{
				iPath = 0;
			}
		}
		else if (e.getKeyCode() == KeyEvent.VK_D)
		{
			ourGpxProcessor.doTheDegradation(new Integer(maxAngle.getText()), new Integer(nbIter.getText()), new Integer(incrAngle.getText()));
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {

		MainWindow myMainWindow = new MainWindow();
						
		myMainWindow.createAndShowMap();				
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
