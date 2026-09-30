package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public abstract class AbstractApplicationReplicaSetDialog<I extends AbstractApplicationReplicaSetDialog.AbstractApplicationReplicaSetInstructions> extends DataEntryDialog<I> {
    public static class AbstractApplicationReplicaSetInstructions {
        private final String masterReplicationBearerToken;
        private final String replicaReplicationBearerToken;
        private final String releaseNameOrNullForLatestMaster;
        
        public AbstractApplicationReplicaSetInstructions(String releaseNameOrNullForLatestMaster, String masterReplicationBearerToken, String replicaReplicationBearerToken) {
            super();
            this.masterReplicationBearerToken = masterReplicationBearerToken;
            this.replicaReplicationBearerToken = replicaReplicationBearerToken;
            this.releaseNameOrNullForLatestMaster = releaseNameOrNullForLatestMaster;
        }
        public String getReleaseNameOrNullForLatestMaster() {
            return releaseNameOrNullForLatestMaster;
        }
        public String getMasterReplicationBearerToken() {
            return masterReplicationBearerToken;
        }
        public String getReplicaReplicationBearerToken() {
            return replicaReplicationBearerToken;
        }
    }
    
    private final StringMessages stringMessages;
    private final SuggestBox releaseNameBox;
    private final TextBox masterReplicationBearerTokenBox;
    private final TextBox replicaReplicationBearerTokenBox;

    public AbstractApplicationReplicaSetDialog(String title, LandscapeManagementWriteServiceAsync landscapeManagementService,
            Iterable<String> releaseNames, StringMessages stringMessages, ErrorReporter errorReporter, Validator<I> validator, DialogCallback<I> callback) {
        super(title, /* message */ null, stringMessages.ok(), stringMessages.cancel(), validator, callback);
        this.stringMessages = stringMessages;
        releaseNameBox = LandscapeDialogUtil.createReleaseNameBox(stringMessages, releaseNames, this);
        masterReplicationBearerTokenBox = createTextBox("", 40);
        replicaReplicationBearerTokenBox = createTextBox("", 40);
    }
    
    protected StringMessages getStringMessages() {
        return stringMessages;
    }
    
    protected SuggestBox getReleaseNameBox() {
        return releaseNameBox;
    }
    
    protected TextBox getMasterReplicationBearerTokenBox() {
        return masterReplicationBearerTokenBox;
    }
    
    protected TextBox getReplicaReplicationBearerTokenBox() {
        return replicaReplicationBearerTokenBox;
    }
}
