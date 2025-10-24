package com.sap.sailing.domain.test.markpassing;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.markpassingcalculation.CandidateChooser;
import com.sap.sailing.domain.markpassingcalculation.CandidateFinder;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateFinderImpl;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.DateParser;

public class StarIDM2013MarkPassingTest extends AbstractMarkPassingTest {

    public StarIDM2013MarkPassingTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Test
    public void testRace5() throws Exception {
        // Race 5 is really broken on TracTrac's side. I've tried re-calculating the mark passing to no avail.
        // Now it's even worse, and only the windward mark passings and the start mark passings are recognized
        // half-way reasonably, but no mark passings seem to have been detected for subsequent waypoints.
        // To still use this race for testing purposes, let's do spot checks based on manually identified mark
        // passings, based on https://www.sapsailing.com/gwt/RaceBoard.html?eventId=a43b8f96-1e77-448a-9b8a-0f586f1bb18f&leaderboardName=Star%20IDM%202013&leaderboardGroupName=Star%20IDM%202013&raceName=Race%205&viewShowMapControls=true&viewShowNavigationPanel=true&regattaName=IDM%20Starboot%202013%20%28Star%29
        setUp("5");
        synchronized (getSemaphor()) {
            while (!isStoredDataLoaded()) {
                getSemaphor().wait();
            }
        }
        testWholeRace5();
        testOnlyStartOfRace5();
    }

    private void testWholeRace5() throws InvalidDateException {
        new MarkPassingCalculator(getTrackedRace(), false, /* waitForInitialMarkPassingCalculation */ true, /* markPassingRaceFingerprintRegistry */ null); // do our calculation, injecting into TrackedRace
        final Competitor GER7897 = getCompetitorByName("Tusch, Frank,Winkelmann, Sven");
        // the start mark passing of Tuschi and Sven is a bit dubious; TracTrac sees them over the line at 2013-05-04T14:50:10+0200
        // but at that time the pin end still seems to be moving to port, and the track seems to pass the pin end on the outside;
        // they wiggle back behind the extension of the line, and around 2013-05-04T14:50:27+0200 they then at last cross the
        // extension of the start line. Depending on the particular behavior of our filter layers (stationary sequence
        // and highest probability in short time range) the one or the other may be picked. We prefer this one: 
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:10+0200"));
        // not this one...
        // assertMarkPassing(GER7897, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:27+0200"));
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 1), DateParser.parse("2013-05-04T14:57:12+0200"));
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 3), DateParser.parse("2013-05-04T15:06:32+0200"));
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 4), DateParser.parse("2013-05-04T15:15:01+0200"));
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 6), DateParser.parse("2013-05-04T15:21:17+0200"));
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 7), DateParser.parse("2013-05-04T15:29:18+0200"));
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 9), DateParser.parse("2013-05-04T15:36:36+0200"));
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 10), DateParser.parse("2013-05-04T15:38:09+0200"));

        final Competitor GER7616 = getCompetitorByName("Seefelder, Jürgen,Seefelder, N"); // starts late
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:57+0200"));
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 1), DateParser.parse("2013-05-04T14:59:05+0200"));
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 3), DateParser.parse("2013-05-04T15:07:32+0200"));
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 4), DateParser.parse("2013-05-04T15:16:40+0200"));
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 6), DateParser.parse("2013-05-04T15:23:26+0200"));
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 7), DateParser.parse("2013-05-04T15:33:01+0200"));
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 9), DateParser.parse("2013-05-04T15:39:25+0200"));
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 10), DateParser.parse("2013-05-04T15:40:51+0200"));

        final Competitor GER8340 = getCompetitorByName("Griese, Achim,Marcour, Michael"); // starts rather early
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:04+0200"));
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 1), DateParser.parse("2013-05-04T14:56:57+0200"));
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 3), DateParser.parse("2013-05-04T15:06:33+0200"));
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 4), DateParser.parse("2013-05-04T15:14:31+0200"));
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 6), DateParser.parse("2013-05-04T15:20:29+0200"));
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 7), DateParser.parse("2013-05-04T15:28:34+0200"));
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 9), DateParser.parse("2013-05-04T15:35:52+0200"));
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 10), DateParser.parse("2013-05-04T15:37:22+0200"));
    }

    private void testOnlyStartOfRace5() throws InvalidDateException {
        // clear mark passings
        for (Competitor c : getRace().getCompetitors()) {
            getTrackedRace().updateMarkPassings(c, Collections.emptySet());
        }
        CandidateFinder finder = new CandidateFinderImpl(getTrackedRace());
        CandidateChooser chooser = new CandidateChooserImpl(getTrackedRace());
        final TimePoint afterStart = new MillisecondsTimePoint(DateParser.parse("2013-05-04T14:52:30+0200"));
        for (Competitor c : getRace().getCompetitors()) {
            calculateMarkPassingsForPartialTrack(c, afterStart, finder, chooser);
        }
        final Competitor GER7897 = getCompetitorByName("Tusch, Frank,Winkelmann, Sven");
        // the start mark passing of Tuschi and Sven is a bit dubious; TracTrac sees them over the line at 2013-05-04T14:50:10+0200
        // but at that time the pin end still seems to be moving to port, and the track seems to pass the pin end on the outside;
        // they wiggle back behind the extension of the line, and around 2013-05-04T14:50:27+0200 they then at last cross the
        // extension of the start line. Depending on the particular behavior of our filter layers (stationary sequence
        // and highest probability in short time range) the one or the other is picked. We prefer this one:
        assertMarkPassing(GER7897, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:10+0200"));
        // not this one:
        // assertMarkPassing(GER7897, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:27+0200"));
        final Competitor GER7616 = getCompetitorByName("Seefelder, Jürgen,Seefelder, N"); // starts late
        assertMarkPassing(GER7616, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:57+0200"));
        final Competitor GER8340 = getCompetitorByName("Griese, Achim,Marcour, Michael"); // starts rather early
        assertMarkPassing(GER8340, Util.get(getWaypoints(), 0), DateParser.parse("2013-05-04T14:50:04+0200"));
    }

    private void assertMarkPassing(Competitor competitor, Waypoint waypoint, Date when) {
        final MarkPassing markPassing = getTrackedRace().getMarkPassing(competitor, waypoint);
        if (markPassing == null) {
            fail("Expected mark passing for "+competitor+" and "+waypoint+" around "+when+" but no mark passing found");
        }
        final Duration offset = new MillisecondsTimePoint(when).until(markPassing.getTimePoint()).abs();
        assertTrue(offset.asSeconds() < 10, "Expected mark passing for "+competitor+" and "+waypoint+" around "+when+
                        " but found one at "+markPassing.getTimePoint()+" which is "+offset+" off.");
    }

    @Test
    public void testRace6() throws Exception {
        testRace("6");
    }

    @Override
    protected String getFileName() {
        return "event_20130424_IDMStarboo-Race_";
    }

    @Override
    protected String getExpectedEventName() {
        return "IDM Starboot 2013";
    }

}
