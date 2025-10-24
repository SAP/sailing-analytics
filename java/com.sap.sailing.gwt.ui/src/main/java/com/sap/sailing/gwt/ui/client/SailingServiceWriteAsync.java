package com.sap.sailing.gwt.ui.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.UnauthorizedException;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSource;
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
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.security.SecuredDomainType.EventActions;
import com.sap.sailing.expeditionconnector.ExpeditionDeviceConfiguration;
import com.sap.sailing.gwt.ui.adminconsole.RaceLogSetTrackingTimesDTO;
import com.sap.sailing.gwt.ui.adminconsole.RemoteSailingServerEventsSelectionDialog;
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
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.filestorage.FileStorageManagementGwtServiceAsync;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.ImageResizingTaskDTO;
import com.sap.sse.gwt.client.media.VideoDTO;
import com.sap.sse.pairinglist.PairingList;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.ui.shared.SuccessInfo;

public interface SailingServiceWriteAsync extends FileStorageManagementGwtServiceAsync, SailingServiceAsync {

    void addCourseDefinitionToRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            List<Util.Pair<ControlPointDTO, PassingInstruction>> course, int priority, AsyncCallback<Void> callback)
            throws UnauthorizedException;

    void addMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO newFix, AsyncCallback<Void> callback) throws UnauthorizedException;

    void addMarkToRegattaLog(String leaderboardName, MarkDTO mark, AsyncCallback<Void> callback)
            throws UnauthorizedException/*, DoesNotHaveRegattaLogException*/;

    void addOrReplaceExpeditionDeviceConfiguration(ExpeditionDeviceConfiguration expeditionDeviceConfiguration, AsyncCallback<Void> callback)
            throws UnauthorizedException;

    void addOrUpdateBoat(BoatDTO boat, AsyncCallback<BoatDTO> callback) throws UnauthorizedException;

    void addOrUpdateCompetitors(List<CompetitorDTO> competitors, AsyncCallback<List<CompetitorDTO>> callback) throws UnauthorizedException;

    void addOrUpdateCompetitorWithBoat(CompetitorWithBoatDTO competitor, AsyncCallback<CompetitorWithBoatDTO> callback)
            throws UnauthorizedException;

    void addOrUpdateCompetitorWithoutBoat(CompetitorDTO competitor, AsyncCallback<CompetitorDTO> callback) throws UnauthorizedException;

    void addOrUpdateMarkProperties(MarkPropertiesDTO markProperties, AsyncCallback<MarkPropertiesDTO> callback);

    void addOrUpdateMarkTemplate(MarkTemplateDTO markTemplate, AsyncCallback<MarkTemplateDTO> callback);

    void addRaceColumnsToSeries(RegattaIdentifier regattaIdentifier, String seriesName,
            List<Pair<String, Integer>> columnNames, AsyncCallback<List<RaceColumnInSeriesDTO>> callback);

    void addResultImportUrl(String resultProviderName, UrlDTO url, AsyncCallback<Void> callback) throws UnauthorizedException/*, Exception*/;

    void addTag(String leaderboardName, String raceColumnName, String fleetName, String tag, String comment,
            String hiddenInfo, String imageURL, String resizedImageURL, boolean visibleForPublic,
            TimePoint raceTimepoint, AsyncCallback<SuccessInfo> callback) throws UnauthorizedException;

    void addCompetitors(List<CompetitorDescriptor> competitorsForSaving, String searchTag, AsyncCallback<List<CompetitorWithBoatDTO>> callback)
            throws UnauthorizedException;

    void addColumnsToLeaderboard(String leaderboardName, List<Util.Pair<String, Boolean>> columnsToAdd, AsyncCallback<Void> callback)
            throws UnauthorizedException;

    void addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace, AsyncCallback<Void> callback)
            throws UnauthorizedException;

    void addTypedDeviceMappingToRegattaLog(String leaderboardName, TypedDeviceMappingDTO dto, AsyncCallback<Void> callback)
    /*throws NoCorrespondingServiceRegisteredException, TransformationException, DoesNotHaveRegattaLogException,
            NotFoundException*/;

    void addDeviceMappingToRegattaLog(String leaderboardName, DeviceMappingDTO dto, AsyncCallback<Void> callback)
            /*throws NoCorrespondingServiceRegisteredException, TransformationException, DoesNotHaveRegattaLogException*/;


    void allowBoatResetToDefaults(List<BoatDTO> boats, AsyncCallback<Void> callback) throws UnauthorizedException;

    void setTrackingTimes(RaceLogSetTrackingTimesDTO dto, AsyncCallback<Void> callback);

    /**
     * Copies one race's competitor set to other races.
     * @param fromTriple
     *            leaderboard name, race column name, and fleet name identifying the race from which to copy the
     *            competitors
     * @param toTriples leaderboard name, race column name, and fleet name identifying the races to which to copy the
     *            competitors
     */
    void copyCompetitorsToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples, AsyncCallback<Void> callback);

    void copyCourseToOtherRaceLogs(Triple<String, String, String> fromTriple,
            Set<Triple<String, String, String>> toTriples, boolean copyMarkDeviceMappings, int priority, AsyncCallback<Void> callback);

    void revokeMarkDefinitionEventInRegattaLog(String leaderboardName, String raceColumnName, String fleetName,
            MarkDTO markDTO, AsyncCallback<Void> callback);

    void disableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Void> asyncCallback);

    void enableCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Void> asyncCallback);

                            void setBoatRegistrationsInRegattaLog(String leaderboardName, Set<BoatDTO> boats,
            AsyncCallback<Void> callback);

    void editMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO oldFix, Position newPosition, AsyncCallback<Void> callback);

    void removeMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO fix, AsyncCallback<Void> callback);

    void setEliminatedCompetitors(String leaderboardName, Set<CompetitorDTO> eliminatedCompetitors, AsyncCallback<Void> callback);

    void removeExpeditionDeviceConfiguration(ExpeditionDeviceConfiguration expeditionDeviceConfiguration, AsyncCallback<Void> asyncCallback);

            /**
     * Registers all competitors of a {@link PairingList} in the respective {@link RaceColumn}s and {@link Fleet}s.
     *
     * @param leaderboardName
     *            the name of the leaderboard
     * @param flightMultiplier
     *            specifies how often the flights will be cloned
     * @param callback
     */
    void fillRaceLogsFromPairingListTemplate(final String leaderboardName, final int flightMultiplier,
            final List<String> selectedFlightNames,PairingListDTO pairingListDTO, AsyncCallback<Void> callback);

    void sliceRace(RegattaAndRaceIdentifier raceIdentifier, String newRaceColumnName, TimePoint sliceFrom, TimePoint sliceTo,
            AsyncCallback<RegattaAndRaceIdentifier> callback);


    /**
     * @see SailingService#resizeImage(ImageResizingTaskDTO)
     * @param imageResizingTask the information on how the contained ImageDTO should be resized
     * @param asyncCallback The callback called after finishing resizing, storing the returned ImageDTOs somewhere is proposed
     */
    void resizeImage(ImageResizingTaskDTO imageResizingTask, AsyncCallback<Set<ImageDTO>> asyncCallback);
    void updateGroupOwnerForEventHierarchy(UUID eventId,
            MigrateGroupOwnerForHierarchyDTO migrateGroupOwnerForHierarchyDTO, AsyncCallback<Void> callback);

    void updateGroupOwnerForLeaderboardGroupHierarchy(UUID leaderboardGroupId,
            MigrateGroupOwnerForHierarchyDTO migrateGroupOwnerForHierarchyDTO, AsyncCallback<Void> callback);

    void getSecretForRegattaByName(String leaderboardName, AsyncCallback<String> asyncCallback);

    void setORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier raceIdentifier,
            Map<Integer, ORCPerformanceCurveLegImpl> legInfo, AsyncCallback<Void> callback);

    void setORCPerformanceCurveLegInfo(String leaderboardName, String raceColumnName, String fleetName,
            Map<Integer, ORCPerformanceCurveLegImpl> legInfo, AsyncCallback<Void> callback);

    void assignORCPerformanceCurveCertificates(RegattaIdentifier regattaIdentifier,
            Map<String, ORCCertificate> certificatesForBoatsWithIdAsString,
            AsyncCallback<Triple<Integer, Integer, Integer>> callback);

    void assignORCPerformanceCurveCertificates(String leaderboardName, String raceColumnName, String fleetName,
            Map<String, ORCCertificate> certificatesForBoatsWithIdAsString,
            AsyncCallback<Triple<Integer, Integer, Integer>> callback);

    void getORCPerformanceCurveScratchBoat(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<CompetitorDTO> asyncCallback);

    void setORCPerformanceCurveScratchBoat(String leaderboardName, String raceColumnName, String fleetName, CompetitorDTO newScratchBoat,
            AsyncCallback<Void> asyncCallback);

    void updateMarkPropertiesPositioning(UUID markPropertiesId, DeviceIdentifierDTO deviceIdentifier, Position position, AsyncCallback<MarkPropertiesDTO> asyncCallback);

    void createOrUpdateCourseTemplate(CourseTemplateDTO courseTemplate, AsyncCallback<CourseTemplateDTO> asyncCallback);

    /**
     * Remove course templates by UUIDs
     *
     * @param courseTemplatesUuids
     *            the {@link Collection} of course templates' UUIDs which will be remove
     * @param asyncCallback
     *            {@link AsyncCallback} object
     */
    void removeCourseTemplates(Collection<UUID> courseTemplatesUuids, AsyncCallback<Void> asyncCallback);

    void removeSailingServers(Set<String> toRemove, AsyncCallback<Void> callback);

    void addRemoteSailingServerReference(RemoteSailingServerReferenceDTO sailingServer,
            AsyncCallback<RemoteSailingServerReferenceDTO> callback);

    /**
     * Updates {@link RemoteSailingServerReferenceDTO} sailingServer instance based on user selection regarding
     * inclusion type and selected events.
     */
    void updateRemoteSailingServerReference(RemoteSailingServerReferenceDTO sailingServer,
            AsyncCallback<RemoteSailingServerReferenceDTO> callback);

    /**
     * Loads remote sailing server data with all events not filtered by selection in order to show the full list of
     * events on {@link RemoteSailingServerEventsSelectionDialog} dialog.
     */
    void getCompleteRemoteServerReference(String sailingServerName,
            AsyncCallback<RemoteSailingServerReferenceDTO> callback);

    /**
     * Remove mark properties by UUIDs
     *
     * @param markPropertiesUuids
     *            the {@link Collection} of mark properties' UUIDs which will be remove
     * @param asyncCallback
     *            {@link AsyncCallback} object
     */
    void removeMarkProperties(Collection<UUID> markPropertiesUuids, AsyncCallback<Void> asyncCallback);

    void createMarkRole(MarkRoleDTO markRole, AsyncCallback<MarkRoleDTO> asyncCallback);

    /**
     * @param regattaToAddTo
     *            if <code>null</code>, the regatta into which the race has previously been loaded will be looked up; if
     *            found, the race will be loaded into that regatta; otherwise, an existing regatta by the name of the
     *            TracTrac event with the boat class name appended in parentheses will be looked up; if not found, a
     *            default regatta with that name will be created, with a single default series and a single default
     *            fleet. If a valid {@link RegattaIdentifier} is specified, a regatta lookup is performed with that
     *            identifier; if the regatta is found, it is used to add the races to. Otherwise, a default regatta as
     *            described above will be created and used.
     * @param liveURI
     *            may be <code>null</code> or the empty string in which case the server will use the
     *            {@link TracTracRaceRecordDTO#liveURI} from the <code>rr</code> race record.
     * @param offsetToStartTimeOfSimulatedRace
     *            if not <code>null</code>, the connector will adjust the time stamps of all events received such that
     *            the first mark passing for the first waypoint will be set to "now." It will delay the forwarding of
     *            all events received such that they seem to be sent in "real-time" plus the
     *            <code>offsetToStartTimeOfSimulatedRace</code>. So, more or less the time points attached to the events
     *            sent to the receivers will again approximate the wall time.
     * @param useInternalMarkPassingAlgorithm
     *            whether or not to ignore the TracTrac-provided mark passings; if <code>true</code>, a separate mark
     *            passing calculator is used, and the TracTrac-provided ones are ignored.
     * @param storedURI
     *            may be <code>null</code> or the empty string in which case the server will use the
     *            {@link TracTracRaceRecordDTO#storedURI} from the <code>rr</code> race record.
     * @param jsonUrlAsKey
     *            identifies the TracTrac connection parameters which in particular can be used to obtain the
     *            username/password combination for the update URI because we don't send these to the client
     */
    void trackWithTracTrac(RegattaIdentifier regattaToAddTo, List<TracTracRaceRecordDTO> rrs, String liveURI,
            String storedURI, String courseDesignUpdateURI, boolean trackWind, boolean correctWindByDeclination,
            Duration offsetToStartTimeOfSimulatedRace, boolean useInternalMarkPassingAlgorithm,
            boolean useOfficialEventsToUpdateRaceLog, String jsonUrlAsKey, AsyncCallback<Void> callback);

    void trackWithSwissTiming(RegattaIdentifier regattaToAddTo, List<SwissTimingRaceRecordDTO> rrs, String hostname,
            int port, boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm,
            String updateURL, String updateUsername, String updatePassword, String eventName,
            String manage2SailEventUrl, AsyncCallback<Void> asyncCallback);

    void createTracTracConfiguration(String name, String jsonURL, String liveDataURI, String storedDataURI,
            String courseDesignUpdateURI, String tracTracUsername, String tracTracPassword, AsyncCallback<Void> callback);

    /**
     * @param creatorUserName
     *            together with the {@code raceUrl} this is used to identify the connection and obtain it's
     *            username/password combination on the server; this is essential because we don't send those credentials
     *            to the client
     */
    void trackWithYellowBrick(RegattaIdentifier regattaToAddTo, List<YellowBrickRaceRecordDTO> rrs, boolean trackWind,
            boolean correctWindByDeclination, String creatorUserName, String raceUrl,
            AsyncCallback<Void> callback);

    void createYellowBrickConfiguration(String name, String yellowBrickRaceUrl, String yellowBrickUsername,
            String yellowBrickPassword, AsyncCallback<Void> callback);

    void deleteTracTracConfigurations(Collection<TracTracConfigurationWithSecurityDTO> tracTracConfigurations,
            AsyncCallback<Void> callback);

    /**
     * @param tracTracConfiguration when the {@link TracTracConfigurationWithSecurityDTO#getTracTracPassword() password}
     * field of the configuration is {@code null}, the password will not be updated.
     */
    void updateTracTracConfiguration(TracTracConfigurationWithSecurityDTO tracTracConfiguration,
            AsyncCallback<Void> callback);

    void stopTrackingRaces(List<RegattaAndRaceIdentifier> racesToStopTracking, AsyncCallback<Void> asyncCallback);

    /**
     * Untracks the race and removes it from the regatta. It will also be removed in all leaderboards
     *
     * @param regattaNamesAndRaceNames
     *            The identifier for the regatta name, and the race name to remove
     */
    void removeAndUntrackRaces(List<RegattaNameAndRaceName> regattaNamesAndRaceNames, AsyncCallback<Void> callback);

    void setWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO wind, AsyncCallback<Void> callback);

    void removeWind(RegattaAndRaceIdentifier raceIdentifier, WindDTO windDTO, AsyncCallback<Void> callback);


    void updateLeaderboard(String leaderboardName, String newLeaderboardDisplayName, int[] newDiscardingThreasholds,
            List<UUID> newCourseAreaIds, AsyncCallback<StrippedLeaderboardDTO> callback);

    void createFlexibleLeaderboard(String leaderboardName, String leaderboardDisplayName,
            int[] discardThresholds,
            ScoringSchemeType scoringSchemeType, List<UUID> courseAreaIds,
            AsyncCallback<StrippedLeaderboardDTO> asyncCallback);

    void createRegattaLeaderboard(RegattaName regattaIdentifier,
            String leaderboardDisplayName,
            int[] discardThresholds, AsyncCallback<StrippedLeaderboardDTO> asyncCallback);

    void createRegattaLeaderboardWithEliminations(String name, String displayName,
            String regattaName, AsyncCallback<StrippedLeaderboardDTO> asyncCallback);

    void createRegattaLeaderboardWithOtherTieBreakingLeaderboard(RegattaName regattaIdentifier,
            String leaderboardDisplayName, int[] discardThresholds, String otherTieBreakingLeaderboardName,
            AsyncCallback<StrippedLeaderboardDTO> callback);

    void removeLeaderboard(String leaderboardName, AsyncCallback<Void> asyncCallback);

    void removeLeaderboards(Collection<String> leaderboardNames, AsyncCallback<Void> asyncCallback);

    void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName,
            AsyncCallback<Void> callback);

    void removeLeaderboardColumn(String leaderboardName, String columnName, AsyncCallback<Void> callback);

    void connectTrackedRaceToLeaderboardColumn(String leaderboardName, String raceColumnName, String fleetName,
            RegattaAndRaceIdentifier raceIdentifier, AsyncCallback<Boolean> asyncCallback);

    void disconnectLeaderboardColumnFromTrackedRace(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Void> callback);

    void updateLeaderboardCarryValue(String leaderboardName, String competitorIdAsString, Double carriedPoints,
            AsyncCallback<Void> callback);

    /**
     * @param asyncCallback
     *            The result is a {@link Triple} with the new total points in {@link Triple#getA() a}, the new net
     *            points in {@link Triple#getB() b}, and whether or not the result is obtained from the correction
     *            ("isCorrected") as {@link Triple#getC() c}.
     */
    void updateLeaderboardMaxPointsReason(String leaderboardName, String competitorIdAsString, String raceColumnName,
            MaxPointsReason maxPointsReason, Date date,
            AsyncCallback<Util.Triple<Double, Double, Boolean>> asyncCallback);

    /**
     * @param asyncCallback
     *            The result is a {@link Triple} with the new total points in {@link Triple#getA() a}, the new net
     *            points in {@link Triple#getB() b}, and whether or not the result is obtained from the correction
     *            ("isCorrected") as {@link Triple#getC() c}.
     */
    void updateLeaderboardScoreCorrection(String leaderboardName, String competitorIdAsString, String columnName,
            Double correctedScore, Date date, AsyncCallback<Util.Triple<Double, Double, Boolean>> asyncCallback);

    /**
     * @param asyncCallback
     *            The result is a {@link Triple} with the new total points in {@link Triple#getA() a}, the new net
     *            points in {@link Triple#getB() b}, and whether or not the result is obtained from the correction
     *            ("isCorrected") as {@link Triple#getC() c}.
     */
    void updateLeaderboardIncrementalScoreCorrection(String leaderboardName, String competitorIdAsString,
            String columnName, Double scoringOffsetInPoints, Date date,
            AsyncCallback<Triple<Double, Double, Boolean>> callback);

    void updateLeaderboardScoreCorrectionMetadata(String leaderboardName, Date timePointOfLastCorrectionValidity,
            String comment, AsyncCallback<Void> callback);

    void updateLeaderboardScoreCorrectionsAndMaxPointsReasons(BulkScoreCorrectionDTO updates,
            AsyncCallback<Void> callback);

    void updateCompetitorDisplayNameInLeaderboard(String leaderboardName, String competitorID, String displayName,
            AsyncCallback<Void> callback);

    void moveLeaderboardColumnUp(String leaderboardName, String columnName, AsyncCallback<Void> callback);

    void moveLeaderboardColumnDown(String leaderboardName, String columnName, AsyncCallback<Void> callback);

    void updateIsMedalRace(String leaderboardName, String columnName, boolean isMedalRace, AsyncCallback<Void> callback);

    void updateRacesDelayToLive(List<RegattaAndRaceIdentifier> regattaAndRaceIdentifiers, long delayToLiveInMs,
            AsyncCallback<Void> callback);

    void createSwissTimingConfiguration(String configName, String jsonURL, String hostname, Integer port,
            String updateURL, String updateUsername, String updatePassword, AsyncCallback<Void> asyncCallback);

    void updateSwissTimingConfiguration(SwissTimingConfigurationWithSecurityDTO configuration,
            AsyncCallback<Void> asyncCallback);

    void deleteSwissTimingConfigurations(Collection<SwissTimingConfigurationWithSecurityDTO> configurations,
            AsyncCallback<Void> asyncCallback);

    /**
     * Removes the leaderboard groups with the given names from the service and the persistant store.
     */
    void removeLeaderboardGroups(Set<UUID> groupIds, AsyncCallback<Void> asyncCallback);

    /**
     * Creates a new group with the name <code>groupname</code>, the description <code>description</code> and an empty
     * list of leaderboards.<br/>
     */
    void createLeaderboardGroup(String groupName, String description, String displayName,
            boolean displayGroupsInReverseOrder, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType, AsyncCallback<LeaderboardGroupDTO> callback);

    void updateLeaderboardGroup(UUID leaderboardGroupId, String oldName, String newName, String description, String newDisplayName,
            List<String> leaderboardNames, int[] overallLeaderboardDiscardThresholds,
            ScoringSchemeType overallLeaderboardScoringSchemeType, AsyncCallback<Void> callback);

    void setRaceIsKnownToStartUpwind(RegattaAndRaceIdentifier raceIdentifier, boolean raceIsKnownToStartUpwind,
            AsyncCallback<Void> callback);

    void setWindSourcesToExclude(RegattaAndRaceIdentifier raceIdentifier, List<WindSource> windSourcesToExclude,
            AsyncCallback<Void> callback);


    /**
     * Renames the event with the name <code>oldName</code> to the <code>newName</code>.<br />
     * If there's no event with the name <code>oldName</code> or there's already a event with the name
     * <code>newName</code> a {@link IllegalArgumentException} is thrown.
     */
    void renameEvent(UUID eventId, String newName, AsyncCallback<Void> callback);

    /**
     * Removes the event with the id <code>id</code> from the service and the persistence store.
     */
    void removeEvent(UUID eventId, AsyncCallback<Void> callback);

    void removeEvents(Collection<UUID> eventIds, AsyncCallback<Void> asyncCallback);

    void createEvent(String eventName, String eventDescription, Date startDate, Date endDate, String venue,
            boolean isPublic, List<CourseAreaDTO> courseAreas, String officialWebsiteURL, String baseURL,
            Map<String, String> sailorsInfoWebsiteURLsByLocaleName, List<? extends ImageDTO> images,
            List<? extends VideoDTO> videos, List<UUID> leaderboardGroupIDs,
            AsyncCallback<EventDTO> callback);

    /**
     * Extracts all fields from {@code eventDTO}, then delegates to
     * {@link #updateEvent(UUID, String, String, Date, Date, VenueDTO, boolean, List, String, String, Map, List, List, List, AsyncCallback)}.
     */
    void updateEvent(EventDTO eventDTO, AsyncCallback<EventDTO> callback);

    /**
     * Updates the {@link Event} identified by {@code eventId}. This generally requires the
     * {@link SecuredDomainType#EVENT EVENT}{@code :}{@link DefaultActions#UPDATE UPDATE} permission for the event
     * identified by {@code eventId}; however, if the calling subject has at least the {@link SecuredDomainType#EVENT
     * EVENT}{@code :}{@link EventActions#UPLOAD_MEDIA UPLOAD_MEDIA} permission then changes to the {@code images} and
     * {@code videos} are permitted; if the user only has the {@link EventActions#UPLOAD_MEDIA UPLOAD_MEDIA} but not the
     * more general {@link DefaultActions#UPDATE UPDATE} permission, changes other than for {@code images} and
     * {@code videos} will cause a {@link UnauthorizedException} to be thrown.
     */
    void updateEvent(UUID eventId, String eventName, String eventDescription, Date startDate, Date endDate,
            VenueDTO venue, boolean isPublic, List<UUID> leaderboardGroupIds, String officialWebsiteURL,
            String baseURL, Map<String, String> sailorsInfoWebsiteURLsByLocaleName, List<? extends ImageDTO> images,
            List<? extends VideoDTO> videos, List<String> windFinderReviewedSpotCollectionIds, AsyncCallback<EventDTO> callback);

    void createCourseAreas(UUID eventId, List<CourseAreaDTO> courseAreas, AsyncCallback<Void> callback);

    void removeCourseAreas(UUID eventId, UUID[] idsOfCourseAreasToRemove, AsyncCallback<Void> callback);

    void removeRegatta(RegattaIdentifier regattaIdentifier, AsyncCallback<Void> callback);

    void removeRegattas(Collection<RegattaIdentifier> regattas, AsyncCallback<Void> asyncCallback);

    void createRegatta(String regattaName, String boatClassName, boolean canBoatsOfCompetitorsChangePerRace,
            CompetitorRegistrationType competitorRegistrationType, String registrationLinkSecret, Date startDate, Date endDate,
            RegattaCreationParametersDTO seriesNamesWithFleetNamesAndFleetOrderingAndMedal, boolean persistent,
            ScoringSchemeType scoringSchemeType, List<UUID> courseAreaIds, Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange,
            RankingMetrics rankingMetricType, AsyncCallback<RegattaDTO> callback);

    void updateSeries(RegattaIdentifier regattaIdentifier, String seriesName, String newSeriesName, boolean isMedal,
            boolean isFleetsCanRunInParallel, int[] resultDiscardingThresholds, boolean startsWithZeroScore,
            boolean firstRaceIsNonDiscardableCarryForward, boolean hasSplitFleetScore, boolean hasCrossFleetMergedRanking, Integer maximumNumberOfDiscards,
            boolean oneAlwaysStaysOne, List<FleetDTO> fleets, AsyncCallback<Void> callback);

    void removeRaceColumnsFromSeries(RegattaIdentifier regattaIdentifier, String seriesName, List<String> columnNames,
            AsyncCallback<Void> callback);

    void updateRaceCourse(RegattaAndRaceIdentifier raceIdentifier,
            List<Util.Pair<ControlPointDTO, PassingInstruction>> controlPoints, AsyncCallback<Void> callback);

    void removeResultImportURLs(String resultProviderName, Set<UrlDTO> toRemove, AsyncCallback<Void> callback);

    void suppressCompetitorInLeaderboard(String leaderboardName, String competitorIdAsString, boolean suppressed,
            AsyncCallback<Void> asyncCallback);

    void updateLeaderboardColumnFactor(String leaderboardName, String columnName, Double newFactor,
            AsyncCallback<Void> callback);

    void createSwissTimingArchiveConfiguration(String jsonUrl, AsyncCallback<Void> asyncCallback);

    void updateSwissTimingArchiveConfiguration(SwissTimingArchiveConfigurationWithSecurityDTO dto,
            AsyncCallback<Void> asyncCallback);

    void deleteSwissTimingArchiveConfigurations(Collection<SwissTimingArchiveConfigurationWithSecurityDTO> dtos,
            AsyncCallback<Void> asyncCallback);

    void updateRegatta(RegattaIdentifier regattaIdentifier, Date startDate, Date endDate, List<UUID> courseAreaUuids,
            RegattaConfigurationDTO regattaConfiguration, Double buoyZoneRadiusInHullLengths,
            boolean useStartTimeInference, boolean controlTrackingFromStartAndFinishTimes,
            boolean autoRestartTrackingUponCompetitorSetChange, String registrationLinkSecret,
            CompetitorRegistrationType registrationType, AsyncCallback<Void> callback);

    void importMasterData(String host, UUID[] leaderboardGroupIds, boolean override, boolean compress, boolean exportWind,
            boolean exportDeviceConfigurations, String targetServerUsername, String targetServerPassword,
            boolean exportTrackedRacesAndStartTracking, AsyncCallback<UUID> asyncCallback);

    void getImportOperationProgress(UUID id, AsyncCallback<DataImportProgress> asyncCallback);

    void allowCompetitorResetToDefaults(List<CompetitorDTO> competitors, AsyncCallback<Void> asyncCallback);

    void removeDeviceConfiguration(UUID deviceConfigurationId, AsyncCallback<Boolean> asyncCallback);

    void addIgtimiDataAccessWindow(String deviceSerialNumber, Date from, Date to, AsyncCallback<IgtimiDataAccessWindowWithSecurityDTO> asyncCallback);

    void removeIgtimiDataAccessWindow(long id, AsyncCallback<Void> asyncCallback);

    void updateIgtimiDevice(IgtimiDeviceWithSecurityDTO editedObject, AsyncCallback<Void> asyncCallback);

    void removeIgtimiDevice(String deviceSerialNumber, AsyncCallback<Void> asyncCallback);

    void importWindFromIgtimi(List<RaceDTO> selectedRaces, boolean correctByDeclination,
            String optionalBearerTokenOrNull, AsyncCallback<Map<RegattaAndRaceIdentifier, Integer>> asyncCallback);

    /**
     * The boolean result reflects whether a connection to the device identified by {@code serialNumber}
     * was found on the local Igtimi Riot service; if {@code false}, the message may still have been delivered
     * through another replica to which the device maintains a live connection.
     */
    void sendGPSOffCommandToIgtimiDevice(String serialNumber, AsyncCallback<Boolean> callback);

    /**
     * The boolean result reflects whether a connection to the device identified by {@code serialNumber}
     * was found on the local Igtimi Riot service; if {@code false}, the message may still have been delivered
     * through another replica to which the device maintains a live connection.
     */
    void sendGPSOnCommandToIgtimiDevice(String serialNumber, AsyncCallback<Boolean> callback);

    /**
     * The boolean result reflects whether a connection to the device identified by {@code serialNumber}
     * was found on the local Igtimi Riot service; if {@code false}, the message may still have been delivered
     * through another replica to which the device maintains a live connection.
     */
    void sendPowerOffCommandToIgtimiDevice(String serialNumber, AsyncCallback<Boolean> callback);

    /**
     * The boolean result reflects whether a connection to the device identified by {@code serialNumber}
     * was found on the local Igtimi Riot service; if {@code false}, the message may still have been delivered
     * through another replica to which the device maintains a live connection.
     */
    void sendRestartCommandToIgtimiDevice(String serialNumber, AsyncCallback<Boolean> callback);
    
    /**
     * Sends the command sequence to calibrate the IMU of the Igtimi device identified by {@code serialNumber}.
     * Waits 1s in between commands to allow the device to process them. So, expect the method invocation to take
     * at least 5 seconds.
     * <p>
     * 
     * The boolean result reflects whether a connection to the device identified by {@code serialNumber} was found on
     * the local Igtimi Riot service; if {@code false}, the message may still have been delivered through another
     * replica to which the device maintains a live connection.
     */
    void sendIMUCalibrationCommandSequenceToIgtimiDevice(String serialNumber, AsyncCallback<Boolean> callback);

    /**
     * Sends a "freestyle" command to the Igtimi device identified by {@code serialNumber}.
     */
    void sendIgtimiCommand(String serialNumber, String command, AsyncCallback<Boolean> asyncCallback);
    
    void enableIgtimiDeviceOverTheAirLog(String deviceSerialNumber, boolean enable, AsyncCallback<Boolean> asyncCallback);
    
    void getIgtimiDeviceLogs(String serialNumber, Duration duration, AsyncCallback<ArrayList<Pair<TimePoint, String>>> asyncCallback);

    /**
     * @return {@code true} if the race was not yet denoted for race log tracking and now has successfully been denoted
     *         so
     */
    void denoteForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Boolean> callback);

    void denoteForRaceLogTracking(String leaderboardName,String prefix, AsyncCallback<Void> callback);

    void startRaceLogTracking(List<Triple<String, String, String>> leaderboardRaceColumnFleetNames,
            final boolean trackWind, final boolean correctWindByDeclination, AsyncCallback<Void> callback);

    /**
     * Set the competitor (with contained boats) registrations in the racelog. Unregisters formerly registered competitors that are not listed
     * in {@code competitors}.
     */
    void setCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            Set<CompetitorWithBoatDTO> competitors, AsyncCallback<Void> callback);

    /**
     * Set the competitor and boat registrations in the racelog. Unregisters formerly registered competitors that are not listed
     * in {@code competitors}.
     */
    void setCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            Map<? extends CompetitorDTO, BoatDTO> competitorsAndBoats, AsyncCallback<Void> callback);

    /**
     * Set the competitor registrations in the leaderboard. Unregisters formerly registered competitors that are not
     * listed in {@code competitors}.
     */
    void setCompetitorRegistrationsInRegattaLog(String leaderboardName, Set<? extends CompetitorDTO> competitors,
            AsyncCallback<Void> callback);

    void updateTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tagToUpdate, String tag,
            String comment, String hiddenInfo, String imageURL, String resizedImageURL, boolean visibleForPublic, AsyncCallback<SuccessInfo> asyncCallback);

    void removeTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tag,
            AsyncCallback<SuccessInfo> asyncCallback);

    void pingMark(String leaderboardName, MarkDTO mark,
            TimePoint timePoint, Position position, AsyncCallback<Void> callback);

    void removeSeries(RegattaIdentifier regattaIdentifier, String seriesName, AsyncCallback<Void> callback);

    void removeDenotationForRaceLogTracking(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Void> callback);

    void setStartTimeReceivedForRace(RaceIdentifier raceIdentifier, Date newStartTimeReceived,
            AsyncCallback<RaceDTO> callback);

    void updateSuppressedMarkPassings(String leaderboardName, String raceColumnName, String fleetName,
            Integer newZeroBasedIndexOfSuppressedMarkPassing, CompetitorDTO competitorDTO, AsyncCallback<Void> callback);

    void createRegattaStructure(List<RegattaDTO> regattaNames, EventDTO newEvent,
            AsyncCallback<Void> asyncCallback);

    void updateFixedMarkPassing(String leaderboardName, String raceColumnName, String fleetName,
            Integer indexOfWaypoint, Date dateOfMarkPassing, CompetitorDTO competitorDTO, AsyncCallback<Void> callback);

    void inviteCompetitorsForTrackingViaEmail(String serverUrlWithoutTrailingSlash, EventDTO event,
            String leaderboardName, Collection<CompetitorDTO> competitors, String iOSAppUrl, String androidAppUrl,
            String localeInfo,
            AsyncCallback<Void> callback);

    void inviteBuoyTenderViaEmail(String serverUrlWithoutTrailingSlash, EventDTO eventDto, String leaderboardName,
            String emails, String iOSAppUrl, String androidAppUrl, String localeInfoName, AsyncCallback<Void> callback);

    void revokeRaceAndRegattaLogEvents(String leaderboardName, List<UUID> eventIds, AsyncCallback<Void> callback);

    void closeOpenEndedDeviceMapping(String leaderboardName, DeviceMappingDTO mappingDto, Date closingTimePoint,
            AsyncCallback<Void> asyncCallback);

    void createOrUpdateDeviceConfiguration(DeviceConfigurationDTO configurationDTO, AsyncCallback<Void> callback);

    /**
     * Sets the a new start time.
     *
     * @param dto
     *            {@link RaceLogSetStartTimeAndProcedureDTO} identifying the race to set the start time on and the new
     *            start time.
     */
    void setStartTimeAndProcedure(RaceLogSetStartTimeAndProcedureDTO dto, AsyncCallback<Boolean> callback);

    /**
     * Sets the a new finishing and end time.
     *
     * @param dto
     *            {@link RaceLogSetFinishingAndFinishTimeDTO} identifying the race and the new finishing and
     *            end time.
     */
    void setFinishingAndEndTime(RaceLogSetFinishingAndFinishTimeDTO editedObject, AsyncCallback<Pair<Boolean, Boolean>> asyncCallback);

    void setImpliedWindSource(String leaderboardName, String raceColumnName, String fleetName,
            ImpliedWindSource impliedWindSource, AsyncCallback<Void> callback);

    void getTrackFileImportDeviceIds(List<String> uuids,
            AsyncCallback<List<TrackFileImportDeviceIdentifierDTO>> callback);

    void getMarkTrack(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            AsyncCallback<MarkTrackDTO> callback);

    void getDeviceMappings(String leaderboardName, AsyncCallback<List<DeviceMappingDTO>> callback);

    void updateServerConfiguration(ServerConfigurationDTO serverConfiguration, AsyncCallback<Void> callback);

    void deleteYellowBrickConfigurations(Collection<YellowBrickConfigurationWithSecurityDTO> selectedSet, AsyncCallback<Void> callback);

    /**
     * @param editedObject
     *            if the {@link YellowBrickConfigurationWithSecurityDTO#getPassword() password} field of the
     *            configuration object is {@code null}, this means that the original password is to be left unchanged.
     */
    void updateYellowBrickConfiguration(YellowBrickConfigurationWithSecurityDTO editedObject, AsyncCallback<Void> callback);
    
    void startAICommentingOnEvent(UUID eventId, AsyncCallback<Void> callback);
    
    void stopAICommentingOnEvent(UUID eventId, AsyncCallback<Void> callback);
    
    void getIdsOfEventsWithAICommenting(AsyncCallback<List<EventDTO>> callback);

    /**
     * If {@code null} is returned to the callback's {@link AsyncCallback#onSuccess(Object) onSuccess} method, this
     * means that the AI agent hasn't been initialized properly. The typical reason for this will be missing AI Core
     * credentials but may also be issues with finding a valid deployment of a language model to use. In any case, if
     * this method has delivered a {@code null} model name then the
     * {@link #startAICommentingOnEvent(UUID, AsyncCallback)}, {@link #stopAICommentingOnEvent(UUID, AsyncCallback)} and
     * {@link #getIdsOfEventsWithAICommenting(AsyncCallback)} methods must not be called, or you may see exceptions
     * being thrown.
     */
    void getAIAgentLanguageModelName(AsyncCallback<String> callback);
    
    void hasAIAgentCredentials(AsyncCallback<Boolean> callback);

    void setAIAgentCredentials(String credentials, AsyncCallback<Void> callback);
    
    void resetAIAgentCredentials(AsyncCallback<Void> callback);
    
    void copyPairingListFromOtherLeaderboard(String sourceLeaderboardName, String targetLeaderboardName, String fromRaceColumnName,
            String toRaceColumnInclusiveName, AsyncCallback<Void> asyncCallback);
}
