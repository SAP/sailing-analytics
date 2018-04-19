package com.sap.sailing.gwt.autoplay.client.nodes.base;

import java.util.UUID;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayFailureEvent;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.autoplay.client.events.FailureEvent;
import com.sap.sailing.gwt.autoplay.client.utils.AutoplayHelper;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sse.common.Util.Pair;

public abstract class RootNodeBase extends BaseCompositeNode {
    protected static final long LIVE_SWITCH_DELAY = 1000;
    private int UPDATE_STATE_TIMER = 5000;
    private final AutoPlayClientFactory cf;
    private String leaderBoardName;
    private int errorCount = 0;;
    private RootNodeState currentState;
    boolean firstTimeEventLoaded = true;

    private Timer checkTimer = new Timer() {
        @Override
        public void run() {
            doCheck();
        }
    };

    protected RootNodeBase(String name, AutoPlayClientFactory cf) {
        super(name);
        this.cf = cf;
    }

    @Override
    public final void onStart() {
        if (cf.getAutoPlayCtx() == null || //
                cf.getAutoPlayCtx().getContextDefinition() == null//
        ) {
            backToConfig();
            return;
        }
        getBus().addHandler(AutoPlayFailureEvent.TYPE, new AutoPlayFailureEvent.Handler() {
            @Override
            public void onFailure(AutoPlayFailureEvent e) {
                processFailure(e);
            }
        });
        doCheck();
    }

    @Override
    public final void onStop() {
        checkTimer.cancel();
    }

    private void doCheck() {
        // start next update, to ensure it is done no matter any error cases
        checkTimer.schedule(UPDATE_STATE_TIMER);
        final UUID eventUUID = cf.getAutoPlayCtx().getContextDefinition().getEventId();
        cf.getSailingService().getEventById(eventUUID, true, new AsyncCallback<EventDTO>() {

            @Override
            public void onSuccess(final EventDTO event) {
                if (firstTimeEventLoaded) {
                    AutoPlayHeaderEvent hE = new AutoPlayHeaderEvent(event.getName(), "");
                    if(event.getLogoImage()!=null){
                        hE.setHeaderLogoUrl(event.getLogoImage().getSourceRef());
                    }
                    cf.getEventBus().fireEvent(hE);
                    firstTimeEventLoaded = false;
                }
                cf.getAutoPlayCtx().updateEvent(event);
                if (event.isFinished() || event.isRunning()) {
                    _doCheck();
                } else {
                    setCurrentState(false, null, RootNodeState.PRE_EVENT, currentState);
                    // faster update if event not yet started!
                    checkTimer.schedule(1000);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                getBus().fireEvent(new AutoPlayFailureEvent(caught, "Error loading Event with id " + eventUUID));
            }
        });
    }

    private void _doCheck() {
        final RegattaAndRaceIdentifier currentPreLiveRace = cf.getAutoPlayCtx().getPreLiveRace();
        final RegattaAndRaceIdentifier currentLiveRace = cf.getAutoPlayCtx().getLiveRace();

        this.leaderBoardName = cf.getAutoPlayCtx().getContextDefinition().getLeaderboardName();
        AutoplayHelper.getLiveRace(cf.getSailingService(), cf.getErrorReporter(), cf.getAutoPlayCtx().getEvent(),
                leaderBoardName, cf.getDispatch(), getWaitTimeAfterRaceEndInMillis(), getSwitchBeforeRaceStartInMillies(), new AsyncCallback<Pair<Long, RegattaAndRaceIdentifier>>() {
                    @Override
                    public void onSuccess(Pair<Long, RegattaAndRaceIdentifier> result) {
                        errorCount = 0;

                        // we have no race, or we have one, and had a different one in the past
                        if (result == null || result.getB() == null) {

                            boolean comingFromLiveRace = currentLiveRace != null || currentPreLiveRace != null;

                            log("No live race found, " + (!comingFromLiveRace ? "not " : "") + "coming from live race");

                            setCurrentState(false, null,
                                    comingFromLiveRace ? RootNodeState.AFTER_LIVE : RootNodeState.IDLE, currentState);

                        } else {

                            final Long timeToRaceStartInMs = result.getA();
                            final RegattaAndRaceIdentifier loadedLiveRace = result.getB();

                            boolean isPreLiveRace = timeToRaceStartInMs > LIVE_SWITCH_DELAY;
                            // exit
                            if (currentLiveRace != null && !loadedLiveRace.equals(currentLiveRace)) {
                                log("Received different live race, hard switching to AFTER_LIVE race");
                                setCurrentState(isPreLiveRace, loadedLiveRace, RootNodeState.AFTER_LIVE, currentState);
                            } else {

                                log("New " + (isPreLiveRace ? "live " : "pre live") + " race found: " + loadedLiveRace
                                        + " starting in " + (timeToRaceStartInMs / 1000) + "s");

                                setCurrentState(isPreLiveRace, loadedLiveRace,
                                        isPreLiveRace ? RootNodeState.PRE_RACE : RootNodeState.LIVE, currentState);

                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorCount++;
                        if (errorCount > 5) {
                            cf.getEventBus().fireEvent(new AutoPlayFailureEvent(caught));
                        }
                    }
                });
    }

    protected abstract long getSwitchBeforeRaceStartInMillies();

    protected abstract long getWaitTimeAfterRaceEndInMillis();

    private final void setCurrentState(boolean isPreLiveRace, RegattaAndRaceIdentifier liveRace, RootNodeState goingTo,
            RootNodeState comingFrom) {
        if (goingTo != comingFrom) {
            log("RootNodeBase transition " + comingFrom + " -> " + goingTo);
            RegattaAndRaceIdentifier candidatePreLiveRace = isPreLiveRace ? liveRace : null;
            RegattaAndRaceIdentifier candidateLiveRace = isPreLiveRace ? null : liveRace;
            boolean veto = processStateTransition(candidatePreLiveRace, candidateLiveRace, goingTo, comingFrom);
            if (veto) {
                log("Vetoed switching to state " + goingTo + " coming from " + comingFrom);
            } else {
                // only update the state if it was not vetoed, to ensure that a futurs change comingFrom is clean
                log("Switching to state " + goingTo + " coming from " + comingFrom);
                this.currentState = goingTo;
            }
        } else {
            log("Transition to same autoplay state, skipping");
        }
    }

    public AutoPlayClientFactory getClientFactory() {
        return cf;
    }

    protected void backToConfig() {
        cf.getPlaceController().goTo(cf.getDefaultPlace());
    }

    protected abstract boolean processStateTransition(RegattaAndRaceIdentifier currentLiveRace,
            RegattaAndRaceIdentifier currentPreLiveRace, RootNodeState goingTo, RootNodeState comingFrom);

    protected abstract void processFailure(FailureEvent event);
}
