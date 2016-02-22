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

import java.awt.AWTEvent;
import java.awt.TextField;

import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;

public class NTADialogListener implements DialogListener {

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		boolean isAvalidTextfield=true;
		TextField textTemp=(TextField) (gd.getNumericFields().get(3));
		TextField textMinSize=(TextField) (gd.getNumericFields().get(1));
		TextField textRadius=(TextField) (gd.getNumericFields().get(0));
		TextField textNM=(TextField) (gd.getNumericFields().get(5));
		TextField textFPS=(TextField) (gd.getNumericFields().get(6));
		TextField source = null;
		double T=0;
		double andrade=0;
		try{
			source = ((TextField)e.getSource());
		}
		catch(Exception f){
			isAvalidTextfield=false;
		}
		if((isAvalidTextfield && source.equals(textTemp)) || 
				(isAvalidTextfield && source.equals(textMinSize)) ||
				(isAvalidTextfield && source.equals(textNM))||
				(isAvalidTextfield && source.equals(textFPS))){
			
			TextField help2 = (TextField) (gd.getNumericFields().get(4));
			try{
			T = Double.parseDouble(textTemp.getText());
			T = T + 273.15;
			andrade = Math.exp(-6.944+2036.8/T) * Math.pow(10, -12); //Andrade-Gleichung [kg nm^-1 s^-2]
			help2.setText(""+andrade* Math.pow(10, 12)) ;
			help2.setEditable(true);
			
			double minSize = Double.parseDouble(textMinSize.getText()); // [nm]
			double nmPerPixel = Double.parseDouble(textNM.getText());
			double fps = Double.parseDouble(textFPS.getText()); //[1/s]
			double kB = 1.3806488* Math.pow(10, -19) * Math.pow(10, 14); //[kg nm^2 s^-2 K^-1]
			double D = (kB * T)/(3*Math.PI*andrade*minSize); //(1.3806504 *10* T)/reibung;
			//IJ.log("D" + D);
			double sigma = Math.sqrt(Math.PI*D/fps);
			double radius = 3*sigma/nmPerPixel;//(2.55*Math.sqrt(4*D*(1.0/fps))*100)/nmPerPixel;
			textRadius.setText(""+IJ.d2s(radius, 2));
			
			}
			catch(Exception u){
				//Do nothing
			}
		}
		
			
		return true;
	}

}
