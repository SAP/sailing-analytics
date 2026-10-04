package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo;


import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.impl.Result;
import com.sap.sailing.racecommittee.app.ui.utils.FlagsResources;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceFinishingFragment extends BaseFragment {

    private TextView mFinishingSince;

    public static RaceFinishingFragment newInstance() {
        RaceFinishingFragment fragment = new RaceFinishingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_finishing, container, false);

        mFinishingSince = ViewHelper.get(layout, R.id.first_vessel_since);

        ImageView flag = ViewHelper.get(layout, R.id.flag);
        if (flag != null) {
            flag.setImageDrawable(FlagsResources.getFlagDrawable(getActivity(), Flags.BLUE.name(),
                    getResources().getInteger(R.integer.flag_size_xlarge)));
        }

        Button down = ViewHelper.get(layout, R.id.flag_down);

        if (down != null) {
            down.setOnClickListener(view -> replaceFragment(MoreFlagsFragment.FinishTimeFragment.newInstance(1),
                    getFrameId(requireActivity(), R.id.race_edit, R.id.race_content, false)));
        }
        Button revoke = ViewHelper.get(layout, R.id.flag_finishing_revoke);
        if (revoke != null) {
            revoke.setOnClickListener(view -> {
                final Result result = getRace().revokeFinishing(preferences.getAuthor());
                if (result.hasError()) {
                    Toast.makeText(getActivity(), result.getMessage(requireContext()), Toast.LENGTH_LONG).show();
                }
            });
        }
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getView() != null) {
            TextView first_vessel = ViewHelper.get(getView(), R.id.first_vessel);
            if (first_vessel != null) {
                if (getRaceState().getFinishingTime() != null) {
                    first_vessel.setText(getString(R.string.finishing_started,
                            TimeUtils.formatTime(getRaceState().getFinishingTime())));
                }
            }
        }
        onCurrentTimeTick(MillisecondsTimePoint.now());
    }

    @Override
    public TickListener getCurrentTimeTickListener() {
        return this::onCurrentTimeTick;
    }

    private void onCurrentTimeTick(TimePoint now) {
        if (mFinishingSince != null && getRaceState().getFinishingTime() != null) {
            String timeDiff = TimeUtils.formatTimeAgo(getActivity(),
                    now.minus(getRaceState().getFinishingTime().asMillis()).asMillis());
            mFinishingSince.setText(getString(R.string.finishing_started_since, timeDiff));
        }
    }
}
