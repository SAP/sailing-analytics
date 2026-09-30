package com.sap.sailing.datamining.impl.data;

import java.text.SimpleDateFormat;
import java.util.Locale;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.datamining.data.HasBravoFixTrackContext;
import com.sap.sailing.datamining.data.HasFoilingSegmentContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.BearingCluster;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class FoilingSegmentWithContext implements HasFoilingSegmentContext {
    private final HasBravoFixTrackContext bravoFixTrackContext;
    private final TimePoint startOfFoilingSegment;
    private final TimePoint endOfFoilingSegment;
    private static final SimpleDateFormat TIMEPOINT_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    public FoilingSegmentWithContext(HasBravoFixTrackContext bravoFixTrackContext, TimePoint startOfFoilingSegment,
            TimePoint endOfFoilingSegment) {
        super();
        this.bravoFixTrackContext = bravoFixTrackContext;
        this.startOfFoilingSegment = startOfFoilingSegment;
        this.endOfFoilingSegment = endOfFoilingSegment;
    }

    @Override
    public String getName() {
        return bravoFixTrackContext.getRaceOfCompetitorContext().getCompetitor().getName() + "@"
                + TIMEPOINT_FORMATTER.format(startOfFoilingSegment.asDate());
    }

    @Override
    public HasBravoFixTrackContext getBravoFixTrackContext() {
        return bravoFixTrackContext;
    }

    @Override
    public TimePoint getStartOfFoilingSegment() {
        return startOfFoilingSegment;
    }

    @Override
    public TimePoint getEndOfFoilingSegment() {
        return endOfFoilingSegment;
    }

    @Override
    public Duration getDuration() {
        return getStartOfFoilingSegment().until(getEndOfFoilingSegment());
    }

    @Override
    public Distance getDistance() {
        return getGpsFixTrack().
                getDistanceTraveled(getStartOfFoilingSegment(), getEndOfFoilingSegment());
    }

    private GPSFixTrack<Competitor, GPSFixMoving> getGpsFixTrack() {
        return getTrackedRace().getTrack(getCompetitor());
    }

    private Competitor getCompetitor() {
        return getBravoFixTrackContext().getRaceOfCompetitorContext().getCompetitor();
    }

    private TrackedRace getTrackedRace() {
        return getBravoFixTrackContext().getRaceOfCompetitorContext().getTrackedRaceContext().getTrackedRace();
    }

    @Override
    public Double getTakeoffSpeedInKnots() {
        final SpeedWithBearing estimatedSpeed = getGpsFixTrack().getEstimatedSpeed(getStartOfFoilingSegment());
        return estimatedSpeed==null?null:estimatedSpeed.getKnots();
    }

    @Override
    public Double getLandingSpeedInKnots() {
        final SpeedWithBearing estimatedSpeed = getGpsFixTrack().getEstimatedSpeed(getEndOfFoilingSegment());
        return estimatedSpeed==null?null:estimatedSpeed.getKnots();
    }

    @Override
    public Bearing getAbsoluteTrueWindAngleAtTakeoffInDegrees() throws NoWindException {
        return getAbsoluteTrueWindAngle(getStartOfFoilingSegment());
    }

    @Override
    public Bearing getAbsoluteTrueWindAngleAtLandingInDegrees() throws NoWindException {
        final TimePoint timePoint = getEndOfFoilingSegment();
        return getAbsoluteTrueWindAngle(timePoint);
    }

    private Bearing getAbsoluteTrueWindAngle(final TimePoint timePoint) throws NoWindException {
        Bearing twa = getTrackedRace().getTWA(getCompetitor(), timePoint);
        return twa == null ? null:twa.abs();
    }

    @Override
    public Bearing getAverageAbsoluteTrueWindAngle() throws NoWindException {
        final BearingCluster bearingCluster = new BearingCluster();
        final BravoFixTrack<Competitor> bravoFixTrack = getBravoFixTrackContext().getBravoFixTrack();
        bravoFixTrack.lockForRead();
        try {
            for (final BravoFix bravoFix : bravoFixTrack.getFixes(getStartOfFoilingSegment(), /* fromInclusive */ true, getEndOfFoilingSegment(), /* toInclusive */ false)) {
                bearingCluster.add(getAbsoluteTrueWindAngle(bravoFix.getTimePoint()));
            }
        } finally {
            bravoFixTrack.unlockAfterRead();
        }
        return bearingCluster.getAverage();
    }

    @Override
    public ClusterDTO getWindStrengthAsBeaufortClusterAtTakeoff(Locale locale,
            ResourceBundleStringMessages stringMessages) {
        return getWindStrengthAsBeaufortCluster(locale, stringMessages, getWindAtTakeoff());
    }

    @Override
    public ClusterDTO getWindStrengthAsBeaufortClusterAtLanding(Locale locale,
            ResourceBundleStringMessages stringMessages) {
        return getWindStrengthAsBeaufortCluster(locale, stringMessages, getWindAtLanding());
     }

    private Wind getWind(TimePoint timePoint) {
        return getTrackedRace().getWind(getGpsFixTrack().getEstimatedPosition(timePoint, /* extrapolate */ true), timePoint);
    }

    @Override
    public Wind getWindAtTakeoff() {
        return getWind(getStartOfFoilingSegment());
    }

    @Override
    public Wind getWindAtLanding() {
        return getWind(getEndOfFoilingSegment());
    }

    private ClusterDTO getWindStrengthAsBeaufortCluster(Locale locale, ResourceBundleStringMessages stringMessages, Wind wind) {
        Cluster<?> cluster = Activator.getClusterGroups().getWindStrengthInBeaufortClusterGroup().getClusterFor(wind);
        return new ClusterDTO(cluster.toString(), ()->cluster.asLocalizedString(locale, stringMessages));
    }

    @Override
    public LegType getStartsOnLegType() throws NoWindException {
        return getLegType(getStartOfFoilingSegment());
    }

    private LegType getLegType(final TimePoint timePoint) throws NoWindException {
        return getTrackedRace().getTrackedLeg(getCompetitor(), timePoint).getTrackedLeg().getLegType(timePoint);
    }

    @Override
    public LegType getEndsOnLegType() throws NoWindException {
        return getLegType(getEndOfFoilingSegment());
    }
}
