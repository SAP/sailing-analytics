package com.sap.sailing.racecommittee.app.ui.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.utils.TouchEventListener;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

public class PanelButton extends FrameLayout implements DialogInterface.OnClickListener {

    public final static int LEVEL_UNKNOWN = -1;
    public final static int LEVEL_NORMAL = 0;
    public final static int LEVEL_TOGGLED = 1;

    public enum PanelType {
        Value(0),
        Image(1),
        Switch(2),
        Flag(3);

        private int value;

        PanelType(int type) {
            value = type;
        }

        @Nullable
        public static PanelType fromId(int id) {
            for (PanelType type : PanelType.values()) {
                if (type.value == id) {
                    return type;
                }
            }
            return null;
        }
    }

    public enum CaptionPosition {
        top(0),
        bottom(1);

        private int value;

        CaptionPosition(int position) {
            value = position;
        }

        @Nullable
        public static CaptionPosition fromId(int id) {
            for (CaptionPosition position : CaptionPosition.values()) {
                if (position.value == id) {
                    return position;
                }
            }
            return null;
        }
    }

    private View mLayer;
    private View mLock;

    private String mCaption;
    private TextView mHeader;
    private TextView mFooter;

    private View mContent;
    private TextView mValue;
    private ImageView mImage;
    private Switch mSwitch;

    private ImageView mMarker;
    private View mLine;

    private PanelType mType;
    private CaptionPosition mCaptionPosition;

    private PanelButtonClick mListener;
    private TouchEventListener mTouchEventListener;

    public PanelButton(Context context) {
        this(context, null);
    }

