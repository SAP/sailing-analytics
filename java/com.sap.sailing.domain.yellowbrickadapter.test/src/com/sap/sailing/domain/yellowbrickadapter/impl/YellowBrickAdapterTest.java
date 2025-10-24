package com.sap.sailing.domain.yellowbrickadapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Optional;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.yellowbrickadapter.YellowBrickRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class YellowBrickAdapterTest {
    private static final String RMSR2019 = "rmsr2019";
    private YellowBrickTrackingAdapterImpl adapter;
    
    @BeforeEach
    public void setUp() {
        adapter = new YellowBrickTrackingAdapterImpl(/* base domain factory */ null);
    }
    
    @Test
    public void testSimpleUrlConstruction() {
        final String rmsr2019Url = adapter.getUrlForLatestFix(RMSR2019, /* username */ Optional.of("hello"), /* password */ Optional.of("world"));
        assertTrue(rmsr2019Url.startsWith("https://yb.tl/API3/Race/rmsr2019/GetPositions?"));
        assertTrue(rmsr2019Url.contains("username=hello"));
        assertTrue(rmsr2019Url.contains("password=world"));
    }
    
    @Test
    public void testGetRaceMetadata() throws IOException, ParseException, java.text.ParseException {
        final YellowBrickRace race = adapter.getRaceMetadata(RMSR2019, Optional.empty(), Optional.empty());
        assertEquals(RMSR2019, race.getRaceUrl());
        assertEquals(TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2019-10-26T23:55:01Z")), race.getTimePointOfLastFix());
        assertEquals(113, race.getNumberOfCompetitors());
    }
    
    @Test
    public void testGetFullRaceData() throws MalformedURLException, IOException, ParseException, java.text.ParseException {
        final PositionsDocument fullRace = adapter.getStoredData(RMSR2019, /* username */ Optional.empty(), /* password */ Optional.empty());
        assertEquals(113, Util.size(fullRace.getTeams()));
        assertEquals(TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse("2019-10-26T23:55:01Z")), fullRace.getTimePointOfLastFix());
        assertEquals(2009, Util.size(Util.filter(fullRace.getTeams(), team->team.getCompetitorName().equals("JYS Jarhead")).iterator().next().getPositions()));
    }
}
