package com.sap.sailing.server.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogCloseOpenEndedDeviceMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.coursetemplate.CommonMarkProperties;
import com.sap.sailing.domain.coursetemplate.ControlPointTemplate;
import com.sap.sailing.domain.coursetemplate.ControlPointWithMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.CourseConfiguration;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.CourseTemplateCompatibilityChecker;
import com.sap.sailing.domain.coursetemplate.FixedPositioning;
import com.sap.sailing.domain.coursetemplate.FreestyleMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.FreestyleMarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationRequestAnnotation;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationRequestAnnotation.MarkRoleCreationRequest;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationResponseAnnotation;
import com.sap.sailing.domain.coursetemplate.MarkConfigurationVisitor;
import com.sap.sailing.domain.coursetemplate.MarkPairWithConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkPropertiesBasedMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkRolePair;
import com.sap.sailing.domain.coursetemplate.MarkRolePair.MarkRolePairFactory;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.MarkTemplateBasedMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.Positioning;
import com.sap.sailing.domain.coursetemplate.PositioningVisitor;
import com.sap.sailing.domain.coursetemplate.RegattaMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.TrackingDeviceBasedPositioning;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.domain.coursetemplate.WaypointWithMarkConfiguration;
import com.sap.sailing.domain.coursetemplate.impl.CourseConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.FreestyleMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.FreestyleMarkPropertiesImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkConfigurationRequestAnnotationImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkPairWithConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkPropertiesBasedMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.MarkTemplateBasedMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.RegattaMarkConfigurationImpl;
import com.sap.sailing.domain.coursetemplate.impl.WaypointTemplateImpl;
import com.sap.sailing.domain.coursetemplate.impl.WaypointWithMarkConfigurationImpl;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.racelogtracking.PingDeviceIdentifier;
import com.sap.sailing.domain.racelogtracking.impl.PingDeviceIdentifierImpl;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.deserialization.impl.CourseConfigurationBuilder;
import com.sap.sailing.server.interfaces.CourseAndMarkConfigurationFactory;
import com.sap.sailing.shared.server.SharedSailingData;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Position;
import com.sap.sse.common.RepeatablePart;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.security.shared.impl.UserGroup;

public class CourseAndMarkConfigurationFactoryImpl implements CourseAndMarkConfigurationFactory {

    private static final Logger logger = Logger.getLogger(CourseAndMarkConfigurationFactoryImpl.class.getName());
    
    private final FullyInitializedReplicableTracker<SharedSailingData> sharedSailingDataTracker;
    private final SensorFixStore sensorFixStore;
    
    /**
     * Obtains a "last known position" for a {@link DeviceIdentifier}.<p>
     * 
     * Note: the resolver will only work on a "master" instance of a master/replica cluster ("replica set") because
     * it accesses the persistence layer which is only defined on the master instance.
     */
    private final Function<DeviceIdentifier, GPSFix> lastKnownPositionResolver;
    private final RaceLogResolver raceLogResolver;
    private final DomainFactory domainFactory;

