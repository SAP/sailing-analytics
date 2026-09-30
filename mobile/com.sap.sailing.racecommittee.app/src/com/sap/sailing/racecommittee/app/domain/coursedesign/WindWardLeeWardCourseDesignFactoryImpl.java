package com.sap.sailing.racecommittee.app.domain.coursedesign;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sse.common.Position;
import com.sap.sse.common.impl.NauticalMileDistance;
import com.sap.sailing.racecommittee.app.utils.GeoUtils;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class WindWardLeeWardCourseDesignFactoryImpl extends AbstractCourseDesignFactory {
    private final int LUV_BUOY1_ANGLE_TO_WIND = 0;
    private final int LUV_BUOY2_ANGLE_TO_WIND = 100;
    private final Distance LUV_BUOY1_TO_LUV_BUOY2_DISTANCE = new NauticalMileDistance(0.03);
    private final int GATE_LENGTH_TO_HULL_LENGTH_FACTOR = 10;
    private final int GATE_4S_WIND_ANGLE = 270;
    private final int GATE_4P_WIND_ANGLE = 90;
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
        result.add(new PositionedMarkImpl("4S",
                GeoUtils.getPositionForGivenPointDistanceAndBearing(this.product.getReferencePoint(),
                        boatClass.getHullLength().scale(GATE_LENGTH_TO_HULL_LENGTH_FACTOR / 2),
                        windDirection.add(new DegreeBearingImpl(GATE_4S_WIND_ANGLE)))));
        result.add(new PositionedMarkImpl("4P",
                GeoUtils.getPositionForGivenPointDistanceAndBearing(this.product.getReferencePoint(),
                        boatClass.getHullLength().scale(GATE_LENGTH_TO_HULL_LENGTH_FACTOR / 2),
                        windDirection.add(new DegreeBearingImpl(GATE_4P_WIND_ANGLE)))));

        // luv buoy calculation
        Map<PointOfSail, Float> speedTable = null;
        for (Entry<WindRange, Map<PointOfSail, Float>> windRangeToSpeedTable : boatClass.getBoatSpeedTable()
                .entrySet()) {
            if (windRangeToSpeedTable.getKey().isInRange(windSpeed)) {
                speedTable = windRangeToSpeedTable.getValue();
                break;
            }
        }
        if (speedTable == null) {
            throw new IllegalArgumentException(
                    "There was no speed diagram for the given boat class and the given wind.");
        }
        double legLength;
        if (((WindWardLeeWardCourseLayouts) courseLayout).isUpWindFinish()) {
            legLength = (targetTime.getTimeInMinutes()
                    / (speedTable.get(PointOfSail.Downwind) * (numberOfRounds.getNumberOfRounds() - 1)
                            + (speedTable.get(PointOfSail.Upwind) * numberOfRounds.getNumberOfRounds())));
        } else {
            legLength = (targetTime.getTimeInMinutes()
                    / (speedTable.get(PointOfSail.Downwind) * (numberOfRounds.getNumberOfRounds())
                            + (speedTable.get(PointOfSail.Upwind) * numberOfRounds.getNumberOfRounds())));
        }
        legDistance = new NauticalMileDistance(legLength);
        Position luvBuoyPosition = GeoUtils.getPositionForGivenPointDistanceAndBearing(this.product.getReferencePoint(),
                legDistance, windDirection.add(new DegreeBearingImpl(LUV_BUOY1_ANGLE_TO_WIND)));
        result.add(new PositionedMarkImpl("1A", luvBuoyPosition));

        result.add(new PositionedMarkImpl("1", GeoUtils.getPositionForGivenPointDistanceAndBearing(luvBuoyPosition,
                LUV_BUOY1_TO_LUV_BUOY2_DISTANCE, windDirection.add(new DegreeBearingImpl(LUV_BUOY2_ANGLE_TO_WIND)))));

        return result;
    }

    @Override
    protected void setCourseDesignDescription(Position startBoatPosition, Double windSpeed, Bearing windDirection,
            BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds, TargetTime targetTime) {
        StringBuilder courseDesignDescription = new StringBuilder();
        courseDesignDescription.append(boatClass.toString());
        courseDesignDescription.append(", course: ");
        courseDesignDescription.append(courseLayout.getShortName());
        courseDesignDescription.append(numberOfRounds);
        courseDesignDescription.append(", target time: ");
        courseDesignDescription.append(targetTime.getTimeInMinutes());
        courseDesignDescription.append(" min, upwind leg: ");
        courseDesignDescription.append(distanceFormat.format(legDistance.getNauticalMiles()));
        courseDesignDescription.append(" nm");
        this.product.setCourseDesignDescription(courseDesignDescription.toString());
    }
}
