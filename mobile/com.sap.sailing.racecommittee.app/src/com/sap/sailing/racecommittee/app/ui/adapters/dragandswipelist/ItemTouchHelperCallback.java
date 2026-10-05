package com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.View;

import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.racecommittee.app.R;

/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and swipe-to-dismiss. Drag
 * events are automatically started by an item long-press.<br/>
 * </br/>
 * Expects the <code>RecyclerView.Adapter</code> to listen for {@link ItemTouchHelperAdapter} callbacks and the
 * <code>RecyclerView.ViewHolder</code> to implement {@link BaseDraggableSwipeViewHolder}.
 */
public class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private final ItemTouchHelperAdapter mAdapter;

    public ItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        if (viewHolder instanceof BaseDraggableSwipeViewHolder) {
            boolean isDragAllowed = ((BaseDraggableSwipeViewHolder) viewHolder).isDragAllowed();
            return makeMovementFlags(isDragAllowed ? dragFlags : 0, swipeFlags);
        }
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        if (source.getItemViewType() != target.getItemViewType()) {
            return false;
        }
        if (target instanceof BaseDraggableSwipeViewHolder
                && !((BaseDraggableSwipeViewHolder) target).isDragAllowed()) {
            return false;
        }
        // Notify the adapter of the move
        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        // Notify the adapter of the dismissal
        mAdapter.onItemRemove(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        boolean hasContainer = itemView.findViewById(R.id.container) != null;
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && hasContainer) {
            itemView.setTranslationX(dX);

            int bgRes = R.attr.swipe_idle;
            if (dX < 0) {
                bgRes = R.attr.swipe_left;
            } else if (dX > 0) {
                bgRes = R.attr.swipe_right;
            }

            Drawable background = BitmapHelper.getAttrDrawable(recyclerView.getContext(), bgRes);
            background.setBounds(0, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        // We only want the active item to change
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && viewHolder instanceof BaseDraggableSwipeViewHolder) {
            // Let the view holder know that this item is being moved or dragged
            BaseDraggableSwipeViewHolder itemViewHolder = (BaseDraggableSwipeViewHolder) viewHolder;
            itemViewHolder.onItemSelected();
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof BaseDraggableSwipeViewHolder) {
            // Tell the view holder it's time to restore the idle state
            BaseDraggableSwipeViewHolder itemViewHolder = (BaseDraggableSwipeViewHolder) viewHolder;
            itemViewHolder.onItemClear();
        }
        super.clearView(recyclerView, viewHolder);
    }
}
