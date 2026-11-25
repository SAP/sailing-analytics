package com.sap.sailing.gwt.ui.server;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.UpdateCourseAreas;
import com.sap.sse.security.SecurityService;

public class CourseAreaUpdatePermissionTest {
    private final RacingEventService service = mock(RacingEventService.class);
    private final SecurityService security = mock(SecurityService.class);
    private final CourseArea area = new CourseAreaImpl("Alpha", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null);
    private final SailingServiceWriteImpl rpc = mock(SailingServiceWriteImpl.class, CALLS_REAL_METHODS);

    public CourseAreaUpdatePermissionTest() {
        doReturn(service).when(rpc).getService();
        doReturn(security).when(rpc).getSecurityService();
    }

    @Test
    public void requiresUpdatePermissionForEveryEventSharingAnArea() {
        final Event edited = event();
        final Event shared = event();
        when(service.getAllEvents()).thenReturn(Arrays.asList(edited, shared));
        doThrow(new UnauthorizedException()).when(security).checkCurrentUserUpdatePermission(shared);
        assertThrows(UnauthorizedException.class, () -> rpc.updateCourseAreas(edited.getId(),
                Collections.singletonList(new CourseAreaDTO(area.getId(), "Renamed"))));
        verify(service, never()).apply(any(UpdateCourseAreas.class));
    }

    @Test
    public void updateOnlyUserCanEditAttributes() {
        final Event edited = event();
        when(service.getAllEvents()).thenReturn(Collections.singletonList(edited));
        rpc.updateCourseAreas(edited.getId(), Collections.singletonList(new CourseAreaDTO(area.getId(), "Renamed")));
        verify(service).apply(any(UpdateCourseAreas.class));
        verify(security, never()).checkCurrentUserDeletePermission(any(Event.class));
    }

    private Event event() {
        final Event event = mock(Event.class);
        final Venue venue = mock(Venue.class);
        final UUID id = UUID.randomUUID();
        when(event.getId()).thenReturn(id);
        when(event.getVenue()).thenReturn(venue);
        when(venue.getCourseAreas()).thenReturn(Collections.singletonList(area));
        when(service.getEvent(id)).thenReturn(event);
        return event;
    }
}
