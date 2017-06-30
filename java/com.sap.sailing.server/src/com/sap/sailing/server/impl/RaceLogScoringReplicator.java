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
import com.sap.sailing.domain.base.impl.RaceColumnListenerWithDefaultAction;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardMaxPointsReason;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrection;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrectionMetadata;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class RaceLogScoringReplicator implements RaceColumnListenerWithDefaultAction {
    
    private static final long serialVersionUID = -5958519195756937338L;
    
    private final RacingEventService service;
    private final static String COMMENT_TEXT_ON_SCORE_CORRECTION_SINGLE_FLEET = "Results of race %s have been updated.";
    private final static String COMMENT_TEXT_ON_SCORE_CORRECTION_MULTI_FLEET = "Results of race %s, %s have been updated.";
    
    public RaceLogScoringReplicator(RacingEventService service) {
        this.service = service;
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
     * last {@link RaceLogFinishPositioningConfirmedEvent} from the racelog and applies the ranks and disqualifications
     * entered by the race committee to the score correstions as follows:
     * <ul>
     * <li>When no {@link CompetitorResult} object is received for a competitor, no change is applied to that
     * competitor's score corrections.</li>
     * <li>When a {@link CompetitorResult} object is received for a competitor, if it contains {@code null} for the
     * score and 0 for the rank, any existing score correction (points) is removed for that competitor, otherwise, the
     * score correction (points) from the CompetitorResult object is applied if provided (in this case a rank provided
     * in the {@link CompetitorResult} object is ignored); if only a rank but no score is provided in the
     * {@link CompetitorResult}, the scoring scheme will compute the score from the rank, and that score will be applied
     * as a score correction.</li>
     * <li>When a {@link CompetitorResult} object is received for a competitor, if it contains {@code null} for the
     * {@link MaxPointsReason}, any existing {@link MaxPointsReason} is removed for that competitor; otherwise, the
     * {@link MaxPointsReason} from the {@link CompetitorResult} object is applied. This is handled entirely
     * independently of the score (points).</li>
     * </ul>
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
     *            the event that has announced the latest results; used with a finder to determine the effective changes
     *            to be applied to the leaderboard; if before {@code event} the race log had a result for a competitor
     *            that is still in the {@code leaderboard}'s score corrections and after applying {@code event} there is
     *            no result for that competitor anymore, remove those score corrections.
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
                int rankByRaceCommittee = getRankInPositioningListByRaceCommittee(positionedCompetitor);
                correctScoreInLeaderboardIfNecessary(leaderboard, raceColumn, timePoint, numberOfCompetitorsInRace, competitor,
                        rankByRaceCommittee, positionedCompetitor.getScore());
                setMaxPointsReasonInLeaderboardIfNecessary(leaderboard, raceColumn, timePoint, positionedCompetitor.getMaxPointsReason(), competitor);
            }
            // Since the metadata update is used by the Sailing suite to determine the final state of a race, it has to
            // be triggered, even though no score correction may have been performed
            String comment = LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleet.getName())
                    ? String.format(COMMENT_TEXT_ON_SCORE_CORRECTION_SINGLE_FLEET, raceColumn.getName())
                    : String.format(COMMENT_TEXT_ON_SCORE_CORRECTION_MULTI_FLEET, raceColumn.getName(), fleet.getName());
            applyMetadataUpdate(leaderboard, timePoint, comment);
        }
    }

    private void setMaxPointsReasonInLeaderboardIfNecessary(Leaderboard leaderboard, RaceColumn raceColumn,
            TimePoint timePoint, MaxPointsReason maxPointsReason, Competitor competitor) {
        MaxPointsReason oldMaxPointsReason = leaderboard.getMaxPointsReason(competitor, raceColumn, timePoint);
        MaxPointsReason maxPointsReasonByRaceCommittee = maxPointsReason;
        if (!Util.equalsWithNull(maxPointsReasonByRaceCommittee, oldMaxPointsReason)) {
            applyMaxPointsReasonOperation(leaderboard, raceColumn, competitor, maxPointsReasonByRaceCommittee, timePoint);
        }
    }

    private void correctScoreInLeaderboardIfNecessary(Leaderboard leaderboard, RaceColumn raceColumn, TimePoint timePoint,
            final int numberOfCompetitorsInRace, 
            Competitor competitor, int rankByRaceCommittee, Double optionalExplicitScore) {
        final Double scoreByRaceCommittee = getScoreFromRaceCommittee(leaderboard, raceColumn, timePoint,
                numberOfCompetitorsInRace, competitor, rankByRaceCommittee, optionalExplicitScore);
        // Apply the score correction if it will cause a change in the explicit score set for the competitor
        if (!Util.equalsWithNull(leaderboard.getScoreCorrection().getExplicitScoreCorrection(competitor, raceColumn), scoreByRaceCommittee)) {
            applyScoreCorrectionOperation(leaderboard, raceColumn, competitor, scoreByRaceCommittee, timePoint);
        }
    }

    /**
     * If a non-{@code null} {@code optionalExplicitScore} is provided, it it returned. Otherwise, a score is determined
     * from the {@code oneBasedRankByRaceCommittee} using the leaderboard scoring scheme's
     * {@link ScoringScheme#getScoreForRank(Leaderboard, RaceColumn, Competitor, int, java.util.concurrent.Callable, com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher, TimePoint)
     * getScoreForRank(...)} method. Usually, scoring schemes will return {@code null} as score for {@code 0} as
     * (one-based) rank.
     */
    private Double getScoreFromRaceCommittee(Leaderboard leaderboard, RaceColumn raceColumn, TimePoint timePoint,
            final int numberOfCompetitorsInRace, Competitor competitor, int oneBasedRankByRaceCommittee,
            Double optionalExplicitScore) {
        final Double scoreByRaceCommittee;
        if (optionalExplicitScore == null) {
            scoreByRaceCommittee = leaderboard.getScoringScheme().getScoreForRank(leaderboard, raceColumn, competitor,
                oneBasedRankByRaceCommittee, ()->numberOfCompetitorsInRace, leaderboard.getNumberOfCompetitorsInLeaderboardFetcher(), timePoint);
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

    /**
     * The default action of this {@link RaceColumnListener} is to do nothing.
     */
    @Override
    public void defaultAction() {
    }

}
