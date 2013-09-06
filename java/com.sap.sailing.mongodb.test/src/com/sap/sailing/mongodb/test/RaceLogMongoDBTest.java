package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.UnknownHostException;
import java.util.UUID;

import junit.framework.Assert;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Gate;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.GateImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.impl.WindImpl;

public abstract class RaceLogMongoDBTest extends AbstractMongoDBTest {

    String raceColumnName = "My.First$Race$1";
    TimePoint now = null;
    
    MongoObjectFactory mongoObjectFactory = null;
    DomainObjectFactory domainObjectFactory = null;
    
    public RaceLogMongoDBTest() throws UnknownHostException, MongoException {
        super();
    }
    
    protected void compareCourseData(CourseBase storedCourse, CourseBase loadedCourse) {
        assertEquals(storedCourse.getFirstWaypoint().getPassingSide(), null);
        assertEquals(loadedCourse.getFirstWaypoint().getPassingSide(), null);
        Assert.assertTrue(storedCourse.getFirstWaypoint().getControlPoint() instanceof Gate);
        Assert.assertTrue(loadedCourse.getFirstWaypoint().getControlPoint() instanceof Gate);
        
        Gate storedGate = (Gate) storedCourse.getFirstWaypoint().getControlPoint();
        Gate loadedGate = (Gate) loadedCourse.getFirstWaypoint().getControlPoint();
        
        assertEquals(storedGate.getId(), loadedGate.getId());
        assertEquals(storedGate.getName(), loadedGate.getName());
        
        compareMarks(storedGate.getLeft(), loadedGate.getLeft());
        compareMarks(storedGate.getRight(), loadedGate.getRight());
        
        assertEquals(storedCourse.getLastWaypoint().getPassingSide(), NauticalSide.PORT);
        assertEquals(loadedCourse.getLastWaypoint().getPassingSide(), NauticalSide.PORT);
        Assert.assertTrue(storedCourse.getLastWaypoint().getControlPoint() instanceof Mark);
        Assert.assertTrue(loadedCourse.getLastWaypoint().getControlPoint() instanceof Mark);
        
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
        
        course.addWaypoint(0, new WaypointImpl(new GateImpl(UUID.randomUUID(), 
                new MarkImpl(UUID.randomUUID(), "Black", MarkType.BUOY, "black", "round", "circle"),
                new MarkImpl(UUID.randomUUID(), "Green", MarkType.BUOY, "green", "round", "circle"),
                "Upper gate")));
        course.addWaypoint(1, new WaypointImpl(new MarkImpl(UUID.randomUUID(), "White", MarkType.BUOY, "white", "conical", "bold"), NauticalSide.PORT));
        
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

}
