package com.sap.sailing.velum.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.resultimport.ResultDocumentDescriptor;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.resultimport.impl.ResultDocumentDescriptorImpl;
import com.sap.sailing.velum.resultimport.CsvParserFactory;
import com.sap.sailing.velum.resultimport.impl.ScoreCorrectionProviderImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ParserTest {
    private static final String SAMPLE_INPUT_NAME_NIXDORF_POKAL_4WF = "Star_HNVPokal4WF.csv";
    private static final String SAMPLE_INPUT_NAME_NIXDORF_POKAL_FINAL = "Star_HNVPokalFinal.csv";
    private static final String SAMPLE_INPUT_NAME_STARIDM_4WF = "Star_IDM2013n4WF.csv";
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
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_NIXDORF_POKAL_4WF), SAMPLE_INPUT_NAME_NIXDORF_POKAL_4WF, now));
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_NIXDORF_POKAL_FINAL), SAMPLE_INPUT_NAME_NIXDORF_POKAL_FINAL, now));
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_STARIDM_4WF), SAMPLE_INPUT_NAME_STARIDM_4WF, now));
                    return result;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void testParsingNixdorfPokal4RacesDocument() throws Exception {
        RegattaResults parseResults = CsvParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_NIXDORF_POKAL_4WF),
                SAMPLE_INPUT_NAME_NIXDORF_POKAL_4WF, MillisecondsTimePoint.now()).parseResults();
        assertNotNull(parseResults);
    }

    @Test
    public void testParsingNixdorfPokalFinalDocument() throws Exception {
        RegattaResults parseResults = CsvParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_NIXDORF_POKAL_FINAL),
                SAMPLE_INPUT_NAME_NIXDORF_POKAL_FINAL, MillisecondsTimePoint.now()).parseResults();
        assertNotNull(parseResults);
    }

    @Test
    public void testParsingStarIDM4RacesDocument() throws Exception {
        RegattaResults parseResults = CsvParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_STARIDM_4WF), 
                SAMPLE_INPUT_NAME_STARIDM_4WF, MillisecondsTimePoint.now()).parseResults();
        assertNotNull(parseResults);
    }

    @Test
    public void testScoreCorrectionProviderNixdorfFinal() throws Exception {
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(getTestDocumentProvider(),
                CsvParserFactory.INSTANCE);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        RegattaScoreCorrections starResult = scoreCorrectionProvider.getScoreCorrections(SAMPLE_INPUT_NAME_NIXDORF_POKAL_FINAL,
                "Star", hasResultsFor.get(SAMPLE_INPUT_NAME_NIXDORF_POKAL_FINAL).iterator().next().getB());
        assertNotNull(starResult);
        Iterable<ScoreCorrectionsForRace> scoreCorrectionsForRaces = starResult.getScoreCorrectionsForRaces();
        assertNotNull(scoreCorrectionsForRaces);
        assertEquals(5, Util.size(scoreCorrectionsForRaces)); // 5 races
        {
            final ScoreCorrectionsForRace resultsForR2 = Util.get(scoreCorrectionsForRaces, 1);
            assertEquals(40, resultsForR2.getScoreCorrectionForCompetitor("ITA8400").getPoints(), 0.00000001);
            assertSame(MaxPointsReason.DNF, resultsForR2.getScoreCorrectionForCompetitor("ITA8400").getMaxPointsReason());
            assertEquals("Müllejans, Christian+Morf, Karsten",
                    resultsForR2.getScoreCorrectionForCompetitor("ITA8400").getCompetitorName());
        }
        {
            final ScoreCorrectionsForRace resultsForR2 = Util.get(scoreCorrectionsForRaces, 1);
            assertEquals(11, resultsForR2.getScoreCorrectionForCompetitor("GER8055").getPoints(), 0.00000001);
            assertSame(null, resultsForR2.getScoreCorrectionForCompetitor("GER8055").getMaxPointsReason());
        }
    }
}
