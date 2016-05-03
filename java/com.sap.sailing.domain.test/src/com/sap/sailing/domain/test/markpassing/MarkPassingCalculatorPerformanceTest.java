package com.sap.sailing.domain.test.markpassing;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Test;

import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.markpassingcalculation.CandidateFinder;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateFinderImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateImpl;
import com.sap.sailing.domain.test.measurements.Measurement;
import com.sap.sailing.domain.test.measurements.MeasurementCase;
import com.sap.sailing.domain.test.measurements.MeasurementXMLFile;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class MarkPassingCalculatorPerformanceTest extends AbstractMockedRaceMarkPassingTest {

    private static LinkedHashMap<String, Long> result = new LinkedHashMap<>();

    public MarkPassingCalculatorPerformanceTest() {
        super();
    }

    protected Random rnd = new Random();
    protected long time;

    @AfterClass
    public static void createXMLFile() throws IOException {
        final MeasurementXMLFile performanceReport = new MeasurementXMLFile(MarkPassingCalculatorPerformanceTest.class);
        for (String key : result.keySet()) {
            MeasurementCase performanceReportCase = performanceReport.addCase(key);
            performanceReportCase.addMeasurement(new Measurement(key, result.get(key)));
        }
        performanceReport.write();
    }

    @Test
    public void testFinder() {
        CandidateFinder f = new CandidateFinderImpl(race);
        List<GPSFix> fixesAdded = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            GPSFixMoving fix = rndFix();
            race.recordFix(ron, fix);
            fixesAdded.add(fix);
        }
        time = System.currentTimeMillis();
        f.getCandidateDeltas(ron, fixesAdded);
        time = System.currentTimeMillis() - time;
        result.put("FinderPerformance", time);
        System.out.println(time);
        Assert.assertTrue("Time expected to be less than 5000ms but was "+time+"ms", time < 5000);
    }

    @Test
    public void testChooser() {
        final long time = timeToAddCandidatesToChooser(500, 1, 25);
        System.out.println(time);
        result.put("ChooserPerformance", time);
        assertTrue(time < 5000);
    }

    private long timeToAddCandidatesToChooser(int numberOfTimesAdding, int numberToAddEachTime, int numberOfRepititions) {
        long totalTime = 0;
        for (int i = 0; i < numberOfRepititions; i++) {
            CandidateChooserImpl c = new CandidateChooserImpl(race);
            for (int j = 0; j < numberOfTimesAdding; j++) {
                List<Candidate> newCandidates = new ArrayList<>();
                for (int k = 0; k < numberToAddEachTime; k++) {
                    newCandidates.add(randomCan());
                }
                time = System.currentTimeMillis();
                c.calculateMarkPassDeltas(ron, newCandidates, new ArrayList<Candidate>());
                totalTime += System.currentTimeMillis() - time;
            }
        }
        return totalTime / numberOfRepititions;
    }

    private CandidateImpl randomCan() {
        final int id = rnd.nextInt(3);
        return new CandidateImpl(id + 1, new MillisecondsTimePoint((long) (rnd.nextDouble() * 200000)),
                0.5 + 0.5 * rnd.nextDouble(),  Util.get(race.getRace().getCourse().getWaypoints(), id));
    }

    private GPSFixMoving rndFix() {
        final DegreePosition position = new DegreePosition(0.001 - rnd.nextDouble() * 0.001,
                0.0002 - rnd.nextDouble() * 0.0004);
        final TimePoint p = new MillisecondsTimePoint((long) (rnd.nextDouble() * 200000));
        final SpeedWithBearing speed = new KnotSpeedWithBearingImpl(rnd.nextInt(11), new DegreeBearingImpl(rnd.nextInt(360)));
        return new GPSFixMovingImpl(position, p, speed);
    }
}
