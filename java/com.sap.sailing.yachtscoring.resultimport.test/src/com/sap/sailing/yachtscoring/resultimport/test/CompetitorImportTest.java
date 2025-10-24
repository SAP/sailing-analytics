package com.sap.sailing.yachtscoring.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.yachtscoring.resultimport.AbstractYachtScoringProvider;
import com.sap.sailing.yachtscoring.resultimport.YachtScoringCompetitorProvider;
import com.sap.sse.common.Util;

public class CompetitorImportTest extends AbstractCharlstonRaceWeek2015Test {
    @Test
    public void simpleCompetitorImportTest() throws FileNotFoundException, IOException, JAXBException, URISyntaxException, SAXException, ParserConfigurationException {
        ResultUrlRegistry resultUrlRegistry = mock(ResultUrlRegistry.class);
        when(resultUrlRegistry.getReadableResultUrls(AbstractYachtScoringProvider.NAME)).thenReturn(Arrays.asList(getClass().getClassLoader().getResource(CHARLSTONRACEWEEK2015_TESTFILE_XRR)));
        final YachtScoringCompetitorProvider competitorProvider = new YachtScoringCompetitorProvider(ParserFactory.INSTANCE, resultUrlRegistry, getTestDocumentProvider());
        assertTrue(competitorProvider.getHasCompetitorsForRegattasInEvent().containsKey(CHARLSTONRACEWEEK2015_EVENT_NAME));
        assertTrue(competitorProvider.getHasCompetitorsForRegattasInEvent().get(CHARLSTONRACEWEEK2015_EVENT_NAME).contains(BOAT_CLASS_J111));
        final Iterable<CompetitorDescriptor> competitorDescriptors = competitorProvider.getCompetitorDescriptors(CHARLSTONRACEWEEK2015_EVENT_NAME, null); // get competitors for all regattas in event
        assertNotNull(competitorDescriptors);
        assertEquals(4449, Util.size(competitorDescriptors));
        final Iterable<CompetitorDescriptor> competitorDescriptorsJ111 = competitorProvider.getCompetitorDescriptors(CHARLSTONRACEWEEK2015_EVENT_NAME, BOAT_CLASS_J111); // get competitors only for J111 regatta
        assertNotNull(competitorDescriptorsJ111);
        assertTrue(Util.size(competitorDescriptors) > Util.size(competitorDescriptorsJ111));
        assertEquals(353, Util.size(competitorDescriptorsJ111));
    }
}
