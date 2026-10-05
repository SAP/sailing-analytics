package com.sap.sailing.android.tracking.app.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sap.sailing.android.shared.data.CheckinUrlInfo;
import com.sap.sailing.android.shared.data.LeaderboardInfo;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.AbstractBaseActivity;
import com.sap.sailing.android.shared.util.NetworkHelper;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.ui.fragments.BuoyFragment;
import com.sap.sailing.android.tracking.app.utils.AboutHelper;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.android.tracking.app.utils.CheckoutHelper;
import com.sap.sailing.android.tracking.app.utils.DatabaseHelper;
import com.sap.sailing.android.tracking.app.valueobjects.BoatInfo;
import com.sap.sailing.android.tracking.app.valueobjects.EventInfo;
import com.sap.sailing.android.tracking.app.valueobjects.MarkInfo;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;

import org.json.JSONObject;

public class BuoyActivity extends AbstractBaseActivity {

    private static String TAG = BuoyActivity.class.getName();

    private EventInfo event;
    private MarkInfo mark;
    private BoatInfo boat;
    private LeaderboardInfo leaderboard;
    private CheckinUrlInfo checkinUrl;
    private String checkinDigest;

    private AppPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        checkinDigest = intent.getStringExtra(getString(R.string.checkin_digest));

        prefs = new AppPreferences(this);
        mark = DatabaseHelper.getInstance().getMarkInfo(this, checkinDigest);
        boat = DatabaseHelper.getInstance().getBoatInfo(this, checkinDigest);
        event = DatabaseHelper.getInstance().getEventInfo(this, checkinDigest);
        leaderboard = DatabaseHelper.getInstance().getLeaderboard(this, checkinDigest);
        checkinUrl = DatabaseHelper.getInstance().getCheckinUrl(this, checkinDigest);

        setContentView(R.layout.fragment_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setHomeButtonEnabled(true);
                toolbar.setNavigationIcon(R.drawable.sap_logo_64dp);
                int sidePadding = (int) getResources().getDimension(R.dimen.toolbar_left_padding);
                toolbar.setPadding(sidePadding, 0, 0, 0);
                getSupportActionBar().setTitle(leaderboard.displayName);
                getSupportActionBar().setSubtitle(event.name);
                ColorDrawable backgroundDrawable = new ColorDrawable(
                        getResources().getColor(R.color.toolbar_background));
                getSupportActionBar().setBackgroundDrawable(backgroundDrawable);
            }
        }
        if (TextUtils.isEmpty(mark.markId)) {
            replaceFragment(R.id.content_frame, BuoyFragment.newInstance(boat.boatName, boat.boatColor));
        } else {
            replaceFragment(R.id.content_frame, BuoyFragment.newInstance(mark.markName, null));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (prefs != null && prefs.getTrackerIsTracking()) {
            String checkinDigest = prefs.getTrackerIsTrackingCheckinDigest();
            startTrackingActivity(checkinDigest);
        }
    }

    @Override
    protected int getOptionsMenuResId() {
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.buoy_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.options_menu_settings) {
            ExLog.i(this, TAG, "Clicked SETTINGS.");
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.options_menu_checkout) {
            ExLog.i(this, TAG, "Clicked CHECKOUT.");
            displayCheckoutConfirmationDialog();
            return true;
        } else if (id == R.id.options_menu_info) {
            AboutHelper.showInfoActivity(this);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void startTrackingActivity(String checkinDigest) {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra(getString(R.string.tracking_activity_checkin_digest_parameter), checkinDigest);
        startActivity(intent);
    }

    public void startTrackingActivity() {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra(getString(R.string.tracking_activity_checkin_digest_parameter), checkinDigest);
        startActivity(intent);
    }

    private void displayCheckoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning);
        builder.setMessage(R.string.checkout_warning_message);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkout();
            }
        });
        builder.setNegativeButton(android.R.string.no, null);
        builder.show();
    }

    /**
     * Check out from regatta;
     */
    public void checkout() {
        CheckoutHelper checkoutHelper = new CheckoutHelper();
        String secret = Uri.parse(checkinUrl.urlString).getQueryParameter(DeviceMappingConstants.URL_SECRET);
        String id = TextUtils.isEmpty(mark.markId) ? boat.boatId : mark.markId;
        int type = TextUtils.isEmpty(mark.markId) ? CheckinUrlInfo.TYPE_BOAT : CheckinUrlInfo.TYPE_MARK;
        checkoutHelper.checkoutMark(this, secret, leaderboard.name, event.server, id, type,
                new NetworkHelper.NetworkHelperSuccessListener() {
                    @Override
                    public void performAction(JSONObject response) {
                        DatabaseHelper.getInstance().deleteRegattaFromDatabase(BuoyActivity.this, event.checkinDigest);
                        dismissProgressDialog();
                        finish();
                    }
                }, new NetworkHelper.NetworkHelperFailureListener() {
                    @Override
                    public void performAction(NetworkHelper.NetworkHelperError e) {
                        dismissProgressDialog();
                        showErrorPopup(R.string.error, R.string.error_could_not_complete_operation_on_server_try_again);
                    }
                });
    }
}
