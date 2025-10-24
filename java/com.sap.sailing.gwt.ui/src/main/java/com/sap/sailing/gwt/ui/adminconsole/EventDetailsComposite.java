package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.settings.client.EntryPointWithSettingsLinkFactory;
import com.sap.sailing.gwt.settings.client.regattaoverview.RegattaOverviewContextDefinition;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;

public class EventDetailsComposite extends Composite  {
    private EventDTO event;
    private final StringMessages stringMessages;
    private final Label eventId;
    private final Label eventName;
    private final Label venueName;
    private final Label description;
    private final Label startDate;
    private final Label endDate;
    private final Label isPublic;
    private final Anchor officialWebsiteURL;
    private final Anchor baseURL;
    private final SimpleAnchorListComposite sailorsInfoWebsiteURLList;
    private final Anchor eventOverviewURL;
    private final SimpleStringListComposite courseAreaNamesList;
    private final SimpleAnchorListComposite imageURLList;
    private final SimpleAnchorListComposite videoURLList;
    private final SimpleStringListComposite leaderboardGroupList;
    private final SimpleStringListComposite windfinderSpotCollectionsList;
    
    private final CaptionPanel mainPanel;

    public EventDetailsComposite(final SailingServiceAsync sailingService, final ErrorReporter errorReporter, final StringMessages stringMessages) {
        super();
        this.stringMessages = stringMessages;
        event = null;
        mainPanel = new CaptionPanel(stringMessages.regatta());
        VerticalPanel vPanel = new VerticalPanel();
        mainPanel.add(vPanel);
        int rows = 17;
        Grid grid = new Grid(rows, 2);
        vPanel.add(grid);
        int currentRow = 0;
        eventId = createLabelAndValueWidget(grid, currentRow++, stringMessages.id(), "IdLabel");
        eventName = createLabelAndValueWidget(grid, currentRow++, stringMessages.eventName(), "NameLabel");
        description = createLabelAndValueWidget(grid, currentRow++, stringMessages.description(), "DescriptionLabel");
        venueName = createLabelAndValueWidget(grid, currentRow++, stringMessages.venue(), "VenueLabel");
        startDate = createLabelAndValueWidget(grid, currentRow++, stringMessages.startDate(), "StartDateLabel");
        endDate = createLabelAndValueWidget(grid, currentRow++, stringMessages.endDate(), "EndDateLabel");
        isPublic = createLabelAndValueWidget(grid, currentRow++, stringMessages.isListedOnHomepage(), "IsPublicLabel");
        officialWebsiteURL = createLabelAndAnchorWidget(grid, currentRow++, stringMessages.eventOfficialWebsiteURL(), "OfficialWebsiteURLLabel");
        baseURL = createLabelAndAnchorWidget(grid, currentRow++, stringMessages.eventBaseURL(), "BaseURLLabel");
        sailorsInfoWebsiteURLList = createLabelAndAnchorListWidget(grid, currentRow++, stringMessages.eventSailorsInfoWebsiteURL(), "SailorsInfoWebsiteURLLabel");
        eventOverviewURL = createLabelAndAnchorWidget(grid, currentRow++, stringMessages.eventOverviewURL(), "EventOverviewURLLabel");
        courseAreaNamesList = createLabelAndValueListWidget(grid, currentRow++, stringMessages.courseAreas(), "CourseAreaValueList");
        imageURLList = createLabelAndAnchorListWidget(grid, currentRow++, stringMessages.images(), "ImageURLValueList");
        videoURLList = createLabelAndAnchorListWidget(grid, currentRow++, stringMessages.videos(), "VideoURLValueList");
        leaderboardGroupList = createLabelAndValueListWidget(grid, currentRow++, stringMessages.leaderboardGroups(), "LeaderboardGroupValueList");
        windfinderSpotCollectionsList = createLabelAndValueListWidget(grid, currentRow++, stringMessages.windFinderSpotCollectionsList(), "WindFinderSpotCollectionsList");
        for (int i = 0; i < rows; i++) {
            grid.getCellFormatter().setVerticalAlignment(i, 0, HasVerticalAlignment.ALIGN_TOP);
            grid.getCellFormatter().setVerticalAlignment(i, 1, HasVerticalAlignment.ALIGN_TOP);
        }
        initWidget(mainPanel);
    }
    
    private Label createLabelAndValueWidget(Grid grid, int row, String label, String debugId) {
        Label valueLabel = new Label();
        valueLabel.ensureDebugId(debugId);
        grid.setWidget(row , 0, new Label(label + ":"));
        grid.setWidget(row , 1, valueLabel);
        return valueLabel;
    }

