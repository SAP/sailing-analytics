package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.server.operationaltransformation.UpdateCourseAreas;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MeterDistance;

public class CourseAreaUpdateReplicationTest extends AbstractServerReplicationTest {
    @Test
    public void preservesReferencesReplicatesAndPersistsEverySharedOccurrence() throws Exception {
        final Event event = event("Edited event");
        final Event sharedEvent = event("Shared event");
        final UUID id = UUID.randomUUID();
        final CourseArea area = master.addCourseAreas(event.getId(), new String[] {"Alpha"}, new UUID[] {id},
                new Position[] {null}, new Distance[] {null})[0];
        final CourseArea shared = master.addCourseAreas(sharedEvent.getId(), new String[] {"Alpha"}, new UUID[] {id},
                new Position[] {null}, new Distance[] {null})[0];
        await(() -> replica.getEvent(sharedEvent.getId()) != null
                && replica.getEvent(sharedEvent.getId()).getVenue().getCourseAreas().iterator().hasNext());
        final CourseArea replicated = replica.getEvent(event.getId()).getVenue().getCourseAreas().iterator().next();
        final Position position = new DegreePosition(/* latDeg */ 54, /* lngDeg */ 10);
        final Distance radius = new MeterDistance(/* meters */ 750);
        master.apply(new UpdateCourseAreas(event.getId(), new UUID[] {id}, new String[] {"Bravo"},
                new Position[] {position}, new Distance[] {radius}));
        await(() -> "Bravo".equals(replicated.getName()) && position.equals(replicated.getCenterPosition())
                && radius.equals(replicated.getRadius()));
        assertSame(area, shared);
        assertSame(area, master.getBaseDomainFactory().getExistingCourseAreaById(id));
        assertSame(replicated, replica.getEvent(event.getId()).getVenue().getCourseAreas().iterator().next());
        assertSame(replicated, replica.getEvent(sharedEvent.getId()).getVenue().getCourseAreas().iterator().next());
        assertEquals(position, replicated.getCenterPosition());
        assertEquals(radius, replicated.getRadius());
        for (final Event affected : new Event[] {event, sharedEvent}) {
            // Each document is loaded into an empty identity cache so one correct event cannot mask a stale document.
            final DomainObjectFactory loader = PersistenceFactory.INSTANCE.getMajorityDomainObjectFactory(
                    testSetUp.mongoDBService, new DomainFactoryImpl(/* raceLogResolver */ null));
            final CourseArea loaded = loader.loadEvent(affected.getName()).getVenue().getCourseAreas().iterator().next();
            assertEquals(id, loaded.getId());
            assertEquals("Bravo", loaded.getName());
            assertEquals(position, loaded.getCenterPosition());
            assertEquals(radius, loaded.getRadius());
        }
        master.apply(new UpdateCourseAreas(event.getId(), new UUID[] {id}, new String[] {"Charlie"},
                new Position[] {null}, new Distance[] {null}));
        await(() -> "Charlie".equals(replicated.getName()) && replicated.getCenterPosition() == null
                && replicated.getRadius() == null);
        assertNull(replicated.getCenterPosition());
        assertNull(replicated.getRadius());
    }

    @Test
    public void rejectsForeignAreaBeforeChangingAnyAttributes() {
        final Event event = event("Edited event");
        final CourseArea area = master.addCourseAreas(event.getId(), new String[] {"Alpha"},
                new UUID[] {UUID.randomUUID()}, new Position[] {null}, new Distance[] {null})[0];
        assertThrows(IllegalArgumentException.class, () -> master.updateCourseAreasWithoutReplication(event.getId(),
                new UUID[] {area.getId(), UUID.randomUUID()}, new String[] {"Bravo", "Foreign"},
                new Position[] {null, null}, new Distance[] {null, null}));
        assertEquals("Alpha", area.getName());
    }

    private Event event(final String name) {
        return master.addEvent(name, /* eventDescription */ null, /* startDate */ null, /* endDate */ null,
                "Venue", /* isPublic */ false, UUID.randomUUID());
    }

    private void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 10000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(/* millis */ 20);
        }
        assertTrue(condition.getAsBoolean(), "Replication did not complete");
    }
}
