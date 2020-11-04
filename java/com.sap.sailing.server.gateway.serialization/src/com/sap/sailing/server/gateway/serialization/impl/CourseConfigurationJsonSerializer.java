package com.sap.sailing.server.gateway.serialization.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.coursetemplate.CommonMarkProperties;
import com.sap.sailing.domain.coursetemplate.FreestyleMarkProperties;
import com.sap.sailing.domain.coursetemplate.ControlPointWithMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.CourseConfiguration;
import com.sap.sailing.domain.coursetemplate.FreestyleMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationResponseAnnotation;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationVisitor;
import com.sap.sailing.domain.coursetemplate.MarkPairWithConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkPropertiesBasedMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkTemplateBasedMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.RegattaMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.RepeatablePart;
import com.sap.sailing.domain.coursetemplate.WaypointWithMarkConfiguration;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Triple;

public class CourseConfigurationJsonSerializer implements JsonSerializer<CourseConfiguration<MarkConfigurationResponseAnnotation>> {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_SHORT_NAME = "shortName";
    public static final String FIELD_OPTIONAL_IMAGE_URL = "optionalImageUrl";
    public static final String FIELD_OPTIONAL_COURSE_TEMPLATE_UUID = "courseTemplateId";
    public static final String FIELD_MARK_CONFIGURATIONS = "markConfigurations";
    public static final String FIELD_MARK_CONFIGURATION_MARK_TEMPLATE_ID = "markTemplateId";
    public static final String FIELD_MARK_CONFIGURATION_MARK_PROPERTIES_ID = "markPropertiesId";
    public static final String FIELD_MARK_CONFIGURATION_MARK_ID = "markId";
    public static final String FIELD_MARK_CONFIGURATION_EFFECTIVE_PROPERTIES = "effectiveProperties";
    public static final String FIELD_MARK_CONFIGURATION_FREESTYLE_PROPERTIES = "freestyleProperties";
    public static final String FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_NAME = "associatedRole";
    public static final String FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_SHORT_NAME = "associatedRoleShortName";
    public static final String FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_ID = "associatedRoleId";
    public static final String FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPINGS = "trackingDevices";
    public static final String FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_TYPE = "trackingDeviceType";
    public static final String FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_HASH = "trackingDeviceHash";
    public static final String FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_LAST_KNOWN_POSITION = "trackingDeviceLastKnownPosition";
    public static final String FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPED_FROM = "trackingDeviceMappedFromMillis";
    public static final String FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPED_TO = "trackingDeviceMappedToMillis";
    public static final String FIELD_MARK_CONFIGURATION_LAST_KNOWN_POSITION = "lastKnownPosition";
    public static final String FIELD_MARK_CONFIGURATION_POSITIONING = "positioning";
    public static final String FIELD_MARK_CONFIGURATION_STORE_TO_INVENTORY = "storeToInventory";
    public static final String FIELD_WAYPOINTS = "waypoints";
    public static final String FIELD_WAYPOINT_CONTROL_POINT_NAME = "controlPointName";
    public static final String FIELD_WAYPOINT_CONTROL_POINT_SHORT_NAME = "controlPointShortName";
    public static final String FIELD_WAYPOINT_PASSING_INSTRUCTION = "passingInstruction";
    public static final String FIELD_WAYPOINT_MARK_CONFIGURATION_IDS = "markConfigurationIds";
    public static final String FIELD_OPTIONAL_REPEATABLE_PART = "optionalRepeatablePart";
    public static final String FIELD_NUMBER_OF_LAPS = "numberOfLaps";
    public static final String FIELD_MARK_CONFIGURATION_ID = "id";

    private final JsonSerializer<RepeatablePart> repeatablePartJsonSerializer;
    private final JsonSerializer<CommonMarkProperties> commonMarkPropertiesJsonSerializer;
    private final JsonSerializer<FreestyleMarkProperties> commonMarkPropertiesWithTagsJsonSerializer;
    private final JsonSerializer<GPSFix> gpsFixJsonSerializer;

