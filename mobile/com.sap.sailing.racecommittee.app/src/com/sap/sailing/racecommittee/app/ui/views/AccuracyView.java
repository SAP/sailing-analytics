package com.sap.sailing.racecommittee.app.ui.views;

import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import android.content.Context;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AccuracyView extends LinearLayout {

    private TextView mAccuracyValue;
    private ImageView mSegment01;
    private ImageView mSegment02;
    private ImageView mSegment03;

    public AccuracyView(Context context) {
        this(context, null);
    }

    public AccuracyView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AccuracyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(getContext(), R.layout.accuracy, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mAccuracyValue = (TextView) findViewById(R.id.accuracy);
        mSegment01 = (ImageView) findViewById(R.id.accuracy_segment_01);
        mSegment02 = (ImageView) findViewById(R.id.accuracy_segment_02);
        mSegment03 = (ImageView) findViewById(R.id.accuracy_segment_03);

        setAccuracy(-1);
    }

    public void setAccuracy(float accuracy) {
        if (accuracy <= 0) {
            setText(getContext().getString(R.string.not_available),
                    ContextCompat.getColor(getContext(), R.color.sap_red));
            hide(mSegment01);
            hide(mSegment02);
            hide(mSegment03);
        } else {
            setText(getContext().getString(R.string.accuracy_value, accuracy),
                    ThemeHelper.getColor(getContext(), R.attr.white));
            if (accuracy <= 10) {
                setColor(mSegment01, R.color.accuracy_green);
                setColor(mSegment02, R.color.accuracy_green);
                setColor(mSegment03, R.color.accuracy_green);
            } else if (accuracy <= 100) {
                setColor(mSegment01, R.color.accuracy_yellow);
                setColor(mSegment02, R.color.accuracy_yellow);
                setColor(mSegment03, R.color.accuracy_yellow_light);
            } else {
                setColor(mSegment01, R.color.accuracy_red);
                setColor(mSegment02, R.color.accuracy_red_light);
                setColor(mSegment03, R.color.accuracy_red_light);
            }
        }
    }

    private void setText(String text, int color) {
        if (mAccuracyValue != null) {
            mAccuracyValue.setText(text);
            mAccuracyValue.setTextColor(color);
        }
    }

    private void setColor(ImageView segment, @ColorRes int color) {
        if (segment != null) {
            segment.setBackgroundColor(getContext().getResources().getColor(color));
            segment.setVisibility(VISIBLE);
        }
    }

    private void hide(View view) {
        if (view != null) {
            view.setVisibility(GONE);
        }
    }
}
