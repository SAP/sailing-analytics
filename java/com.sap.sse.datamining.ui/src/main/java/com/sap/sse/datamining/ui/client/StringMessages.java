package com.sap.sse.datamining.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;


/**
 * Defines the text strings for i18n that are used by the Datamining GWT bundle. 
 * 
 * @author Maximilian Groß (D064866)
 */
@DefaultLocale("en")
public interface StringMessages extends com.sap.sse.gwt.client.StringMessages {

    public static final StringMessages INSTANCE = GWT.create(StringMessages.class);

    String dataMiningSettings();
    String runPredefinedQuery();
    String viewQueryDefinition();
    String selectPredefinedQuery();
    String errorRunningDataMiningQuery();
    String predefinedQueryRunner();
    String useClassGetNameTooltip();
    String useClassGetName();
    String useStringLiteralsTooltip();
    String queryDefinitionViewer();
    String run();
    String queryNotValidBecause();
    String queryRunner();
    String runAQuery();
    String runningQuery();
    String queryResultsChartSubtitle(int retrievedDataAmount, double calculationTime);
    String dataMiningErrorMargins();
    String dataMiningResult();
    String developerOptions();
    String runAutomatically();
    String runAutomaticallyTooltip();
    String currentFilterSelection();
    String plainResultsPresenter();
    String multiResultsPresenter();
    String columnChart();
    String columnChartWithErrorBars();
    String errorFetchingComponentsChangedTimepoint(String message);
    String dataMiningComponentsHaveBeenUpdated();
    String dataMiningComponentsNeedReloadDialogMessage();
    String noDimensionToGroupBySelectedError();
    String noDataRetrieverChainDefinitonSelectedError();
    String noStatisticSelectedError();
    String noAggregatorSelectedError();
    String calculateThe();
    String statisticProvider();
    String groupingProvider();    
    String basedOn();
    String chooseDifferentDimensionTitle();
    String chooseDifferentDimensionMessage();
    String pleaseSelectADimension();
    String queryDefinitionProvider();
    String dataMiningRetrieval();
    String tabbedResultsPresenter();
    String useStringLiterals();
    String numberPairResultsPresenter();
    String copiedToClipboard();
    String csvExport();
    String search();
    String csvCopiedToClipboard();
    String filter();
    String filterShownDimensions();
    String selectDimensionsToFilterBy();
    String clear();
    String changesWillBeLost();
    String confirmQueryDefinitionChangeLoss();
    String discardChanges();
    String keepChanges();
    String anErrorOccurredWhileApplyingTheQuery();
    String queryBasedOnRetrieverChainCanNotBeApplied(String retrieverChainName);
    String statisticNotAvailable(String statisticName);
    String aggregatorNotAvailable(String aggregatorName);
    String filterDimensionsAreNotAvailable(String listedDimensions);
    String filterValuesOfDimensionAreNotAvailable(String dimensionName, String listedValues);
    String groupingDimensionsAreNotAvailable(String listedDimensions);
    String rememberDecisionCanBeChangedInSettings();
    String changeLossStrategy();
    String changeLossStrageyTooltip();
    String askChangeLossStrategyName();
    String askChangeLossStrategyTooltip();
    String discardChangesChangeLossStrategyName();
    String discardChangesChangeLossStrategyTooltip();
    String keepChangesChangeLossStrategyName();
    String keepChangesChangeLossStrategyTooltip();
    String applyingQuery();
    String applyingReport();
    String searchAvailableStatistics();
    String reloadComponents();
    String any();
    String and();
    String followingStatisticsAreNotSupportedByAggregatorWarning();
    String configureQueryParametersDialog();
    String parameterType(String typeName);
    String allValues();
    String listOfValues();
    String containsText();
    String endsWithText();
    String startsWithText();
    String itemsMatchingTextConstraint();
    String selectItemsAvailableForParameter();
    String apply();
    String noMatchingValuesOrNoFilter();
    String filterDimensionValues();
    String pickOrCreateReportParameter();
    String pickOrCreateReportParameterMessage();
    String parameterNamesMustBeUniqueInReport(String name, String typeName);
    String newParameterName();
    String parameterNameMustNotBeEmpty();
    String overwriteExistingReportBySameName(String name);
    String isItOkToReplaceAllTabsWithQueriesByReport();
    String runOtherChangedQueriesInBackground();
    String askUser();
    String beaufort();
    String kilometersPerHour();
    String knots();
    String metersPerSecond();
    String statuteMilesPerHour();
}
