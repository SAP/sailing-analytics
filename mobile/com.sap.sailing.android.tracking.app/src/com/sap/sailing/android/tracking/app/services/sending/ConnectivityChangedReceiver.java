/**
 * 
 */
package com.sap.sailing.android.tracking.app.services.sending;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.utils.ServiceHelper;

/**
 * Informs {@link MessageSendingService} whenever connectivity is restored, so that it can start sending
 * messages again.
 * 
 * Register in manifest:
 * <pre>{@code
 * <receiver android:name="com.sap.sailing.android.tracking.app.services.sending.ConnectivityChangedReceiver" >
 *   <intent-filter>
 *     <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
 *   </intent-filter>
 * </receiver>
 * }</pre>
 */
public class ConnectivityChangedReceiver extends BroadcastReceiver {
    
    private final static String TAG = ConnectivityChangedReceiver.class.getName();
    
    /* (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
		final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

		if (networkInfo == null)
		{
			return; // was null when restarted router..
		}
		
		if (!networkInfo.isConnected()) {
			return;
		}
		
		if (BuildConfig.DEBUG) {
			ExLog.i(context, TAG, "Starting TransmittingService");
    	}
		
		ServiceHelper.getInstance().startTransmittingService(context);
        disable(context);
    }

    /**
     * disables the Connectivity Changed Receiver as there is currently connectivity
     * @param context the context to work on
     */
    public static void disable(Context context) {
        ComponentName receiver = new ComponentName(context, ConnectivityChangedReceiver.class);
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        ExLog.w(context, TAG, "Regained connectivity. ConnectivityChangedReceiver disabled");
    }

    /**
     * enables the Connectivity Changed Receiver to listen on connectivity changes as there is currently no connectivity
     * @param context the context to work on
     */
    public static void enable(Context context) {
        ComponentName receiver = new ComponentName(context, ConnectivityChangedReceiver.class);
        PackageManager packageManager = context.getPackageManager();
        packageManager.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        ExLog.w(context, TAG, "Connectivity lost. ConnectivityChangedReceiver enabled");
    }
}
