package com.sap.sailing.racecommittee.app.ui.utils;

import java.util.List;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sse.common.Color;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

public abstract class MarkImageHelper {
    protected MarkImageDescriptor defaultCourseMarkDescriptor;
    List<MarkImageDescriptor> markImageDescriptors;

    public LayerDrawable resolveMarkImage(Context context, Mark mark) {
        LayerDrawable drawable;
        MarkImageDescriptor result = defaultCourseMarkDescriptor;
        int highestCompatibilityLevel = -1;
        for (MarkImageDescriptor imageDescriptor : markImageDescriptors) {
            int compatibilityLevel = imageDescriptor.getCompatibilityLevel(mark.getType(), mark.getColor(),
                    mark.getShape(), mark.getPattern());
            if (compatibilityLevel > highestCompatibilityLevel) {
                result = imageDescriptor;
                highestCompatibilityLevel = compatibilityLevel;
                if (highestCompatibilityLevel == 3) {
                    break;
                }
            }
        }
        if (result.getDrawableId() != 0) {
            drawable = new LayerDrawable(new Drawable[] { ContextCompat.getDrawable(context, result.getDrawableId()) });
        } else {
            drawable = result.getDrawable(mark.getColor());
        }
        return drawable;
    }

    /**
     * @param color
     *            this color string, with {@code buoy_color_} and {@code buoy_outline_} prepended, is matched against
     *            the color constants defined in {@code res/values/colors.xml}. If not matched, the default constants
     *            {@code buoy_color} and {@code buoy_online} from {@code res/values/colors.xml} are used.
     */
    protected MarkImageDescriptor createMarkImageDescriptor(Context context, @DrawableRes int drawableId, MarkType type,
            Color color, String shape, String pattern) {
        MarkImageDescriptor markIconDescriptor = new MarkImageDescriptor(context, drawableId, type, color, shape,
                pattern);
        markImageDescriptors.add(markIconDescriptor);
        return markIconDescriptor;
    }

}
