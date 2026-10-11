package com.sap.sailing.racecommittee.app.ui.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.core.content.ContextCompat;

import com.sap.sailing.domain.common.racelog.Flags;

import java.util.ArrayList;
import java.util.Locale;

public class FlagsResources {

    private static ArrayList<Integer> getResId(Context context, String res, int size) {
        ArrayList<Integer> result = new ArrayList<>();
        String flag;
        String outline = "flag_shape_XX_outline_" + size + "dp";

        switch (Flags.valueOf(res.toUpperCase(Locale.US))) {
        case ALPHA:
            flag = "flag_alpha_" + size + "dp";
            outline = outline.replace("XX", "03");
            break;

        case AP:
            flag = "flag_ap_" + size + "dp";
            outline = outline.replace("XX", "02");
            break;

        case BLACK:
            flag = "flag_black_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case BLUE:
            flag = "flag_blue_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case BRAVO:
            flag = "flag_bravo_" + size + "dp";
            outline = outline.replace("XX", "03");
            break;

        case CLASS:
            flag = "flag_class_" + size + "dp";
            outline = outline.replace("XX", "02");
            break;

        case ESSONE:
            flag = "flag_ess1_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case ESSTHREE:
            flag = "flag_ess3_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case ESSTWO:
            flag = "flag_ess2_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case FIRSTSUBSTITUTE:
            flag = "flag_first_substitute_" + size + "dp";
            outline = outline.replace("XX", "04");
            break;

        case FOXTROTT:
            flag = "flag_foxtrott_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case GOLF:
            flag = "flag_golf_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case HOTEL:
            flag = "flag_hotel_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case INDIA:
            flag = "flag_india_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case INDIA_ZULU:
            flag = "flag_india_zulu_96dp";
            outline = null; // because of custom xml
            break;

        case JURY:
            flag = "flag_jury_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case NOVEMBER:
            flag = "flag_november_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case OSCAR:
            flag = "flag_oscar_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case PAPA:
            flag = "flag_papa_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case SWC_FIVE:
            flag = "flag_swc_five_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case SWC_FOUR:
            flag = "flag_swc_four_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case SWC_ONE:
            flag = "flag_swc_one_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case SWC_THREE:
            flag = "flag_swc_three_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case SWC_TWO:
            flag = "flag_swc_two_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case SWC_ZERO:
            flag = "flag_swc_zero_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case UNIFORM:
            flag = "flag_uniform_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case XRAY:
            flag = "flag_xray_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        case ZULU:
            flag = "flag_zulu_" + size + "dp";
            outline = outline.replace("XX", "01");
            break;

        default:
            flag = "flag_alpha_32dp";
            outline = "flag_shape_03_outline_32dp";
        }

        result.add(context.getResources().getIdentifier(flag, "drawable", context.getPackageName()));

        if (outline != null) {
            result.add(context.getResources().getIdentifier(outline, "drawable", context.getPackageName()));
        }

        return result;
    }

    public static LayerDrawable getFlagDrawable(Context context, String flag, int size) {
        ArrayList<Integer> drawables = FlagsResources.getResId(context, flag, size);
        ArrayList<Drawable> layers = new ArrayList<>();
        for (Integer resId : drawables) {
            if (resId != 0) {
                layers.add(ContextCompat.getDrawable(context, resId));
            }
        }
        return new LayerDrawable(layers.toArray(new Drawable[layers.size()]));
    }
}
