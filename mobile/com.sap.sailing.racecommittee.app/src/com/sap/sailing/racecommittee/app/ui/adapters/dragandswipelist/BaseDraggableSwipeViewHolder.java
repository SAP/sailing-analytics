package com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist;

import androidx.recyclerview.widget.ItemTouchHelper;

public interface BaseDraggableSwipeViewHolder {

    /**
     * Called when the {@link ItemTouchHelper} first registers an item as being moved or swiped. Implementations should
     * update the item view to indicate it's active state.
     */
    void onItemSelected();

    /**
     * Called when the {@link ItemTouchHelper} has completed the move or swipe, and the active item state should be
     * cleared.
     */
    void onItemClear();

    /**
     * Called when the {@link ItemTouchHelper} registers an item as being moved or swiped or when it is manipulated
     * (e.g. pushed) by other items. Implementations should indicate if the item may be manipulated.
     */
    boolean isDragAllowed();

}
