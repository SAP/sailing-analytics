package com.sap.sailing.domain.test.markpassing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl.Stats;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl.Stats.CompetitorStats;
import com.sap.sailing.domain.test.measurements.Measurement;
import com.sap.sailing.domain.test.measurements.MeasurementCase;
import com.sap.sailing.domain.test.measurements.MeasurementXMLFile;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class MarkPassingCalculatorWithTrackersOnStartBoatTest extends AbstractExportedPositionsBasedTest {
    private DynamicTrackedRaceImpl trackedRace;
    private Map<Competitor, Iterable<MarkPassing>> markPassings;
    private CandidateChooserImpl candidateChooser;
    private TimePoint startOfSetup;
    
    @BeforeEach
    public void setUp() throws IOException, ParseException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        startOfSetup = MillisecondsTimePoint.now();
        trackedRace = readRace("/MoevensteinCompetitorPositions.json.gz", "/MoevensteinMarkPositions.json.gz", new BoatClassImpl(BoatClassMasterdata.J70));
        markPassings = new HashMap<>();
        for (final Competitor competitor : trackedRace.getRace().getCompetitors()) {
            markPassings.put(competitor, trackedRace.getMarkPassings(competitor, /* waitForLatest */ true));
        }
        final Field markPassingCalculatorField = TrackedRaceImpl.class.getDeclaredField("markPassingCalculator");
        markPassingCalculatorField.setAccessible(true);
        final Object markPassingCalculator = markPassingCalculatorField.get(trackedRace);
        Field candidateChooserField = MarkPassingCalculator.class.getDeclaredField("chooser");
        candidateChooserField.setAccessible(true);
        candidateChooser = (CandidateChooserImpl) candidateChooserField.get(markPassingCalculator);
    }
    
    @Override
    protected Course createCourse(Map<String, Mark> marksByName) {
        final Map<String, ControlPoint> controlPoints = new HashMap<>(marksByName);
        controlPoints.put("Start/Ziel", new ControlPointWithTwoMarksImpl(marksByName.get("G2"), marksByName.get("MEU"),
                "Start/Ziel", "Start/Ziel"));
        controlPoints.put("Gate",
                new ControlPointWithTwoMarksImpl(marksByName.get("G1"), marksByName.get("G2"), "Gate", "Gate"));
        return createCourse(controlPoints,
                wp(controlPoints, "Start/Ziel", PassingInstruction.Line),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Gate", PassingInstruction.Gate),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Gate", PassingInstruction.Gate),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Start/Ziel", PassingInstruction.Line),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Gate", PassingInstruction.Gate),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Gate", PassingInstruction.Gate),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Start/Ziel", PassingInstruction.Line),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Gate", PassingInstruction.Gate),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Gate", PassingInstruction.Gate),
                wp(controlPoints, "LUV", PassingInstruction.Port),
                wp(controlPoints, "Start/Ziel", PassingInstruction.Line));
    }

    @Test
    public void testRaceCreation() throws IOException {
        final Duration timeSinceStartOfSetup = startOfSetup.until(MillisecondsTimePoint.now());
        assertNotNull(trackedRace);
        assertEquals(5, Util.size(trackedRace.getRace().getCompetitors()));
        assertEquals(5, markPassings.size());
        final Stats stats = candidateChooser.getStats();
        final MeasurementXMLFile performanceReport = new MeasurementXMLFile(this.getClass());
        final MeasurementCase performanceReportCase = performanceReport.addCase(getClass().getSimpleName());
        performanceReportCase.addMeasurement(new Measurement("TimeForSetupInMillis", timeSinceStartOfSetup.asMillis()));
        performanceReportCase.addMeasurement(new Measurement("TotalNumberOfCandidates", stats.getTotalNumberOfCandidates()));
        performanceReportCase.addMeasurement(new Measurement("TotalNumberOfCandidatesAfterHighestProbabilityInShortTimeFilter", stats.getTotalNumberOfCandidatesAfterHighestProbabilityInShortTimeFilter()));
        performanceReportCase.addMeasurement(new Measurement("TotalFilterRatioStage1",
                (double) stats.getTotalNumberOfCandidatesAfterHighestProbabilityInShortTimeFilter() / (double) stats.getTotalNumberOfCandidates()));
        performanceReportCase.addMeasurement(new Measurement("TotalNumberOfCandidatesAfterBoundingBoxFilter", stats.getTotalNumberOfCandidatesAfterBoundingBoxFilter()));
        performanceReportCase.addMeasurement(new Measurement("TotalFilterRatioStage2",
                (double) stats.getTotalNumberOfCandidatesAfterBoundingBoxFilter() / (double) stats.getTotalNumberOfCandidatesAfterHighestProbabilityInShortTimeFilter()));
        performanceReportCase.addMeasurement(new Measurement("TotalFilterRatio",
                (double) stats.getTotalNumberOfCandidatesAfterBoundingBoxFilter() / (double) stats.getTotalNumberOfCandidates()));
        performanceReportCase.addMeasurement(new Measurement("TotalNumberOfEdges", stats.getTotalNumberOfEdges()));
        for (final Entry<Competitor, CompetitorStats> competitorStats : stats.getPerCompetitorStats().entrySet()) {
            performanceReportCase.addMeasurement(new Measurement(competitorStats.getKey().getName()+"-Candidates", competitorStats.getValue().getCandidates()));
            performanceReportCase.addMeasurement(new Measurement(competitorStats.getKey().getName()+"-CandidatesAfterHighestProbabilityInShortTimeFilter", competitorStats.getValue().getCandidatesAfterHighestProbabilityInShortTimeFilter()));
            performanceReportCase.addMeasurement(new Measurement(competitorStats.getKey().getName()+"-FilterRatioStage1",
                    (double) competitorStats.getValue().getCandidatesAfterHighestProbabilityInShortTimeFilter() / (double) competitorStats.getValue().getCandidates()));
            performanceReportCase.addMeasurement(new Measurement(competitorStats.getKey().getName()+"-CandidatesAfterBoundingBoxFilter", competitorStats.getValue().getCandidatesAfterBoundingBoxFilter()));
            performanceReportCase.addMeasurement(new Measurement(competitorStats.getKey().getName()+"-FilterRatioStage2",
                    (double) competitorStats.getValue().getCandidatesAfterBoundingBoxFilter() / (double) competitorStats.getValue().getCandidatesAfterHighestProbabilityInShortTimeFilter()));
            performanceReportCase.addMeasurement(new Measurement(competitorStats.getKey().getName()+"-FilterRatio",
                    (double) competitorStats.getValue().getCandidatesAfterBoundingBoxFilter() / (double) competitorStats.getValue().getCandidates()));
            performanceReportCase.addMeasurement(new Measurement(competitorStats.getKey().getName()+"-Edges", competitorStats.getValue().getEdges()));
        }
        performanceReport.write();
    }
}
