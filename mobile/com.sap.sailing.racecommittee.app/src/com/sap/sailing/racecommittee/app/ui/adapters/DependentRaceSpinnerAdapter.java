package com.sap.sailing.racecommittee.app.ui.adapters;

import java.util.ArrayList;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sse.common.Util;

import android.content.Context;
import android.database.DataSetObserver;
import androidx.annotation.LayoutRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class DependentRaceSpinnerAdapter implements SpinnerAdapter {

    private static final int DATA = 0;

    private Context mContext;
    private int mLayout;
    private int mSelectedItem = -1;

    private ArrayList<Util.Pair<String, String>> mData;

    public DependentRaceSpinnerAdapter(Context context, @LayoutRes int layout) {
        mContext = context;
        mLayout = layout;
        mData = new ArrayList<>();
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View layout = convertView;
        if (layout == null) {
            layout = LayoutInflater.from(mContext).inflate(mLayout, parent, false);
        }

        TextView mainText = ViewHelper.get(layout, android.R.id.text1);
        if (mainText != null) {
            String text = mData.get(position).getB();
            if (TextUtils.isEmpty(text)) {
                text = mData.get(position).getA();
            }
            mainText.setText(text);
        }

        if (mSelectedItem == position) {
            layout.setBackgroundColor(ThemeHelper.getColor(mContext, R.attr.sap_light_gray));
        } else {
            layout.setBackgroundColor(ThemeHelper.getColor(mContext, R.attr.sap_gray_black_30));
        }
        layout.setClickable(false);
        if (mainText != null) {
            mainText.setTextColor(ThemeHelper.getColor(mContext, R.attr.white));
        }

        return layout;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Util.Pair<String, String> getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View layout = convertView;
        if (layout == null) {
            layout = LayoutInflater.from(mContext).inflate(mLayout, parent, false);
        }

        TextView text = ViewHelper.get(layout, android.R.id.text1);
        String spinnerText = mData.get(position).getB();
        if (TextUtils.isEmpty(spinnerText)) {
            spinnerText = mData.get(position).getA();
        }
        text.setText(spinnerText);

        return layout;
    }

    @Override
    public int getItemViewType(int position) {
        return DATA;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return (mData.size() == 0);
    }

    public int add(Util.Pair<String, String> data) {
        boolean found = false;
        int position = -1;

        int counter = 0;
        for (Util.Pair<String, String> pair : mData) {
            if (pair.getA().equals(data.getA())) {
                if (pair.getB() == null && data.getB() == null) {
                    found = true;
                    position = counter;
                    break;
                }
                if (pair.getB() != null && pair.getB().equals(data.getB())) {
                    found = true;
                    position = counter;
                    break;
                }
            }
            counter++;
        }
        if (!found) {
            mData.add(data);
            position = mData.size() - 1;
        }

        return position;
    }

    public void setSelected(int position) {
        mSelectedItem = position;
    }
}
