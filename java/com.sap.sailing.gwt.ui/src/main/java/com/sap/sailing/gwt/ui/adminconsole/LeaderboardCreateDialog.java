package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.LongBox;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.LeaderboardDTO;

public class LeaderboardCreateDialog extends LeaderboardDialog{
    
    public LeaderboardCreateDialog(Collection<LeaderboardDTO> existingLeaderboards, StringMessages stringConstants,
            ErrorReporter errorReporter, AsyncCallback<LeaderboardDTO> callback) {
        super(new LeaderboardDTO(), stringConstants, errorReporter, new LeaderboardDialog.LeaderboardParameterValidator(stringConstants, existingLeaderboards), callback);

        entryField = createTextBox(null);

        discardThresholdBoxes = new LongBox[MAX_NUMBER_OF_DISCARDED_RESULTS];
        for (int i = 0; i < discardThresholdBoxes.length; i++) {
            discardThresholdBoxes[i] = createLongBoxWithOptionalValue(null, 2);
            discardThresholdBoxes[i].setVisibleLength(2);
        }
    }

}
