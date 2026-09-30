package com.sap.sailing.shared.persistence.impl;

import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.coursetemplate.CommonMarkProperties;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.FixedPositioning;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.PositioningVisitor;
import com.sap.sailing.domain.coursetemplate.TrackingDeviceBasedPositioning;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.shared.persistence.MongoObjectFactory;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sailing.shared.persistence.device.impl.PlaceHolderDeviceIdentifierMongoHandler;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Position;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private static final Logger logger = Logger.getLogger(MongoObjectFactoryImpl.class.getName());
    private final MongoDatabase database;
    private final TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder;

    public MongoObjectFactoryImpl(MongoDatabase mongoDatabase) {
        this(mongoDatabase, /* serviceFinderFactory */ null);
    }

    public MongoObjectFactoryImpl(MongoDatabase mongoDatabase, TypeBasedServiceFinderFactory serviceFinderFactory) {
        this.database = mongoDatabase;
        if (serviceFinderFactory != null) {
            this.deviceIdentifierServiceFinder = serviceFinderFactory.createServiceFinder(DeviceIdentifierMongoHandler.class);
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
    public void storeMarkProperties(TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder,
            MarkProperties markProperties) {
        MongoCollection<Document> collection = database.getCollection(CollectionNames.MARK_PROPERTIES.name());
        Document query = new Document(FieldNames.MARK_PROPERTIES_ID.name(), markProperties.getId().toString());
        try {
            Document entry = storeMarkPropertiesToDocument(deviceIdentifierServiceFinder, markProperties);
            collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry,
                    new ReplaceOptions().upsert(true));
        } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not load mark properties because device identifier could not be stored.", e);
        }
    }

    private Document storeMarkPropertiesToDocument(
            TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder,
            MarkProperties markProperties) throws TransformationException, NoCorrespondingServiceRegisteredException {
        final Document result = new Document();
        result.put(FieldNames.MARK_PROPERTIES_ID.name(), markProperties.getId().toString());
        storeCommonMarkProperties(markProperties, result);
        if (markProperties.getPositioningInformation() != null) {
            markProperties.getPositioningInformation().accept(new PositioningVisitor<Void>() {
                @Override
                public Void visit(FixedPositioning fixedPositioning) {
                    result.put(FieldNames.MARK_PROPERTIES_FIXED_POSITION.name(),
                            storePosition(fixedPositioning.getFixedPosition()));
                    return null;
                }

                @Override
                public Void visit(TrackingDeviceBasedPositioning trackingDeviceBasedPositioning) {
                    try {
                        result.put(FieldNames.MARK_PROPERTIES_TRACKING_DEVICE_IDENTIFIER.name(),
                                storeDeviceId(deviceIdentifierServiceFinder, trackingDeviceBasedPositioning.getDeviceIdentifier()));
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        BasicDBList tags = new BasicDBList();
        if (markProperties.getTags() != null) {
            markProperties.getTags().forEach(tags::add);
        }
        result.put(FieldNames.MARK_PROPERTIES_TAGS.name(), tags);
        Map<String, Long> lastUsedTemplateMap = markProperties.getLastUsedMarkTemplate().entrySet().stream()
                .collect(Collectors.toMap(k -> k.getKey().getId().toString(), v -> v.getValue().asMillis()));
        result.put(FieldNames.MARK_PROPERTIES_USED_TEMPLATE.name(), new BasicDBObject(lastUsedTemplateMap));
        Map<String, Long> lastUsedRoleMap = markProperties.getLastUsedMarkRole().entrySet().stream()
                .collect(Collectors.toMap(k -> k.getKey().getId().toString(), v -> v.getValue().asMillis()));
        result.put(FieldNames.MARK_PROPERTIES_USED_ROLE.name(), new BasicDBObject(lastUsedRoleMap));
        return result;
    }

    public static Document storeDeviceId(
                TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder, DeviceIdentifier device)
                                throws TransformationException, NoCorrespondingServiceRegisteredException {
        final Document result;
        if (device == null) {
            result = null;
        } else {
            String type = device.getIdentifierType();
            DeviceIdentifierMongoHandler handler = deviceIdentifierServiceFinder.findService(type);
            com.sap.sse.common.Util.Pair<String, ? extends Object> pair = handler.serialize(device);
            type = pair.getA();
            Object deviceTypeSpecificId = pair.getB();
            result = new Document()
                            .append(FieldNames.DEVICE_TYPE.name(), type)
                            .append(FieldNames.DEVICE_TYPE_SPECIFIC_ID.name(), deviceTypeSpecificId)
                            .append(FieldNames.DEVICE_STRING_REPRESENTATION.name(), device.getStringRepresentation());
        }
        return result;
    }

    public static Bson getDeviceQuery(
            TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder, DeviceIdentifier device)
                            throws TransformationException, NoCorrespondingServiceRegisteredException {
        final Bson result;
        if (device == null) {
            result = null;
        } else {
            String type = device.getIdentifierType();
            DeviceIdentifierMongoHandler handler = deviceIdentifierServiceFinder.findService(type);
            com.sap.sse.common.Util.Pair<String, ? extends Object> pair = handler.serialize(device);
            type = pair.getA();
            Object deviceTypeSpecificId = pair.getB();
            result = Filters.and(Filters.eq(FieldNames.DEVICE_ID.name()+"."+FieldNames.DEVICE_STRING_REPRESENTATION.name(), device.getStringRepresentation()),
                                 Filters.eq(FieldNames.DEVICE_ID.name()+"."+FieldNames.DEVICE_TYPE_SPECIFIC_ID.name(), deviceTypeSpecificId),
                                 Filters.eq(FieldNames.DEVICE_ID.name()+"."+FieldNames.DEVICE_TYPE.name(), type));
        }
        return result;
    }
    
    private Document storePosition(Position position) {
        final Document result;
        if (position == null) {
            result = null;
        } else {
            result = new Document();
            result.put(FieldNames.LAT_DEG.name(), position.getLatDeg());
            result.put(FieldNames.LNG_DEG.name(), position.getLngDeg());
        }
        return result;
    }

    private void storeCommonMarkProperties(CommonMarkProperties markProperties, final Document result) {
        if (markProperties.getColor() != null) {
            result.put(FieldNames.COMMON_MARK_PROPERTIES_COLOR.name(), markProperties.getColor().getAsHtml());
        }
        result.put(FieldNames.COMMON_MARK_PROPERTIES_NAME.name(), markProperties.getName());
        result.put(FieldNames.COMMON_MARK_PROPERTIES_PATTERN.name(), markProperties.getPattern());
        result.put(FieldNames.COMMON_MARK_PROPERTIES_SHAPE.name(), markProperties.getShape());
        result.put(FieldNames.COMMON_MARK_PROPERTIES_SHORT_NAME.name(), markProperties.getShortName());
        result.put(FieldNames.COMMON_MARK_PROPERTIES_TYPE.name(),
                markProperties.getType() == null ? null : markProperties.getType().name());
    }

    @Override
    public void removeMarkProperties(UUID markPropertiesId) {
        MongoCollection<Document> configurationsCollections = database
                .getCollection(CollectionNames.MARK_PROPERTIES.name());
        Document query = new Document(FieldNames.MARK_PROPERTIES_ID.name(), markPropertiesId.toString());
        configurationsCollections.deleteOne(query);
    }

    @Override
    public void storeMarkTemplate(MarkTemplate markTemplate) {
        final MongoCollection<Document> collection = database.getCollection(CollectionNames.MARK_TEMPLATES.name());
        final Document query = new Document(FieldNames.MARK_TEMPLATE_ID.name(), markTemplate.getId().toString());

        final Document entry = storeMarkTemplateToDocument(markTemplate);
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry,
                new ReplaceOptions().upsert(true));
    }

    private Document storeMarkTemplateToDocument(MarkTemplate markTemplate) {
        final Document result = new Document(FieldNames.MARK_TEMPLATE_ID.name(), markTemplate.getId().toString());
        storeCommonMarkProperties(markTemplate, result);
        return result;
    }

    @Override
    public void removeMarkTemplate(UUID markTemplateId) {
        final MongoCollection<Document> configurationsCollections = database
                .getCollection(CollectionNames.MARK_TEMPLATES.name());
        final Document query = new Document(FieldNames.MARK_TEMPLATE_ID.name(), markTemplateId.toString());
        configurationsCollections.deleteOne(query);
    }

    @Override
    public void storeMarkRole(MarkRole markRole) {
        final MongoCollection<Document> collection = database.getCollection(CollectionNames.MARK_ROLES.name());
        final Document query = new Document(FieldNames.MARK_ROLE_ID.name(), markRole.getId().toString());
        final Document entry = storeMarkRoleToDocument(markRole);
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry,
                new ReplaceOptions().upsert(true));
    }

    private Document storeMarkRoleToDocument(MarkRole markRole) {
        final Document result = new Document(FieldNames.MARK_ROLE_ID.name(), markRole.getId().toString());
        result.put(FieldNames.MARK_ROLE_NAME.name(), markRole.getName());
        result.put(FieldNames.MARK_ROLE_SHORT_NAME.name(), markRole.getShortName());
        return result;
    }
    
    @Override
    public void removeMarkRole(UUID markRoleId) {
        final MongoCollection<Document> configurationsCollections = database
                .getCollection(CollectionNames.MARK_ROLES.name());
        final Document query = new Document(FieldNames.MARK_ROLE_ID.name(), markRoleId.toString());
        configurationsCollections.deleteOne(query);
    }

    @Override
    public void storeCourseTemplate(CourseTemplate courseTemplate) {
        final MongoCollection<Document> collection = database.getCollection(CollectionNames.COURSE_TEMPLATES.name());
        final Document query = new Document(FieldNames.COURSE_TEMPLATE_ID.name(), courseTemplate.getId().toString());
        final Document entry = storeCourseTemplateToDocument(courseTemplate);
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry,
                new ReplaceOptions().upsert(true));
    }

    private Document storeCourseTemplateToDocument(CourseTemplate courseTemplate) {
        final Document result = new Document();
        // store master data
        result.put(FieldNames.COURSE_TEMPLATE_ID.name(), courseTemplate.getId().toString());
        result.put(FieldNames.COURSE_TEMPLATE_NAME.name(), courseTemplate.getName());
        result.put(FieldNames.COURSE_TEMPLATE_SHORT_NAME.name(), courseTemplate.getShortName());
        result.put(FieldNames.COURSE_TEMPLATE_DEFAULT_NUMBER_OF_LAPS.name(), courseTemplate.getDefaultNumberOfLaps());
        final URL optionalImageURL = courseTemplate.getOptionalImageURL();
        if (optionalImageURL != null) {
            result.put(FieldNames.COURSE_TEMPLATE_IMAGE_URL.name(), optionalImageURL.toExternalForm());
        }
        // store mark role list including the mandatory mapping to mark templates
        final BasicDBList markRoles = new BasicDBList();
        courseTemplate.getDefaultMarkTemplatesForMarkRoles().forEach((role, mt) -> {
            final BasicDBObject markRoleObject = new BasicDBObject(
                    FieldNames.COURSE_TEMPLATE_MARK_ROLE_ID.name(), role.getId().toString());
            markRoleObject.append(FieldNames.COURSE_TEMPLATE_MARK_TEMPLATE_ID.name(), mt.getId().toString());
            markRoles.add(markRoleObject);
        });
        result.put(FieldNames.COURSE_TEMPLATE_MARK_ROLES.name(), markRoles);
        // store mark template list including role names for those who have one defined
        final BasicDBList markTemplates = new BasicDBList();
        courseTemplate.getDefaultMarkRolesForMarkTemplates().forEach((m, role) -> {
            final BasicDBObject markTemplateObject = new BasicDBObject(
                    FieldNames.COURSE_TEMPLATE_MARK_TEMPLATE_ID.name(), m.getId().toString());
            if (role != null) {
                markTemplateObject.append(FieldNames.COURSE_TEMPLATE_MARK_TEMPLATE_ROLE_ID.name(), role.getId().toString());
            }
            markTemplates.add(markTemplateObject);
        });
        result.put(FieldNames.COURSE_TEMPLATE_MARK_TEMPLATES.name(), markTemplates);
        // store waypoint templates
        final BasicDBList waypointTemplates = new BasicDBList();
        for (WaypointTemplate waypointTemplate : courseTemplate.getWaypointTemplates()) {
            BasicDBObject waypointTemplateObject = storeWaypointTemplate(waypointTemplate);
            waypointTemplates.add(waypointTemplateObject);
        }
        result.put(FieldNames.COURSE_TEMPLATE_WAYPOINTS.name(), waypointTemplates);
        // tags
        final BasicDBList tags = new BasicDBList();
        if (courseTemplate.getTags() != null) {
            courseTemplate.getTags().forEach(tags::add);
        }
        result.put(FieldNames.COURSE_TEMPLATE_TAGS.name(), tags);
        // repeatable part
        if (courseTemplate.hasRepeatablePart()) {
            final BasicDBObject repeatablePart = storeRepeatablePart(courseTemplate);
            result.put(FieldNames.COURSE_TEMPLATE_REPEATABLE_PART.name(), repeatablePart);
        }
        return result;
    }

    private BasicDBObject storeWaypointTemplate(WaypointTemplate waypointTemplate) {
        BasicDBObject waypointTemplateObject = new BasicDBObject();
        waypointTemplateObject.put(FieldNames.WAYPOINT_TEMPLATE_PASSINGINSTRUCTION.name(),
                getPassingInstructions(waypointTemplate.getPassingInstruction()));
        waypointTemplateObject.put(FieldNames.WAYPOINT_TEMPLATE_CONTROL_POINT_NAME.name(),
                waypointTemplate.getControlPointTemplate().getName());
        waypointTemplateObject.put(FieldNames.WAYPOINT_TEMPLATE_CONTROL_POINT_SHORT_NAME.name(),
                waypointTemplate.getControlPointTemplate().getShortName());
        final BasicDBList markRoles = new BasicDBList();
        waypointTemplate.getControlPointTemplate().getMarkRoles().forEach(m -> markRoles.add(m.getId().toString()));
        waypointTemplateObject.put(FieldNames.WAYPOINT_TEMPLATE_MARK_ROLES.name(), markRoles);
        return waypointTemplateObject;
    }

    public static String getPassingInstructions(PassingInstruction passingInstructions) {
        final String passing;
        if (passingInstructions != null) {
            passing = passingInstructions.name();
        } else {
            passing = null;
        }
        return passing;
    }

    private BasicDBObject storeRepeatablePart(CourseTemplate courseTemplate) {
        final BasicDBObject repeatablePart = new BasicDBObject();
        repeatablePart.put(FieldNames.REPEATABLE_PART_START.name(),
                courseTemplate.getRepeatablePart().getZeroBasedIndexOfRepeatablePartStart());
        repeatablePart.put(FieldNames.REPEATABLE_PART_END.name(),
                courseTemplate.getRepeatablePart().getZeroBasedIndexOfRepeatablePartEnd());
        return repeatablePart;
    }

    @Override
    public void removeCourseTemplate(UUID courseTemplateId) {
        final MongoCollection<Document> configurationsCollections = database
                .getCollection(CollectionNames.COURSE_TEMPLATES.name());
        final Document query = new Document(FieldNames.COURSE_TEMPLATE_ID.name(), courseTemplateId.toString());
        configurationsCollections.deleteOne(query);
    }
}
