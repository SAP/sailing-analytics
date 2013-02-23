package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;

import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.JSONService;
import com.sap.sailing.domain.tractracadapter.RaceRecord;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;

@Ignore
public class ParseTracTracJSONServiceOutputTest {
    @Test
    public void testWaymouth() throws IOException, ParseException {
        JSONObject result = parseJSONObject("/jsonservice_weymouth.php.txt");
        assertNotNull(result);
        JSONObject event = (JSONObject) result.get("event");
        assertNotNull(event);
        assertEquals("Sailing Team Germany", event.get("name"));
        JSONArray races = (JSONArray) result.get("races");
        assertEquals(24, races.size());
        for (Object race : races) {
            JSONObject jsonRace = (JSONObject) race;
            assertNotNull(jsonRace.get("url"));
            assertEquals("weym", ((String) jsonRace.get("name")).substring(0, "weym".length()));
        }
    }

    private JSONObject parseJSONObject(String filename) throws IOException, ParseException {
        InputStream is = getClass().getResourceAsStream(filename);
        assertNotNull(is);
        return parseJSONObject(is);
    }

    private JSONObject parseJSONObject(InputStream is) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object result = parser.parse(new InputStreamReader(is));
        assertTrue(result instanceof JSONObject);
        return (JSONObject) result;
    }
    
    @Test
    public void testHamilton() throws IOException, ParseException {
        JSONObject result = parseJSONObject("/jsonservice-505-hamilton.php.txt");
        assertNotNull(result);
        JSONObject event = (JSONObject) result.get("event");
        assertNotNull(event);
        assertEquals("SAP 2011 505 World Championship", event.get("name"));
        JSONArray races = (JSONArray) result.get("races");
        assertEquals(14, races.size());
        for (Object race : races) {
            JSONObject jsonRace = (JSONObject) race;
            assertNotNull(jsonRace.get("url"));
            assertNotNull(jsonRace.get("name"));
        }
    }
    
    @Test
    public void testWeymouthOnline() throws MalformedURLException, IOException, java.text.ParseException, ParseException, URISyntaxException {
        JSONService jsonService = DomainFactory.INSTANCE.parseJSONURL(new URL(
                "http://" + TracTracConnectionConstants.HOST_NAME + "/events/event_20110505_SailingTea/jsonservice.php?trickOurProxy=true"));
        assertEquals("Academy Tracking 2011", jsonService.getEventName());
        List<RaceRecord> races = jsonService.getRaceRecords();
        assertTrue(races.size()>=28);
        int found = 0;
        for (RaceRecord race : races) {
            assertNotNull(race.getReplayURL());
            if (race.getName().startsWith("weym") || race.getName().startsWith("kiwo") || race.getName().startsWith("schwerttest")) {
                found++;
            }
        }
        assertTrue(found >= 28);
    }

    @Test
    public void testWeymouthURIsOnline() throws MalformedURLException, IOException, java.text.ParseException, ParseException, URISyntaxException {
        JSONService jsonService = DomainFactory.INSTANCE.parseJSONURL(new URL(
                "http://" + TracTracConnectionConstants.HOST_NAME + "/events/event_20110505_SailingTea/jsonservice.php"));
        List<RaceRecord> races = jsonService.getRaceRecords();
        assertFalse(races.isEmpty());
        for (RaceRecord race : races) {
            assertEquals(new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":4400"), race.getLiveURI());
            assertEquals(new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":4401"), race.getStoredURI());
        }
    }

    @Test
    public void testHamiltonOnline() throws MalformedURLException, IOException, java.text.ParseException, ParseException, URISyntaxException {
        JSONService jsonService = DomainFactory.INSTANCE.parseJSONURL(new URL(
                "http://" + TracTracConnectionConstants.HOST_NAME + "/events/event_20110308_SAPWorldCh/jsonservice.php?humba=trala"));
        assertEquals("SAP 2011 505 World Championship", jsonService.getEventName());
        List<RaceRecord> races = jsonService.getRaceRecords();
        assertEquals(14, races.size());
        for (RaceRecord race : races) {
            assertNotNull(race.getReplayURL());
            assertNotNull(race.getName());
        }
    }
}
