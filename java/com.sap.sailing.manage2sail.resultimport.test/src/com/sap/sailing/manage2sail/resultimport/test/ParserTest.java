package com.sap.sailing.manage2sail.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionForCompetitorInRace;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.manage2sail.resultimport.ScoreCorrectionProviderImpl;
import com.sap.sailing.resultimport.ResultDocumentDescriptor;
import com.sap.sailing.resultimport.ResultDocumentProvider;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.resultimport.impl.ResultDocumentDescriptorImpl;
import com.sap.sailing.resultimport.impl.ResultUrlRegistryImpl;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ParserTest {
    private static final String SAMPLE_INPUT_NAME_29er = "YES_29er_XRR.xml";
    private static final String SAMPLE_INPUT_NAME_420 = "YES_420_XRR.xml";
    private static final String SAMPLE_INPUT_NAME_470 = "YES_470_XRR.xml";
    private static final String YES_EVENT_NAME = "YES - Young Europeans Sailing 2013";
    private static final String ISAF_ID_29ER = "29ER";
    private static final String ISAF_ID_420 = "420";
    private static final String ISAF_ID_470 = "470";
    
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
                    List<ResultDocumentDescriptor> result = new ArrayList<ResultDocumentDescriptor>();

                    Date _29erDate = DatatypeConverter.parseDateTime("2013-06-10T07:55:08.000Z").getTime();
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_29er),
                            null, new MillisecondsTimePoint(_29erDate), YES_EVENT_NAME , null, ISAF_ID_29ER));

                    Date _420Date = DatatypeConverter.parseDateTime("2013-06-10T07:55:08.000Z").getTime();
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_420),
                            null, new MillisecondsTimePoint(_420Date), YES_EVENT_NAME , null, ISAF_ID_420));

                    Date _470Date = DatatypeConverter.parseDateTime("2013-06-10T07:55:08.000Z").getTime();
                    result.add(new ResultDocumentDescriptorImpl(getInputStream(SAMPLE_INPUT_NAME_470),
                            null, new MillisecondsTimePoint(_470Date), YES_EVENT_NAME , null, ISAF_ID_470));
                    
                    return result;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void testSimpleParsingSomeManage2SailDocuments() throws JAXBException, IOException, SAXException, ParserConfigurationException {
        RegattaResults r1 = ParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_29er), YES_EVENT_NAME).parse();
        assertNotNull(r1);

        RegattaResults r2 = ParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_420), YES_EVENT_NAME).parse();
        assertNotNull(r2);

        RegattaResults r3 = ParserFactory.INSTANCE.createParser(getInputStream(SAMPLE_INPUT_NAME_470), YES_EVENT_NAME).parse();
        assertNotNull(r3);
    }

    @Test
    public void testScoreCorrectionProviderFeedingAndHasResults() throws IOException, SAXException,
            ParserConfigurationException, JAXBException, URISyntaxException {
        ResultUrlRegistry resultUrlRegistry = new ResultUrlRegistryImpl(mock(MongoObjectFactory.class),
                mock(DomainObjectFactory.class));
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(
                getTestDocumentProvider(), ParserFactory.INSTANCE, resultUrlRegistry);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        
        Set<com.sap.sse.common.Util.Pair<String, TimePoint>> resultsForYES = hasResultsFor.get(YES_EVENT_NAME);
        assertNotNull(resultsForYES);

        assertEquals(3, resultsForYES.size());
    }
    
    @Test
    public void testScoreCorrectionProvider() throws Exception {
        ResultUrlRegistry resultUrlRegistry = new ResultUrlRegistryImpl(mock(MongoObjectFactory.class),
                mock(DomainObjectFactory.class));
        ScoreCorrectionProviderImpl scoreCorrectionProvider = new ScoreCorrectionProviderImpl(
                getTestDocumentProvider(), ParserFactory.INSTANCE, resultUrlRegistry);
        Map<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> hasResultsFor = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        Set<com.sap.sse.common.Util.Pair<String, TimePoint>> resultsForYES = hasResultsFor.get(YES_EVENT_NAME);
        com.sap.sse.common.Util.Pair<String, TimePoint> resultFor29er = null;
        for(com.sap.sse.common.Util.Pair<String, TimePoint> result: resultsForYES) {
            if(result.getA().equals(ISAF_ID_29ER)) {
                resultFor29er = result;
                break;
            }
        }
        assertNotNull(resultFor29er);
        
        int numberOfRaceColumnsWithScores = 8; // R9 is in 'planned' state and has no results
        RegattaScoreCorrections _29erResult = scoreCorrectionProvider.getScoreCorrections(YES_EVENT_NAME, ISAF_ID_29ER , resultFor29er.getB());
        assertNotNull(_29erResult);
        Iterable<ScoreCorrectionsForRace> scoreCorrectionsForRaces = _29erResult.getScoreCorrectionsForRaces();
        assertNotNull(scoreCorrectionsForRaces);
        assertEquals(numberOfRaceColumnsWithScores, Util.size(scoreCorrectionsForRaces));
        
        Set<String> raceNamesOrNumbers = new HashSet<>();
        for (ScoreCorrectionsForRace scoreCorrectionForRace : scoreCorrectionsForRaces) {
            raceNamesOrNumbers.add(scoreCorrectionForRace.getRaceNameOrNumber());
        }
        assertEquals(new HashSet<String>(Arrays.asList(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" })), raceNamesOrNumbers);
        {
            final ScoreCorrectionsForRace resultsForR2 = Util.get(scoreCorrectionsForRaces, 1);
            ScoreCorrectionForCompetitorInRace scoreCorrectionR2ForDEN63 = resultsForR2.getScoreCorrectionForCompetitor("DEN 63");
            assertEquals(0, scoreCorrectionR2ForDEN63.getPoints(), 29.00000001);
            assertSame(MaxPointsReason.UFD, scoreCorrectionR2ForDEN63.getMaxPointsReason());
        }
        {
            final ScoreCorrectionsForRace resultsForR5 = Util.get(scoreCorrectionsForRaces, 4);
            ScoreCorrectionForCompetitorInRace scoreCorrectionR5ForDEN63 = resultsForR5.getScoreCorrectionForCompetitor("DEN 63");
            assertEquals(0, scoreCorrectionR5ForDEN63.getPoints(), 35.00000001);
            assertSame(MaxPointsReason.BFD, scoreCorrectionR5ForDEN63.getMaxPointsReason());
        }
   
    }
/**

    <Race RaceID="3c4563fe-b12b-4dec-8b0f-222ee079c7d5" RaceName="R2 Yellow" RaceNumber="2" RaceStartDate="2013-05-18" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="b68b5758-e172-417a-ba4f-bfd7b441940e" RaceName="R1 Yellow" RaceNumber="1" RaceStartDate="2013-05-18" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="5f78543c-e17c-4130-83ac-bfc15c2a5f50" RaceName="R1 Blue" RaceNumber="1" RaceStartDate="2013-05-18" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="753d9dd7-7fdf-40cb-8243-5d70e3771fc1" RaceName="R3 Yellow" RaceNumber="3" RaceStartDate="2013-05-18" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="857d4587-9e52-46e1-ab3a-8892a6aac167" RaceName="R3 Blue" RaceNumber="3" RaceStartDate="2013-05-18" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="1f4d9046-1067-4d2e-bc78-083a072d63e1" RaceName="R4 Yellow" RaceNumber="4" RaceStartDate="2013-05-19" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="87dfaa87-1d6c-4e92-820e-dd8e96ae3690" RaceName="R4 Blue" RaceNumber="4" RaceStartDate="2013-05-19" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="345e28a8-efa4-4ec5-a7a3-410cb251c88c" RaceName="R5 Yellow" RaceNumber="5" RaceStartDate="2013-05-19" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="62ed6be3-281a-44a3-885e-f0edbbf85409" RaceName="R5 Blue" RaceNumber="5" RaceStartDate="2013-05-19" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="8422bca3-bbfd-4761-ba6c-21e25083c38b" RaceName="R6 Yellow" RaceNumber="6" RaceStartDate="2013-05-19" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="52d92322-d4d6-4f52-a66e-92fdde72767e" RaceName="R6 Blue" RaceNumber="6" RaceStartDate="2013-05-19" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="ec270635-04b2-4a12-afed-2018be075f7e" RaceName="R9 Gold" RaceNumber="9" RaceStartDate="2013-05-20" RaceStartTime="12:00:00" RaceStatus="planned" />
    <Race RaceID="7e6ddb42-8db8-4a3e-9358-d7cb40f28a77" RaceName="R9 Silver" RaceNumber="9" RaceStartDate="2013-05-20" RaceStartTime="12:00:00" RaceStatus="planned" />
    <Race RaceID="e18498ce-3885-4a4a-8ad5-be92893cdb55" RaceName="R7 Silver" RaceNumber="7" RaceStartDate="2013-05-20" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="086b0bfc-5712-44b1-91de-c1c7f74f2218" RaceName="R7 Gold" RaceNumber="7" RaceStartDate="2013-05-20" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="7cdec8a2-5175-4736-9296-823f7e1325f7" RaceName="R8 Gold" RaceNumber="8" RaceStartDate="2013-05-20" RaceStartTime="12:00:00" RaceStatus="finished" />
    <Race RaceID="3025aa09-0427-42ab-8f03-6b019b8d4ddd" RaceName="R8 Silver" RaceNumber="8" RaceStartDate="2013-05-20" RaceStartTime="12:00:00" RaceStatus="inprogress" />
    <Race RaceID="37c2ae0f-4889-46ff-aad9-54d964dd193a" RaceName="R2 Blue" RaceNumber="2" RaceStartDate="2013-05-18" RaceStartTime="12:00:00" RaceStatus="finished" />

   */ 
}
