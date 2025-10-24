package com.sap.sailing.gwt.home.server;

import com.sap.sailing.gwt.home.communication.race.SimpleRaceMetadataDTO.RaceViewState;
import com.sap.sailing.gwt.home.server.EventActionUtil.RaceCallback;

/**
 * {@link RaceCallback} implementation, which calculates flags representing to regatta state based on the
 * {@link RaceContext#getLiveRaceViewState() state} of the {@link RaceContext races} passed in the
 * {@link #doForRace(RaceContext)} method. The aggregated values can be accessed by their respective getter method.
 */
public class OverallRacesStateCalculator implements RaceCallback {
    private boolean hasLiveRace = false;
    private boolean hasFinishedRace = false;
    private boolean hasAbandonedOrPostponedRace = false;
    private boolean hasUnfinishedRace = false;

    @Override
    public void doForRace(RaceContext context) {
        RaceViewState raceViewState = context.getLiveRaceViewState();
        hasLiveRace |= raceViewState == RaceViewState.RUNNING;
        hasFinishedRace |= raceViewState == RaceViewState.FINISHED;
        hasAbandonedOrPostponedRace |= raceViewState == RaceViewState.POSTPONED || raceViewState == RaceViewState.ABANDONED;
        // To be considered unfinished, the enclosing regatta (if it exists) must have no end date or an end date in the future;
        // otherwise, the regatta containing the race is considered finished, and therefore all its races are considered finished,
        // too, even if the race was not even started. This solves bug 6001 and will display regattas in multi-class events as
        // finished if they, e.g., occur in the event's first "half" if this first half has finished already.
        hasUnfinishedRace |= (raceViewState != RaceViewState.FINISHED &&
                (context.getRegatta() == null || context.getRegatta().getEndDate() == null || context.getRegatta().getEndDate().after(context.getLiveTimePoint())));
    }

    /**
     * @return <code>true</code> if at least one currently running race was passed to the
     *         {@link #doForRace(RaceContext)} method, <code>false</code> otherwise
     */
    public boolean hasLiveRace() {
        return hasLiveRace;
    }
    
    /**
     * @return <code>true</code> if at least one finished race was passed to the {@link #doForRace(RaceContext)} method,
     *         <code>false</code> otherwise
     */
    public boolean hasFinishedRace() {
        return hasFinishedRace;
    }
    
    /**
     * @return <code>true</code> if at least one race was passed to the {@link #doForRace(RaceContext)} method which
     *         wasn't finished yet, <code>false</code> otherwise
     */
    public boolean hasUnfinishedRace() {
        return hasUnfinishedRace;
    }
    
    /**
     * @return <code>true</code> if at least one race was passed to the {@link #doForRace(RaceContext)} method which
     *         was abandoned or postponed, <code>false</code> otherwise
     */
    public boolean hasAbandonedOrPostponedRace() {
        return hasAbandonedOrPostponedRace;
    }
}
