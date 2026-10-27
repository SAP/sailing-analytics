package com.sap.sse.shared.media.impl;

import java.io.Serializable;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.shared.media.MediaDescriptor;

/**
 * Common media data for media items
 * 
 * @author pgtaboada
 *
 */
public abstract class AbstractMediaDescriptor implements MediaDescriptor, Serializable {
    private static final long serialVersionUID = -6671425870632517274L;

    protected String title;

    protected String subtitle;

    protected TimePoint createdAtDate;

    protected String copyright;

    protected MimeType mimeType;

    protected Set<String> tags = new LinkedHashSet<String>();

    protected URL url;

    protected boolean missing;

    protected boolean missingMailNotificationSent;
    
    protected Locale locale;

    /**
     * Media item with minimal set of information
     * @param url
     * @param mimeType
     */
    public AbstractMediaDescriptor(URL url, MimeType mimeType, TimePoint createdAtDate) {
        this.mimeType = mimeType;
        this.url = url;
        this.createdAtDate = createdAtDate;
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public boolean isMissing() {
        return missing;
    }

    @Override
    public void setMissing(boolean missing) {
        this.missing = missing;
    }

    @Override
    public boolean isMissingMailNotificationSent() {
        return missingMailNotificationSent;
    }

    @Override
    public void setMissingMailNotificationSent(boolean missingMailNotificationSent) {
        this.missingMailNotificationSent = missingMailNotificationSent;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Set<String> getTags() {
        return tags;
    }

    @Override
    public void setTags(Iterable<String> tags) {
        this.tags.clear();
        if (tags != null) {
            Util.addAll(tags, this.tags);
        }
    }


    @Override
    public boolean addTag(String tagName) {
        return tags.add(tagName);
    }

    @Override
    public boolean removeTag(String tagName) {
        return tags.remove(tagName);
    }

    @Override
    public String getSubtitle() {
        return subtitle;
    }

    @Override
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    @Override
    public TimePoint getCreatedAtDate() {
        return createdAtDate;
    }

    @Override
    public void setCreatedAtDate(TimePoint createdAtDate) {
        this.createdAtDate = createdAtDate;
    }

    @Override
    public String getCopyright() {
        return copyright;
    }

    @Override
    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }
    
    @Override
    public Locale getLocale() {
        return locale;
    }
    
    @Override
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public boolean hasTag(String tagName) {
        return tags.contains(tagName);
    }
}
