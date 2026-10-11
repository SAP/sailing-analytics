package com.sap.sailing.racecommittee.app.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.TimePicker;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ThemeHelper {

    private static final String TAG = ThemeHelper.class.getName();

    public static void positioningPopupMenu(Context context, PopupMenu popupMenu, View anchor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Try to force some vertical offset
            try {
                Object menuHelper;
                Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
                fMenuHelper.setAccessible(true);
                menuHelper = fMenuHelper.get(popupMenu);
                Field fListPopup = menuHelper.getClass().getDeclaredField("mPopup");
                fListPopup.setAccessible(true);
                Object listPopup = fListPopup.get(menuHelper);
                Class<?> listPopupClass = listPopup.getClass();

                int height = anchor.getHeight();
                // Invoke setVerticalOffset() with the negative height to move up by that distance
                Method setVerticalOffset = listPopupClass.getDeclaredMethod("setVerticalOffset", int.class);
                setVerticalOffset.invoke(listPopup, -height);

                int width = (Integer) listPopupClass.getDeclaredMethod("getWidth").invoke(listPopup);
                width -= anchor.getWidth();
                // Invoke setHorizontalOffset() with the negative height to move up by that distance
                Method setHorizontalOffset = listPopupClass.getDeclaredMethod("setHorizontalOffset", int.class);
                setHorizontalOffset.invoke(listPopup, -width);

                // Invoke show() to update the window's position
                Method show = listPopupClass.getDeclaredMethod("show");
                show.invoke(listPopup);
            } catch (Exception e) {
                // an exception here indicates a programming error rather than an exceptional condition
                // at runtime
                ExLog.w(context, TAG, "Unable to force offset" + e.getLocalizedMessage());
            }
        }
    }

    public static void setPickerTextColor(Context context, TimePicker timePicker, @ColorInt int color) {
        NumberPicker hourPicker = getHourPicker(timePicker);
        if (hourPicker != null) {
            setPickerTextColor(context, hourPicker, color);
        }

        NumberPicker minutePicker = getMinutePicker(timePicker);
        if (minutePicker != null) {
            setPickerTextColor(context, minutePicker, color);
        }
    }

    public static void setPickerTextColor(Context context, NumberPicker numberPicker, @ColorInt int color) {
        final int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText) {
                try {
                    Field selectorWheelPaintField = numberPicker.getClass().getDeclaredField("mSelectorWheelPaint");
                    selectorWheelPaintField.setAccessible(true);
                    ((Paint) selectorWheelPaintField.get(numberPicker)).setColor(color);
                    ((EditText) child).setTextColor(color);
                    numberPicker.invalidate();
                } catch (NoSuchFieldException e) {
                    ExLog.w(context, TAG, "NoSuchFieldException - " + e.getMessage());
                } catch (IllegalAccessException e) {
                    ExLog.w(context, TAG, "IllegalAccessException - " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    ExLog.w(context, TAG, "IllegalArgumentException - " + e.getMessage());
                }
            }
        }
    }

    public static void setPickerDividerColor(Context context, TimePicker timePicker, @ColorInt int color) {
        NumberPicker hourPicker = getHourPicker(timePicker);
        if (hourPicker != null) {
            setPickerDividerColor(context, hourPicker, color);
        }

        NumberPicker minutePicker = getMinutePicker(timePicker);
        if (minutePicker != null) {
            setPickerDividerColor(context, minutePicker, color);
        }
    }

    public static void setPickerDividerColor(Context context, NumberPicker numberPicker, @ColorInt int color) {
        try {
            ColorDrawable drawable = new ColorDrawable(color);
            Field selectionDivider = numberPicker.getClass().getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            selectionDivider.set(numberPicker, drawable);
        } catch (NoSuchFieldException e) {
            ExLog.w(context, TAG, "NoSuchFieldException - " + e.getMessage());
        } catch (IllegalArgumentException e) {
            ExLog.w(context, TAG, "IllegalArgumentException - " + e.getMessage());
        } catch (IllegalAccessException e) {
            ExLog.w(context, TAG, "IllegalAccessException - " + e.getMessage());
        }
    }

    public static void setPickerColor(Context context, TimePicker timePicker, @ColorInt int textColor,
            @ColorInt int dividerColor) {
        setPickerTextColor(context, timePicker, textColor);
        setPickerDividerColor(context, timePicker, dividerColor);
    }

    public static void setPickerColor(Context context, NumberPicker numberPicker, @ColorInt int textColor,
            @ColorInt int dividerColor) {
        setPickerTextColor(context, numberPicker, textColor);
        setPickerDividerColor(context, numberPicker, dividerColor);
    }

    public static void setPickerTextSize(Context context, NumberPicker numberPicker, @DimenRes int dimen) {
        final int count = numberPicker.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = numberPicker.getChildAt(i);
            if (child instanceof EditText) {
                try {
                    ((EditText) child).setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            context.getResources().getDimension(dimen));
                    numberPicker.invalidate();
                } catch (IllegalArgumentException e) {
                    ExLog.w(context, TAG, "IllegalArgumentException - " + e.getMessage());
                }
            }
        }
    }

    public static @ColorInt int getColor(Context context, @AttrRes int colorId) {
        int color = 0;
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        if (theme.resolveAttribute(colorId, typedValue, true)) {
            color = context.getResources().getColor(typedValue.resourceId);
        }
        return color;
    }

    private static NumberPicker getHourPicker(TimePicker timePicker) {
        Resources system = Resources.getSystem();
        int hourId = system.getIdentifier("hour", "id", "android");
        return ViewHelper.get(timePicker, hourId);
    }

    private static NumberPicker getMinutePicker(TimePicker timePicker) {
        Resources system = Resources.getSystem();
        int minuteId = system.getIdentifier("minute", "id", "android");
        return ViewHelper.get(timePicker, minuteId);
    }
}
