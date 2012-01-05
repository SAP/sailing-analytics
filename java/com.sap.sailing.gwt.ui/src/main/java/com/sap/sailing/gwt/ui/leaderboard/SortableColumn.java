package com.sap.sailing.gwt.ui.leaderboard;

import java.util.Comparator;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.Header;
import com.sap.sailing.gwt.ui.shared.LeaderboardDAO;
import com.sap.sailing.gwt.ui.shared.LeaderboardRowDAO;

public abstract class SortableColumn<T, C> extends Column<T, C> {
    protected SortableColumn(Cell<C> cell) {
        super(cell);
        setSortable(true);
    }
    
    protected void updateMinMax(LeaderboardDAO leaderboard) {}
    
    /**
     * To enable sorting of <code>null</code> values to the end even in ascending order, the comparators provided
     * by the subclasses will want to know whether the leaderboard table is currently sorted in ascending order
     * for this column.
     */
    protected boolean isSortedAscendingForThisColumn(CellTable<LeaderboardRowDAO> leaderboardTable) {
        ColumnSortList sortList = leaderboardTable.getColumnSortList();
        return sortList.size() > 0 && sortList.get(0).getColumn() == this && sortList.get(0).isAscending();
    }
    
    public abstract Comparator<T> getComparator();
    
    public abstract Header<?> getHeader();

    /**
     * Allows a column to specify a style/CSS class to use to format the &lt;th&gt; header cell.
     * This default implementation returns <code>null</code>, meaning that no additional style other
     * than the GWT-provided default styles will be used.
     */
    public String getHeaderStyle() {
        return null;
    }
    
    /**
     * Allows a column to specify a style/CSS class to use to format the &lt;col&gt; element.
     * This default implementation returns <code>null</code>, meaning that no additional style other
     * than the GWT-provided default styles will be used.
     */
    public String getColumnStyle() {
        return null;
    }
}
