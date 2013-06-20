package com.sap.sailing.racecommittee.app.ui.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sap.sailing.domain.common.Named;
import com.sap.sailing.racecommittee.app.R;

/**
 * @author D053502
 * 
 */
public class NamedArrayAdapter<T extends Named> extends ArrayAdapter<T> {

    public NamedArrayAdapter(Context context, int textViewResourceId, List<T> namedList) {
        super(context, textViewResourceId, namedList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (convertView == null) {

            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.welter_one_row_no_image, /* view group */null);
        }

        T item = getItem(position);
        TextView title = (TextView) view.findViewById(R.id.Welter_Cell_OneRowNoImage_txtTitle);
        title.setText(item.getName());
        title.setAlpha(isEnabled(position) ? 1.0f : 0.2f);

        return view;
    }

}
