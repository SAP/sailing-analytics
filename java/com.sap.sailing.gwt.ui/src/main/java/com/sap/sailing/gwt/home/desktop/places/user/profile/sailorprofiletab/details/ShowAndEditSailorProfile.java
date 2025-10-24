package com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.details;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.ParticipatedEventDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileEventsDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileNumericStatisticType;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileStatisticDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileStatisticDTO.SingleEntry;
import com.sap.sailing.gwt.home.desktop.partials.desktopaccordion.DesktopAccordion;
import com.sap.sailing.gwt.home.desktop.partials.desktopaccordion.DesktopAccordion.AccordionExpansionListener;
import com.sap.sailing.gwt.home.desktop.partials.desktopaccordion.DesktopAccordionResources;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailorProfileDesktopResources;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.details.events.SailorProfileEventsTable;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.details.statistic.SailorProfileStatisticTable;
import com.sap.sailing.gwt.home.shared.partials.editable.EditableSuggestedMultiSelectionCompetitor;
import com.sap.sailing.gwt.home.shared.partials.editable.InlineEditLabel;
import com.sap.sailing.gwt.home.shared.partials.listview.BoatClassListView;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.DataMiningQueryForSailorProfilesPersistor;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.EditSailorProfileDetailsView;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.SharedSailorProfileResources;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.settings.SailingSettingsConstants;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.security.ui.client.UserService;

import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.moxieapps.gwt.highcharts.client.plotOptions.LinePlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.*;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import java.util.Map; 
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import com.sap.sse.gwt.resources.Highcharts; 
import com.sap.sse.gwt.client.async.ParallelExecutionCallback;
import com.sap.sse.gwt.client.async.ParallelExecutionHolder;

/**
 * Implementation of {@link EditSailorProfileDetailsView} where users can view the details of a SailorProfile and edit
 * them. The data is loaded when the accordion is opened for the first time.
 */

public class ShowAndEditSailorProfile extends Composite implements EditSailorProfileDetailsView {
    
    private static SharedSailorProfileUiBinder uiBinder = GWT.create(SharedSailorProfileUiBinder.class);

    interface SharedSailorProfileUiBinder extends UiBinder<Widget, ShowAndEditSailorProfile> {
    }

    @UiField
    StringMessages i18n;

    @UiField(provided = true)
    EditableSuggestedMultiSelectionCompetitor competitorSelectionUi;
    @UiField
    InlineEditLabel titleUi;
    @UiField
    BoatClassListView boatClassesUi;
    @UiField
    DesktopAccordion accordionEventsUi;
    @UiField
    DesktopAccordion accordionStatisticsUi;
//    @UiField
//    DesktopAccordion accordionPolarDiagramUi;
    @UiField
    DesktopAccordion accordionAnalyticsUi;
    
//    @UiField
//    ListBox competitorDropdown_1;
//    UiField
//    ListBox competitorDropdown_2;
//    @UiField
//    Button updateComparisonButton;
    

    Label eventsEmptyUi;

    private final EditSailorProfileDetailsView.Presenter presenter;

    private FlagImageResolver flagImageResolver;
    
    private UserService userService;

    public ShowAndEditSailorProfile(EditSailorProfileDetailsView.Presenter presenter,
            FlagImageResolver flagImageResolver, SailorProfileDetailsView parent, UserService userService) {
        this.presenter = presenter;
        this.flagImageResolver = flagImageResolver;
        this.userService = userService;
        presenter.getDataProvider().setView(this);
        competitorSelectionUi = new EditableSuggestedMultiSelectionCompetitor(presenter.getCompetitorPresenter(),
                flagImageResolver);
        initWidget(uiBinder.createAndBindUi(this));
        SharedSailorProfileResources.INSTANCE.css().ensureInjected();
        SailorProfileDesktopResources.INSTANCE.css().ensureInjected();
        boatClassesUi.setText(i18n.boatClasses());
//        accordionPolarDiagramUi.setVisible(false);
    }

    private void setupTitleChangeListener(final UUID uuid) {
        // setup title change handler
        titleUi.clearTextChangeHandlers();
        titleUi.addTextChangeHandler((text) -> {
            presenter.getDataProvider().updateTitle(uuid, text);
        });
    }

    @Override
    public void setEntry(SailorProfileDTO entry) {
        competitorSelectionUi.setSelectedItems(entry.getCompetitors());
        titleUi.setText(entry.getName());
        boatClassesUi.setEmptyMessage(StringMessages.INSTANCE.pleaseSelectCompetitorFirst());
        boatClassesUi.setItems(entry.getBoatclasses());
        accordionEventsUi.clear();

        // Get events asynchronously
        presenter.getDataProvider().getEvents(entry.getKey(), new AsyncCallback<SailorProfileEventsDTO>() {
            @Override
            public void onFailure(Throwable caught) {
                GWT.log(caught.getMessage(), caught);
            }

            @Override
            public void onSuccess(SailorProfileEventsDTO result) {
                appendEmptyMessageIfNecessary(result);
                for (ParticipatedEventDTO dto : result.getParticipatedEvents()) {
                    SailorProfileEventsTable table = new SailorProfileEventsTable(flagImageResolver,
                            presenter.getPlaceController(), dto);
                    accordionEventsUi.addWidget(table);
                }
            }

            /** adds empty widget */
            private void appendEmptyMessageIfNecessary(SailorProfileEventsDTO result) {
                if (Util.isEmpty(result.getParticipatedEvents())) {
                    createEventsEmptyUiIfNecessary();
                    accordionEventsUi.addWidget(eventsEmptyUi);
                } else if (eventsEmptyUi != null && eventsEmptyUi.isAttached()) {
                    eventsEmptyUi.removeFromParent();
                }
            }

            /** creates empty widget */
            private void createEventsEmptyUiIfNecessary() {
                if (eventsEmptyUi == null) {
                    eventsEmptyUi = new Label(StringMessages.INSTANCE.noEventsFoundForCompetitors());
                    DesktopAccordionResources.INSTANCE.css().ensureInjected();
                    eventsEmptyUi.addStyleName(DesktopAccordionResources.INSTANCE.css().accordionEmptyMessage());
                }
            }
        });

        accordionStatisticsUi.clear();
        setupTables(entry);
        
        accordionAnalyticsUi.clear();
//        createPolarChartWithStatistics(entry, SailorProfileNumericStatisticType.AVERAGE_STARTLINE_DISTANCE); 
//        addSelectionToPolarChart(entry);
//        setupPolarChart(entry, SailorProfileNumericStatisticType.AVERAGE_STARTLINE_DISTANCE);
//        setupPolarChart(entry, SailorProfileNumericStatisticType.BEST_DISTANCE_TO_START);
//        setupPolarChart(entry, SailorProfileNumericStatisticType.BEST_STARTLINE_SPEED);
//        setupPolarChart(entry, SailorProfileNumericStatisticType.MAX_SPEED);
        setupTitleChangeListener(entry.getKey());
        
//        loadAllStatisticsInParallel(entry);
        createGenericPerformanceRadarChart(entry);
    }
   
