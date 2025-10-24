package com.sap.sailing.domain.persistence.impl;

import static com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.getPassingInstructions;
import static com.sap.sailing.shared.persistence.impl.MongoObjectFactoryImpl.storeDeviceId;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoCommandException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCImpliedWindSourceEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCScratchBoatEvent;
import com.sap.sailing.domain.abstractlog.orc.RegattaLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseAreaSpecification;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogDependentStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEndOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogExcludeWindSourcesEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFixedMarkPassingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPassChangeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPathfinderEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogResultsAreOfficialEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogSuppressedMarkPassingsEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogTagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.scoring.RaceLogAdditionalScoringInformationEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogStartTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogUseCompetitorsFromRaceLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogCloseOpenEndedDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDefineMarkEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMarkMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRegisterBoatEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogSetCompetitorTimeOnTimeFactorEvent;
import com.sap.sailing.domain.anniversary.DetailedRaceInfo;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.SailingServerConfiguration;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.Positioned;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithOtherTieBreakingLeaderboard;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorWithBoatRefJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DeviceConfigurationJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RegattaConfigurationJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.ImpliedWindSourceSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.ORCCertificateJsonSerializer;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sailing.shared.persistence.device.impl.PlaceHolderDeviceIdentifierMongoHandler;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.shared.json.JsonSerializer;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private static Logger logger = Logger.getLogger(MongoObjectFactoryImpl.class.getName());
    private final MongoDatabase database;
    private final CompetitorWithBoatRefJsonSerializer competitorWithBoatRefSerializer = CompetitorWithBoatRefJsonSerializer.create(/* serializeNonPublicCompetitorFields */ true);
    private final CompetitorJsonSerializer competitorSerializer = CompetitorJsonSerializer.create(
            /* serialize boat */ true, /* serializeNonPublicCompetitorFields */ true, /* verboseBoatClassSerializer */ false);
    private final BoatJsonSerializer boatSerializer = new BoatJsonSerializer(new BoatClassJsonSerializer(/* verbose */ false));
    private final TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder;
    private final TypeBasedServiceFinder<RaceTrackingConnectivityParametersHandler> raceTrackingConnectivityParamsServiceFinder;

    /**
     * Uses <code>null</code> for the device type service finder and hence will be unable to store device identifiers.
     * Use this constructor only for testing purposes or in cases where there will happen absolutely no access to
     * {@link DeviceIdentifier} objects.
     */
    public MongoObjectFactoryImpl(MongoDatabase database) {
        this(database, /* deviceTypeServiceFinder */ null);
    }

    public MongoObjectFactoryImpl(MongoDatabase database, TypeBasedServiceFinderFactory serviceFinderFactory) {
        this.database = database;
        if (serviceFinderFactory != null) {
            this.deviceIdentifierServiceFinder = serviceFinderFactory.createServiceFinder(DeviceIdentifierMongoHandler.class);
            this.deviceIdentifierServiceFinder.setFallbackService(new PlaceHolderDeviceIdentifierMongoHandler());
            this.raceTrackingConnectivityParamsServiceFinder = serviceFinderFactory.createServiceFinder(RaceTrackingConnectivityParametersHandler.class);
        } else {
            this.deviceIdentifierServiceFinder = null;
            this.raceTrackingConnectivityParamsServiceFinder = null;
        }
    }

    @Override
    public MongoDatabase getDatabase() {
        return database;
    }

    @Override
    public Document storeWind(Wind wind) {
        Document result = new Document();
        storePositioned(wind, result);
        storeTimed(wind, result);
        storeSpeedWithBearing(wind, result);
        return result;
    }

    public static void storeTimePoint(TimePoint timePoint, Document result, String fieldName) {
        if (timePoint != null) {
            result.put(fieldName, timePoint.asMillis());
        }
    }

    public static void storeTimePoint(TimePoint timePoint, Document result, FieldNames field) {
        storeTimePoint(timePoint, result, field.name());
    }

    public static void storeTimeRange(TimeRange timeRange, Document result, FieldNames field) {
        if (timeRange != null) {
            Document timeRangeObj = new Document();
            storeTimePoint(timeRange.from(), timeRangeObj, FieldNames.FROM_MILLIS);
            storeTimePoint(timeRange.to(), timeRangeObj, FieldNames.TO_MILLIS);
            result.put(field.name(), timeRangeObj);
        }
    }

    public void storeTimed(Timed timed, Document result) {
        if (timed.getTimePoint() != null) {
            storeTimePoint(timed.getTimePoint(), result, FieldNames.TIME_AS_MILLIS);
        }
    }

    public void storeSpeedWithBearing(SpeedWithBearing speedWithBearing, Document result) {
        storeSpeed(speedWithBearing, result);
        storeBearing(speedWithBearing.getBearing(), result);
    }
    
    public void storeOptionalTrueHeading(Bearing optionalTrueHeading, Document result) {
        if (optionalTrueHeading != null) {
            result.put(FieldNames.TRUE_HEADING_DEG.name(), optionalTrueHeading.getDegrees());
        }
    }

    public void storeBearing(Bearing bearing, Document result) {
        result.put(FieldNames.DEGREE_BEARING.name(), bearing.getDegrees());
    }

    public void storeSpeed(Speed speed, Document result) {
        result.put(FieldNames.KNOT_SPEED.name(), speed.getKnots());
    }

    public void storePositioned(Positioned positioned, Document result) {
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

    public MongoCollection<Document> getWindTrackCollection() {
        MongoCollection<Document> result = database.getCollection(CollectionNames.WIND_TRACKS.name());
        DomainObjectFactoryImpl.ensureIndicesOnWindTracks(result);
        return result;
    }

    public MongoCollection<Document> getGPSFixCollection(ClientSession clientSession) {
        MongoCollection<Document> gpsFixCollection = database.getCollection(CollectionNames.GPS_FIXES.name());
        // Removes old indexes not needed anymore
        dropIndexSafe(gpsFixCollection, "DEVICE_ID.DEVICE_TYPE_SPECIFIC_ID_1_GPSFIX.TIME_AS_MILLIS_1");
        dropIndexSafe(gpsFixCollection, "DEVICE_ID_1_GPSFIX.TIME_AS_MILLIS_1");
        dropIndexSafe(gpsFixCollection, "fixbytimeanddev");
        Document index = new Document();
        index.put(FieldNames.DEVICE_ID.name()+"."+com.sap.sailing.shared.persistence.impl.FieldNames.DEVICE_STRING_REPRESENTATION.name(), "hashed");
        index.put(FieldNames.TIME_AS_MILLIS.name(), 1);
        index.put(FieldNames.DEVICE_ID.name()+"."+com.sap.sailing.shared.persistence.impl.FieldNames.DEVICE_TYPE.name(), 1);
        index.put(FieldNames.DEVICE_ID.name()+"."+com.sap.sailing.shared.persistence.impl.FieldNames.DEVICE_TYPE_SPECIFIC_ID.name(), 1);
        final IndexOptions indexOptions = new IndexOptions().name("fixbydevandtime").background(false);
        if (clientSession == null) {
            gpsFixCollection.createIndex(index, indexOptions);
        } else {
            gpsFixCollection.createIndex(clientSession, index, indexOptions);
        }
        return gpsFixCollection;
    }

    /**
     * Dropping an index that does not exist causes an exception. This method first checks if the index exist to prevent
     * an exception from occurring.
     */
    private void dropIndexSafe(MongoCollection<Document> collection, String indexName) {
        collection.listIndexes().forEach((Document indexInfo) -> {
            if (indexName.equals(indexInfo.get("name"))) {
                collection.dropIndex(indexName);
            }
        });
    }

    public MongoCollection<Document> getGPSFixMetadataCollection() {
        MongoCollection<Document> collection = database.getCollection(CollectionNames.GPS_FIXES_METADATA.name());
        Document index = new Document();
        index.put(FieldNames.DEVICE_ID.name(), 1);
        collection.createIndex(index);
        return collection;
    }

    /**
     * @param regattaName
     *            the regatta name is stored only for human readability purposes because a time stamp may be a bit unhandy for
     *            identifying where the wind fix was collected
     */
    public Document storeWindTrackEntry(RaceDefinition race, String regattaName, WindSource windSource, Wind wind) {
        Document result = new Document();
        result.put(FieldNames.RACE_ID.name(), race.getId());
        result.put(FieldNames.REGATTA_NAME.name(), regattaName);
        result.put(FieldNames.WIND_SOURCE_NAME.name(), windSource.name());
        if (windSource.getId() != null) {
            result.put(FieldNames.WIND_SOURCE_ID.name(), windSource.getId());
        }
        result.put(FieldNames.WIND.name(), storeWind(wind));
        return result;
    }

    private void storeRaceIdentifiers(RaceColumn raceColumn, Document dbObject) {
        Document raceIdentifiersPerFleet = new Document();
        for (Fleet fleet : raceColumn.getFleets()) {
            RaceIdentifier raceIdentifier = raceColumn.getRaceIdentifier(fleet);
            if (raceIdentifier != null) {
                Document raceIdentifierForFleet = new Document();
                storeRaceIdentifier(raceIdentifierForFleet, raceIdentifier);
                raceIdentifiersPerFleet.put(MongoUtils.escapeDollarAndDot(fleet.getName()), raceIdentifierForFleet);
            }
        }
        dbObject.put(FieldNames.RACE_IDENTIFIERS.name(), raceIdentifiersPerFleet);
    }

    private void storeRaceIdentifier(Document dbObject, RaceIdentifier raceIdentifier) {
        if (raceIdentifier != null) {
            dbObject.put(FieldNames.EVENT_NAME.name(), raceIdentifier.getRegattaName());
            dbObject.put(FieldNames.RACE_NAME.name(), raceIdentifier.getRaceName());
        }
    }

    @Override
    public void storeLeaderboard(Leaderboard leaderboard) {
        MongoCollection<Document> leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name()).withWriteConcern(WriteConcern.MAJORITY);
        try {
            leaderboardCollection.createIndex(new Document(FieldNames.LEADERBOARD_NAME.name(), 1));
        } catch (NullPointerException npe) {
            // sometimes, for reasons yet to be clarified, ensuring an index on the name field causes an NPE
            logger.log(Level.SEVERE, "storeLeaderboard", npe);
        }
        Document query = new Document(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
        Document dbLeaderboard = new Document();
        dbLeaderboard.put(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
        if (leaderboard.getDisplayName() != null) {
            dbLeaderboard.put(FieldNames.LEADERBOARD_DISPLAY_NAME.name(), leaderboard.getDisplayName());
        }
        if (leaderboard instanceof RegattaLeaderboardWithEliminations) {
            dbLeaderboard.put(FieldNames.WRAPPED_REGATTA_LEADERBOARD_NAME.name(), ((RegattaLeaderboardWithEliminations) leaderboard).getRegatta().getName());
            BasicDBList eliminatedCompetitorIds = new BasicDBList();
            for (final Competitor c : ((RegattaLeaderboardWithEliminations) leaderboard).getEliminatedCompetitors()) {
                eliminatedCompetitorIds.add(c.getId());
            }
            dbLeaderboard.put(FieldNames.ELMINATED_COMPETITORS.name(), eliminatedCompetitorIds);
        } else {
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
            final BasicDBList courseAreaIdsAsStrings = new BasicDBList();
            Util.addAll(Util.map(leaderboard.getCourseAreas(), ca->ca.getId().toString()), courseAreaIdsAsStrings);
            dbLeaderboard.put(FieldNames.COURSE_AREA_IDS.name(), courseAreaIdsAsStrings);
            storeColumnFactors(leaderboard, dbLeaderboard);
            storeLeaderboardCorrectionsAndDiscards(leaderboard, dbLeaderboard);
        }
        leaderboardCollection.replaceOne(query, dbLeaderboard, new ReplaceOptions().upsert(true));
    }

    private void storeColumnFactors(Leaderboard leaderboard, Document dbLeaderboard) {
        Document raceColumnFactors = new Document();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            Double explicitFactor = raceColumn.getExplicitFactor();
            if (explicitFactor != null) {
                raceColumnFactors.put(MongoUtils.escapeDollarAndDot(raceColumn.getName()), explicitFactor);
            }
        }
        dbLeaderboard.put(FieldNames.LEADERBOARD_COLUMN_FACTORS.name(), raceColumnFactors);
    }

    private void storeRegattaLeaderboard(RegattaLeaderboard leaderboard, Document dbLeaderboard) {
        dbLeaderboard.put(FieldNames.REGATTA_NAME.name(), leaderboard.getRegatta().getName());
        if (leaderboard instanceof RegattaLeaderboardWithOtherTieBreakingLeaderboard) {
            dbLeaderboard.put(FieldNames.OTHER_TIEBREAKING_LEADERBOARD_NAME.name(),
                    ((RegattaLeaderboardWithOtherTieBreakingLeaderboard) leaderboard).getOtherTieBreakingLeaderboard().getName());
        }
    }

    private void storeFlexibleLeaderboard(FlexibleLeaderboard leaderboard, Document dbLeaderboard) {
        BasicDBList dbRaceColumns = new BasicDBList();
        dbLeaderboard.put(FieldNames.SCORING_SCHEME_TYPE.name(), leaderboard.getScoringScheme().getType().name());
        dbLeaderboard.put(FieldNames.LEADERBOARD_COLUMNS.name(), dbRaceColumns);
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            Document dbRaceColumn = storeRaceColumn(raceColumn);
            dbRaceColumns.add(dbRaceColumn);
        }
    }

    private void storeLeaderboardCorrectionsAndDiscards(Leaderboard leaderboard, Document dbLeaderboard) {
        if (leaderboard.hasCarriedPoints()) {
            BasicDBList dbCarriedPoints = new BasicDBList();
            dbLeaderboard.put(FieldNames.LEADERBOARD_CARRIED_POINTS_BY_ID.name(), dbCarriedPoints);
            for (Entry<Competitor, Double> competitorWithCarriedPoints : leaderboard
                    .getCompetitorsForWhichThereAreCarriedPoints().entrySet()) {
                double carriedPoints = competitorWithCarriedPoints.getValue();
                Competitor competitor = competitorWithCarriedPoints.getKey();
                Document dbCarriedPointsForCompetitor = new Document();
                dbCarriedPointsForCompetitor.put(FieldNames.COMPETITOR_ID.name(), competitor.getId());
                dbCarriedPointsForCompetitor.put(FieldNames.LEADERBOARD_CARRIED_POINTS.name(), carriedPoints);
                dbCarriedPoints.add(dbCarriedPointsForCompetitor);
            }
        }
        Document dbScoreCorrections = new Document();
        storeScoreCorrections(leaderboard, dbScoreCorrections);
        dbLeaderboard.put(FieldNames.LEADERBOARD_SCORE_CORRECTIONS.name(), dbScoreCorrections);
        final ResultDiscardingRule resultDiscardingRule = leaderboard.getResultDiscardingRule();
        storeResultDiscardingRule(dbLeaderboard, resultDiscardingRule, FieldNames.LEADERBOARD_DISCARDING_THRESHOLDS);
        BasicDBList competitorDisplayNames = new BasicDBList();
        for (Competitor competitor : leaderboard.getCompetitors()) {
            String displayNameForCompetitor = leaderboard.getDisplayName(competitor);
            if (displayNameForCompetitor != null) {
                Document dbDisplayName = new Document();
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
    private void storeResultDiscardingRule(Document dbObject,
            final ResultDiscardingRule resultDiscardingRule, FieldNames field) {
        if (resultDiscardingRule != null && resultDiscardingRule instanceof ThresholdBasedResultDiscardingRule) {
            BasicDBList dbResultDiscardingThresholds = new BasicDBList();
            for (int threshold : ((ThresholdBasedResultDiscardingRule) resultDiscardingRule).getDiscardIndexResultsStartingWithHowManyRaces()) {
                dbResultDiscardingThresholds.add(threshold);
            }
            dbObject.put(field.name(), dbResultDiscardingThresholds);
        }
    }

    private Document storeRaceColumn(RaceColumn raceColumn) {
        Document dbRaceColumn = new Document();
        dbRaceColumn.put(FieldNames.LEADERBOARD_COLUMN_NAME.name(), raceColumn.getName());
        dbRaceColumn.put(FieldNames.LEADERBOARD_IS_MEDAL_RACE_COLUMN.name(), raceColumn.isMedalRace());
        storeRaceIdentifiers(raceColumn, dbRaceColumn);
        return dbRaceColumn;
    }

    private void storeScoreCorrections(Leaderboard leaderboard, Document dbScoreCorrections) {
        final TimePoint now = TimePoint.now();
        final SettableScoreCorrection scoreCorrection = leaderboard.getScoreCorrection();
        for (final RaceColumn raceColumn : scoreCorrection.getRaceColumnsThatHaveCorrections()) {
            final BasicDBList dbCorrectionForRace = new BasicDBList();
            for (Competitor competitor : scoreCorrection.getCompetitorsThatHaveCorrectionsIn(raceColumn)) {
                // TODO bug 655: make score corrections time dependent
                if (scoreCorrection.isScoreCorrected(competitor, raceColumn, now)) {
                    final Document dbCorrectionForCompetitor = new Document();
                    dbCorrectionForCompetitor.put(FieldNames.COMPETITOR_ID.name(), competitor.getId());
                    final MaxPointsReason maxPointsReason = scoreCorrection.getMaxPointsReason(competitor, raceColumn, now);
                    if (maxPointsReason != MaxPointsReason.NONE) {
                        dbCorrectionForCompetitor.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name(),
                                maxPointsReason.name());
                    }
                    final Double explicitScoreCorrection = scoreCorrection
                            .getExplicitScoreCorrection(competitor, raceColumn);
                    if (explicitScoreCorrection != null) {
                        dbCorrectionForCompetitor.put(FieldNames.LEADERBOARD_CORRECTED_SCORE.name(),
                                explicitScoreCorrection);
                    }
                    final Double incrementalScoreCorrectionInPoints = scoreCorrection.getIncementalScoreCorrectionInPoints(competitor, raceColumn);
                    if (incrementalScoreCorrectionInPoints != null) {
                        dbCorrectionForCompetitor.put(FieldNames.LEADERBOARD_INCREMENTAL_SCORE_CORRECTION_IN_POINTS.name(), incrementalScoreCorrectionInPoints);
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
        MongoCollection<Document> leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());
        Document query = new Document(FieldNames.LEADERBOARD_NAME.name(), leaderboardName);
        leaderboardCollection.deleteOne(query);
    }

    @Override
    public void storeLeaderboardGroup(LeaderboardGroup leaderboardGroup) {
        MongoCollection<Document> leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        MongoCollection<Document> leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name()).withReadConcern(ReadConcern.MAJORITY);
        try {
            leaderboardGroupCollection.createIndex(new Document(FieldNames.LEADERBOARD_GROUP_NAME.name(), 1));
        } catch (NullPointerException npe) {
            // sometimes, for reasons yet to be clarified, ensuring an index on the name field causes an NPE
            logger.log(Level.SEVERE, "storeLeaderboardGroup", npe);
        }
        Document query = new Document(FieldNames.LEADERBOARD_GROUP_NAME.name(), leaderboardGroup.getName());
        Document dbLeaderboardGroup = new Document();
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_UUID.name(), leaderboardGroup.getId());
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_NAME.name(), leaderboardGroup.getName());
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_DESCRIPTION.name(), leaderboardGroup.getDescription());
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_DISPLAY_NAME.name(), leaderboardGroup.getDisplayName());
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_DISPLAY_IN_REVERSE_ORDER.name(), leaderboardGroup.isDisplayGroupsInReverseOrder());
        final Leaderboard overallLeaderboard = leaderboardGroup.getOverallLeaderboard();
        if (overallLeaderboard != null) {
            Document overallLeaderboardQuery = new Document(FieldNames.LEADERBOARD_NAME.name(), overallLeaderboard.getName());
            Document dbOverallLeaderboard = leaderboardCollection.find(overallLeaderboardQuery).first();
            if (dbOverallLeaderboard == null) {
                storeLeaderboard(overallLeaderboard);
                dbOverallLeaderboard = leaderboardCollection.withReadPreference(ReadPreference.primary()).find(overallLeaderboardQuery).first();
            }
            ObjectId dbOverallLeaderboardId = (ObjectId) dbOverallLeaderboard.get("_id");
            dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_OVERALL_LEADERBOARD.name(), dbOverallLeaderboardId);
        }
        BasicDBList dbLeaderboardIds = new BasicDBList();
        for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
            Document leaderboardQuery = new Document(FieldNames.LEADERBOARD_NAME.name(), leaderboard.getName());
            Document dbLeaderboard = leaderboardCollection.find(leaderboardQuery).first();
            if (dbLeaderboard == null) {
                // TODO use readConcern / writeConcern "majority"
                storeLeaderboard(leaderboard);
                dbLeaderboard = leaderboardCollection.find(leaderboardQuery).first();
            }
            ObjectId dbLeaderboardId = (ObjectId) dbLeaderboard.get("_id");
            dbLeaderboardIds.add(dbLeaderboardId);
        }
        dbLeaderboardGroup.put(FieldNames.LEADERBOARD_GROUP_LEADERBOARDS.name(), dbLeaderboardIds);
        leaderboardGroupCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbLeaderboardGroup, new ReplaceOptions().upsert(true));
    }

    @Override
    public void removeLeaderboardGroup(UUID leaderboardGroupId) {
        MongoCollection<Document> leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        Document query = new Document(FieldNames.LEADERBOARD_GROUP_UUID.name(), leaderboardGroupId);
        leaderboardGroupCollection.deleteOne(query);
    }

    @Override
    public void renameLeaderboardGroup(UUID leaderboardGroupId, String newName) {
        MongoCollection<Document> leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        Document query = new Document(FieldNames.LEADERBOARD_GROUP_UUID.name(), leaderboardGroupId);
        Document update = new Document("$set", new Document(FieldNames.LEADERBOARD_GROUP_NAME.name(), newName));
        leaderboardGroupCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).updateOne(query, update, new UpdateOptions().upsert(true));
    }

    @Override
    public void storeServerConfiguration(SailingServerConfiguration serverConfiguration) {
        MongoCollection<Document> serverCollection = database.getCollection(CollectionNames.SERVER_CONFIGURATION.name());
        Document newServerConfig = new Document();
        newServerConfig.put(FieldNames.SERVER_IS_STANDALONE.name(), serverConfiguration.isStandaloneServer());
        Document currentServerConfig = serverCollection.find().first();
        if (currentServerConfig != null) {
            serverCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(
                    currentServerConfig, newServerConfig, new ReplaceOptions().upsert(true));
        } else {
            serverCollection.insertOne(newServerConfig);
        }
    }

    @Override
    public void storeSailingServer(RemoteSailingServerReference server) {
        MongoCollection<Document> serverCollection = database.getCollection(CollectionNames.SAILING_SERVERS.name());
        serverCollection.createIndex(new Document(FieldNames.SERVER_NAME.name(), 1));
        Document query = new Document();
        query.put(FieldNames.SERVER_NAME.name(), server.getName());
        Document serverDBObject = new Document();
        serverDBObject.put(FieldNames.SERVER_NAME.name(), server.getName());
        serverDBObject.put(FieldNames.SERVER_URL.name(), server.getURL().toExternalForm());
        serverDBObject.put(FieldNames.INCLUDE.name(), server.isInclude());
        serverDBObject.put(FieldNames.SELECTED_EVENT_IDS.name(), server.getSelectedEventIds());
        serverCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, serverDBObject, new ReplaceOptions().upsert(true));
    }

    @Override
    public void updateSailingServer(final String serverName, final boolean include,
            final Set<UUID> selectedEventIds) {
        MongoCollection<Document> serverCollection = database.getCollection(CollectionNames.SAILING_SERVERS.name());
        Document query = new Document();
        query.put(FieldNames.SERVER_NAME.name(), serverName);
        Document serverDBObject = new Document();
        serverDBObject.put(FieldNames.INCLUDE.name(), include);
        serverDBObject.put(FieldNames.SELECTED_EVENT_IDS.name(), selectedEventIds);
        serverCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, serverDBObject,
                new ReplaceOptions().upsert(true));
    }

    @Override
    public void removeSailingServer(String name) {
        MongoCollection<Document> serverCollection = database.getCollection(CollectionNames.SAILING_SERVERS.name());
        Document query = new Document(FieldNames.SERVER_NAME.name(), name);
        serverCollection.deleteOne(query);
    }

    /**
     * StoreEvent() uses some deprecated methods of event to keep backward compatibility.
     */
    @Override
    public void storeEvent(Event event) {
        MongoCollection<Document> eventCollection = database.getCollection(CollectionNames.EVENTS.name());
        eventCollection.createIndex(new Document(FieldNames.EVENT_ID.name(), 1));
        Document query = new Document();
        query.put(FieldNames.EVENT_ID.name(), event.getId());
        Document eventDBObject = new Document();
        eventDBObject.put(FieldNames.EVENT_NAME.name(), event.getName());
        eventDBObject.put(FieldNames.EVENT_DESCRIPTION.name(), event.getDescription());
        eventDBObject.put(FieldNames.EVENT_ID.name(), event.getId());
        eventDBObject.put(FieldNames.EVENT_OFFICIAL_WEBSITE_URL.name(), event.getOfficialWebsiteURL() != null ? event.getOfficialWebsiteURL().toString() : null);
        eventDBObject.put(FieldNames.EVENT_BASE_URL.name(), event.getBaseURL() != null ? event.getBaseURL().toString() : null);
        storeTimePoint(event.getStartDate(), eventDBObject, FieldNames.EVENT_START_DATE);
        storeTimePoint(event.getEndDate(), eventDBObject, FieldNames.EVENT_END_DATE);
        eventDBObject.put(FieldNames.EVENT_IS_PUBLIC.name(), event.isPublic());
        BasicDBList windFinderSpotCollectionIds = new BasicDBList();
        for (final String windFinderSpotCollectionId : event.getWindFinderReviewedSpotsCollectionIds()) {
            windFinderSpotCollectionIds.add(windFinderSpotCollectionId);
        }
        eventDBObject.put(FieldNames.EVENT_WINDFINDER_SPOT_COLLECTION_IDS.name(), windFinderSpotCollectionIds);
        Document venueDBObject = getVenueAsDBObject(event.getVenue());
        eventDBObject.put(FieldNames.VENUE.name(), venueDBObject);
        BasicDBList images = new BasicDBList();
        for (ImageDescriptor image : event.getImages()) {
            Document imageObject = createImageObject(image);
            images.add(imageObject);
        }
        eventDBObject.put(FieldNames.EVENT_IMAGES.name(), images);
        BasicDBList videos = new BasicDBList();
        for (VideoDescriptor video: event.getVideos()) {
            Document videoObject = createVideoObject(video);
            videos.add(videoObject);
        }
        eventDBObject.put(FieldNames.EVENT_VIDEOS.name(), videos);
        BasicDBList sailorsInfoWebsiteURLs = new BasicDBList();
        for(Map.Entry<Locale, URL> sailorsInfoWebsite : event.getSailorsInfoWebsiteURLs().entrySet()) {
            Document sailorsInfoWebsiteObject = createSailorsInfoWebsiteObject(sailorsInfoWebsite.getKey(), sailorsInfoWebsite.getValue());
            sailorsInfoWebsiteURLs.add(sailorsInfoWebsiteObject);
        }
        eventDBObject.put(FieldNames.EVENT_SAILORS_INFO_WEBSITES.name(), sailorsInfoWebsiteURLs);
        eventCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, eventDBObject, new ReplaceOptions().upsert(true));
        // now store the links to the leaderboard groups
        MongoCollection<Document> linksCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUP_LINKS_FOR_EVENTS.name());
        linksCollection.createIndex(new Document(FieldNames.EVENT_ID.name(), 1));
        BasicDBList lgUUIDs = new BasicDBList();
        for (LeaderboardGroup lg : event.getLeaderboardGroups()) {
            lgUUIDs.add(lg.getId());
        }
        Document dbLinks = new Document();
        dbLinks.put(FieldNames.EVENT_ID.name(), event.getId());
        dbLinks.put(FieldNames.LEADERBOARD_GROUP_UUID.name(), lgUUIDs);
        linksCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbLinks, new ReplaceOptions().upsert(true));
    }

    @Override
    public void renameEvent(Serializable id, String newName) {
        MongoCollection<Document> eventCollection = database.getCollection(CollectionNames.EVENTS.name());
        Document query = new Document(FieldNames.EVENT_ID.name(), id);
        Document renameUpdate = new Document("$set", new Document(FieldNames.EVENT_NAME.name(), newName));
        eventCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).updateOne(query, renameUpdate, new UpdateOptions().upsert(true));
    }

    @Override
    public void removeEvent(Serializable id) {
        MongoCollection<Document> eventsCollection = database.getCollection(CollectionNames.EVENTS.name());
        Document query = new Document(FieldNames.EVENT_ID.name(), id);
        eventsCollection.deleteOne(query);
    }

    private Document getVenueAsDBObject(Venue venue) {
        Document result = new Document();
        result.put(FieldNames.VENUE_NAME.name(), venue.getName());
        BasicDBList courseAreaList = new BasicDBList();
        result.put(FieldNames.COURSE_AREAS.name(), courseAreaList);
        for (CourseArea courseArea : venue.getCourseAreas()) {
            Document dbCourseArea = new Document();
            courseAreaList.add(dbCourseArea);
            dbCourseArea.put(FieldNames.COURSE_AREA_NAME.name(), courseArea.getName());
            dbCourseArea.put(FieldNames.COURSE_AREA_ID.name(), courseArea.getId());
            final Document courseAreaCenterPosition;
            if (courseArea.getCenterPosition() != null) {
                courseAreaCenterPosition = new Document();
                storePositioned(courseArea, courseAreaCenterPosition);
            } else {
                courseAreaCenterPosition = null;
            }
            dbCourseArea.put(FieldNames.COURSE_AREA_CENTER_POSITION.name(), courseAreaCenterPosition);
            final Distance courseAreaRadius = courseArea.getRadius();
            if (courseAreaRadius != null) {
                dbCourseArea.put(FieldNames.COURSE_AREA_RADIUS_IN_METERS.name(), courseAreaRadius.getMeters());
            }
        }
        return result;
    }

    @Override
    public void storeRegatta(Regatta regatta) {
        MongoCollection<Document> regattasCollection = database.getCollection(CollectionNames.REGATTAS.name());
        Document regattaByNameIndexKey = new Document(FieldNames.REGATTA_NAME.name(), 1);
        try {
            regattasCollection.createIndex(regattaByNameIndexKey, new IndexOptions().unique(true).background(false));
        } catch (MongoCommandException e) {
            // the index probably existed as non-unique; remove and create again
            regattasCollection.dropIndex(regattaByNameIndexKey);
            regattasCollection.createIndex(regattaByNameIndexKey, new IndexOptions().unique(true).background(false));
        }
        Document regattaByIdIndexKey = new Document(FieldNames.REGATTA_ID.name(), 1);
        try {
            regattasCollection.createIndex(regattaByIdIndexKey, new IndexOptions().unique(true).background(false));
        } catch (MongoCommandException e) {
            regattasCollection.dropIndex(regattaByIdIndexKey);
            regattasCollection.createIndex(regattaByIdIndexKey, new IndexOptions().unique(true).background(false));
        }
        Document dbRegatta = new Document();
        Document query = new Document(FieldNames.REGATTA_NAME.name(), regatta.getName());
        dbRegatta.put(FieldNames.REGATTA_NAME.name(), regatta.getName());
        dbRegatta.put(FieldNames.REGATTA_ID.name(), regatta.getId());
        storeTimePoint(regatta.getStartDate(), dbRegatta, FieldNames.REGATTA_START_DATE);
        storeTimePoint(regatta.getEndDate(), dbRegatta, FieldNames.REGATTA_END_DATE);
        dbRegatta.put(FieldNames.SCORING_SCHEME_TYPE.name(), regatta.getScoringScheme().getType().name());
        if (regatta.getBoatClass() != null) {
            dbRegatta.put(FieldNames.BOAT_CLASS_NAME.name(), regatta.getBoatClass().getName());
            dbRegatta.put(FieldNames.BOAT_CLASS_TYPICALLY_STARTS_UPWIND.name(), regatta.getBoatClass().typicallyStartsUpwind());
        }
        dbRegatta.put(FieldNames.REGATTA_SERIES.name(), storeSeries(regatta.getSeries()));
        final BasicDBList courseAreaIdsAsStrings = new BasicDBList();
        Util.addAll(Util.map(regatta.getCourseAreas(), ca->ca.getId().toString()), courseAreaIdsAsStrings);
        dbRegatta.put(FieldNames.COURSE_AREA_IDS.name(), courseAreaIdsAsStrings);
        if (regatta.getRegattaConfiguration() != null) {
            JsonSerializer<RegattaConfiguration> serializer = RegattaConfigurationJsonSerializer.create();
            JSONObject json = serializer.serialize(regatta.getRegattaConfiguration());
            Document configurationObject = Document.parse(json.toString());
            dbRegatta.put(FieldNames.REGATTA_REGATTA_CONFIGURATION.name(), configurationObject);
        }
        dbRegatta.put(FieldNames.REGATTA_BUOY_ZONE_RADIUS_IN_HULL_LENGTHS.name(), regatta.getBuoyZoneRadiusInHullLengths());
        dbRegatta.put(FieldNames.REGATTA_USE_START_TIME_INFERENCE.name(), regatta.useStartTimeInference());
        dbRegatta.put(FieldNames.REGATTA_CONTROL_TRACKING_FROM_START_AND_FINISH_TIMES.name(), regatta.isControlTrackingFromStartAndFinishTimes());
        dbRegatta.put(FieldNames.REGATTA_AUTO_RESTART_TRACKING_UPON_COMPETITOR_SET_CHANGE.name(), regatta.isAutoRestartTrackingUponCompetitorSetChange());
        dbRegatta.put(FieldNames.REGATTA_CAN_BOATS_OF_COMPETITORS_CHANGE_PER_RACE.name(), regatta.canBoatsOfCompetitorsChangePerRace());
        dbRegatta.put(FieldNames.REGATTA_COMPETITOR_REGISTRATION_TYPE.name(), regatta.getCompetitorRegistrationType().name());
        dbRegatta.put(FieldNames.REGATTA_REGISTRATION_LINK_SECRET.name(), regatta.getRegistrationLinkSecret());
        dbRegatta.put(FieldNames.REGATTA_RANKING_METRIC.name(), storeRankingMetric(regatta));
        boolean success = false;
        final int MAX_TRIES = 3;
        for (int i=0; i<MAX_TRIES && !success; i++) {
            try {
                regattasCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbRegatta, new ReplaceOptions().upsert(true));
                success = true;
            } catch (DuplicateKeyException e) {
                if (i+1==MAX_TRIES) {
                    throw e;
                }
            }
        }
    }

    private Document storeRankingMetric(Regatta regatta) {
        Document rankingMetricJson = new Document();
        final String rankingMetricTypeName = regatta.getRankingMetricType().name();
        rankingMetricJson.put(FieldNames.REGATTA_RANKING_METRIC_TYPE.name(), rankingMetricTypeName);
        return rankingMetricJson;
    }

    @Override
    public void removeRegatta(Regatta regatta) {
        MongoCollection<Document> regattasCollection = database.getCollection(CollectionNames.REGATTAS.name());
        Document query = new Document(FieldNames.REGATTA_NAME.name(), regatta.getName());
        regattasCollection.deleteOne(query);
    }

    private BasicDBList storeSeries(Iterable<? extends Series> series) {
        BasicDBList dbSeries = new BasicDBList();
        for (Series s : series) {
            dbSeries.add(storeSeries(s));
        }
        return dbSeries;
    }

    private Document storeSeries(Series s) {
        Document dbSeries = new Document();
        dbSeries.put(FieldNames.SERIES_NAME.name(), s.getName());
        dbSeries.put(FieldNames.SERIES_IS_MEDAL.name(), s.isMedal());
        dbSeries.put(FieldNames.SERIES_IS_FLEETS_CAN_RUN_IN_PARALLEL.name(), s.isFleetsCanRunInParallel());
        dbSeries.put(FieldNames.SERIES_MAXIMUM_NUMBER_OF_DISCARDS.name(), s.getMaximumNumberOfDiscards());
        dbSeries.put(FieldNames.SERIES_HAS_SPLIT_FLEET_CONTIGUOUS_SCORING.name(), s.hasSplitFleetContiguousScoring());
        dbSeries.put(FieldNames.SERIES_HAS_CROSS_FLEET_MERGED_RANKING.name(), s.hasCrossFleetMergedRanking());
        dbSeries.put(FieldNames.SERIES_STARTS_WITH_ZERO_SCORE.name(), s.isStartsWithZeroScore());
        dbSeries.put(FieldNames.SERIES_STARTS_WITH_NON_DISCARDABLE_CARRY_FORWARD.name(), s.isFirstColumnNonDiscardableCarryForward());
        dbSeries.put(FieldNames.SERIES_ONE_ALWAYS_STAYS_ONE.name(), s.isOneAlwaysStaysOne());
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

    private Document storeFleet(Fleet fleet) {
        Document dbFleet = new Document(FieldNames.FLEET_NAME.name(), fleet.getName());
        if (fleet instanceof FleetImpl) {
            dbFleet.put(FieldNames.FLEET_ORDERING.name(), ((FleetImpl) fleet).getOrdering());
            if(fleet.getColor() != null) {
                com.sap.sse.common.Util.Triple<Integer, Integer, Integer> colorAsRGB = fleet.getColor().getAsRGB();
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
        MongoCollection<Document> regattaForRaceIDCollection = database.getCollection(CollectionNames.REGATTA_FOR_RACE_ID.name());
        Document query = new Document(FieldNames.RACE_ID_AS_STRING.name(), raceIDAsString);
        Document entry = new Document(FieldNames.RACE_ID_AS_STRING.name(), raceIDAsString);
        entry.put(FieldNames.REGATTA_NAME.name(), regatta.getName());
        regattaForRaceIDCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry, new ReplaceOptions().upsert(true));
    }

    @Override
    public void removeRegattaForRaceID(String raceIDAsString, Regatta regatta) {
        MongoCollection<Document> regattaForRaceIDCollection = database.getCollection(CollectionNames.REGATTA_FOR_RACE_ID.name());
        Document query = new Document(FieldNames.RACE_ID_AS_STRING.name(), raceIDAsString);
        regattaForRaceIDCollection.deleteOne(query);
    }

    public MongoCollection<Document> getRaceLogCollection() {
        MongoCollection<Document> result = database.getCollection(CollectionNames.RACE_LOGS.name());
        result.createIndex(new Document(FieldNames.RACE_LOG_IDENTIFIER.name(), 1));
        return result;
    }

    private void storeRaceLogEventAuthor(Document dbObject, AbstractLogEventAuthor author) {
        if (author != null) {
            dbObject.put(FieldNames.RACE_LOG_EVENT_AUTHOR_NAME.name(), author.getName());
            dbObject.put(FieldNames.RACE_LOG_EVENT_AUTHOR_PRIORITY.name(), author.getPriority());
        }
    }

    private void storeRegattaLogEventAuthor(Document dbObject, AbstractLogEventAuthor author) {
        if (author != null) {
            dbObject.put(FieldNames.REGATTA_LOG_EVENT_AUTHOR_NAME.name(), author.getName());
            dbObject.put(FieldNames.REGATTA_LOG_EVENT_AUTHOR_PRIORITY.name(), author.getPriority());
        }
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogFlagEvent flagEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogFlagEvent(flagEvent));
        return result;
    }

    private void storeRaceLogIdentifier(SimpleRaceLogIdentifier raceLogIdentifier, Document result) {
        result.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceLogIdentifier.getIdentifier()));
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogStartTimeEvent startTimeEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogStartTimeEvent(startTimeEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogPassChangeEvent passChangeEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogPassChangeEvent(passChangeEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogRaceStatusEvent raceStatusEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogRaceStatusEvent(raceStatusEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogCourseDesignChangedEvent courseDesignChangedEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogCourseDesignChangedEvent(courseDesignChangedEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogFinishPositioningListChangedEvent finishPositioningListChangedEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogFinishPositioningListChangedEvent(finishPositioningListChangedEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogFinishPositioningConfirmedEvent finishPositioningConfirmedEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogFinishPositioningConfirmedEvent(finishPositioningConfirmedEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogPathfinderEvent pathfinderEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogPathfinderEvent(pathfinderEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogGateLineOpeningTimeEvent gateLineOpeningTimeEvent) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogGateLineOpeningTimeEvent(gateLineOpeningTimeEvent));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogStartProcedureChangedEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogStartProcedureChangedEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogProtestStartTimeEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogProtestStartTimeEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogWindFixEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogWindFix(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogResultsAreOfficialEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogResultsAreOfficialEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogExcludeWindSourcesEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogExcludeWindSourceEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogDenoteForTrackingEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogDenoteForTrackingEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogStartTrackingEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogStartTrackingEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogRevokeEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogRevokeEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogRegisterCompetitorEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogRegisterCompetitorEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogEndOfTrackingEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogEndOfTrackingEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogStartOfTrackingEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogStartOfTrackingEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogAdditionalScoringInformationEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeAdditionalScoringInformation(event));
        return result;
    }

    private Object storeAdditionalScoringInformation(RaceLogAdditionalScoringInformationEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogAdditionalScoringInformationEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_ADDITIONAL_SCORING_INFORMATION_TYPE.name(), event.getType().name());
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogFixedMarkPassingEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogFixedMarkPassingEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogSuppressedMarkPassingsEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogSuppressedMarkPassingsEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogORCLegDataEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeORCLegDataEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogORCCertificateAssignmentEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeORCCertificateAssignmentEvent(event));
        return result;
    }

    private Document storeORCCertificateAssignmentEvent(RaceLogORCCertificateAssignmentEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogORCCertificateAssignmentEvent.class.getSimpleName());
        result.append(FieldNames.RACE_LOG_BOAT_ID.name(), event.getBoatId());
        result.append(FieldNames.ORC_CERTIFICATE.name(), createORCCertificateObject(event.getCertificate()));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogORCScratchBoatEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeORCScratchBoatEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogORCImpliedWindSourceEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeORCImpliedWindSourceEvent(event));
        return result;
    }

    private Document storeORCImpliedWindSourceEvent(RaceLogORCImpliedWindSourceEvent event) {
        final Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogORCImpliedWindSourceEvent.class.getSimpleName());
        final Document impliedWindSourceDocument;
        if (event.getImpliedWindSource() == null) {
            impliedWindSourceDocument = null;
        } else {
            final JSONObject jsonSerializedImpliedWindSource = new ImpliedWindSourceSerializer().serialize(event.getImpliedWindSource());
            impliedWindSourceDocument = Document.parse(jsonSerializedImpliedWindSource.toString());
        }
        result.put(FieldNames.ORC_IMPLIED_WIND_SOURCE.name(), impliedWindSourceDocument);
        return result;
    }

    private Document storeORCScratchBoatEvent(RaceLogORCScratchBoatEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogORCScratchBoatEvent.class.getSimpleName());
        return result;
    }

    private Document storeORCLegDataEvent(RaceLogORCLegDataEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogORCLegDataEvent.class.getSimpleName());
        result.put(FieldNames.ORC_LEG_NR.name(), event.getOneBasedLegNumber());
        result.put(FieldNames.ORC_LEG_LENGTH_IN_NAUTICAL_MILES.name(), event.getLength()==null?null:event.getLength().getNauticalMiles());
        result.put(FieldNames.ORC_LEG_TWA_IN_DEG.name(), event.getTwa()==null?null:event.getTwa().getDegrees());
        result.put(FieldNames.ORC_LEG_TYPE.name(), event.getType().name());
        return result;
    }

    private Object storeRaceLogWindFix(RaceLogWindFixEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogWindFixEvent.class.getSimpleName());
        result.put(FieldNames.WIND.name(), storeWind(event.getWindFix()));
        result.put(FieldNames.IS_MAGNETIC.name(), event.isMagnetic());
        return result;
    }

    private Object storeRaceLogProtestStartTimeEvent(RaceLogProtestStartTimeEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogProtestStartTimeEvent.class.getSimpleName());
        storeTimePoint(event.getProtestTime().from(), result, FieldNames.RACE_LOG_PROTEST_START_TIME);
        storeTimePoint(event.getProtestTime().to(), result, FieldNames.RACE_LOG_PROTEST_END_TIME);
        return result;
    }

    private Object storeRaceLogEndOfTrackingEvent(RaceLogEndOfTrackingEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogEndOfTrackingEvent.class.getSimpleName());
        return result;
    }

    private Object storeRaceLogStartOfTrackingEvent(RaceLogStartOfTrackingEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogStartOfTrackingEvent.class.getSimpleName());
        return result;
    }

    private Object storeRaceLogStartProcedureChangedEvent(RaceLogStartProcedureChangedEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogStartProcedureChangedEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_START_PROCEDURE_TYPE.name(), event.getStartProcedureType().name());
        return result;
    }

    private Object storeRaceLogPathfinderEvent(RaceLogPathfinderEvent pathfinderEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(pathfinderEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogPathfinderEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_PATHFINDER_ID.name(), pathfinderEvent.getPathfinderId());
        return result;
    }

    private void storeDeviceMappingEvent(RegattaLogDeviceMappingEvent<?> event, Document result, FieldNames fromField, FieldNames toField) {
        try {
            result.put(FieldNames.DEVICE_ID.name(), storeDeviceId(deviceIdentifierServiceFinder, event.getDevice()));
        } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not store device identifier for mappng event", e);
        }
        if (event.getFrom() != null) {
            storeTimePoint(event.getFrom(), result, fromField);
        }
        if (event.getToInclusive() != null) {
            storeTimePoint(event.getToInclusive(), result, toField);
        }
    }

    private Object storeRaceLogResultsAreOfficialEvent(RaceLogResultsAreOfficialEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogResultsAreOfficialEvent.class.getSimpleName());
        // currently there are no further properties; the event speaks for itself
        return result;
    }

    private Object storeRaceLogExcludeWindSourceEvent(RaceLogExcludeWindSourcesEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogExcludeWindSourcesEvent.class.getSimpleName());
        final BasicDBList dbWindSourcesToExclude = new BasicDBList();
        for (final WindSource windSourceToExclude : event.getWindSourcesToExclude()) {
            final Document dbWindSourceToExclude = new Document();
            dbWindSourceToExclude.put(FieldNames.WIND_SOURCE_NAME.name(), windSourceToExclude.name());
            if (windSourceToExclude.getId() != null) {
                dbWindSourceToExclude.put(FieldNames.WIND_SOURCE_ID.name(), windSourceToExclude.getId());
            }
            dbWindSourcesToExclude.add(dbWindSourceToExclude);
        }
        result.put(FieldNames.WIND_SOURCES_TO_EXCLUDE.name(), dbWindSourcesToExclude);
        return result;
    }

    private Object storeRaceLogDenoteForTrackingEvent(RaceLogDenoteForTrackingEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogDenoteForTrackingEvent.class.getSimpleName());
        result.put(FieldNames.RACE_NAME.name(), event.getRaceName());
        result.put(FieldNames.BOAT_CLASS_NAME.name(), event.getBoatClass()==null?null:event.getBoatClass().getName());
        result.put(FieldNames.RACE_ID.name(), event.getRaceId());
        return result;
    }

    private Object storeRaceLogStartTrackingEvent(RaceLogStartTrackingEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogStartTrackingEvent.class.getSimpleName());
        return result;
    }

    private Object storeRaceLogRevokeEvent(RaceLogRevokeEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogRevokeEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_REVOKED_EVENT_ID.name(), event.getRevokedEventId());
        result.put(FieldNames.RACE_LOG_REVOKED_EVENT_TYPE.name(), event.getRevokedEventType());
        result.put(FieldNames.RACE_LOG_REVOKED_EVENT_SHORT_INFO.name(), event.getRevokedEventShortInfo());
        result.put(FieldNames.RACE_LOG_REVOKED_REASON.name(), event.getReason());
        return result;
    }

    private Object storeRaceLogRegisterCompetitorEvent(RaceLogRegisterCompetitorEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogRegisterCompetitorEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_COMPETITOR_ID.name(), event.getCompetitor().getId());
        result.put(FieldNames.RACE_LOG_BOAT_ID.name(), event.getBoat().getId());
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogUseCompetitorsFromRaceLogEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogUseCompetitorsFromRaceLogEvent(event));
        return result;
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogTagEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogTagEvent(event));
        return result;
    }

    public Document storeRaceLogTagEvent(RaceLogTagEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogTagEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_TAG.name(), event.getTag());
        result.put(FieldNames.RACE_LOG_COMMENT.name(), event.getComment());
        result.put(FieldNames.RACE_LOG_HIDDEN_INFO.name(), event.getHiddenInfo());
        result.put(FieldNames.RACE_LOG_IMAGE_URL.name(), event.getImageURL());
        result.put(FieldNames.RACE_LOG_RESIZED_IMAGE_URL.name(), event.getResizedImageURL());
        return result;
    }

    public Document storeRaceLogUseCompetitorsFromRaceLogEvent(RaceLogUseCompetitorsFromRaceLogEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogUseCompetitorsFromRaceLogEvent.class.getSimpleName());
        return result;
    }

    private Object storeRaceLogFixedMarkPassingEvent(RaceLogFixedMarkPassingEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFixedMarkPassingEvent.class.getSimpleName());
        result.put(FieldNames.INDEX_OF_PASSED_WAYPOINT.name(), event.getZeroBasedIndexOfPassedWaypoint());
        result.put(FieldNames.TIMEPOINT_OF_FIXED_MARKPASSING.name(), event.getTimePointOfFixedPassing().asMillis());
        return result;
    }

    private Object storeRaceLogSuppressedMarkPassingsEvent(RaceLogSuppressedMarkPassingsEvent event) {
        Document result = new Document();
        storeRaceLogEventProperties(event, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogSuppressedMarkPassingsEvent.class.getSimpleName());
        result.put(FieldNames.INDEX_OF_FIRST_SUPPRESSED_WAYPOINT.name(), event.getZeroBasedIndexOfFirstSuppressedWaypoint());
        return result;
    }

    public Document storeRaceLogFlagEvent(RaceLogFlagEvent flagEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(flagEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFlagEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_EVENT_FLAG_UPPER.name(), flagEvent.getUpperFlag()==null?Flags.NONE.name():flagEvent.getUpperFlag().name());
        result.put(FieldNames.RACE_LOG_EVENT_FLAG_LOWER.name(), flagEvent.getLowerFlag()==null?Flags.NONE.name():flagEvent.getLowerFlag().name());
        result.put(FieldNames.RACE_LOG_EVENT_FLAG_DISPLAYED.name(), String.valueOf(flagEvent.isDisplayed()));
        return result;
    }

    private Document storeRaceLogStartTimeEvent(RaceLogStartTimeEvent startTimeEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(startTimeEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogStartTimeEvent.class.getSimpleName());
        storeTimePoint(startTimeEvent.getStartTime(), result, FieldNames.RACE_LOG_EVENT_START_TIME);
        storeCourseAreaToStartTimeEvent(startTimeEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name(), startTimeEvent.getNextStatus().name());
        return result;
    }

    private void storeCourseAreaToStartTimeEvent(RaceLogCourseAreaSpecification startTimeEvent, Document result) {
        result.put(FieldNames.RACE_LOG_EVENT_COURSE_AREA_ID_AS_STRING.name(),
                startTimeEvent.getCourseAreaId()==null?null:startTimeEvent.getCourseAreaId().toString());
    }

    private void storeRaceLogEventProperties(RaceLogEvent event, Document result) {
        // for compatibility reasons we reuse the field name of Timed
        storeTimePoint(event.getLogicalTimePoint(), result, FieldNames.TIME_AS_MILLIS);
        storeTimePoint(event.getCreatedAt(), result, FieldNames.RACE_LOG_EVENT_CREATED_AT);
        result.put(FieldNames.RACE_LOG_EVENT_ID.name(), event.getId());
        result.put(FieldNames.RACE_LOG_EVENT_PASS_ID.name(), event.getPassId());
        result.put(FieldNames.RACE_LOG_EVENT_INVOLVED_BOATS.name(), storeInvolvedCompetitorsForRaceLogEvent(event.getInvolvedCompetitors()));
        storeRaceLogEventAuthor(result, event.getAuthor());
    }


    private BasicDBList storeInvolvedCompetitorsForRaceLogEvent(List<Competitor> competitors) {
        BasicDBList dbInvolvedCompetitorIds = new BasicDBList();
        for (Competitor competitor : competitors) {
            dbInvolvedCompetitorIds.add(competitor.getId());
        }
        return dbInvolvedCompetitorIds;
    }

    private Document storeRaceLogPassChangeEvent(RaceLogPassChangeEvent passChangeEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(passChangeEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogPassChangeEvent.class.getSimpleName());
        return result;
    }

    private Document storeRaceLogDependentStartTimeEvent(RaceLogDependentStartTimeEvent dependentStartTimeEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(dependentStartTimeEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogDependentStartTimeEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_DEPDENDENT_ON_REGATTALIKE.name(), dependentStartTimeEvent.getDependentOnRaceIdentifier().getRegattaLikeParentName());
        result.put(FieldNames.RACE_LOG_DEPDENDENT_ON_RACECOLUMN.name(), dependentStartTimeEvent.getDependentOnRaceIdentifier().getRaceColumnName());
        result.put(FieldNames.RACE_LOG_DEPDENDENT_ON_FLEET.name(), dependentStartTimeEvent.getDependentOnRaceIdentifier().getFleetName());
        storeDuration(dependentStartTimeEvent.getStartTimeDifference(), result, FieldNames.RACE_LOG_START_TIME_DIFFERENCE_IN_MS);
        storeCourseAreaToStartTimeEvent(dependentStartTimeEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name(), dependentStartTimeEvent.getNextStatus().name());
        return result;
    }

    private void storeDuration(Duration duration, Document result, FieldNames fieldName) {
        if (duration != null) {
            result.put(fieldName.name(), duration.asMillis());
        }
    }

    private Document storeRaceLogRaceStatusEvent(RaceLogRaceStatusEvent raceStatusEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(raceStatusEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogRaceStatusEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name(), raceStatusEvent.getNextStatus().name());
        return result;
    }

    private Document storeRaceLogCourseDesignChangedEvent(RaceLogCourseDesignChangedEvent courseDesignChangedEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(courseDesignChangedEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogCourseDesignChangedEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_COURSE_DESIGN_NAME.name(), courseDesignChangedEvent.getCourseDesign().getName());
        result.put(FieldNames.RACE_LOG_COURSE_ORIGINATING_TEMPLATE_ID.name(),
                courseDesignChangedEvent.getCourseDesign().getOriginatingCourseTemplateIdOrNull());
        result.put(FieldNames.RACE_LOG_COURSE_DESIGNER_MODE.name(),
                courseDesignChangedEvent.getCourseDesignerMode() == null ? null : courseDesignChangedEvent.getCourseDesignerMode().name());
        result.put(FieldNames.RACE_LOG_COURSE_DESIGN.name(), storeCourseBase(courseDesignChangedEvent.getCourseDesign()));

        final BasicDBList roleMap = new BasicDBList();
        courseDesignChangedEvent.getCourseDesign().getAssociatedRoles().forEach((mark, role) -> {
            final Document mapping = new Document();
            mapping.put(FieldNames.RACE_LOG_COURSE_ASSOCIATED_ROLES_MARK_ID.name(), mark.getId().toString());
            mapping.put(FieldNames.RACE_LOG_COURSE_ASSOCIATED_ROLES_ROLE_ID.name(), role.toString());
            roleMap.add(mapping);
        });

        if (!roleMap.isEmpty()) {
            result.put(FieldNames.RACE_LOG_COURSE_ASSOCIATED_ROLES.name(), roleMap);
        }
        return result;
    }

    private Object storeRaceLogFinishPositioningListChangedEvent(RaceLogFinishPositioningListChangedEvent finishPositioningListChangedEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(finishPositioningListChangedEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFinishPositioningListChangedEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_POSITIONED_COMPETITORS.name(), storePositionedCompetitors(finishPositioningListChangedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons()));

        return result;
    }

    private Object storeRaceLogFinishPositioningConfirmedEvent(RaceLogFinishPositioningConfirmedEvent finishPositioningConfirmedEvent) {
        Document result = new Document();
        storeRaceLogEventProperties(finishPositioningConfirmedEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFinishPositioningConfirmedEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_POSITIONED_COMPETITORS.name(), storePositionedCompetitors(finishPositioningConfirmedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons()));

        return result;
    }

    private Object storeRaceLogGateLineOpeningTimeEvent(RaceLogGateLineOpeningTimeEvent gateLineOpeningTimeEvent){
        Document result = new Document();
        storeRaceLogEventProperties(gateLineOpeningTimeEvent, result);
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogGateLineOpeningTimeEvent.class.getSimpleName());
        result.put(FieldNames.RACE_LOG_GATE_LINE_OPENING_TIME.name(), gateLineOpeningTimeEvent.getGateLineOpeningTimes().getGateLaunchStopTime());
        result.put(FieldNames.RACE_LOG_GOLF_DOWN_TIME.name(), gateLineOpeningTimeEvent.getGateLineOpeningTimes().getGolfDownTime());
        return result;
    }

    private BasicDBList storePositionedCompetitors(CompetitorResults positionedCompetitors) {
        BasicDBList dbList = new BasicDBList();
        if (positionedCompetitors != null) {
            for (CompetitorResult competitorPair : positionedCompetitors) {
                dbList.add(storePositionedCompetitor(competitorPair));
            }
        }
        return dbList;
    }

    private Document storePositionedCompetitor(CompetitorResult competitorResult) {
        Document result = new Document();
        result.put(FieldNames.COMPETITOR_ID.name(), competitorResult.getCompetitorId());
        result.put(FieldNames.COMPETITOR_DISPLAY_NAME.name(), competitorResult.getName());
        result.put(FieldNames.COMPETITOR_SHORT_NAME.name(), competitorResult.getShortName());
        result.put(FieldNames.COMPETITOR_BOAT_NAME.name(), competitorResult.getBoatName());
        result.put(FieldNames.COMPETITOR_BOAT_SAIL_ID.name(), competitorResult.getBoatSailId());
        result.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name(), competitorResult.getMaxPointsReason() == null ? null : competitorResult.getMaxPointsReason().name());
        result.put(FieldNames.LEADERBOARD_CORRECTED_SCORE.name(), competitorResult.getScore());
        result.put(FieldNames.RACE_LOG_FINISHING_TIME_AS_MILLIS.name(), competitorResult.getFinishingTime() == null ? null : competitorResult.getFinishingTime().asMillis());
        result.put(FieldNames.LEADERBOARD_RANK.name(), competitorResult.getOneBasedRank());
        result.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_COMMENT.name(), competitorResult.getComment());
        result.put(FieldNames.LEADERBOARD_SCORE_CORRECTION_MERGE_STATE.name(), competitorResult.getMergeState().name());
        return result;
    }

    private BasicDBList storeCourseBase(CourseBase courseData) {
        BasicDBList dbList = new BasicDBList();

        for (Waypoint waypoint : courseData.getWaypoints()) {
            dbList.add(storeWaypoint(waypoint));
        }
        return dbList;
    }

    private Document storeWaypoint(Waypoint waypoint) {
        Document result = new Document();
        result.put(FieldNames.WAYPOINT_PASSINGINSTRUCTIONS.name(), getPassingInstructions(waypoint.getPassingInstructions()));
        result.put(FieldNames.CONTROLPOINT.name(), storeControlPoint(waypoint.getControlPoint()));
        return result;
    }

    private Document storeControlPoint(ControlPoint controlPoint) {
        Document result = new Document();
        if (controlPoint instanceof Mark) {
            result.put(FieldNames.CONTROLPOINT_CLASS.name(), Mark.class.getSimpleName());
            result.put(FieldNames.CONTROLPOINT_VALUE.name(), storeMark((Mark) controlPoint));
        } else if (controlPoint instanceof ControlPointWithTwoMarks) {
            result.put(FieldNames.CONTROLPOINT_CLASS.name(), ControlPointWithTwoMarks.class.getSimpleName());
            result.put(FieldNames.CONTROLPOINT_VALUE.name(), storeControlPointWithTwoMarks((ControlPointWithTwoMarks) controlPoint));
        }
        return result;
    }

    private Document storeControlPointWithTwoMarks(ControlPointWithTwoMarks cpwtm) {
        Document result = new Document();
        result.put(FieldNames.CONTROLPOINTWITHTWOMARKS_ID.name(), cpwtm.getId());
        result.put(FieldNames.CONTROLPOINTWITHTWOMARKS_NAME.name(), cpwtm.getName());
        result.put(FieldNames.CONTROLPOINTWITHTWOMARKS_SHORT_NAME.name(), cpwtm.getShortName());
        result.put(FieldNames.CONTROLPOINTWITHTWOMARKS_LEFT.name(), storeMark(cpwtm.getLeft()));
        result.put(FieldNames.CONTROLPOINTWITHTWOMARKS_RIGHT.name(), storeMark(cpwtm.getRight()));
        return result;
    }

    private Document storeMark(Mark mark) {
        Document result = new Document();
        result.put(FieldNames.MARK_ID.name(), mark.getId());
        result.put(FieldNames.MARK_COLOR.name(), mark.getColor()==null?null:mark.getColor().getAsHtml());
        result.put(FieldNames.MARK_NAME.name(), mark.getName());
        result.put(FieldNames.MARK_SHORT_NAME.name(), mark.getShortName());
        result.put(FieldNames.MARK_PATTERN.name(), mark.getPattern());
        result.put(FieldNames.MARK_SHAPE.name(), mark.getShape());
        result.put(FieldNames.MARK_TYPE.name(), mark.getType() == null ? null : mark.getType().name());
        result.put(FieldNames.MARK_ORIGINATING_MARK_TEMPLATE_ID.name(), mark.getOriginatingMarkTemplateIdOrNull());
        result.put(FieldNames.MARK_ORIGINATING_MARK_PROPERTIES_ID.name(), mark.getOriginatingMarkPropertiesIdOrNull());
        return result;
    }

    @Override
    public void storeCompetitor(Competitor competitor) {
        if (competitor.hasBoat()) {
            storeCompetitorWithBoat((CompetitorWithBoat) competitor);
            storeBoat(((CompetitorWithBoat) competitor).getBoat());
        } else {
            storeCompetitorWithoutBoat(competitor);
        }
    }

    private void storeCompetitorWithoutBoat(Competitor competitor) {
        MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS.name());
        JSONObject json = competitorSerializer.serialize(competitor);
        Document query = Document.parse(CompetitorJsonSerializer.getCompetitorIdQuery(competitor).toJSONString());
        Document entry = Document.parse(json.toJSONString());
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry, new ReplaceOptions().upsert(true));
    }

    private void storeCompetitorWithBoat(CompetitorWithBoat competitor) {
        MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS.name());
        JSONObject json = competitorWithBoatRefSerializer.serialize(competitor);
        Document query = Document.parse(CompetitorJsonSerializer.getCompetitorIdQuery(competitor).toString());
        Document entry = Document.parse(json.toString());
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry, new ReplaceOptions().upsert(true));
    }

    @Override
    public void storeCompetitors(Iterable<? extends Competitor> competitors) {
        if (competitors != null && !Util.isEmpty(competitors)) {
            List<Competitor> competitorsWithoutBoat = new ArrayList<>();
            List<CompetitorWithBoat> competitorsWithBoat = new ArrayList<>();
            for (Competitor competitor : competitors) {
                if (competitor.hasBoat()) {
                    competitorsWithBoat.add((CompetitorWithBoat) competitor);
                } else {
                    competitorsWithoutBoat.add(competitor);
                }
            }
            storeCompetitorsWithBoat(competitorsWithBoat);
            storeCompetitorsWithoutBoat(competitorsWithoutBoat);
        }
    }

    private void storeCompetitorsWithoutBoat(Iterable<Competitor> competitors) {
        if (!Util.isEmpty(competitors)) {
            MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS.name());
            List<Document> competitorsDB = new ArrayList<>();
            for (Competitor competitor : competitors) {
                JSONObject json = competitorSerializer.serialize(competitor);
                Document entry = Document.parse(json.toString());
                competitorsDB.add(entry);
            }
            collection.insertMany(competitorsDB);
        }
    }

    private void storeCompetitorsWithBoat(Iterable<CompetitorWithBoat> competitors) {
        if (!Util.isEmpty(competitors)) {
            MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS.name());
            List<Document> competitorsDB = new ArrayList<>();
            for (CompetitorWithBoat competitor : competitors) {
                JSONObject json = competitorWithBoatRefSerializer.serialize(competitor);
                Document entry = Document.parse(json.toString());
                competitorsDB.add(entry);
            }
            collection.insertMany(competitorsDB);
        }
    }

    @Override
    public void removeAllCompetitors() {
        logger.info("Removing all persistent competitors");
        MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS.name());
        collection.drop();
    }

    @Override
    public void removeCompetitor(Competitor competitor) {
        logger.info("Removing persistent competitor info for competitor "+competitor.getName()+" with ID "+competitor.getId());
        MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS.name());
        Document query = Document.parse(CompetitorJsonSerializer.getCompetitorIdQuery(competitor).toString());
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).deleteOne(query);
    }

    @Override
    public void storeBoat(Boat boat) {
        MongoCollection<Document> collection = database.getCollection(CollectionNames.BOATS.name());
        JSONObject json = boatSerializer.serialize(boat);
        Document query = Document.parse(BoatJsonSerializer.getBoatIdQuery(boat).toString());
        Document entry = Document.parse(json.toString());
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry, new ReplaceOptions().upsert(true));
    }

    @Override
    public void storeBoats(Iterable<? extends Boat> boats) {
        if (boats != null && !Util.isEmpty(boats)) {
            MongoCollection<Document> collection = database.getCollection(CollectionNames.BOATS.name());
            List<Document> boatsDB = new ArrayList<>();
            for (Boat boat : boats) {
                JSONObject json = boatSerializer.serialize(boat);
                Document entry = Document.parse(json.toString());
                boatsDB.add(entry);
            }
            collection.insertMany(boatsDB);
        }
    }

    @Override
    public void removeAllBoats() {
        logger.info("Removing all persistent boats");
        MongoCollection<Document> collection = database.getCollection(CollectionNames.BOATS.name());
        collection.drop();
    }

    @Override
    public void removeBoat(Boat boat) {
        logger.info("Removing persistent boat "+boat.getName()+" with ID "+boat.getId());
        MongoCollection<Document> collection = database.getCollection(CollectionNames.BOATS.name());
        Document query = Document.parse(BoatJsonSerializer.getBoatIdQuery(boat).toString());
        collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).deleteOne(query);
    }

    /**
     * Lets the {@link DeviceConfigurationJsonSerializer} create a JSON-serialized copy of the {@code configuration} and
     * adds as a key field the stringified configuration's UUID as a field named
     * {@link FieldNames#CONFIGURATION_ID_AS_STRING}. This field will be redundant to the serializer's copy of the ID field.
     */
    @Override
    public void storeDeviceConfiguration(DeviceConfiguration configuration) {
        MongoCollection<Document> configurationsCollections = database.getCollection(CollectionNames.CONFIGURATIONS.name());
        final Document configDocument = createDeviceConfigurationObject(configuration);
        configDocument.put(FieldNames.CONFIGURATION_ID_AS_STRING.name(), configuration.getId().toString());
        Document query = new Document();
        query.put(FieldNames.CONFIGURATION_ID_AS_STRING.name(), configuration.getId().toString());
        configurationsCollections.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, configDocument, new ReplaceOptions().upsert(true));
    }

    private Document createDeviceConfigurationObject(DeviceConfiguration configuration) {
        JsonSerializer<DeviceConfiguration> serializer = DeviceConfigurationJsonSerializer.create();
        JSONObject json = serializer.serialize(configuration);
        Document entry = Document.parse(json.toString());
        return entry;
    }

    @Override
    public void removeDeviceConfiguration(UUID id) {
        MongoCollection<Document> configurationsCollections = database.getCollection(CollectionNames.CONFIGURATIONS.name());
        Document query = new Document(FieldNames.CONFIGURATION_ID_AS_STRING.name(), id.toString());
        configurationsCollections.deleteOne(query);
    }

    void storeRaceLogEventEvent(Document eventEntry) {
        getRaceLogCollection().insertOne(eventEntry);
    }

    @Override
    public void removeRaceLog(RaceLogIdentifier identifier) {
        Document query = new Document();
        storeRaceLogIdentifier(identifier, query);
        getRaceLogCollection().deleteMany(query);
    }

    @Override
    public void removeAllRaceLogs() {
        getRaceLogCollection().drop();;
    }

    @Override
    public void removeRegattaLog(RegattaLikeIdentifier identifier) {
        Document query = new Document();
        addRegattaLikeIdentifier(identifier, query);
        getRegattaLogCollection().deleteOne(query);
    }

    @Override
    public void removeAllRegattaLogs() {
        getRegattaLogCollection().drop();
    }

    @Override
    public void storeResultUrl(String resultProviderName, URL url) {
        MongoCollection<Document> resultUrlsCollection = database.getCollection(CollectionNames.RESULT_URLS.name());
        Document query = new Document(FieldNames.RESULT_PROVIDERNAME.name(), resultProviderName);
        Document entry = new Document(FieldNames.RESULT_PROVIDERNAME.name(), resultProviderName);
        entry.put(FieldNames.RESULT_URL.name(), url.toString());
        resultUrlsCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, entry, new ReplaceOptions().upsert(true));
    }

    @Override
    public void removeResultUrl(String resultProviderName, URL url) {
        MongoCollection<Document> resultUrlsCollection = database.getCollection(CollectionNames.RESULT_URLS.name());
        Document query = new Document().append(FieldNames.RESULT_PROVIDERNAME.name(), resultProviderName)
                .append(FieldNames.RESULT_URL.name(), url.toString());
        resultUrlsCollection.deleteOne(query);
    }

    public MongoCollection<Document> getRegattaLogCollection() {
        MongoCollection<Document> result = database.getCollection(CollectionNames.REGATTA_LOGS.name());
        Document index = new Document(FieldNames.REGATTA_LOG_IDENTIFIER_TYPE.name(), 1);
        index.put(FieldNames.REGATTA_LOG_IDENTIFIER_NAME.name(), 1);
        result.createIndex(index, new IndexOptions().name("regattaLogById").background(false));
        return result;
    }

    private Document createBasicRegattaLogEventDBObject(RegattaLogEvent event) {
        Document result = new Document();
        storeTimePoint(event.getLogicalTimePoint(), result, FieldNames.TIME_AS_MILLIS);
        storeTimePoint(event.getCreatedAt(), result, FieldNames.REGATTA_LOG_EVENT_CREATED_AT);
        storeRegattaLogEventAuthor(result, event.getAuthor());
        result.put(FieldNames.REGATTA_LOG_EVENT_ID.name(), event.getId());
        return result;
    }

    private void addRegattaLikeIdentifier(RegattaLikeIdentifier regattaLikeId, Document toObject) {
        toObject.put(FieldNames.REGATTA_LOG_IDENTIFIER_TYPE.name(), regattaLikeId.getIdentifierType());
        toObject.put(FieldNames.REGATTA_LOG_IDENTIFIER_NAME.name(), regattaLikeId.getName());
    }

    private Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, Document innerObject) {
        Document result = new Document(FieldNames.REGATTA_LOG_EVENT.name(), innerObject);
        addRegattaLikeIdentifier(regattaLikeId, result);
        getRegattaLogCollection().insertOne(result);
        return result;
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogDeviceCompetitorMappingEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogDeviceCompetitorMappingEvent.class.getSimpleName());
        storeDeviceMappingEvent(event, result, FieldNames.REGATTA_LOG_FROM, FieldNames.REGATTA_LOG_TO);
        result.put(FieldNames.COMPETITOR_ID.name(), event.getMappedTo().getId());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogDeviceBoatMappingEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogDeviceBoatMappingEvent.class.getSimpleName());
        storeDeviceMappingEvent(event, result, FieldNames.REGATTA_LOG_FROM, FieldNames.REGATTA_LOG_TO);
        result.put(FieldNames.RACE_LOG_BOAT_ID.name(), event.getMappedTo().getId());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId,
            RegattaLogDeviceCompetitorSensorDataMappingEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), event.getClass().getSimpleName());
        storeDeviceMappingEvent(event, result, FieldNames.REGATTA_LOG_FROM, FieldNames.REGATTA_LOG_TO);
        result.put(FieldNames.COMPETITOR_ID.name(), event.getMappedTo().getId());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId,
            RegattaLogDeviceBoatSensorDataMappingEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), event.getClass().getSimpleName());
        storeDeviceMappingEvent(event, result, FieldNames.REGATTA_LOG_FROM, FieldNames.REGATTA_LOG_TO);
        result.put(FieldNames.RACE_LOG_BOAT_ID.name(), event.getMappedTo().getId());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogDeviceMarkMappingEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogDeviceMarkMappingEvent.class.getSimpleName());
        storeDeviceMappingEvent(event, result, FieldNames.REGATTA_LOG_FROM, FieldNames.REGATTA_LOG_TO);
        result.put(FieldNames.MARK.name(), storeMark(event.getMappedTo()));
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogRevokeEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogRevokeEvent.class.getSimpleName());
        result.put(FieldNames.REGATTA_LOG_REVOKED_EVENT_ID.name(), event.getRevokedEventId());
        result.put(FieldNames.REGATTA_LOG_REVOKED_EVENT_TYPE.name(), event.getRevokedEventType());
        result.put(FieldNames.REGATTA_LOG_REVOKED_EVENT_SHORT_INFO.name(), event.getRevokedEventShortInfo());
        result.put(FieldNames.REGATTA_LOG_REVOKED_REASON.name(), event.getReason());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogRegisterBoatEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogRegisterBoatEvent.class.getSimpleName());
        result.put(FieldNames.REGATTA_LOG_BOAT_ID.name(), event.getBoat().getId());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogRegisterCompetitorEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogRegisterCompetitorEvent.class.getSimpleName());
        result.put(FieldNames.REGATTA_LOG_COMPETITOR_ID.name(), event.getCompetitor().getId());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogCloseOpenEndedDeviceMappingEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogCloseOpenEndedDeviceMappingEvent.class.getSimpleName());
        result.put(FieldNames.REGATTA_LOG_DEVICE_MAPPING_EVENT_ID.name(), event.getDeviceMappingEventId());
        storeTimePoint(event.getClosingTimePointInclusive(), result, FieldNames.REGATTA_LOG_CLOSING_TIMEPOINT);
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogSetCompetitorTimeOnTimeFactorEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogSetCompetitorTimeOnTimeFactorEvent.class.getSimpleName());
        result.put(FieldNames.REGATTA_LOG_COMPETITOR_ID.name(), event.getCompetitor().getId());
        result.put(FieldNames.REGATTA_LOG_TIME_ON_TIME_FACTOR.name(), event.getTimeOnTimeFactor());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRaceLogEntry(RaceLogIdentifier raceLogIdentifier, RaceLogDependentStartTimeEvent event) {
        Document result = new Document();
        storeRaceLogIdentifier(raceLogIdentifier, result);
        result.put(FieldNames.RACE_LOG_EVENT.name(), storeRaceLogDependentStartTimeEvent(event));
        return result;
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeId, RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEvent.class.getSimpleName());
        result.put(FieldNames.REGATTA_LOG_COMPETITOR_ID.name(), event.getCompetitor().getId());
        result.put(FieldNames.REGATTA_LOG_TIME_ON_DISTANCE_SECONDS_ALLOWANCE_PER_NAUTICAL_MILE.name(), event.getTimeOnDistanceAllowancePerNauticalMile().asSeconds());
        return storeRegattaLogEvent(regattaLikeId, result);
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeIdentifier, RegattaLogDefineMarkEvent event) {
        Document result = createBasicRegattaLogEventDBObject(event);
        result.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogDefineMarkEvent.class.getSimpleName());
        result.put(FieldNames.REGATTA_LOG_MARK.name(), storeMark(event.getMark()));
        return storeRegattaLogEvent(regattaLikeIdentifier, result);
    }

    private Document createImageObject(ImageDescriptor image) {
        Document result = new Document();
        result.put(FieldNames.IMAGE_URL.name(), image.getURL().toString());
        result.put(FieldNames.IMAGE_LOCALE.name(), image.getLocale() != null ? image.getLocale().toLanguageTag() : null);
        result.put(FieldNames.IMAGE_TITLE.name(), image.getTitle());
        result.put(FieldNames.IMAGE_SUBTITLE.name(), image.getSubtitle());
        result.put(FieldNames.IMAGE_COPYRIGHT.name(), image.getCopyright());
        result.put(FieldNames.IMAGE_WIDTH_IN_PX.name(), image.getWidthInPx());
        result.put(FieldNames.IMAGE_HEIGHT_IN_PX.name(), image.getHeightInPx());
        storeTimePoint(image.getCreatedAtDate(), result, FieldNames.IMAGE_CREATEDATDATE);
        BasicDBList tags = new BasicDBList();
        for (String tag : image.getTags()) {
            tags.add(tag);
        }
        result.put(FieldNames.IMAGE_TAGS.name(), tags);
        return result;
    }

    private Document createVideoObject(VideoDescriptor video) {
        Document result = new Document();
        result.put(FieldNames.VIDEO_URL.name(), video.getURL().toString());
        result.put(FieldNames.VIDEO_LOCALE.name(), video.getLocale() != null ? video.getLocale().toLanguageTag() : null);
        result.put(FieldNames.VIDEO_THUMBNAIL_URL.name(), video.getThumbnailURL() != null ? video.getThumbnailURL().toString() : null);
        result.put(FieldNames.VIDEO_TITLE.name(), video.getTitle());
        result.put(FieldNames.VIDEO_SUBTITLE.name(), video.getSubtitle());
        result.put(FieldNames.VIDEO_MIMETYPE.name(), video.getMimeType() != null ? video.getMimeType().name() : null);
        result.put(FieldNames.VIDEO_COPYRIGHT.name(), video.getCopyright());
        result.put(FieldNames.VIDEO_LENGTH_IN_SECONDS.name(), video.getLengthInSeconds());
        storeTimePoint(video.getCreatedAtDate(), result, FieldNames.VIDEO_CREATEDATDATE);
        BasicDBList tags = new BasicDBList();
        for (String tag : video.getTags()) {
            tags.add(tag);
        }
        result.put(FieldNames.VIDEO_TAGS.name(), tags);
        return result;
    }

    private Document createSailorsInfoWebsiteObject(Locale locale, URL url) {
        Document result = new Document();
        result.put(FieldNames.SAILORS_INFO_URL.name(), url.toString());
        result.put(FieldNames.SAILORS_INFO_LOCALE.name(), locale != null ? locale.toLanguageTag() : null);
        return result;
    }

    @Override
    public void removeConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters params) throws MalformedURLException {
        if (raceTrackingConnectivityParamsServiceFinder != null) {
            final String typeIdentifier = params.getTypeIdentifier();
            final RaceTrackingConnectivityParametersHandler paramsPersistenceService = raceTrackingConnectivityParamsServiceFinder.findService(typeIdentifier);
            if (paramsPersistenceService != null) {
                final MongoCollection<Document> collection = database.getCollection(CollectionNames.CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED.name());
                Document key = new Document();
                key.putAll(paramsPersistenceService.getKey(params));
                final DeleteResult deleteResult = collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).deleteOne(key);
                if (deleteResult.getDeletedCount() != 1) {
                    logger.warning("Tried to delete connectivity params "+params+" from restore list but delete count was "+
                            deleteResult.getDeletedCount());
                }
            } else {
                logger.warning("Couldn't find a persistence service for connectivity parameters of type "+typeIdentifier);
            }
        } else {
            logger.warning("No connectivity parameters service finder set; unable to remove connectivity parameters from persistent store for "
                    + params + " of type " + params.getTypeIdentifier());
        }
    }

    @Override
    public void addConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters params) {
        final String typeIdentifier = params.getTypeIdentifier();
        if (raceTrackingConnectivityParamsServiceFinder == null) {
            logger.warning("No service finder has been configured to find connectivity parameter persistence services. Can't add connectivity parameters to DB for restore.");
        } else {
            try {
                final RaceTrackingConnectivityParametersHandler paramsPersistenceService = raceTrackingConnectivityParamsServiceFinder.findService(typeIdentifier);
                final MongoCollection<Document> collection = database.getCollection(CollectionNames.CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED.name());
                Document key = new Document();
                key.putAll(paramsPersistenceService.getKey(params));
                Document dbObject = new Document();
                dbObject.putAll(paramsPersistenceService.mapFrom(params));
                collection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(key, dbObject, new ReplaceOptions().upsert(true));
            } catch (NoCorrespondingServiceRegisteredException e) {
                logger.log(Level.WARNING, "Couldn't find a persistence service for connectivity parameters of type "+typeIdentifier+
                        ". Couldn't store race "+params.getTrackerID()+" for restoring.", e);
            } catch (MalformedURLException e) {
                logger.log(Level.WARNING, "Issue with reading a URL from the tracking params for tracker "+params.getTrackerID()+" for restoring.", e);
            }
        }
    }

    @Override
    public void removeAllConnectivityParametersForRacesToRestore() {
        database.getCollection(CollectionNames.CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED.name()).drop();
    }

    @Override
    public void storeAnniversaryData(
            ConcurrentHashMap<Integer, Pair<DetailedRaceInfo, AnniversaryType>> knownAnniversaries) {
        try {
            MongoCollection<Document> anniversarysStored = database.getCollection(CollectionNames.ANNIVERSARIES.name());
            for (Entry<Integer, Pair<DetailedRaceInfo, AnniversaryType>> anniversary : knownAnniversaries.entrySet()) {
                Document currentProxy = new Document(FieldNames.ANNIVERSARY_NUMBER.name(),
                        anniversary.getKey().intValue());
                Document newValue = new Document(FieldNames.ANNIVERSARY_NUMBER.name(),
                        anniversary.getKey().intValue());
                DetailedRaceInfo raceInfo = anniversary.getValue().getA();
                newValue.append(FieldNames.RACE_NAME.name(), raceInfo.getIdentifier().getRaceName());
                newValue.append(FieldNames.REGATTA_NAME.name(), raceInfo.getIdentifier().getRegattaName());
                newValue.append(FieldNames.LEADERBOARD_NAME.name(), raceInfo.getLeaderboardName());
                newValue.append(FieldNames.LEADERBOARD_DISPLAY_NAME.name(), raceInfo.getLeaderboardDisplayName());
                storeTimePoint(raceInfo.getStartOfRace(), newValue, (FieldNames.START_OF_RACE.name()));
                newValue.append(FieldNames.EVENT_ID.name(), raceInfo.getEventID().toString());
                newValue.append(FieldNames.EVENT_NAME.name(), raceInfo.getEventName());
                newValue.append(FieldNames.EVENT_TYPE.name(),
                        raceInfo.getEventType() == null ? null : raceInfo.getEventType().name());

                final URL remoteUrl = raceInfo.getRemoteUrl();
                newValue.append(FieldNames.REMOTE_URL.name(), remoteUrl == null ? null : remoteUrl.toExternalForm());
                newValue.append(FieldNames.ANNIVERSARY_TYPE.name(), anniversary.getValue().getB().toString());
                anniversarysStored.replaceOne(currentProxy, newValue, new ReplaceOptions().upsert(true));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Document createORCCertificateObject(ORCCertificate certificate) {
        final ORCCertificateJsonSerializer serializer = new ORCCertificateJsonSerializer();
        final Document result = Document.parse(serializer.serialize(certificate).toString());
        return result;
    }

    public Document storeRegattaLogEvent(RegattaLikeIdentifier regattaLikeIdentifier, RegattaLogORCCertificateAssignmentEvent event) {
        Document document = createBasicRegattaLogEventDBObject(event);
        document.put(FieldNames.REGATTA_LOG_EVENT_CLASS.name(), RegattaLogORCCertificateAssignmentEvent.class.getSimpleName());
        document.append(FieldNames.RACE_LOG_BOAT_ID.name(), event.getBoatId());
        document.append(FieldNames.ORC_CERTIFICATE.name(), createORCCertificateObject(event.getCertificate()));
        return storeRegattaLogEvent(regattaLikeIdentifier, document);
    }
    
    private List<Document> storeMarkPassings(Map<Competitor, Map<Waypoint, MarkPassing>> markPassings, Course course) {
        final List<Document> result = new ArrayList<>();
        for (final Entry<Competitor, Map<Waypoint, MarkPassing>> e : markPassings.entrySet()) {
            final Document competitorMarkPassings = new Document();
            competitorMarkPassings.put(FieldNames.COMPETITOR_ID.name(), e.getKey().getId());
            final List<Document> markPassingsList = new ArrayList<>();
            for (final Entry<Waypoint, MarkPassing> f : e.getValue().entrySet()) {
                final Document markPassingDoc = new Document();
                markPassingDoc.put(FieldNames.INDEX_OF_PASSED_WAYPOINT.name(), course.getIndexOfWaypoint(f.getKey()));
                markPassingDoc.put(FieldNames.TIME_AS_MILLIS.name(), f.getValue().getTimePoint().asMillis());
                markPassingsList.add(markPassingDoc);
            }
            competitorMarkPassings.put(FieldNames.MARK_PASSINGS.name(), markPassingsList);
            result.add(competitorMarkPassings);
        }
        return result;
    }

    @Override
    public void storeMarkPassings(RaceIdentifier raceIdentifier, MarkPassingRaceFingerprint fingerprint,
            Map<Competitor, Map<Waypoint, MarkPassing>> markPassings, Course course) {
        MongoCollection<Document> markPassingCollection = database.getCollection(CollectionNames.MARKPASSINGS.name());
        JSONObject fingerprintjson = fingerprint.toJson();
        final Document query = new Document();
        DomainObjectFactoryImpl.addRaceIdentifierToQuery(query, raceIdentifier);
        final Document result = new Document();
        final Document fingerprintDoc = Document.parse(fingerprintjson.toString());
        result.put(FieldNames.MARK_PASSINGS_FINGERPRINT.name(), fingerprintDoc);
        storeRaceIdentifier(result, raceIdentifier);
        final List<Document> markPassingsDoc = storeMarkPassings(markPassings, course);
        result.put(FieldNames.MARK_PASSINGS.name(), markPassingsDoc);
        markPassingCollection.replaceOne(query, result, new ReplaceOptions().upsert(true));
    }
    
    @Override
    public void removeMarkPassings(RaceIdentifier raceIdentifier) {
        MongoCollection<Document> markPassingCollection = database.getCollection(CollectionNames.MARKPASSINGS.name());
        final Document query = new Document();
        DomainObjectFactoryImpl.addRaceIdentifierToQuery(query, raceIdentifier);
        markPassingCollection.deleteOne(query);
    }
}
