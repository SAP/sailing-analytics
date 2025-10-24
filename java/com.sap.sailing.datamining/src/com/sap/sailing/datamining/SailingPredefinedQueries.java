package com.sap.sailing.datamining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.datamining.data.HasGPSFixContext;
import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.data.HasRaceOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.impl.components.GPSFixRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.LeaderboardGroupRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.LeaderboardRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.RaceOfCompetitorRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TerminalTrackedLegOfCompetitorRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TrackedLegRetrievalProcessor;
import com.sap.sailing.datamining.impl.components.TrackedRaceRetrievalProcessor;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.Distance;
import com.sap.sse.datamining.shared.data.AverageWithStats;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.PredefinedQueryIdentifier;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.LocalizedTypeDTO;
import com.sap.sse.datamining.shared.impl.dto.ModifiableStatisticQueryDefinitionDTO;

public class SailingPredefinedQueries {
    private final Map<PredefinedQueryIdentifier, StatisticQueryDefinitionDTO> predefinedQueries;

    public final static String QUERY_AVERAGE_SPEED_PER_REGATTA_RACE = "AvgSpeed_Per_Regatta-Race";
    public final static String QUERY_AVERAGE_SPEED_PER_COMPETITOR_LEGTYPE = "AvgSpeed_Per_Competitor-LegType";
    public final static String QUERY_AVERAGE_SPEED_PER_COMPETITOR = "AvgSpeed_Per_Competitor";
    public final static String QUERY_DISTANCE_TRAVELED_PER_COMPETITOR_LEGTYPE = "DistanceTraveled_Per_Competitor-LegType";
    public final static String QUERY_DISTANCE_TRAVELED_PER_COMPETITOR = "DistanceTraveled_Per_Competitor";
    public final static String QUERY_MANEUVERS_PER_COMPETITOR = "Maneuvers_Per_Competitor";
    
    public SailingPredefinedQueries() {
        predefinedQueries = new HashMap<>();
        predefinedQueries.put(new PredefinedQueryIdentifier(QUERY_AVERAGE_SPEED_PER_REGATTA_RACE, "Average Speed grouped by regatta name and race name"),
                              create_AvgSpeed_Per_Regatta_Race());
        predefinedQueries.put(new PredefinedQueryIdentifier(QUERY_AVERAGE_SPEED_PER_COMPETITOR_LEGTYPE, "Average speed grouped by competitor/team name and leg type"),
                              create_AvgSpeed_Per_Competitor_LegType());
        predefinedQueries.put(new PredefinedQueryIdentifier(QUERY_AVERAGE_SPEED_PER_COMPETITOR, "Average speed grouped by competitor/team name"),
                create_AvgSpeed_Per_Competitor());
        predefinedQueries.put(new PredefinedQueryIdentifier(QUERY_DISTANCE_TRAVELED_PER_COMPETITOR_LEGTYPE, "Distance traveled grouped by competitor/team name and leg type"),
                              create_SumTraveledDistance_Per_Competitor_LegType());
        predefinedQueries.put(new PredefinedQueryIdentifier(QUERY_DISTANCE_TRAVELED_PER_COMPETITOR, "Distance traveled grouped by competitor/team name"),
                create_SumTraveledDistance_Per_Competitor());
        predefinedQueries.put(new PredefinedQueryIdentifier(QUERY_MANEUVERS_PER_COMPETITOR, "Maneuvers grouped by competitor/team name"),
                              create_SumManeuvers_Per_Competitor());
    }
    
