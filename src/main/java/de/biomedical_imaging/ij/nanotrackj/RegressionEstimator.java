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

import ij.gui.GenericDialog;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class RegressionEstimator extends IDiffusionCoefficientEstimator {
	
	private int minTimeLag;
	private int maxTimeLag;
	private int tau[];

	@Override
	public double getDiffusionCoefficient(Track track, double driftx,
			double drifty) {
		if(track.size()==1){
			return 0;
		}
		SimpleRegression reg = new SimpleRegression(true);
		double msdhelp = 0;
		if(tau.length==1){
			reg.addData(0, 0);
		}
		
		for(int i = 0; i < tau.length; i++){
			
			msdhelp = track.getMeanSquareDisplacement(driftx,drifty,tau[i]); 
			reg.addData(tau[i]*(1.0/getFramesPerSecond()), msdhelp);
		}

		double D = reg.getSlope()/4; 
		return D;
	}

	@Override
	void setup() {
		// TODO Auto-generated method stub
		GenericDialog gd = new GenericDialog("Setup Regression Estimator");
		gd.addNumericField("Min. Time Lag", 1, 0);
		gd.addNumericField("Max. Time Lag", 2, 0);
		gd.showDialog();
		minTimeLag = (int)gd.getNextNumber();
		maxTimeLag = (int)gd.getNextNumber();
		tau = new int[maxTimeLag];
		for(int i = 0 ; i < maxTimeLag-minTimeLag+1; i++){
			tau[i] = minTimeLag + i;
		}
	}
	
	public int getMinTimelag(){
		return minTimeLag;
	}
	
	public int getMaxTimeLag(){
		return maxTimeLag;
	}

}
