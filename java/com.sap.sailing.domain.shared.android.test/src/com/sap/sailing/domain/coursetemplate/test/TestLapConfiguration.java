package com.sap.sailing.domain.coursetemplate.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.coursetemplate.ControlPointTemplate;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.domain.coursetemplate.impl.CourseTemplateImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkRoleImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkRolePairImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkTemplateImpl;
import com.sap.sailing.domain.coursetemplate.impl.WaypointTemplateImpl;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.RepeatablePartImpl;

public class TestLapConfiguration {
    private CourseTemplate courseTemplate;
    private MarkTemplate startBoat;
    private MarkRole startBoatRole;
    private MarkTemplate pin;
    private MarkRole pinRole;
    private MarkTemplate top;
    private MarkRole one;
    private MarkTemplate gateLeft;
    private MarkRole threeP;
    private MarkTemplate gateRight;
    private MarkRole threeS;
    private ControlPointTemplate startFinish;
    private ControlPointTemplate gate;
    private Map<MarkRole, MarkTemplate> defaultMarkTemplatesForMarkRoles;
    
    @BeforeEach
    public void setUp() {
        startBoat = new MarkTemplateImpl("Start Boat", "SB", /* color */ null, /* shape */ null, /* pattern */ null, MarkType.STARTBOAT);
        startBoatRole = new MarkRoleImpl(UUID.randomUUID(), "Start Boat", "SB");
        pin = new MarkTemplateImpl("Pin End", "Pin", /* color */ null, /* shape */ null, /* pattern */ null, MarkType.BUOY);
        pinRole = new MarkRoleImpl(UUID.randomUUID(), "Pin End", "P");
        top = new MarkTemplateImpl("Windward Mark", "1", /* color */ null, /* shape */ null, /* pattern */ null, MarkType.BUOY);
        one = new MarkRoleImpl(UUID.randomUUID(), "Windward Mark", "1");
        gateLeft = new MarkTemplateImpl("Leeward Gate Port", "4p", /* color */ null, /* shape */ null, /* pattern */ null, MarkType.BUOY);
        threeP = new MarkRoleImpl(UUID.randomUUID(), "Leeward Gate Port", "3p");
        gateRight = new MarkTemplateImpl("Leeward Gate Starboard", "4s", /* color */ null, /* shape */ null, /* pattern */ null, MarkType.BUOY);
        threeS = new MarkRoleImpl(UUID.randomUUID(), "Leeward Gate Starboard", "3s");
        startFinish = new MarkRolePairImpl("Start/Finish", "S/F", startBoatRole, pinRole);
        gate = new MarkRolePairImpl("Leeward Gate", "LG", threeP, threeS);
        defaultMarkTemplatesForMarkRoles = new HashMap<>();
        defaultMarkTemplatesForMarkRoles.put(startBoatRole, startBoat);
        defaultMarkTemplatesForMarkRoles.put(pinRole, pin);
        defaultMarkTemplatesForMarkRoles.put(one, top);
        defaultMarkTemplatesForMarkRoles.put(threeP, gateLeft);
        defaultMarkTemplatesForMarkRoles.put(threeS, gateRight);
        courseTemplate = new CourseTemplateImpl(UUID.randomUUID(), "Windward/Leeward", "L",
                /* marks */ Arrays.asList(startBoat, pin, top, gateLeft, gateRight),
                /* waypoints */ Arrays.asList(new WaypointTemplateImpl(startFinish, PassingInstruction.Line),
                                              new WaypointTemplateImpl(one, PassingInstruction.Port),
                                              new WaypointTemplateImpl(gate, PassingInstruction.Gate),
                                              new WaypointTemplateImpl(one, PassingInstruction.Port),
                                              new WaypointTemplateImpl(startFinish, PassingInstruction.Line)),
                defaultMarkTemplatesForMarkRoles,
                /* associatedRoles */ Collections.emptyMap(),
                /* optionaImageURL */ null, new RepeatablePartImpl(/* zeroBasedIndexOfRepeatablePartStart */ 1,
                                /* zeroBasedIndexOfRepeatablePartEnd */ 3), /* Default Number of Laps */ 1);
    }
    
