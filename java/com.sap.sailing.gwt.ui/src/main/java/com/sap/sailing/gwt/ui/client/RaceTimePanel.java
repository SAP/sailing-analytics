package com.sap.sailing.gwt.ui.client;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.common.EventAndRaceIdentifier;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.gwt.ui.client.Timer.PlayModes;
import com.sap.sailing.gwt.ui.client.Timer.PlayStates;
import com.sap.sailing.gwt.ui.shared.MarkPassingTimesDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.components.SettingsDialogComponent;

public class RaceTimePanel extends TimePanel<RaceTimePanelSettings> implements RaceSelectionChangeListener, RaceTimesInfoProviderListener {
    private RaceTimesInfoProvider raceTimesInfoProvider;
    private RaceIdentifier selectedRace;
    private boolean autoAdjustPlayMode;
    private RaceTimesInfoDTO lastRaceTimesInfo;
    
    public RaceTimePanel(Timer timer, StringMessages stringMessages, RaceTimesInfoProvider raceTimesInfoProvider) {
        super(timer, stringMessages);
        this.raceTimesInfoProvider = raceTimesInfoProvider;
        selectedRace = null;
        autoAdjustPlayMode = true;
    }
    
    @Override
    public void updateSettings(RaceTimePanelSettings newSettings) {
        super.updateSettings(newSettings);
        raceTimesInfoProvider.setRequestInterval(newSettings.getRefreshInterval());
    }

    @Override
    public RaceTimePanelSettings getSettings() {
        RaceTimePanelSettings result = new RaceTimePanelSettings();
        result.setDelayToLivePlayInSeconds(timer.getLivePlayDelayInMillis()/1000);
        result.setRefreshInterval(timer.getRefreshInterval());
        result.setRaceTimesInfo(raceTimesInfoProvider.getRaceTimesInfo(selectedRace));
        return result;
    }

    @Override
    public SettingsDialogComponent<RaceTimePanelSettings> getSettingsDialogComponent() {
        return new RaceTimePanelSettingsDialogComponent(getSettings(), stringMessages);
    }

    private void updateTimeInfo(RaceTimesInfoDTO raceTimesInfo) {
        if (raceTimesInfo == null) { 
            // in case the race is not tracked anymore we reset the timer
            reset();
        } else { 
            if ((raceTimesInfo.startOfTracking != null || raceTimesInfo.startOfRace != null) && 
                    (raceTimesInfo.newestTrackingEvent != null || raceTimesInfo.endOfRace != null)) {
                // we set here the min and max of the time slider, the start and end of the race as well as the known
                // leg markers
                boolean liveModeToBeMadePossible = isLiveModeToBeMadePossible();
                setLiveGenerallyPossible(liveModeToBeMadePossible);
                setJumpToLiveEnablement(liveModeToBeMadePossible && timer.getPlayMode() != PlayModes.Live);
                if (autoAdjustPlayMode && liveModeToBeMadePossible) {
                    timer.setPlayMode(PlayModes.Live);
                }
                
                boolean timerAlreadyInitialized = getMin() != null && getMax() != null && sliderBar.getCurrentValue() != null;
                initMinMax(raceTimesInfo);
                if (!timerAlreadyInitialized) {
                    initTimerPosition(raceTimesInfo);
                }
                updateLegMarkers(raceTimesInfo);
            } else {
                // the tracked race did not start yet or has no events yet
                // maybe show a special state for this like "Race did not start yet"
            }
        }
    } 
    
    @Override
    protected boolean isLiveModeToBeMadePossible() {
        long livePlayDelayInMillis = timer.getLivePlayDelayInMillis();
        long eventTimeoutTolerance = 30 * 1000; // 30s 
        long liveTimePointInMillis = System.currentTimeMillis() - livePlayDelayInMillis;
        RaceTimesInfoDTO lastRaceTimesInfo = raceTimesInfoProvider != null ? raceTimesInfoProvider.getRaceTimesInfo(selectedRace) : null;
        return lastRaceTimesInfo != null &&
                lastRaceTimesInfo.newestTrackingEvent != null &&
                liveTimePointInMillis < lastRaceTimesInfo.newestTrackingEvent.getTime() + eventTimeoutTolerance &&
                lastRaceTimesInfo.startOfTracking != null &&
                liveTimePointInMillis > lastRaceTimesInfo.startOfTracking.getTime();
    }
    
