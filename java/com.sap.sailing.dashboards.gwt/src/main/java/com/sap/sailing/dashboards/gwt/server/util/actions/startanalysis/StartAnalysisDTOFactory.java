package com.sap.sailing.dashboards.gwt.server.util.actions.startanalysis;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.dashboards.gwt.shared.StartlineAdvantageType;
import com.sap.sailing.dashboards.gwt.shared.dispatch.DashboardDispatchContext;
import com.sap.sailing.dashboards.gwt.shared.dto.StartAnalysisCompetitorDTO;
import com.sap.sailing.dashboards.gwt.shared.dto.StartAnalysisDTO;
import com.sap.sailing.dashboards.gwt.shared.dto.StartAnalysisRankingTableEntryDTO;
import com.sap.sailing.dashboards.gwt.shared.dto.StartLineAdvantageDTO;
import com.sap.sailing.dashboards.gwt.shared.dto.WindAndAdvantagesInfoForStartLineDTO;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

@GwtIncompatible
public final class StartAnalysisDTOFactory extends AbstractStartAnalysisCreationValidator {

    private static int DEFAULT_GATE_START_INTERVALL_IN_MILLISECONDS = 5*60*1000;
    private static int ONE_MINUTE_INTERVALL_IN_MILLISECONDS = 60*1000;

    private static final Logger logger = Logger.getLogger(StartAnalysisDTOFactory.class.getName());

    private StartAnalysisDTOFactory() {}

    public static StartAnalysisDTO createStartAnalysisForCompetitorAndTrackedRace(DashboardDispatchContext dashboardDispatchContext, Competitor competitor, TrackedRace trackedRace) {
        StartAnalysisDTO startAnalysisDTO = new StartAnalysisDTO();
        addStaticDataToStartAnalysisDTOFrom(startAnalysisDTO, trackedRace);
        List<StartAnalysisCompetitorDTO> competitors = new ArrayList<StartAnalysisCompetitorDTO>();
        Waypoint secondWaypoint = trackedRace.getRace().getCourse().getFirstLeg().getTo();
        List<MarkPassing> markPassingsInOrder = convertMarkpPassingsIteratorToList(trackedRace.getMarkPassingsInOrder(secondWaypoint).iterator());
        boolean isCompetitorNullOrOneOfFirstThreeCompetitors = false;
        for (int i = 0; i < MINIMUM_MARKPASSIINGS_AT_FIRST_MARK; i++) {
            if (markPassingsInOrder.size() - 1 >= i) {
                competitors.add(createStartAnalysisCompetitorDTO(dashboardDispatchContext, trackedRace, i+1, markPassingsInOrder.get(i).getCompetitor()));
                if (competitor == null || markPassingsInOrder.get(i).getCompetitor().equals(competitor)) {
                    isCompetitorNullOrOneOfFirstThreeCompetitors = true;
                }
            }
        }
        if (competitor != null && !isCompetitorNullOrOneOfFirstThreeCompetitors) {
            int rankOfCompetitorWhilePassingSecondWaypoint = getRankOfCompetitorWhilePassingSecondWaypoint(competitor, trackedRace);
            competitors.add(createStartAnalysisCompetitorDTO(dashboardDispatchContext, trackedRace, rankOfCompetitorWhilePassingSecondWaypoint, competitor));
        }
        if (competitor != null) {
            startAnalysisDTO.competitor = dashboardDispatchContext.getRacingEventService().getBaseDomainFactory().getCompetitorAndBoatStore().convertToCompetitorDTO(competitor);
            logger.log(Level.INFO, "Created startanalysis for competitor"+competitor);
        }
        startAnalysisDTO.startAnalysisCompetitorDTOs = competitors;
        final Boolean isGateStart = trackedRace.isGateStart();
        if(isGateStart == Boolean.TRUE){
            logger.log(Level.INFO, "Creating startanalysis for gate start");
            startAnalysisDTO.racingProcedureType = RacingProcedureType.GateStart;
            long timePointOfGolfDownTime = trackedRace.getGateStartGolfDownTime();
            startAnalysisDTO.timeOfStartInMilliSeconds = trackedRace.getStartOfRace().asMillis();
            if (timePointOfGolfDownTime < trackedRace.getStartOfRace().asMillis() || timePointOfGolfDownTime == 0) {
                startAnalysisDTO.tailLenghtInMilliseconds = DEFAULT_GATE_START_INTERVALL_IN_MILLISECONDS;
            } else {
                startAnalysisDTO.tailLenghtInMilliseconds = timePointOfGolfDownTime-trackedRace.getStartOfRace().asMillis();
            }
        } else {
            logger.log(Level.INFO, "Creating startanalysis for ess start");
            startAnalysisDTO.racingProcedureType = RacingProcedureType.ESS;
            startAnalysisDTO.timeOfStartInMilliSeconds = trackedRace.getStartOfRace().asMillis();
            startAnalysisDTO.tailLenghtInMilliseconds = ONE_MINUTE_INTERVALL_IN_MILLISECONDS;
        }
        startAnalysisDTO.regattaAndRaceIdentifier = trackedRace.getRaceIdentifier();
        logger.log(Level.INFO, "Created startanalysis for "+ trackedRace.getRace().getName());
        return startAnalysisDTO;
    }

