package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.CellPreviewEvent;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithToolTipDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.ColorColumn.ColorRetriever;
import com.sap.sailing.gwt.ui.client.FlagImageRenderer;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
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

/**
 * A filterable competitor table. The data model is managed by the {@link #getFilterField() filter field}. In
 * order to set an initial set of competitors to display by this table, use {@link #refreshCompetitorList(Iterable)}.
 * The selected competitors can be obtained from the {@link #getSelectionModel() selection model}. The competitor
 * set can also be updated to that of a leaderboard by using {@link #refreshCompetitorList(String)}, providing the
 * leaderboard name as parameter. The competitors currently in the table (regardless of the current filter settings)
 * are returned by {@link #getAllCompetitors()}.<p>
 * 
 * The table shows columns for boat data such as the boat class and the sail number; those columns will be populated
 * only for {@link CompetitorDTO} objects with are also instance of {@link CompetitorWithBoatDTO}.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <S>
 */
public class CompetitorTableWrapper<S extends RefreshableSelectionModel<CompetitorDTO>> extends TableWrapper<CompetitorDTO, S> {
    private final LabeledAbstractFilterablePanel<CompetitorDTO> filterField;
    private final boolean filterCompetitorsWithBoat;
    private final boolean filterCompetitorsWithoutBoat;
    private final Refresher<CompetitorDTO> competitorsRefresher;
    private final Refresher<BoatDTO> boatsRefresher;
    
