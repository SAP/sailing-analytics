package com.sap.sailing.racecommittee.app.ui.views;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import android.util.AttributeSet;

public class UrlEditTextPreference extends EditTextPreference {

    public UrlEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public UrlEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UrlEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UrlEditTextPreference(Context context) {
        super(context);
    }

    @Override
    public void setText(String text) {
        super.setText(normalizeUrl(text));
    }

    private String normalizeUrl(@NonNull String url) {
        //Remove any newline and whitespace
        final Uri uri = Uri.parse(url.replaceAll("[\\n\\s]", ""))
                .normalizeScheme();
        final String scheme = uri.getScheme();
        //Replace (trailing) slash
        final String ssp = uri.getSchemeSpecificPart().replaceAll("^/+", "//").replaceAll("/$", "");
        final Uri.Builder builder = new Uri.Builder()
                //Use default scheme if missing
                .scheme(scheme == null ? "https" : scheme)
                //Ensure leading double slash in scheme-specific-part
                .encodedOpaquePart(ssp.startsWith("//") ? ssp : "//" + ssp);
        return builder.build().toString();
    }

    @Override
    public CharSequence getSummary() {
        return getText();
    }
}
