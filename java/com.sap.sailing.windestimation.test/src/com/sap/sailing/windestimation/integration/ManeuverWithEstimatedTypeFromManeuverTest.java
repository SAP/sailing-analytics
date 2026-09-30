package com.sap.sailing.windestimation.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverLoss;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverWithMainCurveBoundariesImpl;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.ManeuverWithEstimatedType;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimationImpl;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedTypeImpl;
import com.sap.sailing.windestimation.windinference.DummyBasedTwsCalculatorImpl;
import com.sap.sailing.windestimation.windinference.MiddleCourseBasedTwdCalculatorImpl;
import com.sap.sailing.windestimation.windinference.WindTrackCalculator;
import com.sap.sailing.windestimation.windinference.WindTrackCalculatorImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MeterDistance;

/**
 * Deals with converting a {@link Maneuver} instance into a {@link ManeuverWithEstimatedType} object. This will be
 * useful in producing an estimated wind track from maneuvers loaded from the database.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ManeuverWithEstimatedTypeFromManeuverTest {
    @Test
    public void testProducingManeuverWithEstimatedTypeFromManeuver() {
        final BoatClass boatClass = new BoatClassImpl(BoatClassMasterdata._5O5);
        final ManeuverCurveBoundariesImpl mainCurveBoundaries = new ManeuverCurveBoundariesImpl(
                TimePoint.now().minus(Duration.ONE_SECOND), TimePoint.now().plus(Duration.ONE_SECOND),
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(10)),
                new KnotSpeedWithBearingImpl(9, new DegreeBearingImpl(100)), 90, new KnotSpeedImpl(5),
                new KnotSpeedImpl(7));
        final ManeuverCurveBoundariesImpl maneuverCurveWithStableSpeedAndCourseBoundaries = new ManeuverCurveBoundariesImpl(
                TimePoint.now().minus(Duration.ONE_SECOND), TimePoint.now().plus(Duration.ONE_SECOND),
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(10)),
                new KnotSpeedWithBearingImpl(9, new DegreeBearingImpl(100)), 90, new KnotSpeedImpl(5),
                new KnotSpeedImpl(7));
        final Maneuver maneuver = new ManeuverWithMainCurveBoundariesImpl(ManeuverType.TACK, Tack.PORT,
                new DegreePosition(54, 8), TimePoint.now(), mainCurveBoundaries,
                maneuverCurveWithStableSpeedAndCourseBoundaries, 5.0, /* markPassing */ null,
                new ManeuverLoss(/* distanceSailedProjectedOnMiddleManeuverAngle */ new MeterDistance(10),
                        /* distanceSailedIfNotManeuveringProjectedOnMiddleManeuverAngle */ new MeterDistance(20),
                        new DegreePosition(55, 9), new DegreePosition(56, 10), Duration.ONE_SECOND.times(20),
                        new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(10)), new DegreeBearingImpl(15)));
        final SimpleManeuverForEstimation maneuverForEstimation = getManeuverForEstimation(maneuver, boatClass);
        final SimpleManeuverWithEstimatedTypeImpl<SimpleManeuverForEstimation> maneuverWithEstimatedType = new SimpleManeuverWithEstimatedTypeImpl<>(
                maneuverForEstimation, mapManeuverType(maneuver.getType()), 0.7);
        final WindTrackCalculator calculator = new WindTrackCalculatorImpl(new MiddleCourseBasedTwdCalculatorImpl(),
                new DummyBasedTwsCalculatorImpl());
        final List<WindWithConfidence<Pair<Position, TimePoint>>> track = calculator
                .getWindTrackFromManeuverClassifications(Arrays.asList(maneuverWithEstimatedType));
        assertEquals(1, track.size());
        assertEquals(55., track.get(0).getObject().getFrom().getDegrees(), 0.0001); // 55deg is the middle between course in (10deg) and course out (100deg)
    }

    private SimpleManeuverForEstimation getManeuverForEstimation(Maneuver maneuver, BoatClass boatClass) {
        return new SimpleManeuverForEstimationImpl(maneuver.getTimePoint(), maneuver.getPosition(), maneuver.getMainCurveBoundaries().getMiddleCourse(),
                maneuver.getSpeedWithBearingBefore(), maneuver.getSpeedWithBearingAfter(), /* is clean */ true, boatClass);
    }

    private ManeuverTypeForClassification mapManeuverType(ManeuverType type) {
        switch (type) {
        case BEAR_AWAY:
            return ManeuverTypeForClassification.BEAR_AWAY;
        case HEAD_UP:
            return ManeuverTypeForClassification.HEAD_UP;
        case JIBE:
            return ManeuverTypeForClassification.JIBE;
        case PENALTY_CIRCLE:
            return null;
        case TACK:
            return ManeuverTypeForClassification.TACK;
        case UNKNOWN:
            return null;
        default:
            return null;
        }
    }
}
