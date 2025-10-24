package com.sap.sailing.datamining;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sailing.datamining.data.HasBravoFixContext;
import com.sap.sailing.datamining.data.HasBravoFixTrackContext;
import com.sap.sailing.datamining.data.HasCompetitorContext;
import com.sap.sailing.datamining.data.HasCompetitorDayContext;
import com.sap.sailing.datamining.data.HasCompleteManeuverCurveWithEstimationDataContext;
import com.sap.sailing.datamining.data.HasFoilingSegmentContext;
import com.sap.sailing.datamining.data.HasGPSFixContext;
import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.data.HasManeuverContext;
import com.sap.sailing.datamining.data.HasManeuverSpeedDetailsContext;
import com.sap.sailing.datamining.data.HasMarkPassingContext;
import com.sap.sailing.datamining.data.HasRaceOfCompetitorContext;
import com.sap.sailing.datamining.data.HasRaceResultOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTackTypeSegmentContext;
import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedLegSliceOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.data.HasWindFixContext;
import com.sap.sailing.datamining.data.HasWindTrackContext;
import com.sap.sailing.datamining.data.TerminalHasTrackedLegSliceOfCompetitorContext;
import com.sap.sailing.datamining.impl.components.BravoFixRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.BravoFixTrackRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.CompetitorDayRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.CompetitorOfRaceInLeaderboardRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.CompetitorRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.CompleteManeuverCurveWithEstimationDataRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.FoilingSegmentRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.GPSFixRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.LeaderboardGroupRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.LeaderboardRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.ManeuverRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.ManeuverSpeedDetailsRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.MarkPassingRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.RaceOfCompetitorRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TackTypeSegmentRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TerminalTrackedLegOfCompetitorRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TrackedLegOfCompetitorRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TrackedLegRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TrackedLegSliceOfCompetitorRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TrackedRaceRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.WindFixRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.WindTrackRetrievalProcessor;
import com.sap.sailing.datamining.shared.FoilingSegmentsDataMiningSettings;
import com.sap.sailing.datamining.shared.ManeuverSettings;
import com.sap.sailing.datamining.shared.ManeuverSettingsImpl;
import com.sap.sailing.datamining.shared.ManeuverSpeedDetailsSettings;
import com.sap.sailing.datamining.shared.ManeuverSpeedDetailsSettingsImpl;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.impl.components.SimpleDataRetrieverChainDefinition;

public class SailingDataRetrievalChainDefinitions {

    private final Collection<DataRetrieverChainDefinition<?, ?>> dataRetrieverChainDefinitions;

