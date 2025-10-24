package com.sap.sailing.domain.test.markpassing;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Disabled;

public class Kiel2011505MarkPassingTest extends AbstractMarkPassingTest {

    public  Kiel2011505MarkPassingTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    // The Start-Times are set very wrong on these races
    
    @Disabled
    public void testRace2() throws Exception {
        testRace("2");
    }
    @Disabled
    public void testRace3() throws Exception {
        testRace("3");
    }
    @Disabled
    public void testRace4() throws Exception {
        testRace("4");
    }

    @Override
    protected String getFileName() {
        return "event_20110609_KielerWoch-505_Race_";
    }
}

