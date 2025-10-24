package com.sap.sailing.domain.racelog.state.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartProcedureChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.gate.GateStartRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.RacingProcedureFactoryImpl;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.configuration.ConfigurationLoader;
import com.sap.sailing.domain.base.configuration.RacingProcedureConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.configuration.impl.EmptyRegattaConfiguration;
import com.sap.sailing.domain.base.configuration.impl.RegattaConfigurationImpl;
import com.sap.sailing.domain.base.configuration.procedures.ESSConfiguration;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceStateTest {
    
    private RaceLog raceLog;
    private AbstractLogEventAuthor author;
    private RacingProcedureType defaultRacingProcedureType;
    private ConfigurationLoader<RegattaConfiguration> configuration;
    private RaceStateChangedListener listener;
    private TimePoint nowMock;
    
    private RaceState state;
    
    @BeforeEach
    public void setUp() {
        raceLog = new RaceLogImpl("test-log");
        author = new LogEventAuthorImpl("Test", 1);
        defaultRacingProcedureType = RacingProcedureType.RRS26;
        configuration = new EmptyRegattaConfiguration();
        listener = mock(RaceStateChangedListener.class);
        nowMock = mock(TimePoint.class);
        
        state = new RaceStateImpl(mock(RaceLogResolver.class), raceLog, author, new RacingProcedureFactoryImpl(author, configuration));
    }
    
    @Test
    public void testStateOnEmptyRaceLog() {
        assertNull(state.getCourseDesign());
        assertNull(state.getFinishedTime());
        assertNull(state.getFinishingTime());
        assertNull(state.getFinishPositioningList());
        assertNull(state.getProtestTime());
        assertNull(state.getStartTime());
        assertNotNull(state.getRacingProcedure());
        assertEquals(defaultRacingProcedureType, state.getRacingProcedure().getType());
        assertEquals(RaceLogRaceStatus.UNSCHEDULED, state.getStatus());
        assertNull(state.getConfirmedFinishPositioningList().getCompetitorResults());
    }
    
    @Test
    public void testInitialProcedureTypeFallback() throws Exception {
        assertEquals(ReadonlyRaceStateImpl.fallbackInitialProcedureType, state.getRacingProcedure().getType());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testInitialProcedureTypeConfiguration() throws Exception {
        RegattaConfigurationImpl config = new RegattaConfigurationImpl();
        config.setDefaultRacingProcedureType(RacingProcedureType.BASIC);
        config.setBasicConfiguration(mock(RacingProcedureConfiguration.class));
        configuration = mock(ConfigurationLoader.class);
        when(configuration.load()).thenReturn(config);
        state = new RaceStateImpl(mock(RaceLogResolver.class), raceLog, author, new RacingProcedureFactoryImpl(author, configuration));
        
        assertEquals(RacingProcedureType.BASIC, state.getRacingProcedure().getType());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testInitialProcedureTypeRaceLog() throws Exception {
        RegattaConfigurationImpl config = new RegattaConfigurationImpl();
        config.setDefaultRacingProcedureType(RacingProcedureType.BASIC);
        config.setBasicConfiguration(mock(RacingProcedureConfiguration.class));
        config.setESSConfiguration(mock(ESSConfiguration.class));
        configuration = mock(ConfigurationLoader.class);
        when(configuration.load()).thenReturn(config);
        raceLog.add(new RaceLogStartProcedureChangedEventImpl(nowMock, nowMock, author, "12", 0, RacingProcedureType.ESS));
        state = new RaceStateImpl(mock(RaceLogResolver.class), raceLog, author, new RacingProcedureFactoryImpl(author, configuration));
        
        assertEquals(RacingProcedureType.ESS, state.getRacingProcedure().getType());
    }
    
    @Test
    public void testStartTime() {
        state.addChangedListener(listener);
        TimePoint startTime = MillisecondsTimePoint.now().plus(60 * 60 * 1000);
        state.forceNewStartTime(nowMock, startTime, /* courseAreaId */ null);
        assertEquals(startTime, state.getStartTime());
        assertEquals(RaceLogRaceStatus.SCHEDULED, state.getStatus());
        verify(listener).onStartTimeChanged(state);
        verify(listener).onStatusChanged(state);
        verifyNoMoreInteractions(listener);
    }

    /**
     * See bug 3086: make sure that race log change events cause an update of the RacingProcedure
     */
    @Test
    public void testSetIndividualRecall() {
        assertFalse(state.getRacingProcedure().isIndividualRecallDisplayed());
        raceLog.add(new RaceLogFlagEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(), author, UUID
                .randomUUID(), /* pass */0, Flags.XRAY, Flags.NONE, /* pIsDisplayed */true));
        assertTrue(state.getRacingProcedure().isIndividualRecallDisplayed());
    }

    @Test
    public void testAdvancePass() throws InterruptedException {
        state.addChangedListener(listener);
        int oldPassId = raceLog.getCurrentPassId();
        state.setAdvancePass(mock(TimePoint.class));
        assertEquals(oldPassId + 1, raceLog.getCurrentPassId());
        verify(listener).onAdvancePass(state);
        verifyNoMoreInteractions(listener);
    }
    
    @Test
    public void testAbort() throws InterruptedException {
        state.addChangedListener(listener);
        state.setAborted(mock(TimePoint.class), false, Flags.NONE);
        // TODO: change test when interface is complete
        verifyNoMoreInteractions(listener);
    }
    
    @Test
    public void testGeneralRecall() throws InterruptedException {
        state.addChangedListener(listener);
        state.setGeneralRecall(mock(TimePoint.class));
        // TODO: change test when interface is complete
        verifyNoMoreInteractions(listener);
    }
    
    @Test
    public void testInvalidateAfterAdvancePass() throws InterruptedException {
        state.addChangedListener(listener);
        state.forceNewStartTime(nowMock, new MillisecondsTimePoint(1), /* courseAreaId */ null);
        Thread.sleep(100);
        state.setFinishedTime(new MillisecondsTimePoint(10));
        state.setCourseDesign(nowMock, mock(CourseBase.class), CourseDesignerMode.ADMIN_CONSOLE);
        state.setAdvancePass(mock(TimePoint.class));
        assertNull(state.getStartTime());
        assertEquals(RaceLogRaceStatus.UNSCHEDULED, state.getStatus());
        verify(listener, times(1)).onAdvancePass(state);
        verify(listener, times(2)).onStartTimeChanged(state);
        verify(listener, times(2)).onFinishedTimeChanged(state);
        verify(listener, times(3)).onStatusChanged(state);
        verify(listener, times(1)).onCourseDesignChanged(state);
        verifyNoMoreInteractions(listener);
    }
    
    @Test
    public void testNewRacingProcedure() throws InterruptedException {
        state.addChangedListener(listener);
        
        RacingProcedure oldProcedure = state.getRacingProcedure();
        assertEquals(defaultRacingProcedureType, oldProcedure.getType());
        
        state.setRacingProcedure(nowMock, RacingProcedureType.ESS);
        
        RacingProcedure newProcedure = state.getRacingProcedure();
        assertEquals(RacingProcedureType.ESS, newProcedure.getType());
        assertFalse(oldProcedure.equals(newProcedure));
    }
    
    @Test
    public void testGetRacingProcedure() throws InterruptedException {
        state.setRacingProcedure(nowMock, RacingProcedureType.RRS26);
        GateStartRacingProcedure procedure = state.getTypedRacingProcedure(GateStartRacingProcedure.class);
        assertNull(procedure);
    }

    @Test
    public void testResultsAreOfficial() {
        assertFalse(state.isResultsAreOfficial());
        state.setResultsAreOfficial(TimePoint.now());
        assertTrue(state.isResultsAreOfficial());
    }
}
