package com.sap.sailing.gwt.home.mobile.partials.eventheader;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.mobile.partials.sharing.SharingButtons;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.partials.shared.SharingMetadataProvider;
import com.sap.sailing.gwt.home.shared.places.ShareablePlaceContext;
import com.sap.sailing.gwt.home.shared.places.event.EventContext;
import com.sap.sailing.gwt.home.shared.utils.EventDatesFormatterUtil;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sailing.gwt.home.shared.utils.LogoUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.databylogo.DataByLogo;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class EventHeader extends Composite {
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, EventHeader> {
    }
    
    @UiField SpanElement eventNameUi;
    @UiField BRElement nameSeparatorUi;
    @UiField SpanElement eventRegattaUi;
    @UiField DivElement eventStateUi;
    @UiField AnchorElement eventLogoUi;
    @UiField(provided=true) MediaMenuIcon eventLogoMenuButton;
    @UiField DivElement eventDateUi;
    @UiField DivElement eventLocationUi;
    @UiField DivElement eventHeader;
    @UiField DataByLogo dataByLogo;
    @UiField SharingButtons sharingButtons;

    public EventHeader(EventContext eventContext, EventViewDTO event, String optionalRegattaDisplayName, PlaceNavigation<?> logoNavigation, TakedownNoticeService takedownNoticeService) {
        EventHeaderResources.INSTANCE.css().ensureInjected();
        this.eventLogoMenuButton = new MediaMenuIcon(takedownNoticeService, "takedownRequestForLogoImage");
        initWidget(uiBinder.createAndBindUi(this));
        setUiFieldValues(event, optionalRegattaDisplayName, logoNavigation);
        setupSharing(eventContext, event);
    }

    private void setUiFieldValues(EventViewDTO event, String optionalRegattaDisplayName, PlaceNavigation<?> logoNavigation) {
        eventNameUi.setInnerText(event.getDisplayName());
        if (optionalRegattaDisplayName != null && !optionalRegattaDisplayName.isEmpty()) {
            eventRegattaUi.setInnerText(optionalRegattaDisplayName);
        } else {
            nameSeparatorUi.removeFromParent();
        }
        LabelTypeUtil.renderLabelType(eventStateUi, event.getState().getStateMarker());
        LogoUtil.setEventLogo(eventLogoUi, event, eventLogoMenuButton);
        if (logoNavigation != null) {
            logoNavigation.configureAnchorElement(eventLogoUi);
        }
        dataByLogo.setUp(event.getTrackingConnectorInfos(), /** colorIfPossible **/ true, /** enforceTextColor **/ true);
        if (dataByLogo.isVisible()) {
            this.addStyleName(EventHeaderResources.INSTANCE.css().eventheader_with_logo());
        }
        eventDateUi.setInnerText(EventDatesFormatterUtil.formatDateRangeWithYear(event.getStartDate(), event.getEndDate()));
        eventLocationUi.setInnerText(event.getLocationAndVenueAndCountry());
    }

    private void setupSharing(EventContext eventContext, EventViewDTO event) {
        sharingButtons.setUp(new SharingMetadataProvider() {
            
            @Override
            public ShareablePlaceContext getContext() {
                return eventContext;
            }
            
            @Override
            public String getShortText() {
                String dateString = EventDatesFormatterUtil.formatDateRangeWithYear(event.getStartDate(), event.getEndDate());
                return StringMessages.INSTANCE.eventSharingShortText(event.getDisplayName(), event.getLocationOrVenue(), dateString);
            }
        });
    }
}
