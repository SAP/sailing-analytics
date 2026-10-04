package com.sap.sailing.android.tracking.app.ui.activities;

import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.ui.fragments.LeaderboardFragment;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

public class LeaderboardWebViewActivity extends BaseActivity {

    public final static String LEADERBOARD_EXTRA_SERVER_URL = "leaderboardExtraServerUrl";
    public final static String LEADERBOARD_EXTRA_EVENT_ID = "leaderboardExtraEventId";
    public final static String LEADERBOARD_EXTRA_LEADERBOARD_NAME = "leaderboardExtraLeaderboardName";

    public String serverUrl;
    public String eventId;
    public String leaderboardName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        serverUrl = intent.getStringExtra(LEADERBOARD_EXTRA_SERVER_URL);
        eventId = intent.getStringExtra(LEADERBOARD_EXTRA_EVENT_ID);
        leaderboardName = intent.getStringExtra(LEADERBOARD_EXTRA_LEADERBOARD_NAME);

        setContentView(R.layout.fragment_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            toolbar.setNavigationIcon(R.drawable.sap_logo_64dp);
            int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
            toolbar.setPadding(sidePadding, 0, 0, 0);
            getSupportActionBar().setTitle(getString(R.string.title_activity_leaderboard));
            ColorDrawable backgroundDrawable = new ColorDrawable(getResources().getColor(R.color.toolbar_background));
            getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
        }
        View view = findViewById(R.id.toolbar_subtitle);
        if (view != null) {
            view.setVisibility(View.GONE);
        }

        replaceFragment(R.id.content_frame, new LeaderboardFragment());
    }
}
