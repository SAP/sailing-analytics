package com.sap.sailing.windestimation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.maneuverdetection.impl.IncrementalManeuverDetectorImpl;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorWithEstimationDataSupportDecoratorImpl;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.domain.windestimation.IncrementalWindEstimation;
import com.sap.sailing.domain.windestimation.TimePointAndPositionWithToleranceComparator;
import com.sap.sailing.polars.impl.PolarDataServiceImpl;
import com.sap.sailing.windestimation.ManeuverBasedWindEstimationComponentImpl;
import com.sap.sailing.windestimation.aggregator.ManeuverClassificationsAggregatorFactory;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.WindQuality;
import com.sap.sailing.windestimation.data.transformer.CompleteManeuverCurveWithEstimationDataToManeuverForEstimationTransformer;
import com.sap.sailing.windestimation.model.exception.ModelPersistenceException;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DistanceBasedTwdTransitionRegressorModelContext.DistanceValueRange;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DurationBasedTwdTransitionRegressorModelContext.DurationValueRange;
import com.sap.sailing.windestimation.model.store.ClassPathReadOnlyModelStoreImpl;
import com.sap.sailing.windestimation.preprocessing.RaceElementsFilteringPreprocessingPipelineImpl;
import com.sap.sailing.windestimation.windinference.DummyBasedTwsCalculatorImpl;
import com.sap.sailing.windestimation.windinference.MiddleCourseBasedTwdCalculatorImpl;
import com.sap.sailing.windestimation.windinference.WindTrackCalculatorImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.util.Wait;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class IncrementalMstHmmWindEstimationForTrackedRaceTest extends OnlineTracTracBasedTest {

    private static final double PERCENT_QUANTILE = 0.8;

    /**
     * These model file names must match up with the boundaries defined in the {@link DistanceValueRange} and {@link DurationValueRange}
     * enumeration types. The files themselves are obtained by executing the training runs, particularly the launch configurations
     * {@code AggregatedDurationBasedTwdTransitionImporter} and {@code AggregatedDistanceBasedTwdTransitionImporter} which, when provided
     * with the argument {@code ../com.sap.sailing.windestimation.test/resources/trained_wind_estimation_models} will store the serialized
     * versions of the wind regressor models there, using the boundaries as defined in the two enumeration types.<p>
     * 
     * Failing to update these files and their names after making changes to either of the enumeration types will lead to exceptions
     * during test runs.
     */
    public static final String[] modelFilesNames = {
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom0.0To10.0.clf",
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom10.0To100.0.clf",
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom100.0To500.0.clf",
            "SERIALIZATION.modelForDistanceBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DistanceBasedTwdTransitionRegressorFrom500.0ToMaximum.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom0.0To1.0.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom1.0To140.0.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom140.0To5394.0.clf",
            "SERIALIZATION.modelForDurationBasedTwdDeltaStdRegressor.IncrementalSingleDimensionPolynomialRegressor.DurationBasedTwdTransitionRegressorFrom5394.0ToMaximum.clf",
            "SERIALIZATION.modelForManeuverClassifier.NeuralNetworkClassifier.ManeuverClassification-Basic-5O5.clf",
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

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"
                + new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS, ReceiverType.MARKPOSITIONS });
        final MillisecondsTimePoint timePointForFixes = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23, 10, 00).getTime());
        final WindSourceWithAdditionalID testWindSource = new WindSourceWithAdditionalID(WindSourceType.EXPEDITION, "Test");
        getTrackedRace().getOrCreateWindTrack(testWindSource).add(
                new WindImpl(new DegreePosition(54.48448470246412, 10.185846456327479),
                        timePointForFixes, new KnotSpeedWithBearingImpl(12.5, /* to */ new DegreeBearingImpl(60))));
        final MillisecondsTimePoint timePointForFixes2 = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23, 10, 30).getTime());
        getTrackedRace().getOrCreateWindTrack(testWindSource).add(
                new WindImpl(new DegreePosition(54.48448470246412, 10.185846456327479),
                        timePointForFixes2, new KnotSpeedWithBearingImpl(11.5, /* to */ new DegreeBearingImpl(58))));
        final MillisecondsTimePoint timePointForFixes3 = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23, 10, 45).getTime());
        getTrackedRace().getOrCreateWindTrack(testWindSource).add(
                new WindImpl(new DegreePosition(54.4844847, 10.1858464),
                        timePointForFixes3, new KnotSpeedWithBearingImpl(12.1, /* to */ new DegreeBearingImpl(59))));
        final PolarDataServiceImpl polarDataService = new PolarDataServiceImpl();
        getTrackedRace().setPolarDataService(polarDataService);
        polarDataService.insertExistingFixes(getTrackedRace());
        Wait.wait(()->!polarDataService.isCurrentlyActiveAndOrHasQueue(), /* timeout */ Optional.of(Duration.ONE_MINUTE),
                /* sleepBetweenAttempts */ Duration.ONE_SECOND.times(5), Level.INFO, "Waiting for polar data service to finish computing");
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), timePointForFixes);
        final IncrementalWindEstimation windEstimation = windEstimationFactoryService.createIncrementalWindEstimationTrack(getTrackedRace());
        getTrackedRace()
                .setWindEstimation(windEstimation);
        getTrackedRace().waitForManeuverDetectionToFinish();
        windEstimation.waitUntilDone();
    }

    @Test
    public void testIncrementalMstHmmWindEstimationForTrackedRace() throws NoWindException, ModelPersistenceException {
        assertTrue(windEstimationFactoryService.isReady(), "Wind estimation models are empty");
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
        assertMostFixesTWDAround(targetWindFixes, 233, /* range for 90% quantile */ 17, /* average tolerance */ 2);
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
        int foundCount = 0;
        for (Wind wind : estimatedWindFixes) {
            Pair<Position, TimePoint> relativeTo = new Pair<>(wind.getPosition(), wind.getTimePoint());
            estimatedWindFixesMap.put(relativeTo, wind);
            Wind targetWind = findWithinTolerance(targetWindFixesMap, relativeTo);
            if (targetWind != null && targetWind.getBearing().getDifferenceTo(wind.getBearing()).abs().getDegrees() <= 10) {
                foundCount++;
            }
        }
        assertTrue((double) foundCount / (double) estimatedWindFixes.size() > PERCENT_QUANTILE,
                "Expected ratio of matching fixes to be at least "+PERCENT_QUANTILE+" but was only "+(double) foundCount / (double) estimatedWindFixes.size());
        foundCount = 0;
        for (Wind wind : targetWindFixes) {
            if (findWithinTolerance(estimatedWindFixesMap, new Pair<>(wind.getPosition(), wind.getTimePoint())) != null) {
                foundCount++;
            }
        }
        assertTrue((double) foundCount / (double) targetWindFixes.size() > PERCENT_QUANTILE,
                "Expected ratio of matching fixes to be at least "+PERCENT_QUANTILE+" but was only "+(double) foundCount / (double) estimatedWindFixes.size());
    }

    /**
     * {@link Position} objects may deviate slightly, e.g., because they are represented in an internal
     * compact format. Therefore, we don't simply {@link Map#get(Object) get} the object based on the {@code relativeTo}
     * pair but we iterate the map's keys and try to match based on "reasonably close" which is defined to be
     * less than 1/10000 of a lat/lng degree.
     */
    private Wind findWithinTolerance(Map<Pair<Position, TimePoint>, Wind> targetWindFixesMap,
            Pair<Position, TimePoint> relativeTo) {
        for (final Entry<Pair<Position, TimePoint>, Wind> e : targetWindFixesMap.entrySet()) {
            if (Math.abs(e.getKey().getA().getLatDeg() - relativeTo.getA().getLatDeg()) < 1./10000.
             && Math.abs(e.getKey().getA().getLngDeg() - relativeTo.getA().getLngDeg()) < 1./10000.
             && e.getKey().getB().equals(relativeTo.getB())) {
                return e.getValue();
            }
        }
        return null;
    }

    private void assertMostFixesTWDAround(List<Wind> targetWindFixes, double expectedTWDAverageInDegrees, double toleranceForPercentQuantile, double averageToleranceInDegrees) {
        int insideRange = 0;
        Bearing targetTWD = new DegreeBearingImpl(expectedTWDAverageInDegrees);
        ScalableBearing bearingSum = null;
        for (final Wind wind : targetWindFixes) {
            if (wind.getFrom().getDifferenceTo(targetTWD).abs().getDegrees() <= toleranceForPercentQuantile) {
                insideRange++;
            }
            final ScalableBearing scalableBearing = new ScalableBearing(wind.getFrom());
            if (bearingSum == null) {
                bearingSum = scalableBearing;
            } else {
                bearingSum = bearingSum.add(scalableBearing);
            }
        }
        final Bearing averageBearing = bearingSum.divide(targetWindFixes.size());
        assertTrue((double) insideRange / targetWindFixes.size() >= PERCENT_QUANTILE,
                        "Expected at least "+((int) (100*PERCENT_QUANTILE))+"% of the wind fixes to be in range "+
                                new DegreeBearingImpl(expectedTWDAverageInDegrees).add(new DegreeBearingImpl(-toleranceForPercentQuantile))+
                                        " to "+new DegreeBearingImpl(expectedTWDAverageInDegrees).add(new DegreeBearingImpl(toleranceForPercentQuantile))+
                                        " but only "+(int) (100*(double) insideRange / targetWindFixes.size())+"% were.");
        assertEquals(expectedTWDAverageInDegrees, averageBearing.getDegrees(), averageToleranceInDegrees);
    }

}
