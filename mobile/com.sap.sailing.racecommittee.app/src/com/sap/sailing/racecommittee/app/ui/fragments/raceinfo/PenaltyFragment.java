package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
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
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.ConfigurableStartModeFlagRacingProcedure;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;
import com.sap.sailing.racecommittee.app.domain.impl.CompetitorResultEditableImpl;
import com.sap.sailing.racecommittee.app.domain.impl.CompetitorResultWithIdImpl;
import com.sap.sailing.racecommittee.app.domain.impl.LeaderboardResult;
import com.sap.sailing.racecommittee.app.ui.adapters.CompetitorResultsList;
import com.sap.sailing.racecommittee.app.ui.adapters.PenaltyAdapter;
import com.sap.sailing.racecommittee.app.ui.adapters.PenaltyAdapter.ItemListener;
import com.sap.sailing.racecommittee.app.ui.adapters.PenaltyAdapter.OrderBy;
import com.sap.sailing.racecommittee.app.ui.adapters.StringArraySpinnerAdapter;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.layouts.CompetitorEditLayout;
import com.sap.sailing.racecommittee.app.ui.layouts.HeaderLayout;
import com.sap.sailing.racecommittee.app.ui.utils.CompetitorUtils;
import com.sap.sailing.racecommittee.app.ui.views.SearchView;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PenaltyFragment extends BaseFragment
        implements PopupMenu.OnMenuItemClickListener, ItemListener, SearchView.SearchTextWatcher {

    private static final int COMPETITOR_LOADER = 0;
    private static final int LEADERBOARD_ORDER_LOADER = 2;

    private CompetitorResultsList<CompetitorResultEditableImpl> mCompetitorResults =
            new CompetitorResultsList<CompetitorResultEditableImpl>(Collections.synchronizedList(new ArrayList<CompetitorResultEditableImpl>()));
    private CompetitorResultsList<CompetitorResult> mChangedData =
            new CompetitorResultsList<CompetitorResult>(Collections.synchronizedList(new ArrayList<CompetitorResult>()));
    private CompetitorResultsList<CompetitorResult> mConfirmedData =
            new CompetitorResultsList<CompetitorResult>(Collections.synchronizedList(new ArrayList<CompetitorResult>()));

    private View mButtonBar;
    private Spinner mPenaltyDropDown;
    private StringArraySpinnerAdapter mPenaltyAdapter;
    private Button mPublishButton;
    private PenaltyAdapter mAdapter;
    private TextView mEntryCount;

    private View mListButtonLayout;
    private ImageView mListButton;
    private HeaderLayout mHeader;
    private StateChangeListener mStateChangeListener;
    private SearchView mSearchView;

    public static PenaltyFragment newInstance() {
        Bundle args = new Bundle();
        PenaltyFragment fragment = new PenaltyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_penalty_fragment, container, false);

        mStateChangeListener = new StateChangeListener(this);

        mSearchView = ViewHelper.get(layout, R.id.competitor_search);
        if (mSearchView != null) {
            mSearchView.setSearchTextWatcher(this);
        }
        mHeader = ViewHelper.get(layout, R.id.header);
        mListButtonLayout = ViewHelper.get(layout, R.id.list_button_layout);
        if (mListButtonLayout != null) {
            mListButtonLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    sendUnconfirmed();
                    RaceFragment fragment = TrackingListFragment.newInstance(new Bundle(), 1);
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
        mListButton = ViewHelper.get(layout, R.id.list_button);
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
                    popupMenu.setOnMenuItemClickListener(PenaltyFragment.this);
                    popupMenu.show();
                    ThemeHelper.positioningPopupMenu(getActivity(), popupMenu, v);
                }
            });
        }
        mButtonBar = ViewHelper.get(layout, R.id.button_bar);
        mButtonBar.setVisibility(View.GONE);
        mPenaltyDropDown = ViewHelper.get(layout, R.id.spinner_penalty);
        if (mPenaltyDropDown != null) {
            mPenaltyAdapter = new StringArraySpinnerAdapter(getAllMaxPointsReasons());
            mPenaltyDropDown.setAdapter(mPenaltyAdapter);
            mPenaltyDropDown
                    .setOnItemSelectedListener(new StringArraySpinnerAdapter.SpinnerSelectedListener(mPenaltyAdapter));
        }
        View applyButton = ViewHelper.get(layout, R.id.button_apply);
        if (applyButton != null) {
            applyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setReason((String) mPenaltyDropDown.getSelectedItem());
                }
            });
        }
        View penaltyButton = ViewHelper.get(layout, R.id.button_penalty);
        if (penaltyButton != null) {
            penaltyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    final CharSequence[] maxPointsReasons = getAllMaxPointsReasons();
                    builder.setTitle(R.string.select_penalty_reason);
                    builder.setItems(maxPointsReasons, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int position) {
                            setReason(maxPointsReasons[position].toString());
                        }
                    });
                    builder.show();
                }
            });
        }
        mPublishButton = ViewHelper.get(layout, R.id.button_publish);
        if (mPublishButton != null) {
            mPublishButton.setOnClickListener(new View.OnClickListener() {
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
                    initLocalData();
                    setPublishButton();
                    Toast.makeText(getActivity(), R.string.publish_clicked, Toast.LENGTH_SHORT).show();
                }
            });
        }
        mEntryCount = ViewHelper.get(layout, R.id.competitor_entry_count);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAdapter = new PenaltyAdapter(getActivity(), this,
                getRace().getRaceGroup().canBoatsOfCompetitorsChangePerRace());
        RecyclerView recyclerView = ViewHelper.get(getView(), R.id.competitor_list);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(mAdapter);
        }

        if (mPenaltyDropDown != null) {
            int selection = mPenaltyAdapter.getPosition(MaxPointsReason.OCS.name());
            RacingProcedure procedure = getRaceState().getRacingProcedure();
            if (procedure instanceof ConfigurableStartModeFlagRacingProcedure) {
                ConfigurableStartModeFlagRacingProcedure racingProcedure = getRaceState().getTypedRacingProcedure();
                switch (racingProcedure.getStartModeFlag()) {
                    case BLACK:
                        selection = mPenaltyAdapter.getPosition(MaxPointsReason.BFD.name());
                        break;

                    case UNIFORM:
                        selection = mPenaltyAdapter.getPosition(MaxPointsReason.UFD.name());
                        break;

                    default:
                        // nothing
                        break;
                }
            }
            if (getRaceState().getStatus() == RaceLogRaceStatus.FINISHED) {
                selection = mPenaltyAdapter.getPosition(MaxPointsReason.DNF.name());
            }
            mPenaltyDropDown.setSelection(selection);
        }
        switch (getRaceState().getStatus()) {
            case FINISHED:
                if (mHeader != null) {
                    if (AppUtils.with(getActivity()).isPhone()) {
                        mHeader.setVisibility(View.VISIBLE);
                        mHeader.setHeaderOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sendIntent(AppConstants.ACTION_SHOW_SUMMARY_CONTENT);
                            }
                        });
                    } else {
                        mHeader.setVisibility(View.GONE);
                    }
                }
            case FINISHING:
                if (mListButton != null) {
                    mListButton.setImageDrawable(BitmapHelper.getAttrDrawable(requireContext(), R.attr.list_both_24dp));
                }
                if (mListButtonLayout != null) {
                    mListButtonLayout.setVisibility(View.VISIBLE);
                }
                break;

            default:
                if (mListButtonLayout != null) {
                    mListButtonLayout.setVisibility(View.GONE);
                }
        }
        if (mListButtonLayout != null) {
            mSearchView.isEditSmall(mListButtonLayout.getVisibility() == View.VISIBLE);
        }

        loadCompetitors();
        loadLeaderboardResult();
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

        if (getRace() != null) {
            getRaceState().removeChangedListener(mStateChangeListener);
        }

        //Check and send the diff of changed results
        CompetitorResults diff = getCompetitorResultsDiff(mChangedData);
        if (diff.size() > 0) {
            getRaceState().setFinishPositioningListChanged(MillisecondsTimePoint.now(), diff);
        }
    }

    private void setReason(String reason) {
        for (CompetitorResultEditableImpl item : mCompetitorResults) {
            if (item.isChecked()) {
                item.setMergeState(MergeState.OK);
                item.setMaxPointsReason(MaxPointsReason.valueOf(reason));
                item.setChecked(false);
            }
        }
        mAdapter.notifyDataSetChanged();
        setPublishButton();
    }

    private void initLocalData() {
        mChangedData.clear();
        if (getRaceState().getFinishPositioningList() != null) {
            for (CompetitorResult item : getRaceState().getFinishPositioningList()) {
                mChangedData.add(new CompetitorResultImpl(item));
            }
        }

        mConfirmedData.clear();
        if (getRaceState().getConfirmedFinishPositioningList().getCompetitorResults() != null) {
            for (CompetitorResult item : getRaceState().getConfirmedFinishPositioningList().getCompetitorResults()) {
                mConfirmedData.add(new CompetitorResultImpl(item));
            }
        }
    }

    private String[] getAllMaxPointsReasons() {
        List<String> result = new ArrayList<>();
        for (MaxPointsReason reason : MaxPointsReason.getLexicographicalValues()) {
            result.add(reason.name());
        }
        return result.toArray(new String[result.size()]);
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
        final Activity activity = getActivity();
        if (activity != null) {
            ReadonlyDataManager dataManager = OnlineDataManager.create(getActivity());
            final Loader<?> leaderboardResultLoader = getLoaderManager().initLoader(LEADERBOARD_ORDER_LOADER, null,
                    dataManager.createLeaderboardLoader(getRace(), new LoadClient<LeaderboardResult>() {
                        @Override
                        public void onLoadFailed(Exception reason) {
                        }

                        @Override
                        public void onLoadSucceeded(LeaderboardResult data, boolean isCached) {
                            onLoadLeaderboardResultSucceeded(data);
                        }
                    }));
            leaderboardResultLoader.forceLoad();
        }
    }

    private void onLoadCompetitorsSucceeded(Map<Competitor, Boat> data) {
        mCompetitorResults.clear();

        for (Map.Entry<Competitor, Boat> item : data.entrySet()) {
            Competitor competitor = item.getKey();
            Boat boat = item.getValue();

            CompetitorResult result = new CompetitorResultImpl(competitor.getId(), competitor.getName(),
                    competitor.getShortName(), boat.getName(), boat.getSailID(), 0, MaxPointsReason.NONE,
                    /* score */ null, /* finishingTime */ null, /* comment */ null,
                    MergeState.OK);
            mCompetitorResults.add(new CompetitorResultEditableImpl(result));
        }
        if (getRaceState().getFinishPositioningList() != null) { // mix with finish position
            // list
            for (CompetitorResult result : getRaceState().getFinishPositioningList()) {
                int pos = 0;
                for (int i = 0; i < mCompetitorResults.size(); i++) {
                    if (mCompetitorResults.get(i).getCompetitorId().equals(result.getCompetitorId())) {
                        mCompetitorResults.remove(i);
                        break;
                    }
                    pos++;
                }
                mCompetitorResults.add(pos, new CompetitorResultEditableImpl(result));
            }
        }
        mAdapter.setCompetitor(mCompetitorResults, data);
        setPublishButton();
    }

    private void onLoadLeaderboardResultSucceeded(LeaderboardResult data) {
        final String raceName = getRace().getName();
        List<Util.Pair<Long, String>> sortByRank = data.getResult(raceName);
        List<CompetitorResultEditableImpl> sortedList = new ArrayList<>();
        if (sortByRank != null) {
            for (Util.Pair<Long, String> item : sortByRank) {
                for (CompetitorResultEditableImpl competitor : mCompetitorResults) {
                    if (competitor.getCompetitorId().toString().equals(item.getB())) {
                        sortedList.add(competitor);
                        break;
                    }
                }
            }
        }
        mCompetitorResults.clear();
        mCompetitorResults.addAll(sortedList);
        mAdapter.setCompetitor(mCompetitorResults, null);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        OrderBy orderBy = OrderBy.SAILING_NUMBER;
        final int id = item.getItemId();
        if (id == R.id.by_short_name) {
            orderBy = OrderBy.COMPETITOR_SHORT_NAME;
        } else if (id == R.id.by_name) {
            orderBy = OrderBy.COMPETITOR_NAME;
        } else if (id == R.id.by_start) {
            orderBy = OrderBy.START_LINE;
        } else if (id == R.id.by_goal) {
            orderBy = OrderBy.FINISH_LINE;
            loadLeaderboardResult();
        }
        mAdapter.setOrderedBy(orderBy);
        return true;
    }

    @Override
    public void onCheckedChanged(CompetitorResultEditableImpl competitor, boolean isChecked) {
        setPublishButton();
    }

    private void mergeData(CompetitorResults results) {
        //Wrap results
        CompetitorResultsList<CompetitorResultEditableImpl> data = new CompetitorResultsList<CompetitorResultEditableImpl>(new ArrayList<CompetitorResultEditableImpl>());
        for (CompetitorResult result : results) {
            data.add(new CompetitorResultEditableImpl(result));
        }

        //Evaluate changes between current race log and changed events
        CompetitorResults localDiff = getCompetitorResultsDiff(mChangedData);
        CompetitorResults raceLogDiff = getCompetitorResultsDiff(mChangedData, data);

        Map<Serializable, String> changedCompetitor = new HashMap<>();
        //Iterate over the race log diff
        for (CompetitorResult result : raceLogDiff) {
            CompetitorResult initial = null;
            CompetitorResultEditableImpl local = null;
            CompetitorResultEditableImpl draft = null;

            for (CompetitorResult item : mChangedData) {
                if (result.getCompetitorId().equals(item.getCompetitorId())) {
                    initial = item;
                    break;
                }
            }
            for (CompetitorResultEditableImpl item : mCompetitorResults) {
                if (result.getCompetitorId().equals(item.getCompetitorId())) {
                    local = item;
                    break;
                }
            }
            //Search draft in local diff
            for (CompetitorResult item : localDiff) {
                if (result.getCompetitorId().equals(item.getCompetitorId())) {
                    draft = new CompetitorResultEditableImpl(local != null ? local : item);
                    break;
                }
            }

            if (local == null) {
                //Local item never exists; no need to compare properties
                mCompetitorResults.add(new CompetitorResultEditableImpl(result));
            } else if (draft == null) {
                //Only the remote item has changes
                local.setMaxPointsReason(result.getMaxPointsReason());
                local.setScore(result.getScore());
                local.setComment(result.getComment());
                local.setMergeState(MergeState.OK);
            } else {
                //Both items have the same changes; nothing to do
                if (areEqual(draft, result)) {
                    break;
                }

                //At least a warning
                MergeState state = MergeState.WARNING;

                //Compare penalties
                MaxPointsReason maxPointsReason = result.getMaxPointsReason();
                if (!ObjectsCompat.equals(local.getMaxPointsReason(), result.getMaxPointsReason())) {
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
                        if (local.getMaxPointsReason() != MaxPointsReason.NONE && result.getMaxPointsReason() != MaxPointsReason.NONE) {
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
                if (!ObjectsCompat.equals(local.getScore(), result.getScore())) {
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
                        if (local.getScore() != null && local.getScore() > 0 && result.getScore() != null && result.getScore() > 0) {
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

                //Compare comments
                String comment = result.getComment();
//                if (!Util.equalStringsWithEmptyIsNull(local.getComment(), result.getComment())) {
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
//                        if (!TextUtils.isEmpty(local.getComment()) && !TextUtils.isEmpty(result.getComment())) {
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

                local.setMaxPointsReason(maxPointsReason);
                local.setScore(score);
                local.setComment(comment);
                local.setMergeState(state);

                changedCompetitor.put(local.getCompetitorId(), CompetitorUtils.getDisplayName(local));
            }
        }
        mAdapter.notifyDataSetChanged();
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
        }
    }

    private void setMergeState(CompetitorResultEditableImpl item, MergeState newState) {
        switch (item.getMergeState()) {
            case ERROR:
                if (newState != MergeState.ERROR) {
                    break;
                }
                item.setMergeState(newState);
                break;

            case WARNING:
                if (newState != MergeState.WARNING && newState != MergeState.ERROR) {
                    break;
                }
                item.setMergeState(newState);
                break;

            case OK:
                item.setMergeState(newState);
                break;
        }
    }

    private void setPublishButton() {
        Set<Serializable> changed = new HashSet<>();
        boolean isChecked = false;
        boolean hasError = false;

        for (CompetitorResultEditableImpl item : mCompetitorResults) {
            if (item.isChecked()) {
                isChecked = true;
            }
            if (item.getMergeState() != MergeState.OK) {
                hasError = true;
            }
        }
        CompetitorResults diff = getCompetitorResultsDiff(mConfirmedData);
        for (CompetitorResult item : diff) {
            changed.add(item.getCompetitorId());
        }
        String text = getString(R.string.publish_button_empty);
        if (changed.size() != 0) {
            text = getString(R.string.publish_button_other, changed.size());
        }
        mPublishButton.setText(text);
        int warningSign = hasError ? R.drawable.ic_warning_red_small : 0;
        mPublishButton.setCompoundDrawablesWithIntrinsicBounds(warningSign, 0, 0, 0);
        mPublishButton.setEnabled(changed.size() != 0 && !hasError);
        mButtonBar.setVisibility((changed.size() != 0 || isChecked) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEditClicked(final CompetitorResultEditableImpl competitor) {
        Context context = requireContext();
        if (context instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
            if (actionBar != null) {
                context = actionBar.getThemedContext();
            }
        }
        competitor.setMergeState(MergeState.OK);
        CompetitorResultWithIdImpl item = new CompetitorResultWithIdImpl(0, competitor);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppTheme_AlertDialog);
        builder.setTitle(CompetitorUtils.getDisplayName(item));
        final CompetitorEditLayout layout = new CompetitorEditLayout(getActivity(), item,
                mCompetitorResults.getFirstRankZeroPosition() +
                        /*
                         * allow for setting rank as the new last in the list in case the competitor did not have a rank so far
                         */
                        (item.getOneBasedRank() == 0 ? 1 : 0),
                competitor.getMergeState() == MergeState.ERROR);
        builder.setView(layout);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CompetitorResult item = layout.getValue();
                // no need to compare rank as long as the dialog doesn't allow the user to edit it
                if (!Util.equalsWithNull(competitor.getMaxPointsReason(), item.getMaxPointsReason())) {
                    competitor.setMaxPointsReason(item.getMaxPointsReason());
                }
                if (!Util.equalsWithNull(competitor.getScore(), item.getScore())) {
                    competitor.setScore(item.getScore());
                }
                if (!Util.equalStringsWithEmptyIsNull(competitor.getComment(), item.getComment())) {
                    competitor.setComment(item.getComment());
                }
                mAdapter.notifyDataSetChanged();
                setPublishButton();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (AppUtils.with(getActivity()).isTablet()) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.competitor_dialog_width),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    @Nullable
    private Boat getBoat(Serializable competitorId) {
        Boat boat = null;
        for (Map.Entry<Competitor, Boat> entry : getRace().getCompetitorsAndBoats().entrySet()) {
            if (entry.getKey().getId().equals(competitorId)) {
                boat = entry.getValue();
                break;
            }
        }
        return boat;
    }

    private CompetitorResults getCompetitorResultsDiff(CompetitorResultsList<? extends CompetitorResult> results) {
        return getCompetitorResultsDiff(results, mCompetitorResults);
    }

    private CompetitorResultsImpl getCompetitorResultsDiff(CompetitorResultsList<? extends CompetitorResult> oldResults, CompetitorResultsList<CompetitorResultEditableImpl> newResults) {
        CompetitorResultsImpl result = new CompetitorResultsImpl();

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
                //Additionally reset penalty, score and finish time
                //TODO Comment?
                result.add(new CompetitorResultImpl(oldItem, 0, MaxPointsReason.NONE, null, null));
            }
        }

        // all new items
        for (CompetitorResultEditableImpl newItem : newResults) {
            boolean found = false;
            for (CompetitorResult oldItem : oldResults) {
                if (newItem.getCompetitorId().equals(oldItem.getCompetitorId())) {
                    found = true;
                }
            }
            if (!found && isValid(newItem)) {
                result.add(new CompetitorResultImpl(newItem));
            }
        }

        return result;
    }

    @Override
    public void onTextChanged(String text) {
        if (mAdapter != null) {
            mAdapter.setFilter(text);
            int count = mAdapter.getItemCount();
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

    /**
     * Checks a {@link CompetitorResult} if it is valid.
     * It is valid, if it has a penalty or a score.
     *
     * @param result The competitor result to check whether it's a valid result or not.
     * @return True if the result is valid.
     */
    private boolean isValid(CompetitorResult result) {
        return result.getOneBasedRank() > 0
                || result.getMaxPointsReason() != MaxPointsReason.NONE
                || (result.getScore() != null && result.getScore() > 0);
    }

    /**
     * Compares two {@link CompetitorResult}s.
     * They are equal, if they have the same rank, penalty, score and finish time.
     */
    private boolean areEqual(CompetitorResult result, CompetitorResult anotherResult) {
        return ObjectsCompat.equals(result.getMaxPointsReason(), anotherResult.getMaxPointsReason())
                && ObjectsCompat.equals(result.getScore(), anotherResult.getScore());
    }

    private static class StateChangeListener extends BaseRaceStateChangedListener {

        private WeakReference<PenaltyFragment> mReference;

        StateChangeListener(PenaltyFragment fragment) {
            mReference = new WeakReference<>(fragment);
        }

        @Override
        public void onFinishingPositionsChanged(ReadonlyRaceState state) {
            super.onFinishingPositionsChanged(state);

            PenaltyFragment fragment = mReference.get();
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
