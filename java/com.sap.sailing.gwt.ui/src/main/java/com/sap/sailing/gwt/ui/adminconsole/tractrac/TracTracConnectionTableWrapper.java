package com.sap.sailing.gwt.ui.adminconsole.tractrac;

import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
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
 * A wrapper for a CellTable displaying an overview over the existing TracTracConnections. It shows the name, the Live
 * URI, the Stored Uri, the JSON URL, the TracTrac Server Update URI, the TracTrac user name, the TracTrac password, the
 * name of the creator, the group and user each user group is owned by. There are options edit or to delete the
 * connection, change the ownership or edit the associated ACL. Editing the connection will open a new instance of
 * {@link TracTracConnectionDialog}.
 */
public class TracTracConnectionTableWrapper extends
        TableWrapper<TracTracConfigurationWithSecurityDTO, RefreshableMultiSelectionModel<TracTracConfigurationWithSecurityDTO>, StringMessages, CellTableWithCheckboxResources> {
    private final LabeledAbstractFilterablePanel<TracTracConfigurationWithSecurityDTO> filterField;
    private final SailingServiceAsync sailingServiceWriteAsync;
    private final com.sap.sailing.gwt.ui.client.StringMessages stringMessagesClient;

    public TracTracConnectionTableWrapper(final UserService userService, final SailingServiceWriteAsync sailingServiceWriteAsync,
            final com.sap.sailing.gwt.ui.client.StringMessages stringMessages, final ErrorReporter errorReporter,
            final boolean enablePager, final CellTableWithCheckboxResources tableResources, final Runnable refresher) {
        super(stringMessages, errorReporter, true, enablePager,
                new EntityIdentityComparator<TracTracConfigurationWithSecurityDTO>() {
                    @Override
                    public boolean representSameEntity(TracTracConfigurationWithSecurityDTO dto1,
                            TracTracConfigurationWithSecurityDTO dto2) {
                        return dto1.getIdentifier().equals(dto2.getIdentifier());
                    }

                    @Override
                    public int hashCode(TracTracConfigurationWithSecurityDTO t) {
                        return t.getIdentifier().hashCode();
                    }
                }, tableResources);
        this.stringMessagesClient = stringMessages;
        this.sailingServiceWriteAsync = sailingServiceWriteAsync;
        final ListHandler<TracTracConfigurationWithSecurityDTO> tracTracAccountColumnListHandler = getColumnSortHandler();
        // table
        final TextColumn<TracTracConfigurationWithSecurityDTO> tracTracAccountNameColumn = new AbstractSortableTextColumn<TracTracConfigurationWithSecurityDTO>(
                dto -> dto.getName(), tracTracAccountColumnListHandler);
        final TextColumn<TracTracConfigurationWithSecurityDTO> tracTracAccountLiveUriColumn = new AbstractSortableTextColumn<TracTracConfigurationWithSecurityDTO>(
                dto -> dto.getLiveDataURI()==null?"":dto.getLiveDataURI(), tracTracAccountColumnListHandler);
        final TextColumn<TracTracConfigurationWithSecurityDTO> tracTracAccountStoredUriColumn = new AbstractSortableTextColumn<TracTracConfigurationWithSecurityDTO>(
                dto -> dto.getStoredDataURI()==null?"":dto.getStoredDataURI(), tracTracAccountColumnListHandler);
        final TextColumn<TracTracConfigurationWithSecurityDTO> tracTracAccountJsonUrlColumn = new AbstractSortableTextColumn<TracTracConfigurationWithSecurityDTO>(
                dto -> dto.getJsonUrl(), tracTracAccountColumnListHandler);
        final TextColumn<TracTracConfigurationWithSecurityDTO> tracTracAccountTracTracServerUpdateUriColumn = new AbstractSortableTextColumn<TracTracConfigurationWithSecurityDTO>(
                dto -> dto.getUpdateURI()==null?"":dto.getUpdateURI(), tracTracAccountColumnListHandler);
        final TextColumn<TracTracConfigurationWithSecurityDTO> tracTracAccountUsernameColumn = new AbstractSortableTextColumn<TracTracConfigurationWithSecurityDTO>(
                dto -> dto.getTracTracApiToken(), tracTracAccountColumnListHandler);
        final TextColumn<TracTracConfigurationWithSecurityDTO> tracTracAccountCreatorNameColumn = new AbstractSortableTextColumn<TracTracConfigurationWithSecurityDTO>(
                dto -> dto.getCreatorName(), tracTracAccountColumnListHandler);
        final HasPermissions type = SecuredDomainType.TRACTRAC_ACCOUNT;
        final AccessControlledActionsColumn<TracTracConfigurationWithSecurityDTO, DefaultActionsImagesBarCell> actionColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_UPDATE, DefaultActions.UPDATE, dto -> {
            new TracTracConnectionEditDialog(dto, new DialogCallback<TracTracConfigurationWithSecurityDTO>() {
                @Override
                public void ok(final TracTracConfigurationWithSecurityDTO editedObject) {
                    sailingServiceWriteAsync.updateTracTracConfiguration(editedObject,
                            new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError(
                                            "Exception trying to update configuration in DB: " + caught.getMessage());
                                }

                                @Override
                                public void onSuccess(Void voidResult) {
                                    refreshTracTracConnectionList();
                                }
                            }));
                }

                @Override
                public void cancel() {
                }
            }, userService, errorReporter).show();
        });
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_DELETE, DefaultActions.DELETE, dto -> {
            sailingServiceWriteAsync.deleteTracTracConfigurations(Collections.singletonList(dto), new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Exception trying to delete configuration in DB: " + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    refreshTracTracConnectionList();
                }
            });
        });
        final EditOwnershipDialog.DialogConfig<TracTracConfigurationWithSecurityDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, dto -> refreshTracTracConnectionList(),
                        stringMessages);
        final EditACLDialog.DialogConfig<TracTracConfigurationWithSecurityDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, dto -> dto.getAccessControlList(), stringMessages);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP, DefaultActions.CHANGE_OWNERSHIP,
                configOwnership::openOwnershipDialog);
        actionColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                u -> configACL.openDialog(u));
        filterField = new LabeledAbstractFilterablePanel<TracTracConfigurationWithSecurityDTO>(
                new Label(stringMessages.filterTracTracConnections()),
                new ArrayList<TracTracConfigurationWithSecurityDTO>(), dataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(TracTracConfigurationWithSecurityDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.getCreatorName());
                strings.add(t.getJsonUrl());
                return strings;
            }

            @Override
            public AbstractCellTable<TracTracConfigurationWithSecurityDTO> getCellTable() {
                return table;
            }
        };
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        filterField.setUpdatePermissionFilterForCheckbox(connection -> userService.hasPermission(connection, DefaultActions.UPDATE));
        mainPanel.insert(filterField, 0);
        table.addColumnSortHandler(tracTracAccountColumnListHandler);
        table.addColumn(tracTracAccountNameColumn, getStringMessages().name());
        table.addColumn(tracTracAccountLiveUriColumn, stringMessagesClient.liveUri());
        table.addColumn(tracTracAccountStoredUriColumn, stringMessagesClient.storedUri());
        table.addColumn(tracTracAccountJsonUrlColumn, stringMessagesClient.jsonUrl());
        table.addColumn(tracTracAccountTracTracServerUpdateUriColumn, stringMessagesClient.tracTracUpdateUrl());
        table.addColumn(tracTracAccountUsernameColumn, stringMessagesClient.tractracUsername());
        table.addColumn(tracTracAccountCreatorNameColumn, stringMessagesClient.creatorName());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, tracTracAccountColumnListHandler, stringMessages);
        table.addColumn(actionColumn, stringMessages.actions());
        table.ensureDebugId("TracTracConfigurationWithSecurityDTOTable");
    }

    public LabeledAbstractFilterablePanel<TracTracConfigurationWithSecurityDTO> getFilterField() {
        return filterField;
    }

    public void refreshTracTracConnectionList() {
        refreshTracTracConnectionList(/* selectWhenDone */ null);
    }
    
    /**
     * @param selectEntityWhenDone
     *            if not {@code null}, select an entity in the table that the {@link EntityIdentityComparator} considers
     *            the same entity if present
     */
    public void refreshTracTracConnectionList(final TracTracConfigurationWithSecurityDTO selectEntityWhenDone) {
        sailingServiceWriteAsync
                .getPreviousTracTracConfigurations(new AsyncCallback<List<TracTracConfigurationWithSecurityDTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter
                                .reportError("Remote Procedure Call getPreviousTracTracConfigurations() - Failure: "
                                        + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<TracTracConfigurationWithSecurityDTO> result) {
                        filterField.updateAll(result);
                        if (selectEntityWhenDone != null) {
                            Scheduler.get().scheduleDeferred(()->{
                                for (final TracTracConfigurationWithSecurityDTO oneResult : result) {
                                    if (getSelectionModel().getEntityIdentityComparator().representSameEntity(oneResult, selectEntityWhenDone)) {
                                        getSelectionModel().setSelected(oneResult, true);
                                        filterField.search(oneResult.getJsonUrl());
                                        break;
                                    }
                                }
                            });
                        }
                    }
                });
    }
}
