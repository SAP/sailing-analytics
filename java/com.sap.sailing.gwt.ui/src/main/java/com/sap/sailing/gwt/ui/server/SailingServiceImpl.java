package com.sap.sailing.gwt.ui.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Gate;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.domain.common.CountryCode;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.NoWindError;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.PolarSheetGenerationTriggerResponse;
import com.sap.sailing.domain.common.PolarSheetsData;
import com.sap.sailing.domain.common.PolarSheetsHistogramData;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceFetcher;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaFetcher;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionForCompetitorInRace;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.PositionDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.RaceStatusDTO;
import com.sap.sailing.domain.common.dto.TrackedRaceDTO;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.PolarSheetGenerationTriggerResponseImpl;
import com.sap.sailing.domain.common.impl.PolarSheetsHistogramDataImpl;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.MetaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.caching.LiveLeaderboardUpdater;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.MongoWindStoreFactory;
import com.sap.sailing.domain.polarsheets.BoatAndWindSpeed;
import com.sap.sailing.domain.polarsheets.PolarSheetGenerationWorker;
import com.sap.sailing.domain.polarsheets.PolarSheetsWindStepping;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.domain.racelog.analyzing.impl.AbortingFlagFinder;
import com.sap.sailing.domain.racelog.analyzing.impl.GateLineOpeningTimeFinder;
import com.sap.sailing.domain.racelog.analyzing.impl.LastFlagFinder;
import com.sap.sailing.domain.racelog.analyzing.impl.LastPublishedCourseDesignFinder;
import com.sap.sailing.domain.racelog.analyzing.impl.PathfinderFinder;
import com.sap.sailing.domain.racelog.analyzing.impl.RaceStatusAnalyzer;
import com.sap.sailing.domain.racelog.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingArchiveConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.persistence.SwissTimingAdapterPersistence;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayRace;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayService;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayServiceFactory;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.MarkPassingManeuver;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.RaceRecord;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.gwt.ui.client.SailingService;
import com.sap.sailing.gwt.ui.shared.BulkScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.CompactRaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorsRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.CourseAreaDTO;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.GateDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.LegInfoDTO;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.MarkPassingTimesDTO;
import com.sap.sailing.gwt.ui.shared.MarkpassingManeuverDTO;
import com.sap.sailing.gwt.ui.shared.QuickRankDTO;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceWithCompetitorsDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.RegattaOverviewEntryDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO.ScoreCorrectionEntryDTO;
import com.sap.sailing.gwt.ui.shared.ReplicaDTO;
import com.sap.sailing.gwt.ui.shared.ReplicationMasterDTO;
import com.sap.sailing.gwt.ui.shared.ReplicationStateDTO;
import com.sap.sailing.gwt.ui.shared.ScoreCorrectionProviderDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sailing.gwt.ui.shared.SpeedWithBearingDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingArchiveConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingReplayRaceDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.resultimport.UrlResultProvider;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.AddCourseArea;
import com.sap.sailing.server.operationaltransformation.AddSpecificRegatta;
import com.sap.sailing.server.operationaltransformation.ConnectTrackedRaceToLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.CreateEvent;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.DisconnectLeaderboardColumnFromTrackedRace;
import com.sap.sailing.server.operationaltransformation.MoveColumnInSeriesDown;
import com.sap.sailing.server.operationaltransformation.MoveColumnInSeriesUp;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnDown;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnUp;
import com.sap.sailing.server.operationaltransformation.RemoveAndUntrackRace;
import com.sap.sailing.server.operationaltransformation.RemoveColumnFromSeries;
import com.sap.sailing.server.operationaltransformation.RemoveEvent;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sailing.server.operationaltransformation.RenameEvent;
import com.sap.sailing.server.operationaltransformation.RenameLeaderboard;
import com.sap.sailing.server.operationaltransformation.RenameLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.RenameLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.SetRaceIsKnownToStartUpwind;
import com.sap.sailing.server.operationaltransformation.SetSuppressedFlagForCompetitorInLeaderboard;
import com.sap.sailing.server.operationaltransformation.SetWindSourcesToExclude;
import com.sap.sailing.server.operationaltransformation.StopTrackingRace;
import com.sap.sailing.server.operationaltransformation.StopTrackingRegatta;
import com.sap.sailing.server.operationaltransformation.UpdateCompetitorDisplayNameInLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateEvent;
import com.sap.sailing.server.operationaltransformation.UpdateIsMedalRace;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardCarryValue;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardColumnFactor;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardMaxPointsReason;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrection;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardScoreCorrectionMetadata;
import com.sap.sailing.server.operationaltransformation.UpdateRaceDelayToLive;
import com.sap.sailing.server.operationaltransformation.UpdateSpecificRegatta;
import com.sap.sailing.server.replication.ReplicaDescriptor;
import com.sap.sailing.server.replication.ReplicationFactory;
import com.sap.sailing.server.replication.ReplicationMasterDescriptor;
import com.sap.sailing.server.replication.ReplicationService;

/**
 * The server side implementation of the RPC service.
 */
public class SailingServiceImpl extends ProxiedRemoteServiceServlet implements SailingService, RaceFetcher, RegattaFetcher {
    private static final Logger logger = Logger.getLogger(SailingServiceImpl.class.getName());

    private static final long serialVersionUID = 9031688830194537489L;

    private final ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;

    private final ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;

    private final ServiceTracker<ScoreCorrectionProvider, ScoreCorrectionProvider> scoreCorrectionProviderServiceTracker;

    private final MongoObjectFactory mongoObjectFactory;

    private final SwissTimingAdapterPersistence swissTimingAdapterPersistence;

    private final com.sap.sailing.domain.tractracadapter.persistence.MongoObjectFactory tractracMongoObjectFactory;

    private final DomainObjectFactory domainObjectFactory;

    private final SwissTimingFactory swissTimingFactory;

    private final com.sap.sailing.domain.tractracadapter.persistence.DomainObjectFactory tractracDomainObjectFactory;

    private final com.sap.sailing.domain.common.CountryCodeFactory countryCodeFactory;

    private final Executor executor;

    private final DomainFactory tractracDomainFactory;

    private final com.sap.sailing.domain.base.DomainFactory baseDomainFactory;

    private final Map<String,PolarSheetGenerationWorker> polarSheetGenerationWorkers = new HashMap<String, PolarSheetGenerationWorker>();

    public SailingServiceImpl() {
        BundleContext context = Activator.getDefault();
        tractracDomainFactory = DomainFactory.INSTANCE;
        baseDomainFactory = tractracDomainFactory.getBaseDomainFactory();
        racingEventServiceTracker = createAndOpenRacingEventServiceTracker(context);
        replicationServiceTracker = createAndOpenReplicationServiceTracker(context);
        scoreCorrectionProviderServiceTracker = createAndOpenScoreCorrectionProviderServiceTracker(context);
        mongoObjectFactory = MongoFactory.INSTANCE.getDefaultMongoObjectFactory();
        domainObjectFactory = MongoFactory.INSTANCE.getDefaultDomainObjectFactory();
        swissTimingAdapterPersistence = SwissTimingAdapterPersistence.INSTANCE;
        tractracDomainObjectFactory = com.sap.sailing.domain.tractracadapter.persistence.DomainObjectFactory.INSTANCE;
        tractracMongoObjectFactory = com.sap.sailing.domain.tractracadapter.persistence.MongoObjectFactory.INSTANCE;
        swissTimingFactory = SwissTimingFactory.INSTANCE;
        countryCodeFactory = com.sap.sailing.domain.common.CountryCodeFactory.INSTANCE;
        // When many updates are triggered in a short period of time by a single thread, ensure that the single thread
        // providing the updates is not outperformed by all the re-calculations happening here. Leave at least one
        // core to other things, but by using at least three threads ensure that no simplistic deadlocks may occur.
        final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);
        executor = new ThreadPoolExecutor(/* corePoolSize */ THREAD_POOL_SIZE,
                /* maximumPoolSize */ THREAD_POOL_SIZE,
                /* keepAliveTime */ 60, TimeUnit.SECONDS,
                /* workQueue */ new LinkedBlockingQueue<Runnable>());
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    protected ServiceTracker<RacingEventService, RacingEventService> createAndOpenRacingEventServiceTracker(
            BundleContext context) {
        ServiceTracker<RacingEventService, RacingEventService> result = new ServiceTracker<RacingEventService, RacingEventService>(
                context, RacingEventService.class.getName(), null);
        result.open();
        return result;
    }

    /**
     * Asks the OSGi system for registered score correction provider services
     */
    protected ServiceTracker<ScoreCorrectionProvider, ScoreCorrectionProvider> createAndOpenScoreCorrectionProviderServiceTracker(
            BundleContext bundleContext) {
        ServiceTracker<ScoreCorrectionProvider, ScoreCorrectionProvider> tracker = new ServiceTracker<ScoreCorrectionProvider, ScoreCorrectionProvider>(bundleContext,
                ScoreCorrectionProvider.class.getName(),
                /* customizer */null);
        tracker.open();
        return tracker;
    }

    @Override
    public Iterable<ScoreCorrectionProviderDTO> getScoreCorrectionProviderDTOs() throws Exception {
        List<ScoreCorrectionProviderDTO> result = new ArrayList<ScoreCorrectionProviderDTO>();
        final Iterable<ScoreCorrectionProvider> services = getScoreCorrectionProviders();
        for (ScoreCorrectionProvider scoreCorrectionProvider : services) {
            result.add(createScoreCorrectionProviderDTO((ScoreCorrectionProvider) scoreCorrectionProvider));
        }
        return result;
    }

    private Iterable<ScoreCorrectionProvider> getScoreCorrectionProviders() {
        final Object[] services = scoreCorrectionProviderServiceTracker.getServices();
        List<ScoreCorrectionProvider> result = new ArrayList<ScoreCorrectionProvider>();
        if (services != null) {
            for (Object service : services) {
                result.add((ScoreCorrectionProvider) service);
            }
        }
        return result;
    }

    private ScoreCorrectionProviderDTO createScoreCorrectionProviderDTO(ScoreCorrectionProvider scoreCorrectionProvider)
            throws Exception {
        Map<String, Set<Pair<String, Date>>> hasResultsForBoatClassFromDateByEventName = new HashMap<String, Set<Pair<String,Date>>>();
        for (Map.Entry<String, Set<Pair<String, TimePoint>>> e : scoreCorrectionProvider
                .getHasResultsForBoatClassFromDateByEventName().entrySet()) {
            Set<Pair<String, Date>> set = new HashSet<Pair<String, Date>>();
            for (Pair<String, TimePoint> p : e.getValue()) {
                set.add(new Pair<String, Date>(p.getA(), p.getB().asDate()));
            }
            hasResultsForBoatClassFromDateByEventName.put(e.getKey(), set);
        }
        return new ScoreCorrectionProviderDTO(scoreCorrectionProvider.getName(), hasResultsForBoatClassFromDateByEventName);
    }

    protected ServiceTracker<ReplicationService, ReplicationService> createAndOpenReplicationServiceTracker(
            BundleContext context) {
        ServiceTracker<ReplicationService, ReplicationService> result = new ServiceTracker<ReplicationService, ReplicationService>(
                context, ReplicationService.class.getName(), null);
        result.open();
        return result;
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
     */
    @Override
    public LeaderboardDTO getLeaderboardByName(final String leaderboardName, final Date date,
            final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails)
                    throws NoWindException, InterruptedException, ExecutionException {
        long startOfRequestHandling = System.currentTimeMillis();
        LeaderboardDTO result = null;
        final Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            TimePoint timePoint;
            if (date == null) {
                timePoint = null;
            } else {
                timePoint = new MillisecondsTimePoint(date);
            }
            result = leaderboard.getLeaderboardDTO(timePoint, namesOfRaceColumnsForWhichToLoadLegDetails, getService(), baseDomainFactory);
            logger.fine("getLeaderboardByName(" + leaderboardName + ", " + date + ", "
                    + namesOfRaceColumnsForWhichToLoadLegDetails + ") took "
                    + (System.currentTimeMillis() - startOfRequestHandling) + "ms");
        }
        return result;
    }

    @Override
    public List<RegattaDTO> getRegattas() throws IllegalArgumentException {
        List<RegattaDTO> result = new ArrayList<RegattaDTO>();
        for (Regatta regatta : getService().getAllRegattas()) {
            result.add(convertToRegattaDTO(regatta));
        }
        return result;
    }

    private MarkDTO convertToMarkDTO(Mark mark, Position position) {
        MarkDTO markDTO;
        if(position != null) {
            markDTO = new MarkDTO(mark.getId().toString(), mark.getName(), position.getLatDeg(), position.getLngDeg());
        } else {
            markDTO = new MarkDTO(mark.getId().toString(), mark.getName());
        }
        markDTO.color = mark.getColor();
        markDTO.shape = mark.getShape();
        markDTO.pattern = mark.getPattern();
        markDTO.type = mark.getType();
        return markDTO;
    }

    private RegattaDTO convertToRegattaDTO(Regatta regatta) {
        RegattaDTO regattaDTO = new RegattaDTO(regatta.getName(), regatta.getScoringScheme().getType());
        regattaDTO.races = convertToRaceDTOs(regatta);
        regattaDTO.series = convertToSeriesDTOs(regatta);
        BoatClass boatClass = regatta.getBoatClass();
        if (boatClass != null) {
            regattaDTO.boatClass = new BoatClassDTO(boatClass.getName(), boatClass.getHullLength().getMeters());
        }
        if (regatta.getDefaultCourseArea() != null) {
            regattaDTO.defaultCourseAreaIdAsString = regatta.getDefaultCourseArea().getId().toString();
            regattaDTO.defaultCourseAreaName = regatta.getDefaultCourseArea().getName();
        }
        return regattaDTO;
    }

    private List<SeriesDTO> convertToSeriesDTOs(Regatta regatta) {
        List<SeriesDTO> result = new ArrayList<SeriesDTO>();
        for (Series series : regatta.getSeries()) {
            SeriesDTO seriesDTO = convertToSeriesDTO(series);
            result.add(seriesDTO);
        }
        return result;
    }

    private SeriesDTO convertToSeriesDTO(Series series) {
        List<FleetDTO> fleets = new ArrayList<FleetDTO>();
        for (Fleet fleet : series.getFleets()) {
            fleets.add(baseDomainFactory.convertToFleetDTO(series, fleet));
        }
        List<RaceColumnDTO> raceColumns = new ArrayList<RaceColumnDTO>();
        for (RaceColumnInSeries raceColumn : series.getRaceColumns()) {
            RaceColumnDTO raceColumnDTO = new RaceColumnDTO(/* isValidInTotalScore not relevant here because no scores conveyed */ null);
            raceColumnDTO.name = raceColumn.getName();
            raceColumnDTO.setMedalRace(raceColumn.isMedalRace());
            raceColumnDTO.setExplicitFactor(raceColumn.getExplicitFactor());
            raceColumns.add(raceColumnDTO);
        }
        SeriesDTO result = new SeriesDTO(series.getName(), fleets, raceColumns, series.isMedal());
        return result;
    }

