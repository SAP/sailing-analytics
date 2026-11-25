package com.sap.sailing.racecommittee.app.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.DataStore;
import com.sap.sailing.racecommittee.app.ui.fragments.LoginBackdrop;

public class PasswordActivity extends BaseActivity {

    private IntentReceiver mReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.password_activity);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_layout, LoginBackdrop.newInstance());
        transaction.commit();

        mReceiver = new IntentReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();

        BroadcastManager.getInstance(this).addIntent(new Intent(AppConstants.ACTION_CHECK_LOGIN));

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.ACTION_VALID_DATA);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            DataStore dataStore = DataManager.create(PasswordActivity.this).getDataStore();
            Intent start;
            if (dataStore.getCourseAreaId() != null) {
                start = new Intent(PasswordActivity.this, RacingActivity.class);
                start.putExtra(AppConstants.EXTRA_COURSE_UUID, dataStore.getCourseAreaId());
                start.putExtra(AppConstants.EXTRA_EVENT_ID, dataStore.getEventUUID());
            } else {
                start = new Intent(PasswordActivity.this, LoginActivity.class);
            }
            startActivity(start);
            finish();
        }
    }
}
