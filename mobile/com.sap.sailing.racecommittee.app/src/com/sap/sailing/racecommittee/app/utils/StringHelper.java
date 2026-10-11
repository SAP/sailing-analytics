package com.sap.sailing.racecommittee.app.utils;

import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;

import android.content.Context;
import androidx.annotation.NonNull;

public class StringHelper {

    private Context mContext;

    private StringHelper(Context context) {
        mContext = context;
    }

    public static StringHelper on(Context context) {
        return new StringHelper(context);
    }

    public String getAuthor(String author) {
        switch (author) {
        case AppConstants.AUTHOR_TYPE_OFFICER_VESSEL:
            author = mContext.getString(R.string.author_type_officer_vessel);
            break;
        case AppConstants.AUTHOR_TYPE_SUPERUSER:
            author = mContext.getString(R.string.author_type_superuser);
            break;
        case AppConstants.AUTHOR_TYPE_SHORE_CONTROL:
            author = mContext.getString(R.string.author_type_shore_control);
            break;
        case AppConstants.AUTHOR_TYPE_VIEWER:
            author = mContext.getString(R.string.author_viewer);
            break;
        }
        return author;
    }

    public boolean containsIgnoreCase(@NonNull String src, @NonNull String what) {
        final int length = what.length();
        if (length == 0) {
            return true; // Empty string is contained
        }

        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            // Quick check before calling the more expensive regionMatches() method:
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp) {
                continue;
            }

            if (src.regionMatches(true, i, what, 0, length)) {
                return true;
            }
        }

        return false;
    }
}
