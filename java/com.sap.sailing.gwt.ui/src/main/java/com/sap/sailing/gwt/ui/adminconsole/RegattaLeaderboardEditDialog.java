package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.google.gwt.user.client.ui.Focusable;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.ErrorReporter;

public class RegattaLeaderboardEditDialog<LD extends LeaderboardDescriptor> extends RegattaLeaderboardDialog<LD> {
    public RegattaLeaderboardEditDialog(Collection<StrippedLeaderboardDTO> otherExistingLeaderboards, Collection<RegattaDTO> existingRegattas,
            LD leaderboardDescriptor, StringMessages stringConstants, ErrorReporter errorReporter,
            DialogCallback<LD> callback) {
        super(stringConstants.editRegattaLeaderboard(), leaderboardDescriptor, existingRegattas, stringConstants, errorReporter,
                new RegattaLeaderboardDialog.LeaderboardParameterValidator<>(stringConstants, otherExistingLeaderboards), callback);
        displayNameTextBox = createTextBox(leaderboardDescriptor.getDisplayName());
        displayNameTextBox.setVisibleLength(50);
        regattaListBox = createSortedRegattaListBox(existingRegattas, leaderboardDescriptor.getRegattaName());
        regattaListBox.setEnabled(false);
        if (!getSelectedRegatta().definesSeriesDiscardThresholds()) {
            discardThresholdBoxes = new DiscardThresholdBoxes(this, leaderboardDescriptor.getDiscardThresholds(), stringMessages);
        } // else, the regatta leaderboard obtains its result discarding rule implicitly from the underlying regatta
        adjustVisibilityOfResultDiscardingRuleComponent();
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return displayNameTextBox;
    }
}
