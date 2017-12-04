package com.sap.sailing.server.gateway.jaxrs.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.datamining.SailingPredefinedQueries;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TargetTimeInfo;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.security.Permission;
import com.sap.sailing.domain.common.security.Permission.Mode;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.ControlPointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.GateJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.WaypointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.AbstractTrackedRaceDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ColorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorWithBoatJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DefaultWindTrackJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DistanceJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.FleetJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuversJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.MarkPassingsJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.NationalityJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PersonJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RaceEntriesJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RegattaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.SeriesJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TargetTimeInfoSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TeamJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TrackedRaceJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.WindJsonSerializer;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.util.RoundingUtil;
import com.sap.sse.datamining.shared.impl.PredefinedQueryIdentifier;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.User;

@Path("/v1/regattas")
public class RegattasResource extends AbstractSailingServerResource {
    private static final Logger logger = Logger.getLogger(RegattasResource.class.getName());

    private DataMiningResource dataMiningResource;
    
    private DataMiningResource getDataMiningResource() {
        if (dataMiningResource == null) {
            dataMiningResource = getResourceContext().getResource(DataMiningResource.class);
        }
        return dataMiningResource;
    }

    private Response getBadRegattaErrorResponse(String regattaName) {
        return Response.status(Status.NOT_FOUND).entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadBoatClassResponse(String boatClassName) {
        return Response.status(Status.NOT_FOUND).entity("Could not use a boat class with name '" + StringEscapeUtils.escapeHtml(boatClassName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadCompetitorIdResponse(Serializable competitorId) {
        return Response.status(Status.NOT_FOUND).entity("Could not find a competitor with ID '" + StringEscapeUtils.escapeHtml(competitorId.toString()) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadRaceErrorResponse(String regattaName, String raceName) {
        return Response.status(Status.NOT_FOUND)
                .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "' in regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }
    
    private Response getBadSeriesErrorResponse(String regattaName, String seriesName) {
        return Response.status(Status.NOT_FOUND)
                .entity("Could not find a series with name '" + StringEscapeUtils.escapeHtml(seriesName) + "' in regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }
    
    private Response getNoTrackedRaceErrorResponse(String regattaName, String raceName) {
        return Response.status(Status.NOT_FOUND)
                .entity("No tracked race for race with name '" + StringEscapeUtils.escapeHtml(raceName) + "' in regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getNotEnoughDataAvailabeErrorResponse(String regattaName, String raceName) {
        return Response.status(Status.NOT_FOUND)
                .entity("No wind or polar data for race with name '" + StringEscapeUtils.escapeHtml(raceName) + "' in regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getRegattas() {
        RegattaJsonSerializer regattaJsonSerializer = new RegattaJsonSerializer();

        JSONArray regattasJson = new JSONArray();
        for (Regatta regatta : getService().getAllRegattas()) {
            regattasJson.add(regattaJsonSerializer.serialize(regatta));
        }
        String json = regattasJson.toJSONString();
        return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}")
    public Response getRegatta(@PathParam("regattaname") String regattaName) {
        Response response;
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            SeriesJsonSerializer seriesJsonSerializer = new SeriesJsonSerializer(new FleetJsonSerializer(
                    new ColorJsonSerializer()));
            JsonSerializer<Regatta> regattaSerializer = new RegattaJsonSerializer(seriesJsonSerializer, null);
            JSONObject serializedRegatta = regattaSerializer.serialize(regatta);

            String json = serializedRegatta.toJSONString();
            response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    /**
     * Gets all entries for a regatta.
     * 
     * @param regattaName
     *            the name of the regatta
     * @return
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/entries")
    public Response getEntries(@PathParam("regattaname") String regattaName) {
        Response response;
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            NationalityJsonSerializer nationalityJsonSerializer = new NationalityJsonSerializer();
            CompetitorJsonSerializer competitorJsonSerializer = new CompetitorJsonSerializer(new TeamJsonSerializer(
                    new PersonJsonSerializer(nationalityJsonSerializer)));
            JsonSerializer<Regatta> regattaSerializer = new RegattaJsonSerializer(null, competitorJsonSerializer);
            JSONObject serializedRegatta = regattaSerializer.serialize(regatta);

            String json = serializedRegatta.toJSONString();
            response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    /**
     * Gets all entries for a race.
     * 
     * @param regattaName
     *            the name of the regatta
     * @return
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/entries")
    public Response getEntries(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                CompetitorWithBoatJsonSerializer competitorJsonSerializer = CompetitorWithBoatJsonSerializer.create();
                JsonSerializer<RaceDefinition> raceEntriesSerializer = new RaceEntriesJsonSerializer(competitorJsonSerializer);
                JSONObject serializedRaceEntries = raceEntriesSerializer.serialize(race);
    
                String json = serializedRaceEntries.toJSONString();
                response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/competitors")
    public Response getCompetitors(@PathParam("regattaname") String regattaName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            final CompetitorJsonSerializer competitorSerializer = CompetitorJsonSerializer.create();
            final JSONArray result = new JSONArray();
            for (final Competitor competitor : regatta.getAllCompetitors()) {
                result.add(competitorSerializer.serialize(competitor));
            }
            String json = result.toJSONString();
            response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/competitors/{competitorid}/add")
    public Response addCompetitor(@PathParam("regattaname") String regattaName,
            @PathParam("competitorid") String competitorIdAsString) {
        final Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(Permission.REGATTA.getStringPermissionForObjects(Mode.UPDATE, regattaName));
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            Serializable competitorId;
            try {
                competitorId = UUID.fromString(competitorIdAsString);
            } catch (IllegalArgumentException e) {
                competitorId = competitorIdAsString;
            }

            final Competitor competitor = getService().getCompetitorStore().getExistingCompetitorById(competitorId);
            if (competitor == null) {
                response = getBadCompetitorIdResponse(competitorId);
            } else {
                regatta.registerCompetitor(competitor);
                response = Response.ok().build();
            }
        }
        return response;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/competitors/createandadd")
    public Response createAndAddCompetitor(@PathParam("regattaname") String regattaName,
            @QueryParam("boatclass") String boatClassName, @QueryParam("sailid") String sailId,
            @QueryParam("nationalityIOC") String nationalityThreeLetterIOCCode,
            @QueryParam("timeontimefactor") Double timeOnTimeFactor,
            @QueryParam("timeondistanceallowancepernauticalmileasmillis") Long timeOnDistanceAllowancePerNauticalMileAsMillis,
            @QueryParam("searchtag") String searchTag) {
        final Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(Permission.REGATTA.getStringPermissionForObjects(Mode.UPDATE, regattaName));
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else if (boatClassName == null) {
            response = getBadBoatClassResponse(boatClassName);
        } else {
            final User user = getService(SecurityService.class).getCurrentUser();
            final Boat boat = new BoatImpl(UUID.randomUUID(), user.getName(),
                    getService().getBaseDomainFactory().getOrCreateBoatClass(boatClassName, /* typicallyStartsUpwind */ true),
                    sailId);
            final CompetitorWithBoat competitor = getService().getCompetitorStore().getOrCreateCompetitorWithBoat(UUID.randomUUID(),
                    user.getFullName() == null ? user.getName() : user.getFullName(), /* shortName */ null,
                    /* displayColor */ null, user.getEmail(), /* flagImageURI */ null,
                    new TeamImpl(user.getName(), Collections.singleton(new PersonImpl(user.getFullName() == null ? user.getName() : user.getFullName(),
                            getService().getBaseDomainFactory().getOrCreateNationality(nationalityThreeLetterIOCCode),
                            /* dateOfBirth */ null, /* description */ null)),
                            /* coach */ null), timeOnTimeFactor,
                    timeOnDistanceAllowancePerNauticalMileAsMillis == null ? null : new MillisecondsDurationImpl(timeOnDistanceAllowancePerNauticalMileAsMillis),
                    searchTag, (DynamicBoat) boat);
            regatta.registerCompetitor(competitor);
            response = Response.ok(CompetitorWithBoatJsonSerializer.create().serialize(competitor).toJSONString()).
                    header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/competitors/{competitorid}/remove")
    public Response removeCompetitor(@PathParam("regattaname") String regattaName,
            @PathParam("competitorid") String competitorIdAsString) {
        final Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(Permission.REGATTA.getStringPermissionForObjects(Mode.UPDATE, regattaName));
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            Serializable competitorId;
            try {
                competitorId = UUID.fromString(competitorIdAsString);
            } catch (IllegalArgumentException e) {
                competitorId = competitorIdAsString;
            }

            final Competitor competitor = getService().getCompetitorStore().getExistingCompetitorById(competitorId);
            if (competitor == null) {
                response = getBadCompetitorIdResponse(competitorId);
            } else {
                regatta.deregisterCompetitor(competitor);
                response = Response.ok().build();
            }
        }
        return response;
    }

    /**
     * Gets all GPS positions of the competitors for a given race.
     * 
     * @param regattaName
     *            the name of the regatta
     * @param tack
     *            whether or not to include the tack in the output for each fix. Determining tack requires an expensive
     *            wind calculation for the competitor's position for each fix's time point. If this value is not
     *            absolutely required, <code>false</code> should be provided here which is also the default.
     * @return
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/competitors/positions")
    public Response getCompetitorPositions(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName, @QueryParam("fromtime") String fromtime,
            @QueryParam("fromtimeasmillis") Long fromtimeasmillis, @QueryParam("totime") String totime,
            @QueryParam("totimeasmillis") Long totimeasmillis, @QueryParam("withtack") Boolean withTack,
            @QueryParam("competitorId") Set<String> competitorIds,
            @DefaultValue("false") @QueryParam("lastknown") boolean addLastKnown) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);

                TimePoint from;
                TimePoint to;
                try {
                    from = parseTimePoint(fromtime, fromtimeasmillis,
                            trackedRace.getStartOfRace() == null ? new MillisecondsTimePoint(0) :
                            /* 24h before race start */new MillisecondsTimePoint(trackedRace.getStartOfRace()
                                    .asMillis() - 24 * 3600 * 1000));
                } catch (InvalidDateException e1) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the 'from' time.")
                            .type(MediaType.TEXT_PLAIN).build();
                }
                try {
                    to = parseTimePoint(totime, totimeasmillis, MillisecondsTimePoint.now());
                } catch (InvalidDateException e1) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the 'to' time.")
                            .type(MediaType.TEXT_PLAIN).build();
                }

                JSONObject jsonRace = new JSONObject();
                jsonRace.put("name", trackedRace.getRace().getName());
                jsonRace.put("regatta", regatta.getName());
                JSONArray jsonCompetitors = new JSONArray();
                for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                    if (competitorIds == null || competitorIds.isEmpty()
                            || competitorIds.contains(competitor.getId().toString())) {
                        JSONObject jsonCompetitor = new JSONObject();
                        jsonCompetitor.put("id", competitor.getId() != null ? competitor.getId().toString() : null);
                        jsonCompetitor.put("name", competitor.getName());
                        jsonCompetitor.put("sailNumber", trackedRace.getBoatOfCompetitor(competitor).getSailID());
                        jsonCompetitor.put("color", competitor.getColor() != null ? competitor.getColor().getAsHtml() : null);
                        if(competitor.getFlagImage() != null) {
                            jsonCompetitor.put("flagImage", competitor.getFlagImage().toString());
                        }
                        GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
                        JSONArray jsonFixes = new JSONArray();
                        track.lockForRead();
                        try {
                            Iterator<GPSFixMoving> fixIter;
                            if (from == null) {
                                fixIter = track.getFixes().iterator();
                            } else {
                                fixIter = track.getFixesIterator(from, /* inclusive */true);
                            }
                            GPSFixMoving fix = null; 
                            boolean lastAdded = false;
                            while (fixIter.hasNext()) {
                                fix = fixIter.next();
                                if (to != null && fix.getTimePoint() != null && to.compareTo(fix.getTimePoint()) < 0) {
                                    lastAdded = false;
                                    break;
                                }
                                Tack tack = null;
                                if (withTack != null && withTack) {
                                    try {
                                        tack = trackedRace.getTack(competitor, fix.getTimePoint());
                                    } catch (NoWindException e) {
                                        // don't output tack
                                    }
                                }
                                addCompetitorFixToJsonFixes(jsonFixes, fix, tack);
                                lastAdded = true;
                            }
                            
                            if (addLastKnown && !lastAdded) {
                                // find a fix earlier than the interval requested:
                                Iterator<GPSFixMoving> earlierFixIter = track.getFixesDescendingIterator(from, /* inclusive */false);
                                final GPSFixMoving earlierFix;
                                if (earlierFixIter.hasNext()) {
                                    earlierFix = earlierFixIter.next();
                                } else {
                                    earlierFix = null;
                                }
                                Tack tack = null;
                                if (withTack != null && withTack) {
                                    try {
                                        tack = trackedRace.getTack(competitor, fix.getTimePoint());
                                    } catch (NoWindException e) {
                                        // don't output tack
                                    }
                                }
                                if (earlierFix != null && (fix == null || earlierFix.getTimePoint().until(from).compareTo(to.until(fix.getTimePoint())) <= 0)) {
                                    addCompetitorFixToJsonFixes(jsonFixes, earlierFix, tack); // the earlier fix is closer to the interval's beginning than fix is to its end
                                } else if (fix != null) {
                                    addCompetitorFixToJsonFixes(jsonFixes, fix, tack);
                                }
                            }
                        } finally {
                            track.unlockAfterRead();
                        }
                        jsonCompetitor.put("track", jsonFixes);
                        jsonCompetitors.add(jsonCompetitor);
                    }
                }
                jsonRace.put("competitors", jsonCompetitors);

                String json = jsonRace.toJSONString();
                response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    private JSONObject addCompetitorFixToJsonFixes(JSONArray jsonFixes, GPSFixMoving fix, Tack tack) {
        JSONObject jsonFix = new JSONObject();
        jsonFix.put("timepoint-ms", fix.getTimePoint().asMillis());
        jsonFix.put("lat-deg", RoundingUtil.latLngDecimalFormatter.format(fix
                .getPosition().getLatDeg()));
        jsonFix.put("lng-deg", RoundingUtil.latLngDecimalFormatter.format(fix
                .getPosition().getLngDeg()));
        jsonFix.put("truebearing-deg", fix.getSpeed().getBearing().getDegrees());
        jsonFix.put("speed-kts",
                RoundingUtil.knotsDecimalFormatter.format(fix.getSpeed().getKnots()));
        if (tack != null) {
            jsonFix.put("tack", tack.name());
        }
        jsonFixes.add(jsonFix);
        return jsonFix;
    }

    /**
     * Gets all GPS positions of the course marks for a given race.
     * 
     * @param regattaName
     *            the name of the regatta
     * @return
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/marks/positions")
    public Response getMarkPositions(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName, @QueryParam("fromtime") String fromtime,
            @QueryParam("fromtimeasmillis") Long fromtimeasmillis, @QueryParam("totime") String totime,
            @QueryParam("totimeasmillis") Long totimeasmillis,
            @DefaultValue("false") @QueryParam("lastknown") boolean addLastKnown) {
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            return getBadRegattaErrorResponse(regattaName);
        }

        RaceDefinition race = findRaceByName(regatta, raceName);
        if (race == null) {
            return getBadRaceErrorResponse(regattaName, raceName);
        }

        TrackedRace trackedRace = findTrackedRace(regattaName, raceName);

        TimePoint from;
        TimePoint to;
        try {
            from = parseTimePoint(fromtime, fromtimeasmillis,
                    trackedRace.getStartOfRace() == null ? new MillisecondsTimePoint(0) :
                    /* 24h before race start */new MillisecondsTimePoint(
                            trackedRace.getStartOfRace().asMillis() - 24 * 3600 * 1000));
        } catch (InvalidDateException e1) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the 'from' time.")
                    .type(MediaType.TEXT_PLAIN).build();
        }
        try {
            to = parseTimePoint(totime, totimeasmillis, MillisecondsTimePoint.now());
        } catch (InvalidDateException e1) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the 'to' time.")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        JSONObject jsonRace = new JSONObject();
        jsonRace.put("name", trackedRace.getRace().getName());
        jsonRace.put("regatta", regatta.getName());
        JSONArray jsonMarks = new JSONArray();
        Set<Mark> marks = new HashSet<Mark>();
        Course course = trackedRace.getRace().getCourse();
        for (Waypoint waypoint : course.getWaypoints()) {
            for (Mark mark : waypoint.getMarks()) {
                marks.add(mark);
            }
        }

        for (Mark mark : marks) {
            JSONObject jsonMark = new JSONObject();
            jsonMark.put("name", mark.getName());
            jsonMark.put("id", mark.getId() != null ? mark.getId().toString() : null);
            GPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(mark);
            JSONArray jsonFixes = new JSONArray();
            track.lockForRead();
            try {
                Iterator<GPSFix> fixIter;
                if (from == null) {
                    fixIter = track.getFixes().iterator();
                } else {
                    fixIter = track.getFixesIterator(from, /* inclusive */true);
                }
                GPSFix fix = null; 
                boolean lastAdded = false;
                while (fixIter.hasNext()) {
                    fix = fixIter.next();
                    if (to != null && fix.getTimePoint() != null && to.compareTo(fix.getTimePoint()) < 0) {
                        lastAdded = false;
                        break;
                    }
                    addMarkFixToJsonFixes(jsonFixes, fix);
                    lastAdded = true;
                }
                
                if (addLastKnown && !lastAdded) {
                    // find a fix earlier than the interval requested:
                    Iterator<GPSFix> earlierFixIter = track.getFixesDescendingIterator(from, /* inclusive */false);
                    final GPSFix earlierFix;
                    if (earlierFixIter.hasNext()) {
                        earlierFix = earlierFixIter.next();
                    } else {
                        earlierFix = null;
                    }
                    if (earlierFix != null && (fix == null || earlierFix.getTimePoint().until(from).compareTo(to.until(fix.getTimePoint())) <= 0)) {
                        addMarkFixToJsonFixes(jsonFixes, earlierFix); // the earlier fix is closer to the interval's beginning than fix is to its end
                    } else if (fix != null) {
                        addMarkFixToJsonFixes(jsonFixes, fix);
                    }
                }
                
            } finally {
                track.unlockAfterRead();
            }
            jsonMark.put("track", jsonFixes);
            jsonMarks.add(jsonMark);
        }
        jsonRace.put("marks", jsonMarks);

        String json = jsonRace.toJSONString();

        return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
    }

