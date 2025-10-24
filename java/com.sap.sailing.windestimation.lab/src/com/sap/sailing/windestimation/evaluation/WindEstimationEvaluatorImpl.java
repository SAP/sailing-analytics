package com.sap.sailing.windestimation.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.sap.sailing.domain.common.BearingChangeAnalyzer;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl;
import com.sap.sailing.windestimation.WindEstimationComponentWithInternals;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.WindQuality;
import com.sap.sailing.windestimation.preprocessing.DummyRacePreprocessingPipelineImpl;
import com.sap.sailing.windestimation.preprocessing.RaceWithRandomClippingPreprocessingPipelineImpl;
import com.sap.sailing.windestimation.util.LoggingUtil;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindEstimationEvaluatorImpl<T> implements WindEstimationEvaluator<T> {

    private final double maxWindCourseDeviationInDegrees;
    private final double maxWindSpeedDeviationInPercent;
    private final double minAccuracyPerRaceForCorrectEstimation;
    private final boolean evaluationPerCompetitorTrack;
    private final Integer minManeuversPerRace;
    private final boolean randomClipping;
    private final Integer fixedNumberOfManeuvers;

    public WindEstimationEvaluatorImpl(double maxWindCourseDeviationInDegrees, double maxWindSpeedDeviationInPercent,
            double minAccuracyPerRaceForCorrectEstimation, boolean evaluationPerCompetitorTrack,
            int minManeuversPerRace, boolean randomCompetitorTrackClipping, Integer fixedNumberOfManeuvers) {
        this.maxWindCourseDeviationInDegrees = maxWindCourseDeviationInDegrees;
        this.maxWindSpeedDeviationInPercent = maxWindSpeedDeviationInPercent;
        this.minAccuracyPerRaceForCorrectEstimation = minAccuracyPerRaceForCorrectEstimation;
        this.evaluationPerCompetitorTrack = evaluationPerCompetitorTrack;
        this.minManeuversPerRace = minManeuversPerRace;
        this.randomClipping = randomCompetitorTrackClipping;
        this.fixedNumberOfManeuvers = fixedNumberOfManeuvers;
    }

    @Override
    public WindEstimatorEvaluationResult evaluateWindEstimator(
            WindEstimatorFactory<RaceWithEstimationData<T>> windEstimatorFactory,
            TargetWindFixesExtractor<T> targetWindFixesExtractor, Iterator<RaceWithEstimationData<T>> racesIterator,
            long numberOfRaces) {
        Stream<RaceWithEstimationData<T>> stream = StreamSupport.stream(
                new FixedBatchSpliteratorWrapper<>(
                        Spliterators.spliterator(racesIterator, numberOfRaces, Spliterator.NONNULL), numberOfRaces, 50),
                true);
        if (evaluationPerCompetitorTrack) {
            stream = stream.flatMap(race -> {
                List<RaceWithEstimationData<T>> racesToEvaluate = new ArrayList<>();
                if (race.getCompetitorTracks().size() <= 1) {
                    racesToEvaluate.add(race);
                } else {
                    for (CompetitorTrackWithEstimationData<T> competitorTrack : race.getCompetitorTracks()) {
                        RaceWithEstimationData<T> raceWithSingleCompetitorTrack = race
                                .constructWithElements(Collections.singletonList(competitorTrack));
                        racesToEvaluate.add(raceWithSingleCompetitorTrack);
                    }
                }
                return racesToEvaluate.stream();
            });
        }
        Stream<EvaluationCase<T>> preprocessingStream = stream.map(race -> {
            WindEstimationComponentWithInternals<RaceWithEstimationData<T>> windEstimator = (WindEstimationComponentWithInternals<RaceWithEstimationData<T>>) windEstimatorFactory
                    .createNewEstimatorInstance();
            RaceWithEstimationData<ManeuverForEstimation> preprocessedRace = windEstimator.getPreprocessingPipeline()
                    .preprocessInput(race);
            WindTrack targetWindTrack = new WindTrackImpl(1000, false, "targetWindTrack");
            for (CompetitorTrackWithEstimationData<T> competitorTrackWithEstimationData : race.getCompetitorTracks()) {
                List<Wind> targetWindFixes = targetWindFixesExtractor
                        .extractTargetWindFixes(competitorTrackWithEstimationData);
                for (Wind wind : targetWindFixes) {
                    targetWindTrack.add(wind);
                }
            }
            return new EvaluationCase<>(windEstimator, preprocessedRace, targetWindTrack);
        });
        preprocessingStream = preprocessingStream
                .filter(evaluationCase -> evaluationCase.getRace().getWindQuality() == WindQuality.EXPEDITION
                        && evaluationCase.getRace().getCompetitorTracks().stream()
                                .mapToInt(competitorTrack -> competitorTrack.getElements().size())
                                .sum() >= minManeuversPerRace);
        if (randomClipping) {
            RaceWithRandomClippingPreprocessingPipelineImpl<ManeuverForEstimation, ManeuverForEstimation> clippingPipeline = new RaceWithRandomClippingPreprocessingPipelineImpl<ManeuverForEstimation, ManeuverForEstimation>(
                    new DummyRacePreprocessingPipelineImpl<>(), fixedNumberOfManeuvers == null ? 1 : fixedNumberOfManeuvers,
                    fixedNumberOfManeuvers == null ? Integer.MAX_VALUE : fixedNumberOfManeuvers, evaluationPerCompetitorTrack);
            preprocessingStream = preprocessingStream
                    .map(evaluationCase -> new EvaluationCase<>(evaluationCase.getWindEstimator(),
                            clippingPipeline.preprocessInput(evaluationCase.getRace()),
                            evaluationCase.getTargetWindTrack()));
        } else if (fixedNumberOfManeuvers != null) {
            throw new IllegalArgumentException(
                    "fixedNumberOfManeuver requires randomClippingOfCompetitorTracks to be true");
        }
        return preprocessingStream.map(evaluationCase -> evaluateWindEstimator(evaluationCase))
                .reduce((one, two) -> one.mergeBySum(two)).orElse(new WindEstimatorEvaluationResult());
    }

    @Override
    public WindEstimatorEvaluationResult evaluateWindEstimator(EvaluationCase<T> evaluationCase) {
        RaceWithEstimationData<ManeuverForEstimation> raceWithEstimationData = evaluationCase.getRace();
        LoggingUtil.logInfo("Evaluating on " + raceWithEstimationData.getRegattaName() + " Race "
                + raceWithEstimationData.getRaceName());
        WindTrack targetWindTrack = evaluationCase.getTargetWindTrack();
        WindEstimationComponentWithInternals<RaceWithEstimationData<T>> windEstimator = evaluationCase
                .getWindEstimator();
        List<WindWithConfidence<Pair<Position, TimePoint>>> windTrack = windEstimator
                .estimateWindTrackAfterPreprocessing(raceWithEstimationData);
        WindEstimatorEvaluationResult result = new WindEstimatorEvaluationResult();
        WindTrack estimatedWindTrack = new WindTrackImpl(1000, false, "estimatedWindTrack");
        for (WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence : windTrack) {
            Wind estimatedWind = windWithConfidence.getObject();
            Wind targetWind = targetWindTrack.getAveragedWind(estimatedWind.getPosition(),
                    estimatedWind.getTimePoint());
            if (targetWind.getBearing().getDegrees() > 0.001) {
                estimatedWindTrack.add(estimatedWind);
                double windCourseDeviationInDegrees = targetWind.getBearing()
                        .getDifferenceTo(estimatedWind.getBearing()).getDegrees();
                boolean windCourseDeviationWithinTolerance = Math
                        .abs(windCourseDeviationInDegrees) <= maxWindCourseDeviationInDegrees;
                double confidence = windWithConfidence.getConfidence();
                if (targetWind.getKnots() > 2) {
                    double windSpeedDeviationInPercent = Math.abs(targetWind.getKnots() - estimatedWind.getKnots())
                            / targetWind.getKnots();
                    boolean windSpeedDeviationWithinTolerance = windSpeedDeviationInPercent <= maxWindSpeedDeviationInPercent;
                    result = result.mergeBySum(new WindEstimatorEvaluationResult(windCourseDeviationInDegrees,
                            windCourseDeviationWithinTolerance, windSpeedDeviationInPercent,
                            windSpeedDeviationWithinTolerance, confidence, null));
                } else {
                    result = result.mergeBySum(new WindEstimatorEvaluationResult(windCourseDeviationInDegrees,
                            windCourseDeviationWithinTolerance, confidence, null));
                }
            }
        }
        int numberOfPossibleManeuverTypes = ManeuverTypeForClassification.values().length;
        int[][] confusionMatrix = new int[numberOfPossibleManeuverTypes][numberOfPossibleManeuverTypes];
        if (!estimatedWindTrack.isEmpty()) {
            for (CompetitorTrackWithEstimationData<ManeuverForEstimation> competitorTrack : raceWithEstimationData
                    .getCompetitorTracks()) {
                for (ManeuverForEstimation maneuver : competitorTrack.getElements()) {
                    Wind estimatedWind = estimatedWindTrack.getAveragedWind(maneuver.getManeuverPosition(),
                            maneuver.getManeuverTimePoint());
                    Wind targetWind = targetWindTrack.getAveragedWind(maneuver.getManeuverPosition(),
                            maneuver.getManeuverTimePoint());
                    ManeuverTypeForClassification estimatedManeuverType = determineManeuverType(maneuver,
                            estimatedWind);
                    ManeuverTypeForClassification targetManeuverType = determineManeuverType(maneuver, targetWind);
                    confusionMatrix[estimatedManeuverType.ordinal()][targetManeuverType.ordinal()]++;
                }
            }
        }
        result = result.mergeBySum(new WindEstimatorEvaluationResult(confusionMatrix));
        LoggingUtil.logInfo("Evaluating on " + raceWithEstimationData.getRegattaName() + " Race "
                + raceWithEstimationData.getRaceName() + " succeeded");
        result.printEvaluationStatistics(false);
        return result.getAvgAsSingleResult(minAccuracyPerRaceForCorrectEstimation - 0.00001);
    }

    private ManeuverTypeForClassification determineManeuverType(ManeuverForEstimation maneuver, Wind wind) {
        ManeuverTypeForClassification maneuverType;
        int numberOfJibes = getNumberOfJibes(maneuver, wind);
        int numberOfTacks = getNumberOfTacks(maneuver, wind);
        if (numberOfTacks > 0 || numberOfJibes > 0) {
            maneuverType = numberOfTacks > 0 ? ManeuverTypeForClassification.TACK : ManeuverTypeForClassification.JIBE;
        } else {
            // heading up or bearing away
            Bearing windBearing = wind.getBearing();
            Bearing toWindBeforeManeuver = windBearing
                    .getDifferenceTo(maneuver.getSpeedWithBearingBefore().getBearing());
            Bearing toWindAfterManeuver = windBearing.getDifferenceTo(maneuver.getSpeedWithBearingAfter().getBearing());
            maneuverType = Math.abs(toWindBeforeManeuver.getDegrees()) < Math.abs(toWindAfterManeuver.getDegrees())
                    ? ManeuverTypeForClassification.HEAD_UP
                    : ManeuverTypeForClassification.BEAR_AWAY;
        }
        return maneuverType;
    }

    /**
     * Gets the number of cases, when the boats bow was headed through the wind coming from behind.
     */
    protected int getNumberOfJibes(ManeuverForEstimation maneuver, Wind wind) {
        BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
        int numberOfJibes = wind == null ? 0
                : bearingChangeAnalyzer.didPass(maneuver.getSpeedWithBearingBefore().getBearing(),
                        maneuver.getCourseChangeInDegrees(), maneuver.getSpeedWithBearingAfter().getBearing(),
                        wind.getBearing());
        return numberOfJibes;
    }

    /**
     * Gets the number of cases, when the boats bow was headed through the wind coming from the front.
     */
    protected int getNumberOfTacks(ManeuverForEstimation maneuver, Wind wind) {
        BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
        int numberOfTacks = wind == null ? 0
                : bearingChangeAnalyzer.didPass(maneuver.getSpeedWithBearingBefore().getBearing(),
                        maneuver.getCourseChangeInDegrees(), maneuver.getSpeedWithBearingAfter().getBearing(),
                        wind.getFrom());
        return numberOfTacks;
    }

}