    /** generic performance radar chart automatically generated for all sailorprofilenumericstatistictype enum values */
    private void createGenericPerformanceRadarChart(SailorProfileDTO entry) {
        List<ParallelExecutionCallback<SailorProfileStatisticDTO>> callbacks = new ArrayList<>();
        Map<SailorProfileNumericStatisticType, ParallelExecutionCallback<SailorProfileStatisticDTO>> callbackMap = new HashMap<>();
        List<SailorProfileNumericStatisticType> radarTypes = getRadarChartTypes();
        
        // Automatically create callbacks for all enum values
        for (SailorProfileNumericStatisticType type : radarTypes) {
            ParallelExecutionCallback<SailorProfileStatisticDTO> callback = new ParallelExecutionCallback<>(); // Create Callback 
            callbacks.add(callback); // Holder
            callbackMap.put(type, callback); // Value assignment
        }
        
        // new ParallelExecutionHolder(callbacks.toArray(new ParallelExecutionCallback<?>[0])) {
        // new ParallelExecutionHolder(callbackMap.values().toArray(new ParallelExecutionCallback<?>[0])) {
        ParallelExecutionHolder holder = new ParallelExecutionHolder(callbacks.toArray(new ParallelExecutionCallback<?>[0])) {
            @Override
            protected void handleSuccess() {
                // If all Callbacks were successfull 
                Map<SailorProfileNumericStatisticType, SailorProfileStatisticDTO> allStatistics = new HashMap<>();
                for (SailorProfileNumericStatisticType type : radarTypes) {
                    allStatistics.put(type, callbackMap.get(type).getData());
                }
                // Create Radar Chart 
                createDynamicPerformanceRadarChart(allStatistics, entry);
            }
            
            @Override
            protected void handleFailure(Throwable t) {
                // If at least one Callback fails
//               Label errorLabel = new Label("Error loading performance data: " + t.getMessage());
//                errorLabel.getElement().getStyle().setColor("red");
//                accordionAnalyticsUi.addWidget(errorLabel);
            }
        };
        
        // Start all server calls in parallel`
        for (SailorProfileNumericStatisticType type : radarTypes) {
            presenter.getDataProvider().getStatisticFor(entry.getKey(), type, callbackMap.get(type));
        }
    }
      // currently not checking if values meet the boolean isShownInRadarChart criteria
//    private void createGenericPerformanceRadarChart(SailorProfileDTO entry) {
//        // NUR eine Datenstruktur!
//        Map<SailorProfileNumericStatisticType, ParallelExecutionCallback<SailorProfileStatisticDTO>> callbackMap = new HashMap<>();
//        
//        // Callbacks nur in Map erstellen
//        for (SailorProfileNumericStatisticType type : SailorProfileNumericStatisticType.values()) {
//            callbackMap.put(type, new ParallelExecutionCallback<>());
//        }
//        
//        // Holder direkt aus Map erstellen
//        new ParallelExecutionHolder(callbackMap.values().toArray(new ParallelExecutionCallback<?>[0])) {
//            @Override
//            protected void handleSuccess() {
//                // Rest bleibt gleich...
//            }
//        };
//        
//        // Server-Calls starten
//        for (SailorProfileNumericStatisticType type : SailorProfileNumericStatisticType.values()) {
//            presenter.getDataProvider().getStatisticFor(entry.getKey(), type, callbackMap.get(type));
//        }
//    }
    
    /** Creates Team vs Field Performance Radar Chart (only 2 lines instead of individual competitors) */
    private void createDynamicPerformanceRadarChart(Map<SailorProfileNumericStatisticType, SailorProfileStatisticDTO> allStatistics, SailorProfileDTO entry) {
        try {
            Highcharts.ensureInjectedWithMore();
            
            // Dynamic categories based on available enum values
            List<SailorProfileNumericStatisticType> radarTypes = getRadarChartTypes();
            String[] categories = radarTypes.stream()
                .map(this::getDisplayName)
                .toArray(String[]::new);
            
            String teamName = (entry.getName() != null && !entry.getName().trim().isEmpty()) 
                    ? entry.getName() 
                    : "Performance";
            
            Chart radarChart = new Chart()
                .setType(Series.Type.LINE)
                .setPolar(true)
                .setHeight100()
                .setWidth100()
                .setChartTitle(new ChartTitle().setText("Team: \"" + teamName + "\" vs Field Performance"))
                .setChartSubtitle(new ChartSubtitle().setText("Team Average vs Field Average (100% = Field Average)"));
         
            // Radar chart configuration
            radarChart.getXAxis()
                .setCategories(categories)
                .setOption("tickmarkPlacement", "on")
                .setLineWidth(0);
            
            radarChart.getYAxis()
                .setOption("gridLineInterpolation", "polygon")
                .setLineWidth(0)
                .setMin(0)
                .setMax(200); // 200% = twice as good as average
                
            radarChart.setLegend(new Legend().setEnabled(true));
            radarChart.setExporting(new Exporting().setEnabled(false));

            // Calculate team vs field performance percentages
            Map<String, double[]> performancePercentages = calculateTeamVsFieldPerformancePercentages(allStatistics, entry);
            
            // Team-Series (Performance in %)
            Series teamSeries = radarChart.createSeries()
                .setName(entry.getName() != null && !entry.getName().trim().isEmpty() ? entry.getName() : "Team Performance")
                .setOption("color", "#FF6B6B")
                .setOption("pointPlacement", "on");
            
            for (double percentage : performancePercentages.get("team")) {
                teamSeries.addPoint(percentage);
            }
            radarChart.addSeries(teamSeries);
            
            // 100% Baseline (Field Average)
            Series baselineSeries = radarChart.createSeries()
                .setName("Field Average (100%)")
                .setOption("color", "#999999")
                .setOption("dashStyle", "dash")
                .setOption("pointPlacement", "on");

            // Set all values to 100%
            for (int i = 0; i < categories.length; i++) {
                baselineSeries.addPoint(100.0);
            }
            radarChart.addSeries(baselineSeries);
            
            // Add chart to UI
            SimpleLayoutPanel chartPanel = new SimpleLayoutPanel();
            chartPanel.add(radarChart);
            chartPanel.setHeight("600px");
            accordionAnalyticsUi.addWidget(chartPanel);
            
        } catch (Exception e) {
            Label errorLabel = new Label("Error creating team vs field radar chart: " + e.getMessage());
            errorLabel.getElement().getStyle().setColor("red");
            accordionAnalyticsUi.addWidget(errorLabel);
            GWT.log("Radar chart error: ", e);
        }
    }

