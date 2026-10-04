package com.sap.sailing.racecommittee.app.ui.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.CompetitorResultEditableImpl;
import com.sap.sailing.racecommittee.app.ui.comparators.CompetitorSailIdComparator;
import com.sap.sailing.racecommittee.app.ui.comparators.CompetitorShortNameComparator;
import com.sap.sailing.racecommittee.app.ui.utils.CompetitorUtils;
import com.sap.sailing.racecommittee.app.utils.StringHelper;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sse.common.util.NaturalComparator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PenaltyAdapter extends RecyclerView.Adapter<PenaltyAdapter.ViewHolder> {

    private static final int COMPETITOR_SHORT_NAME_POSITION = 0;
    private static final int SAILING_NUMBER_POSITION = 1;
    private static final int COMPETITOR_NAME_POSITION = 2;

    private final Context mContext;
    private final ItemListener mListener;
    private final boolean mCanBoatsOfCompetitorsChangePerRace;

    private List<CompetitorResultEditableImpl> mCompetitor;
    private List<CompetitorResultEditableImpl> mFiltered;
    private Map<Serializable, Boat> mBoats;
    private OrderBy mOrderBy = OrderBy.SAILING_NUMBER;
    private String mFilter;

    public PenaltyAdapter(Context context, @NonNull ItemListener listener, boolean canBoatsOfCompetitorsChangePerRace) {
        mContext = context;
        mListener = listener;
        mCanBoatsOfCompetitorsChangePerRace = canBoatsOfCompetitorsChangePerRace;

        mBoats = new HashMap<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.race_penalty_row_item, parent, false);
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final CompetitorResultEditableImpl item = mFiltered.get(position);
        int bgId = R.attr.sap_gray_black_30;
        if (item.getOneBasedRank() != 0) {
            bgId = R.attr.sap_gray_black_20;
        }
        holder.itemView.setBackgroundColor(ThemeHelper.getColor(mContext, bgId));
        holder.mItemText.setText(CompetitorUtils.getDisplayName(item));
        final boolean hasReason = !MaxPointsReason.NONE.equals(item.getMaxPointsReason());
        holder.mItemPenalty.setVisibility(hasReason ? View.VISIBLE : View.GONE);
        if (hasReason) {
            holder.mItemPenalty.setText(item.getMaxPointsReason().name());
        }
        holder.mItemCheck.setOnCheckedChangeListener(null); // because of item recycling
        holder.mItemCheck.setChecked(item.isChecked());
        holder.mItemCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setChecked(isChecked);
                mListener.onCheckedChanged(item, isChecked);
            }
        });
        holder.mItemEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onEditClicked(item);
            }
        });
        holder.mItemVessel.setVisibility(View.GONE);
        if (mCanBoatsOfCompetitorsChangePerRace) {
            Boat boat = mBoats.get(item.getCompetitorId());
            if (boat != null) {
                holder.mItemVessel.setVisibility(View.VISIBLE);
                holder.mItemVessel.setText(boat.getSailID());
                if (boat.getColor() != null) {
                    ViewHelper.setColors(holder.mItemVessel, boat.getColor().getAsHtml());
                }
            }
        }
        Drawable mergeIcon;
        switch (item.getMergeState()) {
        case WARNING:
            mergeIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_warning_yellow);
            break;

        case ERROR:
            mergeIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_warning_red);
            break;

        default:
            mergeIcon = null;
            break;
        }
        holder.mItemMergeState.setImageDrawable(mergeIcon);
    }

    @Override
    public int getItemCount() {
        return (mFiltered != null) ? mFiltered.size() : 0;
    }

    public void setCompetitor(List<CompetitorResultEditableImpl> competitor, @Nullable Map<Competitor, Boat> data) {
        if (data != null) {
            mBoats.clear();
            for (Map.Entry<Competitor, Boat> entry : data.entrySet()) {
                mBoats.put(entry.getKey().getId(), entry.getValue());
            }
        }
        mCompetitor = competitor;
        mFiltered = filterData();
        sortData();
    }

    public void setOrderedBy(OrderBy orderBy) {
        mOrderBy = orderBy;
        sortData();
    }

    public void setFilter(String filter) {
        mFilter = filter;
        mFiltered = filterData();
        sortData();
        notifyDataSetChanged();
    }

    private List<CompetitorResultEditableImpl> filterData() {
        List<CompetitorResultEditableImpl> result = new ArrayList<>();
        if (mCompetitor != null) {
            if (TextUtils.isEmpty(mFilter)) {
                result.addAll(mCompetitor);
            } else {
                for (int i = 0; i < mCompetitor.size(); i++) {
                    if (StringHelper.on(mContext).containsIgnoreCase(CompetitorUtils.getDisplayName(mCompetitor.get(i)),
                            mFilter)) {
                        result.add(mCompetitor.get(i));
                    }
                }
            }
        }
        return result;
    }

    private void sortData() {
        Comparator<CompetitorResultEditableImpl> comparator = null;
        switch (mOrderBy) {
            case COMPETITOR_SHORT_NAME:
                comparator = new DisplayNameComparator(COMPETITOR_SHORT_NAME_POSITION);
                break;
            case SAILING_NUMBER:
                comparator = new DisplayNameComparator(SAILING_NUMBER_POSITION);
                break;
            case COMPETITOR_NAME:
                comparator = new DisplayNameComparator(COMPETITOR_NAME_POSITION);
                break;
            default:
                break;
        }

        if (mFiltered != null && comparator != null) {
            Collections.sort(mFiltered, comparator);
        }
        notifyDataSetChanged();
    }

    public enum OrderBy {
        COMPETITOR_SHORT_NAME, SAILING_NUMBER, COMPETITOR_NAME, START_LINE, FINISH_LINE
    }

    public interface ItemListener {

        void onCheckedChanged(CompetitorResultEditableImpl competitor, boolean isChecked);

        void onEditClicked(CompetitorResultEditableImpl competitor);

    }

    private static class DisplayNameComparator implements Comparator<CompetitorResultEditableImpl> {

        private NaturalComparator mNaturalComparator;
        private int mPos;

        DisplayNameComparator(int position) {
            mNaturalComparator = new NaturalComparator();

            mPos = position;
        }

        @Override
        public int compare(CompetitorResultEditableImpl lhs, CompetitorResultEditableImpl rhs) {
            switch (mPos) {
                case COMPETITOR_SHORT_NAME_POSITION:
                    return CompetitorShortNameComparator
                            .compare(lhs.getShortName(), rhs.getShortName(), mNaturalComparator);
                case SAILING_NUMBER_POSITION:
                    return CompetitorSailIdComparator
                            .compare(lhs.getBoatSailId(), rhs.getBoatSailId(), mNaturalComparator);
                default:
                    return mNaturalComparator.compare(lhs.getName(), rhs.getName());
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private CheckBox mItemCheck;
        private TextView mItemVessel;
        private TextView mItemText;
        private TextView mItemPenalty;
        private View mItemEdit;
        private ImageView mItemMergeState;

        public ViewHolder(View itemView) {
            super(itemView);

            mItemCheck = ViewHelper.get(itemView, R.id.item_check);
            mItemText = ViewHelper.get(itemView, R.id.item_text);
            mItemVessel = ViewHelper.get(itemView, R.id.item_vessel);
            mItemPenalty = ViewHelper.get(itemView, R.id.item_penalty);
            mItemEdit = ViewHelper.get(itemView, R.id.item_edit);
            mItemMergeState = ViewHelper.get(itemView, R.id.item_merge_state);
        }
    }
}
