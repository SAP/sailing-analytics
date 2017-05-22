package com.sap.sailing.server.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.ConfirmedFinishPositioningListFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.FinishPositioningListFinder;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardMaxPointsReason;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrection;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrectionMetadata;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class RaceLogScoringReplicator implements RaceColumnListener {
    
    private static final long serialVersionUID = -5958519195756937338L;
    
    private final RacingEventService service;
    private final static String COMMENT_TEXT_ON_SCORE_CORRECTION = "Update triggered by Race Committee.";
    
    public RaceLogScoringReplicator(RacingEventService service) {
        this.service = service;
    }

    @Override
    public void trackedRaceLinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
    }

    @Override
    public void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
    }

    @Override
    public void isMedalRaceChanged(RaceColumn raceColumn, boolean newIsMedalRace) {
    }

    @Override
    public void isFleetsCanRunInParallelChanged(RaceColumn raceColumn, boolean newIsFleetsCanRunInParallel) {
    }

    @Override
    public void isStartsWithZeroScoreChanged(RaceColumn raceColumn, boolean newIsStartsWithZeroScore) {
    }

    @Override
    public void hasSplitFleetContiguousScoringChanged(RaceColumn raceColumn, boolean hasSplitFleetContiguousScoring) {
    }

    @Override
    public void isFirstColumnIsNonDiscardableCarryForwardChanged(RaceColumn raceColumn, boolean firstColumnIsNonDiscardableCarryForward) {
    }

    @Override
    public void raceColumnAddedToContainer(RaceColumn raceColumn) {
    }

    @Override
    public void raceColumnRemovedFromContainer(RaceColumn raceColumn) {
    }

    @Override
    public void raceColumnMoved(RaceColumn raceColumn, int newIndex) {
    }

    @Override
    public void factorChanged(RaceColumn raceColumn, Double oldFactor, Double newFactor) {
    }

    @Override
    public void competitorDisplayNameChanged(Competitor competitor, String oldDisplayName, String displayName) {
    }

    @Override
    public void resultDiscardingRuleChanged(ResultDiscardingRule oldDiscardingRule, ResultDiscardingRule newDiscardingRule) {
    }

    @Override
    public void raceLogEventAdded(RaceColumn raceColumn, RaceLogIdentifier raceLogIdentifier, RaceLogEvent event) {
        event.accept(new BaseRaceLogEventVisitor() {
            @Override
            public void visit(RaceLogFinishPositioningConfirmedEvent event) {
                handleFinishPositioningList(raceColumn, raceLogIdentifier, event);
            }
        });
    }

    private void handleFinishPositioningList(RaceColumn raceColumn, RaceLogIdentifier raceLogIdentifier, RaceLogFinishPositioningConfirmedEvent event) {
        Leaderboard leaderboard = service.getLeaderboardByName(raceLogIdentifier.getRegattaLikeParent().getName());
        if (leaderboard != null) {
            Fleet fleet = raceColumn.getFleetByName(raceLogIdentifier.getFleetName());
            RaceLog raceLog = raceColumn.getRaceLog(fleet);
            checkNeedForScoreCorrectionByResultsOfRaceCommittee(leaderboard, raceColumn, fleet, raceLog, event.getCreatedAt(), event);
        }
    }

    @Override
    public boolean isTransient() {
        return true;
    }
    
    /**
     * Called when a {@link RaceLogFinishPositioningConfirmedEvent} was received by the {@link RaceLog}. Retrieves the
     * last {@link RaceLogFinishPositioningConfirmedEvent} from the racelog and compares the ranks and disqualifications
     * entered by the race committee with the tracked ranks. When a tracked rank for a competitor is not the same as the
     * rank of the race committee, a score correction is issued. The positioning list contains a list of competitors
     * sorted by the positioning order when finishing. Additionally a MaxPointsReason might be entered by the Race
     * Committee.
     * <p>
     * 
     * For backward compatibility (old releases didn't store the {@link CompetitorResults} in the event) if no
     * {@link CompetitorResults} are found even though there was a {@link RaceLogFinishPositioningConfirmedEvent}, the
     * last {@link RaceLogFinishPositioningListChangedEvent} event with {@link CompetitorResults} is looked up and its
     * results are used.
     * 
     * @param timePoint
     *            the TimePoint at which the race committee confirmed their last rank list entered in the app.
     * @param event
     *            the event that has announced the latest results; used with a finder to determine the effective changes to
     *            be applied to the leaderboard; if before {@code event} the race log had a result for a competitor that
     *            is still in the {@code leaderboard}'s score corrections and after applying {@code event} there is no
     *            result for that competitor anymore, remove those score corrections.
     */
    private void checkNeedForScoreCorrectionByResultsOfRaceCommittee(Leaderboard leaderboard, RaceColumn raceColumn,
            Fleet fleet, RaceLog raceLog, TimePoint timePoint, RaceLogFinishPositioningConfirmedEvent event) {
        int numberOfCompetitorsInLeaderboard = Util.size(leaderboard.getCompetitors());
        int numberOfCompetitorsInRace;
        CompetitorResults positioningList;
        numberOfCompetitorsInRace = getNumberOfCompetitorsInRace(raceColumn, fleet, numberOfCompetitorsInLeaderboard);
        final ConfirmedFinishPositioningListFinder confirmedPositioningListFinder = new ConfirmedFinishPositioningListFinder(raceLog);
        positioningList = confirmedPositioningListFinder.analyze();
        if (positioningList == null) {
            // we expect this case for old sailing events such as ESS Singapore, Quingdao, where the confirmation event did not contain the finish
            // positioning list
            FinishPositioningListFinder positioningListFinder = new FinishPositioningListFinder(raceLog);
            positioningList = positioningListFinder.analyze(); // this is OK because the precondition for calling this method is that a
            // RaceLogFinishPositioningConfirmedEvent event was found in the race log
        }
        if (positioningList != null) {
            final Map<Serializable, CompetitorResult> newResultsByCompetitorId = new HashMap<>();
            for (CompetitorResult positionedCompetitor : positioningList) {
                newResultsByCompetitorId.put(positionedCompetitor.getCompetitorId(), positionedCompetitor);
                final Competitor competitor = service.getBaseDomainFactory().getExistingCompetitorById(positionedCompetitor.getCompetitorId());
                // The score is updated when explicitly provided or when no penalty was set;
                // in turn, this means that when a penalty is set and no score is explicitly provided,
                // it is up to the scoring scheme to infer a penalty score for the MaxPointsReason.
                // See also bug 3955.
                if (isNeedToCorrectScore(positionedCompetitor)) {
                    int rankByRaceCommittee = getRankInPositioningListByRaceCommittee(positionedCompetitor);
                    correctScoreInLeaderboard(leaderboard, raceColumn, timePoint, numberOfCompetitorsInRace, competitor,
                            rankByRaceCommittee, positionedCompetitor.getScore());
                }
                setMaxPointsReasonInLeaderboardIfNecessary(leaderboard, raceColumn, timePoint, positionedCompetitor.getMaxPointsReason(), competitor);
            }
            final CompetitorResults oldResults = confirmedPositioningListFinder.analyzeIgnoring(event);
            if (oldResults != null) {
                // check if any of the old results need to be canceled from the leaderboard's score corrections, such
                // as a cleared OCS; see also bug 4025
                for (final CompetitorResult oldResult : oldResults) {
                    if (!newResultsByCompetitorId.containsKey(oldResult.getCompetitorId())) {
                        final Competitor competitor = service.getBaseDomainFactory().getExistingCompetitorById(oldResult.getCompetitorId());
                        // check what the leaderboard score correction looks like; if it matches the old result, remove the correction:
                        if (Util.equalsWithNull(leaderboard.getScoreCorrection().getMaxPointsReason(competitor, raceColumn, TimePoint.EndOfTime), oldResult.getMaxPointsReason())) {
                            service.apply(new UpdateLeaderboardMaxPointsReason(leaderboard.getName(), raceColumn.getName(), competitor.getId().toString(), /* reason */ null, timePoint));
                        }
                        if (isNeedToCorrectScore(oldResult) &&
                                Util.equalsWithNull(leaderboard.getScoreCorrection().getExplicitScoreCorrection(competitor, raceColumn),
                                    getScoreFromRaceCommittee(leaderboard, raceColumn, timePoint, numberOfCompetitorsInRace, competitor, oldResult.getOneBasedRank(), oldResult.getScore()))) {
                            service.apply(new UpdateLeaderboardScoreCorrection(leaderboard.getName(), raceColumn.getName(), competitor.getId().toString(), /* correctedScore */ null, timePoint));
                        }
                    }
                }
            }
            // Since the metadata update is used by the Sailing suite to determine the final state of a race, it has to
            // be triggered, even though no score correction may have been performed
            applyMetadataUpdate(leaderboard, timePoint, COMMENT_TEXT_ON_SCORE_CORRECTION);
        }
    }

    private boolean isNeedToCorrectScore(CompetitorResult positionedCompetitor) {
        return positionedCompetitor.getScore() != null
                || positionedCompetitor.getMaxPointsReason() == null
                || positionedCompetitor.getMaxPointsReason().equals(MaxPointsReason.NONE);
    }

    private boolean setMaxPointsReasonInLeaderboardIfNecessary(Leaderboard leaderboard, RaceColumn raceColumn,
            TimePoint timePoint, MaxPointsReason maxPointsReason, Competitor competitor) {
        boolean scoreHasBeenCorrected = false;
        MaxPointsReason oldMaxPointsReason = leaderboard.getMaxPointsReason(competitor, raceColumn, timePoint);
        MaxPointsReason maxPointsReasonByRaceCommittee = maxPointsReason;
        if (!Util.equalsWithNull(maxPointsReasonByRaceCommittee, oldMaxPointsReason)) {
            applyMaxPointsReasonOperation(leaderboard, raceColumn, competitor, maxPointsReasonByRaceCommittee, timePoint);
            scoreHasBeenCorrected = true;
        }
        return scoreHasBeenCorrected;
    }

    private void correctScoreInLeaderboard(Leaderboard leaderboard, RaceColumn raceColumn, TimePoint timePoint,
            final int numberOfCompetitorsInRace, 
            Competitor competitor, int rankByRaceCommittee, Double optionalExplicitScore) {
        final Double scoreByRaceCommittee = getScoreFromRaceCommittee(leaderboard, raceColumn, timePoint,
                numberOfCompetitorsInRace, competitor, rankByRaceCommittee, optionalExplicitScore);
        // Do ALWAYS apply score corrections from race committee
        applyScoreCorrectionOperation(leaderboard, raceColumn, competitor, scoreByRaceCommittee, timePoint);
    }

    /**
     * If a non-{@code null} {@code optionalExplicitScore} is provided, it it returned. Otherwise, an implicit score is
     * determined from the {@code rankByRaceCommittee} using the leaderboard scoring scheme's
     * {@link ScoringScheme#getScoreForRank(Leaderboard, RaceColumn, Competitor, int, java.util.concurrent.Callable, com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher, TimePoint)
     * getScoreForRank(...)} method. Usually, scoring schemes will return {@code null} as score for {@code 0} as
     * (one-based) rank.
     */
    private Double getScoreFromRaceCommittee(Leaderboard leaderboard, RaceColumn raceColumn, TimePoint timePoint,
            final int numberOfCompetitorsInRace, Competitor competitor, int rankByRaceCommittee,
            Double optionalExplicitScore) {
        final Double scoreByRaceCommittee;
        if (optionalExplicitScore == null) {
            scoreByRaceCommittee = leaderboard.getScoringScheme().getScoreForRank(leaderboard, raceColumn, competitor,
                rankByRaceCommittee, ()->numberOfCompetitorsInRace, leaderboard.getNumberOfCompetitorsInLeaderboardFetcher(), timePoint);
        } else {
            scoreByRaceCommittee = optionalExplicitScore;
        }
        return scoreByRaceCommittee;
    }

    private void applyScoreCorrectionOperation(Leaderboard leaderboard, RaceColumn raceColumn, Competitor competitor, Double correctedScore, TimePoint timePoint) {
        RacingEventServiceOperation<?> operation = new UpdateLeaderboardScoreCorrection(leaderboard.getName(), raceColumn.getName(), competitor.getId().toString(), correctedScore, timePoint);
        service.apply(operation);
    }
    
    private void applyMaxPointsReasonOperation(Leaderboard leaderboard, RaceColumn raceColumn, Competitor competitor, MaxPointsReason reason, TimePoint timePoint) {
        RacingEventServiceOperation<?> operation = new UpdateLeaderboardMaxPointsReason(leaderboard.getName(), raceColumn.getName(), competitor.getId().toString(), reason, timePoint);
        service.apply(operation);
    }
    
    private void applyMetadataUpdate(Leaderboard leaderboard, TimePoint timePointOfLastCorrectionValidity, String comment) {
        RacingEventServiceOperation<?> operation = new UpdateLeaderboardScoreCorrectionMetadata(leaderboard.getName(), timePointOfLastCorrectionValidity, comment);
        service.apply(operation);
    }

    /**
     * The positioning list contains a list of competitors sorted by the positioning order when finishing. Additionally
     * a MaxPointsReason might be entered by the Race Committee. The rank of a competitor according to the Race
     * Committee used to be represented by the position in the list in earlier versions; now it is made explicit in the
     * {@link CompetitorResult#getOneBasedRank()} attribute. 
     * @param positionedCompetitor
     *            the competitor whose rank shall be determined
     * 
     * @return the (one-based) rank of the given positionedCompetitor
     */
    private int getRankInPositioningListByRaceCommittee(CompetitorResult positionedCompetitor) {
        return positionedCompetitor.getOneBasedRank();
    }

    private int getNumberOfCompetitorsInRace(RaceColumn raceColumn, Fleet fleet, int numberOfCompetitorsInLeaderboard) {
        int numberOfCompetitorsInRace;
        if (raceColumn.getRaceDefinition(fleet) != null) {
            numberOfCompetitorsInRace = Util.size(raceColumn.getRaceDefinition(fleet).getCompetitors());
        } else {
            numberOfCompetitorsInRace = numberOfCompetitorsInLeaderboard;
        }
        return numberOfCompetitorsInRace;
    }

}
