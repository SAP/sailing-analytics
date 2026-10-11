package com.sap.sailing.racecommittee.app.ui.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.utils.CompetitorUtils;

import java.util.List;
import java.util.Map;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class CompetitorAndBoatAdapter extends RecyclerView.Adapter<CompetitorAndBoatAdapter.ViewHolder> {

    private static final String TAG = CompetitorAndBoatAdapter.class.getName();

    private final Context mContext;
    private final List<Map.Entry<Competitor, Boat>> mData;
    private boolean mCanBoatsOfCompetitorsChangePerRace;
    private CompetitorClick mListener;

    public CompetitorAndBoatAdapter(Context context, List<Map.Entry<Competitor, Boat>> data,
            boolean canBoatsOfCompetitorsChangePerRace) {
        mContext = context;
        mData = data;
        mCanBoatsOfCompetitorsChangePerRace = canBoatsOfCompetitorsChangePerRace;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.race_tracking_list_competitor_item, parent, false);
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Competitor competitor = mData.get(position).getKey();
        Boat boat = mData.get(position).getValue();
        if (competitor != null) {
            if (mCanBoatsOfCompetitorsChangePerRace && boat != null) {
                holder.vesselId.setText(boat.getSailID());
                if (boat.getColor() != null) {
                    ViewHelper.setColors(holder.vesselId, boat.getColor().getAsHtml());
                }
            }
            String name = CompetitorUtils.getDisplayName(competitor);
            if (holder.competitor != null) {
                holder.competitor.setText(name);
            }
        } else {
            ExLog.e(mContext, TAG, "Competitor at position " + position + " was unexpected null");
        }
    }

    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        }
        return 0;
    }

    public void setListener(CompetitorClick listener) {
        mListener = listener;
    }

    public interface CompetitorClick {
        void onCompetitorClick(Competitor competitor);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View container;
        TextView vesselId;
        TextView competitor;

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            container = itemView.findViewById(R.id.container);
            vesselId = (TextView) itemView.findViewById(R.id.vessel_id);
            competitor = (TextView) itemView.findViewById(R.id.competitor);
        }

        @Override
        public void onClick(View v) {
            // Returns RecyclerView#NO_POSITION if item has been removed from the adapter
            // This can be reproduced by double-tap on item
            int position = getAdapterPosition();
            if (position != NO_POSITION && mListener != null) {
                Competitor competitor = mData.get(position).getKey();
                if (competitor != null) {
                    mListener.onCompetitorClick(competitor);
                }
            }
        }
    }
}
