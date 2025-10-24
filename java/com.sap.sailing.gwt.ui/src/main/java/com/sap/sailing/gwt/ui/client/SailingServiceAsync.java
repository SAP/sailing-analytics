package com.sap.sailing.gwt.ui.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.AuthorizationException;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.MailInvitationType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorAndBoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.IncrementalOrFullLeaderboardDTO;
import com.sap.sailing.domain.common.dto.PairingListDTO;
import com.sap.sailing.domain.common.dto.PairingListTemplateDTO;
import com.sap.sailing.domain.common.dto.PersonDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.orc.ImpliedWindSource;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.OwnMaxImpliedWind;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.tracking.impl.PreciseCompactGPSFixMovingImpl.PreciseCompactPosition;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.expeditionconnector.ExpeditionDeviceConfiguration;
import com.sap.sailing.gwt.ui.shared.BearingWithConfidenceDTO;
import com.sap.sailing.gwt.ui.shared.CompactBoatPositionsDTO;
import com.sap.sailing.gwt.ui.shared.CompactRaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorProviderDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorsRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegType;
import com.sap.sailing.gwt.ui.shared.IgtimiDataAccessWindowWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.IgtimiDeviceWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.QRCodeEvent;
import com.sap.sailing.gwt.ui.shared.RaceCourseDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sailing.gwt.ui.shared.RaceLogDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceboardDataDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.RegattaLogDTO;
import com.sap.sailing.gwt.ui.shared.RegattaOverviewEntryDTO;
import com.sap.sailing.gwt.ui.shared.RegattaScoreCorrectionDTO;
import com.sap.sailing.gwt.ui.shared.RemoteSailingServerReferenceDTO;
import com.sap.sailing.gwt.ui.shared.SailingServiceConstants;
import com.sap.sailing.gwt.ui.shared.ScoreCorrectionProviderDTO;
import com.sap.sailing.gwt.ui.shared.SerializationDummy;
import com.sap.sailing.gwt.ui.shared.ServerConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorResultsDTO;
import com.sap.sailing.gwt.ui.shared.SliceRacePreperationDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingArchiveConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingEventRecordDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingReplayRaceDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.UrlDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.YellowBrickConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.YellowBrickRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.CourseTemplateDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkPropertiesDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkRoleDTO;
import com.sap.sailing.gwt.ui.shared.courseCreation.MarkTemplateDTO;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.SecondsDurationImpl;
import com.sap.sse.gwt.client.replication.RemoteReplicationServiceAsync;
import com.sap.sse.pairinglist.PairingList;
import com.sap.sse.pairinglist.PairingListTemplate;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.SecuredDTO;

/**
 * The async counterpart of {@link SailingService}
 */
public interface SailingServiceAsync extends RemoteReplicationServiceAsync {

    void getRegattas(AsyncCallback<List<RegattaDTO>> callback);

    void getRegattaByName(String regattaName, AsyncCallback<RegattaDTO> asyncCallback);

    /**
     * The string returned in the callback's pair is the common event name
     * 
     * @param listHiddenRaces
     */
    void listTracTracRacesInEvent(String eventJsonURL, boolean listHiddenRaces,
            AsyncCallback<Util.Pair<String, List<TracTracRaceRecordDTO>>> callback);

    void replaySwissTimingRace(RegattaIdentifier regattaIdentifier, Iterable<SwissTimingReplayRaceDTO> replayRaces,
            boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm,
            AsyncCallback<Void> asyncCallback);

    /**
     * Passwords are erased from {@link TracTracConfigurationWithSecurityDTO#getTracTracPassword()} which will
     * always return {@code null} for the objects sent to the callback's {@link AsyncCallback#onSuccess(Object)} method.
     */
    void getPreviousTracTracConfigurations(AsyncCallback<List<TracTracConfigurationWithSecurityDTO>> callback);

    void getRawWindFixes(RegattaAndRaceIdentifier raceIdentifier, Collection<WindSource> windSources,
            AsyncCallback<WindInfoForRaceDTO> callback);

