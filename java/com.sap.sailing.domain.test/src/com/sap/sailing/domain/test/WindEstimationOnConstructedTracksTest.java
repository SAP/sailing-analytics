package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.CombinedWindTrackImpl;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.TrackBasedEstimationWindTrackImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;

public class WindEstimationOnConstructedTracksTest extends StoredTrackBasedTest {
    private List<Competitor> competitors;
    private static final String[] competitorNames = new String[] { "Wolfgang Hunger", "Dr. Hasso Plattner",  "Robert Stanjek", "Simon Grotelueschen" };
    
    @Before
    public void setUp() {
        competitors = new ArrayList<Competitor>();
        for (String name : competitorNames) {
            competitors.add(createCompetitor(name));
        }
    }
    
    private void initRace(int numberOfCompetitorsToUse, int[] numberOfMarksPassed, TimePoint timePointForFixes) {
        setTrackedRace(createTestTrackedRace("Kieler Woche", "505 Race 2", "505",
                competitors.subList(0, numberOfCompetitorsToUse), timePointForFixes));
        for (int i=0; i<numberOfCompetitorsToUse; i++) {
            initializeMarkPassingForStartGate(competitors.get(i), numberOfMarksPassed[i], timePointForFixes);
        }
    }

    private void initializeMarkPassingForStartGate(Competitor competitor, int numberOfMarksPassed, TimePoint timePointForFixes) {
        TimePoint fixTimePoint = new MillisecondsTimePoint(timePointForFixes.asMillis());
        Set<MarkPassing> markPassingForCompetitor = new HashSet<MarkPassing>();
        int i=0;
        for (Waypoint waypoint : getTrackedRace().getRace().getCourse().getWaypoints()) {
            if (i++ >= numberOfMarksPassed) {
                break;
            }
            markPassingForCompetitor.add(new MarkPassingImpl(fixTimePoint, waypoint, competitor));
            fixTimePoint = new MillisecondsTimePoint(fixTimePoint.asMillis()+1);
        }
        getTrackedRace().updateMarkPassings(competitor, markPassingForCompetitor);
    }

