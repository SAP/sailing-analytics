package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.CHANGE_OWNERSHIP;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.DELETE;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.READ;
import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;
import static com.sap.sse.security.ui.client.component.AccessControlledActionsColumn.create;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.RankingMetricTypeFormatter;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
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
 * A composite showing the list of all regattas
 * @author Frank
 */
public class RegattaListComposite extends Composite {

    protected final ListDataProvider<RegattaDTO> regattaListDataProvider;
    protected final StringMessages stringMessages;

    private final CellTable<RegattaDTO> regattaTable;
    private final Label noRegattasLabel;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final RefreshableMultiSelectionModel<RegattaDTO> refreshableRegattaMultiSelectionModel;
    private final ErrorReporter errorReporter;
    private final Presenter presenter;
    protected final LabeledAbstractFilterablePanel<RegattaDTO> filterablePanelRegattas;
    private final UserService userService;
    private List<RegattaDTO> allRegattas;

    protected static AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);

    public static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    private final Displayer<RegattaDTO> regattasDisplayer = new Displayer<RegattaDTO>() {

        @Override
        public void fill(Iterable<RegattaDTO> result) {
            fillRegattas(result);
        }
    };

    public Displayer<RegattaDTO> getRegattasDisplayer() {
        return regattasDisplayer;
    }

    public RegattaListComposite(final Presenter presenter, final StringMessages stringMessages) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.presenter = presenter;
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        this.userService = presenter.getUserService();
        allRegattas = new ArrayList<RegattaDTO>();
        final VerticalPanel panel = new VerticalPanel();
        Label filterRegattasLabel = new Label(stringMessages.filterRegattasByName() + ":");
        filterRegattasLabel.setWordWrap(false);
        noRegattasLabel = new Label(stringMessages.noRegattasYet());
        noRegattasLabel.ensureDebugId("NoRegattasLabel");
        noRegattasLabel.setWordWrap(false);
        panel.add(noRegattasLabel);
        regattaListDataProvider = new ListDataProvider<RegattaDTO>();
        filterablePanelRegattas = new LabeledAbstractFilterablePanel<RegattaDTO>(filterRegattasLabel, allRegattas,
                regattaListDataProvider, stringMessages) {
            @Override
            public Iterable<String> getSearchableStrings(RegattaDTO t) {
                List<String> string = new ArrayList<String>();
                string.add(t.getName());
                if (t.boatClass != null) {
                    string.add(t.boatClass.getName());
                }             
                if (t.courseAreas != null) {
                    for (final CourseAreaDTO courseArea : t.courseAreas) {
                        string.add(courseArea.getName());
                    }
                }
                return string;          
            }

            @Override
            public AbstractCellTable<RegattaDTO> getCellTable() {
                return regattaTable;
            }
        };
        filterablePanelRegattas.getTextBox().ensureDebugId("RegattasFilterTextBox");
        regattaTable = createRegattaTable(userService);
        regattaTable.ensureDebugId("RegattasCellTable");
        @SuppressWarnings("unchecked")
        final RefreshableMultiSelectionModel<RegattaDTO> selectionModel = (RefreshableMultiSelectionModel<RegattaDTO>) regattaTable.getSelectionModel();
        refreshableRegattaMultiSelectionModel = selectionModel;
        regattaTable.setVisible(false);
        setUpdatePermissionFilter(userService);
        panel.add(filterablePanelRegattas);
        panel.add(regattaTable);
        initWidget(panel);
    }

    /**
     * True {@link RegattaDTO}s as managed by this panel usually shall be filterable based on the user's
     * permission to update. However, this panel may also be subclassed and used for objects that have not
     * yet been committed to the server or for other reasons are lacking ownership / security information.
     * Those subclasses should override this method to not set a filter.<p>
     *
     * This implementation sets a filter that requires the user to have {@link DefaultActions#UPDATE} permission
     * for the regatta in question.
     */
    protected void setUpdatePermissionFilter(final UserService userService) {
        filterablePanelRegattas
                .setUpdatePermissionFilterForCheckbox(regatta -> userService.hasPermission(regatta, DefaultActions.UPDATE));
    }

    public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler) {
        return refreshableRegattaMultiSelectionModel.addSelectionChangeHandler(handler);
    }

    protected CellTable<RegattaDTO> createRegattaTable(final UserService userService) {
        FlushableCellTable<RegattaDTO> table = new FlushableCellTable<RegattaDTO>(/* pageSize */10000, tableRes);
        regattaListDataProvider.addDataDisplay(table);
        table.setWidth("100%");
        SelectionCheckboxColumn<RegattaDTO> regattaSelectionCheckboxColumn = new SelectionCheckboxColumn<RegattaDTO>(
                tableRes.cellTableStyle().cellTableCheckboxSelected(),
                tableRes.cellTableStyle().cellTableCheckboxDeselected(),
                tableRes.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<RegattaDTO>() {
                    @Override
                    public boolean representSameEntity(RegattaDTO dto1, RegattaDTO dto2) {
                        return dto1.getRegattaIdentifier().equals(dto2.getRegattaIdentifier());
                    }
                    @Override
                    public int hashCode(RegattaDTO t) {
                        return t.getRegattaIdentifier().hashCode();
                    }
                }, filterablePanelRegattas.getAllListDataProvider(), table);
        ListHandler<RegattaDTO> columnSortHandler = new ListHandler<RegattaDTO>(regattaListDataProvider.getList());
        table.addColumnSortHandler(columnSortHandler);
        columnSortHandler.setComparator(regattaSelectionCheckboxColumn, regattaSelectionCheckboxColumn.getComparator());
        TextColumn<RegattaDTO> regattaNameColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.getName();
            }
        };
        regattaNameColumn.setSortable(true);
        columnSortHandler.setComparator(regattaNameColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                return new NaturalComparator().compare(r1.getName(), r2.getName());
            }
        });
        TextColumn<RegattaDTO> regattaCanBoatsOfCompetitorsChangePerRaceColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.canBoatsOfCompetitorsChangePerRace ? stringMessages.yes() : stringMessages.no();
            }
        };
        regattaCanBoatsOfCompetitorsChangePerRaceColumn.setSortable(true);
        columnSortHandler.setComparator(regattaCanBoatsOfCompetitorsChangePerRaceColumn,
                (r1, r2)->Boolean.valueOf(r1.canBoatsOfCompetitorsChangePerRace).compareTo(Boolean.valueOf(r2.canBoatsOfCompetitorsChangePerRace)));
        TextColumn<RegattaDTO> competitorRegistrationTypeColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.competitorRegistrationType.getLabel(stringMessages);
            }
        };
        competitorRegistrationTypeColumn.setSortable(true);
        columnSortHandler.setComparator(competitorRegistrationTypeColumn, (r1, r2)->r1.competitorRegistrationType.ordinal() - r2.competitorRegistrationType.ordinal());
        TextColumn<RegattaDTO> startEndDateColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return DateAndTimeFormatterUtil.formatDateRange(regatta.startDate, regatta.endDate);
            }
        };
        startEndDateColumn.setSortable(true);
        columnSortHandler.setComparator(startEndDateColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                int result;
                if(r1.startDate != null && r2.startDate != null) {
                    result = r2.startDate.compareTo(r1.startDate);
                } else if(r1.startDate == null && r2.startDate != null) {
                    result = 1;
                } else if(r1.startDate != null && r2.startDate == null) {
                    result = -1;
                } else {
                    result = 0;
                }
                return result;
            }
        });
        TextColumn<RegattaDTO> regattaBoatClassColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.boatClass != null ? regatta.boatClass.getName() : "";
            }
        };
        regattaBoatClassColumn.setSortable(true);
        columnSortHandler.setComparator(regattaBoatClassColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                return new NaturalComparator(false).compare(r1.boatClass.getName(), r2.boatClass.getName());
            }
        });
        TextColumn<RegattaDTO> rankingMetricColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.rankingMetricType != null ? RankingMetricTypeFormatter.format(regatta.rankingMetricType, stringMessages) : "";
            }
        };
        rankingMetricColumn.setSortable(true);
        columnSortHandler.setComparator(rankingMetricColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                return new NaturalComparator(false).compare(r1.rankingMetricType.name(), r2.rankingMetricType.name());
            }
        });
        TextColumn<RegattaDTO> courseAreasColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO leaderboard) {
                return Util.joinStrings(", ", Util.map(leaderboard.courseAreas, CourseAreaDTO::getName));
            }
        };
        courseAreasColumn.setSortable(true);
        columnSortHandler.setComparator(courseAreasColumn,
                (r1, r2) -> new NaturalComparator().compare(Util.joinStrings(", ", Util.map(r1.courseAreas, CourseAreaDTO::getName)),
                        Util.joinStrings(", ", Util.map(r2.courseAreas, CourseAreaDTO::getName))));
        final HasPermissions type = SecuredDomainType.REGATTA;
        final AccessControlledActionsColumn<RegattaDTO, RegattaConfigImagesBarCell> actionsColumn = create(
                new RegattaConfigImagesBarCell(stringMessages), userService);
        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_UPDATE, UPDATE, this::editRegatta);
        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_DELETE, DELETE, regatta -> {
            if (Window.confirm(stringMessages.doYouReallyWantToRemoveRegatta(regatta.getName()))) {
                removeRegatta(regatta);
            }
        });
        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_CERTIFICATES_UPDATE, READ, this::handleBoatCertificateAssignment);
        // Using lambda instead of method reference for opening both dialogs because the GWT compiler tries to share the
        // method across two different objects. See:
        // https://github.com/gwtproject/gwt/issues/9333
        // https://github.com/gwtproject/gwt/issues/9307
        final DialogConfig<RegattaDTO> config = EditOwnershipDialog.create(userService.getUserManagementWriteService(), type,
                regatta -> {
                    presenter.getRegattasRefresher().reloadAndCallFillAll();
                    presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
                }, stringMessages);
        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_CHANGE_OWNERSHIP, CHANGE_OWNERSHIP,
                regattaDTO -> config.openOwnershipDialog(regattaDTO));
        final EditACLDialog.DialogConfig<RegattaDTO> configACL = EditACLDialog.create(
                userService.getUserManagementWriteService(), type, regatta -> presenter.getRegattasRefresher().reloadAndCallFillAll(),
                stringMessages);
        actionsColumn.addAction(RegattaConfigImagesBarCell.ACTION_CHANGE_ACL, DefaultActions.CHANGE_ACL,
                regattaDTO -> configACL.openDialog(regattaDTO));
        table.addColumn(regattaSelectionCheckboxColumn, regattaSelectionCheckboxColumn.getHeader());
        table.addColumn(regattaNameColumn, stringMessages.regattaName());
        table.addColumn(regattaCanBoatsOfCompetitorsChangePerRaceColumn, stringMessages.canBoatsChange());
        table.addColumn(competitorRegistrationTypeColumn, stringMessages.competitorRegistrationTypeShort());
        table.addColumn(startEndDateColumn, stringMessages.from() + "/" + stringMessages.to());
        table.addColumn(regattaBoatClassColumn, stringMessages.boatClass());
        table.addColumn(courseAreasColumn, stringMessages.courseAreas());
        table.addColumn(rankingMetricColumn, stringMessages.rankingMetric());
        SecuredDTOOwnerColumn.configureOwnerColumns(table, columnSortHandler, stringMessages);
        table.addColumn(actionsColumn, stringMessages.actions());
        table.setSelectionModel(regattaSelectionCheckboxColumn.getSelectionModel(), regattaSelectionCheckboxColumn.getSelectionManager());
        return table;
    }

    private void removeRegatta(final RegattaDTO regatta) {
        final RegattaIdentifier regattaIdentifier = new RegattaName(regatta.getName());
        sailingServiceWrite.removeRegatta(regattaIdentifier, new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to remove regatta " + regatta.getName() + ": "
                        + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                presenter.getRegattasRefresher().remove(regatta);
                presenter.getRegattasRefresher().callAllFill();
                presenter.getLeaderboardsRefresher().removeAll(leaderboard->Util.equalsWithNull(leaderboard.regattaName, regatta.getName()));
            }
        }));
    }

    private void editRegatta(final RegattaDTO toBeEdited) {
        final Collection<RegattaDTO> existingRegattas = getAllRegattas();
        @SuppressWarnings("unchecked")
        final Displayer<EventDTO>[] eventDisplayer = (Displayer<EventDTO>[]) new Displayer<?>[1];
        eventDisplayer[0] = new Displayer<EventDTO>() {
            @Override
            public void fill(Iterable<EventDTO> events) {
                openEditRegattaDialog(toBeEdited, existingRegattas, events);
                presenter.getEventsRefresher().removeDisplayer(eventDisplayer[0]);
            }
        };
        presenter.getEventsRefresher().addDisplayerAndCallFillOnInit(eventDisplayer[0]);
    }

    private void openEditRegattaDialog(RegattaDTO regatta, Collection<RegattaDTO> existingRegattas,
            Iterable<EventDTO> existingEvents) {
        RegattaWithSeriesAndFleetsDialog dialog = new RegattaWithSeriesAndFleetsEditDialog(regatta, existingRegattas,
                existingEvents, /*correspondingEvent*/ null, sailingServiceWrite, userService, stringMessages, new DialogCallback<RegattaDTO>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(RegattaDTO editedRegatta) {
                        commitEditedRegatta(editedRegatta);
                    }
                });
        dialog.show();
    }

    private void commitEditedRegatta(final RegattaDTO editedRegatta) {
        final RegattaIdentifier regattaName = new RegattaName(editedRegatta.getName());
        sailingServiceWrite.updateRegatta(regattaName, editedRegatta.startDate, editedRegatta.endDate,
                Util.mapToArrayList(editedRegatta.courseAreas, CourseAreaDTO::getId),
                editedRegatta.configuration, editedRegatta.buoyZoneRadiusInHullLengths, editedRegatta.useStartTimeInference, editedRegatta.controlTrackingFromStartAndFinishTimes,
                editedRegatta.autoRestartTrackingUponCompetitorSetChange, editedRegatta.registrationLinkSecret,
                editedRegatta.competitorRegistrationType, new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to update regatta " + editedRegatta.getName() + ": "
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        presenter.getRegattasRefresher().reloadAndCallFillAll();
                        presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
                    }
                }));

        final Iterator<SeriesDTO> seriesIter = editedRegatta.series.iterator();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (seriesIter.hasNext()) {
                    final SeriesDTO series = seriesIter.next();
                    sailingServiceWrite.updateSeries(regattaName, series.getName(), series.getName(), series.isMedal(),
                        series.isFleetsCanRunInParallel(), series.getDiscardThresholds(), series.isStartsWithZeroScore(),
                        series.isFirstColumnIsNonDiscardableCarryForward(), series.hasSplitFleetContiguousScoring(), series.hasCrossFleetMergedRanking(),
                        series.getMaximumNumberOfDiscards(), series.isOneAlwaysStaysOne(), series.getFleets(),
                        new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Error trying to update regatta " + editedRegatta.getName()
                                        + ": " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                presenter.getRegattasRefresher().reloadAndCallFillAll();
                                presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
                                run(); // update next series if iterator has next element
                            }
                        }));
                }
            }
        };
        r.run();
    }

    protected List<RegattaDTO> getSelectedRegattas() {
        return new ArrayList<RegattaDTO>(refreshableRegattaMultiSelectionModel.getSelectedSet());
    }

    public void fillRegattas(Iterable<RegattaDTO> regattas) {
        if (Util.isEmpty(regattas)) {
            regattaTable.setVisible(false);
            noRegattasLabel.setVisible(true);
        } else {
            regattaTable.setVisible(true);
            noRegattasLabel.setVisible(false);
        }
        List<RegattaDTO> newAllRegattas = new ArrayList<RegattaDTO>();
        Util.addAll(regattas, newAllRegattas);
        allRegattas = newAllRegattas;
        filterablePanelRegattas.updateAll(allRegattas);
    }

    private void handleBoatCertificateAssignment(RegattaDTO regatta) {
        BoatCertificateAssignmentDialog dialog = new BoatCertificateAssignmentDialog(sailingServiceWrite, userService,
                stringMessages, errorReporter, new RegattaBoatCertificatesPanel(sailingServiceWrite, userService, regatta, stringMessages, errorReporter));
        dialog.show();
    }

    public List<RegattaDTO> getAllRegattas() {
        return allRegattas;
    }

    public RefreshableMultiSelectionModel<RegattaDTO> getRefreshableMultiSelectionModel() {
        return refreshableRegattaMultiSelectionModel;
    }

    public CellTable<RegattaDTO> getRegattaTable() {
        return regattaTable;
    }
}
