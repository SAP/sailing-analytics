package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.FuzzyBoatClassNameMatcher;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

public class FuzzyBoatClassNameMatcherTest {
    private final FuzzyBoatClassNameMatcher matcher = new FuzzyBoatClassNameMatcher();
    
    @Test
    public void testFKMenVsWomenBasedOnLeaderboardName() {
        final List<Pair<String, Pair<String, Date>>> results = Arrays.asList(
                r("Kieler Woche 2023", "ILCA7", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "470, Mixed", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "FK, Women", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "FK, Men", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "ILCA6", TimePoint.now().asDate()));
        matcher.sortOfficialResultsByRelevance(bc("FK"), results, /* leaderboardName */ "Women's Kite");
        assertEquals("FK, Women", results.get(0).getB().getA());
    }

    @Test
    public void testFKMenVsWomenBasedOnLeaderboardName2() {
        final List<Pair<String, Pair<String, Date>>> results = Arrays.asList(
                r("Kieler Woche 2023", "ILCA7", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "470, Mixed", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "FK, Women", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "FK, Men", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "ILCA6", TimePoint.now().asDate()));
        matcher.sortOfficialResultsByRelevance(bc("FK"), results, /* leaderboardName */ "Men's Kite");
        assertEquals("FK, Men", results.get(0).getB().getA());
    }

    @Test
    public void testIQFWomen() {
        final List<Pair<String, Pair<String, Date>>> results = Arrays.asList(
                r("Kieler Woche 2023", "ILCA7", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "470, Mixed", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "FK, Women", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "IQF, Women", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "IQF, Men", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "FK, Men", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "ILCA6", TimePoint.now().asDate()));
        matcher.sortOfficialResultsByRelevance(bc("iQFOil Women"), results, /* leaderboardName */ "Women's Windsurfing");
        assertEquals("IQF, Women", results.get(0).getB().getA());
    }

    @Test
    public void simpleFullMatchTest() {
        final List<Pair<String, Pair<String, Date>>> results = Arrays.asList(
                r("Kieler Woche 2023", "49er", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "N17F", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "ILCA6", TimePoint.now().asDate()));
        matcher.sortOfficialResultsByRelevance(bc("N17F"), results, /* leaderboardName */ null);
        assertEquals("N17F", results.get(0).getB().getA());
    }

    @Test
    public void simplePartialMatchTest() {
        final List<Pair<String, Pair<String, Date>>> results = Arrays.asList(
                r("Kieler Woche 2023", "49er", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "N17 Foiling", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "ILCA6", TimePoint.now().asDate()));
        matcher.sortOfficialResultsByRelevance(bc("N17F"), results, /* leaderboardName */ null);
        assertEquals("N17 Foiling", results.get(0).getB().getA());
    }

    @Test
    public void multiClassMatchTest() {
        final List<Pair<String, Pair<String, Date>>> results = Arrays.asList(
                r("Kieler Woche 2023", "49er", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "Europe", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "49er", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "49er", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "N17 Foiling", TimePoint.now().asDate()),
                r("Kieler Woche 2023", "ILCA6", TimePoint.now().asDate()));
        matcher.sortOfficialResultsByRelevance(bc("Europe Int."), results, /* leaderboardName */ null);
        assertEquals("Europe", results.get(0).getB().getA());
    }

    private BoatClassDTO bc(String name) {
        return new BoatClassDTO(name, Distance.NULL, Distance.NULL);
    }

    private Pair<String, Pair<String, Date>> r(String eventNameFromResult, String boatClassNameFromResult, Date timeStamp) {
        return new Pair<>(eventNameFromResult, new Pair<>(boatClassNameFromResult, timeStamp));
    }
}
