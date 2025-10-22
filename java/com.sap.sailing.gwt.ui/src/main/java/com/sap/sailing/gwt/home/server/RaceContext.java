package com.sap.sailing.gwt.home.server;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbortingFlagFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.FlagPoleState;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.TimingConstants;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.racelog.FlagPole;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoreCorrection;
import com.sap.sailing.domain.tracking.RaceWindCalculator;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindSummary;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.event.LiveRaceDTO;
import com.sap.sailing.gwt.home.communication.event.RaceListRaceDTO;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO.RaceDataInfo;
import com.sap.sailing.gwt.home.communication.race.FlagStateDTO;
import com.sap.sailing.gwt.home.communication.race.FleetMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.RaceMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.RaceProgressDTO;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO.RaceTrackingState;
import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO.RaceViewState;
import com.sap.sailing.gwt.home.communication.race.wind.SimpleWindDTO;
import com.sap.sailing.gwt.home.communication.race.wind.WindStatisticsDTO;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaType;
import com.sap.sse.gwt.shared.DTO;
import com.sap.sse.security.ui.server.SecurityDTOUtil;

/**
 * This class aggregates race information by preparing {@link DTO}s for different components representing a race in the
 * UI and providing convenience methods for several race state and other required information.<p>
 * 
 * An object of this type represents a snapshot of a race for the time point when the object is created. Using the
 * object at a later point in time will still represent the race at the time when this object was created, including
 * the race's flag state, live state and view state.
 */
@GwtIncompatible
public class RaceContext {
    private static final Logger logger = Logger.getLogger(RaceContext.class.getName());
    private static final Duration TIME_BEFORE_START_TO_SHOW_RACES_AS_LIVE = Duration.ONE_HOUR;
    private static final Duration TIME_TO_SHOW_CANCELED_RACES_AS_LIVE = Duration.ONE_MINUTE.times(5);
    private final TimePoint now = MillisecondsTimePoint.now();
    private final LeaderboardContext leaderboardContext;
    private final Leaderboard leaderboard;
    private final RaceColumn raceColumn;
    private final Fleet fleet;
    private final RaceDefinition raceDefinition;
    private final TrackedRace trackedRace;
    private final RaceLog raceLog;
    private final ReadonlyRaceState state;
    private final Event event;
    private final RacingEventService service;
    private TimePoint startTime;
    private boolean startTimeCalculated = false;
    private TimePoint finishTime;
    private boolean finishTimeCalculated = false;
    private RaceViewState raceViewState;

    public RaceContext(RacingEventService service, Event event, LeaderboardContext leaderboardContext, 
            RaceColumn raceColumn, Fleet fleet, RaceLogResolver raceLogResolver) {
        this.service = service;
        this.event = event;
        this.leaderboardContext = leaderboardContext;
        this.leaderboard = leaderboardContext.getLeaderboard();
        this.raceColumn = raceColumn;
        this.raceDefinition = raceColumn.getRaceDefinition(fleet);
        this.fleet = fleet;
        trackedRace = raceColumn.getTrackedRace(fleet);
        raceLog = raceColumn.getRaceLog(fleet);
        state = (raceLog == null) ? null : ReadonlyRaceStateImpl.getOrCreate(raceLogResolver, raceLog);
    }

