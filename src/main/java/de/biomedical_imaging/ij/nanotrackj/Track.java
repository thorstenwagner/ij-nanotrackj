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
import ij.IJ;
import ij.blob.Blob;

import java.awt.Polygon;
import java.util.ArrayList;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;

public class Track extends ArrayList<Step> implements Comparable<Track>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int startFrame;
	private int endFrame;
	public static double  nmPerPixel = 166; //166nm=Typical Pixelsize in Nanosight Devices
	public static double  framerate = 1.0/30;
	private double msd = -1; //Mean Square Displacement
	private double dc = -1; //Diffusion Coefficient
	private double lastDCUpdate = -1;
	private Polygon trackAsPolygon = null;
	private int trackID=0;
	private static int trackCounter=1;

	public static IDiffusionCoefficientEstimator diffCoeffEst;
	
	public Track(int frameIndex){
			startFrame = frameIndex;
			trackID=trackCounter;
			trackCounter++;
	}
	/**
	 * @param est The diffusion coefficient estimator.
	 */
	public static void setDiffusionCoefficientEstimator(IDiffusionCoefficientEstimator est){
		diffCoeffEst = est;
	
	
	}
	
	/**
	 * Do a Kalmin Filtering on the 
	 * @param R Motion blue coefficient (1/6 = full time-integration, 0=instantaneous camera shutter)
	 * @return
	 */
	public Track getKalmanFilteredTrack(double R){
		
		//Get Drift
		double[] drift = AllTracks.getInstance().getDrift();
		
		CovarianceEstimator koest;
		if (diffCoeffEst instanceof CovarianceEstimator) {
			koest = (CovarianceEstimator) diffCoeffEst;
		}
		else {
			IJ.error("Kalman Filtering only works with covariance estimator");
			return(this);
		}
		
		double[] locnoise = koest.getLocalizationNoise(this, R, drift[0], drift[1]); 
		
		Track filteredTrack = new Track(this.getStartFrameIndex());
		
		double postX = this.get(0).getX();
		double postY = this.get(0).getY();
		double postMMSEX=locnoise[0];
		double postMMSEY=locnoise[1];
		double varDF = koest.getDiffusionCoefficient(this, drift[0], drift[1])*2*framerate;
		double varNoiseX = locnoise[0];
		double varNoiseY = locnoise[1];
		
		CenterBlob cb = new CenterBlob((float)postX,(float)postY,((CenterBlob)this.get(0).getBlob()).getHUE());
		Step s = new Step(cb, this.get(0).getFrameIndex());
		filteredTrack.add(s);
		
		//Get drift corrected track
		for (int i = 1; i < this.size(); i++) {
			//Prediction
			double priorX = postX + i*drift[0];
			double priorY = postY + i*drift[1];
			
			//Minimum Predction MSE
			double priorMMSEX = postMMSEX + varDF;
			double priorMMSEY = postMMSEY + varDF;
			
			//Kalman Gain
			double Kx = priorMMSEX * 1/(varNoiseX+priorMMSEX);
			double Ky = priorMMSEY * 1/(varNoiseY+priorMMSEY);
			
			//Correction
			postX = priorX + Kx * (this.get(i).getX()+i*drift[0] - priorX); // 
			postY = priorY + Ky * (this.get(i).getY()+i*drift[1] - priorY); //
			
			cb = new CenterBlob((float)postX,(float)postY,((CenterBlob)this.get(i).getBlob()).getHUE());
			s = new Step(cb, this.get(i).getFrameIndex());
			filteredTrack.add(s);
			
			//Minimum MSE
			postMMSEX = (1-Kx)*priorMMSEX;
			postMMSEY = (1-Ky)*priorMMSEY;
		}
		
		return filteredTrack;
	}
	

	
	/**
	 * @return Track as Polygon
	 */
	public Polygon getTrackAsPolygon(){
		trackAsPolygon = new Polygon();
		for(int i = 0; i < this.size(); i++){
			trackAsPolygon.addPoint((int)this.get(i).getX(), (int)this.get(i).getY());
		}
		return trackAsPolygon;
	}
	
	/**
	 * @return Track as Polygon up to a slice number
	 */
	public Polygon getTrackAsPolygon(int slicenumber){
		trackAsPolygon = new Polygon();
		int i = 0;
		while(this.get(i).getFrameIndex() < slicenumber){
			trackAsPolygon.addPoint((int)this.get(i).getX(), (int)this.get(i).getY());
			i++;
		}
		return trackAsPolygon;
	}
	
	@Override
	public boolean add(Step e) {
		endFrame = e.getFrameIndex();
		return super.add(e);
	}
	
	/**
	 * @return The index of the frame where the track begins.
	 */
	public int getStartFrameIndex() {
		return startFrame;
	}
	
	/**
	 * @return The index of the frame where track ends.
	 */
	public int getEndFrameIndex() {
		return endFrame;
	}
	
	/**
	 * @param correctDrift True if the drift has to be corrected.
	 * @param tau Timelag
	 * @return The mean squared displacement for the timelag tau
	 */
	public double getMeanSquareDisplacement(boolean correctDrift, int tau){
		msd =0;
		if(correctDrift){
			double[] drift = AllTracks.getInstance().getDrift();
			msd = getMeanSquareDisplacement(drift[0], drift[1], tau);
		}
		else
		{
			msd = getMeanSquareDisplacement(tau);
		}
		return msd;
	}
	
	
	public double[] getMeanSquareDisplacementSD(boolean correctDrift, int tau){
		double[] sdAndN = new double[2];
		if(correctDrift){
			double[] drift = AllTracks.getInstance().getDrift();
			sdAndN = getMeanSquareDisplacementSD(drift[0], drift[1], tau);
		}
		else
		{
			sdAndN = getMeanSquareDisplacementSD(tau);
		}
		return sdAndN;
	}
	/**
	 * @param tau Timelag
	 * @return Mean Squared Displacement for timelage tau with no drift correction
	 */
	private double getMeanSquareDisplacement(int tau){
		return getMeanSquareDisplacement(0,0,tau);
	}
	
	private double[] getMeanSquareDisplacementSD(int tau){
		return getMeanSquareDisplacementSD(0,0,tau);
	}
	
	
	/**
	 * 
	 * @param driftx Drift in x direction (in pixels)
	 * @param drifty Drift in y direction (in pixels)
	 * @param tau Timelag
	 * @return The mean squared displacement for the timelag tau
	 */
	public double getMeanSquareDisplacement(double driftx, double drifty, int tau){

		msd = 0;
		if(this.size()==1){
			return 0;
		}
		int N = 0;
		for(int i = tau; i < this.size(); ++i){
			msd = msd + Math.pow(this.get(i-tau).getX()-this.get(i).getX() - tau*driftx,2) + Math.pow(this.get(i-tau).getY()-this.get(i).getY()-tau*drifty,2);
			++N;
		}
		
		msd = msd/N; //- 1.0/3;
		return msd;
	}
	/**
	 * 
	 * @param driftx Drift in x direction (in pixels)
	 * @param drifty Drift in y direction (in pixels)
	 * @param tau Timelag
	 * @return [0] = The mean squared displacement, [1] = Number of data points
	 */
	public double[] getMeanSquareDisplacementSD(double driftx, double drifty, int tau){

		double msd = 0;

		StandardDeviation sd = new StandardDeviation();
		int N = 0;
		for(int i = tau; i < this.size(); ++i){
			
			msd = Math.pow(this.get(i-tau).getX()-this.get(i).getX() - tau*driftx,2) + Math.pow(this.get(i-tau).getY()-this.get(i).getY()-tau*drifty,2);
			sd.increment(msd);
			
			++N;
		}
		
		double[] result = new double[2];
		result[0] = sd.getResult();
		result[1] = N;
		return result;
	}
	
	public double getSumOfAbsoluteDisplacements(){

		double sum = 0;
		if(this.size()==1){
			return 0;
		}
		for(int i = 1; i < this.size(); ++i){
			sum = sum + Math.abs((this.get(i-1).getX())-(this.get(i).getX())) + Math.abs((this.get(i-1).getY())-(this.get(i).getY()));

		}
		return sum;
	}
	
	public double getMaxDistanceFromStart(){
		double d = 0;
		double max = Double.MIN_VALUE;
		for(int i = 1; i < this.size(); ++i){
			d = Math.abs((this.get(0).getX())-(this.get(i).getX())) + Math.abs((this.get(0).getY())-(this.get(i).getY()));
			if(d>max){
				max = d;
			}

		}
		return max;
	}
	
	/**
	 * 
	 * @param correctDrift True, if the drift has to be corrected.
	 * @return The diffusion coefficient in pixel^2 / s
	 */
	public double getDiffusionCoefficient(boolean correctDrift, boolean useKalman){
		if(dc > -1 && lastDCUpdate == this.size()){
			return dc;
		}
		if(useKalman){
			double R = (836.0/1500)/6;
			dc = getKalmanFilteredTrack(R).getDiffusionCoefficient(false, false);
		}
		else if(correctDrift){
			double[] drift = AllTracks.getInstance().getDrift();
			dc = getDiffusionCoefficient(drift[0], drift[1]);
		}
		else
		{
			dc = getDiffusionCoefficient(0, 0);
		}
		lastDCUpdate=this.size();
		return dc;
	}

	
	/**
	 * 
	 * @param driftx Drift in x direction
	 * @param drifty Drift in y direction
	 * @return The diffusion coefficient in 10^-10 cm^2 / s
	 */
	private double getDiffusionCoefficient(double driftx, double drifty){

		double pixelSquared_to_E10x_cmSquared=nmPerPixel*nmPerPixel*Math.pow(10,-4);
		dc = diffCoeffEst.getDiffusionCoefficient(this, driftx, drifty) * pixelSquared_to_E10x_cmSquared;
		return dc;
	}
	
	/**
	 * @return The last blob of the track
	 */
	public Blob getLastBlob(){
		
		return this.get(this.size()-1).getBlob();
	}
	
	/**
	 * 
	 * @return The unique track id
	 */
	public int getTrackID(){
		return trackID;
	}

	@Override
	public int compareTo(Track o) {
		if(trackID<o.getTrackID()){
			return -1;
		}
		if(trackID>o.getTrackID()){
			return 1;
		}
		return 0;
	}
	/**
	 * @return The median hue of the track
	 */
	public float getMedianHUE(){
		Median median = new Median();
		double[] hues = new double[this.size()];
		for(int i = 0; i < this.size(); i++){
			hues[i]=((CenterBlob)this.get(i).getBlob()).getHUE();
		}
		return (float)median.evaluate(hues);
	}

}
