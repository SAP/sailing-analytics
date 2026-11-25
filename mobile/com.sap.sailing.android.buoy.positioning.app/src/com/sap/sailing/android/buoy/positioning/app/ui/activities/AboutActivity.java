package com.sap.sailing.android.buoy.positioning.app.ui.activities;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.AboutFragment;
import com.sap.sailing.android.shared.ui.customviews.OpenSansToolbar;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.Menu;

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
