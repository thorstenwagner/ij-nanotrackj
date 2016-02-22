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
import ij.blob.*;
import java.util.ArrayList;
import java.util.List;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class TrackSynthesizerKDTree {
	private double searchradius;
	
	public TrackSynthesizerKDTree(double radius) {
		searchradius = radius;
		// TODO Auto-generated constructor stub
	}
	
	public void archiveOpenTracks(){
		AllTracks.getInstance().finishAllOpenTracks();
	}
	
	public void updateTracks(ArrayList<Blob> sortedBlobs, int frameIndex){
		
		AllTracks allTracks = AllTracks.getInstance();
		if(frameIndex==1){
			allTracks.clear();
			allTracks.getFinishedTracks().clear();
		}

		KDTree<Track> kdOpenTracks = new KDTree<Track>(2);
		for(int i = 0; i < allTracks.size(); i++){
			Blob lastBlobOfTrack =  allTracks.get(i).getLastBlob();
			double[] coord = new double[2];
			coord[0] = lastBlobOfTrack.getCenterOfGravity().getX();
			coord[1] = lastBlobOfTrack.getCenterOfGravity().getY();
			
			try {
				kdOpenTracks.insert(coord, allTracks.get(i));
			} catch (KeySizeException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (KeyDuplicateException e) {
				// TODO Auto-generated catch block
				IJ.log("INSERT: " + frameIndex + " "+ coord[0] + " " + coord[1]);
				IJ.log("AREA: " + lastBlobOfTrack.getEnclosedArea());
			//	e.printStackTrace();
			}
		}
		
		for(int i = 0; i < sortedBlobs.size(); i++){
			double[] targ = new double[2];
			targ[0] = sortedBlobs.get(i).getCenterOfGravity().getX();
			targ[1] = sortedBlobs.get(i).getCenterOfGravity().getY();
			List<Track> cand = null;
			try {
				cand = kdOpenTracks.nearestEuclidean(targ, searchradius);
			} catch (KeySizeException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (IllegalArgumentException e) {
				//IJ.log("INSERT: " + frameIndex + " "+ targ[0] + " " + targ[1]);
				//IJ.log("AREA: " + sortedBlobs.get(i).getEnclosedArea());
				//e.printStackTrace();
			}
			if(cand==null){
				Track t = new Track(frameIndex);
				Step s = new Step(sortedBlobs.get(i),frameIndex);
				t.add(s);
				allTracks.add(t);
			}
			else if(cand.size()>1){
				for(int j = 0; j < cand.size(); j++){
				allTracks.finishTrack(cand.get(j));
				}
			}
			else if(cand.size()==1){
				//boolean firstInside = isInsideRadius(cand.get(0).getLastBlob(), sortedBlobs.get(i), searchradius);
				//if(firstInside){
					Step s = new Step(sortedBlobs.get(i),frameIndex);
					cand.get(0).add(s);
				//}
			}else{
				//IJ.log("!!");
				Track t = new Track(frameIndex);
				Step s = new Step(sortedBlobs.get(i),frameIndex);
				t.add(s);
				allTracks.add(t);
			}
		
		}
		
		//Schließe alle nicht geupdateden tracks...
		for(int i = 0; i < allTracks.size(); i++){
			if(allTracks.get(i).getEndFrameIndex() < frameIndex){
				allTracks.finishTrack(i);
			}
		}
	}

		
	private boolean isInsideRadius(Blob lastBlobInTrack, Blob testBlob, double radius){
		double d = lastBlobInTrack.getCenterOfGravity().distance(testBlob.getCenterOfGravity());
		return d < radius;
	}

	
}
