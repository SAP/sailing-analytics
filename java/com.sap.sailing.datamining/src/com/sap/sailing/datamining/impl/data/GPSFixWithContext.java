package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasGPSFixContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;

public class GPSFixWithContext implements HasGPSFixContext {
    
    private final HasTrackedLegOfCompetitorContext trackedLegOfCompetitorContext;
    
    private final GPSFixMoving gpsFix;
    private Wind wind;

    public GPSFixWithContext(HasTrackedLegOfCompetitorContext trackedLegOfCompetitorContext, GPSFixMoving gpsFix) {
        this.trackedLegOfCompetitorContext = trackedLegOfCompetitorContext;
        this.gpsFix = gpsFix;
    }
    
    @Override
    public HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext() {
        return trackedLegOfCompetitorContext;
    }

    @Override
    public GPSFixMoving getGPSFix() {
        return gpsFix;
    }
    
    @Override
    public Wind getWind() {
        if (wind == null) {
            wind = getTrackedLegOfCompetitorContext().getTrackedLegContext().getTrackedRaceContext().getTrackedRace()
                    .getWind(gpsFix.getPosition(), gpsFix.getTimePoint());
        }
        return wind;
    }
    
}