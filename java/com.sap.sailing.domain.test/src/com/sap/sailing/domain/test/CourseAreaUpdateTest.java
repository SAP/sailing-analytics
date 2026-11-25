package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MeterDistance;

public class CourseAreaUpdateTest {
    @Test
    public void readsCourseAreaSerializedBeforeEditingWasSupported() throws Exception {
        // Serialized with the original CourseAreaImpl from the event-editing integration merge.
        final String legacy = "rO0ABXNyAC9jb20uc2FwLnNhaWxpbmcuZG9tYWluLmJhc2UuaW1wbC5Db3Vyc2VBcmVhSW1wbFINAyjwLuNeAgAD"
                + "TAAOY2VudGVyUG9zaXRpb250AB1MY29tL3NhcC9zc2UvY29tbW9uL1Bvc2l0aW9uO0wAAmlkdAAQTGphdmEvdXRp"
                + "bC9VVUlEO0wABnJhZGl1c3QAHUxjb20vc2FwL3NzZS9jb21tb24vRGlzdGFuY2U7eHIAIWNvbS5zYXAuc3NlLmNv"
                + "bW1vbi5pbXBsLk5hbWVkSW1wbL0tPREpXaNcAgABTAAEbmFtZXQAEkxqYXZhL2xhbmcvU3RyaW5nO3hwdAAMTGVn"
                + "YWN5IEFscGhhcHNyAA5qYXZhLnV0aWwuVVVJRLyZA/eYbYUvAgACSgAMbGVhc3RTaWdCaXRzSgALbW9zdFNpZ0Jp"
                + "dHN4cAAAAAAAAAABVClUKQAAAABw";
        try (final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(legacy)))) {
            final CourseArea area = (CourseArea) input.readObject();
            assertEquals("Legacy Alpha", area.getName());
            assertEquals(UUID.fromString("54295429-0000-0000-0000-000000000001"), area.getId());
            area.update("Renamed", /* centerPosition */ null, /* radius */ null);
            assertEquals("Renamed", roundTrip(area).getName());
        }
    }

    @Test
    public void cachedIdentityAndSerializedAttributesSurviveUpdates() throws Exception {
        final DomainFactoryImpl factory = new DomainFactoryImpl(/* raceLogResolver */ null);
        final UUID id = UUID.randomUUID();
        final CourseArea area = factory.getOrCreateCourseArea(id, "Alpha", /* centerPosition */ null, /* radius */ null);
        assertEquals("Alpha", roundTrip(area).getName());
        final Position center = new DegreePosition(/* latDeg */ 54, /* lngDeg */ 10);
        final MeterDistance radius = new MeterDistance(/* meters */ 500);
        area.update("Bravo", center, radius);
        assertSame(area, factory.getOrCreateCourseArea(id, "Alpha", /* centerPosition */ null, /* radius */ null));
        final CourseArea restored = roundTrip(area);
        assertEquals(id, restored.getId());
        assertEquals("Bravo", restored.getName());
        assertEquals(center, restored.getCenterPosition());
        assertEquals(radius, restored.getRadius());
        assertSame(area, restored.resolve(factory));
        area.update("Charlie", /* centerPosition */ null, /* radius */ null);
        assertNull(area.getCenterPosition());
        assertNull(area.getRadius());
    }

    private CourseArea roundTrip(final CourseArea area) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(area);
        }
        final CourseArea result;
        try (final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            result = (CourseArea) input.readObject();
        }
        return result;
    }
}
