package com.sap.sailing.domain.racelog.state.impl;

import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.configuration.ConfigurationLoader;
import com.sap.sailing.domain.base.configuration.impl.EmptyRegattaConfiguration;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.racelog.CompetitorResults;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventFactory;
import com.sap.sailing.domain.racelog.analyzing.impl.RaceStatusAnalyzer;
import com.sap.sailing.domain.racelog.state.RaceState;
import com.sap.sailing.domain.racelog.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.racelog.state.racingprocedure.RacingProcedureFactory;
import com.sap.sailing.domain.racelog.state.racingprocedure.RacingProcedurePrerequisite;
import com.sap.sailing.domain.racelog.state.racingprocedure.impl.RacingProcedureFactoryImpl;
import com.sap.sailing.domain.tracking.Wind;

public class RaceStateImpl extends ReadonlyRaceStateImpl implements RaceState {
    
    private final RaceLogEventAuthor author;
    private final RaceLogEventFactory factory;
    
    /**
     * Creates a {@link RaceState} with the initial racing procedure type set to a fallback value and an empty configuration.
     */
    public static RaceState create(RaceLog raceLog, RaceLogEventAuthor author) {
        return create(raceLog, author, RaceStateImpl.FallbackInitialProcedureType, new EmptyRegattaConfiguration());
    }
    
    /**
     * Creates a {@link RaceState} with the initial racing procedure type set to a fallback value.
     */
    public static RaceState create(RaceLog raceLog, RaceLogEventAuthor author, ConfigurationLoader<RegattaConfiguration> configuration) {
        return create(raceLog, author, RaceStateImpl.FallbackInitialProcedureType, configuration);
    }
    
    /**
     * Creates a {@link RaceState}.
     */
    public static RaceState create(RaceLog raceLog, RaceLogEventAuthor author, RacingProcedureType initalRacingProcedureType,
            ConfigurationLoader<RegattaConfiguration> configuration) {
        return new RaceStateImpl(raceLog, author, 
                RaceLogEventFactory.INSTANCE,
                initalRacingProcedureType, 
                new RacingProcedureFactoryImpl(author, RaceLogEventFactory.INSTANCE, configuration));
    }
    
    public RaceStateImpl(RaceLog raceLog, RaceLogEventAuthor author, RaceLogEventFactory eventFactory,
            RacingProcedureType initalRacingProcedureType, RacingProcedureFactory procedureFactory) {
        this(raceLog, author, eventFactory, initalRacingProcedureType, 
                new RaceStatusAnalyzer.StandardClock(), procedureFactory);
    }

    public RaceStateImpl(RaceLog raceLog, RaceLogEventAuthor author, RaceLogEventFactory eventFactory,
            RacingProcedureType initalRacingProcedureType, RaceStatusAnalyzer.Clock analyzersClock,
            RacingProcedureFactory procedureFactory) {
        super(raceLog, initalRacingProcedureType, analyzersClock, procedureFactory);
        this.author = author;
        this.factory = eventFactory;
    }
    
    @Override
    public RacingProcedure getRacingProcedure() {
        return (RacingProcedure) super.getRacingProcedure();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RacingProcedure> T getTypedRacingProcedure() {
        return (T) getRacingProcedure();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RacingProcedure> T getTypedRacingProcedure(Class<T> clazz) {
        RacingProcedure procedure = getRacingProcedure();
        if (clazz.isAssignableFrom(procedure.getClass())) {
            return (T) procedure;
        } else {
            return null;
        }
    }

    @Override
    public RaceLogEventAuthor getAuthor() {
        return author;
    }

    @Override
    public void setRacingProcedure(TimePoint timePoint, RacingProcedureType newType) {
        raceLog.add(factory.createStartProcedureChangedEvent(timePoint, author, raceLog.getCurrentPassId(), newType));
    }

    @Override
    public void requestStartTime(final TimePoint now, final TimePoint startTime, RacingProcedurePrerequisite.Resolver resolver) {
        RacingProcedurePrerequisite.FulfillmentFunction function = new RacingProcedurePrerequisite.FulfillmentFunction() {
            @Override
            public void execute() {
                raceLog.add(factory.createStartTimeEvent(now, author, raceLog.getCurrentPassId(), startTime));
            }
        };
        
        getRacingProcedure().checkPrerequisitesForStart(now, startTime, function).resolve(resolver);
    }
    
    @Override
    public void forceStartTime(TimePoint now, TimePoint startTime) {
        raceLog.add(factory.createStartTimeEvent(now, author, raceLog.getCurrentPassId(), startTime));
    }

    @Override
    public void setFinishingTime(TimePoint timePoint) {
        raceLog.add(factory.createRaceStatusEvent(timePoint, author, raceLog.getCurrentPassId(), RaceLogRaceStatus.FINISHING));
    }

    @Override
    public void setFinishedTime(TimePoint timePoint) {
        raceLog.add(factory.createRaceStatusEvent(timePoint, author, raceLog.getCurrentPassId(), RaceLogRaceStatus.FINISHED));
    }

    @Override
    public void setProtestTime(TimePoint now, TimePoint timePoint) {
        raceLog.add(factory.createProtestStartTimeEvent(now, author, raceLog.getCurrentPassId(), timePoint));
    }

    @Override
    public void setAdvancePass(TimePoint timePoint) {
        raceLog.add(factory.createPassChangeEvent(timePoint, author, raceLog.getCurrentPassId() + 1));
    }

    @Override
    public void setAborted(TimePoint timePoint, boolean isPostponed, Flags reasonFlag) {
        Flags markerFlag = isPostponed ? Flags.AP : Flags.NOVEMBER;
        raceLog.add(factory.createFlagEvent(timePoint, author, raceLog.getCurrentPassId(), markerFlag, reasonFlag, true));
        setAdvancePass(timePoint.plus(1));
    }

    @Override
    public void setGeneralRecall(TimePoint timePoint) {
        raceLog.add(factory.createFlagEvent(timePoint, author, raceLog.getCurrentPassId(), Flags.FIRSTSUBSTITUTE, Flags.NONE, true));
        setAdvancePass(timePoint);
    }

    @Override
    public void setFinishPositioningListChanged(TimePoint timePoint, CompetitorResults positionedCompetitors) {
        raceLog.add(factory.createFinishPositioningListChangedEvent(
                timePoint, author, raceLog.getCurrentPassId(), positionedCompetitors));
    }

    @Override
    public void setFinishPositioningConfirmed(TimePoint timePoint) {
        raceLog.add(factory.createFinishPositioningConfirmedEvent(
                timePoint, author, raceLog.getCurrentPassId(), getFinishPositioningList()));
    }

    @Override
    public void setCourseDesign(TimePoint timePoint, CourseBase courseDesign) {
        raceLog.add(factory.createCourseDesignChangedEvent(timePoint, author, raceLog.getCurrentPassId(), courseDesign));
    }

    @Override
    public void setWindFix(TimePoint timePoint, Wind wind) {
        raceLog.add(factory.createWindFixEvent(timePoint, author, raceLog.getCurrentPassId(), wind));
    }

}
