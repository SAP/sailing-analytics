package com.sap.sailing.gwt.ui.server;

import java.awt.image.BufferedImage;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.imageio.metadata.IIOMetadata;
import javax.management.InvalidAttributeValueException;

import org.apache.http.client.ClientProtocolException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.BaseORCCertificateAssignmentAnalyzer;
import com.sap.sailing.domain.abstractlog.orc.ORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCImpliedWindSourceEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataAnalyzer;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEventFinder;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCScratchBoatAnalyzer;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCScratchBoatEvent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCScratchBoatFinder;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCImpliedWindSourceEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCLegDataEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCScratchBoatEventImpl;
import com.sap.sailing.domain.abstractlog.orc.impl.RegattaLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogDependentStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEndOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFixedMarkPassingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPathfinderEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogSuppressedMarkPassingsEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.FinishedTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.FinishingTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastPublishedCourseDesignFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.TrackingTimesEventFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.TrackingTimesFinder;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogDependentStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogExcludeWindSourcesEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningListChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFixedMarkPassingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogGateLineOpeningTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPathfinderEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogProtestStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartProcedureChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogSuppressedMarkPassingsEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.abstractlog.race.scoring.RaceLogAdditionalScoringInformationEvent;
import com.sap.sailing.domain.abstractlog.race.scoring.impl.RaceLogAdditionalScoringInformationEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogUseCompetitorsFromRaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDenoteForTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogStartTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogUseCompetitorsFromRaceLogEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogCloseOpenEndedDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceBoatMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.BaseRegattaLogDeviceMappingFinder;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMappingFinder;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMarkMappingFinder;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogOpenEndedDeviceMappingCloser;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitorWithBoat;
import com.sap.sailing.domain.base.impl.DynamicPerson;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.EventBaseImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.SailingServerConfigurationImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.MailInvitationType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.NotFoundException;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.ServiceException;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.UnableToCloseDeviceMappingException;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.PairingListDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.tracking.CompetitorRegistrationOnRaceLogDisabledException;
import com.sap.sailing.domain.common.racelog.tracking.DoesNotHaveRegattaLogException;
import com.sap.sailing.domain.common.racelog.tracking.MarkAlreadyUsedInRaceException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotableForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.security.SecuredDomainType.EventActions;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.common.tagging.TagAlreadyExistsException;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.coursetemplate.CourseTemplate;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkRole;
import com.sap.sailing.domain.coursetemplate.MarkRolePair.MarkRolePairFactory;
import com.sap.sailing.domain.coursetemplate.MarkTemplate;
import com.sap.sailing.domain.coursetemplate.WaypointTemplate;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotStandardCommand;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogtracking.DeviceMapping;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.regattalike.HasRegattaLike;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.regattalike.LeaderboardThatHasRegattaLike;
import com.sap.sailing.domain.resultimport.ResultUrlProvider;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingArchiveConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tractracadapter.RaceRecord;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.yellowbrickadapter.YellowBrickConfiguration;
import com.sap.sailing.domain.yellowbrickadapter.YellowBrickTrackingAdapter;
import com.sap.sailing.expeditionconnector.ExpeditionDeviceConfiguration;
import com.sap.sailing.expeditionconnector.ExpeditionSensorDeviceIdentifier;
import com.sap.sailing.gwt.ui.adminconsole.RaceLogSetTrackingTimesDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceWrite;
import com.sap.sailing.gwt.ui.client.shared.SailingVideoDTO;
import com.sap.sailing.gwt.ui.client.shared.charts.MarkPositionService.MarkTrackDTO;
import com.sap.sailing.gwt.ui.shared.BulkScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO.RegattaConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceIdentifierDTO;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.IgtimiDataAccessWindowWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.IgtimiDeviceWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.MigrateGroupOwnerForHierarchyDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetFinishingAndFinishTimeDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.RemoteSailingServerReferenceDTO;
import com.sap.sailing.gwt.ui.shared.SailingImageDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sailing.gwt.ui.shared.ServerConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingArchiveConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.TrackFileImportDeviceIdentifierDTO;
import com.sap.sailing.gwt.ui.shared.TypedDeviceMappingDTO;
import com.sap.sailing.gwt.ui.shared.UrlDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.YellowBrickConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.YellowBrickRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPropertiesDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sailing.server.hierarchy.SailingHierarchyOwnershipUpdater;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AbstractLeaderboardGroupOperation;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.AddCourseAreas;
import com.sap.sailing.server.operationaltransformation.AddOrReplaceExpeditionDeviceConfiguration;
import com.sap.sailing.server.operationaltransformation.AddRemoteSailingServerReference;
import com.sap.sailing.server.operationaltransformation.AddSpecificRegatta;
import com.sap.sailing.server.operationaltransformation.AllowBoatResetToDefaults;
import com.sap.sailing.server.operationaltransformation.AllowCompetitorResetToDefaults;
import com.sap.sailing.server.operationaltransformation.ConnectTrackedRaceToLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.CreateEvent;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboardWithEliminations;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboardWithOtherTieBreakingLeaderboard;
import com.sap.sailing.server.operationaltransformation.DisconnectLeaderboardColumnFromTrackedRace;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnDown;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnUp;
import com.sap.sailing.server.operationaltransformation.RemoveAndUntrackRace;
import com.sap.sailing.server.operationaltransformation.RemoveColumnFromSeries;
import com.sap.sailing.server.operationaltransformation.RemoveCourseAreas;
import com.sap.sailing.server.operationaltransformation.RemoveEvent;
import com.sap.sailing.server.operationaltransformation.RemoveExpeditionDeviceConfiguration;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sailing.server.operationaltransformation.RemoveRemoteSailingServerReference;
import com.sap.sailing.server.operationaltransformation.RemoveSeries;
import com.sap.sailing.server.operationaltransformation.RenameEvent;
import com.sap.sailing.server.operationaltransformation.RenameLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.SetRaceIsKnownToStartUpwind;
import com.sap.sailing.server.operationaltransformation.SetSuppressedFlagForCompetitorInLeaderboard;
import com.sap.sailing.server.operationaltransformation.StopTrackingRace;
import com.sap.sailing.server.operationaltransformation.UpdateBoat;
import com.sap.sailing.server.operationaltransformation.UpdateCompetitor;
import com.sap.sailing.server.operationaltransformation.UpdateCompetitorDisplayNameInLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateEliminatedCompetitorsInLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateEvent;
import com.sap.sailing.server.operationaltransformation.UpdateIsMedalRace;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardCarryValue;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardColumnFactor;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardIncrementalScoreCorrection;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardMaxPointsReason;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrection;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrectionMetadata;
import com.sap.sailing.server.operationaltransformation.UpdateRaceDelayToLive;
import com.sap.sailing.server.operationaltransformation.UpdateSailingServerReference;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sailing.server.operationaltransformation.UpdateServerConfiguration;
import com.sap.sailing.server.operationaltransformation.UpdateSpecificRegatta;
import com.sap.sailing.server.security.SailingViewerRole;
import com.sap.sailing.server.util.WaitForTrackedRaceUtil;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sse.ServerInfo;
import com.sap.sse.aicore.Credentials;
import com.sap.sse.aicore.CredentialsParser;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.RepeatablePart;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.filestorage.FileStorageManagementService;
import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.filestorage.InvalidPropertiesException;
import com.sap.sse.filestorage.OperationFailedException;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.ImageResizingTaskDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.gwt.server.filestorage.FileStorageServiceDTOUtils;
import com.sap.sse.gwt.shared.filestorage.FileStorageServiceDTO;
import com.sap.sse.gwt.shared.filestorage.FileStorageServicePropertyErrorsDTO;
import com.sap.sse.security.Action;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.ui.server.SecurityDTOUtil;
import com.sap.sse.security.ui.shared.SuccessInfo;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.util.impl.UUIDHelper;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.ImageConverter;
import com.sap.sse.util.ImageConverter.ImageWithMetadata;
import com.sap.sse.util.ThreadPoolUtil;

public class SailingServiceWriteImpl extends SailingServiceImpl implements SailingServiceWrite {

    /** serial version uid */
    private static final long serialVersionUID = -992637440342246674L;

    // WRITE
    public static class TimeoutExtendingInputStream extends FilterInputStream {

        // default timeout is high to ensure that long running client operations
        // such as compressing data will not have the server run into a timeout.
        // this especially applies to foiling data where compression on slower machines
        // can take up to two hours.
        private static final int DEFAULT_TIMEOUT_IN_SECONDS = 60 * 60 * 2;

        private final URLConnection connection;

        public TimeoutExtendingInputStream(InputStream in, URLConnection connection) {
            super(in);
            this.connection = connection;
        }

        @Override
        public int read() throws IOException {
            connection.setReadTimeout(DEFAULT_TIMEOUT_IN_SECONDS * 1000);
            return super.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            connection.setReadTimeout(DEFAULT_TIMEOUT_IN_SECONDS * 1000);
            return super.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            connection.setReadTimeout(DEFAULT_TIMEOUT_IN_SECONDS * 1000);
            return super.read(b, off, len);
        }

    }

    @Override
    public MarkRoleDTO createMarkRole(MarkRoleDTO markRole) {
        MarkRole existingMarkRole = getSharedSailingData().getMarkRoleById(markRole.getUuid());
        final MarkRoleDTO result;
        if (existingMarkRole != null) {
            throw new IllegalArgumentException("Mark role with ID " + markRole.getUuid() + " already exists");
        } else {
            result = convertToMarkRoleDTO(
                    getSharedSailingData().createMarkRole(markRole.getName(), markRole.getShortName()));
        }
        return result;
    }

    @Override
    public void removeMarkProperties(Collection<UUID> markPropertiesUuids) {
        for (UUID uuid : markPropertiesUuids) {
            getSharedSailingData().deleteMarkProperties(getSharedSailingData().getMarkPropertiesById(uuid));
        }
    }

