package com.sap.sse.gwt.client.celltable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.view.client.ListDataProvider;
import com.sap.sse.common.InvertibleComparator;

public class SortedCellTable<T> extends BaseCelltable<T> {
    /**
     * The currently sorted column
     */
    private Column<T, ?> currentlySortedColumn;
    
    /**
     * The default sort orders of all columns
     */
    private Map<Column<T, ?>, Boolean> defaultSortOrderMap = new HashMap<Column<T, ?>, Boolean>();
    
    /**
     * Comparators associated with their columns
     */
    private Map<Column<T, ?>, InvertibleComparator<T>> comparators = new HashMap<Column<T, ?>, InvertibleComparator<T>>();
    
    /**
     * Column to sort when the data provider's list is refreshed using {@link SortedCellTable#setList(List)}
     */   
    private Column<T, ?> initialSortColumn;
    
    /**
     * The data provider for the celltable
     */
    private ListDataProvider<T> dataProvider;
    
    /**
     * Column sorting handler to control the sorting
     */
    private ListHandler<T> columnSortHandler;

    public SortedCellTable(int pageSize, CellTable.Resources resources) {
        super(pageSize, resources);
        dataProvider = new ListDataProvider<T>();
        dataProvider.addDataDisplay(this);
        columnSortHandler = new ListHandler<T>(dataProvider.getList()) {
            @Override
            public void onColumnSort(ColumnSortEvent event) {
                @SuppressWarnings("unchecked")
                Column<T, ?> column = (Column<T, ?>) event.getColumn();
                if (column == null) {
                    return;
                }

                final InvertibleComparator<T> comparator = comparators.get(column);
                if (comparator == null) {
                    return;
                }

                boolean ascending;
                if (column.equals(currentlySortedColumn)) {
                    comparator.invertOrder();
                    ascending = comparator.isAscending();
                } else {
                    // Initial sort; look up which direction we need
                    ascending = defaultSortOrderMap.get(column);
                    comparator.setAscending(ascending);
                }
                sortColumn(column, comparator, ascending);
            }

            @Override
            public void setComparator(Column<T, ?> column, Comparator<T> comparator) {
                comparators.put(column, (InvertibleComparator<T>) comparator);
                super.setComparator(column, comparator);
            }
        };
        addColumnSortHandler(columnSortHandler);
    }

    public Column<T, ?> getCurrentlySortedColumn() {
        return currentlySortedColumn;
    }
    
    public void sort() {
        if (currentlySortedColumn != null) {
            InvertibleComparator<T> comparator = comparators.get(currentlySortedColumn);
            if (comparator != null) {
                sortColumn(currentlySortedColumn, comparator, comparator.isAscending());
            }            
        }
    }

    public void sortColumn(Column<T, ?> column) {
        InvertibleComparator<T> comparator = comparators.get(column);
        if (comparator != null) {
            sortColumn(column, comparator, comparator.isAscending());
        }
    }

    public void sortColumn(Column<T, ?> column, boolean ascending) {
        InvertibleComparator<T> comparator = comparators.get(column);
        if (comparator != null) {
            sortColumn(column, comparator, ascending);
        }
    }

    private void sortColumn(Column<T, ?> column, Comparator<T> comparator, boolean ascending) {
        if (ascending) {
            Collections.sort(dataProvider.getList(), comparator);
        } else {
            Collections.sort(dataProvider.getList(), Collections.reverseOrder(comparator));
        }
        getColumnSortList().push(new ColumnSortInfo(column, ascending));
        currentlySortedColumn = column;
    }

    /**
     * Adds a column to the table and sets its sortable state
     */
    public void addColumn(Column<T, ?> column, String headerName, InvertibleComparator<T> comparator, boolean ascendingSorting) {
        addColumn(column, headerName);
        column.setSortable(comparator != null);
        if (comparator != null) {
            defaultSortOrderMap.put(column, ascendingSorting);
            comparators.put(column, comparator);
        }
    }

    /**
     * Easy helper to correctly add sortableColumns without that many parameters
     */
    public void addColumn(SortableColumn<T, ?> column) {
        addColumn(column, column.getHeader(), column.getComparator(), column.getPreferredSortingOrder().isAscending());
    }
    
