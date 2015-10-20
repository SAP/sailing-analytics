package com.sap.sailing.server.gateway.impl.rc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;

import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RaceLogDefinedMarkFinder;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.racelog.RaceLogServletConstants;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.gateway.AbstractJsonHttpServlet;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;

public class MarksJsonExportServlet extends AbstractJsonHttpServlet {
    private static final long serialVersionUID = 4510175441769759252L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String leaderboardName = request.getParameter(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME);
        if (leaderboardName == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    String.format("Missing parameter '%s'.", RaceLogServletConstants.PARAMS_LEADERBOARD_NAME));
            return;
        }
        String raceColumnName = request.getParameter(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME);
        if (raceColumnName == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    String.format("Missing parameter '%s'.", RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME));
            return;
        }
        String fleetName = request.getParameter(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME);
        if (fleetName == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    String.format("Missing parameter '%s'.", RaceLogServletConstants.PARAMS_RACE_FLEET_NAME));
            return;
        }
        RacingEventService service = getService();
        Leaderboard leaderboard = service.getLeaderboardByName(leaderboardName);
        if (leaderboard == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such leaderboard found.");
            return;
        }
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        if (raceColumn == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such race column found.");
            return;
        }
        Fleet fleet = raceColumn.getFleetByName(fleetName);
        if (fleet == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such fleet found.");
            return;
        }
        TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
        JSONArray result = new JSONArray();
        final Iterable<Mark> marks;
        if (trackedRace != null) {
            marks = trackedRace.getMarks();
        } else {
            final List<Mark> marksList = new ArrayList<>();
            // no tracked race associated yet; grab the mark definitions from the race log:
            for (Mark markDefinitionFromRaceLog : new RaceLogDefinedMarkFinder(raceColumn.getRaceLog(fleet)).analyze()) {
                marksList.add(markDefinitionFromRaceLog);
            }
            marks = marksList;
        }
        MarkJsonSerializer serializer = new MarkJsonSerializer();
        for (Mark mark : marks) {
            if (toUUID(mark.getId().toString()) != null) {
                result.add(serializer.serialize(mark));
            }
        }
        setJsonResponseHeader(response);
        result.writeJSONString(response.getWriter());
    }
    
    private UUID toUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }
}
