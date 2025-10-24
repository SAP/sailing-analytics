package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Comparator;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.MappableToDeviceFormatter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.RefreshableSingleSelectionModel;

public class DeviceMappingTableWrapper extends TableWrapper<DeviceMappingDTO, RefreshableSingleSelectionModel<DeviceMappingDTO>> {
    public DeviceMappingTableWrapper(SailingServiceWriteAsync sailingServiceWrite, final StringMessages stringMessages,
            ErrorReporter errorReporter) {
        super(sailingServiceWrite, stringMessages, errorReporter, /* multiSelection */ false, /* enablePager */ true,
                /* leaving EntityIdentityComparator null, reducing comparison to equals/hashCode which is well
                 * defined on DeviceMappingDTO
                 */ null);
        table.setWidth("1000px", true);
        table.addStyleName("wrap-cols");
        ListHandler<DeviceMappingDTO> listHandler = getColumnSortHandler();
        TextColumn<DeviceMappingDTO> itemTypeCol = new TextColumn<DeviceMappingDTO>() {
            @Override
            public String getValue(DeviceMappingDTO mapping) {
                return MappableToDeviceFormatter.formatType(mapping.mappedTo, stringMessages);
            }
        };
        itemTypeCol.setSortable(true);
        listHandler.setComparator(itemTypeCol, new Comparator<DeviceMappingDTO>() {
            @Override
            public int compare(DeviceMappingDTO o1, DeviceMappingDTO o2) {
                return o1.mappedTo.getClass().getName().compareTo(o2.mappedTo.getClass().getName());
            }
        });
        table.addColumn(itemTypeCol, stringMessages.mappedToType());
        TextColumn<DeviceMappingDTO> itemCol = new TextColumn<DeviceMappingDTO>() {
            @Override
            public String getValue(DeviceMappingDTO mapping) {
                return MappableToDeviceFormatter.formatName(mapping.mappedTo);
            }
        };
        itemCol.setSortable(true);
        listHandler.setComparator(itemCol, new Comparator<DeviceMappingDTO>() {
            @Override
            public int compare(DeviceMappingDTO o1, DeviceMappingDTO o2) {
                return MappableToDeviceFormatter.formatName(o1.mappedTo)
                        .compareTo(MappableToDeviceFormatter.formatName(o2.mappedTo));
            }
        });
        table.addColumn(itemCol, stringMessages.mappedTo());
        TextColumn<DeviceMappingDTO> deviceTypeCol = new TextColumn<DeviceMappingDTO>() {
            @Override
            public String getValue(DeviceMappingDTO mapping) {
                return mapping.deviceIdentifier.deviceType;
            }
        };
        deviceTypeCol.setSortable(true);
        listHandler.setComparator(deviceTypeCol, new Comparator<DeviceMappingDTO>() {
            @Override
            public int compare(DeviceMappingDTO o1, DeviceMappingDTO o2) {
                return o1.deviceIdentifier.deviceType.compareTo(o2.deviceIdentifier.deviceType);
            }
        });
        table.addColumn(deviceTypeCol, stringMessages.deviceType());
        TextColumn<DeviceMappingDTO> deviceIdCol = new TextColumn<DeviceMappingDTO>() {
            @Override
            public String getValue(DeviceMappingDTO mapping) {
                return mapping.deviceIdentifier.deviceId;
            }
        };
        deviceIdCol.setSortable(true);
        listHandler.setComparator(deviceIdCol, new Comparator<DeviceMappingDTO>() {
            @Override
            public int compare(DeviceMappingDTO o1, DeviceMappingDTO o2) {
                return o1.deviceIdentifier.deviceId.compareTo(o2.deviceIdentifier.deviceId);
            }
        });
        table.addColumn(deviceIdCol, stringMessages.deviceId());
        table.setColumnWidth(deviceIdCol, 400, Unit.PX);
        TextColumn<DeviceMappingDTO> fromCol = new TextColumn<DeviceMappingDTO>() {
            @Override
            public String getValue(DeviceMappingDTO mapping) {
                return DateAndTimeFormatterUtil.formatDateAndTime(mapping.from);
            }
        };
        fromCol.setSortable(true);
        listHandler.setComparator(fromCol, new Comparator<DeviceMappingDTO>() {
            @Override
            public int compare(DeviceMappingDTO o1, DeviceMappingDTO o2) {
                return Util.compareToWithNull(o1.from, o2.from, /* nullIsLess */ false);
            }
        });
        table.addColumn(fromCol, stringMessages.from());
        TextColumn<DeviceMappingDTO> toCol = new TextColumn<DeviceMappingDTO>() {
            @Override
            public String getValue(DeviceMappingDTO mapping) {
                return DateAndTimeFormatterUtil.formatDateAndTime(mapping.to);
            }
        };
        toCol.setSortable(true);
        listHandler.setComparator(toCol, new Comparator<DeviceMappingDTO>() {
            @Override
            public int compare(DeviceMappingDTO o1, DeviceMappingDTO o2) {
                return Util.compareToWithNull(o1.to, o2.to, /* nullIsLess */ false);
            }
        });
        table.addColumn(toCol, stringMessages.to());
        TextColumn<DeviceMappingDTO> lastFixCol = new TextColumn<DeviceMappingDTO>() {
            @Override
            public String getValue(DeviceMappingDTO mapping) {
                return mapping.lastFix==null?"":DateAndTimeFormatterUtil.formatDateAndTime(mapping.lastFix.asDate());
            }
        };
        lastFixCol.setSortable(true);
        listHandler.setComparator(lastFixCol, new Comparator<DeviceMappingDTO>() {
            @Override
            public int compare(DeviceMappingDTO o1, DeviceMappingDTO o2) {
                return Util.compareToWithNull(o1.lastFix, o2.lastFix, /* nullIsLess */ false);
            }
        });
        table.addColumn(lastFixCol, stringMessages.lastFix());
        table.addColumnSortHandler(listHandler);
    }
    
    @Override
    public Widget asWidget() {
        return  mainPanel;
    }
}
