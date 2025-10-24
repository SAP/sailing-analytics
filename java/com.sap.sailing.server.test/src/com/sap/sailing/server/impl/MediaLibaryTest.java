package com.sap.sailing.server.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.common.media.MimeType;


public class MediaLibaryTest {
    
    private static final Duration FIFTEEN_MINUTES_IN_MILLIS = MillisecondsDurationImpl.ONE_MINUTE.times(15);
    private static final Duration THIRTY_MINUTES_IN_MILLIS = MillisecondsDurationImpl.ONE_MINUTE.times(30);
    private static final Duration ONE_HOUR_IN_MILLIS = MillisecondsDurationImpl.ONE_HOUR;

    private static final RegattaNameAndRaceName RACE_1 = new RegattaNameAndRaceName("regatta 1", "race 1");
//    private static final RegattaNameAndRaceName RACE_1_COPY = new RegattaNameAndRaceName(RACE_1.getRegattaName(), RACE_1.getRaceName());
//    private static final RegattaNameAndRaceName RACE_2 = new RegattaNameAndRaceName("regatta 1", "race 2");
//    private static final RegattaNameAndRaceName RACE_3 = new RegattaNameAndRaceName("regatta 1", "race 3");

    private MediaLibrary mediaLibary;

    @BeforeEach
    public void before() {
        this.mediaLibary = new MediaLibrary();
    }

    /* util */
    private MediaTrack createMediaTrack(String dbId) {
        String title = "title";
        String url = "url";
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = MillisecondsDurationImpl.ONE_HOUR;
        MimeType mimeType = MimeType.mp4;
        Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<RegattaAndRaceIdentifier>();
        assignedRaces.add(RACE_1);
        MediaTrack mediaTrack = new MediaTrack(dbId, title, url, startTime, duration, mimeType, assignedRaces);
        return mediaTrack;
    }
    
    @Test
    public void testGetElementsFromEmtpy() throws Exception {
        assertThat(this.mediaLibary.allTracks().size(), is(0));
    }

    @Test
    public void testCreateOneElement() throws Exception {
        String dbId = "1234";
        MediaTrack mediaTrack = createMediaTrack(dbId);
        this.mediaLibary.addMediaTrack(mediaTrack);
        Iterator<MediaTrack> allMediaTracks = this.mediaLibary.allTracks().iterator();
        assertThat(allMediaTracks.hasNext(), is(true));
        MediaTrack item = allMediaTracks.next();
        assertThat(allMediaTracks.hasNext(), is(false));
        assertTrue(item.equals(mediaTrack));
        assertThat(item.title, is(mediaTrack.title));
        assertThat(item.url, is(mediaTrack.url));
        assertThat(item.startTime, is(mediaTrack.startTime));
        assertThat(item.duration, is(mediaTrack.duration));
        assertThat(item.mimeType, is(mediaTrack.mimeType));
        
    }

    @Test
    public void testCreateDeleteOneElement() throws Exception {
        String dbId = "1234";
        MediaTrack mediaTrack = createMediaTrack(dbId);
        this.mediaLibary.addMediaTrack(mediaTrack);
        MediaTrack mediaTrackClone = new MediaTrack(mediaTrack.dbId, mediaTrack.title, mediaTrack.url, mediaTrack.startTime, mediaTrack.duration, mediaTrack.mimeType, mediaTrack.assignedRaces);
        this.mediaLibary.deleteMediaTrack(mediaTrackClone );
        Collection<MediaTrack> allMediaTracks = this.mediaLibary.allTracks();
        assertThat(allMediaTracks.size(), is(0));
    }

    @Test
    public void testQueryMediaTracksBetween_MatchingStartTimes_TrackLongerThanEndTime() {
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = ONE_HOUR_IN_MILLIS;
        TimePoint rangeStart = startTime;
        TimePoint rangeEnd = rangeStart.plus(THIRTY_MINUTES_IN_MILLIS);

        assertOverlap(startTime, duration, rangeStart, rangeEnd);
    }

    @Test
    public void testQueryMediaTracksBetween_MatchingStartTimes_TrackShorterThanEndTime() {
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = THIRTY_MINUTES_IN_MILLIS;
        TimePoint rangeStart = startTime;
        TimePoint rangeEnd = rangeStart.plus(ONE_HOUR_IN_MILLIS);

        assertOverlap(startTime, duration, rangeStart, rangeEnd);
    }
    
    @Test
    public void testQueryMediaTracksBetween_StartsBeforeEndsAfterRange() {
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = ONE_HOUR_IN_MILLIS;
        TimePoint rangeStart = startTime.plus(FIFTEEN_MINUTES_IN_MILLIS);
        TimePoint rangeEnd = rangeStart.plus(FIFTEEN_MINUTES_IN_MILLIS);

        assertOverlap(startTime, duration, rangeStart, rangeEnd);
    }
    