    private static List<MarkPassing> convertMarkpPassingsIteratorToList(Iterator<MarkPassing> iterator) {
        List<MarkPassing> list = new ArrayList<MarkPassing>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    private static int getRankOfCompetitorWhilePassingSecondWaypoint(Competitor competitor, TrackedRace trackedRace) {
        Waypoint secondWaypoint = trackedRace.getRace().getCourse().getFirstLeg().getTo();
        Iterator<MarkPassing> markPassings = trackedRace.getMarkPassingsInOrder(secondWaypoint).iterator();
        int counter = 0;
        while (markPassings.hasNext()) {
            counter++;
            if (markPassings.next().getCompetitor().getId().equals(competitor.getId()))
                return counter;
        }
        return 0;
    }

    private static StartAnalysisDTO addStaticDataToStartAnalysisDTOFrom(StartAnalysisDTO startAnalysisDTO, TrackedRace trackedRace) {
        startAnalysisDTO.raceName = trackedRace.getRace().getName();
        startAnalysisDTO.startAnalysisWindLineInfoDTO = createStartAnalysisWindAndLineData(trackedRace);
        return startAnalysisDTO;
    }

    private static StartAnalysisRankingTableEntryDTO createRankTableEntry(TrackedRace trackedRace, int rank,
            Competitor competitor) {
        StartAnalysisRankingTableEntryDTO startAnalysisRankTableEntryDTO = createStartAnalysisRankTableEntryDTOWithRankAndStartTimepoint(
                competitor, rank, trackedRace);
        return startAnalysisRankTableEntryDTO;
    }

    private static StartAnalysisRankingTableEntryDTO createStartAnalysisRankTableEntryDTOWithRankAndStartTimepoint(
            Competitor competitor, int rank, TrackedRace trackedRace) {
        StartAnalysisRankingTableEntryDTO tableentry = new StartAnalysisRankingTableEntryDTO();
        tableentry.rankAtFirstMark = rank;
        tableentry.teamName = competitor.getName();
        tableentry.speedAtStartTime = trackedRace.getSpeed(competitor, 1).getKnots();
        final Boolean isGateStart = trackedRace.isGateStart();
        if(isGateStart == Boolean.TRUE){
            tableentry.distanceToLineAtStartTime = trackedRace.getDistanceFromStarboardSideOfStartLineWhenPassingStart(
                    competitor).getMeters();
        } else {
            tableentry.distanceToLineAtStartTime = trackedRace.getDistanceToStartLine(competitor, 0).getMeters();
        }
        tableentry.tailColor = "#100100";
        return tableentry;
    }

    private static WindAndAdvantagesInfoForStartLineDTO createStartAnalysisWindAndLineData(TrackedRace trackedRace) {
        WindAndAdvantagesInfoForStartLineDTO startAnalysisWindLineInfoDTO = new WindAndAdvantagesInfoForStartLineDTO();
        final TimePoint startOfRace = trackedRace.getStartOfRace();
        final TimePoint timePoint = startOfRace == null ? MillisecondsTimePoint.now() : startOfRace;
        LineDetails startline = trackedRace.getStartLine(timePoint);
        StartLineAdvantageDTO startLineAdvantageDTO = new StartLineAdvantageDTO();
        startLineAdvantageDTO.startLineAdvantageType = getStartlineAdvantageType(trackedRace, new MillisecondsTimePoint(
                new Date()));
        startLineAdvantageDTO.startLineAdvantage = startline == null ? null : startline.getAdvantage().getMeters();
        startAnalysisWindLineInfoDTO.startLineAdvantage = startLineAdvantageDTO;
	if (trackedRace.getStartTimeReceived() != null) {
	    Position portMarkPosition = trackedRace.getOrCreateTrack(
		    trackedRace.getStartLine(trackedRace.getStartTimeReceived()).getStarboardMarkWhileApproachingLine())
		    .getEstimatedPosition(trackedRace.getStartTimeReceived(), /* extrapolate */
		    false);
	    Wind windAtStart = trackedRace.getWind(portMarkPosition, trackedRace.getStartTimeReceived());
	    startAnalysisWindLineInfoDTO.windDirectionInDegrees = windAtStart.getBearing().getDegrees();
	    startAnalysisWindLineInfoDTO.windSpeedInKnots = windAtStart.getKnots();
	} else {
	    startAnalysisWindLineInfoDTO.windDirectionInDegrees = 0.0;
	    startAnalysisWindLineInfoDTO.windSpeedInKnots = 0.0;
	}
        return startAnalysisWindLineInfoDTO;
    }

    private static StartlineAdvantageType getStartlineAdvantageType(TrackedRace trackedRace, TimePoint timePoint) {
        try {
            LegType typeOfFirstLeg;
            typeOfFirstLeg = getFirstLegTypeOfTrackedRaceAtTimePoint(trackedRace, timePoint);
            switch (typeOfFirstLeg) {
            case UPWIND:
                return StartlineAdvantageType.WIND;
            case REACHING:
                return StartlineAdvantageType.GEOMETRIC;
            default:
                return StartlineAdvantageType.WIND;
            }
        } catch (NoWindException e) {
            logger.log(Level.INFO, "", e);
            return null;
        }
    }

    private static LegType getFirstLegTypeOfTrackedRaceAtTimePoint(TrackedRace trackedRace, TimePoint timePoint)
            throws NoWindException {
        Iterable<TrackedLeg> trackedLegs = trackedRace.getTrackedLegs();
        if (trackedLegs != null && trackedLegs.iterator().hasNext()) {
            TrackedLeg firstLegInTrackedRace = trackedLegs.iterator().next();
            return firstLegInTrackedRace.getLegType(timePoint);
        } else {
            return null;
        }
    }

    private static StartAnalysisCompetitorDTO createStartAnalysisCompetitorDTO(DashboardDispatchContext dashboardDispatchContext, TrackedRace trackedRace, int rank, Competitor competitor) {
        StartAnalysisCompetitorDTO startAnalysisCompetitorDTOsForRace = new StartAnalysisCompetitorDTO();
        startAnalysisCompetitorDTOsForRace.competitorDTO = dashboardDispatchContext.getRacingEventService().getBaseDomainFactory().getCompetitorAndBoatStore()
                .convertToCompetitorDTO(competitor);
        startAnalysisCompetitorDTOsForRace.boatDTO = dashboardDispatchContext.getRacingEventService().getBaseDomainFactory().getCompetitorAndBoatStore()
                .convertToBoatDTO(trackedRace.getBoatOfCompetitor(competitor));
        startAnalysisCompetitorDTOsForRace.rankingTableEntryDTO = createRankTableEntry(trackedRace, rank, competitor);
        return startAnalysisCompetitorDTOsForRace;
    }
}
