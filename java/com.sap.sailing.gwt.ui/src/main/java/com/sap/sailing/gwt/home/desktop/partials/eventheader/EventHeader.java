package com.sap.sailing.gwt.home.desktop.partials.eventheader;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.communication.eventview.HasRegattaMetadata;
import com.sap.sailing.gwt.home.desktop.partials.sharing.EventHeaderSharingButtons;
import com.sap.sailing.gwt.home.desktop.places.event.EventView;
import com.sap.sailing.gwt.home.desktop.places.event.EventView.PlaceCallback;
import com.sap.sailing.gwt.home.desktop.places.event.EventView.Presenter;
import com.sap.sailing.gwt.home.shared.partials.shared.SharingMetadataProvider;
import com.sap.sailing.gwt.home.shared.places.ShareablePlaceContext;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.shared.utils.EventDatesFormatterUtil;
import com.sap.sailing.gwt.home.shared.utils.LabelTypeUtil;
import com.sap.sailing.gwt.home.shared.utils.LogoUtil;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.databylogo.DataByLogo;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.LinkUtil;
import com.sap.sse.gwt.client.controls.dropdown.DropdownHandler;
import com.sap.sse.gwt.client.controls.dropdown.DropdownItem;
import com.sap.sse.gwt.client.controls.dropdown.DropdownResources;
import com.sap.sse.gwt.client.media.MediaMenuIcon;

public class EventHeader extends Composite {
    private static EventHeaderUiBinder uiBinder = GWT.create(EventHeaderUiBinder.class);

    interface EventHeaderUiBinder extends UiBinder<Widget, EventHeader> {
    }
    
    @UiField StringMessages i18n;
    
    @UiField DivElement eventLogo;
    @UiField(provided=true) MediaMenuIcon eventLogoMenuButton;
    @UiField AnchorElement eventLogoAnchorUi;
    @UiField HeadingElement staticTitle;
    @UiField SpanElement eventName;
    @UiField DivElement eventState;
    @UiField DivElement dropdownTitle;
    @UiField SpanElement dropdownEventName;
    @UiField DivElement dropdownEventState;
    @UiField AnchorElement dropdownTrigger;
    @UiField DivElement eventDate;
    @UiField SpanElement eventVenue;
    @UiField DivElement eventVenueContainer;
    @UiField AnchorElement eventLink;
    @UiField DivElement competitors;
    @UiField DivElement races;
    @UiField DivElement eventCategory;
    @UiField DivElement courseAreaUi;
    @UiField FlowPanel dropdownContent;
    @UiField EventHeaderSharingButtons sharing;
    @UiField DataByLogo dataByLogo;
    @UiField EventHeaderResources local_res;
    @UiField DropdownResources dropdownitem_res;

    private EventViewDTO event;
    private Presenter presenter;
    
    public EventHeader(EventView.Presenter presenter) {
        this.event = presenter.getEventDTO();
        this.presenter = presenter;
        eventLogoMenuButton = new MediaMenuIcon(presenter.getUserService(), "takedownRequestForLogoImage");
        initWidget(uiBinder.createAndBindUi(this));
        local_res.css().ensureInjected();
        dropdownitem_res.css().ensureInjected();
        initFields();
        initSharing();
    }

    private void initSharing() {
        sharing.setUp(new SharingMetadataProvider() {
            @Override
            public String getShortText() {
                // TODO regatta details?
                String dateString = EventDatesFormatterUtil.formatDateRangeWithYear(event.getStartDate(), event.getEndDate());
                return StringMessages.INSTANCE.eventSharingShortText(event.getDisplayName(), event.getLocationOrVenue(), dateString);
            }
            
            @Override
            public ShareablePlaceContext getContext() {
                return presenter.getCtx();
            }
        });
    }

