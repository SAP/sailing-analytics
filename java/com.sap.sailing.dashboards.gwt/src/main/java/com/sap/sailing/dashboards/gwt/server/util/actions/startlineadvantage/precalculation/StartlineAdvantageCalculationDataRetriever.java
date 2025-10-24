package com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage.precalculation;

import java.util.Iterator;

import com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage.DefaultPolarValues;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.BearingWithConfidence;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * @author Alexander Ries (D062114)
 *
 */
public class StartlineAdvantageCalculationDataRetriever {

    private PolarDataService polarDataService;
    private final DomainFactory domainFactory;
    
    public StartlineAdvantageCalculationDataRetriever(DomainFactory domainFactory, PolarDataService polarDataService) {
        super();
        this.polarDataService = polarDataService;
        this.domainFactory = domainFactory;
    }

    public StartlineAdvantageCalculationData retrieveDataForTrackedRace(TrackedRace trackedRace) {
        StartlineAdvantageCalculationData result = null;
        Position startBoatPosition = null;
        Position pinEndPosition = null;
        Position firstMarkPosition = null;
        Double startlineAdvantageAtPinEndInMeters = null;
        Double startlineLenghtInMeters = null;
        Wind wind = null;
        Double maneuverAngle = null;
        Pair<Position/* StartBoatPosition */, Position/* PinEndPosition */> startlineMarkPositions = retrieveStartlineMarksPositions(trackedRace);
        startBoatPosition = startlineMarkPositions.getA();
        pinEndPosition = startlineMarkPositions.getB();
        firstMarkPosition = retrieveFirstMarkPosition(trackedRace);
        startlineAdvantageAtPinEndInMeters = retrieveStartlineAdvantage(trackedRace);
        startlineLenghtInMeters = retrieveStartlineLenght(trackedRace);
        wind = retrieveWindAtPosition(startBoatPosition, trackedRace);
        maneuverAngle = retrieveManouvreAngleAtWindSpeedAndBoatClass(domainFactory.getOrCreateBoatClass("Extreme 40"), ManeuverType.TACK, wind);
        result = new StartlineAdvantageCalculationData(startBoatPosition, pinEndPosition, firstMarkPosition, startlineAdvantageAtPinEndInMeters, startlineLenghtInMeters, wind, maneuverAngle);
        return result;
    }
    
    private Position retrieveFirstMarkPosition(TrackedRace trackedRace) {
        Position result = null;
        Course course = trackedRace.getRace().getCourse();
        if (course != null) {
            Waypoint firstmarkWayPoint = course.getFirstLeg().getTo();
            if (firstmarkWayPoint != null) {
                result = retrieveFirstMarkPositionFromFirstMarkWayPoint(firstmarkWayPoint, trackedRace);
            }
        }
        return result;
    }
    
    private Pair<Position/* StartBoatPosition */, Position/* PinEndPosition */> retrieveStartlineMarksPositions(TrackedRace trackedRace) {
        Pair<Position, Position> result = new Pair<Position, Position>(null, null);
        Course course = trackedRace.getRace().getCourse();
        if (course != null) {
            Waypoint startlineWayPoint = course.getFirstLeg().getFrom();
            Waypoint firstmarkWayPoint = course.getFirstLeg().getTo();
            if (startlineWayPoint != null && firstmarkWayPoint != null) {
                result = retrieveStartlineMarkPositionsFromStartLineWayPoint(startlineWayPoint, trackedRace);
            }
        }
        return result;
    }

    private Pair<Position/*StartBoatPosition*/, Position/*PinEndPosition*/> retrieveStartlineMarkPositionsFromStartLineWayPoint(Waypoint startLineWayPoint, TrackedRace trackedRace) {
        Pair<Position, Position> result = null;
        Iterator<Mark> markIterator = startLineWayPoint.getMarks().iterator();
        if (markIterator.hasNext()) {
            Mark startboat = (Mark) markIterator.next();
            if (markIterator.hasNext()) {
                Mark pinEnd = (Mark) markIterator.next();
                TimePoint now = MillisecondsTimePoint.now();
                Position startBoatPosition = getPositionFromMarkAtTimePoint(trackedRace, startboat, now);
                Position pinEndPosition = getPositionFromMarkAtTimePoint(trackedRace, pinEnd, now);
                result = new Pair<Position, Position>(startBoatPosition, pinEndPosition);
            }
        }
        return result;
    }

    private Position retrieveFirstMarkPositionFromFirstMarkWayPoint(Waypoint firstMarkWayPoint, TrackedRace trackedRace) {
        Position result = null;
        if (firstMarkWayPoint.getMarks().iterator().hasNext()) {
            Mark firstMark = firstMarkWayPoint.getMarks().iterator().next();
            TimePoint now = MillisecondsTimePoint.now();
            result = getPositionFromMarkAtTimePoint(trackedRace, firstMark, now);
        }
        return result;
    }

    private Position getPositionFromMarkAtTimePoint(TrackedRace trackedRace, Mark mark, TimePoint timePoint) {
        GPSFixTrack<Mark, GPSFix> fixTrack = trackedRace.getTrack(mark);
        return fixTrack.getEstimatedPosition(timePoint, true);
    }
    
    private Double retrieveStartlineAdvantage(TrackedRace trackedRace) {
        Double result = null;
        LineDetails startline = trackedRace.getStartLine(MillisecondsTimePoint.now());
        if (startline != null && startline.getAdvantage() != null) {
            result = Double.valueOf(startline.getAdvantage().getMeters());
        }
        return result;
    }

    private Double retrieveStartlineLenght(TrackedRace trackedRace) {
        Double result = null;
        LineDetails startline = trackedRace.getStartLine(MillisecondsTimePoint.now());
        result = Double.valueOf(startline.getLength().getMeters());
        return result;
    }
    
    private Wind retrieveWindAtPosition(Position position, TrackedRace trackedRace) {
        Wind result = null;
        result = trackedRace.getWind(position, MillisecondsTimePoint.now());
        return result;
    }
    
    private Double retrieveManouvreAngleAtWindSpeedAndBoatClass(BoatClass boatClass, ManeuverType maneuverType, Speed windSpeed) {
        Double result = DefaultPolarValues.getManouvreAngle(ManeuverType.TACK);
        try {
            if (boatClass != null && maneuverType != null && windSpeed != null) {
                BearingWithConfidence<Void> bearingWithConfidence =  polarDataService.getManeuverAngle(boatClass, maneuverType, windSpeed);
                result = Double.valueOf(bearingWithConfidence.getObject().getDegrees());
            }
        } catch (NotEnoughDataHasBeenAddedException | NullPointerException e) {
            e.printStackTrace();
        }
        return result;
    }
}
