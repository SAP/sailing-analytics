package com.sap.sailing.domain.abstractlog.race.state.racingprocedure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.common.racelog.FlagPole;
import com.sap.sse.common.TimePoint;

/**
 * Allows you to query the current state of displayed and removed flags.
 */
public class FlagPoleState {
    
    private final List<FlagPole> currentState;
    private final List<FlagPole> nextState;
    private final TimePoint currentStateValidFrom;
    private final TimePoint nextStateValidFrom;
    private final boolean hasNextState;
    
    public FlagPoleState(List<FlagPole> currentState, TimePoint currentStateValidFrom) {
        this(currentState, currentStateValidFrom, null, null, false);
    }
    
    public FlagPoleState(List<FlagPole> currentState, TimePoint currentStateValidFrom,
            List<FlagPole> nextState, TimePoint nextStateValidFrom) {
        this(currentState, currentStateValidFrom, nextState, nextStateValidFrom, true);
    }
    
    private FlagPoleState(List<FlagPole> currentState, TimePoint currentStateValidFrom,
            List<FlagPole> nextState, TimePoint nextStateValidFrom, boolean hasNextState) {
        this.currentState = currentState;
        this.currentStateValidFrom = currentStateValidFrom;
        this.nextState = nextState;
        this.nextStateValidFrom = nextStateValidFrom;
        this.hasNextState = hasNextState;
    }
    
    public List<FlagPole> getCurrentState() {
        return currentState;
    }
    public List<FlagPole> getNextState() {
        return nextState;
    }

    public TimePoint getNextStateValidFrom() {
        return nextStateValidFrom;
    }
    
    public TimePoint getCurrentStateValidFrom() {
        return currentStateValidFrom;
    }

    public boolean hasNextState() {
        return hasNextState;
    }
    
    public static List<FlagPole> computeChanges(List<FlagPole> before, List<FlagPole> after) {
        before = before == null ? Collections.<FlagPole>emptyList() : before;
        after = after == null ? Collections.<FlagPole>emptyList() : after;
        
        List<FlagPole> beforeWithoutMatchInAfter = new ArrayList<FlagPole>(before);
        List<FlagPole> changes = new ArrayList<FlagPole>();
        for (FlagPole poleAfter : after) {
            boolean hasSamePoleBefore = false;
            for (FlagPole poleBefore : before) {
                if (poleAfter.describesSameButForDisplayed(poleBefore)) {
                    beforeWithoutMatchInAfter.remove(poleBefore);
                }
                if (poleAfter.describesSame(poleBefore)) {
                    hasSamePoleBefore = true;
                    break;
                }
            }
            if (!hasSamePoleBefore) {
                changes.add(poleAfter);
            }
        }

        for (FlagPole poleBefore : beforeWithoutMatchInAfter) {
            if (poleBefore.isDisplayed()) {
                changes.add(new FlagPole(poleBefore.getUpperFlag(), poleBefore.getLowerFlag(), false));
            }
        }
        return changes;
    }
    
    public List<FlagPole> computeUpcomingChanges() {
        if (!hasNextState) {
            return Collections.emptyList();
        }
        
        return computeChanges(currentState, nextState);
    }

    public static boolean describesSameState(FlagPoleState left, FlagPoleState right) {
        if (left.hasNextState() != right.hasNextState()) {
            return false;
        }
        
        if (left.getCurrentState().size() != right.getCurrentState().size()) {
            return false;
        }
        
        if (describesSamePoles(left.getCurrentState(), right.getCurrentState())) {
            if (left.hasNextState()) {
                
                return left.getNextStateValidFrom().compareTo(right.getNextStateValidFrom()) == 0 &&
                        describesSamePoles(left.getNextState(), right.getNextState());
            }
            return true;
        }
        return false;
    }

    private static boolean describesSamePoles(List<FlagPole> leftPoles, List<FlagPole> rightPoles) {
        for (int i = 0; i < leftPoles.size(); i++) {
            FlagPole leftPole = leftPoles.get(i);
            FlagPole rightPole = rightPoles.get(i);
            if (!leftPole.describesSame(rightPole)) {
                return false;
            }
        }
        return true;
    }
    
    public FlagPoleState getPreviousState(ReadonlyRacingProcedure racingProcedure, TimePoint startTime) {
        if (currentStateValidFrom == null) {
            return this;
        }
        return racingProcedure.getActiveFlags(startTime, currentStateValidFrom.minusResolution());
    }
    
    public static FlagPole getMostInterestingFlagPole(List<FlagPole> poles) {
        return getMostInterestingFlagPole(poles, poles);
    }
    
    public static FlagPole getMostInterestingFlagPole(List<FlagPole> previousPoles, List<FlagPole> currentPoles) {
        List<FlagPole> changes = computeChanges(previousPoles, currentPoles);
        FlagPole poleRemoved = null;
        for (FlagPole changedPole : changes) {
            if (changedPole.isDisplayed()) {
                return changedPole;
            } else {
                poleRemoved = changedPole;
            }
        }
        if (poleRemoved != null) {
            return poleRemoved;
        }
        
        for (FlagPole currentPole : currentPoles) {
            if (currentPole.isDisplayed()) {
                return currentPole;
            }
        }
        return currentPoles.size() == 0 ? null : currentPoles.get(0);
    }
    
    public boolean hasPoleChanged(FlagPole newPole) {
        boolean found = false;
        for (FlagPole current : currentState) {
            if (current.describesSameButForDisplayed(newPole)) {
                found = true;
                if (current.isDisplayed() != newPole.isDisplayed()) {
                    return true;
                }
            }
        }
        if (! found && newPole.isDisplayed()) {
            return true;
        }
        return false;
    }
}
