package com.sap.sailing.domain.common;

import org.junit.Test;

import com.sap.sailing.domain.common.impl.MillisecondsDurationImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.media.MediaTrack;

import static org.junit.Assert.*;

public class MediaTrackTest {
    
    private static final Duration ONE_MILLISECOND = new MillisecondsDurationImpl(1);

    @Test
    public void testExactOverlap() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND;
        
        TimePoint startTime = mediaTrack.startTime;
        TimePoint endTime = mediaTrack.deriveEndTime();
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testNoOverlapLeft() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND;
        
        TimePoint startTime = mediaTrack.startTime.plus(2);
        TimePoint endTime = startTime.plus(1);
        assertFalse(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testNoOverlapRight() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND;
        
        TimePoint startTime = mediaTrack.startTime.minus( 2);
        TimePoint endTime = startTime.plus(1);
        assertFalse(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testPartialOverlapLeft() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND.times(2);
        
        TimePoint startTime = mediaTrack.startTime.minus( 1);
        TimePoint endTime = startTime.plus(2);
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testPartialOverlapRight() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND.times(2);
        
        TimePoint startTime = mediaTrack.startTime.plus(1);
        TimePoint endTime = startTime.plus(2);
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testMediaFullyIncluded() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND;
        
        TimePoint startTime = mediaTrack.startTime.minus( 1);
        TimePoint endTime = startTime.plus(3);
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testMediaFullyIncluding() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND.times(3);
        
        TimePoint startTime = mediaTrack.startTime.plus(1);
        TimePoint endTime = startTime.plus(1);
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testOverlapOpenEndStartingEarlier() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND;
        
        TimePoint startTime = mediaTrack.startTime.minus( 1);
        TimePoint endTime = null; //--> open end
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testOverlapOpenEndStartingLater() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND.times(2);
        
        TimePoint startTime = mediaTrack.startTime.plus(1);
        TimePoint endTime = null; //--> open end
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testOpenEndNoOverlap() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = ONE_MILLISECOND;
        
        TimePoint startTime = mediaTrack.startTime.plus(2);
        TimePoint endTime = null; //--> open end
        assertFalse(mediaTrack.overlapsWith(startTime, endTime));
        
    }

}
