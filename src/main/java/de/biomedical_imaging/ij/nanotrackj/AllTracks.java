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

import java.util.ArrayList;

@SuppressWarnings("serial")
public class AllTracks extends ArrayList<Track>  {
	
	private static AllTracks openTracks; 		//Tracks which are "active"
	private ArrayList<Track> finishedTracks; 	//Tracks which are finished
	
	
	private AllTracks(){
		finishedTracks = new ArrayList<Track>();
	}
	
	/**
	 * Method to finish a track by index
	 * @param index Index of the track in openTracks ArrayList which has finished.
	 */
	public void finishTrack(int index){
		Track trackToFinish = openTracks.remove(index);
		finishedTracks.add(trackToFinish);
	}
	
	/**
	 * Method to finish a track. The track specified by the parameter t will be removed from
	 * the opentracks arraylist and added to finished tracks list.
	 * @param t Track which is finished
	 */
	public void finishTrack(Track t){
		int index = openTracks.indexOf(t);
		if(index>=0){
		Track trackToFinish = openTracks.remove(index);
		finishedTracks.add(trackToFinish);
		}
	}
	
	/**
	 * Return a track specified by the track id
	 * @param id track id
	 * @return Track with the specified track id. If such a track is missing, the return is null.
	 */
	public Track getFinishedTrackByID(int id){
		for(int i = 0; i < finishedTracks.size(); i++){
			if(finishedTracks.get(i).getTrackID()==id){
				return finishedTracks.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Finishes all open tracks
	 */
	public void finishAllOpenTracks(){
		finishedTracks.addAll(openTracks);
		openTracks.clear();
	}
	
	/**
	 * @return Arraylist of finished tracks
	 */
	public ArrayList<Track>  getFinishedTracks(){
		return finishedTracks;
	}
	
	/**
	 * Estimates the Drift of the detected tracks
	 * @return Double Array with two entrys: [0] = Mean drift in x direction, [1] = Mean drift in y direction
	 */
	public double[] getDrift(){
		double[] drift = new double[2];
		drift[0] = 0;
		drift[1] = 0;
		double N = 0;
		
		// Calculate the drift for the finished tracks
		for(int i = 0; i < finishedTracks.size(); i++){
			Track currTrack = finishedTracks.get(i);
			if(currTrack.size()>5){
				for(int j = 1; j<currTrack.size(); j++){
					double dx = currTrack.get(j-1).getX()-currTrack.get(j).getX();
					drift[0] = drift[0]+ dx; 
					double dy = currTrack.get(j-1).getY()-currTrack.get(j).getY();
					drift[1] = drift[1]+ dy;
					N++;
				}
			}
		}
		// Add the drift from the open tracks
		for(int i = 0; i < openTracks.size(); i++){
			Track currTrack = openTracks.get(i);
			if(currTrack.size()>5){
				for(int j = 1; j<currTrack.size(); j++){
					double dx = currTrack.get(j-1).getX()-currTrack.get(j).getX();
					drift[0] = drift[0]+ dx;
					double dy = currTrack.get(j-1).getY()-currTrack.get(j).getY();
					drift[1] = drift[1]+ dy;
					N++;
				}
			}
		}
		drift[0] = drift[0]/N;
		drift[1] = drift[1]/N;
		return drift;
	}
	/**
	 * Singleton Pattern..
	 * @return Instance of the AllTracks class
	 */
	public static synchronized AllTracks getInstance(){
		if(openTracks==null){
			openTracks = new AllTracks();
			
		}
		return openTracks;
	}


}
