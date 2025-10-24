package com.sap.sailing.sailti.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.resultimport.impl.ResultUrlRegistryImpl;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sailing.sailti.resultimport.ScoreCorrectionProviderImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class TrofeoSofia_ParserTest extends AbstractTrofeoSofiaTest {
    @Test
    public void testSimpleParsingSomeSailtiDocuments470Men() throws JAXBException, IOException, SAXException, ParserConfigurationException {
        RegattaResults regattaResults = ParserFactory.INSTANCE.createParser(getInputStream(TROFEO_SOFIA_TESTFILE_XRR_470_MEN), TROFEO_SOFIA_EVENT_NAME).parse();
        assertNotNull(regattaResults);
    }

    @Test
    public void testSimpleParsingSomeSailtiDocuments470Women() throws JAXBException, IOException, SAXException, ParserConfigurationException {
        RegattaResults regattaResults = ParserFactory.INSTANCE.createParser(getInputStream(TROFEO_SOFIA_TESTFILE_XRR_470_WOMEN), TROFEO_SOFIA_EVENT_NAME).parse();
        assertNotNull(regattaResults);
    }

    @Test
    public void testScoreCorrectionProviderFeedingAndHasResults() throws IOException, SAXException,
            ParserConfigurationException, JAXBException, URISyntaxException {
        ResultUrlRegistry resultUrlRegistry = new ResultUrlRegistryImpl(mock(MongoObjectFactory.class),
                mock(DomainObjectFactory.class));
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(
                getTestDocumentProvider(), ParserFactory.INSTANCE, resultUrlRegistry);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        Set<com.sap.sse.common.Util.Pair<String, TimePoint>> resultsForTrofeoSofia470Men = hasResultsFor.get(TROFEO_SOFIA_EVENT_NAME);
        assertNotNull(resultsForTrofeoSofia470Men);
        assertEquals(3, resultsForTrofeoSofia470Men.size());
    }
    
    @Test
    public void testScoreCorrectionProvider() throws Exception {
        ResultUrlRegistry resultUrlRegistry = new ResultUrlRegistryImpl(mock(MongoObjectFactory.class), mock(DomainObjectFactory.class));
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(
                getTestDocumentProvider(), ParserFactory.INSTANCE, resultUrlRegistry);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        Set<com.sap.sse.common.Util.Pair<String, TimePoint>> resultsForKeyWestRaceWeek = hasResultsFor.get(TROFEO_SOFIA_EVENT_NAME);
        com.sap.sse.common.Util.Pair<String, TimePoint> resultFor470Men = null;
        for (com.sap.sse.common.Util.Pair<String, TimePoint> result : resultsForKeyWestRaceWeek) {
            if (result.getA().equals(BOAT_CLASS_470_MEN)) {
                resultFor470Men = result;
                break;
            }
        }
        assertNotNull(resultFor470Men);
        RegattaScoreCorrections _J111Result = scoreCorrectionProvider.getScoreCorrections(TROFEO_SOFIA_EVENT_NAME, BOAT_CLASS_470_MEN,
                resultFor470Men.getB());
        assertNotNull(_J111Result);
        Iterable<ScoreCorrectionsForRace> scoreCorrectionsForRaces = _J111Result.getScoreCorrectionsForRaces();
        assertNotNull(scoreCorrectionsForRaces);
        assertEquals(11, Util.size(scoreCorrectionsForRaces)); 
    }
}