    private StatisticQueryDefinitionDTO create_AvgSpeed_Per_Regatta_Race() {
        FunctionDTO statistic = new FunctionDTO(false, "getGPSFix().getSpeed().getKnots()", HasGPSFixContext.class.getName(), double.class.getName(), new ArrayList<String>(), "", 0);
        AggregationProcessorDefinitionDTO aggregator = new AggregationProcessorDefinitionDTO("Average", Number.class.getName(), AverageWithStats.class.getName(), "");

        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, LeaderboardGroupRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardGroupContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, LeaderboardRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(2, TrackedRaceRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedRaceContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(3, TrackedLegRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(4, TerminalTrackedLegOfCompetitorRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegOfCompetitorContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(5, GPSFixRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasGPSFixContext.class.getName(), ""), null));
        DataRetrieverChainDefinitionDTO retrieverChain = new DataRetrieverChainDefinitionDTO("", RacingEventService.class.getName(), retrieverLevels);

        ModifiableStatisticQueryDefinitionDTO queryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statistic, aggregator, retrieverChain);

        FunctionDTO dimensionToGroupBy0 = new FunctionDTO(true, "getTrackedLegOfCompetitorContext().getTrackedLegContext().getTrackedRaceContext().getRegatta().getName()", HasGPSFixContext.class.getName(), String.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy0);

        FunctionDTO dimensionToGroupBy1 = new FunctionDTO(true, "getTrackedLegOfCompetitorContext().getTrackedLegContext().getTrackedRaceContext().getRace().getName()", HasGPSFixContext.class.getName(), String.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy1);
        
        return queryDefinition;
    }
    
    private StatisticQueryDefinitionDTO create_AvgSpeed_Per_Competitor_LegType() {
        FunctionDTO statistic = new FunctionDTO(false, "getGPSFix().getSpeed().getKnots()", HasGPSFixContext.class.getName(), double.class.getName(), new ArrayList<String>(), "", 0);
        AggregationProcessorDefinitionDTO aggregator = new AggregationProcessorDefinitionDTO("Average", Number.class.getName(), AverageWithStats.class.getName(), "");

        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, LeaderboardGroupRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardGroupContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, LeaderboardRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(2, TrackedRaceRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedRaceContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(3, TrackedLegRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(4, TerminalTrackedLegOfCompetitorRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegOfCompetitorContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(5, GPSFixRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasGPSFixContext.class.getName(), ""), null));
        DataRetrieverChainDefinitionDTO retrieverChain = new DataRetrieverChainDefinitionDTO("", RacingEventService.class.getName(), retrieverLevels);

        ModifiableStatisticQueryDefinitionDTO queryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statistic, aggregator, retrieverChain);

        FunctionDTO dimensionToGroupBy0 = new FunctionDTO(true, "getTrackedLegOfCompetitorContext().getCompetitor().getName()", HasGPSFixContext.class.getName(), String.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy0);

        FunctionDTO dimensionToGroupBy1 = new FunctionDTO(true, "getTrackedLegOfCompetitorContext().getTrackedLegContext().getLegType()", HasGPSFixContext.class.getName(), LegType.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy1);
        
        return queryDefinition;
    }
    
    private StatisticQueryDefinitionDTO create_SumTraveledDistance_Per_Competitor_LegType() {
        FunctionDTO statistic = new FunctionDTO(false, "getDistanceTraveled()", HasTrackedLegOfCompetitorContext.class.getName(), Distance.class.getName(), new ArrayList<String>(), "", 0);
        AggregationProcessorDefinitionDTO aggregator = new AggregationProcessorDefinitionDTO("Sum", Distance.class.getName(), Distance.class.getName(), "");

        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, LeaderboardGroupRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardGroupContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, LeaderboardRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(2, TrackedRaceRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedRaceContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(3, TrackedLegRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(4, TerminalTrackedLegOfCompetitorRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegOfCompetitorContext.class.getName(), ""), null));
        DataRetrieverChainDefinitionDTO retrieverChain = new DataRetrieverChainDefinitionDTO("", RacingEventService.class.getName(), retrieverLevels);

        ModifiableStatisticQueryDefinitionDTO queryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statistic, aggregator, retrieverChain);

        FunctionDTO dimensionToGroupBy0 = new FunctionDTO(true, "getCompetitor().getName()", HasTrackedLegOfCompetitorContext.class.getName(), String.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy0);

        FunctionDTO dimensionToGroupBy1 = new FunctionDTO(true, "getTrackedLegContext().getLegType()", HasTrackedLegOfCompetitorContext.class.getName(), LegType.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy1);
        
