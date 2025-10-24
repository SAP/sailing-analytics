package com.sap.sailing.domain.persistence.impl;

import static com.mongodb.client.model.Filters.eq;
import static com.sap.sailing.shared.persistence.impl.DomainObjectFactoryImpl.loadDeviceId;
import static com.sap.sailing.shared.persistence.impl.DomainObjectFactoryImpl.loadPosition;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.result.DeleteResult;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCImpliedWindSourceEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCScratchBoatEvent;
import com.sap.sailing.domain.abstractlog.orc.RegattaLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCImpliedWindSourceEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCLegDataEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCScratchBoatEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RegattaLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
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
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogDependentStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogExcludeWindSourcesEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningListChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFixedMarkPassingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogGateLineOpeningTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPathfinderEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogProtestStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogResultsAreOfficialEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRevokeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartProcedureChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogSuppressedMarkPassingsEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogTagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.abstractlog.race.scoring.AdditionalScoringInformationType;
import com.sap.sailing.domain.abstractlog.race.scoring.RaceLogAdditionalScoringInformationEvent;
import com.sap.sailing.domain.abstractlog.race.scoring.impl.RaceLogAdditionalScoringInformationEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogStartTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogUseCompetitorsFromRaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDenoteForTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogStartTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogUseCompetitorsFromRaceLogEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogCloseOpenEndedDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDefineMarkEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMarkMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRegisterBoatEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogSetCompetitorTimeOnTimeFactorEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogCloseOpenEndedDeviceMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatBravoExtendedMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatBravoMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorBravoExtendedMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorBravoMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterBoatEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRevokeEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogSetCompetitorTimeOnTimeFactorEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sailing.domain.anniversary.DetailedRaceInfo;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaRegistry;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.SailingServerConfiguration;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.EventImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.RemoteSailingServerReferenceImpl;
import com.sap.sailing.domain.base.impl.SailingServerConfigurationImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.VenueImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.domain.common.dto.EventType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.tracking.impl.CompetitorJsonConstants;
import com.sap.sailing.domain.leaderboard.DelayedLeaderboardCorrections;
import com.sap.sailing.domain.leaderboard.EventResolver;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.leaderboard.LeaderboardRegistry;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.DelayedLeaderboardCorrectionsImpl;
import com.sap.sailing.domain.leaderboard.impl.DelegatingRegattaLeaderboardWithCompetitorElimination;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardWithOtherTieBreakingLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.markpassinghash.impl.MarkPassingRaceFingerprintImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.MongoRegattaLogStoreFactory;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.ranking.RankingMetricsFactory;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl;
import com.sap.sailing.server.gateway.deserialization.impl.BoatJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorWithBoatRefJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.DeviceConfigurationJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.deserialization.impl.LegacyCompetitorWithContainedBoatJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.RegattaConfigurationJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.ImpliedWindSourceDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.ORCCertificateJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.DeviceConfigurationJsonSerializer;
import com.sap.sailing.shared.persistence.device.DeviceIdentifierMongoHandler;
import com.sap.sailing.shared.persistence.device.impl.PlaceHolderDeviceIdentifierMongoHandler;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Color;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.RGBColor;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.media.impl.ImageDescriptorImpl;
import com.sap.sse.shared.media.impl.VideoDescriptorImpl;
import com.sap.sse.shared.util.impl.UUIDHelper;
import com.sap.sse.util.ThreadPoolUtil;

public class DomainObjectFactoryImpl implements DomainObjectFactory {
    private static final Logger logger = Logger.getLogger(DomainObjectFactoryImpl.class.getName());
    private final LegacyCompetitorWithContainedBoatJsonDeserializer legacyCompetitorWithBoatDeserializer;
    private final CompetitorWithBoatRefJsonDeserializer competitorWithBoatRefDeserializer;
    private final BoatJsonDeserializer boatDeserializer;

    private final MongoDatabase database;

    private final DomainFactory baseDomainFactory;
    private final TypeBasedServiceFinderFactory serviceFinderFactory;
    private final TypeBasedServiceFinder<DeviceIdentifierMongoHandler> deviceIdentifierServiceFinder;
    private final TypeBasedServiceFinder<RaceTrackingConnectivityParametersHandler> raceTrackingConnectivityParamsServiceFinder;

    /**
     * Uses <code>null</code> as the {@link TypeBasedServiceFinder}, meaning that no {@link DeviceIdentifier}s can be
     * loaded using this instance of a {@link DomainObjectFactory}.
     */
    public DomainObjectFactoryImpl(MongoDatabase db, DomainFactory baseDomainFactory) {
        this(db, baseDomainFactory, /* deviceTypeServiceFinder */ null);
    }

    public DomainObjectFactoryImpl(MongoDatabase db, DomainFactory baseDomainFactory,
            TypeBasedServiceFinderFactory serviceFinderFactory) {
        super();
        this.serviceFinderFactory = serviceFinderFactory;
        if (serviceFinderFactory != null) {
            this.deviceIdentifierServiceFinder = serviceFinderFactory
                    .createServiceFinder(DeviceIdentifierMongoHandler.class);
            this.deviceIdentifierServiceFinder.setFallbackService(new PlaceHolderDeviceIdentifierMongoHandler());
            this.raceTrackingConnectivityParamsServiceFinder = serviceFinderFactory
                    .createServiceFinder(RaceTrackingConnectivityParametersHandler.class);
        } else {
            this.deviceIdentifierServiceFinder = null;
            this.raceTrackingConnectivityParamsServiceFinder = null;
        }
        this.baseDomainFactory = baseDomainFactory;
        this.legacyCompetitorWithBoatDeserializer = LegacyCompetitorWithContainedBoatJsonDeserializer.create(baseDomainFactory);
        this.competitorWithBoatRefDeserializer = CompetitorWithBoatRefJsonDeserializer.create(baseDomainFactory);
        this.boatDeserializer = BoatJsonDeserializer.create(baseDomainFactory, /* storeDeserializedBoatPersistently */ false);
        this.database = db;
    }

    @Override
    public DomainFactory getBaseDomainFactory() {
        return baseDomainFactory;
    }

    @Override
    public Wind loadWind(Document object) {
        return new WindImpl(loadPosition(object), loadTimePoint(object), loadSpeedWithBearing(object));
    }

    public static TimePoint loadTimePoint(Document object, String fieldName) {
        final TimePoint result;
        Number timePointAsNumber = (Number) object.get(fieldName);
        if (timePointAsNumber != null) {
            result = new MillisecondsTimePoint(timePointAsNumber.longValue());
        } else {
            result = null;
        }
        return result;
    }

    public static TimePoint loadTimePoint(Document object, FieldNames field) {
        return loadTimePoint(object, field.name());
    }

    public static TimeRange loadTimeRange(Document object, FieldNames field) {
        Document timeRangeObj = (Document) object.get(field.name());
        final TimeRange result;
        if (timeRangeObj == null) {
            result = null;
        } else {
            TimePoint from = loadTimePoint(timeRangeObj, FieldNames.FROM_MILLIS);
            TimePoint to = loadTimePoint(timeRangeObj, FieldNames.TO_MILLIS);
            result = new TimeRangeImpl(from, to);
        }
        return result;
    }

    /**
     * Loads a {@link TimePoint} on the given object at {@link FieldNames#TIME_AS_MILLIS}.
     */
    public TimePoint loadTimePoint(Document object) {
        return loadTimePoint(object, FieldNames.TIME_AS_MILLIS);
    }

    public SpeedWithBearing loadSpeedWithBearing(Document object) {
        return new KnotSpeedWithBearingImpl(((Number) object.get(FieldNames.KNOT_SPEED.name())).doubleValue(),
                new DegreeBearingImpl(((Number) object.get(FieldNames.DEGREE_BEARING.name())).doubleValue()));
    }
    
    public Bearing loadOptionalTrueHeading(Document object) {
        final Bearing result;
        if (object.containsKey(FieldNames.TRUE_HEADING_DEG.name())) {
            result = new DegreeBearingImpl(((Number) object.get(FieldNames.TRUE_HEADING_DEG.name())).doubleValue());
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public RaceIdentifier loadRaceIdentifier(Document dbObject) {
        RaceIdentifier result = null;
        if (dbObject != null) {
            String regattaName = (String) dbObject.get(FieldNames.EVENT_NAME.name());
            String raceName = (String) dbObject.get(FieldNames.RACE_NAME.name());
            if (regattaName != null && raceName != null) {
                result = new RegattaNameAndRaceName(regattaName, raceName);
            }
        }
        return result;
    }
    
    /**
     * @see #loadRaceIdentifier(Document)
     */
    static void addRaceIdentifierToQuery(Document query, RaceIdentifier raceIdentifier) {
        query.put(FieldNames.EVENT_NAME.name(), raceIdentifier.getRegattaName());
        query.put(FieldNames.RACE_NAME.name(), raceIdentifier.getRaceName());
    }

    static void ensureIndicesOnWindTracks(MongoCollection<Document> windTracks) {
        windTracks.createIndex(new Document(FieldNames.RACE_ID.name(), 1), new IndexOptions().name("windbyrace").background(false)); // for new programmatic access
        windTracks.createIndex(new Document(FieldNames.REGATTA_NAME.name(), 1), new IndexOptions().name("windbyregatta").background(false)); // for export or human look-up
        // for legacy access to not yet migrated fixes
        windTracks.createIndex(new Document().append(FieldNames.EVENT_NAME.name(), 1)
                .append(FieldNames.RACE_NAME.name(), 1), new IndexOptions().name("windbyeventandrace").background(false));
        // Unique index
        try {
            windTracks.createIndex(
                    new Document().append(FieldNames.RACE_ID.name(), 1)
                            .append(FieldNames.WIND_SOURCE_NAME.name(), 1).append(FieldNames.WIND_SOURCE_ID.name(), 1)
                            .append(FieldNames.WIND.name() + "." + FieldNames.TIME_AS_MILLIS.name(), 1),
                            new IndexOptions().name("windByRaceSourceAndTime").unique(true).background(false));
        } catch (MongoException exception) {
            if (exception.getCode() == 10092) {
                logger.warning(String.format(
                        "Setting the unique index on the %s collection failed because you have too many duplicates. "
                                + "This leads to the mongo error code %s and the following message: %s \nTo fix this follow "
                                + "the steps provided on the wiki page: http://wiki.sapsailing.com/wiki/howto/misc/cook-book#Remove-"
                                + "duplicates-from-WIND_TRACK-collection",
                        CollectionNames.WIND_TRACKS.name(), exception.getCode(), exception.getMessage()));
            } else {
                logger.severe(String.format(
                        "Setting the unique index on the %s collection failed with error code %s and message: %s",
                        CollectionNames.WIND_TRACKS.name(), exception.getCode(), exception.getMessage()));
            }
        }
    }

    @Override
    public Leaderboard loadLeaderboard(String name, RegattaRegistry regattaRegistry,
            LeaderboardRegistry leaderboardRegistry) {
        MongoCollection<Document> leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());
        Leaderboard result = null;
        try {
            Document query = new Document();
            query.put(FieldNames.LEADERBOARD_NAME.name(), name);
            for (Document o : leaderboardCollection.find(query)) {
                result = loadLeaderboard(o, regattaRegistry, leaderboardRegistry, /* groupForMetaLeaderboard */ null);
            }
        } catch (Exception e) {
            // something went wrong during DB access; report, then use empty new wind track
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load leaderboard " + name + ".");
            logger.log(Level.SEVERE, "loadLeaderboard", e);
        }
        return result;
    }

    @Override
    public RegattaLeaderboardWithEliminations loadRegattaLeaderboardWithEliminations(Document dbLeaderboard,
            String leaderboardName, String wrappedRegattaLeaderboardName, LeaderboardRegistry leaderboardRegistry) {
        final RegattaLeaderboardWithEliminations result;
        Iterable<?> eliminatedCompetitorIds = (Iterable<?>) dbLeaderboard.get(FieldNames.ELMINATED_COMPETITORS.name());
        result = new DelegatingRegattaLeaderboardWithCompetitorElimination(
                () -> (RegattaLeaderboard) leaderboardRegistry.getLeaderboardByName(wrappedRegattaLeaderboardName),
                leaderboardName);
        for (Object eliminatedCompetitorId : eliminatedCompetitorIds) {
            Competitor eliminatedCompetitor = baseDomainFactory.getCompetitorAndBoatStore()
                    .getExistingCompetitorById((Serializable) eliminatedCompetitorId);
            if (eliminatedCompetitor == null) {
                logger.warning("Couldn't find eliminated competitor with ID " + eliminatedCompetitorId);
            } else {
                result.setEliminated(eliminatedCompetitor, true);
            }
        }
        return result;
    }

    /**
     * If the DBObject has a field {@link FieldNames#WRAPPED_REGATTA_LEADERBOARD_NAME} then the object represents a
     * {@link RegattaLeaderboardWithEliminations}. Otherwise, if the DBObject has a field
     * {@link FieldNames#REGATTA_NAME} then the object represents a {@link RegattaLeaderboard}. Otherwise, a
     * {@link FlexibleLeaderboard} will be loaded.
     *
     * @param leaderboardRegistry
     *            if not <code>null</code>, then before creating and loading the leaderboard it is looked up in this
     *            registry and only loaded if not found there. If <code>leaderboardRegistry</code> is <code>null</code>,
     *            the leaderboard is loaded in any case. If the leaderboard is loaded and
     *            <code>leaderboardRegistry</code> is not <code>null</code>, the leaderboard loaded is
     *            {@link LeaderboardRegistry#addLeaderboard(Leaderboard) added to the registry}.
     * @param groupForMetaLeaderboard
     *            if not <code>null</code>, a {@link LeaderboardGroupMetaLeaderboard} instance is created and set as the
     *            group's {@link LeaderboardGroup#setOverallLeaderboard(Leaderboard) overall leaderboard}
     *
     * @return <code>null</code> in case the leaderboard couldn't be loaded, e.g., because the regatta referenced by a
     *         {@link RegattaLeaderboard} cannot be found; the leaderboard loaded or found in
     *         <code>leaderboardRegistry</code>, otherwise
     */
    private Leaderboard loadLeaderboard(Document dbLeaderboard, RegattaRegistry regattaRegistry,
            LeaderboardRegistry leaderboardRegistry, LeaderboardGroup groupForMetaLeaderboard) {
        Leaderboard result = null;
        String leaderboardName = (String) dbLeaderboard.get(FieldNames.LEADERBOARD_NAME.name());
        if (leaderboardRegistry != null) {
            result = leaderboardRegistry.getLeaderboardByName(leaderboardName);
        }
        if (result == null) {
            final String wrappedRegattaLeaderboardName = (String) dbLeaderboard
                    .get(FieldNames.WRAPPED_REGATTA_LEADERBOARD_NAME.name());
            if (wrappedRegattaLeaderboardName != null) {
                result = loadRegattaLeaderboardWithEliminations(dbLeaderboard, leaderboardName,
                        wrappedRegattaLeaderboardName, leaderboardRegistry);
            } else {
                if (groupForMetaLeaderboard != null) {
                    result = new LeaderboardGroupMetaLeaderboard(groupForMetaLeaderboard,
                            loadScoringScheme(dbLeaderboard),
                            loadResultDiscardingRule(dbLeaderboard, FieldNames.LEADERBOARD_DISCARDING_THRESHOLDS));
                    groupForMetaLeaderboard.setOverallLeaderboard(result);
                } else {
                    String regattaName = (String) dbLeaderboard.get(FieldNames.REGATTA_NAME.name());
                    ThresholdBasedResultDiscardingRule resultDiscardingRule = loadResultDiscardingRule(dbLeaderboard,
                            FieldNames.LEADERBOARD_DISCARDING_THRESHOLDS);
                    if (regattaName == null) {
                        result = loadFlexibleLeaderboard(dbLeaderboard, resultDiscardingRule);
                    } else {
                        result = loadRegattaLeaderboard(leaderboardName, regattaName, dbLeaderboard,
                                resultDiscardingRule, regattaRegistry, leaderboardRegistry);
                    }
                }
                if (result != null) {
                    DelayedLeaderboardCorrections loadedLeaderboardCorrections = new DelayedLeaderboardCorrectionsImpl(
                            result, baseDomainFactory);
                    loadLeaderboardCorrections(dbLeaderboard, loadedLeaderboardCorrections,
                            result.getScoreCorrection());
                    loadSuppressedCompetitors(dbLeaderboard, loadedLeaderboardCorrections);
                    loadColumnFactors(dbLeaderboard, result);
                }
            }
            if (result != null) {
                final Leaderboard finalResult = result;
                finalResult.setDisplayName((String) dbLeaderboard.get(FieldNames.LEADERBOARD_DISPLAY_NAME.name()));
                // add the leaderboard to the registry
                if (leaderboardRegistry != null) {
                    leaderboardRegistry.addLeaderboard(result);
                    logger.info("loaded leaderboard " + result.getName() + " into " + leaderboardRegistry);
                }
            }
        }
        return result;
    }

    private void loadColumnFactors(Document dbLeaderboard, Leaderboard result) {
        Document dbColumnFactors = (Document) dbLeaderboard.get(FieldNames.LEADERBOARD_COLUMN_FACTORS.name());
        if (dbColumnFactors != null) {
            for (String encodedRaceColumnName : dbColumnFactors.keySet()) {
                double factor = ((Number) dbColumnFactors.get(encodedRaceColumnName)).doubleValue();
                String raceColumnName = MongoUtils.unescapeDollarAndDot(encodedRaceColumnName);
                final RaceColumn raceColumn = result.getRaceColumnByName(raceColumnName);
                if (raceColumn != null) {
                    raceColumn.setFactor(factor);
                } else {
                    logger.warning("Expected to find race column named " + raceColumnName + " in leaderboard "
                            + result.getName() + " to apply column factor " + factor
                            + ", but the race column wasn't found. Ignoring factor.");
                }
            }
        }
    }

    private void loadSuppressedCompetitors(Document dbLeaderboard,
            DelayedLeaderboardCorrections loadedLeaderboardCorrections) {
        Iterable<?> dbSuppressedCompetitorIDs = (Iterable<?>) dbLeaderboard
                .get(FieldNames.LEADERBOARD_SUPPRESSED_COMPETITOR_IDS.name());
        if (dbSuppressedCompetitorIDs != null) {
            for (Object competitorId : dbSuppressedCompetitorIDs) {
                loadedLeaderboardCorrections.suppressCompetitorByID((Serializable) competitorId);
            }
        }
    }

    /**
     * @param dbObject
     *            expects to find a field identified by <code>field</code> which holds a {@link Iterable<?>}
     */
    private ThresholdBasedResultDiscardingRule loadResultDiscardingRule(Document dbObject, FieldNames field) {
        @SuppressWarnings("unchecked")
        Iterable<Document> dbDiscardIndexResultsStartingWithHowManyRaces = (Iterable<Document>) dbObject.get(field.name());
        final ThresholdBasedResultDiscardingRule result;
        if (dbDiscardIndexResultsStartingWithHowManyRaces == null) {
            result = null;
        } else {
            int[] discardIndexResultsStartingWithHowManyRaces = new int[Util.size(dbDiscardIndexResultsStartingWithHowManyRaces)];
            int i = 0;
            for (Object discardingThresholdAsObject : dbDiscardIndexResultsStartingWithHowManyRaces) {
                discardIndexResultsStartingWithHowManyRaces[i++] = ((Number) discardingThresholdAsObject).intValue();
            }
            result = new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces);
        }
        return result;
    }

    /**
     * @return <code>null</code> if the regatta cannot be resolved; otherwise the leaderboard for the regatta specified
     */
    private RegattaLeaderboard loadRegattaLeaderboard(String leaderboardName, String regattaName,
            Document dbLeaderboard, ThresholdBasedResultDiscardingRule resultDiscardingRule,
            RegattaRegistry regattaRegistry, LeaderboardRegistry leaderboardRegistry) {
        final RegattaLeaderboard result;
        Regatta regatta = regattaRegistry.getRegatta(new RegattaName(regattaName));
        if (regatta == null) {
            logger.info("Couldn't find regatta " + regattaName
                    + " for corresponding regatta leaderboard. Not loading regatta leaderboard.");
            result = null;
        } else {
            final String otherTieBreakingLeaderboardName = (String) dbLeaderboard.get(FieldNames.OTHER_TIEBREAKING_LEADERBOARD_NAME.name());
            if (otherTieBreakingLeaderboardName == null) {
                result = new RegattaLeaderboardImpl(regatta, resultDiscardingRule);
            } else {
                result = new RegattaLeaderboardWithOtherTieBreakingLeaderboardImpl(regatta, resultDiscardingRule,
                        () -> (RegattaLeaderboard) leaderboardRegistry.getLeaderboardByName(otherTieBreakingLeaderboardName));
            }
        }
        return result;
    }

