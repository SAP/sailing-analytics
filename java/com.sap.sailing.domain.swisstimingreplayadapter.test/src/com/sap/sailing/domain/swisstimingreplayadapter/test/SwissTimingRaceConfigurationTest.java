package com.sap.sailing.domain.swisstimingreplayadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayRace;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingRaceConfig;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.SwissTimingReplayServiceImpl;
import com.sap.sse.util.HttpUrlConnectionHelper;

public class SwissTimingRaceConfigurationTest {
    
    private static final String JSON_URL = "https://static.sapsailing.com/OSG2012/OSG2012_static.json";
    
    @Test
    public void testLoadConfigurations() throws IOException, ParseException, org.json.simple.parser.ParseException {
        InputStream inputStream = (InputStream) HttpUrlConnectionHelper.redirectConnection(new URL(JSON_URL)).getContent();
        List<SwissTimingReplayRace> races = new SwissTimingReplayServiceImpl(DomainFactory.INSTANCE).parseJSONObject(inputStream , JSON_URL);
        assertFalse(races.isEmpty());
    }
    
    @Disabled("SwissTiming shut down their configuration server for good (2020-11-13), so we cannot get at the configs anymore")
    @Test
    public void testRaceConfig_446483_no_detail_config() throws Exception {
        URL configFileURL = new URL(MessageFormat.format(SwissTimingReplayServiceImpl.RACE_CONFIG_URL_TEMPLATE, "446483"));
        URLConnection connection = configFileURL.openConnection();
        InputStream configDataStream = connection.getInputStream();
        final Charset charset = HttpUrlConnectionHelper.getCharsetFromConnectionOrDefault(connection, "UTF-8");
        SwissTimingRaceConfig config_446483 = new SwissTimingReplayServiceImpl(DomainFactory.INSTANCE).loadRaceConfig(configDataStream, charset);
        assertNotNull(config_446483);
        assertEquals("GB", config_446483.country_code);
        assertNull(config_446483.event_name);
        assertEquals("60", config_446483.gmt_offset);
        assertEquals("50,603424", config_446483.latitude);
        assertEquals("Nothe", config_446483.location);
        assertEquals("-2,442963", config_446483.longitude);
        assertNull(config_446483.race_start_ts);

    }

    @Disabled("SwissTiming shut down their configuration server for good (2020-11-13), so we cannot get at the configs anymore")
    @Test
    public void testRaceConfig_6260_with_detail_config() throws Exception {
        URL configFileURL = new URL(MessageFormat.format(SwissTimingReplayServiceImpl.RACE_CONFIG_URL_TEMPLATE, "6260"));
        URLConnection connection = configFileURL.openConnection();
        InputStream configDataStream = connection.getInputStream();
        final Charset charset = HttpUrlConnectionHelper.getCharsetFromConnectionOrDefault(connection, "UTF-8");
        SwissTimingRaceConfig config_446483 = new SwissTimingReplayServiceImpl(DomainFactory.INSTANCE).loadRaceConfig(configDataStream, charset);
        assertNotNull(config_446483);
        assertEquals("GB", config_446483.country_code);
        // TODO There used to be an attribute event_name in the "config" sub-element; around 2017-07-25 these things seem to have vanished;
        // Asked Radek Masnica <masnica.r@st-software.com> and Christian Sgodzay <Sgodzay.C@st-sportservice.com> about the changes.
        // Hopefully this can be re-enabled soon...
        // assertEquals("London 2012 Olympic Games", config_446483.event_name);
        // assertEquals("1343914800000", config_446483.race_start_ts);
        assertEquals("60", config_446483.gmt_offset);
        assertEquals("50,603424", config_446483.latitude);
        assertEquals("Nothe", config_446483.location);
        assertEquals("-2,442963", config_446483.longitude);
    }
}
