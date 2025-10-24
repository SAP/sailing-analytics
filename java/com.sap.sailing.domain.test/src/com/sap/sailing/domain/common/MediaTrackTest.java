package com.sap.sailing.domain.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

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

    @Test
    public void testNoDurationWithOverlap() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = null; // --> open end
        
        TimePoint startTime = mediaTrack.startTime.plus(2);
        TimePoint endTime = startTime.plus(2); 
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testNoDurationWithoutOverlap() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = null; // --> open end
        
        TimePoint startTime = mediaTrack.startTime.minus(10);
        TimePoint endTime = startTime.plus(2); 
        assertFalse(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testNoDurationOpenEnd() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = null; // --> open end
        
        TimePoint startTime = mediaTrack.startTime.minus(10);
        TimePoint endTime = null; 
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

    @Test
    public void testNoDurationOpenEnd2() throws Exception {
        MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.startTime = MillisecondsTimePoint.now();
        mediaTrack.duration = null; // --> open end
        
        TimePoint startTime = mediaTrack.startTime.plus(10);
        TimePoint endTime = null; 
        assertTrue(mediaTrack.overlapsWith(startTime, endTime));
        
    }

}
