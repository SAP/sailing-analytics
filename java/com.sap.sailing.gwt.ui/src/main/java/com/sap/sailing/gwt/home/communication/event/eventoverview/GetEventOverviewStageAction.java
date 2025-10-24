package com.sap.sailing.gwt.home.communication.event.eventoverview;

import static com.sap.sailing.gwt.server.HomeServiceUtil.findEventThumbnailImageUrlAsString;
import static com.sap.sailing.gwt.server.HomeServiceUtil.getStageImageURLAsString;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.dispatch.shared.caching.IsClientCacheable;
import com.sap.sse.gwt.dispatch.shared.commands.ResultWithTTL;
import com.sap.sse.shared.media.VideoDescriptor;

/**
 * <p>
 * {@link SailingAction} implementation to load data to be shown on the event overview page for the
 * {@link #GetEventOverviewStageAction(UUID, boolean) given event-id}, where the prepared data depends on the
 * {@link HomeServiceUtil#calculateEventState(event) event's state} and video and image availability.
 * </p>
 * <p>
 * The {@link ResultWithTTL result's} time to live is the <i>time until start</i> for upcoming or planned events and
 * <i>2 minutes</i> for currently running events, otherwise a duration of <i>5 minutes</i> is used.
 * </p>
 */
public class GetEventOverviewStageAction implements SailingAction<ResultWithTTL<EventOverviewStageDTO>>, IsClientCacheable {
    
    private UUID eventId;
    private boolean useTeaserImage;
    
    @SuppressWarnings("unused")
    private GetEventOverviewStageAction() {
    }

    /**
     * Creates a {@link GetEventOverviewStageAction} instance for the given event-id, where the use of stage or teaser
     * images can be specified.
     * 
     * @param eventId
     *            {@link UUID} of the {@link Event} to load data for
     * @param useTeaserImage
     *            <code>true</code> to use a lower resolution teaser image for the event, <code>false</code> to use a
     *            high resolution stage image. Can be useful to safe data traffic on mobile connections.
     */
    public GetEventOverviewStageAction(UUID eventId, boolean useTeaserImage) {
        this.eventId = eventId;
        this.useTeaserImage = useTeaserImage;
    }
    
    @Override
    @GwtIncompatible
    public ResultWithTTL<EventOverviewStageDTO> execute(SailingDispatchContext context) {
        final TimePoint now = MillisecondsTimePoint.now();
        final Event event = context.getRacingEventService().getEvent(eventId);
        final EventState state = HomeServiceUtil.calculateEventState(event);
        long ttl = Duration.ONE_MINUTE.times(5).asMillis();
        if(state == EventState.RUNNING) {
            ttl = Duration.ONE_MINUTE.times(2).asMillis();
        }
        if (isUpcomingOrPlanned(event, state)) {
            ttl = Math.min(ttl, now.until(event.getStartDate()).asMillis());
        }
        
        // TODO get correct message
        EventOverviewStageDTO stage = new EventOverviewStageDTO(null, getStageContent(context, event, state, now));
        return new ResultWithTTL<>(new MillisecondsDurationImpl(ttl), stage);
    }

    @GwtIncompatible
    public EventOverviewStageContentDTO getStageContent(SailingDispatchContext context, Event event, EventState state, TimePoint now) {
        // P1: Featured video if available
        List<String> videoTags = Collections.singletonList(MediaTagConstants.FEATURED.getName());
        VideoDescriptor featuredVideo = HomeServiceUtil.getStageVideo(event, context.getClientLocale(), videoTags , false);
        if (featuredVideo != null) {
            return new EventOverviewVideoStageDTO(EventOverviewVideoStageDTO.Type.MEDIA,
                    HomeServiceUtil.toVideoDTO(featuredVideo));
        }
        
        // P2: Featured image if available
        String imageUrl = HomeServiceUtil.getFeaturedImageUrlAsString(event);
        if (imageUrl == null) {
            // P3: Show Teaser/Stage image
            imageUrl = useTeaserImage ? findEventThumbnailImageUrlAsString(event) : getStageImageURLAsString(event);
        }
        
        // Show countdown for planned or upcoming events
        if (isUpcomingOrPlanned(event, state)) {
            return new EventOverviewTickerStageDTO(event.getStartDate().asDate(), event.getName(), imageUrl);
        }
        return new EventOverviewTickerStageDTO(null, event.getName(), imageUrl);
        
        // TODO do the full implementation
        
//        String stageImageUrl = HomeServiceUtil.getStageImageURLAsString(event);
//        if(state == EventState.RUNNING && TODO live video) {
//          return new EventOverviewVideoStageDTO(Type.LIVESTREAM);
//      }
        
//        if(TODO highlight video) {
//          return new EventOverviewVideoStageDTO(Type.HIGHLIGHTS);
//      }
        
//        if(state == EventState.RUNNING) {
//            NextRaceFinder nextRaceFinder = new NextRaceFinder();
//            RacesActionUtil.forRacesOfEvent(context, eventId, nextRaceFinder);
//            
//            RaceContext nextRace = nextRaceFinder .getNextRace();
//            if(nextRace != null) {
//                return new EventOverviewRaceTickerStageDTO(nextRace.getRaceIdentifier(), nextRace.getStageText(), nextRaceFinder.getNextStartTime().asDate(), stageImageUrl);
//            }
//            
//            // TODO This is not a good idea due to Axel and Frank as regatta start times aren't correctly set.
//            Regatta nextRegatta = null;
//            for (LeaderboardGroup lg : event.getLeaderboardGroups()) {
//                for (Leaderboard lb : lg.getLeaderboards()) {
//                    if(lb instanceof RegattaLeaderboard) {
//                        Regatta regatta = ((RegattaLeaderboard) lb).getRegatta();
//                        if (regatta.getStartDate() != null
//                                && now.before(regatta.getStartDate())
//                                && (nextRegatta == null || nextRegatta.getStartDate().after(regatta.getStartDate()))) {
//                            nextRegatta = regatta;
//                        }
//                    }
//                }
//            }
//            if(!HomeServiceUtil.isSingleRegatta(event) && nextRegatta != null) {
//                return new EventOverviewRegattaTickerStageDTO(
//                        new RegattaName(nextRegatta.getName()), nextRegatta.getName(), nextRegatta.getStartDate()
//                                .asDate(), stageImageUrl);
//            }
//        }
//        
//        if(state == EventState.UPCOMING || state == EventState.PLANNED) {
//            return new EventOverviewTickerStageDTO(event.getStartDate()
//                    .asDate(), event.getName(), stageImageUrl);
//        }
//        
//        List<URL> photoGalleryImageURLs = HomeServiceUtil.getPhotoGalleryImageURLs(event);
//        if(photoGalleryImageURLs.size() >= 3) {
//            // TODO image gallery
//        }
//        
//        return new EventOverviewTickerStageDTO(null, null, stageImageUrl);
    }

    @GwtIncompatible
    private boolean isUpcomingOrPlanned(Event event, EventState state) {
        final TimePoint startDate = event.getStartDate();
        return startDate != null && (state == EventState.UPCOMING || state == EventState.PLANNED);
    }

    @Override
    public void cacheInstanceKey(StringBuilder key) {
        key.append(eventId).append("_").append(useTeaserImage);
    }
}
