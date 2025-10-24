package com.sap.sailing.domain.tractracadapter.impl;

import java.io.Serializable;
import java.net.URI;
import java.util.Iterator;

import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.tracking.impl.CourseDesignUpdateHandler;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sse.common.Util;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.map.IMapItem;

public class TracTracCourseDesignUpdateHandler extends CourseDesignUpdateHandler {
    private final IRace tractracRace;
    private final DomainFactory domainFactory;
    
    public TracTracCourseDesignUpdateHandler(URI updateURI, String tracTracUsername, String tracTracPassword, Serializable tracTracEventId, Serializable raceId, IRace tractracRace, DomainFactory domainFactory) {
        super(updateURI, tracTracUsername, tracTracPassword, tracTracEventId, raceId);
        this.domainFactory = domainFactory;
        this.tractracRace = tractracRace;
    }

    @Override
    protected CourseBase replaceControlPointsByMatchingExistingControlPoints(CourseBase courseDesign) {
        final Iterable<IMapItem> candidates = domainFactory.getControlsForCourseArea(tractracRace.getEvent(), tractracRace.getCourseArea());
        final CourseBase result = new CourseDataImpl(courseDesign.getName());
        int zeroBasedPosition = 0;
        boolean changed = false;
        for (final Waypoint waypoint : courseDesign.getWaypoints()) {
            if (Util.size(waypoint.getMarks()) > 1) {
                final Iterator<Mark> markIter = waypoint.getMarks().iterator();
                final Mark first = markIter.next();
                final Mark second = markIter.next();
                final ControlPoint existingControlPoint = domainFactory.getExistingControlWithTwoMarks(candidates,
                        first, second);
                if (existingControlPoint == null) {
                    result.addWaypoint(zeroBasedPosition++, waypoint);
                } else {
                    result.addWaypoint(zeroBasedPosition++, domainFactory.getBaseDomainFactory()
                            .createWaypoint(existingControlPoint, waypoint.getPassingInstructions()));
                    changed = true;
                }
            } else {
                result.addWaypoint(zeroBasedPosition++, waypoint);
            }
        }
        return changed ? result : courseDesign;
    }
}
