package com.sap.sailing.sailti.resultimport.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.resultimport.impl.ResultUrlRegistryImpl;
import com.sap.sailing.sailti.resultimport.EventResultDescriptor;
import com.sap.sailing.sailti.resultimport.SailtiEventResultsParserImpl;
import com.sap.sailing.sailti.resultimport.ScoreCorrectionProviderImpl;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.HttpUrlConnectionHelper;

public class TestParsingEventHtml {
    @Test
    public void parseOldEventHtml() throws IOException {
        final EventResultDescriptor eventResults = new SailtiEventResultsParserImpl(new URL("http://localhost/12345")).getEventResult(getClass().getResourceAsStream("/EventTestHtml.xml"));
        assertEquals(10, eventResults.getRegattaResults().size());
    }

    @Test
    public void parseOldEventHtmlOnline() throws IOException {
        final URL url = new URL("https://www.trofeoprincesasofia.org/default/racesajax/race-results-ws/token/2360");
        final EventResultDescriptor eventResults = new SailtiEventResultsParserImpl(url)
                .getEventResult(HttpUrlConnectionHelper.redirectConnection(url).getInputStream());
        assertEquals(10, eventResults.getRegattaResults().size());
    }
    
    @Test
    public void parseXrrOfOldEventHtmlOnline() throws Exception {
        final URL url = new URL("https://www.trofeoprincesasofia.org/default/racesajax/race-results-ws/token/2360");
        final ResultUrlRegistry resultUrlRegistry = mock(ResultUrlRegistryImpl.class);
        final ScoreCorrectionProvider scoreCorrectionProvider = new ScoreCorrectionProviderImpl(ParserFactory.INSTANCE, resultUrlRegistry);
        when(resultUrlRegistry.getAllResultUrls(scoreCorrectionProvider.getName())).thenReturn(Collections.singleton(url));
        when(resultUrlRegistry.getReadableResultUrls(scoreCorrectionProvider.getName())).thenReturn(Collections.singleton(url));
        resultUrlRegistry.registerResultUrl(scoreCorrectionProvider.getName(), url);
        final Map<String, Set<Pair<String, TimePoint>>> results = scoreCorrectionProvider.getHasResultsForBoatClassFromDateByEventName();
        assertEquals(1, results.size());
        final String eventName = results.keySet().iterator().next();
        assertEquals("50 Trofeo S.A.R. Princesa Sofía IBEROSTAR", eventName);
        final Set<Pair<String, TimePoint>> resultsPerBoatClass = results.values().iterator().next();
        assertEquals(10, resultsPerBoatClass.size());
        for (final Pair<String, TimePoint> boatClassAndResultTimePoint : resultsPerBoatClass) {
            final RegattaScoreCorrections result = scoreCorrectionProvider.getScoreCorrections(eventName, boatClassAndResultTimePoint.getA(), boatClassAndResultTimePoint.getB());
            assertNotNull(
                    result,
                    "Expected result for event " + eventName + " and boat class " + boatClassAndResultTimePoint.getA()
                            + " for time point " + boatClassAndResultTimePoint.getB() + " to not be null");
        }
    }
}