    /** Converts enum names to user-friendly display names */  
    private String getDisplayName(SailorProfileNumericStatisticType type) {
        switch (type) {
            case MAX_SPEED: return "Max Speed";
            case BEST_DISTANCE_TO_START: return "Best Start Distance";
            case BEST_STARTLINE_SPEED: return "Best Start Speed";
            case AVERAGE_STARTLINE_DISTANCE: return "Avg. Start Distance";
            case AVERAGE_STARTLINE_DISTANCE_WITH_VALIDATION: return "Avg. Start Distance with Validation";
            case AVERAGE_VELOCITY_MADE_GOOD_UPWIND_LEG: return "Avg. VMG Upwind Leg";
            case AVERAGE_VELOCITY_MADE_GOOD_DOWNWIND_LEG: return "Avg. VMG Downwind Leg";
            case AVERAGE_SPEED_OVER_GROUND_UPWIND_LEG: return "Avg. SOG Upwind Leg";
            case AVERAGE_SPEED_OVER_GROUND_DOWNWIND_LEG: return "Avg. SOG Downwind Leg";
            default:
                return Arrays.stream(type.name().split("_"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .reduce((a, b) -> a + " " + b)
                    .orElse(type.name());
        }
    }

    /** Calculates team average vs field average as percentage values (100% = Field Average) (automatic value inversion) */
    private Map<String, double[]> calculateTeamVsFieldPerformancePercentages(Map<SailorProfileNumericStatisticType, SailorProfileStatisticDTO> allStatistics, SailorProfileDTO entry) {
//        SailorProfileNumericStatisticType[] types = SailorProfileNumericStatisticType.values(); 
        List<SailorProfileNumericStatisticType> radarTypes = getRadarChartTypes();
        double[] teamPercentages = new double[radarTypes.size()]; 
        double[] fieldPercentages = new double[radarTypes.size()]; 
        
        for (int i = 0; i < radarTypes.size(); i++) {
//            SailorProfileNumericStatisticType type = types[i];
            SailorProfileNumericStatisticType type = radarTypes.get(i); 
            SailorProfileStatisticDTO stat = allStatistics.get(type);
            
            if (stat != null && !stat.getResult().isEmpty()) {
                
                // Calculate team average 
                double teamSum = 0.0;
                int teamCount = 0;
                for (SimpleCompetitorWithIdDTO competitor : entry.getCompetitors()) {
                    if (stat.getResult().containsKey(competitor) && !stat.getResult().get(competitor).isEmpty()) {
                        teamSum += stat.getResult().get(competitor).get(0).getValue();
                        teamCount++;
                    }
                }
                double teamAvg = teamCount > 0 ? teamSum / teamCount : 0.0;

                // Calculate field average 
                double fieldSum = 0.0;
                int fieldCount = 0;
                for (SimpleCompetitorWithIdDTO competitor : stat.getAggregateForOtherCompetitors().keySet()) {
                    if (!stat.getAggregateForOtherCompetitors().get(competitor).isEmpty()) {
                        fieldSum += stat.getAggregateForOtherCompetitors().get(competitor).get(0).getValue();
                        fieldCount++;
                    }
                }
                double fieldAvg = fieldCount > 0 ? fieldSum / fieldCount : 0.0;

                // ========================== DEBUG ========================== // 
                // Log all team values individually              GWT.log("=== TEAM VALUES FOR " + type.name() + " ===");
//                for (SimpleCompetitorWithIdDTO competitor : entry.getCompetitors()) {
//                    if (stat.getResult().containsKey(competitor)) {
//                        Double value = stat.getResult().get(competitor).get(0).getValue();
//                        GWT.log("Team Competitor " + competitor.getName() + ": " + value + "m");
//                    }
//                }
//                GWT.log("Team Average: " + teamAvg + "m (calculated from " + teamCount + " competitors)");
//
//                // Log field values individually                GWT.log("=== FIELD VALUES FOR " + type.name() + " ===");
//                for (SimpleCompetitorWithIdDTO competitor : stat.getAggregateForOtherCompetitors().keySet()) {
//                    Double value = stat.getAggregateForOtherCompetitors().get(competitor).get(0).getValue();
//                    GWT.log("Field Average for " + competitor.getName() + ": " + value + "m");
//                }
//                
//                GWT.log("=== " + type.name() + " ===");
//                GWT.log("Team Avg: " + teamAvg);
//                GWT.log("Field Avg: " + fieldAvg);
//                GWT.log("isLowerIsBetter: " + type.isLowerIsBetter());
                // ========================== DEBUG ========================== // 

                // Calculate team performance as percentage (like with individual competitors)
                if (fieldAvg != 0.0) {
                    if (type.isLowerIsBetter()) {
                        // Lower is better: fieldAvg / teamAvg * 100
                        teamPercentages[i] = (fieldAvg / teamAvg) * 100.0;
                    } else {
                        // Higher is better: teamAvg / fieldAvg * 100
                        teamPercentages[i] = (teamAvg / fieldAvg) * 100.0;
                    }
                } else {
                    teamPercentages[i] = 100.0; // Fallback
                }
                
                
                // Log result
                GWT.log("Result: " + teamPercentages[i] + " %");
                GWT.log("========================");
                

                // Field Average is always 100%
                fieldPercentages[i] = 100.0;
                
            } else {
                teamPercentages[i] = 100.0;
                fieldPercentages[i] = 100.0;
            }
        }
        
        Map<String, double[]> result = new HashMap<>();
        result.put("team", teamPercentages);
        result.put("field", fieldPercentages);
        return result;
    }
    
    /** Returns all SailorProfileNumericStatisticType values that should be shown in radar chart */
    private List<SailorProfileNumericStatisticType> getRadarChartTypes() {
        List<SailorProfileNumericStatisticType> radarTypes = new ArrayList<>();
        for (SailorProfileNumericStatisticType type : SailorProfileNumericStatisticType.values()) {
            if (type.isShowInRadarChart()) {
                radarTypes.add(type);
            }
        }
        return radarTypes;
    }
    
    /** create tables for statistic types */
    private void setupTables(SailorProfileDTO entry) {
        DataMiningQueryForSailorProfilesPersistor.removeDMQueriesFromLocalStorage(userService.getStorage());
        
        for (SailorProfileNumericStatisticType type : SailorProfileNumericStatisticType.values()) {
            if (type.isShowInStatisticsTables()) { // filter which metrics are displayed
                SailorProfileStatisticTable table = new SailorProfileStatisticTable(flagImageResolver, type, i18n,
                        userService);
                accordionStatisticsUi.addWidget(table);
                if (accordionStatisticsUi.isExpanded()) {
                    updateStatistic(entry, type, table);
                } else {
                    // load the statistic data when the accordion is expanded for the first time
                    accordionStatisticsUi.addAccordionListener(new AccordionExpansionListener() {
                        @Override
                        public void onExpansion(boolean expanded) {
                            if (expanded) {
                                updateStatistic(entry, type, table);
                            }
                        }
    
                    });
                }
            }
        }
    }

    /** retrieve and update the statistic for a given type of a sailor profile entry */
    private void updateStatistic(SailorProfileDTO entry, SailorProfileNumericStatisticType type,
            SailorProfileStatisticTable table) {
        presenter.getDataProvider().getStatisticFor(entry.getKey(), type,
                new AsyncCallback<SailorProfileStatisticDTO>() {

                    private String navigationUrl;

                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(i18n.couldNotDetermineStatistic(), NotificationType.WARNING);
                    }

                    @Override
                    public void onSuccess(SailorProfileStatisticDTO answer) {
                        ArrayList<Pair<SimpleCompetitorWithIdDTO, SingleEntry>> data = new ArrayList<>();
                        for (Entry<SimpleCompetitorWithIdDTO, ArrayList<SingleEntry>> entry : answer.getResult()
                                .entrySet()) {
                            for (SingleEntry value : entry.getValue()) {
                                data.add(new Pair<SimpleCompetitorWithIdDTO, SingleEntry>(entry.getKey(), value));
                            }
                        }
                        if (answer.getDataMiningQuery() != null) {
                            final String identifier = SailingSettingsConstants.DATAMINING_QUERY_PREFIX
                                    + UUID.randomUUID().toString();
                            navigationUrl = "DataMining.html?q=" + identifier;
                            table.setNavigationTarget(this::func);
                            DataMiningQueryForSailorProfilesPersistor.writeDMQueriesToLocalStorageIfPossible(answer,
                                    identifier, userService.getStorage());
                        }
                        table.setData(data);
                    }

                    String func(Object o) {
                        return navigationUrl;
                    }
                });
    }
}    
//    /** Sets up polar chart with lazy loading when accordion is expanded */
//    private void setupPolarChart(SailorProfileDTO entry, SailorProfileNumericStatisticType type) {
//        if(accordionAnalyticsUi.isExpanded()) { // Create PolarChart with factory here?
//            createPolarChartWithStatistics(entry, type);
//        }
//        else {
//            // load the statistic data when the accordion is expanded for the first time
//            accordionAnalyticsUi.addAccordionListener(new AccordionExpansionListener() {
//                @Override
//                public void onExpansion(boolean expanded) {
//                    if (expanded) {
//                        createPolarChartWithStatistics(entry, type);
//                    }
//                }
//            });
//        }
//    }
//    
//    /** Retrieves data for polar chart visualization */
//    private void createPolarChartWithStatistics(SailorProfileDTO entry, SailorProfileNumericStatisticType type) { // Pass a PolarChart here?
//            presenter.getDataProvider().getStatisticFor(entry.getKey(), type, 
//                    new AsyncCallback<SailorProfileStatisticDTO>() { 
//   
//                        @Override
//                        public void onSuccess(SailorProfileStatisticDTO answer) {
//                            handleDataAndCreatePolarChart(answer);
//                        }
//                        
//                        @Override
//                        public void onFailure(Throwable caught) {
//                            Notification.notify(i18n.couldNotDetermineStatistic(), NotificationType.WARNING);
//                            GWT.log("Error loading data: ", caught);
//                        }
//            });
//    }
//    
//    /** Processes statistic data and converts to chart points */
//    private void handleDataAndCreatePolarChart(SailorProfileStatisticDTO statisticData) {
//            // Null check
//            if (statisticData == null || statisticData.getResult().isEmpty()) {
//                Label noDataLabel = new Label("No data available for polar chart");
//                accordionAnalyticsUi.addWidget (noDataLabel);
//                return;
//            }
//        
//            ArrayList<Point> chartPoints = new ArrayList<>();
//
////            GWT.log("Result size: " + statisticData.getResult().size());
//                            
//            for (Entry<SimpleCompetitorWithIdDTO, ArrayList<SingleEntry>> entry : statisticData.getResult().entrySet()) {
//                SimpleCompetitorWithIdDTO competitor = entry.getKey();
//                ArrayList<SingleEntry> values = entry.getValue();
//                
////                // Log all competitors
////                GWT.log("Competitor: " + (competitor != null ? competitor.getName() : "NULL") + "\n--->Values: " + (values != null ? values.size() : "NULL"));     
////                if (values != null) { // test
////                    for (SingleEntry singleEntry : values) { 
////                        Double value = singleEntry.getValue(); 
////                        GWT.log("--->Value: " + value); 
////                    }
////                }
//                    
//                // Iterate through the values for each competitor
//                for (SingleEntry singleEntry : values) {
//                    Double value = singleEntry.getValue(); // Y-Value
//                    
//                    /** Here you need to decide what the X-value should be
//                        For a polar chart, for example, you could:
//                        - Use the index as the angle
//                        - Convert the timestamp into an angle */
//                    
//                    /** Example: Use index as angle (distributed from 0–360 degrees)
//                        int angleIndex = chartPoints.size();
//                        double angle = (angleIndex * 360.0) / Math.max(values.size(), 1) - 180; // range -180 to +180
//                        chartPoints.add(new Point(angle, value));
//                     */
//                    
//                    // refactor?
//                    int angleIndex = chartPoints.size();
//                    double angle = angleIndex * 90; // 0°, 90°, 180°, 270°
//                    if (angle > 180) angle -= 360; 
//
//                    chartPoints.add(new Point(angle, value));
//                    GWT.log("--->Added data point: " + angle + "°," + value + "m (" + competitor.getName() + ") \nTotal chart points: " + chartPoints.size()); 
//                }
//            }  
//            polarChartFactory(statisticData, chartPoints, "Sailing Performance Chart", "Average Startline Distance", "Distance Values"); // refactor this part?
//    }
//    
//    private void polarChartFactory(SailorProfileStatisticDTO statisticData, ArrayList<Point> chartPoints, String chartTitle, String chartSubtitle, String seriesName) {      
//        try {
//            // Highcharts-More for PolarChart
//            Highcharts.ensureInjectedWithMore();
//            
//            LinePlotOptions linePlotOptions = new LinePlotOptions()
//                .setLineWidth(1)
//                .setMarker(new Marker().setEnabled(true)); // originally false
//            
//            Chart polarChart = new Chart()
//                .setType(Series.Type.LINE)
//                .setLinePlotOptions(linePlotOptions)
//                .setPolar(true) 
//                .setHeight100()
//                .setWidth100()
//                .setTitle(new ChartTitle().setText(chartTitle), 
//                          new ChartSubtitle().setText(chartSubtitle));
//            polarChart.getXAxis().setMin(-179).setMax(180).setTickInterval(45);
////             lineChart.getXAxis().setMin(-180).setMax(180).setTickInterval(45);
//            polarChart.setOption("/pane/startAngle", 180); // unnecessary?
//            polarChart.setExporting(new Exporting().setEnabled(false));
//      
//            Series series = polarChart.createSeries().setName(seriesName);
//            
//            // Enable legend (shows sailor names) //working?
//            polarChart.setLegend(new Legend().setEnabled(true));
//                       
//            for (Point point : chartPoints) {
//                series.addPoint(point);
//            }
//            polarChart.addSeries(series);
//            
//            // Add Chart to Panel
//            SimpleLayoutPanel chartPanel = new SimpleLayoutPanel();
//            chartPanel.add(polarChart);
//            chartPanel.setHeight("300px");
//            
//            // Add Chart to Accordion
//            accordionAnalyticsUi.addWidget(chartPanel);
//            
//            // Success-Label
////            Label successLabel = new Label("Chart created successfull!");
////            accordionAnalyticsUi.addWidget(successLabel);
//        }
//        // Error-Label
//        catch (Exception e) {
//            Label errorLabel = new Label("Error: " + e.getMessage());
//            accordionAnalyticsUi.addWidget(errorLabel);
//            GWT.log("Chart error: ", e);
//        }
//    }
//    
//    private void addSelectionToPolarChart(SailorProfileDTO entry) {                 
//        VerticalPanel VerticalPanelCompetitorSelection = new VerticalPanel();
//        CaptionPanel CaptionPanelCompetitorSelection = new CaptionPanel("Compare the following sailors: ");
//        VerticalPanelCompetitorSelection.add(CaptionPanelCompetitorSelection);
//        
//        VerticalPanelCompetitorSelection.setWidth("100%");
//        
//        Grid GridCompetitorSelection = new Grid(2, 3);
//        
//        GridCompetitorSelection.setText(0, 0, "Competitor A");
//        GridCompetitorSelection.setText(0, 1, "Competitor B");
//        GridCompetitorSelection.setText(0, 2, "Compare"); 
//        
//        ListBox competitorDropdown_1 = new ListBox();
//        ListBox competitorDropdown_2 = new ListBox();
//        Button updateComparisonButton = new Button("Update");
//                
//        // Fill both dropdowns: first a placeholder, then all competitors 
//        competitorDropdown_1.addItem("----Selection----");
//        competitorDropdown_2.addItem("----Selection----");
//
//        for(SimpleCompetitorWithIdDTO competitor : entry.getCompetitors()) {
//            String teamName = competitor.getName();
//            String teamId = competitor.getIdAsString();
//            competitorDropdown_1.addItem (teamName, teamId);
//            competitorDropdown_2.addItem (teamName, teamId);
//        }
//        
//        competitorDropdown_2.addItem("----Average----", "AVERAGE");
//        competitorDropdown_2.addItem ("----Median----", "MEDIAN");
//        competitorDropdown_2.addItem("----Top 10%----", "TOP_10_PERCENT");
//        
//        // updateComparisonButton-Handler
//        updateComparisonButton.addClickHandler(event -> {
//            handleComparisonBtnUpdate(competitorDropdown_1, competitorDropdown_2, entry);
//        });
//
//        GridCompetitorSelection.setWidget(1, 0, competitorDropdown_1);
//        GridCompetitorSelection.setWidget(1, 1, competitorDropdown_2);
//        GridCompetitorSelection.setWidget(1, 2, updateComparisonButton);
//        
//        GridCompetitorSelection.setWidth("100%");
//        GridCompetitorSelection.getColumnFormatter().setWidth(0, "40%");
//        GridCompetitorSelection.getColumnFormatter().setWidth(1, "40%");
//        GridCompetitorSelection.getColumnFormatter().setWidth(2, "20%");
//        
//        competitorDropdown_1.setWidth("90%");
//        competitorDropdown_2.setWidth("90%");
//        updateComparisonButton.setWidth("100%");
//
//        CaptionPanelCompetitorSelection.add(GridCompetitorSelection);
//        VerticalPanelCompetitorSelection.add(CaptionPanelCompetitorSelection);
//        VerticalPanelCompetitorSelection.add(new HTML("<br/>"));
//        accordionAnalyticsUi.addWidget(VerticalPanelCompetitorSelection);
//       
//    }
//    
//    private void handleComparisonBtnUpdate(ListBox dropdown_1, ListBox dropdown_2, SailorProfileDTO entry) {
//        // Read selection from Dropdowns 
//        int selectedIndex1 = dropdown_1.getSelectedIndex();
//        int selectedIndex2 = dropdown_2.getSelectedIndex();
//        
//        // Proof selection is legit
//        if (selectedIndex1 <= 0 || selectedIndex2 <= 0) {
//            // No valid selection
//            Notification.notify("Please select both competitors", NotificationType.WARNING);
//            return;
//        }
//        
//        // Read selected values
//        String competitor1Id = dropdown_1.getSelectedValue();  
//        String competitor1Name = dropdown_1.getSelectedItemText(); 
//        
//        String competitor2Id = dropdown_2.getSelectedValue();  
//        String competitor2Name = dropdown_2.getSelectedItemText(); 
//        
//        
//        GWT.log("Selected: " + competitor1Id + " vs. " + competitor2Id +"\nValues: " + competitor1Name + " vs. " + competitor2Name);
//        
//        // remove previous charts?
//        
//        createComparisonChart(competitor1Id, competitor2Id, competitor1Name, competitor2Name, entry);
//    }
//    
//    /**
//     * Creates a comparison chart based on the user's selection
//     * Supports: sailor vs sailor, sailor vs average, sailor vs median, sailor vs top 10%.
//     */
//    private void createComparisonChart(String competitor1Id, String competitor2Id, 
//                                      String competitor1Name, String competitor2Name, 
//                                      SailorProfileDTO entry) {
//        
//        // Load data from server
//        presenter.getDataProvider().getStatisticFor(entry.getKey(), 
//            SailorProfileNumericStatisticType.AVERAGE_STARTLINE_DISTANCE, 
//            new AsyncCallback<SailorProfileStatisticDTO>() {
//                
//                @Override
//                public void onSuccess(SailorProfileStatisticDTO statisticData) {
//                    
//                    // Collect data for Competitor 1 (always a single sailor)
//                    ArrayList<Point> competitor1Points = new ArrayList<>();
//                    
//                    // Iterate over all sailor data
//                    for (Entry<SimpleCompetitorWithIdDTO, ArrayList<SingleEntry>> entry : statisticData.getResult().entrySet()) {
//                        SimpleCompetitorWithIdDTO competitor = entry.getKey();
//                        
//                        // Process only the selected sailor
//                        if (competitor.getIdAsString().equals(competitor1Id)) {
//                            ArrayList<SingleEntry> values = entry.getValue();
//                            
//                            // Create data points for the polar chart
//                            for (int i = 0; i < values.size(); i++) {
//                                // Distribute angles evenly across 360°
//                                double angle = (i * 360.0 / values.size());
//                                if (angle > 180) angle -= 360; // Convert to range [-180, +180]
//                                
//                                Double value = values.get(i).getValue();
//                                competitor1Points.add(new Point(angle, value));
//                            }
//                            break; // Sailor found; break the loop
//                        }
//                    }
//                    
//                    // Collect data for Competitor 2 (sailor or statistic) 
//                    ArrayList<Point> competitor2Points = new ArrayList<>();
//                    
//                    // Check if Competitor 2 is a statistic
//                    if (competitor2Id.equals("AVERAGE")) {
//                        // Compute Average
//                        competitor2Points = calculateAveragePoints(statisticData);
//                        
//                    } else if (competitor2Id.equals("MEDIAN")) {
//                        // Compute Median
//                        competitor2Points = calculateMedianPoints(statisticData);
//                        
//                    } else if (competitor2Id.equals("TOP_10_PERCENT")) {
//                        // Compute top 10%
//                        competitor2Points = calculateTop10PercentPoints(statisticData);
//                        
//                    } else {
//                        // Regular sailor — same logic as for Competitor 1
//                        for (Entry<SimpleCompetitorWithIdDTO, ArrayList<SingleEntry>> entry : statisticData.getResult().entrySet()) {
//                            SimpleCompetitorWithIdDTO competitor = entry.getKey();
//                            
//                            if (competitor.getIdAsString().equals(competitor2Id)) {
//                                ArrayList<SingleEntry> values = entry.getValue();
//                                
//                                for (int i = 0; i < values.size(); i++) {
//                                    double angle = (i * 360.0 / values.size());
//                                    if (angle > 180) angle -= 360;
//                                    
//                                    Double value = values.get(i).getValue();
//                                    competitor2Points.add(new Point(angle, value));
//                                }
//                                break;
//                            }
//                        }
//                    }
//                    
//                    // Create the polar chart
//                    try {
//                        // Load Highcharts for the polar chart
//                        Highcharts.ensureInjectedWithMore();
//                        
//                        LinePlotOptions linePlotOptions = new LinePlotOptions()
//                                .setLineWidth(1)
//                                .setMarker(new Marker().setEnabled(true)); // originally false
//                        
//                        Chart polarChart = new Chart()
//                            .setType(Series.Type.LINE)
//                            .setLinePlotOptions(linePlotOptions)
//                            .setPolar(true)  
//                            .setHeight100()
//                            .setWidth100()
//                            .setTitle(new ChartTitle().setText("Sailor Comparison: "), 
//                                      new ChartSubtitle().setText(competitor1Name + " vs. " + competitor2Name));
//                        
//                        polarChart.getXAxis().setMin(-179).setMax(180).setTickInterval(45); // Angle axis
////                        polarChart.getYAxis().setMin(0); // Distanz-Achse 
//                        polarChart.setOption("/pane/startAngle", 180); // unnecessary?
//                        polarChart.setExporting(new Exporting().setEnabled(false)); 
//                        
//                        // Add first series (Competitor 1)
//                        Series series_1 = polarChart.createSeries()
//                            .setName(competitor1Name)
//                            .setOption("color", "#FF6B6B"); // red
//                        
//                        // Add all points to the first series
//                        for (Point point : competitor1Points) {
//                            series_1.addPoint(point);
//                        }
//                        polarChart.addSeries(series_1);
//                        
//                        // Add second series (Competitor 2)
//                        Series series_2 = polarChart.createSeries()
//                            .setName(competitor2Name)
//                            .setOption("color", "#4ECDC4"); // turquoise
//                        
//                        // Add all points to the second series
//                        for (Point point : competitor2Points) {
//                            series_2.addPoint(point);
//                        }
//                        polarChart.addSeries(series_2);
//                        
//                        // Enable legend (shows sailor names)
//                        polarChart.setLegend(new Legend().setEnabled(true));
//                        
//                        SimpleLayoutPanel chartPanel = new SimpleLayoutPanel();
//                        chartPanel.add(polarChart);
//                        chartPanel.setHeight("300px");
//                        accordionAnalyticsUi.addWidget(chartPanel);
//                        
//                    } 
//                    // Error-Label
//                    catch (Exception e) {
//                        Label errorLabel = new Label("Error: " + e.getMessage());
//                        accordionAnalyticsUi.addWidget(errorLabel);
//                        GWT.log("Chart error: ", e);
//                    }
//                }
//                
//                @Override
//                public void onFailure(Throwable caught) {
//                    Notification.notify(i18n.couldNotDetermineStatistic(), NotificationType.WARNING);
//                    GWT.log("Error loading data: ", caught);
//                }
//            });
//    }
//
//    /**
//     * Computes average values for all angles.
//     */
//    private ArrayList<Point> calculateAveragePoints(SailorProfileStatisticDTO statisticData) {
//        // Collect all values by position
//        Map<Integer, List<Double>> valuesByPosition = new HashMap<>();
//        
//        // Iterate over all sailors
//        for (Entry<SimpleCompetitorWithIdDTO, ArrayList<SingleEntry>> entry : statisticData.getResult().entrySet()) {
//            ArrayList<SingleEntry> values = entry.getValue();
//            
//            // Process each angle position
//            for (int i = 0; i < values.size(); i++) {
//                if (!valuesByPosition.containsKey(i)) {
//                    valuesByPosition.put(i, new ArrayList<>());
//                }
//                valuesByPosition.get(i).add(values.get(i).getValue());
//            }
//        }
//        
//        // Compute average per position
//        ArrayList<Point> averagePoints = new ArrayList<>();
//        for (int position = 0; position < valuesByPosition.size(); position++) {
//            // Compute angle
//            double angle = (position * 360.0 / valuesByPosition.size());
//            if (angle > 180) angle -= 360;
//            
//            // Average of all values at this position
//            double average = valuesByPosition.get(position).stream()
//                .mapToDouble(Double::doubleValue)
//                .average()
//                .orElse(0.0);
//            
//            averagePoints.add(new Point(angle, average));
//        }
//        
//        return averagePoints;
//    }
//
//    /**
//     * Computes median values for all angles.
//     */
//    private ArrayList<Point> calculateMedianPoints(SailorProfileStatisticDTO statisticData) {
//        Map<Integer, List<Double>> valuesByPosition = new HashMap<>();
//        
//        for (Entry<SimpleCompetitorWithIdDTO, ArrayList<SingleEntry>> entry : statisticData.getResult().entrySet()) {
//            ArrayList<SingleEntry> values = entry.getValue();
//            for (int i = 0; i < values.size(); i++) {
//                if (!valuesByPosition.containsKey(i)) {
//                    valuesByPosition.put(i, new ArrayList<>());
//                }
//                valuesByPosition.get(i).add(values.get(i).getValue());
//            }
//        }
//        
//        ArrayList<Point> medianPoints = new ArrayList<>();
//        for (int position = 0; position < valuesByPosition.size(); position++) {
//            double angle = (position * 360.0 / valuesByPosition.size());
//            if (angle > 180) angle -= 360;
//            
//            // Compute median
//            List<Double> values = valuesByPosition.get(position);
//            Collections.sort(values);
//            double median;
//            if (values.size() % 2 == 0) {
//                median = (values.get(values.size()/2 - 1) + values.get(values.size()/2)) / 2.0;
//            } else {
//                median = values.get(values.size()/2);
//            }
//            
//            medianPoints.add(new Point(angle, median));
//        }
//        
//        return medianPoints;
//    }
//
//    /**
//     * Computes top 10% values for all angles.
//     */
//    private ArrayList<Point> calculateTop10PercentPoints(SailorProfileStatisticDTO statisticData) {
//        Map<Integer, List<Double>> valuesByPosition = new HashMap<>();
//        
//        for (Entry<SimpleCompetitorWithIdDTO, ArrayList<SingleEntry>> entry : statisticData.getResult().entrySet()) {
//            ArrayList<SingleEntry> values = entry.getValue();
//            for (int i = 0; i < values.size(); i++) {
//                if (!valuesByPosition.containsKey(i)) {
//                    valuesByPosition.put(i, new ArrayList<>());
//                }
//                valuesByPosition.get(i).add(values.get(i).getValue());
//            }
//        }
//        
//        ArrayList<Point> top10Points = new ArrayList<>();
//        for (int position = 0; position < valuesByPosition.size(); position++) {
//            double angle = (position * 360.0 / valuesByPosition.size());
//            if (angle > 180) angle -= 360;
//            
//            // Compute top 10% (best = smallest values)
//            List<Double> values = valuesByPosition.get(position);
//            Collections.sort(values);
//            int top10Count = Math.max(1, (int) Math.ceil(values.size() * 0.1));
//            double top10Average = values.subList(0, top10Count).stream()
//                .mapToDouble(Double::doubleValue)
//                .average()
//                .orElse(0.0);
//            
//            top10Points.add(new Point(angle, top10Average));
//        }
//        return top10Points;
//    }
//}

//polarChartTest = ChartFactory.createPolarChart();
//polarChartTest.getYAxis().setMin(0);
//polarChartWrapperPanel = new SimpleLayoutPanel() {
//  @Override
//  public void onResize() {
//      polarChartTest.setSizeToMatchContainer();
//      polarChartTest.redraw();
//  }
//};

//polarChartWrapperPanel.add(polarChartTest);
//dockLayoutPanel = new DockLayoutPanel(Unit.PCT);
//dockLayoutPanel.addWest(polarChartWrapperPanel, 40);
//accordionAnalyticsUi.setVisible(true);
//setupAnalyticsTablesAndCharts(entry); 
//polarChartWrapperPanel.setHeight("400px");
//accordionAnalyticsUi.addWidget(polarChartWrapperPanel);

//Chart polarChart = ChartFactory.createPolarChart();
//SimpleLayoutPanel wrapper = new SimpleLayoutPanel();
//wrapper.add(polarChart);
//wrapper.setHeight("400px"); 
//accordionAnalyticsUi.addWidget(wrapper);

// Implementation of setupAnalyticsTablesAndChart() with ChartFactory
//private void setupAnalyticsTablesAndCharts(SailorProfileDTO entry) {
//  try {
//      // Highcharts-More laden für Polar Charts
//      Highcharts.ensureInjectedWithMore();
//
//      Chart polarChart = ChartFactory.createPolarChart();
//          polarChart.setType(Series.Type.LINE); // oder AREA, COLUMN
//          polarChart.setChartTitleText("Sailing Performance Chart");
//          polarChart.setPolar(true);
//          polarChart.getYAxis().setMin(0);
//          polarChart.setWidth("100%");
//          polarChart.setHeight("400px");     
//          polarChart.setOption("/pane/startAngle", 0);  // Startwinkel
//
//          // X-Axis = Winkel (0-360° oder -180 bis +180°)
//          polarChart.getXAxis()
//              .setMin(-179)
//              .setMax(180)
//              .setTickInterval(45);  // Alle 45°
//
//          // Y-Axis = Radius (Geschwindigkeit, Distanz, etc.)
//          polarChart.getYAxis()
//              .setMin(0);
//
//      Series series = polarChart.createSeries()
//        .setName("Boat Speed")
//        .addPoint(-180, 3.5)
//        .addPoint(-135, 5.2)
//        .addPoint(-90, 6.8)
//        .addPoint(-45, 7.1)
//        .addPoint(0, 4.2)
//        .addPoint(45, 7.3)
//        .addPoint(90, 6.9)
//        .addPoint(135, 5.1)
//        .addPoint(180, 3.4);              
//      polarChart.addSeries(series);  
//      
//      // Chart in Panel packen
//      SimpleLayoutPanel chartWrapper = new SimpleLayoutPanel();
//      chartWrapper.add(polarChart);
//      chartWrapper.setHeight("400px");
//        
//      // Zum Accordion hinzufügen
//      accordionAnalyticsUi.addWidget(chartWrapper);
//
//      Label successLabel = new Label("Chart erfolgreich erstellt!");
//      accordionPolarDiagramUi.addWidget(successLabel);
//  } 
//  catch (Exception e) {
//        Label errorLabel = new Label("Fehler: " + e.getMessage());
//        accordionPolarDiagramUi.addWidget(errorLabel);
//        GWT.log("Chart error", e);
//  }
//}
//private void DEPRECATEDcreatePolarChart(SailorProfileDTO entry) {
//  try {
//      // Highcharts-More laden für Polar Charts
//      Highcharts.ensureInjectedWithMore();
//      
//      //LinePlotOptions 
//      LinePlotOptions linePlotOptions = new LinePlotOptions()
//          .setLineWidth(1)
//          .setMarker(new Marker().setEnabled(false));
//      
//      // Einfaches Line Chart erstellen
//      Chart chart = new Chart()
//          .setType(Series.Type.LINE)
//          .setLinePlotOptions(linePlotOptions)
//          .setPolar(true)
//          .setHeight100()
//          .setWidth100()
//          .setTitle(new ChartTitle().setText("Sailing Performance Chart"), 
//                    new ChartSubtitle().setText("KPI-1"));
//     chart.getXAxis().setMin(-179).setMax(180).setTickInterval(45);
//     chart.setOption("/pane/startAngle", 180);
//     chart.setExporting(new Exporting().setEnabled(false));
//    
//      // Sample-Daten hinzufügen
//      Series series = chart.createSeries()
//          .setName("Boat Speed")
//          .addPoint(-180, 3.5)
//          .addPoint(-135, 5.2)
//          .addPoint(-90, 6.8)
//          .addPoint(-45, 7.1)
//          .addPoint(0, 4.2)
//          .addPoint(45, 7.3)
//          .addPoint(90, 6.9)
//          .addPoint(135, 5.1)
//          .addPoint(180, 3.4); 
//      chart.addSeries(series);
//      
//      Series series2 = chart.createSeries()
//              .setName("Boat Speed")
//              .addPoint(-190, 3.5)
//              .addPoint(-155, 5.2)
//              .addPoint(-90, 6.8)
//              .addPoint(-45, 7.3)
//              .addPoint(0, 4.2)
//              .addPoint(45, 7.3)
//              .addPoint(90, 6.9)
//              .addPoint(165, 5.1)
//              .addPoint(180, 3.6); 
//          chart.addSeries(series2);
//                  
//      // Chart in Panel packen
//      SimpleLayoutPanel chartWrapper = new SimpleLayoutPanel();
//      chartWrapper.add(chart);
//      chartWrapper.setHeight("400px");
//      
//      // Zum Accordion hinzufügen
//      accordionAnalyticsUi.addWidget(chartWrapper);
//      
//      // Erfolgs-Label
//      Label successLabel = new Label("Chart erfolgreich erstellt!");
//      accordionStatisticsUi.addWidget(successLabel);
//  } 
//  catch (Exception e) {
//      Label errorLabel = new Label("Fehler: " + e.getMessage());
//      accordionStatisticsUi.addWidget(errorLabel);
//      GWT.log("Chart error", e);
//  }
//}
