package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.racelog.tracking.MappableToDevice;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;

public class ItemToMapToDeviceSelectionPanel implements IsWidget {
    private final CompetitorTableWrapper<RefreshableSingleSelectionModel<CompetitorDTO>> competitorTable;
    private final MarkTableWrapper<RefreshableSingleSelectionModel<MarkDTO>> markTable;
    private MappableToDevice selected;
    private final VerticalPanel mainPanel;
    private final ErrorReporter errorReporter;
    
    static interface SelectionChangedHandler {
        void onSelectionChange(CompetitorDTO competitor);
        void onSelectionChange(MarkDTO mark);
    }

    public ItemToMapToDeviceSelectionPanel(SailingServiceAsync sailingService, StringMessages stringMessages,
            ErrorReporter errorReporter, final SelectionChangedHandler handler, MappableToDevice selected) {
        this.selected = selected;
        this.errorReporter = errorReporter;
        competitorTable = new CompetitorTableWrapper<>(sailingService, stringMessages, errorReporter, /* multiSelection */ false,
                /* enablePager */ true, /* filterCompetitorWithBoat */ false, /* filterCompetitorsWithoutBoat */ false);
        markTable = new MarkTableWrapper<RefreshableSingleSelectionModel<MarkDTO>>(/* multiSelection */ false, sailingService,
                stringMessages, errorReporter);
        final SingleSelectionModel<MarkDTO> markSelectionModel = markTable.getSelectionModel();
        competitorTable.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (competitorTable.getSelectionModel().getSelectedSet().size() == 1) {
                    CompetitorDTO selectedCompetitor = competitorTable.getSelectionModel().getSelectedSet().iterator()
                            .next();
                    ItemToMapToDeviceSelectionPanel.this.selected = selectedCompetitor;
                    deselectAll(markTable.getSelectionModel(), markTable.getDataProvider().getList());
                    handler.onSelectionChange(selectedCompetitor);
                }
            }
        });
        markSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (markSelectionModel.getSelectedSet().size() == 1) {
                    MarkDTO selectedMark = markSelectionModel.getSelectedSet().iterator().next();
                    ItemToMapToDeviceSelectionPanel.this.selected = selectedMark;
                    deselectAll(competitorTable.getSelectionModel(), competitorTable.getAllCompetitors());
                    handler.onSelectionChange(selectedMark);
                }
            }
        });
        // build UI
        this.mainPanel = new VerticalPanel();
        CaptionPanel marksPanel = new CaptionPanel(stringMessages.mark());
        CaptionPanel competitorsPanel = new CaptionPanel(stringMessages.competitor());
        marksPanel.setContentWidget(markTable.asWidget());
        competitorsPanel.setContentWidget(competitorTable.asWidget());
        mainPanel.add(marksPanel);
        mainPanel.add(competitorsPanel);
    }

    @Override
    public Widget asWidget() {
        return mainPanel;
    }
    
    private static <T> void deselectAll(SelectionModel<T> selectionModel, Iterable<T> list) {
        for (T t : list) {
            selectionModel.setSelected(t, false);
        }
    }
    
    private <T extends MappableToDevice> void select(Iterable<T> elements, SelectionModel<T> selectionModel) {
        if (selected == null) {
            return;
        }
        for (T element : elements) {
            if (element.getIdAsString().equals(selected.getIdAsString())) {
                selectionModel.setSelected(element, true);
            }
        }
    }    
    
    public AsyncCallback<Collection<CompetitorDTO>> getSetCompetitorsCallback() {
        return new AsyncCallback<Collection<CompetitorDTO>>() {
            @Override
            public void onSuccess(Collection<CompetitorDTO> result) {
                competitorTable.refreshCompetitorList(result);
                select(result, competitorTable.getSelectionModel());
            }
            
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Could not load competitors: " + caught.getMessage());
            }
        };
    }
    
    public AsyncCallback<Iterable<MarkDTO>> getSetMarksCallback() {
        return new AsyncCallback<Iterable<MarkDTO>>() {
            @Override
            public void onSuccess(Iterable<MarkDTO> result) {
                markTable.refresh(result);
                select(result, markTable.getSelectionModel());
            }
            
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Could not load marks: " + caught.getMessage());
            }
        };
    }
}
