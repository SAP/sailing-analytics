package com.sap.sailing.gwt.ui.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.UnauthorizedException;

import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogTagEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DataImportProgress;
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
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.ServiceException;
import com.sap.sailing.domain.common.UnableToCloseDeviceMappingException;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.PairingListDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.common.racelog.tracking.CompetitorRegistrationOnRaceLogDisabledException;
import com.sap.sailing.domain.common.racelog.tracking.DoesNotHaveRegattaLogException;
import com.sap.sailing.domain.common.racelog.tracking.MarkAlreadyUsedInRaceException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotableForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.expeditionconnector.ExpeditionDeviceConfiguration;
import com.sap.sailing.gwt.ui.adminconsole.RaceLogSetTrackingTimesDTO;
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
import com.sap.sse.common.Duration;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.gwt.client.filestorage.FileStorageManagementGwtService;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.ImageResizingTaskDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.gwt.shared.filestorage.FileStorageServiceDTO;
import com.sap.sse.gwt.shared.filestorage.FileStorageServicePropertyErrorsDTO;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.ui.shared.SuccessInfo;

public interface SailingServiceWrite extends FileStorageManagementGwtService, SailingService {

    void setORCPerformanceCurveScratchBoat(String leaderboardName, String raceColumnName, String fleetName,
            CompetitorDTO newScratchBoatDTO) throws NotFoundException;

    CompetitorDTO getORCPerformanceCurveScratchBoat(String leaderboardName, String raceColumnName, String fleetName)
            throws NotFoundException;

    Triple<Integer, Integer, Integer> assignORCPerformanceCurveCertificates(String leaderboardName,
            String raceColumnName, String fleetName, Map<String, ORCCertificate> certificatesForBoatsWithIdAsString)
            throws IOException, NotFoundException;

    Triple<Integer, Integer, Integer> assignORCPerformanceCurveCertificates(RegattaIdentifier regattaIdentifier,
            Map<String, ORCCertificate> certificatesForBoatsWithIdAsString) throws IOException, NotFoundException;

    String getSecretForRegattaByName(String regattaName);

    void updateGroupOwnerForLeaderboardGroupHierarchy(UUID leaderboardGroupId,
            MigrateGroupOwnerForHierarchyDTO migrateGroupOwnerForHierarchyDTO);

    void updateGroupOwnerForEventHierarchy(UUID eventId,
            MigrateGroupOwnerForHierarchyDTO migrateGroupOwnerForHierarchyDTO);

    /**
     * Resizes an {@Link ImageDTO} that is part of an {@link ImageResizingTaskDTO} into a set of resized versions. This
     * set only contains one image in most cases, because most {@Link ImageDTO} only hold one predefined
     * {@link MediaTagConstants}. All {@link MediaTagConstants} stored in the resizingTask of the
     * {@link ImageResizingTaskDTO} create a resize. Since no {@link MediaTagConstants} have the same defined bounds,
     * there will be no merge of these {@Link ImageDTO}. Uses the {@link FileStorageService} to store the resized
     * images. If an error occurs during resize or storing process, it will be tried to restore the previous state.
     *
     * @author Robin Fleige (D067799)
     *
     * @param imageResizingTask
     *            is an {@link ImageResizingTaskDTO} with the information on how the image saved in the {@Link ImageDTO}
     *            should be resized. The resizingTask attribute should not be null or empty at this point
     * @return returns a set of {@Link ImageDTO}, that contain the resized variants of the {@Link ImageDTO} in
     *         toResizeImage
     * @throws UnauthorizedException,
     *             Exception can throw different type of exceptions
     */
    Set<ImageDTO> resizeImage(final ImageResizingTaskDTO resizingTask) throws Exception;

    void removeExpeditionDeviceConfiguration(ExpeditionDeviceConfiguration deviceConfiguration);

    void setEliminatedCompetitors(String leaderboardName, Set<CompetitorDTO> newEliminatedCompetitorDTOs)
            throws NotFoundException;

