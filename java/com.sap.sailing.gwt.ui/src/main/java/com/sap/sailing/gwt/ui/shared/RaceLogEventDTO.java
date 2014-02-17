package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * DTO for a race log entry.
 */
public class RaceLogEventDTO implements IsSerializable {
    private Integer passId;
    private String authorName;
    private Integer authorPriority;
    private Date createdAt;
    private Date logicalTimePoint;
    
    /** RaceLogEvent.getClass().getSimpleName() */
    private String type;
    
    /** RaceLogEvent.toString() */
    private String info;
    
    RaceLogEventDTO() {}

    public RaceLogEventDTO(int passId, String authorName, Integer authorPriority, Date createdAt, Date logicalTimePoint,
            String type, String info) {
        this.passId = passId;
        this.authorName = authorName;
        this.authorPriority = authorPriority;
        this.createdAt = createdAt;
        this.logicalTimePoint = logicalTimePoint;
        this.type = type;
        this.info = info;
    }

    public int getPassId() {
        return passId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public Integer getAuthorPriority() {
        return authorPriority;
    }

    public Date getLogicalTimePoint() {
        return logicalTimePoint;
    }

    public String getType() {
        String result = type;
        if(result.startsWith("RaceLog")) {
            result = result.substring("RaceLog".length(), result.length());
        }
        if(result.endsWith("EventImpl")) {
            result = result.substring(0, result.length()- "EventImpl".length());
        }
        return result;
    }

    public String getInfo() {
        return info;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
