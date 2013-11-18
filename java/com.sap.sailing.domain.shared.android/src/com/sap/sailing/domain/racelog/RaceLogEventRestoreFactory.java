package com.sap.sailing.domain.racelog;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.racelog.impl.RaceLogEventRestoreFactoryImpl;
import com.sap.sailing.domain.tracking.Wind;

public interface RaceLogEventRestoreFactory extends RaceLogEventFactory {
    RaceLogEventRestoreFactory INSTANCE = new RaceLogEventRestoreFactoryImpl();

    RaceLogFlagEvent createFlagEvent(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable id, List<Competitor> involvedBoats, int passId, Flags upperFlag, Flags lowerFlag, boolean isDisplayed);

    RaceLogStartTimeEvent createStartTimeEvent(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable id, List<Competitor> involvedBoats, int passId, TimePoint startTime);

    RaceLogRaceStatusEvent createRaceStatusEvent(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable id, List<Competitor> competitors, int passId, RaceLogRaceStatus nextStatus);

    RaceLogPassChangeEvent createPassChangeEvent(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable id, List<Competitor> competitors, int passId);

    RaceLogCourseAreaChangedEvent createCourseAreaChangedEvent(TimePoint createdAt, RaceLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, List<Competitor> competitors, int passId, Serializable courseAreaId);

    RaceLogCourseDesignChangedEvent createCourseDesignChangedEvent(TimePoint createdAt, RaceLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, List<Competitor> competitors, int passId, CourseBase courseData);

    RaceLogFinishPositioningListChangedEvent createFinishPositioningListChangedEvent(TimePoint createdAt,
            RaceLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, List<Competitor> competitors,
            int passId, CompetitorResults positionedCompetitors);

    RaceLogFinishPositioningConfirmedEvent createFinishPositioningConfirmedEvent(TimePoint createdAt,
            RaceLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, List<Competitor> competitors, 
            int passId, CompetitorResults positionedCompetitors);

    RaceLogPathfinderEvent createPathfinderEvent(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable id, List<Competitor> competitors, int passId, String pathfinderId);

    RaceLogGateLineOpeningTimeEvent createGateLineOpeningTimeEvent(TimePoint createdAt, RaceLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, List<Competitor> competitors, int passId, Long gateLineOpeningTime);

    RaceLogStartProcedureChangedEvent createStartProcedureChangedEvent(TimePoint createdAt, RaceLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, List<Competitor> competitors, int passId, RacingProcedureType type);
    
    RaceLogProtestStartTimeEvent createProtestStartTimeEvent(TimePoint createdAt, RaceLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, List<Competitor> competitors, int passId, TimePoint protestStartTime);

    RaceLogWindFixEvent createWindFixEvent(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable id, List<Competitor> competitors, Integer passId, Wind wind);
}
