package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ManeuverDetectionOnMdM2011SemifinalTest extends OnlineTracTracBasedTest {

    public ManeuverDetectionOnMdM2011SemifinalTest() throws MalformedURLException, URISyntaxException {
    }
    
    @Override
    protected String getExpectedEventName() {
        return "Academy Tracking 2011";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        super.setUp("event_20110505_SailingTea", // Semifinale
                /* raceId */ "01ea3604-02ef-11e1-9efc-406186cbf87c",
                /* liveUri */ null, /* storedUri */ null,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        TimePoint epoch = new MillisecondsTimePoint(0l);
        TimePoint now = MillisecondsTimePoint.now();
        Map<String, Position> markPositions = new HashMap<String, Position>();
        markPositions.put("CR Start - 1", new DegreePosition(53.562944999999985, 10.010104000000046));
        markPositions.put("CR Start - 2", new DegreePosition(53.562944999999985, 10.010104000000046));
        markPositions.put("Leeward mark", new DegreePosition(53.562145000000015, 10.009252));
        markPositions.put("Luvtonne", new DegreePosition(53.560581899999995, 10.005657));
        for (Waypoint w : getTrackedRace().getRace().getCourse().getWaypoints()) {
            for (Mark mark : w.getMarks()) {
                getTrackedRace().getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), epoch));
                getTrackedRace().getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), now));
            }
        }
        getTrackedRace().recordWind(new WindImpl(/* position */ null, MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(51))), new WindSourceImpl(WindSourceType.WEB));
    }
    
    @Test
    public void testManeuversForDennis() throws NoWindException {
        Competitor dennis = getCompetitorByName("Gehrlein");
        NavigableSet<MarkPassing> dennisMarkPassings = getTrackedRace().getMarkPassings(dennis);
        Iterable<Maneuver> maneuvers = getTrackedRace().getManeuvers(dennis, dennisMarkPassings.first().getTimePoint(),
                dennisMarkPassings.last().getTimePoint(), /* waitForLatest */ true);
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("Europe/Berlin"));
        c.set(2011, 10-1, 30, 13, 32, 42);
        assertManeuver(maneuvers, ManeuverType.TACK, Tack.STARBOARD, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 3000);
        c.set(2011, 10-1, 30, 13, 34, 00);
        assertManeuver(maneuvers, ManeuverType.TACK, Tack.PORT, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 3000);
        c.set(2011, 10-1, 30, 13, 35, 46);
        // tolerance for the JIBE maneuver around 13:35:46 needs to be a bit greater than for the others because depending on coincidental
        // Douglas-Peucker approximation, the DP point after the jibe may either be detected at 13:35:51 which will still make it inside
        // the bounds for grouping DP points into one maneuver, or the DP point is put to 13:35:54 which then is one second too late
        // to be grouped into the same maneuver as the DP point before the jibe which is at 13:35:45. In that case, the jibe is detected
        // at 13:35:54 with a much smaller jibing angle while the maneuver before which otherwise is grouped into the jibe then is
        // only a bearing away by a lot.
        assertManeuver(maneuvers, ManeuverType.JIBE, Tack.PORT, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 10000);
        c.set(2011, 10-1, 30, 13, 36, 49);
        assertManeuver(maneuvers, ManeuverType.JIBE, Tack.STARBOARD, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 3000);
    }

    private void assertManeuver(Iterable<Maneuver> maneuvers, ManeuverType type, Tack newTack,
            MillisecondsTimePoint timePoint, int toleranceInMilliseconds) {
        for (Maneuver maneuver : maneuvers) {
            if (maneuver.getType() == type && (newTack == null || newTack == maneuver.getNewTack()) &&
                    Math.abs(maneuver.getTimePoint().asMillis() - timePoint.asMillis()) <= toleranceInMilliseconds) {
                return;
            }
        }
        fail("Didn't find maneuver type " + type + (newTack == null ? "" : " to new tack " + newTack) + " in "
                + toleranceInMilliseconds + "ms around " + timePoint);
    }

}
