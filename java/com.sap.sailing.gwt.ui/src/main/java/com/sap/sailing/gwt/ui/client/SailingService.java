package com.sap.sailing.gwt.ui.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gwt.user.client.rpc.RemoteService;
import com.sap.sailing.domain.abstractlog.Revokable;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.NotFoundException;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.PolarSheetsXYDiagramData;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.UnableToCloseDeviceMappingException;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.IncrementalOrFullLeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.racelog.tracking.CompetitorRegistrationOnRaceLogDisabledException;
import com.sap.sailing.domain.common.racelog.tracking.DoesNotHaveRegattaLogException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.racelog.tracking.GPSFixStore;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.gwt.ui.adminconsole.RaceLogSetTrackingTimesDTO;
import com.sap.sailing.gwt.ui.client.shared.charts.MarkPositionService.MarkTracksDTO;
import com.sap.sailing.gwt.ui.shared.BulkScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.CompactBoatPositionsDTO;
import com.sap.sailing.gwt.ui.shared.CompactRaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorsRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO.RegattaConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationMatcherDTO;
import com.sap.sailing.gwt.ui.shared.DeviceMappingDTO;
import com.sap.sailing.gwt.ui.shared.EventBaseDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogSetStartTimeAndProcedureDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceboardDataDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.RegattaLogDTO;
import com.sap.sailing.gwt.ui.shared.RegattaOverviewEntryDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RemoteSailingServerReferenceDTO;
import com.sap.sailing.gwt.ui.shared.ReplicationStateDTO;
import com.sap.sailing.gwt.ui.shared.ScoreCorrectionProviderDTO;
import com.sap.sailing.gwt.ui.shared.ServerConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingArchiveConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingEventRecordDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingReplayRaceDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.TrackFileImportDeviceIdentifierDTO;
import com.sap.sailing.gwt.ui.shared.VenueDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.gwt.client.filestorage.FileStorageManagementGwtService;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.VideoDTO;

/**
 * The client side stub for the RPC service. Usually, when a <code>null</code> date is passed to
 * the time-dependent service methods, an empty (non-<code>null</code>) result is returned.
 */
public interface SailingService extends RemoteService, FileStorageManagementGwtService {
    List<TracTracConfigurationDTO> getPreviousTracTracConfigurations() throws Exception;
    
    List<RegattaDTO> getRegattas();

    RegattaDTO getRegattaByName(String regattaName);

    List<EventDTO> getEvents() throws Exception;

    List<EventBaseDTO> getPublicEventsOfAllSailingServers() throws Exception;

    Util.Pair<String, List<TracTracRaceRecordDTO>> listTracTracRacesInEvent(String eventJsonURL, boolean listHiddenRaces) throws Exception;

    void trackWithTracTrac(RegattaIdentifier regattaToAddTo, Iterable<TracTracRaceRecordDTO> rrs, String liveURI,
            String storedURI, String courseDesignUpdateURI, boolean trackWind, boolean correctWindByDeclination,
            Duration offsetToStartTimeOfSimulatedRace, boolean useInternalMarkPassingAlgorithm, String tracTracUsername, String tracTracPassword) throws Exception;

    void trackWithSwissTiming(RegattaIdentifier regattaToAddTo, Iterable<SwissTimingRaceRecordDTO> rrs, String hostname, int port,
            boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm) throws Exception;
    
    void replaySwissTimingRace(RegattaIdentifier regattaIdentifier, Iterable<SwissTimingReplayRaceDTO> replayRaces,
            boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm);

    void storeTracTracConfiguration(String name, String jsonURL, String liveDataURI, String storedDataURI, String courseDesignUpdateURI, String tracTracUsername, String tracTracPassword) throws Exception;

    void stopTrackingEvent(RegattaIdentifier eventIdentifier) throws Exception;

    void stopTrackingRaces(Iterable<RegattaAndRaceIdentifier> racesToStopTracking) throws Exception;
    
    void removeAndUntrackRaces(Iterable<RegattaAndRaceIdentifier> regattaNamesAndRaceNames);

    WindInfoForRaceDTO getRawWindFixes(RegattaAndRaceIdentifier raceIdentifier, Collection<WindSource> windSources);

