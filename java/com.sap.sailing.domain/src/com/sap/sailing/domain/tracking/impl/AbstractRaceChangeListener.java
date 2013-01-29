package com.sap.sailing.domain.tracking.impl;

import java.util.Map;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LifecycleState;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.Wind;

public abstract class AbstractRaceChangeListener implements RaceChangeListener {

    @Override
    public void statusChanged(LifecycleState newStatus) {}

    @Override
    public void windSourcesToExcludeChanged(Iterable<? extends WindSource> windSourcesToExclude) {
    }

    @Override
    public void raceTimesChanged(TimePoint startOfTracking, TimePoint endOfTracking, TimePoint startTimeReceived) {
    }

    @Override
    public void markPositionChanged(GPSFix fix, Mark mark) {
    }

    @Override
    public void windDataReceived(Wind wind, WindSource windSource) {
    }

    @Override
    public void windDataRemoved(Wind wind, WindSource windSource) {
    }

    @Override
    public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
    }

    @Override
    public void competitorPositionChanged(GPSFixMoving fix, Competitor item) {
    }

    @Override
    public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings, Iterable<MarkPassing> markPassings) {
    }

    @Override
    public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
    }

    @Override
    public void delayToLiveChanged(long delayToLiveInMillis) {
    }

}
