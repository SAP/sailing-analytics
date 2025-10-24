package com.sap.sailing.landscape.ui.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Allows the user to specify the parameters required for launching an application replica set. It produces an output in
 * the form of an {@link CreateApplicationReplicaSetInstructions} object (a static inner class) which can then be used
 * by a caller to parameterize subsequent calls to the landscape management service.
 * <p>
 * 
 * The dialog can itself be parameterized to launch different scenarios:
 * <ul>
 * <li>master on dedicated or pre-selected/arbitrary shared instance</li>
 * <li>initial replica provided through auto-scaling group on dedicated instance, or on a shared instance</li>
 * <li>dynamic load balancing may or may not be allowed (usually depending on the region)</li>
 * </ul>
 * 
 * Different instance type choices and defaulting rules are required in the different scenarios:
 * <ul>
 * <li>When a new master is to be launched (dedicated or shared), the master instance type must be specified. A
 * different default is suggested if we know the user wants to launch a shared master instance.</li>
 * <li>For the auto-scaling group that creates dedicated replica instances an instance type must always be specified. If
 * a dedicated master instance is requested, its instance type is a good default for the type of instances launched by
 * the auto-scaling group. In case of a shared master instance, however, a default instance type for dedicated instances
 * will be suggested for the auto-scaling group's instance type.</li>
 * <li>If a first replica is requested to run on a shared instance, that instance may need to be launched in case no
 * eligible instance can be found in an availability zone different from the master instance's AZ with the necessary
 * available port(s). To be prepared for this case, the instance type must be specified if a first replica is desired
 * to run on a shared / sharable instance. If the master is also to run on a shared instance, the instance type selected
 * for the new shared master instance serves as a default for launching a shared replica instance. If the master is
 * configured to run on a dedicated instance, its instance type is not a good default for a shared replica instance, but it
 * is for the auto-scaling group's replica instances. Instead, the user then needs to specify the shared instance type
 * to use in case a shared replica instance needs to be launched.</li>
 * </ul>
 * We can boil this down to two instance types: the one for shared and the one for dedicated instances. The instance
 * type for dedicated instances is always required for the auto-scaling group configuration. The one for shared
 * instances, however, is required only if the master or a first replica shall run on a shared instance. If the
 * master shall run on a dedicated instance and the replicas shall be managed entirely by the auto-scaling group,
 * no specification for a shared instance type is required.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CreateApplicationReplicaSetDialog extends AbstractApplicationReplicaSetDialog<CreateApplicationReplicaSetDialog.CreateApplicationReplicaSetInstructions> {
    
    public static class CreateApplicationReplicaSetInstructions extends AbstractApplicationReplicaSetDialog.AbstractApplicationReplicaSetInstructions {
        private final String name;
        private final boolean sharedMasterInstance;
        private final String dedicatedInstanceType;
        private final String optionalSharedInstanceType;
        private final boolean dynamicLoadBalancerMapping;
        private final String optionalDomainName;
        private final Integer optionalMemoryInMegabytesOrNull;
        private final Integer optionalMemoryTotalSizeFactorOrNull;
        private final Integer optionalIgtimiRiotPort;
        private final boolean firstReplicaOnSharedInstance;
        
        public CreateApplicationReplicaSetInstructions(String name, boolean sharedMasterInstance, String dedicatedInstanceType,
                String optionalSharedInstanceType, String releaseNameOrNullForLatestMaster,
                boolean dynamicLoadBalancerMapping, String masterReplicationBearerToken,
                String replicaReplicationBearerToken, String optionalDomainName,
                Integer optionalMemoryInMegabytesOrNull, Integer optionalMemoryTotalSizeFactorOrNull,
                Integer optionalIgtimiRiotPort, boolean firstReplicaOnSharedInstance) {
            super(releaseNameOrNullForLatestMaster, masterReplicationBearerToken, replicaReplicationBearerToken);
            this.name = name;
            this.sharedMasterInstance = sharedMasterInstance;
            this.dynamicLoadBalancerMapping = dynamicLoadBalancerMapping;
            this.optionalDomainName = Util.hasLength(optionalDomainName) ? optionalDomainName : null;
            this.dedicatedInstanceType = dedicatedInstanceType;
            this.optionalSharedInstanceType = optionalSharedInstanceType;
            this.optionalMemoryInMegabytesOrNull = optionalMemoryInMegabytesOrNull;
            this.optionalMemoryTotalSizeFactorOrNull = optionalMemoryTotalSizeFactorOrNull;
            this.firstReplicaOnSharedInstance = firstReplicaOnSharedInstance;
            this.optionalIgtimiRiotPort = optionalIgtimiRiotPort;
        }
        public String getName() {
            return name;
        }
        public boolean isSharedMasterInstance() {
            return sharedMasterInstance;
        }
        public boolean isDynamicLoadBalancerMapping() {
            return dynamicLoadBalancerMapping;
        }
        public String getOptionalDomainName() {
            return optionalDomainName;
        }
        public String getDedicatedInstanceType() {
            return dedicatedInstanceType;
        }
        public String getOptionalSharedInstanceType() {
            return optionalSharedInstanceType;
        }
        public Integer getOptionalMemoryInMegabytesOrNull() {
            return optionalMemoryInMegabytesOrNull;
        }
        public Integer getOptionalMemoryTotalSizeFactorOrNull() {
            return optionalMemoryTotalSizeFactorOrNull;
        }
        public Integer getOptionalIgtimiRiotPort() {
            return optionalIgtimiRiotPort;
        }
        public boolean isFirstReplicaOnSharedInstance() {
            return firstReplicaOnSharedInstance;
        }
    }
    
    private static class Validator implements DataEntryDialog.Validator<CreateApplicationReplicaSetInstructions> {
        private final StringMessages stringMessages;
        private final LandscapeManagementWriteServiceAsync landscapeManagementService;
        
        public Validator(StringMessages stringMessages, LandscapeManagementWriteServiceAsync landscapeManagementService) {
            this.stringMessages = stringMessages;
            this.landscapeManagementService = landscapeManagementService;
        }

        @Override
        public void validate(CreateApplicationReplicaSetInstructions valueToValidate, AsyncCallback<String> callback,
                AsyncActionsExecutor validationExecutor) {
            // TODO Auto-generated method stub
            final String localErrorMessage = getErrorMessage(valueToValidate);
            if (localErrorMessage != null) {
                validationExecutor.execute(cb->cb.onSuccess(localErrorMessage), VALIDATION_ACTION_CATEGORY, callback);
            } else {
                // check availability of DNS name remotely:
                landscapeManagementService.hasDNSResourceRecordsForReplicaSet(valueToValidate.getName(), valueToValidate.getOptionalDomainName(),
                        new AsyncCallback<Boolean>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                callback.onFailure(caught);
                            }

                            @Override
                            public void onSuccess(Boolean hasDNSResourceRecordsForReplicaSet) {
                                if (hasDNSResourceRecordsForReplicaSet) {
                                    callback.onSuccess(stringMessages.dnsNameAlreadyInUse());
                                } else {
                                    callback.onSuccess(null); // no error
                                }
                            }
                });
            }
        }

        @Override
        public String getErrorMessage(CreateApplicationReplicaSetInstructions valueToValidate) {
            final String result;
            if (!Util.hasLength(valueToValidate.getDedicatedInstanceType())) {
                result = stringMessages.pleaseSelectInstanceTypeForNewMaster();
            } else if (!Util.hasLength(valueToValidate.getName())) {
                result = stringMessages.pleaseProvideApplicationReplicaSetName();
            } else {
                result = null;
            }
            return result;
        }
    }

    private final StringMessages stringMessages;
    private final boolean useExistingSharedMasterInstance;
    private final TextBox nameBox;
    private final CheckBox sharedMasterInstanceBox;
    private final CheckBox startWithReplicaOnSharedInstanceBox;
    private final ListBox dedicatedInstanceTypeListBox;
    private final Label dedicatedInstanceTypeLabel;
    private final ListBox sharedInstanceTypeListBox;
    private final Label sharedInstanceTypeLabel;
    private final CheckBox dynamicLoadBalancerCheckBox;
    private final TextBox domainNameBox;
    private final IntegerBox memoryInMegabytesBox;
    private final IntegerBox memoryTotalSizeFactorBox;
    private final IntegerBox igtimiRiotPortBox;
    private boolean memoryAsFactorToTotalMemoryAdjusted;

    public CreateApplicationReplicaSetDialog(LandscapeManagementWriteServiceAsync landscapeManagementService,
            boolean useExistingSharedMasterInstance, Iterable<String> releaseNames,
            StringMessages stringMessages, ErrorReporter errorReporter, DialogCallback<CreateApplicationReplicaSetInstructions> callback,
            boolean mayUseDynamicLoadBalancer) {
        super(stringMessages
                .createApplicationReplicaSet(), landscapeManagementService, releaseNames, stringMessages,
                errorReporter, new Validator(stringMessages, landscapeManagementService), callback);
        this.stringMessages = stringMessages;
        this.useExistingSharedMasterInstance = useExistingSharedMasterInstance;
        nameBox = createTextBox("", 40);
        dynamicLoadBalancerCheckBox = mayUseDynamicLoadBalancer ? createCheckbox(stringMessages.useDynamicLoadBalancer()) : null;
        domainNameBox = createTextBox(SharedLandscapeConstants.DEFAULT_DOMAIN_NAME, 40);
        dedicatedInstanceTypeListBox = LandscapeDialogUtil.createInstanceTypeListBox(this, landscapeManagementService,
                stringMessages, SharedLandscapeConstants.DEFAULT_DEDICATED_INSTANCE_TYPE_NAME, errorReporter, /* canBeDeployedInNlbInstanceBasedTargetGroup */ false);
        dedicatedInstanceTypeLabel = new Label();
        sharedInstanceTypeListBox = LandscapeDialogUtil.createInstanceTypeListBox(this, landscapeManagementService,
                stringMessages, SharedLandscapeConstants.DEFAULT_SHARED_INSTANCE_TYPE_NAME, errorReporter, /* canBeDeployedInNlbInstanceBasedTargetGroup */ false);
        sharedInstanceTypeLabel = new Label();
        memoryInMegabytesBox = createIntegerBox(null, 7);
        memoryTotalSizeFactorBox = createIntegerBox(null, 2);
        memoryTotalSizeFactorBox.addValueChangeHandler(e->memoryAsFactorToTotalMemoryAdjusted=true);
        if (useExistingSharedMasterInstance) {
            memoryTotalSizeFactorBox.setValue(SharedLandscapeConstants.DEFAULT_NUMBER_OF_PROCESSES_IN_MEMORY);
        }
        memoryInMegabytesBox.addValueChangeHandler(e->memoryTotalSizeFactorBox.setEnabled(e.getValue() == null));
        igtimiRiotPortBox = createIntegerBox(null, 10);
        igtimiRiotPortBox.getElement().setAttribute("placeholder", stringMessages.examplePort(SharedLandscapeConstants.IGTIMI_DEFAULT_RIOT_PORT));
        startWithReplicaOnSharedInstanceBox = createCheckbox(stringMessages.firstReplicaOnSharedInstance());
        startWithReplicaOnSharedInstanceBox.addValueChangeHandler(e->updateInstanceTypesBasedOnSharedMasterInstanceBox());
        startWithReplicaOnSharedInstanceBox.setValue(useExistingSharedMasterInstance);
        sharedMasterInstanceBox = createCheckbox(stringMessages.sharedMasterInstance());
        sharedMasterInstanceBox.addValueChangeHandler(e->updateInstanceTypesBasedOnSharedMasterInstanceBox());
        sharedMasterInstanceBox.setValue(useExistingSharedMasterInstance);
        sharedMasterInstanceBox.setEnabled(!useExistingSharedMasterInstance);
        updateInstanceTypesBasedOnSharedMasterInstanceBox();
        validateAndUpdate(); // recognize the effects of setting the default values in some entry fields
    }
    
    private void updateInstanceTypesBasedOnSharedMasterInstanceBox() {
        dedicatedInstanceTypeLabel.setText(sharedMasterInstanceBox.getValue()
                ? stringMessages.autoScalingReplicaInstanceType()
                : stringMessages.dedicatedMasterAndAutoScalingReplicaInstanceType());
        if (sharedMasterInstanceBox.getValue()) {
            if (startWithReplicaOnSharedInstanceBox.getValue()) {
                setVisibilityOfSharedInstanceTypeSelection(true);
                sharedInstanceTypeLabel.setText(useExistingSharedMasterInstance
                        ? stringMessages.sharedReplicaInstanceType()
                        : stringMessages.sharedMasterAndReplicaInstanceType());
            } else {
                setVisibilityOfSharedInstanceTypeSelection(!useExistingSharedMasterInstance);
                sharedInstanceTypeLabel.setText(stringMessages.sharedMasterInstanceType());
            }
        } else {
            if (startWithReplicaOnSharedInstanceBox.getValue()) {
                setVisibilityOfSharedInstanceTypeSelection(true);
                sharedInstanceTypeLabel.setText(stringMessages.sharedReplicaInstanceType());
            } else {
                setVisibilityOfSharedInstanceTypeSelection(false);
            }
        }
        if (!memoryAsFactorToTotalMemoryAdjusted) {
            if (sharedMasterInstanceBox.getValue() || startWithReplicaOnSharedInstanceBox.getValue()) {
                memoryTotalSizeFactorBox.setValue(SharedLandscapeConstants.DEFAULT_NUMBER_OF_PROCESSES_IN_MEMORY);
            } else {
                memoryTotalSizeFactorBox.setText("");
            }
        }
    }

    private void setVisibilityOfSharedInstanceTypeSelection(boolean show) {
        sharedInstanceTypeLabel.setVisible(show);
        sharedInstanceTypeListBox.setVisible(show);
    }

    protected ListBox getDedicatedInstanceTypeListBox() {
        return dedicatedInstanceTypeListBox;
    }

    protected ListBox getSharedInstanceTypeListBox() {
        return sharedInstanceTypeListBox;
    }
    
    protected CheckBox getStartWithReplicaOnSharedInstanceBox() {
        return startWithReplicaOnSharedInstanceBox;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(13, 2);
        int row=0;
        result.setWidget(row, 0, new Label(stringMessages.name()));
        result.setWidget(row++, 1, nameBox);
        result.setWidget(row, 0, new Label(stringMessages.release()));
        result.setWidget(row++, 1, getReleaseNameBox());
        if (!useExistingSharedMasterInstance) {
            result.setWidget(row, 0, new Label(stringMessages.sharedMasterInstance()));
            result.setWidget(row++, 1, sharedMasterInstanceBox);
        }
        result.setWidget(row, 0, new Label(stringMessages.firstReplicaOnSharedInstance()));
        result.setWidget(row++, 1, getStartWithReplicaOnSharedInstanceBox());
        result.setWidget(row, 0, dedicatedInstanceTypeLabel);
        result.setWidget(row++, 1, getDedicatedInstanceTypeListBox());
        result.setWidget(row, 0, sharedInstanceTypeLabel);
        result.setWidget(row++, 1, getSharedInstanceTypeListBox());
        if (dynamicLoadBalancerCheckBox != null) {
            result.setWidget(row, 0, new Label(stringMessages.useDynamicLoadBalancer()));
            result.setWidget(row++, 1, dynamicLoadBalancerCheckBox);
        }
        result.setWidget(row, 0, new Label(stringMessages.bearerTokenForSecurityReplication()));
        result.setWidget(row++, 1, getMasterReplicationBearerTokenBox());
        result.setWidget(row, 0, new Label(stringMessages.replicaReplicationBearerToken()));
        result.setWidget(row++, 1, getReplicaReplicationBearerTokenBox());
        result.setWidget(row, 0, new Label(stringMessages.domainName()));
        result.setWidget(row++, 1, domainNameBox);
        result.setWidget(row, 0, new Label(stringMessages.memoryInMegabytes()));
        result.setWidget(row++, 1, memoryInMegabytesBox);
        result.setWidget(row, 0, new Label(stringMessages.memoryTotalSizeFactor()));
        result.setWidget(row++, 1, memoryTotalSizeFactorBox);
        result.setWidget(row, 0, new Label(stringMessages.igtimiRiotPort()));
        result.setWidget(row++, 1, igtimiRiotPortBox);
        return result;
    }

    @Override
    public FocusWidget getInitialFocusWidget() {
        return nameBox;
    }
    
    @Override
    protected CreateApplicationReplicaSetInstructions getResult() {
        return new CreateApplicationReplicaSetInstructions(nameBox.getValue(), sharedMasterInstanceBox.getValue(),
                getDedicatedInstanceTypeListBox().getSelectedValue(),
                getSharedInstanceTypeListBox().getSelectedValue(),
                getReleaseNameBoxValue(), dynamicLoadBalancerCheckBox==null?false:dynamicLoadBalancerCheckBox.getValue(),
                getMasterReplicationBearerTokenBox().getValue(), getReplicaReplicationBearerTokenBox().getValue(),
                domainNameBox.getValue(), memoryInMegabytesBox.getValue(), memoryTotalSizeFactorBox.getValue(),
                igtimiRiotPortBox.getValue(), startWithReplicaOnSharedInstanceBox.getValue());
    }
}
