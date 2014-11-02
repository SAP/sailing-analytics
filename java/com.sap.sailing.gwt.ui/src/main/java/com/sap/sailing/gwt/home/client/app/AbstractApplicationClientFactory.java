package com.sap.sailing.gwt.home.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.client.place.start.StartPlace;
import com.sap.sailing.gwt.ui.client.MediaService;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingService;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sse.gwt.client.mvp.ClientFactoryImpl;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.shared.UserManagementService;
import com.sap.sse.security.ui.shared.UserManagementServiceAsync;


public abstract class AbstractApplicationClientFactory extends ClientFactoryImpl implements ApplicationClientFactory {
    private final SailingServiceAsync sailingService;
    private final MediaServiceAsync mediaService;
    private final HomePlacesNavigator navigator;
    private final UserManagementServiceAsync userManagementService;
    private final UserService userService;

    public AbstractApplicationClientFactory(ApplicationTopLevelView root, EventBus eventBus, PlaceController placeController) {
        super(root, eventBus, placeController);
        navigator = new HomePlacesNavigator(placeController);
        sailingService = GWT.create(SailingService.class);
        mediaService = GWT.create(MediaService.class);
        userManagementService = GWT.create(UserManagementService.class);
        userService = new UserService(userManagementService);
    }
    
    @Override
    public Place getDefaultPlace() {
        return new StartPlace();
    }

    @Override
    public SailingServiceAsync getSailingService() {
        return sailingService;
    }

    @Override
    public MediaServiceAsync getMediaService() {
        return mediaService;
    }

    @Override
    public HomePlacesNavigator getHomePlacesNavigator() {
        return navigator;
    }

    @Override
    public UserManagementServiceAsync getUserManagementService() {
        return userManagementService;
    }

    @Override
    public UserService getUserService() {
        return userService;
    }
}
