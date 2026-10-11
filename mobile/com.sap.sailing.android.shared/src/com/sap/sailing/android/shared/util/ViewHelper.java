package com.sap.sailing.android.shared.util;

import com.sap.sse.common.impl.HSVColor;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ViewHelper {

    @SuppressWarnings("unchecked")
    public static <T extends View> T get(View view, int id) {
        SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
        if (viewHolder == null) {
            viewHolder = new SparseArray<View>();
            view.setTag(viewHolder);
        }

        View childView = viewHolder.get(id);
        if (childView == null) {
            childView = view.findViewById(id);
            viewHolder.put(id, childView);
        }

        return (T) childView;
    }

    public static void disableSave(View view) {
        view.setSaveFromParentEnabled(false);
        view.setSaveEnabled(false);
    }

    public static void setSiblingsVisibility(View view, int visibility) {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            int max = parent.getChildCount();
            View child;
            for (int i = 0; i < max; i++) {
                child = parent.getChildAt(i);
                if (!child.equals(view)) {
                    child.setVisibility(visibility);
                }
            }
        }
    }

    /**
     * Set the background to the color and tint the text color to be readable
     *
     * @param view
     *            TextView to colorize
     * @param backgroundColor
     *            background color
     */
    public static void setColors(@NonNull TextView view, @Nullable String backgroundColor) {
        com.sap.sse.common.Color color = HSVColor.getCssColor(backgroundColor);
        if (color != null) {
            float textColor = (1 - color.getAsHSV().getC()) * 255f;
            view.setTextColor(Color.argb(255, (int) textColor, (int) textColor, (int) textColor));
            view.setBackgroundColor(Color.parseColor(backgroundColor));
        }
    }
}
