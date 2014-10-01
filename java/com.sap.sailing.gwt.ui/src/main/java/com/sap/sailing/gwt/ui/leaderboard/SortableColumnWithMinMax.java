package com.sap.sailing.gwt.ui.leaderboard;

public interface SortableColumnWithMinMax<T, C> {
    /**
     * Based on the table rows, updates the min/max values for this column. Triggered when the leaderboard updates its rows.
     */
    void updateMinMax();
}
