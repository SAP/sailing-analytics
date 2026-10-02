package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.content.Loader;
import androidx.core.util.ObjectsCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.domain.impl.CompetitorResultWithIdImpl;
import com.sap.sailing.racecommittee.app.domain.impl.LeaderboardResult;
import com.sap.sailing.racecommittee.app.ui.adapters.CompetitorAndBoatAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.CompetitorResultsList;
import com.sap.sailing.racecommittee.app.ui.adapters.FinishListAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist.HolderAwareOnDragListener;
import com.sap.sailing.racecommittee.app.ui.adapters.dragandswipelist.ItemTouchHelperCallback;
import com.sap.sailing.racecommittee.app.ui.comparators.CompetitorGoalPassingComparator;
import com.sap.sailing.racecommittee.app.ui.comparators.CompetitorNameComparator;
import com.sap.sailing.racecommittee.app.ui.comparators.CompetitorSailIdComparator;
import com.sap.sailing.racecommittee.app.ui.comparators.CompetitorShortNameComparator;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.layouts.CompetitorEditLayout;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sailing.racecommittee.app.ui.utils.CompetitorUtils;
import com.sap.sailing.racecommittee.app.ui.views.SearchView;
import com.sap.sailing.racecommittee.app.utils.StringHelper;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrackingListFragment extends BaseFragment
        implements CompetitorAndBoatAdapter.CompetitorClick, View.OnClickListener, AdapterView.OnItemSelectedListener,
        PopupMenu.OnMenuItemClickListener, SearchView.SearchTextWatcher, HolderAwareOnDragListener {

    private static final int COMPETITOR_LOADER = 0;
    private static final int LEADERBOARD_ORDER_LOADER = 2;

    private static final int SORT_SHORT_NAME = 0;
    private static final int SORT_SAIL_NUMBER = 1;
    private static final int SORT_NAME = 2;
    private static final int SORT_GOAL = 3;
    private static final int SORT_START = 4;

    private RecyclerView mFinishView;
    private FinishListAdapter mFinishedAdapter;
    private CompetitorResultsList<CompetitorResultWithIdImpl> mFinishedData =
            new CompetitorResultsList<>(Collections.synchronizedList(Collections.synchronizedList(new ArrayList<CompetitorResultWithIdImpl>())));
    private CompetitorResultsList<CompetitorResult> mChangedData =
            new CompetitorResultsList<CompetitorResult>(Collections.synchronizedList(new ArrayList<CompetitorResult>()));
    private CompetitorResultsList<CompetitorResult> mConfirmedData =
            new CompetitorResultsList<CompetitorResult>(Collections.synchronizedList(new ArrayList<CompetitorResult>()));
    private ItemTouchHelper mItemTouchHelper;

    private Button mConfirm;
    private TextView mEntryCount;

    private CompetitorAndBoatAdapter mCompetitorAdapter;
    private List<Map.Entry<Competitor, Boat>> mCompetitorData;
    private List<Map.Entry<Competitor, Boat>> mFilteredCompetitorData;
    private int mId = 0;
    private HeaderLayout mHeader;

    private Comparator<Map.Entry<Competitor, Boat>> mComparator;
    private List<Comparator<Map.Entry<Competitor, Boat>>> mComparators;
    private String mFilter;
    private View mTools;

    private StateChangeListener mStateChangeListener;

    public TrackingListFragment() {
        mCompetitorData = Collections.synchronizedList(new ArrayList<Map.Entry<Competitor, Boat>>());
        mFilteredCompetitorData = Collections.synchronizedList(new ArrayList<Map.Entry<Competitor, Boat>>());
    }

    public static TrackingListFragment newInstance(Bundle args, int startMode) {
        TrackingListFragment fragment = new TrackingListFragment();
        args.putInt(START_MODE, startMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_tracking_list, container, false);

        mStateChangeListener = new StateChangeListener(this);

        mHeader = ViewHelper.get(layout, R.id.header);
        mDots = new ArrayList<>();
        mPanels = new ArrayList<>();
        ImageView dot;
        dot = ViewHelper.get(layout, R.id.dot_1);
        if (dot != null) {
            mDots.add(dot);
        }
        dot = ViewHelper.get(layout, R.id.dot_2);
        if (dot != null) {
            mDots.add(dot);
        }
        mTools = ViewHelper.get(layout, R.id.tools_layout);
        ImageView listButton = ViewHelper.get(layout, R.id.list_button);
        if (listButton != null) {
            listButton.setImageDrawable(BitmapHelper.getAttrDrawable(getActivity(), R.attr.list_single_24dp));
            listButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RaceFragment fragment = PenaltyFragment.newInstance();
                    int viewId = R.id.race_content;
                    switch (getRaceState().getStatus()) {
                        case FINISHED:
                            viewId = getFrameId(getActivity(), R.id.finished_edit, R.id.finished_content, true);
                            break;

                        default:
                            break;
                    }
                    replaceFragment(fragment, viewId);
                }
            });
        }
        ImageView btnPrev = ViewHelper.get(layout, R.id.nav_prev);
        if (btnPrev != null) {
            btnPrev.setOnClickListener(this);
        }
        ImageView btnNext = ViewHelper.get(layout, R.id.nav_next);
        if (btnNext != null) {
            btnNext.setOnClickListener(this);
        }
        if (mHeader != null) {
            mHeader.setHeaderOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Check if finished or not
                    boolean finished = getRaceState().getFinishedTime() != null;
                    if (finished) {
                        sendIntent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
                    } else {
                        Intent intent = new Intent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                        intent.putExtra(AppConstants.ACTION_EXTRA_FORCED, true);
                        sendIntent(intent);
                    }
                }
            });
        }
        if (getArguments().getInt(START_MODE, 0) != 0) {
            if (mHeader != null) {
                mHeader.setVisibility(View.GONE);
            }
        }
        mEntryCount = ViewHelper.get(layout, R.id.competitor_entry_count);
        SearchView searchView = ViewHelper.get(layout, R.id.competitor_search);
        if (searchView != null) {
            searchView.setSearchTextWatcher(this);
            searchView.isEditSmall(true);
        }
        View sortByButton = ViewHelper.get(layout, R.id.competitor_sort);
        if (sortByButton != null) {
            sortByButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        popupMenu = new PopupMenu(getActivity(), v, Gravity.RIGHT);
                    } else {
                        popupMenu = new PopupMenu(getActivity(), v);
                    }
                    popupMenu.inflate(R.menu.sort_menu);
                    popupMenu.setOnMenuItemClickListener(TrackingListFragment.this);
                    popupMenu.show();
                    ThemeHelper.positioningPopupMenu(getActivity(), popupMenu, v);
                }
            });
        }

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mComparators = new ArrayList<>();
        mComparators.add(SORT_SHORT_NAME, new CompetitorShortNameComparator());
        mComparators.add(SORT_SAIL_NUMBER, new CompetitorSailIdComparator());
        mComparators.add(SORT_NAME, new CompetitorNameComparator());
        mComparators.add(SORT_GOAL, new CompetitorGoalPassingComparator());
        mComparator = mComparators.get(SORT_SAIL_NUMBER);

        Util.addAll(getRace().getCompetitorsAndBoats().entrySet(), mCompetitorData);
        mFilteredCompetitorData.addAll(mCompetitorData);
        sortCompetitors();

        initializeFinishList();

        deleteCompetitorsFromCompetitorList();

        loadCompetitors();
        loadLeaderboardResult();

        if (getView() != null) {
            initCompetitorsView();
            initFinishersView();
            initConfirmationButton();
            if (getArguments().getInt(START_MODE, 0) == 0) {
                onClick(ViewHelper.get(getView(), R.id.nav_next));
            } else {
                viewPanel(MOVE_NONE);
            }
        }
    }

    private void initCompetitorsView() {
        RecyclerView competitorView = (RecyclerView) getView().findViewById(R.id.list_positioning_all);
        if (competitorView != null) {
            mCompetitorAdapter = new CompetitorAndBoatAdapter(getActivity(), mFilteredCompetitorData,
                    getRace().getRaceGroup().canBoatsOfCompetitorsChangePerRace());
            mCompetitorAdapter.setListener(this);
            competitorView.setLayoutManager(new LinearLayoutManager(getActivity()));
            competitorView.setAdapter(mCompetitorAdapter);
            mPanels.add(competitorView);
        }
    }

    private void initFinishersView() {
        mFinishView = (RecyclerView) getView().findViewById(R.id.list_positioning_chosen);
        if (mFinishView != null) {

            mFinishedAdapter = new FinishListAdapter(getActivity(), mFinishedData,
                    getRace().getRaceGroup().canBoatsOfCompetitorsChangePerRace(), this);
            mFinishedAdapter.setDragListener(this);

            mFinishView.setAdapter(mFinishedAdapter);
            mFinishView.setLayoutManager(new LinearLayoutManager(getActivity()));

            ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mFinishedAdapter);
            mItemTouchHelper = new ItemTouchHelper(callback);
            mItemTouchHelper.attachToRecyclerView(mFinishView);

            mPanels.add(mFinishView);
        }
    }

    private void initConfirmationButton() {
        mConfirm = (Button) getView().findViewById(R.id.confirm);
        if (mConfirm != null) {
            mConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePoint now = MillisecondsTimePoint.now();
                    CompetitorResults diffChanged = getCompetitorResultsDiff(mChangedData);
                    if (diffChanged.size() > 0) {
                        getRaceState().setFinishPositioningListChanged(now, diffChanged);
                    }
                    CompetitorResults diffConfirmed = getCompetitorResultsDiff(mConfirmedData);
                    if (diffConfirmed.size() > 0) {
                        getRaceState().setFinishPositioningConfirmed(now, diffConfirmed);
                    }
                    initializeFinishList();
                    initLocalData();
                    Toast.makeText(getActivity(), R.string.publish_clicked, Toast.LENGTH_SHORT).show();
                    //Check if finished or not
                    boolean finished = getRaceState().getFinishedTime() != null;
                    if (finished) {
                        sendIntent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
                    } else {
                        Intent intent = new Intent(AppConstants.ACTION_SHOW_MAIN_CONTENT);
                        intent.putExtra(AppConstants.ACTION_EXTRA_FORCED, true);
                        sendIntent(intent);
                    }
                }
            });
        }
    }

    private int getFirstRankZeroPosition() {
        return mFinishedAdapter.getFirstRankZeroPosition();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getRaceState() != null) {
            getRaceState().addChangedListener(mStateChangeListener);
        }

        initLocalData();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (getRaceState() != null) {
            getRaceState().removeChangedListener(mStateChangeListener);
        }

        //Check and send the diff of changed results
        CompetitorResultsImpl diff = (CompetitorResultsImpl) getCompetitorResultsDiff(mChangedData);
        if (!diff.isEmpty()) {
            getRaceState().setFinishPositioningListChanged(MillisecondsTimePoint.now(), diff);
        }
    }

    @Override
    public void onDestroy() {
        if (mFinishView != null) {
            mFinishView.setItemAnimator(null);
            mFinishView.setAdapter(null);
            mFinishView = null;
        }

        if (mFinishedAdapter != null) {
            mFinishedAdapter = null;
        }

        super.onDestroy();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.by_short_name) {
            mComparator = mComparators.get(SORT_SHORT_NAME);
        } else if (id == R.id.by_name) {
            mComparator = mComparators.get(SORT_NAME);
        } else if (id == R.id.by_start) {
            mComparator = mComparators.get(SORT_START);
        } else if (id == R.id.by_goal) {
            mComparator = mComparators.get(SORT_GOAL);
        } else if (id == R.id.by_boat) {
            mComparator = mComparators.get(SORT_SAIL_NUMBER);
        } else {
            mComparator = mComparators.get(SORT_SAIL_NUMBER);
        }
        sortCompetitors();
        mCompetitorAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewholder) {
        mItemTouchHelper.startDrag(viewholder);
    }

    private void setPublishButton() {
        Set<Integer> positions = new HashSet<>();
        boolean multiplePositions = false;
        Set<Serializable> errors = new HashSet<>();

        for (CompetitorResult item : mFinishedData) {
            // check for multiple positions
            if (item.getOneBasedRank() != 0 && positions.contains(item.getOneBasedRank())) {
                multiplePositions = true;
            }
            positions.add(item.getOneBasedRank());

            // check for merge errors
            if (item.getMergeState() == MergeState.ERROR) {
                errors.add(item.getCompetitorId());
            }
        }

        boolean error = multiplePositions || errors.size() > 0;
        int warningSign = error ? R.drawable.ic_warning_red_small : 0;
        mConfirm.setEnabled(!error);
        mConfirm.setCompoundDrawablesWithIntrinsicBounds(warningSign, 0, 0, 0);
    }

    private void sendUnconfirmed() {
        getRaceState().setFinishPositioningListChanged(MillisecondsTimePoint.now(),
                getCompetitorResultsDiff(mConfirmedData));
    }

    private void loadCompetitors() {
        // invalidate all competitors of this race
        ReadonlyDataManager dataManager = OnlineDataManager.create(getActivity());
        SharedDomainFactory domainFactory = dataManager.getDataStore().getDomainFactory();
        for (Competitor competitor : getRace().getCompetitors()) {
            domainFactory.getCompetitorAndBoatStore().allowCompetitorResetToDefaults(competitor);
        }

        final Loader<?> competitorLoader = getLoaderManager().initLoader(COMPETITOR_LOADER, null,
                dataManager.createCompetitorsLoader(getRace(), new LoadClient<Map<Competitor, Boat>>() {

                    @Override
                    public void onLoadFailed(Exception reason) {
                        Toast.makeText(getActivity(), getString(R.string.competitor_load_error, reason.toString()),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onLoadSucceeded(Map<Competitor, Boat> data, boolean isCached) {
                        if (isAdded() && !isCached) {
                            onLoadCompetitorsSucceeded(data);
                        }
                    }
                }));

        // Force load to get non-cached remote competitors...
        competitorLoader.forceLoad();
    }

    private void loadLeaderboardResult() {
        ReadonlyDataManager dataManager = OnlineDataManager.create(getActivity());

        final Loader<?> leaderboardResultLoader = getLoaderManager().initLoader(LEADERBOARD_ORDER_LOADER, null,
                dataManager.createLeaderboardLoader(getRace(), new LoadClient<LeaderboardResult>() {
                    @Override
                    public void onLoadFailed(Exception reason) {
                    }

                    @Override
                    public void onLoadSucceeded(LeaderboardResult data, boolean isCached) {
                        List<Util.Pair<Long, String>> sortByRank = data.getResult(getRace().getName());
                        if (isAdded() && sortByRank != null) {
                            CompetitorGoalPassingComparator comparator = (CompetitorGoalPassingComparator) mComparators
                                    .get(SORT_GOAL);
                            comparator.updateWith(sortByRank);
                            sortCompetitors();
                            mCompetitorAdapter.notifyDataSetChanged();
                        }
                    }
                }));

        leaderboardResultLoader.forceLoad();
    }

    protected void onLoadCompetitorsSucceeded(Map<Competitor, Boat> data) {
        Collection<Competitor> competitors = data.keySet();
        mCompetitorData.clear();
        Util.addAll(data.entrySet(), mCompetitorData);
        mFilteredCompetitorData.clear();
        Util.addAll(data.entrySet(), mFilteredCompetitorData);
        sortCompetitors();
        deleteCompetitorsFromFinishedList(competitors);
        deleteCompetitorsFromCompetitorList();
        mCompetitorAdapter.notifyDataSetChanged();
    }

    private void deleteCompetitorsFromFinishedList(Collection<Competitor> validCompetitors) {
        List<CompetitorResultWithIdImpl> toBeDeleted = new ArrayList<>();
        for (CompetitorResultWithIdImpl item : mFinishedData) {
            if (!validCompetitors.contains(getCompetitorStore().getExistingCompetitorById(item.getCompetitorId()))) {
                toBeDeleted.add(item);
                continue;
            }
            if (!isValid(item)) {
                toBeDeleted.add(item);
                continue;
            }
            item.setBoat(getBoat(item.getCompetitorId()));
        }
        mFinishedData.removeAll(toBeDeleted);
        setPublishButton();
        mFinishedAdapter.notifyDataSetChanged();
    }

    private void deleteCompetitorsFromCompetitorList() {
        for (CompetitorResultWithIdImpl item : mFinishedData) {
            Competitor competitor = getCompetitorStore().getExistingCompetitorById(item.getCompetitorId());
            for (Map.Entry<Competitor, Boat> entry : getRace().getCompetitorsAndBoats().entrySet()) {
                if (entry.getKey().equals(competitor)) {
                    mCompetitorData.remove(entry);
                    mFilteredCompetitorData.remove(entry);
                }
            }
        }
    }

    private CompetitorAndBoatStore getCompetitorStore() {
        return DataManager.create(getActivity()).getDataStore().getDomainFactory().getCompetitorAndBoatStore();
    }

    private void initializeFinishList() {
        CompetitorResultsList<CompetitorResultWithIdImpl> positioning = new CompetitorResultsList<>(
                Collections.synchronizedList(new ArrayList<CompetitorResultWithIdImpl>()));
        if (getRaceState() != null && getRaceState().getFinishPositioningList() != null) {
            for (CompetitorResult result : getRaceState().getFinishPositioningList()) {
                if (isValid(result)) {
                    positioning.add(new CompetitorResultWithIdImpl(mId, result));
                    mId++;
                }
            }
        }
        Collections.sort(positioning,
                new DefaultCompetitorResultComparator(/* lowPoint TODO where to get this from? */ true));
        mFinishedData.clear();
        mFinishedData.addAll(positioning);
    }

    private void initLocalData() {
        mChangedData.clear();
        if (getRaceState().getFinishPositioningList() != null) {
            for (CompetitorResult item : getRaceState().getFinishPositioningList()) {
                mChangedData.add(new CompetitorResultWithIdImpl(-1, item));
            }
        }

        mConfirmedData.clear();
        if (getRaceState().getConfirmedFinishPositioningList().getCompetitorResults() != null) {
            for (CompetitorResult item : getRaceState().getConfirmedFinishPositioningList().getCompetitorResults()) {
                mConfirmedData.add(new CompetitorResultWithIdImpl(-1, item));
            }
        }
    }

    @Nullable
    public Boat getBoat(Serializable competitorId) {
        Boat boat = null;
        for (Map.Entry<Competitor, Boat> entry : getRace().getCompetitorsAndBoats().entrySet()) {
            if (entry.getKey().getId().equals(competitorId)) {
                boat = entry.getValue();
                break;
            }
        }
        return boat;
    }

    @Override
    public void onCompetitorClick(Competitor competitor) {
        moveCompetitorToFinishList(competitor);
        for (Map.Entry<Competitor, Boat> entry : mFilteredCompetitorData) {
            if (entry.getKey().equals(competitor)) {
                removeCompetitorFromList(entry);
                break;
            }
        }
    }

    public void onItemEdit(final CompetitorResultWithIdImpl item) {
        Context context = getContext();
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
            if (actionBar != null) {
                context = actionBar.getThemedContext();
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_AlertDialog);
        builder.setTitle(CompetitorUtils.getDisplayName(item));
        final CompetitorEditLayout layout = new CompetitorEditLayout(getActivity(), getRace().getState().getFinishingTime(), item,
                mFinishedAdapter.getFirstRankZeroPosition() +
                        /* allow for setting rank as the new last in the list in case the competitor did not have a rank so far */
                        (item.getOneBasedRank() == 0 ? 1 : 0), false, item.getMergeState() == MergeState.ERROR);
        builder.setView(layout);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CompetitorResultWithIdImpl newItem = layout.getValue();
                updateItem(item, newItem, true);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (AppUtils.with(getContext()).isTablet()) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(getContext().getResources().getDimensionPixelSize(R.dimen.competitor_dialog_width),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void moveCompetitorToFinishList(Competitor competitor) {
        int pos = mFinishedAdapter.getFirstRankZeroPosition();
        // FIXME mFinishedData.size()+1 also counts penalized competitors before which the competitor is to be inserted! I just wonder how the position shown in the app seems correct...
        int greatestOneBasedRankSoFar = 0;
        for (final CompetitorResultWithIdImpl result : mFinishedData) {
            if (result.getOneBasedRank() > greatestOneBasedRankSoFar) {
                greatestOneBasedRankSoFar = result.getOneBasedRank();
            }
        }
        CompetitorResultWithIdImpl theCompetitor = new CompetitorResultWithIdImpl(mId, competitor.getId(),
                competitor.getName(), competitor.getShortName(), getBoat(competitor.getId()),
                greatestOneBasedRankSoFar + 1, MaxPointsReason.NONE, /* score */ null, /* finishingTime */ null,
                /* comment */ null, MergeState.OK);
        mFinishedData.add(pos, theCompetitor);
        mId++;
        setPublishButton();
        mFinishedAdapter.notifyItemInserted(pos);
        if (mDots.size() > 0) {
            Toast.makeText(getActivity(), getString(R.string.added_to_result_list, CompetitorUtils.getDisplayName(theCompetitor), pos + 1), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeCompetitorFromList(Map.Entry<Competitor, Boat> entry) {
        mCompetitorData.remove(entry);
        int pos = mFilteredCompetitorData.indexOf(entry);
        mFilteredCompetitorData.remove(pos);
        mCompetitorAdapter.notifyItemRemoved(pos);
    }

    private void addNewCompetitorToCompetitorList(Map.Entry<Competitor, Boat> entry) {
        mCompetitorData.add(entry);
        mFilteredCompetitorData.add(entry);
        sortCompetitors();
        mCompetitorAdapter.notifyItemInserted(mFilteredCompetitorData.indexOf(entry));
    }

    /**
     * The {@code fromPosition} identifies the position of the item to be moved in the underlying {@link #mFinishedData
     * data model}; it shall be moved to {@code toPosition}. The item will be moved in {@link #mFinishedData} as
     * requested, and all ranks of items affected will be {@link #adjustRanks(int, int) adjusted}. The item moved will
     * only have its rank corrected if it moved to a position before the {@link #getFirstRankZeroPosition()}. Four cases
     * are possible:
     * <ol>
     * <li>Rank set from non-0 to 0: move item to the end of the list, into the "penalized section"; decrement ranks
     * greater than old rank</li>
     * <li>Rank set from 0 to non-0: move from penalized to ranked section; increment ranks greater than or equal to old
     * rank</li>
     * <li>Rank changed from non-0 to other non-0 value: adjust all ranks between and including old and new list
     * position</li>
     * </ol>
     *
     * @param fromPosition the position (zero-based index) where the item currently is in {@link #mFinishedData}
     * @param toPosition   the position (zero-based index <em>after</em> removal of the item from its {@code fromPosition}) where
     *                     to {@link List#add(int, Object)} the item again in {@link #mFinishedData}
     */
    public void onItemMove(int fromPosition, int toPosition, boolean adjustRanks) {
        final int firstPositionChanged = Math.min(fromPosition, toPosition);
        final int lastPositionChanged = Math.max(fromPosition, toPosition);
        if (adjustRanks) {
            adjustRanks(firstPositionChanged, lastPositionChanged);
        }
        mFinishedAdapter.notifyItemRangeChanged(firstPositionChanged, lastPositionChanged - firstPositionChanged + 1);
        setPublishButton();
    }

    public void onItemRemove(int position, CompetitorResultWithIdImpl item, boolean adjustRanks) {
        if (adjustRanks) {
            adjustRanks(position, getFirstRankZeroPosition() - 1);
        }
        mFinishedAdapter.notifyItemRangeChanged(position, getFirstRankZeroPosition() - position);
        setPublishButton();
        for (Map.Entry<Competitor, Boat> entry : getRace().getCompetitorsAndBoats().entrySet()) {
            if (entry.getKey().getId().equals(item.getCompetitorId())) {
                addNewCompetitorToCompetitorList(entry);
                break;
            }
        }
    }

    /**
     * In {@link #mFinishedData}, starting at index {@code fromPosition}, updates all ranks so they equal the position
     * in the list plus one; stop before reaching the entry at list index {@code toPosition} or the
     * {@link #getFirstRankZeroPosition() first penalty position}.
     *
     * @param fromPosition inclusive start index into {@link #mFinishedData} where to start updating the ranks
     * @param toPosition   inclusive end index into {@link #mFinishedData} where to stop updating the ranks; must not be -1
     */
    private void adjustRanks(int fromPosition, int toPosition) {
        final int end = Math.min(toPosition, getFirstRankZeroPosition() - 1);
        for (int i = fromPosition; i <= end; i++) {
            CompetitorResultWithIdImpl result = mFinishedData.get(i);
            final int rank = result.getOneBasedRank();
            if (rank > 0) {
                final int newOneBasedRank = i + 1;
                mFinishedData.set(i,
                        cloneCompetitorResultAndAdjustRank(result, newOneBasedRank));
            }
        }
    }

    private CompetitorResultWithIdImpl cloneCompetitorResultAndAdjustRank(CompetitorResultWithIdImpl competitor,
                                                                          final int newOneBasedRank) {
        return new CompetitorResultWithIdImpl(competitor.getId(), competitor.getCompetitorId(), competitor.getName(),
                competitor.getShortName(), competitor.getBoatName(), competitor.getBoatSailId(), newOneBasedRank, competitor.getMaxPointsReason(),
                competitor.getScore(), competitor.getFinishingTime(), competitor.getComment(), competitor.getMergeState());
    }

    public void onLongClick(final CompetitorResultWithIdImpl item) {
        final CharSequence[] maxPointsReasons = getAllMaxPointsReasons();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.select_penalty_reason);
        builder.setItems(maxPointsReasons, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int position) {
                setMaxPointsReasonForItem(item, maxPointsReasons[position]);
                setPublishButton();
                mFinishedAdapter.notifyItemChanged(mFinishedData.indexOf(item));
            }
        });
        builder.show();
    }

    public void addItem(CompetitorResultWithIdImpl item) {
        //Determine the position for the new result
        int index = Collections.binarySearch(mFinishedData, item, new DefaultCompetitorResultComparator(true));
        if (index < 0) {
            index = -(index) - 1;
        }
        //Add the new result
        mFinishedData.add(index, item);
        mFinishedAdapter.notifyItemInserted(index);
        //Remove the competitor
        for (Map.Entry<Competitor, Boat> entry : mFilteredCompetitorData) {
            if (entry.getKey().getId().equals(item.getCompetitorId())) {
                removeCompetitorFromList(entry);
                break;
            }
        }
    }

    /**
     * The following cases are possible:
     * <ol>
     * <li>Rank set from non-0 to 0 and MaxPointsReason is not NONE: move item to the end of the list, into the
     * "penalized section"; decrement ranks greater than old rank</li>
     * <li>Rank set from non-0 to 0 and MaxPointsReason is NONE: remove item from finish list</li>
     * <li>Rank set from 0 to non-0: move from penalized to ranked section; increment ranks greater than or equal to old
     * rank</li>
     * <li>Rank changed from non-0 to other non-0 value: adjust all ranks between and including old and new list
     * position</li>
     * </ol>
     */
    public void updateItem(final CompetitorResultWithIdImpl item, CompetitorResultWithIdImpl newItem, boolean adjustRanks) {
        int index = -1;
        for (int i = 0; i < mFinishedData.size(); i++) {
            if (mFinishedData.get(i).getCompetitorId().equals(item.getCompetitorId())) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            if (isValid(newItem)) {
                if (item.getOneBasedRank() != newItem.getOneBasedRank()) {
                    int newIndex;
                    if (newItem.getOneBasedRank() == 0) {
                        //Temporarily remove the item
                        mFinishedData.remove(index);
                        //Determine the new position
                        newIndex = Collections.binarySearch(mFinishedData, newItem, new DefaultCompetitorResultComparator(true));
                        if (newIndex < 0) {
                            newIndex = -(newIndex) - 1;
                        }
                        //Add the new item at the old position
                        mFinishedData.add(index, newItem);
                    } else {
                        newIndex = newItem.getOneBasedRank() - 1;
                        //Update the item
                        mFinishedData.set(index, newItem);
                    }
                    //Move the item
                    mFinishedAdapter.onItemMove(index, newIndex, adjustRanks);
                } else {
                    //Update the item
                    mFinishedData.set(index, newItem);
                    //Move the item
                    mFinishedAdapter.notifyItemChanged(index);
                }
            } else {
                mFinishedAdapter.onItemRemove(index, adjustRanks);
            }
        } else {
            if (isValid(newItem)) {
                addItem(newItem);
            }
        }
        setPublishButton();
    }

    private CharSequence[] getAllMaxPointsReasons() {
        List<CharSequence> result = new ArrayList<>();
        for (MaxPointsReason reason : MaxPointsReason.getLexicographicalValues()) {
            result.add(reason.name());
        }
        return result.toArray(new CharSequence[result.size()]);
    }

    protected void setMaxPointsReasonForItem(CompetitorResultWithIdImpl item, CharSequence maxPointsReasonName) {
        MaxPointsReason maxPointsReason = MaxPointsReason.valueOf(maxPointsReasonName.toString());
        CompetitorResultWithIdImpl newItem = new CompetitorResultWithIdImpl(item.getId(), item, maxPointsReason);
        updateItem(item, newItem, true);
        getRaceState().setFinishPositioningListChanged(MillisecondsTimePoint.now(), getCompetitorResults());
    }

    private CompetitorResults getCompetitorResults() {
        CompetitorResults result = new CompetitorResultsImpl();
        for (CompetitorResult item : mFinishedData) {
            result.add(new CompetitorResultImpl(item));
        }
        return result;
    }

    private CompetitorResults getCompetitorResultsDiff(CompetitorResultsList<? extends CompetitorResult> results) {
        return getCompetitorResultsDiff(results, mFinishedData);
    }

    private CompetitorResults getCompetitorResultsDiff(CompetitorResultsList<? extends CompetitorResult> oldResults, CompetitorResultsList<? extends CompetitorResult> newResults) {
        CompetitorResults result = new CompetitorResultsImpl();

        // all changed items
        for (CompetitorResult oldItem : oldResults) {
            boolean found = false;
            for (CompetitorResult newItem : newResults) {
                if (oldItem.getCompetitorId().equals(newItem.getCompetitorId())) {
                    if (!areEqual(oldItem, newItem)) {
                        result.add(new CompetitorResultImpl(newItem));
                    }
                    found = true;
                    break;
                }
            }
            if (!found && isValid(oldItem)) {
                //Additionally reset penalty, score, finish time and merge state
                //TODO Comment?
                result.add(new CompetitorResultImpl(oldItem, 0, MaxPointsReason.NONE, null, null, MergeState.OK));
            }
        }

        // all new items
        for (CompetitorResult newItem : newResults) {
            boolean found = false;
            for (CompetitorResult oldItem : oldResults) {
                if (newItem.getCompetitorId().equals(oldItem.getCompetitorId())) {
                    found = true;
                    break;
                }
            }
            if (!found && isValid(newItem)) {
                result.add(new CompetitorResultImpl(newItem));
            }
        }

        return result;
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            final int id = v.getId();
            if (id == R.id.nav_prev) {
                viewPanel(MOVE_DOWN);
            } else if (id == R.id.nav_next) {
                viewPanel(MOVE_UP);
            }
        }
        if (mHeader != null) {
            switch (mActivePage) {
                case 0:
                    mHeader.setHeaderText(R.string.tracking_list_01);
                    break;

                default:
                    mHeader.setHeaderText(R.string.tracking_list_02);
            }
        }
        if (mTools != null) {
            mTools.setVisibility(mActivePage == 0 ? View.VISIBLE : View.GONE);
        }
        setPublishButton();
    }

    private void sortCompetitors() {
        Collections.sort(mFilteredCompetitorData, mComparator);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mComparator = mComparators.get(position);
        sortCompetitors();
        mCompetitorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mComparator = mComparators.get(0);
        sortCompetitors();
        mCompetitorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTextChanged(String text) {
        if (mCompetitorAdapter != null) {
            mFilter = text;
            filterData();
            mCompetitorAdapter.notifyDataSetChanged();
            int count = mCompetitorAdapter.getItemCount();
            if (mEntryCount != null && AppUtils.with(getActivity()).isTablet()) {
                if (!TextUtils.isEmpty(text)) {
                    mEntryCount.setText(getString(R.string.competitor_count, count));
                    mEntryCount.setVisibility(View.VISIBLE);
                } else {
                    mEntryCount.setVisibility(View.GONE);
                }
            }
        }
    }

    private void filterData() {
        mFilteredCompetitorData.clear();
        if (mCompetitorData != null) {
            if (TextUtils.isEmpty(mFilter)) {
                mFilteredCompetitorData.addAll(mCompetitorData);
            } else {
                for (Map.Entry<Competitor, Boat> entry : mCompetitorData) {
                    String name = "";
                    if (entry.getKey().getShortInfo() != null) {
                        name += entry.getKey().getShortInfo() + " - ";
                    }
                    name += entry.getKey().getName();
                    if (StringHelper.on(getActivity()).containsIgnoreCase(name, mFilter)) {
                        mFilteredCompetitorData.add(entry);
                    }
                }
            }
        }
    }

    private void mergeData(CompetitorResults results) {
        //Wrap results
        CompetitorResultsList<CompetitorResultWithIdImpl> data = new CompetitorResultsList<CompetitorResultWithIdImpl>(new ArrayList<CompetitorResultWithIdImpl>());
        for (CompetitorResult result : results) {
            data.add(new CompetitorResultWithIdImpl(-1, result));
        }

        //Evaluate changes between current race log and changed events
        CompetitorResults localDiff = getCompetitorResultsDiff(mChangedData);
        CompetitorResults raceLogDiff = getCompetitorResultsDiff(mChangedData, data);

        Map<Serializable, String> changedCompetitor = new HashMap<>();
        //Iterate over the race log diff
        for (CompetitorResult result : raceLogDiff) {
            CompetitorResult initial = null;
            CompetitorResultWithIdImpl local = null;
            CompetitorResultWithIdImpl draft = null;
            CompetitorResultWithIdImpl mergedResult;
            for (CompetitorResult item : mChangedData) {
                if (result.getCompetitorId().equals(item.getCompetitorId())) {
                    initial = item;
                    break;
                }
            }
            for (CompetitorResultWithIdImpl item : mFinishedData) {
                if (result.getCompetitorId().equals(item.getCompetitorId())) {
                    local = item;
                    break;
                }
            }
            //Search draft in local diff
            for (CompetitorResult item : localDiff) {
                if (result.getCompetitorId().equals(item.getCompetitorId())) {
                    draft = local != null ? local : new CompetitorResultWithIdImpl(mId++, item);
                    break;
                }
            }

            if (local == null && (draft == null || !isValid(draft))) {
                //Local item never exists or has been removed; no need to compare properties
                if (isValid(result)) {
                    addItem(new CompetitorResultWithIdImpl(mId++, result));
                }
            } else if (draft == null) {
                mergedResult = new CompetitorResultWithIdImpl(local.getId(), result.getCompetitorId(), result.getName(), result.getShortName(),
                        result.getBoatName(), result.getBoatSailId(),
                        result.getOneBasedRank(), result.getMaxPointsReason(), result.getScore(), result.getFinishingTime(), result.getComment(),
                        MergeState.OK);
                updateChangedItem(changedCompetitor, local, mergedResult);
            } else {
                //Both items have the same changes; nothing to do and skip it
                if (areEqual(draft, result)) {
                    continue;
                }

                //At least a warning
                MergeState state = MergeState.WARNING;

                //Compare ranks
                int oneBasedRank = result.getOneBasedRank();
                if (!ObjectsCompat.equals(draft.getOneBasedRank(), result.getOneBasedRank())) {
                    if (initial != null) {
                        if (!ObjectsCompat.equals(initial.getOneBasedRank(), draft.getOneBasedRank())) {
                            if (!ObjectsCompat.equals(initial.getOneBasedRank(), result.getOneBasedRank())) {
                                //Both ranks are different to the initial one
                                state = MergeState.ERROR;
                            } else {
                                //Use the local rank because it has not been changed remotely
                                oneBasedRank = draft.getOneBasedRank();
                            }
                        }
                    } else {
                        //Both items are new and have a different rank
                        state = MergeState.ERROR;
                    }
                }

                //Compare penalties
                MaxPointsReason maxPointsReason = result.getMaxPointsReason();
                if (!ObjectsCompat.equals(draft.getMaxPointsReason(), result.getMaxPointsReason())) {
                    if (initial != null) {
                        if (!ObjectsCompat.equals(initial.getMaxPointsReason(), draft.getMaxPointsReason())) {
                            if (!ObjectsCompat.equals(initial.getMaxPointsReason(), result.getMaxPointsReason())) {
                                //Both penalties are different to the initial one
                                state = MergeState.ERROR;
                            } else {
                                //Use the local penalty because it has not been changed remotely
                                maxPointsReason = draft.getMaxPointsReason();
                            }
                        }
                    } else {
                        //Both items are new
                        if (draft.getMaxPointsReason() != MaxPointsReason.NONE && result.getMaxPointsReason() != MaxPointsReason.NONE) {
                            //Both items have a valid penalty
                            state = MergeState.ERROR;
                        } else {
                            if (result.getMaxPointsReason() == MaxPointsReason.NONE) {
                                //Use the local penalty because it has not been changed remotely
                                maxPointsReason = draft.getMaxPointsReason();
                            }
                        }
                    }
                }

                //Compare scores
                Double score = result.getScore();
                if (!ObjectsCompat.equals(draft.getScore(), result.getScore())) {
                    if (initial != null) {
                        if (!ObjectsCompat.equals(initial.getScore(), draft.getScore())) {
                            if (!ObjectsCompat.equals(initial.getScore(), result.getScore())) {
                                //Both scores are different to the initial one
                                state = MergeState.ERROR;
                            } else {
                                //Use the local score because it has not been changed remotely
                                score = draft.getScore();
                            }
                        }
                    } else {
                        //Both items are new
                        if (draft.getScore() != null && draft.getScore() > 0 && result.getScore() != null && result.getScore() > 0) {
                            //Both items have a valid score
                            state = MergeState.ERROR;
                        } else {
                            if (result.getScore() == null || result.getScore() == 0) {
                                //Use the local score because it has not been changed remotely
                                score = draft.getScore();
                            }
                        }
                    }
                }

                //Compare finish time
                TimePoint finishingTime = result.getFinishingTime();
                if (!ObjectsCompat.equals(draft.getFinishingTime(), result.getFinishingTime())) {
                    if (initial != null) {
                        if (!ObjectsCompat.equals(initial.getFinishingTime(), draft.getFinishingTime())) {
                            if (!ObjectsCompat.equals(initial.getFinishingTime(), result.getFinishingTime())) {
                                //Both finish times are different to the initial one
                                state = MergeState.ERROR;
                            } else {
                                //Use the local finish time because it has not been changed remotely
                                finishingTime = draft.getFinishingTime();
                            }
                        }
                    } else {
                        //Both items are new
                        if (draft.getFinishingTime() != null && result.getFinishingTime() != null) {
                            //Both items have a valid penalty
                            state = MergeState.ERROR;
                        } else {
                            if (result.getFinishingTime() == null) {
                                //Use the local finish time because it has not been changed remotely
                                finishingTime = draft.getFinishingTime();
                            }
                        }
                    }
                }

                //Compare comments
                String comment = result.getComment();
//                if (!Util.equalStringsWithEmptyIsNull(draft.getComment(), result.getComment())) {
//                    if (initial != null) {
//                        if (!Util.equalStringsWithEmptyIsNull(initial.getComment(), draft.getComment())) {
//                            if (!Util.equalStringsWithEmptyIsNull(initial.getComment(), result.getComment())) {
//                                //Both comments are different to the initial one
//                                state = MergeState.ERROR;
//                            } else {
//                                //Use the local comment because it has not been changed remotely
//                                comment = draft.getComment();
//                            }
//                        }
//                    } else {
//                        //Both items are new
//                        if (!TextUtils.isEmpty(draft.getComment()) && !TextUtils.isEmpty(result.getComment())) {
//                            //Both items have a valid comment
//                            state = MergeState.ERROR;
//                        } else {
//                            if (TextUtils.isEmpty(result.getComment())) {
//                                //Use the local comment because it has not been changed remotely
//                                comment = draft.getComment();
//                            }
//                        }
//                    }
//                }

                mergedResult = new CompetitorResultWithIdImpl(draft.getId(), draft.getCompetitorId(), draft.getName(), draft.getShortName(),
                        draft.getBoatName(), draft.getBoatSailId(),
                        oneBasedRank, maxPointsReason, score, finishingTime, comment,
                        getMergeState(draft, state));
                updateChangedItem(changedCompetitor, draft, mergedResult);
            }
        }
        setPublishButton();
        if (changedCompetitor.size() > 0) { // show message to user
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.refresh_title);
            StringBuilder string = new StringBuilder(1024);
            for (String competitor : changedCompetitor.values()) {
                string.append(competitor);
                string.append("\n");
            }
            builder.setMessage(getString(R.string.refresh_message, string.toString()));
            builder.setPositiveButton(R.string.refresh_positive, null);
            builder.show();
            // goal passing might have changed too, so update them
            loadLeaderboardResult();
        }
    }

    private void updateChangedItem(Map<Serializable, String> changedCompetitor,
                                   CompetitorResultWithIdImpl item, CompetitorResultWithIdImpl newItem) {
        updateItem(item, newItem, false);
        if (newItem.getMergeState() != MergeState.OK) {
            changedCompetitor.put(newItem.getCompetitorId(), CompetitorUtils.getDisplayName(newItem));
        }
    }

    /**
     * Checks a {@link CompetitorResult} if it is valid.
     * It is valid, if it has a rank, any penalty, a score or a finish time.
     *
     * @param result The competitor result to check whether it's a valid result or not.
     * @return True if the result is valid.
     */
    private boolean isValid(CompetitorResult result) {
        return result.getOneBasedRank() > 0
                || result.getMaxPointsReason() != MaxPointsReason.NONE
                || (result.getScore() != null && result.getScore() > 0)
                || result.getFinishingTime() != null
                || result.getMergeState() != MergeState.OK;
    }

    /**
     * Compares two {@link CompetitorResult}s.
     * They are equal, if they have the same rank, penalty, score and finish time.
     */
    private boolean areEqual(CompetitorResult result, CompetitorResult anotherResult) {
        return result.getOneBasedRank() == anotherResult.getOneBasedRank()
                && ObjectsCompat.equals(result.getMaxPointsReason(), anotherResult.getMaxPointsReason())
                && ObjectsCompat.equals(result.getScore(), anotherResult.getScore())
                && ObjectsCompat.equals(result.getFinishingTime(), anotherResult.getFinishingTime());
    }

    private MergeState getMergeState(CompetitorResult item, MergeState newState) {
        MergeState state = item.getMergeState();
        switch (item.getMergeState()) {
            case ERROR:
                if (newState != MergeState.ERROR) {
                    break;
                }
                state = newState;
                break;

            case WARNING:
                if (newState != MergeState.WARNING && newState != MergeState.ERROR) {
                    break;
                }
                state = newState;
                break;

            case OK:
                state = newState;
                break;
        }
        return state;
    }

    private static class StateChangeListener extends BaseRaceStateChangedListener {

        private WeakReference<TrackingListFragment> mReference;

        StateChangeListener(TrackingListFragment fragment) {
            mReference = new WeakReference<>(fragment);
        }

        @Override
        public void onFinishingPositionsChanged(ReadonlyRaceState state) {
            super.onFinishingPositionsChanged(state);

            TrackingListFragment fragment = mReference.get();
            if (fragment != null) {
                CompetitorResults results = state.getFinishPositioningList();
                //Merge changes into draft
                fragment.mergeData(results);
                //Refresh changed and confirmed results because they serve as diff basis
                fragment.initLocalData();
            }
        }
    }
}
