package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;

public class LeaderboardGroupCreateDialog extends LeaderboardGroupDialog {

    public LeaderboardGroupCreateDialog(Collection<LeaderboardGroupDTO> existingGroups,
            StringMessages stringMessages, DialogCallback<LeaderboardGroupDescriptor> callback) {
        super(new LeaderboardGroupDTO(/* ID */ null, /* displayName */ null), stringMessages,
                callback, existingGroups);
        nameEntryField = createTextBox(null, 50);
        nameEntryField.ensureDebugId("NameTextBox");
        
        descriptionEntryField = createTextArea(null);
        descriptionEntryField.ensureDebugId("DescriptionTextArea");
        
        displayNameEntryField = createTextBox("");
        displayNameEntryField.ensureDebugId("DisplayNameTextArea");
        
        displayLeaderboardsInReverseOrderCheckBox.setValue(false);
        useOverallLeaderboardCheckBox.setValue(false);
    }
}
