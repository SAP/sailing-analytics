package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.json.simple.JSONArray;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.polars.windestimation.ManeuverBasedWindEstimationTrack;
import com.sap.sailing.polars.windestimation.ManeuverBasedWindEstimationTrackImpl;
import com.sap.sailing.polars.windestimation.ManeuverClassification;
import com.sap.sailing.polars.windestimation.ScalableBearingAndScalableDouble;
import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.WindJsonSerializer;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.scalablevalue.ScalableDouble;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;
import com.sap.sse.util.kmeans.Cluster;

/**
 * Right now this service is only used for quick debugging and testing of the polar api. Some services use plain text
 * responses. JSON or other common formats should be used if the series are to be consumed in production.
 * 
 * @author Frederik Petersen / Axel Uhl (maneuver-based wind estimation)
 *
 */
@Path("/v1/polars")
public class PolarResource extends AbstractSailingServerResource {

    @GET
    @Produces("text/plain;charset=UTF-8")
    @Path("{boatClassName}")
    public Response getSpeed(@PathParam("boatClassName") String boatClassName, @QueryParam("angle") double angle,
            @QueryParam("windspeedInKnots") double windSpeed) {
        BoatClass boatClass = getService().getDomainObjectFactory().getBaseDomainFactory()
                .getOrCreateBoatClass(boatClassName);
        SpeedWithConfidence<Void> speedWithConfidence;
        ResponseBuilder responseBuilder;
        try {
            speedWithConfidence = getService().getPolarDataService().getSpeed(boatClass, new KnotSpeedImpl(windSpeed),
                    new DegreeBearingImpl(angle));
            String resultString = "Speed: " + speedWithConfidence.getObject().getKnots() + "kn; Confidence: "
                    + speedWithConfidence.getConfidence();
            responseBuilder = Response.ok(resultString, MediaType.TEXT_PLAIN);
        } catch (NotEnoughDataHasBeenAddedException e) {
            responseBuilder = Response.noContent();
        }

        return responseBuilder.build();
    }
    
    @GET
    @Produces("text/plain;charset=UTF-8")
    @Path("functions")
    public Response getFunctions() {
        PolarDataService polarDataService = getService().getPolarDataService();
        Set<BoatClass> boatClasses = polarDataService.getAllBoatClassesWithPolarSheetsAvailable();
        ResponseBuilder responseBuilder;
        StringBuilder stringBuilder = new StringBuilder();
        for (BoatClass boatClass : boatClasses) {
            for (LegType legType : new LegType[] { LegType.UPWIND, LegType.DOWNWIND }) {
               try {
                   PolynomialFunction speedFunction = polarDataService.getSpeedRegressionFunction(boatClass, legType);
                   stringBuilder.append("Speed: " + boatClass + " " + legType + ": " + speedFunction.toString() + "\n");
                   PolynomialFunction angleFunction = polarDataService.getAngleRegressionFunction(boatClass, legType);
                   stringBuilder.append("Angle: " + boatClass + " " + legType + ": " + angleFunction.toString() + "\n");
               } catch (NotEnoughDataHasBeenAddedException e) {
                    stringBuilder.append("No data for " + boatClass + " " + legType + "\n");
               }
                
            }
            for (double trueWindAngle = -177.5; trueWindAngle < 180; trueWindAngle = trueWindAngle + 5) {
                try {
                    PolynomialFunction speedForTWAFunction = polarDataService.getSpeedRegressionFunction(boatClass, trueWindAngle);
                    stringBuilder.append("Speed for TWA: " + boatClass + " " + trueWindAngle + ": " + speedForTWAFunction.toString() + "\n");
                } catch (NotEnoughDataHasBeenAddedException e) {
                    stringBuilder.append("No data for " + boatClass + " " + trueWindAngle + "\n");
                }
            }
            stringBuilder.append("\n\n");
        }

        responseBuilder = Response.ok(stringBuilder.toString(), MediaType.TEXT_PLAIN);
        return responseBuilder.build();
    }
    
