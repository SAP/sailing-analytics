package com.sap.sailing.android.buoy.positioning.app.adapter;

import java.text.DecimalFormat;
import java.util.List;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.buoy.positioning.app.provider.AnalyticsContract;
import com.sap.sailing.android.buoy.positioning.app.util.DatabaseHelper;
import com.sap.sailing.android.buoy.positioning.app.valueobjects.MarkPingInfo;
import com.sap.sailing.server.gateway.serialization.impl.FlatGPSFixJsonSerializer;

import android.content.Context;
import android.database.Cursor;
import androidx.cursoradapter.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.TextView;

public class MarkAdapter extends ResourceCursorAdapter {

    public MarkAdapter(Context context, int layout, Cursor cursor, int flags) {
        super(context, layout, cursor, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final TextView markName = (TextView) view.findViewById(R.id.mark_name);
        final TextView markSet = (TextView) view.findViewById(R.id.mark_set);
        final String name = cursor.getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Mark.MARK_NAME));
        final String markID = cursor.getString(cursor.getColumnIndexOrThrow(AnalyticsContract.Mark.MARK_ID));
        final List<MarkPingInfo> markPings = DatabaseHelper.getInstance().getMarkPings(context, markID);
        final String setText;
        if (markPings.isEmpty()) {
            setText = context.getString(R.string.not_set);
        } else {
            final DecimalFormat df = new DecimalFormat("#.##");
            final double accuracy = markPings.get(0).getAccuracy();
            final String accuracyString;
            // accuracy-values that are stored as -1 (meaning unknown) will simply be displayed as "set" without an
            // accuracy
            if (accuracy == FlatGPSFixJsonSerializer.NOT_AVAILABLE_THROUGH_SERVER) {
                accuracyString = context.getString(R.string.set);
            } else {
                accuracyString = context.getString(R.string.set) + " "
                        + context.getString(R.string.mark_list_accuracy, df.format(accuracy));
            }
            setText = accuracyString;
        }
        markName.setText(name);
        markSet.setText(setText);
    }
}
