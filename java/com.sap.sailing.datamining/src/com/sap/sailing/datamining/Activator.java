package com.sap.sailing.datamining;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;

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
import com.sap.sailing.datamining.impl.components.aggregators.ManeuverSpeedDetailsStatisticAvgAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ManeuverSpeedDetailsStatisticMedianAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelBearingAverageDegreesAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelBearingMaxAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelBearingMinAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelBoolSumAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDistanceAverageAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDistanceMaxAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDistanceMedianAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDistanceMinAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDistanceSumAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDurationAverageAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDurationMaxAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDurationMinAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelDurationSumAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelSpeedAverageAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelSpeedMaxAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelSpeedMedianAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelSpeedMinAggregationProcessor;
import com.sap.sailing.datamining.impl.components.aggregators.ParallelSpeedSumAggregationProcessor;
import com.sap.sailing.datamining.provider.RacingEventServiceProvider;
import com.sap.sse.datamining.DataSourceProvider;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.impl.AbstractDataMiningActivatorWithPredefinedQueries;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.PredefinedQueryIdentifier;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;

public class Activator extends AbstractDataMiningActivatorWithPredefinedQueries {

    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/Sailing_StringMessages";
    private static final SailingClusterGroups clusterGroups = new SailingClusterGroups();

    private static Activator INSTANCE;

    private BundleContext context = null;

    private final ResourceBundleStringMessages sailingServerStringMessages;
    private final SailingDataRetrievalChainDefinitions dataRetrieverChainDefinitions;
    private final SailingPredefinedQueries predefinedQueries;
    private Collection<DataSourceProvider<?>> dataSourceProviders;
    private boolean dataSourceProvidersHaveBeenInitialized;

    public Activator() {
        sailingServerStringMessages = new ResourceBundleStringMessagesImpl(STRING_MESSAGES_BASE_NAME, getClassLoader(),
                StandardCharsets.UTF_8.name());
        dataRetrieverChainDefinitions = new SailingDataRetrievalChainDefinitions();
        predefinedQueries = new SailingPredefinedQueries();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        INSTANCE = this;
        this.context = context;
        dataSourceProvidersHaveBeenInitialized = false;
        super.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        this.context = null;
        INSTANCE = null;
        super.stop(context);
    }

    @Override
    public ResourceBundleStringMessages getStringMessages() {
        return sailingServerStringMessages;
    }

    @Override
    public Iterable<Class<?>> getClassesWithMarkedMethods() {
        Set<Class<?>> internalClasses = new HashSet<>();
        internalClasses.add(HasTrackedRaceContext.class);
        internalClasses.add(HasRaceResultOfCompetitorContext.class);
        internalClasses.add(HasTrackedLegContext.class);
        internalClasses.add(HasTrackedLegOfCompetitorContext.class);
        internalClasses.add(HasTrackedLegSliceOfCompetitorContext.class);
        internalClasses.add(HasGPSFixContext.class);
        internalClasses.add(HasWindTrackContext.class);
        internalClasses.add(HasWindFixContext.class);
        internalClasses.add(HasBravoFixContext.class);
        internalClasses.add(HasBravoFixTrackContext.class);
        internalClasses.add(HasFoilingSegmentContext.class);
        internalClasses.add(HasTackTypeSegmentContext.class);
        internalClasses.add(HasManeuverContext.class);
        internalClasses.add(HasManeuverSpeedDetailsContext.class);
        internalClasses.add(HasCompleteManeuverCurveWithEstimationDataContext.class);
        internalClasses.add(HasMarkPassingContext.class);
        internalClasses.add(HasRaceOfCompetitorContext.class);
        internalClasses.add(HasCompetitorDayContext.class);
        internalClasses.add(HasLeaderboardGroupContext.class);
        internalClasses.add(HasLeaderboardContext.class);
        internalClasses.add(HasCompetitorContext.class);
        return internalClasses;
    }

    @Override
    public Iterable<DataRetrieverChainDefinition<?, ?>> getDataRetrieverChainDefinitions() {
        return dataRetrieverChainDefinitions.get();
    }

    @Override
    public Iterable<DataSourceProvider<?>> getDataSourceProviders() {
        if (!dataSourceProvidersHaveBeenInitialized) {
            initializeDataSourceProviders();
            dataSourceProvidersHaveBeenInitialized = true;
        }
        return dataSourceProviders;
    }

    @Override
    public Iterable<AggregationProcessorDefinition<?, ?>> getAggregationProcessorDefinitions() {
        HashSet<AggregationProcessorDefinition<?, ?>> aggregators = new HashSet<>();
        aggregators.add(ParallelBoolSumAggregationProcessor.getDefinition());
        aggregators.add(ParallelDistanceSumAggregationProcessor.getDefinition());
        aggregators.add(ParallelDistanceAverageAggregationProcessor.getDefinition());
        aggregators.add(ParallelDistanceMaxAggregationProcessor.getDefinition());
        aggregators.add(ParallelDistanceMinAggregationProcessor.getDefinition());
        aggregators.add(ParallelDistanceMedianAggregationProcessor.getDefinition());
        aggregators.add(ParallelSpeedSumAggregationProcessor.getDefinition());
        aggregators.add(ParallelSpeedAverageAggregationProcessor.getDefinition());
        aggregators.add(ParallelSpeedMaxAggregationProcessor.getDefinition());
        aggregators.add(ParallelSpeedMinAggregationProcessor.getDefinition());
        aggregators.add(ParallelSpeedMedianAggregationProcessor.getDefinition());
        aggregators.add(ParallelDurationSumAggregationProcessor.getDefinition());
        aggregators.add(ParallelDurationAverageAggregationProcessor.getDefinition());
        aggregators.add(ParallelDurationMaxAggregationProcessor.getDefinition());
        aggregators.add(ParallelDurationMinAggregationProcessor.getDefinition());
        aggregators.add(ParallelBearingAverageDegreesAggregationProcessor.getDefinition());
        aggregators.add(ParallelBearingMaxAggregationProcessor.getDefinition());
        aggregators.add(ParallelBearingMinAggregationProcessor.getDefinition());
        aggregators.add(ManeuverSpeedDetailsStatisticAvgAggregationProcessor.getDefinition());
        aggregators.add(ManeuverSpeedDetailsStatisticMedianAggregationProcessor.getDefinition());
        return aggregators;
    }

    @Override
    public Map<PredefinedQueryIdentifier, StatisticQueryDefinitionDTO> getPredefinedQueries() {
        return predefinedQueries.getQueries();
    }

    private void initializeDataSourceProviders() {
        dataSourceProviders = new HashSet<>();
        dataSourceProviders.add(new RacingEventServiceProvider(context));
    }

    public static SailingClusterGroups getClusterGroups() {
        return clusterGroups;
    }

    public static Activator getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new Activator(); // probably non-OSGi case, as in test execution
        }
        return INSTANCE;
    }

}
