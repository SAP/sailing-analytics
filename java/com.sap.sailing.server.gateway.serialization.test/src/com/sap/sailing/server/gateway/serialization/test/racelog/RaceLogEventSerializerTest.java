package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCImpliedWindSourceEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogExcludeWindSourcesEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningListChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogGateLineOpeningTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPathfinderEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogProtestStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRevokeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartProcedureChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogTagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDenoteForTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogStartTrackingEventImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogEventSerializer;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.shared.json.JsonSerializer;

public class RaceLogEventSerializerTest {

    private RaceLogEventSerializer serializer;

    private JsonSerializer<RaceLogEvent> flagEventSerializer;
    private JsonSerializer<RaceLogEvent> startTimeSerializer;
    private JsonSerializer<RaceLogEvent> raceStatusSerializer;
    private JsonSerializer<RaceLogEvent> passChangedSerializer;
    private JsonSerializer<RaceLogEvent> courseDesignChangedEventSerializer;
    private JsonSerializer<RaceLogEvent> finishPositioningListChangedEventSerializer;
    private JsonSerializer<RaceLogEvent> finishPositioningConfirmedEventSerializer;
    private JsonSerializer<RaceLogEvent> pathfinderEventSerializer;
    private JsonSerializer<RaceLogEvent> gateLineOpeningTimeEventSerializer;
    private JsonSerializer<RaceLogEvent> startProcedureTypeChangedEventSerializer;
    private JsonSerializer<RaceLogEvent> protestStartTimeEventSerializer;
    private JsonSerializer<RaceLogEvent> windFixEventSerializer;
    private JsonSerializer<RaceLogEvent> denoteForTrackingEventSerializer;
    private JsonSerializer<RaceLogEvent> startTrackingEventSerializer;
    private JsonSerializer<RaceLogEvent> revokeEventSerializer;
    private JsonSerializer<RaceLogEvent> registerCompetitorEventSerializer;
    private JsonSerializer<RaceLogEvent> additionalScoringInformationSerializer;
    private JsonSerializer<RaceLogEvent> fixedMarkPassingEventSerializer;
    private JsonSerializer<RaceLogEvent> suppressedMarkPassingsSerializer;
    private JsonSerializer<RaceLogEvent> dependentStartTimeEventSerializer;
    private JsonSerializer<RaceLogEvent> startOfTrackingEventSerializer;
    private JsonSerializer<RaceLogEvent> useCompetitorsFromRaceLogEventSerializer;
    private JsonSerializer<RaceLogEvent> endOfTrackingEventSerializer;
    private JsonSerializer<RaceLogEvent> tagEventSerializer;
    private JsonSerializer<RaceLogEvent> orcLegDataEventSerializer;
    private JsonSerializer<RaceLogEvent> orcCertificateAssignmentEventSerializer;
    private JsonSerializer<RaceLogEvent> orcScratchBoatEventSerializer;
    private JsonSerializer<RaceLogEvent> orcSetImpliedWindEventSerializer;
    private JsonSerializer<RaceLogEvent> resultsAreOfficialEventSerializer;
    private JsonSerializer<RaceLogEvent> excludeWindSourceEventSerializer;
    
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);


    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() {
        flagEventSerializer = mock(JsonSerializer.class);
        startTimeSerializer = mock(JsonSerializer.class);
        raceStatusSerializer = mock(JsonSerializer.class);
        passChangedSerializer = mock(JsonSerializer.class);
        courseDesignChangedEventSerializer = mock(JsonSerializer.class);
        finishPositioningListChangedEventSerializer = mock(JsonSerializer.class);
        finishPositioningConfirmedEventSerializer = mock(JsonSerializer.class);
        pathfinderEventSerializer = mock(JsonSerializer.class);
        gateLineOpeningTimeEventSerializer = mock(JsonSerializer.class);
        startProcedureTypeChangedEventSerializer = mock(JsonSerializer.class);
        protestStartTimeEventSerializer = mock(JsonSerializer.class);
        windFixEventSerializer = mock(JsonSerializer.class);
        denoteForTrackingEventSerializer = mock(JsonSerializer.class);
        startTrackingEventSerializer = mock(JsonSerializer.class);
        revokeEventSerializer = mock(JsonSerializer.class);
        registerCompetitorEventSerializer = mock(JsonSerializer.class);
        additionalScoringInformationSerializer = mock(JsonSerializer.class);
        fixedMarkPassingEventSerializer = mock(JsonSerializer.class);
        suppressedMarkPassingsSerializer = mock(JsonSerializer.class);
        dependentStartTimeEventSerializer = mock(JsonSerializer.class);
        startOfTrackingEventSerializer = mock(JsonSerializer.class);
        useCompetitorsFromRaceLogEventSerializer = mock(JsonSerializer.class);
        endOfTrackingEventSerializer = mock(JsonSerializer.class);
        tagEventSerializer = mock(JsonSerializer.class);
        orcLegDataEventSerializer = mock(JsonSerializer.class);
        orcCertificateAssignmentEventSerializer = mock(JsonSerializer.class);
        orcScratchBoatEventSerializer = mock(JsonSerializer.class);
        orcSetImpliedWindEventSerializer = mock(JsonSerializer.class);
        resultsAreOfficialEventSerializer = mock(JsonSerializer.class);
        excludeWindSourceEventSerializer = mock(JsonSerializer.class);
        
        serializer = new RaceLogEventSerializer(flagEventSerializer, startTimeSerializer, raceStatusSerializer,
                passChangedSerializer, courseDesignChangedEventSerializer,
                finishPositioningListChangedEventSerializer, finishPositioningConfirmedEventSerializer,
                pathfinderEventSerializer, gateLineOpeningTimeEventSerializer,
                startProcedureTypeChangedEventSerializer, protestStartTimeEventSerializer, windFixEventSerializer,
                denoteForTrackingEventSerializer, startTrackingEventSerializer, revokeEventSerializer,
                registerCompetitorEventSerializer, 
                fixedMarkPassingEventSerializer, suppressedMarkPassingsSerializer, 
                additionalScoringInformationSerializer, dependentStartTimeEventSerializer, 
                startOfTrackingEventSerializer, useCompetitorsFromRaceLogEventSerializer, 
                endOfTrackingEventSerializer, tagEventSerializer, orcLegDataEventSerializer,
                orcCertificateAssignmentEventSerializer, orcScratchBoatEventSerializer,
                orcSetImpliedWindEventSerializer, resultsAreOfficialEventSerializer,
                excludeWindSourceEventSerializer);
    }

    @Test
    public void testExcludeWindSourceEventSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogExcludeWindSourcesEventImpl(null, author, 0, Collections.singleton(new WindSourceWithAdditionalID(WindSourceType.WEB, "123")));
        serializer.serialize(event);
        verify(excludeWindSourceEventSerializer).serialize(event);
    }
    
    @Test
    public void testChoosesFlagEventSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogFlagEventImpl(null, author, 0, null, null, false);
        serializer.serialize(event);
        verify(flagEventSerializer).serialize(event);
    }

    @Test
    public void testChoosesStartTimeSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogStartTimeEventImpl(null, author, 0, null, /* courseAreaId */ null);
        serializer.serialize(event);
        verify(startTimeSerializer).serialize(event);
    }

    @Test
    public void testChoosesRaceStatusSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogRaceStatusEventImpl(null, author, 0, RaceLogRaceStatus.SCHEDULED);
        serializer.serialize(event);
        verify(raceStatusSerializer).serialize(event);
    }
    
    @Test
    public void testNullImpliedWindSourceSerializer() {
        RaceLogEvent event = new RaceLogORCImpliedWindSourceEventImpl(null, null, author, null, 0, null);
        serializer.serialize(event);
        verify(orcSetImpliedWindEventSerializer).serialize(event);
    }

    @Test
    public void testChoosesPassChangedSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogPassChangeEventImpl(null, author, 0);
        serializer.serialize(event);
        verify(passChangedSerializer).serialize(event);
    }

    @Test
    public void testChoosesCourseDesignChangedSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogCourseDesignChangedEventImpl(null, author, 0, null, CourseDesignerMode.ADMIN_CONSOLE);
        serializer.serialize(event);
        verify(courseDesignChangedEventSerializer).serialize(event);
    }

    @Test
    public void testChoosesFinishPositioningListChangedSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogFinishPositioningListChangedEventImpl(null, author, 0, null);
        serializer.serialize(event);
        verify(finishPositioningListChangedEventSerializer).serialize(event);
    }

    @Test
    public void testChoosesFinishPositioningConfirmedSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogFinishPositioningConfirmedEventImpl(null, author, 0, null);
        serializer.serialize(event);
        verify(finishPositioningConfirmedEventSerializer).serialize(event);
    }

    @Test
    public void testChoosesPathfinderSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogPathfinderEventImpl(null, author, 0, "GER 20");
        serializer.serialize(event);
        verify(pathfinderEventSerializer).serialize(event);
    }

    @Test
    public void testChoosesGateLineOpeningTimeSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogGateLineOpeningTimeEventImpl(null, author, 0, 2l, 1l);
        serializer.serialize(event);
        verify(gateLineOpeningTimeEventSerializer).serialize(event);
    }

    @Test
    public void testStartProcedureTypeChangedSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogStartProcedureChangedEventImpl(null, author, 0, RacingProcedureType.ESS);
        serializer.serialize(event);
        verify(startProcedureTypeChangedEventSerializer).serialize(event);
    }

    @Test
    public void testProtestStartTimeSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogProtestStartTimeEventImpl(null, author, 0, new TimeRangeImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now()));
        serializer.serialize(event);
        verify(protestStartTimeEventSerializer).serialize(event);
    }

    @Test
    public void testWindFixSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogWindFixEventImpl(null, author, 0, null, /* isMagnetic */ false);
        serializer.serialize(event);
        verify(windFixEventSerializer).serialize(event);
    }
    
    @Test
    public void testDenoteForTrackingSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogDenoteForTrackingEventImpl(null, author, 0, "", null, null);
        serializer.serialize(event);
        verify(denoteForTrackingEventSerializer).serialize(event);
    }

    @Test
    public void testCreateRaceSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogStartTrackingEventImpl(null, author, 0);
        serializer.serialize(event);
        verify(startTrackingEventSerializer).serialize(event);
    }

    @Test
    public void testRevokeEventSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogRevokeEventImpl(MillisecondsTimePoint.now(), MillisecondsTimePoint.now(),
                author, 0, 0, UUID.randomUUID(), "type", "short info", "reason");
        serializer.serialize(event);
        verify(revokeEventSerializer).serialize(event);
    }

    @Test
    public void testRegisterCompetitorEventSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        Competitor c = DomainFactory.INSTANCE.getOrCreateCompetitor("comp", "comp", "c", null, null, null, null,
                /* timeOnTimeFactor */null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat b = DomainFactory.INSTANCE.getOrCreateBoat("boat", "b", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null, null, /* storePersistently */ true);
        RaceLogEvent event = new RaceLogRegisterCompetitorEventImpl(null, author, 0, c, b);
        serializer.serialize(event);
        verify(registerCompetitorEventSerializer).serialize(event);
        Boat b2 = new BoatImpl("boat", "b", new BoatClassImpl("505", /* typicallyStartsUpwind */ true), null, null);
        CompetitorWithBoat c2 = DomainFactory.INSTANCE.getOrCreateCompetitorWithBoat("comp2", "comp", "c", null, null,
                null, null, /* timeOnTimeFactor */null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, (DynamicBoat) b2, /* storePersistently */ true);
        RaceLogEvent event2 = new RaceLogRegisterCompetitorEventImpl(null, author, 0, c2, b2);
        serializer.serialize(event2);
        verify(registerCompetitorEventSerializer).serialize(event2);        
    }

    @Test
    public void testTagSerializer() {
        // we use the real event type here because we do not want to re-implement the dispatching.
        RaceLogEvent event = new RaceLogTagEventImpl("", "", /* hiddenInfo */ null, "", "", MillisecondsTimePoint.now(), author, 0);
        serializer.serialize(event);
        verify(tagEventSerializer).serialize(event);
    }
}
