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

import java.awt.Rectangle;

import ij.ImagePlus;
import ij.gui.EllipseRoi;
import ij.measure.ResultsTable;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class NanoTrackUtil {
	/**
	 * Calculate the Histogram for specific bin width
	 * @param data [k][0] = the data from which a histogram is calculated, [k][1] weight of the entry
	 * @param binSize binSize in units of data
	 * @return histogram [i][0] = bin, [i][1] = weight
	 */
	public static double[][] getHistogram(double[][] data, double binSize){
		double maxData = Double.MIN_NORMAL;
		double sumOfWeights = 0;
		for(int i = 0; i < data.length; i++){
			//Calculate Sum for Normalization
			sumOfWeights += data[i][1];
			
			//Find Maxima
			if(data[i][0] > maxData){
				maxData = data[i][0];
			}
		}
		
		//Normalization
		for(int i = 0; i < data.length; i++){
			data[i][1] = data[i][1] / sumOfWeights;
		}
		
		int binNumber = (int)Math.ceil(maxData/binSize) + 1;
		double[][] histogram = new double[binNumber][2]; //
		for(int i = 0; i < data.length; i++){
			int index = (int)Math.floor(data[i][0]/binSize);
			histogram[index][1] = histogram[index][1] + data[i][1];
		}
		
		for(int i = 0; i < histogram.length; i++){
			histogram[i][0] = ((i+1)*binSize-binSize/2);
		}
		
		return histogram;
	}
	
	
	/**
	 * Fills Histogram Data into a Result Table
	 * @param data Histogram Data, data[i][0] = Bin data[i][1] = Probablity
	 * @param title Title of the result table
	 */
	public static void outputHistogramData(double[][] data, String title, String xtitle, String ytitle){
		 ResultsTable rt = new ResultsTable();
		 
		 for(int i = 0; i < data.length; i++){
			rt.incrementCounter();
			rt.addValue(xtitle, data[i][0]);
			rt.addValue(ytitle, data[i][1]);
		 }
		 rt.show(title);
	}
	
	/**
	 * Checks if a sign of a value is unchanged.
	 * @param ldx Last value
	 * @param dx Current value
	 * @return True if the sign has not changed.
	 */
	private static boolean signUnChanged(int ldx, int dx){
		if(ldx==Integer.MIN_VALUE){
			return true;
		}
		if(ldx*dx < 0){
			return false;
		}
		return true;
	}
	
	/**
	 * Estimates the width of the central maximum of the diffraction pattern by searching the first maxima of the second drivative in x direction
	 * @param ip The image which contains the diffraction pattern
	 * @param xc Estimated x-coordinate of the center of gravity of the central maximum
	 * @param yc Estimated y-coordinate of the center of gravity of the central maximum
	 * @param direction direction > 0 -> Right, Direction < 0 -> Left
	 * @return Radius of central maximum
	 */
	private static int findRadius(ImageProcessor ip, double xc, double yc, int direction){
		int x = (int)xc;
		int y = (int)yc;
		int c = 0;
		int ldx = Integer.MIN_VALUE;
		int dx = Integer.MIN_VALUE;
		if(Math.abs(direction)!=1){
			direction = (int)Math.signum(direction);
		}
		do{
			ldx = dx;
			dx = 2*ip.get(x + (direction*c), y)-ip.get(x + (direction*c) -1, y)-ip.get(x + (direction*c) +1, y);
			c++;
		}while(c<=30 && signUnChanged(ldx,dx) );
		return c;
	}
	/**
	 * Estimates the Parameter of a Gaussian for the central maximum of a diffraction pattern
	 * @param ip The image which contains the diffraction pattern
	 * @param xc Estimated x-coordinate of the center of gravity of the central maximum
	 * @param yc Estimated y-coordinate of the center of gravity of the central maximum
	 * @return Return the parameters of Gaussian Distribution as float array p:
	 * p[0] x-coordinate of the center of gravity, p[1] y-coordinate of the center of gravity
	 */
	public static float[] getFittedParameter(ImageProcessor ip, double xc, double yc){
		int radius1 = findRadius(ip, xc, yc,1);
		int radius2 = findRadius(ip, xc, yc,-1);
		int radius = Math.max(radius1, radius2);

		EllipseRoi circleRoi = new EllipseRoi(xc-radius, yc-radius, xc+radius, yc+radius, 1);
		ImagePlus imp = new ImagePlus("", ip);
		circleRoi.setImage(imp);
		
		ImageProcessor mask = circleRoi.getMask();
		Rectangle r = circleRoi.getBounds();
		double sumx = 0;
		double sumy = 0;
		double sumWeight = 0;

		for (int y=0; y<r.height; y++) {
			for (int x=0; x<r.width; x++) {
				if (mask==null||mask.getPixel(x,y)!=0) {
					float weight = (int)ip.getPixelValue(x+r.x, y+r.y);					
					sumWeight += weight;
					sumx += (x+r.x)*weight;
					sumy += (y+r.y)*weight;
				}
			}
		}

		float[] para = new float[2];
		para[0] = (float)(sumx/sumWeight);
		para[1] = (float)(sumy/sumWeight);
		return para;
	}
	
	private static String whichIsMax(int R, int G, int B){
		if(R > G && R > B) return "R";
		if(G > R && G > B) return "G";
		if(B > G && B > R) return "B";
		return "";
	}
	
	public static float meanHUE(ColorProcessor ip, int x, int y){
		int[] rgb = new int[3];
		float mean = 2*getHUE(ip.getPixel(x, y,rgb))+
				getHUE(ip.getPixel(x+1, y,rgb)) + 
				getHUE(ip.getPixel(x-1, y,rgb)) +
				getHUE(ip.getPixel(x, y+1,rgb)) +
				getHUE(ip.getPixel(x, y-1,rgb)) +
				getHUE(ip.getPixel(x+1, y+1,rgb)) +
				getHUE(ip.getPixel(x-1, y-1,rgb))+
				getHUE(ip.getPixel(x-1, y+1,rgb))+
				getHUE(ip.getPixel(x+1, y-1,rgb));
		mean = mean / 10;
		return mean;
		
	}
	
	public static float meanIntensity(ImageProcessor ip, int x, int y){
		float mean = 2*ip.getPixel(x, y)+
				ip.getPixel(x+1, y) + 
				ip.getPixel(x-1, y) +
				ip.getPixel(x, y+1) +
				ip.getPixel(x, y-1) +
				ip.getPixel(x+1, y+1) +
				ip.getPixel(x-1, y-1)+
				ip.getPixel(x-1, y+1)+
				ip.getPixel(x+1, y-1);
		mean = mean / 10;
		return mean;
		
	}
	
	public static float meanIntensity(ColorProcessor ip, int x, int y){
		int[] rgb = new int[3];
		float mean = 2*getHSBBrightness(ip.getPixel(x, y,rgb))+
				getHSBBrightness(ip.getPixel(x+1, y,rgb)) + 
				getHSBBrightness(ip.getPixel(x-1, y,rgb)) +
				getHSBBrightness(ip.getPixel(x, y+1,rgb)) +
				getHSBBrightness(ip.getPixel(x, y-1,rgb)) +
				getHSBBrightness(ip.getPixel(x+1, y+1,rgb)) +
				getHSBBrightness(ip.getPixel(x-1, y-1,rgb))+
				getHSBBrightness(ip.getPixel(x-1, y+1,rgb))+
				getHSBBrightness(ip.getPixel(x+1, y-1,rgb));
		mean = mean / 10;
		return mean;
		
	}
	
	public static float getHSBBrightness(int[] rgb){
		return getHSBBrightness(rgb[0],rgb[1],rgb[2]);
	}
	
	public static float getHSBBrightness(int R, int G, int B){
		int max = Math.max(Math.max(R, G),B);
		return max;
	}

	public static float getHUE(int[] rgb){
		return getHUE(rgb[0],rgb[1],rgb[2]);
	}
	
	public static float getHUE(int R, int G, int B){
		int max = Math.max(Math.max(R, G),B);
		int min = Math.min(Math.min(R, G),B);
		if(max==min) return 0;
		float  hue = 0;
		String maxChannel = whichIsMax(R, G, B);
		if(maxChannel=="R") {
			hue = 60*(((float)(G-B))/(max-min));
		}
		else if(maxChannel=="G"){
			hue = 60*(2+ ((float)(B-R))/(max-min));
		}
		else if(maxChannel=="B"){
			hue = 60*(4+ ((float)(R-G))/(max-min));
		}
		if(hue<0){
			hue = hue + 360;
		}
		return hue;
	}

}