package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Allows the user to specify the parameters required for moving a replica set's master process to a different
 * instance.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class AbstractNewProcessDialog<T> extends DataEntryDialog<T> {
    public static class NewProcessInstructions  {
        private final String instanceTypeOrNull;
        private final Integer optionalMemoryInMegabytesOrNull;
        private final Integer optionalMemoryTotalSizeFactorOrNull;
        private final String masterReplicationBearerToken;
        private final String replicaReplicationBearerToken;
        
        public NewProcessInstructions(String instanceTypeOrNull,
                String masterReplicationBearerToken, String replicaReplicationBearerToken, Integer optionalMemoryInMegabytesOrNull,
                Integer optionalMemoryTotalSizeFactorOrNull) {
            this.instanceTypeOrNull = instanceTypeOrNull;
            this.optionalMemoryInMegabytesOrNull = optionalMemoryInMegabytesOrNull;
            this.optionalMemoryTotalSizeFactorOrNull = optionalMemoryTotalSizeFactorOrNull;
            this.masterReplicationBearerToken = masterReplicationBearerToken;
            this.replicaReplicationBearerToken = replicaReplicationBearerToken;
        }
        public String getDedicatedInstanceType() {
            return instanceTypeOrNull;
        }
        public Integer getOptionalMemoryInMegabytesOrNull() {
            return optionalMemoryInMegabytesOrNull;
        }
        public Integer getOptionalMemoryTotalSizeFactorOrNull() {
            return optionalMemoryTotalSizeFactorOrNull;
        }
        public String getInstanceTypeOrNull() {
            return instanceTypeOrNull;
        }
        public String getMasterReplicationBearerToken() {
            return masterReplicationBearerToken;
        }
        public String getReplicaReplicationBearerToken() {
            return replicaReplicationBearerToken;
        }
    }
    
    protected final StringMessages stringMessages;
    private final ListBox instanceTypeListBox;
    private final Label instanceTypeLabel;
    private final TextBox masterReplicationBearerTokenBox;
    private final TextBox replicaReplicationBearerTokenBox;
    private final IntegerBox memoryInMegabytesBox;
    private final IntegerBox memoryTotalSizeFactorBox;

    public AbstractNewProcessDialog(String title, String defaultInstanceTypeName,
            LandscapeManagementWriteServiceAsync landscapeManagementService, StringMessages stringMessages,
            ErrorReporter errorReporter, DialogCallback<T> callback) {
        super(title, /* message */ null, stringMessages.ok(), stringMessages.cancel(), /* validator */ null, callback);
        this.stringMessages = stringMessages;
        instanceTypeListBox = LandscapeDialogUtil.createInstanceTypeListBox(this, landscapeManagementService,
                stringMessages, defaultInstanceTypeName, errorReporter, /* canBeDeployedInNlbInstanceBasedTargetGroup */ false);
        instanceTypeLabel = new Label();
        masterReplicationBearerTokenBox = createTextBox("", 40);
        replicaReplicationBearerTokenBox = createTextBox("", 40);
        memoryInMegabytesBox = createIntegerBox(null, 7);
        memoryTotalSizeFactorBox = createIntegerBox(null, 2);
        getMemoryInMegabytesBox().addValueChangeHandler(e->getMemoryTotalSizeFactorBox().setEnabled(e.getValue() == null));
    }
    
    @Override
    protected Grid getAdditionalWidget() {
        final Grid result = new Grid(5, 2);
        int row=0;
        result.setWidget(row, 0, getInstanceTypeLabel());
        result.setWidget(row++, 1, getInstanceTypeListBox());
        result.setWidget(row, 0, new Label(stringMessages.bearerTokenForSecurityReplication()));
        result.setWidget(row++, 1, getMasterReplicationBearerTokenBox());
        result.setWidget(row, 0, new Label(stringMessages.replicaReplicationBearerToken()));
        result.setWidget(row++, 1, getReplicaReplicationBearerTokenBox());
        result.setWidget(row, 0, new Label(stringMessages.memoryInMegabytes()));
        result.setWidget(row++, 1, getMemoryInMegabytesBox());
        result.setWidget(row, 0, new Label(stringMessages.memoryTotalSizeFactor()));
        result.setWidget(row++, 1, getMemoryTotalSizeFactorBox());
        return result;
    }

    protected IntegerBox getMemoryTotalSizeFactorBox() {
        return memoryTotalSizeFactorBox;
    }

    protected IntegerBox getMemoryInMegabytesBox() {
        return memoryInMegabytesBox;
    }

    protected TextBox getReplicaReplicationBearerTokenBox() {
        return replicaReplicationBearerTokenBox;
    }

    protected TextBox getMasterReplicationBearerTokenBox() {
        return masterReplicationBearerTokenBox;
    }

    protected Label getInstanceTypeLabel() {
        return instanceTypeLabel;
    }

    protected ListBox getInstanceTypeListBox() {
        return instanceTypeListBox;
    }
}
