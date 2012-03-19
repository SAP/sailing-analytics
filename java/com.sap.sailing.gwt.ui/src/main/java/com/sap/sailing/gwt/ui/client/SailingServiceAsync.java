package com.sap.sailing.gwt.ui.client;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.EventAndRaceIdentifier;
import com.sap.sailing.domain.common.EventIdentifier;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.gwt.ui.shared.CompetitorDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardEntryDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.MultiCompetitorRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.QuickRankDTO;
import com.sap.sailing.gwt.ui.shared.RaceDTO;
import com.sap.sailing.gwt.ui.shared.RaceInLeaderboardDTO;
import com.sap.sailing.gwt.ui.shared.RaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.SwissTimingRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationDTO;
import com.sap.sailing.gwt.ui.shared.TracTracRaceRecordDTO;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;

/**
 * The async counterpart of {@link SailingService}
 */
public interface SailingServiceAsync {
    
    void listEvents(AsyncCallback<List<EventDTO>> callback);
    
    /**
     * The string returned in the callback's pair is the common event name
     */
    void listTracTracRacesInEvent(String eventJsonURL, AsyncCallback<Pair<String, List<TracTracRaceRecordDTO>>> callback);

    /**
     * @param liveURI may be <code>null</code> or the empty string in which case the server will
     * use the {@link TracTracRaceRecordDTO#liveURI} from the <code>rr</code> race record.
     * @param storedURImay be <code>null</code> or the empty string in which case the server will
     * use the {@link TracTracRaceRecordDTO#storedURI} from the <code>rr</code> race record.
     */
    void track(TracTracRaceRecordDTO rr, String liveURI, String storedURI, boolean trackWind, boolean correctWindByDeclination,
            AsyncCallback<Void> callback);

    void getPreviousTracTracConfigurations(AsyncCallback<List<TracTracConfigurationDTO>> callback);

    void storeTracTracConfiguration(String name, String jsonURL, String liveDataURI, String storedDataURI,
            AsyncCallback<Void> callback);

    void stopTrackingEvent(EventIdentifier eventIdentifier, AsyncCallback<Void> callback);

    void stopTrackingRace(EventAndRaceIdentifier raceIdentifier, AsyncCallback<Void> asyncCallback);
    
    /**
     * Untracks the race and removes it from the event. It will also be removed in all leaderboards
     * @param eventAndRaceidentifier The identifier for the event name, and the race name to remove
     * @throws Exception
     */
    void removeAndUntrackRace(EventAndRaceIdentifier eventAndRaceidentifier, AsyncCallback<Void> callback);

    void getWindInfo(RaceIdentifier raceIdentifier, Date from, Date to, WindSource[] windSources,
            AsyncCallback<WindInfoForRaceDTO> callback);

    /**
     * @param from if <code>null</code>, the tracked race's start of tracking is used
     * @param to if <code>null</code>, the tracked race's time point of newest event is used
     */
    void getWindInfo(RaceIdentifier raceIdentifier, Date from, Date to, long resolutionInMilliseconds,
            Collection<String> windSourceTypeNames, AsyncCallback<WindInfoForRaceDTO> callback);

    /**
     * @param windSourceTypeNames
     *            if <code>null</code>, data from all available wind sources will be returned, otherwise only from those
     *            whose {@link WindSource} name is contained in the <code>windSources</code> collection.
     */
    void getWindInfo(RaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth, int numberOfFixes,
            double latDeg, double lngDeg, Collection<String> windSourceTypeNames,
            AsyncCallback<WindInfoForRaceDTO> callback);

