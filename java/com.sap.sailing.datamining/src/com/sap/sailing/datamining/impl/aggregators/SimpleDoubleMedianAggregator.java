package com.sap.sailing.datamining.impl.aggregators;

public class SimpleDoubleMedianAggregator extends SimpleMedianAggregator<Double> {

    @Override
    protected Double add(Double value1, Double value2) {
        return value1 + value2;
    }

    @Override
    protected Double divideByTwo(Double value) {
        return value / 2.0;
    }

}