    public SailingDataRetrievalChainDefinitions() {
        dataRetrieverChainDefinitions = new ArrayList<>();
        final DataRetrieverChainDefinition<RacingEventService, HasLeaderboardContext> leaderboardRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                RacingEventService.class, HasLeaderboardContext.class, "LeaderboardSailingDomainRetrieverChain");
        leaderboardRetrieverChainDefinition.startWith(LeaderboardGroupRetrievalProcessor.class, HasLeaderboardGroupContext.class, "LeaderboardGroup");
        leaderboardRetrieverChainDefinition.endWith(LeaderboardGroupRetrievalProcessor.class, LeaderboardRetrievalProcessor.class,
                HasLeaderboardContext.class, "Leaderboard");
        dataRetrieverChainDefinitions.add(leaderboardRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasCompetitorContext> competitorRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                leaderboardRetrieverChainDefinition, HasCompetitorContext.class, "CompetitorSailingDomainRetrieverChain");
        competitorRetrieverChainDefinition.endWith(LeaderboardRetrievalProcessor.class, CompetitorRetrievalProcessor.class, HasCompetitorContext.class, "CompetitorInLeaderboard");
        dataRetrieverChainDefinitions.add(competitorRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasTrackedRaceContext> trackedRaceResultOfCompetitorRetrieverChainDefinition =
                new SimpleDataRetrieverChainDefinition<>(leaderboardRetrieverChainDefinition, HasTrackedRaceContext.class, "TrackedRaceResultOfCompetitorRetrieverChain");
        trackedRaceResultOfCompetitorRetrieverChainDefinition.endWith(LeaderboardRetrievalProcessor.class, TrackedRaceRetrievalProcessor.class, HasTrackedRaceContext.class, "Race");
        dataRetrieverChainDefinitions.add(trackedRaceResultOfCompetitorRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasRaceResultOfCompetitorContext> raceResultOfCompetitorRetrieverChainDefinition =
                new SimpleDataRetrieverChainDefinition<>(trackedRaceResultOfCompetitorRetrieverChainDefinition, HasRaceResultOfCompetitorContext.class, "RaceResultSailingDomainRetrieverChain");
        raceResultOfCompetitorRetrieverChainDefinition.endWith(TrackedRaceRetrievalProcessor.class, CompetitorOfRaceInLeaderboardRetrievalProcessor.class, HasRaceResultOfCompetitorContext.class, "Competitor");
        dataRetrieverChainDefinitions.add(raceResultOfCompetitorRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasTrackedRaceContext> trackedRaceRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                leaderboardRetrieverChainDefinition, HasTrackedRaceContext.class, "RaceSailingDomainRetrieverChain");
        trackedRaceRetrieverChainDefinition.endWith(LeaderboardRetrievalProcessor.class, TrackedRaceRetrievalProcessor.class,
                HasTrackedRaceContext.class, "Race");
        dataRetrieverChainDefinitions.add(trackedRaceRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasRaceOfCompetitorContext> raceOfCompetitorRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                trackedRaceRetrieverChainDefinition, HasRaceOfCompetitorContext.class, "RaceOfCompetitorSailingDomainRetrieverChain");
        // use TackTypeSegmentsDataMiningSettings here to support the per-leg statistics about tack type segments
        raceOfCompetitorRetrieverChainDefinition.endWith(TrackedRaceRetrievalProcessor.class, RaceOfCompetitorRetrievalProcessor.class, HasRaceOfCompetitorContext.class,
                TackTypeSegmentsDataMiningSettings.class, TackTypeSegmentsDataMiningSettings.createDefaultSettings(), "Competitor");
        dataRetrieverChainDefinitions.add(raceOfCompetitorRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasCompetitorDayContext> competitorDayRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                raceOfCompetitorRetrieverChainDefinition, HasCompetitorDayContext.class, "CompetitorDaySailingDomainRetrieverChain");
        competitorDayRetrieverChainDefinition.endWith(RaceOfCompetitorRetrievalProcessor.class, CompetitorDayRetrievalProcessor.class, HasCompetitorDayContext.class, "CompetitorDay");
        dataRetrieverChainDefinitions.add(competitorDayRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasBravoFixTrackContext> bravoFixTrackRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                raceOfCompetitorRetrieverChainDefinition, HasBravoFixTrackContext.class, "BravoFixTrackSailingDomainRetrieverChain");
        bravoFixTrackRetrieverChainDefinition.endWith(RaceOfCompetitorRetrievalProcessor.class, BravoFixTrackRetrievalProcessor.class,
                HasBravoFixTrackContext.class, "BravoFixTrack");
        dataRetrieverChainDefinitions.add(bravoFixTrackRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasFoilingSegmentContext> foilingSegmentsRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                raceOfCompetitorRetrieverChainDefinition, HasFoilingSegmentContext.class, "FoilingSegmentsSailingDomainRetrieverChain");
        foilingSegmentsRetrieverChainDefinition.endWith(RaceOfCompetitorRetrievalProcessor.class, FoilingSegmentRetrievalProcessor.class,
                HasFoilingSegmentContext.class, FoilingSegmentsDataMiningSettings.class, FoilingSegmentsDataMiningSettings.createDefaultSettings(), "FoilingSegments");
        dataRetrieverChainDefinitions.add(foilingSegmentsRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasTackTypeSegmentContext> tackTypeSegmentsRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                raceOfCompetitorRetrieverChainDefinition, HasTackTypeSegmentContext.class, "tackTypeSegmentsRetrieverChainDefinition");
        tackTypeSegmentsRetrieverChainDefinition.endWith(RaceOfCompetitorRetrievalProcessor.class, TackTypeSegmentRetrievalProcessor.class,
                HasTackTypeSegmentContext.class, TackTypeSegmentsDataMiningSettings.class, TackTypeSegmentsDataMiningSettings.createDefaultSettings(), "TackTypeSegments");
        dataRetrieverChainDefinitions.add(tackTypeSegmentsRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasTrackedLegContext> legRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                trackedRaceRetrieverChainDefinition, HasTrackedLegContext.class, "LegSailingDomainRetrieverChain");
        legRetrieverChainDefinition.endWith(TrackedRaceRetrievalProcessor.class, TrackedLegRetrievalProcessor.class,
                HasTrackedLegContext.class, "Leg");
        dataRetrieverChainDefinitions.add(legRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasTrackedLegOfCompetitorContext> legOfCompetitorRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                legRetrieverChainDefinition, HasTrackedLegOfCompetitorContext.class, "LegOfCompetitorSailingDomainRetrieverChain");
        // use TackTypeSegmentsDataMiningSettings here to support the per-leg statistics about tack type segments
        legOfCompetitorRetrieverChainDefinition.endWith(TrackedLegRetrievalProcessor.class, TrackedLegOfCompetitorRetrievalProcessor.class,
                HasTrackedLegOfCompetitorContext.class, TackTypeSegmentsDataMiningSettings.class, TackTypeSegmentsDataMiningSettings.createDefaultSettings(), "LegOfCompetitor");
        //
        final DataRetrieverChainDefinition<RacingEventService, TerminalHasTrackedLegSliceOfCompetitorContext> terminalLegOfCompetitorRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                legRetrieverChainDefinition, TerminalHasTrackedLegSliceOfCompetitorContext.class, "LegOfCompetitorSailingDomainRetrieverChain");
        // use TackTypeSegmentsDataMiningSettings here to support the per-leg statistics about tack type segments
        terminalLegOfCompetitorRetrieverChainDefinition.endWith(TrackedLegRetrievalProcessor.class, TerminalTrackedLegOfCompetitorRetrievalProcessor.class,
                TerminalHasTrackedLegSliceOfCompetitorContext.class, TackTypeSegmentsDataMiningSettings.class, TackTypeSegmentsDataMiningSettings.createDefaultSettings(), "LegOfCompetitor");
        dataRetrieverChainDefinitions.add(terminalLegOfCompetitorRetrieverChainDefinition);
        // tenth of a competitor's tracked leg retriever, based on the tracked leg of competitor retriever:
        final DataRetrieverChainDefinition<RacingEventService, HasTrackedLegSliceOfCompetitorContext> legOfCompetitorSliceRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                legOfCompetitorRetrieverChainDefinition, HasTrackedLegSliceOfCompetitorContext.class, "LegOfCompetitorSliceSailingDomainRetrieverChain");
        // use TackTypeSegmentsDataMiningSettings here to support the per-leg statistics about tack type segments
        legOfCompetitorSliceRetrieverChainDefinition.endWith(TrackedLegOfCompetitorRetrievalProcessor.class, TrackedLegSliceOfCompetitorRetrievalProcessor.class,
                HasTrackedLegSliceOfCompetitorContext.class, TackTypeSegmentsDataMiningSettings.class, TackTypeSegmentsDataMiningSettings.createDefaultSettings(), "LegOfCompetitorSlice");
        dataRetrieverChainDefinitions.add(legOfCompetitorSliceRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasGPSFixContext> gpsFixRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                legOfCompetitorRetrieverChainDefinition, HasGPSFixContext.class, "GPSFixSailingDomainRetrieverChain");
        gpsFixRetrieverChainDefinition.endWith(TrackedLegOfCompetitorRetrievalProcessor.class, GPSFixRetrievalProcessor.class,
                HasGPSFixContext.class, "GpsFix");
        dataRetrieverChainDefinitions.add(gpsFixRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasBravoFixContext> bravoFixRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                legOfCompetitorRetrieverChainDefinition, HasBravoFixContext.class, "BravoFixSailingDomainRetrieverChain");
        bravoFixRetrieverChainDefinition.endWith(TrackedLegOfCompetitorRetrievalProcessor.class, BravoFixRetrievalProcessor.class,
                HasBravoFixContext.class, "BravoFix");
        dataRetrieverChainDefinitions.add(bravoFixRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasWindFixContext> windFixRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                trackedRaceRetrieverChainDefinition, HasWindFixContext.class, "WindFixSailingDomainRetrieverChain");
        windFixRetrieverChainDefinition.addAfter(TrackedRaceRetrievalProcessor.class, WindTrackRetrievalProcessor.class, HasWindTrackContext.class, "WindTrack");
        windFixRetrieverChainDefinition.endWith(WindTrackRetrievalProcessor.class, WindFixRetrievalProcessor.class,
                HasWindFixContext.class, "WindFix");
        dataRetrieverChainDefinitions.add(windFixRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasManeuverContext> maneuverRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                legOfCompetitorRetrieverChainDefinition, HasManeuverContext.class, "ManeuverSailingDomainRetrieverChain");
        maneuverRetrieverChainDefinition.endWith(TrackedLegOfCompetitorRetrievalProcessor.class, ManeuverRetrievalProcessor.class,
                HasManeuverContext.class, ManeuverSettings.class, ManeuverSettingsImpl.createDefault(), "Maneuver");
        dataRetrieverChainDefinitions.add(maneuverRetrieverChainDefinition);
        //
        DataRetrieverChainDefinition<RacingEventService, HasManeuverSpeedDetailsContext> speedDetailsDataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                maneuverRetrieverChainDefinition, HasManeuverSpeedDetailsContext.class,
                "ManeuverSpeedDetailsRetrieverChain");
        speedDetailsDataRetrieverChainDefinition.endWith(ManeuverRetrievalProcessor.class,
                ManeuverSpeedDetailsRetrievalProcessor.class, HasManeuverSpeedDetailsContext.class,
                ManeuverSpeedDetailsSettings.class, ManeuverSpeedDetailsSettingsImpl.createDefault(),
                "ManeuverSpeedDetails");
        dataRetrieverChainDefinitions.add(speedDetailsDataRetrieverChainDefinition);
        //
        DataRetrieverChainDefinition<RacingEventService, HasCompleteManeuverCurveWithEstimationDataContext> completeManeuverCurveWithEstimationDataRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                raceOfCompetitorRetrieverChainDefinition, HasCompleteManeuverCurveWithEstimationDataContext.class,
                "CompleteManeuverCurveWithEstimationDataRetrieverChain");
        completeManeuverCurveWithEstimationDataRetrieverChainDefinition.endWith(RaceOfCompetitorRetrievalProcessor.class,
                CompleteManeuverCurveWithEstimationDataRetrievalProcessor.class, HasCompleteManeuverCurveWithEstimationDataContext.class, ManeuverSettings.class, ManeuverSettingsImpl.createDefault(),
                "CompleteManeuverCurveWithEstimationData");
        dataRetrieverChainDefinitions.add(completeManeuverCurveWithEstimationDataRetrieverChainDefinition);
        //
        final DataRetrieverChainDefinition<RacingEventService, HasMarkPassingContext> markPassingRetrieverChainDefinition = new SimpleDataRetrieverChainDefinition<>(
                legOfCompetitorRetrieverChainDefinition, HasMarkPassingContext.class, "MarkPassingSailingDomainRetrieverChain");
        markPassingRetrieverChainDefinition.endWith(TrackedLegOfCompetitorRetrievalProcessor.class, MarkPassingRetrievalProcessor.class, HasMarkPassingContext.class, "MarkPassing");
        dataRetrieverChainDefinitions.add(markPassingRetrieverChainDefinition);
    }

    public Iterable<DataRetrieverChainDefinition<?, ?>> get() {
        return dataRetrieverChainDefinitions;
    }

}
