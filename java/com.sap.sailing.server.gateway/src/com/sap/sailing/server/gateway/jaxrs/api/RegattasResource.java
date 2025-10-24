package com.sap.sailing.server.gateway.jaxrs.api;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sap.sailing.datamining.SailingPredefinedQueries;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastPublishedCourseDesignFinder;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogSetCompetitorTimeOnTimeFactorEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMappingFinder;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TargetTimeInfo;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.security.SecuredDomainType.LeaderboardActions;
import com.sap.sailing.domain.common.sharding.ShardingType;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.CompetitorJsonConstants;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.sharding.ShardingContext;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceWindCalculator;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.WindSummary;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.ControlPointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseBaseWithGeometryJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.CourseBaseWithGeometryJsonSerializer.CourseGeometry;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.GateJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.WaypointJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.AbstractTrackedRaceDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ColorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorTrackWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompleteManeuverCurveWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompleteManeuverCurvesWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DefaultWindTrackJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DetailedBoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DeviceIdentifierJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DistanceJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.FleetJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixMovingJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GpsFixesWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverMainCurveWithEstimationDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuverWindJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.ManeuversJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.MarkPassingsJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.NationalityJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PersonJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RaceEntriesJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RaceWindJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RegattaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.SeriesJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TargetTimeInfoSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TeamJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TrackedRaceJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.WindJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sailing.server.operationaltransformation.UpdateSpecificRegatta;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Color;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.RGBColor;
import com.sap.sse.common.util.RoundingUtil;
import com.sap.sse.datamining.shared.impl.PredefinedQueryIdentifier;
import com.sap.sse.security.BearerAuthenticationToken;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonSerializer;
import com.sap.sse.shared.util.impl.UUIDHelper;
import com.sap.sse.util.HttpRequestUtils;
import com.sap.sse.util.SingleCalculationPerSubjectCache;
import com.sap.sse.util.ThreadPoolUtil;

@Path("/v1/regattas")
public class RegattasResource extends AbstractSailingServerResource {
    private static final String SECONDARY_USER_BEARER_TOKEN = "secondaryuserbearertoken";

    private static final Logger logger = Logger.getLogger(RegattasResource.class.getName());

    private DataMiningResource dataMiningResource;
    
    private final ScheduledExecutorService executor = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor();
    
    private class CompetitorRanksRequest {
        private final Regatta regatta;
        private final RaceColumn raceColumn;
        private final Fleet fleet;
        public Regatta getRegatta() {
            return regatta;
        }
        public RaceColumn getRaceColumn() {
            return raceColumn;
        }
        public Fleet getFleet() {
            return fleet;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((fleet == null) ? 0 : fleet.hashCode());
            result = prime * result + ((raceColumn == null) ? 0 : raceColumn.hashCode());
            result = prime * result + ((regatta == null) ? 0 : regatta.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CompetitorRanksRequest other = (CompetitorRanksRequest) obj;
            if (fleet == null) {
                if (other.fleet != null)
                    return false;
            } else if (!fleet.equals(other.fleet))
                return false;
            if (raceColumn == null) {
                if (other.raceColumn != null)
                    return false;
            } else if (!raceColumn.equals(other.raceColumn))
                return false;
            if (regatta == null) {
                if (other.regatta != null)
                    return false;
            } else if (!regatta.equals(other.regatta))
                return false;
            return true;
        }
        public CompetitorRanksRequest(Regatta regatta, RaceColumn raceColumn, Fleet fleet) {
            super();
            this.regatta = regatta;
            this.raceColumn = raceColumn;
            this.fleet = fleet;
        }
        public RegattasResource getResource() {
            return RegattasResource.this;
        }
    }
    
    private final static SingleCalculationPerSubjectCache<CompetitorRanksRequest, JSONObject> competitorRanksCache = new SingleCalculationPerSubjectCache<>(
            r->r.getResource().computeCompetitorRanks(r.getRegatta(), r.getRaceColumn(), r.getFleet()), /* ten minute timeout */ Duration.ONE_MINUTE.times(10));
    
    private class CompetitorLiveRanksRequest {
        private final String regattaName;
        private final String raceName;
        private final Integer topN;
        public CompetitorLiveRanksRequest(String regattaName, String raceName, Integer topN) {
            super();
            this.regattaName = regattaName;
            this.raceName = raceName;
            this.topN = topN;
        }
        public String getRegattaName() {
            return regattaName;
        }
        public String getRaceName() {
            return raceName;
        }
        public Integer getTopN() {
            return topN;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((raceName == null) ? 0 : raceName.hashCode());
            result = prime * result + ((regattaName == null) ? 0 : regattaName.hashCode());
            result = prime * result + ((topN == null) ? 0 : topN.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CompetitorLiveRanksRequest other = (CompetitorLiveRanksRequest) obj;
            if (raceName == null) {
                if (other.raceName != null)
                    return false;
            } else if (!raceName.equals(other.raceName))
                return false;
            if (regattaName == null) {
                if (other.regattaName != null)
                    return false;
            } else if (!regattaName.equals(other.regattaName))
                return false;
            if (topN == null) {
                if (other.topN != null)
                    return false;
            } else if (!topN.equals(other.topN))
                return false;
            return true;
        }
        public RegattasResource getResource() {
            return RegattasResource.this;
        }
    }

    private final static SingleCalculationPerSubjectCache<CompetitorLiveRanksRequest, JSONObject> competitorLiveRanksCache = new SingleCalculationPerSubjectCache<>(
            r->r.getResource().computeCompetitorLiveRanks(r.getRegattaName(), r.getRaceName(), r.getTopN()), /* ten minute timeout */ Duration.ONE_MINUTE.times(10));
    
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

