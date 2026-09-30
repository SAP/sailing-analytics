package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;

public class UpgradeApplicationReplicaSetDialog extends AbstractApplicationReplicaSetDialog<UpgradeApplicationReplicaSetDialog.UpgradeApplicationReplicaSetInstructions> {
    public static class UpgradeApplicationReplicaSetInstructions extends AbstractApplicationReplicaSetDialog.AbstractApplicationReplicaSetInstructions {
        public UpgradeApplicationReplicaSetInstructions(String releaseNameOrNullForLatestMaster, String masterReplicationBearerToken, String replicaReplicationBearerToken) {
            super(releaseNameOrNullForLatestMaster, masterReplicationBearerToken, replicaReplicationBearerToken);
        }
    }
    
    private final StringMessages stringMessages;

    public UpgradeApplicationReplicaSetDialog(LandscapeManagementWriteServiceAsync landscapeManagementService, Iterable<String> releaseNames,
            StringMessages stringMessages, ErrorReporter errorReporter, DialogCallback<UpgradeApplicationReplicaSetInstructions> callback) {
        super(stringMessages.upgradeApplicationReplicaSet(), landscapeManagementService, releaseNames, stringMessages, errorReporter, /* validator */ null, callback);
        this.stringMessages = stringMessages;
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(2, 2);
        int row=0;
        result.setWidget(row, 0, new Label(stringMessages.release()));
        result.setWidget(row++, 1, getReleaseNameBox());
        result.setWidget(row, 0, new Label(stringMessages.replicaReplicationBearerToken()));
        result.setWidget(row++, 1, getReplicaReplicationBearerTokenBox());
        return result;
    }

    @Override
    public FocusWidget getInitialFocusWidget() {
        return getReleaseNameBox().getValueBox();
    }
    
    @Override
    protected UpgradeApplicationReplicaSetInstructions getResult() {
        return new UpgradeApplicationReplicaSetInstructions(
                LandscapeDialogUtil.getReleaseNameBoxValue(getReleaseNameBox(), stringMessages),
                getMasterReplicationBearerTokenBox().getValue(), getReplicaReplicationBearerTokenBox().getValue());
    }
}
