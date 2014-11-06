package com.sap.sailing.dashboards.gwt.client.startanalysis.rankingtable;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.sap.sailing.dashboards.gwt.shared.dto.startanalysis.StartAnalysisCompetitorDTO;
import com.sap.sse.common.Util.Pair;

public class StartAnalysisStartRankTable extends AbsolutePanel {

    CellTable<StartAnalysisCompetitorDTO> table;

    public StartAnalysisStartRankTable(List<StartAnalysisCompetitorDTO> saCompetitors) {

        initTable(saCompetitors);
    }

    private void initTable(List<StartAnalysisCompetitorDTO> saCompetitors) {

        CellTable.Resources tableRes = GWT.create(StartAnalysisStartRankTableStyleResource.class);
        
        table = new CellTable<StartAnalysisCompetitorDTO>(1, tableRes);
        table.getElement().getStyle().setWidth(100, Unit.PCT);
        table.setSkipRowHoverStyleUpdate(true);
        table.setSkipRowHoverCheck(true);
        table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

        StartAnalysisStartRankTableRankAtFirstMarkColumn<StartAnalysisCompetitorDTO> rankColumn = new StartAnalysisStartRankTableRankAtFirstMarkColumn<StartAnalysisCompetitorDTO>() {
            @Override    
            public String getValue(StartAnalysisCompetitorDTO object) {
                return ""+object.rankingTableEntryDTO.rankAtFirstMark;    
            }  
        };
        
        StartAnalysisStartRankTableTeamCollumn<StartAnalysisCompetitorDTO> teamColumn = new StartAnalysisStartRankTableTeamCollumn<StartAnalysisCompetitorDTO>() {
            @Override
            public Pair<String, String> getValue(StartAnalysisCompetitorDTO saCompetitor) {
                return new Pair<String, String>(saCompetitor.rankingTableEntryDTO.teamName, saCompetitor.rankingTableEntryDTO.tailColor);
            }
        };

        TextColumn<StartAnalysisCompetitorDTO> speedColumn = new TextColumn<StartAnalysisCompetitorDTO>() {
            @Override
            public String getValue(StartAnalysisCompetitorDTO saCompetitor) {
                return "" + NumberFormat.getFormat("#0.0").format(saCompetitor.rankingTableEntryDTO.speedAtStartTime);
            }
        };

        TextColumn<StartAnalysisCompetitorDTO> distanceColumn = new TextColumn<StartAnalysisCompetitorDTO>() {
            @Override
            public String getValue(StartAnalysisCompetitorDTO saCompetitor) {
                return "" + NumberFormat.getFormat("#0.0").format(saCompetitor.rankingTableEntryDTO.distanceToLineAtStartTime);
            }
        };

        table.addColumn(rankColumn, "Rank at first mark");
        table.setColumnWidth(rankColumn, 18.0, Unit.PCT);
        table.addColumn(teamColumn, "Team");
        table.setColumnWidth(teamColumn, 42.0, Unit.PCT);
        table.addColumn(speedColumn, "Speed in kts");
        table.setColumnWidth(speedColumn, 15.0, Unit.PCT);
        table.addColumn(distanceColumn, "Distance to line in m");
        table.setColumnWidth(distanceColumn, 25.0, Unit.PCT);

        table.setRowCount(4, true);

        table.setRowData(saCompetitors);

        this.add(table);
    }
}
