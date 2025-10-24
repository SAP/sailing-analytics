package com.sap.sailing.gwt.home.desktop.partials.event;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.LabelType;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventSeriesDTO;
import com.sap.sailing.gwt.home.desktop.utils.LongNamesUtil;
import com.sap.sailing.gwt.home.shared.SharedHomeResources;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.utils.EventDatesFormatterUtil;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.LinkUtil;
import com.sap.sse.gwt.client.media.MediaMenuIcon;
import com.sap.sse.gwt.client.media.TakedownNoticeService;

public class EventTeaser extends Composite {

    @UiField SpanElement eventName;
    @UiField SpanElement venue;
    @UiField SpanElement eventDate;
    @UiField AnchorElement eventLink;
    @UiField DivElement eventImage;
    @UiField DivElement eventState;
    @UiField(provided=true) MediaMenuIcon takedownButton;

    private final EventMetadataDTO event;

    interface EventTeaserUiBinder extends UiBinder<Widget, EventTeaser> {
    }

    private static EventTeaserUiBinder uiBinder = GWT.create(EventTeaserUiBinder.class);
    private final LabelType labelType;

    public EventTeaser(final PlaceNavigation<?> placeNavigation, final EventMetadataDTO event, LabelType labelType, TakedownNoticeService takedownNoticeService) {
        this.event = event;
        this.labelType = labelType;
        this.takedownButton = new MediaMenuIcon(takedownNoticeService, "takedownRequestForEventTeaserImage");
        EventTeaserResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        eventLink.setHref(placeNavigation.getTargetUrl());
        this.ensureDebugId("eventTeaser-" + event.getId());
        Event.sinkEvents(eventLink, Event.ONCLICK);
        Event.setEventListener(eventLink, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if(LinkUtil.handleLinkClick(event)) {
                    event.preventDefault();
                    placeNavigation.goToPlace();
                }
            }
        });
        updateUI();
    }

    private void updateUI() {
        eventName.setInnerSafeHtml(LongNamesUtil.breakLongName(event.getDisplayName()));
        if(labelType.isRendered()) {
            LabelTypeUtil.renderLabelType(eventState, labelType);
        } else {
            eventState.removeFromParent();
        }
        venue.setInnerText(event.getLocationOrVenue());
        eventDate.setInnerText(EventDatesFormatterUtil.formatDateRangeWithoutYear(event.getStartDate(), event.getEndDate()));
        final StringBuilder thumbnailUrlBuilder = new StringBuilder("url('");
        final String thumbnailImageUrl = event.getThumbnailImageURL();
        if (thumbnailImageUrl == null || thumbnailImageUrl.isEmpty()) {
            thumbnailUrlBuilder.append(SharedHomeResources.INSTANCE.defaultEventPhotoImage().getSafeUri().asString());
        } else {
            thumbnailUrlBuilder.append(UriUtils.fromString(thumbnailImageUrl).asString());
        }
        thumbnailUrlBuilder.append("')");
        eventImage.getStyle().setBackgroundImage(thumbnailUrlBuilder.toString());
        takedownButton.setData(event.getDisplayName(), thumbnailUrlBuilder.toString());
    }
    
    public void setSeriesInformation(PlaceNavigation<?> seriesNavigation, EventListEventSeriesDTO eventSeries) {
        eventImage.appendChild(new EventTeaserSeriesInfoCorner(seriesNavigation, eventSeries).getElement());
        eventName.setInnerSafeHtml(LongNamesUtil.breakLongName(eventSeries.getSeriesDisplayName()));
        venue.setInnerText(StringMessages.INSTANCE.lastEvent(event.getLocationOrVenue()));
        eventImage.setTitle(StringMessages.INSTANCE.teaserOverallLinkToolTip());
    }
    
    public void hideImage(boolean hide) {
        if (hide) {
            eventImage.getStyle().setDisplay(Display.NONE);
        } else {
            eventImage.getStyle().setDisplay(Display.BLOCK);
        }
    }
}
