package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;

public abstract class FlexibleLeaderboardDialog extends AbstractLeaderboardDialog<LeaderboardDescriptor> {
    protected ListBox scoringSchemeListBox;
    protected ListBox sailingEventsListBox;
    protected Collection<EventDTO> existingEvents;
    protected CourseAreaSelection courseAreaSelection;
    private final UserService userService;

    protected static class LeaderboardParameterValidator implements Validator<LeaderboardDescriptor> {
        protected final StringMessages stringMessages;
        protected final Collection<StrippedLeaderboardDTO> existingLeaderboards;

        public LeaderboardParameterValidator(StringMessages stringConstants, Collection<StrippedLeaderboardDTO> existingLeaderboards) {
            super();
            this.stringMessages = stringConstants;
            this.existingLeaderboards = existingLeaderboards;
        }

        @Override
        public String getErrorMessage(LeaderboardDescriptor leaderboardToValidate) {
            String errorMessage;
            boolean nonEmpty = leaderboardToValidate.getName() != null && leaderboardToValidate.getName().length() > 0;
            boolean unique = true;
            for (StrippedLeaderboardDTO dao : existingLeaderboards) {
                if(dao.getName().equals(leaderboardToValidate.getName())){
                    unique = false;
                }
            }
            if (!nonEmpty) {
                errorMessage = stringMessages.pleaseEnterAName();
            } else if(!unique){
                errorMessage = stringMessages.leaderboardWithThisNameAlreadyExists();
            } else {
                String discardThresholdErrorMessage = DiscardThresholdBoxes.getErrorMessage(leaderboardToValidate.getDiscardThresholds(), stringMessages);
                if (discardThresholdErrorMessage != null) {
                    errorMessage = discardThresholdErrorMessage;
                } else {
                    errorMessage = null;
                }
            }
            return errorMessage;
        }
    }

    public FlexibleLeaderboardDialog(String title, LeaderboardDescriptor leaderboardDTO, UserService userService, 
            StringMessages stringMessages,
            Collection<EventDTO> existingEvents, ErrorReporter errorReporter,  LeaderboardParameterValidator validator, DialogCallback<LeaderboardDescriptor> callback) {
        super(title, leaderboardDTO, stringMessages, validator, callback);
        this.userService = userService;
        this.existingEvents = existingEvents;
        courseAreaSelection = new CourseAreaSelection(stringMessages, this);
        courseAreaSelection.setEnabled(false);
    }

    @Override
    protected LeaderboardDescriptor getResult() {
        LeaderboardDescriptor leaderboard = super.getResult();
        leaderboard.setRegattaName(null);
        leaderboard.setScoringScheme(getSelectedScoringSchemeType(scoringSchemeListBox, stringMessages));
        setCourseAreaInDescriptor(leaderboard);
        return leaderboard;
    }

    private void setCourseAreaInDescriptor(LeaderboardDescriptor leaderboard) {
        leaderboard.setCourseAreaIds(Util.mapToArrayList(getSelectedCourseAreas(), CourseAreaDTO::getId));
    }

    @Override
    protected Widget getAdditionalWidget() {
        FlowPanel mainPanel = new FlowPanel();
        Grid formGrid = new Grid(5,3);
        formGrid.setCellSpacing(3);
        formGrid.setWidget(0,  0, createLabel(stringMessages.name()));
        formGrid.setWidget(0, 1, nameTextBox);
        formGrid.setWidget(1,  0, createLabel(stringMessages.displayName()));
        formGrid.setWidget(1, 1, displayNameTextBox);
        formGrid.setWidget(2, 0, createLabel(stringMessages.scoringSystem()));
        formGrid.setWidget(2, 1, scoringSchemeListBox);
        formGrid.setWidget(3, 0, new Label(stringMessages.event() + ":"));
        formGrid.setWidget(3, 1, sailingEventsListBox);
        formGrid.setWidget(4, 0, new Label(stringMessages.courseArea() + ":"));
        formGrid.setWidget(4, 1, courseAreaSelection);
        mainPanel.add(formGrid);
        mainPanel.add(discardThresholdBoxes.getWidget());

        return mainPanel;
    }

    protected ListBox createSailingEventListBox() {
        ListBox eventListBox = createListBox(false);
        eventListBox.addItem("Please select a sailing event...");
        for (EventDTO event : Util.filter(Util.sortNamedCollection(existingEvents), e->userService.hasPermission(e, DefaultActions.UPDATE))) {
            eventListBox.addItem(event.getName());
        }
        eventListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                onEventSelectionChanged();
            }
        });
        return eventListBox;
    }

    protected void onEventSelectionChanged() {
        EventDTO selectedEvent = getSelectedEvent();
        courseAreaSelection.removeAll();
        courseAreaSelection.setEnabled(false);
        if (selectedEvent != null) {
            fillCourseAreaListBox(selectedEvent);
        }
    }
    
    private void fillCourseAreaListBox(EventDTO selectedEvent) {
        for (CourseAreaDTO courseArea : selectedEvent.getVenue().getCourseAreas()) {
            courseAreaSelection.addCourseArea(courseArea);
        }
        courseAreaSelection.setEnabled(true);
    }

    public EventDTO getSelectedEvent() {
        EventDTO result = null;
        int selIndex = sailingEventsListBox.getSelectedIndex();
        if (selIndex > 0) { // the zero index represents the 'no selection' text
            String itemText = sailingEventsListBox.getItemText(selIndex);
            for (EventDTO eventDTO : existingEvents) {
                if (eventDTO.getName().equals(itemText)) {
                    result = eventDTO;
                    break;
                }
            }
        }
        return result;
    }

    public Iterable<CourseAreaDTO> getSelectedCourseAreas() {
        return courseAreaSelection.getSelectedCourseAreas();
    }
}
