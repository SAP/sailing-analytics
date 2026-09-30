package com.sap.sailing.server.gateway.impl.rc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.common.racelog.RaceLogServletConstants;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.server.gateway.AbstractJsonHttpServlet;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ColorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.FleetJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RegattaConfigurationJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TargetTimeInfoSerializer;
import com.sap.sailing.server.gateway.serialization.impl.WindJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racegroup.impl.RaceCellJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racegroup.impl.RaceGroupJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racegroup.impl.RaceRowJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racegroup.impl.RaceRowsOfSeriesWithRowsSerializer;
import com.sap.sailing.server.gateway.serialization.racegroup.impl.SeriesWithRowsJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racegroup.impl.SeriesWithRowsOfRaceGroupSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogEventSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogSerializer;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.shared.json.JsonSerializer;

public class RaceGroupJsonExportServlet extends AbstractJsonHttpServlet {
    private static final long serialVersionUID = 4510175441769759252L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String courseAreaFilter = request.getParameter(RaceLogServletConstants.PARAMS_COURSE_AREA_FILTER);
        if (courseAreaFilter == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Need to set a course area filter.");
            return;
        }
        UUID courseAreaId = toUUID(courseAreaFilter);
        if (courseAreaId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Course area filter must be valid UUID.");
            return;
        }
        CourseArea filterCourseArea = getService().getCourseArea(courseAreaId);
        if (filterCourseArea == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No course area found with given UUID.");
            return;
        }
        String clientUuidAsString = request.getParameter(RaceLogServletConstants.PARAMS_CLIENT_UUID);
        final UUID clientUuid;
        if (clientUuidAsString == null) {
            clientUuid = null;
        } else {
            clientUuid = UUID.fromString(clientUuidAsString);
        }
        JsonSerializer<RaceGroup> serializer = createSerializer(clientUuid);
        JSONArray result = new JSONArray();
        RaceGroupFactory raceGroupFactory = new RaceGroupFactory();
        final Set<Regatta> regattasForWhichRegattaLeaderboardsWereAdded = new HashSet<>();
        for (Leaderboard leaderboard : getService().getLeaderboards().values()) {
            if (Util.contains(leaderboard.getCourseAreas(), filterCourseArea)) {
                SecurityUtils.getSubject()
                        .checkPermission(leaderboard.getIdentifier().getStringPermission(DefaultActions.READ));
                if (leaderboard instanceof RegattaLeaderboard && !(leaderboard instanceof RegattaLeaderboardWithEliminations)) {
                    result.add(serializer.serialize(raceGroupFactory.convert((RegattaLeaderboard) leaderboard)));
                    final Regatta regatta = ((RegattaLeaderboard) leaderboard).getRegatta();
                    SecurityUtils.getSubject()
                            .checkPermission(regatta.getIdentifier().getStringPermission(DefaultActions.READ));
                    regattasForWhichRegattaLeaderboardsWereAdded.add(regatta);
                } else if (leaderboard instanceof FlexibleLeaderboard) {
                    result.add(serializer.serialize(raceGroupFactory.convert((FlexibleLeaderboard) leaderboard)));
                }
            }
        }
        // now add only those RegattaLeaderboardWithEliminations for which the original RegattaLeaderboard hasn't been added
        for (Leaderboard leaderboard : getService().getLeaderboards().values()) {
            if (leaderboard instanceof RegattaLeaderboardWithEliminations &&
                    Util.contains(leaderboard.getCourseAreas(), filterCourseArea) && 
                    !regattasForWhichRegattaLeaderboardsWereAdded.contains(((RegattaLeaderboardWithEliminations) leaderboard).getRegatta())) {
                result.add(serializer.serialize(raceGroupFactory.convert((RegattaLeaderboardWithEliminations) leaderboard)));
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

    private static JsonSerializer<RaceGroup> createSerializer(UUID clientUuid) {
        return new RaceGroupJsonSerializer(new BoatClassJsonSerializer(), RegattaConfigurationJsonSerializer.create(),
                new SeriesWithRowsOfRaceGroupSerializer(new SeriesWithRowsJsonSerializer(
                        new RaceRowsOfSeriesWithRowsSerializer(new RaceRowJsonSerializer(new FleetJsonSerializer(
                                new ColorJsonSerializer()), new RaceCellJsonSerializer(createRaceLogSerializer(clientUuid), 
                                        new TargetTimeInfoSerializer(new WindJsonSerializer(new PositionJsonSerializer()))))))));

    }

    /**
     * @param clientUuid
     *            used to tell the race log to which client the race log is delivered so that when the client asks later
     *            which race log events are new for that client, the race log already knows which events were already
     *            delivered
     */
    private static JsonSerializer<RaceLog> createRaceLogSerializer(UUID clientUuid) {
        return new RaceLogSerializer(RaceLogEventSerializer.create(CompetitorJsonSerializer.create()), clientUuid);
    }

}
