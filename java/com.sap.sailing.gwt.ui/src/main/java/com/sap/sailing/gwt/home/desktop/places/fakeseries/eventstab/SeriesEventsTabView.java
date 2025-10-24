package com.sap.sailing.gwt.home.desktop.places.fakeseries.eventstab;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.desktop.partials.eventsrecent.RecentEventTeaser;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.SeriesTabView;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.SeriesView;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;

/**
 * Created by pgtaboada on 25.11.14.
 */
public class SeriesEventsTabView extends Composite implements SeriesTabView<SeriesEventsPlace> {

    interface MyBinder extends UiBinder<Widget, SeriesEventsTabView> {
    }

    private static MyBinder ourUiBinder = GWT.create(MyBinder.class);
    private SeriesView.Presenter currentPresenter;

    @UiField FlowPanel eventsContainer;
    
    public SeriesEventsTabView() {
    }

    @Override
    public Class<SeriesEventsPlace> getPlaceClassForActivation() {
        return SeriesEventsPlace.class;
    }
    
    @Override
    public void start(SeriesEventsPlace myPlace, AcceptsOneWidget contentArea) {
        initWidget(ourUiBinder.createAndBindUi(this));
        for (EventMetadataDTO eventOfSeries : currentPresenter.getSeriesDTO().getEventsDescending()) {
            if(eventOfSeries.getState() == EventState.PLANNED) {
                continue;
            }
            final PlaceNavigation<EventDefaultPlace> eventNavigation = currentPresenter.getEventNavigation(eventOfSeries.getId());
            RecentEventTeaser eventTeaser = new RecentEventTeaser(eventNavigation, eventOfSeries, eventOfSeries.getState().getStateMarker(), currentPresenter.getUserService());
            eventsContainer.add(eventTeaser);
        }
        contentArea.setWidget(this);
    }

    @Override
    public void stop() {
    }

    @Override
    public SeriesEventsPlace placeToFire() {
        return new SeriesEventsPlace(currentPresenter.getCtx());
    }

    @Override
    public void setPresenter(SeriesView.Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
    }
    
    @Override
    public SeriesView.Presenter getPresenter() {
        return currentPresenter;
    }
}