    private RaceLogStore getRaceLogStore() {
        return MongoRaceLogStoreFactory.INSTANCE
                .getMongoRaceLogStore(new MongoObjectFactoryImpl(database, serviceFinderFactory), this);
    }

    private RegattaLogStore getRegattaLogStore() {
        return MongoRegattaLogStoreFactory.INSTANCE
                .getMongoRegattaLogStore(new MongoObjectFactoryImpl(database, serviceFinderFactory), this);
    }

    private FlexibleLeaderboard loadFlexibleLeaderboard(Document dbLeaderboard,
            ThresholdBasedResultDiscardingRule resultDiscardingRule) {
        final FlexibleLeaderboardImpl result;
        @SuppressWarnings("unchecked")
        Iterable<Document> dbRaceColumns = (Iterable<Document>) dbLeaderboard.get(FieldNames.LEADERBOARD_COLUMNS.name());
        if (dbRaceColumns == null) {
            // this was probably an orphaned overall leaderboard
            logger.warning("Probably found orphan overall leaderboard named "
                    + dbLeaderboard.get(FieldNames.LEADERBOARD_NAME.name()) + ". Ignoring.");
            result = null;
        } else {
            final ScoringScheme scoringScheme = loadScoringScheme(dbLeaderboard);
            final Iterable<CourseArea> courseAreas = loadCourseAreas(dbLeaderboard);
            result = new FlexibleLeaderboardImpl(getRaceLogStore(), getRegattaLogStore(),
                    (String) dbLeaderboard.get(FieldNames.LEADERBOARD_NAME.name()), resultDiscardingRule, scoringScheme,
                    courseAreas);
            // For a FlexibleLeaderboard, there should be only the default fleet for any race column
            for (Object dbRaceColumnAsObject : dbRaceColumns) {
                Document dbRaceColumn = (Document) dbRaceColumnAsObject;
                String columnName = (String) dbRaceColumn.get(FieldNames.LEADERBOARD_COLUMN_NAME.name());
                RaceColumn raceColumn = result.addRaceColumn(columnName,
                        (Boolean) dbRaceColumn.get(FieldNames.LEADERBOARD_IS_MEDAL_RACE_COLUMN.name()));
                Map<String, RaceIdentifier> raceIdentifiers = loadRaceIdentifiers(dbRaceColumn);
                RaceIdentifier defaultFleetRaceIdentifier = raceIdentifiers.get(result.getFleet(null).getName());
                if (defaultFleetRaceIdentifier == null) {
                    // Backward compatibility
                    defaultFleetRaceIdentifier = raceIdentifiers.get(null);
                }
                if (defaultFleetRaceIdentifier != null) {
                    Fleet defaultFleet = result.getFleet(null);
                    if (defaultFleet != null) {
                        raceColumn.setRaceIdentifier(defaultFleet, defaultFleetRaceIdentifier);
                    } else {
                        // leaderboard has no default fleet; don't know what to do with default RaceIdentifier
                        logger.warning("Discarding RaceIdentifier " + defaultFleetRaceIdentifier
                                + " for default fleet for leaderboard " + result.getName()
                                + " because no default fleet was found in leaderboard");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Loads no, one, or several course areas from the {@link FieldNames#COURSE_AREA_ID} and the
     * {@link FieldNames#COURSE_AREA_IDS} fields of the document passed. For backward compatibility, {@code null} or a
     * single course area ID are accepted in {@link FieldNames#COURSE_AREA_ID}. If a non-{@code null} value is found
     * there, it is looked up as a course area and dded to the result. The default way of representing the course areas
     * now is in the {@link FieldNames#COURSE_AREA_IDS} field where a list is expected.
     *
     * @return an always valid, non-{@code null} sequence which may be empty
     */
    private Iterable<CourseArea> loadCourseAreas(Document documentContainingCourseAreaIds) {
        final Set<CourseArea> courseAreas = new HashSet<>();
        String courseAreaId = (String) documentContainingCourseAreaIds.get(FieldNames.COURSE_AREA_ID.name());
        if (courseAreaId != null) {
            lookupCourseAreaAndAddIfFound(courseAreas, courseAreaId);
        }
        List<?> courseAreaIds = (List<?>) documentContainingCourseAreaIds.get(FieldNames.COURSE_AREA_IDS.name());
        if (courseAreaIds != null) {
            for (final Object courseAreaIdAsString : courseAreaIds) {
                lookupCourseAreaAndAddIfFound(courseAreas, courseAreaIdAsString.toString());
            }
        }
        return courseAreas;
    }

    private void lookupCourseAreaAndAddIfFound(final Set<CourseArea> courseAreas, String courseAreaIdAsString) {
        UUID courseAreaUuid = UUID.fromString(courseAreaIdAsString);
        final CourseArea lookupResult = baseDomainFactory.getExistingCourseAreaById(courseAreaUuid);
        if (lookupResult != null) {
            courseAreas.add(lookupResult);
        }
    }

    private ScoringScheme loadScoringScheme(Document dbLeaderboard) {
        ScoringSchemeType scoringSchemeType = getScoringSchemeType(dbLeaderboard);
        final ScoringScheme scoringScheme = baseDomainFactory.createScoringScheme(scoringSchemeType);
        return scoringScheme;
    }

    private void loadLeaderboardCorrections(Document dbLeaderboard, DelayedLeaderboardCorrections correctionsToUpdate,
            SettableScoreCorrection scoreCorrectionToUpdate) {
        @SuppressWarnings("unchecked")
        Iterable<Document> carriedPointsById = (Iterable<Document>) dbLeaderboard
                .get(FieldNames.LEADERBOARD_CARRIED_POINTS_BY_ID.name());
        if (carriedPointsById != null) {
            for (Object o : carriedPointsById) {
                Document competitorIdAndCarriedPoints = (Document) o;
                Serializable competitorId = (Serializable) competitorIdAndCarriedPoints
                        .get(FieldNames.COMPETITOR_ID.name());
                Double carriedPointsForCompetitor = ((Number) competitorIdAndCarriedPoints
                        .get(FieldNames.LEADERBOARD_CARRIED_POINTS.name())).doubleValue();
                if (carriedPointsForCompetitor != null) {
                    correctionsToUpdate.setCarriedPointsByID(competitorId, carriedPointsForCompetitor);
                }
            }
        }
        Document dbScoreCorrection = (Document) dbLeaderboard.get(FieldNames.LEADERBOARD_SCORE_CORRECTIONS.name());
        if (dbScoreCorrection.containsKey(FieldNames.LEADERBOARD_SCORE_CORRECTION_TIMESTAMP.name())) {
            scoreCorrectionToUpdate.setTimePointOfLastCorrectionsValidity(new MillisecondsTimePoint(
                    (Long) dbScoreCorrection.get(FieldNames.LEADERBOARD_SCORE_CORRECTION_TIMESTAMP.name())));
            dbScoreCorrection.remove(FieldNames.LEADERBOARD_SCORE_CORRECTION_TIMESTAMP.name());
        }
        if (dbScoreCorrection.containsKey(FieldNames.LEADERBOARD_SCORE_CORRECTION_COMMENT.name())) {
            scoreCorrectionToUpdate
                    .setComment((String) dbScoreCorrection.get(FieldNames.LEADERBOARD_SCORE_CORRECTION_COMMENT.name()));
            dbScoreCorrection.remove(FieldNames.LEADERBOARD_SCORE_CORRECTION_COMMENT.name());
        }
        for (String escapedRaceColumnName : dbScoreCorrection.keySet()) {
            // deprecated style: a DBObject per race where the keys are the escaped competitor names
            // new style: a BsonArray per race where each entry is a DBObject with COMPETITOR_ID and
            // LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON and LEADERBOARD_CORRECTED_SCORE fields each
            @SuppressWarnings("unchecked")
            Iterable<Document> dbScoreCorrectionForRace = (Iterable<Document>) dbScoreCorrection.get(escapedRaceColumnName);
            final RaceColumn raceColumn = correctionsToUpdate.getLeaderboard()
                    .getRaceColumnByName(MongoUtils.unescapeDollarAndDot(escapedRaceColumnName));
            if (raceColumn != null) {
                for (Document dbScoreCorrectionForCompetitorInRace : dbScoreCorrectionForRace) {
                    Serializable competitorId = (Serializable) dbScoreCorrectionForCompetitorInRace
                            .get(FieldNames.COMPETITOR_ID.name());
                    if (dbScoreCorrectionForCompetitorInRace
                            .containsKey(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name())) {
                        correctionsToUpdate.setMaxPointsReasonByID(competitorId, raceColumn,
                                MaxPointsReason.valueOf((String) dbScoreCorrectionForCompetitorInRace
                                        .get(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name())));
                    }
                    if (dbScoreCorrectionForCompetitorInRace
                            .containsKey(FieldNames.LEADERBOARD_CORRECTED_SCORE.name())) {
                        final Number dbScoreCorrectionForCompetitorInRaceAsNumber = (Number) dbScoreCorrectionForCompetitorInRace
                                        .get(FieldNames.LEADERBOARD_CORRECTED_SCORE.name());
                        final Double leaderboardCorrectedScore = dbScoreCorrectionForCompetitorInRaceAsNumber == null ? null :
                            dbScoreCorrectionForCompetitorInRaceAsNumber.doubleValue();
                        correctionsToUpdate.correctScoreByID(competitorId, raceColumn, leaderboardCorrectedScore);
                    }
                    if (dbScoreCorrectionForCompetitorInRace
                            .containsKey(FieldNames.LEADERBOARD_INCREMENTAL_SCORE_CORRECTION_IN_POINTS.name())) {
                        final Number dbIncrementalScoreCorrectionForCompetitorInRaceAsNumberInPoints = (Number) dbScoreCorrectionForCompetitorInRace
                                        .get(FieldNames.LEADERBOARD_INCREMENTAL_SCORE_CORRECTION_IN_POINTS.name());
                        final Double leaderboardIncrementalCorrectedScoreInPoints = dbIncrementalScoreCorrectionForCompetitorInRaceAsNumberInPoints == null ? null :
                            dbIncrementalScoreCorrectionForCompetitorInRaceAsNumberInPoints.doubleValue();
                        correctionsToUpdate.correctScoreIncrementallyByID(competitorId, raceColumn, leaderboardIncrementalCorrectedScoreInPoints);
                    }
                }
            } else {
                logger.warning("Couldn't find race column " + MongoUtils.unescapeDollarAndDot(escapedRaceColumnName)
                        + " in leaderboard " + correctionsToUpdate.getLeaderboard().getName());
            }
        }
        Iterable<?> competitorDisplayNames = (Iterable<?>) dbLeaderboard
                .get(FieldNames.LEADERBOARD_COMPETITOR_DISPLAY_NAMES.name());
        // deprecated style: a Document whose keys are the escaped competitor names
        // new style: a BsonArray whose entries are Documents with COMPETITOR_ID and COMPETITOR_DISPLAY_NAME fields
        if (competitorDisplayNames != null) {
            if (competitorDisplayNames instanceof Iterable<?>) {
                for (Object o : (Iterable<?>) competitorDisplayNames) {
                    Document competitorDisplayName = (Document) o;
                    final Serializable competitorId = (Serializable) competitorDisplayName
                            .get(FieldNames.COMPETITOR_ID.name());
                    final String displayName = (String) competitorDisplayName
                            .get(FieldNames.COMPETITOR_DISPLAY_NAME.name());
                    correctionsToUpdate.setDisplayNameByID(competitorId, displayName);
                }
            } else {
                logger.severe("Deprecated, now unreadable format of the "
                        + FieldNames.LEADERBOARD_COMPETITOR_DISPLAY_NAMES.name() + " field for leaderboard "
                        + dbLeaderboard.get(FieldNames.LEADERBOARD_NAME.name())
                        + ". You will have to update the competitor display names manually: " + competitorDisplayNames);
            }
        }
    }

    /**
     * Expects a Document under the key {@link FieldNames#RACE_IDENTIFIERS} whose keys are the fleet names and whose
     * values are the race identifiers as Documents (see {@link #loadRaceIdentifier(Document)}). If legacy DB instances
     * have a {@link RaceIdentifier} that is not associated with a fleet name, it may be stored directly in the
     * <code>dbRaceColumn</code>. In this case, it is returned with <code>null</code> as the fleet name key.
     *
     * @return a map with fleet names as key and the corresponding fleet's race identifier as value; the special
     *         <code>null</code> key is used to identify a "default fleet" for backward compatibility with stored
     *         leaderboards which don't know about fleets yet; this key should be mapped to the leaderboard's default
     *         fleet.
     */
    private Map<String, RaceIdentifier> loadRaceIdentifiers(Document dbRaceColumn) {
        Map<String, RaceIdentifier> result = new HashMap<String, RaceIdentifier>();
        // try to load a deprecated single race identifier to associate with the default fleet:
        RaceIdentifier singleLegacyRaceIdentifier = loadRaceIdentifier(dbRaceColumn);
        if (singleLegacyRaceIdentifier != null) {
            result.put(null, singleLegacyRaceIdentifier);
        }
        Document raceIdentifiersPerFleet = (Document) dbRaceColumn.get(FieldNames.RACE_IDENTIFIERS.name());
        if (raceIdentifiersPerFleet != null) {
            for (String escapedFleetName : raceIdentifiersPerFleet.keySet()) {
                String fleetName = MongoUtils.unescapeDollarAndDot(escapedFleetName);
                result.put(fleetName, loadRaceIdentifier((Document) raceIdentifiersPerFleet.get(escapedFleetName)));
            }
        }
        return result;
    }

    @Override
    public LeaderboardGroup loadLeaderboardGroup(String name, RegattaRegistry regattaRegistry,
            LeaderboardRegistry leaderboardRegistry) {
        MongoCollection<Document> leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        LeaderboardGroup leaderboardGroup = null;
        try {
            Document query = new Document();
            query.put(FieldNames.LEADERBOARD_GROUP_NAME.name(), name);
            leaderboardGroup = loadLeaderboardGroup(leaderboardGroupCollection.find(query).first(), regattaRegistry,
                    leaderboardRegistry);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load leaderboard group " + name + ".");
            logger.log(Level.SEVERE, "loadLeaderboardGroup", e);
        }

        return leaderboardGroup;
    }

    @Override
    public Iterable<LeaderboardGroup> getAllLeaderboardGroups(RegattaRegistry regattaRegistry,
            LeaderboardRegistry leaderboardRegistry) {
        MongoCollection<Document> leaderboardGroupCollection = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name());
        Set<LeaderboardGroup> leaderboardGroups = new HashSet<LeaderboardGroup>();
        try {
            for (Document o : leaderboardGroupCollection.find()) {
                boolean hasUUID = o.containsKey(FieldNames.LEADERBOARD_GROUP_UUID.name());
                final LeaderboardGroup leaderboardGroup = loadLeaderboardGroup(o, regattaRegistry, leaderboardRegistry);
                leaderboardGroups.add(leaderboardGroup);
                if (!hasUUID) {
                    // in an effort to migrate leaderboard groups without ID to such that have a UUID as their ID, we
                    // need
                    // to write a leaderboard group to the database again after it just received a UUID for the first
                    // time:
                    logger.info("Existing LeaderboardGroup " + leaderboardGroup.getName()
                            + " received a UUID during migration; updating the leaderboard group in the database");
                    new MongoObjectFactoryImpl(database).storeLeaderboardGroup(leaderboardGroup);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load leaderboard groups.");
            logger.log(Level.SEVERE, "loadLeaderboardGroup", e);
        }

        return leaderboardGroups;
    }

    private LeaderboardGroup loadLeaderboardGroup(Document o, RegattaRegistry regattaRegistry,
            LeaderboardRegistry leaderboardRegistry) {
        MongoCollection<Document> leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());
        String name = (String) o.get(FieldNames.LEADERBOARD_GROUP_NAME.name());
        UUID uuid = (UUID) o.get(FieldNames.LEADERBOARD_GROUP_UUID.name());
        if (uuid == null) {
            uuid = UUID.randomUUID();
            logger.info("Leaderboard group " + name + " receives UUID " + uuid + " in a migration effort");
            // migration: leaderboard groups that don't yet have a UUID receive a random one
        }
        String description = (String) o.get(FieldNames.LEADERBOARD_GROUP_DESCRIPTION.name());
        String displayName = (String) o.get(FieldNames.LEADERBOARD_GROUP_DISPLAY_NAME.name());
        boolean displayGroupsInReverseOrder = false; // default value
        Object displayGroupsInReverseOrderObj = o.get(FieldNames.LEADERBOARD_GROUP_DISPLAY_IN_REVERSE_ORDER.name());
        if (displayGroupsInReverseOrderObj != null) {
            displayGroupsInReverseOrder = (Boolean) displayGroupsInReverseOrderObj;
        }
        ArrayList<Leaderboard> leaderboards = new ArrayList<Leaderboard>();
        @SuppressWarnings("unchecked")
        Iterable<Document> dbLeaderboardIds = (Iterable<Document>) o.get(FieldNames.LEADERBOARD_GROUP_LEADERBOARDS.name());
        for (Object object : dbLeaderboardIds) {
            ObjectId dbLeaderboardId = (ObjectId) object;
            Document dbLeaderboard = leaderboardCollection.find(eq("_id", dbLeaderboardId)).first();
            if (dbLeaderboard != null) {
                final Leaderboard loadedLeaderboard = loadLeaderboard(dbLeaderboard, regattaRegistry,
                        leaderboardRegistry, /* groupForMetaLeaderboard */null);
                if (loadedLeaderboard != null) {
                    leaderboards.add(loadedLeaderboard);
                }
            } else {
                logger.warning("couldn't find leaderboard with ID " + dbLeaderboardId
                        + " referenced by leaderboard group " + name);
            }
        }
        logger.info("loaded leaderboard group " + name);
        LeaderboardGroupImpl result = new LeaderboardGroupImpl(uuid, name, description, displayName,
                displayGroupsInReverseOrder, leaderboards);
        Object overallLeaderboardIdOrName = o.get(FieldNames.LEADERBOARD_GROUP_OVERALL_LEADERBOARD.name());
        if (overallLeaderboardIdOrName != null) {
            final Document dbOverallLeaderboard;
            if (overallLeaderboardIdOrName instanceof ObjectId) {
                dbOverallLeaderboard = leaderboardCollection.find(eq("_id", overallLeaderboardIdOrName)).first();
            } else {
                dbOverallLeaderboard = (Document) overallLeaderboardIdOrName;
            }
            if (dbOverallLeaderboard != null) {
                // the loadLeaderboard call adds the overall leaderboard to the leaderboard registry and sets it as the
                // overall leaderboard of the leaderboard group
                loadLeaderboard(dbOverallLeaderboard, regattaRegistry, leaderboardRegistry,
                        /* groupForMetaLeaderboard */ result);
            }
        }
        return result;
    }

    @Override
    public Iterable<Leaderboard> getLeaderboardsNotInGroup(RegattaRegistry regattaRegistry,
            LeaderboardRegistry leaderboardRegistry) {
        MongoCollection<Document> leaderboardCollection = database.getCollection(CollectionNames.LEADERBOARDS.name());
        Set<Leaderboard> result = new HashSet<Leaderboard>();
        try {
            // For MongoDB 2.4 $where with refs to global objects no longer works
            // http://docs.mongodb.org/manual/reference/operator/where/#op._S_where
            // Also a single where leads to a table walk without using indexes. So avoid $where.

            // Don't change the query object, unless you know what you're doing.
            // It queries all leaderboards not referenced to be part of a leaderboard group
            // and in particular not being an overall leaderboard of a leaderboard group.
            FindIterable<Document> allLeaderboards = leaderboardCollection.find();
            for (Document leaderboardFromDB : allLeaderboards) {
                Document inLeaderboardGroupsQuery = new Document();
                inLeaderboardGroupsQuery.put(FieldNames.LEADERBOARD_GROUP_LEADERBOARDS.name(),
                        ((ObjectId) leaderboardFromDB.get("_id")).toString());
                boolean inLeaderboardGroups = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name())
                        .find(inLeaderboardGroupsQuery).first() != null;

                Document inLeaderboardGroupOverallQuery = new Document();
                inLeaderboardGroupOverallQuery.put(FieldNames.LEADERBOARD_GROUP_OVERALL_LEADERBOARD.name(),
                        ((ObjectId) leaderboardFromDB.get("_id")).toString());
                boolean inLeaderboardGroupOverall = database.getCollection(CollectionNames.LEADERBOARD_GROUPS.name())
                        .find(inLeaderboardGroupOverallQuery).first() != null;

                Document inLeaderboardGroupOverallQueryName = new Document();
                inLeaderboardGroupOverallQueryName.put(FieldNames.LEADERBOARD_GROUP_OVERALL_LEADERBOARD.name(),
                        leaderboardFromDB.get(FieldNames.LEADERBOARD_NAME.name()));
                boolean inLeaderboardGroupOverallName = database
                        .getCollection(CollectionNames.LEADERBOARD_GROUPS.name())
                        .find(inLeaderboardGroupOverallQueryName).first() != null;

                if (!inLeaderboardGroups && !inLeaderboardGroupOverall && !inLeaderboardGroupOverallName) {
                    final Leaderboard loadedLeaderboard = loadLeaderboard(leaderboardFromDB, regattaRegistry,
                            leaderboardRegistry, /* groupForMetaLeaderboard */ null);
                    if (loadedLeaderboard != null) {
                        result.add(loadedLeaderboard);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load leaderboards.");
            logger.log(Level.SEVERE, "getAllLeaderboards", e);
        }
        return result;
    }

    @Override
    public WindTrack loadWindTrack(String regattaName, RaceDefinition race, WindSource windSource,
            long millisecondsOverWhichToAverage) {
        final WindTrack result;
        Map<WindSource, WindTrack> resultMap = loadWindTracks(regattaName, race, windSource,
                millisecondsOverWhichToAverage);
        if (resultMap.containsKey(windSource)) {
            result = resultMap.get(windSource);
        } else {
            // create an empty wind track as result if no fixes were found in store for the wind source requested
            result = new WindTrackImpl(millisecondsOverWhichToAverage, windSource.getType().getBaseConfidence(),
                    windSource.getType().useSpeed(), /* nameForReadWriteLock */ WindTrackImpl.class.getSimpleName()
                            + " for source " + windSource.toString());
        }
        return result;
    }

    @Override
    public Map<? extends WindSource, ? extends WindTrack> loadWindTracks(String regattaName, RaceDefinition race,
            long millisecondsOverWhichToAverageWind) {
        Map<WindSource, WindTrack> result = loadWindTracks(regattaName, race, /* constrain wind source */ null,
                millisecondsOverWhichToAverageWind);
        return result;
    }

    /**
     * @param constrainToWindSource
     *            if <code>null</code>, wind for all sources will be loaded; otherwise, only wind data for the wind
     *            source specified by this argument will be loaded
     */
    private Map<WindSource, WindTrack> loadWindTracks(String regattaName, RaceDefinition race,
            WindSource constrainToWindSource, long millisecondsOverWhichToAverageWind) {
        Map<WindSource, WindTrack> result = new HashMap<WindSource, WindTrack>();
        try {
            MongoCollection<Document> windTracks = database.getCollection(CollectionNames.WIND_TRACKS.name());
            ensureIndicesOnWindTracks(windTracks);
            Document queryById = new Document();
            // the default query is by the RACE_ID key:
            queryById.put(FieldNames.RACE_ID.name(), race.getId());
            if (constrainToWindSource != null) {
                queryById.put(FieldNames.WIND_SOURCE_NAME.name(), constrainToWindSource.name());
            }
            for (Document dbWind : windTracks.find(queryById)) {
                loadWindFix(result, dbWind, millisecondsOverWhichToAverageWind);
            }
            // Additionally check for legacy wind fixes stored with the old EVENT_NAME key; if any are found, migrate
            // them
            Document queryByName = new Document();
            queryByName.put(FieldNames.EVENT_NAME.name(), regattaName);
            queryByName.put(FieldNames.RACE_NAME.name(), race.getName());
            if (constrainToWindSource != null) {
                queryByName.put(FieldNames.WIND_SOURCE_NAME.name(), constrainToWindSource.name());
            }
            final FindIterable<Document> windFixesFoundByName = windTracks.find(queryByName).batchSize(100000);
            if (windFixesFoundByName.iterator().hasNext()) {
                List<Document> windFixesToMigrate = new ArrayList<>();
                for (Document dbWind : windFixesFoundByName) {
                    Util.Pair<Wind, WindSource> wind = loadWindFix(result, dbWind, millisecondsOverWhichToAverageWind);
                    // write the wind fix with the new ID-based key and remove the legacy wind fix from the DB
                    windFixesToMigrate.add(new MongoObjectFactoryImpl(database).storeWindTrackEntry(race, regattaName,
                            wind.getB(), wind.getA()));
                }
                final long size = windTracks.countDocuments(queryByName);
                logger.info("Migrating " + size + " wind fixes of regatta " + regattaName
                        + " and race " + race.getName() + " to ID-based keys");
                windTracks.insertMany(windFixesToMigrate);
                logger.info("Removing " + size
                        + " wind fixes that were keyed by the names of regatta " + regattaName + " and race "
                        + race.getName());
                windTracks.deleteMany(queryByName);
            }
        } catch (Exception e) {
            // something went wrong during DB access; report, then use empty new wind track
            logger.log(Level.SEVERE,
                    "Error connecting to MongoDB, unable to load recorded wind data. Check MongoDB settings.");
            logger.log(Level.SEVERE, "loadWindTrack", e);
        }
        return result;
    }

    private Util.Pair<Wind, WindSource> loadWindFix(Map<WindSource, WindTrack> result, Document dbWind,
            long millisecondsOverWhichToAverageWind) {
        Wind wind = loadWind((Document) dbWind.get(FieldNames.WIND.name()));
        WindSourceType windSourceType = WindSourceType.valueOf((String) dbWind.get(FieldNames.WIND_SOURCE_NAME.name()));
        WindSource windSource;
        if (dbWind.containsKey(FieldNames.WIND_SOURCE_ID.name())) {
            windSource = new WindSourceWithAdditionalID(windSourceType,
                    (String) dbWind.get(FieldNames.WIND_SOURCE_ID.name()));
        } else {
            windSource = new WindSourceImpl(windSourceType);
        }
        WindTrack track = result.get(windSource);
        if (track == null) {
            track = new WindTrackImpl(millisecondsOverWhichToAverageWind, windSource.getType().getBaseConfidence(),
                    windSource.getType().useSpeed(), /* nameForReadWriteLock */ WindTrackImpl.class.getSimpleName()
                            + " for source " + windSource.toString());
            result.put(windSource, track);
        }
        track.add(wind);
        return new Util.Pair<Wind, WindSource>(wind, windSource);
    }

    @Override
    public void loadLeaderboardGroupLinksForEvents(EventResolver eventResolver,
            LeaderboardGroupResolver leaderboardGroupResolver) {
        MongoCollection<Document> links = database.getCollection(CollectionNames.LEADERBOARD_GROUP_LINKS_FOR_EVENTS.name());
        for (Object o : links.find()) {
            Document dbLink = (Document) o;
            UUID eventId = (UUID) dbLink.get(FieldNames.EVENT_ID.name());
            Event event = eventResolver.getEvent(eventId);
            if (event == null) {
                logger.info(
                        "Found leaderboard group IDs for event with ID " + eventId + " but couldn't find that event.");
            } else {
                @SuppressWarnings("unchecked")
                List<UUID> leaderboardGroupIDs = (List<UUID>) dbLink.get(FieldNames.LEADERBOARD_GROUP_UUID.name());
                for (UUID leaderboardGroupID : leaderboardGroupIDs) {
                    LeaderboardGroup leaderboardGroup = leaderboardGroupResolver
                            .getLeaderboardGroupByID(leaderboardGroupID);
                    if (leaderboardGroup != null) {
                        event.addLeaderboardGroup(leaderboardGroup);
                    }
                }
            }
        }
    }

    @Override
    public Event loadEvent(String name) {
        Event result;
        Document query = new Document();
        query.put(FieldNames.EVENT_NAME.name(), name);
        MongoCollection<Document> eventCollection = database.getCollection(CollectionNames.EVENTS.name());
        Document eventDBObject = eventCollection.find(query).first();
        if (eventDBObject != null) {
            result = loadEvent(eventDBObject);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Iterable<Pair<Event, Boolean>> loadAllEvents() {
        ArrayList<Pair<Event, Boolean>> result = new ArrayList<>();
        MongoCollection<Document> eventCollection = database.getCollection(CollectionNames.EVENTS.name());

        try {
            for (Document object : eventCollection.find()) {
                Event event = loadEvent(object);
                boolean requiresStoreAfterMigration = loadLegacyImageAndVideoURLs(event, object);
                requiresStoreAfterMigration |= loadLegacySailorsInfoWebsiteURL(event, object);
                result.add(new Pair<Event, Boolean>(event, requiresStoreAfterMigration));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load events.");
            logger.log(Level.SEVERE, "loadAllEvents", e);
        }

        return result;
    }

    @Override
    public SailingServerConfiguration loadServerConfiguration() {
        SailingServerConfiguration result;
        MongoCollection<Document> serverCollection = database.getCollection(CollectionNames.SERVER_CONFIGURATION.name());
        Document theServer = serverCollection.find().first();
        if (theServer != null) {
            result = loadServerConfiguration(theServer);
        } else {
            // create a default configuration
            result = new SailingServerConfigurationImpl(false);
        }
        return result;
    }

    private SailingServerConfiguration loadServerConfiguration(Document serverDBObject) {
        boolean isStandaloneServer = (Boolean) serverDBObject.get(FieldNames.SERVER_IS_STANDALONE.name());
        return new SailingServerConfigurationImpl(isStandaloneServer);
    }

    private RemoteSailingServerReference loadRemoteSailingSever(Document serverDBObject) {
        RemoteSailingServerReference result = null;
        final String name = (String) serverDBObject.get(FieldNames.SERVER_NAME.name());
        final Boolean include = (Boolean) serverDBObject.get(FieldNames.INCLUDE.name());
        @SuppressWarnings("unchecked")
        final List<UUID> selectedEventIds = (List<UUID>) serverDBObject.get(FieldNames.SELECTED_EVENT_IDS.name());
        final String urlAsString = (String) serverDBObject.get(FieldNames.SERVER_URL.name());
        try {
            URL serverUrl = new URL(urlAsString);
            // if the INCLUDE field is not present, assume "false", meaning that the list of "selected" events
            // which the most likely is also missing (we assume we're reading an old record that isn't aware
            // of per-event includes/excludes) will therefore exclude no event, leading to backward-compatible
            // behavior of considering all events found across that reference.
            result = new RemoteSailingServerReferenceImpl(name, serverUrl, include == null ? false : include,
                    selectedEventIds == null ? Collections.emptySet() : selectedEventIds);
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Can't load the sailing server with URL " + urlAsString, e);
        }
        return result;
    }

    @Override
    public Iterable<RemoteSailingServerReference> loadAllRemoteSailingServerReferences() {
        ArrayList<RemoteSailingServerReference> result = new ArrayList<RemoteSailingServerReference>();
        MongoCollection<Document> serverCollection = database.getCollection(CollectionNames.SAILING_SERVERS.name());
        try {
            for (Document o : serverCollection.find()) {
                if (loadRemoteSailingSever(o) != null) {
                    result.add(loadRemoteSailingSever(o));
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load sailing server instances URLs.");
            logger.log(Level.SEVERE, "loadAllSailingServers", e);
        }
        return result;
    }

    /**
     * An event doesn't store its regattas; it's the regatta that stores a reference to its event; the regatta needs to
     * add itself to the event when loaded or instantiated.
     */
    private Event loadEvent(Document eventDBObject) {
        String name = (String) eventDBObject.get(FieldNames.EVENT_NAME.name());
        String description = (String) eventDBObject.get(FieldNames.EVENT_DESCRIPTION.name());
        UUID id = (UUID) eventDBObject.get(FieldNames.EVENT_ID.name());
        TimePoint startDate = loadTimePoint(eventDBObject, FieldNames.EVENT_START_DATE);
        TimePoint endDate = loadTimePoint(eventDBObject, FieldNames.EVENT_END_DATE);
        if (endDate != null && startDate != null && endDate.before(startDate)) {
            logger.warning("End date "+endDate+" of event "+name+" with ID "+id+" is before its start date "+
                    startDate+"; adjusting such that end date equals start date.");
            endDate = startDate;
        }
        boolean isPublic = eventDBObject.get(FieldNames.EVENT_IS_PUBLIC.name()) != null
                ? (Boolean) eventDBObject.get(FieldNames.EVENT_IS_PUBLIC.name()) : false;
        Venue venue = loadVenue((Document) eventDBObject.get(FieldNames.VENUE.name()));
        Event result = new EventImpl(name, startDate, endDate, venue, isPublic, id);
        result.setDescription(description);
        @SuppressWarnings("unchecked")
        final List<String> windFinderReviewedSpotCollectionIds = (List<String>) eventDBObject.get(FieldNames.EVENT_WINDFINDER_SPOT_COLLECTION_IDS.name());
        if (windFinderReviewedSpotCollectionIds != null) {
            result.setWindFinderReviewedSpotsCollection(windFinderReviewedSpotCollectionIds);
        }
        String officialWebSiteURLAsString = (String) eventDBObject.get(FieldNames.EVENT_OFFICIAL_WEBSITE_URL.name());
        if (officialWebSiteURLAsString != null) {
            try {
                result.setOfficialWebsiteURL(new URL(officialWebSiteURLAsString));
            } catch (MalformedURLException e) {
                logger.severe("Error parsing official website URL " + officialWebSiteURLAsString + " for event " + name
                        + ". Ignoring this URL.");
            }
        }
        String baseURLAsString = (String) eventDBObject.get(FieldNames.EVENT_BASE_URL.name());
        if (baseURLAsString != null) {
            try {
                result.setBaseURL(new URL(baseURLAsString));
            } catch (MalformedURLException e) {
                logger.severe(
                        "Error parsing base URL " + baseURLAsString + " for event " + name + ". Ignoring this URL.");
            }
        }
        Iterable<?> images = (Iterable<?>) eventDBObject.get(FieldNames.EVENT_IMAGES.name());
        if (images != null) {
            for (Object imageObject : images) {
                ImageDescriptor image = loadImage((Document) imageObject);
                if (image != null) {
                    result.addImage(image);
                }
            }
        }
        Iterable<?> videos = (Iterable<?>) eventDBObject.get(FieldNames.EVENT_VIDEOS.name());
        if (videos != null) {
            for (Object videoObject : videos) {
                VideoDescriptor video = loadVideo((Document) videoObject);
                if (video != null) {
                    result.addVideo(video);
                }
            }
        }
        Iterable<?> sailorsInfoWebsiteURLs = (Iterable<?>) eventDBObject
                .get(FieldNames.EVENT_SAILORS_INFO_WEBSITES.name());
        if (sailorsInfoWebsiteURLs != null) {
            for (Object sailorsInfoWebsiteObject : sailorsInfoWebsiteURLs) {
                Document sailorsInfoWebsiteDBObject = (Document) sailorsInfoWebsiteObject;
                URL url = loadURL(sailorsInfoWebsiteDBObject, FieldNames.SAILORS_INFO_URL);
                String localeRaw = (String) sailorsInfoWebsiteDBObject.get(FieldNames.SAILORS_INFO_LOCALE.name());
                if (url != null) {
                    Locale locale = localeRaw != null ? Locale.forLanguageTag(localeRaw) : null;
                    result.setSailorsInfoWebsiteURL(locale, url);
                }
            }
        }
        return result;
    }

    private Venue loadVenue(Document dbObject) {
        String name = (String) dbObject.get(FieldNames.VENUE_NAME.name());
        Iterable<?> dbCourseAreas = (Iterable<?>) dbObject.get(FieldNames.COURSE_AREAS.name());
        Venue result = new VenueImpl(name);
        for (Object courseAreaDBObject : dbCourseAreas) {
            CourseArea courseArea = loadCourseArea((Document) courseAreaDBObject);
            result.addCourseArea(courseArea);
        }
        return result;
    }

    private CourseArea loadCourseArea(Document courseAreaDBObject) {
        final String name = (String) courseAreaDBObject.get(FieldNames.COURSE_AREA_NAME.name());
        final UUID id = (UUID) courseAreaDBObject.get(FieldNames.COURSE_AREA_ID.name());
        final Document centerPositionDoc = (Document) courseAreaDBObject.get(FieldNames.COURSE_AREA_CENTER_POSITION.name());
        final Position centerPosition;
        final Distance radius;
        if (centerPositionDoc != null) {
            centerPosition = loadPosition(centerPositionDoc);
        } else {
            centerPosition = null;
        }
        final Number radiusNumber = (Number) courseAreaDBObject.get(FieldNames.COURSE_AREA_RADIUS_IN_METERS.name());
        if (radiusNumber != null) {
            radius = new MeterDistance(radiusNumber.doubleValue());
        } else {
            radius = null;
        }
        final CourseArea result = baseDomainFactory.getOrCreateCourseArea(id, name, centerPosition, radius);
        return result;
    }

    @Override
    public Iterable<Regatta> loadAllRegattas(TrackedRegattaRegistry trackedRegattaRegistry) {
        List<Regatta> result = new ArrayList<Regatta>();
        MongoCollection<Document> regattaCollection = database.getCollection(CollectionNames.REGATTAS.name());
        for (Document dbRegatta : regattaCollection.find()) {
            result.add(loadRegatta(dbRegatta, trackedRegattaRegistry));
        }
        return result;
    }

    @Override
    public Regatta loadRegatta(String name, TrackedRegattaRegistry trackedRegattaRegistry) {
        Document query = new Document(FieldNames.REGATTA_NAME.name(), name);
        MongoCollection<Document> regattaCollection = database.getCollection(CollectionNames.REGATTAS.name());
        Document dbRegatta = regattaCollection.find(query).first();
        Regatta result = loadRegatta(dbRegatta, trackedRegattaRegistry);
        assert result == null || result.getName().equals(name);
        return result;
    }

    private Regatta loadRegatta(Document dbRegatta, TrackedRegattaRegistry trackedRegattaRegistry) {
        Regatta result = null;
        if (dbRegatta != null) {
            String name = (String) dbRegatta.get(FieldNames.REGATTA_NAME.name());
            String boatClassName = (String) dbRegatta.get(FieldNames.BOAT_CLASS_NAME.name());
            TimePoint startDate = loadTimePoint(dbRegatta, FieldNames.REGATTA_START_DATE);
            TimePoint endDate = loadTimePoint(dbRegatta, FieldNames.REGATTA_END_DATE);
            Serializable id = (Serializable) dbRegatta.get(FieldNames.REGATTA_ID.name());
            if (id == null) {
                id = name;
            }
            BoatClass boatClass = null;
            if (boatClassName != null) {
                boolean typicallyStartsUpwind = (Boolean) dbRegatta
                        .get(FieldNames.BOAT_CLASS_TYPICALLY_STARTS_UPWIND.name());
                boatClass = baseDomainFactory.getOrCreateBoatClass(boatClassName, typicallyStartsUpwind);
            }
            @SuppressWarnings("unchecked")
            Iterable<Document> dbSeries = (Iterable<Document>) dbRegatta.get(FieldNames.REGATTA_SERIES.name());
            Iterable<Series> series = loadSeries(dbSeries, trackedRegattaRegistry);
            final Iterable<CourseArea> courseAreas = loadCourseAreas(dbRegatta);
            RegattaConfiguration configuration = null;
            if (dbRegatta.containsKey(FieldNames.REGATTA_REGATTA_CONFIGURATION.name())) {
                final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
                try {
                    JSONObject json = Helpers.toJSONObjectSafe(new JSONParser()
                            .parse(((Document) dbRegatta.get(FieldNames.REGATTA_REGATTA_CONFIGURATION.name())).toJson(writerSettings)));
                    configuration = RegattaConfigurationJsonDeserializer.create().deserialize(json);
                } catch (JsonDeserializationException | ParseException e) {
                    logger.log(Level.WARNING, "Error loading racing procedure configration for regatta.", e);
                }
            }
            final Number buoyZoneRadiusInHullLengthsAsNumber = (Number) dbRegatta
                    .get(FieldNames.REGATTA_BUOY_ZONE_RADIUS_IN_HULL_LENGTHS.name());
            final Double buoyZoneRadiusInHullLengths = buoyZoneRadiusInHullLengthsAsNumber==null?null:buoyZoneRadiusInHullLengthsAsNumber.doubleValue();
            final Boolean useStartTimeInference = (Boolean) dbRegatta
                    .get(FieldNames.REGATTA_USE_START_TIME_INFERENCE.name());
            final Boolean controlTrackingFromStartAndFinishTimes = (Boolean) dbRegatta
                    .get(FieldNames.REGATTA_CONTROL_TRACKING_FROM_START_AND_FINISH_TIMES.name());
            final Boolean autoRestartTrackingUponCompetitorSetChange = (Boolean) dbRegatta
                    .get(FieldNames.REGATTA_AUTO_RESTART_TRACKING_UPON_COMPETITOR_SET_CHANGE.name());
            Boolean canBoatsOfCompetitorsChangePerRace = (Boolean) dbRegatta
                    .get(FieldNames.REGATTA_CAN_BOATS_OF_COMPETITORS_CHANGE_PER_RACE.name());
            // for backward compatibility
            boolean createMigratableRegatta = false;
            if (canBoatsOfCompetitorsChangePerRace == null) {
                canBoatsOfCompetitorsChangePerRace = false;
                createMigratableRegatta = true;
            }
            CompetitorRegistrationType competitorRegistrationType = CompetitorRegistrationType
                    .valueOfOrDefault((String) dbRegatta.get(FieldNames.REGATTA_COMPETITOR_REGISTRATION_TYPE.name()));
            final RankingMetricConstructor rankingMetricConstructor = loadRankingMetricConstructor(dbRegatta);
            String registrationLinkSecret = (String) dbRegatta.get(FieldNames.REGATTA_REGISTRATION_LINK_SECRET.name());
            if (createMigratableRegatta) {
                result = new <Series> MigratableRegattaImpl(getRaceLogStore(), getRegattaLogStore(), name, boatClass,
                        canBoatsOfCompetitorsChangePerRace, competitorRegistrationType, startDate, endDate, series, /* persistent */true,
                        loadScoringScheme(dbRegatta), id, courseAreas,
                        buoyZoneRadiusInHullLengths == null ? Regatta.DEFAULT_BUOY_ZONE_RADIUS_IN_HULL_LENGTHS
                                : buoyZoneRadiusInHullLengths,
                        useStartTimeInference == null ? true : useStartTimeInference,
                        controlTrackingFromStartAndFinishTimes == null ? false : controlTrackingFromStartAndFinishTimes,
                                autoRestartTrackingUponCompetitorSetChange == null ? false : autoRestartTrackingUponCompetitorSetChange, rankingMetricConstructor,
                                        new MongoObjectFactoryImpl(database), registrationLinkSecret);
            } else {
                result = new <Series> RegattaImpl(getRaceLogStore(), getRegattaLogStore(), name, boatClass,
                        canBoatsOfCompetitorsChangePerRace, competitorRegistrationType, startDate, endDate, series, /* persistent */true,
                        loadScoringScheme(dbRegatta), id, courseAreas,
                        buoyZoneRadiusInHullLengths == null ? Regatta.DEFAULT_BUOY_ZONE_RADIUS_IN_HULL_LENGTHS
                                : buoyZoneRadiusInHullLengths,
                        useStartTimeInference == null ? true : useStartTimeInference,
                        controlTrackingFromStartAndFinishTimes == null ? false : controlTrackingFromStartAndFinishTimes,
                        autoRestartTrackingUponCompetitorSetChange == null ? false : autoRestartTrackingUponCompetitorSetChange, rankingMetricConstructor, registrationLinkSecret);
            }
            result.setRegattaConfiguration(configuration);
        }
        // all regattas are required to have a secret, to allow tracking in classic scenarios or in open regatta ones
        if (result.getRegistrationLinkSecret() == null) {
            logger.info("Added missing RegistrationLinkSecret to " + result + " and stored to database");
            result.setRegistrationLinkSecret(UUID.randomUUID().toString());
            new MongoObjectFactoryImpl(database).storeRegatta(result);
        }
        return result;
    }

    private RankingMetricConstructor loadRankingMetricConstructor(Document dbRegatta) {
        Document rankingMetricJson = (Document) dbRegatta.get(FieldNames.REGATTA_RANKING_METRIC.name());
        // default is OneDesignRankingMetric
        final RankingMetricConstructor result;
        if (rankingMetricJson == null) {
            result = OneDesignRankingMetric::new;
        } else {
            final String rankingMetricTypeName = (String) rankingMetricJson
                    .get(FieldNames.REGATTA_RANKING_METRIC_TYPE.name());
            result = RankingMetricsFactory.getRankingMetricConstructor(RankingMetrics.valueOf(rankingMetricTypeName));
        }
        return result;
    }

    private ScoringSchemeType getScoringSchemeType(Document dbObject) {
        String scoringSchemeTypeName = (String) dbObject.get(FieldNames.SCORING_SCHEME_TYPE.name());
        ScoringSchemeType scoringSchemeType;
        if (scoringSchemeTypeName == null) {
            scoringSchemeType = ScoringSchemeType.LOW_POINT; // the default
        } else {
            try {
                scoringSchemeType = ScoringSchemeType.valueOf(scoringSchemeTypeName);
            } catch (IllegalArgumentException ila) {
                // can happen that the database contains a scoring scheme that
                // has not yet been implemented - fall back with a warning
                scoringSchemeType = ScoringSchemeType.LOW_POINT;
                logger.warning("Could not find scoring scheme " + scoringSchemeTypeName
                        + "! Most probably this has not yet been implemented or even been removed.");
            }
        }
        return scoringSchemeType;
    }

    private Iterable<Series> loadSeries(Iterable<Document> dbSeries, TrackedRegattaRegistry trackedRegattaRegistry) {
        List<Series> result = new ArrayList<Series>();
        for (Document oneDBSeries : dbSeries) {
            Series series = loadSeries(oneDBSeries, trackedRegattaRegistry);
            result.add(series);
        }
        return result;
    }

    private Series loadSeries(Document dbSeries, TrackedRegattaRegistry trackedRegattaRegistry) {
        String name = (String) dbSeries.get(FieldNames.SERIES_NAME.name());
        boolean isMedal = (Boolean) dbSeries.get(FieldNames.SERIES_IS_MEDAL.name());
        // isFleetCanRunInParallel is a new field -> set to true when it does not exist in older db versions
        boolean isFleetsCanRunInParallel = true;
        Object isFleetCanRunInParallelObject = dbSeries.get(FieldNames.SERIES_IS_FLEETS_CAN_RUN_IN_PARALLEL.name());
        if (isFleetCanRunInParallelObject != null) {
            isFleetsCanRunInParallel = (Boolean) dbSeries.get(FieldNames.SERIES_IS_FLEETS_CAN_RUN_IN_PARALLEL.name());
        }
        final Number maximumNumberOfDiscardsAsObject = (Number) dbSeries
                .get(FieldNames.SERIES_MAXIMUM_NUMBER_OF_DISCARDS.name());
        final Integer maximumNumberOfDiscards = maximumNumberOfDiscardsAsObject == null ? null : maximumNumberOfDiscardsAsObject.intValue();
        final Boolean startsWithZeroScore = (Boolean) dbSeries.get(FieldNames.SERIES_STARTS_WITH_ZERO_SCORE.name());
        final Boolean hasSplitFleetContiguousScoring = (Boolean) dbSeries
                .get(FieldNames.SERIES_HAS_SPLIT_FLEET_CONTIGUOUS_SCORING.name());
        final Boolean hasCrossFleetMergedRankingObject = (Boolean) dbSeries
                .get(FieldNames.SERIES_HAS_CROSS_FLEET_MERGED_RANKING.name());
        final Boolean firstColumnIsNonDiscardableCarryForward = (Boolean) dbSeries
                .get(FieldNames.SERIES_STARTS_WITH_NON_DISCARDABLE_CARRY_FORWARD.name());
        final Boolean oneAlwaysStaysOne = (Boolean) dbSeries.get(FieldNames.SERIES_ONE_ALWAYS_STAYS_ONE.name());
        @SuppressWarnings("unchecked")
        final Iterable<Document> dbFleets = (Iterable<Document>) dbSeries.get(FieldNames.SERIES_FLEETS.name());
        List<Fleet> fleets = loadFleets(dbFleets);
        @SuppressWarnings("unchecked")
        Iterable<Document> dbRaceColumns = (Iterable<Document>) dbSeries.get(FieldNames.SERIES_RACE_COLUMNS.name());
        Iterable<String> raceColumnNames = loadRaceColumnNames(dbRaceColumns);
        Series series = new SeriesImpl(name, isMedal, isFleetsCanRunInParallel, fleets, raceColumnNames,
                trackedRegattaRegistry);
        if (dbSeries.get(FieldNames.SERIES_DISCARDING_THRESHOLDS.name()) != null) {
            ThresholdBasedResultDiscardingRule resultDiscardingRule = loadResultDiscardingRule(dbSeries,
                    FieldNames.SERIES_DISCARDING_THRESHOLDS);
            series.setResultDiscardingRule(resultDiscardingRule);
        }
        if (startsWithZeroScore != null) {
            series.setStartsWithZeroScore(startsWithZeroScore);
        }
        if (hasSplitFleetContiguousScoring != null) {
            series.setSplitFleetContiguousScoring(hasSplitFleetContiguousScoring);
        }
        if (hasCrossFleetMergedRankingObject != null) {
            series.setCrossFleetMergedRanking(hasCrossFleetMergedRankingObject);
        }
        series.setMaximumNumberOfDiscards(maximumNumberOfDiscards);
        if (firstColumnIsNonDiscardableCarryForward != null) {
            series.setFirstColumnIsNonDiscardableCarryForward(firstColumnIsNonDiscardableCarryForward);
        }
        if (oneAlwaysStaysOne != null) {
            series.setOneAlwaysStaysOne(oneAlwaysStaysOne);
        }
        loadRaceColumnRaceLinks(dbRaceColumns, series);
        return series;
    }

    private Iterable<String> loadRaceColumnNames(Iterable<Document> dbRaceColumns) {
        List<String> result = new ArrayList<String>();
        for (Object o : dbRaceColumns) {
            Document dbRaceColumn = (Document) o;
            result.add((String) dbRaceColumn.get(FieldNames.LEADERBOARD_COLUMN_NAME.name()));
        }
        return result;
    }

    private void loadRaceColumnRaceLinks(Iterable<Document> dbRaceColumns, Series series) {
        for (Object o : dbRaceColumns) {
            Document dbRaceColumn = (Document) o;
            String name = (String) dbRaceColumn.get(FieldNames.LEADERBOARD_COLUMN_NAME.name());
            Map<String, RaceIdentifier> raceIdentifiersPerFleetName = loadRaceIdentifiers(dbRaceColumn);
            for (Map.Entry<String, RaceIdentifier> e : raceIdentifiersPerFleetName.entrySet()) {
                // null key for "default" fleet is not acceptable here
                if (e.getKey() == null) {
                    logger.warning("Ignoring null fleet name while loading RaceColumn " + name);
                } else {
                    series.getRaceColumnByName(name).setRaceIdentifier(series.getFleetByName(e.getKey()), e.getValue());
                }
            }
        }
    }

    private List<Fleet> loadFleets(Iterable<Document> dbFleets) {
        List<Fleet> result = new ArrayList<Fleet>();
        for (Document dbFleet : dbFleets) {
            Fleet fleet = loadFleet(dbFleet);
            result.add(fleet);
        }
        return result;
    }

    private Fleet loadFleet(Document dbFleet) {
        Fleet result;
        String name = (String) dbFleet.get(FieldNames.FLEET_NAME.name());
        Number orderingAsNumber = (Number) dbFleet.get(FieldNames.FLEET_ORDERING.name());
        Integer ordering = orderingAsNumber == null ? null : orderingAsNumber.intValue();
        if (ordering == null) {
            ordering = 0;
        }
        Number colorAsNumber = (Number) dbFleet.get(FieldNames.FLEET_COLOR.name());
        Integer colorAsInt = colorAsNumber == null ? null : colorAsNumber.intValue();
        Color color = null;
        if (colorAsInt != null) {
            int r = colorAsInt % 256;
            int g = (colorAsInt / 256) % 256;
            int b = (colorAsInt / 256 / 256) % 256;
            color = new RGBColor(r, g, b);
        }
        result = new FleetImpl(name, ordering, color);
        return result;
    }

    @Override
    public Map<String, Regatta> loadRaceIDToRegattaAssociations(RegattaRegistry regattaRegistry) {
        MongoCollection<Document> raceIDToRegattaCollection = database.getCollection(CollectionNames.REGATTA_FOR_RACE_ID.name());
        Map<String, Regatta> result = new HashMap<String, Regatta>();
        for (Document o : raceIDToRegattaCollection.find()) {
            Regatta regatta = regattaRegistry.getRegattaByName((String) o.get(FieldNames.REGATTA_NAME.name()));
            if (regatta != null) {
                result.put((String) o.get(FieldNames.RACE_ID_AS_STRING.name()), regatta);
            } else {
                logger.warning("Couldn't find regatta " + o.get(FieldNames.REGATTA_NAME.name())
                        + ". Cannot restore race associations with this regatta.");
            }
        }
        return result;
    }

    @Override
    public RaceLog loadRaceLog(RaceLogIdentifier identifier) {
        RaceLog result = new RaceLogImpl(RaceLogImpl.class.getSimpleName(), identifier.getIdentifier());
        try {
            Document query = new Document();
            query.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(identifier.getIdentifier()));
            loadRaceLogEvents(result, query);
        } catch (Throwable t) {
            // something went wrong during DB access; report, then use empty new race log
            logger.log(Level.SEVERE,
                    "Error connecting to MongoDB, unable to load recorded race log data. Check MongoDB settings.");
            logger.log(Level.SEVERE, "loadRaceLog", t);
        }
        return result;
    }

    private List<RaceLogEvent> loadRaceLogEvents(RaceLog targetRaceLog, Document query) throws JsonDeserializationException, ParseException {
        List<RaceLogEvent> result = new ArrayList<>();
        MongoCollection<Document> raceLog = database.getCollection(CollectionNames.RACE_LOGS.name());
        for (Document o : raceLog.find(query)) {
            try {
                Pair<RaceLogEvent, Optional<Document>> raceLogEventAndOptionalUpdateInstructions = loadRaceLogEvent((Document) o.get(FieldNames.RACE_LOG_EVENT.name()));
                final RaceLogEvent raceLogEvent = raceLogEventAndOptionalUpdateInstructions.getA();
                if (raceLogEvent != null) {
                    targetRaceLog.load(raceLogEvent);
                    result.add(raceLogEvent);
                }
                raceLogEventAndOptionalUpdateInstructions.getB().ifPresent(dbObjectForUpdate->{
                    final Document q = new Document("_id", o.get("_id"));
                    o.put(FieldNames.RACE_LOG_EVENT.name(), dbObjectForUpdate);
                    raceLog.replaceOne(q, o);
                });
            } catch (IllegalStateException e) {
                logger.log(Level.SEVERE, "Couldn't load race log event " + o + ": " + e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * @return the race log event read, and an optional {@link Document} that, if present, indicates the need to update
     *         the event representation in the DB, e.g., because of a migration activity. The caller is responsible for
     *         updating this object into the RACE_LOGS collection then because only the caller knows the key surrounding
     *         the event object passed to this method.
     */
    public Pair<RaceLogEvent, Optional<Document>> loadRaceLogEvent(Document dbObject) throws JsonDeserializationException, ParseException {
        TimePoint logicalTimePoint = loadTimePoint(dbObject);
        TimePoint createdAt = loadTimePoint(dbObject, FieldNames.RACE_LOG_EVENT_CREATED_AT);
        Serializable id = (Serializable) dbObject.get(FieldNames.RACE_LOG_EVENT_ID.name());
        Integer passId = (Integer) dbObject.get(FieldNames.RACE_LOG_EVENT_PASS_ID.name());
        @SuppressWarnings("unchecked")
        Iterable<Document> dbCompetitors = (Iterable<Document>) dbObject.get(FieldNames.RACE_LOG_EVENT_INVOLVED_BOATS.name());
        List<Competitor> competitors = loadCompetitorsForRaceLogEvent(dbCompetitors);
        final AbstractLogEventAuthor author;
        String authorName = (String) dbObject.get(FieldNames.RACE_LOG_EVENT_AUTHOR_NAME.name());
        Number authorPriority = (Number) dbObject.get(FieldNames.RACE_LOG_EVENT_AUTHOR_PRIORITY.name());
        if (authorName != null && authorPriority != null) {
            author = new LogEventAuthorImpl(authorName, authorPriority.intValue());
        } else {
            author = LogEventAuthorImpl.createCompatibilityAuthor();
        }
        String eventClass = (String) dbObject.get(FieldNames.RACE_LOG_EVENT_CLASS.name());
        final RaceLogEvent resultEvent;
        Optional<Document> dbObjectForUpdate = Optional.empty();
        if (eventClass.equals(RaceLogStartTimeEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogStartTimeEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogStartOfTrackingEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogStartOfTrackingEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogEndOfTrackingEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogEndOfTrackingEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogDependentStartTimeEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogDependentStartTimeEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogRaceStatusEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogRaceStatusEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogFlagEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogFlagEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogPassChangeEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogPassChangeEvent(createdAt, author, logicalTimePoint, id, passId, competitors);
        } else if (eventClass.equals(RaceLogCourseDesignChangedEvent.class.getSimpleName())) {
            final Pair<RaceLogCourseDesignChangedEvent, Optional<Document>> resultPair = loadRaceLogCourseDesignChangedEvent(
                    createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
            resultEvent = resultPair.getA();
            dbObjectForUpdate = resultPair.getB();
        } else if (eventClass.equals(RaceLogFinishPositioningListChangedEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogFinishPositioningListChangedEvent(createdAt, author, logicalTimePoint, id, passId,
                    competitors, dbObject);
        } else if (eventClass.equals(RaceLogFinishPositioningConfirmedEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogFinishPositioningConfirmedEvent(createdAt, author, logicalTimePoint, id, passId,
                    competitors, dbObject);
        } else if (eventClass.equals(RaceLogPathfinderEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogPathfinderEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogGateLineOpeningTimeEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogGateLineOpeningTimeEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogStartProcedureChangedEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogStartProcedureChangedEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogProtestStartTimeEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogProtestStartTimeEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogWindFixEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogWindFixEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogDenoteForTrackingEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogDenoteForTrackingEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogStartTrackingEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogStartTrackingEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogRevokeEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogRevokeEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogRegisterCompetitorEvent.class.getSimpleName())) {
            final Pair<RaceLogEvent, Optional<Document>> resultPair = loadRaceLogRegisterCompetitorEvent(createdAt,
                    author, logicalTimePoint, id, passId, competitors, dbObject);
            resultEvent = resultPair.getA();
            dbObjectForUpdate = resultPair.getB();
        } else if (eventClass.equals(RaceLogAdditionalScoringInformationEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogAdditionalScoringInformationEvent(createdAt, author, logicalTimePoint, id, passId,
                    competitors, dbObject);
        } else if (eventClass.equals(RaceLogFixedMarkPassingEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogFixedMarkPassingEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogSuppressedMarkPassingsEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogSuppressedMarkPassingsEvent(createdAt, author, logicalTimePoint, id, passId, competitors,
                    dbObject);
        } else if (eventClass.equals(RaceLogUseCompetitorsFromRaceLogEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogUseCompetitorsFromRaceLogEvent(createdAt, author, logicalTimePoint, id, passId,
                    competitors, dbObject);
        } else if (eventClass.equals(RaceLogTagEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogTagEvent(createdAt, author, logicalTimePoint, id, passId, dbObject);
        } else if (eventClass.equals(RaceLogORCLegDataEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogORCLegDataEvent(createdAt, author, logicalTimePoint, id, passId, dbObject);
        } else if (eventClass.equals(RaceLogORCCertificateAssignmentEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogORCCertificateAssignmentEvent(createdAt, author, logicalTimePoint, id, passId, dbObject);
        } else if (eventClass.equals(RaceLogORCScratchBoatEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogORCScratchBoatEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogORCImpliedWindSourceEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogORCSetImpliedWindEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogResultsAreOfficialEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogResultsAreOfficialEvent(createdAt, author, logicalTimePoint, id, passId, competitors, dbObject);
        } else if (eventClass.equals(RaceLogExcludeWindSourcesEvent.class.getSimpleName())) {
            resultEvent = loadRaceLogExcludeWindSourceEvent(createdAt, author, logicalTimePoint, passId, passId, dbObject);
        } else {
            throw new IllegalStateException(String.format("Unknown RaceLogEvent type %s", eventClass));
        }
        return new Pair<>(resultEvent, dbObjectForUpdate);
    }

    private RaceLogEvent loadRaceLogORCScratchBoatEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors, Document dbObject) {
        return new RaceLogORCScratchBoatEventImpl(createdAt, logicalTimePoint, author, id, passId, competitors.get(0));
    }

    private RaceLogEvent loadRaceLogORCSetImpliedWindEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Integer passId,
            List<Competitor> competitors, Document dbObject) throws JsonDeserializationException, ParseException {
        final Document impliedWindSourceDocument = (Document) dbObject.get(FieldNames.ORC_IMPLIED_WIND_SOURCE.name());
        final ImpliedWindSource impliedWindSource;
        if (impliedWindSourceDocument == null) {
            impliedWindSource = null;
        } else {
            final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
            JSONObject json = Helpers.toJSONObjectSafe(new JSONParser().parse(impliedWindSourceDocument.toJson(writerSettings)));
            impliedWindSource = new ImpliedWindSourceDeserializer().deserialize(json);
        }
        return new RaceLogORCImpliedWindSourceEventImpl(createdAt, logicalTimePoint, author, id, passId, impliedWindSource);
    }

    private RaceLogEvent loadRaceLogResultsAreOfficialEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Integer passId,
            List<Competitor> competitors, Document dbObject) throws JsonDeserializationException, ParseException {
        return new RaceLogResultsAreOfficialEventImpl(createdAt, logicalTimePoint, author, id, passId);
    }

    private RaceLogEvent loadRaceLogUseCompetitorsFromRaceLogEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        return new RaceLogUseCompetitorsFromRaceLogEventImpl(createdAt, author, logicalTimePoint, id, passId);
    }

    private RaceLogEvent loadRaceLogWindFixEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        Wind wind = loadWind((Document) dbObject.get(FieldNames.WIND.name()));
        Boolean isMagnetic = (Boolean) dbObject.get(FieldNames.IS_MAGNETIC.name());
        return new RaceLogWindFixEventImpl(createdAt, logicalTimePoint, author, id, passId, wind,
                isMagnetic == null ? true : isMagnetic);
    }

    private RaceLogEvent loadRaceLogDenoteForTrackingEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        String raceName = (String) dbObject.get(FieldNames.RACE_NAME.name());
        BoatClass boatClass = baseDomainFactory
                .getOrCreateBoatClass((String) dbObject.get(FieldNames.BOAT_CLASS_NAME.name()));
        Serializable raceId = (Serializable) dbObject.get(FieldNames.RACE_ID.name());
        return new RaceLogDenoteForTrackingEventImpl(createdAt, logicalTimePoint, author, id, passId, raceName,
                boatClass, raceId);
    }

    private RaceLogEvent loadRaceLogStartTrackingEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        return new RaceLogStartTrackingEventImpl(createdAt, logicalTimePoint, author, id, passId);
    }

    private RaceLogEvent loadRaceLogRevokeEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        Serializable revokedEventId = UUIDHelper
                .tryUuidConversion((Serializable) dbObject.get(FieldNames.RACE_LOG_REVOKED_EVENT_ID.name()));
        String revokedEventType = (String) dbObject.get(FieldNames.RACE_LOG_REVOKED_EVENT_TYPE.name());
        String revokedEventShortInfo = (String) dbObject.get(FieldNames.RACE_LOG_REVOKED_EVENT_SHORT_INFO.name());
        String reason = (String) dbObject.get(FieldNames.RACE_LOG_REVOKED_REASON.name());
        return new RaceLogRevokeEventImpl(createdAt, logicalTimePoint, author, id, passId, revokedEventId,
                revokedEventType, revokedEventShortInfo, reason);
    }

    private Pair<RaceLogEvent, Optional<Document>> loadRaceLogRegisterCompetitorEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        final RaceLogRegisterCompetitorEvent result;
        final Serializable competitorId = (Serializable) dbObject.get(FieldNames.RACE_LOG_COMPETITOR_ID.name());
        final Serializable boatId = (Serializable) dbObject.get(FieldNames.RACE_LOG_BOAT_ID.name());
        final Boat boat = baseDomainFactory.getCompetitorAndBoatStore().getExistingBoatById(boatId);
        // legacy RaceLogRegisterCompetitorEvent's do not have a boatId, it's expected that the
        // corresponding competitors have the type CompetitorWithBoat
        Competitor competitor = baseDomainFactory.getCompetitorAndBoatStore().getExistingCompetitorById(competitorId);
        final Optional<Document> dbObjectForUpdate;
        if (competitor == null) {
            logger.severe("Competitor with ID "+competitorId+" not found; can't register with boat with ID "+boatId+" for race");
            result = null; // the competitor wasn't found
            dbObjectForUpdate = Optional.empty();
        } else if (boatId == null) {
            if (competitor.hasBoat()) {
                // bug4520: this is dangerous if the competitor is migrated later because a TracTrac event with
                // dedicated boat mappings is found that includes this competitor. In this case, the Boat that is
                // referenced here will be removed from the competitor *and* from the DB. An orphaned copy will
                // remain in the RaceLogRegisterCompetitorEventImpl object created here, but when loading this object
                // from the DB the next time the boat ID cannot be resolved anymore. However, during a second run, the
                // else if (boat == null) branch below will be executed because a non-null boat ID was not resolved to
                // a valid boat; a 2nd league boat will then be assigned.
                result = new RaceLogRegisterCompetitorEventImpl(createdAt, logicalTimePoint, author, id, passId, competitor, ((CompetitorWithBoat) competitor).getBoat());
            } else {
                logger.warning("Bug2822: Competitor with ID "+competitorId+" already seems to have been migrated to one without boat."+
                        " But the RaceLogRegisterCompetitorEventImpl event loaded does not specify one either. We'll try to find a boat...");
                // Now comes a hack: we assume here that we're probably migrating a regatta from the 2nd German Sailing League of 2017
                // which is the only case we know of where a few events had boat metadata provided by TracTrac, and other events
                // were tracked using smartphones. We will simply look for the boats of that season that we know the IDs of
                // and cycle through them, six by six, assuming that regatta loading does not happen in parallel
                result = createBoatForSecondGermanLeague2017(createdAt, author, logicalTimePoint, id, passId, competitor);
            }
            dbObjectForUpdate = requestBoatIdUpdateInDB(dbObject, result);
        } else if (boat == null) {
            logger.warning("Bug2822: Competitor with ID "+competitorId+" references boat with ID "+boatId+
                    " which wasn't found; assigning a boat from the 2nd German Sailing League 2017 season and updating this "+
                    getClass().getName()+" event with ID "+id+" in the DB");
            // the a boat was referenced by a non-null boatId, but the boat was not found; this would most likely mean
            // that an earlier migration first loaded and migrated the RaceLogRegisterCompetitorEventImpl event, using the
            // competitor's own boat with its ID, and later the competitor was migrated, clearing and removing its boat.
            // The only case we know of is that of a smartphone-tracked league-style event where no boats were modeled for
            // the smartphone-tracked event but for later events TracTrac tags provided boat data, leading to competitor migration.
            // The only known leaderboards are the first two 2. Bundesliga events of 2017, so same as above: we map the competitors
            // cyclically to the six boats used in that season.
            result = createBoatForSecondGermanLeague2017(createdAt, author, logicalTimePoint, id, passId, competitor);
            dbObjectForUpdate = requestBoatIdUpdateInDB(dbObject, result);
        } else {
            result = createRaceLogRegisterCompetitorEventImpl(createdAt, author, logicalTimePoint, id, passId, boat, competitor);
            dbObjectForUpdate = Optional.empty();
        }
        return new Pair<>(result, dbObjectForUpdate);
    }

    private Optional<Document> requestBoatIdUpdateInDB(Document dbObject, final RaceLogRegisterCompetitorEvent result) {
        final Optional<Document> dbObjectForUpdate;
        // now update the event in the DB:
        dbObject.put(FieldNames.RACE_LOG_BOAT_ID.name(), result.getBoat().getId());
        dbObjectForUpdate = Optional.of(dbObject);
        return dbObjectForUpdate;
    }

    private int secondLeagueBoatCounter = 0;
    /**
     * map the competitors cyclically to the six boats used in the 2017 season of the 2. Segelbundesliga and create
     * the boat with that ID if it doesn't exist yet
     */
    private RaceLogRegisterCompetitorEvent createBoatForSecondGermanLeague2017(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, Competitor competitor) {
        final String sailId = ""+(secondLeagueBoatCounter++ % 6 + 1);
        final String auxiliaryBoatId = "b2567e08-26d9-45c1-b5e0-8c410c8db18b#"+sailId;
        final Boat auxiliaryBoat = baseDomainFactory.getCompetitorAndBoatStore().getOrCreateBoat(auxiliaryBoatId, sailId,
                baseDomainFactory.getOrCreateBoatClass(BoatClassMasterdata.J70.getDisplayName()), sailId, /* color */ null, /* storePersistently */ true);
        return createRaceLogRegisterCompetitorEventImpl(createdAt, author, logicalTimePoint, id, passId, auxiliaryBoat, competitor);
    }

    private RaceLogRegisterCompetitorEvent createRaceLogRegisterCompetitorEventImpl(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, Boat boat, Competitor competitor) {
        final RaceLogRegisterCompetitorEvent result;
        // a boat was explicitly specified; use it
        if (boat != null) {
            result = new RaceLogRegisterCompetitorEventImpl(createdAt, logicalTimePoint, author, id, passId, competitor, boat);
        } else {
            result = null;
        }
        return result;
    }

    private RaceLogEvent loadRaceLogAdditionalScoringInformationEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Integer passId,
            List<Competitor> competitors, Document dbObject) {
        Object additionalScoringInformationTypeInfo = dbObject
                .get(FieldNames.RACE_LOG_ADDITIONAL_SCORING_INFORMATION_TYPE.name());
        AdditionalScoringInformationType informationType = AdditionalScoringInformationType.UNKNOWN;
        if (additionalScoringInformationTypeInfo != null) {
            informationType = AdditionalScoringInformationType.valueOf(additionalScoringInformationTypeInfo.toString());
        } else {
            logger.warning(
                    "Could not find additional scoring information attached to db log for " + dbObject.toString());
        }
        return new RaceLogAdditionalScoringInformationEventImpl(createdAt, logicalTimePoint, author, id, passId,
                informationType);
    }

    private RaceLogEvent loadRaceLogProtestStartTimeEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        TimePoint protestStartTime = loadTimePoint(dbObject, FieldNames.RACE_LOG_PROTEST_START_TIME);
        TimePoint protestEndTime = loadTimePoint(dbObject, FieldNames.RACE_LOG_PROTEST_END_TIME);
        if (protestEndTime == null) {
            // fallback old data
            protestEndTime = protestStartTime.plus(Duration.ONE_MINUTE.times(90));
        }
        TimeRange protestTime = new TimeRangeImpl(protestStartTime, protestEndTime);
        return new RaceLogProtestStartTimeEventImpl(createdAt, logicalTimePoint, author, id, passId, protestTime);
    }

    private RaceLogEvent loadRaceLogStartProcedureChangedEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        RacingProcedureType type = RacingProcedureType
                .valueOf(dbObject.get(FieldNames.RACE_LOG_START_PROCEDURE_TYPE.name()).toString());
        return new RaceLogStartProcedureChangedEventImpl(createdAt, logicalTimePoint, author, id, passId, type);
    }

    private RaceLogEvent loadRaceLogTagEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId,
            Document dbObject) {
        String tag = (String) dbObject.get(FieldNames.RACE_LOG_TAG.name());
        String comment = (String) dbObject.get(FieldNames.RACE_LOG_COMMENT.name());
        String hiddenInfo = (String) dbObject.get(FieldNames.RACE_LOG_HIDDEN_INFO.name());
        String imageUrl = (String) dbObject.get(FieldNames.RACE_LOG_IMAGE_URL.name());
        String resizedImageURL = (String) dbObject.get(FieldNames.RACE_LOG_RESIZED_IMAGE_URL.name());
        return new RaceLogTagEventImpl(tag, comment, hiddenInfo, imageUrl, resizedImageURL, createdAt, logicalTimePoint, author, id, passId);
    }

    private RaceLogEvent loadRaceLogGateLineOpeningTimeEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        Number gateLaunchStopTime = (Number) dbObject.get(FieldNames.RACE_LOG_GATE_LINE_OPENING_TIME.name());
        Number golfDownTime = 0;
        if (dbObject.containsKey(FieldNames.RACE_LOG_GOLF_DOWN_TIME.name())) {
            golfDownTime = (Number) dbObject.get(FieldNames.RACE_LOG_GOLF_DOWN_TIME.name());
        }
        return new RaceLogGateLineOpeningTimeEventImpl(createdAt, logicalTimePoint, author, id, passId,
                gateLaunchStopTime == null ? null : gateLaunchStopTime.longValue(), golfDownTime.longValue());
    }

    private RaceLogEvent loadRaceLogPathfinderEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        String pathfinderId = dbObject.get(FieldNames.RACE_LOG_PATHFINDER_ID.name()).toString();
        return new RaceLogPathfinderEventImpl(createdAt, logicalTimePoint, author, id, passId, pathfinderId);
    }

    private RaceLogEvent loadRaceLogFinishPositioningConfirmedEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        Iterable<?> dbPositionedCompetitorList = (Iterable<?>) dbObject
                .get(FieldNames.RACE_LOG_POSITIONED_COMPETITORS.name());
        CompetitorResults positionedCompetitors = null;
        // When a confirmation event is loaded that does not contain the positioned competitors (this is the case for
        // the ESS events in
        // Singapore and Quingdao) then null should be set for the positionedCompetitors, which is evaluated later on.
        if (dbPositionedCompetitorList != null) {
            positionedCompetitors = loadPositionedCompetitors(dbPositionedCompetitorList);
        }

        return new RaceLogFinishPositioningConfirmedEventImpl(createdAt, logicalTimePoint, author, id, passId,
                positionedCompetitors);
    }

    private RaceLogEvent loadRaceLogFinishPositioningListChangedEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Integer passId,
            List<Competitor> competitors, Document dbObject) {
        Iterable<?> dbPositionedCompetitorList = (Iterable<?>) dbObject
                .get(FieldNames.RACE_LOG_POSITIONED_COMPETITORS.name());
        CompetitorResults positionedCompetitors = loadPositionedCompetitors(dbPositionedCompetitorList);

        return new RaceLogFinishPositioningListChangedEventImpl(createdAt, logicalTimePoint, author, id, passId,
                positionedCompetitors);
    }

    private RaceLogEvent loadRaceLogPassChangeEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors) {
        return new RaceLogPassChangeEventImpl(createdAt, logicalTimePoint, author, id, passId);
    }

    /**
     * @return the second pair component has a Document in case migration was performed and the updated race log event's
     *         {@link Document} representation shall be updated to the DB
     */
    private Pair<RaceLogCourseDesignChangedEvent, Optional<Document>> loadRaceLogCourseDesignChangedEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Integer passId,
            List<Competitor> competitors, Document dbObject) {
        String courseName = (String) dbObject.get(FieldNames.RACE_LOG_COURSE_DESIGN_NAME.name());
        UUID courseOriginatingTemplateId = (UUID) dbObject.get(FieldNames.RACE_LOG_COURSE_ORIGINATING_TEMPLATE_ID.name());
        Pair<CourseBase, Boolean> courseData = loadCourseData((Iterable<?>) dbObject.get(FieldNames.RACE_LOG_COURSE_DESIGN.name()),
                courseName, courseOriginatingTemplateId);

        // load associated roles
        final ArrayList<?> markList = dbObject.get(FieldNames.RACE_LOG_COURSE_ASSOCIATED_ROLES.name(), ArrayList.class);
        if (markList != null) {
            for (final Object entry : markList) {
                if (entry instanceof Document) {
                    final Document entryObject = (Document) entry;
                    final UUID markUUID = UUID.fromString(
                            entryObject.getString(FieldNames.RACE_LOG_COURSE_ASSOCIATED_ROLES_MARK_ID.name()));
                    final Mark mark = baseDomainFactory.getExistingMarkById(markUUID);
                    if (mark != null) {
                        final String roleIdAsStringOrNull = entryObject
                                .getString(FieldNames.RACE_LOG_COURSE_ASSOCIATED_ROLES_ROLE_ID.name());
                        if (roleIdAsStringOrNull != null) {
                            courseData.getA().addRoleMapping(mark, UUID.fromString(roleIdAsStringOrNull));
                        }
                    } else {
                        logger.warning(String.format("Could not resolve mark with id %s for course %s.", markUUID, id));
                    }
                } else {
                    logger.warning(String.format("Unexpected mark entry found for course %s.", id));
                }
            }
        }

        final String courseDesignerModeName = (String) dbObject.get(FieldNames.RACE_LOG_COURSE_DESIGNER_MODE.name());
        final CourseDesignerMode courseDesignerMode = courseDesignerModeName == null ? null
                : CourseDesignerMode.valueOf(courseDesignerModeName);
        return new Pair<>(new RaceLogCourseDesignChangedEventImpl(createdAt, logicalTimePoint, author, id, passId, courseData.getA(),
                courseDesignerMode), courseData.getB() ? /* migrated */ Optional.of(dbObject) : Optional.empty());
    }

    private RaceLogEvent loadRaceLogFixedMarkPassingEvent(TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint,
            Serializable id, Integer passId, List<Competitor> competitors, Document dbObject) {
        TimePoint ofFixedPassing = loadTimePoint(dbObject, FieldNames.TIMEPOINT_OF_FIXED_MARKPASSING);
        Integer zeroBasedIndexOfWaypoint = (Integer) dbObject.get(FieldNames.INDEX_OF_PASSED_WAYPOINT.name());
        return new RaceLogFixedMarkPassingEventImpl(createdAt, logicalTimePoint, author, id, competitors, passId,
                ofFixedPassing, zeroBasedIndexOfWaypoint);
    }

    private RaceLogEvent loadRaceLogSuppressedMarkPassingsEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        Integer zeroBasedIndexOfFirstSuppressedWaypoint = (Integer) dbObject
                .get(FieldNames.INDEX_OF_FIRST_SUPPRESSED_WAYPOINT.name());
        return new RaceLogSuppressedMarkPassingsEventImpl(createdAt, logicalTimePoint, author, id, competitors, passId,
                zeroBasedIndexOfFirstSuppressedWaypoint);
    }

    private CompetitorResults loadPositionedCompetitors(Iterable<?> dbPositionedCompetitorList) {
        CompetitorResultsImpl positionedCompetitors = new CompetitorResultsImpl();
        int rankCounter = 1;
        for (Object object : dbPositionedCompetitorList) {
            Document dbObject = (Document) object;
            final Serializable competitorId = (Serializable) dbObject.get(FieldNames.COMPETITOR_ID.name());
            String competitorDisplayName = (String) dbObject.get(FieldNames.COMPETITOR_DISPLAY_NAME.name());
            String competitorShortName = (String) dbObject.get(FieldNames.COMPETITOR_SHORT_NAME.name());
            String competitorBoatName = (String) dbObject.get(FieldNames.COMPETITOR_BOAT_NAME.name());
            String competitorBoatSailId = (String) dbObject.get(FieldNames.COMPETITOR_BOAT_SAIL_ID.name());
            // At this point we do not retrieve the competitor object since at any point in time, especially after a
            // server restart, the DomainFactory and its competitor
            // cache might be empty. But at this time the race log is loaded from database, so the competitor would be
            // null.
            // By not using the Competitor object retrieved from the DomainFactory we get completely independent from
            // server restarts and the timepoint of loading
            // competitors by tracking providers.
            final Integer rank = (Integer) dbObject.get(FieldNames.LEADERBOARD_RANK.name());
            final String maxPointsReasonString = (String) dbObject.get(FieldNames.LEADERBOARD_SCORE_CORRECTION_MAX_POINTS_REASON.name());
            final MaxPointsReason maxPointsReason = maxPointsReasonString == null ? MaxPointsReason.NONE : MaxPointsReason.valueOf(maxPointsReasonString);
            final Number scoreAsNumber = (Number) dbObject.get(FieldNames.LEADERBOARD_CORRECTED_SCORE.name());
            final Double score = scoreAsNumber == null ? null : scoreAsNumber.doubleValue();
            final Number finishingTimePointAsMillisAsNumber = (Number) dbObject
                    .get(FieldNames.RACE_LOG_FINISHING_TIME_AS_MILLIS.name());
            final Long finishingTimePointAsMillis = finishingTimePointAsMillisAsNumber == null ? null : finishingTimePointAsMillisAsNumber.longValue();
            final TimePoint finishingTime = finishingTimePointAsMillis == null ? null
                    : new MillisecondsTimePoint(finishingTimePointAsMillis);
            final String comment = (String) dbObject.get(FieldNames.LEADERBOARD_SCORE_CORRECTION_COMMENT.name());
            final String mergeStateAsString = (String) dbObject.get(FieldNames.LEADERBOARD_SCORE_CORRECTION_MERGE_STATE.name());
            final MergeState mergeState;
            if (mergeStateAsString == null) {
                mergeState = MergeState.OK;
            } else {
                mergeState = MergeState.valueOf(mergeStateAsString);
            }
            CompetitorResultImpl positionedCompetitor = new CompetitorResultImpl(competitorId, competitorDisplayName,
                    competitorShortName, competitorBoatName, competitorBoatSailId, rank == null ? rankCounter : rank,
                            maxPointsReason, score, finishingTime, comment, mergeState);
            positionedCompetitors.add(positionedCompetitor);
            rankCounter++;
        }
        return positionedCompetitors;
    }

    private List<Competitor> loadCompetitorsForRaceLogEvent(Iterable<Document> dbCompetitorList) {
        List<Competitor> competitors = new ArrayList<Competitor>();
        for (Object object : dbCompetitorList) {
            Serializable competitorId = (Serializable) object;
            Competitor competitor = baseDomainFactory.getCompetitorAndBoatStore().getExistingCompetitorById(competitorId);
            competitors.add(competitor);
        }
        return competitors;
    }

    private RaceLogFlagEvent loadRaceLogFlagEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        Flags upperFlag = Flags.valueOf((String) dbObject.get(FieldNames.RACE_LOG_EVENT_FLAG_UPPER.name()));
        Flags lowerFlag = Flags.valueOf((String) dbObject.get(FieldNames.RACE_LOG_EVENT_FLAG_LOWER.name()));
        Boolean displayed = Boolean.valueOf((String) dbObject.get(FieldNames.RACE_LOG_EVENT_FLAG_DISPLAYED.name()));

        if (upperFlag == null || lowerFlag == null || displayed == null) {
            return null;
        }

        return new RaceLogFlagEventImpl(createdAt, logicalTimePoint, author, id, passId, upperFlag, lowerFlag,
                displayed);
    }

    private RaceLogStartTimeEvent loadRaceLogStartTimeEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        final TimePoint startTime = loadTimePoint(dbObject, FieldNames.RACE_LOG_EVENT_START_TIME);
        final UUID courseAreaId = loadCourseAreaId(dbObject);
        final RaceLogRaceStatus nextStatus = RaceLogRaceStatus
                .valueOf((String) dbObject.get(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name()));
        return new RaceLogStartTimeEventImpl(createdAt, logicalTimePoint, author, id, passId, startTime, nextStatus, courseAreaId);
    }

    private UUID loadCourseAreaId(Document dbObject) {
        final String courseAreaIdAsString = (String) dbObject.get(FieldNames.RACE_LOG_EVENT_COURSE_AREA_ID_AS_STRING.name());
        final UUID courseAreaId = courseAreaIdAsString == null ? null : UUID.fromString(courseAreaIdAsString);
        return courseAreaId;
    }

    private RaceLogStartOfTrackingEvent loadRaceLogStartOfTrackingEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Integer passId,
            List<Competitor> competitors, Document dbObject) {
        return new RaceLogStartOfTrackingEventImpl(createdAt, logicalTimePoint, author, id, passId);
    }

    private RaceLogEndOfTrackingEvent loadRaceLogEndOfTrackingEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        return new RaceLogEndOfTrackingEventImpl(createdAt, logicalTimePoint, author, id, passId);
    }

    private RaceLogDependentStartTimeEvent loadRaceLogDependentStartTimeEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Integer passId,
            List<Competitor> competitors, Document dbObject) {
        final Object regattaLikeNameObject = dbObject.get(FieldNames.RACE_LOG_DEPDENDENT_ON_REGATTALIKE.name());
        final String regattaLikeName = regattaLikeNameObject == null ? null : regattaLikeNameObject.toString();
        final Object raceColumnNameObject = dbObject.get(FieldNames.RACE_LOG_DEPDENDENT_ON_RACECOLUMN.name());
        final String raceColumnName = raceColumnNameObject == null ? null : raceColumnNameObject.toString();
        final Object fleetNameObject = dbObject.get(FieldNames.RACE_LOG_DEPDENDENT_ON_FLEET.name());
        final String fleetName = fleetNameObject == null ? null : fleetNameObject.toString();
        final SimpleRaceLogIdentifier dependentRaceLog = new SimpleRaceLogIdentifierImpl(regattaLikeName,
                raceColumnName, fleetName);
        final Object startTimeDifferenceObject = dbObject.get(FieldNames.RACE_LOG_START_TIME_DIFFERENCE_IN_MS.name());
        final Duration startTimeDifference = startTimeDifferenceObject == null ? null
                : new MillisecondsDurationImpl(((Number) startTimeDifferenceObject).longValue());
        final RaceLogRaceStatus nextStatus = RaceLogRaceStatus
                .valueOf((String) dbObject.get(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name()));
        final UUID courseAreaId = loadCourseAreaId(dbObject);
        return new RaceLogDependentStartTimeEventImpl(createdAt, logicalTimePoint, author, id, passId, dependentRaceLog,
                startTimeDifference, nextStatus, courseAreaId);
    }

    private RaceLogRaceStatusEvent loadRaceLogRaceStatusEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, List<Competitor> competitors,
            Document dbObject) {
        RaceLogRaceStatus nextStatus = RaceLogRaceStatus
                .valueOf((String) dbObject.get(FieldNames.RACE_LOG_EVENT_NEXT_STATUS.name()));
        return new RaceLogRaceStatusEventImpl(createdAt, logicalTimePoint, author, id, passId, nextStatus);
    }

    private RaceLogORCLegDataEvent loadRaceLogORCLegDataEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Integer passId, Document dbObject) {
        final int legNr = ((Number) dbObject.get(FieldNames.ORC_LEG_NR.name())).intValue();
        final Number twaInDegrees = (Number) dbObject.get(FieldNames.ORC_LEG_TWA_IN_DEG.name());
        final Bearing twa = twaInDegrees==null?null:new DegreeBearingImpl(twaInDegrees.doubleValue());
        final Number lengthInNauticalMiles = (Number) dbObject.get(FieldNames.ORC_LEG_LENGTH_IN_NAUTICAL_MILES.name());
        final Distance length = lengthInNauticalMiles==null?null:new NauticalMileDistance(lengthInNauticalMiles.doubleValue());
        final ORCPerformanceCurveLegTypes type = ORCPerformanceCurveLegTypes.valueOf(dbObject.getString(FieldNames.ORC_LEG_TYPE.name()));
        return new RaceLogORCLegDataEventImpl(createdAt, logicalTimePoint, author, id, passId, legNr, twa, length, type);
    }

    private RaceLogORCCertificateAssignmentEvent loadRaceLogORCCertificateAssignmentEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, int passId, Document dbObject)
            throws JsonDeserializationException, ParseException {
        final Document certificateDbObject = (Document) dbObject.get(FieldNames.ORC_CERTIFICATE.name());
        final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
        JSONObject json = Helpers.toJSONObjectSafe(new JSONParser().parse(certificateDbObject.toJson(writerSettings)));
        final ORCCertificate certificate = new ORCCertificateJsonDeserializer().deserialize(json);
        Serializable boatId = (Serializable) dbObject.get(FieldNames.RACE_LOG_BOAT_ID.name());
        return new RaceLogORCCertificateAssignmentEventImpl(createdAt, logicalTimePoint, author, id, passId, certificate, boatId);
    }

    private RaceLogExcludeWindSourcesEvent loadRaceLogExcludeWindSourceEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, int passId, Document dbObject)
            throws JsonDeserializationException, ParseException {
        final Set<WindSource> windSourcesToExclude = new HashSet<>();
        final Iterable<?> dbWindSourcesToExclude = (Iterable<?>) dbObject.get(FieldNames.WIND_SOURCES_TO_EXCLUDE.name());
        for (final Object dbWindSourceToExcludeObject : dbWindSourcesToExclude) {
            final Document dbWindSourceToExclude = (Document) dbWindSourceToExcludeObject;
            WindSourceType windSourceType = WindSourceType.valueOf((String) dbWindSourceToExclude.get(FieldNames.WIND_SOURCE_NAME.name()));
            final WindSource windSourceToExclude;
            if (dbWindSourceToExclude.containsKey(FieldNames.WIND_SOURCE_ID.name())) {
                windSourceToExclude = new WindSourceWithAdditionalID(windSourceType,
                        (String) dbWindSourceToExclude.get(FieldNames.WIND_SOURCE_ID.name()));
            } else {
                windSourceToExclude = new WindSourceImpl(windSourceType);
            }
            windSourcesToExclude.add(windSourceToExclude);
        }
        return new RaceLogExcludeWindSourcesEventImpl(createdAt, logicalTimePoint, author, id, passId, windSourcesToExclude);
    }

    @Override
    public RegattaLog loadRegattaLog(RegattaLikeIdentifier identifier) {
        RegattaLog result = new RegattaLogImpl(RegattaLogImpl.class.getSimpleName(), identifier);
        try {
            Document query = new Document();
            query.put(FieldNames.REGATTA_LOG_IDENTIFIER_TYPE.name(), identifier.getIdentifierType());
            query.put(FieldNames.REGATTA_LOG_IDENTIFIER_NAME.name(), identifier.getName());
            loadRegattaLogEvents(result, query, identifier);
        } catch (Throwable t) {
            // something went wrong during DB access; report, then use empty new regatta log
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load recorded regatta log data for "
                    + identifier + ". Check MongoDB settings.", t);
        }
        return result;
    }

