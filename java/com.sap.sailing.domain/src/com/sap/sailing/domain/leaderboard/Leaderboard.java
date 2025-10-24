package com.sap.sailing.domain.leaderboard;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.LeaderboardBase;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionForCompetitorInRace;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCache;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.leaderboard.caching.LiveLeaderboardUpdater;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.metering.HasCPUMeter;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

/**
 * A leaderboard is used to display the results of one or more {@link TrackedRace races}. It manages the competitors'
 * scores and can aggregate them, e.g., to show the overall regatta standings. In addition to the races, a "carry"
 * column may be used to carry results of races not displayed in the leaderboard into the calculations.
 * <p>
 *
 * While a single {@link TrackedRace} can tell about the ranks in which according to the tracking information the
 * competitors crossed the finish line, the leaderboard may overlay this information with disqualifications, changes in
 * results because the finish-line tracking was inaccurate, jury penalties and discarded results (depending on the
 * regatta rules, the worst zero, one or more races of each competitor are discarded from the aggregated points).
 * <p>
 *
 * @author Axel Uhl (d043530)
 *
 */
public interface Leaderboard extends LeaderboardBase, HasRaceColumns, HasCPUMeter {
    /**
     * If the leaderboard is a "matrix" with the cells being defined by a competitor / race "coordinate,"
     * then this interface defines the structure of the "cells."
     *
     * @author Axel Uhl (d043530)
     *
     */
    public interface Entry {
        int getTrackedRank();
        Double getTotalPoints();
        Double getIncrementalScoreCorrectionInPoints();
        Double getTotalPointsUncorrected();
        Double getNetPoints();
        MaxPointsReason getMaxPointsReason();
        boolean isDiscarded();
        /**
         * Tells if the total points have been corrected by a {@link ScoreCorrection}
         */
        boolean isTotalPointsCorrected();

        /**
         * @return <code>null</code>, if the competitor's fleet in the race column cannot be determined, the
         *         {@link Fleet} otherwise
         */
        Fleet getFleet();
    }

    /**
     * This class is a default RankComparable that uses the ranks as Comparable.
     * This class is only used as a placeholder while the new RankingSystem is under development.
     *
     * @author I518097
     *
     */
    public class RankComparableRank implements RankComparable {
        private final Integer rank;

        public RankComparableRank(Integer rank) {
            assert rank != null;
            if (rank == null) {
                throw new NullPointerException("rank must not be null");
            }
            this.rank = rank;
        }
        @Override
        public int compareTo(RankComparable o) {
            RankComparableRank otherRankComparableRank = (RankComparableRank) o; 
            final int result; 
            if (rank == 0) {
                result = 1; // the other RankComparable is better because this RankComparable has not started
            } else {
                if (otherRankComparableRank.rank == 0) {
                    result =-1; // this RankComparable is better (lower rank) than the other one because the other one
                               // has not started
                } else {
                    result = rank - otherRankComparableRank.rank; // normal integer compare
                }
            }
            return result;
        }
    }

    LeaderboardDTO computeDTO(final TimePoint timePoint,
            final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            final boolean waitForLatestAnalyses, TrackedRegattaRegistry trackedRegattaRegistry,
            DomainFactory baseDomainFactory, boolean fillTotalPointsUncorrected) throws NoWindException;

    /**
     * Obtains the unique set of {@link Competitor} objects from all {@link TrackedRace}s currently linked to this
     * leaderboard, with suppressed competitors removed. See also {@link #getAllCompetitors()} which also returns
     * the suppressed competitors.
     */
    Iterable<Competitor> getCompetitors();

    /**
     * A leaderboard may suppress particular competitors which are then not assigned a score and shall not be displayed
     * in a regular view of the leaderboard. Editors for leaderboards shall, though, also display the suppressed competitors
     * together with a visual indication showing which competitors are currently suppressed. The "suppressed-state" of a
     * competitor can change over the life cycle of a leaderboard. A typical use case for suppressing a competitor is
     * removing one-time entries from a series (meta-)leaderboard or suppressing a camera boat in scoring.<p>
     *
     * @return all competitors in this leaderboard, including the suppressed ones.
     */
    Iterable<Competitor> getAllCompetitors();

    Iterable<Boat> getAllBoats();

    /**
     * Same as {@link #getAllCompetitors()}, only that additionally the method returns as a first element in a pair
     * which {@link RaceDefinition}s were used in order to fetch their {@link RaceDefinition#getCompetitors()} to
     * assemble the results.
     */
    Pair<Iterable<RaceDefinition>, Iterable<Competitor>> getAllCompetitorsWithRaceDefinitionsConsidered();

