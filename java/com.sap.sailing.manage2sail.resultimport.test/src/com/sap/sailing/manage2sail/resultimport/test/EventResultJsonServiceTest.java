package com.sap.sailing.manage2sail.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.sap.sailing.manage2sail.EventResultDescriptor;
import com.sap.sailing.manage2sail.Manage2SailEventResultsParser;
import com.sap.sailing.manage2sail.Manage2SailEventResultsParserImpl;

public class EventResultJsonServiceTest extends AbstractEventResultJsonServiceTest {
    @Test
    public void testParsingEventResultsFromJson() throws IOException {
        Manage2SailEventResultsParserImpl parser = new Manage2SailEventResultsParserImpl();
        EventResultDescriptor eventResult = parser.getEventResult(getInputStream(EVENT_RESULTS_JSON));
        assertNotNull(eventResult);
        assertEquals(14, eventResult.getRegattaResults().size());
    }
    
    @Test
    public void testEventResult() throws IOException {
        Manage2SailEventResultsParser parser = new Manage2SailEventResultsParserImpl();
        InputStream is = getClass().getClassLoader().getResourceAsStream("worldcupMallorca2013_Races.json");
        final EventResultDescriptor eventResult = parser.getEventResult(is);
        assertNotNull(eventResult);
        assertEquals("ec19d09b-7e65-4e12-9fc4-8cafe449a4dc", eventResult.getId());
        assertEquals("WC 2013 Mallorca", eventResult.getName());
    }
}
