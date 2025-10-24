package com.sap.sailing.server.gateway.jaxrs.sharing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.UriInfo;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.WithDescription;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.Named;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.shared.media.ImageDescriptor;

public class HomeSharingUtils {
    private static final String HTML_COMMENT_START = "<!--";
    private static final String HTML_COMMENT_END = "-->";
    private static final String REPLACEMENT_KEY_REDIRECT_URL_FALLBACK = "redirect_url_fallback";
    private static final String REPLACEMENT_KEY_IMAGE = "image";
    private static final String REPLACEMENT_KEY_REDIRECT_URL = "redirect_url";
    private static final String REPLACEMENT_KEY_DISPLAY_URL = "display_url";
    private static final String REPLACEMENT_KEY_DESCRIPTION = "description";
    private static final String REPLACEMENT_KEY_TITLE = "title";
    private static final Logger logger = Logger.getLogger(HomeSharingUtils.class.getName());
    private static final String DEFAULT_TEASER_URL = "https://s3-eu-west-1.amazonaws.com/media.sapsailing.com/2014/505Worlds/Images_Homepage/505Worlds2014_eventteaser.jpg";
    private static final String SHARED_PROXY_RESOURCE = "/SharedProxy.html";
    private static final String DEFAULT_TITLE = "SAP Sailing";
    private static final String DEFAULT_DESCRIPTION = "Help sailors analyze performance and optimize strategy &#8226; Bring fans closer to the action "
            + "&#8226; Provide the media with information and insights to deliver a greater informed commentary";
    private static final String REPLACEMENT_KEY_DISABLE_REDIRECT_START = "disable_redirect_start";
    private static final String REPLACEMENT_KEY_DISABLE_REDIRECT_END = "disable_redirect_end";

    protected static String loadSharingHTML(ClassLoader classLoader, UriInfo uri) {
        try (InputStream stream = classLoader.getResourceAsStream(SHARED_PROXY_RESOURCE)) {
            String content = readInputStreamToString(stream);
            return content;
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not process or read resource " + uri.getPath(true), e);
            return null;
        }
    }

    private static String readInputStreamToString(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        in.close();
        buffer.flush();
        String content = buffer.toString();
        buffer.close();
        return content;
    }

    protected static String replaceMetatags(String content, Map<String, String> replacementMap) {
        String modifiedContent = new String(content);
        for (Map.Entry<String, String> item : replacementMap.entrySet()) {
            modifiedContent = modifiedContent.replace("${" + item.getKey() + "}", item.getValue());
        }
        return modifiedContent;
    }

    /**
     * 
     * @param event
     * @return Returns a URL pointing to an image for the event. The first image found is returned in the following
     *         order: 1. Teaser Image 2. Stage Image 3. Default Image (505 Worlds 2014 Teaser)
     */
    protected static String findTeaserImageUrl(Event event) {
         String thumbnailUrl = findEventSpecificTeaserImage(event);
        if(!(thumbnailUrl == null)) {
            return thumbnailUrl;
        }else {
            return DEFAULT_TEASER_URL;
        }
    }

    private static String findEventSpecificTeaserImage(Event event) {
        ImageDescriptor findImageWithTag = event.findImageWithTag(MediaTagConstants.TEASER.getName());
        if (!(findImageWithTag == null)) {
            return findImageWithTag.getURL().toString();
        } else {
            findImageWithTag = event.findImageWithTag(MediaTagConstants.STAGE.getName());
            if (!(findImageWithTag == null)) {
                return findImageWithTag.getURL().toString();
            }
        }
        return null;
    }
    
    protected static String findTeaserImageUrl(LeaderboardGroup leaderboardGroup, RacingEventService eventService) {
        String thumbnailUrl = DEFAULT_TEASER_URL;
        final List<Event> eventsForSeriesOrdered = getEventsInSeries(leaderboardGroup, eventService);
        if(!eventsForSeriesOrdered.isEmpty()) {
            final Event event = eventsForSeriesOrdered.get(eventsForSeriesOrdered.size() -1);
            final String specificTeaserImageUrl = findEventSpecificTeaserImage(event);
            if(specificTeaserImageUrl != null) {
                thumbnailUrl = specificTeaserImageUrl;
            }
        }
        return thumbnailUrl;
    }

    protected static String findDescription(final WithDescription withDescription) {
        String description = withDescription.getDescription();
        if (description == null || description.equals("")) {
            description = DEFAULT_DESCRIPTION;
        }
        return description;
    }

    protected static String findTitle(Named named) {
        String name = named.getName();
        if (name == null) {
            name = DEFAULT_TITLE;
        }
        return name;
    }

    public static Map<String, String> createReplacementMap(String title, String description,
            String imageUrl, String placeUrl, String userAgent) {
        final Map<String, String> replacementMap = new HashMap<String, String>();
        final String disabledStart;
        final String disabledEnd;
        if (userAgent != null && userAgent.contains("facebookexternalhit")) {
            disabledStart = HTML_COMMENT_START;
            disabledEnd = HTML_COMMENT_END;
        } else {
            disabledStart = "";
            disabledEnd = "";
        }
        replacementMap.put(REPLACEMENT_KEY_TITLE, title);
        replacementMap.put(REPLACEMENT_KEY_DESCRIPTION, description);
        replacementMap.put(REPLACEMENT_KEY_DISPLAY_URL, placeUrl);
        replacementMap.put(REPLACEMENT_KEY_IMAGE, imageUrl);
        replacementMap.put(REPLACEMENT_KEY_REDIRECT_URL, placeUrl);
        replacementMap.put(REPLACEMENT_KEY_DISABLE_REDIRECT_START, disabledStart);
        replacementMap.put(REPLACEMENT_KEY_DISABLE_REDIRECT_END, disabledEnd);
        replacementMap.put(REPLACEMENT_KEY_REDIRECT_URL_FALLBACK, placeUrl);
        return replacementMap;
    }
    
    /**
     * The given {@link LeaderboardGroup} needs to be one that is used to define a {@link Event} series (e.g. ESS or
     * Bundesliga). In this case, multiple {@link Event Events} reference the same {@link LeaderboardGroup}. This method
     * calculates all Events that are associated to the given {@link LeaderboardGroup}.
     */
    private static List<Event> getEventsInSeries(LeaderboardGroup overallLeaderboardGroup,
            RacingEventService service) {
        List<Event> eventsInSeries = new ArrayList<>();
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
}
