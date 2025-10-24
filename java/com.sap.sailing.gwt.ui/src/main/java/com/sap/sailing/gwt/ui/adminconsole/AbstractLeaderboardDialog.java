package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public abstract class AbstractLeaderboardDialog<LD extends LeaderboardDescriptor> extends DataEntryDialog<LD> {
    protected final StringMessages stringMessages;
    protected TextBox nameTextBox;
    protected TextBox displayNameTextBox;
    protected LD leaderboardDescriptor;

    protected DiscardThresholdBoxes discardThresholdBoxes;

    public AbstractLeaderboardDialog(String title, LD leaderboardDescriptor, StringMessages stringMessages,
            Validator<LD> validator, DialogCallback<LD> callback) {
        super(title, null, stringMessages.ok(), stringMessages.cancel(), validator, callback);
        nameTextBox = createTextBox(leaderboardDescriptor.getName());
        nameTextBox.setVisibleLength(50);
        nameTextBox.ensureDebugId("NameTextBox");
        nameTextBox.setEnabled(false); // name is not editable; see also bug5282
        this.stringMessages = stringMessages;
        this.leaderboardDescriptor = leaderboardDescriptor;
    }

    @Override
    protected LD getResult() {
        // setting the name is relevant only for the FlexibleLeaderboard and RegattaLeaderboardWithElimination creation:
        leaderboardDescriptor.setName(nameTextBox.getValue().trim()); // avoid trailing blank issues; leaderboard names may appear in URLs
        leaderboardDescriptor.setDisplayName(displayNameTextBox.getValue().trim().isEmpty() ? null : displayNameTextBox.getValue());
        leaderboardDescriptor.setDiscardThresholds(discardThresholdBoxes==null?null:discardThresholdBoxes.getDiscardThresholds());
        return leaderboardDescriptor;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return nameTextBox;
    }

    protected static ListBox createScoringSchemeListBox(DataEntryDialog<?> dialog, StringMessages stringMessages) {
        ListBox scoringSchemeListBox2 = dialog.createListBox(false);
        for (ScoringSchemeType scoringSchemeType: ScoringSchemeType.values()) {
            scoringSchemeListBox2.addItem(ScoringSchemeTypeFormatter.format(scoringSchemeType, stringMessages));
        }
        return scoringSchemeListBox2;
    }

    protected static ScoringSchemeType getSelectedScoringSchemeType(ListBox scoringSchemeListBox, StringMessages stringMessages) {
        ScoringSchemeType result = null;
        int selIndex = scoringSchemeListBox.getSelectedIndex();
        if (selIndex >= 0) { 
            String itemText = scoringSchemeListBox.getItemText(selIndex);
            for (ScoringSchemeType scoringSchemeType : ScoringSchemeType.values()) {
                if (ScoringSchemeTypeFormatter.format(scoringSchemeType, stringMessages).equals(itemText)) {
                    result = scoringSchemeType;
                    break;
                }
            }
        }
        return result;
    }
    
    protected static <T> ListBox createSortedRegattaLeaderboardsListBox(Collection<StrippedLeaderboardDTO> existingLeaderboards, String preSelectedRegattaName, StringMessages stringMessages, DataEntryDialog<T> dialog) {
        ListBox result = dialog.createListBox(false);
        // sort the regatta names
        List<StrippedLeaderboardDTO> sortedRegattaLeaderboards = new ArrayList<>();
        for (StrippedLeaderboardDTO leaderboard : existingLeaderboards) {
            sortedRegattaLeaderboards.add(leaderboard);
        }
        Collections.sort(sortedRegattaLeaderboards, (rl1, rl2) -> rl1.getName().compareTo(rl2.getName()));
        result.addItem(stringMessages.pleaseSelectARegatta());
        int i=1;
        for (StrippedLeaderboardDTO leaderboard : sortedRegattaLeaderboards) {
            if (leaderboard.type.isRegattaLeaderboard()) {
                result.addItem(leaderboard.getName(), leaderboard.getName());
                if (preSelectedRegattaName != null && leaderboard.getName().equals(preSelectedRegattaName)) {
                    result.setSelectedIndex(i);
                }
                i++;
            }
        }
        return result;
    }
}
