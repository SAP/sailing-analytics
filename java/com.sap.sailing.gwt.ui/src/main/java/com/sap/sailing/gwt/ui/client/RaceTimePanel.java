package com.sap.sailing.gwt.ui.client;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.common.RaceTimesCalculationUtil;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.security.SecuredDomainType.TrackedRaceActions;
import com.sap.sailing.gwt.ui.shared.MarkPassingTimesDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceWithCompetitorsAndBoatsDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;

/**
 * A specific {@link TimePanel} that shows and manages a time slider for a race. Receives {@link RaceTimesInfoDTO}
 * objects when registered as a {@link RaceTimesInfoProviderListener} on a {@link RaceTimesInfoProvider} and adjusts the
 * time panel including its time slider and the marks displayed and the zoom model accordingly.
 * <p>
 * 
 * It is possible to register as a "transitive" {@link RaceTimesInfoProviderListener} on this object in case a component
 * is interested in being notified when this object is done with its adjustments based on a
 * {@link RaceTimesInfoProviderListener} notification.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RaceTimePanel extends TimePanel<RaceTimePanelSettings> implements RaceTimesInfoProviderListener {
    private final RaceTimesInfoProvider raceTimesInfoProvider;
    
    private final RegattaAndRaceIdentifier selectedRace;
    private boolean autoAdjustPlayMode;
    private RaceTimesInfoDTO lastRaceTimesInfo;
    private boolean hasCanReplayDuringLiveRacesPermission = false;
    private final UserService userService;
    private final RaceTimePanelLifecycle componentLifecycle;
    
    /**
     * These listeners are notified transitively after this object has been
     * {@link RaceTimesInfoProviderListener#raceTimesInfosReceived(Map, long, Date, long) notified} about race timing
     * changes and has finished reacting accordingly. This way, e.g., a race board mode implementation can do its
     * thing while assuming that the time slider has already been adjusted accordingly, therefore allowing for the
     * timer to be set to a reasonable value.
     */
    private final Set<RaceTimesInfoProviderListener> listeners;

    private final RaceWithCompetitorsAndBoatsDTO raceDTO;

    private final UserStatusEventHandler userStatusEventHandler = new UserStatusEventHandler() {
        @Override
        public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
            RaceTimePanel.this.hasCanReplayDuringLiveRacesPermission = userService.hasPermission(raceDTO,
                    TrackedRaceActions.CAN_REPLAY_DURING_LIVE_RACES);
        }
    };

    /**
     * When a marker update is required but min/max are not yet initialized, this flag is set. If later
     * {@link #setMinMax(Date, Date, boolean)} is invoked and this flag is set, a {@link #redrawAllMarkers(RaceTimesInfoDTO)}
     * will be issued and the flag is cleared.
     */
    private boolean redrawAllMarkersPendingForMinMaxBeingInitialized;
    
    private final Duration initialTimeAfterRaceStartInReplayMode;
    
    public RaceTimePanel(Component<?> parent, ComponentContext<?> context, RaceTimePanelLifecycle componentLifecycle,
            UserService userService,
            Timer timer, TimeRangeWithZoomProvider timeRangeProvider, StringMessages stringMessages,
            RaceTimesInfoProvider raceTimesInfoProvider, boolean canReplayWhileLiveIsPossible, boolean forcePaddingRightToAlignToCharts,
            RegattaAndRaceIdentifier selectedRaceIdentifier, Duration initialTimeAfterRaceStartInReplayMode,
            final RaceWithCompetitorsAndBoatsDTO raceDTO) {
        super(parent, context, timer, timeRangeProvider, stringMessages, canReplayWhileLiveIsPossible,
                forcePaddingRightToAlignToCharts, userService, raceDTO);
        this.raceDTO = raceDTO;
        this.componentLifecycle = componentLifecycle;
        this.userService = userService;
        this.raceTimesInfoProvider = raceTimesInfoProvider;
        this.listeners = new HashSet<>();
        autoAdjustPlayMode = true;
        selectedRace = selectedRaceIdentifier;
        this.initialTimeAfterRaceStartInReplayMode = initialTimeAfterRaceStartInReplayMode;
        if (!raceTimesInfoProvider.containsRaceIdentifier(selectedRace)) {
            raceTimesInfoProvider.addRaceIdentifier(selectedRace, true);
        }
    }
    
    @Override
    protected void onLoad() {
        super.onLoad();
        userService.addUserStatusEventHandler(userStatusEventHandler, true);
    }
    
    @Override
    protected void onUnload() {
        super.onUnload();
        userService.removeUserStatusEventHandler(userStatusEventHandler);
    }
    
    public void addRaceTimesInfoProviderListener(RaceTimesInfoProviderListener listener) {
        this.listeners.add(listener);
    }
    
    public void removeRaceTimesInfoProviderListener(RaceTimesInfoProviderListener listener) {
        this.listeners.remove(listener);
    }
    
    @Override
    protected boolean canReplayWhileLiveIsPossible() {
        return this.hasCanReplayDuringLiveRacesPermission;
    }
    
    /**
     * If the race's start time is already known and the <code>time</code> is before the start we can set the count-down label
     */
    @Override
    protected String getTimeToStartLabelText(Date time) {
        String result = null;
        RaceTimesInfoDTO selectedRaceTimes = raceTimesInfoProvider.getRaceTimesInfo(selectedRace);
        if (selectedRaceTimes != null && selectedRaceTimes.startOfRace != null) {
            if (time.before(selectedRaceTimes.startOfRace) || time.equals(selectedRaceTimes.startOfRace)) {
                long timeToStartInMs = selectedRaceTimes.startOfRace.getTime() - time.getTime();
                result = timeToStartInMs < 1000 ? stringMessages.start() : stringMessages.timeToStart(DateAndTimeFormatterUtil.formatElapsedTime(timeToStartInMs));
            } else {
                long timeSinceStartInMs = time.getTime() - selectedRaceTimes.startOfRace.getTime();
                result = stringMessages.timeSinceStart(DateAndTimeFormatterUtil.formatElapsedTime(timeSinceStartInMs));
            } 
        }
        return result;
    }

    @Override
    public void updateSettings(RaceTimePanelSettings newSettings) {
        super.updateSettings(newSettings);
        raceTimesInfoProvider.setRequestInterval(newSettings.getRefreshInterval());
    }

    @Override
    public RaceTimePanelSettings getSettings() {
        return new RaceTimePanelSettings(timer.getRefreshInterval());
    }

    @Override
    public SettingsDialogComponent<RaceTimePanelSettings> getSettingsDialogComponent(RaceTimePanelSettings settings) {
        return componentLifecycle.getSettingsDialogComponent(settings);
    }
    
    public RaceTimesInfoDTO getLastRaceTimesInfo() {
        return lastRaceTimesInfo;
    }

    private void updateTimeInfo(RaceTimesInfoDTO raceTimesInfo) {
        if (raceTimesInfo == null) { 
            // in case the race is not tracked anymore we reset the time slider
            resetTimeSlider();
        } else {
            // timer will only accept this update if the delay hasn't been updated explicitly
            timer.setLivePlayDelayInMillis(raceTimesInfo.delayToLiveInMs);
            if ((raceTimesInfo.startOfTracking != null || raceTimesInfo.startOfRace != null) && 
                    (raceTimesInfo.newestTrackingEvent != null || raceTimesInfo.endOfRace != null || raceTimesInfo.endOfTracking != null)) {
                // we set here the min and max of the time slider, the start and end of the race as well as the known
                // leg markers
                boolean liveModeToBeMadePossible = isLiveModeToBeMadePossible();
                setLiveGenerallyPossible(liveModeToBeMadePossible);
                setJumpToLiveEnablement(liveModeToBeMadePossible && timer.getPlayMode() != PlayModes.Live);
                if (autoAdjustPlayMode && liveModeToBeMadePossible) {
                    timer.setPlayMode(PlayModes.Live);
                }
                updatePlayPauseButtonsVisibility(timer.getPlayMode());
                if (liveModeToBeMadePossible && !canReplayWhileLiveIsPossible() &&
                        timer.getPlayMode()==PlayModes.Replay && timer.getPlayState()==PlayStates.Playing) {
                    // pause timer because it must not be playing in replay mode while the race is live
                    timer.pause();
                }
                
                boolean timerAlreadyInitialized = getFromTime() != null && getToTime() != null && timeSlider.getCurrentValue() != null;
                if (!timeRangeProvider.isZoomed()) {
                    updateMinMax(raceTimesInfo);
                    if (!timerAlreadyInitialized) {
                        initTimerPosition(raceTimesInfo);
                    }
                    updateLegMarkers(raceTimesInfo);
                }
            } else {
                // the tracked race did not start yet or has no events yet
                // maybe show a special state for this like "Race did not start yet"
            }
        }
        lastRaceTimesInfo = raceTimesInfo; // TODO bug 3122: when not redrawn yet because min/max not initialized yet, further redraw is disabled by this.
    } 
    
    @Override
    public void onTimeZoomChanged(Date zoomStartTimepoint, Date zoomEndTimepoint) {
        super.onTimeZoomChanged(zoomStartTimepoint, zoomEndTimepoint);
        timeSlider.setZoomed(true);
        setMinMax(zoomStartTimepoint, zoomEndTimepoint, false);
        timeSlider.clearMarkersAndLabelsAndTicks();
        if (lastRaceTimesInfo != null) {
            redrawAllMarkers(lastRaceTimesInfo);
        }
    }

    @Override
    public void onTimeZoomReset() {
        super.onTimeZoomReset();
        timeSlider.setZoomed(false);
        timeSlider.setMinValue(Double.valueOf(timeRangeProvider.getFromTime().getTime()), false);
        timeSlider.setMaxValue(Double.valueOf(timeRangeProvider.getToTime().getTime()), false);
        timeSlider.setCurrentValue(Double.valueOf(timer.getTime().getTime()), true);
        timeSlider.clearMarkersAndLabelsAndTicks();
        if (lastRaceTimesInfo != null) {
            redrawAllMarkers(lastRaceTimesInfo);
        }
    }

    /**
     * If and only if the {@link #selectedRace}'s timing is described by the {@link #raceTimesInfoProvider} and
     * according to the timing the current live time point is after the start of tracking or at least after
     * {@link RaceTimesCalculationUtil.MIN_TIME_BEFORE_RACE_START} minutes before the race, and the current live time
     * point is not after the end of tracking then live mode will be made possible (<code>true</code> will be returned).<p>
     * 
     * Bug 3482: when startOfTracking and endOfTracking are both {@code null} it would be good to check for the
     * {@link RaceTimesInfoDTO#newestTrackingEvent} and {@link RaceTimesInfoDTO#endOfRace} values to see if---with
     * a little leeway---an end for the live period may be determined. Without this logic, such races would remain
     * live until the end of time.
     */
    @Override
    protected boolean isLiveModeToBeMadePossible() {
        RaceTimesInfoDTO lastRaceTimesInfo = raceTimesInfoProvider != null
                ? raceTimesInfoProvider.getRaceTimesInfo(selectedRace)
                : null;
        if (lastRaceTimesInfo == null) return false;

        Date timePoint = timer.getLiveTimePointAsDate();
        Pair<Date, Date> minMax = RaceTimesCalculationUtil.calculateRaceMinMax(timer.getLiveTimePointAsDate(),
                lastRaceTimesInfo.startOfTracking, lastRaceTimesInfo.startOfRace, lastRaceTimesInfo.raceFinishingTime,
                lastRaceTimesInfo.raceFinishedTime, lastRaceTimesInfo.endOfRace, lastRaceTimesInfo.endOfTracking);
        Date min = minMax.getA();
        Date max = minMax.getB();

        // Bug 3482: try to determine a value for max if max is null
        if (max == null && lastRaceTimesInfo.newestTrackingEvent != null) {
            max = new Date(lastRaceTimesInfo.newestTrackingEvent.getTime() + RaceTimesCalculationUtil.TIME_AFTER_LIVE);
        }

        // Assume we are live (no end known as of now)
        boolean beforeEnd = true;
        if (max != null) {
            beforeEnd = !timePoint.after(max);
        }

        if (min != null) {
            // min < timePoint < max; max might be unknown as of now
            return !timePoint.before(min) && beforeEnd;
        }
        return beforeEnd;
    }
    
    @Override
    public void playStateChanged(PlayStates playState, PlayModes playMode) {
        super.playStateChanged(playState, playMode);
        switch (playMode) {
        case Replay:
            autoAdjustPlayMode = false;
            break;
        case Live:
            break;
        }
    }

    /**
     * Obtains the min/max range for the slider bar from <code>newRaceTimesInfo</code> and adjusts the slider bar if
     * needed. Since we extend the slider bar to the right when playing across the "end of time," we don't want to
     * constrain it back again here. Therefore, the max value is never reduced here but at best initially set if it
     * was <code>null</code> before, or extended to a later point in time.
     */
    private void updateMinMax(RaceTimesInfoDTO newRaceTimesInfo) {
        Util.Pair<Date, Date> raceMinMax = RaceTimesCalculationUtil.calculateRaceMinMax(timer.getLiveTimePointAsDate(),
                newRaceTimesInfo.startOfTracking, newRaceTimesInfo.startOfRace, newRaceTimesInfo.raceFinishingTime,
                newRaceTimesInfo.raceFinishedTime, newRaceTimesInfo.endOfRace, newRaceTimesInfo.endOfTracking);
        
        Date min = raceMinMax.getA();
        Date max = raceMinMax.getB();
        
        // never reduce max if it was already set
        if (min != null && max != null &&
                (getToTime() == null || getToTime().before(max) ||
                 getFromTime() == null || getFromTime().after(min))) {
            setMinMax(min, max, /* fireEvent */ false); // no event because we guarantee time to be between min and max
        }
    }
    
    @Override
    public void setMinMax(Date min, Date max, boolean fireEvent) {
        super.setMinMax(min, max, fireEvent);
        if (redrawAllMarkersPendingForMinMaxBeingInitialized && timeSlider.isMinMaxInitialized()) {
            redrawAllMarkers(lastRaceTimesInfo);
            redrawAllMarkersPendingForMinMaxBeingInitialized = false;
        }
    }

    /**
     * When in {@link PlayModes#Replay} mode, tries to put the {@link #timer} to the time point when the last leg was
     * finished first. If this time point is not (yet?) known, tries to put the {@link #timer} to the
     * {@link RaceTimesInfoDTO#endOfRace end of the race}. If that happens to be undefined (<code>null</code>), the
     * {@link RaceTimesInfoDTO#startOfRace start of the race} is used instead. If that isn't available either, the timer
     * remains unchanged.
     * <p>
     * 
     * When in {@link PlayModes#Live} mode, tries to advance the timer to the time point of the
     * {@link RaceTimesInfoDTO#timePointOfNewestEvent timePointOfNewestEvent} received from the tracking infrastructure
     * so far and puts the timer into {@link PlayStates#Playing play mode}.
     */
    private void initTimerPosition(RaceTimesInfoDTO newRaceTimesInfo) {
        // initialize timer position
        switch (timer.getPlayMode()) {
        case Live:
            if (newRaceTimesInfo.newestTrackingEvent != null) {
                timer.setTime(newRaceTimesInfo.newestTrackingEvent.getTime());
            }
            timer.play();
            break;
        case Replay:
            // set time to start of race
            if (newRaceTimesInfo.startOfRace != null) {
                timer.setTime(newRaceTimesInfo.startOfRace.getTime() +
                        (initialTimeAfterRaceStartInReplayMode == null ? 0l : initialTimeAfterRaceStartInReplayMode.asMillis()));
            } else if (newRaceTimesInfo.startOfTracking != null) {
                timer.setTime(newRaceTimesInfo.startOfTracking.getTime() +
                        (initialTimeAfterRaceStartInReplayMode == null ? 0l : initialTimeAfterRaceStartInReplayMode.asMillis()));
            }
            break;
        }
    }

    /**
     * Expected to be called before {@link #lastRaceTimesInfo} has been updated to <code>newRaceTimesInfo</code>
     */
    private void updateLegMarkers(RaceTimesInfoDTO newRaceTimesInfo) {
        boolean requiresMarkerUpdate = true;
        // updating the sliderbar markers requires a lot of time, therefore we need to do this only if required
        if (lastRaceTimesInfo != null && lastRaceTimesInfo.markPassingTimes.size() == newRaceTimesInfo.markPassingTimes.size()) {
            requiresMarkerUpdate = false;
            int numberOfLegs = newRaceTimesInfo.markPassingTimes.size();
            for (int i = 0; i < numberOfLegs; i++) {
                if (!Util.equalsWithNull(newRaceTimesInfo.markPassingTimes.get(i).firstPassingDate, lastRaceTimesInfo.markPassingTimes.get(i).firstPassingDate)) {
                    requiresMarkerUpdate = true;
                    break;
                }
            }
            if (!requiresMarkerUpdate &&
                (!Util.equalsWithNull(lastRaceTimesInfo.startOfRace, newRaceTimesInfo.startOfRace) ||
                 !Util.equalsWithNull(lastRaceTimesInfo.endOfRace, newRaceTimesInfo.endOfRace))) {
                requiresMarkerUpdate = true;
            }
        }
        if (requiresMarkerUpdate) {
            if (timeSlider.isMinMaxInitialized()) { // TODO bug 3122 later, the lastRaceTimesInfo will be updated even if not redrawn because min/max not initialized
                redrawAllMarkers(newRaceTimesInfo);
            } else {
                redrawAllMarkersPendingForMinMaxBeingInitialized = true;
            }
        }
    }
    
    private void redrawAllMarkers(RaceTimesInfoDTO newRaceTimesInfo) {
        List<MarkPassingTimesDTO> markPassingTimes = newRaceTimesInfo.getMarkPassingTimes();
        timeSlider.clearMarkers();
        if (newRaceTimesInfo.startOfRace != null) {
            long markerTime = newRaceTimesInfo.startOfRace.getTime();
            if (!timeSlider.isZoomed() || (timeSlider.isZoomed() && markerTime > timeSlider.getMinValue() && markerTime < timeSlider.getMaxValue())) {
                timeSlider.addMarker("S", Double.valueOf(markerTime));
            }
        }
        int markPassingCounter = 1;
        for (MarkPassingTimesDTO markPassingTimesDTO: markPassingTimes) {
            // ignore the start mark passing
            if (markPassingCounter > 1 && markPassingTimesDTO.firstPassingDate != null) {
                long markerTime = markPassingTimesDTO.firstPassingDate.getTime();
                if (!timeSlider.isZoomed() || (timeSlider.isZoomed() && markerTime > timeSlider.getMinValue() && markerTime < timeSlider.getMaxValue())) {
                    timeSlider.addMarker(markPassingTimesDTO.getName(), Double.valueOf(markerTime));
                }
            }
            markPassingCounter++;
        }
        if (newRaceTimesInfo.endOfRace != null) {
            long markerTime = newRaceTimesInfo.endOfRace.getTime();
            // If there is a "blue flag down" event after endOfRace use it instead to show the E marker
            if (newRaceTimesInfo.getFinishedTime() != null) {
                markerTime = newRaceTimesInfo.getFinishedTime().getTime();
            }
            if (!timeSlider.isZoomed() || (timeSlider.isZoomed() && markerTime > timeSlider.getMinValue() && markerTime < timeSlider.getMaxValue())) {
                timeSlider.addMarker("E", Double.valueOf(markerTime));
            }
        }
        timeSlider.redraw(); 
    }
    
    @Override
    public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfos, long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest, long clientTimeWhenResponseWasReceived) {
        timer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
        updateTimeInfo(raceTimesInfos.get(selectedRace));
        for (final RaceTimesInfoProviderListener listener : listeners) {
            listener.raceTimesInfosReceived(raceTimesInfos, clientTimeWhenRequestWasSent, serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
        }
    }
    
    @Override
    public String getId() {
        return componentLifecycle.getComponentId();
    }
}
