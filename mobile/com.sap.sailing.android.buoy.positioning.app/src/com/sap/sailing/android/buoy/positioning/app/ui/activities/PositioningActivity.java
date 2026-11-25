package com.sap.sailing.android.buoy.positioning.app.ui.activities;

import java.util.List;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.BuoyFragment;
import com.sap.sailing.android.buoy.positioning.app.ui.fragments.BuoyFragment.pingListener;
import com.sap.sailing.android.buoy.positioning.app.util.AboutHelper;
import com.sap.sailing.android.buoy.positioning.app.util.DatabaseHelper;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkInfo;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkPingInfo;
import com.sap.sailing.android.shared.data.LeaderboardInfo;
import com.sap.sailing.android.shared.ui.customviews.OpenSansToolbar;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class PositioningActivity extends BaseActivity implements pingListener {

    private MarkInfo markInfo;
    private MarkPingInfo markPing;
    private LeaderboardInfo leaderBoard;
    private String markIdAsString;
    private String checkinDigest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);
        Intent intent = getIntent();
        markIdAsString = intent.getExtras().getString(getString(R.string.mark_id));
        checkinDigest = intent.getExtras().getString(getString(R.string.checkin_digest));

        loadDataFromDatabase();

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
            getSupportActionBar().setTitle(markInfo.getName());
        }
        BuoyFragment fragment = new BuoyFragment();
        fragment.setPingListener(this);
        replaceFragment(R.id.content_frame, fragment);

    }

    public void loadDataFromDatabase() {
        List<MarkInfo> marks = DatabaseHelper.getInstance().getMarks(this, checkinDigest);
        setLeaderBoard(DatabaseHelper.getInstance().getLeaderboard(this, checkinDigest));
        for (MarkInfo mark : marks) {
            if (mark.getId().toString().equals(markIdAsString)) {
                setMarkInfo(mark);
                break;
            }
        }
        if (markInfo != null) {
            setPingFromDatabase(markIdAsString);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.about_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.about) {
            AboutHelper.showInfoActivity(this);
            return true;
        } else if (id == R.id.settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getOptionsMenuResId() {
        // Set to 0 to avoid redundant menu inflation
        return 0;
    }

    public void setPingFromDatabase(String markerID) {
        List<MarkPingInfo> markPings = DatabaseHelper.getInstance().getMarkPings(this, markerID);
        if (!markPings.isEmpty()) {
            setMarkPing(markPings.get(0));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        BuoyFragment fragment = (BuoyFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (fragment != null) {
            fragment.setPingListener(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public MarkInfo getMarkInfo() {
        return markInfo;
    }

    public void setMarkInfo(MarkInfo markInfo) {
        this.markInfo = markInfo;
    }

    public MarkPingInfo getMarkPing() {
        return markPing;
    }

    public void setMarkPing(MarkPingInfo markPing) {
        this.markPing = markPing;
    }

    public LeaderboardInfo getLeaderBoard() {
        return leaderBoard;
    }

    public void setLeaderBoard(LeaderboardInfo leaderBoard) {
        this.leaderBoard = leaderBoard;
    }

    @Override
    public void updatePing() {
        setPingFromDatabase(markInfo.getId().toString());
    }
}
