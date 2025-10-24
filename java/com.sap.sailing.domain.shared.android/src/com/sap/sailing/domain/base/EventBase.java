package com.sap.sailing.domain.base;

import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sse.common.NamedWithID;
import com.sap.sse.common.Renamable;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.media.ImageSize;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.shared.media.WithMedia;

/**
 * Base interface for an Event consisting of all static information, which might be shared
 * by the server and an Android application.
 */
public interface EventBase extends NamedWithID, WithDescription, Renamable, WithMedia, WithQualifiedObjectIdentifier {

    void setDescription(String description);
    
    /**
     * @return a non-<code>null</code> venue for this event
     */
    Venue getVenue();

    /**
     *  @return the start date of the event 
     */
    TimePoint getStartDate();
    
    /**
     * "Atomically" adjusts start and end date. Constraint checks for the end date not being before the start date
     * are made for the two parameters, regardless of the current event's state.
     */
    void setStartAndEndDate(TimePoint startDate, TimePoint endDate);

    void setStartDate(TimePoint startDate);

    /**
     *  @return the end date of the event 
     */
    TimePoint getEndDate();

    void setEndDate(TimePoint startDate);

    boolean isPublic();

    void setPublic(boolean isPublic);

    /**
     * @return the URL of the event's official web site, or <code>null</code> if such a site does not exist or its URL
     *         is not known
     */
    URL getOfficialWebsiteURL();
    
    void setOfficialWebsiteURL(URL officialWebsiteURL);

    /**
     * @return the URL under which the event's landing page in the {@code sapsailing.com} universe can be reached. This
     *         could be something like {@code tw2015.sapsailing.com} or {@link 505worlds2012.sapsailing.com} or similar.
     *         It shall be a URL to which relative paths to pages in the context of this event can be appended, such
     *         as links to race boards, leaderboards or the regatta overview.
     */
    URL getBaseURL();
    
    void setBaseURL(URL baseURL);
    
    /**
     * Returns a mapping of Locales to URLs where the URLs are meant as an external web site containing sailor related
     * information like protests, official results, etc.
     * The default, language independent URL has {@code null} as key.
     * 
     * @return the URLs of an external web site containing sailor related information like protests, official results,
     *         etc.
     */
    Map<Locale, URL> getSailorsInfoWebsiteURLs();
    
    void setSailorsInfoWebsiteURLs(Map<Locale, URL> sailorsInfoWebsiteURLs);
    
    /**
     * Sets a sailorsInfoWebsiteURL for the given locale
     */
    void setSailorsInfoWebsiteURL(Locale locale, URL sailorsInfoWebsiteURL);
    
    /**
     * Checks if there is a sailorsInfoWebsiteURL available for the given locale.
     * 
     * @param locale a locale or null
     * @return true if there is a sailorsInfoWebsiteURL available for the given locale
     */
    boolean hasSailorsInfoWebsiteURL(Locale locale);
    
    /**
     * @param locale a locale to get the associated sailors info website URL for
     * @return the URL of an external web site containing sailor related information like protests, official results, etc.
     *  or <code>null</code> if such a site does not exist for the given locale or its URL is not known.
     */
    URL getSailorsInfoWebsiteURL(Locale locale);
    
    /**
     * Gets the sailorsInfoWebsiteURL for the given locale. If there is no sailorsInfoWebsiteURL for the specific locale
     * but there is a default sailorsInfoWebsiteURL available, this one is returned.
     * So this method returns the best available sailorsInfoWebsiteURL for a user who uses the given locale.
     */
    URL getSailorsInfoWebsiteURLOrFallback(Locale locale);

    Iterable<? extends LeaderboardGroupBase> getLeaderboardGroups();
    
    /** 
     * Sets and converts all event images and videos from the old URL based format to the new richer format 
     * */ 
    boolean setMediaURLs(Iterable<URL> imageURLs, Iterable<URL> sponsorImageURLs, Iterable<URL> videoURLs, URL logoImageURL, Map<URL, ImageSize> imageSizes);
    
    /**
     * Gets all TrackingConnectorInfos containing information over what TrackingConnectors are involved in tracking this
     * event
     * 
     * @return a Set of {@link TrackingConnectorInfo}; the set returned is never {@code null} and never contains
     *         {@code null} values, but it may be empty
     */
    Set<TrackingConnectorInfo> getTrackingConnectorInfos();
}
