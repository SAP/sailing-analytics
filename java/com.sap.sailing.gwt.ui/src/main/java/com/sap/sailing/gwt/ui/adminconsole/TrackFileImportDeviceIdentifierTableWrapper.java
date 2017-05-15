package com.sap.sailing.gwt.ui.adminconsole;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractCellTable.Style;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.common.client.DateAndTimeFormatterUtil;
import com.sap.sailing.gwt.ui.shared.TrackFileImportDeviceIdentifierDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;

public class TrackFileImportDeviceIdentifierTableWrapper extends
        TableWrapper<TrackFileImportDeviceIdentifierDTO, RefreshableSingleSelectionModel<TrackFileImportDeviceIdentifierDTO>> {
    private TextColumn<TrackFileImportDeviceIdentifierDTO> trackNameColumn;
    private final Map<TrackFileImportDeviceIdentifierDTO, CompetitorDTO> mappings = new HashMap<>();

    public TrackFileImportDeviceIdentifierTableWrapper(SailingServiceAsync sailingService,
            StringMessages stringMessages, ErrorReporter errorReporter) {
        super(sailingService, stringMessages, errorReporter, /* multiSelection */ false, /* enablePager */ true,
                new EntityIdentityComparator<TrackFileImportDeviceIdentifierDTO>() {
                    @Override
                    public boolean representSameEntity(TrackFileImportDeviceIdentifierDTO dto1,
                            TrackFileImportDeviceIdentifierDTO dto2) {
                        return dto1.uuidAsString.equals(dto2.uuidAsString);
                    }

                    @Override
                    public int hashCode(TrackFileImportDeviceIdentifierDTO t) {
                        return t.uuidAsString.hashCode();
                    }
                });
        TextColumn<TrackFileImportDeviceIdentifierDTO> uuidColumn = new TextColumn<TrackFileImportDeviceIdentifierDTO>() {
            @Override
            public String getValue(TrackFileImportDeviceIdentifierDTO object) {
                return object.uuidAsString;
            }
        };
        TextColumn<TrackFileImportDeviceIdentifierDTO> fileNameColumn = new TextColumn<TrackFileImportDeviceIdentifierDTO>() {
            @Override
            public String getValue(TrackFileImportDeviceIdentifierDTO object) {
                return object.fileName;
            }
        };
        trackNameColumn = new TextColumn<TrackFileImportDeviceIdentifierDTO>() {
            @Override
            public String getValue(TrackFileImportDeviceIdentifierDTO object) {
                return object.trackName;
            }
        };
        TextColumn<TrackFileImportDeviceIdentifierDTO> fromColumn = new TextColumn<TrackFileImportDeviceIdentifierDTO>() {
            @Override
            public String getValue(TrackFileImportDeviceIdentifierDTO object) {
                return DateAndTimeFormatterUtil.formatDateAndTime(object.from);
            }
        };
        TextColumn<TrackFileImportDeviceIdentifierDTO> toColumn = new TextColumn<TrackFileImportDeviceIdentifierDTO>() {
            @Override
            public String getValue(TrackFileImportDeviceIdentifierDTO object) {
                return DateAndTimeFormatterUtil.formatDateAndTime(object.to);
            }
        };
        TextColumn<TrackFileImportDeviceIdentifierDTO> numberOfFixesColumn = new TextColumn<TrackFileImportDeviceIdentifierDTO>() {
            @Override
            public String getValue(TrackFileImportDeviceIdentifierDTO object) {
                return "" + object.numFixes;
            }
        };
        table.setTableBuilder(new MappingTableBuilder(table));
        table.addColumn(uuidColumn, "UUID");
        table.addColumn(fileNameColumn, "Filename");
        table.addColumn(trackNameColumn, "Trackname");
        table.addColumn(fromColumn, stringMessages.from());
        table.addColumn(toColumn, stringMessages.to());
        table.addColumn(numberOfFixesColumn, "# fixes");
    }

    public void removeTrackNameColumn() {
        table.removeColumn(trackNameColumn);
    }

    class MappingTableBuilder extends DefaultCellTableBuilder<TrackFileImportDeviceIdentifierDTO> {
        private final String evenRowStyle;
        private final String oddRowStyle;
        private final String cellStyle;
        private final String evenCellStyle;
        private final String oddCellStyle;
        private final String firstColumnStyle;
        private final AbstractCellTable<TrackFileImportDeviceIdentifierDTO> cellTable;

        public MappingTableBuilder(AbstractCellTable<TrackFileImportDeviceIdentifierDTO> cellTable) {
            super(cellTable);
            this.cellTable = cellTable;
            Style style = cellTable.getResources().style();
            evenRowStyle = style.evenRow();
            oddRowStyle = style.oddRow();
            cellStyle = style.cell();
            evenCellStyle = " " + style.evenRowCell();
            oddCellStyle = " " + style.oddRowCell();
            firstColumnStyle = " " + style.firstColumn();
        }

        @Override
        public void buildRowImpl(TrackFileImportDeviceIdentifierDTO rowValue, int absRowIndex) {
            super.buildRowImpl(rowValue, absRowIndex);
            boolean isEven = absRowIndex % 2 == 0;

            StringBuilder trClasses = new StringBuilder(isEven ? evenRowStyle : oddRowStyle);
            RowStyles<TrackFileImportDeviceIdentifierDTO> rowStyles = cellTable.getRowStyles();
            if (rowStyles != null) {
                String extraRowStyles = rowStyles.getStyleNames(rowValue, absRowIndex);
                if (extraRowStyles != null) {
                    trClasses.append(" ").append(extraRowStyles);
                }
            }

            StringBuilder tdClasses = new StringBuilder(cellStyle);
            tdClasses.append(isEven ? evenCellStyle : oddCellStyle);

            TableRowBuilder tr = startRow();
            tr.className(trClasses.toString());
            tr.startTD().className(new StringBuilder(tdClasses).append(firstColumnStyle).toString())
                    .html(SafeHtmlUtils.fromSafeConstant(" ")).end();
            TableCellBuilder td = tr.startTD();
            td.className(tdClasses.toString());
            td.colSpan(getColumns().size() - 1);
            CompetitorDTO competitorDTO = mappings.get(rowValue);
            if (competitorDTO == null) {
                td.html(new SafeHtmlBuilder().appendEscaped("--").toSafeHtml());
            } else {
                td.html(new SafeHtmlBuilder().appendEscaped(competitorDTO.getName()).toSafeHtml());
            }
            td.end();
            tr.end();
        }
    }

    public CompetitorDTO getMappedCompetitorForCurrentSelection() {
        TrackFileImportDeviceIdentifierDTO selectedObject = getSelectionModel().getSelectedObject();
        if (selectedObject != null) {
            return mappings.get(selectedObject);
        }
        return null;
    }

    public void didSelectCompetitorForMapping(CompetitorDTO competitor) {
        TrackFileImportDeviceIdentifierDTO selectedObject = getSelectionModel().getSelectedObject();
        if (selectedObject != null) {
            if (competitor != null) {
                mappings.put(selectedObject, competitor);
            } else {
                mappings.remove(selectedObject);
            }
        }
        table.redraw();
    }

    public Map<TrackFileImportDeviceIdentifierDTO, CompetitorDTO> getMappings() {
        return mappings;
    }
}