    private RaceInfoDTO createRaceInfoDTO(RaceColumn raceColumn, Fleet fleet) {
        RaceInfoDTO raceInfoDTO = new RaceInfoDTO();
        RaceLog raceLog = raceColumn.getRaceLog(fleet);
        if (raceLog != null) {
            
            StartTimeFinder startTimeFinder = new StartTimeFinder(raceLog);
            if(startTimeFinder.getStartTime()!=null){
                raceInfoDTO.startTime = startTimeFinder.getStartTime().asDate();
            }

            RaceStatusAnalyzer raceStatusAnalyzer = new RaceStatusAnalyzer(raceLog);
            raceInfoDTO.lastStatus = raceStatusAnalyzer.getStatus();

            PathfinderFinder pathfinderFinder = new PathfinderFinder(raceLog);
            raceInfoDTO.pathfinderId = pathfinderFinder.getPathfinderId();

            GateLineOpeningTimeFinder gateLineOpeningTimeFinder = new GateLineOpeningTimeFinder(raceLog);
            raceInfoDTO.gateLineOpeningTime = gateLineOpeningTimeFinder.getGateLineOpeningTime();

            LastFlagFinder lastFlagFinder = new LastFlagFinder(raceLog);

            RaceLogFlagEvent lastFlagEvent = lastFlagFinder.getLastFlagEvent();
            if (lastFlagEvent != null) {
                raceInfoDTO.lastUpperFlag = lastFlagEvent.getUpperFlag();
                raceInfoDTO.lastLowerFlag = lastFlagEvent.getLowerFlag();
                raceInfoDTO.isLastFlagDisplayed = lastFlagEvent.isDisplayed();
            }
            
            AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
            
            RaceLogFlagEvent abortingFlagEvent = abortingFlagFinder.getAbortingFlagEvent();
            if (abortingFlagEvent != null) {
                raceInfoDTO.isRaceAbortedInPassBefore = true;
                
                if (raceInfoDTO.lastStatus.equals(RaceLogRaceStatus.UNSCHEDULED)) {
                    raceInfoDTO.lastUpperFlag = abortingFlagEvent.getUpperFlag();
                    raceInfoDTO.lastLowerFlag = abortingFlagEvent.getLowerFlag();
                    raceInfoDTO.isLastFlagDisplayed = abortingFlagEvent.isDisplayed();
                }
            }
            
            LastPublishedCourseDesignFinder courseDesignFinder = new LastPublishedCourseDesignFinder(raceLog);
            raceInfoDTO.lastCourseDesign = convertCourseDesignToRaceCourseDTO(courseDesignFinder.getLastCourseDesign());
        }
        raceInfoDTO.raceName = raceColumn.getName();
        raceInfoDTO.fleetName = fleet.getName();
        raceInfoDTO.fleetOrdering = fleet.getOrdering();
        raceInfoDTO.raceIdentifier = raceColumn.getRaceIdentifier(fleet);
        return raceInfoDTO;
    }

    private RaceCourseDTO convertCourseDesignToRaceCourseDTO(CourseBase lastCourseDesign) {
        RaceCourseDTO result = new RaceCourseDTO(Collections.<WaypointDTO> emptyList());
        if (lastCourseDesign != null) {
            List<WaypointDTO> waypointDTOs = new ArrayList<WaypointDTO>();
            for (Waypoint waypoint : lastCourseDesign.getWaypoints()) {
                ControlPointDTO controlPointDTO = convertToControlPointDTO(waypoint.getControlPoint());
                List<MarkDTO> marks = new ArrayList<MarkDTO>();
                for (MarkDTO markDTO : controlPointDTO.getMarks()) {
                    marks.add(markDTO);
                }
                WaypointDTO waypointDTO = new WaypointDTO(waypoint.getName(), controlPointDTO, marks, waypoint.getPassingSide());
                waypointDTOs.add(waypointDTO);
            }
            result = new RaceCourseDTO(waypointDTOs);
        }
        return result;
    }

    private List<RaceWithCompetitorsDTO> convertToRaceDTOs(Regatta regatta) {
        List<RaceWithCompetitorsDTO> result = new ArrayList<RaceWithCompetitorsDTO>();
        for (RaceDefinition r : regatta.getAllRaces()) {
            RegattaAndRaceIdentifier raceIdentifier = new RegattaNameAndRaceName(regatta.getName(), r.getName());
            TrackedRace trackedRace = getService().getExistingTrackedRace(raceIdentifier);
            TrackedRaceDTO trackedRaceDTO = null; 
            if (trackedRace != null) {
                trackedRaceDTO = new TrackedRaceDTO();
                trackedRaceDTO.hasWindData = trackedRace.hasWindData();
                trackedRaceDTO.hasGPSData = trackedRace.hasGPSData();
                trackedRaceDTO.startOfTracking = trackedRace.getStartOfTracking() == null ? null : trackedRace.getStartOfTracking().asDate();
                trackedRaceDTO.endOfTracking = trackedRace.getEndOfTracking() == null ? null : trackedRace.getEndOfTracking().asDate();
                trackedRaceDTO.timePointOfNewestEvent = trackedRace.getTimePointOfNewestEvent() == null ? null : trackedRace.getTimePointOfNewestEvent().asDate();
                trackedRaceDTO.delayToLiveInMs = trackedRace.getDelayToLiveInMillis(); 
            }
            RaceWithCompetitorsDTO raceDTO = new RaceWithCompetitorsDTO(raceIdentifier, convertToCompetitorDTOs(r.getCompetitors()),
                    trackedRaceDTO, getService().isRaceBeingTracked(r));
            if (trackedRace != null) {
                raceDTO.startOfRace = trackedRace.getStartOfRace() == null ? null : trackedRace.getStartOfRace().asDate();
                raceDTO.endOfRace = trackedRace.getEndOfRace() == null ? null : trackedRace.getEndOfRace().asDate();
                raceDTO.status = new RaceStatusDTO();
                raceDTO.status.status = trackedRace.getStatus().getStatus();
                raceDTO.status.loadingProgress = trackedRace.getStatus().getLoadingProgress();
            }
            raceDTO.boatClass = regatta.getBoatClass() == null ? null : regatta.getBoatClass().getName(); 
            result.add(raceDTO);
        }
        return result;
    }

    private List<CompetitorDTO> convertToCompetitorDTOs(Iterable<Competitor> competitors) {
        List<CompetitorDTO> result = new ArrayList<CompetitorDTO>();
        for (Competitor c : competitors) {
            CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(c);
            result.add(competitorDTO);
        }
        return result;
    }

