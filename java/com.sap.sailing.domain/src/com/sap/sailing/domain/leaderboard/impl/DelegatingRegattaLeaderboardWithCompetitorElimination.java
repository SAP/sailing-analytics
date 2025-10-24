package com.sap.sailing.domain.leaderboard.impl;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.ScoreCorrectionListener;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.ObscuringIterable;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.metering.CPUMeter;

/**
 * A regatta leaderboard that is derived from another regatta leaderboard by eliminating a subset of the competitors and
 * that provides its own, unique name and optionally its own display name. The class generally implements a "delegate"
 * pattern for a {@link RegattaLeaderboard}. It therefore does not maintain its own score corrections or set of
 * suppressed competitors. Note: "suppressed" is different from "eliminated" in that suppressed competitors do not show
 * in any race and are not assigned any rank in any race, but eliminated competitors are; they only don't receive a
 * regatta ("total") rank, and all competitors advance by as many ranks compared to the original leaderboard as there
 * are eliminated competitors ranking better in the original leaderboard.
 * <p>
 *
 * This behavior is achieved by overriding any method returning a collection of {@link Competitor}s, such as
 * {@link #getCompetitors()}, such that the eliminated competitors are removed from the result which should let any
 * leaderboard panel displaying the contents of this leaderboard list only the non-eliminated competitors. This includes
 * {@link #getCompetitorsFromBestToWorst(TimePoint)} which also leads the implementation of
 * {@link #getTotalRankOfCompetitor(Competitor, TimePoint)} to calculate the ranks based on the competitor list without
 * those eliminated.
 *
 * @author Axel Uhl (d043530)
 */
public class DelegatingRegattaLeaderboardWithCompetitorElimination extends AbstractLeaderboardWithCache implements RegattaLeaderboardWithEliminations {
    private static final long serialVersionUID = 8331154893189722924L;
    private final String name;
    private final DelegateLeaderboard fullLeaderboard;

    /**
     * Competitors eliminated from this leaderboard for regatta ranking; those competitors are not part of
     * {@link #getCompetitors()} but appear in {@link #getAllCompetitors()}. They may have an overlap with
     * {@link #getSuppressedCompetitors()}, but while suppressed competitors cannot receive a score in a single race,
     * eliminated competitors can, and their scores are relevant for computing the regatta ranks, but ultimately, an
     * eliminated competitor's regatta rank is defined as {@code 0} for this leaderboard, and competitors ranking worse
     * in the {@link #fullLeaderboard original leaderboard} will advance by one rank per eliminated competitor ranking
     * better.
     */
    private final ConcurrentHashMap<Competitor, Boolean> eliminatedCompetitors;