    private JSONObject addMarkFixToJsonFixes(JSONArray jsonFixes, GPSFix fix) {
        JSONObject jsonFix = new JSONObject();
        jsonFix.put("timepoint-ms", fix.getTimePoint().asMillis());
        jsonFix.put("lat-deg",
                RoundingUtil.latLngDecimalFormatter.format(fix.getPosition().getLatDeg()));
        jsonFix.put("lng-deg",
                RoundingUtil.latLngDecimalFormatter.format(fix.getPosition().getLngDeg()));
        jsonFixes.add(jsonFix);
        return jsonFix;
    }

    /**
     * Gets the course of the race.
     * 
     * @param regattaName
     *            the name of the regatta
     * @return
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/course")
    public Response getCourse(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                Course course = race.getCourse();
                CourseBaseJsonSerializer serializer = new CourseBaseJsonSerializer(new WaypointJsonSerializer(
                        new ControlPointJsonSerializer(new MarkJsonSerializer(), new GateJsonSerializer(
                                new MarkJsonSerializer()))));

                JSONObject jsonCourse = serializer.serialize(course);
                String json = jsonCourse.toJSONString();
                response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }
    
    /**
     * Gets the target time of the race
     * 
     * @param regattaName
     *            the name of the regatta
     * @return -1 if not enough polar data or no wind information is available
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/targettime")
    public Response getTargetTime(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName,
            @QueryParam("timeasmillis") Long timeasmillis) {
        // TODO bug 3108: add distances upwind/downwind/reach
        if (timeasmillis == null) {
            timeasmillis = System.currentTimeMillis();
        }
        TimePoint timePoint = new MillisecondsTimePoint(timeasmillis);
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                DynamicTrackedRace trackedRace = getService().getTrackedRace(regatta, race);
                if (trackedRace != null) {
                    TargetTimeInfo targetTime;
                    try {
                        targetTime = trackedRace.getEstimatedTimeToComplete(timePoint);
                        final TargetTimeInfoSerializer serializer = new TargetTimeInfoSerializer(new WindJsonSerializer(new PositionJsonSerializer()));
                        JSONObject jsonCourse = serializer.serialize(targetTime);
                        String json = jsonCourse.toJSONString();
                        response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
                    } catch (NotEnoughDataHasBeenAddedException | NoWindException e) {
                        response = getNotEnoughDataAvailabeErrorResponse(regattaName, raceName);
                    }
                } else {
                    response = getNoTrackedRaceErrorResponse(regattaName, raceName);
                }
                
            }
        }
        return response;
    }

    /**
     * Gets the relevant times of the race.
     * 
     * @param regattaName
     *            the name of the regatta
     * @return
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/times")
    public Response getTimes(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response = null;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);

                JSONObject jsonRaceTimes = new JSONObject();
                jsonRaceTimes.put("name", trackedRace.getRace().getName());
                jsonRaceTimes.put("regatta", regatta.getName());

                jsonRaceTimes.put("startOfRace-ms", trackedRace.getStartOfRace() == null ? null : trackedRace
                        .getStartOfRace().asMillis());
                jsonRaceTimes.put("startOfTracking-ms", trackedRace.getStartOfTracking() == null ? null : trackedRace
                        .getStartOfTracking().asMillis());
                jsonRaceTimes.put("newestTrackingEvent-ms", trackedRace.getTimePointOfNewestEvent() == null ? null
                        : trackedRace.getTimePointOfNewestEvent().asMillis());
                jsonRaceTimes.put("endOfTracking-ms", trackedRace.getEndOfTracking() == null ? null : trackedRace
                        .getEndOfTracking().asMillis());
                jsonRaceTimes.put("endOfRace-ms", trackedRace.getEndOfRace() == null ? null : trackedRace
                        .getEndOfRace().asMillis());
                jsonRaceTimes.put("delayToLive-ms", trackedRace.getDelayToLiveInMillis());

                JSONArray jsonMarkPassingTimes = new JSONArray();
                List<TimePoint> firstPassingTimepoints = new ArrayList<>();
                Iterable<com.sap.sse.common.Util.Pair<Waypoint, com.sap.sse.common.Util.Pair<TimePoint, TimePoint>>> markPassingsTimes = trackedRace
                        .getMarkPassingsTimes();
                synchronized (markPassingsTimes) {
                    int numberOfWaypoints = Util.size(markPassingsTimes);
                    int wayPointNumber = 1;
                    for (com.sap.sse.common.Util.Pair<Waypoint, com.sap.sse.common.Util.Pair<TimePoint, TimePoint>> markPassingTimes : markPassingsTimes) {
                        JSONObject jsonMarkPassing = new JSONObject();
                        String name = "M" + (wayPointNumber - 1);
                        if (wayPointNumber == numberOfWaypoints) {
                            name = "F";
                        }
                        jsonMarkPassing.put("name", name);
                        com.sap.sse.common.Util.Pair<TimePoint, TimePoint> timesPair = markPassingTimes.getB();
                        TimePoint firstPassingTime = timesPair.getA();
                        TimePoint lastPassingTime = timesPair.getB();
                        jsonMarkPassing.put("firstPassing-ms",
                                firstPassingTime == null ? null : firstPassingTime.asMillis());
                        jsonMarkPassing.put("lastPassing-ms",
                                lastPassingTime == null ? null : lastPassingTime.asMillis());

                        firstPassingTimepoints.add(firstPassingTime);

                        jsonMarkPassingTimes.add(jsonMarkPassing);
                        wayPointNumber++;
                    }
                }
                jsonRaceTimes.put("markPassings", jsonMarkPassingTimes);

                JSONArray jsonLegInfos = new JSONArray();
                trackedRace.getRace().getCourse().lockForRead();
                try {
                    Iterable<TrackedLeg> trackedLegs = trackedRace.getTrackedLegs();
                    int legNumber = 1;
                    for (TrackedLeg trackedLeg : trackedLegs) {
                        JSONObject jsonLegInfo = new JSONObject();
                        jsonLegInfo.put("name", "L" + legNumber);

                        try {
                            TimePoint firstPassingTime = firstPassingTimepoints.get(legNumber - 1);
                            if (firstPassingTime != null) {
                                jsonLegInfo.put("type", trackedLeg.getLegType(firstPassingTime));
                                jsonLegInfo.put(
                                        "bearing-deg",
                                        RoundingUtil.bearingDecimalFormatter.format(trackedLeg.getLegBearing(
                                                firstPassingTime).getDegrees()));
                            }
                        } catch (NoWindException e) {
                            // do nothing
                        }
                        jsonLegInfos.add(jsonLegInfo);

                        legNumber++;
                    }
                } finally {
                    trackedRace.getRace().getCourse().unlockAfterRead();
                }
                jsonRaceTimes.put("legs", jsonLegInfos);

                Date now = new Date();
                jsonRaceTimes.put("currentServerTime-ms", now.getTime());

                String json = jsonRaceTimes.toJSONString();
                response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/windsources")
    public Response getWindSources(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                JSONArray windSourcesAvailable = new JSONArray();
                if (trackedRace != null) {
                    for (WindSource windSource : trackedRace.getWindSources()) {
                        JSONObject windSourceJson = new JSONObject();
                        windSourceJson.put("typeName", windSource.getType().name());
                        windSourceJson.put("id", windSource.getId() != null ? windSource.getId().toString() : "");
                        windSourcesAvailable.add(windSourceJson);
                    }
                }
                return Response.ok(windSourcesAvailable.toString()).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/wind")
    public Response getWind(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName,
            @DefaultValue("COMBINED") @QueryParam("windsource") String windSource,
            @QueryParam("windsourceid") String windSourceId, @QueryParam("fromtime") String fromtime,
            @QueryParam("fromtimeasmillis") Long fromtimeasmillis, @QueryParam("totime") String totime,
            @QueryParam("totimeasmillis") Long totimeasmillis) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                .build();
        } else if (!((fromtime != null && totime != null) || (fromtimeasmillis != null && totimeasmillis != null))) {
            response = Response.status(Status.NOT_FOUND)
                .entity("Either the 'fromtime' and 'totime' or the 'fromtimeasmillis' and 'totimeasmillis' parameter must be set.").type(MediaType.TEXT_PLAIN)
                .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);

                TimePoint from;
                TimePoint to;
                try {
                    from = parseTimePoint(fromtime, fromtimeasmillis,
                            trackedRace.getStartOfRace() == null ? new MillisecondsTimePoint(0) :
                            /* 24h before race start */new MillisecondsTimePoint(trackedRace.getStartOfRace()
                                    .asMillis() - 24 * 3600 * 1000));
                } catch (InvalidDateException e1) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the 'from' time.")
                            .type(MediaType.TEXT_PLAIN).build();
                }
                try {
                    to = parseTimePoint(totime, totimeasmillis, MillisecondsTimePoint.now());
                } catch (InvalidDateException e1) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the 'to' time.")
                            .type(MediaType.TEXT_PLAIN).build();
                }
                // Crop request interval to startOfTracking / [endOfTracking|timePointOfLastEvent]
                final TimePoint finalFrom = Util.getLatestOfTimePoints(from, trackedRace.getStartOfTracking());
                final TimePoint finalTo = Util.getEarliestOfTimePoints(to, Util.getEarliestOfTimePoints(trackedRace.getEndOfTracking(), trackedRace.getTimePointOfNewestEvent()));
                TrackedRaceJsonSerializer serializer = new TrackedRaceJsonSerializer(
                        ws->new DefaultWindTrackJsonSerializer(/* maxNumberOfFixes */ 10000, finalFrom, finalTo, ws), windSource, windSourceId);

                JSONObject jsonWindTracks = serializer.serialize(trackedRace);
                String json = jsonWindTracks.toJSONString();
                return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/firstlegbearing")
    public Response getFirstLegBearing(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName, @QueryParam("time") String time,
            @QueryParam("timeasmillis") Long timeasmillis) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                final TimePoint timePoint;
                try {
                    timePoint = parseTimePoint(
                            time,
                            timeasmillis,
                            trackedRace.getStartOfRace() == null ? new MillisecondsTimePoint(0) : trackedRace
                                    .getStartOfRace());
                } catch (InvalidDateException e1) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the 'from' time.")
                            .type(MediaType.TEXT_PLAIN).build();
                }

                BearingJsonSerializer serializer = new BearingJsonSerializer();
                JSONObject jsonBearing = serializer.serialize(trackedRace.getDirectionFromStartToNextMark(timePoint)
                        .getFrom());
                String json = jsonBearing.toJSONString();
                return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/markpassings")
    public Response getMarkPassings(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                AbstractTrackedRaceDataJsonSerializer serializer = new MarkPassingsJsonSerializer();
                JSONObject jsonMarkPassings = serializer.serialize(trackedRace);
                String json = jsonMarkPassings.toJSONString();
                return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/maneuvers")
    public Response getManeuvers(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                ManeuversJsonSerializer serializer = new ManeuversJsonSerializer(new CompetitorWithBoatJsonSerializer(),
                        new ManeuverJsonSerializer(new GPSFixJsonSerializer(), new DistanceJsonSerializer()));
                JSONObject jsonMarkPassings = serializer.serialize(trackedRace);
                String json = jsonMarkPassings.toJSONString();
                return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    /**
     * @return the actual or anticipated start order for the race identified by {@coode regattaName} and {@code raceName}.
     * Those competitors for which a start mark passing is already known are sorted by those start mark passings. All other
     * boats are ordered by their geometric distance from the start line or the windward distance from the start mark
     * if the start for some reason is defined by a single mark. See {@link #compareDistanceFromStartLine(Competitor, Competitor)}.
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/startorder")
    public Response getStartOrder(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                final TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                Course course = trackedRace.getRace().getCourse();
                final List<Competitor> competitors = new ArrayList<>();
                final Waypoint firstWaypoint = course.getFirstWaypoint();
                final TimePoint timeToUseForStart;
                if (trackedRace.getStartOfRace() != null) {
                    timeToUseForStart = trackedRace.getStartOfRace();
                } else {
                    timeToUseForStart = MillisecondsTimePoint.now();
                }
                if (firstWaypoint != null) {
                    final List<Position> startWaypointMarkPositions =
                        StreamSupport.stream(firstWaypoint.getMarks().spliterator(), /* parallel */ false).
                        map(mark->trackedRace.getTrack(mark)).filter(markTrack->markTrack!=null).
                        map(markTrack->markTrack.getEstimatedPosition(timeToUseForStart, /* extrapolate */ false)).
                        collect(Collectors.toList());
                    Util.addAll(trackedRace.getRace().getCompetitors(), competitors);
                    competitors.sort((a, b)->{
                        final MarkPassing aStartMarkPassing = trackedRace.getMarkPassing(a, firstWaypoint);
                        final MarkPassing bStartMarkPassing = trackedRace.getMarkPassing(b, firstWaypoint);
                        final int result;
                        if (aStartMarkPassing == null) {
                            if (bStartMarkPassing == null) {
                                result = compareDistanceFromStartLine(trackedRace, startWaypointMarkPositions, timeToUseForStart, a, b);
                            } else {
                                result = 1; // b consider less than a because it has a start mark passing and therefore is assumed to have started before a
                            }
                        } else if (bStartMarkPassing == null) {
                            result = -1; // a consider less than b because it has a start mark passing and therefore is assumed to have started before b
                        } else {
                            // both have a start mark passing; compare time points
                            result = aStartMarkPassing.getTimePoint().compareTo(bStartMarkPassing.getTimePoint());
                        }
                        return result;
                    });
                }
                CompetitorWithBoatJsonSerializer serializer = new CompetitorWithBoatJsonSerializer();
                JSONArray result = new JSONArray();
                for (final Competitor c : competitors) {
                    Boat boat = trackedRace.getBoatOfCompetitor(c);
                    JSONObject jsonCompetitor = serializer.serialize(new CompetitorWithBoatImpl(c, (DynamicBoat) boat));
                    result.add(jsonCompetitor);
                }
                String json = result.toJSONString();
                return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    private int compareDistanceFromStartLine(TrackedRace trackedRace, Iterable<Position> startWaypointMarkPositions,
            TimePoint timeToUseForStart, Competitor a, Competitor b) {
        final Position aPos = trackedRace.getTrack(a).getEstimatedPosition(timeToUseForStart, /* extrapolate */ true);
        final Position bPos = trackedRace.getTrack(b).getEstimatedPosition(timeToUseForStart, /* extrapolate */ true);
        final Distance aDist = aPos==null?null:getDistanceFromStartWaypoint(aPos, startWaypointMarkPositions);
        final Distance bDist = bPos==null?null:getDistanceFromStartWaypoint(bPos, startWaypointMarkPositions);
        return Comparator.<Distance>nullsLast(Comparator.naturalOrder()).compare(aDist, bDist);
    }

    private Distance getDistanceFromStartWaypoint(Position pos, Iterable<Position> startWaypointMarkPositions) {
        final Distance result;
        if (Util.isEmpty(startWaypointMarkPositions)) {
            result = null;
        } else if (Util.size(startWaypointMarkPositions) == 1) {
            // single mark start; strange, but possible:
            result = startWaypointMarkPositions.iterator().next().getDistance(pos);
        } else {
            final Position first = Util.get(startWaypointMarkPositions, 0);
            final Position second = Util.get(startWaypointMarkPositions, 1);
            result = pos.getDistanceToLine(first, second).abs();
        }
        return result;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races")
    public Response getRaces(@PathParam("regattaname") String regattaName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            JSONObject jsonRaceResults = new JSONObject();
            jsonRaceResults.put("regatta", regatta.getName());
            JSONArray jsonRaces = new JSONArray();
            jsonRaceResults.put("races", jsonRaces);
            for (RaceDefinition race : regatta.getAllRaces()) {
                JSONObject jsonRace = new JSONObject();
                jsonRaces.add(jsonRace);
                jsonRace.put("name", race.getName());
                jsonRace.put("id", race.getId().toString());
            }
            String json = jsonRaceResults.toJSONString();
            return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }
    
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/competitors/legs")
    public Response getCompetitorRanks(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                TimePoint timePoint = trackedRace.getTimePointOfNewestEvent() == null ? MillisecondsTimePoint.now()
                        : trackedRace.getTimePointOfNewestEvent();
                final RankingInfo rankingInfo = trackedRace.getRankingMetric().getRankingInfo(timePoint);
                JSONObject jsonRaceResults = new JSONObject();
                jsonRaceResults.put("name", trackedRace.getRace().getName());
                jsonRaceResults.put("regatta", regatta.getName());
                jsonRaceResults.put("startOfRace-ms", trackedRace.getStartOfRace() == null ? null : trackedRace
                        .getStartOfRace().asMillis());

                JSONArray jsonLegs = new JSONArray();
                Course course = trackedRace.getRace().getCourse();
                course.lockForRead();
                try {
                    for (TrackedLeg leg : trackedRace.getTrackedLegs()) {
                        JSONObject jsonLeg = new JSONObject();
                        jsonLeg.put("from", leg.getLeg().getFrom().getName());
                        jsonLeg.put("fromWaypointId", leg.getLeg().getFrom().getId() != null ? leg.getLeg().getFrom().getId().toString() : null);
                        jsonLeg.put("to", leg.getLeg().getTo().getName());
                        jsonLeg.put("toWaypointId", leg.getLeg().getTo().getId() != null ? leg.getLeg().getTo().getId().toString() : null);
                        try {
                            jsonLeg.put("upOrDownwindLeg", leg.isUpOrDownwindLeg(timePoint));
                        } catch (NoWindException e) {
                            // no wind, then it's simply no upwind or downwind leg
                            jsonLeg.put("upOrDownwindLeg", "false");
                        }
                        JSONArray jsonCompetitors = new JSONArray();
                        Map<Competitor, Integer> ranks = leg.getRanks(timePoint);
                        for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                            JSONObject jsonCompetitorInLeg = new JSONObject();
                            TrackedLegOfCompetitor trackedLegOfCompetitor = leg.getTrackedLeg(competitor);
                            if (trackedLegOfCompetitor != null) {
                                jsonCompetitorInLeg.put("id", competitor.getId() != null ? competitor.getId()
                                        .toString() : null);
                                jsonCompetitorInLeg.put("name", competitor.getName());
                                jsonCompetitorInLeg.put("sailNumber", trackedRace.getBoatOfCompetitor(competitor).getSailID());
                                jsonCompetitorInLeg.put("color", competitor.getColor() != null ? competitor.getColor()
                                        .getAsHtml() : null);

                                Speed averageSpeedOverGround = trackedLegOfCompetitor
                                        .getAverageSpeedOverGround(timePoint);
                                if (averageSpeedOverGround != null) {
                                    jsonCompetitorInLeg.put("averageSOG-kts",
                                            RoundingUtil.knotsDecimalFormatter.format(averageSpeedOverGround
                                                    .getKnots()));
                                }
                                try {
                                    Integer numberOfTacks = trackedLegOfCompetitor.getNumberOfTacks(timePoint, /* waitForLatest */
                                            false);
                                    Integer numberOfJibes = trackedLegOfCompetitor.getNumberOfJibes(timePoint, /* waitForLatest */
                                            false);
                                    Integer numberOfPenaltyCircles = trackedLegOfCompetitor.getNumberOfPenaltyCircles(
                                            timePoint, /* waitForLatest */false);
                                    jsonCompetitorInLeg.put("tacks", numberOfTacks);
                                    jsonCompetitorInLeg.put("jibes", numberOfJibes);
                                    jsonCompetitorInLeg.put("penaltyCircles", numberOfPenaltyCircles);
                                } catch (NoWindException e) {
                                    logger.log(Level.FINE,
                                            "No wind information while trying to determing maneuvers for competitor "
                                                    + competitor.getName(), e);
                                }

                                TimePoint startTime = trackedLegOfCompetitor.getStartTime();
                                TimePoint finishTime = trackedLegOfCompetitor.getFinishTime();
                                TimePoint startOfRace = trackedRace.getStartOfRace();
                                // between the start of the race and the start of the first leg we have no
                                // 'timeSinceGun'
                                // for the competitor
                                if (startOfRace != null && startTime != null) {
                                    long timeSinceGun = -1;
                                    if (finishTime != null) {
                                        timeSinceGun = finishTime.asMillis() - startOfRace.asMillis();
                                    } else {
                                        timeSinceGun = timePoint.asMillis() - startOfRace.asMillis();
                                    }
                                    if (timeSinceGun > 0) {
                                        jsonCompetitorInLeg.put("timeSinceGun-ms", timeSinceGun);
                                    }
                                    Distance distanceSinceGun = trackedRace.getTrack(competitor).getDistanceTraveled(
                                            startOfRace, finishTime != null ? finishTime : timePoint);
                                    if (distanceSinceGun != null) {
                                        jsonCompetitorInLeg.put("distanceSinceGun-m",
                                                RoundingUtil.distanceDecimalFormatter.format(distanceSinceGun
                                                        .getMeters()));
                                    }
                                }

                                Distance distanceTraveled = trackedLegOfCompetitor.getDistanceTraveled(timePoint);
                                if (distanceTraveled != null) {
                                    jsonCompetitorInLeg.put("distanceTraveled-m",
                                            RoundingUtil.distanceDecimalFormatter.format(distanceTraveled
                                                    .getMeters()));
                                }
                                Distance distanceTraveledIncludingGateStart = trackedLegOfCompetitor
                                        .getDistanceTraveledConsideringGateStart(timePoint);
                                if (distanceTraveledIncludingGateStart != null) {
                                    jsonCompetitorInLeg.put("distanceTraveledIncludingGateStart-m",
                                            RoundingUtil.distanceDecimalFormatter
                                                    .format(distanceTraveledIncludingGateStart.getMeters()));
                                }
                                try {
                                    Integer rank = ranks.get(competitor);
                                    jsonCompetitorInLeg.put("rank", rank);
                                } catch (RuntimeException re) {
                                    if (re.getCause() != null && re.getCause() instanceof NoWindException) {
                                        // well, we don't know the wind direction, so we can't compute a ranking
                                    } else {
                                        throw re;
                                    }
                                }
                                Duration gapToLeaderDuration = trackedLegOfCompetitor.getGapToLeader(timePoint, rankingInfo, WindPositionMode.LEG_MIDDLE);
                                jsonCompetitorInLeg.put("gapToLeader-s", gapToLeaderDuration != null ? gapToLeaderDuration.asSeconds() : 0.0);
                                Distance gapToLeaderDistance = trackedLegOfCompetitor.getWindwardDistanceToCompetitorFarthestAhead(timePoint, WindPositionMode.LEG_MIDDLE, rankingInfo);
                                jsonCompetitorInLeg.put("gapToLeader-m", gapToLeaderDistance != null ? gapToLeaderDistance.getMeters() : 0.0);
                                jsonCompetitorInLeg.put("started", trackedLegOfCompetitor.hasStartedLeg(timePoint));
                                jsonCompetitorInLeg.put("finished", trackedLegOfCompetitor.hasFinishedLeg(timePoint));
                                jsonCompetitors.add(jsonCompetitorInLeg);
                            }
                        }
                        jsonLeg.put("competitors", jsonCompetitors);
                        jsonLegs.add(jsonLeg);
                    }
                } finally {
                    course.unlockAfterRead();
                }
                jsonRaceResults.put("legs", jsonLegs);

                String json = jsonRaceResults.toJSONString();
                return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/competitors/live")
    public Response getCompetitorLiveRanks(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName, @DefaultValue("-1") @QueryParam("topN") Integer topN) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                Leaderboard leaderboard = getService().getLeaderboardByName(regattaName);
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                Course course = trackedRace.getRace().getCourse();
                Waypoint lastWaypoint = course.getLastWaypoint();

                TimePoint timePoint = MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis());
                final RankingInfo rankingInfo = trackedRace.getRankingMetric().getRankingInfo(timePoint);
                JSONObject jsonLiveData = new JSONObject();
                jsonLiveData.put("name", trackedRace.getRace().getName());
                jsonLiveData.put("regatta", regatta.getName());

                if (trackedRace.getStartOfRace() != null) {
                    TimePoint startOfRace = trackedRace.getStartOfRace();
                    TimePoint now = MillisecondsTimePoint.now();
                    jsonLiveData.put("startTime", startOfRace.asMillis());
                    jsonLiveData.put("liveTime", now.asMillis());
                    if (startOfRace.before(now)) {
                        jsonLiveData.put("timeSinceStart-s", (now.asMillis() - startOfRace.asMillis()) / 1000.0);
                    } else {
                        jsonLiveData.put("timeToStart-s", (startOfRace.asMillis() - now.asMillis()) / 1000.0);
                    }
                }
                JSONArray jsonCompetitors = new JSONArray();
                List<Competitor> competitorsFromBestToWorst = trackedRace.getCompetitorsFromBestToWorst(timePoint);
                Map<Competitor, Integer> overallRankPerCompetitor = new HashMap<>();
                if (leaderboard != null) {
                    List<Competitor> overallRanking = leaderboard.getCompetitorsFromBestToWorst(timePoint);
                    Integer overallRank = 1;
                    for (Competitor competitor : overallRanking) {
                        overallRankPerCompetitor.put(competitor, overallRank++);
                    }
                }
                Integer rank = 1;
                for (Competitor competitor : competitorsFromBestToWorst) {
                    JSONObject jsonCompetitorInLeg = new JSONObject();

                    if (topN != null && topN > 0 && rank > topN) {
                        break;
                    }
                    jsonCompetitorInLeg.put("id", competitor.getId() != null ? competitor.getId().toString() : null);
                    jsonCompetitorInLeg.put("name", competitor.getName());
                    jsonCompetitorInLeg.put("sailNumber", trackedRace.getBoatOfCompetitor(competitor).getSailID());
                    jsonCompetitorInLeg.put("color", competitor.getColor() != null ? competitor.getColor().getAsHtml()
                            : null);
                    jsonCompetitorInLeg.put("rank", rank++);
                    final Integer overallRank = overallRankPerCompetitor.get(competitor);
                    if (overallRank != null) {
                        jsonCompetitorInLeg.put("overallRank", overallRank);
                    }
                    if (trackedRace.getEndOfTracking() == null || trackedRace.getEndOfTracking().after(timePoint)) {
                        GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = trackedRace.getTrack(competitor);
                        if (competitorTrack != null) {
                            final SpeedWithBearing estimatedSpeed = competitorTrack.getEstimatedSpeed(timePoint);
                            if (estimatedSpeed != null) {
                                jsonCompetitorInLeg.put("speedOverGround-kts", roundDouble(estimatedSpeed.getKnots(), 2));
                            }
                        }
                    }
                    TrackedLegOfCompetitor currentLegOfCompetitor = trackedRace.getCurrentLeg(competitor, timePoint);
                    if (currentLegOfCompetitor != null) {
                        int indexOfWaypoint = course.getIndexOfWaypoint(currentLegOfCompetitor.getLeg().getFrom());
                        jsonCompetitorInLeg.put("leg", indexOfWaypoint + 1);
                        Distance distanceTraveled = currentLegOfCompetitor.getDistanceTraveled(timePoint);
                        if (distanceTraveled != null) {
                            jsonCompetitorInLeg.put("distanceTraveled-m", roundDouble(distanceTraveled.getMeters(), 2));
                        }
                        Distance distanceTraveledConsideringGateStart = currentLegOfCompetitor
                                .getDistanceTraveledConsideringGateStart(timePoint);
                        if (distanceTraveledConsideringGateStart != null) {
                            jsonCompetitorInLeg.put("distanceTraveledConsideringGateStart-m",
                                    roundDouble(distanceTraveledConsideringGateStart.getMeters(), 2));
                        }

                        Duration gapToLeader = currentLegOfCompetitor.getGapToLeader(timePoint,
                                rankingInfo, WindPositionMode.LEG_MIDDLE);
                        if (gapToLeader != null) {
                            jsonCompetitorInLeg.put("gapToLeader-s", roundDouble(gapToLeader.asSeconds(), 2));
                        }

                        Distance windwardDistanceToCompetitorFarthestAhead = currentLegOfCompetitor
                                .getWindwardDistanceToCompetitorFarthestAhead(timePoint, WindPositionMode.LEG_MIDDLE, rankingInfo);
                        if (windwardDistanceToCompetitorFarthestAhead != null) {
                            jsonCompetitorInLeg.put("gapToLeader-m",
                                    roundDouble(windwardDistanceToCompetitorFarthestAhead.getMeters(), 2));
                        }
                        jsonCompetitorInLeg.put("finished", false);
                    } else {
                        // we need to distinguish between competitors which did not start and competitors which
                        // already finished
                        if (trackedRace.getMarkPassing(competitor, lastWaypoint) != null) {
                            jsonCompetitorInLeg.put("finished", true);
                        } else {
                            jsonCompetitorInLeg.put("finished", false);
                        }
                    }
                    jsonCompetitors.add(jsonCompetitorInLeg);
                }
                jsonLiveData.put("competitors", jsonCompetitors);
                String json = jsonLiveData.toJSONString();
                return Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }
    
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("datamining")
    public Response getRegattaPredefinedQueries() {
        List<PredefinedQueryIdentifier> predefinedRegattaQueries = getDataMiningResource().getPredefinedRegattaDataMiningQueries();
        return getDataMiningResource().predefinedQueryIdentifiersToJSON(predefinedRegattaQueries);
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/datamining/" + SailingPredefinedQueries.QUERY_AVERAGE_SPEED_PER_COMPETITOR_LEGTYPE)
    public Response avgSpeedPerCompetitorAndLegType(@PathParam("regattaname") String regattaName) {
        Response response;
        
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            response = getDataMiningResource().avgSpeedPerCompetitorAndLegType(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_AVERAGE_SPEED_PER_COMPETITOR_LEGTYPE)
    public Response avgSpeedPerCompetitorAndLegType(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                response = getDataMiningResource().avgSpeedPerCompetitorAndLegType(regattaName, raceName);
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/datamining/" + SailingPredefinedQueries.QUERY_DISTANCE_TRAVELED_PER_COMPETITOR_LEGTYPE)
    public Response sumDistancePerCompetitorAndLegType(@PathParam("regattaname") String regattaName) {
        Response response;
        
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            response = getDataMiningResource().sumDistanceTraveledPerCompetitorAndLegType(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_DISTANCE_TRAVELED_PER_COMPETITOR_LEGTYPE)
    public Response sumDistancePerCompetitorAndLegType(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                response = getDataMiningResource().sumDistanceTraveledPerCompetitorAndLegType(regattaName, raceName);
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/datamining/" + SailingPredefinedQueries.QUERY_MANEUVERS_PER_COMPETITOR)
    public Response sumManeuversPerCompetitor(@PathParam("regattaname") String regattaName) {
        Response response;
        
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            response = getDataMiningResource().sumManeuversPerCompetitor(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_MANEUVERS_PER_COMPETITOR)
    public Response sumManeuversPerCompetitor(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                response = getDataMiningResource().sumManeuversPerCompetitor(regattaName, raceName);
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/datamining/" + SailingPredefinedQueries.QUERY_AVERAGE_SPEED_PER_COMPETITOR)
    public Response avgSpeedPerCompetitor(@PathParam("regattaname") String regattaName) {
        Response response;
        
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            response = getDataMiningResource().avgSpeedPerCompetitor(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_AVERAGE_SPEED_PER_COMPETITOR)
    public Response avgSpeedPerCompetitor(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                response = getDataMiningResource().avgSpeedPerCompetitor(regattaName, raceName);
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/datamining/"+ SailingPredefinedQueries.QUERY_DISTANCE_TRAVELED_PER_COMPETITOR)
    public Response sumDistancePerCompetitor(@PathParam("regattaname") String regattaName) {
        Response response;
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            response = getDataMiningResource().sumDistanceTraveledPerCompetitor(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_DISTANCE_TRAVELED_PER_COMPETITOR)
    public Response sumDistancePerCompetitor(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        Response response;
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                response = getDataMiningResource().sumDistanceTraveledPerCompetitor(regattaName, raceName);
            }
        }
        return response;
    }
    
    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/addracecolumns")
    public Response addRaceColumns(@PathParam("regattaname") String regattaName,
            @QueryParam("numberofraces") Integer numberOfRaces, @QueryParam("prefix") String prefix,
            @QueryParam("toseries") String toSeries) {
        final Response response;
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            final JSONArray jsonResponse = new JSONArray();
            final Series series = getSeriesUsingLastAsDefault(regatta, toSeries);
            if (series == null) {
                response = getBadSeriesErrorResponse(regattaName, toSeries);
            } else {
                final String raceNamePrefix = prefix == null ? "R" : prefix;
                int oneBasedNumberOfLast = Util.size(series.getRaceColumns());
                for (int i = 1; i <= (numberOfRaces==null?1:numberOfRaces); i++) {
                    final int oneBasedNumberOfNext = findNextFreeRaceName(series, raceNamePrefix, oneBasedNumberOfLast);
                    final RaceColumnInSeries raceColumn = addRaceColumn(regattaName, series.getName(), getRaceName(raceNamePrefix, oneBasedNumberOfNext));
                    final JSONObject raceColumnDataAsJson = new JSONObject();
                    raceColumnDataAsJson.put("seriesname", raceColumn.getSeries().getName());
                    raceColumnDataAsJson.put("racename", raceColumn.getName());
                    jsonResponse.add(raceColumnDataAsJson);
                    oneBasedNumberOfLast = oneBasedNumberOfNext;
                }
                String json = jsonResponse.toJSONString();
                response = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @POST
    @Path("{regattaname}/removeracecolumn")
    public Response addRaceColumns(@PathParam("regattaname") String regattaName,
            @QueryParam("racecolumn") String raceColumnName) {
        SecurityUtils.getSubject().checkPermission(Permission.REGATTA.getStringPermissionForObjects(Mode.UPDATE, regattaName));
        final Response response;
        Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            boolean found = false;
            for (final Series series : regatta.getSeries()) {
                if (series.getRaceColumnByName(raceColumnName) != null) {
                    series.removeRaceColumn(raceColumnName);
                    found = true;
                    break;
                }
            }
            if (!found) {
                response = getBadRaceErrorResponse(regattaName, raceColumnName);
            } else {
                response = Response.ok().build();
            }
        }
        return response;
    }

    private String getRaceName(final String raceNamePrefix, final int number) {
        return raceNamePrefix+number;
    }
    
    private int findNextFreeRaceName(Series series, String raceNamePrefix, int oneBasedNumberOfLast) {
        int result = oneBasedNumberOfLast;
        boolean clash = false;
        do {
            result++;
            clash = false;
            final String raceNameCandidate = getRaceName(raceNamePrefix, result);
            for (final RaceColumnInSeries raceColumn : series.getRaceColumns()) {
                if (raceColumn.getName().equals(raceNameCandidate)) {
                    clash = true;
                    break;
                }
            }
        } while (clash);
        return result;
    }

    private RaceColumnInSeries addRaceColumn(String regattaName, String seriesName, String columnName) {
        SecurityUtils.getSubject().checkPermission(Permission.REGATTA.getStringPermissionForObjects(Mode.UPDATE, regattaName));
        return getService().apply(new AddColumnToSeries(new RegattaName(regattaName), seriesName, columnName));
    }

    private Series getSeriesUsingLastAsDefault(Regatta regatta, String seriesName) {
        final Series result;
        if (seriesName != null) {
            result = regatta.getSeriesByName(seriesName);
        } else {
            final Iterator<? extends Series> i = regatta.getSeries().iterator();
            if (i.hasNext()) {
                result = i.next();
            } else {
                result = null;
            }
        }
        return result;
    }
}
