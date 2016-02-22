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

package de.biomedical_imaging.ij.nanotrackj;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.plaf.basic.BasicScrollPaneUI.HSBChangeListener;

import org.jfree.ui.RefineryUtilities;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.RankFilters;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.blob.*;
import de.biomedical_imaging.ij.nanotrackj.colorreplot.Color_Replotter;
import de.biomedical_imaging.ij.plot.BarplotDataset;
import de.biomedical_imaging.ij.plot.HistogramPlotter;


public class NanoTrackJ_ implements PlugInFilter {
	private ImagePlus impStack;
	private TrackSynthesizer trackSyn;
	private final static double kB = 1.3806488* Math.pow(10, -19); 				// Bolzmann Constant in [kg cm^2 s^-2 K^-1]
	private int minTrackLength=10;												// Minimum Track Length
	private double temp=22 + 273.15;  											// Temperature in [K]
	private double visk=0.9548 * Math.pow(10, -5); 								// Viscosity [kg cm^-1 s^-1]
	private double framerate=30; 												// Frames per Second [1/s]
	private boolean hasBlackBackground=true;									// Only relevant for "Blob" Center Estimation									
	private boolean doCorrectDrift=true;										// If true, the drift will be corrected
	private boolean doDrawtracks=false;											// If true, the particle trajectories are plotted in an overlay 
	private int meanFilterSize =3;												// Maximum-Dialog: Size of the mean filter
	private int tolerance=15;													// Maximum-Dialog: Tolerance of Find Maxima Algorithm
	private double minSize=70; 													// Minimal expected particle size in [nm]
	private boolean useWalker=true;												// If true, maximum likelihood estimation is used
	private String centerMethods[] = {"Blob","Maxima","Maxima & Gaussian Fit"};  // All available center methods
	public static final int CM_BLOB = 0, CM_MAXIMA = 1, CM_MAXIMA_GAUSS = 2;
	public String dcEstMethods[] = {"Regression","Covariance"}; //,"Covariance+Kalman"};//,"Maximum"};
	public static final int DC_REG = 0, DC_COV = 1, DC_COVKAL = 2;
	private String dcEstimator=dcEstMethods[1];									// Selected Diffusion Coefficient Estimator
	private double radius = 15;													// Search Radius for particle tracking
	private String savepath = IJ.getDirectory("temp") + "IJ_NTA_OLDSETTINGS.txt";
	private Overlay trackOverlay;												// The overlay where tracks are drawn.
	private String centerMethodChoice=centerMethods[1];
	private int maxWalkerHistogrammDiameter = 0;								// The maximum diameter for the walker method
	private static NanoTrackJ_ instance = null;
	private boolean useKalman = false; //
	
	/**
	 * singlton pattern
	 * @return Instance of NanoTrackJ
	 */
	public static NanoTrackJ_ getInstance(){
		return instance;
	}
	
