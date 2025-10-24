package com.sap.sailing.gwt.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.gwt.home.communication.event.EventAndLeaderboardReferenceWithStateDTO;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.home.communication.start.EventStageDTO;
import com.sap.sailing.gwt.home.communication.start.StageEventType;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sailing.gwt.ui.shared.EventLinkDTO;
import com.sap.sailing.gwt.ui.shared.EventReferenceDTO;
import com.sap.sailing.gwt.ui.shared.TrackingConnectorInfoDTO;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.util.EventUtil;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;
import com.sap.sse.gwt.dispatch.shared.exceptions.ServerDispatchException;
import com.sap.sse.security.SecurityService;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.MediaDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

public final class HomeServiceUtil {
    private static final Logger logger = Logger.getLogger(HomeServiceUtil.class.getName());

    public interface EventVisitor {
        void visit(EventBase event, boolean onRemoteServer, URL baseURL);
    }
    
    private HomeServiceUtil() {
    }

    private static final int MINIMUM_IMAGE_HEIGHT_FOR_SAILING_PHOTOGRAPHY_IN_PIXELS = 500;
    
    public static String findEventThumbnailImageUrlAsString(EventBase event) {
        ImageDescriptor url = findEventThumbnailImage(event);
        return url == null ? null : url.getURL().toString();
    }
    
    public static boolean isSingleRegatta(Event event) {
        boolean first = true;
        for(LeaderboardGroup lg : event.getLeaderboardGroups()) {
            for(@SuppressWarnings("unused") Leaderboard lb: lg.getLeaderboards()) {
                if(!first) {
                    return false;
                }
                first = false;
            }
        }
        return true;
    }
    
    public static EventState calculateEventState(EventBase event) {
        final TimePoint startDate = event.getStartDate();
        if (startDate == null) {
            return EventState.PLANNED;
        }
        final TimePoint now = MillisecondsTimePoint.now();
        if (now.before(startDate)) {
            return event.isPublic() ? EventState.UPCOMING : EventState.PLANNED;
        }
        final TimePoint endDate = event.getEndDate();
        if (endDate != null && now.after(endDate)) {
            return EventState.FINISHED;
        }
        return EventState.RUNNING;
    }
    
    public static VideoDTO toVideoDTO(VideoDescriptor video) {
        VideoDTO videoDTO = new VideoDTO(video.getURL().toString(), video.getMimeType(), video.getCreatedAtDate().asDate());
        fillVideoDTOFields(video, videoDTO);
        return videoDTO;
    }
    
    public static SailingVideoDTO toSailingVideoDTO(EventReferenceDTO eventRef, VideoDescriptor video) {
        SailingVideoDTO videoDTO = new SailingVideoDTO(eventRef, video.getURL().toString(), video.getMimeType(), video.getCreatedAtDate().asDate());
        fillVideoDTOFields(video, videoDTO);
        return videoDTO;
    }

    private static void fillVideoDTOFields(VideoDescriptor video, VideoDTO videoDTO) {
        videoDTO.setTitle(video.getTitle());
        videoDTO.setSubtitle(video.getSubtitle());
        videoDTO.setTags(video.getTags());
        videoDTO.setCopyright(video.getCopyright());
        videoDTO.setLocale(video.getLocale() != null ? video.getLocale().toString() : null);
        videoDTO.setLengthInSeconds(video.getLengthInSeconds());
        videoDTO.setThumbnailRef(video.getThumbnailURL() != null ? video.getThumbnailURL().toString(): null);
    }
    
    private static ImageDescriptor findEventThumbnailImage(EventBase event) {
        return event.findImageWithTag(MediaTagConstants.TEASER.getName());
    }
    
    public static ImageDescriptor getFeaturedImage(EventBase event) {
        return event.findImageWithTag(MediaTagConstants.FEATURED.getName());
    }
    
    public static String getFeaturedImageUrlAsString(EventBase event) {
        ImageDescriptor image = getFeaturedImage(event);
        return image == null ? null : image.getURL().toString();
    }
    
