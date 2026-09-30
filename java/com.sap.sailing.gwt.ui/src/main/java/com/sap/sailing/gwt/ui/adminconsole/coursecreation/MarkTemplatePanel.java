package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
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
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.AccessControlledButtonPanel;
import com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public class MarkTemplatePanel extends FlowPanel implements FilterablePanelProvider<MarkTemplateDTO>{
    private static AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);

    private final SailingServiceWriteAsync sailingServiceWrite;
    private final LabeledAbstractFilterablePanel<MarkTemplateDTO> filterableMarkTemplates;
    private List<MarkTemplateDTO> allMarkTemplates;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private FlushableCellTable<MarkTemplateDTO> markTemplateTable;
    private ListDataProvider<MarkTemplateDTO> markTemplateListDataProvider = new ListDataProvider<>();
    private RefreshableMultiSelectionModel<MarkTemplateDTO> refreshableSelectionModel;

    public MarkTemplatePanel(SailingServiceWriteAsync sailingServiceWrite, ErrorReporter errorReporter,
            StringMessages stringMessages, final UserService userService) {
        this.sailingServiceWrite = sailingServiceWrite;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        AccessControlledButtonPanel buttonAndFilterPanel = new AccessControlledButtonPanel(userService,
                SecuredDomainType.MARK_TEMPLATE);
        add(buttonAndFilterPanel);
        allMarkTemplates = new ArrayList<>();
        buttonAndFilterPanel.addUnsecuredAction(stringMessages.refresh(), new Command() {
            @Override
            public void execute() {
                loadMarkTemplates();
            }
        });
        buttonAndFilterPanel.addCreateAction(stringMessages.add(), new Command() {
            @Override
            public void execute() {
                openEditMarkTemplateDialog(new MarkTemplateDTO());
            }
        });
        Label lblFilterRaces = new Label(stringMessages.filterMarkTemplateByName() + ":");
        lblFilterRaces.setWordWrap(false);
        buttonAndFilterPanel.addUnsecuredWidget(lblFilterRaces);
        this.filterableMarkTemplates = new LabeledAbstractFilterablePanel<MarkTemplateDTO>(lblFilterRaces,
                allMarkTemplates, markTemplateListDataProvider, stringMessages) {
            @Override
            public List<String> getSearchableStrings(MarkTemplateDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.getCommonMarkProperties().getShortName());
                strings.add(t.getUuid().toString());
                return strings;
            }

            @Override
            public AbstractCellTable<MarkTemplateDTO> getCellTable() {
                return markTemplateTable;
            }
        };
        createMarkTemplatesTable(userService);
        filterableMarkTemplates.getTextBox().ensureDebugId("MarkTemplatesFilterTextBox");
        buttonAndFilterPanel.addUnsecuredWidget(filterableMarkTemplates);
        filterableMarkTemplates
                .setUpdatePermissionFilterForCheckbox(event -> userService.hasPermission(event, DefaultActions.UPDATE));
    }

    public void loadMarkTemplates() {
        markTemplateListDataProvider.getList().clear();
        sailingServiceWrite.getMarkTemplates(new AsyncCallback<List<MarkTemplateDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.toString());
            }

            @Override
            public void onSuccess(List<MarkTemplateDTO> result) {
                markTemplateListDataProvider.getList().clear();
                Util.addAll(result, markTemplateListDataProvider.getList());
                filterableMarkTemplates.updateAll(markTemplateListDataProvider.getList());
                markTemplateListDataProvider.refresh();
            }
        });
    }

    private void createMarkTemplatesTable(final UserService userService) {
        markTemplateTable = new FlushableCellTable<>(1000, tableResources);
        markTemplateTable.setWidth("100%");
        // Attach a column sort handler to the ListDataProvider to sort the list.
        ListHandler<MarkTemplateDTO> sortHandler = new ListHandler<>(markTemplateListDataProvider.getList());
        markTemplateTable.addColumnSortHandler(sortHandler);
        // Initialize the columns.
        initTableColumns(sortHandler, userService);
        markTemplateListDataProvider.addDataDisplay(markTemplateTable);
        add(markTemplateTable);
        allMarkTemplates.clear();
        allMarkTemplates.addAll(markTemplateListDataProvider.getList());
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns(final ListHandler<MarkTemplateDTO> sortHandler, final UserService userService) {
        final SelectionCheckboxColumn<MarkTemplateDTO> checkColumn = new SelectionCheckboxColumn<MarkTemplateDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<MarkTemplateDTO>() {
                    @Override
                    public boolean representSameEntity(MarkTemplateDTO dto1, MarkTemplateDTO dto2) {
                        return dto1.getUuid().equals(dto2.getUuid());
                    }
                    @Override
                    public int hashCode(MarkTemplateDTO t) {
                        return t.getUuid().hashCode();
                    }
                }, filterableMarkTemplates.getAllListDataProvider());
        Header<Boolean> selectAllHeader = checkColumn.createHeader();
        markTemplateTable.addColumn(checkColumn, selectAllHeader);
        markTemplateTable.setColumnWidth(checkColumn, 40, Unit.PX);
        // id
        Column<MarkTemplateDTO, String> idColumn = new Column<MarkTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkTemplateDTO markTemplate) {
                return markTemplate.getUuid().toString();
            }
        };
        // name
        Column<MarkTemplateDTO, String> nameColumn = new Column<MarkTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkTemplateDTO markTemplate) {
                return markTemplate.getName();
            }
        };
        // short name
        Column<MarkTemplateDTO, String> shortNameColumn = new Column<MarkTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkTemplateDTO markTemplate) {
                return markTemplate.getCommonMarkProperties().getShortName();
            }
        };
        // color
        Column<MarkTemplateDTO, String> colorColumn = new Column<MarkTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkTemplateDTO markTemplate) {
                return markTemplate.getCommonMarkProperties().getColor() != null
                        ? markTemplate.getCommonMarkProperties().getColor().getAsHtml()
                        : "";
            }
        };
        // shape
        Column<MarkTemplateDTO, String> shapeColumn = new Column<MarkTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkTemplateDTO markTemplate) {
                return markTemplate.getCommonMarkProperties().getShape();
            }
        };
        // pattern
        Column<MarkTemplateDTO, String> patternColumn = new Column<MarkTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkTemplateDTO markTemplate) {
                return markTemplate.getCommonMarkProperties().getPattern();
            }
        };
        // mark type
        Column<MarkTemplateDTO, String> typeColumn = new Column<MarkTemplateDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkTemplateDTO markTemplate) {
                return markTemplate.getCommonMarkProperties().getType() != null
                        ? markTemplate.getCommonMarkProperties().getType().name()
                        : "";
            }
        };
        nameColumn.setSortable(true);
        sortHandler.setComparator(nameColumn, new Comparator<MarkTemplateDTO>() {
            public int compare(MarkTemplateDTO markTemplate1, MarkTemplateDTO markTemplate2) {
                return markTemplate1.getName().compareTo(markTemplate2.getName());
            }
        });
        markTemplateTable.addColumn(nameColumn, stringMessages.name());
        markTemplateTable.addColumn(shortNameColumn, stringMessages.shortName());
        markTemplateTable.addColumn(colorColumn, stringMessages.color());
        markTemplateTable.addColumn(shapeColumn, stringMessages.shape());
        markTemplateTable.addColumn(patternColumn, stringMessages.pattern());
        markTemplateTable.addColumn(typeColumn, stringMessages.type());
        SecuredDTOOwnerColumn.configureOwnerColumns(markTemplateTable, sortHandler, stringMessages);
        final HasPermissions type = SecuredDomainType.MARK_TEMPLATE;
        final AccessControlledActionsColumn<MarkTemplateDTO, DefaultActionsImagesBarCell> actionsColumn = create(
                new DefaultActionsImagesBarCell(stringMessages), userService);
        final EditOwnershipDialog.DialogConfig<MarkTemplateDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, markTemplate -> markTemplateListDataProvider.refresh(), stringMessages);
        final EditACLDialog.DialogConfig<MarkTemplateDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, markTemplate -> markTemplate.getAccessControlList(),
                stringMessages);
        actionsColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                markTemplate -> configACL.openDialog(markTemplate));
        markTemplateTable.addColumn(idColumn, stringMessages.id());
        markTemplateTable.addColumn(actionsColumn, stringMessages.actions());
        refreshableSelectionModel = checkColumn.getSelectionModel();
        markTemplateTable.setSelectionModel(refreshableSelectionModel, checkColumn.getSelectionManager());
    }

    public void refreshMarkTemplates() {
        loadMarkTemplates();
    }

    void openEditMarkTemplateDialog(final MarkTemplateDTO originalMarkTemplate) {
        final MarkTemplateEditDialog dialog = new MarkTemplateEditDialog(stringMessages, originalMarkTemplate,
                new DialogCallback<MarkTemplateDTO>() {
                    @Override
                    public void ok(MarkTemplateDTO markTemplate) {
                        sailingServiceWrite.addOrUpdateMarkTemplate(markTemplate, new AsyncCallback<MarkTemplateDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter
                                        .reportError("Error trying to update mark template: " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(MarkTemplateDTO updatedMarkTemplate) {
                                int editedMarkTemplateIndex = filterableMarkTemplates.indexOf(originalMarkTemplate);
                                filterableMarkTemplates.remove(originalMarkTemplate);
                                if (editedMarkTemplateIndex >= 0) {
                                    filterableMarkTemplates.add(editedMarkTemplateIndex, updatedMarkTemplate);
                                } else {
                                    filterableMarkTemplates.add(updatedMarkTemplate);
                                }
                                markTemplateListDataProvider.refresh();
                            }
                        });
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.ensureDebugId("MarkTemplateEditDialog");
        dialog.show();
    }

    @Override
    public AbstractFilterablePanel<MarkTemplateDTO> getFilterablePanel() {
        return filterableMarkTemplates;
    }
}
