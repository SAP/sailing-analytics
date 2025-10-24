package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;

public class RaceLogEventAuthorTest {
    
    @Test
    public void testCompareToSame() {
        AbstractLogEventAuthor author = new LogEventAuthorImpl("", 1);
        int result = author.compareTo(author);
        
        assertEquals(0, result);
    }
    
    @Test
    public void testCompareToMinor() {
        AbstractLogEventAuthor author = new LogEventAuthorImpl("", 1);
        AbstractLogEventAuthor minor = new LogEventAuthorImpl("", 2);
        int result = author.compareTo(minor);
        
        assertTrue(result > 0);
    }
    
    @Test
    public void testCompareToMajor() {
        AbstractLogEventAuthor author = new LogEventAuthorImpl("", 3);
        AbstractLogEventAuthor major = new LogEventAuthorImpl("", 2);
        int result = author.compareTo(major);
        
        assertTrue(result < 0);
    }

}
