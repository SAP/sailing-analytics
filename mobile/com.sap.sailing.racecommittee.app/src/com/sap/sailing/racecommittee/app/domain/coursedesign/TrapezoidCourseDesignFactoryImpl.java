package com.sap.sailing.racecommittee.app.domain.coursedesign;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;

public class TrapezoidCourseDesignFactoryImpl extends AbstractCourseDesignFactory {

    // we use 0.67 here, because also ISAF doesn't calculate with 1/3
    private final double REACH_LEG_FACTOR = 0.67;
    private final Distance FINISH_LEG_LENGTH = new NauticalMileDistance(0.15);
    private final int LUV_BUOY_ANGLE_TO_WIND_OFFSET = 0;
    private final int BUOY2_ANGLE_TO_WIND = 180;
    private final int GATE_LENGTH_TO_HULL_LENGTH_FACTOR = 10;
    private final int GATE_XS_WIND_ANGLE = 270;
    private final int GATE_XP_WIND_ANGLE = 90;
    private final int FINISH_S_WIND_ANGLE = 180;
    private final int FINISH_P_WIND_ANGLE = 0;
    private Distance legDistance;

    @Override
    public CourseDesign createCourseDesign(Position startBoatPosition, Double windSpeed, Bearing windDirection,
            BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds, TargetTime targetTime) {
        this.product = new WindWardLeeWardCourseDesignImpl();
        this.initializeCourseDesign(startBoatPosition, windSpeed, windDirection, boatClass, courseLayout,
                numberOfRounds, targetTime);
        this.finalizeCourseDesign(startBoatPosition, windSpeed, windDirection, boatClass, courseLayout, numberOfRounds,
                targetTime);
        setCourseDesignDescription(startBoatPosition, windSpeed, windDirection, boatClass, courseLayout, numberOfRounds,
                targetTime);
        return this.product;
    }

