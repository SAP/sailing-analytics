package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.datamining.annotations.Dimension;

public interface HasWindOnTrackedLegOfCompetitor extends HasTrackedLegOfCompetitor, HasWind {
    default Wind getWind() {
        if (getWindInternal() == null) {
            setWindInternal(getTrackedRace().getWind(getPosition(), getTimePoint()));
        }
        return getWindInternal();
    }

    @Dimension(messageKey = "Tack")
    default Tack getTack() throws NoWindException {
        return getTrackedRace().getTack(getTrackedLegOfCompetitorContext().getCompetitor(), getTimePoint());
    }
    
    default TrackedRace getTrackedRace() {
        return getTrackedLegOfCompetitorContext().getTrackedLegContext().getTrackedRaceContext().getTrackedRace();
    }
    
    Wind getWindInternal();
    
    void setWindInternal(Wind wind);
}
