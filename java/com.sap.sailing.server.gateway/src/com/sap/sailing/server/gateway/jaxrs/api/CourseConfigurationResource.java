package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.coursetemplate.ControlPointWithMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.CourseConfiguration;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.FreestyleMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationRequestAnnotation;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationResponseAnnotation;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationVisitor;
import com.sap.sailing.domain.coursetemplate.MarkPairWithConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkPropertiesBasedMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkTemplateBasedMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.RegattaMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.WaypointWithMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.impl.CourseConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.FreestyleMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkPairWithConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkPropertiesBasedMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkTemplateBasedMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.RegattaMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.WaypointWithMarkConfigurationImpl;
import com.sap.sailing.server.gateway.deserialization.impl.CourseConfigurationBuilder;
import com.sap.sailing.server.gateway.deserialization.impl.CourseConfigurationJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.DeviceIdentifierJsonDeserializer;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sailing.server.gateway.serialization.impl.CourseConfigurationJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.PlaceHolderDeviceIdentifierJsonHandler;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.shared.json.JsonSerializer;
import com.sun.jersey.api.client.ClientResponse.Status;

@Path("/v1/courseconfiguration")
public class CourseConfigurationResource extends AbstractSailingServerResource {
    private static final Logger log = Logger.getLogger(CourseConfigurationResource.class.getName());
    
    private JsonSerializer<CourseConfiguration<MarkConfigurationResponseAnnotation>> courseConfigurationJsonSerializer;
    private final BiFunction<Regatta, DeviceIdentifier, GPSFix> positionResolver;
    private DeviceIdentifierJsonDeserializer deviceIdentifierDeserializer;
    private TypeBasedServiceFinder<DeviceIdentifierJsonHandler> deviceJsonServiceFinder;

    public static final String FIELD_TAGS = "tags";

