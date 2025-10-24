package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.MongoRegattaLogStoreFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.DegreeBearingImpl;

public abstract class RaceLogMongoDBTest extends AbstractMongoDBTest {

    String raceColumnName = "My.First$Race$1";
    TimePoint now = null;

    MongoObjectFactory mongoObjectFactory = null;
    DomainObjectFactory domainObjectFactory = null;

    public RaceLogMongoDBTest() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    public void setup() {
        mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(),
                DomainFactory.INSTANCE);
    }

    protected void compareCourseData(CourseBase storedCourse, CourseBase loadedCourse) {
        assertEquals(storedCourse.getFirstWaypoint().getPassingInstructions(), PassingInstruction.None);
        assertEquals(loadedCourse.getFirstWaypoint().getPassingInstructions(), PassingInstruction.None);
        Assertions.assertTrue(storedCourse.getFirstWaypoint().getControlPoint() instanceof ControlPointWithTwoMarks);
        Assertions.assertTrue(loadedCourse.getFirstWaypoint().getControlPoint() instanceof ControlPointWithTwoMarks);

        ControlPointWithTwoMarks storedGate = (ControlPointWithTwoMarks) storedCourse.getFirstWaypoint()
                .getControlPoint();
        ControlPointWithTwoMarks loadedGate = (ControlPointWithTwoMarks) loadedCourse.getFirstWaypoint()
                .getControlPoint();

        assertEquals(storedGate.getId(), loadedGate.getId());
        assertEquals(storedGate.getName(), loadedGate.getName());

        compareMarks(storedGate.getLeft(), loadedGate.getLeft());
        compareMarks(storedGate.getRight(), loadedGate.getRight());

        assertEquals(storedCourse.getLastWaypoint().getPassingInstructions(), PassingInstruction.Port);
        assertEquals(loadedCourse.getLastWaypoint().getPassingInstructions(), PassingInstruction.Port);
        Assertions.assertTrue(storedCourse.getLastWaypoint().getControlPoint() instanceof Mark);
        Assertions.assertTrue(loadedCourse.getLastWaypoint().getControlPoint() instanceof Mark);

        Mark storedMark = (Mark) storedCourse.getLastWaypoint().getControlPoint();
        Mark loadedMark = (Mark) loadedCourse.getLastWaypoint().getControlPoint();
        compareMarks(storedMark, loadedMark);
    }

    private void compareMarks(Mark storedMark, Mark loadedMark) {
        assertEquals(storedMark.getId(), loadedMark.getId());
        assertEquals(storedMark.getColor(), loadedMark.getColor());
        assertEquals(storedMark.getName(), loadedMark.getName());
        assertEquals(storedMark.getPattern(), loadedMark.getPattern());
        assertEquals(storedMark.getShape(), loadedMark.getShape());
        assertEquals(storedMark.getType(), loadedMark.getType());
    }

    protected CourseBase createCourseBase() {
        CourseBase course = new CourseDataImpl("Test Course");

        course.addWaypoint(0,
                new WaypointImpl(new ControlPointWithTwoMarksImpl(UUID.randomUUID(), new MarkImpl(UUID.randomUUID(),
                        "Black", MarkType.BUOY, AbstractColor.getCssColor("black"), "round", "circle"), new MarkImpl(UUID.randomUUID(), "Green",
                                MarkType.BUOY, AbstractColor.getCssColor("green"), "round", "circle"),
                        "Upper gate", "Upper gate")));
        course.addWaypoint(1, new WaypointImpl(new MarkImpl(UUID.randomUUID(), "White", MarkType.BUOY, AbstractColor.getCssColor("white"),
                "conical", "bold"), PassingInstruction.Port));

        return course;
    }

    protected void compareWind(Wind storedWindFix, Wind loadedWindFix) {
        assertEquals(storedWindFix.getTimePoint(), loadedWindFix.getTimePoint());
        assertNotNull(storedWindFix.getPosition());
        assertNotNull(loadedWindFix.getPosition());
        assertEquals(storedWindFix.getPosition().getLatDeg(), loadedWindFix.getPosition().getLatDeg(), 0);
        assertEquals(storedWindFix.getPosition().getLngDeg(), loadedWindFix.getPosition().getLngDeg(), 0);
        assertEquals(storedWindFix.getKnots(), loadedWindFix.getKnots(), 0);
        assertNotNull(storedWindFix.getBearing());
        assertNotNull(loadedWindFix.getBearing());
        assertEquals(storedWindFix.getBearing().getDegrees(), loadedWindFix.getBearing().getDegrees(), 0);
    }

    protected Wind createWindFix() {
        Position position = new DegreePosition(23.0313, 2.2344);
        Bearing bearing = new DegreeBearingImpl(25.5);
        SpeedWithBearing speedBearing = new KnotSpeedWithBearingImpl(10.4, bearing);
        return new WindImpl(position, now, speedBearing);
    }

    protected RaceLogStore getRaceLogStore() {
        return MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory);
    }

    protected RegattaLogStore getRegattaLogStore() {
        return MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStore(mongoObjectFactory, domainObjectFactory);
    }

    public static void assertCompetitorResultsEqual(final CompetitorResults expectedCompetitorResults, final CompetitorResults loadedCompetitorResults) {
        assertEquals(Util.size(expectedCompetitorResults), Util.size(loadedCompetitorResults));
        final Iterator<CompetitorResult> iExpected = expectedCompetitorResults.iterator();
        final Iterator<CompetitorResult> iLoaded = loadedCompetitorResults.iterator();
        while (iExpected.hasNext()) {
            assertEquals(iExpected.next(), iLoaded.next());
        }
    }

}
