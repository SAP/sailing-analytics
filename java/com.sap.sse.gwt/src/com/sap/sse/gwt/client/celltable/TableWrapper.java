package com.sap.sse.gwt.client.celltable;

import java.util.Comparator;
import java.util.function.Function;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.Range;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.gwt.client.panels.AbstractFilterablePanel;

/**
 * The {@link #getTable() table} created and wrapped by this object offers already a {@link ListHandler} for sorting.
 * Subclasses can obtain the table's default column sort handler created by this class's constructor by calling
 * {@link #getColumnSortHandler}. This may not even be necessary when adding sortable columns by using the
 * {@link #addColumn(Column, String, Comparator)} method which provides the comparator and sets the column
 * as {@link Column#setSortable(boolean) sortable}.<p>
 * 
 * The table is wrapped by a panel that can be obtained using {@link #asWidget()}
 * and which contains, if requested, the pager widget underneath the table.
 */
public abstract class TableWrapper<T, S extends RefreshableSelectionModel<T>, SM extends StringMessages, TR extends CellTableWithCheckboxResources> implements IsWidget {
    /**
     * If the {@code enablePager} constructor argument is set to {@code true} then this many entries are shown
     * at most on one page, and users will have to flip through the pages one by one.
     */
    private static final int PAGING_SIZE = 100;
    
    protected final FlushableCellTable<T> table;
    private S selectionModel;
    protected ListDataProvider<T> dataProvider;
    protected VerticalPanel mainPanel;
    protected final ErrorReporter errorReporter;
    private final SM stringMessages;
    private final boolean multiSelection;
    private SelectionCheckboxColumn<T> selectionCheckboxColumn;
    private final EntityIdentityComparator<T> entityIdentityComparator;

    private final TR tableRes;
    private final ListHandler<T> columnSortHandler;
    
    @Override
    public Widget asWidget() {
        return mainPanel;
    }
    
    public TableWrapper(final SM stringMessages, ErrorReporter errorReporter,
            boolean multiSelection, boolean enablePager, EntityIdentityComparator<T> entityIdentityComparator) {
        this(stringMessages, errorReporter, multiSelection, enablePager, entityIdentityComparator, GWT.create(CellTableWithCheckboxResources.class));
    }
    
    /**
     * @param entityIdentityComparator
     *            {@link EntityIdentityComparator} to create a {@link RefreshableSelectionModel}
     */
    public TableWrapper(final SM stringMessages, ErrorReporter errorReporter,
            boolean multiSelection, boolean enablePager, EntityIdentityComparator<T> entityIdentityComparator, TR tableRes) {
        this.entityIdentityComparator = entityIdentityComparator;
        this.multiSelection = multiSelection;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        this.tableRes = tableRes;
        table = new FlushableCellTable<T>(10000, tableRes);
        table.ensureDebugId("WrappedTable");
        this.dataProvider = new ListDataProvider<T>();
        this.columnSortHandler = new ListHandler<T>(dataProvider.getList());
        table.addColumnSortHandler(this.columnSortHandler);
        registerSelectionModelOnNewDataProvider(dataProvider);
        mainPanel = new VerticalPanel();
        dataProvider.addDataDisplay(table);
        mainPanel.add(table);
        if (enablePager) {
            table.setPageSize(PAGING_SIZE);
            SimplePager pager = new SimplePager() {
                protected String createText() {
                    HasRows display = getDisplay();
                    Range range = display.getVisibleRange();
                    int pageStart = range.getStart() + 1;
                    int pageSize = range.getLength();
                    int dataSize = display.getRowCount();
                    int endIndex = Math.min(dataSize, pageStart + pageSize - 1);
                    endIndex = Math.max(pageStart, endIndex);
                    boolean exact = display.isRowCountExact();
                    return stringMessages.pagerStateInfo(pageStart, endIndex, dataSize, exact);
                }
            };
            pager.setDisplay(table);
            mainPanel.add(pager);
        }
    }
    
    public ListHandler<T> getColumnSortHandler() {
        return columnSortHandler;
    }

    public FlushableCellTable<T> getTable() {
        return table;
    }
    
    public void addColumn(Column<T, ?> column) {
        table.addColumn(column);
    }
    
    public void addColumn(Column<T, ?> column, String header) {
        table.addColumn(column, header);
    }
    
    /**
     * Sets the {@code column} as {@link Column#setSortable(boolean) sortable}, assigns the comparator for the
     * {@code column} in the {@link #getColumnSortHandler() sort handler} and {@link #addColumn(Column, String) adds the
     * column}.
     */
    public void addColumnWithNaturalComparatorOnStringRepresentation(Column<T, ?> column, String header) {
        addColumn(column, header,
                (t1, t2)->new NaturalComparator(/* case sensitive */ false)
                    .compare(""+column.getValue(t1), ""+column.getValue(t2)));
    }
    
    /**
     * Sets the {@code column} as {@link Column#setSortable(boolean) sortable}, assigns the comparator for the
     * {@code column} in the {@link #getColumnSortHandler() sort handler} and {@link #addColumn(Column, String) adds the
     * column}.
     */
    public void addColumn(Column<T, ?> column, String header, Comparator<T> comparator) {
        ListHandler<T> columnListHandler = getColumnSortHandler();
        column.setSortable(true);
        columnListHandler.setComparator(column, comparator);
        addColumn(column, header);
    }
    
