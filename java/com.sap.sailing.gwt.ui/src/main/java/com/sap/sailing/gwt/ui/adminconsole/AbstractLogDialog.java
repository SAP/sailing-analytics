package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Comparator;
import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.AbstractLogDTO;
import com.sap.sailing.gwt.ui.shared.AbstractLogEventDTO;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.DateAndTimeFormatterUtil;
import com.sap.sse.gwt.client.celltable.BaseCelltable;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public abstract class AbstractLogDialog<R extends AbstractLogDTO<E>, E extends AbstractLogEventDTO> extends DataEntryDialog<R> {
    
    protected final StringMessages stringMessages;
    private final R logDTO;

    private final SelectionModel<E> selectionModel;
    private final CellTable<E> raceLogEventsTable;
    private ListDataProvider<E> raceLogEventsList;

    public AbstractLogDialog(final R logDTO, final StringMessages stringMessages, String logName, DialogCallback<R> callback) {
        super(logName, null, stringMessages.ok(), stringMessages.cancel(), /* validator */ null, callback);
        this.stringMessages = stringMessages;
        this.logDTO = logDTO;

        raceLogEventsList = new ListDataProvider<E>();
        selectionModel = new SingleSelectionModel<E>();

        AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);
        raceLogEventsTable = new BaseCelltable<E>(/* pageSize */10000, tableRes);
        raceLogEventsTable.ensureDebugId("TrackedRacesTable");
        raceLogEventsTable.setSelectionModel(selectionModel);

        raceLogEventsList.setList(logDTO.getEntries());
        ListHandler<E> columnSortHandler = new ListHandler<>(raceLogEventsList.getList());
        raceLogEventsTable.addColumnSortHandler(columnSortHandler);

        TextColumn<E> logEventAuthorColumn = new TextColumn<E>() {
            @Override
            public String getValue(E logEventDTO) {
                return String.valueOf(logEventDTO.getAuthorName());
            }
        };
        logEventAuthorColumn.setSortable(true);
        columnSortHandler.setComparator(logEventAuthorColumn, new Comparator<E>() {
            @Override
            public int compare(E r1, E r2) {
                return new NaturalComparator().compare(r1.getAuthorName(), r1.getAuthorName());
            }
        });

        TextColumn<E> logEventAuthorPriorityColumn = new TextColumn<E>() {
            @Override
            public String getValue(E logEventDTO) {
                return String.valueOf(logEventDTO.getAuthorPriority());
            }
        };
        logEventAuthorPriorityColumn.setSortable(true);
        columnSortHandler.setComparator(logEventAuthorPriorityColumn, new Comparator<E>() {
            @Override
            public int compare(E r1, E r2) {
                return r1.getAuthorPriority() - r2.getAuthorPriority();
            }
        });

        TextColumn<E> logEventCreatedColumn = new TextColumn<E>() {
            @Override
            public String getValue(E raceLogEventDTO) {
                Date createdAt = raceLogEventDTO.getCreatedAt();
                if(createdAt != null) {
                    return DateAndTimeFormatterUtil.defaultDateFormatter.render(createdAt) + " " + 
                            DateAndTimeFormatterUtil.defaultTimeFormatter.render(createdAt);
                }
                return "";
            }
        };
        logEventCreatedColumn.setSortable(true);
        columnSortHandler.setComparator(logEventCreatedColumn, new Comparator<E>() {
            @Override
            public int compare(E r1, E r2) {
                if (r1.getCreatedAt() != null && r2.getCreatedAt() != null) {
                    return r1.getCreatedAt().compareTo(r2.getCreatedAt());
                }
                return r1.getCreatedAt() == null ? (r2.getCreatedAt() == null ? 0 : -1) : 1;
            }
        });

        TextColumn<E> logEventLogicalTimeColumn = new TextColumn<E>() {
            @Override
            public String getValue(E logEventDTO) {
                Date logicalTimePoint = logEventDTO.getLogicalTimePoint();
                if (logicalTimePoint != null) {
                    return DateAndTimeFormatterUtil.defaultDateFormatter.render(logicalTimePoint) + " " + 
                            DateAndTimeFormatterUtil.defaultTimeFormatter.render(logicalTimePoint);
                }
                return "";
            }
        };
        logEventLogicalTimeColumn.setSortable(true);
        columnSortHandler.setComparator(logEventLogicalTimeColumn, new Comparator<E>() {
            @Override
            public int compare(E r1, E r2) {
                if (r1.getLogicalTimePoint() != null && r2.getLogicalTimePoint() != null) {
                    return r1.getLogicalTimePoint().compareTo(r2.getLogicalTimePoint());
                }
                return r1.getLogicalTimePoint() == null ? (r2.getLogicalTimePoint() == null ? 0 : -1) : 1;
            }
        });

        TextColumn<E> logEventTypeColumn = new TextColumn<E>() {
            @Override
            public String getValue(E logEventDTO) {
                return logEventDTO.getType();
            }
        };
        logEventTypeColumn.setSortable(true);
        columnSortHandler.setComparator(logEventTypeColumn, new Comparator<E>() {
            @Override
            public int compare(E r1, E r2) {
                return new NaturalComparator().compare(r1.getType(), r2.getType());
            }
        });

        TextColumn<E> logEventInfoColumn = new TextColumn<E>() {
            @Override
            public String getValue(E logEventDTO) {
                return logEventDTO.getInfo();
            }
        };
        logEventInfoColumn.setSortable(true);
        columnSortHandler.setComparator(logEventInfoColumn, new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
                return new NaturalComparator().compare(o1.getInfo(), o2.getInfo());
            }
        });
        
        addFirstColumns(raceLogEventsTable, columnSortHandler);
        raceLogEventsTable.addColumn(logEventTypeColumn, stringMessages.type());
        raceLogEventsTable.addColumn(logEventCreatedColumn, stringMessages.createdAt());
        raceLogEventsTable.addColumn(logEventLogicalTimeColumn, stringMessages.logicalTimepoint());
        raceLogEventsTable.addColumn(logEventAuthorColumn, stringMessages.authorName());
        raceLogEventsTable.addColumn(logEventAuthorPriorityColumn, stringMessages.authorPriority());
        raceLogEventsTable.addColumn(logEventInfoColumn, stringMessages.info());
        addLastColumns(raceLogEventsTable, columnSortHandler);

        raceLogEventsList.addDataDisplay(raceLogEventsTable);
        raceLogEventsTable.getColumnSortList().push(logEventLogicalTimeColumn);
    }

    protected void addFirstColumns(CellTable<E> table, ListHandler<E> columnSortHandler) {}
    protected void addLastColumns(CellTable<E> table, ListHandler<E> columnSortHandler) {}
    
    @Override
    protected R getResult() {
        return null;
    }

    @Override
    protected Widget getAdditionalWidget() {
        VerticalPanel vPanel = new VerticalPanel(); 
        addFirstWidgetComponents(vPanel);
        ScrollPanel scrollPanel = new ScrollPanel(raceLogEventsTable);
        scrollPanel.setSize("960px", "500px");
        vPanel.add(scrollPanel);
        addLastWidgetComponents(vPanel);
        return vPanel; 
    }

    protected R getLogDTO() {
        return logDTO;
    }
    
    protected void addFirstWidgetComponents(VerticalPanel vPanel) {}
    protected void addLastWidgetComponents(VerticalPanel vPanel) {}
}
