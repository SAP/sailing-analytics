package com.sap.sailing.windestimation.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.maneuverdetection.impl.IncrementalManeuverDetectorImpl;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorWithEstimationDataSupportDecoratorImpl;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.domain.windestimation.TimePointAndPositionWithToleranceComparator;
import com.sap.sailing.windestimation.aggregator.msthmm.DistanceAndDurationAwareWindTransitionProbabilitiesCalculator;
import com.sap.sailing.windestimation.aggregator.msthmm.MstGraphLevel;
import com.sap.sailing.windestimation.aggregator.msthmm.MstManeuverGraphGenerator.MstManeuverGraphComponents;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifiersCache;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverFeatures;
import com.sap.sailing.windestimation.model.exception.ModelPersistenceException;
import com.sap.sailing.windestimation.model.regressor.twdtransition.GaussianBasedTwdTransitionDistributionCache;
import com.sap.sailing.windestimation.model.store.ClassPathReadOnlyModelStoreImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class IncrementalMstManeuverGraphGeneratorTest extends OnlineTracTracBasedTest {

    protected final SimpleDateFormat dateFormat;
    private ClassPathReadOnlyModelStoreImpl modelStore;

    public IncrementalMstManeuverGraphGeneratorTest()
            throws MalformedURLException, URISyntaxException, ModelPersistenceException {
        dateFormat = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2")); // will result in CEST
        modelStore = new ClassPathReadOnlyModelStoreImpl("trained_wind_estimation_models", getClass().getClassLoader(),
                IncrementalMstHmmWindEstimationForTrackedRaceTest.modelFilesNames);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"
                + new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(),
                new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        getTrackedRace().waitForManeuverDetectionToFinish();
    }

    @Test
    public void testIncrementalMstManeuverGraphGenerator() {
        GaussianBasedTwdTransitionDistributionCache gaussianBasedTwdTransitionDistributionCache = new GaussianBasedTwdTransitionDistributionCache(
                modelStore, false, Long.MAX_VALUE);
        DistanceAndDurationAwareWindTransitionProbabilitiesCalculator transitionProbabilitiesCalculator = new DistanceAndDurationAwareWindTransitionProbabilitiesCalculator(
                gaussianBasedTwdTransitionDistributionCache, true);
        ManeuverClassifiersCache maneuverClassifiersCache = new ManeuverClassifiersCache(modelStore, true,
                Long.MAX_VALUE, new ManeuverFeatures(false, false, false));
        assertTrue(gaussianBasedTwdTransitionDistributionCache.isReady() && maneuverClassifiersCache.isReady(),
                "Wind estimation models are empty");
        DynamicTrackedRaceImpl trackedRace = getTrackedRace();
        IncrementalMstManeuverGraphGenerator generator = new IncrementalMstManeuverGraphGenerator(
                new CompleteManeuverCurveToManeuverForEstimationConverter(trackedRace, null),
                transitionProbabilitiesCalculator, maneuverClassifiersCache);
        Set<Pair<Position, TimePoint>> cleanManeuvers = new TreeSet<>(
                new TimePointAndPositionWithToleranceComparator());
        for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
            IncrementalManeuverDetectorImpl maneuverDetector = new IncrementalManeuverDetectorImpl(trackedRace,
                    competitor, null);
            TrackTimeInfo trackTimeInfo = maneuverDetector.getTrackTimeInfo();
            ManeuverDetectorWithEstimationDataSupportDecoratorImpl maneuverDetectorWithEstimationDataSupportDecorator = new ManeuverDetectorWithEstimationDataSupportDecoratorImpl(
                    maneuverDetector, null);
            List<CompleteManeuverCurve> maneuverCurves = maneuverDetectorWithEstimationDataSupportDecorator
                    .detectCompleteManeuverCurves();
            CompleteManeuverCurve previousManeuver = null;
            CompleteManeuverCurve currentManeuver = null;
            for (CompleteManeuverCurve nextManeuver : maneuverCurves) {
                generator.add(competitor, nextManeuver, trackTimeInfo);
                if (currentManeuver != null) {
                    ManeuverForEstimation convertedManeuver = generator.getManeuverConverter()
                            .convertCleanManeuverSpotToManeuverForEstimation(currentManeuver, previousManeuver,
                                    nextManeuver, competitor, trackTimeInfo);
                    if (convertedManeuver != null && convertedManeuver.isClean()) {
                        cleanManeuvers.add(new Pair<>(convertedManeuver.getManeuverPosition(),
                                convertedManeuver.getManeuverTimePoint()));
                    }
                }
                previousManeuver = currentManeuver;
                currentManeuver = nextManeuver;
            }
            if (currentManeuver != null) {
                ManeuverForEstimation convertedManeuver = generator.getManeuverConverter()
                        .convertCleanManeuverSpotToManeuverForEstimation(currentManeuver, previousManeuver, null,
                                competitor, trackTimeInfo);
                if (convertedManeuver != null && convertedManeuver.isClean()) {
                    cleanManeuvers.add(new Pair<>(convertedManeuver.getManeuverPosition(),
                            convertedManeuver.getManeuverTimePoint()));
                }
            }
        }
        MstManeuverGraphComponents mstGraph = generator.parseGraph();
        List<ManeuverForEstimation> collectedManeuversFromGraph = new ArrayList<>();
        collectAllManeuversInGraph(mstGraph.getRoot(), collectedManeuversFromGraph);
        Set<Pair<Position, TimePoint>> cleanManeuversFromGraph = new TreeSet<>(
                new TimePointAndPositionWithToleranceComparator());
        collectedManeuversFromGraph.stream()
                .map(maneuver -> new Pair<>(maneuver.getManeuverPosition(), maneuver.getManeuverTimePoint()))
                .forEach(pair -> cleanManeuversFromGraph.add(pair));

        for (Pair<Position, TimePoint> pair : cleanManeuversFromGraph) {
            assertTrue(cleanManeuvers.contains(pair), "Target set does not contain maneuver at " + pair);
        }
        for (Pair<Position, TimePoint> pair : cleanManeuvers) {
            assertTrue(cleanManeuversFromGraph.contains(pair), "Set from graph  does not contain maneuver at " + pair);
        }
    }

    private void collectAllManeuversInGraph(MstGraphLevel fromNode, List<ManeuverForEstimation> collectedManeuvers) {
        ManeuverForEstimation maneuver = fromNode.getManeuver();
        collectedManeuvers.add(maneuver);
        for (MstGraphLevel child : fromNode.getChildren()) {
            collectAllManeuversInGraph(child, collectedManeuvers);
        }
    }

}