    @GET
    @Produces("text/plain;charset=UTF-8")
    @Path("average/{boatClassName}")
    public Response getAverageSpeedAndBearing(@PathParam("boatClassName") String boatClassName,
            @QueryParam("windspeedInKnots") double wSpeed, @QueryParam("legtype") LegType legType, @QueryParam("tack") Tack tack) {
        BoatClass boatClass = getService().getDomainObjectFactory().getBaseDomainFactory()
                .getOrCreateBoatClass(boatClassName);
        ResponseBuilder responseBuilder;
        Speed windSpeed = new KnotSpeedImpl(wSpeed);
        try {
            PolarDataService service = getService().getPolarDataService();
            SpeedWithBearingWithConfidence<Void> speedWithBearing = service.getAverageSpeedWithTrueWindAngle(boatClass,
                    windSpeed, legType, tack);
            String resultString = "Speed: " + speedWithBearing.getObject().getKnots() + "kn; Angle: "
                    + speedWithBearing.getObject().getBearing().getDegrees() + "°; Confidence: "
                    + speedWithBearing.getConfidence();
            responseBuilder = Response.ok(resultString, MediaType.TEXT_PLAIN);
        } catch (NotEnoughDataHasBeenAddedException e) {
            responseBuilder = Response.noContent();
        }
        return responseBuilder.build();
    }

    private Response getBadRegattaErrorResponse(String regattaName) {
        return  Response.status(Status.NOT_FOUND).entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadRaceErrorResponse(String regattaName, String raceName) {
        return Response.status(Status.NOT_FOUND).entity("Could not find a race with name '" + StringEscapeUtils.escapeHtml(raceName) +
                "' in regatta '" + StringEscapeUtils.escapeHtml(regattaName) + "'.").type(MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("windestimation/{regattaname}/races/{racename}")
    public Response getCompetitorPositions(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName) throws NotEnoughDataHasBeenAddedException {
        Regatta regatta = findRegattaByName(regattaName);
        Response response;
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {     
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                JSONArray resultAsJson = new JSONArray();
                WindJsonSerializer serializer = new WindJsonSerializer(new PositionJsonSerializer());
                PolarDataService service = getService().getPolarDataService();
                final ManeuverBasedWindEstimationTrack maneuverBasedWindEstimationTrackImpl = new ManeuverBasedWindEstimationTrackImpl(
                        service, trackedRace, /* millisecondsOverWhichToAverage */ 30000, /* waitForLatest */ false);
                maneuverBasedWindEstimationTrackImpl.initialize();
                maneuverBasedWindEstimationTrackImpl.lockForRead();
                try {
                    for (Wind wind : maneuverBasedWindEstimationTrackImpl.getFixes()) {
                        resultAsJson.add(serializer.serialize(wind));
                    }
                } finally {
                    maneuverBasedWindEstimationTrackImpl.unlockAfterRead();
                }
                response = Response.ok(streamingOutput(resultAsJson)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            }
        }
        return response;
    }

    @GET
    @Produces("text/plain;charset=UTF-8")
    @Path("maneuvers/{regattaname}/races/{racename}")
    public Response getManeuvers(@PathParam("regattaname") String regattaName, @PathParam("racename") String raceName,
            @QueryParam("cluster") String cluster)
            throws NotEnoughDataHasBeenAddedException {
        Regatta regatta = findRegattaByName(regattaName);
        Response response;
        if (regatta == null) {
            response = getBadRegattaErrorResponse(regattaName);
        } else {
            getSecurityService().checkCurrentUserReadPermission(regatta);
            RaceDefinition race = findRaceByName(regatta, raceName);
            if (race == null) {
                response = getBadRaceErrorResponse(regattaName, raceName);
            } else {     
                TrackedRace trackedRace = findTrackedRace(regattaName, raceName);
                PolarDataService service = getService().getPolarDataService();
                ManeuverBasedWindEstimationTrackImpl maneuverBasedWindEstimationTrackImpl = new ManeuverBasedWindEstimationTrackImpl(
                        service, trackedRace, /* millisecondsOverWhichToAverage */ 30000, /* waitForLatest */ false);
                final Stream<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> clusters;
                if ("tack".equals(cluster)) {
                    clusters = maneuverBasedWindEstimationTrackImpl.getTackClusters().stream();
                } else if ("jibe".equals(cluster)) {
                    clusters = maneuverBasedWindEstimationTrackImpl.getJibeClusters().stream();
                } else if (cluster != null && cluster.matches("[0-9][0-9]*")) {
                    clusters = Stream.of(new ArrayList<>(maneuverBasedWindEstimationTrackImpl.getClusters()).get(Integer.valueOf(cluster)));
                } else {
                    clusters = maneuverBasedWindEstimationTrackImpl.getClusters().stream();
                }
                response = Response.ok(maneuverBasedWindEstimationTrackImpl.getStringRepresentation(clusters),
                        MediaType.TEXT_PLAIN).build();
            }
        }
        return response;
    }
}
