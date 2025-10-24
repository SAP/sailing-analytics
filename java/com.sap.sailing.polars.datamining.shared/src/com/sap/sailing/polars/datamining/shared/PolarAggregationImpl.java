package com.sap.sailing.polars.datamining.shared;

import java.util.HashMap;
import java.util.Map;



public class PolarAggregationImpl implements PolarAggregation {
    
    private static final long serialVersionUID = 9177124509619315748L;
    private double[] sumSpeedsPerAngle = new double[360];
    private int[] countPerAngle = new int[360];
    /**
     * FIXME Right now the histogram data is only valid, if the results are grouped by windrange. Otherwise the
     * column-indices of different ranges are mixed in one histogram.
     * <p>
     * 
     * Keys are the angles in degrees (0...359). The {@link Double} key of the inner map is the histogram x-value (e.g.
     * 5.0 for wind speed 4.5...5.5 with step 1.0).
     */
    private Map<Integer, Map<Double, Integer>> histogramData;
    private int count = 0;
    private PolarDataMiningSettings settings;
    
    public PolarAggregationImpl() {
        //GWT
    }
    
    public PolarAggregationImpl(PolarDataMiningSettings polarDataMiningSettings) {
        this.settings = polarDataMiningSettings;
        histogramData = new HashMap<>();
        for (int i = 0; i < 360; i++) {
            histogramData.put(i, new HashMap<Double, Integer>());
        }
    }

    @Override
    public void addElement(PolarStatistic dataEntry) {
        long roundedAngleDeg = Math.round(dataEntry.getTrueWindAngleDeg());
        int angleDeg = (int) roundedAngleDeg;
        if (angleDeg < 0) {
            angleDeg = (360 + angleDeg);
        }
        sumSpeedsPerAngle[angleDeg] += dataEntry.getBoatSpeed().getKnots();
        countPerAngle[angleDeg]++;
        Double histogramXValue = settings.getWindSpeedStepping().getHistogramXValue(settings.getNumberOfHistogramColumns(),
                dataEntry.getWindSpeed().getKnots());
        Map<Double, Integer> histDataForAngle = histogramData.get(angleDeg);
        if (!histDataForAngle.containsKey(histogramXValue)) {
            histDataForAngle.put(histogramXValue, 0);
        }
        Integer currentCount = histDataForAngle.get(histogramXValue) + 1;
        histDataForAngle.put(histogramXValue, currentCount);
        count++;
    }
    
    @Override
    public double[] getAverageSpeedsPerAngle() {
        double[] averages = new double[360];
        for (int i = 0; i < 360; i++) {
            if (countPerAngle[i] > 0) {
                averages[i] = sumSpeedsPerAngle[i] / countPerAngle[i];
            }
        }
        return averages;
    }

    @Override
    public int[] getCountPerAngle() {
        return countPerAngle;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public PolarDataMiningSettings getSettings() {
        return settings;
    }

    @Override
    public Map<Integer, Map<Double, Integer>> getCountHistogramPerAngle() {
        return histogramData;
    }
}
