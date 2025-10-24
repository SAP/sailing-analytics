package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TackType;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TackTypeTest extends OnlineTracTracBasedTest {
    public TackTypeTest() throws MalformedURLException, URISyntaxException {
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"
                + new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri, new ReceiverType[] { ReceiverType.MARKPASSINGS,
                        ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(),
                new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, MillisecondsTimePoint.now(),
                        new KnotSpeedWithBearingImpl(8, new DegreeBearingImpl(220))),
                new WindSourceImpl(WindSourceType.WEB));
    }

    @Test
    public void TestShortTack() throws NoWindException {
        final Competitor findel = getCompetitorByName("Findel");
        final TrackedLegOfCompetitor findelsFirstLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(0)).getTrackedLeg(findel);
        final TimePoint findelStartedHisFirstLegAt = findelsFirstLeg.getStartTime();
        final TackType actualTackType = findelsFirstLeg.getTackType(findelStartedHisFirstLegAt.plus(30000));
        assertEquals(TackType.SHORTTACK, actualTackType);
    }

    @Test
    public void TestLongTack() throws NoWindException {
        final Competitor findel = getCompetitorByName("Findel");
        final TrackedLegOfCompetitor findelsFirstLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(0)).getTrackedLeg(findel);
        final TimePoint findelStartedHisFirstLegAt = findelsFirstLeg.getStartTime();
        final TackType actualTackType = findelsFirstLeg.getTackType(findelStartedHisFirstLegAt.plus(90000));
        assertEquals(TackType.LONGTACK, actualTackType);
    }
   
    @Test
    public void TestAfterLeg() throws NoWindException {
        final Competitor findel = getCompetitorByName("Findel");
        final TrackedLegOfCompetitor findelsFirstLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(0)).getTrackedLeg(findel);
        final TrackedLegOfCompetitor findelsSecondLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(1)).getTrackedLeg(findel);
        final TimePoint findelStartedHisSecondLegAt = findelsSecondLeg.getStartTime();
        final TackType actualTackType = findelsFirstLeg.getTackType(findelStartedHisSecondLegAt.plus(90000));
        assertEquals(null, actualTackType);
    }

    @Test
    public void TestBeforeLeg() throws NoWindException {
        final Competitor findel = getCompetitorByName("Findel");
        final TrackedLegOfCompetitor findelsFirstLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(0)).getTrackedLeg(findel);
        final TrackedLegOfCompetitor findelsSecondLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(1)).getTrackedLeg(findel);
        final TimePoint findelStartedHisFirstLegAt = findelsFirstLeg.getStartTime();
        final TackType actualTackType = findelsSecondLeg.getTackType(findelStartedHisFirstLegAt.plus(90000));
        assertEquals(null, actualTackType);
    }

    @Test
    public void TestNoGPSFix() throws NoWindException {
        final Competitor findel = getCompetitorByName("Findel");
        final TimePoint endOfFindelsRace = getTrackedRace().getEndOfRace();
        final List<Leg> listOfFindelsLegs = getTrackedRace().getRace().getCourse().getLegs();
        List<MarkPassing> newMarkPassings = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            newMarkPassings.add(
                    new MarkPassingImpl(endOfFindelsRace.plus(9000 * i), listOfFindelsLegs.get(i).getTo(), findel));
        }
        getTrackedRace().updateMarkPassings(findel, newMarkPassings);
        final TrackedLegOfCompetitor findelsFirstLeg = getTrackedRace().getTrackedLeg(listOfFindelsLegs.get(0))
                .getTrackedLeg(findel);
        final TackType actualTackType = findelsFirstLeg.getTackType(endOfFindelsRace.plus(9000));
        assertEquals(null, actualTackType);
    }

    @Test
    public void TestNoWaypoint() throws NoWindException {
        final Competitor findel = getCompetitorByName("Findel");
        getTrackedRace().getRace().getCourse().addWaypoint(1,
                new WaypointImpl(getDomainFactory().getBaseDomainFactory().getOrCreateMark("TestMark")));
        final TrackedLegOfCompetitor findelsFirstLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(0)).getTrackedLeg(findel);
        final TimePoint findelStartedHisFirstLegAt = findelsFirstLeg.getStartTime();
        final TackType actualTackType = findelsFirstLeg.getTackType(findelStartedHisFirstLegAt.plus(30000));
        assertEquals(null, actualTackType);
    }

    @Test
    public void TestNoWind() throws NoWindException {
        final Competitor findel = getCompetitorByName("Findel");
        getTrackedRace().setWindSourcesToExclude(getTrackedRace().getWindSources());
        final TrackedLegOfCompetitor findelsFirstLeg = getTrackedRace()
                .getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(0)).getTrackedLeg(findel);
        final TimePoint findelStartedHisFirstLegAt = findelsFirstLeg.getStartTime();
        final TackType actualTackType = findelsFirstLeg.getTackType(findelStartedHisFirstLegAt.plus(30000));
        assertEquals(null, actualTackType);
    }
}