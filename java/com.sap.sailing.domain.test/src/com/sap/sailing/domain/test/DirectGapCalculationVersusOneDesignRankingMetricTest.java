package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * This is a test for bug 2009 (see http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2009).
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class DirectGapCalculationVersusOneDesignRankingMetricTest extends AbstractManeuverDetectionTestCase {
    public DirectGapCalculationVersusOneDesignRankingMetricTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Override
    protected String getExpectedEventName() {
        return "Kieler Woche 2014 - Olympic Week";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20140619_KieleWoche-R1_Blue_Laser.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20140619_KieleWoche-R1_Blue_Laser.txt").getCanonicalPath()),
                /* liveUri */null, /* storedUri */storedUri, new ReceiverType[] { ReceiverType.MARKPASSINGS,
                        ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, new MillisecondsTimePoint(dateFormat.parse("06/21/2014-13:03:35")),
                        new KnotSpeedWithBearingImpl(18, new DegreeBearingImpl(90))),
                new WindSourceImpl(WindSourceType.WEB));
    }
    
    /**
     * Asserts that Philipp Buhl is having equal gap value, regardless whether calculated using the traditional
     * {@link TrackedLegOfCompetitor#getGapToLeader(TimePoint, RankingInfo, WindPositionMode)} method or the
     * {@link OneDesignRankingMetric#getGapToLeaderInOwnTime(Competitor, TimePoint)} method.
     */
    @Test
    public void testBuhlisGapsAtVariousTimePoints() throws ParseException, NoWindException {
        Competitor buhli = getCompetitorByName("Buhl");
        TimePoint timePoint = getTrackedRace().getStartOfRace();
        for (int i=1; i<10; i++) {
            timePoint = timePoint.plus(Duration.ONE_MINUTE);
            Duration classicGap = getTrackedRace().getTrackedLeg(buhli, timePoint).getGapToLeader(timePoint,
                    getTrackedRace().getRankingMetric().getRankingInfo(timePoint), WindPositionMode.LEG_MIDDLE);
            Duration rankingMetricGap = getTrackedRace().getRankingMetric().getGapToLeaderInOwnTime(buhli, timePoint);
            assertEquals(classicGap.asSeconds(), rankingMetricGap.asSeconds(), 0.000001, "At "+i+" minutes into the race ("+timePoint+"): ");
        }
    }
    
    @Test
    public void testEqualLeaders() {
        TimePoint timePoint = getTrackedRace().getStartOfRace();
        for (int i=1; i<10; i++) {
            timePoint = timePoint.plus(Duration.ONE_MINUTE);
            Competitor classicOverallLeader = getTrackedRace().getOverallLeader(timePoint);
            Competitor oneDesignRankingMetricLeader = getTrackedRace().getRankingMetric()
                    .getRankingInfo(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint))
                    .getLeaderByCorrectedEstimatedTimeToCompetitorFarthestAhead();
            assertSame(classicOverallLeader, oneDesignRankingMetricLeader, "At "+i+" minutes into the race ("+timePoint+"): ");
        }
    }
}
