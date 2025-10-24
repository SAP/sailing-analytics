package com.sap.sailing.landscape.ui.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.landscape.ui.client.i18n.StringMessages;
import com.sap.sailing.landscape.ui.shared.AwsInstanceDTO;
import com.sap.sailing.landscape.ui.shared.SailingAnalyticsProcessDTO;
import com.sap.sailing.landscape.ui.shared.SailingApplicationReplicaSetDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

/**
 * Shows the user which application processes are affected by moving all processes away from a host and asks
 * for an optional instance type for the new host to which to move all processes.<p>
 * 
 * The result of the dialog is the instance type (as {@link String}) selected by the user, or {@code null}
 * if the user wants the new host to be of the same type as the original host.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MoveAllAwayFromHostDialog extends DataEntryDialog<String> {
    private final ListBox instanceTypeListBox;
    private final Label instanceTypeLabel;
    private final String SAME_AS_MASTER = "__same_as_master__";

    public MoveAllAwayFromHostDialog(LandscapeManagementWriteServiceAsync landscapeManagementService,
            AwsInstanceDTO fromHost, Iterable<SailingApplicationReplicaSetDTO<String>> replicaSets,
            StringMessages stringMessages, ErrorReporter errorReporter, DialogCallback<String> callback) {
        super(stringMessages.moveAllApplicationProcessesAwayFromMaster(), /* message */ getMessage(fromHost, replicaSets, stringMessages),
                stringMessages.ok(), stringMessages.cancel(), /* validator */ null, callback);
        instanceTypeListBox = LandscapeDialogUtil.createInstanceTypeListBoxWithAdditionalDefaultEntry(this,
                stringMessages.sameAsExistingHost(), SAME_AS_MASTER, landscapeManagementService, stringMessages, /* default */ stringMessages.sameAsExistingHost(),
                errorReporter, /* canBeDeployedInNlbInstanceBasedTargetGroup */ false);
        instanceTypeLabel = new Label();
    }
    
    private static String getMessage(AwsInstanceDTO host,
            Iterable<SailingApplicationReplicaSetDTO<String>> replicaSets, StringMessages stringMessages) {
        final Map<String, SailingAnalyticsProcessDTO> masterProcessesOnHostByReplicaSetName = new HashMap<>();
        final Map<String, SailingAnalyticsProcessDTO> replicaProcessesOnHostByReplicaSetName = new HashMap<>();
        for (final SailingApplicationReplicaSetDTO<String> replicaSet : replicaSets) {
            if (replicaSet.getMaster().getHost().getInstanceId().equals(host.getInstanceId())) {
                masterProcessesOnHostByReplicaSetName.put(replicaSet.getName(), replicaSet.getMaster());
            } else {
                Util.stream(replicaSet.getReplicas())
                        .filter(replica -> replica.getHost().getInstanceId().equals(host.getInstanceId()))
                        .forEach(replicaOnHost -> replicaProcessesOnHostByReplicaSetName.put(replicaSet.getName(), replicaOnHost));
            }
        }
        return stringMessages.moveTheFollowingMasterAndReplicaProcessesAway(host.getInstanceId(),
                String.join(", ", Util.map(masterProcessesOnHostByReplicaSetName.entrySet(),
                        e -> getProcessFromReplicaSetForMessage(e.getKey(), e.getValue(), stringMessages))),
                String.join(", ", Util.map(replicaProcessesOnHostByReplicaSetName.entrySet(),
                        e -> getProcessFromReplicaSetForMessage(e.getKey(), e.getValue(), stringMessages))));
    }
    
    private static String getProcessFromReplicaSetForMessage(String replicaSetName, SailingAnalyticsProcessDTO process, StringMessages stringMessages) {
        return stringMessages.processOfReplicaSet(process.getPort(), replicaSetName);
    }

    protected ListBox getInstanceTypeListBox() {
        return instanceTypeListBox;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final Grid result = new Grid(1, 2);
        int row=0;
        result.setWidget(row, 0, instanceTypeLabel);
        result.setWidget(row++, 1, getInstanceTypeListBox());
        return result;
    }

    @Override
    public Focusable getInitialFocusWidget() {
        return getInstanceTypeListBox();
    }
    
    @Override
    protected String getResult() {
        return getInstanceTypeListBox().getSelectedValue().equals(SAME_AS_MASTER) ? null : getInstanceTypeListBox().getSelectedValue();
    }
}