    /**
     * Retrieves all competitors expected to race in the fleet and column specified.
     * When a {@link TrackedRace} is {@link RaceColumn#getTrackedRace(Fleet) attached} to the race
     * column for the <code>fleet</code> specified, its competitor set is returned. Otherwise,
     * the competitors are collected from any other information, such as a regatta log and/or the
     * race log for the combination of race column and fleet or, in case of a meta-leaderboard,
     * from the leaderboard represented by the race column.
     */
    Iterable<Competitor> getAllCompetitors(RaceColumn raceColumn, Fleet fleet);

    /**
     * Same as {@link #getAllCompetitors(RaceColumn, Fleet)} with competitors from {@link #getSuppressedCompetitors()}
     * removed
     */
    Iterable<Competitor> getCompetitors(RaceColumn raceColumn, Fleet fleet);

    /**
     * Convenience method which returns the difference between {@link #getAllCompetitors()} and {@link #getCompetitors()}.
     * The collection is a copy that can safely be held and modified, but it is not "live" in the sense that it does not
     * reflect changes that occur to the set of suppressed competitors after calling this method, nor vice-versa.
     */
    Iterable<Competitor> getSuppressedCompetitors();

    /**
     * Tells whether {@code competitor} is among those {@link #getSuppressedCompetitors() suppressed}
     */
    boolean isSuppressed(Competitor competitor);

    /**
     * Can be used to exclude competitor from regular views of this leaderboard as well as from the scoring process.
     * As a result of suppressing a competitor, it will no longer result from calls to {@link #getCompetitors} nor to
     * {@link #getCompetitorsFromBestToWorst(TimePoint)} nor {@link #getCompetitorsFromBestToWorst(RaceColumn, TimePoint)}.
     * It will, however, continue to be returned from {@link #getAllCompetitors()}.
     */
    void setSuppressed(Competitor competitor, boolean suppressed);

    /**
     * Retrieves the boat of a given competitor for the specified raceColumn and fleet.
     */
    Boat getBoatOfCompetitor(Competitor competitor, RaceColumn raceColumn, Fleet fleet);

    /**
     * Returns the first fleet found in the sequence of this leaderboard's {@link #getRaceColumns() race columns}'
     * {@link RaceColumn#getFleets() fleets} whose name equals <code>fleetName</code>. If no such fleet is found,
     * <code>null</code> is returned. If <code>fleetName</code> is <code>null</code>, the leaderboard may return
     * a default fleet if it has one, or <code>null</code> otherwise.
     */
    Fleet getFleet(String fleetName);

    Entry getEntry(Competitor competitor, RaceColumn race, TimePoint timePoint) throws NoWindException;