    private void initFields() {
        LogoUtil.setEventLogo(eventLogo, event, eventLogoMenuButton);
        if (presenter.showRegattaMetadata()) {
            presenter.getCurrentEventNavigation().configureAnchorElement(eventLogoAnchorUi);
        }
        String eventDisplayName = event.getDisplayName();
        String nameToShow;
        if (presenter.showRegattaMetadata()) {
            HasRegattaMetadata regattaMetadata = presenter.getRegattaMetadata();
            String regattaDisplayName = regattaMetadata.getDisplayName();
            if (regattaDisplayName.toLowerCase().contains(eventDisplayName.toLowerCase())) {
                nameToShow = regattaDisplayName;
            } else {
                nameToShow = eventDisplayName + " - " + regattaDisplayName;
            }
            if (regattaMetadata.getCompetitorsCount() > 0) {
                competitors.setInnerText((i18n.competitorsCount(regattaMetadata.getCompetitorsCount())));
            } else {
                hide(competitors);
            }
            if (regattaMetadata.getRaceCount() > 0) {
                races.setInnerText((i18n.racesCount(regattaMetadata.getRaceCount())));
            } else {
                hide(races);
            }
            if (regattaMetadata.getDefaultCourseAreaName() != null) {
                courseAreaUi.setInnerText(i18n.courseAreaName(regattaMetadata.getDefaultCourseAreaName()));
            } else {
                hide(courseAreaUi);
            }
            if (regattaMetadata.getLeaderboardGroupNames() != null) {
                eventCategory.setInnerText(Util.joinStrings(", ", regattaMetadata.getLeaderboardGroupNames()));
            } else {
                hide(eventCategory);
            }
            Date startDate = regattaMetadata.getStartDate() != null ? regattaMetadata.getStartDate() : event.getStartDate();
            Date endDate = regattaMetadata.getEndDate() != null ? regattaMetadata.getEndDate() : event.getEndDate();
            eventDate.setInnerHTML(EventDatesFormatterUtil.formatDateRangeWithYear(startDate, endDate));
            hide(eventVenueContainer, eventLink);
        } else {
            nameToShow = eventDisplayName;
            eventDate.setInnerHTML(EventDatesFormatterUtil.formatDateRangeWithYear(event.getStartDate(), event.getEndDate()));
            eventVenue.setInnerText(event.getLocationAndVenueAndCountry());
            if (event.getOfficialWebsiteURL() != null) {
                String title = withoutPrefix(event.getOfficialWebsiteURL(), "http://", "https://");
                if (title.length() > 35) {
                    title = StringMessages.INSTANCE.officalEventWebsite();
                }
                eventLink.setInnerText(title);
                eventLink.setHref(event.getOfficialWebsiteURL());
            } else {
                hide(eventLink);
            }
            hide(competitors, races, courseAreaUi, eventCategory);
        }
        dataByLogo.setUp(event.getTrackingConnectorInfos(), /** colorIfPossible **/ true, /** enforceTextColor **/ false);
        initTitleAndSelection(nameToShow);
    }

    private void initTitleAndSelection(String nameToShow) {
        if (!presenter.needsSelectionInHeader()) {
            eventName.setInnerText(nameToShow);
            LabelTypeUtil.renderLabelType(eventState, event.getState().getStateMarker());
            UIObject.ensureDebugId(eventState, "EventStateLabelDiv");
            hide(dropdownTitle);
        } else {
            dropdownEventName.setInnerText(nameToShow);
            LabelTypeUtil.renderLabelType(dropdownEventState,
                    presenter.showRegattaMetadata() ? presenter.getRegattaMetadata().getState().getStateMarker()
                            : event.getState().getStateMarker());
            UIObject.ensureDebugId(dropdownEventState, "EventStateLabelDiv");
            hide(staticTitle);
            initDropdown();
        }
    }

    private void initDropdown() {
        new DropdownHandler(dropdownTrigger, dropdownContent.getElement()) {
            @Override
            protected void dropdownStateChanged(boolean dropdownShown) {
                if (dropdownShown) {
                    dropdownTitle.addClassName(dropdownitem_res.css().jsdropdownactive());
                } else {
                    dropdownTitle.removeClassName(dropdownitem_res.css().jsdropdownactive());
                }
            }
        };
        presenter.forPlaceSelection(new PlaceCallback() {
            @Override
            public void forPlace(final AbstractEventPlace place, String title, boolean active) {
                DropdownItem dropdownItem = new DropdownItem(title, presenter.getUrl(place), active);
                dropdownItem.addDomHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        if (LinkUtil.handleLinkClick((Event) event.getNativeEvent())) {
                            event.preventDefault();
                            presenter.navigateTo(place);
                        }
                    }
                }, ClickEvent.getType());
                dropdownContent.add(dropdownItem);
            }
        });
    }

    private String withoutPrefix(String title, String... prefixes) {
        for (String prefix : prefixes) {
            if (title.startsWith(prefix)) {
                return title.substring(prefix.length(), title.length());
            }
        }
        return title;
    }

    private void hide(Element... elementsToHide) {
        for (Element element : elementsToHide) {
            element.removeFromParent();
        }
    }
}
