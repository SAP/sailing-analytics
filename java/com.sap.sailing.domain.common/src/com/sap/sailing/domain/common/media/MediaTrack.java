package com.sap.sailing.domain.common.media;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.media.MediaSubType;
import com.sap.sse.common.media.MimeType;

/**
 * The {@link #hashCode()} and {@link #equals(Object)} methods are based solely on the {@link #dbId} field.
 * <p>
 * 
 * See http://my.opera.com/core/blog/2010/03/03/everything-you-need-to-know-about-html5-video-and-audio-2
 * <p>
 * 
 * @author Jens Rommel (D047974)
 * 
 */
public class MediaTrack implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        UNDEFINED('?'), CANNOT_PLAY('-'), NOT_REACHABLE('#'), REACHABLE('+');

        private final char symbol;

        private Status(char symbol) {
            this.symbol = symbol;
        }

        public String toString() {
            return String.valueOf(this.symbol);
        }
    }

    public String dbId;
    public String title;
    public String url;
    public TimePoint startTime;
    public Duration duration;
    public MimeType mimeType;
    @Deprecated
    // should not be used
    public Status status = Status.UNDEFINED;
    public Set<RegattaAndRaceIdentifier> assignedRaces;

    public MediaTrack() {
        assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
    }

    public MediaTrack(String title, String url, TimePoint startTime, Duration duration, MimeType mimeType,
            Set<RegattaAndRaceIdentifier> assignedRaces) {
        this();
        this.title = title;
        this.url = url;
        this.startTime = startTime;
        this.duration = duration;
        this.mimeType = mimeType;
        if (assignedRaces != null) {
            this.assignedRaces.addAll(assignedRaces);
        }
    }

    public MediaTrack(String dbId, String title, String url, TimePoint startTime, Duration duration, MimeType mimeType,
            Set<RegattaAndRaceIdentifier> assignedRaces) {
        this(title, url, startTime, duration, mimeType, assignedRaces);
        this.dbId = dbId;
    }

    public String toString() {
        return title + " - " + url + " [" + typeToString() + ']' + " - " + assignedRaces + " - " + startTime + " [" + duration + status + ']'; 
    }

    public TimePoint deriveEndTime() {
        if (startTime != null) {
            return startTime.plus(duration);
        } else {
            return null;
        }
    }

    public String typeToString() {
        return mimeType == null ? "undefined" : mimeType.toString();
    }

    public boolean isYoutube() {
        return (mimeType != null) && MediaSubType.youtube.equals(mimeType.mediaSubType);
    }

    /**
     * Checks for overlap of this start time and duration with the given startTime and endTime, excluding boundaries!
     * Behaviour for given endTime being earlier than given startTime is not specified. endTime being null represents
     * "open end". Open beginning is not allow, though!
     * 
     * @param startTime
     *            Must not be null.
     * @param endTime
     *            May be null representing "open end".
     */
    public boolean overlapsWith(TimePoint startTime, TimePoint endTime) {
        if (this.startTime == null) {
            return false;
        } else {
            return !this.endsBefore(startTime != null ? startTime.asDate() : null) && !this.beginsAfter(endTime != null ? endTime.asDate() : null);
        }
    }

    public boolean isConnectedTo(RegattaAndRaceIdentifier race) {
        if (assignedRaces.contains(race)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MediaTrack) {
            MediaTrack mediaTrack = (MediaTrack) obj;
            return this.dbId == null ? mediaTrack.dbId == null : this.dbId.equals(mediaTrack.dbId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.dbId == null ? 0 : this.dbId.hashCode();
    }

    public boolean beginsAfter(Date date) {
        if (date == null) {
            return false;
        } else if (startTime.asDate().after(date)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean endsBefore(Date date) {
        if (date == null) {
            return false;
        } else if (duration == null) {
            return false; //null-duration implies open-ended!
        } else if (deriveEndTime().asDate().before(date)) {
            return true;
        } else {
            return false;
        }
    }

}
