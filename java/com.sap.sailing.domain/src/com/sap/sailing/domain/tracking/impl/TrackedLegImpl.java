package com.sap.sailing.domain.tracking.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;

public class TrackedLegImpl implements TrackedLeg, RaceChangeListener {
    private static final long serialVersionUID = -1944668527284130545L;

    private final static Logger logger = Logger.getLogger(TrackedLegImpl.class.getName());
    
    private final static double UPWIND_DOWNWIND_TOLERANCE_IN_DEG = 60; // TracTrac does 22.5, Marcus Baur suggest 40; Nils Schr�der suggests 60

    private final Leg leg;
    private final Map<Competitor, TrackedLegOfCompetitor> trackedLegsOfCompetitors;
    private TrackedRaceImpl trackedRace;
    private final Map<TimePoint, List<TrackedLegOfCompetitor>> competitorTracksOrderedByRank;
    
    public TrackedLegImpl(DynamicTrackedRaceImpl trackedRace, Leg leg, Iterable<Competitor> competitors) {
        super();
        this.leg = leg;
        this.trackedRace = trackedRace;
        trackedLegsOfCompetitors = new HashMap<Competitor, TrackedLegOfCompetitor>();
        for (Competitor competitor : competitors) {
            trackedLegsOfCompetitors.put(competitor, new TrackedLegOfCompetitorImpl(this, competitor));
        }
        trackedRace.addListener(this);
        competitorTracksOrderedByRank = new HashMap<TimePoint, List<TrackedLegOfCompetitor>>();
    }
    
    @Override
    public Leg getLeg() {
        return leg;
    }
    
    @Override
    public TrackedRace getTrackedRace() {
        return trackedRace;
    }

    @Override
    public Iterable<TrackedLegOfCompetitor> getTrackedLegsOfCompetitors() {
        return trackedLegsOfCompetitors.values();
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor) {
        return trackedLegsOfCompetitors.get(competitor);
    }

    protected Competitor getLeader(TimePoint timePoint) {
        List<TrackedLegOfCompetitor> byRank = getCompetitorTracksOrderedByRank(timePoint);
        return byRank.get(0).getCompetitor();
    }

    /**
     * Orders the tracked legs for all competitors for this tracked leg for the given time point. This
     * results in an order that gives a ranking for this tracked leg. In particular, boats that have not
     * yet entered this leg will all be ranked equal because their windward distance to go is the full
     * leg's winward distance. Boats who already finished this leg have their tracks ordered by the time
     * points at which they finished the leg.<p>
     * 
     * Note that this does not reflect overall race standings. For that, the ordering would have to
     * consider the order of the boats not currently in this leg, too.
     */
    protected List<TrackedLegOfCompetitor> getCompetitorTracksOrderedByRank(TimePoint timePoint) {
        List<TrackedLegOfCompetitor> rankedCompetitorList;
        synchronized (competitorTracksOrderedByRank) {
            rankedCompetitorList = competitorTracksOrderedByRank.get(timePoint);
            if (rankedCompetitorList != null) {
                rankedCompetitorList = new ArrayList<TrackedLegOfCompetitor>(rankedCompetitorList);
            }
        }
        if (rankedCompetitorList == null) {
            rankedCompetitorList = new ArrayList<TrackedLegOfCompetitor>();
            for (TrackedLegOfCompetitor competitorLeg : getTrackedLegsOfCompetitors()) {
                rankedCompetitorList.add(competitorLeg);
            }
            // ensure that race isn't updated by events as we're tying to sort the competitors
            synchronized (getTrackedRace()) {
                Collections.sort(rankedCompetitorList, new WindwardToGoComparator(this, timePoint));
                rankedCompetitorList = Collections.unmodifiableList(rankedCompetitorList);
                synchronized (competitorTracksOrderedByRank) {
                    competitorTracksOrderedByRank.put(timePoint, rankedCompetitorList);
                }
                if (Util.size(getTrackedLegsOfCompetitors()) != rankedCompetitorList.size()) {
                    logger.warning("Number of competitors in leg (" + Util.size(getTrackedLegsOfCompetitors())
                            + ") differs from number of competitors in race ("
                            + Util.size(getTrackedRace().getRace().getCompetitors()) + ")");
                }
            }
        }
        return rankedCompetitorList;
    }
    