    @Override
    public void onRaceSelectionChange(List<EventAndRaceIdentifier> selectedRaces) {
        if (selectedRaces != null && !selectedRaces.isEmpty()) {
            selectedRace = selectedRaces.iterator().next();
            if (!raceTimesInfoProvider.containsRaceIdentifier(selectedRace)) {
                raceTimesInfoProvider.addRaceIdentifier(selectedRace, true);
            }
        }
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
    private void initMinMax(RaceTimesInfoDTO newRaceTimesInfo) {
        Date min = null;
        Date max = null;

        switch (timer.getPlayMode()) {
        case Live:
            if (newRaceTimesInfo.startOfRace != null) {
                long extensionTime = calculateRaceExtensionTime(newRaceTimesInfo.startOfRace, newRaceTimesInfo.newestTrackingEvent);
                
                min = new Date(newRaceTimesInfo.startOfRace.getTime() - extensionTime);
            } else if (newRaceTimesInfo.startOfTracking != null) {
                min = newRaceTimesInfo.startOfTracking;
            }
            
            if (newRaceTimesInfo.newestTrackingEvent != null) {
                max = newRaceTimesInfo.newestTrackingEvent;
            }
            break;
        case Replay:
            long extensionTime = calculateRaceExtensionTime(newRaceTimesInfo.startOfRace, newRaceTimesInfo.endOfRace);
            
            if (newRaceTimesInfo.startOfRace != null) {
                min = new Date(newRaceTimesInfo.startOfRace.getTime() - extensionTime);
            } else if (newRaceTimesInfo.startOfTracking != null) {
                min = newRaceTimesInfo.startOfTracking;
            }
            
            if (newRaceTimesInfo.endOfRace != null) {
                max = new Date(newRaceTimesInfo.endOfRace.getTime() + extensionTime);
            } else if (newRaceTimesInfo.newestTrackingEvent != null) {
                max = newRaceTimesInfo.newestTrackingEvent;
            }
            break;
        }
        // never reduce max if it was already set
        if (min != null && max != null && (getMax() == null || getMax().before(max))) {
            setMinMax(min, max, /* fireEvent */ false); // no event because we guarantee time to be between min and max
        }
    }
    
    private long calculateRaceExtensionTime(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return 5 * 60 * 1000; //5 minutes
        }
        
        long minExtensionTime = 60 * 1000; // 1 minute
        long maxExtensionTime = 10 * 60 * 1000; // 10 minutes
        double extensionTimeFactor = 0.1; // 10 percent of the overall race length
        long extensionTime = (long) ((endTime.getTime() - startTime.getTime()) * extensionTimeFactor);
        
        return extensionTime < minExtensionTime ? minExtensionTime : extensionTime > maxExtensionTime ? maxExtensionTime : extensionTime;
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
    private boolean initTimerPosition(RaceTimesInfoDTO newRaceTimesInfo) {
        // initialize timer position
        switch (timer.getPlayMode()) {
        case Live:
            if (newRaceTimesInfo.newestTrackingEvent != null) {
                timer.setTime(newRaceTimesInfo.newestTrackingEvent.getTime());
            }
            timer.play();
            break;
        case Replay:
            // set time to end of race
            if (newRaceTimesInfo.getLastMarkPassingTimes() != null) {
                timer.setTime(newRaceTimesInfo.getLastMarkPassingTimes().firstPassingDate.getTime());
            } else  if (newRaceTimesInfo.endOfRace != null) {
                timer.setTime(newRaceTimesInfo.endOfRace.getTime());
            } else  if (newRaceTimesInfo.startOfRace != null) {
                timer.setTime(newRaceTimesInfo.startOfRace.getTime());
            }
            break;
        }
        return timer.getTime() != null;
    }

    private void updateLegMarkers(RaceTimesInfoDTO newRaceTimesInfo) {
        List<MarkPassingTimesDTO> markPassingTimes = newRaceTimesInfo.getMarkPassingTimes();
        boolean requiresMarkerUpdate = true;
        
        // updating the sliderbar markers requires a lot of time, therefore we need to do this only if required
        if(lastRaceTimesInfo != null  && lastRaceTimesInfo.markPassingTimes.size() == newRaceTimesInfo.markPassingTimes.size()) {
            requiresMarkerUpdate = false;
            int numberOfLegs = newRaceTimesInfo.markPassingTimes.size();
            for(int i = 0; i < numberOfLegs; i++) {
                if(newRaceTimesInfo.markPassingTimes.get(i).firstPassingDate.getTime() != lastRaceTimesInfo.markPassingTimes.get(i).firstPassingDate.getTime()) {
                    requiresMarkerUpdate = true;
                    break;
                }
            }
            if((lastRaceTimesInfo.endOfRace == null && newRaceTimesInfo.endOfRace != null) ||
               (lastRaceTimesInfo.endOfRace != null && newRaceTimesInfo.endOfRace == null) ||
               lastRaceTimesInfo.endOfRace != null && newRaceTimesInfo.endOfRace != null && lastRaceTimesInfo.endOfRace.getTime() != newRaceTimesInfo.endOfRace.getTime()) {
                requiresMarkerUpdate = true;
            }
        }
        if (requiresMarkerUpdate && sliderBar.isMinMaxInitialized()) {
            sliderBar.clearMarkers();
            for (MarkPassingTimesDTO markPassingTimesDTO: markPassingTimes) {
              sliderBar.addMarker(markPassingTimesDTO.name, new Double(markPassingTimesDTO.firstPassingDate.getTime()));
            }
            if(newRaceTimesInfo.endOfRace != null) {
                sliderBar.addMarker("E", new Double(newRaceTimesInfo.endOfRace.getTime()));
            }
                
            sliderBar.redraw(); 
        }
        lastRaceTimesInfo = newRaceTimesInfo;
    }
    
    @Override
    public void raceTimesInfosReceived(Map<RaceIdentifier, RaceTimesInfoDTO> raceTimesInfos) {
        updateTimeInfo(raceTimesInfos.get(selectedRace));
    }
}
