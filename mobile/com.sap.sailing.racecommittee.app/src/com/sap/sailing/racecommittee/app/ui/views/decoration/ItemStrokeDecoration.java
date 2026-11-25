package com.sap.sailing.racecommittee.app.ui.views.decoration;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class ItemStrokeDecoration extends RecyclerView.ItemDecoration {

    private int mPadding;
    private int mPaddingDouble;
    private Paint mLinePaint;

    public ItemStrokeDecoration(int padding, float strokeWidth, int color) {
        mPadding = padding;
        mPaddingDouble = padding * 2;

        mLinePaint = new Paint();
        mLinePaint.setStrokeWidth(strokeWidth);
        mLinePaint.setColor(color);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        outRect.set(mPaddingDouble, mPaddingDouble, mPaddingDouble, mPaddingDouble);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);

        final RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();

        for (int i = 0; i < parent.getChildCount(); i++) {
            final View child = parent.getChildAt(i);
            c.drawRect(layoutManager.getDecoratedLeft(child) + mPadding,
                    layoutManager.getDecoratedTop(child) + mPadding, layoutManager.getDecoratedRight(child) - mPadding,
                    layoutManager.getDecoratedBottom(child) - mPadding, mLinePaint);
        }
    }
}
