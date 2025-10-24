package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.fileupload.FileItem;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.json.simple.parser.ParseException;
import org.osgi.framework.BundleContext;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDenoteForTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogStartTrackingEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.EventBaseImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.Placemark;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.ExpeditionAllInOneConstants.ImportMode;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.tracking.BravoExtendedFix;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.geocoding.ReverseGeocoder;
import com.sap.sailing.server.gateway.trackfiles.impl.ImportResult.ErrorImportDTO;
import com.sap.sailing.server.gateway.trackfiles.impl.ImportResult.TrackImportDTO;
import com.sap.sailing.server.gateway.windimport.AbstractWindImporter;
import com.sap.sailing.server.gateway.windimport.AbstractWindImporter.WindImportResult;
import com.sap.sailing.server.gateway.windimport.expedition.WindImporter;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.AddSpecificRegatta;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.UpdateEvent;
import com.sap.sailing.server.security.PermissionAwareRaceTrackingHandler;
import com.sap.sailing.server.trackfiles.impl.ExpeditionImportFileHandler;
import com.sap.sailing.server.util.WaitForTrackedRaceUtil;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.util.FileItemHelper;

/**
 * Importer for expedition data that imports all available data for a boat:
 * <ul>
 * <li>{@link GPSFixMoving} and {@link BravoExtendedFix} instances are imported as a distinct track that needs to be
 * mapped to a specific competitor afterwards</li>
 * <li>Wind fixes are being imported as a new {@link WindTrack}</li>
 * </ul>
 * There are several {@link ImportMode ImportModes} which have specific preconditions and activate different importing
 * behavior:
 * <ul>
 * <li>{@link ImportMode#NEW_EVENT} requires a boatclass name to be given. In this case new {@link Event},
 * {@link Regatta} and {@link RegattaLeaderboard} entities are created with one {@link RaceColumn} for a new
 * {@link TrackedRace} to be the target of the imported data. The names of the created entities as well as the created
 * {@link RaceColumn} and {@link WindSource} are generated from the name of the given {@link FileItem}.</li>
 * <li>{@link ImportMode#NEW_COMPETITOR} requires a regatta name to be given. In this case the wind data is being
 * imported as new {@link WindSource} to any existing race of the given regatta. The name of the {@link WindSource} is
 * determined from the name of the given {@link FileItem}. No new entities are created.</li>
 * <li>{@link ImportMode#NEW_RACE} requires a regatta name to be given. In this case the a new {@link RaceColumn} is
 * created in the last existing {@link Series}. A new {@link WindSource} is added to the {@link TrackedRace} associated
 * with the newly added {@link RaceColumn}. This mode does not support importing in cases where fleet racing is used by
 * a {@link Regatta}.</li>
 * </ul>
 * The imported {@link GPSFixMoving} and {@link BravoExtendedFix} tracks aren't mapped to a {@link Competitor} by the
 * importer. Instead the IDs of the imported tracks are contained in the result and are expected to be mapped by the
 * user afterwards.
 *
 * This importer is intended to be used by {@link ExpeditionAllInOneImportServlet}.
 */
public class ExpeditionAllInOneImporter {

    private static final Logger logger = Logger.getLogger(ExpeditionAllInOneImporter.class.getName());

    private static final double VENUE_RANGE_CHECK = 10;

    /**
     * For sessions created automatically from start times found in the log, tries to set the tracking start
     * time this much before the race start, unless it would be before the first fix received which then would
     * provide the start of tracking time instead.
     */
    private static final Duration TRACKING_DURATION_BEFORE_START = Duration.ONE_MINUTE.times(5);

    /**
     * This prefix is used to create race columns based on start times automatically.
     */
    private static final String START_PER_SESSION_RACE_COLUMN_NAME_PREFIX = "R";

    private final RacingEventService service;
    private final RaceLogTrackingAdapter adapter;
    private final TypeBasedServiceFinderFactory serviceFinderFactory;
    private final BundleContext context;

    private ResourceBundleStringMessages serverStringMessages;
    private Locale uiLocale;

    private final SecurityService securityService;

    public static class ImporterResult {
        final UUID eventId;
        final List<Triple<String, String, String>> raceNameRaceColumnNameFleetnameList = new ArrayList<>();
        final String leaderboardName, leaderboardGroupName, regattaName;
        final List<TrackImportDTO> importGpsFixData, importSensorFixData;
        final String sensorFixImporterType;
        final List<ErrorImportDTO> errorList = new ArrayList<>();
        final ExpeditionStartData startData;

        public ImporterResult(String error) {
            this(null, "", "", "", Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), "", Collections.emptyList(), /* startData */ null);
            errorList.add(new ErrorImportDTO(error));
        }

        public ImporterResult(Throwable exception, List<ErrorImportDTO> additionalErrors) {
            this(null, "", "","", Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList(), "", Collections.emptyList(), /* startData */ null);
            errorList.add(new ErrorImportDTO(exception.getClass().getName(), exception.getMessage()));
            if (additionalErrors != null) {
                errorList.addAll(additionalErrors);
            }
        }

