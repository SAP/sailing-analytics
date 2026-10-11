package com.sap.sailing.android.shared.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.util.TypedValue;
import android.view.View;

/**
 * Utility class for decoding Bitmap files and requiring Drawables.
 */
public class BitmapHelper {

    /**
     * Decodes a bitmap resource, calculates its sample size, and returns the size-reduced Bitmap for that resource.
     *
     * @param res
     *            the resource to decode
     * @param resId
     *            the resource ID
     * @param reqWidth
     *            the required width of the output Bitmap
     * @param reqHeight
     *            the required height of the output Bitmap
     * @return the down-sampled decoded Bitmap
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Decodes a bitmap file, calculates its sample size, and returns the size-reduced Bitmap for that file.
     *
     * @param fileName
     *            the absolute path of the file to decode
     * @param reqWidth
     *            the required width of the output Bitmap
     * @param reqHeight
     *            the required height of the output Bitmap
     * @param preferredConfig
     *            additional preferred config (optional, can be null)
     * @return the down-sampled decoded Bitmap
     */
    public static Bitmap decodeSampleBitmapFromFile(String fileName, int reqWidth, int reqHeight,
            Bitmap.Config preferredConfig) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        if (preferredConfig != null) {
            options.inPreferredConfig = preferredConfig;
        }
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(fileName, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Gets a Drawable from the provided attribute resource.
     *
     * @param context
     *            the context
     * @param attrRes
     *            the attribute resource
     * @return the Drawable of the resource
     */
    public static Drawable getAttrDrawable(Context context, @AttrRes int attrRes) {
        final Drawable drawable;
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attrRes, value, true)) {
            if (value.resourceId != 0) {
                drawable = ContextCompat.getDrawable(context, value.resourceId);
            } else {
                drawable = null;
            }
        } else {
            drawable = null;
        }
        return drawable;
    }

    /**
     * Gets a Drawable from the provided attribute identifier.
     *
     * @param context
     *            the context
     * @param attr
     *            the attribute identifier specifying a Drawable resource
     * @return the Drawable of the resource
     */
    public static Drawable getAttrDrawable(Context context, String attr) {
        int attrRes = context.getResources().getIdentifier(attr, "attr", context.getPackageName());
        if (attrRes != 0) {
            return getAttrDrawable(context, attrRes);
        }
        return null;
    }

    /**
     * Sets the given Drawable as background for the given View.
     *
     * @param view
     *            the view that gets its background set
     * @param drawable
     *            the Drawable for the background
     */
    // @SuppressWarnings, but it is handled correctly
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void setBackground(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    /**
     * Tints a drawable given by its resource ID in the given color.
     *
     * @param context
     *            the context
     * @param drawableResId
     *            the resource ID of the drawable to tint
     * @param color
     *            the color in which the drawable should be tinted, in the form {@code 0xAARRGGBB}
     * @return the tinted Drawable
     */
    public static Drawable getTintedDrawable(Context context, @DrawableRes int drawableResId, int color) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
        drawable = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }

    /**
     * Creates a Bitmap from the given Drawable. Uses {@link Bitmap.Config#ARGB_8888}.
     *
     * @param drawable
     *            the source Drawable
     * @return the Bitmap for that Drawable
     */
    public static Bitmap toBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}
