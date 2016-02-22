package de.biomedical_imaging.ij.SingleTrack;

import java.awt.Polygon;
import java.awt.Rectangle;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class SingleTrack_ implements PlugInFilter{
	
	Rectangle kernelCoord;
	ColorProcessor kernelImg;
	double[][] kernelArray;
	double kernelMean = 0;
	double kernelSD = 0;
	ColorProcessor currentFrame;
	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		kernelCoord = imp.getProcessor().getRoi();
		currentFrame = (ColorProcessor) imp.getProcessor();
		updateWeightedKernelArray(kernelCoord);
		
		return DOES_ALL | DOES_STACKS;
	}

	@Override
	public void run(ImageProcessor ip) {
		ip.resetRoi();
		currentFrame = (ColorProcessor) ip;
		
		int newX = 0;
		int newY = 0;
		int N = 0;
		double lastCorr = Double.MIN_VALUE;
		for(int x = kernelCoord.x-kernelCoord.width; x < kernelCoord.x+kernelCoord.width; x++){
			for(int y = kernelCoord.y-kernelCoord.height; y < kernelCoord.y+kernelCoord.height;y++){
				double c = getCorrelation(x, y);
				//IJ.log("C " + c + "x " + x + " y " + y);
				if(c>lastCorr){
					lastCorr=c;
					newX = x;
					newY = y;
					N=1;
				}else if(c==lastCorr){
					newX += x;
					newY += y;
					N++;
				}
			}
		}
		newX = newX/N;
		newY = newY/N;
		IJ.log("NX " + newX + " NY " + newY);
		
		kernelCoord.translate(newX-kernelCoord.x, newY-kernelCoord.y);
		updateWeightedKernelArray(kernelCoord);
		
	}
	
	public void updateWeightedKernelArray(Rectangle kernelCoord){
		currentFrame.resetRoi();
		ColorProcessor help = (ColorProcessor) currentFrame.duplicate();
		
		//Recenter
		/*
		float fitX = 0;
		float fitY = 0;
		float sumB = 0;
		for(int i = kernelCoord.x; i < kernelCoord.x + kernelCoord.width; i++){
			for(int j = kernelCoord.y; j < kernelCoord.y + kernelCoord.height; j++){
				int col = currentFrame.get(i, j);
				float b = getHSB(col)[2];
				fitX += i*b;
				fitY += j*b;
				sumB += b;
			}
		}
		fitX = fitX/sumB - kernelCoord.width/2;
		fitY = fitY/sumB - kernelCoord.height/2;
		IJ.log("FX " + fitX + " FY " + fitY);
		//kernelCoord.translate(, );
		kernelCoord.setLocation((int)(fitX), (int)(fitY));
		*/
		
		help.setRoi(kernelCoord);
		MaximumFinder mf = new MaximumFinder();
		boolean excludeOnEdges = true;
		Polygon maximas = mf.getMaxima(help, 10, excludeOnEdges);
		help.resetRoi();
		kernelCoord.setLocation(maximas.xpoints[0]- kernelCoord.width/2, maximas.ypoints[0]- kernelCoord.height/2);
		IJ.log("X " + kernelCoord.x + " FY " + kernelCoord.y);
		help.setRoi(kernelCoord);
		kernelImg = (ColorProcessor) help.crop();
		kernelArray = new double[kernelImg.getWidth()][kernelImg.getHeight()];
		
		SummaryStatistics s = new SummaryStatistics();
		for(int i = 0; i < kernelImg.getWidth(); i++){
			for(int j = 0; j < kernelImg.getHeight(); j++){
				kernelArray[i][j] = HxB(kernelImg.get(i, j));
				
				s.addValue(kernelArray[i][j]);
			}
		}
		kernelMean = s.getMean();
		kernelSD = s.getStandardDeviation();
	}
	public float[] getHSB(int color){
		int r = (color>>16)&255;
		int g = (color>>8)&255;
		int b = (color)&255;
		float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
		return hsb;
	}
	
	public double HxB(int color){
		float[] hsb = getHSB(color);
		return hsb[0]*hsb[1];
	}
	
	public double getCorrelation(int x, int y){
		SummaryStatistics s = new SummaryStatistics();
		double[][] windowedFrame = new double[kernelImg.getWidth()][kernelImg.getHeight()];
		for(int i = 0; i < kernelImg.getWidth(); i++){
			for(int j = 0; j < kernelImg.getHeight(); j++){
				windowedFrame[i][j] = HxB(currentFrame.get(x+i, y+j));
				s.addValue(windowedFrame[i][j]);
			}
		}
		double windowedFrameMean = s.getMean();
		double windowedFrameSD = s.getStandardDeviation();
		double corrcoeff = 0;
		for(int i = 0; i < kernelImg.getWidth(); i++){
			for(int j = 0; j < kernelImg.getHeight(); j++){
				corrcoeff += (kernelArray[i][j]-kernelMean)*(windowedFrame[i][j]-windowedFrameMean);
			}
		}
		int N = kernelImg.getWidth()*kernelImg.getHeight();
		corrcoeff = corrcoeff/((N-1)*windowedFrameSD*kernelSD);
		return corrcoeff;
	}

}
