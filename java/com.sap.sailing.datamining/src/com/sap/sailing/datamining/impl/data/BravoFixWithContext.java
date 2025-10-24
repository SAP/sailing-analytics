package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasBravoFixContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Equality is based on the {@link #getBravoFix() Bravo fix} only.
 */
public class BravoFixWithContext implements HasBravoFixContext {
    private static final long serialVersionUID = 3452314555495774433L;

    private final HasTrackedLegOfCompetitorContext trackedLegOfCompetitorContext;
    
    private final BravoFix bravoFix;

    private Wind wind;

    public BravoFixWithContext(HasTrackedLegOfCompetitorContext trackedLegOfCompetitorContext, BravoFix bravoFix) {
        this.trackedLegOfCompetitorContext = trackedLegOfCompetitorContext;
        this.bravoFix = bravoFix;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bravoFix == null) ? 0 : bravoFix.hashCode());
        result = prime * result
                + ((trackedLegOfCompetitorContext == null) ? 0 : trackedLegOfCompetitorContext.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BravoFixWithContext other = (BravoFixWithContext) obj;
        if (bravoFix == null) {
            if (other.bravoFix != null)
                return false;
        } else if (!bravoFix.equals(other.bravoFix))
            return false;
        if (trackedLegOfCompetitorContext == null) {
            if (other.trackedLegOfCompetitorContext != null)
                return false;
        } else if (!trackedLegOfCompetitorContext.equals(other.trackedLegOfCompetitorContext))
            return false;
        return true;
    }

    @Override
    public Wind getWindInternal() {
        return wind;
    }

    @Override
    public void setWindInternal(Wind wind) {
        this.wind = wind;
    }

    @Override
    public Position getPosition() {
        return getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(getTimePoint(), /* extrapolate */ true);
    }

    @Override
    public HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext() {
        return trackedLegOfCompetitorContext;
    }

    @Override
    public BravoFix getBravoFix() {
        return bravoFix;
    }
    
    @Override
    public TimePoint getTimePoint() {
        return getBravoFix().getTimePoint();
    }
    
    @Override
    public TrackedRace getTrackedRace() {
        return getTrackedLegOfCompetitorContext().getTrackedRace();
    }
    
    @Override
    public SpeedWithBearing getSpeed() {
        return getGpsFixTrack().getEstimatedSpeed(getTimePoint());
    }

    @Override
    public Speed getVelocityMadeGood() {
        return getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getVelocityMadeGood(getTimePoint(), WindPositionMode.EXACT);
    }

    @Override
    public Wind getWind() {
        return HasBravoFixContext.super.getWind();
    }

    @Override
    public Bearing getTrueWindAngle() {
        return getTrackedRace().getTWA(getCompetitor(), getTimePoint());
    }

    @Override
    public Bearing getAbsoluteTrueWindAngle() {
        return getTrackedRace().getTWA(getCompetitor(), getTimePoint()).abs();
    }

    private Competitor getCompetitor() {
        return getTrackedLegOfCompetitorContext().getCompetitor();
    }

    private GPSFixTrack<Competitor, GPSFixMoving> getGpsFixTrack() {
        return getTrackedRace().getTrack(getTrackedLegOfCompetitorContext().getCompetitor());
    }
}
