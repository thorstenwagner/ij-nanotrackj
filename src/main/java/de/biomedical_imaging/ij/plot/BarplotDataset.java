package de.biomedical_imaging.ij.plot;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class BarplotDataset extends IDatasetCreator {

	@Override
	public IntervalXYDataset create(double[] data) {
		XYSeries xyseries = new XYSeries("");
		for(int i = 0; i<data.length;i++){
			xyseries.add(i, data[i]);
		}
        XYSeriesCollection xyseriescollection = new XYSeriesCollection(xyseries);
		return xyseriescollection;
	}

	@Override
	public IntervalXYDataset create(double[][] data) {
		XYSeries xyseries = new XYSeries("");
		for(int i = 0; i<data.length;i++){
			xyseries.add(data[i][0], data[i][1]);
		}
        XYSeriesCollection xyseriescollection = new XYSeriesCollection(xyseries);
		return xyseriescollection;
	}

}