	@Override
	public int setup(String arg, ImagePlus imp) {
		if(imp==null){
			IJ.error("There is no image stack open");
			return -1;
		}
		if(!(imp.getType()==ImagePlus.GRAY8 || imp.getType()==ImagePlus.COLOR_RGB)){
			IJ.error("This Plugin needs a 8 bit grayscale or rgb color stack as input");
			return -1;
		}
		
		//Init
		instance = this;
		impStack = imp;
		trackOverlay = new Overlay();
		
		//Get Settings
		boolean wasCanceled = showGUIandGetSettings();
		if(wasCanceled){
			return -1;
		}
		
		/// Configure Diffusion Coefficient Estimator
		IDiffusionCoefficientEstimator dcEst = null;
		if(dcEstimator.equals(dcEstMethods[1]) ){
			dcEst = new CovarianceEstimator();
		}else if(dcEstimator.equals(dcEstMethods[0]) ){
			dcEst = new RegressionEstimator();
		}else if(dcEstimator.equals(dcEstMethods[2]) ){
			dcEst = new KalmanCovarianceEstimator();
		}else{
			throw new IllegalArgumentException("No center estimation technique is selected");
		}
		dcEst.setup();
		dcEst.setFramesPerSecond(framerate);
		Track.setDiffusionCoefficientEstimator(dcEst);
		
		
		//Configure Center-Estimation Method
		if(getCenterMethodType()==CM_BLOB){
			//If "Blob" is used, an binary image is necassary. The blob in the binary image will be tracked.
			ImageStatistics stats = imp.getStatistics();
			boolean notBinary = (stats.histogram[0] + stats.histogram[255]) != stats.pixelCount;
			if(notBinary){
				IJ.error("The Center-Method 'Blob' needs a binary image. The Input Image is not binary. Please use the Center-Method 'Maximum' instead or binarize your image.");
				return -1;
			}
		}
		else if(getCenterMethodType()==CM_MAXIMA|| getCenterMethodType()==CM_MAXIMA_GAUSS){
			if(!hasBlackBackground){
				//Find maximum only works with dark background images.
				IJ.run("Invert", "stack");
			}
			wasCanceled=MaximumAssistant();
			if (wasCanceled) {
				//Stop execuation
				return -1;
			}
		}
		
		//Save Settings
		saveSettings();
		
		
		return STACK_REQUIRED | DOES_8G | DOES_RGB | DOES_STACKS;
	}
	
	/**
	 * Save all settings
	 */
	private void saveSettings(){
		Prefs.set("nanotrackj.centerMethodChoice", centerMethodChoice);
		Prefs.set("nanotrackj.dcEstimator", dcEstimator);
		Prefs.set("nanotrackj.radius", radius);
		Prefs.set("nanotrackj.minSize", minSize);
		Prefs.set("nanotrackj.minTrackLength", minTrackLength);
		Prefs.set("nanotrackj.temp", temp);
		Prefs.set("nanotrackj.visk", visk);
		Prefs.set("nanotrackj.nmPerPixel", Track.nmPerPixel);
		Prefs.set("nanotrackj.framerate", framerate);
		Prefs.set("nanotrackj.hasBlackBackground", hasBlackBackground);
		Prefs.set("nanotrackj.doCorrectDrift", doCorrectDrift);
		Prefs.set("nanotrackj.doDrawtracks", doDrawtracks);
		Prefs.set("nanotrackj.useWalker", useWalker);
		Prefs.set("nanotrackj.maxWalkerHistogrammDiameter", maxWalkerHistogrammDiameter);
		Prefs.set("nanotrackj.maxdialog.mean", meanFilterSize);
		Prefs.set("nanotrackj.maxdialog.tolerance", tolerance);
		Prefs.savePreferences();
	}
	
