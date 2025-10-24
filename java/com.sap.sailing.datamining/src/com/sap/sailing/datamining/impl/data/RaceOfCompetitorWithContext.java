package com.sap.sailing.datamining.impl.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.datamining.SailingClusterGroups;
import com.sap.sailing.datamining.data.HasRaceOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTackTypeSegmentContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.impl.components.TackTypeSegmentRetrievalProcessor;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

/**
 * Equality is based on competitor and race which are compared by identity, assuming there are no duplicated
 * instances for the same {@link Competitor} and {@link TrackedRace} entities.
 */
public class RaceOfCompetitorWithContext implements HasRaceOfCompetitorContext {

    private final HasTrackedRaceContext trackedRaceContext;
    private final Competitor competitor;
    private final TackTypeSegmentsDataMiningSettings settings;

    public RaceOfCompetitorWithContext(HasTrackedRaceContext trackedRaceContext, Competitor competitor, TackTypeSegmentsDataMiningSettings settings) {
        this.trackedRaceContext = trackedRaceContext;
        this.competitor = competitor;
        this.settings = settings;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((competitor == null) ? 0 : competitor.hashCode());
        result = prime * result + ((trackedRaceContext == null) ? 0 : trackedRaceContext.hashCode());
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
        RaceOfCompetitorWithContext other = (RaceOfCompetitorWithContext) obj;
        if (competitor == null) {
            if (other.competitor != null)
                return false;
        } else if (!competitor.equals(other.competitor))
            return false;
        if (trackedRaceContext == null) {
            if (other.trackedRaceContext != null)
                return false;
        } else if (!trackedRaceContext.equals(other.trackedRaceContext))
            return false;
        return true;
    }

    @Override
    public HasTrackedRaceContext getTrackedRaceContext() {
        return trackedRaceContext;
    }

    private TrackedRace getTrackedRace() {
        return getTrackedRaceContext().getTrackedRace();
    }

    @Override
    public Competitor getCompetitor() {
        return competitor;
    }
    
    @Override
    public Tack getTackAtStart() throws NoWindException {
        TimePoint startOfRace = getTrackedRace().getStartOfRace();
        return startOfRace == null ? null : getTrackedRace().getTack(getCompetitor(), startOfRace);
    }
    
    @Override
    public Boat getBoat() {
        Boat boatOfCompetitor = getTrackedRace().getBoatOfCompetitor(getCompetitor());
        return boatOfCompetitor;
    }

    @Override
    public ClusterDTO getPercentageClusterForDistanceToStarboardSideAtStart() {
        Double normalizedDistance = getNormalizedDistanceToStarboardSideAtStartOfCompetitor();
        if (normalizedDistance == null) {
            return null;
        }
        
        SailingClusterGroups clusterGroups = Activator.getClusterGroups();
        Cluster<Double> cluster = clusterGroups.getPercentageClusterGroup().getClusterFor(normalizedDistance);
        return new ClusterDTO(clusterGroups.getPercentageClusterFormatter().format(cluster));
    }
    
    @Override
    public Distance getDistanceToStartLineAtStart() {
        return getTrackedRace().getDistanceToStartLine(getCompetitor(), 0);
    }
    
