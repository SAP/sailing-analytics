package com.sap.sailing.datamining.impl;

import com.sap.sailing.domain.base.Moving;

public class SpeedInKnotsExtractor extends AbstractExtractionWorker<Moving, Double> {

    @Override
    public Double extract(Moving dataEntry) {
        return dataEntry.getSpeed().getKnots();
    }

}