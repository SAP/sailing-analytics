package com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.impl;

import java.util.Arrays;
import java.util.Collections;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateEvent;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateEvents;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.FlagPoleState;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.SWCRacingProcedure;
import com.sap.sailing.domain.base.configuration.procedures.SWCStartConfiguration;
import com.sap.sailing.domain.common.racelog.FlagPole;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public class ShortSWCRacingProcedureImpl extends SWCRacingProcedureImpl implements SWCRacingProcedure {

    private final static Duration CLASS_AND_STARTMODE_UP_INTERVAL_4MIN = Duration.ONE_MINUTE.times(4); // 4 minutes before start

    public ShortSWCRacingProcedureImpl(RaceLog raceLog, AbstractLogEventAuthor author,
                                  SWCStartConfiguration configuration, RaceLogResolver raceLogResolver) {
        super(raceLog, author, configuration, raceLogResolver);
    }

    @Override
    protected Duration getStartPhaseStartModeUpInterval() {
        return CLASS_AND_STARTMODE_UP_INTERVAL_4MIN;
    }

    @Override
    public RacingProcedureType getType() {
        return RacingProcedureType.SWC_4MIN;
    }

    @Override
    public Iterable<RaceStateEvent> createStartStateEvents(TimePoint startTime) {
        return Arrays.<RaceStateEvent>asList(
                new RaceStateEventImpl(startTime.minus(CLASS_AND_STARTMODE_UP_INTERVAL_4MIN), RaceStateEvents.SWC_CLASS_AND_STARTMODE_UP),
                new RaceStateEventImpl(startTime.minus(THREE_MINUTES_FLAG_UP_INTERVAL), RaceStateEvents.SWC_THREE_UP),
                new RaceStateEventImpl(startTime.minus(TWO_MINUTES_FLAG_UP_INTERVAL), RaceStateEvents.SWC_TWO_UP),
                new RaceStateEventImpl(startTime.minus(ONE_MINUTE_FLAG_UP_INTERVAL), RaceStateEvents.SWC_ONE_UP),
                new RaceStateEventImpl(startTime, RaceStateEvents.START),
                new RaceStateEventImpl(startTime, RaceStateEvents.SWC_GREEN_UP),
                new RaceStateEventImpl(startTime.plus(CLASS_AND_STARTMODE_DOWN_INTERVAL), RaceStateEvents.SWC_CLASS_AND_STARTMODE_DOWN));
    }

    @Override
    public FlagPoleState getActiveFlags(TimePoint startTime, TimePoint now) {
        if (now.before(startTime.minus(CLASS_AND_STARTMODE_UP_INTERVAL_4MIN))) {
            return new FlagPoleState(
                    Collections.singletonList(new FlagPole(cachedStartmodeFlag, false)),
                    null,
                    Collections.singletonList(new FlagPole(cachedStartmodeFlag, true)),
                    startTime.minus(CLASS_AND_STARTMODE_UP_INTERVAL_4MIN));
            } else if (now.before(startTime.minus(THREE_MINUTES_FLAG_UP_INTERVAL))) {
                return new FlagPoleState(
                        Arrays.asList(new FlagPole(cachedStartmodeFlag, true), new FlagPole(Flags.SWC_THREE, false)),
                        startTime.minus(CLASS_AND_STARTMODE_UP_INTERVAL_4MIN),
                        Arrays.asList(new FlagPole(cachedStartmodeFlag, true), new FlagPole(Flags.SWC_THREE, true)),
                        startTime.minus(THREE_MINUTES_FLAG_UP_INTERVAL));
        } else {
            return super.getActiveFlags(startTime, now);
        }
    }
}
