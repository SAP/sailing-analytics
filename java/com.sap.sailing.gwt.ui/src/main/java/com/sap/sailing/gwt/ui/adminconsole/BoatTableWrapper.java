package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.ColorColumn.ColorRetriever;
import com.sap.sailing.gwt.ui.client.Refresher;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.component.AccessControlledActionsColumn;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog;
import com.sap.sse.security.ui.client.component.EditOwnershipDialog.DialogConfig;
import com.sap.sse.security.ui.client.component.SecuredDTOOwnerColumn;
import com.sap.sse.security.ui.client.component.editacl.EditACLDialog;

public class BoatTableWrapper<S extends RefreshableSelectionModel<BoatDTO>> extends TableWrapper<BoatDTO, S> {
    private final LabeledAbstractFilterablePanel<BoatDTO> filterField;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final Refresher<BoatDTO> boatsRefresher;
    private final Refresher<CompetitorDTO> competitorsRefresher;

    public BoatTableWrapper(SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
            Refresher<BoatDTO> boatsRefresher, Refresher<CompetitorDTO> competitorsRefresher,
            StringMessages stringMessages, ErrorReporter errorReporter, boolean multiSelection, boolean enablePager,
            boolean allowActions) {
        this(sailingServiceWrite, userService, boatsRefresher, competitorsRefresher, stringMessages, errorReporter,
                multiSelection, enablePager, DEFAULT_PAGING_SIZE, allowActions);
    }