    private void setBearingForCompetitor(Competitor competitor, TimePoint timePoint, double bearingDeg) {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> competitorTrack = getTrackedRace().getTrack(competitor);
        competitorTrack.addGPSFix(new GPSFixMovingImpl(new DegreePosition(54.4680424, 10.234451), timePoint,
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(bearingDeg))));
    }

    /**
     * Uses competitors on upwind leg to cause a valid wind estimation. Additionally, for the same time point, adds a
     * wind fix to the {@link WindSourceType#WEB} wind track that is slightly different from the expected estimation
     * result. Then, asks the tracked race for the combined wind direction. The expected outcome is that the larger
     * the minimal cluster size (boats on the same tack going upwind), the more confident the wind estimation is,
     * and subsequently the more it is considered in averaging between the {@link WindSourceType#WEB} wind track
     * and the estimation wind track.
     */
    @Test
    public void testWindEstimationPreferringLargerClusters() throws NoWindException {
        initRace(4, new int[] { 1, 1, 1, 1 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = new MillisecondsTimePoint(new GregorianCalendar(2012, 03, 14).getTime());
        getTrackedRace().recordWind(new WindImpl(null, now, new KnotSpeedWithBearingImpl(/* speedInKnots */ 12, new DegreeBearingImpl(180.))),
                new WindSourceImpl(WindSourceType.WEB));
        // produces estimated bearing of 170deg; result should be averaged between 170 (estimation) and 180 (web) deg
        setBearingForCompetitor(competitors.get(0), now, 305);
        setBearingForCompetitor(competitors.get(1), now, 35);
        WindWithConfidence<Pair<Position, TimePoint>> combinedWindDirectionMinClusterSizeOne = getTrackedRace()
                .getWindWithConfidence(/* position */null, now);
        final double combinedDegreesMinClusterSizeOne = combinedWindDirectionMinClusterSizeOne.getObject().getBearing().getDegrees();
        assertTrue(combinedDegreesMinClusterSizeOne > 170 && combinedDegreesMinClusterSizeOne < 180);
        // now produce a minimum cluster size of 2, raising the estimation's confidence
        setBearingForCompetitor(competitors.get(2), now, 305);
        setBearingForCompetitor(competitors.get(3), now, 35);
        WindWithConfidence<Pair<Position, TimePoint>> combinedWindDirectionMinClusterSizeTwo = getTrackedRace()
                .getWindWithConfidence(/* position */null, now);
        final double combinedDegreesNow = combinedWindDirectionMinClusterSizeTwo.getObject().getBearing().getDegrees();
        assertTrue(combinedDegreesNow > 170 && combinedDegreesNow < 180);
        // we expect the combined direction now to be closer to the estimation as compared to before because the estimation is more confident
        // since the minimum cluster size is 2 instead of 1
        assertTrue("expected combinedDegreesNow ("+combinedDegreesNow+
                ") < combinedDegreesMinClusterSizeOne ("+combinedDegreesMinClusterSizeOne+")",
                combinedDegreesNow < combinedDegreesMinClusterSizeOne);
    }

    @Test
    public void testCombinedWindTrack() throws NoWindException {
        initRace(4, new int[] { 1, 1, 2, 2 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 315);
        setBearingForCompetitor(competitors.get(1), now, 45); // on the same tack, should give no read-out
        setBearingForCompetitor(competitors.get(2), now, 135);
        setBearingForCompetitor(competitors.get(3), now, 225); // on the same tack, should give no read-out
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, now);
        // less precision because downwind estimation has less confidence
        assertEquals(180., estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
        CombinedWindTrackImpl combinedTrack = new CombinedWindTrackImpl(getTrackedRace(), WindSourceType.COMBINED.getBaseConfidence());
        Wind combinedWindDirection = combinedTrack.getAveragedWind(/* position */ null, now);
        //Since the course layout makes a wind estimation around 7� and the confidence of the cluster based wind estimation is not
        // way higher that that of the course layout wind, we will not end up with exactly 180 deg.
        assertEquals(180.2, combinedWindDirection.getBearing().getDegrees(), 0.1);
    }

    /**
     * See <a href="http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=166">bug #166</a>
     */
    @Test
    public void testWindEstimationCacheInvalidationAfterLegTypeChange() throws NoWindException {
        TimePoint fixTime = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime());
        TimePoint checkTime = new MillisecondsTimePoint(fixTime.asMillis()+60000); // one minute later
        initRace(4, new int[] { 1, 1, 1, 1 }, fixTime);
        getTrackedRace().setRaceIsKnownToStartUpwind(false); // use only WEB wind to determine leg type
        TimePoint now = checkTime;
        setBearingForCompetitor(competitors.get(0), now, 320);
        setBearingForCompetitor(competitors.get(1), now, 50);
        setBearingForCompetitor(competitors.get(2), now, 140);
        setBearingForCompetitor(competitors.get(3), now, 230);
        TrackedLeg firstLeg = getTrackedRace().getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().iterator().next());
        assertEquals(LegType.UPWIND, firstLeg.getLegType(new MillisecondsTimePoint(MillisecondsTimePoint.now().asMillis())));
        final Map<TimePoint, WindWithConfidence<TimePoint>> cachedFixes = new HashMap<TimePoint, WindWithConfidence<TimePoint>>();
        TrackBasedEstimationWindTrackImpl track = new TrackBasedEstimationWindTrackImpl(
                getTrackedRace(), /* millisecondsOverWhichToAverage */ 30000, WindSourceType.TRACK_BASED_ESTIMATION.getBaseConfidence(),
                /* delay for cache invalidation in milliseconds */ 0l) {
                    private static final long serialVersionUID = -4540785297605915273L;

                    @Override
                    protected void cache(TimePoint timePoint, WindWithConfidence<TimePoint> fix) {
                        super.cache(timePoint, fix);
                        if (fix != null) {
                            cachedFixes.put(timePoint, fix);
                        }
                    }
        };
        Wind estimatedWindDirection = track.getAveragedWind(/* position */ null, checkTime);
        assertNotNull(estimatedWindDirection);
        assertEquals(185., estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
        assertFalse(cachedFixes.isEmpty());
        assertEquals(185., cachedFixes.values().iterator().next().getObject().getBearing().getDegrees(), 0.00000001);
        // now invert leg's type by moving the top mark along the wind from the leeward gate:
        Iterator<Waypoint> waypointsIter = getTrackedRace().getRace().getCourse().getWaypoints().iterator();
        Waypoint leewardMark = waypointsIter.next();
        Waypoint windwardMark = waypointsIter.next();
        Position leewardGatePosition = getTrackedRace().getApproximatePosition(leewardMark, checkTime);
        Distance d = new NauticalMileDistance(1);
        Wind wind = getTrackedRace().getWind(null, checkTime, getTrackedRace().getWindSources(WindSourceType.TRACK_BASED_ESTIMATION));
        Position newWindwardMarkPosition = leewardGatePosition.translateGreatCircle(wind.getBearing(), d);
        getTrackedRace().getOrCreateTrack(windwardMark.getMarks().iterator().next()).addGPSFix(
                new GPSFixImpl(newWindwardMarkPosition, checkTime));
        assertEquals(LegType.DOWNWIND, firstLeg.getLegType(fixTime));
        Wind estimatedWindDirectionDownwind = track.getAveragedWind(/* position */ null, checkTime);
        assertNotNull(estimatedWindDirectionDownwind);
        assertEquals(185., estimatedWindDirectionDownwind.getBearing().getDegrees(), 0.00000001);
    }
    
    @Test
    public void testWindEstimationCaching() {
        MillisecondsTimePoint fixTime = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime());
        TimePoint checkTime = new MillisecondsTimePoint(fixTime.asMillis()+60000); // one minute later
        initRace(2, new int[] { 1, 1 }, fixTime);
        TimePoint now = checkTime;
        setBearingForCompetitor(competitors.get(0), checkTime, 320);
        setBearingForCompetitor(competitors.get(1), checkTime, 50);
        final Map<TimePoint, WindWithConfidence<TimePoint>> cachedFixes = new HashMap<TimePoint, WindWithConfidence<TimePoint>>();
        TrackBasedEstimationWindTrackImpl track = new TrackBasedEstimationWindTrackImpl(
                getTrackedRace(), /* millisecondsOverWhichToAverage */ 30000, WindSourceType.TRACK_BASED_ESTIMATION.getBaseConfidence(),
                /* delay for cache invalidation in milliseconds */ 0l) {
                    private static final long serialVersionUID = -2264781053554744932L;

                    @Override
                    protected void cache(TimePoint timePoint, WindWithConfidence<TimePoint> fix) {
                        super.cache(timePoint, fix);
                        if (fix != null) {
                            cachedFixes.put(timePoint, fix);
                        }
                    }
        };
        Wind estimatedWindDirection = track.getAveragedWind(/* position */ null, checkTime);
        assertNotNull(estimatedWindDirection);
        assertEquals(185., estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
        assertFalse(cachedFixes.isEmpty());
        assertEquals(185., cachedFixes.values().iterator().next().getObject().getBearing().getDegrees(), 0.00000001);
        // now clear set of cached fixes, ask again and ensure nothing is cached again:
        cachedFixes.clear();
        Wind estimatedWindDirectionCached = track.getAveragedWind(/* position */ null, checkTime);
        assertTrue(cachedFixes.isEmpty());
        assertNotNull(estimatedWindDirectionCached);
        assertEquals(185., estimatedWindDirectionCached.getBearing().getDegrees(), 0.00000001);
        // now add a GPS fix and make sure the cache is invalidated by adding it inside the averaging interval
        now = new MillisecondsTimePoint(checkTime.asMillis() + track.getMillisecondsOverWhichToAverageWind()/2);
        setBearingForCompetitor(competitors.get(0), now, 330);
        Wind estimatedWindDirectionNew = track.getAveragedWind(/* position */ null, checkTime);
        assertFalse(cachedFixes.isEmpty());
        assertNotNull(estimatedWindDirectionNew);
        assertTrue("Expected estimated wind direction to now be greater than 185 degrees but was "
                + estimatedWindDirectionCached.getBearing().getDegrees(), estimatedWindDirectionCached.getBearing()
                .getDegrees() < 185.); // remember: bearing is opposite of from; boats start with upwind
    }
    
    @Test
    public void testWindEstimationForSimpleTracks() throws NoWindException {
        initRace(2, new int[] { 1, 1 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 320);
        setBearingForCompetitor(competitors.get(1), now, 50);
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, now);
        assertEquals(185., estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testWindEstimationForSimpleTracksWithOneAncientFixToBeSuppressedByLowConfidence() throws NoWindException {
        initRace(3, new int[] { 1, 1, 1 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 320);
        setBearingForCompetitor(competitors.get(1), now, 50);
        setBearingForCompetitor(competitors.get(2), new MillisecondsTimePoint(0), 100); // this shouldn't disturb the estimation because it's too old
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, now);
        assertEquals(185., estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testWindEstimationForSimpleTracksWithOneFixNearMarkPassingToBeSuppressedByLowConfidence() throws NoWindException {
        TimePoint markPassingTimePoint = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime());
        initRace(3, new int[] { 1, 1, 1 }, markPassingTimePoint);
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 320);
        setBearingForCompetitor(competitors.get(1), now, 50);
        setBearingForCompetitor(competitors.get(2), markPassingTimePoint, 100); // this shouldn't disturb the estimation because it's too old
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, now);
        assertEquals(185., estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testWindEstimationForTwoBoatsOnSameTack() throws NoWindException {
        initRace(2, new int[] { 1, 1 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 320);
        setBearingForCompetitor(competitors.get(1), now, 330); // on the same tack, should give no read-out
        Wind nullWind = getTrackedRace().getEstimatedWindDirection(/* position */null, now);
        assertNull(
                "Shouldn't have been able to determine estimated wind direction because no two distinct direction clusters found upwind nor downwind",
                nullWind);
    }

    @Test
    public void testWindEstimationForFourBoatsOnSameTack() throws NoWindException {
        initRace(4, new int[] { 1, 1, 2, 2 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 320);
        setBearingForCompetitor(competitors.get(1), now, 330); // on the same tack, should give no read-out
        setBearingForCompetitor(competitors.get(2), now, 135);
        setBearingForCompetitor(competitors.get(3), now, 145); // on the same tack, should give no read-out
        Wind nullWind = getTrackedRace().getEstimatedWindDirection(/* position */null, now);
        assertNull(
                "Shouldn't have been able to determine estimated wind direction because no two distinct direction clusters found upwind nor downwind",
                nullWind);
    }

    @Test
    public void testWindEstimationForFourBoatsWithUpwindOnSameTack() throws NoWindException {
        initRace(4, new int[] { 1, 1, 2, 2 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 320);
        setBearingForCompetitor(competitors.get(1), now, 330); // on the same tack, should give no read-out
        setBearingForCompetitor(competitors.get(2), now, 135);
        setBearingForCompetitor(competitors.get(3), now, 220); // on the same tack, should give no read-out
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, now);
        assertEquals(177.5, estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testWindEstimationForFourBoats() throws NoWindException {
        initRace(4, new int[] { 1, 1, 2, 2 }, new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        MillisecondsTimePoint now = MillisecondsTimePoint.now();
        setBearingForCompetitor(competitors.get(0), now, 315);
        setBearingForCompetitor(competitors.get(1), now, 45); // on the same tack, should give no read-out
        setBearingForCompetitor(competitors.get(2), now, 135);
        setBearingForCompetitor(competitors.get(3), now, 225); // on the same tack, should give no read-out
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, now);
        assertEquals(180., estimatedWindDirection.getBearing().getDegrees(), 0.00000001);
    }

}
