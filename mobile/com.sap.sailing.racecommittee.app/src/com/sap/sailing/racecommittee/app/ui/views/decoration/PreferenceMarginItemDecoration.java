package com.sap.sailing.racecommittee.app.ui.views.decoration;

import com.sap.sailing.racecommittee.app.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class PreferenceMarginItemDecoration extends RecyclerView.ItemDecoration {

    private int mMargin;
    private Drawable mBackground;
    private int mItemHeight;

    public PreferenceMarginItemDecoration(Context context, int margin) {
        mMargin = margin;
        mBackground = new ColorDrawable(context.getResources().getColor(R.color.light_sap_gray_black_30));
        mItemHeight = context.getResources().getDimensionPixelSize(R.dimen.preference_item_height);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);

        int spanCount = getSpanCount(parent);
        int rowCount = parent.getChildCount();
        if (spanCount % 2 == 0) {
            if (rowCount % 2 != 0) {
                rowCount = (rowCount + 1) / 2;
            } else {
                rowCount /= 2;
            }
        }
        mBackground.setBounds(0, 0, parent.getWidth(), mItemHeight * rowCount);
        mBackground.draw(c);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        int spanCount = getSpanCount(parent);
        int marginLeft = mMargin;
        int marginRight = mMargin;
        if (spanCount != 1) {
            int pos = parent.getChildLayoutPosition(view) + 1;
            if (pos % 2 == 0) {
                marginLeft = marginLeft / 2;
            } else {
                marginRight = marginRight / 2;
            }
        }
        outRect.set(marginLeft, 0, marginRight, 0);
    }

    private int getSpanCount(RecyclerView parent) {
        int spanCount = 1;
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
        }
        return spanCount;
    }
}
