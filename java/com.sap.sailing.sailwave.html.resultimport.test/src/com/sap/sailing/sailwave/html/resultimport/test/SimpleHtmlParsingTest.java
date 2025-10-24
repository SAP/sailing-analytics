package com.sap.sailing.sailwave.html.resultimport.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.resultimport.CompetitorRow;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.sailwave.html.resultimport.impl.SailwaveHtmlParser;
import com.sap.sse.common.Util;

public class SimpleHtmlParsingTest {
    @Test
    public void testOpenSampleHtml() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("ILCA4.html")));
        final String readLine = br.readLine();
        assertNotNull(readLine);
        br.close();
    }

    @Test
    public void testMultiLineTD() throws IOException {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("ILCA6.html");
        SailwaveHtmlParser parser = new SailwaveHtmlParser();
        final RegattaResults regattaResults = parser.getRegattaResults(resourceAsStream);
        List<CompetitorRow> result = regattaResults.getCompetitorResults();
        assertFalse(result.isEmpty());
        assertEquals(17, result.size());
        assertTrue(regattaResults.getMetadata().get("seriestitle").contains("Results are provisional as of  9:43 on February 16, 2025"));
        int competitorsChecked = 0;
        for (CompetitorRow row : result) {
            if (Util.contains(row.getNames(), "Daniel Kin Chung Chan")) {
                competitorsChecked++;
                assertEquals("HKG 223750", row.getSailID());
                assertEquals(25.0, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(40.0, row.getTotalPointsBeforeDiscarding(), 0.000000001);
            }
            if (Util.contains(row.getNames(), "Haolun Xia")) {
                competitorsChecked++;
                assertEquals("CHN 210199", row.getSailID());
                assertEquals(47.0, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(69.0, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals((double) 11.00, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getScore(), 0.000000001);
                assertEquals("SCP", Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getMaxPointsReason());
                assertEquals(true, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).isDiscarded());
            }
        }
        assertEquals(2, competitorsChecked);
    }
    
    @Test
    public void testGetCompetitorRows29er() throws IOException {
        SailwaveHtmlParser parser = new SailwaveHtmlParser();
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("29er.html");
        List<CompetitorRow> result = parser.getRegattaResults(resourceAsStream).getCompetitorResults();
        assertFalse(result.isEmpty());
        assertEquals(19, result.size());
        int competitorsChecked = 0;
        for (CompetitorRow row : result) {
            if (Util.contains(row.getNames(), "Ethan Kiu")) {
                competitorsChecked++;
                assertEquals("HKG 3354", row.getSailID());
                assertEquals(41.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(58.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals((double) 2, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).isDiscarded());
            } else if (Util.contains(row.getNames(), "Kaden Chan")) {
                competitorsChecked++;
                assertEquals("HKG 3405", row.getSailID());
                assertEquals(80.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(107.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals((double) 8, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 2).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 2).isDiscarded());
                assertEquals((double) 15, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 4).getScore(), 0.000000001);
                assertEquals(true, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 4).isDiscarded());
            } else if (Util.contains(row.getNames(), "Chloe Kong")) {
                competitorsChecked++;
                assertEquals("HKG 2566", row.getSailID());
                assertEquals(67.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(107.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals("SCP", Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).getMaxPointsReason());
                assertEquals((double) 20, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).getScore(), 0.000000001);
                assertEquals(true, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).isDiscarded());
            }
        }
        assertEquals(3, competitorsChecked);
    }

    @Test
    public void testGetCompetitorRowsILCA4() throws IOException {
        SailwaveHtmlParser parser = new SailwaveHtmlParser();
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("ILCA4.html");
        List<CompetitorRow> result = parser.getRegattaResults(resourceAsStream).getCompetitorResults();
        assertFalse(result.isEmpty());
        assertEquals(50, result.size());
        int competitorsChecked = 0;
        for (CompetitorRow row : result) {
            if (Util.contains(row.getNames(), "YONG LIANG Chuang")) {
                competitorsChecked++;
                assertEquals("TPE 221094", row.getSailID());
                assertEquals(198.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(300.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals((double) 17, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).isDiscarded());
                assertEquals((double) 30, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).isDiscarded());
                assertEquals("DNC", Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).getMaxPointsReason());
                assertEquals((double) 51, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).isDiscarded());
            }
        }
        assertEquals(1, competitorsChecked);
    }
}