    /**
     * Adds a sortable {@link TextColumn} whose {@link TextColumn#getValue(Object)} method is based on the {@code valueMapper}
     * and whose sorting is based on a {@link NaturalComparator}.
     */
    public void addColumn(Function<T, String> valueMapper, String header) {
        final TextColumn<T> textColumn = new TextColumn<T>() {
            @Override
            public String getValue(T object) {
                return valueMapper.apply(object);
            }
        };
        addColumn(textColumn, header, Comparator.comparing(t->textColumn.getValue(t), new NaturalComparator()));
    }

    /**
     * Adds a sortable {@link TextColumn} whose {@link TextColumn#getValue(Object)} method is based on the {@code valueMapper}
     * and whose sorting is based on the comparator passed.
     */
    public void addColumn(Function<T, String> valueMapper, String header, Comparator<T> comparator) {
        final TextColumn<T> textColumn = new TextColumn<T>() {
            @Override
            public String getValue(T object) {
                return valueMapper.apply(object);
            }
        };
        addColumn(textColumn, header, comparator);
    }

    public void setEmptyTableWidget(Widget widget) {
        table.setEmptyTableWidget(widget);
    }
    
    public S getSelectionModel() {
        return selectionModel;
    }
    
    public ListDataProvider<T> getDataProvider() {
        return dataProvider;
    }
    
    public void add(T t) {
        getDataProvider().getList().add(t);
    }
    
    public void remove(T t) {
        getDataProvider().getList().remove(t);
    }
    
    public void replaceBasedOnEntityIdentityComparator(T t) {
        if (entityIdentityComparator == null) {
            remove(t);
            add(t);
        } else {
            for (final T existingElement : getDataProvider().getList()) {
                if (entityIdentityComparator.representSameEntity(existingElement, t)) {
                    remove(existingElement);
                    add(t);
                    break;
                }
            }
        }
    }
    
    /**
     * Remove all items from this table's data model
     */
    public void clear() {
        getDataProvider().getList().clear();
    }
    
    public void refresh(Iterable<T> newItems) {
        dataProvider.getList().clear();
        Util.addAll(newItems, dataProvider.getList());
        dataProvider.refresh();
        dataProvider.flush();
    }
    
    /**
     * This method allows you to change the data base for the {@link RefreshableSelectionModel}. This can, e.g., be useful
     * if the table wrapped by this object is filtered through an {@link AbstractFilterablePanel} that has an
     * {@link AbstractFilterablePanel#getAll() all} data structure of which the table displays only a subset, but selection
     * shall be kept across filtering, based on all records available so that removing the filter will restore the previous
     * selection again.
     * 
     * @param dataProvider
     *            {@link ListDataProvider} as data base for the {@link RefreshableSelectionModel}.
     */
    public void registerSelectionModelOnNewDataProvider(ListDataProvider<T> dataProvider) {
        this.dataProvider = dataProvider;
        if (multiSelection) {
            if (selectionCheckboxColumn != null) {
                table.removeColumn(selectionCheckboxColumn);
            }
            selectionCheckboxColumn = new SelectionCheckboxColumn<T>(
                    getTableRes().cellTableStyle().cellTableCheckboxSelected(),
                    getTableRes().cellTableStyle().cellTableCheckboxDeselected(),
                    getTableRes().cellTableStyle().cellTableCheckboxColumnCell(), entityIdentityComparator, dataProvider);
            columnSortHandler.setComparator(selectionCheckboxColumn, selectionCheckboxColumn.getComparator());
            @SuppressWarnings("unchecked")
            final S typedSelectionModel = (S) selectionCheckboxColumn.getSelectionModel();
            selectionModel = typedSelectionModel;
            table.setSelectionModel(selectionModel, selectionCheckboxColumn.getSelectionManager());
            final Header<Boolean> selectAllHeader = selectionCheckboxColumn.createHeader();
            table.addColumn(selectionCheckboxColumn, selectAllHeader);
        } else {
            @SuppressWarnings("unchecked")
            final S typedSelectionModel = (S) new RefreshableSingleSelectionModel<T>(entityIdentityComparator, dataProvider);
            selectionModel = typedSelectionModel;
            table.setSelectionModel(selectionModel);
        }
    }

    protected SM getStringMessages() {
        return stringMessages;
    }

    protected TR getTableRes() {
        return tableRes;
    }
    
    /**
     * @return {@code null} if no or multiple objects are currently selected; the single selected object otherwise; this
     *         can be useful if certain actions are enabled with this table only if a single object is selected.
     */
    public static <X> X getSingleSelectedObjectOrNull(MultiSelectionModel<X> selectionModel) {
        return selectionModel.getSelectedSet() != null && selectionModel.getSelectedSet().size() == 1 ?
                selectionModel.getSelectedSet().iterator().next() : null;
    }

    public void refresh() {
        // TODO Implement TableWrapper.refresh(...)
        
    }
}