package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;

/**
 * Allows the user to specify the parameters required for moving a replica set's master process to a different
 * instance.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MoveMasterProcessDialog extends AbstractNewProcessDialog<MoveMasterProcessDialog.MoveMasterToOtherInstanceInstructions> {
    
    public static class MoveMasterToOtherInstanceInstructions extends AbstractNewProcessDialog.NewProcessInstructions {
        private final boolean sharedMasterInstance;
        
        public MoveMasterToOtherInstanceInstructions(boolean sharedMasterInstance,
                String instanceTypeOrNull,
                String masterReplicationBearerToken, String replicaReplicationBearerToken, Integer optionalMemoryInMegabytesOrNull,
                Integer optionalMemoryTotalSizeFactorOrNull) {
            super(instanceTypeOrNull, masterReplicationBearerToken, replicaReplicationBearerToken, optionalMemoryInMegabytesOrNull, optionalMemoryTotalSizeFactorOrNull);
            this.sharedMasterInstance = sharedMasterInstance;
        }
        public boolean isSharedMasterInstance() {
            return sharedMasterInstance;
        }
    }
    
    private final CheckBox sharedMasterInstanceBox;
    private boolean memoryAsFactorToTotalMemoryAdjusted;

    public MoveMasterProcessDialog(LandscapeManagementWriteServiceAsync landscapeManagementService,
            StringMessages stringMessages, ErrorReporter errorReporter,
            DialogCallback<MoveMasterToOtherInstanceInstructions> callback) {
        super(stringMessages.moveMasterToOtherInstance(), SharedLandscapeConstants.DEFAULT_DEDICATED_INSTANCE_TYPE_NAME,
                landscapeManagementService, stringMessages, errorReporter, callback);
        getMemoryTotalSizeFactorBox().addValueChangeHandler(e->memoryAsFactorToTotalMemoryAdjusted=true);
        sharedMasterInstanceBox = createCheckbox(stringMessages.sharedMasterInstance());
        sharedMasterInstanceBox.addValueChangeHandler(e->updateInstanceTypeBasedOnSharedMasterInstanceBox());
        updateInstanceTypeBasedOnSharedMasterInstanceBox();
    }
    
    private void updateInstanceTypeBasedOnSharedMasterInstanceBox() {
        getInstanceTypeLabel().setText(sharedMasterInstanceBox.getValue() ? stringMessages.sharedMasterInstanceType() : stringMessages.dedicatedInstanceType());
        LandscapeDialogUtil.selectInstanceType(getInstanceTypeListBox(),
                sharedMasterInstanceBox.getValue() ? SharedLandscapeConstants.DEFAULT_SHARED_INSTANCE_TYPE_NAME : SharedLandscapeConstants.DEFAULT_DEDICATED_INSTANCE_TYPE_NAME);
        if (!memoryAsFactorToTotalMemoryAdjusted) {
            if (sharedMasterInstanceBox.getValue()) {
                getMemoryTotalSizeFactorBox().setValue(SharedLandscapeConstants.DEFAULT_NUMBER_OF_PROCESSES_IN_MEMORY);
            } else {
                getMemoryTotalSizeFactorBox().setText("");
            }
        }
    }

    @Override
    protected Grid getAdditionalWidget() {
        final Grid result = super.getAdditionalWidget();
        int row=0;
        result.insertRow(row);
        result.setWidget(row, 0, new Label(stringMessages.sharedMasterInstance()));
        result.setWidget(row++, 1, sharedMasterInstanceBox);
        return result;
    }

    @Override
    protected MoveMasterToOtherInstanceInstructions getResult() {
        return new MoveMasterToOtherInstanceInstructions(sharedMasterInstanceBox.getValue(),
                getInstanceTypeListBox().getSelectedValue(),
                getMasterReplicationBearerTokenBox().getValue(), getReplicaReplicationBearerTokenBox().getValue(),
                getMemoryInMegabytesBox().getValue(), getMemoryTotalSizeFactorBox().getValue());
    }
}
