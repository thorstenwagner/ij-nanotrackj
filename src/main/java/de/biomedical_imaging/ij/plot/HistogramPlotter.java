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

package de.biomedical_imaging.ij.plot;


import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import ij.IJ;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.IntervalXYDataset;

import de.biomedical_imaging.ij.nanotrackj.NanoTrackJ_;
import de.biomedical_imaging.ij.nanotrackj.RegressionEstimator;
import de.biomedical_imaging.ij.nanotrackj.Track;


@SuppressWarnings("serial")
public class HistogramPlotter extends JFrame{//extends ApplicationFrame{
	
	private String title;
	private int numberOfParticles;
	private int meanTrackLength;
	private String xlabel;

	JPanel main;
	JLabel txt;
/*
	public HistogramPlotter(String title, String xlabel, double[] data, int numberOfParticles, int meanTrackLength, IDatasetCreator datacreator){

		super(title);
		this.title = title;
		this.xlabel = xlabel;
		this.numberOfParticles = numberOfParticles;  
	    this.meanTrackLength = meanTrackLength;
	    
	   
		
	    IntervalXYDataset xydataset = datacreator.create(data);
	    boolean isbarplot = (datacreator instanceof BarplotDataset);
		JFreeChart chart  = createChart(xydataset,isbarplot);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
	        // add it to our application
	    setContentPane(chartPanel);

	    setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
	    
	    pack();
	    setVisible(true);
        
	}
	*/
	public HistogramPlotter(String title, String xlabel, double[][] data, int numberOfParticles, int meanTrackLength, IDatasetCreator datacreator){

		super(title);
		this.title = title;
		this.xlabel = xlabel;
		this.numberOfParticles = numberOfParticles;  
	    this.meanTrackLength = meanTrackLength;
	    IntervalXYDataset xydataset = datacreator.create(data);
	    boolean isbarplot = (datacreator instanceof BarplotDataset);
	    txt = new JLabel();
		Font f = new Font("Verdana", Font.PLAIN, 12);
		txt.setFont(f);
		
		JFreeChart chart  = createChart(xydataset,isbarplot);
		
		ChartPanel chartPanel = new ChartPanel(chart);
	
		
		
		txt.setText(formatSettingsString());

		main = new JPanel();
		main.setPreferredSize(new java.awt.Dimension(500, 350));
		main.add(chartPanel);
		main.add(txt);
	    setContentPane(main);
	    
	    setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
	    
	    pack();
	    setVisible(true);
	    
	    JMenuItem savebutton = ((JMenuItem)chartPanel.getPopupMenu().getComponent(3));
	    chartPanel.getPopupMenu().remove(3); // Remove Save button
		//ActionListener al = savebutton.getActionListeners()[0];
	    savebutton = new JMenuItem("Save as png");
		//savebutton.removeActionListener(al);
		savebutton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				
				try {
					JFileChooser saveFile = new JFileChooser();
					saveFile.setAcceptAllFileFilterUsed(false);
					saveFile.addChoosableFileFilter(new FileNameExtensionFilter("Images","png"));
					int userSelection = saveFile.showSaveDialog(main);
					if (userSelection == JFileChooser.APPROVE_OPTION) {
						BufferedImage bi = ScreenImage.createImage(main);
					    File fileToSave = saveFile.getSelectedFile();
					    String filename = fileToSave.getName();
					    int i = filename.lastIndexOf('.');
					    String suffix = filename.substring(i+1);
					    String path = fileToSave.getAbsolutePath();
					    if(!(suffix.equals("png"))){
					    	path += ".png";
					    }
					    ScreenImage.writeImage(bi, path);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					IJ.log(""+e.getMessage());
				}
				
			}
		});
		chartPanel.getPopupMenu().insert(savebutton, 3);
        
	}
	
	public String[] getStettingsString(){
		int basesize = 22;
		int arraysize = basesize;
		NanoTrackJ_ nj = NanoTrackJ_.getInstance();
		if(nj.doUseWalkerMethod()==true){
			arraysize += 2;
		}
		if(nj.getCenterMethodType()==NanoTrackJ_.CM_MAXIMA || nj.getCenterMethodType()==NanoTrackJ_.CM_MAXIMA_GAUSS){
			arraysize += 4;
		}
		if(nj.getDCEstimatorType()==NanoTrackJ_.DC_REG){
			arraysize += 4;
			
		}
	
		String[] allset = new String[arraysize];
		allset[0] = "Center estimation";
		allset[1] = ""+nj.getCenterMethodChoice();
		allset[2] = "Diffusion-Coefficient estimator";
		allset[3] = "" + nj.getDCEstimatorChoice();
		allset[4] = "Search radius";
		allset[5] = ""+nj.getSearchRadius();
		allset[6] = "Min. exp. particle diameter";
		allset[7] = ""+nj.getMinParticleSize();
		allset[8] = "Min. number of steps per track";
		allset[9] = ""+nj.getMinTrackLength();
		allset[10] = "Temp.";
		allset[11] = ""+(nj.getTemp() - 273.15);
		allset[12] = "Visc.";
		allset[13] = IJ.d2s(nj.getViscosity()*Math.pow(10, 5), 4);
		allset[14] = "Pixelsize";
		allset[15] = ""+nj.getPixelsize();
		allset[16] = "Framerate";
		allset[17] = ""+nj.getFramerate();
		allset[18] = "Correct linear drift";
		allset[19] = ""+nj.doCorrectDrift();
		allset[20] = "Walker's method";
		allset[21] = "" + nj.doUseWalkerMethod();
		if(nj.doUseWalkerMethod()==true){
			allset[basesize++] = "Walker's Method Min Size";
			allset[basesize++] = "" + nj.getWalkerMaxSize();
		}
		if(nj.getCenterMethodType()==NanoTrackJ_.CM_MAXIMA || nj.getCenterMethodType()==NanoTrackJ_.CM_MAXIMA_GAUSS){
			allset[basesize++] = "Mean size (maxima dialog)";
			allset[basesize++] = ""+nj.getMaximumDialogMeanFilterSize();
			allset[basesize++] = "Tolerance (maxima dialog)";
			allset[basesize++] = ""+nj.getMaximumDialogTolerance();
		}
		if(nj.getDCEstimatorType()==NanoTrackJ_.DC_REG){
			RegressionEstimator re = (RegressionEstimator)Track.diffCoeffEst;
			allset[basesize++] = "Min. timelag (regression dialog)";
			allset[basesize++] = ""+re.getMinTimelag();
			allset[basesize++] = "Max. timelag (regression dialog)";
			allset[basesize++] = ""+re.getMaxTimeLag();
		}
		return allset;
	}
	
	
	public String formatSettingsString(){
		String[] str = getStettingsString();
		String s = "<html>";
		s = s + "<b>Number of tracks:</b> " + this.numberOfParticles + " <b>Mean number of steps per track:</b> " + this.meanTrackLength + "<hr width='100%' align='center'/> ";
		String windowtitle = IJ.getImage().getTitle();
		s+="<b>Filename:</b> " + windowtitle;
		for(int i = 0; i < str.length; i+=2){
			String label = " "+str[i]+":";
			String value = " "+str[i+1];
			s=s+"<b>"+label+"</b>"+value;
		}
		
		s = s + "</html>";
		return s;
		
	}
	
    private JFreeChart createChart(IntervalXYDataset intervalxydataset,boolean barplot)
    { 
    	if(barplot){
    		JFreeChart jfreechart = ChartFactory.createXYBarChart(title, xlabel , false, "Rel. Frequency", intervalxydataset, PlotOrientation.VERTICAL, false,true,false);
    		
    		return jfreechart;

    	}
    	JFreeChart jfreechart = ChartFactory.createHistogram(title, xlabel, "Rel. Frequency", intervalxydataset, PlotOrientation.VERTICAL, false, true, false);
            
            return jfreechart;
    }
}
