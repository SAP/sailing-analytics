package com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.startphase;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sap.sailing.android.shared.util.ViewHelper;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.ui.adapters.PanelsAdapter;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.BaseRaceInfoRaceFragment;
import com.sap.sailing.racecommittee.app.utils.BitmapHelper;
import com.sap.sailing.racecommittee.app.utils.ThemeHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public abstract class BaseStartphaseRaceFragment<ProcedureType extends RacingProcedure> extends BaseRaceInfoRaceFragment<ProcedureType> {

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

        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ViewPager pager = ViewHelper.get(getView(), R.id.panels_pager);
        if (pager != null) {
            pager.setAdapter(new PanelsAdapter(getFragmentManager(), getArguments()));
            pager.addOnPageChangeListener(new ViewPagerChangeListener(this));
            markDot(0);
        }
    }

    @Override
    protected void setupUi() {
    }

    private void markDot(int position) {
        // tint all dots gray
        for (ImageView mDot : mDots) {
            int tint = ThemeHelper.getColor(getActivity(), R.attr.sap_light_gray);
            Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
            mDot.setImageDrawable(drawable);
        }

        int tint = ThemeHelper.getColor(getActivity(), R.attr.black);
        Drawable drawable = BitmapHelper.getTintedDrawable(getActivity(), R.drawable.ic_dot, tint);
        mDots.get(position).setImageDrawable(drawable);
    }

    private static class ViewPagerChangeListener implements ViewPager.OnPageChangeListener {

        private WeakReference<BaseStartphaseRaceFragment> reference;

        public ViewPagerChangeListener(BaseStartphaseRaceFragment fragment) {
            reference = new WeakReference<>(fragment);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            BaseStartphaseRaceFragment fragment = reference.get();
            if (fragment != null) {
                fragment.markDot(position);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }
}
