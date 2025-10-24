package com.sap.sailing.gwt.home.communication.fakeseries;

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO.EventSeriesState;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sse.common.Util;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.dispatch.shared.caching.IsClientCacheable;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;
import com.sap.sse.shared.media.ImageDescriptor;

/**
 * <p>
 * {@link SailingAction} implementation to load data to be shown in the series list overview for the
 * {@link #GetEventSeriesViewAction(UUID) given series-id}, preparing the appropriate data structure.
 * </p>
 */
public class GetEventSeriesViewAction implements SailingAction<EventSeriesViewDTO>, IsClientCacheable {

    private UUID seriesUUIDOrNull;
    private UUID leaderboardGroupUUIDOrNull;

    @SuppressWarnings("unused")
    private GetEventSeriesViewAction() {
    }

    public GetEventSeriesViewAction(UUID leaderboardGroupUUID) {
        super();
        this.leaderboardGroupUUIDOrNull = leaderboardGroupUUID;
        if(leaderboardGroupUUID == null) {
            throw new RuntimeException("leaderboardgroupid is known");
        }
    }

    
    /**
     * Creates a {@link GetEventSeriesViewAction} instance for the given series-id or leaderboardGroupId
     * 
     * @param id
     *            {@link UUID} of the series to load data for
     */
    public GetEventSeriesViewAction(SeriesContext ctx) {
        super();
        this.leaderboardGroupUUIDOrNull = ctx.getLeaderboardGroupId();
        if(leaderboardGroupUUIDOrNull == null) {
            if (ctx.getSeriesId() == null) {
                throw new RuntimeException("invalid context, neither seriesid not leaderboardgroupid is known");
            }
            this.seriesUUIDOrNull = ctx.getSeriesId();
        }
    }

    @Override
    @GwtIncompatible
    public EventSeriesViewDTO execute(SailingDispatchContext ctx) throws DispatchException {
        LeaderboardGroup leaderBoardGroup;
        Event o;
        if (leaderboardGroupUUIDOrNull != null) {
            leaderBoardGroup = ctx.getRacingEventService().getLeaderboardGroupByID(leaderboardGroupUUIDOrNull);
            if (leaderBoardGroup == null) {
                throw new RuntimeException("LeaderboardGroup not found");
            }
            o = HomeServiceUtil.determineBestMatchingEvent(ctx.getRacingEventService(), leaderBoardGroup);
        } else {
            // legacy code for old links
            o = ctx.getRacingEventService().getEvent(seriesUUIDOrNull);
            if (o == null) {
                throw new RuntimeException("Series not found");
            }
            if (Util.size(o.getLeaderboardGroups()) != 1) {
                throw new RuntimeException("Could not map event to LeaderboardGroup");
            }
            leaderBoardGroup = o.getLeaderboardGroups().iterator().next();
        }

        if (!leaderBoardGroup.hasOverallLeaderboard()) {
            throw new RuntimeException("Is not overall leaderboard");
        }

        EventSeriesViewDTO dto = new EventSeriesViewDTO();
        dto.setLeaderboardGroupUUID(leaderBoardGroup.getId());
        ImageDescriptor logoImage = o.findImageWithTag(MediaTagConstants.LOGO.getName());
        dto.setLogoImage(logoImage != null ? HomeServiceUtil.convertToImageDTO(logoImage) : null);
        // TODO implement correctly. We currently do not show media for series.
        dto.setHasMedia(false);

        boolean oneEventStarted = false;
        boolean oneEventLive = false;
        boolean allFinished = true;
        dto.setDisplayName(leaderBoardGroup.getDisplayName() != null ? leaderBoardGroup.getDisplayName()
                : leaderBoardGroup.getName());

        if (leaderBoardGroup.getOverallLeaderboard() != null) {
            dto.setLeaderboardId(leaderBoardGroup.getOverallLeaderboard().getName());
        }

        for (Event eventInSeries : HomeServiceUtil.getEventsForSeriesOrdered(leaderBoardGroup,
                ctx.getRacingEventService())) {
            EventMetadataDTO eventOfSeries = HomeServiceUtil.convertToMetadataDTO(eventInSeries);
            dto.addEvent(eventOfSeries);

            oneEventStarted |= eventOfSeries.isStarted();
            oneEventLive |= (eventOfSeries.isStarted() && !eventOfSeries.isFinished());
            allFinished &= eventOfSeries.isFinished();
        }
        
        HomeServiceUtil.getEventAndLeaderboardReferencesForSeriesOrdered(leaderBoardGroup,
                ctx.getRacingEventService()).forEach(dto::addEventAndLeaderboard);
        
        if (oneEventLive) {
            dto.setState(EventSeriesState.RUNNING);
        } else if (!oneEventStarted) {
            dto.setState(EventSeriesState.UPCOMING);
        } else if (allFinished) {
            dto.setState(EventSeriesState.FINISHED);
        } else {
            dto.setState(EventSeriesState.IN_PROGRESS);
        }

        dto.setHasAnalytics(oneEventStarted);
        return dto;
    }

    @Override
    public void cacheInstanceKey(StringBuilder key) {
        key.append(leaderboardGroupUUIDOrNull);
        key.append(seriesUUIDOrNull);
    }

}