        return queryDefinition;
    }

    private StatisticQueryDefinitionDTO create_AvgSpeed_Per_Competitor() {
        FunctionDTO statistic = new FunctionDTO(false, "getGPSFix().getSpeed().getKnots()", HasGPSFixContext.class.getName(), double.class.getName(), new ArrayList<String>(), "", 0);
        AggregationProcessorDefinitionDTO aggregator = new AggregationProcessorDefinitionDTO("Average", Number.class.getName(), AverageWithStats.class.getName(), "");

        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, LeaderboardGroupRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardGroupContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, LeaderboardRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(2, TrackedRaceRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedRaceContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(3, TrackedLegRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(4, TerminalTrackedLegOfCompetitorRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegOfCompetitorContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(5, GPSFixRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasGPSFixContext.class.getName(), ""), null));
        DataRetrieverChainDefinitionDTO retrieverChain = new DataRetrieverChainDefinitionDTO("", RacingEventService.class.getName(), retrieverLevels);

        ModifiableStatisticQueryDefinitionDTO queryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statistic, aggregator, retrieverChain);

        FunctionDTO dimensionToGroupBy0 = new FunctionDTO(true, "getTrackedLegOfCompetitorContext().getCompetitor().getName()", HasGPSFixContext.class.getName(), String.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy0);
        
        return queryDefinition;
    }

    private StatisticQueryDefinitionDTO create_SumTraveledDistance_Per_Competitor() {
        FunctionDTO statistic = new FunctionDTO(false, "getDistanceTraveled()", HasTrackedLegOfCompetitorContext.class.getName(), Distance.class.getName(), new ArrayList<String>(), "", 0);
        AggregationProcessorDefinitionDTO aggregator = new AggregationProcessorDefinitionDTO("Sum", Distance.class.getName(), Distance.class.getName(), "");

        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, LeaderboardGroupRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardGroupContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, LeaderboardRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(2, TrackedRaceRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedRaceContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(3, TrackedLegRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(4, TerminalTrackedLegOfCompetitorRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedLegOfCompetitorContext.class.getName(), ""), null));
        DataRetrieverChainDefinitionDTO retrieverChain = new DataRetrieverChainDefinitionDTO("", RacingEventService.class.getName(), retrieverLevels);

        ModifiableStatisticQueryDefinitionDTO queryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statistic, aggregator, retrieverChain);

        FunctionDTO dimensionToGroupBy0 = new FunctionDTO(true, "getCompetitor().getName()", HasTrackedLegOfCompetitorContext.class.getName(), String.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy0);
        
        return queryDefinition;
    }

    private StatisticQueryDefinitionDTO create_SumManeuvers_Per_Competitor() {
        FunctionDTO statistic = new FunctionDTO(false, "getNumberOfManeuvers()", HasRaceOfCompetitorContext.class.getName(), int.class.getName(), new ArrayList<String>(), "", 0);
        AggregationProcessorDefinitionDTO aggregator = new AggregationProcessorDefinitionDTO("Sum", Number.class.getName(), Number.class.getName(), "");

        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>();
        retrieverLevels.add(new DataRetrieverLevelDTO(0, LeaderboardGroupRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardGroupContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(1, LeaderboardRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasLeaderboardContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(2, TrackedRaceRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasTrackedRaceContext.class.getName(), ""), null));
        retrieverLevels.add(new DataRetrieverLevelDTO(3, RaceOfCompetitorRetrievalProcessor.class.getName(), new LocalizedTypeDTO(HasRaceOfCompetitorContext.class.getName(), ""), null));
        DataRetrieverChainDefinitionDTO retrieverChain = new DataRetrieverChainDefinitionDTO("", RacingEventService.class.getName(), retrieverLevels);

        ModifiableStatisticQueryDefinitionDTO queryDefinition = new ModifiableStatisticQueryDefinitionDTO("default", statistic, aggregator, retrieverChain);

        FunctionDTO dimensionToGroupBy0 = new FunctionDTO(true, "getCompetitor().getName()", HasRaceOfCompetitorContext.class.getName(), String.class.getName(), new ArrayList<String>(), "", 0);
        queryDefinition.appendDimensionToGroupBy(dimensionToGroupBy0);
        
        return queryDefinition;
    }

    public Map<PredefinedQueryIdentifier, StatisticQueryDefinitionDTO> getQueries() {
        return predefinedQueries;
    }

}
