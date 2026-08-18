package com.sap.sailing.windestimation;

import java.util.List;

import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.windestimation.aggregator.ManeuverClassificationsAggregator;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverWithEstimatedType;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedType;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifiersCache;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverWithProbabilisticTypeClassification;
import com.sap.sailing.windestimation.preprocessing.PreprocessingPipeline;
import com.sap.sailing.windestimation.windinference.WindTrackCalculator;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Stand-alone wind estimation which is capable of delivering a list of wind fixes by processing the given input. This
 * implementation defines the following steps which the wind estimation implementation must implement:
 * <ol>
 * <li>Pre-process input by appropriate {@link PreprocessingPipeline} where irrelevant or bad quality input is sorted
 * out and the input is converted into {@link ManeuverForEstimation}</li>
 * <li>Classify each maneuver instance using {@link ManeuverClassifiersCache} to produce
 * {@link ManeuverWithProbabilisticTypeClassification}</li>
 * <li>Aggregate maneuver classifications so that it allows conclusion about a plausible non-bumping wind track. By this
 * step, the final maneuver classifications are determined and constructed as {@link ManeuverWithEstimatedType}</li>
 * <li>Derive a wind track from the classified maneuver with wind fixes each containing TWD, TWS, position, time point
 * and confidence.</li>
 * </ol>
 * For each of the aforementioned steps, this interface exposes methods allowing to hook into various estimation steps.
 * This allows for example to skip the pre-processing step of this implementation and resume with already derived
 * results required for the next estimation step.
 * 
 * @author Vladislav Chumak (D069712)
 *
 * @param <InputType>
 *            The type of the input, from which this wind estimation implementation infers the desired wind track.
 */
public interface WindEstimationComponentWithInternals<InputType> extends WindEstimationComponent<InputType> {

    List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrackAfterPreprocessing(
            RaceWithEstimationData<ManeuverForEstimation> race);

    List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrackAfterManeuverClassification(
            RaceWithEstimationData<ManeuverWithProbabilisticTypeClassification> raceWithManeuverClassifications);

    List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrackAfterManeuverClassificationsAggregation(
            List<? extends SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> improvedManeuverClassifications);

    PreprocessingPipeline<InputType, RaceWithEstimationData<ManeuverForEstimation>> getPreprocessingPipeline();

    ManeuverClassificationsAggregator getManeuverClassificationsAggregator();

    ManeuverClassifiersCache getManeuverClassifiersCache();

    WindTrackCalculator getWindTrackCalculator();

}
