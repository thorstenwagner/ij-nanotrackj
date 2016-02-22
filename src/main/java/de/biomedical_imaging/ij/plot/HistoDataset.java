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

package de.biomedical_imaging.ij.plot;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.xy.IntervalXYDataset;

public class HistoDataset extends IDatasetCreator {
	
	@Override
	public IntervalXYDataset create(double[] data) {
		HistogramDataset histogramdataset = new HistogramDataset();
        double max = getMaxValue(data);
        double min = getMinValue(data);
        double diff = max-min;
        int bins = (int)(diff/4);
        if(bins ==0){
        	bins=1;
        }
        
        histogramdataset.addSeries("", data, bins);
        histogramdataset.setType(HistogramType.RELATIVE_FREQUENCY);
  

        return histogramdataset;
	}
	@Override
	public IntervalXYDataset create(double[][] data) {
		// TODO Auto-generated method stub
		return null;
	}
	
    public static double getMaxValue(double[] numbers){
  	  double maxValue = numbers[0];
  	  for(int i=1;i < numbers.length;i++){
  	    if(numbers[i] > maxValue){
  		  maxValue = numbers[i];
  		}
  	  }
  	  return maxValue;
  	}

  	public static double getMinValue(double[] numbers){
  	  double minValue = numbers[0];
  	  for(int i=1;i<numbers.length;i++){
  	    if(numbers[i] < minValue){
  		  minValue = numbers[i];
  		}
  	  }
  	  return minValue;
  	}

}
