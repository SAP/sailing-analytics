package com.sap.sailing.simulator.test.util;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.impl.PathGeneratorTracTrac;
import com.sap.sse.common.Distance;

public class PathGeneratorTracTracEval {

    private static final double WIND_SCALE = 4.5;

    /** * proxy configuration */
    private static final String LIVE_URI = "tcp://10.18.22.156:1520";

    /** * proxy configuration */
    private static final String STORED_URI = "tcp://10.18.22.156:1521";

    /** * Internationale Deutche Meisterschaft, 49er Race4 */
    private static final String RACE_URL = "http://germanmaster.traclive.dk/events/event_20110929_Internatio/clientparams.php?event=event_20110929_Internatio&race=d1f521fa-ec52-11e0-a523-406186cbf87c";

    private static PathGeneratorTracTrac pathGenerator = null;

    public static void main(String[] args) throws IOException {
        System.out.println("Collection of test methods for evaluating tractrac race access.");
    }    

    public static void initialize() {

        System.setProperty("mongo.port", "10200");
        System.setProperty("http.proxyHost", "proxy.wdf.sap.corp");
        System.setProperty("http.proxyPort", "8080");

        PathGeneratorTracTracEval.pathGenerator = new PathGeneratorTracTrac(null);
        PathGeneratorTracTracEval.pathGenerator.setEvaluationParameters(RACE_URL, LIVE_URI, STORED_URI, WIND_SCALE);
    }

    public void testGetLeg() {

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(0, 0);
        Path leg0 = PathGeneratorTracTracEval.pathGenerator.getPath();
        Assertions.assertEquals(99, leg0.getPathPoints().size());

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(1, 0);
        Path leg1 = PathGeneratorTracTracEval.pathGenerator.getPath();
        Assertions.assertEquals(110, leg1.getPathPoints().size());

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(2, 0);
        Path leg2 = PathGeneratorTracTracEval.pathGenerator.getPath();
        Assertions.assertEquals(151, leg2.getPathPoints().size());

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(3, 0);
        Path leg3 = PathGeneratorTracTracEval.pathGenerator.getPath();
        Assertions.assertEquals(98, leg3.getPathPoints().size());
    }

    public void testGetLegPolyline() {

        Distance maxDistance = new MeterDistance(4.88);

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(0, 0);
        Path legPolyline0 = PathGeneratorTracTracEval.pathGenerator.getPathPolyline(maxDistance);
        Assertions.assertEquals(10, legPolyline0.getPathPoints().size());

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(1, 0);
        Path legPolyline1 = PathGeneratorTracTracEval.pathGenerator.getPathPolyline(maxDistance);
        Assertions.assertEquals(7, legPolyline1.getPathPoints().size());

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(2, 0);
        Path legPolyline2 = PathGeneratorTracTracEval.pathGenerator.getPathPolyline(maxDistance);
        Assertions.assertEquals(8, legPolyline2.getPathPoints().size());

        PathGeneratorTracTracEval.pathGenerator.setSelectionParameters(3, 0);
        Path legPolyline3 = PathGeneratorTracTracEval.pathGenerator.getPathPolyline(maxDistance);
        Assertions.assertEquals(7, legPolyline3.getPathPoints().size());
    }

    public void testGetLegsNames() {

        List<String> legsNames = PathGeneratorTracTracEval.pathGenerator.getLegsNames();

        Assertions.assertEquals(4, legsNames.size());
        Assertions.assertEquals("G1 Start-Finish -> G1 Mark 1", legsNames.get(0));
        Assertions.assertEquals("G1 Mark 1 -> G1 Mark 4", legsNames.get(1));
        Assertions.assertEquals("G1 Mark 4 -> G1 Mark 1", legsNames.get(2));
        Assertions.assertEquals("G1 Mark 1 -> G1 Start-Finish", legsNames.get(3));
    }
}
