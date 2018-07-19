package com.sap.sailing.android.tracking.app.customviews;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

public class OpenSansButton extends AppCompatButton {

    public OpenSansButton(Context context) {
        super(context);
        init();
    }

    public OpenSansButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OpenSansButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/OpenSans-Regular.ttf"));
    }

}
