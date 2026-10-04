package com.sap.sailing.gwt.home.shared.places.error;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.gwt.client.mvp.ErrorView;

public abstract class AbstractErrorActivity extends AbstractActivity {

    private final ErrorPlace currentPlace;
    private final Command reloadCommand;

    public AbstractErrorActivity(ErrorPlace place, PlaceController placeController) {
        this.currentPlace = place;
        this.reloadCommand = currentPlace.getComingFrom() == null ? null
                : () -> placeController.goTo(currentPlace.getComingFrom());
    }

    @Override
    public final void start(AcceptsOneWidget panel, EventBus eventBus) {
        if (currentPlace.isReloadedError()) {
            // When the error place is reloaded, we cannot go to the place where we were coming from.
            // To avoid the reload of the error page itself, we will redirect to the main page instead.
            Window.Location.assign(SharedLandscapeConstants.DEFAULT_SAILING_SERVER_URL);
        } else {
            final String message = currentPlace.getErrorMessage();
            final String details = currentPlace.getErrorMessageDetail();
            panel.setWidget(currentPlace.hasCustomErrorMessages()
                    ? createView(message, details, currentPlace.getException(), reloadCommand)
                    : createView(message, currentPlace.getException(), reloadCommand));
        }
    }
    
    protected abstract ErrorView createView(String errorMsg, Throwable reason, Command reloadCommand);
    
    protected abstract ErrorView createView(String customMsg, String errorMsg, Throwable reason, Command reloadCommand);
}
