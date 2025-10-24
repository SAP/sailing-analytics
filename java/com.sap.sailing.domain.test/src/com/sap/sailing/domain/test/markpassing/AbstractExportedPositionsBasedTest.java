package com.sap.sailing.domain.test.markpassing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Can read JSON files exported through the
 * <code>/sailingserver/api/v1/regattas/{regattaname}/races/{racename}/competitors/positions</code> and
 * <code>/sailingserver/api/v1/regattas/{regattaname}/races/{racename}/marks/positions</code>.
 * 
 * REST end point.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public abstract class AbstractExportedPositionsBasedTest {
    protected DynamicTrackedRaceImpl readRace(String gzippedJsonCompetitorPositionsResourceName,
            String gzippedJsonMarkPositionsResourceName, BoatClass boatClass) throws IOException, ParseException {
        final JSONParser parser = new JSONParser();
        final JSONObject competitorPositionsJson = (JSONObject) parser.parse(new InputStreamReader(new GZIPInputStream(getClass().getResourceAsStream(gzippedJsonCompetitorPositionsResourceName))));
        final JSONObject markPositionsJson = (JSONObject) parser.parse(new InputStreamReader(new GZIPInputStream(getClass().getResourceAsStream(gzippedJsonMarkPositionsResourceName))));
        assertEquals(competitorPositionsJson.get("name"), markPositionsJson.get("name"));
        assertEquals(competitorPositionsJson.get("regatta"), markPositionsJson.get("regatta"));
        final Map<Mark, Iterable<GPSFix>> markTracks = createMarkTracks(markPositionsJson);
        final Map<String, Mark> marksByName = new HashMap<>();
        for (final Mark mark : markTracks.keySet()) {
            marksByName.put(mark.getName(), mark);
        }
        final Course course = createCourse(marksByName);
        final Regatta regatta = new RegattaImpl((String) competitorPositionsJson.get("regatta"), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ false, null, /* startDate */ null, /* endDate */ null,
                Collections.singleton(new SeriesImpl("Default", /* isMedal */ false,
                        /* isFleetsCanRunInParallel */ true, Collections.singleton(new FleetImpl("Default")),
                        /* raceColumnNames */ Collections.singleton("R1"), /* trackedRegattaRegistry */ null)),
                /* persistent */ false, new LowPoint(), UUID.randomUUID(),
                new CourseAreaImpl("CourseArea", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null), OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        final DynamicTrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        final Map<CompetitorWithBoat, Iterable<GPSFixMoving>> competitorsAndTheirTracks = createCompetitorsAndTheirTracks(competitorPositionsJson, boatClass);
        final Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        for (final CompetitorWithBoat cwb : competitorsAndTheirTracks.keySet()) {
            competitorsAndBoats.put(cwb, cwb.getBoat());
        }
        final DynamicTrackedRaceImpl result = new DynamicTrackedRaceImpl(trackedRegatta,
                new RaceDefinitionImpl((String) competitorPositionsJson.get("name"), course, boatClass,
                        competitorsAndBoats, UUID.randomUUID()),
                /* sidelines */ Collections.emptySet(),
                EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 3000, /* millisecondsOverWhichToAverageWind */ 15000,
                /* millisecondsOverWhichToAverageSpeed */ 10000, /* useInternalMarkPassingAlgorithm */ true,
                OneDesignRankingMetric::new, /* raceLogResolver */ null, /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        result.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 0.0));
        for (final Entry<CompetitorWithBoat, Iterable<GPSFixMoving>> e : competitorsAndTheirTracks.entrySet()) {
            final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = result.getTrack(e.getKey());
            for (final GPSFixMoving fix : e.getValue()) {
                track.add(fix);
            }
        }
        for (final Entry<Mark, Iterable<GPSFix>> e : markTracks.entrySet()) {
            final DynamicGPSFixTrack<Mark, GPSFix> markTrack = result.getOrCreateTrack(e.getKey());
            for (final GPSFix fix : e.getValue()) {
                markTrack.add(fix);
            }
        }
        result.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 1.0));
        result.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0));
        return result;
    }

    protected Course createCourse(Map<String, ControlPoint> controlPoints, Waypoint... waypoints) {
        return new CourseImpl("Course", Arrays.asList(waypoints));
    }

    protected Waypoint wp(Map<String, ControlPoint> controlPoints, String controlPointName, PassingInstruction passingInstruction) {
        return new WaypointImpl(controlPoints.get(controlPointName), passingInstruction);
    }

    private Map<CompetitorWithBoat, Iterable<GPSFixMoving>> createCompetitorsAndTheirTracks(JSONObject competitorPositionsJson, BoatClass boatClass) {
        final JSONArray competitorsJson = (JSONArray) competitorPositionsJson.get("competitors");
        final Map<CompetitorWithBoat, Iterable<GPSFixMoving>> result = new HashMap<>();
        for (final Object competitorJsonObject : competitorsJson) {
            final JSONObject competitorJson = (JSONObject) competitorJsonObject;
            final UUID id = UUID.fromString((String) competitorJson.get("id"));
            final String name = (String) competitorJson.get("name");
            final Color color = AbstractColor.getCssColor((String) competitorJson.get("color"));
            final String sailNumber = (String) competitorJson.get("sailNumber");
            final CompetitorWithBoat competitor = new CompetitorWithBoatImpl(id, name, name, color, /* email */ null, /* flagImage */ null,
                    new TeamImpl(name,
                            Collections.singleton(new PersonImpl(name, new NationalityImpl("GER"),
                                    /* dateOfBirth */ null, /* description */ null)),
                            /* coach */ null),
                            /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, /* searchTag */ null,
                            new BoatImpl(UUID.randomUUID(), name, boatClass, sailNumber));
            final List<GPSFixMoving> fixes = new ArrayList<>();
            final JSONArray fixesAsJson = (JSONArray) competitorJson.get("track");
            for (final Object fixAsJsonObject : fixesAsJson) {
                final JSONObject fixAsJson = (JSONObject) fixAsJsonObject;
                GPSFixMoving fix = new GPSFixMovingImpl(new DegreePosition((Double) fixAsJson.get("lat-deg"), (Double) fixAsJson.get("lng-deg")),
                        new MillisecondsTimePoint((Long) fixAsJson.get("timepoint-ms")),
                                new KnotSpeedWithBearingImpl((Double) fixAsJson.get("speed-kts"),
                                        new DegreeBearingImpl((Double) fixAsJson.get("truebearing-deg"))), /* optionalTrueHeading */ null);
                fixes.add(fix);
            }
            result.put(competitor, fixes);
        }
        return result;
    }

    protected abstract Course createCourse(Map<String, Mark> marksByName);

    private Map<Mark, Iterable<GPSFix>> createMarkTracks(JSONObject markPositionsJson) {
        final Map<Mark, Iterable<GPSFix>> result = new HashMap<>();
        final JSONArray marksJson = (JSONArray) markPositionsJson.get("marks");
        for (final Object markJsonObject : marksJson) {
            final JSONObject markJson = (JSONObject) markJsonObject;
            final String name = (String) markJson.get("name");
            final UUID id = UUID.fromString((String) markJson.get("id"));
            final JSONArray track = (JSONArray) markJson.get("track");
            final List<GPSFix> fixes = new ArrayList<>();
            for (final Object fixJsonObject : track) {
                final JSONObject fixAsJson = (JSONObject) fixJsonObject;
                final GPSFix fix = new GPSFixImpl(new DegreePosition((Double) fixAsJson.get("lat-deg"), (Double) fixAsJson.get("lng-deg")),
                        new MillisecondsTimePoint((Long) fixAsJson.get("timepoint-ms")));
                fixes.add(fix);
            }
            result.put(new MarkImpl(id, name), fixes);
        }
        return result;
    }
}
