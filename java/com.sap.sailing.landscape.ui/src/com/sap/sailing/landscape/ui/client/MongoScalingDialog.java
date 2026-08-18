package com.sap.sailing.landscape.ui.client;

import java.util.Arrays;
import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sailing.landscape.ui.shared.MongoEndpointDTO;
import com.sap.sailing.landscape.ui.shared.MongoLaunchParametersDTO;
import com.sap.sailing.landscape.ui.shared.MongoProcessDTO;
import com.sap.sailing.landscape.ui.shared.MongoScalingInstructionsDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.TableWrapperWithMultiSelectionAndFilter;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.landscape.common.shared.MongoDBConstants;

public class MongoScalingDialog extends DataEntryDialog<MongoScalingInstructionsDTO> {
    private final VerticalPanel panel;
    private final TableWrapperWithMultiSelectionAndFilter<MongoProcessDTO, StringMessages, AdminConsoleTableResources> tableForChoosingInstancesToShutDown;
    private final IntegerBox numberOfInstancesToLaunchBox;
    private final IntegerBox voteBox;
    private final IntegerBox priorityBox;
    private final ListBox instanceTypeBox;
    private final String replicaSetPrimary;
    private final String replicaSetName;
    private final String DEFAULT_INSTANCE_TYPE = "I4_I_2_XLARGE";
    
    private static class MongoScalingInstructionsValidator implements Validator<MongoScalingInstructionsDTO> {
        private final StringMessages stringMessages;
        
        public MongoScalingInstructionsValidator(StringMessages stringMessages) {
            super();
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(MongoScalingInstructionsDTO valueToValidate) {
            final String result;
            if (valueToValidate == null) {
                result = null;
            } else {
                final Iterable<MongoProcessDTO> hostsAndPortsToShutdownOnNonDefaultPorts = Util.filter(valueToValidate.getHostnamesAndPortsToShutDown(),
                        hostnameAndPortToShutDown->hostnameAndPortToShutDown.getPort() != MongoDBConstants.DEFAULT_PORT);
                if (!Util.isEmpty(hostsAndPortsToShutdownOnNonDefaultPorts)) {
                    result = stringMessages.youCannotShutdownMongoDBInstancesNotRunningOnDefaultPort(Util.joinStrings(", ", hostsAndPortsToShutdownOnNonDefaultPorts));
                } else if (valueToValidate.getLaunchParameters().getNumberOfInstances() == null || valueToValidate.getLaunchParameters().getNumberOfInstances() < 0) {
                    result = stringMessages.youHaveToProvideAPositiveNumberOfInstancesToLaunch();
                } else if (valueToValidate.getLaunchParameters().getReplicaSetPriority() == null || valueToValidate.getLaunchParameters().getReplicaSetPriority() < 0) {
                    result = stringMessages.youHaveToProvideANonNegativePriority();
                } else if (valueToValidate.getLaunchParameters().getReplicaSetVotes() == null || valueToValidate.getLaunchParameters().getReplicaSetVotes() < 0) {
                    result = stringMessages.youHaveToProvideANonNegativeNumberOfVotes();
                } else {
                    result = null;
                }
            }
            return result;
        }
    }

