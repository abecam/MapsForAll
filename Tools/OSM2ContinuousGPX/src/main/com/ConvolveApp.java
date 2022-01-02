package com;

/* Arpana*/   
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class ConvolveApp extends JFrame {
 CPanel displayPanel;

 JButton sharpenButton, blurringButton, edButton, resetButton;

 public ConvolveApp() {
   super();
   Container container = getContentPane();

   displayPanel = new CPanel();
   container.add(displayPanel);

   JPanel panel = new JPanel();
   panel.setLayout(new GridLayout(2, 2));
   panel
       .setBorder(new TitledBorder(
           "Click a Button to Perform the Associated Operation and Reset..."));

   sharpenButton = new JButton("Sharpen");
   sharpenButton.addActionListener(new ButtonListener());
   blurringButton = new JButton("Blur");
   blurringButton.addActionListener(new ButtonListener());
   edButton = new JButton("Edge Detect");
   edButton.addActionListener(new ButtonListener());
   resetButton = new JButton("Reset");
   resetButton.addActionListener(new ButtonListener());

   panel.add(sharpenButton);
   panel.add(blurringButton);
   panel.add(edButton);
   panel.add(resetButton);

   container.add(BorderLayout.SOUTH, panel);

   addWindowListener(new WindowAdapter() {
     public void windowClosing(WindowEvent e) {
       System.exit(0);
     }
   });
   setSize(displayPanel.getWidth(), displayPanel.getHeight() + 10);
   setVisible(true); 
 }
 public static void main(String arg[]) {
   new ConvolveApp();
 }

 class ButtonListener implements ActionListener {
   public void actionPerformed(ActionEvent e) {
     JButton button = (JButton) e.getSource();

     if (button.equals(sharpenButton)) {
       displayPanel.sharpen();
       displayPanel.repaint();
     } else if (button.equals(blurringButton)) {
       displayPanel.blur();
       displayPanel.repaint();
     } else if (button.equals(edButton)) {
       displayPanel.edgeDetect();
       displayPanel.repaint();
     } else if (button.equals(resetButton)) {
       displayPanel.reset();
       displayPanel.repaint();
     }
   }
 }
}

class CPanel extends JLabel {
 Image displayImage;

 BufferedImage biSrc;

 BufferedImage biDest; // Destination image is mandetory.

 BufferedImage bi; // Only an additional reference.

 Graphics2D big;

 CPanel() {
   setBackground(Color.black);
   loadImage();
   setSize(500, 500); 
   //setSize(displayImage.getWidth(this), displayImage.getWidth(this)); 
   //createBufferedImages();
   bi = biSrc;
 }

 public void loadImage() {
   displayImage = Toolkit.getDefaultToolkit().getImage("Arpana.jpg");
   MediaTracker mt = new MediaTracker(this);
   mt.addImage(displayImage, 1);
   try {
     mt.waitForAll();
   } catch (Exception e) {
     System.out.println("Exception while loading.");
   }
   if (displayImage.getWidth(this) == -1) {
     System.out.println("No jpg file");
     //System.exit(0);
   }
 }

 public void createBufferedImages() {
   biSrc = new BufferedImage(displayImage.getWidth(this), displayImage
       .getHeight(this), BufferedImage.TYPE_INT_RGB);

   big = biSrc.createGraphics();
   big.drawImage(displayImage, 0, 0, this);

   biDest = new BufferedImage(displayImage.getWidth(this), displayImage
       .getHeight(this), BufferedImage.TYPE_INT_RGB);
 }

 public void sharpen() {
   float data[] = { -1.0f, -1.0f, -1.0f, -1.0f, 9.0f, -1.0f, -1.0f, -1.0f,
       -1.0f };
   Kernel kernel = new Kernel(3, 3, data);
   ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP,
       null);
   convolve.filter(biSrc, biDest);
   bi = biDest;
 }

 public void blur() {
   float data[] = { 0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f,
       0.0625f, 0.125f, 0.0625f };
   Kernel kernel = new Kernel(3, 3, data);
   ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP,
       null);
   convolve.filter(biSrc, biDest);
   bi = biDest;
 }

   public void edgeDetect() {
   float data[] = { 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
       -1.0f };

   Kernel kernel = new Kernel(3, 3, data);
   ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP,
       null);
   convolve.filter(biSrc, biDest);

   bi = biDest;
 }

 public void reset() {
   big.setColor(Color.black);
   big.clearRect(0, 0, bi.getWidth(this), bi.getHeight(this));
   big.drawImage(displayImage, 0, 0, this);
   bi = biSrc;
 }

 public void update(Graphics g) {
   g.clearRect(0, 0, getWidth(), getHeight());
   paintComponent(g);
 }

 public void paintComponent(Graphics g) {
   super.paintComponent(g);
   Graphics2D g2D = (Graphics2D) g;

   g2D.drawImage(bi, 0, 0, this);
 }
}