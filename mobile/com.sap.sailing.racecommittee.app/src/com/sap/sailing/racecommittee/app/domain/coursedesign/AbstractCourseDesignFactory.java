package com.sap.sailing.racecommittee.app.domain.coursedesign;

import java.text.DecimalFormat;
import java.util.Set;

import com.sap.sse.common.Position;
import com.sap.sse.common.impl.NauticalMileDistance;
import com.sap.sailing.racecommittee.app.utils.GeoUtils;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

public abstract class AbstractCourseDesignFactory implements CourseDesignFactory {
    protected CourseDesign product;
    private final int ANGLE_OF_START_LINE_TO_WIND = 270;
    private final Distance REFERENCE_POINT_DISTANCE_FROM_START_LINE = new NauticalMileDistance(0.05);
    DecimalFormat distanceFormat = new DecimalFormat("0.00");

    abstract protected Set<PositionedMark> computeDesignSpecificMarks(Position startBoatPosition, Double windSpeed,
            Bearing windDirection, BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds,
            TargetTime targetTime);

    abstract protected void setCourseDesignDescription(Position startBoatPosition, Double windSpeed,
            Bearing windDirection, BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds,
            TargetTime targetTime);

    @Override
    public abstract CourseDesign createCourseDesign(Position startBoatPosition, Double windSpeed, Bearing windDirection,
            BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds, TargetTime targetTime);

    protected void initializeCourseDesign(Position startBoatPosition, Double windSpeed, Bearing windDirection,
            BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds, TargetTime targetTime) {
        this.product.setStartBoatPosition(startBoatPosition);
        this.product.setWindSpeed(windSpeed);
        this.product.setWindDirection(windDirection);
        setPinEnd(boatClass, startBoatPosition, windDirection);
        setReferencePoint(boatClass, startBoatPosition, windDirection);
    }

    protected void finalizeCourseDesign(Position startBoatPosition, Double windSpeed, Bearing windDirection,
            BoatClassType boatClass, CourseLayouts courseLayout, NumberOfRounds numberOfRounds, TargetTime targetTime) {
        this.product.getCourseDesignSpecificMarks().addAll(this.computeDesignSpecificMarks(startBoatPosition, windSpeed,
                windDirection, boatClass, courseLayout, numberOfRounds, targetTime));
    }

    protected void setPinEnd(BoatClassType boatClass, Position startBoatPosition, Bearing windDirection) {

        PositionedMark pinEnd = new PositionedMarkImpl(
                "start pin, start line length: " + boatClass.getStartLineLength().getMeters() + "m",
                GeoUtils.getPositionForGivenPointDistanceAndBearing(startBoatPosition, boatClass.getStartLineLength(),
                        windDirection.add(new DegreeBearingImpl(ANGLE_OF_START_LINE_TO_WIND))));
        product.setPinEnd(pinEnd);
    }

    protected void setReferencePoint(BoatClassType boatClass, Position startBoatPosition, Bearing windDirection) {
        Position startLineMid = GeoUtils.getPositionForGivenPointDistanceAndBearing(startBoatPosition,
                boatClass.getStartLineLength().scale(0.5),
                windDirection.add(new DegreeBearingImpl(ANGLE_OF_START_LINE_TO_WIND)));
        Position referencePoint = GeoUtils.getPositionForGivenPointDistanceAndBearing(startLineMid,
                REFERENCE_POINT_DISTANCE_FROM_START_LINE, windDirection);
        product.setReferencePoint(referencePoint);
    }

}