        private ImporterResult(final UUID eventId, final String leaderboardName, String leaderboardGroupName,
                final String regattaName, List<Triple<String, String, String>> raceNameRaceColumnNameFleetnameList,
                final List<TrackImportDTO> importGpsFixData, final List<TrackImportDTO> importSensorFixData,
                final String sensorFixImporterType, List<ErrorImportDTO> errors, ExpeditionStartData startData) {
            this.eventId = eventId;
            this.leaderboardName = leaderboardName;
            this.leaderboardGroupName = leaderboardGroupName;
            this.regattaName = regattaName;
            if (raceNameRaceColumnNameFleetnameList != null) {
                this.raceNameRaceColumnNameFleetnameList.addAll(raceNameRaceColumnNameFleetnameList);
            }
            this.importGpsFixData = importGpsFixData;
            this.importSensorFixData = importSensorFixData;
            this.sensorFixImporterType = sensorFixImporterType;
            this.errorList.addAll(errors);
            this.startData = startData;
        }
    }

    public ExpeditionAllInOneImporter(ResourceBundleStringMessages serverStringMessages, Locale uiLocale,
            final RacingEventService service, SecurityService securityService, RaceLogTrackingAdapter adapter,
            final TypeBasedServiceFinderFactory serviceFinderFactory, final BundleContext context) {
        this.serverStringMessages = serverStringMessages;
        this.uiLocale = uiLocale;
        this.service = service;
        this.securityService = securityService;
        this.adapter = adapter;
        this.serviceFinderFactory = serviceFinderFactory;
        this.context = context;
    }

    private static class TimePointsOfFirstAndLastFix {
        private final TimePoint firstFixAt;
        private final TimePoint lastFixAt;
        public TimePointsOfFirstAndLastFix(TimePoint firstFixAt, TimePoint lastFixAt) {
            super();
            this.firstFixAt = firstFixAt;
            this.lastFixAt = lastFixAt;
        }
        public TimePoint getFirstFixAt() {
            return firstFixAt;
        }
        public TimePoint getLastFixAt() {
            return lastFixAt;
        }
    }

    private TimePointsOfFirstAndLastFix importFixes(final String filenameWithSuffix, final FileItem fileItem,
            final ImportResult jsonHolderForGpsFixImport, final ImportResult jsonHolderForSensorFixImport,
            final List<ErrorImportDTO> errors) throws AllInOneImportException {
        // Import GPS Fixes
        final List<Pair<String, FileItem>> filesForGpsFixImport = Arrays.asList(new Pair<>(filenameWithSuffix, fileItem));
        try {
            new TrackFilesImporter(service, serviceFinderFactory, context).importFixes(jsonHolderForGpsFixImport,
                    GPSFixImporter.EXPEDITION_TYPE, filesForGpsFixImport);
            this.ensureSuccessfulImport(jsonHolderForGpsFixImport,
                    serverStringMessages.get(uiLocale, "allInOneErrorGPSDataImportFailed"));
        } catch (IOException e1) {
            errors.addAll(jsonHolderForGpsFixImport.getErrorList());
            throw new AllInOneImportException(e1, errors);
        }
        errors.addAll(jsonHolderForGpsFixImport.getErrorList());
        // Import Extended Sensor Data
        final Iterable<Pair<String, FileItem>> importerNamesAndFilesForSensorFixImport = Arrays
                .asList(new Pair<>(DoubleVectorFixImporter.EXPEDITION_EXTENDED_TYPE, fileItem));
        try {
            new SensorDataImporter(service, context).importFiles(false, jsonHolderForSensorFixImport,
                    importerNamesAndFilesForSensorFixImport);
            this.ensureSuccessfulImport(jsonHolderForSensorFixImport,
                    serverStringMessages.get(uiLocale, "allInOneErrorSensorDataImportFailed"));
        } catch (IOException e1) {
            errors.addAll(jsonHolderForSensorFixImport.getErrorList());
            throw new AllInOneImportException(e1, errors);
        }
        TimePoint firstFixAt = null;
        TimePoint lastFixAt = null;
        final ArrayList<TrackImportDTO> allData = new ArrayList<>();
        allData.addAll(jsonHolderForGpsFixImport.getImportResult());
        allData.addAll(jsonHolderForSensorFixImport.getImportResult());
        for (TrackImportDTO result : allData) {
            final TimePoint deviceTrackStart = result.getRange().from();
            final TimePoint deviceTrackEnd = result.getRange().to();
            if (firstFixAt == null || deviceTrackStart.before(firstFixAt)) {
                firstFixAt = deviceTrackStart;
            }
            if (lastFixAt == null || deviceTrackEnd.after(lastFixAt)) {
                lastFixAt = deviceTrackEnd;
            }
        }
        return new TimePointsOfFirstAndLastFix(firstFixAt, lastFixAt);
    }

    public ImporterResult importFiles(final String filenameWithSuffix, final FileItem fileItem,
            final String boatClassName, ImportMode importMode, String existingRegattaName, boolean importStartData)
                    throws AllInOneImportException, IOException, FormatNotSupportedException {
        securityService.checkCurrentUserServerPermission(ServerActions.CREATE_OBJECT);
        final List<ErrorImportDTO> errors = new ArrayList<>();
        final String importTimeString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now(ZoneOffset.UTC));
        final String filename = ExpeditionImportFilenameUtils.truncateFilenameExtentions(filenameWithSuffix, new ExpeditionImportFileHandler() {
            @Override // used only to resolve the standard file name extensions defined by the Expedition import file handler
            protected void handleExpeditionFile(String fileName, InputStream inputStream, Charset charset)
                    throws IOException, FormatNotSupportedException {
            }});
        final String filenameWithDateTimeSuffix = filename + "_" + importTimeString;
        final String trackedRaceName = filenameWithDateTimeSuffix;
        final String windSourceId = filenameWithDateTimeSuffix;
        final int[] discardThresholds = new int[0];
        final ImportResult jsonHolderForGpsFixImport = new ImportResult(logger);
        final ImportResult jsonHolderForSensorFixImport = new ImportResult(logger);
        errors.addAll(jsonHolderForSensorFixImport.getErrorList());
        final UUID eventId;
        final String leaderboardGroupName;
        final String regattaNameAndleaderboardName;
        final List<Triple<String, String, String>> raceNameRaceColumnNameFleetnameList = new ArrayList<>();
        final List<DynamicTrackedRace> trackedRaces = new ArrayList<>();
        final ExpeditionCourseInferrer expeditionCourseInferrer = new ExpeditionCourseInferrer(adapter);
        final ExpeditionStartData startData = expeditionCourseInferrer.getStartData(fileItem.getInputStream(), filenameWithSuffix,
                FileItemHelper.getCharset(fileItem, Charset.forName("UTF-8")));
        final Iterable<String> additionalTrackedRaceNames = importStartData ? getNextRaceColumnNames(/* start race index */ 1,
                /* how many */ Util.size(startData.getStartTimes())) : Collections.emptySet();
        if (importMode == ImportMode.NEW_EVENT) {
            leaderboardGroupName = filenameWithDateTimeSuffix;
            regattaNameAndleaderboardName = filenameWithDateTimeSuffix;
            String raceColumnName = filename;
            eventId = UUID.randomUUID();
            String fleetName = LeaderboardNameConstants.DEFAULT_FLEET_NAME;
            final String eventName = filenameWithDateTimeSuffix;
            final String regattaName = filenameWithDateTimeSuffix;
            // The permissions for competitor/boat creation are checked when the UI, after receiving this request's
            // result, selects or creates the competitor to map the import results to
            securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.EVENT,
                    EventBaseImpl.getTypeRelativeObjectIdentifier(eventId), filenameWithDateTimeSuffix,
                    ()->securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                            SecuredDomainType.REGATTA, Regatta.getTypeRelativeObjectIdentifier(regattaName), regattaName,
                            ()->securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                                    SecuredDomainType.LEADERBOARD, Leaderboard.getTypeRelativeObjectIdentifier(regattaName), regattaName,
                                    ()->checkTrackedRacesCreationPermission(regattaName, trackedRaceName, additionalTrackedRaceNames,
                                            ()->{
                        final TimePointsOfFirstAndLastFix firstAndLastFixAt = importFixes(filenameWithSuffix, fileItem, jsonHolderForGpsFixImport, jsonHolderForSensorFixImport, errors);
                        final TimePoint eventStartDate = firstAndLastFixAt.getFirstFixAt();
                        final TimePoint eventEndDate = firstAndLastFixAt.getLastFixAt();
                        logger.info("Trying to create event "+eventName+" and regatta "+regattaName+" on behalf of user "+SecurityUtils.getSubject().getPrincipal());
                        final Iterable<Triple<DynamicTrackedRace, String, String>> trackedRacesAndRaceColumnNamesAndFleetNames =
                                createEventStructureWithASingleRaceAndTrackIt(
                                    filenameWithSuffix, boatClassName, errors, importTimeString, filename,
                                    eventName, regattaName, trackedRaceName, discardThresholds, firstAndLastFixAt.getFirstFixAt(), firstAndLastFixAt.getLastFixAt(),
                                    eventStartDate, eventEndDate, eventId, leaderboardGroupName, regattaNameAndleaderboardName,
                                    fleetName, raceColumnName, importStartData?startData:null);
                        for (Triple<DynamicTrackedRace, String, String> i : trackedRacesAndRaceColumnNamesAndFleetNames) {
                            trackedRaces.add(i.getA());
                            raceNameRaceColumnNameFleetnameList.add(new Triple<>(i.getA().getRace().getName(),
                                    /* race column name */ i.getB(), /* fleet name */ i.getC()));
                        }
                        updateVenueName(filename, jsonHolderForGpsFixImport.getImportResult(), eventId);
                        return null;
                                            }))));
        } else {
            regattaNameAndleaderboardName = existingRegattaName;
            if (existingRegattaName != null && !existingRegattaName.isEmpty()) {
                final Regatta regatta = service.getRegattaByName(existingRegattaName);
                if (regatta == null) {
                    return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorInvalidData"));
                }
                final Leaderboard leaderboard = service.getLeaderboardByName(existingRegattaName);
                if (leaderboard == null || !(leaderboard instanceof RegattaLeaderboard)) {
                    return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorInvalidLeaderBoard"));
                }
                final RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                final Pair<Event, LeaderboardGroup> foundEventAndLeaderboardGroup = findEventAndLeaderboardGroupForExistingLeaderboard(leaderboard);
                if (foundEventAndLeaderboardGroup == null) {
                    return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorInvalidLeaderBoardEventLink"));
                }
                eventId = foundEventAndLeaderboardGroup.getA().getId();
                leaderboardGroupName = foundEventAndLeaderboardGroup.getB().getName();
                securityService.checkCurrentUserExplicitPermissions(service.getEvent(eventId), DefaultActions.UPDATE);
                securityService.checkCurrentUserExplicitPermissions(service.getRegatta(new RegattaName(regattaNameAndleaderboardName)), DefaultActions.UPDATE);
                securityService.checkCurrentUserExplicitPermissions(service.getLeaderboardByName(regattaNameAndleaderboardName), DefaultActions.UPDATE);
                if (importMode == ImportMode.NEW_COMPETITOR) {
                    final DynamicTrackedRace trackedRace = service.getTrackedRace(new RegattaNameAndRaceName(regattaNameAndleaderboardName, trackedRaceName));
                    if (trackedRace != null) {
                        securityService.checkCurrentUserExplicitPermissions(trackedRace, DefaultActions.UPDATE);
                        final TimePointsOfFirstAndLastFix firstAndLastFixAt = importFixes(filenameWithSuffix, fileItem, jsonHolderForGpsFixImport, jsonHolderForSensorFixImport, errors);
                        ensureEventLongEnough(firstAndLastFixAt.getFirstFixAt(), firstAndLastFixAt.getLastFixAt(), eventId);
                        final Iterable<RaceColumn> raceColumns = regattaLeaderboard.getRaceColumns();
                        if (Util.isEmpty(raceColumns)) {
                            return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorInvalidRace"));
                        }
                        try {
                            for (RaceColumn raceColumn : raceColumns) {
                                final Iterable<? extends Fleet> fleets = raceColumn.getFleets();
                                if (Util.size(fleets) != 1) {
                                    return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorSplitFleetNotSupported"));
                                }
                                final Fleet fleet = fleets.iterator().next();
                                DynamicTrackedRace trackedRaceForColumn = (DynamicTrackedRace) raceColumn.getTrackedRace(fleet);
                                if (trackedRaceForColumn == null) {
                                    trackedRaceForColumn = trackRace(regattaLeaderboard, raceColumn, fleet);
                                }
                                trackedRaces.add(trackedRaceForColumn);
                                raceNameRaceColumnNameFleetnameList
                                        .add(new Triple<>(trackedRaceForColumn.getRaceIdentifier().getRaceName(),
                                                raceColumn.getName(), fleet.getName()));
                            }
                        } catch (Exception e) {
                            throw new AllInOneImportException(e, errors);
                        }
                    }
                } else if (importMode == ImportMode.NEW_RACE) {
                    // When uploading files with identical name, the second RaceColumn will be named with the upload time in its name
                    // First, create the session for the full log
                    final String raceColumnName = regatta.getRaceColumnByName(filename) == null ? filename : filenameWithDateTimeSuffix;
                    ImporterResult trackedRacesResult = checkTrackedRacesCreationPermission(regattaNameAndleaderboardName,
                            trackedRaceName, additionalTrackedRaceNames, ()->{
                        final TimePointsOfFirstAndLastFix firstAndLastFixAt = importFixes(filenameWithSuffix, fileItem, jsonHolderForGpsFixImport, jsonHolderForSensorFixImport, errors);
                        ensureEventLongEnough(firstAndLastFixAt.getFirstFixAt(), firstAndLastFixAt.getLastFixAt(), eventId);
                        final Iterable<? extends Series> seriesInRegatta = regatta.getSeries();
                        if (Util.isEmpty(seriesInRegatta)) {
                            return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorInvalidSeries"));
                        }
                        final Series series = Util.get(seriesInRegatta, Util.size(seriesInRegatta) - 1);
                        final Iterable<? extends Fleet> fleets = series.getFleets();
                        if (Util.size(fleets) != 1) {
                            return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorMultiSeries"));
                        }
                        final Triple<DynamicTrackedRace, String, String> trackedRaceAndRaceColumnNameAndFleetName = addRace(
                                errors, regatta, raceColumnName, trackedRaceName, regattaLeaderboard,
                                firstAndLastFixAt.getFirstFixAt(), firstAndLastFixAt.getLastFixAt(), /* start time */ null);
                        trackedRaces.add(trackedRaceAndRaceColumnNameAndFleetName.getA());
                        raceNameRaceColumnNameFleetnameList.add(new Triple<>(trackedRaceAndRaceColumnNameAndFleetName.getA().getRace().getName(),
                                /* race column name */ trackedRaceAndRaceColumnNameAndFleetName.getB(),
                                /* fleet name */ trackedRaceAndRaceColumnNameAndFleetName.getC()));
                        if (importStartData) {
                            // then create another session per start time found:
                            createSessionsForStartTimes(errors, raceNameRaceColumnNameFleetnameList, trackedRaces,
                                    startData, regatta, regattaLeaderboard, firstAndLastFixAt);
                        }
                        return null;
                    });
                    if (trackedRacesResult != null) {
                        return trackedRacesResult;
                    }
                } else {
                    return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorInvalidImportMode") + importMode);
                }
            } else {
                return new ImporterResult(serverStringMessages.get(uiLocale, "allInOneErrorInvalidRegattaName"));
            }
        }
        if (importStartData) {
            expeditionCourseInferrer.setStartLine(startData, trackedRaces, service);
        }
        // Import Wind Data
        try {
            final WindImportResult windImportResult = new AbstractWindImporter.WindImportResult();
            final WindSourceWithAdditionalID windSource = new WindSourceWithAdditionalID(WindSourceType.EXPEDITION, windSourceId);
            final Map<InputStream, Pair<String, Charset>> streamsWithFilenames = new HashMap<>();
            streamsWithFilenames.put(fileItem.getInputStream(), new Pair<>(filenameWithSuffix, FileItemHelper.getCharset(fileItem)));
            new WindImporter().importWindToWindSourceAndTrackedRaces(service, windImportResult, windSource, trackedRaces, streamsWithFilenames);
            return new ImporterResult(eventId, regattaNameAndleaderboardName, leaderboardGroupName,
                    regattaNameAndleaderboardName, raceNameRaceColumnNameFleetnameList,
                    jsonHolderForGpsFixImport.getImportResult(), jsonHolderForSensorFixImport.getImportResult(),
                    DoubleVectorFixImporter.EXPEDITION_EXTENDED_TYPE, errors, startData);
        } catch (Exception e) {
            throw new AllInOneImportException(e, errors);
        }
    }

    /**
     * Checks whether the current {@link Subject} is permitted to created the {@link SecuredDomainType#TRACKED_RACE
     * tracked races} named as specified by {@code trackedRaceName} and the additional strings in
     * {@code additionalTrackedRaceNames} which may have resulted from an automated session split based on start times
     * found in the Expedition log. For this, ownerships for those tracked races are tentatively created, and the
     * created permission is then checked. If any of the create permission checks fails, all those tracked race
     * ownerships tentatively created are removed again, the {@code action} is not executed, and this method fails for
     * the original {@link AuthorizationException}. Otherwise, the {@code action} will be {@link ActionWithResult#run()
     * run} and unless it fails with an {@link AuthorizationException}, the ownerships for the tracked races will
     * persist and the result of the action is returned.
     */
    private <T> T checkTrackedRacesCreationPermission(final String regattaName, final String trackedRaceName,
            final Iterable<String> additionalTrackedRaceNames, final Callable<T> action) {
        Iterator<String> additionalTrackedRaceNamesIterator = additionalTrackedRaceNames.iterator();
        return checkTrackedRaceCreationPermission(regattaName, trackedRaceName, ()->{
            return checkTrackedRaceCreationPermissionRecursively(regattaName, additionalTrackedRaceNamesIterator, action);
        });
    }

    private <T> T checkTrackedRaceCreationPermissionRecursively(final String regattaName, final Iterator<String> additionalTrackedRaceNamesIterator,
            final Callable<T> terminalAction) throws Exception {
        if (additionalTrackedRaceNamesIterator.hasNext()) {
            return checkTrackedRaceCreationPermission(regattaName, additionalTrackedRaceNamesIterator.next(),
                    ()->checkTrackedRaceCreationPermissionRecursively(regattaName, additionalTrackedRaceNamesIterator, terminalAction));
        } else {
            return terminalAction.call();
        }
    }

    private <T> T checkTrackedRaceCreationPermission(String regattaName, String trackedRaceName, Callable<T> action) {
        return securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                TrackedRace.getSecuredDomainType(),
                TrackedRace.getIdentifier(new RegattaNameAndRaceName(regattaName, trackedRaceName)).getTypeRelativeObjectIdentifier(),
                trackedRaceName, action);
    }

    /**
     * Determines start time and start/end of tracking for a sequence of race sessions within the regatta, based on the
     * result of {@link #getStartTimesAndStartAndEndOfTrackingTimes(Iterable, TimePoint, TimePoint)} and passes those on
     * to the {@code consumer} passed.
     * <p>
     *
     * Idea: use this to first determine all race names to check permissions ("dry run"). If no permission problems
     * exist, use a second call to actually perform the race creation.
     */
    private void createSessionsForStartTimes(final List<ErrorImportDTO> errors,
            final List<Triple<String, String, String>> raceNameRaceColumnNameFleetnameList,
            final List<DynamicTrackedRace> trackedRaces, final ExpeditionStartData startData, final Regatta regatta,
            final RegattaLeaderboard regattaLeaderboard, final TimePointsOfFirstAndLastFix firstAndLastFixAt)
            throws AllInOneImportException {
        for (final Triple<TimePoint, TimePoint, TimePoint> startTimesAndStartAndEndOfTrackingTimes : getStartTimesAndStartAndEndOfTrackingTimes(
                startData.getStartTimes(), firstAndLastFixAt.getFirstFixAt(), firstAndLastFixAt.getLastFixAt())) {
            Triple<DynamicTrackedRace, String, String> session = createSessionForStartTime(
                    startTimesAndStartAndEndOfTrackingTimes.getA(),
                    startTimesAndStartAndEndOfTrackingTimes.getB(),
                    startTimesAndStartAndEndOfTrackingTimes.getC(), errors, regatta, regattaLeaderboard);
            trackedRaces.add(session.getA());
            raceNameRaceColumnNameFleetnameList.add(new Triple<>(session.getA().getRace().getName(),
                    /* race column name */ session.getB(),
                    /* fleet name */ session.getC()));
        }
    }

    private Iterable<String> getNextRaceColumnNames(Regatta regatta, int howMany) {
        return getNextRaceColumnNames(getNextAvailableStartBasedSessionCount(regatta), howMany);
    }

    private Iterable<String> getNextRaceColumnNames(int startIndex, int howMany) {
        final List<String> result = new ArrayList<>(howMany);
        int counter = startIndex;
        for (int i=0; i<howMany; i++) {
            result.add(START_PER_SESSION_RACE_COLUMN_NAME_PREFIX + counter++);
        }
        return result;
    }

    private Triple<DynamicTrackedRace, String, String> createSessionForStartTime(TimePoint startTime,
            TimePoint firstFixAt, TimePoint lastFixAt, List<ErrorImportDTO> errors, Regatta regatta,
            RegattaLeaderboard regattaLeaderboard) throws AllInOneImportException {
        final String raceColumnName = getNextRaceColumnNames(regatta, 1).iterator().next();
        final Triple<DynamicTrackedRace, String, String> trackedRaceAndRaceColumnNameAndFleetName = addRace(
                errors, regatta, raceColumnName, raceColumnName, regattaLeaderboard, firstFixAt, lastFixAt, startTime);
        return trackedRaceAndRaceColumnNameAndFleetName;
    }

    private int getNextAvailableStartBasedSessionCount(Regatta regatta) {
        int maxNumberFound = 0;
        final Pattern pattern = Pattern.compile(START_PER_SESSION_RACE_COLUMN_NAME_PREFIX+"([0-9]+)");
        for (final RaceColumn raceColumn : regatta.getRaceColumns()) {
            Matcher matcher = pattern.matcher(raceColumn.getName());
            if (matcher.matches()) {
                if (matcher.groupCount() > 0) {
                    final String numberAsString = matcher.group(1);
                    if (!numberAsString.isEmpty()) {
                        final int number = Integer.valueOf(numberAsString);
                        if (number > maxNumberFound) {
                            maxNumberFound = number;
                        }
                    }
                }
            }
        }
        return maxNumberFound+1;
    }

    /**
     * @param startTime
     *            optional; if not {@code null}, a start time race log event will be added to fix this start time
     * @return the race created, and the race column name and the fleet name
     */
    private Triple<DynamicTrackedRace, String, String> addRace(final List<ErrorImportDTO> errors, final Regatta regatta,
            final String raceColumnName, final String trackedRaceName, final RegattaLeaderboard regattaLeaderboard,
            final TimePoint startOfTracking, final TimePoint endOfTracking, TimePoint startTime) throws AllInOneImportException {
        final Iterable<? extends Series> seriesInRegatta = regatta.getSeries();
        assert !Util.isEmpty(seriesInRegatta);
        final Series series = Util.get(seriesInRegatta, Util.size(seriesInRegatta) - 1);
        final Iterable<? extends Fleet> fleets = series.getFleets();
        assert !Util.isEmpty(fleets);
        final Fleet fleet = fleets.iterator().next();
        final String fleetName = fleet.getName();
        // When uploading files with identical name, the second RaceColumn will be named with the upload time in its name
        final RaceColumn raceColumn = service.apply(new AddColumnToSeries(regatta.getRegattaIdentifier(), series.getName(), raceColumnName));
        final DynamicTrackedRace trackedRace = createTrackedRaceAndSetupRaceTimes(errors, trackedRaceName,
                startOfTracking, endOfTracking, regatta, regattaLeaderboard, raceColumn, fleet);
        if (startTime != null) {
            final RaceLog raceLog = raceColumn.getRaceLog(raceColumn.getFleets().iterator().next());
            raceLog.add(new RaceLogStartTimeEventImpl(startTime, service.getServerAuthor(), /* priority */ 0, startTime, /* courseAreaId */ null));
        }
        return new Triple<>(trackedRace, raceColumnName, fleetName);
    }

    private void ensureEventLongEnough(TimePoint firstFixAt, TimePoint lastFixAt, UUID eventId) {
        Event event = service.getEvent(eventId);
        TimePoint startDate = event.getStartDate();
        if (firstFixAt.before(startDate)) {
            startDate = firstFixAt;
        }
        TimePoint endDate = event.getEndDate();
        if (lastFixAt.after(endDate)) {
            endDate = lastFixAt;
        }
        Iterable<UUID> leaderboardGroups = StreamSupport.stream(event.getLeaderboardGroups().spliterator(), false).map(t -> t.getId()).collect(Collectors.toList());
        service.apply(new UpdateEvent(event.getId(), event.getName(), event.getDescription(), startDate,
                endDate, event.getVenue().getName(), event.isPublic(),
                leaderboardGroups, event.getOfficialWebsiteURL(), event.getBaseURL(),
                event.getSailorsInfoWebsiteURLs(), event.getImages(), event.getVideos(), event.getWindFinderReviewedSpotsCollectionIds()));
    }

    private Pair<Event, LeaderboardGroup> findEventAndLeaderboardGroupForExistingLeaderboard(final Leaderboard leaderboard) {
        for (Event event : service.getAllEvents()) {
            for (LeaderboardGroup leaderboardGroup : event.getLeaderboardGroups()) {
                for (Leaderboard lb : leaderboardGroup.getLeaderboards()) {
                    if (lb.equals(leaderboard)) {
                        return new Pair<>(event, leaderboardGroup);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param startData if not {@code null}, an additional race column will be created for each start time; see also
     * {@link #createTrackedRaceAndSetupRaceTimes(List, String, TimePoint, TimePoint, Regatta, RegattaLeaderboard, RaceColumn, Fleet)}.
     */
    private Iterable<Triple<DynamicTrackedRace, String, String>> createEventStructureWithASingleRaceAndTrackIt(
            final String filenameWithSuffix, final String boatClassName,
            final List<ErrorImportDTO> errors, final String importTimeString, final String filename,
            final String eventName, final String regattaName, final String trackedRaceName, final int[] discardThresholds,
            TimePoint firstFixAt, TimePoint lastFixAt, final TimePoint eventStartDate, final TimePoint eventEndDate,
            final UUID eventId, final String leaderboardGroupName, final String regattaNameAndleaderboardName,
            final String fleetName, final String raceColumnName, final ExpeditionStartData startData) throws AllInOneImportException {
        final DynamicTrackedRace trackedRace;
        final String description = MessageFormat.format("Event imported from expedition file ''{0}'' on {1}",
                filenameWithSuffix, importTimeString);
        final RegattaIdentifier regattaIdentifier = new RegattaName(regattaName);
        // This is just the default used in the UI
        final Double buoyZoneRadiusInHullLengths = 3.0;
        final String seriesName = LeaderboardNameConstants.DEFAULT_SERIES_NAME;
        final Event event = service.addEvent(eventName, description, eventStartDate, eventEndDate, filename, true, eventId);
        final UUID courseAreaId = addDefaultCourseArea(event);
        final Regatta regatta = createRegattaWithOneRaceColumn(boatClassName, regattaNameAndleaderboardName,
                fleetName, raceColumnName, regattaIdentifier, courseAreaId, buoyZoneRadiusInHullLengths, seriesName);
        final RegattaLeaderboard regattaLeaderboard = service.apply(new CreateRegattaLeaderboard(regattaIdentifier, null, discardThresholds));
        createLeaderboardGroupAndAddItToTheEvent(leaderboardGroupName, regattaNameAndleaderboardName, description, event);
        final RaceColumn raceColumn = regattaLeaderboard.getRaceColumns().iterator().next();
        final Fleet fleet = raceColumn.getFleets().iterator().next();
        trackedRace = createTrackedRaceAndSetupRaceTimes(errors, trackedRaceName, firstFixAt, lastFixAt, regatta, regattaLeaderboard, raceColumn, fleet);
        final List<Triple<DynamicTrackedRace, String, String>> result = new ArrayList<>();
        result.add(new Triple<>(trackedRace, raceColumnName, fleetName));
        if (startData != null) {
            // then create another session per start time found:
            for (final Triple<TimePoint, TimePoint, TimePoint> startTimesAndStartAndEndOfTrackingTimes : getStartTimesAndStartAndEndOfTrackingTimes(startData.getStartTimes(), firstFixAt, lastFixAt)) {
                final Triple<DynamicTrackedRace, String, String> session = createSessionForStartTime(
                        startTimesAndStartAndEndOfTrackingTimes.getA(),
                        startTimesAndStartAndEndOfTrackingTimes.getB(),
                        startTimesAndStartAndEndOfTrackingTimes.getC(), errors, regatta, regattaLeaderboard);
                result.add(session);
            }
        }
        return result;
    }

    /**
     * From the start times inferred from the log and the first and last fix determines a sequence of triples telling
     * for each session to create the race start time as well as the tracking start/end times. Tracking intervals will
     * be cropped so they fit into {@code [firstFixAt,lastFixAt]}. Ideally, tracking is set such that it starts
     * {@link #TRACKING_DURATION_BEFORE_START} before the race start or at {@code firstFixAt} if it needs to be cropped.
     * The last session extends until {@code lastFixAt}. All other sessions are set to end {@link #TRACKING_DURATION_BEFORE_START}
     * before the next start.
     */
    private Iterable<Triple<TimePoint, TimePoint, TimePoint>> getStartTimesAndStartAndEndOfTrackingTimes(Iterable<TimePoint> startTimes, TimePoint firstFixAt, TimePoint lastFixAt) {
        final List<Triple<TimePoint, TimePoint, TimePoint>> result = new ArrayList<>();
        TimePoint previousStartOfTracking = null;
        TimePoint previousStartTime = null;
        for (final TimePoint startTime : startTimes) {
            if (previousStartOfTracking != null) {
                assert previousStartTime != null;
                final TimePoint preferredEndOfTracking = startTime.minus(TRACKING_DURATION_BEFORE_START);
                result.add(createInterval(previousStartOfTracking, preferredEndOfTracking, previousStartTime, firstFixAt, lastFixAt));
            }
            final TimePoint preferredStartOfTracking = startTime.minus(TRACKING_DURATION_BEFORE_START);
            if (preferredStartOfTracking.before(firstFixAt)) {
                previousStartOfTracking = firstFixAt;
            } else {
                previousStartOfTracking = preferredStartOfTracking;
            }
            previousStartTime = startTime;
        }
        if (previousStartOfTracking != null) {
            // close last interval
            result.add(createInterval(previousStartOfTracking, /* preferred end of tracking */ lastFixAt, previousStartTime, firstFixAt, lastFixAt));
        }
        return result;
    }

    private Triple<TimePoint, TimePoint, TimePoint> createInterval(TimePoint startOfTracking, TimePoint preferredEndOfTracking,
            TimePoint startTime, TimePoint firstFixAt, TimePoint lastFixAt) {
        assert startOfTracking != null;
        assert startTime != null;
        final TimePoint endOfTracking;
        if (preferredEndOfTracking.after(lastFixAt)) {
            endOfTracking = lastFixAt;
        } else {
            if (preferredEndOfTracking.before(firstFixAt)) {
                endOfTracking = firstFixAt;
            } else {
                endOfTracking = preferredEndOfTracking;
            }
        }
        return new Triple<>(startTime, startOfTracking, endOfTracking);
    }
    private void updateVenueName(String filename, List<TrackImportDTO> list, UUID eventId) {
        for (TrackImportDTO f : list) {
            TrackFileImportDeviceIdentifier deviceIdentifier = TrackFileImportDeviceIdentifierImpl.getOrCreate(f.getDevice());
            final TimePoint end = f.getRange().to();
            try {
                final CompletableFuture<GPSFix> waitForFix = new CompletableFuture<>();
                service.getSensorFixStore().loadFixes(waitForFix::complete, deviceIdentifier, end, end, true);
                GPSFix gpsFix = waitForFix.getNow(null);
                if (gpsFix != null) {
                    Position pos = gpsFix.getPosition();
                    if (pos != null) {
                        try {
                            Placemark reverseVenue = ReverseGeocoder.INSTANCE.getPlacemarkFirst(pos, VENUE_RANGE_CHECK,
                                    new Placemark.ByPopulationDistanceRatio(pos));
                            if (reverseVenue != null) {
                                String newVenueName = reverseVenue.getName();
                                Event event = service.getEvent(eventId);
                                Iterable<UUID> leaderboardGroups = StreamSupport
                                        .stream(event.getLeaderboardGroups().spliterator(), false).map(t -> t.getId())
                                        .collect(Collectors.toList());
                                service.apply(new UpdateEvent(event.getId(), event.getName(), event.getDescription(),
                                        event.getStartDate(), event.getEndDate(), newVenueName, event.isPublic(),
                                        leaderboardGroups, event.getOfficialWebsiteURL(), event.getBaseURL(),
                                        event.getSailorsInfoWebsiteURLs(), event.getImages(), event.getVideos(),
                                        event.getWindFinderReviewedSpotsCollectionIds()));
                                break;
                            }
                        } catch (IOException | ParseException e) {
                            logger.log(Level.WARNING, "Could not reverse determine location " + pos, e);
                        }
                    }
                }
            } catch (NoCorrespondingServiceRegisteredException | TransformationException e) {
                logger.log(Level.WARNING, "Could not reverse determine location", e);
            }
        }
    }

    private UUID addDefaultCourseArea(final Event event) {
        final String courseAreaName = "Default";
        final UUID courseAreaId = UUID.randomUUID();
        service.addCourseAreas(event.getId(), new String[] { courseAreaName }, new UUID[] { courseAreaId }, /* centerPositions */ new Position[0], /* radiuses */ new Distance[0]);
        return courseAreaId;
    }

    private Regatta createRegattaWithOneRaceColumn(final String boatClassName, final String regattaNameAndleaderboardName, final String fleetName,
            final String raceColumnName, final RegattaIdentifier regattaIdentifier, final UUID courseAreaId,
            final Double buoyZoneRadiusInHullLengths, final String seriesName) throws AllInOneImportException {
        final ScoringSchemeType scoringSchemeType = ScoringSchemeType.LOW_POINT;
        final RankingMetrics rankingMetric = RankingMetrics.ONE_DESIGN;
        final Regatta regatta;
        final ScoringScheme scoringScheme = service.getBaseDomainFactory().createScoringScheme(scoringSchemeType);
        final LinkedHashMap<String, SeriesCreationParametersDTO> seriesCreationParameters = new LinkedHashMap<>();
        final List<FleetDTO> fleets = new ArrayList<>();
        fleets.add(new FleetDTO(fleetName, 0, null));
        seriesCreationParameters.put(seriesName,
                new SeriesCreationParametersDTO(fleets, /*isMedal*/ false,
                        /* isFleetsCanRunInParallel */ false, /*isStartsWithZeroScore*/ false, /*firstColumnIsNonDiscardableCarryForward*/false, /*discardingThresholds*/ null,
                        /*hasSplitFleetContiguousScoring*/ false, /* hasCrossFleetMergedRanking */ false, /*maximumNumberOfDiscards*/ null, /* oneAlwaysStaysOne */ false));
        final RegattaCreationParametersDTO regattaCreationParameters = new RegattaCreationParametersDTO(seriesCreationParameters);
        regatta = service.apply(new AddSpecificRegatta(regattaNameAndleaderboardName, boatClassName,
                /* can boats of competitors change */ false, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ UUID.randomUUID().toString(), /* start date */ null, /* end date */ null,
                UUID.randomUUID(),
                regattaCreationParameters, true, scoringScheme,
                courseAreaId==null?Collections.emptySet():Collections.singleton(courseAreaId),
                buoyZoneRadiusInHullLengths, /* use start time inference */ true,
                /* control tracking from start and finish times */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, rankingMetric));
        this.ensureBoatClassDetermination(regatta);
        service.apply(new AddColumnToSeries(regattaIdentifier, seriesName, raceColumnName));
        return regatta;
    }

    private void createLeaderboardGroupAndAddItToTheEvent(final String leaderboardGroupName,
            final String regattaNameAndleaderboardName, final String description, final Event event) {
        UUID newGroupid = UUID.randomUUID();
        final LeaderboardGroup leaderboardGroup = securityService
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD_GROUP,
                        LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(newGroupid),
                        /* securityDisplayName */ null, new Callable<LeaderboardGroup>() {
                            @Override
                            public LeaderboardGroup call() throws Exception {
                                CreateLeaderboardGroup createLeaderboardGroup = new CreateLeaderboardGroup(newGroupid,
                                        leaderboardGroupName, description, /* displayName */ null,
                                        /* displayGroupsInReverseOrder */ false,
                                        Collections.singletonList(regattaNameAndleaderboardName),
                                        /* overallLeaderboardDiscardThresholds */ null,
                                        /* overallLeaderboardScoringSchemeType */ null);
                                return service.apply(createLeaderboardGroup);
                            }
                        });
        service.apply(new UpdateEvent(event.getId(), event.getName(), event.getDescription(), event.getStartDate(),
                event.getEndDate(), event.getVenue().getName(), event.isPublic(),
                Collections.singleton(leaderboardGroup.getId()), event.getOfficialWebsiteURL(), event.getBaseURL(),
                event.getSailorsInfoWebsiteURLs(), event.getImages(), event.getVideos(),
                event.getWindFinderReviewedSpotsCollectionIds()));
    }

    private DynamicTrackedRace createTrackedRaceAndSetupRaceTimes(final List<ErrorImportDTO> errors,
            final String trackedRaceName, TimePoint firstFixAt, TimePoint lastFixAt, final Regatta regatta,
            final RegattaLeaderboard regattaLeaderboard, final RaceColumn raceColumn, final Fleet fleet)
            throws AllInOneImportException {
        // TODO this could be where we evaluate the ExpeditionStartData to optionally create one additional race per start
        final RaceLog raceLog = raceColumn.getRaceLog(fleet);
        final AbstractLogEventAuthor author = service.getServerAuthor();
        final TimePoint startOfTracking = firstFixAt;
        final TimePoint endOfTracking = lastFixAt;
        raceLog.add(new RaceLogStartOfTrackingEventImpl(startOfTracking, author, raceLog.getCurrentPassId()));
        raceLog.add(new RaceLogEndOfTrackingEventImpl(endOfTracking, author, raceLog.getCurrentPassId()));
        try {
            TimePoint startTrackingTimePoint = MillisecondsTimePoint.now();
            // this ensures that the events consistently have different timepoints to ensure a consistent result of the
            // state analysis
            // that's why we can't just call adapter.denoteRaceForRaceLogTracking
            final TimePoint denotationTimePoint = startTrackingTimePoint.minus(1);
            raceLog.add(new RaceLogDenoteForTrackingEventImpl(denotationTimePoint, service.getServerAuthor(),
                    raceLog.getCurrentPassId(), trackedRaceName, regatta.getBoatClass(), UUID.randomUUID()));
            raceLog.add(new RaceLogStartTrackingEventImpl(startTrackingTimePoint, author, raceLog.getCurrentPassId()));
            return trackRace(regattaLeaderboard, raceColumn, fleet);
        } catch (Exception e) {
            throw new AllInOneImportException(e, errors);
        }
    }

    private DynamicTrackedRace trackRace(final RegattaLeaderboard regattaLeaderboard, final RaceColumn raceColumn,
            final Fleet fleet) throws NotDenotedForRaceLogTrackingException, Exception {
        DynamicTrackedRace trackedRace;
        final RaceHandle raceHandle = adapter.startTracking(service, regattaLeaderboard, raceColumn, fleet,
                /* trackWind */ false, /* correctWindDirectionByMagneticDeclination */ true,
                new PermissionAwareRaceTrackingHandler(securityService));
        // wait for the RaceDefinition to be created
        raceHandle.getRace();
        trackedRace = WaitForTrackedRaceUtil.waitForTrackedRace(raceColumn, fleet, 10);
        if (trackedRace == null) {
            throw new IllegalStateException("Could not obtain imported race");
        }
        return trackedRace;
    }

    private void ensureSuccessfulImport(ImportResult result, String errorMessage) throws AllInOneImportException {
        if (!result.getErrorList().isEmpty()) {
            throw new AllInOneImportException(errorMessage, result.getErrorList());
        }
    }

    private void ensureBoatClassDetermination(Regatta regatta) throws AllInOneImportException {
        if (regatta.getBoatClass() == null) {
            throw new AllInOneImportException(serverStringMessages.get(uiLocale, "allInOneErrorBoatClassDeterminationFailed"));
        }
    }
}
