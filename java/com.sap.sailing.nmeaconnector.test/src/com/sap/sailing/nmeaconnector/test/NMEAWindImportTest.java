package com.sap.sailing.nmeaconnector.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.nmeaconnector.NmeaFactory;
import com.sap.sse.common.Util;

public class NMEAWindImportTest {
    Iterable<Wind> windFixes;
    
    @BeforeEach
    public void setUp() throws FileNotFoundException, InterruptedException {
        NmeaFactory.INSTANCE.getUtil().registerAdditionalParsers();
        windFixes = NmeaFactory.INSTANCE.readWind(new FileInputStream("resources/ExcerptFromLogJB010815.txt"));
    }
    
    @Test
    public void testThatSomeWindWasFound() {
        assertNotNull(windFixes);
        assertTrue(!Util.isEmpty(windFixes));
    }
    
    @AfterEach
    public void tearDown() {
        NmeaFactory.INSTANCE.getUtil().unregisterAdditionalParsers();
    }
}