    public static String getStageImageURLAsString(final EventBase event) {
        ImageDescriptor image = getStageImage(event);
        return image == null ? null : image.getURL().toString();
    }
    
    public static ImageDescriptor getStageImage(final EventBase event) {
        return event.findImageWithTag(MediaTagConstants.STAGE.getName());
    }

    public static List<String> getPhotoGalleryImageURLsAsString(EventBase event) {
        List<ImageDescriptor> urls = getPhotoGalleryImages(event);
        List<String> result = new ArrayList<String>(urls.size());
        for (ImageDescriptor url : urls) {
            result.add(url.getURL().toString());
        }
        return result;
    }

    public static List<ImageDescriptor> getPhotoGalleryImages(EventBase event) {
        return event.findImagesWithTag(MediaTagConstants.GALLERY.getName());
    }
    
    public static List<ImageDescriptor> getSailingLovesPhotographyImages(EventBase event) {
        final List<ImageDescriptor> acceptedImages = new LinkedList<>();
        for (ImageDescriptor candidateImageUrl : event.getImages()) {
            if (candidateImageUrl.hasSize() && candidateImageUrl.getHeightInPx() > MINIMUM_IMAGE_HEIGHT_FOR_SAILING_PHOTOGRAPHY_IN_PIXELS) {
                if (candidateImageUrl.hasTag(MediaTagConstants.STAGE.getName()) || candidateImageUrl.hasTag(MediaTagConstants.GALLERY.getName())) {
                    acceptedImages.add(candidateImageUrl);
                }
            }
        }
        return acceptedImages;
    }

    public static int calculateCompetitorsCount(Leaderboard sl) {
        return Util.size(sl.getCompetitors());
    }
    
    public static String getBoatClassName(Leaderboard leaderboard) {
        BoatClass boatClass = getBoatClass(leaderboard);
        return boatClass == null ? null : boatClass.getName();
    }

    public static BoatClass getBoatClass(Leaderboard leaderboard) {
        if(leaderboard instanceof RegattaLeaderboard) {
            RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
            BoatClass boatClassFromRegatta = regattaLeaderboard.getRegatta().getBoatClass();
            if(boatClassFromRegatta != null) {
                return boatClassFromRegatta;
            }
        }
        return getBoatClassFromTrackedRaces(leaderboard);
    }

    private static BoatClass getBoatClassFromTrackedRaces(Leaderboard leaderboard) {
        for (TrackedRace trackedRace : leaderboard.getTrackedRaces()) {
            return trackedRace.getRace().getBoatClass();
        }
        return null;
    }

    public static boolean hasMedia(Event event) {
        return hasVideos(event) || hasPhotos(event);
    }
    
    public static boolean hasPhotos(Event event) {
        return event.hasImageWithTag(MediaTagConstants.GALLERY.getName());
    }
    
    public static boolean hasVideos(Event event) {
        return !Util.isEmpty(event.getVideos());
    }
    
    public static VideoDescriptor getRandomVideo(Iterable<VideoDescriptor> urls) {
        if(Util.isEmpty(urls)) {
            return null;
        }
        int size = Util.size(urls);
        return Util.get(urls, new Random(size).nextInt(size));
    }
    
    public static VideoDescriptor getStageVideo(Event event, Locale locale, Collection<String> rankedTags, boolean acceptOtherTags) {
        VideoDescriptor bestMatch = null;
        
        for (VideoDescriptor videoCandidate : event.getVideos()) {
            if(!MediaTagConstants.SUPPORTED_VIDEO_TYPES.contains(videoCandidate.getMimeType())) {
                continue;
            }
            
            if(!acceptOtherTags && !hasOneTag(videoCandidate, rankedTags)) {
                continue;
            }
            
            LocaleMatch localeMatch = matchLocale(videoCandidate, locale);
            if(localeMatch == LocaleMatch.NO_MATCH) {
                continue;
            }
            
            if(bestMatch == null) {
                bestMatch = videoCandidate;
                continue;
            }
            
            int compareByTag = compareByTag(videoCandidate, bestMatch, rankedTags);
            if(compareByTag > 0 || (compareByTag == 0 && isBetter(videoCandidate, bestMatch, locale))) {
                bestMatch = videoCandidate;
                continue;
            }
        }
        return bestMatch;
    }
    
