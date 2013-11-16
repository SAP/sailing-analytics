package com.sap.sailing.domain.persistence.impl;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Gate;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationMatcher;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationMatcherMulti;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationMatcherSingle;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.racelog.RaceLogCourseAreaChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.domain.racelog.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogPassChangeEvent;
import com.sap.sailing.domain.racelog.RaceLogPathfinderEvent;
import com.sap.sailing.domain.racelog.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.racelog.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogStartTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogWindFixEvent;
import com.sap.sailing.domain.tracking.Positioned;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DeviceConfigurationJsonSerializer;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private static Logger logger = Logger.getLogger(MongoObjectFactoryImpl.class.getName());
    private final DB database;
    private final CompetitorJsonSerializer competitorSerializer = CompetitorJsonSerializer.create();

    public MongoObjectFactoryImpl(DB database) {
        super();
        this.database = database;
    }
    
    @Override
    public DB getDatabase() {
        return database;
    }

    public DBObject storeWind(Wind wind) {
        DBObject result = new BasicDBObject();
        storePositioned(wind, result);
        storeTimed(wind, result);
        storeSpeedWithBearing(wind, result);
        return result;
    }
    
    private void storeTimePoint(TimePoint timePoint, DBObject result, FieldNames field) {
        result.put(field.name(), timePoint.asMillis());
    }

    private void storeTimed(Timed timed, DBObject result) {
        storeTimePoint(timed.getTimePoint(), result, FieldNames.TIME_AS_MILLIS);
    }

    private void storeSpeedWithBearing(SpeedWithBearing speedWithBearing, DBObject result) {
        storeSpeed(speedWithBearing, result);
        storeBearing(speedWithBearing.getBearing(), result);

    }

    private void storeBearing(Bearing bearing, DBObject result) {
        result.put(FieldNames.DEGREE_BEARING.name(), bearing.getDegrees());
    }

    private void storeSpeed(Speed speed, DBObject result) {
        result.put(FieldNames.KNOT_SPEED.name(), speed.getKnots());
    }

    private void storePositioned(Positioned positioned, DBObject result) {
        if (positioned.getPosition() != null) {
            result.put(FieldNames.LAT_DEG.name(), positioned.getPosition().getLatDeg());
            result.put(FieldNames.LNG_DEG.name(), positioned.getPosition().getLngDeg());
        }
    }

    @Override
    public void addWindTrackDumper(TrackedRegatta trackedRegatta, TrackedRace trackedRace, WindSource windSource) {
        WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
        windTrack.addListener(new MongoWindListener(trackedRace, trackedRegatta.getRegatta().getName(), windSource, this, database));
    }

    public DBCollection getWindTrackCollection() {
        DBCollection result = database.getCollection(CollectionNames.WIND_TRACKS.name());
        result.ensureIndex(new BasicDBObject(FieldNames.REGATTA_NAME.name(), null));
        return result;
    }

    /**
     * @param regattaName
     *            the regatta name is stored only for human readability purposes because a time stamp may be a bit unhandy for
     *            identifying where the wind fix was collected
     */
    public DBObject storeWindTrackEntry(RaceDefinition race, String regattaName, WindSource windSource, Wind wind) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_ID.name(), race.getId());
        result.put(FieldNames.REGATTA_NAME.name(), regattaName);
        result.put(FieldNames.WIND_SOURCE_NAME.name(), windSource.name());
        if (windSource.getId() != null) {
            result.put(FieldNames.WIND_SOURCE_ID.name(), windSource.getId());
        }
        result.put(FieldNames.WIND.name(), storeWind(wind));
        return result;
    }

    private void storeRaceIdentifiers(RaceColumn raceColumn, DBObject dbObject) {
        BasicDBObject raceIdentifiersPerFleet = new BasicDBObject();
        for (Fleet fleet : raceColumn.getFleets()) {
            RaceIdentifier raceIdentifier = raceColumn.getRaceIdentifier(fleet);
            if (raceIdentifier != null) {
                DBObject raceIdentifierForFleet = new BasicDBObject();
                storeRaceIdentifier(raceIdentifierForFleet, raceIdentifier);
                raceIdentifiersPerFleet.put(MongoUtils.escapeDollarAndDot(fleet.getName()), raceIdentifierForFleet);
            }
        }
        dbObject.put(FieldNames.RACE_IDENTIFIERS.name(), raceIdentifiersPerFleet);
    }

    private void storeRaceIdentifier(DBObject dbObject, RaceIdentifier raceIdentifier) {
        if (raceIdentifier != null) {
            dbObject.put(FieldNames.EVENT_NAME.name(), raceIdentifier.getRegattaName());
            dbObject.put(FieldNames.RACE_NAME.name(), raceIdentifier.getRaceName());
        }
    }

    @Override
    public void storeLeaderboard(Leaderboard leaderboard) {
        DBCollection leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());
        try {
            leaderboardCollection.ensureIndex(FieldNames.LEADERBOARD_NAME.name());
        } catch (NullPointerException npe) {
            // sometimes, for reasons yet to be clarified, ensuring an index on the name field causes an NPE
            logger.log(Level.SEVERE, "storeLeaderboard", npe);
        }
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
        BasicDBObject dbLeaderboard = new BasicDBObject();
        dbLeaderboard.put(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
        if (leaderboard.getDisplayName() != null) {
            dbLeaderboard.put(FieldNames.LEADERBOARD_DISPLAY_NAME.name(), leaderboard.getDisplayName());
        }
        BasicDBList dbSuppressedCompetitorIds = new BasicDBList();
        for (Competitor suppressedCompetitor : leaderboard.getSuppressedCompetitors()) {
            dbSuppressedCompetitorIds.add(suppressedCompetitor.getId());
        }
        dbLeaderboard.put(FieldNames.LEADERBOARD_SUPPRESSED_COMPETITOR_IDS.name(), dbSuppressedCompetitorIds);
        if (leaderboard instanceof FlexibleLeaderboard) {
            storeFlexibleLeaderboard((FlexibleLeaderboard) leaderboard, dbLeaderboard);
        } else if (leaderboard instanceof RegattaLeaderboard) {
            storeRegattaLeaderboard((RegattaLeaderboard) leaderboard, dbLeaderboard);
        } else {
            // at least store the scoring scheme
            dbLeaderboard.put(FieldNames.SCORING_SCHEME_TYPE.name(), leaderboard.getScoringScheme().getType().name());
        }
        if (leaderboard.getDefaultCourseArea() != null) {
            dbLeaderboard.put(FieldNames.COURSE_AREA_ID.name(), leaderboard.getDefaultCourseArea().getId().toString());
        } else {
            dbLeaderboard.put(FieldNames.COURSE_AREA_ID.name(), null);
        }
        storeColumnFactors(leaderboard, dbLeaderboard);
        storeLeaderboardCorrectionsAndDiscards(leaderboard, dbLeaderboard);
        leaderboardCollection.update(query, dbLeaderboard, /* upsrt */ true, /* multi */ false);
    }

    private void storeColumnFactors(Leaderboard leaderboard, BasicDBObject dbLeaderboard) {
        DBObject raceColumnFactors = new BasicDBObject();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            Double explicitFactor = raceColumn.getExplicitFactor();
            if (explicitFactor != null) {
                raceColumnFactors.put(MongoUtils.escapeDollarAndDot(raceColumn.getName()), explicitFactor);
            }
        }
        dbLeaderboard.put(FieldNames.LEADERBOARD_COLUMN_FACTORS.name(), raceColumnFactors);
    }

    private void storeRegattaLeaderboard(RegattaLeaderboard leaderboard, DBObject dbLeaderboard) {
        dbLeaderboard.put(FieldNames.REGATTA_NAME.name(), leaderboard.getRegatta().getName());
    }

    private void storeFlexibleLeaderboard(FlexibleLeaderboard leaderboard, BasicDBObject dbLeaderboard) {
        BasicDBList dbRaceColumns = new BasicDBList();
        dbLeaderboard.put(FieldNames.SCORING_SCHEME_TYPE.name(), leaderboard.getScoringScheme().getType().name());
        dbLeaderboard.put(FieldNames.LEADERBOARD_COLUMNS.name(), dbRaceColumns);
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            BasicDBObject dbRaceColumn = storeRaceColumn(raceColumn);
            dbRaceColumns.add(dbRaceColumn);
        }
    }

    private void storeLeaderboardCorrectionsAndDiscards(Leaderboard leaderboard, BasicDBObject dbLeaderboard) {
        if (leaderboard.hasCarriedPoints()) {
            BasicDBList dbCarriedPoints = new BasicDBList();
            dbLeaderboard.put(FieldNames.LEADERBOARD_CARRIED_POINTS_BY_ID.name(), dbCarriedPoints);
            for (Competitor competitor : leaderboard.getCompetitors()) {
                if (leaderboard.hasCarriedPoints(competitor)) {
                    DBObject dbCarriedPointsForCompetitor = new BasicDBObject();
                    dbCarriedPointsForCompetitor.put(FieldNames.COMPETITOR_ID.name(), competitor.getId());
                    dbCarriedPointsForCompetitor.put(FieldNames.LEADERBOARD_CARRIED_POINTS.name(), leaderboard.getCarriedPoints(competitor));
                    dbCarriedPoints.add(dbCarriedPointsForCompetitor);
                }
            }
        }
        BasicDBObject dbScoreCorrections = new BasicDBObject();
        storeScoreCorrections(leaderboard, dbScoreCorrections);
        dbLeaderboard.put(FieldNames.LEADERBOARD_SCORE_CORRECTIONS.name(), dbScoreCorrections);
        final ResultDiscardingRule resultDiscardingRule = leaderboard.getResultDiscardingRule();
        storeResultDiscardingRule(dbLeaderboard, resultDiscardingRule, FieldNames.LEADERBOARD_DISCARDING_THRESHOLDS);
        BasicDBList competitorDisplayNames = new BasicDBList();
        for (Competitor competitor : leaderboard.getCompetitors()) {
            String displayNameForCompetitor = leaderboard.getDisplayName(competitor);
            if (displayNameForCompetitor != null) {
                DBObject dbDisplayName = new BasicDBObject();
                dbDisplayName.put(FieldNames.COMPETITOR_ID.name(), competitor.getId());
                dbDisplayName.put(FieldNames.COMPETITOR_DISPLAY_NAME.name(), displayNameForCompetitor);
                competitorDisplayNames.add(dbDisplayName);
            }
        }
        dbLeaderboard.put(FieldNames.LEADERBOARD_COMPETITOR_DISPLAY_NAMES.name(), competitorDisplayNames);
    }

    /**
     * Stores the result discarding rule to <code>dbObject</code>'s field identified by <code>field</code> if the result discarding
     * rule is not <code>null</code> and is of type {@link ThresholdBasedResultDiscardingRule}. Otherwise, it is assumed that the
     * result discarding rule is otherwise implicitly obtained, e.g., from a definition of a regatta with its series, stored elsewhere.
     */
    private void storeResultDiscardingRule(DBObject dbObject,
            final ResultDiscardingRule resultDiscardingRule, FieldNames field) {
        if (resultDiscardingRule != null && resultDiscardingRule instanceof ThresholdBasedResultDiscardingRule) {
            BasicDBList dbResultDiscardingThresholds = new BasicDBList();
            for (int threshold : ((ThresholdBasedResultDiscardingRule) resultDiscardingRule).getDiscardIndexResultsStartingWithHowManyRaces()) {
                dbResultDiscardingThresholds.add(threshold);
            }
            dbObject.put(field.name(), dbResultDiscardingThresholds);
        }
    }

    private BasicDBObject storeRaceColumn(RaceColumn raceColumn) {
        BasicDBObject dbRaceColumn = new BasicDBObject();
        dbRaceColumn.put(FieldNames.LEADERBOARD_COLUMN_NAME.name(), raceColumn.getName());
        dbRaceColumn.put(FieldNames.LEADERBOARD_IS_MEDAL_RACE_COLUMN.name(), raceColumn.isMedalRace());
        storeRaceIdentifiers(raceColumn, dbRaceColumn);
        return dbRaceColumn;
    }

    private void storeScoreCorrections(Leaderboard leaderboard, BasicDBObject dbScoreCorrections) {
        TimePoint now = MillisecondsTimePoint.now();
        SettableScoreCorrection scoreCorrection = leaderboard.getScoreCorrection();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            BasicDBList dbCorrectionForRace = new BasicDBList();
            for (Competitor competitor : leaderboard.getCompetitors()) {
                // TODO bug 655: make score corrections time dependent
                if (scoreCorrection.isScoreCorrected(competitor, raceColumn, now)) {
                    BasicDBObject dbCorrectionForCompetitor = new BasicDBObject();
                    dbCorrectionForCompetitor.put(FieldNames.COMPETITOR_ID.name(), competitor.getId());
                    MaxPointsReason maxPointsReason = scoreCorrection.getMaxPointsReason(competitor, raceColumn, now);
                    if (maxPointsReason != MaxPointsReason.NONE) {
                        dbCorrectionForCompetitor.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name(),
                                maxPointsReason.name());
                    }
                    Double explicitScoreCorrection = scoreCorrection
                            .getExplicitScoreCorrection(competitor, raceColumn);
                    if (explicitScoreCorrection != null) {
                        dbCorrectionForCompetitor.put(FieldNames.LEADERBOARD_CORRECTED_SCORE.name(),
                                explicitScoreCorrection);
                    }
                    dbCorrectionForRace.add(dbCorrectionForCompetitor);
                }
            }
            if (!dbCorrectionForRace.isEmpty()) {
                // using the column name as the key for the score corrections requires re-writing the score corrections
                // of a meta-leaderboard if the name of one of its leaderboards changes
                dbScoreCorrections.put(MongoUtils.escapeDollarAndDot(raceColumn.getName()), dbCorrectionForRace);
            }
        }
        final TimePoint timePointOfLastCorrectionsValidity = scoreCorrection.getTimePointOfLastCorrectionsValidity();
        if (timePointOfLastCorrectionsValidity != null) {
            dbScoreCorrections.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_TIMESTAMP.name(), timePointOfLastCorrectionsValidity.asMillis());
        }
        if (scoreCorrection.getComment() != null) {
            dbScoreCorrections.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_COMMENT.name(), scoreCorrection.getComment());
        }
    }

    @Override
    public void removeLeaderboard(String leaderboardName) {
        DBCollection leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_NAME.name(), leaderboardName);
        leaderboardCollection.remove(query);
    }

    @Override
    public void renameLeaderboard(String oldName, String newName) {
        DBCollection leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_NAME.name(), oldName);
        BasicDBObject renameUpdate = new BasicDBObject("$set", new BasicDBObject(FieldNames.LEADERBOARD_NAME.name(), newName));
        leaderboardCollection.update(query, renameUpdate);
    }

    @Override
    public void storeLeaderboardGroup(LeaderboardGroup leaderboardGroup) {
        DBCollection leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        DBCollection leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());

        try {
            leaderboardGroupCollection.ensureIndex(FieldNames.LEADERBOARD_GROUP_NAME.name());
        } catch (NullPointerException npe) {
            // sometimes, for reasons yet to be clarified, ensuring an index on the name field causes an NPE
            logger.log(Level.SEVERE, "storeLeaderboardGroup", npe);
        }
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_GROUP_NAME.name(), leaderboardGroup.getName());
        BasicDBObject dbLeaderboardGroup = new BasicDBObject();
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_NAME.name(), leaderboardGroup.getName());
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_DESCRIPTION.name(), leaderboardGroup.getDescription());
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_DISPLAY_IN_REVERSE_ORDER.name(), leaderboardGroup.isDisplayGroupsInReverseOrder());
        final Leaderboard overallLeaderboard = leaderboardGroup.getOverallLeaderboard();
        if (overallLeaderboard != null) {
            BasicDBObject overallLeaderboardQuery = new BasicDBObject(FieldNames.LEADERBOARD_NAME.name(), overallLeaderboard.getName());
            DBObject dbOverallLeaderboard = leaderboardCollection.findOne(overallLeaderboardQuery);
            if (dbOverallLeaderboard == null) {
                storeLeaderboard(overallLeaderboard);
                dbOverallLeaderboard = leaderboardCollection.findOne(overallLeaderboardQuery);
            }
            ObjectId dbOverallLeaderboardId = (ObjectId) dbOverallLeaderboard.get("_id");
            dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_OVERALL_LEADERBOARD.name(), dbOverallLeaderboardId);
        }
        BasicDBList dbLeaderboardIds = new BasicDBList();
        for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
            BasicDBObject leaderboardQuery = new BasicDBObject(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
            DBObject dbLeaderboard = leaderboardCollection.findOne(leaderboardQuery);
            if (dbLeaderboard == null) {
                storeLeaderboard(leaderboard);
                dbLeaderboard = leaderboardCollection.findOne(leaderboardQuery);
            }
            ObjectId dbLeaderboardId = (ObjectId) dbLeaderboard.get("_id");
            dbLeaderboardIds.add(dbLeaderboardId);
        }
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_LEADERBOARDS.name(), dbLeaderboardIds);
        leaderboardGroupCollection.update(query, dbLeaderboardGroup, true, false);
    }

    @Override
    public void removeLeaderboardGroup(String groupName) {
        DBCollection leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_GROUP_NAME.name(), groupName);
        leaderboardGroupCollection.remove(query);
    }

    @Override
    public void renameLeaderboardGroup(String oldName, String newName) {
        DBCollection leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_GROUP_NAME.name(), oldName);
        BasicDBObject update = new BasicDBObject("$set", new BasicDBObject(FieldNames.LEADERBOARD_GROUP_NAME.name(), newName));
        leaderboardGroupCollection.update(query, update);
    }

    @Override
    public void storeEvent(Event event) {
        DBCollection eventCollection = database.getCollection(CollectionNames.EVENTS.name());
        eventCollection.ensureIndex(FieldNames.EVENT_ID.name());
        DBObject query = new BasicDBObject();
        query.put(FieldNames.EVENT_ID.name(), event.getId());
        DBObject eventDBObject = new BasicDBObject();
        eventDBObject.put(FieldNames.EVENT_NAME.name(), event.getName());
        eventDBObject.put(FieldNames.EVENT_ID.name(), event.getId());
        eventDBObject.put(FieldNames.EVENT_PUBLICATION_URL.name(), event.getPublicationUrl());
        eventDBObject.put(FieldNames.EVENT_IS_PUBLIC.name(), event.isPublic());
        DBObject venueDBObject = getVenueAsDBObject(event.getVenue());
        eventDBObject.put(FieldNames.VENUE.name(), venueDBObject);
        eventCollection.update(query, eventDBObject, /* upsrt */ true, /* multi */ false);
    }

    @Override
    public void renameEvent(Serializable id, String newName) {
        DBCollection eventCollection = database.getCollection(CollectionNames.EVENTS.name());
        BasicDBObject query = new BasicDBObject(FieldNames.EVENT_ID.name(), id);
        BasicDBObject renameUpdate = new BasicDBObject("$set", new BasicDBObject(FieldNames.EVENT_NAME.name(), newName));
        eventCollection.update(query, renameUpdate);
    }

    @Override
    public void removeEvent(Serializable id) {
        DBCollection eventsCollection = database.getCollection(CollectionNames.EVENTS.name());
        BasicDBObject query = new BasicDBObject(FieldNames.EVENT_ID.name(), id);
        eventsCollection.remove(query);
    }

    private DBObject getVenueAsDBObject(Venue venue) {
        DBObject result = new BasicDBObject();
        result.put(FieldNames.VENUE_NAME.name(), venue.getName());
        BasicDBList courseAreaList = new BasicDBList();
        result.put(FieldNames.COURSE_AREAS.name(), courseAreaList);
        for (CourseArea courseArea : venue.getCourseAreas()) {
            DBObject dbCourseArea = new BasicDBObject();
            courseAreaList.add(dbCourseArea);
            dbCourseArea.put(FieldNames.COURSE_AREA_NAME.name(), courseArea.getName());
            dbCourseArea.put(FieldNames.COURSE_AREA_ID.name(), courseArea.getId());
        }
        return result;
    }

    @Override
    public void storeRegatta(Regatta regatta) {
        DBCollection regattasCollection = database.getCollection(CollectionNames.REGATTAS.name());
        regattasCollection.ensureIndex(FieldNames.REGATTA_NAME.name());
        regattasCollection.ensureIndex(FieldNames.REGATTA_ID.name());
        DBObject dbRegatta = new BasicDBObject();
        DBObject query = new BasicDBObject(FieldNames.REGATTA_NAME.name(), regatta.getName());
        dbRegatta.put(FieldNames.REGATTA_NAME.name(), regatta.getName());
        dbRegatta.put(FieldNames.REGATTA_BASE_NAME.name(), regatta.getBaseName());
        dbRegatta.put(FieldNames.REGATTA_ID.name(), regatta.getId());
        dbRegatta.put(FieldNames.SCORING_SCHEME_TYPE.name(), regatta.getScoringScheme().getType().name());
        if (regatta.getBoatClass() != null) {
            dbRegatta.put(FieldNames.BOAT_CLASS_NAME.name(), regatta.getBoatClass().getName());
            dbRegatta.put(FieldNames.BOAT_CLASS_TYPICALLY_STARTS_UPWIND.name(), regatta.getBoatClass().typicallyStartsUpwind());
        }
        dbRegatta.put(FieldNames.REGATTA_SERIES.name(), storeSeries(regatta.getSeries()));

        if (regatta.getDefaultCourseArea() != null) {
            dbRegatta.put(FieldNames.COURSE_AREA_ID.name(), regatta.getDefaultCourseArea().getId().toString());
        } else {
            dbRegatta.put(FieldNames.COURSE_AREA_ID.name(), null);
        }

        regattasCollection.update(query, dbRegatta, /* upsrt */ true, /* multi */ false);
    }

    @Override
    public void removeRegatta(Regatta regatta) {
        DBCollection regattasCollection = database.getCollection(CollectionNames.REGATTAS.name());
        DBObject query = new BasicDBObject(FieldNames.REGATTA_NAME.name(), regatta.getName());
        regattasCollection.remove(query);
    }

    private BasicDBList storeSeries(Iterable<? extends Series> series) {
        BasicDBList dbSeries = new BasicDBList();
        for (Series s : series) {
            dbSeries.add(storeSeries(s));
        }
        return dbSeries;
    }

    private DBObject storeSeries(Series s) {
        DBObject dbSeries = new BasicDBObject();
        dbSeries.put(FieldNames.SERIES_NAME.name(), s.getName());
        dbSeries.put(FieldNames.SERIES_IS_MEDAL.name(), s.isMedal());
        dbSeries.put(FieldNames.SERIES_STARTS_WITH_ZERO_SCORE.name(), s.isStartsWithZeroScore());
        dbSeries.put(FieldNames.SERIES_STARTS_WITH_NON_DISCARDABLE_CARRY_FORWARD.name(), s.isFirstColumnIsNonDiscardableCarryForward());
        BasicDBList dbFleets = new BasicDBList();
        for (Fleet fleet : s.getFleets()) {
            dbFleets.add(storeFleet(fleet));
        }
        dbSeries.put(FieldNames.SERIES_FLEETS.name(), dbFleets);
        BasicDBList dbRaceColumns = new BasicDBList();
        for (RaceColumn raceColumn : s.getRaceColumns()) {
            dbRaceColumns.add(storeRaceColumn(raceColumn));
        }
        dbSeries.put(FieldNames.SERIES_RACE_COLUMNS.name(), dbRaceColumns);
        if (s.getResultDiscardingRule() != null) {
            storeResultDiscardingRule(dbSeries, s.getResultDiscardingRule(), FieldNames.SERIES_DISCARDING_THRESHOLDS);
        }
        return dbSeries;
    }

    private DBObject storeFleet(Fleet fleet) {
        DBObject dbFleet = new BasicDBObject(FieldNames.FLEET_NAME.name(), fleet.getName());
        if (fleet instanceof FleetImpl) {
            dbFleet.put(FieldNames.FLEET_ORDERING.name(), ((FleetImpl) fleet).getOrdering());
            if(fleet.getColor() != null) {
                Triple<Integer, Integer, Integer> colorAsRGB = fleet.getColor().getAsRGB();
                // we save the color as a integer value representing the RGB values
                int colorAsInt = (256 * 256 * colorAsRGB.getC()) + colorAsRGB.getB() * 256 + colorAsRGB.getA(); 
                dbFleet.put(FieldNames.FLEET_COLOR.name(), colorAsInt);
            } else {
                dbFleet.put(FieldNames.FLEET_COLOR.name(), null);
            }
        }
        return dbFleet;
    }

    @Override
    public void storeRegattaForRaceID(String raceIDAsString, Regatta regatta) {
        DBCollection regattaForRaceIDCollection = database.getCollection(CollectionNames.REGATTA_FOR_RACE_ID.name());
        DBObject query = new BasicDBObject(FieldNames.RACE_ID_AS_STRING.name(), raceIDAsString);
        DBObject entry = new BasicDBObject(FieldNames.RACE_ID_AS_STRING.name(), raceIDAsString);
        entry.put(FieldNames.REGATTA_NAME.name(), regatta.getName());
        regattaForRaceIDCollection.update(query, entry, /* upsrt */ true, /* multi */ false);
    }

    @Override
    public void removeRegattaForRaceID(String raceIDAsString, Regatta regatta) {
        DBCollection regattaForRaceIDCollection = database.getCollection(CollectionNames.REGATTA_FOR_RACE_ID.name());
        DBObject query = new BasicDBObject(FieldNames.RACE_ID_AS_STRING.name(), raceIDAsString);
        regattaForRaceIDCollection.remove(query);
    }

    public DBCollection getRaceLogCollection() {
        DBCollection result = database.getCollection(CollectionNames.RACE_LOGS.name());
        result.ensureIndex(new BasicDBObject(FieldNames.RACE_LOG_IDENTIFIER.name(), null));
        return result;
    }
    
    private void storeRaceLogEventAuthor(DBObject dbObject, RaceLogEventAuthor author) {
        if (author != null) {
            dbObject.put(FieldNames.RACE_LOG_EVENT_AUTHOR_NAME.name(), author.getName());
            dbObject.put(FieldNames.RACE_LOG_EVENT_AUTHOR_PRIORITY.name(), author.getPriority());
        }
    }

    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogFlagEvent flagEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));

        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogFlagEvent(flagEvent));
        return result;
    }

    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogStartTimeEvent startTimeEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));

        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogStartTimeEvent(startTimeEvent));
        return result;
    }

    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogPassChangeEvent passChangeEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));

        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogPassChangeEvent(passChangeEvent));
        return result;
    }

    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogRaceStatusEvent raceStatusEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));

        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogRaceStatusEvent(raceStatusEvent));
        return result;
    }

    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogCourseAreaChangedEvent courseAreaChangedEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));       
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogCourseAreaChangedEvent(courseAreaChangedEvent));
        return result;
    }
    
    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogCourseDesignChangedEvent courseDesignChangedEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));       
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogCourseDesignChangedEvent(courseDesignChangedEvent));
        return result;
    }
    
    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogFinishPositioningListChangedEvent finishPositioningListChangedEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));       
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogFinishPositioningListChangedEvent(finishPositioningListChangedEvent));
        return result;
    }
    
    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogFinishPositioningConfirmedEvent finishPositioningConfirmedEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));       
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogFinishPositioningConfirmedEvent(finishPositioningConfirmedEvent));
        return result;
    }
    
    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogPathfinderEvent pathfinderEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));       
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogPathfinderEvent(pathfinderEvent));
        return result;
    }
    
    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogGateLineOpeningTimeEvent gateLineOpeningTimeEvent) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));       
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogGateLineOpeningTimeEvent(gateLineOpeningTimeEvent));
        return result;
    }

    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogStartProcedureChangedEvent event) {
        DBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogStartProcedureChangedEvent(event));
        return result;
    }

    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogProtestStartTimeEvent event) {
        DBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogProtestStartTimeEvent(event));
        return result;
    }
    
    public DBObject storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogWindFixEvent event) {
        DBObject result = new BasicDBObject();
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogWindFix(event));
        return result;
    }
    
    private Object storeRaceLogWindFix(RaceLogWindFixEvent event) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(event, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogWindFixEvent.class.getSimpleName());
        result.put(FieldNames.WIND.name(), storeWind(event.getWindFix()));
        return result;
    }

    private Object storeRaceLogProtestStartTimeEvent(RaceLogProtestStartTimeEvent event) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(event, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogProtestStartTimeEvent.class.getSimpleName());
        storeTimePoint(event.getProtestStartTime(), result, FieldNames.RACE_LOG_PROTEST_START_TIME);
        return result;
    }

    private Object storeRaceLogStartProcedureChangedEvent(RaceLogStartProcedureChangedEvent event) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(event, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogStartProcedureChangedEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_START_PROCEDURE_TYPE.name(), event.getStartProcedureType().name());
        return result;
    }
    
    private Object storeRaceLogPathfinderEvent(RaceLogPathfinderEvent pathfinderEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(pathfinderEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogPathfinderEvent.class.getSimpleName());

        result.put(FieldNames.RACE_LOG_PATHFINDER_ID.name(), pathfinderEvent.getPathfinderId());
        return result;
    }

    private DBObject storeRaceLogStartTimeEvent(RaceLogStartTimeEvent startTimeEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(startTimeEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogStartTimeEvent.class.getSimpleName());
        
        storeTimePoint(startTimeEvent.getStartTime(), result, FieldNames.RACE_LOG_EVENT_START_TIME);
        result.put(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name(), startTimeEvent.getNextStatus().name());
        return result;
    }

    public DBObject storeRaceLogFlagEvent(RaceLogFlagEvent flagEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(flagEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFlagEvent.class.getSimpleName());

        result.put(FieldNames.RACE_LOG_EVENT_FLAG_UPPER.name(), flagEvent.getUpperFlag().name());
        result.put(FieldNames.RACE_LOG_EVENT_FLAG_LOWER.name(), flagEvent.getLowerFlag().name());
        result.put(FieldNames.RACE_LOG_EVENT_FLAG_DISPLAYED.name(), String.valueOf(flagEvent.isDisplayed()));
        return result;
    }
    
    private void storeRaceLogEventProperties(RaceLogEvent event, DBObject result) {
        storeTimed(event, result);
        storeTimePoint(event.getCreatedAt(), result, FieldNames.RACE_LOG_EVENT_CREATED_AT);
        result.put(FieldNames.RACE_LOG_EVENT_ID.name(), event.getId());
        result.put(FieldNames.RACE_LOG_EVENT_PASS_ID.name(), event.getPassId());
        result.put(FieldNames.RACE_LOG_EVENT_INVOLVED_BOATS.name(), storeInvolvedBoatsForRaceLogEvent(event.getInvolvedBoats()));
        storeRaceLogEventAuthor(result, event.getAuthor());
    }


    private BasicDBList storeInvolvedBoatsForRaceLogEvent(List<Competitor> competitors) {
        BasicDBList dbInvolvedCompetitorIds = new BasicDBList();
        for (Competitor competitor : competitors) {
            dbInvolvedCompetitorIds.add(competitor.getId());
        }
        return dbInvolvedCompetitorIds;
    }

    private DBObject storeRaceLogPassChangeEvent(RaceLogPassChangeEvent passChangeEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(passChangeEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogPassChangeEvent.class.getSimpleName());
        return result;
    }

    private DBObject storeRaceLogRaceStatusEvent(RaceLogRaceStatusEvent raceStatusEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(raceStatusEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogRaceStatusEvent.class.getSimpleName());

        result.put(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name(), raceStatusEvent.getNextStatus().name());
        return result;
    }

    private DBObject storeRaceLogCourseAreaChangedEvent(RaceLogCourseAreaChangedEvent courseAreaChangedEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(courseAreaChangedEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogCourseAreaChangedEvent.class.getSimpleName());

        result.put(FieldNames.COURSE_AREA_ID.name(), courseAreaChangedEvent.getCourseAreaId());
        return result;
    }

    private DBObject storeRaceLogCourseDesignChangedEvent(RaceLogCourseDesignChangedEvent courseDesignChangedEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(courseDesignChangedEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogCourseDesignChangedEvent.class.getSimpleName());
        
        result.put(FieldNames.RACE_LOG_COURSE_DESIGN_NAME.name(), courseDesignChangedEvent.getCourseDesign().getName());
        result.put(FieldNames.RACE_LOG_COURSE_DESIGN.name(), storeCourseBase(courseDesignChangedEvent.getCourseDesign()));
        return result;
    }
    
    private Object storeRaceLogFinishPositioningListChangedEvent(RaceLogFinishPositioningListChangedEvent finishPositioningListChangedEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(finishPositioningListChangedEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFinishPositioningListChangedEvent.class.getSimpleName());
        
        result.put(FieldNames.RACE_LOG_POSITIONED_COMPETITORS.name(), storePositionedCompetitors(finishPositioningListChangedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons()));

        return result;
    }

    private Object storeRaceLogFinishPositioningConfirmedEvent(RaceLogFinishPositioningConfirmedEvent finishPositioningConfirmedEvent) {
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(finishPositioningConfirmedEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFinishPositioningConfirmedEvent.class.getSimpleName());
        
        result.put(FieldNames.RACE_LOG_POSITIONED_COMPETITORS.name(), storePositionedCompetitors(finishPositioningConfirmedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons()));

        return result;
    }
    
    private Object storeRaceLogGateLineOpeningTimeEvent(RaceLogGateLineOpeningTimeEvent gateLineOpeningTimeEvent){
        DBObject result = new BasicDBObject();
        storeRaceLogEventProperties(gateLineOpeningTimeEvent, result);

        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogGateLineOpeningTimeEvent.class.getSimpleName());

        result.put(FieldNames.RACE_LOG_GATE_LINE_OPENING_TIME.name(), gateLineOpeningTimeEvent.getGateLineOpeningTime());
        return result;
    }
    
    private BasicDBList storePositionedCompetitors(List<Triple<Serializable, String, MaxPointsReason>> positionedCompetitors) {
        BasicDBList dbList = new BasicDBList();
        
        if (positionedCompetitors != null) {
            for (Triple<Serializable, String, MaxPointsReason> competitorPair : positionedCompetitors) {
                dbList.add(storePositionedCompetitor(competitorPair));
            }
        }

        return dbList;
    }
    
    private DBObject storePositionedCompetitor(Triple<Serializable, String, MaxPointsReason> competitorTriple) {
        DBObject result = new BasicDBObject();
        
        result.put(FieldNames.COMPETITOR_ID.name(), competitorTriple.getA());
        result.put(FieldNames.COMPETITOR_DISPLAY_NAME.name(), competitorTriple.getB());
        result.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name(), competitorTriple.getC().name());
        
        return result;
    }

    private BasicDBList storeCourseBase(CourseBase courseData) {
        BasicDBList dbList = new BasicDBList();
        
        for (Waypoint waypoint : courseData.getWaypoints()) {
            dbList.add(storeWaypoint(waypoint));
        }
        return dbList;
    }

    private DBObject storeWaypoint(Waypoint waypoint) {
        DBObject result = new BasicDBObject();
        result.put(FieldNames.WAYPOINT_PASSINGSIDE.name(), getPassingSide(waypoint.getPassingSide()));
        result.put(FieldNames.CONTROLPOINT.name(), storeControlPoint(waypoint.getControlPoint()));
        return result;
    }

    private DBObject storeControlPoint(ControlPoint controlPoint) {
        DBObject result = new BasicDBObject();
        if (controlPoint instanceof Mark) {
            result.put(FieldNames.CONTROLPOINT_CLASS.name(), Mark.class.getSimpleName());
            result.put(FieldNames.CONTROLPOINT_VALUE.name(), storeMark((Mark) controlPoint));
        } else if (controlPoint instanceof Gate) {
            result.put(FieldNames.CONTROLPOINT_CLASS.name(), Gate.class.getSimpleName());
            result.put(FieldNames.CONTROLPOINT_VALUE.name(), storeGate((Gate) controlPoint));
        }
        return result;
    }

    private DBObject storeGate(Gate gate) {
        DBObject result = new BasicDBObject();
        result.put(FieldNames.GATE_ID.name(), gate.getId());
        result.put(FieldNames.GATE_NAME.name(), gate.getName());
        result.put(FieldNames.GATE_LEFT.name(), storeMark(gate.getLeft()));
        result.put(FieldNames.GATE_RIGHT.name(), storeMark(gate.getRight()));
        return result;
    }

    private DBObject storeMark(Mark mark) {
        DBObject result = new BasicDBObject();
        result.put(FieldNames.MARK_ID.name(), mark.getId());
        result.put(FieldNames.MARK_COLOR.name(), mark.getColor());
        result.put(FieldNames.MARK_NAME.name(), mark.getName());
        result.put(FieldNames.MARK_PATTERN.name(), mark.getPattern());
        result.put(FieldNames.MARK_SHAPE.name(), mark.getShape());
        result.put(FieldNames.MARK_TYPE.name(), mark.getType().name());
        return result;
    }

    private String getPassingSide(NauticalSide passingSide) {
        String passing = null;
        if (passingSide != null) {
            passing = passingSide.name();
        }
        return passing;
    }

    @Override
    public void storeCompetitor(Competitor competitor) {
        DBCollection collection = database.getCollection(CollectionNames.COMPETITORS.name());
        JSONObject json = competitorSerializer.serialize(competitor);
        BasicDBObject query = new BasicDBObject(CompetitorJsonSerializer.FIELD_ID, competitor.getId());
        DBObject entry = (DBObject) JSON.parse(json.toString());
        collection.update(query, entry, /* upsrt */true, /* multi */false);
    }
    
    @Override
    public void removeAllCompetitors() {
        logger.info("Removing all persistent competitor info");
        DBCollection collection = database.getCollection(CollectionNames.COMPETITORS.name());
        collection.drop();
    }

    @Override
    public void removeCompetitor(Competitor competitor) {
        logger.info("Removing persistent competitor info for competitor "+competitor.getName()+" with ID "+competitor.getId());
        DBCollection collection = database.getCollection(CollectionNames.COMPETITORS.name());
        BasicDBObject query = new BasicDBObject(CompetitorJsonSerializer.FIELD_ID, competitor.getId().toString());
        collection.remove(query);
    }
    @Override
    public void storeDeviceConfiguration(DeviceConfigurationMatcher matcher, DeviceConfiguration configuration) {
        DBCollection configurationsCollections = database.getCollection(CollectionNames.CONFIGURATIONS.name());
        
        DBObject query = new BasicDBObject();
        query.put(FieldNames.CONFIGURATION_MATCHER_ID.name(), matcher.getMatcherIdentifier());
        
        DBObject entryObject = new BasicDBObject();
        entryObject.put(FieldNames.CONFIGURATION_MATCHER_ID.name(), matcher.getMatcherIdentifier());
        entryObject.put(FieldNames.CONFIGURATION_MATCHER.name(), createDeviceConfigurationMatcherObject(matcher));
        entryObject.put(FieldNames.CONFIGURATION_CONFIG.name(), createDeviceConfigurationObject(configuration));
        
        configurationsCollections.update(query, entryObject, /* upsrt */ true, /* multi */ false);
    }

    private DBObject createDeviceConfigurationMatcherObject(DeviceConfigurationMatcher matcher) {
        DBObject matcherObject = new BasicDBObject();
        matcherObject.put(FieldNames.CONFIGURATION_MATCHER_TYPE.name(), matcher.getMatcherType().name());
        if (matcher instanceof DeviceConfigurationMatcherSingle) {
            BasicDBList client = new BasicDBList();
            client.add(((DeviceConfigurationMatcherSingle)matcher).getClientIdentifier());
            matcherObject.put(FieldNames.CONFIGURATION_MATCHER_CLIENTS.name(), client);
        } else if (matcher instanceof DeviceConfigurationMatcherMulti) {
            BasicDBList clients = new BasicDBList();
            Util.addAll(((DeviceConfigurationMatcherMulti)matcher).getClientIdentifiers(), clients);
            matcherObject.put(FieldNames.CONFIGURATION_MATCHER_CLIENTS.name(), clients);
        }
        return matcherObject;
    }

    private DBObject createDeviceConfigurationObject(DeviceConfiguration configuration) {
        JsonSerializer<DeviceConfiguration> serializer = DeviceConfigurationJsonSerializer.create();
        JSONObject json = serializer.serialize(configuration);
        DBObject entry = (DBObject) JSON.parse(json.toString());
        return entry;
    }

    @Override
    public void removeDeviceConfiguration(DeviceConfigurationMatcher matcher) {
        DBCollection configurationsCollections = database.getCollection(CollectionNames.CONFIGURATIONS.name());
        DBObject query = new BasicDBObject();
        query.put(FieldNames.CONFIGURATION_MATCHER_ID.name(), matcher.getMatcherIdentifier());
        configurationsCollections.remove(query);
    }

}
