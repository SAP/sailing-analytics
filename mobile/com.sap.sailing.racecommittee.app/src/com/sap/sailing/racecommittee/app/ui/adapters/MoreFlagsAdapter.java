package com.sap.sailing.racecommittee.app.ui.adapters;

import java.util.ArrayList;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.utils.FlagsResources;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MoreFlagsAdapter extends BaseFlagsAdapter {

    public class MoreFlag extends FlagItem {

        public MoreFlag(String line1, String line2, Flags flag) {
            super(line1, line2, null, true, flag);
        }
    }

    private Context mContext;
    private ArrayList<MoreFlag> mFlags;
    private MoreFlagItemClick mListener;

    public MoreFlagsAdapter(Context context, MoreFlagItemClick listener) {
        mContext = context;
        mListener = listener;

        mFlags = new ArrayList<>();
        mFlags.add(new MoreFlag(context.getString(R.string.flag_blue), context.getString(R.string.flag_blue_desc),
                Flags.BLUE));
    }

    @Override
    public int getCount() {
        return mFlags.size();
    }

    @Override
    public MoreFlag getItem(int position) {
        return mFlags.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.flag_list_item, parent, false);
        }

        final MoreFlag item = getItem(position);

        final ImageView flagImage = ViewHelper.get(convertView, R.id.flag);
        if (flagImage != null) {
            flagImage.setVisibility(View.INVISIBLE);
            if (!TextUtils.isEmpty(item.file_name)) {
                int flagResId = mContext.getResources().getIdentifier(item.file_name, "drawable",
                        mContext.getPackageName());
                if (flagResId != 0) {
                    flagImage.setImageDrawable(ContextCompat.getDrawable(mContext, flagResId));
                    flagImage.setVisibility(View.VISIBLE);
                }
            } else if (item.flag != null) {
                flagImage.setImageDrawable(FlagsResources.getFlagDrawable(mContext, item.flag.name(), 96));
                flagImage.setVisibility(View.VISIBLE);
            }
        }

        final TextView first_line = ViewHelper.get(convertView, R.id.first_line);
        if (first_line != null) {
            first_line.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(item.first_line)) {
                first_line.setVisibility(View.VISIBLE);
                first_line.setText(item.first_line);
            }
        }

        final TextView second_line = ViewHelper.get(convertView, R.id.second_line);
        if (second_line != null) {
            second_line.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(item.second_line)) {
                second_line.setVisibility(View.VISIBLE);
                second_line.setText(item.second_line);
            }
        }

        final Button confirm = ViewHelper.get(convertView, R.id.confirm);
        if (confirm != null && mListener != null) {
            confirm.setVisibility(View.GONE);
            if (item.touched) {
                confirm.setVisibility(View.VISIBLE);
            }

            confirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onClick(item);
                }
            });
        }

        final ImageView more_data = ViewHelper.get(convertView, R.id.more_data);
        if (more_data != null) {
            more_data.setVisibility(View.GONE);
            if (item.more) {
                more_data.setVisibility(View.VISIBLE);
            }
        }

        final RelativeLayout layout = ViewHelper.get(convertView, R.id.line);
        if (layout != null) {
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.more) {
                        if (mListener != null) {
                            mListener.showMore(item);
                        }
                    } else {
                        for (MoreFlag flag : mFlags) {
                            flag.touched = flag.file_name.equals(item.file_name);
                        }
                        notifyDataSetChanged();
                    }
                }
            });
        }

        return convertView;
    }

    public interface MoreFlagItemClick {
        void onClick(MoreFlag flag);

        void showMore(MoreFlag flag);
    }
}