	/**
	 * Setup and display the GUI
	 * @return Return true if the GUI was canceled, false otherwise.
	 */
	private boolean showGUIandGetSettings() {
		loadOldSettings();
		
		
		
		GenericDialog gd = new GenericDialog("NanoTrackJ");
		gd.addChoice("Center estimator", centerMethods, centerMethodChoice);
		gd.addChoice("Diffusion coefficient estimator", dcEstMethods, dcEstimator);
		gd.addNumericField("Search radius [Pixel]", radius, 2);
		gd.addNumericField("Min. exp. particle size [nm]", minSize, 2);
		gd.addNumericField("Min. number of steps per track", minTrackLength, 0);
		gd.addNumericField("Temperature [°C]", temp-273.15, 1);
		gd.addNumericField("Viscosity [mPa x s]", visk * Math.pow(10, 5), 4);
		gd.addNumericField("Pixelsize [nm]", Track.nmPerPixel, 2);
		gd.addNumericField("Framerate [Hz]", framerate, 1);
		gd.addCheckbox("Black/Dark background", hasBlackBackground);
		gd.addCheckbox("Correct linear drift", doCorrectDrift);
		gd.addCheckbox("Draw tracks", doDrawtracks);
		gd.addCheckbox("Size distribution estimation by Walker's method (WM)", useWalker);
		gd.addNumericField("Maximum Diameter (WM only, 0 = auto)", maxWalkerHistogrammDiameter, 0);
		gd.addHelp("http://fiji.sc/NanoTrackJ");

		NTADialogListener ntaListener = new NTADialogListener();
		gd.addDialogListener(ntaListener);
		gd.showDialog();
		
		if (gd.wasCanceled()) {
			return true;
		}
		
		centerMethodChoice = gd.getNextChoice();
		dcEstimator = gd.getNextChoice();
		radius = gd.getNextNumber();
		minSize = (int)gd.getNextNumber();
		minTrackLength = (int)gd.getNextNumber();
		temp = gd.getNextNumber() + 273.15;
		visk = gd.getNextNumber() * Math.pow(10, -5);
		Track.nmPerPixel = gd.getNextNumber();
		framerate = gd.getNextNumber();
		
		hasBlackBackground = gd.getNextBoolean();
		doCorrectDrift = gd.getNextBoolean();
		doDrawtracks = gd.getNextBoolean();
		useWalker = gd.getNextBoolean();
		maxWalkerHistogrammDiameter = (int)gd.getNextNumber();
		trackSyn = new TrackSynthesizer(radius);
		
		
		
		return false;
	}
	
	private void loadOldSettings(){
		//Read Settings
		centerMethodChoice = Prefs.get("nanotrackj.centerMethodChoice", centerMethodChoice);
		dcEstimator = Prefs.get("nanotrackj.dcEstimator", dcEstimator);
		radius = Prefs.get("nanotrackj.radius", 12.58);
		minSize = Prefs.get("nanotrackj.minSize", minSize);
		minTrackLength = (int)Prefs.get("nanotrackj.minTrackLength", minTrackLength);
		temp = Prefs.get("nanotrackj.temp", 22+273.15);
		visk = Prefs.get("nanotrackj.visk", 0.9578135430287747*Math.pow(10, -5));
		Track.nmPerPixel = Prefs.get("nanotrackj.nmPerPixel", 164);
		framerate = Prefs.get("nanotrackj.framerate", 30);
		hasBlackBackground = Prefs.get("nanotrackj.hasBlackBackground",true);
		doCorrectDrift = Prefs.get("nanotrackj.doCorrectDrift",true);
		doDrawtracks = Prefs.get("nanotrackj.doDrawtracks",false);
		useWalker =  Prefs.get("nanotrackj.useWalker",useWalker);
		maxWalkerHistogrammDiameter = (int)Prefs.get("nanotrackj.maxWalkerHistogrammDiameter",800);
		meanFilterSize = (int)Prefs.get("nanotrackj.maxdialog.mean", 3);
		tolerance = (int)Prefs.get("nanotrackj.maxdialog.tolerance", 15);
	}
	
