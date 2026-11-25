package com.sap.sailing.racecommittee.app.ui.utils;

import java.lang.ref.WeakReference;

import androidx.core.view.ViewConfigurationCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class TouchEventListener {

    private WeakReference<View> mView;
    private Runnable mClickRunnable;
    private float mStartX;
    private float mStartY;
    private boolean mInXRange;
    private boolean mInYRange;
    private int mTouchSlop;
    private boolean mEnabled;

    public TouchEventListener(View view) {
        mView = new WeakReference<>(view);

        final ViewConfiguration configuration = ViewConfiguration.get(view.getContext());
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);

        mEnabled = true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        View view = mView.get();

        if (view == null) {
            return false;
        }

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mInXRange = true;
            mInYRange = true;
            view.getParent().requestDisallowInterceptTouchEvent(true);
            mStartX = event.getX();
            mStartY = event.getY();
            return true;

        case MotionEvent.ACTION_MOVE:
            boolean inLimit = false;
            if (mInXRange) {
                inLimit = Math.abs(event.getX() - mStartX) <= mTouchSlop;
                mInXRange = inLimit;
            } else {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            if (mInYRange) {
                mInYRange = Math.abs(event.getY() - mStartY) <= mTouchSlop;
            }
            return inLimit;

        case MotionEvent.ACTION_UP:
            view.getParent().requestDisallowInterceptTouchEvent(false);
            if (mInXRange && mEnabled) {
                if (mClickRunnable != null) {
                    mClickRunnable.run();
                    return true;
                }
            }
            break;

        default:
            break;
        }
        return false;
    }

    public void setClickRunnable(Runnable runnable) {
        mClickRunnable = runnable;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}