    public PanelButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanelButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PanelButton);

        Runnable clickRunnable = new Runnable() {
            @Override
            public void run() {
                if (mLock != null && mLock.getVisibility() == VISIBLE && isNormal()) {
                    if (mType == PanelType.Switch && mSwitch != null) {
                        mSwitch.setChecked(!mSwitch.isChecked());
                    }
                    showChangeDialog(PanelButton.this);
                } else {
                    if (mType == PanelType.Switch) {
                        if (mListener != null && mSwitch != null) {
                            mListener.onChangedSwitch(PanelButton.this, mSwitch.isChecked());
                        }
                    } else {
                        if (mListener != null) {
                            mListener.onClick(PanelButton.this);
                        }
                    }
                }
            }
        };

        mTouchEventListener = new TouchEventListener(this);
        mTouchEventListener.setClickRunnable(clickRunnable);

        setPanelType(PanelType.fromId(a.getInt(R.styleable.PanelButton_buttonType, PanelType.Value.value)));

        switch (mType) {
            default:
                inflate(getContext(), R.layout.panel_button_setup, this);
        }

        mLayer = findViewById(R.id.panel_layer);
        mContent = findViewById(R.id.panel_content);

        mHeader = (TextView) findViewById(R.id.panel_button_header);
        mFooter = (TextView) findViewById(R.id.panel_button_footer);
        setCaptionPosition(CaptionPosition.fromId(a.getInt(R.styleable.PanelButton_captionPosition, CaptionPosition.top.value)));
        setCaption(a.getString(R.styleable.PanelButton_buttonCaption));

        mValue = (TextView) findViewById(R.id.panel_value);
        setPanelText(a.getString(R.styleable.PanelButton_buttonValue));

        mImage = (ImageView) findViewById(R.id.panel_image);
        int size = a.getDimensionPixelSize(R.styleable.PanelButton_imageSize, 0);
        if (size > 0) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mImage.getLayoutParams();
            params.height = size;
            params.width = size;
        }
        setPanelImage(a.getDrawable(R.styleable.PanelButton_buttonImage));

        mSwitch = (Switch) findViewById(R.id.panel_switch);
        setPanelSwitch(a.getString(R.styleable.PanelButton_buttonSwitch));

        mLock = findViewById(R.id.panel_lock);
        setLock(a.getBoolean(R.styleable.PanelButton_showLock, false));

        mMarker = (ImageView) findViewById(R.id.marker_bottom);

        setLinePosition(a.getInt(R.styleable.PanelButton_linePosition, -1));

        a.recycle();

        setMarkerLevel(LEVEL_NORMAL);
    }

    public void setListener(PanelButtonClick listener) {
        mListener = listener;
    }

    public void setCaptionPosition(CaptionPosition position) {
        mCaptionPosition = position;
        if (mHeader != null) {
            mHeader.setVisibility(GONE);
        }
        if (mFooter != null) {
            mFooter.setVisibility(GONE);
        }
        switch (mCaptionPosition) {
            case bottom:
                if (mFooter != null) {
                    mFooter.setVisibility(VISIBLE);
                }
                break;

            default:
                if (mHeader != null) {
                    mHeader.setVisibility(VISIBLE);
                }
        }
        setCaption(mCaption);
    }

    public void setCaption(String caption) {
        mCaption = caption;

        switch (mCaptionPosition) {
            case bottom:
                if (mFooter != null) {
                    mFooter.setText(caption);
                }
                break;

            default:
                if (mHeader != null) {
                    mHeader.setText(caption);
                }
        }
    }

    public boolean isLocked() {
        return mLock != null && mLock.getVisibility() == VISIBLE;
    }

    public void setLock(boolean show) {
        if (mLock != null) {
            mLock.setVisibility(show ? VISIBLE : GONE);
        }
        if (mHeader != null) {
            mHeader.setAlpha(show ? .5f : 1f);
        }
        if (mContent != null) {
            mContent.setAlpha(show ? .5f : 1f);
        }
    }

    public void setPanelType(PanelType type) {
        mType = type;
        if (mType != null) {
            switch (mType) {
                case Switch:
                    mTouchEventListener.setEnabled(false);
                    break;

                default:
                    mTouchEventListener.setEnabled(true);
            }
        } else {
            mTouchEventListener.setEnabled(true);
        }
    }

    public void setPanelText(@StringRes int id) {
        setPanelText(getResources().getString(id));
    }

    public void setPanelText(String text) {
        hideValues();
        if (mValue != null) {
            mValue.setText(text);
        }
    }

    public void setPanelImage(Drawable drawable) {
        hideValues();
        if (mImage != null) {
            mImage.setImageDrawable(drawable);
        }
    }

    public void setPanelSwitch(@StringRes int id) {
        setPanelSwitch(getResources().getString(id));
    }

    public void setPanelSwitch(String text) {
        hideValues();
        if (mSwitch != null) {
            mSwitch.setText(text);
        }
    }

    public void setPanelSwitch(boolean isChecked) {
        if (mSwitch != null) {
            mSwitch.setChecked(isChecked);
        }
    }

    public void setLinePosition(int position) {
        switch (position) {
            case 0:
                mLine = findViewById(R.id.line_left);
                break;

            case 1:
                mLine = findViewById(R.id.line_bottom);
                break;

            case 2:
                mLine = findViewById(R.id.line_right);
                break;

            default:
                // no-op
        }

        if (mLine != null) {
            mLine.setVisibility(VISIBLE);
        }
    }

    private void hideValues() {
        if (mValue != null) {
            mValue.setVisibility(GONE);
        }

        if (mImage != null) {
            mImage.setVisibility(GONE);
        }

        if (mSwitch != null) {
            mSwitch.setVisibility(GONE);
        }

        switch (mType) {
            case Value:
                if (mValue != null) {
                    mValue.setVisibility(VISIBLE);
                }
                break;

            case Image:
                if (mImage != null) {
                    mImage.setVisibility(VISIBLE);
                }
                break;

            case Switch:
                if (mSwitch != null) {
                    mSwitch.setVisibility(VISIBLE);
                }
                break;

            default:
                // no-op
        }
    }

    protected void showChangeDialog(DialogInterface.OnClickListener positiveButton) {
        showChangeDialog(positiveButton, null);
    }

    protected void showChangeDialog(DialogInterface.OnClickListener positiveButton, DialogInterface.OnClickListener negativeButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AppTheme_AlertDialog);
        builder.setTitle(getContext().getString(R.string.change_title));
        builder.setMessage(getContext().getString(R.string.change_message));
        builder.setPositiveButton(getContext().getString(R.string.change_proceed), positiveButton);
        builder.setNegativeButton(getContext().getString(R.string.change_cancel), negativeButton);
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mType == PanelType.Switch) {
            if (mListener != null & mSwitch != null) {
                mSwitch.setChecked(!mSwitch.isChecked());
                mListener.onChangedSwitch(this, mSwitch.isChecked());
            }
        } else {
            if (mListener != null) {
                mListener.onClick(this);
            }
        }
    }

    public int toggleMarker() {
        int retValue = LEVEL_UNKNOWN;

        if (mMarker != null) {
            Drawable drawable = mMarker.getDrawable();
            if (drawable != null) {
                int level = drawable.getLevel();
                retValue = setMarkerLevel(LEVEL_TOGGLED - level);
            }
        }

        return retValue;
    }

    public int getMarkerLevel() {
        int retValue = LEVEL_UNKNOWN;

        if (mMarker != null) {
            Drawable drawable = mMarker.getDrawable();
            if (drawable != null) {
                retValue = drawable.getLevel();
            }
        }

        return retValue;
    }

    public int setMarkerLevel(int level) {
        int retValue = LEVEL_UNKNOWN;

        if (mLayer != null && mMarker != null) {
            Drawable drawable = mMarker.getDrawable();
            if (drawable != null) {
                drawable.setLevel(level);
                retValue = drawable.getLevel();
                switch (retValue) {
                    case LEVEL_TOGGLED:
                        mLayer.setBackgroundColor(ThemeHelper.getColor(getContext(), R.attr.sap_gray_black_20));
                        break;

                    default:
                        mLayer.setBackgroundColor(ThemeHelper.getColor(getContext(), R.attr.sap_gray));
                        break;
                }
            }
        }

        return retValue;
    }

    private boolean isNormal() {
        return (getMarkerLevel() == LEVEL_NORMAL);
    }

    public void disableToggle() {
        setClickable(false);
        mLayer.setBackgroundColor(getResources().getColor(R.color.constant_sap_yellow_1));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setClickable(true);
                setMarkerLevel(getMarkerLevel());
            }
        }, 200);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mTouchEventListener.onTouchEvent(event);
    }

    public interface PanelButtonClick {
        void onClick(PanelButton view);

        void onChangedSwitch(PanelButton view, boolean isChecked);
    }
}