    private static int compareByTag(VideoDescriptor videoCandidate, VideoDescriptor bestMatch,
            Collection<String> rankedTags) {
        for(String rankedTag : rankedTags) {
            boolean hasTag = hasTag(videoCandidate, rankedTag);
            boolean hasTagBestMatch = hasTag(bestMatch, rankedTag);
            if(hasTag != hasTagBestMatch) {
                return hasTag ? 1 : -1;
            }
        }
        return 0;
    }

    private static boolean isBetter(VideoDescriptor candidate, VideoDescriptor reference, Locale locale) {
        LocaleMatch localeMatch = matchLocale(candidate, locale);
        LocaleMatch localeMatchRef = matchLocale(reference, locale);
        if(localeMatch != localeMatchRef) {
            return localeMatch.compareTo(localeMatchRef) < 0 ? true : false;
        }
        
        // TODO filter by length
        
        return candidate.getCreatedAtDate().compareTo(reference.getCreatedAtDate()) > 0;
    }
    
    private static boolean hasTag(MediaDescriptor videoCandidate, String tag) {
        return Util.contains(videoCandidate.getTags(), tag);
    }

    private static boolean hasOneTag(MediaDescriptor videoCandidate, Collection<String> acceptedTags) {
        for(String tag : videoCandidate.getTags()) {
            if(acceptedTags.contains(tag)) {
                return true;
            }
        }
        return false;
    }
    
    private enum LocaleMatch {
        PERFECT, NOT_TAGGED, EN_FALLBACK, NO_MATCH
    }

    private static LocaleMatch matchLocale(VideoDescriptor videoCandidate, Locale locale) {
        Locale localeOfCandidate = videoCandidate.getLocale();
        if(localeOfCandidate == null) {
            return LocaleMatch.NOT_TAGGED;
        }
        if(videoCandidate.getLocale().equals(locale)) {
            return LocaleMatch.PERFECT;
        }
        if(videoCandidate.getLocale().equals(Locale.ENGLISH)) {
            return LocaleMatch.EN_FALLBACK;
        }
        return LocaleMatch.NO_MATCH;
    }
    
    public static EventStageDTO convertToEventStageDTO(EventBase event, URL baseURL, boolean onRemoteServer, StageEventType stageType, RacingEventService service, boolean useTeaserImage) {
        EventStageDTO dto = new EventStageDTO();
        mapToMetadataDTO(event, dto);
        dto.setBaseURL(baseURL.toString());
        dto.setOnRemoteServer(onRemoteServer);
        dto.setStageType(stageType);
        Set<TrackingConnectorInfoDTO> trackingConnectorInfos = event.getTrackingConnectorInfos().stream()
                .map(TrackingConnectorInfoDTO::new).collect(Collectors.toSet());
        dto.setTrackingConnectorInfos(trackingConnectorInfos);
        dto.setStageImageURL(useTeaserImage ? findEventThumbnailImageUrlAsString(event) : getStageImageURLAsString(event));
        return dto;
    }
    
    public static EventListEventDTO convertToEventListDTO(EventBase event, URL baseURL, boolean onRemoteServer) {
        EventListEventDTO dto = new EventListEventDTO();
        mapToMetadataDTO(event, dto);
        dto.setBaseURL(String.valueOf(baseURL));
        dto.setOnRemoteServer(onRemoteServer);
        return dto;
    }
    
    public static EventMetadataDTO convertToMetadataDTO(EventBase event) {
        EventMetadataDTO dto = new EventMetadataDTO();
        mapToMetadataDTO(event, dto);
        return dto;
    }
    