	@Override
	public void run(ImageProcessor ip) {
		IJ.showStatus("Tracking...");
	    IJ.showProgress(ip.getSliceNumber(), impStack.getStackSize());
	    ArrayList<Blob> blobs = new ArrayList<Blob>();

	    
	    if(getCenterMethodType()==CM_BLOB){
	    	//Use ijblob to find the blobs.
	    	blobs = getBlobsOfFrame(ip, hasBlackBackground);
	    	
	    }
	    else if (getCenterMethodType() == CM_MAXIMA || getCenterMethodType() == CM_MAXIMA_GAUSS){
	    	Polygon maximas;
	    	if(meanFilterSize>0){
	    		//If a mean filer is applied.
		    	RankFilters rank = new RankFilters();
		    	ImageProcessor ipclone = ip.duplicate();
		    	rank.rank(ipclone, meanFilterSize, RankFilters.MEAN);
		    	maximas = findMaxima(tolerance,ipclone);
	    	}else
	    	{
	    		maximas = findMaxima(tolerance,ip);
	    	}
	    	
	    	boolean doGaussianFit = (getCenterMethodType() == CM_MAXIMA_GAUSS);
			for(int i = 0; i < maximas.npoints; ++i){
				CenterBlob cb = null; // Build artifical blobs, because the TrackSynthesizer only works with blobs from the ijblob library
				cb = generateCenterBlob(ip, doGaussianFit, maximas.xpoints[i], maximas.ypoints[i]);
				cb.setIntensity(ip.get(maximas.xpoints[i], maximas.ypoints[i]));
				blobs.add(cb);
			}
			impStack.deleteRoi();
	    }
		
	    //Update Tracks
		trackSyn.updateTracks(blobs, ip.getSliceNumber());
		if(doDrawtracks){
			drawTracks(AllTracks.getInstance(),ip.getSliceNumber());
		}
		
		//If the last frame was analyzed...
		if(impStack.getStackSize()==ip.getSliceNumber()){
			trackSyn.archiveOpenTracks(); //Close all open tracks
			
			if(validTrackExists(AllTracks.getInstance().getFinishedTracks())){
			
				if(useWalker==false){
					plotDiffCoeffDistribution(AllTracks.getInstance().getFinishedTracks());
				}
				plotSizeDistribution(AllTracks.getInstance().getFinishedTracks());
				if(impStack.getType()==ImagePlus.COLOR_RGB){
					plotHueHistogram();
				}
			}
			else
			{
				IJ.error("No track could be completed");
			}
			outputTracksInResultTable();
			impStack.setOverlay(trackOverlay);
			impStack.updateAndRepaintWindow();
			
			
			
		}
		
	}
	
	public ImagePlus getImageStack(){
		return impStack;
	}
	
	/**
	 * Plots a Hue Histogram of the tracks.
	 */
	private void plotHueHistogram() {
		ArrayList<Double> hueAL = new ArrayList<Double>();
		ArrayList<Double> trackLenghAL = new ArrayList<Double>();
		for (Track t : AllTracks.getInstance().getFinishedTracks()) { 
			double hue = (double)t.getMedianHUE();
			if(t.size()>minTrackLength && hue < 270){
				hueAL.add(hue);
				trackLenghAL.add((double)t.size());
			}
		}
		double[][] hueData = new double[hueAL.size()][2];
		for(int i = 0; i < hueAL.size(); i++){
			hueData[i][0] = hueAL.get(i);
			hueData[i][1] = trackLenghAL.get(i);
		}
		double[][] hist = NanoTrackUtil.getHistogram(hueData, 1);
		double[][] histOutput = new double[hist.length][2];
		//Monochromatic Map: Hue -> Monochromatic Wavelength
		
		for(int i = 0; i < hist.length; i++){
			
			histOutput[i][0] = 620 - hist[i][0] * 170f/270f;
			//IJ.log("Hi " + hist[i][0] + " value: "+ histOutput[i][0]);
			histOutput[i][1] = hist[i][1];
		}
		Color_Replotter rep = new Color_Replotter("Monochromatic Map",histOutput);
		rep.setVisible(true);

		NanoTrackUtil.outputHistogramData(histOutput, "Monochromatic Map", "Monochomatic Wavelength [nm]", "Probablity");
		
		
	}

	
	/**
	 * @param ip A binary image
	 * @param blackBackground True if the background color is black, else false.
	 * @return All blobs of the binary image ip.
	 */
	public ArrayList<Blob> getBlobsOfFrame(ImageProcessor ip, boolean blackBackground) {
		ManyBlobs blobs = new ManyBlobs(new ImagePlus("frame", ip));
		if(blackBackground){
			blobs.setBackground(0);
		}
		
		blobs.findConnectedComponents();
		return blobs;
	}
	