    /**
     * Adds a column to the table and sets its sortable state
     */
    public void addColumn(Column<T, ?> column, Header<?> header, InvertibleComparator<T> comparator, boolean ascendingSorting) {
        addColumn(column, header);
        column.setSortable(comparator != null);
        if (comparator != null) {
            defaultSortOrderMap.put(column, ascendingSorting);
            comparators.put(column, comparator);
        }
    }

    /**
     * Allows simpler inserting of a SortableColumn 
     */
    public void insertColumn(int beforeIndex, SortableColumn<T, ?> column) {
        insertColumn(beforeIndex, column, column.getHeader(), column.getComparator(),
                column.getPreferredSortingOrder().isAscending());
    }
    
    public void insertColumn(int beforeIndex, Column<T, ?> column, Header<?> header, InvertibleComparator<T> comparator, boolean ascendingSorting) {
        insertColumn(beforeIndex, column, header);
        column.setSortable(comparator != null);
        if (comparator != null) {
            defaultSortOrderMap.put(column, ascendingSorting);
            comparators.put(column, comparator);
        }
    }
    
    /**
     * Sets the column to sort when the data list is reset using {@link SortedCellTable#setList(List)}
     * 
     * @param column
     */
    public void setInitialSortColumn(Column<T, ?> column) {
        initialSortColumn = column;
    }

    /**
     * Sets a comparator to use when sorting the given column
     * 
     * @param column
     * @param comparator
     */
    public void setComparator(Column<T, ?> column, InvertibleComparator<T> comparator) {
        columnSortHandler.setComparator(column, comparator);
    }

    /**
     * Sets the sort order to use when this column is clicked and it was not previously sorted
     * 
     * @param column
     * @param ascending
     */
    public void setDefaultSortOrder(Column<T, ?> column, boolean ascending) {
        defaultSortOrderMap.put(column, ascending);
    }

    /**
     * Sets the table's data provider list and sorts the table based on the column given in
     * {@link SortedCellTable#setInitialSortColumn(Column)}
     * 
     * @param list
     */
    public void setList(Iterable<T> list) {
        dataProvider.getList().clear();
        if (list != null) {
            for (T element : list) {
                dataProvider.getList().add(element);
            }
        }

        // Do a first-time sort based on which column was set in
        // setInitialSortColumn()
        if (initialSortColumn != null) {
            Collections.sort(dataProvider.getList(), new Comparator<T>() {

                @Override
                public int compare(T o1, T o2) {
                    return (defaultSortOrderMap.get(initialSortColumn) ? 1 : -1)
                            * comparators.get(initialSortColumn).compare(o1, o2);
                }

            });
            // Might as well get the little arrow on the header to make it
            // official
            getColumnSortList().push(new ColumnSortInfo(initialSortColumn, defaultSortOrderMap.get(initialSortColumn)));
            currentlySortedColumn = initialSortColumn;
        }
    }
    
    @Override
    /**
     * DO NOT USE! use setList instead, as else the content will not be sorted correctly!
     */
    public void setRowData(int start, List<? extends T> values) {
        super.setRowData(start, values);
    }
    
    public ListDataProvider<T> getDataProvider() {
        return dataProvider;
    }

    @SuppressWarnings("unchecked")
    public void restoreColumnSortInfos(Column<T, ?> defaultSortColumn) {
        final ColumnSortList sortList = this.getColumnSortList();
        final List<ColumnSortInfo> oldSortInfos;
        if (sortList.size() == 0) {
            boolean ascending = defaultSortOrderMap.get(defaultSortColumn);
            comparators.get(defaultSortColumn).setAscending(ascending);
            oldSortInfos = Collections.singletonList(new ColumnSortInfo(defaultSortColumn, ascending));
        } else {
            oldSortInfos = new ArrayList<ColumnSortInfo>(sortList.size());
            for (int i = sortList.size() - 1; i >= 0; i--) {
                oldSortInfos.add(sortList.get(i));
            }
        }
        oldSortInfos.forEach(sortInfo -> this.sortColumn((Column<T, ?>) sortInfo.getColumn()));
    }
}