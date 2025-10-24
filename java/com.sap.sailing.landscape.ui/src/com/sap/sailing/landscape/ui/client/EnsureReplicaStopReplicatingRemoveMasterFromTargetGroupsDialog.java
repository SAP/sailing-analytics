package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class EnsureReplicaStopReplicatingRemoveMasterFromTargetGroupsDialog extends DataEntryDialog<String> {
    private final StringMessages stringMessages;
    private final TextBox replicaReplicationBearerTokenBox;

    public EnsureReplicaStopReplicatingRemoveMasterFromTargetGroupsDialog(
            StringMessages stringMessages, ErrorReporter errorReporter, DialogCallback<String> callback) {
        super(stringMessages.ensureAtLeastOneReplicaExistsStopReplicatingAndRemoveMasterFromTargetGroups(), /* message */ null,
                stringMessages.ok(), stringMessages.cancel(), /* validator */ null, callback);
        this.stringMessages = stringMessages;
        replicaReplicationBearerTokenBox = createTextBox("", 40);
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(1, 2);
        int row=0;
        result.setWidget(row, 0, new Label(stringMessages.replicaReplicationBearerToken()));
        result.setWidget(row++, 1, replicaReplicationBearerTokenBox);
        return result;
    }

    @Override
    public Focusable getInitialFocusWidget() {
        return replicaReplicationBearerTokenBox;
    }
    
    @Override
    protected String getResult() {
        return replicaReplicationBearerTokenBox.getValue();
    }
}
