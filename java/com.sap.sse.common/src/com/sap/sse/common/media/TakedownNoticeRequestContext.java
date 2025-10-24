package com.sap.sse.common.media;

import java.io.Serializable;

/**
 * Whenever media content is made available that may have been uploaded by an end user, there is a possibility that
 * other users would like to request this content to be taken down, hidden or otherwise made unavailable. Reasons for
 * this could be IP infringements, copyright / licensing violations, nudity, violence or otherwise offensive content.
 * <p>
 * 
 * In order to allow users to be specific in their take-down notice / request, an instance of this class collects all
 * information required to generate a very specific message to an administrator that provides data about the location /
 * URL where the content was found, as well as a link to the content itself, such as the image / video URL.
 * <p>
 * 
 * See also <a href="https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=6105">bug 6105</a>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TakedownNoticeRequestContext implements Serializable {
    private static final long serialVersionUID = -7013553116801501898L;

    /**
     * A message key for a message that takes as parameters the following strings:
     * <ol>
     * <li>{@link #getContentUrl() content URL}</li>
     * <li>a {@link #getContextDescriptionMessageParameter() message parameter} identifying the place where the image was found, such as a competitor or event name</li>
     * <li>{@link #getUsername() name of the user} requesting the take-down<li>
     * <li>reason for the request, as taken from {@link #getNatureOfClaim()}</li>
     * <li>user-provided {@link #getReportingUserComment() free-text comment}</li>
     * <li>list of {@link #getSupportingURLs() additional URLs} supporting the request, in a single string</li>
     * </ol>
     * A message template then may look like this:
     * <pre>
     *   competitorImage=The image with URL {0} that it used as a competitor image for {1} has been reported by user {2} for reason: {3}. The user sends the following additional explanation: "{4}". Additional URLs to help clarify this request are: {5}.
     * </pre>
     * The message can be generated using a {@link TakedownNoticeFactory} for the administrator user's locale.
     */
    private final String contextDescriptionMessageKey;
    
    /**
     * One parameter for the context description message that describes, e.g., which competitor, which event,
     * which regatta or which race has been the context of the medium whose take-down is requested.
     */
    private final String contextDescriptionMessageParameter;
    
    /**
     * The URL pointing to the media object
     */
    private final String contentUrl;
    
    private final NatureOfClaim natureOfClaim;
    
    /**
     * A free text form for the reporting user to describe the nature of the claim
     */
    private final String reportingUserComment;
    
    /**
     * Optional additional URLs provided by the user reporting the medium, providing information
     * about the nature or justification of the claim.
     */
    private final Iterable<String> supportingURLs;
    
    /**
     * Name of the user who reports the medium. This user is expected to have a validated e-mail address, as a confirmation
     * about the report must be sent to that address.
     */
    private final String username;

    /**
     * URL of the page on which the media was seen by the user
     */
    private final String pageUrl;

    public TakedownNoticeRequestContext(String contextDescriptionMessageKey, String contextDescriptionMessageParameter, String contentUrl,
            String pageUrl, NatureOfClaim natureOfClaim, String reportingUserComment, Iterable<String> supportingURLs, String username) {
        this.contextDescriptionMessageKey = contextDescriptionMessageKey;
        this.contextDescriptionMessageParameter = contextDescriptionMessageParameter;
        this.contentUrl = contentUrl;
        this.pageUrl = pageUrl;
        this.natureOfClaim = natureOfClaim;
        this.reportingUserComment = reportingUserComment;
        this.supportingURLs = supportingURLs;
        this.username = username;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public NatureOfClaim getNatureOfClaim() {
        return natureOfClaim;
    }

    public String getReportingUserComment() {
        return reportingUserComment;
    }

    public Iterable<String> getSupportingURLs() {
        return supportingURLs;
    }

    public String getUsername() {
        return username;
    }

    public String getContextDescriptionMessageKey() {
        return contextDescriptionMessageKey;
    }

    public String getContextDescriptionMessageParameter() {
        return contextDescriptionMessageParameter;
    }
}
