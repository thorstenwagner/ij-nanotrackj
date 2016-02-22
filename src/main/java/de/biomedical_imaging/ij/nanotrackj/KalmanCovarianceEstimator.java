package de.biomedical_imaging.ij.nanotrackj;

import ij.IJ;
import ij.gui.GenericDialog;

public class KalmanCovarianceEstimator extends CovarianceEstimator {
	
	double R;
	Track track;
	@Override
	public double getDiffusionCoefficient(Track track, double driftX,
			double driftY) {
		this.track = track;
		Track t = getKalmanFilteredTrack(R);
		//IJ.log("KALMAN");
		return super.getDiffusionCoefficient(t, driftX, driftY);
	}
	
public Track getKalmanFilteredTrack(double R){
		
		//Get Drift
		double[] drift = AllTracks.getInstance().getDrift();
		/*
		CovarianceEstimator koest;
		if (diffCoeffEst instanceof CovarianceEstimator) {
			koest = (CovarianceEstimator) diffCoeffEst;
		}
		else {
			IJ.error("Kalman Filtering only works with covariance estimator");
			return(this);
		}
		*/
		//double R = 0.059333333;//(836.0/1500)/6;
		double[] locnoise = super.getLocalizationNoise(track, R, drift[0], drift[1]); 
		
		Track filteredTrack = new Track(track.getStartFrameIndex());
		
		double postX = track.get(0).getX();
		double postY = track.get(0).getY();
		double postMMSEX=locnoise[0];
		double postMMSEY=locnoise[1];
		double varDF = super.getDiffusionCoefficient(track, drift[0], drift[1])*2*getFramesPerSecond();
		double varNoiseX = locnoise[0];
		double varNoiseY = locnoise[1];
		
		CenterBlob cb = new CenterBlob((float)postX,(float)postY,((CenterBlob)track.get(0).getBlob()).getHUE());
		Step s = new Step(cb, track.get(0).getFrameIndex());
		filteredTrack.add(s);
		
		//Get drift corrected track
		for (int i = 1; i < track.size(); i++) {
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
			postX = priorX + Kx * (track.get(i).getX()+i*drift[0] - priorX); // 
			postY = priorY + Ky * (track.get(i).getY()+i*drift[1] - priorY); //
			
			cb = new CenterBlob((float)postX,(float)postY,((CenterBlob)track.get(i).getBlob()).getHUE());
			s = new Step(cb, track.get(i).getFrameIndex());
			filteredTrack.add(s);
			
			//Minimum MSE
			postMMSEX = (1-Kx)*priorMMSEX;
			postMMSEY = (1-Ky)*priorMMSEY;
		}
		
		return filteredTrack;
	}
	
	@Override
	void setup() {
		GenericDialog gd = new GenericDialog("Setup Kalman Filter");
		
		gd.addNumericField("Exposure Time [ms]", 1, 0);
		gd.showDialog();
		
		R = (gd.getNextNumber() / (1/getFramesPerSecond() * 1000))/6;
		//super.setup();
	}

}