    public static EventLinkDTO convertToEventLinkDTO(EventBase event, URL baseURL, boolean onRemoteServer) {
        EventLinkDTO dto = new EventLinkDTO();
        mapToReferenceDTO(event, dto);
        dto.setBaseURL(String.valueOf(baseURL));
        dto.setOnRemoteServer(onRemoteServer);
        return dto;
    }
    
    public static void mapToMetadataDTO(EventBase event, EventMetadataDTO dto) {
        mapToReferenceDTO(event, dto);
        dto.setStartDate(event.getStartDate() == null ? null : event.getStartDate().asDate());
        dto.setEndDate(event.getEndDate() == null ? null : event.getEndDate().asDate());
        dto.setState(HomeServiceUtil.calculateEventState(event));
        dto.setVenue(event.getVenue().getName());
        if (EventUtil.isFakeSeries(event)) {
            dto.setLocation(getLocation(event));
        }
        dto.setThumbnailImageURL(HomeServiceUtil.findEventThumbnailImageUrlAsString(event));
    }
    
    private static void mapToReferenceDTO(EventBase event, EventReferenceDTO dto) {
        dto.setId((UUID) event.getId());
        dto.setDisplayName(getEventDisplayName(event));
    }

    public static String getEventDisplayName(EventBase event) {
        if (EventUtil.isFakeSeries(event)) {
            String seriesName = getSeriesName(event);
            if (seriesName != null) {
                String location = getLocation(event);
                if (location != null) {
                    return seriesName + " - " + location;
                }
            }
        }
        return event.getName();
    }

    public static String getSeriesName(EventBase event) {
        LeaderboardGroupBase overallLeaderboardGroup = event.getLeaderboardGroups().iterator().next();
        return getLeaderboardDisplayName(overallLeaderboardGroup);
    }

    public static String getLeaderboardDisplayName(LeaderboardGroupBase overallLeaderboardGroup) {
        return overallLeaderboardGroup.getDisplayName() != null ? overallLeaderboardGroup.getDisplayName() : overallLeaderboardGroup.getName();
    }
    
    public static String getLocation(EventBase eventBase) {
        if(!(eventBase instanceof Event)) {
            return null;
        }
        final Event event = (Event) eventBase;
        String displayNameOfSingleAssociatedRegatta = null;
        for (Leaderboard leaderboard : event.getLeaderboardGroups().iterator().next().getLeaderboards()) {
            if (leaderboard.isPartOfEvent(event)) {
                if (displayNameOfSingleAssociatedRegatta != null) {
                    // more than one Regatta is associated to the specific event
                    return null;
                }
                displayNameOfSingleAssociatedRegatta = getLocation(event, leaderboard);
            }
        }
        return displayNameOfSingleAssociatedRegatta;
    }
    
    public static String getLocation(Event eventBase, Leaderboard leaderboard) {
        return leaderboard.getDisplayName() != null ? leaderboard.getDisplayName() : leaderboard.getName();
    }
    
    public static ImageDTO convertToImageDTO(ImageDescriptor image) {
        ImageDTO result = new ImageDTO(image.getURL().toString(), image.getCreatedAtDate() != null ? image.getCreatedAtDate().asDate() : null);
        result.setCopyright(image.getCopyright());
        result.setTitle(image.getTitle());
        result.setSubtitle(image.getSubtitle());
        result.setMimeType(image.getMimeType());
        result.setSizeInPx(image.getWidthInPx(), image.getHeightInPx());
        result.setLocale(image.getLocale() != null ? image.getLocale().toString() : null);
        List<String> tags = new ArrayList<String>();
        for(String tag: image.getTags()) {
            tags.add(tag);
        }
        result.setTags(tags);
        return result;
    }
    
