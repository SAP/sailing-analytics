package com.sap.sailing.racecommittee.app.ui.layouts;

import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.racecommittee.app.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.StringRes;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HeaderLayout extends RelativeLayout {

    private TextView mHeaderText;

    public HeaderLayout(Context context) {
        this(context, null);
    }

    public HeaderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.header_view, this);

        mHeaderText = (TextView) findViewById(R.id.header_text);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HeaderLayout);

        setHeaderText(a.getString(R.styleable.HeaderLayout_headerText));
        showChevron(a.getBoolean(R.styleable.HeaderLayout_showChevron, true));

        a.recycle();
    }

    public CharSequence getHeaderText() {
        if (mHeaderText != null) {
            return mHeaderText.getText();
        }
        return null;
    }

    public void setHeaderText(@StringRes int id) {
        setHeaderText(getContext().getString(id));
    }

    public void setHeaderText(CharSequence headerText) {
        if (mHeaderText != null) {
            mHeaderText.setText(headerText);
        }
    }

    public void showChevron(boolean show) {
        if (mHeaderText != null && !isInEditMode()) {
            Drawable chevron = BitmapHelper.getAttrDrawable(getContext(), R.attr.chevron_left_24dp);
            mHeaderText.setCompoundDrawablesWithIntrinsicBounds((show) ? chevron : null, null, null, null);
        }
    }

    public void setHeaderOnClickListener(OnClickListener listener) {
        if (mHeaderText != null) {
            mHeaderText.setOnClickListener(listener);
        }
    }
}
