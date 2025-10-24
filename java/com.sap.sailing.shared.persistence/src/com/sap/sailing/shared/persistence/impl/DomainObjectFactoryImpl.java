package com.sap.sailing.shared.persistence.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.coursetemplate.ControlPointTemplate;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkPropertiesBuilder;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkRolePair.MarkRolePairFactory;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.domain.coursetemplate.impl.CourseTemplateImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkRoleImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkTemplateImpl;
import com.sap.sailing.domain.coursetemplate.impl.WaypointTemplateImpl;
import com.sap.sailing.domain.racelogtracking.impl.PlaceHolderDeviceIdentifierSerializationHandler;
import com.sap.sailing.shared.persistence.DomainObjectFactory;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sailing.shared.persistence.device.impl.PlaceHolderDeviceIdentifierMongoHandler;
import com.sap.sse.common.Color;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.RepeatablePart;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.RepeatablePartImpl;
import com.sap.sse.common.TransformationException;

public class DomainObjectFactoryImpl implements DomainObjectFactory {
    private static final Logger logger = Logger.getLogger(DomainObjectFactoryImpl.class.getName());
    private final MongoDatabase database;
    private final TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder;

    public DomainObjectFactoryImpl(MongoDatabase mongoDatabase) {
        this(mongoDatabase, /* serviceFinderFactory */ null);
    }

    public DomainObjectFactoryImpl(MongoDatabase mongoDatabase,
            TypeBasedServiceFinderFactory serviceFinderFactory) {
        this.database = mongoDatabase;
        if (serviceFinderFactory != null) {
            this.deviceIdentifierServiceFinder = serviceFinderFactory
                    .createServiceFinder(DeviceIdentifierMongoHandler.class);
            this.deviceIdentifierServiceFinder.setFallbackService(new PlaceHolderDeviceIdentifierMongoHandler());
        } else {
            this.deviceIdentifierServiceFinder = null;
        }
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }

