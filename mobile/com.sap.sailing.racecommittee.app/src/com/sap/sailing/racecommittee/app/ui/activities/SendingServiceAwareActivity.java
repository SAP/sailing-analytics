package com.sap.sailing.racecommittee.app.ui.activities;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.MessageSendingBinder;
import com.sap.sailing.android.shared.services.sending.MessageSendingService.MessageSendingServiceLogger;
import com.sap.sailing.android.shared.ui.activities.ResilientActivity;
import com.sap.sailing.android.shared.util.PrefUtils;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import java.util.Date;

public abstract class SendingServiceAwareActivity extends ResilientActivity {
    
    private class MessageSendingServiceConnection implements ServiceConnection, MessageSendingServiceLogger {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MessageSendingBinder binder = (MessageSendingBinder) service;
            sendingService = binder.getService();
            boundSendingService = true;
            sendingService.setMessageSendingServiceLogger(this);
            updateSendingServiceInformation();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg) {
            boundSendingService = false;
        }

        @Override
        public void onMessageSentSuccessful() {
            updateSendingServiceInformation();
        }

        @Override
        public void onMessageSentFailed() {
            updateSendingServiceInformation();
        }
    }

    private static final String TAG = SendingServiceAwareActivity.class.getName();

    protected MenuItem menuItemLive;
    protected int menuItemLiveId = -1;

    protected boolean boundSendingService = false;
    protected MessageSendingService sendingService;
    private MessageSendingServiceConnection sendingServiceConnection;
    
    private String sendingServiceStatus = "";
    
    public SendingServiceAwareActivity() {
        this.sendingServiceConnection = new MessageSendingServiceConnection();
    }

    @Override
    public void onStart() {
        super.onStart();
        
        Intent intent = new Intent(this, MessageSendingService.class);
        bindService(intent, sendingServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onStop() {
        super.onStop();
        
        if (boundSendingService) {
            unbindService(sendingServiceConnection);
            boundSendingService = false;
        }
    }

    protected void updateSendingServiceInformation() {
        if (menuItemLive == null)
            return;
        
        if (!boundSendingService)
            return;
        
        int errorCount = this.sendingService.getDelayedIntentsCount();
        if (errorCount > 0) {
            menuItemLive.setIcon(getTintedDrawable(getResources(), R.drawable.ic_share_white_36dp, ThemeHelper.getColor(this, R.attr.sap_red_1)));
            Date lastSuccessfulSend = this.sendingService.getLastSuccessfulSend();
            String statusText = getString(R.string.events_waiting_to_be_sent);
            sendingServiceStatus = String.format(statusText,
                    errorCount, lastSuccessfulSend == null ? getString(R.string.never) : lastSuccessfulSend);
        } else {
            menuItemLive.setIcon(R.drawable.ic_share_white_36dp);
            sendingServiceStatus = getString(R.string.no_event_to_be_sent);
        }
    }
    
    /**
     * @return the resource ID for the options menu, {@code 0} if none.
     *          The menu item displaying the connection status is added automatically.
     */
    protected abstract int getOptionsMenuResId();
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_live_status, menu);
        menuItemLive = menu.findItem(R.id.options_menu_live);
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

    private String getLiveIconText() {
        return String.format(getString(R.string.connected_to_wp), PrefUtils.getString(this, R.string.preference_server_url_key,
                R.string.preference_server_url_default), sendingServiceStatus);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Drawable getTintedDrawable(Resources res, @DrawableRes int drawableResId, @ColorRes int color) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = getDrawable(drawableResId);
        } else {
            drawable = res.getDrawable(drawableResId);
        }
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return drawable;
    }
}
