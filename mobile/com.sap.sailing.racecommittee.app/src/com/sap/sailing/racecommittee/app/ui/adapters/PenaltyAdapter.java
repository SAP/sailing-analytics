package com.sap.sailing.racecommittee.app.ui.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.CompetitorResultEditableImpl;
import com.sap.sailing.racecommittee.app.utils.StringHelper;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sse.common.util.NaturalComparator;

public class PenaltyAdapter extends RecyclerView.Adapter<PenaltyAdapter.ViewHolder> {

    private Context mContext;
    private List<CompetitorResultEditableImpl> mCompetitor;
    private List<CompetitorResultEditableImpl> mFiltered;
    private ItemListener mListener;
    private OrderBy mOrderBy = OrderBy.SAILING_NUMBER;
    private String mFilter;

    public PenaltyAdapter(Context context, @NonNull ItemListener listener) {
        mContext = context;
        mListener = listener;
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
        if (!item.getMaxPointsReason().equals(MaxPointsReason.NONE)) {
            bgId = R.attr.sap_gray_black_20;
        }
        holder.itemView.setBackgroundColor(ThemeHelper.getColor(mContext, bgId));
        holder.mItemText.setText(item.getCompetitorDisplayName());

        final boolean hasReason = !MaxPointsReason.NONE.equals(item.getMaxPointsReason());
        holder.mItemPenalty.setVisibility(hasReason ? View.VISIBLE : View.GONE);
        if (hasReason) {
            holder.mItemPenalty.setText(item.getMaxPointsReason().name());
        }

        holder.mItemCheck.setChecked(item.isChecked());
        holder.mItemCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setChecked(isChecked);
                if (mListener != null) {
                    mListener.onCheckedChanged(item, isChecked);
                }
            }
        });

        holder.mItemEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onEditClicked(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return (mFiltered != null) ? mFiltered.size() : 0;
    }

    public void setCompetitor(List<CompetitorResultEditableImpl> competitor) {
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
                    if (StringHelper.on(mContext).containsIgnoreCase(mCompetitor.get(i).getCompetitorDisplayName(), mFilter)) {
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
            case SAILING_NUMBER:
                comparator = new DisplayNameComparator(0);
                break;

            case COMPETITOR_NAME:
                comparator = new DisplayNameComparator(1);
                break;

            default:
                break;
        }

        if (comparator != null) {
            Collections.sort(mFiltered, comparator);
        }
        notifyDataSetChanged();
    }

    public enum OrderBy {
        SAILING_NUMBER, COMPETITOR_NAME, START_LINE, FINISH_LINE
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
            String[] leftItem = splitDisplayName(lhs.getCompetitorDisplayName());
            String[] rightItem = splitDisplayName(rhs.getCompetitorDisplayName());
            return mNaturalComparator.compare(leftItem[mPos], rightItem[mPos]);
        }

        private String[] splitDisplayName(String displayName) {
            return displayName.split(" - ");
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private CheckBox mItemCheck;
        private TextView mItemText;
        private TextView mItemPenalty;
        private View mItemEdit;

        public ViewHolder(View itemView) {
            super(itemView);

            mItemCheck = ViewHelper.get(itemView, R.id.item_check);
            mItemText = ViewHelper.get(itemView, R.id.item_text);
            mItemPenalty = ViewHelper.get(itemView, R.id.item_penalty);
            mItemEdit = ViewHelper.get(itemView, R.id.item_edit);
        }
    }
}
