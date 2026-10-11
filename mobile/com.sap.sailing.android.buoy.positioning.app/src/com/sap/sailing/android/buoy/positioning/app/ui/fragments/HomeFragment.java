package com.sap.sailing.android.buoy.positioning.app.ui.fragments;

import com.sap.sailing.android.buoy.positioning.app.BuildConfig;
import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.adapter.RegattaAdapter;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract;
import com.sap.sailing.android.buoy.positioning.app.ui.activities.RegattaActivity;
import com.sap.sailing.android.buoy.positioning.app.ui.activities.StartActivity;
import com.sap.sailing.android.buoy.positioning.app.util.AppPreferences;
import com.sap.sailing.android.buoy.positioning.app.util.CheckinManager;
import com.sap.sailing.android.buoy.positioning.app.util.DatabaseHelper;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.CheckinData;
import com.sap.sailing.android.shared.data.BaseCheckinData;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.ui.activities.CheckinDataActivity;
import com.sap.sailing.android.shared.ui.fragments.AbstractHomeFragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class HomeFragment extends AbstractHomeFragment implements LoaderCallbacks<Cursor> {

    private final static String TAG = HomeFragment.class.getName();

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        prefs = new AppPreferences(getActivity());

        getLoaderManager().initLoader(REGATTA_LOADER, null, this);
        ListView listView = (ListView) view.findViewById(R.id.listRegatta);
        if (listView != null) {
            listView.addHeaderView(inflater.inflate(R.layout.regatta_listview_header, null));

            adapter = new RegattaAdapter(getActivity(), R.layout.ragatta_listview_row, null, 0);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new ItemClickListener());
        }

        return view;
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

    @Override
    public void displayUserConfirmationScreen(BaseCheckinData data) {
        CheckinData chData = (CheckinData) data;
        checkinWithApiAndStartRegattaActivity(chData);
    }

    private void checkinWithApiAndStartRegattaActivity(CheckinData checkinData) {
        try {
            if (!DatabaseHelper.getInstance().markLeaderboardCombinationAvailable(getActivity(),
                    checkinData.checkinDigest)) {
                DatabaseHelper.getInstance().deleteRegattaFromDatabase(getActivity(), checkinData.checkinDigest);
            }
            DatabaseHelper.getInstance().storeCheckinRow(getActivity(), checkinData.marks, checkinData.getLeaderboard(),
                    checkinData.getCheckinUrl(), checkinData.pings);
            adapter.notifyDataSetChanged();
        } catch (DatabaseHelper.GeneralDatabaseHelperException e) {
            ExLog.e(getActivity(), TAG, "Batch insert failed: " + e.getMessage());
            ((StartActivity) getActivity()).displayDatabaseError();
            return;
        }

        if (BuildConfig.DEBUG) {
            ExLog.i(getActivity(), TAG, "Batch-insert of checkinData completed.");
        }
        startRegatta(checkinData.leaderboardDisplayName, checkinData.checkinDigest);
    }

    @Override
    public void handleScannedOrUrlMatchedUri(Uri uri) {
        @SuppressWarnings("unchecked")
        final CheckinDataActivity<CheckinData> activity = (CheckinDataActivity<CheckinData>) getActivity();
        CheckinManager manager = new CheckinManager(uri.toString(), activity);
        manager.callServerAndGenerateCheckinData();
    }

    /**
     * Start regatta activity.
     *
     * @param leaderboardName
     * @param checkinDigest
     */
    private void startRegatta(String leaderboardName, String checkinDigest) {
        Intent intent = new Intent(getActivity(), RegattaActivity.class);
        intent.putExtra(getString(R.string.leaderboard_name), leaderboardName);
        intent.putExtra(getString(R.string.checkin_digest), checkinDigest);
        getActivity().startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
        case REGATTA_LOADER:
            return new CursorLoader(getActivity(), AnalyticsContract.Leaderboard.CONTENT_URI, null, null, null, null);

        default:
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
        case REGATTA_LOADER:
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
            adapter.changeCursor(null);
            break;

        default:
            break;
        }
    }

    private class ItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (position < 1) // tapped header
            {
                return;
            }

            // -1, because there's a header row
            Cursor cursor = (Cursor) adapter.getItem(position - 1);
            String checkinDigest = cursor
                    .getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Leaderboard.LEADERBOARD_CHECKIN_DIGEST));
            String leaderboardName = cursor
                    .getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Leaderboard.LEADERBOARD_DISPLAY_NAME));
            startRegatta(leaderboardName, checkinDigest);
        }
    }

}