	/**
	 * Generates a object of the class CenterBlob 
	 * @param ip	The image which contains the blob
	 * @param doGaussianFit	If true, a 2D gaussian will fitted to intensity profile of the blob. 
	 * The center of the gaussian will used as center of the CenterBlob.
	 * @param xc x coordinate of the center
	 * @param yc y coordinate of the center
	 */
	private CenterBlob generateCenterBlob(ImageProcessor ip, boolean doGaussianFit, double xc, double yc){
		CenterBlob cb = null;
		float blobXc = (float)xc;
		float blobYc = (float)yc;
		if(doGaussianFit){ // Maxima & Gaussian Fit
			//Do Gaussian Fit
			float[] coords = NanoTrackUtil.getFittedParameter(ip, xc, yc);
			blobXc = coords[0];
			blobYc = coords[1];
			
		}
		if(impStack.getType()==ImagePlus.COLOR_RGB){
			ColorProcessor cip = (ColorProcessor)ip;
			float hue = NanoTrackUtil.meanHUE(cip, (int)blobXc, (int)blobYc);
			cb = new CenterBlob(blobXc, blobYc,hue);
			float meanIntensity = NanoTrackUtil.meanIntensity(cip, (int)blobXc, (int)blobYc);
			cb.setIntensity(meanIntensity);
		}else{
			float mean = NanoTrackUtil.meanIntensity(ip, (int)blobXc, (int)blobYc);
			cb = new CenterBlob(blobXc, blobYc);
			cb.setIntensity(mean);
		}
		return cb;
		
	}
	
	/** 
	 * @return -1 of the cancel button was pressed, 0 otherwise.
	 */
	private boolean MaximumAssistant(){
		GenericDialog gd = new GenericDialog("Spot-Assistant");
		gd.addSlider("Preview-Slice", 1, impStack.getStackSize(), impStack.getSlice());
		gd.addSlider("Mean filter", 0, 12, meanFilterSize);
		gd.addSlider("Tolerance", 1, 200, tolerance);
		
		
		DialogListener dl = new DialogListener() {
			
			@Override
			public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
				//if(e != null){
					IJ.run("Undo");
				//}
				impStack.setSlice((int)gd.getNextNumber());
				int slice = impStack.getSlice();
				impStack.deleteRoi();
				
			
				int meanSize= (int)gd.getNextNumber();
				
				if(meanSize>0){
					IJ.run("Mean...", "radius="+meanSize+" slice");
				}
				
				Polygon pol = findMaxima(gd.getNextNumber(),impStack.getStack().getProcessor(slice));//nsStack.getProcessor(slice)
				Roi points = new PointRoi(pol.xpoints, pol.ypoints, pol.npoints);
				((PointRoi)points).setHideLabels(true);
				impStack.setRoi(points);
				impStack.updateAndRepaintWindow();
				return true;
			}
		};
		
