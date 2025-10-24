package com.sap.sailing.gwt.home.mobile.partials.seriesheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.home.mobile.partials.sharing.SharingButtons;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.shared.SharingMetadataProvider;
import com.sap.sailing.gwt.home.shared.places.ShareablePlaceContext;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sailing.gwt.home.shared.utils.LogoUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class SeriesHeader extends Composite {
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, SeriesHeader> {
    }
    
    @UiField SpanElement eventNameUi;
    @UiField DivElement eventStateUi;
    @UiField AnchorElement eventLogoUi;
    @UiField(provided=true) MediaMenuIcon seriesLogoMenuButton;
    @UiField DivElement locationsUi;
    @UiField SharingButtons sharingButtons;

    public SeriesHeader(SeriesContext seriesContext, EventSeriesViewDTO series, TakedownNoticeService takedownNoticeService) {
        this(seriesContext, series, null, takedownNoticeService);
    }
    
    public SeriesHeader(SeriesContext seriesContext, EventSeriesViewDTO series, PlaceNavigation<?> logoNavigation, TakedownNoticeService takedownNoticeService) {
        SeriesHeaderResources.INSTANCE.css().ensureInjected();
        this.seriesLogoMenuButton = new MediaMenuIcon(takedownNoticeService, "takedownRequestForLogoImage");
        initWidget(uiBinder.createAndBindUi(this));
        setUiFieldValues(series, logoNavigation);
        setupSharing(seriesContext, series);
    }

    private void setUiFieldValues(EventSeriesViewDTO series, PlaceNavigation<?> logoNavigation) {
        LogoUtil.setEventLogo(eventLogoUi, series, seriesLogoMenuButton);
        if (logoNavigation != null) {
            logoNavigation.configureAnchorElement(eventLogoUi);
        }
        eventNameUi.setInnerText(series.getDisplayName());
        LabelTypeUtil.renderLabelType(eventStateUi, series.getState().getStateMarker());
        StringBuilder locationsBuilder = new StringBuilder();
        boolean first = true;
        for (EventAndLeaderboardReferenceWithStateDTO eventOfSeries : series.getEventsAndRegattasOfSeriesAscending()) {
            if(!first) {
                locationsBuilder.append(", ");
            }
            final String location = eventOfSeries.getDisplayName();
            if (location != null && !location.isEmpty()) {
                locationsBuilder.append(location);
                first = false;
            }
        }
        locationsUi.setInnerText(locationsBuilder.toString());
    }

    private void setupSharing(SeriesContext seriesContext, EventSeriesViewDTO series) {
        sharingButtons.setUp(new SharingMetadataProvider() {
            @Override
            public ShareablePlaceContext getContext() {
                return seriesContext;
            }

            @Override
            public String getShortText() {
                return StringMessages.INSTANCE.seriesSharingShortText(series.getDisplayName());
            }
        });
    }
}
