package com.sap.sailing.windestimation.aggregator.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.polars.windestimation.AbstractManeuverBasedWindEstimationTrackImpl;
import com.sap.sailing.polars.windestimation.ManeuverBasedWindEstimationTrackImpl;
import com.sap.sailing.polars.windestimation.ManeuverClassification;
import com.sap.sailing.polars.windestimation.ScalableBearingAndScalableDouble;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifiersCache;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverWithProbabilisticTypeClassification;
import com.sap.sailing.windestimation.model.exception.ModelOperationException;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.scalablevalue.ScalableDouble;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;
import com.sap.sse.util.kmeans.Cluster;

/**
 * Adapted variant of clustering-based wind estimation which was initially coded by Axel Uhl as
 * {@link ManeuverBasedWindEstimationTrackImpl}. It includes maneuver classification results of
 * {@link ManeuverClassifiersCache} to compute tack and jibe clusters.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverClusteringBasedWindEstimationTrackImpl extends AbstractManeuverBasedWindEstimationTrackImpl {

    private static final long serialVersionUID = 3474387111725677525L;
    private final RaceWithEstimationData<ManeuverWithProbabilisticTypeClassification> raceWithManeuvers;
    private final PolarDataService polarService;

    public ManeuverClusteringBasedWindEstimationTrackImpl(
            RaceWithEstimationData<ManeuverWithProbabilisticTypeClassification> raceWithManeuvers, BoatClass boatClass,
            PolarDataService polarService, long millisecondsOverWhichToAverage) {
        super(raceWithManeuvers.getRaceName(), boatClass, millisecondsOverWhichToAverage);
        this.raceWithManeuvers = raceWithManeuvers;
        this.polarService = polarService;
    }

    @Override
    protected double getLikelihoodOfBeingTackCluster(
            Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble> cluster,
            Set<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> clusters) {
        // start with the average of all of the cluster's maneuvers' likelihood to be a tack
        double averageTackLikelihood = getAverageLikelihoodOfBeingManeuver(ManeuverType.TACK, cluster.stream());
        Bearing upwindCog = getWeightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg(cluster, ManeuverType.TACK)
                .getA();
        if (upwindCog == null) {
            return 0.1;
        }
        final Bearing averageUpwindCOG = new ScalableBearing(upwindCog).divide(1.0);

        // under the assumption that cluster holds tacks, find the clusters that then most likely hold the jibes
        final Bearing approximateMiddleCOGForJibes = averageUpwindCOG.reverse();
        Stream<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> jibeClusters = getJibeClusters(
                approximateMiddleCOGForJibes, clusters);
        // increase cluster's score if "good" jibe clusters are found; quality is defined by how well the angle matches
        // and how well the speed ratio fits
        Speed tackClusterWeightedAverageSpeed = getWeightedAverageSpeed(cluster.stream(), ManeuverType.TACK);
        final double result;
        if (tackClusterWeightedAverageSpeed != null) {
            // find out how likely the speed ratio is between the candidate tack cluster and the corresponding
            // hypothetical jibe clusters
            double jibeClusterLikelihood = getLikelihoodOfBeingJibeCluster(tackClusterWeightedAverageSpeed,
                    jibeClusters.map((jc) -> jc.stream()).reduce(Stream::concat).orElse(Stream.empty()), getBoatClass(),
                    clusters);
            // Likely jibe clusters may raise the general likelihood by up to 20% of the value so far, but not to over
            // 1.0
            result = (averageTackLikelihood
                    * (1.0 + BOOST_FACTOR_FOR_FULL_JIBE_CLUSTER_LIKELIHOOD * jibeClusterLikelihood))
                    / (1 + BOOST_FACTOR_FOR_FULL_JIBE_CLUSTER_LIKELIHOOD);
        } else {
            result = 0.1;
        }
        return result;
    }

    /**
     * By looking at the weighted speeds over ground averages, and based on the {@link #polarService} determines how
     * {@link PolarDataService#getManeuverLikelihoodAndTwsTwa(BoatClass, Speed, double, ManeuverType) likely} it is that
     * the jibe cluster contents are actually jibes. This is based on the idea that if for the most likely wind speeds
     * the jibes have a different (usually significantly higher) entry speed than the tacks then this difference should
     * show in the cluster elements. If they don't, then this should give a penalty for the cluster likelihoods.
     * <p>
     * 
     * The weight of a maneuver for computing the weighted speed average is determined to be the
     * {@link ManeuverClassification#getLikelihoodForManeuverType(ManeuverType) likelihood of the maneuver being what it
     * is assumed to be}.
     */
    @Override
    protected double getLikelihoodOfBeingJibeCluster(Speed tackClusterWeightedAverageSpeed,
            Stream<ManeuverClassification> jibeClustersContent, BoatClass boatClass,
            Set<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> clusters) {
        final int[] count = new int[1];
        final double[] likelihoodSum = new double[1];
        Stream<ManeuverClassification> jibeClustersContentPeeker = jibeClustersContent.peek((mc) -> {
            count[0]++;
            final double likelihood = mc.getLikelihoodForManeuverType(ManeuverType.JIBE);
            likelihoodSum[0] += likelihood;
        });
        Speed jibeClusterWeightedAverageSpeed = getWeightedAverageSpeed(jibeClustersContentPeeker, ManeuverType.JIBE);
        final double result;
        if (jibeClusterWeightedAverageSpeed != null) {
            double tackJibeSpeedRatioLikelihood = polarService.getConfidenceForTackJibeSpeedRatio(
                    tackClusterWeightedAverageSpeed, jibeClusterWeightedAverageSpeed, boatClass);
            if (count[0] > 0) {
                double averageJibeLikelihood = likelihoodSum[0] / count[0];
                result = Math.min(1.0, averageJibeLikelihood
                        * (1.0 + BOOST_FACTOR_FOR_JIBE_TACK_SPEED_RATIO_LIKELIHOOD * tackJibeSpeedRatioLikelihood));
            } else {
                throw new ModelOperationException(
                        "Internal error: no maneuvers in jibe cluster candidate but still a valid weighted average speed "
                                + jibeClusterWeightedAverageSpeed);
            }
        } else {
            result = 0.1; // no maneuvers in the jibe clusters; could be but may also not be a jibe cluster
        }
        return result;
    }

    /**
     * For each maneuver from <code>maneuvers</code>, a {@link ManeuverClassification object} is created.
     */
    @Override
    protected Stream<ManeuverClassification> getManeuverClassifications() {
        List<ManeuverClassification> maneuverClassifications = new ArrayList<>();
        for (CompetitorTrackWithEstimationData<ManeuverWithProbabilisticTypeClassification> competitorTrack : raceWithManeuvers
                .getCompetitorTracks()) {
            for (ManeuverWithProbabilisticTypeClassification maneuver : competitorTrack.getElements()) {
                ManeuverClassificationForClusteringImpl maneuverClassification = new ManeuverClassificationForClusteringImpl(
                        maneuver, competitorTrack.getCompetitorName(), polarService);
                maneuverClassifications.add(maneuverClassification);
            }
        }
        return maneuverClassifications.stream();
    }

}
