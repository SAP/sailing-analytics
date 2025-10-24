package com.sap.sailing.gwt.autoplay.client.app;

import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.desktop.places.user.profile.WithSailingServiceWrite;
import com.sap.sailing.gwt.ui.client.SailingClientFactory;

public interface AutoPlayClientFactory extends SailingClientFactory, WithSailingServiceWrite {
    AutoPlayPlaceNavigator getPlaceNavigator();

    void setAutoPlayContext(AutoPlayContext configurationSixtyInch);

    /**
     * Will signal an error to the autoplay system if the context is not properly configured, this will lead to error
     * recovery tries by restarting the player
     */
    AutoPlayContext getAutoPlayCtxSignalError();

    SailingDispatchSystem getDispatch();

    /**
     * Required to determine if the ctx is properly populated from either the StartNode, or the url. If not go to
     * StartNode.
     */
    boolean isConfigured();
}
