package com.sap.sailing.windestimation.data.importer;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.windestimation.aggregator.hmm.GraphLevel;
import com.sap.sailing.windestimation.aggregator.hmm.GraphNode;
import com.sap.sailing.windestimation.aggregator.hmm.IntersectedWindRange;
import com.sap.sailing.windestimation.aggregator.hmm.IntersectedWindRangeBasedTransitionProbabilitiesCalculator;
import com.sap.sailing.windestimation.aggregator.hmm.SimpleIntersectedWindRangeBasedTransitionProbabilitiesCalculator;
import com.sap.sailing.windestimation.aggregator.hmm.WindCourseRange;
import com.sap.sailing.windestimation.aggregator.hmm.WindCourseRange.CombinationModeOnViolation;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.LabelledManeuverForEstimation;
import com.sap.sailing.windestimation.data.LabelledTwdTransition;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.TwdTransition;
import com.sap.sailing.windestimation.data.persistence.maneuver.PersistedElementsIterator;
import com.sap.sailing.windestimation.data.persistence.maneuver.RaceWithCompleteManeuverCurvePersistenceManager;
import com.sap.sailing.windestimation.data.persistence.twdtransition.TwdTransitionPersistenceManager;
import com.sap.sailing.windestimation.data.transformer.LabelledManeuverForEstimationTransformer;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverWithProbabilisticTypeClassification;
import com.sap.sailing.windestimation.preprocessing.RaceElementsFilteringPreprocessingPipelineImpl;
import com.sap.sailing.windestimation.util.LoggingUtil;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class TwdTransitionImporter {

    public static void main(String[] args) throws UnknownHostException {
        LoggingUtil.logInfo("###################\r\nTWD transitions Import started");
        RaceWithCompleteManeuverCurvePersistenceManager racesPersistenceManager = new RaceWithCompleteManeuverCurvePersistenceManager();
        TwdTransitionPersistenceManager twdTransitionPersistenceManager = new TwdTransitionPersistenceManager();
        twdTransitionPersistenceManager.dropCollection();
        DummyManeuverClassifier maneuverClassifier = new DummyManeuverClassifier();
        RaceElementsFilteringPreprocessingPipelineImpl preprocessingPipeline = new RaceElementsFilteringPreprocessingPipelineImpl(
                new LabelledManeuverForEstimationTransformer());
        long twdTransitionsCount = 0;
        for (PersistedElementsIterator<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> iterator = racesPersistenceManager
                .getIterator(); iterator.hasNext();) {
            RaceWithEstimationData<CompleteManeuverCurveWithEstimationData> race = iterator.next();
            LoggingUtil.logInfo("Processing race " + race.getRaceName() + " of regatta " + race.getRegattaName());
            List<ManeuverWithProbabilisticTypeClassification> sortedManeuvers = getPreprocessedSortedManeuvers(
                    maneuverClassifier, preprocessingPipeline, race);
            if (sortedManeuvers.size() > 1) {
                List<TwdTransition> twdTransitions = getTwdTransitions(sortedManeuvers, race.getRegattaName());
                int numberOfTwdTransitions = twdTransitions.size();
                twdTransitionPersistenceManager.add(twdTransitions);
                twdTransitionsCount += numberOfTwdTransitions;
                LoggingUtil.logInfo(numberOfTwdTransitions + " TWD transitions imported");
            } else {
                LoggingUtil.logInfo("No TWD transitions to import");
            }
        }
        LoggingUtil.logInfo("###################\r\nTWD transitions Import finished");
        LoggingUtil.logInfo("Totally " + twdTransitionsCount + " TWD transitions imported");
    }

    private static List<ManeuverWithProbabilisticTypeClassification> getPreprocessedSortedManeuvers(
            DummyManeuverClassifier maneuverClassifier,
            RaceElementsFilteringPreprocessingPipelineImpl preprocessingPipeline,
            RaceWithEstimationData<CompleteManeuverCurveWithEstimationData> race) {
        RaceWithEstimationData<ManeuverForEstimation> preprocessedRace = preprocessingPipeline.preprocessRace(race);
        List<CompetitorTrackWithEstimationData<ManeuverWithProbabilisticTypeClassification>> competitorTracks = preprocessedRace
                .getCompetitorTracks().stream().map(competitorTrack -> {
                    List<ManeuverWithProbabilisticTypeClassification> maneuverClassifications = competitorTrack
                            .getElements().stream().map(maneuver -> maneuverClassifier.classifyInstance(maneuver))
                            .collect(Collectors.toList());
                    return competitorTrack.constructWithElements(maneuverClassifications);
                }).collect(Collectors.toList());
        RaceWithEstimationData<ManeuverWithProbabilisticTypeClassification> raceWithManeuverClassifications = race
                .constructWithElements(competitorTracks);

        List<ManeuverWithProbabilisticTypeClassification> sortedManeuvers = raceWithManeuverClassifications
                .getCompetitorTracks().stream().flatMap(competitorTrack -> competitorTrack.getElements().stream())
                .sorted().collect(Collectors.toList());
        return sortedManeuvers;
    }

    private static List<TwdTransition> getTwdTransitions(
            List<ManeuverWithProbabilisticTypeClassification> sortedManeuvers, String regattaName) {
        IntersectedWindRangeBasedTransitionProbabilitiesCalculator intersectedWindRangeBasedTransitionProbabilitiesCalculator = new IntersectedWindRangeBasedTransitionProbabilitiesCalculator();
        SimpleIntersectedWindRangeBasedTransitionProbabilitiesCalculator simpleIntersectedWindRangeBasedTransitionProbabilitiesCalculator = new SimpleIntersectedWindRangeBasedTransitionProbabilitiesCalculator();
        List<TwdTransition> result = new ArrayList<>(sortedManeuvers.size()
                * ManeuverTypeForClassification.values().length * ManeuverTypeForClassification.values().length);
        int maneuverIndex = 0;
        for (ManeuverWithProbabilisticTypeClassification previousManeuver : sortedManeuvers) {
            for (ListIterator<ManeuverWithProbabilisticTypeClassification> iterator = sortedManeuvers
                    .listIterator(++maneuverIndex); iterator.hasNext();) {
                ManeuverWithProbabilisticTypeClassification currentManeuver = iterator.next();
                GraphLevel previousLevel = new GraphLevel(previousManeuver,
                        intersectedWindRangeBasedTransitionProbabilitiesCalculator);
                GraphLevel currentLevel = new GraphLevel(currentManeuver,
                        intersectedWindRangeBasedTransitionProbabilitiesCalculator);
                previousLevel.appendNextManeuverNodesLevel(currentLevel);
                Duration duration = previousManeuver.getManeuver().getManeuverTimePoint()
                        .until(currentManeuver.getManeuver().getManeuverTimePoint());
                Distance distance = previousManeuver.getManeuver().getManeuverPosition()
                        .getDistance(currentManeuver.getManeuver().getManeuverPosition());
                for (GraphNode previousNode : previousLevel.getLevelNodes()) {
                    if (previousNode.getManeuverType() == ((LabelledManeuverForEstimation) previousLevel.getManeuver())
                            .getManeuverType()) {
                        WindCourseRange previousWindCourseRange = simpleIntersectedWindRangeBasedTransitionProbabilitiesCalculator
                                .getWindCourseRangeForManeuverType(previousManeuver.getManeuver(),
                                        previousNode.getManeuverType());
                        Bearing bearingToPreviusManeuver = currentManeuver.getManeuver().getManeuverPosition()
                                .getBearingGreatCircle(previousManeuver.getManeuver().getManeuverPosition());
                        for (GraphNode currentNode : currentLevel.getLevelNodes()) {
                            WindCourseRange currentWindCourseRange = simpleIntersectedWindRangeBasedTransitionProbabilitiesCalculator
                                    .getWindCourseRangeForManeuverType(currentManeuver.getManeuver(),
                                            currentNode.getManeuverType());
                            Bearing bearingToPreviousManeuverMinusTwd = bearingToPreviusManeuver
                                    .getDifferenceTo(
                                            new DegreeBearingImpl(currentWindCourseRange.getAvgWindCourse()).reverse())
                                    .abs();
                            IntersectedWindRange intersectedWindRange = previousWindCourseRange
                                    .intersect(currentWindCourseRange, CombinationModeOnViolation.INTERSECTION);
                            double twdChangeDegrees = intersectedWindRange.getViolationRange();
                            double intersectedTwdChangeDegrees = intersectedWindRangeBasedTransitionProbabilitiesCalculator
                                    .mergeWindRangeAndGetTransitionProbability(previousNode, previousLevel,
                                            intersectedWindRange, currentNode, currentLevel)
                                    .getA().getViolationRange();
                            boolean correct = currentNode
                                    .getManeuverType() == ((LabelledManeuverForEstimation) currentLevel.getManeuver())
                                            .getManeuverType();
                            boolean testDataset = regattaName.contains("2018");
                            LabelledTwdTransition labelledTwdTransition = new LabelledTwdTransition(distance, duration,
                                    new DegreeBearingImpl(twdChangeDegrees),
                                    new DegreeBearingImpl(intersectedTwdChangeDegrees),
                                    bearingToPreviousManeuverMinusTwd, correct, previousNode.getManeuverType(),
                                    currentNode.getManeuverType(), testDataset);
                            result.add(labelledTwdTransition);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static class DummyManeuverClassifier {

        public ManeuverWithProbabilisticTypeClassification classifyInstance(ManeuverForEstimation maneuver) {
            ManeuverWithProbabilisticTypeClassification maneuverWithProbabilisticTypeClassification = new ManeuverWithProbabilisticTypeClassification(
                    maneuver, new double[ManeuverTypeForClassification.values().length]);
            return maneuverWithProbabilisticTypeClassification;
        }

    }

}
