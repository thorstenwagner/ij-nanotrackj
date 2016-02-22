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

import java.awt.geom.Point2D;


import ij.blob.*;

public class Step {

	private Blob blob; //The detected "blob"
	private int frame; //The frame in which the blob was detected
	
	public Step(Blob blob, int frame) {
		this.blob = blob;
		this.frame = frame;
	}
	
	public Point2D getCenter() {
		return blob.getCenterOfGravity();
	}
	
	public double getX() {
		return blob.getCenterOfGravity().getX();
	}
	
	public double getY() {
		return blob.getCenterOfGravity().getY();
	}
	
	public Blob getBlob() {
		return blob;
	}
	
	public int getFrameIndex(){
		return frame;
	}
	
}
