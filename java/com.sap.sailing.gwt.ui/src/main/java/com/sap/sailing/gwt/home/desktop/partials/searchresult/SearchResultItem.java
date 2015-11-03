package com.sap.sailing.gwt.home.desktop.partials.searchresult;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.AbstractEventRegattaPlace;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.shared.utils.EventDatesFormatterUtil;
import com.sap.sailing.gwt.ui.shared.EventBaseDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardSearchResultDTO;

public class SearchResultItem extends Composite {
    interface SearchResultUiBinder extends UiBinder<Widget, SearchResultItem> {
    }
    
    private static SearchResultUiBinder uiBinder = GWT.create(SearchResultUiBinder.class);

    private static final HyperlinkImpl HYPERLINK_IMPL = GWT.create(HyperlinkImpl.class);

    @UiField Anchor regattaLink;
    @UiField SpanElement resultRegattaDetails;
    @UiField SpanElement resultEventDate;
    @UiField SpanElement resultEventVenue;
    @UiField Anchor eventOverviewLink;
    
    private final DesktopPlacesNavigator placeNavigator;
    private final PlaceNavigation<AbstractEventRegattaPlace> regattaNavigation;
    private final PlaceNavigation<EventDefaultPlace> eventNavigation;

    public SearchResultItem(DesktopPlacesNavigator navigator, LeaderboardSearchResultDTO searchResult) {
        this.placeNavigator = navigator;
        SearchResultResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        EventBaseDTO event = searchResult.getEvent();
        regattaNavigation = placeNavigator.getRegattaNavigation(event.id.toString(), searchResult.getLeaderboardName(), searchResult.getBaseURL(), searchResult.isOnRemoteServer());
        eventNavigation = placeNavigator.getEventNavigation(event.id.toString(), searchResult.getBaseURL(), searchResult.isOnRemoteServer());
        String headlineLink = searchResult.getLeaderboardDisplayName();
        if(headlineLink == null) {
            headlineLink = searchResult.getRegattaName() != null ? searchResult.getRegattaName() : searchResult.getLeaderboardName();
        }
        regattaLink.setHref(regattaNavigation.getTargetUrl());
        regattaLink.setText(headlineLink);
        eventOverviewLink.setHref(eventNavigation.getTargetUrl());
        eventOverviewLink.setText(searchResult.getEvent().getName());
        resultEventVenue.setInnerText(searchResult.getEvent().venue.getName());
        if(searchResult.getEvent().startDate != null) {
            resultEventDate.setInnerText(EventDatesFormatterUtil.formatDateRangeWithYear(searchResult.getEvent().startDate, searchResult.getEvent().endDate));
        } else {
            resultEventDate.setInnerText("Unknown date");
        }
    }

    @UiHandler("regattaLink")
    public void goToRegatta(ClickEvent e) {
        handleClickEvent(e, regattaNavigation);
    }

    @UiHandler("eventOverviewLink")
    public void goToEventPlace(ClickEvent e) {
        handleClickEvent(e, eventNavigation);
    }
    
    private void handleClickEvent(ClickEvent e, PlaceNavigation<?> placeNavigation) {
        if (HYPERLINK_IMPL.handleAsClick((Event) e.getNativeEvent())) {
            placeNavigator.goToPlace(placeNavigation);
            e.preventDefault();
         }
    }
}