    /**
     * The leaderboard wrapper starts out with an empty set of eliminated competitors
     */
    public DelegatingRegattaLeaderboardWithCompetitorElimination(Supplier<RegattaLeaderboard> fullLeaderboardSupplier, String name) {
        this.name = name;
        this.eliminatedCompetitors = new ConcurrentHashMap<>();
        fullLeaderboard = new DelegateLeaderboard(fullLeaderboardSupplier);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CPUMeter getCPUMeter() {
        return getDelegateLeaderboard().getCPUMeter();
    }

    private RegattaLeaderboard getDelegateLeaderboard() {
        return fullLeaderboard.getDelegateLeaderboard();
    }

    @Override
    public Iterable<Competitor> getCompetitors() {
        return new ObscuringIterable<>(getDelegateLeaderboard().getCompetitors(), eliminatedCompetitors.keySet());
    }

    @Override
    public void setEliminated(Competitor competitor, boolean eliminated) {
        if (eliminated) {
            eliminatedCompetitors.put(competitor, true);
        } else {
            eliminatedCompetitors.remove(competitor);
        }
        getLeaderboardDTOCache().invalidate(this);
    }

    @Override
    public boolean isEliminated(Competitor competitor) {
        return eliminatedCompetitors.containsKey(competitor);
    }

    @Override
    public Set<Competitor> getEliminatedCompetitors() {
        return new HashSet<Competitor>(eliminatedCompetitors.keySet());
    }

    @Override
    public Map<RaceColumn, List<Competitor>> getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException {
        Map<RaceColumn, List<Competitor>> preResult = getDelegateLeaderboard().getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(timePoint, cache);
        for (final List<Competitor> e : preResult.values()) {
            e.removeAll(eliminatedCompetitors.keySet());
        }
        return preResult;
    }

    @Override
    public Map<Competitor, Double> getCompetitorsForWhichThereAreCarriedPoints() {
        final Map<Competitor, Double> result = new HashMap<>();
        for (final java.util.Map.Entry<Competitor, Double> e : getDelegateLeaderboard().getCompetitorsForWhichThereAreCarriedPoints().entrySet()) {
            if (!isEliminated(e.getKey())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(RaceColumn raceColumn, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return new ObscuringIterable<>(getDelegateLeaderboard().getCompetitorsFromBestToWorst(raceColumn, timePoint, cache), eliminatedCompetitors.keySet());
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return new ObscuringIterable<>(getDelegateLeaderboard().getCompetitorsFromBestToWorst(timePoint, cache), eliminatedCompetitors.keySet());
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(RaceColumn raceColumn, TimePoint timePoint,
            Function<Competitor, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return new ObscuringIterable<>(getDelegateLeaderboard().getCompetitorsFromBestToWorst(raceColumn, timePoint, totalPointsSupplier, cache),
                eliminatedCompetitors.keySet());
    }

    @Override
    public Map<Pair<Competitor, RaceColumn>, Entry> getContent(TimePoint timePoint) throws NoWindException {
        final Map<Pair<Competitor, RaceColumn>, Entry> result = new HashMap<>();
        for (final java.util.Map.Entry<Pair<Competitor, RaceColumn>, Entry> e : getDelegateLeaderboard().getContent(timePoint).entrySet()) {
            if (!isEliminated(e.getKey().getA())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    @Override
    public LeaderboardType getLeaderboardType() {
        return LeaderboardType.RegattaLeaderboardWithEliminations;
    }

    // --------------------- Delegate Pattern Implementation ----------------------
    @Override
    public CompetitorProviderFromRaceColumnsAndRegattaLike getOrCreateCompetitorsProvider() {
        return getDelegateLeaderboard().getOrCreateCompetitorsProvider();
    }

    @Override
    public Regatta getRegatta() {
        return getDelegateLeaderboard().getRegatta();
    }

    @Override
    public Iterable<Competitor> getCompetitorsRegisteredInRegattaLog() {
        return getDelegateLeaderboard().getCompetitorsRegisteredInRegattaLog();
    }

    @Override
    public IsRegattaLike getRegattaLike() {
        return getDelegateLeaderboard().getRegattaLike();
    }

    @Override
    public RaceLog getRacelog(String raceColumnName, String fleetName) {
        return getDelegateLeaderboard().getRacelog(raceColumnName, fleetName);
    }

    @Override
    public void registerCompetitor(Competitor competitor) {
        getDelegateLeaderboard().registerCompetitor(competitor);
    }

    @Override
    public void registerCompetitors(Iterable<Competitor> competitors) {
        getDelegateLeaderboard().registerCompetitors(competitors);
    }

    @Override
    public void deregisterCompetitor(Competitor competitor) {
        getDelegateLeaderboard().deregisterCompetitor(competitor);
    }

    @Override
    public void deregisterCompetitors(Iterable<Competitor> competitor) {
        getDelegateLeaderboard().deregisterCompetitors(competitor);
    }

    @Override
    public Iterable<Competitor> getAllCompetitors() {
        return getDelegateLeaderboard().getAllCompetitors();
    }

    @Override
    public Pair<Iterable<RaceDefinition>, Iterable<Competitor>> getAllCompetitorsWithRaceDefinitionsConsidered() {
        return getDelegateLeaderboard().getAllCompetitorsWithRaceDefinitionsConsidered();
    }

    @Override
    public Iterable<Competitor> getAllCompetitors(RaceColumn raceColumn, Fleet fleet) {
        return getDelegateLeaderboard().getAllCompetitors(raceColumn, fleet);
    }

    @Override
    public Iterable<Competitor> getCompetitors(RaceColumn raceColumn, Fleet fleet) {
        return getDelegateLeaderboard().getCompetitors(raceColumn, fleet);
    }

    @Override
    public Iterable<Competitor> getSuppressedCompetitors() {
        return getDelegateLeaderboard().getSuppressedCompetitors();
    }

    @Override
    public boolean isSuppressed(Competitor competitor) {
        return getDelegateLeaderboard().isSuppressed(competitor);
    }

    @Override
    public void setSuppressed(Competitor competitor, boolean suppressed) {
        getDelegateLeaderboard().setSuppressed(competitor, suppressed);
    }

    @Override
    public Fleet getFleet(String fleetName) {
        return getDelegateLeaderboard().getFleet(fleetName);
    }

    @Override
    public Entry getEntry(Competitor competitor, RaceColumn race, TimePoint timePoint) throws NoWindException {
        return getDelegateLeaderboard().getEntry(competitor, race, timePoint);
    }

    @Override
    public Entry getEntry(Competitor competitor, RaceColumn race, TimePoint timePoint,
            Set<RaceColumn> discardedRaceColumns, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getDelegateLeaderboard().getEntry(competitor, race, timePoint, discardedRaceColumns, cache);
    }

    @Override
    public Map<RaceColumn, Map<Competitor, Double>> getNetPointsSumAfterRaceColumn(TimePoint timePoint)
            throws NoWindException {
        return getDelegateLeaderboard().getNetPointsSumAfterRaceColumn(timePoint);
    }

    @Override
    public double getCarriedPoints(Competitor competitor) {
        return getDelegateLeaderboard().getCarriedPoints(competitor);
    }

    @Override
    public int getTrackedRank(Competitor competitor, RaceColumn race, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getDelegateLeaderboard().getTrackedRank(competitor, race, timePoint, cache);
    }

    @Override
    public Double getTotalPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getDelegateLeaderboard().getTotalPoints(competitor, raceColumn, timePoint, cache);
    }

    @Override
    public MaxPointsReason getMaxPointsReason(Competitor competitor, RaceColumn race, TimePoint timePoint) {
        return getDelegateLeaderboard().getMaxPointsReason(competitor, race, timePoint);
    }

    @Override
    public Double getNetPoints(Competitor competitor, RaceColumn race, TimePoint timePoint) {
        return getDelegateLeaderboard().getNetPoints(competitor, race, timePoint);
    }

    @Override
    public boolean isDiscarded(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        return getDelegateLeaderboard().isDiscarded(competitor, raceColumn, timePoint);
    }

    @Override
    public Double getNetPoints(Competitor competitor, TimePoint timePoint) {
        return getDelegateLeaderboard().getNetPoints(competitor, timePoint);
    }

    @Override
    public Double getNetPoints(Competitor competitor, Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint) {
        return getDelegateLeaderboard().getNetPoints(competitor, raceColumnsToConsider, timePoint);
    }

    @Override
    public Iterable<RaceColumn> getRaceColumns() {
        final RegattaLeaderboard theFullLeaderboard = getDelegateLeaderboard();
        return theFullLeaderboard == null ? Collections.emptySet() : theFullLeaderboard.getRaceColumns();
    }

    @Override
    public RaceColumn getRaceColumnByName(String name) {
        return getDelegateLeaderboard().getRaceColumnByName(name);
    }

    @Override
    public void setCarriedPoints(Competitor competitor, double carriedPoints) {
        getDelegateLeaderboard().setCarriedPoints(competitor, carriedPoints);
    }

    @Override
    public void unsetCarriedPoints(Competitor competitor) {
        getDelegateLeaderboard().unsetCarriedPoints(competitor);
    }

    @Override
    public boolean hasCarriedPoints() {
        return getDelegateLeaderboard().hasCarriedPoints();
    }

    @Override
    public boolean hasCarriedPoints(Competitor competitor) {
        return getDelegateLeaderboard().hasCarriedPoints(competitor);
    }

    @Override
    public SettableScoreCorrection getScoreCorrection() {
        return getDelegateLeaderboard().getScoreCorrection();
    }

    @Override
    public void addScoreCorrectionListener(ScoreCorrectionListener listener) {
        fullLeaderboard.runOrSchedule(leaderboard->leaderboard.addScoreCorrectionListener(listener));
    }

    @Override
    public void removeScoreCorrectionListener(ScoreCorrectionListener listener) {
        fullLeaderboard.runOrSchedule(leaderboard->leaderboard.removeScoreCorrectionListener(listener));
    }

    @Override
    public Competitor getCompetitorByName(String competitorName) {
        return getDelegateLeaderboard().getCompetitorByName(competitorName);
    }

    public void setDisplayName(Competitor competitor, String displayName) {
        getDelegateLeaderboard().setDisplayName(competitor, displayName);
    }

    @Override
    public String getDisplayName(Competitor competitor) {
        return getDelegateLeaderboard().getDisplayName(competitor);
    }

    @Override
    public boolean countRaceForComparisonWithDiscardingThresholds(Competitor competitor, RaceColumn raceColumn,
            TimePoint timePoint) {
        return getDelegateLeaderboard().countRaceForComparisonWithDiscardingThresholds(competitor, raceColumn, timePoint);
    }

    @Override
    public ResultDiscardingRule getResultDiscardingRule() {
        return getDelegateLeaderboard().getResultDiscardingRule();
    }

    @Override
    public void setCrossLeaderboardResultDiscardingRule(ThresholdBasedResultDiscardingRule discardingRule) {
        getDelegateLeaderboard().setCrossLeaderboardResultDiscardingRule(discardingRule);
    }

    @Override
    public Competitor getCompetitorByIdAsString(String idAsString) {
        return getDelegateLeaderboard().getCompetitorByIdAsString(idAsString);
    }

    @Override
    public void addRaceColumnListener(RaceColumnListener listener) {
        getDelegateLeaderboard().addRaceColumnListener(listener);
    }

    @Override
    public void removeRaceColumnListener(RaceColumnListener listener) {
        getDelegateLeaderboard().removeRaceColumnListener(listener);
    }

    @Override
    public Long getDelayToLiveInMillis() {
        return getDelegateLeaderboard().getDelayToLiveInMillis();
    }

    @Override
    public Iterable<TrackedRace> getTrackedRaces() {
        return getDelegateLeaderboard().getTrackedRaces();
    }

    @Override
    public ScoringScheme getScoringScheme() {
        return getDelegateLeaderboard().getScoringScheme();
    }

    @Override
    public TimePoint getTimePointOfLatestModification() {
        return getDelegateLeaderboard().getTimePointOfLatestModification();
    }

    @Override
    public Pair<GPSFixMoving, Speed> getMaximumSpeedOverGround(Competitor competitor, TimePoint timePoint) {
        return getDelegateLeaderboard().getMaximumSpeedOverGround(competitor, timePoint);
    }

    @Override
    public Speed getAverageSpeedOverGround(Competitor competitor, TimePoint timePoint) {
        return getDelegateLeaderboard().getAverageSpeedOverGround(competitor, timePoint);
    }

    @Override
    public Double getNetPoints(Competitor competitor, RaceColumn raceColumn, Iterable<RaceColumn> raceColumnsToConsider,
            TimePoint timePoint) throws NoWindException {
        return getDelegateLeaderboard().getNetPoints(competitor, raceColumn, raceColumnsToConsider, timePoint);
    }

    @Override
    public Double getNetPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint,
            Set<RaceColumn> discardedRaceColumns) {
        return getDelegateLeaderboard().getNetPoints(competitor, raceColumn, timePoint, discardedRaceColumns);
    }

    @Override
    public TimePoint getNowMinusDelay() {
        return getDelegateLeaderboard().getNowMinusDelay();
    }

    @Override
    public Iterable<CourseArea> getCourseAreas() {
        return getDelegateLeaderboard().getCourseAreas();
    }

    @Override
    public NumberOfCompetitorsInLeaderboardFetcher getNumberOfCompetitorsInLeaderboardFetcher() {
        return getDelegateLeaderboard().getNumberOfCompetitorsInLeaderboardFetcher();
    }

    @Override
    public Pair<RaceColumn, Fleet> getRaceColumnAndFleet(TrackedRace trackedRace) {
        return getDelegateLeaderboard().getRaceColumnAndFleet(trackedRace);
    }

    @Override
    public BoatClass getBoatClass() {
        return getDelegateLeaderboard().getBoatClass();
    }

    @Override
    public Boat getBoatOfCompetitor(Competitor competitor, RaceColumn raceColumn, Fleet fleet) {
        return getDelegateLeaderboard().getBoatOfCompetitor(competitor, raceColumn, fleet);
    }

    /**
     * Before being serialized, ensure that the leaderboard supplier has been used
     * to resolve the leaderboard.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        getDelegateLeaderboard();
        oos.defaultWriteObject();
    }

    @Override
    public Iterable<Boat> getBoatsRegisteredInRegattaLog() {
        return getDelegateLeaderboard().getBoatsRegisteredInRegattaLog();
    }

    @Override
    public Iterable<Boat> getAllBoats() {
        return getDelegateLeaderboard().getAllBoats();
    }

    @Override
    public void registerBoat(Boat boat) {
        getDelegateLeaderboard().registerBoat(boat);
    }

    @Override
    public void registerBoats(Iterable<Boat> boats) {
        getDelegateLeaderboard().registerBoats(boats);
    }

    @Override
    public void deregisterBoat(Boat boat) {
        getDelegateLeaderboard().deregisterBoat(boat);
    }

    @Override
    public void deregisterBoats(Iterable<Boat> boats) {
        getDelegateLeaderboard().deregisterBoats(boats);
    }

    @Override
    public Double getNetPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint,
            Set<RaceColumn> discardedRaceColumns, Supplier<Double> totalPointsProvider) {
        return getDelegateLeaderboard().getNetPoints(competitor, raceColumn, timePoint, discardedRaceColumns,
                totalPointsProvider);
    }

    @Override
    public boolean isResultsAreOfficial(RaceColumn raceColumn, Fleet fleet) {
        return getDelegateLeaderboard().isResultsAreOfficial(raceColumn, fleet);
    }
}
