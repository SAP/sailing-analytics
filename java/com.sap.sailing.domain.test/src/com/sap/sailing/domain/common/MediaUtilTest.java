package com.sap.sailing.domain.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.media.MediaUtil;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class MediaUtilTest {

    @Test
    public void testCompareDatesAllNull() throws Exception {
        TimePoint date1 = null;
        TimePoint date2 = null;
        assertTrue(MediaUtil.compareDatesAllowingNull(date1, date2) == 0);
    }
    
    @Test
    public void testCompareDatesFirstNull() throws Exception {
        TimePoint date1 = null;
        TimePoint date2 = MillisecondsTimePoint.now();
        assertTrue(MediaUtil.compareDatesAllowingNull(date1, date2) < 0);
    }
    
    @Test
    public void testCompareDatesSecondNull() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = null;
        assertTrue(MediaUtil.compareDatesAllowingNull(date1, date2) > 0);
    }
    
    @Test
    public void testCompareDatesFirstGreaterSecond() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = date1.minus(1);
        assertTrue(MediaUtil.compareDatesAllowingNull(date1, date2) > 0);
    }
    
    @Test
    public void testCompareDatesSecondGreaterFirst() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = date1.plus(1);
        assertTrue(MediaUtil.compareDatesAllowingNull(date1, date2) < 0);
    }
    
    @Test
    public void testEqualsDatesAllNull() throws Exception {
        TimePoint date1 = null;
        TimePoint date2 = null;
        assertTrue(MediaUtil.equalsDatesAllowingNull(date1, date2));
    }
    
    @Test
    public void testEqualsDatesFirstNull() throws Exception {
        TimePoint date1 = null;
        TimePoint date2 = MillisecondsTimePoint.now();
        assertFalse(MediaUtil.equalsDatesAllowingNull(date1, date2));
    }
    
    @Test
    public void testEqualsDatesSecondNull() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = null;
        assertFalse(MediaUtil.equalsDatesAllowingNull(date1, date2));
    }
    
    @Test
    public void testEqualsDatesBothEqual() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = date1.plus(0);
        assertTrue(MediaUtil.equalsDatesAllowingNull(date1, date2));
    }
    
    @Test
    public void testEqualsDatesNotEqual() throws Exception {
        TimePoint date1 = MillisecondsTimePoint.now();
        TimePoint date2 = date1.plus(1);
        assertFalse(MediaUtil.equalsDatesAllowingNull(date1, date2));
    }
    
}
