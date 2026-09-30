package com.sap.sailing.gwt.ui.adminconsole.coursecreation;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_DELETE;
import static com.sap.sse.security.ui.client.component.DefaultActionsImagesBarCell.ACTION_UPDATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.AdminConsoleResources;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceIdentifierDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPropertiesDTO;
import com.sap.sse.common.Position;
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

public class MarkPropertiesPanel extends FlowPanel implements FilterablePanelProvider<MarkPropertiesDTO>{
    private static AdminConsoleResources resources = GWT.create(AdminConsoleResources.class);
    private static AdminConsoleTableResources tableResources = GWT.create(AdminConsoleTableResources.class);
    private static final AbstractImagePrototype positionImagePrototype = AbstractImagePrototype
            .create(resources.ping());
    private static final AbstractImagePrototype setDeviceIdentifierImagePrototype = AbstractImagePrototype
            .create(resources.mapDevices());

    private final SailingServiceWriteAsync sailingServiceWrite;
    private final LabeledAbstractFilterablePanel<MarkPropertiesDTO> filterableMarkProperties;
    private List<MarkPropertiesDTO> allMarkProperties;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private FlushableCellTable<MarkPropertiesDTO> markPropertiesTable;
    private ListDataProvider<MarkPropertiesDTO> markPropertiesListDataProvider = new ListDataProvider<>();
    private RefreshableMultiSelectionModel<MarkPropertiesDTO> refreshableSelectionModel;

    public MarkPropertiesPanel(SailingServiceWriteAsync sailingServiceWrite, ErrorReporter errorReporter,
            StringMessages stringMessages, final UserService userService) {
        this.sailingServiceWrite = sailingServiceWrite;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        AccessControlledButtonPanel buttonAndFilterPanel = new AccessControlledButtonPanel(userService,
                SecuredDomainType.MARK_TEMPLATE);
        add(buttonAndFilterPanel);
        allMarkProperties = new ArrayList<>();
        Label lblFilterRaces = new Label(stringMessages.filterMarkPropertiesByName() + ":");
        lblFilterRaces.setWordWrap(false);
        this.filterableMarkProperties = new LabeledAbstractFilterablePanel<MarkPropertiesDTO>(lblFilterRaces,
                allMarkProperties, markPropertiesListDataProvider, stringMessages) {
            @Override
            public List<String> getSearchableStrings(MarkPropertiesDTO t) {
                List<String> strings = new ArrayList<String>();
                strings.add(t.getName());
                strings.add(t.getCommonMarkProperties().getShortName());
                strings.add(t.getUuid().toString());
                Util.addAll(t.getTags(), strings);
                return strings;
            }

            @Override
            public AbstractCellTable<MarkPropertiesDTO> getCellTable() {
                return markPropertiesTable;
            }
        };
        createMarkPropertiesTable(userService);
        buttonAndFilterPanel.addUnsecuredAction(stringMessages.refresh(), this::loadMarkProperties);
        buttonAndFilterPanel.addCreateAction(stringMessages.add(),
                () -> openEditMarkPropertiesDialog(new MarkPropertiesDTO()));
        buttonAndFilterPanel.addRemoveAction(stringMessages.remove(), refreshableSelectionModel, true,
                () -> { final List<UUID> uuids = refreshableSelectionModel.getSelectedSet().stream().map(markPropertiesDTO -> markPropertiesDTO.getUuid()).collect(Collectors.toList());
            removeMarkProperties(uuids);
        });
        buttonAndFilterPanel.addUnsecuredWidget(lblFilterRaces);
        filterableMarkProperties.getTextBox().ensureDebugId("MarkPropertiesFilterTextBox");
        buttonAndFilterPanel.addUnsecuredWidget(filterableMarkProperties);
        filterableMarkProperties
                .setUpdatePermissionFilterForCheckbox(event -> userService.hasPermission(event, DefaultActions.UPDATE));
    }