    public static String getCourseAreaNameForRegattaIfThereIsMoreThanOne(EventBase event, Leaderboard leaderboard) {
        /** The course area will not be shown if there is only one course area defined for the event */
        final String result;
        if (Util.size(event.getVenue().getCourseAreas()) <= 1) {
            result = null;
        } else {
            final Iterable<CourseArea> courseAreas = leaderboard.getCourseAreas();
            result = Util.isEmpty(courseAreas) ? null : Util.join(", ", courseAreas);
        }
        return result;
    }
    
    public static Iterable<String> getCourseAreaIdsAsStringsForRegatta(EventBase event, Leaderboard leaderboard) {
        return Util.map(leaderboard.getCourseAreas(), ca->ca.getId().toString());
    }
    
    public static void forAllPublicEventsWithReadPermission(RacingEventService service, HttpServletRequest request,
            SecurityService securityService,
            EventVisitor... visitors) throws DispatchException {
        URL requestedBaseURL = getRequestBaseURL(request);
        for (Event event : service.getAllEvents()) {
            if (event.isPublic() && securityService.hasCurrentUserReadPermission(event)) {
                for(EventVisitor visitor : visitors) {
                    visitor.visit(event, false, requestedBaseURL);
                }
            }
        }
        for (Entry<RemoteSailingServerReference, Pair<Iterable<EventBase>, Exception>> serverRefAndEventsOrException :
            service.getPublicEventsOfAllSailingServers().entrySet()) {
            final Pair<Iterable<EventBase>, Exception> eventsOrException = serverRefAndEventsOrException.getValue();
            final RemoteSailingServerReference serverRef = serverRefAndEventsOrException.getKey();
            final Iterable<EventBase> remoteEvents = eventsOrException.getA();
            URL baseURL = getBaseURL(serverRef.getURL());
            if (remoteEvents != null) {
                for (EventBase remoteEvent : remoteEvents) {
                    // Those events have been publicly advertised and have been received by an unauthenticated
                    // anonymous call to the /events end point to the remote server. No security checks or filtering
                    // seems necessary here because we should be seeing only the public remote events anyway.
                    // See also bug 5000.
                    for (EventVisitor visitor : visitors) {
                        visitor.visit(remoteEvent, true, baseURL);
                    }
                }
            }
        }
    }

    /**
     * Determines the base URL (protocol, host and port parts) used for the currently executing servlet request. Defaults
     * to <code>http://sapsailing.com</code>.
     * @throws MalformedURLException 
     */
    public static URL getRequestBaseURL(HttpServletRequest request) throws DispatchException {
        URL url;
        try {
            url = new URL(request.getRequestURL().toString());
            final URL baseURL = getBaseURL(url);
            return baseURL;
        } catch (MalformedURLException e) {
            throw new ServerDispatchException(e);
        }
    }

