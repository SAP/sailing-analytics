package com.sap.sailing.polars.datamining.shared;

import java.io.Serializable;
import java.util.Map;

public interface PolarAggregation extends Serializable {

    void addElement(PolarStatistic dataEntry);

    double[] getAverageSpeedsPerAngle();
    
    int[] getCountPerAngle();
    
    int getCount();
    
    PolarDataMiningSettings getSettings();
    
    /**
     * Keys are the angles in degrees (0...359). The {@link Double} key of the inner map is the histogram x-value (e.g.
     * 5.0 for wind speed 4.5...5.5 with step 1.0).
     */
    Map<Integer, Map<Double, Integer>> getCountHistogramPerAngle();
    
}
