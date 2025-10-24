package com.sap.sailing.domain.abstractlog.race.impl;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogTagEvent;
import com.sap.sse.common.TimePoint;

/**
 * Default implementation of {@link RaceLogTagEvent}.
 */
public class RaceLogTagEventImpl extends RaceLogEventImpl implements RaceLogTagEvent {

    private static final long serialVersionUID = 7213518902555323432L;

    private final String tag, comment, username, imageURL, resizedImageURL, hiddenInfo;
    private TimePoint revokedAt;

    /**
     * Creates {@link RaceLogTagEvent} with all required information.
     */
    public RaceLogTagEventImpl(String tag, String comment, String hiddenInfo, String imageURL, String resizedImageURL,
            TimePoint createdAt, TimePoint logicalTimePoint, AbstractLogEventAuthor author, Serializable id, int passId) {
        super(createdAt, logicalTimePoint, author, id, passId);
        this.tag = tag;
        this.comment = comment;
        this.hiddenInfo = hiddenInfo;
        this.imageURL = imageURL;
        this.resizedImageURL = resizedImageURL;
        username = author.getName();
        revokedAt = null;
    }

    /**
     * Creates {@link RaceLogTagEvent} without required serializable id by generating it.
     */
    public RaceLogTagEventImpl(String tag, String comment, String hiddenInfo, String imageURL, String resizedImageURL,
            TimePoint createdAt, TimePoint logicalTimePoint, AbstractLogEventAuthor author, int passId) {
        this(tag, comment, hiddenInfo, imageURL, resizedImageURL, createdAt, logicalTimePoint, author, randId(), passId);
    }

    /**
     * Creates {@link RaceLogTagEvent} with minimal required information.
     */
    public RaceLogTagEventImpl(String tag, String comment, String hiddenInfo, String imageURL, String resizedImageURL,
            TimePoint logicalTimePoint, AbstractLogEventAuthor author, int passId) {
        this(tag, comment, hiddenInfo, imageURL, resizedImageURL, now(), logicalTimePoint, author, randId(), passId);
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public String getHiddenInfo() {
        return hiddenInfo;
    }
    
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Only sets revokedAt if tag is not already revoked and given {@link TimePoint} is non-<code>null</code>.
     */
    @Override
    public void markAsRevoked(TimePoint revokedAt) {
        if (this.revokedAt == null && revokedAt != null) {
            this.revokedAt = revokedAt;
        }
    }

    @Override
    public TimePoint getRevokedAt() {
        return revokedAt;
    }

    @Override
    public void accept(RaceLogEventVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getShortInfo() {
        return "tag=" + tag + ", comment=" + comment + ", hiddenInfo=" + hiddenInfo;
    }
    
    @Override
    public int hashCode() {
        return 1023 ^
                (comment == null ? 0 : comment.hashCode()) ^
                (hiddenInfo == null ? 0 : hiddenInfo.hashCode()) ^
                (imageURL == null ? 0 : imageURL.hashCode()) ^
                (resizedImageURL == null ? 0 : resizedImageURL.hashCode()) ^
                (tag == null ? 0 : tag.hashCode()) ^
                (username == null ? 0 : username.hashCode()) ^
                (getLogicalTimePoint() == null ? 0 : getLogicalTimePoint().hashCode()) ^
                (getCreatedAt() == null ? 0 : getCreatedAt().hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RaceLogTagEventImpl other = (RaceLogTagEventImpl) obj;
        if (comment == null) {
            if (other.comment != null)
                return false;
        } else if (!comment.equals(other.comment))
            return false;
        if (hiddenInfo == null) {
            if (other.hiddenInfo != null)
                return false;
        } else if (!hiddenInfo.equals(other.hiddenInfo))
            return false;
        if (imageURL == null) {
            if (other.imageURL != null)
                return false;
        } else if (!imageURL.equals(other.imageURL))
            return false;
        if (resizedImageURL == null) {
            if (other.resizedImageURL != null)
                return false;
        } else if (!resizedImageURL.equals(other.resizedImageURL))
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        if (getLogicalTimePoint() == null) {
            if (other.getLogicalTimePoint() != null)
                return false;
        } else if (!getLogicalTimePoint().equals(other.getLogicalTimePoint()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RaceLogTagEvent [tag=" + tag + ", comment=" + comment + ", hiddenInfo=" + hiddenInfo + ", imageURL=" + imageURL + ", resizedImageURL=" + resizedImageURL + ", username="
                + username + ", revokedAt=" + revokedAt + "]";
    }

    @Override
    public String getImageURL() {
        return imageURL;
    }

    @Override
    public String getResizedImageURL() {
        return resizedImageURL;
    }
}