package com.sap.sailing.racecommittee.app.ui.utils;

import java.util.ArrayList;

import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util.Triple;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;

public class BuoyHelper {

    public static LayerDrawable getBuoy(Context context, MarkType type, Color color, String shape, String pattern) {
        LayerDrawable drawable = null;
        ArrayList<Drawable> layers = new ArrayList<>();
        Drawable shadowDrawable;
        Drawable outlineDrawable;
        Drawable colorDrawable;
        if (TextUtils.isEmpty(shape) && type == MarkType.BUOY) { // default buoy
            shadowDrawable = ContextCompat.getDrawable(context, R.drawable.buoy_shadow);
            final int outlineColor; // as 0xAARRGGBB
            final int colorColor; // as 0xAARRGGBB
            outlineColor = ContextCompat.getColor(context, R.color.buoy_outline);
            if (color == null) {
                colorColor = ContextCompat.getColor(context, R.color.buoy_color);
            } else {
                colorColor = getAndroidIntColor(color);
            }
            outlineDrawable = BitmapHelper.getTintedDrawable(context, R.drawable.buoy_outline, outlineColor);
            colorDrawable = BitmapHelper.getTintedDrawable(context, R.drawable.buoy_color, colorColor);
            layers.add(shadowDrawable);
            layers.add(outlineDrawable);
            layers.add(colorDrawable);
            drawable = new LayerDrawable(layers.toArray(new Drawable[layers.size()]));
        }
        return drawable;
    }

    /**
     * Turns a {@link Color} object into an Android color in the form {@code 0xAARRGGBB} with Alpha value 0. For
     * {@code null} values of {@code color}, {@code 0} is returned.
     */
    private static int getAndroidIntColor(Color color) {
        final int result;
        if (color == null) {
            result = 0;
        } else {
            Triple<Integer, Integer, Integer> rgb = color.getAsRGB();
            result = android.graphics.Color.rgb(rgb.getA(), rgb.getB(), rgb.getC());
        }
        return result;
    }
}
