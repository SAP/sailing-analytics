package com.sap.sailing.domain.yellowbrickadapter.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Parses the JSON output of the YellowBrick end point {@code https://yb.tl/API3/Race/{raceUrl}/GetPositions}. A sample document
 * looks like this:
 * <pre>
 * {
        "raceUrl": "rmsr2019",
        "teams": [
            {
                "serial": 5753,
                "marker": 1,
                "name": "2HARD",
                "positions": [
                    {
                    "sogKmph": 0,
                    "txAt": "2019-10-26T23:03:22Z",
                    "gpsAtMillis": 1572130827000,
                    "altitude": 0,
                    "dtfNm": 202.418,
                    "latitude": 35.8974,
                    "type": "automatic",
                    "battery": 14,
                    "dtfKm": 374.878,
                    "sogKnots": 0,
                    "alert": false,
                    "cog": 219,
                    "id": 112369406,
                    "gpsAt": "2019-10-26T23:00:27Z",
                    "longitude": 14.4991
                    }
                ]
            }
        ]
    }
 * </pre>
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class GetPositionsParser {
    private static final String RACE_URL = "raceUrl";
    private static final String TEAMS = "teams";
    private static final String SERIAL = "serial";
    private static final String MARKER = "marker";
    private static final String NAME = "name";
    private static final String POSITIONS = "positions";
    private static final String GPS_AT_MILLIS = "gpsAtMillis";
    private static final String LATITUDE = "latitude";
    private static final String SOG_KNOTS = "sogKnots";
    private static final String COG = "cog";
    private static final String ID = "id";
    private static final String LONGITUDE = "longitude";
    
    public PositionsDocument parse(Reader reader, boolean inferSpeedAndBearing) throws IOException, ParseException {
        final JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);
        return parse(jsonObject, inferSpeedAndBearing);
    }

    private PositionsDocument parse(JSONObject jsonObject, boolean inferSpeedAndBearing) {
        final String raceUrl = jsonObject.get(RACE_URL).toString();
        final JSONArray teamsJsonArray = (JSONArray) jsonObject.get(TEAMS);
        final Iterable<TeamPositions> teamPositions = parseTeamsPositions(teamsJsonArray, inferSpeedAndBearing);
        return new PositionsDocument(raceUrl, teamPositions);
    }

    private Iterable<TeamPositions> parseTeamsPositions(JSONArray teamsJsonArray, boolean inferSpeedAndBearing) {
        final List<TeamPositions> result = new ArrayList<>(teamsJsonArray.size());
        for (final Object teamObject : teamsJsonArray) {
            final JSONObject teamJson = (JSONObject) teamObject;
            final int serial = ((Number) teamJson.get(SERIAL)).intValue();
            final int marker = ((Number) teamJson.get(MARKER)).intValue();
            final String name = teamJson.get(NAME).toString();
            final JSONArray positionsJson = (JSONArray) teamJson.get(POSITIONS);
            result.add(new TeamPositions(serial, marker, name, parseTeamPositions(positionsJson, inferSpeedAndBearing)));
        }
        return result;
    }

    private Iterable<TeamPosition> parseTeamPositions(JSONArray positionsJson, boolean inferSpeedAndBearing) {
        final List<TeamPosition> result = new ArrayList<>(positionsJson.size());
        GPSFixMoving lastFix = null;
        for (final Object positionObject : positionsJson) {
            final JSONObject positionJson = (JSONObject) positionObject;
            final long timeAsMillis = ((Number) positionJson.get(GPS_AT_MILLIS)).longValue();
            final MillisecondsTimePoint timePoint = new MillisecondsTimePoint(timeAsMillis);
            final double latDeg = ((Number) positionJson.get(LATITUDE)).doubleValue();
            final double lngDeg = ((Number) positionJson.get(LONGITUDE)).doubleValue();
            final DegreePosition position = new DegreePosition(latDeg, lngDeg);
            final Number sogAsNumber = (Number) positionJson.get(SOG_KNOTS);
            final Number cogAsNumber = (Number) positionJson.get(COG);
            final Bearing cog;
            if (cogAsNumber != null) {
                cog = new DegreeBearingImpl(cogAsNumber.doubleValue());
            } else if (inferSpeedAndBearing && lastFix != null && lastFix.getPosition() != null) {
                cog = lastFix.getTimePoint().before(timePoint) ? lastFix.getPosition().getBearingGreatCircle(position)
                        : position.getBearingGreatCircle(lastFix.getPosition());
            } else {
                cog = new DegreeBearingImpl(0.0);
            }
            final double sogInKnots;
            if (sogAsNumber != null) {
                sogInKnots = sogAsNumber.doubleValue();
            } else if (inferSpeedAndBearing && lastFix != null && lastFix.getPosition() != null) {
                sogInKnots = lastFix.getTimePoint().before(timePoint)
                        ? lastFix.getSpeedAndBearingRequiredToReach(new GPSFixImpl(position, timePoint)).getKnots()
                        : new GPSFixImpl(position, timePoint).getSpeedAndBearingRequiredToReach(lastFix).getKnots();
            } else {
                sogInKnots = 0.0;
            }
            final long id = ((Number) positionJson.get(ID)).longValue();
            final TeamPosition teamPosition = new TeamPosition(new KnotSpeedWithBearingImpl(sogInKnots, cog), timePoint, id, position);
            result.add(teamPosition);
            lastFix = teamPosition.getGPSFixMoving();
        }
        return result;
    }
}
