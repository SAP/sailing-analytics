package com.sap.sailing.domain.racelog;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.racelog.impl.RaceLogEventFactoryImpl;
import com.sap.sailing.domain.racelog.tracking.CreateRaceEvent;
import com.sap.sailing.domain.racelog.tracking.DenoteForTrackingEvent;
import com.sap.sailing.domain.racelog.tracking.DeviceCompetitorMappingEvent;
import com.sap.sailing.domain.racelog.tracking.DeviceIdentifier;
import com.sap.sailing.domain.racelog.tracking.DeviceMarkMappingEvent;
import com.sap.sailing.domain.racelog.tracking.RevokeEvent;
import com.sap.sailing.domain.tracking.Wind;

public interface RaceLogEventFactory {
    RaceLogEventFactory INSTANCE = new RaceLogEventFactoryImpl();

    RaceLogFlagEvent createFlagEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, Serializable id, List<Competitor> involvedBoats,
            int passId, Flags upperFlag, Flags lowerFlag, boolean isDisplayed);

    RaceLogFlagEvent createFlagEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId, Flags upperFlag,
            Flags lowerFlag, boolean isDisplayed);

    RaceLogStartTimeEvent createStartTimeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, Serializable id,
            List<Competitor> involvedBoats, int passId, TimePoint startTime);

    RaceLogStartTimeEvent createStartTimeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId, TimePoint startTime);

    RaceLogRaceStatusEvent createRaceStatusEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, Serializable id,
            List<Competitor> competitors, int passId, RaceLogRaceStatus nextStatus);

    RaceLogRaceStatusEvent createRaceStatusEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId, RaceLogRaceStatus nextStatus);

    RaceLogPassChangeEvent createPassChangeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, Serializable id,
            List<Competitor> competitors, int passId);

    RaceLogPassChangeEvent createPassChangeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId);

    RaceLogCourseAreaChangedEvent createCourseAreaChangedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            Serializable id, List<Competitor> competitors, int passId, Serializable courseAreaId);

    RaceLogCourseAreaChangedEvent createCourseAreaChangedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            int passId, Serializable courseAreaId);

    RaceLogCourseDesignChangedEvent createCourseDesignChangedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            Serializable id, List<Competitor> competitors, int passId, CourseBase courseData);

    RaceLogCourseDesignChangedEvent createCourseDesignChangedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            int passId, CourseBase courseData);

    RaceLogFinishPositioningListChangedEvent createFinishPositioningListChangedEvent(TimePoint logicalTimePoint,
            RaceLogEventAuthor author, Serializable id, List<Competitor> competitors,
            int passId, CompetitorResults positionedCompetitors);

    RaceLogFinishPositioningListChangedEvent createFinishPositioningListChangedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            int passId, CompetitorResults positionedCompetitors);

    RaceLogFinishPositioningConfirmedEvent createFinishPositioningConfirmedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            Serializable id, List<Competitor> competitors, int passId, CompetitorResults positionedCompetitors);

    RaceLogFinishPositioningConfirmedEvent createFinishPositioningConfirmedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, 
            int passId, CompetitorResults positionedCompetitors);

    RaceLogPathfinderEvent createPathfinderEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, Serializable id,
            List<Competitor> competitors, int passId, String pathfinderId);

    RaceLogPathfinderEvent createPathfinderEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId, String pathfinderId);

    RaceLogGateLineOpeningTimeEvent createGateLineOpeningTimeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            Serializable id, List<Competitor> competitors, int passId, Long gateLaunchStopTime, Long golfDownTime);

    RaceLogGateLineOpeningTimeEvent createGateLineOpeningTimeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            int passId, Long gateLaunchStopTime, Long golfDownTime);

    RaceLogStartProcedureChangedEvent createStartProcedureChangedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            Serializable id, List<Competitor> competitors, int passId, RacingProcedureType type);

    RaceLogStartProcedureChangedEvent createStartProcedureChangedEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            int passId, RacingProcedureType type);

    RaceLogProtestStartTimeEvent createProtestStartTimeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            Serializable id, List<Competitor> competitors, int passId, TimePoint protestStartTime);

    RaceLogProtestStartTimeEvent createProtestStartTimeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId, TimePoint protestStartTime);

    RaceLogWindFixEvent createWindFixEvent(TimePoint eventTime, RaceLogEventAuthor author, int currentPassId, Wind wind);
    
    RaceLogWindFixEvent createWindFixEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            Serializable id, List<Competitor> competitors, int passId, Wind wind);

    DeviceCompetitorMappingEvent createDeviceCompetitorMappingEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            DeviceIdentifier device, Competitor mappedTo, int passId, TimePoint from, TimePoint to);

    DeviceMarkMappingEvent createDeviceMarkMappingEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author,
            DeviceIdentifier device, Mark mappedTo, int passId, TimePoint from, TimePoint to);

    DenoteForTrackingEvent createDenoteForTrackingEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId,
    		String raceName, BoatClass boatClass);

    CreateRaceEvent createCreateRaceEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId);
    
    RevokeEvent createRevokeEvent(TimePoint logicalTimePoint, RaceLogEventAuthor author, int passId, Serializable revokedEventId);

}
