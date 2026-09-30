package com.sap.sailing.polars.windestimation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoat;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CompetitorAndBoatImpl;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.scalablevalue.ScalableDouble;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;
import com.sap.sse.util.kmeans.Cluster;

/**
 * Implements a wind estimation based on maneuver classifications.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ManeuverBasedWindEstimationTrackImpl extends AbstractManeuverBasedWindEstimationTrackImpl {
    private static final long serialVersionUID = -7608973407450278215L;

    private final TrackedRace trackedRace;

    private boolean waitForLatest;

    private final PolarDataService polarService;

    public ManeuverBasedWindEstimationTrackImpl(PolarDataService polarService, TrackedRace trackedRace,
            long millisecondsOverWhichToAverage, boolean waitForLatest) throws NotEnoughDataHasBeenAddedException {
        super(trackedRace.getRace().getName(), trackedRace.getRace().getBoatClass(), millisecondsOverWhichToAverage);
        this.polarService = polarService;
        this.trackedRace = trackedRace;
        this.waitForLatest = waitForLatest;
    }

    /**
     * Determines the likelihood of <code>cluster</code> being a cluster of tacks, based on the average likelihood of
     * the maneuver classifications in <code>cluster</code> to be maneuvers of type {@link ManeuverType#TACK TACK}, in
     * turn based on what the {@link #polarService} thinks that this
     * {@link PolarDataService#getManeuverLikelihoodAndTwsTwa(BoatClass, Speed, double, ManeuverType) likelihood} is and
     * based on whether we find other <code>clusters</code> that support this hypothesis. The likelihood is positively
     * affected
     * 
     * <ul>
     * <li>if a second cluster with similar average middle COG and a similar average maneuver angle with opposite sign
     * is found that also has a high average likelihood of its maneuvers being tacks</li>
     * <li>if other clusters are found that for the average true wind speed estimated from the maneuvers in
     * <code>cluster</code> seem like head-up/bear-away clusters on port/starboard tack with an average middle COG that
     * is half the tacking angle for that wind speed away from the tack cluster's average middle COG</li>
     * <li>if clusters with approximately reversed average middle COG are found, weighted by the maneuvers' likelihood
     * to be jibes</li>
     * <li>if the jibe cluster(s) are surrounded by head-up/bear-away clusters on port/starboard tack with their average
     * middle COG being approximately half the jibing angle at the average jibe maneuvers' wind speed away from the jibe
     * clusters' weighted average middle COG</li>
     * <li>the jibe clusters' weighted average speed relates to the tack clusters' weighted average speed in a way that
     * is supported by the {@link #polarService} (see
     * {@link PolarDataService#getConfidenceForTackJibeSpeedRatio(Speed, Speed, BoatClass)}); background: for many boat
     * classes and many wind conditions, jibes have a significantly higher entry speed than tacks and can hence be kept
     * apart from the tacks even if they have similar maneuver angles.</li>
     * </ul>
     * 
     * @return a likelihood between 0..1 (inclusive), obtained by multiplying the based likelihood of the
     *         <code>cluster</code> to hold tacks, based on the maneuver angles and their speed and how likely the
     *         {@link #polarService} considers this to be a tack; with boost factors added for the likelihood of having
     *         found a good jibe cluster and boost factors for the likelihoods of having found starboard and port tack
     *         clusters that hold the head-up/bear-away maneuvers. These boosts can add as much as
     *         {@link #BOOST_FACTOR_FOR_FULL_JIBE_CLUSTER_LIKELIHOOD} + 2*
     *         {@link #BOOST_FACTOR_FOR_FULL_HEAD_UP_BEAR_AWAY_CLUSTER_LIKELIHOOD} of the original value but will be
     *         capped at the maximum likelihood of 1.0.
     * 
     * @see ManeuverClassification#getLikelihoodForManeuverType(ManeuverType)
     * @see PolarDataService#getManeuverLikelihoodAndTwsTwa(BoatClass, Speed, double, ManeuverType)
     */
    protected double getLikelihoodOfBeingTackCluster(
            Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble> cluster,
            Set<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> clusters) {
        final double result;
        if (cluster.isEmpty()) {
            result = 0;
        } else {
            // start with the average of all of the cluster's maneuvers' likelihood to be a tack
            double averageTackLikelihood = getAverageLikelihoodOfBeingManeuver(ManeuverType.TACK, cluster.stream());
            final Bearing approximateMiddleCOG = getWeightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg(cluster,
                    ManeuverType.TACK).getA();
            // search for the opposite tack cluster by finding the one that has the smallest angular distance to the expected
            // middle COG (same as for the candidate cluster) and expected maneuver angle (the cluster's average maneuver
            // angle with inverted sign)
            final Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble> oppositeTackCluster = clusters
                    .stream().filter((c) -> c != cluster)
                    .min((a, b) -> a.isEmpty() ? b.isEmpty() ? 0 : -1 : b.isEmpty() ? 1 : (int) Math
                            .signum(getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(a,
                                    -cluster.getCentroid().getB(), approximateMiddleCOG)
                                    - getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(b,
                                            -cluster.getCentroid().getB(), approximateMiddleCOG)))
                    .orElse(null);
            // if the opposite tack cluster is empty, it's pretty unlikely that it's a tack cluster; assign a low default
            // probability of 10%
            final double likelihoodOfOppositeCluster = oppositeTackCluster.isEmpty() ? 0.1
                    : getLikelihoodOfClusterBasedOnDistanceFromExpected(-cluster.getCentroid().getB(), approximateMiddleCOG,
                            oppositeTackCluster);
            // compute weighted averages across the current candidate cluster and its supposed opposite tack cluster where
            // the latter is weighted by the likelihoodOfOppositeCluster whereas the candidate cluster is always weighed as 1.0 for
            // this purpose
            final double averageTackingAngleDeg = (Math
                    .abs(getWeightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg(cluster, ManeuverType.TACK).getB())
                    + likelihoodOfOppositeCluster
                            * Math.abs(getWeightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg(oppositeTackCluster,
                                    ManeuverType.TACK).getB()))
                    / (1 + likelihoodOfOppositeCluster);
            final Bearing averageUpwindCOG = new ScalableBearing(
                    getWeightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg(cluster, ManeuverType.TACK).getA())
                            .add(new ScalableBearing(getWeightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg(
                                    oppositeTackCluster, ManeuverType.TACK).getA()).multiply(likelihoodOfOppositeCluster))
                            .divide(1 + likelihoodOfOppositeCluster);
    
            // Now search for head-up/bear-away cluster(s) to port and to starboard of cluster's weighted middle COG
            final Bearing expectedUpwindStarboardTackCOG = averageUpwindCOG
                    .add(new DegreeBearingImpl(-averageTackingAngleDeg / 2.));
            double starboardHeadUpBearAwayClusterLikelihood = getLikelihoodOfBestFittingHeadUpBearAwayCluster(
                    clusters.stream().filter(c -> c != cluster), expectedUpwindStarboardTackCOG);
            final Bearing expectedUpwindPortTackCOG = averageUpwindCOG
                    .add(new DegreeBearingImpl(averageTackingAngleDeg / 2.));
            double portHeadUpBearAwayClusterLikelihood = getLikelihoodOfBestFittingHeadUpBearAwayCluster(
                    clusters.stream().filter(c -> c != cluster), expectedUpwindPortTackCOG);
            // under the assumption that cluster holds tacks, find the clusters that then most likely hold the jibes
            final Bearing approximateMiddleCOGForJibes = averageUpwindCOG.reverse();
            Stream<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> jibeClusters = getJibeClusters(
                    approximateMiddleCOGForJibes, clusters);
            // increase cluster's score if "good" jibe clusters are found; quality is defined by how well the angle matches
            // and how well the speed ratio fits
            Speed tackClusterWeightedAverageSpeed = getWeightedAverageSpeed(cluster.stream(), ManeuverType.TACK);
            if (tackClusterWeightedAverageSpeed != null) {
                // find out how likely the speed ratio is between the candidate tack cluster and the corresponding
                // hypothetical jibe clusters
                double jibeClusterLikelihood = getLikelihoodOfBeingJibeCluster(tackClusterWeightedAverageSpeed,
                        jibeClusters.map((jc) -> jc.stream()).reduce(Stream::concat).orElse(Stream.empty()), getBoatClass(),
                        clusters);
                // Likely jibe clusters may raise the general likelihood by up to 20% of the value so far, but not to over 1.0
                result = Math.min(1.0,
                        averageTackLikelihood * likelihoodOfOppositeCluster
                                * (1.0 + BOOST_FACTOR_FOR_FULL_JIBE_CLUSTER_LIKELIHOOD * jibeClusterLikelihood
                                        + BOOST_FACTOR_FOR_FULL_HEAD_UP_BEAR_AWAY_CLUSTER_LIKELIHOOD
                                                * starboardHeadUpBearAwayClusterLikelihood
                                        + BOOST_FACTOR_FOR_FULL_HEAD_UP_BEAR_AWAY_CLUSTER_LIKELIHOOD
                                                * portHeadUpBearAwayClusterLikelihood));
            } else {
                result = .1; // no elements in the cluster, therefore no average speed; low propability that this would be a tack cluster
            }
        }
        return result;
    }

    /**
     * Determines a likelihood for <code>cluster</code> being a head-up/bear-away cluster for the
     * <code>expectedMiddleCOG</code>.
     * 
     * @return a value between 0..1 (exclusive)
     * @see #getLikelihoodOfClusterBasedOnDistanceFromExpected(double, Bearing, Cluster)
     */
    protected double getLikelihoodOfBestFittingHeadUpBearAwayCluster(
            Stream<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> clusters,
            final Bearing expectedAverageMiddleCOG) {
        final Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble> starboardTackHeadUpAndBearAwayCluster = clusters
                .max((a, b) -> (int) Math.signum(getLikelihoodOfClusterBasedOnDistanceFromExpected(
                        EXPECTED_AVERAGE_HEAD_UP_AND_BEAR_AWAY_MANEUVER_ANGLE_DEG, expectedAverageMiddleCOG, a)
                        - getLikelihoodOfClusterBasedOnDistanceFromExpected(
                                EXPECTED_AVERAGE_HEAD_UP_AND_BEAR_AWAY_MANEUVER_ANGLE_DEG, expectedAverageMiddleCOG,
                                b)))
                .orElse(null);
        double starboardHeadUpBearAwayClusterLikelihood = starboardTackHeadUpAndBearAwayCluster == null ? 0.0
                : getLikelihoodOfClusterBasedOnDistanceFromExpected(
                        EXPECTED_AVERAGE_HEAD_UP_AND_BEAR_AWAY_MANEUVER_ANGLE_DEG, expectedAverageMiddleCOG,
                        starboardTackHeadUpAndBearAwayCluster);
        return starboardHeadUpBearAwayClusterLikelihood;
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
    protected double getLikelihoodOfBeingJibeCluster(Speed tackClusterWeightedAverageSpeed,
            Stream<ManeuverClassification> jibeClustersContent, BoatClass boatClass,
            Set<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> clusters) {
        final int[] count = new int[1];
        final double[] likelihoodSum = new double[1];
        final ScalableBearing[] scaledAverageDownwindCOG = new ScalableBearing[1];
        final double[] scaledAbsJibingAngleSum = new double[1];
        Stream<ManeuverClassification> jibeClustersContentPeeker = jibeClustersContent.peek((mc) -> {
            count[0]++;
            final double likelihood = mc.getLikelihoodForManeuverType(ManeuverType.JIBE);
            likelihoodSum[0] += likelihood;
            final ScalableBearing scaledCOG = new ScalableBearing(mc.getMiddleManeuverCourse()).multiply(likelihood);
            if (scaledAverageDownwindCOG[0] == null) {
                scaledAverageDownwindCOG[0] = scaledCOG;
            } else {
                scaledAverageDownwindCOG[0] = scaledAverageDownwindCOG[0].add(scaledCOG);
            }
            scaledAbsJibingAngleSum[0] += likelihood * Math.abs(mc.getManeuverAngleDeg());
        });
        Speed jibeClusterWeightedAverageSpeed = getWeightedAverageSpeed(jibeClustersContentPeeker, ManeuverType.JIBE);
        Bearing averageDownwindCOG = scaledAverageDownwindCOG[0] == null ? null
                : scaledAverageDownwindCOG[0].divide(likelihoodSum[0]);
        double absWeightedAverageJibingAbgle = scaledAbsJibingAngleSum[0] / likelihoodSum[0];
        final double result;
        if (jibeClusterWeightedAverageSpeed != null) {
            double tackJibeSpeedRatioLikelihood = polarService.getConfidenceForTackJibeSpeedRatio(
                    tackClusterWeightedAverageSpeed, jibeClusterWeightedAverageSpeed, boatClass);
            if (count[0] > 0) {
                double averageJibeLikelihood = likelihoodSum[0] / count[0];
                // Now search for head-up/bear-away cluster(s) to port and to starboard of cluster's weighted middle COG
                final Bearing expectedDownwindStarboardTackCOG = averageDownwindCOG
                        .add(new DegreeBearingImpl(+absWeightedAverageJibingAbgle / 2.));
                double starboardHeadUpBearAwayClusterLikelihood = getLikelihoodOfBestFittingHeadUpBearAwayCluster(
                        clusters.stream(), expectedDownwindStarboardTackCOG);
                final Bearing expectedDownwindPortTackCOG = averageDownwindCOG
                        .add(new DegreeBearingImpl(-absWeightedAverageJibingAbgle / 2.));
                double portHeadUpBearAwayClusterLikelihood = getLikelihoodOfBestFittingHeadUpBearAwayCluster(
                        clusters.stream(), expectedDownwindPortTackCOG);
                result = Math.min(1.0,
                        averageJibeLikelihood * (1.0
                                + BOOST_FACTOR_FOR_JIBE_TACK_SPEED_RATIO_LIKELIHOOD * tackJibeSpeedRatioLikelihood
                                + BOOST_FACTOR_FOR_FULL_HEAD_UP_BEAR_AWAY_CLUSTER_LIKELIHOOD
                                        * starboardHeadUpBearAwayClusterLikelihood
                                + BOOST_FACTOR_FOR_FULL_HEAD_UP_BEAR_AWAY_CLUSTER_LIKELIHOOD
                                        * portHeadUpBearAwayClusterLikelihood));
            } else {
                throw new RuntimeException(
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
        Map<Maneuver, CompetitorAndBoat> maneuvers = getAllManeuvers(waitForLatest);
        return maneuvers.entrySet().stream()
                .map((maneuverAndCompetitor) -> new ManeuverClassificationImpl(maneuverAndCompetitor.getValue(),
                        maneuverAndCompetitor.getKey(), polarService));
    }

    /**
     * Package scope to let test fragment access it
     */
    Map<Maneuver, CompetitorAndBoat> getAllManeuvers(boolean waitForLatest) {
        Map<Maneuver, CompetitorAndBoat> maneuvers = new HashMap<>();
        final Waypoint firstWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
        final Waypoint lastWaypoint = trackedRace.getRace().getCourse().getLastWaypoint();
        for (Map.Entry<Competitor, Boat> competitorAndBoat : trackedRace.getRace().getCompetitorsAndTheirBoats()
                .entrySet()) {
            Competitor competitor = competitorAndBoat.getKey();
            Boat boat = competitorAndBoat.getValue();
            final TimePoint from = Util.getFirstNonNull(
                    firstWaypoint == null ? null
                            : trackedRace.getMarkPassing(competitor, firstWaypoint) == null ? null
                                    : trackedRace.getMarkPassing(competitor, firstWaypoint).getTimePoint(),
                    trackedRace.getStartOfRace(), trackedRace.getStartOfTracking());
            final TimePoint to = Util.getFirstNonNull(
                    lastWaypoint == null ? null
                            : trackedRace.getMarkPassing(competitor, lastWaypoint) == null ? null
                                    : trackedRace.getMarkPassing(competitor, lastWaypoint).getTimePoint(),
                    trackedRace.getEndOfRace(), trackedRace.getEndOfTracking(), MillisecondsTimePoint.now());
            for (Maneuver maneuver : trackedRace.getManeuvers(competitor, from, to, waitForLatest)) {
                maneuvers.put(maneuver, new CompetitorAndBoatImpl(competitor, boat));
            }
        }
        return Collections.unmodifiableMap(maneuvers);
    }

    /**
     * Assigns a likelihood to a cluster based on its
     * {@link #getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(Cluster, double, Bearing)
     * "angular distance"} from an expected average middle COG and Tells how likely it is that the two clusters are
     * opposite tack clusters. This is determined by looking at the
     * {@link #getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(Cluster, double, Bearing)
     * angular distance} between the two clusters regarding their middle COG and their average maneuver angles. The
     * likelihood decreases exponentially with increasing angle differences. It is 1.0 for an exact match and reaches
     * 0.5 for an angular distance of 20deg.
     */
    protected double getLikelihoodOfClusterBasedOnDistanceFromExpected(double expectedManeuverAngleDeg,
            Bearing expectedApproximateMiddleCOG,
            Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble> oppositeTackCluster) {
        final double clusterDistanceFromExpected = getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(
                oppositeTackCluster, expectedManeuverAngleDeg, expectedApproximateMiddleCOG);
        return getLikelihoodOfClusterBasedOnDistanceFromExpected(clusterDistanceFromExpected);
    }

    /**
     * Assigns a likelihood to a cluster based on its
     * {@link #getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(Cluster, double, Bearing)
     * "angular distance"} from an expected average middle COG and Tells how likely it is that the two clusters are
     * opposite tack clusters. This is determined by looking at the
     * {@link #getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(Cluster, double, Bearing)
     * angular distance} between the two clusters regarding their middle COG and their average maneuver angles. The
     * likelihood decreases exponentially with increasing angle differences. It is 1.0 for an exact match and reaches
     * 0.5 for an angular distance of 20deg (see
     * {@link #ANGULAR_DISTANCE_FOR_HALF_CONFIDENCE_FOR_OPPOSITE_TACK_CLUSTER_DEG}).
     * 
     * @param clusterDistanceFromExpected
     *            an angular distance as computed by {@link #getClusterDistance(double, Bearing, double, Bearing)} and
     *            {@link #getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(Cluster, double, Bearing)}
     *            .
     */
    protected double getLikelihoodOfClusterBasedOnDistanceFromExpected(final double clusterDistanceFromExpected) {
        return Math.exp(Math.log(0.5)
                * (clusterDistanceFromExpected / ANGULAR_DISTANCE_FOR_HALF_CONFIDENCE_FOR_OPPOSITE_TACK_CLUSTER_DEG));
    }

    /**
     * A euklidian degree distance based on the two degree distances between the maneuver angles and the middle COGs.
     * The expected pair is passed as <code>expcetedManeuverAngleDeg</code> and
     * <code>expectedApproximateMiddleCOG</code> whereas the actuals are determined by computing the
     * {@link ManeuverType#TACK}-weighted average for those values from <code>cluster</code>.
     */
    private double getClusterDistanceBasedOnAverageManeuverLikelihoodAndWeightedAverageMiddleCOG(
            Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble> cluster,
            double expectedManeuverAngleDeg, Bearing expectedApproximateMiddleCOG) {
        final Pair<Bearing, Double> weightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg = getWeightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg(
                cluster, ManeuverType.TACK);
        final Bearing weightedAverageMiddleCOG = weightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg.getA();
        final Double weightedAverageManeuverAngle = weightedAverageMiddleManeuverCOGDegAndManeuverAngleDeg.getB();
        return getClusterDistance(expectedManeuverAngleDeg, expectedApproximateMiddleCOG, weightedAverageManeuverAngle,
                weightedAverageMiddleCOG);
    }

    /**
     * A euklidian degree distance based on the two degree distances between the maneuver angles and the middle COGs.
     */
    private double getClusterDistance(double expectedManeuverAngleDeg, Bearing expectedApproximateMiddleCOG,
            final double weightedAverageManeuverAngle, final Bearing weightedAverageMiddleCOG) {
        double middleCOGDiff = weightedAverageMiddleCOG.getDifferenceTo(expectedApproximateMiddleCOG).getDegrees();
        double maneuverAngleDiff = weightedAverageManeuverAngle - expectedManeuverAngleDeg;
        return Math.sqrt(middleCOGDiff * middleCOGDiff + maneuverAngleDiff * maneuverAngleDiff);
    }

}