    void setWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO wind);

    WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth,
            int numberOfFixes, Collection<String> windSourceTypeNames, boolean onlyUpToNewestEvent,
            boolean includeCombinedWindForAllLegMiddles) throws NoWindException;

    WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, Date to, long resolutionInMilliseconds,
            Collection<String> windSourceTypeNames, boolean onlyUpToNewestEvent);

    WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth,
            int numberOfFixes, double latDeg, double lngDeg, Collection<String> windSources) throws NoWindException;
    
    boolean getPolarResults(RegattaAndRaceIdentifier raceIdentifier);

    SimulatorResultsDTO getSimulatorResults(LegIdentifier legIdentifier);

    RaceboardDataDTO getRaceboardData(String regattaName, String raceName, String leaderboardName, String leaderboardGroupName, UUID eventId);

    Map<CompetitorDTO, BoatDTO> getCompetitorBoats(RegattaAndRaceIdentifier raceIdentifier);
    
    CompactRaceMapDataDTO getRaceMapData(RegattaAndRaceIdentifier raceIdentifier, Date date, Map<String, Date> fromPerCompetitorIdAsString,
            Map<String, Date> toPerCompetitorIdAsString, boolean extrapolate, LegIdentifier simulationLegIdentifier,
            byte[] md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID) throws NoWindException;
    
    CompactBoatPositionsDTO getBoatPositions(RegattaAndRaceIdentifier raceIdentifier,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate) throws NoWindException;

    RaceTimesInfoDTO getRaceTimesInfo(RegattaAndRaceIdentifier raceIdentifier);
    
    List<RaceTimesInfoDTO> getRaceTimesInfos(Collection<RegattaAndRaceIdentifier> raceIdentifiers);
    
    CoursePositionsDTO getCoursePositions(RegattaAndRaceIdentifier raceIdentifier, Date date);

    RaceCourseDTO getRaceCourse(RegattaAndRaceIdentifier raceIdentifier, Date date);

    void removeWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO);

    public List<String> getLeaderboardNames();
    
    IncrementalOrFullLeaderboardDTO getLeaderboardByName(String leaderboardName, Date date,
            Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            String previousLeaderboardId, boolean fillNetPointsUncorrected) throws Exception;

    List<StrippedLeaderboardDTO> getLeaderboards();
    
    List<StrippedLeaderboardDTO> getLeaderboardsByEvent(EventDTO event);
    
    StrippedLeaderboardDTO updateLeaderboard(String leaderboardName, String newLeaderboardName, String newLeaderboardDisplayName, int[] newDiscardingThreasholds, UUID newCourseAreaId);

    StrippedLeaderboardDTO createFlexibleLeaderboard(String leaderboardName, String leaderboardDisplayName, int[] discardThresholds, ScoringSchemeType scoringSchemeType, UUID courseAreaId);

    StrippedLeaderboardDTO createRegattaLeaderboard(RegattaIdentifier regattaIdentifier, String leaderboardDisplayName, int[] discardThresholds);

    void removeLeaderboard(String leaderboardName);

    void removeLeaderboards(Collection<String> leaderboardNames);

    void renameLeaderboard(String leaderboardName, String newLeaderboardName);

    void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName);

    void removeLeaderboardColumn(String leaderboardName, String columnName);
    
    void addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace);
    
    void moveLeaderboardColumnUp(String leaderboardName, String columnName);
    
    void moveLeaderboardColumnDown(String leaderboardName, String columnName);
    
    RegattaDTO createRegatta(String regattaName, String boatClassName, Date startDate, Date endDate,
            RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedal, boolean persistent,
            ScoringSchemeType scoringSchemeType, UUID defaultCourseAreaId, boolean useStartTimeInference, RankingMetrics rankingMetricType);
    
    void removeRegatta(RegattaIdentifier regattaIdentifier);

    void removeSeries(RegattaIdentifier regattaIdentifier, String seriesName);

    void removeRegattas(Collection<RegattaIdentifier> regattas);
    
    void updateRegatta(RegattaIdentifier regattaIdentifier, Date startDate, Date endDate, UUID defaultCourseAreaUuid, 
            RegattaConfigurationDTO regattaConfiguration, boolean useStartTimeInference);
    
    List<RaceColumnInSeriesDTO> addRaceColumnsToSeries(RegattaIdentifier regattaIdentifier, String seriesName,
            List<Pair<String, Integer>> columnNames);

    void updateSeries(RegattaIdentifier regattaIdentifier, String seriesName, String newSeriesName, boolean isMedal,
            int[] resultDiscardingThresholds, boolean startsWithZeroScore,
            boolean firstRaceIsNonDiscardableCarryForward, boolean hasSplitFleetScore, List<FleetDTO> fleets);

    RaceColumnInSeriesDTO addRaceColumnToSeries(RegattaIdentifier regattaIdentifier, String seriesName, String columnName);

    void removeRaceColumnsFromSeries(RegattaIdentifier regattaIdentifier, String seriesName, List<String> columnNames);

    void removeRaceColumnFromSeries(RegattaIdentifier regattaIdentifier, String seriesName, String columnName);

    void moveRaceColumnInSeriesUp(RegattaIdentifier regattaIdentifier, String seriesName, String columnName);

    void moveRaceColumnInSeriesDown(RegattaIdentifier regattaIdentifier, String seriesName, String columnName);

    boolean connectTrackedRaceToLeaderboardColumn(String leaderboardName, String raceColumnName,
            String fleetName, RegattaAndRaceIdentifier raceIdentifier);
    
    void disconnectLeaderboardColumnFromTrackedRace(String leaderboardName, String raceColumnName, String fleetName);
    
    Map<String, RegattaAndRaceIdentifier> getRegattaAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(String leaderboardName, String raceColumnName);

    void updateLeaderboardCarryValue(String leaderboardName, String competitorIdAsString, Double carriedPoints);

    /**
     * @return the new net points in {@link Pair#getA()} and the new total points in {@link Pair#getB()} for time point
     * <code>date</code> after the max points reason has been updated to <code>maxPointsReasonAsString</code>.
     */
    Util.Triple<Double, Double, Boolean> updateLeaderboardMaxPointsReason(String leaderboardName, String competitorIdAsString,
            String raceColumnName, MaxPointsReason maxPointsReason, Date date) throws NoWindException;

    Util.Triple<Double, Double, Boolean> updateLeaderboardScoreCorrection(String leaderboardName, String competitorIdAsString,
            String columnName, Double correctedScore, Date date) throws NoWindException;

    void updateCompetitorDisplayNameInLeaderboard(String leaderboardName, String competitorIdAsString, String displayName);
    
    void updateIsMedalRace(String leaderboardName, String columnName, boolean isMedalRace);

    List<SwissTimingConfigurationDTO> getPreviousSwissTimingConfigurations();

    SwissTimingEventRecordDTO getRacesOfSwissTimingEvent(String eventJsonURL) throws Exception;

    void storeSwissTimingConfiguration(String configName, String jsonURL, String hostname, int port);

    String[] getCountryCodes();
    
    Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> getDouglasPoints(RegattaAndRaceIdentifier raceIdentifier,
            Map<CompetitorDTO, Date> from, Map<CompetitorDTO, Date> to, double meters) throws NoWindException;

    Map<CompetitorDTO, List<ManeuverDTO>> getManeuvers(RegattaAndRaceIdentifier raceIdentifier,
            Map<CompetitorDTO, Date> from, Map<CompetitorDTO, Date> to) throws NoWindException;

    List<StrippedLeaderboardDTO> getLeaderboardsByRaceAndRegatta(RaceDTO race, RegattaIdentifier regattaIdentifier);
    
    List<LeaderboardGroupDTO> getLeaderboardGroups(boolean withGeoLocationData);
    
    LeaderboardGroupDTO getLeaderboardGroupByName(String groupName, boolean withGeoLocationData);
    
    void renameLeaderboardGroup(String oldName, String newName);
    
    void removeLeaderboardGroups(Set<String> groupNames);
    
    LeaderboardGroupDTO createLeaderboardGroup(String groupName, String description, String displayName,
            boolean displayGroupsInReverseOrder, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType);
    
    void updateLeaderboardGroup(String oldName, String newName, String description, String newDisplayName,
            List<String> leaderboardNames, int[] overallLeaderboardDiscardThresholds, ScoringSchemeType overallLeaderboardScoringSchemeType);

    CompetitorsRaceDataDTO getCompetitorsRaceData(RegattaAndRaceIdentifier race, List<CompetitorDTO> competitors, Date from, Date to,
            long stepSizeInMs, DetailType detailType, String leaderboardGroupName, String leaderboardName) throws NoWindException;

    void setRaceIsKnownToStartUpwind(RegattaAndRaceIdentifier raceIdentifier, boolean raceIsKnownToStartUpwind);

    void setWindSourcesToExclude(RegattaAndRaceIdentifier raceIdentifier, Iterable<WindSource> windSourcesToExclude);
    
    ReplicationStateDTO getReplicaInfo();

    void startReplicatingFromMaster(String messagingHost, String masterHost, String exchangeName, int servletPort, int messagingPort) throws Exception;

    void updateRaceDelayToLive(RegattaAndRaceIdentifier regattaAndRaceIdentifier, long delayToLiveInMs);

    void updateRacesDelayToLive(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers, long delayToLiveInMs);

    Pair<Integer, Integer> resolveImageDimensions(String imageUrlAsString) throws Exception;
    
    EventDTO updateEvent(UUID eventId, String eventName, String eventDescription, Date startDate, Date endDate,
            VenueDTO venue, boolean isPublic, Iterable<UUID> leaderboardGroupIds, String officialWebsiteURL, Map<String, String> sailorsInfoWebsiteURLsByLocaleName,
            Iterable<ImageDTO> images, Iterable<VideoDTO> videos) throws Exception;

    EventDTO createEvent(String eventName, String eventDescription, Date startDate, Date endDate, String venue,
            boolean isPublic, List<String> courseAreaNames, String officialWebsiteURL, Map<String, String> sailorsInfoWebsiteURLsByLocaleName, Iterable<ImageDTO> images, Iterable<VideoDTO> videos) throws Exception;
    
    void removeEvent(UUID eventId);

    void removeEvents(Collection<UUID> eventIds);

    void renameEvent(UUID eventId, String newName);

    EventDTO getEventById(UUID id, boolean withStatisticalData) throws Exception;

    Iterable<String> getScoreCorrectionProviderNames();

    ScoreCorrectionProviderDTO getScoreCorrectionsOfProvider(String providerName) throws Exception;

    RegattaScoreCorrectionDTO getScoreCorrections(String scoreCorrectionProviderName, String eventName, String boatClassName,
            Date timePointWhenResultPublished) throws Exception;

    void updateLeaderboardScoreCorrectionsAndMaxPointsReasons(BulkScoreCorrectionDTO updates) throws NoWindException;

    WindInfoForRaceDTO getWindSourcesInfo(RegattaAndRaceIdentifier raceIdentifier);

    ServerInfoDTO getServerInfo();

    ServerConfigurationDTO getServerConfiguration();

    void updateServerConfiguration(ServerConfigurationDTO serverConfiguration);
    
    List<RemoteSailingServerReferenceDTO> getRemoteSailingServerReferences();

    void removeSailingServers(Set<String> toRemove) throws Exception;

    RemoteSailingServerReferenceDTO addRemoteSailingServerReference(RemoteSailingServerReferenceDTO sailingServer) throws Exception;

    List<String> getResultImportUrls(String resultProviderName);

    void removeResultImportURLs(String resultProviderName, Set<String> toRemove) throws Exception;

    void addResultImportUrl(String resultProviderName, String url) throws Exception;

    void updateLeaderboardScoreCorrectionMetadata(String leaderboardName, Date timePointOfLastCorrectionValidity,
            String comment);

    List<Pair<String, String>> getUrlResultProviderNamesAndOptionalSampleURL();
    
    void updateRaceCourse(RegattaAndRaceIdentifier raceIdentifier, List<Util.Pair<ControlPointDTO, PassingInstruction>> controlPoints);

    void addColumnsToLeaderboard(String leaderboardName, List<Util.Pair<String, Boolean>> columnsToAdd);

    void removeLeaderboardColumns(String leaderboardName, List<String> columnsToRemove);

    StrippedLeaderboardDTO getLeaderboard(String leaderboardName);

    void suppressCompetitorInLeaderboard(String leaderboardName, String competitorIdAsString, boolean suppressed);

    void updateLeaderboardColumnFactor(String leaderboardName, String columnName, Double newFactor);

    List<SwissTimingReplayRaceDTO> listSwissTiminigReplayRaces(String swissTimingUrl);

    List<Util.Triple<String, List<CompetitorDTO>, List<Double>>> getLeaderboardDataEntriesForAllRaceColumns(String leaderboardName,
            Date date, DetailType detailType) throws Exception;

    List<String> getOverallLeaderboardNamesContaining(String leaderboardName);

    List<SwissTimingArchiveConfigurationDTO> getPreviousSwissTimingArchiveConfigurations();

    void storeSwissTimingArchiveConfiguration(String swissTimingUrl);
    
    void createCourseAreas(UUID eventId, String[] courseAreaNames);
    
    void removeCourseAreas(UUID eventId, UUID[] courseAreaIds);

    List<Util.Pair<String, String>> getLeaderboardsNamesOfMetaLeaderboard(String metaLeaderboardName);

    Util.Pair<String, LeaderboardType> checkLeaderboardName(String leaderboardName);

        /** for backward compatibility with the regatta overview */
    List<RaceGroupDTO> getRegattaStructureForEvent(UUID eventId);

    /** the replacement service for getRegattaStructureForEvent() */
    List<RaceGroupDTO> getRegattaStructureOfEvent(UUID eventId);

    List<RegattaOverviewEntryDTO> getRaceStateEntriesForRaceGroup(UUID eventId, List<UUID> visibleCourseAreas,
            List<String> visibleRegattas, boolean showOnlyCurrentlyRunningRaces, boolean showOnlyRacesOfSameDay)
            throws Exception;
    
    List<RegattaOverviewEntryDTO> getRaceStateEntriesForLeaderboard(String leaderboardName,
            boolean showOnlyCurrentlyRunningRaces, boolean showOnlyRacesOfSameDay, List<String> visibleRegattas)
            throws Exception;

    void stopReplicatingFromMaster();

    void stopAllReplicas();

    void stopSingleReplicaInstance(String identifier);

    void reloadRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet);

    RaceLogDTO getRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet);

    RegattaLogDTO getRegattaLog(String leaderboardName) throws DoesNotHaveRegattaLogException;

    List<String> getLeaderboardGroupNamesFromRemoteServer(String host);

    UUID importMasterData(String host, String[] groupNames, boolean override, boolean compress, boolean exportWind, boolean exportDeviceConfigurations);
    
    DataImportProgress getImportOperationProgress(UUID id);

    Iterable<CompetitorDTO> getCompetitors();
    
    Iterable<CompetitorDTO> getCompetitorsOfLeaderboard(String leaderboardName);

    CompetitorDTO addOrUpdateCompetitor(CompetitorDTO competitor) throws Exception;

    void allowCompetitorResetToDefaults(Iterable<CompetitorDTO> competitors);
    
    List<DeviceConfigurationMatcherDTO> getDeviceConfigurationMatchers();
    
    DeviceConfigurationDTO getDeviceConfiguration(DeviceConfigurationMatcherDTO matcher);
    
    DeviceConfigurationMatcherDTO createOrUpdateDeviceConfiguration(DeviceConfigurationMatcherDTO matcherDTO, DeviceConfigurationDTO configurationDTO);

    boolean removeDeviceConfiguration(List<String> clientIds);

    boolean setStartTimeAndProcedure(RaceLogSetStartTimeAndProcedureDTO dto);
    
    Util.Triple<Date, Integer, RacingProcedureType> getStartTimeAndProcedure(String leaderboardName, String raceColumnName, String fleetName);

    Iterable<String> getAllIgtimiAccountEmailAddresses();

    String getIgtimiAuthorizationUrl();

    boolean authorizeAccessToIgtimiUser(String eMailAddress, String password) throws Exception;

    void removeIgtimiAccount(String eMailOfAccountToRemove);

    Map<RegattaAndRaceIdentifier, Integer> importWindFromIgtimi(List<RaceDTO> selectedRaces, boolean correctByDeclination) throws Exception;
    
    void denoteForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName) throws Exception;
    
    /**
     * Revoke the {@link RaceLogDenoteForTrackingEvent}. This does not affect an existing {@code RaceLogRaceTracker}
     * or {@link TrackedRace} for this {@code RaceLog}.
     * @throws NotFoundException 
     * 
     * @see RaceLogTrackingAdapter#removeDenotationForRaceLogTracking
     */
    void removeDenotationForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException;

    void denoteForRaceLogTracking(String leaderboardName) throws Exception;
    
    /**
     * Performs all the necessary steps to start tracking the race.
     * The {@code RaceLog} needs to be denoted for racelog-tracking beforehand.
     * 
     * @see RaceLogTrackingAdapter#startTracking
     */
    void startRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName, boolean trackWind, boolean correctWindByDeclination)
            throws NotDenotedForRaceLogTrackingException, Exception;
    
    void setCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName, Set<CompetitorDTO> competitors) throws CompetitorRegistrationOnRaceLogDisabledException, NotFoundException;
    
    /**
     * Adds the course definition to the racelog, while trying to reuse existing marks, controlpoints and waypoints
     * from the previous course definition in the racelog.
     * @throws NotFoundException 
     */
    void addCourseDefinitionToRaceLog(String leaderboardName, String raceColumnName, String fleetName, List<Util.Pair<ControlPointDTO, PassingInstruction>> course) throws NotFoundException;
    
    RaceCourseDTO getLastCourseDefinitionInRaceLog(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException;
    
    /**
     * Adds a fix to the {@link GPSFixStore}, and creates a mapping with a virtual device for exactly the current timepoint.
     * @throws DoesNotHaveRegattaLogException 
     */
    void pingMark(String leaderboardName, MarkDTO mark, Position position) throws DoesNotHaveRegattaLogException;
    
    List<String> getDeserializableDeviceIdentifierTypes();
    
    /**
     * Revoke the events in the {@code RaceLog} that are identified by the {@code eventIds}.
     * This only affects such events that implement {@link Revokable}.
     * @throws NotFoundException 
     */
    void revokeRaceAndRegattaLogEvents(String leaderboardName, String raceColumnName, String fleetName, List<UUID> eventIds)
            throws NotRevokableException, NotFoundException;
    
    Collection<String> getGPSFixImporterTypes();
    
    List<TrackFileImportDeviceIdentifierDTO> getTrackFileImportDeviceIds(List<String> uuids)
            throws NoCorrespondingServiceRegisteredException, TransformationException;
    
    /**
     * @return The RaceDTO of the modified race or <code>null</code>, if the given newStartTimeReceived was null.
     */
    RaceDTO setStartTimeReceivedForRace(RaceIdentifier raceIdentifier, Date newStartTimeReceived);

    PolarSheetsXYDiagramData createXYDiagramForBoatClass(String itemText);

    Map<Integer, Date> getCompetitorMarkPassings(RegattaAndRaceIdentifier race, CompetitorDTO competitorDTO);

    /**
     * Obtains fixed mark passings and mark passing suppressions from the race log identified by
     * <code>leaderboardName</code>, <code>raceColumnDTO</code> and <code>fleet</code>. The result contains
     * pairs of zero-based waypoint numbers and times where <code>null</code> represents a suppressed mark
     * passing and a valid {@link Date} objects represents a fixed mark passing.
     */
    Map<Integer, Date> getCompetitorRaceLogMarkPassingData(String leaderboardName, String raceColumnName, String fleetName,
            CompetitorDTO competitor);

    void updateSuppressedMarkPassings(String leaderboardName, String raceColumnName, String fleetName,
            Integer newZeroBasedIndexOfSuppressedMarkPassing, CompetitorDTO competitorDTO);

    void updateFixedMarkPassing(String leaderboardName, String raceColumnName, String fleetName, Integer indexOfWaypoint,
            Date dateOfMarkPassing, CompetitorDTO competitorDTO);

    void setCompetitorRegistrationsInRegattaLog(String leaderboardName, Set<CompetitorDTO> competitors)
            throws DoesNotHaveRegattaLogException, NotFoundException;
    
    /**
     * A leaderboard may be situated under multiple events (connected via a leaderboardgroup).
     * This method traverses all events and leaderboardgroup to build the collection of events this
     * leaderboard is coupled to.
     */
    Collection<EventDTO> getEventsForLeaderboard(String leaderboardName);

    /**
     * Imports regatta structure definitions from an ISAF XRR document
     * 
     * @param manage2SailJsonUrl the URL pointing to a Manage2Sail JSON document that contains the link to the XRR document
     */
    Iterable<RegattaDTO> getRegattas(String manage2SailJsonUrl);

    void createRegattaStructure(Iterable<RegattaDTO> regattas, EventDTO newEvent) throws Exception;

    Integer getStructureImportOperationProgress();
    
    void inviteCompetitorsForTrackingViaEmail(String serverUrlWithoutTrailingSlash, EventDTO event,
            String leaderboardName, Collection<CompetitorDTO> competitors, String localeInfo) throws MailException;

    void inviteBuoyTenderViaEmail(String serverUrlWithoutTrailingSlash, EventDTO eventDto, String leaderboardName,
            String emails, String localeInfoName) throws MailException;
            
    ArrayList<LeaderboardGroupDTO> getLeaderboardGroupsByEventId(UUID id);

    Iterable<MarkDTO> getMarksInRegattaLog(String leaderboardName) throws DoesNotHaveRegattaLogException;

    List<DeviceMappingDTO> getDeviceMappings(String leaderboardName) throws DoesNotHaveRegattaLogException, TransformationException;

    void revokeRaceAndRegattaLogEvents(String leaderboardName, List<UUID> eventIds) throws NotRevokableException, DoesNotHaveRegattaLogException;

    void closeOpenEndedDeviceMapping(String leaderboardName, DeviceMappingDTO mappingDto, Date closingTimePoint) throws TransformationException, DoesNotHaveRegattaLogException, UnableToCloseDeviceMappingException;

    void addDeviceMappingToRegattaLog(String leaderboardName, DeviceMappingDTO dto)
            throws NoCorrespondingServiceRegisteredException, TransformationException, DoesNotHaveRegattaLogException;

    boolean doesRegattaLogContainCompetitors(String name) throws DoesNotHaveRegattaLogException;

    RegattaAndRaceIdentifier getRaceIdentifier(String regattaLikeName, String raceColumnName, String fleetName);
    void setTrackingTimes(RaceLogSetTrackingTimesDTO dto) throws NotFoundException;

    Pair<TimePoint, TimePoint> getTrackingTimes(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException;

    /**
     * @param raceLogFrom identifies the race log to copy from by its leaderboard name, race column name and fleet name
     * @param raceLogsTo identifies the race log to copy from by their leaderboard name, race column name and fleet name
     * @throws NotFoundException 
     */
    void copyCompetitorsToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples) throws NotFoundException;

    /**
     * @param raceLogFrom identifies the race log to copy from by its leaderboard name, race column name and fleet name
     * @param raceLogsTo identifies the race log to copy from by their leaderboard name, race column name and fleet name
     * @throws NotFoundException 
     */
    void copyCourseToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples) throws NotFoundException;

    /**
     * Get the competitors registered for a certain race. This automatically checks, whether competitors are registered 
     * in the raceLog (in case of e.g. splitFleets) or in the RegattaLog (default) 
     * @throws NotFoundException 
     */
    Collection<CompetitorDTO> getCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName,
            String fleetName) throws DoesNotHaveRegattaLogException, NotFoundException;

    void addMarkToRegattaLog(String leaderboardName, MarkDTO mark) throws DoesNotHaveRegattaLogException;

    void revokeMarkDefinitionEventInRegattaLog(String leaderboardName, MarkDTO markDTO) throws DoesNotHaveRegattaLogException;

    Collection<CompetitorDTO> getCompetitorRegistrationsInRegattaLog(String leaderboardName) throws DoesNotHaveRegattaLogException, NotFoundException;

    Boolean areCompetitorRegistrationsEnabledForRace(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException;

    void disableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName) throws NotRevokableException, NotFoundException;

    void enableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName) throws IllegalArgumentException, NotFoundException;

    Pair<Boolean, String> checkIfMarksAreUsedInOtherRaceLogs(String leaderboardName, String raceColumnName,
            String fleetName, Set<MarkDTO> marksToRemove) throws NotFoundException;

    Collection<CompetitorDTO> getCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName,
            String fleetName) throws NotFoundException;

    Collection<CompetitorDTO> getCompetitorRegistrationsForLeaderboard(String leaderboardName) throws NotFoundException;
    
    MarkTracksDTO getMarkTracks(String leaderboardName, String raceColumnName, String fleetName);
    
    /**
     * The service may decide whether a mark fix can be removed. It may, for example, be impossible to
     * cleanly remove a mark fix if a tracked race already exists and the mark fixes are already part of
     * the GPS fix track which currently does not support a remove operation. However, when only the
     * regatta log is the basis of the service and no tracked race exists yet, mark fixes may be removed
     * by revoking the device mappings.
     */
    boolean canRemoveMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString, GPSFixDTO fix);
    
    void removeMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString, GPSFixDTO fix) throws NotRevokableException;
    
    void addMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString, GPSFixDTO newFix);
    
    void editMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString, GPSFixDTO oldFix, Position newPosition) throws NotRevokableException;

    Map<Triple<String, String, String>, Pair<TimePoint, TimePoint>> getTrackingTimes(Collection<Triple<String, String, String>> raceColumnsAndFleets);
}
