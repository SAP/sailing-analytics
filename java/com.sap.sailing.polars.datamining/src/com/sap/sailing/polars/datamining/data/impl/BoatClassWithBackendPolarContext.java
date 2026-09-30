package com.sap.sailing.polars.datamining.data.impl;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.polars.datamining.data.HasBackendPolarBoatClassContext;
import com.sap.sse.common.impl.KnotSpeedImpl;

public class BoatClassWithBackendPolarContext implements HasBackendPolarBoatClassContext {

    private BoatClass boatClass;
    private PolarDataService polarDataService;

    public BoatClassWithBackendPolarContext(BoatClass bc, PolarDataService polarDataService) {
        this.boatClass = bc;
        this.polarDataService = polarDataService;
    }

    @Override
    public BoatClass getBoatClass() {
        return boatClass;
    }

    @Override
    public PolarDataService getPolarDataService() {
        return polarDataService;
    }
    
    public Double getTargetBeatAngle() {
        try {
            return polarDataService.getAverageSpeedWithTrueWindAngle(boatClass, new KnotSpeedImpl(12), LegType.UPWIND, Tack.STARBOARD).getObject().getBearing().getDegrees();
        } catch (NotEnoughDataHasBeenAddedException e) {
            return null;
        }
    }
    
    public Double getTargetRunawayAngle() {
        try {
            return polarDataService.getAverageSpeedWithTrueWindAngle(boatClass, new KnotSpeedImpl(12), LegType.DOWNWIND, Tack.STARBOARD).getObject().getBearing().getDegrees();
        } catch (NotEnoughDataHasBeenAddedException e) {
            return null;
        }
    }

    @Override
    public HasBackendPolarBoatClassContext getBackendPolarBoatClassContext() {
        return this;
    }

}
