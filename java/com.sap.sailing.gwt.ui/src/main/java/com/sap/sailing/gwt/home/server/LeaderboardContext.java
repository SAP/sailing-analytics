package com.sap.sailing.gwt.home.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.home.communication.event.EventSeriesReferenceDTO;
import com.sap.sailing.gwt.home.communication.event.EventState;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.GetMiniLeaderboardDTO;
import com.sap.sailing.gwt.home.communication.event.minileaderboard.MiniLeaderboardItemDTO;
import com.sap.sailing.gwt.home.communication.eventview.HasRegattaMetadata.RegattaState;
import com.sap.sailing.gwt.home.communication.eventview.RegattaMetadataDTO;
import com.sap.sailing.gwt.home.communication.regatta.RegattaWithProgressDTO;
import com.sap.sailing.gwt.home.server.EventActionUtil.RaceCallback;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.util.RegattaUtil;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.dispatch.shared.commands.ResultWithTTL;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;
import com.sap.sse.gwt.shared.DTO;
import com.sap.sse.security.SecurityService;

/**
 * This class aggregates {@link Leaderboard} information by preparing {@link DTO}s for different components representing
 * a regatta in the UI and providing convenience methods for several other required information.
 */
public class LeaderboardContext {
    private static final Logger logger = Logger.getLogger(LeaderboardContext.class.getName());

    private final TimePoint now = MillisecondsTimePoint.now();
    private final Event event;
    private final Iterable<LeaderboardGroup> leaderboardGroups;
    private final Leaderboard leaderboard;
    private final RacingEventService service;
    private SecurityService securityService;
    private Boolean hasMultipleFleets = null;


    public LeaderboardContext(SailingDispatchContext dispatchContext, Event event, Iterable<LeaderboardGroup> leaderboardGroup, Leaderboard leaderboard) {
        this.service = dispatchContext.getRacingEventService();
        this.securityService = dispatchContext.getSecurityService();
        this.event = event;
        this.leaderboardGroups = leaderboardGroup;
        this.leaderboard = leaderboard;
    }
    
