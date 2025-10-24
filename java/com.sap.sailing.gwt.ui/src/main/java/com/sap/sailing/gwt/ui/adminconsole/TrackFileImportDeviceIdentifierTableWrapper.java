package com.sap.sailing.gwt.ui.adminconsole;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractCellTable.Style;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.racelog.tracking.MappableToDevice;
import com.sap.sailing.gwt.ui.client.MappableToDeviceFormatter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.TrackFileImportDeviceIdentifierDTO;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;

public class TrackFileImportDeviceIdentifierTableWrapper extends
        TableWrapper<TrackFileImportDeviceIdentifierDTO, RefreshableSingleSelectionModel<TrackFileImportDeviceIdentifierDTO>> {
    private TextColumn<TrackFileImportDeviceIdentifierDTO> trackNameColumn;
    private final Map<TrackFileImportDeviceIdentifierDTO, MappableToDevice> mappings = new HashMap<>();

    public TrackFileImportDeviceIdentifierTableWrapper(SailingServiceWriteAsync sailingServiceWrite,
            StringMessages stringMessages, ErrorReporter errorReporter) {
        super(sailingServiceWrite, stringMessages, errorReporter, /* multiSelection */ false, /* enablePager */ true,
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
            final MappableToDevice mappableToDevice = mappings.get(rowValue);
            tr.startTD().className(new StringBuilder(tdClasses).append(firstColumnStyle).toString())
                    .html(SafeHtmlUtils
                            .fromString(MappableToDeviceFormatter.formatType(mappableToDevice, getStringMessages())))
                    .end();
            TableCellBuilder td = tr.startTD();
            td.className(tdClasses.toString());
            td.colSpan(getColumns().size() - 1);
            final String nameOfMappedItem;
            if (mappableToDevice instanceof CompetitorWithBoatDTO) {
                final CompetitorWithBoatDTO competitorDTO = (CompetitorWithBoatDTO) mappableToDevice;
                nameOfMappedItem = competitorDTO.getName();
            } else if (mappableToDevice instanceof BoatDTO) {
                final BoatDTO boatDTO = (BoatDTO) mappableToDevice;
                nameOfMappedItem = boatDTO.getDisplayName();
            } else if (mappableToDevice instanceof MarkDTO) {
                final MarkDTO markDTO = (MarkDTO) mappableToDevice;
                nameOfMappedItem = markDTO.getName();
            } else {
                nameOfMappedItem = "--";
            }
            td.html(SafeHtmlUtils.fromString(nameOfMappedItem));
            td.end();
            tr.end();
        }
    }

    public MappableToDevice getMappedObjectForDeviceId(TrackFileImportDeviceIdentifierDTO deviceId) {
        return mappings.get(deviceId);
    }

    public void setMappedObjectForSelectedDevice(MappableToDevice mappableToDevice) {
        TrackFileImportDeviceIdentifierDTO selectedObject = getSelectionModel().getSelectedObject();
        if (selectedObject != null) {
            if (mappableToDevice != null) {
                mappings.put(selectedObject, mappableToDevice);
            } else {
                mappings.remove(selectedObject);
            }
        }
        table.redraw();
    }

    public Map<TrackFileImportDeviceIdentifierDTO, MappableToDevice> getMappings() {
        return mappings;
    }
}