    @Override
    protected Set<PositionedMark> computeDesignSpecificMarks(Position startBoatPosition, Double windSpeed,
            Bearing windDirection, BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds,
            TargetTime targetTime) {
        Set<PositionedMark> result = new HashSet<PositionedMark>();

        // gate calculation
        result.add(new PositionedMarkImpl("4S", getPositionForGivenPointDistanceAndBearing(this.product
                .getReferencePoint(), boatClass.getHullLength().scale(GATE_LENGTH_TO_HULL_LENGTH_FACTOR / 2),
                windDirection.add(new DegreeBearingImpl(GATE_XS_WIND_ANGLE)))));
        result.add(new PositionedMarkImpl("4P", getPositionForGivenPointDistanceAndBearing(this.product
                .getReferencePoint(), boatClass.getHullLength().scale(GATE_LENGTH_TO_HULL_LENGTH_FACTOR / 2),
                windDirection.add(new DegreeBearingImpl(GATE_XP_WIND_ANGLE)))));

        // luv buoy calculation
        Map<PointOfSail, Float> speedTable = null;
        for (Entry<WindRange, Map<PointOfSail, Float>> windRangeToSpeedTable : boatClass.getBoatSpeedTable().entrySet()) {
            if (windRangeToSpeedTable.getKey().isInRange(windSpeed)) {
                speedTable = windRangeToSpeedTable.getValue();
                break;
            }
        }
        if (speedTable == null) {
            throw new IllegalArgumentException(
                    "There was no speed diagram for the given boat class and the given wind.");
        }

        // alternate form of:
        // number_of_rounds*(leg_length*upwind_speed)+number_of_rounds*(leg_length*d)+(REACH_LEG_FACTOR*leg_length*reach_speed)+FINISH_LEG_LENGTH*reach_speed=target_time
        double legLength = (targetTime.getTimeInMinutes() - FINISH_LEG_LENGTH.getNauticalMiles()
                * speedTable.get(PointOfSail.Reach))
                / (speedTable.get(PointOfSail.Downwind) * numberOfRounds.getNumberOfRounds()
                        + (speedTable.get(PointOfSail.Upwind) * numberOfRounds.getNumberOfRounds()) + REACH_LEG_FACTOR
                        * speedTable.get(PointOfSail.Reach));

        legDistance = new NauticalMileDistance(legLength);
        Position luvBuoyPosition = getPositionForGivenPointDistanceAndBearing(this.product.getReferencePoint(),
                legDistance, windDirection.add(new DegreeBearingImpl(LUV_BUOY_ANGLE_TO_WIND_OFFSET)));
        result.add(new PositionedMarkImpl("1",
                luvBuoyPosition));

        // reach leg
        Position topReachBuoy = getPositionForGivenPointDistanceAndBearing(
                luvBuoyPosition,
                legDistance.scale(REACH_LEG_FACTOR),
                windDirection.add(new DegreeBearingImpl(BUOY2_ANGLE_TO_WIND
                        + ((TrapezoidCourseLayouts) courseLayout).getReachAngle())));
        result.add(new PositionedMarkImpl("2", topReachBuoy));

        // downwind gate
        Position downWindGateMid = getPositionForGivenPointDistanceAndBearing(topReachBuoy, legDistance,
                windDirection.add(new DegreeBearingImpl(BUOY2_ANGLE_TO_WIND)));

        result.add(new PositionedMarkImpl("3S", getPositionForGivenPointDistanceAndBearing(downWindGateMid, boatClass
                .getHullLength().scale(GATE_LENGTH_TO_HULL_LENGTH_FACTOR / 2), windDirection.add(new DegreeBearingImpl(
                GATE_XS_WIND_ANGLE)))));
        result.add(new PositionedMarkImpl("3P", getPositionForGivenPointDistanceAndBearing(downWindGateMid, boatClass
                .getHullLength().scale(GATE_LENGTH_TO_HULL_LENGTH_FACTOR / 2), windDirection.add(new DegreeBearingImpl(
                GATE_XP_WIND_ANGLE)))));

        // finish gate
        Position finishGateMid = getPositionForGivenPointDistanceAndBearing(downWindGateMid, FINISH_LEG_LENGTH,
                windDirection.add(new DegreeBearingImpl(BUOY2_ANGLE_TO_WIND
                        -((TrapezoidCourseLayouts) courseLayout).getReachAngle())));

        result.add(new PositionedMarkImpl("FS", getPositionForGivenPointDistanceAndBearing(finishGateMid, boatClass
                .getStartLineLength().scale(0.5), windDirection.add(new DegreeBearingImpl(
                        FINISH_S_WIND_ANGLE+((TrapezoidCourseLayouts) courseLayout).getReachAngle()))), MarkType.FINISHBOAT));
        result.add(new PositionedMarkImpl("FP", getPositionForGivenPointDistanceAndBearing(finishGateMid, boatClass
                .getStartLineLength().scale(0.5), windDirection.add(new DegreeBearingImpl(
                        FINISH_P_WIND_ANGLE+((TrapezoidCourseLayouts) courseLayout).getReachAngle()))), MarkType.FINISHBOAT));

        return result;
    }

    @Override
    protected void setCourseDesignDescription(Position startBoatPosition, Double windSpeed, Bearing windDirection,
            BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds, TargetTime targetTime) {
        StringBuffer courseDesignDescription = new StringBuffer();
        courseDesignDescription.append(boatClass.toString());
        courseDesignDescription.append(", course: ");
        courseDesignDescription.append(courseLayout.getShortName());
        courseDesignDescription.append(numberOfRounds);
        courseDesignDescription.append(", target time: ");
        courseDesignDescription.append(targetTime.getTimeInMinutes());
        courseDesignDescription.append(" min, upwind leg: ");
        courseDesignDescription.append(distanceFormat.format(legDistance.getNauticalMiles()));
        courseDesignDescription.append(" nm, reach leg: ");
        courseDesignDescription.append(distanceFormat.format(legDistance.scale(REACH_LEG_FACTOR).getNauticalMiles()));
        courseDesignDescription.append(" nm");
        this.product.setCourseDesignDescription(courseDesignDescription.toString());
    }
}
