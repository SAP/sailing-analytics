package com.sap.sailing.windestimation.data.transformer;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.ManeuverCategory;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;

import smile.sort.QuickSelect;

/**
 * Converts instances which implement {@link ConvertableToManeuverForEstimation} to {@link ManeuverForEstimation}
 * instances.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverForEstimationTransformer
        implements CompetitorTrackTransformer<ConvertableToManeuverForEstimation, ManeuverForEstimation> {

    public static final int MIN_SECONDS_TO_OTHER_MANEUVER = 4;

    public List<ManeuverForEstimation> getManeuversForEstimation(
            List<? extends ConvertableToManeuverForEstimation> convertableManeuvers, BoatClass boatClass, String competitorName) {
        double speedScalingDivisor = getSpeedScalingDivisor(convertableManeuvers);
        List<ManeuverForEstimation> maneuversForEstimation = new ArrayList<>();
        for (ConvertableToManeuverForEstimation maneuver : convertableManeuvers) {
            ManeuverForEstimation maneuverForEstimation = getManeuverForEstimation(maneuver, speedScalingDivisor,
                    boatClass, competitorName);
            maneuversForEstimation.add(maneuverForEstimation);
        }
        return maneuversForEstimation;
    }

    public double getSpeedScalingDivisor(List<? extends ConvertableToManeuverForEstimation> competitorTrackManeuvers) {
        List<Double> speedsInKnotsOfCleanSailingSegments = new ArrayList<>();
        ConvertableToManeuverForEstimation previousManeuver = null;
        ConvertableToManeuverForEstimation maneuver = null;
        for (ConvertableToManeuverForEstimation nextManeuver : competitorTrackManeuvers) {
            if (maneuver != null) {
                if (previousManeuver != null
                        && isSegmentBetweenManeuversEligibleForPolarsCollection(previousManeuver, maneuver)) {
                    speedsInKnotsOfCleanSailingSegments.add(maneuver.getSpeedWithBearingBefore().getKnots());
                }
                if (isSegmentBetweenManeuversEligibleForPolarsCollection(maneuver, nextManeuver)) {
                    speedsInKnotsOfCleanSailingSegments.add(maneuver.getSpeedWithBearingAfter().getKnots());
                }
            }
            previousManeuver = maneuver;
            maneuver = nextManeuver;
        }
        if (maneuver != null) {
            if (isSegmentBetweenManeuversEligibleForPolarsCollection(previousManeuver, maneuver)) {
                speedsInKnotsOfCleanSailingSegments.add(maneuver.getSpeedWithBearingBefore().getKnots());
            }
            if (isSegmentBetweenManeuversEligibleForPolarsCollection(maneuver, null)) {
                speedsInKnotsOfCleanSailingSegments.add(maneuver.getSpeedWithBearingAfter().getKnots());
            }
        }
        double[] speedsArray = new double[speedsInKnotsOfCleanSailingSegments.size()];
        int i = 0;
        for (Double speed : speedsInKnotsOfCleanSailingSegments) {
            speedsArray[i++] = speed;
        }
        if (speedsArray.length > 0) {
            double scalingDivisor = QuickSelect.select(speedsArray, (int) (speedsArray.length * 0.98));
            return scalingDivisor;
        } else {
            return 10;
        }
    }

    public boolean isSegmentBetweenManeuversEligibleForPolarsCollection(ConvertableToManeuverForEstimation fromManeuver,
            ConvertableToManeuverForEstimation toManeuver) {
        if (fromManeuver == null || toManeuver == null) {
            if (fromManeuver == toManeuver) {
                // both are null
                return false;
            }
            if (fromManeuver == null) {
                return isManeuverBoundariesDataClean(toManeuver, true, false);
            }
        }
        return isManeuverBoundariesDataClean(fromManeuver, false, true);
    }

    public boolean isManeuverBoundariesDataClean(ConvertableToManeuverForEstimation maneuver,
            boolean validateManeuverEntering, boolean validateManeuverExiting) {
        return maneuver.getLongestIntervalBetweenTwoFixes().asSeconds() <= 4
                && (!validateManeuverEntering
                        || maneuver.getIntervalBetweenFirstFixOfCurveAndPreviousFix().asSeconds() <= 4)
                && (!validateManeuverExiting || maneuver.getIntervalBetweenLastFixOfCurveAndNextFix().asSeconds() <= 4)
                && (!validateManeuverEntering || maneuver.getSpeedWithBearingBefore().getKnots() > 2)
                && (!validateManeuverExiting || maneuver.getSpeedWithBearingAfter().getKnots() > 2)
                && (!validateManeuverEntering
                        || maneuver.getDurationFromPreviousManeuverEndToManeuverStart()
                                .asSeconds() >= MIN_SECONDS_TO_OTHER_MANEUVER
                        || maneuver.getCourseChangeInDegreesWithinTurningSectionOfPreviousManeuver() != null && Math
                                .abs(maneuver.getCourseChangeInDegreesWithinTurningSectionOfPreviousManeuver()) < Math
                                        .abs(maneuver.getCourseChangeInDegreesWithinTurningSection()) * 0.3)
                && (!validateManeuverExiting
                        || maneuver.getDurationFromManeuverEndToNextManeuverStart()
                                .asSeconds() >= MIN_SECONDS_TO_OTHER_MANEUVER
                        || maneuver.getCourseChangeInDegreesWithinTurningSectionOfNextManeuver() != null && Math
                                .abs(maneuver.getCourseChangeInDegreesWithinTurningSectionOfNextManeuver()) < Math
                                        .abs(maneuver.getCourseChangeInDegreesWithinTurningSection()) * 0.3);
    }

    public boolean isManeuverClean(ConvertableToManeuverForEstimation maneuver) {
        return isManeuverEligibleForAnalysis(maneuver.getCourseChangeInDegrees(),
                maneuver.getCourseChangeInDegreesWithinTurningSection())
                && isManeuverBoundariesDataClean(maneuver, true, true)
                && Math.abs(maneuver.getSpeedWithBearingBefore().getKnots()
                        - maneuver.getSpeedWithBearingAfter().getKnots())
                        * 3 < Math.min(maneuver.getSpeedWithBearingBefore().getKnots(),
                                maneuver.getSpeedWithBearingAfter().getKnots())
                && Math.abs(maneuver.getCourseChangeInDegreesWithinTurningSection()
                        - maneuver.getCourseChangeInDegrees()) < Math
                                .min(Math.abs(maneuver.getCourseChangeInDegrees()) / 2.0, 40);
    }

    public ManeuverCategory getManeuverCategory(ConvertableToManeuverForEstimation maneuver) {
        return getManeuverCategory(maneuver.getCourseChangeInDegreesWithinTurningSection(), maneuver.isMarkPassing());
    }

    public boolean isManeuverEligibleForAnalysis(double courseChangeInDegrees,
            double courseChangeWithinTurningSectionInDegrees) {
        return getManeuverCategory(courseChangeWithinTurningSectionInDegrees, false) == ManeuverCategory.REGULAR
                && getManeuverCategory(courseChangeInDegrees, false) == ManeuverCategory.REGULAR;
    }

    public ManeuverCategory getManeuverCategory(double courseChangeWithinTurningSectionInDegrees, boolean markPassing) {
        double absCourseChangeInDegrees = Math.abs(courseChangeWithinTurningSectionInDegrees);
        if (absCourseChangeInDegrees < 30) {
            return ManeuverCategory.SMALL;
        }
        if (absCourseChangeInDegrees <= 120) {
            return markPassing ? ManeuverCategory.MARK_PASSING : ManeuverCategory.REGULAR;
        }
        if (absCourseChangeInDegrees <= 150) {
            return ManeuverCategory.WIDE;
        }
        if (absCourseChangeInDegrees <= 310) {
            return ManeuverCategory._180;
        }
        return ManeuverCategory._360;
    }

    public ManeuverForEstimation getManeuverForEstimation(ConvertableToManeuverForEstimation maneuver,
            double speedScalingDivisor, BoatClass boatClass, String competitorName) {
        ManeuverCategory maneuverCategory = getManeuverCategory(maneuver);
        double speedLossRatio = maneuver.getSpeedWithBearingBefore().getKnots() > 0
                ? maneuver.getLowestSpeed().getKnots() / maneuver.getSpeedWithBearingBefore().getKnots()
                : 0;
        double speedGainRatio = maneuver.getHighestSpeedWithinTurningSection().getKnots() > 0
                ? maneuver.getSpeedWithBearingBeforeWithinTurningSection().getKnots()
                        / maneuver.getHighestSpeedWithinTurningSection().getKnots()
                : 0;
        double lowestSpeedVsExitingSpeedRatio = maneuver.getSpeedWithBearingAfter().getKnots() > 0
                ? maneuver.getLowestSpeed().getKnots() / maneuver.getSpeedWithBearingAfter().getKnots()
                : 0;
        Double deviationFromOptimalTackAngleInDegrees = maneuver.getTargetTackAngleInDegrees() == null ? null
                : Math.abs(maneuver.getCourseChangeInDegrees()) - maneuver.getTargetTackAngleInDegrees();
        Double deviationFromOptimalJibeAngleInDegrees = maneuver.getTargetJibeAngleInDegrees() == null ? null
                : Math.abs(maneuver.getCourseChangeInDegrees()) - maneuver.getTargetJibeAngleInDegrees();
        boolean clean = isManeuverClean(maneuver);
        double scaledSpeedBeforeInKnots = maneuver.getSpeedWithBearingBefore().getKnots() / speedScalingDivisor;
        double scaledSpeedAfterInKnots = maneuver.getSpeedWithBearingAfter().getKnots() / speedScalingDivisor;
        boolean markPassingDataAvailable = maneuver.hasMarkPassingData();
        ManeuverForEstimation maneuverForEstimation = new ManeuverForEstimation(maneuver.getTimePoint(),
                maneuver.getPosition(), maneuver.getMiddleCourse(), maneuver.getSpeedWithBearingBefore(),
                maneuver.getSpeedWithBearingAfter(), maneuver.getCourseChangeInDegrees(),
                maneuver.getCourseChangeInDegreesWithinTurningSection(), maneuver.getMaxTurningRateInDegreesPerSecond(),
                deviationFromOptimalTackAngleInDegrees, deviationFromOptimalJibeAngleInDegrees, speedLossRatio,
                speedGainRatio, lowestSpeedVsExitingSpeedRatio, clean, maneuverCategory, scaledSpeedBeforeInKnots,
                scaledSpeedAfterInKnots, maneuver.isMarkPassing(), boatClass, markPassingDataAvailable,
                competitorName);
        return maneuverForEstimation;
    }

    @Override
    public List<ManeuverForEstimation> transformElements(
            CompetitorTrackWithEstimationData<ConvertableToManeuverForEstimation> competitorTrackWithElementsToTransform) {
        return getManeuversForEstimation(competitorTrackWithElementsToTransform.getElements(),
                competitorTrackWithElementsToTransform.getBoatClass(), competitorTrackWithElementsToTransform.getCompetitorName());
    }

}