    private void removeMarkProperties(Collection<UUID> markPropertiesUuids) {
        if (!markPropertiesUuids.isEmpty()) {
            sailingServiceWrite.removeMarkProperties(markPropertiesUuids, new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error trying to remove mark properties:" + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    refreshMarkProperties();
                }
            });
        }
    }

    public void loadMarkProperties() {
        markPropertiesListDataProvider.getList().clear();
        sailingServiceWrite.getMarkProperties(new AsyncCallback<List<MarkPropertiesDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(caught.toString());
            }

            @Override
            public void onSuccess(List<MarkPropertiesDTO> result) {
                markPropertiesListDataProvider.getList().clear();
                Util.addAll(result, markPropertiesListDataProvider.getList());
                filterableMarkProperties.updateAll(markPropertiesListDataProvider.getList());
                markPropertiesListDataProvider.refresh();
            }
        });
    }

    private void createMarkPropertiesTable(final UserService userService) {
        markPropertiesTable = new FlushableCellTable<>(1000, tableResources);
        markPropertiesTable.setWidth("100%");
        // Attach a column sort handler to the ListDataProvider to sort the list.
        ListHandler<MarkPropertiesDTO> sortHandler = new ListHandler<>(markPropertiesListDataProvider.getList());
        markPropertiesTable.addColumnSortHandler(sortHandler);
        // Initialize the columns.
        initTableColumns(sortHandler, userService);
        markPropertiesListDataProvider.addDataDisplay(markPropertiesTable);
        add(markPropertiesTable);
        allMarkProperties.clear();
        allMarkProperties.addAll(markPropertiesListDataProvider.getList());
    }

    /**
     * Add the columns to the table.
     */
    private void initTableColumns(final ListHandler<MarkPropertiesDTO> sortHandler, final UserService userService) {
        final SelectionCheckboxColumn<MarkPropertiesDTO> checkColumn = new SelectionCheckboxColumn<MarkPropertiesDTO>(
                tableResources.cellTableStyle().cellTableCheckboxSelected(),
                tableResources.cellTableStyle().cellTableCheckboxDeselected(),
                tableResources.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<MarkPropertiesDTO>() {
                    @Override
                    public boolean representSameEntity(MarkPropertiesDTO dto1, MarkPropertiesDTO dto2) {
                        return dto1.getUuid().equals(dto2.getUuid());
                    }
                    @Override
                    public int hashCode(MarkPropertiesDTO t) {
                        return t.getUuid().hashCode();
                    }
                }, filterableMarkProperties.getAllListDataProvider());
        final Header<Boolean> selectAllHeader = checkColumn.createHeader();
        markPropertiesTable.addColumn(checkColumn, selectAllHeader);
        markPropertiesTable.setColumnWidth(checkColumn, 40, Unit.PX);
        // id
        Column<MarkPropertiesDTO, String> idColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return markProperties.getUuid().toString();
            }
        };
        // name
        Column<MarkPropertiesDTO, String> nameColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return markProperties.getName();
            }
        };
        // short name
        Column<MarkPropertiesDTO, String> shortNameColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return markProperties.getCommonMarkProperties().getShortName();
            }
        };
        // color
        Column<MarkPropertiesDTO, String> colorColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return markProperties.getCommonMarkProperties().getColor() != null
                        ? markProperties.getCommonMarkProperties().getColor().getAsHtml()
                        : "";
            }
        };
        // shape
        Column<MarkPropertiesDTO, String> shapeColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return markProperties.getCommonMarkProperties().getShape();
            }
        };
        // pattern
        Column<MarkPropertiesDTO, String> patternColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return markProperties.getCommonMarkProperties().getPattern();
            }
        };
        // mark type
        Column<MarkPropertiesDTO, String> typeColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return markProperties.getCommonMarkProperties().getType() != null
                        ? markProperties.getCommonMarkProperties().getType().name()
                        : "";
            }
        };
        // tags
        Column<MarkPropertiesDTO, String> tagsColumn = new Column<MarkPropertiesDTO, String>(new TextCell()) {
            @Override
            public String getValue(MarkPropertiesDTO markProperties) {
                return String.join(", ", markProperties.getTags());
            }
        };
        Column<MarkPropertiesDTO, AbstractImagePrototype> positioningColumn = new Column<MarkPropertiesDTO, AbstractImagePrototype>(
                new AbstractCell<AbstractImagePrototype>() {
                    @Override
                    public void render(Context context, AbstractImagePrototype image, SafeHtmlBuilder sb) {
                        if (image != null) sb.append(image.getSafeHtml());
                    }
                }) {
            @Override
            public AbstractImagePrototype getValue(MarkPropertiesDTO markProperties) {
                switch (markProperties.getPositioningType()) {
                case "FIXED_POSITION":
                    return positionImagePrototype;
                case "DEVICE":
                    return setDeviceIdentifierImagePrototype;
                default:
                    return null;
                }
            }
        };
        nameColumn.setSortable(true);
        sortHandler.setComparator(nameColumn, new Comparator<MarkPropertiesDTO>() {
            public int compare(MarkPropertiesDTO markProperties1, MarkPropertiesDTO markProperties2) {
                return markProperties1.getName().compareTo(markProperties2.getName());
            }
        });
        markPropertiesTable.addColumn(nameColumn, stringMessages.name());
        markPropertiesTable.addColumn(shortNameColumn, stringMessages.shortName());
        markPropertiesTable.addColumn(colorColumn, stringMessages.color());
        markPropertiesTable.addColumn(shapeColumn, stringMessages.shape());
        markPropertiesTable.addColumn(patternColumn, stringMessages.pattern());
        markPropertiesTable.addColumn(typeColumn, stringMessages.type());
        markPropertiesTable.addColumn(positioningColumn, stringMessages.position());
        markPropertiesTable.addColumn(tagsColumn, stringMessages.tags());
        SecuredDTOOwnerColumn.configureOwnerColumns(markPropertiesTable, sortHandler, stringMessages);
        final HasPermissions type = SecuredDomainType.MARK_PROPERTIES;
        final AccessControlledActionsColumn<MarkPropertiesDTO, MarkPropertiesImagesbarCell> actionsColumn = create(
                new MarkPropertiesImagesbarCell(stringMessages), userService);
        final EditOwnershipDialog.DialogConfig<MarkPropertiesDTO> configOwnership = EditOwnershipDialog
                .create(userService.getUserManagementWriteService(), type, markProperties -> 
                    markPropertiesListDataProvider.refresh(), stringMessages);
        final EditACLDialog.DialogConfig<MarkPropertiesDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, markProperties -> markProperties.getAccessControlList(),
                stringMessages);
        actionsColumn.addAction(ACTION_DELETE, DELETE, e -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveMarkProperties(e.getName()))) {
                sailingServiceWrite.removeMarkProperties(Collections.singletonList(e.getUuid()), new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError(stringMessages.couldNotRemoveMarkProperties(caught.getMessage()));
                    }

                    @Override
                    public void onSuccess(Void result) {
                        refreshMarkProperties();
                    }
                });
            }
        });
        actionsColumn.addAction(ACTION_UPDATE, UPDATE, this::openEditMarkPropertiesDialog);
        actionsColumn.addAction(MarkPropertiesImagesbarCell.ACTION_SET_DEVICE_IDENTIFIER, UPDATE,
                this::openEditMarkPropertiesDeviceIdentifierDialog);
        actionsColumn.addAction(MarkPropertiesImagesbarCell.ACTION_SET_POSITION, UPDATE,
                this::openEditMarkPropertiesPositionDialog);
        actionsColumn.addAction(MarkPropertiesImagesbarCell.ACTION_UNSET_POSITION, UPDATE,
                this::unsetPosition);
        actionsColumn.addAction(ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP, configOwnership::openOwnershipDialog);
        actionsColumn.addAction(DefaultActionsImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                markProperties -> configACL.openDialog(markProperties));
        markPropertiesTable.addColumn(idColumn, stringMessages.id());
        markPropertiesTable.addColumn(actionsColumn, stringMessages.actions());
        refreshableSelectionModel = checkColumn.getSelectionModel();
        markPropertiesTable.setSelectionModel(checkColumn.getSelectionModel(), checkColumn.getSelectionManager());
    }

    public void refreshMarkProperties() {
        loadMarkProperties();
    }

    void openEditMarkPropertiesDialog(final MarkPropertiesDTO originalMarkProperties) {
        final MarkPropertiesEditDialog dialog = new MarkPropertiesEditDialog(stringMessages, originalMarkProperties,
                new DialogCallback<MarkPropertiesDTO>() {
                    @Override
                    public void ok(MarkPropertiesDTO markProperties) {
                        sailingServiceWrite.addOrUpdateMarkProperties(markProperties,
                                new AsyncCallback<MarkPropertiesDTO>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(
                                                "Error trying to update mark properties: " + caught.getMessage());
                                    }

                                    @Override
                                    public void onSuccess(MarkPropertiesDTO updatedMarkProperties) {
                                        int editedMarkPropertiesIndex = filterableMarkProperties
                                                .indexOf(originalMarkProperties);
                                        filterableMarkProperties.remove(originalMarkProperties);
                                        if (editedMarkPropertiesIndex >= 0) {
                                            filterableMarkProperties.add(editedMarkPropertiesIndex,
                                                    updatedMarkProperties);
                                        } else {
                                            filterableMarkProperties.add(updatedMarkProperties);
                                        }
                                        markPropertiesListDataProvider.refresh();
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.ensureDebugId("MarkPropertiesEditDialog");
        dialog.show();
    }

    void openEditMarkPropertiesDeviceIdentifierDialog(final MarkPropertiesDTO originalMarkProperties) {
        final MarkPropertiesDeviceIdentifierEditDialog dialog = new MarkPropertiesDeviceIdentifierEditDialog(
                stringMessages, null, new DialogCallback<DeviceIdentifierDTO>() {
                    @Override
                    public void ok(DeviceIdentifierDTO deviceIdentifier) {
                        sailingServiceWrite.updateMarkPropertiesPositioning(originalMarkProperties.getUuid(),
                                deviceIdentifier, null, new AsyncCallback<MarkPropertiesDTO>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(stringMessages.errorTryingToUpdateMarkProperties(caught.getMessage()));
                                    }

                                    @Override
                                    public void onSuccess(MarkPropertiesDTO updatedMarkProperties) {
                                        int editedMarkPropertiesIndex = filterableMarkProperties
                                                .indexOf(originalMarkProperties);
                                        filterableMarkProperties.remove(originalMarkProperties);
                                        if (editedMarkPropertiesIndex >= 0) {
                                            filterableMarkProperties.add(editedMarkPropertiesIndex,
                                                    updatedMarkProperties);
                                        } else {
                                            filterableMarkProperties.add(updatedMarkProperties);
                                        }
                                        markPropertiesListDataProvider.refresh();
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.ensureDebugId("MarkPropertiesDeviceIdentifierEditDialog");
        dialog.show();
    }

    private void unsetPosition(final MarkPropertiesDTO originalMarkProperties) {
        if (Window.confirm(stringMessages.confirmUnsettingPositionForMarkProperties(originalMarkProperties.getName()))) {
            sailingServiceWrite.updateMarkPropertiesPositioning(originalMarkProperties.getUuid(), /* no tracking device */ null,
                    /* and no fixed position either */ null, new AsyncCallback<MarkPropertiesDTO>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError(stringMessages.errorTryingToUpdateMarkProperties(caught.getMessage()));
                        }

                        @Override
                        public void onSuccess(MarkPropertiesDTO updatedMarkProperties) {
                            int editedMarkPropertiesIndex = filterableMarkProperties
                                    .indexOf(originalMarkProperties);
                            filterableMarkProperties.remove(originalMarkProperties);
                            if (editedMarkPropertiesIndex >= 0) {
                                filterableMarkProperties.add(editedMarkPropertiesIndex,
                                        updatedMarkProperties);
                            } else {
                                filterableMarkProperties.add(updatedMarkProperties);
                            }
                            markPropertiesListDataProvider.refresh();
                        }
                    });
        }
    }
    
    void openEditMarkPropertiesPositionDialog(final MarkPropertiesDTO originalMarkProperties) {
        final MarkPropertiesPositionEditDialog dialog = new MarkPropertiesPositionEditDialog(stringMessages, null,
                new DialogCallback<Position>() {
                    @Override
                    public void ok(Position fixedPosition) {
                        sailingServiceWrite.updateMarkPropertiesPositioning(originalMarkProperties.getUuid(), null,
                                fixedPosition, new AsyncCallback<MarkPropertiesDTO>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError(stringMessages.errorTryingToUpdateMarkProperties(caught.getMessage()));
                                    }

                                    @Override
                                    public void onSuccess(MarkPropertiesDTO updatedMarkProperties) {
                                        int editedMarkPropertiesIndex = filterableMarkProperties
                                                .indexOf(originalMarkProperties);
                                        filterableMarkProperties.remove(originalMarkProperties);
                                        if (editedMarkPropertiesIndex >= 0) {
                                            filterableMarkProperties.add(editedMarkPropertiesIndex,
                                                    updatedMarkProperties);
                                        } else {
                                            filterableMarkProperties.add(updatedMarkProperties);
                                        }
                                        markPropertiesListDataProvider.refresh();
                                    }
                                });
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.ensureDebugId("MarkPropertiesPositionEditDialog");
        dialog.show();
    }

    private static class MarkPropertiesImagesbarCell extends DefaultActionsImagesBarCell {
        public static final String ACTION_SET_DEVICE_IDENTIFIER = "ACTION_SET_DEVICE_IDENTIFIER";
        public static final String ACTION_SET_POSITION = "ACTION_SET_POSITION";
        public static final String ACTION_UNSET_POSITION = "ACTION_UNSET_POSITION";
        private final StringMessages stringMessages;

        public MarkPropertiesImagesbarCell(StringMessages stringMessages) {
            super(stringMessages);
            this.stringMessages = stringMessages;
        }

        @Override
        protected Iterable<ImageSpec> getImageSpecs() {
            return Arrays.asList(getUpdateImageSpec(),
                    new ImageSpec(ACTION_SET_DEVICE_IDENTIFIER, stringMessages.setDeviceIdentifier(),
                            resources.mapDevices()),
                    new ImageSpec(ACTION_SET_POSITION, stringMessages.setPosition(), resources.ping()),
                    new ImageSpec(ACTION_UNSET_POSITION, stringMessages.unsetPosition(), resources.removePing()),
                    getDeleteImageSpec(), getChangeOwnershipImageSpec(), getChangeACLImageSpec());
        }
    }

    @Override
    public AbstractFilterablePanel<MarkPropertiesDTO> getFilterablePanel() {
        return filterableMarkProperties;
    }
}