    @Override
    public Iterable<MarkProperties> loadAllMarkProperties(Function<UUID, MarkTemplate> markTemplateResolver,
            Function<UUID, MarkRole> markRoleResolver) {
        final List<MarkProperties> result = new ArrayList<>();
        final MongoCollection<Document> configurationCollection = database
                .getCollection(CollectionNames.MARK_PROPERTIES.name());
        try {
            for (final Document dbObject : configurationCollection.find()) {
                final MarkProperties entry = loadMarkPropertiesEntry(dbObject, markTemplateResolver, markRoleResolver);
                result.add(entry);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load mark properties.");
            logger.log(Level.SEVERE, "loadAllMarkProperties", e);
        }

        return result;
    }

    private MarkProperties loadMarkPropertiesEntry(final Document dbObject,
            Function<UUID, MarkTemplate> markTemplateResolver,
            Function<UUID, MarkRole> markRoleResolver) {
        // load all mandatory data
        final UUID id = UUID.fromString(dbObject.getString(FieldNames.MARK_PROPERTIES_ID.name()));
        final String name = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_NAME.name());
        final String shortName = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_SHORT_NAME.name());
        final String pattern = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_PATTERN.name());
        final String shape = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_SHAPE.name());
        final String type = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_TYPE.name());
        final MarkType markType = type == null ? null : MarkType.valueOf(type);
        final String storedColor = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_COLOR.name());
        final Color color = AbstractColor.getCssColor(storedColor);
        final Document positionDocument = dbObject.get(FieldNames.MARK_PROPERTIES_FIXED_POSITION.name(), Document.class);
        final Position fixedPosition = positionDocument == null ? null : loadPosition(positionDocument);
        final ArrayList<?> tagsList = dbObject.get(FieldNames.MARK_PROPERTIES_TAGS.name(), ArrayList.class);
        final Collection<String> tags = tagsList == null ? Collections.emptyList() : tagsList.stream().map(t -> t.toString()).collect(Collectors.toList());
        // all mandatory data are loaded -> create builder 
        final MarkPropertiesBuilder builder = new MarkPropertiesBuilder(id, name, shortName, color, shape, pattern, markType).withTags(tags);
        if (fixedPosition != null) {
            builder.withFixedPosition(fixedPosition);
        } else {
            // load optional deviceId
            try {
                final Document deviceIdDocument = dbObject.get(FieldNames.MARK_PROPERTIES_TRACKING_DEVICE_IDENTIFIER.name(), Document.class);
                final DeviceIdentifier deviceIdentifier = deviceIdDocument == null ? null : loadDeviceId(deviceIdentifierServiceFinder, deviceIdDocument);
                if (deviceIdentifier != null) {
                    builder.withDeviceId(deviceIdentifier);
                }
            } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                logger.log(Level.WARNING, "Could not load deviceId for MarkProperties", e);
            }
        }
        // load map of last used templates
        final Document lastUsedTemplateObject = dbObject.get(FieldNames.MARK_PROPERTIES_USED_TEMPLATE.name(), Document.class);
        final Map<Object, Object> mapLastUsedTemplate = new HashMap<>();
        lastUsedTemplateObject.forEach((k, v) -> mapLastUsedTemplate.put(k, v));
        final Map<MarkTemplate, TimePoint> lastUsedTemplate = new HashMap<>();
        for (Map.Entry<Object, Object> e : mapLastUsedTemplate.entrySet()) {
            MarkTemplate markTemplate = markTemplateResolver.apply(UUID.fromString(e.getKey().toString()));
            if (markTemplate != null) {
                lastUsedTemplate.put(markTemplate, parseTimePoint(e.getValue()));
            }
            else {
                logger.warning(String.format("Could not resolve MarkTemplate with id %s for MarkProperties %s.",
                        e.getKey().toString(), id));
            }
        }
        builder.withLastUsedTemplate(lastUsedTemplate);
        // load map of last used roles
        final Document lastUsedRoleObject = dbObject.get(FieldNames.MARK_PROPERTIES_USED_ROLE.name(), Document.class);
        final Map<Object, Object> mapUsedRole = new HashMap<>();
        lastUsedRoleObject.forEach((k, v) -> mapUsedRole.put(k, v));
        final Map<MarkRole, TimePoint> lastUsedRole = mapUsedRole.entrySet().stream()
                .collect(Collectors.toMap(
                        (Object markRoleIdObject) -> markRoleResolver.apply(UUID.fromString((String) markRoleIdObject)),
                        v -> parseTimePoint(v)));
        builder.withLastUsedRole(lastUsedRole);
        return builder.build();
    }

    public static DeviceIdentifier loadDeviceId(
            TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder, Document deviceId)
            throws TransformationException, NoCorrespondingServiceRegisteredException {
        String deviceType = (String) deviceId.get(FieldNames.DEVICE_TYPE.name());
        Object deviceTypeId = deviceId.get(FieldNames.DEVICE_TYPE_SPECIFIC_ID.name());
        String stringRepresentation = (String) deviceId.get(FieldNames.DEVICE_STRING_REPRESENTATION.name());

        try {
            return deviceIdentifierServiceFinder.findService(deviceType).deserialize(deviceTypeId, deviceType,
                    stringRepresentation);
        } catch (TransformationException e) {
            return new PlaceHolderDeviceIdentifierSerializationHandler().deserialize(stringRepresentation, deviceType,
                    stringRepresentation);
        }
    }

    public static Position loadPosition(Document object) {
        Number latNumber = (Number) object.get(FieldNames.LAT_DEG.name());
        Double lat = latNumber == null ? null : latNumber.doubleValue();
        Number lngNumber = (Number) object.get(FieldNames.LNG_DEG.name());
        Double lng = lngNumber == null ? null : lngNumber.doubleValue();
        if (lat != null && lng != null) {
            return new DegreePosition(lat, lng);
        } else {
            return null;
        }
    }

    private TimePoint parseTimePoint(Object timePointAsNumber) {
        return timePointAsNumber != null ? new MillisecondsTimePoint(((Number) timePointAsNumber).longValue()) : null;
    }
    

    @Override
    public Iterable<MarkTemplate> loadAllMarkTemplates() {
        final List<MarkTemplate> result = new ArrayList<>();
        final MongoCollection<Document> configurationCollection = database
                .getCollection(CollectionNames.MARK_TEMPLATES.name());
        try {
            for (final Document dbObject : configurationCollection.find()) {
                final MarkTemplate entry = loadMarkTemplateEntry(dbObject);
                result.add(entry);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load mark templates.");
            logger.log(Level.SEVERE, "loadAllMarkTemplates", e);
        }
        return result;
    }

    private MarkTemplate loadMarkTemplateEntry(Document dbObject) {
        final UUID id = UUID.fromString(dbObject.getString(FieldNames.MARK_TEMPLATE_ID.name()));
        final String name = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_NAME.name());
        final String shortName = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_SHORT_NAME.name());
        final String pattern = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_PATTERN.name());
        final String shape = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_SHAPE.name());
        final String type = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_TYPE.name());
        final MarkType markType = type != null ? MarkType.valueOf(type) : null;
        final String storedColor = dbObject.getString(FieldNames.COMMON_MARK_PROPERTIES_COLOR.name());
        final Color color = AbstractColor.getCssColor(storedColor);
        return new MarkTemplateImpl(id, name, shortName, color, shape, pattern, markType);
    }
    
    @Override
    public Iterable<MarkRole> loadAllMarkRoles() {
        final List<MarkRole> result = new ArrayList<>();
        final MongoCollection<Document> configurationCollection = database
                .getCollection(CollectionNames.MARK_ROLES.name());
        try {
            for (final Document dbObject : configurationCollection.find()) {
                final MarkRole entry = loadMarkRoleEntry(dbObject);
                result.add(entry);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load mark roles.");
            logger.log(Level.SEVERE, "loadAllMarkRoles", e);
        }
        return result;
    }
    
    private MarkRole loadMarkRoleEntry(Document dbObject) {
        final UUID id = UUID.fromString(dbObject.getString(FieldNames.MARK_ROLE_ID.name()));
        final String name = dbObject.getString(FieldNames.MARK_ROLE_NAME.name());
        final String shortName = dbObject.getString(FieldNames.MARK_ROLE_SHORT_NAME.name());
        return new MarkRoleImpl(id, name, shortName);
    }

    @Override
    public Iterable<CourseTemplate> loadAllCourseTemplates(Function<UUID, MarkTemplate> markTemplateResolver,
            Function<UUID, MarkRole> markRoleResolver) {
        final List<CourseTemplate> result = new ArrayList<>();
        final MongoCollection<Document> configurationCollection = database
                .getCollection(CollectionNames.COURSE_TEMPLATES.name());
        for (final Document dbObject : configurationCollection.find()) {
            try {
                final CourseTemplate entry = loadCourseTemplateEntry(dbObject, markTemplateResolver, markRoleResolver);
                result.add(entry);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load a course template. Continuing with other course templates...", e);
            }
        }
        return result;
    }

    private CourseTemplate loadCourseTemplateEntry(Document dbObject,
            Function<UUID, MarkTemplate> markTemplateResolver,
            Function<UUID, MarkRole> markRoleResolver) {
        // load master data
        final UUID id = UUID.fromString(dbObject.getString(FieldNames.COURSE_TEMPLATE_ID.name()));
        final String name = dbObject.getString(FieldNames.COURSE_TEMPLATE_NAME.name());
        final String shortName = dbObject.getString(FieldNames.COURSE_TEMPLATE_SHORT_NAME.name());
        final String imageURLString = dbObject.getString(FieldNames.COURSE_TEMPLATE_IMAGE_URL.name());
        final Integer defaultNumberOfLaps = dbObject
                .getInteger(FieldNames.COURSE_TEMPLATE_DEFAULT_NUMBER_OF_LAPS.name());
        URL optionalImageURL = null;
        if (imageURLString != null) {
            try {
                optionalImageURL = new URL(imageURLString);
            } catch (MalformedURLException e) {
                logger.warning(String.format("Error parsing image URL %s for course template %s", imageURLString, id));
            }
        }
        // load mark templates and associated roles
        final ArrayList<?> markTemplatesList = dbObject.get(FieldNames.COURSE_TEMPLATE_MARK_TEMPLATES.name(), ArrayList.class);
        final Set<MarkTemplate> markTemplates = new HashSet<>();
        final Map<MarkTemplate, MarkRole> associatedRoles = new HashMap<>();
        for (Object entry : markTemplatesList) {
            if (entry instanceof Document) {
                final Document entryObject = (Document) entry;
                final UUID markTemplateUUID = UUID.fromString(entryObject.getString(FieldNames.COURSE_TEMPLATE_MARK_TEMPLATE_ID.name()));
                final MarkTemplate markTemplate = markTemplateResolver.apply(markTemplateUUID);
                if (markTemplate != null) {
                    markTemplates.add(markTemplate);
                    final String roleIdAsStringOrNull = entryObject.getString(FieldNames.COURSE_TEMPLATE_MARK_TEMPLATE_ROLE_ID.name());
                    final MarkRole markRoleOrNull = roleIdAsStringOrNull == null ? null : markRoleResolver.apply(UUID.fromString(roleIdAsStringOrNull));
                    if (markRoleOrNull != null) {
                        associatedRoles.put(markTemplate, markRoleOrNull);
                    }
                } else {
                    logger.warning(String.format("Could not resolve MarkTemplate with id %s for CourseTemplate %s.", markTemplateUUID, id));
                }
            } else {
                logger.warning(String.format("Unexpected entry for MarkTemplate found for CourseTemplate %s.", id));
            }
        }
        final Map<MarkRole, MarkTemplate> defaultMarkTemplatesForRoles = new HashMap<>();
        final ArrayList<?> markRolesList = dbObject.get(FieldNames.COURSE_TEMPLATE_MARK_ROLES.name(), ArrayList.class);
        final Set<MarkRole> markRoles = new HashSet<>();
        for (Object entry : markRolesList) {
            if (entry instanceof Document) {
                final Document entryObject = (Document) entry;
                final UUID markRoleUUID = UUID.fromString(entryObject.getString(FieldNames.COURSE_TEMPLATE_MARK_ROLE_ID.name()));
                final MarkRole markRole = markRoleResolver.apply(markRoleUUID);
                if (markRole != null) {
                    markRoles.add(markRole);
                    final String markTemplateIdAsString = entryObject.getString(FieldNames.COURSE_TEMPLATE_MARK_ROLE_DEFAULT_MARK_TEMPLATE_ID.name());
                    final MarkTemplate markTemplate = markTemplateIdAsString == null ? null : markTemplateResolver.apply(UUID.fromString(markTemplateIdAsString));
                    if (markTemplate != null) {
                        defaultMarkTemplatesForRoles.put(markRole, markTemplate);
                    }
                } else {
                    logger.warning(String.format("Could not resolve MarkTemplate with id %s for CourseTemplate %s.", markRoleUUID, id));
                }
            } else {
                logger.warning(String.format("Unexpected entry for MarkTemplate found for CourseTemplate %s.", id));
            }
        }
        // load waypoints
        final ArrayList<?> waypointTemplatesList = dbObject.get(FieldNames.COURSE_TEMPLATE_WAYPOINTS.name(), ArrayList.class);
        final List<WaypointTemplate> waypointTemplates = new ArrayList<>();
        final MarkRolePairFactory markRolePairFactory = new MarkRolePairFactory();
        for (final Object o : waypointTemplatesList) {
            if (o instanceof Document) {
                final Document bdo = (Document) o;
                waypointTemplates.add(loadWaypointTemplate(bdo, markRoleResolver, markRolePairFactory));
            } else {
                logger.warning(String.format("Could not load document for CourseTemplate %s.", id));
            }
        }
        // load repeatable parts
        final BasicDBObject dbRepPart = (BasicDBObject) dbObject.get(FieldNames.COURSE_TEMPLATE_REPEATABLE_PART.name());
        final RepeatablePart optionalRepeatablePart;
        if (dbRepPart == null) {
            optionalRepeatablePart = null;
        } else {
            final int zeroBasedIndexOfRepeatablePartStart = dbRepPart.getInt(FieldNames.REPEATABLE_PART_START.name());
            final int zeroBasedIndexOfRepeatablePartEnd = dbRepPart.getInt(FieldNames.REPEATABLE_PART_END.name());
            optionalRepeatablePart = new RepeatablePartImpl(zeroBasedIndexOfRepeatablePartStart, zeroBasedIndexOfRepeatablePartEnd);
        }
        // load tags
        final ArrayList<?> tagsDbObject = dbObject.get(FieldNames.COURSE_TEMPLATE_TAGS.name(), ArrayList.class);
        final List<String> tags = new ArrayList<>();
        tagsDbObject.forEach(t -> tags.add(t.toString()));
        final CourseTemplateImpl courseTemplateImpl = new CourseTemplateImpl(id, name, shortName, markTemplates,
                waypointTemplates, defaultMarkTemplatesForRoles, associatedRoles, optionalImageURL, optionalRepeatablePart, defaultNumberOfLaps);
        courseTemplateImpl.setTags(tags);
        return courseTemplateImpl;
    }

    private WaypointTemplate loadWaypointTemplate(Document bdo,
            Function<UUID, MarkRole> markRoleResolver,
            MarkRolePairFactory markRolePairResolver) {
        // load passing instruction
        final PassingInstruction passingInstruction = PassingInstruction
                .valueOf(bdo.get(FieldNames.WAYPOINT_TEMPLATE_PASSINGINSTRUCTION.name()).toString());
        // load master data
        final String name = bdo.getString(FieldNames.WAYPOINT_TEMPLATE_CONTROL_POINT_NAME.name());
        final String shortName = bdo.getString(FieldNames.WAYPOINT_TEMPLATE_CONTROL_POINT_SHORT_NAME.name());
        // load mark roles for control point
        final ArrayList<?> markRoleUUIDsDbList = bdo.get(FieldNames.WAYPOINT_TEMPLATE_MARK_ROLES.name(), ArrayList.class);
        final List<MarkRole> markRoles = new ArrayList<>();
        for (Object obj : markRoleUUIDsDbList) {
            final MarkRole markRole = markRoleResolver.apply(UUID.fromString(obj.toString()));
            if (markRole == null) {
                logger.warning(String.format("Could not resolve MarkRole with id %s for WaypointTemplate.", obj.toString()));
            } else {
                markRoles.add(markRole);
            }
        }
        // create MarkTemplate or MarkTemplatePairImpl
        final ControlPointTemplate controlPointTemplate;
        if (markRoles.size() == 2) {
            controlPointTemplate = markRolePairResolver.create(name, shortName, markRoles.get(0), markRoles.get(1));
        } else {
            controlPointTemplate = markRoles.get(0);
        }
        return new WaypointTemplateImpl(controlPointTemplate, passingInstruction);
    }
}