    private void loadRegattaLogEvents(RegattaLog targetRegattaLog, Document query,
            RegattaLikeIdentifier regattaLogIdentifier) throws JsonDeserializationException, ParseException {
        MongoCollection<Document> collection = database.getCollection(CollectionNames.REGATTA_LOGS.name());
        for (Document o : collection.find(query)) {
            try {
                RegattaLogEvent event = loadRegattaLogEvent(o, regattaLogIdentifier);
                if (event != null) {
                    targetRegattaLog.load(event);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Couldn't load regatta log event " + o + ": " + e.getMessage(), e);
            }
        }
    }

    public RegattaLogEvent loadRegattaLogEvent(Document o, RegattaLikeIdentifier regattaLogIdentifier)
            throws JsonDeserializationException, ParseException {
        Document dbObject = (Document) o.get(FieldNames.REGATTA_LOG_EVENT.name());
        TimePoint logicalTimePoint = loadTimePoint(dbObject);
        TimePoint createdAt = loadTimePoint(dbObject, FieldNames.REGATTA_LOG_EVENT_CREATED_AT);
        Serializable id = (Serializable) dbObject.get(FieldNames.REGATTA_LOG_EVENT_ID.name());
        final AbstractLogEventAuthor author;
        String authorName = (String) dbObject.get(FieldNames.REGATTA_LOG_EVENT_AUTHOR_NAME.name());
        Number authorPriority = (Number) dbObject.get(FieldNames.REGATTA_LOG_EVENT_AUTHOR_PRIORITY.name());
        if (authorName != null && authorPriority != null) {
            author = new LogEventAuthorImpl(authorName, authorPriority.intValue());
        } else {
            author = LogEventAuthorImpl.createCompatibilityAuthor();
        }
        // CloseOpenEnded, DeviceCompMapping, DeviceMarkMapping, RegisterComp, Revoke
        String eventClass = (String) dbObject.get(FieldNames.REGATTA_LOG_EVENT_CLASS.name());
        if (eventClass.equals(RegattaLogDeviceCompetitorMappingEvent.class.getSimpleName())) {
            return loadRegattaLogDeviceCompetitorMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceBoatMappingEvent.class.getSimpleName())) {
            return loadRegattaLogDeviceBoatMappingEvent(createdAt, author, logicalTimePoint, id, dbObject, regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceCompetitorBravoMappingEventImpl.class.getSimpleName())) {
            return loadRegattaLogDeviceCompetitorBravoMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceCompetitorBravoExtendedMappingEventImpl.class.getSimpleName())) {
            return loadRegattaLogDeviceCompetitorBravoExtendedMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl.class.getSimpleName())) {
            return loadRegattaLogDeviceCompetitorExpeditionExtendedMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceBoatBravoMappingEventImpl.class.getSimpleName())) {
            return loadRegattaLogDeviceBoatBravoMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceBoatBravoExtendedMappingEventImpl.class.getSimpleName())) {
            return loadRegattaLogDeviceBoatBravoExtendedMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl.class.getSimpleName())) {
            return loadRegattaLogDeviceBoatExpeditionExtendedMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogDeviceMarkMappingEvent.class.getSimpleName())) {
            return loadRegattaLogDeviceMarkMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                    regattaLogIdentifier, o);
        } else if (eventClass.equals(RegattaLogCloseOpenEndedDeviceMappingEvent.class.getSimpleName())) {
            return loadRegattaLogCloseOpenEndedDeviceMappingEvent(createdAt, author, logicalTimePoint, id, dbObject);
        } else if (eventClass.equals(RegattaLogRegisterBoatEvent.class.getSimpleName())) {
            return loadRegattaLogRegisterBoatEvent(createdAt, author, logicalTimePoint, id, dbObject);
        } else if (eventClass.equals(RegattaLogRegisterCompetitorEvent.class.getSimpleName())) {
            return loadRegattaLogRegisterCompetitorEvent(createdAt, author, logicalTimePoint, id, dbObject);
        } else if (eventClass.equals(RegattaLogSetCompetitorTimeOnTimeFactorEvent.class.getSimpleName())) {
            return loadRegattaLogSetCompetitorTimeOnTimeFactorEvent(createdAt, author, logicalTimePoint, id, dbObject);
        } else if (eventClass
                .equals(RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEvent.class.getSimpleName())) {
            return loadRegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEvent(createdAt, author,
                    logicalTimePoint, id, dbObject);
        } else if (eventClass.equals(RegattaLogDefineMarkEvent.class.getSimpleName())) {
            return loadRegattaLogDefineMarkEvent(createdAt, author, logicalTimePoint, id, dbObject);
        } else if (eventClass.equals(RegattaLogRevokeEvent.class.getSimpleName())) {
            return loadRegattaLogRevokeEvent(createdAt, author, logicalTimePoint, id, dbObject);
        } else if (eventClass.equals(RegattaLogORCCertificateAssignmentEvent.class.getSimpleName())) {
            return loadRegattaLogORCCertificateAssignmentEvent(createdAt, author, logicalTimePoint, id, dbObject);
        }
        throw new IllegalStateException(String.format("Unknown RegattaLogEvent type %s", eventClass));
    }

    private Competitor getCompetitorByID(Document dbObject) {
        Serializable competitorId = (Serializable) dbObject.get(FieldNames.REGATTA_LOG_COMPETITOR_ID.name());
        Competitor comp = baseDomainFactory.getCompetitorAndBoatStore().getExistingCompetitorById(competitorId);
        return comp;
    }

    private Boat getBoatByID(Document dbObject) {
        Serializable boatId = (Serializable) dbObject.get(FieldNames.REGATTA_LOG_BOAT_ID.name());
        Boat boat = baseDomainFactory.getCompetitorAndBoatStore().getExistingBoatById(boatId);
        return boat;
    }

    private RegattaLogEvent loadRegattaLogSetCompetitorTimeOnTimeFactorEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Document dbObject) {
        final Competitor comp = getCompetitorByID(dbObject);
        final Number timeOnTimeFactorAsNumber = (Number) dbObject.get(FieldNames.REGATTA_LOG_TIME_ON_TIME_FACTOR.name());
        final Double timeOnTimeFactor = timeOnTimeFactorAsNumber == null ? null : timeOnTimeFactorAsNumber.doubleValue();
        return new RegattaLogSetCompetitorTimeOnTimeFactorEventImpl(createdAt, logicalTimePoint, author, id, comp,
                timeOnTimeFactor);
    }

    private RegattaLogEvent loadRegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Document dbObject) {
        final Competitor comp = getCompetitorByID(dbObject);
        final Number timeOnDistanceSecondsAllowancePerNauticalMileAsNumber = (Number) dbObject
                .get(FieldNames.REGATTA_LOG_TIME_ON_DISTANCE_SECONDS_ALLOWANCE_PER_NAUTICAL_MILE.name());
        final Double timeOnDistanceSecondsAllowancePerNauticalMile = timeOnDistanceSecondsAllowancePerNauticalMileAsNumber == null
                ? null : timeOnDistanceSecondsAllowancePerNauticalMileAsNumber.doubleValue();
        final Duration timeOnDistanceAllowancePerNauticalMile = timeOnDistanceSecondsAllowancePerNauticalMile == null
                ? null : new MillisecondsDurationImpl((long) (timeOnDistanceSecondsAllowancePerNauticalMile * 1000));
        return new RegattaLogSetCompetitorTimeOnDistanceAllowancePerNauticalMileEventImpl(createdAt, logicalTimePoint,
                author, id, comp, timeOnDistanceAllowancePerNauticalMile);
    }

    private RegattaLogRevokeEvent loadRegattaLogRevokeEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Document dbObject) {
        Serializable revokedEventId = UUIDHelper
                .tryUuidConversion((Serializable) dbObject.get(FieldNames.REGATTA_LOG_REVOKED_EVENT_ID.name()));
        String revokedEventType = (String) dbObject.get(FieldNames.REGATTA_LOG_REVOKED_EVENT_TYPE.name());
        String revokedEventShortInfo = (String) dbObject.get(FieldNames.REGATTA_LOG_REVOKED_EVENT_SHORT_INFO.name());
        String reason = (String) dbObject.get(FieldNames.REGATTA_LOG_REVOKED_REASON.name());
        return new RegattaLogRevokeEventImpl(createdAt, logicalTimePoint, author, id, revokedEventId, revokedEventType,
                revokedEventShortInfo, reason);
    }

    private RegattaLogEvent loadRegattaLogDefineMarkEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Document dbObject) {
        Mark mark = loadMark((Document) dbObject.get(FieldNames.REGATTA_LOG_MARK.name()));
        return new RegattaLogDefineMarkEventImpl(createdAt, author, logicalTimePoint, id, mark);
    }

    private RegattaLogRegisterCompetitorEvent loadRegattaLogRegisterCompetitorEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Document dbObject) {
        Competitor comp = getCompetitorByID(dbObject);
        final RegattaLogRegisterCompetitorEvent result;
        if (comp == null) {
            result = null;
            logger.log(Level.SEVERE,
                    "Couldn't resolve competitor with ID " + dbObject.get(FieldNames.REGATTA_LOG_COMPETITOR_ID.name())
                            + " from registration event with ID " + id + ". Skipping this competitor registration.");
        } else {
            result = new RegattaLogRegisterCompetitorEventImpl(createdAt, logicalTimePoint, author, id, comp);
        }
        return result;
    }

    private RegattaLogRegisterBoatEvent loadRegattaLogRegisterBoatEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Document dbObject) {
        Boat boat = getBoatByID(dbObject);
        final RegattaLogRegisterBoatEvent result;
        if (boat == null) {
            result = null;
            logger.log(Level.SEVERE,
                    "Couldn't resolve boat with ID " + dbObject.get(FieldNames.REGATTA_LOG_BOAT_ID.name())
                            + " from registration event with ID " + id + ". Skipping this boat registration.");
        } else {
            result = new RegattaLogRegisterBoatEventImpl(createdAt, logicalTimePoint, author, id, boat);
        }
        return result;
    }

    private RegattaLogCloseOpenEndedDeviceMappingEvent loadRegattaLogCloseOpenEndedDeviceMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            Document dbObject) {
        Serializable deviceMappingEventId = UUIDHelper
                .tryUuidConversion((Serializable) dbObject.get(FieldNames.REGATTA_LOG_DEVICE_MAPPING_EVENT_ID.name()));
        TimePoint closingTimePointInclusive = loadTimePoint(dbObject, FieldNames.REGATTA_LOG_CLOSING_TIMEPOINT);
        return new RegattaLogCloseOpenEndedDeviceMappingEventImpl(createdAt, author, logicalTimePoint, id,
                deviceMappingEventId, closingTimePointInclusive);
    }

    private RegattaLogDeviceMarkMappingEvent loadRegattaLogDeviceMarkMappingEvent(TimePoint createdAt, AbstractLogEventAuthor author,
            TimePoint logicalTimePoint, Serializable id, Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return this.loadRegattaLogDeviceMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject, () -> loadMark((Document) dbObject.get(FieldNames.MARK.name())),
                RegattaLogDeviceMarkMappingEventImpl::new,
                result -> new MongoObjectFactoryImpl(database, serviceFinderFactory)
                        .storeRegattaLogEvent(regattaLogIdentifier, result));
    }

    private RegattaLogORCCertificateAssignmentEvent loadRegattaLogORCCertificateAssignmentEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, Document dbObject)
            throws JsonDeserializationException, ParseException {
        final Document certificateDbObject = (Document) dbObject.get(FieldNames.ORC_CERTIFICATE.name());
        final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
        JSONObject json = Helpers.toJSONObjectSafe(new JSONParser().parse(certificateDbObject.toJson(writerSettings)));
        final ORCCertificate certificate = new ORCCertificateJsonDeserializer().deserialize(json);
        Serializable boatId = (Serializable) dbObject.get(FieldNames.RACE_LOG_BOAT_ID.name());
        return new RegattaLogORCCertificateAssignmentEventImpl(createdAt, logicalTimePoint, author, id, certificate, boatId);
    }

    /**
     * Loads a from and a to time point from <code>fromField</code> and <code>toField</code> of <code>dbObject</code>.
     * If the <code>fromField</code> is not found, the <code>fromFieldDeprecated</code> is attempted. If found,
     * migration is deemed necessary, expressed by returning <code>true</code> in the {@link Triple#getC()} component of
     * the result. Same for the to-field.
     *
     * @return the from-time in {@link Triple#getA()}, the to-time in {@link Triple#getB()} and whether or not migration
     *         is necessary because a value was only found in a deprecated field in {@link Triple#getC()}.
     */
    private Triple<TimePoint, TimePoint, Boolean> loadFromToTimePoint(final Document dbObject, FieldNames fromField,
            FieldNames fromFieldDeprecated, FieldNames toField, FieldNames toFieldDeprecated) {
        boolean needsMigration = false;
        TimePoint from = loadTimePoint(dbObject, fromField);
        if (from == null) {
            // see bug 2733: erroneously, some records before the fix were written using RACE_LOG_FROM instead of
            // REGATTA_LOG_FROM
            // If such a case is found here, migrate the record.
            from = loadTimePoint(dbObject, fromFieldDeprecated);
            if (from != null) {
                needsMigration = true;
            }
        }
        TimePoint to = loadTimePoint(dbObject, toField);
        if (to == null) {
            // see bug 2733: erroneously, some records before the fix were written using RACE_LOG_FROM instead of
            // REGATTA_LOG_FROM
            // If such a case is found here, migrate the record.
            to = loadTimePoint(dbObject, toFieldDeprecated);
            if (to != null) {
                needsMigration = true;
            }
        }
        return new Triple<>(from, to, needsMigration);
    }

    private RegattaLogDeviceCompetitorMappingEvent loadRegattaLogDeviceCompetitorMappingEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, final Document dbObject,
            RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return this.loadRegattaLogDeviceMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject,
                () -> baseDomainFactory
                        .getExistingCompetitorById((Serializable) dbObject.get(FieldNames.COMPETITOR_ID.name())),
                RegattaLogDeviceCompetitorMappingEventImpl::new,
                result -> new MongoObjectFactoryImpl(database, serviceFinderFactory)
                        .storeRegattaLogEvent(regattaLogIdentifier, result));
    }

    private RegattaLogDeviceBoatMappingEvent loadRegattaLogDeviceBoatMappingEvent(TimePoint createdAt,
            AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id, final Document dbObject,
            RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return this.loadRegattaLogDeviceMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject,
                () -> baseDomainFactory.getCompetitorAndBoatStore()
                        .getExistingBoatById((Serializable) dbObject.get(FieldNames.RACE_LOG_BOAT_ID.name())),
                RegattaLogDeviceBoatMappingEventImpl::new,
                result -> new MongoObjectFactoryImpl(database, serviceFinderFactory)
                        .storeRegattaLogEvent(regattaLogIdentifier, result));
    }

    private RegattaLogDeviceCompetitorBravoMappingEventImpl loadRegattaLogDeviceCompetitorBravoMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return loadRegattaLogDeviceCompetitorSensorDataMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject, RegattaLogDeviceCompetitorBravoMappingEventImpl::new);
    }

    private RegattaLogDeviceCompetitorBravoExtendedMappingEventImpl loadRegattaLogDeviceCompetitorBravoExtendedMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return loadRegattaLogDeviceCompetitorSensorDataMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject, RegattaLogDeviceCompetitorBravoExtendedMappingEventImpl::new);
    }

    private RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl loadRegattaLogDeviceCompetitorExpeditionExtendedMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return loadRegattaLogDeviceCompetitorSensorDataMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject, RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl::new);
    }

    private <MappingT extends RegattaLogDeviceCompetitorSensorDataMappingEvent> MappingT loadRegattaLogDeviceCompetitorSensorDataMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject,
            RegattaLogDeviceMappingEventImpl.Factory<Competitor, MappingT> factory) {
        return this.<Competitor, MappingT>loadRegattaLogDeviceMappingEvent(createdAt, author, logicalTimePoint, id,
                dbObject, regattaLogIdentifier, outerDBObject,
                () -> baseDomainFactory
                        .getExistingCompetitorById((Serializable) dbObject.get(FieldNames.COMPETITOR_ID.name())),
                factory, result -> new MongoObjectFactoryImpl(database, serviceFinderFactory)
                        .storeRegattaLogEvent(regattaLogIdentifier, result));
    }

    private RegattaLogDeviceBoatBravoMappingEventImpl loadRegattaLogDeviceBoatBravoMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return loadRegattaLogDeviceBoatSensorDataMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject, RegattaLogDeviceBoatBravoMappingEventImpl::new);
    }

    private RegattaLogDeviceBoatBravoExtendedMappingEventImpl loadRegattaLogDeviceBoatBravoExtendedMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return loadRegattaLogDeviceBoatSensorDataMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject, RegattaLogDeviceBoatBravoExtendedMappingEventImpl::new);
    }

    private RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl loadRegattaLogDeviceBoatExpeditionExtendedMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject) {
        return loadRegattaLogDeviceBoatSensorDataMappingEvent(createdAt, author, logicalTimePoint, id, dbObject,
                regattaLogIdentifier, outerDBObject, RegattaLogDeviceBoatExpeditionExtendedMappingEventImpl::new);
    }

    private <MappingT extends RegattaLogDeviceBoatSensorDataMappingEvent> MappingT loadRegattaLogDeviceBoatSensorDataMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject,
            RegattaLogDeviceMappingEventImpl.Factory<Boat, MappingT> factory) {
        return this.<Boat, MappingT>loadRegattaLogDeviceMappingEvent(createdAt, author, logicalTimePoint, id,
                dbObject, regattaLogIdentifier, outerDBObject,
                () -> baseDomainFactory
                .getExistingBoatById((Serializable) dbObject.get(FieldNames.RACE_LOG_BOAT_ID.name())),
                factory, result -> new MongoObjectFactoryImpl(database, serviceFinderFactory)
                .storeRegattaLogEvent(regattaLogIdentifier, result));
    }

    private <ItemType extends WithID, MappingT extends RegattaLogDeviceMappingEvent<ItemType>> MappingT loadRegattaLogDeviceMappingEvent(
            TimePoint createdAt, AbstractLogEventAuthor author, TimePoint logicalTimePoint, Serializable id,
            final Document dbObject, RegattaLikeIdentifier regattaLogIdentifier, Document outerDBObject, Supplier<ItemType> itemResolver,
            RegattaLogDeviceMappingEventImpl.Factory<ItemType, MappingT> factory, Consumer<MappingT> storeCallback) {
        DeviceIdentifier device = null;
        try {
            device = loadDeviceId(deviceIdentifierServiceFinder, (Document) dbObject.get(FieldNames.DEVICE_ID.name()));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not load deviceId for RaceLogEvent", e);
        }
        ItemType mappedTo = itemResolver.get();
        @SuppressWarnings("deprecation")
        // used only for auto-migration; may be removed in future releases
        final FieldNames deprecatedFromFieldName = FieldNames.RACE_LOG_FROM;
        @SuppressWarnings("deprecation")
        // used only for auto-migration; may be removed in future releases
        final FieldNames deprecatedToFieldName = FieldNames.RACE_LOG_TO;
        Triple<TimePoint, TimePoint, Boolean> times = loadFromToTimePoint(dbObject, FieldNames.REGATTA_LOG_FROM,
                deprecatedFromFieldName, FieldNames.REGATTA_LOG_TO, deprecatedToFieldName);
        final TimePoint from = times.getA();
        final TimePoint to = times.getB();
        final boolean needsMigration = times.getC();
        final MappingT result = factory.create(
                createdAt, logicalTimePoint, author, id, mappedTo, device, from, to);
        if (needsMigration) {
            // remove old version of mapping event
            DeleteResult removeResult = database.getCollection(CollectionNames.REGATTA_LOGS.name())
                    .deleteMany(outerDBObject);
            assert removeResult.getDeletedCount() == 1;
            // and then insert using the fixed storage implementation
            storeCallback.accept(result);
        }
        return result;
    }

    /**
     * The old field name WAYPOINT_PASSINGSIDE has been replaced by WAYPOINT_PASSINGINSTRUCTIONS. If a race with the old
     * field is loaded, the value of PASSINGSIDE is used and then migrated to PASSINGINSTRUCTION. If the first or last
     * Waypoint has the PassingInstructions Gate, it is transfered to Line.
     *
     * @return the second component tells if migration was performed; in this case the {@code dbCourseList} passed has
     *         been edited "in place" to describe the migration that has happened
     */
    @SuppressWarnings("deprecation") // Used to migrate from PASSINGSIDE to the new PASSINGINSTRUCTIONS
    private Pair<CourseBase, Boolean> loadCourseData(Iterable<?> dbCourseList, String courseName,
            UUID originatingCourseTemplateId) {
        boolean migrated = false;
        if (courseName == null) {
            courseName = "Course";
        }
        CourseBase courseData = new CourseDataImpl(courseName, originatingCourseTemplateId);
        int i = 0;
        for (Object object : dbCourseList) {
            Document dbObject = (Document) object;
            Waypoint waypoint = null;
            PassingInstruction passingInstructions = null;
            String waypointPassingInstruction = (String) dbObject.get(FieldNames.WAYPOINT_PASSINGINSTRUCTIONS.name());
            if (waypointPassingInstruction == null) {
                waypointPassingInstruction = (String) dbObject.get(FieldNames.WAYPOINT_PASSINGSIDE.name());
                if (waypointPassingInstruction != null) {
                    logger.info("Migrating PassingInstruction " + waypointPassingInstruction
                            + " to field name WAYPOINT_PASSINGINSTRUCTIONS");
                    if ((i == 0 || i == Util.size(dbCourseList) - 1)
                            && waypointPassingInstruction.toLowerCase().equals("gate")) {
                        logger.warning("Changing PassingInstructions of first or last Waypoint from Gate to Line.");
                        waypointPassingInstruction = "Line";
                    }
                    dbObject.put(FieldNames.WAYPOINT_PASSINGINSTRUCTIONS.name(), waypointPassingInstruction);
                    dbObject.remove(FieldNames.WAYPOINT_PASSINGSIDE.name());
                    migrated = true;
                }
            }
            if (waypointPassingInstruction != null) {
                passingInstructions = PassingInstruction.valueOfIgnoringCase(waypointPassingInstruction);
            }
            Pair<ControlPoint, Boolean> controlPoint = loadControlPoint((Document) dbObject.get(FieldNames.CONTROLPOINT.name()));
            migrated = migrated || controlPoint.getB();
            if (passingInstructions == null) {
                waypoint = new WaypointImpl(controlPoint.getA());
            } else {
                waypoint = new WaypointImpl(controlPoint.getA(), passingInstructions);
            }
            courseData.addWaypoint(i++, waypoint);
        }
        return new Pair<>(courseData, migrated);
    }

    /**
     * @return second component tells whether migration was performed on {@code dbObject} in place
     */
    private Pair<ControlPoint, Boolean> loadControlPoint(Document dbObject) {
        String controlPointClass = (String) dbObject.get(FieldNames.CONTROLPOINT_CLASS.name());
        ControlPoint controlPoint = null;
        boolean migrated = false;
        if (controlPointClass != null) {
            if (controlPointClass.equals(Mark.class.getSimpleName())) {
                Mark mark = loadMark((Document) dbObject.get(FieldNames.CONTROLPOINT_VALUE.name()));
                controlPoint = mark;
            } else if (controlPointClass.equals("Gate")) {
                ControlPointWithTwoMarks cpwtm = loadControlPointWithTwoMarks(
                        (Document) dbObject.get(FieldNames.CONTROLPOINT_VALUE.name()));
                dbObject.put(FieldNames.CONTROLPOINT_CLASS.name(), ControlPointWithTwoMarks.class.getSimpleName());
                controlPoint = cpwtm;
                migrated = true;
            } else if (controlPointClass.equals(ControlPointWithTwoMarks.class.getSimpleName())) {
                ControlPointWithTwoMarks cpwtm = loadControlPointWithTwoMarks(
                        (Document) dbObject.get(FieldNames.CONTROLPOINT_VALUE.name()));
                controlPoint = cpwtm;
            }
        }
        return new Pair<>(controlPoint, migrated);
    }

    /**
     * Checks for the old GATE fields and migrates them to the new CONTROLPOINTWITHTWOMARKS fields.
     */
    @SuppressWarnings("deprecation") // Used for migrating old races
    private ControlPointWithTwoMarks loadControlPointWithTwoMarks(Document dbObject) {
        String controlPointName = (String) dbObject.get(FieldNames.CONTROLPOINTWITHTWOMARKS_NAME.name());
        if (controlPointName == null) {
            controlPointName = (String) dbObject.get(FieldNames.GATE_NAME.name());
            logger.info("Migrating name of ControlPointWithTwoMarks " + controlPointName
                    + " from GATE_NAME to new field CONTROLPOINTWITHTWOMARKS_NAME.");
            dbObject.put(FieldNames.CONTROLPOINTWITHTWOMARKS_NAME.name(), controlPointName);
            dbObject.remove(FieldNames.GATE_NAME.name());
        }
        Serializable controlPointId = (Serializable) dbObject.get(FieldNames.CONTROLPOINTWITHTWOMARKS_ID.name());
        if (controlPointId == null) {
            controlPointId = (Serializable) dbObject.get(FieldNames.GATE_ID.name());
            logger.info("Migrating id of ControlPointWithTwoMarks " + controlPointName
                    + " from old field GATE_ID to CONTROLPOINTWITHTWOMARKS_ID.");
            dbObject.put(FieldNames.CONTROLPOINTWITHTWOMARKS_ID.name(), controlPointId);
            dbObject.remove(FieldNames.GATE_ID.name());
        }
        Document dbLeft = (Document) dbObject.get(FieldNames.CONTROLPOINTWITHTWOMARKS_LEFT.name());
        if (dbLeft == null) {
            dbLeft = (Document) dbObject.get(FieldNames.GATE_LEFT.name());
            logger.info("Migrating left Mark of ControlPointWithTwoMarks " + controlPointName
                    + " from old field GATE_LEFT to CONTROLPOINTWITHTWOMARKS_LEFT");
            dbObject.put(FieldNames.CONTROLPOINTWITHTWOMARKS_LEFT.name(), dbLeft);
            dbObject.remove(FieldNames.GATE_LEFT.name());
        }
        Mark leftMark = loadMark(dbLeft);
        Document dbRight = (Document) dbObject.get(FieldNames.CONTROLPOINTWITHTWOMARKS_RIGHT.name());
        if (dbRight == null) {
            dbRight = (Document) dbObject.get(FieldNames.GATE_RIGHT.name());
            logger.info("Migrating right Mark of ControlPointWithTwoMarks " + controlPointName
                    + " from old field GATE_RIGHT to CONTROLPOINTWITHTWOMARKS_RIGHT");
            dbObject.put(FieldNames.CONTROLPOINTWITHTWOMARKS_RIGHT.name(), dbRight);
            dbObject.remove(FieldNames.GATE_RIGHT.name());
        }
        Mark rightMark = loadMark(dbRight);
        String shortName = (String) dbObject.get(FieldNames.CONTROLPOINTWITHTWOMARKS_SHORT_NAME.name());

        if (shortName == null || shortName.isEmpty()) {
            shortName = controlPointName;
        }
        ControlPointWithTwoMarks gate = baseDomainFactory.createControlPointWithTwoMarks(controlPointId, leftMark,
                rightMark, controlPointName, shortName);
        return gate;
    }

    private Mark loadMark(Document dbObject) {
        Serializable markId = (Serializable) dbObject.get(FieldNames.MARK_ID.name());
        String markColorAsString = (String) dbObject.get(FieldNames.MARK_COLOR.name());
        Color markColor = AbstractColor.getCssColor(markColorAsString);
        String markName = (String) dbObject.get(FieldNames.MARK_NAME.name());
        String markShortName = (String) dbObject.get(FieldNames.MARK_SHORT_NAME.name());
        String markPattern = (String) dbObject.get(FieldNames.MARK_PATTERN.name());
        String markShape = (String) dbObject.get(FieldNames.MARK_SHAPE.name());
        Object markTypeRaw = dbObject.get(FieldNames.MARK_TYPE.name());
        Object originatingMarkTemplateIdObject = dbObject.get(FieldNames.MARK_ORIGINATING_MARK_TEMPLATE_ID.name());
        UUID originatingMarkTemplateId = originatingMarkTemplateIdObject == null ? null
                : UUID.fromString(originatingMarkTemplateIdObject.toString());
        Object originatingMarkPropertiesIdObject = dbObject
                .get(FieldNames.MARK_ORIGINATING_MARK_PROPERTIES_ID.name());
        UUID originatingMarkPropertiesId = originatingMarkPropertiesIdObject == null ? null
                : UUID.fromString(originatingMarkPropertiesIdObject.toString());
        MarkType markType = markTypeRaw == null ? null : MarkType.valueOf((String) markTypeRaw);
        Mark mark = baseDomainFactory.getOrCreateMark(markId, markName, markShortName, markType, markColor, markShape,
                markPattern, originatingMarkTemplateId, originatingMarkPropertiesId);
        return mark;
    }

    @Override
    public Collection<DynamicCompetitor> loadAllCompetitors() {
        Map<Serializable, DynamicCompetitor> competitorsById = new HashMap<>();
        MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS.name());
            final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
            for (Document o : collection.find()) {
                try {
                    JSONObject json = Helpers.toJSONObjectSafe(new JSONParser().parse(o.toJson(writerSettings)));
                    DynamicCompetitor c = competitorWithBoatRefDeserializer.deserialize(json);
                    // ensure that in case there should be multiple competitors with equal IDs in the DB
                    // only one will survive
                    if (competitorsById.containsKey(c.getId())) {
                        collection.deleteOne(o);
                    } else {
                        competitorsById.put(c.getId(), c);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load competitors: "+
                            o.toString());
                    logger.log(Level.SEVERE, "loadCompetitors", e);
                }
            }
        return competitorsById.values();
    }

    @Override
    public Iterable<CompetitorWithBoat> migrateLegacyCompetitorsIfRequired() {
        Map<Serializable, CompetitorWithBoat> competitorsById = null;
        boolean competitorsCollectionExist = false;
        boolean boatsCollectionCollectionExist = false;
        for (final String collectionName : database.listCollectionNames()) {
            if (collectionName.equals(CollectionNames.COMPETITORS.name())) {
                competitorsCollectionExist = true;
            }
            if (collectionName.equals(CollectionNames.BOATS.name())) {
                boatsCollectionCollectionExist = true;
            }
        }
        MongoCollection<Document> orginalCompetitorCollection = database.getCollection(CollectionNames.COMPETITORS.name());
        // there is a corner case where tests can create just one competitor without boat
        // before we migrate we need to check if this case
        if (competitorsCollectionExist && !boatsCollectionCollectionExist) {
            long competitorCount = orginalCompetitorCollection.countDocuments();
            if (competitorCount > 0) {
                Document oneCompetitorDbObject = orginalCompetitorCollection.find().first();
                Object boatObject = oneCompetitorDbObject.get(CompetitorJsonConstants.FIELD_BOAT);
                // only in case such a boat object exist we need a migration, because the new type stores only a boatID or no boat at all
                if (boatObject != null) {
                    logger.log(Level.INFO, "Bug2822 DB-Migration: Rename COMPETITORS collection to COMPETITORS_BAK.");
                    competitorsById = new HashMap<>();
                    orginalCompetitorCollection.renameCollection(new MongoNamespace(database.getName(), CollectionNames.COMPETITORS_BAK.name()), new RenameCollectionOptions().dropTarget(true));
                    MongoCollection<Document> collection = database.getCollection(CollectionNames.COMPETITORS_BAK.name());
                    try {
                        logger.log(Level.INFO, "Bug2822 DB-Migration: Load old competitors with embedded boats from COMPETITORS_BAK.");
                        final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
                        for (Document o : collection.find()) {
                            JSONObject json = Helpers.toJSONObjectSafe(new JSONParser().parse(o.toJson(writerSettings)));
                            CompetitorWithBoat c = legacyCompetitorWithBoatDeserializer.deserialize(json);
                            // accept only the first instance for any given ID
                            if (!competitorsById.containsKey(c.getId())) {
                                competitorsById.put(c.getId(), c);
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Bug2822 DB-Migration: Error connecting to MongoDB, unable to load competitors.");
                        logger.log(Level.SEVERE, "Bug2822 DB-Migration: renameCompetitorsCollectionAndloadAllLegacyCompetitors", e);
                    }
                }
            }
        }
        return competitorsById==null?null:competitorsById.values();
    }

    @Override
    public Collection<DynamicBoat> loadAllBoats() {
        ArrayList<DynamicBoat> result = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection(CollectionNames.BOATS.name());
        try {
            final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
            for (Document o : collection.find()) {
                JSONObject json = Helpers.toJSONObjectSafe(new JSONParser().parse(o.toJson(writerSettings)));
                DynamicBoat b = boatDeserializer.deserialize(json);
                result.add(b);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load boats.");
            logger.log(Level.SEVERE, "loadBoats", e);
        }
        return result;
    }

    @Override
    public Iterable<DeviceConfiguration> loadAllDeviceConfigurations() {
        List<DeviceConfiguration> result = new ArrayList<>();
        MongoCollection<Document> configurationCollection = database.getCollection(CollectionNames.CONFIGURATIONS.name());
        try {
            for (Document dbObject : configurationCollection.find()) {
                DeviceConfiguration entry = loadConfigurationEntry(dbObject);
                result.add(entry);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load configurations.");
            logger.log(Level.SEVERE, "loadAllDeviceConfigurations", e);
        }

        return result;
    }


    private DeviceConfiguration loadConfigurationEntry(Document dbObject) throws JsonDeserializationException, ParseException {
        final String idAsString = dbObject.getString(FieldNames.CONFIGURATION_ID_AS_STRING.name());
        final DeviceConfiguration configuration;
        if (idAsString == null) {
            @SuppressWarnings("deprecation") // migration
            final String oldCombinedName = dbObject.getString(FieldNames.CONFIGURATION_MATCHER_ID.name());
            final String name = oldCombinedName.substring("DeviceConfigurationMatcherSingle".length());
            final UUID id = UUID.randomUUID();
            logger.info("Migrating configuration with old matcher ID "+oldCombinedName+" to config with ID "+id+" with name "+name);
            Document configObject = (Document) dbObject.get(FieldNames.CONFIGURATION_CONFIG.name());
            // inject name and ID into old config document format to satisfy new de-serializer's format:
            configObject.put(DeviceConfigurationJsonSerializer.FIELD_ID_AS_STRING, id.toString());
            configObject.put(DeviceConfigurationJsonSerializer.FIELD_NAME, name);
            configuration = loadConfiguration(configObject);
            @SuppressWarnings("deprecation") // migration
            final DeleteResult deleteResult = database.getCollection(CollectionNames.CONFIGURATIONS.name()).deleteOne(
                    new Document(FieldNames.CONFIGURATION_MATCHER_ID.name(), oldCombinedName));
            if (deleteResult.getDeletedCount() != 1) {
                logger.warning("Expected to delete device configuration "+oldCombinedName+", but couldn't delete any");
            }
            new MongoObjectFactoryImpl(database).storeDeviceConfiguration(configuration);
        } else {
            configuration = loadConfiguration(dbObject);
        }
        return configuration;
    }

    private DeviceConfiguration loadConfiguration(Document configObject) throws JsonDeserializationException, ParseException {
        DeviceConfiguration configuration = null;
        final JsonWriterSettings writerSettings = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
        JsonDeserializer<DeviceConfiguration> deserializer = DeviceConfigurationJsonDeserializer.create();
        JSONObject json = Helpers.toJSONObjectSafe(new JSONParser().parse(configObject.toJson(writerSettings)));
        configuration = deserializer.deserialize(json);
        return configuration;
    }

    @Override
    public Map<String, Set<URL>> loadResultUrls() {
        Map<String, Set<URL>> resultUrls = new HashMap<>();
        MongoCollection<Document> resultUrlCollection = database.getCollection(CollectionNames.RESULT_URLS.name());
        for (Document dbObject : resultUrlCollection.find()) {
            String providerName = (String) dbObject.get(FieldNames.RESULT_PROVIDERNAME.name());
            String urlString = (String) dbObject.get(FieldNames.RESULT_URL.name());
            URL url;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "Failed to parse result Url String: " + urlString + ". Did not load url!");
                continue;
            }
            if (!resultUrls.containsKey(providerName)) {
                resultUrls.put(providerName, new HashSet<URL>());
            }
            Set<URL> set = resultUrls.get(providerName);
            set.add(url);
        }
        return resultUrls;
    }

    private ImageDescriptor loadImage(Document dbObject) {
        ImageDescriptor image = null;
        URL imageURL = loadURL(dbObject, FieldNames.IMAGE_URL);
        if (imageURL != null) {
            String title = (String) dbObject.get(FieldNames.IMAGE_TITLE.name());
            String subtitle = (String) dbObject.get(FieldNames.IMAGE_SUBTITLE.name());
            String copyright = (String) dbObject.get(FieldNames.IMAGE_COPYRIGHT.name());
            String localeRaw = (String) dbObject.get(FieldNames.IMAGE_LOCALE.name());
            Locale locale = localeRaw != null ? Locale.forLanguageTag(localeRaw) : null;
            Number imageWidth = (Number) dbObject.get(FieldNames.IMAGE_WIDTH_IN_PX.name());
            Number imageHeight = (Number) dbObject.get(FieldNames.IMAGE_HEIGHT_IN_PX.name());
            TimePoint createdAtDate = loadTimePoint(dbObject, FieldNames.IMAGE_CREATEDATDATE);
            Iterable<?> tags = (Iterable<?>) dbObject.get(FieldNames.IMAGE_TAGS.name());
            List<String> imageTags = new ArrayList<String>();
            if (tags != null) {
                for (Object tagObject : tags) {
                    imageTags.add((String) tagObject);
                }
            }
            image = new ImageDescriptorImpl(imageURL, createdAtDate);
            image.setCopyright(copyright);
            image.setTitle(title);
            image.setSubtitle(subtitle);
            image.setLocale(locale);
            image.setTags(imageTags);
            if (imageWidth != null && imageHeight != null) {
                image.setSize(imageWidth.intValue(), imageHeight.intValue());
            }
        }
        return image;
    }

    private VideoDescriptor loadVideo(Document dbObject) {
        VideoDescriptor video = null;
        URL videoURL = loadURL(dbObject, FieldNames.VIDEO_URL);
        if (videoURL != null) {
            String title = (String) dbObject.get(FieldNames.VIDEO_TITLE.name());
            String subtitle = (String) dbObject.get(FieldNames.VIDEO_SUBTITLE.name());
            String copyright = (String) dbObject.get(FieldNames.VIDEO_COPYRIGHT.name());
            Object mimeTypeRaw = dbObject.get(FieldNames.VIDEO_MIMETYPE.name());
            MimeType mimeType = mimeTypeRaw == null ? null : MimeType.valueOf((String) mimeTypeRaw);
            String localeRaw = (String) dbObject.get(FieldNames.VIDEO_LOCALE.name());
            Locale locale = localeRaw != null ? Locale.forLanguageTag(localeRaw) : null;
            TimePoint createdAtDate = loadTimePoint(dbObject, FieldNames.VIDEO_CREATEDATDATE);
            Iterable<?> tags = (Iterable<?>) dbObject.get(FieldNames.VIDEO_TAGS.name());
            Number lengthInSeconds = (Number) dbObject.get(FieldNames.VIDEO_LENGTH_IN_SECONDS.name());
            URL thumbnailURL = loadURL(dbObject, FieldNames.VIDEO_THUMBNAIL_URL);
            List<String> videoTags = new ArrayList<String>();
            if (tags != null) {
                for (Object tagObject : tags) {
                    videoTags.add((String) tagObject);
                }
            }
            video = new VideoDescriptorImpl(videoURL, mimeType, createdAtDate);
            video.setCopyright(copyright);
            video.setTitle(title);
            video.setSubtitle(subtitle);
            video.setLocale(locale);
            video.setTags(videoTags);
            video.setLengthInSeconds(lengthInSeconds == null ? null : lengthInSeconds.intValue());
            video.setThumbnailURL(thumbnailURL);
        }
        return video;
    }

    private URL loadURL(Document dbObject, FieldNames field) {
        URL result = null;
        String urlAsString = (String) dbObject.get(field.name());
        if (urlAsString != null) {
            try {
                result = new URL(urlAsString);
            } catch (MalformedURLException e) {
                logger.severe("Error parsing URL '" + urlAsString + "' in field " + field.name() + ".");
            }
        }
        return result;
    }

    /**
     * Legacy code to support conversion of old image and video URLs
     *
     * @param event
     * @param eventDBObject
     */
    private boolean loadLegacyImageAndVideoURLs(Event event, Document eventDBObject) {
        URL logoImageURL = null;
        List<URL> imageURLs = new ArrayList<URL>();
        List<URL> sponsorImageURLs = new ArrayList<URL>();
        List<URL> videoURLs = new ArrayList<URL>();

        String logoImageURLAsString = (String) eventDBObject.get(FieldNames.EVENT_LOGO_IMAGE_URL.name());
        if (logoImageURLAsString != null) {
            try {
                logoImageURL = new URL(logoImageURLAsString);
            } catch (MalformedURLException e) {
                logger.severe("Error parsing logo image URL " + logoImageURLAsString + " for event " + event.getName()
                        + ". Ignoring this URL.");
            }
        }
        Iterable<?> imageURLsJson = (Iterable<?>) eventDBObject.get(FieldNames.EVENT_IMAGE_URLS.name());
        if (imageURLsJson != null) {
            for (Object imageURL : imageURLsJson) {
                try {
                    imageURLs.add(new URL((String) imageURL));
                } catch (MalformedURLException e) {
                    logger.severe("Error parsing image URL " + imageURL + " for event " + event.getName()
                            + ". Ignoring this image URL.");
                }
            }
        }
        Iterable<?> videoURLsJson = (Iterable<?>) eventDBObject.get(FieldNames.EVENT_VIDEO_URLS.name());
        if (videoURLsJson != null) {
            for (Object videoURL : videoURLsJson) {
                try {
                    videoURLs.add(new URL((String) videoURL));
                } catch (MalformedURLException e) {
                    logger.severe("Error parsing video URL " + videoURL + " for event " + event.getName()
                            + ". Ignoring this video URL.");
                }
            }
        }
        Iterable<?> sponsorImageURLsJson = (Iterable<?>) eventDBObject.get(FieldNames.EVENT_SPONSOR_IMAGE_URLS.name());
        if (sponsorImageURLsJson != null) {
            for (Object sponsorImageURL : sponsorImageURLsJson) {
                try {
                    sponsorImageURLs.add(new URL((String) sponsorImageURL));
                } catch (MalformedURLException e) {
                    logger.severe("Error parsing sponsor image URL " + sponsorImageURL + " for event " + event.getName()
                            + ". Ignoring this sponsor image URL.");
                }
            }
        }
        return event.setMediaURLs(imageURLs, sponsorImageURLs, videoURLs, logoImageURL, Collections.emptyMap());
    }

    private boolean loadLegacySailorsInfoWebsiteURL(Event event, Document eventDBObject) {
        final boolean modified;
        final String sailorsInfoWebSiteURLAsString = (String) eventDBObject
                .get(FieldNames.EVENT_SAILORS_INFO_WEBSITE_URL.name());
        if (sailorsInfoWebSiteURLAsString != null) {
            try {
                // The legacy sailors info URL (only used at Kieler/Travemuender Woche events) used to have 2 localized
                // versions:
                // The German version with no suffix (e.g. http://sailorsinfo.travemuender-woche.com)
                // The English/international version with "/en" suffix (e.g.
                // http://sailorsinfo.travemuender-woche.com/en)
                if (!event.hasSailorsInfoWebsiteURL(null)) {
                    final String englishURL = sailorsInfoWebSiteURLAsString
                            + (sailorsInfoWebSiteURLAsString.endsWith("/") ? "" : "/") + "en";
                    event.setSailorsInfoWebsiteURL(null, new URL(englishURL));
                }
                if (!event.hasSailorsInfoWebsiteURL(Locale.GERMAN)) {
                    event.setSailorsInfoWebsiteURL(Locale.GERMAN, new URL(sailorsInfoWebSiteURLAsString));
                }
            } catch (MalformedURLException e) {
                logger.severe("Error parsing sailors info website URL " + sailorsInfoWebSiteURLAsString + " for event "
                        + event.getName() + ". Ignoring this URL.");
            }
            modified = true;
        } else {
            modified = false;
        }
        return modified;
    }

    @Override
    public ConnectivityParametersLoadingResult loadConnectivityParametersForRacesToRestore(
            Consumer<RaceTrackingConnectivityParameters> callback) {
        final MongoCollection<Document> collection = database
                .getCollection(CollectionNames.CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED.name());
        final FindIterable<Document> cursor = collection.find();
        final long count = collection.countDocuments();
        logger.info("Restoring " + count + " races");
        final List<Document> restoreParameters = new ArrayList<>();
        // consume all elements quickly to avoid cursor/DB timeouts while restoring many races;
        // MongoDB cursors by default time out after ten minutes if no more batch (of by default 100 elements)
        // has been requested during this time.
        Util.addAll(cursor, restoreParameters);
        logger.info("Obtained " + restoreParameters.size() + " race parameters to restore");
        final ScheduledExecutorService backgroundExecutor = ThreadPoolUtil.INSTANCE
                .getDefaultBackgroundTaskThreadPoolExecutor();
        final Set<FutureTask<Void>> waiters = new HashSet<>();
        logger.info("Starting to restore races");
        final AtomicInteger i = new AtomicInteger();
        for (final Document o : restoreParameters) {
            final FutureTask<Void> waiter = new FutureTask<>(() -> {
                final String type = (String) o.get(TypeBasedServiceFinder.TYPE);
                final int finalI = i.incrementAndGet();
                logger.info("Applying to restore race #" + finalI + "/" + count + " of type " + type);
                raceTrackingConnectivityParamsServiceFinder.applyServiceWhenAvailable(type,
                        connectivityParamsPersistenceService -> {
                            logger.info("Restoring race #" + finalI + "/" + count + " of type " + type);
                            final Map<String, Object> map = new HashMap<>();
                            for (final String key : o.keySet()) {
                                if (!key.equals(TypeBasedServiceFinder.TYPE)) {
                                    map.put(key, o.get(key));
                                }
                            }
                            try {
                                final RaceTrackingConnectivityParameters params = connectivityParamsPersistenceService.mapTo(map);
                                if (params != null) {
                                    callback.accept(params);
                                    logger.info("Done restoring race #" + finalI + "/" + count + " of type " + type);
                                } else {
                                    logger.warning("Couldn't restore race #" + finalI + "/" + count + " of type " + type
                                            + " with parameters " + o
                                            + " because the parameters loaded from the DB couldn't be mapped. Maybe the owning leaderboard was removed?"
                                            + " Removing this parameter set from the list of races to restore. The server will make no further attempt to restore this race.");
                                    collection.deleteOne(o);
                                }
                            } catch (Exception e) {
                                logger.log(Level.SEVERE,
                                        "Exception trying to load race #" + finalI + "/" + count + " of type " + type
                                                + " from restore connectivity parameters " + o + " with handler "
                                                + connectivityParamsPersistenceService
                                                + ". Removing this parameter set from the list of races to restore."
                                                + " The server will make no further attempt to restore this race: "+o,
                                        e);
                                collection.deleteOne(o);
                            }
                        });
            }, /* void result */ null);
            waiters.add(waiter);
            backgroundExecutor.execute(waiter); // OK to not associate with the current Subject here because we may not even have a session
        }
        logger.info("Done restoring races; restored " + i + " of " + count + " races");
        return new ConnectivityParametersLoadingResult() {
            @Override
            public long getNumberOfParametersToLoad() {
                return count;
            }

            @Override
            public void waitForCompletionOfCallbacksForAllParameters() throws InterruptedException, ExecutionException {
                for (final FutureTask<Void> waiter : waiters) {
                    waiter.get();
                }
            }
        };
    }

    @Override
    public Map<Integer, Pair<DetailedRaceInfo, AnniversaryType>> getAnniversaryData() throws MalformedURLException {
        HashMap<Integer, Pair<DetailedRaceInfo, AnniversaryType>> fromDb = new HashMap<>();
        MongoCollection<Document> anniversarysStored = database.getCollection(CollectionNames.ANNIVERSARIES.name());
        MongoCursor<Document> cursor = anniversarysStored.find().iterator();
        while (cursor.hasNext()) {
            Document toLoad = cursor.next();
            String leaderboardName = toLoad.get(FieldNames.LEADERBOARD_NAME.name()).toString();
            String eventID = toLoad.get(FieldNames.EVENT_ID.name()).toString();

            final Object mongoDisplayName = toLoad.get(FieldNames.LEADERBOARD_DISPLAY_NAME.name());
            final String leaderboardDisplayName;
            if (mongoDisplayName == null) {
                leaderboardDisplayName = null;
            } else {
                leaderboardDisplayName = mongoDisplayName.toString();
            }
            final Object mongoEventName = toLoad.get(FieldNames.EVENT_NAME.name());
            final String eventName;
            if (mongoEventName == null) {
                eventName = null;
            } else {
                eventName = mongoEventName.toString();
            }

            TimePoint startOfRace = new MillisecondsTimePoint(
                    ((Number) toLoad.get(FieldNames.START_OF_RACE.name())).longValue());
            String race = toLoad.get(FieldNames.RACE_NAME.name()).toString();
            String regatta = toLoad.get(FieldNames.REGATTA_NAME.name()).toString();
            Object rurl = toLoad.get(FieldNames.REMOTE_URL.name());
            final URL remoteUrlOrNull;
            if (rurl != null) {
                remoteUrlOrNull = new URL(rurl.toString());
            } else {
                remoteUrlOrNull = null;
            }
            final Object typeJson = toLoad.get(FieldNames.EVENT_TYPE.name());
            final EventType eventType;
            if (typeJson == null) {
                eventType = null;
            } else {
                eventType = EventType.valueOf(typeJson.toString());
            }
            DetailedRaceInfo loadedAnniversary = new DetailedRaceInfo(new RegattaNameAndRaceName(regatta, race),
                    leaderboardName, leaderboardDisplayName, startOfRace, UUID.fromString(eventID), eventName, eventType,
                    remoteUrlOrNull);
            int anniversary = ((Number) toLoad.get(FieldNames.ANNIVERSARY_NUMBER.name())).intValue();
            String type = toLoad.get(FieldNames.ANNIVERSARY_TYPE.name()).toString();
            fromDb.put(anniversary, new Pair<>(loadedAnniversary, AnniversaryType.valueOf(type)));
        }
        return fromDb;
    }

    @Override
    public TypeBasedServiceFinder<RaceTrackingConnectivityParametersHandler> getRaceTrackingConnectivityParamsServiceFinder() {
        return raceTrackingConnectivityParamsServiceFinder;
    }
    
    @Override
    public Map<RaceIdentifier, MarkPassingRaceFingerprint> loadFingerprintsForMarkPassingHashes() {
        final MongoCollection<Document> markPassingsCollection = database.getCollection(CollectionNames.MARKPASSINGS.name());
        markPassingsCollection.createIndex(new Document()
                .append(FieldNames.EVENT_NAME.name(), 1)
                .append(FieldNames.RACE_NAME.name(), 1),
            new IndexOptions()
                .unique(true)
                .name("markpassingsbyeventandrace")
                .background(false));
        final Map<RaceIdentifier, MarkPassingRaceFingerprint> fingerprintHashMap = new HashMap<>();
        for (final Document currentDocument : markPassingsCollection.find()) {
            final Pair<RaceIdentifier, MarkPassingRaceFingerprint> fingerprint = loadMarkPassingsFingerprint(currentDocument);
            if (fingerprint != null && fingerprint.getB() != null) {
                fingerprintHashMap.put(fingerprint.getA(), fingerprint.getB());
            }
        }
        return fingerprintHashMap;
    }

    private Pair<RaceIdentifier, MarkPassingRaceFingerprint> loadMarkPassingsFingerprint(final Document currentDocument) {
        final RaceIdentifier raceIdentifier = loadRaceIdentifier(currentDocument);
        MarkPassingRaceFingerprint fingerprint;
        try {
            final JSONObject json = Helpers.toJSONObjectSafe(
                    new JSONParser().parse(((Document) currentDocument.get(FieldNames.MARK_PASSINGS_FINGERPRINT.name())).toJson()));
            fingerprint = new MarkPassingRaceFingerprintImpl(json);
        } catch (JsonDeserializationException | ParseException e) {
            logger.log(Level.WARNING, "Problem de-serializing mark passings from document; ignoring", e);
            fingerprint = null;
        }
        return new Pair<>(raceIdentifier, fingerprint);
    }

    @Override
    public Map<Competitor, Map<Waypoint, MarkPassing>> loadMarkPassings(RaceIdentifier raceIdentifier, Course course) {
        final Map<Competitor, Map<Waypoint, MarkPassing>> result;
        final Document query = new Document();
        addRaceIdentifierToQuery(query, raceIdentifier);
        final MongoCollection<Document> markPassingsCollection = database.getCollection(CollectionNames.MARKPASSINGS.name());
        final Document doc = markPassingsCollection.find(query).first();
        if (doc != null) {
            result = new HashMap<>();
            final List<Document> markPassingsDoc = doc.getList(FieldNames.MARK_PASSINGS.name(), Document.class);
            for (final Document markPassingsForOneCompetitorDoc : markPassingsDoc) {
                final Serializable competitorId = markPassingsForOneCompetitorDoc.get(FieldNames.COMPETITOR_ID.name(), Serializable.class);
                final Competitor competitor = baseDomainFactory.getExistingCompetitorById(competitorId);
                for (final Document markPassingForWaypoint : markPassingsForOneCompetitorDoc.getList(FieldNames.MARK_PASSINGS.name(), Document.class)) {
                    final Pair<Waypoint, MarkPassing> waypointAndMarkPassing = loadWaypointAndMarkPassing(competitor, markPassingForWaypoint, course);
                    result.computeIfAbsent(competitor, c->new HashMap<>()).put(waypointAndMarkPassing.getA(), waypointAndMarkPassing.getB());
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    private Pair<Waypoint, MarkPassing> loadWaypointAndMarkPassing(Competitor competitor, Document markPassingForWaypoint, Course course) {
        final int waypointIndex = markPassingForWaypoint.getInteger(FieldNames.INDEX_OF_PASSED_WAYPOINT.name());
        final Waypoint waypoint = Util.get(course.getWaypoints(), waypointIndex);
        final TimePoint timePoint = TimePoint.of(markPassingForWaypoint.getLong(FieldNames.TIME_AS_MILLIS.name()));
        final MarkPassing markPassing = new MarkPassingImpl(timePoint, waypoint, competitor);
        return new Pair<>(waypoint, markPassing);
    }
}
