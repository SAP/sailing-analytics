package com.sap.sailing.gwt.home.desktop.partials.seriesheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.home.desktop.partials.sharing.EventHeaderSharingButtons;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.overviewtab.RegattaOverviewPlace;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.SeriesView;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.SeriesView.Presenter;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.shared.SharingMetadataProvider;
import com.sap.sailing.gwt.home.shared.places.ShareablePlaceContext;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sailing.gwt.home.shared.utils.LogoUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.LinkUtil;
import com.sap.sse.gwt.client.media.MediaMenuIcon;

public class SeriesHeader extends Composite {
    private static SeriesHeaderUiBinder uiBinder = GWT.create(SeriesHeaderUiBinder.class);

    interface SeriesHeaderUiBinder extends UiBinder<Widget, SeriesHeader> {
    }
    
    @UiField StringMessages i18n;
    
    @UiField DivElement eventLogo;
    @UiField(provided=true) MediaMenuIcon seriesLogoMenuButton;
    @UiField SpanElement eventName;
    @UiField DivElement eventState;
    @UiField FlowPanel venues;
    @UiField EventHeaderSharingButtons sharing;

    private EventSeriesViewDTO series;
    private Presenter presenter;
    boolean dropdownShown = false;
    
    public SeriesHeader(SeriesView.Presenter presenter) {
        this.series = presenter.getSeriesDTO();
        this.presenter = presenter;
        SeriesHeaderResources.INSTANCE.css().ensureInjected();
        seriesLogoMenuButton = new MediaMenuIcon(presenter.getUserService(), "takedownRequestForLogoImage");
        initWidget(uiBinder.createAndBindUi(this));
        initFields();
        initSharing();
    }

    private void initSharing() {
        sharing.setUp(new SharingMetadataProvider() {
            @Override
            public String getShortText() {
                return StringMessages.INSTANCE.seriesSharingShortText(series.getDisplayName());
            }
            @Override
            public ShareablePlaceContext getContext() {
                return presenter.getCtx();
            }
            
        });
    }

    private void initFields() {
        LogoUtil.setEventLogo(eventLogo, series, seriesLogoMenuButton);
        eventName.setInnerText(series.getDisplayName());
        LabelTypeUtil.renderLabelType(eventState, series.getState().getStateMarker());
        
        for (EventAndLeaderboardReferenceWithStateDTO eventOfSeries : series.getEventsAndRegattasOfSeriesAscending()) {
            if (eventOfSeries.getState() == EventState.PLANNED) {
                InlineLabel eventLabel = new InlineLabel(eventOfSeries.getDisplayName());
                eventLabel.addStyleName(SeriesHeaderResources.INSTANCE.css().eventheader_intro_details_item());
                eventLabel.addStyleName(SeriesHeaderResources.INSTANCE.css().eventheader_intro_details_item_inactive());
                venues.add(eventLabel);
            } else {
                Anchor eventAnchor = new Anchor(eventOfSeries.getDisplayName());
                eventAnchor.addStyleName(SeriesHeaderResources.INSTANCE.css().eventheader_intro_details_item());
                final PlaceNavigation<RegattaOverviewPlace> regattaNavigation = presenter
                        .getRegattaNavigation(eventOfSeries.getId(), eventOfSeries.getLeaderboardName());
                eventAnchor.setHref(regattaNavigation.getTargetUrl());
                venues.add(eventAnchor);
                eventAnchor.addClickHandler(event -> {
                    if (LinkUtil.handleLinkClick(Event.as(event.getNativeEvent()))) {
                        event.preventDefault();
                        regattaNavigation.goToPlace();
                    }
                });
            }
        }
    }
}
