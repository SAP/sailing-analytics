package com.sap.sailing.freg.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.sap.sailing.resultimport.CompetitorRow;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.freg.resultimport.impl.FregHtmlParser;
import com.sap.sse.common.Util;

public class SimpleHtmlParsingTest {
    @Test
    public void testOpenSampleHtml() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("freg_html_export_sample.html")));
        final String readLine = br.readLine();
        assertNotNull(readLine);
        br.close();
    }

    @Test
    public void testMultiLineTD() throws IOException {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("Coupe-internationale-de-france.htm");
        FregHtmlParser parser = new FregHtmlParser();
        final RegattaResults regattaResults = parser.getRegattaResults(resourceAsStream);
        List<CompetitorRow> result = regattaResults.getCompetitorResults();
        assertFalse(result.isEmpty());
        assertEquals(57, result.size());
        assertTrue(regattaResults.getMetadata().get("info1").contains("PRE- WORLD \n      2012 - COUPE INTERNATIONALE DE FRANCE 505"));
        for (CompetitorRow row : result) {
            if (Util.contains(row.getNames(), "MARTIN Aline")) {
                assertEquals("FRA 8716", row.getSailID());
                assertEquals(111, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(230.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
            }
        }
    }
    
    @Test
    public void testEmptyAndDashedScores() throws IOException {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("pre_world_505_s.htm");
        FregHtmlParser parser = new FregHtmlParser();
        List<CompetitorRow> result = parser.getRegattaResults(resourceAsStream).getCompetitorResults();
        assertFalse(result.isEmpty());
        assertEquals(117, result.size());
        for (CompetitorRow row : result) {
            if (Util.contains(row.getNames(), "NIEMINEN Jukka")) {
                assertEquals("FIN 8768", row.getSailID());
                assertEquals(118, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(118.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertNull(Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getRank());
                assertNull(Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getScore());
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).isDiscarded());
                assertNull(Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getRank());
                assertEquals((double) 118.00, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).isDiscarded());
            } else if (Util.contains(row.getNames(), "HAGAN Douglas")) {
                assertEquals("AUS 8817", row.getSailID());
                assertEquals(235.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(235.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertNull(Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getRank());
                assertEquals("DNF", Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getMaxPointsReason());
                assertEquals((double) 117.00, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).isDiscarded());
                assertNull(Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getRank());
                assertEquals("DNF", Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getMaxPointsReason());
                assertEquals((double) 118.00, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).isDiscarded());
            }
        }
    }
    
    @Test
    public void matchAngleBracketTest() {
        Pattern tr = Pattern.compile("<(tr|TR)( [^>]*)?>");
        Matcher matcher = tr.matcher("Humba humba <tr><td>Trala</td></td>");
        assertTrue(matcher.find(0));
        assertEquals("<tr>", matcher.group());
        Matcher matcher2 = tr.matcher("Humba humba <tr color=#123456><td>Trala</td></td>");
        assertTrue(matcher2.find(0));
        assertEquals("<tr color=#123456>", matcher2.group());
    }
    
    @Test
    public void testFindTableRows() throws IOException {
        FregHtmlParser parser = new FregHtmlParser();
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("freg_html_export_sample.html");
        List<String> rowContents = parser.getRowContents(resourceAsStream);
        assertNotNull(rowContents);
        assertTrue(!rowContents.isEmpty());
        resourceAsStream.close();
        for (int i=2; i<rowContents.size()-1; i++) {
            List<String> tdContent = parser.getTagContents(rowContents.get(i), "td");
            assertEquals(14, tdContent.size(), "Expected "+tdContent+" index "+i+" to have size 14 but was "+tdContent.size());
        }
    }
    
    @Test
    public void testGetCompetitorRows505() throws IOException {
        FregHtmlParser parser = new FregHtmlParser();
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("freg_html_export_sample.html");
        List<CompetitorRow> result = parser.getRegattaResults(resourceAsStream).getCompetitorResults();
        assertFalse(result.isEmpty());
        assertEquals(75, result.size());
        for (CompetitorRow row : result) {
            if (Util.contains(row.getNames(), "HOLZAPFEL Alexander")) {
                assertEquals("GER 8975", row.getSailID());
                assertEquals(60.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(74.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals((int) 11, (int) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getRank());
                assertEquals((double) 11, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).isDiscarded());
            } else if (Util.contains(row.getNames(), "BART Cedric")) {
                assertEquals("SUI 8543", row.getSailID());
                assertEquals(55.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(74.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals((int) 2, (int) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 2).getRank());
                assertEquals((double) 2, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 2).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 2).isDiscarded());
                assertEquals((int) 19, (int) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 3).getRank());
                assertEquals((double) 19, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 3).getScore(), 0.000000001);
                assertEquals(true, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 3).isDiscarded());
            } else if (Util.contains(row.getNames(), "LEWNS Chris")) {
                assertEquals("GBR 9057", row.getSailID());
                assertEquals(64.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(140.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertNull(Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 7).getRank());
                assertEquals("DNF", Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 7).getMaxPointsReason());
                assertEquals((double) 76, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 7).getScore(), 0.000000001);
                assertEquals(true, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 7).isDiscarded());
            }
        }
    }

    @Test
    public void testGetCompetitorRows29er() throws IOException {
        FregHtmlParser parser = new FregHtmlParser();
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("eurocup_29er_29e.htm");
        List<CompetitorRow> result = parser.getRegattaResults(resourceAsStream).getCompetitorResults();
        assertFalse(result.isEmpty());
        assertEquals(62, result.size());
        for (CompetitorRow row : result) {
            if (Util.contains(row.getNames(), "HOLSTE Kim (M1993)")) {
                assertEquals("GER 1864", row.getSailID());
                assertEquals(58.00, row.getScoreAfterDiscarding(), 0.000000001);
                assertEquals(169.00, row.getTotalPointsBeforeDiscarding(), 0.000000001);
                assertEquals((int) 25, (int) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getRank());
                assertEquals((double) 25, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).getScore(), 0.000000001);
                assertEquals(false, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 0).isDiscarded());
                assertEquals((int) 38, (int) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getRank());
                assertEquals((double) 38, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).getScore(), 0.000000001);
                assertEquals(true, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 1).isDiscarded());
                assertNull(Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).getRank());
                assertEquals("DSQ", Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).getMaxPointsReason());
                assertEquals((double) 63, (double) Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).getScore(), 0.000000001);
                assertEquals(true, Util.get(row.getRankAndMaxPointsReasonAndPointsAndDiscarded(), 6).isDiscarded());
            }
        }
    }
}
