package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public abstract class AbstractPairingListCreationSetupDialog<T> extends DataEntryDialog<T> {
    
    protected StringMessages stringMessages;
    protected final StrippedLeaderboardDTO leaderboardDTO;
    
    public AbstractPairingListCreationSetupDialog(StrippedLeaderboardDTO leaderboardDTO, String title, StringMessages stringMessages,
            Validator<T> validator, DialogCallback<T> callback) {
        super(title, null, stringMessages.ok(), stringMessages.cancel(), validator, callback);
        this.stringMessages = stringMessages;
        this.leaderboardDTO = leaderboardDTO;
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        
        return panel;
    }

    @Override
    protected T getResult() {
        return null;
    }
}