    /**
     * @param windSourceTypeNames
     *            if <code>null</code>, information from all wind sources is returned; otherwise, information only from
     *            the sources listed in this parameter by name are returned
     * @param onlyUpToNewestEvent
     *            if <code>true</code>, no wind data will be returned for time points later than
     *            {@link TrackedRace#getTimePointOfNewestEvent() trackedRace.getTimePointOfNewestEvent()}. This is
     *            helpful in case the client wants to populate a chart during live mode. If <code>false</code>, the
     *            "best effort" readings are provided for the time interval requested, no matter if based on any sensor
     *            evidence or not, regardless of {@link TrackedRace#getTimePointOfNewestEvent()
     *            trackedRace.getTimePointOfNewestEvent()}.
     */
    void getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, Date to,
            long resolutionInMilliseconds, Collection<String> windSourceTypeNames, boolean onlyUpToNewestEvent,
            AsyncCallback<WindInfoForRaceDTO> callback);

    /**
     * @param onlyUpToNewestEvent
     *            if <code>true</code>, no wind data will be returned for time points later than
     *            {@link TrackedRace#getTimePointOfNewestEvent() trackedRace.getTimePointOfNewestEvent()}. This is
     *            helpful in case the client wants to populate a chart during live mode. If <code>false</code>, the
     *            "best effort" readings are provided for the time interval requested, no matter if based on any sensor
     *            evidence or not, regardless of {@link TrackedRace#getTimePointOfNewestEvent()
     *            trackedRace.getTimePointOfNewestEvent()}.
     * @param includeCombinedWindForAllLegMiddles
     *            if <code>true</code>, the result will return non-<code>null</code> results for calls to
     *            {@link WindInfoForRaceDTO#getCombinedWindOnLegMiddle(int)}.
     */
    void getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth,
            int numberOfFixes, Collection<String> windSourceTypeNames, boolean onlyUpToNewestEvent,
            boolean includeCombinedWindForAllLegMiddles, AsyncCallback<WindInfoForRaceDTO> callback);

    void getPolarResults(RegattaAndRaceIdentifier raceIdentifier, AsyncCallback<Boolean> callback);

    void getManeuverAngle(BoatClassDTO boatClass, ManeuverType maneuverType, Speed windSpeed,
            AsyncCallback<BearingWithConfidenceDTO> callback);

    void getSimulatorResults(LegIdentifier legIdentifier, AsyncCallback<SimulatorResultsDTO> callback);

    void getRaceTimesInfo(RegattaAndRaceIdentifier raceIdentifier, AsyncCallback<RaceTimesInfoDTO> callback);

    void getRaceTimesInfoIncludingTags(RegattaAndRaceIdentifier raceIdentifier, TimePoint searchSince,
            AsyncCallback<RaceTimesInfoDTO> callback);

    void getRaceTimesInfos(Collection<RegattaAndRaceIdentifier> raceIdentifiers,
            AsyncCallback<List<RaceTimesInfoDTO>> callback);

    void getRaceTimesInfosIncludingTags(Collection<RegattaAndRaceIdentifier> raceIdentifiers,
            Map<RegattaAndRaceIdentifier, TimePoint> searchSinceMap, AsyncCallback<List<RaceTimesInfoDTO>> callback);

    void getCourseAreas(String leaderboardName, AsyncCallback<List<CourseAreaDTO>> callback);

    void getLeaderboardByName(String leaderboardName, Date date,
            Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            String previousLeaderboardId, boolean fillTotalPointsUncorrected,
            AsyncCallback<IncrementalOrFullLeaderboardDTO> callback);

    void getLeaderboardForRace(RegattaAndRaceIdentifier raceIdentifer, String leaderboardName, Date date,
            Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            String previousLeaderboardId, boolean fillTotalPointsUncorrected,
            AsyncCallback<IncrementalOrFullLeaderboardDTO> callback);

    void getLeaderboardNames(AsyncCallback<List<String>> callback);

    void getLeaderboardsWithSecurity(AsyncCallback<List<StrippedLeaderboardDTO>> callback);

    /**
     * The key set of the map returned contains all fleets of the race column identified by the combination of
     * <code>leaderboardName</code> and <code>raceColumnName</code>. If a value is <code>null</code>, there is no
     * tracked race currently linked to the fleet in the race column; otherwise, the value is the {@link RaceIdentifier}
     * of the tracked race currently connected for the fleet whose name is the key. The map returned is never
     * <code>null</code>.
     */
    void getRegattaAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(String leaderboardName, String raceColumnName,
            AsyncCallback<Map<String, RegattaAndRaceIdentifier>> callback);

    void getPreviousSwissTimingConfigurations(AsyncCallback<List<SwissTimingConfigurationWithSecurityDTO>> asyncCallback);

    void getRacesOfSwissTimingEvent(String eventJsonUrl, AsyncCallback<SwissTimingEventRecordDTO> asyncCallback);

    void getDouglasPoints(RegattaAndRaceIdentifier raceIdentifier, Map<CompetitorDTO, TimeRange> competitorTimeRanges,
            double meters, AsyncCallback<Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>>> callback);

    void getManeuvers(RegattaAndRaceIdentifier raceIdentifier, Map<CompetitorDTO, TimeRange> competitorTimeRanges,
            AsyncCallback<Map<CompetitorDTO, List<ManeuverDTO>>> callback);

    void getLeaderboardGroups(boolean withGeoLocationData, AsyncCallback<List<LeaderboardGroupDTO>> callback);

    void getLeaderboardGroupByName(String groupName, boolean withGeoLocationData,
            AsyncCallback<LeaderboardGroupDTO> callback);
    
    void getLeaderboardGroupById(UUID groupId, AsyncCallback<LeaderboardGroupDTO> callback);
    
    void getLeaderboardGroupByUuidOrName(UUID groupUuid, String groupName, AsyncCallback<LeaderboardGroupDTO> callback);

    void getCompetitorBoats(RegattaAndRaceIdentifier raceIdentifier, AsyncCallback<Map<CompetitorDTO, BoatDTO>> callback);
    
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
    void getRaceboardData(String regattaName, String raceName, String leaderboardName, String leaderboardGroupName,
            UUID leaderboardGroupId, UUID eventId, AsyncCallback<RaceboardDataDTO> callback);

    void getBoatPositions(RegattaAndRaceIdentifier raceIdentifier, Map<String, Date> fromPerCompetitorIdAsString,
            Map<String, Date> toPerCompetitorIdAsString, boolean extrapolate, DetailType detailType,
            String leaderboardName, String leaderboardGroupName, UUID leaderboardGroupId,
            AsyncCallback<CompactBoatPositionsDTO> callback);

    void getEvents(AsyncCallback<List<EventDTO>> callback);

    void resolveImageDimensions(String imageUrlAsString, AsyncCallback<Pair<Integer, Integer>> callback);

    void getScoreCorrectionProviderNames(AsyncCallback<Iterable<String>> callback);

    void getScoreCorrectionsOfProvider(String providerName, AsyncCallback<ScoreCorrectionProviderDTO> callback);

    void getScoreCorrections(String scoreCorrectionProviderName, String eventName, String boatClassName,
            Date timePointWhenResultPublished, AsyncCallback<RegattaScoreCorrectionDTO> asyncCallback);

    void getCompetitorProviderNames(AsyncCallback<Iterable<String>> callback);

    void getCompetitorProviderDTOByName(String providerName, AsyncCallback<CompetitorProviderDTO> callback);

    void getCompetitorDescriptorsAndHint(String competitorProviderName, String eventName, String regattaName, String localeForHint, AsyncCallback<Pair<List<CompetitorDescriptor>, String>> callback);

    void getWindSourcesInfo(RegattaAndRaceIdentifier raceIdentifier, AsyncCallback<WindInfoForRaceDTO> callback);

    void getServerConfiguration(AsyncCallback<ServerConfigurationDTO> callback);

    void getRemoteSailingServerReferences(AsyncCallback<List<RemoteSailingServerReferenceDTO>> callback);

    void getResultImportUrls(String resultProviderName, AsyncCallback<List<UrlDTO>> callback);

    /*
     * Validates if a {@link UrlDTO} which could contain a URL / event ID / event short-name can be used by the
     * specified {@link ResultUrlProvider}.
     * @returns {@code null} if valid, otherwise {@link String} containing error message.
     */
    void validateResultImportUrl(String resultProviderName, UrlDTO urlDTO, AsyncCallback<String> callback);

    void getUrlResultProviderNamesAndOptionalSampleURL(AsyncCallback<List<Pair<String, String>>> callback);

    void getLeaderboard(String leaderboardName, AsyncCallback<StrippedLeaderboardDTO> callback);

    void getLeaderboardWithSecurity(String leaderboardName, AsyncCallback<StrippedLeaderboardDTO> callback);

    void listSwissTiminigReplayRaces(String swissTimingUrl, AsyncCallback<List<SwissTimingReplayRaceDTO>> asyncCallback);

    /**
     * Callers will be able to obtain at most {@link SailingServiceConstants#MAX_NUMBER_OF_FIXES_TO_QUERY} fixes using
     * this method. If a finer resolution is requested based on {@code from}, {@code to}, and {@code stepSize}, the
     * resolution will automatically be lowered so that no more than
     * {@link SailingServiceConstants#MAX_NUMBER_OF_FIXES_TO_QUERY} are returned per competitor.
     */
    void getCompetitorsRaceData(RegattaAndRaceIdentifier race, List<CompetitorDTO> competitors, Date from, Date to,
            long stepSize, DetailType detailType, String leaderboarGroupName, UUID leaderboardGroupId, String leaderboardName,
            AsyncCallback<CompetitorsRaceDataDTO> callback);

    /**
     * Finds out the names of all {@link com.sap.sailing.domain.leaderboard.MetaLeaderboard}s managed by this server
     * that {@link com.sap.sailing.domain.leaderboard.MetaLeaderboard#getLeaderboards() contain} the leaderboard
     * identified by <code>leaderboardName</code>. The names of those meta-leaderboards are returned. The list returned
     * is never <code>null</code> but may be empty if no such leaderboard is found.
     */
    void getOverallLeaderboardNamesContaining(String leaderboardName, AsyncCallback<List<String>> asyncCallback);

    void getPreviousSwissTimingArchiveConfigurations(
            AsyncCallback<List<SwissTimingArchiveConfigurationWithSecurityDTO>> asyncCallback);

    /**
     * @param detailType
     *            supports {@link DetailType#REGATTA_RANK}, {@link DetailType#REGATTA_NET_POINTS_SUM} and
     *            {@link DetailType#OVERALL_RANK}.
     * 
     * @return the first triple element is the race column name; then follows the list of competitors, and finally the
     *         list of values whose indices correspond with the elements in the {@link CompetitorWithBoatDTO} list.
     */
    void getLeaderboardDataEntriesForAllRaceColumns(String leaderboardName, Date date, DetailType detailType,
            AsyncCallback<List<Util.Triple<String, List<CompetitorDTO>, List<Double>>>> callback);

    void getLeaderboardsNamesOfMetaLeaderboard(String metaLeaderboardName,
            AsyncCallback<List<Util.Pair<String, String>>> callback);

    void getRegattaStructureForEvent(UUID eventId, AsyncCallback<List<RaceGroupDTO>> asyncCallback);

    void reloadRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet,
            AsyncCallback<Void> asyncCallback);

    void getRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet,
            AsyncCallback<RaceLogDTO> callback);

    void getRegattaLog(String leaderboardName, AsyncCallback<RegattaLogDTO> callback);

    void getLeaderboardGroupNamesAndIdsAsStringsFromRemoteServer(String host, String username, String password,
            AsyncCallback<Map<String, String>> leaderboardGroups);

    void getCompetitors(boolean ignoreCompetitorsWithBoat, boolean ignoreCompetitorsWithoutBoat, AsyncCallback<Iterable<CompetitorDTO>> asyncCallback);

    void getCompetitorsOfLeaderboard(String leaderboardName, AsyncCallback<Iterable<CompetitorDTO>> asyncCallback);

    void getCompetitorsAndBoatsOfRace(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Map<? extends CompetitorDTO, BoatDTO>> asyncCallback);
    
    void getAllBoats(AsyncCallback<Iterable<BoatDTO>> asyncCallback);

    void getStandaloneBoats(AsyncCallback<Iterable<BoatDTO>> asyncCallback);

    void getBoatLinkedToCompetitorForRace(String leaderboardName, String raceColumnName, String fleetName, String competitorIdAsString, AsyncCallback<BoatDTO> asyncCallback); 
    
    void getDeviceConfigurations(AsyncCallback<List<DeviceConfigurationWithSecurityDTO>> asyncCallback);

    /**
     * Gets the race's current start time, current pass identifier and racing procedure. If no start time is set, the
     * pass identifier will still be returned, but the start time field will be <code>null</code>.
     */
    void getStartTimeAndProcedure(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Util.Triple<Date, Integer, RacingProcedureType>> callback);

    /**
     * Gets the race's current finishing and finish times as well as the current pass identifier. If no finishing or finish time is set, the
     * pass identifier will still be returned, but the finishing/finish time field will be <code>null</code>.
     */
    void getFinishingAndFinishTime(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Util.Triple<Date, Date, Integer>> asyncCallback);
    
    void getPrivateTags(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<List<TagDTO>> asyncCallback);

    void getDeserializableDeviceIdentifierTypes(AsyncCallback<List<String>> callback);

    void getGPSFixImporterTypes(AsyncCallback<Collection<String>> callback);
    
    void getSensorDataImporterTypes(AsyncCallback<Collection<String>> callback);

    void getEventsForLeaderboard(String leaderboardName, AsyncCallback<Collection<EventDTO>> callback);

    /**
     * Imports regatta structure definitions from an ISAF XRR document provided through SwissTiming's Manage2Sail.
     * The back-end will enhance the URL with the necessary API access token if none is provided in the URL.
     * 
     * @param manage2SailJsonUrl
     *            the URL pointing to a Manage2Sail JSON document that contains the link to the XRR document
     */
    void getManage2SailRegattas(String manage2SailJsonUrl, AsyncCallback<Iterable<RegattaDTO>> asyncCallback);
    
    /**
     * Returns mark passings for the competitor. Using the {@code waitForCalculations} parameter callers can control
     * whether to obtain a snapshot immediately of wait for pending updates. Waiting may be desirable, e.g., when having
     * submitted a fixed mark passing into a race log which triggers the re-calculations asynchronously.
     */
    void getCompetitorMarkPassings(RegattaAndRaceIdentifier race, CompetitorDTO competitorDTO,
            boolean waitForCalculations, AsyncCallback<Map<Integer, Date>> callback);

    void getCompetitorRaceLogMarkPassingData(String leaderboardName, String raceColumnName, String fleetName,
            CompetitorDTO competitor, AsyncCallback<Map<Integer, Date>> callback);

    void doesRegattaLogContainCompetitors(String name, AsyncCallback<Boolean> regattaLogCallBack);

    void getRaceIdentifierAndTrackedRaceSecuredDTO(String regattaLikeName, String raceColumnName, String fleetName,
            AsyncCallback<Pair<RegattaAndRaceIdentifier, SecuredDTO>> asyncCallback);

    void getTrackingTimes(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>> callback);

    void checkIfMarksAreUsedInOtherRaceLogs(String leaderboardName, String raceColumnName, String fleetName,
            Set<MarkDTO> marksToRemove, AsyncCallback<Pair<Boolean, String>> asyncCallback);

    void getCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Collection<CompetitorAndBoatDTO>> callback);

    void getCompetitorAndBoatRegistrationsInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Map<CompetitorDTO, BoatDTO>> callback);

    void getCompetitorRegistrationsForLeaderboard(String leaderboardName, AsyncCallback<Collection<CompetitorDTO>> callback);

    void getCompetitorRegistrationsInRegattaLog(String leaderboardName, AsyncCallback<Collection<CompetitorDTO>> setCompetitorsCallback);

    void getBoatRegistrationsInRegattaLog(String leaderboardName, AsyncCallback<Collection<BoatDTO>> setBoatsCallback);

    void getBoatRegistrationsForLeaderboard(String leaderboardName, AsyncCallback<Collection<BoatDTO>> callback);

    void canRemoveMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO fix, AsyncCallback<Boolean> callback);
    
    void getMarksInTrackedRace(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Iterable<MarkDTO>> callback);

    
    void getTrackingTimes(Collection<Triple<String, String, String>> leaderboardRaceColumnFleetNames,
            AsyncCallback<Map<Triple<String, String, String>, Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>>> asyncCallback);

    void serializationDummy(PersonDTO dummy, CountryCode ccDummy, PreciseCompactPosition preciseCompactPosition,
            TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, SecondsDurationImpl secondsDuration,
            KnotSpeedImpl knotSpeedImpl, KilometersPerHourSpeedImpl kmhSpeedImpl, HasPermissions hasPermissions,
            IgtimiDeviceWithSecurityDTO igtimiDeviceWithSecurityDTO, AsyncCallback<SerializationDummy> callback);

    /**
     * @param leaderboardName
     *            expected to match a regatta leaderboard with eliminations
     */
    void getEliminatedCompetitors(String leaderboardName, AsyncCallback<Collection<CompetitorDTO>> asyncCallback);

    /**
     * Used to determine for a Chart the available Detailtypes. This is for example used, to only show the RideHeight as
     * an option for charts, if it actually recorded for the race.
     */
    void determineDetailTypesForCompetitorChart(String leaderboardGroupName, UUID leaderboardGroupId, RegattaAndRaceIdentifier identifier,
            AsyncCallback<Iterable<DetailType>> callback);

    void getExpeditionDeviceConfigurations(AsyncCallback<List<ExpeditionDeviceConfiguration>> callback);

    /**
     * Calculates a {@link PairingListTemplate} based on a competitor count, flight count and group count of the
     * leaderboard. Since the competitor count must not be the competitors that are registered on the leaderboard, it
     * can vary.
     * 
     * @param leaderboardName
     *            the name of the leaderboard
     * @param competitorCount
     *            the count of competitors
     * @param flightMultiplier
     *            specifies how often the flights will be cloned
     * @param boatChangeFactor
     *            specifies the priority of well distributed assignment of competitors to boats (smallest factor) or
     *            minimization of boat changes within a {@link PairingList} (highest factor); valid factors are
     *            {@code 0..numberOfFlights}
     * @param callback
     *            returns a {@link PairingListTemplateDTO}
     */
    void calculatePairingListTemplate(final int flightCount, final int groupCount, final int competitorCount,
            final int flightMultiplier, final int boatChangeFactor, AsyncCallback<PairingListTemplateDTO> callback);

    /**
     * Creates a {@link PairingListDTO} in which the competitors will be matched to a {@link PairingList} based on the
     * information that the {@link PairingListTemplate} contains.
     * 
     * @param leaderboardName
     * @param flightMultiplier
     *            specifies how often the flights will be cloned
     * @param callback
     */
    void getPairingListFromTemplate(final String leaderboardName, final int flightMultiplier,
            final Iterable<String> selectedFlightNames, PairingListTemplateDTO templateDTO, AsyncCallback<PairingListDTO> callback);

    /**
     * Creates a {@link PairingListDTO} that is based on the competitors in the race logs of a leaderboard.
     * 
     * @param leaderboardName
     *            the name of the leaderboard
     * @param callback
     *            returns a {@link PairingListDTO}
     */
    void getPairingListFromRaceLogs(final String leaderboardName, AsyncCallback<PairingListDTO> callback);

    void getRaceDisplayNamesFromLeaderboard(final String leaderboardName, List<String> raceColumnNames, AsyncCallback<List<String>> callback);

    void getAvailableDetailTypesForLeaderboard(String leaderboardName, RegattaAndRaceIdentifier raceOrNull,
            AsyncCallback<Iterable<DetailType>> asyncCallback);

    void prepareForSlicingOfRace(RegattaAndRaceIdentifier raceIdentifier,
            AsyncCallback<SliceRacePreperationDTO> callback);
    
    /**
     * For a given WindFinder spot ID provides a {@link URL} that a web UI can use to link
     * to the WindFinder web site with the content most appropriate given the time point.
     * This could be the report page if the time is about now; the forecast page if the time point
     * is up to ten days in the future, or the statistics page if the time point is out of any
     * of the scopes above.
     */
    void getWindFinderSpot(String spotId, AsyncCallback<SpotDTO> callback);

    /**
     * @see SailingService#checkIfRaceIsTracking(RegattaAndRaceIdentifier)
     */
    void checkIfRaceIsTracking(RegattaAndRaceIdentifier raceIdentifier, AsyncCallback<Boolean> asyncCallback);

    void getMailType(AsyncCallback<MailInvitationType> callback);

    /**
     * @see SailingService#openRegattaRegistrationQrCode(String url)
     * @param url
     * @param asyncCallback
     */
    void openRegattaRegistrationQrCode(String url, AsyncCallback<String> asyncCallback);
    
    /**
     * @see SailingService#createRaceBoardLinkQrCode(String url)
     * @param url
     * @param asyncCallback
     */
    void createRaceBoardLinkQrCode(String url, AsyncCallback<String> asyncCallback);

    void getAllIgtimiDevicesWithSecurity(AsyncCallback<ArrayList<IgtimiDeviceWithSecurityDTO>> callback);

    void getAllIgtimiDataAccessWindowsWithSecurity(AsyncCallback<ArrayList<IgtimiDataAccessWindowWithSecurityDTO>> callback);

    void getIgtimiConnectionFactoryBaseUrl(AsyncCallback<Pair<String, Boolean>> callback);

    /**
     * Allows reading public Boats, or Boats that are registered in races belonging in the given regatta
     */
    void getBoat(UUID boatId, String regattaName, String regattaRegistrationLinkSecret,
            AsyncCallback<BoatDTO> asyncCallback);

    /**
     * Allows reading public Events, or Events that are related to the given regatta
     */
    void getEvent(UUID eventId, String regattaName, String regattaRegistrationLinkSecret,
            AsyncCallback<QRCodeEvent> asyncCallback);

    /**
     * Allows reading public Competitors, or Competitors that are registered in the given regatta
     */
    void getCompetitor(UUID competitorId, String leaderboardName, String regattaRegistrationLinkSecret,
            AsyncCallback<CompetitorDTO> asyncCallback);

    void getTrackedRaceIsUsingMarkPassingCalculator(RegattaAndRaceIdentifier regattaNameAndRaceName, AsyncCallback<Boolean> callback);

    void getLegGeometry(String leaderboardName, String raceColumnName, String fleetName, int[] zeroBasedLegIndices,
            ORCPerformanceCurveLegTypes[] legTypes, AsyncCallback<ORCPerformanceCurveLegImpl[]> callback);

    void getLegGeometry(RegattaAndRaceIdentifier singleSelectedRace, int[] zeroBasedLegIndices, ORCPerformanceCurveLegTypes[] legTypes,
            AsyncCallback<ORCPerformanceCurveLegImpl[]> callback);

    void getORCPerformanceCurveLegInfo(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Map<Integer, ORCPerformanceCurveLegImpl>> asyncCallback);

    void getORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier singleSelectedRace,
            AsyncCallback<Map<Integer, ORCPerformanceCurveLegImpl>> asyncCallback);

    void getBoatRegistrationsForRegatta(RegattaIdentifier regattaIdentifier, AsyncCallback<Collection<BoatDTO>> callback);
    
    void getORCCertificates(String json, AsyncCallback<Collection<ORCCertificate>> callback);

    void getORCCertificateAssignmentsByBoatIdAsString(RegattaIdentifier regattaIdentifier, AsyncCallback<Map<String, ORCCertificate>> asyncCallback);

    void getORCCertificateAssignmentsByBoatIdAsString(String leaderboardName, String raceColumnName, String fleetName, AsyncCallback<Map<String, ORCCertificate>> asyncCallback);

    /**
     * Obtains the {@link ImpliedWindSource} set in the race log identified by the triple {@code leaderboardName},
     * {@code raceColumnName}, and {@code fleetName}. Note that other than in the ORC Polar Curve ranking metric
     * no defaulting takes place here, and {@code null} is a possible result that indicates that either no race log
     * event was found that set an implied wind source, or that event explicitly set the implied wind source to {@code null}
     * (which will have the ranking metric default to {@link OwnMaxImpliedWind}, eventually).
     */
    void getImpliedWindSource(String leaderboardName, String raceColumnName, String fleetName, AsyncCallback<ImpliedWindSource> asyncCallback);

    void getSuggestedORCBoatCertificates(ArrayList<BoatDTO> boats, AsyncCallback<Map<BoatDTO, Set<ORCCertificate>>> callback);

    /**
     * Searches for ORC certificates based on various criteria. Pass {@code null} for a criterion to not restrict search
     * results based on that criterion. You can use "%" as wildcards in the {@code yachtName}, {@code sailNumber} and
     * {@code boatClassName} parameters.
     * <p>
     * 
     * @return an always valid, never {@code null} object which may be {@link Util#isEmpty() empty}.
     */
    void searchORCBoatCertificates(CountryCode country, Integer yearOfIssuance, String referenceNumber,
            String yachtName, String sailNumber, String boatClassName, AsyncCallback<Set<ORCCertificate>> callback);
    
    void getMarkTemplates(AsyncCallback<List<MarkTemplateDTO>> callback);

    void getMarkProperties(AsyncCallback<List<MarkPropertiesDTO>> asyncCallback);

    void getCourseTemplates(AsyncCallback<List<CourseTemplateDTO>> asyncCallback);

    void getMarkRoles(AsyncCallback<List<MarkRoleDTO>> callback);

    void areCompetitorRegistrationsEnabledForRace(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Boolean> callback);

    void getCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<Collection<CompetitorAndBoatDTO>> callback);

    void getEventById(UUID id, boolean withStatisticalData, AsyncCallback<EventDTO> callback);

    /**
     * Checks whether the user may cut the race identified by {@code radeIdentifier} into multiple races. For this, it
     * has to be found and it has to be a "smartphone-tracked" race with a valid start-of-tracking time. If not,
     * {@code false} will be returned. If the user it not <em>permitted</em> to slice the race, e.g., because no
     * permission has been granted to modify the leaderboard or regatta, an {@link AuthorizationException} will be
     * thrown.
     */
    void canSliceRace(RegattaAndRaceIdentifier raceIdentifier, AsyncCallback<Boolean> callback);

    void getMarksInRegattaLog(String leaderboardName, AsyncCallback<Iterable<MarkDTO>> callback);

    /**
     * Allows reading public Marks, or Marks that are registered in the given regatta
     */
    void getMark(UUID markId, String regattaName, String regattaRegistrationLinkSecret,
            AsyncCallback<MarkDTO> asyncCallback);

    void getCoursePositions(RegattaAndRaceIdentifier raceIdentifier, Date date,
            AsyncCallback<CoursePositionsDTO> asyncCallback);

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
    void getRaceMapData(RegattaAndRaceIdentifier raceIdentifier, Date date,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate, LegIdentifier simulationLegIdentifier,
            byte[] md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID,
            Date timeToGetTheEstimatedDurationFor, boolean estimatedDurationRequired, DetailType detailType,
            String leaderboardName, String leaderboardGroupName, UUID leaderboardGroupId, AsyncCallback<CompactRaceMapDataDTO> callback);

    void getRaceCourse(RegattaAndRaceIdentifier raceIdentifier, Date date, AsyncCallback<RaceCourseDTO> callback);

    void getRaceStateEntriesForRaceGroup(UUID eventId, List<UUID> visibleCourseAreas, List<String> visibleRegattas,
            boolean showOnlyCurrentlyRunningRaces, boolean showOnlyRacesOfSameDay,
            Duration clientTimeZoneOffset, AsyncCallback<List<RegattaOverviewEntryDTO>> callback);

    void getLastCourseDefinitionInRaceLog(String leaderboardName, String raceColumnName, String fleetName,
            AsyncCallback<RaceCourseDTO> callback);
    
    void getAdminConsoleChangeLogSize(AsyncCallback<Integer> callback);

    void getPreviousYellowBrickConfigurations(
            AsyncCallback<List<YellowBrickConfigurationWithSecurityDTO>> callback);

    void listYellowBrickRacesInEvent(YellowBrickConfigurationWithSecurityDTO configuration,
            AsyncCallback<Pair<String, List<YellowBrickRaceRecordDTO>>> callback);

    /**
     * Tries to find the connection between the leaderboard identified by the {@code leaderboardName} and one or more {@link Event}s and then
     * from the {@link Event}s obtains their {@link CourseArea}s.
     */
    void getCourseAreaForEventOfLeaderboard(String leaderboardName, AsyncCallback<List<CourseAreaDTO>> callback);

    void getGoogleMapsLoaderAuthenticationParams(AsyncCallback<String> callback);
}
