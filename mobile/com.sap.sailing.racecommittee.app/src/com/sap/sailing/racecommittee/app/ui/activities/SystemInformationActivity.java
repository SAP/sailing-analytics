package com.sap.sailing.racecommittee.app.ui.activities;

import com.sap.sailing.racecommittee.app.R;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

public class SystemInformationActivity extends BaseActivity {
    private SystemInformationActivityHelper helper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        helper = new SystemInformationActivityHelper(this, preferences.getDeviceConfigurationName());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    protected int getOptionsMenuResId() {
        return 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void updateSendingServiceInformation() {
        helper.updateSendingServiceInformation();
        super.updateSendingServiceInformation();
    }
}
