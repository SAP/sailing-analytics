package com.sap.sailing.android.shared.ui.customviews;

import com.sap.sailing.android.shared.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

/**
 * Draws one vertical indicator that is colored depending on the signal quality
 *
 * @author Lukas Zielinski
 * @author Peter Siegmund
 */
public class SignalQualityIndicatorView extends View {

    private GPSQuality mSignalQuality;

    private Paint paintNone;
    private Paint paintPoor;
    private Paint paintGood;
    private Paint paintGreat;

    private float height;
    private float width;
    private Rect rect;

    public SignalQualityIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SignalQualityIndicatorView, 0, 0);
        try {
            mSignalQuality = GPSQuality.getValue(a.getInteger(R.styleable.SignalQualityIndicatorView_signalQuality, 0));
            if (mSignalQuality == null) {
                mSignalQuality = GPSQuality.noSignal;
            }
        } finally {
            a.recycle();
        }

        setAccessibilityString();
        initPaint();
    }

    public int getSignalQuality() {
        return mSignalQuality.toInt();
    }

    private void setAccessibilityString() {
        String desc = getContext().getString(R.string.signal_accuracy_indicator_view_description) + " ";
        switch (mSignalQuality) {
        case poor:
            desc += getContext().getString(R.string.poor);
            break;

        case good:
            desc += getContext().getString(R.string.good);
            break;

        case great:
            desc += getContext().getString(R.string.great);
            break;

        default:
            desc += getContext().getString(R.string.no_signal);

        }
        setContentDescription(desc);
    }

    private void initPaint() {
        paintNone = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNone.setColor(ContextCompat.getColor(getContext(), R.color.signal_none));
        paintPoor = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintPoor.setColor(ContextCompat.getColor(getContext(), R.color.signal_poor));
        paintGood = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGood.setColor(ContextCompat.getColor(getContext(), R.color.signal_good));
        paintGreat = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGreat.setColor(ContextCompat.getColor(getContext(), R.color.signal_great));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint;
        switch (mSignalQuality) {
        case poor:
            paint = paintPoor;
            break;

        case good:
            paint = paintGood;
            break;

        case great:
            paint = paintGreat;
            break;

        default:
            paint = paintNone;
        }

        rect.top = 0;
        rect.left = 0;
        rect.right = (int) this.width;
        rect.bottom = (int) this.height;
        canvas.drawRect(rect, paint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float boxWidth = w / 4.5f;
        float boxHeight = h;

        rect = new Rect(0, 0, (int) boxWidth, (int) boxHeight);

        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());
        this.width = (float) w - xpad;
        this.height = (float) h - ypad;
    }

    @Override
    public int getBaseline() {
        return getMeasuredHeight() - 10;
    }

    /**
     * Must be 0,2,3 or 4, otherwise 1 will be set
     *
     * @param signalQuality
     *            the new signal quality
     */
    public void setSignalQuality(GPSQuality signalQuality) {
        GPSQuality previousSignalQuality = mSignalQuality;

        mSignalQuality = signalQuality;
        if (mSignalQuality == null) {
            mSignalQuality = GPSQuality.noSignal;
        }

        if (mSignalQuality != previousSignalQuality) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }

        setAccessibilityString();

        invalidate();
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minw = getPaddingLeft() + getPaddingRight() + 20;
        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);
        int minh = 50;
        setMeasuredDimension(w, minh);
    }

    /**
     * generate some accessibility info
     */
    public String getAccessibilityText() {
        String result;
        switch (mSignalQuality) {
        case poor:
            result = "poor signal quality";
            break;

        case good:
            result = "good signal quality";
            break;

        case great:
            result = "great signal quality";
            break;

        default:
            result = "no signal";
        }
        return result;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);

        CharSequence text = getAccessibilityText();
        if (!TextUtils.isEmpty(text)) {
            event.getText().add(text);
        }
    }
}
