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


import java.awt.Polygon;
import java.awt.geom.Point2D;
import ij.blob.Blob;
/**
 * 
 * @author Thorsten Wagner, wagner@biomedical-imaging.de
 */
public class CenterBlob extends Blob {

	private Point2D.Float center;
	private float i0;					// Intensity
	private float hue = Float.NaN; 		// Hue for RGB Images
	
	public CenterBlob(Polygon outerContour, int label) {
		super(outerContour, label);
		// TODO Auto-generated constructor stub
	}
	/**
	 * @param x x coordinate of the center of gravity
	 * @param y y coordinate of the center of gravity
	 */
	public CenterBlob(float x, float y){
		super(new Polygon(),0);
		center = new Point2D.Float(x, y);;
	}
	
	/**
	 * @param x x coordinate of the center of gravity
	 * @param y y coordinate of the center of gravity
	 * @param hue hue of the blob
	 */
	public CenterBlob(float x, float y, float hue){
		super(new Polygon(),0);
		center = new Point2D.Float(x, y);;
		this.hue = hue;
	}
	/**
	 * @return The hue value (HSB color space)
	 */
	public float getHUE(){
		return hue;
	}
	
	@Override
	public Point2D.Float getCenterOfGravity() {
		// TODO Auto-generated method stub
		return center;
	}
	
	/**
	 * @return Intensity of the blob
	 */
	public float getIntensity(){
		return i0;
	}
	
	/**
	 * Setter for the Intensity
	 */
	public void setIntensity(float i0){
		this.i0 = i0;
	}
}
