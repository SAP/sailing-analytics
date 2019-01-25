package com.sap.sailing.windestimation.preprocessing;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.transformer.CompetitorTrackTransformer;

public class RaceElementsFilteringPreprocessingPipelineImpl
        implements RacePreprocessingPipeline<CompleteManeuverCurveWithEstimationData, ManeuverForEstimation> {

    private final CompetitorTrackTransformer<CompleteManeuverCurveWithEstimationData, ManeuverForEstimation> maneuverForEstimationTransformer;
    private final CompetitorTrackFilteringImpl<CompleteManeuverCurveWithEstimationData> competitorTrackFiltering = new CompetitorTrackFilteringImpl<>();
    private final ManeuverFilteringImpl maneuverFiltering = new ManeuverFilteringImpl();
    private final boolean enableCompetitorTrackFiltering;

    public RaceElementsFilteringPreprocessingPipelineImpl(
            CompetitorTrackTransformer<CompleteManeuverCurveWithEstimationData, ManeuverForEstimation> maneuverForEstimationTransformer) {
        this(true, maneuverForEstimationTransformer);
    }

    public RaceElementsFilteringPreprocessingPipelineImpl(boolean enableCompetitorTrackFiltering,
            CompetitorTrackTransformer<CompleteManeuverCurveWithEstimationData, ManeuverForEstimation> maneuverForEstimationTransformer) {
        this.enableCompetitorTrackFiltering = enableCompetitorTrackFiltering;
        this.maneuverForEstimationTransformer = maneuverForEstimationTransformer;
    }

    @Override
    public RaceWithEstimationData<ManeuverForEstimation> preprocessRace(
            RaceWithEstimationData<CompleteManeuverCurveWithEstimationData> race) {
        Stream<CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData>> stream = race
                .getCompetitorTracks().stream();
        if (enableCompetitorTrackFiltering) {
            stream = stream.filter(competitorTrackFiltering);
        }
        List<CompetitorTrackWithEstimationData<ManeuverForEstimation>> competitorTracks = stream
                .map(competitorTrack -> competitorTrack
                        .constructWithElements(maneuverForEstimationTransformer.apply(competitorTrack).getElements()
                                .stream().filter(maneuverFiltering).collect(Collectors.toList())))
                .collect(Collectors.toList());
        RaceWithEstimationData<ManeuverForEstimation> newRace = race.constructWithElements(competitorTracks);
        return newRace;
    }

}
