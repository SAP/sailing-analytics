package com.sap.sailing.gwt.home.communication.event;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.gwt.common.client.EventWindFinderUtil;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.communication.eventview.SeriesReferenceWithEventsDTO;
import com.sap.sailing.gwt.home.server.EventActionUtil;
import com.sap.sailing.gwt.home.server.EventActionUtil.LeaderboardCallback;
import com.sap.sailing.gwt.home.server.LeaderboardContext;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.gwt.ui.shared.TrackingConnectorInfoDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.dispatch.shared.caching.IsClientCacheable;
import com.sap.sse.security.ui.server.SecurityDTOUtil;
import com.sap.sse.shared.media.ImageDescriptor;

/**
 * <p>
 * {@link SailingAction} implementation to load the basic logo, name, state, date and navigation information for a
 * {@link #GetEventViewAction(UUID) given event-id} to be shown on several pages of this event.
 * </p>
 */
public class GetEventViewAction implements SailingAction<EventViewDTO>, IsClientCacheable {
    private static final Logger logger = Logger.getLogger(GetEventViewAction.class.getName());

    private UUID eventId;
    
    @SuppressWarnings("unused")
    private GetEventViewAction() {
    }

    /**
     * Creates a {@link GetEventViewAction} instance for the given event-id.
     * 
     * @param eventId
     *            {@link UUID} of the {@link Event} to load data for
     */
    public GetEventViewAction(UUID eventId) {
        this.eventId = eventId;
    }
    
    /* (non-Javadoc)
     * @see com.sap.sailing.gwt.home.communication.SailingAction#execute(com.sap.sailing.gwt.home.communication.SailingDispatchContext)
     */
    @GwtIncompatible
    public EventViewDTO execute(SailingDispatchContext context) {
        final Event event = context.getRacingEventService().getEvent(eventId);
        if (event == null) {
            throw new RuntimeException("Event not found");
        }
        context.getSecurityService().checkCurrentUserReadPermission(event);
        final EventViewDTO dto = new EventViewDTO();
        HomeServiceUtil.mapToMetadataDTO(event, dto);
        ImageDescriptor logoImage = event.findImageWithTag(MediaTagConstants.LOGO.getName());
        dto.setLogoImage(logoImage != null ? HomeServiceUtil.convertToImageDTO(logoImage) : null);
        dto.setOfficialWebsiteURL(event.getOfficialWebsiteURL() == null ? null : event.getOfficialWebsiteURL().toString());
        URL sailorsInfoWebsiteURL = event.getSailorsInfoWebsiteURLOrFallback(context.getClientLocale());
        dto.setSailorsInfoWebsiteURL(sailorsInfoWebsiteURL == null ? null : sailorsInfoWebsiteURL.toString());
        if (context.getWindFinderTrackerFactory() != null) {
            dto.setAllWindFinderSpotsUsedByEvent(new EventWindFinderUtil().getWindFinderSpotsToConsider(event,
                    context.getWindFinderTrackerFactory(), /* useCachedSpotsForTrackedRaces */ true));
        }
        dto.setHasMedia(HomeServiceUtil.hasMedia(event));
        dto.setState(HomeServiceUtil.calculateEventState(event));
        // after extending EventViewDTO with SecureDTO a name is needed.
        dto.setName(event.getName());
        // bug2982: always show leaderboard and competitor analytics 
        dto.setHasAnalytics(true);
        String description = event.getDescription();
        if (description == null || description.trim().isEmpty() || event.getName().equalsIgnoreCase(description)) {
            // If a description isn't useful, it should not be shown in the UI
            description = null;
        }
        dto.setDescription(description);
        final Set<RegattaMetadataDTO> regattasOfEvent = new HashSet<>();
        final Set<LeaderboardGroup> relevantLeaderboardGroupsOfEvent = new HashSet<>();
        EventActionUtil.forLeaderboardsOfEventWithReadPermissions(context, event, new LeaderboardCallback() {
            @Override
            public void doForLeaderboard(LeaderboardContext lcontext) {
                try {
                    Util.addAll(lcontext.getLeaderboardGroups(), relevantLeaderboardGroupsOfEvent);
                    RegattaMetadataDTO regattaDTO = lcontext.asRegattaMetadataDTO();
                    regattasOfEvent.add(regattaDTO);
                    dto.getRegattas().add(regattaDTO);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Catched exception while reading data for leaderboard " + lcontext.getLeaderboardName(), e);
                }
            }
        });
        dto.setMultiRegatta(regattasOfEvent.size() != 1);
        if (relevantLeaderboardGroupsOfEvent.size() == 1) {
            final LeaderboardGroup singleLeaderboardGroup = relevantLeaderboardGroupsOfEvent.iterator().next();
            if (singleLeaderboardGroup.hasOverallLeaderboard()) {
                final List<EventAndLeaderboardReferenceWithStateDTO> eventAndLeaderboardReferencesForSeriesOrdered = HomeServiceUtil
                        .getEventAndLeaderboardReferencesForSeriesOrdered(singleLeaderboardGroup,
                                context.getRacingEventService());
                Collections.reverse(eventAndLeaderboardReferencesForSeriesOrdered);
                dto.setSeriesData(new SeriesReferenceWithEventsDTO(
                        HomeServiceUtil.getLeaderboardDisplayName(singleLeaderboardGroup),
                        singleLeaderboardGroup.getId(), eventAndLeaderboardReferencesForSeriesOrdered));
            }
        }
        Set<TrackingConnectorInfoDTO> trackingConnectorInfos = event.getTrackingConnectorInfos().stream()
                .map(TrackingConnectorInfoDTO::new).collect(Collectors.toSet());
        dto.setTrackingConnectorInfos(trackingConnectorInfos);
        SecurityDTOUtil.addSecurityInformation(context.getSecurityService(), dto);
        return dto;
    }

    @Override
    public void cacheInstanceKey(StringBuilder key) {
        key.append(eventId);
    }
}
