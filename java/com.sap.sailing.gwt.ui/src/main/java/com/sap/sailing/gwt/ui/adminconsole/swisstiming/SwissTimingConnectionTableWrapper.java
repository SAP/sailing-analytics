package com.sap.sailing.gwt.ui.adminconsole.swisstiming;

import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationWithSecurityDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.AbstractSortableTextColumn;
import com.sap.sse.gwt.client.celltable.CellTableWithCheckboxResources;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.TableWrapper;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;
import com.sap.sse.security.ui.client.i18n.StringMessages;

/**
 * A wrapper for a CellTable displaying an overview over the existing SwissTimingConnections. It shows the name, the
 * event id, the event json url, the hostname, the port, the update url, the username for sending updates, the password
 * for sending updates, the name of the creator, the group and user each user group is owned by. There are options edit
 * or to delete the connection, change the ownership or edit the associated ACL. Editing the connection will open a new
 * instance of {@link SwissTimingConnectionDialog}.
 */
public class SwissTimingConnectionTableWrapper extends
        TableWrapper<SwissTimingConfigurationWithSecurityDTO, RefreshableMultiSelectionModel<SwissTimingConfigurationWithSecurityDTO>, StringMessages, CellTableWithCheckboxResources> {
    private final LabeledAbstractFilterablePanel<SwissTimingConfigurationWithSecurityDTO> filterField;
    private final SailingServiceAsync sailingServiceAsync;
    private final com.sap.sailing.gwt.ui.client.StringMessages stringMessagesClient;

    public SwissTimingConnectionTableWrapper(final UserService userService, final SailingServiceWriteAsync sailingServiceWriteAsync,
            final com.sap.sailing.gwt.ui.client.StringMessages stringMessages, final ErrorReporter errorReporter,
            final boolean enablePager, final CellTableWithCheckboxResources tableResources, final Runnable refresher) {
        super(stringMessages, errorReporter, true, enablePager,
                new EntityIdentityComparator<SwissTimingConfigurationWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(SwissTimingConfigurationWithSecurityDTO dto1,
                            SwissTimingConfigurationWithSecurityDTO dto2) {
                        return dto1.getIdentifier().equals(dto2.getIdentifier());
                    }

                    @Override
                    public int hashCode(SwissTimingConfigurationWithSecurityDTO t) {
                        return t.getIdentifier().hashCode();
                    }
                }, tableResources);
        this.stringMessagesClient = stringMessages;
        this.sailingServiceAsync = sailingServiceWriteAsync;
        final ListHandler<SwissTimingConfigurationWithSecurityDTO> swissTimingConectionColumnListHandler = getColumnSortHandler();
        // table
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionNameColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> dto.getName(), swissTimingConectionColumnListHandler);
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionManage2SailEventIdColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> SwissTimingEventIdUrlUtil.getEventIdFromUrl(dto.getJsonUrl()),
                swissTimingConectionColumnListHandler);
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionJsonUrlColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> dto.getJsonUrl(), swissTimingConectionColumnListHandler);
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionHostnameColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> dto.getHostname(), swissTimingConectionColumnListHandler);
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionPortColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> dto.getPort() == null ? "" : ("" + dto.getPort()), swissTimingConectionColumnListHandler);
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionUpdateUrlColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> dto.getUpdateURL(), swissTimingConectionColumnListHandler);
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionUpdateUsernameColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> dto.getApiToken(), swissTimingConectionColumnListHandler);
        final TextColumn<SwissTimingConfigurationWithSecurityDTO> swissTimingConnectionCreatorNameColumn = new AbstractSortableTextColumn<SwissTimingConfigurationWithSecurityDTO>(
                dto -> dto.getCreatorName(), swissTimingConectionColumnListHandler);
        final HasPermissions type = SecuredDomainType.SWISS_TIMING_ACCOUNT;
        final AccessControlledActionsColumn<SwissTimingConfigurationWithSecurityDTO, DefaultActionsImagesBarCell> actionColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_UPDATE, DefaultActions.UPDATE, dto -> {
            new SwissTimingConnectionEditDialog(dto, new DialogCallback<SwissTimingConfigurationWithSecurityDTO>() {
                @Override
                public void ok(final SwissTimingConfigurationWithSecurityDTO editedObject) {
                    sailingServiceWriteAsync.updateSwissTimingConfiguration(editedObject,
                            new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError(
                                            "Exception trying to update configuration in DB: " + caught.getMessage());
                                }

                                @Override
                                public void onSuccess(Void voidResult) {
                                    refreshSwissTimingConnectionList();
                                }
                            }));
                }

                @Override
                public void cancel() {
                }
            }, userService, errorReporter).show();
        });
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_DELETE, DefaultActions.DELETE, dto -> {
            sailingServiceWriteAsync.deleteSwissTimingConfigurations(Collections.singletonList(dto), new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Exception trying to delete configuration in DB: " + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    refreshSwissTimingConnectionList();
                }
            });
        });
        final EditOwnershipDialog.DialogConfig<SwissTimingConfigurationWithSecurityDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, dto -> refreshSwissTimingConnectionList(),
                        stringMessages);
        final EditACLDialog.DialogConfig<SwissTimingConfigurationWithSecurityDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, dto -> dto.getAccessControlList(), stringMessages);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP, DefaultActions.CHANGE_OWNERSHIP,
                configOwnership::openOwnershipDialog);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                u -> configACL.openDialog(u));
        // filter field:
        filterField = new LabeledAbstractFilterablePanel<SwissTimingConfigurationWithSecurityDTO>(
                new Label(stringMessagesClient.filterSwissTimingConnections()),
                new ArrayList<SwissTimingConfigurationWithSecurityDTO>(), dataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(SwissTimingConfigurationWithSecurityDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                if (t.getApiToken() != null) {
                    strings.add(t.getApiToken());
                }
                strings.add(t.getCreatorName());
                if (t.getHostname() != null) {
                    strings.add(t.getHostname());
                }
                if (t.getPort() != null) {
                    strings.add(t.getPort().toString());
                }
                if (t.getJsonUrl() != null) {
                    strings.add(t.getJsonUrl());
                }
                return strings;
            }

            @Override
            public AbstractCellTable<SwissTimingConfigurationWithSecurityDTO> getCellTable() {
                return table;
            }
        };
        filterField
                .setUpdatePermissionFilterForCheckbox(connection -> userService.hasPermission(connection, DefaultActions.UPDATE));
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        mainPanel.insert(filterField, 0);
        table.addColumnSortHandler(swissTimingConectionColumnListHandler);
        table.addColumn(swissTimingConnectionNameColumn, getStringMessages().name());
        table.addColumn(swissTimingConnectionManage2SailEventIdColumn, stringMessagesClient.manage2SailEventIdBox());
        table.addColumn(swissTimingConnectionJsonUrlColumn, stringMessagesClient.manage2SailEventURLBox());
        table.addColumn(swissTimingConnectionHostnameColumn, stringMessagesClient.hostname());
        table.addColumn(swissTimingConnectionPortColumn, stringMessagesClient.manage2SailPort());
        table.addColumn(swissTimingConnectionUpdateUrlColumn, stringMessagesClient.updateURL());
        table.addColumn(swissTimingConnectionUpdateUsernameColumn, stringMessagesClient.username());
        table.addColumn(swissTimingConnectionCreatorNameColumn, stringMessagesClient.creatorName());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, swissTimingConectionColumnListHandler, stringMessages);
        table.addColumn(actionColumn, stringMessages.actions());
        table.ensureDebugId("SwissTimingConfigurationWithSecurityDTOTable");
    }

    public LabeledAbstractFilterablePanel<SwissTimingConfigurationWithSecurityDTO> getFilterField() {
        return filterField;
    }

    public void refreshSwissTimingConnectionList() {
        sailingServiceAsync
                .getPreviousSwissTimingConfigurations(
                        new AsyncCallback<List<SwissTimingConfigurationWithSecurityDTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter
                                        .reportError(
                                                "Remote Procedure Call getPreviousSwissTimingConfigurations() - Failure: "
                                        + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<SwissTimingConfigurationWithSecurityDTO> result) {
                        filterField.updateAll(result);
                    }
                });
    }
}
