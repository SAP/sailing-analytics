package com.sap.sailing.gwt.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.gwt.dispatch.client.exceptions.DispatchException;
import com.sap.sailing.gwt.dispatch.client.exceptions.ServerDispatchException;
import com.sap.sailing.gwt.home.communication.event.EventMetadataDTO;
import com.sap.sailing.gwt.home.communication.event.EventReferenceDTO;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.eventlist.EventListEventDTO;
import com.sap.sailing.gwt.home.communication.media.SailingVideoDTO;
import com.sap.sailing.gwt.home.communication.start.EventStageDTO;
import com.sap.sailing.gwt.home.communication.start.StageEventType;
import com.sap.sailing.gwt.ui.shared.media.MediaConstants;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.MediaDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

public final class HomeServiceUtil {
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
    
    public static boolean isFakeSeries(EventBase event) {
        Iterator<? extends LeaderboardGroupBase> lgIter = event.getLeaderboardGroups().iterator();
        if(!lgIter.hasNext()) {
            return false;
        }
        LeaderboardGroupBase lg = lgIter.next();
        if(lgIter.hasNext()) {
            return false;
        }
        return lg.hasOverallLeaderboard();
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
        TimePoint now = MillisecondsTimePoint.now();
        if (now.before(event.getStartDate())) {
            return event.isPublic() ? EventState.UPCOMING : EventState.PLANNED;
        }
        if (now.after(event.getEndDate())) {
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
        return event.findImageWithTag(MediaTagConstants.TEASER);
    }
    
    public static ImageDescriptor getFeaturedImage(EventBase event) {
        return event.findImageWithTag(MediaTagConstants.FEATURED);
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
        return event.findImageWithTag(MediaTagConstants.STAGE);
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
        return event.findImagesWithTag(MediaTagConstants.GALLERY);
    }
    
    public static List<ImageDescriptor> getSailingLovesPhotographyImages(EventBase event) {
        final List<ImageDescriptor> acceptedImages = new LinkedList<>();
        for (ImageDescriptor candidateImageUrl : event.getImages()) {
            if (candidateImageUrl.hasSize() && candidateImageUrl.getHeightInPx() > MINIMUM_IMAGE_HEIGHT_FOR_SAILING_PHOTOGRAPHY_IN_PIXELS) {
                if (candidateImageUrl.hasTag(MediaTagConstants.STAGE) || candidateImageUrl.hasTag(MediaTagConstants.GALLERY)) {
                    acceptedImages.add(candidateImageUrl);
                }
            }
        }
        return acceptedImages;
    }

    public static int calculateCompetitorsCount(Leaderboard sl) {
        return Util.size(sl.getCompetitors());
    }
    
    public static int calculateRaceCount(Leaderboard sl) {
        int nonCarryForwardRacesCount = 0;
        for (RaceColumn column : sl.getRaceColumns()) {
            if (!column.isCarryForward()) {
                nonCarryForwardRacesCount += Util.size(column.getFleets());
            }
        }
        return nonCarryForwardRacesCount;
    }
    
    public static int calculateRaceColumnCount(Leaderboard sl) {
        int nonCarryForwardRacesCount = 0;
        for (RaceColumn rc : sl.getRaceColumns()) {
            nonCarryForwardRacesCount += rc.isCarryForward() ? 0 : 1;
        }
        return nonCarryForwardRacesCount;
    }
    
    public static int calculateTrackedRaceCount(Leaderboard sl) {
        int count=0;
        for (RaceColumn column : sl.getRaceColumns()) {
            for (Fleet fleet : column.getFleets()) {
                TrackedRace trackedRace = column.getTrackedRace(fleet);
                if(trackedRace != null && trackedRace.hasGPSData() && trackedRace.hasWindData()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    public static int calculateTrackedRaceColumnCount(Leaderboard sl) {
        int count=0;
        for (RaceColumn column : sl.getRaceColumns()) {
            for (Fleet fleet : column.getFleets()) {
                TrackedRace trackedRace = column.getTrackedRace(fleet);
                if(trackedRace != null && trackedRace.hasGPSData() && trackedRace.hasWindData()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
    
    public static String getBoatClassName(Leaderboard leaderboard) {
        BoatClass boatClass = getBoatClass(leaderboard);
        return boatClass == null ? null : boatClass.getName();
    }

    private static BoatClass getBoatClass(Leaderboard leaderboard) {
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
        return event.hasImageWithTag(MediaTagConstants.GALLERY);
    }
    
    public static boolean hasVideos(Event event) {
        return !Util.isEmpty(event.getVideos());
    }

    public static boolean isPartOfEvent(EventBase event, Leaderboard regattaEntity) {
        for (CourseArea courseArea : event.getVenue().getCourseAreas()) {
            if(courseArea.equals(regattaEntity.getDefaultCourseArea())) {
                return true;
            }
        }
        return false;
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
            if(!MediaConstants.SUPPORTED_VIDEO_TYPES.contains(videoCandidate.getMimeType())) {
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
        mapToMetadataDTO(event, dto, service);
        dto.setBaseURL(baseURL.toString());
        dto.setOnRemoteServer(onRemoteServer);
        dto.setStageType(stageType);
        dto.setStageImageURL(useTeaserImage ? findEventThumbnailImageUrlAsString(event) : getStageImageURLAsString(event));
        return dto;
    }
    
    public static EventListEventDTO convertToEventListDTO(EventBase event, URL baseURL, boolean onRemoteServer, RacingEventService service) {
        EventListEventDTO dto = new EventListEventDTO();
        mapToMetadataDTO(event, dto, service);
        dto.setBaseURL(baseURL.toString());
        dto.setOnRemoteServer(onRemoteServer);
        return dto;
    }
    
    public static EventMetadataDTO convertToMetadataDTO(EventBase event, RacingEventService service) {
        EventMetadataDTO dto = new EventMetadataDTO();
        mapToMetadataDTO(event, dto, service);
        return dto;
    }
    
    public static void mapToMetadataDTO(EventBase event, EventMetadataDTO dto, RacingEventService service) {
        dto.setId((UUID) event.getId());
        dto.setDisplayName(getEventDisplayName(event, service));
        dto.setStartDate(event.getStartDate().asDate());
        dto.setEndDate(event.getEndDate().asDate());
        dto.setState(HomeServiceUtil.calculateEventState(event));
        dto.setVenue(event.getVenue().getName());
        if(HomeServiceUtil.isFakeSeries(event)) {
            dto.setLocation(getLocation(event, service));
        }
        dto.setThumbnailImageURL(HomeServiceUtil.findEventThumbnailImageUrlAsString(event));
    }
    
    public static String getEventDisplayName(EventBase event, RacingEventService service) {
        if(isFakeSeries(event)) {
            String seriesName = getSeriesName(event);
            if(seriesName != null) {
                String location = getLocation(event, service);
                if(location != null) {
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
    
    public static String getLocation(EventBase eventBase, RacingEventService service) {
        if(!(eventBase instanceof Event)) {
            return null;
        }
        Event event = (Event) eventBase;
        for (Leaderboard leaderboard : event.getLeaderboardGroups().iterator().next().getLeaderboards()) {
            if(HomeServiceUtil.isPartOfEvent(event, leaderboard)) {
                return leaderboard.getDisplayName() != null ? leaderboard.getDisplayName() : leaderboard.getName();
            }
        }
        return null;
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
    
    public static TimePoint getLiveTimePoint() {
        return new MillisecondsTimePoint(getLiveTimePointInMillis());
    }
    
    public static long getLiveTimePointInMillis() {
        // TODO better solution
        long livePlayDelayInMillis = 15_000;
        return System.currentTimeMillis() - livePlayDelayInMillis;
    }
    
    public static String getCourseAreaNameForRegattaIdThereIsMoreThanOne(EventBase event, Leaderboard leaderboard) {
        /** The course area will not be shown if there is only one course area defined for the event */
        if (Util.size(event.getVenue().getCourseAreas()) <= 1) {
            return null;
        }
        CourseArea courseArea = null;
        if (leaderboard instanceof FlexibleLeaderboard) {
            courseArea = ((FlexibleLeaderboard) leaderboard).getDefaultCourseArea();
        } else if(leaderboard instanceof RegattaLeaderboard) {
            Regatta regatta = ((RegattaLeaderboard) leaderboard).getRegatta();
            if (regatta != null) {
                courseArea = regatta.getDefaultCourseArea();
            }
        }
        return courseArea == null ? null : courseArea.getName();
    }
    
    public static String getCourseAreaIdForRegatta(EventBase event, Leaderboard leaderboard) {
        CourseArea courseArea = null;
        if (leaderboard instanceof FlexibleLeaderboard) {
            courseArea = ((FlexibleLeaderboard) leaderboard).getDefaultCourseArea();
        } else if(leaderboard instanceof RegattaLeaderboard) {
            Regatta regatta = ((RegattaLeaderboard) leaderboard).getRegatta();
            if (regatta != null) {
                courseArea = regatta.getDefaultCourseArea();
            }
        }
        return courseArea == null ? null : courseArea.getId().toString();
    }
    
    public static void forAllPublicEvents(RacingEventService service, HttpServletRequest request,
            EventVisitor... visitors) throws DispatchException {
        URL requestedBaseURL = getRequestBaseURL(request);
        for (Event event : service.getAllEvents()) {
            if(event.isPublic()) {
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
                    if(remoteEvent.isPublic()) {
                        for(EventVisitor visitor : visitors) {
                            visitor.visit(remoteEvent, true, baseURL);
                        }
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
            throw new ServerDispatchException(e);
        }
    }

    public static boolean hasRegattaData(EventBase event) {
        final boolean fakeSeries = HomeServiceUtil.isFakeSeries(event);
        for (LeaderboardGroupBase leaderboardGroupBase : event.getLeaderboardGroups()) {
            if(leaderboardGroupBase instanceof LeaderboardGroup) {
                // for events that are locally available, we can see if there are any leaderboards
                LeaderboardGroup leaderboardGroup = (LeaderboardGroup) leaderboardGroupBase;
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    if(!fakeSeries || isPartOfEvent(event, leaderboard)) {
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
}
