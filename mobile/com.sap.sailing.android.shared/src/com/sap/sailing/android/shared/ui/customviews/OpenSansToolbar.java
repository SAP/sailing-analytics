package com.sap.sailing.android.shared.ui.customviews;

import com.sap.sailing.android.shared.R;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

public class OpenSansToolbar extends Toolbar {

    public OpenSansToolbar(Context context) {
        super(context);
    }

    public OpenSansToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenSansToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTitle(int resId) {
        TextView titleTextView = (TextView) findViewById(R.id.toolbar_title);
        titleTextView.setText(resId);
    }

    @Override
    public void setSubtitle(int resId) {
        TextView subtitleTextView = (TextView) findViewById(R.id.toolbar_subtitle);
        subtitleTextView.setText(resId);
    }

    @Override
    public void setTitle(CharSequence arg0) {
        TextView titleTextView = (TextView) findViewById(R.id.toolbar_title);
        titleTextView.setText(arg0);
    }

    @Override
    public void setSubtitle(CharSequence arg0) {
        TextView subtitleTextView = (TextView) findViewById(R.id.toolbar_subtitle);
        subtitleTextView.setText(arg0);
    }

    public void hideSubtitle() {
        TextView subtitleTextView = (TextView) findViewById(R.id.toolbar_subtitle);
        subtitleTextView.setVisibility(View.GONE);
    }

    public void setTitleSize(float size) {
        TextView titleTextView = (TextView) findViewById(R.id.toolbar_title);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        titleTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.toolbar_text_color));
    }
}
