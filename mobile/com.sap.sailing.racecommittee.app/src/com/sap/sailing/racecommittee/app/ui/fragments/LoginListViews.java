package com.sap.sailing.racecommittee.app.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sap.sailing.android.shared.util.AppUtils;
import com.sap.sailing.android.shared.util.ScreenHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.fragments.dialogs.LoggableDialogFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.lists.EventListFragment;

import java.util.ArrayList;
import java.util.List;

public class LoginListViews extends LoggableDialogFragment implements View.OnClickListener {

    private Button mSignUp;
    private IntentListener mListener;

    private ToggleContainer mEventContainer;
    private ToggleContainer mCourseAreaContainer;
    private ToggleContainer mPositionContainer;

    public LoginListViews() {
        mListener = new IntentListener();
    }

    public ToggleContainer getEventContainer() {
        return mEventContainer;
    }

    public ToggleContainer getCourseAreaContainer() {
        return mCourseAreaContainer;
    }

    public ToggleContainer getPositionContainer() {
        return mPositionContainer;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_listviews, container, false);

        View event_layout = ViewHelper.get(view, R.id.events);
        FrameLayout event_fragment = ViewHelper.get(view, R.id.event_fragment);
        RelativeLayout event_header = ViewHelper.get(view, R.id.event_header);
        TextView event_text = ViewHelper.get(view, R.id.selected_event);
        mEventContainer = new ToggleContainer(view, event_fragment, event_header, event_text, null);

        View area_layout = ViewHelper.get(view, R.id.areas);
        FrameLayout area_fragment = ViewHelper.get(view, R.id.area_fragment);
        RelativeLayout area_header = ViewHelper.get(view, R.id.area_header);
        TextView area_text = ViewHelper.get(view, R.id.selected_area);
        ArrayList<View> above_area = new ArrayList<>();
        above_area.add(event_layout);
        mCourseAreaContainer = new ToggleContainer(view, area_fragment, area_header, area_text, above_area);

        FrameLayout position_fragment = ViewHelper.get(view, R.id.position_fragment);
        RelativeLayout position_header = ViewHelper.get(view, R.id.position_header);
        TextView position_text = ViewHelper.get(view, R.id.selected_position);
        ArrayList<View> above_position = new ArrayList<>();
        above_position.add(event_layout);
        above_position.add(area_layout);
        mPositionContainer = new ToggleContainer(view, position_fragment, position_header, position_text,
                above_position);

        // add listeners to the click areas
        if (event_header != null) {
            event_header.setOnClickListener(this);
        }
        if (area_header != null) {
            area_header.setOnClickListener(this);
        }
        if (position_header != null) {
            position_header.setOnClickListener(this);
        }

        mSignUp = view.findViewById(R.id.login_submit);
        if (mSignUp != null) {
            mSignUp.setOnClickListener(this);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppConstants.ACTION_TOGGLE);
        filter.addAction(AppConstants.ACTION_RESET);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mListener, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mListener);
    }

    @Override
    public void onClick(View view) {

        final int id = view.getId();
        if (id == R.id.event_header) {
            mCourseAreaContainer.close();
            mPositionContainer.close();
            final boolean expanded = mEventContainer.toggle();
            final Fragment fragment = requireFragmentManager().findFragmentById(R.id.event_fragment);
            if (fragment instanceof EventListFragment) {
                if (expanded) {
                    ((EventListFragment) fragment).onExpanded();
                } else {
                    ((EventListFragment) fragment).onCollapsed();
                }
            }
        } else if (id == R.id.area_header) {
            mEventContainer.close();
            mPositionContainer.close();
            mCourseAreaContainer.toggle();
        } else if (id == R.id.position_header) {
            mEventContainer.close();
            mCourseAreaContainer.close();
            mPositionContainer.toggle();
        }

        showButton();
    }

    public void closeAll() {
        mEventContainer.close();
        mCourseAreaContainer.close();
        mPositionContainer.close();
        showButton();
    }

    private void showButton() {
        mSignUp.setVisibility(View.GONE);
        if (mEventContainer.isClosed() && mCourseAreaContainer.isClosed() && mPositionContainer.isClosed()) {
            mSignUp.setVisibility(View.VISIBLE);
        }
    }

    public class ToggleContainer {
        private View mRootView;
        private FrameLayout mFrame;
        private TextView mText;
        private RelativeLayout mHeader;
        private List<View> mLayouts;

        private AppUtils mAppUtils;

        public ToggleContainer(View rootView, FrameLayout frame, RelativeLayout header, TextView text,
                               List<View> layouts) {
            mRootView = rootView;
            mFrame = frame;
            mText = text;
            mHeader = header;
            mLayouts = layouts;

            mAppUtils = AppUtils.with(getActivity());
        }

        public boolean toggle() {
            final int[] pos = new int[2];

            if (mFrame != null && mFrame.getLayoutParams() != null) {
                // open the frame
                if (mFrame.getLayoutParams().height == 0) {
                    mFrame.getLocationOnScreen(pos);
                    if ((mAppUtils.isPhone() && mAppUtils.isLandscape())
                            || (mAppUtils.isPhone() && mAppUtils.isHDPI())) {
                        if (mLayouts != null) {
                            for (View view : mLayouts) {
                                setVisibility(view, View.GONE);
                            }
                        }
                        mRootView.getLocationOnScreen(pos);
                        pos[1] += mHeader.getMeasuredHeight();
                    }
                    mFrame.getLayoutParams().height = ScreenHelper.on(getActivity()).getScreenHeight() - pos[1];
                    mFrame.requestLayout();
                    setVisibility(mText, View.GONE);
                    return true;
                } else {
                    close();
                }
            }
            return false;
        }

        public void close() {
            if (mFrame != null && mFrame.getLayoutParams() != null) {
                if ((!mAppUtils.is10inch() && mAppUtils.isLandscape()) || mAppUtils.isPhone() && mAppUtils.isHDPI()) {
                    if (mLayouts != null) {
                        for (View view : mLayouts) {
                            setVisibility(view, View.VISIBLE);
                        }
                    }
                }
                mFrame.getLayoutParams().height = 0;
                mFrame.requestLayout();
                setVisibility(mText, View.VISIBLE);
            }
        }

        public boolean isClosed() {
            return (mFrame != null && mFrame.getLayoutParams() != null && mFrame.getLayoutParams().height == 0);
        }

        public RelativeLayout getHeader() {
            return this.mHeader;
        }

        public void setHeaderText(String header) {
            mText.setText(header);
        }

        private void setVisibility(View view, int visibility) {
            if (view != null) {
                view.setVisibility(visibility);
            }
        }
    }

    private class IntentListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case AppConstants.ACTION_TOGGLE:
                    String data = intent.getStringExtra(AppConstants.EXTRA_DEFAULT);
                    if (AppConstants.ACTION_TOGGLE_EVENT.equals(data)) {
                        onClick(mEventContainer.getHeader());
                    }
                    if (AppConstants.ACTION_TOGGLE_AREA.equals(data)) {
                        onClick(mCourseAreaContainer.getHeader());
                    }
                    if (AppConstants.ACTION_TOGGLE_POSITION.equals(data)) {
                        onClick(mPositionContainer.getHeader());
                    }
                    break;

                case AppConstants.ACTION_RESET:
                    mSignUp.setEnabled(false);
                    break;

                default:
                    break;
            }
        }
    }
}
