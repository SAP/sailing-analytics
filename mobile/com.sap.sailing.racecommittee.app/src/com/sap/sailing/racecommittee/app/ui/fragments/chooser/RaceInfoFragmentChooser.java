package com.sap.sailing.racecommittee.app.ui.fragments.chooser;

import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.logging.ExLog;
import com.sap.sailing.racecommittee.app.ui.fragments.RaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.ErrorRaceFragment;
import com.sap.sailing.racecommittee.app.ui.fragments.raceinfo.SetStartTimeRaceFragment;

public abstract class RaceInfoFragmentChooser {
    private static final String TAG = RaceInfoFragmentChooser.class.getName();
    
    public static RaceInfoFragmentChooser on(RacingProcedureType racingProcedureType) {
        switch (racingProcedureType) {
        case RRS26:
            return new RRS26RaceInfoFragmentChooser();
        case GateStart:
            return new GateStartRaceInfoFragmentChooser();
        case ESS:
            return new ESSRaceInfoFragmentChooser();
        default:
            throw new UnsupportedOperationException("");
        }
    }
    
    protected abstract Class<? extends RaceFragment> getStartphaseFragment();
    protected abstract Class<? extends RaceFragment> getRunningFragment();
    protected abstract Class<? extends RaceFragment> getFinishingFragment();
    protected abstract Class<? extends RaceFragment> getFinishedFragment();

    public RaceFragment choose(ManagedRace managedRace) {
        switch (managedRace.getStatus()) {
        case UNSCHEDULED:
            return createInfoFragment(SetStartTimeRaceFragment.class, managedRace);
        case SCHEDULED:
        case STARTPHASE:
            return createInfoFragment(getStartphaseFragment(), managedRace);
        case RUNNING:
            return createInfoFragment(getRunningFragment(), managedRace);
        case FINISHING:
            return createInfoFragment(getFinishingFragment(), managedRace);
        case FINISHED:
            return createInfoFragment(getFinishedFragment(), managedRace);
        default:
            return createInfoFragment(ErrorRaceFragment.class, managedRace);
        }
    }
    

    protected RaceFragment createInfoFragment(Class<? extends RaceFragment> fragmentClass, ManagedRace managedRace) {
        try {
            RaceFragment fragment = fragmentClass.newInstance();
            fragment.setArguments(RaceFragment.createArguments(managedRace));
            return fragment;
        } catch (Exception e) {
            ExLog.e(TAG, String.format("Exception while instantiating race info fragment:\n%s", e.toString()));
            return new ErrorRaceFragment();
        }
    }

}
