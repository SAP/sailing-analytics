package com.sap.sailing.domain.abstractlog.race.impl;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogTagEvent;
import com.sap.sse.common.TimePoint;

public class RaceLogTagEventImpl extends RaceLogEventImpl implements RaceLogTagEvent {

    private static final long serialVersionUID = 7213518902555323432L;

    private final String tag, comment, imageURL, username;

    private final boolean visibleForPublic;

    public RaceLogTagEventImpl(String pTag, String pComment, String pImageURL, boolean pVisibleForPublic, TimePoint createdAt,
            TimePoint logicalTimePoint, AbstractLogEventAuthor author, Serializable pId, int pPassId) {
        super(createdAt, logicalTimePoint, author, pId, pPassId);
        tag = pTag;
        comment = pComment;
        imageURL = pImageURL;
        username = author.getName();
        visibleForPublic = pVisibleForPublic;
    }

    public RaceLogTagEventImpl(String pTag, String pComment, String pImageURL, boolean visibleForPublic, TimePoint createdAt,
            TimePoint logicalTimePoint, AbstractLogEventAuthor author, int pPassId) {
        this(pTag, pComment, pImageURL, visibleForPublic, createdAt, logicalTimePoint, author, randId(), pPassId);
    }

    public RaceLogTagEventImpl(String pTag, String pComment, String pImageURL, boolean visibleForPublic, 
            TimePoint logicalTimePoint, AbstractLogEventAuthor author, int pPassId) {
        this(pTag, pComment, pImageURL, visibleForPublic, now(), logicalTimePoint, author, randId(), pPassId);
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
    public String getImageURL() {
        return imageURL;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public void accept(RaceLogEventVisitor visitor) {
        visitor.visit(this);
    }
    
    @Override
    public String getShortInfo() {
        return "tag=" + tag + ", comment=" + comment;
    }

    @Override
    public boolean isPublic() {
        return visibleForPublic;
    }
}