    public MongoScalingDialog(MongoEndpointDTO mongoEndpointToScale, StringMessages stringMessages,
            ErrorReporter errorReporter, LandscapeManagementWriteServiceAsync landscapeManagementService, DialogCallback<MongoScalingInstructionsDTO> dialogCallback) {
        super(stringMessages.scale(), /* message */ null, stringMessages.ok(), stringMessages.cancel(),
                /* validator */ new MongoScalingInstructionsValidator(stringMessages), /* animationEnabled */ true, dialogCallback);
        replicaSetName = mongoEndpointToScale.getReplicaSetName();
        replicaSetPrimary = mongoEndpointToScale.getHostnamesAndPorts().get(0).getHostname()+":"+mongoEndpointToScale.getHostnamesAndPorts().get(0).getPort();
        panel = new VerticalPanel();
        tableForChoosingInstancesToShutDown = new TableWrapperWithMultiSelectionAndFilter<MongoProcessDTO, StringMessages, AdminConsoleTableResources>(
                stringMessages, errorReporter, /* enablePager */ false,
                /* entity identity comparator not required; pairs work by equality */ Optional.empty(),
                GWT.create(AdminConsoleTableResources.class),
                Optional.of(hostnameAndPort -> hostnameAndPort.getPort() == MongoDBConstants.DEFAULT_PORT),
                /* filter label */ Optional.empty(), stringMessages.showOnlyInstanceYouCanStop()) {
            @Override
            protected Iterable<String> getSearchableStrings(MongoProcessDTO t) {
                return Arrays.asList(t.getHostname(), Integer.toString(t.getPort()));
            }
        };
        tableForChoosingInstancesToShutDown.getSelectionModel().addSelectionChangeHandler(e->validateAndUpdate());
        tableForChoosingInstancesToShutDown.addColumn(hostnameAndPort->hostnameAndPort.getHost().getInstanceId(), stringMessages.instanceId());
        tableForChoosingInstancesToShutDown.addColumn(hostnameAndPort->hostnameAndPort.getHostname(), stringMessages.hostname());
        tableForChoosingInstancesToShutDown.addColumn(hostnameAndPort->Integer.toString(hostnameAndPort.getPort()), stringMessages.port());
        tableForChoosingInstancesToShutDown.addColumn(hostnameAndPort->hostnameAndPort.getHost().getLaunchTimePoint().toString(), stringMessages.launchTimePoint(),
                (mp1, mp2)->mp1.getHost().getLaunchTimePoint().compareTo(mp2.getHost().getLaunchTimePoint()));
        tableForChoosingInstancesToShutDown.refresh(mongoEndpointToScale.getHostnamesAndPorts());
        panel.add(new Label(stringMessages.mongoInstancesToStop()));
        panel.add(tableForChoosingInstancesToShutDown);
        final CaptionPanel launchPanel = new CaptionPanel(stringMessages.mongoInstancesToLaunch());
        final Grid grid = new Grid(4, 2);
        int row = 0;
        grid.setWidget(row, 0, new Label(stringMessages.numberOfMongoInstancesToLaunch()));
        numberOfInstancesToLaunchBox = createIntegerBox(0, /* visibleLength */ 2);
        grid.setWidget(row++, 1, numberOfInstancesToLaunchBox);
        grid.setWidget(row, 0, new Label(stringMessages.priority()));
        priorityBox = createIntegerBox(0, /* visibleLength */ 2);
        grid.setWidget(row++, 1, priorityBox);
        grid.setWidget(row, 0, new Label(stringMessages.votes()));
        voteBox = createIntegerBox(0, /* visibleLength */ 2);
        grid.setWidget(row++, 1, voteBox);
        grid.setWidget(row, 0, new Label(stringMessages.instanceType()));
        instanceTypeBox = LandscapeDialogUtil.createInstanceTypeListBox(this, landscapeManagementService, stringMessages, DEFAULT_INSTANCE_TYPE, errorReporter, /* canBeDeployedInNlbInstanceBasedTargetGroup */ false);
        grid.setWidget(row++, 1, instanceTypeBox);
        launchPanel.add(grid);
        panel.add(launchPanel);
    }

    @Override
    protected Widget getAdditionalWidget() {
        return panel;
    }

    @Override
    protected MongoScalingInstructionsDTO getResult() {
        final MongoLaunchParametersDTO launchParameters = new MongoLaunchParametersDTO(replicaSetName,
                replicaSetPrimary, priorityBox.getValueOrNullIfUnparsable(), voteBox.getValueOrNullIfUnparsable(), instanceTypeBox.getSelectedValue(),
                numberOfInstancesToLaunchBox.getValueOrNullIfUnparsable());
        return new MongoScalingInstructionsDTO(replicaSetName,
                tableForChoosingInstancesToShutDown.getSelectionModel().getSelectedSet(), launchParameters);
    }
}
