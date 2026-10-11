package com.sap.sailing.racecommittee.app.ui.utils;

import com.sap.sailing.domain.common.MarkType;
import com.sap.sse.common.Color;

import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.DrawableRes;

public class MarkImageDescriptor {
    private final int drawableId;
    private final Color color;
    private final String shape;
    private final String pattern;
    private final MarkType type;
    private final Context context;

    public MarkImageDescriptor(Context context, int drawableId, MarkType type, Color color, String shape,
            String pattern) {
        this.drawableId = drawableId;
        this.type = type;
        this.color = color;
        this.shape = shape;
        this.pattern = pattern;
        this.context = context;
    }

    public @DrawableRes int getDrawableId() {
        return drawableId;
    }

    /**
     * @param instanceColor
     *            when this descriptor has {@code null{} as its {@link #color} then {@code instanceColor} can be used to
     *            provide a specific color; if {@code instanceColor} is {@code null}, whatever the value of {@link
     *            #color} is will be used.
     */
    public LayerDrawable getDrawable(Color instanceColor) {
        return BuoyHelper.getBuoy(context, type, instanceColor == null ? color : instanceColor, shape, pattern);
    }

    public String getShape() {
        return shape;
    }

    public String getPattern() {
        return pattern;
    }

    public MarkType getType() {
        return type;
    }

    /**
     * An utility method to check if the mark icon has compatible display properties. In case of null values at least
     * the mark type and the color must exist and fit.
     */
    public int getCompatibilityLevel(MarkType typeToCheck, Color colorToCheck, String shapeToCheck,
            String patternToCheck) {
        boolean isSameType = false;
        boolean isSameColor = false;
        boolean isSameShape = false;
        boolean isSamePattern = false;
        int result = -1;
        if (typeToCheck != null && type != null) {
            isSameType = type == typeToCheck;
        }
        if (colorToCheck != null && color != null) {
            isSameColor = color.equals(colorToCheck);
        }
        if (shapeToCheck != null && shape != null) {
            isSameShape = shape.equalsIgnoreCase(shapeToCheck);
        }
        if (patternToCheck != null && pattern != null) {
            isSamePattern = pattern.equalsIgnoreCase(patternToCheck);
        }
        if (isSameType && isSameColor) {
            result = 1;
            if (isSameShape) {
                result += 1;
            }
            if (isSamePattern) {
                result += 1;
            }
        }
        return result;
    }
}