    private boolean isShowFleetData() {
        return !LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleet.getName());
    }

    public String getLeaderboardName() {
        return leaderboard.getName();
    }

    public String getRegattaName() {
        final String result;
        final Regatta regatta = getRegatta();
        if (regatta != null) {
            result = regatta.getName();
        } else {
            result = leaderboard.getName();
        }
        return result;
    }
    
    /**
     * Returns {@code null} if the {@link #leaderboard};s type does not conform to {@link RegattaLeaderboard}
     */
    public Regatta getRegatta() {
        final Regatta result;
        if (leaderboard instanceof RegattaLeaderboard) {
            result = ((RegattaLeaderboard) leaderboard).getRegatta();
        } else {
            result = null;
        }
        return result;
    }

    private String getRegattaDisplayName() {
        String displayName = leaderboard.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        return leaderboard.getName();
    }

    private FleetMetadataDTO getFleetMetadataOrNull() {
        if (!isShowFleetData()) {
            return null;
        }
        return getFleetMetadata();
    }

    /**
     * Prepares a {@link FleetMetadataDTO} which is used to show the regattas progress or to visualize the races in
     * competition format. 
     * 
     * @return the {@link FleetMetadataDTO} instance
     */
    public FleetMetadataDTO getFleetMetadata() {
        return new FleetMetadataDTO(fleet.getName(), fleet.getColor() == null ? null : fleet.getColor().getAsHtml(),
                fleet.getOrdering());
    }

    private SimpleWindDTO getWindOrNull() {
        final SimpleWindDTO result;
        if (trackedRace != null) {
            TimePoint toTimePoint = getWindToTimePoint();
            WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> averagedWindWithConfidence =
                    new RaceWindCalculator().getWindFromTrackedRace(toTimePoint, trackedRace);
            if (averagedWindWithConfidence != null) {
                result = new SimpleWindDTO(averagedWindWithConfidence.getObject().getFrom().getDegrees(), averagedWindWithConfidence.getObject().getKnots());
            } else {
                result = null;
            }
        } else {
            Wind wind = new RaceWindCalculator().checkForLatestWindFixFromRaceLog(raceLog);
            if (wind != null) {
                result = new SimpleWindDTO(wind.getFrom().getDegrees(), wind.getKnots());
            } else {
                result = null;
            }
        }
        return result;
    }
    
    /**
     * Determines the end of the interval for which to compute wind data for the race. Usually, this will be the
     * {@link getFinishTime() end of the race}; however, if this time point is not (yet) known, e.g., during
     * a live race that is still ongoing, instead the current time point minus the live delay is chosen. If, however,
     * the {@link TrackedRace#getTimePointOfNewestEvent() the race's newest event} is older than this time point,
     * the time point of the race's newest event is used.
     */
    private TimePoint getWindToTimePoint() {
        final TimePoint finishTime = getFinishTime();
        TimePoint toTimePoint = finishTime == null ? getLiveTimePoint() : finishTime;
        if (trackedRace != null) {
            TimePoint newestEvent = trackedRace.getTimePointOfNewestEvent();
            if (newestEvent != null && newestEvent.before(toTimePoint)) {
                toTimePoint = newestEvent;
            }
        }
        return toTimePoint;
    }

    private FlagStateDTO getFlagStateOrNull() {
        if(raceLog == null) {
            return null;
        }
        // Code extracted from SailingServiceImpl.createRaceInfoDTO
        // TODO: extract to to util to be used from both places
        TimePoint startTime = state.getStartTime();
        Flags lastUpperFlag = null;
        Flags lastLowerFlag = null;
        boolean lastFlagsAreDisplayed = false;
        boolean lastFlagsDisplayedStateChanged = false;
        if (startTime != null) {
            FlagPoleState activeFlagState = state.getRacingProcedure().getActiveFlags(startTime, now);
            List<FlagPole> activeFlags = activeFlagState.getCurrentState();
            FlagPoleState previousFlagState = activeFlagState.getPreviousState(state.getRacingProcedure(), startTime);
            List<FlagPole> previousFlags = previousFlagState.getCurrentState();
            FlagPole mostInterestingFlagPole = FlagPoleState.getMostInterestingFlagPole(previousFlags, activeFlags);
            // TODO: adapt the LastFlagFinder#getMostRecent method!
            if (mostInterestingFlagPole != null) {
                lastUpperFlag = mostInterestingFlagPole.getUpperFlag();
                lastLowerFlag = mostInterestingFlagPole.getLowerFlag();
                lastFlagsAreDisplayed = mostInterestingFlagPole.isDisplayed();
                lastFlagsDisplayedStateChanged = previousFlagState.hasPoleChanged(mostInterestingFlagPole);
            }
        }
        final RaceLogRaceStatus status = state.getStatus();
        if (status == RaceLogRaceStatus.FINISHED) {
            TimeRange protestTime = state.getProtestTime();
            if (protestTime != null) {
                lastUpperFlag = Flags.BRAVO;
                lastLowerFlag = Flags.NONE;
                lastFlagsAreDisplayed = true;
                lastFlagsDisplayedStateChanged = true;
            }
        } else if (state.getStatus().isAbortingFlagFromPreviousPassValid()) {
            // search for race aborting in last pass
            AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
            RaceLogFlagEvent abortingFlagEvent = abortingFlagFinder.analyze();
            if (abortingFlagEvent != null) {
                lastUpperFlag = abortingFlagEvent.getUpperFlag();
                lastLowerFlag = abortingFlagEvent.getLowerFlag();
                lastFlagsAreDisplayed = abortingFlagEvent.isDisplayed();
                lastFlagsDisplayedStateChanged = true;
            }
        }
        final FlagStateDTO result;
        if (lastUpperFlag != null || lastLowerFlag != null) {
            result = new FlagStateDTO(lastUpperFlag, lastLowerFlag, lastFlagsAreDisplayed, lastFlagsDisplayedStateChanged);
        } else {
            result = null;
        }
        return result;
    }
    
    private String getCourseAreaOrNull() {
        return HomeServiceUtil.getCourseAreaNameForRegattaIfThereIsMoreThanOne(event, leaderboard);
    }

    private RaceProgressDTO getProgressOrNull() {
        RaceProgressDTO raceProgress = null;
        if (raceDefinition != null && raceDefinition.getCourse() != null && trackedRace != null) {
            int totalLegsCount = raceDefinition.getCourse().getLegs().size();
            int currentLegNo = trackedRace.getLastLegStarted(MillisecondsTimePoint.now());
            if (currentLegNo > 0 && totalLegsCount > 0) {
                raceProgress = new RaceProgressDTO(currentLegNo, totalLegsCount);
            }
        }
        return raceProgress;
    }

    private String getCourseNameOrNull() {
        if(state == null) {
            return null;
        }
        CourseBase lastCourse = state.getCourseDesign();
        if (lastCourse != null) {
            return lastCourse.getName();
        }
        return null;
    }

    public Date getStartTimeAsDate() {
        TimePoint startTime = getStartTime();
        return startTime == null ? null : startTime.asDate();
    }

    public TimePoint getStartTime() {
        if (!startTimeCalculated) {
            if (state != null) {
                startTime = state.getStartTime();
            }
            if (startTime == null && trackedRace != null) {
                startTime = trackedRace.getStartOfRace();
            }
            startTimeCalculated = true;
        }
        return startTime;
    }

    private TimePoint getFinishTime() {
        if (!finishTimeCalculated) {
            if (state != null) {
                finishTime = state.getFinishedTime();
            }
            if (finishTime == null && trackedRace != null) {
                finishTime = trackedRace.getEndOfRace();
            }
            finishTimeCalculated = true;
        }
        return finishTime;
    }

    /**
     * Prepares a {@link LiveRaceDTO} to show in the "Live" section, if this race is live or of public interest. 
     * 
     * @return the {@link LiveRaceDTO} if this race is live or of public interest, <code>null</code> otherwise
     */
    public LiveRaceDTO getLiveRaceOrNull() {
        // a race is of 'public interest' of a race is a combination of it's 'live' state
        // and special flags states indicating how the postponed/canceled races will be continued
        if (isLiveOrOfPublicInterest()) {
            // the start time is always given for live races
            LiveRaceDTO liveRaceDTO = new LiveRaceDTO(getLeaderboardName(), getRaceIdentifierOrNull(), getRaceName());
            fillRaceMetadata(liveRaceDTO);
            liveRaceDTO.setFlagState(getFlagStateOrNull());
            liveRaceDTO.setProgress(getProgressOrNull());
            liveRaceDTO.setWind(getWindOrNull());
            if(getRaceIdentifierOrNull() != null) {
                SecurityDTOUtil.addSecurityInformation(service.getSecurityService(), liveRaceDTO);
            }
            return liveRaceDTO;
        }
        return null;
    }

    /**
     * Prepares a {@link RaceListRaceDTO} to show in the "Finished Races" section, if this race is finished. 
     * 
     * @return the {@link RaceListRaceDTO} if this race is finished, <code>null</code> otherwise
     */
    public RaceListRaceDTO getFinishedRaceOrNull() {
        // a race is of 'public interest' of a race is a combination of it's 'live' state
        // and special flags states indicating how the postponed/canceled races will be continued
        // See: https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5029
        if (getLiveRaceViewState() == RaceViewState.FINISHED && !isLiveOrOfPublicInterest()) {
            // the start time is always given for live races
            RaceListRaceDTO raceListRaceDTO = new RaceListRaceDTO(getLeaderboardName(), 
                    getRaceIdentifierOrNull(), getRaceName());
            fillRaceMetadata(raceListRaceDTO);
            raceListRaceDTO.setDuration(getDurationOrNull());
            raceListRaceDTO.setWinner(getWinnerOrNull());
            raceListRaceDTO.setWindSourcesCount(getWindSourceCount());
            raceListRaceDTO.setVideoCount(getVideoCount());
            raceListRaceDTO.setAudioCount(getAudioCount());
            final WindSummary windSummary = new RaceWindCalculator().getWindSummary(trackedRace, raceLog);
            final WindStatisticsDTO windStatsDTO;
            if (windSummary != null) {
                windStatsDTO = new WindStatisticsDTO(windSummary.getTrueWindDirection().getDegrees(),
                        windSummary.getTrueLowerboundWind().getKnots(), windSummary.getTrueUpperboundWind().getKnots());
            } else {
                windStatsDTO = null;
            }
            raceListRaceDTO.setWind(windStatsDTO);
            if(getRaceIdentifierOrNull() != null) {
                SecurityDTOUtil.addSecurityInformation(service.getSecurityService(), raceListRaceDTO);
            }
            return raceListRaceDTO;
        }
        return null;
    }
    
    public SimpleRaceMetadataDTO getRaceCompetitionFormat() {
        SimpleRaceMetadataDTO raceDTO = new SimpleRaceMetadataDTO(getLeaderboardName(), getRaceIdentifierOrNull(), getRaceName());
        fillSimpleRaceMetadata(raceDTO);
        if(getRaceIdentifierOrNull() != null) {
            SecurityDTOUtil.addSecurityInformation(service.getSecurityService(), raceDTO);
        }
        return raceDTO;
    }

    private int getAudioCount() {
        return getMediaCount(MediaType.audio);
    }

    private int getVideoCount() {
        return getMediaCount(MediaType.video);
    }

    private int getMediaCount(MediaType mediaType) {
        int mediaCount = 0;
        if (trackedRace != null) {
            for (MediaTrack mediaTrack : service.getMediaTracksForRace(trackedRace.getRaceIdentifier())) {
                if (mediaTrack.mimeType != null && mediaTrack.mimeType.mediaType == mediaType) {
                    mediaCount++;
                }
            }
        }
        return mediaCount;
    }

    private int getWindSourceCount() {
        if (trackedRace != null) {
            return Util.size(trackedRace.getWindSources(WindSourceType.EXPEDITION));
        }
        return 0;
    }

    TimePoint getLiveTimePoint() {
        final TimePoint liveTimePoint;
        if (trackedRace != null) {
            liveTimePoint = MillisecondsTimePoint.now().minus(
                    trackedRace.getDelayToLiveInMillis());
        } else {
            final Long liveDelay = leaderboard.getDelayToLiveInMillis();
            long liveTimePointInMillis = System.currentTimeMillis() - (liveDelay == null ? 0l : liveDelay);
            liveTimePoint = new MillisecondsTimePoint(liveTimePointInMillis);
        }
        return liveTimePoint;
    }
    
    private SimpleCompetitorDTO getWinnerOrNull() {
        if (getLiveRaceViewState() != RaceViewState.FINISHED) {
            // We can't reliably calculate the winner for non finished races
            return null;
        }
        // TODO do not calculate the winner if the blue flag is currently shown.
        try {
            TimePoint finishTime = getLiveTimePoint();
            Iterable<Competitor> competitors = leaderboard.getCompetitorsFromBestToWorst(raceColumn, finishTime);
            if (competitors == null || Util.isEmpty(competitors)) {
                return null;
            }
            if (Util.size(raceColumn.getFleets()) == 1) {
                return new SimpleCompetitorDTO(competitors.iterator().next());
            }
            for (Competitor competitor : competitors) {
                if (isCompetitorInFleet(competitor)) {
                    return new SimpleCompetitorDTO(competitor);
                }
            }
        } catch (NoWindException e) {
            logger.log(Level.WARNING, "Error while calculating winner for race.", e);
        }
        return null;
    }

    private Duration getDurationOrNull() {
        TimePoint startTime = getStartTime();
        TimePoint finishTime = getFinishTime();
        if (startTime != null && finishTime != null) {
            return startTime.until(finishTime);
        }
        return null;
    }
    
    private void fillSimpleRaceMetadata(SimpleRaceMetadataDTO dto) {
        final Iterable<String> leaderboardGroupNames = leaderboardContext.getLeaderboardGroupNames();
        dto.setLeaderboardGroupName(leaderboardGroupNames == null || Util.isEmpty(leaderboardGroupNames) ? null :
            leaderboardGroupNames.iterator().next());
        final Iterable<UUID> leaderboardGroupIds = leaderboardContext.getLeaderboardGroupIds();
        dto.setLeaderboardGroupId(leaderboardGroupIds == null || Util.isEmpty(leaderboardGroupIds) ? null :
            leaderboardGroupIds.iterator().next());
        dto.setStart(getStartTimeAsDate());
        dto.setViewState(getLiveRaceViewState());
        dto.setTrackingState(getRaceTrackingState());
        dto.setCompetitors(getCompetitors());
    }

    private void fillRaceMetadata(RaceMetadataDTO<?> dto) {
        fillSimpleRaceMetadata(dto);
        dto.setRegattaDisplayName(getRegattaDisplayName());
        dto.setFleet(getFleetMetadataOrNull());
        dto.setBoatClass(HomeServiceUtil.getBoatClassName(leaderboard));
        dto.setCourseArea(getCourseAreaOrNull());
        dto.setCourse(getCourseNameOrNull());
    }

    public boolean isLiveOrOfPublicInterest() {
        if (HomeServiceUtil.calculateEventState(event) == EventState.FINISHED) {
            return false;
        }
        boolean isLive = false;
        boolean isOfPublicInterest = false;
        if (trackedRace != null) {
            isLive = trackedRace.isLive(now);
        }
        if (!isLive) {
            TimePoint startTime = getStartTime();
            if (startTime != null) {
                TimePoint finishTime = getFinishTime();
                // no data from tracking but maybe a manual setting of the start and finish time
                TimePoint startOfLivePeriod = startTime.minus(TIME_BEFORE_START_TO_SHOW_RACES_AS_LIVE);
                TimePoint endOfLivePeriod = finishTime != null ? finishTime
                        .plus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS) : null;
                if (now.after(startOfLivePeriod) && (endOfLivePeriod == null || now.before(endOfLivePeriod))) {
                    isOfPublicInterest = true;
                }
            } else if (raceLog != null) {
                // in case there is no start time set it could be an postponed or abandoned race
                RaceLogFlagEvent abortingFlagEvent = checkForAbortFlagEvent();
                if (abortingFlagEvent != null) {
                    TimePoint abortingTimeInPassBefore = abortingFlagEvent.getLogicalTimePoint();
                    if (abortingTimeInPassBefore.until(now).compareTo(TIME_TO_SHOW_CANCELED_RACES_AS_LIVE) < 0) {
                        isOfPublicInterest = true;
                        // TODO: Problem: This causes the race added to the live races list without having a start time!!!
                        // This does not work right now -> consider using a start time of the last pass.
                    }
                }
            }
        }
        return isLive || isOfPublicInterest;
    }

    /**
     * Calculates the races tracking state, which is {@link RaceTrackingState#TRACKED_VALID_DATA tracked with GPS data
     * available}, {@link RaceTrackingState#TRACKED_NO_VALID_DATA tracked with no GPS data available} or
     * {@link RaceTrackingState#NOT_TRACKED not tracked at all}.
     * 
     * @return the {@link RaceTrackingState}
     */
    public RaceTrackingState getRaceTrackingState() {
        RaceTrackingState trackingState = RaceTrackingState.NOT_TRACKED;
        if (trackedRace != null) {
            trackingState = RaceTrackingState.TRACKED_NO_VALID_DATA;
            if (trackedRace.hasGPSData()) {
                trackingState = RaceTrackingState.TRACKED_VALID_DATA;
            }
        }
        return trackingState;
    }
    
    /**
     * Get the races calculated {@link RaceViewState}, which depends on start/finish time, possible
     * {@link RaceLogFlagEvent}s or {@link ScoreCorrection}s.
     * 
     * @return the calculated {@link RaceViewState}
     */
    public RaceViewState getLiveRaceViewState() {
        if (raceViewState == null) {
            raceViewState = calculateRaceViewState();
        }
        return raceViewState;
    }

    private RaceViewState calculateRaceViewState() {
        TimePoint startTime = getStartTime();
        TimePoint finishTime = getFinishTime();
        if (startTime != null && now.before(startTime)) {
            return RaceViewState.SCHEDULED;
        }
        if (state != null && state.getStatus() == RaceLogRaceStatus.FINISHING) {
            // someone pulled up the blue flag; it's pretty likely that we'll also see the blue flag down
            // event for the transition into the FINISHED state, so we can report FINISHING for now:
            return RaceViewState.FINISHING;
        }
        if (finishTime != null && now.after(finishTime)) {
            return RaceViewState.FINISHED;
        }
        if (raceLog != null) {
            RaceLogFlagEvent abortingFlagEvent = checkForAbortFlagEvent();
            if (abortingFlagEvent != null) {
                Flags upperFlag = abortingFlagEvent.getUpperFlag();
                if (upperFlag.equals(Flags.AP)) {
                    return RaceViewState.POSTPONED;
                }
                if (upperFlag.equals(Flags.NOVEMBER)) {
                    return RaceViewState.ABANDONED;
                }
                if (upperFlag.equals(Flags.FIRSTSUBSTITUTE)) {
                    return RaceViewState.ABANDONED;
                }
            }
        }
        ScoreCorrection scoreCorrection = leaderboard.getScoreCorrection();
        // bug6168: for split fleets we need to check the fleet too
        if (trackedRace == null && scoreCorrection != null && scoreCorrection.hasCorrectionForNonTrackedFleet(raceColumn, fleet)) {
            return RaceViewState.FINISHED;
        }
        if (startTime != null) {
            return RaceViewState.RUNNING;
        }
        return RaceViewState.PLANNED;
    }

    private RaceLogFlagEvent checkForAbortFlagEvent() {
        RaceLogFlagEvent result = null;
        if (raceLog != null) {
            AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
            RaceLogFlagEvent abortingFlagEvent = abortingFlagFinder.analyze();
            if (abortingFlagEvent != null) {
                RaceLogRaceStatus lastStatus = state.getStatus();
                if (lastStatus.isAbortingFlagFromPreviousPassValid()) {
                    result = abortingFlagEvent;
                }
            }
        }
        return result;
    }

    public String getStageText() {
        // TODO fleet
        return getRegattaDisplayName() + " - " + raceColumn.getName();
    }

    public RegattaAndRaceIdentifier getRaceIdentifierOrNull() {
        return trackedRace == null ? null : trackedRace.getRaceIdentifier();
    }

    public String getSeriesName() {
        if (raceColumn instanceof RaceColumnInSeries) {
            return ((RaceColumnInSeries) raceColumn).getSeries().getName();
        }
        return "";
    }

    public String getRaceName() {
        return raceColumn.getName();
    }

    public String getFleetName() {
        return fleet.getName();
    }

    /**
     * Determine if this race is already finished or not.
     * 
     * @return <code>true</code> if this races {@link #getLiveRaceViewState() view state} is
     *         {@link RaceViewState#FINISHED}, false otherwise
     */
    public boolean isFinished() {
        return getLiveRaceViewState() == RaceViewState.FINISHED;
    }
    
    /**
     * Determine if this race is currently running or not.
     * 
     * @return <code>true</code> if this races {@link #getLiveRaceViewState() view state} is
     *         {@link RaceViewState#RUNNING}, false otherwise
     */
    public boolean isLive() {
        return getLiveRaceViewState() == RaceViewState.RUNNING || getLiveRaceViewState() == RaceViewState.FINISHING;
    }
    
    public Collection<SimpleCompetitorDTO> getCompetitors() {
        Set<SimpleCompetitorDTO> compotitorDTOs = new HashSet<>();
        if (leaderboardContext.hasMultipleFleets()) {
            boolean isFleetRacing = Util.size(raceColumn.getFleets()) > 1;
            for (Competitor competitor : leaderboard.getCompetitors()) {
                if (!isFleetRacing || isCompetitorInFleet(competitor)) {
                    compotitorDTOs.add(new SimpleCompetitorDTO(competitor));
                } 
            }
        }
        return compotitorDTOs;
    }
    
    private boolean isCompetitorInFleet(Competitor competitor) {
        Fleet fleetOfCompetitor = raceColumn.getFleetOfCompetitor(competitor);
        return fleetOfCompetitor != null && Util.equalsWithNull(fleet.getName(), fleetOfCompetitor.getName());
    }
    
    /**
     * Gets the {@link RaceDataInfo} for this race.
     * 
     * @return the {@link RaceDataInfo} instance
     */
    public RaceDataInfo getRaceDataInfo() {
        boolean hasGPSData = getRaceTrackingState() == RaceTrackingState.TRACKED_VALID_DATA; 
        boolean hasWindData = getWindSourceCount() > 0, hasVideo = getVideoCount() > 0, hasAudioData = getAudioCount() > 0;
        return new RaceDataInfo(hasGPSData, hasWindData, hasVideo, hasAudioData);
    }
}