    @Override
    public Double getNormalizedDistanceToStarboardSideAtStartOfCompetitor() {
        TrackedRace trackedRace = getTrackedRace();
        TrackedLegOfCompetitor firstTrackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor, trackedRace.getRace().getCourse().getFirstLeg());
        TimePoint competitorStartTime = firstTrackedLegOfCompetitor.getStartTime();
        if (competitorStartTime == null) {
            return null;
        }
        Double distance = trackedRace.getDistanceFromStarboardSideOfStartLine(getCompetitor(), competitorStartTime).getMeters();
        Double length = trackedRace.getStartLine(competitorStartTime).getLength().getMeters();
        return distance / length;
    }
    
    @Override
    public Pair<Double, Integer> getNormalizedDistanceToStarboardSideAtStartOfCompetitorVsRankAtFirstMark(){
        return new Pair<>(getNormalizedDistanceToStarboardSideAtStartOfCompetitor(), getRankAtFirstMark());
    }
    
    @Override
    public Distance getWindwardDistanceToAdvantageousLineEndAtStartofRace() {
        return getTrackedRace().getWindwardDistanceToFavoredSideOfStartLine(getCompetitor(), 0);
    }
    
    @Override
    public Distance getWindwardDistanceToAdvantageousLineEndAtStartofCompetitor() {
        final Distance result;
        TimePoint competitorStartTime = getCompetitorStartTime();
        if (competitorStartTime == null) {
            result = null;
        } else {
            result = getTrackedRace().getWindwardDistanceToFavoredSideOfStartLine(getCompetitor(), competitorStartTime);
        }
        return result;
    }

    private TimePoint getCompetitorStartTime() {
        TrackedLegOfCompetitor firstTrackedLegOfCompetitor = getFirstLegOfCompetitor();
        TimePoint competitorStartTime = firstTrackedLegOfCompetitor.getStartTime();
        return competitorStartTime;
    }
    
    @Override
    public Distance getAbsoluteWindwardDistanceToStarboardSideAtStartOfCompetitor() {
        final Distance result;
        final TimePoint competitorStartTime = getCompetitorStartTime();
        if (competitorStartTime == null) {
            result = null;
        } else {
            TrackedRace trackedRace = getTrackedRace();
            TimePoint startOfRace = trackedRace.getStartOfRace();
            LineDetails startLine = trackedRace.getStartLine(startOfRace);
            Mark starboardMark = startLine.getStarboardMarkWhileApproachingLine();
            if (starboardMark == null) {
                return null;
            }
            GPSFixTrack<Mark, GPSFix> starboardMarkTrack = trackedRace.getOrCreateTrack(starboardMark);
            Position starboardMarkPosition = starboardMarkTrack.getEstimatedPosition(startOfRace, false);
            GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = trackedRace.getTrack(getCompetitor());
            Position competitorPosition = competitorTrack.getEstimatedPosition(startOfRace, false);
            TrackedLeg trackedLeg = trackedRace.getTrackedLeg(trackedRace.getRace().getCourse().getFirstLeg());
            result = trackedLeg.getAbsoluteWindwardDistance(competitorPosition, starboardMarkPosition, startOfRace, WindPositionMode.LEG_MIDDLE);
        }
        return result;
    }
    
    @Override
    public ClusterDTO getPercentageClusterForRelativeScore() {
        Double relativeScore = getTrackedRaceContext().getRelativeScoreForCompetitor(getCompetitor());
        if (relativeScore == null) {
            return null;
        }
        SailingClusterGroups clusterGroups = Activator.getClusterGroups();
        Cluster<Double> cluster = clusterGroups.getPercentageClusterGroup().getClusterFor(relativeScore);
        return new ClusterDTO(clusterGroups.getPercentageClusterFormatter().format(cluster));
    }
    
    @Override
    public MaxPointsReason getMaxPointsReason() {
        return getTrackedRaceContext().getLeaderboardContext().getLeaderboard().getMaxPointsReason(competitor,
                getTrackedRaceContext().getRaceColumn(), MillisecondsTimePoint.now());
    }
    
    @Override
    public boolean isDiscarded() {
        return getTrackedRaceContext().getLeaderboardContext().getLeaderboard().isDiscarded(competitor,
                getTrackedRaceContext().getRaceColumn(), MillisecondsTimePoint.now());
    }

    @Override
    public Speed getSpeedWhenStarting() {
        return getTrackedRace().getSpeedWhenCrossingStartLine(getCompetitor());
    }
    
    @Override
    public Duration getStartDelay() {
        final NavigableSet<MarkPassing> competitorMarkPassings = getTrackedRace().getMarkPassings(competitor);
        getTrackedRace().lockForRead(competitorMarkPassings);
        try {
            final Duration result;
            final TimePoint startOfRace;
            if (!competitorMarkPassings.isEmpty() && (startOfRace = getTrackedRace().getStartOfRace()) != null) {
                final MarkPassing firstMarkPassing = competitorMarkPassings.iterator().next();
                TimePoint competitorStartTime = firstMarkPassing.getTimePoint();
                result = startOfRace.until(competitorStartTime);
            } else {
                result = null;
            }
            return result;
        } finally {
            getTrackedRace().unlockAfterRead(competitorMarkPassings);
        }
    }
    
    @Override
    public Speed getSpeedTenSecondsBeforeStart() {
        return getTrackedRace().getSpeed(getCompetitor(), TimeUnit.SECONDS.toMillis(10));
    }
    
    @Override
    public Speed getSpeedTenSecondsAfterStartOfRace() {
        TimePoint startOfRace = getTrackedRace().getStartOfRace();
        if (startOfRace == null) {
            return null;
        }
        return getTrackOfCompetitor().getEstimatedSpeed(startOfRace.plus(TimeUnit.SECONDS.toMillis(10)));
    }

    @Override
    public Double getRankAfterHalfOfTheFirstLeg() {
        Course course = getTrackedRace().getRace().getCourse();
        TrackedLegOfCompetitor trackedLeg = getTrackedRace().getTrackedLeg(getCompetitor(), course.getFirstLeg());
        TimePoint startTime = trackedLeg.getStartTime();
        TimePoint finishTime = trackedLeg.getFinishTime();
        if (startTime == null || finishTime == null) {
            return null;
        }
        long halfOffset = (finishTime.asMillis() - startTime.asMillis()) / 2;
        int rank = getTrackedRace().getRank(getCompetitor(), startTime.plus(halfOffset));
        return rank == 0 ? null : Double.valueOf(rank);
    }
    
    @Override
    public Integer getRankAtFirstMark() {
        Course course = getTrackedRace().getRace().getCourse();
        Waypoint firstMark = course.getFirstLeg().getTo();
        Competitor competitor = getCompetitor();
        final MarkPassing markPassing = getTrackedRace().getMarkPassing(competitor, firstMark);
        int rank = markPassing == null ? 0 : getTrackedRace().getRank(competitor, markPassing.getTimePoint());
        return rank == 0 ? null : rank;
    }
    
    @Override
    public Integer getRankGainsOrLossesBetweenFirstMarkAndFinish() {
        Integer rankAtFirstMark = getRankAtFirstMark();
        Integer rankAtFinish = getTrackedRaceContext().getRankAtFinishForCompetitor(getCompetitor());
        return rankAtFirstMark != null && rankAtFinish != null ? rankAtFirstMark - rankAtFinish : null;
    }

    @Override
    public int getNumberOfManeuvers() {
        Set<ManeuverType> maneuverTypes = new HashSet<>();
        maneuverTypes.add(ManeuverType.TACK);
        maneuverTypes.add(ManeuverType.JIBE);
        return getNumberOf(maneuverTypes);
    }

    @Override
    public int getNumberOfTacks() {
        return getNumberOf(Collections.singleton(ManeuverType.TACK));
    }

    @Override
    public int getNumberOfJibes() {
        return getNumberOf(Collections.singleton(ManeuverType.JIBE));
    }

    @Override
    public int getNumberOfPenaltyCircles() {
        return getNumberOf(Collections.singleton(ManeuverType.PENALTY_CIRCLE));
    }

    private int getNumberOf(Set<ManeuverType> maneuverTypes) {
        int number = 0;
        TrackedRace trackedRace = getTrackedRace();
        if (trackedRace != null) {
            TimePoint from = null;
            TimePoint to = null;
            Competitor competitor = getCompetitor();
            Course course = trackedRace.getRace().getCourse();
            List<Waypoint> waypoints = Util.asList(course.getWaypoints());
            int fromIndex = 0;
            while (fromIndex < waypoints.size()) {
                MarkPassing markPassing = trackedRace.getMarkPassing(competitor, waypoints.get(fromIndex));
                TimePoint passingTime = markPassing != null ? markPassing.getTimePoint() : null;
                if (passingTime != null) {
                    if (from == null) {
                        from = passingTime;
                    } else {
                        to = passingTime;
                    }
                }
                fromIndex++;
            }
            if (from != null && to != null) {
                for (Maneuver maneuver : trackedRace.getManeuvers(getCompetitor(), from, to, false)) {
                    if (maneuverTypes.contains(maneuver.getType())) {
                        number++;
                    }
                }
            }
        }
        return number;
    }

    @Override
    public Distance getDistanceTraveled() {
        return getTrackedRace().getDistanceTraveledIncludingGateStart(getCompetitor(), MillisecondsTimePoint.now());
    }
    
    @Override 
    public Distance getLineLengthAtStart() {
        TrackedLegOfCompetitor firstTrackedLegOfCompetitor = getFirstLegOfCompetitor();
        TimePoint competitorStartTime = firstTrackedLegOfCompetitor.getStartTime();
        if (competitorStartTime == null) {
            return null;
        }
        return getTrackedRace().getStartLine(competitorStartTime).getLength();
    }
    
    @Override
    public Pair<Double, Integer> getRelativeDistanceToStarboardSideAtStartOfCompetitorVsFinalRank(){
        return new Pair<>(getNormalizedDistanceToStarboardSideAtStartOfCompetitor(), getTrackedRaceContext().getRankAtFinishForCompetitor(getCompetitor()));
    }
    
    @Override
    public Pair<Double, Double> getWindwardDistanceToAdvantageousEndOfLineAtStartOfRaceVsRelativeDistanceToAdvantageousEndOfLineAtStartOfRace(){
        return new Pair<>(getWindwardDistanceToAdvantageousLineEndAtStartofRace().getMeters(), getRelativeDistanceToAdvantageousEndOfLineAtStartOfRace());
    }
    
    @Override
    public Double getRelativeDistanceToAdvantageousEndOfLineAtStartOfRace() {
        final TimePoint startOfRace = getTrackedRace().getStartOfRace();
        return getRelativeDistanceToAdvantageousEndOfLine(startOfRace);
    }
    
    @Override
    public Double getRelativeDistanceToAdvantageousEndOfLineAtStartOfCompetitor() {
        final TimePoint competitorStartTimePoint = getTrackedRace().getTrackedLeg(getCompetitor(), getTrackedRace().getRace().getCourse().getFirstLeg()).getStartTime();
        return getRelativeDistanceToAdvantageousEndOfLine(competitorStartTimePoint);
    }

    private Double getRelativeDistanceToAdvantageousEndOfLine(final TimePoint timePoint) {
        final LineDetails startLine = getTrackedRace().getStartLine(timePoint);
        Mark advantageousMark = null;
        switch (startLine.getAdvantageousSideWhileApproachingLine()) {
        case PORT:
            advantageousMark = startLine.getPortMarkWhileApproachingLine();
            break;
        case STARBOARD:
            advantageousMark = startLine.getStarboardMarkWhileApproachingLine();
            break;
        }
        if (advantageousMark == null) {
            return null;
        }
        final GPSFixTrack<Mark, GPSFix> advantageousMarkTrack = getTrackedRace().getOrCreateTrack(advantageousMark);
        final Position advantageousMarkPosition = advantageousMarkTrack.getEstimatedPosition(timePoint, false);
        final GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = getTrackedRace().getTrack(getCompetitor());
        final Position competitorPosition = competitorTrack.getEstimatedPosition(timePoint, false);
        final Double distance = competitorPosition.getDistance(advantageousMarkPosition).getMeters();
        final TrackedLegOfCompetitor firstTrackedLegOfCompetitor = getTrackedRace().getTrackedLeg(competitor, getTrackedRace().getRace().getCourse().getFirstLeg());
        final TimePoint competitorStartTime = firstTrackedLegOfCompetitor.getStartTime();
        final Double length = getTrackedRace().getStartLine(competitorStartTime).getLength().getMeters();
        return distance / length;
    }
    
    @Override
    public Duration getDuration() {
        Duration duration = null;
        TrackedRace race = getTrackedRace();
        Course course = race.getRace().getCourse();
        MarkPassing startPassing = race.getMarkPassing(competitor, course.getFirstWaypoint());
        MarkPassing finishPassing = race.getMarkPassing(competitor, course.getLastWaypoint());
        if (startPassing != null && finishPassing != null) {
            long durationMillis = finishPassing.getTimePoint().asMillis() - startPassing.getTimePoint().asMillis();
            duration = new MillisecondsDurationImpl(durationMillis);
        }
        return duration;
    }

    @Override
    public Duration getDurationFromStartToFirstTack() {
        final TrackedRace race = getTrackedRace();
        final Duration result;
        if (race.getStartOfRace() == null) {
            result = null;
        } else {
            final Iterable<Maneuver> maneuvers = race.getManeuvers(competitor, /* wait for latest */ false);
            final List<Maneuver> tacks = Util.asList(Util.filter(maneuvers,
                    m->m.getType() == ManeuverType.TACK && !m.getTimePoint().before(race.getStartOfRace())));
            if (tacks.isEmpty()) {
                result = null;
            } else {
                tacks.sort(TimedComparator.INSTANCE);
                result = race.getStartOfRace().until(tacks.get(0).getTimePoint());
            }
        }
        return result;
    }

    @Override
    public Double getRelativeDistanceToStarboardSideAtStartOfRace() {
        TrackedRace trackedRace = getTrackedRace();
        TrackedLegOfCompetitor firstTrackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor, trackedRace.getRace().getCourse().getFirstLeg());
        TimePoint competitorStartTime = firstTrackedLegOfCompetitor.getStartTime();
        if (competitorStartTime == null) {
            return null;
        }
        return getNormalizeDistanceToStarboardSideAtTimePoint(getStartOfRace());
    }

    @Override
    public Speed getVMG5SecondsBeforeStartOfRace() {
        return getTrackedRace().getVelocityMadeGood(getCompetitor(), getStartOfRace().minus(TimeUnit.SECONDS.toMillis(5)));
    }

    @Override
    public Speed getVMGAtStartOfRace() {
        return getTrackedRace().getVelocityMadeGood(getCompetitor(), getStartOfRace());
    }

    @Override
    public Speed getVMG5SecondsAfterStartOfRace() {
        return getTrackedRace().getVelocityMadeGood(getCompetitor(), getStartOfRace().plus(TimeUnit.SECONDS.toMillis(5)));
    }

    @Override
    public Pair<Double, Integer> getRelativeDistanceToAdvantageousSideAtStartOfRaceVsRankAtFirstMark() {
        return new Pair<>(getRelativeDistanceToAdvantageousEndOfLineAtStartOfRace(), getRankAtFirstMark());
    }

    @Override
    public Pair<Integer, Integer> getRankAtFirstMarkVsFinalRank() {
        return new Pair<>(getRankAtFirstMark(), getFinalRank());
    }

    @Override
    public Integer getRankThirtySecondsAfterStartOfRace() {
        return getRankAt(getStartOfRace().plus(TimeUnit.SECONDS.toMillis(30)));
    }
    
    @Override
    public Integer getRankSixtySecondsAfterStartOfRace() {
        return getRankAt(getStartOfRace().plus(TimeUnit.SECONDS.toMillis(60)));
    }
    
    @Override
    public Integer getRankNinetySecondsAfterStartOfRace() {
        return getRankAt(getStartOfRace().plus(TimeUnit.SECONDS.toMillis(90)));
    }

    @Override
    public Integer getFinalRank() {
        if(getEndOfRace() == null) {
            return null;
        }
        return getRankAt(getEndOfRace());
    }

    @Override
    public Pair<Double, Integer> getRelativeDistanceToAdvantageousSideAtStartOfRaceVsFinalRank() {
        return new Pair<>(getRelativeDistanceToAdvantageousEndOfLineAtStartOfRace(), getFinalRank());
    }

    @Override
    public Speed getAverageRaceWindSpeed() {
        return getTrackedRace().getAverageWindSpeedWithConfidence(5000).getObject();
    }

    private GPSFixTrack<Competitor, GPSFixMoving> getTrackOfCompetitor() {
        return getTrackedRace().getTrack(getCompetitor());
    }

    private TrackedLegOfCompetitor getFirstLegOfCompetitor() {
        return getTrackedRace().getTrackedLeg(competitor, getTrackedRace().getRace().getCourse().getFirstLeg());
    }
    
    private TimePoint getStartOfRace() {
        return getTrackedRace().getStartOfRace();
    }

    private TimePoint getEndOfRace() {
        return getTrackedRace().getEndOfRace();
    }
    
    private Double getNormalizeDistanceToStarboardSideAtTimePoint(TimePoint timepoint) {
        Double distance = getTrackedRace().getDistanceFromStarboardSideOfStartLine(getCompetitor(), timepoint).getMeters();
        Double length = getTrackedRace().getStartLine(timepoint).getLength().getMeters();
        return distance / length;
    }
    
    @Override
    public Distance getDistanceFromStarboardSideOfStartLineProjectedOntoLineAtStartOfRace() {
        return getTrackedRace().getDistanceFromStarboardSideOfStartLineProjectedOntoLine(getCompetitor(), getStartOfRace());
    }

    @Override
    public Distance getDistanceToNextBoatToStarboardProjectedToStartLineAtStartOfRace() {
        final Distance result;
        final SortedMap<Competitor, Distance> competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine =
                getTrackedRace().getDistancesFromStarboardSideOfStartLineProjectedOntoLine(getStartOfRace(), getMaxPointsReasonSupplier());
        final Competitor competitorImmediatelyToStarboard = getTrackedRace().getNextCompetitorToStarboardOnStartLine(getCompetitor(), getStartOfRace(), getMaxPointsReasonSupplier());
        if (competitorImmediatelyToStarboard == null) {
            // use distance to starboard side of line for boat farthest to starboard
            result = competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.get(getCompetitor());
        } else {
            final Distance competitorDistance = competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.get(getCompetitor());
            if (competitorDistance == null) {
                result = null; // it's a bit strange because just before we found out who the immediate neighbor to starboard is...
            } else {
                final Distance distanceToStarboardEndOfLineProjectedOntoLineOfCompetitorImmediatelyToStarboard =
                        competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.get(competitorImmediatelyToStarboard);
                result = competitorDistance.add(distanceToStarboardEndOfLineProjectedOntoLineOfCompetitorImmediatelyToStarboard.scale(-1));
            }
        }
        return result;
    }
    
    @Override
    public Distance getTotalDistanceToNeighboursProjectedToStartLineAtStartOfRace() {
        final Distance toPort = getDistanceToNextBoatToPortProjectedToStartLineAtStartOfRace();
        final Distance toStarboard = getDistanceToNextBoatToStarboardProjectedToStartLineAtStartOfRace();
        return toPort==null || toStarboard==null ? null : toPort.add(toStarboard);
    }
    
    @Override
    public Distance getTotalWindwardDistanceToNeighboursAtStartOfRace() {
        final Distance toPort = getWindwardDistanceToNextBoatToPortAtStartOfRace();
        final Distance toStarboard = getWindwardDistanceToNextBoatToStarboardAtStartOfRace();
        return toPort==null || toStarboard==null ? null : toPort.abs().add(toStarboard.abs());
    }
    
    @Override
    public Distance getTotalDistanceToNeighboursPerpendicularToStarLineAtStartOfRace() {
        final Distance toPort = getDistanceToNextBoatToPortPerpendicularToStartLineAtStartOfRace();
        final Distance toStarboard = getDistanceToNextBoatToStarboardPerpendicularToStartLineAtStartOfRace();
        return toPort==null || toStarboard==null ? null : toPort.abs().add(toStarboard.abs());
    }
    
    @Override
    public Distance getDistanceToNextBoatToPortProjectedToStartLineAtStartOfRace() {
        final Distance result;
        final SortedMap<Competitor, Distance> competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine =
                getTrackedRace().getDistancesFromStarboardSideOfStartLineProjectedOntoLine(getStartOfRace(), getMaxPointsReasonSupplier());
        final Distance competitorDistance = competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.get(getCompetitor());
        final Competitor competitorImmediatelyToPort = getTrackedRace().getNextCompetitorToPortOnStartLine(getCompetitor(), getStartOfRace(), getMaxPointsReasonSupplier());
        if (competitorImmediatelyToPort == null) {
            final LineDetails startLine = getTrackedRace().getStartLine(getStartOfRace());
            if (competitorDistance == null || startLine == null) {
                result = null;
            } else {
                // use distance to starboard side of line for boat farthest to port
                result = startLine.getLength().add(competitorDistance.scale(-1));
            }
        } else {
            if (competitorDistance == null) {
                result = null; // it's a bit strange because just before we found out who the immediate neighbor to port is...
            } else {
                final Distance distanceToStarboardEndOfLineProjectedOntoLineOfCompetitorImmediatelyToPort =
                        competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.get(competitorImmediatelyToPort);
                result = distanceToStarboardEndOfLineProjectedOntoLineOfCompetitorImmediatelyToPort.add(competitorDistance.scale(-1));
            }
        }
        return result;
    }

    private Distance getWindwardDistanceToOtherCompetitorAtRaceStart(final Competitor other) {
        final Distance result;
        final Position competitorPosition = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(getStartOfRace(), /* extrapolate */ true);
        final Position neighborPosition = getTrackedRace().getTrack(other).getEstimatedPosition(getStartOfRace(), /* extrapolate */ true);
        if (competitorPosition == null || neighborPosition == null) {
            result = null;
        } else {
            final Iterable<TrackedLeg> trackedLegs = getTrackedRace().getTrackedLegs();
            if (Util.isEmpty(trackedLegs)) {
                result = null;
            } else {
                final TrackedLeg firstLeg = trackedLegs.iterator().next();
                result = firstLeg.getWindwardDistance(neighborPosition, competitorPosition, getStartOfRace(), WindPositionMode.LEG_MIDDLE);
            }
        }
        return result;
    }
    
    @Override
    public Distance getWindwardDistanceToNextBoatToPortAtStartOfRace() {
        final Distance result;
        final Competitor competitorImmediatelyToPort = getTrackedRace().getNextCompetitorToPortOnStartLine(getCompetitor(), getStartOfRace(), getMaxPointsReasonSupplier());
        if (competitorImmediatelyToPort == null) {
            result = null;
        } else {
            result = getWindwardDistanceToOtherCompetitorAtRaceStart(competitorImmediatelyToPort);
        }
        return result;
    }
    
    @Override
    public Distance getWindwardDistanceToNextBoatToStarboardAtStartOfRace() {
        final Distance result;
        final Competitor competitorImmediatelyToStarboard = getTrackedRace().getNextCompetitorToStarboardOnStartLine(getCompetitor(), getStartOfRace(), getMaxPointsReasonSupplier());
        if (competitorImmediatelyToStarboard == null) {
            result = null;
        } else {
            result = getWindwardDistanceToOtherCompetitorAtRaceStart(competitorImmediatelyToStarboard);
        }
        return result;
    }

    private Distance getDistanceToOtherCompetitorAtRaceStartPerpendicularToStartLine(final Competitor other) {
        final Distance result;
        final Position competitorPosition = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(getStartOfRace(), /* extrapolate */ true);
        final Position neighborPosition = getTrackedRace().getTrack(other).getEstimatedPosition(getStartOfRace(), /* extrapolate */ true);
        if (competitorPosition == null || neighborPosition == null) {
            result = null;
        } else {
            Pair<Bearing, Position> startLineBearingAndStarboardMarkPosition = getTrackedRace().getStartLineBearingAndStarboardMarkPosition(getStartOfRace());
            if (startLineBearingAndStarboardMarkPosition.getA() == null) {
                result = null;
            } else {
                result = competitorPosition.crossTrackError(neighborPosition, startLineBearingAndStarboardMarkPosition.getA());
            }
        }
        return result;
    }
    
    private BiFunction<Competitor, TimePoint, MaxPointsReason> getMaxPointsReasonSupplier() {
        final Leaderboard leaderboard = getTrackedRaceContext().getLeaderboardContext().getLeaderboard();
        return (competitor, timePoint)->leaderboard.getMaxPointsReason(competitor, getTrackedRaceContext().getRaceColumn(), timePoint);
    }
    
    @Override
    public Distance getDistanceToNextBoatToPortPerpendicularToStartLineAtStartOfRace() {
        final Distance result;
        final Competitor competitorImmediatelyToPort = getTrackedRace().getNextCompetitorToPortOnStartLine(getCompetitor(), getStartOfRace(), getMaxPointsReasonSupplier());
        if (competitorImmediatelyToPort == null) {
            result = null;
        } else {
            result = getDistanceToOtherCompetitorAtRaceStartPerpendicularToStartLine(competitorImmediatelyToPort);
        }
        return result;
    }

    @Override
    public Distance getDistanceToNextBoatToStarboardPerpendicularToStartLineAtStartOfRace() {
        final Distance result;
        final Competitor competitorImmediatelyToStarboard = getTrackedRace().getNextCompetitorToStarboardOnStartLine(getCompetitor(), getStartOfRace(), getMaxPointsReasonSupplier());
        if (competitorImmediatelyToStarboard == null) {
            result = null;
        } else {
            result = getDistanceToOtherCompetitorAtRaceStartPerpendicularToStartLine(competitorImmediatelyToStarboard);
        }
        return result;
    }

    @Override
    public Double getNormalizedDistanceFromStarboardSideOfStartLineProjectedOntoLineAtStartOfRace() {
        final Double result;
        final LineDetails startLine = getTrackedRace().getStartLine(getStartOfRace());
        if (startLine == null) {
            result = null;
        } else {
            result = getTrackedRace().getDistanceFromStarboardSideOfStartLineProjectedOntoLine(getCompetitor(), getStartOfRace()).divide(startLine.getLength());
        }
        return result;
    }
    
    private Integer getRankAt(TimePoint timePoint) {
        TimePoint startOfRace = getStartOfRace();
        if (startOfRace == null) {
            return null;
        }
        Integer rank = getTrackedRace().getRank(getCompetitor(), timePoint);
        return rank == 0 ? null : rank;
    }

    @Override
    public double getRatioDurationLongVsShortTack() {
        final TackTypeRatioCollector<Duration> resultProcessor = new TackTypeRatioCollector<Duration>(Duration.NULL) {
            @Override
            protected Duration add(Duration a, Duration b) {
                return a.plus(b);
            }

            @Override
            protected double divide(Duration a, Duration b) {
                return a.divide(b);
            }

            @Override
            protected Duration getAddable(HasTackTypeSegmentContext element) {
                return element.getDuration();
            }
        };
        final TackTypeSegmentRetrievalProcessor tackTypeSegmentRetriever = new TackTypeSegmentRetrievalProcessor(
                /* executor */ null,
                Collections.emptySet(), settings, 0, "TackTypeSegments");
        return Util.stream(tackTypeSegmentRetriever.retrieveData(this)).collect(resultProcessor);
    }

    @Override
    public double getRatioDistanceLongVsShortTack() {
        final TackTypeRatioCollector<Distance> resultProcessor = new TackTypeRatioCollector<Distance>(Distance.NULL) {
            @Override
            protected Distance add(Distance a, Distance b) {
                return a.add(b);
            }

            @Override
            protected double divide(Distance a, Distance b) {
                return a.divide(b);
            }

            @Override
            protected Distance getAddable(HasTackTypeSegmentContext element) {
                return element.getDistance();
            }
        };
        final TackTypeSegmentRetrievalProcessor tackTypeSegmentRetriever = new TackTypeSegmentRetrievalProcessor(/* executor */ null,
                Collections.emptySet(), settings, 0, "TackTypeSegments");
        return Util.stream(tackTypeSegmentRetriever.retrieveData(this)).collect(resultProcessor);
    }
}