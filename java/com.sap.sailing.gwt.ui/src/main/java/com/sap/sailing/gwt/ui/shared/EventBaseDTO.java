package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.WithID;
import com.sap.sse.common.media.ImageSize;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.security.shared.dto.NamedDTO;

/**
 * Basic event information as a DTO. The inherited {@link NamedDTO#equals(Object)} and {@link NamedDTO#hashCode()}
 * methods that are based on the {@link NamedDTO#getName()} response are overridden here to be based on this event's
 * {@link #getId() ID}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class EventBaseDTO extends NamedDTO implements WithID, IsSerializable {
    private static final long serialVersionUID = 818666323178097939L;

    private VenueDTO venue;
    public Date startDate;
    public Date endDate;
    public boolean isPublic;
    public UUID id;

    private String description;
    private List<? extends LeaderboardGroupBaseDTO> leaderboardGroups;
    private String officialWebsiteURL;
    private Map<String, String> sailorsInfoWebsiteURLs;
    private List<SailingImageDTO> images = new ArrayList<>();
    private List<SailingVideoDTO> videos = new ArrayList<>();

    /**
     * For the image URL keys holds the sizes of these images if known. An image size is "known" by this object if it
     * was provided to the {@link #setImageSize} method.
     */
    private Map<String, ImageSize> imageSizes;

    /**
     * The base URL for the server instance on which the data for this event can be reached. Could be something like
     * <code>http://sapsailing.com</code> for archived events that will forever remain in the archive, or
     * <code>http://danishleague2014.sapsailing.com</code> for other events that may not yet be archived or may change
     * servers at any time in the future, therefore requiring a dedicated stable URL that the Apache server can resolve
     * to the correct host IP and Java server instance.
     */
    private String baseURL;

    /**
     * Indicates whether the event is hosted on a remote server or not
     */
    private boolean isOnRemoteServer;

    @Deprecated
    EventBaseDTO() {
    } // for serialization only

    public EventBaseDTO(String name, List<? extends LeaderboardGroupBaseDTO> leaderboardGroups) {
        super(name);
        this.leaderboardGroups = leaderboardGroups;
        this.imageSizes = new HashMap<String, ImageSize>();
        sailorsInfoWebsiteURLs = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        return Util.equalsWithNull(this.getId(), ((EventBaseDTO) o).getId());
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public ImageDTO getLogoImage() {
        ImageDTO result = null;
        for (ImageDTO image : images) {
            if (image.hasTag(MediaTagConstants.LOGO.getName())) {
                result = image;
                break;
            }
        }
        return result;
    }

    public boolean isRunning() {
        Date now = new Date();
        if (startDate != null && endDate != null && (now.after(startDate) && now.before(endDate))) {
            return true;
        }
        return false;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOfficialWebsiteURL() {
        return officialWebsiteURL;
    }

    private String getUrlWithHttpsAsDefaultProtocolIfMissing(String url) {
        final String result;
        if (url != null && !url.contains("://")) {
            result = "https://" + url;
        } else {
            result = url;
        }
        return result;
    }

    public void setOfficialWebsiteURL(String officialWebsiteURL) {
        this.officialWebsiteURL = getUrlWithHttpsAsDefaultProtocolIfMissing(officialWebsiteURL);
    }

    public Map<String, String> getSailorsInfoWebsiteURLs() {
        return sailorsInfoWebsiteURLs;
    }

    public String getSailorsInfoWebsiteURL(String locale) {
        return sailorsInfoWebsiteURLs.get(locale);
    }

    public void setSailorsInfoWebsiteURL(String locale, String url) {
        if (url == null || url.isEmpty()) {
            sailorsInfoWebsiteURLs.remove(locale);
        } else {
            sailorsInfoWebsiteURLs.put(locale, getUrlWithHttpsAsDefaultProtocolIfMissing(url));
        }
    }

    public void setSailorsInfoWebsiteURLs(Map<String, String> sailorsInfoWebsiteURLs) {
        this.sailorsInfoWebsiteURLs.clear();
        for (final Entry<String, String> e : sailorsInfoWebsiteURLs.entrySet()) {
            this.sailorsInfoWebsiteURLs.put(e.getKey(), getUrlWithHttpsAsDefaultProtocolIfMissing(e.getValue()));
        }
    }

    /**
     * If not {@link #setBaseURL(String) set}, defaults to <code>http://sapsailing.com</code>. Meant to describe the
     * base URL that maps to the server instance at which this event can be found, such as
     * <code>http://505worlds2013.sapsailing.com</code>.
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Assign the event's base URL.
     *
     * NOTE: <code>https://</code> will be assumed if no protocol has been provided.
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = getUrlWithHttpsAsDefaultProtocolIfMissing(baseURL);
    }

    public Iterable<? extends LeaderboardGroupBaseDTO> getLeaderboardGroups() {
        return leaderboardGroups;
    }

    public boolean isOnRemoteServer() {
        return isOnRemoteServer;
    }

    public void setIsOnRemoteServer(boolean isOnRemoteServer) {
        this.isOnRemoteServer = isOnRemoteServer;
    }

    /**
     * Constructs a new {@link SailingImageDTO} from the {@code image}, using this event's data to construct
     * the required {@link SailingImageDTO#getEventLink() event link}. The new object which is then also part
     * of the result of {@link #getImages()} is returned and must be used during {@link #removeImage(SailingImageDTO)}
     * when trying to again remove the image added.
     */
    public SailingImageDTO addImage(ImageDTO image) {
        final SailingImageDTO result = new SailingImageDTO(getEventLink(), image);
        images.add(result);
        return result;
    }

    public boolean removeImage(SailingImageDTO image) {
        return images.remove(image);
    }

    public List<SailingImageDTO> getImages() {
        return images;
    }

    private EventLinkDTO getEventLink() {
        final EventLinkDTO eventLink = new EventLinkDTO();
        eventLink.setBaseURL(getBaseURL());
        eventLink.setDisplayName(getName());
        eventLink.setId(getId());
        eventLink.setOnRemoteServer(isOnRemoteServer());
        return eventLink;
    }

    /**
     * Constructs a new {@link SailingVideoDTO} from the {@code video}, using this event's data to construct
     * the required {@link SailingVideoDTO#getEventRef() event reference}. The new object which is then also part
     * of the result of {@link #getVideos()} is returned and must be used during {@link #removeVideo(SailingVideoDTO)}
     * when trying to again remove the video added.
     */
    public SailingVideoDTO addVideo(VideoDTO video) {
        final SailingVideoDTO result = new SailingVideoDTO(getEventLink(), video);
        videos.add(result);
        return result;
    }

    public boolean removeVideo(SailingVideoDTO video) {
        return videos.remove(video);
    }

    public List<SailingVideoDTO> getVideos() {
        return videos;
    }

    public void setImageSize(String imageURL, ImageSize imageSize) {
        if (imageSize == null) {
            imageSizes.remove(imageURL);
        } else {
            imageSizes.put(imageURL, imageSize);
        }
    }

    /**
     * @return the size of the image referenced by <code>imageURL</code> or <code>null</code> if that size is not known
     *         of the image URL is none of those known to this event, in particular neither of {@link #getImageURLs()}
     *         or {@link #getSponsorImageURLs()}.
     */
    public ImageSize getImageSize(String imageURL) {
        return imageSizes.get(imageURL);
    }

    public VenueDTO getVenue() {
        return venue;
    }

    public void setVenue(VenueDTO venue) {
        this.venue = venue;
    }
}
