package com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.racemapwithleaderboard;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.sap.sailing.gwt.autoplay.client.events.FailureEvent;

/**
 * Sample custom event class for copy & paste
 */
public class LiveRaceWithRacemapAndLeaderBoardFailureEvent extends GwtEvent<LiveRaceWithRacemapAndLeaderBoardFailureEvent.Handler> implements FailureEvent {
    public static final Type<Handler> TYPE = new Type<Handler>();

    private Throwable caught;
    private String message;

    /**
     * Event handler interface
     */
    public interface Handler extends EventHandler {
        void onFailure(LiveRaceWithRacemapAndLeaderBoardFailureEvent e);
    }

    public LiveRaceWithRacemapAndLeaderBoardFailureEvent(Throwable caught) {
        this(caught, caught.getMessage());
    }

    public LiveRaceWithRacemapAndLeaderBoardFailureEvent(String message) {
        this(null, message);
    }

    public LiveRaceWithRacemapAndLeaderBoardFailureEvent(Throwable caught, String message) {
        this.caught = caught;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCaught() {
        return caught;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onFailure(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LiveRaceWithRacemapAndLeaderBoardFailureEvent");
        sb.append(", message: ").append(message);
        if (caught != null) {
            sb.append(", ex: ").append(caught);
        }
        return sb.toString();
    }
}
