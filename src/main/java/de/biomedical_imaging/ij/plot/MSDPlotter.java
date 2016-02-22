package de.biomedical_imaging.ij.plot;

import ij.measure.ResultsTable;

import java.awt.Paint;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import de.biomedical_imaging.ij.nanotrackj.AllTracks;
import de.biomedical_imaging.ij.nanotrackj.NanoTrackJ_;
import de.biomedical_imaging.ij.nanotrackj.Track;

@SuppressWarnings("serial")
public class MSDPlotter extends JFrame {
	
	private int trackid;
	private int maxlag;
	
	public MSDPlotter(String title, int trackid,int maxlag){
		super(title);
		this.trackid = trackid;
		this.maxlag = maxlag;
		JPanel chartPanel = createDemoPanel();
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
	}

    
    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     * 
     * @return A panel.
     */
    public JPanel createDemoPanel() {
    	XYSeries series1 = new XYSeries("");
    	//XYIntervalSeries series1 = new XYIntervalSeries("");
    	Track track = AllTracks.getInstance().getFinishedTrackByID(trackid);
    	SimpleRegression reg = new SimpleRegression(false);
    	double hz = 1.0/NanoTrackJ_.getInstance().getFramerate();
    	ResultsTable trackdata = new ResultsTable();
    	
    	for(int i = 0; i < maxlag; i++){
    		
    		double lag = (i+1)*hz;
    		double msd = track.getMeanSquareDisplacement(true, i+1);
    		reg.addData(lag, msd);
    		series1.add(lag,msd);
    		
    		trackdata.incrementCounter();
    		trackdata.addValue("Timelag", lag);
    		trackdata.addValue("MSD", msd);
    		
    		double[] sdAndN = track.getMeanSquareDisplacementSD(true, i+1);
    		trackdata.addValue("N",sdAndN[1]);
    		//trackdata.addValue("Standard Deviation", sdAndN[0]);
    		//trackdata.addValue("Standard Error", sdAndN[0]/);
    	}
    	trackdata.show("Track Data (TID: " + trackid + ")");
    	XYSeriesCollection data = new XYSeriesCollection(series1);
    	//XYIntervalSeriesCollection data = new XYIntervalSeriesCollection();
	    XYSeries regr = new XYSeries("");
			regr.add(0, 0);
			regr.add(maxlag*hz, reg.getSlope()*maxlag*hz);
		XYSeriesCollection regcoll = new XYSeriesCollection(regr);
	
	     
        JFreeChart chart = ChartFactory.createScatterPlot("MSD Plot of track " + trackid + " upto the dimensionless timelag " + maxlag, "Time lags [s]", "MSD [Pixel^2/s]", data,
				PlotOrientation.VERTICAL, false, false, false);
		XYPlot plot = (XYPlot) chart.getPlot();
		XYItemRenderer scatterRenderer = plot.getRenderer();
		StandardXYItemRenderer regressionRenderer = new StandardXYItemRenderer();
		
		regressionRenderer.setBaseSeriesVisibleInLegend(false);
		
		plot.setDataset(1, regcoll);
		plot.setRenderer(1, regressionRenderer);
		DrawingSupplier ds = plot.getDrawingSupplier();
		for (int i = 0; i < data.getSeriesCount(); i++) {
			Paint paint = ds.getNextPaint();
			scatterRenderer.setSeriesPaint(i, paint);
			regressionRenderer.setSeriesPaint(i, paint);
		}
		ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPopupMenu(null);
        
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);
        return chartPanel;
    }
}