    @Override
    public CourseTemplateDTO createOrUpdateCourseTemplate(CourseTemplateDTO courseTemplate) {
        CourseTemplate existingCourseTemplate = getSharedSailingData().getCourseTemplateById(courseTemplate.getUuid());
        URL optionalImageURL = null;
        try {
            optionalImageURL = courseTemplate.getOptionalImageUrl().isPresent()
                    ? new URL(courseTemplate.getOptionalImageUrl().get())
                    : null;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid URL: %s", courseTemplate.getOptionalImageUrl().get()));
        }
        final CourseTemplateDTO result;
        if (existingCourseTemplate != null) {
            getSecurityService().checkCurrentUserUpdatePermission(existingCourseTemplate);
            result = convertToCourseTemplateDTO(getSharedSailingData().updateCourseTemplate(courseTemplate.getUuid(),
                    courseTemplate.getName(), courseTemplate.getShortName(), optionalImageURL, courseTemplate.getTags(),
                    courseTemplate.getDefaultNumberOfLaps()));
        } else {
            final List<MarkTemplate> marks = courseTemplate.getMarkTemplates().stream()
                    .map(t -> getSharedSailingData().getMarkTemplateById(t.getUuid())).collect(Collectors.toList());
            final MarkRolePairFactory markPairTemplateFactory = new MarkRolePairFactory();
            final List<WaypointTemplate> waypoints = courseTemplate.getWaypointTemplates().stream()
                    .map(wp -> convertToWaypointTemplate(wp, markPairTemplateFactory)).collect(Collectors.toList());
            final Map<MarkTemplate, MarkRole> defaultMarkRolesForMarkTemplates = courseTemplate.getDefaultMarkRolesForMarkTemplates().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> getSharedSailingData().getMarkTemplateById(entry.getKey().getUuid()),
                            entry -> getSharedSailingData().getMarkRoleById(entry.getValue().getUuid())));
            final Map<MarkRole, MarkTemplate> defaultMarkTemplatesForMarkRoles = courseTemplate.getDefaultMarkTemplatesForMarkRoles().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> getSharedSailingData().getMarkRoleById(entry.getKey().getUuid()),
                            entry -> getSharedSailingData().getMarkTemplateById(entry.getValue().getUuid())));
            final RepeatablePart optionalRepeatablePart = courseTemplate.getRepeatablePart() != null
                    ? convertToRepeatablePart(courseTemplate.getRepeatablePart())
                    : null;
            result = convertToCourseTemplateDTO(
                    getSharedSailingData().createCourseTemplate(courseTemplate.getName(), courseTemplate.getShortName(), marks,
                            waypoints, defaultMarkRolesForMarkTemplates, defaultMarkTemplatesForMarkRoles,
                            optionalRepeatablePart, courseTemplate.getTags(), optionalImageURL, courseTemplate.getDefaultNumberOfLaps()));
        }
        return result;
    }

    @Override
    public void removeCourseTemplates(Collection<UUID> courseTemplateUuids) {
        for (UUID uuid : courseTemplateUuids) {
            getSharedSailingData().deleteCourseTemplate(getSharedSailingData().getCourseTemplateById(uuid));
        }
    }

    @Override
    public MarkPropertiesDTO updateMarkPropertiesPositioning(UUID markPropertiesId, DeviceIdentifierDTO deviceIdentifier,
            Position fixedPosition) throws NoCorrespondingServiceRegisteredException, TransformationException {
        MarkProperties markProperties = getSharedSailingData().getMarkPropertiesById(markPropertiesId);
        if (deviceIdentifier != null) {
            getSharedSailingData().setTrackingDeviceIdentifierForMarkProperties(markProperties, convertDtoToDeviceIdentifier(deviceIdentifier));
        } else if (fixedPosition != null) {
            getSharedSailingData().setFixedPositionForMarkProperties(markProperties, fixedPosition);
        } else {
            getSharedSailingData().clearPositioningForMarkProperties(markProperties);
        }
        return convertToMarkPropertiesDTO(getSharedSailingData().updateMarkProperties(markProperties.getId(),
                markProperties, markProperties.getTags()));
    }

    @Override
    public MarkPropertiesDTO addOrUpdateMarkProperties(MarkPropertiesDTO markProperties) {
        MarkProperties createdOrUpdatedMarkProperties = getSharedSailingData()
                .getMarkPropertiesById(markProperties.getUuid());
        if (createdOrUpdatedMarkProperties != null) {
            getSecurityService().checkCurrentUserUpdatePermission(createdOrUpdatedMarkProperties);
            getSharedSailingData().updateMarkProperties(markProperties.getUuid(),
                    convertDtoToCommonMarkProperties(markProperties.getCommonMarkProperties()),
                    markProperties.getTags());
            createdOrUpdatedMarkProperties = getSharedSailingData()
                    .getMarkPropertiesById(createdOrUpdatedMarkProperties.getId());
        } else {
            createdOrUpdatedMarkProperties = getSecurityService()
                    .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.MARK_PROPERTIES,
                            MarkTemplate.getTypeRelativeObjectIdentifier(UUID.randomUUID()), markProperties.getName(),
                            () -> getSharedSailingData().createMarkProperties(
                                    convertDtoToCommonMarkProperties(markProperties.getCommonMarkProperties()),
                                    markProperties.getTags(),
                                    /* non-default mark properties group ownership */ Optional.empty()));
        }
        return convertToMarkPropertiesDTO(createdOrUpdatedMarkProperties);
    }

    @Override
    public void trackWithTracTrac(RegattaIdentifier regattaToAddTo, List<TracTracRaceRecordDTO> rrs,
            String liveURIFromConfiguration, String storedURIFromConfiguration,
            String updateURI, boolean trackWind, final boolean correctWindByDeclination,
            final Duration offsetToStartTimeOfSimulatedRace, final boolean useInternalMarkPassingAlgorithm,
            boolean useOfficialEventsToUpdateRaceLog, String jsonUrlAsKey)
            throws Exception {
        logger.info("tracWithTracTrac for regatta " + regattaToAddTo + " for race records " + rrs + " with liveURI " + liveURIFromConfiguration
                + " and storedURI " + storedURIFromConfiguration);
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CREATE_OBJECT);
        final TracTracConfiguration config = tractracDomainObjectFactory.getTracTracConfiguration(jsonUrlAsKey);
        for (TracTracRaceRecordDTO rr : rrs) {
            try {
                // reload JSON and load clientparams.php
                final RaceRecord record = getTracTracAdapter().getSingleTracTracRaceRecord(new URL(rr.jsonURL), rr.id, /*loadClientParams*/true);
                logger.info("Loaded race " + record.getName() + " in " + record.getEventName() + " start:" + record.getRaceStartTime() +
                        " trackingStart:" + record.getTrackingStartTime() + " trackingEnd:" + record.getTrackingEndTime());
                // note that the live URI may be null for races that were put into replay mode
                final URI effectiveLiveURI;
                if (!record.getRaceStatus().equals(TracTracConnectionConstants.REPLAY_STATUS)) {
                    if (liveURIFromConfiguration == null || liveURIFromConfiguration.trim().length() == 0) {
                        effectiveLiveURI = record.getLiveURI();
                    } else {
                        effectiveLiveURI = new URI(liveURIFromConfiguration);
                    }
                } else {
                    effectiveLiveURI = null;
                }
                final URI effectiveStoredURI;
                if (storedURIFromConfiguration == null || storedURIFromConfiguration.trim().length() == 0) {
                    effectiveStoredURI = record.getStoredURI();
                } else {
                    effectiveStoredURI = new URI(storedURIFromConfiguration);
                }
                final URI effectiveUpdateURI;
                if (updateURI == null || updateURI.trim().length() == 0) {
                    effectiveUpdateURI = record.getDefaultUpdateURI();
                } else {
                    effectiveUpdateURI = new URI(updateURI);
                }
                getTracTracAdapter().addTracTracRace(getService(), regattaToAddTo,
                        record.getParamURL(), effectiveLiveURI, effectiveStoredURI, effectiveUpdateURI,
                        new MillisecondsTimePoint(record.getTrackingStartTime().asMillis()),
                        new MillisecondsTimePoint(record.getTrackingEndTime().asMillis()), getRaceLogStore(),
                        getRegattaLogStore(), RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        offsetToStartTimeOfSimulatedRace, useInternalMarkPassingAlgorithm, config == null ? null : config.getTracTracUsername(),
                        config == null ? null : config.getTracTracPassword(), record.getRaceStatus(), record.getRaceVisibility(), trackWind,
                        correctWindByDeclination, useOfficialEventsToUpdateRaceLog,
                        liveURIFromConfiguration==null || liveURIFromConfiguration.trim().length() == 0 ? null : new URI(liveURIFromConfiguration),
                        storedURIFromConfiguration==null || storedURIFromConfiguration.trim().length() == 0 ? null : new URI(storedURIFromConfiguration));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error trying to load race " + rrs+". Continuing with remaining races...", e);
            }
        }
    }

    @Override
    public void trackWithYellowBrick(RegattaIdentifier regattaToAddTo, List<YellowBrickRaceRecordDTO> rrs,
            boolean trackWind, final boolean correctWindByDeclination, String creatorUsername,
            String raceUrl) throws Exception {
        logger.info(
                "trackWithYellowBrick for regatta " + regattaToAddTo + " for race records " + rrs );
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CREATE_OBJECT);
        for (YellowBrickRaceRecordDTO rr : rrs) {
            try {
                getYellowBrickTrackingAdapter().addYellowBrickRace(getService(), regattaToAddTo, rr.getRaceUrl(),
                        getRaceLogStore(), getRegattaLogStore(), creatorUsername, raceUrl, trackWind,
                        correctWindByDeclination);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error trying to load race " + rrs+". Continuing with remaining races...", e);
            }
        }
    }

    @Override
    public void createYellowBrickConfiguration(String name, String yellowBrickRaceUrl, String yellowBrickUsername,
            String yellowBrickPassword) {
        if (existsYellowBrickConfigurationForCurrentUser(yellowBrickRaceUrl)) {
            throw new RuntimeException("A configuration for the current user with this race URL already exists.");
        }
        final String currentUserName = getSecurityService().getCurrentUser().getName();
        final TypeRelativeObjectIdentifier identifier = YellowBrickConfiguration.getTypeRelativeObjectIdentifier(yellowBrickRaceUrl, currentUserName);
        final YellowBrickTrackingAdapter yellowBrickTrackingAdapter = getYellowBrickTrackingAdapter();
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.YELLOWBRICK_ACCOUNT,
                identifier, name,
                () -> yellowBrickTrackingAdapter.createYellowBrickConfiguration(name, yellowBrickRaceUrl, yellowBrickUsername, yellowBrickPassword, currentUserName));
    }

    @Override
    public void createTracTracConfiguration(String name, String jsonURL, String liveDataURI, String storedDataURI,
            String courseDesignUpdateURI, String tracTracUsername, String tracTracPassword) throws Exception {
        if (existsTracTracConfigurationForCurrentUser(jsonURL)) {
            throw new RuntimeException("A configuration for the current user with this json URL already exists.");
        }
        final String currentUserName = getSecurityService().getCurrentUser().getName();
        final TypeRelativeObjectIdentifier identifier = TracTracConfiguration.getTypeRelativeObjectIdentifier(jsonURL, currentUserName);
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.TRACTRAC_ACCOUNT,
                identifier, name,
                () -> tractracMongoObjectFactory.createTracTracConfiguration(
                        getTracTracAdapter().createTracTracConfiguration(currentUserName, name, jsonURL, liveDataURI,
                                storedDataURI,
                                courseDesignUpdateURI, tracTracUsername, tracTracPassword)));
    }

    @Override
    public void deleteTracTracConfigurations(Collection<TracTracConfigurationWithSecurityDTO> tracTracConfigurations) {
        for (TracTracConfigurationWithSecurityDTO tracTracConfiguration : tracTracConfigurations) {
            getSecurityService().checkCurrentUserDeletePermission(tracTracConfiguration);
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(tracTracConfiguration,
                    () -> tractracMongoObjectFactory.deleteTracTracConfiguration(tracTracConfiguration.getCreatorName(),
                            tracTracConfiguration.getJsonUrl()));
        }
    }

    @Override
    public void updateTracTracConfiguration(TracTracConfigurationWithSecurityDTO tracTracConfiguration)
            throws Exception {
        getSecurityService().checkCurrentUserUpdatePermission(tracTracConfiguration);
        tractracMongoObjectFactory.updateTracTracConfiguration(
                getTracTracAdapter().createTracTracConfiguration(tracTracConfiguration.getCreatorName(),
                tracTracConfiguration.getName(), tracTracConfiguration.getJsonUrl(),
                tracTracConfiguration.getLiveDataURI(), tracTracConfiguration.getStoredDataURI(),
                tracTracConfiguration.getUpdateURI(), tracTracConfiguration.getTracTracUsername(),
                        tracTracConfiguration.getTracTracPassword()));
    }

    @Override
    public void stopTrackingRaces(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) throws Exception {
        for (RegattaAndRaceIdentifier regattaAndRaceIdentifier : regattaAndRaceIdentifiers) {
            getSecurityService().checkCurrentUserUpdatePermission(regattaAndRaceIdentifier);
            getService().apply(new StopTrackingRace(regattaAndRaceIdentifier));
        }
    }

    @Override
    public void removeAndUntrackRaces(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) {
        for (RegattaAndRaceIdentifier regattaAndRaceIdentifier : regattaAndRaceIdentifiers) {
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(regattaAndRaceIdentifier,
                    () -> getService().apply(new RemoveAndUntrackRace(regattaAndRaceIdentifier)));
        }
    }

    @Override
    public void setWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO) {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserUpdatePermission(trackedRace);
            Position p = null;
            if (windDTO.position != null) {
                p = windDTO.position;
            }
            TimePoint at = null;
            if (windDTO.measureTimepoint != null) {
                at = new MillisecondsTimePoint(windDTO.measureTimepoint);
            }
            SpeedWithBearing speedWithBearing = null;
            Speed speed = null;
            if (windDTO.trueWindSpeedInKnots != null) {
                speed = new KnotSpeedImpl(windDTO.trueWindSpeedInKnots);
            } else if (windDTO.trueWindSpeedInMetersPerSecond != null) {
                speed = new KilometersPerHourSpeedImpl(windDTO.trueWindSpeedInMetersPerSecond * 3600. / 1000.);
            } else if (windDTO.dampenedTrueWindSpeedInKnots != null) {
                speed = new KnotSpeedImpl(windDTO.dampenedTrueWindSpeedInKnots);
            } else if (windDTO.dampenedTrueWindSpeedInMetersPerSecond != null) {
                speed = new KilometersPerHourSpeedImpl(windDTO.dampenedTrueWindSpeedInMetersPerSecond * 3600. / 1000.);
            }
            if (speed != null) {
                if (windDTO.trueWindBearingDeg != null) {
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(),
                            new DegreeBearingImpl(windDTO.trueWindBearingDeg));
                } else if (windDTO.trueWindFromDeg != null) {
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(),
                            new DegreeBearingImpl(windDTO.trueWindFromDeg).reverse());
                }
            }
            Wind wind = new WindImpl(p, at, speedWithBearing);
            Iterable<WindSource> webWindSources = trackedRace.getWindSources(WindSourceType.WEB);
            if (Util.size(webWindSources) == 0) {
                // create a new WEB wind source if not available
                trackedRace.recordWind(wind, new WindSourceImpl(WindSourceType.WEB));
            } else {
                trackedRace.recordWind(wind, webWindSources.iterator().next());
            }
        }
    }

    /**
     * Creates new ControlPoints, if nThe resulting list of control points is then passed to
     * {@link Course#update(List, com.sap.sailing.domain.base.DomainFactory)} for the course of the race identified by
     * <code>raceIdentifier</code>.
     */
    @Override
    public void updateRaceCourse(RegattaAndRaceIdentifier raceIdentifier,
            List<Pair<ControlPointDTO, PassingInstruction>> courseDTO) {
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserUpdatePermission(trackedRace);
            Course course = trackedRace.getRace().getCourse();
            List<Pair<ControlPoint, PassingInstruction>> controlPoints = new ArrayList<>();
            for (Pair<ControlPointDTO, PassingInstruction> waypointDTO : courseDTO) {
                controlPoints.add(new Pair<>(getOrCreateControlPoint(waypointDTO.getA()), waypointDTO.getB()));
            }
            try {
                course.update(controlPoints, course.getAssociatedRoles(), course.getOriginatingCourseTemplateIdOrNull(),
                        baseDomainFactory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void removeSailingServers(Set<String> namesOfSailingServersToRemove) throws Exception {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
        for (String serverName : namesOfSailingServersToRemove) {
            getService().apply(new RemoveRemoteSailingServerReference(serverName));
        }
    }

    @Override
    public RemoteSailingServerReferenceDTO addRemoteSailingServerReference(RemoteSailingServerReferenceDTO sailingServer) throws MalformedURLException {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
        final String expandedURL;
        if (sailingServer.getUrl().contains("//")) {
            expandedURL = sailingServer.getUrl();
        } else {
            expandedURL = "https://" + sailingServer.getUrl();
        }
        URL serverURL = new URL(expandedURL);
        RemoteSailingServerReference serverRef = getService().apply(new AddRemoteSailingServerReference(sailingServer.getName(), serverURL, sailingServer.isInclude()));
        com.sap.sse.common.Util.Pair<Iterable<EventBase>, Exception> eventsOrException = getService()
                .updateRemoteServerEventCacheSynchronously(serverRef, false);
        return createRemoteSailingServerReferenceDTO(serverRef, eventsOrException);

    }

    @Override
    public RemoteSailingServerReferenceDTO updateRemoteSailingServerReference(
            final RemoteSailingServerReferenceDTO sailingServer) throws MalformedURLException {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
        RemoteSailingServerReference serverRef = getService()
                .apply(new UpdateSailingServerReference(sailingServer.getName(),
                        sailingServer.isInclude(), sailingServer.getSelectedEvents().stream().map(element -> {
                            return (UUID) element;
                        }).collect(Collectors.toSet())));
        com.sap.sse.common.Util.Pair<Iterable<EventBase>, Exception> eventsOrException = getService()
                .updateRemoteServerEventCacheSynchronously(serverRef, true);
        return createRemoteSailingServerReferenceDTO(serverRef, eventsOrException);
    }

    @Override
    public RemoteSailingServerReferenceDTO getCompleteRemoteServerReference(final String sailingServerName)
            throws MalformedURLException {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
        RemoteSailingServerReference serverRef = getService().getRemoteServerReferenceByName(sailingServerName);
        com.sap.sse.common.Util.Pair<Iterable<EventBase>, Exception> eventsOrException = getService()
                .getCompleteRemoteServerReference(serverRef);
        return createRemoteSailingServerReferenceDTO(serverRef, eventsOrException);
    }

    @Override
    public void setRaceIsKnownToStartUpwind(RegattaAndRaceIdentifier raceIdentifier, boolean raceIsKnownToStartUpwind) {
        getSecurityService().checkCurrentUserUpdatePermission(raceIdentifier);
        getService().apply(new SetRaceIsKnownToStartUpwind(raceIdentifier, raceIsKnownToStartUpwind));
    }

    @Override
    public void setWindSourcesToExclude(RegattaAndRaceIdentifier raceIdentifier,
            List<WindSource> windSourcesToExclude) {
        getSecurityService().checkCurrentUserUpdatePermission(raceIdentifier);
        final DynamicTrackedRace trackedRace = (DynamicTrackedRace) getExistingTrackedRace(raceIdentifier);
        if (trackedRace == null) {
            logger.info("Couldn't set wind sources to exclude for tracked race "+raceIdentifier+" because the race was not found");
        } else {
            final Iterable<RaceLog> raceLogs = trackedRace.getAttachedRaceLogs();
            if (raceLogs != null && !Util.isEmpty(raceLogs)) {
                final RaceLog defaultRaceLog = raceLogs.iterator().next();
                logger.info("Set wind sources to exclude for tracked race "+raceIdentifier+" to "+windSourcesToExclude);
                defaultRaceLog.add(new RaceLogExcludeWindSourcesEventImpl(TimePoint.now(),
                        getService().getServerAuthor(), defaultRaceLog.getCurrentPassId(), windSourcesToExclude));
            } else {
                logger.info("Couldn't set wind sources to exclude for tracked race "+raceIdentifier+" because no race log seems attached");
            }
        }
    }

    @Override
    public void removeWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO) {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserUpdatePermission(trackedRace);
            Position p = null;
            if (windDTO.position != null) {
                p = windDTO.position;
            }
            TimePoint at = null;
            if (windDTO.measureTimepoint != null) {
                at = new MillisecondsTimePoint(windDTO.measureTimepoint);
            }
            SpeedWithBearing speedWithBearing = null;
            Speed speed = null;
            if (windDTO.trueWindSpeedInKnots != null) {
                speed = new KnotSpeedImpl(windDTO.trueWindSpeedInKnots);
            } else if (windDTO.trueWindSpeedInMetersPerSecond != null) {
                speed = new KilometersPerHourSpeedImpl(windDTO.trueWindSpeedInMetersPerSecond * 3600. / 1000.);
            } else if (windDTO.dampenedTrueWindSpeedInKnots != null) {
                speed = new KnotSpeedImpl(windDTO.dampenedTrueWindSpeedInKnots);
            } else if (windDTO.dampenedTrueWindSpeedInMetersPerSecond != null) {
                speed = new KilometersPerHourSpeedImpl(windDTO.dampenedTrueWindSpeedInMetersPerSecond * 3600. / 1000.);
            }
            if (speed != null) {
                if (windDTO.trueWindBearingDeg != null) {
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(),
                            new DegreeBearingImpl(windDTO.trueWindBearingDeg));
                } else if (windDTO.trueWindFromDeg != null) {
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(),
                            new DegreeBearingImpl(windDTO.trueWindFromDeg).reverse());
                }
            }
            Wind wind = new WindImpl(p, at, speedWithBearing);
            trackedRace.removeWind(wind, trackedRace.getWindSources(WindSourceType.WEB).iterator().next());
        }
    }

    @Override
    public void createRegattaStructure(final List<RegattaDTO> regattas, final EventDTO newEvent) throws MalformedURLException {
        final List<String> leaderboardNames = new ArrayList<String>();
        for (RegattaDTO regatta : regattas) {
            createRegattaFromRegattaDTO(regatta);
            addRaceColumnsToRegattaSeries(regatta);
            if (getLeaderboard(regatta.getName()) == null) {
                leaderboardNames.add(regatta.getName());
                createRegattaLeaderboard(new RegattaName(regatta.getName()), regatta.boatClass.toString(), new int[0]);
            }
        }
        createAndAddLeaderboardGroup(newEvent, leaderboardNames);
        // TODO find a way to import the competitors for the selected regattas. You'll need the regattas as
        // Iterable<RegattaResults>
        // structureImporter.setCompetitors(regattas, "");
    }

    @Override
    public void createSwissTimingArchiveConfiguration(final String jsonURL)
            throws Exception {
        if (existsSwissTimingArchiveConfigurationForCurrentUser(jsonURL)) {
            throw new RuntimeException("A configuration for the current user with this json URL already exists.");
        }
        final String currentUserName = getSecurityService().getCurrentUser().getName();
        final TypeRelativeObjectIdentifier identifier = SwissTimingArchiveConfiguration
                .getTypeRelativeObjectIdentifier(jsonURL, currentUserName);
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.SWISS_TIMING_ARCHIVE_ACCOUNT,
                identifier, identifier.toString(),
                () -> swissTimingAdapterPersistence.createSwissTimingArchiveConfiguration(
                        swissTimingFactory.createSwissTimingArchiveConfiguration(jsonURL,
                                currentUserName)));
    }

    @Override
    public void updateSwissTimingArchiveConfiguration(SwissTimingArchiveConfigurationWithSecurityDTO dto)
            throws Exception {
        getSecurityService().checkCurrentUserUpdatePermission(dto);
        swissTimingAdapterPersistence.updateSwissTimingArchiveConfiguration(
                swissTimingFactory.createSwissTimingArchiveConfiguration(dto.getJsonUrl(), dto.getCreatorName()));
    }

    @Override
    public void deleteSwissTimingArchiveConfigurations(Collection<SwissTimingArchiveConfigurationWithSecurityDTO> dtos)
            throws Exception {
        for (SwissTimingArchiveConfigurationWithSecurityDTO dto : dtos) {
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(dto.getIdentifier(),
                    () -> swissTimingAdapterPersistence.deleteSwissTimingArchiveConfiguration(swissTimingFactory
                            .createSwissTimingArchiveConfiguration(dto.getJsonUrl(), dto.getCreatorName())));
        }
    }

    @Override
    public void removeResultImportURLs(String resultProviderName, Set<UrlDTO> toRemove) throws Exception {
        final RacingEventService racingEventService = getService();
        Set<URL> urlsToRemove = toRemove.stream().map(urlDto -> {
            try {
                return new URL(urlDto.getUrl());
            } catch (MalformedURLException e) {
                return null;
            }
        }).filter(url -> url != null).collect(Collectors.toSet());
        racingEventService.removeResultImportURLs(resultProviderName, urlsToRemove);
    }

    @Override
    public void addResultImportUrl(String resultProviderName, UrlDTO urlDTO) throws Exception {
        final RacingEventService racingEventService = getService();
        ResultUrlProvider resultUrlProvider = racingEventService.getUrlBasedScoreCorrectionProvider(resultProviderName)
                .orElseThrow(() -> new IllegalStateException("ResultUrlProvider not found: " + resultProviderName));
        final URL url = resultUrlProvider.resolveUrl(urlDTO.getUrl());
        racingEventService.addResultImportUrl(resultProviderName, url);
    }

    @Override
    public StrippedLeaderboardDTO createFlexibleLeaderboard(String leaderboardName,
            String leaderboardDisplayName, int[] discardThresholds, ScoringSchemeType scoringSchemeType,
            List<UUID> courseAreaIds) {
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.LEADERBOARD, Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName),
                leaderboardDisplayName, new Callable<StrippedLeaderboardDTO>() {
                    @Override
                    public StrippedLeaderboardDTO call() throws Exception {
                        return createStrippedLeaderboardDTO(
                                getService().apply(new CreateFlexibleLeaderboard(leaderboardName,
                                        leaderboardDisplayName, discardThresholds,
                                baseDomainFactory.createScoringScheme(scoringSchemeType), courseAreaIds)), false, false);
                    }
                });
    }

    @Override
    public StrippedLeaderboardDTO createRegattaLeaderboard(RegattaName regattaIdentifier,
            String leaderboardDisplayName, int[] discardThresholds) {
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.LEADERBOARD, Leaderboard.getTypeRelativeObjectIdentifier(regattaIdentifier),
                leaderboardDisplayName, new Callable<StrippedLeaderboardDTO>() {
                    @Override
                    public StrippedLeaderboardDTO call() throws Exception {
                        return createStrippedLeaderboardDTO(getService().apply(new CreateRegattaLeaderboard(
                                regattaIdentifier, leaderboardDisplayName, discardThresholds)), false, false);
                    }
                });
    }

    @Override
    public StrippedLeaderboardDTO createRegattaLeaderboardWithEliminations(String name, String displayName,
            String fullRegattaLeaderboardName) {
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.LEADERBOARD, Leaderboard.getTypeRelativeObjectIdentifier(name), displayName,
                new Callable<StrippedLeaderboardDTO>() {
                    @Override
                    public StrippedLeaderboardDTO call() throws Exception {
                        return createStrippedLeaderboardDTO(
                                getService().apply(new CreateRegattaLeaderboardWithEliminations(name, displayName,
                                        fullRegattaLeaderboardName)),
                                false, false);
                    }
                });
    }

    @Override
    public StrippedLeaderboardDTO createRegattaLeaderboardWithOtherTieBreakingLeaderboard(RegattaName regattaIdentifier,
            String leaderboardDisplayName, int[] discardThresholds, String otherTieBreakingLeaderboardName) {
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.LEADERBOARD, Leaderboard.getTypeRelativeObjectIdentifier(regattaIdentifier), leaderboardDisplayName,
                new Callable<StrippedLeaderboardDTO>() {
                    @Override
                    public StrippedLeaderboardDTO call() throws Exception {
                        return createStrippedLeaderboardDTO(
                                getService().apply(new CreateRegattaLeaderboardWithOtherTieBreakingLeaderboard(regattaIdentifier, leaderboardDisplayName, discardThresholds,
                                        otherTieBreakingLeaderboardName)),
                                false, false);
                    }
                });
    }

    @Override
    public StrippedLeaderboardDTO updateLeaderboard(String leaderboardName, String newLeaderboardDisplayName,
            int[] newDiscardingThresholds, List<UUID> newCourseAreaIds) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        Leaderboard updatedLeaderboard = getService().apply(
                new UpdateLeaderboard(leaderboardName, newLeaderboardDisplayName, newDiscardingThresholds, newCourseAreaIds));
        return createStrippedLeaderboardDTO(updatedLeaderboard, false, false);
    }

    @Override
    public void removeLeaderboards(Collection<String> leaderboardNames) {
        for (String leaderboardName : leaderboardNames) {
            removeLeaderboard(leaderboardName);
        }
    }

    @Override
    public void removeLeaderboard(String leaderboardName) {
        Leaderboard leaderBoard = getService().getLeaderboardByName(leaderboardName);
        if (leaderBoard != null) {
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(leaderBoard, new Action() {
                @Override
                public void run() throws Exception {
                    getService().apply(new RemoveLeaderboard(leaderboardName));
                }
            });
        }
    }

    @Override
    public void addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new AddColumnToLeaderboard(columnName, leaderboardName, medalRace));
    }

    @Override
    public void removeLeaderboardGroups(Set<UUID> groupIds) {
        for (final UUID groupId : groupIds) {
            removeLeaderboardGroup(groupId);
        }
    }

    private void removeLeaderboardGroup(UUID groupId) {
        LeaderboardGroup group = getService().getLeaderboardGroupByID(groupId);
        if (group != null) {
            if (group.getOverallLeaderboard() != null) {
                removeLeaderboard(group.getOverallLeaderboard().getName());
            }
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(group, new Action() {
                @Override
                public void run() throws Exception {
                    getService().apply(new RemoveLeaderboardGroup(groupId));
                }
            });
        }
    }

    @Override
    public void addColumnsToLeaderboard(String leaderboardName,
            List<com.sap.sse.common.Util.Pair<String, Boolean>> columnsToAdd) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        for (com.sap.sse.common.Util.Pair<String, Boolean> columnToAdd : columnsToAdd) {
            getService().apply(new AddColumnToLeaderboard(columnToAdd.getA(), leaderboardName, columnToAdd.getB()));
        }
    }

    @Override
    public void removeLeaderboardColumn(String leaderboardName, String columnName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new RemoveLeaderboardColumn(columnName, leaderboardName));
    }

    @Override
    public void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new RenameLeaderboardColumn(leaderboardName, oldColumnName, newColumnName));
    }

    @Override
    public void updateLeaderboardColumnFactor(String leaderboardName, String columnName, Double newFactor) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new UpdateLeaderboardColumnFactor(leaderboardName, columnName, newFactor));
    }

    @Override
    public void suppressCompetitorInLeaderboard(String leaderboardName, String competitorIdAsString,
            boolean suppressed) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(
                new SetSuppressedFlagForCompetitorInLeaderboard(leaderboardName, competitorIdAsString, suppressed));
    }

    @Override
    public boolean connectTrackedRaceToLeaderboardColumn(String leaderboardName, String raceColumnName,
            String fleetName, RegattaAndRaceIdentifier raceIdentifier) {
        final Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(
                DefaultActions.UPDATE, Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        Object principal = subject.getPrincipal();
        if (principal != null) {
            logger.info(String.format("%s linked race column %s %s (%s) with tracked race %s.", principal.toString(),
                    leaderboardName, raceColumnName, fleetName, raceIdentifier.getRaceName()));
        } else {
            logger.info(String.format("Linked race column %s %s (%s) with tracked race %s.", leaderboardName,
                    raceColumnName, fleetName, raceIdentifier.getRaceName()));
        }
        return getService().apply(
                new ConnectTrackedRaceToLeaderboardColumn(leaderboardName, raceColumnName, fleetName, raceIdentifier));
    }

    @Override
    public void disconnectLeaderboardColumnFromTrackedRace(String leaderboardName, String raceColumnName,
            String fleetName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new DisconnectLeaderboardColumnFromTrackedRace(leaderboardName, raceColumnName, fleetName));
    }

    @Override
    public void updateLeaderboardCarryValue(String leaderboardName, String competitorIdAsString, Double carriedPoints) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new UpdateLeaderboardCarryValue(leaderboardName, competitorIdAsString, carriedPoints));
    }

    @Override
    public Triple<Double, Double, Boolean> updateLeaderboardMaxPointsReason(
            String leaderboardName, String competitorIdAsString, String raceColumnName, MaxPointsReason maxPointsReason,
            Date date) throws NoWindException {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        return getService().apply(new UpdateLeaderboardMaxPointsReason(leaderboardName, raceColumnName,
                competitorIdAsString, maxPointsReason, new MillisecondsTimePoint(date)));
    }

    @Override
    public Triple<Double, Double, Boolean> updateLeaderboardScoreCorrection(
            String leaderboardName, String competitorIdAsString, String columnName, Double correctedScore, Date date) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        return getService().apply(new UpdateLeaderboardScoreCorrection(leaderboardName, columnName,
                competitorIdAsString, correctedScore, new MillisecondsTimePoint(date)));
    }

    @Override
    public Triple<Double, Double, Boolean> updateLeaderboardIncrementalScoreCorrection(
            String leaderboardName, String competitorIdAsString, String columnName, Double scoringOffsetInPoints, Date date) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        return getService().apply(new UpdateLeaderboardIncrementalScoreCorrection(leaderboardName, columnName,
                competitorIdAsString, scoringOffsetInPoints, new MillisecondsTimePoint(date)));
    }

    @Override
    public void updateLeaderboardScoreCorrectionMetadata(String leaderboardName, Date timePointOfLastCorrectionValidity, String comment) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new UpdateLeaderboardScoreCorrectionMetadata(leaderboardName,
                timePointOfLastCorrectionValidity == null ? null
                        : new MillisecondsTimePoint(timePointOfLastCorrectionValidity),
                comment));
    }

    @Override
    public void updateLeaderboardScoreCorrectionsAndMaxPointsReasons(BulkScoreCorrectionDTO updates)
            throws NoWindException {
        SecurityUtils.getSubject()
                .checkPermission(SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(
                        DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(updates.getLeaderboardName())));
        Date dateForResults = new Date(); // we don't care about the result date/time here; use current date as default
        for (Map.Entry<String, Map<String, Double>> e : updates.getScoreUpdatesForRaceColumnByCompetitorIdAsString()
                .entrySet()) {
            for (Map.Entry<String, Double> raceColumnNameAndCorrectedScore : e.getValue().entrySet()) {
                updateLeaderboardScoreCorrection(updates.getLeaderboardName(), e.getKey(),
                        raceColumnNameAndCorrectedScore.getKey(), raceColumnNameAndCorrectedScore.getValue(),
                        dateForResults);
            }
        }
        for (Map.Entry<String, Map<String, MaxPointsReason>> e : updates
                .getMaxPointsUpdatesForRaceColumnByCompetitorIdAsString().entrySet()) {
            for (Map.Entry<String, MaxPointsReason> raceColumnNameAndNewMaxPointsReason : e.getValue().entrySet()) {
                updateLeaderboardMaxPointsReason(updates.getLeaderboardName(), e.getKey(),
                        raceColumnNameAndNewMaxPointsReason.getKey(), raceColumnNameAndNewMaxPointsReason.getValue(),
                        dateForResults);
            }
        }
    }

    @Override
    public void updateCompetitorDisplayNameInLeaderboard(String leaderboardName, String competitorIdAsString,
            String displayName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(
                new UpdateCompetitorDisplayNameInLeaderboard(leaderboardName, competitorIdAsString, displayName));
    }

    @Override
    public void moveLeaderboardColumnUp(String leaderboardName, String columnName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new MoveLeaderboardColumnUp(leaderboardName, columnName));
    }

    @Override
    public void moveLeaderboardColumnDown(String leaderboardName, String columnName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new MoveLeaderboardColumnDown(leaderboardName, columnName));
    }

    @Override
    public void updateIsMedalRace(String leaderboardName, String columnName, boolean isMedalRace) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)));
        getService().apply(new UpdateIsMedalRace(leaderboardName, columnName, isMedalRace));
    }

    @Override
    public void updateRacesDelayToLive(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers, long delayToLiveInMs) {
        for (RegattaAndRaceIdentifier regattaAndRaceIdentifier : regattaAndRaceIdentifiers) {
            getSecurityService().checkCurrentUserUpdatePermission(regattaAndRaceIdentifier);
            getService().apply(new UpdateRaceDelayToLive(regattaAndRaceIdentifier, delayToLiveInMs));
        }
    }

    @Override
    public void createSwissTimingConfiguration(String configName, String jsonURL, String hostname, Integer port,
            String updateURL, String updateUsername, String updatePassword) throws Exception {
        if (!jsonURL.equalsIgnoreCase("test")) {
            if (existsSwissTimingConfigurationForCurrentUser(jsonURL)) {
                throw new RuntimeException("A Configuration for the current user with this json URL already exists.");
            }
            final String currentUserName = getSecurityService().getCurrentUser().getName();
            final TypeRelativeObjectIdentifier identifier = SwissTimingConfiguration.getTypeRelativeObjectIdentifier(jsonURL, currentUserName);
            getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredDomainType.SWISS_TIMING_ACCOUNT,
                    identifier, configName,
                    () -> swissTimingAdapterPersistence
                            .createSwissTimingConfiguration(
                                    swissTimingFactory.createSwissTimingConfiguration(configName,
                                            jsonURL, hostname, port, updateURL, updateUsername, updatePassword,
                                            currentUserName)));
        }
    }

    @Override
    public void deleteSwissTimingConfigurations(
            final Collection<SwissTimingConfigurationWithSecurityDTO> configurations) {
        for (SwissTimingConfigurationWithSecurityDTO dto : configurations) {
            getSecurityService().checkCurrentUserDeletePermission(dto);
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(dto,
                    () -> swissTimingAdapterPersistence.deleteSwissTimingConfiguration(dto.getCreatorName(),
                            dto.getJsonUrl()));
        }
    }

    @Override
    public void updateSwissTimingConfiguration(SwissTimingConfigurationWithSecurityDTO configuration)
            throws Exception {
        getSecurityService().checkCurrentUserUpdatePermission(configuration);
        swissTimingAdapterPersistence.updateSwissTimingConfiguration(swissTimingFactory.createSwissTimingConfiguration(
                configuration.getName(), configuration.getJsonUrl(), configuration.getHostname(),
                configuration.getPort(), configuration.getUpdateURL(), configuration.getUpdateUsername(),
                configuration.getUpdatePassword(), configuration.getCreatorName()));
    }

    @Override
    public void trackWithSwissTiming(RegattaIdentifier regattaToAddTo, List<SwissTimingRaceRecordDTO> rrs,
            String hostname, int port, boolean trackWind, final boolean correctWindByDeclination,
            boolean useInternalMarkPassingAlgorithm, String updateURL, String updateUsername, String updatePassword,
            String eventName, String manage2SailEventUrl) throws InterruptedException, ParseException, Exception {
        logger.info(
                "tracWithSwissTiming for regatta " + regattaToAddTo + " for race records " + rrs
                + " with hostname " + hostname + " and port " + port);
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CREATE_OBJECT);
        Map<String, RegattaResults> cachedRegattaEntriesLists = new HashMap<String, RegattaResults>();
        for (SwissTimingRaceRecordDTO rr : rrs) {
            BoatClass boatClass = getBaseDomainFactory().getOrCreateBoatClass(rr.boatClass);
            String raceDescription = rr.regattaName != null ? rr.regattaName : "";
            raceDescription += rr.seriesName != null ? "/" + rr.seriesName : "";
            raceDescription += raceDescription.length() > 0 ? "/" + rr.getName() : rr.getName();
            // try to find a cached entry list for the regatta
            RegattaResults regattaResults = cachedRegattaEntriesLists.get(rr.xrrEntriesUrl);
            if (regattaResults == null && rr.xrrEntriesUrl != null) {
                regattaResults = getSwissTimingAdapter().readRegattaEntryListFromXrrUrl(rr.xrrEntriesUrl);
                if (regattaResults != null) {
                    cachedRegattaEntriesLists.put(rr.xrrEntriesUrl, regattaResults);
                }
            }
            StartList startList = null;
            if (regattaResults != null) {
                startList = getSwissTimingAdapter().readStartListForRace(rr.raceId, regattaResults);
            }
            // now read the entry list for the race from the result
            getSwissTimingAdapter().addSwissTimingRace(getService(), regattaToAddTo,
                    rr.raceId, rr.getName(), raceDescription, boatClass, hostname, port, startList,
                    getRaceLogStore(), getRegattaLogStore(),
                    RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS, useInternalMarkPassingAlgorithm, trackWind,
                    correctWindByDeclination, updateURL, updateUsername, updatePassword, eventName, manage2SailEventUrl);
        }
    }

    @Override
    public LeaderboardGroupDTO createLeaderboardGroup(String groupName, String description, String displayName,
            boolean displayGroupsInReverseOrder, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType) {
        final List<String> leaderboards = new ArrayList<>();
        return doCreateLeaderboardGroup(groupName, description, displayName, displayGroupsInReverseOrder,
                overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType, leaderboards);
    }

    private LeaderboardGroupDTO doCreateLeaderboardGroup(String groupName, String description, String displayName,
            boolean displayGroupsInReverseOrder, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType, List<String> leaderBoards) {
        final UUID newLeaderboardGroupId = UUID.randomUUID();
        final Callable<LeaderboardGroupDTO> createLeaderboardGroup = ()->getSecurityService()
            .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD_GROUP,
                LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(newLeaderboardGroupId), displayName,
                ()->{
                    AbstractLeaderboardGroupOperation<LeaderboardGroup> createLeaderboardGroupOp = new CreateLeaderboardGroup(
                            newLeaderboardGroupId, groupName, description, displayName, displayGroupsInReverseOrder,
                            leaderBoards, overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType);
                    return convertToLeaderboardGroupDTO(getService().apply(createLeaderboardGroupOp), false, false);
                });
        final LeaderboardGroupDTO result;
        // if an overall leaderboard is requested, establish ownership and check create permission; roll back if not possible;
        // otherwise, try to establish the leaderboard group's ownership; if that fails with an AuthorizationException,
        // the overall leaderboard's ownership is removed again, too.
        if (overallLeaderboardScoringSchemeType != null) {
            final String overallLeaderboardName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(groupName);
            result = getSecurityService()
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD,
                    new TypeRelativeObjectIdentifier(overallLeaderboardName), overallLeaderboardName, createLeaderboardGroup);
        } else {
            try {
                result = createLeaderboardGroup.call();
            } catch (Exception e) {
                throw new RuntimeException(e); // the callable isn't throwing any checked exception
            }
        }
        return result;
    }

    @Override
    public void updateLeaderboardGroup(UUID leaderboardGroupId, String oldName, String newName, String newDescription,
            String newDisplayName, List<String> leaderboardNames, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType) {
        final LeaderboardGroup leaderboardGroup = getService().getLeaderboardGroupByID(leaderboardGroupId);
        if (leaderboardGroup == null) {
            throw new IllegalArgumentException("Leadearboard group with ID "+leaderboardGroupId+" not found.");
        }
        final Action updateLeaderboardGroup = ()->{
            SecurityUtils.getSubject()
                .checkPermission(SecuredDomainType.LEADERBOARD_GROUP.getStringPermissionForTypeRelativeIdentifier(
                    DefaultActions.UPDATE,
                    LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(leaderboardGroupId)));
            getService().apply(new UpdateLeaderboardGroup(leaderboardGroupId, newName, newDescription, newDisplayName,
                    leaderboardNames, overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType));
        };
        if (leaderboardGroup.getOverallLeaderboard() == null && overallLeaderboardScoringSchemeType != null) {
            // an overall leaderboard will be created; check permissions:
            final String overallLeaderboardName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(newName);
            getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD,
                    new TypeRelativeObjectIdentifier(overallLeaderboardName), overallLeaderboardName, updateLeaderboardGroup);
        } else if (leaderboardGroup.getOverallLeaderboard() != null && overallLeaderboardScoringSchemeType == null) {
            // an overall leaderboard existed but will now be removed; check permissions
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(leaderboardGroup.getOverallLeaderboard(), updateLeaderboardGroup);
        } else {
            if (leaderboardGroup.getOverallLeaderboard() != null) {
                SecurityUtils.getSubject()
                    .checkPermission(SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(
                        DefaultActions.UPDATE,
                        new TypeRelativeObjectIdentifier(leaderboardGroup.getOverallLeaderboard().getName())));
            }
            try {
                updateLeaderboardGroup.run();
            } catch (Exception e) {
                throw new RuntimeException(e); // the createLeaderboardGroup block above doesn't throw any checked exceptions
            }
        }
    }

    protected void createAndAddLeaderboardGroup(final EventDTO newEvent, List<String> leaderboardNames)
            throws MalformedURLException {
        LeaderboardGroupDTO leaderboardGroupDTO = null;
        String description = "";
        if (newEvent.getDescription() != null) {
            description = newEvent.getDescription();
        }
        String eventName = newEvent.getName();
        List<UUID> eventLeaderboardGroupUUIDs = new ArrayList<>();
        // create Leaderboard Group
        LeaderboardGroup leaderboardGroup = getService().getLeaderboardGroupByName(eventName);
        if (leaderboardGroup == null) {
            leaderboardGroupDTO = doCreateLeaderboardGroup(eventName, description, eventName, false, null, null,
                    leaderboardNames);
            eventLeaderboardGroupUUIDs.add(leaderboardGroupDTO.getId());
        } else {
            updateLeaderboardGroup(leaderboardGroup.getId(), eventName, eventName, newEvent.getDescription(), eventName,
                    leaderboardNames, null, null);
            leaderboardGroupDTO = getLeaderboardGroupByName(eventName, false);
        }
        for (LeaderboardGroupDTO lg : newEvent.getLeaderboardGroups()) {
            eventLeaderboardGroupUUIDs.add(lg.getId());
        }
        updateEvent(newEvent.id, newEvent.getName(), description, newEvent.startDate, newEvent.endDate, newEvent.getVenue(),
                newEvent.isPublic, eventLeaderboardGroupUUIDs, newEvent.getOfficialWebsiteURL(), newEvent.getBaseURL(),
                newEvent.getSailorsInfoWebsiteURLs(), newEvent.getImages(), newEvent.getVideos(),
                newEvent.getWindFinderReviewedSpotsCollectionIds());
    }

    @Override
    public EventDTO updateEvent(EventDTO eventDTO) throws MalformedURLException, UnauthorizedException {
        UUID eventId = eventDTO.id;
        String eventName = eventDTO.getName();
        String eventDescription = eventDTO.getDescription();
        Date startDate = eventDTO.startDate;
        Date endDate = eventDTO.endDate;
        VenueDTO venue = eventDTO.getVenue();
        boolean isPublic = eventDTO.isPublic;
        List<UUID> leaderboardGroupIds = eventDTO.getLeaderboardGroupIds();
        String officialWebsiteURLString = eventDTO.getOfficialWebsiteURL();
        String baseURLAsString = eventDTO.getBaseURL();
        Map<String, String> sailorsInfoWebsiteURLsByLocaleName = eventDTO.getSailorsInfoWebsiteURLs();
        List<SailingImageDTO> images = eventDTO.getImages();
        List<SailingVideoDTO> videos = eventDTO.getVideos();
        List<String> windFinderReviewedSpotCollectionIds = eventDTO.getWindFinderReviewedSpotsCollectionIds();
        return updateEvent(eventId, eventName, eventDescription, startDate, endDate, venue, isPublic,
                leaderboardGroupIds, officialWebsiteURLString, baseURLAsString, sailorsInfoWebsiteURLsByLocaleName,
                images, videos, windFinderReviewedSpotCollectionIds);
    }

    @Override
    public EventDTO updateEvent(UUID eventId, String eventName, String eventDescription, Date startDate, Date endDate,
            VenueDTO venue, boolean isPublic, List<UUID> leaderboardGroupIds, String officialWebsiteURLString,
            String baseURLAsString, Map<String, String> sailorsInfoWebsiteURLsByLocaleName, List<? extends ImageDTO> images,
            List<? extends VideoDTO> videos, List<String> windFinderReviewedSpotCollectionIds)
            throws MalformedURLException, UnauthorizedException {
        final TimePoint startTimePoint = startDate != null ? new MillisecondsTimePoint(startDate) : null;
        final TimePoint endTimePoint = endDate != null ? new MillisecondsTimePoint(endDate) : null;
        final URL officialWebsiteURL = officialWebsiteURLString != null ? new URL(officialWebsiteURLString) : null;
        final URL baseURL = baseURLAsString != null ? new URL(baseURLAsString) : null;
        final Map<Locale, URL> sailorsInfoWebsiteURLs = convertToLocalesAndUrls(sailorsInfoWebsiteURLsByLocaleName);
        final List<ImageDescriptor> eventImages = convertToImages(images);
        final List<VideoDescriptor> eventVideos = convertToVideos(videos);
        final TypeRelativeObjectIdentifier typeRelativeObjectIdentifier = EventBaseImpl.getTypeRelativeObjectIdentifier(eventId);
        if (SecurityUtils.getSubject().isPermitted(SecuredDomainType.EVENT.getStringPermissionForTypeRelativeIdentifier(
                DefaultActions.UPDATE, typeRelativeObjectIdentifier))) {
            // it's fine; the subject has full UPDATE permission for the event
        } else if (SecurityUtils.getSubject().isPermitted(SecuredDomainType.EVENT.getStringPermissionForTypeRelativeIdentifier(
                EventActions.UPLOAD_MEDIA, typeRelativeObjectIdentifier))) {
            final EventDTO currentEventState = getEventById(eventId, false);
            if (!Util.equalsWithNull(startTimePoint, TimePoint.of(currentEventState.startDate))
             || !Util.equalsWithNull(endTimePoint, TimePoint.of(currentEventState.endDate))
             || !Util.equalsWithNull(officialWebsiteURLString, currentEventState.getOfficialWebsiteURL())
             || !Util.equalsWithNull(baseURLAsString, currentEventState.getBaseURL())
             || !Util.equalsWithNull(sailorsInfoWebsiteURLsByLocaleName, currentEventState.getSailorsInfoWebsiteURLs())
             || !Util.equalsWithNull(venue.getName(), currentEventState.getVenue().getName())
             || !Util.equalsWithNull(eventName, currentEventState.getName())
             || !Util.equalsWithNull(windFinderReviewedSpotCollectionIds, currentEventState.getWindFinderReviewedSpotsCollectionIds())
             || !Util.equalsWithNull(leaderboardGroupIds, currentEventState.getLeaderboardGroupIds())
             || !Util.equalsWithNull(isPublic, currentEventState.isPublic)
             || !Util.isOnlyAdding(images, currentEventState.getImages(), (a, b)->a.compareTo(b) == 0)
             || !Util.isOnlyAdding(videos, currentEventState.getVideos(), (a, b)->a.compareTo(b) == 0)) {
                throw new UnauthorizedException("You are not permitted to edit event " + eventId + " other than by adding images and videos");
            } else {
                final Set<String> sourceRefsOfImagesAdded = new HashSet<>();
                Util.addAll(Util.map(images, ImageDTO::getSourceRef), sourceRefsOfImagesAdded);
                Util.removeAll(Util.map(currentEventState.getImages(), ImageDTO::getSourceRef), sourceRefsOfImagesAdded);
                final Set<String> sourceRefsOfVideosAdded = new HashSet<>();
                Util.addAll(Util.map(videos, VideoDTO::getSourceRef), sourceRefsOfVideosAdded);
                Util.removeAll(Util.map(currentEventState.getVideos(), VideoDTO::getSourceRef), sourceRefsOfVideosAdded);
                logger.info("User "+SecurityUtils.getSubject().getPrincipal()+" is adding the following media to event "+currentEventState.getName()+
                        " with ID "+currentEventState.getId()+": images: "+sourceRefsOfImagesAdded+", videos: "+sourceRefsOfVideosAdded);
            }
        } else {
            throw new UnauthorizedException("You are not permitted to edit event " + eventId);
        }
        getService().apply(new UpdateEvent(eventId, eventName, eventDescription, startTimePoint, endTimePoint,
                venue.getName(), isPublic, leaderboardGroupIds, officialWebsiteURL, baseURL, sailorsInfoWebsiteURLs,
                eventImages, eventVideos, windFinderReviewedSpotCollectionIds));
        return getEventById(eventId, false);
    }

    @Override
    public EventDTO createEvent(String eventName, String eventDescription, Date startDate, Date endDate, String venue,
            boolean isPublic, List<CourseAreaDTO> courseAreas, String officialWebsiteURLAsString, String baseURLAsString,
            Map<String, String> sailorsInfoWebsiteURLsByLocaleName, List<ImageDTO> images,
            List<VideoDTO> videos, List<UUID> leaderboardGroupIds)
            throws UnauthorizedException {
        final UUID eventUuid = UUID.randomUUID();
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.EVENT, EventBaseImpl.getTypeRelativeObjectIdentifier(eventUuid), eventName,
                new Callable<EventDTO>() {
                    @Override
                    public EventDTO call() throws Exception {
                        logger.info("User "+SecurityUtils.getSubject().getPrincipal()+" is trying to create event "+eventName);
                        final TimePoint startTimePoint = startDate != null ? new MillisecondsTimePoint(startDate) : null;
                        final TimePoint endTimePoint = endDate != null ? new MillisecondsTimePoint(endDate) : null;
                                final URL officialWebsiteURL = officialWebsiteURLAsString != null
                                        ? new URL(officialWebsiteURLAsString)
                                        : null;
                                final URL baseURL = baseURLAsString != null ? new URL(baseURLAsString) : null;
                        final Map<Locale, URL> sailorsInfoWebsiteURLs = convertToLocalesAndUrls(
                                sailorsInfoWebsiteURLsByLocaleName);
                        final List<ImageDescriptor> eventImages = convertToImages(images);
                        final List<VideoDescriptor> eventVideos = convertToVideos(videos);
                        getService().apply(new CreateEvent(eventName, eventDescription, startTimePoint, endTimePoint,
                                venue, isPublic, eventUuid, officialWebsiteURL, baseURL, sailorsInfoWebsiteURLs,
                                eventImages, eventVideos, leaderboardGroupIds));
                        createCourseAreas(eventUuid, courseAreas);
                        return getEventById(eventUuid, false);
                    }
                });
    }

    @Override
    public void createCourseAreas(UUID eventId, List<CourseAreaDTO> courseAreas) {
        getSecurityService().checkCurrentUserUpdatePermission(getService().getEvent(eventId));
        getService().apply(new AddCourseAreas(eventId,
                Util.toArray(Util.map(courseAreas, CourseAreaDTO::getName), new String[courseAreas.size()]),
                Util.toArray(Util.map(courseAreas, CourseAreaDTO::getId), new UUID[courseAreas.size()]),
                Util.toArray(Util.map(courseAreas, CourseAreaDTO::getCenterPosition), new Position[courseAreas.size()]),
                Util.toArray(Util.map(courseAreas, CourseAreaDTO::getRadius), new Distance[courseAreas.size()])));
    }

    @Override
    public void removeCourseAreas(UUID eventId, UUID[] courseAreaIds) {
        getSecurityService().checkCurrentUserDeletePermission(getService().getEvent(eventId));
        getService().apply(new RemoveCourseAreas(eventId, courseAreaIds));
    }

    @Override
    public void removeEvents(Collection<UUID> eventIds) throws UnauthorizedException {
        for (UUID eventId : eventIds) {
            removeEvent(eventId);
        }
    }

    @Override
    public void removeEvent(UUID eventId) throws UnauthorizedException {
        Event event = getService().getEvent(eventId);
        if (event != null) {
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(event, new Action() {
                @Override
                public void run() throws Exception {
                    getService().apply(new RemoveEvent(eventId));
                }
            });
        }
    }

    @Override
    public void renameEvent(UUID eventId, String newName) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted(SecuredDomainType.EVENT.getStringPermissionForTypeRelativeIdentifier(
                DefaultActions.UPDATE, EventBaseImpl.getTypeRelativeObjectIdentifier(eventId)))) {
            getService().apply(new RenameEvent(eventId, newName));
        } else {
            throw new UnauthorizedException("You are not permitted to edit event " + eventId);
        }
    }

    @Override
    public void removeRegattas(Collection<RegattaIdentifier> selectedRegattas) {
        for (RegattaIdentifier regatta : selectedRegattas) {
            removeRegatta(regatta);
        }
    }

    @Override
    public void removeRegatta(RegattaIdentifier regattaIdentifier) {
        Regatta regatta = getService().getRegatta(regattaIdentifier);
        if (regatta != null) {
            Set<QualifiedObjectIdentifier> objectsThatWillBeImplicitlyCleanedByRemoveRegatta = new HashSet<>();
            objectsThatWillBeImplicitlyCleanedByRemoveRegatta.add(regatta.getIdentifier());
            for (RaceDefinition race : regatta.getAllRaces()) {
                TypeRelativeObjectIdentifier typeRelativeObjectIdentifier = RegattaNameAndRaceName
                        .getTypeRelativeObjectIdentifier(regatta.getName(), race.getName());
                QualifiedObjectIdentifier identifier = SecuredDomainType.TRACKED_RACE
                        .getQualifiedObjectIdentifier(typeRelativeObjectIdentifier);
                objectsThatWillBeImplicitlyCleanedByRemoveRegatta.add(identifier);
            }
            for (Leaderboard leaderboard : getService().getLeaderboards().values()) {
                if (leaderboard instanceof RegattaLeaderboard) {
                    RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                    if (regattaLeaderboard.getRegatta() == regatta) {
                        objectsThatWillBeImplicitlyCleanedByRemoveRegatta.add(regattaLeaderboard.getIdentifier());
                    }
                }
            }
            // check if we can delete everything RemoveRegatta will remove
            for (QualifiedObjectIdentifier toRemovePermissionObjects : objectsThatWillBeImplicitlyCleanedByRemoveRegatta) {
                getSecurityService().checkCurrentUserDeletePermission(toRemovePermissionObjects);
            }
            // we have all permissions, execute
            getService().apply(new RemoveRegatta(regattaIdentifier));
            // cleanup the Ownership and ACLs
            for (QualifiedObjectIdentifier toRemovePermissionObjects : objectsThatWillBeImplicitlyCleanedByRemoveRegatta) {
                getSecurityService().deleteAllDataForRemovedObject(toRemovePermissionObjects);
            }
        }
    }

    @Override
    public void removeSeries(RegattaIdentifier identifier, String seriesName) {
        Regatta regatta = getService().getRegatta(identifier);
        if (regatta != null) {
            SecurityUtils.getSubject().checkPermission(
                    SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        }
        getService().apply(new RemoveSeries(identifier, seriesName));
    }

    @Override
    public void updateRegatta(RegattaIdentifier regattaName, Date startDate, Date endDate, List<UUID> courseAreaUuids,
            RegattaConfigurationDTO configurationDTO, Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference, boolean controlTrackingFromStartAndFinishTimes,
            boolean autoRestartTrackingUponCompetitorSetChange, String registrationLinkSecret, CompetitorRegistrationType registrationType) {
        Regatta regatta = getService().getRegatta(regattaName);
        if (regatta != null) {
            SecurityUtils.getSubject().checkPermission(SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        }
        TimePoint startTimePoint = startDate != null ? new MillisecondsTimePoint(startDate) : null;
        TimePoint endTimePoint = endDate != null ? new MillisecondsTimePoint(endDate) : null;
        getService().apply(new UpdateSpecificRegatta(regattaName, startTimePoint, endTimePoint, courseAreaUuids,
                convertToRegattaConfiguration(configurationDTO), buoyZoneRadiusInHullLengths, useStartTimeInference,
                controlTrackingFromStartAndFinishTimes, autoRestartTrackingUponCompetitorSetChange, registrationLinkSecret, registrationType));
    }

    @Override
    public List<RaceColumnInSeriesDTO> addRaceColumnsToSeries(RegattaIdentifier regattaIdentifier, String seriesName,
            List<Pair<String, Integer>> columnNamesWithInsertIndex) {
        Regatta regatta = getService().getRegatta(regattaIdentifier);
        if (regatta != null) {
            SecurityUtils.getSubject().checkPermission(SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        }
        List<RaceColumnInSeriesDTO> result = new ArrayList<RaceColumnInSeriesDTO>();
        for (Pair<String, Integer> columnNameAndInsertIndex : columnNamesWithInsertIndex) {
            RaceColumnInSeries raceColumnInSeries = getService().apply(
                    new AddColumnToSeries(columnNameAndInsertIndex.getB(), regattaIdentifier, seriesName, columnNameAndInsertIndex.getA()));
            if (raceColumnInSeries != null) {
                result.add(convertToRaceColumnInSeriesDTO(raceColumnInSeries));
            }
        }
        return result;
    }

    @Override
    public void updateSeries(RegattaIdentifier regattaIdentifier, String seriesName, String newSeriesName, boolean isMedal, boolean isFleetsCanRunInParallel,
            int[] resultDiscardingThresholds, boolean startsWithZeroScore,
            boolean firstColumnIsNonDiscardableCarryForward, boolean hasSplitFleetContiguousScoring,
            boolean hasCrossFleetMergedRanking,Integer maximumNumberOfDiscards, boolean oneAlwaysStaysOne, List<FleetDTO> fleets) {
        Regatta regatta = getService().getRegatta(regattaIdentifier);
        if (regatta != null) {
            SecurityUtils.getSubject().checkPermission(SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        }
        getService().apply(
                new UpdateSeries(regattaIdentifier, seriesName, newSeriesName, isMedal, isFleetsCanRunInParallel, resultDiscardingThresholds,
                        startsWithZeroScore, firstColumnIsNonDiscardableCarryForward, hasSplitFleetContiguousScoring, hasCrossFleetMergedRanking,
                        maximumNumberOfDiscards, oneAlwaysStaysOne, fleets));
    }

    @Override
    public void removeRaceColumnsFromSeries(RegattaIdentifier regattaIdentifier, String seriesName, List<String> columnNames) {
        Regatta regatta = getService().getRegatta(regattaIdentifier);
        if (regatta != null) {
            SecurityUtils.getSubject().checkPermission(SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        }
        for (String columnName: columnNames) {
            getService().apply(new RemoveColumnFromSeries(regattaIdentifier, seriesName, columnName));
        }
    }

    @Override
    public RegattaDTO createRegatta(String regattaName, String boatClassName,
            boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType, String registrationLinkSecret,
            Date startDate, Date endDate,
            RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedal,
            boolean persistent, ScoringSchemeType scoringSchemeType, List<UUID> courseAreaIds, Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange, RankingMetrics rankingMetricType) {
        logger.info("User "+SecurityUtils.getSubject().getPrincipal()+" is trying to create regatta "+regattaName);
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.REGATTA, Regatta.getTypeRelativeObjectIdentifier(regattaName),
                regattaName, new Callable<RegattaDTO>() {
                    @Override
                    public RegattaDTO call() throws Exception {
                        final TimePoint startTimePoint = startDate != null ? new MillisecondsTimePoint(startDate) : null;
                        final TimePoint endTimePoint = endDate != null ? new MillisecondsTimePoint(endDate) : null;
                        final Regatta regatta = getService().apply(new AddSpecificRegatta(regattaName, boatClassName,
                                canBoatsOfCompetitorsChangePerRace, competitorRegistrationType, registrationLinkSecret,
                                startTimePoint, endTimePoint, UUID.randomUUID(),
                                seriesNamesWithFleetNamesAndFleetOrderingAndMedal, persistent,
                                baseDomainFactory.createScoringScheme(scoringSchemeType), courseAreaIds,
                                buoyZoneRadiusInHullLengths, useStartTimeInference,
                                controlTrackingFromStartAndFinishTimes, autoRestartTrackingUponCompetitorSetChange, rankingMetricType));
                        return convertToRegattaDTO(regatta);
                    }
                });
    }

    private void createRegattaFromRegattaDTO(RegattaDTO regatta) {
        this.createRegatta(regatta.getName(), regatta.boatClass.getName(), regatta.canBoatsOfCompetitorsChangePerRace,
                regatta.competitorRegistrationType, regatta.registrationLinkSecret, regatta.startDate, regatta.endDate,
                new RegattaCreationParametersDTO(getSeriesCreationParameters(regatta)), true, regatta.scoringScheme,
                Util.mapToArrayList(regatta.courseAreas, CourseAreaDTO::getId), regatta.buoyZoneRadiusInHullLengths,
                regatta.useStartTimeInference, regatta.controlTrackingFromStartAndFinishTimes,
                regatta.autoRestartTrackingUponCompetitorSetChange, regatta.rankingMetricType);
    }

    /**
     * Uses {@link #addRaceColumnsToSeries} which also handles replication to update the regatta identified by
     * <code>regatta</code>'s {@link RegattaDTO#getRegattaIdentifier() identifier} with the race columns as specified by
     * <code>regatta</code>. The domain regatta object is assumed to have no races associated when this method is
     * called.
     */
    protected void addRaceColumnsToRegattaSeries(RegattaDTO regatta) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.REGATTA.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Regatta.getTypeRelativeObjectIdentifier(regatta.getName())));
        for (SeriesDTO series : regatta.series) {
            List<Pair<String, Integer>> raceNamesAndInsertIndex = new ArrayList<>();
            int insertIndex = 0;
            for (RaceColumnDTO raceColumnInSeries : series.getRaceColumns()) {
                raceNamesAndInsertIndex.add(new Pair<>(raceColumnInSeries.getName(), insertIndex));
                insertIndex++;
            }
            addRaceColumnsToSeries(regatta.getRegattaIdentifier(), series.getName(), raceNamesAndInsertIndex);
        }
    }

    protected RaceColumnInSeriesDTO convertToRaceColumnInSeriesDTO(RaceColumnInSeries raceColumnInSeries) {
        RaceColumnInSeriesDTO raceColumnInSeriesDTO = new RaceColumnInSeriesDTO(raceColumnInSeries.getName(),
                raceColumnInSeries.getSeries().getName(), raceColumnInSeries.getRegatta().getName(),
                raceColumnInSeries.isOneAlwaysStaysOne());
        fillRaceColumnDTO(raceColumnInSeries, raceColumnInSeriesDTO);
        return raceColumnInSeriesDTO;
    }

    @Override
    public UUID importMasterData(final String urlAsString, final UUID[] leaderboardGroupIds, final boolean override,
            final boolean compress, final boolean exportWind, final boolean exportDeviceConfigurations,
            String targetServerUsername, String targetServerPassword,
            final boolean exportTrackedRacesAndStartTracking) {
        final UUID importOperationId = UUID.randomUUID();
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CAN_IMPORT_MASTERDATA);
        final String targetServerBearerToken;
        if (!Util.hasLength(targetServerUsername) || !Util.hasLength(targetServerPassword)) {
            targetServerBearerToken = getSecurityService().getOrCreateAccessToken(getSecurityService().getCurrentUser().getName());
        } else {
            targetServerBearerToken = null;
        }
        // Create a progress indicator for as long as the server gets data from the other server.
        // As soon as the server starts the import operation, a progress object will be built on every server
        Runnable masterDataImportTask = new Runnable() {
            @Override
            public void run() {
                getService().importMasterData(urlAsString, leaderboardGroupIds, override, compress, exportWind,
                        exportDeviceConfigurations, targetServerUsername, targetServerPassword, targetServerBearerToken,
                        exportTrackedRacesAndStartTracking, importOperationId);
            }
        };
        // We need to convey the current user's credentials into the masterDataImportTask for default object ownership and permissions in case no
        // deviating username and password have been provided:
        executor.execute(ThreadPoolUtil.INSTANCE.associateWithSubjectIfAny(masterDataImportTask));
        return importOperationId;
    }

    @Override
    public List<CompetitorDTO> addOrUpdateCompetitors(List<CompetitorDTO> competitors) throws URISyntaxException {
        final List<CompetitorDTO> result = new ArrayList<>();
        for (final CompetitorDTO competitor : competitors) {
            if (competitor.hasBoat()) {
                final CompetitorWithBoatDTO competitorWithBoat = addOrUpdateCompetitorWithBoat(
                        (CompetitorWithBoatDTO) competitor);
                SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorWithBoat);
                result.add(competitorWithBoat);
            } else {
                final CompetitorDTO competitorWithoutBoat = addOrUpdateCompetitorWithoutBoat(competitor);
                SecurityDTOUtil.addSecurityInformation(getSecurityService(), competitorWithoutBoat);
                result.add(competitorWithoutBoat);
            }
        }
        return result;
    }

    private CompetitorWithBoat addOrUpdateCompetitorWithBoatInternal(CompetitorWithBoatDTO competitor)
            throws URISyntaxException {
        final CompetitorWithBoat result;
        CompetitorWithBoat existingCompetitor = getService().getCompetitorAndBoatStore()
                .getExistingCompetitorWithBoatByIdAsString(competitor.getIdAsString());
        Nationality nationality = (competitor.getThreeLetterIocCountryCode() == null
                || competitor.getThreeLetterIocCountryCode().isEmpty()) ? null
                        : getBaseDomainFactory().getOrCreateNationality(competitor.getThreeLetterIocCountryCode());
        if (competitor.getIdAsString() == null || competitor.getIdAsString().isEmpty() || existingCompetitor == null) {
            // new competitor
            final UUID competitorUUID = UUID.randomUUID();
            final DynamicPerson sailor = new PersonImpl(competitor.getName(), nationality, null, null);
            final DynamicTeam team = new TeamImpl(competitor.getName() + " team", Collections.singleton(sailor), null);
            // new boat
            final DynamicBoat boat = (DynamicBoat) addOrUpdateBoatInternal(competitor.getBoat());
            result = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredDomainType.COMPETITOR, CompetitorImpl.getTypeRelativeObjectIdentifier(competitorUUID),
                    competitor.getName(),
                    () -> getBaseDomainFactory().getCompetitorAndBoatStore().getOrCreateCompetitorWithBoat(
                            competitorUUID, competitor.getName(), competitor.getShortName(), competitor.getColor(),
                            competitor.getEmail(),
                            competitor.getFlagImageURL() == null ? null : new URI(competitor.getFlagImageURL()), team,
                            competitor.getTimeOnTimeFactor(), competitor.getTimeOnDistanceAllowancePerNauticalMile(),
                            competitor.getSearchTag(), boat, /* storePersistently */ true));
        } else {
            SecurityUtils.getSubject().checkPermission(
                    SecuredDomainType.COMPETITOR.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                            CompetitorImpl.getTypeRelativeObjectIdentifier(competitor.getId())));
            Competitor updatedCompetitor = getService().apply(new UpdateCompetitor(competitor.getIdAsString(),
                    competitor.getName(), competitor.getShortName(), competitor.getColor(), competitor.getEmail(),
                    nationality, competitor.getImageURL() == null ? null : new URI(competitor.getImageURL()),
                    competitor.getFlagImageURL() == null ? null : new URI(competitor.getFlagImageURL()),
                    competitor.getTimeOnTimeFactor(), competitor.getTimeOnDistanceAllowancePerNauticalMile(),
                    competitor.getSearchTag()));
            DynamicBoat updatedBoat = (DynamicBoat) addOrUpdateBoatInternal(competitor.getBoat());
            result = new CompetitorWithBoatImpl(updatedCompetitor, updatedBoat);
        }
        return result;
    }

    private Competitor addOrUpdateCompetitorWithoutBoatInternal(CompetitorDTO competitor) throws URISyntaxException {
        final Competitor result;
        Competitor existingCompetitor = getService().getCompetitorAndBoatStore()
                .getExistingCompetitorByIdAsString(competitor.getIdAsString());
        Nationality nationality = (competitor.getThreeLetterIocCountryCode() == null
                || competitor.getThreeLetterIocCountryCode().isEmpty()) ? null
                        : getBaseDomainFactory().getOrCreateNationality(competitor.getThreeLetterIocCountryCode());
        if (competitor.getIdAsString() == null || competitor.getIdAsString().isEmpty() || existingCompetitor == null) {
            // new competitor
            UUID competitorUUID = UUID.randomUUID();
            DynamicPerson sailor = new PersonImpl(competitor.getName(), nationality, null, null);
            DynamicTeam team = new TeamImpl(competitor.getName() + " team", Collections.singleton(sailor), null);
            result = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredDomainType.COMPETITOR, CompetitorImpl.getTypeRelativeObjectIdentifier(competitorUUID),
                    competitor.getName(),
                    () -> getBaseDomainFactory().getOrCreateCompetitor(competitorUUID, competitor.getName(),
                            competitor.getShortName(), competitor.getColor(), competitor.getEmail(),
                            competitor.getFlagImageURL() == null ? null : new URI(competitor.getFlagImageURL()), team,
                            competitor.getTimeOnTimeFactor(), competitor.getTimeOnDistanceAllowancePerNauticalMile(),
                            competitor.getSearchTag(), /* storePersistently */ true));
        } else {
            SecurityUtils.getSubject().checkPermission(
                    SecuredDomainType.COMPETITOR.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                            CompetitorImpl.getTypeRelativeObjectIdentifier(competitor.getId())));
            result = getService().apply(new UpdateCompetitor(competitor.getIdAsString(), competitor.getName(),
                    competitor.getShortName(), competitor.getColor(), competitor.getEmail(), nationality,
                    competitor.getImageURL() == null ? null : new URI(competitor.getImageURL()),
                    competitor.getFlagImageURL() == null ? null : new URI(competitor.getFlagImageURL()),
                    competitor.getTimeOnTimeFactor(), competitor.getTimeOnDistanceAllowancePerNauticalMile(),
                    competitor.getSearchTag()));
        }
        return result;
    }

    @Override
    public CompetitorDTO addOrUpdateCompetitorWithoutBoat(CompetitorDTO competitorDTO) throws URISyntaxException {
        Competitor competitor = addOrUpdateCompetitorWithoutBoatInternal(competitorDTO);
        return convertToCompetitorDTO(competitor);
    }

    @Override
    public CompetitorWithBoatDTO addOrUpdateCompetitorWithBoat(CompetitorWithBoatDTO competitorDTO)
            throws URISyntaxException {
        CompetitorWithBoat competitor = addOrUpdateCompetitorWithBoatInternal(competitorDTO);
        return convertToCompetitorWithBoatDTO(competitor);
    }

    @Override
    public List<CompetitorWithBoatDTO> addCompetitors(List<CompetitorDescriptor> competitorDescriptors,
            String searchTag) throws URISyntaxException {
        List<DynamicCompetitorWithBoat> competitorsForSaving = new ArrayList<>();
        for (final CompetitorDescriptor competitorDescriptor : competitorDescriptors) {
            final Action action = () -> competitorsForSaving
                    .add(getService().convertCompetitorDescriptorToCompetitorWithBoat(competitorDescriptor, searchTag));
            final Boat existingBoat = getService().getCompetitorAndBoatStore()
                    .getExistingBoatById(competitorDescriptor.getBoatId());
            final Action actionIncludingBoatSecurityCheck;
            if (existingBoat == null) {
                actionIncludingBoatSecurityCheck = () -> getSecurityService()
                        .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.BOAT,
                                BoatImpl.getTypeRelativeObjectIdentifier(competitorDescriptor.getBoatId()),
                                competitorDescriptor.getBoatName(), action);
            } else {
                actionIncludingBoatSecurityCheck = action;
            }
            final Competitor existingCompetitor = getService().getCompetitorAndBoatStore()
                    .getExistingCompetitorById(competitorDescriptor.getCompetitorId());
            final Action actionIncludingCompetitorAndBoatSecurityCheck;
            if (existingCompetitor == null) {
                actionIncludingCompetitorAndBoatSecurityCheck = () -> getSecurityService()
                        .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.COMPETITOR,
                                CompetitorImpl
                                        .getTypeRelativeObjectIdentifier(competitorDescriptor.getCompetitorId()),
                                competitorDescriptor.getName(), actionIncludingBoatSecurityCheck);
            } else {
                actionIncludingCompetitorAndBoatSecurityCheck = actionIncludingBoatSecurityCheck;
            }
            try {
                actionIncludingCompetitorAndBoatSecurityCheck.run();
            } catch (Exception e) {
                throw new RuntimeException(e); // this can onlyhave been a RuntimeException in the first place because
                                               // nothing of the above throws a checked one
            }
        }
        getBaseDomainFactory().getCompetitorAndBoatStore().addNewCompetitorsWithBoat(competitorsForSaving);
        return convertToCompetitorWithBoatDTOs(competitorsForSaving);
    }

    @Override
    public void allowCompetitorResetToDefaults(List<CompetitorDTO> competitors) {
        List<String> competitorIdsAsStrings = new ArrayList<String>();
        for (CompetitorDTO competitor : competitors) {
            getSecurityService().checkCurrentUserUpdatePermission(competitor);
            competitorIdsAsStrings.add(competitor.getIdAsString());
        }
        getService().apply(new AllowCompetitorResetToDefaults(competitorIdsAsStrings));
    }

    @Override
    public BoatDTO addOrUpdateBoat(BoatDTO boat) {
        return convertToBoatDTO(addOrUpdateBoatInternal(boat));
    }

    private Boat addOrUpdateBoatInternal(BoatDTO boat) {
        Boat existingBoat = getService().getCompetitorAndBoatStore().getExistingBoatByIdAsString(boat.getIdAsString());
        final Boat result;
        if (boat.getIdAsString() == null || boat.getIdAsString().isEmpty() || existingBoat == null) {
            // new boat
            UUID boatUUID = UUID.randomUUID();
            BoatClass boatClass = getBaseDomainFactory().getOrCreateBoatClass(boat.getBoatClass().getName());
            result = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredDomainType.BOAT, BoatImpl.getTypeRelativeObjectIdentifier(boatUUID), boat.getName(),
                    () -> getBaseDomainFactory().getOrCreateBoat(boatUUID, boat.getName(), boatClass, boat.getSailId(), boat.getColor(), /* storePersistently */ true));
        } else {
            SecurityUtils.getSubject().checkPermission(
                    SecuredDomainType.BOAT.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                            BoatImpl.getTypeRelativeObjectIdentifier(boat.getId())));
            result = getService()
                    .apply(new UpdateBoat(boat.getIdAsString(), boat.getName(), boat.getColor(), boat.getSailId()));
        }
        return result;
    }

    @Override
    public void allowBoatResetToDefaults(List<BoatDTO> boats) {
        List<String> boatIdsAsStrings = new ArrayList<String>();
        for (BoatDTO boat : boats) {
            getSecurityService().checkCurrentUserUpdatePermission(boat);
            boatIdsAsStrings.add(boat.getIdAsString());
        }
        getService().apply(new AllowBoatResetToDefaults(boatIdsAsStrings));
    }

    @Override
    public DataImportProgress getImportOperationProgress(UUID id) {
        return getService().getDataImportLock().getProgress(id);
    }

    @Override
    public boolean removeDeviceConfiguration(UUID deviceConfigurationId) {
        final DeviceConfiguration configuration = getService().getDeviceConfigurationById(deviceConfigurationId);
        getSecurityService().checkCurrentUserDeletePermission(configuration);
        getService().removeDeviceConfiguration(deviceConfigurationId);
        return true;
    }

    @Override
    public void setTrackingTimes(RaceLogSetTrackingTimesDTO dto) throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(dto.leaderboardName));
        RaceLog raceLog = getRaceLog(dto.leaderboardName, dto.raceColumnName, dto.fleetName);
        // the tracking start/end time events are not revoked; updates with null as TimePoint may be added instead
        final LogEventAuthorImpl author = new LogEventAuthorImpl(dto.authorName, dto.authorPriority);
        if (!Util.equalsWithNull(dto.newStartOfTracking, dto.currentStartOfTracking)) {
            if (dto.newStartOfTracking != null) {
                raceLog.add(new RaceLogStartOfTrackingEventImpl(dto.newStartOfTracking.getTimePoint(), author,
                        raceLog.getCurrentPassId()));
            } else {
                final Pair<RaceLogStartOfTrackingEvent, RaceLogEndOfTrackingEvent> trackingTimesEvents = new TrackingTimesEventFinder(
                        raceLog).analyze();
                // we assume to find a valid "set start of tracking time" event that matches the old start of tracking
                // time;
                // if the time doesn't match dto.currentStartOfTracking, the revocation is rejected with an exception;
                // the result of trying to revoke is returned otherwise, and it may not be the result intended in case
                // the author's priority was lower than that of the author of the event that is to be revoked.
                if (trackingTimesEvents == null || trackingTimesEvents.getA() == null || !Util.equalsWithNull(
                        trackingTimesEvents.getA().getLogicalTimePoint(),
                        dto.currentStartOfTracking == null ? null : dto.currentStartOfTracking.getTimePoint())) {
                    throw new NotFoundException("Old start of tracking time in the race log ("
                            + (trackingTimesEvents == null || trackingTimesEvents.getA() == null ? "unset"
                                    : trackingTimesEvents.getA().getLogicalTimePoint())
                            + ") does not match start of tracking time at transaction start ("
                            + dto.currentStartOfTracking == null ? null
                                    : dto.currentStartOfTracking.getTimePoint() + ")");
                } else {
                    try {
                        raceLog.revokeEvent(author, trackingTimesEvents.getA(), "resetting tracking start time");
                    } catch (NotRevokableException e) {
                        logger.log(Level.WARNING,
                                "Internal error: event " + trackingTimesEvents.getA() + " was expected to be revokable",
                                e);
                    }
                }
            }
        }
        if (!Util.equalsWithNull(dto.newEndOfTracking, dto.currentEndOfTracking)) {
            if (dto.newEndOfTracking != null) {
                raceLog.add(new RaceLogEndOfTrackingEventImpl(dto.newEndOfTracking.getTimePoint(), author,
                        raceLog.getCurrentPassId()));
            } else {
                final Pair<RaceLogStartOfTrackingEvent, RaceLogEndOfTrackingEvent> trackingTimesEvents = new TrackingTimesEventFinder(
                        raceLog).analyze();
                // we assume to find a valid "set start of tracking time" event that matches the old start of tracking
                // time;
                // if the time doesn't match dto.currentStartOfTracking, the revocation is rejected with an exception;
                // the result of trying to revoke is returned otherwise, and it may not be the result intended in case
                // the author's priority was lower than that of the author of the event that is to be revoked.
                if (trackingTimesEvents == null || trackingTimesEvents.getB() == null
                        || !Util.equalsWithNull(trackingTimesEvents.getB().getLogicalTimePoint(),
                                dto.currentEndOfTracking == null ? null : dto.currentEndOfTracking.getTimePoint())) {
                    throw new NotFoundException("Old end of tracking time in the race log ("
                            + (trackingTimesEvents == null || trackingTimesEvents.getB() == null ? "unset"
                                    : trackingTimesEvents.getB().getLogicalTimePoint())
                            + ") does not match end of tracking time at transaction start ("
                            + dto.currentEndOfTracking == null ? null : dto.currentEndOfTracking.getTimePoint() + ")");
                } else {
                    try {
                        raceLog.revokeEvent(author, trackingTimesEvents.getB(), "resetting tracking end time");
                    } catch (NotRevokableException e) {
                        logger.log(Level.WARNING,
                                "Internal error: event " + trackingTimesEvents.getB() + " was expected to be revokable",
                                e);
                    }
                }
            }
        }
    }

    @Override
    public IgtimiDataAccessWindowWithSecurityDTO addIgtimiDataAccessWindow(String deviceSerialNumber, Date from, Date to) {
        final DataAccessWindow result = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.IGTIMI_DATA_ACCESS_WINDOW,
                new TypeRelativeObjectIdentifier(deviceSerialNumber, ""+from.getTime(), ""+to.getTime()),
                "Data Access Window for device "+deviceSerialNumber+" from "+from+" to "+to,
                ()->getRiotServer().createDataAccessWindow(deviceSerialNumber, TimePoint.of(from), TimePoint.of(to)));
        final IgtimiDataAccessWindowWithSecurityDTO resultWithSecurity = new IgtimiDataAccessWindowWithSecurityDTO(result.getId(), result.getDeviceSerialNumber(),
                result.getStartTime().asDate(), result.getEndTime().asDate());
        SecurityDTOUtil.addSecurityInformation(getSecurityService(), resultWithSecurity);
        return resultWithSecurity;
    }

    @Override
    public void removeIgtimiDataAccessWindow(long id) {
        final RiotServer riotServer = getRiotServer();
        final DataAccessWindow daw = riotServer.getDataAccessWindowById(id);
        if (daw != null) {
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(daw, ()->getRiotServer().removeDataAccessWindow(id));
        }
    }

    @Override
    public void updateIgtimiDevice(IgtimiDeviceWithSecurityDTO editedObject) {
        final RiotServer riotServer = getRiotServer();
        final Device existingDevice = riotServer.getDeviceBySerialNumber(editedObject.getSerialNumber());
        if (existingDevice != null) {
            getSecurityService().checkCurrentUserUpdatePermission(existingDevice);
            riotServer.updateDeviceName(existingDevice.getId(), editedObject.getName());
        }
    }

    @Override
    public void removeIgtimiDevice(String serialNumber) {
        final RiotServer riotServer = getRiotServer();
        final Device existingDevice = riotServer.getDeviceBySerialNumber(serialNumber);
        if (existingDevice != null) {
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(existingDevice, () -> {
                riotServer.removeDevice(existingDevice.getId());
            });
        }
    }
    
    private void checkCurrentUserUpdatePermissionForIgtimiDevice(String serialNumber) {
        final Device existingDevice = getIgtimiDevice(serialNumber);
        if (existingDevice != null) {
            getSecurityService().checkCurrentUserUpdatePermission(existingDevice);
        }
    }

    private Device getIgtimiDevice(String serialNumber) {
        final RiotServer riotServer = getRiotServer();
        final Device existingDevice = riotServer.getDeviceBySerialNumber(serialNumber);
        return existingDevice;
    }

    private void checkCurrentUserReadPermissionForIgtimiDevice(String serialNumber) {
        final Device existingDevice = getIgtimiDevice(serialNumber);
        if (existingDevice != null) {
            getSecurityService().checkCurrentUserReadPermission(existingDevice);
        }
    }

    @Override
    public boolean sendGPSOffCommandToIgtimiDevice(String serialNumber) throws IOException {
        checkCurrentUserUpdatePermissionForIgtimiDevice(serialNumber);
        return getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_GPS_OFF);
    }

    @Override
    public boolean sendGPSOnCommandToIgtimiDevice(String serialNumber) throws IOException {
        checkCurrentUserUpdatePermissionForIgtimiDevice(serialNumber);
        return getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_GPS_ON);
    }

    @Override
    public boolean sendPowerOffCommandToIgtimiDevice(String serialNumber) throws IOException {
        checkCurrentUserUpdatePermissionForIgtimiDevice(serialNumber);
        return getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_POWER_OFF);
    }

    @Override
    public boolean sendRestartCommandToIgtimiDevice(String serialNumber) throws IOException {
        checkCurrentUserUpdatePermissionForIgtimiDevice(serialNumber);
        return getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_RESTART);
    }
    
    @Override
    public boolean sendIMUCalibrationCommandSequenceToIgtimiDevice(String serialNumber) throws IOException, InterruptedException {
        checkCurrentUserUpdatePermissionForIgtimiDevice(serialNumber);
        boolean result = true;
        result = getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_IMU_STOP) && result;
        Thread.sleep(1000); // wait for 1s for the command to process
        result = getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_IMU_GYROCAL_PERFORM) && result;
        Thread.sleep(1000); // wait for 1s for the command to process
        result = getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_IMU_CAL_FROM_FILE) && result;
        Thread.sleep(1000); // wait for 1s for the command to process
        result = getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_IMU_GYROCAL_PERFORM) && result;
        Thread.sleep(1000); // wait for 1s for the command to process
        result = getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_IMU_SAVE) && result;
        Thread.sleep(1000); // wait for 1s for the command to process
        result = getRiotServer().sendStandardCommand(serialNumber, RiotStandardCommand.CMD_IMU_ON) && result;
        return result;
    }

    @Override
    public boolean sendIgtimiCommand(String serialNumber, String command) throws IOException, InterruptedException {
        checkCurrentUserUpdatePermissionForIgtimiDevice(serialNumber);
        return getRiotServer().sendFreestyleCommand(serialNumber, command);
    }
    
    @Override
    public boolean enableIgtimiDeviceOverTheAirLog(String deviceSerialNumber, boolean enable) throws Exception {
        checkCurrentUserUpdatePermissionForIgtimiDevice(deviceSerialNumber);
        return getRiotServer().enableOverTheAirLog(deviceSerialNumber, enable);
    }
    
    @Override
    public ArrayList<Pair<TimePoint, String>> getIgtimiDeviceLogs(String serialNumber, Duration duration) throws IOException, org.json.simple.parser.ParseException {
        checkCurrentUserReadPermissionForIgtimiDevice(serialNumber);
        return Util.mapToArrayList(getRiotServer().getDeviceLogs(serialNumber, duration), s->s);
    }

    @Override
    public Map<RegattaAndRaceIdentifier, Integer> importWindFromIgtimi(List<RaceDTO> selectedRaces,
            boolean correctByDeclination, String optionalBearerTokenOrNull)
            throws IllegalStateException, ClientProtocolException, IOException, org.json.simple.parser.ParseException {
        final List<DynamicTrackedRace> trackedRaces = new ArrayList<>();
        if (selectedRaces != null && !selectedRaces.isEmpty()) {
            for (RaceDTO raceDTO : selectedRaces) {
                final DynamicTrackedRace trackedRace = getTrackedRace(raceDTO.getRaceIdentifier());
                if (trackedRace != null) {
                    // In case the user selected a distinct set of races, we want the call to completely fail if
                    // tracking wind isn't allowed for at least one race
                    getSecurityService().checkCurrentUserUpdatePermission(trackedRace);
                    trackedRaces.add(trackedRace);
                }
            }
        } else {
            for (DynamicTrackedRace race : getAllTrackedRaces()) {
                // In case the user likes to track wind for all races, we silently filter for the ones
                // for which the user has sufficient permissions
                if (getSecurityService().hasCurrentUserUpdatePermission(race)) {
                    trackedRaces.add(race);
                }
            }
        }
        Map<RegattaAndRaceIdentifier, Integer> numberOfWindFixesImportedPerRace = new HashMap<RegattaAndRaceIdentifier, Integer>();
        final IgtimiConnection conn = createIgtimiConnection(Optional.ofNullable(optionalBearerTokenOrNull));
        // filter account based on used permissions to read account:
        Map<TrackedRace, Integer> resultsForAccounts = conn.importWindIntoRace(trackedRaces, correctByDeclination);
        for (Entry<TrackedRace, Integer> resultForAccount : resultsForAccounts.entrySet()) {
            RegattaAndRaceIdentifier key = resultForAccount.getKey().getRaceIdentifier();
            Integer i = numberOfWindFixesImportedPerRace.get(key);
            if (i == null) {
                i = 0;
            }
            numberOfWindFixesImportedPerRace.put(key, i + resultForAccount.getValue());
        }
        for (final TrackedRace trackedRace : trackedRaces) {
            // update polar sheets:
            getService().getPolarDataService().insertExistingFixes(trackedRace);
        }
        return numberOfWindFixesImportedPerRace;
    }

    @Override
    public Boolean denoteForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName)
            throws NotFoundException, NotDenotableForRaceLogTrackingException {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);
        return getRaceLogTrackingAdapter().denoteRaceForRaceLogTracking(getService(), leaderboard, raceColumn, fleet,
                null);
    }

    @Override
    public void removeDenotationForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName)
            throws Exception {
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        final TrackedRace trackedRace = raceColumn.getTrackedRace(raceColumn.getFleetByName(fleetName));
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserDeletePermission(trackedRace);
            removeAndUntrackRaces(Arrays.asList(trackedRace.getRaceIdentifier()));
        }
        getRaceLogTrackingAdapter().removeDenotationForRaceLogTracking(getService(), raceLog);
    }

    @Override
    public void denoteForRaceLogTracking(String leaderboardName, String prefix) throws Exception {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        getRaceLogTrackingAdapter().denoteAllRacesForRaceLogTracking(getService(), leaderboard, prefix);
    }

    @Override
    public void setCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            Map<? extends CompetitorDTO, BoatDTO> competitorAndBoatDTOs)
            throws CompetitorRegistrationOnRaceLogDisabledException, NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);

        Map<Competitor, Boat> competitorToBoatMappingsToRegister = new HashMap<>();
        for (Entry<? extends CompetitorDTO, BoatDTO> competitorAndBoatEntry : competitorAndBoatDTOs.entrySet()) {
            competitorToBoatMappingsToRegister.put(getCompetitor(competitorAndBoatEntry.getKey()),
                    getBoat(competitorAndBoatEntry.getValue()));
        }

        final Iterable<Competitor> competitorRegistrationsToRemove = filterCompetitorDuplicates(
                competitorToBoatMappingsToRegister, raceColumn.getCompetitorsRegisteredInRacelog(fleet));
        raceColumn.deregisterCompetitors(competitorRegistrationsToRemove, fleet);
        // we assume that the competitors id of type Competitor here, so we need to find the corresponding boat
        for (final Entry<Competitor, Boat> competitorAndBoatToRegister : competitorToBoatMappingsToRegister
                .entrySet()) {
            raceColumn.registerCompetitor(competitorAndBoatToRegister.getKey(), competitorAndBoatToRegister.getValue(),
                    fleet);
        }
    }

    @Override
    public void setCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            Set<CompetitorWithBoatDTO> competitorDTOs)
            throws CompetitorRegistrationOnRaceLogDisabledException, NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
        Fleet fleet = getFleetByName(raceColumn, fleetName);
        Map<CompetitorWithBoat, Boat> competitorsToRegister = new HashMap<>();
        for (CompetitorWithBoatDTO dto : competitorDTOs) {
            competitorsToRegister.put(getCompetitor(dto), getBoat(dto.getBoat()));
        }
        Map<CompetitorWithBoat, Boat> competitorsRegisteredInRaceLog = new HashMap<>();
        for (final Entry<Competitor, Boat> e : raceColumn.getCompetitorsRegisteredInRacelog(fleet).entrySet()) {
            competitorsRegisteredInRaceLog.put((CompetitorWithBoat) e.getKey(), e.getValue());
        }
        final Iterable<CompetitorWithBoat> competitorSetToRemove = filterCompetitorDuplicates(competitorsToRegister,
                competitorsRegisteredInRaceLog);
        raceColumn.deregisterCompetitors(competitorSetToRemove, fleet);
        // we assume that the competitors id of type Competitor here, so we need to find the corresponding boat
        for (CompetitorWithBoat competitorToRegister : competitorsToRegister.keySet()) {
            if (competitorToRegister.hasBoat()) {
                raceColumn.registerCompetitor(competitorToRegister, fleet);
            } else {
                logger.warning("The competitor " + competitorToRegister.getName()
                        + " does not have a boat associated but should have; "
                        + "competitor is not registered for race log of race " + raceColumnName + " in leaderboard "
                        + leaderboardName + " for fleet " + fleetName);
            }
        }
    }

    @Override
    public void setCompetitorRegistrationsInRegattaLog(String leaderboardName,
            Set<? extends CompetitorDTO> competitorDTOs) throws DoesNotHaveRegattaLogException, NotFoundException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        if (!(leaderboard instanceof HasRegattaLike)) {
            throw new DoesNotHaveRegattaLogException();
        }
        Map<Competitor, Boat> competitorsToRegister = new HashMap<>();
        for (CompetitorDTO dto : competitorDTOs) {
            competitorsToRegister.put(getCompetitor(dto), /* boat doesn't matter here */ null);
        }
        HasRegattaLike hasRegattaLike = (HasRegattaLike) leaderboard;
        Map<Competitor, Boat> competitorsAlreadyRegistered = new HashMap<>();
        for (final Competitor c : leaderboard.getAllCompetitors()) {
            competitorsAlreadyRegistered.put(c, /* boat doesn't matter here */ null);
        }
        final Iterable<Competitor> competitorSetToRemove = filterCompetitorDuplicates(competitorsToRegister,
                competitorsAlreadyRegistered);
        hasRegattaLike.deregisterCompetitors(competitorSetToRemove);
        hasRegattaLike.registerCompetitors(competitorsToRegister.keySet());
    }

    /**
     * Removes competitors already registered to the same boats (those in {@code competitorsRegistered}) from
     * {@code competitorsToRegister} to avoid registering them again and then returns those competitors that need to be
     * de-registered. Those to de-register includes those registered but to a different boat, and those will be left in
     * the {@code competitorToBoatMappingsToRegister} map.
     *
     * @param competitorToBoatMappingsToRegister
     *            will be modified by removing all competitors in {@code competitorsRegistered}
     * @param competitorToBoatMappingsRegistered
     *            the competitors already registered; those will be removed from {@code competitorsToRegister}
     * @return the competitors to de-register because they were in {@code competitorsRegistered} but are not in
     *         {@code competitorsToRegister}
     */
    private <CompetitorType extends Competitor> Iterable<CompetitorType> filterCompetitorDuplicates(
            Map<CompetitorType, Boat> competitorToBoatMappingsToRegister,
            Map<CompetitorType, Boat> competitorToBoatMappingsRegistered) {
        final Set<CompetitorType> competitorsToUnregister = new HashSet<>();
        Util.addAll(competitorToBoatMappingsRegistered.keySet(), competitorsToUnregister);
        for (final Entry<CompetitorType, Boat> e : competitorToBoatMappingsRegistered.entrySet()) {
            CompetitorType competitor = e.getKey();
            if (competitorToBoatMappingsToRegister.containsKey(competitor)) { // is competitor to be registered?
                final Boat boatOfCompetitorToRegister = competitorToBoatMappingsToRegister.get(competitor);
                final Boat boatOfRegisteredCompetitor = e.getValue();
                if (boatOfCompetitorToRegister == boatOfRegisteredCompetitor) {
                    // User wants to map competitor to boat, and that mapping already exists; neither add nor remove
                    // this registration but leave as is:
                    competitorToBoatMappingsToRegister.remove(competitor);
                    competitorsToUnregister.remove(competitor);
                }
            }
        }
        return competitorsToUnregister;
    }

    private HashSet<Boat> filterBoatDuplicates(Set<Boat> boatsToRegister, HashSet<Boat> boatSetToRemove) {
        for (Iterator<Boat> iterator = boatSetToRemove.iterator(); iterator.hasNext();) {
            Boat boat = iterator.next();
            if (boatsToRegister.remove(boat)) {
                iterator.remove();
            }
        }
        return boatSetToRemove;
    }

    @Override
    public void setBoatRegistrationsInRegattaLog(String leaderboardName, Set<BoatDTO> boatDTOs)
            throws DoesNotHaveRegattaLogException, NotFoundException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        if (!(leaderboard instanceof HasRegattaLike)) {
            throw new DoesNotHaveRegattaLogException();
        }

        Set<Boat> boatsToRegister = new HashSet<Boat>();
        for (BoatDTO dto : boatDTOs) {
            final Boat boat = getBoat(dto);
            getSecurityService().checkCurrentUserReadPermission(boat);
            boatsToRegister.add(boat);
        }

        HasRegattaLike hasRegattaLike = (HasRegattaLike) leaderboard;
        Iterable<Boat> boatsToRemove = leaderboard.getAllBoats();
        HashSet<Boat> boatSetToRemove = new HashSet<>();
        Util.addAll(boatsToRemove, boatSetToRemove);
        filterBoatDuplicates(boatsToRegister, boatSetToRemove);

        hasRegattaLike.deregisterBoats(boatSetToRemove);
        hasRegattaLike.registerBoats(boatsToRegister);
    }

    @Override
    public void fillRaceLogsFromPairingListTemplate(final String leaderboardName, final int flightMultiplier,
            final List<String> selectedFlightNames, final PairingListDTO pairingListDTO)
            throws NotFoundException, CompetitorRegistrationOnRaceLogDisabledException {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        int flightCount = 0;
        int groupCount = 0;
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            groupCount = 0;
            for (Fleet fleet : raceColumn.getFleets()) {
                raceColumn.enableCompetitorRegistrationOnRaceLog(fleet);
                if (Util.contains(selectedFlightNames, raceColumn.getName())) {
                    Map<CompetitorDTO, BoatDTO> competitors = new HashMap<>();
                    List<Pair<CompetitorDTO, BoatDTO>> competitorsFromPairingList = pairingListDTO.getPairingList()
                            .get(flightCount).get(groupCount);
                    for (Pair<CompetitorDTO, BoatDTO> competitorAndBoatPair : competitorsFromPairingList) {
                        if (competitorAndBoatPair.getA() != null && competitorAndBoatPair.getA().getName() != null) {
                            competitors.put(competitorAndBoatPair.getA(), competitorAndBoatPair.getB());
                        }
                    }
                    this.setCompetitorRegistrationsInRaceLog(leaderboard.getName(), raceColumn.getName(),
                            fleet.getName(), competitors);
                    groupCount++;
                } else {
                    this.setCompetitorRegistrationsInRaceLog(leaderboard.getName(), raceColumn.getName(),
                            fleet.getName(), new HashSet<CompetitorWithBoatDTO>());
                }
            }
            flightCount++;
        }
        if (leaderboard instanceof LeaderboardThatHasRegattaLike && flightMultiplier > 1) {
            final IsRegattaLike regattaLike = ((LeaderboardThatHasRegattaLike) leaderboard).getRegattaLike();
            logger.info("Updating regatta " + regattaLike.getRegattaLikeIdentifier().getName()
                    + ", setting flag that fleets can run in parallel because a pairing list with flight multiplier "
                    + flightMultiplier + " has been used.");
            regattaLike.setFleetsCanRunInParallelToTrue();
        }
    }

    @Override
    public void addMarkToRegattaLog(String leaderboardName, MarkDTO markDTO) throws DoesNotHaveRegattaLogException {
        Mark mark = convertToMark(markDTO, false);
        getService().addMarkToRegattaLog(leaderboardName, mark);
    }

    @Override
    public void revokeMarkDefinitionEventInRegattaLog(String leaderboardName, String raceColumnName, String fleetName, MarkDTO markDTO)
            throws DoesNotHaveRegattaLogException, MarkAlreadyUsedInRaceException {
        getService().revokeMarkDefinitionEventInRegattaLog(leaderboardName, raceColumnName, fleetName, markDTO.getIdAsString());
    }

    @Override
    public void addCourseDefinitionToRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            List<com.sap.sse.common.Util.Pair<ControlPointDTO, PassingInstruction>> courseDTO, int priority)
            throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        String courseName = "Course of " + raceColumnName;
        if (!LeaderboardNameConstants.DEFAULT_FLEET_NAME.equals(fleetName)) {
            courseName += " - " + fleetName;
        }
        CourseBase lastPublishedCourse = new LastPublishedCourseDesignFinder(raceLog,
                /* onlyCoursesWithValidWaypointList */ false).analyze();
        if (lastPublishedCourse == null) {
            lastPublishedCourse = new CourseDataImpl(courseName);
        }

        List<Pair<ControlPoint, PassingInstruction>> controlPoints = new ArrayList<>();
        for (Pair<ControlPointDTO, PassingInstruction> waypointDTO : courseDTO) {
            controlPoints.add(new Pair<>(getOrCreateControlPoint(waypointDTO.getA()), waypointDTO.getB()));
        }
        Course course = new CourseImpl(courseName, lastPublishedCourse.getWaypoints());

        try {
            course.update(controlPoints, lastPublishedCourse.getAssociatedRoles(),
                    lastPublishedCourse.getOriginatingCourseTemplateIdOrNull(), baseDomainFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        RaceLogEvent event = new RaceLogCourseDesignChangedEventImpl(MillisecondsTimePoint.now(),
                new LogEventAuthorImpl(getService().getServerAuthor().getName(), priority), raceLog.getCurrentPassId(),
                course, CourseDesignerMode.ADMIN_CONSOLE);
        raceLog.add(event);
    }

    @Override
    public void pingMark(String leaderboardName, MarkDTO markDTO, TimePoint timePoint, Position positionDTO)
            throws DoesNotHaveRegattaLogException, NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        final RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        final Mark mark = convertToMark(markDTO, true);
        final TimePoint time = timePoint == null ? MillisecondsTimePoint.now() : timePoint;
        final Position position = positionDTO;
        final GPSFix fix = new GPSFixImpl(position, time);
        getRaceLogTrackingAdapter().pingMark(regattaLog, mark, fix, getService());
    }

    @Override
    public void copyCourseToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples, boolean copyMarkDeviceMappings, int priority)
            throws NotFoundException {
        final LeaderboardThatHasRegattaLike fromLeaderboard = (LeaderboardThatHasRegattaLike) getLeaderboardByName(fromTriple.getA());
        getSecurityService().checkCurrentUserReadPermission(fromLeaderboard);
        LeaderboardThatHasRegattaLike toLeaderboard = null;
        for (Triple<String, String, String> toTriple : toTriples) {
            toLeaderboard = (LeaderboardThatHasRegattaLike) getLeaderboardByName(toTriple.getA()); // they should all be the same
            getSecurityService().checkCurrentUserUpdatePermission(toLeaderboard);
        }
        RaceLog fromRaceLog = getRaceLog(fromTriple);
        Set<RaceLog> toRaceLogs = new HashSet<>();
        for (Triple<String, String, String> toTriple : toTriples) {
            toRaceLogs.add(getRaceLog(toTriple));
        }
        getRaceLogTrackingAdapter().copyCourse(fromRaceLog, fromLeaderboard, toRaceLogs, toLeaderboard,
                copyMarkDeviceMappings, baseDomainFactory, getService(), priority);
    }

    @Override
    public void copyCompetitorsToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples) throws NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(fromTriple.getA()));
        for (Triple<String, String, String> toTriple : toTriples) {
            getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(toTriple.getA()));
        }
        final RaceColumn raceColumn = getRaceColumn(fromTriple.getA(), fromTriple.getB());
        final Set<Pair<RaceColumn, Fleet>> toRaces = new HashSet<>();
        for (Triple<String, String, String> toTriple : toTriples) {
            final RaceColumn toRaceColumn = getRaceColumn(toTriple.getA(), toTriple.getB());
            final Fleet toFleet = getFleetByName(toRaceColumn, toTriple.getC());
            toRaces.add(new Pair<>(toRaceColumn, toFleet));
        }
        getRaceLogTrackingAdapter().copyCompetitors(raceColumn, getFleetByName(raceColumn, fromTriple.getC()), toRaces);
    }

    @Override
    public void addDeviceMappingToRegattaLog(String leaderboardName, DeviceMappingDTO dto)
            throws NoCorrespondingServiceRegisteredException, TransformationException, DoesNotHaveRegattaLogException {
        SecurityUtils.getSubject().checkPermission(SecuredDomainType.REGATTA
                .getStringPermissionForObject(DefaultActions.UPDATE, getService().getRegattaByName(leaderboardName)));
        RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        DeviceMapping<?> mapping = convertToDeviceMapping(dto);
        TimePoint now = MillisecondsTimePoint.now();
        RegattaLogEvent event = null;
        TimePoint from = mapping.getTimeRange().hasOpenBeginning() ? null : mapping.getTimeRange().from();
        TimePoint to = mapping.getTimeRange().hasOpenEnd() ? null : mapping.getTimeRange().to();
        if (mapping.getMappedTo() instanceof Mark) {
            Mark mark = (Mark) mapping.getMappedTo();
            event = new RegattaLogDeviceMarkMappingEventImpl(now, now, getService().getServerAuthor(),
                    UUID.randomUUID(), mark, mapping.getDevice(), from, to);
        } else if (mapping.getMappedTo() instanceof Competitor) {
            Competitor competitor = (Competitor) mapping.getMappedTo();
            if (mapping.getDevice().getIdentifierType().equals(ExpeditionSensorDeviceIdentifier.TYPE)) {
                event = new RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl(now, now,
                        getService().getServerAuthor(), UUID.randomUUID(), competitor, mapping.getDevice(), from, to);
            } else {
                event = new RegattaLogDeviceCompetitorMappingEventImpl(now, now, getService().getServerAuthor(),
                        UUID.randomUUID(), competitor, mapping.getDevice(), from, to);
            }
        } else if (mapping.getMappedTo() instanceof Boat) {
            final Boat boat = (Boat) mapping.getMappedTo();
            event = new RegattaLogDeviceBoatMappingEventImpl(now, now, getService().getServerAuthor(),
                    UUID.randomUUID(), boat, mapping.getDevice(), from, to);
        } else {
            throw new RuntimeException("Can only map devices to competitors, boats or marks");
        }
        regattaLog.add(event);
    }

    /**
     * Uses the device mapping's {@link TypedDeviceMappingDTO#dataType dataType} to
     * {@link #getRegisteredImporter(Class, String) determine} a matching {@link DoubleVectorFixImporter} for sensor
     * data. This importer is then used to create the {@link RegattaLogEvent} for the device mapping which is then added
     * to the {@link RegattaLog} for the {@link Leaderboard} identified by the {@code leaderboardName}.
     */
    @Override
    public void addTypedDeviceMappingToRegattaLog(String leaderboardName, TypedDeviceMappingDTO dto)
            throws NoCorrespondingServiceRegisteredException, TransformationException, DoesNotHaveRegattaLogException,
            NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        DeviceMapping<?> mapping = convertToDeviceMapping(dto);
        TimePoint now = MillisecondsTimePoint.now();
        RegattaLogEvent event = null;
        TimePoint from = mapping.getTimeRange().hasOpenBeginning() ? null : mapping.getTimeRange().from();
        TimePoint to = mapping.getTimeRange().hasOpenEnd() ? null : mapping.getTimeRange().to();
        final DoubleVectorFixImporter importer = getRegisteredImporter(DoubleVectorFixImporter.class, dto.dataType);
        if (dto.mappedTo instanceof CompetitorWithBoatDTO) {
            event = importer.createEvent(now, now, getService().getServerAuthor(), UUID.randomUUID(),
                    getCompetitor((CompetitorWithBoatDTO) dto.mappedTo), mapping.getDevice(), from, to);
        } else if (dto.mappedTo instanceof BoatDTO) {
            event = importer.createEvent(now, now, getService().getServerAuthor(), UUID.randomUUID(),
                    getBoat((BoatDTO) dto.mappedTo), mapping.getDevice(), from, to);
        } else {
            throw new RuntimeException("Can only map devices to a competitor or boat");
        }
        regattaLog.add(event);
    }

    private void closeOpenEndedDeviceMapping(final RegattaLog regattaLog, DeviceMappingDTO mappingDTO, Date closingTimePoint)
            throws TransformationException, UnableToCloseDeviceMappingException {
        boolean successfullyClosed = false;
        List<RegattaLogCloseOpenEndedDeviceMappingEvent> closingEvents = null;
        regattaLog.lockForRead();
        try {
            RegattaLogEvent event = regattaLog.getEventById(mappingDTO.originalRaceLogEventIds.get(0));
            if (event != null) {
                successfullyClosed = true;
                DeviceMapping<?> mapping = convertToDeviceMapping(mappingDTO);
                closingEvents = new RegattaLogOpenEndedDeviceMappingCloser(regattaLog, mapping,
                        getService().getServerAuthor(), new MillisecondsTimePoint(closingTimePoint)).analyze();
            }
        } finally {
            regattaLog.unlockAfterRead();
        }
        // important: read lock must be release before write lock is obtained in add(...); see bug 3774
        if (successfullyClosed) {
            for (RegattaLogEvent closingEvent : closingEvents) {
                regattaLog.add(closingEvent);
            }
        } else {
            throw new UnableToCloseDeviceMappingException();
        }
    }

    @Override
    public void revokeRaceAndRegattaLogEvents(String leaderboardName,
            List<UUID> eventIds) throws NotRevokableException, DoesNotHaveRegattaLogException, NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        List<AbstractLog<?, ?>> logs = getLogHierarchy(leaderboardName);
        revokeEventsFromLogs(logs, eventIds);
    }

    private void revokeEventsFromLogs(List<AbstractLog<?, ?>> logs, List<UUID> eventIds) throws NotRevokableException{
        boolean eventRevoked = false;
        for (Serializable idToRevoke : eventIds) {
            eventRevoked = false;
            for (AbstractLog<?, ?> abstractLog : logs) {
                eventRevoked = revokeEvent(eventRevoked, idToRevoke, abstractLog);
            }
            if (!eventRevoked){
                logger.warning("Could not revoke event with id "+idToRevoke);
            }
        }
    }

    private <EventT extends AbstractLogEvent<VisitorT>, VisitorT> boolean revokeEvent(boolean eventRevoked, Serializable idToRevoke, final AbstractLog<EventT, VisitorT> abstractLog)
            throws NotRevokableException {
        final EventT event;
        abstractLog.lockForRead();
        try {
            event = abstractLog.getEventById(idToRevoke);
        } finally {
            abstractLog.unlockAfterRead();
        }
        if (event != null) {
            abstractLog.revokeEvent(getService().getServerAuthor(), event, "revoke triggered by GWT user action");
            eventRevoked = true;
        }
        return eventRevoked;
    }

    private void startRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName, final boolean trackWind, final boolean correctWindByDeclination)
            throws NotDenotedForRaceLogTrackingException, Exception {
        // no permission checks needed here, since they already exist in PermissionAwareRaceTrackingHandler
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        Fleet fleet = raceColumn.getFleetByName(fleetName);
        getRaceLogTrackingAdapter().startTracking(getService(), leaderboard, raceColumn, fleet, trackWind, correctWindByDeclination,
                getService().getPermissionAwareRaceTrackingHandler());
    }

    @Override
    public void startRaceLogTracking(List<Triple<String, String, String>> leaderboardRaceColumnFleetNames,
            final boolean trackWind, final boolean correctWindByDeclination)
            throws NotDenotedForRaceLogTrackingException, Exception {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CREATE_OBJECT);
        for (final Triple<String, String, String> leaderboardRaceColumnFleetName : leaderboardRaceColumnFleetNames) {
            startRaceLogTracking(leaderboardRaceColumnFleetName.getA(), leaderboardRaceColumnFleetName.getB(),
                    leaderboardRaceColumnFleetName.getC(), trackWind, correctWindByDeclination);
        }
    }

    @Override
    public RaceDTO setStartTimeReceivedForRace(RaceIdentifier raceIdentifier, Date newStartTimeReceived) {
        RegattaNameAndRaceName regattaAndRaceIdentifier = new RegattaNameAndRaceName(raceIdentifier.getRegattaName(),
                raceIdentifier.getRaceName());
        final DynamicTrackedRace trackedRace = getService().getTrackedRace(regattaAndRaceIdentifier);
        if (trackedRace != null) {
            getSecurityService().checkCurrentUserUpdatePermission(trackedRace);
            trackedRace.setStartTimeReceived(
                    newStartTimeReceived == null ? null : new MillisecondsTimePoint(newStartTimeReceived));
        }
        return trackedRace == null ? null : baseDomainFactory.createRaceDTO(getService(), false, regattaAndRaceIdentifier, trackedRace);
    }

    @Override
    public void closeOpenEndedDeviceMapping(String leaderboardName, DeviceMappingDTO mappingDTO, Date closingTimePoint)
            throws TransformationException, DoesNotHaveRegattaLogException, UnableToCloseDeviceMappingException,
            NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        closeOpenEndedDeviceMapping(regattaLog, mappingDTO, closingTimePoint);
    }

    @Override
    public void updateFixedMarkPassing(String leaderboardName, String raceColumnName, String fleetName, Integer indexOfWaypoint,
            Date dateOfMarkPassing, CompetitorDTO competitorDTO) throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        final RaceLog raceLog = getService().getRaceLog(leaderboardName, raceColumnName, fleetName);
        final Competitor competitor = getCompetitor(competitorDTO);
        RaceLogFixedMarkPassingEvent oldFixedMarkPassingEvent = null;
        raceLog.lockForRead();
        try {
            for (RaceLogEvent event : raceLog.getUnrevokedEvents()) {
                if (event instanceof RaceLogFixedMarkPassingEventImpl && event.getInvolvedCompetitors().contains(competitor)) {
                    RaceLogFixedMarkPassingEvent fixedEvent = (RaceLogFixedMarkPassingEvent) event;
                    if (Util.equalsWithNull(fixedEvent.getZeroBasedIndexOfPassedWaypoint(), indexOfWaypoint)) {
                        oldFixedMarkPassingEvent = fixedEvent;
                    }
                }
            }
        } finally {
            raceLog.unlockAfterRead();
        }
        if (oldFixedMarkPassingEvent != null) {
            try {
                raceLog.revokeEvent(getService().getServerAuthor(), oldFixedMarkPassingEvent);
            } catch (NotRevokableException e) {
                e.printStackTrace();
            }
        }
        if (dateOfMarkPassing != null) {
            raceLog.add(new RaceLogFixedMarkPassingEventImpl(MillisecondsTimePoint.now(), getService()
                    .getServerAuthor(), competitor, raceLog.getCurrentPassId(),
                    new MillisecondsTimePoint(dateOfMarkPassing), indexOfWaypoint));
        }
    }

    @Override
    public void updateSuppressedMarkPassings(String leaderboardName, String raceColumnName, String fleetName,
            Integer newZeroBasedIndexOfSuppressedMarkPassing, CompetitorDTO competitorDTO) throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        RaceLogSuppressedMarkPassingsEvent oldSuppressedMarkPassingEvent = null;
        final RaceLog raceLog = getService().getRaceLog(leaderboardName, raceColumnName, fleetName);
        final Competitor competitor = getCompetitor(competitorDTO);
        raceLog.lockForRead();
        try {
            final NavigableSet<RaceLogEvent> unrevokedEvents = raceLog.getUnrevokedEvents();
            for (RaceLogEvent event : unrevokedEvents) {
                if (event instanceof RaceLogSuppressedMarkPassingsEvent && event.getInvolvedCompetitors().contains(competitor)) {
                    oldSuppressedMarkPassingEvent = (RaceLogSuppressedMarkPassingsEvent) event;
                    break;
                }
            }
        } finally {
            raceLog.unlockAfterRead();
        }
        final boolean create;
        final boolean revoke;
        if (oldSuppressedMarkPassingEvent == null) {
            if (newZeroBasedIndexOfSuppressedMarkPassing == null) {
                create = false;
                revoke = false;
            } else {
                create = true;
                revoke = false;
            }
        } else {
            if (newZeroBasedIndexOfSuppressedMarkPassing == null) {
                revoke = true;
                create = false;
            } else {
                boolean equal = Util.equalsWithNull(newZeroBasedIndexOfSuppressedMarkPassing,
                                oldSuppressedMarkPassingEvent.getZeroBasedIndexOfFirstSuppressedWaypoint());
                create = !equal;
                revoke = !equal;
            }
        }
        if (revoke) {
            try {
                raceLog.revokeEvent(getService().getServerAuthor(), oldSuppressedMarkPassingEvent);
            } catch (NotRevokableException e) {
                logger.log(Level.SEVERE, "Unable to revoke event "+oldSuppressedMarkPassingEvent, e);
            }
        }
        if (create) {
            raceLog.add(new RaceLogSuppressedMarkPassingsEventImpl(MillisecondsTimePoint.now(), getService()
                    .getServerAuthor(), competitor, raceLog.getCurrentPassId(),
                    newZeroBasedIndexOfSuppressedMarkPassing));
        }
    }

    private FileStorageService getFileStorageService(String name) {
        if (name == null || name.equals("")) {
            return null;
        }
        return getService().getFileStorageManagementService().getFileStorageService(name);
    }

    @Override
    public FileStorageServiceDTO[] getAvailableFileStorageServices(String localeInfoName) {
        List<FileStorageServiceDTO> serviceDtos = new ArrayList<>();
        final FileStorageManagementService fileStorageManagementService = getService()
                .getFileStorageManagementService();
        if (fileStorageManagementService != null) {
            for (FileStorageService s : fileStorageManagementService.getAvailableFileStorageServices()) {
                serviceDtos.add(FileStorageServiceDTOUtils.convert(s, getLocale(localeInfoName)));
            }
        }
        return serviceDtos.toArray(new FileStorageServiceDTO[0]);
    }

    @Override
    public void setFileStorageServiceProperties(String serviceName, Map<String, String> properties) {
        getSecurityService().checkCurrentUserUpdatePermission(getServerInfo());
        for (Entry<String, String> p : properties.entrySet()) {
            try {
                getService().getFileStorageManagementService()
                        .setFileStorageServiceProperty(getFileStorageService(serviceName), p.getKey(), p.getValue());
            } catch (NoCorrespondingServiceRegisteredException | IllegalArgumentException e) {
                // ignore, doing refresh afterwards anyways
            }
        }
    }

    @Override
    public FileStorageServicePropertyErrorsDTO testFileStorageServiceProperties(String serviceName,
            String localeInfoName) throws IOException {
        try {
            if (serviceName == null) {
                serviceName = getActiveFileStorageServiceName();
            }
            FileStorageService service = getFileStorageService(serviceName);
            if (service != null) {
                service.testProperties();
            }
        } catch (InvalidPropertiesException e) {
            return FileStorageServiceDTOUtils.convert(e, getLocale(localeInfoName));
        }
        return null;
    }

    @Override
    public void setActiveFileStorageService(String serviceName, String localeInfoName) {
        getSecurityService().checkCurrentUserUpdatePermission(getServerInfo());
        getService().getFileStorageManagementService().setActiveFileStorageService(getFileStorageService(serviceName));
    }

    @Override
    public String getActiveFileStorageServiceName() {
        try {
            final FileStorageService activeFileStorageService = getService().getFileStorageManagementService()
                    .getActiveFileStorageService();
            return activeFileStorageService == null ? null : activeFileStorageService.getName();
        } catch (NoCorrespondingServiceRegisteredException e) {
            return null;
        }
    }

    @Override
    public void inviteCompetitorsForTrackingViaEmail(String serverUrlWithoutTrailingSlash, EventDTO eventDto,
            String leaderboardName, Collection<CompetitorDTO> competitorDtos, String iOSAppUrl, String androidAppUrl,
            String localeInfoName) throws MailException {
        Event event = getService().getEvent(eventDto.id);
        getSecurityService().checkCurrentUserReadPermission(event);
        Set<Competitor> competitors = new HashSet<>();
        for (CompetitorDTO c : competitorDtos) {
            competitors.add(getCompetitor(c));
        }
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        Regatta regatta = getService().getRegattaByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(regatta);
        getRaceLogTrackingAdapter().inviteCompetitorsForTrackingViaEmail(event, leaderboard, regatta,
                serverUrlWithoutTrailingSlash, competitors, iOSAppUrl, androidAppUrl, getLocale(localeInfoName),
                getMailType());
    }

    @Override
    public void inviteBuoyTenderViaEmail(String serverUrlWithoutTrailingSlash, EventDTO eventDto,
            String leaderboardName, String emails, String iOSAppUrl, String androidAppUrl, String localeInfoName)
            throws MailException {
        Event event = getService().getEvent(eventDto.id);
        getSecurityService().checkCurrentUserReadPermission(event);
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserReadPermission(leaderboard);
        Regatta regatta = getService().getRegattaByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(regatta);
        MailInvitationType type = MailInvitationType
                .valueOf(System.getProperty(MAILTYPE_PROPERTY, MailInvitationType.SailInsight1.name()));
        getRaceLogTrackingAdapter().inviteBuoyTenderViaEmail(event, leaderboard, regatta, serverUrlWithoutTrailingSlash,
                emails, iOSAppUrl, androidAppUrl, getLocale(localeInfoName), type);
    }

    @Override
    public void disableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName)
            throws NotRevokableException, NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        if (areCompetitorRegistrationsEnabledForRace(leaderboardName, raceColumnName, fleetName)) {
            RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
            raceColumn.disableCompetitorRegistrationOnRaceLog(getFleetByName(raceColumn, fleetName));
        }
    }

    @Override
    public void enableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName)
            throws IllegalArgumentException, NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        if (!areCompetitorRegistrationsEnabledForRace(leaderboardName, raceColumnName, fleetName)) {
            RaceColumn raceColumn = getRaceColumn(leaderboardName, raceColumnName);
            raceColumn.enableCompetitorRegistrationOnRaceLog(getFleetByName(raceColumn, fleetName));
        }
    }

    @Override
    public void removeMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO fix) throws NotRevokableException {
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        if (leaderboard != null) {
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final TimePoint fixTimePoint = new MillisecondsTimePoint(fix.timepoint);
                final RegattaLog regattaLog = raceColumn.getRegattaLog();
                final BaseRegattaLogDeviceMappingFinder<Mark> mappingFinder = new RegattaLogDeviceMarkMappingFinder(
                        regattaLog);
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    for (final Mark mark : raceColumn.getAvailableMarks(fleet)) {
                        if (mark.getId().toString().equals(markIdAsString)) {
                            mappingFinder.removeTimePointFromMapping(mark, fixTimePoint);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO newFix) {
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final RegattaLog regattaLog = raceColumn.getRegattaLog();
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    for (final Mark mark : raceColumn.getAvailableMarks(fleet)) {
                        if (mark.getId().toString().equals(markIdAsString)) {
                            getRaceLogTrackingAdapter().pingMark(regattaLog, mark,
                                    new GPSFixImpl(newFix.position, new MillisecondsTimePoint(newFix.timepoint)),
                                    getService());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void editMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO oldFix, Position newPosition) throws NotRevokableException {
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        if (leaderboard != null) {
            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
            if (raceColumn != null) {
                final RegattaLog regattaLog = raceColumn.getRegattaLog();
                final BaseRegattaLogDeviceMappingFinder<Mark> mappingFinder = new RegattaLogDeviceMarkMappingFinder(
                        regattaLog);
                final Fleet fleet = raceColumn.getFleetByName(fleetName);
                if (fleet != null) {
                    final TimePoint fixTimePoint = new MillisecondsTimePoint(oldFix.timepoint);
                    for (final Mark mark : raceColumn.getAvailableMarks(fleet)) {
                        if (mark.getId().toString().equals(markIdAsString)) {
                            mappingFinder.removeTimePointFromMapping(mark, fixTimePoint);
                            getRaceLogTrackingAdapter().pingMark(regattaLog, mark,
                                    new GPSFixImpl(newPosition, fixTimePoint), getService());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setEliminatedCompetitors(String leaderboardName, Set<CompetitorDTO> newEliminatedCompetitorDTOs)
            throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(leaderboardName));
        Set<Competitor> newEliminatedCompetitors = new HashSet<>();
        for (final CompetitorDTO cDTO : newEliminatedCompetitorDTOs) {
            final Competitor competitor = getCompetitor(cDTO);
            newEliminatedCompetitors.add(competitor);
        }
        getService().apply(new UpdateEliminatedCompetitorsInLeaderboard(leaderboardName, newEliminatedCompetitors));
    }

    @Override
    public void addOrReplaceExpeditionDeviceConfiguration(ExpeditionDeviceConfiguration deviceConfiguration) throws IllegalStateException {
        getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.EXPEDITION_DEVICE_CONFIGURATION, deviceConfiguration.getTypeRelativeObjectIdentifier(),
                /* display name */ deviceConfiguration.getName(), () -> {
                    getService()
                            .apply(new AddOrReplaceExpeditionDeviceConfiguration(deviceConfiguration.getDeviceUuid(),
                                    deviceConfiguration.getName(), deviceConfiguration.getExpeditionBoatId()));
                });
    }

    @Override
    public void removeExpeditionDeviceConfiguration(ExpeditionDeviceConfiguration deviceConfiguration) {
        getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(deviceConfiguration.getIdentifier(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                getService().apply(new RemoveExpeditionDeviceConfiguration(deviceConfiguration.getDeviceUuid()));
                return null;
            }
        });
    }

    @Override
    public RegattaAndRaceIdentifier sliceRace(RegattaAndRaceIdentifier raceIdentifier, String newRaceColumnName,
            TimePoint sliceFrom, TimePoint sliceTo) throws ServiceException {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.REGATTA.getStringPermissionForTypeRelativeIdentifier(DefaultActions.UPDATE,
                        Regatta.getTypeRelativeObjectIdentifier(raceIdentifier.getRegattaName())));
        SecurityUtils.getSubject()
                .checkPermission(SecuredDomainType.LEADERBOARD.getStringPermissionForTypeRelativeIdentifier(
                        DefaultActions.UPDATE,
                        Leaderboard.getTypeRelativeObjectIdentifier(raceIdentifier.getRegattaName())));
        final Locale locale = getClientLocale();
        if (!canSliceRace(raceIdentifier)) {
            throw new ServiceException(serverStringMessages.get(locale, "slicingCannotSliceRace"));
        }
        final String trackedRaceName = newRaceColumnName;
        final RegattaIdentifier regattaIdentifier = new RegattaName(raceIdentifier.getRegattaName());
        final Regatta regatta = getService().getRegatta(regattaIdentifier);
        final Leaderboard regattaLeaderboard = getService().getLeaderboardByName(regatta.getName());
        if (regattaLeaderboard == null) {
            throw new IllegalArgumentException("Cannot slice a race for which no regatta leaderboard exists");
        }
        if (regatta.getRaceColumnByName(newRaceColumnName) != null) {
            throw new ServiceException(serverStringMessages.get(locale, "slicingRaceColumnAlreadyUsedThe"));
        }
        final DynamicTrackedRace trackedRaceToSlice = getService().getTrackedRace(raceIdentifier);
        // If tracked race isn't found, a NullPointerException will be thrown next, and that's okay because
        // it's a bit unusual to not find a race that is just about to be sliced. We couldn't continue anyway.
        final TimePoint startOfTrackingOfRaceToSlice = trackedRaceToSlice.getStartOfTracking();
        final TimePoint endOfTrackingOfRaceToSlice = trackedRaceToSlice.getEndOfTracking();
        if (sliceFrom == null || sliceTo == null || startOfTrackingOfRaceToSlice.after(sliceFrom)
                || (endOfTrackingOfRaceToSlice != null && endOfTrackingOfRaceToSlice.before(sliceTo))) {
            throw new ServiceException(serverStringMessages.get(locale, "slicingTimeRangeOutOfBounds"));
        }
        final Pair<RaceColumn, Fleet> raceColumnAndFleetOfRaceToSlice = regatta.getRaceColumnAndFleet(trackedRaceToSlice);
        // RaceColumns in a RegattaLeaderboard are always RaceColumnInSeries instances
        final RaceColumnInSeries raceColumnOfRaceToSlice = (RaceColumnInSeries) raceColumnAndFleetOfRaceToSlice.getA();
        final Fleet fleet = raceColumnAndFleetOfRaceToSlice.getB();
        final Series series = raceColumnOfRaceToSlice.getSeries();
        final RaceLog raceLogOfRaceToSlice = raceColumnOfRaceToSlice.getRaceLog(fleet);
        final RaceColumn raceColumn = getService().apply(new AddColumnToSeries(regattaIdentifier, series.getName(), newRaceColumnName));
        final RaceLog raceLog = raceColumn.getRaceLog(fleet);
        final AbstractLogEventAuthor author = getService().getServerAuthor();
        final TimePoint startOfTracking = sliceFrom;
        final TimePoint endOfTracking = sliceTo;
        raceLog.add(new RaceLogStartOfTrackingEventImpl(startOfTracking, author, raceLog.getCurrentPassId()));
        raceLog.add(new RaceLogEndOfTrackingEventImpl(endOfTracking, author, raceLog.getCurrentPassId()));
        final TimeRange timeRange = new TimeRangeImpl(sliceFrom, sliceTo);
        final StartTimeFinderResult startTimeFinderResult = new StartTimeFinder(getService(), raceLogOfRaceToSlice).analyze();
        final TimePoint startTime = startTimeFinderResult.getStartTime();
        final boolean hasStartTime = startTime != null && timeRange.includes(startTime);
        final boolean dependentStartTime = startTimeFinderResult.isDependentStartTime();
        final boolean hasFinishingTime;
        final boolean hasFinishedTime;
        if (hasStartTime) {
            final TimePoint finishingTime = new FinishingTimeFinder(raceLog).analyze();
            hasFinishingTime = finishingTime != null && timeRange.includes(finishingTime);
            if (hasFinishingTime) {
                final TimePoint finishedTime = new FinishedTimeFinder(raceLog).analyze();
                hasFinishedTime = finishedTime != null && timeRange.includes(finishedTime);
            } else {
                hasFinishedTime = false;
            }
        } else {
            hasFinishingTime = false;
            hasFinishedTime = false;
        }
        // Only wind fixes in the new tracking interval as well as the best fallback fixes are added to the new RaceLog
        final LogEventTimeRangeWithFallbackFilter<RaceLogWindFixEvent> windFixEvents = new LogEventTimeRangeWithFallbackFilter<>(
                timeRange);
        raceLogOfRaceToSlice.lockForRead();
        try {
            for (RaceLogEvent raceLogEvent : raceLogOfRaceToSlice.getUnrevokedEvents()) {
                raceLogEvent.accept(new BaseRaceLogEventVisitor() {
                    @Override
                    public void visit(RaceLogDependentStartTimeEvent event) {
                        if (dependentStartTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogDependentStartTimeEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    raceLog.getCurrentPassId(), event.getDependentOnRaceIdentifier(),
                                    event.getStartTimeDifference(), event.getNextStatus(), event.getCourseAreaId()));
                        }
                    }

                    @Override
                    public void visit(RaceLogStartTimeEvent event) {
                        if (!dependentStartTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogStartTimeEventImpl(event.getCreatedAt(), event.getLogicalTimePoint(),
                                    event.getAuthor(), UUID.randomUUID(), raceLog.getCurrentPassId(), event.getStartTime(),
                                    event.getNextStatus(), event.getCourseAreaId()));
                        }
                    }

                    @Override
                    public void visit(RaceLogRegisterCompetitorEvent event) {
                        raceLog.add(new RaceLogRegisterCompetitorEventImpl(event.getCreatedAt(),
                                event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                raceLog.getCurrentPassId(), event.getCompetitor(), event.getBoat()));
                    }

                    @Override
                    public void visit(RaceLogWindFixEvent event) {
                        windFixEvents.addEvent(event);
                    }

                    @Override
                    public void visit(RaceLogUseCompetitorsFromRaceLogEvent event) {
                        raceLog.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(event.getCreatedAt(), event.getAuthor(),
                                event.getLogicalTimePoint(), UUID.randomUUID(), raceLog.getCurrentPassId()));
                    }

                    @Override
                    public void visit(RaceLogCourseDesignChangedEvent event) {
                        raceLog.add(new RaceLogCourseDesignChangedEventImpl(event.getCreatedAt(),
                                event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                raceLog.getCurrentPassId(), event.getCourseDesign(), event.getCourseDesignerMode()));
                    }

                    @Override
                    public void visit(RaceLogFlagEvent event) {
                        if (hasStartTime && isLatestPass(event) && !event.getLogicalTimePoint().after(sliceTo)) {
                            raceLog.add(new RaceLogFlagEventImpl(event.getCreatedAt(), event.getLogicalTimePoint(),
                                    event.getAuthor(), UUID.randomUUID(), raceLog.getCurrentPassId(), event.getUpperFlag(),
                                    event.getLowerFlag(), event.isDisplayed()));
                        }
                    }

                    @Override
                    public void visit(RaceLogStartProcedureChangedEvent event) {
                        if (hasStartTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogStartProcedureChangedEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    raceLog.getCurrentPassId(), event.getStartProcedureType()));
                        }
                    }

                    @Override
                    public void visit(RaceLogFinishPositioningConfirmedEvent event) {
                        if (hasFinishedTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogFinishPositioningConfirmedEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    raceLog.getCurrentPassId(), event.getPositionedCompetitorsIDsNamesMaxPointsReasons()));
                        }
                    }

                    @Override
                    public void visit(RaceLogFinishPositioningListChangedEvent event) {
                        if (hasFinishedTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogFinishPositioningListChangedEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    raceLog.getCurrentPassId(), event.getPositionedCompetitorsIDsNamesMaxPointsReasons()));
                        }
                    }

                    @Override
                    public void visit(RaceLogFixedMarkPassingEvent event) {
                        if (hasStartTime && isLatestPass(event) && timeRange.includes(event.getTimePointOfFixedPassing())) {
                            raceLog.add(new RaceLogFixedMarkPassingEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    event.getInvolvedCompetitors(), raceLog.getCurrentPassId(),
                                    event.getTimePointOfFixedPassing(), event.getZeroBasedIndexOfPassedWaypoint()));
                        }
                    }

                    @Override
                    public void visit(RaceLogSuppressedMarkPassingsEvent event) {
                        if (hasStartTime && isLatestPass(event) && timeRange.includes(event.getLogicalTimePoint())) {
                            raceLog.add(new RaceLogSuppressedMarkPassingsEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    event.getInvolvedCompetitors(), raceLog.getCurrentPassId(),
                                    event.getZeroBasedIndexOfFirstSuppressedWaypoint()));
                        }
                    }

                    @Override
                    public void visit(RaceLogProtestStartTimeEvent event) {
                        if (hasFinishedTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogProtestStartTimeEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    raceLog.getCurrentPassId(), event.getProtestTime()));
                        }
                    }

                    @Override
                    public void visit(RaceLogAdditionalScoringInformationEvent event) {
                        if (hasFinishedTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogAdditionalScoringInformationEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    raceLog.getCurrentPassId(), event.getType()));
                        }
                    }

                    @Override
                    public void visit(RaceLogPathfinderEvent event) {
                        if (hasStartTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogPathfinderEventImpl(event.getCreatedAt(), event.getLogicalTimePoint(),
                                    event.getAuthor(), UUID.randomUUID(), raceLog.getCurrentPassId(),
                                    event.getPathfinderId()));
                        }
                    }

                    @Override
                    public void visit(RaceLogGateLineOpeningTimeEvent event) {
                        if (hasStartTime && isLatestPass(event)) {
                            raceLog.add(new RaceLogGateLineOpeningTimeEventImpl(event.getCreatedAt(),
                                    event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                    raceLog.getCurrentPassId(), event.getGateLineOpeningTimes().getGateLaunchStopTime(),
                                    event.getGateLineOpeningTimes().getGolfDownTime()));
                        }
                    }

                    @Override
                    public void visit(RaceLogRaceStatusEvent event) {
                        if (isLatestPass(event) && !(event instanceof RaceLogDependentStartTimeEvent)
                                && !(event instanceof RaceLogStartTimeEvent)) {
                            if ((hasStartTime
                                    && event.getNextStatus().getOrderNumber() <= RaceLogRaceStatus.RUNNING.getOrderNumber())
                                    || (hasFinishingTime && event.getNextStatus() == RaceLogRaceStatus.FINISHING)
                                    || (hasFinishedTime && event.getNextStatus() == RaceLogRaceStatus.FINISHED)) {
                                raceLog.add(new RaceLogRaceStatusEventImpl(event.getCreatedAt(), event.getLogicalTimePoint(),
                                        event.getAuthor(), UUID.randomUUID(), raceLog.getCurrentPassId(),
                                        event.getNextStatus()));
                            }
                        }
                    }

                    @Override
                    public void visit(RaceLogORCImpliedWindSourceEvent event) {
                        raceLog.add(new RaceLogORCImpliedWindSourceEventImpl(event.getCreatedAt(),
                                event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                raceLog.getCurrentPassId(), event.getImpliedWindSource()));
                    }

                    @Override
                    public void visit(RaceLogORCCertificateAssignmentEvent event) {
                        raceLog.add(new RaceLogORCCertificateAssignmentEventImpl(event.getCreatedAt(),
                                event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                raceLog.getCurrentPassId(), event.getCertificate(), event.getBoatId()));
                    }

                    @Override
                    public void visit(RaceLogORCScratchBoatEvent event) {
                        raceLog.add(new RaceLogORCScratchBoatEventImpl(event.getCreatedAt(),
                                event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(),
                                raceLog.getCurrentPassId(), event.getCompetitor()));
                    }

                    private boolean isLatestPass(RaceLogEvent event) {
                        return event.getPassId() == raceLogOfRaceToSlice.getCurrentPassId();
                    }
                });
            }
        } finally {
            raceLogOfRaceToSlice.unlockAfterRead();
        }
        windFixEvents.getFilteredEvents()
                .forEach(event -> raceLog.add(new RaceLogWindFixEventImpl(event.getCreatedAt(),
                        event.getLogicalTimePoint(), event.getAuthor(), UUID.randomUUID(), raceLog.getCurrentPassId(),
                        event.getWindFix(), event.isMagnetic())));
        final TimePoint startTrackingTimePoint = MillisecondsTimePoint.now();
        // this ensures that the events consistently have different timepoints to ensure a consistent result of the state analysis
        // that's why we can't just call adapter.denoteRaceForRaceLogTracking
        final TimePoint denotationTimePoint = startTrackingTimePoint.minus(1);
        raceLog.add(new RaceLogDenoteForTrackingEventImpl(denotationTimePoint,
                author, raceLog.getCurrentPassId(), trackedRaceName, regatta.getBoatClass(), UUID.randomUUID()));
        raceLog.add(new RaceLogStartTrackingEventImpl(startTrackingTimePoint, author, raceLog.getCurrentPassId()));
        try {
            final RaceHandle raceHandle = getRaceLogTrackingAdapter().startTracking(getService(), regattaLeaderboard,
                    raceColumn, fleet, /* trackWind */ true, /* correctWindDirectionByMagneticDeclination */ true,
                    getService().getPermissionAwareRaceTrackingHandler());

            // wait for the RaceDefinition to be created
            raceHandle.getRace();

            final DynamicTrackedRace trackedRace = WaitForTrackedRaceUtil.waitForTrackedRace(raceColumn, fleet, 10);
            if (trackedRace == null) {
                throw new ServiceException(serverStringMessages.get(locale, "slicingCouldNotObtainRace"));
            }
            for (WindSource windSourceToCopy : trackedRaceToSlice.getWindSources()) {
                if (windSourceToCopy.canBeStored()) {
                    final WindTrack windTrackToCopyFrom = trackedRaceToSlice.getOrCreateWindTrack(windSourceToCopy);
                    windTrackToCopyFrom.lockForRead();
                    try {
                        for (Wind windToCopy : windTrackToCopyFrom.getFixes(startOfTracking, true, endOfTracking, true)) {
                            trackedRace.recordWind(windToCopy, windSourceToCopy);
                        }
                    } finally {
                        windTrackToCopyFrom.unlockAfterRead();
                    }
                }
            }

            final Iterable<MediaTrack> mediaTracksForOriginalRace = getService().getMediaTracksForRace(raceIdentifier);
            for (MediaTrack mediaTrack : mediaTracksForOriginalRace) {
                if (mediaTrack.overlapsWith(sliceFrom, sliceTo)) {
                    final Set<RegattaAndRaceIdentifier> assignedRaces = new HashSet<>(mediaTrack.assignedRaces);
                    assignedRaces.add(trackedRace.getRaceIdentifier());
                    // we can't just use the original instance and add the Race due to the fact that this leads to
                    // assignedRaces being empty afterwards.
                    final MediaTrack mediaTrackToSave = new MediaTrack(mediaTrack.dbId, mediaTrack.title,
                            mediaTrack.url, mediaTrack.startTime, mediaTrack.duration, mediaTrack.mimeType,
                            assignedRaces);
                    getService().mediaTrackAssignedRacesChanged(mediaTrackToSave);
                }
            }
            return trackedRace.getRaceIdentifier();
        } catch (Exception e) {
            throw new ServiceException(serverStringMessages.get(locale, "slicingError"));
        }
    }


    @Override
    public Set<ImageDTO> resizeImage(final ImageResizingTaskDTO resizingTask) throws Exception {
        if (resizingTask.getResizingTask() == null || resizingTask.getResizingTask().size() == 0) {
            throw new InvalidAttributeValueException("Resizing Task can not be null or empty");
        }
        final ImageConverter converter = new ImageConverter();
        // calculating the fileType of the image by its uri
        final String sourceRef = resizingTask.getImage().getSourceRef();
        final String fileType = sourceRef.substring(sourceRef.lastIndexOf(".") + 1);
        final ImageWithMetadata imageAndMetadata = converter
                .loadImage(HttpUrlConnectionHelper.redirectConnection(new URL(sourceRef)).getInputStream(), fileType);
        final List<BufferedImage> resizedImages = converter.convertImage(imageAndMetadata.getImage(),
                resizingTask.getResizingTask());
        final List<String> sourceRefs = storeImages(resizedImages, fileType, imageAndMetadata.getMetadata());
        // if an error occures while storing the files, all already stored files are removed before throwing an
        // exception
        if (sourceRefs == null || sourceRefs.size() < resizingTask.getResizingTask().size()) {
            for (String alreadyStoredFileRef : sourceRefs) {
                try {
                    getService().getFileStorageManagementService().getActiveFileStorageService()
                            .removeFile(new URI(alreadyStoredFileRef));
                } catch (Exception e) {
                    logger.warning("Exception trying to remove image file "+alreadyStoredFileRef+": "+e.getMessage());
                }
                // Exception occured while trying to revert changes after exception
                // This only keeps some trash on the FileStorage
            }
            throw new Exception("Error occured while storing images on the FileStorage");
        }
        final Set<ImageDTO> resizedImagesAsDTOs = createImageDTOsFromURLsAndResizingTask(sourceRefs, resizingTask, resizedImages);
        final ImageDTO image = resizingTask.getImage();
        for (String tag : new ArrayList<>(image.getTags())) {
            final MediaTagConstants predefinedTag = MediaTagConstants.fromName(tag);
            if (predefinedTag != null && !resizingTask.getResizingTask().contains(predefinedTag)) {
                for (MediaTagConstants tagConstant : resizingTask.getResizingTask()) {
                    image.getTags().remove(tagConstant.getName());
                }
                resizedImagesAsDTOs.add(image);
            }
        }
        return resizedImagesAsDTOs;
    }

    /**
     * Stores a list of BufferedImages and returns a list of URLs as Strings under which the BufferedImages are stored
     *
     * @author Robin Fleige (D067799)
     *
     * @param resizedImages
     *            the BufferedImages that will be stored
     * @param fileType
     *            the format of the image, for example "png", "jpeg" or "jpg"
     * @param metadata
     *            the metadata of the original image
     * @returns a list of URLs as Strings under which the BufferedImages are stored
     */
    private List<String> storeImages(final List<BufferedImage> resizedImages, final String fileType,
            final IIOMetadata metadata) {
        final List<String> sourceRefs = new ArrayList<>();

        try {
            for (final BufferedImage resizedImage : resizedImages) {
                final InputStream fileStorageStream = new ImageConverter().imageWithMetadataToInputStream(resizedImage,
                        metadata, fileType);
                sourceRefs.add(getService().getFileStorageManagementService().getActiveFileStorageService()
                        .storeFile(fileStorageStream, "." + fileType, Long.valueOf(fileStorageStream.available()))
                        .toString());
            }
        } catch (NoCorrespondingServiceRegisteredException | IOException | OperationFailedException
                | InvalidPropertiesException e) {
            logger.log(Level.SEVERE, "Could not store file. Cause: " + e.getMessage());
        }
        return sourceRefs;
    }

    @Override
    public SuccessInfo addTag(String leaderboardName, String raceColumnName, String fleetName, String tag,
            String comment, String hiddenInfo, String imageURL, String resizedImageURL,
            boolean visibleForPublic, TimePoint raceTimepoint) {
        SuccessInfo successInfo = new SuccessInfo(true, null, null, null);
        try {
            if (visibleForPublic) {
                getSecurityService().checkCurrentUserUpdatePermission(getService().getLeaderboardByName(leaderboardName));
            }
            getService().getTaggingService().addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo,
                    imageURL, resizedImageURL, visibleForPublic, raceTimepoint);
        } catch (AuthorizationException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "missingAuthorization"),
                    null, null);
        } catch (IllegalArgumentException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "invalidParameters"), null,
                    null);
        } catch (RaceLogNotFoundException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "raceLogNotFound"), null,
                    null);
        } catch (ServiceNotFoundException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "securityServiceNotFound"),
                    null, null);
        } catch (TagAlreadyExistsException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "tagAlreadyExists"), null,
                    null);
        } catch (Exception e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "unknownError"), null,
                    null);
        }
        return successInfo;
    }

    @Override
    public SuccessInfo removeTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tag) {
        SuccessInfo successInfo = new SuccessInfo(true, null, null, null);
        try {
            getService().getTaggingService().removeTag(leaderboardName, raceColumnName, fleetName, tag);
        } catch (AuthorizationException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "missingAuthorization"),
                    null, null);
        } catch (IllegalArgumentException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "invalidParameters"), null,
                    null);
        } catch (NotRevokableException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "tagNotRevokable"), null,
                    null);
        } catch (RaceLogNotFoundException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "raceLogNotFound"), null,
                    null);
        } catch (Exception e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "unknownError"), null,
                    null);
        }
        return successInfo;
    }

    @Override
    public SuccessInfo updateTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tagToUpdate,
            String tag, String comment, String hiddenInfo, String imageURL, String resizedImageURL, boolean visibleForPublic) {
        SuccessInfo successInfo = new SuccessInfo(true, null, null, null);
        try {
            if (visibleForPublic) {
                getSecurityService().checkCurrentUserUpdatePermission(getService().getLeaderboardByName(leaderboardName));
            }
            getService().getTaggingService().updateTag(leaderboardName, raceColumnName, fleetName, tagToUpdate, tag,
                    comment, hiddenInfo, imageURL, resizedImageURL, visibleForPublic);
        } catch (AuthorizationException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "missingAuthorization"),
                    null, null);
        } catch (IllegalArgumentException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "invalidParameters"), null,
                    null);
        } catch (NotRevokableException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "tagNotRevokable"), null,
                    null);
        } catch (RaceLogNotFoundException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "raceLogNotFound"), null,
                    null);
        } catch (TagAlreadyExistsException e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "tagAlreadyExists"), null,
                    null);
        } catch (Exception e) {
            successInfo = new SuccessInfo(false, serverStringMessages.get(getClientLocale(), "unknownError"), null,
                    null);
        }
        return successInfo;
    }

    @Override
    public void updateGroupOwnerForEventHierarchy(UUID eventId,
            MigrateGroupOwnerForHierarchyDTO migrateGroupOwnerForHierarchyDTO) {
        Event event = getService().getEvent(eventId);
        if (event != null) {
            createOwnershipUpdater(migrateGroupOwnerForHierarchyDTO).updateGroupOwnershipForEventHierarchy(event);
        }
    }

    private SailingHierarchyOwnershipUpdater createOwnershipUpdater(
            MigrateGroupOwnerForHierarchyDTO migrateGroupOwnerForHierarchyDTO) {
        return SailingHierarchyOwnershipUpdater.createOwnershipUpdater(
                migrateGroupOwnerForHierarchyDTO.isCreateNewGroup(),
                migrateGroupOwnerForHierarchyDTO.getExistingUserGroupIdOrNull(),
                migrateGroupOwnerForHierarchyDTO.getGroupName(), migrateGroupOwnerForHierarchyDTO.isUpdateCompetitors(),
                migrateGroupOwnerForHierarchyDTO.isUpdateBoats(),
                migrateGroupOwnerForHierarchyDTO.isCopyMembersAndRoles(), getService());
    }

    @Override
    public void updateGroupOwnerForLeaderboardGroupHierarchy(UUID leaderboardGroupId,
            MigrateGroupOwnerForHierarchyDTO migrateGroupOwnerForHierarchyDTO) {
        LeaderboardGroup leaderboardGroup = getService().getLeaderboardGroupByID(leaderboardGroupId);
        if (leaderboardGroup != null) {
            createOwnershipUpdater(migrateGroupOwnerForHierarchyDTO)
                    .updateGroupOwnershipForLeaderboardGroupHierarchy(leaderboardGroup);
        }
    }

    @Override
    public String getSecretForRegattaByName(String regattaName) {
        String result = "";
        Regatta regatta = getService().getRegattaByName(regattaName);
        if (regatta != null) {
            getSecurityService().checkCurrentUserUpdatePermission(regatta);
            result = regatta.getRegistrationLinkSecret();
        }
        return result;
    }

    @Override
    public void setORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier raceIdentifier,
            Map<Integer, ORCPerformanceCurveLegImpl> legInfo) throws NotRevokableException {
        final TrackedRace existingTrackedRace = getExistingTrackedRace(raceIdentifier);
        getService().getSecurityService().checkCurrentUserUpdatePermission(existingTrackedRace);
        setORCPerformanceCurveInfo(existingTrackedRace.getAttachedRaceLogs().iterator().next(), legInfo);
    }

    private void setORCPerformanceCurveInfo(RaceLog raceLog, Map<Integer, ORCPerformanceCurveLegImpl> legInfo)
            throws NotRevokableException {
        final AbstractLogEventAuthor author = getService().getServerAuthor();
        for (final Entry<Integer, RaceLogORCLegDataEvent> e : new RaceLogORCLegDataEventFinder(raceLog).analyze()
                .entrySet()) {
            assert e.getValue() != null;
            final ORCPerformanceCurveLeg leg = RaceLogORCLegDataAnalyzer.createORCPerformanceCurveLeg(e.getValue());
            final ORCPerformanceCurveLegImpl desiredLeg = legInfo.get(e.getKey());
            final boolean explicitRevoke = legInfo.containsKey(e.getKey()) && legInfo.get(e.getKey()) == null;
            if (Util.equalsWithNull(desiredLeg, leg)) {
                assert desiredLeg != null;
                // we already have what the client wants, namely either nothing found and an explicitRevoke was
                // requested,
                // or the desiredLeg matched with what the log has; remove the request from the legInfo, meaning it has
                // been handled:
                legInfo.remove(e.getKey());
            } else {
                // either we have an explicitRevoke and found a leg setting in the race log that therefore needs to be
                // revoked,
                // or the leg event found in the log does not match the desiredLeg for this index.
                raceLog.revokeEvent(author, e.getValue());
                // In case an explicit revoke was what was requested, we have handled the request:
                if (explicitRevoke) {
                    legInfo.remove(e.getKey());
                }
            }
        }
        // for the remaining legInfo entries we now have to create race log events:
        final TimePoint now = MillisecondsTimePoint.now();
        for (final Entry<Integer, ORCPerformanceCurveLegImpl> e : legInfo.entrySet()) {
            if (e.getValue() != null) {
                raceLog.add(new RaceLogORCLegDataEventImpl(now, now, author, UUID.randomUUID(), /* pPassId */ 0,
                        e.getKey(), e.getValue().getTwa(), e.getValue().getLength(), e.getValue().getType()));
            }
        }
    }

    @Override
    public void setORCPerformanceCurveLegInfo(String leaderboardName, String raceColumnName, String fleetName,
            Map<Integer, ORCPerformanceCurveLegImpl> legInfo) throws NotFoundException, NotRevokableException {
        setORCPerformanceCurveInfo(getRaceLog(leaderboardName, raceColumnName, fleetName), legInfo);
    }

    private Triple<Integer, Integer, Integer> createCertificateAssignmentsForRaceLog(String leaderboardName,
            String raceColumnName, String fleetName, Map<String, ORCCertificate> certificatesForBoatIdsAsString)
            throws IOException, NotFoundException {
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final LogEventConstructor<RaceLogEvent, RaceLogEventVisitor> logEventConstructor = createRaceLogEventConstructor();
        return createCertificateAssignments(raceLog, logEventConstructor, certificatesForBoatIdsAsString);
    }

    private Triple<Integer, Integer, Integer> createCertificateAssignmentsForRegatta(
            RegattaIdentifier regattaIdentifier, Map<String, ORCCertificate> certificatesForBoatIdsAsString)
            throws IOException, NotFoundException {
        final Regatta regatta = (Regatta) regattaIdentifier.getRegatta(getService());
        if (regatta == null) {
            throw new NotFoundException("Regatta named " + regattaIdentifier + " not found");
        } else {
            getService().getSecurityService().checkCurrentUserUpdatePermission(regatta);
            final RegattaLog regattaLog = regatta.getRegattaLog();
            final LogEventConstructor<RegattaLogEvent, RegattaLogEventVisitor> logEventConstructor = createRegattaLogEventConstructor();
            return createCertificateAssignments(regattaLog, logEventConstructor, certificatesForBoatIdsAsString);
        }
    }

    private LogEventConstructor<RegattaLogEvent, RegattaLogEventVisitor> createRegattaLogEventConstructor() {
        final LogEventConstructor<RegattaLogEvent, RegattaLogEventVisitor> logEventConstructor = (TimePoint createdAt,
                TimePoint logicalTimePoint, AbstractLogEventAuthor author, Serializable pId, ORCCertificate certificate,
                Boat boat) -> new RegattaLogORCCertificateAssignmentEventImpl(createdAt, logicalTimePoint, author, pId,
                        certificate, boat);
        return logEventConstructor;
    }

    @Override
    public Triple<Integer, Integer, Integer> assignORCPerformanceCurveCertificates(RegattaIdentifier regattaIdentifier,
            Map<String, ORCCertificate> certificatesForBoatsWithIdAsString) throws IOException, NotFoundException {
        return createCertificateAssignmentsForRegatta(regattaIdentifier, certificatesForBoatsWithIdAsString);
    }

    @Override
    public Triple<Integer, Integer, Integer> assignORCPerformanceCurveCertificates(String leaderboardName,
            String raceColumnName, String fleetName, Map<String, ORCCertificate> certificatesForBoatsWithIdAsString)
            throws IOException, NotFoundException {
        return createCertificateAssignmentsForRaceLog(leaderboardName, raceColumnName, fleetName,
                certificatesForBoatsWithIdAsString);
    }

    /**
     * @return the number of certificate assignments inserted into (not replaced in) the log in the first component of
     *         the triple returned; this may be fewer than requested because redundant assignments are ignored; the
     *         number of assignments replaced in the log in the second component of the triple. The third element of the
     *         triple is the number of assignments effectively revoked (not including those replaced).
     */
    protected <LogT extends AbstractLog<LogEventT, VisitorT>, VisitorT, LogEventT extends AbstractLogEvent<VisitorT>, AssignmentEventT extends ORCCertificateAssignmentEvent<VisitorT>> Triple<Integer, Integer, Integer> createCertificateAssignments(
            LogT logToAddTo, LogEventConstructor<LogEventT, VisitorT> logEventConstructor,
            Map<String, ORCCertificate> certificatesForBoatIdsAsString) throws IOException {
        int insertedCount = 0;
        int replacedCount = 0;
        int removedCount = 0;
        final BaseORCCertificateAssignmentAnalyzer<LogT, VisitorT, LogEventT, AssignmentEventT> analyzerForPreviousAssignments = new BaseORCCertificateAssignmentAnalyzer<>(
                logToAddTo);
        final Map<Serializable, AssignmentEventT> validCertificateAssignmentsInLogByBoatId = analyzerForPreviousAssignments
                .analyze();
        final TimePoint now = MillisecondsTimePoint.now();
        final CompetitorAndBoatStore boatStore = getService().getCompetitorAndBoatStore();
        final AbstractLogEventAuthor serverAuthor = getService().getServerAuthor();
        for (final Entry<String, ORCCertificate> mapping : certificatesForBoatIdsAsString.entrySet()) {
            final Serializable boatId = UUIDHelper.tryUuidConversion(mapping.getKey());
            final AssignmentEventT previouslyValidAssignmentEventForBoat = validCertificateAssignmentsInLogByBoatId
                    .remove(boatId);
            final ORCCertificate certificate = mapping.getValue();
            final Boat boat = boatStore.getExistingBoatById(boatId);
            if (boat != null) {
                final boolean addEvent;
                if (previouslyValidAssignmentEventForBoat != null) {
                    if (!previouslyValidAssignmentEventForBoat.getCertificate().getId().equals(certificate.getId())) {
                        // revoke the previously valid event because it has a different certificate
                        logger.info("Replacing certificate " + previouslyValidAssignmentEventForBoat.getCertificate()
                                + " for boat " + boat + " by " + certificate);
                        replacedCount++;
                        addEvent = true;
                        try {
                            @SuppressWarnings("unchecked")
                            final LogEventT previouslyValidAssignmentEventForBoatAsLogEventT = (LogEventT) previouslyValidAssignmentEventForBoat;
                            logToAddTo.revokeEvent(serverAuthor, previouslyValidAssignmentEventForBoatAsLogEventT);
                        } catch (NotRevokableException e) {
                            logger.severe("Couldn't revoke old certificate assignment event "
                                    + previouslyValidAssignmentEventForBoat + ": " + e.getMessage());
                        }
                    } else {
                        addEvent = false;
                        logger.info("Not replacing certificate "
                                + previouslyValidAssignmentEventForBoat.getCertificate() + " because the certificate "
                                + certificate + " requested to be assigned is considered equal.");
                    }
                } else {
                    addEvent = true;
                    insertedCount++;
                }
                if (addEvent) {
                    final LogEventT assignment = logEventConstructor.create(now, now, serverAuthor, UUID.randomUUID(),
                            certificate, boat);
                    logToAddTo.add(assignment);
                }
            }
        }
        for (final Entry<Serializable, AssignmentEventT> boatIdAndEventToRevoke : validCertificateAssignmentsInLogByBoatId
                .entrySet()) {
            @SuppressWarnings("unchecked")
            final LogEventT eventToRevoke = (LogEventT) boatIdAndEventToRevoke.getValue();
            try {
                logToAddTo.revokeEvent(serverAuthor, eventToRevoke);
                removedCount++;
            } catch (NotRevokableException e) {
                logger.severe(
                        "Couldn't revoke old certificate assignment event " + eventToRevoke + ": " + e.getMessage());
            }
        }
        return new Triple<>(insertedCount, replacedCount, removedCount);
    }

    protected LogEventConstructor<RaceLogEvent, RaceLogEventVisitor> createRaceLogEventConstructor() {
        return (TimePoint createdAt, TimePoint logicalTimePoint, AbstractLogEventAuthor author, Serializable pId,
                ORCCertificate certificate, Boat boat) -> new RaceLogORCCertificateAssignmentEventImpl(createdAt,
                        logicalTimePoint, author, pId, /* passId */ 0, certificate, boat);
    }

    @Override
    public CompetitorDTO getORCPerformanceCurveScratchBoat(String leaderboardName, String raceColumnName,
            String fleetName) throws NotFoundException {
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserReadPermission(leaderboard);
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final RaceLogORCScratchBoatFinder finder = new RaceLogORCScratchBoatFinder(raceLog);
        final Competitor scratchBoat = finder.analyze();
        return scratchBoat == null ? null : convertToCompetitorDTO(scratchBoat);
    }

    @Override
    public void setORCPerformanceCurveScratchBoat(String leaderboardName, String raceColumnName, String fleetName,
            CompetitorDTO newScratchBoatDTO) throws NotFoundException {
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final Competitor newScratchBoat = newScratchBoatDTO == null ? null
                : getService().getCompetitorAndBoatStore()
                        .getExistingCompetitorById(UUIDHelper.tryUuidConversion(newScratchBoatDTO.getIdAsString()));
        final RaceLogORCScratchBoatAnalyzer analyzer = new RaceLogORCScratchBoatAnalyzer(raceLog);
        final Pair<Competitor, RaceLogORCScratchBoatEvent> previousScratchBoatAndEvent = analyzer.analyze();
        final Competitor previousScratchBoat = previousScratchBoatAndEvent == null ? null
                : previousScratchBoatAndEvent.getA();
        if (!Util.equalsWithNull(newScratchBoat, previousScratchBoat)) {
            final AbstractLogEventAuthor serverAuthor = getService().getServerAuthor();
            if (previousScratchBoatAndEvent != null) {
                // revoke scratch boat setting so far:
                try {
                    raceLog.revokeEvent(serverAuthor, previousScratchBoatAndEvent.getB());
                } catch (NotRevokableException e) {
                    logger.log(Level.SEVERE,
                            "Unable to revoke scratch boat definition event " + previousScratchBoatAndEvent.getB(), e);
                }
            }
            if (newScratchBoat != null) {
                final TimePoint now = MillisecondsTimePoint.now();
                raceLog.add(new RaceLogORCScratchBoatEventImpl(now, now, serverAuthor, UUID.randomUUID(),
                        /* passId */ raceLog.getCurrentPassId(), newScratchBoat));
            }
        }
    }

    @Override
    public MarkTemplateDTO addOrUpdateMarkTemplate(MarkTemplateDTO markTemplate) {
        final UUID markTemplateUUID = UUID.randomUUID();
        final MarkTemplate mTemplate = getSecurityService()
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.MARK_TEMPLATE,
                        MarkTemplate.getTypeRelativeObjectIdentifier(markTemplateUUID), markTemplate.getName(),
                        () -> getSharedSailingData().createMarkTemplate(
                                convertDtoToCommonMarkProperties(markTemplate.getCommonMarkProperties())));
        return convertToMarkTemplateDTO(mTemplate);
    }

    private boolean existsSwissTimingArchiveConfigurationForCurrentUser(String jsonUrl)
            throws Exception, UnauthorizedException {
        boolean found = false;
        final String currentUserName = getSecurityService().getCurrentUser().getName();
        for (final SwissTimingArchiveConfigurationWithSecurityDTO dto : getPreviousSwissTimingArchiveConfigurations()) {
            if (dto.getJsonUrl().equals(jsonUrl) && currentUserName.equals(dto.getCreatorName())) {
                found = true;
                break;
            }
        }
        return found;
    }

    private boolean existsYellowBrickConfigurationForCurrentUser(String raceUrl) {
        boolean found = false;
        final String currentUserName = getSecurityService().getCurrentUser().getName();
        for (final YellowBrickConfigurationWithSecurityDTO dto : getPreviousYellowBrickConfigurations()) {
            if (dto.getRaceUrl().equals(raceUrl) && currentUserName.equals(dto.getCreatorName())) {
                found = true;
                break;
            }
        }
        return found;
    }

    private boolean existsTracTracConfigurationForCurrentUser(String jsonUrl) throws Exception, UnauthorizedException {
        boolean found = false;
        final String currentUserName = getSecurityService().getCurrentUser().getName();
        for (final TracTracConfigurationWithSecurityDTO dto : getPreviousTracTracConfigurations()) {
            if (dto.getJsonUrl().equals(jsonUrl) && currentUserName.equals(dto.getCreatorName())) {
                found = true;
                break;
            }
        }
        return found;
    }

    public boolean existsSwissTimingConfigurationForCurrentUser(String jsonUrl)
            throws Exception, UnauthorizedException {
        boolean found = false;
        final String currentUserName = getSecurityService().getCurrentUser().getName();
        for (final SwissTimingConfigurationWithSecurityDTO dto : getPreviousSwissTimingConfigurations()) {
            if (dto.getJsonUrl().equals(jsonUrl) && currentUserName.equals(dto.getCreatorName())) {
                found = true;
                break;
            }
        }
        return found;
    }

    @Override
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

    private DeviceConfigurationImpl convertToDeviceConfiguration(DeviceConfigurationDTO dto) {
        DeviceConfigurationImpl configuration = new DeviceConfigurationImpl(convertToRegattaConfiguration(dto.regattaConfiguration), dto.id, dto.name);
        configuration.setAllowedCourseAreaNames(dto.allowedCourseAreaNames);
        configuration.setResultsMailRecipient(dto.resultsMailRecipient);
        configuration.setByNameDesignerCourseNames(dto.byNameDesignerCourseNames);
        configuration.setEventId(dto.eventId);
        configuration.setCourseAreaId(dto.courseAreaId);
        configuration.setPriority(dto.priority);
        return configuration;
    }


    @Override
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
    public boolean setStartTimeAndProcedure(RaceLogSetStartTimeAndProcedureDTO dto) throws NotFoundException {
        getSecurityService().checkCurrentUserUpdatePermission(getLeaderboardByName(dto.leaderboardName));
        TimePoint newStartTime = getService().setStartTimeAndProcedure(dto.leaderboardName, dto.raceColumnName,
                dto.fleetName, dto.authorName, dto.authorPriority,
                dto.passId, new MillisecondsTimePoint(dto.logicalTimePoint), new MillisecondsTimePoint(dto.startTime),
                dto.racingProcedure, dto.courseAreaId);
        return new MillisecondsTimePoint(dto.startTime).equals(newStartTime);
    }

    @Override
    public void setImpliedWindSource(String leaderboardName, String raceColumnName, String fleetName, ImpliedWindSource impliedWindSource) throws NotFoundException {
        final Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        getService().getSecurityService().checkCurrentUserUpdatePermission(leaderboard);
        final RaceLog raceLog = getRaceLog(leaderboardName, raceColumnName, fleetName);
        final TimePoint now = MillisecondsTimePoint.now();
        final RaceLogEvent impliedWindSourceEvent = new RaceLogORCImpliedWindSourceEventImpl(now, now, getService().getServerAuthor(), UUID.randomUUID(), raceLog.getCurrentPassId(), impliedWindSource);
        raceLog.add(impliedWindSourceEvent);
    }

    @Override
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

    private MarkTrackDTO getMarkTrack(Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet, String markIdAsString) {
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

    private List<DeviceMappingDTO> getDeviceMappings(RegattaLog regattaLog) throws TransformationException {
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
    public List<DeviceMappingDTO> getDeviceMappings(String leaderboardName)
            throws DoesNotHaveRegattaLogException, TransformationException, NotFoundException {
        getSecurityService().checkCurrentUserReadPermission(getLeaderboardByName(leaderboardName));
        RegattaLog regattaLog = getRegattaLogInternal(leaderboardName);
        return getDeviceMappings(regattaLog);
    }

    @Override
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
                            getSecurityService().putRoleDefinitionToUserGroup(serverGroup, viewerRole, /* forAll */ true);
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
    public void deleteYellowBrickConfigurations(Collection<YellowBrickConfigurationWithSecurityDTO> configs) {
        for (final YellowBrickConfigurationWithSecurityDTO config : configs) {
            getYellowBrickTrackingAdapter().removeYellowBrickConfiguration(config.getRaceUrl(), config.getCreatorName());
        }
    }

    @Override
    public void updateYellowBrickConfiguration(YellowBrickConfigurationWithSecurityDTO editedObject) {
        getYellowBrickTrackingAdapter().updateYellowBrickConfiguration(editedObject.getName(),
                editedObject.getRaceUrl(), editedObject.getUsername(), editedObject.getPassword(),
                editedObject.getCreatorName());
    }

    private void checkAIAgentConfigPermission() throws AuthorizationException {
        final Subject subject = SecurityUtils.getSubject();
        subject.checkPermission(SecuredSecurityTypes.SERVER.getStringPermissionForTypeRelativeIdentifier(ServerActions.CONFIGURE_AI_AGENT, new TypeRelativeObjectIdentifier(ServerInfo.getName())));
    }
    
    @Override
    public void startAICommentingOnEvent(UUID eventId) {
        changeAICommentingForEvent(eventId, getAIAgent()::startCommentingOnEvent);
    }
    
    private void changeAICommentingForEvent(UUID eventId, Consumer<Event> changeFunction) {
        checkAIAgentConfigPermission();
        final Event event = getService().getEvent(eventId);
        if (event != null) {
            getSecurityService().checkCurrentUserUpdatePermission(event);
            changeFunction.accept(event);
        } else {
            logger.warning("User "+SecurityUtils.getSubject().getPrincipal()+" was trying to change AI commenting for event with ID "+eventId+", but that event wasn't found");
        }
    }
    
    @Override
    public void stopAICommentingOnEvent(UUID eventId) {
        changeAICommentingForEvent(eventId, getAIAgent()::stopCommentingOnEvent);
    }
    
    /**
     * Event though this is a reading API method, we place it on {@link SailingServiceWrite} because it is available
     * and makes sense only on the primary/master process of a replica set. There is no replication of the {@link AIAgent}
     * itself; only its actions will be replicated. Therefore, AI agent configuration and introspection will work only
     * on the primary/master.
     */
    @Override
    public List<EventDTO> getIdsOfEventsWithAICommenting() {
        checkAIAgentConfigPermission();
        return getSecurityService().mapAndFilterByReadPermissionForCurrentUser(getAIAgent().getCommentingOnEvents(), event->convertToEventDTO(event, /* withStatisticalData */ false));
    }

    @Override
    public String getAIAgentLanguageModelName() {
        checkAIAgentConfigPermission();
        final AIAgent aiAgent = getAIAgent();
        return aiAgent == null ? null : aiAgent.getModelName();
    }

    @Override
    public boolean hasAIAgentCredentials() {
        checkAIAgentConfigPermission();
        final AIAgent aiAgent = getAIAgent();
        return aiAgent == null ? false : aiAgent.hasCredentials();
    }

    @Override
    public void setAIAgentCredentials(String credentials) throws MalformedURLException, org.json.simple.parser.ParseException {
        checkAIAgentConfigPermission();
        final AIAgent aiAgent = getAIAgent();
        if (aiAgent != null) {
            if (Util.hasLength(credentials)) {
                Credentials parsedCredentials;
                try {
                    parsedCredentials = CredentialsParser.create().parse(credentials);
                } catch (Exception e) {
                    throw new IllegalStateException("Credentials could not be parsed");
                }
                if (parsedCredentials != null) {
                    aiAgent.setCredentials(parsedCredentials);
                }
            } else {
                throw new IllegalStateException("Blank credentials received");
            }
        }
    }
    
    @Override
    public void resetAIAgentCredentials() {
        checkAIAgentConfigPermission();
        final AIAgent aiAgent = getAIAgent();
        if (aiAgent != null) {
            aiAgent.setCredentials(null);
        }
    }

    @Override
    public void copyPairingListFromOtherLeaderboard(String sourceLeaderboardName, String targetLeaderboardName,
            String fromRaceColumnName, String toRaceColumnInclusiveName)
            throws UnauthorizedException, NotFoundException {
        final Leaderboard sourceLeaderboard = getLeaderboardByName(sourceLeaderboardName);
        getService().getSecurityService().checkCurrentUserUpdatePermission(sourceLeaderboard);
        final Leaderboard targetLeaderboard = getLeaderboardByName(targetLeaderboardName);
        getService().getSecurityService().checkCurrentUserUpdatePermission(targetLeaderboard);
        if (!(sourceLeaderboard instanceof RegattaLeaderboard)) {
            throw new IllegalArgumentException("Source leaderboard " + sourceLeaderboardName
                    + " must be a regatta leaderboard, but was: " + sourceLeaderboard.getLeaderboardType());
        }
        if (!(targetLeaderboard instanceof RegattaLeaderboard)) {
            throw new IllegalArgumentException("Target leaderboard " + sourceLeaderboardName
                    + " must be a regatta leaderboard, but was: " + targetLeaderboard.getLeaderboardType());
        }
        // we don't need to worry about replication here because all operations carried out by
        // the following call will only manipulate race logs and regatta logs, and those have
        // their own listener-based replication scheme.
        getRaceLogTrackingAdapter().copyPairingListFromOtherLeaderboard((RegattaLeaderboard) sourceLeaderboard,
                (RegattaLeaderboard) targetLeaderboard, fromRaceColumnName, toRaceColumnInclusiveName);
    }
}