    public CourseConfigurationResource() {
        positionResolver = (regatta, deviceIdentifier) -> {
            GPSFix lastPosition = null;
            try {
                final Map<DeviceIdentifier, Timed> lastFix = getService().getSensorFixStore().getFixLastReceived(Collections.singleton(deviceIdentifier));
                final Timed t = lastFix.get(deviceIdentifier);
                if (t instanceof GPSFix) {
                    lastPosition = ((GPSFix) t);
                }
            } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                log.log(Level.WARNING, "Could not load associated fix for device " + deviceIdentifier, e);
            }
            return lastPosition;
        };
    }

    private JsonDeserializer<CourseConfiguration<MarkConfigurationRequestAnnotation>> getCourseConfigurationDeserializer(final Regatta regatta) {
        return new CourseConfigurationJsonDeserializer(this.getSharedSailingData(), getDeviceIdentifierDeserializer(), regatta);
    }

    private Response getBadRegattaErrorResponse(String regattaName) {
        return Response.status(Status.NOT_FOUND)
                .entity("Could not find a regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadRaceErrorResponse(String regattaName, String raceColumn, String fleet) {
        return Response.status(Status.NOT_FOUND)
                .entity("Could not find a race with race column '" + StringEscapeUtils.escapeHtml(raceColumn)
                        + "' and fleet '" + fleet
                        + "' for regatta with name '" + StringEscapeUtils.escapeHtml(regattaName) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadCourseTemplateErrorResponse(String courseTemplateId) {
        return Response.status(Status.NOT_FOUND).entity(
                "Could not find a CourseTemplate with ID '" + StringEscapeUtils.escapeHtml(courseTemplateId) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadCourseConfigurationValidationErrorResponse(String errorText) {
        return Response.status(Status.BAD_REQUEST).entity(StringEscapeUtils.escapeHtml(errorText) + ".")
                .type(MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("getFromCourse/{regattaName}/{raceColumn}/{fleet}")
    public Response createCourseConfigurationFromCourse(@PathParam("regattaName") String regattaName,
            @PathParam("raceColumn") String raceColumn, @PathParam("fleet") String fleet,
            @QueryParam("tags") List<String> tags) throws Exception {
        if (regattaName == null || raceColumn == null || fleet == null) {
            return getBadCourseConfigurationValidationErrorResponse(
                    "Course configuration is required to have a regatta name and a race name");
        }
        final Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            return getBadRegattaErrorResponse(regattaName);
        }
        getSecurityService().checkCurrentUserReadPermission(regatta);
        final RaceColumn raceColumnByName = findRaceColumnByName(regatta, raceColumn);
        final Fleet fleetByName = findFleetByName(raceColumnByName, fleet);
        if (raceColumnByName == null || fleetByName == null) {
            return getBadRaceErrorResponse(regattaName, raceColumn, fleet);
        }
        final RaceDefinition raceDefinition = raceColumnByName.getRaceDefinition(fleetByName);
        final CourseBase courseBase;
        if (raceDefinition != null) {
            courseBase = raceDefinition.getCourse();
        } else {
            final ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(getService(), raceColumnByName.getRaceLog(fleetByName));
            courseBase = raceState.getCourseDesign();
        }
        // courseBase may be null in case, no course is defined for the race yet.
        // createCourseConfigurationFromCourse returns a course configuration with an empty sequence in this case.
        // Any mark already defined in the regatta will be added to the included mark configurations as an initial
        // set of marks to be used while defining a course for the regatta.
        // An additional call to get the marks defined in the regatta isn't necessary with the described behavior of this API.
        final CourseConfiguration<MarkConfigurationResponseAnnotation> courseConfiguration = getService().getCourseAndMarkConfigurationFactory()
                .createCourseConfigurationFromRegatta(courseBase, regatta, raceColumnByName.getTrackedRace(fleetByName),
                        tags);
        final JSONObject jsonResult = getCourseConfigurationJsonSerializer().serialize(courseConfiguration);
        return Response.ok(streamingOutput(jsonResult)).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("getFromCourseTemplate/{courseTemplateId}")
    public Response createCourseConfigurationFromCourseTemplate(@PathParam("courseTemplateId") String courseTemplateId,
            @QueryParam("regattaName") String regattaName,
            @QueryParam("tags") List<String> tags,
            @QueryParam("numberOfLaps") Integer optionalNumberOfLaps) {
        final CourseTemplate courseTemplate = this.getSharedSailingData()
                .getCourseTemplateById(UUID.fromString(courseTemplateId));
        if (courseTemplate == null) {
            return getBadCourseTemplateErrorResponse(courseTemplateId);
        }
        Regatta regatta = null;
        if (regattaName != null) {
            regatta = findRegattaByName(regattaName);
            if (regatta == null) {
                return getBadRegattaErrorResponse(regattaName);
            }
        }
        final CourseConfiguration<MarkConfigurationResponseAnnotation> courseConfiguration = getService().getCourseAndMarkConfigurationFactory()
                .createCourseConfigurationFromTemplate(courseTemplate, regatta, tags, optionalNumberOfLaps);
        return Response.ok(streamingOutput(getCourseConfigurationJsonSerializer().serialize(courseConfiguration))).build();

    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("createCourseTemplate")
    public Response createCourseTemplate(@QueryParam("regattaName") String regattaName,
            @QueryParam("markPropertiesGroupOwnership") String markPropertiesGroupOwnership,
            String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return getBadCourseConfigurationValidationErrorResponse(
                    "Course configuration is required to be given as json object");
        }
        Regatta regatta = null;
        if (regattaName != null) {
            regatta = findRegattaByName(regattaName);
            if (regatta == null) {
                return getBadRegattaErrorResponse(regattaName);
            }
        }
        final Object parsedObject = new JSONParser().parse(json);
        if (parsedObject == null || !(parsedObject instanceof JSONObject)) {
            getBadCourseConfigurationValidationErrorResponse(
                    "Course configuration is required to be given as json object");
        }
        final CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfiguration = getCourseConfigurationDeserializer(regatta)
                .deserialize((JSONObject) parsedObject);
        final Iterable<String> tags = Arrays
                .asList(ArrayUtils.nullToEmpty((String[]) ((JSONObject) parsedObject).get(FIELD_TAGS)));
        final Optional<UserGroup> optionalUserGroupForNonDefaultMarkPropertiesOwnership = getOptionalGroupOwnership(
                markPropertiesGroupOwnership);
        final CourseConfiguration<MarkConfigurationResponseAnnotation> courseTemplate = annotateWithLastKnownPositionInformation(
                regatta, getService().getCourseAndMarkConfigurationFactory()
                    .createCourseTemplateAndUpdatedConfiguration(courseConfiguration, tags,
                        optionalUserGroupForNonDefaultMarkPropertiesOwnership));
        return Response
                .ok(streamingOutput(getCourseConfigurationJsonSerializer().serialize(courseTemplate))).build();
    }

    private CourseConfiguration<MarkConfigurationResponseAnnotation> annotateWithLastKnownPositionInformation(
            Regatta regatta, CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfiguration) {
        final Map<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigMap = new HashMap<>();
        for (final MarkConfiguration<MarkConfigurationRequestAnnotation> sourceMark : courseConfiguration.getAllMarks()) {
            annotateWithLastKnownPositionInformation(regatta, sourceMark, markConfigMap);
        }
        final Map<MarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> targetAssociatedRoles = new HashMap<>();
        for (final Entry<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkRole> e : courseConfiguration.getAssociatedRoles().entrySet()) {
            targetAssociatedRoles.put(markConfigMap.get(e.getKey()), e.getValue());
        }
        final List<WaypointWithMarkConfiguration<MarkConfigurationResponseAnnotation>> targetWaypoints = new ArrayList<>();
        for (final WaypointWithMarkConfiguration<MarkConfigurationRequestAnnotation> sourceWaypoint : courseConfiguration.getWaypoints()) {
            targetWaypoints.add(new WaypointWithMarkConfigurationImpl<MarkConfigurationResponseAnnotation>(
                    annotateWithLastKnownPositionInformation(sourceWaypoint.getControlPoint(), regatta, markConfigMap), sourceWaypoint.getPassingInstruction()));
        }
        return new CourseConfigurationImpl<MarkConfigurationResponseAnnotation>(
                courseConfiguration.getOptionalCourseTemplate(),
                new HashSet<>(markConfigMap.values()), targetAssociatedRoles, targetWaypoints,
                courseConfiguration.getRepeatablePart(), courseConfiguration.getNumberOfLaps(),
                courseConfiguration.getName(), courseConfiguration.getShortName(), courseConfiguration.getOptionalImageURL());
    }

    private ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> annotateWithLastKnownPositionInformation(
            ControlPointWithMarkConfiguration<MarkConfigurationRequestAnnotation> controlPoint, Regatta regatta,
            Map<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigMap) {
        final ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> result;
        if (controlPoint instanceof MarkPairWithConfiguration) {
            final MarkPairWithConfiguration<MarkConfigurationRequestAnnotation> sourceMarkPair = (MarkPairWithConfiguration<MarkConfigurationRequestAnnotation>) controlPoint;
            result = new MarkPairWithConfigurationImpl<MarkConfigurationResponseAnnotation>(sourceMarkPair.getName(),
                    annotateWithLastKnownPositionInformation(regatta, sourceMarkPair.getLeft(), markConfigMap),
                    annotateWithLastKnownPositionInformation(regatta, sourceMarkPair.getRight(), markConfigMap),
                    sourceMarkPair.getShortName());
        } else if (controlPoint instanceof MarkConfiguration) {
            result = annotateWithLastKnownPositionInformation(regatta,
                    ((MarkConfiguration<MarkConfigurationRequestAnnotation>) controlPoint), markConfigMap);
        } else {
            throw new IllegalStateException("Unknown ControlPointWithMarkConfiguration subclass: "+controlPoint.getClass().getName());
        }
        return result;
    }

    /**
     * @param markConfigMap
     *            used for look-up of {@code sourceMark}; if found, the value is returned; otherwise, a new value is
     *            computed, entered into {@code markConfigMap} and the value is returned.
     */
    private MarkConfiguration<MarkConfigurationResponseAnnotation> annotateWithLastKnownPositionInformation(
            Regatta regatta, MarkConfiguration<MarkConfigurationRequestAnnotation> sourceMark,
            Map<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigMap) {
        return markConfigMap.computeIfAbsent(sourceMark,
                sm->sm.accept(new MarkConfigurationVisitor<MarkConfiguration<MarkConfigurationResponseAnnotation>, MarkConfigurationRequestAnnotation>() {
            @Override
            public MarkConfiguration<MarkConfigurationResponseAnnotation> visit(
                    FreestyleMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                return new FreestyleMarkConfigurationImpl<MarkConfigurationResponseAnnotation>(markConfiguration.getOptionalMarkTemplate(),
                        markConfiguration.getOptionalMarkProperties(), markConfiguration.getFreestyleProperties(),
                        getLastKnownPositionInformation(markConfiguration.getAnnotationInfo(),
                            /* regatta mark */ null, positionResolver, regatta));
            }

            @Override
            public MarkConfiguration<MarkConfigurationResponseAnnotation> visit(
                    MarkPropertiesBasedMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                        return new MarkPropertiesBasedMarkConfigurationImpl<MarkConfigurationResponseAnnotation>(
                                markConfiguration.getOptionalMarkProperties(),
                                markConfiguration.getOptionalMarkTemplate(),
                                getLastKnownPositionInformation(markConfiguration.getAnnotationInfo(), /* regattaMark */ null, positionResolver, regatta));
                    }

            @Override
            public MarkConfiguration<MarkConfigurationResponseAnnotation> visit(
                    MarkTemplateBasedMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                return new MarkTemplateBasedMarkConfigurationImpl<MarkConfigurationResponseAnnotation>(
                        markConfiguration.getOptionalMarkTemplate(),
                        getLastKnownPositionInformation(markConfiguration.getAnnotationInfo(), /* regattaMark */ null, positionResolver, regatta));
            }

            @Override
            public MarkConfiguration<MarkConfigurationResponseAnnotation> visit(
                    RegattaMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                return new RegattaMarkConfigurationImpl<MarkConfigurationResponseAnnotation>(
                        markConfiguration.getMark(), getLastKnownPositionInformation(markConfiguration.getAnnotationInfo(),
                                markConfiguration.getMark(), positionResolver, regatta), 
                        markConfiguration.getOptionalMarkTemplate(), markConfiguration.getOptionalMarkProperties());
            }
        }));
    }

    private MarkConfigurationResponseAnnotation getLastKnownPositionInformation(MarkConfigurationRequestAnnotation positioningAnnotation,
            Mark regattaMark, BiFunction<Regatta, DeviceIdentifier, GPSFix> positionResolver, Regatta regatta) {
        final MarkConfigurationResponseAnnotation result;
        final Function<DeviceIdentifier, GPSFix> positionResolverFromDeviceIdentifier = deviceIdentifier->positionResolver.apply(regatta, deviceIdentifier);
        if (regattaMark == null) {
            if (positioningAnnotation != null) {
                result = CourseConfigurationBuilder.getPositioningIfAvailable(positioningAnnotation.getOptionalPositioning(),
                        positionResolverFromDeviceIdentifier);
            } else {
                result = null;
            }
        } else {
            result = CourseConfigurationBuilder.getPositioningIfAvailable(regatta, /* optionalRace */ null, regattaMark,
                    positionResolverFromDeviceIdentifier);
        }
        return result;
    }

    private Optional<UserGroup> getOptionalGroupOwnership(String optionalGroupName) {
        final Optional<UserGroup> optionalUserGroupForNonDefaultMarkPropertiesOwnership;
        if (optionalGroupName != null) {
            optionalUserGroupForNonDefaultMarkPropertiesOwnership = Optional.of(
                    getService().getSecurityService().getUserGroupByName(optionalGroupName));
        } else {
            optionalUserGroupForNonDefaultMarkPropertiesOwnership = Optional.empty();
        }
        return optionalUserGroupForNonDefaultMarkPropertiesOwnership;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("createCourse/{regattaName}/{raceColumn}/{fleet}")
    public Response createCourse(@PathParam("regattaName") String regattaName,
            @QueryParam("markPropertiesGroupOwnership") String markPropertiesGroupOwnership,
            @PathParam("raceColumn") String raceColumn, @PathParam("fleet") String fleet,
            String jsonCourseConfiguration) throws Exception {
        if (jsonCourseConfiguration == null || jsonCourseConfiguration.isEmpty()) {
            return getBadCourseConfigurationValidationErrorResponse(
                    "Course configuration is required to be given as json object");
        }
        if (regattaName == null || raceColumn == null || fleet == null) {
            return getBadCourseConfigurationValidationErrorResponse(
                    "Course configuration is required to have a regatta name and a race name");
        }
        final Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            return getBadRegattaErrorResponse(regattaName);
        }
        getSecurityService().checkCurrentUserUpdatePermission(regatta);
        final RaceColumn raceColumnByName = findRaceColumnByName(regatta, raceColumn);
        final Fleet fleetByName = findFleetByName(raceColumnByName, fleet);
        if (raceColumnByName == null || fleetByName == null) {
            return getBadRaceErrorResponse(regattaName, raceColumn, fleet);
        }
        final Object parsedObject = new JSONParser().parse(jsonCourseConfiguration);
        if (parsedObject == null || !(parsedObject instanceof JSONObject)) {
            getBadCourseConfigurationValidationErrorResponse(
                    "Course configuration is required to be given as json object");
        }
        final CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfiguration = getCourseConfigurationDeserializer(regatta)
                .deserialize((JSONObject) parsedObject);
        final TimePoint timestampForLogEntries = MillisecondsTimePoint.now();
        final Optional<UserGroup> optionalUserGroupForNonDefaultMarkPropertiesOwnership = getOptionalGroupOwnership(
                markPropertiesGroupOwnership);
        final CourseBase course = getService().getCourseAndMarkConfigurationFactory()
                .createCourseFromConfigurationAndDefineMarksAsNeeded(regatta, courseConfiguration,
                        timestampForLogEntries, getService().getServerAuthor(), optionalUserGroupForNonDefaultMarkPropertiesOwnership);
        final RaceLog raceLog = raceColumnByName.getRaceLog(fleetByName);
        raceLog.add(new RaceLogCourseDesignChangedEventImpl(timestampForLogEntries, getService().getServerAuthor(),
                raceLog.getCurrentPassId(), course, CourseDesignerMode.BY_MARKS));
        final CourseConfiguration<MarkConfigurationResponseAnnotation> courseConfigurationResult = getService().getCourseAndMarkConfigurationFactory()
                .createCourseConfigurationFromRegatta(course, regatta, raceColumnByName.getTrackedRace(fleetByName),
                        /* tagsToFilterMarkProperties */ Collections.emptyList());
        return Response.ok(streamingOutput(getCourseConfigurationJsonSerializer().serialize(courseConfigurationResult))).build();
    }

    private synchronized JsonSerializer<CourseConfiguration<MarkConfigurationResponseAnnotation>> getCourseConfigurationJsonSerializer() {
        if (courseConfigurationJsonSerializer == null) {
            courseConfigurationJsonSerializer = new CourseConfigurationJsonSerializer();
        }
        return courseConfigurationJsonSerializer;
    }

    private DeviceIdentifierJsonDeserializer getDeviceIdentifierDeserializer() {
        if (deviceIdentifierDeserializer == null) {
            deviceIdentifierDeserializer = new DeviceIdentifierJsonDeserializer(getDeviceJsonServiceFinder());
        }
        return deviceIdentifierDeserializer;
    }

    private synchronized TypeBasedServiceFinder<DeviceIdentifierJsonHandler> getDeviceJsonServiceFinder() {
        if (deviceJsonServiceFinder == null) {
            deviceJsonServiceFinder = getServiceFinderFactory().createServiceFinder(DeviceIdentifierJsonHandler.class);
            deviceJsonServiceFinder.setFallbackService(new PlaceHolderDeviceIdentifierJsonHandler());
        }
        return deviceJsonServiceFinder;
    }
}
