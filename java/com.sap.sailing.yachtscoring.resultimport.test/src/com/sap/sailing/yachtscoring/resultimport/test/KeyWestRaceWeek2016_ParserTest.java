package com.sap.sailing.yachtscoring.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.resultimport.ResultDocumentDescriptor;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.resultimport.impl.ResultDocumentDescriptorImpl;
import com.sap.sailing.resultimport.impl.ResultUrlRegistryImpl;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sailing.yachtscoring.resultimport.ScoreCorrectionProviderImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class KeyWestRaceWeek2016_ParserTest extends AbstractYachtScoringEventResourceTest{
    private static final String KEYWESTRACEWEEEK2016_TESTFILE_XRR = "event1390_KeyWestRaceWeek2016_xrr.xml";
    private static final String KEYWESTRACEWEEEK2016_EVENT_NAME = "Quantum Key West Race Week 2016, Key West, FL, USA";

    private static final String BOAT_CLASS_J88 = "J 88";
    private static final String BOAT_CLASS_MELGES24 = "Melges 24";
    

    private ResultDocumentProvider getTestDocumentProvider() {
        return new ResultDocumentProvider() {
            @Override
            public Iterable<ResultDocumentDescriptor> getResultDocumentDescriptors() throws IOException {
                try {
                    List<ResultDocumentDescriptor> result = new ArrayList<ResultDocumentDescriptor>();

                    Date _J111Date = DatatypeConverter.parseDateTime("2016-01-19T12:55:08.000Z").getTime();
                    Date _Melges24Date = DatatypeConverter.parseDateTime("2016-01-19T12:55:08.000Z").getTime();
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(KEYWESTRACEWEEEK2016_TESTFILE_XRR),
                            null, new MillisecondsTimePoint(_J111Date), KEYWESTRACEWEEEK2016_EVENT_NAME , null, BOAT_CLASS_J88));
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(KEYWESTRACEWEEEK2016_TESTFILE_XRR),
                            null, new MillisecondsTimePoint(_Melges24Date), KEYWESTRACEWEEEK2016_EVENT_NAME , null, BOAT_CLASS_MELGES24));
                    
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void testSimpleParsingSomeYachtscoringDocuments() throws JAXBException, IOException, SAXException, ParserConfigurationException {
        RegattaResults regattaResults = ParserFactory.INSTANCE.createParser(getInputStream(KEYWESTRACEWEEEK2016_TESTFILE_XRR), KEYWESTRACEWEEEK2016_EVENT_NAME).parse();
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
        Set<com.sap.sse.common.Util.Pair<String, TimePoint>> resultsForKeyWestRaceWeek = hasResultsFor.get(KEYWESTRACEWEEEK2016_EVENT_NAME);
        assertNotNull(resultsForKeyWestRaceWeek);
        assertEquals(2, resultsForKeyWestRaceWeek.size());
    }
    
    @Test
    public void testScoreCorrectionProvider() throws Exception {
        ResultUrlRegistry resultUrlRegistry = new ResultUrlRegistryImpl(mock(MongoObjectFactory.class), mock(DomainObjectFactory.class));
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(
                getTestDocumentProvider(), ParserFactory.INSTANCE, resultUrlRegistry);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        Set<com.sap.sse.common.Util.Pair<String, TimePoint>> resultsForKeyWestRaceWeek = hasResultsFor.get(KEYWESTRACEWEEEK2016_EVENT_NAME);
        com.sap.sse.common.Util.Pair<String, TimePoint> resultForJ88 = null;
        for(com.sap.sse.common.Util.Pair<String, TimePoint> result: resultsForKeyWestRaceWeek) {
            if(result.getA().equals(BOAT_CLASS_J88)) {
                resultForJ88 = result;
                break;
            }
        }
        assertNotNull(resultForJ88);
        RegattaScoreCorrections _J88Result = scoreCorrectionProvider.getScoreCorrections(KEYWESTRACEWEEEK2016_EVENT_NAME, BOAT_CLASS_J88,
                resultForJ88.getB());
        assertNotNull(_J88Result);
        Iterable<ScoreCorrectionsForRace> scoreCorrectionsForRaces = _J88Result.getScoreCorrectionsForRaces();
        assertNotNull(scoreCorrectionsForRaces);
        assertEquals(10, Util.size(scoreCorrectionsForRaces)); 
    }
}
