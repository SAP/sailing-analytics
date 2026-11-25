package com.sap.sailing.racecommittee.app.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.BroadcastManager;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.OnlineDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.ui.NavigationEvents;
import com.sap.sailing.racecommittee.app.ui.activities.BaseActivity;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TickSingleton;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public abstract class RaceFragment extends LoggableFragment {

    @IntDef({MOVE_DOWN, MOVE_NONE, MOVE_UP})
    @Retention(RetentionPolicy.SOURCE)
    @interface MOVE_VALUES {
    }

    private static final String TAG = RaceFragment.class.getSimpleName();

    protected ManagedRace managedRace;
    protected AppPreferences preferences;

    protected ArrayList<ImageView> mDots;
    protected ArrayList<View> mPanels;
    protected int mActivePage = 0;

    protected final static int MOVE_DOWN = -1;
    protected final static int MOVE_NONE = 0;
    protected final static int MOVE_UP = 1;

    private final TickListener currentTimeListener = getCurrentTimeTickListener();
    private final TickListener startTimeListener = getStartTimeTickListener();

    private final RaceStateChangedListener stateChangedListener = new BaseRaceStateChangedListener() {
        @Override
        public void onStartTimeChanged(ReadonlyRaceState state) {
            unregisterTickListeners();
            registerTickListeners();
        }
    };

    public static Bundle createArguments(ManagedRace race) {
        Bundle arguments = new Bundle();
        arguments.putString(AppConstants.EXTRA_RACE_ID, race.getId());
        return arguments;
    }

    @NonNull
    public ManagedRace getRace() {
        return managedRace;
    }

    @NonNull
    public RaceState getRaceState() {
        return managedRace.getState();
    }

    public TimePoint getStartTime() {
        return managedRace.getState().getStartTime();
    }

    /**
     * Creates a bundle that contains the race id as parameter for the next fragment
     *
     * @return a bundle containing the race id
     */
    protected Bundle getRecentArguments() {
        Bundle args = new Bundle();
        args.putString(AppConstants.EXTRA_RACE_ID, managedRace.getId());
        return args;
    }

    @Nullable
    public TickListener getCurrentTimeTickListener() {
        //See subclassess
        return null;
    }

    @Nullable
    public TickListener getStartTimeTickListener() {
        //See subclassess
        return null;
    }

    /**
     * Registers a {@link TickListener} for the start time.
     */
    public void registerTickListeners() {
        //Register the {@link TickListener} for the current time without millis
        if (currentTimeListener != null) {
            TickSingleton.INSTANCE.registerListener(currentTimeListener);
        }
        if (startTimeListener != null) {
            final TimePoint startTime = getStartTime();
            if (startTime != null) {
                //Register the {@link TickListener} for the start time
                TickSingleton.INSTANCE.registerListener(startTimeListener, startTime);
            }
        }
    }

    public void unregisterTickListeners() {
        if (currentTimeListener != null) {
            TickSingleton.INSTANCE.unregisterListener(currentTimeListener);
        }
        if (startTimeListener != null) {
            TickSingleton.INSTANCE.unregisterListener(startTimeListener);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null) {
            String raceId = getArguments().getString(AppConstants.EXTRA_RACE_ID);
            managedRace = OnlineDataManager.create(getActivity()).getDataStore().getRace(raceId);
            if (managedRace == null) {
                throw new IllegalStateException("Unable to obtain ManagedRace " + raceId
                        + " from datastore on start of " + getClass().getName());
            }
        } else {
            ExLog.i(getActivity(), TAG, "no arguments!?");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerTickListeners();
        getRaceState().addChangedListener(stateChangedListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterTickListeners();
        getRaceState().removeChangedListener(stateChangedListener);
    }

    protected void sendIntent(String action) {
        sendIntent(action, null, null);
    }

    protected void sendIntent(String action, String extra_key, String extra_value) {
        if (action != null) {
            Intent intent = new Intent(action);
            if (extra_key != null) {
                intent.putExtra(extra_key, extra_value);
            }
            sendIntent(intent);
        }
    }

    protected void sendIntent(Intent intent) {
        BroadcastManager.getInstance(getActivity()).addIntent(intent);
    }

    protected String getCourseName() {
        CourseBase courseDesign = getRaceState().getCourseDesign();
        if (courseDesign != null) {
            if (Util.isEmpty(courseDesign.getWaypoints())) {
                return courseDesign.getName();
            } else {
                return String.format(getString(R.string.course_design_number_waypoints),
                        Util.size(courseDesign.getWaypoints()));
            }
        } else {
            return getString(R.string.no_course_active);
        }
    }

    @IdRes
    protected static int getFrameId(Activity activity, @IdRes int defaultFrame, @IdRes int fallbackFrame, boolean changeVisibility) {
        int frame = 0;
        View view = activity.findViewById(defaultFrame);
        if (view != null) {
            if (changeVisibility) {
                ViewHelper.setSiblingsVisibility(view, View.GONE);
            }
            frame = defaultFrame;
        } else if (activity.findViewById(fallbackFrame) != null) {
            frame = fallbackFrame;
        }
        return frame;
    }

    protected void viewPanel(@MOVE_VALUES int direction) {
        if (mDots.size() == 0 || (mPanels != null && mPanels.size() == 0)) {
            return;
        }

        // find next active page (with overflow)
        mActivePage += direction;
        if (mActivePage < 0) {
            mActivePage = mDots.size() - 1;
        }
        if (mActivePage == mDots.size()) {
            mActivePage = 0;
        }

        // ignore invisible dots
        if (mDots.get(mActivePage).getVisibility() == View.GONE) {
            viewPanel(direction);
        }

        // tint all dots gray
        for (ImageView mDot : mDots) {
            int tint = ThemeHelper.getColor(requireContext(), R.attr.sap_light_gray);
            Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
            mDot.setImageDrawable(drawable);
        }

        // tint current dot black
        int tint = ThemeHelper.getColor(requireContext(), R.attr.black);
        Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
        mDots.get(mActivePage).setImageDrawable(drawable);

        // hide all panels
        if (mPanels != null) {
            for (View view : mPanels) {
                view.setVisibility(View.GONE);
            }

            // show current panel
            mPanels.get(mActivePage).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ExLog.i(getActivity(), TAG, "attach fragment " + this.getClass().getSimpleName());
        NavigationEvents.INSTANCE.attach(this);

        if (context instanceof BaseActivity) {
            BaseActivity baseActivity = (BaseActivity) context;
            preferences = baseActivity.getPreferences();
        } else {
            preferences = AppPreferences.on(context);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ExLog.i(getActivity(), TAG, "detach fragment " + this.getClass().getSimpleName());
        NavigationEvents.INSTANCE.detach(this);
    }
}
