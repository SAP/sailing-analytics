package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.adminconsole.FilterablePanelProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public class MarkRolePanel extends FlowPanel implements FilterablePanelProvider<MarkRoleDTO>{

    private static AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final ListDataProvider<MarkRoleDTO> markRoleListDataProvider = new ListDataProvider<>();
    private final LabeledAbstractFilterablePanel<MarkRoleDTO> filterableMarkRoles;
    private List<MarkRoleDTO> allMarkRoles;
    private FlushableCellTable<MarkRoleDTO> markRolesTable;
    private RefreshableMultiSelectionModel<MarkRoleDTO> refreshableSelectionModel;

    public MarkRolePanel(SailingServiceWriteAsync sailingServiceWrite, ErrorReporter errorReporter, StringMessages stringMessages,
            final UserService userService) {
        this.sailingServiceWrite = sailingServiceWrite;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        AccessControlledButtonPanel buttonAndFilterPanel = new AccessControlledButtonPanel(userService,
                SecuredDomainType.MARK_ROLE);
        add(buttonAndFilterPanel);
        allMarkRoles = new ArrayList<>();
        buttonAndFilterPanel.addUnsecuredAction(stringMessages.refresh(), new Command() {
            @Override
            public void execute() {
                loadMarkRoles();
            }
        });
        buttonAndFilterPanel.addCreateAction(stringMessages.add(), new Command() {
            @Override
            public void execute() {
                openEditMarkRoleDialog(new MarkRoleDTO());
            }
        });
        Label lblFilter = new Label(stringMessages.filterMarkRoles() + ":");
        lblFilter.setWordWrap(false);
        buttonAndFilterPanel.addUnsecuredWidget(lblFilter);
        this.filterableMarkRoles = new LabeledAbstractFilterablePanel<MarkRoleDTO>(lblFilter, allMarkRoles,
                markRoleListDataProvider, stringMessages) {
            @Override
            public List<String> getSearchableStrings(MarkRoleDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.getUuid().toString());
                return strings;
            }

            @Override
            public AbstractCellTable<MarkRoleDTO> getCellTable() {
                return markRolesTable;
            }
        };
        createMarkRoleTable(userService);
        filterableMarkRoles.getTextBox().ensureDebugId("MarkRolesFilterTextBox");
        buttonAndFilterPanel.addUnsecuredWidget(filterableMarkRoles);
        filterableMarkRoles
                .setUpdatePermissionFilterForCheckbox(event -> userService.hasPermission(event, DefaultActions.UPDATE));
    }

    public void loadMarkRoles() {
        markRoleListDataProvider.getList().clear();
        sailingServiceWrite.getMarkRoles(new AsyncCallback<List<MarkRoleDTO>>() {

            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.toString());
            }

            @Override
            public void onSuccess(List<MarkRoleDTO> result) {
                markRoleListDataProvider.getList().clear();
                Util.addAll(result, markRoleListDataProvider.getList());
                filterableMarkRoles.updateAll(markRoleListDataProvider.getList());
                markRoleListDataProvider.refresh();
            }

        });
    }

    public void refreshMarkRoles() {
        loadMarkRoles();
    }

    private void createMarkRoleTable(final UserService userService) {
        markRolesTable = new FlushableCellTable<>(1000, tableResources);
        markRolesTable.setWidth("100%");
        ListHandler<MarkRoleDTO> sortHandler = new ListHandler<>(markRoleListDataProvider.getList());
        markRolesTable.addColumnSortHandler(sortHandler);
        markRolesTable.setSelectionModel(refreshableSelectionModel, DefaultSelectionEventManager
                .createCustomManager(new DefaultSelectionEventManager.CheckboxEventTranslator<MarkRoleDTO>() {
                    @Override
                    public boolean clearCurrentSelection(CellPreviewEvent<MarkRoleDTO> event) {
                        return !isCheckboxColumn(event.getColumn());
                    }

                    @Override
                    public SelectAction translateSelectionEvent(CellPreviewEvent<MarkRoleDTO> event) {
                        NativeEvent nativeEvent = event.getNativeEvent();
                        if (BrowserEvents.CLICK.equals(nativeEvent.getType())) {
                            if (nativeEvent.getCtrlKey()) {
                                MarkRoleDTO value = event.getValue();
                                refreshableSelectionModel.setSelected(value,
                                        !refreshableSelectionModel.isSelected(value));
                                return SelectAction.IGNORE;
                            }
                            if (!refreshableSelectionModel.getSelectedSet().isEmpty()
                                    && !isCheckboxColumn(event.getColumn())) {
                                return SelectAction.DEFAULT;
                            }
                        }
                        return SelectAction.TOGGLE;
                    }

                    private boolean isCheckboxColumn(int columnIndex) {
                        return columnIndex == 0;
                    }
                }));

        initTableColumns(sortHandler, userService);
        markRoleListDataProvider.addDataDisplay(markRolesTable);
        add(markRolesTable);
        allMarkRoles.clear();
        allMarkRoles.addAll(markRoleListDataProvider.getList());
    }

    private void initTableColumns(final ListHandler<MarkRoleDTO> sortHandler, final UserService userService) {
        final SelectionCheckboxColumn<MarkRoleDTO> checkColumn = new SelectionCheckboxColumn<MarkRoleDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<MarkRoleDTO>() {
                    @Override
                    public boolean representSameEntity(MarkRoleDTO dto1, MarkRoleDTO dto2) {
                        return dto1.getUuid().equals(dto2.getUuid());
                    }
                    @Override
                    public int hashCode(MarkRoleDTO t) {
                        return t.getUuid().hashCode();
                    }
                }, filterableMarkRoles.getAllListDataProvider());
        final Header<Boolean> selectAllHeader = checkColumn.createHeader();
        markRolesTable.addColumn(checkColumn, selectAllHeader);
        markRolesTable.setColumnWidth(checkColumn, 40, Unit.PX);
        // id
        Column<MarkRoleDTO, String> idColumn = new Column<MarkRoleDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkRoleDTO markRole) {
                return markRole.getUuid().toString();
            }
        };
        // name
        Column<MarkRoleDTO, String> nameColumn = new Column<MarkRoleDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkRoleDTO markRole) {
                return markRole.getName();
            }
        };
        nameColumn.setSortable(true);
        sortHandler.setComparator(nameColumn, new Comparator<MarkRoleDTO>() {
            public int compare(MarkRoleDTO markRole1, MarkRoleDTO markRole2) {
                return markRole1.getName().compareTo(markRole2.getName());
            }
        });
        // short name
        Column<MarkRoleDTO, String> shortNameColumn = new Column<MarkRoleDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkRoleDTO markRole) {
                return markRole.getShortName();
            }
        };
        shortNameColumn.setSortable(true);
        sortHandler.setComparator(shortNameColumn, new Comparator<MarkRoleDTO>() {
            public int compare(MarkRoleDTO markRole1, MarkRoleDTO markRole2) {
                return markRole1.getShortName().compareTo(markRole2.getShortName());
            }
        });
        markRolesTable.addColumn(nameColumn, stringMessages.name());
        markRolesTable.addColumn(shortNameColumn, stringMessages.shortName());
        SecuredDTOOwnerColumn.configureOwnerColumns(markRolesTable, sortHandler, stringMessages);
        final AccessControlledActionsColumn<MarkRoleDTO, DefaultActionsImagesBarCell> actionsColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        final EditOwnershipDialog.DialogConfig<MarkRoleDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), SecuredDomainType.MARK_ROLE, markRole -> markRoleListDataProvider.refresh(), stringMessages);
        final EditACLDialog.DialogConfig<MarkRoleDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), SecuredDomainType.MARK_ROLE,
                markRole -> markRole.getAccessControlList(), stringMessages);
        actionsColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                markRole -> configACL.openDialog(markRole));
        markRolesTable.addColumn(idColumn, stringMessages.id());
        markRolesTable.addColumn(actionsColumn, stringMessages.actions());
        refreshableSelectionModel = checkColumn.getSelectionModel();
        markRolesTable.setSelectionModel(refreshableSelectionModel, checkColumn.getSelectionManager());
    }

    private void openEditMarkRoleDialog(final MarkRoleDTO originalMarkRole) {
        final MarkRoleEditDialog dialog = new MarkRoleEditDialog(stringMessages, originalMarkRole,
                new DialogCallback<MarkRoleDTO>() {
                    @Override
                    public void ok(MarkRoleDTO markRole) {
                        sailingServiceWrite.createMarkRole(markRole, new AsyncCallback<MarkRoleDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter
                                        .reportError("Error trying to update mark properties: " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(MarkRoleDTO updatedMarkRole) {
                                int editedMarkPropertiesIndex = filterableMarkRoles.indexOf(originalMarkRole);
                                filterableMarkRoles.remove(originalMarkRole);
                                if (editedMarkPropertiesIndex >= 0) {
                                    filterableMarkRoles.add(editedMarkPropertiesIndex, updatedMarkRole);
                                } else {
                                    filterableMarkRoles.add(updatedMarkRole);
                                }
                                markRoleListDataProvider.refresh();
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.ensureDebugId("MarkRoleEditDialog");
        dialog.show();
    }

    @Override
    public AbstractFilterablePanel<MarkRoleDTO> getFilterablePanel() {
        return filterableMarkRoles;
    }
}
