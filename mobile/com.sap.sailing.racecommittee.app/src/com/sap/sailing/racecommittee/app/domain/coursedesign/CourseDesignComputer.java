package com.sap.sailing.racecommittee.app.domain.coursedesign;

import com.sap.sse.common.Position;
import com.sap.sse.common.Bearing;

import android.util.Log;

public class CourseDesignComputer {
    private static final String TAG = CourseDesignComputer.class.getName();

    private Position startBoatPosition;
    private Double windSpeed;
    private Bearing windDirection;
    private BoatClassType boatClass;
    private CourseLayouts courseLayout;
    private NumberOfRounds numberOfRounds;
    private TargetTime targetTime;

    public Position getStartBoatPosition() {
        return startBoatPosition;
    }

    public CourseDesignComputer setStartBoatPosition(Position startBoatPosition) {
        this.startBoatPosition = startBoatPosition;
        return this;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public CourseDesignComputer setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
        return this;
    }

    public Bearing getWindDirection() {
        return windDirection;
    }

    public CourseDesignComputer setWindDirection(Bearing windDirection) {
        this.windDirection = windDirection;
        return this;
    }

    public BoatClassType getBoatClass() {
        return boatClass;
    }

    public CourseDesignComputer setBoatClass(BoatClassType boatClass) {
        this.boatClass = boatClass;
        return this;
    }

    public CourseLayouts getCourseLayout() {
        return courseLayout;
    }

    public CourseDesignComputer setCourseLayout(CourseLayouts courseLayout) {
        this.courseLayout = courseLayout;
        return this;
    }

    public NumberOfRounds getNumberOfRounds() {
        return numberOfRounds;
    }

    public CourseDesignComputer setNumberOfRounds(NumberOfRounds numberOfRounds) {
        this.numberOfRounds = numberOfRounds;
        return this;
    }

    public TargetTime getTargetTime() {
        return targetTime;
    }

    public CourseDesignComputer setTargetTime(TargetTime targetTime) {
        this.targetTime = targetTime;
        return this;
    }

    public CourseDesign compute() {
        CourseDesign computedCourseDesign = null;
        if (startBoatPosition != null && windSpeed != null && windDirection != null && boatClass != null
                && courseLayout != null && numberOfRounds != null && targetTime != null) {
            try {
                if (!boatClass.getPossibleCourseLayoutsWithTargetTime().keySet().contains(courseLayout)) {
                    throw new IllegalArgumentException("The given course design for the given boat class is illegal.");
                }
                computedCourseDesign = courseLayout.getCourseDesignFactoryClass().newInstance().createCourseDesign(
                        startBoatPosition, windSpeed, windDirection, boatClass, courseLayout, numberOfRounds,
                        targetTime);
            } catch (InstantiationException e) {
                Log.e(TAG, "Exception trying compute course design", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Exception trying compute course design", e);
            }
        } else
            throw new IllegalStateException("At least one mandatory parameter was not set in the computer!");
        return computedCourseDesign;
    }
}