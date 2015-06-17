package com.sap.sailing.racecommittee.app.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.RaceApplication;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.impl.RaceGroupSeriesFleet;
import com.sap.sailing.racecommittee.app.ui.activities.RacingActivity;
import com.sap.sailing.racecommittee.app.ui.activities.SessionActivity;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.ManagedRaceListAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.RaceListDataType;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.RaceListDataTypeHeader;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.RaceListDataTypeRace;
import com.sap.sailing.racecommittee.app.ui.comparators.NaturalNamedComparator;
import com.sap.sailing.racecommittee.app.ui.comparators.RaceListDataTypeComparator;
import com.sap.sailing.racecommittee.app.ui.comparators.RegattaSeriesFleetComparator;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.ProtestTimeDialogFragment;
import com.sap.sailing.racecommittee.app.utils.*;
import com.sap.sse.common.TimePoint;

import java.io.Serializable;
import java.util.*;

public class RaceListFragment
    extends LoggableFragment
    implements OnItemClickListener, OnItemSelectedListener, TickListener, OnScrollListener {

    private final static String TAG = RaceListFragment.class.getName();
    private final static String LAYOUT = "layout";
    private ManagedRaceListAdapter mAdapter;
    private RaceListCallbacks mCallbacks;
    private Button mCurrentRacesButton;
    private Button mAllRacesButton;
    private TextView mCourse;
    private TextView mData;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private FilterMode mFilterMode;
    private ListView mListView;
    private HashMap<Serializable, ManagedRace> mManagedRacesById;
    private TreeMap<RaceGroupSeriesFleet, List<ManagedRace>> mRacesByGroup;
    private ManagedRace mSelectedRace;
    private IntentReceiver mReceiver;
    private boolean mUpdateList = true;
    private ArrayList<RaceListDataType> mViewItems;
    private BaseRaceStateChangedListener stateListener = new BaseRaceStateChangedListener() {
        @Override
        public void onStartTimeChanged(ReadonlyRaceState state) {
            update(state);
        }

        @Override
        public void onStatusChanged(ReadonlyRaceState state) {
            update(state);
        }

        public void update(ReadonlyRaceState state) {
            dataChanged(state);
            filterChanged();
        }
    };

    public RaceListFragment() {
        mFilterMode = FilterMode.ACTIVE;
        mSelectedRace = null;
        mManagedRacesById = new HashMap<>();
        mRacesByGroup = new TreeMap<>(new RegattaSeriesFleetComparator());
        mViewItems = new ArrayList<>();
    }

    public static RaceListFragment newInstance(int layout) {
        RaceListFragment fragment = new RaceListFragment();
        Bundle args = new Bundle();
        args.putInt(LAYOUT, layout);
        fragment.setArguments(args);
        return fragment;
    }

    public static void showProtest(Context context, ManagedRace race) {
        Intent intent = new Intent(AppConstants.INTENT_ACTION_SHOW_PROTEST);
        String extra = race.getRaceGroup().getName();
        if (!race.getSeries().getName().equals(AppConstants.DEFAULT)) {
            extra += " - " + race.getSeries().getName();
        }
        if (!race.getFleet().getName().equals(AppConstants.DEFAULT)) {
            extra += " - " + race.getFleet().getName();
        }
        intent.putExtra(AppConstants.INTENT_ACTION_EXTRA, extra);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void dataChanged(ReadonlyRaceState changedState) {
        List<RaceListDataType> adapterItems = mAdapter.getItems();
        for (RaceListDataType adapterItem : adapterItems) {
            if (adapterItem instanceof RaceListDataTypeRace) {
                RaceListDataTypeRace raceView = (RaceListDataTypeRace) adapterItem;
                ManagedRace race = raceView.getRace();
                if (changedState != null && race.getState().equals(changedState)) {
                    boolean allowUpdateIndicator = !raceView.getRace().equals(mSelectedRace);
                    raceView.onStatusChanged(changedState.getStatus(), allowUpdateIndicator);
                }
            }
        }
        mAdapter.sort(new RaceListDataTypeComparator());
        mAdapter.notifyDataSetChanged();
    }

    private void filterChanged() {
        mAdapter.sort(new RaceListDataTypeComparator());
        mAdapter.getFilter().filterByMode(getFilterMode());
        mAdapter.notifyDataSetChanged();

        if (mCurrentRacesButton != null && mAllRacesButton != null) {
            int colorGrey = ThemeHelper.getColor(getActivity(), R.attr.sap_light_gray);
            int colorOrange = ThemeHelper.getColor(getActivity(), R.attr.sap_yellow_1);
            mCurrentRacesButton.setTextColor(colorGrey);
            mAllRacesButton.setTextColor(colorGrey);
            BitmapHelper.setBackground(mCurrentRacesButton, null);
            BitmapHelper.setBackground(mAllRacesButton, null);

            int id;
            if (AppConstants.LIGHT_THEME.equals(AppPreferences.on(getActivity()).getTheme())) {
                id = R.drawable.nav_drawer_tab_button_light;
            } else {
                id = R.drawable.nav_drawer_tab_button_dark;
            }
            Drawable drawable = BitmapHelper.getDrawable(getActivity(), id);
            switch (getFilterMode()) {
            case ALL:
                mAllRacesButton.setTextColor(colorOrange);
                BitmapHelper.setBackground(mAllRacesButton, drawable);
                break;

            default:
                mCurrentRacesButton.setTextColor(colorOrange);
                BitmapHelper.setBackground(mCurrentRacesButton, drawable);
                break;
            }
        }
    }

    public FilterMode getFilterMode() {
        return mFilterMode;
    }

    public void setFilterMode(FilterMode filterMode) {
        mFilterMode = filterMode;
        filterChanged();
    }

    @Override
    public void notifyTick(TimePoint now) {
        if (mAdapter != null && mAdapter.getCount() >= 0 && mUpdateList) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RaceListDataTypeRace.initializeTemplates(this);

        if (mListView != null) {
            mAdapter = new ManagedRaceListAdapter(getActivity(), mViewItems);
            mListView.setAdapter(mAdapter);
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            mListView.setOnItemClickListener(this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (RaceListCallbacks) activity;
        } catch (ClassCastException ex) {
            ExLog.ex(getActivity(), TAG, ex);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int layout = R.layout.race_list_vertical;
        if (getArguments() != null && getArguments().getInt(LAYOUT) != 0) {
            layout = getArguments().getInt(LAYOUT);
        }
        View view = inflater.inflate(layout, container, false);

        mReceiver = new IntentReceiver();
        mListView = (ListView) view.findViewById(R.id.listView);
        mListView.setOnScrollListener(this);

        mCurrentRacesButton = (Button) view.findViewById(R.id.races_current);
        if (mCurrentRacesButton != null) {
            mCurrentRacesButton.setTypeface(Typeface.DEFAULT_BOLD);
            mCurrentRacesButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    setFilterMode(FilterMode.ACTIVE);
                    filterChanged();
                }
            });
        }

        mAllRacesButton = (Button) view.findViewById(R.id.races_all);
        if (mAllRacesButton != null) {
            mAllRacesButton.setTypeface(Typeface.DEFAULT_BOLD);
            mAllRacesButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    setFilterMode(FilterMode.ALL);
                    filterChanged();
                }
            });
        }

        mCourse = (TextView) view.findViewById(R.id.regatta_course);
        mData = (TextView) view.findViewById(R.id.regatta_data);

        ImageView imageView = (ImageView) view.findViewById(R.id.nav_button);
        if (imageView != null) {
            imageView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ((RacingActivity) getActivity()).logoutSession();
                }
            });
        }

        view.setClickable(true);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mCallbacks = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ExLog.i(getActivity(), TAG, "Touched " + mAdapter.getItem(position).toString());

        mDrawerLayout.closeDrawers();
        mAdapter.setSelectedRace(mAdapter.getItem(position));
        mCallbacks.onRaceListItemSelected(mAdapter.getItem(position));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setFilterMode(Arrays.asList(FilterMode.values()).get(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPause() {
        super.onPause();

        TickSingleton.INSTANCE.unregisterListener(this);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        TickSingleton.INSTANCE.registerListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.INTENT_ACTION_SHOW_PROTEST);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
        case SCROLL_STATE_FLING:
        case SCROLL_STATE_TOUCH_SCROLL:
            mUpdateList = false;
            break;

        default:
            mUpdateList = true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        unregisterOnAllRaces();
        registerOnAllRaces();
        for (ManagedRace race : mManagedRacesById.values()) {
            stateListener.onStatusChanged(race.getState());
        }
    }

    @Override
    public void onStop() {
        unregisterOnAllRaces();
        super.onStop();
    }

    private void registerOnAllRaces() {
        for (ManagedRace managedRace : mManagedRacesById.values()) {
            managedRace.getState().addChangedListener(stateListener);
        }
    }

    public void setUp(DrawerLayout drawerLayout, String course, String author) {
        mDrawerLayout = drawerLayout;
        mDrawerLayout.setStatusBarBackgroundColor(ThemeHelper.getColor(getActivity(), R.attr.colorPrimaryDark));
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, (Toolbar) getActivity()
            .findViewById(R.id.toolbar), R.string.nav_drawer_open, R.string.nav_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

                mUpdateList = false;
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                mUpdateList = true;
            }
        };

        mDrawerLayout.post(new Runnable() {

            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (mCourse != null) {
            SpannableString text = new SpannableString(course);
            StyleSpan spanBold = new StyleSpan(Typeface.BOLD);
            text.setSpan(spanBold, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mCourse.setText(text);
        }

        if (mData != null) {
            mData.setText(StringHelper.on(getActivity()).getAuthor(author));
        }
    }

    private void initializeRacesByGroup() {
        mRacesByGroup.clear();
        for (ManagedRace race : mManagedRacesById.values()) {
            RaceGroupSeriesFleet container = new RaceGroupSeriesFleet(race);

            if (!mRacesByGroup.containsKey(container)) {
                mRacesByGroup.put(container, new LinkedList<ManagedRace>());
            }
            mRacesByGroup.get(container).add(race);
        }
    }

    private void initializeViewElements() {
        // 1. Group races by <boat class, series, fleet>
        initializeRacesByGroup();

        // 2. Remove previous view items
        mViewItems.clear();

        // 3. Create view elements from tree
        for (RaceGroupSeriesFleet key : mRacesByGroup.navigableKeySet()) {
            // ... add the header view...
            mViewItems.add(new RaceListDataTypeHeader(key));

            List<ManagedRace> races = mRacesByGroup.get(key);
            Collections.sort(races, new NaturalNamedComparator());
            for (ManagedRace race : races) {
                // ... and add the race view!
                mViewItems.add(new RaceListDataTypeRace(race));
            }
        }
    }

    public void setupOn(Collection<ManagedRace> races) {
        ExLog.i(getActivity(), TAG, String.format("Setting up %s with %d races.", this.getClass().getSimpleName(), races.size()));

        unregisterOnAllRaces();

        mManagedRacesById.clear();

        for (ManagedRace managedRace : races) {
            mManagedRacesById.put(managedRace.getId(), managedRace);
        }
        registerOnAllRaces();

        initializeViewElements();
        // prepare views and do initial filtering
        filterChanged();
        mAdapter.sort(new RaceListDataTypeComparator());
        mAdapter.notifyDataSetChanged();
    }

    public void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    private void unregisterOnAllRaces() {
        for (ManagedRace managedRace : mManagedRacesById.values()) {
            managedRace.getState().removeChangedListener(stateListener);
        }
    }

    public enum FilterMode {
        ACTIVE(R.string.race_list_filter_show_active), ALL(R.string.race_list_filter_show_all);

        private String displayName;

        FilterMode(int resId) {
            this.displayName = RaceApplication.getStringContext().getString(resId);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }


    public interface RaceListCallbacks {
        void onRaceListItemSelected(RaceListDataType selectedItem);
    }


    private class IntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AppConstants.INTENT_ACTION_SHOW_PROTEST.equals(intent.getAction())) {
                String raceGroupName = intent.getExtras().getString(AppConstants.INTENT_ACTION_EXTRA);
                if (raceGroupName != null) {
                    showProtestTimeDialog(raceGroupName);
                } else {
                    ExLog.e(getActivity(), TAG, "INTENT_ACTION_SHOW_PROTEST does not carry an INTENT_ACTION_EXTRA with the race group name!");
                }
            }
        }
    }

    private void showProtestTimeDialog(String raceGroupName) {
        // Find the race group for which the
        for (RaceGroupSeriesFleet raceGroupSeriesFleet : mRacesByGroup.keySet()) {
            Boolean matchingRaceGroup = raceGroupName.equals(raceGroupSeriesFleet.getDisplayName());
            if (matchingRaceGroup) {
                List<ManagedRace> races = mRacesByGroup.get(raceGroupSeriesFleet);
                if (!isRaceListDirty(races)) {
                    ProtestTimeDialogFragment fragment = ProtestTimeDialogFragment.newInstance(races);
                    fragment.show(getFragmentManager(), null);
                }
            }
        }
    }

    private boolean isRaceListDirty(List<ManagedRace> races) {
        ReadonlyDataManager manager = DataManager.create(getActivity());
        for (ManagedRace race : races) {
            // check for data consistency if race is still in data store and not only in fragment
            if (manager.getDataStore().getRace(race.getId()) == null) {
                SessionActivity sessionActivity = (SessionActivity) getActivity();
                sessionActivity.forceLogout();
                return true;
            }
        }
        return false;
    }


}
