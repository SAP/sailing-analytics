package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.security.ui.client.UserService;

public class FlexibleLeaderboardCreateDialog extends FlexibleLeaderboardDialog {

    public FlexibleLeaderboardCreateDialog(Collection<StrippedLeaderboardDTO> existingLeaderboards, UserService userService,
            StringMessages stringMessages, Collection<EventDTO> existingEvents, ErrorReporter errorReporter, DialogCallback<LeaderboardDescriptor> callback) {
        super(stringMessages.createFlexibleLeaderboard(), new LeaderboardDescriptor(), userService, stringMessages,
                existingEvents, errorReporter, new FlexibleLeaderboardDialog.LeaderboardParameterValidator(stringMessages, existingLeaderboards), callback);
        nameTextBox.setEnabled(true); // a name can be selected during FlexibleLeaderboard creation
        displayNameTextBox = createTextBox(null);
        displayNameTextBox.ensureDebugId("DisplayNameTextBox");
        displayNameTextBox.setVisibleLength(50);
        scoringSchemeListBox = createScoringSchemeListBox(this, stringMessages);
        sailingEventsListBox = createSailingEventListBox();
        discardThresholdBoxes = new DiscardThresholdBoxes(this, stringMessages);
    }
}