    @Test
    public void testQueryMediaTracksBetween_StartsAfterEndsBeforeRange() {
        TimePoint rangeStart = MillisecondsTimePoint.now();
        TimePoint rangeEnd = rangeStart.plus(ONE_HOUR_IN_MILLIS);
        TimePoint startTime = rangeStart.plus(FIFTEEN_MINUTES_IN_MILLIS);
        Duration duration = FIFTEEN_MINUTES_IN_MILLIS;

        assertOverlap(startTime, duration, rangeStart, rangeEnd);
    }
    
    @Test
    public void testQueryMediaTracksBetween_StartsAfterEndsAfterRange() {
        TimePoint rangeStart = MillisecondsTimePoint.now();
        TimePoint rangeEnd = rangeStart.plus(THIRTY_MINUTES_IN_MILLIS);
        TimePoint startTime = rangeStart.plus(FIFTEEN_MINUTES_IN_MILLIS);
        Duration duration = THIRTY_MINUTES_IN_MILLIS;

        assertOverlap(startTime, duration, rangeStart, rangeEnd);
    }
    
    @Test
    public void testQueryMediaTracksBetween_StartsBeforeEndsBeforeRange() {
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = THIRTY_MINUTES_IN_MILLIS;
        TimePoint rangeStart = startTime.plus(FIFTEEN_MINUTES_IN_MILLIS);
        TimePoint rangeEnd = rangeStart.plus(THIRTY_MINUTES_IN_MILLIS);

        assertOverlap(startTime, duration, rangeStart, rangeEnd);
    }
    
    @Test
    public void testQueryMediaTracksBetween_TrackEndsBeforeRange() {
        TimePoint startTime = MillisecondsTimePoint.now();
        Duration duration = THIRTY_MINUTES_IN_MILLIS;
        TimePoint rangeStart = startTime.plus(duration).plus(1);
        TimePoint rangeEnd = rangeStart.plus(ONE_HOUR_IN_MILLIS);

        assertNoOverlap(startTime, duration, rangeStart, rangeEnd);
    }
    
    @Test
    public void testQueryMediaTracksBetween_TrackStartsAfterRange() {
        TimePoint rangeStart = MillisecondsTimePoint.now();
        TimePoint rangeEnd = rangeStart.plus(THIRTY_MINUTES_IN_MILLIS);
        TimePoint startTime = rangeStart.plus(ONE_HOUR_IN_MILLIS);
        Duration duration = THIRTY_MINUTES_IN_MILLIS;

        assertNoOverlap(startTime, duration, rangeStart, rangeEnd);
    }
    
    @Test
    public void testRemoveMediaTrack() {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.dbId = "a";
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = MillisecondsDurationImpl.ONE_HOUR;
        
        RegattaNameAndRaceName race = new RegattaNameAndRaceName("regatta 1", "race 1");
        mediaTrack.assignedRaces.add(race);
        
        TimePoint originalStartTime = mediaTrack.startTime;
        TimePoint originalEndTime = mediaTrack.deriveEndTime();

        mediaLibary.addMediaTrack(mediaTrack);
        
        Collection<MediaTrack> firstQueryResult = mediaLibary.findMediaTracksInTimeRange(originalStartTime, originalEndTime);
        assertThat(firstQueryResult.size(), is(1));

        RegattaNameAndRaceName raceClone = new RegattaNameAndRaceName(race.getRegattaName(), race.getRaceName());
        firstQueryResult = mediaLibary.findMediaTracksForRace(raceClone);
        assertThat(firstQueryResult.size(), is(1));
        
        MediaTrack mediaTrackClone = new MediaTrack(mediaTrack.dbId, mediaTrack.title, mediaTrack.url, mediaTrack.startTime, mediaTrack.duration, mediaTrack.mimeType, mediaTrack.assignedRaces);
        mediaLibary.deleteMediaTrack(mediaTrackClone);
        
        Collection<MediaTrack> secondQueryResult = mediaLibary.findMediaTracksInTimeRange(originalStartTime, originalEndTime);
        assertThat(secondQueryResult.size(), is(0));

        secondQueryResult = mediaLibary.findMediaTracksForRace(raceClone);
        assertThat(secondQueryResult.size(), is(0));

    }
    
