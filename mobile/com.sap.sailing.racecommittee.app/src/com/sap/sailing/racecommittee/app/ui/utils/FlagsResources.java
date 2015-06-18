package com.sap.sailing.racecommittee.app.ui.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.utils.BitmapHelper;

import java.util.ArrayList;

public class FlagsResources {

    private static ArrayList<Integer> getResId(Context context, String res, int size) {
        ArrayList<Integer> result = new ArrayList<>();
        String flag;
        String outline = "flag_shape_XX_outline_" + size + "dp";

        switch (Flags.valueOf(res)) {
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
            flag = "flag_india_zulu_" + AppPreferences.on(context).getTheme();
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

        case PAPA:
            flag = "flag_papa_" + size + "dp";
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

        if (AppConstants.LIGHT_THEME.equals(AppPreferences.on(context).getTheme()) && outline != null) {
            result.add(context.getResources().getIdentifier(outline, "drawable", context.getPackageName()));
        }

        return result;
    }

    public static LayerDrawable getFlagDrawable(Context context, String flag, int size) {
        ArrayList<Integer> drawables = FlagsResources.getResId(context, flag, size);
        ArrayList<Drawable> layers = new ArrayList<>();
        for (Integer resId : drawables) {
            if (resId != 0) {
                layers.add(BitmapHelper.getDrawable(context, resId));
            }
        }
        return new LayerDrawable(layers.toArray(new Drawable[layers.size()]));
    }
}
