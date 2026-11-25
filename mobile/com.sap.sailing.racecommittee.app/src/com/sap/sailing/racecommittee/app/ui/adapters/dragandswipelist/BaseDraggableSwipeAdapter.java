package com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public abstract class BaseDraggableSwipeAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH>
        implements ItemTouchHelperAdapter {

    protected List mItems;

    protected HolderAwareOnDragListener mDragListener;

    public BaseDraggableSwipeAdapter(Context context, List items, HolderAwareOnDragListener dragListener) {
        super();
        mItems = items;
        mDragListener = dragListener;
    }

    public void setDragListener(HolderAwareOnDragListener listener) {
        mDragListener = listener;
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /**
     * Called when an item has been dragged far enough to trigger a move. This is called every time an item is shifted,
     * and <strong>not</strong> at the end of a "drop" event.<br/>
     * <br/>
     * Implementations should call {@link RecyclerView.Adapter#notifyItemMoved(int, int)} after adjusting the underlying
     * data to reflect this move.
     *
     * @param fromPosition The start position of the moved item.
     * @param toPosition   Then resolved position of the moved item.
     * @return True if the item was moved to the new adapter position.
     * @see RecyclerView#getAdapterPositionFor(RecyclerView.ViewHolder)
     * @see RecyclerView.ViewHolder#getAdapterPosition()
     */
    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mItems, i, i + 1);
            }
        } else if (fromPosition > toPosition) {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mItems, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    /**
     * Called when an item has been dismissed by a swipe.<br/>
     * <br/>
     * Implementations should call {@link RecyclerView.Adapter#notifyItemRemoved(int)} after adjusting the underlying
     * data to reflect this removal.
     *
     * @param position The position of the item dismissed.
     * @see RecyclerView#getAdapterPositionFor(RecyclerView.ViewHolder)
     * @see RecyclerView.ViewHolder#getAdapterPosition()
     */
    @Override
    public void onItemRemove(int position) {
        mItems.remove(position);
        notifyItemRemoved(position);
    }
}