    /**
     * Same as {@link #getEntry}, but the discards for the competitor across the leaderboard can be provided for better performance
     * in case the entries for several columns shall be computed by multiple calls to this method.
     *
     * @param discardedRaceColumns
     *            expected to be the result of what we would get if we called {@link #getResultDiscardingRule()}.
     *            {@link ResultDiscardingRule#getDiscardedRaceColumns(Competitor, Leaderboard, Iterable, TimePoint, ScoringScheme)
     *            getDiscardedRaceColumns(competitor, this, raceColumnsToConsider, timePoint)}.
     */
    default Entry getEntry(Competitor competitor, RaceColumn race, TimePoint timePoint, Set<RaceColumn> discardedRaceColumns) {
        return getEntry(competitor, race, timePoint, discardedRaceColumns, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * Like {@link #getEntry(Competitor, RaceColumn, TimePoint, Set)}, but with the option to specify a re-usable cache
     */
    Entry getEntry(Competitor competitor, RaceColumn race, TimePoint timePoint, Set<RaceColumn> discardedRaceColumns,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Computes the competitor's ranks as they were or would have been after each race column (from left to right)
     * was completed.<p>
     *
     * A leaderboard fills up over time, usually "from left to right" with one race after another finishing.
     * For split fleets things can vary slightly. There, one fleet may complete a few races before the another fleet
     * starts with those races. In this case there isn't even any point in time at which all fleets have finished
     * exactly <i>n</i> races. Still, this method pretends such a time point would have existed, actually ignoring
     * the <i>times</i> at which a race took place but only looking at the resulting scores and discards.<p>
     *
     * When computing the ranks after all columns up to and including the race column that is the key of the resulting
     * map, the method applies the discarding and tie breaking rules as they would have had to be applied had the races
     * in the respective column just completed.
     *
     * @return The resulting map is guaranteed to have the same iteration order regarding the race columns
     * as {@link #getRaceColumns()}.
     */
    default Map<RaceColumn, List<Competitor>> getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(
            TimePoint timePoint) throws NoWindException {
        return getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(timePoint,
                new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * Computes the competitor's ranks as they were or would have been after each race column (from left to right)
     * was completed.<p>
     *
     * A leaderboard fills up over time, usually "from left to right" with one race after another finishing.
     * For split fleets things can vary slightly. There, one fleet may complete a few races before the another fleet
     * starts with those races. In this case there isn't even any point in time at which all fleets have finished
     * exactly <i>n</i> races. Still, this method pretends such a time point would have existed, actually ignoring
     * the <i>times</i> at which a race took place but only looking at the resulting scores and discards.<p>
     *
     * When computing the ranks after all columns up to and including the race column that is the key of the resulting
     * map, the method applies the discarding and tie breaking rules as they would have had to be applied had the races
     * in the respective column just completed.
     *
     * @return The resulting map is guaranteed to have the same iteration order regarding the race columns
     * as {@link #getRaceColumns()}.
     */
    Map<RaceColumn, List<Competitor>> getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException;

    /**
     * Computes the competitor's net points sum as they were or would have been after each race column (from left to right)
     * was completed.<p>
     *
     * A leaderboard fills up over time, usually "from left to right" with one race after another finishing.
     * For split fleets things can vary slightly. There, one fleet may complete a few races before the another fleet
     * starts with those races. In this case there isn't even any point in time at which all fleets have finished
     * exactly <i>n</i> races. Still, this method pretends such a time point would have existed, actually ignoring
     * the <i>times</i> at which a race took place but only looking at the resulting scores and discards.<p>
     * 
     * When computing the net points sum after all columns up to and including the race column that is the key of the resulting
     * map, the method applies the discarding and tie breaking rules as they would have had to be applied had the races
     * in the respective column just completed.
     *
     * @return The resulting map is guaranteed to have the same iteration order regarding the race columns
     * as {@link #getRaceColumns()}.
     */
    Map<RaceColumn, Map<Competitor, Double>> getNetPointsSumAfterRaceColumn(TimePoint timePoint) throws NoWindException;

    /**
     * Tells the number of points carried over from previous races not tracked by this leaderboard for
     * the <code>competitor</code>. Returns <code>0</code> if there is no carried points definition for
     * <code>competitor</code>.
     */
    double getCarriedPoints(Competitor competitor);

    /**
     *
     * @return an unmodifiable map of competitors and their carried points. The key set can be a true super set of what
     *         {@link #getAllCompetitors()} returns.
     */
    Map<Competitor, Double> getCompetitorsForWhichThereAreCarriedPoints();

    /**
     * Shorthand for {@link TrackedRace#getRank(Competitor, com.sap.sse.common.TimePoint)} with the additional logic
     * that in case the <code>race</code> hasn't {@link TrackedRace#hasStarted(TimePoint) started} yet or no
     * {@link TrackedRace} exists for <code>race</code>, 0 will be returned for all those competitors. The tracked race
     * for the correct {@link Fleet} is determined using {@link RaceColumn#getTrackedRace(Competitor)}.
     * <p>
     *
     * For each competitor tracking-wise ranking better than <code>competitor</code> but with a
     * {@link #getMaxPointsReason(Competitor, RaceColumn, TimePoint) disqualification reason} given,
     * <code>competitor</code>'s rank is improved by one.
     *
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param race
     *            a race that is contained in the {@link #getRaceColumns()} result
     * @return a 1-based rank, or 0 if no rank can be determined for the {@code competitor} in {@code race}
     */
    default int getTrackedRank(Competitor competitor, RaceColumn race, TimePoint timePoint) {
        return getTrackedRank(competitor, race, timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * Shorthand for {@link TrackedRace#getRank(Competitor, com.sap.sse.common.TimePoint)} with the additional logic
     * that in case the <code>race</code> hasn't {@link TrackedRace#hasStarted(TimePoint) started} yet or no
     * {@link TrackedRace} exists for <code>race</code>, 0 will be returned for all those competitors. The tracked race
     * for the correct {@link Fleet} is determined using {@link RaceColumn#getTrackedRace(Competitor)}.
     * <p>
     *
     * For each competitor tracking-wise ranking better than <code>competitor</code> but with a
     * {@link #getMaxPointsReason(Competitor, RaceColumn, TimePoint) disqualification reason} given,
     * <code>competitor</code>'s rank is improved by one.
     *
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param race
     *            a race that is contained in the {@link #getRaceColumns()} result
     * @return a 1-based rank, or 0 if no rank can be determined for the {@code competitor} in {@code race}
     */
    int getTrackedRank(Competitor competitor, RaceColumn race, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * A possibly corrected number of points for the race specified. Defaults to the result of calling
     * {@link #getTrackedRank(Competitor, TrackedRace, TimePoint)} but may be corrected by disqualifications or calls by
     * the jury for the particular race that differ from the tracking results.
     *
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param raceColumn
     *            a race that is contained in the {@link #getRaceColumns()} result
     * @return <code>null</code> if the competitor didn't participate in the race or the race hasn't started yet at
     *         <code>timePoint</code>
     */
    default Double getTotalPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        return getTotalPoints(competitor, raceColumn, timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * A possibly corrected number of points for the race specified. Defaults to the result of calling
     * {@link #getTrackedRank(Competitor, TrackedRace, TimePoint)} but may be corrected by disqualifications or calls by
     * the jury for the particular race that differ from the tracking results.
     *
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param raceColumn
     *            a race that is contained in the {@link #getRaceColumns()} result
     * @return <code>null</code> if the competitor didn't participate in the race or the race hasn't started yet at
     *         <code>timePoint</code>
     */
    Double getTotalPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Tells if and why a competitor received "penalty" points for a race (however the scoring rules define the
     * points for such a penalty; usually, it would be a high score defined by the number of competitors plus one)
     */
    MaxPointsReason getMaxPointsReason(Competitor competitor, RaceColumn race, TimePoint timePoint);

    /**
     * A possibly corrected number of points for the race specified, multiplied with a column factor which defaults to
     * 1.0, may be overridden by an {@link RaceColumn#getExplicitFactor() explicit factor on the race column} and by the
     * {@link ScoringScheme#getScoreFactor(RaceColumn) scoring scheme} which may apply rules, e.g., for doubling medal
     * race scores. Defaults to the result of calling {@link #getTotalPoints(Competitor, TrackedRace, TimePoint)} but
     * may be corrected by the regatta rules for discarding results. If
     * {@link #isDiscarded(Competitor, RaceColumn, TimePoint) discarded}, the points returned will be 0.
     *
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param race
     *            a race that is contained in the {@link #getRaceColumns()} result
     * @return <code>null</code> if the <code>competitor<code> obtained no score (yet?) in <code>race</code>. This may
     *         happen if the competitor has no score correction for <code>race</code> in this leaderboard and there is
     *         no tracked rank available for the competitor (e.g., because the race hasn't started yet) or the
     *         competitor doesn't appear in any of the race column's attached tracked races. A 0.0 score is returned if
     *         the competitor's result for <code>race</code> is discarded.
     */
    Double getNetPoints(Competitor competitor, RaceColumn race, TimePoint timePoint);

    /**
     * Tells whether the contribution of <code>raceColumn</code> is discarded in the current leaderboard's standings for
     * <code>competitor</code>. A column representing a {@link RaceColumn#isMedalRace() medal race} cannot be discarded.
     * Neither can be a race where the competitor received a non-{@link MaxPointsReason#isDiscardable() discardable}
     * penalty or disqualification.
     */
    boolean isDiscarded(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint);

    /**
     * Sums up the {@link #getNetPoints(Competitor, TrackedRace, TimePoint) net points} of <code>competitor</code>
     * across all races tracked by this leaderboard, respecting the {@link RaceColumn#isStartsWithZeroScore()} property.
     */
    Double getNetPoints(Competitor competitor, TimePoint timePoint);

    /**
     * Sums up the {@link #getNetPoints(Competitor, RaceColumn, TimePoint) net points} of <code>competitor</code>
     * across all race columns listed in <code>raceColumnsToconsider</code>, respecting the
     * {@link RaceColumn#isStartsWithZeroScore()} property.
     */
    Double getNetPoints(Competitor competitor, Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint);

    /**
     * Sorts the competitors according to their ranking in the race column specified. Only competitors who have a score
     * are added to the result list. This excludes competitors whose fleet hasn't raced for the <code>raceColumn</code>
     * yet, and those where no tracked rank is known and no manual score correction was performed.
     * <p>
     *
     * The sorting order considers this leaderboard's scoring scheme including the semantics of
     * {@link Fleet#compareTo(Fleet) ordered fleets} and {@link RaceColumn#isMedalRace() medal races}. The ordering
     * does not consider result discarding because when sorting for a race column it is of interest how the competitor
     * performed in that race and not how the score affected the overall regatta score. Therefore, it is based on
     * {@link #getTotalPoints(Competitor, RaceColumn, TimePoint)} and not on
     * {@link #getNetPoints(Competitor, RaceColumn, TimePoint)}.
     */
    default Iterable<Competitor> getCompetitorsFromBestToWorst(RaceColumn raceColumn, TimePoint timePoint) throws NoWindException {
        return getCompetitorsFromBestToWorst(raceColumn, timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * Sorts the competitors according to their ranking in the race column specified. Only competitors who have a score
     * are added to the result list. This excludes competitors whose fleet hasn't raced for the <code>raceColumn</code>
     * yet, and those where no tracked rank is known and no manual score correction was performed.
     * <p>
     *
     * The sorting order considers this leaderboard's scoring scheme including the semantics of
     * {@link Fleet#compareTo(Fleet) ordered fleets} and {@link RaceColumn#isMedalRace() medal races}. The ordering
     * does not consider result discarding because when sorting for a race column it is of interest how the competitor
     * performed in that race and not how the score affected the overall regatta score. Therefore, it is based on
     * {@link #getTotalPoints(Competitor, RaceColumn, TimePoint)} and not on
     * {@link #getNetPoints(Competitor, RaceColumn, TimePoint)}.
     */
    Iterable<Competitor> getCompetitorsFromBestToWorst(RaceColumn raceColumn, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);
    
    Iterable<Competitor> getCompetitorsFromBestToWorst(final RaceColumn raceColumn, TimePoint timePoint,
            Function<Competitor, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Sorts the competitors according to the overall regatta standings, considering the sorting rules for
     * {@link Series}, {@link Fleet}s, medal races, discarding rules and score corrections. A new list is
     * created per call, so the caller may freely manipulate the result.
     * @throws NoWindException
     */
    default Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) {
        return getCompetitorsFromBestToWorst(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * Sorts the competitors according to the overall regatta standings, considering the sorting rules for
     * {@link Series}, {@link Fleet}s, medal races, discarding rules and score corrections. A new list is
     * created per call, so the caller may freely manipulate the result.
     * @throws NoWindException
     */
    Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Returns the total rank of the given competitor or {@code 0} if no rank can be determined for
     * the {@code competitor} in this leaderboard.
     */
    int getTotalRankOfCompetitor(Competitor competitor, TimePoint timePoint);

    /**
     * Fetches all entries for all competitors of all races tracked by this leaderboard in one sweep. This saves some
     * computational effort compared to fetching all entries separately, particularly because all
     * {@link #isDiscarded(Competitor, RaceColumn, TimePoint) discarded races} of a competitor are computed in one
     * sweep using {@link ResultDiscardingRule#getDiscardedRaceColumns(Competitor, Leaderboard, Iterable, TimePoint, ScoringScheme)} only once.
     * Note that in order to get the {@link #getNetPoints(Competitor, TimePoint) total points} for a competitor
     * for the entire leaderboard, the {@link #getCarriedPoints(Competitor) carried-over points} need to be added.
     */
    Map<Util.Pair<Competitor, RaceColumn>, Entry> getContent(TimePoint timePoint) throws NoWindException;

    /**
     * Retrieves all race columns that were added, either by {@link #addRace(TrackedRace, String, boolean)} or
     * {@link #addRaceColumn(String, boolean)}.
     */
    Iterable<RaceColumn> getRaceColumns();

    /**
     * Retrieves a {@link RaceColumn race column} by the name used in calls to either {@link #addRaceColumn} or
     * {@link #addRace}. If no race column by the requested <code>name</code> exists, <code>null</code> is returned.
     */
    RaceColumn getRaceColumnByName(String name);

    /**
     * A leaderboard can carry over points from races that are not tracked by this leaderboard in detail,
     * so for which no {@link RaceColumn} column is present in this leaderboard. These scores are
     * simply added to the scores tracked by this leaderboard in the {@link #getNetPoints(Competitor, TimePoint)}
     * method.
     */
    void setCarriedPoints(Competitor competitor, double carriedPoints);

    /**
     * Reverses the effect of {@link #setCarriedPoints(Competitor, int)}, i.e., afterwards, asking {@link #getCarriedPoints(Competitor)}
     * will return <code>0</code>. Furthermore, other than invoking {@link #setCarriedPoints(Competitor, int) setCarriedPoints(c, 0)},
     * this will, when executed for all competitors of this leaderboard, have {@link #hasCarriedPoints} return <code>false</code>.
     */
    void unsetCarriedPoints(Competitor competitor);

    /**
     * Tells if a carry-column shall be displayed. If the result is <code>false</code>, then no
     * {@link #setCarriedPoints(Competitor, int) scores are carried} into this leaderboard, and
     * only the race columns will be accumulated by the board.
     */
    boolean hasCarriedPoints();

    boolean hasCarriedPoints(Competitor competitor);

    SettableScoreCorrection getScoreCorrection();

    void addScoreCorrectionListener(ScoreCorrectionListener listener);

    void removeScoreCorrectionListener(ScoreCorrectionListener listener);

    Competitor getCompetitorByName(String competitorName);

    void setDisplayName(Competitor competitor, String displayName);

    void setDisplayName(String displayName);

    /**
     * If a display name different from the competitor's {@link Competitor#getName() name} has been defined,
     * this method returns it; otherwise, <code>null</code> is returned.
     */
    String getDisplayName(Competitor competitor);

    /**
     * Tells if the column represented by <code>raceColumn</code> shall be considered when counting the number of "races
     * so far" for discarding. Although medal races are never discarded themselves, they still count in determining the
     * number of "races so far" which is then the basis for deciding how many races may be discarded. If a leaderboard
     * has corrections for a column for the <code>competitor</code> that are already to be applied at
     * <code>timePoint</code> (e.g., because the race can be proven to have finished at <code>timePoint</code>), then
     * that column shall be considered for discarding and counts for determining the number of races so far. Also, if a
     * tracked race is connected to the column and has started already, the column is to be considered for discarding
     * unless the column has several unordered fleets and not all fleets have started their race yet (see
     * {@link ScoringScheme#isValidInNetScore(Leaderboard, RaceColumn, Competitor, TimePoint)}).
     */
    boolean countRaceForComparisonWithDiscardingThresholds(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint);

    /**
     * Returns the result discarding rule for this leaderboard. This may be an explicit rule set for the entire leaderboard,
     * or it may be an implicit rule that relies on other data such as a regatta's series-specific result discarding rules.
     */
    ResultDiscardingRule getResultDiscardingRule();

    /**
     * Trying to set a cross-leaderboard result discarding rule may fail with an exception if the leaderboard obtains
     * its result discarding rule in another way. For example, if the leaderboard is a {@link RegattaLeaderboard} and the
     * regatta's series define their own result discarding rules, the regatta leaderboard will use a composite
     * result discarding rule that considers all series-specific result discarding rules.
     */
    public void setCrossLeaderboardResultDiscardingRule(ThresholdBasedResultDiscardingRule discardingRule);

    Competitor getCompetitorByIdAsString(String idAsString);

    void addRaceColumnListener(RaceColumnListener listener);

    void removeRaceColumnListener(RaceColumnListener listener);

    /**
     * For this leaderboard computes the delay to live of the {@link TrackedRace} linked to any of the leaderboard's
     * columns that has the latest start date. If no tracked race is linked to the leaderboard, <code>null</code> is returned.
     */
    Long getDelayToLiveInMillis();

    /**
     * Obtains all {@link TrackedRace}s currently attached to any of the columns of this leaderboard
     */
    Iterable<TrackedRace> getTrackedRaces();

    ScoringScheme getScoringScheme();

    /**
     * Finds out the time point when any of the {@link Leaderboard#getTrackedRaces() tracked races currently attached to
     * the <code>leaderboard</code>} and the {@link Leaderboard#getScoreCorrection() score corrections} have last been
     * modified. If no tracked race is attached and no time-stamped score corrections have been applied to the leaderboard,
     * <code>null</code> is returned.
     */
    TimePoint getTimePointOfLatestModification();

    /**
     * @return <code>null</code> if no tracked race is available for <code>competitor</code> in this leaderboard
     * or the competitor hasn't started sailing a single race at <code>timePoint</code> for any of the tracked
     * races attached to this leaderboard; the fix where the maximum speed was achieved, and the speed value
     */
    Util.Pair<GPSFixMoving, Speed> getMaximumSpeedOverGround(Competitor competitor, TimePoint timePoint);

    /**
     * @return the {@link #getTotalDistanceTraveled(Competitor, TimePoint) total distance} the competitor has traveled
     *         up to {@code timePoint}, divided by the {@link #getTotalTimeSailed(Competitor, TimePoint) total time
     *         sailed} up to this {@code timePoint}; {@code null} in case a zero or {@code null} time has been spent so
     *         far, or a {@code null} distance is returned by {@link #getTotalDistanceTraveled(Competitor, TimePoint)}.
     */
    Speed getAverageSpeedOverGround(Competitor competitor, TimePoint timePoint);

    /**
     * @param legType the leg type for which to add up the times sailed
     * @return <code>null</code> if no tracked race is available for <code>competitor</code> in this leaderboard
     * or the competitor hasn't started sailing a single downwind leg at <code>timePoint</code> for any of the tracked
     * races attached to this leaderboard
     */
    Duration getTotalTimeSailedInLegType(Competitor competitor, LegType legType, TimePoint timePoint) throws NoWindException;

    /**
     * Starts counting when the gun goes off, not when the competitor passed the line.
     *
     * @return <code>null</code> if no tracked race is available for <code>competitor</code> in this leaderboard
     * or the competitor hasn't started sailing a single race at <code>timePoint</code> for any of the tracked
     * races attached to this leaderboard
     */
    Duration getTotalTimeSailed(Competitor competitor, TimePoint timePoint);

    /**
     * Computes the distance the <code>competitor</code> has sailed in the tracked races in this leaderboard, starting
     * to count in each race when the competitor passes the start line, aggregating up to <code>timePoint</code> or the
     * end of the last race, whichever is first.
     *
     * @return <code>null</code> if the <code>competitor</code> hasn't sailed any distance in any tracked race in this
     *         leaderboard
     */
    Distance getTotalDistanceTraveled(Competitor competitor, TimePoint timePoint);

    /**
     * Computes the distance the <code>competitor</code> has been foiling in the tracked races in this leaderboard, starting
     * to count in each race when the competitor passes the start line, aggregating up to <code>timePoint</code> or the
     * end of the last race, whichever is first.
     *
     * @return <code>null</code> if the <code>competitor</code> hasn't foiled any distance in any tracked race in this
     *         leaderboard
     */
    Distance getTotalDistanceFoiled(Competitor competitor, TimePoint timePoint);

    /**
     * Computes the duration the <code>competitor</code> has foiled in the tracked races in this leaderboard, starting
     * to count in each race when the competitor passes the start line, aggregating up to <code>timePoint</code> or the
     * end of the last race, whichever is first.
     *
     * @return <code>null</code> if the <code>competitor</code> hasn't foiled at any time in any tracked race in this
     *         leaderboard
     */
    Duration getTotalDurationFoiled(Competitor competitor, TimePoint timePoint);

    /**
     * Same as {@link #getNetPoints(Competitor, RaceColumn, TimePoint)}, only that for determining the discarded
     * results only <code>raceColumnsToConsider</code> are considered.
     */
    Double getNetPoints(Competitor competitor, RaceColumn raceColumn, Iterable<RaceColumn> raceColumnsToConsider,
            TimePoint timePoint) throws NoWindException;

    /**
     * Same as {@link #getNetPoints(Competitor, RaceColumn, Iterable, TimePoint)}, only that the set of discarded race columns can
     * be specified which is useful when total points are to be computed for more than one column for the same
     * competitor because then the calculation of discards (which requires looking at all columns) only needs to be done
     * once and not again for each column (which would lead to quadratic effort).
     *
     * @param discardedRaceColumns
     *            expected to be the result of what we would get if we called {@link #getResultDiscardingRule()}.
     *            {@link ResultDiscardingRule#getDiscardedRaceColumns(Competitor, Leaderboard, Iterable, TimePoint, ScoringScheme)
     *            getDiscardedRaceColumns(competitor, this, raceColumnsToConsider, timePoint)}.
     */
    Double getNetPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint,
            Set<RaceColumn> discardedRaceColumns);

    /**
     * Same as {@link #getNetPoints(Competitor, RaceColumn, TimePoint, Set)}, only that a supplier for
     * the total points for the {@code competitor} in column {@code raceColumn} at time point {@code timePoint}
     * is provided. This helps if a caller also needs to determine the total points anyway, saving redundant
     * calculations.
     */
    Double getNetPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint,
            Set<RaceColumn> discardedRaceColumns, Supplier<Double> totalPointsProvider);

    TimePoint getNowMinusDelay();

    /**
     * Gets the course areas that the races of this leaderboard are expected to be run on. This can, e.g., be used to
     * implement a filter when retrieving leaderboards from an event.
     *
     * @return the {@link CourseArea} objects on which races of this leaderboard may run; always valid, never
     *         {@code null}, but may be empty
     */
    Iterable<CourseArea> getCourseAreas();

    /**
     * Must be called when the leaderboard is removed from its server, becoming inaccessible. This will give the leaderboard
     * a chance to release all its resources that won't be collected or freed automatically. In particular, a leaderboard may
     * hold on to listeners which in turn are registered with {@link TrackedRace}s and therefore won't be released to the garbage
     * collector unless the tracked race becomes unreferenced. This may take long because the tracked race can generally
     * be referenced by more than one leaderboard. For example, a test leaderboard may additionally reference a tracked
     * race already referenced by an "official" leaderboard. When the test leaderboard is removed, its listeners would not
     * be released and avoid garbage-collecting the test leaderboard. When calling this method, the leaderboard will cleanly
     * unregister its listeners from the tracked races and therefore become eligible for garbage collection.
     */
    void destroy();

    /**
     * Returns a data transfer object (DTO) that has the leaderboard's data for the race columns with basic information
     * for all columns, and with detailed information for those columns whose names are provided in
     * <code>namesOfRaceColumnsForWhichToLoadLegDetails</code>. The leaderboard is evaluated at <code>timePoint</code>,
     * or, if <code>timePoint</code> is <code>null</code>, for the "live" time point (now - delay).
     * <p>
     *
     * The implementation uses different approaches for caching the results. For "live" requests, a
     * {@link LiveLeaderboardUpdater} is used to keep refreshing the cached results. Other queries are managed by a
     * {@link LeaderboardDTOCache} which remembers a number of results before it starts evicting the least frequently
     * used ones.
     *
     * @param timePoint
     *            <code>null</code> for "live" results for the time point "now" - delay; otherwise, the explicit time
     *            point at which to evaluate the leaderboard status
     * @param namesOfRaceColumnsForWhichToLoadLegDetails
     *            the names of the race columns of which to expand the details in the result
     * @param trackedRegattaRegistry
     *            used to determine which of the races are still being tracked and which ones are not
     * @param baseDomainFactory
     *            required as factory and cache for various DTO types
     * @return a leaderboard DTO with no defined security info (ownership, ACL) set; note that the DTO may originate
     *         from a cache, e.g., the {@link LiveLeaderboardUpdater} or the {@link LeaderboardDTOCache}. A caller may
     *         augment the DTO by adding security information that needs to be transmitted to a client. If such
     *         information happens to be found on the object returned from this method it shall be considered "stale"
     *         and has to be updated before sending it to a client. See {@code SecurityDTOUtil.addSecurityInformation(...)}.
     */
    LeaderboardDTO getLeaderboardDTO(TimePoint timePoint,
            Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails,
            boolean addOverallDetails, TrackedRegattaRegistry trackedRegattaRegistry, DomainFactory baseDomainFactory, boolean fillTotalPointsUncorrected) throws NoWindException,
            InterruptedException, ExecutionException;

    NumberOfCompetitorsInLeaderboardFetcher getNumberOfCompetitorsInLeaderboardFetcher();

    /**
     * Gets the ("dominant") boat class for this leaderboard. For a {@link RegattaLeaderboard} this is the {@link Regatta}'s boat class.
     * For a {@link FlexibleLeaderboard} the implementation is more complex because no fixed boat class is set for the leaderboard. There,
     * the boat class will be determined based on the most frequently occurring boat class when iterating across the competitors.
     */
    BoatClass getBoatClass();

    LeaderboardType getLeaderboardType();

    /**
     * Tells if there is at least one non-{@code null} score for the given {@code competitor} in the
     * leaderboard.
     */
    boolean hasScores(Competitor competitor, TimePoint timePoint);

    /**
     * Returns true if a race column evaluates to be a win for the given competitor at the given timepoint. If the
     * competitor is not scored for this race, {@code false} is returned. See
     * {@link ScoringScheme#isWin(Leaderboard, Competitor, RaceColumn, TimePoint)}.
     */
    default boolean isWin(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = new LeaderboardDTOCalculationReuseCache(timePoint);
        return isWin(competitor, raceColumn, timePoint, cache);
    }

    default boolean isWin(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getScoringScheme().isWin(this, competitor, raceColumn, timePoint, c->getTotalPoints(c, raceColumn, timePoint, cache), cache);
    }

    @Override
    default QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    default TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(getName());
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String name) {
        return new TypeRelativeObjectIdentifier(name);
    }

    static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(RegattaName regattaName) {
        return new TypeRelativeObjectIdentifier(regattaName.getRegattaName());
    }

    @Override
    default HasPermissions getPermissionType() {
        return SecuredDomainType.LEADERBOARD;
    }

    default boolean isPartOfEvent(EventBase event) {
        return Util.containsAny(event.getVenue().getCourseAreas(), getCourseAreas());
    }

    /**
     * @return a map whose keys contain different averaging time spans, e.g., 5s, 30s, and 60s, and whose values contain
     *         a pair with the average leaderboard computation time for all computation requests not older than the time
     *         given as the key, and as the second pair component the number of computations in that time period.
     */
    Map<Duration, Pair<Duration, Integer>> getComputationTimeStatistics();

    /**
     * Matches the results in {@code regattaScoreCorrections} to the {@link RaceColumn}s and {@link Competitor}s in this
     * leaderboard. The race columns are identified in {@code regattaScoreCorrections} by the
     * {@link ScoreCorrectionsForRace#getRaceNameOrNumber()} result and the ordering of the
     * {@link ScoreCorrectionsForRace} objects as delivered by
     * {@link RegattaScoreCorrections#getScoreCorrectionsForRaces()} as compared to the {@link #getRaceColumns()}
     * ordering of this leaderboard, furthermore the explicit mappings specified in
     * {@code raceNumberOrNameToRaceColumnMap}. The competitor mapping happens based on the
     * {@link ScoreCorrectionForCompetitorInRace#getSailID()} result that is compared to the {@link Competitor}'s
     * {@link Competitor#getShortName() short name} if the boats can change in this leaderboard, or to the
     * {@link Boat#getSailID()} result of the competitor's boat, overruled by the explicit mappings in
     * {@code sailIdToCompetitorMap}.
     *
     * @param allowRaceDefaultsByOrder
     *            if {@code true}, an attempt will be made to map the race names/numbers from the
     *            {@link RegattaScoreCorrections} to the leaderboard's {@link RaceColumn}s by their ordering, one by
     *            one, but only for those that are not mapped explicitly by {@code raceNumberOrNameToRaceColumnMap}. If
     *            there are excess race names/numbers in the {@link RegattaScoreCorrections} objects beyond the number
     *            of race columns in this leaderboard, no mapping will be inferred for the excess races.
     * @param allowPartialImport
     *            if {@code true}, a valid mapping will result even if the mapping is not complete regarding the set of
     *            races and the set of competitors for which results are provided in {@code regattaScoreCorrections}. If
     *            {@code false} and if one or more competitors or one or more races cannot be mapped successfully to
     *            this leaderboard, {@code null} will be returned, implying that no results shall be imported at all.
     */
    ScoreCorrectionMapping mapRegattaScoreCorrections(RegattaScoreCorrections regattaScoreCorrections,
            Map<String, RaceColumn> raceNumberOrNameToRaceColumnMap, Map<String, Competitor> sailIdToCompetitorMap,
            boolean allowRaceDefaultsByOrder, boolean allowPartialImport);

    boolean isResultsAreOfficial(RaceColumn raceColumn, Fleet fleet);
}