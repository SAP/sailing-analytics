package com.sap.sailing.shared.server;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.coursetemplate.CommonMarkProperties;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.FixedPositioning;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.Positioning;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.shared.server.impl.ReplicatingSharedSailingData;
import com.sap.sailing.shared.server.impl.SharedSailingDataImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.RepeatablePart;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.ReplicableWithObjectInputStream;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.UserGroup;

/**
 * {@link com.sap.sailing.server.interfaces.RacingEventService RacingEventService} is scoped to one server only. To
 * share data that is not exclusively bound to one specific server but relevant for several servers,
 * {@link SharedSailingData} provides an alternative replication scope. This means {@link SharedSailingData} may be
 * replicated in a similar way as {@link SecurityService}.<br>
 * {@link SharedSailingData} encapsulates persistence, replication and security aspects for the following domain types:
 * <ul>
 * <li>{@link MarkTemplate}s</li>
 * <li>{@link MarkProperties}</li>
 * <li>{@link CourseTemplate}s</li>
 * </ul>
 * 
 * In particular, the reading methods that produce iterables of entities, such as {@link #getAllMarkRoles()} or
 * {@link #getAllMarkTemplates()}, will deliver a view restricted by the availability of the {@link DefaultActions#READ}
 * permission of the user on whose behalf the request is being executed.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface SharedSailingData extends ReplicableWithObjectInputStream<ReplicatingSharedSailingData, OperationWithResult<ReplicatingSharedSailingData, ?>> {
    String REPLICABLE_FULLY_QUALIFIED_CLASSNAME = SharedSailingDataImpl.class.getName();

    Iterable<MarkProperties> getAllMarkProperties(Iterable<String> tagsToFilterFor);
    
    Iterable<MarkTemplate> getAllMarkTemplates();
    
    Iterable<CourseTemplate> getAllCourseTemplates(Iterable<String> tagsToFilterFor);
    
    Iterable<MarkRole> getAllMarkRoles();
    
    MarkRole createMarkRole(String name, String shortName);
    
    MarkRole getMarkRoleById(UUID id);

    /**
     * @param optionalNonDefaultGroupOwnership
     *            if {@link Optional#isPresent() present}, defines the {@link UserGroup} to use for the new mark
     *            properties object's group ownership. Otherwise, the session user's default creation group ownership
     *            will be used for the current server.
     */
    MarkProperties createMarkProperties(CommonMarkProperties properties, Iterable<String> tags, Optional<UserGroup> optionalNonDefaultGroupOwnership);
    
    MarkProperties updateMarkProperties(UUID uuid, CommonMarkProperties properties, Iterable<String> tags);

    /**
     * @param positioningInformation
     *            if {@code null}, no update will be performed to the positioning information of the
     *            {@link MarkProperties} object identified by {@code uuid}. To clear the positioning information, pass
     *            in a valid, non-{@code null} {@link Positioning} object that makes an "empty" specification, such as a
     *            {@link FixedPositioning} with a {@code null} {@link FixedPositioning#getFixedPosition()} return.
     */
    MarkProperties updateMarkProperties(UUID uuid, CommonMarkProperties properties, Positioning positioningInformation, Iterable<String> tags);

    /**
     * Removes any positioning information from the {@code markProperties} object, regardless of what the positioning was before
     * (device-based or fixed position or empty);
     */
    void clearPositioningForMarkProperties(MarkProperties markProperties);
    
    /**
     * This overrides a previously set fixed position or associated tracking device.
     * 
     * @param position
     *            must not be {@code null}; use {@link #clearPositioningForMarkProperties} if you'd like to remove any
     *            positioning assignment from the {@link MarkProperties} object altogether.
     */
    void setFixedPositionForMarkProperties(MarkProperties markProperties, Position position);
    
    MarkProperties getMarkPropertiesById(UUID id);
    
    /**
     * This overrides a previously set fixed position or associated tracking device.
     * 
     * @param deviceIdentifier
     *            must not be {@code null}; use {@link #clearPositioningForMarkProperties} if you'd like to remove any
     *            positioning assignment from the {@link MarkProperties} object altogether.
     */
    void setTrackingDeviceIdentifierForMarkProperties(MarkProperties markProperties, DeviceIdentifier deviceIdentifier);
    
    MarkTemplate createMarkTemplate(CommonMarkProperties properties);
    
    MarkTemplate getMarkTemplateById(UUID id);
    
    /**
     * @param waypoints the waypoints in their defined order (iteration order equals order of waypoints in course)
     */
    CourseTemplate createCourseTemplate(String courseTemplateName, String courseTemplateShortName,
            Iterable<MarkTemplate> marks, Iterable<WaypointTemplate> waypoints,
            Map<MarkTemplate, MarkRole> associatedRoles, Map<MarkRole, MarkTemplate> defaultMarkTemplatesForMarkRoles,
            RepeatablePart optionalRepeatablePart, Iterable<String> tags, URL optionalImageURL, Integer defaultNumberOfLaps);
    
    CourseTemplate getCourseTemplateById(UUID id);
    
    /**
     * Records the fact that the {@code markProperties} were used to configure a mark based on a 
     * {@code markTemplate}. Keeps the {@link MillisecondsTimePoint#now() current time} of this call which will be
     * returned for {@code markTemplate} when invoking {@link #getUsedMarkProperties(MarkTemplate)}.
     */
    void recordUsage(MarkTemplate markTemplate, MarkProperties markProperties);

    /**
     * Records the fact that the {@code markProperties} were used to configure a mark that takes the given role name.
     * Keeps the {@link MillisecondsTimePoint#now() current time} of this call which will be returned for the role name
     * when invoking {@link #getUsedMarkProperties(String)}.
     */
    void recordUsage(MarkProperties markProperties, MarkRole markRole);
    
    /**
     * Returns the time points when {@link MarkProperties} objects were {@link #recordUsage(MarkTemplate, MarkProperties) last used}
     * for the {@link MarkTemplate} passed in the {@code markTemplate} parameter.
     */
    Map<MarkProperties, TimePoint> getUsedMarkProperties(MarkTemplate markTemplate);
    
    /**
     * Returns the time points when {@link MarkProperties} objects were {@link #recordUsage(MarkProperties, String) last used}
     * for the role name passed in the {@code roleName} parameter.
     */
    Map<MarkProperties, TimePoint> getUsedMarkProperties(MarkRole roleName);
    
    void deleteMarkProperties(MarkProperties markProperties);

    void deleteCourseTemplate(CourseTemplate courseTemplate);

    Iterable<MarkProperties> getAllMarkProperties();

    Iterable<CourseTemplate> getAllCourseTemplates();

    CourseTemplate updateCourseTemplate(UUID uuid, String name, String shortName, URL optionalImageURL, ArrayList<String> tags,
            Integer defaultNumberOfLaps);
}
