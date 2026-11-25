package com.sap.sailing.android.buoy.positioning.app.ui.activities;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.GeneralPreferenceFragment;
import com.sap.sailing.android.shared.ui.customviews.OpenSansToolbar;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

public class SettingActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pref_with_actionbar);

        setupToolbar();

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new GeneralPreferenceFragment())
                .commit();
    }

    private void setupToolbar() {
        OpenSansToolbar toolbar = (OpenSansToolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.hideSubtitle();
            toolbar.setTitleSize(20);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            setSupportActionBar(toolbar);
            int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
            toolbar.setPadding(sidePadding, 0, 0, 0);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            ColorDrawable backgroundDrawable = new ColorDrawable(
                    ContextCompat.getColor(this, R.color.toolbar_background));
            getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
            toolbar.setNavigationIcon(R.drawable.sap_logo_64dp);
            getSupportActionBar().setTitle(getString(R.string.settings));
        }
    }

}
