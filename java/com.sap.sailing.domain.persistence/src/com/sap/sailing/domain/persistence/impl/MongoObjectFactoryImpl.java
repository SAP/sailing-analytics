package com.sap.sailing.domain.persistence.impl;

import java.util.logging.Logger;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RaceInLeaderboard;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.tracking.Positioned;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindTrack;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private static Logger logger = Logger.getLogger(MongoObjectFactoryImpl.class.getName());
    private final DB database;
    
    public MongoObjectFactoryImpl(DB database) {
        super();
        this.database = database;
    }
    
    public DBObject storeWind(Wind wind) {
        DBObject result = new BasicDBObject();
        storePositioned(wind, result);
        storeTimed(wind, result);
        storeSpeedWithBearing(wind, result);
        return result;
    }

    private void storeTimed(Timed timed, DBObject result) {
        result.put(FieldNames.TIME_AS_MILLIS.name(), timed.getTimePoint().asMillis());
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
        windTrack.addListener(new MongoWindListener(trackedRegatta, trackedRace, windSource, this, database));
    }

    public DBCollection getWindTrackCollection() {
        DBCollection result = database.getCollection(CollectionNames.WIND_TRACKS.name());
        result.ensureIndex(new BasicDBObject(FieldNames.EVENT_NAME.name(), null));
        return result;
    }

    public DBObject storeWindTrackEntry(Regatta regatta, RaceDefinition race, WindSource windSource, Wind wind) {
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.EVENT_NAME.name(), regatta.getName());
        result.put(FieldNames.RACE_NAME.name(), race.getName());
        result.put(FieldNames.WIND_SOURCE_NAME.name(), windSource.name());
        if (windSource.getId() != null) {
            result.put(FieldNames.WIND_SOURCE_ID.name(), windSource.getId());
        }
        result.put(FieldNames.WIND.name(), storeWind(wind));
        return result;
    }
    
    @Override
    public void storeRaceIdentifier(RaceIdentifier raceIdentifier, DBObject dbObject) {
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
            logger.throwing(MongoObjectFactoryImpl.class.getName(), "storeLeaderboard", npe);
        }
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
        BasicDBList dbRaceColumns = new BasicDBList();
        result.put(FieldNames.LEADERBOARD_COLUMNS.name(), dbRaceColumns);
        for (RaceInLeaderboard raceColumn : leaderboard.getRaceColumns()) {
            BasicDBObject dbRaceColumn = new BasicDBObject();
            dbRaceColumn.put(FieldNames.LEADERBOARD_COLUMN_NAME.name(), raceColumn.getName());
            dbRaceColumn.put(FieldNames.LEADERBOARD_IS_MEDAL_RACE_COLUMN.name(), raceColumn.isMedalRace());
            storeRaceIdentifier(raceColumn.getRaceIdentifier(), dbRaceColumn);
            dbRaceColumns.add(dbRaceColumn);
        }
        if (leaderboard.hasCarriedPoints()) {
            BasicDBObject dbCarriedPoints = new BasicDBObject();
            result.put(FieldNames.LEADERBOARD_CARRIED_POINTS.name(), dbCarriedPoints);
            for (Competitor competitor : leaderboard.getCompetitors()) {
                dbCarriedPoints.put(MongoUtils.escapeDollarAndDot(competitor.getName()), leaderboard.getCarriedPoints(competitor));
            }
        }
        BasicDBObject dbScoreCorrections = new BasicDBObject();
        storeScoreCorrections(leaderboard, dbScoreCorrections);
        result.put(FieldNames.LEADERBOARD_SCORE_CORRECTIONS.name(), dbScoreCorrections);
        BasicDBList dbResultDiscardingThresholds = new BasicDBList();
        for (int threshold : leaderboard.getResultDiscardingRule().getDiscardIndexResultsStartingWithHowManyRaces()) {
            dbResultDiscardingThresholds.add(threshold);
        }
        result.put(FieldNames.LEADERBOARD_DISCARDING_THRESHOLDS.name(), dbResultDiscardingThresholds);
        BasicDBObject competitorDisplayNames = new BasicDBObject();
        for (Competitor competitor : leaderboard.getCompetitors()) {
            String displayNameForCompetitor = leaderboard.getDisplayName(competitor);
            if (displayNameForCompetitor != null) {
                competitorDisplayNames.put(MongoUtils.escapeDollarAndDot(competitor.getName()), displayNameForCompetitor);
            }
        }
        result.put(FieldNames.LEADERBOARD_COMPETITOR_DISPLAY_NAMES.name(), competitorDisplayNames);
        leaderboardCollection.update(query, result, /* upsrt */ true, /* multi */ false);
    }

    private void storeScoreCorrections(Leaderboard leaderboard, BasicDBObject dbScoreCorrections) {
        SettableScoreCorrection scoreCorrection = leaderboard.getScoreCorrection();
        for (RaceInLeaderboard raceColumn : leaderboard.getRaceColumns()) {
            BasicDBObject dbCorrectionForRace = new BasicDBObject();
            for (Competitor competitor : leaderboard.getCompetitors()) {
                if (scoreCorrection.isScoreCorrected(competitor, raceColumn)) {
                    BasicDBObject dbCorrectionForCompetitor = new BasicDBObject();
                    MaxPointsReason maxPointsReason = scoreCorrection.getMaxPointsReason(competitor, raceColumn);
                    if (maxPointsReason != MaxPointsReason.NONE) {
                        dbCorrectionForCompetitor.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name(),
                                maxPointsReason.name());
                    }
                    Integer explicitScoreCorrection = scoreCorrection
                            .getExplicitScoreCorrection(competitor, raceColumn);
                    if (explicitScoreCorrection != null) {
                        dbCorrectionForCompetitor.put(FieldNames.LEADERBOARD_CORRECTED_SCORE.name(),
                                explicitScoreCorrection);
                    }
                    dbCorrectionForRace.put(MongoUtils.escapeDollarAndDot(competitor.getName()), dbCorrectionForCompetitor);
                }
            }
            if (!dbCorrectionForRace.isEmpty()) {
                dbScoreCorrections.put(raceColumn.getName(), dbCorrectionForRace);
            }
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
            logger.throwing(MongoObjectFactoryImpl.class.getName(), "storeLeaderboardGroup", npe);
        }
        
        BasicDBObject query = new BasicDBObject(FieldNames.LEADERBOARD_GROUP_NAME.name(), leaderboardGroup.getName());
        
        BasicDBObject result = new BasicDBObject();
        result.put(FieldNames.LEADERBOARD_GROUP_NAME.name(), leaderboardGroup.getName());
        result.put(FieldNames.LEADERBOARD_GROUP_DESCRIPTION.name(), leaderboardGroup.getDescription());
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
        result.put(FieldNames.LEADERBOARD_GROUP_LEADERBOARDS.name(), dbLeaderboardIds);
        
        leaderboardGroupCollection.update(query, result, true, false);
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
    
    

}