    @Test
    public void testCacheAddSecondMediaTrackWithSameInterval() {
        MediaTrack firstMediaTrack = new MediaTrack();
        firstMediaTrack.dbId = "a";
        firstMediaTrack.startTime = MillisecondsTimePoint.now();
        firstMediaTrack.duration = MillisecondsDurationImpl.ONE_HOUR;
        mediaLibary.addMediaTrack(firstMediaTrack);
        
        TimePoint queryStartTime = firstMediaTrack.startTime.plus(1);
        TimePoint queryEndTime = firstMediaTrack.deriveEndTime().minus(1);
        
        Collection<MediaTrack> firstQueryResult = mediaLibary.findMediaTracksInTimeRange(queryStartTime, queryEndTime);
        assertThat(firstQueryResult.size(), is(1));
        
        MediaTrack secondMediaTrack = new MediaTrack();
        secondMediaTrack.dbId = "b";
        secondMediaTrack.startTime = firstMediaTrack.startTime;
        secondMediaTrack.duration = firstMediaTrack.duration;
        mediaLibary.addMediaTrack(secondMediaTrack);
        
        Collection<MediaTrack> secondQueryResult = mediaLibary.findMediaTracksInTimeRange(queryStartTime, queryEndTime);
        assertThat(secondQueryResult.size(), is(2));

        TimePoint uncachedStartTime = firstMediaTrack.startTime.plus(2);
        TimePoint uncachedEndTime = firstMediaTrack.deriveEndTime().minus(2);

        Collection<MediaTrack> thirdQueryResult = mediaLibary.findMediaTracksInTimeRange(uncachedStartTime, uncachedEndTime);
        assertThat(thirdQueryResult.size(), is(2));
}
    
    private void assertOverlap(TimePoint startTime, Duration duration, TimePoint rangeStart, TimePoint rangeEnd) {
        //insert test object
        String dbId = "1234";
        String title = "title";
        String url = "url";
        MimeType mimeType = MimeType.mp4;
        Set<RegattaAndRaceIdentifier> emptyRaces = Collections.emptySet();
        MediaTrack mediaTrack = new MediaTrack(dbId, title, url, startTime, duration, mimeType, emptyRaces);
        mediaLibary.addMediaTrack(mediaTrack);
        
        Collection<MediaTrack> mediaTracks = mediaLibary.findMediaTracksInTimeRange(rangeStart, rangeEnd);
        assertThat(mediaTracks.size(), is(1));
        assertThat(mediaTracks.iterator().next().dbId, is(dbId));
    }
    
    private void assertNoOverlap(TimePoint startTime, Duration duration, TimePoint rangeStart, TimePoint rangeEnd) {
        //insert test object
        String dbId = "1234";
        String title = "title";
        String url = "url";
        MimeType mimeType = MimeType.mp4;
        Set<RegattaAndRaceIdentifier> emptyRaces = Collections.emptySet();
        MediaTrack mediaTrack = new MediaTrack(dbId, title, url, startTime, duration, mimeType, emptyRaces);
        mediaLibary.addMediaTrack(mediaTrack);
        
        Collection<MediaTrack> mediaTracks = mediaLibary.findMediaTracksInTimeRange(rangeStart, rangeEnd);
        assertThat(mediaTracks.size(), is(0));
    }
    
    @Test
    public void testIntervalEqualsIdentical() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = date1.plus(1);
        TimeRange interval = new TimeRangeImpl(date1, date2);
        assertTrue(interval.equals(interval));
    }
    
    @Test
    public void testIntervalEqualsSame() throws Exception {
        TimePoint date1_1 = MillisecondsTimePoint.now();
        TimePoint date1_2 = date1_1.plus(1);
        TimePoint date2_1 = date1_1;
        TimePoint date2_2 = date1_2;
        TimeRange interval1 = new TimeRangeImpl(date1_1, date1_2);
        TimeRange interval2 = new TimeRangeImpl(date2_1, date2_2);
        assertTrue(interval1.equals(interval2));
        assertEquals(interval1.hashCode(), interval2.hashCode());
    }
    
    @Test
    public void testIntervalEqualsNull() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = date1.plus(1);
        TimeRange interval = new TimeRangeImpl(date1, date2);
        assertFalse(interval.equals(null));
    }
    
    @Test
    public void testIntervalNotEquals() throws Exception {
        TimePoint date1_1 = MillisecondsTimePoint.now();
        TimePoint date1_2 = date1_1.plus(1);
        TimePoint date2_1 = date1_1.plus(2);
        TimePoint date2_2 = date1_1.plus(3);
        TimeRange interval1 = new TimeRangeImpl(date1_1, date1_2);
        TimeRange interval2 = new TimeRangeImpl(date2_1, date2_2);
        assertFalse(interval1.equals(interval2));
    }
    
}
