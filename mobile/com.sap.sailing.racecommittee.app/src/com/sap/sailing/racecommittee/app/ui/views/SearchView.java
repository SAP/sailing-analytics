package com.sap.sailing.racecommittee.app.ui.views;

import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.racecommittee.app.R;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class SearchView extends FrameLayout {

    private ImageView mSearchIcon;
    private EditText mEditText;
    private SearchTextWatcher mWatcher;
    private boolean mCollapsed = true;

    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.search_view, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mEditText = (EditText) findViewById(R.id.search_input);
        if (mEditText != null) {
            mEditText.setVisibility(getResources().getBoolean(R.bool.penalty_search_open) ? VISIBLE : GONE);
            mEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (mWatcher != null) {
                        mWatcher.onTextChanged(s.toString());
                    }
                }
            });
        }
        mSearchIcon = (ImageView) findViewById(R.id.search_icon);
        if (mSearchIcon != null && getResources().getBoolean(R.bool.penalty_search_open)) {
            mSearchIcon.setImageDrawable(BitmapHelper.getAttrDrawable(getContext(), R.attr.clear_24dp));
        }
        View searchButton = findViewById(R.id.search_button);
        if (searchButton != null) {
            searchButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickButton(getResources().getBoolean(R.bool.penalty_search_open));
                }
            });
        }
    }

    public void setSearchTextWatcher(SearchTextWatcher watcher) {
        mWatcher = watcher;
    }

    public void isEditSmall(boolean smallEdit) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mEditText.getLayoutParams();
        if (params != null) {
            int width = getResources().getDimensionPixelSize(R.dimen.search_view_size);
            if (smallEdit) {
                width -= mSearchIcon.getWidth();
            }
            params.width = width;
            mEditText.setLayoutParams(params);
            mEditText.invalidate();
        }
    }

    private void onClickButton(boolean startOpen) {
        if (startOpen) {
            mEditText.setText(null);
        } else {
            if (mCollapsed) {
                mSearchIcon.setImageDrawable(BitmapHelper.getAttrDrawable(getContext(), R.attr.clear_24dp));
                mEditText.setVisibility(VISIBLE);
                mEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
            } else {
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                mSearchIcon.setImageDrawable(BitmapHelper.getAttrDrawable(getContext(), R.attr.search_24dp));
                mEditText.setText(null);
                mEditText.setVisibility(GONE);
            }
            mCollapsed = !mCollapsed;
        }
    }

    public interface SearchTextWatcher {

        void onTextChanged(String text);

    }
}
