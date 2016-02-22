package de.biomedical_imaging.ij.removebackground;

import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;

import ij.plugin.ImageCalculator;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;


public class RemoveBackground_ implements ExtendedPlugInFilter, DialogListener {
	double radius;
	int nPasses;
	private int pass;
	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		return DOES_ALL;
	}

	@Override
	public void run(ImageProcessor ip) {
		ImageCalculator imcalc = new ImageCalculator();
		ImageProcessor img2 = ip.duplicate();
		RankFilters rank = new RankFilters();

		rank.rank(img2, radius, RankFilters.MAX);
		rank.rank(img2, radius, RankFilters.MIN);
		rank.rank(img2, radius, RankFilters.MEAN);
		long[] stat = img2.getStatistics().getHistogram();
		int i=0;
		while(stat[i]==0){
			i++;
		}
		
		img2.subtract(i);
		
		ImagePlus imp1 = new ImagePlus("",ip);
		ImagePlus imp2 = new ImagePlus("",img2);
		imcalc.run("subtract", imp1, imp2);
		
		pass++;
		
		showProgress();
		
	}
	
	

	@Override
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog(command+"...");
		gd.addSlider("Radius", 1, 20, 10);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.showDialog();
		if (gd.wasCanceled()) return DONE;
		radius = gd.getNextNumber();
		return IJ.setupDialog(imp, DOES_ALL);
	}
	
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		radius = gd.getNextNumber();
		return true;
	}

	@Override
	public void setNPasses(int nPasses) {
		this.nPasses = nPasses;
        pass = 0;
	}
	
	private void showProgress() {
        double percent = (double)(pass-1)/nPasses;
        IJ.showProgress(percent);
    }

}
