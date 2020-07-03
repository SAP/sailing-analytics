package com.sap.sailing.gwt.ui.server;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.competitorimport.CompetitorProvider;
import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.AllEventsOfTypeFinder;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCCertificateAssignmentFinder;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCImpliedWindSourceFinder;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataAnalyzer;
import com.sap.sailing.domain.abstractlog.orc.RegattaLogORCCertificateAssignmentFinder;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCImpliedWindSourceEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbortingFlagFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastPublishedCourseDesignFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.MarkPassingDataFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.TrackingTimesFinder;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.FlagPoleState;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.gate.ReadonlyGateStartRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.line.ConfigurableStartModeFlagRacingProcedure;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RaceLogTrackingStateAnalyzer;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDefineMarkEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.BaseRegattaLogDeviceMappingFinder;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMappingFinder;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMarkMappingFinder;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.PairingListLeaderboardAdapter;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.SailingServerConfiguration;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.RacingProcedureConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.ESSConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.GateStartConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.LeagueConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.RRS26ConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.RacingProcedureConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.RacingProcedureWithConfigurableStartModeFlagConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.RegattaConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.SWCStartConfigurationImpl;
import com.sap.sailing.domain.base.configuration.procedures.ConfigurableStartModeFlagRacingProcedureConfiguration;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.SailingServerConfigurationImpl;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.MailInvitationType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.NotFoundException;
import com.sap.sailing.domain.common.PathType;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceFetcher;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaFetcher;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionForCompetitorInRace;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorAndBoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.FullLeaderboardDTO;
import com.sap.sailing.domain.common.dto.IncrementalLeaderboardDTO;
import com.sap.sailing.domain.common.dto.IncrementalOrFullLeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.PairingListDTO;
import com.sap.sailing.domain.common.dto.PairingListTemplateDTO;
import com.sap.sailing.domain.common.dto.PersonDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTOFactory;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.RaceLogTrackingInfoDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.dto.TrackedRaceDTO;
import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCCertificateUploadConstants;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.common.racelog.FlagPole;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.racelog.tracking.DoesNotHaveRegattaLogException;
import com.sap.sailing.domain.common.racelog.tracking.MappableToDevice;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.sharding.ShardingType;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.common.tracking.impl.PreciseCompactGPSFixMovingImpl.PreciseCompactPosition;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sailing.domain.coursetemplate.CommonMarkProperties;
import com.sap.sailing.domain.coursetemplate.ControlPointTemplate;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.FixedPositioning;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkRolePair.MarkRolePairFactory;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.PositioningVisitor;
import com.sap.sailing.domain.coursetemplate.RepeatablePart;
import com.sap.sailing.domain.coursetemplate.TrackingDeviceBasedPositioning;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.domain.coursetemplate.impl.CommonMarkPropertiesImpl;
import com.sap.sailing.domain.coursetemplate.impl.RepeatablePartImpl;
import com.sap.sailing.domain.coursetemplate.impl.WaypointTemplateImpl;
import com.sap.sailing.domain.igtimiadapter.Account;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.MetaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.leaderboard.caching.LiveLeaderboardUpdater;
import com.sap.sailing.domain.leaderboard.meta.MetaLeaderboardColumn;
import com.sap.sailing.domain.orc.ORCPerformanceCurveRankingMetric;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase.CertificateHandle;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.MongoRegattaLogStoreFactory;
import com.sap.sailing.domain.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.racelog.RaceStateOfSameDayHelper;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifierStringSerializationHandler;
import com.sap.sailing.domain.racelogtracking.DeviceMapping;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapterFactory;
import com.sap.sailing.domain.racelogtracking.impl.DeviceMappingImpl;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.regattalike.HasRegattaLike;
import com.sap.sailing.domain.regattalike.LeaderboardThatHasRegattaLike;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.resultimport.ResultUrlProvider;
import com.sap.sailing.domain.sharding.ShardingContext;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingAdapter;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingAdapterFactory;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingArchiveConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.persistence.SwissTimingAdapterPersistence;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayRace;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayService;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayServiceFactory;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.LineDetails;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.Track;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tractracadapter.RaceRecord;
import com.sap.sailing.domain.tractracadapter.TracTracAdapter;
import com.sap.sailing.domain.tractracadapter.TracTracAdapterFactory;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.windfinder.Spot;
import com.sap.sailing.domain.windfinder.WindFinderTrackerFactory;
import com.sap.sailing.expeditionconnector.ExpeditionDeviceConfiguration;
import com.sap.sailing.expeditionconnector.ExpeditionTrackerFactory;
import com.sap.sailing.gwt.common.client.EventWindFinderUtil;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.gwt.ui.client.SailingService;
import com.sap.sailing.gwt.ui.client.shared.charts.MarkPositionService.MarkTrackDTO;
import com.sap.sailing.gwt.ui.shared.AccountWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.CompactBoatPositionsDTO;
import com.sap.sailing.gwt.ui.shared.CompactRaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorProviderDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorsRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO.RegattaConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO.RegattaConfigurationDTO.RacingProcedureConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO.RegattaConfigurationDTO.RacingProcedureWithConfigurableStartModeFlagConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.DeviceIdentifierDTO;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;
import com.sap.sailing.gwt.ui.shared.EventBaseDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.GateDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupBaseDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.LegInfoDTO;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.ManeuverLossDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.MarkPassingTimesDTO;
import com.sap.sailing.gwt.ui.shared.PathDTO;
import com.sap.sailing.gwt.ui.shared.QRCodeEvent;
import com.sap.sailing.gwt.ui.shared.QuickRankDTO;
import com.sap.sailing.gwt.ui.shared.QuickRanksDTO;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupSeriesDTO;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO.GateStartInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO.LineStartInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO.RaceInfoExtensionDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogEventDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetFinishingAndFinishTimeDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.RaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceWithCompetitorsAndBoatsDTO;
import com.sap.sailing.gwt.ui.shared.RaceboardDataDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.RegattaLogDTO;
import com.sap.sailing.gwt.ui.shared.RegattaLogEventDTO;
import com.sap.sailing.gwt.ui.shared.RegattaOverviewEntryDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO.ScoreCorrectionEntryDTO;
import com.sap.sailing.gwt.ui.shared.RemoteSailingServerReferenceDTO;
import com.sap.sailing.gwt.ui.shared.SailingServiceConstants;
import com.sap.sailing.gwt.ui.shared.ScoreCorrectionProviderDTO;
import com.sap.sailing.gwt.ui.shared.SerializationDummy;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sailing.gwt.ui.shared.ServerConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SidelineDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.shared.SliceRacePreperationDTO;
import com.sap.sailing.gwt.ui.shared.SpeedWithBearingDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTOWithSecurity;
import com.sap.sailing.gwt.ui.shared.SwissTimingArchiveConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingEventRecordDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingReplayRaceDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.TrackFileImportDeviceIdentifierDTO;
import com.sap.sailing.gwt.ui.shared.TrackingConnectorInfoDTO;
import com.sap.sailing.gwt.ui.shared.UrlDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CommonMarkPropertiesDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPropertiesDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.RepeatablePartDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.WaypointTemplateDTO;
import com.sap.sailing.manage2sail.EventResultDescriptor;
import com.sap.sailing.manage2sail.Manage2SailEventResultsParserImpl;
import com.sap.sailing.manage2sail.RaceResultDescriptor;
import com.sap.sailing.manage2sail.RegattaResultDescriptor;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.ORCCertificateJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.LeaderboardGroupConstants;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.SimulationService;
import com.sap.sailing.server.operationaltransformation.AddRemoteSailingServerReference;
import com.sap.sailing.server.operationaltransformation.RemoveRemoteSailingServerReference;
import com.sap.sailing.server.operationaltransformation.UpdateServerConfiguration;
import com.sap.sailing.server.security.SailingViewerRole;
import com.sap.sailing.shared.server.SharedSailingData;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationResults;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.impl.PolarDiagramGPS;
import com.sap.sailing.simulator.impl.SparseSimulationDataException;
import com.sap.sailing.util.RegattaUtil;
import com.sap.sailing.xrr.structureimport.SeriesParameters;
import com.sap.sailing.xrr.structureimport.StructureImporter;
import com.sap.sailing.xrr.structureimport.buildstructure.SetRacenumberFromSeries;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Base64Utils;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.PairingListCreationException;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.SecondsDurationImpl;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.ImageResizingTaskDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.gwt.server.ResultCachingProxiedRemoteServiceServlet;
import com.sap.sse.gwt.shared.replication.ReplicaDTO;
import com.sap.sse.gwt.shared.replication.ReplicationMasterDTO;
import com.sap.sse.gwt.shared.replication.ReplicationStateDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;
import com.sap.sse.pairinglist.PairingList;
import com.sap.sse.pairinglist.PairingListTemplate;
import com.sap.sse.pairinglist.impl.PairingListTemplateImpl;
import com.sap.sse.qrcode.QRCodeGenerationUtil;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.ReplicaDescriptor;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.security.Action;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.ui.server.SecurityDTOFactory;
import com.sap.sse.security.ui.server.SecurityDTOUtil;
import com.sap.sse.security.util.RemoteServerUtil;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.MediaUtils;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.media.impl.ImageDescriptorImpl;
import com.sap.sse.shared.media.impl.VideoDescriptorImpl;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sap.sse.util.ThreadPoolUtil;
import com.sapsailing.xrr.structureimport.eventimport.RegattaJSON;


/**
 * The server side implementation of the RPC service.
 */
public class SailingServiceImpl extends ResultCachingProxiedRemoteServiceServlet implements SailingService, RaceFetcher, RegattaFetcher {
    protected static final Logger logger = Logger.getLogger(SailingServiceImpl.class.getName());

    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/StringMessages";

    private static final long serialVersionUID = 9031688830194537489L;

    private final FullyInitializedReplicableTracker<RacingEventService> racingEventServiceTracker;

    private final ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;

    private final ServiceTracker<ScoreCorrectionProvider, ScoreCorrectionProvider> scoreCorrectionProviderServiceTracker;

    private final ServiceTracker<CompetitorProvider, CompetitorProvider> competitorProviderServiceTracker;
    
    private final ServiceTracker<WindFinderTrackerFactory, WindFinderTrackerFactory> windFinderTrackerFactoryServiceTracker;

    private final MongoObjectFactory mongoObjectFactory;

    protected final ServiceTracker<ExpeditionTrackerFactory, ExpeditionTrackerFactory> expeditionConnectorTracker;

    protected final SwissTimingAdapterPersistence swissTimingAdapterPersistence;
    
    private final ServiceTracker<SwissTimingAdapterFactory, SwissTimingAdapterFactory> swissTimingAdapterTracker;

    private final ServiceTracker<TracTracAdapterFactory, TracTracAdapterFactory> tractracAdapterTracker;

    private final ServiceTracker<IgtimiConnectionFactory, IgtimiConnectionFactory> igtimiAdapterTracker;

    private final ServiceTracker<RaceLogTrackingAdapterFactory, RaceLogTrackingAdapterFactory> raceLogTrackingAdapterTracker;
    
    private final ServiceTracker<DeviceIdentifierStringSerializationHandler, DeviceIdentifierStringSerializationHandler>
    deviceIdentifierStringSerializationHandlerTracker;
    
    private final FullyInitializedReplicableTracker<SecurityService> securityServiceTracker;

    private final FullyInitializedReplicableTracker<SharedSailingData> sharedSailingDataTracker;
    
    protected final com.sap.sailing.domain.tractracadapter.persistence.MongoObjectFactory tractracMongoObjectFactory;

    private final DomainObjectFactory domainObjectFactory;

    protected final SwissTimingFactory swissTimingFactory;

    private final com.sap.sailing.domain.tractracadapter.persistence.DomainObjectFactory tractracDomainObjectFactory;

    protected final Executor executor;
    
    protected final com.sap.sailing.domain.base.DomainFactory baseDomainFactory;
    
    private static final int LEADERBOARD_BY_NAME_RESULTS_CACHE_BY_ID_SIZE = 100;
    
    private static final int LEADERBOARD_DIFFERENCE_CACHE_SIZE = 50;

    protected static final String MAILTYPE_PROPERTY = "com.sap.sailing.domain.tracking.MailInvitationType";

    protected ResourceBundleStringMessages serverStringMessages;

    private final LinkedHashMap<String, LeaderboardDTO> leaderboardByNameResultsCacheById;

    private int leaderboardDifferenceCacheByIdPairHits;
    private int leaderboardDifferenceCacheByIdPairMisses;
    /**
     * Caches some results of the hard to compute difference between two {@link LeaderboardDTO}s. The objects contained as values
     * have been obtained by {@link IncrementalLeaderboardDTO#strip(LeaderboardDTO)}. The cache size is limited to
     * {@link #LEADERBOARD_DIFFERENCE_CACHE_SIZE}.
     */
    private final LinkedHashMap<com.sap.sse.common.Util.Pair<String, String>, IncrementalLeaderboardDTO> leaderboardDifferenceCacheByIdPair;

    private final SwissTimingReplayService swissTimingReplayService;

    private final QuickRanksLiveCache quickRanksLiveCache;
    
