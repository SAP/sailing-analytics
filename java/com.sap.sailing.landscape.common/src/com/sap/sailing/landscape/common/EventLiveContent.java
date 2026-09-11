package com.sap.sailing.landscape.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Live tracked races belonging to one event. */
public final class EventLiveContent implements Serializable {
    private static final long serialVersionUID = 852529878882048466L;
    private final String eventId;
    private final String eventName;
    private final Long eventStartMillis;
    private final Long eventEndMillis;
    private final List<RaceLiveContent> racesWithLiveContent;

    public EventLiveContent(final String eventId, final String eventName, final Long eventStartMillis,
            final Long eventEndMillis, final Iterable<RaceLiveContent> racesWithLiveContent) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventStartMillis = eventStartMillis;
        this.eventEndMillis = eventEndMillis;
        final List<RaceLiveContent> racesWithLiveContentCopy = new ArrayList<>();
        racesWithLiveContent.forEach(racesWithLiveContentCopy::add);
        this.racesWithLiveContent = Collections.unmodifiableList(racesWithLiveContentCopy);
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public Long getEventStartMillis() {
        return eventStartMillis;
    }

    public Long getEventEndMillis() {
        return eventEndMillis;
    }

    public List<RaceLiveContent> getRacesWithLiveContent() {
        return racesWithLiveContent;
    }
}