    /**
     * Same as {@link #getWindInfo(RaceIdentifier, Date, long, int, double, double, Collection, AsyncCallback)}, only
     * that the wind is not requested for a specific position, but instead the wind sources associated with the tracked
     * race identified by <code>raceIdentifier</code> are requested to deliver their original position. This will in
     * particular preserve the positions of actual measurements and will deliver the averaged positions for averaged /
     * combined wind read-outs.
     * 
     * @param from
     *            must not be <code>null</code>
     * @param numberOfFixes
     *            no matter how great this value is chosen, never returns data beyond the newest event recorded in the
     *            race
     */
    void getWindInfo(RaceIdentifier raceIdentifier, Date from, long millisecondsStepWidth, int numberOfFixes,
            Collection<String> windSourceTypeNames, AsyncCallback<WindInfoForRaceDTO> callback);

    void setWind(RaceIdentifier raceIdentifier, WindDTO wind, AsyncCallback<Void> callback);
    
    void removeWind(RaceIdentifier raceIdentifier, WindDTO windDTO, AsyncCallback<Void> callback);

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
    void getBoatPositions(RaceIdentifier raceIdentifier,
            Map<CompetitorDTO, Date> from, Map<CompetitorDTO, Date> to,
            boolean extrapolate, AsyncCallback<Map<CompetitorDTO, List<GPSFixDTO>>> callback);

    void getRaceTimesInfo(RaceIdentifier raceIdentifier, AsyncCallback<RaceTimesInfoDTO> callback);
    
    void getRaceTimesInfos(Collection<RaceIdentifier> raceIdentifiers, AsyncCallback<List<RaceTimesInfoDTO>> callback);

    void getMarkPositions(RaceIdentifier raceIdentifier, Date date, AsyncCallback<List<MarkDTO>> asyncCallback);

    void getQuickRanks(RaceIdentifier raceIdentifier, Date date, AsyncCallback<List<QuickRankDTO>> callback);

    /**
     * Returns a {@link LeaderboardDTO} will information about all races, their points and competitor display names
     * filled in. The column details are filled for the races whose named are provided in
     * <code>namesOfRacesForWhichToLoadLegDetails</code>.
     * 
     * @param namesOfRacesForWhichToLoadLegDetails
     *            if <code>null</code>, no {@link LeaderboardEntryDTO#legDetails leg details} will be present in the
     *            result ({@link LeaderboardEntryDTO#legDetails} will be <code>null</code> for all
     *            {@link LeaderboardEntryDTO} objects contained). Otherwise, the {@link LeaderboardEntryDTO#legDetails}
     *            list will contain one entry per leg of the race {@link Course} for those race columns whose
     *            {@link RaceInLeaderboard#getType() name} is contained in
     *            <code>namesOfRacesForWhichToLoadLegDetails</code>. For all other columns,
     *            {@link LeaderboardEntryDTO#legDetails} is <code>null</code>.
     */
    void getLeaderboardByName(String leaderboardName, Date date,
            Collection<String> namesOfRacesForWhichToLoadLegDetails, AsyncCallback<LeaderboardDTO> callback);

    void getLeaderboardNames(AsyncCallback<List<String>> callback);

    /**
     * Creates a {@link LeaderboardDTO} for each leaderboard known by the server and fills in the name, race master data
     * in the form of {@link RaceInLeaderboardDTO}s, whether or not there are {@link LeaderboardDTO#hasCarriedPoints
     * carried points} and the {@link LeaderboardDTO#discardThresholds discarding thresholds} for the leaderboard. No
     * data about the points is filled into the result object. No data about the competitor display names is filled in;
     * instead, an empty map is used for {@link LeaderboardDTO#competitorDisplayNames}.
     */
    void getLeaderboards(AsyncCallback<List<LeaderboardDTO>> callback);
    
    /**
     * Does the same as {@link SailingServiceAsync#getLeaderboards(AsyncCallback) getLeaderboards} but returns only
     * leaderboards which have the given event as race
     */
    void getLeaderboardsByEvent(EventDTO event, AsyncCallback<List<LeaderboardDTO>> callback);
    
    void getLeaderboardsByRace(RaceDTO race, AsyncCallback<List<LeaderboardDTO>> callback);
    
