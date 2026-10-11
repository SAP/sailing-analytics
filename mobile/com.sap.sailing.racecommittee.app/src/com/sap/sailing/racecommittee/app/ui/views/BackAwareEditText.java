package com.sap.sailing.racecommittee.app.ui.views;

import android.content.Context;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class BackAwareEditText extends AppCompatEditText {
    private InputDownPressedListener mInputDownPressedListener;

    public BackAwareEditText(Context context) {
        super(context);
    }

    public BackAwareEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackAwareEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (mInputDownPressedListener != null) {
                mInputDownPressedListener.onImeBack(this);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void setInputDownPressedListener(InputDownPressedListener listener) {
        mInputDownPressedListener = listener;
    }

    public interface InputDownPressedListener {
        void onImeBack(BackAwareEditText editText);
    }
}
