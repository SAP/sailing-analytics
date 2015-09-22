package com.sap.sailing.racecommittee.app.ui.views.decoration;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class PhotoListItemDecoration extends RecyclerView.ItemDecoration {

    private int mPadding;

    public PhotoListItemDecoration(int padding) {
        mPadding = padding;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        int paddingLeft = 0;
        int paddingRight = mPadding;

        int pos = parent.getChildAdapterPosition(view);
        if (pos == 0) {
            paddingLeft = mPadding;
        }
        outRect.set(paddingLeft, 0, paddingRight, 0);
    }
}
