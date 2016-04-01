package com.sap.sailing.android.shared.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;

import com.sap.sailing.android.shared.R;

public class LocationHelper {

    /**
     * Helper method to check if device receives gps updates
     * @param context
     * @return
     */
    public static boolean isGPSEnabled(Context context) {
        LocationManager service = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return service.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Helper method that checks if gps is activated in the settings and shows and error message if not.
     * @param context
     */
    public static void checkGPSAvailable(final Context context) {
        checkGPSAvailable(context, null);
    }

    /**
     * Helper method that checks if gps is activated in the settings and shows and error message if not.
     * @param context
     * @param errorMessage
     */
    public static void checkGPSAvailable(final Context context, String errorMessage) {
        if (!isGPSEnabled(context)) {
            showNoGPSError(context, errorMessage);
        }
    }

    /**
     * Show an alert dialog that sends the user to android location settings when tapping on the positive button.
     * @param context
     * @param errorMessage
     */
    public static void showNoGPSError(final Context context, String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true).setTitle(context.getString(R.string.warning))
            .setNegativeButton(context.getString(R.string.no), null)
            .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openLocationSettings(context);
                }
            });
        if (errorMessage != null) {
            builder.setMessage(errorMessage);
        } else {
            builder.setMessage(context.getString(R.string.enable_gps));
        }
        builder.show();
    }

    /**
     * Helper method to access androids location settings
     * @param context
     */
    public static void openLocationSettings(Context context) {
        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }
}
