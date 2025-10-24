package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.ErrorReporter;

public class RegattaLeaderboardWithOtherTieBreakingLeaderboardCreateDialog extends RegattaLeaderboardWithOtherTieBreakingLeaderboardDialog {
    public RegattaLeaderboardWithOtherTieBreakingLeaderboardCreateDialog(
            Collection<StrippedLeaderboardDTO> existingLeaderboards, Collection<RegattaDTO> existingRegattas,
            StringMessages stringMessages, ErrorReporter errorReporter,
            DialogCallback<LeaderboardDescriptorWithOtherTieBreakingLeaderboard> callback) {
        super(stringMessages.createRegattaLeaderboardWithOtherTieBreakingLeaderboard(), new LeaderboardDescriptorWithOtherTieBreakingLeaderboard(),
                existingLeaderboards, existingRegattas, stringMessages, errorReporter, callback);
        displayNameTextBox = createTextBox(null);
        displayNameTextBox.ensureDebugId("DisplayNameTextBox");
        displayNameTextBox.setVisibleLength(50);
        regattaListBox = createSortedRegattaListBox(existingRegattas, null);
        regattaListBox.ensureDebugId("RegattaListBox");
        regattaListBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                int selectedIndex = regattaListBox.getSelectedIndex();
                if (selectedIndex > 0) {
                    nameTextBox.setText(regattaListBox.getValue(selectedIndex)); 
                    validateAndUpdate();
                }
                adjustVisibilityOfResultDiscardingRuleComponent();
            }
        });
        otherTieBreakingLeaderboardsListBox = createSortedRegattaLeaderboardsListBox(existingLeaderboards, null, stringMessages, this);
        otherTieBreakingLeaderboardsListBox.ensureDebugId("OtherTieBreakingLeaderboardsListBox");
        discardThresholdBoxes = new DiscardThresholdBoxes(this, stringMessages);
    }
}
