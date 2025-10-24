package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasManeuverContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Equality is based on the {@link #getManeuver() maneuver} only.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverWithContext implements HasManeuverContext {
    private static final long serialVersionUID = 7717196485074392156L;
    private final HasTrackedLegOfCompetitorContext trackedLegOfCompetitor;
    private final Maneuver maneuver;
    private final TimePoint timePointBeforeForAnalysis;
    private final TimePoint timePointAfterForAnalysis;
    private final double directionChangeInDegreesForAnalysis;
    private final Maneuver previousManeuver;
    private final Maneuver nextManeuver;
    private Wind wind;

    public ManeuverWithContext(HasTrackedLegOfCompetitorContext trackedLegOfCompetitor, Maneuver maneuver,
            boolean mainCurveAnalysis, Maneuver previousManeuver, Maneuver nextManeuver) {
        this.trackedLegOfCompetitor = trackedLegOfCompetitor;
        this.maneuver = maneuver;
        this.previousManeuver = previousManeuver;
        this.nextManeuver = nextManeuver;
        ManeuverCurveBoundaries enteringAndExistingDetails = mainCurveAnalysis ? maneuver.getMainCurveBoundaries()
                : maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries();
        this.timePointBeforeForAnalysis = enteringAndExistingDetails.getTimePointBefore();
        this.timePointAfterForAnalysis = enteringAndExistingDetails.getTimePointAfter();
        this.directionChangeInDegreesForAnalysis = enteringAndExistingDetails.getDirectionChangeInDegrees();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((maneuver == null) ? 0 : maneuver.hashCode());
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
        ManeuverWithContext other = (ManeuverWithContext) obj;
        if (maneuver == null) {
            if (other.maneuver != null)
                return false;
        } else if (!maneuver.equals(other.maneuver))
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

    public TimePoint getTimePointBeforeForAnalysis() {
        return timePointBeforeForAnalysis;
    }

    public TimePoint getTimePointAfterForAnalysis() {
        return timePointAfterForAnalysis;
    }

    public double getDirectionChangeInDegreesForAnalysis() {
        return directionChangeInDegreesForAnalysis;
    }

    @Override
    public ManeuverType getTypeOfPreviousManeuver() {
        return previousManeuver != null ? previousManeuver.getType() : ManeuverType.UNKNOWN;
    }

    @Override
    public ManeuverType getTypeOfNextManeuver() {
        return nextManeuver != null ? nextManeuver.getType() : ManeuverType.UNKNOWN;
    }
    

    @Override
    public Double getManeuverEnteringSpeed() {
        return getSpeedInKnotsAtTimePoint(getTimePointBeforeForAnalysis());
    }

    @Override
    public Double getManeuverExitingSpeed() {
        return getSpeedInKnotsAtTimePoint(getTimePointAfterForAnalysis());
    }

    @Override
    public Double getManeuverDurationInSeconds() {
        return getTimePointBeforeForAnalysis().until(getTimePointAfterForAnalysis()).asSeconds();
    }

    @Override
    public Double getAbsTWAAtManeuverClimax() {
        Competitor competitor = getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getCompetitor();
        TrackedRace trackedRace = getTrackedLegOfCompetitorContext().getTrackedRace();
        Wind wind = trackedRace.getWind(maneuver.getPosition(), maneuver.getTimePoint());
        SpeedWithBearing speedWithBearing = trackedRace.getTrack(competitor).getEstimatedSpeed(maneuver.getTimePoint());
        return Math.abs(wind.getFrom().getDifferenceTo(speedWithBearing.getBearing()).getDegrees());
    }

    @Override
    public HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext() {
        return trackedLegOfCompetitor;
    }

    @Override
    public Maneuver getManeuver() {
        return maneuver;
    }

    @Override
    public Double getAbsoluteDirectionChangeInDegrees() {
        return Math.abs(getDirectionChangeInDegreesForAnalysis());
    }

    @Override
    public Distance getManeuverLossDistanceLost() {
        return getManeuver().getManeuverLoss() == null ? null : getManeuver().getManeuverLoss().getProjectedDistanceLost();
    }

    @Override
    public Double getEnteringAbsTWA() {
        return getAbsTWAAtTimepoint(getTimePointBeforeForAnalysis());
    }

    @Override
    public Double getExitingAbsTWA() {
        return getAbsTWAAtTimepoint(getTimePointAfterForAnalysis());
    }

    private Double getAbsTWAAtTimepoint(TimePoint timepoint) {
        Double twa = getTWAAtTimepoint(timepoint);
        if (twa == null) {
            return null;
        }
        return Math.abs(twa);
    }

    private Double getTWAAtTimepoint(TimePoint timepoint) {
        Wind wind = getTrackedLegOfCompetitorContext().getTrackedRace().getWind(maneuver.getPosition(), timepoint);
        final GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = getTrackedLegOfCompetitorContext().getTrackedRace().getTrack(getTrackedLegOfCompetitorContext().getCompetitor());
        if (wind != null) {
            competitorTrack.lockForRead();
            try {
                SpeedWithBearing speedWithBearing = competitorTrack.getEstimatedSpeed(timepoint);
                double twa = wind.getFrom().getDifferenceTo(speedWithBearing.getBearing()).getDegrees();
                return twa;
            } finally {
                competitorTrack.unlockAfterRead();
            }
        }
        return null;
    }
    
    public Pair<Double, Double> getWindSpeedVsManeuverLoss(){
        Wind wind = getTrackedLegOfCompetitorContext().getTrackedRace().getWind(maneuver.getPosition(), getTimePointBeforeForAnalysis());
        return new Pair<>(wind.getKnots(), getManeuverLossDistanceLost().getMeters());
    }

    @Override
    public Double getEnteringManeuverSpeedMinusExitingSpeed() {
        return getManeuverEnteringSpeed() - getManeuverExitingSpeed();
    }

    @Override
    public Double getRatioBetweenManeuverEnteringAndExitingSpeed() {
        return getManeuverEnteringSpeed() / getManeuverExitingSpeed();
    }

    @Override
    public Tack getTackBeforeManeuver() {
        Double twa = getTWAAtTimepoint(getTimePointBeforeForAnalysis());
        if (twa == null) {
            return null;
        }
        if (twa < 0) {
            return Tack.PORT;
        }
        return Tack.STARBOARD;
    }

    private Double getSpeedInKnotsAtTimePoint(TimePoint timePoint) {
        return getGPSFixTrack().getEstimatedSpeed(timePoint).getKnots();
    }

    private GPSFixTrack<Competitor, GPSFixMoving> getGPSFixTrack() {
        Competitor competitor = getTrackedLegOfCompetitorContext().getTrackedLegOfCompetitor().getCompetitor();
        TrackedRace trackedRace = getTrackedLegOfCompetitorContext().getTrackedRace();
        return trackedRace.getTrack(competitor);
    }

    @Override
    public Double getDurationBetweenStableSpeedWithCourseAndMainCurveBeginningSeconds() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointBefore()
                .until(maneuver.getMainCurveBoundaries().getTimePointBefore()).asSeconds();
    }

    @Override
    public Double getSpeedRatioBetweenStableSpeedWithCourseAndMainCurveBeginning() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingBefore().getKnots()
                / maneuver.getMainCurveBoundaries().getSpeedWithBearingBefore().getKnots();
    }

    @Override
    public Double getAbsCourseDifferenceBetweenStableSpeedWithCourseAndMainCurveBeginningInDegrees() {
        return Math.abs(
                maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingBefore().getBearing()
                        .getDifferenceTo(maneuver.getMainCurveBoundaries().getSpeedWithBearingBefore().getBearing())
                        .getDegrees());
    }

    @Override
    public Double getDurationBetweenStableSpeedWithCourseAndMainCurveEndInSeconds() {
        return maneuver.getMainCurveBoundaries().getTimePointAfter()
                .until(maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getTimePointAfter()).asSeconds();
    }

    @Override
    public Double getSpeedRatioBetweenStableSpeedWithCourseAndMainCurveEnd() {
        return maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingAfter().getKnots()
                / maneuver.getMainCurveBoundaries().getSpeedWithBearingAfter().getKnots();
    }

    @Override
    public Double getAbsCourseDifferenceBetweenStableSpeedWithCourseAndMainCurveEndInDegrees() {
        return Math.abs(maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getSpeedWithBearingAfter()
                .getBearing().getDifferenceTo(maneuver.getMainCurveBoundaries().getSpeedWithBearingAfter().getBearing())
                .getDegrees());
    }

    @Override
    public Speed getLowestSpeed() {
        return getManeuver().getLowestSpeed();
    }

    @Override
    public Speed getSpeedDifference() {
        return new KnotSpeedImpl(getManeuver().getSpeedWithBearingAfter().getKnots() - getManeuver().getSpeedWithBearingBefore().getKnots());
    }

    @Override
    public double getMaximimumTurningRateInDegreesPerSecond() {
        return getManeuver().getMaxTurningRateInDegreesPerSecond();
    }

    @Override
    public double getAverageTurningRateInDegreesPerSecond() {
        return getManeuver().getAvgTurningRateInDegreesPerSecond();
    }
}