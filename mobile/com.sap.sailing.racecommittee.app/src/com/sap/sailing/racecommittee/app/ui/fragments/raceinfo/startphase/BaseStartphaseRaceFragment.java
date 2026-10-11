package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.startphase;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sap.sailing.android.shared.util.BitmapHelper;
import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.PanelsAdapter;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.BaseRaceInfoRaceFragment;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public abstract class BaseStartphaseRaceFragment<ProcedureType extends RacingProcedure>
        extends BaseRaceInfoRaceFragment<ProcedureType> {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.race_main, container, false);

        mDots = new ArrayList<>();

        ImageView dot;
        dot = ViewHelper.get(layout, R.id.dot_1);
        if (dot != null) {
            mDots.add(dot);
        }
        dot = ViewHelper.get(layout, R.id.dot_2);
        if (dot != null) {
            mDots.add(dot);
        }
        dot = ViewHelper.get(layout, R.id.dot_3);
        if (dot != null) {
            mDots.add(dot);
        }

        final ViewPager pager = ViewHelper.get(layout, R.id.panels_pager);
        if (pager != null) {
            final PanelsAdapter adapter = new PanelsAdapter(requireFragmentManager(), getArguments());
            pager.setAdapter(adapter);
            pager.addOnPageChangeListener(new ViewPagerChangeListener(this));
            markDot(0);
        }

        ImageView nav_prev = ViewHelper.get(layout, R.id.nav_prev);
        if (nav_prev != null) {
            nav_prev.setOnClickListener(v -> {
                viewPanel(MOVE_DOWN);
                if (pager != null) {
                    pager.setCurrentItem(mActivePage);
                }
            });
        }

        ImageView nav_next = ViewHelper.get(layout, R.id.nav_next);
        if (nav_next != null) {
            nav_next.setOnClickListener(view -> {
                viewPanel(MOVE_UP);
                if (pager != null) {
                    pager.setCurrentItem(mActivePage);
                }
            });
        }

        return layout;
    }

    private void markDot(int position) {
        // tint all dots gray
        for (ImageView mDot : mDots) {
            int tint = ThemeHelper.getColor(requireContext(), R.attr.sap_light_gray);
            Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
            mDot.setImageDrawable(drawable);
        }

        int tint = ThemeHelper.getColor(requireContext(), R.attr.black);
        Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
        mDots.get(position).setImageDrawable(drawable);
    }

    private static class ViewPagerChangeListener implements ViewPager.OnPageChangeListener {

        private WeakReference<BaseStartphaseRaceFragment<?>> reference;

        public ViewPagerChangeListener(BaseStartphaseRaceFragment<?> fragment) {
            reference = new WeakReference<>(fragment);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            BaseStartphaseRaceFragment<?> fragment = reference.get();
            if (fragment != null) {
                fragment.markDot(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
