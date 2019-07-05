package com.sap.sailing.domain.orc.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.orc.ORCPerformanceCurveLeg;
import com.sap.sse.common.Distance;

public class ORCPerformanceCurveCourseImpl implements ORCPerformanceCurveCourse {
    private final List<ORCPerformanceCurveLeg> legs;
    private final Distance totalLength;

    public ORCPerformanceCurveCourseImpl(List<ORCPerformanceCurveLeg> legs) {
        this.legs = Collections.unmodifiableList(legs);
        Distance myTotalLength = Distance.NULL;
        for (ORCPerformanceCurveLeg leg : legs) {
            myTotalLength = myTotalLength.add(leg.getLength());
        }
        totalLength = myTotalLength;
    }

    public ORCPerformanceCurveLeg getLeg(int i) {
        return legs.get(i);
    }

    @Override
    public Distance getTotalLength() {
        return totalLength;
        
    }

    @Override
    public List<ORCPerformanceCurveLeg> getLegs() {
        return legs;
    }

    @Override
    public ORCPerformanceCurveCourse subcourse(int lastFinishedLegOneBased, double perCentOfCurrentLeg) {
        // does function for empty courses, returns again empty course
        if (lastFinishedLegOneBased >= legs.size()) {
            return this;
        } else {
            List<ORCPerformanceCurveLeg> resultLegs = new ArrayList<>();
            resultLegs.addAll(legs.subList(0, lastFinishedLegOneBased));
            ORCPerformanceCurveLeg lastFinishedLeg = legs.get(lastFinishedLegOneBased);
            resultLegs.add(new ORCPerformanceCurveLegImpl(lastFinishedLeg.getLength().scale(perCentOfCurrentLeg), lastFinishedLeg.getTwa()));
            return new ORCPerformanceCurveCourseImpl(resultLegs);
        }
    }

    @Override
    public String toString() {
        return "[legs=" + legs + ", totalLength=" + totalLength + "]";
    }
}
