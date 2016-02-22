package de.biomedical_imaging.ij.plot;

import org.jfree.data.xy.IntervalXYDataset;

public abstract class IDatasetCreator {
	
	abstract public IntervalXYDataset create(double[] data);
	
	abstract public IntervalXYDataset create(double[][] data);

}
