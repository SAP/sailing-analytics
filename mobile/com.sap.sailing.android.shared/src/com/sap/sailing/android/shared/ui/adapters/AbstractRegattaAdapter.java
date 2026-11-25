package com.sap.sailing.android.shared.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

public abstract class AbstractRegattaAdapter extends ResourceCursorAdapter {

    public AbstractRegattaAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

}
