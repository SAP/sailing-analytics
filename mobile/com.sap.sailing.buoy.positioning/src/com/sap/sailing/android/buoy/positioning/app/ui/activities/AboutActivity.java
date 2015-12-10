package com.sap.sailing.android.buoy.positioning.app.ui.activities;

import android.os.Bundle;
import android.view.Menu;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.AboutFragment;
import com.sap.sailing.android.shared.ui.customviews.OpenSansToolbar;

public class AboutActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.sap.sailing.android.shared.R.layout.fragment_container);

        OpenSansToolbar toolbar = (OpenSansToolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.hideSubtitle();
            toolbar.setTitleSize(20);
            setSupportActionBar(toolbar);
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            toolbar.setNavigationIcon(R.drawable.sap_logo_64_sq);
            toolbar.setPadding(20, 0, 0, 0);
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            getSupportActionBar().setTitle(R.string.about_this_app);
        }
        replaceFragment(R.id.content_frame, AboutFragment.newInstance());
    }

    /**
     * Empty method to avoid creation of parent menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