    void editMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO oldFix, Position newPosition) throws NotRevokableException;

    void removeMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO fix) throws NotRevokableException;

    void enableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName)
            throws IllegalArgumentException, NotFoundException;

    void disableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName)
            throws NotRevokableException, NotFoundException;

    void inviteBuoyTenderViaEmail(String serverUrlWithoutTrailingSlash, EventDTO eventDto, String leaderboardName,
            String emails, String iOSAppUrl, String androidAppUrl, String localeInfoName) throws MailException;

    void inviteCompetitorsForTrackingViaEmail(String serverUrlWithoutTrailingSlash, EventDTO eventDto,
            String leaderboardName, Collection<CompetitorDTO> competitorDtos, String iOSAppUrl, String androidAppUrl,
            String localeInfoName) throws MailException;

    String getActiveFileStorageServiceName();

    void setActiveFileStorageService(String serviceName, String localeInfoName);

    void startRaceLogTracking(List<Triple<String, String, String>> leaderboardRaceColumnFleetNames, boolean trackWind,
            boolean correctWindByDeclination)
            throws UnauthorizedException, NotDenotedForRaceLogTrackingException, Exception;

    FileStorageServicePropertyErrorsDTO testFileStorageServiceProperties(String serviceName, String localeInfoName)
            throws IOException;

    void setFileStorageServiceProperties(String serviceName, Map<String, String> properties);

    FileStorageServiceDTO[] getAvailableFileStorageServices(String localeInfoName);

    void updateSuppressedMarkPassings(String leaderboardName, String raceColumnName, String fleetName,
            Integer newZeroBasedIndexOfSuppressedMarkPassing, CompetitorDTO competitorDTO) throws NotFoundException;

    void updateFixedMarkPassing(String leaderboardName, String raceColumnName, String fleetName,
            Integer indexOfWaypoint, Date dateOfMarkPassing, CompetitorDTO competitorDTO) throws NotFoundException;

    void closeOpenEndedDeviceMapping(String leaderboardName, DeviceMappingDTO mappingDTO, Date closingTimePoint)
            throws TransformationException, DoesNotHaveRegattaLogException, UnableToCloseDeviceMappingException,
            NotFoundException;

    void revokeRaceAndRegattaLogEvents(String leaderboardName, List<UUID> eventIds)
            throws NotRevokableException, DoesNotHaveRegattaLogException, NotFoundException;

    void addTypedDeviceMappingToRegattaLog(String leaderboardName, TypedDeviceMappingDTO dto)
            throws NoCorrespondingServiceRegisteredException, TransformationException, DoesNotHaveRegattaLogException,
            NotFoundException;

    void addDeviceMappingToRegattaLog(String leaderboardName, DeviceMappingDTO dto)
            throws NoCorrespondingServiceRegisteredException, TransformationException, DoesNotHaveRegattaLogException;

    void revokeMarkDefinitionEventInRegattaLog(String leaderboardName, String raceColumnName, String fleetName,
            MarkDTO markDTO) throws DoesNotHaveRegattaLogException, MarkAlreadyUsedInRaceException;

    void fillRaceLogsFromPairingListTemplate(final String leaderboardName, final int flightMultiplier,
            final List<String> selectedFlightNames, final PairingListDTO pairingListDTO)
            throws NotFoundException, CompetitorRegistrationOnRaceLogDisabledException;

    void setBoatRegistrationsInRegattaLog(String leaderboardName, Set<BoatDTO> boatDTOs)
            throws DoesNotHaveRegattaLogException, NotFoundException;

    void setCompetitorRegistrationsInRegattaLog(String leaderboardName, Set<? extends CompetitorDTO> competitorDTOs)
            throws DoesNotHaveRegattaLogException, NotFoundException;

    void setCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            Set<CompetitorWithBoatDTO> competitorDTOs)
            throws CompetitorRegistrationOnRaceLogDisabledException, NotFoundException;

    void setCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            Map<? extends CompetitorDTO, BoatDTO> competitorAndBoatDTOs)
            throws CompetitorRegistrationOnRaceLogDisabledException, NotFoundException;

    void denoteForRaceLogTracking(String leaderboardName, String prefix) throws Exception;

    Boolean denoteForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName)
            throws NotFoundException, NotDenotableForRaceLogTrackingException;

    Map<RegattaAndRaceIdentifier, Integer> importWindFromIgtimi(List<RaceDTO> selectedRaces,
            boolean correctByDeclination, String optionalBearerTokenOrNull)
            throws IllegalStateException, Exception;

    IgtimiDataAccessWindowWithSecurityDTO addIgtimiDataAccessWindow(String deviceSerialNumber, Date from, Date to);

    void removeIgtimiDataAccessWindow(long id);
    
    void updateIgtimiDevice(IgtimiDeviceWithSecurityDTO editedObject);

    void removeIgtimiDevice(String serialNumber);
    
    boolean sendGPSOffCommandToIgtimiDevice(String serialNumber) throws IOException;

    boolean sendGPSOnCommandToIgtimiDevice(String serialNumber) throws IOException;

    boolean sendPowerOffCommandToIgtimiDevice(String serialNumber) throws IOException;

    boolean sendRestartCommandToIgtimiDevice(String serialNumber) throws IOException;

    boolean sendIMUCalibrationCommandSequenceToIgtimiDevice(String serialNumber) throws IOException, InterruptedException;
    
    boolean sendIgtimiCommand(String serialNumber, String command) throws IOException, InterruptedException;
    
    boolean enableIgtimiDeviceOverTheAirLog(String serialNumber, boolean enable) throws Exception;
    
    ArrayList<Pair<TimePoint, String>> getIgtimiDeviceLogs(String serialNumber, Duration duration) throws Exception;
    
    void setTrackingTimes(RaceLogSetTrackingTimesDTO dto) throws NotFoundException;

    boolean removeDeviceConfiguration(UUID deviceConfigurationId);

    DataImportProgress getImportOperationProgress(UUID id) throws UnauthorizedException;

    void allowCompetitorResetToDefaults(List<CompetitorDTO> competitors);

    UUID importMasterData(String host, UUID[] leaderboardGroupIds, boolean override, boolean compress, boolean exportWind,
            boolean exportDeviceConfigurations, String targetServerUsername, String targetServerPassword,
            boolean exportTrackedRacesAndStartTracking) throws UnauthorizedException;

    RegattaDTO createRegatta(String regattaName, String boatClassName, boolean canBoatsOfCompetitorsChangePerRace,
            CompetitorRegistrationType competitorRegistrationType, String registrationLinkSecret, Date startDate,
            Date endDate, RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedal,
            boolean persistent, ScoringSchemeType scoringSchemeType, List<UUID> courseAreaIds,
            Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange, RankingMetrics rankingMetricType)
            throws UnauthorizedException;

    void removeRaceColumnsFromSeries(RegattaIdentifier regattaIdentifier, String seriesName, List<String> columnNames)
            throws UnauthorizedException;

    void updateSeries(RegattaIdentifier regattaIdentifier, String seriesName, String newSeriesName, boolean isMedal,
            boolean isFleetsCanRunInParallel, int[] resultDiscardingThresholds, boolean startsWithZeroScore,
            boolean firstRaceIsNonDiscardableCarryForward, boolean hasSplitFleetScore, boolean hasCrossFleetMergedRanking, Integer maximumNumberOfDiscards,
            boolean oneAlwaysStaysOne, List<FleetDTO> fleets) throws UnauthorizedException;

    void updateRegatta(RegattaIdentifier regattaIdentifier, Date startDate, Date endDate, List<UUID> courseAreaUuids,
            RegattaConfigurationDTO regattaConfiguration, Double buoyZoneRadiusInHullLengths,
            boolean useStartTimeInference, boolean controlTrackingFromStartAndFinishTimes,
            boolean autoRestartTrackingUponCompetitorSetChange, String registrationLinkSecret, CompetitorRegistrationType registrationType) throws UnauthorizedException;

    void removeSeries(RegattaIdentifier regattaIdentifier, String seriesName) throws UnauthorizedException;

    void removeRegatta(RegattaIdentifier regattaIdentifier) throws UnauthorizedException;

    void removeRegattas(Collection<RegattaIdentifier> regattas) throws UnauthorizedException;

    void renameEvent(UUID eventId, String newName) throws UnauthorizedException;

    void removeEvent(UUID eventId) throws UnauthorizedException;

    void removeEvents(Collection<UUID> eventIds) throws UnauthorizedException;

    void removeCourseAreas(UUID eventId, UUID[] courseAreaIds) throws UnauthorizedException;

    void createCourseAreas(UUID eventId, List<CourseAreaDTO> courseAreas);

    EventDTO createEvent(String eventName, String eventDescription, Date startDate, Date endDate, String venue,
            boolean isPublic, List<CourseAreaDTO> courseAreas, String officialWebsiteURLAsString, String baseURLAsString,
            Map<String, String> sailorsInfoWebsiteURLsByLocaleName, List<ImageDTO> images,
            List<VideoDTO> videos, List<UUID> leaderboardGroupIds)
            throws UnauthorizedException;

    EventDTO updateEvent(EventDTO eventDTO) throws UnauthorizedException, IOException;

    EventDTO updateEvent(UUID eventId, String eventName, String eventDescription, Date startDate, Date endDate,
            VenueDTO venue, boolean isPublic, List<UUID> leaderboardGroupIds, String officialWebsiteURLString,
            String baseURLAsString, Map<String, String> sailorsInfoWebsiteURLsByLocaleName, List<? extends ImageDTO> images,
            List<? extends VideoDTO> videos, List<String> windFinderReviewedSpotCollectionIds)
            throws UnauthorizedException, IOException;

    void updateLeaderboardGroup(UUID leaderboardGroupId, String oldName, String newName, String newDescription,
            String newDisplayName, List<String> leaderboardNames, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType);

    LeaderboardGroupDTO createLeaderboardGroup(String groupName, String description, String displayName,
            boolean displayGroupsInReverseOrder, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType);

    void trackWithSwissTiming(RegattaIdentifier regattaToAddTo, List<SwissTimingRaceRecordDTO> rrs, String hostname,
            int port, boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm,
            String updateURL, String updateUsername, String updatePassword, String eventName, String manage2SailEventUrl) throws UnauthorizedException, Exception;

    void updateSwissTimingConfiguration(SwissTimingConfigurationWithSecurityDTO configuration)
            throws UnauthorizedException, Exception;

    void deleteSwissTimingConfigurations(Collection<SwissTimingConfigurationWithSecurityDTO> configurations)
            throws UnauthorizedException, Exception;

    void createSwissTimingConfiguration(String configName, String jsonURL, String hostname, Integer port,
            String updateURL, String updateUsername, String updatePassword) throws UnauthorizedException, Exception;

    void updateRacesDelayToLive(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers, long delayToLiveInMs);

    void updateIsMedalRace(String leaderboardName, String columnName, boolean isMedalRace);

    void moveLeaderboardColumnUp(String leaderboardName, String columnName) throws UnauthorizedException;

    void moveLeaderboardColumnDown(String leaderboardName, String columnName) throws UnauthorizedException;

    void updateCompetitorDisplayNameInLeaderboard(String leaderboardName, String competitorIdAsString,
            String displayName);

    void updateLeaderboardScoreCorrectionsAndMaxPointsReasons(BulkScoreCorrectionDTO updates) throws NoWindException;

    void updateLeaderboardScoreCorrectionMetadata(String leaderboardName, Date timePointOfLastCorrectionValidity,
            String comment);

    Triple<Double, Double, Boolean> updateLeaderboardScoreCorrection(String leaderboardName,
            String competitorIdAsString, String columnName, Double correctedScore, Date date) throws NoWindException;

    Triple<Double, Double, Boolean> updateLeaderboardIncrementalScoreCorrection(
            String leaderboardName, String competitorIdAsString, String columnName, Double scoringOffsetInPoints, Date date);
    
    void updateLeaderboardCarryValue(String leaderboardName, String competitorIdAsString, Double carriedPoints);

    void disconnectLeaderboardColumnFromTrackedRace(String leaderboardName, String raceColumnName, String fleetName)
            throws UnauthorizedException;

    boolean connectTrackedRaceToLeaderboardColumn(String leaderboardName, String raceColumnName, String fleetName,
            RegattaAndRaceIdentifier raceIdentifier) throws UnauthorizedException;

    void suppressCompetitorInLeaderboard(String leaderboardName, String competitorIdAsString, boolean suppressed);

    void updateLeaderboardColumnFactor(String leaderboardName, String columnName, Double newFactor);

    void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName)
            throws UnauthorizedException;

    void removeLeaderboardColumn(String leaderboardName, String columnName) throws UnauthorizedException;

    void removeLeaderboardGroups(Set<UUID> groupIds);

    void removeLeaderboard(String leaderboardName) throws UnauthorizedException;

    void removeLeaderboards(Collection<String> leaderboardNames) throws UnauthorizedException;

    StrippedLeaderboardDTO updateLeaderboard(String leaderboardName, String newLeaderboardDisplayName,
            int[] newDiscardingThreasholds, List<UUID> newCourseAreaIds)
            throws UnauthorizedException;

    StrippedLeaderboardDTO createRegattaLeaderboardWithEliminations(String name, String displayName,
            String regattaName) throws UnauthorizedException;

    StrippedLeaderboardDTO createRegattaLeaderboardWithOtherTieBreakingLeaderboard(RegattaName regattaIdentifier,
            String leaderboardDisplayName, int[] discardThresholds, String otherTieBreakingLeaderboardName);

    StrippedLeaderboardDTO createRegattaLeaderboard(RegattaName regattaIdentifier,
            String leaderboardDisplayName, int[] discardThresholds) throws UnauthorizedException;

    StrippedLeaderboardDTO createFlexibleLeaderboard(String leaderboardName, String leaderboardDisplayName,
            int[] discardThresholds, ScoringSchemeType scoringSchemeType, List<UUID> courseAreaIds)
            throws UnauthorizedException;

    void removeResultImportURLs(String resultProviderName, Set<UrlDTO> toRemove) throws Exception;

    void deleteSwissTimingArchiveConfigurations(Collection<SwissTimingArchiveConfigurationWithSecurityDTO> dtos)
            throws UnauthorizedException, Exception;

    void updateSwissTimingArchiveConfiguration(SwissTimingArchiveConfigurationWithSecurityDTO dto)
            throws UnauthorizedException, Exception;

    void createSwissTimingArchiveConfiguration(String jsonUrl)
            throws UnauthorizedException, Exception;

    void createRegattaStructure(final List<RegattaDTO> regattas, final EventDTO newEvent) throws IOException;

    void removeWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO);

    void setWindSourcesToExclude(RegattaAndRaceIdentifier raceIdentifier, List<WindSource> windSourcesToExclude);

    void setRaceIsKnownToStartUpwind(RegattaAndRaceIdentifier raceIdentifier, boolean raceIsKnownToStartUpwind);

    void updateRaceCourse(RegattaAndRaceIdentifier raceIdentifier,
            List<Pair<ControlPointDTO, PassingInstruction>> courseDTO);

    void removeSailingServers(Set<String> toRemove) throws UnauthorizedException, Exception;

    RemoteSailingServerReferenceDTO addRemoteSailingServerReference(RemoteSailingServerReferenceDTO sailingServer)
            throws UnauthorizedException, Exception;

    RemoteSailingServerReferenceDTO updateRemoteSailingServerReference(
            RemoteSailingServerReferenceDTO sailingServer) throws UnauthorizedException, Exception;

    RemoteSailingServerReferenceDTO getCompleteRemoteServerReference(String sailingServerName)
            throws UnauthorizedException, Exception;

    void setWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO);

    void removeAndUntrackRaces(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers);

    void stopTrackingRaces(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers) throws Exception;

    void updateTracTracConfiguration(TracTracConfigurationWithSecurityDTO tracTracConfiguration)
            throws UnauthorizedException, Exception;

    void deleteTracTracConfigurations(Collection<TracTracConfigurationWithSecurityDTO> tracTracConfigurations)
            throws UnauthorizedException, Exception;

    void createTracTracConfiguration(String name, String jsonURL, String liveDataURI, String storedDataURI,
            String courseDesignUpdateURI, String tracTracUsername, String tracTracPassword) throws Exception;

    void trackWithTracTrac(RegattaIdentifier regattaToAddTo, List<TracTracRaceRecordDTO> rrs, String liveURIFromConfiguration,
            String storedURIFromConfiguration, String courseDesignUpdateURI, boolean trackWind, boolean correctWindByDeclination,
            Duration offsetToStartTimeOfSimulatedRace, boolean useInternalMarkPassingAlgorithm, boolean useOfficialEventsToUpdateRaceLog,
            String jsonUrlAsKey) throws UnauthorizedException, Exception;

    void trackWithYellowBrick(RegattaIdentifier regattaToAddTo, List<YellowBrickRaceRecordDTO> rrs,
            boolean trackWind, final boolean correctWindByDeclination, String creatorUsername,
            String raceUrl) throws Exception;

    void createYellowBrickConfiguration(String name, String yellowBrickRaceUrl, String yellowBrickUsername,
            String yellowBrickPassword);

    MarkPropertiesDTO updateMarkPropertiesPositioning(UUID markPropertiesId, DeviceIdentifierDTO deviceIdentifier,
            Position fixedPosition) throws Exception;

    CourseTemplateDTO createOrUpdateCourseTemplate(CourseTemplateDTO courseTemplate);

    /**
     * Removes course templates list
     *
     * @param courseTemplateDTOs
     *            list of course templates to remove
     */
    void removeCourseTemplates(Collection<UUID> courseTemplatesUuids);

    /**
     * Removes mark properties list
     *
     * @param markPropertiesDTOS
     *            list of mark properties to remove
     */
    void removeMarkProperties(Collection<UUID> markPropertiesUuids);

    MarkRoleDTO createMarkRole(MarkRoleDTO markRole);

    List<CompetitorWithBoatDTO> addCompetitors(List<CompetitorDescriptor> competitorsForSaving, String searchTag)
            throws UnauthorizedException, Exception;

    void addColumnsToLeaderboard(String leaderboardName, List<Util.Pair<String, Boolean>> columnsToAdd)
            throws UnauthorizedException;

    void addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace)
            throws UnauthorizedException;

    /**
     * Adds the course definition to the racelog, while trying to reuse existing marks, controlpoints and waypoints from
     * the previous course definition in the racelog.
     */
    void addCourseDefinitionToRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            List<Util.Pair<ControlPointDTO, PassingInstruction>> course, int priority)
            throws UnauthorizedException, NotFoundException;

    void addMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO newFix) throws UnauthorizedException;

    void addMarkToRegattaLog(String leaderboardName, MarkDTO mark)
            throws UnauthorizedException, DoesNotHaveRegattaLogException;

    void addOrReplaceExpeditionDeviceConfiguration(ExpeditionDeviceConfiguration expeditionDeviceConfiguration)
            throws UnauthorizedException, IllegalStateException;

    BoatDTO addOrUpdateBoat(BoatDTO boat) throws UnauthorizedException, Exception;

    List<CompetitorDTO> addOrUpdateCompetitors(List<CompetitorDTO> competitors) throws UnauthorizedException, Exception;

    CompetitorWithBoatDTO addOrUpdateCompetitorWithBoat(CompetitorWithBoatDTO competitor)
            throws UnauthorizedException, Exception;

    CompetitorDTO addOrUpdateCompetitorWithoutBoat(CompetitorDTO competitor) throws UnauthorizedException, Exception;

    MarkPropertiesDTO addOrUpdateMarkProperties(MarkPropertiesDTO markProperties);

    MarkTemplateDTO addOrUpdateMarkTemplate(MarkTemplateDTO markTemplate);

    List<RaceColumnInSeriesDTO> addRaceColumnsToSeries(RegattaIdentifier regattaIdentifier, String seriesName,
            List<Pair<String, Integer>> columnNames) throws UnauthorizedException;

    void addResultImportUrl(String resultProviderName, UrlDTO url) throws UnauthorizedException, Exception;

    /**
     * Adds public tag as {@link RaceLogTagEvent} to {@link RaceLog} and private tag to
     * {@link com.sap.sse.security.interfaces.UserStore UserStore}.
     *
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param tag
     *            title of tag, must <b>NOT</b> be <code>null</code>
     * @param comment
     *            optional comment of tag
     * @param hiddenInfo
     *            Data that will not be displayed to the end user; it may be visible, e.g., when viewing the technical
     *            race log entries or user preference objects, so don't use this to store secrets. But it can be used,
     *            e.g., to store information that identifies the tag in some unique way, for example, when the tag was
     *            automatically produced by some rule or agent, and that rule or agent later needs to decide whether or
     *            not there already is a tag produced by that rule/agent for the race to which the tag pertains.
     * @param visibleForPublic
     *            when set to <code>true</code> tag will be saved as public tag (visible for every user), when set to
     *            <code>false</code> tag will be saved as private tag (visible only for creator)
     * @param raceTimepoint
     *            timepoint in race where user created tag, must <b>NOT</b> be <code>null</code>
     * @param imageURLs
     *            optional image URLs of tag
     * @return <code>successful</code> {@link SuccessInfo} if tag was added successfully, otherwise
     *         <code>non-successful</code> {@link SuccessInfo}
     */
    SuccessInfo addTag(String leaderboardName, String raceColumnName, String fleetName, String tag, String comment,
            String hiddenInfo, String imageURL, String resizedImageURL, boolean visibleForPublic, TimePoint raceTimepoint)
            throws UnauthorizedException;

    void allowBoatResetToDefaults(List<BoatDTO> boats) throws UnauthorizedException;

    /**
     * Into the first {@link RaceLog} {@link TrackedRace#getAttachedRaceLogs() attached} to the tracked race identified
     * by {@code raceIdentifier} writes {@link RaceLogORCLegDataEvent}s and/or revokation events such that afterwards
     * the {@link #getORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier)} will return a map equal to the one passed,
     * except for {@code null} values which then would be missing from the results, where here they mean to explicitly
     * revoke any previous setting for that leg.
     */
    void setORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier raceIdentifier,
            Map<Integer, ORCPerformanceCurveLegImpl> legInfo) throws NotRevokableException;

    /**
     * Into the {@link RaceLog} identified by the leaderboard, race column and fleet names, writes
     * {@link RaceLogORCLegDataEvent}s and/or revokation events such that afterwards the
     * {@link #getORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier)} will return a map equal to the one passed,
     * except for {@code null} values which then would be missing from the results, where here they mean to explicitly
     * revoke any previous setting for that leg.
     *
     * @throws NotFoundException
     *             in case the race log cannot be found by the leaderboard, race column and fleet names provided
     */
    void setORCPerformanceCurveLegInfo(String leaderboardName, String raceColumnName, String fleetName,
            Map<Integer, ORCPerformanceCurveLegImpl> legInfo) throws NotFoundException, NotRevokableException;

    /**
     * @return The RaceDTO of the modified race or <code>null</code>, if the given newStartTimeReceived was null.
     */
    RaceDTO setStartTimeReceivedForRace(RaceIdentifier raceIdentifier, Date newStartTimeReceived)
            throws UnauthorizedException;

    /**
     * Slices a new race from the race specified by the given {@link RegattaAndRaceIdentifier} using the given time
     * range. A new {@link RaceColumn} with the given name is added to the {@link RegattaLeaderboard}.
     *
     * @throws ServiceException
     */
    RegattaAndRaceIdentifier sliceRace(RegattaAndRaceIdentifier raceIdentifier, String newRaceColumnName,
            TimePoint sliceFrom, TimePoint sliceTo) throws UnauthorizedException, ServiceException;

    /**
     * @return the new net points in {@link Pair#getA()} and the new total points in {@link Pair#getB()} for time point
     *         <code>date</code> after the max points reason has been updated to <code>maxPointsReasonAsString</code>.
     */
    Util.Triple<Double, Double, Boolean> updateLeaderboardMaxPointsReason(String leaderboardName,
            String competitorIdAsString, String raceColumnName, MaxPointsReason maxPointsReason, Date date)
            throws NoWindException, UnauthorizedException;

    /**
     * Updates given <code>tagToUpdate</code> with the given attributes <code>tag</code>, <code>comment</code>,
     * <code>imageURL</code> and <code>visibleForPublic</code>. Tags are not really updated, instead public tags are
     * revoked/private tags removed first and then the new tags gets saved depending on the new value
     * <code>visibleForPublic</code>.
     *
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param tagToUpdate
     *            tag to be updated
     * @param tag
     *            new tag title
     * @param comment
     *            new comment
     * @param hiddenInfo TODO
     * @param imageURL
     *            new image url
     * @param visibleForPublic
     *            new visibility status
     * @return <code>successful</code> {@link SuccessInfo} if tag was updated successfully, otherwise
     *         <code>non-successful</code> {@link SuccessInfo}
     */
    SuccessInfo updateTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tagToUpdate,
            String tag, String comment, String hiddenInfo, String imageURL, String resizedImageURL, boolean visibleForPublic)
            throws UnauthorizedException;

    /**
     * @param raceLogFrom
     *            identifies the race log to copy from by its leaderboard name, race column name and fleet name
     * @param raceLogsTo
     *            identifies the race log to copy from by their leaderboard name, race column name and fleet name
     * @throws NotFoundException
     */
    void copyCompetitorsToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples) throws UnauthorizedException, NotFoundException;

    /**
     * @param raceLogFrom
     *            identifies the race log to copy from by its leaderboard name, race column name and fleet name
     * @param raceLogsTo
     *            identifies the race log to copy from by their leaderboard name, race column name and fleet name
     * @throws NotFoundException
     */
    void copyCourseToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples, boolean copyMarkDeviceMappings, int priority)
            throws UnauthorizedException, NotFoundException;

    /**
     * Adds a fix to the {@link SensorFixStore}, and creates a mapping with a virtual device for exactly the current
     * timepoint.
     *
     * @param timePoint
     *            the time point for the fix; if {@code null}, the current time is used
     *
     * @throws DoesNotHaveRegattaLogException
     * @throws NotFoundException
     */
    void pingMark(String leaderboardName, MarkDTO mark, TimePoint timePoint, Position position)
            throws UnauthorizedException, DoesNotHaveRegattaLogException, NotFoundException;

    /**
     * Revoke the {@link RaceLogDenoteForTrackingEvent}. This does not affect an existing {@code RaceLogRaceTracker} or
     * {@link TrackedRace} for this {@code RaceLog}.
     *
     * @see RaceLogTrackingAdapter#removeDenotationForRaceLogTracking
     */
    void removeDenotationForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName)
            throws UnauthorizedException, NotFoundException, Exception;

    /**
     * Removes public {@link TagDTO tag} from {@link RaceLog} and private {@link TagDTO tag} from {@link UserStore}.
     *
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param tag
     *            tag to remove
     * @return <code>successful</code> {@link SuccessInfo} if tag was removed successfully, otherwise
     *         <code>non-successful</code> {@link SuccessInfo}
     */
    SuccessInfo removeTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tag)
            throws UnauthorizedException;

    boolean setStartTimeAndProcedure(RaceLogSetStartTimeAndProcedureDTO dto)
            throws UnauthorizedException, NotFoundException;

    Pair<Boolean, Boolean> setFinishingAndEndTime(RaceLogSetFinishingAndFinishTimeDTO dto)
            throws UnauthorizedException, NotFoundException;

    void setImpliedWindSource(String leaderboardName, String raceColumnName, String fleetName,
            ImpliedWindSource impliedWindSource) throws NotFoundException;

    void createOrUpdateDeviceConfiguration(DeviceConfigurationDTO configurationDTO) throws UnauthorizedException;

    List<TrackFileImportDeviceIdentifierDTO> getTrackFileImportDeviceIds(List<String> uuids)
            throws NoCorrespondingServiceRegisteredException, TransformationException;

    MarkTrackDTO getMarkTrack(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString)
            throws UnauthorizedException;

    List<DeviceMappingDTO> getDeviceMappings(String leaderboardName)
            throws UnauthorizedException, DoesNotHaveRegattaLogException, TransformationException, NotFoundException;

    void updateServerConfiguration(ServerConfigurationDTO serverConfiguration) throws UnauthorizedException;

    void deleteYellowBrickConfigurations(Collection<YellowBrickConfigurationWithSecurityDTO> singletonList);

    void updateYellowBrickConfiguration(YellowBrickConfigurationWithSecurityDTO editedObject);

    void startAICommentingOnEvent(UUID eventId);

    void stopAICommentingOnEvent(UUID eventId);

    /**
     * Event though this is a reading API method, we place it on {@link SailingServiceWrite} because it is available
     * and makes sense only on the primary/master process of a replica set. There is no replication of the {@link AIAgent}
     * itself; only its actions will be replicated. Therefore, AI agent configuration and introspection will work only
     * on the primary/master.
     */
    List<EventDTO> getIdsOfEventsWithAICommenting();

    String getAIAgentLanguageModelName();
    
    boolean hasAIAgentCredentials();
    
    void setAIAgentCredentials(String credentials) throws Exception;
    
    void resetAIAgentCredentials();

    void copyPairingListFromOtherLeaderboard(String sourceLeaderboardName, String targetLeaderboardName, String fromRaceColumnName,
            String toRaceColumnInclusiveName) throws UnauthorizedException, NotFoundException;
}
