package com.sap.sailing.android.tracking.app.ui.activities;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.sap.sailing.android.shared.BuildConfig;
import com.sap.sailing.android.shared.R;
import com.sap.sailing.android.shared.data.BaseCheckinData;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.CheckinDataActivity;
import com.sap.sailing.android.shared.ui.customviews.OpenSansToolbar;
import com.sap.sailing.android.ui.fragments.AbstractHomeFragment;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

public abstract class AbstractBranchStartActivity<C extends BaseCheckinData> extends CheckinDataActivity<C> {

    private final static String TAG = com.sap.sailing.android.tracking.app.ui.activities.AbstractBranchStartActivity.class.getName();
    protected OpenSansToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);
        toolbar = (OpenSansToolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.hideSubtitle();
            toolbar.setTitleSize(20);
            toolbar.setNavigationIcon(R.drawable.sap_logo_64dp);
            toolbar.setPadding(20, 0, 0, 0);
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    @Override
    public void onStart() {

        super.onStart();

        Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    try {
                        Boolean clickedBranchLink = referringParams.getBoolean("+clicked_branch_link");
                        if (!clickedBranchLink) {
                            AbstractBranchStartActivity.this.handleLegacyStart();
                            return;
                        }
                        ExLog.i(AbstractBranchStartActivity.this, "BRANCH SDK", referringParams.toString());
                        String checkinUrl = referringParams.getString("checkinUrl");
                        if (checkinUrl != null) {
                            if (BuildConfig.DEBUG) {
                                ExLog.i(AbstractBranchStartActivity.this, TAG, "Matched URL, handling  matched URL.");
                            }
                            getHomeFragment().handleScannedOrUrlMatchedUri(Uri.parse(checkinUrl));
                            // if we don't clear the intent data here the next onStart cycle
                            // will error in the legacy start procedure while trying to interpret
                            // the branch.io deeplink as legacy link
                            AbstractBranchStartActivity.this.getIntent().setData(null);
                        }
                    } catch (JSONException e) {
                        ExLog.ex(AbstractBranchStartActivity.this, TAG, e);
                    }
                } else {
                    ExLog.i(AbstractBranchStartActivity.this, "BRANCH SDK", error.getMessage());
                }
            }
        }, this.getIntent().getData(), this);

    }

    private void handleLegacyStart() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            if (BuildConfig.DEBUG) {
                ExLog.i(this, TAG, "Matched URL, handling scanned or matched URL.");
            }
            getHomeFragment().handleScannedOrUrlMatchedUri(uri);
        }
        intent.setData(null);
    }

    public abstract AbstractHomeFragment getHomeFragment();

    @Override
    protected void onResume() {
        super.onResume();
        // checkForUpdates();

        int googleServicesResultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (googleServicesResultCode != ConnectionResult.SUCCESS) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googleServicesResultCode, this, 0);
            dialog.show();
        }
    }
}