    private static URL getBaseURL(URL url) throws DispatchException {
        try {
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), /* file */"");
        } catch (MalformedURLException e) {
            ServerDispatchException dispatchException = new ServerDispatchException(e);
            logger.log(Level.SEVERE, "Uncaught server exception id: " + dispatchException.getExceptionId(), e);
            throw dispatchException;
        }
    }

    public static boolean hasRegattaData(EventBase event) {
        final boolean fakeSeries = EventUtil.isFakeSeries(event);
        for (LeaderboardGroupBase leaderboardGroupBase : event.getLeaderboardGroups()) {
            if(leaderboardGroupBase instanceof LeaderboardGroup) {
                // for events that are locally available, we can see if there are any leaderboards
                LeaderboardGroup leaderboardGroup = (LeaderboardGroup) leaderboardGroupBase;
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    if(!fakeSeries || leaderboard.isPartOfEvent(event)) {
                        return true;
                    }
                }
            } else {
                // we can't know if the event has leaderboards but the existence of a leaderboard group is a good sign for that
                return true;
            }
        }
        return false;
    }
    
    /**
     * Provides the list of {@link Event}s for a series based on the given overall {@link LeaderboardGroup} in a
     * descending order sorted by the {@link Event#getStartDate() event's start date}.
     * 
     * @param overallLeaderboardGroup the series overall {@link LeaderboardGroup}
     * @param service {@link RacingEventService}
     * @return the {@link Event}s for the series in descending od
     */
    public static List<Event> getEventsForSeriesInDescendingOrder(LeaderboardGroup overallLeaderboardGroup,
            RacingEventService service) {
        List<Event> eventsForSeriesOrdered = getEventsForSeriesOrdered(overallLeaderboardGroup, service);
        Collections.reverse(eventsForSeriesOrdered);
        return eventsForSeriesOrdered;
    }
    
    /**
     * Provides the list of {@link Event}s for a series based on the given overall {@link LeaderboardGroup} in an
     * order that matches the order of {@link Leaderboard Leaderboards} in the {@link LeaderboardGroup}.
     * 
     * @param overallLeaderboardGroup the series overall {@link LeaderboardGroup}
     * @param service {@link RacingEventService}
     * @return the {@link Event}s for the series
     */
    public static List<Event> getEventsForSeriesOrdered(LeaderboardGroup overallLeaderboardGroup,
            RacingEventService service) {
        return getEventsAndLeaderboardsForSeriesOrdered(overallLeaderboardGroup, service).stream()
                .map(pair -> pair.getA()).distinct().collect(Collectors.toList());
    }
    
    /**
     * Provides the list of {@link Event Events} and {@link Leaderboard Leaderboards} for a series based on the given
     * overall {@link LeaderboardGroup} in an order that matches the order of {@link Leaderboard Leaderboards} in the
     * {@link LeaderboardGroup}.
     * 
     * @param overallLeaderboardGroup
     *            the series overall {@link LeaderboardGroup}
     * @param service
     *            {@link RacingEventService}
     * @return the {@link Event} and {@link Leaderboard} pairs for the series
     */
    public static List<Pair<Event, Leaderboard>> getEventsAndLeaderboardsForSeriesOrdered(LeaderboardGroup overallLeaderboardGroup,
            RacingEventService service) {
        final Iterable<Event> eventsInSeries = getEventsInSeries(overallLeaderboardGroup, service);
        final Iterable<Leaderboard> orderedLeaderboards = getLeaderboardsForSeriesInOrderWithReadPermissions(overallLeaderboardGroup,
                service);
        final List<Pair<Event, Leaderboard>> orderedEventsInSeries = new ArrayList<>();
        for (Leaderboard leaderboard : orderedLeaderboards) {
            final Event associatedEvent = getAssociatedEventForLeaderboardInSeries(leaderboard, eventsInSeries);
            if (associatedEvent != null) {
                orderedEventsInSeries.add(new Pair<>(associatedEvent, leaderboard));
            }
        }
        return orderedEventsInSeries;
    }

    /**
     * The {@link Leaderboard Leaderboards} referenced in the given {@link LeaderboardGroup} have a defined order. If
     * the displayGroupsInReverseOrder flag is set for the {@link LeaderboardGroup}, the order needs to change in the
     * UI. This methods sorts the {@link Leaderboard Leaderboards} using this flag.
     * @param service 
     */
    public static Iterable<Leaderboard> getLeaderboardsForSeriesInOrderWithReadPermissions(LeaderboardGroup overallLeaderboardGroup, RacingEventService service) {
        if (overallLeaderboardGroup.isDisplayGroupsInReverseOrder()) {
            List<Leaderboard> leaderboardsInSeries = new ArrayList<>();
            for(Leaderboard leaderboard: overallLeaderboardGroup.getLeaderboards()) {
                if (service.getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                    if (leaderboard instanceof RegattaLeaderboard) {
                        if (service.getSecurityService()
                                .hasCurrentUserReadPermission(((RegattaLeaderboard) leaderboard).getRegatta())) {
                            leaderboardsInSeries.add(leaderboard);
                        }
                    } else {
                        leaderboardsInSeries.add(leaderboard);
                    }
                }
            }
            Collections.reverse(leaderboardsInSeries);
            return leaderboardsInSeries;
        }
        return overallLeaderboardGroup.getLeaderboards();
    }

    private static Event getAssociatedEventForLeaderboardInSeries(Leaderboard leaderboard,
        Iterable<Event> eventsInSeries) {
        for (Event event : eventsInSeries) {
            if (Util.containsAny(event.getVenue().getCourseAreas(), leaderboard.getCourseAreas())) {
                return event;
            }
        }
        return null;
    }

    public static List<EventAndLeaderboardReferenceWithStateDTO> getEventAndLeaderboardReferencesForSeriesOrdered(
            LeaderboardGroup overallLeaderboardGroup, RacingEventService service) {
        final ArrayList<EventAndLeaderboardReferenceWithStateDTO> eventsOfSeries = new ArrayList<>();
        for (Pair<Event, Leaderboard> eventAndLeaderboardInSeries : getEventsAndLeaderboardsForSeriesOrdered(
                overallLeaderboardGroup, service)) {
            final Event eventInSeries = eventAndLeaderboardInSeries.getA();
            final Leaderboard leaderboardInSeries = eventAndLeaderboardInSeries.getB();
            String displayName = HomeServiceUtil.getLocation(eventInSeries, leaderboardInSeries);
            if (displayName == null) {
                displayName = eventInSeries.getName();
            }
            final EventState eventState = HomeServiceUtil.calculateEventState(eventInSeries);
            eventsOfSeries.add(new EventAndLeaderboardReferenceWithStateDTO(eventInSeries.getId(),
                    leaderboardInSeries.getName(), displayName, eventState));
        }
        return eventsOfSeries;
    }

    /**
     * The given {@link LeaderboardGroup} needs to be one that is used to define a {@link Event} series (e.g. ESS or
     * Bundesliga). In this case, multiple {@link Event Events} reference the same {@link LeaderboardGroup}. This method
     * calculates all Events that are associated to the given {@link LeaderboardGroup}.
     */
    private static Iterable<Event> getEventsInSeries(LeaderboardGroup overallLeaderboardGroup,
            RacingEventService service) {
        Set<Event> eventsInSeries = new HashSet<>();
        for (Event event : service.getAllEvents()) {
            if (service.getSecurityService().hasCurrentUserReadPermission(event)) {
                for (LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
                    if (service.getSecurityService().hasCurrentUserReadPermission(leaderboardGroup)) {
                        if (overallLeaderboardGroup.equals(leaderboardGroup)) {
                            eventsInSeries.add(event);
                        }
                    }
                }
            }
        }
        return eventsInSeries;
    }
    
    /**
     * Determines the Event for a LeaderboardGroup, if an exact 1-1 match exists this is returned,
     * else the newes 1-1 is returned. If that does not exist also, the newest n-1 match is returned
     */
    public static Event determineBestMatchingEvent(RacingEventService service, LeaderboardGroup leaderBoardGroup) {
        List<Event> events = new ArrayList<>(
                HomeServiceUtil.getEventsForSeriesOrdered(leaderBoardGroup, service));
        Collections.sort(events, new Comparator<Event>() {

            @Override
            public int compare(Event o1, Event o2) {
                boolean o1GroupPerfectMatch = Util.size(o1.getLeaderboardGroups()) == 1;
                boolean o2GroupPerfectMatch = Util.size(o2.getLeaderboardGroups()) == 1;
                int result = Boolean.compare(o1GroupPerfectMatch, o2GroupPerfectMatch);
                if (result == 0) {
                    TimePoint o1Start = o1.getStartDate();
                    if (o1Start == null) {
                        o1Start = TimePoint.BeginningOfTime;
                    }
                    TimePoint o2Start = o2.getStartDate();
                    if (o2Start == null) {
                        o2Start = TimePoint.BeginningOfTime;
                    }
                    result = o1Start.compareTo(o2Start);
                }
                return result;
            }
        });
        return events.get(0);
    }
}