    private Response getBadRegattaRegistrationTypeErrorResponse(String regattaName) {
        return Response.status(Status.FORBIDDEN).entity("Self-registration to regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "' is not allowed.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadRegattaRegistrationValidationErrorResponse(String errorText) {
        return Response.status(Status.BAD_REQUEST).entity(StringEscapeUtils.escapeHtml(errorText) + ".")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getAlreadyRegisteredDeviceErrorResponse(String regattaName, String deviceId) {
        return Response.status(Status.FORBIDDEN).entity("Device is already registered to regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getDeregisterCompetitorErrorResponse(String regattaName, String competitorId, String errorText) {
        return Response.status(Status.BAD_REQUEST)
                .entity("Deregistering competitor " + StringEscapeUtils.escapeHtml(competitorId) + " from regatta "
                        + StringEscapeUtils.escapeHtml(regattaName) + " failed: " + errorText)
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

    private Response getBadCourseErrorResponse(String regattaName, String raceColumn, String fleet) {
        return Response.status(Status.NOT_FOUND)
                .entity("No course found for given race with raceColumn '" + StringEscapeUtils.escapeHtml(raceColumn)
                        + "' and fleet '" + StringEscapeUtils.escapeHtml(fleet) + "' in regatta '"
                        + StringEscapeUtils.escapeHtml(regattaName) + "'.").build();
    }

    private Response getBadRaceErrorResponse(String regattaName, String raceColumn, String fleet) {
        return Response.status(Status.NOT_FOUND)
                .entity("Could not find a race with raceColumn '" + StringEscapeUtils.escapeHtml(raceColumn)
                        + "' and fleet '" + StringEscapeUtils.escapeHtml(fleet) + "' in regatta '"
                        + StringEscapeUtils.escapeHtml(regattaName) + "'.")
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
        RegattaJsonSerializer regattaJsonSerializer = new RegattaJsonSerializer(getSecurityService());
        JSONArray regattasJson = new JSONArray();
        for (Regatta regatta : getService().getAllRegattas()) {
            if (getSecurityService().hasCurrentUserReadPermission(regatta)) {
                regattasJson.add(regattaJsonSerializer.serialize(regatta));
            }
        }
        return Response.ok(streamingOutput(regattasJson)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}")
    public Response getRegatta(@PathParam("regattaname") String regattaName,
            @QueryParam("secret") String regattaSecret) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            boolean skip = getService().skipChecksDueToCorrectSecret(regattaName, regattaSecret);
            if (!skip) {
                getSecurityService().checkCurrentUserReadPermission(regatta);
            }
            SeriesJsonSerializer seriesJsonSerializer = new SeriesJsonSerializer(new FleetJsonSerializer(
                    new ColorJsonSerializer()), getService());
            JsonSerializer<Regatta> regattaSerializer = new RegattaJsonSerializer(seriesJsonSerializer, null, null, getSecurityService());
            JSONObject serializedRegatta = regattaSerializer.serialize(regatta);
            response = Response.ok(streamingOutput(serializedRegatta)).build();
        }
        return response;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaName}")
    public Response updateRegatta(String jsonBody, @PathParam("regattaName") String regattaName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserUpdatePermission(regatta);
            final JSONObject requestObject;
            try {
                Object requestBody = JSONValue.parseWithException(jsonBody);
                requestObject = Helpers.toJSONObjectSafe(requestBody);
            } catch (ParseException | JsonDeserializationException e) {
                logger.log(Level.WARNING, "Exception while parsing post request", e);
                return Response.status(Status.BAD_REQUEST).entity("Invalid JSON body in request")
                        .type(MediaType.TEXT_PLAIN).build();
            }
            final Number startTimePointAsMillis = (Number) requestObject.get("startTimePointAsMillis");
            final TimePoint startTimePoint = startTimePointAsMillis == null ? null
                    : new MillisecondsTimePoint(startTimePointAsMillis.longValue());
            final Number endTimePointAsMillis = (Number) requestObject.get("endTimePointAsMillis");
            final TimePoint endTimePoint = endTimePointAsMillis == null ? null
                    : new MillisecondsTimePoint(endTimePointAsMillis.longValue());
            final String defaultCourseAreaUuidString = (String) requestObject.get("defaultCourseAreaUuid");
            final UUID defaultCourseAreaUuid = defaultCourseAreaUuidString == null ? null
                    : UUID.fromString(defaultCourseAreaUuidString);
            final Number buoyZoneRadiusInHullLengthsNumber = (Number) requestObject.get("buoyZoneRadiusInHullLengths");
            final Double buoyZoneRadiusInHullLengths = buoyZoneRadiusInHullLengthsNumber == null ? null
                    : buoyZoneRadiusInHullLengthsNumber.doubleValue();
            final boolean useStartTimeInference = Boolean.TRUE.equals(requestObject.get("useStartTimeInference"));
            final boolean controlTrackingFromStartAndFinishTimes = Boolean.TRUE
                    .equals(requestObject.get("controlTrackingFromStartAndFinishTimes"));
            final boolean autoRestartTrackingUponCompetitorSetChange = Boolean.TRUE.equals(requestObject.get("autoRestartTrackingUponCompetitorSetChange"));
            String registrationLinkSecret = (String) requestObject.get("registrationLinkSecret");
            if (registrationLinkSecret == null) {
                registrationLinkSecret = regatta.getRegistrationLinkSecret();
            }
            final String competitorRegistrationTypeString = (String) requestObject.get("competitorRegistrationType");
            final CompetitorRegistrationType competitorRegistrationType = competitorRegistrationTypeString == null
                    ? regatta.getCompetitorRegistrationType()
                    : CompetitorRegistrationType.valueOf(competitorRegistrationTypeString);
            getService().apply(new UpdateSpecificRegatta(new RegattaName(regattaName), startTimePoint, endTimePoint,
                    defaultCourseAreaUuid,
                    /* TODO updating the configuration is currently not supported */ regatta.getRegattaConfiguration(),
                    buoyZoneRadiusInHullLengths, useStartTimeInference, controlTrackingFromStartAndFinishTimes,
                    autoRestartTrackingUponCompetitorSetChange, registrationLinkSecret, competitorRegistrationType));
            SeriesJsonSerializer seriesJsonSerializer = new SeriesJsonSerializer(
                    new FleetJsonSerializer(new ColorJsonSerializer()), getService());
            JsonSerializer<Regatta> regattaSerializer = new RegattaJsonSerializer(seriesJsonSerializer, null, null,
                    getSecurityService());
            JSONObject serializedRegatta = regattaSerializer.serialize(regatta);
            response = Response.ok(streamingOutput(serializedRegatta)).build();
        }
        return response;
    }

    @DELETE
    @Path("{regattaname}")
    public Response delete(@PathParam("regattaname") String regattaName) {
        Regatta regatta = getService().getRegattaByName(regattaName);
        // TODO this is the same code, as the one used by the SailingServiceImpl, consolidate if an additional layer is
        // ever wrapped around the RacingEventService
        if (regatta != null) {
            Set<QualifiedObjectIdentifier> objectsThatWillBeImplicitlyCleanedByRemoveRegatta = new HashSet<>();
            objectsThatWillBeImplicitlyCleanedByRemoveRegatta.add(regatta.getIdentifier());
            for (RaceDefinition race : regatta.getAllRaces()) {
                TypeRelativeObjectIdentifier typeRelativeObjectIdentifier = RegattaNameAndRaceName
                        .getTypeRelativeObjectIdentifier(regatta.getName(), race.getName());
                QualifiedObjectIdentifier identifier = SecuredDomainType.TRACKED_RACE
                        .getQualifiedObjectIdentifier(typeRelativeObjectIdentifier);
                objectsThatWillBeImplicitlyCleanedByRemoveRegatta.add(identifier);
            }
            for (Leaderboard leaderboard : getService().getLeaderboards().values()) {
                if (leaderboard instanceof RegattaLeaderboard) {
                    RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                    if (regattaLeaderboard.getRegatta() == regatta) {
                        objectsThatWillBeImplicitlyCleanedByRemoveRegatta.add(regattaLeaderboard.getIdentifier());
                    }
                }
            }
            // check if we can delete everything RemoveRegatta will remove
            for (QualifiedObjectIdentifier toRemovePermissionObjects : objectsThatWillBeImplicitlyCleanedByRemoveRegatta) {
                getSecurityService().checkCurrentUserDeletePermission(toRemovePermissionObjects);
            }
            // we have all permissions, execute
            getService().apply(new RemoveRegatta(regatta.getRegattaIdentifier()));
            // cleanup the Ownership and ACLs
            for (QualifiedObjectIdentifier toRemovePermissionObjects : objectsThatWillBeImplicitlyCleanedByRemoveRegatta) {
                getSecurityService().deleteAllDataForRemovedObject(toRemovePermissionObjects);
            }

        }
        return Response.ok().build();
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
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            NationalityJsonSerializer nationalityJsonSerializer = new NationalityJsonSerializer();
            BoatJsonSerializer boatJsonSerializer = new BoatJsonSerializer(new BoatClassJsonSerializer());
            CompetitorJsonSerializer competitorJsonSerializer = new CompetitorJsonSerializer(new TeamJsonSerializer(
                    new PersonJsonSerializer(nationalityJsonSerializer)), boatJsonSerializer, /* serializeNonPublicFields */ false);
            JsonSerializer<Regatta> regattaSerializer = new RegattaJsonSerializer(null, competitorJsonSerializer, boatJsonSerializer, getSecurityService());
            JSONObject serializedRegatta = regattaSerializer.serialize(regatta);
            response = Response.ok(streamingOutput(serializedRegatta)).build();
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
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                JsonSerializer<RaceDefinition> raceEntriesSerializer = new RaceEntriesJsonSerializer(getSecurityService());
                JSONObject serializedRaceEntries = raceEntriesSerializer.serialize(race);
                response = Response.ok(streamingOutput(serializedRaceEntries)).build();
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
            getSecurityService().checkCurrentUserReadPermission(regatta);
            final CompetitorJsonSerializer competitorSerializer = CompetitorJsonSerializer.create();
            final JSONArray result = new JSONArray();
            for (final Competitor competitor : regatta.getAllCompetitors()) {
                if (getSecurityService().hasCurrentUserOneOfExplicitPermissions(competitor,
                        SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                    final double effectiveTimeOnTimeFactor = regatta.getTimeOnTimeFactor(competitor, /* changeCallback */ Optional.empty());
                    final Duration effectiveTimeOnDistanceAllowancePerNauticalMile = regatta.getTimeOnDistanceAllowancePerNauticalMile(competitor, /* changeCallback */ Optional.empty());
                    final JSONObject competitorJson = competitorSerializer.serialize(competitor);
                    // overwrite the competitor-specific values by the efffective ones in the scope of the regatta:
                    competitorJson.put(CompetitorJsonConstants.FIELD_TIME_ON_TIME_FACTOR, effectiveTimeOnTimeFactor);
                    competitorJson.put(CompetitorJsonConstants.FIELD_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE,
                            effectiveTimeOnDistanceAllowancePerNauticalMile==null?null:effectiveTimeOnDistanceAllowancePerNauticalMile.asSeconds());
                    result.add(competitorJson);
                }
            }
            response = Response.ok(streamingOutput(result)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/competitors/{competitorid}/add")
    public Response addCompetitor(@PathParam("regattaname") String regattaName,
            @PathParam("competitorid") String competitorIdAsString,
            @QueryParam("secret") String registrationLinkSecret) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            boolean skipPermissionCheck = getService().skipChecksDueToCorrectSecret(regattaName, registrationLinkSecret);
            if (!skipPermissionCheck) {
                getSecurityService().checkCurrentUserUpdatePermission(regatta);
            }
            Serializable competitorId;
            try {
                competitorId = UUID.fromString(competitorIdAsString);
            } catch (IllegalArgumentException e) {
                competitorId = competitorIdAsString;
            }

            final Competitor competitor = getService().getCompetitorAndBoatStore().getExistingCompetitorById(competitorId);
            if (competitor == null) {
                response = getBadCompetitorIdResponse(competitorId);
            } else {
                getSecurityService().checkCurrentUserHasOneOfExplicitPermissions(competitor,
                        SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS);
                regatta.registerCompetitor(competitor);
                response = Response.ok().build();
            }
        }
        return response;
    }

    @FunctionalInterface
    private static interface BoatObtainer {
        DynamicBoat getBoat(String boatName, OwnershipAnnotation regattaOwnershipAnnotation, boolean skipPermissionChecksBasedOnCorrectRegattaSecretProvided);
    }

    /**
     * Creates a {@link CompetitorWithBoat} object; the {@link Boat} is provided by the {@code boatObtainer} which could
     * be a construction or a look-up, depending on the variant of
     * {@link #createAndAddCompetitor(String, String, String, String, String, String, String, Double, Long, String, String, String, String, String, String)}
     * /
     * {@link #createAndAddCompetitorWithBoat(String, String, String, String, String, String, String, Double, Long, String, String, String, String, String, String)}
     * has been used. The {@link CompetitorWithBoat} object will be registered on the regatta specified by the
     * {@code regattaName}.<p>
     *
     * Additionally, if a non-{@code null} {@code deviceUuid} is provided, a device mapping is created that maps the device with
     * the UUID provided to the competitor created and registered. If such a device mapping already exists, then that is considered an
     * error, and the method aborts.<p>
     *
     * Security-wise, in order for this to succeed, the caller either has to present the correct {@code registrationLinkSecret} for the regatta,
     * or the calling {@link Subject} must have the {@code REGATTA:UPDATE} permission for the regatta identified by {@code regattaName} as well as the
     * {@code SERVER:CREATE_OBJECT} permission for the server on which the request is executed, also the {@code CREATE:COMPETITOR} and, if the {@link Boat}
     * needs to be created too, the {@code CREATE:BOAT} permission for the competitor and boat to be created, respectively.
     */
    private Response createAndAddCompetitor(String regattaName, String nationalityThreeLetterIOCCode, String rgbColor,
            Double timeOnTimeFactor, Long timeOnDistanceAllowancePerNauticalMileAsMillis, String searchTag,
            String competitorName, String competitorShortName, String competitorEmail, String flagImageURIString,
            String teamImageURIString, BoatObtainer boatObtainer, String deviceUuid,
            String registrationLinkSecret) {
        final User user = getSecurityService().getCurrentUser();
        Response response;
        final Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            return getBadRegattaErrorResponse(regattaName);
        }
        OwnershipAnnotation regattaOwnershipAnnotation = getSecurityService().getOwnership(regatta.getIdentifier());
        if (regattaOwnershipAnnotation == null) {
            return getBadRegattaErrorResponse(regattaName);
        }
        final boolean skipPermissionChecksBasedOnCorrectRegattaSecretProvided = Util.equalsWithNull(regatta.getRegistrationLinkSecret(), registrationLinkSecret);
        // users must at least have REGATTA:UPDATE or must provide a valid secret where then the regatta must be a "OPEN" regatta:
        if (!getSecurityService().hasCurrentUserUpdatePermission(regatta) && (!skipPermissionChecksBasedOnCorrectRegattaSecretProvided || !regatta.getCompetitorRegistrationType().isOpen())) {
            return getBadRegattaRegistrationTypeErrorResponse(regattaName);
        }
        String eCompetitorName = null, eCompetitorShortName = null, eCompetitorEmail = null;
        if (competitorName == null && user == null) {
            return getBadRegattaRegistrationValidationErrorResponse("No competitor name specified and no user authenticated; can't derive competitor name");
        }
        // defaulting competitor attributes to user attributes, and the short name / boat name to the long name
        eCompetitorName = competitorName == null ? user.getFullName() == null ? user.getName() : user.getFullName() : competitorName;
        eCompetitorShortName = competitorShortName == null ? eCompetitorName : competitorShortName;
        eCompetitorEmail = competitorEmail == null ? user != null ? user.getEmail() : null : competitorEmail;
        // Check regattalog if device has been already registered to this regatta
        boolean duplicateDeviceId = false;
        if (deviceUuid != null) {
            final RegattaLog regattaLog = regatta.getRegattaLog();
            regattaLog.lockForRead();
            try {
                duplicateDeviceId = regattaLog.getUnrevokedEvents().stream().anyMatch(event -> {
                    if (event instanceof RegattaLogDeviceCompetitorMappingEvent) {
                        return deviceUuid.equals(
                                ((RegattaLogDeviceCompetitorMappingEvent) event).getDevice().getStringRepresentation());
                    } else {
                        return false;
                    }
                });
            } finally {
                regattaLog.unlockAfterRead();
            }
        }
        if (duplicateDeviceId) {
            response = this.getAlreadyRegisteredDeviceErrorResponse(regattaName, deviceUuid);
        } else {
            DynamicBoat boat = boatObtainer.getBoat(eCompetitorShortName, regattaOwnershipAnnotation, skipPermissionChecksBasedOnCorrectRegattaSecretProvided);
            final Color color;
            if (rgbColor == null || rgbColor.length() == 0) {
                color = null;
            } else {
                try {
                    color = new RGBColor(rgbColor);
                } catch (IllegalArgumentException iae) {
                    return getBadRegattaRegistrationValidationErrorResponse(
                            String.format("invalid color %s", iae.getMessage()));
                }
            }
            final URI flagImageURI;
            if (flagImageURIString == null || flagImageURIString.length() == 0) {
                flagImageURI = null;
            } else {
                try {
                    flagImageURI = new URI(flagImageURIString);
                } catch (URISyntaxException use) {
                    return getBadRegattaRegistrationValidationErrorResponse(
                            String.format("invalid flagImageURIString %s", flagImageURIString));
                }
            }
            final URI teamImageURI;
            if (teamImageURIString == null || teamImageURIString.length() == 0) {
                teamImageURI = null;
            } else {
                try {
                    teamImageURI = new URI(teamImageURIString);
                } catch (URISyntaxException use) {
                    return getBadRegattaRegistrationValidationErrorResponse(
                            String.format("invalid flagImageURIString %s", teamImageURIString));
                }
            }
            final TeamImpl team = new TeamImpl(eCompetitorShortName,
                    Collections.singleton(new PersonImpl(eCompetitorName,
                            getService().getBaseDomainFactory().getOrCreateNationality(nationalityThreeLetterIOCCode),
                            /* dateOfBirth */ null, /* description */ null)),
                    /* coach */ null, teamImageURI);
            final UUID competitorUuid = UUID.randomUUID();
            final String name = eCompetitorName;
            final String shortName = eCompetitorShortName;
            final String email = eCompetitorEmail;
            CompetitorWithBoat competitor = null;
            final Callable<CompetitorWithBoat> createCompetitorJob = ()->getService().getCompetitorAndBoatStore().getOrCreateCompetitorWithBoat(
                    competitorUuid, name, shortName, color, email, flagImageURI, team,
                    timeOnTimeFactor,
                    timeOnDistanceAllowancePerNauticalMileAsMillis == null ? null
                            : new MillisecondsDurationImpl(
                                    timeOnDistanceAllowancePerNauticalMileAsMillis),
                            searchTag, boat, /* storePersistently */ true);
            if (skipPermissionChecksBasedOnCorrectRegattaSecretProvided) {
                try {
                    competitor = createCompetitorJob.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                getSecurityService().setOwnership(competitor.getIdentifier(),
                        (User) regattaOwnershipAnnotation.getAnnotation().getUserOwner(),
                        regattaOwnershipAnnotation.getAnnotation().getTenantOwner(), name);
            } else {
                competitor = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                        SecuredDomainType.COMPETITOR, CompetitorImpl.getTypeRelativeObjectIdentifier(competitorUuid), name,
                        createCompetitorJob);
            }
            regatta.registerCompetitor(competitor); // FIXME what about replication???
            response = Response.ok(streamingOutput(CompetitorJsonSerializer.create().serialize(competitor)))
                    .header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            if (deviceUuid != null) {
                final DeviceIdentifier device = new SmartphoneUUIDIdentifierImpl(UUID.fromString(deviceUuid));
                final TimePoint now = MillisecondsTimePoint.now();
                RegattaLogDeviceMappingEventImpl<Competitor> event = new RegattaLogDeviceCompetitorMappingEventImpl(now,
                        now, new LogEventAuthorImpl(eCompetitorName, 0), UUID.randomUUID(), competitor, device, now,
                        /* to */ null);
                regatta.getRegattaLog().add(event);
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
            @QueryParam("displayColor") String displayColor, @QueryParam("flagImageURI") String flagImageURI,
            @QueryParam("teamImageURI") String teamImageURI, @QueryParam("timeontimefactor") Double timeOnTimeFactor,
            @QueryParam("timeondistanceallowancepernauticalmileasmillis") Long timeOnDistanceAllowancePerNauticalMileAsMillis,
            @QueryParam("searchtag") String searchTag, @QueryParam("competitorName") String competitorName,
            @QueryParam("competitorShortName") String competitorShortName,
            @QueryParam("competitorEmail") String competitorEmail, @QueryParam("deviceUuid") String deviceUuid,
            @QueryParam("secret") String registrationLinkSecret) {
        Response response;
        if (boatClassName == null) {
            response = getBadBoatClassResponse(boatClassName);
        } else {
            response = createAndAddCompetitor(regattaName, nationalityThreeLetterIOCCode, displayColor,
                    timeOnTimeFactor, timeOnDistanceAllowancePerNauticalMileAsMillis, searchTag, competitorName,
                    competitorShortName, competitorEmail, flagImageURI, teamImageURI,
                    (shortName, regattaOwnershipAnnotation, skipPermissionChecksBasedOnCorrectRegattaSecretProvided) ->
                        createBoat(shortName, boatClassName, sailId, regattaOwnershipAnnotation, skipPermissionChecksBasedOnCorrectRegattaSecretProvided),
                    deviceUuid, registrationLinkSecret);
        }
        return response;
    }

    private DynamicBoat createBoat(String name, String boatClassName, String sailId,
            OwnershipAnnotation regattaOwnershipAnnotation,
            boolean skipPermissionChecksBasedOnCorrectRegattaSecretProvided) {
        final UUID boatUUID = UUID.randomUUID();
        final DynamicBoat boat;
        final Callable<DynamicBoat> job = ()->new BoatImpl(boatUUID, name, getService().getBaseDomainFactory()
                .getOrCreateBoatClass(boatClassName, /* typicallyStartsUpwind */ true), sailId);
        if (skipPermissionChecksBasedOnCorrectRegattaSecretProvided) {
            try {
                boat = job.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (getSecurityService().getOwnership(boat.getIdentifier()) == null) {
                getSecurityService().setOwnership(boat.getIdentifier(),
                        (User) regattaOwnershipAnnotation.getAnnotation().getUserOwner(),
                        regattaOwnershipAnnotation.getAnnotation().getTenantOwner(), name);
            }
        } else {
            boat = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredDomainType.BOAT, BoatImpl.getTypeRelativeObjectIdentifier(boatUUID), name, job);
        }
        return boat;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/competitors/createandaddwithboat")
    public Response createAndAddCompetitorWithBoat(@PathParam("regattaname") String regattaName,
            @QueryParam("boatId") String boatId, @QueryParam("sailid") String sailId,
            @QueryParam("nationalityIOC") String nationalityThreeLetterIOCCode,
            @QueryParam("flagImageURI") String flagImageURI, @QueryParam("teamImageURI") String teamImageURI,
            @QueryParam("displayColor") String displayColor, @QueryParam("timeontimefactor") Double timeOnTimeFactor,
            @QueryParam("timeondistanceallowancepernauticalmileasmillis") Long timeOnDistanceAllowancePerNauticalMileAsMillis,
            @QueryParam("searchtag") String searchTag, @QueryParam("competitorName") String competitorName,
            @QueryParam("competitorShortName") String competitorShortName,
            @QueryParam("competitorEmail") String competitorEmail, @QueryParam("deviceUuid") String deviceUuid,
            @QueryParam("secret") String registrationLinkSecret) {
        Response response;
        DynamicBoat boat = getService().getCompetitorAndBoatStore().getExistingBoatByIdAsString(boatId);
        if (boat == null) {
            response = Response.status(Status.NOT_FOUND).entity("Boat is not valid").type(MediaType.TEXT_PLAIN).build();
        } else {
            getSecurityService().checkCurrentUserHasOneOfExplicitPermissions(boat,
                    SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS);
            response = createAndAddCompetitor(regattaName, nationalityThreeLetterIOCCode, displayColor,
                    timeOnTimeFactor, timeOnDistanceAllowancePerNauticalMileAsMillis, searchTag, competitorName,
                    competitorShortName, competitorEmail, flagImageURI, teamImageURI, (t, regattaOwnershipAnnotation, skipPermissionChecks) -> boat, deviceUuid,
                    registrationLinkSecret);
        }
        return response;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/competitors/{competitorid}/remove")
    public Response removeCompetitor(@PathParam("regattaname") String regattaName,
            @PathParam("competitorid") String competitorIdAsString,
            @QueryParam("secret") String registrationLinkSecret) {
        final Subject subject = SecurityUtils.getSubject();
        final User user = getSecurityService().getCurrentUser();
        Response response;
        final Regatta regatta = findRegattaByName(regattaName);
        if (registrationLinkSecret != null && registrationLinkSecret.length() > 0
                && !CompetitorRegistrationType.CLOSED.equals(regatta.getCompetitorRegistrationType())) {
            if (!regatta.getRegistrationLinkSecret().equals(registrationLinkSecret)) {
                return getBadRegattaRegistrationTypeErrorResponse(regattaName);
            }
            final RegattaLog regattaLog = regatta.getRegattaLog();
            final Set<RegattaLogDeviceCompetitorMappingEvent> eventsToRevoke = new HashSet<>();
            regattaLog.lockForRead();
            try {
                for (RegattaLogEvent event : regattaLog.getUnrevokedEvents()) {
                    if (event instanceof RegattaLogDeviceCompetitorMappingEvent) {
                        eventsToRevoke.add((RegattaLogDeviceCompetitorMappingEvent) event);
                    }
                }
            } finally {
                regattaLog.unlockAfterRead();
            }
            for (final RegattaLogDeviceCompetitorMappingEvent eventToRevoke : eventsToRevoke) {
                try {
                    regattaLog.revokeEvent(
                            new LogEventAuthorImpl(user == null ? "anonymous" : user.getFullName(), 0), eventToRevoke,
                            "deregister device " + eventToRevoke.getDevice()
                            .getStringRepresentation());
                } catch (NotRevokableException e) {
                    return getDeregisterCompetitorErrorResponse(regattaName, competitorIdAsString, e.getMessage());
                }
            }
        } else {
            subject.checkPermission(
                    SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        }
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            Serializable competitorId;
            try {
                competitorId = UUID.fromString(competitorIdAsString);
            } catch (IllegalArgumentException e) {
                competitorId = competitorIdAsString;
            }

            final Competitor competitor = getService().getCompetitorAndBoatStore()
                    .getExistingCompetitorById(competitorId);
            if (competitor == null) {
                response = getBadCompetitorIdResponse(competitorId);
            } else {
                getSecurityService().checkCurrentUserHasOneOfExplicitPermissions(competitor,
                        SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS);
                regatta.deregisterCompetitor(competitor); // FIXME what about replication???
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
            @DefaultValue("false") @QueryParam("lastknown") boolean addLastKnown,
            @DefaultValue("false") @QueryParam("raw") boolean raw,
            @HeaderParam(SECONDARY_USER_BEARER_TOKEN) String secondaryUserBearerToken,
            @Context HttpServletRequest request) {
        Response response;
        final String clientIP = HttpRequestUtils.getClientIP(request);
        final String userAgent = HttpRequestUtils.getUserAgent(request);
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                getSecurityService().checkCurrentUserReadPermission(trackedRace);
                checkExportPermission(trackedRace, secondaryUserBearerToken, clientIP, userAgent);
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
                    if (getSecurityService().hasCurrentUserOneOfExplicitPermissions(competitor,
                            SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                        if (competitorIds == null || competitorIds.isEmpty()
                                || competitorIds.contains(competitor.getId().toString())) {
                            JSONObject jsonCompetitor = new JSONObject();
                            jsonCompetitor.put("id", competitor.getId() != null ? competitor.getId().toString() : null);
                            jsonCompetitor.put("name", competitor.getName());
                            jsonCompetitor.put("sailNumber", trackedRace.getBoatOfCompetitor(competitor).getSailID());
                            jsonCompetitor.put("color",
                                    competitor.getColor() != null ? competitor.getColor().getAsHtml() : null);
                            if (competitor.getFlagImage() != null) {
                                jsonCompetitor.put("flagImage", competitor.getFlagImage().toString());
                            }
                            final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
                            JSONArray jsonFixes = new JSONArray();
                            track.lockForRead();
                            try {
                                Iterator<GPSFixMoving> fixIter;
                                if (from == null) {
                                    fixIter = raw?track.getRawFixes().iterator():track.getFixes().iterator();
                                } else {
                                    fixIter = raw?track.getRawFixesIterator(from, /* inclusive */true):track.getFixesIterator(from, /* inclusive */true);
                                }
                                GPSFixMoving fix = null;
                                boolean lastAdded = false;
                                while (fixIter.hasNext()) {
                                    fix = fixIter.next();
                                    if (to != null && fix.getTimePoint() != null
                                            && to.compareTo(fix.getTimePoint()) < 0) {
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
                                    Iterator<GPSFixMoving> earlierFixIter = raw ?
                                        track.getRawFixesDescendingIterator(from, /* inclusive */false) :
                                            track.getFixesDescendingIterator(from, /* inclusive */false);
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
                                    if (earlierFix != null && (fix == null || earlierFix.getTimePoint().until(from)
                                            .compareTo(to.until(fix.getTimePoint())) <= 0)) {
                                        // the earlier fix is closer to the interval's beginning than fix is to its end
                                        addCompetitorFixToJsonFixes(jsonFixes, earlierFix, tack);
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
                }
                jsonRace.put("competitors", jsonCompetitors);
                response = Response.ok(streamingOutput(jsonRace)).build();
            }
        }
        return response;
    }

    private void checkExportPermission(TrackedRace trackedRace, String secondaryUserBearerToken, String clientIP, String userAgent) {
        if (Util.hasLength(secondaryUserBearerToken)) {
            logger.info("Found secondary user bearer token");
            final Subject secondarySubject = new Subject.Builder().buildSubject();
            secondarySubject.login(new BearerAuthenticationToken(secondaryUserBearerToken, clientIP, userAgent));
            logger.info("Authenticated secondary subject for export permission: "+secondarySubject.getPrincipal());
            secondarySubject.checkPermission(trackedRace.getPermissionType().getStringPermissionForObject(SecuredDomainType.TrackedRaceActions.EXPORT, trackedRace));
            logger.info("Secondary subject has export permission for "+trackedRace);
        } else {
            getSecurityService().checkCurrentUserExplicitPermissions(trackedRace, SecuredDomainType.TrackedRaceActions.EXPORT);
        }
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
        if (fix.getOptionalTrueHeading() != null) {
            jsonFix.put("trueheading-deg", fix.getOptionalTrueHeading().getDegrees());
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
            @DefaultValue("false") @QueryParam("lastknown") boolean addLastKnown,
            @HeaderParam(SECONDARY_USER_BEARER_TOKEN) String secondaryUserBearerToken,
            @Context HttpServletRequest request) {
        final String clientIP = HttpRequestUtils.getClientIP(request);
        final String userAgent = HttpRequestUtils.getUserAgent(request);
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            return getBadRegattaErrorResponse(regattaName);
        }
        getSecurityService().checkCurrentUserReadPermission(regatta);
        RaceDefinition race = findRaceByName(regatta, raceName);
        if (race == null) {
            return getBadRaceErrorResponse(regattaName, raceName);
        }
        TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        checkExportPermission(trackedRace, secondaryUserBearerToken, clientIP, userAgent);
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
            final GPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(mark);
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
        return Response.ok(streamingOutput(jsonRace)).build();
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
            getSecurityService().checkCurrentUserReadPermission(regatta);
            TrackedRace trackedRace = findTrackedRace(regatta, raceName);
            if (trackedRace == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                CourseBase course = trackedRace.getRace().getCourse();
                response = getCourseResult(course, trackedRace);
            }
        }
        return response;
    }

    /**
     * Gets the course of the race defined by the raceColumn/fleet tupel.
     *
     * @param regattaName
     *            the name of the regatta
     * @return
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/structure/{raceColumn}/{fleet}/course")
    public Response getCourse(@PathParam("regattaname") String regattaName, @PathParam("raceColumn") String raceColumnName, @PathParam("fleet") String fleetName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            final RaceColumn raceColumn = findRaceColumnByName(regatta, raceColumnName);
            final Fleet fleet = findFleetByName(raceColumn, fleetName);
            if (raceColumn == null || fleet == null) {
                response = getBadRaceErrorResponse(regattaName, raceColumnName, fleetName);
            } else {
                final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                final RaceDefinition raceDefinition = trackedRace == null ? null : trackedRace.getRace();
                final CourseBase course;
                if (raceDefinition != null) {
                    course = raceDefinition.getCourse();
                } else {
                    final LastPublishedCourseDesignFinder courseDesginFinder = new LastPublishedCourseDesignFinder(
                            raceColumn.getRaceLog(fleet), /* onlyCoursesWithValidWaypointList */ true);
                    course = courseDesginFinder.analyze();
                }
                if (course == null) {
                    response = getBadCourseErrorResponse(regattaName, raceColumnName, fleetName);
                } else {
                    response = getCourseResult(course, trackedRace);
                }
            }
        }
        return response;
    }

    /**
     * @param optionalTrackedRace
     *            if not {@code null}, the tracked race will be used to obtain information about the course geometry
     *            which the serializer will put into the result.
     */
    private Response getCourseResult(CourseBase course, TrackedRace optionalTrackedRace) {
        Response response;
        final WaypointJsonSerializer waypointSerializer = new WaypointJsonSerializer(
                new ControlPointJsonSerializer(new MarkJsonSerializer(), new GateJsonSerializer(
                        new MarkJsonSerializer())));
        final JSONObject jsonCourse;
        if (optionalTrackedRace == null) {
            jsonCourse = new CourseBaseJsonSerializer(waypointSerializer).serialize(course);
        } else {
            final CourseGeometry geometry = getCourseGeometry(optionalTrackedRace);
            final TimePoint timePointForStartLine = optionalTrackedRace.getStartOfRace() != null ?
                    optionalTrackedRace.getStartOfRace() : optionalTrackedRace.getStartOfTracking() != null ?
                            optionalTrackedRace.getStartOfTracking() : MillisecondsTimePoint.now();
            final LineDetails startLineDetails = optionalTrackedRace.getStartLine(timePointForStartLine);
            jsonCourse = new CourseBaseWithGeometryJsonSerializer(waypointSerializer).serialize(new Triple<>(course, geometry, startLineDetails));
        }
        response = Response.ok(streamingOutput(jsonCourse)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        return response;
    }

    /**
     * The leg distance and bearing is taken for the time point when the first boat enters the leg, or for the start of the race
     * if no mark passing exists for that leg yet, or for the start of tracking if no start of race exists, or for "now" if no
     * start of tracking time point exists, either. The total distance is computed as the sum of the leg distances, therefore
     * not necessarily representing the total course distance at any single point in time.
     */
    private CourseGeometry getCourseGeometry(TrackedRace trackedRace) {
        assert trackedRace != null;
        final Course course = trackedRace.getRace().getCourse();
        Distance totalDistance = Distance.NULL;
        final Map<Leg, Distance> legDistances = new HashMap<>();
        final Map<Leg, Bearing> legBearings = new HashMap<>();
        course.lockForRead();
        try {
            for (final Leg leg : course.getLegs()) {
                final TrackedLeg trackedLeg = trackedRace.getTrackedLeg(leg);
                final TimePoint timePointForLegGeometry = getTimePointForLegGeometry(trackedRace, leg);
                final Distance legDistance = trackedLeg.getGreatCircleDistance(timePointForLegGeometry);
                legDistances.put(leg, legDistance);
                legBearings.put(leg, trackedLeg.getLegBearing(timePointForLegGeometry));
                totalDistance = legDistance == null || totalDistance == null ? null : totalDistance.add(legDistance);
            }
        } finally {
            course.unlockAfterRead();
        }
        return new CourseGeometry(totalDistance, legDistances, legBearings);
    }

    private TimePoint getTimePointForLegGeometry(TrackedRace trackedRace, Leg leg) {
        final Iterable<MarkPassing> markPassingsForLegStart = trackedRace.getMarkPassingsInOrder(leg.getFrom());
        final Iterator<MarkPassing> firstMarkPassingForLegStartIter = markPassingsForLegStart.iterator();
        final TimePoint result;
        if (firstMarkPassingForLegStartIter.hasNext()) {
            result = firstMarkPassingForLegStartIter.next().getTimePoint();
        } else {
            final TimePoint raceStartTime = trackedRace.getStartOfRace();
            if (raceStartTime != null) {
                result = raceStartTime;
            } else {
                final TimePoint startOfTracking = trackedRace.getStartOfTracking();
                if (startOfTracking != null) {
                    result = startOfTracking;
                } else {
                    result = MillisecondsTimePoint.now();
                }
            }
        }
        return result;
    }

    /**
     * Gets the target time of the race
     *
     * @param regattaName
     *            the name of the regatta
     * @param timeasmillis
     *            a hypothetical time point for the start of the race; this will be used to determine
     *            the mark positions and wind field at the respective time, while carrying forward
     *            the calculated estimated leg durations, adding the previous leg's duration to
     *            obtain an estimated start time point for the next leg.
     * @return 404 response status if not enough polar data or no wind information is available
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
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                final DynamicTrackedRace trackedRace = getService().getTrackedRace(regatta, race);
                if (trackedRace != null) {
                    TargetTimeInfo targetTime;
                    try {
                        targetTime = trackedRace.getEstimatedTimeToComplete(timePoint);
                        final TargetTimeInfoSerializer serializer = new TargetTimeInfoSerializer(new WindJsonSerializer(new PositionJsonSerializer()));
                        JSONObject jsonCourse = serializer.serialize(targetTime);
                        response = Response.ok(streamingOutput(jsonCourse)).build();
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

    /** gets the relevant times for multiple race names */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/times")
    public Response getMultiTimes(@PathParam("regattaname") String regattaName,
            @QueryParam("racename") final List<String> raceNames, @QueryParam("secret") String regattaSecret) {
        final JSONArray resultJson = new JSONArray();

        Response response = null;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            if (!getService().skipChecksDueToCorrectSecret(regattaName, regattaSecret)) {
                getSecurityService().checkCurrentUserReadPermission(regatta);
            }

            for (String raceName : raceNames) {
                RaceDefinition race = findRaceByName(regatta, raceName);
                if (race != null) {
                    resultJson.add(getRaceTimesJSONForRaceName(raceName, regatta));
                }
            }
            response = Response.ok(streamingOutput(resultJson)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    /**
     * Calculates all start analysis parameters for all competitors in the race.
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/startanalysis")
    public Response getStartAnalysis(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName,
            @QueryParam("secret") String regattaSecret) {
        Response response = null;
        final Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            if (!getService().skipChecksDueToCorrectSecret(regattaName, regattaSecret)) {
                getSecurityService().checkCurrentUserReadPermission(regatta);
            }
            final TrackedRace trackedRace = findTrackedRace(regatta, raceName);
            if (trackedRace == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                try {
                    final JSONObject jsonStartAnalysis = getStartAnalysis(trackedRace, regatta);
                    response = Response.ok(streamingOutput(jsonStartAnalysis)).build();
                } catch (NoWindException | InterruptedException | ExecutionException e) {
                    response = Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Error computing start analysis for race '" + StringEscapeUtils.escapeHtml(raceName) + "' in regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "': "+
                                    StringEscapeUtils.escapeHtml(e.getMessage()))
                            .type(MediaType.TEXT_PLAIN).build();
                }
            }
        }
        return response;
    }

    private JSONObject getStartAnalysis(TrackedRace trackedRace, Regatta regatta) throws NoWindException, InterruptedException, ExecutionException {
        assert trackedRace != null;
        final Leaderboard leaderboard = getService().getLeaderboardByName(regatta.getName());
        final CompetitorJsonSerializer competitorSerializer = new CompetitorJsonSerializer(new TeamJsonSerializer(new PersonJsonSerializer(
                new NationalityJsonSerializer())),
                BoatJsonSerializer.create(), /* serialize non-public fields */ false);
        final JSONObject result = new JSONObject();
        final JSONArray competitorStartAnalysis = new JSONArray();
        if (leaderboard != null) {
            final RaceDefinition race = trackedRace.getRace();
            final Map<String, Competitor> competitorByIdAsString = new HashMap<>();
            for (final Competitor c : race.getCompetitors()) {
                competitorByIdAsString.put(c.getId().toString(), c);
            }
            final Pair<RaceColumn, Fleet> raceColumnAndFleet = regatta.getRaceColumnAndFleet(trackedRace);
            if (raceColumnAndFleet != null && raceColumnAndFleet.getA() != null) {
                final LeaderboardDTO latestLeaderboard = leaderboard.getLeaderboardDTO(/* live time point */ null,
                        Arrays.asList(raceColumnAndFleet.getA().getName()), /* addOverallDetails */ false, getService(),
                        getService().getBaseDomainFactory(), /* fillTotalPointsUncorrected */ false);
                if (latestLeaderboard != null) {
                    result.put("startline", getStartLineData(trackedRace));
                    for (final Entry<CompetitorDTO, LeaderboardRowDTO> e : latestLeaderboard.rows.entrySet()) {
                        final Competitor competitor = competitorByIdAsString.get(e.getKey().getIdAsString());
                        if (competitor != null) {
                            final LeaderboardRowDTO competitorRow = e.getValue();
                            final LeaderboardEntryDTO entry = competitorRow.fieldsByRaceColumnName.get(raceColumnAndFleet.getA().getName());
                            if (entry != null) {
                                final JSONObject competitorStartAnalysisJson = new JSONObject();
                                competitorStartAnalysisJson.put("competitor",  competitorSerializer.serialize(competitor));
                                competitorStartAnalysisJson.put("distanceToStarboardSideOfStartLineInMeters", entry.distanceToStarboardSideOfStartLineInMeters);
                                competitorStartAnalysisJson.put("distanceToStartLineAtStartOfRaceInMeters", entry.distanceToStartLineAtStartOfRaceInMeters);
                                competitorStartAnalysisJson.put("distanceToStartLineFiveSecondsBeforeStartInMeters", entry.distanceToStartLineFiveSecondsBeforeStartInMeters);
                                competitorStartAnalysisJson.put("speedOverGroundAtPassingStartWaypointInKnots", entry.speedOverGroundAtPassingStartWaypointInKnots);
                                competitorStartAnalysisJson.put("speedOverGroundAtStartOfRaceInKnots", entry.speedOverGroundAtStartOfRaceInKnots);
                                competitorStartAnalysisJson.put("speedOverGroundFiveSecondsBeforeStartInKnots", entry.speedOverGroundFiveSecondsBeforeStartInKnots);
                                competitorStartAnalysisJson.put("timeBetweenRaceStartAndCompetitorStartInSeconds", entry.timeBetweenRaceStartAndCompetitorStartInSeconds);
                                competitorStartAnalysisJson.put("startTack", entry.startTack);
                                competitorStartAnalysis.add(competitorStartAnalysisJson);
                            }
                        }
                    }
                }
            }
        }
        result.put("competitors", competitorStartAnalysis);
        return result;
    }

    private JSONObject getStartLineData(final TrackedRace trackedRace) {
        final TimePoint startOfRace = trackedRace.getStartOfRace();
        final JSONObject result;
        if (startOfRace != null) {
            final LineDetails lineInfo = trackedRace.getStartLine(startOfRace);
            result = new JSONObject();
            result.put("lengthInMeters", lineInfo.getLength().getMeters());
            result.put("favoredEnd", lineInfo.getAdvantageousSideWhileApproachingLine().name());
            result.put("biasInMeters", lineInfo.getAdvantage().getMeters());
        } else {
            result = null;
        }
        return result;
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
    public Response getTimes(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName,
            @QueryParam("secret") String regattaSecret) {
        Response response = null;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            if (!getService().skipChecksDueToCorrectSecret(regattaName, regattaSecret)) {
                getSecurityService().checkCurrentUserReadPermission(regatta);
            }
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {
                JSONObject jsonRaceTimes = getRaceTimesJSONForRaceName(raceName, regatta);
                response = Response.ok(streamingOutput(jsonRaceTimes)).build();
            }
        }
        return response;
    }

    private JSONObject getRaceTimesJSONForRaceName(String raceName, Regatta regatta) {
        TrackedRace trackedRace = findTrackedRace(regatta.getName(), raceName);

        JSONObject jsonRaceTimes = new JSONObject();
        jsonRaceTimes.put("name", trackedRace.getRace().getName());
        jsonRaceTimes.put("regatta", regatta.getName());

        jsonRaceTimes.put("startOfRace-ms",
                trackedRace.getStartOfRace() == null ? null : trackedRace.getStartOfRace().asMillis());
        jsonRaceTimes.put("startOfTracking-ms",
                trackedRace.getStartOfTracking() == null ? null : trackedRace.getStartOfTracking().asMillis());
        jsonRaceTimes.put("newestTrackingEvent-ms", trackedRace.getTimePointOfNewestEvent() == null ? null
                : trackedRace.getTimePointOfNewestEvent().asMillis());
        jsonRaceTimes.put("endOfTracking-ms",
                trackedRace.getEndOfTracking() == null ? null : trackedRace.getEndOfTracking().asMillis());
        jsonRaceTimes.put("endOfRace-ms",
                trackedRace.getEndOfRace() == null ? null : trackedRace.getEndOfRace().asMillis());
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
                jsonMarkPassing.put("firstPassing-ms", firstPassingTime == null ? null : firstPassingTime.asMillis());
                jsonMarkPassing.put("lastPassing-ms", lastPassingTime == null ? null : lastPassingTime.asMillis());

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
                        jsonLegInfo.put("bearing-deg", RoundingUtil.bearingDecimalFormatter
                                .format(trackedLeg.getLegBearing(firstPassingTime).getDegrees()));
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
        return jsonRaceTimes;
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
            getSecurityService().checkCurrentUserReadPermission(regatta);
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
    @Path("{regattaname}/races/{racename}/highQualityWindFixes")
    public Response getHighQualityWindFixes(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName,
            @HeaderParam(SECONDARY_USER_BEARER_TOKEN) String secondaryUserBearerToken,
            @Context HttpServletRequest request) {
        Response response;
        final String clientIP = HttpRequestUtils.getClientIP(request);
        final String userAgent = HttpRequestUtils.getUserAgent(request);
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.")
                        .type(MediaType.TEXT_PLAIN).build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                getSecurityService().checkCurrentUserReadPermission(regatta);
                getSecurityService().checkCurrentUserReadPermission(trackedRace);
                checkExportPermission(trackedRace, secondaryUserBearerToken, clientIP, userAgent);
                RaceWindJsonSerializer serializer = new RaceWindJsonSerializer();
                JSONObject jsonWindTracks = serializer.serialize(trackedRace);
                return Response.ok(streamingOutput(jsonWindTracks)).build();
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
            @QueryParam("totimeasmillis") Long totimeasmillis,
            @HeaderParam(SECONDARY_USER_BEARER_TOKEN) String secondaryUserBearerToken,
            @Context HttpServletRequest request) {
        Response response;
        final String clientIP = HttpRequestUtils.getClientIP(request);
        final String userAgent = HttpRequestUtils.getUserAgent(request);
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            if (!((fromtime != null && totime != null) || (fromtimeasmillis != null && totimeasmillis != null))) {
                response = Response.status(Status.NOT_FOUND).entity(
                        "Either the 'fromtime' and 'totime' or the 'fromtimeasmillis' and 'totimeasmillis' parameter must be set.")
                        .type(MediaType.TEXT_PLAIN).build();
            } else {
                RaceDefinition race = findRaceByName(regatta, raceName);
                if (race == null) {
                    response = Response.status(Status.NOT_FOUND)
                            .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.")
                            .type(MediaType.TEXT_PLAIN).build();
                } else {
                    final TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                    getSecurityService().checkCurrentUserReadPermission(trackedRace);
                    checkExportPermission(trackedRace, secondaryUserBearerToken, clientIP, userAgent);
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
                    // Crop request interval to startOfTracking / [endOfTracking|timePointOfLastEvent]
                    final TimePoint finalFrom = Util.getLatestOfTimePoints(from, trackedRace.getStartOfTracking());
                    final TimePoint finalTo = Util.getEarliestOfTimePoints(to, Util.getEarliestOfTimePoints(
                            trackedRace.getEndOfTracking(), trackedRace.getTimePointOfNewestEvent()));
                    final TrackedRaceJsonSerializer serializer = new TrackedRaceJsonSerializer(
                            ws -> new DefaultWindTrackJsonSerializer(/* maxNumberOfFixes */ 10000, finalFrom, finalTo,
                                    ws),
                            windSource, windSourceId);

                    final JSONObject jsonWindTracks = serializer.serialize(trackedRace);
                    response = Response.ok(streamingOutput(jsonWindTracks)).build();
                }
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/windsummary")
    public Response getWindSummary(@PathParam("regattaname") String regattaName,
            @QueryParam("racecolumn") String raceColumnName, @QueryParam("fleet") String fleetName,
            @QueryParam("secret") String regattaSecret) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                .build();
        } else {
            boolean skip = getService().skipChecksDueToCorrectSecret(regattaName, regattaSecret);
            if (!skip) {
                getSecurityService().checkCurrentUserReadPermission(regatta);
            }
            final JSONArray result = new JSONArray();
            final Iterable<? extends RaceColumn> raceColumns;
            if (raceColumnName != null) {
                final RaceColumn raceColumn = regatta.getRaceColumnByName(raceColumnName);
                if (raceColumn == null) {
                    return Response.status(Status.NOT_FOUND)
                            .entity("Could not find a race column with name '" + StringEscapeUtils.escapeHtml(raceColumnName) + "'.").type(MediaType.TEXT_PLAIN)
                            .build();
                }
                raceColumns = Collections.singleton(raceColumn);
            } else {
                raceColumns = regatta.getRaceColumns();
            }
            for (final RaceColumn raceColumn : raceColumns) {
                final Iterable<? extends Fleet> fleets;
                if (fleetName != null) {
                    final Fleet fleet = raceColumn.getFleetByName(fleetName);
                    if (fleet == null) {
                        return Response.status(Status.NOT_FOUND)
                                .entity("Could not find a fleet with name '" + StringEscapeUtils.escapeHtml(fleetName) +
                                        "' in race column '"+raceColumn.getName()+"'.").type(MediaType.TEXT_PLAIN).build();
                    }
                    fleets = Collections.singleton(fleet);
                } else {
                    fleets = raceColumn.getFleets();
                }
                for (final Fleet fleet : fleets) {
                    final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                    final RaceLog raceLog = raceColumn.getRaceLog(fleet);
                    final WindSummary windSummary = new RaceWindCalculator().getWindSummary(trackedRace, raceLog);
                    result.add(toJson(raceColumn, fleet, windSummary));
                }
            }
            response = Response.ok(streamingOutput(result)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    private JSONObject toJson(RaceColumn raceColumn, Fleet fleet, WindSummary windSummary) {
        final JSONObject result = new JSONObject();
        result.put("racecolumn", raceColumn.getName());
        result.put("fleet", fleet.getName());
        result.put("trueLowerboundWindInKnots", windSummary == null ? null : windSummary.getTrueLowerboundWind().getKnots());
        result.put("trueUppwerboundWindInKnots", windSummary == null ? null : windSummary.getTrueUpperboundWind().getKnots());
        result.put("trueWindDirectionInDegrees", windSummary == null ? null : windSummary.getTrueWindDirection().getDegrees());
        return result;
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
            getSecurityService().checkCurrentUserReadPermission(regatta);
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
                return Response.ok(streamingOutput(jsonBearing)).build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/markpassings")
    public Response getMarkPassings(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName,
    		@QueryParam("leaderboard") String leaderboardName) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                final Leaderboard leaderboard = leaderboardName == null
                        ? getService().getLeaderboardByName(regattaName)
                        : getService().getLeaderboardByName(leaderboardName);
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                getSecurityService().checkCurrentUserReadPermission(trackedRace);
                AbstractTrackedRaceDataJsonSerializer serializer = new MarkPassingsJsonSerializer(leaderboard,
                		/* use now-livedelay as time point for mark ranks */ null);
                JSONObject jsonMarkPassings = serializer.serialize(trackedRace);
                return Response.ok(streamingOutput(jsonMarkPassings)).build();
            }
        }
        return response;
    }

    private TimePoint determineEndTimeForManeuverDetection(TrackedRace trackedRace) {
        final TimePoint endOfRace = trackedRace.getEndOfRace();
        final TimePoint endTime;
        if (endOfRace != null) {
            endTime = endOfRace;
        } else {
            final TimePoint endOfTracking = trackedRace.getEndOfTracking();
            if (endOfTracking == null || endOfTracking.after(MillisecondsTimePoint.now())) {
                endTime = MillisecondsTimePoint.now();
            } else {
                endTime = endOfTracking;
            }
        }
        return endTime;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/maneuvers")
    public Response getManeuvers(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName,
            @QueryParam("competitorId") String competitorId, @QueryParam("fromTime") String fromTime) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.")
                        .type(MediaType.TEXT_PLAIN).build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                if (trackedRace == null) {
                    response = Response.status(Status.NOT_FOUND).entity(
                            "Could not find a trackedrace with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.")
                            .type(MediaType.TEXT_PLAIN).build();
                } else {
                    List<Pair<Competitor, Iterable<Maneuver>>> data = new ArrayList<>();
                    Iterable<Competitor> competitors = trackedRace.getRace().getCompetitors();
                    UUID competitorFilter = null;
                    if (competitorId != null) {
                        competitorFilter = UUID.fromString(competitorId);
                    }
                    final TimePoint endTime = determineEndTimeForManeuverDetection(trackedRace);
                    final TimePoint startTime;
                    if (fromTime != null) {
                        startTime = new MillisecondsTimePoint(Long.parseLong(fromTime));
                    } else {
                        startTime = trackedRace.getStartOfRace();
                    }

                    for (Competitor competitor : competitors) {
                        if (getSecurityService().hasCurrentUserOneOfExplicitPermissions(competitor,
                                SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                            if (competitorFilter == null || competitor.getId().equals(competitorFilter)) {
                                Iterable<Maneuver> maneuversForCompetitor = trackedRace.getManeuvers(competitor, startTime, endTime, false);
                                data.add(new Pair<Competitor, Iterable<Maneuver>>(competitor, maneuversForCompetitor));
                            }
                        }
                    }

                    ManeuversJsonSerializer serializer = new ManeuversJsonSerializer(
                            new ManeuverJsonSerializer(new GPSFixJsonSerializer(), new DistanceJsonSerializer()));
                    JSONObject jsonMarkPassings = serializer.serialize(data);
                    return Response.ok(streamingOutput(jsonMarkPassings)).build();
                }
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/completeManeuverCurvesWithEstimationData")
    public Response getCompleteManeuverCurvesWithEstimationData(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName,
            @QueryParam("startBeforeStartLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer startBeforeStartLineInSeconds,
            @QueryParam("endBeforeStartLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer endBeforeStartLineInSeconds,
            @QueryParam("startAfterFinishLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer startAfterFinishLineInSeconds,
            @QueryParam("endAfterFinishLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer endAfterFinishLineInSeconds) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.")
                        .type(MediaType.TEXT_PLAIN).build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                CompetitorTrackWithEstimationDataJsonSerializer serializer = new CompetitorTrackWithEstimationDataJsonSerializer(
                        getService().getPolarDataService(), getSecurityService(), new DetailedBoatClassJsonSerializer(),
                        new CompleteManeuverCurvesWithEstimationDataJsonSerializer(getService().getPolarDataService(),
                                new CompleteManeuverCurveWithEstimationDataJsonSerializer(
                                        new ManeuverMainCurveWithEstimationDataJsonSerializer(),
                                        new ManeuverCurveWithUnstableCourseAndSpeedWithEstimationDataJsonSerializer(),
                                        new ManeuverWindJsonSerializer(), new PositionJsonSerializer())),
                        getNullableValueFromDefault(startBeforeStartLineInSeconds),
                        getNullableValueFromDefault(endBeforeStartLineInSeconds),
                        getNullableValueFromDefault(startAfterFinishLineInSeconds),
                        getNullableValueFromDefault(endAfterFinishLineInSeconds));
                JSONObject jsonMarkPassings = serializer.serialize(trackedRace);
                return Response.ok(streamingOutput(jsonMarkPassings)).build();
            }
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/gpsFixesWithEstimationData")
    public Response getGpsFixesWithEstimationData(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName, @QueryParam("addWind") @DefaultValue("true") Boolean addWind,
            @QueryParam("addNextWaypoint") @DefaultValue("true") Boolean addNextWaypoint,
            @QueryParam("smoothFixes") @DefaultValue("true") Boolean smoothFixes,
            @QueryParam("startBeforeStartLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer startBeforeStartLineInSeconds,
            @QueryParam("endBeforeStartLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer endBeforeStartLineInSeconds,
            @QueryParam("startAfterFinishLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer startAfterFinishLineInSeconds,
            @QueryParam("endAfterFinishLineInSeconds") @DefaultValue(Integer.MIN_VALUE
                    + "") Integer endAfterFinishLineInSeconds) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.")
                        .type(MediaType.TEXT_PLAIN).build();
            } else {
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                CompetitorTrackWithEstimationDataJsonSerializer serializer = new CompetitorTrackWithEstimationDataJsonSerializer(
                        getService().getPolarDataService(), getSecurityService(), new DetailedBoatClassJsonSerializer(),
                        new GpsFixesWithEstimationDataJsonSerializer(new GPSFixMovingJsonSerializer(),
                                new ManeuverWindJsonSerializer(), addWind, addNextWaypoint, smoothFixes),
                        getNullableValueFromDefault(startBeforeStartLineInSeconds),
                        getNullableValueFromDefault(endBeforeStartLineInSeconds),
                        getNullableValueFromDefault(startAfterFinishLineInSeconds),
                        getNullableValueFromDefault(endAfterFinishLineInSeconds));
                JSONObject jsonMarkPassings = serializer.serialize(trackedRace);
                return Response.ok(streamingOutput(jsonMarkPassings)).build();
            }
        }
        return response;
    }
    
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/competitordata")
    public Response getCompetitorRaceData(
            @PathParam("regattaname") final String regattaName,
            @PathParam("racename") final String raceName,
            @QueryParam("fromtime") final String fromtime, @QueryParam("fromtimeasmillis") Long fromtimeasmillis,
            @QueryParam("totime") final String totime, @QueryParam("totimeasmillis") Long totimeasmillis,
            @QueryParam("competitorId") final Set<String> competitorIds,
            @QueryParam("detailType") final Set<DetailType> detailTypes,
            @QueryParam("leaderboardGroupNameOrUUID") final String leaderboardGroupName,
            @QueryParam("leaderboardName") final String leaderboardName,
            @QueryParam("stepSizeMillis") @DefaultValue("1000") final long stepSizeMillis) {
        final Response response;
        final ConcurrentHashMap<TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache> cachesByTimePoint = new ConcurrentHashMap<>();
        final TimePoint from;
        final TimePoint to;
        final Regatta regatta = findRegattaByName(regattaName);
        final TrackedRace trackedRace;
        final Serializable uuid = UUIDHelper.tryUuidConversion(leaderboardGroupName);
        final LeaderboardGroup leaderboardGroup;
        if (uuid != leaderboardGroupName) {
            leaderboardGroup = getService().getLeaderboardGroupByID((UUID) uuid);
        } else {
            leaderboardGroup = getService().getLeaderboardGroupByName(leaderboardGroupName);
        }
        if (leaderboardGroup == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a leaderboard group with name '"
                            + StringEscapeUtils.escapeHtml(leaderboardGroupName) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            if (regatta == null) {
                return getBadRegattaErrorResponse(regattaName);
            } else {
                RaceDefinition race = findRaceByName(regatta, raceName);
                if (race == null) {
                    return getBadRaceErrorResponse(regattaName, raceName);
                } else {
                    trackedRace = findTrackedRace(regattaName, raceName);
                    if (trackedRace == null) {
                        return getBadRaceErrorResponse(regattaName, raceName);
                    }
                    getSecurityService().checkCurrentUserReadPermission(trackedRace);
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
                }
            }
            final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
            final int MAX_NUMBER_OF_FIXES_TO_QUERY = getSecurityService().hasCurrentUserExplicitPermissions(leaderboard, LeaderboardActions.PREMIUM_LEADERBOARD_INFORMATION) ? 1000 : 50000;
            final TimePoint newestEvent = trackedRace.getTimePointOfNewestEvent();
            final TimePoint startTime = from == null ? trackedRace.getStartOfTracking() : from;
            final TimePoint endTime = (to == null || to.after(newestEvent)) ? newestEvent : to;
            final long adjustedStepSizeInMillis = (long) Math.max((double) stepSizeMillis, startTime.until(endTime).divide(MAX_NUMBER_OF_FIXES_TO_QUERY).asMillis());
            final int MAX_CACHE_SIZE = MAX_NUMBER_OF_FIXES_TO_QUERY;
            for (final DetailType detailType : detailTypes) {
                if (detailType.getPremiumAction() != null && leaderboard.getPermissionType().supports(detailType.getPremiumAction())) {
                    getSecurityService().checkCurrentUserExplicitPermissions(leaderboard, detailType.getPremiumAction());
                }
            }
            final Map<Competitor, FutureTask<Iterable<Pair<TimePoint, Map<DetailType, Double>>>>> resultFutures = new HashMap<>();
            for (final String competitorIdAsString : competitorIds) {
                final Competitor competitor = getService().getCompetitorAndBoatStore().getExistingCompetitorByIdAsString(competitorIdAsString);
                if (competitor == null) {
                    return getBadCompetitorIdResponse(competitorIdAsString);
                }
                getSecurityService().checkCurrentUserExplicitPermissions(competitor, SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC);
                final FutureTask<Iterable<Pair<TimePoint, Map<DetailType, Double>>>> future =new FutureTask<Iterable<Pair<TimePoint, Map<DetailType, Double>>>>(
                        new Callable<Iterable<Pair<TimePoint, Map<DetailType, Double>>>>() {
                    @Override
                            public Iterable<Pair<TimePoint, Map<DetailType, Double>>> call()
                                    throws NoWindException, NotEnoughDataHasBeenAddedException,
                                    MaxIterationsExceededException, FunctionEvaluationException {
                                final List<Pair<TimePoint, Map<DetailType, Double>>> raceData = new ArrayList<>();
                        if (startTime != null && endTime != null) {
                            for (long i = startTime.asMillis(); i <= endTime.asMillis(); i += adjustedStepSizeInMillis) {
                                final TimePoint time = TimePoint.of(i);
                                WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = cachesByTimePoint.get(time);
                                if (cache == null) {
                                    cache = new LeaderboardDTOCalculationReuseCache(time);
                                    // ensure maximum number of cache objects:
                                    if (cachesByTimePoint.size() >= MAX_CACHE_SIZE) {
                                        final Iterator<Entry<TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache>> iterator = cachesByTimePoint.entrySet().iterator();
                                        while (cachesByTimePoint.size() >= MAX_CACHE_SIZE && iterator.hasNext()) {
                                            iterator.next();
                                            iterator.remove();
                                        }
                                    }
                                    cachesByTimePoint.put(time, cache);
                                }
                                final Map<DetailType, Double> valuesForTimepoint = new HashMap<>();
                                raceData.add(new Pair<>(time, valuesForTimepoint));
                                for (final DetailType detailType : detailTypes) {
                                    final Double competitorRaceData = getService()
                                            .getCompetitorRaceDataEntry(detailType, trackedRace, competitor,
                                                    time, leaderboardGroup, leaderboardName, cache);
                                    if (competitorRaceData != null) {
                                        valuesForTimepoint.put(detailType, competitorRaceData);
                                    }
                                }
                            }
                        }
                        return raceData;
                    }
                });
                resultFutures.put(competitor, future);
                executor.execute(future); // security checks happen before; no need to associate future with Subject/session
            }
            final JSONObject result = new JSONObject();
            for (Entry<Competitor, FutureTask<Iterable<Pair<TimePoint, Map<DetailType, Double>>>>> e : resultFutures.entrySet()) {
                try {
                    final JSONArray resultsForCompetitor = new JSONArray();
                    result.put(e.getKey().getId().toString(), resultsForCompetitor);
                    for (final Pair<TimePoint, Map<DetailType, Double>> competitorData : e.getValue().get()) {
                        final JSONObject resultsForCompetitorAtTimepoint = new JSONObject();
                        resultsForCompetitorAtTimepoint.put("timepoint-ms", competitorData.getA().asMillis());
                        final JSONObject resultsForCompetitorByDetailType = new JSONObject();
                        for (final Entry<DetailType, Double> entry : competitorData.getB().entrySet()) {
                            resultsForCompetitorByDetailType.put(entry.getKey().name(), entry.getValue());
                        }
                        resultsForCompetitorAtTimepoint.put("values", resultsForCompetitorByDetailType);
                        resultsForCompetitor.add(resultsForCompetitorAtTimepoint);
                    }
                } catch (InterruptedException | ExecutionException e1) {
                    logger.log(Level.SEVERE, "Exception while trying to compute competitor data "+detailTypes+" for competitor "+e.getKey().getName(), e1);
                }
            }
            response = Response.ok(streamingOutput(result)).build();
        }
        return response;
    }

    private Integer getNullableValueFromDefault(Integer value) {
        return Integer.MIN_VALUE == value ? null : value;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races")
    public Response getRaces(@PathParam("regattaname") String regattaName, @QueryParam("secret") String regattaSecret) {
        Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            boolean skip = getService().skipChecksDueToCorrectSecret(regattaName, regattaSecret);
            if (!skip) {
                getSecurityService().checkCurrentUserReadPermission(regatta);
            }
            JSONObject jsonRaceResults = new JSONObject();
            jsonRaceResults.put("regatta", regatta.getName());
            JSONArray jsonRaces = new JSONArray();
            jsonRaceResults.put("races", jsonRaces);
            for (RaceDefinition race : regatta.getAllRaces()) {
                JSONObject jsonRace = new JSONObject();
                jsonRaces.add(jsonRace);
                jsonRace.put("name", race.getName());
                jsonRace.put("courseName", race.getCourse() != null ? race.getCourse().getName() : "");
                jsonRace.put("id", race.getId().toString());
            }
            return Response.ok(streamingOutput(jsonRaceResults)).build();
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/competitors/legs")
    public Response getCompetitorRanks(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName) {
        Response response;
        final Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            final RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                final TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                final Leaderboard leaderboard = getService().getLeaderboardByName(regattaName);
                getSecurityService().checkCurrentUserReadPermission(trackedRace);
                getSecurityService().checkCurrentUserReadPermission(leaderboard);
                if (leaderboard == null) {
                    response = Response.status(Status.NOT_FOUND)
                            .entity("Could not find a leaderboard with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                            .build();
                } else {
                    final Pair<RaceColumn, Fleet> raceColumnAndFleet = leaderboard.getRaceColumnAndFleet(trackedRace);
                    if (raceColumnAndFleet == null) {
                        response = Response.status(Status.NOT_FOUND)
                                .entity("Could not find a race column for race "+StringEscapeUtils.escapeHtml(raceName)+
                                        " in leaderboard with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                                .build();
                    } else {
                        final CompetitorRanksRequest cacheKey = new CompetitorRanksRequest(regatta, raceColumnAndFleet.getA(), raceColumnAndFleet.getB());
                        try {
                            final JSONObject jsonRaceResults = competitorRanksCache.get(cacheKey);
                            response = Response.ok(streamingOutput(jsonRaceResults)).build();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Exception while trying to compute competitor ranks for regatta "+regattaName+", race "+raceName, e);
                            response = Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .entity("Exception while trying to compute leaderboard '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                                    .build();
                        }
                    }
                }
            }
        }
        return response;
    }

    private JSONObject computeCompetitorRanks(Regatta regatta, RaceColumn raceColumn, Fleet fleet) {
        final Leaderboard leaderboard = getService().getLeaderboardByName(regatta.getName());
        final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
        final String raceColumnName = raceColumn.getName();
        LeaderboardDTO leaderboardDTO;
        try {
            ShardingContext.setShardingConstraint(ShardingType.LEADERBOARDNAME, regatta.getName());
            try {
                leaderboardDTO = leaderboard.getLeaderboardDTO(/* live time point */ null,
                        Collections.singleton(raceColumnName), /* addOverallDetails */ false,
                        /* trackedRegattaRegistry */ getService(), getService().getBaseDomainFactory(), /* fillTotalPointsUncorrected */ false);
            } finally {
                ShardingContext.clearShardingConstraint(ShardingType.LEADERBOARDNAME);
            }
            final Map<String, CompetitorDTO> competitorDTOsByIdAsString = new HashMap<>();
            for (final CompetitorDTO competitorDTO : leaderboardDTO.rows.keySet()) {
                competitorDTOsByIdAsString.put(competitorDTO.getIdAsString(), competitorDTO);
            }
            final TimePoint timePoint = TimePoint.of(leaderboardDTO.getTimePoint());
            final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = new LeaderboardDTOCalculationReuseCache(timePoint);
            final RankingInfo rankingInfo = trackedRace.getRankingMetric().getRankingInfo(timePoint, cache);
            final JSONObject jsonRaceResults = new JSONObject();
            jsonRaceResults.put("name", trackedRace.getRace().getName());
            jsonRaceResults.put("regatta", regatta.getName());
            jsonRaceResults.put("startOfRace-ms", trackedRace.getStartOfRace() == null ? null : trackedRace.getStartOfRace().asMillis());
            final JSONArray jsonLegs = new JSONArray();
            final Course course = trackedRace.getRace().getCourse();
            course.lockForRead();
            try {
                int zeroBasedLegIndex = 0;
                for (final TrackedLeg leg : trackedRace.getTrackedLegs()) {
                    final JSONObject jsonLeg = new JSONObject();
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
                    final JSONArray jsonCompetitors = new JSONArray();
                    for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                        if (getSecurityService().hasCurrentUserOneOfExplicitPermissions(competitor,
                                SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                            final JSONObject jsonCompetitorInLeg = new JSONObject();
                            final TrackedLegOfCompetitor trackedLegOfCompetitor = leg.getTrackedLeg(competitor);
                            final LeaderboardRowDTO row = leaderboardDTO.rows.get(competitorDTOsByIdAsString.get(competitor.getId().toString()));
                            final LeaderboardEntryDTO entry = row.fieldsByRaceColumnName.get(raceColumnName);
                            final LegEntryDTO legDetail = entry.legDetails.get(zeroBasedLegIndex);
                            if (trackedLegOfCompetitor != null) {
                                jsonCompetitorInLeg.put("id",
                                        competitor.getId() != null ? competitor.getId().toString() : null);
                                jsonCompetitorInLeg.put("name", competitor.getName());
                                jsonCompetitorInLeg.put("sailNumber",
                                        trackedRace.getBoatOfCompetitor(competitor).getSailID());
                                jsonCompetitorInLeg.put("color",
                                        competitor.getColor() != null ? competitor.getColor().getAsHtml() : null);
                                final Double averageSpeedOverGroundInKnots = legDetail == null ? null : legDetail.averageSpeedOverGroundInKnots;
                                if (averageSpeedOverGroundInKnots != null) {
                                    jsonCompetitorInLeg.put("averageSOG-kts", RoundingUtil.knotsDecimalFormatter
                                            .format(averageSpeedOverGroundInKnots));
                                }
                                final Pair<GPSFixMoving, Speed> maxSpeedOverGround = trackedLegOfCompetitor.getMaximumSpeedOverGround(timePoint);
                                if (maxSpeedOverGround != null) {
                                    jsonCompetitorInLeg.put("maxSOG-kts", RoundingUtil.knotsDecimalFormatter
                                            .format(maxSpeedOverGround.getB().getKnots()));
                                    jsonCompetitorInLeg.put("maxSOGTimePoint-millis", RoundingUtil.knotsDecimalFormatter
                                            .format(maxSpeedOverGround.getA().getTimePoint().asMillis()));
                                }
                                final boolean hasFinishedLeg = trackedLegOfCompetitor.hasFinishedLeg(timePoint);
                                if (!hasFinishedLeg) {
                                    final Distance currentSignedXTE = trackedLegOfCompetitor.getSignedCrossTrackError(timePoint);
                                    if (currentSignedXTE != null) {
                                        jsonCompetitorInLeg.put("currentSignedXTE-m", currentSignedXTE.getMeters());
                                    }
                                    final Distance currentAbsolutXTE = trackedLegOfCompetitor.getAbsoluteCrossTrackError(timePoint);
                                    if (currentAbsolutXTE != null) {
                                        jsonCompetitorInLeg.put("currentAbsolutXTE-m", currentAbsolutXTE.getMeters());
                                    }
                                }
                                final Distance averageSignedXTE = trackedLegOfCompetitor.getAverageSignedCrossTrackError(timePoint, /* waitForLatest */ false);
                                if (averageSignedXTE != null) {
                                    jsonCompetitorInLeg.put("averageSignedXTE-m", averageSignedXTE.getMeters());
                                }
                                final Distance averageAbsolutXTE = trackedLegOfCompetitor.getAverageAbsoluteCrossTrackError(timePoint, /* waitForLatest */ false);
                                if (averageAbsolutXTE != null) {
                                    jsonCompetitorInLeg.put("averageAbsolutXTE-m", averageAbsolutXTE.getMeters());
                                }
                                Integer numberOfTacks = legDetail == null ? null :
                                    legDetail.numberOfManeuvers == null ? 0 : legDetail.numberOfManeuvers.getOrDefault(ManeuverType.TACK, 0);
                                Integer numberOfJibes = legDetail == null ? null :
                                    legDetail.numberOfManeuvers == null ? 0 : legDetail.numberOfManeuvers.getOrDefault(ManeuverType.JIBE, 0);
                                Integer numberOfPenaltyCircles = legDetail == null ? null :
                                    legDetail.numberOfManeuvers == null ? 0 : legDetail.numberOfManeuvers.getOrDefault(ManeuverType.PENALTY_CIRCLE, 0);
                                jsonCompetitorInLeg.put("tacks", numberOfTacks);
                                jsonCompetitorInLeg.put("jibes", numberOfJibes);
                                jsonCompetitorInLeg.put("penaltyCircles", numberOfPenaltyCircles);
                                final TimePoint startTime = trackedLegOfCompetitor.getStartTime();
                                final TimePoint finishTime = trackedLegOfCompetitor.getFinishTime();
                                final TimePoint startOfRace = trackedRace.getStartOfRace();
                                // between the start of the race and the start of the first leg we have no
                                // 'timeSinceGun' for the competitor
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
                                    Distance distanceSinceGun = trackedRace.getTrack(competitor)
                                            .getDistanceTraveled(startOfRace,
                                                    finishTime != null ? finishTime : timePoint);
                                    if (distanceSinceGun != null) {
                                        jsonCompetitorInLeg.put("distanceSinceGun-m",
                                                RoundingUtil.distanceDecimalFormatter
                                                        .format(distanceSinceGun.getMeters()));
                                    }
                                }
                                final Double distanceTraveledInMeters = legDetail == null ? null : legDetail.distanceTraveledInMeters;
                                if (distanceTraveledInMeters != null) {
                                    jsonCompetitorInLeg.put("distanceTraveled-m",
                                            RoundingUtil.distanceDecimalFormatter.format(distanceTraveledInMeters));
                                }
                                final Double distanceTraveledIncludingGateStartInMeters = legDetail == null ? null : legDetail.distanceTraveledIncludingGateStartInMeters;
                                if (distanceTraveledIncludingGateStartInMeters != null) {
                                    jsonCompetitorInLeg.put("distanceTraveledIncludingGateStart-m",
                                            RoundingUtil.distanceDecimalFormatter.format(distanceTraveledIncludingGateStartInMeters));
                                }
                                try {
                                    final int rank = legDetail == null ? 0 : legDetail.rank;
                                    jsonCompetitorInLeg.put("rank", rank == 0 ? null : rank);
                                } catch (RuntimeException re) {
                                    if (re.getCause() != null && re.getCause() instanceof NoWindException) {
                                        // well, we don't know the wind direction, so we can't compute a ranking
                                    } else {
                                        throw re;
                                    }
                                }
                                // the following expensive-to-compute metrics will be delivered only to our valued "premium" customers:
                                if (SecurityUtils.getSubject().isPermitted(SecuredDomainType.LEADERBOARD.getStringPermissionForObject(LeaderboardActions.PREMIUM_LEADERBOARD_INFORMATION, leaderboard))) {
                                    // TODO bug5899: check if for Subject a request for the same regattaName/raceName is already being computed; if so, wait for it and use it; otherwise register computation for other parallel requests to re-use; synchronization / atomicity!
                                    final Double gapToLeaderInSeconds = legDetail == null ? null : legDetail.gapToLeaderInSeconds;
                                    jsonCompetitorInLeg.put("gapToLeader-s",
                                            gapToLeaderInSeconds != null ? gapToLeaderInSeconds : 0.0);
                                    final Distance gapToLeaderDistance = trackedLegOfCompetitor
                                            .getWindwardDistanceToCompetitorFarthestAhead(timePoint,
                                                    WindPositionMode.LEG_MIDDLE, rankingInfo, cache);
                                    jsonCompetitorInLeg.put("gapToLeader-m",
                                            gapToLeaderDistance != null ? gapToLeaderDistance.getMeters() : 0.0);
                                }
                                jsonCompetitorInLeg.put("started", trackedLegOfCompetitor.hasStartedLeg(timePoint));
                                jsonCompetitorInLeg.put("finished",
                                        trackedLegOfCompetitor.hasFinishedLeg(timePoint));
                                jsonCompetitors.add(jsonCompetitorInLeg);
                            }
                        }
                    }
                    jsonLeg.put("competitors", jsonCompetitors);
                    jsonLegs.add(jsonLeg);
                    zeroBasedLegIndex++;
                }
                jsonRaceResults.put("legs", jsonLegs);
            } finally {
                course.unlockAfterRead();
            }
            return jsonRaceResults;
        } catch (NoWindException | InterruptedException | ExecutionException e1) {
            throw new RuntimeException(e1);
        }
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/competitors/live")
    public Response getCompetitorLiveRanks(@PathParam("regattaname") String regattaName,
            @PathParam("racename") String raceName, @DefaultValue("-1") @QueryParam("topN") Integer topN) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN)
                    .build();
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) + "'.").type(MediaType.TEXT_PLAIN)
                        .build();
            } else {
                final CompetitorLiveRanksRequest cacheKey = new CompetitorLiveRanksRequest(regattaName, raceName, topN);
                final JSONObject jsonLiveData = competitorLiveRanksCache.get(cacheKey);
                response = Response.ok(streamingOutput(jsonLiveData)).build();
            }
        }
        return response;
    }

    private JSONObject computeCompetitorLiveRanks(String regattaName, String raceName, Integer topN) {
        Leaderboard leaderboard = getService().getLeaderboardByName(regattaName);
        TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
        Course course = trackedRace.getRace().getCourse();
        Waypoint lastWaypoint = course.getLastWaypoint();
        TimePoint timePoint = MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis());
        final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = new LeaderboardDTOCalculationReuseCache(timePoint);
        final RankingInfo rankingInfo = trackedRace.getRankingMetric().getRankingInfo(timePoint, cache);
        JSONObject jsonLiveData = new JSONObject();
        jsonLiveData.put("name", trackedRace.getRace().getName());
        jsonLiveData.put("regatta", regattaName);
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
        final JSONArray jsonCompetitors = new JSONArray();
        final Iterable<Competitor> competitorsFromBestToWorst = trackedRace.getCompetitorsFromBestToWorst(timePoint, cache);
        final Map<Competitor, Integer> overallRankPerCompetitor = new HashMap<>();
        if (leaderboard != null) {
            final Iterable<Competitor> overallRanking = leaderboard.getCompetitorsFromBestToWorst(timePoint, cache);
            Integer overallRank = 1;
            for (Competitor competitor : overallRanking) {
                if (getSecurityService().hasCurrentUserOneOfExplicitPermissions(competitor,
                        SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                    overallRankPerCompetitor.put(competitor, overallRank++);
                }
            }
        }
        Integer rank = 1;
        for (Competitor competitor : competitorsFromBestToWorst) {
            if (getSecurityService().hasCurrentUserOneOfExplicitPermissions(competitor,
                    SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS)) {
                final JSONObject jsonCompetitorInLeg = new JSONObject();
                if (topN != null && topN > 0 && rank > topN) {
                    break;
                }
                jsonCompetitorInLeg.put("id",
                        competitor.getId() != null ? competitor.getId().toString() : null);
                jsonCompetitorInLeg.put("name", competitor.getName());
                jsonCompetitorInLeg.put("sailNumber", trackedRace.getBoatOfCompetitor(competitor).getSailID());
                jsonCompetitorInLeg.put("color",
                        competitor.getColor() != null ? competitor.getColor().getAsHtml() : null);
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
                            jsonCompetitorInLeg.put("speedOverGround-kts",
                                    roundDouble(estimatedSpeed.getKnots(), 2));
                        }
                    }
                }
                TrackedLegOfCompetitor currentLegOfCompetitor = trackedRace.getCurrentLeg(competitor,
                        timePoint);
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
                    // the following expensive-to-compute metrics will be delivered only to our valued "premium" customers:
                    if (SecurityUtils.getSubject().isPermitted(SecuredDomainType.LEADERBOARD.getStringPermissionForObject(LeaderboardActions.PREMIUM_LEADERBOARD_INFORMATION, leaderboard))) {
                        Duration gapToLeader = currentLegOfCompetitor.getGapToLeader(timePoint,
                                WindPositionMode.LEG_MIDDLE, rankingInfo, cache);
                        if (gapToLeader != null) {
                            final double gapToLeaderAsSeconds = gapToLeader.asSeconds();
                            jsonCompetitorInLeg.put("gapToLeader-s",
                                    Double.isFinite(gapToLeaderAsSeconds) ? roundDouble(gapToLeaderAsSeconds, 2) : null);
                        }
                        Distance windwardDistanceToCompetitorFarthestAhead = currentLegOfCompetitor
                                .getWindwardDistanceToCompetitorFarthestAhead(timePoint,
                                        WindPositionMode.LEG_MIDDLE, rankingInfo, cache);
                        if (windwardDistanceToCompetitorFarthestAhead != null) {
                            jsonCompetitorInLeg.put("gapToLeader-m",
                                    roundDouble(windwardDistanceToCompetitorFarthestAhead.getMeters(), 2));
                        }
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
        }
        jsonLiveData.put("competitors", jsonCompetitors);
        return jsonLiveData;
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

        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            response = getDataMiningResource().avgSpeedPerCompetitorAndLegType(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_AVERAGE_SPEED_PER_COMPETITOR_LEGTYPE)
    public Response avgSpeedPerCompetitorAndLegType(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
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
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            response = getDataMiningResource().sumDistanceTraveledPerCompetitorAndLegType(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_DISTANCE_TRAVELED_PER_COMPETITOR_LEGTYPE)
    public Response sumDistancePerCompetitorAndLegType(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
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
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            response = getDataMiningResource().sumManeuversPerCompetitor(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_MANEUVERS_PER_COMPETITOR)
    public Response sumManeuversPerCompetitor(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
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
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            response = getDataMiningResource().avgSpeedPerCompetitor(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_AVERAGE_SPEED_PER_COMPETITOR)
    public Response avgSpeedPerCompetitor(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
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
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            response = getDataMiningResource().sumDistanceTraveledPerCompetitor(regattaName);
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaname}/races/{racename}/datamining/" + SailingPredefinedQueries.QUERY_DISTANCE_TRAVELED_PER_COMPETITOR)
    public Response sumDistancePerCompetitor(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
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
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            final JSONArray jsonResponse = new JSONArray();
            final Series series = getSeriesUsingLastAsDefault(regatta, toSeries);
            if (series == null) {
                response = getBadSeriesErrorResponse(regattaName, toSeries);
            } else {
                final String raceNamePrefix = prefix == null ? "R" : prefix;
                int oneBasedNumberOfLast = Util.size(series.getRaceColumns());
                for (int i = 1; i <= (numberOfRaces==null?1:numberOfRaces); i++) {
                    final int oneBasedNumberOfNext = findNextFreeRaceName(series, raceNamePrefix, oneBasedNumberOfLast);
                    final RaceColumnInSeries raceColumn = addRaceColumn(regatta, series.getName(), getRaceName(raceNamePrefix, oneBasedNumberOfNext));
                    final JSONObject raceColumnDataAsJson = new JSONObject();
                    raceColumnDataAsJson.put("seriesname", raceColumn.getSeries().getName());
                    raceColumnDataAsJson.put("racename", raceColumn.getName());
                    jsonResponse.add(raceColumnDataAsJson);
                    oneBasedNumberOfLast = oneBasedNumberOfNext;
                }
                response = Response.ok(streamingOutput(jsonResponse)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @POST
    @Path("{regattaname}/removeracecolumn")
    public Response removeRaceColumns(@PathParam("regattaname") String regattaName,
            @QueryParam("racecolumn") String raceColumnName) {
        final Response response;
        Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            SecurityUtils.getSubject().checkPermission(regatta.getIdentifier().getStringPermission(DefaultActions.UPDATE));
            getSecurityService().checkCurrentUserReadPermission(regatta);
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

    private RaceColumnInSeries addRaceColumn(Regatta regatta, String seriesName, String columnName) {
        SecurityUtils.getSubject().checkPermission(regatta.getIdentifier().getStringPermission(DefaultActions.UPDATE));
        return getService().apply(new AddColumnToSeries(new RegattaName(regatta.getName()), seriesName, columnName));
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

    @POST
    @Path("/updateOrCreateSeries")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json;charset=UTF-8")
    public Response updateOrCreateSeries(String json) throws ParseException, JsonDeserializationException {
        Object requestBody = JSONValue.parseWithException(json);
        JSONObject requestObject = Helpers.toJSONObjectSafe(requestBody);
        String regattaName = (String) requestObject.get("regattaName");
        Regatta regatta = getService().getRegattaByName(regattaName);
        if (regatta != null) {
            SecurityUtils.getSubject().checkPermission(regatta.getIdentifier().getStringPermission(DefaultActions.UPDATE));
            final String seriesName = (String) requestObject.get("seriesName");
            final String seriesNameNew = (String) requestObject.get("seriesNameNew");
            final boolean isMedal = (boolean) requestObject.get("isMedal");
            final boolean isFleetsCanRunInParallel = (boolean) requestObject.get("isFleetsCanRunInParallel");
            final boolean startsWithZeroScore = (boolean) requestObject.get("startsWithZeroScore");
            final boolean firstColumnIsNonDiscardableCarryForward = (boolean) requestObject
                    .get("firstColumnIsNonDiscardableCarryForward");
            final boolean hasSplitFleetContiguousScoring = (boolean) requestObject.get("hasSplitFleetContiguousScoring");
            final boolean hasCrossFleetMergedRanking = (boolean) requestObject.get("hasCrossFleetMergedRanking");
            final boolean oneAlwaysStaysOne = (boolean) requestObject.get("oneAlwaysStaysOne");
            Integer maximumNumberOfDiscards = null;
            if (requestObject.containsKey("maximumNumberOfDiscards")) {
                maximumNumberOfDiscards = ((Long) requestObject.get("maximumNumberOfDiscards")).intValue();
            }
            int[] resultDiscardingThresholds = null;
            if (requestObject.containsKey("resultDiscardingThresholds")) {
                JSONArray resultDiscardingThresholdsRaw = (JSONArray) requestObject.get("resultDiscardingThresholds");
                resultDiscardingThresholds = new int[resultDiscardingThresholdsRaw.size()];
                for (int i = 0; i < resultDiscardingThresholdsRaw.size(); i++) {
                    resultDiscardingThresholds[i] = ((Long) resultDiscardingThresholdsRaw.get(i)).intValue();
                }
            }
            JSONArray fleetsRaw = (JSONArray) requestObject.get("fleets");
            List<FleetDTO> fleets = new ArrayList<>();
            for (Object fleetRaw : fleetsRaw) {
                JSONObject fleet = Helpers.toJSONObjectSafe(fleetRaw);
                String fleetName = (String) fleet.get("fleetName");
                int orderNo = ((Long) fleet.get("orderNo")).intValue();
                String htmlColor = (String) fleet.get("htmlColor");
                fleets.add(new FleetDTO(fleetName, orderNo, new RGBColor(htmlColor)));
            }
            getService().apply(new UpdateSeries(regatta.getRegattaIdentifier(), seriesName, seriesNameNew, isMedal,
                    isFleetsCanRunInParallel, resultDiscardingThresholds, startsWithZeroScore,
                    firstColumnIsNonDiscardableCarryForward, hasSplitFleetContiguousScoring, hasCrossFleetMergedRanking, maximumNumberOfDiscards,
                    oneAlwaysStaysOne, fleets));
        } else {
            throw new IllegalStateException("RegattaName could not be resolved to regatta " + regattaName);
        }
        return Response.ok().header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
    }

    /**
     * Outputs device bindings in the scope of the regatta identified by {@code regattaName}, optionally limited to a single
     * device whose ID can be provided by the {@code optionalDeviceUuid} parameter. Generally, callers need to have the
     * {@link DefaultActions#UPDATE} permission for the regatta, but exceptionally, if a caller presents the correct
     * {@code regattaSecret}, querying the tracking status for a single {@code optionalDeviceUuid} is permitted.
     */
    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{regattaName}/tracking_devices")
    public Response getTrackingStatus(@PathParam("regattaName") String regattaName, @QueryParam("deviceUuid") String optionalDeviceUuid,
            @QueryParam("secret") String regattaSecret) {
        final Regatta regatta = getService().getRegattaByName(regattaName);
        if (regatta != null) {
            // Deeper insights to tracking data is only available to users who can administer a regatta
            boolean skip = getService().skipChecksDueToCorrectSecret(regattaName, regattaSecret);
            if (!skip || !Util.hasLength(optionalDeviceUuid)) {
                SecurityUtils.getSubject().checkPermission(regatta.getIdentifier().getStringPermission(DefaultActions.UPDATE));
            }
            final TrackingDeviceStatusSerializer serializer = new TrackingDeviceStatusSerializer(
                    new DeviceIdentifierJsonSerializer(
                            getServiceFinderFactory().createServiceFinder(DeviceIdentifierJsonHandler.class)));
            final RegattaLogDeviceMappingFinder<WithID> regattaLogDeviceMappingFinder = new RegattaLogDeviceMappingFinder<WithID>(
                    regatta.getRegattaLog());
            final Map<WithID, List<DeviceMappingWithRegattaLogEvent<WithID>>> foundMappings = regattaLogDeviceMappingFinder.analyze();
            final JSONObject result = new JSONObject();
            foundMappings.forEach((item, mappings) -> {
                Map<DeviceIdentifier, DeviceMappingWithRegattaLogEvent<WithID>> mappingByDeviceID = new HashMap<>();
                for (DeviceMappingWithRegattaLogEvent<WithID> mapping : mappings) {
                    mappingByDeviceID.compute(mapping.getDevice(), (di, existingMapping) -> {
                        if (existingMapping == null) {
                            return mapping;
                        }
                        TimeRange existingTimeRange = existingMapping.getTimeRange();
                        TimeRange newTimeRange = mapping.getTimeRange();
                        if ((!existingTimeRange.hasOpenEnd() && newTimeRange.hasOpenEnd())
                                || newTimeRange.endsAfter(existingTimeRange)
                                || (newTimeRange.to().compareTo(existingTimeRange.to()) == 0
                                && newTimeRange.startsAfter(existingTimeRange))) {
                            return mapping;
                        }
                        return existingMapping;
                    });
                }
                final JSONArray deviceStatusesOfTrackedItem = new JSONArray();
                for (DeviceMappingWithRegattaLogEvent<WithID> deviceMapping : mappingByDeviceID.values()) {
                    // if a device ID has been specified, output only content relating to that device ID:
                    if (!Util.hasLength(optionalDeviceUuid) || Util.equalsWithNull(deviceMapping.getDevice().getStringRepresentation(), optionalDeviceUuid)) {
                        JSONObject serializedDeviceStatus = serializer.serialize(
                                TrackingDeviceStatus.calculateDeviceStatus(deviceMapping.getDevice(), getService()));
                        deviceStatusesOfTrackedItem.add(serializedDeviceStatus);
                        TimeRange mappedTimeRange = deviceMapping.getTimeRange();
                        serializedDeviceStatus.put("mappedFrom", mappedTimeRange.from().asMillis());
                        serializedDeviceStatus.put("mappedTo",
                                mappedTimeRange.hasOpenEnd() ? null : mappedTimeRange.to().asMillis());
                    }
                }
                final JSONObject itemObject = new JSONObject();
                itemObject.put("deviceStatuses", deviceStatusesOfTrackedItem);
                if (item instanceof Competitor) {
                    itemObject.put("competitorId", item.getId().toString());
                    ((JSONArray) result.computeIfAbsent("competitors", k -> new JSONArray())).add(itemObject);
                } else if (item instanceof Boat) {
                    itemObject.put("boatId", item.getId().toString());
                    ((JSONArray) result.computeIfAbsent("boats", k -> new JSONArray())).add(itemObject);
                } else if (item instanceof Mark) {
                    itemObject.put("markId", item.getId().toString());
                    ((JSONArray) result.computeIfAbsent("marks", k -> new JSONArray())).add(itemObject);
                } else {
                    logger.log(Level.WARNING, "Unexpected tracked item found while calculating the tracker status. ID: "
                            + item.getId() + "; type: " + item.getClass().getName());
                }
            });
            return Response.ok(streamingOutput(result)).build();
        } else {
            throw new IllegalStateException("Regatta named " + regattaName + " could not be resolved");
        }
    }

    @POST
    @Path("{regattaname}/competitors/{competitorid}/updateToTHandicap")
    @Produces("application/json;charset=UTF-8")
    public Response updateCompetitorToTHandicap(@PathParam("regattaname") String regattaName,
            @PathParam("competitorid") String competitorId, @QueryParam("timeOnTimeFactor") Double timeOnTimeFactor)
            throws IllegalStateException, ParseException {
        final Regatta regatta = getService().getRegattaByName(regattaName);
        if (regatta != null) {
            final SecurityService securityService = getSecurityService();
            securityService.checkCurrentUserUpdatePermission(regatta);
            Serializable potentialUUID = UUIDHelper.tryUuidConversion(competitorId);
            final Competitor competitor = getService().getCompetitorAndBoatStore()
                    .getExistingCompetitorById(potentialUUID);
            if (competitor != null) {
                if (timeOnTimeFactor != null) {
                    final User author = securityService.getCurrentUser();
                    final LogEventAuthorImpl logEventAuthor = new LogEventAuthorImpl(author.getName(),
                            /* priority */ 0);
                    final TimePoint now = MillisecondsTimePoint.now();
                    final RegattaLogEvent event = new RegattaLogSetCompetitorTimeOnTimeFactorEventImpl(now, now,
                            logEventAuthor, UUID.randomUUID(), competitor, timeOnTimeFactor);
                    final RegattaLog regattaLog = regatta.getRegattaLog();
                    regattaLog.add(event);
                } else {
                    throw new IllegalStateException("Missing required parameter: timeOnTimeFactor");
                }
            } else {
                return getBadCompetitorIdResponse(competitorId);
            }
        } else {
            throw new IllegalStateException("RegattaName could not be resolved to regatta " + regattaName);
        }
        return Response.ok().header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
    }

    @POST
    @Path("{regattaname}/competitors/{competitorId}/updateToDHandicap")
    @Produces("application/json;charset=UTF-8")
    public Response updateCompetitorToDHandicap(@PathParam("regattaname") String regattaName,
            @PathParam("competitorId") String competitorId,
            @QueryParam("timeOnDistanceAllowancePerNauticalMile") Long timeOnDistanceAllowancePerNauticalMile)
            throws IllegalStateException, ParseException {
        final Regatta regatta = getService().getRegattaByName(regattaName);
        final SecurityService securityService = getSecurityService();
        securityService.checkCurrentUserUpdatePermission(regatta);
        if (regatta != null) {
            Serializable potentialUUID = UUIDHelper.tryUuidConversion(competitorId);
            final Competitor competitor = getService().getCompetitorAndBoatStore()
                    .getExistingCompetitorById(potentialUUID);
            if (competitor != null) {
                if (timeOnDistanceAllowancePerNauticalMile != null) {
                    final Duration durationTimeOnDistanceAllowancePerNauticalMile = new MillisecondsDurationImpl(
                            timeOnDistanceAllowancePerNauticalMile);
                    final User author = securityService.getCurrentUser();
                    final LogEventAuthorImpl logEventAuthor = new LogEventAuthorImpl(author.getName(),
                            /* priority */ 0);
                    final TimePoint now = MillisecondsTimePoint.now();
                    final RegattaLogEvent event = new RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEventImpl(
                            now, now, logEventAuthor, UUID.randomUUID(), competitor,
                            durationTimeOnDistanceAllowancePerNauticalMile);
                    final RegattaLog regattaLog = regatta.getRegattaLog();
                    regattaLog.add(event);
                } else {
                    throw new IllegalStateException(
                            "Missing required parameter: timeOnDistanceAllowancePerNauticalMile");
                }
            } else {
                return getBadCompetitorIdResponse(competitorId);
            }
        } else {
            throw new IllegalStateException("RegattaName could not be resolved to regatta " + regattaName);
        }
        return Response.ok().header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
    }
}
