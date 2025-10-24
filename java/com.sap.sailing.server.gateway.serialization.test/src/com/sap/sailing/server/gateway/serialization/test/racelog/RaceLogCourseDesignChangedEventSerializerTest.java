package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.ControlPointDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.CourseBaseDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.GateDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.MarkDeserializer;
import com.sap.sailing.server.gateway.deserialization.coursedata.impl.WaypointDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogCourseDesignChangedEventDeserializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.ControlPointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.GateJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.WaypointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogCourseDesignChangedEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

public class RaceLogCourseDesignChangedEventSerializerTest {

    private RaceLogCourseDesignChangedEventSerializer serializer;
    private RaceLogCourseDesignChangedEventDeserializer deserializer;
    private RaceLogCourseDesignChangedEvent event;
    private TimePoint now;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);

    @BeforeEach
    public void setUp() {
        SharedDomainFactory<?> factory = DomainFactory.INSTANCE;
        serializer = new RaceLogCourseDesignChangedEventSerializer(CompetitorJsonSerializer.create(), new CourseBaseJsonSerializer(
                new WaypointJsonSerializer(new ControlPointJsonSerializer(new MarkJsonSerializer(),
                        new GateJsonSerializer(new MarkJsonSerializer())))));
        deserializer = new RaceLogCourseDesignChangedEventDeserializer(CompetitorJsonDeserializer.create(DomainFactory.INSTANCE),
                new CourseBaseDeserializer(new WaypointDeserializer(new ControlPointDeserializer(new MarkDeserializer(
                        factory), new GateDeserializer(factory, new MarkDeserializer(factory))))));
        now = MillisecondsTimePoint.now();

        event = new RaceLogCourseDesignChangedEventImpl(now, author, 0, createCourseData(), CourseDesignerMode.BY_MARKS);
    }

    @Test
    public void testSerializeAndDeserializeRaceLogCourseDesignChangedEvent() throws JsonDeserializationException {
        JSONObject jsonCourseDesignEvent = serializer.serialize(event);
        RaceLogCourseDesignChangedEvent deserializedEvent = (RaceLogCourseDesignChangedEvent) deserializer
                .deserialize(jsonCourseDesignEvent);

        assertEquals(event.getId(), deserializedEvent.getId());
        assertEquals(event.getPassId(), deserializedEvent.getPassId());
        assertEquals(event.getLogicalTimePoint(), deserializedEvent.getLogicalTimePoint());
        assertEquals(event.getCourseDesignerMode(), deserializedEvent.getCourseDesignerMode());
        assertEquals(0, Util.size(event.getInvolvedCompetitors()));
        assertEquals(0, Util.size(deserializedEvent.getInvolvedCompetitors()));

        compareCourseData(event.getCourseDesign(), deserializedEvent.getCourseDesign());
    }

    protected CourseBase createCourseData() {
        CourseBase course = new CourseDataImpl("Test Course");

        course.addWaypoint(0, new WaypointImpl(new ControlPointWithTwoMarksImpl(UUID.randomUUID(), new MarkImpl(UUID.randomUUID(), "Black",
                MarkType.BUOY, AbstractColor.getCssColor("black"), "round", "circle"), new MarkImpl(UUID.randomUUID(), "Green", MarkType.BUOY,
                                AbstractColor.getCssColor("green"), "round", "circle"),
                        "Upper gate", "Upper gate")));
        course.addWaypoint(1, new WaypointImpl(new MarkImpl(UUID.randomUUID(), "White", MarkType.BUOY, AbstractColor.getCssColor("white"),
                "conical", "bold"), PassingInstruction.Port));

        return course;
    }

    protected void compareCourseData(CourseBase serializedCourse, CourseBase deserializedCourse) {
        assertEquals(serializedCourse.getFirstWaypoint().getPassingInstructions(), PassingInstruction.None);
        assertEquals(deserializedCourse.getFirstWaypoint().getPassingInstructions(), PassingInstruction.None);
        Assertions.assertTrue(serializedCourse.getFirstWaypoint().getControlPoint() instanceof ControlPointWithTwoMarks);
        Assertions.assertTrue(deserializedCourse.getFirstWaypoint().getControlPoint() instanceof ControlPointWithTwoMarks);

        ControlPointWithTwoMarks serializedGate = (ControlPointWithTwoMarks) serializedCourse.getFirstWaypoint().getControlPoint();
        ControlPointWithTwoMarks deserializedGate = (ControlPointWithTwoMarks) deserializedCourse.getFirstWaypoint().getControlPoint();

        assertEquals(serializedGate.getName(), deserializedGate.getName());
        compareMarks(serializedGate.getLeft(), deserializedGate.getLeft());
        compareMarks(serializedGate.getRight(), deserializedGate.getRight());

        assertEquals(serializedCourse.getLastWaypoint().getPassingInstructions(), PassingInstruction.Port);
        assertEquals(deserializedCourse.getLastWaypoint().getPassingInstructions(), PassingInstruction.Port);
        Assertions.assertTrue(serializedCourse.getLastWaypoint().getControlPoint() instanceof Mark);
        Assertions.assertTrue(deserializedCourse.getLastWaypoint().getControlPoint() instanceof Mark);

        Mark serializedMark = (Mark) serializedCourse.getLastWaypoint().getControlPoint();
        Mark deserializedMark = (Mark) deserializedCourse.getLastWaypoint().getControlPoint();
        compareMarks(serializedMark, deserializedMark);
    }

    private void compareMarks(Mark serializedMark, Mark deserializedMark) {
        assertEquals(serializedMark.getId(), deserializedMark.getId());
        assertEquals(serializedMark.getColor(), deserializedMark.getColor());
        assertEquals(serializedMark.getName(), deserializedMark.getName());
        assertEquals(serializedMark.getPattern(), deserializedMark.getPattern());
        assertEquals(serializedMark.getShape(), deserializedMark.getShape());
        assertEquals(serializedMark.getType(), deserializedMark.getType());
    }

}
