package com.sap.sailing.android.buoy.positioning.app.ui.activities;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.HomeFragment;
import com.sap.sailing.android.buoy.positioning.app.util.AboutHelper;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.CheckinData;
import com.sap.sailing.android.shared.ui.activities.AbstractStartActivity;
import com.sap.sailing.android.shared.ui.fragments.AbstractHomeFragment;
import com.sap.sailing.android.shared.util.EulaHelper;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class StartActivity extends AbstractStartActivity<CheckinData> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_start));
            getSupportActionBar().setHomeButtonEnabled(false);
            ColorDrawable backgroundDrawable = new ColorDrawable(ContextCompat.getColor(this, R.color.toolbar_background));
            getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
            int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
            toolbar.setPadding(sidePadding, 0, 0, 0);
        }
        replaceFragment(R.id.content_frame, new HomeFragment());

        if (!EulaHelper.with(this).isEulaAccepted()) {
            EulaHelper.with(this).showEulaDialog(R.style.AppTheme_AlertDialog);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                AboutHelper.showInfoActivity(this);
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getOptionsMenuResId() {
        return 0;
    }

    public AbstractHomeFragment getHomeFragment() {
        HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        return homeFragment;
    }

    @Override
    public void onCheckinDataAvailable(CheckinData data) {
        if (data != null) {
            getHomeFragment().displayUserConfirmationScreen(data);
        }
    }

}
