package com.sap.sailing.polars.mining;

import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.confidence.BearingWithConfidence;
import com.sap.sse.common.confidence.ConfidenceFactory;
import com.sap.sse.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterGroup;

/**
 * Encapsulates a {@link GPSFixMoving} with some polar context. Some of the important and computeintensive data is
 * calculated once and then cached for further usage in this class.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class GPSFixMovingWithPolarContext implements LegTypePolarClusterKey, AngleClusterPolarClusterKey {

    private final GPSFixMoving fix;
    private final TrackedRace race;
    private final Competitor competitor;
    private final Set<WindSource> windSourcesToExcludeForSpeed;
    private final ClusterGroup<Bearing> angleClusterGroup;
    private final BearingWithConfidence<Void> absTrueWindAngle;
    private final WindWithConfidence<Pair<Position, TimePoint>> wind;
    private final SpeedWithBearingWithConfidence<TimePoint> boatSpeed;
    private LegType legType;

    public GPSFixMovingWithPolarContext(GPSFixMoving fix, TrackedRace race, Competitor competitor,
            ClusterGroup<Bearing> angleClusterGroup) {
        this.fix = fix;
        this.race = race;
        this.competitor = competitor;
        this.angleClusterGroup = angleClusterGroup;
        this.windSourcesToExcludeForSpeed = collectWindSourcesToIgnoreForSpeed();
        this.absTrueWindAngle = computeTrueWindAngleAbsolute();
        this.wind = race.getTrackedRegatta().getCPUMeter().callWithCPUMeter(
                () -> race.getWindWithConfidence(fix.getPosition(), fix.getTimePoint(), windSourcesToExcludeForSpeed),
                CPUMeteringType.BACKEND_POLARS.name());
        this.boatSpeed = computeBoatSpeed();
    }

    public GPSFixMoving getFix() {
        return fix;
    }

    public TrackedRace getRace() {
        return race;
    }

    public Competitor getCompetitor() {
        return competitor;
    }

    public BearingWithConfidence<Void> getAbsoluteAngleToTheWind() {
        return absTrueWindAngle;
    }

    private BearingWithConfidence<Void> computeTrueWindAngleAbsolute() {
        BearingWithConfidenceImpl<Void> result = null;
        Bearing bearing = null;
        TrackedLegOfCompetitor currentLeg = race.getCurrentLeg(competitor, fix.getTimePoint());
        if (currentLeg != null) {
            Bearing realBearing = race.getTWA(competitor, fix.getTimePoint());
            bearing = realBearing == null ? null : new DegreeBearingImpl(Math.abs(realBearing.getDegrees()));
        }
        WindWithConfidence<Pair<Position, TimePoint>> wind = race.getWindWithConfidence(fix.getPosition(),
                fix.getTimePoint());
        if (bearing != null && wind != null) {
            result = new BearingWithConfidenceImpl<Void>(bearing, wind.getConfidence(), null);
        }
        return result;
    }

    public WindWithConfidence<Pair<Position, TimePoint>> getWind() {
        return wind;
    }

    public SpeedWithBearingWithConfidence<TimePoint> getBoatSpeed() {
        return boatSpeed;
    }

    private SpeedWithBearingWithConfidence<TimePoint> computeBoatSpeed() {
        GPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(competitor);
        return track.getEstimatedSpeed(fix.getTimePoint(),
                ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(
                // use a minimum confidence to avoid the bearing to flip to 270deg in case all is zero
                        track.getMillisecondsOverWhichToAverageSpeed() / 2, /* minimumConfidence */0.00000001));
    }

    private Set<WindSource> collectWindSourcesToIgnoreForSpeed() {
        Set<WindSource> windSourcesToExclude = new HashSet<WindSource>();
        Iterable<WindSource> combinedSources = race.getWindSources(WindSourceType.COMBINED);
        for (WindSource combinedSource : combinedSources) {
            windSourcesToExclude.add(combinedSource);
        }
        Iterable<WindSource> courseSources = race.getWindSources(WindSourceType.COURSE_BASED);
        for (WindSource courseSource : courseSources) {
            windSourcesToExclude.add(courseSource);
        }
        Iterable<WindSource> trackBasedSources = race.getWindSources(WindSourceType.TRACK_BASED_ESTIMATION);
        for (WindSource trackBasedSource : trackBasedSources) {
            windSourcesToExclude.add(trackBasedSource);
        }
        Iterable<WindSource> maneuverBasedSources = race.getWindSources(WindSourceType.MANEUVER_BASED_ESTIMATION);
        for (WindSource maneuverBasedSource : maneuverBasedSources) {
            windSourcesToExclude.add(maneuverBasedSource);
        }
        Iterable<WindSource> rcSources = race.getWindSources(WindSourceType.RACECOMMITTEE);
        for (WindSource rcSource : rcSources) {
            windSourcesToExclude.add(rcSource);
        }
        Iterable<WindSource> webSources = race.getWindSources(WindSourceType.WEB);
        for (WindSource webSource : webSources) {
            windSourcesToExclude.add(webSource);
        }
        return windSourcesToExclude;
    }

    @Override
    public BoatClass getBoatClass() {
        return race.getBoatOfCompetitor(getCompetitor()).getBoatClass();
    }

    @Override
    public LegType getLegType() {
        LegType result = null;
        if (legType == null) {
            TimePoint timePoint = fix.getTimePoint();
            try {
                TrackedLegOfCompetitor currentLegOfCompetitor = race.getCurrentLeg(getCompetitor(), timePoint);
                if (currentLegOfCompetitor != null) {
                    final TrackedLeg currentLeg = currentLegOfCompetitor.getTrackedLeg();
                    legType = currentLeg == null ? null : currentLeg.getLegType(timePoint);
                }
            } catch (NoWindException e) {
                legType = null;
            }
            result = legType;
        } else {
            result = legType;
        }
        return result;
    }

    @Override
    public Cluster<Bearing> getAngleCluster() {
        return angleClusterGroup.getClusterFor(getAbsoluteAngleToTheWind().getObject());
    }

}