    public void forRacesWithReadPermissions(RaceCallback callback) {
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            if (!raceColumn.isCarryForward()) {
                for (Fleet fleet : raceColumn.getFleets()) {
                    TrackedRace tRaceOrNull = raceColumn.getTrackedRace(fleet);
                    if (tRaceOrNull == null || securityService.hasCurrentUserReadPermission(tRaceOrNull)) {
                        callback.doForRace(new RaceContext(service, event, this, raceColumn, fleet, service));
                    }
                }
            }
        }
    }
    
    public RegattaWithProgressDTO getRegattaWithProgress() {
        RegattaProgressCalculator regattaProgressCalculator = new RegattaProgressCalculator();
        forRacesWithReadPermissions(regattaProgressCalculator);
        RegattaWithProgressDTO regattaDTO = new RegattaWithProgressDTO(regattaProgressCalculator.getResult());
        fillRegattaFields(regattaDTO);
        return regattaDTO;
    }
    
    public ResultWithTTL<GetMiniLeaderboardDTO> calculateMiniLeaderboard(RacingEventService service, int limit) {
        GetMiniLeaderboardDTO result = new GetMiniLeaderboardDTO();
        try {
            final LeaderboardDTO leaderboardDTO = leaderboard.getLeaderboardDTO(null, Collections.<String> emptyList(), true,
                    service, service.getBaseDomainFactory(), false);
            
            result.setScoreCorrectionText(leaderboardDTO.getComment());
            result.setLastScoreUpdate(leaderboardDTO.getTimePointOfLastCorrectionsValidity());
            boolean isLive = hasLiveRace();
            result.setLive(isLive);
            
            int rank = 0;
            for (CompetitorDTO competitor : leaderboardDTO.competitors) {
                rank++;
                LeaderboardRowDTO row = leaderboardDTO.rows.get(competitor);
                int raceCount = 0;
                if(row.fieldsByRaceColumnName != null) {
                    for(LeaderboardEntryDTO lbEntry : row.fieldsByRaceColumnName.values()) {
                        if(lbEntry.netPoints != null) {
                            raceCount++;
                        }
                    }
                }
                result.addItem(new MiniLeaderboardItemDTO(new SimpleCompetitorDTO(competitor), rank, row.netPoints, raceCount));
                if (limit > 0 && rank >= limit) break;
            }
            result.setTotalCompetitorCount(leaderboardDTO.competitors.size());
            return new ResultWithTTL<>(Duration.ONE_MINUTE.times(isLive ? 1 : 2), result);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading leaderboard", e);
            throw new DispatchException("Error loading leaderboard");
        }
    }

    private boolean hasLiveRace() {
        OverallRacesStateCalculator racesStateCalculator = new OverallRacesStateCalculator();
        forRacesWithReadPermissions(racesStateCalculator);
        return racesStateCalculator.hasLiveRace();
    }
    
    public RegattaState calculateRegattaState() {
        // First using event state -> fast and ensures that all regattas are marked as finished after the event is
        // finished
        EventState eventState = HomeServiceUtil.calculateEventState(event);
        if (eventState == EventState.FINISHED) {
            return RegattaState.FINISHED;
        }
        if (eventState == EventState.UPCOMING || eventState == EventState.PLANNED) {
            return RegattaState.UPCOMING;
        }
        // Using regatta start and end -> fast calculation of upcoming and finished states but not helpful to
        // distinguish between live and progress
        TimePoint startDate = getStartTimePoint();
        if (startDate != null && now.before(startDate)) {
            return RegattaState.UPCOMING;
        }
        TimePoint endDate = getEndTimePoint();
        if (endDate != null && now.after(endDate)) {
            return RegattaState.FINISHED;
        }
        // Using the race states to calculate the real state for running events/regattas
        OverallRacesStateCalculator racesStateCalculator = new OverallRacesStateCalculator();
        forRacesWithReadPermissions(racesStateCalculator);
        if (racesStateCalculator.hasLiveRace()) {
            return RegattaState.RUNNING;
        }
        if (!racesStateCalculator.hasUnfinishedRace()) {
            return RegattaState.FINISHED;
        }
        if (racesStateCalculator.hasAbandonedOrPostponedRace() || racesStateCalculator.hasFinishedRace()) {
            return RegattaState.PROGRESS;
        }
        return RegattaState.UPCOMING;
    }
    
    private Date getStartDateWithEventFallback() {
        Date result = getStartDate();
        if(result != null) {
            return result;
        }
        return event.getStartDate().asDate();
    }
    
    private Date getStartDate() {
        TimePoint timePoint = getStartTimePoint();
        if(timePoint != null) {
            return timePoint.asDate();
        }
        return null;
    }
    
    private TimePoint getStartTimePoint() {
        if(leaderboard instanceof RegattaLeaderboard) {
            Regatta regatta = ((RegattaLeaderboard) leaderboard).getRegatta();
            return regatta.getStartDate();
        }
        return null;
    }
    
    private Date getEndDateWithEventFallback() {
        Date result = getEndDate();
        if(result != null) {
            return result;
        }
        return event.getEndDate().asDate();
    }
    
    private Date getEndDate() {
        TimePoint timePoint = getEndTimePoint();
        if(timePoint != null) {
            return timePoint.asDate();
        }
        return null;
    }
    
    private TimePoint getEndTimePoint() {
        if(leaderboard instanceof RegattaLeaderboard) {
            Regatta regatta = ((RegattaLeaderboard) leaderboard).getRegatta();
            return regatta.getEndDate();
        }
        return null;
    }

    public String getLeaderboardName() {
        return leaderboard.getName();
    }
    
    public Iterable<String> getLeaderboardGroupNames() {
        final List<String> result = new ArrayList<>();
        for (final LeaderboardGroup lg : leaderboardGroups) {
            result.add(lg.getName());
        }
        return result;
    }
    
    public Iterable<UUID> getLeaderboardGroupIds() {
        final List<UUID> result = new ArrayList<>();
        for (final LeaderboardGroup lg : leaderboardGroups) {
            result.add(lg.getId());
        }
        return result;
    }

    public RegattaMetadataDTO asRegattaMetadataDTO() {
        RegattaMetadataDTO regattaDTO = new RegattaMetadataDTO();
        fillRegattaFields(regattaDTO);
        
        return regattaDTO;
    }
    
    private int calculateRaceCount(Leaderboard sl) {
        int result = 0;
        for (RaceColumn column : sl.getRaceColumns()) {
            if (!column.isCarryForward()) {
                result += Util.size(column.getFleets());
            }
        }
        return result;
    }

    public void fillRegattaFields(RegattaMetadataDTO regattaDTO) {
        regattaDTO.setId(getLeaderboardName());
        regattaDTO.setDisplayName(leaderboard.getDisplayName() != null ? leaderboard.getDisplayName() : leaderboard.getName());
        if (hasMultipleLeaderboardGroups(event)) {
            for (final LeaderboardGroup lg : leaderboardGroups) {
                regattaDTO.addLeaderboardGroupName(lg.getDisplayName() != null ? lg.getDisplayName() : lg.getName());
            }
        }
        if (Util.size(leaderboardGroups) == 1) {
            final LeaderboardGroup singleLeaderboardGroup = leaderboardGroups.iterator().next();
            if (singleLeaderboardGroup.hasOverallLeaderboard()) {
                regattaDTO.setSeriesReference(
                        new EventSeriesReferenceDTO(HomeServiceUtil.getLeaderboardDisplayName(singleLeaderboardGroup),
                                singleLeaderboardGroup.getId()));
            }
        }
        regattaDTO.setCompetitorsCount(HomeServiceUtil.calculateCompetitorsCount(leaderboard));
        regattaDTO.setRaceCount(calculateRaceCount(leaderboard));
        regattaDTO.setBoatClass(HomeServiceUtil.getBoatClassName(leaderboard));
        if (leaderboard instanceof RegattaLeaderboard) {
            regattaDTO.setStartDate(getStartDateWithEventFallback());
            regattaDTO.setEndDate(getEndDateWithEventFallback());
        }
        regattaDTO.setState(calculateRegattaState());
        regattaDTO.setDefaultCourseAreaName(HomeServiceUtil.getCourseAreaNameForRegattaIfThereIsMoreThanOne(event, leaderboard));
        regattaDTO.setCourseAreaIdsAsStrings(HomeServiceUtil.getCourseAreaIdsAsStringsForRegatta(event, leaderboard));
        regattaDTO.setFlexibleLeaderboard(leaderboard instanceof FlexibleLeaderboard);
        
        RegattaRaceDataInfoCalculator regattaRaceDataInfoCalculator = new RegattaRaceDataInfoCalculator();
        forRacesWithReadPermissions(regattaRaceDataInfoCalculator);
        regattaDTO.setRaceDataInfo(regattaRaceDataInfoCalculator.getRaceDataInfo());
        regattaDTO.setBuoyZoneRadius(getRegattaBuoyZoneRadius());
    }

    private Distance getRegattaBuoyZoneRadius() {
        Regatta regatta = service.getRegattaByName(getLeaderboardName());
        BoatClass boatClass = HomeServiceUtil.getBoatClass(leaderboard);
        return RegattaUtil.getCalculatedRegattaBuoyZoneRadius(regatta, boatClass);
    }

    private static boolean hasMultipleLeaderboardGroups(EventBase event) {
        return Util.size(event.getLeaderboardGroups()) > 1;
    }

    public boolean isPartOfEvent() {
        return leaderboard.isPartOfEvent(event);
    }
    
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }
    
    private boolean calculateHasMultipleFleets() {
        Set<Fleet> fleets = new HashSet<Fleet>();
        for (RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            Util.addAll(raceColumn.getFleets(), fleets);
            if (fleets.size() > 1) {
                return true;
            }
        }
        return false;
    }
        
    public boolean hasMultipleFleets() {
        if (hasMultipleFleets == null) {
            hasMultipleFleets = calculateHasMultipleFleets();
        }
        return hasMultipleFleets;
    }
    
    public Iterable<LeaderboardGroup> getLeaderboardGroups() {
        return leaderboardGroups;
    }
}