    public CourseConfigurationJsonSerializer() {
        this.repeatablePartJsonSerializer = new RepeatablePartJsonSerializer();
        this.commonMarkPropertiesJsonSerializer = new CommonMarkPropertiesJsonSerializer();
        this.commonMarkPropertiesWithTagsJsonSerializer = new CommonMarkPropertiesWithTagsJsonSerializer();
        this.gpsFixJsonSerializer = new GPSFixJsonSerializer();
    }

    @Override
    public JSONObject serialize(CourseConfiguration<MarkConfigurationResponseAnnotation> courseConfiguration) {
        final JSONObject result = new JSONObject();
        result.put(FIELD_NAME, courseConfiguration.getName());
        result.put(FIELD_SHORT_NAME, courseConfiguration.getShortName());
        if (courseConfiguration.getOptionalCourseTemplate() != null) {
            result.put(FIELD_OPTIONAL_COURSE_TEMPLATE_UUID,
                    courseConfiguration.getOptionalCourseTemplate().getId().toString());
        }
        if (courseConfiguration.getOptionalImageURL() != null) {
            result.put(FIELD_OPTIONAL_IMAGE_URL, courseConfiguration.getOptionalImageURL().toString());
        }
        final Map<MarkConfiguration<MarkConfigurationResponseAnnotation>, UUID> markConfigurationsToTempIdMap = new HashMap<>();
        final JSONArray markConfigurationsJSON = new JSONArray();
        for (Map.Entry<MarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> markWithOptionalRole : courseConfiguration
                .getAllMarksWithOptionalRoles().entrySet()) {
            final MarkConfiguration<MarkConfigurationResponseAnnotation> markConfiguration = markWithOptionalRole.getKey();
            JSONObject markConfigurationsEntry = new JSONObject();
            final UUID markConfigurationId = UUID.randomUUID();
            markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_ID, markConfigurationId.toString());
            if (markConfiguration.getOptionalMarkTemplate() != null) {
                markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_MARK_TEMPLATE_ID,
                        markConfiguration.getOptionalMarkTemplate().getId().toString());
            }
            final MarkRole associatedRole = markWithOptionalRole.getValue();
            if (associatedRole != null) {
                markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_NAME, associatedRole.getName());
                markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_SHORT_NAME, associatedRole.getShortName());
                if (associatedRole instanceof MarkRole) {
                    markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_ID, ((MarkRole) associatedRole).getId().toString());
                }
            }
            markConfiguration.accept(new MarkConfigurationVisitor<Void, MarkConfigurationResponseAnnotation>() {
                @Override
                public Void visit(FreestyleMarkConfiguration<MarkConfigurationResponseAnnotation> freeStyleMarkConfiguration) {
                    if (freeStyleMarkConfiguration.getOptionalMarkProperties() != null) {
                        markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_MARK_PROPERTIES_ID,
                                freeStyleMarkConfiguration.getOptionalMarkProperties().getId().toString());
                    }
                    markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_FREESTYLE_PROPERTIES,
                            commonMarkPropertiesWithTagsJsonSerializer
                                    .serialize(freeStyleMarkConfiguration.getFreestyleProperties()));
                    return null;
                }

                @Override
                public Void visit(
                        MarkPropertiesBasedMarkConfiguration<MarkConfigurationResponseAnnotation> markPropertiesBasedMarkConfiguration) {
                    markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_MARK_PROPERTIES_ID,
                            markPropertiesBasedMarkConfiguration.getOptionalMarkProperties().getId().toString());
                    return null;
                }

                @Override
                public Void visit(
                        MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation> markConfiguration) {
                    // nothing to be done because the "optional" (in this case mandatory) getOptionalMarkTemplate() case
                    // has already been considered above generally
                    return null;
                }

                @Override
                public Void visit(RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> regattaMarkConfiguration) {
                    if (regattaMarkConfiguration.getOptionalMarkProperties() != null) {
                        markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_MARK_PROPERTIES_ID,
                                regattaMarkConfiguration.getOptionalMarkProperties().getId().toString());
                    }
                    markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_MARK_ID, regattaMarkConfiguration.getMark().getId().toString());
                    return null;
                }
            });
            markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_EFFECTIVE_PROPERTIES,
                    commonMarkPropertiesJsonSerializer.serialize(markConfiguration.getEffectiveProperties()));
            if (markConfiguration.getAnnotationInfo() != null) {
                final JSONArray deviceMappings = new JSONArray();
                markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPINGS,
                        deviceMappings);
                for (Triple<DeviceIdentifier, TimeRange, GPSFix> deviceMapping : markConfiguration.getAnnotationInfo()
                        .getDeviceMappings()) {
                    final JSONObject deviceMappingObject = new JSONObject();
                    final DeviceIdentifier deviceIdentifier = deviceMapping.getA();
                    deviceMappingObject.put(FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_TYPE,
                            deviceIdentifier.getIdentifierType());
                    deviceMappingObject.put(FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_HASH,
                            HashedStringUtil.toHashedString(deviceIdentifier.getStringRepresentation()));
                    final TimeRange mappedTimeRange = deviceMapping.getB();
                    if (!mappedTimeRange.hasOpenBeginning()) {
                        deviceMappingObject.put(FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPED_FROM,
                                mappedTimeRange.from().asMillis());
                    }
                    if (!mappedTimeRange.hasOpenEnd()) {
                        deviceMappingObject.put(FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPED_TO,
                                mappedTimeRange.to().asMillis());
                    }
                    final GPSFix lastKnownPosition = deviceMapping.getC();
                    if (lastKnownPosition != null) {
                        deviceMappingObject.put(FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_LAST_KNOWN_POSITION,
                                gpsFixJsonSerializer.serialize(lastKnownPosition));
                    }
                    deviceMappings.add(deviceMappingObject);
                }
                if (markConfiguration.getAnnotationInfo().getLastKnownPosition() != null) {
                    markConfigurationsEntry.put(FIELD_MARK_CONFIGURATION_LAST_KNOWN_POSITION,
                            gpsFixJsonSerializer.serialize(markConfiguration.getAnnotationInfo().getLastKnownPosition()));
                }
            }
            markConfigurationsToTempIdMap.put(markConfiguration, markConfigurationId);
            markConfigurationsJSON.add(markConfigurationsEntry);
        }
        result.put(FIELD_MARK_CONFIGURATIONS, markConfigurationsJSON);
        final JSONArray waypoints = new JSONArray();
        for (final WaypointWithMarkConfiguration<MarkConfigurationResponseAnnotation> waypoint :
            courseConfiguration.getNumberOfLaps() != null && courseConfiguration.getNumberOfLaps() > 1
                ? courseConfiguration.getWaypoints(courseConfiguration.getNumberOfLaps())
                : courseConfiguration.getWaypoints()) {
            final JSONObject waypointEntry = new JSONObject();
            waypointEntry.put(FIELD_WAYPOINT_PASSING_INSTRUCTION, waypoint.getPassingInstruction().name());
            final JSONArray markConfigurationIDs = new JSONArray();
            final ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> controlPoint = waypoint.getControlPoint();
            controlPoint.getMarkConfigurations()
                    .forEach(mc -> markConfigurationIDs.add(markConfigurationsToTempIdMap.get(mc).toString()));
            waypointEntry.put(FIELD_WAYPOINT_MARK_CONFIGURATION_IDS, markConfigurationIDs);
            if (controlPoint instanceof MarkPairWithConfiguration) {
                final MarkPairWithConfiguration<MarkConfigurationResponseAnnotation> markPairWithConfiguration =
                        (MarkPairWithConfiguration<MarkConfigurationResponseAnnotation>) controlPoint;
                waypointEntry.put(FIELD_WAYPOINT_CONTROL_POINT_NAME, markPairWithConfiguration.getName());
                waypointEntry.put(FIELD_WAYPOINT_CONTROL_POINT_SHORT_NAME, markPairWithConfiguration.getShortName());
            }
            waypoints.add(waypointEntry);
        }
        result.put(FIELD_WAYPOINTS, waypoints);
        if (courseConfiguration.hasRepeatablePart()) {
            result.put(FIELD_OPTIONAL_REPEATABLE_PART,
                    repeatablePartJsonSerializer.serialize(courseConfiguration.getRepeatablePart()));
        }
        result.put(FIELD_NUMBER_OF_LAPS, courseConfiguration.getNumberOfLaps());
        return result;
    }
}
