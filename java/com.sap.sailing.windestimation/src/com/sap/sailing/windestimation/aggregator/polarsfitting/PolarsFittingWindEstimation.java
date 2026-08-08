package com.sap.sailing.windestimation.aggregator.polarsfitting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class PolarsFittingWindEstimation {

    private static final int COURSE_CLUSTER_SIZE = 10;

    private final PolarDataService polarService;
    private final Map<BoatClass, SpeedStatistics[]> speedsPerBoatClass = new HashMap<>();

    public PolarsFittingWindEstimation(PolarDataService polarService) {
        this.polarService = polarService;
    }

    public PolarsFittingWindEstimation(PolarDataService polarService,
            List<CompetitorTrackWithEstimationData<ManeuverForEstimation>> competitorTracks) {
        this(polarService);
        for (CompetitorTrackWithEstimationData<ManeuverForEstimation> competitorTrack : competitorTracks) {
            for (ManeuverForEstimation maneuver : competitorTrack.getElements()) {
                if (maneuver.isClean()) {
                    addSpeedWithCourseRecord(maneuver.getSpeedWithBearingBefore(), competitorTrack.getBoatClass());
                    addSpeedWithCourseRecord(maneuver.getSpeedWithBearingAfter(), competitorTrack.getBoatClass());
                }
            }
        }
    }

    public void addSpeedWithCourseRecord(SpeedWithBearing speedWithCourse, BoatClass boatClass) {
        SpeedStatistics speedStatistics = getSpeedStatistics(boatClass, speedWithCourse.getBearing().getDegrees());
        speedStatistics.addSpeed(speedWithCourse.getKnots());
    }

    private SpeedStatistics getSpeedStatistics(BoatClass boatClass, double courseInDegrees) {
        int index = ((int) courseInDegrees) / COURSE_CLUSTER_SIZE;
        SpeedStatistics[] speedStatisticsPerCourseCluster = speedsPerBoatClass.get(boatClass);
        if (speedStatisticsPerCourseCluster == null) {
            speedStatisticsPerCourseCluster = new SpeedStatistics[360 / COURSE_CLUSTER_SIZE];
            speedsPerBoatClass.put(boatClass, speedStatisticsPerCourseCluster);
        }
        SpeedStatistics speedStatistics = speedStatisticsPerCourseCluster[index];
        if (speedStatistics == null) {
            speedStatistics = new SpeedStatistics();
            speedStatisticsPerCourseCluster[index] = speedStatistics;
        }
        return speedStatistics;
    }

    public SpeedWithBearingWithConfidence<Void> estimateWind() {
        List<SpeedWithBearingWithConfidence<Void>> windCandidates = new ArrayList<>();
        for (int trueWindCourseCandidateInDegrees = 0; trueWindCourseCandidateInDegrees < 360; trueWindCourseCandidateInDegrees += 5) {
            int totalSpeeds = 0;
            int speedsWithinDeadWindZone = 0;
            List<Pair<WindSpeedRange, Integer>> windSpeedRangesWithFixesCount = new ArrayList<>();
            int[] pointOfSailCounts = new int[CoarseGrainedPointOfSail.values().length];
            for (Entry<BoatClass, SpeedStatistics[]> speedsForBoatClassEntry : speedsPerBoatClass.entrySet()) {
                BoatClass boatClass = speedsForBoatClassEntry.getKey();
                SpeedStatistics[] speedsForBoatClass = speedsForBoatClassEntry.getValue();
                Bearing trueWindCourseCandidate = new DegreeBearingImpl(trueWindCourseCandidateInDegrees);
                for (int i = 0; i < 360 / COURSE_CLUSTER_SIZE; i++) {
                    SpeedStatistics speedStatistics = speedsForBoatClass[i];
                    if (speedStatistics != null && speedStatistics.getSpeedsCount() > 0
                            && speedStatistics.getAvgSpeed() > 2) {
                        Bearing boatCourse = new DegreeBearingImpl(i * COURSE_CLUSTER_SIZE);
                        Bearing twa = trueWindCourseCandidate.reverse().getDifferenceTo(boatCourse);
                        double absTwaInDegrees = Math.abs(twa.getDegrees());
                        double avgSpeedInKnots = speedStatistics.getAvgSpeed();
                        WindSpeedRange windSpeedRange = getWindSpeedRange(boatClass, avgSpeedInKnots, absTwaInDegrees);
                        if (windSpeedRange != null) {
                            FineGrainedPointOfSail pointOfSail = FineGrainedPointOfSail.valueOf(twa.getDegrees());
                            pointOfSailCounts[pointOfSail.getCoarseGrainedPointOfSail().ordinal()] = speedStatistics
                                    .getSpeedsCount();
                            windSpeedRangesWithFixesCount
                                    .add(new Pair<>(windSpeedRange, speedStatistics.getSpeedsCount()));
                            totalSpeeds += speedStatistics.getSpeedsCount();
                            if (absTwaInDegrees <= 20) {
                                speedsWithinDeadWindZone += speedStatistics.getSpeedsCount();
                            }
                        }
                    }
                }
            }
            SpeedWithConfidence<Void> windSpeedCandidate = calculateWindCandidate(windSpeedRangesWithFixesCount,
                    totalSpeeds, speedsWithinDeadWindZone);
            if (windSpeedCandidate != null) {
                windCandidates.add(new SpeedWithBearingWithConfidenceImpl<>(
                        new KnotSpeedWithBearingImpl(windSpeedCandidate.getObject().getKnots(),
                                new DegreeBearingImpl(trueWindCourseCandidateInDegrees)),
                        windSpeedCandidate.getConfidence(), null));
            }
        }
        SpeedWithBearingWithConfidence<Void> bestCandidate = determineBestWindCandidateAndCalculateConfidence(
                windCandidates);
        return bestCandidate;
    }

    private SpeedWithBearingWithConfidence<Void> determineBestWindCandidateAndCalculateConfidence(
            List<SpeedWithBearingWithConfidence<Void>> windCandidates) {
        SpeedWithBearingWithConfidence<Void> bestWindCandidate = null;
        for (SpeedWithBearingWithConfidence<Void> windCandidate : windCandidates) {
            if (bestWindCandidate == null || bestWindCandidate.getConfidence() < windCandidate.getConfidence()) {
                bestWindCandidate = windCandidate;
            }
        }
        double bestChallengingConfidence = 0;
        for (SpeedWithBearingWithConfidence<Void> windCandidate : windCandidates) {
            double bearingToBestCandidate = windCandidate.getObject().getBearing()
                    .getDifferenceTo(bestWindCandidate.getObject().getBearing()).getDegrees();
            double absBearingToBestCandidate = Math.abs(bearingToBestCandidate);
            if (absBearingToBestCandidate >= 45 && bestChallengingConfidence < windCandidate.getConfidence()) {
                bestChallengingConfidence = windCandidate.getConfidence();
            }
        }
        if (bestWindCandidate == null) {
            return null;
        }
        double bestVariance = 1.0 / bestWindCandidate.getConfidence();
        double bestChallengingVariance = 1.0 / bestChallengingConfidence;
        double realConfidence = (bestChallengingVariance - bestVariance) / (bestChallengingVariance + bestVariance);

        return new SpeedWithBearingWithConfidenceImpl<Void>(bestWindCandidate.getObject(), realConfidence, null);
    }

    private SpeedWithConfidence<Void> calculateWindCandidate(
            List<Pair<WindSpeedRange, Integer>> windSpeedRangesWithFixesCount, int totalSpeeds,
            int speedsWithinDeadWindZone) {
        WindSpeedRange extendedWindSpeedRange = null;
        for (Pair<WindSpeedRange, Integer> windSpeedRangeWithFixesCount : windSpeedRangesWithFixesCount) {
            extendedWindSpeedRange = extendedWindSpeedRange == null ? windSpeedRangeWithFixesCount.getA()
                    : extendedWindSpeedRange.extend(windSpeedRangeWithFixesCount.getA());
        }
        if (extendedWindSpeedRange == null) {
            return null;
        }
        double twsInKnots = 0;
        for (Pair<WindSpeedRange, Integer> windSpeedRangeWithFixesCount : windSpeedRangesWithFixesCount) {
            double weight = 1.0 * windSpeedRangeWithFixesCount.getB() / totalSpeeds;
            twsInKnots += weight * windSpeedRangeWithFixesCount.getA().getMiddleSpeed();
        }
        double variance = 0;
        for (Pair<WindSpeedRange, Integer> windSpeedRangeWithFixesCount : windSpeedRangesWithFixesCount) {
            double deviationOfSpeedFromRange = windSpeedRangeWithFixesCount.getA()
                    .getDeviationOfSpeedFromRange(twsInKnots);
            double relevanceFactor = 1.0 * windSpeedRangeWithFixesCount.getB() / totalSpeeds;
            variance += deviationOfSpeedFromRange * deviationOfSpeedFromRange * relevanceFactor;
        }
        variance *= 1 - 1.0 * speedsWithinDeadWindZone / totalSpeeds;
        return new SpeedWithConfidenceImpl<Void>(new KnotSpeedImpl(twsInKnots), 1 / variance, null);
    }

    public WindSpeedRange getWindSpeedRange(BoatClass boatClass, double avgSpeedInKnots, double absTwaInDegrees) {
        Pair<List<Speed>, Double> windSpeeds;
        try {
            windSpeeds = polarService.estimateWindSpeeds(boatClass, new KnotSpeedImpl(avgSpeedInKnots),
                    new DegreeBearingImpl(absTwaInDegrees));
        } catch (NotEnoughDataHasBeenAddedException e) {
            return null;
        }
        double minSpeed = 0;
        double maxSpeed = 0;
        for (Speed speed : windSpeeds.getA()) {
            double speedInKnots = speed.getKnots();
            if (speedInKnots > 2) {
                if (minSpeed == 0 || minSpeed > speedInKnots) {
                    minSpeed = speedInKnots;
                }
                if (maxSpeed == 0 || maxSpeed < speedInKnots) {
                    maxSpeed = speedInKnots;
                }
            }
        }
        // if(maxSpeed - minSpeed > 2) {
        // maxSpeed = minSpeed + 2;
        // }
        return minSpeed == 0 ? null : new WindSpeedRange(minSpeed, maxSpeed);
    }

    public WindWithConfidence<Void> estimateAverageWind() {
        SpeedWithBearingWithConfidence<Void> wind = estimateWind();
        WindWithConfidenceImpl<Void> windWithConfidence = null;
        if (wind != null) {
            windWithConfidence = new WindWithConfidenceImpl<Void>(new WindImpl(null, null, wind.getObject()),
                    wind.getConfidence(), null, wind.getObject().getKnots() > 0);
        }
        return windWithConfidence;
    }

    public Speed getWindSpeed(SimpleManeuverForEstimation maneuver, Bearing windCourse) {
        WindSpeedRange windSpeedRange = null;
        BoatClass boatClass = maneuver.getBoatClass();
        if (maneuver.isClean()) {
            double absTwaInDegrees = Math.abs(windCourse.reverse()
                    .getDifferenceTo(maneuver.getSpeedWithBearingBefore().getBearing()).getDegrees());
            double avgSpeedInKnots = maneuver.getSpeedWithBearingBefore().getKnots();
            windSpeedRange = getWindSpeedRange(boatClass, avgSpeedInKnots, absTwaInDegrees);
            absTwaInDegrees = Math.abs(windCourse.reverse()
                    .getDifferenceTo(maneuver.getSpeedWithBearingAfter().getBearing()).getDegrees());
            avgSpeedInKnots = maneuver.getSpeedWithBearingAfter().getKnots();
            WindSpeedRange currentTwaWindSpeedRange = getWindSpeedRange(boatClass, avgSpeedInKnots, absTwaInDegrees);
            if (currentTwaWindSpeedRange != null) {
                windSpeedRange = windSpeedRange == null ? currentTwaWindSpeedRange
                        : windSpeedRange.extend(currentTwaWindSpeedRange);
            }
        }
        if (windSpeedRange != null) {
            return new KnotSpeedImpl(windSpeedRange.getMiddleSpeed());
        }
        return new KnotSpeedImpl(0.0);
    }

}