    @Override
    public Pair<String, List<TracTracRaceRecordDTO>> listTracTracRacesInEvent(String eventJsonURL, boolean listHiddenRaces) throws MalformedURLException, IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        com.sap.sailing.domain.common.impl.Util.Pair<String,List<RaceRecord>> raceRecords;
        raceRecords = getService().getTracTracRaceRecords(new URL(eventJsonURL));
        List<TracTracRaceRecordDTO> result = new ArrayList<TracTracRaceRecordDTO>();
        for (RaceRecord raceRecord : raceRecords.getB()) {
            if (listHiddenRaces == false && raceRecord.getRaceStatus().equals(TracTracConnectionConstants.HIDDEN_STATUS)) {
                continue;
            }
            
            result.add(new TracTracRaceRecordDTO(raceRecord.getID(), raceRecord.getEventName(), raceRecord.getName(),
                    raceRecord.getParamURL().toString(), raceRecord.getReplayURL(), raceRecord.getLiveURI().toString(),
                    raceRecord.getStoredURI().toString(), raceRecord.getTrackingStartTime().asDate(), raceRecord
                    .getTrackingEndTime().asDate(), raceRecord.getRaceStartTime().asDate(), raceRecord.getBoatClassNames(),
                    raceRecord.getRaceStatus()));
        }
        return new Pair<String, List<TracTracRaceRecordDTO>>(raceRecords.getA(), result);
    }

    @Override
    public void trackWithTracTrac(RegattaIdentifier regattaToAddTo, Iterable<TracTracRaceRecordDTO> rrs, String liveURI, String storedURI, 
            String courseDesignUpdateURI, boolean trackWind, final boolean correctWindByDeclination, final boolean simulateWithStartTimeNow, 
            String tracTracUsername, String tracTracPassword) throws Exception {
        logger.info("tracWithTracTrac for regatta "+regattaToAddTo+" for race records "+rrs+" with liveURI "+liveURI+" and storedURI "+storedURI);
        for (TracTracRaceRecordDTO rr : rrs) {
            if (liveURI == null || liveURI.trim().length() == 0) {
                liveURI = rr.liveURI;
            }
            if (storedURI == null || storedURI.trim().length() == 0) {
                storedURI = rr.storedURI;
            }
            final RacesHandle raceHandle = getService().addTracTracRace(regattaToAddTo, new URL(rr.paramURL),
                    new URI(liveURI), new URI(storedURI), new URI(courseDesignUpdateURI), new MillisecondsTimePoint(rr.trackingStartTime),
                    new MillisecondsTimePoint(rr.trackingEndTime),
                    MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory),
                    MongoWindStoreFactory.INSTANCE.getMongoWindStore(mongoObjectFactory, domainObjectFactory),
                    RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS, simulateWithStartTimeNow, tracTracUsername, tracTracPassword);
            if (trackWind) {
                new Thread("Wind tracking starter for race " + rr.regattaName + "/" + rr.name) {
                    public void run() {
                        try {
                            startTrackingWind(raceHandle, correctWindByDeclination,
                                    RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }.start();
            }
        }
    }

    @Override
    public List<TracTracConfigurationDTO> getPreviousTracTracConfigurations() throws Exception {
        Iterable<TracTracConfiguration> configs = tractracDomainObjectFactory.getTracTracConfigurations();
        List<TracTracConfigurationDTO> result = new ArrayList<TracTracConfigurationDTO>();
        for (TracTracConfiguration ttConfig : configs) {
            result.add(new TracTracConfigurationDTO(ttConfig.getName(), ttConfig.getJSONURL().toString(),
                    ttConfig.getLiveDataURI().toString(), ttConfig.getStoredDataURI().toString(), ttConfig.getCourseDesignUpdateURI().toString(),
                    ttConfig.getTracTracUsername().toString(), ttConfig.getTracTracPassword().toString()));
        }
        return result;
    }

    @Override
    public void storeTracTracConfiguration(String name, String jsonURL, String liveDataURI, String storedDataURI, String courseDesignUpdateURI, String tracTracUsername, String tracTracPassword) throws Exception {
        tractracMongoObjectFactory.storeTracTracConfiguration(tractracDomainFactory.createTracTracConfiguration(name, jsonURL, liveDataURI, storedDataURI, 
                courseDesignUpdateURI, tracTracUsername, tracTracPassword));
    }

    @Override
    public void stopTrackingEvent(RegattaIdentifier regattaIdentifier) throws Exception {
        getService().apply(new StopTrackingRegatta(regattaIdentifier));
    }

    private RaceDefinition getRaceByName(Regatta regatta, String raceName) {
        if (regatta != null) {
            return regatta.getRaceByName(raceName);
        } else {
            return null;
        }
    }

    @Override
    public void stopTrackingRaces(Iterable<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) throws Exception {
        for (RegattaAndRaceIdentifier regattaAndRaceIdentifier : regattaAndRaceIdentifiers) {
            getService().apply(new StopTrackingRace(regattaAndRaceIdentifier));
        }
    }

    @Override
    public void removeAndUntrackRaces(Iterable<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) {
        for (RegattaAndRaceIdentifier regattaAndRaceIdentifier : regattaAndRaceIdentifiers) {
            getService().apply(new RemoveAndUntrackRace(regattaAndRaceIdentifier));
        }
    }

    /**
     * @param timeoutInMilliseconds eventually passed to {@link RacesHandle#getRaces(long)}. If the race definition
     * can be obtained within this timeout, wind for the race will be tracked; otherwise, the method returns without
     * taking any effect.
     */
    private void startTrackingWind(RacesHandle raceHandle, boolean correctByDeclination, long timeoutInMilliseconds) throws Exception {
        Regatta regatta = raceHandle.getRegatta();
        if (regatta != null) {
            for (RaceDefinition race : raceHandle.getRaces(timeoutInMilliseconds)) {
                if (race != null) {
                    getService().startTrackingWind(regatta, race, correctByDeclination);
                } else {
                    log("RaceDefinition wasn't received within " + timeoutInMilliseconds + "ms for a race in regatta "
                            + regatta.getName() + ". Aborting wait; no wind tracking for this race.");
                }
            }
        }
    }

    @Override
    public WindInfoForRaceDTO getRawWindFixes(RegattaAndRaceIdentifier raceIdentifier, Collection<WindSource> windSources) {
        WindInfoForRaceDTO result = null;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
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

    protected WindDTO createWindDTO(Wind wind, WindTrack windTrack) {
        WindDTO windDTO = new WindDTO();
        windDTO.trueWindBearingDeg = wind.getBearing().getDegrees();
        windDTO.trueWindFromDeg = wind.getBearing().reverse().getDegrees();
        windDTO.trueWindSpeedInKnots = wind.getKnots();
        windDTO.trueWindSpeedInMetersPerSecond = wind.getMetersPerSecond();
        if (wind.getPosition() != null) {
            windDTO.position = new PositionDTO(wind.getPosition().getLatDeg(), wind.getPosition()
                    .getLngDeg());
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
    protected WindDTO createWindDTOFromAlreadyAveraged(Wind wind, WindTrack windTrack, TimePoint requestTimepoint) {
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
            windDTO.position = new PositionDTO(wind.getPosition().getLatDeg(), wind.getPosition()
                    .getLngDeg());
        }
        if (wind.getTimePoint() != null) {
            windDTO.measureTimepoint = wind.getTimePoint().asMillis();
        }
        return windDTO;
    }

    /**
     * Fetches the {@link WindTrack#getAveragedWind(Position, TimePoint) average wind} from all wind tracks or those identified
     * by <code>windSourceTypeNames</code>
     */
    //@Override
    public WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth,
            int numberOfFixes, double latDeg, double lngDeg, Collection<String> windSourceTypeNames)
                    throws NoWindException {
        Position position = new DegreePosition(latDeg, lngDeg);
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
            List<WindSource> windSourcesToDeliver = new ArrayList<WindSource>();
            Util.addAll(trackedRace.getWindSources(), windSourcesToDeliver);
            // TODO bug #375: add the combined wind; currently, CombinedWindTrackImpl just takes too long to return results...
            windSourcesToDeliver.add(new WindSourceImpl(WindSourceType.COMBINED));
            for (WindSource windSource : windSourcesToDeliver) {
                if (windSourceTypeNames == null || windSourceTypeNames.contains(windSource.getType().name())) {
                    TimePoint fromTimePoint = new MillisecondsTimePoint(from);
                    WindTrackInfoDTO windTrackInfoDTO = new WindTrackInfoDTO();
                    windTrackInfoDTO.windFixes = new ArrayList<WindDTO>();
                    WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
                    windTrackInfoDTOs.put(windSource, windTrackInfoDTO);
                    windTrackInfoDTO.dampeningIntervalInMilliseconds = windTrack
                            .getMillisecondsOverWhichToAverageWind();
                    TimePoint timePoint = fromTimePoint;
                    Double minWindConfidence = 2.0;
                    Double maxWindConfidence = -1.0;
                    for (int i = 0; i < numberOfFixes; i++) {
                        WindWithConfidence<Pair<Position, TimePoint>> averagedWindWithConfidence = windTrack.getAveragedWindWithConfidence(position, timePoint);

                        if (averagedWindWithConfidence != null) {
                            WindDTO windDTO = createWindDTOFromAlreadyAveraged(averagedWindWithConfidence.getObject(), windTrack, timePoint);
                            double confidence = averagedWindWithConfidence.getConfidence();
                            windDTO.confidence = confidence;
                            windTrackInfoDTO.windFixes.add(windDTO);
                            if(confidence < minWindConfidence) {
                                minWindConfidence = confidence;
                            }
                            if(confidence > maxWindConfidence) {
                                maxWindConfidence = confidence;
                            }
                        }
                        timePoint = new MillisecondsTimePoint(timePoint.asMillis() + millisecondsStepWidth);
                    }
                    windTrackInfoDTO.minWindConfidence = minWindConfidence; 
                    windTrackInfoDTO.maxWindConfidence = maxWindConfidence; 
                }
            }
        }
        return result;
    }

    @Override
    public WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth,
            int numberOfFixes, Collection<String> windSourceTypeNames)
                    throws NoWindException {
        assert from != null;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);

        WindInfoForRaceDTO result = getAveragedWindInfo(new MillisecondsTimePoint(from), millisecondsStepWidth, numberOfFixes,
                windSourceTypeNames, trackedRace);
        return result;
    }

    private WindInfoForRaceDTO getAveragedWindInfo(TimePoint from, long millisecondsStepWidth, int numberOfFixes,
            Collection<String> windSourceTypeNames, TrackedRace trackedRace) {
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
            List<WindSource> windSourcesToDeliver = new ArrayList<WindSource>();
            Util.addAll(trackedRace.getWindSources(), windSourcesToDeliver);
            windSourcesToDeliver.add(new WindSourceImpl(WindSourceType.COMBINED));
            for (WindSource windSource : windSourcesToDeliver) {
                // TODO consider parallelizing
                if (windSourceTypeNames == null || windSourceTypeNames.contains(windSource.getType().name())) {
                    WindTrackInfoDTO windTrackInfoDTO = new WindTrackInfoDTO();
                    windTrackInfoDTOs.put(windSource, windTrackInfoDTO);
                    windTrackInfoDTO.windFixes = new ArrayList<WindDTO>();
                    WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
                    windTrackInfoDTO.dampeningIntervalInMilliseconds = windTrack
                            .getMillisecondsOverWhichToAverageWind();
                    TimePoint timePoint = from;
                    Double minWindConfidence = 2.0;
                    Double maxWindConfidence = -1.0;
                    for (int i = 0; i < numberOfFixes && newestEvent != null && timePoint.compareTo(newestEvent) < 0; i++) {
                        WindWithConfidence<Pair<Position, TimePoint>> averagedWindWithConfidence = windTrack.getAveragedWindWithConfidence(null, timePoint);
                        if (averagedWindWithConfidence != null) {
                            double confidence = averagedWindWithConfidence.getConfidence();
                            WindDTO windDTO = createWindDTOFromAlreadyAveraged(averagedWindWithConfidence.getObject(), windTrack, timePoint);
                            windDTO.confidence = confidence;
                            windTrackInfoDTO.windFixes.add(windDTO);
                            if(confidence < minWindConfidence) {
                                minWindConfidence = confidence;
                            }
                            if(confidence > maxWindConfidence) {
                                maxWindConfidence = confidence;
                            }
                        }
                        timePoint = new MillisecondsTimePoint(timePoint.asMillis() + millisecondsStepWidth);
                    }
                    windTrackInfoDTO.minWindConfidence = minWindConfidence; 
                    windTrackInfoDTO.maxWindConfidence = maxWindConfidence; 
                }
            }
        }
        return result;
    }

    @Override
    public WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, Date to,
            long resolutionInMilliseconds, Collection<String> windSourceTypeNames) {
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        WindInfoForRaceDTO result = null;
        if (trackedRace != null) {
            TimePoint fromTimePoint = from == null ? trackedRace.getStartOfTracking() : new MillisecondsTimePoint(from);
            TimePoint toTimePoint = to == null ? trackedRace.getEndOfRace() : new MillisecondsTimePoint(to);
            if(fromTimePoint != null && toTimePoint != null) {
                int numberOfFixes = (int) ((toTimePoint.asMillis() - fromTimePoint.asMillis())/resolutionInMilliseconds);
                result = getAveragedWindInfo(fromTimePoint, resolutionInMilliseconds, numberOfFixes, windSourceTypeNames, trackedRace);
            }
        }
        return result;
    }

    @Override
    public void setWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO) {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            Position p = null;
            if (windDTO.position != null) {
                p = new DegreePosition(windDTO.position.latDeg, windDTO.position.lngDeg);
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
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(), new DegreeBearingImpl(
                            windDTO.trueWindBearingDeg));
                } else if (windDTO.trueWindFromDeg != null) {
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(), new DegreeBearingImpl(
                            windDTO.trueWindFromDeg).reverse());
                }
            }
            Wind wind = new WindImpl(p, at, speedWithBearing);
            Iterable<WindSource> webWindSources = trackedRace.getWindSources(WindSourceType.WEB);
            if(Util.size(webWindSources) == 0) {
                // create a new WEB wind source if not available
                trackedRace.recordWind(wind, new WindSourceImpl(WindSourceType.WEB));
            } else {
                trackedRace.recordWind(wind, webWindSources.iterator().next());
            }
        }
    }

    @Override
    public CompactRaceMapDataDTO getRaceMapData(RegattaAndRaceIdentifier raceIdentifier, Date date,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate) throws NoWindException {
        return new CompactRaceMapDataDTO(getBoatPositions(raceIdentifier, fromPerCompetitorIdAsString,
                toPerCompetitorIdAsString, extrapolate), getCoursePositions(raceIdentifier, date), getQuickRanks(
                raceIdentifier, date));
    }    

    /**
     * @param from
     *            for the list of competitors provided as keys of this map, requests the GPS fixes starting with the
     *            date provided as value
     * @param to
     *            for the list of competitors provided as keys (expected to be equal to the set of competitors used as
     *            keys in the <code>from</code> parameter, requests the GPS fixes up to but excluding the date provided
     *            as value
     * @param extrapolate
     *            if <code>true</code> and no position is known for <code>date</code>, the last entry returned in the
     *            list of GPS fixes will be obtained by extrapolating from the competitors last known position before
     *            <code>date</code> and the estimated speed.
     * @return a map where for each competitor participating in the race the list of GPS fixes in increasing
     *         chronological order is provided. The last one is the last position at or before <code>date</code>.
     */
    private Map<CompetitorDTO, List<GPSFixDTO>> getBoatPositions(RegattaAndRaceIdentifier raceIdentifier,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate) throws NoWindException {
        Map<CompetitorDTO, List<GPSFixDTO>> result = new HashMap<CompetitorDTO, List<GPSFixDTO>>();
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                if (fromPerCompetitorIdAsString.containsKey(competitor.getId().toString())) {
                    CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
                    List<GPSFixDTO> fixesForCompetitor = new ArrayList<GPSFixDTO>();
                    result.put(competitorDTO, fixesForCompetitor);
                    GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
                    TimePoint fromTimePoint = new MillisecondsTimePoint(fromPerCompetitorIdAsString.get(competitorDTO.idAsString));
                    TimePoint toTimePointExcluding = new MillisecondsTimePoint(toPerCompetitorIdAsString.get(competitorDTO.idAsString));
                    // copy the fixes into a list while holding the monitor; then release the monitor to avoid deadlocks
                    // during wind estimations required for tack determination
                    List<GPSFixMoving> fixes = new ArrayList<GPSFixMoving>();
                    track.lockForRead();
                    try {
                        Iterator<GPSFixMoving> fixIter = track.getFixesIterator(fromTimePoint, /* inclusive */true);
                        while (fixIter.hasNext()) {
                            GPSFixMoving fix = fixIter.next();
                            if (fix.getTimePoint().compareTo(toTimePointExcluding) < 0) {
                                fixes.add(fix);
                            } else {
                                break;
                            }
                        }
                    } finally {
                        track.unlockAfterRead();
                    }
                    if (fixes.isEmpty()) {
                        // then there was no (smoothened) fix between fromTimePoint and toTimePointExcluding; estimate...
                        TimePoint middle = new MillisecondsTimePoint((toTimePointExcluding.asMillis()+fromTimePoint.asMillis())/2);
                        Position estimatedPosition = track.getEstimatedPosition(middle, extrapolate);
                        SpeedWithBearing estimatedSpeed = track.getEstimatedSpeed(middle);
                        if(estimatedPosition != null && estimatedSpeed != null) {
                            fixes.add(new GPSFixMovingImpl(estimatedPosition, middle, estimatedSpeed));
                        }
                    }
                    Iterator<GPSFixMoving> fixIter = fixes.iterator();
                    if (fixIter.hasNext()) {
                        final WindSource windSource = new WindSourceImpl(WindSourceType.COMBINED);
                        GPSFixMoving fix = fixIter.next();
                        while (fix != null && (fix.getTimePoint().compareTo(toTimePointExcluding) < 0 ||
                                (fix.getTimePoint().equals(toTimePointExcluding) && toTimePointExcluding.equals(fromTimePoint)))) {
                            Tack tack = trackedRace.getTack(competitor, fix.getTimePoint());
                            TrackedLegOfCompetitor trackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor,
                                    fix.getTimePoint());
                            LegType legType = trackedLegOfCompetitor == null ? null : trackedRace.getTrackedLeg(
                                    trackedLegOfCompetitor.getLeg()).getLegType(fix.getTimePoint());
                            Wind wind = trackedRace.getWind(fix.getPosition(),toTimePointExcluding);
                            WindDTO windDTO = createWindDTOFromAlreadyAveraged(wind, trackedRace.getOrCreateWindTrack(windSource), toTimePointExcluding);
                            GPSFixDTO fixDTO = createGPSFixDTO(fix, fix.getSpeed(), windDTO, tack, legType, /* extrapolate */
                                    false);
                            fixesForCompetitor.add(fixDTO);
                            if (fixIter.hasNext()) {
                                fix = fixIter.next();
                            } else {
                                // check if fix was at date and if extrapolation is requested
                                if (!fix.getTimePoint().equals(toTimePointExcluding) && extrapolate) {
                                    Position position = track.getEstimatedPosition(toTimePointExcluding, extrapolate);
                                    Tack tack2 = trackedRace.getTack(competitor, toTimePointExcluding);
                                    LegType legType2 = trackedLegOfCompetitor == null ? null : trackedRace
                                            .getTrackedLeg(trackedLegOfCompetitor.getLeg()).getLegType(
                                                    fix.getTimePoint());
                                    SpeedWithBearing speedWithBearing = track.getEstimatedSpeed(toTimePointExcluding);
                                    Wind wind2 = trackedRace.getWind(position, toTimePointExcluding);
                                    WindDTO windDTO2 = createWindDTOFromAlreadyAveraged(wind2, trackedRace.getOrCreateWindTrack(windSource), toTimePointExcluding);
                                    GPSFixDTO extrapolated = new GPSFixDTO(
                                            toPerCompetitorIdAsString.get(competitorDTO.idAsString),
                                            position==null?null:new PositionDTO(position.getLatDeg(), position.getLngDeg()),
                                                    speedWithBearing==null?null:createSpeedWithBearingDTO(speedWithBearing), windDTO2,
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

    private SpeedWithBearingDTO createSpeedWithBearingDTO(SpeedWithBearing speedWithBearing) {
        return new SpeedWithBearingDTO(speedWithBearing.getKnots(), speedWithBearing
                .getBearing().getDegrees());
    }

    private GPSFixDTO createGPSFixDTO(GPSFix fix, SpeedWithBearing speedWithBearing, WindDTO windDTO, Tack tack, LegType legType, boolean extrapolated) {
        return new GPSFixDTO(fix.getTimePoint().asDate(), fix.getPosition()==null?null:new PositionDTO(fix
                .getPosition().getLatDeg(), fix.getPosition().getLngDeg()),
                speedWithBearing==null?null:createSpeedWithBearingDTO(speedWithBearing), windDTO, tack, legType, extrapolated);
    }

    @Override
    public RaceTimesInfoDTO getRaceTimesInfo(RegattaAndRaceIdentifier raceIdentifier) {
        RaceTimesInfoDTO raceTimesInfo = null;
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);

        if (trackedRace != null) {
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
            raceTimesInfo.delayToLiveInMs = trackedRace.getDelayToLiveInMillis();

            Iterable<Pair<Waypoint, Pair<TimePoint, TimePoint>>> markPassingsTimes = trackedRace.getMarkPassingsTimes();
            synchronized (markPassingsTimes) {
                int numberOfWaypoints = Util.size(markPassingsTimes);
                int wayPointNumber = 1;
                for (Pair<Waypoint, Pair<TimePoint, TimePoint>> markPassingTimes : markPassingsTimes) {
                    MarkPassingTimesDTO markPassingTimesDTO = new MarkPassingTimesDTO();
                    String name = "M" + (wayPointNumber - 1);
                    if (wayPointNumber == 1) {
                        name = "S";
                    } else if (wayPointNumber == numberOfWaypoints) {
                        name = "F";
                    }
                    markPassingTimesDTO.name = name;
                    Pair<TimePoint, TimePoint> timesPair = markPassingTimes.getB();
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
                    legInfoDTO.name = "L" + legNumber;
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
        return raceTimesInfo;
    }

    @Override
    public List<RaceTimesInfoDTO> getRaceTimesInfos(Collection<RegattaAndRaceIdentifier> raceIdentifiers) {
        List<RaceTimesInfoDTO> raceTimesInfos = new ArrayList<RaceTimesInfoDTO>();
        for (RegattaAndRaceIdentifier raceIdentifier : raceIdentifiers) {
            RaceTimesInfoDTO raceTimesInfo = getRaceTimesInfo(raceIdentifier);
            if (raceTimesInfo != null) {
                raceTimesInfos.add(raceTimesInfo);
            }
        }
        return raceTimesInfos;
    }

    @Override
    public CoursePositionsDTO getCoursePositions(RegattaAndRaceIdentifier raceIdentifier, Date date) {
        CoursePositionsDTO result = new CoursePositionsDTO();
        if (date != null) {
            TimePoint dateAsTimePoint = new MillisecondsTimePoint(date);
            TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
            if (trackedRace != null) {
                result.marks = new HashSet<MarkDTO>();
                result.waypointPositions = new ArrayList<PositionDTO>();
                Set<Mark> marks = new HashSet<Mark>();
                Course course = trackedRace.getRace().getCourse();
                for (Waypoint waypoint : course.getWaypoints()) {
                    Position waypointPosition = trackedRace.getApproximatePosition(waypoint, dateAsTimePoint);
                    if (waypointPosition != null) {
                        result.waypointPositions.add(new PositionDTO(waypointPosition.getLatDeg(), waypointPosition.getLngDeg()));
                    }
                    for (Mark b : waypoint.getMarks()) {
                        marks.add(b);
                    }
                }
                for (Mark mark : marks) {
                    GPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(mark);
                    Position positionAtDate = track.getEstimatedPosition(dateAsTimePoint, /* extrapolate */false);
                    if (positionAtDate != null) {
                        result.marks.add(convertToMarkDTO(mark, positionAtDate));
                    }
                }

                // set the positions of start and finish
                Waypoint firstWaypoint = course.getFirstWaypoint();
                if (firstWaypoint != null) {
                    result.startMarkPositions = getMarkPositionDTOs(dateAsTimePoint, trackedRace, firstWaypoint);
                }                    
                Waypoint lastWaypoint = course.getLastWaypoint();
                if (lastWaypoint != null) {
                    result.finishMarkPositions = getMarkPositionDTOs(dateAsTimePoint, trackedRace, lastWaypoint);
                }
            }
        }
        return result;
    }
      
    @Override
    public RaceCourseDTO getRaceCourse(RegattaAndRaceIdentifier raceIdentifier, Date date) {
        List<WaypointDTO> waypointDTOs = new ArrayList<WaypointDTO>();
        RaceCourseDTO result = new RaceCourseDTO(waypointDTOs);
        if (date != null) {
            TimePoint dateAsTimePoint = new MillisecondsTimePoint(date);
            TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
            if (trackedRace != null) {
                Course course = trackedRace.getRace().getCourse();
                for (Waypoint waypoint : course.getWaypoints()) {
                    List<MarkDTO> markDTOs = new ArrayList<MarkDTO>();
                    for (Mark mark : trackedRace.getMarks()) {
                        GPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(mark);
                        Position positionAtDate = track.getEstimatedPosition(dateAsTimePoint, /* extrapolate */false);
                        markDTOs.add(convertToMarkDTO(mark, positionAtDate));
                    }
                    ControlPointDTO controlPointDTO = convertToControlPointDTO(waypoint.getControlPoint(), trackedRace, dateAsTimePoint);
                    WaypointDTO waypointDTO = new WaypointDTO(waypoint.getName(), controlPointDTO, markDTOs, waypoint.getPassingSide());
                    waypointDTOs.add(waypointDTO);
                }
            }
        }
        return result;
    }
    
    private ControlPointDTO convertToControlPointDTO(ControlPoint controlPoint, TrackedRace trackedRace, TimePoint timePoint) {
        ControlPointDTO result;
        if (controlPoint instanceof Gate) {
            final Mark left = ((Gate) controlPoint).getLeft();
            final Position leftPos = trackedRace.getOrCreateTrack(left).getEstimatedPosition(timePoint, /* extrapolate */ false);
            final Mark right = ((Gate) controlPoint).getRight();
            final Position rightPos = trackedRace.getOrCreateTrack(right).getEstimatedPosition(timePoint, /* extrapolate */ false);
            result = new GateDTO(controlPoint.getId().toString(), controlPoint.getName(), convertToMarkDTO(left, leftPos), convertToMarkDTO(right, rightPos)); 
        } else {
            final Position posOfFirst = trackedRace.getOrCreateTrack(controlPoint.getMarks().iterator().next()).
                    getEstimatedPosition(timePoint, /* extrapolate */ false);
            result = new MarkDTO(controlPoint.getId().toString(), controlPoint.getName(), posOfFirst.getLatDeg(), posOfFirst.getLngDeg());
        }
        return result;
    }
    
    private ControlPointDTO convertToControlPointDTO(ControlPoint controlPoint) {
        ControlPointDTO result;
        if (controlPoint instanceof Gate) {
            final Mark left = ((Gate) controlPoint).getLeft();
            final Mark right = ((Gate) controlPoint).getRight();
            result = new GateDTO(controlPoint.getId().toString(), controlPoint.getName(), convertToMarkDTO(left, null), convertToMarkDTO(right, null)); 
        } else {
            result = new MarkDTO(controlPoint.getId().toString(), controlPoint.getName());
        }
        return result;
    }

    /**
     * For each {@link ControlPointDTO} in <code>controlPoints</code> tries to find the best-matching waypoint
     * from the course that belongs to the race identified by <code>raceIdentifier</code>. If such a waypoint is
     * found, its control point is added to the control point list for the new course. Otherwise, a new control
     * point is created using the default {@link com.sap.sailing.domain.base.DomainFactory} instance. The resulting
     * list of control points is then passed to {@link Course#update(List, com.sap.sailing.domain.base.DomainFactory)} for
     * the course of the race identified by <code>raceIdentifier</code>.
     */
    @Override
    public void updateRaceCourse(RegattaAndRaceIdentifier raceIdentifier, List<Pair<ControlPointDTO, NauticalSide>> controlPoints) {
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            Course course = trackedRace.getRace().getCourse();
            Iterable<Waypoint> waypoints = course.getWaypoints();
            List<Pair<ControlPoint, NauticalSide>> newControlPoints = new ArrayList<Pair<ControlPoint, NauticalSide>>();
            int lastMatchPosition = -1;
            for (Pair<ControlPointDTO, NauticalSide> controlPointAndPassingSide : controlPoints) {
                ControlPointDTO controlPointDTO = controlPointAndPassingSide.getA();
                ControlPoint matchFromOldCourse = null;
                for (int i=lastMatchPosition+1; matchFromOldCourse == null && i<Util.size(waypoints); i++) {
                    Waypoint waypointAtI = Util.get(waypoints, i);
                    ControlPoint controlPointAtI = waypointAtI.getControlPoint();
                    if (controlPointAtI.getId().toString().equals(controlPointDTO.getIdAsString()) && markIDsMatch(controlPointAtI.getMarks(), controlPointDTO.getMarks())) {
                        matchFromOldCourse = controlPointAtI;
                        newControlPoints.add(new Pair<ControlPoint, NauticalSide>(matchFromOldCourse, null));
                        lastMatchPosition = i;
                    }
                }
                if (matchFromOldCourse == null) {
                    // no match found; create new control point:
                    ControlPoint newControlPoint;
                    if (controlPointDTO instanceof GateDTO) {
                        GateDTO gateDTO = (GateDTO) controlPointDTO;
                        final Serializable id;
                        if (gateDTO.getIdAsString() == null) {
                            id = UUID.randomUUID();
                        } else {
                            id = gateDTO.getIdAsString();
                        }
                        Mark left = baseDomainFactory.getOrCreateMark(gateDTO.getLeft().getIdAsString(), gateDTO.getLeft().name);
                        Mark right = baseDomainFactory.getOrCreateMark(gateDTO.getRight().getIdAsString(), gateDTO.getRight().name);
                        newControlPoint = baseDomainFactory.createGate(id, left, right, gateDTO.name);
                        newControlPoints.add(new Pair<ControlPoint, NauticalSide>(newControlPoint, null));
                    } else {
                        newControlPoint = baseDomainFactory.getOrCreateMark(controlPointDTO.getIdAsString(), controlPointDTO.name);
                        NauticalSide nauticalSide = controlPointAndPassingSide.getB();
                        newControlPoints.add(new Pair<ControlPoint, NauticalSide>(newControlPoint, nauticalSide));
                    }
                }
            }
            try {
                course.update(newControlPoints, baseDomainFactory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean markIDsMatch(Iterable<Mark> marks, Iterable<MarkDTO> marksDTOs) {
        Iterator<Mark> marksIter = marks.iterator();
        Iterator<MarkDTO> markDTOsIter = marksDTOs.iterator();
        while (marksIter.hasNext() && markDTOsIter.hasNext()) {
            Mark nextMark = marksIter.next();
            MarkDTO nextMarkDTO = markDTOsIter.next();
            if (!nextMark.getId().toString().equals(nextMarkDTO.getIdAsString())) {
                return false;
            }
        }
        return marksIter.hasNext() == markDTOsIter.hasNext();
    }

    private List<PositionDTO> getMarkPositionDTOs(TimePoint timePoint, TrackedRace trackedRace, Waypoint waypoint) {
        List<PositionDTO> startMarkPositions = new ArrayList<PositionDTO>();
        for (Mark startMark : waypoint.getMarks()) {
            final Position estimatedMarkPosition = trackedRace.getOrCreateTrack(startMark)
                    .getEstimatedPosition(timePoint, /* extrapolate */false);
            if (estimatedMarkPosition != null) {
                startMarkPositions.add(new PositionDTO(estimatedMarkPosition.getLatDeg(), estimatedMarkPosition.getLngDeg()));
            }
        }
        return startMarkPositions;
    }

    private List<QuickRankDTO> getQuickRanks(RegattaAndRaceIdentifier raceIdentifier, Date date) throws NoWindException {
        List<QuickRankDTO> result = new ArrayList<QuickRankDTO>();
        if (date != null) {
            TimePoint dateAsTimePoint = new MillisecondsTimePoint(date);
            TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
            if (trackedRace != null) {
                RaceDefinition race = trackedRace.getRace();
                for (Competitor competitor : race.getCompetitors()) {
                    int rank = trackedRace.getRank(competitor, dateAsTimePoint);
                    TrackedLegOfCompetitor trackedLeg = trackedRace.getTrackedLeg(competitor, dateAsTimePoint);
                    if (trackedLeg != null) {
                        int legNumber = race.getCourse().getLegs().indexOf(trackedLeg.getLeg()) + 1;
                        QuickRankDTO quickRankDTO = new QuickRankDTO(baseDomainFactory.convertToCompetitorDTO(competitor), rank, legNumber);
                        result.add(quickRankDTO);
                    }
                }
                Collections.sort(result, new Comparator<QuickRankDTO>() {
                    @Override
                    public int compare(QuickRankDTO o1, QuickRankDTO o2) {
                        return o1.rank - o2.rank;
                    }
                });
            }
        }
        return result;
    }

    @Override
    public void setRaceIsKnownToStartUpwind(RegattaAndRaceIdentifier raceIdentifier, boolean raceIsKnownToStartUpwind) {
        getService().apply(new SetRaceIsKnownToStartUpwind(raceIdentifier, raceIsKnownToStartUpwind));
    }

    @Override
    public void setWindSourcesToExclude(RegattaAndRaceIdentifier raceIdentifier, Iterable<WindSource> windSourcesToExclude) {
        getService().apply(new SetWindSourcesToExclude(raceIdentifier, windSourcesToExclude));
    }

    @Override
    public WindInfoForRaceDTO getWindSourcesInfo(RegattaAndRaceIdentifier raceIdentifier) {
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

            for(WindSource windSource: trackedRace.getWindSources()) {
                windTrackInfoDTOs.put(windSource, new WindTrackInfoDTO());
            }
            windTrackInfoDTOs.put(new WindSourceImpl(WindSourceType.COMBINED), new WindTrackInfoDTO());
        }
        return result;
    }

    @Override
    public void removeWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO) {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            Position p = null;
            if (windDTO.position != null) {
                p = new DegreePosition(windDTO.position.latDeg, windDTO.position.lngDeg);
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
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(), new DegreeBearingImpl(
                            windDTO.trueWindBearingDeg));
                } else if (windDTO.trueWindFromDeg != null) {
                    speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(), new DegreeBearingImpl(
                            windDTO.trueWindFromDeg).reverse());
                }
            }
            Wind wind = new WindImpl(p, at, speedWithBearing);
            trackedRace.removeWind(wind, trackedRace.getWindSources(WindSourceType.WEB).iterator().next());
        }
    }

    protected RacingEventService getService() {
        return racingEventServiceTracker.getService(); // grab the service
    }

    private ReplicationService getReplicationService() {
        return replicationServiceTracker.getService();
    }

    @Override
    public List<String> getLeaderboardNames() throws Exception {
        return new ArrayList<String>(getService().getLeaderboards().keySet());
    }

    @Override
    public StrippedLeaderboardDTO createFlexibleLeaderboard(String leaderboardName, String leaderboardDisplayName, int[] discardThresholds, ScoringSchemeType scoringSchemeType,
            String courseAreaId) {
        UUID courseAreaUuid = convertIdentifierStringToUuid(courseAreaId);
        return createStrippedLeaderboardDTO(getService().apply(new CreateFlexibleLeaderboard(leaderboardName, leaderboardDisplayName, discardThresholds,
                baseDomainFactory.createScoringScheme(scoringSchemeType), courseAreaUuid)), false);
    }

    private UUID convertIdentifierStringToUuid(String identifierToConvert) {
        UUID convertedUuid = null;
        if (identifierToConvert != null) {
            try {
                convertedUuid = UUID.fromString(identifierToConvert);
            } catch (IllegalArgumentException iae) {}
        }
        return convertedUuid;
    }

    public StrippedLeaderboardDTO createRegattaLeaderboard(RegattaIdentifier regattaIdentifier, String leaderboardDisplayName, int[] discardThresholds) {
        return createStrippedLeaderboardDTO(getService().apply(new CreateRegattaLeaderboard(regattaIdentifier, leaderboardDisplayName, discardThresholds)), false);
    }

    @Override
    public List<StrippedLeaderboardDTO> getLeaderboards() {
        Map<String, Leaderboard> leaderboards = getService().getLeaderboards();
        List<StrippedLeaderboardDTO> results = new ArrayList<StrippedLeaderboardDTO>();
        for(Leaderboard leaderboard: leaderboards.values()) {
            StrippedLeaderboardDTO dao = createStrippedLeaderboardDTO(leaderboard, false);
            results.add(dao);
        }
        return results;
    }

    @Override
    public StrippedLeaderboardDTO getLeaderboard(String leaderboardName) {
        Map<String, Leaderboard> leaderboards = getService().getLeaderboards();
        StrippedLeaderboardDTO result = null;
        Leaderboard leaderboard = leaderboards.get(leaderboardName);
        if (leaderboard != null) {
            result = createStrippedLeaderboardDTO(leaderboard, false);
        }
        return result;
    }

    @Override
    public List<StrippedLeaderboardDTO> getLeaderboardsByEvent(RegattaDTO regatta) {
        List<StrippedLeaderboardDTO> results = new ArrayList<StrippedLeaderboardDTO>();
        for (RaceDTO race : regatta.races) {
            List<StrippedLeaderboardDTO> leaderboard = getLeaderboardsByRace(race);
            if (leaderboard != null && !leaderboard.isEmpty()) {
                results.addAll(leaderboard);
            }
        }
        // Removing duplicates
        HashSet<StrippedLeaderboardDTO> set = new HashSet<StrippedLeaderboardDTO>(results);
        results.clear();
        results.addAll(set);
        return results;
    }

    @Override
    public List<StrippedLeaderboardDTO> getLeaderboardsByRace(RaceDTO race) {
        List<StrippedLeaderboardDTO> results = new ArrayList<StrippedLeaderboardDTO>();
        Map<String, Leaderboard> leaderboards = getService().getLeaderboards();
        for (Leaderboard leaderboard : leaderboards.values()) {
            Iterable<RaceColumn> races = leaderboard.getRaceColumns();
            for (RaceColumn raceInLeaderboard : races) {
                for (Fleet fleet : raceInLeaderboard.getFleets()) {
                    TrackedRace trackedRace = raceInLeaderboard.getTrackedRace(fleet);
                    if (trackedRace != null) {
                        RaceDefinition trackedRaceDef = trackedRace.getRace();
                        if (trackedRaceDef.getName().equals(race.name)) {
                            results.add(createStrippedLeaderboardDTO(leaderboard, false));
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Creates a {@link LeaderboardDTO} for <code>leaderboard</code> and fills in the name, race master data
     * in the form of {@link RaceColumnDTO}s, whether or not there are {@link LeaderboardDTO#hasCarriedPoints carried points}
     * and the {@link LeaderboardDTO#discardThresholds discarding thresholds} for the leaderboard. No data about the points
     * is filled into the result object. No data about the competitor display names is filled in; instead, an empty map
     * is used for {@link LeaderboardDTO#competitorDisplayNames}.<br />
     * If <code>withGeoLocationData</code> is <code>true</code> the geographical location of all races will be determined.
     */
    private StrippedLeaderboardDTO createStrippedLeaderboardDTO(Leaderboard leaderboard, boolean withGeoLocationData) {
        StrippedLeaderboardDTO leaderboardDTO = new StrippedLeaderboardDTO();
        TimePoint startOfLatestRace = null;
        TimePoint now = MillisecondsTimePoint.now();
        Long delayToLiveInMillisForLatestRace = null;
        leaderboardDTO.name = leaderboard.getName();
        leaderboardDTO.displayName = leaderboard.getDisplayName();
        leaderboardDTO.competitorDisplayNames = new HashMap<CompetitorDTO, String>();
        leaderboardDTO.isMetaLeaderboard = leaderboard instanceof MetaLeaderboard ? true : false;
        if (leaderboard instanceof RegattaLeaderboard) {
            RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
            Regatta regatta = regattaLeaderboard.getRegatta();
            leaderboardDTO.regattaName = regatta.getName(); 
            leaderboardDTO.isRegattaLeaderboard = true;
            leaderboardDTO.scoringScheme = regatta.getScoringScheme().getType();
        } else {
            leaderboardDTO.isRegattaLeaderboard = false;
            leaderboardDTO.scoringScheme = leaderboard.getScoringScheme().getType();
        }
        if (leaderboard.getDefaultCourseArea() != null) {
            leaderboardDTO.defaultCourseAreaIdAsString = leaderboard.getDefaultCourseArea().getId().toString();
            leaderboardDTO.defaultCourseAreaName = leaderboard.getDefaultCourseArea().getName();
        }
        leaderboardDTO.setDelayToLiveInMillisForLatestRace(delayToLiveInMillisForLatestRace);
        leaderboardDTO.hasCarriedPoints = leaderboard.hasCarriedPoints();
        leaderboardDTO.discardThresholds = leaderboard.getResultDiscardingRule().getDiscardIndexResultsStartingWithHowManyRaces();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                TimePoint latestTimePointAfterQueryTimePointWhenATrackedRaceWasLive = null;
                RaceDTO raceDTO = null;
                RegattaAndRaceIdentifier raceIdentifier = null;
                TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                if (trackedRace != null) {
                    if (startOfLatestRace == null || (trackedRace.getStartOfRace() != null && trackedRace.getStartOfRace().compareTo(startOfLatestRace) > 0)) {
                        delayToLiveInMillisForLatestRace = trackedRace.getDelayToLiveInMillis();
                    }
                    raceIdentifier = new RegattaNameAndRaceName(trackedRace.getTrackedRegatta().getRegatta().getName(), trackedRace.getRace().getName());
                    raceDTO = baseDomainFactory.createRaceDTO(getService(), withGeoLocationData, raceIdentifier, trackedRace);
                    if (trackedRace.hasStarted(now) && trackedRace.hasGPSData() && trackedRace.hasWindData()) {
                        TimePoint liveTimePointForTrackedRace = now;
                        final TimePoint endOfRace = trackedRace.getEndOfRace();
                        if (endOfRace != null) {
                            liveTimePointForTrackedRace = endOfRace;
                        }
                        latestTimePointAfterQueryTimePointWhenATrackedRaceWasLive = liveTimePointForTrackedRace;
                    }
                }    
                final FleetDTO fleetDTO = baseDomainFactory.convertToFleetDTO(raceColumn, fleet);
                RaceColumnDTO raceColumnDTO = leaderboardDTO.addRace(raceColumn.getName(), raceColumn.getExplicitFactor(), raceColumn.getFactor(),
                        fleetDTO, raceColumn.isMedalRace(), raceIdentifier, raceDTO);
                if (latestTimePointAfterQueryTimePointWhenATrackedRaceWasLive != null) {
                    raceColumnDTO.setWhenLastTrackedRaceWasLive(fleetDTO, latestTimePointAfterQueryTimePointWhenATrackedRaceWasLive.asDate());
                }
            }
        }
        return leaderboardDTO;
    }

    @Override
    public StrippedLeaderboardDTO updateLeaderboard(String leaderboardName, String newLeaderboardName, String newLeaderboardDisplayName, int[] newDiscardingThresholds, String newCourseAreaIdAsString) {
        UUID newCourseAreaUuid = convertIdentifierStringToUuid(newCourseAreaIdAsString);
        Leaderboard updatedLeaderboard = getService().apply(new UpdateLeaderboard(leaderboardName, newLeaderboardName, newLeaderboardDisplayName, newDiscardingThresholds, newCourseAreaUuid));
        return createStrippedLeaderboardDTO(updatedLeaderboard, false);
    }

    @Override
    public void removeLeaderboard(String leaderboardName) {
        getService().apply(new RemoveLeaderboard(leaderboardName));
    }

    @Override
    public void renameLeaderboard(String leaderboardName, String newLeaderboardName) {
        getService().apply(new RenameLeaderboard(leaderboardName, newLeaderboardName));
    }

    @Override
    public void addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace) {
        getService().apply(new AddColumnToLeaderboard(columnName, leaderboardName, medalRace));
    }

    @Override
    public void addColumnsToLeaderboard(String leaderboardName, List<Pair<String, Boolean>> columnsToAdd) {
        for(Pair<String, Boolean> columnToAdd: columnsToAdd) {
            getService().apply(new AddColumnToLeaderboard(columnToAdd.getA(), leaderboardName, columnToAdd.getB()));
        }
    }

    @Override
    public void removeLeaderboardColumns(String leaderboardName, List<String> columnsToRemove) {
        for (String columnToRemove : columnsToRemove) {
            getService().apply(new RemoveLeaderboardColumn(columnToRemove, leaderboardName));
        }
    }

    @Override
    public void removeLeaderboardColumn(String leaderboardName, String columnName) {
        getService().apply(new RemoveLeaderboardColumn(columnName, leaderboardName));
    }

    @Override
    public void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName) {
        getService().apply(new RenameLeaderboardColumn(leaderboardName, oldColumnName, newColumnName));
    }

    @Override
    public void updateLeaderboardColumnFactor(String leaderboardName, String columnName, Double newFactor) {
        getService().apply(new UpdateLeaderboardColumnFactor(leaderboardName, columnName, newFactor));
    }

    @Override
    public void suppressCompetitorInLeaderboard(String leaderboardName, String competitorIdAsString, boolean suppressed) {
        getService().apply(new SetSuppressedFlagForCompetitorInLeaderboard(leaderboardName, competitorIdAsString, suppressed));
    }

    @Override
    public boolean connectTrackedRaceToLeaderboardColumn(String leaderboardName, String raceColumnName,
            String fleetName, RegattaAndRaceIdentifier raceIdentifier) {
        return getService().apply(new ConnectTrackedRaceToLeaderboardColumn(leaderboardName, raceColumnName, fleetName, raceIdentifier));
    }

    @Override
    public Map<String, RegattaAndRaceIdentifier> getRegattaAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(String leaderboardName, String raceColumnName) {
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
    public void disconnectLeaderboardColumnFromTrackedRace(String leaderboardName, String raceColumnName, String fleetName) {
        getService().apply(new DisconnectLeaderboardColumnFromTrackedRace(leaderboardName, raceColumnName, fleetName));
    }

    @Override
    public void updateLeaderboardCarryValue(String leaderboardName, String competitorIdAsString, Double carriedPoints) {
        getService().apply(new UpdateLeaderboardCarryValue(leaderboardName, competitorIdAsString, carriedPoints));
    }

    @Override
    public Triple<Double, Double, Boolean> updateLeaderboardMaxPointsReason(String leaderboardName, String competitorIdAsString, String raceColumnName,
            MaxPointsReason maxPointsReason, Date date) throws NoWindException {
        return getService().apply(
                new UpdateLeaderboardMaxPointsReason(leaderboardName, raceColumnName, competitorIdAsString,
                        maxPointsReason, new MillisecondsTimePoint(date)));
    }

    @Override
    public Triple<Double, Double, Boolean> updateLeaderboardScoreCorrection(String leaderboardName,
            String competitorIdAsString, String columnName, Double correctedScore, Date date) throws NoWindException {
        return getService().apply(
                new UpdateLeaderboardScoreCorrection(leaderboardName, columnName, competitorIdAsString, correctedScore,
                        new MillisecondsTimePoint(date)));
    }

    @Override
    public Void updateLeaderboardScoreCorrectionMetadata(String leaderboardName, Date timePointOfLastCorrectionValidity, String comment) {
        return getService().apply(
                new UpdateLeaderboardScoreCorrectionMetadata(leaderboardName,
                        timePointOfLastCorrectionValidity == null ? null : new MillisecondsTimePoint(timePointOfLastCorrectionValidity),
                                comment));
    }

    @Override
    public void updateLeaderboardScoreCorrectionsAndMaxPointsReasons(BulkScoreCorrectionDTO updates) throws NoWindException {
        Date dateForResults = new Date(); // we don't care about the result date/time here; use current date as default
        for (Map.Entry<String, Map<String, Double>> e : updates.getScoreUpdatesForRaceColumnByCompetitorIdAsString().entrySet()) {
            for (Map.Entry<String, Double> raceColumnNameAndCorrectedScore : e.getValue().entrySet()) {
                updateLeaderboardScoreCorrection(updates.getLeaderboardName(), e.getKey(),
                        raceColumnNameAndCorrectedScore.getKey(), raceColumnNameAndCorrectedScore.getValue(), dateForResults);
            }
        }
        for (Map.Entry<String, Map<String, MaxPointsReason>> e : updates.getMaxPointsUpdatesForRaceColumnByCompetitorIdAsString().entrySet()) {
            for (Map.Entry<String, MaxPointsReason> raceColumnNameAndNewMaxPointsReason : e.getValue().entrySet()) {
                updateLeaderboardMaxPointsReason(updates.getLeaderboardName(), e.getKey(),
                        raceColumnNameAndNewMaxPointsReason.getKey(), raceColumnNameAndNewMaxPointsReason.getValue(), dateForResults);
            }
        }
    }

    @Override
    public void updateCompetitorDisplayNameInLeaderboard(String leaderboardName, String competitorIdAsString, String displayName) {
        getService().apply(new UpdateCompetitorDisplayNameInLeaderboard(leaderboardName, competitorIdAsString, displayName));
    }

    @Override
    public void moveLeaderboardColumnUp(String leaderboardName, String columnName) {
        getService().apply(new MoveLeaderboardColumnUp(leaderboardName, columnName));
    }

    @Override
    public void moveLeaderboardColumnDown(String leaderboardName, String columnName) {
        getService().apply(new MoveLeaderboardColumnDown(leaderboardName, columnName));
    }

    @Override
    public void updateIsMedalRace(String leaderboardName, String columnName, boolean isMedalRace) {
        getService().apply(new UpdateIsMedalRace(leaderboardName, columnName, isMedalRace));
    }

    @Override
    public void updateRaceDelayToLive(RegattaAndRaceIdentifier regattaAndRaceIdentifier, long delayToLiveInMs) {
        getService().apply(new UpdateRaceDelayToLive(regattaAndRaceIdentifier, delayToLiveInMs));
    }

    @Override
    public void updateRacesDelayToLive(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers, long delayToLiveInMs) {
        for (RegattaAndRaceIdentifier regattaAndRaceIdentifier : regattaAndRaceIdentifiers) {
            getService().apply(new UpdateRaceDelayToLive(regattaAndRaceIdentifier, delayToLiveInMs));
        }
    }

    @Override
    public List<SwissTimingConfigurationDTO> getPreviousSwissTimingConfigurations() {
        Iterable<SwissTimingConfiguration> configs = swissTimingAdapterPersistence.getSwissTimingConfigurations();
        List<SwissTimingConfigurationDTO> result = new ArrayList<SwissTimingConfigurationDTO>();
        for (SwissTimingConfiguration stConfig : configs) {
            result.add(new SwissTimingConfigurationDTO(stConfig.getName(), stConfig.getHostname(), stConfig.getPort(), stConfig.canSendRequests()));
        }
        return result;
    }

    @Override
    public List<SwissTimingRaceRecordDTO> listSwissTimingRaces(String hostname, int port, boolean canSendRequests) 
            throws UnknownHostException, IOException, InterruptedException, ParseException {
        List<SwissTimingRaceRecordDTO> result = new ArrayList<SwissTimingRaceRecordDTO>();
        for (com.sap.sailing.domain.swisstimingadapter.RaceRecord rr : getService().getSwissTimingRaceRecords(hostname, port, canSendRequests)) {
            SwissTimingRaceRecordDTO swissTimingRaceRecordDTO = new SwissTimingRaceRecordDTO(rr.getRaceID(), rr.getDescription(), rr.getStartTime());
            BoatClass boatClass = com.sap.sailing.domain.swisstimingadapter.DomainFactory.INSTANCE.getOrCreateBoatClassFromRaceID(rr.getRaceID());
            swissTimingRaceRecordDTO.boatClass = boatClass != null ? boatClass.getName() : null;
            swissTimingRaceRecordDTO.discipline = rr.getRaceID().length() >= 3 ? rr.getRaceID().substring(2, 3) : null;
            result.add(swissTimingRaceRecordDTO);
        }
        return result;
    }

    @Override
    public void storeSwissTimingConfiguration(String configName, String hostname, int port, boolean canSendRequests) {
        swissTimingAdapterPersistence.storeSwissTimingConfiguration(swissTimingFactory.createSwissTimingConfiguration(configName, hostname, port, canSendRequests));
    }

    @Override
    public void trackWithSwissTiming(RegattaIdentifier regattaToAddTo, Iterable<SwissTimingRaceRecordDTO> rrs, String hostname, int port,
            boolean canSendRequests, boolean trackWind, final boolean correctWindByDeclination) throws Exception {
        logger.info("tracWithSwissTiming for regatta " + regattaToAddTo + " for race records " + rrs
                + " with hostname " + hostname + " and port " + port + " and canSendRequests=" + canSendRequests);
        for (SwissTimingRaceRecordDTO rr : rrs) {
            final RacesHandle raceHandle = getService().addSwissTimingRace(regattaToAddTo, rr.ID, hostname, port,
                    canSendRequests,
                    MongoWindStoreFactory.INSTANCE.getMongoWindStore(mongoObjectFactory, domainObjectFactory),
                    MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory),
                    RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS);
            if (trackWind) {
                new Thread("Wind tracking starter for race " + rr.ID + "/" + rr.description) {
                    public void run() {
                        try {
                            startTrackingWind(raceHandle, correctWindByDeclination,
                                    RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }.start();
            }
        }
    }

    @Override
    public void sendSwissTimingDummyRace(String racMessage, String stlMesssage, String ccgMessage) {
        getService().storeSwissTimingDummyRace(racMessage,stlMesssage,ccgMessage);
    }

    @Override
    public List<SwissTimingReplayRaceDTO> listSwissTiminigReplayRaces(String swissTimingUrl) {
        List<SwissTimingReplayRace> replayRaces = SwissTimingReplayServiceFactory.INSTANCE
                .createSwissTimingReplayService().listReplayRaces(swissTimingUrl);
        List<SwissTimingReplayRaceDTO> result = new ArrayList<SwissTimingReplayRaceDTO>(replayRaces.size()); 
        for (SwissTimingReplayRace replayRace : replayRaces) {
            result.add(new SwissTimingReplayRaceDTO(replayRace.getFlightNumber(), replayRace.getRaceId(), replayRace.getRsc(), replayRace.getName(), replayRace.getBoatClass(), replayRace.getStartTime(), replayRace.getLink()));
        }
        return result;
    }

    @Override
    public void replaySwissTimingRace(RegattaIdentifier regattaIdentifier, Iterable<SwissTimingReplayRaceDTO> replayRaceDTOs,
            boolean trackWind, boolean correctWindByDeclination, boolean simulateWithStartTimeNow) {
        logger.info("replaySwissTimingRace for regatta "+regattaIdentifier+" for races "+replayRaceDTOs);
        Regatta regatta;
        for (SwissTimingReplayRaceDTO replayRaceDTO : replayRaceDTOs) {
            if (regattaIdentifier == null) {
                String boatClass = replayRaceDTO.boat_class;
                for (String genderIndicator : new String[] { "Man", "Woman", "Men", "Women", "M", "W" }) {
                    Pattern p = Pattern.compile("(( - )|-| )" + genderIndicator + "$");
                    Matcher m = p.matcher(boatClass.trim());
                    if (m.find()) {
                        boatClass = boatClass.trim().substring(0, m.start(1));
                        break;
                    }
                }
                regatta = getService().createRegatta(
                        replayRaceDTO.rsc,
                        boatClass.trim(),
                        RegattaImpl.getDefaultName(replayRaceDTO.rsc, replayRaceDTO.boat_class),
                        Collections.singletonList(new SeriesImpl(
                                LeaderboardNameConstants.DEFAULT_SERIES_NAME, 
                                /* isMedal */false, 
                                Collections.singletonList(new FleetImpl(LeaderboardNameConstants.DEFAULT_FLEET_NAME)), 
                                /* race column names */ new ArrayList<String>(), getService())), 
                                false,
                                baseDomainFactory.createScoringScheme(ScoringSchemeType.LOW_POINT), null);
                //TODO: is course area relevant for swiss timing replay?
            } else {
                regatta = getService().getRegatta(regattaIdentifier);
            }
            SwissTimingReplayService replayService = SwissTimingReplayServiceFactory.INSTANCE
                    .createSwissTimingReplayService();
            replayService.loadRaceData(replayRaceDTO.link, getService().getSwissTimingDomainFactory(), regatta,
                    getService());
        }
    }

    @Override
    public String[] getCountryCodes() {
        List<String> countryCodes = new ArrayList<String>();
        for (CountryCode cc : countryCodeFactory.getAll()) {
            if (cc.getThreeLetterIOCCode() != null && !cc.getThreeLetterIOCCode().equals("")) {
                countryCodes.add(cc.getThreeLetterIOCCode());
            }
        }
        Collections.sort(countryCodes);
        return countryCodes.toArray(new String[0]);
    }

    /**
     * Finds a competitor in a sequence of competitors that has an {@link Competitor#getId()} equal to <code>id</code>. 
     */
    private Competitor getCompetitorById(Iterable<Competitor> competitors, String id) {
        for (Competitor c : competitors) {
            if (c.getId().toString().equals(id)) {
                return c;
            }
        }
        return null;
    }

    private Double getCompetitorRaceDataEntry(DetailType dataType, TrackedRace trackedRace, Competitor competitor,
            TimePoint timePoint, String leaderboardGroupName, String leaderboardName) throws NoWindException {
        Double result = null;
        Course course = trackedRace.getRace().getCourse();
        course.lockForRead(); // make sure the tracked leg survives this call even if a course update is pending
        try {
            TrackedLegOfCompetitor trackedLeg = trackedRace.getTrackedLeg(competitor, timePoint);
            if (trackedLeg != null) {
                switch (dataType) {
                case CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
                    SpeedWithBearing speedOverGround = trackedLeg.getSpeedOverGround(timePoint);
                    result = (speedOverGround == null) ? null : speedOverGround.getKnots();
                    break;
                case VELOCITY_MADE_GOOD_IN_KNOTS:
                    Speed velocityMadeGood = trackedLeg.getVelocityMadeGood(timePoint);
                    result = (velocityMadeGood == null) ? null : velocityMadeGood.getKnots();
                    break;
                case DISTANCE_TRAVELED:
                    Distance distanceTraveled = trackedRace.getDistanceTraveled(competitor, timePoint);
                    result = distanceTraveled == null ? null : distanceTraveled.getMeters();
                    break;
                case GAP_TO_LEADER_IN_SECONDS:
                    result = trackedLeg.getGapToLeaderInSeconds(timePoint);
                    break;
                case WINDWARD_DISTANCE_TO_OVERALL_LEADER:
                    Distance distanceToLeader = trackedLeg.getWindwardDistanceToOverallLeader(timePoint);
                    result = (distanceToLeader == null) ? null : distanceToLeader.getMeters();
                    break;
                case RACE_RANK:
                    result = (double) trackedLeg.getRank(timePoint);
                    break;
                case REGATTA_RANK:
                    if (leaderboardName == null || leaderboardName.isEmpty()) {
                        break;
                    }

                    Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
                    result = leaderboard == null ? null : (double) leaderboard.getTotalRankOfCompetitor(competitor, timePoint);
                    break;
                case OVERALL_RANK:
                    if (leaderboardGroupName == null || leaderboardGroupName.isEmpty()) {
                        break;
                    }

                    LeaderboardGroup group = getService().getLeaderboardGroupByName(leaderboardGroupName);
                    Leaderboard overall = group.getOverallLeaderboard();
                    result = overall == null ? null : (double) overall.getTotalRankOfCompetitor(competitor, timePoint);
                    break;
                default:
                    throw new UnsupportedOperationException("Theres currently no support for the enum value '"
                            + dataType + "' in this method.");
                }
            }
            return result;
        } finally {
            course.unlockAfterRead();
        }
    }

    @Override
    public CompetitorsRaceDataDTO getCompetitorsRaceData(RegattaAndRaceIdentifier race, List<CompetitorDTO> competitors, Date from, Date to,
            final long stepSizeInMs, final DetailType detailType, final String leaderboardGroupName, final String leaderboardName) throws NoWindException {
        CompetitorsRaceDataDTO result = null;
        final TrackedRace trackedRace = getExistingTrackedRace(race);
        if (trackedRace != null) {
            TimePoint newestEvent = trackedRace.getTimePointOfNewestEvent();
            final TimePoint startTime = from == null ? trackedRace.getStartOfTracking() : new MillisecondsTimePoint(from);
            final TimePoint endTime = (to == null || to.after(newestEvent.asDate())) ? newestEvent : new MillisecondsTimePoint(to);
            result = new CompetitorsRaceDataDTO(detailType, startTime==null?null:startTime.asDate(), endTime==null?null:endTime.asDate());

            Map<CompetitorDTO, FutureTask<CompetitorRaceDataDTO>> resultFutures = new HashMap<CompetitorDTO, FutureTask<CompetitorRaceDataDTO>>();
            for (final CompetitorDTO competitorDTO : competitors) {
                FutureTask<CompetitorRaceDataDTO> future = new FutureTask<CompetitorRaceDataDTO>(new Callable<CompetitorRaceDataDTO>() {
                    @Override
                            public CompetitorRaceDataDTO call() throws NoWindException {
                                Competitor competitor = getCompetitorById(trackedRace.getRace().getCompetitors(),
                                        competitorDTO.idAsString);
                                ArrayList<Triple<String, Date, Double>> markPassingsData = new ArrayList<Triple<String, Date, Double>>();
                                ArrayList<Pair<Date, Double>> raceData = new ArrayList<Pair<Date, Double>>();
                                // Filling the mark passings
                                Set<MarkPassing> competitorMarkPassings = trackedRace.getMarkPassings(competitor);
                                if (competitorMarkPassings != null) {
                                    trackedRace.lockForRead(competitorMarkPassings);
                                    try {
                                        for (MarkPassing markPassing : competitorMarkPassings) {
                                            MillisecondsTimePoint time = new MillisecondsTimePoint(markPassing.getTimePoint().asMillis());
                                            Double competitorMarkPassingsData = getCompetitorRaceDataEntry(detailType,
                                                    trackedRace, competitor, time, leaderboardGroupName, leaderboardName);
                                            if (competitorMarkPassingsData != null) {
                                                markPassingsData.add(new Triple<String, Date, Double>(markPassing
                                                        .getWaypoint().getName(), time.asDate(), competitorMarkPassingsData));
                                            }
                                        }
                                    } finally {
                                        trackedRace.unlockAfterRead(competitorMarkPassings);
                                    }
                                }
                                if (startTime != null && endTime != null) {
                                    for (long i = startTime.asMillis(); i <= endTime.asMillis(); i += stepSizeInMs) {
                                        MillisecondsTimePoint time = new MillisecondsTimePoint(i);
                                        Double competitorRaceData = getCompetitorRaceDataEntry(detailType, trackedRace,
                                                competitor, time, leaderboardGroupName, leaderboardName);
                                        if (competitorRaceData != null) {
                                            raceData.add(new Pair<Date, Double>(time.asDate(), competitorRaceData));
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
                    logger.log(Level.SEVERE, "Exception while trying to compute competitor data "+detailType+" for competitor "+e.getKey().name, e1);
                } catch (ExecutionException e1) {
                    competitorData = null;
                    logger.log(Level.SEVERE, "Exception while trying to compute competitor data "+detailType+" for competitor "+e.getKey().name, e1);
                }
                result.setCompetitorData(e.getKey(), competitorData);
            }
        }
        return result;
    }

    @Override
    public Map<CompetitorDTO, List<GPSFixDTO>> getDouglasPoints(RegattaAndRaceIdentifier raceIdentifier,
            Map<CompetitorDTO, Date> from, Map<CompetitorDTO, Date> to,
            double meters) throws NoWindException {
        Map<CompetitorDTO, List<GPSFixDTO>> result = new HashMap<CompetitorDTO, List<GPSFixDTO>>();
        TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            final WindSource windSource = new WindSourceImpl(WindSourceType.COMBINED);
            MeterDistance maxDistance = new MeterDistance(meters);
            for (Competitor competitor : trackedRace.getRace().getCompetitors()) {
                CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
                if (from.containsKey(competitorDTO)) {
                    // get Track of competitor
                    GPSFixTrack<Competitor, GPSFixMoving> gpsFixTrack = trackedRace.getTrack(competitor);
                    // Distance for DouglasPeucker
                    TimePoint timePointFrom = new MillisecondsTimePoint(from.get(competitorDTO));
                    TimePoint timePointTo = new MillisecondsTimePoint(to.get(competitorDTO));
                    List<GPSFixMoving> gpsFixApproximation = trackedRace.approximate(competitor, maxDistance,
                            timePointFrom, timePointTo);
                    List<GPSFixDTO> gpsFixDouglasList = new ArrayList<GPSFixDTO>();
                    for (int i = 0; i < gpsFixApproximation.size(); i++) {
                        GPSFix fix = gpsFixApproximation.get(i);
                        SpeedWithBearing speedWithBearing;
                        if (i < gpsFixApproximation.size() - 1) {
                            GPSFix next = gpsFixApproximation.get(i + 1);
                            Bearing bearing = fix.getPosition().getBearingGreatCircle(next.getPosition());
                            Speed speed = fix.getPosition().getDistance(next.getPosition())
                                    .inTime(next.getTimePoint().asMillis() - fix.getTimePoint().asMillis());
                            speedWithBearing = new KnotSpeedWithBearingImpl(speed.getKnots(), bearing);
                        } else {
                            speedWithBearing = gpsFixTrack.getEstimatedSpeed(fix.getTimePoint());
                        }
                        Tack tack = trackedRace.getTack(competitor, fix.getTimePoint());
                        TrackedLegOfCompetitor trackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor, fix.getTimePoint());
                        LegType legType = trackedLegOfCompetitor == null ? null : trackedRace.getTrackedLeg(
                                trackedLegOfCompetitor.getLeg()).getLegType(fix.getTimePoint());
                        Wind wind = trackedRace.getWind(fix.getPosition(), fix.getTimePoint());
                        WindDTO windDTO = createWindDTOFromAlreadyAveraged(wind, trackedRace.getOrCreateWindTrack(windSource), fix.getTimePoint());
                        GPSFixDTO fixDTO = createGPSFixDTO(fix, speedWithBearing,  windDTO, tack, legType, /* extrapolated */false);
                        gpsFixDouglasList.add(fixDTO);
                    }
                    result.put(competitorDTO, gpsFixDouglasList);
                }
            }
        }
        return result;
    }

    @Override
    public Map<CompetitorDTO, List<ManeuverDTO>> getManeuvers(RegattaAndRaceIdentifier raceIdentifier,
            Map<CompetitorDTO, Date> from, Map<CompetitorDTO, Date> to) throws NoWindException {
        Map<CompetitorDTO, List<ManeuverDTO>> result = new HashMap<CompetitorDTO, List<ManeuverDTO>>();
        final TrackedRace trackedRace = getExistingTrackedRace(raceIdentifier);
        if (trackedRace != null) {
            Map<CompetitorDTO, Future<List<ManeuverDTO>>> futures = new HashMap<CompetitorDTO, Future<List<ManeuverDTO>>>();
            for (final Competitor competitor : trackedRace.getRace().getCompetitors()) {
                CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
                if (from.containsKey(competitorDTO)) {
                    final TimePoint timePointFrom = new MillisecondsTimePoint(from.get(competitorDTO));
                    final TimePoint timePointTo = new MillisecondsTimePoint(to.get(competitorDTO));
                    RunnableFuture<List<ManeuverDTO>> future = new FutureTask<List<ManeuverDTO>>(
                            new Callable<List<ManeuverDTO>>() {
                                @Override
                                public List<ManeuverDTO> call() {
                                    List<Maneuver> maneuversForCompetitor;
                                    try {
                                        maneuversForCompetitor = trackedRace.getManeuvers(competitor, timePointFrom,
                                                timePointTo, /* waitForLatest */ true);
                                    } catch (NoWindException e) {
                                        throw new NoWindError(e);
                                    }
                                    return createManeuverDTOsForCompetitor(maneuversForCompetitor, trackedRace,
                                            competitor);
                                }
                            });
                    executor.execute(future);
                    futures.put(competitorDTO, future);
                }
            }
            for (Map.Entry<CompetitorDTO, Future<List<ManeuverDTO>>> competitorAndFuture : futures.entrySet()) {
                try {
                    result.put(competitorAndFuture.getKey(), competitorAndFuture.getValue().get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    private List<ManeuverDTO> createManeuverDTOsForCompetitor(List<Maneuver> maneuvers, TrackedRace trackedRace, Competitor competitor) {
        List<ManeuverDTO> result = new ArrayList<ManeuverDTO>();
        for (Maneuver maneuver : maneuvers) {
            final ManeuverDTO maneuverDTO;
            if (maneuver.getType() == ManeuverType.MARK_PASSING) {
                maneuverDTO = new MarkpassingManeuverDTO(maneuver.getType(), maneuver.getNewTack(),
                        new PositionDTO(maneuver.getPosition().getLatDeg(), maneuver.getPosition().getLngDeg()), 
                        maneuver.getTimePoint().asDate(),
                        createSpeedWithBearingDTO(maneuver.getSpeedWithBearingBefore()),
                        createSpeedWithBearingDTO(maneuver.getSpeedWithBearingAfter()),
                        maneuver.getDirectionChangeInDegrees(), maneuver.getManeuverLoss()==null?null:maneuver.getManeuverLoss().getMeters(),
                                ((MarkPassingManeuver) maneuver).getSide());
            } else  {
                maneuverDTO = new ManeuverDTO(maneuver.getType(), maneuver.getNewTack(),
                        new PositionDTO(maneuver.getPosition().getLatDeg(), maneuver.getPosition().getLngDeg()), 
                        maneuver.getTimePoint().asDate(),
                        createSpeedWithBearingDTO(maneuver.getSpeedWithBearingBefore()),
                        createSpeedWithBearingDTO(maneuver.getSpeedWithBearingAfter()),
                        maneuver.getDirectionChangeInDegrees(), maneuver.getManeuverLoss()==null?null:maneuver.getManeuverLoss().getMeters());
            }
            result.add(maneuverDTO);
        }
        return result;
    }

    @Override
    public RaceDefinition getRace(RegattaAndRaceIdentifier raceIdentifier) {
        Regatta regatta = getService().getRegattaByName(raceIdentifier.getRegattaName());
        RaceDefinition race = getRaceByName(regatta, raceIdentifier.getRaceName());
        return race;
    }

    @Override
    public TrackedRace getTrackedRace(RegattaAndRaceIdentifier regattaNameAndRaceName) {
        Regatta regatta = getService().getRegattaByName(regattaNameAndRaceName.getRegattaName());
        RaceDefinition race = getRaceByName(regatta, regattaNameAndRaceName.getRaceName());
        TrackedRace trackedRace = getService().getOrCreateTrackedRegatta(regatta).getTrackedRace(race);
        return trackedRace;
    }

    @Override
    public TrackedRace getExistingTrackedRace(RegattaAndRaceIdentifier regattaNameAndRaceName) {
        return getService().getExistingTrackedRace(regattaNameAndRaceName);
    }

    @Override
    public Regatta getRegatta(RegattaName regattaIdentifier) {
        return getService().getRegattaByName(regattaIdentifier.getRegattaName());
    }

    /**
     * Returns a servlet context that, when asked for a resource, first tries the original servlet context's implementation. If that
     * fails, it prepends "war/" to the request because the war/ folder contains all the resources exposed externally
     * through the HTTP server.
     */
    @Override
    public ServletContext getServletContext() {
        return new DelegatingServletContext(super.getServletContext());
    }

    @Override
    /**
     * Override of function to prevent exception "Blocked request without GWT permutation header (XSRF attack?)" when testing the GWT sites
     */
    protected void checkPermutationStrongName() throws SecurityException {
        //Override to prevent exception "Blocked request without GWT permutation header (XSRF attack?)" when testing the GWT sites
        return;
    }

    @Override
    public List<LeaderboardGroupDTO> getLeaderboardGroups(boolean withGeoLocationData) {
        ArrayList<LeaderboardGroupDTO> leaderboardGroupDTOs = new ArrayList<LeaderboardGroupDTO>();
        Map<String, LeaderboardGroup> leaderboardGroups = getService().getLeaderboardGroups();

        for (LeaderboardGroup leaderboardGroup : leaderboardGroups.values()) {
            leaderboardGroupDTOs.add(convertToLeaderboardGroupDTO(leaderboardGroup, withGeoLocationData));
        }

        return leaderboardGroupDTOs;
    }

    @Override
    public LeaderboardGroupDTO getLeaderboardGroupByName(String groupName, boolean withGeoLocationData) {
        return convertToLeaderboardGroupDTO(getService().getLeaderboardGroupByName(groupName), withGeoLocationData);
    }

    private LeaderboardGroupDTO convertToLeaderboardGroupDTO(LeaderboardGroup leaderboardGroup, boolean withGeoLocationData) {
        LeaderboardGroupDTO groupDTO = new LeaderboardGroupDTO();
        groupDTO.name = leaderboardGroup.getName();
        groupDTO.description = leaderboardGroup.getDescription();
        groupDTO.displayLeaderboardsInReverseOrder = leaderboardGroup.isDisplayGroupsInReverseOrder();
        for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
            groupDTO.leaderboards.add(createStrippedLeaderboardDTO(leaderboard, withGeoLocationData));
        }
        Leaderboard overallLeaderboard = leaderboardGroup.getOverallLeaderboard();
        if (overallLeaderboard != null) {
            groupDTO.setOverallLeaderboardDiscardThresholds(overallLeaderboard.getResultDiscardingRule().getDiscardIndexResultsStartingWithHowManyRaces());
            groupDTO.setOverallLeaderboardScoringSchemeType(overallLeaderboard.getScoringScheme().getType());
        }
        return groupDTO;
    }


    @Override
    public void renameLeaderboardGroup(String oldName, String newName) {
        getService().apply(new RenameLeaderboardGroup(oldName, newName));
    }

    @Override
    public void removeLeaderboardGroup(String groupName) {
        getService().apply(new RemoveLeaderboardGroup(groupName));
    }

    @Override
    public LeaderboardGroupDTO createLeaderboardGroup(String groupName, String description, boolean displayGroupsInReverseOrder,
            int[] overallLeaderboardDiscardThresholds, ScoringSchemeType overallLeaderboardScoringSchemeType) {
        CreateLeaderboardGroup createLeaderboardGroupOp = new CreateLeaderboardGroup(groupName, description, displayGroupsInReverseOrder,
                new ArrayList<String>(), overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType);
        return convertToLeaderboardGroupDTO(getService().apply(createLeaderboardGroupOp), false);
    }

    @Override
    public void updateLeaderboardGroup(String oldName, String newName, String description, List<String> leaderboardNames,
            int[] overallLeaderboardDiscardThresholds, ScoringSchemeType overallLeaderboardScoringSchemeType) {
        getService().apply(
                new UpdateLeaderboardGroup(oldName, newName, description, leaderboardNames,
                        overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType));
    }

    @Override
    public ReplicationStateDTO getReplicaInfo() {
        ReplicationService service = getReplicationService();
        Set<ReplicaDTO> replicaDTOs = new HashSet<ReplicaDTO>();
        for (ReplicaDescriptor replicaDescriptor : service.getReplicaInfo()) {
            final Map<Class<? extends RacingEventServiceOperation<?>>, Integer> statistics = service.getStatistics(replicaDescriptor);
            Map<String, Integer> replicationCountByOperationClassName = new HashMap<String, Integer>();
            for (Map.Entry<Class<? extends RacingEventServiceOperation<?>>, Integer> e : statistics.entrySet()) {
                replicationCountByOperationClassName.put(e.getKey().getName(), e.getValue());
            }
            replicaDTOs.add(new ReplicaDTO(replicaDescriptor.getIpAddress().getHostName(), replicaDescriptor.getRegistrationTime().asDate(),
                    replicationCountByOperationClassName));
        }
        ReplicationMasterDTO master;
        ReplicationMasterDescriptor replicatingFromMaster = service.isReplicatingFromMaster();
        if (replicatingFromMaster == null) {
            master = null;
        } else {
            master = new ReplicationMasterDTO(replicatingFromMaster.getHostname(), replicatingFromMaster.getMessagingPort(),
                    replicatingFromMaster.getServletPort());
        }
        return new ReplicationStateDTO(master, replicaDTOs);
    }

    @Override
    public void startReplicatingFromMaster(String masterName, String exchangeName, int servletPort, int messagingPort) throws IOException, ClassNotFoundException, InterruptedException {
        getReplicationService().startToReplicateFrom(
                ReplicationFactory.INSTANCE.createReplicationMasterDescriptor(masterName, exchangeName, servletPort, messagingPort));
    }

    @Override
    public List<EventDTO> getEvents() {
        List<EventDTO> result = new ArrayList<EventDTO>();
        for (Event event : getService().getAllEvents()) {
            EventDTO eventDTO = convertToEventDTO(event);
            result.add(eventDTO);
        }
        return result;
    }

    @Override
    public void updateEvent(String eventName, String eventIdAsString, VenueDTO venue, String publicationUrl, boolean isPublic, List<String> regattaNames) {
        UUID eventUuid = convertIdentifierStringToUuid(eventIdAsString);
        getService().apply(new UpdateEvent(eventUuid, eventName, venue.name, publicationUrl, isPublic, regattaNames));
    }

    @Override
    public EventDTO createEvent(String eventName, String venue, String publicationUrl, boolean isPublic, List<String> courseAreaNames) {
        UUID eventUuid = UUID.randomUUID();
        getService().apply(new CreateEvent(eventName, venue, publicationUrl, isPublic, eventUuid, courseAreaNames));

        for (String courseAreaName : courseAreaNames) {
            createCourseArea(eventUuid.toString(), courseAreaName);
        }

        return getEventById(eventUuid);
    }

    @Override
    public CourseAreaDTO createCourseArea(String eventIdAsString, String courseAreaName) {
        UUID eventUuid = convertIdentifierStringToUuid(eventIdAsString);
        CourseArea courseArea = getService().apply(new AddCourseArea(eventUuid, courseAreaName, UUID.randomUUID()));
        return convertToCourseAreaDTO(courseArea);
    }

    @Override
    public void removeEvent(String eventIdAsString) {
        UUID eventUuid = convertIdentifierStringToUuid(eventIdAsString);
        getService().apply(new RemoveEvent(eventUuid));
    }

    @Override
    public void renameEvent(String eventIdAsString, String newName) {
        UUID eventUuid = convertIdentifierStringToUuid(eventIdAsString);
        getService().apply(new RenameEvent(eventUuid, newName));
    }

    @Override
    public EventDTO getEventByName(String eventName) {
        EventDTO result = null;
        for (Event event : getService().getAllEvents()) {
            if(event.getName().equals(eventName)) {
                result = convertToEventDTO(event);
                break;
            }
        }
        return result;
    }
    
    @Override
    public EventDTO getEventByIdAsString(String eventIdAsString) {
        UUID eventUuid = convertIdentifierStringToUuid(eventIdAsString);
        return getEventById(eventUuid);
    }

    @Override
    public EventDTO getEventById(Serializable id) {
        EventDTO result = null;
        Event event = getService().getEvent(id);
        if (event != null) {
            result = convertToEventDTO(event);
        }
        return result;
    }

    private EventDTO convertToEventDTO(Event event) {
        EventDTO eventDTO = new EventDTO(event.getName());
        eventDTO.venue = new VenueDTO();
        eventDTO.venue.name = event.getVenue() != null ? event.getVenue().getName() : null;
        eventDTO.publicationUrl = event.getPublicationUrl();
        eventDTO.isPublic = event.isPublic();
        eventDTO.id = event.getId().toString();
        eventDTO.regattas = new ArrayList<RegattaDTO>();
        for (Regatta regatta: event.getRegattas()) {
            RegattaDTO regattaDTO = new RegattaDTO();
            regattaDTO.name = regatta.getName();
            eventDTO.regattas.add(regattaDTO);
        }
        eventDTO.venue.setCourseAreas(new ArrayList<CourseAreaDTO>());
        for (CourseArea courseArea : event.getVenue().getCourseAreas()) {
            CourseAreaDTO courseAreaDTO = convertToCourseAreaDTO(courseArea);
            eventDTO.venue.getCourseAreas().add(courseAreaDTO);
        }
        return eventDTO;
    }

    private CourseAreaDTO convertToCourseAreaDTO(CourseArea courseArea) {
        CourseAreaDTO courseAreaDTO = new CourseAreaDTO(courseArea.getName());
        courseAreaDTO.id = courseArea.getId().toString();
        return courseAreaDTO;
    }
    
    @Override
    public List<RegattaOverviewEntryDTO> getRegattaOverviewEntriesForEvent(String eventIdAsString) {
        List<RegattaOverviewEntryDTO> result = new ArrayList<RegattaOverviewEntryDTO>();
        Event event = getService().getEvent(convertIdentifierStringToUuid(eventIdAsString));
        if (event != null) {
            for (CourseArea courseArea : event.getVenue().getCourseAreas()) {
                for (Leaderboard leaderboard : getService().getLeaderboards().values()) {
                    if (leaderboard.getDefaultCourseArea() != null && leaderboard.getDefaultCourseArea().equals(courseArea)) {
                        String regattaName = getRegattaNameFromLeaderboard(leaderboard);
                        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                            for (Fleet fleet : raceColumn.getFleets()) {
                                RegattaOverviewEntryDTO entry = new RegattaOverviewEntryDTO();
                                entry.courseAreaName = courseArea.getName();
                                entry.courseAreaIdAsString = courseArea.getId().toString();
                                entry.regattaName = regattaName;
                                entry.raceInfo = createRaceInfoDTO(raceColumn, fleet);
                                result.add(entry);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private String getRegattaNameFromLeaderboard(Leaderboard leaderboard) {
        String regattaName;
        if (leaderboard instanceof RegattaLeaderboard) {
            RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
            if (regattaLeaderboard.getDisplayName() != null) {
                regattaName = regattaLeaderboard.getDisplayName();
            } else {
                regattaName = regattaLeaderboard.getRegatta().getName();
            }
        } else {
            if (leaderboard.getDisplayName() != null) {
                regattaName = leaderboard.getDisplayName();
            } else {
                regattaName = leaderboard.getName();
            }
        }
        return regattaName;
    }
    
    @Override
    public void removeRegatta(RegattaIdentifier regattaIdentifier) {
        getService().apply(new RemoveRegatta(regattaIdentifier));
    }

    private RaceColumnInSeriesDTO convertToRaceColumnInSeriesDTO(RaceColumnInSeries raceColumnInSeries) {
        RaceColumnInSeriesDTO raceColumnInSeriesDTO = new RaceColumnInSeriesDTO(raceColumnInSeries.getSeries().getName(),
                raceColumnInSeries.getRegatta().getName());
        raceColumnInSeriesDTO.name = raceColumnInSeries.getName();
        raceColumnInSeriesDTO.setMedalRace(raceColumnInSeries.isMedalRace());
        return raceColumnInSeriesDTO;
    }

    @Override
    public void updateRegatta(RegattaIdentifier regattaName, String defaultCourseAreaId) {
        UUID courseAreaUuid = convertIdentifierStringToUuid(defaultCourseAreaId);
        getService().apply(new UpdateSpecificRegatta(regattaName, courseAreaUuid));
    }

    @Override
    public List<RaceColumnInSeriesDTO> addRaceColumnsToSeries(RegattaIdentifier regattaIdentifier, String seriesName, List<String> columnNames) {
        List<RaceColumnInSeriesDTO> result = new ArrayList<RaceColumnInSeriesDTO>();
        for(String columnName: columnNames) {
            RaceColumnInSeries raceColumnInSeries = getService().apply(new AddColumnToSeries(regattaIdentifier, seriesName, columnName));
            if(raceColumnInSeries != null) {
                result.add(convertToRaceColumnInSeriesDTO(raceColumnInSeries));
            }
        }
        return result;
    }

    @Override
    public RaceColumnInSeriesDTO addRaceColumnToSeries(RegattaIdentifier regattaIdentifier, String seriesName, String columnName) {
        RaceColumnInSeriesDTO result = null;
        RaceColumnInSeries raceColumnInSeries = getService().apply(new AddColumnToSeries(regattaIdentifier, seriesName, columnName));
        if(raceColumnInSeries != null) {
            result = convertToRaceColumnInSeriesDTO(raceColumnInSeries);
        }
        return result;
    }

    @Override
    public void removeRaceColumnsFromSeries(RegattaIdentifier regattaIdentifier, String seriesName, List<String> columnNames) {
        for(String columnName: columnNames) {
            getService().apply(new RemoveColumnFromSeries(regattaIdentifier, seriesName, columnName));
        }
    }

    @Override
    public void removeRaceColumnFromSeries(RegattaIdentifier regattaIdentifier, String seriesName, String columnName) {
        getService().apply(new RemoveColumnFromSeries(regattaIdentifier, seriesName, columnName));
    }

    @Override
    public void moveRaceColumnInSeriesUp(RegattaIdentifier regattaIdentifier, String seriesName, String columnName) {
        getService().apply(new MoveColumnInSeriesUp(regattaIdentifier, seriesName, columnName));
    }

    @Override
    public void moveRaceColumnInSeriesDown(RegattaIdentifier regattaIdentifier, String seriesName, String columnName) {
        getService().apply(new MoveColumnInSeriesDown(regattaIdentifier, seriesName, columnName));
    }

    @Override
    public RegattaDTO createRegatta(String regattaName, String boatClassName,
            LinkedHashMap<String, Pair<List<Triple<String, Integer, Color>>, Boolean>> seriesNamesWithFleetNamesAndFleetOrderingAndMedal,
            boolean persistent, ScoringSchemeType scoringSchemeType, String defaultCourseAreaId) {
        UUID courseAreaUuid = convertIdentifierStringToUuid(defaultCourseAreaId);
        Regatta regatta = getService().apply(
                new AddSpecificRegatta(
                        regattaName, boatClassName, UUID.randomUUID(),
                        seriesNamesWithFleetNamesAndFleetOrderingAndMedal,
                        persistent, baseDomainFactory.createScoringScheme(scoringSchemeType), courseAreaUuid));
        return convertToRegattaDTO(regatta);
    }

    @Override
    public RegattaScoreCorrectionDTO getScoreCorrections(String scoreCorrectionProviderName, String eventName,
            String boatClassName, Date timePointWhenResultPublished) throws Exception {
        RegattaScoreCorrectionDTO result = null;
        for (ScoreCorrectionProvider scp : getScoreCorrectionProviders()) {
            if (scp.getName().equals(scoreCorrectionProviderName)) {
                result = createScoreCorrection(scp.getScoreCorrections(eventName, boatClassName,
                        new MillisecondsTimePoint(timePointWhenResultPublished)));
                break;
            }
        }
        return result;
    }

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

    private ScoreCorrectionEntryDTO createScoreCorrectionEntryDTO(
            ScoreCorrectionForCompetitorInRace scoreCorrectionForCompetitor) {
        return new ScoreCorrectionEntryDTO(scoreCorrectionForCompetitor.getPoints(),
                scoreCorrectionForCompetitor.isDiscarded(), scoreCorrectionForCompetitor.getMaxPointsReason());
    }
    
    @Override
    public List<String> getUrlResultProviderNames() {
        List<String> result = new ArrayList<String>();
        for (ScoreCorrectionProvider scp : getScoreCorrectionProviders()) {
            if (scp instanceof UrlResultProvider) {
            	result.add(scp.getName());
            }
        }
        return result;
    }

    private UrlResultProvider getUrlBasedScoreCorrectionProvider(String resultProviderName) {
    	UrlResultProvider result = null;
        for (ScoreCorrectionProvider scp : getScoreCorrectionProviders()) {
            if (scp instanceof UrlResultProvider && scp.getName().equals(resultProviderName)) {
            	result = (UrlResultProvider) scp;
            	break;
            }
        }
        return result;
    }

    @Override
    public List<String> getResultImportUrls(String resultProviderName) {
        List<String> result = new ArrayList<String>();

        UrlResultProvider urlBasedScoreCorrectionProvider = getUrlBasedScoreCorrectionProvider(resultProviderName);
        if (urlBasedScoreCorrectionProvider != null) {
            Iterable<URL> allUrls = urlBasedScoreCorrectionProvider.getAllUrls();
            for (URL url : allUrls) {
                result.add(url.toString());
            }
        }
        return result;
    }

    @Override
    public void removeResultImportURLs(String resultProviderName, Set<String> toRemove) throws Exception {
        UrlResultProvider urlBasedScoreCorrectionProvider = getUrlBasedScoreCorrectionProvider(resultProviderName);
        if (urlBasedScoreCorrectionProvider != null) {
            for (String urlToRemove : toRemove) {
            	urlBasedScoreCorrectionProvider.removeResultUrl(new URL(urlToRemove));
            }
        }
    }

    @Override
    public void addResultImportUrl(String resultProviderName, String url) throws Exception {
        UrlResultProvider urlBasedScoreCorrectionProvider = getUrlBasedScoreCorrectionProvider(resultProviderName);
        if (urlBasedScoreCorrectionProvider != null) {
        	urlBasedScoreCorrectionProvider.registerResultUrl(new URL(url));
        }
    }    

    @Override
    public List<Pair<String, List<CompetitorDTO>>> getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(String leaderboardName, Date date) throws NoWindException {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        final TimePoint timePoint;
        if (date == null) {
            final TimePoint nowMinusDelay = leaderboard.getNowMinusDelay();
            final TimePoint timePointOfLatestModification = leaderboard.getTimePointOfLatestModification();
            if (timePointOfLatestModification != null && !nowMinusDelay.before(timePointOfLatestModification)) {
                timePoint = timePointOfLatestModification;
            } else {
                timePoint = nowMinusDelay;
            }
        } else {
            timePoint = new MillisecondsTimePoint(date);
        }
        Map<RaceColumn, List<Competitor>> preResult = leaderboard
                .getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(timePoint);
        List<Pair<String, List<CompetitorDTO>>> result = new ArrayList<Util.Pair<String,List<CompetitorDTO>>>();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            List<CompetitorDTO> competitorList = baseDomainFactory.getCompetitorDTOList(preResult.get(raceColumn));
            result.add(new Pair<String, List<CompetitorDTO>>(raceColumn.getName(), competitorList));
        }
        return result;
    }

    @Override
    public List<String> getOverallLeaderboardNamesContaining(String leaderboardName) {
        Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
        if (leaderboard == null) {
            throw new IllegalArgumentException("Couldn't find leaderboard named "+leaderboardName);
        }
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, Leaderboard> leaderboardEntry : getService().getLeaderboards().entrySet()) {
            if (leaderboardEntry.getValue() instanceof MetaLeaderboard) {
                MetaLeaderboard metaLeaderboard = (MetaLeaderboard) leaderboardEntry.getValue();
                if (Util.contains(metaLeaderboard.getLeaderboards(), leaderboard)) {
                    result.add(leaderboardEntry.getKey());
                }
            }
        }
        return result;
    }

    @Override
    public List<SwissTimingArchiveConfigurationDTO> getPreviousSwissTimingArchiveConfigurations() {
        Iterable<SwissTimingArchiveConfiguration> configs = swissTimingAdapterPersistence.getSwissTimingArchiveConfigurations();
        List<SwissTimingArchiveConfigurationDTO> result = new ArrayList<SwissTimingArchiveConfigurationDTO>();
        for (SwissTimingArchiveConfiguration stArchiveConfig : configs) {
            result.add(new SwissTimingArchiveConfigurationDTO(stArchiveConfig.getJsonUrl()));
        }
        return result;
    }

    @Override
    public void storeSwissTimingArchiveConfiguration(String swissTimingJsonUrl) {
        swissTimingAdapterPersistence.storeSwissTimingArchiveConfiguration(swissTimingFactory.createSwissTimingArchiveConfiguration(
                swissTimingJsonUrl));
    }

    @Override
    public PolarSheetGenerationTriggerResponse generatePolarSheetForRaces(List<RegattaAndRaceIdentifier> selectedRaces) {
        String id = UUID.randomUUID().toString();
        RacingEventService service = getService();
        Set<TrackedRace> trackedRaces = new HashSet<TrackedRace>();
        for (RegattaAndRaceIdentifier race : selectedRaces) {
            trackedRaces.add(service.getTrackedRace(race));
        }
        PolarSheetGenerationWorker genWorker = new PolarSheetGenerationWorker(trackedRaces, executor);
        polarSheetGenerationWorkers.put(id, genWorker);
        genWorker.startPolarSheetGeneration();
        String name = getCommonBoatClass(trackedRaces);
        return new PolarSheetGenerationTriggerResponseImpl(id, name);
    }

    private String getCommonBoatClass(Set<TrackedRace> trackedRaces) {
        BoatClass boatClass = null;
        for (TrackedRace race : trackedRaces) {
            if (boatClass == null) {
                boatClass = race.getRace().getBoatClass();
            }
            if (!boatClass.getName().matches(race.getRace().getBoatClass().getName())) {
                return "Mixed";
            }
        }

        return boatClass.getName();
    }

    @Override
    public PolarSheetsData getPolarSheetsGenerationResults(String id) {
        PolarSheetsData data = null;
        if (polarSheetGenerationWorkers.containsKey(id)) {
            PolarSheetGenerationWorker worker = polarSheetGenerationWorkers.get(id);
            data = worker.getPolarData();
            if (data.isComplete()) {
                polarSheetGenerationWorkers.remove(id);
                HttpServletRequest httpServletRequest = this.getThreadLocalRequest();
                if (httpServletRequest != null) {
                    HttpSession session = httpServletRequest.getSession();
                    session.setAttribute(id, worker.getCompleteData());
                    session.setAttribute("stepping", worker.getStepping());
                }       
            }
        } else {
            //TODO Exception handling
        }

        return data;      
    }

    @Override
    public PolarSheetsHistogramData getPolarSheetData(String polarSheetId, int angle, int windSpeed) {
        HttpServletRequest httpServletRequest = this.getThreadLocalRequest();
        HttpSession session = httpServletRequest.getSession();
        @SuppressWarnings("unchecked")
        List<List<BoatAndWindSpeed>> data = (List<List<BoatAndWindSpeed>>) session.getAttribute(polarSheetId);
        if (data == null) {
            //TODO exception handling
            return null;
        }
        List<BoatAndWindSpeed> dataForAngle = data.get(angle);
        if (dataForAngle.size() < 1) {
            //TODO exception handling
            return null;
        }
        
        PolarSheetsWindStepping stepping = (PolarSheetsWindStepping) session.getAttribute("stepping");

        List<Double> dataForAngleAndWindSpeed = new ArrayList<Double>();
        int windSpeedLevel = stepping.getLevelForValue(windSpeed);

        for (BoatAndWindSpeed dataPoint: dataForAngle) {
            if ((stepping.getLevelForValue(dataPoint.getWindSpeed().getKnots()) == windSpeedLevel)) {
                dataForAngleAndWindSpeed.add(dataPoint.getBoatSpeed().getKnots());
            }
        }

        if (dataForAngleAndWindSpeed.size() < 1) {
            //TODO exception handling
            return null;
        }

        Double min = Collections.min(dataForAngleAndWindSpeed);
        Double max = Collections.max(dataForAngleAndWindSpeed);
        //TODO make number of columns dynamic to chart size
        int numberOfColumns = 20;
        double range = (max - min) / numberOfColumns;
        Double[] xValues = new Double[numberOfColumns];
        for (int i = 0; i < numberOfColumns; i++) {
            xValues[i] = min + i * range + ( 0.5 * range);
        }

        Integer[] yValues = new Integer[numberOfColumns];
        for (Double dataPoint : dataForAngleAndWindSpeed) {
            int i = (int) (((dataPoint - min) / range));
            if (i == numberOfColumns) {
                //For max value
                i = 19;
            }
            if (yValues[i] == null) {
                yValues[i] = 0;
            }
            yValues[i]++;
        }

        PolarSheetsHistogramData histogramData = new PolarSheetsHistogramDataImpl(angle, xValues, yValues, dataForAngleAndWindSpeed.size());


        return histogramData;
    }

    protected com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory() {
        return baseDomainFactory;
    }
    
    public String getBuildVersion() {
        String version = "Unknown or Development";
        File versionfile = new File(System.getProperty("jetty.home") + File.separator + "version.txt");
        if (versionfile.exists()) {
            try {
                version = new BufferedReader(new FileReader(versionfile)).readLine();
            } catch (Exception ex) {
                logger.severe("Could not load file " + versionfile.getAbsolutePath());
            }
        }
        return version;
    }
}