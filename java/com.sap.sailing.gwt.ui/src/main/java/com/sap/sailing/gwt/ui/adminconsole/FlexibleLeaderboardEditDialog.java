package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.ui.Focusable;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.security.ui.client.UserService;

public class FlexibleLeaderboardEditDialog extends FlexibleLeaderboardDialog {
    public FlexibleLeaderboardEditDialog(Collection<StrippedLeaderboardDTO> otherExistingLeaderboards,
            LeaderboardDescriptor leaderboard, UserService userService, StringMessages stringMessages, List<EventDTO> existingEvents,
            ErrorReporter errorReporter, DialogCallback<LeaderboardDescriptor> callback) {
        super(stringMessages.editFlexibleLeaderboard(), leaderboard, userService, stringMessages, existingEvents,
                errorReporter, new FlexibleLeaderboardDialog.LeaderboardParameterValidator(stringMessages, otherExistingLeaderboards),
                callback);
        displayNameTextBox = createTextBox(leaderboard.getDisplayName());
        scoringSchemeListBox = createListBox(false);
        displayNameTextBox.setVisibleLength(50);
        int j = 0;
        for (ScoringSchemeType scoringSchemeType: ScoringSchemeType.values()) {
            scoringSchemeListBox.addItem(ScoringSchemeTypeFormatter.format(scoringSchemeType, stringMessages));
            if(leaderboard.getScoringScheme() == scoringSchemeType) {
                scoringSchemeListBox.setSelectedIndex(j);
            }
            j++;
        }
        scoringSchemeListBox.setEnabled(false);
        sailingEventsListBox = createSailingEventListBox();
        for (EventDTO event : existingEvents) {
            for (CourseAreaDTO courseArea : event.getVenue().getCourseAreas()) {
                if (Util.contains(leaderboard.getCourseAreaIds(), courseArea.getId())) {
                    int index = existingEvents.indexOf(event) + 1; // + 1 because of the "Please select... item"
                    sailingEventsListBox.setSelectedIndex(index);
                    onEventSelectionChanged();
                    courseAreaSelection.setSelectedSet(Util.map(leaderboard.getCourseAreaIds(),
                            id->Util.first(Util.filter(event.getVenue().getCourseAreas(), ca->ca.getId().equals(id)))));
                    break;
                }
            }
        }
        discardThresholdBoxes = new DiscardThresholdBoxes(this, leaderboard.getDiscardThresholds(), stringMessages);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return displayNameTextBox;
    }
}