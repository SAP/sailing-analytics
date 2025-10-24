package com.sap.sailing.gwt.home.mobile.places.start;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.communication.anniversary.GetAnniversariesAction;
import com.sap.sailing.gwt.home.communication.start.EventQuickfinderDTO;
import com.sap.sailing.gwt.home.communication.start.EventStageDTO;
import com.sap.sailing.gwt.home.communication.start.GetRecentEventsAction;
import com.sap.sailing.gwt.home.communication.start.GetStagedEventsAction;
import com.sap.sailing.gwt.home.mobile.app.MobileApplicationClientFactory;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
import com.sap.sailing.gwt.home.mobile.places.start.StartView.Presenter;
import com.sap.sailing.gwt.home.shared.app.ActivityCallback;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sailing.gwt.home.shared.refresh.RefreshManagerWithErrorAndBusy;
import com.sap.sse.gwt.dispatch.shared.commands.ListResult;
import com.sap.sse.security.ui.client.UserService;

public class StartActivity extends AbstractActivity implements Presenter {
    private final MobileApplicationClientFactory clientFactory;
    private final StartPlace place;

    public StartActivity(StartPlace place, MobileApplicationClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.place = place;
    }

    @Override
    public void start(final AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(clientFactory.createBusyView());
        Window.setTitle(place.getTitle());
        final StartView view = new StartViewImpl(StartActivity.this);
        final RefreshManagerWithErrorAndBusy refreshManager = new RefreshManagerWithErrorAndBusy(view.asWidget(), panel,
                clientFactory.getDispatch(), clientFactory);
        clientFactory.getDispatch().execute(new GetStagedEventsAction(true), 
                new ActivityCallback<ListResult<EventStageDTO>>(clientFactory, panel) {
            @Override
            public void onSuccess(ListResult<EventStageDTO> result) {
                refreshManager.attachContent();
                Window.setTitle(place.getTitle());
                view.setFeaturedEvents(result.getValues());
            }
        });
        clientFactory.getDispatch().execute(new GetRecentEventsAction(15),
                new ActivityCallback<ListResult<EventQuickfinderDTO>>(clientFactory, panel) {
            @Override
            public void onSuccess(ListResult<EventQuickfinderDTO> result) {
                view.setQuickFinderValues(result.getValues());
            }
        });

        refreshManager.add(view.getAnniversariesView(), new GetAnniversariesAction());
    }

    @Override
    public MobilePlacesNavigator getNavigator() {
        return clientFactory.getNavigator();
    }

    @Override
    public PlaceNavigation<?> getEventNavigation(EventQuickfinderDTO event) {
        return clientFactory.getNavigator().getEventNavigation(event.getId().toString(), event.getBaseURL(), event.isOnRemoteServer());
    }

    @Override
    public UserService getUserService() {
        return clientFactory.getUserService();
    }
}
