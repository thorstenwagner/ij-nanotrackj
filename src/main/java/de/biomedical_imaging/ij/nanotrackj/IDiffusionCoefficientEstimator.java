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

public abstract class IDiffusionCoefficientEstimator {
	private double framesPerSecond = 30.0;
	
	/**
	 * Calculates the diffusion coefficient
	 * @param track Particle trajectory 
	 * @param driftx The mean drift in x direction
	 * @param drifty The mean drift in y direction
	 * @return the diffusion coefficient (pixel^2 / s)
	 */
	public abstract double getDiffusionCoefficient(Track track, double driftx, double drifty);
	
	
	public void setFramesPerSecond(double fr){
		framesPerSecond=fr;
	}
	
	public double getFramesPerSecond(){
		return framesPerSecond;
	}
	
	/**
	 * Method for setup the diffusion coefficient estimator
	 */
	abstract void setup();

}
