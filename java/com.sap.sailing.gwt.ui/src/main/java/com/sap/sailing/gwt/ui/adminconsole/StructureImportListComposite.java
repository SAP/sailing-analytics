package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.impl.NaturalComparator;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.RegattaSelectionProvider;
import com.sap.sailing.gwt.ui.client.RegattasDisplayer;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.SelectionCheckboxColumn;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;

public class StructureImportListComposite extends RegattaListComposite implements RegattasDisplayer {

    private /*final*/SelectionCheckboxColumn<RegattaDTO> selectionCheckboxColumn;

    public StructureImportListComposite(final SailingServiceAsync sailingService,
            final RegattaSelectionProvider regattaSelectionProvider, RegattaRefresher regattaRefresher,
            final ErrorReporter errorReporter, final StringMessages stringMessages) {
        super(sailingService,regattaSelectionProvider,regattaRefresher,errorReporter,stringMessages);
        regattaSelectionModel = this.selectionCheckboxColumn.getSelectionModel();
        regattaTable.setSelectionModel(regattaSelectionModel, this.selectionCheckboxColumn.getSelectionManager());
        regattaSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                List<RegattaDTO> selectedRegattas = getSelectedRegattas();
                List<RegattaIdentifier> selectedRaceIdentifiers = new ArrayList<RegattaIdentifier>();
                for (RegattaDTO selectedRegatta : selectedRegattas) {
                    selectedRaceIdentifiers.add(selectedRegatta.getRegattaIdentifier());
                }
                StructureImportListComposite.this.regattaSelectionProvider.setSelection(selectedRaceIdentifiers);
            }
        });
    }

    // create Regatta Table in StructureImportManagementPanel
    @Override
    protected CellTable<RegattaDTO> createRegattaTable() {
        CellTable<RegattaDTO> table = new CellTable<RegattaDTO>(/* pageSize */10000, tableRes);
        regattaListDataProvider.addDataDisplay(table);
        table.setWidth("100%");
        
        this.selectionCheckboxColumn = new SelectionCheckboxColumn<RegattaDTO>(tableRes.cellTableStyle()
                .cellTableCheckboxSelected(), tableRes.cellTableStyle().cellTableCheckboxDeselected(),
                tableRes.cellTableStyle().cellTableCheckboxColumnCell()) {
            @Override
            protected ListDataProvider<RegattaDTO> getListDataProvider() {
                return regattaListDataProvider;
            }

            @Override
            public Boolean getValue(RegattaDTO row) {
                return regattaTable.getSelectionModel().isSelected(row);
            }
        };

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
                return new NaturalComparator().compare(r1.getName(), r2.getName());
            }
        });

        columnSortHandler.setComparator(selectionCheckboxColumn, selectionCheckboxColumn.getComparator());
        table.addColumn(selectionCheckboxColumn, selectionCheckboxColumn.getHeader());
        table.addColumn(regattaNameColumn, stringMessages.regattaName());

        return table;
    }

}