    public CourseAndMarkConfigurationFactoryImpl(
            FullyInitializedReplicableTracker<SharedSailingData> sharedSailingDataTracker,
            SensorFixStore sensorFixStore, RaceLogResolver raceLogResolver, DomainFactory domainFactory) {
        this.sharedSailingDataTracker = sharedSailingDataTracker;
        this.domainFactory = domainFactory;
        this.sensorFixStore = sensorFixStore;
        this.raceLogResolver = raceLogResolver;
        lastKnownPositionResolver = identifier -> {
            GPSFix lastPosition = null;
            try {
                final Map<DeviceIdentifier, Timed> lastFix = sensorFixStore.getFixLastReceived(Collections.singleton(identifier));
                final Timed t = lastFix.get(identifier);
                if (t instanceof GPSFix) {
                    lastPosition = ((GPSFix) t);
                }
            } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                logger.log(Level.WARNING, "Could not load associated fix for device " + identifier, e);
            }
            return lastPosition;
        };
    }
    
    private SharedSailingData getSharedSailingData() {
        SharedSailingData result;
        try {
            result = sharedSailingDataTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while waiting for a fully initialized SharedSailingData service; "
                    + "continuing with null, probably causing a NullPointerException along the way", e);
            result = null;
        }
        return result;
    }

    private CourseTemplate resolveCourseTemplateSafe(CourseBase course) {
        CourseTemplate courseTemplateOrNull;
        try {
            courseTemplateOrNull = resolveCourseTemplate(course);
        } catch (org.apache.shiro.authz.AuthorizationException e) {
            // The call may fail due to required permissions.
            // In this case we just handle it as there is no CourseTemplate.
            courseTemplateOrNull = null;
        }
        return courseTemplateOrNull;
    }
    
    @Override
    public CourseTemplate resolveCourseTemplate(CourseBase course) {
        final CourseTemplate result;
        if (course.getOriginatingCourseTemplateIdOrNull() == null) {
            result = null;
        } else {
            result = getSharedSailingData().getCourseTemplateById(course.getOriginatingCourseTemplateIdOrNull());
        }
        return result;
    }
    
    /**
     * For all {@link MarkConfiguration} objects contained in the {@code courseConfiguration} that have
     * {@link MarkConfiguration#getAnnotationInfo()}.{@link MarkConfigurationRequestAnnotation#isStoreToInventory()
     * isStoreToInventory()}{@code ==true}, a create/update for a {@link MarkProperties} object is performed.
     * <p>
     * 
     * If saving to the inventory is requested for a {@link MarkConfiguration} then if the requested
     * {@link MarkConfiguration} is a {@link RegattaMarkConfiguration} (requesting that an existing {@link Mark} in a
     * regatta be used), a corresponding {@link RegattaMarkConfiguration} is returned in the resulting course
     * configuration that has its {@link MarkConfiguration#getOptionalMarkProperties()} set to the
     * {@link MarkProperties} created for the regatta mark. If saving to inventory was request for any other type of
     * mark configuration, the mark configuration will be replaced in the result by a
     * {@link MarkPropertiesBasedMarkConfiguration} that references the {@link MarkProperties} object created or updated
     * based on the request.<br>
     * 
     * When the resulting {@link CourseConfiguration} is used to create a course from it, this ensures that all references
     * of the resulting {@link Mark#getOriginatingMarkPropertiesIdOrNull()} will then point to the {@link MarkProperties} objects
     * stored or updated to the "inventory." For example, if an existing {@link Mark} is referenced by an incoming
     * {@link RegattaMarkConfiguration}, the result contains an augmented {@link RegattaMarkConfiguration} whose
     * {@link RegattaMarkConfiguration#getOptionalMarkProperties()} refers to the {@link MarkProperties} that was created
     * (which is only the case if the {@link Mark} did not reference a {@link MarkProperties} object before). For all
     * other types of {@link MarkConfiguration} the new {@link MarkPropertiesBasedMarkConfiguration} ensures that a mark
     * is created such that it has all the properties requested and points to the desired combination of {@link MarkProperties}
     * and/or {@link MarkTemplate}.
     * 
     * @param optionalNonDefaultGroupOwnership
     *            in case {@link MarkProperties} objects have to be created then this parameter can be used to specify
     *            their group owner which otherwise will default to the calling subject user's default creation group
     *            for the server / replica set on which this request is executed.
     */
    private CourseConfiguration<MarkConfigurationRequestAnnotation> handleSaveToInventory(
            CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfiguration,
            Optional<UserGroup> optionalNonDefaultGroupOwnership) {
        final Map<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkConfiguration<MarkConfigurationRequestAnnotation>> effectiveConfigurations = new HashMap<>();
        for (MarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration : courseConfiguration.getAllMarks()) {
            if (markConfiguration.getAnnotationInfo().isStoreToInventory()) {
                final MarkProperties markPropertiesOrNull = markConfiguration.getOptionalMarkProperties();
                final MarkProperties markPropertiesInInventory;
                final Positioning positioningOrNull = markConfiguration.getAnnotationInfo().getOptionalPositioning();
                // create or update the non position-related aspects of the MarkProperties object in the "inventory":
                if (markPropertiesOrNull == null) {
                    CommonMarkProperties effectiveProperties = markConfiguration.getEffectiveProperties();
                    Iterable<String> tags = Collections.emptySet();
                    if (effectiveProperties instanceof FreestyleMarkProperties) {
                        tags = ((FreestyleMarkProperties) effectiveProperties).getTags();
                    } else {
                        tags = Collections.emptySet();
                    }
                    // If no mark properties exist yet, a new one is created
                    markPropertiesInInventory = getSharedSailingData().createMarkProperties(effectiveProperties, tags,
                            optionalNonDefaultGroupOwnership);
                } else {
                    CommonMarkProperties effectiveProperties = markConfiguration.getEffectiveProperties();
                    final Iterable<String> tags;
                    if (effectiveProperties instanceof FreestyleMarkProperties) {
                        tags = ((FreestyleMarkProperties) effectiveProperties).getTags();
                    }else {
                        tags = Collections.emptySet();
                    }
                    // in the case of a MarkPropertiesBasedMarkConfiguration, the following call is expected to notice
                    // the identity between the mark properties object to update and the mark properties that constitute
                    // the effective properties and then skip the update.
                    markPropertiesInInventory = markPropertiesOrNull;
                    getSharedSailingData().updateMarkProperties(markPropertiesInInventory.getId(), effectiveProperties,
                            positioningOrNull, tags);
                }
                if (positioningOrNull != null) {
                    positioningOrNull.accept(new PositioningVisitor<Void>() {
                        @Override
                        public Void visit(FixedPositioning fixedPositioning) {
                            getSharedSailingData().setFixedPositionForMarkProperties(markPropertiesInInventory, fixedPositioning.getFixedPosition());
                            return null;
                        }

                        @Override
                        public Void visit(TrackingDeviceBasedPositioning trackingDeviceBasedPositioning) {
                            getSharedSailingData().setTrackingDeviceIdentifierForMarkProperties(markPropertiesInInventory,
                                    trackingDeviceBasedPositioning.getDeviceIdentifier());
                            return null;
                        }
                    });
                }
                final MarkConfiguration<MarkConfigurationRequestAnnotation> effectiveMarkConfiguration;
                if (markConfiguration instanceof RegattaMarkConfiguration) {
                    // FIXME if the Mark referenced by this markConfiguration already references a MarkProperties by UUID, what if that's different from markPropertiesInInventory?
                    final RegattaMarkConfiguration<MarkConfigurationRequestAnnotation> regattaMarkConfiguration = (RegattaMarkConfiguration<MarkConfigurationRequestAnnotation>) markConfiguration;
                    effectiveMarkConfiguration = new RegattaMarkConfigurationImpl<MarkConfigurationRequestAnnotation>(regattaMarkConfiguration.getMark(),
                            new MarkConfigurationRequestAnnotationImpl(/* storeToInventory */ true,
                                    regattaMarkConfiguration.getAnnotationInfo().getOptionalPositioning(), /* optionalMarkRoleCreationRequest */ null),
                            regattaMarkConfiguration.getOptionalMarkTemplate(), markPropertiesInInventory);
                } else {
                    effectiveMarkConfiguration = new MarkPropertiesBasedMarkConfigurationImpl<>(markPropertiesInInventory,
                            markConfiguration.getOptionalMarkTemplate(), new MarkConfigurationRequestAnnotationImpl(/* storeToInventory */ true,
                                    // use the MarkProperty's own positioning information (which should be up to date here with the request annotation):
                                    markPropertiesInInventory.getPositioningInformation(), /* optionalMarkRoleCreationRequest */ null));
                }
                effectiveConfigurations.put(markConfiguration, effectiveMarkConfiguration);
            } else {
                // the request does not ask for this mark configuration to be stored to the MarkProperties "inventory" and hence
                // we can use the markConfiguration as-is
                effectiveConfigurations.put(markConfiguration, markConfiguration);
            }
        }
        final CourseConfigurationToCourseConfigurationMapper<MarkConfigurationRequestAnnotation> waypointConfigurationMapper =
                new CourseConfigurationToCourseConfigurationMapper<MarkConfigurationRequestAnnotation>(
                courseConfiguration.getWaypoints(), courseConfiguration.getAssociatedRoles(),
                effectiveConfigurations);
        return new CourseConfigurationImpl<>(courseConfiguration.getOptionalCourseTemplate(),
                new HashSet<>(effectiveConfigurations.values()),
                waypointConfigurationMapper.explicitAssociatedRoles,
                waypointConfigurationMapper.effectiveWaypoints, courseConfiguration.getRepeatablePart(),
                courseConfiguration.getNumberOfLaps(), courseConfiguration.getName(),
                courseConfiguration.getShortName(), courseConfiguration.getOptionalImageURL());
    }

    private MarkRole resolveMarkRoleByID(UUID markRoleId, CourseTemplate optionalCourseTemplate) {
        final MarkRole result;
        if (markRoleId == null) {
            result = null;
        } else {
            MarkRole markRole = null;
            if (optionalCourseTemplate != null) {
                markRole = optionalCourseTemplate.getMarkRoleByIdIfContainedInCourseTemplate(markRoleId);
            }
            if (markRole == null) {
                markRole = getSharedSailingData().getMarkRoleById(markRoleId);
            }
            result = markRole;
        }
        return result;
    }

    @Override
    public CourseConfiguration<MarkConfigurationRequestAnnotation> createCourseTemplateAndUpdatedConfiguration(
            final CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfiguration, Iterable<String> tags,
            Optional<UserGroup> optionalNonDefaultGroupOwnership) {
        final Set<MarkRole> allMarkRolesInNewCourseTemplate = new HashSet<>(courseConfiguration.getAssociatedRoles().values());
        final CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfigurationAfterInventory = handleSaveToInventory(courseConfiguration, optionalNonDefaultGroupOwnership);
        final Map<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkRole> markRolesByMarkConfigurations = new HashMap<>();
        final Map<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkConfiguration<MarkConfigurationRequestAnnotation>> marksConfigurationsMapping = new HashMap<>();
        final Set<MarkTemplate> allMarkTemplatesInNewCourseTemplate = new HashSet<>();
        final Map<MarkTemplate, MarkRole> defaultMarkRolesForMarkTemplates = new HashMap<>();
        final Map<MarkRole, MarkTemplate> defaultMarkTemplatesForMarkRoles = new HashMap<>();
        for (MarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration : courseConfigurationAfterInventory.getAllMarks()) {
            final MarkConfiguration<MarkConfigurationRequestAnnotation> effectiveConfiguration;
            final MarkTemplate effectiveMarkTemplate;
            final Pair<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkTemplate> effectiveConfigurationAndEffectiveMarkTemplate = markConfiguration.accept(
                    new MarkConfigurationVisitor<Pair<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkTemplate>, MarkConfigurationRequestAnnotation>() {
                @Override
                public Pair<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkTemplate> visit(
                        FreestyleMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                    final MarkConfiguration<MarkConfigurationRequestAnnotation> effectiveConfiguration;
                    final MarkTemplate effectiveMarkTemplate = getEffectiveMarkTemplate(markConfiguration);
                    final MarkProperties markPropertiesOrNull = markConfiguration.getOptionalMarkProperties();
                    if (markPropertiesOrNull != null) {
                        if (markPropertiesOrNull.hasEqualAppeareanceWith(effectiveMarkTemplate)) {
                            effectiveConfiguration = new MarkPropertiesBasedMarkConfigurationImpl<>(markPropertiesOrNull,
                                    effectiveMarkTemplate, markConfiguration.getAnnotationInfo());
                                } else {
                                    effectiveConfiguration = new FreestyleMarkConfigurationImpl<>(effectiveMarkTemplate,
                                            markPropertiesOrNull,
                                            new FreestyleMarkPropertiesImpl(effectiveMarkTemplate,
                                                    /* tags */ null),
                                            markConfiguration.getAnnotationInfo());
                                }
                            } else {
                        effectiveConfiguration = new MarkTemplateBasedMarkConfigurationImpl<>(effectiveMarkTemplate,
                                markConfiguration.getAnnotationInfo());
                    }
                    return new Pair<>(effectiveConfiguration, effectiveMarkTemplate);
                }

                @Override
                public Pair<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkTemplate> visit(
                        MarkPropertiesBasedMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                    // In this case the appearance of the created MarkTemplate is identical to the MarkProperties it is
                    // based upon.
                    return new Pair<>(markConfiguration, getEffectiveMarkTemplate(markConfiguration));
                }

                @Override
                public Pair<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkTemplate> visit(
                        MarkTemplateBasedMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                    final MarkTemplateBasedMarkConfigurationImpl<MarkConfigurationRequestAnnotation> effectiveConfiguration =
                            new MarkTemplateBasedMarkConfigurationImpl<>(markConfiguration.getOptionalMarkTemplate(), /* no positioning information known for a mark template */ null);
                    final MarkTemplate effectiveMarkTemplate = effectiveConfiguration.getOptionalMarkTemplate();
                    return new Pair<>(effectiveConfiguration, effectiveMarkTemplate);
                }

                @Override
                public Pair<MarkConfiguration<MarkConfigurationRequestAnnotation>, MarkTemplate> visit(
                        RegattaMarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
                    // The configuration is used as is. We can't enrich the Mark with the newly created MarkTemplate.
                    // In the UI, the regatta Mark still needs to be selected to ensure that all connections to regatta
                    // Marks are unchanged when saving the course to a race afterwards.
                    return new Pair<>(markConfiguration, getEffectiveMarkTemplate(markConfiguration));
                }
            });
            effectiveConfiguration = effectiveConfigurationAndEffectiveMarkTemplate.getA();
            effectiveMarkTemplate = effectiveConfigurationAndEffectiveMarkTemplate.getB();
            allMarkTemplatesInNewCourseTemplate.add(effectiveMarkTemplate);
            MarkRole effectiveMarkRole = courseConfigurationAfterInventory.getAssociatedRoles().get(markConfiguration);
            if (effectiveMarkRole == null) {
                // no existing MarkRole specified for the mark configuration; we now need to look for a MarkRoleCreationRequest annotation:
                final MarkRoleCreationRequest optionalMarkRoleCreationRequest = markConfiguration.getAnnotationInfo().getOptionalMarkRoleCreationRequest();
                if (optionalMarkRoleCreationRequest != null) {
                    effectiveMarkRole = getOrCreateMarkRole(allMarkRolesInNewCourseTemplate, optionalMarkRoleCreationRequest);
                    allMarkRolesInNewCourseTemplate.add(effectiveMarkRole); // could replace effectiveMarkRole by itself, but that's no problem
                }
            }
            assert effectiveMarkTemplate != null;
            if (effectiveMarkRole != null) {
                defaultMarkRolesForMarkTemplates.put(effectiveMarkTemplate, effectiveMarkRole);
                defaultMarkTemplatesForMarkRoles.put(effectiveMarkRole, effectiveMarkTemplate); // several different mark templates may refer to the same mark role; last wins for the default
            }
            markRolesByMarkConfigurations.put(markConfiguration, effectiveMarkRole);
            marksConfigurationsMapping.put(markConfiguration, effectiveConfiguration);
        }
        // now we should have a MarkTemplate for each MarkConfiguration from the CourseConfiguration;
        // let's see which MarkRoles need to be created based on the waypoints in the CourseConfiguration
        final MarkRolePairFactory markRolePairFactory = new MarkRolePairFactory();
        final CourseConfigurationToCourseConfigurationMapper<MarkConfigurationRequestAnnotation> waypointConfigurationMapper = new CourseConfigurationToCourseConfigurationMapper<>(
                    courseConfigurationAfterInventory.getWaypoints(), courseConfigurationAfterInventory.getAssociatedRoles(),
                    marksConfigurationsMapping);
        recordUsagesForMarkProperties(waypointConfigurationMapper.effectiveWaypoints, waypointConfigurationMapper.allAssociatedRoles);
        final CourseSequenceMapper<ControlPointTemplate, MarkRole, WaypointTemplate, MarkConfigurationRequestAnnotation> waypointTemplateMapper =
                new CourseSequenceMapper<ControlPointTemplate, MarkRole, WaypointTemplate, MarkConfigurationRequestAnnotation>(
                courseConfigurationAfterInventory.getWaypoints(),
                courseConfigurationAfterInventory.getAssociatedRoles(), markRolesByMarkConfigurations) {
            @Override
            protected ControlPointTemplate createMarkPair(MarkRole left, MarkRole right, String name, String shortName) {
                return markRolePairFactory.create(name, shortName, left, right);
            }

            @Override
            protected WaypointTemplate createWaypoint(ControlPointTemplate controlPoint, PassingInstruction passingInstruction) {
                return new WaypointTemplateImpl(controlPoint, passingInstruction);
            }
        };
        final CourseTemplate newCourseTemplate = getSharedSailingData().createCourseTemplate(courseConfigurationAfterInventory.getName(), courseConfigurationAfterInventory.getShortName(),
                allMarkTemplatesInNewCourseTemplate, waypointTemplateMapper.effectiveWaypoints,
                defaultMarkRolesForMarkTemplates, defaultMarkTemplatesForMarkRoles,
                courseConfigurationAfterInventory.getRepeatablePart(), tags,
                courseConfigurationAfterInventory.getOptionalImageURL(), courseConfigurationAfterInventory.getNumberOfLaps());
        return new CourseConfigurationImpl<MarkConfigurationRequestAnnotation>(newCourseTemplate,
                new HashSet<>(marksConfigurationsMapping.values()),
                waypointConfigurationMapper.allAssociatedRoles, waypointConfigurationMapper.effectiveWaypoints, courseConfigurationAfterInventory.getRepeatablePart(),
                courseConfigurationAfterInventory.getNumberOfLaps(),
                courseConfigurationAfterInventory.getName(), courseConfigurationAfterInventory.getShortName(), courseConfigurationAfterInventory.getOptionalImageURL());
    }

    private MarkTemplate getEffectiveMarkTemplate(
            MarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration) {
        final MarkTemplate effectiveMarkTemplate;
        final MarkTemplate markTemplateOrNull = markConfiguration.getOptionalMarkTemplate();
        if (markTemplateOrNull != null && markTemplateOrNull.hasEqualAppeareanceWith(markConfiguration.getEffectiveProperties())) {
            effectiveMarkTemplate = markTemplateOrNull;
        } else {
            effectiveMarkTemplate = getSharedSailingData().createMarkTemplate(markConfiguration.getEffectiveProperties());
        }
        return effectiveMarkTemplate;
    }
    
    /**
     * Based on the {@code markRoleCreationRequest} looks for a {@link MarkRole} in {@code markRoles}
     * that matches all properties requested (name and short name being equal to the values, including {@code null},
     * as requested in {@code markRoleCreationRequest}. If none are found, a new {@link MarkRole} entity is
     * created with the properties requested and returned. It is the caller's responsibility to add it to the
     * {@code markRoles} collection for subsequent calls. It isn't added here.<p>
     * 
     * If one is found, it is returned.
     */
    private MarkRole getOrCreateMarkRole(Iterable<MarkRole> markRoles,
            MarkRoleCreationRequest markRoleCreationRequest) {
        for (final MarkRole markRole : markRoles) {
            if (Util.equalsWithNull(markRole.getName(), markRoleCreationRequest.getMarkRoleName()) &&
                    Util.equalsWithNull(markRole.getShortName(), markRoleCreationRequest.getMarkRoleShortName())) {
                return markRole;
            }
        }
        return getSharedSailingData().createMarkRole(markRoleCreationRequest.getMarkRoleName(), markRoleCreationRequest.getMarkRoleShortName());
    }

    private <P> void recordUsagesForMarkProperties(Iterable<WaypointWithMarkConfiguration<P>> effectiveWaypoints,
            Map<MarkConfiguration<P>, MarkRole> allAssociatedRoles) {
        for (MarkConfiguration<P> markConfiguration : getAllMarkConfigurations(effectiveWaypoints)) {
            final MarkProperties markProperties = markConfiguration.getOptionalMarkProperties();
            if (markProperties != null) {
                final MarkRole markRoleOrNull = allAssociatedRoles.get(markConfiguration);
                if (markRoleOrNull != null) {
                    try {
                        getSharedSailingData().recordUsage(markProperties, markRoleOrNull);
                    } catch (Exception e) {
                        logger.log(Level.WARNING,
                                "Could not record usage for mark properties " + markProperties + " and role " + markRoleOrNull,
                                e);
                    }
                }
                final MarkTemplate markTemplateOrNull = markConfiguration.getOptionalMarkTemplate();
                if (markTemplateOrNull != null) {
                    try {
                        getSharedSailingData().recordUsage(markTemplateOrNull, markProperties);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Could not record usage for mark properties " + markProperties
                                + " and mark template " + markTemplateOrNull, e);
                    }
                }
            }
        }
    }

    private <P> Iterable<MarkConfiguration<P>> getAllMarkConfigurations(Iterable<WaypointWithMarkConfiguration<P>> waypoints) {
        final Set<MarkConfiguration<P>> result = new HashSet<>();
        for (WaypointWithMarkConfiguration<P> waypoint : waypoints) {
            Util.addAll(waypoint.getControlPoint().getMarkConfigurations(), result);
        }
        return result;
    }
    
    private void savePositioningToMark(Regatta regatta, Mark mark, Positioning optionalExplicitPositioning,
            MarkProperties optionalAssociatedMarkProperties, TimePoint timePointForDefinitionOfMarksAndDeviceMappings,
            AbstractLogEventAuthor author) {
        final Position[] position = new Position[1];
        final DeviceIdentifier[] deviceIdentifier = new DeviceIdentifier[1];
        final Positioning effectivePositioning;
        if (optionalExplicitPositioning != null) {
            effectivePositioning = optionalExplicitPositioning;
        } else if (optionalAssociatedMarkProperties != null && optionalAssociatedMarkProperties.getPositioningInformation() != null) {
            effectivePositioning = optionalAssociatedMarkProperties.getPositioningInformation();
        } else {
            effectivePositioning = null;
        }
        if (effectivePositioning != null) {
            effectivePositioning.accept(new PositioningVisitor<Void>() {
                @Override
                public Void visit(FixedPositioning fixedPositioning) {
                    position[0] = fixedPositioning.getFixedPosition();
                    return null;
                }

                @Override
                public Void visit(TrackingDeviceBasedPositioning trackingDeviceBasedPositioning) {
                    deviceIdentifier[0] = trackingDeviceBasedPositioning.getDeviceIdentifier();
                    return null;
                }
            });
        }
        // TODO combine the code below with the visitor pattern for the Positioning object above
        if (position[0] != null ^ deviceIdentifier[0] != null) {
            final DeviceMappingWithRegattaLogEvent<Mark> existingDeviceMapping = CourseConfigurationBuilder.findMostRecentOrOngoingMapping(regatta, mark);
            boolean terminateOpenEndedDeviceMapping = false;
            if (deviceIdentifier[0] != null) {
                // establish a new device mapping
                if (existingDeviceMapping == null || !(deviceIdentifier[0].equals(existingDeviceMapping.getDevice()) && existingDeviceMapping.getTimeRange().hasOpenEnd())) {
                    regatta.getRegattaLog()
                            .add(new RegattaLogDeviceMarkMappingEventImpl(
                                    timePointForDefinitionOfMarksAndDeviceMappings, author, mark, deviceIdentifier[0],
                                    timePointForDefinitionOfMarksAndDeviceMappings, null));
                    terminateOpenEndedDeviceMapping = true;
                }
            } else if (position[0] != null) {
                // ping the mark with the position given
                final boolean update;
                if (existingDeviceMapping != null) {
                    if (!PingDeviceIdentifier.TYPE.equals(existingDeviceMapping.getDevice().getIdentifierType())) {
                        update = true;
                        terminateOpenEndedDeviceMapping = true;
                    } else {
                        final GPSFix lastFixOrNull = lastKnownPositionResolver.apply(existingDeviceMapping.getDevice());
                        final Position lastPingedPositionOrNull = lastFixOrNull == null ? null : lastFixOrNull.getPosition();
                        update = lastPingedPositionOrNull == null || !lastPingedPositionOrNull.equals(position[0]);
                    }
                } else {
                    update = true;
                }
                if (update) {
                    // TODO check if we can use com.sap.sailing.domain.racelogtracking.impl.RaceLogTrackingAdapterImpl.pingMark(RegattaLog, Mark, GPSFix, RacingEventService)
                    final PingDeviceIdentifierImpl pingIdentifier = new PingDeviceIdentifierImpl(UUID.randomUUID());
                    sensorFixStore.storeFix(pingIdentifier,
                            new GPSFixImpl(position[0], timePointForDefinitionOfMarksAndDeviceMappings));
                    regatta.getRegattaLog()
                            .add(new RegattaLogDeviceMarkMappingEventImpl(
                                    timePointForDefinitionOfMarksAndDeviceMappings, author, mark, pingIdentifier,
                                    timePointForDefinitionOfMarksAndDeviceMappings,
                                    timePointForDefinitionOfMarksAndDeviceMappings));
                }
            }
            if (terminateOpenEndedDeviceMapping && existingDeviceMapping != null && existingDeviceMapping.getTimeRange().hasOpenEnd()) {
                regatta.getRegattaLog()
                        .add(new RegattaLogCloseOpenEndedDeviceMappingEventImpl(
                                timePointForDefinitionOfMarksAndDeviceMappings, author,
                                existingDeviceMapping.getRegattaLogEvent().getId(),
                                timePointForDefinitionOfMarksAndDeviceMappings.minusResolution()));
            }
        }
    }

    @Override
    public CourseBase createCourseFromConfigurationAndDefineMarksAsNeeded(Regatta regatta,
            final CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfiguration,
            TimePoint timePointForDefinitionOfMarksAndDeviceMappings,
            AbstractLogEventAuthor author, Optional<UserGroup> optionalNonDefaultGroupOwnership) {
        final CourseConfiguration<MarkConfigurationRequestAnnotation> courseConfigurationAfterInventory =
                handleSaveToInventory(courseConfiguration, optionalNonDefaultGroupOwnership);
        recordUsagesForMarkProperties(courseConfiguration.getWaypoints(), courseConfiguration.getAssociatedRoles());
        final Map<MarkConfiguration<MarkConfigurationRequestAnnotation>, Mark> marksByMarkConfigurations = new HashMap<>();
        final RegattaLog regattaLog = regatta.getRegattaLog();
        for (MarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration : courseConfigurationAfterInventory.getAllMarks()) {
            if (markConfiguration instanceof RegattaMarkConfiguration) {
                final Mark mark = ((RegattaMarkConfiguration<MarkConfigurationRequestAnnotation>) markConfiguration).getMark();
                marksByMarkConfigurations.put(markConfiguration, mark);
                savePositioningToMark(regatta, mark, markConfiguration.getAnnotationInfo().getOptionalPositioning(), /* optional MarkProperties */ null,
                        timePointForDefinitionOfMarksAndDeviceMappings, author);
            } else {
                final MarkTemplate optionalMarkTemplate = markConfiguration.getOptionalMarkTemplate();
                final MarkProperties optionalMarkProperties = markConfiguration.getOptionalMarkProperties();
                final CommonMarkProperties effectiveProperties = markConfiguration.getEffectiveProperties();
                final Mark markToCreate = domainFactory.getOrCreateMark(UUID.randomUUID(),
                        effectiveProperties.getName(), effectiveProperties.getShortName(),
                        effectiveProperties.getType(), effectiveProperties.getColor(), effectiveProperties.getShape(),
                        effectiveProperties.getPattern(),
                        optionalMarkTemplate == null ? null : optionalMarkTemplate.getId(),
                        optionalMarkProperties == null ? null : optionalMarkProperties.getId());
                regattaLog.add(new RegattaLogDefineMarkEventImpl(timePointForDefinitionOfMarksAndDeviceMappings, author,
                        timePointForDefinitionOfMarksAndDeviceMappings, UUID.randomUUID(), markToCreate));
                marksByMarkConfigurations.put(markConfiguration, markToCreate);
                savePositioningToMark(regatta, markToCreate, markConfiguration.getAnnotationInfo().getOptionalPositioning(),
                        optionalMarkProperties, timePointForDefinitionOfMarksAndDeviceMappings, author);
            }
        }
        // create the CourseBase result object:
        final CourseDataImpl course = new CourseDataImpl(courseConfigurationAfterInventory.getName(),
                courseConfigurationAfterInventory.getOptionalCourseTemplate() == null ? null
                        : courseConfigurationAfterInventory.getOptionalCourseTemplate().getId());
        final Iterable<WaypointWithMarkConfiguration<MarkConfigurationRequestAnnotation>> waypoints;
        if (courseConfigurationAfterInventory.hasRepeatablePart()) {
            if (courseConfigurationAfterInventory.getNumberOfLaps() == null) {
                throw new IllegalStateException("A course with repeatable part requires a lap count");
            }
            waypoints = courseConfigurationAfterInventory.getWaypoints(courseConfigurationAfterInventory.getNumberOfLaps());
        } else {
            waypoints = courseConfigurationAfterInventory.getWaypoints();
        }
        final CourseSequenceMapper<ControlPoint, Mark, Waypoint, MarkConfigurationRequestAnnotation> courseSequenceMapper =
                new CourseSequenceMapper<ControlPoint, Mark, Waypoint, MarkConfigurationRequestAnnotation>(
                        waypoints, courseConfigurationAfterInventory.getAssociatedRoles(), marksByMarkConfigurations) {
            @Override
            protected ControlPointWithTwoMarks createMarkPair(Mark left, Mark right, String name, String shortName) {
                return new ControlPointWithTwoMarksImpl(UUID.randomUUID(), left, right, name, shortName);
            }

            @Override
            protected Waypoint createWaypoint(ControlPoint controlPoint, PassingInstruction passingInstruction) {
                return new WaypointImpl(controlPoint, passingInstruction);
            }
        };
        courseSequenceMapper.effectiveWaypoints.forEach(wp -> course.addWaypoint(Util.size(course.getWaypoints()), wp));
        courseSequenceMapper.explicitAssociatedRoles.forEach((m, mr) -> course.addRoleMapping(m, mr.getId()));
        return course;
    }

    @Override
    public CourseConfiguration<MarkConfigurationResponseAnnotation> createCourseConfigurationFromTemplate(CourseTemplate courseTemplate,
            Regatta optionalRegatta, Iterable<String> tagsToFilterMarkProperties, Integer optionalNumberOfLaps) {
        final Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> markTemplatesToMarkConfigurations = new HashMap<>();
        if (optionalRegatta != null) {
            // If we have a regatta context, we first try to get all existing marks and their association to
            // MarkTemplates from the regatta
            final Map<MarkTemplate, RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigurationsByMarkTemplate = new HashMap<>();
            // record usages in races/courses in the regatta
            final Pair<Map<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, TimePoint>, LastUsageBasedAssociater<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole>> markUsagesForRoles =
                    getMarkUsagesForRoles(optionalRegatta, courseTemplate);
            final Map<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, TimePoint> lastUsages = markUsagesForRoles.getA();
            final LastUsageBasedAssociater<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> usagesForRole = markUsagesForRoles.getB();
            Set<MarkTemplate> markTemplatesToAssociate = new HashSet<>();
            Util.addAll(courseTemplate.getMarkTemplates(), markTemplatesToAssociate);
            // Primary matching is based on the associated role.
            for (Iterator<MarkTemplate> iterator = markTemplatesToAssociate.iterator(); iterator.hasNext();) {
                MarkTemplate mt = iterator.next();
                MarkRole roleOrNull = courseTemplate.getDefaultMarkRolesForMarkTemplates().get(mt);
                if (roleOrNull != null) {
                    RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> bestMatchForRole = usagesForRole.getBestMatchForT2(roleOrNull);
                    if (bestMatchForRole != null) {
                        iterator.remove();
                        markConfigurationsByMarkTemplate.put(mt, bestMatchForRole);
                        usagesForRole.removeT1(bestMatchForRole);
                        usagesForRole.removeT2(roleOrNull);
                    }
                }
            }
            // Marks that couldn't be matched by a role could be directly matched by an originating MarkTemplate and last usage
            for (RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> regattaMarkConfiguration : usagesForRole.usagesByT1.keySet()) {
                final MarkTemplate associatedMarkTemplateOrNull = regattaMarkConfiguration.getOptionalMarkTemplate();
                if (associatedMarkTemplateOrNull != null) {
                    markConfigurationsByMarkTemplate.compute(associatedMarkTemplateOrNull, (mt, rmc) -> {
                        final RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> result;
                        if (rmc == null) {
                            result = regattaMarkConfiguration;
                        } else {
                            final TimePoint lastUsageOrNull = lastUsages.get(regattaMarkConfiguration);
                            if (lastUsageOrNull == null) {
                                result = rmc;
                            } else {
                                final TimePoint lastUsageOfExistingOrNull = lastUsages.get(rmc);
                                if (lastUsageOfExistingOrNull == null) {
                                    result =  regattaMarkConfiguration;
                                } else {
                                    result = lastUsageOrNull.after(lastUsageOfExistingOrNull) ? regattaMarkConfiguration : rmc;
                                }
                            }
                        }
                        return result;
                    });
                }
            }
            markTemplatesToMarkConfigurations.putAll(markConfigurationsByMarkTemplate);
        }
        for (MarkTemplate markTemplate : courseTemplate.getMarkTemplates()) {
            // For any MarkTemplate that wasn't resolved from the regatta, an explicit entry needs to get created
            markTemplatesToMarkConfigurations.computeIfAbsent(markTemplate, mt -> {
                final MarkConfiguration<MarkConfigurationResponseAnnotation> markConfiguration = new MarkTemplateBasedMarkConfigurationImpl<>(markTemplate,
                        /* no positioning information for a MarkTemplate-based mark configuration */ null);
                return markConfiguration;
            });
        }
        replaceTemplateBasedConfigurationCandidatesBySuggestedPropertiesInPlace(markTemplatesToMarkConfigurations, tagsToFilterMarkProperties,
                courseTemplate.getDefaultMarkRolesForMarkTemplates());
        final Map<MarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> resultingRoleMapping = createRoleMappingWithMarkTemplateMapping(
                courseTemplate, markTemplatesToMarkConfigurations);
        final List<WaypointWithMarkConfiguration<MarkConfigurationResponseAnnotation>> resultingWaypoints = createWaypointConfigurationsWithMarkTemplateMapping(
                courseTemplate, markTemplatesToMarkConfigurations,
                optionalNumberOfLaps == null
                        ? courseTemplate.getDefaultNumberOfLaps() == null ? 0 : courseTemplate.getDefaultNumberOfLaps()
                        : optionalNumberOfLaps);
        return new CourseConfigurationImpl<MarkConfigurationResponseAnnotation>(courseTemplate, markTemplatesToMarkConfigurations.values(), resultingRoleMapping,
                resultingWaypoints, courseTemplate.getRepeatablePart(), optionalNumberOfLaps == null ? courseTemplate.getDefaultNumberOfLaps() : optionalNumberOfLaps,
                courseTemplate.getName(), courseTemplate.getShortName(), courseTemplate.getOptionalImageURL());
    }
    
    private Pair<Map<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, TimePoint>,
                 LastUsageBasedAssociater<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole>> getMarkUsagesForRoles(final Regatta regatta, final CourseTemplate courseTemplate) {
        final LastUsageBasedAssociater<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> usagesForRole = new LastUsageBasedAssociater<>(
                new HashSet<MarkRole>(courseTemplate.getDefaultMarkRolesForMarkTemplates().values())); 
        final Map<RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>, TimePoint> lastUsages = new HashMap<>();
        final Map<Mark, RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigurationsByMark = new HashMap<>();
        for (RaceColumn raceColumn : regatta.getRaceColumns()) {
            for (Mark mark : raceColumn.getAvailableMarks()) {
                markConfigurationsByMark
                        .computeIfAbsent(mark,
                                m -> createMarkConfigurationForRegattaMark(courseTemplate, regatta, /* optionalRace */ null, m));
            }
        }
        for (RaceColumn raceColumn : regatta.getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                final TrackedRace trackedRaceOrNull = raceColumn.getTrackedRace(fleet);
                TimePoint usage = null;
                if (trackedRaceOrNull != null) {
                    usage = trackedRaceOrNull.getStartOfRace();
                    if (usage == null) {
                        usage = trackedRaceOrNull.getStartOfTracking();
                    }
                }
                CourseBase courseOrNull = null;
                final RaceDefinition raceDefinition = raceColumn.getRaceDefinition(fleet);
                if (raceDefinition != null) {
                    courseOrNull = raceDefinition.getCourse();
                }
                if (courseOrNull == null || usage == null) {
                    final ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(raceLogResolver, raceColumn.getRaceLog(fleet));
                    if (courseOrNull == null) {
                        courseOrNull = raceState.getCourseDesign();
                    }
                    if (usage == null) {
                        usage = raceState.getStartTime();
                    }
                }
                if (usage == null) {
                    usage = TimePoint.BeginningOfTime;
                }
                if (courseOrNull != null) {
                    final TimePoint effectiveUsageTP = usage;
                    for (Waypoint waypoint : courseOrNull.getWaypoints()) {
                        for (Mark mark : waypoint.getMarks()) {
                            // the mark can be expected to be in the markConfigurationsByMark because all race columns have been enumerated
                            // and all their getAllAvailableMarks() have been mapped in markConfigurationsByMark already
                            final RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> regattaMarkConfiguration = markConfigurationsByMark.get(mark);
                            assert regattaMarkConfiguration != null;
                            lastUsages.compute(regattaMarkConfiguration,
                                    (mc, existingTP) -> (existingTP == null || existingTP.before(effectiveUsageTP))
                                    ? effectiveUsageTP
                                            : existingTP);
                            MarkRole markRole = resolveMarkRoleByID(courseOrNull.getAssociatedRoles().get(mark), courseTemplate);
                            if (markRole != null) {
                                usagesForRole.addUsage(regattaMarkConfiguration, markRole, effectiveUsageTP);
                            }
                        }
                    }
                }
            }
        }
        return new Pair<>(lastUsages, usagesForRole);
    }

    private Map<MarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> createRoleMappingWithMarkTemplateMapping(CourseTemplate courseTemplate,
            final Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> markTemplatesToMarkConfigurations) {
        final Map<MarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> resultingRoleMapping = new HashMap<>();
        for (Entry<MarkTemplate, MarkRole> markTemplateWithRole : courseTemplate.getDefaultMarkRolesForMarkTemplates().entrySet()) {
            resultingRoleMapping.put(markTemplatesToMarkConfigurations.get(markTemplateWithRole.getKey()),
                    markTemplateWithRole.getValue());
        }
        return resultingRoleMapping;
    }

    /**
     * For each {@link WaypointTemplate} in the {@link CourseTemplate} passed as {@code courseTemplate}, a
     * {@link WaypointWithMarkConfiguration} object is created and added to the resulting list. If a
     * {@link CourseTemplate#getRepeatablePart() repeatable part} exists in the course template, it occurs one time
     * fewer than the number of laps specified, but at least once. The {@link WaypointWithMarkConfiguration} will use
     * those {@link MarkConfiguration}s that correspond with the {@link MarkRole}s in the {@link CourseTemplate} where
     * the correspondence is defined by the {@code markTemplatesToMarkConfigurations} map, using the
     * {@link CourseTemplate#getDefaultMarkTemplateForMarkRole(MarkRole) default mark templates defined for the roles}.
     * 
     * @param numberOfLaps
     *            based on the number of laps, the number of occurrences of a repeatable part are decided. The repeatable
     *            part will occur {@code numberOfLaps-1} times, but at least once in the result.
     */
    private List<WaypointWithMarkConfiguration<MarkConfigurationResponseAnnotation>> createWaypointConfigurationsWithMarkTemplateMapping(
            CourseTemplate courseTemplate,
            final Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> markTemplatesToMarkConfigurations, int numberOfLaps) {
        return createWaypointConfigurationsWithMarkTemplateMapping(courseTemplate,
                markRole -> markTemplatesToMarkConfigurations.get(courseTemplate.getDefaultMarkTemplateForMarkRole(markRole)),
                numberOfLaps);
    }
    
    private List<WaypointWithMarkConfiguration<MarkConfigurationResponseAnnotation>> createWaypointConfigurationsWithMarkTemplateMapping(
            CourseTemplate courseTemplate,
            final Function<MarkRole, MarkConfiguration<MarkConfigurationResponseAnnotation>> getMarkConfigurationForMarkRole, int numberOfLaps) {
        final List<WaypointWithMarkConfiguration<MarkConfigurationResponseAnnotation>> resultingWaypoints = new ArrayList<>();
        for (WaypointTemplate waypointTemplate : numberOfLaps<2 ? courseTemplate.getWaypointTemplates() : courseTemplate.getWaypointTemplates(numberOfLaps)) {
            final ControlPointTemplate controlPointTemplate = waypointTemplate.getControlPointTemplate();
            final ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> resultingControlPoint = getOrCreateMarkConfigurationForControlPointTemplate(
                    getMarkConfigurationForMarkRole, controlPointTemplate);
            resultingWaypoints.add(new WaypointWithMarkConfigurationImpl<>(resultingControlPoint, waypointTemplate.getPassingInstruction()));
        }
        return resultingWaypoints;
    }

    /**
     * Maps the {@link MarkRole}s of the {@code controlPointTemplate} through the
     * {@code getMarkConfigurationForMarkRole}. If the control point template is a single-mark control point, the result
     * is returned. Otherwise, a {@link MarkPairWithConfiguration} is created with the two mark configurations obtained
     * by mapping the two {@link MarkRole}s from the control point template with two marks.
     */
    private ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> getOrCreateMarkConfigurationForControlPointTemplate(
            final Function<MarkRole, MarkConfiguration<MarkConfigurationResponseAnnotation>> getMarkConfigurationForMarkRole,
            final ControlPointTemplate controlPointTemplate) {
        final ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> resultingControlPoint;
        if (controlPointTemplate instanceof MarkRole) {
            MarkRole markRole = (MarkRole) controlPointTemplate;
            resultingControlPoint = getMarkConfigurationForMarkRole.apply(markRole);
            assert resultingControlPoint != null;
        } else {
            final MarkRolePair markPairTemplate = (MarkRolePair) controlPointTemplate;
            final MarkConfiguration<MarkConfigurationResponseAnnotation> left = getMarkConfigurationForMarkRole.apply(markPairTemplate.getLeft());
            assert left != null;
            final MarkConfiguration<MarkConfigurationResponseAnnotation> right = getMarkConfigurationForMarkRole.apply(markPairTemplate.getRight());
            assert right != null;
            resultingControlPoint = new MarkPairWithConfigurationImpl<>(markPairTemplate.getName(), left, right,
                    markPairTemplate.getShortName());
        }
        return resultingControlPoint;
    }

    @Override
    public CourseConfiguration<MarkConfigurationResponseAnnotation> createCourseConfigurationFromRegatta(CourseBase course, Regatta regatta,
            TrackedRace optionalRace, Iterable<String> tagsToFilterMarkProperties) {
        assert regatta != null;
        final Set<MarkConfiguration<MarkConfigurationResponseAnnotation>> allMarkConfigurations = new HashSet<>();
        final CourseTemplate courseTemplateOrNull = course == null ? null : resolveCourseTemplateSafe(course);
        // the RegattaMarkConfigurations returned by the following call will reference the MarkTemplates that their
        // underlying Marks referenced by UUID. For those we shall adopt the MarkTemplate-->MarkConfiguration mapping.
        final Map<Mark, RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigurationsByMark = createMarkConfigurationsForRegatta(
                regatta, optionalRace, courseTemplateOrNull);
        allMarkConfigurations.addAll(markConfigurationsByMark.values());
        final Map<MarkConfiguration<MarkConfigurationResponseAnnotation>, MarkRole> resultingRoleMapping = new HashMap<>();
        final Map<MarkRole, MarkConfiguration<MarkConfigurationResponseAnnotation>> resultingRoleToMarkConfigurationMapping = new HashMap<>();
        if (course != null) {
            for (Entry<Mark, UUID> markWithRole : course.getAssociatedRoles().entrySet()) {
                final MarkRole markRoleForMark = resolveMarkRoleByID(markWithRole.getValue(), courseTemplateOrNull);
                if (markRoleForMark != null) {
                    final RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> markConfigurationForRole = markConfigurationsByMark.get(markWithRole.getKey());
                    resultingRoleMapping.put(markConfigurationForRole, markRoleForMark);
                    resultingRoleToMarkConfigurationMapping.put(markRoleForMark, markConfigurationForRole);
                }
            }
        }
        String name = null;
        String shortName = null;
        URL optionalImageURL = null;
        final List<WaypointWithMarkConfiguration<MarkConfigurationResponseAnnotation>> resultingWaypoints = new ArrayList<>();
        RepeatablePart optionalRepeatablePart = null;
        final Integer numberOfLapsOrNullIfNoValidCourseTemplateInstance;
        if (courseTemplateOrNull != null && course != null) {
            numberOfLapsOrNullIfNoValidCourseTemplateInstance = isCourseInstanceOfCourseTemplate(course, courseTemplateOrNull);
            if (numberOfLapsOrNullIfNoValidCourseTemplateInstance != null) {
                // The course is a valid instance of the template. The resulting CourseConfiguration shall reflect this fact.
                // We'll copy the course template attributes and the number of laps we identified.
                name = courseTemplateOrNull.getName();
                shortName = courseTemplateOrNull.getShortName();
                optionalImageURL = courseTemplateOrNull.getOptionalImageURL();
                optionalRepeatablePart = courseTemplateOrNull.getRepeatablePart();
                final Map<MarkTemplate, MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigurationsForUnusedMarkRoles = new HashMap<>();
                // Now we know that the Course conforms to the CourseTemplate, and we have RegattaMarkConfigurations for all marks in the regatta.
                // But the course could be a one-lapper, with zero repetitions of the repeatable part (example: no leeward gate if
                // only one lap is sailed in a windward-leeward course). There may not be Marks in the regatta for the roles used
                // in the CourseTemplate's repeatable part.
                // We need to ensure that the resulting CourseConfiguration has MarkConfigurations for all MarkRoles used in
                // the CourseTemplate, so a client can create repetitions of the repeatable part.
                for (final Entry<MarkRole, MarkTemplate> e : courseTemplateOrNull.getDefaultMarkTemplatesForMarkRoles().entrySet()) {
                    if (!resultingRoleToMarkConfigurationMapping.containsKey(e.getKey())) {
                        final MarkTemplate markTemplateForMarkRoleWithoutMarkConfigurationSoFar = e.getValue();
                        // We found a MarkRole used in the CourseTemplate that does not have a MarkConfiguration mapping to it.
                        // We need to use the default MarkTemplate for that MarkRole and create a MarkConfiguration for it:
                        final MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation> markConfiguration = new MarkTemplateBasedMarkConfigurationImpl<MarkConfigurationResponseAnnotation>(
                                markTemplateForMarkRoleWithoutMarkConfigurationSoFar, /* response annotation: nothing known about positioning */ null);
                        markConfigurationsForUnusedMarkRoles.put(markTemplateForMarkRoleWithoutMarkConfigurationSoFar, markConfiguration);
                    }
                }
                // now check if we have good MarkProperties matches for any of those MarkTemplates; if so,
                // their MarkTemplateBasedMarkConfiguration is replaced by a MarkPropertiesBasedMarkConfiguration
                // that refers back to the MarkTemplate
                final Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> markConfigurationsForUnusedMarkRolesWithMatchingMarkProperties =
                        replaceTemplateBasedConfigurationCandidatesBySuggestedProperties(
                                markConfigurationsForUnusedMarkRoles, tagsToFilterMarkProperties,
                                courseTemplateOrNull.getDefaultMarkRolesForMarkTemplates());
                allMarkConfigurations.addAll(markConfigurationsForUnusedMarkRolesWithMatchingMarkProperties.values());
                for (final Entry<MarkRole, MarkTemplate> e : courseTemplateOrNull.getDefaultMarkTemplatesForMarkRoles().entrySet()) {
                    // record all mark configurations for MarkTemplates referenced by unused roles in resultingRoleToMarkConfigurationMapping and resultingRoleMapping
                    final MarkConfiguration<MarkConfigurationResponseAnnotation> markConfigForUnusedRole =
                            markConfigurationsForUnusedMarkRolesWithMatchingMarkProperties.get(e.getValue());
                    if (markConfigForUnusedRole != null) { // otherwise it may be unused but there is still a mark referring to that role, hence having
                        // a mark configuration connected to that role
                        resultingRoleMapping.put(markConfigForUnusedRole, e.getKey());
                        resultingRoleToMarkConfigurationMapping.put(e.getKey(), markConfigForUnusedRole);
                    }
                }
            }
        } else {
            numberOfLapsOrNullIfNoValidCourseTemplateInstance = null;
        }
        if (course != null) {
            final Iterator<WaypointTemplate> waypointTemplateIterator;
            int waypointIndex = 0;
            if (numberOfLapsOrNullIfNoValidCourseTemplateInstance != null && numberOfLapsOrNullIfNoValidCourseTemplateInstance == 1) {
                // course is a valid instance of a course template but has no occurrence of the repeatable sequence;
                // ensure that at least one occurrence of the repeatable part is inserted into the resulting waypoint sequence
                waypointTemplateIterator = courseTemplateOrNull.getWaypointTemplates().iterator();
            } else {
                waypointTemplateIterator = null;
            }
            for (Waypoint waypoint : course.getWaypoints()) {
                if (waypointTemplateIterator != null) {
                    WaypointTemplate waypointTemplate = waypointTemplateIterator.next();
                    while (waypointIndex >= courseTemplateOrNull.getRepeatablePart().getZeroBasedIndexOfRepeatablePartStart() &&
                            waypointIndex < courseTemplateOrNull.getRepeatablePart().getZeroBasedIndexOfRepeatablePartEnd()) {
                        // now insert the waypoint configurations for one occurrence of the repeatable part
                        final ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> resultingControlPoint =
                                getOrCreateMarkConfigurationForControlPointTemplate(resultingRoleToMarkConfigurationMapping::get,
                                        waypointTemplate.getControlPointTemplate());
                        resultingWaypoints.add(new WaypointWithMarkConfigurationImpl<>(resultingControlPoint,
                                waypointTemplate.getPassingInstruction()));
                        waypointIndex++;
                        if (waypointTemplateIterator.hasNext()) {
                            waypointTemplate = waypointTemplateIterator.next();
                        }
                    }
                }
                final ControlPoint controlPoint = waypoint.getControlPoint();
                final ControlPointWithMarkConfiguration<MarkConfigurationResponseAnnotation> resultingControlPoint;
                if (controlPoint instanceof Mark) {
                    final Mark mark = (Mark) controlPoint;
                    resultingControlPoint = markConfigurationsByMark.get(mark);
                } else {
                    final ControlPointWithTwoMarks markPair = (ControlPointWithTwoMarks) controlPoint;
                    final MarkConfiguration<MarkConfigurationResponseAnnotation> left = markConfigurationsByMark
                            .get(markPair.getLeft());
                    final MarkConfiguration<MarkConfigurationResponseAnnotation> right = markConfigurationsByMark
                            .get(markPair.getRight());
                    resultingControlPoint = new MarkPairWithConfigurationImpl<>(markPair.getName(), left, right,
                            markPair.getShortName());
                }
                resultingWaypoints.add(new WaypointWithMarkConfigurationImpl<>(resultingControlPoint,
                        waypoint.getPassingInstructions()));
                waypointIndex++;
            }
        }
        return new CourseConfigurationImpl<MarkConfigurationResponseAnnotation>(courseTemplateOrNull, allMarkConfigurations, resultingRoleMapping,
                resultingWaypoints, optionalRepeatablePart, numberOfLapsOrNullIfNoValidCourseTemplateInstance, name, shortName, optionalImageURL);
    }
    
    /**
     * Creates a {@link RegattaMarkConfiguration} for all {@link Mark}s {@link RaceColumn#getAvailableMarks() discovered} across all the
     * regatta's {@link Regatta#getRaceColumns() race columns}. If a {@link Mark} in the regatta references a {@link MarkTemplate}
     * through its {@link Mark#getOriginatingMarkTemplateIdOrNull()} then the {@link RegattaMarkConfiguration#getOptionalMarkTemplate()}
     * will refer to the resolved {@link MarkTemplate}. Likewise, if the {@link Mark#getOriginatingMarkPropertiesIdOrNull()} ID can
     * be resolved to a {@link MarkProperties} object successfully, that object will then be what you get by calling
     * {@link RegattaMarkConfiguration#getOptionalMarkProperties()} on the mark configuration created for that mark.
     */
    private Map<Mark, RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>> createMarkConfigurationsForRegatta(
            Regatta regatta, TrackedRace optionalRace, CourseTemplate courseTemplate) {
        final Map<Mark, RegattaMarkConfiguration<MarkConfigurationResponseAnnotation>> result = new HashMap<>();
        for (RaceColumn raceColumn : regatta.getRaceColumns()) {
            for (Mark mark : raceColumn.getAvailableMarks()) {
                result
                .computeIfAbsent(mark,
                        m -> createMarkConfigurationForRegattaMark(courseTemplate, regatta, optionalRace, m));
            }
        }
        return result;
    }
    
    class CourseTemplateCompatibilityCheckerForCourseBase
            extends CourseTemplateCompatibilityChecker<CourseBase, Mark, Waypoint> {
        public CourseTemplateCompatibilityCheckerForCourseBase(CourseBase course, CourseTemplate courseTemplate) {
            super(course, courseTemplate);
        }

        @Override
        protected MarkRole getMarkRole(Mark markFromRegatta) {
            return resolveMarkRoleByID(getCourse().getAssociatedRoles().get(markFromRegatta), getCourseTemplate());
        }

        @Override
        protected Iterable<Mark> getMarks(Waypoint waypoint) {
            return waypoint.getMarks();
        }

        @Override
        protected Iterable<Waypoint> getWaypoints(CourseBase course) {
            return course.getWaypoints();
        }
    }

    /**
     * Checks compatibility of the {@code course} with the {@code courseTemplate} and return {@code null} if
     * incompatible, or the number of laps if compatible, where {@code -1} is used in case the {@link CourseTemplate}
     * has no {@link CourseTemplate#getRepeatablePart() repeatable part}. See {@link CourseTemplateCompatibilityChecker}
     * for details.
     */
    private Integer isCourseInstanceOfCourseTemplate(CourseBase course, CourseTemplate courseTemplate) {
        return new CourseTemplateCompatibilityCheckerForCourseBase(course, courseTemplate).isCourseInstanceOfCourseTemplate();
    }

    /**
     * Replaces {@link MarkTemplateBasedMarkConfiguration}s in {@code markTemplatesToMarkConfigurationsToReplace}'s
     * value set by {@link MarkPropertiesBasedMarkConfiguration}s which
     * {@link MarkPropertiesBasedMarkConfiguration#getOptionalMarkTemplate() refer} to the respective
     * {@link MarkTemplate} in case a usage of a {@link MarkProperties} object for the respective mark template is
     * found in {@link MarkProperties#getLastUsedMarkRole()} or {@link MarkProperties#getLastUsedMarkTemplate()}.
     * All {@link MarkProperties} delivered from {@link SharedSailingData#getAllMarkProperties(Iterable)}, filtered
     * by the {@code tagsToFilterMarkProperties}, are considered.
     * <p>
     */
    private void replaceTemplateBasedConfigurationCandidatesBySuggestedPropertiesInPlace(
            Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> markTemplatesToMarkConfigurationsToReplace,
            Iterable<String> tagsToFilterMarkProperties, Map<MarkTemplate, MarkRole> associatedRoles) {
        // find candidates for replacement of mark configuration
        final Map<MarkTemplate, MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>> replacementCandidates = markTemplatesToMarkConfigurationsToReplace
                .entrySet().stream().filter(e -> e.getValue() instanceof MarkTemplateBasedMarkConfiguration)
                .collect(Collectors.toMap(s -> s.getKey(), s -> (MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>) s.getValue()));
        final Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> mapWithReplacements = replaceTemplateBasedConfigurationCandidatesBySuggestedProperties(
                replacementCandidates, tagsToFilterMarkProperties, associatedRoles);
        for (final Entry<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> e : mapWithReplacements.entrySet()) {
            markTemplatesToMarkConfigurationsToReplace.put(e.getKey(), e.getValue());
        }
    }

    private Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> replaceTemplateBasedConfigurationCandidatesBySuggestedProperties(
            final Map<MarkTemplate, MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>> replacementCandidates,
            Iterable<String> tagsToFilterMarkProperties, Map<MarkTemplate, MarkRole> associatedRoles) {
        final Map<MarkTemplate, MarkConfiguration<MarkConfigurationResponseAnnotation>> result = new HashMap<>();
        result.putAll(replacementCandidates);
        final Set<MarkProperties> markPropertiesCandidates = new HashSet<>();
        Util.addAll(getSharedSailingData().getAllMarkProperties(tagsToFilterMarkProperties), markPropertiesCandidates);
        // Already included mark properties may not get associated again
        markPropertiesCandidates.removeAll(replacementCandidates.values().stream()
                .map(mp->mp.getOptionalMarkProperties()).filter(v -> v != null).collect(Collectors.toSet()));
        final LastUsageBasedAssociater<MarkProperties, MarkRole> roleBasedAssociater = new LastUsageBasedAssociater<>(
                replacementCandidates.keySet().stream().map(associatedRoles::get).filter(v -> v != null)
                        .collect(Collectors.toSet()));
        for (MarkProperties mp : markPropertiesCandidates) {
            roleBasedAssociater.addUsages(mp, mp.getLastUsedMarkRole());
        }
        final Map<MarkTemplate, MarkProperties> suggestedMappings = new HashMap<>();
        for (Iterator<Entry<MarkTemplate, MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>>> iterator = replacementCandidates.entrySet().iterator(); iterator
                .hasNext();) {
            Entry<MarkTemplate, MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>> entry = iterator.next();
            final MarkRole markRole = associatedRoles.get(entry.getKey());
            if (markRole != null) {
                final MarkProperties bestMatchOrNull = roleBasedAssociater.getBestMatchForT2(markRole);
                if (bestMatchOrNull != null) {
                    suggestedMappings.put(entry.getKey(), bestMatchOrNull);
                    iterator.remove();
                    markPropertiesCandidates.remove(bestMatchOrNull);
                }
            }
        }
        // Trying to map the left over candidates by direct usages of the MarkTemplate with the MarkProperties
        final LastUsageBasedAssociater<MarkProperties, MarkTemplate> templateBasedAssociater = new LastUsageBasedAssociater<>(
                new HashSet<>(replacementCandidates.keySet()));
        for (MarkProperties mp : markPropertiesCandidates) {
            templateBasedAssociater.addUsages(mp, mp.getLastUsedMarkTemplate());
        }
        for (Entry<MarkTemplate, MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>> entry : replacementCandidates.entrySet()) {
            final MarkProperties bestMatchOrNull = templateBasedAssociater.getBestMatchForT2(entry.getKey());
            if (bestMatchOrNull != null) {
                suggestedMappings.put(entry.getKey(), bestMatchOrNull);
            }
        }
        // replace candidates if possible
        for (Map.Entry<MarkTemplate, MarkTemplateBasedMarkConfiguration<MarkConfigurationResponseAnnotation>> entr : replacementCandidates.entrySet()) {
            final MarkTemplate keyTemplate = entr.getKey();
            if (suggestedMappings.containsKey(keyTemplate)) {
                final MarkProperties suggestedPropertiesMapping = suggestedMappings.get(keyTemplate);
                final MarkPropertiesBasedMarkConfiguration<MarkConfigurationResponseAnnotation> newMarkPropertiesBasedConfiguration =
                        new MarkPropertiesBasedMarkConfigurationImpl<>(suggestedPropertiesMapping, keyTemplate,
                                getPositioningIfAvailable(suggestedPropertiesMapping));
                result.put(keyTemplate, newMarkPropertiesBasedConfiguration);
            }
        }
        return result;
    }

    private MarkConfigurationResponseAnnotation getPositioningIfAvailable(Regatta regatta, TrackedRace optionalRace, Mark mark) {
        return CourseConfigurationBuilder.getPositioningIfAvailable(regatta, optionalRace, mark, lastKnownPositionResolver);
    }

    private MarkConfigurationResponseAnnotation getPositioningIfAvailable(MarkProperties markProperties) {
        return CourseConfigurationBuilder.getPositioningIfAvailable(markProperties.getPositioningInformation(), lastKnownPositionResolver);
    }

    @Override
    public List<MarkProperties> createMarkPropertiesSuggestionsForMarkConfiguration(Regatta optionalRegatta,
            MarkConfiguration<MarkConfigurationRequestAnnotation> markConfiguration, Iterable<String> tagsToFilterMarkProperties) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * First tries to resolve from the course template and then by ID using sharedSailingData. This allows users having
     * access to a course template to use all mark templates being included even if they don't have explicit read
     * permissions for those.
     */
    private MarkTemplate resolveMarkTemplateByID(UUID markTemplateID, CourseTemplate courseTemplate) {
        MarkTemplate resolvedMarkTemplate = null;
        if (courseTemplate != null) {
            resolvedMarkTemplate = courseTemplate.getMarkTemplateByIdIfContainedInCourseTemplate(markTemplateID);
        }
        if (resolvedMarkTemplate == null) {
            try {
                resolvedMarkTemplate = getSharedSailingData().getMarkTemplateById(markTemplateID);
            } catch(Exception e) {
                // This call may fail due to missing permissions but should not prevent the user from creating a course for a regatta.
                // This is just the case, a regatta Mark is based on a MarkTemplate that is not part of the associated CourseTemplate.
                // In this case we don't require the MarkTemplate reference to reconstruct a CourseTemplate.
            }
        }
        return resolvedMarkTemplate;
    }

    /**
     * The resulting mark configurations has {@link MarkConfiguration#getOptionalMarkProperties()} set if the {@code mark}
     * has a {@link Mark#getOriginatingMarkPropertiesIdOrNull() mark properties reference} that can be resolved successfully;
     * the {@link MarkConfiguration#getOptionalMarkTemplate()} will be set if the {@code mark} has a
     * {@link Mark#getOriginatingMarkTemplateIdOrNull() mark template reference} that can be resolved successfully.
     */
    private RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> createMarkConfigurationForRegattaMark(CourseTemplate courseTemplate,
            Regatta regatta, TrackedRace optionalRace, Mark mark) {
        final UUID markTemplateIdOrNull = mark.getOriginatingMarkTemplateIdOrNull();
        final MarkTemplate markTemplateOrNull = markTemplateIdOrNull == null ? null : resolveMarkTemplateByID(markTemplateIdOrNull, courseTemplate);
        final UUID markPropertiesIdOrNull = mark.getOriginatingMarkPropertiesIdOrNull();
        final MarkProperties markPropertiesOrNull = markPropertiesIdOrNull == null ? null
                : getSharedSailingData().getMarkPropertiesById(markPropertiesIdOrNull);
        final RegattaMarkConfiguration<MarkConfigurationResponseAnnotation> regattaMarkConfiguration = new RegattaMarkConfigurationImpl<MarkConfigurationResponseAnnotation>(
                mark, getPositioningIfAvailable(regatta, optionalRace, mark), markTemplateOrNull, markPropertiesOrNull);
        return regattaMarkConfiguration;
    }

    /**
     * Provided with a mapping from {@link MarkConfiguration} to {@link MarkRole}, and provided with a waypoint sequence
     * in the form used by a {@link CourseConfiguration}, namely as {@link WaypointWithMarkConfiguration} objects, a
     * course sequence mapper finds a mark-like object of type {@code M} for all {@link MarkConfiguration} objects, as
     * defined by the {@link #getOrCreateMarkReplacement(MarkConfiguration)} method that subclasses have to define. If
     * the original {@link MarkConfiguration} had an {@link #existingRoleMapping existing mapping to a mark role}, that
     * mapping is also stored for the resulting {@code M} object in the {@link #explicitAssociatedRoles} map.
     * <p>
     * 
     * Eventually, a list of {@code W} waypoint-like objects is created, one for each
     * {@link WaypointWithMarkConfiguration} in the original list provided to the constructor, based on the {@code M}
     * objects to which the mark configurations were mapped. The list can be found in {@link #effectiveWaypoints} after
     * the constructor has completed.<p>
     *
     * @param <CP>
     *            a control point-like type; something that is one or has two object(s) of type {@code M}.
     * @param <M>
     *            a mark-like type; something that in particular is something like a control point, hence this type has
     *            to extend the {@code CP} type
     * @param <W>
     *            a waypoint-like type; something that references a control point-like object of type {@code CP} and
     *            adds {@link PassingInstruction passing instructions} information
     */
    private abstract class CourseSequenceMapper<CP, M extends CP, W, P> {
        final Map<M, MarkRole> explicitAssociatedRoles = new HashMap<>();
        final Map<M, MarkRole> allAssociatedRoles = new HashMap<>();
        // TODO should we remove this field and let calculateEffectiveWaypoints return the list instead; this will clean up the somewhat convoluted construction order for subclasses
        final List<W> effectiveWaypoints = new ArrayList<>();
        private final Map<MarkConfiguration<P>, ? extends MarkRole> existingRoleMapping;
        private final Map<MarkConfiguration<P>, M> existingMapping;

        public CourseSequenceMapper(Iterable<WaypointWithMarkConfiguration<P>> waypoints,
                Map<MarkConfiguration<P>, ? extends MarkRole> existingRoleMapping,
                Map<MarkConfiguration<P>, M> existingMapping) {
            this.existingRoleMapping = existingRoleMapping;
            this.existingMapping = existingMapping;
            
            // all pre-existing explicit roles are added to the result
            for (Entry<MarkConfiguration<P>, ? extends MarkRole> entry : existingRoleMapping.entrySet()) {
                explicitAssociatedRoles.put(mapMarkConfiguration(entry.getKey()), entry.getValue());
            }
            allAssociatedRoles.putAll(explicitAssociatedRoles);
            // Cache to allow reusing ControlPointWithTwoMarks objects that are based on the same MarkPairWithConfiguration
            final Map<MarkPairWithConfiguration<P>, CP> markPairCache = new HashMap<>();
            for (WaypointWithMarkConfiguration<P> waypointWithMarkConfiguration : waypoints) {
                final ControlPointWithMarkConfiguration<P> controlPointWithMarkConfiguration = waypointWithMarkConfiguration.getControlPoint();
                if (controlPointWithMarkConfiguration instanceof MarkConfiguration) {
                    final MarkConfiguration<P> markConfiguration = (MarkConfiguration<P>) controlPointWithMarkConfiguration;
                    effectiveWaypoints.add(createWaypoint(mapMarkConfiguration(markConfiguration), waypointWithMarkConfiguration.getPassingInstruction()));
                } else {
                    final CP controlPoint = markPairCache
                            .computeIfAbsent((MarkPairWithConfiguration<P>) controlPointWithMarkConfiguration, mpwc -> {
                                final M left = mapMarkConfiguration(mpwc.getLeft());
                                final M right = mapMarkConfiguration(mpwc.getRight());
                                return createMarkPair(left, right, mpwc.getName(), mpwc.getShortName());
                            });
                    effectiveWaypoints.add(createWaypoint(controlPoint, waypointWithMarkConfiguration.getPassingInstruction()));
                }
            }
        }
        
        private M mapMarkConfiguration(MarkConfiguration<P> markConfiguration) {
            final M mark = getOrCreateMarkReplacement(markConfiguration);
            if (mark == null) {
                throw new IllegalStateException("Non declared mark "+markConfiguration.getName()+" found in waypoint sequence");
            }
            // If an explicit role mapping isn't given -> default to the mark's name
            allAssociatedRoles.computeIfAbsent(mark, m -> existingRoleMapping.get(markConfiguration));
            return mark;
        }
        
        private M getOrCreateMarkReplacement(MarkConfiguration<P> markConfiguration) {
            return existingMapping.get(markConfiguration);
        }
        
        protected abstract CP createMarkPair(M left, M right, String name, String shortName);
        
        protected abstract W createWaypoint(CP controlPoint, PassingInstruction passingInstruction);
    }
    
    private class CourseConfigurationToCourseConfigurationMapper<P> extends
            CourseSequenceMapper<ControlPointWithMarkConfiguration<P>, MarkConfiguration<P>, WaypointWithMarkConfiguration<P>, P> {
        public CourseConfigurationToCourseConfigurationMapper(Iterable<WaypointWithMarkConfiguration<P>> waypoints,
                Map<MarkConfiguration<P>, ? extends MarkRole> existingRoleMapping,
                Map<MarkConfiguration<P>, MarkConfiguration<P>> existingMapping) {
            super(waypoints, existingRoleMapping, existingMapping);
        }

        @Override
        protected MarkPairWithConfiguration<P> createMarkPair(MarkConfiguration<P> left, MarkConfiguration<P> right, String name, String shortName) {
            return new MarkPairWithConfigurationImpl<>(name, left, right, shortName);
        }

        @Override
        protected WaypointWithMarkConfiguration<P> createWaypoint(ControlPointWithMarkConfiguration<P> controlPoint, PassingInstruction passingInstruction) {
            return new WaypointWithMarkConfigurationImpl<>(controlPoint, passingInstruction);
        }
    };
    
    /**
     * When creating course configurations, it is tried to match regatta marks as well as mark properties to mark
     * templates based on the last usage. Because of the fact that a regatta mark or mark properties could be associated
     * to different mark templates or roles historically, it could be the best match for more than one mark template.
     * 
     * Example: A mark M was associated to role R1 in race 1. M was associated to role R2 in race 2. The roles R1 and R2
     * are distinctly used in one of the races. This means M would match both, R1 and R2. For R1 and R2 the only match
     * would be M. In a course template including both R1 and R2 we can't match M to both roles. In this case it is
     * checked if the best match in reverse direction leads to the same role. In this example, the latest (best) match
     * for M is R2. Given that, R1 will not be matched to a mark at all.
     * 
     * In general this means: A match based on last usage is only counted as match if both elements (e.g. Role and mark)
     * reference each other as the only or latest match.
     * 
     * This rule is always applied for at least the following cases:
     * <ul>
     *   <li>Matching regatta marks by usage of their associated roles to mark templates</li>
     *   <li>Matching marks properties by usage of their associated roles to mark templates</li>
     *   <li>Matching marks properties by direct usage for mark templates</li>
     * </ul>
     *
     */
    private class LastUsageBasedAssociater<T1, T2> {
        private final Map<T1, Map<T2, TimePoint>> usagesByT1 = new HashMap<>();
        private final Map<T2, Map<T1, TimePoint>> usagesByT2 = new HashMap<>();
        private final Predicate<T2> t2Filter;
        
        public LastUsageBasedAssociater(Iterable<T2> t2Whitelist) {
            t2Filter = t2 -> Util.contains(t2Whitelist, t2);
        }
        
        public void addUsage(T1 t1, T2 t2, TimePoint lastUsage) {
            if (t2Filter.test(t2)) {
                insertOrUpdateUsage(usagesByT1, t1, t2, lastUsage);
                insertOrUpdateUsage(usagesByT2, t2, t1, lastUsage);
            }
        }
        
        public void addUsages(T1 t1, Map<T2, TimePoint> lastUsages) {
            for (Entry<T2, TimePoint> entry : lastUsages.entrySet()) {
                addUsage(t1, entry.getKey(), entry.getValue());
            }
        }
        
        private <K, V> void insertOrUpdateUsage(Map<K, Map<V, TimePoint>> usages, K key, V value, TimePoint timePoint) {
            Map<V, TimePoint> usagesForKey = usages.computeIfAbsent(key, k -> new HashMap<>());
            usagesForKey.compute(value,
                    (k, currentValue) -> (currentValue == null || timePoint.after(currentValue)) ? timePoint
                            : currentValue);
        }
        
        private <K, V> V getBestMatch(Map<K, Map<V, TimePoint>> forwardUsages, Map<V, Map<K, TimePoint>> backwardUsages, K keyToSearch) {
            V bestMatch = getBestMatchCandidate(forwardUsages, keyToSearch);
            if (bestMatch != null && ! keyToSearch.equals(getBestMatchCandidate(backwardUsages, bestMatch))) {
                // match would be better suited for another key
                bestMatch = null;
            }
            return bestMatch;
        }
        
        private <K, V> V getBestMatchCandidate(Map<K, Map<V, TimePoint>> usages, K keyToSearch) {
            final V result;
            final Map<V, TimePoint> usagesForT1 = usages.get(keyToSearch);
            if (usagesForT1 == null) {
                // No match at all
                result = null;
            } else {
                TimePoint bestMatchTP = null;
                V bestMatch = null;
                for (Map.Entry<V, TimePoint> entry : usagesForT1.entrySet()) {
                    if (bestMatchTP == null || bestMatchTP.after(entry.getValue())) {
                        bestMatchTP = entry.getValue();
                        bestMatch = entry.getKey();
                    } else if (bestMatchTP.compareTo(entry.getValue()) == 0) {
                        // ambiguous match
                        bestMatch = null;
                    }
                }
                result = bestMatch;
            }
            return result;
        }
        
        public T1 getBestMatchForT2(T2 t2) {
            return getBestMatch(usagesByT2, usagesByT1, t2);
        }
        
        private <K, V> void removeT1(T1 t1) {
            remove(usagesByT1, usagesByT2, t1);
        }
        
        private <K, V> void removeT2(T2 t2) {
            remove(usagesByT2, usagesByT1, t2);
        }
        
        private <K, V> void remove(Map<K, Map<V, TimePoint>> forwardUsages, Map<V, Map<K, TimePoint>> backwardUsages, K keyToRemove) {
            final Map<V, TimePoint> associatedUses = forwardUsages.remove(keyToRemove);
            if (associatedUses != null) {
                for (V associatedValue : associatedUses.keySet()) {
                    final Map<K, TimePoint> usesForValue = backwardUsages.get(associatedValue);
                    if (usesForValue != null) {
                        usesForValue.remove(keyToRemove);
                        if (usesForValue.isEmpty()) {
                            backwardUsages.remove(associatedValue);
                        }
                    }
                }
            }
        }
    }
}
