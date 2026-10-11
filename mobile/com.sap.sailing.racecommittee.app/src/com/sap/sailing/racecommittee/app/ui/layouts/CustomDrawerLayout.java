package com.sap.sailing.racecommittee.app.ui.layouts;

import android.content.Context;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomDrawerLayout extends DrawerLayout {
    public CustomDrawerLayout(Context context) {
        super(context);
    }

    public CustomDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Fix for multitouch issue see: https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=3659
            return false;
        }
    }
}
