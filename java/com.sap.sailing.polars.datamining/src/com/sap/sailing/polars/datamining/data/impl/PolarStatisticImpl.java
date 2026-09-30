package com.sap.sailing.polars.datamining.data.impl;

import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.polars.datamining.shared.PolarDataMiningSettings;
import com.sap.sailing.polars.datamining.shared.PolarStatistic;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;

public class PolarStatisticImpl implements PolarStatistic {

    private final SpeedWithBearing boatSpeed;
    private final Wind windSpeed;
    private final double trueWindAngleDeg;
    private final PolarDataMiningSettings settings;

    public PolarStatisticImpl(TrackedRace trackedRace, Competitor competitor, GPSFixMoving fix, PolarDataMiningSettings settings,
            Wind windSpeed) {
        this.settings = settings;
        this.windSpeed = windSpeed;
        GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        boatSpeed = track.getEstimatedSpeed(fix.getTimePoint());
        Bearing bearing = boatSpeed.getBearing();

        Position position = fix.getPosition();
        Set<WindSource> windSourcesToExclude;
        if (settings.useOnlyEstimatedForWindDirection()) {
            windSourcesToExclude = collectWindSourcesToIgnoreForBearing(trackedRace, /* exclude course based */true);
        } else {
            windSourcesToExclude = new HashSet<>();
        }

        Wind windEstimated = trackedRace.getWind(position, fix.getTimePoint(), windSourcesToExclude);
        if (windEstimated == null) {
            // no estimated wind; try to include course layout
            windSourcesToExclude = collectWindSourcesToIgnoreForBearing(trackedRace, /* exclude course based */false);
            windEstimated = trackedRace.getWind(position, fix.getTimePoint(), windSourcesToExclude);
        }
        if (windEstimated == null) {
            windEstimated = windSpeed; // maybe no upwind start; need to default to measured wind speed/direction
        }
        Bearing windBearing = windEstimated.getFrom();
        trueWindAngleDeg = bearing.getDifferenceTo(windBearing).getDegrees();
    }
    
    @Override
    public SpeedWithBearing getBoatSpeed() {
        return boatSpeed;
    }

    @Override
    public Speed getWindSpeed() {
        return windSpeed;
    }

    @Override
    public double getTrueWindAngleDeg() {
        return trueWindAngleDeg;
    }

    public static Set<WindSource> collectWindSourcesToIgnoreForBearing(TrackedRace race, boolean excludeCourseBased) {
        Set<WindSource> windSourcesToExclude = new HashSet<WindSource>();
        Iterable<WindSource> combinedSources = race.getWindSources(WindSourceType.COMBINED);
        for (WindSource combinedSource : combinedSources) {
            windSourcesToExclude.add(combinedSource);
        }
        if (excludeCourseBased) {
            Iterable<WindSource> courseSources = race.getWindSources(WindSourceType.COURSE_BASED);
            for (WindSource courseSource : courseSources) {
                windSourcesToExclude.add(courseSource);
            }
        }
        Iterable<WindSource> expSources = race.getWindSources(WindSourceType.EXPEDITION);
        for (WindSource expSource : expSources) {
            windSourcesToExclude.add(expSource);
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
    
    public static Set<WindSource> collectWindSourcesToIgnoreForSpeed(TrackedRace race, boolean useOnlyWindGaugesForWindSpeed) {
        Set<WindSource> windSourcesToExclude = new HashSet<WindSource>();
        if (useOnlyWindGaugesForWindSpeed) {
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
        }
        return windSourcesToExclude;
    }

    @Override
    public PolarDataMiningSettings getSettings() {
        return settings;
    }

}