		gd.addDialogListener(dl);
		dl.dialogItemChanged(gd, null);
		gd.showDialog();
		 gd.getNextNumber();
		meanFilterSize =  (int)gd.getNextNumber();
		tolerance =  (int)gd.getNextNumber();
		if (gd.wasCanceled()) {
			return true;
		}
		if(meanFilterSize>0){
			IJ.run("Undo");
		}
		return false;
	}
	/**
	 * @param tolerance Tolerance of the IJ FindMaximum method
	 * @param slice The slice to be processed.
	 * @return Polygon of all maximas.
	 */
	private Polygon findMaxima(double tolerance, ImageProcessor proc){
		MaximumFinder mf = new MaximumFinder();
		boolean excludeOnEdges = true;
		return mf.getMaxima(proc, tolerance, excludeOnEdges);
	}
	
	/**
	 * Generates a Results Table which consists of all tracks, their start and end frame, 
	 * the estimated diffusion coefficient, size and the global drift.
	 */
	private void outputTracksInResultTable(){
		
		ResultsTable rt = Analyzer.getResultsTable();
		ArrayList<Track> tracks = AllTracks.getInstance().getFinishedTracks();
		if(rt==null)
		{
			 rt = new ResultsTable();
			 Analyzer.setResultsTable(rt);
		}
		
		double[] drift = AllTracks.getInstance().getDrift();
		for(int i = 0; i < tracks.size(); i++){
			
			Track track = tracks.get(i);

	
			double D = track.getDiffusionCoefficient(doCorrectDrift,useKalman);
		
			if(isValidTrack(track)){
				rt.incrementCounter();
				rt.addValue("TID", track.getTrackID());
				rt.addValue("#Steps", track.size());
				rt.addValue("Start Frame", track.getStartFrameIndex());
				rt.addValue("End Frame", track.getEndFrameIndex());
				rt.addValue("D [10^(-10) cm^2 / s]", D);
				double size = diffCoeffToDiameter(D);
				rt.addValue("Size [nm]", size);
				rt.addValue("Global Drift x [10^(-5) cm/s]", drift[0]);
				rt.addValue("Global Drift y [10^(-5) cm/s]", drift[1]);
			}

		}
		rt.show("Tracking Data");
		
	}
	/**
	 * Converts the diffusion coefficient to hydrodynamic diameter
	 * @param dc Diffusion coeffcient in 10^-10 cm^2 s^-1
	 * @return Hydrodynamic Diameter in [nm]
	 */
	public double diffCoeffToDiameter(double dc){
		double E10cm2nm = Math.pow(10, 17);
		return (kB*temp/(dc*3*Math.PI*visk))*E10cm2nm;
	}
	
	/**
	 * 
	 * @param tracks Tracks to be drawn.
	 * @param currentslice the current slice
	 */
	public void drawTracks(ArrayList<Track> tracks, int currentslice){
		Iterator<Track> ittrack = tracks.iterator();
		while(ittrack.hasNext()){
			Track currentTrack = ittrack.next();
			boolean lastTrackIsCurrentSlice = (currentTrack.getEndFrameIndex() >= currentslice) 
					&& (currentTrack.getStartFrameIndex() < currentslice) 
					&& isMoving(currentTrack,5)
					&& hasMinTrackLength(currentTrack);
			//Only draw tracks which "active" in the current slice.
			if(lastTrackIsCurrentSlice | (currentslice==-1)){
				
				PolygonRoi proi = new PolygonRoi(currentTrack.getTrackAsPolygon(currentslice), Roi.POLYLINE);
				if(currentTrack.size()>=minTrackLength){
				
					proi.setStrokeColor(Color.yellow);
				}
				else{
					proi.setStrokeColor(Color.gray);
				}
				
				proi.setPosition(currentslice);
				trackOverlay.add(proi);
				
				double dc=0;
			
				if(doCorrectDrift==true && currentslice>20){
					dc = currentTrack.getDiffusionCoefficient(true,useKalman);
				}else{
					dc = currentTrack.getDiffusionCoefficient(false,false);
				}

				int d = (int)diffCoeffToDiameter(dc);
				if(d == Integer.MAX_VALUE){
					d = 0;
				}
				TextRoi.setFont(Font.MONOSPACED, 10, Font.PLAIN);
				TextRoi troi = new TextRoi((int)currentTrack.getLastBlob().getCenterOfGravity().getX(), (int)currentTrack.getLastBlob().getCenterOfGravity().getY(), 
						""+d +" T:"+currentTrack.getTrackID());
				troi.setPosition(currentslice);
				troi.setStrokeColor(Color.magenta);
				trackOverlay.add(troi);	
			}
		}
	}
	
	/**
	 * Filter all Tracks which median hue is inside the lower and upper hue threshold
	 * @param lowerhue Lower hue threshold
	 * @param upperhue Upper hue threshold		
	 * @return An arraylist of all tracks which are inside the lower and upper thrshold
	 */
	public ArrayList<Track> getHueFilteredTracks(double lowerhue,double upperhue){
		ArrayList<Track> hueFilteredTracks = new ArrayList<Track>();
		for (Track track : AllTracks.getInstance().getFinishedTracks()) {
			double hue = track.getMedianHUE();
			if(hue > lowerhue && hue < upperhue){
				hueFilteredTracks.add(track);
			}
		}
		return hueFilteredTracks;
	}
	
	/**
	 * @param lowerHue Lower hue threshold
	 * @param upperHue Upper hue threshold	
	 */
	public void plotHueDependendSizeDistribution(double lowerHue,double upperHue){
		ArrayList<Track> hueFilteredTracks = getHueFilteredTracks(lowerHue,upperHue);
		if(useWalker==false){
			plotDistribution(hueFilteredTracks, false, "Diffusion Coefficients ("+lowerHue+" < HUE < " + upperHue + ")");
		}
		plotDistribution(hueFilteredTracks,true, "Diameter Histogram ("+lowerHue+" < HUE < " + upperHue + ")");
	}
	
	private void plotDiffCoeffDistribution(ArrayList<Track> tracks){
		plotDistribution(tracks,false,"Diffusion Coefficients");
	}
	
	private void plotSizeDistribution(ArrayList<Track> tracks){
		plotDistribution(tracks,true ,"Diameter Histogram");
	}
	
	/**
	 * Plot Size / Diffusion coefficient distribution
	 * @param minTrackLength
	 * @param size True if size distribution, false if diffusion coefficent distribution
	 * @param temp Temperatur in kelvin
	 * @param visk Viscosity
	 * @param plotTitle The title of the plot
	 */
	private void plotDistribution(ArrayList<Track> tracks, boolean size, String plotTitle){
		ArrayList<Double> data = new ArrayList<Double>();
		HistogramPlotter histogram =null;
		int numberOfParticels = 0;
		int meanTrackLength = 0;
		int normfactor = 0;
		IJ.showStatus("Estimate diffusion coefficients...");
		if(useWalker==false)
		{
			for(int i = 0; i < tracks.size(); i++){
				IJ.showProgress(i, tracks.size());
				if(isValidTrack(tracks.get(i))){
					double d = tracks.get(i).getDiffusionCoefficient(doCorrectDrift,useKalman);
					numberOfParticels++;
					if(size){
						d= diffCoeffToDiameter(d);
					}
					meanTrackLength += tracks.get(i).size();
					normfactor += tracks.get(i).size();
					data.add(d);
					data.add((double)tracks.get(i).size());	
				}
			}
			double[][] dataarray = new double[data.size()/2][2];
			int k = 0;
			for(int i = 0; i < data.size(); i=i+2){
				dataarray[k][0]=data.get(i);
				dataarray[k][1]=data.get(i+1)/normfactor;
				k++;
			}
			meanTrackLength = meanTrackLength/numberOfParticels;
			BarplotDataset dataset = new BarplotDataset();
			String xlabel = "Diffusion coefficient [x 10^-10 cm^2]";
			String histLabel = "Histogram Diffusion coefficient";
			double binsize = 10;
			if(size){
				xlabel = "Diameter [nm]";
				histLabel = "Histogram Diameter";
				binsize = 4;
			}
			double[][] dens = NanoTrackUtil.getHistogram(dataarray, binsize);
			histogram = new HistogramPlotter(plotTitle,xlabel,dens ,numberOfParticels,meanTrackLength,dataset);
			NanoTrackUtil.outputHistogramData(dens, histLabel,"Bin [nm]","Probablity");
		}
		else
		{
			for(int i = 0; i < tracks.size(); i++){
				IJ.showProgress(i, tracks.size());
				if(isValidTrack(tracks.get(i))){
					double d = tracks.get(i).getDiffusionCoefficient(doCorrectDrift,useKalman);
					numberOfParticels++;
					meanTrackLength += tracks.get(i).size();
					double msd = d*4.0/framerate; //Diffusionkoeffizient zurückrechnen
					data.add(msd);
					data.add((double)tracks.get(i).size());
				}	
			}
			double[][] dataarray = new double[data.size()/2][2];
			int k = 0;
			for(int i = 0; i < data.size(); i=i+2){
				dataarray[k][0]=data.get(i);
				dataarray[k][1]=data.get(i+1);
				k++;
			}
			if(numberOfParticels>0){
				WalkerMethodEstimator walker = new WalkerMethodEstimator(dataarray, temp, visk, framerate,maxWalkerHistogrammDiameter);
				meanTrackLength = meanTrackLength/numberOfParticels;
				BarplotDataset dataset = new BarplotDataset();
				double[][] dens = walker.estimate();
				histogram = new HistogramPlotter(plotTitle,"Diameter [nm]", dens,numberOfParticels,meanTrackLength,dataset);
				NanoTrackUtil.outputHistogramData(dens, "Walker Density","Bin [nm]","Probablity"); //outputWalkerHistogramData(dens);
			}else{
				IJ.error("No track could be completed");
			}
		}
		
        histogram.pack();
        RefineryUtilities.centerFrameOnScreen(histogram);
        histogram.setVisible(true);
	}
	
	public boolean validTrackExists(ArrayList<Track> tracks){
		boolean  validTrackEsits = false;
		for (Track t : tracks) {
			if(isValidTrack(t)){
				return true;
			}
		}
		return false;
	}
	
	public boolean isValidTrack(Track t){
		return hasMinTrackLength(t)
				&& (t.getDiffusionCoefficient(doCorrectDrift,useKalman)>0)
				&& isMoving(t,5);
				//&& (t.getSumOfAbsoluteDisplacements()>t.size()*2);
	}
	
	private boolean isMoving(Track t, int minDistance){
		
		return (t.getMaxDistanceFromStart() > minDistance);
		
	}
	
	private boolean hasMinTrackLength(Track t) {
		return (t.size() >= minTrackLength);
	}
	
	public Overlay getTrackOverlay(){
		return trackOverlay;
	}
	
	
	public int getMinTrackLength() {
		return minTrackLength;
	}



	public double getTemp() {
		return temp;
	}



	public double getFramerate() {
		return framerate;
	}



	public int getMaximumTolerance() {
		return tolerance;
	}
	
	public int getMaximumMeanFilterSize(){
		return meanFilterSize;
	}



	public double getMinParticleSize() {
		return minSize;
	}



	public int getDCEstimatorType() {
		int type =0;
		if(dcEstimator.equals(dcEstMethods[0])){
			type = DC_REG;
		} else if(dcEstimator.equals(dcEstMethods[1]) ) {
			type = DC_COV;
		} else if(dcEstimator.equals(dcEstMethods[2]) ) {
			type = DC_COVKAL;
		}
		return type;
	}
	
	public String getDCEstimatorChoice() {
		return dcEstimator;
	}



	public double getSearchRadius() {
		return radius;
	}
	
	public double getViscosity(){
		return visk;
	}

	public double getPixelsize(){
		return Track.nmPerPixel;
	}
	
	public boolean doCorrectDrift(){
		return doCorrectDrift;
	}
	
	public boolean doUseWalkerMethod(){
		return useWalker;
	}
	
	public double getWalkerMaxSize(){
		return maxWalkerHistogrammDiameter;
	}
	
	public int getMaximumDialogMeanFilterSize(){
		return meanFilterSize;
	}
	
	public int getMaximumDialogTolerance(){
		return tolerance;
	}

	public int getCenterMethodType() {
		int type = 0;
		if(centerMethodChoice.equals(centerMethods[0])){
			type = CM_BLOB;
		}
		else if(centerMethodChoice.equals(centerMethods[1])){
			type = CM_MAXIMA;
		}else if(centerMethodChoice.equals(centerMethods[2]) ){
			type = CM_MAXIMA_GAUSS;
		}
		return type;
	}
	
	public String getCenterMethodChoice() {

		return centerMethodChoice;
	}


}


