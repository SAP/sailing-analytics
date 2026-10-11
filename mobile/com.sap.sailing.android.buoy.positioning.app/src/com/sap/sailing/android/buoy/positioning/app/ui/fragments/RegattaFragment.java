package com.sap.sailing.android.buoy.positioning.app.ui.fragments;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.adapter.MarkAdapter;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract;
import com.sap.sailing.android.buoy.positioning.app.ui.activities.PositioningActivity;
import com.sap.sailing.android.buoy.positioning.app.ui.activities.RegattaActivity;
import com.sap.sailing.android.shared.ui.fragments.BaseFragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class RegattaFragment extends BaseFragment implements LoaderCallbacks<Cursor> {
    private static final String TAG = RegattaFragment.class.getName();
    private static final int MARKER_LOADER = 1;
    private MarkAdapter adapter;
    private IntentReceiver mReceiver;

    public static RegattaFragment newInstance() {
        Bundle args = new Bundle();
        RegattaFragment fragment = new RegattaFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mReceiver = new IntentReceiver();

        View view = inflater.inflate(R.layout.fragment_buoy_postion_overview, container, false);
        adapter = new MarkAdapter(getActivity(), R.layout.mark_listview_row, null, 0);
        ListView markListView = (ListView) view.findViewById(R.id.listMarks);
        markListView.setEmptyView(view.findViewById(R.id.no_marks));
        markListView.setAdapter(adapter);
        markListView.setOnItemClickListener(new ItemClickListener());

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.database_changed));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);

        loadData();
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }

    public void loadData() {
        getLoaderManager().restartLoader(MARKER_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        String checkinDigest = ((RegattaActivity) getActivity()).getCheckinDigest();
        switch (loaderId) {
        case MARKER_LOADER:
            return new CursorLoader(getActivity(), AnalyticsContract.MarksLeaderBoardsJoined.CONTENT_URI, null,
                    AnalyticsContract.Mark.MARK_CHECKIN_DIGEST + " = ?", new String[] { checkinDigest }, null);
        default:
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
        case MARKER_LOADER:
            adapter.changeCursor(cursor);
            break;
        default:
            break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case MARKER_LOADER:
            adapter.changeCursor(null);
            break;
        default:
            break;
        }
    }

    private class ItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = (Cursor) adapter.getItem(position);
            String markerID = cursor.getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Mark.MARK_ID));
            String checkinDigest = cursor.getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Mark.MARK_CHECKIN_DIGEST));
            Intent intent = new Intent(getActivity(), PositioningActivity.class);
            intent.putExtra(getString(R.string.mark_id), markerID);
            intent.putExtra(getString(R.string.checkin_digest), checkinDigest);
            getActivity().startActivity(intent);

        }
    }

    // Broadcast receiver to update ui on data changed
    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Adapter will be notified");
            String action = intent.getAction();
            if (action.equals(getString(R.string.database_changed))) {
                loadData();
            }
        }
    }
}
