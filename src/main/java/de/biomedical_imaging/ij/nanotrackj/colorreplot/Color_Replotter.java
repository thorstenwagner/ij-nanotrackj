/*
    The MIT License (MIT)
    
    NanoTrackJ is a software to characterize the size of nanoparticles by its trajectories
    Copyright (C) 2013  Thorsten Wagner wagner@biomedical-imaging.de

	Permission is hereby granted, free of charge, to any person obtaining a copy of
	this software and associated documentation files (the "Software"), to deal in
	the Software without restriction, including without limitation the rights to
	use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
	the Software, and to permit persons to whom the Software is furnished to do so,
	subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
	FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
	CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package de.biomedical_imaging.ij.nanotrackj.colorreplot;

import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import de.biomedical_imaging.ij.nanotrackj.NanoTrackJ_;
import de.biomedical_imaging.ij.nanotrackj.Track;


import ij.IJ;
import ij.ImageJ;
import ij.gui.GUI;
import ij.gui.TrimmedButton;
import ij.measure.Measurements;
import ij.plugin.frame.PlugInFrame;


@SuppressWarnings("serial")
public class Color_Replotter extends PlugInFrame implements AdjustmentListener, ActionListener, PropertyChangeListener {
	private ImageJ ij;
	private Label labelh, labelf, label1, label2;
	private Scrollbar minSlider, maxSlider;
	private Checkbox checkboxDrawTracks;
	private BandPlot plot = new BandPlot();
	private int sliderRange = 270;
	private Panel panel;
	private Button  replotB, closeB; 
	private int minHue = 0;
	private int maxHue = 270;
	
	public Color_Replotter(String title, double[][] hueData) {
		
		super(title);
		int maxWidth = 0;
		int[] histo = new int[hueData.length];
		for(int i =0; i < hueData.length; i++){
			histo[i] = (int)(hueData[i][1]*10000);
		}
		for(int i = 0; i < histo.length; i++){
			if(histo[i]>0){
				maxWidth=i;
				maxHue=i;
				sliderRange=i;
			}
		}
		plot = new BandPlot(maxWidth);
		
		// TODO Auto-generated constructor stub
		ij = IJ.getInstance();
		Font font = new Font("SansSerif", Font.PLAIN, 10);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);
		
		int y = 0;
		c.gridx = 0;
		c.gridy = y;
		c.gridwidth = 1;
		c.weightx = 0;
		c.insets = new Insets(5, 0, 0, 0);
		labelh = new Label("Hue", Label.CENTER);
		add(labelh, c);
		

		c.gridx = 1;
		c.gridy = y++;
		c.gridwidth = 1;
		c.weightx = 0;
		c.insets = new Insets(7, 0, 0, 0);
		labelf = new Label("", Label.RIGHT);
		add(labelf, c);

		// plot
		c.gridx = 0;
		c.gridy = y;
		c.gridwidth = 1;
		
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(0, 5, 0, 0);
		add(plot, c);
		 y++;
		// minHue slider
		minSlider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 1;
		c.weightx = IJ.isMacintosh()?90:100;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 0, 0);

		add(minSlider, c);
		minSlider.addAdjustmentListener(this);
		minSlider.setUnitIncrement(1);

		// minHue slider label
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = IJ.isMacintosh()?10:0;
		c.insets = new Insets(5, 0, 0, 0);
		label1 = new Label("      ", Label.LEFT);
		label1.setFont(font);
		add(label1, c);

		// maxHue sliderHue
		maxSlider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
		c.gridx = 0;
		c.gridy = y;
		c.gridwidth = 1;
		c.weightx = 100;
		c.insets = new Insets(5, 5, 0, 0);
		add(maxSlider, c);
		maxSlider.addAdjustmentListener(this);
		maxSlider.setUnitIncrement(1);
		

		// maxHue slider label
		c.gridx = 1;
		c.gridwidth = 1;
		c.gridy = y++;
		c.weightx = 0;
		c.insets = new Insets(5, 0, 0, 0);
		label2 = new Label("      ", Label.LEFT);
		label2.setFont(font);
		add(label2, c);
		
		//Buttons
		int trim = IJ.isMacOSX()?10:0;
		panel = new Panel();
		panel.setLayout(new GridLayout(0, 2, 0, 0));
		replotB = new TrimmedButton("Replot", trim);
		replotB.addActionListener(this);
		replotB.addKeyListener(ij);
		panel.add(replotB);
		
		closeB = new TrimmedButton("Close", trim);
		closeB.addActionListener(this);
		closeB.addKeyListener(ij);
		panel.add(closeB);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.insets = new Insets(5, 5, 10, 5);
		gridbag.setConstraints(panel, c);
		add(panel);
		
		checkboxDrawTracks = new Checkbox("Draw Tracks", false);
		checkboxDrawTracks.addPropertyChangeListener(this);
		checkboxDrawTracks.addKeyListener(ij);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 1;
		c.weightx = IJ.isMacintosh()?90:100;
		add(checkboxDrawTracks,c);
		plot.setHistogram(histo);
		pack();
		GUI.center(this);
		setVisible(true);
		maxSlider.setValue(maxHue);
		minSlider.setValue(0);
		
		this.setSize(290, 300);
		this.setResizable(false);
		
		
	}
	
	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == minSlider)
			adjustMinHue((int) minSlider.getValue());
		else if (e.getSource() == maxSlider)
			adjustMaxHue((int) maxSlider.getValue());
		updateLabels();
		updatePlot();
		
	}
	
	void updatePlot() {
		plot.minHue = minHue;
		plot.maxHue = maxHue;
		plot.repaint();
	}
	void adjustMinHue(int value) {
		minHue = value;
		if (maxHue<minHue) {
			maxHue = minHue;
			maxSlider.setValue((int)maxHue);
		}
	}
	
	void adjustMaxHue(int value) {
		maxHue = value;
		if (minHue>maxHue) {
			minHue = maxHue;
			minSlider.setValue((int)minHue);
		}
	}
	
	void updateLabels() {
		label1.setText(""+((int)minHue));
		label2.setText(""+((int)maxHue));
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		//NanoTrackJ_.getInstance().plotHueDependingSizeDistribution(minHue, maxHue);
		NanoTrackJ_ nj = NanoTrackJ_.getInstance();
		if(arg0.getSource().equals(replotB)){
			nj.plotHueDependendSizeDistribution(minHue, maxHue);
			
			if(checkboxDrawTracks.getState()){
				nj.getTrackOverlay().clear();
				ArrayList<Track> huefiltered = nj.getHueFilteredTracks(minHue, maxHue);
				for(int i = 0; i < nj.getImageStack().getStackSize(); i++){
					NanoTrackJ_.getInstance().drawTracks(huefiltered, i);
				}
			}
		}
		else if(arg0.getSource().equals(closeB)){
			this.close();
		}
		
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
	class BandPlot extends Canvas implements Measurements, MouseListener {
		
		int WIDTH = 270, HEIGHT=120;
		double minHue = 0;
		double maxHue = 270;
		int[] histogram;
		Color[] hColors;
		int hmax;
		Image os;
		Graphics osg;
	
		public BandPlot() {
			addMouseListener(this);
			setSize(WIDTH+1, HEIGHT+1);
		}
		
		public BandPlot(int WIDTH) {
			addMouseListener(this);
			this.WIDTH=WIDTH;
			setSize(WIDTH+1, HEIGHT+1);
		}
	
		/** Overrides Component getPreferredSize(). Added to work
		around a bug in Java 1.4 on Mac OS X.*/
		public Dimension getPreferredSize() {
			return new Dimension(WIDTH+1, HEIGHT+1);
		}
	
		void setHistogram(int[] hist) {
		//	ImageProcessor ip = imp.getProcessor();
		//	ImageStatistics stats = ImageStatistics.getStatistics(ip, AREA+MODE, null);
			int maxcount =0;
			for(int i = 0; i < hist.length; i++){
				if(hist[i]>maxcount){
					maxcount=hist[i];	
				}
			}
			setSize(WIDTH+1, HEIGHT+1);
			histogram = hist;
			hmax = (int)(maxcount*1.15);//(int)(maxCount2 * 1.15);//GL was 1.5
			os = null;
			
			hColors = new Color[270];
	
			for (int i=0; i<270; i++){
						hColors[i] = Color.getHSBColor(i/360f, 1f, 1f);//new Color(r[i]&255, g[i]&255, b[i]&255);
			}
		}
		
		int[] getHistogram() {
			return histogram;
		}
	
		public void update(Graphics g) {
			paint(g);
		}
	
		public void paint(Graphics g ) {
			int hHist=0;
			if (histogram!=null) {
				if (os==null) {
					os = createImage(WIDTH,HEIGHT);
					osg = os.getGraphics();
					//osg.setColor(Color.white);
					osg.setColor(new Color(140,152,144));
					osg.fillRect(0, 0, WIDTH, HEIGHT);
					for (int i = 0; i < WIDTH; i++) {
						if (hColors!=null) osg.setColor(hColors[i]);
						hHist=HEIGHT - ((int)(HEIGHT * histogram[i])/hmax)-6;
						osg.drawLine(i, HEIGHT, i, hHist);
						osg.setColor(Color.black);
						osg.drawLine(i, hHist, i, hHist);
					}
					osg.dispose();
				}
				if (os!=null) g.drawImage(os, 0, 0, this);
			} else {
				g.setColor(Color.white);
				g.fillRect(0, 0, WIDTH, HEIGHT);
			}
			g.setColor(Color.black);
			g.drawLine(0, HEIGHT -6, WIDTH, HEIGHT-6);
			g.drawRect(0, 0, WIDTH, HEIGHT);
			g.drawRect((int)minHue, 1, (int)(maxHue-minHue), HEIGHT-7);
		}
	
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
	} // BandPlot class

	




}
