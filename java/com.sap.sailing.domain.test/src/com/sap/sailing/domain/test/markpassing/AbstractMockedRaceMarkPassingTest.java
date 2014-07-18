package com.sap.sailing.domain.test.markpassing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.racelog.tracking.EmptyGPSFixStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sse.common.Util;

public class AbstractMockedRaceMarkPassingTest {
    protected Competitor ron = new CompetitorImpl("Ron", "Ron", null, null, null);
    protected Competitor tom = new CompetitorImpl("Tom", "Tom", null, null, null);
    protected Competitor ben = new CompetitorImpl("Ben", "Ben", null, null, null);

    protected Mark m = new MarkImpl("Mark");
    protected Mark gate1 = new MarkImpl("Gate1");
    protected Mark gate2 = new MarkImpl("Gate2");
    protected Mark reaching = new MarkImpl("Reaching");

    protected List<Waypoint> waypoints = new ArrayList<>();

    protected DynamicTrackedRace race;

    public AbstractMockedRaceMarkPassingTest() {
        ControlPointWithTwoMarks cp = new ControlPointWithTwoMarksImpl(gate1, gate2, "cp");

        Waypoint w1 = new WaypointImpl(cp, PassingInstruction.Line);
        Waypoint w2 = new WaypointImpl(m, PassingInstruction.Port);
        Waypoint w3 = new WaypointImpl(cp, PassingInstruction.Gate);
        Waypoint w4 = new WaypointImpl(m, PassingInstruction.Starboard);
        Waypoint w5 = new WaypointImpl(cp, PassingInstruction.Line);
        waypoints = Arrays.asList(w1, w2, w3, w4, w5);
        final BoatClassImpl boatClass = new BoatClassImpl("boat", true);
        Regatta r = new RegattaImpl(RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass, Arrays.asList(new SeriesImpl("Series", true, Arrays.asList(new FleetImpl("fleet")),
                new ArrayList<String>(), null)), true, new HighPoint(), "ID", new CourseAreaImpl("area", new UUID(5, 5)));
        Course course = new CourseImpl("course", waypoints);
        RaceDefinition raceDef = new RaceDefinitionImpl("Performance Race", course, boatClass, Arrays.asList(ron, tom, ben));
        race = new DynamicTrackedRaceImpl(new DynamicTrackedRegattaImpl(r), raceDef, new ArrayList<Sideline>(), new EmptyWindStore(), EmptyGPSFixStore.INSTANCE, 0, 10000, 10000);
        race.setStartTimeReceived(new MillisecondsTimePoint(10000));
        TimePoint t = new MillisecondsTimePoint(30000);
        List<Util.Pair<Mark, Position>> pos = Arrays.asList(new Util.Pair<Mark, Position>(m, new DegreePosition(0, 0)),
                new Util.Pair<Mark, Position>(gate1, new DegreePosition(-0.001, -0.00005)), new Util.Pair<Mark, Position>(gate2, new DegreePosition(-0.001, 0.00005)), new Util.Pair<Mark, Position>(reaching, new DegreePosition(-0.0005, -0.0005)));
        for (Util.Pair<Mark, Position> pair : pos) {
            race.recordFix(pair.getA(), new GPSFixImpl(pair.getB(), t));
        }
    }
}
