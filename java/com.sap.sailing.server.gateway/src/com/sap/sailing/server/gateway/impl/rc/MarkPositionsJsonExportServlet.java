package com.sap.sailing.server.gateway.impl.rc;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.racelog.RaceLogServletConstants;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.AbstractJsonHttpServlet;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PositionedMarkJsonSerializer;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class MarkPositionsJsonExportServlet extends AbstractJsonHttpServlet {

    private static final long serialVersionUID = -5489857104922281761L;
    
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
        SecurityUtils.getSubject()
                .checkPermission(leaderboard.getIdentifier().getStringPermission(DefaultActions.READ));

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
        
        if (trackedRace != null) {
            SecurityUtils.getSubject()
                    .checkPermission(trackedRace.getIdentifier().getStringPermission(DefaultActions.READ));

            MarkJsonSerializer markSerializer = new MarkJsonSerializer();
            PositionJsonSerializer positionSerializer = new PositionJsonSerializer();
            PositionedMarkJsonSerializer positionedMarkSerializer = new PositionedMarkJsonSerializer(markSerializer, positionSerializer);
            
            //TODO propagate only marks that are on the course area of the race
            //Position courseAreaCenterPosition = leaderboard.getDefaultCourseArea().getPosition();
            //Distance courseAreaRadius = leaderboard.getDefaultCourseArea().getRadius();
            for (Mark mark : trackedRace.getMarks()) {
                if (toUUID(mark.getId().toString()) != null) {
                    Position position = trackedRace.getOrCreateTrack(mark).getEstimatedPosition(MillisecondsTimePoint.now(), false);
                    
                    //The position of the mark needs to be in the course area
                    //if (isMarkInCourseArea(courseAreaCenterPosition, courseAreaRadius, position)) {
                        result.add(positionedMarkSerializer.serialize(new Util.Pair<Mark, Position>(mark, position)));
                    //}
                    
                }
            }
        }
        
        setJsonResponseHeader(response);
        result.writeJSONString(response.getWriter());
    }

    @SuppressWarnings("unused") //unused since course area has no position yet
    private boolean isMarkInCourseArea(Position courseAreaCenterPosition, Distance courseAreaRadius, Position position) {
        return position.getDistance(courseAreaCenterPosition).compareTo(courseAreaRadius) < 0;
    }
    
    private UUID toUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException iae) {
            return null;
        }
    }
}
