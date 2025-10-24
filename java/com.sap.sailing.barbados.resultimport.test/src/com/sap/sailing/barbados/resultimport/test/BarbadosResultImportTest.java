package com.sap.sailing.barbados.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.barbados.resultimport.impl.BarbadosResultSpreadsheet;
import com.sap.sailing.barbados.resultimport.impl.ScoreCorrectionProviderImpl;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.resultimport.ResultDocumentDescriptor;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.resultimport.impl.ResultDocumentDescriptorImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class BarbadosResultImportTest {
    private static final TimePoint NOW = MillisecondsTimePoint.now();
    private static final String SAMPLE_INPUT_NAME_EMPTY_RESULTS = "RESULTS-505Barbados.xlsx";
    private static final String SAMPLE_INPUT_NAME_SOME_RESULTS = "RESULTS2.xlsx";
    private static final String RESOURCES = "resources/";
    private BarbadosResultSpreadsheet spreadsheet;
    private ScoreCorrectionProviderImpl scp;

    private InputStream getInputStream(String filename) throws FileNotFoundException, IOException {
        return new FileInputStream(getFile(filename));
    }

    private File getFile(String filename) {
        return new File(RESOURCES + filename);
    }

    @BeforeEach
    public void setUp() throws FileNotFoundException, IOException, Exception {
        spreadsheet = new BarbadosResultSpreadsheet(getInputStream(SAMPLE_INPUT_NAME_EMPTY_RESULTS));
        scp = new ScoreCorrectionProviderImpl(new ResultDocumentProvider() {
            @Override
            public Iterable<ResultDocumentDescriptor> getResultDocumentDescriptors() throws IOException {
                List<ResultDocumentDescriptor> result = new ArrayList<>();
                result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_EMPTY_RESULTS),
                        SAMPLE_INPUT_NAME_EMPTY_RESULTS, NOW));
                return result;
            }
        });
    }
    
    @Test
    public void testOpenDocument() {
        assertNotNull(spreadsheet);
        RegattaResults regattaResults = spreadsheet.getRegattaResults();
        assertNotNull(regattaResults);
        assertEquals("505", regattaResults.getMetadata().get(ScoreCorrectionProviderImpl.BOATCLASS_NAME_METADATA_PROPERTY));
    }
    
    @Test
    public void testHasResultsThroughScoreCorrectionProvider() throws Exception {
        Map<String, Set<Util.Pair<String, TimePoint>>> hasResultsFor = scp.getHasResultsForBoatClassFromDateByEventName();
        assertNotNull(hasResultsFor);
        assertEquals(1, hasResultsFor.size());
        Entry<String, Set<Util.Pair<String, TimePoint>>> entry = hasResultsFor.entrySet().iterator().next();
        assertEquals("505", entry.getValue().iterator().next().getA());
    }

    @Test
    public void testResultsThroughScoreCorrectionProvider() throws Exception {
        Map<String, Set<Util.Pair<String, TimePoint>>> hasResultsFor = scp.getHasResultsForBoatClassFromDateByEventName();
        String eventName = hasResultsFor.entrySet().iterator().next().getKey();
        String boatClassName = hasResultsFor.entrySet().iterator().next().getValue().iterator().next().getA();
        TimePoint timePoint = hasResultsFor.entrySet().iterator().next().getValue().iterator().next().getB();
        RegattaScoreCorrections result = scp.getScoreCorrections(eventName, boatClassName, timePoint);
        Iterator<ScoreCorrectionsForRace> scfr = result.getScoreCorrectionsForRaces().iterator();
        ScoreCorrectionsForRace scfr1 = scfr.next();
        assertEquals("1", scfr1.getRaceNameOrNumber());
        ScoreCorrectionsForRace scfr2 = scfr.next();
        assertEquals("2", scfr2.getRaceNameOrNumber());
        ScoreCorrectionsForRace scfr3 = scfr.next();
        assertEquals("3", scfr3.getRaceNameOrNumber());
        assertTrue(scfr1.getSailIDs().contains("GER 9113"));
        assertTrue(scfr1.getSailIDs().contains("GER 9112"));
        assertTrue(scfr1.getSailIDs().contains("GER 9110"));
        assertEquals(3, scfr1.getScoreCorrectionForCompetitor("GER 9113").getPoints(), 0.0000000001);
        assertEquals(7, scfr2.getScoreCorrectionForCompetitor("GER 9113").getPoints(), 0.0000000001);
        assertEquals(78, scfr3.getScoreCorrectionForCompetitor("GER 9113").getPoints(), 0.0000000001);
        assertSame(MaxPointsReason.DNF, scfr3.getScoreCorrectionForCompetitor("GER 9113").getMaxPointsReason());
        
        assertEquals(2, scfr1.getScoreCorrectionForCompetitor("GER 9112").getPoints(), 0.0000000001);
        assertEquals(9, scfr2.getScoreCorrectionForCompetitor("GER 9112").getPoints(), 0.0000000001);
        assertEquals(9, scfr3.getScoreCorrectionForCompetitor("GER 9112").getPoints(), 0.0000000001);
        assertNull(scfr3.getScoreCorrectionForCompetitor("GER 9112").getMaxPointsReason());
        
        assertEquals(5, scfr1.getScoreCorrectionForCompetitor("GER 9110").getPoints(), 0.0000000001);
        assertEquals(7, scfr2.getScoreCorrectionForCompetitor("GER 9110").getPoints(), 0.0000000001);
        assertEquals(78, scfr3.getScoreCorrectionForCompetitor("GER 9110").getPoints(), 0.0000000001);
        assertSame(MaxPointsReason.OCS, scfr3.getScoreCorrectionForCompetitor("GER 9110").getMaxPointsReason());
    }

    @Test
    public void testResultsThroughScoreCorrectionProviderWithSomeExperimentalResults() throws Exception {
        ScoreCorrectionProviderImpl scp2 = new ScoreCorrectionProviderImpl(new ResultDocumentProvider() {
            @Override
            public Iterable<ResultDocumentDescriptor> getResultDocumentDescriptors() throws IOException {
                List<ResultDocumentDescriptor> result = new ArrayList<>();
                result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_SOME_RESULTS),
                        SAMPLE_INPUT_NAME_SOME_RESULTS, NOW));
                return result;
            }
        });
        Map<String, Set<Util.Pair<String, TimePoint>>> hasResultsFor = scp2.getHasResultsForBoatClassFromDateByEventName();
        String eventName = hasResultsFor.entrySet().iterator().next().getKey();
        String boatClassName = hasResultsFor.entrySet().iterator().next().getValue().iterator().next().getA();
        TimePoint timePoint = hasResultsFor.entrySet().iterator().next().getValue().iterator().next().getB();
        RegattaScoreCorrections result = scp2.getScoreCorrections(eventName, boatClassName, timePoint);
        Iterator<ScoreCorrectionsForRace> scfr = result.getScoreCorrectionsForRaces().iterator();
        ScoreCorrectionsForRace scfr1 = scfr.next();
        assertEquals("1", scfr1.getRaceNameOrNumber());
        ScoreCorrectionsForRace scfr2 = scfr.next();
        assertEquals("2", scfr2.getRaceNameOrNumber());
        ScoreCorrectionsForRace scfr3 = scfr.next();
        assertEquals("3", scfr3.getRaceNameOrNumber());
        assertTrue(scfr1.getSailIDs().contains("FRA 9067"));
        assertTrue(scfr1.getSailIDs().contains("GER 9071"));
        assertTrue(scfr1.getSailIDs().contains("GBR 9079"));
        assertEquals(18, scfr1.getScoreCorrectionForCompetitor("FRA 9067").getPoints(), 0.0000000001);
        assertEquals(2, scfr2.getScoreCorrectionForCompetitor("FRA 9067").getPoints(), 0.0000000001);
        assertEquals(18, scfr3.getScoreCorrectionForCompetitor("FRA 9067").getPoints(), 0.0000000001);
        assertNull(scfr3.getScoreCorrectionForCompetitor("FRA 9067").getMaxPointsReason());
        
        assertEquals(17, scfr1.getScoreCorrectionForCompetitor("GER 9071").getPoints(), 0.0000000001);
        assertEquals(79, scfr2.getScoreCorrectionForCompetitor("GER 9071").getPoints(), 0.0000000001);
        assertEquals(17, scfr3.getScoreCorrectionForCompetitor("GER 9071").getPoints(), 0.0000000001);
        assertSame(MaxPointsReason.DNF, scfr2.getScoreCorrectionForCompetitor("GER 9071").getMaxPointsReason());
        
        assertEquals(16, scfr1.getScoreCorrectionForCompetitor("GBR 9079").getPoints(), 0.0000000001);
        assertEquals(6, scfr2.getScoreCorrectionForCompetitor("GBR 9079").getPoints(), 0.0000000001);
        assertEquals(14, scfr3.getScoreCorrectionForCompetitor("GBR 9079").getPoints(), 0.0000000001);
    }
}
