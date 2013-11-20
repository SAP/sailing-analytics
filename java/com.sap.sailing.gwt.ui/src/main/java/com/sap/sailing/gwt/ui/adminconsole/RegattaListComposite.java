package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.gwt.ui.client.DataEntryDialog.DialogCallback;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.RegattaDisplayer;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.RegattaSelectionProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.panels.AbstractFilterablePanel;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;

public class RegattaListComposite extends Composite implements RegattaDisplayer {

    private final SelectionModel<RegattaDTO> regattaSelectionModel;
    private final CellTable<RegattaDTO> regattaTable;
    private ListDataProvider<RegattaDTO> regattaListDataProvider;
    private List<RegattaDTO> allRegattas;
    private final SimplePanel mainPanel;
    private final VerticalPanel panel;

    private final Label noRegattasLabel;

    private final SailingServiceAsync sailingService;
    private final RegattaSelectionProvider regattaSelectionProvider;
    private final ErrorReporter errorReporter;
    private final RegattaRefresher regattaRefresher;
    private final StringMessages stringMessages;

    private final AbstractFilterablePanel<RegattaDTO> filterablePanelRegattas;

    private static AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);

    public static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    public RegattaListComposite(final SailingServiceAsync sailingService,
            final RegattaSelectionProvider regattaSelectionProvider, RegattaRefresher regattaRefresher,
            final ErrorReporter errorReporter, final StringMessages stringMessages) {
        this.sailingService = sailingService;
        this.regattaSelectionProvider = regattaSelectionProvider;
        this.regattaRefresher = regattaRefresher;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        mainPanel = new SimplePanel();
        panel = new VerticalPanel();
        mainPanel.setWidget(panel);
        Label filterRegattasLabel = new Label(stringMessages.filterRegattasByName() + ":");
        filterRegattasLabel.setWordWrap(false);
        noRegattasLabel = new Label(stringMessages.noRegattasYet());
        noRegattasLabel.setWordWrap(false);
        panel.add(noRegattasLabel);
        regattaListDataProvider = new ListDataProvider<RegattaDTO>();
        regattaTable = createRegattaTable();
        regattaTable.setVisible(false);
        filterablePanelRegattas = new AbstractFilterablePanel<RegattaDTO>(filterRegattasLabel, allRegattas,
                regattaTable, regattaListDataProvider) {
            @Override
            public Iterable<String> getSearchableStrings(RegattaDTO t) {
                List<String> string = new ArrayList<String>();
                string.add(t.getName());
                string.add(t.boatClass.getName());
                return string;
            }
        };
        panel.add(filterablePanelRegattas);
        regattaSelectionModel = new MultiSelectionModel<RegattaDTO>();
        regattaSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                List<RegattaDTO> selectedRegattas = getSelectedRegattas();
                List<RegattaIdentifier> selectedRaceIdentifiers = new ArrayList<RegattaIdentifier>();
                for (RegattaDTO selectedRegatta : selectedRegattas) {
                    selectedRaceIdentifiers.add(selectedRegatta.getRegattaIdentifier());
                }
                RegattaListComposite.this.regattaSelectionProvider.setSelection(selectedRaceIdentifiers);
            }
        });
        regattaTable.setSelectionModel(regattaSelectionModel);

        panel.add(regattaTable);

        initWidget(mainPanel);
    }

    private CellTable<RegattaDTO> createRegattaTable() {
        CellTable<RegattaDTO> table = new CellTable<RegattaDTO>(/* pageSize */10000, tableRes);
        regattaListDataProvider.addDataDisplay(table);
        table.setWidth("100%");

        ListHandler<RegattaDTO> columnSortHandler = new ListHandler<RegattaDTO>(regattaListDataProvider.getList());
        table.addColumnSortHandler(columnSortHandler);

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
                return r1.getName().compareTo(r2.getName());
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
                return r1.boatClass.getName().compareTo(r2.boatClass.getName());
            }
        });

        ImagesBarColumn<RegattaDTO, RegattaConfigImagesBarCell> regattaActionColumn = new ImagesBarColumn<RegattaDTO, RegattaConfigImagesBarCell>(
                new RegattaConfigImagesBarCell(stringMessages));
        regattaActionColumn.setFieldUpdater(new FieldUpdater<RegattaDTO, String>() {
            @Override
            public void update(int index, RegattaDTO regatta, String value) {
                if (RegattaConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    editRegatta(regatta);
                } else if (RegattaConfigImagesBarCell.ACTION_REMOVE.equals(value)) {
                    if (Window.confirm(stringMessages.doYouReallyWantToRemoveRegatta(regatta.getName()))) {
                        removeRegatta(regatta);
                    }
                }
            }
        });

        table.addColumn(regattaNameColumn, stringMessages.regattaName());
        table.addColumn(regattaBoatClassColumn, stringMessages.boatClass());
        table.addColumn(regattaActionColumn, stringMessages.actions());

        return table;
    }

    private void removeRegatta(final RegattaDTO regatta) {
        final RegattaIdentifier regattaIdentifier = new RegattaName(regatta.getName());
        sailingService.removeRegatta(regattaIdentifier, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to remove regatta " + regatta.getName() + ": " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                regattaRefresher.fillRegattas();
            }
        });
    }

    private void editRegatta(final RegattaDTO toBeEdited) {
        final Collection<RegattaDTO> existingRegattas = getAllRegattas();
        sailingService.getEvents(new AsyncCallback<List<EventDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                openEditRegattaDialog(toBeEdited, existingRegattas, Collections.<EventDTO> emptyList());
            }

            @Override
            public void onSuccess(List<EventDTO> events) {
                openEditRegattaDialog(toBeEdited, existingRegattas, Collections.unmodifiableList(events));
            }
        });
    }

    private void openEditRegattaDialog(RegattaDTO regatta, Collection<RegattaDTO> existingRegattas,
            List<EventDTO> existingEvents) {
        RegattaWithSeriesAndFleetsDialog dialog = new RegattaWithSeriesAndFleetsEditDialog(regatta, existingRegattas,
                existingEvents, stringMessages, new DialogCallback<RegattaDTO>() {
                    @Override
                    public void cancel() { }

                    @Override
                    public void ok(RegattaDTO editedRegatta) {
                        commitEditedRegatta(editedRegatta);
                    }
                });
        dialog.show();
    }

    private void commitEditedRegatta(final RegattaDTO editedRegatta) {
        final RegattaIdentifier regattaName = new RegattaName(editedRegatta.getName());
        
        sailingService.updateRegatta(regattaName, editedRegatta.defaultCourseAreaIdAsString,
                editedRegatta.defaultRacingProcedureType, editedRegatta.defaultCourseDesignerMode,
                new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to update regatta " + editedRegatta.getName() + ": " + caught.getMessage());
            }
            
            @Override
            public void onSuccess(Void result) {
                regattaRefresher.fillRegattas();
            }
        });
    }

    private List<RegattaDTO> getSelectedRegattas() {
        List<RegattaDTO> result = new ArrayList<RegattaDTO>();
        if (regattaListDataProvider != null) {
            for (RegattaDTO regatta : regattaListDataProvider.getList()) {
                if (regattaSelectionModel.isSelected(regatta)) {
                    result.add(regatta);
                }
            }
        }
        return result;
    }

    @Override
    public void fillRegattas(List<RegattaDTO> regattas) {
        if (regattas.isEmpty()) {
            regattaTable.setVisible(false);
            noRegattasLabel.setVisible(true);
        } else {
            regattaTable.setVisible(true);
            noRegattasLabel.setVisible(false);
        }
        List<RegattaDTO> newAllRegattas = new ArrayList<RegattaDTO>(regattas);
        List<RegattaIdentifier> newAllRegattaIdentifiers = new ArrayList<RegattaIdentifier>();
        for (RegattaDTO regatta : regattas) {
            newAllRegattaIdentifiers.add(regatta.getRegattaIdentifier());
        }
        allRegattas = newAllRegattas;
        filterablePanelRegattas.updateAll(allRegattas);
        regattaSelectionProvider.setAllRegattas(newAllRegattaIdentifiers);
    }

    public List<RegattaDTO> getAllRegattas() {
        return allRegattas;
    }
}