    @Test
    public void testSimpleCourseWithOneLap() {
        final Iterable<WaypointTemplate> waypointsOfOneLapper = courseTemplate.getWaypointTemplates(1);
        final List<ControlPointTemplate> expected = Arrays.asList(startFinish, one, startFinish);
        verify(waypointsOfOneLapper, expected);
    }

    private void verify(final Iterable<WaypointTemplate> waypointsOfOneLapper,
            final List<ControlPointTemplate> expected) {
        assertEquals(expected.size(), Util.size(waypointsOfOneLapper));
        final List<ControlPointTemplate> controlPoints = new ArrayList<>();
        for (final WaypointTemplate waypoint : waypointsOfOneLapper) {
            controlPoints.add(waypoint.getControlPointTemplate());
        }
        assertEquals(expected, controlPoints);
    }

    @Test
    public void testSimpleCourseWithTwoLaps() {
        final Iterable<WaypointTemplate> waypointsOfOneLapper = courseTemplate.getWaypointTemplates(2);
        final List<ControlPointTemplate> expected = Arrays.asList(startFinish, one, gate, one, startFinish);
        verify(waypointsOfOneLapper, expected);
    }

    @Test
    public void testSimpleCourseWithThreeLaps() {
        final Iterable<WaypointTemplate> waypointsOfOneLapper = courseTemplate.getWaypointTemplates(3);
        final List<ControlPointTemplate> expected = Arrays.asList(startFinish, one, gate, one, gate, one, startFinish);
        verify(waypointsOfOneLapper, expected);
    }

    @Test
    public void testExceptionInCaseMarkIsMissing() {
        startBoat = new MarkTemplateImpl("Start Boat", "SB", /* color */ null, /* shape */ null, /* pattern */ null, MarkType.STARTBOAT);
        pin = new MarkTemplateImpl("Pin End", "Pin", /* color */ null, /* shape */ null, /* pattern */ null, MarkType.BUOY);
        startFinish = new MarkRolePairImpl("Start/Finish","Start/Finish", startBoatRole, pinRole);
        try {
            courseTemplate = new CourseTemplateImpl("Test", "T",
                    /* marks */ Arrays.asList(startBoat),
                    /* waypoints */ Arrays.asList(new WaypointTemplateImpl(startFinish, PassingInstruction.Line)),
                    /* defaultMarkRolesForMarkTemplates */ Collections.emptyMap(),
                    /* defaultMarkTemplatesForRoles */ Collections.emptyMap(), /* optionaImageURL */ null);
            fail("Expected an IllegalArgumentException due to missing mark <pin> but it wasn't thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testZeroLapsOkIfNoRepeatablePart() {
        courseTemplate = new CourseTemplateImpl("Test", "T",
                /* marks */ Arrays.asList(startBoat, pin),
                /* waypoints */ Arrays.asList(new WaypointTemplateImpl(startFinish, PassingInstruction.Line)),
                defaultMarkTemplatesForMarkRoles,
                /* defaultMarkTemplatesForRoles */ Collections.emptyMap(), /* optionaImageURL */ null);
        try {
            courseTemplate.getWaypointTemplates(0);
        } catch (IllegalArgumentException e) {
            fail("No IllegalArgumentException should have been thrown for zero laps because the course has no repeatable part: "+e.getMessage());
        }
    }
    
    @Test
    public void testIllegalArgumentExceptionForZeroLaps() {
        try {
            courseTemplate
                    .getWaypointTemplates(/* illegal to request 0 laps if course template defines repeatable part */ 0);
            fail("Expected an IllegalArgumentException but none was thrown");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