    public BoatTableWrapper(SailingServiceWriteAsync sailingServiceWrite, final UserService userService,
            Refresher<BoatDTO> boatsRefresher, Refresher<CompetitorDTO> competitorsRefresher,
            StringMessages stringMessages, ErrorReporter errorReporter, boolean multiSelection, boolean enablePager,
            int pagingSize, boolean allowActions) {
        super(sailingServiceWrite, stringMessages, errorReporter, multiSelection, enablePager, pagingSize,
                new EntityIdentityComparator<BoatDTO>() {
                    @Override
                    public boolean representSameEntity(BoatDTO dto1, BoatDTO dto2) {
                        return Util.equalsWithNull(dto1.getIdAsString(), dto2.getIdAsString());
                    }
                    @Override
                    public int hashCode(BoatDTO t) {
                        return t.getIdAsString() != null ? t.getIdAsString().hashCode() : 0;
                    }
                });
        this.boatsRefresher = boatsRefresher;
        this.competitorsRefresher = competitorsRefresher;
        this.sailingServiceWrite = sailingServiceWrite;
        ListHandler<BoatDTO> boatColumnListHandler = getColumnSortHandler();
        // boats table
        TextColumn<BoatDTO> boatNameColumn = new TextColumn<BoatDTO>() {
            @Override
            public String getValue(BoatDTO boat) {
                return boat.getName();
            }
        };
        boatNameColumn.setSortable(true);
        boatColumnListHandler.setComparator(boatNameColumn, new Comparator<BoatDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* caseSensitive */ false);
            @Override
            public int compare(BoatDTO o1, BoatDTO o2) {
                return comparator.compare(o1.getName(), o2.getName());
            }
        });
        TextColumn<BoatDTO> boatClassColumn = new TextColumn<BoatDTO>() {
            @Override
            public String getValue(BoatDTO competitor) {
                return competitor.getBoatClass() != null ? competitor.getBoatClass().getName() : "";
            }
        };
        boatClassColumn.setSortable(true);
        boatColumnListHandler.setComparator(boatClassColumn, new Comparator<BoatDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* caseSensitive */ false);
            @Override
            public int compare(BoatDTO o1, BoatDTO o2) {
                return comparator.compare(o1.getBoatClass().getName(), o2.getBoatClass().getName());
            }
        });
        Column<BoatDTO, SafeHtml> sailIdColumn = new Column<BoatDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(BoatDTO competitor) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendEscaped(competitor.getSailId());
                return sb.toSafeHtml();
            }
        };
        sailIdColumn.setSortable(true);
        boatColumnListHandler.setComparator(sailIdColumn, new Comparator<BoatDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(BoatDTO o1, BoatDTO o2) {
                return comparator.compare(o1.getSailId(), o2.getSailId());
            }
        });
        Column<BoatDTO, SafeHtml> boatColorColumn = new ColorColumn<>(new ColorRetriever<BoatDTO>() {
            @Override
            public Color getColor(BoatDTO t) {
                return t.getColor();
            }
        });
        boatColorColumn.setSortable(true);
        boatColumnListHandler.setComparator(boatColorColumn, new Comparator<BoatDTO>() {
            @Override
            public int compare(BoatDTO o1, BoatDTO o2) {
                if (o1.getColor() == null) {
                    if (o2.getColor() == null) {
                        return 0;
                    }
                    return -1;
                } else if (o2.getColor() == null) {
                    return 1;
                }
                return o1.getColor().getAsHtml().compareTo(o2.getColor().getAsHtml());
            }
        });
        TextColumn<BoatDTO> boatIdColumn = new TextColumn<BoatDTO>() {
            @Override
            public String getValue(BoatDTO boat) {
                return boat.getIdAsString();
            }
        };
        boatIdColumn.setSortable(true);
        boatColumnListHandler.setComparator(boatIdColumn, new Comparator<BoatDTO>() {
            @Override
            public int compare(BoatDTO o1, BoatDTO o2) {
                return new NaturalComparator(false).compare(o1.getIdAsString(), o2.getIdAsString());
            }
        });
        filterField = new LabeledAbstractFilterablePanel<BoatDTO>(new Label(stringMessages.filterBoats()),
                new ArrayList<BoatDTO>(), dataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(BoatDTO boat) {
                List<String> string = new ArrayList<String>();
                string.add(boat.getName());
                string.add(boat.getSailId());
                string.add(boat.getBoatClass().getName());
                string.add(boat.getIdAsString());
                return string;
            }

            @Override
            public AbstractCellTable<BoatDTO> getCellTable() {
                return table;
            }
        };
        
        filterField.setUpdatePermissionFilterForCheckbox(boat -> {
            return boat != null && boat.getIdAsString() != null && userService.hasPermission(boat, DefaultActions.UPDATE);
        });
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        // BoatTable edit features
        final HasPermissions type = SecuredDomainType.BOAT;
        AccessControlledActionsColumn<BoatDTO, BoatConfigImagesBarCell> boatActionColumn = create(
                new BoatConfigImagesBarCell(getStringMessages()), userService);
        boatActionColumn.addAction(BoatConfigImagesBarCell.ACTION_UPDATE, HasPermissions.DefaultActions.UPDATE,
                this::openEditBoatDialog);
        boatActionColumn.addAction(BoatConfigImagesBarCell.ACTION_REFRESH, this::allowUpdate);
        final DialogConfig<BoatDTO> editOwnerShipDialog = EditOwnershipDialog.create(
                userService.getUserManagementWriteService(), SecuredDomainType.BOAT, boatDTO -> getFilterField().filter(), stringMessages);
        boatActionColumn.addAction(BoatConfigImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP,
                editOwnerShipDialog::openOwnershipDialog);
        final EditACLDialog.DialogConfig<BoatDTO> configACL = EditACLDialog
                .create(userService.getUserManagementWriteService(), type, null, stringMessages);
        boatActionColumn.addAction(BoatConfigImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);
        mainPanel.insert(filterField, 0);
        table.addColumnSortHandler(boatColumnListHandler);
        table.addColumn(boatNameColumn, stringMessages.name());
        table.addColumn(sailIdColumn, stringMessages.sailNumber());
        table.addColumn(boatClassColumn, stringMessages.boatClass());
        table.addColumn(boatColorColumn, stringMessages.color());
        table.addColumn(boatIdColumn, stringMessages.id());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, getColumnSortHandler(), stringMessages);
        if (allowActions) {
            table.addColumn(boatActionColumn, stringMessages.actions());
        }
        table.ensureDebugId("BoatsTable");
    }
    
    public Iterable<BoatDTO> getAllBoats() {
        return filterField.getAll();
    }
    
    public LabeledAbstractFilterablePanel<BoatDTO> getFilterField() {
        return filterField;
    }
    
    public void filterBoats(Iterable<BoatDTO> boats) {
        filterField.updateAll(boats);
    }
    
    public void refreshBoatList(boolean loadOnlyStandaloneBoats, final Callback<Iterable<BoatDTO>, Throwable> callback) {
        if (loadOnlyStandaloneBoats) {
            sailingServiceWrite.getStandaloneBoats(new AsyncCallback<Iterable<BoatDTO>>() {
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Remote Procedure Call getBoats() - Failure: " + caught.getMessage());
                    if (callback != null) {
                        callback.onFailure(caught);
                    }
                }

                @Override
                public void onSuccess(Iterable<BoatDTO> result) {
                    filterBoats(result);
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                }
            });
        } else {
            if (boatsRefresher != null) {
                // Don't fetch from server but ask our unified data model to deliver the boats without forcing server
                // round-trip unless the boats haven't been loaded at all so far
                boatsRefresher.callFillAndReloadInitially(boats -> {
                    filterBoats(boats);
                    if (callback != null) {
                        callback.onSuccess(boats);
                    }
                });
            } else {
                sailingServiceWrite.getAllBoats(new AsyncCallback<Iterable<BoatDTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Remote Procedure Call getBoats() - Failure: " + caught.getMessage());
                        if (callback != null) {
                            callback.onFailure(caught);
                        }
                    }
    
                    @Override
                    public void onSuccess(Iterable<BoatDTO> result) {
                        filterBoats(result);
                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }
                });
            }
        }
    }

    void openEditBoatDialog(final BoatDTO originalBoat, String boatClassName) {
        final BoatEditDialog dialog = new BoatEditDialog(getStringMessages(), originalBoat, boatClassName, new DialogCallback<BoatDTO>() {
            @Override
            public void ok(BoatDTO boat) {
                sailingServiceWrite.addOrUpdateBoat(boat, new AsyncCallback<BoatDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to update boat: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(BoatDTO updatedBoat) {
                        if (boatsRefresher != null) {
                            boatsRefresher.addIfNotContainedElseReplace(updatedBoat, new EntityIdentityComparator<BoatDTO>() {
                                @Override
                                public boolean representSameEntity(BoatDTO dto1, BoatDTO dto2) {
                                    return Util.equalsWithNull(dto1.getIdAsString(), dto2.getIdAsString());
                                }
                                @Override
                                public int hashCode(BoatDTO t) {
                                    return t.getIdAsString() != null ? t.getIdAsString().hashCode() : 0;
                                }
                            });
                        }
                        if (competitorsRefresher != null) {
                            competitorsRefresher.reloadAndCallFillAll();
                        }
                        int editedBoatIndex = getFilterField().indexOf(originalBoat);
                        getFilterField().remove(originalBoat);
                        if (editedBoatIndex >= 0){
                            getFilterField().add(editedBoatIndex, updatedBoat);
                        } else {
                            // in case boat was not present --> not edit, but create
                            getFilterField().add(updatedBoat);
                        }
                        getDataProvider().refresh();
                    }  
                });
            }

            @Override
            public void cancel() {
            }
        });
        dialog.ensureDebugId("BoatEditDialog");
        dialog.show();
    }

    void openEditBoatDialog(final BoatDTO originalBoat) {
        openEditBoatDialog(originalBoat, null);
    }

    public void allowUpdate(final Iterable<BoatDTO> boats) {
        List<BoatDTO> serializableSingletonList = new ArrayList<BoatDTO>();
        Util.addAll(boats, serializableSingletonList);
        sailingServiceWrite.allowBoatResetToDefaults(serializableSingletonList, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to allow resetting boats " + boats
                        + " to defaults: " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                Notification.notify(getStringMessages().successfullyAllowedBoatReset(boats.toString()), NotificationType.SUCCESS);
            }
        });
    }

    private void allowUpdate(final BoatDTO boat) {
        allowUpdate(Collections.singleton(boat));
    }
}
