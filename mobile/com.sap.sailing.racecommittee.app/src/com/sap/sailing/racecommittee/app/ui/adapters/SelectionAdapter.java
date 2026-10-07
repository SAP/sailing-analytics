package com.sap.sailing.racecommittee.app.ui.adapters;

import java.util.List;

import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.SelectionItem;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {

    private Context mContext;
    private List<SelectionItem> mItems;
    private ItemClick mListener;
    private ViewGroup mParent;

    public SelectionAdapter(Context context, List<SelectionItem> items, ItemClick listener) {
        mContext = context;
        mItems = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mParent = parent;
        View view = LayoutInflater.from(mContext).inflate(R.layout.selection_list_item, mParent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SelectionItem item = mItems.get(position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(item.getRunnable());
                }
            }
        });
        holder.icon.setVisibility(View.VISIBLE);
        holder.caption.setText(item.getCaption());
        holder.value.setText(item.getValue());
        holder.mSwitch.setVisibility(View.GONE);
        holder.mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                item.setChecked(isChecked);
            }
        });
        holder.drawable.setImageDrawable(null);
        holder.drawable.setVisibility(View.GONE);
        if (!item.isSwitch()) {
            if (item.getDrawable() != null) {
                holder.drawable.setImageDrawable(item.getDrawable());
                holder.drawable.setVisibility(View.VISIBLE);
            }
        } else {
            holder.icon.setVisibility(View.INVISIBLE);
            holder.mSwitch.setChecked(item.isChecked());
            holder.mSwitch.setVisibility(View.VISIBLE);
        }

        if (AppUtils.with(mContext).is10inch() && AppUtils.with(mContext).isLandscape()) {
            int minHeight = mContext.getResources().getDimensionPixelSize(R.dimen.selector_header_min_height);
            int height = mParent.getMeasuredHeight() / mItems.size();
            int maxHeight = mContext.getResources().getDimensionPixelSize(R.dimen.selector_header_height);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.layout.getLayoutParams();
            params.height = Math.max(minHeight, Math.min(height, maxHeight));
        }
    }

    @Override
    public int getItemCount() {
        return (mItems != null) ? mItems.size() : 0;
    }

    public interface ItemClick {
        void onItemClick(Runnable runnable);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ViewGroup layout;
        public TextView caption;
        public ImageView drawable;
        public TextView value;
        public Switch mSwitch;
        public ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);

            icon = ViewHelper.get(itemView, R.id.item_icon);
            layout = ViewHelper.get(itemView, R.id.selection);
            caption = ViewHelper.get(itemView, R.id.item_caption);
            drawable = ViewHelper.get(itemView, R.id.item_flag);
            value = ViewHelper.get(itemView, R.id.item_value);
            mSwitch = ViewHelper.get(itemView, R.id.item_switch);
        }
    }
}
