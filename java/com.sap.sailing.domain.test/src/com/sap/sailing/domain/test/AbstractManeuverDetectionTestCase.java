package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class AbstractManeuverDetectionTestCase extends OnlineTracTracBasedTest {
    /**
     * A date parser for UTC+2 in format MM/dd/yyyy-HH:mm:ss.
     */
    protected SimpleDateFormat dateFormat;
    protected static final int TACK_TOLERANCE = 7000;
    protected static final int JIBE_TOLERANCE = 7000;
    protected static final int PENALTYCIRCLE_TOLERANCE = 9000;

    protected List<Maneuver> maneuversInvalid;

    public AbstractManeuverDetectionTestCase() throws MalformedURLException, URISyntaxException {
        super();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2")); // will result in CEST
    }

    /**
     * Checks that a maneuver of the type <code>maneuverType</code> to the time of <code>maneuverTimePoint</code> does
     * exist. The new tack is not verified.
     * 
     * @param maneuverList
     *            The whole list of maneuvers to search for that maneuver type.
     * @param maneuverType
     *            The type of maneuver that should have happened to the given time point.
     * @param maneuverTimePoint
     *            The time point the maneuver type should have happened.
     * @param toleranceInMillis
     *            The tolerance of time, the maneuver should have happened in milliseconds.
     */
    protected void assertManeuver(Iterable<Maneuver> maneuverList, ManeuverType maneuverType,
            TimePoint maneuverTimePoint, int toleranceInMillis) {
        assertManeuver(maneuverList, maneuverType, /* new tack doesn't matter */ null,
                maneuverTimePoint, toleranceInMillis);
    }

    /**
     * Checks that a maneuver of the type <code>maneuverType</code> to the time of <code>maneuverTimePoint</code> does
     * exist. The new tack is not verified.
     * 
     * @param maneuverList
     *            The whole list of maneuvers to search for that maneuver type.
     * @param maneuverType
     *            The type of maneuver that should have happened to the given time point.
     * @param newTack
     *            the new tack on which the boat is expected to be after the maneuver; if {@code null}, the new tack is
     *            not verified
     * @param maneuverTimePoint
     *            The time point the maneuver type should have happened.
     * @param toleranceInMillis
     *            The tolerance of time, the maneuver should have happened in milliseconds.
     */
    protected void assertManeuver(Iterable<Maneuver> maneuverList, ManeuverType maneuverType, Tack newTack,
            TimePoint maneuverTimePoint, int toleranceInMillis) {
        for (Maneuver maneuver : maneuverList) {
            assertNotNull(maneuver.getTimePoint());
            if (maneuver.getType() == maneuverType
                    && (newTack == null || newTack == maneuver.getNewTack())
                    && Math.abs(maneuver.getTimePoint().asMillis() - maneuverTimePoint.asMillis()) <= toleranceInMillis) {
                maneuversInvalid.remove(maneuver);
                return;
            }
        }
        fail("Didn't find maneuver type " + maneuverType + " in " + toleranceInMillis + "ms around " + maneuverTimePoint
                + "; maneuvers found instead: " + maneuverList);
    }

    /**
     * Checks if there were additional maneuvers of the given types listed in <code>maneuverTypesFound</code> found,
     * that where not found by {@link ManeuverAnalysisIDMChampionsFinalTest#assertManeuver(List, ManeuverType, MillisecondsTimePoint, int)}.
     * 
     * @param maneuverTypesFound
     *            The maneuver types that should be found.
     * @param maneuversNotDetected
     *            The maneuvers of the types listed in <code>maneuverTypesFound</code> that where not detected by
     *            {@link ManeuverAnalysisIDMChampionsFinalTest#assertManeuver(List, ManeuverType, MillisecondsTimePoint, int)}
     */
    protected void assertAllManeuversOfTypesDetected(List<ManeuverType> maneuverTypesFound, List<Maneuver> maneuversNotDetected) {
        for (Maneuver maneuver : maneuversNotDetected) {
            for (ManeuverType type : maneuverTypesFound) {
                if (maneuver.getType().equals(type)) {
                    fail("The maneuver "+maneuver+" was detected but not expected");
                }
            }
        }
    }
}
