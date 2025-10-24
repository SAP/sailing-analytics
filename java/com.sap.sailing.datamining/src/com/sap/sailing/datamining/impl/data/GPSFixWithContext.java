package com.sap.sailing.datamining.impl.data;

import java.util.function.BiFunction;

import com.sap.sailing.datamining.data.HasGPSFixContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TackType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Equality is based on the {@link #getGPSFix() GPS fix} only.
 */
public class GPSFixWithContext implements HasGPSFixContext {
    private static final long serialVersionUID = -2808861038064003352L;
    private final HasTrackedLegOfCompetitorContext trackedLegOfCompetitorContext;
    private final GPSFixMoving gpsFix;
    private Wind wind;

    public GPSFixWithContext(HasTrackedLegOfCompetitorContext trackedLegOfCompetitorContext, GPSFixMoving gpsFix) {
        this.trackedLegOfCompetitorContext = trackedLegOfCompetitorContext;
        this.gpsFix = gpsFix;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gpsFix == null) ? 0 : gpsFix.hashCode());
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
        GPSFixWithContext other = (GPSFixWithContext) obj;
        if (gpsFix == null) {
            if (other.gpsFix != null)
                return false;
        } else if (!gpsFix.equals(other.gpsFix))
            return false;
        return true;
    }

    @Override
    public TimePoint getTimePoint() {
        return getGPSFix().getTimePoint();
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
        return getGPSFix().getPosition();
    }

    @Override
    public TrackedRace getTrackedRace() {
        return getTrackedLegOfCompetitorContext().getTrackedRace();
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
    public Bearing getTrueWindAngle() throws NoWindException {
        return getTrackedRace().getTWA(getTrackedLegOfCompetitorContext().getCompetitor(), getTimePoint());
    }

    @Override
    public Bearing getAbsoluteTrueWindAngle() throws NoWindException {
        return getTrackedRace().getTWA(getTrackedLegOfCompetitorContext().getCompetitor(), getTimePoint()).abs();
    }

    @Override
    public SpeedWithBearing getVelocityMadeGood() {
        return getTrackedRace().getVelocityMadeGood(getTrackedLegOfCompetitorContext().getCompetitor(), getTimePoint());
    }

    @Override
    public Distance getXTE() {
        return getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getSignedCrossTrackError(getTimePoint());
    }

    @Override
    public Distance getAbsoluteXTE() {
        return getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getAbsoluteCrossTrackError(getTimePoint());
    }
    
    @Override
    public Double getRelativeXTESigned() {
        return getRelativeXTE(TrackedLegOfCompetitor::getSignedCrossTrackError);
    }
    
    private Double getRelativeXTE(final BiFunction<TrackedLegOfCompetitor, TimePoint, Distance> xteFunction) {
        final Double result;
        // The XTE is calculated relative to half the leg length
        final Distance xte = xteFunction.apply(getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor(), getTimePoint());
        if (xte == null) {
            result = null;
        } else {
            final Distance legLength = getTrackedLegOfCompetitorContext().getTrackedLegContext().getTrackedLeg().getGreatCircleDistance(getTimePoint());
            if (legLength == null) {
                result = null;
            } else {
                result = xte.divide(legLength) * 2.0; // half the leg length
            }
        }
        return result;
    }
    
    @Override
    public Double getRelativeXTEUnsigned() {
        return getRelativeXTE(TrackedLegOfCompetitor::getAbsoluteCrossTrackError);
    }
    

    @Override
    public TackType getTackType() throws NoWindException {
        return getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getTackType(getTimePoint());
    }

    @Override
    public Speed getSmoothedSpeed() {
        final SpeedWithBearing result;
        if (getGPSFix().isEstimatedSpeedCached()) {
            result = getGPSFix().getCachedEstimatedSpeed();
        } else {
            result = getTrackedLegOfCompetitorContext().getTrackedRace().getTrack(getTrackedLegOfCompetitorContext().getCompetitor()).getEstimatedSpeed(getTimePoint());
        }
        return result;
    }

    @Override
    public Integer getTenthOfLeg() {
        final Distance windwardDistanceToGo = getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getWindwardDistanceToGo(getGPSFix().getTimePoint(), WindPositionMode.LEG_MIDDLE);
        final Distance legWindwardDistance = getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getTrackedLeg().getWindwardDistance();
        // eliminate negative values which may result if the competitor has just entered the leg but is yet to arrive at the windward
        // level of the waypoint just rounded:
        final double fractionSailed = Math.max(0, legWindwardDistance.add(windwardDistanceToGo.scale(-1)).divide(legWindwardDistance));
        return (int) Math.min(10, 1+10*fractionSailed);
    }

    @Override
    public Distance getXTEToWindAxisUnsigned() {
        return getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getUnsignedCrossTrackErrorToWindAxis(getTimePoint());
    }

    @Override
    public Distance getXTEToWindAxisSigned() {
        return getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getSignedCrossTrackErrorToWindAxis(getTimePoint());
    }

    @Override
    public Double getXTEToWindAxisRelativeUnsigned() {
        return getRelativeXTE(TrackedLegOfCompetitor::getUnsignedCrossTrackErrorToWindAxis);
    }

    @Override
    public Double getXTEToWindAxisRelativeSigned() {
        return getRelativeXTE(TrackedLegOfCompetitor::getSignedCrossTrackErrorToWindAxis);
    }
}