    @Override
    public LinkedHashMap<Competitor, Integer> getRanks(TimePoint timePoint) {
        List<TrackedLegOfCompetitor> orderedTrackedLegsOfCompetitors = getCompetitorTracksOrderedByRank(timePoint);
        LinkedHashMap<Competitor, Integer> result = new LinkedHashMap<Competitor, Integer>();
        int i=1;
        for (TrackedLegOfCompetitor tloc : orderedTrackedLegsOfCompetitors) {
            result.put(tloc.getCompetitor(), i++);
        }
        return result;
    }
    
    @Override
    public LegType getLegType(TimePoint at) throws NoWindException {
        Wind wind = getWindOnLeg(at);
        if (wind == null) {
            throw new NoWindException("Need to know wind direction to determine whether leg "+getLeg()+
                    " is an upwind or downwind leg");
        }
        Bearing legBearing = getLegBearing(at);
        if (legBearing != null) {
            double deltaDeg = legBearing.getDifferenceTo(wind.getBearing()).getDegrees();
            if (Math.abs(deltaDeg) < UPWIND_DOWNWIND_TOLERANCE_IN_DEG) {
                return LegType.DOWNWIND;
            } else {
                double deltaDegOpposite = legBearing.getDifferenceTo(wind.getBearing().reverse()).getDegrees();
                if (Math.abs(deltaDegOpposite) < UPWIND_DOWNWIND_TOLERANCE_IN_DEG) {
                    return LegType.UPWIND;
                }
            }
        }
        return LegType.REACHING;
    }

    @Override
    public Bearing getLegBearing(TimePoint at) {
        Position startBuoyPos = getTrackedRace().getApproximatePosition(getLeg().getFrom(), at);
        Position endBuoyPos = getTrackedRace().getApproximatePosition(getLeg().getTo(), at);
        Bearing legBearing = (startBuoyPos != null && endBuoyPos != null) ? startBuoyPos.getBearingGreatCircle(endBuoyPos) : null;
        return legBearing;
    }

    @Override
    public boolean isUpOrDownwindLeg(TimePoint at) throws NoWindException {
        return getLegType(at) != LegType.REACHING;
    }

    private Wind getWindOnLeg(TimePoint at) {
        Wind wind;
        Position approximateLegStartPosition = getTrackedRace().getOrCreateTrack(
                getLeg().getFrom().getBuoys().iterator().next()).getEstimatedPosition(at, false);
        Position approximateLegEndPosition = getTrackedRace().getOrCreateTrack(
                getLeg().getTo().getBuoys().iterator().next()).getEstimatedPosition(at, false);
        if (approximateLegStartPosition == null || approximateLegEndPosition == null) {
            wind = null;
        } else {
            // exclude track-based estimation; it is itself based on the leg type which is based on the getWindOnLeg
            // result which
            // would therefore lead to an endless recursion without further tricks being applied
            wind = getWind(approximateLegStartPosition.translateGreatCircle(
                    approximateLegStartPosition.getBearingGreatCircle(approximateLegEndPosition),
                    approximateLegStartPosition.getDistance(approximateLegEndPosition).scale(0.5)), at,
                    getTrackedRace().getWindSources(WindSourceType.TRACK_BASED_ESTIMATION));
        }
        return wind;
    }

    private Wind getWind(Position p, TimePoint at, Iterable<WindSource> windSourcesToExclude) {
        return getTrackedRace().getWind(p, at, windSourcesToExclude);
    }

    @Override
    public void competitorPositionChanged(GPSFix fix, Competitor competitor) {
        clearCaches();
    }

    @Override
    public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        clearCaches();
    }

    @Override
    public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        clearCaches();
    }

    @Override
    public void markPassingReceived(MarkPassing oldMarkPassing, MarkPassing markPassing) {
        clearCaches();
    }

    @Override
    public void buoyPositionChanged(GPSFix fix, Buoy buoy) {
        clearCaches();
    }

    @Override
    public void windDataReceived(Wind wind) {
        clearCaches();
    }
    
    @Override
    public void windDataRemoved(Wind wind) {
        clearCaches();
    }
    
    private void clearCaches() {
        synchronized (competitorTracksOrderedByRank) {
            competitorTracksOrderedByRank.clear();
        }
    }

    @Override
    public Distance getCrossTrackError(Position p, TimePoint timePoint) {
        return p.crossTrackError(getTrackedRace().getApproximatePosition(getLeg().getFrom(), timePoint), getLegBearing(timePoint));
    }

}
