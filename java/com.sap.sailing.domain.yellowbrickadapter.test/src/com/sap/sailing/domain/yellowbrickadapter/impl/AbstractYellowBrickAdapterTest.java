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

public abstract class AbstractYellowBrickAdapterTest {
    protected String raceName;
    private YellowBrickTrackingAdapterImpl adapter;
    protected String expectedTimePointOfLastFix;
    protected int expectedNumberOfCompetitors;
    protected String expectedCompetitorName;
    protected int expectedCompetitorNumberOfPositions;
    
    @BeforeEach
    public void setUp() {
        adapter = new YellowBrickTrackingAdapterImpl(/* base domain factory */ null);
    }
    
    @Test
    public void testSimpleUrlConstruction() {
        final String rmsr2019Url = adapter.getUrlForLatestFix(raceName, /* username */ Optional.of("hello"), /* password */ Optional.of("world"));
        assertTrue(rmsr2019Url.startsWith("https://yb.tl/API3/Race/"+raceName+"/GetPositions?"));
        assertTrue(rmsr2019Url.contains("username=hello"));
        assertTrue(rmsr2019Url.contains("password=world"));
    }
    
    @Test
    public void testGetRaceMetadata() throws IOException, ParseException, java.text.ParseException {
        final YellowBrickRace race = adapter.getRaceMetadata(raceName, Optional.empty(), Optional.empty());
        assertEquals(raceName, race.getRaceUrl());
        assertEquals(TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(expectedTimePointOfLastFix)), race.getTimePointOfLastFix());
        assertEquals(expectedNumberOfCompetitors, race.getNumberOfCompetitors());
    }
    
    @Test
    public void testGetFullRaceData() throws MalformedURLException, IOException, ParseException, java.text.ParseException {
        final PositionsDocument fullRace = adapter.getStoredData(raceName, /* username */ Optional.empty(), /* password */ Optional.empty());
        assertEquals(expectedNumberOfCompetitors, Util.size(fullRace.getTeams()));
        assertEquals(TimePoint.of(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(expectedTimePointOfLastFix)), fullRace.getTimePointOfLastFix());
        assertEquals(expectedCompetitorNumberOfPositions,
                Util.size(Util
                        .filter(fullRace.getTeams(), team -> team.getCompetitorName().equals(expectedCompetitorName))
                        .iterator().next().getPositions()));
    }
}
