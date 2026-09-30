package com.sap.sailing.racecommittee.app.domain.coursedesign;

import java.util.Set;

import com.sap.sse.common.Position;
import com.sap.sse.common.Bearing;

public interface CourseDesign {
    Position getStartBoatPosition();

    void setStartBoatPosition(Position startBoatPosition);

    Double getWindSpeed();

    void setWindSpeed(Double windSpeed);

    Bearing getWindDirection();

    void setWindDirection(Bearing windDirection);

    PositionedMark getPinEnd();

    public void setPinEnd(PositionedMark pinEnd);

    Position getReferencePoint();

    public void setReferencePoint(Position referencePoint);

    void setCourseDesignSpecificMarks(Set<PositionedMark> courseDesignSpecificMarks);

    Set<PositionedMark> getCourseDesignSpecificMarks();

    String getCourseDesignDescription();

    void setCourseDesignDescription(String courseDesignDescription);
}
