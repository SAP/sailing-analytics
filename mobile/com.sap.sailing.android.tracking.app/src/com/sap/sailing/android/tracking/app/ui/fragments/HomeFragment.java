package com.sap.sailing.android.tracking.app.ui.fragments;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AlertDialog;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.data.BaseCheckinData;
import com.sap.sailing.android.shared.data.CheckinUrlInfo;
import com.sap.sailing.android.shared.data.LeaderboardInfo;
import com.sap.sailing.android.shared.data.http.HttpJsonPostRequest;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.fragments.AbstractHomeFragment;
import com.sap.sailing.android.shared.util.NetworkHelper;
import com.sap.sailing.android.shared.util.NetworkHelper.NetworkHelperError;
import com.sap.sailing.android.shared.util.NetworkHelper.NetworkHelperFailureListener;
import com.sap.sailing.android.shared.util.NetworkHelper.NetworkHelperSuccessListener;
import com.sap.sailing.android.tracking.app.BuildConfig;
import com.sap.sailing.android.tracking.app.R;
import com.sap.sailing.android.tracking.app.adapter.RegattaAdapter;
import com.sap.sailing.android.tracking.app.provider.AnalyticsContract;
import com.sap.sailing.android.tracking.app.provider.AnalyticsDatabase;
import com.sap.sailing.android.tracking.app.ui.activities.BuoyActivity;
import com.sap.sailing.android.tracking.app.ui.activities.RegattaActivity;
import com.sap.sailing.android.tracking.app.ui.activities.StartActivity;
import com.sap.sailing.android.tracking.app.utils.AppPreferences;
import com.sap.sailing.android.tracking.app.utils.CheckinHelper;
import com.sap.sailing.android.tracking.app.utils.CheckinManager;
import com.sap.sailing.android.tracking.app.utils.CheckoutHelper;
import com.sap.sailing.android.tracking.app.utils.DatabaseHelper;
import com.sap.sailing.android.tracking.app.utils.DatabaseHelper.GeneralDatabaseHelperException;
import com.sap.sailing.android.tracking.app.valueobjects.BoatCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.BoatInfo;
import com.sap.sailing.android.tracking.app.valueobjects.CheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CompetitorCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.CompetitorInfo;
import com.sap.sailing.android.tracking.app.valueobjects.EventInfo;
import com.sap.sailing.android.tracking.app.valueobjects.MarkCheckinData;
import com.sap.sailing.android.tracking.app.valueobjects.MarkInfo;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class HomeFragment extends AbstractHomeFragment implements LoaderCallbacks<Cursor> {

    private final static String TAG = HomeFragment.class.getName();

    private ListView mListView;
    private View mHeader;

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        prefs = new AppPreferences(getActivity());

        mListView = (ListView) view.findViewById(R.id.listRegatta);
        if (mListView != null) {
            mHeader = inflater.inflate(R.layout.regatta_listview_header, mListView, false);
            setFooterView(inflater);

            adapter = new RegattaAdapter(getActivity(), R.layout.ragatta_listview_row, null, 0);
            mListView.setAdapter(adapter);
            mListView.setOnItemClickListener(new ItemClickListener());
            mListView.setOnItemLongClickListener(new LongItemClickListener());
        }

        getLoaderManager().initLoader(REGATTA_LOADER, null, this);

        return view;
    }

    private void setFooterView(LayoutInflater inflater) {
        View footer = inflater.inflate(R.layout.regatta_listview_footer, mListView, false);
        TextView text = (TextView) footer.findViewById(R.id.list_header_url);
        final String url = getString(R.string.footer_text_link);
        SpannableString spannable = new SpannableString(url);
        ClickableSpan click = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // just for styling the link
                // isn't working in a ListView -> see ItemClickListener
            }
        };
        spannable.setSpan(click, 0, url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setText(spannable);
        mListView.addFooterView(footer);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(REGATTA_LOADER, null, this);

        String lastQRCode = prefs.getLastScannedQRCode();
        if (lastQRCode != null) {
            handleQRCode(lastQRCode);
        }
    }

    /**
     * Perform a checkin request and launch RegattaAcitivity afterwards
     */
    private void checkInWithAPIAndDisplayTrackingActivity(CheckinData checkinData) {
        if (checkinData instanceof CompetitorCheckinData) {
            storeCompetitorCheckinData((CompetitorCheckinData) checkinData);
        } else if (checkinData instanceof MarkCheckinData || checkinData instanceof BoatCheckinData) {
            storeMarkCheckinData(checkinData);
        }
        performAPICheckin(checkinData);
    }

    private void storeCompetitorCheckinData(CompetitorCheckinData checkinData) {
        if (DatabaseHelper.getInstance().eventLeaderboardCompetitorCombinationAvailable(getActivity(),
                checkinData.checkinDigest)) {
            try {
                DatabaseHelper.getInstance().storeCompetitorCheckinRow(getActivity(), checkinData);
                adapter.notifyDataSetChanged();
            } catch (GeneralDatabaseHelperException e) {
                ExLog.e(getActivity(), TAG, "Batch insert failed: " + e.getMessage());
                ((StartActivity) getActivity()).displayDatabaseError();
                return;
            }

            if (BuildConfig.DEBUG) {
                ExLog.i(getActivity(), TAG, "Batch-insert of checkinData completed.");
            }
        } else {
            ExLog.w(getActivity(), TAG, "Combination of eventId, leaderboardName and competitorId already exists!");
            Toast.makeText(getActivity(), getString(R.string.info_already_checked_in_this_qr_code), Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void storeMarkCheckinData(CheckinData checkinData) {
        if (DatabaseHelper.getInstance().eventLeaderboardMarkCombinationAvailable(getActivity(),
                checkinData.checkinDigest)) {
            try {
                if (checkinData instanceof MarkCheckinData) {
                    DatabaseHelper.getInstance().storeMarkCheckinRow(getActivity(), (MarkCheckinData) checkinData);
                } else {
                    DatabaseHelper.getInstance().storeBoatCheckinRow(getActivity(), (BoatCheckinData) checkinData);
                }
                adapter.notifyDataSetChanged();
            } catch (GeneralDatabaseHelperException e) {
                ExLog.e(getActivity(), TAG, "Batch insert failed: " + e.getMessage());
                ((StartActivity) getActivity()).displayDatabaseError();
                return;
            }

            if (BuildConfig.DEBUG) {
                ExLog.i(getActivity(), TAG, "Batch-insert of checkinData completed.");
            }
        } else {
            ExLog.w(getActivity(), TAG, "Combination of eventId, leaderboardName and markID already exists!");
            Toast.makeText(getActivity(), getString(R.string.info_already_checked_in_this_qr_code), Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Checkin with API.
     *
     * @param checkinData
     */
    private void performAPICheckin(CheckinData checkinData) {
        Date date = new Date();
        StartActivity startActivity = (StartActivity) getActivity();
        startActivity.showProgressDialog(R.string.please_wait, R.string.checking_in);
        try {
            JSONObject requestObject = null;
            if (checkinData instanceof CompetitorCheckinData) {
                CompetitorCheckinData competitorCheckinData = (CompetitorCheckinData) checkinData;
                requestObject = CheckinHelper.getCompetitorCheckinJson(
                        competitorCheckinData.secret, competitorCheckinData.competitorId,
                        competitorCheckinData.deviceUid, "TODO push device ID!!",
                        date.getTime());
            } else if (checkinData instanceof MarkCheckinData) {
                MarkCheckinData markCheckinData = (MarkCheckinData) checkinData;
                requestObject = CheckinHelper.getMarkCheckinJson(
                        markCheckinData.secret, markCheckinData.getMark().getId().toString(),
                        markCheckinData.deviceUid, "TODO push device ID!!",
                        date.getTime());
            } else if (checkinData instanceof BoatCheckinData) {
                BoatCheckinData boatCheckinData = (BoatCheckinData) checkinData;
                requestObject = CheckinHelper.getBoatCheckinJson(
                        boatCheckinData.secret, boatCheckinData.getBoat().getId().toString(),
                        boatCheckinData.deviceUid, "TODO push device ID!!",
                        date.getTime());
            }
            HttpJsonPostRequest request = new HttpJsonPostRequest(getActivity(), new URL(checkinData.checkinURL),
                    requestObject.toString());
            NetworkHelper.getInstance(getActivity()).executeHttpJsonRequestAsync(request,
                    new CheckinListener(checkinData.checkinDigest, checkinData.getCheckinType()),
                    new CheckinErrorListener(checkinData.checkinDigest));
        } catch (JSONException | NullPointerException e) {
            ExLog.e(getActivity(), TAG, "Failed to generate checkin JSON: " + e.getMessage());
            displayAPIErrorRecommendRetry();
        } catch (MalformedURLException e) {
            ExLog.e(getActivity(), TAG, "Failed to perform checkin, MalformedURLException: " + e.getMessage());
            displayAPIErrorRecommendRetry();
        }
    }

    @Override
    public void handleScannedOrUrlMatchedUri(Uri uri) {
        String uriString = uri.toString();
        CheckinManager manager = new CheckinManager(uriString, (StartActivity) getActivity(), false);
        manager.callServerAndGenerateCheckinData();
    }

    /**
     * Display a confirmation-dialog in which the user confirms his full name and sail-id.
     */
    @Override
    public void displayUserConfirmationScreen(final BaseCheckinData data) {
        if (data instanceof CompetitorCheckinData) {
            final CompetitorCheckinData checkinData = (CompetitorCheckinData) data;
            String message1 = getString(R.string.confirm_data_hello_name).replace("{full_name}",
                    checkinData.competitorName);
            String message2 = getString(R.string.confirm_data_you_are_signed_in_as_sail_id).replace("{sail_id}",
                    checkinData.competitorSailId.isEmpty() ? checkinData.competitorName : checkinData.competitorSailId);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(message1 + "\n\n" + message2);
            builder.setCancelable(true);
            builder.setPositiveButton(getString(R.string.confirm_data_is_correct),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clearScannedQRCodeInPrefs();
                            checkInWithAPIAndDisplayTrackingActivity(checkinData);
                        }
                    }).setNegativeButton(R.string.decline_data_is_incorrect, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clearScannedQRCodeInPrefs();
                            dialog.cancel();
                        }
                    });
            builder.show();
        } else if (data instanceof MarkCheckinData) {
            MarkCheckinData checkinData = (MarkCheckinData) data;
            clearScannedQRCodeInPrefs();
            checkInWithAPIAndDisplayTrackingActivity(checkinData);
        } else if (data instanceof BoatCheckinData) {
            BoatCheckinData checkinData = (BoatCheckinData) data;
            clearScannedQRCodeInPrefs();
            checkInWithAPIAndDisplayTrackingActivity(checkinData);
        }
    }

    /**
     * Start regatta activity.
     *
     * @param checkinDigest
     */
    private void startRegatta(String checkinDigest, int type) {
        Intent intent = new Intent();
        if (type == CheckinUrlInfo.TYPE_COMPETITOR) {
            intent.setClass(getActivity(), RegattaActivity.class);
        } else if (type == CheckinUrlInfo.TYPE_MARK || type == CheckinUrlInfo.TYPE_BOAT) {
            intent.setClass(getActivity(), BuoyActivity.class);
        }
        intent.putExtra(getString(R.string.checkin_digest), checkinDigest);
        getActivity().startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
        case REGATTA_LOADER:
            String[] projection = new String[] {
                    AnalyticsDatabase.Tables.EVENTS + "." + AnalyticsContract.Event.EVENT_CHECKIN_DIGEST,
                    AnalyticsDatabase.Tables.EVENTS + "." + AnalyticsContract.Event._ID,
                    AnalyticsDatabase.Tables.EVENTS + "." + AnalyticsContract.Event.EVENT_NAME,
                    AnalyticsDatabase.Tables.EVENTS + "." + AnalyticsContract.Event.EVENT_SERVER,
                    AnalyticsDatabase.Tables.LEADERBOARDS + "."
                            + AnalyticsContract.Leaderboard.LEADERBOARD_DISPLAY_NAME,
                    AnalyticsDatabase.Tables.COMPETITORS + "." + AnalyticsContract.Competitor.COMPETITOR_DISPLAY_NAME,
                    AnalyticsDatabase.Tables.MARKS + "." + AnalyticsContract.Mark.MARK_NAME,
                    AnalyticsDatabase.Tables.CHECKIN_URIS + "." + AnalyticsContract.Checkin.CHECKIN_TYPE,
                    AnalyticsDatabase.Tables.BOATS + "." + AnalyticsContract.Boat.BOAT_NAME,
                    AnalyticsDatabase.Tables.BOATS + "." + AnalyticsContract.Boat.BOAT_COLOR };
            return new CursorLoader(getActivity(),
                    AnalyticsContract.LeaderboardsEventsCompetitorsMarksBoatsJoined.CONTENT_URI, projection, null, null,
                    null);

        default:
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
        case REGATTA_LOADER:
            mListView.removeHeaderView(mHeader);
            if (cursor != null && cursor.getCount() > 0) {
                mListView.addHeaderView(mHeader);
            }
            adapter.changeCursor(cursor);
            break;

        default:
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case REGATTA_LOADER:
            mListView.removeHeaderView(mHeader);
            adapter.changeCursor(null);
            break;

        default:
            break;
        }
    }

    private void reloadList() {
        getLoaderManager().restartLoader(REGATTA_LOADER, null, this);
    }

    private boolean showDeleteConfirmationDialog(int position) {
        // -1, because there's a header row
        Cursor cursor = (Cursor) adapter.getItem(position - 1);
        final String checkinDigest = cursor
                .getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Event.EVENT_CHECKIN_DIGEST));
        final int type = cursor.getInt(cursor.getColumnIndexOrThrow(AnalyticsContract.Checkin.CHECKIN_TYPE));
        DatabaseHelper.getInstance().getEventInfo(getActivity(), checkinDigest);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage(getString(R.string.confirm_delete_checkin));
        builder.setCancelable(true);
        builder.setNegativeButton(getString(R.string.no), null);
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteRegatta(checkinDigest, type);
            }
        });
        builder.show();

        return true;
    }

    private void deleteRegatta(final String checkinDigest, final int type) {
        NetworkHelperSuccessListener successListener = new NetworkHelperSuccessListener() {
            @Override
            public void performAction(JSONObject response) {
                dismissProgressDialogDeleteRegattaAndReloadList(checkinDigest);
            }
        };
        NetworkHelperFailureListener failureListener = new NetworkHelperFailureListener() {
            @Override
            public void performAction(NetworkHelperError e) {
                dismissProgressDialogDeleteRegattaAndReloadList(checkinDigest);
            }
        };
        CheckoutHelper checkoutHelper = new CheckoutHelper();
        CheckinUrlInfo checkinUrl = DatabaseHelper.getInstance().getCheckinUrl(getActivity(), checkinDigest);
        String secret = Uri.parse(checkinUrl.urlString).getQueryParameter(DeviceMappingConstants.URL_SECRET);
        LeaderboardInfo leaderboardInfo = DatabaseHelper.getInstance().getLeaderboard(getActivity(), checkinDigest);
        EventInfo eventInfo = DatabaseHelper.getInstance().getEventInfo(getActivity(), checkinDigest);
        if (type == CheckinUrlInfo.TYPE_COMPETITOR) {
            CompetitorInfo competitorInfo = DatabaseHelper.getInstance().getCompetitor(getActivity(), checkinDigest);
            checkoutHelper.checkoutCompetitor((StartActivity) getActivity(), secret, leaderboardInfo.name, eventInfo.server,
                    competitorInfo.id, successListener, failureListener);
        } else if (type == CheckinUrlInfo.TYPE_MARK) {
            MarkInfo markInfo = DatabaseHelper.getInstance().getMarkInfo(getActivity(), checkinDigest);
            checkoutHelper.checkoutMark((StartActivity) getActivity(), secret, leaderboardInfo.name, eventInfo.server,
                    markInfo.markId, type, successListener, failureListener);
        } else if (type == CheckinUrlInfo.TYPE_BOAT) {
            BoatInfo boatInfo = DatabaseHelper.getInstance().getBoatInfo(getActivity(), checkinDigest);
            checkoutHelper.checkoutMark((StartActivity) getActivity(), secret, leaderboardInfo.name, eventInfo.server,
                    boatInfo.boatId, type, successListener, failureListener);
        }
    }

    public void dismissProgressDialogDeleteRegattaAndReloadList(final String checkinDigest) {
        StartActivity startActivity = (StartActivity) getActivity();
        startActivity.dismissProgressDialog();
        DatabaseHelper.getInstance().deleteRegattaFromDatabase(getActivity(), checkinDigest);
        reloadList();
    }

    private class ItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (position == adapter.getCount() + 1 || adapter.getCount() == 0) { // footer
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.footer_text_link)));
                startActivity(browserIntent);
                return;
            }

            if (position < 1) { // header
                return;
            }

            // -1, because there's a header row
            Cursor cursor = (Cursor) adapter.getItem(position - 1);

            String checkinDigest = cursor
                    .getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Event.EVENT_CHECKIN_DIGEST));
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(AnalyticsContract.Checkin.CHECKIN_TYPE));
            startRegatta(checkinDigest, type);
        }
    }

    private class LongItemClickListener implements OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (position < 1 || adapter.getCount() == 0 || position == adapter.getCount() + 1) // tapped header
            {
                return false;
            }
            return showDeleteConfirmationDialog(position);
        }
    }

    private class CheckinListener implements NetworkHelperSuccessListener {

        public String checkinDigest;
        public int type;

        public CheckinListener(String checkinDigest, int type) {
            this.checkinDigest = checkinDigest;
            this.type = type;
        }

        @Override
        public void performAction(JSONObject response) {
            StartActivity startActivity = (StartActivity) getActivity();
            startActivity.dismissProgressDialog();
            startRegatta(checkinDigest, type);
        }
    }

    private class CheckinErrorListener implements NetworkHelperFailureListener {

        public String checkinDigest;

        public CheckinErrorListener(String checkinDigest) {
            this.checkinDigest = checkinDigest;
        }

        @Override
        public void performAction(NetworkHelperError e) {
            if (e.getMessage() != null) {
                ExLog.e(getActivity(), TAG, e.getMessage());
            } else {
                ExLog.e(getActivity(), TAG, "Unknown Error");
            }

            StartActivity startActivity = (StartActivity) getActivity();
            startActivity.dismissProgressDialog();
            startActivity.showErrorPopup(R.string.error,
                    R.string.error_could_not_complete_operation_on_server_try_again);

            DatabaseHelper.getInstance().deleteRegattaFromDatabase(getActivity(), checkinDigest);
            Toast.makeText(getActivity(), getString(R.string.error_while_receiving_server_data), Toast.LENGTH_LONG)
                    .show();
        }
    }

}