    private Anchor createLabelAndAnchorWidget(Grid grid, int row, String label, String debugId) {
        Anchor valueAnchor = new Anchor();
        valueAnchor.setTarget("_blank");
        valueAnchor.ensureDebugId(debugId);
        grid.setWidget(row , 0, new Label(label + ":"));
        grid.setWidget(row , 1, valueAnchor);
        return valueAnchor;
    }

    private SimpleAnchorListComposite createLabelAndAnchorListWidget(Grid grid, int row, String label, String debugId) {
        SimpleAnchorListComposite valueList = new SimpleAnchorListComposite();
        valueList.ensureDebugId(debugId);
        grid.setWidget(row , 0, new Label(label + ":"));
        grid.setWidget(row , 1, valueList);
        return valueList;
    }

    private SimpleStringListComposite createLabelAndValueListWidget(Grid grid, int row, String label, String debugId) {
        SimpleStringListComposite valueList = new SimpleStringListComposite();
        valueList.ensureDebugId(debugId);
        grid.setWidget(row , 0, new Label(label + ":"));
        grid.setWidget(row , 1, valueList);
        return valueList;
    }

    public EventDTO getEvent() {
        return event;
    }

    public void setEvent(EventDTO event) {
        this.event = event;
        updateEventDetails();
    }

    private void updateEventDetails() {
        if (event != null) {
            mainPanel.setCaptionText(stringMessages.event() + " " + event.getName());
            eventName.setText(event.getName());
            eventId.setText(event.id.toString());
            venueName.setText(event.getVenue().getName());
            description.setText(event.getDescription());
            startDate.setText(event.startDate != null ? event.startDate.toString() : "");
            endDate.setText(event.endDate != null ? event.endDate.toString() : "");
            isPublic.setText(String.valueOf(event.isPublic));
            officialWebsiteURL.setText(event.getOfficialWebsiteURL());
            officialWebsiteURL.setHref(event.getOfficialWebsiteURL());
            baseURL.setText(event.getBaseURL());
            baseURL.setHref(event.getBaseURL());
            sailorsInfoWebsiteURLList.setValues(new ArrayList<String>(event.getSailorsInfoWebsiteURLs().values()));
            String regattaOverviewLink = EntryPointWithSettingsLinkFactory
                    .createRegattaOverviewLink(new RegattaOverviewContextDefinition(event.id));
            eventOverviewURL.setText(regattaOverviewLink);
            eventOverviewURL.setHref(UriUtils.fromString(regattaOverviewLink));
            List<String> courseAreaNames = new ArrayList<>();
            if (event.getVenue().getCourseAreas() != null && event.getVenue().getCourseAreas().size() > 0) {
                for (CourseAreaDTO courseArea : event.getVenue().getCourseAreas()) {
                    courseAreaNames.add(courseArea.getName());
                }
            }
            courseAreaNamesList.setValues(courseAreaNames);
            List<String> imageURLStringsAsList = new ArrayList<>();
            for (ImageDTO image: event.getImages()) {
                imageURLStringsAsList.add(image.getSourceRef());
            }
            imageURLList.setValues(imageURLStringsAsList);
            List<String> videoURLStringsAsList = new ArrayList<>();
            for( VideoDTO video: event.getVideos()) {
                videoURLStringsAsList.add(video.getSourceRef());
            }
            videoURLList.setValues(videoURLStringsAsList);
            List<String> leaderboardGroupNamesAsList = new ArrayList<>();
            for (LeaderboardGroupDTO leaderboardGroupDTO : event.getLeaderboardGroups()) {
                leaderboardGroupNamesAsList.add(leaderboardGroupDTO.getName());
            }
            leaderboardGroupList.setValues(leaderboardGroupNamesAsList);
            List<String> windfinderSpotCollectionsNamesAsList = new ArrayList<>();
            for (String windfinderSpotCollection : event.getWindFinderReviewedSpotsCollectionIds()) {
                windfinderSpotCollectionsNamesAsList.add(windfinderSpotCollection);
            }
            windfinderSpotCollectionsList.setValues(windfinderSpotCollectionsNamesAsList);
        }
    }

    private class SimpleStringListComposite extends Composite {
        private final VerticalPanel panel;
            
        public SimpleStringListComposite() {
            super();
            panel = new VerticalPanel();
            this.initWidget(panel);
        }
        
        public void setValues(List<String> values) {
            panel.clear();
            for(String value: values) {
                panel.add(new Label(value));
            }
        }
    }

    private class SimpleAnchorListComposite extends Composite {
        private final VerticalPanel panel;
            
        public SimpleAnchorListComposite() {
            super();
            panel = new VerticalPanel();
            this.initWidget(panel);
        }
        
        public void setValues(List<String> values) {
            panel.clear();
            for(String value: values) {
                panel.add(new Anchor(value, value, "_blank"));
            }
        }
    }
}
