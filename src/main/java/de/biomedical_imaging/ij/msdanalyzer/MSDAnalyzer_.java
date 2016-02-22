package de.biomedical_imaging.ij.msdanalyzer;

import org.jfree.ui.RefineryUtilities;


import de.biomedical_imaging.ij.plot.MSDPlotter;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * The MSD Analyzer plots the msd curve.
 * @author Thosten Wagner
 *
 */
public class MSDAnalyzer_ implements PlugIn {
	private int trackid;
	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		GenericDialog gd = new GenericDialog("MSD Analyzer");
		gd.addNumericField("Track ID", 1, 0);
		gd.addNumericField("Max. Timelag", 10, 0);
		gd.showDialog();
		if(gd.wasCanceled()){
			return;
		}
		trackid = (int)gd.getNextNumber();
		int maxlag = (int)gd.getNextNumber();
	
		MSDPlotter demo = new MSDPlotter("MSD Analyzer",trackid,maxlag);
	    demo.pack();
	    RefineryUtilities.centerFrameOnScreen(demo);
	    demo.setVisible(true);
	    /*
	    Track track = AllTracks.getInstance().getFinishedTrackByID(trackid);
	    for(int i = 1; i < track.size(); i++){
	    	CenterBlob cbn1 = ((CenterBlob)(track.get(i-1).getBlob()));
	    	CenterBlob cbn = ((CenterBlob)(track.get(i).getBlob()));
	    	float Fn=cbn.getIntensity();
	    	float Fn1=cbn1.getIntensity();
	    	float Xn = cbn.getCenterOfGravity().x;
	    	float Xn1 = cbn1.getCenterOfGravity().x;
	    	double z = -166*(Math.log(Fn1/Fn));
	    	double x = 166*(Xn1 - Xn);
	    	IJ.log("Z: " + z + " X: " +x);
	    }
	    */
	    
		
	}

}
