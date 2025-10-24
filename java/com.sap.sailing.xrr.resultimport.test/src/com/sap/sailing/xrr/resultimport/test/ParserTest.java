package com.sap.sailing.xrr.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.resultimport.ResultDocumentDescriptor;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.resultimport.impl.ResultDocumentDescriptorImpl;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.resultimport.impl.ScoreCorrectionProviderImpl;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ParserTest {
    private static final String SAMPLE_INPUT_NAME_LASER = "Laser_20130403_111958.xml";
    private static final String SAMPLE_INPUT_NAME_STAR = "Star_20130403_112020.xml";
    private static final String SAMPLE_INPUT_NAME_MELBOURNE = "melbourne_results_actual.xml";
    private static final String SAMPLE_INPUT_NAME_SPLIT_FLEET = "470 M_20130402_144542.xml";
    private static final String RESOURCES = "resources/";

    private InputStream getInputStream(String filename) throws FileNotFoundException, IOException {
        return new FileInputStream(getFile(filename));
    }

    private File getFile(String filename) {
        return new File(RESOURCES + filename);
    }

    private ResultDocumentProvider getTestDocumentProvider() {
        return new ResultDocumentProvider() {
            @Override
            public Iterable<ResultDocumentDescriptor> getResultDocumentDescriptors() throws IOException {
                try {
                    List<ResultDocumentDescriptor> result = new ArrayList<>();
                    TimePoint now = MillisecondsTimePoint.now();
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_LASER), SAMPLE_INPUT_NAME_LASER, now));
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_STAR), SAMPLE_INPUT_NAME_STAR, now));
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_MELBOURNE), SAMPLE_INPUT_NAME_MELBOURNE, now));
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_SPLIT_FLEET), SAMPLE_INPUT_NAME_SPLIT_FLEET, now));
                    return result;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void testSimpleParsingSomeLaserDocument() throws JAXBException, IOException, SAXException, ParserConfigurationException {
        RegattaResults o = ParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_LASER), SAMPLE_INPUT_NAME_LASER).parse();
        assertNotNull(o);
    }

    @Test
    public void testSimpleParsingSomeStarDocument() throws JAXBException, IOException, SAXException, ParserConfigurationException {
        RegattaResults o = ParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_STAR), SAMPLE_INPUT_NAME_STAR).parse();
        assertNotNull(o);
    }

    @Test
    public void testScoreCorrectionProviderFeedingAndHasResults() throws IOException, SAXException,
            ParserConfigurationException, JAXBException, URISyntaxException {
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(getTestDocumentProvider(),
                ParserFactory.INSTANCE);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        assertTrue(hasResultsFor.containsKey("Star Men"));
        // expecting 2013-04-03T13:20:23.000+0200 = 2013-04-03T11:20:23.000Z
        Calendar cal = new GregorianCalendar(2013, /* 3 means April; zero-based */ 3, 3, 11, 20, 23);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        TimePoint expectedTimePoint = new MillisecondsTimePoint(cal.getTime());
        TimePoint xrrTimePoint = hasResultsFor.get("Star Men").iterator().next().getB();
        assertEquals(expectedTimePoint, xrrTimePoint);
        assertTrue(hasResultsFor.containsKey("Laser Men"));
    }
    
    @Test
    public void testScoreCorrectionProvider() throws IOException, SAXException,
            ParserConfigurationException, JAXBException, URISyntaxException {
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(getTestDocumentProvider(),
                ParserFactory.INSTANCE);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        RegattaScoreCorrections starResult = scoreCorrectionProvider.getScoreCorrections("Star Men", "STR", hasResultsFor.get("Star Men").iterator().next().getB());
        assertNotNull(starResult);
        Iterable<ScoreCorrectionsForRace> scoreCorrectionsForRaces = starResult.getScoreCorrectionsForRaces();
        assertNotNull(scoreCorrectionsForRaces);
        assertEquals(11, Util.size(scoreCorrectionsForRaces)); // 10 regular races, one medal race
        {
            final ScoreCorrectionsForRace resultsForR6 = Util.get(scoreCorrectionsForRaces, 5);
            assertEquals(11, resultsForR6.getScoreCorrectionForCompetitor("NOR").getPoints(), 0.00000001);
            assertSame(MaxPointsReason.NONE, resultsForR6.getScoreCorrectionForCompetitor("NOR")
                    .getMaxPointsReason());
            assertEquals("Melleby, Eivind + Pedersen, Petter Morland",
                    resultsForR6.getScoreCorrectionForCompetitor("NOR").getCompetitorName());
        }
        {
            final ScoreCorrectionsForRace resultsForM1 = Util.get(scoreCorrectionsForRaces, 10);
            assertEquals(10, resultsForM1.getScoreCorrectionForCompetitor("NOR").getPoints(), 0.00000001);
            assertSame(MaxPointsReason.NONE, resultsForM1.getScoreCorrectionForCompetitor("NOR")
                    .getMaxPointsReason());
            assertEquals("Melleby, Eivind + Pedersen, Petter Morland",
                    resultsForM1.getScoreCorrectionForCompetitor("NOR").getCompetitorName());
        }
        {
            final ScoreCorrectionsForRace resultsForR7 = Util.get(scoreCorrectionsForRaces, 6);
            assertEquals(17, resultsForR7.getScoreCorrectionForCompetitor("IRL").getPoints(), 0.00000001);
            assertSame(MaxPointsReason.DSQ, resultsForR7.getScoreCorrectionForCompetitor("IRL")
                    .getMaxPointsReason());
            assertEquals("O'leary, Peter + Burrows, David",
                    resultsForR7.getScoreCorrectionForCompetitor("IRL").getCompetitorName());
        }
    }

    @Test
    public void testScoreCorrectionProviderForSplitFleet() throws IOException, SAXException,
            ParserConfigurationException, JAXBException, URISyntaxException {
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(getTestDocumentProvider(),
                ParserFactory.INSTANCE);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        RegattaScoreCorrections _470Result = scoreCorrectionProvider.getScoreCorrections("470 Men", "470", hasResultsFor.get("470 Men").iterator().next().getB());
        assertNotNull(_470Result);
        Iterable<ScoreCorrectionsForRace> scoreCorrectionsForRaces = _470Result.getScoreCorrectionsForRaces();
        assertNotNull(scoreCorrectionsForRaces);
        assertEquals(4, Util.size(scoreCorrectionsForRaces)); // four split-fleet races (eight races altogether)
        Set<String> raceNamesOrNumbers = new HashSet<>();
        for (ScoreCorrectionsForRace scoreCorrectionForRace : scoreCorrectionsForRaces) {
            raceNamesOrNumbers.add(scoreCorrectionForRace.getRaceNameOrNumber());
        }
        assertEquals(new HashSet<String>(Arrays.asList(new String[] { "1", "2", "3", "4" })), raceNamesOrNumbers);
        {
            final ScoreCorrectionsForRace resultsForR1 = Util.get(scoreCorrectionsForRaces, 0);
            assertEquals(0, resultsForR1.getScoreCorrectionForCompetitor("AUT-3").getPoints(), 0.00000001);
            assertSame(MaxPointsReason.NONE, resultsForR1.getScoreCorrectionForCompetitor("AUT-3")
                    .getMaxPointsReason());
        }
    }
}
