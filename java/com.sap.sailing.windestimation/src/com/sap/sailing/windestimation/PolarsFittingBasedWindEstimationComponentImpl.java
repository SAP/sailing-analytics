package com.sap.sailing.windestimation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sailing.windestimation.aggregator.ManeuverClassificationsAggregator;
import com.sap.sailing.windestimation.aggregator.polarsfitting.PolarsFittingWindEstimation;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedType;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifiersCache;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverWithProbabilisticTypeClassification;
import com.sap.sailing.windestimation.preprocessing.PreprocessingPipeline;
import com.sap.sailing.windestimation.windinference.WindTrackCalculator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Wind estimation implementation which operates by matching COG/SOG tuples contained in pre-processed maneuver
 * instances with polar chart. This implementation is considered as experimental and highly inaccurate in comparison to
 * {@link ManeuverBasedWindEstimationComponentImpl}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class PolarsFittingBasedWindEstimationComponentImpl<InputType>
        implements WindEstimationComponentWithInternals<InputType> {

    private final PolarDataService polarService;
    private final PreprocessingPipeline<InputType, RaceWithEstimationData<ManeuverForEstimation>> preprocessingPipeline;

    public PolarsFittingBasedWindEstimationComponentImpl(
            PreprocessingPipeline<InputType, RaceWithEstimationData<ManeuverForEstimation>> preprocessingPipeline,
            PolarDataService polarService) {
        this.preprocessingPipeline = preprocessingPipeline;
        this.polarService = polarService;
    }

    @Override
    public List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrack(InputType input) {
        RaceWithEstimationData<ManeuverForEstimation> race = preprocessingPipeline.preprocessInput(input);
        return estimateWindTrackAfterPreprocessing(race);
    }

    @Override
    public List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrackAfterPreprocessing(
            RaceWithEstimationData<ManeuverForEstimation> race) {
        PolarsFittingWindEstimation windEstimation = new PolarsFittingWindEstimation(polarService,
                race.getCompetitorTracks());
        List<ManeuverForEstimation> maneuvers = race.getCompetitorTracks().stream()
                .flatMap(competitorTrack -> competitorTrack.getElements().stream())
                .sorted((one, two) -> one.getManeuverTimePoint().compareTo(two.getManeuverTimePoint()))
                .collect(Collectors.toList());
        List<WindWithConfidence<Pair<Position, TimePoint>>> result = new ArrayList<>();
        WindWithConfidence<Void> wind = windEstimation.estimateAverageWind();
        if (wind != null) {
            for (ManeuverForEstimation maneuver : maneuvers) {
                if (maneuver.isClean()) {
                    Bearing twaBefore = wind.getObject().getFrom()
                            .getDifferenceTo(maneuver.getSpeedWithBearingBefore().getBearing());
                    Bearing twaAfter = wind.getObject().getFrom()
                            .getDifferenceTo(maneuver.getSpeedWithBearingAfter().getBearing());
                    if (twaBefore.getDegrees() * twaAfter.getDegrees() < 0
                            && Math.abs(Math.abs(twaBefore.getDegrees()) - Math.abs(twaAfter.getDegrees())) <= 20) {
                        Wind newWind = new WindImpl(maneuver.getManeuverPosition(), maneuver.getManeuverTimePoint(),
                                wind.getObject());
                        result.add(new WindWithConfidenceImpl<>(newWind, wind.getConfidence(),
                                new Pair<>(maneuver.getManeuverPosition(), maneuver.getManeuverTimePoint()),
                                newWind.getKnots() > 0));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrackAfterManeuverClassification(
            RaceWithEstimationData<ManeuverWithProbabilisticTypeClassification> raceWithManeuverClassifications) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<WindWithConfidence<Pair<Position, TimePoint>>> estimateWindTrackAfterManeuverClassificationsAggregation(
            List<? extends SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> improvedManeuverClassifications) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PreprocessingPipeline<InputType, RaceWithEstimationData<ManeuverForEstimation>> getPreprocessingPipeline() {
        return preprocessingPipeline;
    }

    @Override
    public ManeuverClassificationsAggregator getManeuverClassificationsAggregator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ManeuverClassifiersCache getManeuverClassifiersCache() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WindTrackCalculator getWindTrackCalculator() {
        throw new UnsupportedOperationException();
    }
}
