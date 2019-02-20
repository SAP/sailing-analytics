package com.sap.sailing.windestimation.integration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.impl.IncrementalManeuverDetectorImpl;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorWithEstimationDataSupportDecoratorImpl;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.domain.windestimation.TimePointAndPositionWithToleranceComparator;
import com.sap.sailing.windestimation.ManeuverBasedWindEstimationComponentImpl;
import com.sap.sailing.windestimation.aggregator.ManeuverClassificationsAggregatorFactory;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.WindQuality;
import com.sap.sailing.windestimation.data.transformer.CompleteManeuverCurveWithEstimationDataToManeuverForEstimationTransformer;
import com.sap.sailing.windestimation.model.exception.ModelPersistenceException;
import com.sap.sailing.windestimation.model.store.ClassPathReadOnlyModelStoreImpl;
import com.sap.sailing.windestimation.preprocessing.RaceElementsFilteringPreprocessingPipelineImpl;
import com.sap.sailing.windestimation.windinference.DummyBasedTwsCalculatorImpl;
import com.sap.sailing.windestimation.windinference.MiddleCourseBasedTwdCalculatorImpl;
import com.sap.sailing.windestimation.windinference.WindTrackCalculatorImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class IncrementalMstHmmWindEstimationForTrackedRaceTest extends OnlineTracTracBasedTest {

    public static final String[] modelFilesNames = {
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom0.0To10.0.clf",
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom10.0To912.0.clf",
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom1368.0ToMaximum.clf",
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom912.0To1368.0.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom0.0To1.0.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom1.0To140.0.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom140.0To5394.0.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom5394.0ToMaximum.clf",
            "SERIALIZATION.modelForManeuverClassifier.NeuralNetworkClassifier.ManeuverClassification-Basic-All.clf" };

    protected final SimpleDateFormat dateFormat;
    private WindEstimationFactoryServiceImpl windEstimationFactoryService;
    private ClassPathReadOnlyModelStoreImpl modelStore;

    public IncrementalMstHmmWindEstimationForTrackedRaceTest() throws Exception {
        dateFormat = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2")); // will result in CEST
        windEstimationFactoryService = new WindEstimationFactoryServiceImpl();
        windEstimationFactoryService.clearState();
        modelStore = new ClassPathReadOnlyModelStoreImpl("trained_wind_estimation_models", getClass().getClassLoader(),
                modelFilesNames);
        windEstimationFactoryService.importAllModelsFromModelStore(modelStore);
    }

    @Before
    public void setUp() throws MalformedURLException, IOException, InterruptedException, URISyntaxException,
            ParseException, SubscriberInitializationException, CreateModelException {
        super.setUp();
        URI storedUri = new URI("file:///"
                + new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(),
                new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        getTrackedRace()
                .setWindEstimation(windEstimationFactoryService.createIncrementalWindEstimationTrack(getTrackedRace()));
        getTrackedRace().waitForManeuverDetectionToFinish();
    }

    @Test
    public void testIncrementalMstHmmWindEstimationForTrackedRace() throws NoWindException, ModelPersistenceException {
        assertTrue("Wind estimation models are empty", windEstimationFactoryService.isReady());
        DynamicTrackedRaceImpl trackedRace = getTrackedRace();
        WindTrack estimatedWindTrackOfTrackedRace = trackedRace
                .getOrCreateWindTrack(new WindSourceImpl(WindSourceType.MANEUVER_BASED_ESTIMATION));
        List<CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData>> competitorTracks = new ArrayList<>();
        for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
            IncrementalManeuverDetectorImpl maneuverDetector = new IncrementalManeuverDetectorImpl(trackedRace,
                    competitor, null);
            ManeuverDetectorWithEstimationDataSupportDecoratorImpl maneuverDetectorWithEstimationDataSupportDecorator = new ManeuverDetectorWithEstimationDataSupportDecoratorImpl(
                    maneuverDetector, null);
            List<CompleteManeuverCurve> maneuverCurves = maneuverDetectorWithEstimationDataSupportDecorator
                    .detectCompleteManeuverCurves();
            List<CompleteManeuverCurveWithEstimationData> completeManeuverCurvesWithEstimationData = maneuverDetectorWithEstimationDataSupportDecorator
                    .getCompleteManeuverCurvesWithEstimationData(maneuverCurves);
            CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData> competitorTrack = new CompetitorTrackWithEstimationData<>(
                    trackedRace.getTrackedRegatta().getRegatta().getName(), trackedRace.getRace().getName(),
                    competitor.getName(), trackedRace.getBoatOfCompetitor(competitor).getBoatClass(),
                    completeManeuverCurvesWithEstimationData, 1, null, null, null, 0, 0);
            competitorTracks.add(competitorTrack);
        }
        RaceWithEstimationData<CompleteManeuverCurveWithEstimationData> race = new RaceWithEstimationData<>(
                competitorTracks.get(0).getRegattaName(), competitorTracks.get(0).getRaceName(), WindQuality.LOW,
                competitorTracks);
        ManeuverBasedWindEstimationComponentImpl<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> targetWindEstimation = new ManeuverBasedWindEstimationComponentImpl<>(
                new RaceElementsFilteringPreprocessingPipelineImpl(false,
                        new CompleteManeuverCurveWithEstimationDataToManeuverForEstimationTransformer()),
                windEstimationFactoryService.maneuverClassifiersCache,
                new ManeuverClassificationsAggregatorFactory(null, modelStore, false, Long.MAX_VALUE).mstHmm(false),
                new WindTrackCalculatorImpl(new MiddleCourseBasedTwdCalculatorImpl(),
                        new DummyBasedTwsCalculatorImpl()));
        List<WindWithConfidence<Pair<Position, TimePoint>>> windFixes = targetWindEstimation.estimateWindTrack(race);
        List<Wind> targetWindFixes = new ArrayList<>(windFixes.size());
        for (WindWithConfidence<Pair<Position, TimePoint>> windFix : windFixes) {
            Wind wind = windFix.getObject();
            targetWindFixes.add(wind);
            // System.out.println("Target: " + wind.getTimePoint() + " " + wind.getPosition() + " "
            // + Math.round(wind.getFrom().getDegrees()));
        }
        List<Wind> estimatedWindFixes = new ArrayList<>();
        estimatedWindTrackOfTrackedRace.lockForRead();
        try {
            for (Wind wind : estimatedWindTrackOfTrackedRace.getFixes()) {
                estimatedWindFixes.add(wind);
                // System.out.println("Estimated: " + wind.getTimePoint() + " " + wind.getPosition() + " "
                // + Math.round(wind.getFrom().getDegrees()));
            }
        } finally {
            estimatedWindTrackOfTrackedRace.unlockAfterRead();
        }
        Comparator<Wind> windFixesComparator = new Comparator<Wind>() {

            @Override
            public int compare(Wind o1, Wind o2) {
                return o1.getTimePoint().compareTo(o2.getTimePoint());
            }
        };
        Collections.sort(targetWindFixes, windFixesComparator);
        Collections.sort(estimatedWindFixes, windFixesComparator);

        Map<Pair<Position, TimePoint>, Wind> targetWindFixesMap = new TreeMap<>(
                new TimePointAndPositionWithToleranceComparator());
        for (Wind wind : targetWindFixes) {
            targetWindFixesMap.put(new Pair<>(wind.getPosition(), wind.getTimePoint()), wind);
        }
        Map<Pair<Position, TimePoint>, Wind> estimatedWindFixesMap = new TreeMap<>(
                new TimePointAndPositionWithToleranceComparator());
        for (Wind wind : estimatedWindFixes) {
            Pair<Position, TimePoint> relativeTo = new Pair<>(wind.getPosition(), wind.getTimePoint());
            estimatedWindFixesMap.put(relativeTo, wind);
            Wind targetWind = targetWindFixesMap.get(relativeTo);
            if (targetWind == null) {
                fail("Wind fix not present in target wind fixes set at: " + wind.getTimePoint() + " "
                        + wind.getPosition());
            } else if (targetWind.getBearing().getDifferenceTo(wind.getBearing()).abs().getDegrees() > 10) {
                fail("TWD difference with target wind fix: " + wind.getTimePoint() + " " + wind.getPosition() + " "
                        + targetWind.getBearing().getDifferenceTo(wind.getBearing()).abs().getDegrees() + " deg");
            }
        }
        for (Wind wind : targetWindFixes) {
            if (!estimatedWindFixesMap.containsKey(new Pair<>(wind.getPosition(), wind.getTimePoint()))) {
                fail("Target wind fix not present at: " + wind.getTimePoint() + " " + wind.getPosition());
            }
        }
    }

}
