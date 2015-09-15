package com.sap.sailing.gwt.home.desktop.partials.seriesheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.LinkUtil;
import com.sap.sailing.gwt.home.client.place.event.EventDefaultPlace;
import com.sap.sailing.gwt.home.client.place.fakeseries.SeriesView;
import com.sap.sailing.gwt.home.client.place.fakeseries.SeriesView.Presenter;
import com.sap.sailing.gwt.home.desktop.partials.sharing.SharingButtons;
import com.sap.sailing.gwt.home.desktop.partials.sharing.SharingMetadataProvider;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.ui.shared.general.EventMetadataDTO;
import com.sap.sailing.gwt.ui.shared.general.EventState;

public class SeriesHeader extends Composite {
    private static SeriesHeaderUiBinder uiBinder = GWT.create(SeriesHeaderUiBinder.class);

    interface SeriesHeaderUiBinder extends UiBinder<Widget, SeriesHeader> {
    }
    
    @UiField StringMessages i18n;
    
    @UiField DivElement eventLogo;
    @UiField SpanElement eventName;
    @UiField DivElement eventState;
    @UiField FlowPanel venues;
    @UiField SharingButtons sharing;

    private EventSeriesViewDTO series;
    private Presenter presenter;
    boolean dropdownShown = false;
    
    public SeriesHeader(SeriesView.Presenter presenter) {
        this.series = presenter.getCtx().getSeriesDTO();
        this.presenter = presenter;
        
        SeriesHeaderResources.INSTANCE.css().ensureInjected();
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
            public String getLongText(String url) {
                return StringMessages.INSTANCE.seriesSharingLongText(series.getDisplayName(), url);
            }
        });
    }

    private void initFields() {
        String logoUrl = series.getLogoImage() != null ? series.getLogoImage().getSourceRef() : SeriesHeaderResources.INSTANCE.defaultEventLogoImage().getSafeUri().asString();
        eventLogo.getStyle().setBackgroundImage("url(" + logoUrl + ")");
        eventLogo.setTitle(series.getDisplayName());
        eventName.setInnerText(series.getDisplayName());
        LabelTypeUtil.renderLabelType(eventState, series.getState().getStateMarker());
        
        for (EventMetadataDTO eventOfSeries : series.getEvents()) {
            if(eventOfSeries.getState() == EventState.PLANNED) {
                InlineLabel eventLabel = new InlineLabel(eventOfSeries.getLocationOrVenue());
                eventLabel.addStyleName(SeriesHeaderResources.INSTANCE.css().eventheader_intro_details_item());
                eventLabel.addStyleName(SeriesHeaderResources.INSTANCE.css().eventheader_intro_details_item_inactive());
                venues.add(eventLabel);
            } else {
                Anchor eventAnchor = new Anchor(eventOfSeries.getLocationOrVenue());
                eventAnchor.addStyleName(SeriesHeaderResources.INSTANCE.css().eventheader_intro_details_item());
                final PlaceNavigation<EventDefaultPlace> eventNavigation = presenter.getEventNavigation(eventOfSeries.getId());
                eventAnchor.setHref(eventNavigation.getTargetUrl());
                venues.add(eventAnchor);
                eventAnchor.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        if(LinkUtil.handleLinkClick(Event.as(event.getNativeEvent()))) {
                            event.preventDefault();
                            eventNavigation.goToPlace();
                        }
                    }
                });
            }
        }
    }
}
