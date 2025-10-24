package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.datamining.data.HasManeuverContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.impl.data.ManeuverWithContext;
import com.sap.sailing.datamining.shared.ManeuverSettings;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;

public class ManeuverRetrievalProcessor
        extends AbstractRetrievalProcessor<HasTrackedLegOfCompetitorContext, HasManeuverContext> {

    private final ManeuverSettings settings;

    public ManeuverRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasManeuverContext, ?>> resultReceivers, ManeuverSettings settings,
            int retrievalLevel, String retrievedDataTypeMessageKey) {
        super(HasTrackedLegOfCompetitorContext.class, HasManeuverContext.class, executor, resultReceivers,
                retrievalLevel, retrievedDataTypeMessageKey);
        this.settings = settings;
    }

    @Override
    protected Iterable<HasManeuverContext> retrieveData(HasTrackedLegOfCompetitorContext element) {
        Collection<HasManeuverContext> maneuversWithContext = new ArrayList<>();
        TimePoint finishTime = element.getTrackedLegOfCompetitor().getFinishTime();
        if (finishTime != null) {
            Iterable<Maneuver> maneuvers = null;
            if (!isLastLeg(element)) {
                finishTime = finishTime.minus(1);
            }
            try {
                maneuvers = element.getTrackedLegOfCompetitor().getManeuvers(finishTime, false);
            } catch (NoWindException e) {
                throw new IllegalStateException("No wind retrieving the maneuvers", e);
            }
            Maneuver previousManeuver = null;
            Maneuver currentManeuver = null;
            for (Maneuver nextManeuver : maneuvers) {
                if (isAborted()) {
                    break;
                }
                
                if (currentManeuver != null) {
                    ManeuverWithContext maneuverWithContext = new ManeuverWithContext(element, currentManeuver,
                            settings.isMainCurveAnalysis(), previousManeuver, nextManeuver);
                    if (isManeuverCompliantWithSettings(previousManeuver, maneuverWithContext, nextManeuver)) {
                        maneuversWithContext.add(maneuverWithContext);
                    }
                }
                previousManeuver = currentManeuver;
                currentManeuver = nextManeuver;
            }
            if (currentManeuver != null) {
                ManeuverWithContext maneuverWithContext = new ManeuverWithContext(element, currentManeuver,
                        settings.isMainCurveAnalysis(), previousManeuver, null);
                if (isManeuverCompliantWithSettings(previousManeuver, maneuverWithContext, null)) {
                    maneuversWithContext.add(maneuverWithContext);
                }
            }
        }
        return maneuversWithContext;
    }

    private boolean isLastLeg(HasTrackedLegOfCompetitorContext element) {
        if(element.getTrackedLegContext().getTrackedRaceContext().getRace() != null) {
            Waypoint finishWaypoint = element.getTrackedLegContext().getTrackedRaceContext().getRace().getCourse().getLastWaypoint();
            Waypoint toWaypoint = element.getTrackedLegOfCompetitor().getLeg().getTo();
            return finishWaypoint.equals(toWaypoint);
        }
        return false;
    }

    private boolean isManeuverCompliantWithSettings(Maneuver previousManeuver,
            ManeuverWithContext currentManeuverWithContext, Maneuver nextManeuver) {
        boolean mainCurveAnalysis = settings.isMainCurveAnalysis();
        // Compute only numbers which are really required for filtering
        Duration maneuverDuration = settings.getMinManeuverDuration() != null
                || settings.getMaxManeuverDuration() != null
                        ? currentManeuverWithContext.getTimePointBeforeForAnalysis()
                                .until(currentManeuverWithContext.getTimePointAfterForAnalysis())
                        : null;
        double maneuverEnteringSpeed = settings.getMinManeuverEnteringSpeedInKnots() != null
                || settings.getMaxManeuverEnteringSpeedInKnots() != null
                        ? currentManeuverWithContext.getManeuverEnteringSpeed() : 0;
        double maneuverExitingSpeed = settings.getMinManeuverExitingSpeedInKnots() != null
                || settings.getMaxManeuverExitingSpeedInKnots() != null
                        ? currentManeuverWithContext.getManeuverExitingSpeed() : 0;
        double maneuverEnteringAbsTWA = settings.getMinManeuverEnteringAbsTWA() != null
                ? currentManeuverWithContext.getEnteringAbsTWA() : 0;
        double maneuverExitingAbsTWA = settings.getMinManeuverExitingAbsTWA() != null
                ? currentManeuverWithContext.getExitingAbsTWA() : 0;

        Duration durationToPreviousManeuver = previousManeuver != null
                ? getManeuverBoundariesForAnalysis(previousManeuver, mainCurveAnalysis).getTimePointAfter()
                        .until(currentManeuverWithContext.getTimePointBeforeForAnalysis())
                : null;
        Duration durationToNextManeuver = nextManeuver != null
                ? currentManeuverWithContext.getTimePointAfterForAnalysis().until(
                        getManeuverBoundariesForAnalysis(nextManeuver, mainCurveAnalysis).getTimePointBefore())
                : null;
        if (!(settings.getMinManeuverDuration() != null
                && maneuverDuration.compareTo(settings.getMinManeuverDuration()) < 0
                || settings.getMaxManeuverDuration() != null
                        && maneuverDuration.compareTo(settings.getMaxManeuverDuration()) > 0
                || settings.getMinManeuverEnteringSpeedInKnots() != null
                        && maneuverEnteringSpeed < settings.getMinManeuverEnteringSpeedInKnots()
                || settings.getMaxManeuverEnteringSpeedInKnots() != null
                        && maneuverEnteringSpeed > settings.getMaxManeuverEnteringSpeedInKnots()
                || settings.getMinManeuverExitingSpeedInKnots() != null
                        && maneuverExitingSpeed < settings.getMinManeuverExitingSpeedInKnots()
                || settings.getMaxManeuverExitingSpeedInKnots() != null
                        && maneuverExitingSpeed > settings.getMaxManeuverExitingSpeedInKnots()
                || settings.getMinManeuverEnteringAbsTWA() != null
                        && maneuverEnteringAbsTWA < settings.getMinManeuverEnteringAbsTWA()
                || settings.getMaxManeuverEnteringAbsTWA() != null
                        && maneuverEnteringAbsTWA > settings.getMaxManeuverEnteringAbsTWA()
                || settings.getMinManeuverExitingAbsTWA() != null
                        && maneuverExitingAbsTWA < settings.getMinManeuverExitingAbsTWA()
                || settings.getMaxManeuverExitingAbsTWA() != null
                        && maneuverExitingAbsTWA > settings.getMaxManeuverExitingAbsTWA()
                || settings.getMinAbsCourseChangeInDegrees() != null && currentManeuverWithContext
                        .getAbsoluteDirectionChangeInDegrees() < settings.getMinAbsCourseChangeInDegrees()
                || settings.getMaxAbsCourseChangeInDegrees() != null && currentManeuverWithContext
                        .getAbsoluteDirectionChangeInDegrees() > settings.getMaxAbsCourseChangeInDegrees()
                || settings.getMinDurationFromPrecedingManeuver() != null && (previousManeuver == null
                        || durationToPreviousManeuver.compareTo(settings.getMinDurationFromPrecedingManeuver()) < 0)
                || settings.getMaxDurationFromPrecedingManeuver() != null && (previousManeuver == null
                        || durationToPreviousManeuver.compareTo(settings.getMaxDurationFromPrecedingManeuver()) > 0)
                || settings.getMinDurationToFollowingManeuver() != null && (nextManeuver == null
                        || durationToNextManeuver.compareTo(settings.getMinDurationToFollowingManeuver()) < 0)
                || settings.getMaxDurationToFollowingManeuver() != null && (nextManeuver == null
                        || durationToNextManeuver.compareTo(settings.getMaxDurationToFollowingManeuver()) > 0))) {
            return true;
        } else {
            return false;
        }
    }

    private ManeuverCurveBoundaries getManeuverBoundariesForAnalysis(Maneuver maneuver, boolean mainCurveAnalysis) {
        return mainCurveAnalysis ? maneuver.getMainCurveBoundaries()
                : maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries();
    }

}