    /**
     * @param filterCompetitorsWithBoat
     *            relevant if {@link #refreshCompetitorList(String)} or {@link #refreshCompetitorList(String, Callback)}
     *            will be called using {@code null} for the leaderboard name, hence requesting <em>all</em> competitors
     *            to be loaded. In this case, competitors with boat will be fetched only if this flag is
     *            {@code false}.
     * @param filterCompetitorsWithoutBoat
     *            relevant if {@link #refreshCompetitorList(String)} or {@link #refreshCompetitorList(String, Callback)}
     *            will be called using {@code null} for the leaderboard name, hence requesting <em>all</em> competitors
     *            to be loaded. In this case, competitors without boat will be fetched only if this flag is
     *            {@code false}.
     */
    public CompetitorTableWrapper(SailingServiceWriteAsync sailingServiceWrite, UserService userService,
            Refresher<CompetitorDTO> competitorsRefresher, Refresher<BoatDTO> boatsRefresher,
            StringMessages stringMessages, ErrorReporter errorReporter, boolean multiSelection, boolean enablePager,
            boolean filterCompetitorsWithBoat, boolean filterCompetitorsWithoutBoat) {
        super(sailingServiceWrite, stringMessages, errorReporter, multiSelection, enablePager,
                new EntityIdentityComparator<CompetitorDTO>() {
                    @Override
                    public boolean representSameEntity(CompetitorDTO dto1, CompetitorDTO dto2) {
                        return Util.equalsWithNull(dto1.getIdAsString(), dto2.getIdAsString());
                    }
                    @Override
                    public int hashCode(CompetitorDTO t) {
                        return t.getIdAsString() != null ? t.getIdAsString().hashCode() : 0;
                    }
                });
        this.competitorsRefresher = competitorsRefresher;
        this.boatsRefresher = boatsRefresher;
        this.filterCompetitorsWithBoat = filterCompetitorsWithBoat;
        this.filterCompetitorsWithoutBoat = filterCompetitorsWithoutBoat;
        ListHandler<CompetitorDTO> competitorColumnListHandler = getColumnSortHandler();
        // competitors table
        TextColumn<CompetitorDTO> competitorNameColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getName();
            }
        };
        competitorNameColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorNameColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/*case sensitive*/ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getName(), o2.getName());
            }
        });
        TextColumn<CompetitorDTO> competitorShortNameColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getShortName();
            }
        };
        competitorShortNameColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorShortNameColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getShortName(), o2.getShortName());
            }
        });
        TextColumn<CompetitorDTO> boatClassColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.hasBoat() 
                        && ((CompetitorWithBoatDTO) competitor).getBoatClass() != null
                            ? ((CompetitorWithBoatDTO) competitor).getBoatClass().getName()
                            : "";
            }
        };
        boatClassColumn.setSortable(true);
        competitorColumnListHandler.setComparator(boatClassColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* caseSensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                BoatDTO boat1 = o1.hasBoat() ? ((CompetitorWithBoatDTO) o1).getBoat() : null;
                BoatDTO boat2 = o2.hasBoat() ? ((CompetitorWithBoatDTO) o2).getBoat() : null;
                if (boat1 == null && boat2 == null) {
                    return 0;
                } else if (boat1 != null && boat2 == null) { 
                    return 1;
                } else if (boat1 == null && boat2 != null) {
                    return -1;
                }
                return comparator.compare(boat1.getBoatClass().getName(), boat2.getBoatClass().getName());
            }
        });
        Column<CompetitorDTO, SafeHtml> flagImageColumn = new Column<CompetitorDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(CompetitorDTO competitor) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                final String twoLetterIsoCountryCode = competitor.getTwoLetterIsoCountryCode();
                final String flagImageURL = competitor.getFlagImageURL();
                if (flagImageURL != null && !flagImageURL.isEmpty()) {
                    sb.append(FlagImageRenderer.imageWithTitle(flagImageURL, competitor.getName()));
                    sb.appendHtmlConstant("&nbsp;");
                } else {
                    final ImageResource flagImageResource;
                    if (twoLetterIsoCountryCode==null || twoLetterIsoCountryCode.isEmpty()) {
                        flagImageResource = FlagImageResolverImpl.get().getEmptyFlagImageResource();
                    } else {
                        flagImageResource = FlagImageResolverImpl.get().getFlagImageResource(twoLetterIsoCountryCode);
                    }
                    if (flagImageResource != null) {
                        sb.append(FlagImageRenderer.imageWithTitle(flagImageResource.getSafeUri().asString() ,competitor.getName()));
                        sb.appendHtmlConstant("&nbsp;");
                    }
                }
                return sb.toSafeHtml();
            }
        };
        flagImageColumn.setSortable(true);
        competitorColumnListHandler.setComparator(flagImageColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getThreeLetterIocCountryCode(), o2.getThreeLetterIocCountryCode());
            }
        });
        TextColumn<CompetitorDTO> sailIdColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.hasBoat() ? ((CompetitorWithBoatDTO) competitor).getSailID() : "";
            }
        };
        sailIdColumn.setSortable(true);
        competitorColumnListHandler.setComparator(sailIdColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                BoatDTO boat1 = o1.hasBoat() ? ((CompetitorWithBoatDTO) o1).getBoat() : null;
                BoatDTO boat2 = o2.hasBoat() ? ((CompetitorWithBoatDTO) o2).getBoat() : null;
                if (boat1 == null && boat2 == null) {
                    return 0;
                } else if (boat1 != null && boat2 == null) { 
                    return 1;
                } else if (boat1 == null && boat2 != null) {
                    return -1;
                }
                return comparator.compare(boat1.getSailId(), boat2.getSailId());
            }
        });
        Column<CompetitorDTO, SafeHtml> displayColorColumn = new ColorColumn<>(new ColorRetriever<CompetitorDTO>() {
            @Override
            public Color getColor(CompetitorDTO t) {
                return t.getColor();
            }
        });
        Column<CompetitorDTO, SafeHtml> imageColumn = new Column<CompetitorDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(CompetitorDTO competitor) {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                if (competitor.getImageURL() != null && !competitor.getImageURL().isEmpty()) {
                    sb.appendHtmlConstant("<img src=\"" + competitor.getImageURL() + "\" height=\"40px\" title=\""
                            + competitor.getImageURL() + "\"/>");
                }
                return sb.toSafeHtml();
            }
        };
        imageColumn.setSortable(true);
        competitorColumnListHandler.setComparator(imageColumn, new Comparator<CompetitorDTO>() {
            private final NaturalComparator comparator = new NaturalComparator(/* case sensitive */ false);
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return comparator.compare(o1.getImageURL(), o2.getImageURL());
            }
        });
        TextColumn<CompetitorDTO> competitorIdColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getIdAsString();
            }
        };
        competitorIdColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorIdColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return new NaturalComparator(false).compare(o1.getIdAsString(), o2.getIdAsString());
            }
        });
        TextColumn<CompetitorDTO> competitorEMailColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getEmail();
            }
        };
        competitorEMailColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorEMailColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return new NaturalComparator(false).compare(o1.getEmail(), o2.getEmail());
            }
        });
        TextColumn<CompetitorDTO> competitorSearchTagColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getSearchTag();
            }
        };
        competitorSearchTagColumn.setSortable(true);
        competitorColumnListHandler.setComparator(competitorSearchTagColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return new NaturalComparator(false).compare(o1.getSearchTag(), o2.getSearchTag());
            }
        });

        TextColumn<CompetitorDTO> timeOnTimeFactorColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getTimeOnTimeFactor()==null?"":(""+competitor.getTimeOnTimeFactor());
            }
        };
        timeOnTimeFactorColumn.setSortable(true);
        competitorColumnListHandler.setComparator(timeOnTimeFactorColumn, new Comparator<CompetitorDTO>() {
            @Override
            public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                return o1.getTimeOnTimeFactor()==null?o2.getTimeOnTimeFactor()==null?0:-1:o2.getTimeOnTimeFactor()==null?1:
                    o1.getTimeOnTimeFactor().compareTo(o2.getTimeOnTimeFactor());
            }
        });
        TextColumn<CompetitorDTO> timeOnDistanceAllowancePerNauticalMileColumn = new TextColumn<CompetitorDTO>() {
            @Override
            public String getValue(CompetitorDTO competitor) {
                return competitor.getTimeOnDistanceAllowancePerNauticalMile()==null?"":(""+competitor.getTimeOnDistanceAllowancePerNauticalMile());
            }
        };
        timeOnDistanceAllowancePerNauticalMileColumn.setSortable(true);
        competitorColumnListHandler.setComparator(timeOnDistanceAllowancePerNauticalMileColumn, new Comparator<CompetitorDTO>() {
            @Override
                    public int compare(CompetitorDTO o1, CompetitorDTO o2) {
                        return o1.getTimeOnDistanceAllowancePerNauticalMile() == null ? o2
                                .getTimeOnDistanceAllowancePerNauticalMile() == null ? 0 : -1 : o2
                                .getTimeOnDistanceAllowancePerNauticalMile() == null ? 1 : o1
                                .getTimeOnDistanceAllowancePerNauticalMile().compareTo(
                                        o2.getTimeOnDistanceAllowancePerNauticalMile());
                    }
        });
        filterField = new LabeledAbstractFilterablePanel<CompetitorDTO>(new Label(getStringMessages().filterCompetitors()),
                Collections.emptySet(), dataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(CompetitorDTO t) {
                List<String> string = new ArrayList<String>();
                string.add(t.getName());
                string.add(t.getShortName());
                string.add(t.getIdAsString());
                string.add(t.getSearchTag());
                if (t.hasBoat()) {
                    string.add(((CompetitorWithBoatDTO) t).getBoatClass().getName());
                    string.add(((CompetitorWithBoatDTO) t).getSailID());
                }
                return string;
            }

            @Override
            public AbstractCellTable<CompetitorDTO> getCellTable() {
                return table;
            }
        };
        filterField.setUpdatePermissionFilterForCheckbox(comp -> userService.hasPermission(comp, DefaultActions.UPDATE));
        registerSelectionModelOnNewDataProvider(filterField.getAllListDataProvider());
        // CompetitorTableEditFeatures
        final HasPermissions type = SecuredDomainType.COMPETITOR;
        final AccessControlledActionsColumn<CompetitorDTO, CompetitorConfigImagesBarCell> competitorActionColumn = create(
                new CompetitorConfigImagesBarCell(getStringMessages()), userService);
        competitorActionColumn.addAction(CompetitorConfigImagesBarCell.ACTION_UPDATE, HasPermissions.DefaultActions.UPDATE, this::editCompetitor);
        competitorActionColumn.addAction(CompetitorConfigImagesBarCell.ACTION_REFRESH, this::allowUpdate);
        final DialogConfig<CompetitorDTO> editOwnerShipDialog = EditOwnershipDialog.create(userService.getUserManagementWriteService(), SecuredDomainType.COMPETITOR,
                competitorDTO -> getFilterField().filter(), stringMessages);
        competitorActionColumn.addAction(CompetitorConfigImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP,
                editOwnerShipDialog::openOwnershipDialog);
        final EditACLDialog.DialogConfig<CompetitorDTO> configACL = EditACLDialog
                .create(userService.getUserManagementWriteService(), type, null, stringMessages);
        competitorActionColumn.addAction(CompetitorConfigImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                configACL::openDialog);
        mainPanel.insert(filterField, 0);
        table.addColumnSortHandler(competitorColumnListHandler);
        table.addColumn(competitorNameColumn, getStringMessages().name());
        table.addColumn(competitorShortNameColumn, stringMessages.shortName());
        table.addColumn(flagImageColumn, stringMessages.flags());
        table.addColumn(timeOnTimeFactorColumn, getStringMessages().timeOnTimeFactor());
        table.addColumn(timeOnDistanceAllowancePerNauticalMileColumn, getStringMessages().timeOnDistanceAllowanceInSecondsPerNauticalMile());
        table.addColumn(displayColorColumn, getStringMessages().color());
        table.addColumn(imageColumn, getStringMessages().image());
        table.addColumn(competitorEMailColumn, getStringMessages().email());
        table.addColumn(competitorSearchTagColumn, getStringMessages().searchTag());
        table.addColumn(competitorIdColumn, getStringMessages().id());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, getColumnSortHandler(), stringMessages);
        table.addColumn(competitorActionColumn, getStringMessages().actions());
        table.addColumn(sailIdColumn, getStringMessages().sailNumber());
        table.addColumn(boatClassColumn, getStringMessages().boatClass());
        table.ensureDebugId("CompetitorsTable");
    }
    
    public Iterable<CompetitorDTO> getAllCompetitors() {
        return filterField.getAll();
    }
    
    public LabeledAbstractFilterablePanel<CompetitorDTO> getFilterField() {
        return filterField;
    }
    
    /**
     * Updates the table's data model by filtering the {@code competitors} based on the settings in the fields
     * {@link #filterCompetitorsWithBoat} and {@link #filterCompetitorsWithoutBoat}.
     */
    public void refreshCompetitorList(Iterable<? extends CompetitorDTO> competitors) {
        getFilteredCompetitors(Util.filter(competitors, c->(!c.hasBoat() || !filterCompetitorsWithBoat) && (c.hasBoat() || !filterCompetitorsWithoutBoat)));
    }
    
    public void refreshCompetitorList(String leaderboardName) {
        refreshCompetitorList(leaderboardName, null);
    }
    
    /**
     * @param leaderboardName
     *            If {@code null}, all existing competitors are loaded.
     * @param callback
     *            if not {@code null}, its {@code AsyncCallback#onSuccess(Object)} method will be called after
     *            successfully refreshing the competitors
     */
    public void refreshCompetitorList(String leaderboardName, final Callback<Iterable<CompetitorDTO>, Throwable> callback) {
        final AsyncCallback<Iterable<CompetitorDTO>> myCallback = new AsyncCallback<Iterable<CompetitorDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Remote Procedure Call getCompetitors() - Failure: " + caught.getMessage());
                if (callback != null) {
                    callback.onFailure(caught);
                }
            }

            @Override
            public void onSuccess(Iterable<CompetitorDTO> result) {
                getFilteredCompetitors(result);
                refreshCompetitorList(result);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }
        };
        if (leaderboardName != null) {
            sailingServiceWrite.getCompetitorsOfLeaderboard(leaderboardName, myCallback);
        } else {
            if (competitorsRefresher != null) {
                // Don't fetch from server but ask our unified data model to deliver the competitors without forcing server
                // round-trip unless the competitors haven't been loaded at all so far
                competitorsRefresher.callFillAndReloadInitially(competitors->myCallback.onSuccess(competitors));
            } else {
                sailingServiceWrite.getCompetitors(filterCompetitorsWithBoat, filterCompetitorsWithoutBoat, myCallback);
            }
        }
    }

    private void getFilteredCompetitors(Iterable<? extends CompetitorDTO> result) {
        filterField.updateAll(result);
    }

    private void editCompetitor(final CompetitorDTO competitor) {
        final String boatClassName;
        if (competitor.hasBoat()) {
            BoatClassDTO boatClass = ((CompetitorWithBoatDTO) competitor).getBoatClass();
            boatClassName = boatClass != null ? boatClass.getName() : null;
        } else {
            boatClassName = null;
        }
        if (boatClassName != null) {
            openEditCompetitorWithBoatDialog((CompetitorWithBoatDTO) competitor, boatClassName);
        } else {
            openEditCompetitorWithoutBoatDialog(competitor);
        }
    }

    void openEditCompetitorWithBoatDialog(final CompetitorWithBoatDTO originalCompetitor, String boatClassName) {
        final CompetitorWithBoatEditDialog dialog = new CompetitorWithBoatEditDialog(getStringMessages(), 
                originalCompetitor, new DialogCallback<CompetitorWithBoatDTO>() {
            @Override
            public void ok(final CompetitorWithBoatDTO competitor) {
                sailingServiceWrite.addOrUpdateCompetitorWithBoat(competitor, new AsyncCallback<CompetitorWithBoatDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to update competitor with boat: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(CompetitorWithBoatDTO updatedCompetitor) {
                        if (competitorsRefresher != null) {
                            competitorsRefresher.addIfNotContainedElseReplace(updatedCompetitor, new EntityIdentityComparator<CompetitorDTO>() {
                                @Override
                                public boolean representSameEntity(CompetitorDTO dto1, CompetitorDTO dto2) {
                                    return Util.equalsWithNull(dto1.getIdAsString(), dto2.getIdAsString());
                                }
                                @Override
                                public int hashCode(CompetitorDTO t) {
                                    return t.getIdAsString() != null ? t.getIdAsString().hashCode() : 0;
                                }
                            });
                            if (boatsRefresher != null) {
                                boatsRefresher.addIfNotContainedElseReplace(updatedCompetitor.getBoat(), new EntityIdentityComparator<BoatDTO>() {
                                    @Override
                                    public boolean representSameEntity(BoatDTO dto1, BoatDTO dto2) {
                                        return Util.equalsWithNull(dto1.getIdAsString(), dto2.getIdAsString());
                                    }
                                    @Override
                                    public int hashCode(BoatDTO t) {
                                        return t.getIdAsString() != null ? t.getIdAsString().hashCode() : 0;
                                    }
                                });
                                boatsRefresher.callAllFill();
                            }
                        }
                        
                        //only reload selected competitors reloading with refreshCompetitorList(leaderboardName)
                        //would not work in case the list is not based on a leaderboard e.g. AbstractCompetitorRegistrationDialog
                        int editedCompetitorIndex = getFilterField().indexOf(originalCompetitor);
                        getFilterField().remove(originalCompetitor);
                        if (editedCompetitorIndex >= 0){
                            getFilterField().add(editedCompetitorIndex, updatedCompetitor);
                        } else {
                            //in case competitor was not present --> not edit, but create
                            getFilterField().add(updatedCompetitor);
                        }
                        getDataProvider().refresh();
                    }  
                });
            }

            @Override
            public void cancel() {
            }
        },  boatClassName);
        dialog.show();
    }

    void openCompetitorWithBoatAddDialog(final CompetitorWithBoatDTO newCompetitor, boolean createWithBoatByDefault) {
        final CompetitorWithOptionalBoatAddDialog dialog = new CompetitorWithOptionalBoatAddDialog(getStringMessages(),
                newCompetitor, createWithBoatByDefault, new DialogCallback<CompetitorWithBoatDTO>() {
                    @Override
                    public void ok(final CompetitorWithBoatDTO competitor) {
                        if (competitor.hasBoat()) {
                            sailingServiceWrite.addOrUpdateCompetitorWithBoat(competitor, createAddCompetitorCallback());
                        } else {
                            sailingServiceWrite.addOrUpdateCompetitorWithoutBoat(competitor, createAddCompetitorCallback());
                        }
                    }

                    @Override
                    public void cancel() {
                    }
                });
        dialog.show();

    }
    
    void openEditCompetitorWithoutBoatDialog(final CompetitorDTO originalCompetitor) {
        final CompetitorEditDialog<CompetitorDTO> dialog = CompetitorEditDialog.create(getStringMessages(), originalCompetitor, new DialogCallback<CompetitorDTO>() {
            @Override
            public void ok(final CompetitorDTO competitor) {
                sailingServiceWrite.addOrUpdateCompetitorWithoutBoat(competitor, new AsyncCallback<CompetitorDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to update competitor: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(CompetitorDTO updatedCompetitor) {
                        if (competitorsRefresher != null) {
                            competitorsRefresher.addIfNotContainedElseReplace(updatedCompetitor, new EntityIdentityComparator<CompetitorDTO>() {
                                @Override
                                public boolean representSameEntity(CompetitorDTO dto1, CompetitorDTO dto2) {
                                    return Util.equalsWithNull(dto1.getIdAsString(), dto2.getIdAsString());
                                }
                                @Override
                                public int hashCode(CompetitorDTO t) {
                                    return t.getIdAsString() != null ? t.getIdAsString().hashCode() : 0;
                                }
                            });
                        }
                        //only reload selected competitors reloading with refreshCompetitorList(leaderboardName)
                        //would not work in case the list is not based on a leaderboard e.g. AbstractCompetitorRegistrationDialog
                        int editedCompetitorIndex = getFilterField().indexOf(originalCompetitor);
                        getFilterField().remove(originalCompetitor);
                        if (editedCompetitorIndex >= 0){
                            getFilterField().add(editedCompetitorIndex, updatedCompetitor);
                        } else {
                            //in case competitor was not present --> not edit, but create
                            getFilterField().add(updatedCompetitor);
                        }
                        getDataProvider().refresh();
                    }  
                });
            }

            @Override
            public void cancel() {
            }
        });
        dialog.show();
    }

    protected void allowUpdate(final Iterable<CompetitorDTO> competitors) {
        List<CompetitorDTO> serializableSingletonList = new ArrayList<>();
        Util.addAll(competitors, serializableSingletonList);
        sailingServiceWrite.allowCompetitorResetToDefaults(serializableSingletonList, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to allow resetting competitors " + competitors
                        + " to defaults: " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                Notification.notify(getStringMessages().successfullyAllowedCompetitorReset(competitors.toString()), NotificationType.SUCCESS);
            }
        });
    }

    private void allowUpdate(final CompetitorDTO competitor) {
        allowUpdate(Collections.singleton(competitor));
    }

    /**
     * This method makes rows grayed out with a tool tip
     */
    public void grayOutCompetitors(final List<CompetitorWithToolTipDTO> competitors) {
        table.addCellPreviewHandler((CellPreviewEvent<CompetitorDTO> event) -> {
            for (CompetitorWithToolTipDTO competitor : competitors) {
                if (competitor.getCompetitor().equals(event.getValue())) {
                    table.getRowElement(event.getIndex()).setTitle(competitor.getToolTipMessage());
                }
            }
        });
        table.setRowStyles((CompetitorDTO row, int rowIndex) -> {
            for (CompetitorWithToolTipDTO competitor : competitors) {
                if (competitor.getCompetitor().equals(row)) {
                    return getTableRes().cellTableStyle().cellTableDisabledRow();
                }
            }
            return "";
        });
    }

    private <T extends CompetitorDTO> AsyncCallback<T> createAddCompetitorCallback() {
        return new AsyncCallback<T>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to add competitor: " + caught.getMessage());
            }

            @Override
            public void onSuccess(T addedCompetitor) {
                if (competitorsRefresher != null) {
                    competitorsRefresher.add(addedCompetitor);
                }
                if (boatsRefresher != null && addedCompetitor instanceof CompetitorWithBoatDTO) {
                    CompetitorWithBoatDTO competitorWithBoatDTO = (CompetitorWithBoatDTO) addedCompetitor;
                    boatsRefresher.add(competitorWithBoatDTO.getBoat());
                    boatsRefresher.callAllFill();
                }
                getFilterField().add(addedCompetitor);
                getDataProvider().refresh();
            }
        };
    }

}
