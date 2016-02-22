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

import ij.blob.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TrackSynthesizer {
	private double searchradius;
	
	public TrackSynthesizer(double radius) {
		searchradius = radius;
		// TODO Auto-generated constructor stub
	}
	
	public void archiveOpenTracks(){
		AllTracks.getInstance().finishAllOpenTracks();
	}
	
	public void updateTracks(ArrayList<Blob> blobs, int frameIndex){
		
		AllTracks allTracks = AllTracks.getInstance();
		if(frameIndex==1){
			allTracks.clear();
			allTracks.getFinishedTracks().clear();
		}

		HashMap<Blob,ArrayList<Track>> mapBlobToTrack = new HashMap<Blob,ArrayList<Track>> ();
		HashMap<Track,ArrayList<Blob>> mapTrackToBlob = new HashMap<Track,ArrayList<Blob>> ();

		//Match all candidate Blobs to Tracks.
		for (Track track : allTracks) 
		{
			// Get the last blob from track
			Blob lastBlobOfTrack =  track.getLastBlob();
			
			ArrayList<Blob> candidateBlobs = mapTrackToBlob.get(track);
			
			for (Blob candBlob : blobs)
			{
				//A blob could only be assigned if the distance to the last blob in track is smaller than the searchradius.
				if(isInsideRadius(lastBlobOfTrack, candBlob,searchradius)){
					
					//Gibt es schon zu diesem Kandidaten Tracks die ihn enthalten?
					ArrayList<Track> candidateTracks = mapBlobToTrack.get(candBlob);
					if(candidateTracks==null){
						//Existiert keine solche Liste, lege eine neue für diesen Kandidaten an
						candidateTracks = new ArrayList<Track>();
						candidateTracks.add(track);
						mapBlobToTrack.put(candBlob, candidateTracks);
					}
					else {
						//Ansonsten füge den Track hinzu
						candidateTracks.add(track);
					}
					
					if(candidateBlobs == null){
						candidateBlobs = new ArrayList<Blob>();
						candidateBlobs.add(candBlob);
						mapTrackToBlob.put(track, candidateBlobs);
					}
					else
					{
						candidateBlobs.add(candBlob);
					}
					
				}
			}
		}
		
		//Assign a blob to a track
		for (Blob blobToAssign : blobs)
		{
			ArrayList<Track> listOfCandidateTracks = mapBlobToTrack.get(blobToAssign);

			Step s = new Step(blobToAssign,frameIndex);
			
			if(listOfCandidateTracks==null){
				//Neuer Track
				Track t = new Track(frameIndex);
				t.add(s);
				allTracks.add(t);
			}
			else if(listOfCandidateTracks.size() == 1) {
				// Track fortsetzen
				ArrayList<Blob> listOfCandidateBlobs = mapTrackToBlob.get(listOfCandidateTracks.get(0));
				if(listOfCandidateBlobs.size()==1){
					listOfCandidateTracks.get(0).add(s);
				}
			}

		}
		
		//Schließe alle nicht geupdateden tracks...
		for(int i = 0; i < allTracks.size(); i++){
			if(allTracks.get(i).getEndFrameIndex() < frameIndex){
				allTracks.finishTrack(i);
			}
		}
		
	}

	/**
	 * Checks if the distance between blob a and b is smaller as a specific radius
	 * @return True if the distance is smaller than the specified radius.
	 */
	private boolean isInsideRadius(Blob a, Blob b, double radius){
		double d = a.getCenterOfGravity().distance(b.getCenterOfGravity());
		return d < radius;
	}
	
}
