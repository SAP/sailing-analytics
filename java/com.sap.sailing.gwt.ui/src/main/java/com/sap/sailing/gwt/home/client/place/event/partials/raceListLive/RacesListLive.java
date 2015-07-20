package com.sap.sailing.gwt.home.client.place.event.partials.raceListLive;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.client.place.event.EventView;
import com.sap.sailing.gwt.home.client.place.event.partials.racelist.RaceList;
import com.sap.sailing.gwt.home.client.place.event.regatta.tabs.reload.RefreshableWidget;
import com.sap.sailing.gwt.ui.shared.dispatch.event.LiveRacesDTO;

public class RacesListLive extends Composite implements RefreshableWidget<LiveRacesDTO> {

    private static RacesListLiveUiBinder uiBinder = GWT.create(RacesListLiveUiBinder.class);

    interface RacesListLiveUiBinder extends UiBinder<Widget, RacesListLive> {
    }

    @UiField(provided = true) RaceList raceList;
    
    public RacesListLive(EventView.Presenter presenter, boolean showRegattaDetails) {
        RacesListLiveResources.INSTANCE.css().ensureInjected();
        raceList = new RaceList(presenter, showRegattaDetails);
        initWidget(uiBinder.createAndBindUi(this));
        getElement().getStyle().setDisplay(Display.NONE);
    }

    @Override
    public void setData(LiveRacesDTO data) {
        if(data == null || data.getRaces().isEmpty()) {
            getElement().getStyle().setDisplay(Display.NONE);
        } else {
            getElement().getStyle().clearDisplay();
            this.raceList.setListData(data);
        }
    }

}