    void updateLeaderboard(String leaderboardName, String newLeaderboardName, int[] newDiscardingThreasholds,
            AsyncCallback<Void> callback);

    /**
     * Creates a leaderboard with the name specified by <code>leaderboardName</code> and the initial discarding thesholds
     * as specified by <code>discardThresholds</code>. The leaderboard returned has the leaderboard name and the master
     * data about the race columns filled in, but no details about the race points. As such, the result structure
     * equals that of the result of {@link #getLeaderboards(AsyncCallback)}.
     */
    void createLeaderboard(String leaderboardName, int[] discardThresholds, AsyncCallback<LeaderboardDTO> asyncCallback);

    void removeLeaderboard(String leaderboardName, AsyncCallback<Void> asyncCallback);
    
    void renameLeaderboard(String leaderboardName, String newLeaderboardName, AsyncCallback<Void> asyncCallback);

    void addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace,
            AsyncCallback<Void> callback);

    void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName, AsyncCallback<Void> callback);

    void removeLeaderboardColumn(String leaderboardName, String columnName, AsyncCallback<Void> callback);

    /**
     * @param asyncCallback receives <code>true</code> if connecting was successful
     */
    void connectTrackedRaceToLeaderboardColumn(String leaderboardName, String raceColumnName,
            RaceIdentifier raceIdentifier, AsyncCallback<Boolean> asyncCallback);

    void getEventAndRaceNameOfTrackedRaceConnectedToLeaderboardColumn(String leaderboardName, String raceColumnName,
            AsyncCallback<Pair<String, String>> callback);

    void disconnectLeaderboardColumnFromTrackedRace(String leaderboardName, String raceColumnName,
            AsyncCallback<Void> callback);

    void updateLeaderboardCarryValue(String leaderboardName, String competitorName, Integer carriedPoints, AsyncCallback<Void> callback);

    void updateLeaderboardMaxPointsReason(String leaderboardName, String competitorName, String raceColumnName,
            String maxPointsReasonAsString, Date date, AsyncCallback<Pair<Integer, Integer>> asyncCallback);

    void updateLeaderboardScoreCorrection(String leaderboardName, String competitorName, String raceName,
            Integer correctedScore, Date date, AsyncCallback<Pair<Integer, Integer>> asyncCallback);

    void updateCompetitorDisplayNameInLeaderboard(String leaderboardName, String competitorName, String displayName,
            AsyncCallback<Void> callback);

	void moveLeaderboardColumnUp(String leaderboardName, String columnName,
			AsyncCallback<Void> callback);

	void moveLeaderboardColumnDown(String leaderboardName, String columnName,
			AsyncCallback<Void> callback);

	void updateIsMedalRace(String leaderboardName, String columnName, boolean isMedalRace,
			AsyncCallback<Void> callback);

    void getPreviousSwissTimingConfigurations(AsyncCallback<List<SwissTimingConfigurationDTO>> asyncCallback);

    void listSwissTimingRaces(String hostname, int port, boolean canSendRequests,
            AsyncCallback<List<SwissTimingRaceRecordDTO>> asyncCallback);

    void storeSwissTimingConfiguration(String configName, String hostname, int port, boolean canSendRequests, AsyncCallback<Void> asyncCallback);

    void trackWithSwissTiming(SwissTimingRaceRecordDTO rr, String hostname, int port, boolean canSendRequests,
            boolean trackWind, boolean correctWindByDeclination, AsyncCallback<Void> asyncCallback);

    void sendSwissTimingDummyRace(String racMessage, String stlMesssage, String ccgMessage, AsyncCallback<Void> callback);
    
    /**
     * Requests the computation of the {@link LeaderboardDTO} for <code>leaderboardName</code> <code>times</code> times.
     * The date used for the {@link #getLeaderboardByName(String, Date, Collection, AsyncCallback)} call is iterated
     * in 10ms time steps, going backwards from "now." For all races, all details are requested.
     */
    void stressTestLeaderboardByName(String leaderboardName, int times, AsyncCallback<Void> callback);

    void getCountryCodes(AsyncCallback<String[]> callback);
    
    void getDouglasPoints(RaceIdentifier raceIdentifier, Map<CompetitorDTO, Date> from, Map<CompetitorDTO, Date> to,
            double meters, AsyncCallback<Map<CompetitorDTO, List<GPSFixDTO>>> callback);

    void getManeuvers(RaceIdentifier raceIdentifier, Map<CompetitorDTO, Date> from, Map<CompetitorDTO, Date> to,
            AsyncCallback<Map<CompetitorDTO, List<ManeuverDTO>>> callback);

    /**
     * Creates a {@link LeaderboardGroupDTO} for each {@link LeaderboardGroup} known by the server, which contains the
     * name, the description and a list with {@link LeaderboardDTO LeaderboardDTOs} contained by the group.
     */
    void getLeaderboardGroups(AsyncCallback<List<LeaderboardGroupDTO>> callback);

    /**
     * Creates a {@link LeaderboardGroupDTO} for the {@link LeaderboardGroup} with the name <code>groupName</code>, which contains the
     * name, the description and a list with {@link LeaderboardDTO LeaderboardDTOs} contained by the group.<br />
     * If no group with the name <code>groupName</code> is known, an {@link IllegalArgumentException} is thrown.
     */
    void getLeaderboardGroupByName(String groupName, AsyncCallback<LeaderboardGroupDTO> callback);
    
    /**
    * Renames the group with the name <code>oldName</code> to the <code>newName</code>.<br />
    * If there's no group with the name <code>oldName</code> or there's already a group with the name
    * <code>newName</code> a {@link IllegalArgumentException} is thrown.
    */
    void renameLeaderboardGroup(String oldName, String newName, AsyncCallback<Void> callback);
    
    /**
     * Removes the leaderboard group with the name <code>groupName</code> from the service and the persistant store.
     */
    void removeLeaderboardGroup(String groupName, AsyncCallback<Void> callback);
    
    /**
     * Creates a new group with the name <code>groupname</code>, the description <code>description</code> and an empty list of leaderboards.<br/>
     */
    void createLeaderboardGroup(String groupName, String description, AsyncCallback<LeaderboardGroupDTO> callback);
    
    /**
     * Updates the data of the group with the name <code>oldName</code>.
     * 
     * @param oldName The old name of the group
     * @param newName The new name of the group
     * @param description The new description of the group
     * @param leaderboards The new leaderboards of the group
     */
    void updateLeaderboardGroup(String oldName, String newName, String description, List<LeaderboardDTO> leaderboards, AsyncCallback<Void> callback);

    /**
     * Returns the mark passings and the data for the given {@link DetailType} of all competitors in
     * <code>competitorsQuery</code> in the <code>race</code>, including the first dates in the query.<br />
     * The Long part in the <code>competitorsQuery</code> defines the time point, from which on the data should be
     * returned. If this time point is lesser than the start of race, all available data for this competitor is
     * returned.<br />
     * Returns <code>null</code>, if <code>race</code> isn't tracked. 
     */
    void getCompetitorsRaceData(RaceIdentifier race, List<Pair<Date,CompetitorDTO>> competitorsToLoad, Date toDate, long stepSize,
            DetailType detailType, AsyncCallback<MultiCompetitorRaceDataDTO> callback);

    void setRaceIsKnownToStartUpwind(RaceIdentifier raceIdentifier, boolean raceIsKnownToStartUpwind,
            AsyncCallback<Void> callback);

    void setWindSourcesToExclude(RaceIdentifier raceIdentifier, Iterable<WindSource> windSourcesToExclude,
            AsyncCallback<Void> callback);

    void getRaceMapData(RaceIdentifier raceIdentifier, Date date, Map<CompetitorDTO, Date> from,
            Map<CompetitorDTO, Date> to, boolean extrapolate, AsyncCallback<RaceMapDataDTO> callback);

}
