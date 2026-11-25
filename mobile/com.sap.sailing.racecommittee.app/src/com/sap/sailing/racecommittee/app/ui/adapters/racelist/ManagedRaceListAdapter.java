package com.sap.sailing.racecommittee.app.ui.adapters.racelist;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.base.SeriesBase;
import com.sap.sailing.domain.base.racegroup.CurrentRaceComparator;
import com.sap.sailing.domain.base.racegroup.RaceGroupSeries;
import com.sap.sailing.domain.base.racegroup.RaceGroupSeriesComparator;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.ui.adapters.racelist.RaceFilter.FilterSubscriber;
import com.sap.sailing.racecommittee.app.ui.utils.FlagsResources;
import com.sap.sailing.racecommittee.app.ui.views.FlagTimeView;
import com.sap.sailing.racecommittee.app.ui.views.RaceTimeView;
import com.sap.sailing.racecommittee.app.utils.RaceHelper;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.Util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ManagedRaceListAdapter extends ArrayAdapter<RaceListDataType> implements FilterSubscriber {

    private final static String TAG = ManagedRaceListAdapter.class.getName();
    private final Object mLockObject = new Object();
    private final RaceFilter mFilter;
    private LayoutInflater mInflater;
    private Resources mResources;
    private final List<RaceListDataType> mShownViewItems;
    private final Map<ManagedRace, RaceListDataTypeRace> viewItemsRaces;
    private final Map<SeriesBase, RaceListDataTypeHeader> viewItemsSeriesHeaders;
    private RaceListDataType mSelectedRace;
    private int flag_size;
    private DecimalFormat factor_format;
    private final Set<ManagedRace> mAllRaces;

    public ManagedRaceListAdapter(Context context, Set<ManagedRace> allRaces) {
        super(context, 0);
        mAllRaces = allRaces;
        mShownViewItems = new ArrayList<>();
        this.viewItemsRaces = new HashMap<>();
        this.viewItemsSeriesHeaders = new HashMap<>();
        createViewItemsForRacesAndSeries();
        mFilter = new RaceFilter(allRaces, this);
        mInflater = LayoutInflater.from(getContext());
        mResources = getContext().getResources();
        flag_size = getContext().getResources().getInteger(R.integer.flag_size);
        factor_format = new DecimalFormat(context.getString(R.string.race_factor_format));
    }

    private void createViewItemsForRacesAndSeries() {
        viewItemsRaces.clear();
        viewItemsSeriesHeaders.clear();
        for (final ManagedRace race : mAllRaces) {
            viewItemsRaces.put(race, new RaceListDataTypeRace(race, mInflater));
            final SeriesBase series = race.getSeries();
            RaceState state = race.getState();
            CompetitorResults draft = state.getFinishPositioningList();
            CompetitorResults confirmed = state.getConfirmedFinishPositioningList().getCompetitorResults();
            boolean draftHasConflict = draft != null && draft.hasConflicts();
            boolean confirmedHasConflict = confirmed != null && confirmed.hasConflicts();
            boolean hasConflict = draftHasConflict || confirmedHasConflict;
            if (!viewItemsSeriesHeaders.containsKey(series)) {
                viewItemsSeriesHeaders.put(series,
                        new RaceListDataTypeHeader(new RaceGroupSeries(race), mInflater, hasConflict));
            } else {
                if (hasConflict) {
                    final RaceListDataTypeHeader header = viewItemsSeriesHeaders.get(series);
                    if (header != null) {
                        header.setHasConflict(true);
                    }
                }
            }
        }
    }

    @Override
    public int getCount() {
        synchronized (mLockObject) {
            return mShownViewItems != null ? mShownViewItems.size() : 0;
        }
    }

    @NonNull
    @Override
    public RaceFilter getFilter() {
        return mFilter;
    }

    @Override
    public RaceListDataType getItem(int position) {
        synchronized (mLockObject) {
            return mShownViewItems != null ? mShownViewItems.get(position) : null;
        }
    }

    public List<RaceListDataType> getItems() {
        return mShownViewItems;
    }

    @Override
    public int getItemViewType(int position) {
        return (getItem(position) instanceof RaceListDataTypeHeader ? ViewType.HEADER.index
                : getItem(position) instanceof RaceListDataTypeRace ? ViewType.RACE.index : -1);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final RaceListDataType raceListElement = getItem(position);

        ViewHolder holder;

        int type = getItemViewType(position);

        if (convertView == null) {
            convertView = raceListElement == null ? mInflater.inflate(R.layout.race_list_area_item, parent, false) : raceListElement.getView(parent);
            holder = new ViewHolder();
            holder.findViews(convertView);
            convertView.setTag(R.id.race_list_holder, holder);
        } else {
            holder = (ViewHolder) convertView.getTag(R.id.race_list_holder);
        }
        resetValues(holder);

        if (type == ViewType.HEADER.index) {
            final RaceListDataTypeHeader header = (RaceListDataTypeHeader) raceListElement;
            if (header != null) {
                String regatta = header.getRaceGroup().getDisplayName();
                String series = RaceHelper.getSeriesName(header.getSeries(), "");

                if (!(raceListElement).equals(convertView.getTag(R.id.race_list_header))) {
                    if (TextUtils.isEmpty(regatta)) {
                        regatta = header.getRaceGroup().getName();
                    }
                    holder.boat_class.setText(regatta);
                    holder.fleet_series.setText(series);
                    if (holder.fleet_series.getText().length() == 0) {
                        holder.fleet_series.setVisibility(View.GONE);
                    } else {
                        holder.fleet_series.setVisibility(View.VISIBLE);
                    }
                    holder.protest_image
                            .setImageDrawable(FlagsResources.getFlagDrawable(getContext(), Flags.BRAVO.name(), flag_size));
                    holder.protest_layout.setOnClickListener(v -> {
                        Intent intent = new Intent(AppConstants.ACTION_SHOW_PROTEST);
                        // TODO don't use toString() to convey semantics and perform check; bug 3617
                        intent.putExtra(AppConstants.EXTRA_DEFAULT,
                                new RaceGroupSeries(header.getRaceGroup(), header.getSeries()).getDisplayName());
                        BroadcastManager.getInstance(getContext()).addIntent(intent);
                    });
                    holder.protest_warning_image.setVisibility(header.hasConflict() ? View.VISIBLE : View.GONE);
                    convertView.setTag(R.id.race_list_header, raceListElement);
                }
            }
        } else if (type == ViewType.RACE.index) {
            final RaceListDataTypeRace race = (RaceListDataTypeRace) raceListElement;
            if (race != null) {
                if (mSelectedRace != null && mSelectedRace.equals(race)) {
                    holder.itemView.setSelected(true);
                    if (race.isUpdateIndicatorVisible()) {
                        race.setUpdateIndicatorVisible(false);
                    }
                } else {
                    holder.itemView.setSelected(false);
                    if (race.isUpdateIndicatorVisible()) {
                        holder.update_badge.setVisibility(View.VISIBLE);
                    }
                }
                holder.race_name.setText(RaceHelper.getReverseRaceFleetName(race.getRace()));
                RaceState state = race.getRace().getState();
                holder.race_flag_new.setRaceState(state);
                holder.time.setRaceState(state);
                CompetitorResults draft = state.getFinishPositioningList();
                CompetitorResults confirmed = state.getConfirmedFinishPositioningList().getCompetitorResults();
                holder.warning_sign.setVisibility(
                        ((draft != null && draft.hasConflicts()) || confirmed != null && confirmed.hasConflicts())
                                ? View.VISIBLE
                                : View.GONE);
                if (state.getStartTime() != null) {
                    int startRes = R.string.race_started;
                    if (state.getFinishedTime() == null) {
                        startRes = R.string.race_start;
                    }
                    String startTime = mResources.getString(startRes, TimeUtils.formatTime(state.getStartTime(), false));
                    holder.race_started.setText(startTime);
                }
                if (state.getFinishedTime() != null) {
                    holder.race_flag_new.setVisibility(View.GONE);
                    holder.time.setVisibility(View.GONE);
                    holder.race_finished.setVisibility(View.VISIBLE);
                    holder.race_finished.setText(mResources.getString(R.string.race_finished,
                            TimeUtils.formatTime(state.getFinishedTime(), false)));
                }
                setDependingText(holder, race);
                if (state.getStartTime() == null && state.getFinishedTime() == null) {
                    if (race.getRace().getStatus() == RaceLogRaceStatus.PRESCHEDULED) {
                        holder.panel_right.setVisibility(View.GONE);
                        holder.race_scheduled.setVisibility(View.GONE);
                        holder.race_unscheduled.setVisibility(View.GONE);
                    } else {
                        holder.race_scheduled.setVisibility(View.GONE);
                        holder.race_unscheduled.setVisibility(View.VISIBLE);
                    }
                }

                Double factor = race.getRace().getExplicitFactor();
                if (factor != null) {
                    holder.explicit_factor.setText(factor_format.format(factor));
                    holder.explicit_factor.setVisibility(View.VISIBLE);
                }
            }
        }
        return convertView;
    }

    private void setDependingText(ViewHolder holder, RaceListDataTypeRace race) {
        if (holder.depends_on != null) {
            StartTimeFinderResult result = race.getRace().getState().getStartTimeFinderResult();
            if (result != null && result.isDependentStartTime()) {
                SimpleRaceLogIdentifier identifier = Util.get(result.getDependingOnRaces(), 0);
                ManagedRace depending_race = DataManager.create(getContext()).getDataStore().getRace(identifier);
                holder.depends_on.setText(
                        getContext().getString(R.string.minutes_after_long, result.getStartTimeDiff().asMinutes(),
                                RaceHelper.getShortReverseRaceName(depending_race, " / ", race.getRace())));
                holder.depends_on.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getViewTypeCount() {
        return ViewType.values().length;
    }

    @Override
    public boolean isEnabled(int position) {
        return (getItem(position) instanceof RaceListDataTypeRace);
    }

    @Override
    public void onResult(List<ManagedRace> filteredRaces) {
        synchronized (mLockObject) {
            mShownViewItems.clear();
            if (filteredRaces != null) {
                mShownViewItems.addAll(getShownViewItems(filteredRaces));
            }
            notifyDataSetChanged();
        }
    }

    /**
     * For the {@code filteredRaces} ensure that exactly the required view items are in {@link #mShownViewItems}. This
     * encompasses the header items for all non-empty series and the race items for all races in their series.
     */
    private List<RaceListDataType> getShownViewItems(List<ManagedRace> races) {
        final Map<RaceListDataTypeHeader, List<RaceListDataTypeRace>> raceItemsByHeader = getRaceItemsGroupedBySeriesHeader(
                races);
        return serializeItems(raceItemsByHeader);
    }

    private List<RaceListDataType> serializeItems(
            final Map<RaceListDataTypeHeader, List<RaceListDataTypeRace>> raceItemsByHeader) {
        final List<RaceListDataType> result = new ArrayList<>();
        final List<RaceListDataTypeHeader> headers = new ArrayList<>(raceItemsByHeader.keySet());
        Collections.sort(headers, new Comparator<RaceListDataTypeHeader>() {
            private final RaceGroupSeriesComparator c = new RaceGroupSeriesComparator();

            @Override
            public int compare(RaceListDataTypeHeader lhs, RaceListDataTypeHeader rhs) {
                return c.compare(lhs.getRegattaSeries(), rhs.getRegattaSeries());
            }
        });
        for (final RaceListDataTypeHeader header : headers) {
            result.add(header);
            final List<RaceListDataTypeRace> items = raceItemsByHeader.get(header);
            if (items != null) {
                final List<RaceListDataTypeRace> raceItems = new ArrayList<>(items);
                Collections.sort(raceItems, new Comparator<RaceListDataTypeRace>() {
                    final CurrentRaceComparator c = new CurrentRaceComparator();

                    @Override
                    public int compare(RaceListDataTypeRace lhs, RaceListDataTypeRace rhs) {
                        final int result;
                        if (lhs != null && rhs != null) {
                            result = c.compare(lhs.getRace(), rhs.getRace());
                        } else {
                            Log.e(TAG, "Internal error; found null for NavDrawer item while sorting");
                            result = 0;
                        }
                        return result;
                    }
                });
                result.addAll(raceItems);
            }
        }
        return result;
    }

    private Map<RaceListDataTypeHeader, List<RaceListDataTypeRace>> getRaceItemsGroupedBySeriesHeader(
            final List<ManagedRace> filteredRaces) {
        final Map<RaceListDataTypeHeader, List<RaceListDataTypeRace>> raceItemsByHeader = new HashMap<>();
        for (final ManagedRace race : filteredRaces) {
            final SeriesBase series = race.getSeries();
            final RaceListDataTypeHeader seriesHeader = viewItemsSeriesHeaders.get(series);
            List<RaceListDataTypeRace> raceItemsInSeries = raceItemsByHeader.get(seriesHeader);
            if (seriesHeader != null && raceItemsInSeries == null) {
                raceItemsInSeries = new ArrayList<>();
                raceItemsByHeader.put(seriesHeader, raceItemsInSeries);
            }
            final RaceListDataTypeRace viewItemForRace = viewItemsRaces.get(race);
            if (raceItemsInSeries != null && viewItemForRace != null) {
                raceItemsInSeries.add(viewItemForRace);
            } else {
                Log.w(TAG, "A view item for race " + race + " provided by the filter could not be found",
                        new RuntimeException("Here is where it happened"));
            }
        }
        return raceItemsByHeader;
    }

    public void setSelectedRace(RaceListDataType id) {
        mSelectedRace = id;
    }

    private void resetValues(ViewHolder holder) {
        if (holder.panel_left != null) {
            holder.panel_left.setVisibility(View.VISIBLE);
        }
        if (holder.panel_right != null) {
            holder.panel_right.setVisibility(View.VISIBLE);
        }
        if (holder.update_badge != null) {
            holder.update_badge.setVisibility(View.GONE);
        }
        if (holder.race_flag != null) {
            holder.race_flag.setVisibility(View.GONE);
        }
        if (holder.race_started != null) {
            holder.race_started.setText("");
        }
        if (holder.race_finished != null) {
            holder.race_finished.setVisibility(View.GONE);
        }
        if (holder.race_scheduled != null) {
            holder.race_scheduled.setVisibility(View.VISIBLE);
        }
        if (holder.race_unscheduled != null) {
            holder.race_unscheduled.setVisibility(View.GONE);
        }
        if (holder.has_dependent_races != null) {
            holder.has_dependent_races.setVisibility(View.GONE);
        }
        if (holder.depends_on != null) {
            holder.depends_on.setTextColor(ThemeHelper.getColor(getContext(), R.attr.sap_light_gray));
            holder.depends_on.setVisibility(View.GONE);
        }
        if (holder.explicit_factor != null) {
            holder.explicit_factor.setVisibility(View.GONE);
        }
        if (holder.warning_sign != null) {
            holder.warning_sign.setVisibility(View.GONE);
        }
    }

    public void onRacesChanged() {
        synchronized (mLockObject) {
            createViewItemsForRacesAndSeries();
            getFilter().refreshRegattaStructures();
        }
    }

    private enum ViewType {
        HEADER(0), RACE(1);

        public final int index;

        ViewType(int index) {
            this.index = index;
        }
    }

    private static class ViewHolder {

        private View itemView;
        private RaceTimeView time;
        private TextView race_finished;
        private ViewGroup panel_left;
        private ViewGroup panel_right;
        private ImageView update_badge;
        private FlagTimeView race_flag_new;
        private LinearLayout race_flag;
        private TextView race_name;
        private TextView race_started;
        private LinearLayout race_scheduled;
        private TextView race_unscheduled;
        private View protest_layout;
        private ImageView protest_image;
        private ImageView protest_warning_image;
        private TextView boat_class;
        private TextView fleet_series;
        private ImageView has_dependent_races;
        private TextView depends_on;
        private TextView explicit_factor;
        private ImageView warning_sign;

        private void findViews(View layout) {
            itemView = layout;
            panel_left = ViewHelper.get(layout, R.id.panel_left);
            panel_right = ViewHelper.get(layout, R.id.panel_right);
            update_badge = ViewHelper.get(layout, R.id.update_badge);
            race_flag_new = ViewHelper.get(layout, R.id.race_flag);
            race_flag = ViewHelper.get(layout, R.id.race_flag);
            time = ViewHelper.get(layout, R.id.time);
            race_name = ViewHelper.get(layout, R.id.race_name);
            race_finished = ViewHelper.get(layout, R.id.race_finished);
            race_started = ViewHelper.get(layout, R.id.race_started);
            race_scheduled = ViewHelper.get(layout, R.id.race_scheduled);
            race_unscheduled = ViewHelper.get(layout, R.id.race_unscheduled);
            protest_layout = ViewHelper.get(layout, R.id.protest_layout);
            protest_image = ViewHelper.get(layout, R.id.protest_image);
            protest_warning_image = ViewHelper.get(layout, R.id.protest_warning_image);
            boat_class = ViewHelper.get(layout, R.id.boat_class);
            fleet_series = ViewHelper.get(layout, R.id.fleet_series);
            has_dependent_races = ViewHelper.get(layout, R.id.has_dependent_races);
            depends_on = ViewHelper.get(layout, R.id.depends_on);
            explicit_factor = ViewHelper.get(layout, R.id.explicit_factor);
            warning_sign = ViewHelper.get(layout, R.id.panel_additional_image);
        }
    }
}
