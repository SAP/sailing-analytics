package com.sap.sailing.gwt.home.mobile.places.morelogininformation;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Label;

public class MoreLoginInformationActivity implements Activity {

    public MoreLoginInformationActivity(Place place) {
    }

    @Override
    public String mayStop() {
        return null;
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        Label content = new Label("Best login here!");
        panel.setWidget(content);
    }

}
