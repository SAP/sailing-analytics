package com.sap.sailing.shared.server.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.coursetemplate.CommonMarkProperties;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkPropertiesBuilder;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.MarkTemplate.MarkTemplateResolver;
import com.sap.sailing.domain.coursetemplate.Positioning;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.domain.coursetemplate.impl.CommonMarkPropertiesImpl;
import com.sap.sailing.domain.coursetemplate.impl.CourseTemplateImpl;
import com.sap.sailing.domain.coursetemplate.impl.FixedPositioningImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkPropertiesImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkRoleImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkTemplateImpl;
import com.sap.sailing.domain.coursetemplate.impl.TrackingDeviceBasedPositioningImpl;
import com.sap.sailing.shared.persistence.DomainObjectFactory;
import com.sap.sailing.shared.persistence.MongoObjectFactory;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sailing.shared.server.operations.CreateCourseTemplateOperation;
import com.sap.sailing.shared.server.operations.CreateMarkPropertiesOperation;
import com.sap.sailing.shared.server.operations.CreateMarkRoleOperation;
import com.sap.sailing.shared.server.operations.CreateMarkTemplateOperation;
import com.sap.sailing.shared.server.operations.DeleteCourseTemplateOperation;
import com.sap.sailing.shared.server.operations.DeleteMarkPropertiesOperation;
import com.sap.sailing.shared.server.operations.RecordUsageForMarkRoleOperation;
import com.sap.sailing.shared.server.operations.RecordUsageForMarkTemplateOperation;
import com.sap.sailing.shared.server.operations.SetPositioningInformationForMarkPropertiesOperation;
import com.sap.sailing.shared.server.operations.UpdateCourseTemplateOperation;
import com.sap.sailing.shared.server.operations.UpdateMarkPropertiesOperation;
import com.sap.sse.common.Position;
import com.sap.sse.common.RepeatablePart;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.interfaces.impl.AbstractReplicableWithObjectInputStream;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.util.ClearStateTestSupport;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;

