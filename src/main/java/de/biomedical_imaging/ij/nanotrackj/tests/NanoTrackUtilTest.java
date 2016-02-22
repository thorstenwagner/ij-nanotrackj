package de.biomedical_imaging.ij.nanotrackj.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import de.biomedical_imaging.ij.nanotrackj.NanoTrackUtil;

public class NanoTrackUtilTest {

	@Test
	public void getHistogramTestDoNormalize(){
		double[][] data = new double[5][2];
		double[][] expHist = new double[9][2];
		for(int i = 0; i < 5; i++){
			data[i][0] = i*2;
			data[i][1] = 0.5;
		}
		
		for(int i = 0; i < 9; i++){
			expHist[i][0] = (i+1) - 0.5;
			if(i%2==0){
				expHist[i][1] = 0.2;
			}
			else
			{
				expHist[i][1] = 0;
			}
		}
		
		double[][] hist = NanoTrackUtil.getHistogram(data, 1);
		for(int i = 0; i < hist.length; i++){
			System.out.println("B " + hist[i][0] + " W " + hist[i][1]);
			assertEquals(expHist[i][0], hist[i][0],0);
			assertEquals(expHist[i][1], hist[i][1],0);
			
		}
		
		
	}
	
	@Test
	public void testGetHUERedIsZero() {
		int R = 255;
		int G = 0;
		int B = 0;
		double hue = NanoTrackUtil.getHUE(R, G, B);
		assertEquals(0, hue,0);
	}
	@Test
	public void testGetHUEGreenIs120() {
		int R = 0;
		int G = 255;
		int B = 0;
		double hue = NanoTrackUtil.getHUE(R, G, B);
		assertEquals(120, hue,0);
	}
	@Test
	public void testGetHUEBlueIs240() {
		int R = 0;
		int G = 0;
		int B = 255;
		double hue = NanoTrackUtil.getHUE(R, G, B);
		assertEquals(240, hue,0);
	}
	
	@Test
	public void testGetHUEGrayIsZero() {
		int R = 0;
		int G = 0;
		int B = 0;
		double hue = NanoTrackUtil.getHUE(R, G, B);
		assertEquals(0, hue,0);
	}
	
	@Test
	public void testGetHUEAlwaysPositive() {
		int R = 255;
		int G = 0;
		int B = 1;
		double hue = NanoTrackUtil.getHUE(R, G, B);
		assertTrue(hue>0);
	}

}
