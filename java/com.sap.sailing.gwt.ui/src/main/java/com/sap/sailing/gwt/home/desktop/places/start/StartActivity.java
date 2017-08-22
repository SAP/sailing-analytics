package com.sap.sailing.gwt.home.desktop.places.start;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.communication.anniversary.AnniversaryDTO;
import com.sap.sailing.gwt.home.communication.anniversary.GetAnniversariesAction;
import com.sap.sailing.gwt.home.communication.start.GetStartViewAction;
import com.sap.sailing.gwt.home.communication.start.StartViewDTO;
import com.sap.sailing.gwt.home.shared.app.ActivityCallback;
import com.sap.sailing.gwt.home.shared.partials.anniversary.AnniversariesPresenter;
import com.sap.sailing.gwt.home.shared.places.start.StartPlace;
import com.sap.sse.gwt.dispatch.shared.commands.ListResult;

public class StartActivity extends AbstractActivity {
    private final StartClientFactory clientFactory;
    private final StartPlace place;

    public StartActivity(StartPlace place, StartClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        this.place = place;
    }

    @Override
    public void start(final AcceptsOneWidget panel, EventBus eventBus) {
        panel.setWidget(clientFactory.createBusyView());
        final StartView view = clientFactory.createStartView();
        clientFactory.getDispatch().execute(new GetStartViewAction(), new ActivityCallback<StartViewDTO>(clientFactory, panel) {
            @Override
            public void onSuccess(StartViewDTO result) {
                panel.setWidget(view.asWidget());
                Window.setTitle(place.getTitle());
                view.setData(result);
            }
        });

        final AnniversariesPresenter anniversariesPresenter = new AnniversariesPresenter(view.getAnniversariesView());
        clientFactory.getDispatch().execute(new GetAnniversariesAction(),
                new ActivityCallback<ListResult<AnniversaryDTO>>(clientFactory, panel) {
                    @Override
                    public void onSuccess(ListResult<AnniversaryDTO> result) {
                        result.getValues().forEach(anniversariesPresenter::addAnniversary);
                    }
                });
    }
}
