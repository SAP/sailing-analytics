package com.sap.sailing.manage2sail.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import com.sap.sailing.competitorimport.CompetitorProvider;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.manage2sail.RegattaResultDescriptor;
import com.sap.sailing.manage2sail.resultimport.AbstractManage2SailProvider;
import com.sap.sailing.manage2sail.resultimport.CompetitorDocumentProvider;
import com.sap.sailing.manage2sail.resultimport.Manage2SailCompetitorProvider;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sse.common.Util;

/**
 * The Bundle URL connection implementation is really stupid; it doesn't recognize a content type for files exposed by
 * a "bundleresource://" URL. We need to disguise the JSON documents as ".txt" documents for the purpose of this test
 * to allow for the bundle resource class loader to find them.<p>
 * 
 * Ironically, in an Eclipse-based test environment everything seems fine. The problem occurs when a compiled bundle
 * as a JAR file is loaded by an OSGi execution environment, such as during the Maven / Surefire tests.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CompetitorImportTest extends AbstractEventResultJsonServiceTest {
    @Test
    public void simpleCompetitorImportTest() throws FileNotFoundException, IOException, JAXBException, URISyntaxException, SAXException, ParserConfigurationException {
        ResultUrlRegistry resultUrlRegistry = mock(ResultUrlRegistry.class);
        when(resultUrlRegistry.getReadableResultUrls(AbstractManage2SailProvider.NAME)).thenReturn(Arrays.asList(getClass().getClassLoader().getResource(EVENT_RESULTS_JSON+".txt")));
        final Manage2SailCompetitorProvider competitorImporter = new Manage2SailCompetitorProvider(ParserFactory.INSTANCE, resultUrlRegistry);
        assertTrue(competitorImporter.getHasCompetitorsForRegattasInEvent().containsKey("YES - Young Europeans Sailing 2013"));
        assertTrue(competitorImporter.getHasCompetitorsForRegattasInEvent().get("YES - Young Europeans Sailing 2013").contains("29er"));
        final Iterable<CompetitorDescriptor> competitorDescriptors = competitorImporter.getCompetitorDescriptors("YES - Young Europeans Sailing 2013", null); // get competitors for all regattas in event
        assertNotNull(competitorDescriptors);
        assertEquals(1270, Util.size(competitorDescriptors));
        final Iterable<CompetitorDescriptor> competitorDescriptors29er = competitorImporter.getCompetitorDescriptors("YES - Young Europeans Sailing 2013", "29er"); // get competitors only for 29er regatta
        assertNotNull(competitorDescriptors29er);
        assertEquals(134, Util.size(competitorDescriptors29er));
    }

    @Test
    public void simpleCompetitorImportTestNoResultsYet() throws FileNotFoundException, IOException, JAXBException, URISyntaxException, SAXException, ParserConfigurationException {
        ResultUrlRegistry resultUrlRegistry = mock(ResultUrlRegistry.class);
        when(resultUrlRegistry.getReadableResultUrls(AbstractManage2SailProvider.NAME)).thenReturn(Arrays.asList(getClass().getClassLoader().getResource("VSaW_420_Test.json.txt")));
        final CompetitorProvider competitorImporter = new Manage2SailCompetitorProvider(ParserFactory.INSTANCE, resultUrlRegistry) {
            private static final long serialVersionUID = 6450491595924905L;

            @Override
            protected CompetitorDocumentProvider getDocumentProvider() {
                return new CompetitorDocumentProvider(this) {
                    @Override
                    protected URL getDocumentUrlForRegatta(RegattaResultDescriptor regattaResult) {
                        return getClass().getClassLoader().getResource("VSaW_420_Test.xml");
                    }
                };
            }
        };
        assertTrue(competitorImporter.getHasCompetitorsForRegattasInEvent().containsKey("IDJM 2015 - 420er"));
        assertTrue(competitorImporter.getHasCompetitorsForRegattasInEvent().get("IDJM 2015 - 420er").contains("420"));
        final Iterable<CompetitorDescriptor> competitorDescriptors = competitorImporter.getCompetitorDescriptors("IDJM 2015 - 420er", "420"); // get competitors for 420 regatta
        assertNotNull(competitorDescriptors);
        assertEquals(100, Util.size(competitorDescriptors));
    }

    @Test
    public void simpleCompetitorImportTestNoResultsYetAndDivisionEmpty() throws FileNotFoundException, IOException, JAXBException, URISyntaxException, SAXException, ParserConfigurationException {
        ResultUrlRegistry resultUrlRegistry = mock(ResultUrlRegistry.class);
        when(resultUrlRegistry.getReadableResultUrls(AbstractManage2SailProvider.NAME)).thenReturn(Arrays.asList(getClass().getClassLoader().getResource("VSaW_420_Test_EmptyDivision.json.txt")));
        final CompetitorProvider competitorImporter = new Manage2SailCompetitorProvider(ParserFactory.INSTANCE, resultUrlRegistry) {
            private static final long serialVersionUID = -7647297718831584969L;

            @Override
            protected CompetitorDocumentProvider getDocumentProvider() {
                return new CompetitorDocumentProvider(this) {
                    @Override
                    protected URL getDocumentUrlForRegatta(RegattaResultDescriptor regattaResult) {
                        return getClass().getClassLoader().getResource("VSaW_420_Test_EmptyDivision.xml");
                    }
                };
            }
        };
        assertTrue(competitorImporter.getHasCompetitorsForRegattasInEvent().containsKey("IDJM 2015 - 420er"));
        assertTrue(competitorImporter.getHasCompetitorsForRegattasInEvent().get("IDJM 2015 - 420er").contains("420"));
        final Iterable<CompetitorDescriptor> competitorDescriptors = competitorImporter.getCompetitorDescriptors("IDJM 2015 - 420er", null); // get competitors for all regattas in event
        assertNotNull(competitorDescriptors);
        assertEquals(100, Util.size(competitorDescriptors));
    }
}