    public SailingServiceImpl() {
        BundleContext context = Activator.getDefault();
        Activator activator = Activator.getInstance();
        quickRanksLiveCache = new QuickRanksLiveCache(this);
        replicationServiceTracker = ServiceTrackerFactory.createAndOpen(context, ReplicationService.class);
        racingEventServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, RacingEventService.class);
        sharedSailingDataTracker = FullyInitializedReplicableTracker.createAndOpen(context, SharedSailingData.class);
        windFinderTrackerFactoryServiceTracker = ServiceTrackerFactory.createAndOpen(context, WindFinderTrackerFactory.class);
        swissTimingAdapterTracker = ServiceTrackerFactory.createAndOpen(context, SwissTimingAdapterFactory.class);
        tractracAdapterTracker = ServiceTrackerFactory.createAndOpen(context, TracTracAdapterFactory.class);
        raceLogTrackingAdapterTracker = ServiceTrackerFactory.createAndOpen(context,
                RaceLogTrackingAdapterFactory.class);
        deviceIdentifierStringSerializationHandlerTracker = ServiceTrackerFactory.createAndOpen(context,
                DeviceIdentifierStringSerializationHandler.class);
        securityServiceTracker = FullyInitializedReplicableTracker.createAndOpen(context, SecurityService.class);
        securityServiceTracker.open();
        igtimiAdapterTracker = ServiceTrackerFactory.createAndOpen(context, IgtimiConnectionFactory.class);
        baseDomainFactory = getService().getBaseDomainFactory();
        mongoObjectFactory = getService().getMongoObjectFactory();
        domainObjectFactory = getService().getDomainObjectFactory();
        // TODO what about passing on the mongo/domain object factory to obtain an according SwissTimingAdapterPersistence instance similar to how the tractracDomainObjectFactory etc. are created below?
        swissTimingAdapterPersistence = SwissTimingAdapterPersistence.INSTANCE;
        swissTimingReplayService = ServiceTrackerFactory.createAndOpen(context, SwissTimingReplayServiceFactory.class)
                .getService().createSwissTimingReplayService(getSwissTimingAdapter().getSwissTimingDomainFactory(),
                /* raceLogResolver */ getService());
        expeditionConnectorTracker = ServiceTrackerFactory.createAndOpen(context, ExpeditionTrackerFactory.class);
        scoreCorrectionProviderServiceTracker = ServiceTrackerFactory.createAndOpen(context,
                ScoreCorrectionProvider.class);
        competitorProviderServiceTracker = ServiceTrackerFactory.createAndOpen(context, CompetitorProvider.class);
        tractracDomainObjectFactory = com.sap.sailing.domain.tractracadapter.persistence.PersistenceFactory.INSTANCE
                .createDomainObjectFactory(mongoObjectFactory.getDatabase(), getTracTracAdapter()
                        .getTracTracDomainFactory());
        tractracMongoObjectFactory = com.sap.sailing.domain.tractracadapter.persistence.MongoObjectFactory.INSTANCE;
        swissTimingFactory = SwissTimingFactory.INSTANCE;
        leaderboardDifferenceCacheByIdPair = new LinkedHashMap<com.sap.sse.common.Util.Pair<String, String>, IncrementalLeaderboardDTO>(LEADERBOARD_DIFFERENCE_CACHE_SIZE, 0.75f, /* accessOrder */ true) {
            private static final long serialVersionUID = 3775119859130148488L;
            @Override
            protected boolean removeEldestEntry(Entry<com.sap.sse.common.Util.Pair<String, String>, IncrementalLeaderboardDTO> eldest) {
                return this.size() > LEADERBOARD_DIFFERENCE_CACHE_SIZE;
            }
        };
        leaderboardByNameResultsCacheById = new LinkedHashMap<String, LeaderboardDTO>(LEADERBOARD_BY_NAME_RESULTS_CACHE_BY_ID_SIZE, 0.75f, /* accessOrder */ true) {
            private static final long serialVersionUID = 3775119859130148488L;
            @Override
            protected boolean removeEldestEntry(Entry<String, LeaderboardDTO> eldest) {
                return this.size() > LEADERBOARD_BY_NAME_RESULTS_CACHE_BY_ID_SIZE;
            }
        };
        // When many updates are triggered in a short period of time by a single thread, ensure that the single thread
        // providing the updates is not outperformed by all the re-calculations happening here. Leave at least one
        // core to other things, but by using at least three threads ensure that no simplistic deadlocks may occur.
        executor = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor();
        serverStringMessages = new ResourceBundleStringMessagesImpl(STRING_MESSAGES_BASE_NAME,
                this.getClass().getClassLoader(), StandardCharsets.UTF_8.name());
        if (context != null) {
            activator.setSailingService(this); // register so this service is informed when the bundle shuts down
        }
    }
    
    /**
     * Stops this service and frees its resources. In particular, caching services and threads owned by this service will be
     * notified to stop their jobs.
     */
    //UNCLEAR
    public void stop() {
        quickRanksLiveCache.stop();
    }

    protected SwissTimingAdapterFactory getSwissTimingAdapterFactory() {
        return swissTimingAdapterTracker.getService();
    }

    protected SwissTimingAdapter getSwissTimingAdapter() {
        return getSwissTimingAdapterFactory().getOrCreateSwissTimingAdapter(baseDomainFactory);
    }
    
    protected TracTracAdapterFactory getTracTracAdapterFactory() {
        return tractracAdapterTracker.getService();
    }

    protected TracTracAdapter getTracTracAdapter() {
        return getTracTracAdapterFactory().getOrCreateTracTracAdapter(baseDomainFactory);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    //READ
    @Override
    public Iterable<String> getScoreCorrectionProviderNames() {
        List<String> result = new ArrayList<String>();
        for (ScoreCorrectionProvider scoreCorrectionProvider : getAllScoreCorrectionProviders()) {
            result.add(scoreCorrectionProvider.getName());
        }
        return result;
    }

    //READ
    @Override
    public ScoreCorrectionProviderDTO getScoreCorrectionsOfProvider(String providerName) throws Exception {
        ScoreCorrectionProviderDTO result = null;
        for (ScoreCorrectionProvider scoreCorrectionProvider : getAllScoreCorrectionProviders()) {
            if (scoreCorrectionProvider.getName().equals(providerName)) {
                result = convertScoreCorrectionProviderDTO(scoreCorrectionProvider);
                break;
            }
        }
        return result;
    }

    // READ
    private Iterable<ScoreCorrectionProvider> getAllScoreCorrectionProviders() {
        final ScoreCorrectionProvider[] services = scoreCorrectionProviderServiceTracker.getServices(new ScoreCorrectionProvider[0]);
        List<ScoreCorrectionProvider> result = new ArrayList<ScoreCorrectionProvider>();
        if (services != null) {
            for (final ScoreCorrectionProvider service : services) {
                result.add(service);
            }
        }
        return result;
    }

    //READ
    private ScoreCorrectionProviderDTO convertScoreCorrectionProviderDTO(ScoreCorrectionProvider scoreCorrectionProvider)
            throws Exception {
        Map<String, Set<com.sap.sse.common.Util.Pair<String, Date>>> hasResultsForBoatClassFromDateByEventName = new HashMap<String, Set<com.sap.sse.common.Util.Pair<String,Date>>>();
        for (Map.Entry<String, Set<com.sap.sse.common.Util.Pair<String, TimePoint>>> e : scoreCorrectionProvider
                .getHasResultsForBoatClassFromDateByEventName().entrySet()) {
            Set<com.sap.sse.common.Util.Pair<String, Date>> set = new HashSet<com.sap.sse.common.Util.Pair<String, Date>>();
            for (com.sap.sse.common.Util.Pair<String, TimePoint> p : e.getValue()) {
                set.add(new com.sap.sse.common.Util.Pair<String, Date>(p.getA(), p.getB().asDate()));
            }
            hasResultsForBoatClassFromDateByEventName.put(e.getKey(), set);
        }
        return new ScoreCorrectionProviderDTO(scoreCorrectionProvider.getName(), hasResultsForBoatClassFromDateByEventName);
    }

  //READ
    @Override
    public Iterable<String> getCompetitorProviderNames() {
        List<String> result = new ArrayList<>();
        for (CompetitorProvider competitorProvider : getAllCompetitorProviders()) {
            result.add(competitorProvider.getName());
        }
        return result;
    }

    //READ
    private Iterable<CompetitorProvider> getAllCompetitorProviders() {
        final CompetitorProvider[] services = competitorProviderServiceTracker.getServices(new CompetitorProvider[0]);
        List<CompetitorProvider> result = new ArrayList<>();
        if (services != null) {
            for (final CompetitorProvider service : services) {
                result.add(service);
            }
        }
        return result;
    }

  //READ
    @Override
    public CompetitorProviderDTO getCompetitorProviderDTOByName(String providerName) throws Exception {
        for (CompetitorProvider competitorProvider : getAllCompetitorProviders()) {
            if (competitorProvider.getName().equals(providerName)) {
                return new CompetitorProviderDTO(competitorProvider.getName(),
                        new HashMap<>(competitorProvider.getHasCompetitorsForRegattasInEvent()));
            }
        }
        return null;
    }

    //READ
    @Override
    public List<CompetitorDescriptor> getCompetitorDescriptors(String competitorProviderName, String eventName,
            String regattaName) throws Exception {
        final Regatta regatta = getService().getRegattaByName(regattaName);
        getSecurityService().checkCurrentUserReadPermission(regatta);
        for (CompetitorProvider cp : getAllCompetitorProviders()) {
            if (cp.getName().equals(competitorProviderName)) {
                final List<CompetitorDescriptor> result = new ArrayList<>();
                Util.addAll(cp.getCompetitorDescriptors(eventName, regattaName), result);
                return result;
            }
        }
        return Collections.emptyList();
    }
    
    //READ
    @Override
    public SerializationDummy serializationDummy(PersonDTO dummy,
            CountryCode ccDummy, PreciseCompactPosition preciseCompactPosition,
            TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, SecondsDurationImpl secondsDuration,
            KnotSpeedImpl knotSpeedImpl, KilometersPerHourSpeedImpl kmhSpeedImpl) {
        return null;
    }

    /**
     * If <code>date</code> is <code>null</code>, the {@link LiveLeaderboardUpdater} for the
     * <code>leaderboardName</code> requested is obtained or created if it doesn't exist yet. The request is then passed
     * on to the live leaderboard updater which will respond with its live {@link LeaderboardDTO} if it has at least the
     * columns requested as per <code>namesOfRaceColumnsForWhichToLoadLegDetails</code>. Otherwise, the updater will add
     * the missing columns to its profile and start a synchronous computation for the requesting client, the result of
     * which will be used as live leaderboard cache update.
     * <p>
     * 
     * Otherwise, the leaderboard is computed synchronously on the fly.
     * @param previousLeaderboardId
     *            if <code>null</code> or no leaderboard with that {@link LeaderboardDTO#getId() ID} is known, a
     *            {@link FullLeaderboardDTO} will be computed; otherwise, an {@link IncrementalLeaderboardDTO} will be
     *            computed as the difference between the new, resulting leaderboard and the previous leaderboard.
     */
    //READ
    @Override
    public IncrementalOrFullLeaderboardDTO getLeaderboardByName(final String leaderboardName, final Date date,
            final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            String previousLeaderboardId, boolean fillTotalPointsUncorrected) throws NoWindException, InterruptedException, ExecutionException,
            IllegalArgumentException {
        Leaderboard leaderBoard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderBoard);
        if (leaderBoard instanceof RegattaLeaderboard) {
            getSecurityService().checkCurrentUserReadPermission(((RegattaLeaderboard) leaderBoard).getRegatta());
        }
        return getLeaderBoardByNameInternal(leaderboardName, date, namesOfRaceColumnsForWhichToLoadLegDetails,
                addOverallDetails, previousLeaderboardId, fillTotalPointsUncorrected);
    }

    @Override
    public List<CourseAreaDTO> getCourseAreas(String leaderboardName) {
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        final List<CourseAreaDTO> result = new ArrayList<>();
        for (final CourseArea courseArea : leaderboard.getCourseAreas()) {
            result.add(convertToCourseAreaDTO(courseArea));
        }
        return result;
    }

  //READ
    @Override
    public IncrementalOrFullLeaderboardDTO getLeaderboardForRace(final RegattaAndRaceIdentifier race,
            final String leaderboardName, final Date date,
            final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            String previousLeaderboardId, boolean fillTotalPointsUncorrected)
            throws NoWindException, InterruptedException, ExecutionException, IllegalArgumentException {
        final DynamicTrackedRace trackedRace = getService().getExistingTrackedRace(race);
        final IncrementalOrFullLeaderboardDTO result;
        if (trackedRace == null) {
            result = null;
        } else {
            final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
            if (leaderboard.getRaceColumnAndFleet(trackedRace) == null) {
                // this race does not seem to be contained in the leaderboard, also check leaderboard
                getSecurityService().checkCurrentUserReadPermission(leaderboard);
            }
            getSecurityService().checkCurrentUserReadPermission(trackedRace);
            result = getLeaderBoardByNameInternal(leaderboardName, date, namesOfRaceColumnsForWhichToLoadLegDetails,
                    addOverallDetails, previousLeaderboardId, fillTotalPointsUncorrected);
        }
        return result;
    }
  //READ
    private IncrementalOrFullLeaderboardDTO getLeaderBoardByNameInternal(final String leaderboardName,
            final Date date, final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails,
            boolean addOverallDetails, String previousLeaderboardId, boolean fillTotalPointsUncorrected)
            throws NoWindException, InterruptedException, ExecutionException {
        try {
            long startOfRequestHandling = System.currentTimeMillis();
            IncrementalOrFullLeaderboardDTO result = null;
            final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
            if (leaderboard != null) {
                TimePoint timePoint;
                if (date == null) {
                    timePoint = null;
                } else {
                    timePoint = new MillisecondsTimePoint(date);
                }
                LeaderboardDTO leaderboardDTO = leaderboard.getLeaderboardDTO(timePoint,
                        namesOfRaceColumnsForWhichToLoadLegDetails, addOverallDetails, getService(), baseDomainFactory, fillTotalPointsUncorrected);
                LeaderboardDTO previousLeaderboardDTO = null;
                synchronized (leaderboardByNameResultsCacheById) {
                    leaderboardByNameResultsCacheById.put(leaderboardDTO.getId(), leaderboardDTO);
                    if (previousLeaderboardId != null) {
                        previousLeaderboardDTO = leaderboardByNameResultsCacheById.get(previousLeaderboardId);
                    }
                }
                // Un-comment the following lines if you need to update the file used by LeaderboardDTODiffingTest, set a breakpoint
                // and toggle the storeLeaderboardForTesting flag if you found a good version. See also bug 1417.
                // The leaderboard that the test wants to use is that of the 505 Worlds 2013, obtained for
                // an expanded Race R9 at time 2013-05-03T19:17:09Z after the last competitor tracked has finished the last leg. The
                // total distance traveled in meters has to be expanded for the test to work.
                boolean storeLeaderboardForTesting = false;
                if (storeLeaderboardForTesting) {
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("C:/data/SAP/sailing/workspace/java/com.sap.sailing.domain.test/resources/IncrementalLeaderboardDTO.ser")));
                    oos.writeObject(leaderboardDTO);
                    oos.close();
                }
                final IncrementalLeaderboardDTO cachedDiff;
                if (previousLeaderboardId != null) {
                    synchronized (leaderboardDifferenceCacheByIdPair) {
                        cachedDiff = leaderboardDifferenceCacheByIdPair.get(new com.sap.sse.common.Util.Pair<String, String>(previousLeaderboardId, leaderboardDTO.getId()));
                    }
                    if (cachedDiff == null) {
                        leaderboardDifferenceCacheByIdPairMisses++;
                    } else {
                        leaderboardDifferenceCacheByIdPairHits++;
                    }
                } else {
                    cachedDiff = null;
                }
                if (previousLeaderboardDTO == null) {
                    result = new FullLeaderboardDTO(leaderboardDTO);
                } else {
                    final IncrementalLeaderboardDTO incrementalResult;
                    if (cachedDiff == null) {
                        IncrementalLeaderboardDTO preResult = new IncrementalLeaderboardDTOCloner().clone(leaderboardDTO).strip(previousLeaderboardDTO);
                        synchronized (leaderboardDifferenceCacheByIdPair) {
                            leaderboardDifferenceCacheByIdPair.put(new com.sap.sse.common.Util.Pair<String, String>(previousLeaderboardId, leaderboardDTO.getId()), preResult);
                        }
                        incrementalResult = preResult;
                    } else {
                        incrementalResult = cachedDiff;
                    }
                    incrementalResult.setCurrentServerTime(new Date()); // may update a cached object, but we consider a reference update atomic
                    result = incrementalResult;
                }
                logger.fine("getLeaderboardByName(" + leaderboardName + ", " + date + ", "
                        + namesOfRaceColumnsForWhichToLoadLegDetails + ", addOverallDetails=" + addOverallDetails
                        + ") took " + (System.currentTimeMillis() - startOfRequestHandling)
                        + "ms; diff cache hits/misses " + leaderboardDifferenceCacheByIdPairHits + "/"
                        + leaderboardDifferenceCacheByIdPairMisses);
            }
            return result;
        } catch (NoWindException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } catch (ExecutionException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE,"Exception during SailingService.getLeaderboardByName", e);
            throw new RuntimeException(e);
        }
    }

  //READ
    @Override
    public List<RegattaDTO> getRegattas() {
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                getService().getAllRegattas(), this::convertToRegattaDTO);
    }
    
  //READ
    @Override
    public RegattaDTO getRegattaByName(String regattaName) {
        RegattaDTO result = null;
        if (regattaName != null && !regattaName.isEmpty()) {
            Regatta regatta = getService().getRegatta(new RegattaName(regattaName));
            getSecurityService().checkCurrentUserReadPermission(regatta);
            if (regatta != null) {
                result = convertToRegattaDTO(regatta);
            }
        }
        return result;
    }
    
    //??
    private MarkDTO convertToMarkDTO(Mark mark, Position position) {
        MarkDTO markDTO;
        if (position != null) {
            markDTO = new MarkDTO(mark.getId().toString(), mark.getName(), mark.getShortName(), position.getLatDeg(), position.getLngDeg());
        } else {
            markDTO = new MarkDTO(mark.getId().toString(), mark.getName(), mark.getShortName());
        }
        markDTO.color = mark.getColor();
        markDTO.shape = mark.getShape();
        markDTO.pattern = mark.getPattern();
        markDTO.type = mark.getType();
        return markDTO;
    }
    
    //READ
    protected RegattaDTO convertToRegattaDTO(Regatta regatta) {
        RegattaDTO regattaDTO = new RegattaDTO(regatta.getName(), regatta.getScoringScheme().getType());
        regattaDTO.races = convertToRaceDTOs(regatta);
        regattaDTO.series = convertToSeriesDTOs(regatta);
        regattaDTO.startDate = regatta.getStartDate() != null ? regatta.getStartDate().asDate() : null;
        regattaDTO.endDate = regatta.getEndDate() != null ? regatta.getEndDate().asDate() : null;
        BoatClass boatClass = regatta.getBoatClass();
        if (boatClass != null) {
            regattaDTO.boatClass = convertToBoatClassDTO(boatClass);
        }
        regattaDTO.courseAreas = new ArrayList<>();
        Util.addAll(Util.map(regatta.getCourseAreas(), this::convertToCourseAreaDTO), regattaDTO.courseAreas);
        regattaDTO.buoyZoneRadiusInHullLengths = regatta.getBuoyZoneRadiusInHullLengths();
        regattaDTO.useStartTimeInference = regatta.useStartTimeInference();
        regattaDTO.controlTrackingFromStartAndFinishTimes = regatta.isControlTrackingFromStartAndFinishTimes();
        regattaDTO.autoRestartTrackingUponCompetitorSetChange = regatta.isAutoRestartTrackingUponCompetitorSetChange();
        regattaDTO.canBoatsOfCompetitorsChangePerRace = regatta.canBoatsOfCompetitorsChangePerRace();
        regattaDTO.competitorRegistrationType = regatta.getCompetitorRegistrationType();
        regattaDTO.configuration = convertToRegattaConfigurationDTO(regatta.getRegattaConfiguration());
        regattaDTO.rankingMetricType = regatta.getRankingMetricType();
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), regattaDTO, regatta.getIdentifier());
        regattaDTO.registrationLinkSecret = regatta.getRegistrationLinkSecret();
        return regattaDTO;
    }
    
    //??
    private BoatClassDTO convertToBoatClassDTO(BoatClass boatClass) {
        return boatClass==null?null:new BoatClassDTO(boatClass.getName(), boatClass.getDisplayName(), boatClass.getHullLength(), boatClass.getHullBeam());
    }

    //??
    private List<SeriesDTO> convertToSeriesDTOs(Regatta regatta) {
        List<SeriesDTO> result = new ArrayList<SeriesDTO>();
        for (Series series : regatta.getSeries()) {
            SeriesDTO seriesDTO = convertToSeriesDTO(series);
            result.add(seriesDTO);
        }
        return result;
    }

    //READ
    private SeriesDTO convertToSeriesDTO(Series series) {
        List<FleetDTO> fleets = new ArrayList<FleetDTO>();
        for (Fleet fleet : series.getFleets()) {
            fleets.add(baseDomainFactory.convertToFleetDTO(fleet));
        }
        List<RaceColumnDTO> raceColumns = convertToRaceColumnDTOs(series.getRaceColumns());
        SeriesDTO result = new SeriesDTO(series.getName(), fleets, raceColumns, series.isMedal(), series.isFleetsCanRunInParallel(),
                series.getResultDiscardingRule() == null ? null : series.getResultDiscardingRule().getDiscardIndexResultsStartingWithHowManyRaces(),
                        series.isStartsWithZeroScore(), series.isFirstColumnIsNonDiscardableCarryForward(), series.hasSplitFleetContiguousScoring(),
                        series.getMaximumNumberOfDiscards());
        return result;
    }

    // READ
    protected void fillRaceColumnDTO(RaceColumn raceColumn, RaceColumnDTO raceColumnDTO) {
        raceColumnDTO.setName(raceColumn.getName());
        raceColumnDTO.setMedalRace(raceColumn.isMedalRace());
        raceColumnDTO.setExplicitFactor(raceColumn.getExplicitFactor());
    }
    
    //READ
    private List<RaceColumnDTO> convertToRaceColumnDTOs(Iterable<? extends RaceColumn> raceColumns) {
        List<RaceColumnDTO> raceColumnDTOs = new ArrayList<RaceColumnDTO>();
        RaceColumnDTOFactory columnFactory = RaceColumnDTOFactory.INSTANCE;
        for (RaceColumn raceColumn : raceColumns) {
            final RaceColumnDTO raceColumnDTO = columnFactory.createRaceColumnDTO(raceColumn.getName(),
                    raceColumn.isMedalRace(), raceColumn.getExplicitFactor(),
                    raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getRegatta().getName() : null,
                    raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getSeries().getName() : null,
                    raceColumn instanceof MetaLeaderboardColumn);
            raceColumnDTOs.add(raceColumnDTO);
        }
        return raceColumnDTOs;
    }
    
    //READ racestatecache
    private RaceInfoDTO createRaceInfoDTO(String seriesName, RaceColumn raceColumn, Fleet fleet,
            RaceLog raceLog, ReadonlyRaceState state) {
        RaceInfoDTO raceInfoDTO = new RaceInfoDTO();
        final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
        raceInfoDTO.isTracked = trackedRace != null ? true : false;
        if (raceLog != null) {
            TimePoint startTime = state.getStartTime();
            if (startTime != null) {
                raceInfoDTO.startTime = startTime.asDate();
            }
            raceInfoDTO.lastStatus = state.getStatus();
            if (raceLog.getLastRawFix() != null) {
                raceInfoDTO.lastUpdateTime = raceLog.getLastRawFix().getCreatedAt().asDate();
            }
            TimePoint finishedTime = state.getFinishedTime();
            if (finishedTime != null) {
                raceInfoDTO.finishedTime = finishedTime.asDate();
            } else {
                raceInfoDTO.finishedTime = null;
                if (raceInfoDTO.isTracked) {
                    TimePoint endOfRace = trackedRace.getEndOfRace();
                    raceInfoDTO.finishedTime = endOfRace != null ? endOfRace.asDate() : null;
                }
            }
            final TimePoint now = MillisecondsTimePoint.now();
            if (startTime != null) {
                FlagPoleState activeFlagState = state.getRacingProcedure().getActiveFlags(startTime, now);
                List<FlagPole> activeFlags = activeFlagState.getCurrentState();
                FlagPoleState previousFlagState = activeFlagState.getPreviousState(state.getRacingProcedure(), startTime);
                List<FlagPole> previousFlags = previousFlagState.getCurrentState();
                FlagPole mostInterestingFlagPole = FlagPoleState.getMostInterestingFlagPole(previousFlags, activeFlags);
                // TODO: adapt the LastFlagFinder#getMostRecent method!
                if (mostInterestingFlagPole != null) {
                    raceInfoDTO.lastUpperFlag = mostInterestingFlagPole.getUpperFlag();
                    raceInfoDTO.lastLowerFlag = mostInterestingFlagPole.getLowerFlag();
                    raceInfoDTO.lastFlagsAreDisplayed = mostInterestingFlagPole.isDisplayed();
                    raceInfoDTO.lastFlagsDisplayedStateChanged = previousFlagState.hasPoleChanged(mostInterestingFlagPole);
                }
            }
            AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
            RaceLogFlagEvent abortingFlagEvent = abortingFlagFinder.analyze();
            if (abortingFlagEvent != null) {
                raceInfoDTO.isRaceAbortedInPassBefore = true;
                raceInfoDTO.abortingTimeInPassBefore = abortingFlagEvent.getLogicalTimePoint().asDate();
                
                if (raceInfoDTO.lastStatus == RaceLogRaceStatus.UNSCHEDULED || raceInfoDTO.lastStatus == RaceLogRaceStatus.PRESCHEDULED) {
                    raceInfoDTO.lastUpperFlag = abortingFlagEvent.getUpperFlag();
                    raceInfoDTO.lastLowerFlag = abortingFlagEvent.getLowerFlag();
                    raceInfoDTO.lastFlagsAreDisplayed = abortingFlagEvent.isDisplayed();
                    raceInfoDTO.lastFlagsDisplayedStateChanged = true;
                }
            }
            CourseBase lastCourse = state.getCourseDesign();
            if (lastCourse != null) {
                raceInfoDTO.lastCourseDesign = convertToRaceCourseDTO(lastCourse, new TrackedRaceMarkPositionFinder(trackedRace), now);
                raceInfoDTO.lastCourseName = lastCourse.getName();
            }
            if (raceInfoDTO.lastStatus.equals(RaceLogRaceStatus.FINISHED)) {
                if (state.getProtestTime() != null) {
                    final TimePoint protestEndTime = state.getProtestTime().to();
                    if (protestEndTime != null) {
                        final TimePoint protestStartTime = state.getProtestTime().from();
                        raceInfoDTO.protestStartTime = protestStartTime == null ? null : protestStartTime.asDate();
                        raceInfoDTO.protestFinishTime = protestEndTime.asDate();
                        raceInfoDTO.lastUpperFlag = Flags.BRAVO;
                        raceInfoDTO.lastLowerFlag = Flags.NONE;
                        raceInfoDTO.lastFlagsAreDisplayed = true;
                        raceInfoDTO.lastFlagsDisplayedStateChanged = true;
                    }
                }
            }
            Wind wind = state.getWindFix();
            if (wind != null) {
                raceInfoDTO.lastWind = createWindDTOFromAlreadyAveraged(wind, now);
            }
            fillStartProcedureSpecifics(raceInfoDTO, state);
        }
        raceInfoDTO.seriesName = seriesName;
        raceInfoDTO.raceName = raceColumn.getName();
        raceInfoDTO.fleetName = fleet.getName();
        raceInfoDTO.fleetOrdering = fleet.getOrdering();
        raceInfoDTO.raceIdentifier = raceColumn.getRaceIdentifier(fleet);
        return raceInfoDTO;
    }
    
    //READ
    private void fillStartProcedureSpecifics(RaceInfoDTO raceInfoDTO, ReadonlyRaceState state) {
        RaceInfoExtensionDTO info = null;
        raceInfoDTO.startProcedure = state.getRacingProcedure().getType();
        switch (raceInfoDTO.startProcedure) {
        case GateStart:
            ReadonlyGateStartRacingProcedure gateStart = state.getTypedReadonlyRacingProcedure();
            info = new GateStartInfoDTO(gateStart.getPathfinder(), gateStart.getGateLaunchStopTime());
            break;
        case RRS26:
        case SWC:
            ConfigurableStartModeFlagRacingProcedure linestart = state.getTypedReadonlyRacingProcedure();
            info = new LineStartInfoDTO(linestart.getStartModeFlag());
        case UNKNOWN:
        default:
            break;
        }
        raceInfoDTO.startProcedureDTO = info;
    }

    //READ
    private List<RaceWithCompetitorsAndBoatsDTO> convertToRaceDTOs(Regatta regatta) {
        List<RaceWithCompetitorsAndBoatsDTO> result = new ArrayList<RaceWithCompetitorsAndBoatsDTO>();
        for (RaceDefinition r : regatta.getAllRaces()) {
            RegattaAndRaceIdentifier raceIdentifier = new RegattaNameAndRaceName(regatta.getName(), r.getName());
            TrackedRace trackedRace = getService().getExistingTrackedRace(raceIdentifier);
            TrackedRaceDTO trackedRaceDTO = null; 
            final RankingMetrics rankingMetricType;
            if (trackedRace != null) {
                trackedRaceDTO = getBaseDomainFactory().createTrackedRaceDTO(trackedRace);
                rankingMetricType = trackedRace.getRankingMetric().getType();
            } else {
                rankingMetricType = null;
            }
            Map<CompetitorDTO, BoatDTO> competitorAndBoatDTOs = baseDomainFactory.convertToCompetitorAndBoatDTOs(r.getCompetitorsAndTheirBoats());
            RaceWithCompetitorsAndBoatsDTO raceDTO = new RaceWithCompetitorsAndBoatsDTO(raceIdentifier, competitorAndBoatDTOs,
                    trackedRaceDTO, getService().isRaceBeingTracked(regatta, r), rankingMetricType);
            if (trackedRace != null) {
                SecurityDTOUtil.addSecurityInformation(getSecurityService(), raceDTO, trackedRace.getIdentifier());
                getBaseDomainFactory().updateRaceDTOWithTrackedRaceData(trackedRace, raceDTO);
            }
            raceDTO.boatClass = regatta.getBoatClass() == null ? null : regatta.getBoatClass().getName(); 
            result.add(raceDTO);
        }
        return result;
    }

    /**
     * Converts the {@link Competitor} objects passed as {@code iterable} to {@link CompetitorDTO} objects.
     * The iteration order in the result matches that of the {@code iterable} passed.
     */
    //READ
    private List<CompetitorDTO> convertToCompetitorDTOs(Iterable<? extends Competitor> iterable) {
        List<CompetitorDTO> result = new ArrayList<>();
        for (Competitor c : iterable) {
            CompetitorDTO competitorDTO = convertToCompetitorDTO(c);
            result.add(competitorDTO);
        }
        return result;
    }
    
    //READ
    protected CompetitorDTO convertToCompetitorDTO(Competitor competitor) {
        CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorDTO, competitor.getIdentifier());
        clearNonPublicFieldsIfCurrentUserHasNoReadPermission(competitor, competitorDTO);
        return competitorDTO;
    }

    private void clearNonPublicFieldsIfCurrentUserHasNoReadPermission(Competitor competitor,
            CompetitorDTO competitorDTO) {
        if (!getSecurityService().hasCurrentUserReadPermission(competitor)) {
            // probably only READ_PUBLIC; remove e-mail address from DTO:
            competitorDTO.clearNonPublicFields();
        }
    }
    
    //READ
    protected CompetitorWithBoatDTO convertToCompetitorWithBoatDTO(CompetitorWithBoat competitor) {
        CompetitorWithBoatDTO competitorDTO = baseDomainFactory.convertToCompetitorWithBoatDTO(competitor);
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorDTO, competitor.getIdentifier());
        clearNonPublicFieldsIfCurrentUserHasNoReadPermission(competitor, competitorDTO);
        BoatDTO boatDTO = competitorDTO.getBoat();
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), boatDTO, competitor.getBoat().getIdentifier());
        return competitorDTO;
    }

    /**
     * Converts the {@link Competitor} objects passed as {@code iterable} to {@link CompetitorWithBoatDTO} objects with an empty boat.
     * The iteration order in the result matches that of the {@code iterable} passed.
     */
    //READ
    private List<CompetitorAndBoatDTO> convertToCompetitorAndBoatDTOs(Map<? extends Competitor, ? extends Boat> competitorsAndTheirBoats) {
        List<CompetitorAndBoatDTO> result = new ArrayList<>();
        for (final Entry<? extends Competitor, ? extends Boat> c : competitorsAndTheirBoats.entrySet()) {
            CompetitorAndBoatDTO competitorAndBoatDTO = baseDomainFactory.convertToCompetitorAndBoatDTO(c.getKey(), c.getValue());
            SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorAndBoatDTO.getCompetitor(), c.getKey().getIdentifier());
            clearNonPublicFieldsIfCurrentUserHasNoReadPermission(c.getKey(), competitorAndBoatDTO.getCompetitor());
            SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorAndBoatDTO.getBoat(), c.getValue().getIdentifier());
            result.add(competitorAndBoatDTO);
        }
        return result;
    }

    /**
     * Converts the {@link Competitor} objects passed as {@code iterable} to {@link CompetitorWithBoatDTO} objects with an empty boat.
     * The iteration order in the result matches that of the {@code iterable} passed.
     */
    //READ
    protected List<CompetitorWithBoatDTO> convertToCompetitorWithBoatDTOs(Iterable<? extends CompetitorWithBoat> iterable) {
        List<CompetitorWithBoatDTO> result = new ArrayList<>();
        for (CompetitorWithBoat c : iterable) {
            CompetitorWithBoatDTO competitorDTO = baseDomainFactory.convertToCompetitorWithBoatDTO(c);
            SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorDTO, c.getIdentifier());
            clearNonPublicFieldsIfCurrentUserHasNoReadPermission(c, competitorDTO);
            SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorDTO.getBoat(), c.getBoat().getIdentifier());
            result.add(competitorDTO);
        }
        return result;
    }

    /**
     * Converts the {@link Boat} objects passed as {@code iterable} to {@link BoatDTO} objects.
     * The iteration order in the result matches that of the {@code iterable} passed.
     */
    //READ
    private List<BoatDTO> convertToBoatDTOs(Iterable<? extends Boat> iterable) {
        List<BoatDTO> result = new ArrayList<BoatDTO>();
        for (Boat b : iterable) {
            BoatDTO boatDTO = baseDomainFactory.convertToBoatDTO(b);
            SecurityDTOUtil.addSecurityInformation(getSecurityService(), boatDTO, b.getIdentifier());
            result.add(boatDTO);
        }
        return result;
    }
    
    //READ
    protected BoatDTO convertToBoatDTO(Boat boat) {
        BoatDTO boatDTO = baseDomainFactory.convertToBoatDTO(boat);
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), boatDTO, boat.getIdentifier());
        return boatDTO;
    }

    @Override
    //READ
    public com.sap.sse.common.Util.Pair<String, List<TracTracRaceRecordDTO>> listTracTracRacesInEvent(String eventJsonURL, boolean listHiddenRaces) throws MalformedURLException, IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        com.sap.sse.common.Util.Pair<String,List<RaceRecord>> raceRecords;
        raceRecords = getTracTracAdapter().getTracTracRaceRecords(new URL(eventJsonURL), /*loadClientParam*/ false);
        List<TracTracRaceRecordDTO> result = new ArrayList<TracTracRaceRecordDTO>();
        for (RaceRecord raceRecord : raceRecords.getB()) {
            if (listHiddenRaces == false && raceRecord.getRaceVisibility().equals(TracTracConnectionConstants.HIDDEN_VISIBILITY)) {
                continue;
            }
            result.add(new TracTracRaceRecordDTO(raceRecord.getID(), raceRecord.getEventName(), raceRecord.getName(),
                    raceRecord.getTrackingStartTime().asDate(), 
                    raceRecord.getTrackingEndTime().asDate(), raceRecord.getRaceStartTime() == null ? null : raceRecord.getRaceStartTime().asDate(),
                    raceRecord.getBoatClassNames(), raceRecord.getRaceStatus(), raceRecord.getRaceVisibility(), raceRecord.getJsonURL().toString(),
                    hasRememberedRegatta(raceRecord.getID())));
        }
        return new com.sap.sse.common.Util.Pair<String, List<TracTracRaceRecordDTO>>(raceRecords.getA(), result);
    }

    //READ
    private boolean hasRememberedRegatta(Serializable raceID) {
        return getService().getRememberedRegattaForRace(raceID) != null;
    }

    @Override
    //READ
    public List<TracTracConfigurationWithSecurityDTO> getPreviousTracTracConfigurations() throws Exception {
        final Iterable<TracTracConfiguration> configs = tractracDomainObjectFactory.getTracTracConfigurations();
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                configs,
                ttConfig -> {
                    TracTracConfigurationWithSecurityDTO config = new TracTracConfigurationWithSecurityDTO(
                            ttConfig.getName(),
                        ttConfig.getJSONURL().toString(),
                        ttConfig.getLiveDataURI()==null?null:ttConfig.getLiveDataURI().toString(),
                        ttConfig.getStoredDataURI()==null?null:ttConfig.getStoredDataURI().toString(),
                        ttConfig.getCourseDesignUpdateURI()==null?null:ttConfig.getCourseDesignUpdateURI().toString(),
                        ttConfig.getTracTracUsername(), ttConfig.getTracTracPassword(), ttConfig.getCreatorName());
                    SecurityDTOUtil.addSecurityInformation(getSecurityService(), config, config.getIdentifier());
                    return config;
                });
    }
    
    //READ
    private RaceDefinition getRaceByName(Regatta regatta, String raceName) {
        if (regatta != null) {
            return regatta.getRaceByName(raceName);
        } else {
            return null;
        }
    }
    
    @Override
    //READ
    public WindInfoForRaceDTO getRawWindFixes(RegattaAndRaceIdentifier raceIdentifier, Collection<WindSource> windSources) {
        WindInfoForRaceDTO result = null;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        if (trackedRace != null) {
            result = new WindInfoForRaceDTO();
            result.raceIsKnownToStartUpwind = trackedRace.raceIsKnownToStartUpwind();
            Map<WindSource, WindTrackInfoDTO> windTrackInfoDTOs = new HashMap<WindSource, WindTrackInfoDTO>();
            result.windTrackInfoByWindSource = windTrackInfoDTOs;

            List<WindSource> windSourcesToDeliver = new ArrayList<WindSource>();
            if (windSources != null) {
                windSourcesToDeliver.addAll(windSources);
            } else {
                windSourcesToDeliver.add(new WindSourceImpl(WindSourceType.EXPEDITION));
                windSourcesToDeliver.add(new WindSourceImpl(WindSourceType.WEB));
            }
            for (WindSource windSource : windSourcesToDeliver) {
                if(windSource.getType() == WindSourceType.WEB) {
                    WindTrackInfoDTO windTrackInfoDTO = new WindTrackInfoDTO();
                    windTrackInfoDTO.windFixes = new ArrayList<WindDTO>();
                    WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
                    windTrackInfoDTO.resolutionOutsideOfWhichNoFixWillBeReturned = windTrack
                            .getResolutionOutsideOfWhichNoFixWillBeReturned();
                    windTrack.lockForRead();
                    try {
                        Iterator<Wind> windIter = windTrack.getRawFixes().iterator();
                        while (windIter.hasNext()) {
                            Wind wind = windIter.next();
                            if(wind != null) {
                                WindDTO windDTO = createWindDTO(wind, windTrack);
                                windTrackInfoDTO.windFixes.add(windDTO);
                            }
                        }
                    } finally {
                        windTrack.unlockAfterRead();
                    }

                    windTrackInfoDTOs.put(windSource, windTrackInfoDTO);
                }
            }
        }
        return result;
    }

    //READ
    protected WindDTO createWindDTO(Wind wind, WindTrack windTrack) {
        WindDTO windDTO = new WindDTO();
        windDTO.trueWindBearingDeg = wind.getBearing().getDegrees();
        windDTO.trueWindFromDeg = wind.getBearing().reverse().getDegrees();
        windDTO.trueWindSpeedInKnots = wind.getKnots();
        windDTO.trueWindSpeedInMetersPerSecond = wind.getMetersPerSecond();
        if (wind.getPosition() != null) {
            windDTO.position = wind.getPosition();
        }
        if (wind.getTimePoint() != null) {
            windDTO.measureTimepoint = wind.getTimePoint().asMillis();
            Wind estimatedWind = windTrack
                    .getAveragedWind(wind.getPosition(), wind.getTimePoint());
            if (estimatedWind != null) {
                windDTO.dampenedTrueWindBearingDeg = estimatedWind.getBearing().getDegrees();
                windDTO.dampenedTrueWindFromDeg = estimatedWind.getBearing().reverse().getDegrees();
                windDTO.dampenedTrueWindSpeedInKnots = estimatedWind.getKnots();
                windDTO.dampenedTrueWindSpeedInMetersPerSecond = estimatedWind.getMetersPerSecond();
            }
        }
        return windDTO;
    }

    /**
     * Uses <code>wind</code> for both, the non-dampened and dampened fields of the {@link WindDTO} object returned
     */
    //READ
    protected WindDTO createWindDTOFromAlreadyAveraged(Wind wind, TimePoint requestTimepoint) {
        WindDTO windDTO = new WindDTO();
        windDTO.requestTimepoint = requestTimepoint.asMillis();
        windDTO.trueWindBearingDeg = wind.getBearing().getDegrees();
        windDTO.trueWindFromDeg = wind.getBearing().reverse().getDegrees();
        windDTO.trueWindSpeedInKnots = wind.getKnots();
        windDTO.trueWindSpeedInMetersPerSecond = wind.getMetersPerSecond();
        windDTO.dampenedTrueWindBearingDeg = wind.getBearing().getDegrees();
        windDTO.dampenedTrueWindFromDeg = wind.getBearing().reverse().getDegrees();
        windDTO.dampenedTrueWindSpeedInKnots = wind.getKnots();
        windDTO.dampenedTrueWindSpeedInMetersPerSecond = wind.getMetersPerSecond();
        if (wind.getPosition() != null) {
            windDTO.position = wind.getPosition();
        }
        if (wind.getTimePoint() != null) {
            windDTO.measureTimepoint = wind.getTimePoint().asMillis();
        }
        return windDTO;
    }

    /**
     * @param onlyUpToNewestEvent
     *            if <code>true</code>, no wind data will be returned for time points later than
     *            {@link TrackedRace#getTimePointOfNewestEvent() trackedRace.getTimePointOfNewestEvent()}. This is
     *            helpful in case the client wants to populate a chart during live mode. If <code>false</code>, the
     *            "best effort" readings are provided for the time interval requested, no matter if based on any sensor
     *            evidence or not, regardless of {@link TrackedRace#getTimePointOfNewestEvent()
     *            trackedRace.getTimePointOfNewestEvent()}.
     */
    @Override
    //READ
    public WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth,
            int numberOfFixes, Collection<String> windSourceTypeNames, boolean onlyUpToNewestEvent, boolean includeCombinedWindForAllLegMiddles)
                    throws NoWindException {
        assert from != null;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        WindInfoForRaceDTO result = getAveragedWindInfo(new MillisecondsTimePoint(from), millisecondsStepWidth, numberOfFixes,
                windSourceTypeNames, trackedRace, /* onlyUpToNewestEvent FIXME why not pass through onlyUpToNewestEvent here??? */ true, includeCombinedWindForAllLegMiddles);
        return result;
    }

    /**
     * @param onlyUpToNewestEvent
     *            if <code>true</code>, no wind data will be returned for time points later than
     *            {@link TrackedRace#getTimePointOfNewestEvent() trackedRace.getTimePointOfNewestEvent()}. This is
     *            helpful in case the client wants to populate a chart during live mode. If <code>false</code>, the
     *            "best effort" readings are provided for the time interval requested, no matter if based on any sensor
     *            evidence or not, regardless of {@link TrackedRace#getTimePointOfNewestEvent()
     *            trackedRace.getTimePointOfNewestEvent()}.
     * @param windSourceTypeNames
     *            if {@code null}, all wind sources delivered by {@link TrackedRace#getWindSources()} plus the
     *            {@link WindSourceType#COMBINED} wind source are delivered. Note that this does not include
     *            the {@link WindSourceType#LEG_MIDDLE} wind sources.
     * @param includeCombinedWindForAllLegMiddles
     *            if <code>true</code>, the result will return non-<code>null</code> results for calls to
     *            {@link WindInfoForRaceDTO#getCombinedWindOnLegMiddle(int)}.
     */
    //READ
    private WindInfoForRaceDTO getAveragedWindInfo(TimePoint from, long millisecondsStepWidth, int numberOfFixes,
            Collection<String> windSourceTypeNames, final TrackedRace trackedRace, boolean onlyUpToNewestEvent,
            boolean includeCombinedWindForAllLegMiddles) {
        WindInfoForRaceDTO result = null;
        if (trackedRace != null) {
            TimePoint newestEvent = trackedRace.getTimePointOfNewestEvent();
            result = new WindInfoForRaceDTO();
            result.raceIsKnownToStartUpwind = trackedRace.raceIsKnownToStartUpwind();
            List<WindSource> windSourcesToExclude = new ArrayList<WindSource>();
            for (WindSource windSourceToExclude : trackedRace.getWindSourcesToExclude()) {
                windSourcesToExclude.add(windSourceToExclude);
            }
            result.windSourcesToExclude = windSourcesToExclude;
            Map<WindSource, WindTrackInfoDTO> windTrackInfoDTOs = new HashMap<WindSource, WindTrackInfoDTO>();
            result.windTrackInfoByWindSource = windTrackInfoDTOs;
            final List<WindSource> windSourcesToDeliver = new ArrayList<WindSource>();
            final WindSourceImpl combinedWindSource = new WindSourceImpl(WindSourceType.COMBINED);
            if (windSourceTypeNames == null) {
                Util.addAll(trackedRace.getWindSources(), windSourcesToDeliver);
                windSourcesToDeliver.add(combinedWindSource);
            } else {
                for (final String windSourceTypeToAdd : windSourceTypeNames) {
                    for (final WindSource windSource : trackedRace.getWindSources(WindSourceType.valueOf(windSourceTypeToAdd))) {
                        windSourcesToDeliver.add(windSource);
                    }
                }
            }
            for (final WindSource windSource : windSourcesToDeliver) {
                // TODO consider parallelizing
                WindTrackInfoDTO windTrackInfoDTO = createWindTrackInfoDTO(from, millisecondsStepWidth,
                        numberOfFixes, trackedRace, onlyUpToNewestEvent, newestEvent, windSource, /* use default positions */ at->null);
                windTrackInfoDTOs.put(windSource, windTrackInfoDTO);
            }
            if (includeCombinedWindForAllLegMiddles) {
                int zeroBasedLegNumber = 0;
                for (final TrackedLeg trackedLeg : trackedRace.getTrackedLegs()) {
                    WindTrackInfoDTO windTrackInfoForLegMiddle = createWindTrackInfoDTO(from, millisecondsStepWidth,
                            numberOfFixes, trackedRace, onlyUpToNewestEvent, newestEvent, combinedWindSource,
                            new PositionAtTimeProvider() { @Override public Position getPosition(TimePoint at) { return trackedLeg.getMiddleOfLeg(at); }});
                    result.addWindOnLegMiddle(zeroBasedLegNumber, windTrackInfoForLegMiddle);
                    zeroBasedLegNumber++;
                }
            }
        }
        return result;
    }

    //READ
    private interface PositionAtTimeProvider {
        Position getPosition(TimePoint at);
    }
    
    //READ
    private WindTrackInfoDTO createWindTrackInfoDTO(TimePoint from, long millisecondsStepWidth, int numberOfFixes,
            TrackedRace trackedRace, boolean onlyUpToNewestEvent, TimePoint newestEvent, WindSource windSource,
            PositionAtTimeProvider positionProvider) {
        WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
        WindTrackInfoDTO windTrackInfoDTO = new WindTrackInfoDTO();
        windTrackInfoDTO.resolutionOutsideOfWhichNoFixWillBeReturned = windTrack.getResolutionOutsideOfWhichNoFixWillBeReturned();
        windTrackInfoDTO.windFixes = new ArrayList<WindDTO>();
        windTrackInfoDTO.dampeningIntervalInMilliseconds = windTrack.getMillisecondsOverWhichToAverageWind();
        TimePoint timePoint = from;
        Double minWindConfidence = 2.0;
        Double maxWindConfidence = -1.0;
        for (int i = 0; i < numberOfFixes && (!onlyUpToNewestEvent ||
                (newestEvent != null && timePoint.before(newestEvent))); i++) {
            WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> averagedWindWithConfidence =
                    windTrack.getAveragedWindWithConfidence(positionProvider.getPosition(timePoint), timePoint);
            if (averagedWindWithConfidence != null) {
                if (logger.getLevel() != null && logger.getLevel().equals(Level.FINEST)) {
                    logger.finest("Found averaged wind: " + averagedWindWithConfidence);
                }
                double confidence = averagedWindWithConfidence.getConfidence();
                WindDTO windDTO = createWindDTOFromAlreadyAveraged(averagedWindWithConfidence.getObject(), timePoint);
                windDTO.confidence = confidence;
                windTrackInfoDTO.windFixes.add(windDTO);
                if (confidence < minWindConfidence) {
                    minWindConfidence = confidence;
                }
                if (confidence > maxWindConfidence) {
                    maxWindConfidence = confidence;
                }
            } else {
                if (logger.getLevel() != null && logger.getLevel().equals(Level.FINEST)) {
                    logger.finest("Did NOT find any averaged wind for timepoint " + timePoint + " and tracked race " + trackedRace.getRaceIdentifier().getRaceName());
                }
            }
            timePoint = new MillisecondsTimePoint(timePoint.asMillis() + millisecondsStepWidth);
        }
        windTrackInfoDTO.minWindConfidence = minWindConfidence; 
        windTrackInfoDTO.maxWindConfidence = maxWindConfidence;
        return windTrackInfoDTO;
    }

    /**
     * @param to
     *            if <code>null</code>, data is returned up to end of race; if the end of race is not known and
     *            <code>null</code> is used for this parameter, <code>null</code> is returned.
     * @param onlyUpToNewestEvent
     *            if <code>true</code>, no wind data will be returned for time points later than
     *            {@link TrackedRace#getTimePointOfNewestEvent() trackedRace.getTimePointOfNewestEvent()}. This is
     *            helpful in case the client wants to populate a chart during live mode. If <code>false</code>, the
     *            "best effort" readings are provided for the time interval requested, no matter if based on any sensor
     *            evidence or not, regardless of {@link TrackedRace#getTimePointOfNewestEvent()
     *            trackedRace.getTimePointOfNewestEvent()}.
     */
    @Override
  //READ
    public WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, Date to,
            long resolutionInMilliseconds, Collection<String> windSourceTypeNames, boolean onlyUpToNewestEvent) {
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        WindInfoForRaceDTO result = null;
        if (trackedRace != null) {
            TimePoint fromTimePoint = from == null ? trackedRace.getStartOfTracking() == null ? trackedRace
                    .getStartOfRace() : trackedRace.getStartOfTracking() : new MillisecondsTimePoint(from);
            TimePoint toTimePoint = to == null ? trackedRace.getEndOfRace() == null ?
                    MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis()) : trackedRace.getEndOfRace() : new MillisecondsTimePoint(to);
            if (fromTimePoint != null && toTimePoint != null) {
                int numberOfFixes = Math.min(SailingServiceConstants.MAX_NUMBER_OF_WIND_FIXES_TO_DELIVER_IN_ONE_CALL,
                        (int) ((toTimePoint.asMillis() - fromTimePoint.asMillis())/resolutionInMilliseconds));
                result = getAveragedWindInfo(fromTimePoint, resolutionInMilliseconds, numberOfFixes,
                        windSourceTypeNames, trackedRace, onlyUpToNewestEvent, /* includeCombinedWindForAllLegMiddles */ false);
            }
        }
        return result;
    }

    @Override
  //READ
    public boolean getPolarResults(RegattaAndRaceIdentifier raceIdentifier) {
        final boolean result;
        final TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        final PolarDataService polarData = getService().getPolarDataService();
        if (trackedRace == null || polarData == null) {
            result = false;
        } else {
            BoatClass boatClass = trackedRace.getRace().getBoatClass();
            PolarDiagram polarDiagram;
            try {
                polarDiagram = new PolarDiagramGPS(boatClass, polarData);
            } catch (SparseSimulationDataException e) {
                polarDiagram = null;
            }
            result = polarDiagram != null;
        }
        return result;
    }

    @Override
  //READ
    public SimulatorResultsDTO getSimulatorResults(LegIdentifier legIdentifier) {
        if (!getSecurityService()
                .hasCurrentUserAnyPermission(SecuredDomainType.SIMULATOR.getPermission(DefaultActions.READ))) {
            throw new UnauthorizedException("Not permitted to see simulator results.");
        }
        // get simulation-results from smart-future-cached simulation-service
        SimulatorResultsDTO result = null;
        SimulationService simulationService = getService().getSimulationService();
        if (simulationService == null) 
            return result;
        SimulationResults simulationResults = simulationService.getSimulationResults(legIdentifier);
        if (simulationResults == null) 
            return result;
            // prepare simulator-results-dto
        Map<PathType, Path> paths = simulationResults.getPaths();
        if (paths != null) {
            int noOfPaths = paths.size();
                PathDTO[] pathDTOs = new PathDTO[noOfPaths];
                int index = noOfPaths - 1;
            for (Entry<PathType, Path> entry : paths.entrySet()) {
                pathDTOs[index] = new PathDTO(entry.getKey());
                    // fill pathDTO with path points where speed is true wind speed
                    List<SimulatorWindDTO> wList = new ArrayList<SimulatorWindDTO>();
                    for (TimedPositionWithSpeed p : entry.getValue().getPathPoints()) {
                        wList.add(createSimulatorWindDTO(p));
                    }
                    pathDTOs[index].setPoints(wList);
                pathDTOs[index].setAlgorithmTimedOut(entry.getValue().getAlgorithmTimedOut());
                pathDTOs[index].setMixedLeg(entry.getValue().getMixedLeg());
                    index--;
                }
                RaceMapDataDTO rcDTO;
                rcDTO = new RaceMapDataDTO();
                rcDTO.coursePositions = new CoursePositionsDTO();
                rcDTO.coursePositions.waypointPositions = new ArrayList<Position>();
                rcDTO.coursePositions.waypointPositions.add(simulationResults.getStartPosition());
                rcDTO.coursePositions.waypointPositions.add(simulationResults.getEndPosition());
                result = new SimulatorResultsDTO(simulationResults.getVersion().asMillis(), legIdentifier.getLegNumber()+1, simulationResults.getStartTime(), simulationResults.getTimeStep(),
                        simulationResults.getLegDuration(), rcDTO, pathDTOs, null, null);
            }
        return result;
    }
    
  //READ
    private SimulatorWindDTO createSimulatorWindDTO(TimedPositionWithSpeed timedPositionWithSpeed) {

        Position position = timedPositionWithSpeed.getPosition();
        SpeedWithBearing speedWithBearing = timedPositionWithSpeed.getSpeed();
        TimePoint timePoint = timedPositionWithSpeed.getTimePoint();

        SimulatorWindDTO result = new SimulatorWindDTO();
        if (speedWithBearing == null) {
                result.trueWindBearingDeg = 0.0;
                result.trueWindSpeedInKnots = 0.0;
        } else {
                result.trueWindBearingDeg = speedWithBearing.getBearing().getDegrees();
                result.trueWindSpeedInKnots = speedWithBearing.getKnots();
        }

        if (position != null) {
            result.position = position;
        }

        if (timePoint != null) {
            result.timepoint = timePoint.asMillis();
        }

        return result;
    }

    @Override
  //READ
    public Map<CompetitorDTO, BoatDTO> getCompetitorBoats(RegattaAndRaceIdentifier raceIdentifier) {
        Map<CompetitorDTO, BoatDTO> result = null;
        TrackedRace trackedRace = getService().getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        if (trackedRace != null) {
            result = baseDomainFactory.convertToCompetitorAndBoatDTOs(trackedRace.getRace().getCompetitorsAndTheirBoats());
        }
        return result;
    }

    @Override
    //READ
    public RaceboardDataDTO getRaceboardData(String regattaName, String raceName, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, UUID eventId) {
        RaceboardDataDTO result = new RaceboardDataDTO(null, false, false, Collections.emptyList(),
                Collections.emptyList(), null, null);
        RaceWithCompetitorsAndBoatsDTO raceDTO = null;
        Regatta regatta = getService().getRegattaByName(regattaName);
        if (regatta != null) {
            RaceDefinition race = regatta.getRaceByName(raceName);
            if (race != null) {
                RegattaAndRaceIdentifier raceIdentifier = new RegattaNameAndRaceName(regatta.getName(), race.getName());
                TrackedRace trackedRace = getService().getExistingTrackedRace(raceIdentifier);
                getSecurityService().checkCurrentUserReadPermission(trackedRace);
                if (trackedRace != null) {
                    Map<CompetitorDTO, BoatDTO> competitorsAndBoats = baseDomainFactory
                            .convertToCompetitorAndBoatDTOs(race.getCompetitorsAndTheirBoats());
                    TrackedRaceDTO trackedRaceDTO = getBaseDomainFactory().createTrackedRaceDTO(trackedRace);
                    raceDTO = new RaceWithCompetitorsAndBoatsDTO(raceIdentifier, competitorsAndBoats, trackedRaceDTO,
                            getService().isRaceBeingTracked(regatta, race), trackedRace.getRankingMetric().getType());
                    if (trackedRace != null) {
                        getBaseDomainFactory().updateRaceDTOWithTrackedRaceData(trackedRace, raceDTO);
                    }
                    raceDTO.boatClass = regatta.getBoatClass() == null ? null : regatta.getBoatClass().getName();
                    SecurityDTOUtil.addSecurityInformation(getSecurityService(), raceDTO, raceDTO.getIdentifier());
                    Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
                    final LeaderboardGroup leaderboardGroup;
                    leaderboardGroup = getLeaderboardGroupByIdOrName(leaderboardGroupId, leaderboardGroupName);
                    Event event = eventId != null ? getService().getEvent(eventId) : null;
                    if (!getSecurityService().hasCurrentUserReadPermission(event)) {
                        event = null;
                    }
                    boolean isValidLeaderboardGroup = false;
                    if (leaderboardGroup != null) {
                        for (Leaderboard leaderboardInGroup : leaderboardGroup.getLeaderboards()) {
                            if (leaderboardInGroup.getName().equals(leaderboard.getName())) {
                                isValidLeaderboardGroup = true;
                                break;
                            }
                        }
                    }
                    boolean isValidEvent = event != null;
                    if (event != null && leaderboardGroup != null) {
                        isValidEvent = false;
                        for (LeaderboardGroup leaderboardGroupInEvent : event.getLeaderboardGroups()) {
                            if (leaderboardGroupInEvent.getId().equals(leaderboardGroup.getId())) {
                                isValidEvent = true;
                                break;
                            }
                        }
                    }
                    Iterable<DetailType> detailTypesForCompetitorChart = determineDetailTypesForCompetitorChart(
                            leaderboardGroupName, leaderboardGroupId, raceDTO.getRaceIdentifier());
                    Iterable<DetailType> availableDetailTypesForLeaderboard = getAvailableDetailTypesForLeaderboard(
                            leaderboardName, raceDTO.getRaceIdentifier());
                    StrippedLeaderboardDTOWithSecurity leaderboardDTO = createStrippedLeaderboardDTOWithSecurity(
                            leaderboard, false,
                            false);
                    final TrackingConnectorInfo trackingConnectorInfo = trackedRace.getTrackingConnectorInfo();
                    final TrackingConnectorInfoDTO trackingConnectorInfoDTO = trackingConnectorInfo == null ? null
                            : new TrackingConnectorInfoDTO(trackingConnectorInfo);
                    result = new RaceboardDataDTO(raceDTO, isValidLeaderboardGroup, isValidEvent,
                            detailTypesForCompetitorChart, availableDetailTypesForLeaderboard, leaderboardDTO, trackingConnectorInfoDTO);
                }
            }
        }
        return result;
    }

    /**
     * @param leaderboardGroupId
     *            if not {@code null}, this takes precedence over the {@code leaderboardGroupName} parameter which will
     *            then be ignored and will be used to look up an optional leaderboard group providing the context, e.g.,
     *            for seasonal scores from an overall leaderboard
     * @param leaderboardGroupName
     *            evaluated only if {@code leaderboardGroupId} was {@code null}; may even be {@code null} if
     *            {@code leaderboardGroupId} is {@code null} too because leaderboard group resolution is optional. If a
     *            non-{@code null} name is provided here and if {@code leaderboardGroupId} was {@code null} then the
     *            name is used to try to resolve the leaderboard group by name.
     */
    private LeaderboardGroup getLeaderboardGroupByIdOrName(UUID leaderboardGroupId, String leaderboardGroupName) {
        final LeaderboardGroup leaderboardGroup;
        if (leaderboardGroupId != null) {
            leaderboardGroup = getService().getLeaderboardGroupByID(leaderboardGroupId);
        } else if (leaderboardGroupName != null) {
            leaderboardGroup = getService().getLeaderboardGroupByName(leaderboardGroupName);
        } else {
            leaderboardGroup = null;
        }
        return leaderboardGroup;
    }

    @Override
    //READ
    public CompactRaceMapDataDTO getRaceMapData(RegattaAndRaceIdentifier raceIdentifier, Date date,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate, LegIdentifier simulationLegIdentifier,
            byte[] md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID,
            Date timeToGetTheEstimatedDurationFor, boolean estimatedDurationRequired, DetailType detailType,
            String leaderboardName, String leaderboardGroupName, UUID leaderboardGroupId) throws NoWindException {
        final HashSet<String> raceCompetitorIdsAsStrings;
        final TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);

        // if md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID is null, Arrays.equals will return false, and the
        // competitor set will be calculated and returned to the client
        if (trackedRace == null || Arrays.equals(md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID, trackedRace.getRace().getCompetitorMD5())) {
            raceCompetitorIdsAsStrings = null; // tracked race not found or still same MD5 hash, suggesting unchanged competitor set
        } else {
            raceCompetitorIdsAsStrings = new HashSet<>();
            for (final Competitor c : trackedRace.getRace().getCompetitors()) {
                raceCompetitorIdsAsStrings.add(c.getId().toString());
            }
        }
        final Duration estimatedDuration;
        if (estimatedDurationRequired) {
            estimatedDuration = getEstimationForTargetTime(timeToGetTheEstimatedDurationFor, trackedRace);
        } else {
            estimatedDuration = null;
        }
        final Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> boatPositions = getBoatPositionsInternal(raceIdentifier,
                fromPerCompetitorIdAsString, toPerCompetitorIdAsString, extrapolate, detailType, leaderboardName, leaderboardGroupName,
                leaderboardGroupId);
        final CoursePositionsDTO coursePositions = getCoursePositions(raceIdentifier, date);
        final List<SidelineDTO> courseSidelines = getCourseSidelines(raceIdentifier, date);
        final QuickRanksDTO quickRanks = getQuickRanksWithoutSecuritychecks(raceIdentifier, date);
        long simulationResultVersion = 0;
        if (simulationLegIdentifier != null) {
            SimulationService simulationService = getService().getSimulationService();
            simulationResultVersion = simulationService.getSimulationResultsVersion(simulationLegIdentifier);
        }
        return new CompactRaceMapDataDTO(boatPositions, coursePositions, courseSidelines, quickRanks,
                simulationResultVersion, raceCompetitorIdsAsStrings, estimatedDuration);
    }
    
  //READ
    private Duration getEstimationForTargetTime(Date time, final TrackedRace trackedRace) {
        Duration estimatedDuration = null;
        if (trackedRace != null) {
            try {
                estimatedDuration = trackedRace.getEstimatedTimeToComplete(new MillisecondsTimePoint(time)).getExpectedDuration();
            } catch (NotEnoughDataHasBeenAddedException | NoWindException e) {
                logger.log(Level.WARNING, "Problem computing the estimated race duration", e);
            }
        }
        return estimatedDuration;
    }

    @Override
    //READ
    public CompactBoatPositionsDTO getBoatPositions(RegattaAndRaceIdentifier raceIdentifier,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate, DetailType detailType, String leaderboardName, String leaderboardGroupName,
            UUID leaderboardGroupId) throws NoWindException {
        return new CompactBoatPositionsDTO(
                getBoatPositionsInternal(raceIdentifier, fromPerCompetitorIdAsString, toPerCompetitorIdAsString,
                        extrapolate, detailType, leaderboardName, leaderboardGroupName, leaderboardGroupId));
    }

    /**
     * {@link LegType}s are cached within the method with a resolution of one minute. The cache key is a pair of
     * {@link TrackedLegOfCompetitor} and {@link TimePoint}.
     * 
     * @param from
     *            for the list of competitors provided as keys of this map, requests the GPS fixes starting with the
     *            date provided as value
     * @param to
     *            for the list of competitors provided as keys (expected to be equal to the set of competitors used as
     *            keys in the <code>from</code> parameter, requests the GPS fixes up to but excluding (except
     *            {@code extrapolate} is {@code true}) the date provided as value
     * @param extrapolate
     *            if <code>true</code> and no (exact or interpolated) position is known for <code>to</code>, the last
     *            entry returned in the list of GPS fixes will be obtained by extrapolating from the competitors last
     *            known position at <code>to</code> and the estimated speed. With this, the {@code to} time point is no
     *            longer exclusive.
     * @param detailType
     *            if not <code>null</code> the fixes will be equipped with a value representing {@link DetailType} at
     *            their respective timestamps.
     * @param leaderboardGroupId
     *            if not {@code null}, this takes precedence over the {@code leaderboardGroupName} parameter which will
     *            then be ignored and will be used to look up an optional leaderboard group providing the context, e.g.,
     *            for seasonal scores from an overall leaderboard
     * @param leaderboardGroupName
     *            evaluated only if {@code leaderboardGroupId} was {@code null}; may even be {@code null} if
     *            {@code leaderboardGroupId} is {@code null} too because leaderboard group resolution is optional. If a
     *            non-{@code null} name is provided here and if {@code leaderboardGroupId} was {@code null} then the
     *            name is used to try to resolve the leaderboard group by name.
     * @return a map where for each competitor participating in the race the list of GPS fixes in increasing
     *         chronological order is provided. The last one is the last position at or before <code>date</code>.
     */
  //READ
    private Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> getBoatPositionsInternal(RegattaAndRaceIdentifier raceIdentifier,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate, DetailType detailType, String leaderboardName, String leaderboardGroupName, UUID leaderboardGroupId)
            throws NoWindException {
        Map<Pair<Leg, TimePoint>, LegType> legTypeCache = new HashMap<>();
        Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> result = new HashMap<>();
        final ConcurrentHashMap<TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache> cachesByTimePoint = new ConcurrentHashMap<>();
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserReadPermission(trackedRace);
            for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                if (fromPerCompetitorIdAsString.containsKey(competitor.getId().toString())) {
                    CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
                    List<GPSFixDTOWithSpeedWindTackAndLegType> fixesForCompetitor = new ArrayList<GPSFixDTOWithSpeedWindTackAndLegType>();
                    result.put(competitorDTO, fixesForCompetitor);
                    GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
                    TimePoint fromTimePoint = new MillisecondsTimePoint(fromPerCompetitorIdAsString.get(competitorDTO.getIdAsString()));
                    TimePoint toTimePointExcluding = new MillisecondsTimePoint(toPerCompetitorIdAsString.get(competitorDTO.getIdAsString()));
                    // copy the fixes into a list while holding the monitor; then release the monitor to avoid deadlocks
                    // during wind estimations required for tack determination
                    List<GPSFixMoving> fixes = new ArrayList<GPSFixMoving>();
                    track.lockForRead();
                    try {
                        Iterator<GPSFixMoving> fixIter = track.getFixesIterator(fromTimePoint, /* inclusive */true);
                        while (fixIter.hasNext()) {
                            GPSFixMoving fix = fixIter.next();
                            if (fix.getTimePoint().before(toTimePointExcluding) ||
                                    (extrapolate && fix.getTimePoint().equals(toTimePointExcluding))) {
                                logger.finest(()->""+competitor.getName()+": " + fix);
                                fixes.add(fix);
                            } else {
                                break;
                            }
                        }
                    } finally {
                        track.unlockAfterRead();
                    }
                    final Set<GPSFixMoving> extrapolatedFixes;
                    if (fixes.isEmpty()) {
                        // then there was no (smoothened) fix between fromTimePoint and toTimePointExcluding; estimate...
                        TimePoint middle = new MillisecondsTimePoint((toTimePointExcluding.asMillis()+fromTimePoint.asMillis())/2);
                        Position estimatedPosition = track.getEstimatedPosition(middle, extrapolate);
                        SpeedWithBearing estimatedSpeed = track.getEstimatedSpeed(middle);
                        if (estimatedPosition != null && estimatedSpeed != null) {
                            GPSFixMoving estimatedFix = new GPSFixMovingImpl(estimatedPosition, middle, estimatedSpeed);
                            if (logger.getLevel() != null && logger.getLevel().equals(Level.FINEST)) {
                                logger.finest(""+competitor.getName()+": " + estimatedFix+" (estimated)");
                            }
                            fixes.add(estimatedFix);
                            extrapolatedFixes = Collections.singleton(estimatedFix);
                        } else {
                            extrapolatedFixes = Collections.emptySet();
                        }
                    } else {
                        extrapolatedFixes = Collections.emptySet();
                    }
                    Iterator<GPSFixMoving> fixIter = fixes.iterator();
                    if (fixIter.hasNext()) {
                        GPSFixMoving fix = fixIter.next();
                        while (fix != null && (fix.getTimePoint().before(toTimePointExcluding) ||
                                (fix.getTimePoint().equals(toTimePointExcluding) && toTimePointExcluding.equals(fromTimePoint)))) {
                            Wind wind = trackedRace.getWind(fix.getPosition(), fix.getTimePoint());
                            final SpeedWithBearing estimatedSpeed = track.getEstimatedSpeed(fix.getTimePoint());
                            Tack tack = wind == null? null : trackedRace.getTack(estimatedSpeed, wind, fix.getTimePoint());
                            TrackedLegOfCompetitor trackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor,
                                    fix.getTimePoint());
                            LegType legType;
                            if (trackedLegOfCompetitor != null && trackedLegOfCompetitor.getLeg() != null) {
                                TimePoint quantifiedTimePoint = quantifyTimePointWithResolution(fix.getTimePoint(), /* resolutionInMilliseconds */60000);
                                Pair<Leg, TimePoint> cacheKey = new Pair<Leg, TimePoint>(trackedLegOfCompetitor.getLeg(), quantifiedTimePoint);
                                legType = legTypeCache.get(cacheKey);
                                if (legType == null) {
                                    try {
                                        legType = trackedRace.getTrackedLeg(trackedLegOfCompetitor.getLeg()).getLegType(fix.getTimePoint());
                                        legTypeCache.put(cacheKey, legType);
                                    } catch (NoWindException nwe) {
                                        // without wind, leave the leg type null, meaning "unknown"
                                        legType = null;
                                    }
                                }
                            } else {
                                legType = null;
                            }
                            WindDTO windDTO = wind == null ? null : createWindDTOFromAlreadyAveraged(wind, toTimePointExcluding);
                            Double detailValue = null;
                            if (detailType != null) {
                                MillisecondsTimePoint time = new MillisecondsTimePoint(fix.getTimePoint().asMillis());
                                WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = cachesByTimePoint.get(time);
                                if (cache == null) {
                                    cache = new LeaderboardDTOCalculationReuseCache(time);
                                    cachesByTimePoint.put(time, cache);
                                }
                                try {
                                    detailValue = getCompetitorRaceDataEntry(detailType, trackedRace, competitor,
                                            fix.getTimePoint(), leaderboardGroupName, leaderboardGroupId, leaderboardName, cache);
                                } catch (NoWindException nwe) {
                                    detailValue = null;
                                }
                            }
                            GPSFixDTOWithSpeedWindTackAndLegType fixDTO = createGPSFixDTO(fix, estimatedSpeed, windDTO, tack, legType, /* extrapolate */ extrapolatedFixes.contains(fix), detailValue);
                            fixesForCompetitor.add(fixDTO);
                            if (fixIter.hasNext()) {
                                fix = fixIter.next();
                            } else {
                                // check if fix was at date and if extrapolation is requested; 
                                if (!fix.getTimePoint().equals(toTimePointExcluding) && extrapolate) {
                                    Position position = track.getEstimatedPosition(toTimePointExcluding, extrapolate);
                                    Wind wind2 = trackedRace.getWind(position, toTimePointExcluding);
                                    SpeedWithBearing estimatedSpeed2 = track.getEstimatedSpeed(toTimePointExcluding);
                                    Tack tack2 = wind2 == null ? null : trackedRace.getTack(estimatedSpeed2, wind2, toTimePointExcluding);
                                    LegType legType2;
                                    if (trackedLegOfCompetitor != null && trackedLegOfCompetitor.getLeg() != null) {
                                        TimePoint quantifiedTimePoint = quantifyTimePointWithResolution(
                                                fix.getTimePoint(), /* resolutionInMilliseconds */
                                                60000);
                                        Pair<Leg, TimePoint> cacheKey = new Pair<Leg, TimePoint>(
                                                trackedLegOfCompetitor.getLeg(), quantifiedTimePoint);
                                        legType2 = legTypeCache.get(cacheKey);
                                        if (legType2 == null) {
                                            try {
                                                legType2 = trackedRace.getTrackedLeg(trackedLegOfCompetitor.getLeg()).getLegType(fix.getTimePoint());
                                                legTypeCache.put(cacheKey, legType2);
                                            } catch (NoWindException nwe) {
                                                // no wind information; leave leg type null, meaning "unknown"
                                                legType2 = null;
                                            }
                                        }
                                    } else {
                                        legType2 = null;
                                    }
                                    WindDTO windDTO2 = wind2 == null ? null : createWindDTOFromAlreadyAveraged(wind2, toTimePointExcluding);
                                    GPSFixDTOWithSpeedWindTackAndLegType extrapolated = new GPSFixDTOWithSpeedWindTackAndLegType(
                                            toPerCompetitorIdAsString.get(competitorDTO.getIdAsString()),
                                            position==null?null:position,
                                                    estimatedSpeed2==null?null:createSpeedWithBearingDTO(estimatedSpeed2), windDTO2,
                                                            tack2, legType2, /* extrapolated */ true);
                                    fixesForCompetitor.add(extrapolated);
                                }
                                fix = null;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

  //READ
    private TimePoint quantifyTimePointWithResolution(TimePoint timePoint, long resolutionInMilliseconds) {
        return new MillisecondsTimePoint(timePoint.asMillis() / resolutionInMilliseconds * resolutionInMilliseconds);
    }

  //READ
    private SpeedWithBearingDTO createSpeedWithBearingDTO(SpeedWithBearing speedWithBearing) {
        return new SpeedWithBearingDTO(speedWithBearing.getKnots(), speedWithBearing
                .getBearing().getDegrees());
    }

  //READ
    private GPSFixDTOWithSpeedWindTackAndLegType createGPSFixDTO(GPSFix fix, SpeedWithBearing speedWithBearing, WindDTO windDTO, Tack tack, LegType legType, boolean extrapolated, Double detailValue) {
        return new GPSFixDTOWithSpeedWindTackAndLegType(fix.getTimePoint().asDate(), fix.getPosition()==null?null:fix.getPosition(),
                speedWithBearing==null?null:createSpeedWithBearingDTO(speedWithBearing), windDTO, tack, legType, extrapolated, detailValue);
    }

    @Override
  //READ
    public RaceTimesInfoDTO getRaceTimesInfo(RegattaAndRaceIdentifier raceIdentifier) {
        RaceTimesInfoDTO raceTimesInfo = null;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);

        if (trackedRace != null) {
            getSecurityService().checkCurrentUserReadPermission(trackedRace);
            raceTimesInfo = new RaceTimesInfoDTO(raceIdentifier);
            List<LegInfoDTO> legInfos = new ArrayList<LegInfoDTO>();
            raceTimesInfo.setLegInfos(legInfos);
            List<MarkPassingTimesDTO> markPassingTimesDTOs = new ArrayList<MarkPassingTimesDTO>();
            raceTimesInfo.setMarkPassingTimes(markPassingTimesDTOs);

            raceTimesInfo.startOfRace = trackedRace.getStartOfRace() == null ? null : trackedRace.getStartOfRace().asDate();
            raceTimesInfo.startOfTracking = trackedRace.getStartOfTracking() == null ? null : trackedRace.getStartOfTracking().asDate();
            raceTimesInfo.newestTrackingEvent = trackedRace.getTimePointOfNewestEvent() == null ? null : trackedRace.getTimePointOfNewestEvent().asDate();
            raceTimesInfo.endOfTracking = trackedRace.getEndOfTracking() == null ? null : trackedRace.getEndOfTracking().asDate();
            raceTimesInfo.endOfRace = trackedRace.getEndOfRace() == null ? null : trackedRace.getEndOfRace().asDate();
            raceTimesInfo.raceFinishingTime = trackedRace.getFinishingTime() == null ? null : trackedRace.getFinishingTime().asDate();
            raceTimesInfo.raceFinishedTime = trackedRace.getFinishedTime() == null ? null : trackedRace.getFinishedTime().asDate();
            raceTimesInfo.delayToLiveInMs = trackedRace.getDelayToLiveInMillis();

            Iterable<com.sap.sse.common.Util.Pair<Waypoint, com.sap.sse.common.Util.Pair<TimePoint, TimePoint>>> markPassingsTimes = trackedRace.getMarkPassingsTimes();
            synchronized (markPassingsTimes) {
                int numberOfWaypoints = Util.size(markPassingsTimes);
                int wayPointNumber = 1;
                for (com.sap.sse.common.Util.Pair<Waypoint, com.sap.sse.common.Util.Pair<TimePoint, TimePoint>> markPassingTimes : markPassingsTimes) {
                    MarkPassingTimesDTO markPassingTimesDTO = new MarkPassingTimesDTO();
                    String name = "M" + (wayPointNumber - 1);
                    if (wayPointNumber == numberOfWaypoints) {
                        name = "F";
                    }
                    markPassingTimesDTO.setName(name);
                    com.sap.sse.common.Util.Pair<TimePoint, TimePoint> timesPair = markPassingTimes.getB();
                    TimePoint firstPassingTime = timesPair.getA();
                    TimePoint lastPassingTime = timesPair.getB();
                    markPassingTimesDTO.firstPassingDate = firstPassingTime == null ? null : firstPassingTime.asDate();
                    markPassingTimesDTO.lastPassingDate = lastPassingTime == null ? null : lastPassingTime.asDate();
                    markPassingTimesDTOs.add(markPassingTimesDTO);
                    wayPointNumber++;
                }
            }
            trackedRace.getRace().getCourse().lockForRead();
            try {
                Iterable<TrackedLeg> trackedLegs = trackedRace.getTrackedLegs();
                int legNumber = 1;
                for (TrackedLeg trackedLeg : trackedLegs) {
                    LegInfoDTO legInfoDTO = new LegInfoDTO(legNumber);
                    legInfoDTO.setName("L" + legNumber);
                    try {
                        MarkPassingTimesDTO markPassingTimesDTO = markPassingTimesDTOs.get(legNumber - 1);
                        if (markPassingTimesDTO.firstPassingDate != null) {
                            TimePoint p = new MillisecondsTimePoint(markPassingTimesDTO.firstPassingDate);
                            legInfoDTO.legType = trackedLeg.getLegType(p);
                            legInfoDTO.legBearingInDegrees = trackedLeg.getLegBearing(p).getDegrees();
                        }
                    } catch (NoWindException e) {
                        // do nothing
                    }
                    legInfos.add(legInfoDTO);
                    legNumber++;
                }
            } finally {
                trackedRace.getRace().getCourse().unlockAfterRead();
            }
        }   
        if (raceTimesInfo != null) {
            raceTimesInfo.currentServerTime = new Date();
        }
        return raceTimesInfo;
    }

    @Override
  //READ
    public List<RaceTimesInfoDTO> getRaceTimesInfos(Collection<RegattaAndRaceIdentifier> raceIdentifiers) {
        List<RaceTimesInfoDTO> raceTimesInfos = new ArrayList<RaceTimesInfoDTO>();
        for (RegattaAndRaceIdentifier raceIdentifier : raceIdentifiers) {
            TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
            if (trackedRace == null || getSecurityService().hasCurrentUserReadPermission(trackedRace)) {
                RaceTimesInfoDTO raceTimesInfo = getRaceTimesInfo(raceIdentifier);
                if (raceTimesInfo != null) {
                    raceTimesInfos.add(raceTimesInfo);
                }
            }
        }
        return raceTimesInfos;
    }

  //READ
    private List<SidelineDTO> getCourseSidelines(RegattaAndRaceIdentifier raceIdentifier, Date date) {
        List<SidelineDTO> result = new ArrayList<SidelineDTO>();
        final TimePoint dateAsTimePoint;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            if (date == null) {
                dateAsTimePoint = MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis());
            } else {
                dateAsTimePoint = new MillisecondsTimePoint(date);
            }
            for (Sideline sideline : trackedRace.getCourseSidelines()) {
                List<MarkDTO> markDTOs = new ArrayList<MarkDTO>();
                for (Mark mark : sideline.getMarks()) {
                    GPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(mark);
                    Position positionAtDate = track.getEstimatedPosition(dateAsTimePoint, /* extrapolate */false);
                    if (positionAtDate != null) {
                        markDTOs.add(convertToMarkDTO(mark, positionAtDate));
                    }
                }
                result.add(new SidelineDTO(sideline.getName(), markDTOs));
            }
        }
        return result;
    }
        
    @Override
  //READ
    public CoursePositionsDTO getCoursePositions(RegattaAndRaceIdentifier raceIdentifier, Date date) {
        CoursePositionsDTO result = new CoursePositionsDTO();
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserReadPermission(trackedRace);
            final TimePoint dateAsTimePoint;
            if (date == null) {
                dateAsTimePoint = MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis());
            } else {
                dateAsTimePoint = new MillisecondsTimePoint(date);
            }
            result.totalLegsCount = trackedRace.getRace().getCourse().getLegs().size();
            result.currentLegNumber = trackedRace.getLastLegStarted(dateAsTimePoint);
            result.marks = new HashSet<MarkDTO>();
            result.course = convertToRaceCourseDTO(trackedRace.getRace().getCourse(), new TrackedRaceMarkPositionFinder(trackedRace), dateAsTimePoint);
            // now make sure we don't duplicate the MarkDTO objects but instead use the ones from the RaceCourseDTO
            // object and amend them with the Position
            result.waypointPositions = new ArrayList<>();
            Set<Mark> marks = new HashSet<Mark>();
            Course course = trackedRace.getRace().getCourse();
            for (Waypoint waypoint : course.getWaypoints()) {
                Position waypointPosition = trackedRace.getApproximatePosition(waypoint, dateAsTimePoint);
                if (waypointPosition != null) {
                    result.waypointPositions.add(waypointPosition);
                }
                for (Mark b : waypoint.getMarks()) {
                    marks.add(b);
                }
            }
            for (final WaypointDTO waypointDTO : result.course.waypoints) {
                for (final MarkDTO markDTO : waypointDTO.controlPoint.getMarks()) {
                    if (markDTO.position != null) {
                        result.marks.add(markDTO);
                    }
                }
            }

            // set the positions of start and finish
            Waypoint firstWaypoint = course.getFirstWaypoint();
            if (firstWaypoint != null && Util.size(firstWaypoint.getMarks()) == 2) {
                final LineDetails markPositionDTOsAndLineAdvantage = trackedRace.getStartLine(dateAsTimePoint);
                if (markPositionDTOsAndLineAdvantage != null) {
                    result.startLineLengthInMeters = markPositionDTOsAndLineAdvantage.getLength().getMeters();
                    Bearing angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind = markPositionDTOsAndLineAdvantage
                            .getAngleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind();
                    result.startLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind = angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind == null ? null
                            : angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind.getDegrees();
                    result.startLineAdvantageousSide = markPositionDTOsAndLineAdvantage
                            .getAdvantageousSideWhileApproachingLine();
                    Distance advantage = markPositionDTOsAndLineAdvantage.getAdvantage();
                    result.startLineAdvantageInMeters = advantage == null ? null : advantage.getMeters();
                }
            }
            Waypoint lastWaypoint = course.getLastWaypoint();
            if (lastWaypoint != null && Util.size(lastWaypoint.getMarks()) == 2) {
                final LineDetails markPositionDTOsAndLineAdvantage = trackedRace.getFinishLine(dateAsTimePoint);
                if (markPositionDTOsAndLineAdvantage != null) {
                    result.finishLineLengthInMeters = markPositionDTOsAndLineAdvantage.getLength().getMeters();
                    Bearing angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind = markPositionDTOsAndLineAdvantage
                            .getAngleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind();
                    result.finishLineAngleFromPortToStarboardWhenApproachingLineToCombinedWind = angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind == null ? null
                            : angleDifferenceFromPortToStarboardWhenApproachingLineToTrueWind.getDegrees();
                    result.finishLineAdvantageousSide = markPositionDTOsAndLineAdvantage
                            .getAdvantageousSideWhileApproachingLine();
                    Distance advantage = markPositionDTOsAndLineAdvantage.getAdvantage();
                    result.finishLineAdvantageInMeters = advantage == null ? null : advantage.getMeters();
                }
            }
        }
        return result;
    }
      
    @Override
  //READ
    public RaceCourseDTO getRaceCourse(RegattaAndRaceIdentifier raceIdentifier, Date date) {
        List<WaypointDTO> waypointDTOs = new ArrayList<WaypointDTO>();
        Map<Serializable, ControlPointDTO> controlPointCache = new HashMap<>();
        TimePoint dateAsTimePoint = new MillisecondsTimePoint(date);
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        List<MarkDTO> allMarks = new ArrayList<>();
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserReadPermission(trackedRace);
            for (Mark mark : trackedRace.getMarks()) {
                Position pos = trackedRace.getOrCreateTrack(mark).getEstimatedPosition(dateAsTimePoint, false);
                allMarks.add(convertToMarkDTO(mark, pos));
            }
            Course course = trackedRace.getRace().getCourse();
            for (Waypoint waypoint : course.getWaypoints()) {
                ControlPointDTO controlPointDTO = controlPointCache.get(waypoint.getControlPoint().getId());
                if (controlPointDTO == null) {
                    controlPointDTO = convertToControlPointDTO(waypoint.getControlPoint(), new TrackedRaceMarkPositionFinder(trackedRace), dateAsTimePoint);
                    controlPointCache.put(waypoint.getControlPoint().getId(), controlPointDTO);
                }
                WaypointDTO waypointDTO = new WaypointDTO(waypoint.getName(), controlPointDTO,
                        waypoint.getPassingInstructions());
                waypointDTOs.add(waypointDTO);
            }
        }
        return new RaceCourseDTO(waypointDTOs, allMarks);
    }
    
    class TrackedRaceMarkPositionFinder implements MarkPositionFinder{
        private TrackedRace trackedRace;

        public TrackedRaceMarkPositionFinder(TrackedRace trackedRace) {
            this.trackedRace = trackedRace;
        }
        
        @Override
        public Position find(Mark mark, TimePoint at) {
            final TimePoint timePointToUse = trackedRace == null ? null :
                at == null ? MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis()) : at;
            final Position result;
            if (timePointToUse == null) {
                result = null;
            } else {
                result = trackedRace.getOrCreateTrack(mark).getEstimatedPosition(timePointToUse, /* extrapolate */ false);
            }
            return result;
        }
    }
    
    interface MarkPositionFinder {
        Position find(Mark mark, TimePoint at);
    }
    
    //READ
    private ControlPointDTO convertToControlPointDTO(ControlPoint controlPoint, MarkPositionFinder positionFinder, TimePoint timePoint) {
        ControlPointDTO result;
        
        if (controlPoint instanceof ControlPointWithTwoMarks) {
            final Mark left = ((ControlPointWithTwoMarks) controlPoint).getLeft();
            final Position leftPos =  positionFinder.find(left, timePoint);
            final Mark right = ((ControlPointWithTwoMarks) controlPoint).getRight();
            final Position rightPos = positionFinder.find(right, timePoint);
            result = new GateDTO(controlPoint.getId().toString(), controlPoint.getName(),
                    convertToMarkDTO(left, leftPos), convertToMarkDTO(right, rightPos), controlPoint.getShortName());
        } else {
            Mark mark = controlPoint.getMarks().iterator().next();
            final Position position = positionFinder.find(mark, timePoint);
            result = convertToMarkDTO(mark, position);
        }
        return result;
    }
    
  //READ
    protected ControlPoint getOrCreateControlPoint(ControlPointDTO dto) {
        String idAsString = dto.getIdAsString();
        if (idAsString == null) {
            idAsString = UUID.randomUUID().toString();
        }
        if (dto instanceof GateDTO) {
            GateDTO gateDTO = (GateDTO) dto;
            Mark left = (Mark) getOrCreateControlPoint(gateDTO.getLeft());
            Mark right = (Mark) getOrCreateControlPoint(gateDTO.getRight());
            return baseDomainFactory.getOrCreateControlPointWithTwoMarks(idAsString, gateDTO.getName(), left, right,
                    gateDTO.getShortName());
        } else {
            MarkDTO markDTO = (MarkDTO) dto;
            return baseDomainFactory.getOrCreateMark(idAsString, dto.getName(), markDTO.getShortName(), markDTO.type, markDTO.color, markDTO.shape, markDTO.pattern);
        }
    }

    /**
     * @param timePoint
     *            <code>null</code> means "live" and is then replaced by "now" minus the tracked race's
     *            {@link TrackedRace#getDelayToLiveInMillis() delay}.
     */
    //READ
    public QuickRanksDTO computeQuickRanks(RegattaAndRaceIdentifier raceIdentifier, TimePoint timePoint)
            throws NoWindException {
        final List<QuickRankDTO> result = new ArrayList<>();
        TrackedRace trackedRace = getService().getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            final TimePoint actualTimePoint;
            if (timePoint == null) {
                actualTimePoint = MillisecondsTimePoint.now().minus(trackedRace.getDelayToLiveInMillis());
            } else {
                actualTimePoint = timePoint;
            }
            final RaceDefinition race = trackedRace.getRace();
            int oneBasedRank = 1;
            final List<Competitor> competitorsFromBestToWorst = trackedRace.getCompetitorsFromBestToWorst(actualTimePoint);
            for (Competitor competitor : competitorsFromBestToWorst) {
                TrackedLegOfCompetitor trackedLeg = trackedRace.getTrackedLeg(competitor, actualTimePoint);
                if (trackedLeg != null) {
                    int legNumberOneBased = race.getCourse().getLegs().indexOf(trackedLeg.getLeg()) + 1;
                    Boat boatOfCompetitor = trackedRace.getBoatOfCompetitor(competitor);
                    QuickRankDTO quickRankDTO = new QuickRankDTO(
                            baseDomainFactory.convertToCompetitorAndBoatDTO(competitor, boatOfCompetitor).getCompetitor(),
                            oneBasedRank, legNumberOneBased);
                    result.add(quickRankDTO);
                }
                oneBasedRank++;
            }
        }
        return new QuickRanksDTO(result);
    }

    //READ
    private QuickRanksDTO getQuickRanksWithoutSecuritychecks(RegattaAndRaceIdentifier raceIdentifier, Date date) throws NoWindException {
        final QuickRanksDTO result;
        if (date == null) {
            result = quickRanksLiveCache.get(raceIdentifier);
        } else {
            result = computeQuickRanks(raceIdentifier, new MillisecondsTimePoint(date));
        }
        return result;
    }

    @Override
    //READ
    public WindInfoForRaceDTO getWindSourcesInfo(RegattaAndRaceIdentifier raceIdentifier) {
        getSecurityService().checkCurrentUserReadPermission(raceIdentifier);
        WindInfoForRaceDTO result = null;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            result = new WindInfoForRaceDTO();
            result.raceIsKnownToStartUpwind = trackedRace.raceIsKnownToStartUpwind();
            List<WindSource> windSourcesToExclude = new ArrayList<WindSource>();
            for (WindSource windSourceToExclude : trackedRace.getWindSourcesToExclude()) {
                windSourcesToExclude.add(windSourceToExclude);
            }
            result.windSourcesToExclude = windSourcesToExclude;
            Map<WindSource, WindTrackInfoDTO> windTrackInfoDTOs = new HashMap<WindSource, WindTrackInfoDTO>();
            result.windTrackInfoByWindSource = windTrackInfoDTOs;

            for (WindSource windSource: trackedRace.getWindSources()) {
                windTrackInfoDTOs.put(windSource, new WindTrackInfoDTO());
            }
            windTrackInfoDTOs.put(new WindSourceImpl(WindSourceType.COMBINED), new WindTrackInfoDTO());
        }
        return result;
    }

    //??
    protected RacingEventService getService() {
        try {
            return racingEventServiceTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } // grab the service
    }

  //??
    protected SharedSailingData getSharedSailingData() {
        try {
            return sharedSailingDataTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } // grab the service
    }

  //??
    protected ReplicationService getReplicationService() {
        try {
            return replicationServiceTracker.waitForService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
  //??
    protected SecurityService getSecurityService() {
        try {
            return securityServiceTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    //READ
    public List<String> getLeaderboardNames() {
        return getLeaderboardNamesFilteredForCurrentUser();
    }

    //READ
    private ArrayList<String> getLeaderboardNamesFilteredForCurrentUser() {
        final ArrayList<String> result = new ArrayList<>();
        getSecurityService().filterObjectsWithPermissionForCurrentUser(
                DefaultActions.READ, getService().getLeaderboards().values(), l -> result.add(l.getName()));
        return result;
    }

    @Override
  //READ
    public List<StrippedLeaderboardDTOWithSecurity> getLeaderboardsWithSecurity() {
        final Map<String, Leaderboard> leaderboards = getService().getLeaderboards();
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                leaderboards.values(),
                leaderboard -> createStrippedLeaderboardDTOWithSecurity(leaderboard, false, false));
    }

    @Override
    //READ
    public StrippedLeaderboardDTOWithSecurity getLeaderboardWithSecurity(String leaderboardName) {
        Map<String, Leaderboard> leaderboards = getService().getLeaderboards();
        StrippedLeaderboardDTOWithSecurity result = null;
        Leaderboard leaderboard = leaderboards.get(leaderboardName);
        if (leaderboard != null) {
            if (leaderboard instanceof RegattaLeaderboard) {
                getSecurityService().checkCurrentUserReadPermission(((RegattaLeaderboard) leaderboard).getRegatta());
            }
            getSecurityService().checkCurrentUserReadPermission(leaderboard);
            result = createStrippedLeaderboardDTOWithSecurity(leaderboard, false, false);
        }
        return result;
    }

    @Override
    //READ
    public StrippedLeaderboardDTO getLeaderboard(String leaderboardName) {
        Map<String, Leaderboard> leaderboards = getService().getLeaderboards();
        StrippedLeaderboardDTO result = null;
        Leaderboard leaderboard = leaderboards.get(leaderboardName);
        if (leaderboard != null) {
            if (leaderboard instanceof RegattaLeaderboard) {
                getSecurityService().checkCurrentUserReadPermission(((RegattaLeaderboard) leaderboard).getRegatta());
            }
            getSecurityService().checkCurrentUserReadPermission(leaderboard);
            result = createStrippedLeaderboardDTO(leaderboard, false, false);
        }
        return result;
    }

    /**
     * Creates a {@link LeaderboardDTO} for <code>leaderboard</code> and fills in the name, race master data
     * in the form of {@link RaceColumnDTO}s, whether or not there are {@link LeaderboardDTO#hasCarriedPoints carried points}
     * and the {@link LeaderboardDTO#discardThresholds discarding thresholds} for the leaderboard. No data about the points
     * is filled into the result object. No data about the competitor display names is filled in; instead, an empty map
     * is used for {@link LeaderboardDTO#competitorDisplayNames}.<br />
     * If <code>withGeoLocationData</code> is <code>true</code> the geographical location of all races will be determined.
     */
    //READ
    private StrippedLeaderboardDTO createStrippedLeaderboardDTO(Leaderboard leaderboard, boolean withGeoLocationData, boolean withStatisticalData) {
        StrippedLeaderboardDTO leaderboardDTO = new StrippedLeaderboardDTO(convertToBoatClassDTO(leaderboard.getBoatClass()));
        fillLeaderboardData(leaderboard, withGeoLocationData, withStatisticalData, leaderboardDTO);
        return leaderboardDTO;
    }

    /**
     * Creates a {@link LeaderboardDTO} for <code>leaderboard</code> and fills in the name, race master data in the form
     * of {@link RaceColumnDTO}s, whether or not there are {@link LeaderboardDTO#hasCarriedPoints carried points} and
     * the {@link LeaderboardDTO#discardThresholds discarding thresholds} for the leaderboard. No data about the points
     * is filled into the result object. No data about the competitor display names is filled in; instead, an empty map
     * is used for {@link LeaderboardDTO#competitorDisplayNames}.<br />
     * If <code>withGeoLocationData</code> is <code>true</code> the geographical location of all races will be
     * determined.
     */
    //READ
    protected StrippedLeaderboardDTOWithSecurity createStrippedLeaderboardDTOWithSecurity(Leaderboard leaderboard,
            boolean withGeoLocationData, boolean withStatisticalData) {
        StrippedLeaderboardDTOWithSecurity leaderboardDTO = new StrippedLeaderboardDTOWithSecurity(
                convertToBoatClassDTO(leaderboard.getBoatClass()));
        fillLeaderboardData(leaderboard, withGeoLocationData, withStatisticalData, leaderboardDTO);
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), leaderboardDTO, leaderboard.getIdentifier());
        return leaderboardDTO;
    }

    //READ
    private void fillLeaderboardData(Leaderboard leaderboard, boolean withGeoLocationData, boolean withStatisticalData,
            StrippedLeaderboardDTO leaderboardDTO) {
        TimePoint startOfLatestRace = null;
        Long delayToLiveInMillisForLatestRace = null;
        leaderboardDTO.setName(leaderboard.getName());
        leaderboardDTO.displayName = leaderboard.getDisplayName();
        leaderboardDTO.competitorDisplayNames = new HashMap<>();
        leaderboardDTO.competitorsCount = Util.size(leaderboard.getCompetitors());
        leaderboardDTO.boatClassName = leaderboard.getBoatClass()==null?null:leaderboard.getBoatClass().getName();
        leaderboardDTO.type = leaderboard.getLeaderboardType();
        if (leaderboard instanceof RegattaLeaderboard) {
            RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
            Regatta regatta = regattaLeaderboard.getRegatta();
            leaderboardDTO.regattaName = regatta.getName(); 
            leaderboardDTO.scoringScheme = regatta.getScoringScheme().getType();
            leaderboardDTO.canBoatsOfCompetitorsChangePerRace = regatta.canBoatsOfCompetitorsChangePerRace();
        } else {
            leaderboardDTO.scoringScheme = leaderboard.getScoringScheme().getType();
            leaderboardDTO.canBoatsOfCompetitorsChangePerRace = false;
        }
        leaderboardDTO.courseAreas = new ArrayList<>();
        Util.addAll(Util.map(leaderboard.getCourseAreas(), this::convertToCourseAreaDTO), leaderboardDTO.courseAreas);
        leaderboardDTO.setDelayToLiveInMillisForLatestRace(delayToLiveInMillisForLatestRace);
        leaderboardDTO.hasCarriedPoints = leaderboard.hasCarriedPoints();
        if (leaderboard.getResultDiscardingRule() instanceof ThresholdBasedResultDiscardingRule) {
            leaderboardDTO.discardThresholds = ((ThresholdBasedResultDiscardingRule) leaderboard.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces();
        } else {
            leaderboardDTO.discardThresholds = null;
        }
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                RaceDTO raceDTO = null;
                RegattaAndRaceIdentifier raceIdentifier = null;
                TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                if (trackedRace != null) {
                    if (startOfLatestRace == null || (trackedRace.getStartOfRace() != null && trackedRace.getStartOfRace().compareTo(startOfLatestRace) > 0)) {
                        delayToLiveInMillisForLatestRace = trackedRace.getDelayToLiveInMillis();
                    }
                    raceIdentifier = new RegattaNameAndRaceName(trackedRace.getTrackedRegatta().getRegatta().getName(), trackedRace.getRace().getName());
                    raceDTO = baseDomainFactory.createRaceDTO(getService(), withGeoLocationData, raceIdentifier, trackedRace);
                    if(withStatisticalData) {
                        Iterable<MediaTrack> mediaTracksForRace = getService().getMediaTracksForRace(raceIdentifier);
                        raceDTO.trackedRaceStatistics = baseDomainFactory.createTrackedRaceStatisticsDTO(trackedRace, leaderboard, raceColumn, fleet, mediaTracksForRace); 
                    }
                }    
                final FleetDTO fleetDTO = baseDomainFactory.convertToFleetDTO(fleet);
                RaceColumnDTO raceColumnDTO = leaderboardDTO.addRace(raceColumn.getName(),
                        raceColumn.getExplicitFactor(), leaderboard.getScoringScheme().getScoreFactor(raceColumn),
                        raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getRegatta().getName() : null,
                        raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getSeries().getName() : null,
                        fleetDTO, raceColumn.isMedalRace(), raceIdentifier, raceDTO, raceColumn instanceof MetaLeaderboardColumn);
                final RaceLog raceLog = raceColumn.getRaceLog(fleet);
                final RaceLogTrackingState raceLogTrackingState = raceLog == null ? RaceLogTrackingState.NOT_A_RACELOG_TRACKED_RACE :
                    new RaceLogTrackingStateAnalyzer(raceLog).analyze();
                final boolean raceLogTrackerExists = raceLog == null ? false : getService().getRaceTrackerById(raceLog.getId()) != null;
                final boolean competitorRegistrationsExist = raceLog == null ? false : !Util.isEmpty(raceColumn.getAllCompetitorsAndTheirBoats(fleet).keySet());
                final boolean courseExist = raceLog == null ? false : !Util.isEmpty(raceColumn.getCourseMarks(fleet));
                final RaceLogTrackingInfoDTO raceLogTrackingInfo = new RaceLogTrackingInfoDTO(raceLogTrackerExists,
                        competitorRegistrationsExist, courseExist, raceLogTrackingState);
                raceColumnDTO.setRaceLogTrackingInfo(fleetDTO, raceLogTrackingInfo);
            }
        }
    }

    @Override
    //READ
    public Map<String, RegattaAndRaceIdentifier> getRegattaAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(String leaderboardName, String raceColumnName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        Map<String, RegattaAndRaceIdentifier> result = new HashMap<String, RegattaAndRaceIdentifier>();
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                for (Fleet fleet : raceColumn.getFleets()) {
                    TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                    if (trackedRace != null) {
                        result.put(fleet.getName(), trackedRace.getRaceIdentifier());
                    } else {
                        result.put(fleet.getName(), null);
                    }
                }
            }
        }
        return result;
    }

    @Override
  //READ
    public List<SwissTimingConfigurationWithSecurityDTO> getPreviousSwissTimingConfigurations() {
        Iterable<SwissTimingConfiguration> configs = swissTimingAdapterPersistence.getSwissTimingConfigurations();
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(configs,
                stConfig -> {
                    final SwissTimingConfigurationWithSecurityDTO config = new SwissTimingConfigurationWithSecurityDTO(
                            stConfig.getName(), stConfig.getJsonURL(),
                        stConfig.getHostname(), stConfig.getPort(), stConfig.getUpdateURL(),
                            stConfig.getUpdateUsername(), stConfig.getUpdatePassword(), stConfig.getCreatorName());
                    SecurityDTOUtil.addSecurityInformation(getSecurityService(), config, config.getIdentifier());
                    return config;
                });
    }

    @Override
    //READ
    public SwissTimingEventRecordDTO getRacesOfSwissTimingEvent(String eventJsonURL) 
            throws UnknownHostException, IOException, InterruptedException, ParseException {
        SwissTimingEventRecordDTO result = null; 
        List<SwissTimingRaceRecordDTO> swissTimingRaces = new ArrayList<SwissTimingRaceRecordDTO>();
        
        // TODO: delete getSwissTimingAdapter().getSwissTimingRaceRecords() method
        // TODO: delete SwissTimingDomainFactory.getRaceTypeFromRaceID(String raceID)
        URL url = new URL(eventJsonURL);
        URLConnection eventResultConn = HttpUrlConnectionHelper.redirectConnection(url);
        Manage2SailEventResultsParserImpl parser = new Manage2SailEventResultsParserImpl();
        EventResultDescriptor eventResult = parser.getEventResult((InputStream) eventResultConn.getContent());
        if (eventResult != null) {
            for (RegattaResultDescriptor regattaResult : eventResult.getRegattaResults()) {
                for (RaceResultDescriptor race : regattaResult.getRaceResults()) {
                    // add only the  tracked races
                    if (race.isTracked() != null && race.isTracked() == true) {
                        SwissTimingRaceRecordDTO swissTimingRaceRecordDTO = new SwissTimingRaceRecordDTO(race.getId(), race.getName(), 
                                regattaResult.getName(), race.getSeriesName(), race.getFleetName(), race.getStatus(), race.getStartTime(),
                                regattaResult.getXrrEntriesUrl() != null ? regattaResult.getXrrEntriesUrl().toExternalForm() : null, hasRememberedRegatta(race.getId()));
                        swissTimingRaceRecordDTO.boatClass = regattaResult.getIsafId() != null && !regattaResult.getIsafId().isEmpty() ? regattaResult.getIsafId() : regattaResult.getClassName();
                        swissTimingRaceRecordDTO.gender = regattaResult.getCompetitorGenderType().name();
                        swissTimingRaces.add(swissTimingRaceRecordDTO);
                    }
                }
            }
            result = new SwissTimingEventRecordDTO(eventResult.getId(), eventResult.getName(), eventResult.getTrackingDataHost(),
                    eventResult.getTrackingDataPort(), swissTimingRaces);
        }
        return result;
    }
    
    //??
    protected RaceLogStore getRaceLogStore() {
        return MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory,
                domainObjectFactory);
    }
    
    //??
    protected RegattaLogStore getRegattaLogStore() {
        return MongoRegattaLogStoreFactory.INSTANCE.getMongoRegattaLogStore(
                mongoObjectFactory, domainObjectFactory);
    }

    //??
    protected SwissTimingReplayService getSwissTimingReplayService() {
        return swissTimingReplayService;
    }

    @Override
    //READ
    public List<SwissTimingReplayRaceDTO> listSwissTiminigReplayRaces(String swissTimingUrl) {
        List<SwissTimingReplayRace> replayRaces = getSwissTimingReplayService().listReplayRaces(swissTimingUrl);
        List<SwissTimingReplayRaceDTO> result = new ArrayList<SwissTimingReplayRaceDTO>(replayRaces.size()); 
        for (SwissTimingReplayRace replayRace : replayRaces) {
            result.add(new SwissTimingReplayRaceDTO(replayRace.getFlightNumber(), replayRace.getRaceId(),
                    replayRace.getRsc(), replayRace.getName(), replayRace.getBoatClass(), replayRace.getStartTime(),
                    replayRace.getLink(), hasRememberedRegatta(replayRace.getRaceId()), swissTimingUrl));
        }
        return result;
    }

    @Override
    //READ
    public void replaySwissTimingRace(RegattaIdentifier regattaIdentifier, Iterable<SwissTimingReplayRaceDTO> replayRaceDTOs,
            boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm) {
        logger.info("replaySwissTimingRace for regatta "+regattaIdentifier+" for races "+replayRaceDTOs);
        for (SwissTimingReplayRaceDTO replayRaceDTO : replayRaceDTOs) {
            try {
                String boatClassName;
                if (regattaIdentifier == null) {
                    boatClassName = replayRaceDTO.boat_class;
                    for (String genderIndicator : new String[] { "Man", "Woman", "Men", "Women", "M", "W" }) {
                        Pattern p = Pattern.compile("(( - )|-| )" + genderIndicator + "$");
                        Matcher m = p.matcher(boatClassName.trim());
                        if (m.find()) {
                            boatClassName = boatClassName.trim().substring(0, m.start(1));
                            break;
                        }
                    }
                } else {
                    boatClassName = null;
                }
                getSwissTimingReplayService().loadRaceData(regattaIdentifier, replayRaceDTO.link,
                        replayRaceDTO.swissTimingUrl, replayRaceDTO.getName(), replayRaceDTO.race_id, boatClassName, getService(),
                        getService(), useInternalMarkPassingAlgorithm, getRaceLogStore(), getRegattaLogStore());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error trying to load SwissTimingReplay race " + replayRaceDTO, e);
            }
        }
    }

    /**
     * Finds a competitor in a sequence of competitors that has an {@link Competitor#getId()} equal to <code>id</code>. 
     */
    //READ
    private Competitor getCompetitorByIdAsString(Iterable<Competitor> competitors, String idAsString) {
        for (Competitor c : competitors) {
            if (c.getId().toString().equals(idAsString)) {
                return c;
            }
        }
        return null;
    }

    //READ
    /**
     * @param leaderboardGroupId
     *            if not {@code null}, this takes precedence over the {@code leaderboardGroupName} parameter which will
     *            then be ignored and will be used to look up an optional leaderboard group providing the context, e.g.,
     *            for seasonal scores from an overall leaderboard
     * @param leaderboardGroupName
     *            evaluated only if {@code leaderboardGroupId} was {@code null}; may even be {@code null} if
     *            {@code leaderboardGroupId} is {@code null} too because leaderboard group resolution is optional. If a
     *            non-{@code null} name is provided here and if {@code leaderboardGroupId} was {@code null} then the
     *            name is used to try to resolve the leaderboard group by name.
     */
    private Double getCompetitorRaceDataEntry(DetailType dataType, TrackedRace trackedRace, Competitor competitor,
            TimePoint timePoint, String leaderboardGroupName, UUID leaderboardGroupId, String leaderboardName,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException {
        Double result = null;
        Course course = trackedRace.getRace().getCourse();
        course.lockForRead(); // make sure the tracked leg survives this call even if a course update is pending
        try {
            TrackedLegOfCompetitor trackedLeg = trackedRace.getTrackedLeg(competitor, timePoint);
            switch (dataType) {
            case RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
                final GPSFixTrack<Competitor, GPSFixMoving> sogTrack = trackedRace.getTrack(competitor);
                if (sogTrack != null) {
                    SpeedWithBearing speedOverGround = sogTrack.getEstimatedSpeed(timePoint);
                    result = (speedOverGround == null) ? null : speedOverGround.getKnots();
                }
                break;
            case RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES:
            case CHART_COURSE_OVER_GROUND_TRUE_DEGREES:
                final GPSFixTrack<Competitor, GPSFixMoving> cogTrack = trackedRace.getTrack(competitor);
                if (cogTrack != null) {
                    SpeedWithBearing speedOverGround = cogTrack.getEstimatedSpeed(timePoint);
                    result = (speedOverGround == null) ? null : speedOverGround.getBearing().getDegrees();
                }
                break;
            case RACE_CURRENT_POSITION_LAT_DEG:
                final GPSFixTrack<Competitor, GPSFixMoving> latTrack = trackedRace.getTrack(competitor);
                if (latTrack != null) {
                    Position position = latTrack.getEstimatedPosition(timePoint, /* extrapolate */ true);
                    result = (position == null) ? null : position.getLatDeg();
                }
                break;
            case RACE_CURRENT_POSITION_LNG_DEG:
                final GPSFixTrack<Competitor, GPSFixMoving> lngTrack = trackedRace.getTrack(competitor);
                if (lngTrack != null) {
                    Position position = lngTrack.getEstimatedPosition(timePoint, /* extrapolate */ true);
                    result = (position == null) ? null : position.getLngDeg();
                }
                break;
            case LEG_VELOCITY_MADE_GOOD_IN_KNOTS:
                final Speed velocityMadeGood;
                if (trackedLeg != null) {
                    velocityMadeGood = trackedLeg.getVelocityMadeGood(timePoint, WindPositionMode.EXACT, cache);
                } else {
                    // check if wind information is available; if so, compute a VMG only based on wind data:
                    velocityMadeGood = trackedRace.getVelocityMadeGood(competitor, timePoint, cache);
                }
                result = (velocityMadeGood == null) ? null : velocityMadeGood.getKnots();
                break;
            case LEG_DISTANCE_TRAVELED:
                if (trackedLeg != null) {
                    Distance distanceTraveled = trackedRace.getDistanceTraveled(competitor, timePoint);
                    result = distanceTraveled == null ? null : distanceTraveled.getMeters();
                }
                break;
            case LEG_DISTANCE_TRAVELED_INCLUDING_GATE_START:
                if (trackedLeg != null) {
                    Distance distanceTraveledConsideringGateStart = trackedRace.getDistanceTraveledIncludingGateStart(competitor, timePoint);
                    result = distanceTraveledConsideringGateStart == null ? null : distanceTraveledConsideringGateStart.getMeters();
                }
                break;
            case LEG_GAP_TO_LEADER_IN_SECONDS:
                if (trackedLeg != null) {
                    final RankingInfo rankingInfo = trackedRace.getRankingMetric().getRankingInfo(timePoint, cache);
                    final Duration gapToLeaderInOwnTime = trackedLeg.getTrackedLeg().getTrackedRace().getRankingMetric().getGapToLeaderInOwnTime(rankingInfo, competitor, cache);
                    result = gapToLeaderInOwnTime == null ? null : gapToLeaderInOwnTime.asSeconds();
                }
                break;
            case CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD:
                if (trackedLeg != null) {
                    final RankingInfo rankingInfo = trackedRace.getRankingMetric().getRankingInfo(timePoint, cache);
                    Distance distanceToLeader = trackedLeg.getWindwardDistanceToCompetitorFarthestAhead(timePoint, WindPositionMode.LEG_MIDDLE, rankingInfo, cache);
                    result = (distanceToLeader == null) ? null : distanceToLeader.getMeters();
                }
                break;
            case RACE_IMPLIED_WIND:
                final RankingMetric rankingMetric = trackedRace.getRankingMetric();
                if (rankingMetric instanceof ORCPerformanceCurveRankingMetric) {
                    final ORCPerformanceCurveRankingMetric orcPcsRankingMetric = 
                            (ORCPerformanceCurveRankingMetric) rankingMetric;
                    try {
                        final Speed impliedWind = orcPcsRankingMetric.getImpliedWind(competitor, timePoint, cache);
                        result = impliedWind == null ? null : impliedWind.getKnots();
                    } catch (MaxIterationsExceededException | FunctionEvaluationException e) {
                        logger.log(Level.WARNING, "Problem computing implied wind", e);
                        result = null;
                    }
                }
                break;
            case RACE_RANK:
                if (trackedLeg != null) {
                    result = (double) trackedLeg.getRank(timePoint, cache);
                }
                break;
            case REGATTA_RANK:
                if (leaderboardName == null || leaderboardName.isEmpty()) {
                    break;
                }
                Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
                result = leaderboard == null ? null : (double) leaderboard.getTotalRankOfCompetitor(competitor,
                        timePoint);
                break;
            case OVERALL_RANK:
                if (leaderboardGroupName == null || leaderboardGroupName.isEmpty()) {
                    break;
                }
                final LeaderboardGroup group = getLeaderboardGroupByIdOrName(leaderboardGroupId, leaderboardGroupName);
                Leaderboard overall = group.getOverallLeaderboard();
                result = overall == null ? null : (double) overall.getTotalRankOfCompetitor(competitor, timePoint);
                break;
            case CHART_DISTANCE_TO_START_LINE:
                TimePoint startOfRace = trackedRace.getStartOfRace();
                if (startOfRace == null || timePoint.before(startOfRace) || timePoint.equals(startOfRace)) {
                    Distance distanceToStartLine = trackedRace.getDistanceToStartLine(competitor, timePoint);
                    result = distanceToStartLine == null ? null : distanceToStartLine.getMeters();
                }
                break;
            case CHART_BEAT_ANGLE:
            case CHART_ABS_TWA:
                Bearing twa = trackedRace.getTWA(competitor, timePoint, cache);
                result = twa == null ? null : twa.getDegrees();
                if (result != null && dataType == DetailType.CHART_ABS_TWA) {
                    result = Math.abs(result);
                }
                break;
            case BRAVO_RACE_HEEL_IN_DEGREES:
            case BRAVO_LEG_CURRENT_HEEL_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getHeel, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVO_RACE_PITCH_IN_DEGREES:
            case BRAVO_LEG_CURRENT_PITCH_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getPitch, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVO_RACE_CURRENT_RIDE_HEIGHT_IN_METERS: {
                result = getBravoDistanceInMeters(BravoFixTrack::getRideHeight, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_PORT_DAGGERBOARD_RAKE: {
                result = getBravoDoubleValue(BravoFixTrack::getPortDaggerboardRakeIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_STBD_DAGGERBOARD_RAKE: {
                result = getBravoDoubleValue(BravoFixTrack::getStbdDaggerboardRakeStbdIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_PORT_RUDDER_RAKE: {
                result = getBravoDoubleValue(BravoFixTrack::getPortRudderRakeIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_STBD_RUDDER_RAKE: {
                result = getBravoDoubleValue(BravoFixTrack::getStbdRudderRakeIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_MAST_ROTATION_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getMastRotationIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_LEEWAY_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getLeewayIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_SET: {
                result = getBravoDoubleValue(BravoFixTrack::getSetIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_DRIFT_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getDriftIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_DEPTH_IN_METERS: {
                result = getBravoDistanceInMeters(BravoFixTrack::getDepthIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_RUDDER_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getRudderIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_TACK_ANGLE_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getTackAngleIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_PERCENTAGE: {
                result = getBravoDoubleValue(BravoFixTrack::getDeflectorPercentageIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_DEFLECTOR_IN_MILLIMETERS: {
                Double deflectorInMeters = getBravoDistanceInMeters(BravoFixTrack::getDeflectorIfAvailable, trackedRace, competitor, timePoint);
                result = deflectorInMeters == null ? null : (deflectorInMeters * 1000.);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_RAKE_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getRakeIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_TARGET_HEEL_ANGLE_IN_DEGREES: {
                result = getBravoBearingInDegrees(BravoFixTrack::getTargetHeelIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_LOAD: {
                result = getBravoDoubleValue(BravoFixTrack::getForestayLoadIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_FORESTAY_PRESSURE: {
                result = getBravoDoubleValue(BravoFixTrack::getForestayPressureIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case BRAVOEXTENDED_RACE_CURRENT_TARGET_BOATSPEED_PERCENTAGE: {
                result = getBravoDoubleValue(BravoFixTrack::getTargetBoatspeedPIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            
            case EXPEDITION_RACE_AWA: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionAWAIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_AWS: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionAWSIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_BARO: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionBaroIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_BOAT_SPEED: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionBoatSpeedIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_COG: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionCOGIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_COURSE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionCourseDetailIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_DIST_TO_PORT_LAYLINE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionDistToPortLaylineIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_DIST_TO_STB_LAYLINE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionDistToStbLaylineIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_DISTANCE_BELOW_LINE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionDistanceBelowLineInMetersIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_DISTANCE_TO_COMMITTEE_BOAT: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionDistanceToCommitteeBoatIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_DISTANCE_TO_PIN: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionDistanceToPinDetailIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_FORESTAY_LOAD: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionForestayLoadIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_HEADING: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionHeadingIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_JIB_CAR_PORT: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionJibCarPortIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_JIB_CAR_STBD: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionJibCarStbdIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_LINE_SQUARE_FOR_WIND_DIRECTION: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionLineSquareForWindIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_LOAD_P: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionLoadPIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_LOAD_S: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionLoadSIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_MAST_BUTT: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionMastButtIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_RAKE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionRakeIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_RATE_OF_TURN: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionRateOfTurnIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_RUDDER_ANGLE: {
                result = getBravoBearingInDegrees(BravoFixTrack::getRudderIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_SOG: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionSOGIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TARG_BOAT_SPEED: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTargBoatSpeedIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TARG_TWA: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTargTWAIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TARGET_HEEL: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTargetHeelIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_BURN_TO_COMMITTEE_BOAT: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToBurnToCommitteeBoatIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_BURN_TO_LINE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToBurnToLineInSecondsIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_BURN_TO_PIN: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToBurnToPinIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_COMMITTEE_BOAT: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToCommitteeBoatIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_GUN: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToGunInSecondsIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_PIN: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToPinIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_PORT_LAYLINE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToPortLaylineIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TIME_TO_STB_LAYLINE: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTimeToStbLaylineIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TWA: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTWAIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TWD: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTWDIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_TWS: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionTWSIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_VMG: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionVMGIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            case EXPEDITION_RACE_VMG_TARG_VMG_DELTA: {
                result = getBravoDoubleValue(BravoFixTrack::getExpeditionVMGTargVMGDeltaIfAvailable, trackedRace, competitor, timePoint);
                break;
            }
            default:
                throw new UnsupportedOperationException("There is currently no support for the enum value '" + dataType
                        + "' in this method.");
            }
            return result;
        } finally {
            course.unlockAfterRead();
        }
    }

    //READ
    private Double getBravoDoubleValue(BiFunction<BravoFixTrack<Competitor>, TimePoint, Double> valueGetter,
            TrackedRace trackedRace, Competitor competitor, TimePoint timePoint) {
        return getBravoValue(valueGetter, Function.identity(), trackedRace, competitor, timePoint);
    }
    
  //READ
    private Double getBravoBearingInDegrees(BiFunction<BravoFixTrack<Competitor>, TimePoint, Bearing> valueGetter,
            TrackedRace trackedRace, Competitor competitor, TimePoint timePoint) {
        return getBravoValue(valueGetter, Bearing::getDegrees, trackedRace, competitor, timePoint);
    }
    
  //READ
    private Double getBravoDistanceInMeters(BiFunction<BravoFixTrack<Competitor>, TimePoint, Distance> valueGetter,
            TrackedRace trackedRace, Competitor competitor, TimePoint timePoint) {
        return getBravoValue(valueGetter, Distance::getMeters, trackedRace, competitor, timePoint);
    }
    
  //READ
    private <T> Double getBravoValue(BiFunction<BravoFixTrack<Competitor>, TimePoint, T> valueGetter,
            Function<T, Double> mapperToDouble,
            TrackedRace trackedRace, Competitor competitor, TimePoint timePoint) {
        final Double result;
        final BravoFixTrack<Competitor> bravoFixTrack = trackedRace
                .<BravoFix, BravoFixTrack<Competitor>> getSensorTrack(competitor, BravoFixTrack.TRACK_NAME);
        if (bravoFixTrack != null) {
            final T t = valueGetter.apply(bravoFixTrack, timePoint);
            result = t == null ? null : mapperToDouble.apply(t);
        } else {
            result = null;
        }
        return result;
    }

    @Override
  //READ
    public CompetitorsRaceDataDTO getCompetitorsRaceData(RegattaAndRaceIdentifier race, List<CompetitorDTO> competitors, Date from, Date to,
            final long stepSizeInMillis, final DetailType detailType, final String leaderboardGroupName, final UUID leaderboardGroupId, 
            final String leaderboardName) throws NoWindException {
        CompetitorsRaceDataDTO result = null;
        final TrackedRace trackedRace = getExistingTrackedRace(race);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        if (trackedRace != null) {
            TimePoint newestEvent = trackedRace.getTimePointOfNewestEvent();
            final TimePoint startTime = from == null ? trackedRace.getStartOfTracking() : new MillisecondsTimePoint(from);
            final TimePoint endTime = (to == null || to.after(newestEvent.asDate())) ? newestEvent : new MillisecondsTimePoint(to);
            final long adjustedStepSizeInMillis = (long) Math.max((double) stepSizeInMillis, startTime.until(endTime).divide(SailingServiceConstants.MAX_NUMBER_OF_FIXES_TO_QUERY).asMillis());
            result = new CompetitorsRaceDataDTO(detailType, startTime==null?null:startTime.asDate(), endTime==null?null:endTime.asDate());
            final int MAX_CACHE_SIZE = SailingServiceConstants.MAX_NUMBER_OF_FIXES_TO_QUERY;
            final ConcurrentHashMap<TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache> cachesByTimePoint = new ConcurrentHashMap<>();
            Map<CompetitorDTO, FutureTask<CompetitorRaceDataDTO>> resultFutures = new HashMap<>();
            for (final CompetitorDTO competitorDTO : competitors) {
                FutureTask<CompetitorRaceDataDTO> future = new FutureTask<CompetitorRaceDataDTO>(new Callable<CompetitorRaceDataDTO>() {
                    @Override
                    public CompetitorRaceDataDTO call() throws NoWindException {
                        Competitor competitor = getCompetitorByIdAsString(trackedRace.getRace().getCompetitors(),
                                competitorDTO.getIdAsString());
                        ArrayList<com.sap.sse.common.Util.Triple<String, Date, Double>> markPassingsData = new ArrayList<com.sap.sse.common.Util.Triple<String, Date, Double>>();
                        ArrayList<com.sap.sse.common.Util.Pair<Date, Double>> raceData = new ArrayList<com.sap.sse.common.Util.Pair<Date, Double>>();
                        // Filling the mark passings
                        Set<MarkPassing> competitorMarkPassings = trackedRace.getMarkPassings(competitor);
                        if (competitorMarkPassings != null) {
                            trackedRace.lockForRead(competitorMarkPassings);
                            try {
                                for (MarkPassing markPassing : competitorMarkPassings) {
                                    MillisecondsTimePoint time = new MillisecondsTimePoint(markPassing.getTimePoint().asMillis());
                                    WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = cachesByTimePoint
                                            .computeIfAbsent(time, LeaderboardDTOCalculationReuseCache::new);
                                    Double competitorMarkPassingsData = getCompetitorRaceDataEntry(detailType,
                                            trackedRace, competitor, time, leaderboardGroupName, leaderboardGroupId, leaderboardName, cache);
                                    if (competitorMarkPassingsData != null) {
                                        markPassingsData.add(new com.sap.sse.common.Util.Triple<String, Date, Double>(markPassing
                                                .getWaypoint().getName(), time.asDate(), competitorMarkPassingsData));
                                    }
                                }
                            } finally {
                                trackedRace.unlockAfterRead(competitorMarkPassings);
                            }
                        }
                        if (startTime != null && endTime != null) {
                            for (long i = startTime.asMillis(); i <= endTime.asMillis(); i += adjustedStepSizeInMillis) {
                                MillisecondsTimePoint time = new MillisecondsTimePoint(i);
                                WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = cachesByTimePoint.get(time);
                                if (cache == null) {
                                    cache = new LeaderboardDTOCalculationReuseCache(time);
                                    if (cachesByTimePoint.size() >= MAX_CACHE_SIZE) {
                                        final Iterator<Entry<TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache>> iterator = cachesByTimePoint.entrySet().iterator();
                                        while (cachesByTimePoint.size() >= MAX_CACHE_SIZE && iterator.hasNext()) {
                                            iterator.next();
                                            iterator.remove();
                                        }
                                    }
                                    cachesByTimePoint.put(time, cache);
                                }
                                Double competitorRaceData = getCompetitorRaceDataEntry(detailType, trackedRace,
                                        competitor, time, leaderboardGroupName, leaderboardGroupId, leaderboardName, cache);
                                if (competitorRaceData != null) {
                                    raceData.add(new com.sap.sse.common.Util.Pair<Date, Double>(time.asDate(), competitorRaceData));
                                }
                            }
                        }
                        return new CompetitorRaceDataDTO(competitorDTO, detailType, markPassingsData, raceData);
                    }
                });
                resultFutures.put(competitorDTO, future);
                executor.execute(future);
            }
            for (Map.Entry<CompetitorDTO, FutureTask<CompetitorRaceDataDTO>> e : resultFutures.entrySet()) {
                CompetitorRaceDataDTO competitorData;
                try {
                    competitorData = e.getValue().get();
                } catch (InterruptedException e1) {
                    competitorData = null;
                    logger.log(Level.SEVERE, "Exception while trying to compute competitor data "+detailType+" for competitor "+e.getKey().getName(), e1);
                } catch (ExecutionException e1) {
                    competitorData = null;
                    logger.log(Level.SEVERE, "Exception while trying to compute competitor data "+detailType+" for competitor "+e.getKey().getName(), e1);
                }
                result.setCompetitorData(e.getKey(), competitorData);
            }
        }
        return result;
    }

    @Override
  //READ
    public List<Triple<String, List<CompetitorDTO>, List<Double>>> getLeaderboardDataEntriesForAllRaceColumns(String leaderboardName, 
            Date date, DetailType detailType) throws Exception {
        List<com.sap.sse.common.Util.Triple<String, List<CompetitorDTO>, List<Double>>> result = new ArrayList<>();
        // Attention: The reason why we read the data from the LeaderboardDTO and not from the leaderboard directly is to ensure
        // the use of the leaderboard cache.
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            TimePoint timePoint;
            if (date == null) {
                timePoint = null;
            } else {
                timePoint = new MillisecondsTimePoint(date);
            }
            TimePoint effectiveTimePoint = timePoint == null ? leaderboard.getNowMinusDelay() : timePoint;
            if (detailType != null) {
                switch (detailType) {
                case REGATTA_NET_POINTS_SUM:
                    for (Entry<RaceColumn, Map<Competitor, Double>> e : leaderboard.getNetPointsSumAfterRaceColumn(effectiveTimePoint).entrySet()) {
                        List<CompetitorDTO> competitorDTOs = new ArrayList<>();
                        List<Double> pointSums = new ArrayList<>();
                        for (Entry<Competitor, Double> e2 : e.getValue().entrySet()) {
                            competitorDTOs.add(baseDomainFactory.convertToCompetitorDTO(e2.getKey()));
                            pointSums.add(e2.getValue());
                        }
                        result.add(new Triple<>(e.getKey().getName(), competitorDTOs, pointSums)); 
                    }
                    break;
                case REGATTA_RANK:
                case OVERALL_RANK:
                    Map<RaceColumn, List<Competitor>> competitorsFromBestToWorst = leaderboard
                            .getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(effectiveTimePoint);
                    for (Entry<RaceColumn, List<Competitor>> e : competitorsFromBestToWorst.entrySet()) {
                        int rank = 1;
                        List<Double> values = new ArrayList<Double>();
                        List<CompetitorDTO> competitorDTOs = new ArrayList<>();
                        for (Competitor competitor : e.getValue()) {
                            values.add(new Double(rank));
                            competitorDTOs.add(baseDomainFactory.convertToCompetitorDTO(competitor));
                            rank++;
                        }
                        result.add(new Triple<>(e.getKey().getName(), competitorDTOs, values));
                    }
                    break;
                default:
                    break;
                }
            }

        }
        return result;
    }

    @Override
    //READ
    public List<com.sap.sse.common.Util.Pair<String, String>> getLeaderboardsNamesOfMetaLeaderboard(
            String metaLeaderboardName) {
        Leaderboard leaderboard = getService().getLeaderboardByName(metaLeaderboardName);
        if (leaderboard == null) {
            throw new IllegalArgumentException("Couldn't find leaderboard named " + metaLeaderboardName);
        }
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        if (leaderboard instanceof RegattaLeaderboard) {
            getSecurityService().checkCurrentUserReadPermission(((RegattaLeaderboard) leaderboard).getRegatta());
        }
        if (!(leaderboard instanceof MetaLeaderboard)) {
            throw new IllegalArgumentException("The leaderboard " + metaLeaderboardName + " is not a metaleaderboard");
        }
        MetaLeaderboard metaLeaderboard = (MetaLeaderboard) leaderboard;
        LeaderboardGroup groupOrNull = null;
        for (LeaderboardGroup lg : getService().getLeaderboardGroups().values()) {
            if (metaLeaderboard.equals(lg.getOverallLeaderboard())) {
                if (getSecurityService().hasCurrentUserReadPermission(lg)) {
                    groupOrNull = lg;
                    break;
                }
            }
        }
        // If we could identify the associated LeaderboardGroup the Leaderboards can be sorted based on that group
        Iterable<Leaderboard> leaderBoards = groupOrNull != null
                ? HomeServiceUtil.getLeaderboardsForSeriesInOrderWithReadPermissions(groupOrNull, getService())
                : metaLeaderboard.getLeaderboards();
        List<com.sap.sse.common.Util.Pair<String, String>> result = new ArrayList<com.sap.sse.common.Util.Pair<String, String>>();
        for (Leaderboard containedLeaderboard : leaderBoards) {
            // we need to filter because metaLeaderboard.getLeaderboards might return non visible ones
            if (getSecurityService().hasCurrentUserReadPermission(containedLeaderboard)) {
                if (containedLeaderboard instanceof RegattaLeaderboard) {
                    Regatta regatta = ((RegattaLeaderboard) containedLeaderboard).getRegatta();
                    if (getSecurityService().hasCurrentUserReadPermission(regatta)) {
                        result.add(new com.sap.sse.common.Util.Pair<String, String>(containedLeaderboard.getName(),
                                containedLeaderboard.getDisplayName() != null ? containedLeaderboard.getDisplayName()
                                        : containedLeaderboard.getName()));
                    }
                } else {
                    result.add(new com.sap.sse.common.Util.Pair<String, String>(containedLeaderboard.getName(),
                            containedLeaderboard.getDisplayName() != null ? containedLeaderboard.getDisplayName()
                                    : containedLeaderboard.getName()));
                }
            }
        }
        return result;
    }

    @Override
  //READ
    public Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> getDouglasPoints(
            RegattaAndRaceIdentifier raceIdentifier, Map<CompetitorDTO, TimeRange> competitorTimeRanges, double meters)
            throws NoWindException {
        final Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> result = new HashMap<>();
        final TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        if (trackedRace != null) {
            final MeterDistance maxDistance = new MeterDistance(meters);
            for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                final CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
                if (competitorTimeRanges.containsKey(competitorDTO)) {
                    // get Track of competitor
                    final GPSFixTrack<Competitor, GPSFixMoving> gpsFixTrack = trackedRace.getTrack(competitor);
                    // Distance for DouglasPeucker
                    final TimeRange timeRange = competitorTimeRanges.get(competitorDTO);
                    final Iterable<GPSFixMoving> gpsFixApproximation = trackedRace.approximate(competitor, maxDistance,
                            timeRange.from(), timeRange.to());
                    final List<GPSFixDTOWithSpeedWindTackAndLegType> gpsFixDouglasList = new ArrayList<>();
                    GPSFix fix = null;
                    for (GPSFix next : gpsFixApproximation) {
                        if (fix != null) {
                            final Bearing bearing = fix.getPosition().getBearingGreatCircle(next.getPosition());
                            final Speed speed = fix.getPosition().getDistance(next.getPosition())
                                    .inTime(next.getTimePoint().asMillis() - fix.getTimePoint().asMillis());
                            final SpeedWithBearing speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(), bearing);
                            gpsFixDouglasList.add(createDouglasPeuckerGPSFixDTO(trackedRace, competitor, fix, speedWithBearing));
                        }
                        fix = next;
                    }
                    if (fix != null) {
                        // add one last GPSFixDTO with no successor to calculate speed/bearing to:
                        final SpeedWithBearing speedWithBearing = gpsFixTrack.getEstimatedSpeed(fix.getTimePoint());
                        gpsFixDouglasList.add(createDouglasPeuckerGPSFixDTO(trackedRace, competitor, fix, speedWithBearing));
                    }
                    result.put(competitorDTO, gpsFixDouglasList);
                }
            }
        }
        return result;
    }

    //READ
    private GPSFixDTOWithSpeedWindTackAndLegType createDouglasPeuckerGPSFixDTO(TrackedRace trackedRace, Competitor competitor, GPSFix fix,
            SpeedWithBearing speedWithBearing) throws NoWindException {
        Tack tack = trackedRace.getTack(competitor, fix.getTimePoint());
        TrackedLegOfCompetitor trackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor,
                fix.getTimePoint());
        LegType legType = trackedLegOfCompetitor == null ? null : trackedRace.getTrackedLeg(
                trackedLegOfCompetitor.getLeg()).getLegType(fix.getTimePoint());
        Wind wind = trackedRace.getWind(fix.getPosition(), fix.getTimePoint());
        WindDTO windDTO = createWindDTOFromAlreadyAveraged(wind, fix.getTimePoint());
        GPSFixDTOWithSpeedWindTackAndLegType fixDTO = createGPSFixDTO(fix, speedWithBearing, windDTO, tack, legType, /* extrapolated */
                false, null);
        return fixDTO;
    }

    @Override
    //READ
    public Map<CompetitorDTO, List<ManeuverDTO>> getManeuvers(RegattaAndRaceIdentifier raceIdentifier,
            Map<CompetitorDTO, TimeRange> competitorTimeRanges) throws NoWindException {
        final Map<CompetitorDTO, List<ManeuverDTO>> result = new HashMap<>();
        final TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        if (trackedRace != null) {
            final Map<CompetitorDTO, Future<List<ManeuverDTO>>> futures = new HashMap<>();
            for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                final CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
                if (competitorTimeRanges.containsKey(competitorDTO)) {
                    final TimeRange timeRange = competitorTimeRanges.get(competitorDTO);
                    final TimePoint from = timeRange.from(), to = timeRange.to();
                    final RunnableFuture<List<ManeuverDTO>> future = new FutureTask<>(() -> {
                        // We're on a web server request thread. Try not to take too long for this,
                        // so don't wait for the latest results unless the cache doesn't have a valid
                        // result yet:
                        Iterable<Maneuver> maneuvers = trackedRace.getManeuvers(competitor, from, to, /* waitForLatest */ false);
                        if (maneuvers == null) {
                            maneuvers = trackedRace.getManeuvers(competitor, from, to, /* waitForLatest */ true);
                        }
                        return createManeuverDTOsForCompetitor(maneuvers, trackedRace, competitor);
                    });
                    executor.execute(future);
                    futures.put(competitorDTO, future);
                }
            }
            for (Map.Entry<CompetitorDTO, Future<List<ManeuverDTO>>> competitorAndFuture : futures.entrySet()) {
                try {
                    result.put(competitorAndFuture.getKey(), competitorAndFuture.getValue().get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    //READ
    private List<ManeuverDTO> createManeuverDTOsForCompetitor(Iterable<Maneuver> maneuvers, TrackedRace trackedRace, Competitor competitor) {
        final List<ManeuverDTO> result = new ArrayList<ManeuverDTO>();
        for (Maneuver maneuver : maneuvers) {
            final ManeuverType type = maneuver.getType();
            final Tack newTack = maneuver.getNewTack();
            final Position position = maneuver.getPosition();
            final Date timepoint = maneuver.getTimePoint().asDate();
            final Date timePointBefore = maneuver.getManeuverBoundaries().getTimePointBefore().asDate();
            final SpeedWithBearingDTO speedBefore = createSpeedWithBearingDTO(maneuver.getSpeedWithBearingBefore());
            final SpeedWithBearingDTO speedAfter = createSpeedWithBearingDTO(maneuver.getSpeedWithBearingAfter());
            final double directionChangeInDegrees = maneuver.getDirectionChangeInDegrees();
            final double maxTurningRateInDegreesPerSecond = maneuver.getMaxTurningRateInDegreesPerSecond();
            final double averageTurningRateInDegreesPerSecond = maneuver.getAvgTurningRateInDegreesPerSecond();
            final double lowestSpeedInKnots = maneuver.getLowestSpeed().getKnots();
            final Date markPassingTimePoint = maneuver.isMarkPassing()
                    ? maneuver.getMarkPassing().getTimePoint().asDate() : null;
            final NauticalSide markPassingSide = maneuver.isMarkPassing() ? maneuver.getToSide() : null;
            final SpeedWithBearing speedWithBearingBeforeManeuverLoss = maneuver.getManeuverLoss() == null ? null
                    : maneuver.getManeuverLoss().getSpeedWithBearingBefore();
            final Double middleManeuverAngle = maneuver.getManeuverLoss() == null ? null : maneuver.getManeuverLoss().getMiddleManeuverAngle().getDegrees();
            final ManeuverLossDTO maneuverLoss = maneuver.getManeuverLoss() == null ? null
                    : new ManeuverLossDTO(maneuver.getManeuverLoss().getManeuverStartPosition(),
                            maneuver.getManeuverLoss().getManeuverEndPosition(), speedWithBearingBeforeManeuverLoss, middleManeuverAngle,
                            maneuver.getManeuverLoss().getManeuverDuration(), maneuver.getManeuverLoss().getProjectedDistanceLost());
            result.add(new ManeuverDTO(type, newTack, position, timepoint, timePointBefore, speedBefore,
                    speedAfter, directionChangeInDegrees, maxTurningRateInDegreesPerSecond,
                    averageTurningRateInDegreesPerSecond, lowestSpeedInKnots, markPassingTimePoint, markPassingSide, maneuverLoss));
        }
        return result;
    }

    @Override
    //READ
    public RaceDefinition getRace(RegattaAndRaceIdentifier raceIdentifier) {
        Regatta regatta = getService().getRegattaByName(raceIdentifier.getRegattaName());
        RaceDefinition race = getRaceByName(regatta, raceIdentifier.getRaceName());
        return race;
    }

    @Override
  //READ
    public DynamicTrackedRace getTrackedRace(RegattaAndRaceIdentifier regattaNameAndRaceName) {
        Regatta regatta = getService().getRegattaByName(regattaNameAndRaceName.getRegattaName());
        RaceDefinition race = getRaceByName(regatta, regattaNameAndRaceName.getRaceName());
        DynamicTrackedRace trackedRace = getService().getOrCreateTrackedRegatta(regatta).getTrackedRace(race);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        return trackedRace;
    }

    @Override
  //READ
    public TrackedRace getExistingTrackedRace(RegattaAndRaceIdentifier regattaNameAndRaceName) {
        final DynamicTrackedRace existingTrackedRace = getService().getExistingTrackedRace(regattaNameAndRaceName);
        getSecurityService().checkCurrentUserReadPermission(existingTrackedRace);
        return existingTrackedRace;
    }

    @Override
  //READ
    public Regatta getRegatta(RegattaName regattaName) {
        final Regatta regattaByName = getService().getRegattaByName(regattaName.getRegattaName());
        getSecurityService().checkCurrentUserReadPermission(regattaByName);
        return regattaByName;
    }

    /**
     * Returns a servlet context that, when asked for a resource, first tries the original servlet context's implementation. If that
     * fails, it prepends "war/" to the request because the war/ folder contains all the resources exposed externally
     * through the HTTP server.
     */
    @Override
    //??
    public ServletContext getServletContext() {
        return new DelegatingServletContext(super.getServletContext());
    }

    @Override
    /**
     * Override of function to prevent exception "Blocked request without GWT permutation header (XSRF attack?)" when testing the GWT sites
     */
  //??
    protected void checkPermutationStrongName() throws SecurityException {
        //Override to prevent exception "Blocked request without GWT permutation header (XSRF attack?)" when testing the GWT sites
        return;
    }

    @Override
    //READ
    public List<LeaderboardGroupDTO> getLeaderboardGroups(boolean withGeoLocationData) {
        final Map<UUID, LeaderboardGroup> leaderboardGroups = getService().getLeaderboardGroups();
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                leaderboardGroups.values(),
                leaderboardGroup -> convertToLeaderboardGroupDTO(leaderboardGroup, withGeoLocationData, false));
    }

    @Override
    //READ
    public LeaderboardGroupDTO getLeaderboardGroupByName(String groupName, boolean withGeoLocationData) {
        final LeaderboardGroup leaderboardGroupByName = getService().getLeaderboardGroupByName(groupName);
        getSecurityService().checkCurrentUserReadPermission(leaderboardGroupByName);
        return leaderboardGroupByName == null ? null : convertToLeaderboardGroupDTO(leaderboardGroupByName, withGeoLocationData, false);
    }
    
    @Override
    //READ
    public LeaderboardGroupDTO getLeaderboardGroupById(final UUID groupId) {
        final LeaderboardGroup leaderboardGroupById = getService().getLeaderboardGroupByID(groupId);
        getSecurityService().checkCurrentUserReadPermission(leaderboardGroupById);
        return leaderboardGroupById == null ? null : convertToLeaderboardGroupDTO(leaderboardGroupById, false, false);
    }
    
    /**
     * @see #getLeaderboardGroupByIdOrName(UUID, String)
     */
    @Override
    //READ
    public LeaderboardGroupDTO getLeaderboardGroupByUuidOrName(final UUID groupUuid, final String groupName) {
        final LeaderboardGroup leaderboardGroup = getLeaderboardGroupByIdOrName(groupUuid, groupName);
        getSecurityService().checkCurrentUserReadPermission(leaderboardGroup);
        return leaderboardGroup == null ? null : convertToLeaderboardGroupDTO(leaderboardGroup, false, false);
    }
    
    //READ
    protected LeaderboardGroupDTO convertToLeaderboardGroupDTO(final LeaderboardGroup leaderboardGroup,
            final boolean withGeoLocationData, final boolean withStatisticalData) {
        final LeaderboardGroupDTO groupDTO = new LeaderboardGroupDTO(leaderboardGroup.getId(),
                leaderboardGroup.getName(), leaderboardGroup.getDisplayName(), leaderboardGroup.getDescription());
        groupDTO.displayLeaderboardsInReverseOrder = leaderboardGroup.isDisplayGroupsInReverseOrder();
        for (final Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
            try {
                groupDTO.leaderboards.add(createStrippedLeaderboardDTO(leaderboard, withGeoLocationData, withStatisticalData));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Caught exception while reading data for leaderboard " + leaderboard.getName(), e);
            }
        }
        final Leaderboard overallLeaderboard = leaderboardGroup.getOverallLeaderboard();
        if (overallLeaderboard != null) {
            if (overallLeaderboard.getResultDiscardingRule() instanceof ThresholdBasedResultDiscardingRule) {
                groupDTO.setOverallLeaderboardDiscardThresholds(((ThresholdBasedResultDiscardingRule) overallLeaderboard
                        .getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces());
            }
            groupDTO.setOverallLeaderboardScoringSchemeType(overallLeaderboard.getScoringScheme().getType());
        }
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), groupDTO, leaderboardGroup.getIdentifier());
        return groupDTO;
    }


    @Override
    //READ
    public ReplicationStateDTO getReplicaInfo() {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.READ_REPLICATOR);
        ReplicationService service = getReplicationService();
        Set<ReplicaDTO> replicaDTOs = new HashSet<ReplicaDTO>();
        for (ReplicaDescriptor replicaDescriptor : service.getReplicaInfo()) {
            final Map<Class<? extends OperationWithResult<?, ?>>, Integer> statistics = service.getStatistics(replicaDescriptor);
            Map<String, Integer> replicationCountByOperationClassName = new HashMap<String, Integer>();
            for (Entry<Class<? extends OperationWithResult<?, ?>>, Integer> e : statistics.entrySet()) {
                replicationCountByOperationClassName.put(e.getKey().getName(), e.getValue());
            }
            replicaDTOs.add(new ReplicaDTO(replicaDescriptor.getIpAddress().getHostName(),
                    replicaDescriptor.getRegistrationTime().asDate(), replicaDescriptor.getUuid().toString(),
                    replicaDescriptor.getReplicableIdsAsStrings(), replicaDescriptor.getAdditionalInformation(),
                    replicationCountByOperationClassName,
                    service.getAverageNumberOfOperationsPerMessage(replicaDescriptor), service.getNumberOfMessagesSent(replicaDescriptor),
                    service.getNumberOfBytesSent(replicaDescriptor), service.getAverageNumberOfBytesPerMessage(replicaDescriptor)));
        }
        ReplicationMasterDTO master;
        ReplicationMasterDescriptor replicatingFromMaster = service.getReplicatingFromMaster();
        if (replicatingFromMaster == null) {
            master = null;
        } else {
            master = new ReplicationMasterDTO(replicatingFromMaster.getHostname(), replicatingFromMaster.getServletPort(),
                    replicatingFromMaster.getMessagingHostname(), replicatingFromMaster.getMessagingPort(), replicatingFromMaster.getExchangeName(),
                    StreamSupport.stream(replicatingFromMaster.getReplicables().spliterator(), /* parallel */ false).map(r->r.getId()).toArray(s->new String[s]));
        }
        return new ReplicationStateDTO(master, replicaDTOs, service.getServerIdentifier().toString());
    }

    /**
     * A warning shall be issued to the administration user if the {@link RacingEventService} is a replica. For all
     * other {@link Replicable}s such as the {@link SecurityService} we don't care.
     */
    @Override
    //READ
    public String[] getReplicableIdsAsStringThatShallLeadToWarningAboutInstanceBeingReplica() {
        return new String[] { getService().getId().toString() };
    }
    
    @Override
    //READ
    public void startReplicatingFromMaster(String messagingHost, String masterHostName, String exchangeName,
            int servletPort, int messagingPort, String usernameOrNull, String passwordOrNull)
            throws IOException, ClassNotFoundException, InterruptedException {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.START_REPLICATION);
        // The queue name must always be the same for this server. In order to achieve
        // this we're using the unique server identifier
        final ReplicationService replicationService = getReplicationService();
        replicationService.setReplicationStarting(true);
        try {
            replicationService.startToReplicateFrom(replicationService.createReplicationMasterDescriptor(messagingHost,
                    masterHostName, exchangeName, servletPort, messagingPort,
                    /* use local server identifier as queue name */ replicationService.getServerIdentifier().toString(),
                    RemoteServerUtil.resolveBearerTokenForRemoteServer(masterHostName, servletPort, usernameOrNull,
                            passwordOrNull),
                    replicationService.getAllReplicables()));
        } finally {
            replicationService.setReplicationStarting(false);
        }
    }

    @Override
    //READ
    public List<EventDTO> getEvents() throws MalformedURLException {
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                getService().getAllEvents(), event -> {
                    EventDTO eventDTO = convertToEventDTO(event, false);
                    try {
                        eventDTO.setBaseURL(getEventBaseURLFromEventOrRequest(event));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    eventDTO.setIsOnRemoteServer(false);
                    return eventDTO;
                });
    }

    /**
     * Determines the base URL (protocol, host and port parts) used for the currently executing servlet request. Defaults
     * to <code>http://sapsailing.com</code>.
     * @throws MalformedURLException 
     */
  //READ
    private URL getRequestBaseURL() throws MalformedURLException {
        final URL url = new URL(getThreadLocalRequest().getRequestURL().toString());
        final URL baseURL = getBaseURL(url);
        return baseURL;
    }

  //READ
    private URL getBaseURL(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), /* file */ "");
    }

  //READ
    private RemoteSailingServerReferenceDTO createRemoteSailingServerReferenceDTO(
            final RemoteSailingServerReference serverRef,
            final com.sap.sse.common.Util.Pair<Iterable<EventBase>, Exception> eventsOrException) {
        final Iterable<EventBase> events = eventsOrException.getA();
        final Iterable<EventBaseDTO> eventDTOs;
        final RemoteSailingServerReferenceDTO sailingServerDTO;
        if (events == null) {
            eventDTOs = null;
            final Exception exception = eventsOrException.getB();
            sailingServerDTO = new RemoteSailingServerReferenceDTO(serverRef.getName(),
                    serverRef.getURL().toExternalForm(), exception==null?null:exception.getMessage());
        } else {
            eventDTOs = convertToEventDTOs(events);
            sailingServerDTO = new RemoteSailingServerReferenceDTO(
                    serverRef.getName(), serverRef
                            .getURL().toExternalForm(), eventDTOs);
        }
        return sailingServerDTO;
    }
    
  //READ
    private Iterable<EventBaseDTO> convertToEventDTOs(Iterable<EventBase> events) {
        List<EventBaseDTO> result = new ArrayList<>();
        for (EventBase event : events) {
            EventBaseDTO eventDTO = convertToEventDTO(event);
            result.add(eventDTO);
        }
        return result;
    }

    @Override
    //READ
    public Pair<Integer, Integer> resolveImageDimensions(String imageUrlAsString) throws Exception {
        final Pair<Integer, Integer> imageDimensions;
        if (imageUrlAsString != null && !imageUrlAsString.isEmpty()) {
            URL imageURL = new URL(imageUrlAsString);
            imageDimensions = MediaUtils.getImageDimensions(imageURL);
        } else {
            imageDimensions = null;
        }
        return imageDimensions;
    }
    
    //READ
    protected String getEventBaseURLFromEventOrRequest(Event event) throws MalformedURLException {
        return event.getBaseURL() == null ? getRequestBaseURL().toString() : event.getBaseURL().toString();
    }
    //READ
    private EventBaseDTO convertToEventDTO(EventBase event) {
        final EventBaseDTO eventDTO;
        if (event == null) {
            eventDTO = null;
        } else {
            List<LeaderboardGroupBaseDTO> lgDTOs = new ArrayList<>();
            if (event.getLeaderboardGroups() != null) {
                for (LeaderboardGroupBase lgBase : event.getLeaderboardGroups()) {
                    lgDTOs.add(convertToLeaderboardGroupBaseDTO(lgBase));
                }
            }
            eventDTO = new EventBaseDTO(event.getName(), lgDTOs);
            copyEventBaseFieldsToDTO(event, eventDTO);
        }
        return eventDTO;
    }

    //READ
    private LeaderboardGroupBaseDTO convertToLeaderboardGroupBaseDTO(LeaderboardGroupBase leaderboardGroupBase) {
        return new LeaderboardGroupBaseDTO(leaderboardGroupBase.getId(), leaderboardGroupBase.getName(),
                leaderboardGroupBase.getDescription(), leaderboardGroupBase.getDisplayName(),
                leaderboardGroupBase.hasOverallLeaderboard());
    }
    
    //READ
    private void copyEventBaseFieldsToDTO(EventBase event, EventBaseDTO eventDTO) {
        eventDTO.venue = new VenueDTO();
        eventDTO.venue.setName(event.getVenue() != null ? event.getVenue().getName() : null);
        eventDTO.startDate = event.getStartDate() != null ? event.getStartDate().asDate() : null;
        eventDTO.endDate = event.getStartDate() != null ? event.getEndDate().asDate() : null;
        eventDTO.isPublic = event.isPublic();
        eventDTO.id = (UUID) event.getId();
        eventDTO.setDescription(event.getDescription());
        eventDTO.setOfficialWebsiteURL(event.getOfficialWebsiteURL() != null ? event.getOfficialWebsiteURL().toString() : null);
        eventDTO.setBaseURL(event.getBaseURL() != null ? event.getBaseURL().toString() : null);
        for (Map.Entry<Locale, URL> sailorsInfoWebsiteEntry : event.getSailorsInfoWebsiteURLs().entrySet()) {
            eventDTO.setSailorsInfoWebsiteURL(sailorsInfoWebsiteEntry.getKey() == null ? null : sailorsInfoWebsiteEntry
                    .getKey().toLanguageTag(), sailorsInfoWebsiteEntry.getValue().toExternalForm());
        }
        for (ImageDescriptor image : event.getImages()) {
            eventDTO.addImage(convertToImageDTO(image));
        }
        for (VideoDescriptor video : event.getVideos()) {
            eventDTO.addVideo(convertToVideoDTO(video));
        }
    }

    //READ
    protected List<ImageDescriptor> convertToImages(Iterable<ImageDTO> images) throws MalformedURLException {
        List<ImageDescriptor> eventImages = new ArrayList<ImageDescriptor>();
        for (ImageDTO image : images) {
            try {
                eventImages.add(convertToImage(image));
            } catch(Exception e) {
                // broken URLs are not being stored
            }
        }
        return eventImages;
    }

    //READ
    protected List<VideoDescriptor> convertToVideos(Iterable<VideoDTO> videos) throws MalformedURLException {
        List<VideoDescriptor> eventVideos = new ArrayList<VideoDescriptor>();
        for (VideoDTO video : videos) {
            try {
                eventVideos.add(convertToVideo(video));
            } catch(Exception e) {
                // broken URLs are not being stored
            }
        }
        return eventVideos;
    }

    //READ
    protected Map<Locale, URL> convertToLocalesAndUrls(Map<String, String> sailorsInfoWebsiteURLsByLocaleName) {
        Map<Locale, URL> eventURLs = new HashMap<>();
        for (Map.Entry<String, String> entry : sailorsInfoWebsiteURLsByLocaleName.entrySet()) {
            if (entry.getValue() != null) {
                try {
                    eventURLs.put(toLocale(entry.getKey()), new URL(entry.getValue()));
                } catch(Exception e) {
                    // broken URLs or Locales are not being stored
                }
            }
        }
        return eventURLs;
    }

    //READ
    private ImageDescriptor convertToImage(ImageDTO image) throws MalformedURLException {
        ImageDescriptor result = new ImageDescriptorImpl(new URL(image.getSourceRef()), new MillisecondsTimePoint(image.getCreatedAtDate()));
        result.setCopyright(image.getCopyright());
        result.setTitle(image.getTitle());
        result.setSubtitle(image.getSubtitle());
        result.setCopyright(image.getCopyright());
        result.setSize(image.getWidthInPx(), image.getHeightInPx());
        result.setLocale(toLocale(image.getLocale()));
        for (String tag : image.getTags()) {
            result.addTag(tag);
        }
        return result;
    }

    private VideoDescriptor convertToVideo(VideoDTO video) throws MalformedURLException {
        MimeType mimeType = video.getMimeType();
        if(mimeType == null || mimeType == MimeType.unknown) {
            mimeType = MediaUtils.detectMimeTypeFromUrl(video.getSourceRef());
        }
        VideoDescriptor result = new VideoDescriptorImpl(new URL(video.getSourceRef()), mimeType, new MillisecondsTimePoint(video.getCreatedAtDate()));
        result.setCopyright(video.getCopyright());
        result.setTitle(video.getTitle());
        result.setSubtitle(video.getSubtitle());
        result.setCopyright(video.getCopyright());
        result.setLengthInSeconds(video.getLengthInSeconds());
        if(video.getThumbnailRef() != null && !video.getThumbnailRef().isEmpty())
        result.setThumbnailURL(new URL(video.getThumbnailRef()));
        result.setLocale(toLocale(video.getLocale()));
        for (String tag : video.getTags()) {
            result.addTag(tag);
        }
        return result;
    }

    //READ
    private ImageDTO convertToImageDTO(ImageDescriptor image) {
        ImageDTO result = new ImageDTO(image.getURL().toString(), image.getCreatedAtDate() != null ? image.getCreatedAtDate().asDate() : null);
        result.setCopyright(image.getCopyright());
        result.setTitle(image.getTitle());
        result.setSubtitle(image.getSubtitle());
        result.setMimeType(image.getMimeType());
        result.setSizeInPx(image.getWidthInPx(), image.getHeightInPx());
        result.setLocale(toLocaleName(image.getLocale()));
        List<String> tags = new ArrayList<String>();
        for(String tag: image.getTags()) {
            tags.add(tag);
        }
        result.setTags(tags);
        return result;
    }

    //READ
    private VideoDTO convertToVideoDTO(VideoDescriptor video) {
        VideoDTO result = new VideoDTO(video.getURL().toString(), video.getMimeType(), 
                video.getCreatedAtDate() != null ? video.getCreatedAtDate().asDate() : null);
        result.setCopyright(video.getCopyright());
        result.setTitle(video.getTitle());
        result.setSubtitle(video.getSubtitle());
        result.setThumbnailRef(video.getThumbnailURL() != null ? video.getThumbnailURL().toString() : null);
        result.setLengthInSeconds(video.getLengthInSeconds());
        result.setLocale(toLocaleName(video.getLocale()));
        List<String> tags = new ArrayList<String>();
        for(String tag: video.getTags()) {
            tags.add(tag);
        }
        result.setTags(tags);
        return result;
    }
    
    //READ
    private Locale toLocale(String localeName) {
        if(localeName == null || localeName.isEmpty()) {
            return null;
        }
        return Locale.forLanguageTag(localeName);
    }
    
  //READ
    private String toLocaleName(Locale locale) {
        if(locale == null) {
            return null;
        }
        return locale.toString();
    }

    //READ
    protected EventDTO convertToEventDTO(Event event, boolean withStatisticalData) {
        EventDTO eventDTO = new EventDTO(event.getName());
        copyEventBaseFieldsToDTO(event, eventDTO);
        eventDTO.venue.setCourseAreas(new ArrayList<CourseAreaDTO>());
        for (CourseArea courseArea : event.getVenue().getCourseAreas()) {
            CourseAreaDTO courseAreaDTO = convertToCourseAreaDTO(courseArea);
            eventDTO.venue.getCourseAreas().add(courseAreaDTO);
        }
        for (LeaderboardGroup lg : event.getLeaderboardGroups()) {
            eventDTO.addLeaderboardGroup(convertToLeaderboardGroupDTO(lg, /* withGeoLocationData */ false, withStatisticalData));
        }
        eventDTO.setWindFinderReviewedSpotsCollection(event.getWindFinderReviewedSpotsCollectionIds());
        final WindFinderTrackerFactory windFinderTrackerFactory = windFinderTrackerFactoryServiceTracker.getService();
        if (windFinderTrackerFactory != null) {
            eventDTO.setAllWindFinderSpotsUsedByEvent(new EventWindFinderUtil().getWindFinderSpotsToConsider(event,
                    windFinderTrackerFactory, /* useCachedSpotsForTrackedRaces */ false));
        }
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), eventDTO, event.getIdentifier());
        return eventDTO;
    }

    //READ
    private CourseAreaDTO convertToCourseAreaDTO(CourseArea courseArea) {
        CourseAreaDTO courseAreaDTO = new CourseAreaDTO(courseArea.getName());
        courseAreaDTO.id = courseArea.getId();
        return courseAreaDTO;
    }
    
    /** for backward compatibility with the regatta overview */
    @Override
    //READ
    public List<RaceGroupDTO> getRegattaStructureForEvent(UUID eventId) {
        List<RaceGroupDTO> raceGroups = new ArrayList<RaceGroupDTO>();
        Event event = getService().getEvent(eventId);
        getSecurityService().checkCurrentUserReadPermission(event);
        Map<Leaderboard, LeaderboardGroup> leaderboardWithLeaderboardGroups = new HashMap<Leaderboard, LeaderboardGroup>();
        for(LeaderboardGroup leaderboardGroup: event.getLeaderboardGroups()) {
            for(Leaderboard leaderboard: leaderboardGroup.getLeaderboards()) {
                if (getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                    leaderboardWithLeaderboardGroups.put(leaderboard, leaderboardGroup);
                }
            }
        }
        if (event != null) {
            final Set<Leaderboard> leaderboardsAlreadyAddedAsRaceGroup = new HashSet<>();
            for (CourseArea courseArea : event.getVenue().getCourseAreas()) {
                for (Leaderboard leaderboard : getService().getLeaderboards().values()) {
                    if (!leaderboardsAlreadyAddedAsRaceGroup.contains(leaderboard)
                    && getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                        if (Util.contains(leaderboard.getCourseAreas(), courseArea)) {
                            RaceGroupDTO raceGroup = new RaceGroupDTO(leaderboard.getName());
                            raceGroup.displayName = getRegattaNameFromLeaderboard(leaderboard);
                            if (leaderboardWithLeaderboardGroups.containsKey(leaderboard)) {
                                raceGroup.leaderboardGroupName = leaderboardWithLeaderboardGroups.get(leaderboard).getName();
                            }
                            if (leaderboard instanceof RegattaLeaderboard) {
                                RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                                for (Series series : regattaLeaderboard.getRegatta().getSeries()) {
                                    RaceGroupSeriesDTO seriesDTO = new RaceGroupSeriesDTO(series.getName());
                                    raceGroup.getSeries().add(seriesDTO);
                                    for (Fleet fleet : series.getFleets()) {
                                        FleetDTO fleetDTO = new FleetDTO(fleet.getName(), fleet.getOrdering(),
                                                fleet.getColor());
                                        seriesDTO.getFleets().add(fleetDTO);
                                    }
                                    seriesDTO.getRaceColumns().addAll(convertToRaceColumnDTOs(series.getRaceColumns()));
                                }
                            } else {
                                RaceGroupSeriesDTO seriesDTO = new RaceGroupSeriesDTO(
                                        LeaderboardNameConstants.DEFAULT_SERIES_NAME);
                                raceGroup.getSeries().add(seriesDTO);
                                FleetDTO fleetDTO = new FleetDTO(LeaderboardNameConstants.DEFAULT_FLEET_NAME, 0, null);
                                seriesDTO.getFleets().add(fleetDTO);
                                seriesDTO.getRaceColumns()
                                        .addAll(convertToRaceColumnDTOs(leaderboard.getRaceColumns()));
                            }
                            raceGroups.add(raceGroup);
                            leaderboardsAlreadyAddedAsRaceGroup.add(leaderboard);
                        }
                    }
                }
            }
        }
        return raceGroups;
    }

    /**
     * The name of the regatta to be shown on the regatta overview webpage is retrieved from the name of the {@link Leaderboard}. Since regattas are
     * not always represented by a {@link Regatta} object in the Sailing Suite but need to be shown on the regatta overview page, the leaderboard is
     * used as the representative of the sailing regatta. When a display name is set for a leaderboard, this name is favored against the (mostly technical)
     * regatta name as the display name represents the publicly visible name of the regatta. 
     * <br>
     * When the leaderboard is a {@link RegattaLeaderboard} the name of the {@link Regatta} is used, otherwise the leaderboard 
     * is a {@link FlexibleLeaderboard} and it's name is used as the last option.
     * @param leaderboard The {@link Leaderboard} from which the name is be retrieved
     * @return the name of the regatta to be shown on the regatta overview page
     */
    //READ
    private String getRegattaNameFromLeaderboard(Leaderboard leaderboard) {
        String regattaName;
        if (leaderboard.getDisplayName() != null && !leaderboard.getDisplayName().isEmpty()) {
            regattaName = leaderboard.getDisplayName();
        } else {
            if (leaderboard instanceof RegattaLeaderboard) {
                RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                regattaName = regattaLeaderboard.getRegatta().getName();
            } else {
                regattaName = leaderboard.getName();
            }
        }
        return regattaName;
    }

    @Override
    //READ
    public RegattaScoreCorrectionDTO getScoreCorrections(String scoreCorrectionProviderName, String eventName,
            String boatClassName, Date timePointWhenResultPublished) throws Exception {
        RegattaScoreCorrectionDTO result = null;
        for (ScoreCorrectionProvider scp : getAllScoreCorrectionProviders()) {
            if (scp.getName().equals(scoreCorrectionProviderName)) {
                final RegattaScoreCorrections scoreCorrections = scp.getScoreCorrections(eventName, boatClassName,
                        new MillisecondsTimePoint(timePointWhenResultPublished));
                final RegattaDTO regatta = getRegattaByName(scoreCorrections.getRegattaName());
                getSecurityService().checkCurrentUserReadPermission(regatta);
                result = createScoreCorrection(scoreCorrections);
                break;
            }
        }
        return result;
    }

    //READ
    private RegattaScoreCorrectionDTO createScoreCorrection(RegattaScoreCorrections scoreCorrections) {
        // Key is the race name or number as String; values are maps whose key is the sailID.
        LinkedHashMap<String, Map<String, ScoreCorrectionEntryDTO>> map = new LinkedHashMap<String, Map<String, ScoreCorrectionEntryDTO>>();
        for (ScoreCorrectionsForRace sc4r : scoreCorrections.getScoreCorrectionsForRaces()) {
            Map<String, ScoreCorrectionEntryDTO> entryMap = new HashMap<String, RegattaScoreCorrectionDTO.ScoreCorrectionEntryDTO>();
            for (String sailID : sc4r.getSailIDs()) {
                entryMap.put(sailID, createScoreCorrectionEntryDTO(sc4r.getScoreCorrectionForCompetitor(sailID)));
            }
            map.put(sc4r.getRaceNameOrNumber(), entryMap);
        }
        return new RegattaScoreCorrectionDTO(scoreCorrections.getProvider().getName(), map);
    }

    //READ
    private ScoreCorrectionEntryDTO createScoreCorrectionEntryDTO(
            ScoreCorrectionForCompetitorInRace scoreCorrectionForCompetitor) {
        return new ScoreCorrectionEntryDTO(scoreCorrectionForCompetitor.getPoints(),
                scoreCorrectionForCompetitor.isDiscarded(), scoreCorrectionForCompetitor.getMaxPointsReason());
    }
    
    @Override
    //READ
    public List<Pair<String, String>> getUrlResultProviderNamesAndOptionalSampleURL() {
        List<Pair<String, String>> result = new ArrayList<>();
        // In case a user may not read any result import URL, just an empty result is returned because
        // selecting a type will never show an entry.
        if (getSecurityService()
                .hasCurrentUserAnyPermission(SecuredDomainType.RESULT_IMPORT_URL.getPermission(DefaultActions.READ))) {
            for (ScoreCorrectionProvider scp : getAllScoreCorrectionProviders()) {
                if (scp instanceof ResultUrlProvider) {
                    result.add(new Pair<>(scp.getName(), ((ResultUrlProvider) scp).getOptionalSampleURL()));
                }
            }
        }
        return result;
    }

    //READ
    protected ResultUrlProvider getUrlBasedScoreCorrectionProvider(String resultProviderName) {
        ResultUrlProvider result = null;
        for (ScoreCorrectionProvider scp : getAllScoreCorrectionProviders()) {
            if (scp instanceof ResultUrlProvider && scp.getName().equals(resultProviderName)) {
                result = (ResultUrlProvider) scp;
                break;
            }
        }
        return result;
    }

    //READ
    protected ServerInfoDTO getServerInfo() {
        ServerInfoDTO result = new ServerInfoDTO(ServerInfo.getName(), ServerInfo.getBuildVersion(), ServerInfo.getManageEventsBaseUrl());
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), result, result.getIdentifier());
        return result;
    }

    @Override
    //READ
    public ServerConfigurationDTO getServerConfiguration() {
        SailingServerConfiguration sailingServerConfiguration = getService().getSailingServerConfiguration();
        UserGroup serverTenant = getSecurityService().getServerGroup();
        StrippedUserGroupDTO serverTenantDTO = new SecurityDTOFactory()
                .createStrippedUserGroupDTOFromUserGroup(serverTenant, new HashMap<>());
        ServerConfigurationDTO result = new ServerConfigurationDTO(sailingServerConfiguration.isStandaloneServer(),
                isPublicServer(), isSelfServiceServer(), serverTenantDTO);
        return result;
    }

    @Override
    //??
    public void updateServerConfiguration(ServerConfigurationDTO serverConfiguration) {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_LOCAL_SERVER);
        getService().apply(new UpdateServerConfiguration(
                new SailingServerConfigurationImpl(serverConfiguration.isStandaloneServer())));
        if (serverConfiguration.isSelfService() != null) {
            final boolean isCurrentlySelfService = isSelfServiceServer();
            final boolean shouldBeSelfService = serverConfiguration.isSelfService();
            if (isCurrentlySelfService != shouldBeSelfService) {
                SecurityUtils.getSubject().checkPermission(getServerInfo().getIdentifier().getStringPermission(DefaultActions.CHANGE_ACL));
                if (shouldBeSelfService) {
                    getSecurityService().addToAccessControlList(getServerInfo().getIdentifier(), null, ServerActions.CREATE_OBJECT.name());
                } else {
                    getSecurityService().removeFromAccessControlList(getServerInfo().getIdentifier(), null, ServerActions.CREATE_OBJECT.name());
                }
            }
        }
        if (serverConfiguration.isPublic() != null) {
            final RoleDefinition viewerRole = getSecurityService()
                    .getRoleDefinition(SailingViewerRole.getInstance().getId());
            final UserGroup serverGroup = getSecurityService().getServerGroup();
            if (viewerRole != null && serverGroup != null) {
                final boolean isCurrentlyPublic = Boolean.TRUE.equals(serverGroup.getRoleAssociation(viewerRole));
                final boolean shouldBePublic = serverConfiguration.isPublic();
                if (isCurrentlyPublic != shouldBePublic) {
                    // value changed
                    if (getSecurityService().hasCurrentUserUpdatePermission(serverGroup)
                            && getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(
                                    viewerRole, new Ownership(null, serverGroup))) {
                        if (serverConfiguration.isPublic()) {
                            getSecurityService().putRoleDefinitionToUserGroup(serverGroup, viewerRole, true);
                        } else {
                            getSecurityService().removeRoleDefintionFromUserGroup(serverGroup, viewerRole);
                        }
                    } else {
                        throw new AuthorizationException("No permission to make the server public");
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        SailingViewerRole.getInstance().getName() + " role or default server tenant does not exist");
            }
        }
    }

    @Override
    //??
    public List<RemoteSailingServerReferenceDTO> getRemoteSailingServerReferences() {
        List<RemoteSailingServerReferenceDTO> result = new ArrayList<RemoteSailingServerReferenceDTO>();
        for (Entry<RemoteSailingServerReference, com.sap.sse.common.Util.Pair<Iterable<EventBase>, Exception>> remoteSailingServerRefAndItsCachedEvent :
                    getService().getPublicEventsOfAllSailingServers().entrySet()) {
            RemoteSailingServerReferenceDTO dto = createRemoteSailingServerReferenceDTO(
                    remoteSailingServerRefAndItsCachedEvent.getKey(),
                    remoteSailingServerRefAndItsCachedEvent.getValue());
            result.add(dto);
        }
        return result;
    }

    @Override
    //??
    public void removeSailingServers(Set<String> namesOfSailingServersToRemove) throws Exception {
        getSecurityService().checkCurrentUserUpdatePermission(getServerInfo());
        for (String serverName : namesOfSailingServersToRemove) {
            getService().apply(new RemoveRemoteSailingServerReference(serverName));
        }
    }

    @Override
    //??
    public RemoteSailingServerReferenceDTO addRemoteSailingServerReference(RemoteSailingServerReferenceDTO sailingServer) throws MalformedURLException {
        getSecurityService().checkCurrentUserUpdatePermission(getServerInfo());
        final String expandedURL;
        if (sailingServer.getUrl().contains("//")) {
            expandedURL = sailingServer.getUrl();
        } else {
            expandedURL = "https://" + sailingServer.getUrl();
        }
        URL serverURL = new URL(expandedURL);
        RemoteSailingServerReference serverRef = getService().apply(new AddRemoteSailingServerReference(sailingServer.getName(), serverURL));
        com.sap.sse.common.Util.Pair<Iterable<EventBase>, Exception> eventsOrException = getService().updateRemoteServerEventCacheSynchronously(serverRef);
        return createRemoteSailingServerReferenceDTO(serverRef, eventsOrException);
        
    }

    @Override
    //READ
    public List<UrlDTO> getResultImportUrls(String resultProviderName) {
        final List<UrlDTO> result = new ArrayList<>();
            SecurityService securityService = getSecurityService();
        Iterable<URL> allUrlsReadableBySubject = getService().getResultImportUrls(resultProviderName);
            for (URL url : allUrlsReadableBySubject) {
                QualifiedObjectIdentifier objId = SecuredDomainType.RESULT_IMPORT_URL.getQualifiedObjectIdentifier(
                    new TypeRelativeObjectIdentifier(resultProviderName, url.toString()));
                UrlDTO urlDTO = new UrlDTO(url.toString());
                SecurityDTOUtil.addSecurityInformation(securityService, urlDTO, objId);
                result.add(urlDTO);
            }
        return result;
    }

    @Override
    //READ
    public String validateResultImportUrl(String resultProviderName, UrlDTO urlDTO) {
        if (urlDTO == null || urlDTO.getUrl() == null || urlDTO.getUrl().isEmpty()) {
            return serverStringMessages.get(getClientLocale(), "pleaseEnterNonEmptyUrl");
        }
        Optional<ResultUrlProvider> resultUrlProvider = getService()
                .getUrlBasedScoreCorrectionProvider(resultProviderName);
        if (!resultUrlProvider.isPresent()) {
            return serverStringMessages.get(getClientLocale(), "scoreCorrectionProviderNotFound");
        }
        String errorMessage = null;
        try {
            resultUrlProvider.get().resolveUrl(urlDTO.getUrl());
        } catch (MalformedURLException e) {
            errorMessage = e.getMessage();
        }
        return errorMessage;
    }

    @Override
    //READ
    public List<String> getOverallLeaderboardNamesContaining(String leaderboardName) {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard == null) {
            throw new IllegalArgumentException("Couldn't find leaderboard named "+leaderboardName);
        }
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, Leaderboard> leaderboardEntry : getService().getLeaderboards().entrySet()) {
            if (leaderboardEntry.getValue() instanceof MetaLeaderboard) {
                MetaLeaderboard metaLeaderboard = (MetaLeaderboard) leaderboardEntry.getValue();
                if (Util.contains(metaLeaderboard.getLeaderboards(), leaderboard)) {
                    if (getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                        result.add(leaderboardEntry.getKey());
                    }
                }
            }
        }
        return result;
    }

    @Override
    //READ
    public List<SwissTimingArchiveConfigurationWithSecurityDTO> getPreviousSwissTimingArchiveConfigurations() {
        Iterable<SwissTimingArchiveConfiguration> configs = swissTimingAdapterPersistence
                .getSwissTimingArchiveConfigurations();
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(configs,
                stArchiveConfig -> {
                    SwissTimingArchiveConfigurationWithSecurityDTO config = new SwissTimingArchiveConfigurationWithSecurityDTO(
                            stArchiveConfig.getJsonURL(), stArchiveConfig.getCreatorName());
                    SecurityDTOUtil.addSecurityInformation(getSecurityService(), config, config.getIdentifier());
                    return config;
                });
    }

    //READ
    protected com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory() {
        return baseDomainFactory;
    }

    private List<RegattaOverviewEntryDTO> getRaceStateEntriesForLeaderboard(String leaderboardName,
            boolean showOnlyCurrentlyRunningRaces, boolean showOnlyRacesOfSameDay, Duration clientTimeZoneOffset,
            final List<String> visibleRegattas) throws NoWindException, InterruptedException, ExecutionException {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        return getRaceStateEntriesForLeaderboard(leaderboard, showOnlyCurrentlyRunningRaces, showOnlyRacesOfSameDay,
                clientTimeZoneOffset, visibleRegattas);
    }

    /**
     * The client's day starts at <code>00:00:00Z - clientTimeZoneOffset</code> and ends at <code>23:59:59Z - clientTimeZoneOffset</code>.
     */
    //READ
    private List<RegattaOverviewEntryDTO> getRaceStateEntriesForLeaderboard(Leaderboard leaderboard,
            boolean showOnlyCurrentlyRunningRaces, boolean showOnlyRacesOfSameDay, Duration clientTimeZoneOffset, final List<String> visibleRegattas)
            throws NoWindException, InterruptedException, ExecutionException {
        List<RegattaOverviewEntryDTO> result = new ArrayList<RegattaOverviewEntryDTO>();
        Calendar dayToCheck = Calendar.getInstance();
        dayToCheck.setTime(new Date());
        if (leaderboard != null) {
            if (visibleRegattas != null && !visibleRegattas.contains(leaderboard.getName())) {
                return result;
            } 
            String regattaName = getRegattaNameFromLeaderboard(leaderboard);
            if (leaderboard instanceof RegattaLeaderboard) {
                RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                Regatta regatta = regattaLeaderboard.getRegatta();
                BoatClass boatClass = regatta.getBoatClass();
                Distance buyZoneRadius = RegattaUtil.getCalculatedRegattaBuoyZoneRadius(regatta, boatClass);
                for (Series series : regatta.getSeries()) {
                    Map<String, List<RegattaOverviewEntryDTO>> entriesPerFleet = new HashMap<String, List<RegattaOverviewEntryDTO>>();
                    for (RaceColumn raceColumn : series.getRaceColumns()) {
                        getRegattaOverviewEntries(showOnlyRacesOfSameDay, clientTimeZoneOffset, dayToCheck,
                                leaderboard, boatClass.getName(), regattaName, buyZoneRadius, series.getName(),
                                raceColumn, entriesPerFleet);
                    }
                    result.addAll(getRegattaOverviewEntriesToBeShown(showOnlyCurrentlyRunningRaces, entriesPerFleet));
                }

            } else if (leaderboard instanceof FlexibleLeaderboard) {
                BoatClass boatClass = null;
                for (TrackedRace trackedRace : leaderboard.getTrackedRaces()) {
                    boatClass = trackedRace.getRace().getBoatClass();
                    break;
                }
                Distance buyZoneRadius = RegattaUtil.getCalculatedRegattaBuoyZoneRadius(null, boatClass);
                Map<String, List<RegattaOverviewEntryDTO>> entriesPerFleet = new HashMap<String, List<RegattaOverviewEntryDTO>>();
                for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                    getRegattaOverviewEntries(showOnlyRacesOfSameDay, clientTimeZoneOffset, dayToCheck, leaderboard,
                            boatClass == null ? "" : boatClass.getName(), regattaName, buyZoneRadius, LeaderboardNameConstants.DEFAULT_SERIES_NAME,
                            raceColumn, entriesPerFleet);
                }
                result.addAll(getRegattaOverviewEntriesToBeShown(showOnlyCurrentlyRunningRaces, entriesPerFleet));
            }
        }
        return result;
    }
    
    //READ
    private SeriesParameters getSeriesParameters(SeriesDTO seriesDTO) {
        SeriesParameters series = new SeriesParameters(false, false, false, null, seriesDTO.getMaximumNumberOfDiscards());
        series.setFirstColumnIsNonDiscardableCarryForward(seriesDTO.isFirstColumnIsNonDiscardableCarryForward());
        series.setHasSplitFleetContiguousScoring(seriesDTO.hasSplitFleetContiguousScoring());
        series.setStartswithZeroScore(seriesDTO.isStartsWithZeroScore());
        series.setDiscardingThresholds(seriesDTO.getDiscardThresholds());
        return series;
    }
    
    //READ
    protected LinkedHashMap<String, SeriesCreationParametersDTO> getSeriesCreationParameters(RegattaDTO regattaDTO) {
        LinkedHashMap<String, SeriesCreationParametersDTO> seriesCreationParams = new LinkedHashMap<String, SeriesCreationParametersDTO>();
            for (SeriesDTO series : regattaDTO.series){
                SeriesParameters seriesParameters = getSeriesParameters(series);
                seriesCreationParams.put(series.getName(), new SeriesCreationParametersDTO(series.getFleets(),
                false, true, seriesParameters.isStartswithZeroScore(), seriesParameters.isFirstColumnIsNonDiscardableCarryForward(),
                        seriesParameters.getDiscardingThresholds(), seriesParameters.isHasSplitFleetContiguousScoring(),
                        seriesParameters.getMaximumNumberOfDiscards()));
            }
        return seriesCreationParams;
    }

    @Override
    //READ
    public Iterable<RegattaDTO> getRegattas(String manage2SailJsonUrl) { 
        StructureImporter structureImporter = new StructureImporter(new SetRacenumberFromSeries(), baseDomainFactory);
        Iterable<RegattaJSON> parsedEvent = structureImporter.parseEvent(manage2SailJsonUrl);
        List<RegattaDTO> regattaDTOs = new ArrayList<RegattaDTO>();
        Iterable<Regatta> regattas = structureImporter.getRegattas(parsedEvent);
        for (Regatta regatta : regattas) {
            regattaDTOs.add(convertToRegattaDTO(regatta));
        }
        return regattaDTOs;
    }

    private Triple<String, String, String> getLeaderboardSlotKey(RegattaOverviewEntryDTO entry) {
        return new Triple<>(entry.leaderboardName, entry.raceInfo.raceName, entry.raceInfo.fleetName);
    }

    @Override
    //READ
    public List<RegattaOverviewEntryDTO> getRaceStateEntriesForRaceGroup(UUID eventId, List<UUID> visibleCourseAreaIds,
            List<String> visibleRegattas, boolean showOnlyCurrentlyRunningRaces, boolean showOnlyRacesOfSameDay, Duration clientTimeZoneOffset)
            throws NoWindException, InterruptedException, ExecutionException {
        Calendar dayToCheck = Calendar.getInstance();
        dayToCheck.setTime(new Date());
        Event event = getService().getEvent(eventId);
        getSecurityService().checkCurrentUserReadPermission(event);
        final Map<Triple<String, String, String>, RegattaOverviewEntryDTO> entriesByRaceIdentifier = new HashMap<>();
        if (event != null) {
            for (CourseArea courseArea : event.getVenue().getCourseAreas()) {
                if (visibleCourseAreaIds.contains(courseArea.getId())) {
                    for (Leaderboard leaderboard : getService().getLeaderboards().values()) {
                        if (getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                            // leaderboard's course areas must intersect with those of event; if so, add those
                            // race entries that either have no course area explicitly defined or otherwise have
                            // a course area that is part of the visibleCourseAreaIds
                            if (Util.contains(leaderboard.getCourseAreas(), courseArea)) {
                                for (final RegattaOverviewEntryDTO entry : getRaceStateEntriesForLeaderboard(leaderboard.getName(),
                                            showOnlyCurrentlyRunningRaces, showOnlyRacesOfSameDay, clientTimeZoneOffset,
                                            visibleRegattas)) {
                                    // use "leaderboard slot key" consisting of leaderboard name / race column name / fleet name
                                    // because the raceInfo.raceIdentifier could be null as no TrackedRace has to be attached
                                    if (!entriesByRaceIdentifier.containsKey(getLeaderboardSlotKey(entry)) &&
                                            (entry.courseAreaIdAsString == null || visibleCourseAreaIds.contains(UUID.fromString(
                                                    entry.courseAreaIdAsString)))) {
                                        entriesByRaceIdentifier.put(getLeaderboardSlotKey(entry), entry);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(entriesByRaceIdentifier.values());
    }

    /**
     * The client's day starts at <code>00:00:00Z - clientTimeZoneOffset</code> and ends at <code>23:59:59Z - clientTimeZoneOffset</code>.
     */
    //READ
    private void getRegattaOverviewEntries(boolean showOnlyRacesOfSameDay, Duration clientTimeZoneOffset,
            Calendar dayToCheck, Leaderboard leaderboard, String boatClassName, String regattaName,
            Distance buyZoneRadius, String seriesName, RaceColumn raceColumn, Map<String, List<RegattaOverviewEntryDTO>> entriesPerFleet) {
        if (!raceColumn.isCarryForward()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                RegattaOverviewEntryDTO entry = createRegattaOverviewEntryDTO(leaderboard,
                        boatClassName, regattaName, buyZoneRadius, seriesName, raceColumn, fleet, showOnlyRacesOfSameDay, 
                        clientTimeZoneOffset, dayToCheck);
                if (entry != null) {
                    addRegattaOverviewEntryToEntriesPerFleet(entriesPerFleet, fleet, entry);
                }
            }
        }
    }

    //READ
    private List<RegattaOverviewEntryDTO> getRegattaOverviewEntriesToBeShown(boolean showOnlyCurrentlyRunningRaces,
            Map<String, List<RegattaOverviewEntryDTO>> entriesPerFleet) {
        List<RegattaOverviewEntryDTO> result = new ArrayList<RegattaOverviewEntryDTO>();
        for (List<RegattaOverviewEntryDTO> entryList : entriesPerFleet.values()) {
            result.addAll(entryList);
            if (showOnlyCurrentlyRunningRaces) {
                List<RegattaOverviewEntryDTO> finishedEntries = new ArrayList<RegattaOverviewEntryDTO>();
                for (RegattaOverviewEntryDTO entry : entryList) {
                    if (!RaceLogRaceStatus.isActive(entry.raceInfo.lastStatus)) {
                        if (entry.raceInfo.lastStatus.equals(RaceLogRaceStatus.FINISHED)) {
                            finishedEntries.add(entry);
                        } else if (entry.raceInfo.lastStatus.equals(RaceLogRaceStatus.UNSCHEDULED)) {
                            //don't filter when the race is unscheduled and aborted before
                            if (!entry.raceInfo.isRaceAbortedInPassBefore) {
                                result.remove(entry);
                            }
                            
                        }
                    }
                }
                if (!finishedEntries.isEmpty()) {
                    //keep the last finished race in the list to be shown
                    int indexOfLastElement = finishedEntries.size() - 1;
                    finishedEntries.remove(indexOfLastElement);
                    //... and remove all other finished races
                    result.removeAll(finishedEntries);
                }
            }
        }
        return result;
    }

    //READ
    private void addRegattaOverviewEntryToEntriesPerFleet(Map<String, List<RegattaOverviewEntryDTO>> entriesPerFleet,
            Fleet fleet, RegattaOverviewEntryDTO entry) {
        if (!entriesPerFleet.containsKey(fleet.getName())) {
           entriesPerFleet.put(fleet.getName(), new ArrayList<RegattaOverviewEntryDTO>()); 
        }
        entriesPerFleet.get(fleet.getName()).add(entry);
    }

    /**
     * The client's day starts at <code>00:00:00Z - clientTimeZoneOffset</code> and ends at <code>23:59:59Z - clientTimeZoneOffset</code>.
     */
    //READ
    private RegattaOverviewEntryDTO createRegattaOverviewEntryDTO(Leaderboard leaderboard, String boatClassName,
            String regattaName, Distance buyZoneRadius, String seriesName, RaceColumn raceColumn, Fleet fleet,
            boolean showOnlyRacesOfSameDay, Duration clientTimeZoneOffset, Calendar dayToCheck) {
        RegattaOverviewEntryDTO entry = new RegattaOverviewEntryDTO();
        final RaceLog raceLog = raceColumn.getRaceLog(fleet);
        final ReadonlyRaceState state;
        if (raceLog != null) {
            state = ReadonlyRaceStateImpl.getOrCreate(getService(), raceLog);
        } else {
            state = null;
        }
        // try to find course area information in the race log which may tell on which course are
        // the latest / valid start attempt / pass has happened; if this information cannot be found,
        // e.g., because the race log is empty, or it is a legacy race log that doesn't provide this
        // information, and the leaderboard has exactly one course areas assigned, that one is used.
        // If the leaderboard has multiple course areas assigned we assume that the choice must come
        // from a race log start time event or else it hasn't been decided yet where that race will take place.
        final UUID courseAreaIdFromRaceLog;
        if (state != null && (courseAreaIdFromRaceLog=state.getCourseAreaId()) != null) {
            // no race log; use default course area in any case:
            final CourseArea courseAreaFromRaceLog = getBaseDomainFactory().getExistingCourseAreaById(courseAreaIdFromRaceLog);
            entry.courseAreaName = courseAreaFromRaceLog.getName();
            entry.courseAreaIdAsString = courseAreaFromRaceLog.getId().toString();
        } else if (Util.size(leaderboard.getCourseAreas()) == 1) {
            final CourseArea defaultCourseArea = leaderboard.getCourseAreas().iterator().next();
            entry.courseAreaName = defaultCourseArea.getName();
            entry.courseAreaIdAsString = defaultCourseArea.getId().toString();
        }
        entry.boatClassName = boatClassName;
        entry.regattaDisplayName = regattaName;
        entry.leaderboardName = leaderboard.getName();
        entry.raceInfo = createRaceInfoDTO(seriesName, raceColumn, fleet, raceLog, state);
        entry.currentServerTime = new Date();
        entry.buyZoneRadius = buyZoneRadius;
        if (showOnlyRacesOfSameDay) {
            if (!RaceStateOfSameDayHelper.isRaceStateOfSameDay(entry.raceInfo.startTime, entry.raceInfo.finishedTime,
                    entry.raceInfo.abortingTimeInPassBefore, dayToCheck, clientTimeZoneOffset)) {
                entry = null;
            }
        }
        return entry;
    }

    @Override
    //READ
    public void stopReplicatingFromMaster() {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.START_REPLICATION);
        try {
            getReplicationService().stopToReplicateFromMaster();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception trying to stop replicating from master", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    //??
    public void stopAllReplicas() {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.REPLICATE);
        try {
            getReplicationService().stopAllReplicas();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception trying to stop all replicas from receiving updates from this master", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    //??
    public void stopSingleReplicaInstance(String identifier) {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.REPLICATE);
        UUID uuid = UUID.fromString(identifier);
        try {
            getReplicationService().unregisterReplica(uuid);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception trying to unregister replica with UUID "+uuid, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reloadRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet)
            throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        getService().reloadRaceLog(leaderboardName, raceColumnDTO.getName(), fleet.getName());
    }

    @Override
  //READ
    public RaceLogDTO getRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet) {
        Leaderboard lb = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(lb);
        if (lb instanceof RegattaLeaderboard) {
            getSecurityService().checkCurrentUserReadPermission(((RegattaLeaderboard) lb).getRegatta());
        }
        RaceLogDTO result = null;
        RaceLog raceLog = getService().getRaceLog(leaderboardName, raceColumnDTO.getName(), fleet.getName());
        if(raceLog != null) {
            List<RaceLogEventDTO> entries = new ArrayList<RaceLogEventDTO>();
            result = new RaceLogDTO(leaderboardName, raceColumnDTO.getName(), fleet.getName(), raceLog.getCurrentPassId(), entries);
            raceLog.lockForRead();
            try {
                for(RaceLogEvent raceLogEvent: raceLog.getRawFixes()) {
                    RaceLogEventDTO entry = new RaceLogEventDTO(raceLogEvent.getPassId(), 
                            raceLogEvent.getAuthor().getName(), raceLogEvent.getAuthor().getPriority(), 
                            raceLogEvent.getCreatedAt() != null ? raceLogEvent.getCreatedAt().asDate() : null,
                            raceLogEvent.getLogicalTimePoint() != null ? raceLogEvent.getLogicalTimePoint().asDate() : null,
                            raceLogEvent.getClass().getSimpleName(), raceLogEvent.getShortInfo());
                    entries.add(entry);
                }
            } finally {
                raceLog.unlockAfterRead();
            }
        }
        return result;
    }

    @Override
  //READ
    public RegattaLogDTO getRegattaLog(String leaderboardName) throws DoesNotHaveRegattaLogException {
        RegattaLogDTO result = null;
        Leaderboard l = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(l);

        RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        if (regattaLog != null) {
            List<RegattaLogEventDTO> entries = new ArrayList<>();
            result = new RegattaLogDTO(leaderboardName, entries);
            regattaLog.lockForRead();
            try {
                for(RegattaLogEvent raceLogEvent: regattaLog.getRawFixes()) {
                    RegattaLogEventDTO entry = new RegattaLogEventDTO( 
                            raceLogEvent.getAuthor().getName(), raceLogEvent.getAuthor().getPriority(), 
                            raceLogEvent.getCreatedAt() != null ? raceLogEvent.getCreatedAt().asDate() : null,
                            raceLogEvent.getLogicalTimePoint() != null ? raceLogEvent.getLogicalTimePoint().asDate() : null,
                            raceLogEvent.getClass().getSimpleName(), raceLogEvent.getShortInfo());
                    entries.add(entry);
                }
            } finally {
                regattaLog.unlockAfterRead();
            }
        }
        return result;
    }

    @Override
    // READ
    public Map<String, String> getLeaderboardGroupNamesAndIdsAsStringsFromRemoteServer(String url, String username, String password) {
        String token = RemoteServerUtil.resolveBearerTokenForRemoteServer(url, username, password);
        final String path = "/sailingserver/api/v1/leaderboardgroups/identifiable";
        final String query = null;
        URL serverAddress = null;
        InputStream inputStream = null;
        URLConnection connection = null;
        try {
            URL base = RemoteServerUtil.createBaseUrl(url);
            serverAddress = RemoteServerUtil.createRemoteServerUrl(base, path, query);
            connection = HttpUrlConnectionHelper.redirectConnectionWithBearerToken(serverAddress, Duration.ONE_MINUTE,
                    token);
            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));
            final org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
            final org.json.simple.JSONArray array = (org.json.simple.JSONArray) parser.parse(in);
            final Map<String, String> leaderboardGroupsMap = new LinkedHashMap<>();
            final Iterator<Object> iterator = array.iterator();
            while (iterator.hasNext()) {
                JSONObject next = (JSONObject) iterator.next();
                leaderboardGroupsMap.put((String) next.get(LeaderboardGroupConstants.ID), (String) next.get(LeaderboardGroupConstants.NAME));
            }
            List<Map.Entry<String, String>> entries = new ArrayList<>(leaderboardGroupsMap.entrySet());
            leaderboardGroupsMap.clear();
            entries.stream().sorted(Comparator.comparing(Map.Entry::getValue, Comparator.naturalOrder()))
                    .forEachOrdered(e -> leaderboardGroupsMap.put(e.getKey(), e.getValue()));
            return leaderboardGroupsMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // close the connection
            if (connection != null && connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Exception while trying to close the remote stream for leaderboard groups", e);
            }
        }
    }

    @Override
  //READ
    public Iterable<CompetitorDTO> getCompetitors(boolean filterCompetitorsWithBoat,
            boolean filterCompetitorsWithoutBoat) {
        CompetitorAndBoatStore competitorStore = getService().getBaseDomainFactory().getCompetitorAndBoatStore();
        final HasPermissions.Action[] requiredActionsForRead = SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS;
        final Iterable<? extends Competitor> filteredCompetitors;
        if (filterCompetitorsWithBoat == false && filterCompetitorsWithoutBoat == false) {
            filteredCompetitors = competitorStore.getAllCompetitors();
        } else if (filterCompetitorsWithBoat == true && filterCompetitorsWithoutBoat == false) {
            filteredCompetitors = competitorStore.getCompetitorsWithoutBoat();
        } else if (filterCompetitorsWithBoat == false && filterCompetitorsWithoutBoat == true) {
            filteredCompetitors = competitorStore.getCompetitorsWithBoat();
        } else {
            filteredCompetitors = Collections.emptyList();
        }
        return getSecurityService().mapAndFilterByAnyExplicitPermissionForCurrentUser(
                    SecuredDomainType.COMPETITOR,
                requiredActionsForRead, filteredCompetitors,
                    this::convertToCompetitorDTO);
        }

    @Override
  //READ
    public Iterable<CompetitorDTO> getCompetitorsOfLeaderboard(String leaderboardName) {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        return convertToCompetitorDTOs(leaderboard.getAllCompetitors());
    }

    @Override
  //READ
    public Map<? extends CompetitorDTO, BoatDTO> getCompetitorsAndBoatsOfRace(String leaderboardName,
            String raceColumnName, String fleetName) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        Map<Competitor, Boat> competitorsAndBoats = getService().getCompetitorToBoatMappingsForRace(leaderboardName, raceColumnName, fleetName);
        return baseDomainFactory.convertToCompetitorAndBoatDTOs(competitorsAndBoats);
    }
    
    @Override
    //READ
    public Iterable<BoatDTO> getAllBoats() {
        final HasPermissions.Action[] requiredActionsForRead = SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS;
        Iterable<BoatDTO> result = getSecurityService().mapAndFilterByAnyExplicitPermissionForCurrentUser(
                SecuredDomainType.BOAT, requiredActionsForRead,
                getService().getBaseDomainFactory().getCompetitorAndBoatStore().getBoats(),
                this::convertToBoatDTO);
        return result;
    }

    @Override
    //READ
    public Iterable<BoatDTO> getStandaloneBoats() {
        List<BoatDTO> result = new ArrayList<>();
        getSecurityService().filterObjectsWithAnyPermissionForCurrentUser(
                SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS,
                getService().getBaseDomainFactory().getCompetitorAndBoatStore().getStandaloneBoats(),
                filteredObject -> result.add(convertToBoatDTO(filteredObject)));
        return result;
    }

    @Override
    //READ
    public BoatDTO getBoatLinkedToCompetitorForRace(String leaderboardName, String raceColumnName, String fleetName,
            String competitorIdAsString) throws NotFoundException {
        BoatDTO result = null;
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        Competitor existingCompetitor = getService().getCompetitorAndBoatStore().getExistingCompetitorByIdAsString(competitorIdAsString);
        getSecurityService().checkCurrentUserReadPermission(existingCompetitor);
        Map<Competitor, Boat> competitorToBoatMappingsForRace = getService().getCompetitorToBoatMappingsForRace(leaderboardName, raceColumnName, fleetName);
        if (existingCompetitor != null) {
            Boat boatOfCompetitor = competitorToBoatMappingsForRace.get(existingCompetitor);
            if (boatOfCompetitor != null) {
                result = baseDomainFactory.convertToBoatDTO(boatOfCompetitor);
            }
        }
        return result;
    }

    @Override
    //READ
    public List<DeviceConfigurationWithSecurityDTO> getDeviceConfigurations() {
        List<DeviceConfigurationWithSecurityDTO> configs = new ArrayList<DeviceConfigurationWithSecurityDTO>();
        for (DeviceConfiguration config : getService().getAllDeviceConfigurations()) {
            if (getSecurityService().hasCurrentUserReadPermission(config)) {
                configs.add(convertToDeviceConfigurationWithSecurityDTO(config));
            }
        }
        return configs;
    }

    @Override
    //READ
    public void createOrUpdateDeviceConfiguration(DeviceConfigurationDTO configurationDTO) {
        if (getService().getDeviceConfigurationById(configurationDTO.id) == null) {
            final DeviceConfiguration configuration = convertToDeviceConfiguration(configurationDTO);
            getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(configuration.getPermissionType(),
                    configuration.getIdentifier().getTypeRelativeObjectIdentifier(), configuration.getName(),
                    new Action() {
                        @Override
                        public void run() throws Exception {
                            getService().createOrUpdateDeviceConfiguration(configuration);
                        }
                    });
        } else {
            final DeviceConfiguration configuration = convertToDeviceConfiguration(configurationDTO);
            getSecurityService().checkCurrentUserUpdatePermission(configuration);
            getService().createOrUpdateDeviceConfiguration(configuration);
        }
    }

    //READ
    private DeviceConfigurationWithSecurityDTO convertToDeviceConfigurationWithSecurityDTO(
            DeviceConfiguration configuration) {
        DeviceConfigurationWithSecurityDTO dto = new DeviceConfigurationWithSecurityDTO(configuration.getIdentifier());
        dto.id = configuration.getId();
        dto.name = configuration.getName();
        dto.allowedCourseAreaNames = configuration.getAllowedCourseAreaNames();
        dto.resultsMailRecipient = configuration.getResultsMailRecipient();
        dto.byNameDesignerCourseNames = configuration.getByNameCourseDesignerCourseNames();
        if (configuration.getRegattaConfiguration() != null) {
            dto.regattaConfiguration = convertToRegattaConfigurationDTO(configuration.getRegattaConfiguration());
        }
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), dto, configuration.getIdentifier());
        return dto;
    }

    ///READ
    private DeviceConfigurationDTO.RegattaConfigurationDTO convertToRegattaConfigurationDTO(
            RegattaConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        DeviceConfigurationDTO.RegattaConfigurationDTO dto = new DeviceConfigurationDTO.RegattaConfigurationDTO();
        
        dto.defaultRacingProcedureType = configuration.getDefaultRacingProcedureType();
        dto.defaultCourseDesignerMode = configuration.getDefaultCourseDesignerMode();
        dto.defaultProtestTimeDuration = configuration.getDefaultProtestTimeDuration();
        
        if (configuration.getRRS26Configuration() != null) {
            dto.rrs26Configuration = new DeviceConfigurationDTO.RegattaConfigurationDTO.RRS26ConfigurationDTO();
            copyBasicRacingProcedureProperties(configuration.getRRS26Configuration(), dto.rrs26Configuration);
            copyRacingProcedureWithConfigurableStartModeFlagProperties(configuration.getRRS26Configuration(), dto.rrs26Configuration);
        }
        if (configuration.getSWCStartConfiguration() != null) {
            dto.swcStartConfiguration = new DeviceConfigurationDTO.RegattaConfigurationDTO.SWCStartConfigurationDTO();
            copyBasicRacingProcedureProperties(configuration.getSWCStartConfiguration(), dto.swcStartConfiguration);
            copyRacingProcedureWithConfigurableStartModeFlagProperties(configuration.getSWCStartConfiguration(), dto.swcStartConfiguration);
        }
        if (configuration.getGateStartConfiguration() != null) {
            dto.gateStartConfiguration = new DeviceConfigurationDTO.RegattaConfigurationDTO.GateStartConfigurationDTO();
            copyBasicRacingProcedureProperties(configuration.getGateStartConfiguration(), dto.gateStartConfiguration);
            dto.gateStartConfiguration.hasPathfinder = configuration.getGateStartConfiguration().hasPathfinder();
            dto.gateStartConfiguration.hasAdditionalGolfDownTime = configuration.getGateStartConfiguration().hasAdditionalGolfDownTime();
        }
        if (configuration.getESSConfiguration() != null) {
            dto.essConfiguration = new DeviceConfigurationDTO.RegattaConfigurationDTO.ESSConfigurationDTO();
            copyBasicRacingProcedureProperties(configuration.getESSConfiguration(), dto.essConfiguration);
        }
        if (configuration.getBasicConfiguration() != null) {
            dto.basicConfiguration = new DeviceConfigurationDTO.RegattaConfigurationDTO.RacingProcedureConfigurationDTO();
            copyBasicRacingProcedureProperties(configuration.getBasicConfiguration(), dto.basicConfiguration);
        }
        if (configuration.getLeagueConfiguration() != null) {
            dto.leagueConfiguration = new DeviceConfigurationDTO.RegattaConfigurationDTO.LeagueConfigurationDTO();
            copyBasicRacingProcedureProperties(configuration.getLeagueConfiguration(), dto.leagueConfiguration);
        }
        return dto;
    }
    
    //READ
    private void copyBasicRacingProcedureProperties(RacingProcedureConfiguration configuration, final RacingProcedureConfigurationDTO racingProcedureConfigurationDTO) {
        racingProcedureConfigurationDTO.classFlag = configuration.getClassFlag();
        racingProcedureConfigurationDTO.hasIndividualRecall = configuration.hasIndividualRecall();
        racingProcedureConfigurationDTO.isResultEntryEnabled = configuration.isResultEntryEnabled();
    }

    //READ
    private void copyRacingProcedureWithConfigurableStartModeFlagProperties(ConfigurableStartModeFlagRacingProcedureConfiguration configuration, final RacingProcedureWithConfigurableStartModeFlagConfigurationDTO racingProcedureConfigurationDTO) {
        racingProcedureConfigurationDTO.startModeFlags = configuration.getStartModeFlags();
    }

    //READ
    private DeviceConfigurationImpl convertToDeviceConfiguration(DeviceConfigurationDTO dto) {
        DeviceConfigurationImpl configuration = new DeviceConfigurationImpl(convertToRegattaConfiguration(dto.regattaConfiguration), dto.id, dto.name);
        configuration.setAllowedCourseAreaNames(dto.allowedCourseAreaNames);
        configuration.setResultsMailRecipient(dto.resultsMailRecipient);
        configuration.setByNameDesignerCourseNames(dto.byNameDesignerCourseNames);
        return configuration;
    }

    //READ
    protected RegattaConfiguration convertToRegattaConfiguration(RegattaConfigurationDTO dto) {
        if (dto == null) {
            return null;
        }
        RegattaConfigurationImpl configuration = new RegattaConfigurationImpl();
        configuration.setDefaultRacingProcedureType(dto.defaultRacingProcedureType);
        configuration.setDefaultCourseDesignerMode(dto.defaultCourseDesignerMode);
        configuration.setDefaultProtestTimeDuration(dto.defaultProtestTimeDuration);
        if (dto.rrs26Configuration != null) {
            RRS26ConfigurationImpl config = new RRS26ConfigurationImpl();
            applyGeneralRacingProcedureConfigProperties(dto.rrs26Configuration, config);
            applyRacingProcedureWithConfigurableStartModeFlagConfigProperties(dto.rrs26Configuration, config);
            configuration.setRRS26Configuration(config);
        }
        if (dto.swcStartConfiguration != null) {
            SWCStartConfigurationImpl config = new SWCStartConfigurationImpl();
            applyGeneralRacingProcedureConfigProperties(dto.swcStartConfiguration, config);
            applyRacingProcedureWithConfigurableStartModeFlagConfigProperties(dto.swcStartConfiguration, config);
            configuration.setSWCStartConfiguration(config);
        }
        if (dto.gateStartConfiguration != null) {
            GateStartConfigurationImpl config = new GateStartConfigurationImpl();
            applyGeneralRacingProcedureConfigProperties(dto.gateStartConfiguration, config);
            config.setHasPathfinder(dto.gateStartConfiguration.hasPathfinder);
            config.setHasAdditionalGolfDownTime(dto.gateStartConfiguration.hasAdditionalGolfDownTime);
            configuration.setGateStartConfiguration(config);
        }
        if (dto.essConfiguration != null) {
            ESSConfigurationImpl config = new ESSConfigurationImpl();
            applyGeneralRacingProcedureConfigProperties(dto.essConfiguration, config);
            configuration.setESSConfiguration(config);
        }
        if (dto.basicConfiguration != null) {
            RacingProcedureConfigurationImpl config = new RacingProcedureConfigurationImpl();
            applyGeneralRacingProcedureConfigProperties(dto.basicConfiguration, config);
            configuration.setBasicConfiguration(config);
        }
        if (dto.leagueConfiguration != null) {
            LeagueConfigurationImpl config = new LeagueConfigurationImpl();
            applyGeneralRacingProcedureConfigProperties(dto.leagueConfiguration, config);
            configuration.setLeagueConfiguration(config);
        }
        return configuration;
    }

    //READ
    private void applyGeneralRacingProcedureConfigProperties(RacingProcedureConfigurationDTO racingProcedureConfigurationDTO,
            RacingProcedureConfigurationImpl config) {
        config.setClassFlag(racingProcedureConfigurationDTO.classFlag);
        config.setHasIndividualRecall(racingProcedureConfigurationDTO.hasIndividualRecall);
        config.setResultEntryEnabled(racingProcedureConfigurationDTO.isResultEntryEnabled);
    }

    //READ
    private void applyRacingProcedureWithConfigurableStartModeFlagConfigProperties(
            RacingProcedureWithConfigurableStartModeFlagConfigurationDTO racingProcedureConfigurationDTO,
            RacingProcedureWithConfigurableStartModeFlagConfigurationImpl config) {
        config.setStartModeFlags(racingProcedureConfigurationDTO.startModeFlags);
    }
    
    @Override
    //READ
    public boolean setStartTimeAndProcedure(RaceLogSetStartTimeAndProcedureDTO dto) throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(dto.leaderboardName));
        TimePoint newStartTime = getService().setStartTimeAndProcedure(dto.leaderboardName, dto.raceColumnName, 
                dto.fleetName, dto.authorName, dto.authorPriority,
                dto.passId, new MillisecondsTimePoint(dto.logicalTimePoint), new MillisecondsTimePoint(dto.startTime),
                dto.racingProcedure, dto.courseAreaId);
        return new MillisecondsTimePoint(dto.startTime).equals(newStartTime);
    }
    
    @Override
    //READ
    public Pair<Boolean, Boolean> setFinishingAndEndTime(RaceLogSetFinishingAndFinishTimeDTO dto)
            throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(dto.leaderboardName));
        final MillisecondsTimePoint finishingTimePoint = dto.finishingTime==null?null:new MillisecondsTimePoint(dto.finishingTime);
        TimePoint newFinsihingTime = getService().setFinishingTime(dto.leaderboardName, dto.raceColumnName, 
                dto.fleetName, dto.authorName, dto.authorPriority,
                dto.passId, finishingTimePoint);
        
        final TimePoint finishTimePoint = dto.finishTime==null?null:new MillisecondsTimePoint(dto.finishTime);
        TimePoint newEndTime = getService().setEndTime(dto.leaderboardName, dto.raceColumnName, 
                dto.fleetName, dto.authorName, dto.authorPriority,
                dto.passId, finishTimePoint);
        
        return new Pair<Boolean, Boolean>(Util.equalsWithNull(finishingTimePoint, newFinsihingTime),
                Util.equalsWithNull(finishTimePoint, newEndTime));
    }

    @Override
    //READ
    public Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> getTrackingTimes(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        final Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> times;
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        if (raceLog != null) {
            times = new TrackingTimesFinder(raceLog).analyze();
        } else {
            times = null;
        }
        return times;
    }

    @Override
    //READ
    public com.sap.sse.common.Util.Triple<Date, Integer, RacingProcedureType> getStartTimeAndProcedure(
            String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        com.sap.sse.common.Util.Triple<TimePoint, Integer, RacingProcedureType> result = getService().getStartTimeAndProcedure(leaderboardName, raceColumnName, fleetName);
        if (result == null || result.getA() == null) {
            return null;
        }
        return new com.sap.sse.common.Util.Triple<Date, Integer, RacingProcedureType>(result.getA() == null ? null : result.getA().asDate(), result.getB(), result.getC());
    }
    

    @Override
    //READ
    public com.sap.sse.common.Util.Triple<Date, Date, Integer> getFinishingAndFinishTime(String leaderboardName,
            String raceColumnName, String fleetName) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        com.sap.sse.common.Util.Triple<TimePoint, TimePoint, Integer> result = getService().getFinishingAndFinishTime(leaderboardName, raceColumnName, fleetName);
        if (result == null) {
            return null;
        }
        return new com.sap.sse.common.Util.Triple<>(result.getA() == null ? null : result.getA().asDate(),
                result.getB() == null ? null : result.getB().asDate(), result.getC());
    }

    @Override
  //READ
    public Iterable<AccountWithSecurityDTO> getAllIgtimiAccountsWithSecurity() {
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                getIgtimiConnectionFactory().getAllAccounts(), this::toSecuredIgtimiAccountDTO);
    }

    //READ
    private AccountWithSecurityDTO toSecuredIgtimiAccountDTO(final Account igtimiAccount) {
        final com.sap.sailing.domain.igtimiadapter.User user = igtimiAccount.getUser();
        final String email = user.getEmail();
        final String name = user.getFirstName() + " " + user.getSurname();
        final AccountWithSecurityDTO securedAccount = new AccountWithSecurityDTO(email, name,
                igtimiAccount.getCreatorName());
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), securedAccount, igtimiAccount.getIdentifier());
        return securedAccount;
    }

  //??
    protected IgtimiConnectionFactory getIgtimiConnectionFactory() {
        return igtimiAdapterTracker.getService();
    }
  //??
    protected RaceLogTrackingAdapterFactory getRaceLogTrackingAdapterFactory() {
        return raceLogTrackingAdapterTracker.getService();
    }

    //??
    protected RaceLogTrackingAdapter getRaceLogTrackingAdapter() {
        return getRaceLogTrackingAdapterFactory().getAdapter(getBaseDomainFactory());
    }

    @Override
    //READ
    public String getIgtimiAuthorizationUrl(String redirectProtocol, String redirectHostname, String redirectPort) throws MalformedURLException, UnsupportedEncodingException {
        return getIgtimiConnectionFactory().getAuthorizationUrl(redirectProtocol, redirectHostname, redirectPort);
    }

    //READ
    protected Set<DynamicTrackedRace> getAllTrackedRaces() {
        Set<DynamicTrackedRace> result = new HashSet<DynamicTrackedRace>();
        Iterable<Regatta> allRegattas = getService().getAllRegattas();
        for (Regatta regatta : allRegattas) {
            DynamicTrackedRegatta trackedRegatta = getService().getTrackedRegatta(regatta);
            if (trackedRegatta != null) {
                trackedRegatta.lockTrackedRacesForRead();
                try {
                    Iterable<DynamicTrackedRace> trackedRaces = trackedRegatta.getTrackedRaces();
                    for (TrackedRace trackedRace : trackedRaces) {
                        result.add((DynamicTrackedRace) trackedRace);
                    }
                } finally {
                    trackedRegatta.unlockTrackedRacesAfterRead();
                }
            }
        }
        return result;
    }

    /**
     * @param triple
     *            leaderboard and racecolumn and fleet names
     */
    //READ
    protected RaceLog getRaceLog(com.sap.sse.common.Util.Triple<String, String, String> triple) throws NotFoundException {
        return getRaceLog(triple.getA(), triple.getB(), triple.getC());
    }
    
    //READ
    protected RegattaLog getRegattaLogInternal(String leaderboardName) throws DoesNotHaveRegattaLogException {
        Leaderboard l = getService().getLeaderboardByName(leaderboardName);
        if (! (l instanceof HasRegattaLike)) {
            throw new DoesNotHaveRegattaLogException();
        }
        return ((HasRegattaLike) l).getRegattaLike().getRegattaLog();
    }
    
    //READ
    protected RaceLog getRaceLog(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException {
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);
        return raceColumn.getRaceLog(fleet);
    }

    //READ
    protected Competitor getCompetitor(CompetitorDTO dto) {
        return getService().getCompetitorAndBoatStore().getExistingCompetitorByIdAsString(dto.getIdAsString());
    }
    
  //READ
    protected CompetitorWithBoat getCompetitor(CompetitorWithBoatDTO dto) {
        return getService().getCompetitorAndBoatStore().getExistingCompetitorWithBoatByIdAsString(dto.getIdAsString());
    }

  //READ
    protected Boat getBoat(BoatDTO dto) {
        return getService().getCompetitorAndBoatStore().getExistingBoatByIdAsString(dto.getIdAsString());
    }

    //READ
    protected Mark convertToMark(MarkDTO dto, boolean resolve) {
        Mark result = null;
        if (resolve) {
            Mark existing = baseDomainFactory.getExistingMarkByIdAsString(dto.getIdAsString());
            if (existing != null) {
                result = existing;
            }
        }
        if (result == null) {
            Serializable id = UUID.randomUUID();
            result = baseDomainFactory.getOrCreateMark(id, dto.getName(), dto.getShortName(), dto.type, dto.color, dto.shape, dto.pattern);
        }
        return result;
    }
    
    /**
     * Also finds the last position of the marks if set by pinging them as long as currently a {@link TrackedRace} exists
     * whose tracking interval spans the current time. If at least a non-spanning {@link TrackedRace} can be found in the
     * scope of the <code>leaderboard</code>, the time-wise closest position fix for the mark will be used as its position.
     * If the mark has been pinged through the {@link RegattaLog} but no {@link TrackedRace} exists that has loaded that ping,
     * the ping won't be visible to this API.
     */
    //READ
    private MarkDTO convertToMarkDTO(LeaderboardThatHasRegattaLike leaderboard, Mark mark) {
        final TimePoint now = MillisecondsTimePoint.now();
        final Position lastPos = getService().getMarkPosition(mark, leaderboard, now);
        return convertToMarkDTO(mark, lastPos);
    }
    
    /**
     * @param trackedRace
     *            if <code>null</code>, no position data will be attached to the {@link MarkDTO}s
     * @param timePoint
     *            if <code>trackedRace</code> is not <code>null</code>, specifies the time point for which to determine
     *            the mark positions and attach to the {@link MarkDTO}s. If <code>null<c/code>, the current time point
     *            will be used as default.
     */
  //READ
    private WaypointDTO convertToWaypointDTO(Waypoint waypoint, Map<Serializable, ControlPointDTO> controlPointCache, MarkPositionFinder positionFinder, TimePoint timePoint) {
        ControlPointDTO cp = controlPointCache.get(waypoint.getControlPoint().getId());
        if (cp == null) {
            cp = convertToControlPointDTO(waypoint.getControlPoint(), positionFinder, timePoint);
            controlPointCache.put(waypoint.getControlPoint().getId(), cp);
        }
        return new WaypointDTO(waypoint.getName(), cp, waypoint.getPassingInstructions());
    }
    
    /**
     * @param course
     *            the course to convert
     * @param trackedRace
     *            if <code>null</code>, no position data will be attached to the {@link MarkDTO}s
     * @param timePoint
     *            if <code>trackedRace</code> is not <code>null</code>, specifies the time point for which to determine
     *            the mark positions and attach to the {@link MarkDTO}s. If <code>null<c/code>, the current time point
     *            will be used as default.
     */
    //READ
    private RaceCourseDTO convertToRaceCourseDTO(CourseBase course, MarkPositionFinder positionFinder, TimePoint timePoint) {
        final RaceCourseDTO result;
        if (course != null) {
            List<WaypointDTO> waypointDTOs = new ArrayList<WaypointDTO>();
            Map<Serializable, ControlPointDTO> controlPointCache = new HashMap<>();
            for (Waypoint waypoint : course.getWaypoints()) {
                waypointDTOs.add(convertToWaypointDTO(waypoint, controlPointCache, positionFinder, timePoint));
            }
            result = new RaceCourseDTO(waypointDTOs);
        } else {
            result = new RaceCourseDTO(Collections.<WaypointDTO> emptyList());
        }
        return result;
    }
    
    @Override
    //READ
    public RaceCourseDTO getLastCourseDefinitionInRaceLog(final String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException {
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        // only look for course definitions that really define waypoints; ignore by-name course updates
        CourseBase lastPublishedCourse = new LastPublishedCourseDesignFinder(raceLog, /* onlyCoursesWithValidWaypointList */ true).analyze();
        if (lastPublishedCourse == null) {
            lastPublishedCourse = new CourseDataImpl("");
        }
        return convertToRaceCourseDTO(lastPublishedCourse, new MarkPositionFinder() {
            @Override
            public Position find(Mark mark, TimePoint at) {
                return getService().getMarkPosition(mark, (LeaderboardThatHasRegattaLike) getService().getLeaderboardByName(leaderboardName), at);
            }
        }, /* timePoint */ MillisecondsTimePoint.now());
    }
    
    //READ
    private TypeBasedServiceFinder<DeviceIdentifierStringSerializationHandler> getDeviceIdentifierStringSerializerHandlerFinder(
            boolean withFallback) {
        TypeBasedServiceFinderFactory factory = getService().getTypeBasedServiceFinderFactory();
        TypeBasedServiceFinder<DeviceIdentifierStringSerializationHandler> finder = factory.createServiceFinder(DeviceIdentifierStringSerializationHandler.class);
        if (withFallback) {
            finder.setFallbackService(new PlaceHolderDeviceIdentifierStringSerializationHandler());
        }
        return finder;
    }
    
    //READ
    private DeviceIdentifier deserializeDeviceIdentifier(String type, String deviceId) throws NoCorrespondingServiceRegisteredException,
    TransformationException {
        DeviceIdentifierStringSerializationHandler handler =
                getDeviceIdentifierStringSerializerHandlerFinder(false).findService(type);
        return handler.deserialize(deviceId, type, deviceId);
    }
    
  //READ
    private String serializeDeviceIdentifier(DeviceIdentifier deviceId) throws TransformationException {
        return getDeviceIdentifierStringSerializerHandlerFinder(true).findService(
                deviceId.getIdentifierType()).serialize(deviceId).getB();
    }
    
  //READ
    private DeviceMappingDTO convertToDeviceMappingDTO(DeviceMapping<?> mapping) throws TransformationException {
        final Map<DeviceIdentifier, Timed> lastFixes = getService().getSensorFixStore().getFixLastReceived(Collections.singleton(mapping.getDevice()));
        final Timed lastFix;
        if (lastFixes != null && lastFixes.containsKey(mapping.getDevice())) {
            lastFix = lastFixes.get(mapping.getDevice());
        } else {
            lastFix = null;
        }
        String deviceId = serializeDeviceIdentifier(mapping.getDevice());
        Date from = mapping.getTimeRange().from() == null || mapping.getTimeRange().from().equals(TimePoint.BeginningOfTime) ? 
                null : mapping.getTimeRange().from().asDate();
        Date to = mapping.getTimeRange().to() == null || mapping.getTimeRange().to().equals(TimePoint.EndOfTime) ?
                null : mapping.getTimeRange().to().asDate();
        MappableToDevice item = null;
        final WithID mappedTo = mapping.getMappedTo();
        if (mappedTo == null) {
            throw new RuntimeException("Device mapping not mapped to any object");
        } else if (mappedTo instanceof Competitor) {
            item = baseDomainFactory.convertToCompetitorDTO((Competitor) mapping.getMappedTo());
        } else if (mappedTo instanceof Mark) {
            item = convertToMarkDTO((Mark) mapping.getMappedTo(), null);
        } else if (mappedTo instanceof Boat) {
            item = baseDomainFactory.convertToBoatDTO((Boat) mappedTo);
        } else {
            throw new RuntimeException("Can only handle Competitor, Boat or Mark as mapped item type, but not "
                    + mappedTo.getClass().getName());
        }
        //Only deal with UUIDs - otherwise we would have to pass Serializable to browser context - which
        //has a large performance implact for GWT.
        //As any Serializable subclass is converted to String by the BaseRaceLogEventSerializer, and only UUIDs are
        //recovered by the BaseRaceLogEventDeserializer, only UUIDs are safe to use anyway.
        List<UUID> originalRaceLogEventUUIDs = new ArrayList<UUID>();
        for (Serializable id : mapping.getOriginalRaceLogEventIds()) {
            if (! (id instanceof UUID)) {
                logger.log(Level.WARNING, "Got RaceLogEvent with id that was not UUID, but " + id.getClass().getName());
                throw new TransformationException("Could not send device mapping to browser: can only deal with UUIDs");
            }
            originalRaceLogEventUUIDs.add((UUID) id);
        }
        return new DeviceMappingDTO(new DeviceIdentifierDTO(mapping.getDevice().getIdentifierType(),
                deviceId), from, to, item, originalRaceLogEventUUIDs, lastFix==null?null:lastFix.getTimePoint());
    }
    
  //READ
    protected List<AbstractLog<?, ?>> getLogHierarchy(String leaderboardName, String raceColumnName,
            String fleetName) throws NotFoundException {
        List<AbstractLog<?, ?>> result = new ArrayList<>();
        RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        
        if (raceLog != null){
            result.add(raceLog);
        }
        
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        if (leaderboard instanceof HasRegattaLike) {
            result.add(((HasRegattaLike) leaderboard).getRegattaLike().getRegattaLog());
        }
        return result;
    }
    
  //READ
    private List<DeviceMappingDTO> getDeviceMappings(RegattaLog regattaLog)
            throws TransformationException {
        List<DeviceMappingDTO> result = new ArrayList<DeviceMappingDTO>();
        for (List<? extends DeviceMapping<WithID>> list : new RegattaLogDeviceMappingFinder<>(
                regattaLog).analyze().values()) {
            for (DeviceMapping<WithID> mapping : list) {
                result.add(convertToDeviceMappingDTO(mapping));
            }
        }
        return result;
    }
    
    @Override
    //READ
    public List<String> getDeserializableDeviceIdentifierTypes() {
        List<String> result = new ArrayList<String>();
        for (ServiceReference<DeviceIdentifierStringSerializationHandler> reference :
            deviceIdentifierStringSerializationHandlerTracker.getServiceReferences()) {
            result.add((String) reference.getProperty(TypeBasedServiceFinder.TYPE));
        }
        return result;
    }
    
    //READ
    protected DeviceMapping<?> convertToDeviceMapping(DeviceMappingDTO dto)
            throws NoCorrespondingServiceRegisteredException, TransformationException {
        DeviceIdentifier device = deserializeDeviceIdentifier(dto.deviceIdentifier.deviceType, dto.deviceIdentifier.deviceId);
        TimePoint from = dto.from == null ? null : new MillisecondsTimePoint(dto.from);
        TimePoint to = dto.to == null ? null : new MillisecondsTimePoint(dto.to);
        TimeRange timeRange = new TimeRangeImpl(from, to);
        if (dto.mappedTo instanceof MarkDTO) {
            Mark mark = convertToMark(((MarkDTO) dto.mappedTo), true);
            //expect UUIDs
            return new DeviceMappingImpl<Mark>(mark, device, timeRange, dto.originalRaceLogEventIds, RegattaLogDeviceMarkMappingEventImpl.class);
        } else if (dto.mappedTo instanceof CompetitorDTO) {
            Competitor competitor = getService().getCompetitorAndBoatStore().getExistingCompetitorByIdAsString(
                    ((CompetitorDTO) dto.mappedTo).getIdAsString());
            return new DeviceMappingImpl<Competitor>(competitor, device, timeRange, dto.originalRaceLogEventIds, RegattaLogDeviceCompetitorMappingEventImpl.class);
        } else if (dto.mappedTo instanceof BoatDTO) {
            final Boat boat = getService().getCompetitorAndBoatStore()
                    .getExistingBoatByIdAsString(dto.mappedTo.getIdAsString());
            return new DeviceMappingImpl<WithID>(boat, device, timeRange, dto.originalRaceLogEventIds,
                    RegattaLogDeviceBoatMappingEventImpl.class);
        } else {
            throw new RuntimeException("Can only map devices to competitors, boats or marks");
        }
    }

    @Override
  //READ
    public Collection<String> getGPSFixImporterTypes() {
        return getRegisteredImporterTypes(GPSFixImporter.class);
    }
    
    @Override
  //READ
    public Collection<String> getSensorDataImporterTypes() {
        return getRegisteredImporterTypes(DoubleVectorFixImporter.class);
    }
    
  //READ
    private <S> Collection<String> getRegisteredImporterTypes(Class<S> referenceClass) {
        Set<String> result = new HashSet<>();
        for (ServiceReference<S> reference : getRegisteredServiceReferences(referenceClass)) {
            result.add((String) reference.getProperty(TypeBasedServiceFinder.TYPE));
        }
        return result;
    }
    
  //READ
    protected <S extends DoubleVectorFixImporter> S getRegisteredImporter(Class<S> referenceClass, String type)
            throws NoCorrespondingServiceRegisteredException {
        for (ServiceReference<S> reference : getRegisteredServiceReferences(referenceClass)) {
            S importer = Activator.getDefault().getService(reference);
            if (importer != null && importer.getType().equals(type)) {
                return importer;
            }
        }
        throw new NoCorrespondingServiceRegisteredException("No importer service found!", type, referenceClass.getName());
    }
    
  //READ
    private <S> Collection<ServiceReference<S>> getRegisteredServiceReferences(Class<S> referenceClass) {
        try {
            return Activator.getDefault().getServiceReferences(referenceClass, null);
        } catch (InvalidSyntaxException e) {
            // shouldn't happen, as we are passing null for the filter
        }
        return Collections.emptyList();
    }
    
    @Override
    //READ
    public List<TrackFileImportDeviceIdentifierDTO> getTrackFileImportDeviceIds(List<String> uuids)
            throws NoCorrespondingServiceRegisteredException, TransformationException {
        try {
            final List<TrackFileImportDeviceIdentifierDTO> result = new ArrayList<>();
            for (String uuidAsString : uuids) {
                UUID uuid = UUID.fromString(uuidAsString);
                TrackFileImportDeviceIdentifier device = TrackFileImportDeviceIdentifierImpl.getOrCreate(uuid);
                long numFixes = getService().getSensorFixStore().getNumberOfFixes(device);
                TimeRange timeRange = getService().getSensorFixStore().getTimeRangeCoveredByFixes(device);
                Date from = timeRange == null ? null : timeRange.from().asDate();
                Date to = timeRange == null ? null : timeRange.to().asDate();
                result.add(new TrackFileImportDeviceIdentifierDTO(uuidAsString, device.getFileName(), device.getTrackName(),
                        numFixes, from, to));
            }
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception trying to obtain track file import device IDs", e);
            throw e;
        }
    }

    @Override
    //READ
    public ArrayList<EventDTO> getEventsForLeaderboard(String leaderboardName) {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        HashMap<UUID, EventDTO> events = new HashMap<>();
        for (Event e : getService().getAllEvents()) {
            for (LeaderboardGroup g : e.getLeaderboardGroups()) {
                for (Leaderboard l : g.getLeaderboards()) {
                    if (leaderboard.equals(l)) {
                        events.put(e.getId(), convertToEventDTO(e, false));
                    }
                }
            }
        }
        return new ArrayList<>(events.values());
    }

    @Override
    //READ
    public Map<Integer, Date> getCompetitorRaceLogMarkPassingData(String leaderboardName, String raceColumnName,
            String fleetName, CompetitorDTO competitor) throws NotFoundException {
        Map<Integer, Date> result = new HashMap<>();
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        RaceLog raceLog = getService().getRaceLog(leaderboardName, raceColumnName, fleetName);
        for (Triple<Competitor, Integer, TimePoint> fixedEvent : new MarkPassingDataFinder(raceLog).analyze()) {
            if (fixedEvent.getA().getName().equals(competitor.getName())) {
                final Date date;
                if (fixedEvent.getC() != null) {
                    date = new Date(fixedEvent.getC().asMillis());
                } else {
                    date = null;
                }
                result.put(fixedEvent.getB(), date);
            }
        }
        return result;
    }

    @Override
  //READ
    public Map<Integer, Date> getCompetitorMarkPassings(RegattaAndRaceIdentifier race, CompetitorDTO competitorDTO, boolean waitForCalculations) {
        Map<Integer, Date> result = new HashMap<>();
        final TrackedRace trackedRace = getExistingTrackedRace(race);
        getSecurityService().checkCurrentUserReadPermission(trackedRace);
        if (trackedRace != null) {
            Competitor competitor = getCompetitorByIdAsString(trackedRace.getRace().getCompetitors(), competitorDTO.getIdAsString());
            Set<MarkPassing> competitorMarkPassings;
            competitorMarkPassings = trackedRace.getMarkPassings(competitor, waitForCalculations);
            Iterable<Waypoint> waypoints = trackedRace.getRace().getCourse().getWaypoints();
            if (competitorMarkPassings != null) {
                for (MarkPassing markPassing : competitorMarkPassings) {
                    result.put(Util.indexOf(waypoints, markPassing.getWaypoint()), markPassing.getTimePoint().asDate());
                }
            }
        }
        return result;
    }

    protected Locale getLocale(String localeInfoName) {
        return ResourceBundleStringMessages.Util.getLocaleFor(localeInfoName);
    }

    /**
     * Gets all Marks defined in the RegattaLog and the trackedRace
     * @throws DoesNotHaveRegattaLogException 
     */
    @Override
    //READ
    public Iterable<MarkDTO> getMarksInRegattaLog(String leaderboardName) throws DoesNotHaveRegattaLogException {
        final Leaderboard l = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(l);
        if (! (l instanceof HasRegattaLike)) {
            throw new DoesNotHaveRegattaLogException();
        }
        final LeaderboardThatHasRegattaLike leaderboard = (LeaderboardThatHasRegattaLike) l;
        final RegattaLog regattaLog = leaderboard.getRegattaLike().getRegattaLog();
        final Set<MarkDTO> markDTOs = new HashSet<>();
        final List<RegattaLogEvent> markEvents = new AllEventsOfTypeFinder<>(regattaLog, /* only unrevoked */ true, RegattaLogDefineMarkEvent.class).analyze();
        for (RegattaLogEvent regattaLogEvent : markEvents) {
            final RegattaLogDefineMarkEvent defineMarkEvent = (RegattaLogDefineMarkEvent) regattaLogEvent;
            markDTOs.add(convertToMarkDTO(leaderboard, defineMarkEvent.getMark()));
        }
        return markDTOs;
    }

    /**
     * Gets all the logs corresponding to a leaderboard. This includes all the RaceLogs of the leaderBoard's raceColumns
     * @param leaderboardName
     * @return
     */
  //READ
    protected List<AbstractLog<?, ?>> getLogHierarchy(String leaderboardName) {
        final List<AbstractLog<?, ?>> result;
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard == null) {
            result = null;
        } else {
            result = new ArrayList<>();
            if (leaderboard instanceof HasRegattaLike) {
                result.add(((HasRegattaLike) leaderboard).getRegattaLike().getRegattaLog());
            }
            for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                for (Fleet fleet : raceColumn.getFleets()) {
                    RaceLog raceLog = raceColumn.getRaceLog(fleet);
                    result.add(raceLog);
                }
            }
        }
        return result;
    }   

  //READ
    public boolean doesRegattaLogContainCompetitors(String leaderboardName)
            throws DoesNotHaveRegattaLogException, NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        List<RegattaLogEvent> comeptitorRegistrationEvents = new AllEventsOfTypeFinder<>(regattaLog,
                /* only unrevoked */ true, RegattaLogRegisterCompetitorEvent.class).analyze();
        return !comeptitorRegistrationEvents.isEmpty();
    }

    @Override
  //READ
    public RegattaAndRaceIdentifier getRaceIdentifier(String regattaLikeName, String raceColumnName, String fleetName) {
        RegattaAndRaceIdentifier result = null;
        final Leaderboard leaderboard = getService().getLeaderboardByName(regattaLikeName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        if (leaderboard != null) {
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                    getSecurityService().checkCurrentUserReadPermission(trackedRace);
                    if (trackedRace != null) {
                        result = trackedRace.getRaceIdentifier();
                    }
                }
            }
        }
        return result;
    }

    @Override
  //READ
    public Collection<CompetitorAndBoatDTO> getCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName,
            String fleetName) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);
        return convertToCompetitorAndBoatDTOs(raceColumn.getAllCompetitorsAndTheirBoats(fleet));
    }
    
    @Override
  //READ
    public Collection<CompetitorDTO> getCompetitorRegistrationsForLeaderboard(String leaderboardName) throws NotFoundException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        return convertToCompetitorDTOs(leaderboard.getAllCompetitors());
    }

    @Override
  //READ
    public Collection<CompetitorDTO> getCompetitorRegistrationsInRegattaLog(String leaderboardName) throws DoesNotHaveRegattaLogException, NotFoundException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        if (! (leaderboard instanceof HasRegattaLike)) {
            throw new DoesNotHaveRegattaLogException();
        }
        HasRegattaLike regattaLikeLeaderboard = ((HasRegattaLike) leaderboard);
        return convertToCompetitorDTOs(regattaLikeLeaderboard.getCompetitorsRegisteredInRegattaLog());
    }
    
    @Override
  //READ
    public List<CompetitorAndBoatDTO> getCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName,
            String fleetName) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);
        return convertToCompetitorAndBoatDTOs(raceColumn.getCompetitorsRegisteredInRacelog(fleet));
    }
    
    @Override
  //READ
    public Map<CompetitorDTO, BoatDTO> getCompetitorAndBoatRegistrationsInRaceLog(String leaderboardName, String raceColumnName,
            String fleetName) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);
        Map<Competitor, Boat> competitorsAndBoatsRegisteredInRacelog = raceColumn.getCompetitorsRegisteredInRacelog(fleet);
        return baseDomainFactory.convertToCompetitorAndBoatDTOs(competitorsAndBoatsRegisteredInRacelog);
    }

    @Override
  //READ
    public Collection<BoatDTO> getBoatRegistrationsInRegattaLog(String leaderboardName) throws DoesNotHaveRegattaLogException, NotFoundException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        if (!(leaderboard instanceof HasRegattaLike)) {
            throw new DoesNotHaveRegattaLogException();
        }
        HasRegattaLike regattaLikeLeaderboard = ((HasRegattaLike) leaderboard);
        return convertToBoatDTOs(regattaLikeLeaderboard.getBoatsRegisteredInRegattaLog());
    }
    
    @Override
    //READ
    public Collection<BoatDTO> getBoatRegistrationsForLeaderboard(String leaderboardName) throws NotFoundException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        return convertToBoatDTOs(leaderboard.getAllBoats());
    }

    @Override
    //READ
    public Collection<BoatDTO> getBoatRegistrationsForRegatta(RegattaIdentifier regattaIdentifier) throws NotFoundException {
        final Regatta regatta = (Regatta) regattaIdentifier.getRegatta(getService());
        if (regatta == null) {
            throw new NotFoundException("Regatta "+regattaIdentifier+" not found");
        }
        getSecurityService().checkCurrentUserReadPermission(regatta);
        return convertToBoatDTOs(regatta.getAllBoats());
    }

    @Override
  //READ
    public Boolean areCompetitorRegistrationsEnabledForRace(String leaderboardName, String raceColumnName,
            String fleetName) throws NotFoundException {
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);
        return raceColumn.isCompetitorRegistrationInRacelogEnabled(fleet);
    }
    
  //READ
    protected Fleet getFleetByName(RaceColumn raceColumn, String fleetName) throws NotFoundException{
        Fleet fleet = raceColumn.getFleetByName(fleetName);
        if (fleet == null){
            throw new NotFoundException("fleet with name "+fleetName+" not found");
        }
        return fleet;
    }
    
  //READ
    protected Leaderboard getLeaderboardByName(String leaderboardName) throws NotFoundException{
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard == null){
            throw new NotFoundException("Leaderboard with name "+leaderboardName+" not found");
        }
        return leaderboard;
    }
    
  //READ
    protected RaceColumn getRaceColumn(String leaderboardName, String raceColumnName) throws NotFoundException{
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard == null){
            throw new NotFoundException("leaderboard with name "+leaderboardName+" not found");
        } 
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        if (raceColumn == null){
            throw new NotFoundException("raceColumn with name "+raceColumnName+" not found");
        }
        return raceColumn;
    }

    @Override
  //READ
    public Pair<Boolean, String> checkIfMarksAreUsedInOtherRaceLogs(String leaderboardName, String raceColumnName,
            String fleetName, Set<MarkDTO> marksToRemove) throws NotFoundException {
        Set<String> markIds = new HashSet<String>();
        for (MarkDTO markDTO : marksToRemove) {
            markIds.add(markDTO.getIdAsString());
        }
        RaceLog raceLogToIgnore = getRaceLog(leaderboardName, raceColumnName, fleetName);
        HashSet<String> racesContainingMarksToDeleteInCourse = new HashSet<String>();
        boolean marksAreUsedInOtherRaceLogs = false;
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                RaceLog raceLog = raceColumn.getRaceLog(fleet);
                if (raceLog != raceLogToIgnore) {
                    LastPublishedCourseDesignFinder finder = new LastPublishedCourseDesignFinder(raceLog, /* onlyCoursesWithValidWaypointList */ true);
                    CourseBase course = finder.analyze();
                    if (course != null) {
                        for (Waypoint waypoint : course.getWaypoints()) {
                            for (Mark mark : waypoint.getMarks()) {
                                if (markIds.contains(mark.getId().toString())) {
                                    racesContainingMarksToDeleteInCourse.add(raceColumn.getName() + "/"
                                            + fleet.getName());
                                    marksAreUsedInOtherRaceLogs = true;
                                }
                            }
                        }
                    }
                }
            }
        }         
        StringBuilder racesInCollision = new StringBuilder();
        for (String raceName : racesContainingMarksToDeleteInCourse) {
            racesInCollision.append(raceName+", ");
        }
        return new Pair<Boolean, String>(marksAreUsedInOtherRaceLogs, 
                racesInCollision.substring(0, Math.max(0, racesInCollision.length()-2)));
    }

    @Override
  //READ
    public List<DeviceMappingDTO> getDeviceMappings(String leaderboardName)
            throws DoesNotHaveRegattaLogException, TransformationException, NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        return getDeviceMappings(regattaLog);
    }
    
    @Override
  //READ
    public Iterable<MarkDTO> getMarksInTrackedRace(String leaderboardName, String raceColumnName, String fleetName) {
        final List<MarkDTO> marks = new ArrayList<>();
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        if (leaderboard != null) {
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    for (final Mark mark : raceColumn.getAvailableMarks(fleet)) {
                        marks.add(new MarkDTO(mark.getId().toString(), mark.getName(), mark.getShortName()));
                    }
                }
            }
        }
        return marks;
    }

  //READ
    private MarkTrackDTO getMarkTrack(Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet,
            String markIdAsString) {
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        MarkDTO markDTO = null;
        Mark mark = null;
        for (final Mark currMark : raceColumn.getAvailableMarks(fleet)) {
            if (currMark.getId().toString().equals(markIdAsString)) {
                mark = currMark;
                markDTO = convertToMarkDTO(currMark, /* position */ null);
                break;
            }
        }
        if (markDTO != null) {
            final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
            final GPSFixTrack<Mark, ? extends GPSFix> markTrack;
            if (trackedRace != null) {
                markTrack = trackedRace.getOrCreateTrack(mark);
            } else {
                DynamicGPSFixTrackImpl<Mark> writeableMarkTrack = new DynamicGPSFixTrackImpl<Mark>(mark,
                        BoatClass.APPROXIMATE_AVERAGE_MANEUVER_DURATION.asMillis());
                markTrack = writeableMarkTrack;
                final RaceLog raceLog = raceColumn.getRaceLog(fleet);
                final RegattaLog regattaLog = raceColumn.getRegattaLog();
                final TrackingTimesFinder trackingTimesFinder = new TrackingTimesFinder(raceLog);
                final Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> trackingTimes = trackingTimesFinder
                        .analyze();
                try {
                    SensorFixStore sensorFixStore = getService().getSensorFixStore();
                    List<DeviceMappingWithRegattaLogEvent<Mark>> mappings = new RegattaLogDeviceMarkMappingFinder(
                            regattaLog).analyze().get(mark);
                    if (mappings != null) {
                        for (DeviceMapping<Mark> mapping : mappings) {
                            final TimePoint from = Util.getLatestOfTimePoints(trackingTimes.getA().getTimePoint(),
                                    mapping.getTimeRange().from());
                            final TimePoint to = Util.getEarliestOfTimePoints(trackingTimes.getB().getTimePoint(),
                                    mapping.getTimeRange().to());
                            sensorFixStore.<GPSFix> loadFixes(loadedFix -> writeableMarkTrack.add(loadedFix, true),
                                    mapping.getDevice(), from, to, false, () -> false, progressIgnoringConsumer -> {
                                    });
                        }
                    }
                } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                    logger.info("Error trying to load mark track for mark " + mark + " from " + trackingTimes.getA()
                            + " to " + trackingTimes.getB());
                }
            }
            Iterable<GPSFixDTO> gpsFixDTOTrack = convertToGPSFixDTOTrack(markTrack);
            return new MarkTrackDTO(markDTO, gpsFixDTOTrack, /* thinned out */ false);
        }
        return null;
    }
    
    @Override
  //READ
    public MarkTrackDTO getMarkTrack(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString) {
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    return getMarkTrack(leaderboard, raceColumn, fleet, markIdAsString);
                }
            }
        }
        return null;
    }
    
    /**
     * Uses all fixed from {@code track} with the outliers removed, {@link #convertToGPSFixDTO(GPSFix) converts} each
     * of them to a {@link GPSFixDTO} and adds them to the resulting list.
     */
  //READ
    private Iterable<GPSFixDTO> convertToGPSFixDTOTrack(Track<? extends GPSFix> track) {
        final List<GPSFixDTO> result = new ArrayList<>();
        track.lockForRead();
        try {
            for (final GPSFix fix : track.getRawFixes()) {
                result.add(convertToGPSFixDTO(fix));
            }
        } finally {
            track.unlockAfterRead();
        }
        return result;
    }
    
  //READ
    private GPSFixDTO convertToGPSFixDTO(GPSFix fix) {
        final GPSFixDTO result;
        if (fix == null) {
            result = null;
        } else {
            result = new GPSFixDTO(fix.getTimePoint().asDate(), fix.getPosition());
        }
        return result;
    }

    @Override
  //READ
    public boolean canRemoveMarkFix(String leaderboardName, String raceColumnName, String fleetName,
            String markIdAsString, GPSFixDTO fix) {
        boolean result = false;
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        if (leaderboard != null) {
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final TimePoint fixTimePoint = new MillisecondsTimePoint(fix.timepoint);
                final RegattaLog regattaLog = raceColumn.getRegattaLog();
                final BaseRegattaLogDeviceMappingFinder<Mark> mappingFinder = new RegattaLogDeviceMarkMappingFinder(regattaLog);
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    for (final Mark mark : raceColumn.getAvailableMarks(fleet)) {
                        if (mark.getId().toString().equals(markIdAsString)) {
                            result = mappingFinder.hasMappingFor(mark, fixTimePoint);
                            if (result) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    //READ
    public Map<Triple<String, String, String>, Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>> getTrackingTimes(
            Collection<Triple<String, String, String>> leaderboardRaceColumnFleetNames) {
        Map<Triple<String, String, String>, Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>> trackingTimes = new HashMap<>(); 
        for (Triple<String, String, String> leaderboardRaceColumnFleetName : leaderboardRaceColumnFleetNames) {
            try {
                trackingTimes.put(leaderboardRaceColumnFleetName, getTrackingTimes(leaderboardRaceColumnFleetName.getA(), 
                        leaderboardRaceColumnFleetName.getB(), leaderboardRaceColumnFleetName.getC()));
            } catch (Exception e) {
                trackingTimes.put(leaderboardRaceColumnFleetName, null);
            }
        }
        return trackingTimes;
    }

    @Override
  //READ
    public Collection<CompetitorDTO> getEliminatedCompetitors(String leaderboardName) {
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        if (leaderboard == null || !(leaderboard instanceof RegattaLeaderboardWithEliminations)) {
            throw new IllegalArgumentException(leaderboardName+" does not match a regatta leaderboard with eliminations");
        }
        final RegattaLeaderboardWithEliminations rlwe = (RegattaLeaderboardWithEliminations) leaderboard;
        return convertToCompetitorDTOs(rlwe.getEliminatedCompetitors());
    }
    
    @Override
  //READ
  public Iterable<DetailType> determineDetailTypesForCompetitorChart(String leaderboardGroupName,
          UUID leaderboardGroupId, RegattaAndRaceIdentifier identifier) {
        final LinkedHashSet<DetailType> availableDetailTypes = new LinkedHashSet<>();
        availableDetailTypes.addAll(DetailType.getAutoplayDetailTypesForChart());
        availableDetailTypes.removeAll(DetailType.getRaceBravoDetailTypes());
        final DynamicTrackedRace trackedRace = getService().getTrackedRace(identifier);
        if (trackedRace != null) {
            boolean hasBravoTrack = false;
            boolean hasExtendedBravoFixes = false;
            for (BravoFixTrack<Competitor> track : trackedRace.<BravoFix, BravoFixTrack<Competitor>>getSensorTracks(BravoFixTrack.TRACK_NAME)) {
                hasBravoTrack = true;
                if (track.hasExtendedFixes()) {
                    hasExtendedBravoFixes = true;
                    break;
                }
            }
            if (hasBravoTrack) {
                availableDetailTypes.addAll(DetailType.getRaceBravoDetailTypes());
            }
            if (hasExtendedBravoFixes) {
                availableDetailTypes.addAll(DetailType.getRaceExtendedBravoDetailTypes());
                availableDetailTypes.addAll(DetailType.getRaceExpeditionDetailTypes());
            }
            final RankingMetrics rankingMetricType = trackedRace.getRankingMetric().getType();
            switch (rankingMetricType) {
            case ONE_DESIGN:
                availableDetailTypes.removeAll(DetailType.getAllToTToDHandicapDetailTypes());
                availableDetailTypes.removeAll(DetailType.getAllOrcPerformanceCurveDetailTypes());
                break;
            case TIME_ON_TIME_AND_DISTANCE:
                availableDetailTypes.addAll(DetailType.getAllToTToDHandicapDetailTypes());
                availableDetailTypes.removeAll(DetailType.getAllOrcPerformanceCurveDetailTypes());
                break;
            case ORC_PERFORMANCE_CURVE:
            case ORC_PERFORMANCE_CURVE_BY_IMPLIED_WIND:
            case ORC_PERFORMANCE_CURVE_LEADER_FOR_BASELINE:
                availableDetailTypes.addAll(DetailType.getAllOrcPerformanceCurveDetailTypes());
                break;
            }
        }
        if (leaderboardGroupName != null) {
            LeaderboardGroupDTO group = getLeaderboardGroupById(leaderboardGroupId);
            if (group == null) {
                group = getLeaderboardGroupByName(leaderboardGroupName, false);
            }
            if (group != null ? group.hasOverallLeaderboard() : false) {
                availableDetailTypes.add(DetailType.OVERALL_RANK);
            }
        }
        return availableDetailTypes;
    }

    @Override
    //READ
    public List<ExpeditionDeviceConfiguration> getExpeditionDeviceConfigurations() {
        final List<ExpeditionDeviceConfiguration> result = new ArrayList<>();
        final ExpeditionTrackerFactory expeditionConnector = expeditionConnectorTracker.getService();
        final Subject subject = SecurityUtils.getSubject();
        if (expeditionConnector != null) {
            for (final ExpeditionDeviceConfiguration config : expeditionConnector.getDeviceConfigurations()) {
                if (subject.isPermitted(config.getIdentifier().getStringPermission(DefaultActions.READ))) {
                    SecurityDTOUtil.addSecurityInformation(getSecurityService(), config, config.getIdentifier());
                    result.add(config);
                }
            }
        }
        return result;
    }

    @Override
    //READ
    public PairingListTemplateDTO calculatePairingListTemplate(final int flightCount, final int groupCount,
            final int competitorCount, final int flightMultiplier, final int tolerance) {
        PairingListTemplate template = getService().createPairingListTemplate(flightCount, groupCount, competitorCount, 
                flightMultiplier, tolerance);
        return new PairingListTemplateDTO(flightCount, groupCount, competitorCount, flightMultiplier, tolerance,
                template.getBoatChanges(), template.getPairingListTemplate(), template.getQuality(), template.getBoatAssignmentsQuality());
    }
    
    @Override
    //READ
    public PairingListDTO getPairingListFromTemplate(final String leaderboardName, final int flightMultiplier,
            final Iterable<String> selectedRaceColumnNames, PairingListTemplateDTO templateDTO) 
            throws NotFoundException, PairingListCreationException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        List<RaceColumn> selectedRaces = new ArrayList<RaceColumn>();
        for (String raceColumnName : selectedRaceColumnNames) {
            for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                if (raceColumnName.equalsIgnoreCase(raceColumn.getName())) {
                    selectedRaces.add(raceColumn);
                }
            }
        }
        PairingListTemplate pairingListTemplate = new PairingListTemplateImpl(templateDTO.getPairingListTemplate(),
                templateDTO.getCompetitorCount(), templateDTO.getFlightMultiplier(), templateDTO.getBoatChangeFactor());
        PairingList<RaceColumn, Fleet, Competitor, Boat> pairingList = getService()
                .getPairingListFromTemplate(pairingListTemplate, leaderboardName, selectedRaces);
        List<List<List<Pair<CompetitorDTO, BoatDTO>>>> result = new ArrayList<>();
        for (RaceColumn raceColumn : selectedRaces) {
            List<List<Pair<CompetitorDTO, BoatDTO>>> raceColumnList = new ArrayList<>();
            for (Fleet fleet : raceColumn.getFleets()) {
                List<Pair<CompetitorDTO, BoatDTO>> fleetList = new ArrayList<>();
                for (Pair<Competitor, Boat> competitorAndBoatPair : pairingList.getCompetitors(raceColumn, fleet)) {
                    final Boat boat = competitorAndBoatPair.getB();
                    final CompetitorDTO competitorDTO;
                    if (competitorAndBoatPair.getA() != null) {
                        competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitorAndBoatPair.getA());
                    } else {
                        competitorDTO = null;
                    }
                    final BoatDTO boatDTO;
                    if (boat != null) {
                        boatDTO = new BoatDTO(boat.getId().toString(), boat.getName(), convertToBoatClassDTO(boat.getBoatClass()),
                                boat.getSailID(), boat.getColor());
                    } else {
                        boatDTO = null;
                    }
                    fleetList.add(new Pair<>(competitorDTO, boatDTO));
                }
                raceColumnList.add(fleetList);
            }
            result.add(raceColumnList);
        }
        return new PairingListDTO(result, Util.asList(selectedRaceColumnNames));
    }
    
    @Override
    //READ
    public PairingListDTO getPairingListFromRaceLogs(final String leaderboardName) throws NotFoundException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        List<List<List<Pair<CompetitorDTO, BoatDTO>>>> result = new ArrayList<>();
        List<String> raceColumnNames = new ArrayList<>();
        PairingListLeaderboardAdapter adapter = new PairingListLeaderboardAdapter();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            if (!raceColumn.isMedalRace()) {
                List<List<Pair<CompetitorDTO, BoatDTO>>> raceColumnList = new ArrayList<>();
                for (Fleet fleet : raceColumn.getFleets()) {
                    List<Pair<CompetitorDTO, BoatDTO>> fleetList = new ArrayList<>();
                    for (Pair<Competitor, Boat> competitorAndBoatPair : adapter.getCompetitors(raceColumn, fleet)) {
                        final Boat boat = competitorAndBoatPair.getB();
                        fleetList.add(new Pair<CompetitorDTO, BoatDTO>(baseDomainFactory.convertToCompetitorDTO(competitorAndBoatPair.getA()),
                                new BoatDTO(boat.getId().toString(), boat.getName(),
                                        convertToBoatClassDTO(boat.getBoatClass()), boat.getSailID(), 
                                        boat.getColor())));
                    }
                    if (fleetList.size() > 0) {
                        raceColumnList.add(fleetList);
                    }
                }
                if (raceColumnList.size() > 0) {
                    result.add(raceColumnList);
                    if (!raceColumnNames.contains(raceColumn.getName())) {
                        raceColumnNames.add(raceColumn.getName());
                    }
                }
            }
        }
        return new PairingListDTO(result, raceColumnNames);
    }
    
    //READ
    public List<String> getRaceDisplayNamesFromLeaderboard(String leaderboardName,List<String> raceColumnNames) throws NotFoundException {
        Leaderboard leaderboard = this.getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        List<String> result = new ArrayList<>();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            if (raceColumn.hasTrackedRaces()) {
                if (raceColumnNames.contains(raceColumn.getName())) {
                    for (Fleet fleet : raceColumn.getFleets()) {
                        final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                        if (getSecurityService().hasCurrentUserReadPermission(trackedRace)) {
                            if (trackedRace != null && trackedRace.getRaceIdentifier() != null) {
                                result.add(trackedRace.getRaceIdentifier().getRaceName());
                            } else {
                                break;
                            }
                        }
                    }
                }
            } else {
                break;
            }
        }
        if (result.size()==raceColumnNames.size()*Util.size(Util.get(leaderboard.getRaceColumns(), 0).getFleets())) {
            return result;
        }
        result.clear();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            for (Fleet fleet: raceColumn.getFleets()) {
                final RaceLog raceLog = raceColumn.getRaceLog(fleet);
                raceLog.lockForRead();
                try {
                    final NavigableSet<RaceLogEvent> set = raceLog.getUnrevokedEvents();
                    for (RaceLogEvent raceLogEvent : set) {
                        if (raceLogEvent instanceof RaceLogDenoteForTrackingEvent) {
                            RaceLogDenoteForTrackingEvent denoteEvent = (RaceLogDenoteForTrackingEvent) raceLogEvent;
                            result.add(denoteEvent.getRaceName());
                            break;
                        }
                    }
                } finally {
                    raceLog.unlockAfterRead();
                }
            }
        }
        if (result.size()==raceColumnNames.size()*Util.size(Util.get(leaderboard.getRaceColumns(), 0).getFleets())) {
            return result;
        }
        result.clear();
        for (int count=1;count<=raceColumnNames.size()*Util.size(Util.get(leaderboard.getRaceColumns(), 0).getFleets());count++) {
            result.add("Race "+count);
        }
        return result;
    }

    @Override
    //READ
    public Iterable<DetailType> getAvailableDetailTypesForLeaderboard(String leaderboardName, RegattaAndRaceIdentifier raceIdentifierOrNull) {
        final Set<DetailType> allowed = new HashSet<>();
        allowed.addAll(DetailType.getAllNonRestrictedDetailTypes());
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            getSecurityService().checkCurrentUserReadPermission(leaderboard);
            if (leaderboard instanceof RegattaLeaderboard) {
                final RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                final Regatta regatta = regattaLeaderboard.getRegatta();
                switch (regatta.getRankingMetricType()) {
                case TIME_ON_TIME_AND_DISTANCE:
                    allowed.addAll(DetailType.getAllToTToDHandicapDetailTypes());
                    break;
                case ORC_PERFORMANCE_CURVE:
                case ORC_PERFORMANCE_CURVE_BY_IMPLIED_WIND:
                case ORC_PERFORMANCE_CURVE_LEADER_FOR_BASELINE:
                    allowed.addAll(DetailType.getAllOrcPerformanceCurveDetailTypes());
                    break;
                case ONE_DESIGN:
                    break; // no additional columns for one-design
                }
            }
            boolean hasBravoTrack = false;
            boolean hasExtendedBravoFixes = false;
            abort: for (RaceColumn race : leaderboard.getRaceColumns()) {
                for (Fleet fleet : race.getFleets()) {
                    if (raceIdentifierOrNull != null && !raceIdentifierOrNull.equals(race.getRaceIdentifier(fleet))) {
                        continue;
                    }
                    final TrackedRace trace = race.getTrackedRace(fleet);
                    if (trace != null) {
                        final DynamicTrackedRace trackedRace = getService().getTrackedRace(trace.getRaceIdentifier());
                        if (trackedRace != null) {
                            for (BravoFixTrack<Competitor> track : trackedRace
                                    .<BravoFix, BravoFixTrack<Competitor>>getSensorTracks(BravoFixTrack.TRACK_NAME)) {
                                hasBravoTrack = true;
                                if (track.hasExtendedFixes()) {
                                    hasExtendedBravoFixes = true;
                                    break abort;
                                }
                            }
                        }
                    }
                }
            }
            if (hasBravoTrack) {
                allowed.addAll(DetailType.getRaceBravoDetailTypes());
                allowed.addAll(DetailType.getLegBravoDetailTypes());
                allowed.addAll(DetailType.getOverallBravoDetailTypes());
            }
            if (hasExtendedBravoFixes) {
                allowed.addAll(DetailType.getRaceExpeditionDetailTypes());
                allowed.addAll(DetailType.getLegExpeditionDetailColumnTypes());
            }
        }
        allowed.removeAll(DetailType.getDisabledDetailColumTypes());
        return allowed;
    }

    //READ
    public SpotDTO getWindFinderSpot(String spotId) throws MalformedURLException, IOException, org.json.simple.parser.ParseException, InterruptedException, ExecutionException {
        final SpotDTO result;
        final WindFinderTrackerFactory windFinderTrackerFactory = windFinderTrackerFactoryServiceTracker.getService();
        if (windFinderTrackerFactory != null) {
            final Spot spot = windFinderTrackerFactory.getSpotById(spotId, /* cached */ false);
            if (spot != null) {
                result = new SpotDTO(spot);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }
    
    //READ
    protected boolean isSmartphoneTrackingEnabled(DynamicTrackedRace trackedRace) {
        boolean result = false;
        for (RaceLog raceLog : trackedRace.getAttachedRaceLogs()) {
            RaceLogTrackingState raceLogTrackingState = new RaceLogTrackingStateAnalyzer(raceLog).analyze();
            if (raceLogTrackingState.isTracking()) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    @Override
    //READ
    public SliceRacePreperationDTO prepareForSlicingOfRace(final RegattaAndRaceIdentifier raceIdentifier) {
        getSecurityService().checkCurrentUserReadPermission(raceIdentifier);
        final Leaderboard regattaLeaderboard = getService().getLeaderboardByName(raceIdentifier.getRegattaName());
        String prefix = null;
        int currentCount = 0;
        final Pattern pattern = Pattern.compile("^([a-zA-Z_ -]+)([0-9]+)$");
        final HashSet<String> alreadyUsedRaceNames = new HashSet<>();
        for (RaceColumn column : regattaLeaderboard.getRaceColumns()) {
            alreadyUsedRaceNames.add(column.getName());
            final Matcher matcher = pattern.matcher(column.getName());
            if (matcher.matches()) {
                prefix = matcher.group(1);
                currentCount = Integer.parseInt(matcher.group(2));
            }
        }
        if (prefix == null) {
            prefix = "R";
        }
        currentCount++;
        return new SliceRacePreperationDTO(prefix + currentCount, alreadyUsedRaceNames);
    }
    
    @Override
  //READ
    public Boolean checkIfRaceIsTracking(RegattaAndRaceIdentifier race) {
        getSecurityService().checkCurrentUserReadPermission(race);
        boolean result = false;
        DynamicTrackedRace trace = getService().getTrackedRace(race);
        if (trace != null) {
            final TrackedRaceStatusEnum status = trace.getStatus().getStatus();
            if (status == TrackedRaceStatusEnum.LOADING || status == TrackedRaceStatusEnum.TRACKING) {
                result = true;
            }
        }
        return result;
    }

    @Override
  //READ
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        ShardingType identifiedShardingType = null;
        try {
            if (req instanceof HttpServletRequest) {
                identifiedShardingType = ShardingContext.identifyAndSetShardingConstraint(((HttpServletRequest) req).getPathInfo());
            }
            super.service(req, res);
        } finally {
            if (identifiedShardingType != null) {
                ShardingContext.clearShardingConstraint(identifiedShardingType);
            }
        }
    }

    /**
     * Takes a list of source URLs, the resizing task and the sizes of the resized images to create a ImageDTO for every
     * resized image
     * 
     * @author Robin Fleige (D067799)
     * 
     * @param sourceRefs
     *            list of source URLs
     * @param resizingTask
     *            the resizing task, with information about resizes and the original ImageDTO
     * @param images
     *            the BufferedImages, used to get their width and height
     * @returns a List of ImageDTOs that contains an ImageDTO per resized image
     */
    //READ
    protected Set<ImageDTO> createImageDTOsFromURLsAndResizingTask(final List<String> sourceRefs,
            final ImageResizingTaskDTO resizingTask, final List<BufferedImage> images) {
        final Set<ImageDTO> imageDTOs = new HashSet<ImageDTO>();
        for (int i = 0; i < sourceRefs.size(); i++) {
            final ImageDTO imageDTO = resizingTask.cloneImageDTO();
            for (MediaTagConstants tag : MediaTagConstants.values()) {
                imageDTO.getTags().remove(tag.getName());
            }
            imageDTO.getTags().add(resizingTask.getResizingTask().get(i).getName());
            imageDTO.setSourceRef(sourceRefs.get(i));
            imageDTO.setSizeInPx(images.get(i).getWidth(), images.get(i).getHeight());
            imageDTOs.add(imageDTO);
        }
        return imageDTOs;
    }

    @Override
  //READ
    public List<TagDTO> getPrivateTags(String leaderboardName, String raceColumnName, String fleetName) {
        List<TagDTO> result = new ArrayList<TagDTO>();
        try {
            result.addAll(getService().getTaggingService().getPrivateTags(leaderboardName, raceColumnName, fleetName));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Problem obtaining private tags for leaderboard "+leaderboardName+", race column "+raceColumnName+
                    ", fleet "+fleetName, e);
            // do nothing as method will always return at least an empty list
        }
        return result;
    }

    @Override
  //READ
    public RaceTimesInfoDTO getRaceTimesInfoIncludingTags(RegattaAndRaceIdentifier raceIdentifier,
            TimePoint searchSince) {
        RaceTimesInfoDTO raceTimesInfo = getRaceTimesInfo(raceIdentifier);
        raceTimesInfo.setTags(getService().getTaggingService().getPublicTags(raceIdentifier, searchSince));
        return raceTimesInfo;
    }

    @Override
  //READ
    public List<RaceTimesInfoDTO> getRaceTimesInfosIncludingTags(Collection<RegattaAndRaceIdentifier> raceIdentifiers,
            Map<RegattaAndRaceIdentifier, TimePoint> searchSinceMap) {
        List<RaceTimesInfoDTO> raceTimesInfos = new ArrayList<RaceTimesInfoDTO>();
        for (RegattaAndRaceIdentifier raceIdentifier : raceIdentifiers) {
            RaceTimesInfoDTO raceTimesInfo = getRaceTimesInfoIncludingTags(raceIdentifier,
                    searchSinceMap.get(raceIdentifier));
            if (raceTimesInfo != null) {
                raceTimesInfos.add(raceTimesInfo);
            }
        }
        return raceTimesInfos;
    }

    @Override
  //READ
    public MailInvitationType getMailType() {
        MailInvitationType type = MailInvitationType
                .valueOf(System.getProperty(MAILTYPE_PROPERTY, MailInvitationType.SailInsight3.name()));
        return type;
    }

    //READ
    public String openRegattaRegistrationQrCode(String url) {
        String result = "";
        try (DataInputStream imageIs = new DataInputStream(QRCodeGenerationUtil.create(url, 600, "H"))) {
            byte[] targetArray = new byte[imageIs.available()];
            imageIs.readFully(targetArray);
            result = Base64Utils.toBase64(targetArray);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while generating QR code for open regatta", e);
        }
        return result;
    }

  //READ
    private Boolean isSelfServiceServer() {
        final AccessControlListAnnotation serverAclOrNull = getSecurityService().getAccessControlList(getServerInfo().getIdentifier());
        final Boolean result;
        if (serverAclOrNull == null) {
            result = false;
        } else {
            final AccessControlList acl = serverAclOrNull.getAnnotation();
            final Set<String> grantedActionsForNullGroup = acl.getActionsByUserGroup().get(null);
            if (grantedActionsForNullGroup == null) {
                result = false;
            } else {
                result = grantedActionsForNullGroup.contains(ServerActions.CREATE_OBJECT.name());
            }
        }
        return result;
    }

  //READ
    private Boolean isPublicServer() {
        final Boolean result;
        final RoleDefinition viewerRole = getSecurityService()
                .getRoleDefinition(SailingViewerRole.getInstance().getId());
        final UserGroup defaultServerTenant = getSecurityService().getServerGroup();
        if (viewerRole != null && defaultServerTenant != null) {
            result = defaultServerTenant.getRoleAssociation(viewerRole);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    //READ
    public BoatDTO getBoat(UUID boatId, String regattaName, String regattaRegistrationLinkSecret) {
        BoatDTO result = null;
        Leaderboard leaderboard = getService().getLeaderboardByName(regattaName);
        if (leaderboard != null && leaderboard instanceof RegattaLeaderboard) {
            boolean skipSecuritychecks = getService().skipChecksDueToCorrectSecret(regattaName,
                    regattaRegistrationLinkSecret);
            if (skipSecuritychecks || getService().getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                for (Boat boat : leaderboard.getAllBoats()) {
                    if (skipSecuritychecks || getService().getSecurityService().hasCurrentUserReadPermission(boat)) {
                        if (Util.equalsWithNull(boatId, boat.getId())) {
                            result = getBaseDomainFactory().convertToBoatDTO(boat);
                        }
                    }

                }
            }

        }
        return result;
    }

    @Override
    //READ
    public MarkDTO getMark(UUID markId, String regattaName, String regattaRegistrationLinkSecret) {
        MarkDTO result = null;
        Leaderboard leaderboard = getService().getLeaderboardByName(regattaName);
        if (leaderboard != null && leaderboard instanceof RegattaLeaderboard) {
            boolean skipSecuritychecks = getService().skipChecksDueToCorrectSecret(regattaName,
                    regattaRegistrationLinkSecret);
            if (skipSecuritychecks || getService().getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                for (final RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                    for (final Mark availableMark : raceColumn.getAvailableMarks()) {
                        if (Util.equalsWithNull(availableMark.getId(), markId)) {
                            result = convertToMarkDTO((RegattaLeaderboard) leaderboard, availableMark);
                            break;
                        }
                    }
                }
            }

        }
        return result;
    }

    @Override
  //READ
    public QRCodeEvent getEvent(UUID eventId, String regattaName, String regattaRegistrationLinkSecret) {
        QRCodeEvent result = null;
        Event event = getService().getEvent(eventId);
        if (event != null) {
            boolean skipSecuritychecks = getService().skipChecksDueToCorrectSecret(regattaName,
                    regattaRegistrationLinkSecret);
            if (skipSecuritychecks || getService().getSecurityService().hasCurrentUserReadPermission(event)) {
                ImageDescriptor logoImage = event.findImageWithTag(MediaTagConstants.LOGO.getName());
                ImageDTO logo = logoImage != null ? HomeServiceUtil.convertToImageDTO(logoImage) : null;
                String name = HomeServiceUtil.getEventDisplayName(event, getService());
                String location = HomeServiceUtil.getLocation(event, getService());
                if ((location == null || location.isEmpty()) && event.getVenue() != null) {
                    location = event.getVenue().getName();
                }
                result = new QRCodeEvent(name, location, logo);
            }
        }
        return result;
    }

    @Override
  //READ
    public CompetitorDTO getCompetitor(UUID competitorId, String regattaName, String regattaRegistrationLinkSecret) {
        CompetitorDTO result = null;
        Leaderboard leaderboard = getService().getLeaderboardByName(regattaName);
        if (leaderboard != null && leaderboard instanceof RegattaLeaderboard) {
            boolean skipSecuritychecks = getService().skipChecksDueToCorrectSecret(regattaName,
                    regattaRegistrationLinkSecret);
            if (skipSecuritychecks || getService().getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                for (Competitor competitor : leaderboard.getAllCompetitors()) {
                    if (skipSecuritychecks
                            || getService().getSecurityService().hasCurrentUserReadPermission(competitor)) {
                        if (Util.equalsWithNull(competitorId, competitor.getId())) {
                            result = getBaseDomainFactory().convertToCompetitorDTO(competitor);
                        }
                    }
                }
            }

        }
        return result;
    }

    @Override
    //READ
    public boolean getTrackedRaceIsUsingMarkPassingCalculator(RegattaAndRaceIdentifier regattaNameAndRaceName) {
        return getExistingTrackedRace(regattaNameAndRaceName).isUsingMarkPassingCalculator();
    }

    @Override
    //READ
    public ORCPerformanceCurveLegImpl[] getLegGeometry(String leaderboardName, String raceColumnName, String fleetName,
            int[] zeroBasedLegIndices, ORCPerformanceCurveLegTypes[] legTypes) {
        assert zeroBasedLegIndices.length == legTypes.length;
        ORCPerformanceCurveLegImpl[] result = null;
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            getService().getSecurityService().checkCurrentUserReadPermission(leaderboard);
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    final TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                    if (trackedRace != null) {
                        result = new ORCPerformanceCurveLegImpl[zeroBasedLegIndices.length];
                        final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(MillisecondsTimePoint.now());
                        for (int i=0; i<zeroBasedLegIndices.length; i++) {
                            result[i] = getLegGeometry(zeroBasedLegIndices[i], legTypes[i], trackedRace, cache);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    //READ
    public ORCPerformanceCurveLegImpl[] getLegGeometry(RegattaAndRaceIdentifier regattaNameAndRaceName, int[] zeroBasedLegIndices,
            ORCPerformanceCurveLegTypes[] legTypes) {
        final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(MillisecondsTimePoint.now());
        final TrackedRace trackedRace = getExistingTrackedRace(regattaNameAndRaceName);
        getService().getSecurityService().checkCurrentUserReadPermission(trackedRace);
        final ORCPerformanceCurveLegImpl[] result = new ORCPerformanceCurveLegImpl[zeroBasedLegIndices.length];
        for (int i=0; i<zeroBasedLegIndices.length; i++) {
            result[i] = getLegGeometry(zeroBasedLegIndices[i], legTypes[i], trackedRace, cache);
        }
        return result;
    }

    //READ
    private ORCPerformanceCurveLegImpl getLegGeometry(int zeroBasedLegIndex, ORCPerformanceCurveLegTypes legType,
            final TrackedRace trackedRace, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final ORCPerformanceCurveLegImpl result;
        if (trackedRace != null) {
            final Leg leg = trackedRace.getRace().getCourse().getLeg(zeroBasedLegIndex);
            final TrackedLeg trackedLeg = trackedRace.getTrackedLeg(leg);
            final Distance distance = trackedLeg.getWindwardDistance(ORCPerformanceCurveLegTypes.getLegType(legType),
                    trackedLeg.getReferenceTimePoint(), cache);
            Bearing twa;
            try {
                twa = trackedLeg.getTWA(trackedLeg.getReferenceTimePoint());
            } catch (NoWindException e) {
                twa = null;
            }
            result = new ORCPerformanceCurveLegImpl(distance, twa);
        } else {
            result = null;
        }
        return result;
    }

    @Override
  //READ
    public Map<Integer, ORCPerformanceCurveLegImpl> getORCPerformanceCurveLegInfo(String leaderboardName,
            String raceColumnName, String fleetName) throws NotFoundException {
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserReadPermission(leaderboard);
        return getORCPerformanceCurveLegInfo(Collections.singleton(getRaceLog(leaderboardName, raceColumnName, fleetName)));
    }
    
  //READ
    private Map<Integer, ORCPerformanceCurveLegImpl> getORCPerformanceCurveLegInfo(Iterable<RaceLog> raceLogs) {
        final Map<Integer, ORCPerformanceCurveLegImpl> result = new HashMap<>();
        for (final RaceLog raceLog : raceLogs) {
            for (final Entry<Integer, ORCPerformanceCurveLeg> e : new RaceLogORCLegDataAnalyzer(raceLog).analyze().entrySet()) {
                if (e.getValue().getType() == ORCPerformanceCurveLegTypes.TWA) {
                    result.put(e.getKey(), new ORCPerformanceCurveLegImpl(e.getValue().getLength(), e.getValue().getTwa()));
                } else {
                    result.put(e.getKey(), new ORCPerformanceCurveLegImpl(e.getValue().getLength(), e.getValue().getType()));
                }
            }
        }
        return result;
    }

    @Override
  //READ
    public Map<Integer, ORCPerformanceCurveLegImpl> getORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier raceIdentifier) {
        return getORCPerformanceCurveLegInfo(getTrackedRace(raceIdentifier).getAttachedRaceLogs());
    }

    @Override
    //READ
    public Collection<ORCCertificate> getORCCertificates(String json) throws org.json.simple.parser.ParseException, JsonDeserializationException {
        final JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
        final JSONArray certificates = (JSONArray) jsonObject.get(ORCCertificateUploadConstants.CERTIFICATES);
        final ORCCertificateJsonDeserializer deserializer = new ORCCertificateJsonDeserializer();
        final List<ORCCertificate> result = new ArrayList<>();
        for (final Object o : certificates) {
            final JSONObject certificateJson = (JSONObject) o;
            result.add(deserializer.deserialize(certificateJson));
        }
        return result;
    }

    @Override
    //READ
    public Map<String, ORCCertificate> getORCCertificateAssignmentsByBoatIdAsString(RegattaIdentifier regattaIdentifier) throws NotFoundException {
        final Regatta regatta = (Regatta) regattaIdentifier.getRegatta(getService());
        if (regatta == null) {
            throw new NotFoundException("Regatta named "+regattaIdentifier+" not found");
        }
        getService().getSecurityService().checkCurrentUserReadPermission(regatta);
        final Map<String, ORCCertificate> result = new HashMap<>();
        final Map<Serializable, Boat> boatsById = new HashMap<>();
        for (final Boat boat : regatta.getAllBoats()) {
            boatsById.put(boat.getId(), boat);
        }
        for (final Entry<Boat, ORCCertificate> e : new RegattaLogORCCertificateAssignmentFinder(regatta.getRegattaLog(), boatsById).analyze().entrySet()) {
            result.put(e.getKey().getId().toString(), e.getValue());
        }
        return result;
    }

    @Override
    //READ
    public Map<String, ORCCertificate> getORCCertificateAssignmentsByBoatIdAsString(String leaderboardName,
            String raceColumnName, String fleetName) throws NotFoundException {
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserReadPermission(leaderboard);
        final Map<String, ORCCertificate> result = new HashMap<>();
        final Map<Serializable, Boat> boatsById = new HashMap<>();
        for (final Boat boat : leaderboard.getAllBoats()) {
            boatsById.put(boat.getId(), boat);
        }
        for (final Entry<Boat, ORCCertificate> e : new RaceLogORCCertificateAssignmentFinder(raceLog, boatsById).analyze().entrySet()) {
            result.put(e.getKey().getId().toString(), e.getValue());
        }
        return result;
    }

    @FunctionalInterface
    public
    //??
    static interface LogEventConstructor<LogEventT extends AbstractLogEvent<VisitorT>, VisitorT> {
        LogEventT create(TimePoint createdAt, TimePoint logicalTimePoint, AbstractLogEventAuthor author, Serializable pId, ORCCertificate certificate, Boat boat);
    }

    @Override
    //READ
    public ImpliedWindSource getImpliedWindSource(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException {
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserReadPermission(leaderboard);
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final ImpliedWindSource impliedWindSource = new RaceLogORCImpliedWindSourceFinder(raceLog).analyze();
        return impliedWindSource;
    }

    @Override
    //READ
    public void setImpliedWindSource(String leaderboardName, String raceColumnName, String fleetName, ImpliedWindSource impliedWindSource) throws NotFoundException {
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final TimePoint now = MillisecondsTimePoint.now();
        final RaceLogEvent impliedWindSourceEvent = new RaceLogORCImpliedWindSourceEventImpl(now, now, getService().getServerAuthor(), UUID.randomUUID(), raceLog.getCurrentPassId(), impliedWindSource);
        raceLog.add(impliedWindSourceEvent);
    }
    
    @Override
    //READ
    public Map<BoatDTO, Set<ORCCertificate>> getSuggestedORCBoatCertificates(ArrayList<BoatDTO> boats) throws InterruptedException, ExecutionException {
        final ORCPublicCertificateDatabase db = ORCPublicCertificateDatabase.INSTANCE;
        final Map<BoatDTO, Set<ORCCertificate>> result = new HashMap<>();
        final Map<BoatDTO, Future<Set<ORCCertificate>>> futures = new HashMap<>();
        for (final BoatDTO boat : boats) {
            futures.put(boat, db.search(boat.getName(), boat.getSailId(),
                    getService().getBaseDomainFactory().getBoatClass(boat.getBoatClass().getName())));
        }
        for (final Entry<BoatDTO, Future<Set<ORCCertificate>>> boatAndFutures : futures.entrySet()) {
            final Set<ORCCertificate> certificatesForBoat = new HashSet<>();
            certificatesForBoat.addAll(boatAndFutures.getValue().get());
            result.put(boatAndFutures.getKey(), certificatesForBoat);
        }
        return result;
    }

    @Override
    //READ
    public Set<ORCCertificate> searchORCBoatCertificates(CountryCode country, Integer yearOfIssuance,
            String referenceNumber, String yachtName, String sailNumber, String boatClassName) throws Exception {
        final ORCPublicCertificateDatabase db = ORCPublicCertificateDatabase.INSTANCE;
        final Set<ORCCertificate> result = new HashSet<>();
        final Iterable<CertificateHandle> searchResult = db.search(country, yearOfIssuance, referenceNumber, yachtName, sailNumber, boatClassName, /* includeInvalid */ false);
        Util.addAll(
                db.getCertificates(searchResult),
                result);
        result.remove(null); // in case some certificate wasn't found by reference number
        return result;
    }

    @Override
    //READ
    public List<MarkTemplateDTO> getMarkTemplates() {
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                getSharedSailingData().getAllMarkTemplates(), m -> convertToMarkTemplateDTO(m));
    }

    protected MarkTemplateDTO convertToMarkTemplateDTO(MarkTemplate markTemplate) {
        final MarkTemplateDTO markTemplateDTO = new MarkTemplateDTO(markTemplate.getId(), markTemplate.getName(),
                markTemplate.getShortName() == null || markTemplate.getShortName().isEmpty() ? markTemplate.getName()
                        : markTemplate.getShortName(),
                markTemplate.getColor(), markTemplate.getShape(), markTemplate.getPattern(), markTemplate.getType());

        SecurityDTOUtil.addSecurityInformation(getSecurityService(), markTemplateDTO, markTemplate.getIdentifier());
        return markTemplateDTO;
    }

    //READ
    protected CommonMarkProperties convertDtoToCommonMarkProperties(CommonMarkPropertiesDTO markProperties) {
        return new CommonMarkPropertiesImpl(markProperties.getName(), markProperties.getShortName(),
                markProperties.getColor(), markProperties.getShape(), markProperties.getPattern(),
                markProperties.getType());
    }

    //READ
    protected DeviceIdentifier convertDtoToDeviceIdentifier(DeviceIdentifierDTO deviceIdentifier) {
        return deviceIdentifier != null ? new SmartphoneUUIDIdentifierImpl(UUID.fromString(deviceIdentifier.deviceId))
                : null;
    }

    @Override
    //READ
    public List<MarkPropertiesDTO> getMarkProperties() {
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                getSharedSailingData().getAllMarkProperties(), m -> convertToMarkPropertiesDTO(m));
    }

    //READ
    protected MarkPropertiesDTO convertToMarkPropertiesDTO(MarkProperties markProperties) {
        final MarkPropertiesDTO markPropertiesDto = new MarkPropertiesDTO(markProperties.getId(),
                markProperties.getName(), markProperties.getTags(), markProperties.getShortName(),
                markProperties.getColor(), markProperties.getShape(), markProperties.getPattern(),
                markProperties.getType(), markProperties.getPositioningInformation() == null ? null :
                    markProperties.getPositioningInformation().accept(new PositioningVisitor<String>() {
                        @Override
                        public String visit(FixedPositioning fixedPositioning) {
                            return "FIXED_POSITION";
                        }

                        @Override
                        public String visit(TrackingDeviceBasedPositioning trackingDeviceBasedPositioning) {
                            return "DEVICE";
                        }
                    }));
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), markPropertiesDto, markProperties.getIdentifier());
        return markPropertiesDto;
    }

    @Override
    //READ
    public List<CourseTemplateDTO> getCourseTemplates() {
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(
                getSharedSailingData().getAllCourseTemplates(), m -> convertToCourseTemplateDTO(m));
    }

    //READ
    public CourseTemplateDTO convertToCourseTemplateDTO(CourseTemplate courseTemplate) {
        final Map<MarkTemplateDTO, MarkRoleDTO> convertedDefaultMarkRolesForMarkTemplates = courseTemplate.getDefaultMarkRolesForMarkTemplates().entrySet()
                .stream().collect(Collectors.toMap(entry -> convertToMarkTemplateDTO(entry.getKey()),
                        entry -> convertToMarkRoleDTO(entry.getValue())));
        final Map<MarkRoleDTO, MarkTemplateDTO> convertedDefaultMarkTemplatesForMarkRoles = courseTemplate.getDefaultMarkTemplatesForMarkRoles().entrySet()
                .stream().collect(Collectors.toMap(entry -> convertToMarkRoleDTO(entry.getKey()),
                        entry -> convertToMarkTemplateDTO(entry.getValue())));
        final List<MarkTemplateDTO> convertedMarkTemplates = StreamSupport
                .stream(courseTemplate.getMarkTemplates().spliterator(), false).map(this::convertToMarkTemplateDTO)
                .collect(Collectors.toList());
        final List<WaypointTemplateDTO> convertedWaypointTemplates = StreamSupport
                .stream(courseTemplate.getWaypointTemplates().spliterator(), false)
                .map(this::convertToWaypointTemplateDTO).collect(Collectors.toList());
        // convert optional image url
        final String optionalImageURL = courseTemplate.getOptionalImageURL() != null
                ? courseTemplate.getOptionalImageURL().toExternalForm()
                : null;
        final CourseTemplateDTO result = new CourseTemplateDTO(courseTemplate.getId(), courseTemplate.getName(),
                courseTemplate.getShortName(), convertedMarkTemplates, convertedWaypointTemplates,
                convertedDefaultMarkRolesForMarkTemplates, convertedDefaultMarkTemplatesForMarkRoles, optionalImageURL,
                courseTemplate.getTags(),
                convertToRepeatablePartDTO(courseTemplate.getRepeatablePart()), courseTemplate.getDefaultNumberOfLaps());
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), result, courseTemplate.getIdentifier());
        return result;
    }

    //READ
    private RepeatablePartDTO convertToRepeatablePartDTO(RepeatablePart repeatablePart) {
        return repeatablePart != null
                ? new RepeatablePartDTO(repeatablePart.getZeroBasedIndexOfRepeatablePartStart(),
                        repeatablePart.getZeroBasedIndexOfRepeatablePartEnd())
                : null;
    }

    //READ
    private WaypointTemplateDTO convertToWaypointTemplateDTO(WaypointTemplate waypointTemplate) {
        return new WaypointTemplateDTO(waypointTemplate.getControlPointTemplate().getName(),
                waypointTemplate.getControlPointTemplate().getShortName(),
                StreamSupport.stream(waypointTemplate.getControlPointTemplate().getMarkRoles().spliterator(), false)
                        .map(this::convertToMarkRoleDTO).collect(Collectors.toList()),
                waypointTemplate.getPassingInstruction());
    }

    @Override
    //READ
    public void removeCourseTemplates(Collection<UUID> courseTemplateUuids) {
        for (UUID uuid : courseTemplateUuids) {
            getSharedSailingData().deleteCourseTemplate(getSharedSailingData().getCourseTemplateById(uuid));
        }
    }

    //READ
    protected WaypointTemplate convertToWaypointTemplate(WaypointTemplateDTO waypointTemplate, final MarkRolePairFactory markRolePairFactory) {
        final List<MarkRole> resolvedMarkRoles = waypointTemplate.getMarkRolesForControlPoint().stream()
                .map(t -> getSharedSailingData().getMarkRoleById(t.getUuid())).collect(Collectors.toList());
        final ControlPointTemplate controlPointTemplate;
        if (resolvedMarkRoles.size() == 1) {
            controlPointTemplate = resolvedMarkRoles.get(0);
        } else if (resolvedMarkRoles.size() == 2) {
            controlPointTemplate = markRolePairFactory.create(waypointTemplate.getName(),
                    waypointTemplate.getShortName(), resolvedMarkRoles.get(0), resolvedMarkRoles.get(1));
        } else {
            throw new IllegalArgumentException("Waypoints must contain one or two marks");
        }
        
        return new WaypointTemplateImpl(controlPointTemplate, waypointTemplate.getPassingInstruction());
    }

    //READ
    protected RepeatablePart convertToRepeatablePart(RepeatablePartDTO repeatablePart) {
        return new RepeatablePartImpl(repeatablePart.getZeroBasedIndexOfRepeatablePartStart(),
                repeatablePart.getZeroBasedIndexOfRepeatablePartEnd());
    }

    //READ
    protected MarkRoleDTO convertToMarkRoleDTO(final MarkRole markRole) {
        final MarkRoleDTO markRoleDTO = new MarkRoleDTO(markRole.getId(), markRole.getName(), markRole.getShortName());
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), markRoleDTO, markRole.getIdentifier());
        return markRoleDTO;
    }

    @Override
    //READ
    public List<MarkRoleDTO> getMarkRoles() {
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(getSharedSailingData().getAllMarkRoles(),
                m -> convertToMarkRoleDTO(m));
    }

    @Override
    public EventDTO getEventById(UUID id, boolean withStatisticalData) throws MalformedURLException, UnauthorizedException {
        EventDTO result = null;
        Event event = getService().getEvent(id);
        if (event != null) {
            if (SecurityUtils.getSubject()
                    .isPermitted(SecuredDomainType.EVENT.getStringPermissionForObject(DefaultActions.READ, event))) {
                result = convertToEventDTO(event, withStatisticalData);
                result.setBaseURL(getEventBaseURLFromEventOrRequest(event));
                result.setIsOnRemoteServer(false);
            } else {
                throw new UnauthorizedException("You are not permitted to view event " + id);
            }
        }
        return result;
    
    }
}