public class SharedSailingDataImpl
extends AbstractReplicableWithObjectInputStream<ReplicatingSharedSailingData, OperationWithResult<ReplicatingSharedSailingData, ?>>
implements ReplicatingSharedSailingData, ClearStateTestSupport {
    private final DomainObjectFactory domainObjectFactory;
    private final MongoObjectFactory mongoObjectFactory;

    private final Map<UUID, MarkProperties> markPropertiesById = new ConcurrentHashMap<>();
    private final Map<UUID, MarkTemplate> markTemplatesById = new ConcurrentHashMap<>();
    private final Map<UUID, CourseTemplate> courseTemplatesById = new ConcurrentHashMap<>();
    private final Map<UUID, MarkRole> markRolesById = new ConcurrentHashMap<>();

    private final TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder;
    private final FullyInitializedReplicableTracker<SecurityService> securityServiceTracker;

    public SharedSailingDataImpl(final DomainObjectFactory domainObjectFactory,
            final MongoObjectFactory mongoObjectFactory, TypeBasedServiceFinderFactory serviceFinderFactory,
            FullyInitializedReplicableTracker<SecurityService> securityServiceTracker) {
        this.domainObjectFactory = domainObjectFactory;
        this.mongoObjectFactory = mongoObjectFactory;
        this.deviceIdentifierServiceFinder = serviceFinderFactory
                .createServiceFinder(DeviceIdentifierMongoHandler.class);
        this.securityServiceTracker = securityServiceTracker;
        load();
    }

    private void load() {
        // load mark templates and mark roles before mark properties and course templates
        domainObjectFactory.loadAllMarkTemplates().forEach(m -> markTemplatesById.put(m.getId(), m));
        domainObjectFactory.loadAllMarkRoles().forEach(m -> markRolesById.put(m.getId(), m));
        domainObjectFactory.loadAllMarkProperties(markTemplatesById::get, markRolesById::get)
                .forEach(m -> markPropertiesById.put(m.getId(), m));
        domainObjectFactory.loadAllCourseTemplates(markTemplatesById::get, markRolesById::get)
                .forEach(c -> courseTemplatesById.put(c.getId(), c));
    }

    @Override
    public void clearState() throws Exception {
        markPropertiesById.keySet().forEach(mongoObjectFactory::removeMarkProperties);
        markTemplatesById.keySet().forEach(mongoObjectFactory::removeMarkTemplate);
        courseTemplatesById.keySet().forEach(mongoObjectFactory::removeCourseTemplate);
        markRolesById.keySet().forEach(mongoObjectFactory::removeMarkRole);
        removeAll();
    }

    private void removeAll() {
        markPropertiesById.clear();
        markTemplatesById.clear();
        courseTemplatesById.clear();
        markRolesById.clear();
    }

    public SecurityService getSecurityService() {
        try {
            return securityServiceTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<MarkProperties> getAllMarkProperties(Iterable<String> tagsToFilterFor) {
        return markPropertiesById.values().stream().filter(m -> containsAny(m.getTags(), tagsToFilterFor))
                .filter(getSecurityService()::hasCurrentUserReadPermission).collect(Collectors.toList());
    }

    private <T> boolean containsAny(Iterable<T> iterable, Iterable<T> search) {
        if (Util.isEmpty(search)) {
            return true;
        }
        for (T t : search) {
            if (Util.contains(iterable, t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<MarkTemplate> getAllMarkTemplates() {
        return markTemplatesById.values().stream().filter(getSecurityService()::hasCurrentUserReadPermission)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<CourseTemplate> getAllCourseTemplates(Iterable<String> tagsToFilterFor) {
        return courseTemplatesById.values().stream().filter(c -> containsAny(c.getTags(), tagsToFilterFor))
                .filter(getSecurityService()::hasCurrentUserReadPermission).collect(Collectors.toList());
    }

    @Override
    public Iterable<CourseTemplate> getAllCourseTemplates() {
        return courseTemplatesById.values().stream().filter(getSecurityService()::hasCurrentUserReadPermission)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<MarkProperties> getAllMarkProperties() {
        return markPropertiesById.values().stream().filter(getSecurityService()::hasCurrentUserReadPermission)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<MarkRole> getAllMarkRoles() {
        return markRolesById.values().stream().filter(getSecurityService()::hasCurrentUserReadPermission)
                .collect(Collectors.toList());
    }

    @Override
    public MarkRole createMarkRole(final String name, String shortName) {
        final UUID idOfNewMarkRole = UUID.randomUUID();
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.MARK_ROLE,
                MarkRole.getTypeRelativeObjectIdentifier(idOfNewMarkRole), idOfNewMarkRole + "/" + name, () -> {
                    final UUID idOfNewMarkRoleForReplication = idOfNewMarkRole;
                    final String nameForReplication = name;
                    apply(new CreateMarkRoleOperation(idOfNewMarkRoleForReplication, nameForReplication, shortName));
                });
        return getMarkRoleById(idOfNewMarkRole);
    }

    @Override
    public Void internalCreateMarkRole(UUID idOfNewMarkRole, String name, String shortName) {
        final MarkRole markRole = new MarkRoleImpl(idOfNewMarkRole, name, shortName);
        mongoObjectFactory.storeMarkRole(markRole);
        markRolesById.put(markRole.getId(), markRole);
        return null;
    }

    @Override
    public MarkRole getMarkRoleById(UUID id) {
        if (id == null) {
            return null;
        }
        final MarkRole markRole = markRolesById.get(id);
        if (markRole != null) {
            getSecurityService().checkCurrentUserReadPermission(markRole);
        }
        return markRole;
    }

    @Override
    public MarkProperties createMarkProperties(CommonMarkProperties properties, Iterable<String> tags,
            Optional<UserGroup> optionalNonDefaultGroupOwnership) {
        final UUID idOfNewMarkProperties = UUID.randomUUID();
        optionalNonDefaultGroupOwnership.ifPresent(userGroup -> getSecurityService().setOwnership(
                SecuredDomainType.MARK_PROPERTIES.getQualifiedObjectIdentifier(
                        MarkProperties.getTypeRelativeObjectIdentifier(idOfNewMarkProperties)),
                getSecurityService().getCurrentUser(), userGroup));
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.MARK_PROPERTIES,
                MarkProperties.getTypeRelativeObjectIdentifier(idOfNewMarkProperties),
                idOfNewMarkProperties + "/" + properties.getName(), () -> {
                    final UUID idOfNewMarkPropertiesForReplication = idOfNewMarkProperties;
                    // important: CommonMarkProperties can be a variety of objects, particularly MarkImpl
                    // which would try to resolve against SharedDomainFactory which is not what this Replicable's
                    // object input stream resolves against. So clone into a CommonMarkPropertiesImpl
                    // which doesn't need resolution
                    final CommonMarkProperties propertiesForReplication = new CommonMarkPropertiesImpl(properties);
                    final Iterable<String> tagsForReplication = tags;
                    apply(new CreateMarkPropertiesOperation(idOfNewMarkPropertiesForReplication,
                            propertiesForReplication, tagsForReplication));
                });
        return getMarkPropertiesById(idOfNewMarkProperties);
    }

    @Override
    public MarkProperties updateMarkProperties(UUID uuid, CommonMarkProperties properties,
            Positioning positioningInformation, Iterable<String> tags) {
        final MarkProperties markProperties = updateMarkProperties(uuid, properties, tags);
        getSecurityService().checkCurrentUserUpdatePermission(markProperties);
        if (positioningInformation != null && markProperties != properties) { // no update required if same object
            apply(new SetPositioningInformationForMarkPropertiesOperation(uuid, positioningInformation));
        }
        return getMarkPropertiesById(uuid);
    }

    @Override
    public MarkProperties updateMarkProperties(UUID uuid, CommonMarkProperties properties, Iterable<String> tags) {
        final MarkProperties markProperties = markPropertiesById.get(uuid);
        if (markProperties == null) {
            throw new NullPointerException(String.format("Could not find mark properties with id %s", uuid.toString()));
        }
        if (markProperties != properties) { // no update required if same object
            getSecurityService().checkCurrentUserUpdatePermission(markProperties);
            // important: CommonMarkProperties can be a variety of objects, particularly MarkImpl
            // which would try to resolve against SharedDomainFactory which is not what this Replicable's
            // object input stream resolves against. So clone into a CommonMarkPropertiesImpl
            // which doesn't need resolution
            final CommonMarkProperties commonMarkPropertiesForSerialization = new CommonMarkPropertiesImpl(properties);
            apply(new UpdateMarkPropertiesOperation(uuid, commonMarkPropertiesForSerialization, tags));
        }
        return getMarkPropertiesById(uuid);
    }

    @Override
    public Void internalUpdateMarkProperties(UUID idOfMarkProperties, CommonMarkProperties properties,
            Iterable<String> tags) {
        final MarkPropertiesImpl markProperties = (MarkPropertiesImpl) markPropertiesById.get(idOfMarkProperties);
        if (markProperties != properties) { // no update required if same object
            markProperties.setName(properties.getName());
            markProperties.setColor(properties.getColor());
            markProperties.setPattern(properties.getPattern());
            markProperties.setShape(properties.getShape());
            markProperties.setShortName(properties.getShortName());
            markProperties.setType(properties.getType());
            markProperties.setTags(tags);
            mongoObjectFactory.storeMarkProperties(deviceIdentifierServiceFinder, markProperties);
            markPropertiesById.put(idOfMarkProperties, markProperties);
        }
        return null;
    }

    @Override
    public Void internalCreateMarkProperties(UUID idOfNewMarkProperties, CommonMarkProperties properties,
            Iterable<String> tags) {
        final MarkProperties markProperties = new MarkPropertiesBuilder(idOfNewMarkProperties, properties.getName(),
                properties.getShortName(), properties.getColor(), properties.getShape(), properties.getPattern(),
                properties.getType()).withTags(tags).build();
        mongoObjectFactory.storeMarkProperties(deviceIdentifierServiceFinder, markProperties);
        markPropertiesById.put(markProperties.getId(), markProperties);
        return null;
    }
    
    @Override
    public void clearPositioningForMarkProperties(final MarkProperties markProperties) {
        getSecurityService().checkCurrentUserUpdatePermission(markProperties);
        final UUID markPropertiesUUID = markProperties.getId();
        apply(new SetPositioningInformationForMarkPropertiesOperation(markPropertiesUUID, null));
    }

    @Override
    public void setFixedPositionForMarkProperties(final MarkProperties markProperties, final Position position) {
        getSecurityService().checkCurrentUserUpdatePermission(markProperties);
        final UUID markPropertiesUUID = markProperties.getId();
        apply(new SetPositioningInformationForMarkPropertiesOperation(markPropertiesUUID, new FixedPositioningImpl(position)));
    }

    @Override
    public MarkProperties getMarkPropertiesById(UUID id) {
        final MarkProperties markProperties;
        if (id == null) {
            markProperties = null;
        } else {
            markProperties = markPropertiesById.get(id);
            if (markProperties != null) {
                getSecurityService().checkCurrentUserReadPermission(markProperties);
            }
        }
        return markProperties;
    }

    @Override
    public void setTrackingDeviceIdentifierForMarkProperties(final MarkProperties markProperties,
            final DeviceIdentifier deviceIdentifier) {
        getSecurityService().checkCurrentUserUpdatePermission(markProperties);
        final UUID markPropertiesUUID = markProperties.getId();
        apply(new SetPositioningInformationForMarkPropertiesOperation(markPropertiesUUID,
                new TrackingDeviceBasedPositioningImpl(deviceIdentifier)));
    }

    @Override
    public Void internalSetPositioningInformationForMarkProperties(UUID markPropertiesId,
            Positioning positioningInformation) {
        final MarkProperties markProperties = markPropertiesById.get(markPropertiesId);
        if (markProperties == null) {
            logger.warning("Could not find mark properties for ID "+markPropertiesId+"; not setting positioning information");
        } else {
            markProperties.setPositioningInformation(positioningInformation);
            mongoObjectFactory.storeMarkProperties(deviceIdentifierServiceFinder, markProperties);
        }
        return null;
    }

    @Override
    public MarkTemplate createMarkTemplate(CommonMarkProperties properties) {
        final UUID idOfNewMarkTemplate = UUID.randomUUID();
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.MARK_TEMPLATE, MarkTemplate.getTypeRelativeObjectIdentifier(idOfNewMarkTemplate),
                idOfNewMarkTemplate + "/" + properties.getName(), () -> {
                    final UUID idOfNewMarkTemplateForReplication = idOfNewMarkTemplate;
                    // important: CommonMarkProperties can be a variety of objects, particularly MarkImpl
                    // which would try to resolve against SharedDomainFactory which is not what this Replicable's
                    // object input stream resolves against. So clone into a CommonMarkPropertiesImpl
                    // which doesn't need resolution
                    final CommonMarkProperties propertiesForReplication = new CommonMarkPropertiesImpl(properties);
                    apply(new CreateMarkTemplateOperation(idOfNewMarkTemplateForReplication,
                            propertiesForReplication));
                });
        return getMarkTemplateById(idOfNewMarkTemplate);
    }

    @Override
    public Void internalCreateMarkTemplate(UUID idOfNewMarkTemplate, CommonMarkProperties properties) {
        final MarkTemplate markTemplate = new MarkTemplateImpl(idOfNewMarkTemplate, properties);
        mongoObjectFactory.storeMarkTemplate(markTemplate);
        markTemplatesById.put(markTemplate.getId(), markTemplate);
        return null;
    }

    @Override
    public MarkTemplate getMarkTemplateById(UUID id) {
        final MarkTemplate markTemplate = markTemplatesById.get(id);
        if (markTemplate != null) {
            getSecurityService().checkCurrentUserReadPermission(markTemplate);
        }
        return markTemplate;
    }

    @Override
    public CourseTemplate createCourseTemplate(String courseTemplateName, String courseTemplateShortName, Iterable<MarkTemplate> marks,
            Iterable<WaypointTemplate> waypoints, Map<MarkTemplate, MarkRole> defaultRolesForMarkTemplates,
            Map<MarkRole, MarkTemplate> defaultMarkTemplatesForMarkRoles,
            RepeatablePart optionalRepeatablePart, Iterable<String> tags, URL optionalImageURL,
            Integer defaultNumberOfLaps) {
        // check that all MarkRole objects reachable from the waypoints have a valid mapping to their default mark template
        final Set<MarkRole> rolesAlreadyChecked = new HashSet<>();
        for (WaypointTemplate waypoint : waypoints) {
            for (final MarkRole markRole : waypoint.getControlPointTemplate().getMarkRoles()) {
                if (!rolesAlreadyChecked.contains(markRole)) {
                    if (!defaultMarkTemplatesForMarkRoles.containsKey(markRole)) {
                        throw new IllegalArgumentException(
                                "All mark roles contained in the sequence are expected to have a mapping to a mark template");
                    }
                    final MarkTemplate mark = defaultMarkTemplatesForMarkRoles.get(markRole);
                    if (!Util.contains(marks, mark)) {
                        throw new IllegalArgumentException(
                                "All marks contained in the sequence are expected to be part of the course template");
                    }
                    rolesAlreadyChecked.add(markRole);
                }
            }
        }
        final UUID idOfNewCourseTemplate = UUID.randomUUID();
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.COURSE_TEMPLATE,
                CourseTemplate.getTypeRelativeObjectIdentifier(idOfNewCourseTemplate),
                idOfNewCourseTemplate + "/" + courseTemplateName, () -> {
                    final UUID idOfNewCourseTemplateForReplication = idOfNewCourseTemplate;
                    final String courseTemplateNameForReplication = courseTemplateName;
                    final Iterable<MarkTemplate> marksForReplication = marks;
                    final Iterable<WaypointTemplate> waypointsForReplication = waypoints;
                    final Map<MarkTemplate, MarkRole> effectiveAssociatedRolesForReplication = defaultRolesForMarkTemplates;
                    final RepeatablePart optionalRepeatablePartForReplication = optionalRepeatablePart;
                    final Iterable<String> tagsForReplication = tags;
                    final URL optionalImageURLForReplication = optionalImageURL;
                    final Integer defaultNumberOfLapsForReplication = defaultNumberOfLaps;
                    apply(new CreateCourseTemplateOperation(idOfNewCourseTemplateForReplication, courseTemplateNameForReplication, courseTemplateShortName,
                            marksForReplication, waypointsForReplication, effectiveAssociatedRolesForReplication, defaultMarkTemplatesForMarkRoles, optionalRepeatablePartForReplication,
                            tagsForReplication, optionalImageURLForReplication, defaultNumberOfLapsForReplication));
                });
        return getCourseTemplateById(idOfNewCourseTemplate);
    }

    @Override
    public CourseTemplate updateCourseTemplate(UUID uuid, String name, String shortName, URL optionalImageURL, ArrayList<String> tags,
            Integer defaultNumberOfLaps) {
        getSecurityService().checkCurrentUserUpdatePermission(courseTemplatesById.get(uuid));
        apply(new UpdateCourseTemplateOperation(uuid, name, shortName, optionalImageURL, tags, defaultNumberOfLaps));
        return getCourseTemplateById(uuid);
    }

    @Override
    public Void internalUpdateCourseTemplate(UUID uuid, String name, String shortName,
            URL optionalImageURL, ArrayList<String> tags, Integer defaultNumberOfLaps) {
        CourseTemplate existingCourseTemplate = courseTemplatesById.get(uuid);
        CourseTemplateImpl courseTemplate = new CourseTemplateImpl(uuid, name,
                shortName, existingCourseTemplate.getMarkTemplates(),
                existingCourseTemplate.getWaypointTemplates(), existingCourseTemplate.getDefaultMarkTemplatesForMarkRoles(),
                existingCourseTemplate.getDefaultMarkRolesForMarkTemplates(), optionalImageURL,
                existingCourseTemplate.getRepeatablePart(), defaultNumberOfLaps);
        courseTemplate.setTags(tags);
        mongoObjectFactory.storeCourseTemplate(courseTemplate);
        courseTemplatesById.put(courseTemplate.getId(), courseTemplate);
        return null;
    }

    @Override
    public Void internalCreateCourseTemplate(UUID idOfNewCourseTemplate, String courseTemplateName,
            String courseTemplateShortName, Iterable<MarkTemplate> marks,
            Iterable<WaypointTemplate> waypoints,
            Map<MarkTemplate, MarkRole> defaultMarkRolesForMarkTemplates, Map<MarkRole, MarkTemplate> defaultMarkTemplatesForMarkRoles,
            RepeatablePart optionalRepeatablePart, Iterable<String> tags, URL optionalImageURL, Integer defaultNumberOfLaps) {
        final CourseTemplateImpl courseTemplate = new CourseTemplateImpl(idOfNewCourseTemplate, courseTemplateName,
                courseTemplateShortName, marks, waypoints, defaultMarkTemplatesForMarkRoles,
                defaultMarkRolesForMarkTemplates, optionalImageURL, optionalRepeatablePart, defaultNumberOfLaps);
        courseTemplate.setTags(tags);
        mongoObjectFactory.storeCourseTemplate(courseTemplate);
        courseTemplatesById.put(courseTemplate.getId(), courseTemplate);
        return null;
    }

    @Override
    public CourseTemplate getCourseTemplateById(UUID id) {
        final CourseTemplate courseTemplate = courseTemplatesById.get(id);
        if (courseTemplate != null) {
            getSecurityService().checkCurrentUserReadPermission(courseTemplate);
        }
        return courseTemplate;
    }

    @Override
    public Void internalRecordUsage(UUID markTemplateId, UUID markPropertiesId) {
        final MarkProperties markProperties = markPropertiesById.get(markPropertiesId);
        final MarkTemplate markTemplate = markTemplatesById.get(markTemplateId);
        markProperties.addMarkTemplateUsage(markTemplate, MillisecondsTimePoint.now());
        mongoObjectFactory.storeMarkProperties(deviceIdentifierServiceFinder, markProperties);
        return null;
    }

    @Override
    public void recordUsage(MarkTemplate markTemplate, MarkProperties markProperties) {
        getSecurityService().checkCurrentUserUpdatePermission(markProperties);
        final UUID markPropertiesId = markProperties.getId();
        final UUID markTemplateId = markTemplate.getId();
        apply(new RecordUsageForMarkTemplateOperation(markTemplateId, markPropertiesId));
    }

    @Override
    public Void internalRecordUsage(final UUID markPropertiesId, final MarkRole markRole) {
        final MarkProperties markProperties = markPropertiesById.get(markPropertiesId);
        markProperties.addMarkRoleUsage(markRole, MillisecondsTimePoint.now());
        mongoObjectFactory.storeMarkProperties(deviceIdentifierServiceFinder, markProperties);
        return null;
    }

    @Override
    public void recordUsage(final MarkProperties markProperties, final MarkRole markRole) {
        getSecurityService().checkCurrentUserUpdatePermission(markProperties);
        final UUID markPropertiesId = markProperties.getId();
        apply(new RecordUsageForMarkRoleOperation(markPropertiesId, markRole));
    }

    @Override
    public Map<MarkProperties, TimePoint> getUsedMarkProperties(MarkTemplate markTemplate) {
        final Map<MarkProperties, TimePoint> recordedUsage = new HashMap<>();
        for (final MarkProperties mp : markPropertiesById.values()) {
            if (mp.getLastUsedMarkTemplate().containsKey(markTemplate)) {
                recordedUsage.put(mp, mp.getLastUsedMarkTemplate().get(markTemplate));
            }
        }
        return recordedUsage;
    }

    @Override
    public Map<MarkProperties, TimePoint> getUsedMarkProperties(MarkRole roleName) {
        final Map<MarkProperties, TimePoint> recordedUsage = new HashMap<>();
        for (final MarkProperties mp : markPropertiesById.values()) {
            if (mp.getLastUsedMarkRole().containsKey(roleName)) {
                recordedUsage.put(mp, mp.getLastUsedMarkRole().get(roleName));
            }
        }
        return recordedUsage;
    }

    @Override
    public void deleteMarkProperties(MarkProperties markProperties) {
        final UUID id = markProperties.getId();
        getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(markProperties,
                () -> apply(new DeleteMarkPropertiesOperation(id)));
    }

    @Override
    public Void internalDeleteMarkProperties(UUID markPropertiesUUID) {
        if (this.markPropertiesById.remove(markPropertiesUUID) != null) {
            mongoObjectFactory.removeMarkProperties(markPropertiesUUID);
        } else {
            throw new NullPointerException(
                    String.format("Did not find a mark properties with ID %s", markPropertiesUUID));
        }
        return null;
    }

    @Override
    public void deleteCourseTemplate(CourseTemplate courseTemplate) {
        final UUID id = courseTemplate.getId();
        getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(courseTemplate,
                () -> apply(new DeleteCourseTemplateOperation(id)));
    }

    @Override
    public Void internalDeleteCourseTemplate(UUID courseTemplateUUID) {
        if (this.courseTemplatesById.remove(courseTemplateUUID) != null) {
            mongoObjectFactory.removeCourseTemplate(courseTemplateUUID);
        } else {
            throw new NullPointerException(
                    String.format("Did not find a course template with ID %s", courseTemplateUUID));
        }
        return null;
    }

    @Override
    public ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is, Map<String, Class<?>> classLoaderCache) throws IOException {
        return new ObjectInputStreamResolvingAgainstCache<MarkTemplateResolver>(is,
                mt -> markTemplatesById.computeIfAbsent(mt.getId(), id -> mt), null, classLoaderCache) {
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void initiallyFillFromInternal(ObjectInputStream is)
            throws IOException, ClassNotFoundException, InterruptedException {
        markTemplatesById.putAll((Map<UUID, MarkTemplate>) is.readObject());
        markRolesById.putAll((Map<UUID, MarkRole>) is.readObject());
        markPropertiesById.putAll((Map<UUID, MarkProperties>) is.readObject());
        courseTemplatesById.putAll((Map<UUID, CourseTemplate>) is.readObject());
    }

    @Override
    public void serializeForInitialReplicationInternal(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(markTemplatesById);
        objectOutputStream.writeObject(markRolesById);
        objectOutputStream.writeObject(markPropertiesById);
        objectOutputStream.writeObject(courseTemplatesById);
    }

    @Override
    public synchronized void clearReplicaState() throws MalformedURLException, IOException, InterruptedException {
        removeAll();
    }
}
