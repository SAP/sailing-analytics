package com.sap.sailing.ess40.resultimport.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionForCompetitorInRace;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.ess40.resultimport.impl.ScoreCorrectionProviderImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class SimpleESS40ResultImportTest {
    @Test
    public void testProviderFound() throws Exception {
        ScoreCorrectionProvider scp = new ScoreCorrectionProviderImpl();
        Map<String, Set<Util.Pair<String, TimePoint>>> hasResult = scp.getHasResultsForBoatClassFromDateByEventName();
        assertTrue(hasResult.containsKey("qingdao"));
    }

    @Test
    public void testQingdaoResults() throws Exception {
        ScoreCorrectionProvider scp = new ScoreCorrectionProviderImpl();
        Map<String, Set<Util.Pair<String, TimePoint>>> hasResult = scp.getHasResultsForBoatClassFromDateByEventName();
        RegattaScoreCorrections qingdaoResults = scp.getScoreCorrections("qingdao", "extreme 40", hasResult.get("qingdao").iterator().next().getB());
        assertNotNull(qingdaoResults);
        for (ScoreCorrectionsForRace scfr : qingdaoResults.getScoreCorrectionsForRaces()) {
            if (scfr.getRaceNameOrNumber().equals("1")) {
                final ScoreCorrectionForCompetitorInRace theWave = scfr.getScoreCorrectionForCompetitor("The Wave - Muscat");
                assertNull(theWave.getMaxPointsReason());
                assertEquals(7, theWave.getPoints(), 0.00000001);
            }
        }
        // TODO continue here and require results to match what we see in the CSV
    }
}
