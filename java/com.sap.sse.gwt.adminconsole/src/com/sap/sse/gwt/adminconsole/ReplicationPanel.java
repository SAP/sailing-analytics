package com.sap.sse.gwt.adminconsole;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.ActionsColumn;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.TableWrapper;
import com.sap.sse.gwt.client.controls.IntegerBox;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator;
import com.sap.sse.gwt.client.replication.RemoteReplicationServiceAsync;
import com.sap.sse.gwt.shared.replication.ReplicaDTO;
import com.sap.sse.gwt.shared.replication.ReplicationMasterDTO;
import com.sap.sse.gwt.shared.replication.ReplicationStateDTO;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;

/**
 * Allows administrators to manage all aspects of server instance replication such as showing whether the instance
 * is a master or a replica and for a master showing the replicas to which the master is currently replicating.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ReplicationPanel extends FlowPanel {
    private final ReplicaTableWrapper replicasTable;
    private final RefreshableMultiSelectionModel<ReplicaDTO> replicaSelectionModel;
    private final Grid registeredMasters;
    private final CaptionPanel replicaDetailPanel;
    private final Grid replicaDetailGrid;
    private final RemoteReplicationServiceAsync replicationServiceAsync;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final Button addButton;
    private final Button stopReplicationButton;
    
    private static class ReplicationData {
        private final String messagingHost;
        private final String masterHostName;
        private final String exchangeName;
        private final int messagingPort;
        private final int servletPort;
        private final String userName;
        private final String password;

        public ReplicationData(String messagingHost, String masterHostName, String exchangeName, int messagingPort,
                int servletPort, String userName, String password) {
            super();
            this.messagingHost = messagingHost;
            this.masterHostName = masterHostName;
            this.exchangeName = exchangeName;
            this.messagingPort = messagingPort;
            this.servletPort = servletPort;
            this.userName = userName;
            this.password = password;
        }

        public String getMessagingHost() {
            return messagingHost;
        }

        public String getMasterHostName() {
            return masterHostName;
        }

        public String getExchangeName() {
            return exchangeName;
        }

        public int getMessagingPort() {
            return messagingPort;
        }

        public int getServletPort() {
            return servletPort;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }
    }

    /**
     * For administrators it is important to understand whether an instance is a replica. A
     * {@link ErrorReporter#reportPersistentInformation(String) persistent error message} shall be shown
     * in this case; however, some replicables may not be considered part of the "domain-based" replication.
     * For example, if only a security service for user management is replicated in order to implement
     * central user and session management then this shall not lead to the warning being displayed.
     * This set contains the stringified IDs of those replicables for which being a replica shall
     * lead to a warnings.
     */
    private final Set<String> replicableIdsAsStringThatShallLeadToWarningAboutInstanceBeingReplica;
    
    public ReplicationPanel(RemoteReplicationServiceAsync replicationService, UserService userService, ErrorReporter errorReporter, final StringMessages stringMessages) {
        this.replicationServiceAsync = replicationService;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.replicableIdsAsStringThatShallLeadToWarningAboutInstanceBeingReplica = new HashSet<>();
        replicationService.getReplicableIdsAsStringThatShallLeadToWarningAboutInstanceBeingReplica(new MarkedAsyncCallback<>(new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log(stringMessages.errorFetchingReplicaData(caught.getMessage()));
            }

            @Override
            public void onSuccess(String[] result) {
                for (final String replicableIdAsStringThatShallLeadToWarningAboutInstanceBeingReplica : result) {
                    replicableIdsAsStringThatShallLeadToWarningAboutInstanceBeingReplica.add(replicableIdAsStringThatShallLeadToWarningAboutInstanceBeingReplica);
                }
            }
        }));
        final Button refreshButton = new Button(stringMessages.refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                updateReplicaList();
            }
        });
        add(refreshButton);
        // --- Replicas (master side) ---
        final CaptionPanel mastergroup = new CaptionPanel(stringMessages.explainReplicasRegistered());
        final VerticalPanel masterpanel = new VerticalPanel();
        final AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
        replicasTable = new ReplicaTableWrapper(tableResources, userService, stringMessages, errorReporter);
        replicaSelectionModel = replicasTable.getSelectionModel();
        final AccessControlledButtonPanel masterPanelButtons = new AccessControlledButtonPanel(userService, SecuredSecurityTypes.SERVER);
        masterPanelButtons.addCountingAction(stringMessages.dropReplicas(),
                replicaSelectionModel, () -> userService.hasServerPermission(ServerActions.REPLICATE),
                this::dropSelectedReplicas);
        masterpanel.add(replicasTable.asWidget());
        masterpanel.add(masterPanelButtons);
        mastergroup.add(masterpanel);
        add(mastergroup);
        // --- Details caption panel (below replicas table, shown for single selection) ---
        replicaDetailPanel = new CaptionPanel();
        replicaDetailGrid = new Grid(10, 2);
        replicaDetailPanel.setContentWidget(replicaDetailGrid);
        replicaDetailPanel.setVisible(false);
        add(replicaDetailPanel);
        // --- Masters (replica side) ---
        final CaptionPanel replicagroup = new CaptionPanel(stringMessages.explainConnectionsToMaster());
        final VerticalPanel replicapanel = new VerticalPanel();
        final AccessControlledButtonPanel replicapanelbuttons = new AccessControlledButtonPanel(userService, SecuredSecurityTypes.SERVER);
        registeredMasters = new Grid();
        registeredMasters.resizeColumns(3);
        replicapanel.add(registeredMasters);
        addButton = replicapanelbuttons.addAction(stringMessages.connectToMaster(),
                () -> userService.hasServerPermission(ServerActions.START_REPLICATION), this::addReplication);
        stopReplicationButton = replicapanelbuttons.addAction(stringMessages.stopConnectionToMaster(),
                () -> userService.hasServerPermission(ServerActions.START_REPLICATION), this::stopReplication);
        stopReplicationButton.setEnabled(false);
        replicapanel.add(replicapanelbuttons);
        replicagroup.add(replicapanel);
        add(replicagroup);
        replicaSelectionModel.addSelectionChangeHandler(event -> {
            final Set<ReplicaDTO> selected = replicaSelectionModel.getSelectedSet();
            if (selected.size() == 1) {
                refreshReplicaDetail(selected.iterator().next());
            } else {
                replicaDetailPanel.setVisible(false);
            }
        });
        if (userService.hasServerPermission(ServerActions.READ_REPLICATOR)) {
            updateReplicaList();
        }
    }

    /**
     * A {@link TableWrapper} for {@link ReplicaDTO} rows, with sortable text columns and an actions column
     * for dropping individual replica connections. Multi-selection is enabled via a checkbox column managed
     * by the base class.
     */
    private class ReplicaTableWrapper
            extends TableWrapper<ReplicaDTO, RefreshableMultiSelectionModel<ReplicaDTO>, StringMessages, AdminConsoleTableResources> {
        ReplicaTableWrapper(final AdminConsoleTableResources tableResources, final UserService userService,
                final StringMessages stringMessages, final ErrorReporter errorReporter) {
            super(stringMessages, errorReporter, /* multiSelection */ true, /* enablePager */ false,
                    new EntityIdentityComparator<ReplicaDTO>() {
                        @Override
                        public boolean representSameEntity(final ReplicaDTO r1, final ReplicaDTO r2) {
                            return r1.getIdentifier().equals(r2.getIdentifier());
                        }
                        @Override
                        public int hashCode(final ReplicaDTO t) {
                            return t.getIdentifier().hashCode();
                        }
                    }, tableResources);
            addColumn(ReplicaDTO::getHostname, stringMessages.replicaColumnIp(),
                    (r1, r2) -> r1.getHostname().compareTo(r2.getHostname()));
            addColumn(ReplicaDTO::getIdentifier, stringMessages.replicaColumnId(),
                    (r1, r2) -> r1.getIdentifier().compareTo(r2.getIdentifier()));
            addColumn(r -> r.getRegistrationTime().toString(), stringMessages.replicaColumnRegistered(),
                    (r1, r2) -> r1.getRegistrationTime().compareTo(r2.getRegistrationTime()));
            addColumn(r -> String.valueOf(Math.round(r.getAverageNumberOfOperationsPerMessage())),
                    stringMessages.replicaColumnOpsPerMsg(),
                    (r1, r2) -> Double.compare(r1.getAverageNumberOfOperationsPerMessage(), r2.getAverageNumberOfOperationsPerMessage()));
            addColumn(r -> String.valueOf(r.getNumberOfMessagesSent()), stringMessages.replicaColumnMessages(),
                    (r1, r2) -> Long.compare(r1.getNumberOfMessagesSent(), r2.getNumberOfMessagesSent()));
            addColumn(r -> String.valueOf(Math.round(r.getAverageMessageSizeInBytes())), stringMessages.replicaColumnAvgMsgSize(),
                    (r1, r2) -> Double.compare(r1.getAverageMessageSizeInBytes(), r2.getAverageMessageSizeInBytes()));
            addColumn(r -> r.getNumberOfBytesSent() + "B (" + (r.getNumberOfBytesSent() / 1024 / 1024) + "MB)",
                    stringMessages.replicaColumnTotalSize(),
                    (r1, r2) -> Long.compare(r1.getNumberOfBytesSent(), r2.getNumberOfBytesSent()));
            addColumn(r -> String.valueOf(r.getOperationCountByOperationClassName().values().stream().mapToLong(Integer::longValue).sum()),
                    stringMessages.replicaColumnTotalOps(),
                    (r1, r2) -> Long.compare(
                            r1.getOperationCountByOperationClassName().values().stream().mapToLong(Integer::longValue).sum(),
                            r2.getOperationCountByOperationClassName().values().stream().mapToLong(Integer::longValue).sum()));
            final ActionsColumn<ReplicaDTO, ReplicaImagesBarCell> actionsColumn = new ActionsColumn<>(
                    new ReplicaImagesBarCell(stringMessages),
                    (replica, action) -> userService.hasServerPermission(ServerActions.REPLICATE));
            actionsColumn.addAction(ReplicaImagesBarCell.ACTION_DROP, replica -> dropSingleReplica(replica));
            addColumn(actionsColumn, stringMessages.additionalInformation());
        }
    }
    /**
     * Populates {@link #replicaDetailPanel} with the details of the given replica and makes it visible.
     * Called when exactly one row is selected in {@link #replicasTable}. The {@link #replicaDetailGrid}
     * is resized to the required number of rows.
     */
    private void refreshReplicaDetail(final ReplicaDTO replica) {
        replicaDetailPanel.setCaptionText(replica.getHostname() + " (" + replica.getIdentifier() + ")");
        int row = 0;
        replicaDetailGrid.resize(6 + replica.getOperationCountByOperationClassName().size(), 2);
        replicaDetailGrid.setWidget(row, 0, new Label(stringMessages.registrationTime()));
        replicaDetailGrid.setWidget(row, 1, new Label(replica.getRegistrationTime().toString()));
        row++;
        replicaDetailGrid.setWidget(row, 0, new Label(stringMessages.replicables()));
        replicaDetailGrid.setWidget(row, 1, new Label(Arrays.toString(replica.getReplicableIdsAsStrings()).replaceAll(",", "\n")));
        row++;
        replicaDetailGrid.setWidget(row, 0, new Label(stringMessages.additionalInformation()));
        replicaDetailGrid.setWidget(row, 1, new Label(replica.getAdditionalInformation()));
        row++;
        long totalNumberOfOperations = 0;
        for (final Map.Entry<String, Integer> e : replica.getOperationCountByOperationClassName().entrySet()) {
            replicaDetailGrid.setWidget(row, 0, new Label(e.getKey()));
            replicaDetailGrid.setWidget(row, 1, new Label(e.getValue().toString()));
            totalNumberOfOperations += e.getValue();
            row++;
        }
        replicaDetailGrid.setWidget(row, 0, new Label(stringMessages.totalNumberOfOperations()));
        replicaDetailGrid.setWidget(row, 1, new Label(String.valueOf(totalNumberOfOperations)));
        row++;
        replicaDetailGrid.setWidget(row, 0, new Label(stringMessages.totalSize()));
        replicaDetailGrid.setWidget(row, 1, new Label(replica.getNumberOfBytesSent() + "B (" + (replica.getNumberOfBytesSent() / 1024 / 1024) + "MB)"));
        replicaDetailPanel.setVisible(true);
    }
    /**
     * Drops the replication connection for every replica currently selected in {@link #replicasTable}.
     */
    private void dropSelectedReplicas() {
        final Set<ReplicaDTO> selected = new HashSet<>(replicaSelectionModel.getSelectedSet());
        replicaSelectionModel.clear();
        for (final ReplicaDTO replica : selected) {
            dropSingleReplica(replica);
        }
    }
    /**
     * Calls {@link RemoteReplicationServiceAsync#stopSingleReplicaInstance} for the given replica and
     * refreshes the list on both success and failure.
     */
    private void dropSingleReplica(final ReplicaDTO replica) {
        replicationServiceAsync.stopSingleReplicaInstance(replica.getIdentifier(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
                updateReplicaList();
            }
            @Override
            public void onSuccess(Void result) {
                updateReplicaList();
            }
        });
    }
    private void stopReplication() {
        stopReplicationButton.setEnabled(false);
        replicationServiceAsync.stopReplicatingFromMaster(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.getMessage());
                stopReplicationButton.setEnabled(true);
            }
            @Override
            public void onSuccess(Void result) {
                addButton.setEnabled(true);
                stopReplicationButton.setEnabled(false);
                updateReplicaList();
            }
        });
    }
    private void addReplication() {
        new AddReplicationDialog(new Validator<ReplicationData>() {
            @Override
            public String getErrorMessage(ReplicationData valueToValidate) {
                final boolean userNameUnset = valueToValidate.getUserName() == null || valueToValidate.getUserName().isEmpty();
                final boolean passWordUnset = valueToValidate.getPassword() == null || valueToValidate.getPassword().isEmpty();
                return userNameUnset == passWordUnset ? null : stringMessages.usernameAndPasswordMustBothBeSet();
            }
        }, new DialogCallback<ReplicationData>() {
            @Override
            public void ok(final ReplicationData state) {
                addButton.setEnabled(false);
                stopReplicationButton.setEnabled(false);
                replicationServiceAsync.startReplicatingFromMaster(state.getMessagingHost(), state.getMasterHostName(),
                        state.getExchangeName(), state.getServletPort(), state.getMessagingPort(), state.getUserName(),
                        state.getPassword(), new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable e) {
                                addButton.setEnabled(true);
                                errorReporter.reportError(stringMessages.errorStartingReplication(state.getMasterHostName(),
                                        state.getExchangeName(), e.getMessage()));
                                updateReplicaList();
                            }

                            @Override
                            public void onSuccess(Void arg0) {
                                addButton.setEnabled(false);
                                stopReplicationButton.setEnabled(true);
                                updateReplicaList();
                            }
                        });
            }

            @Override
            public void cancel() {
                // simply don't add replication
            }
        }).show();
    }

    public void updateReplicaList() {
        replicationServiceAsync.getReplicaInfo(new AsyncCallback<ReplicationStateDTO>() {
            @Override
            public void onSuccess(ReplicationStateDTO replicas) {
                final List<ReplicaDTO> replicaList = replicasTable.getDataProvider().getList();
                replicaList.clear();
                for (final ReplicaDTO replica : replicas.getReplicas()) {
                    replicaList.add(replica);
                }
                while (registeredMasters.getRowCount() > 0) {
                    registeredMasters.removeRow(0);
                }
                int i = 0;
                registeredMasters.insertRow(i);
                registeredMasters.setWidget(i, 0, new Label("Client UUID: " + replicas.getServerIdentifier()));
                i++;
                final ReplicationMasterDTO replicatingFromMaster = replicas.getReplicatingFromMaster();
                if (replicatingFromMaster != null) { // TODO bug 2465: replicating only the user service from a "domain controller master" shouldn't lead to a warning here...
                    for (final String replicableIdAsStringOfReplicableThatWeAreReplicatingCurrently : replicatingFromMaster.getReplicableIdsAsString()) {
                        if (Util.contains(replicableIdsAsStringThatShallLeadToWarningAboutInstanceBeingReplica, replicableIdAsStringOfReplicableThatWeAreReplicatingCurrently)) {
                            errorReporter.reportPersistentInformation(stringMessages.warningServerIsReplica());
                            break;
                        }
                    }
                    registeredMasters.insertRow(i);
                    registeredMasters.setWidget(i, 0, new Label(stringMessages.replicatingFromMaster(replicatingFromMaster.getHostname(),
                            replicatingFromMaster.getMessagingPort(), replicatingFromMaster.getServletPort(),
                            replicatingFromMaster.getMessagingHostname(), replicatingFromMaster.getExchangeName(),
                            Arrays.toString(replicatingFromMaster.getReplicableIdsAsString()))));
                    i++;
                    addButton.setEnabled(false);
                    stopReplicationButton.setEnabled(true);
                } else {
                    errorReporter.reportPersistentInformation("");
                    registeredMasters.insertRow(i);
                    registeredMasters.setWidget(i, 0, new Label(stringMessages.explainNoConnectionsToMaster()));
                    addButton.setEnabled(true);
                    stopReplicationButton.setEnabled(false);
                }
            }
            @Override
            public void onFailure(Throwable e) {
                errorReporter.reportError(stringMessages.errorFetchingReplicaData(e.getMessage()));
            }
        });
    }

    /**
     * A text entry dialog with ok/cancel button and configurable validation rule. Subclasses may provide a redefinition for
     * {@link #getAdditionalWidget()} to add a widget below the text field, e.g., for capturing additional data. The result of
     * this dialog is a triple containing the RabbitMQ exchange hostname, the servlet hostname and the RabbitMQ exchange name,
     * followed by the RabbitMQ messaging port and the servlet port.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class AddReplicationDialog extends DataEntryDialog<ReplicationData> {
        private final TextBox hostnameEntryField;
        private final TextBox exchangeHostnameEntryField;
        private final TextBox exchangenameEntryField;
        private final IntegerBox messagingPortField;
        private final IntegerBox servletPortField;
        private final TextBox usernameEntryField;
        private final PasswordTextBox passwordEntryField;
        
        public AddReplicationDialog(final Validator<ReplicationData> validator,
                final DialogCallback<ReplicationData> callback) {
            super(stringMessages.connect(), stringMessages.enterMaster(),
                    stringMessages.ok(), stringMessages.cancel(), validator, callback);
            hostnameEntryField = createTextBox("localhost");
            exchangeHostnameEntryField = createTextBox("localhost");
            exchangenameEntryField = createTextBox("sapsailinganalytics");
            messagingPortField = createIntegerBox(0, /* visible length */ 5);
            servletPortField = createIntegerBox(8888, /* visibleLength */ 5);
            usernameEntryField = createTextBox("admin");
            passwordEntryField = createPasswordTextBox("admin");
        }
        /**
         * Can contribute an additional widget to be displayed underneath the text entry field. If <code>null</code> is
         * returned, no additional widget will be displayed. This is the default behavior of this default implementation.
         */
        @Override
        protected Widget getAdditionalWidget() {
            final Grid grid = new Grid(14, 2);
            grid.setWidget(0, 0, new Label(stringMessages.hostname()));
            grid.setWidget(0, 1, hostnameEntryField);
            grid.setWidget(1, 0, new Label(stringMessages.explainReplicationHostname()));
            
            grid.setWidget(2, 0, new Label(stringMessages.exchangeHost()));
            grid.setWidget(2, 1, exchangeHostnameEntryField);
            grid.setWidget(3, 0, new Label(stringMessages.explainExchangeHostName()));
            
            grid.setWidget(4, 0, new Label(stringMessages.exchangeName()));
            grid.setWidget(4, 1, exchangenameEntryField);
            grid.setWidget(5, 0, new Label(stringMessages.explainReplicationExchangeName()));
            
            grid.setWidget(6, 0, new Label(stringMessages.messagingPortNumber()));
            grid.setWidget(6, 1, messagingPortField);
            grid.setWidget(7, 0, new Label(stringMessages.explainReplicationExchangePort()));
            
            grid.setWidget(8, 0, new Label(stringMessages.servletPortNumber()));
            grid.setWidget(8, 1, servletPortField);
            grid.setWidget(9, 0, new Label(stringMessages.explainReplicationServletPort()));
            
            grid.setWidget(10, 0, new Label(stringMessages.username()));
            grid.setWidget(10, 1, usernameEntryField);
            grid.setWidget(11, 0, new Label(stringMessages.explainUserName()));
            
            grid.setWidget(12, 0, new Label(stringMessages.password()));
            grid.setWidget(12, 1, passwordEntryField);
            grid.setWidget(13, 0, new Label(stringMessages.explainPassword()));
            return grid;
        }
        
        @Override
        protected Focusable getInitialFocusWidget() {
            return hostnameEntryField;
        }
        
        @Override
        protected ReplicationData getResult() {
            return new ReplicationData(exchangeHostnameEntryField.getValue(), hostnameEntryField.getValue(),
                    exchangenameEntryField.getValue(), messagingPortField.getValue(), servletPortField.getValue(),
                    usernameEntryField.getValue(), passwordEntryField.getValue());
        }
    }
}
