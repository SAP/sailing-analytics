package com.sap.sailing.android.shared.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.MessageSendingBinder;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.MessageSendingServiceLogger;
import com.sap.sailing.android.shared.util.PrefUtils;

import java.util.Date;

public abstract class SendingServiceAwareActivity extends ResilientActivity {

    private static final String TAG = SendingServiceAwareActivity.class.getName();

    private MessageSendingService mService;
    private boolean mBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MessageSendingBinder binder = (MessageSendingBinder) service;
            mService = binder.getService();
            mService.setMessageSendingServiceLogger(logger);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private final MessageSendingServiceLogger logger = new MessageSendingServiceLogger() {
        @Override
        public void onMessageSentSuccessful() {
            updateSendingServiceInformation();
        }

        @Override
        public void onMessageSentFailed() {
            updateSendingServiceInformation();
        }
    };

    private MenuItem mMenuItemLive;
    private String mStatus = "";

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MessageSendingService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(connection);
        mService = null;
        mBound = false;
    }

    public boolean isBound() {
        return mBound;
    }

    @Nullable
    public MessageSendingService getService() {
        return mService;
    }

    /**
     * @return the resource ID for the options menu, {@code 0} if none. The menu item displaying the connection status
     * is added automatically.
     */
    protected abstract int getOptionsMenuResId();

    protected Drawable getMenuItemLiveDrawable() {
        return ContextCompat.getDrawable(this, R.drawable.ic_menu_share);
    }

    protected Drawable getMenuItemLiveErrorDrawable() {
        return ContextCompat.getDrawable(this, R.drawable.ic_menu_share_red);
    }

    @StringRes
    protected int getStatusTextResId() {
        return R.string.sending_waiting;
    }

    protected void updateSendingServiceInformation() {
        if (mMenuItemLive == null) {
            ExLog.w(this, TAG, "updateSendingServiceInformation -> menuItemLive==null");
            return;
        }

        if (!mBound) {
            ExLog.w(this, TAG, "updateSendingServiceInformation -> !boundSendingService");
            return;
        }

        int errorCount = mService.getDelayedIntentsCount();
        if (errorCount > 0) {
            ExLog.i(this, TAG, "updateSendingServiceInformation -> errorCount > 0");
            mMenuItemLive.setIcon(getMenuItemLiveErrorDrawable());
            Date lastSuccessfulSend = mService.getLastSuccessfulSend();
            mStatus = getString(getStatusTextResId(), errorCount,
                    lastSuccessfulSend == null ? "never" : lastSuccessfulSend);
        } else {
            ExLog.i(this, TAG, "updateSendingServiceInformation -> errorCount <= 0");
            mMenuItemLive.setIcon(getMenuItemLiveDrawable());
            mStatus = getString(R.string.sending_no_waiting);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_live_status, menu);
        mMenuItemLive = menu.findItem(R.id.options_menu_live);
        if (getOptionsMenuResId() != 0) {
            inflater.inflate(getOptionsMenuResId(), menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.options_menu_live == item.getItemId()) {
            ExLog.i(this, TAG, "Clicked LIVE.");
            Toast.makeText(this, getLiveIconText(), Toast.LENGTH_LONG).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateSendingServiceInformation();
        return super.onPrepareOptionsMenu(menu);
    }

    protected String getLiveIconText() {
        return getString(R.string.connected_to_wp,
                PrefUtils.getString(this, R.string.preference_server_url_key, R.string.preference_server_url_default),
                mStatus, mBound ? "bound" : "unbound");
    }
}
