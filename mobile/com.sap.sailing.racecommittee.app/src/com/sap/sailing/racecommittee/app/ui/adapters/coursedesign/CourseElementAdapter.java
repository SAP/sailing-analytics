package com.sap.sailing.racecommittee.app.ui.adapters.coursedesign;

import java.util.List;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.CourseListDataElementWithIdImpl;
import com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist.BaseDraggableSwipeAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist.BaseDraggableSwipeViewHolder;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.CourseFragmentMarks;
import com.sap.sailing.racecommittee.app.ui.utils.MarkImageHelper;

import android.content.Context;
import android.graphics.Color;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class CourseElementAdapter extends BaseDraggableSwipeAdapter<RecyclerView.ViewHolder> {

    private final static String TAG = CourseElementAdapter.class.getName();

    public final static int TOUCH_LEFT_AREA = 1;
    public final static int TOUCH_TYPE_AREA = 2;
    public final static int TOUCH_RIGHT_AREA = 3;

    private final static int ITEM_VIEW = 0;
    private final static int ADD_VIEW = 1;

    private Context mContext;
    private List<CourseListDataElementWithIdImpl> mItems;
    private MarkImageHelper mImageHelper;
    private CourseFragmentMarks mParent;

    private boolean mEditable;

    public CourseElementAdapter(Context context, List<CourseListDataElementWithIdImpl> elements,
            MarkImageHelper imageHelper, boolean editable, CourseFragmentMarks parent) {
        super(context, elements, parent);
        setHasStableIds(true);
        mContext = context;
        mItems = elements;
        mImageHelper = imageHelper;
        mEditable = editable;
        mParent = parent;
    }

    @Override
    public int getItemViewType(int position) {
        if (mItems != null && mItems.size() != position) {
            return ITEM_VIEW;
        }
        return ADD_VIEW;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout;

        switch (viewType) {
        case ADD_VIEW:
            layout = LayoutInflater.from(mContext).inflate(R.layout.course_marks_waypoint_new_item, parent, false);
            return new AddItemHolder(layout, mParent);
        default:
            layout = LayoutInflater.from(mContext).inflate(R.layout.course_marks_waypoint_item, parent, false);
            return new ItemViewHolder(layout, mParent);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddItemHolder) {
            AddItemHolder itemHolder = (AddItemHolder) holder;
            itemHolder.hintText.setVisibility((position == 0) ? View.VISIBLE : View.GONE);
            return;
        }
        CourseListDataElement element = mItems.get(position);
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        itemHolder.dragHandle.setVisibility((mEditable) ? View.VISIBLE : View.GONE);
        itemHolder.dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mDragListener.onStartDrag(holder);
                }
                return false;
            }
        });
        itemHolder.leftText.setVisibility(View.GONE);
        itemHolder.leftImage.setVisibility(View.GONE);
        if (element.getLeftMark() != null) {
            itemHolder.leftText.setText(element.getLeftMark().getName());
            itemHolder.leftText.setVisibility(View.VISIBLE);
            itemHolder.leftImage.setImageDrawable(mImageHelper.resolveMarkImage(mContext, element.getLeftMark()));
            itemHolder.leftImage.setVisibility(View.VISIBLE);
        }
        itemHolder.roundingDirection.setVisibility(View.GONE);
        if (element.getPassingInstructions() != null) {
            itemHolder.roundingDirection.setText(getDisplayValueForRounding(element.getPassingInstructions()));
            itemHolder.roundingDirection.setVisibility(View.VISIBLE);
        }
        itemHolder.rightText.setVisibility(View.GONE);
        itemHolder.rightImage.setVisibility(View.GONE);
        itemHolder.addItem.setVisibility(View.GONE);
        if (element.getRightMark() != null) {
            itemHolder.rightText.setText(element.getRightMark().getName());
            itemHolder.rightText.setVisibility(View.VISIBLE);
            itemHolder.rightImage.setImageDrawable(mImageHelper.resolveMarkImage(mContext, element.getRightMark()));
            itemHolder.rightImage.setVisibility(View.VISIBLE);
        } else if (element.getPassingInstructions() != null
                && (PassingInstruction.Gate.equals(element.getPassingInstructions())
                || PassingInstruction.Line.equals(element.getPassingInstructions()))) {
            itemHolder.addItem.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        if (mEditable) {
            if (mItems != null && mItems.size() != 0) {
                CourseListDataElementWithIdImpl element = mItems.get(mItems.size() - 1);
                if (element.getRightMark() == null && (PassingInstruction.Gate.equals(element.getPassingInstructions())
                        || PassingInstruction.Line.equals(element.getPassingInstructions()))) {
                    return mItems.size();
                }
                return mItems.size() + 1;
            }
            return 1;
        } else {
            return mItems.size();
        }
    }

    @Override
    public long getItemId(int position) {
        if (mItems != null && mItems.size() != position) {
            return mItems.get(position).getId();
        } else {
            return super.getItemId(position);
        }
    }

    private static String getDisplayValueForRounding(PassingInstruction direction) {
        switch (direction) {
        case Gate:
            return "G";
        case Port:
            return "P";
        case Single_Unknown:
            return "U";
        case Starboard:
            return "S";
        case Line:
            return "L";
        case Offset:
            return "O";
        default:
            return "";
        }
    }

    private static Pair<Integer, Integer> getItemDraggableRange(List items) {
        if (items == null) {
            return new Pair<Integer, Integer>(-1, -1);
        }
        final int start = 0;
        final int end = items.size() - 1;
        return new Pair<Integer, Integer>(start, end);
    }

    @Override
    public void onItemRemove(int position) {
        mParent.onItemRemoved();
        super.onItemRemove(position);
    }

    public class AddItemHolder extends RecyclerView.ViewHolder implements BaseDraggableSwipeViewHolder {

        private TextView hintText;

        public AddItemHolder(View itemView, CourseFragmentMarks listener) {
            super(itemView);

            hintText = ViewHelper.get(itemView, R.id.hint_text);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.showMarkDialog(TOUCH_TYPE_AREA, null);
                }
            });

        }

        @Override
        public void onItemSelected() {
        }

        @Override
        public void onItemClear() {
        }

        @Override
        public boolean isDragAllowed() {
            return false;
        }
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder
            implements BaseDraggableSwipeViewHolder, View.OnClickListener {

        public ViewGroup container;
        public View dragHandle;
        public View leftColumn;
        public TextView leftText;
        public ImageView leftImage;
        public TextView roundingDirection;
        public View rightColumn;
        public TextView rightText;
        public ImageView rightImage;
        public ImageView addItem;

        private CourseFragmentMarks mItemClickListener;

        public ItemViewHolder(View itemView, CourseFragmentMarks listener) {
            super(itemView);

            container = ViewHelper.get(itemView, R.id.container);
            dragHandle = ViewHelper.get(itemView, R.id.drag_handle);
            leftColumn = ViewHelper.get(itemView, R.id.column_left);
            leftText = ViewHelper.get(itemView, R.id.column_left_text);
            leftImage = ViewHelper.get(itemView, R.id.column_left_image);
            roundingDirection = ViewHelper.get(itemView, R.id.rounding_direction);
            rightColumn = ViewHelper.get(itemView, R.id.column_right);
            rightText = ViewHelper.get(itemView, R.id.column_right_text);
            rightImage = ViewHelper.get(itemView, R.id.column_right_image);
            addItem = ViewHelper.get(itemView, R.id.add_item);

            mItemClickListener = listener;
            if (mEditable) {
                if (leftColumn != null) {
                    leftColumn.setOnClickListener(this);
                }
                if (roundingDirection != null) {
                    roundingDirection.setOnClickListener(this);
                }
                if (rightColumn != null) {
                    rightColumn.setOnClickListener(this);
                }
                if (addItem != null) {
                    addItem.setOnClickListener(this);
                }
            }
        }

        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == R.id.column_left) {
                mItemClickListener.showMarkDialog(TOUCH_LEFT_AREA, mItems.get(getAdapterPosition()));
            } else if (id == R.id.column_right) {
                if (addItem.getVisibility() == View.GONE) {
                    mItemClickListener.showMarkDialog(TOUCH_RIGHT_AREA, mItems.get(getAdapterPosition()));
                }
            } else if (id == R.id.rounding_direction) {
                mItemClickListener.onItemEditClick(TOUCH_TYPE_AREA, mItems.get(getAdapterPosition()));
            } else {
                mItemClickListener.showMarkDialog(TOUCH_TYPE_AREA, null);
            }
        }

        /**
         * Called when the {@link ItemTouchHelper} first registers an item as being moved or swiped. Implementations
         * should update the item view to indicate it's active state.
         */
        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        /**
         * Called when the {@link ItemTouchHelper} has completed the move or swipe, and the active item state should be
         * cleared.
         */
        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }

        @Override
        public boolean isDragAllowed() {
            Pair<Integer, Integer> range = getItemDraggableRange(mItems);
            return range.first <= getAdapterPosition() && getAdapterPosition() <= range.second;
        }
    }
}
