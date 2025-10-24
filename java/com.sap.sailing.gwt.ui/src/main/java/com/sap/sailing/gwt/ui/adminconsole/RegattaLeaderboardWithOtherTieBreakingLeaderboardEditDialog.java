package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.ErrorReporter;

public class RegattaLeaderboardWithOtherTieBreakingLeaderboardEditDialog extends RegattaLeaderboardWithOtherTieBreakingLeaderboardDialog {
    public RegattaLeaderboardWithOtherTieBreakingLeaderboardEditDialog(Collection<StrippedLeaderboardDTO> otherExistingLeaderboards, Collection<RegattaDTO> existingRegattas,
            LeaderboardDescriptorWithOtherTieBreakingLeaderboard leaderboardDescriptor, StringMessages stringMessages, ErrorReporter errorReporter,
            DialogCallback<LeaderboardDescriptorWithOtherTieBreakingLeaderboard> callback) {
        super(stringMessages.editRegattaLeaderboardWithOtherTieBreakingLeaderboard(), leaderboardDescriptor, otherExistingLeaderboards, existingRegattas, stringMessages, errorReporter, callback);
        displayNameTextBox = createTextBox(leaderboardDescriptor.getDisplayName());
        displayNameTextBox.setVisibleLength(50);
        regattaListBox = createSortedRegattaListBox(existingRegattas, leaderboardDescriptor.getRegattaName());
        regattaListBox.setEnabled(false);
        otherTieBreakingLeaderboardsListBox = createSortedRegattaLeaderboardsListBox(otherExistingLeaderboards, leaderboardDescriptor.getOtherTieBreakingLeaderboardName(), stringMessages, this);
        otherTieBreakingLeaderboardsListBox.ensureDebugId("OtherTieBreakingLeaderboardsListBox");
        otherTieBreakingLeaderboardsListBox.setEnabled(false);
        if (!getSelectedRegatta().definesSeriesDiscardThresholds()) {
            discardThresholdBoxes = new DiscardThresholdBoxes(this, leaderboardDescriptor.getDiscardThresholds(), stringMessages);
        } // else, the regatta leaderboard obtains its result discarding rule implicitly from the underlying regatta
        adjustVisibilityOfResultDiscardingRuleComponent();
    }
}
