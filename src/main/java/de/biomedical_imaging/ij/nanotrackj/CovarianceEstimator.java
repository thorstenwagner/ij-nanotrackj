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

public class CovarianceEstimator extends IDiffusionCoefficientEstimator {

	double driftx;
	double drifty;
	Track track;

	private double getDistanceProductX(int n,int m){
		double xn = track.get(n).getX() - track.get(n-1).getX() + driftx;
		double xm = track.get(m).getX() - track.get(m-1).getX() + driftx; 

		return xn*xm;
	}
	
	private double getDistanceProductY(int n,int m){
		double xn = track.get(n).getY() - track.get(n-1).getY() + drifty;
		double xm = track.get(m).getY() - track.get(m-1).getY() + drifty;
		return xn*xm;
	}
	
	@Override	
	/**
	 * Calculates the Diffusion Coefficient as described in:
	 * Vestergaard, C., 2012. 
	 * Optimal Estimation of Diffusion Coefficients from Noisy Time-Lapse-Recorded Single-Particle Trajectories. 
	 * Technical University of Denmark.
	 */
	public double getDiffusionCoefficient(Track track, double driftX,
			double driftY) {
		this.driftx = driftX;
		this.drifty = driftY;
		this.track = track;
		
		if(track.size()==1){
			return 0;
		}
		double[] covData = getCovData(track, 0, driftX, driftY);
		
		return covData[0];
	}
	
	/**
	 * Calculates the localization noise using an covariance estimate as described in:
	 * Vestergaard, C., 2012. 
	 * Optimal Estimation of Diffusion Coefficients from Noisy Time-Lapse-Recorded Single-Particle Trajectories. 
	 * Technical University of Denmark.
	 * @param t Track
	 * @param R Motion blue coefficient (1/6 = full time-integration, 0=instantaneous camera shutter)
	 * @param driftX Drift in x direction
	 * @param driftY Drift in y direction
	 * @return
	 */
	public double[] getLocalizationNoise(Track t, double R, double driftX,
			double driftY){
		double[] covData = getCovData(t, R, driftX, driftY);
		double[] locNoise = new double[2];
		locNoise[0] = Math.abs(covData[1]);
		locNoise[1] = Math.abs(covData[2]);
		return locNoise;
	}
	
	private double[] getCovData(Track track, double R, double driftX,
			double driftY){
		
		
		this.driftx = driftX;
		this.drifty = driftY;
		this.track = track;

		double sumX = 0;
		double sumX2 = 0;
		double sumY = 0;
		double sumY2 = 0;
		int N=0;
		int M=0;

		for(int i = 1; i < track.size(); i++){
			sumX = sumX + getDistanceProductX(i, i) ;
			sumY = sumY + getDistanceProductY(i, i) ;
			N++;
			if(i < (track.size()-1)){
				sumX2 = sumX2 + getDistanceProductX(i, i+1) ;
				sumY2 = sumY2 + getDistanceProductY(i, i+1);
				M++;
			}
		}
		
		double msdX = (sumX/(N));
		double msdY = (sumY/(N));
		double covX = (sumX2/(M) );
		double covY = (sumY2/(M) );
		double termXA = msdX/2 * getFramesPerSecond();
		double termXB = covX * getFramesPerSecond() ;
		double termYA = msdY/2 * getFramesPerSecond();
		double termYB = covY * getFramesPerSecond() ;
		
		double D1 = termXA+termXB;	
		double D2 = termYA+termYB;
		double D = (D1+D2)/2;
		
		double[] data  = new double[3]; //[0] = Diffusioncoefficient, [1] = LocNoiseX, [2] = LocNoiseY
		data[0] = D;
		data[1] = R*msdX + (2*R-1)+covX;
		data[2] = R*msdY + (2*R-1)+covY;
		return data;
		
	}
	
	
	@Override
	void setup() {
		//No further setup needed!
		
	}

}
