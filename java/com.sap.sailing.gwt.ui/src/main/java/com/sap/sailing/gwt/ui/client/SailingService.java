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

import com.google.gwt.user.client.rpc.RemoteService;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogTagEvent;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.MailInvitationType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.NotFoundException;
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
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.racelog.tracking.DoesNotHaveRegattaLogException;
import com.sap.sailing.domain.common.tracking.impl.PreciseCompactGPSFixMovingImpl.PreciseCompactPosition;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
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
import com.sap.sse.common.PairingListCreationException;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.SecondsDurationImpl;
import com.sap.sse.gwt.client.replication.RemoteReplicationService;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.SecuredDTO;

/**
 * The client side stub for the RPC service. Usually, when a <code>null</code> date is passed to the time-dependent
 * service methods, an empty (non-<code>null</code>) result is returned.
 */
public interface SailingService extends RemoteService, RemoteReplicationService {

    List<TracTracConfigurationWithSecurityDTO> getPreviousTracTracConfigurations()
            throws UnauthorizedException, Exception;

    List<RegattaDTO> getRegattas() throws UnauthorizedException;

    RegattaDTO getRegattaByName(String regattaName) throws UnauthorizedException;

    List<EventDTO> getEvents() throws Exception;

    Util.Pair<String, List<TracTracRaceRecordDTO>> listTracTracRacesInEvent(String eventJsonURL,
            boolean listHiddenRaces) throws UnauthorizedException, Exception;

    void replaySwissTimingRace(RegattaIdentifier regattaIdentifier, Iterable<SwissTimingReplayRaceDTO> replayRaces,
            boolean trackWind, boolean correctWindByDeclination, boolean useInternalMarkPassingAlgorithm)
            throws UnauthorizedException;

    WindInfoForRaceDTO getRawWindFixes(RegattaAndRaceIdentifier raceIdentifier, Collection<WindSource> windSources)
            throws UnauthorizedException;

    WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from,
            long millisecondsStepWidth, int numberOfFixes, Collection<String> windSourceTypeNames,
            boolean onlyUpToNewestEvent, boolean includeCombinedWindForAllLegMiddles)
            throws NoWindException, UnauthorizedException;

    WindInfoForRaceDTO getAveragedWindInfo(RegattaAndRaceIdentifier raceIdentifier, Date from, Date to,
            long resolutionInMilliseconds, Collection<String> windSourceTypeNames, boolean onlyUpToNewestEvent)
            throws UnauthorizedException;

    boolean getPolarResults(RegattaAndRaceIdentifier raceIdentifier) throws UnauthorizedException;

    BearingWithConfidenceDTO getManeuverAngle(BoatClassDTO boatClass, ManeuverType maneuverType, Speed windSpeed)
            throws NotEnoughDataHasBeenAddedException, UnauthorizedException;

    SimulatorResultsDTO getSimulatorResults(LegIdentifier legIdentifier) throws UnauthorizedException;

    RaceboardDataDTO getRaceboardData(String regattaName, String raceName, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, UUID eventId) throws UnauthorizedException;

    Map<CompetitorDTO, BoatDTO> getCompetitorBoats(RegattaAndRaceIdentifier raceIdentifier)
            throws UnauthorizedException;

    CompactBoatPositionsDTO getBoatPositions(RegattaAndRaceIdentifier raceIdentifier,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate, DetailType detailType, String leaderboardName, String leaderboardGroupName,
            UUID leaderboardGroupId)
                    throws NoWindException, UnauthorizedException;

    RaceTimesInfoDTO getRaceTimesInfo(RegattaAndRaceIdentifier raceIdentifier) throws UnauthorizedException;

    /**
     * Returns {@link RaceTimesInfoDTO race times info} for specified race (<code>raceIdentifier</code>) including
     * {@link RaceLogTagEvent tag events} since received timestamp (<code>searchSince</code>). Loads tags from
     * {@link ReadonlyRaceState cache} instead of scanning the whole {@link RaceLog} every request.
     */
    RaceTimesInfoDTO getRaceTimesInfoIncludingTags(RegattaAndRaceIdentifier raceIdentifier, TimePoint searchSince)
            throws UnauthorizedException;

    List<RaceTimesInfoDTO> getRaceTimesInfos(Collection<RegattaAndRaceIdentifier> raceIdentifiers)
            throws UnauthorizedException;

    /**
     * Collects besides {@link RaceTimesInfoDTO race times infos} public {@link RaceLogTagEvent tag events} from
     * {@link ReadonlyRaceState cache} and compares the <code>createdAt</code> timepoint to the received
     * <code>searchSince</code> timepoint. Returns {@link RaceTimesInfoDTO race times infos} including
     * {@link RaceLogTagEvent public tag events} since the latest client-side received tag.
     */
    List<RaceTimesInfoDTO> getRaceTimesInfosIncludingTags(Collection<RegattaAndRaceIdentifier> raceIdentifiers,
            Map<RegattaAndRaceIdentifier, TimePoint> searchSinceMap) throws UnauthorizedException;

    public List<String> getLeaderboardNames() throws UnauthorizedException;

    IncrementalOrFullLeaderboardDTO getLeaderboardByName(String leaderboardName, Date date,
            Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            String previousLeaderboardId, boolean fillTotalPointsUncorrected) throws UnauthorizedException, Exception;

    List<CourseAreaDTO> getCourseAreas(String leaderboardName);

    IncrementalOrFullLeaderboardDTO getLeaderboardForRace(RegattaAndRaceIdentifier raceIdentifer,
            String leaderboardName, Date date, Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails,
            boolean addOverallDetails, String previousLeaderboardId, boolean fillTotalPointsUncorrected)
            throws UnauthorizedException, Exception;

    List<StrippedLeaderboardDTO> getLeaderboardsWithSecurity() throws UnauthorizedException;

    Map<String, RegattaAndRaceIdentifier> getRegattaAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(
            String leaderboardName, String raceColumnName) throws UnauthorizedException;

    List<SwissTimingConfigurationWithSecurityDTO> getPreviousSwissTimingConfigurations() throws UnauthorizedException;

    SwissTimingEventRecordDTO getRacesOfSwissTimingEvent(String eventJsonURL) throws UnauthorizedException, Exception;

    Map<CompetitorDTO, List<GPSFixDTOWithSpeedWindTackAndLegType>> getDouglasPoints(
            RegattaAndRaceIdentifier raceIdentifier, Map<CompetitorDTO, TimeRange> competitorTimeRanges, double meters)
            throws NoWindException, UnauthorizedException;

    Map<CompetitorDTO, List<ManeuverDTO>> getManeuvers(RegattaAndRaceIdentifier raceIdentifier,
            Map<CompetitorDTO, TimeRange> competitorTimeRanges) throws NoWindException, UnauthorizedException;

    List<LeaderboardGroupDTO> getLeaderboardGroups(boolean withGeoLocationData) throws UnauthorizedException;

    LeaderboardGroupDTO getLeaderboardGroupByName(String groupName, boolean withGeoLocationData)
            throws UnauthorizedException;
    
    LeaderboardGroupDTO getLeaderboardGroupById(UUID groupId) throws UnauthorizedException;
    
    LeaderboardGroupDTO getLeaderboardGroupByUuidOrName(final UUID groupUuid, final String groupName);

    CompetitorsRaceDataDTO getCompetitorsRaceData(RegattaAndRaceIdentifier race, List<CompetitorDTO> competitors,
            Date from, Date to, long stepSizeInMs, DetailType detailType, String leaderboardGroupName,
            UUID leaderboardGroupId, String leaderboardName) throws NoWindException, UnauthorizedException, NotFoundException;

    Pair<Integer, Integer> resolveImageDimensions(String imageUrlAsString) throws UnauthorizedException, Exception;

    Iterable<String> getScoreCorrectionProviderNames() throws UnauthorizedException;

    ScoreCorrectionProviderDTO getScoreCorrectionsOfProvider(String providerName)
            throws UnauthorizedException, Exception;

    RegattaScoreCorrectionDTO getScoreCorrections(String scoreCorrectionProviderName, String eventName,
            String boatClassName, Date timePointWhenResultPublished) throws UnauthorizedException, Exception;

    Iterable<String> getCompetitorProviderNames() throws UnauthorizedException;

    CompetitorProviderDTO getCompetitorProviderDTOByName(String providerName) throws UnauthorizedException, Exception;

    Pair<List<CompetitorDescriptor>, String> getCompetitorDescriptorsAndHint(String competitorProviderName, String eventName,
            String regattaName, String localeForHint) throws UnauthorizedException, Exception;

    WindInfoForRaceDTO getWindSourcesInfo(RegattaAndRaceIdentifier raceIdentifier) throws UnauthorizedException;

    ServerConfigurationDTO getServerConfiguration() throws UnauthorizedException;

    List<RemoteSailingServerReferenceDTO> getRemoteSailingServerReferences() throws UnauthorizedException;

    List<UrlDTO> getResultImportUrls(String resultProviderName) throws UnauthorizedException;

    String validateResultImportUrl(String resultProviderName, UrlDTO urlDTO);

    List<Pair<String, String>> getUrlResultProviderNamesAndOptionalSampleURL() throws UnauthorizedException;

    StrippedLeaderboardDTO getLeaderboard(String leaderboardName) throws UnauthorizedException;

    StrippedLeaderboardDTO getLeaderboardWithSecurity(String leaderboardName) throws UnauthorizedException;

    List<SwissTimingReplayRaceDTO> listSwissTiminigReplayRaces(String swissTimingUrl) throws UnauthorizedException;

    List<Triple<String, List<CompetitorDTO>, List<Double>>> getLeaderboardDataEntriesForAllRaceColumns(
            String leaderboardName, Date date, DetailType detailType) throws UnauthorizedException, Exception;

    List<String> getOverallLeaderboardNamesContaining(String leaderboardName) throws UnauthorizedException;

    List<SwissTimingArchiveConfigurationWithSecurityDTO> getPreviousSwissTimingArchiveConfigurations() throws UnauthorizedException;

    List<Util.Pair<String, String>> getLeaderboardsNamesOfMetaLeaderboard(String metaLeaderboardName)
            throws UnauthorizedException;

    /** for backward compatibility with the regatta overview */
    List<RaceGroupDTO> getRegattaStructureForEvent(UUID eventId) throws UnauthorizedException;

    void reloadRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet)
            throws UnauthorizedException, NotFoundException;

    RaceLogDTO getRaceLog(String leaderboardName, RaceColumnDTO raceColumnDTO, FleetDTO fleet)
            throws UnauthorizedException;

    RegattaLogDTO getRegattaLog(String leaderboardName) throws UnauthorizedException, DoesNotHaveRegattaLogException;

    Map<String, String> getLeaderboardGroupNamesAndIdsAsStringsFromRemoteServer(String url, String username, String password)
            throws UnauthorizedException;

    Iterable<CompetitorDTO> getCompetitors(boolean filterCompetitorsWithBoat, boolean filterCompetitorsWithoutBoat)
            throws UnauthorizedException;

    Iterable<CompetitorDTO> getCompetitorsOfLeaderboard(String leaderboardName) throws UnauthorizedException;

    Map<? extends CompetitorDTO, BoatDTO> getCompetitorsAndBoatsOfRace(String leaderboardName, String raceColumnName,
            String fleetName) throws UnauthorizedException, NotFoundException;

    Iterable<BoatDTO> getAllBoats() throws UnauthorizedException;

    Iterable<BoatDTO> getStandaloneBoats() throws UnauthorizedException;

    BoatDTO getBoatLinkedToCompetitorForRace(String leaderboardName, String raceColumnName, String fleetName,
            String competitorIdAsString) throws UnauthorizedException, NotFoundException;

    List<DeviceConfigurationWithSecurityDTO> getDeviceConfigurations() throws UnauthorizedException;

    Util.Triple<Date, Integer, RacingProcedureType> getStartTimeAndProcedure(String leaderboardName,
            String raceColumnName, String fleetName) throws UnauthorizedException, NotFoundException;

    Util.Triple<Date, Date, Integer> getFinishingAndFinishTime(String leaderboardName, String raceColumnName,
            String fleetName) throws UnauthorizedException, NotFoundException;

    /**
     * Returns all private {@link TagDTO tags} of specified race and current user.
     * 
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @return list of private {@link TagDTO tags}
     */
    List<TagDTO> getPrivateTags(String leaderboardName, String raceColumnName, String fleetName)
            throws UnauthorizedException;

    List<String> getDeserializableDeviceIdentifierTypes() throws UnauthorizedException;

    Collection<String> getGPSFixImporterTypes() throws UnauthorizedException;

    Collection<String> getSensorDataImporterTypes() throws UnauthorizedException;

    /**
     * @see SailingServiceAsync#getCompetitorMarkPassings(RegattaAndRaceIdentifier, CompetitorWithBoatDTO, boolean,
     *      com.google.gwt.user.client.rpc.AsyncCallback)
     */
    Map<Integer, Date> getCompetitorMarkPassings(RegattaAndRaceIdentifier race, CompetitorDTO competitorDTO,
            boolean waitForCalculations) throws UnauthorizedException;

    /**
     * Obtains fixed mark passings and mark passing suppressions from the race log identified by
     * <code>leaderboardName</code>, <code>raceColumnDTO</code> and <code>fleet</code>. The result contains pairs of
     * zero-based waypoint numbers and times where <code>null</code> represents a suppressed mark passing and a valid
     * {@link Date} objects represents a fixed mark passing.
     * 
     * @throws NotFoundException
     */
    Map<Integer, Date> getCompetitorRaceLogMarkPassingData(String leaderboardName, String raceColumnName,
            String fleetName, CompetitorDTO competitor) throws UnauthorizedException, NotFoundException;

    /**
     * A leaderboard may be situated under multiple events (connected via a leaderboardgroup). This method traverses all
     * events and leaderboardgroup to build the collection of events this leaderboard is coupled to.
     */
    Collection<EventDTO> getEventsForLeaderboard(String leaderboardName) throws UnauthorizedException;

    /**
     * Imports regatta structure definitions from an ISAF XRR document
     * 
     * @param manage2SailJsonUrl
     *            the URL pointing to a Manage2Sail JSON document that contains the link to the XRR document
     */
    Iterable<RegattaDTO> getManage2SailRegattas(String manage2SailJsonUrl) throws Exception;

    boolean doesRegattaLogContainCompetitors(String name)
            throws UnauthorizedException, DoesNotHaveRegattaLogException, NotFoundException;

    Pair<RegattaAndRaceIdentifier, SecuredDTO> getRaceIdentifierAndTrackedRaceSecuredDTO(String regattaLikeName,
            String raceColumnName, String fleetName);

    Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> getTrackingTimes(String leaderboardName,
            String raceColumnName, String fleetName) throws UnauthorizedException, NotFoundException;

    /**
     * Get the competitors registered for a certain race. This automatically checks, whether competitors are registered
     * in the raceLog (in case of e.g. splitFleets) or in the RegattaLog (default)
     * 
     * @throws NotFoundException
     */
    Collection<CompetitorAndBoatDTO> getCompetitorRegistrationsForRace(String leaderboardName, String raceColumnName,
            String fleetName) throws UnauthorizedException, DoesNotHaveRegattaLogException, NotFoundException;

    Collection<CompetitorDTO> getCompetitorRegistrationsInRegattaLog(String leaderboardName)
            throws UnauthorizedException, DoesNotHaveRegattaLogException, NotFoundException;

    Collection<BoatDTO> getBoatRegistrationsInRegattaLog(String leaderboardName)
            throws UnauthorizedException, DoesNotHaveRegattaLogException, NotFoundException;

    Collection<BoatDTO> getBoatRegistrationsForLeaderboard(String leaderboardName)
            throws UnauthorizedException, NotFoundException;

    Boolean areCompetitorRegistrationsEnabledForRace(String leaderboardName, String raceColumnName, String fleetName)
            throws UnauthorizedException, NotFoundException;

    Pair<Boolean, String> checkIfMarksAreUsedInOtherRaceLogs(String leaderboardName, String raceColumnName,
            String fleetName, Set<MarkDTO> marksToRemove) throws UnauthorizedException, NotFoundException;

    Collection<CompetitorAndBoatDTO> getCompetitorRegistrationsInRaceLog(String leaderboardName, String raceColumnName,
            String fleetName) throws UnauthorizedException, NotFoundException;

    Map<CompetitorDTO, BoatDTO> getCompetitorAndBoatRegistrationsInRaceLog(String leaderboardName,
            String raceColumnName, String fleetName) throws UnauthorizedException, NotFoundException;

    Collection<CompetitorDTO> getCompetitorRegistrationsForLeaderboard(String leaderboardName)
            throws UnauthorizedException, NotFoundException;

    Iterable<MarkDTO> getMarksInTrackedRace(String leaderboardName, String raceColumnName, String fleetName)
            throws UnauthorizedException;

    /**
     * The service may decide whether a mark fix can be removed. This is generally possible if there is a mark device
     * mapping that can be manipulated in such a way that the {@code fix} will no longer be mapped.
     */
    boolean canRemoveMarkFix(String leaderboardName, String raceColumnName, String fleetName, String markIdAsString,
            GPSFixDTO fix) throws UnauthorizedException;

    Map<Triple<String, String, String>, Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>> getTrackingTimes(
            Collection<Triple<String, String, String>> raceColumnsAndFleets) throws UnauthorizedException;

    SerializationDummy serializationDummy(PersonDTO dummy, CountryCode ccDummy,
            PreciseCompactPosition preciseCompactPosition, TypeRelativeObjectIdentifier typeRelativeObjectIdentifier,
            SecondsDurationImpl secondsDuration, KnotSpeedImpl knotSpeedImpl, KilometersPerHourSpeedImpl kmhSpeedImpl,
            HasPermissions hasPermissions, IgtimiDeviceWithSecurityDTO igtimiDeviceWithSecurityDTO) throws UnauthorizedException;

    Collection<CompetitorDTO> getEliminatedCompetitors(String leaderboardName) throws UnauthorizedException;

    /**
     * Used to determine for a Chart the available Detailtypes. This is for example used to only show the RideHeight as
     * an option for charts if it actually recorded for the race.
     */
    Iterable<DetailType> determineDetailTypesForCompetitorChart(String leaderboardGroupName, UUID leaderboardGroupId,
            RegattaAndRaceIdentifier identifier) throws UnauthorizedException;

    List<ExpeditionDeviceConfiguration> getExpeditionDeviceConfigurations() throws UnauthorizedException;

    /**
     * @throws NotFoundException
     *             is thrown if the leaderboard is not found by name
     */
    PairingListTemplateDTO calculatePairingListTemplate(final int flightCount, final int groupCount,
            final int competitorCount, final int flightMultiplier, final int tolerance)
            throws UnauthorizedException, NotFoundException, IllegalArgumentException;

    PairingListDTO getPairingListFromTemplate(String leaderboardName, int flightMultiplier,
            Iterable<String> selectedFlightNames, PairingListTemplateDTO templateDTO)
            throws UnauthorizedException, NotFoundException, PairingListCreationException;

    PairingListDTO getPairingListFromRaceLogs(String leaderboardName) throws UnauthorizedException, NotFoundException;

    List<String> getRaceDisplayNamesFromLeaderboard(String leaderboardName, List<String> raceColumnNames)
            throws UnauthorizedException, NotFoundException;

    Iterable<DetailType> getAvailableDetailTypesForLeaderboard(String leaderboardName,
            RegattaAndRaceIdentifier raceOrNull) throws UnauthorizedException;

    SpotDTO getWindFinderSpot(String spotId) throws UnauthorizedException, Exception;

    /**
     * Returns specific data needed for the slicing UI.
     */
    SliceRacePreperationDTO prepareForSlicingOfRace(RegattaAndRaceIdentifier raceIdentifier)
            throws UnauthorizedException;

    /**
     * Checks if the given race is currently in state tracking or loading.
     */
    Boolean checkIfRaceIsTracking(RegattaAndRaceIdentifier race) throws UnauthorizedException;



    MailInvitationType getMailType() throws UnauthorizedException;

    /**
     * Generates a base64-encoded qrcode for the branch.io url used to allow registrations for open regattas.
     * 
     * @param url
     *            complete deeplink url for registration on open regattas
     * @return base64 encoded string containg a png-image of the genrated qrcode
     */
    String openRegattaRegistrationQrCode(String url) throws UnauthorizedException;
    
    /**
     * Generates a base64-encoded qrcode.
     * 
     * @param url
     *            complete url for a race with settings on the RaceBoard
     * @return base64 encoded string containg a png-image of the genrated qrcode
     */
    String createRaceBoardLinkQrCode(String url);

    ArrayList<IgtimiDeviceWithSecurityDTO> getAllIgtimiDevicesWithSecurity() throws Exception;
    
    ArrayList<IgtimiDataAccessWindowWithSecurityDTO> getAllIgtimiDataAccessWindowsWithSecurity() throws Exception;

    Pair<String, Boolean> getIgtimiConnectionFactoryBaseUrl();

    /**
     * Allows reading public Boats, or Boats that are registered in races belonging in the given regatta
     */
    BoatDTO getBoat(UUID boatId, String regattaName, String regattaRegistrationLinkSecret);

    /**
     * Allows reading public Events, or Events that are related to the given regatta
     */
    QRCodeEvent getEvent(UUID eventId, String regattaName, String regattaRegistrationLinkSecret);
    
    EventDTO getEventById(UUID id, boolean withStatisticalData) throws IOException, UnauthorizedException;

    /**
     * Allows reading public Competitors, or Competitors that are registered in the given regatta
     */
    CompetitorDTO getCompetitor(UUID competitorId, String leaderboardName,
            String regattaRegistrationLinkSecret);

    boolean getTrackedRaceIsUsingMarkPassingCalculator(RegattaAndRaceIdentifier regattaNameAndRaceName);

    ORCPerformanceCurveLegImpl[] getLegGeometry(String leaderboardName, String raceColumnName, String fleetName,
            int[] zeroBasedLegIndices, ORCPerformanceCurveLegTypes[] legTypes);

    ORCPerformanceCurveLegImpl[] getLegGeometry(RegattaAndRaceIdentifier singleSelectedRace, int[] zeroBasedLegIndices,
            ORCPerformanceCurveLegTypes[] legTypes);

    /**
     * @throws NotFoundException
     *             in case the race log cannot be found by the leaderboard, race column and fleet names provided
     */
    Map<Integer, ORCPerformanceCurveLegImpl> getORCPerformanceCurveLegInfo(String leaderboardName,
            String raceColumnName, String fleetName) throws NotFoundException;

    Map<Integer, ORCPerformanceCurveLegImpl> getORCPerformanceCurveLegInfo(RegattaAndRaceIdentifier singleSelectedRace);
    
    Collection<BoatDTO> getBoatRegistrationsForRegatta(RegattaIdentifier regattaIdentifier) throws NotFoundException;

    Collection<ORCCertificate> getORCCertificates(String json) throws Exception;

    Map<String, ORCCertificate> getORCCertificateAssignmentsByBoatIdAsString(RegattaIdentifier regattaIdentifier) throws NotFoundException;
    
    Map<String, ORCCertificate> getORCCertificateAssignmentsByBoatIdAsString(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException;

    ImpliedWindSource getImpliedWindSource(String leaderboardName, String raceColumnName, String fleetName) throws NotFoundException;

    Map<BoatDTO, Set<ORCCertificate>> getSuggestedORCBoatCertificates(ArrayList<BoatDTO> boats) throws Exception;

    Set<ORCCertificate> searchORCBoatCertificates(CountryCode country, Integer yearOfIssuance, String referenceNumber,
            String yachtName, String sailNumber, String boatClassName) throws Exception;

    List<MarkTemplateDTO> getMarkTemplates();

    List<MarkPropertiesDTO> getMarkProperties();

    List<CourseTemplateDTO> getCourseTemplates();

    List<MarkRoleDTO> getMarkRoles();

    /**
     * Returns {@code true} if the given race can be sliced. Only Smarthphone tracked races can be sliced. In addition
     * the race must be part of a {@link RegattaLeaderboard}.
     */
    boolean canSliceRace(RegattaAndRaceIdentifier raceIdentifier) throws UnauthorizedException;

    Iterable<MarkDTO> getMarksInRegattaLog(String leaderboardName)
            throws UnauthorizedException, DoesNotHaveRegattaLogException;

    /**
     * Allows reading public Marks, or Marks that are registered in the given regatta
     */
    MarkDTO getMark(UUID markId, String regattaName, String regattaRegistrationLinkSecret);

    CoursePositionsDTO getCoursePositions(RegattaAndRaceIdentifier raceIdentifier, Date date)
            throws UnauthorizedException;

    CompactRaceMapDataDTO getRaceMapData(RegattaAndRaceIdentifier raceIdentifier, Date date,
            Map<String, Date> fromPerCompetitorIdAsString, Map<String, Date> toPerCompetitorIdAsString,
            boolean extrapolate, LegIdentifier simulationLegIdentifier,
            byte[] md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID,
            Date timeToGetTheEstimatedDurationFor, boolean estimatedDurationRequired, DetailType detailType,
            String leaderboardName, String leaderboardGroupName, UUID leaderboardGroupId)
            throws NoWindException, UnauthorizedException;

    RaceCourseDTO getRaceCourse(RegattaAndRaceIdentifier raceIdentifier, Date date) throws UnauthorizedException;

    List<RegattaOverviewEntryDTO> getRaceStateEntriesForRaceGroup(UUID eventId, List<UUID> visibleCourseAreas,
            List<String> visibleRegattas, boolean showOnlyCurrentlyRunningRaces, boolean showOnlyRacesOfSameDay,
            Duration clientTimeZoneOffset) throws UnauthorizedException, Exception;

    RaceCourseDTO getLastCourseDefinitionInRaceLog(String leaderboardName, String raceColumnName, String fleetName)
            throws UnauthorizedException, NotFoundException;
    
    Integer getAdminConsoleChangeLogSize();

    List<YellowBrickConfigurationWithSecurityDTO> getPreviousYellowBrickConfigurations();

    Pair<String, List<YellowBrickRaceRecordDTO>> listYellowBrickRacesInEvent(
            YellowBrickConfigurationWithSecurityDTO configuration) throws Exception;

    List<CourseAreaDTO> getCourseAreaForEventOfLeaderboard(String leaderboardName);

    String getGoogleMapsLoaderAuthenticationParams();
}
