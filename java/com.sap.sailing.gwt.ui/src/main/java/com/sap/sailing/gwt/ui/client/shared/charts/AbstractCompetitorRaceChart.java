package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.moxieapps.gwt.highcharts.client.Axis;
import org.moxieapps.gwt.highcharts.client.BaseChart;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.ChartSubtitle;
import org.moxieapps.gwt.highcharts.client.ChartTitle;
import org.moxieapps.gwt.highcharts.client.Color;
import org.moxieapps.gwt.highcharts.client.Credits;
import org.moxieapps.gwt.highcharts.client.PlotLine;
import org.moxieapps.gwt.highcharts.client.PlotLine.DashStyle;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.ToolTip;
import org.moxieapps.gwt.highcharts.client.ToolTipData;
import org.moxieapps.gwt.highcharts.client.ToolTipFormatter;
import org.moxieapps.gwt.highcharts.client.events.ChartClickEvent;
import org.moxieapps.gwt.highcharts.client.events.ChartClickEventHandler;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEvent;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEventHandler;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;
import org.moxieapps.gwt.highcharts.client.plotOptions.LinePlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.ScatterPlotOptions;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.ui.actions.GetCompetitorsRaceDataAction;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.CompetitorRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.CompetitorsRaceDataDTO;
import com.sap.sailing.gwt.ui.shared.SailingServiceConstants;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

/**
 * AbstractCompetitorChart is a chart that can show one sort of competitor data (e.g. current speed over ground,
 * windward distance to leader) for different races in a chart.
 * 
 * When calling the constructor a chart is created that creates a final amount of series (so the maximum number of
 * competitors cannot be changed in one chart) which are connected to competitors, when the sailing service returns the
 * data. So {@code seriesID, competitorID and markSeriesID} are linked with the index. So if u know for example the
 * seriesID-index, you can get the competitor by calling competitorID.get(index).
 * 
 * @author Benjamin Ebling (D056866), Axel Uhl (d043530)
 * 
 */
public abstract class AbstractCompetitorRaceChart<SettingsType extends ChartSettings> extends AbstractRaceChart<SettingsType> implements
        CompetitorSelectionChangeListener, RequiresResize {
    public static final String LOAD_COMPETITOR_CHART_DATA_CATEGORY = "loadCompetitorChartData";
    
    public static final long DEFAULT_STEPSIZE = 5000;
    
    private static final int LINE_WIDTH = 1;
    
    private final Label noCompetitorsSelectedLabel;
    private final Label noDataFoundLabel;
    private final RaceCompetitorSelectionProvider competitorSelectionProvider;
    private DetailType selectedFirstDetailType;
    private DetailType selectedSecondDetailType;

    private boolean compactChart;
    private final boolean allowTimeAdjust;
    private final String leaderboardGroupName;
    private final String leaderboardName;
    private long stepSizeInMillis = DEFAULT_STEPSIZE;
    private final Map<Pair<CompetitorWithBoatDTO, DetailType>, Series> dataSeriesForDetailTypeAndCompetitor = new HashMap<>();
    private final Map<Pair<CompetitorWithBoatDTO, DetailType>, Series> markPassingSeriesByCompetitor = new HashMap<>();
    
    private final TimingHolder primary = new TimingHolder();
    private final TimingHolder secondary = new TimingHolder();

    /**
     * Whenever data is received from the server, the effective step size of that data is captured
     * in this field. Ideally, it would equal the {@link #getStepSizeInMillis() desired step size}, but
     * long time ranges covered by the chart and the lack of zoom may require increasing the effective step size
     * so as to reduce the number of fixes requested from the server.<p>
     * 
     * However, when zooming this will be re-evaluated, and a more fine-grained request to the server, limited to
     * the zoom range, may then be issued.
     */
    private long effectiveStepSize = -1;
    
    AbstractCompetitorRaceChart(Component<?> parent, ComponentContext<?> context, SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor,
            RaceCompetitorSelectionProvider competitorSelectionProvider, RegattaAndRaceIdentifier selectedRaceIdentifier,
            Timer timer, TimeRangeWithZoomProvider timeRangeWithZoomProvider, final StringMessages stringMessages,
            ErrorReporter errorReporter, DetailType firstDetailType, DetailType secondDetailType, boolean compactChart,
            boolean allowTimeAdjust,
            String leaderboardGroupName, String leaderboardName) {
        super(parent, context, sailingService, selectedRaceIdentifier, timer, timeRangeWithZoomProvider, stringMessages,
                asyncActionsExecutor, errorReporter);
        
        this.competitorSelectionProvider = competitorSelectionProvider;
        this.compactChart = compactChart;
        this.allowTimeAdjust = allowTimeAdjust;
        this.leaderboardGroupName = leaderboardGroupName;
        this.leaderboardName = leaderboardName;
        
        setSize("100%", "100%");
        noCompetitorsSelectedLabel = new Label(stringMessages.selectAtLeastOneCompetitor() + ".");
        noCompetitorsSelectedLabel.setStyleName("abstractChartPanel-importantMessageOfChart");
        noDataFoundLabel = new Label(stringMessages.noDataFound() + ".");
        noDataFoundLabel.setStyleName("abstractChartPanel-importantMessageOfChart");
        chart = createChart();
        setSelectedDetailTypes(firstDetailType, secondDetailType);
        competitorSelectionProvider.addCompetitorSelectionChangeListener(this);
        clearChart();
        if (selectedRaceIdentifier != null) {
            timeChanged(timer.getTime(), null);
        }
    }
    
    /**
     * Creates a new chart.
     * Attention: We can't reuse the old chart when the detail changes because HighChart does not support the inverting of the Y-Axis  
     */
    private Chart createChart() {

        Chart chart = new Chart().setZoomType(BaseChart.ZoomType.X)
                .setPersistent(true)
                .setReflow(false)
                .setAlignTicks(false)
                .setWidth100()
                .setHeight100()
                .setMarginLeft(65)
                .setMarginRight(65)
                .setBorderColor(new Color("#CACACA"))
                .setBackgroundColor(new Color("#FFFFFF"))
                .setPlotBackgroundColor("#f8f8f8")
                .setBorderWidth(0)
                .setBorderRadius(0)
                .setPlotBorderWidth(0)
                .setCredits(new Credits().setEnabled(false))
                .setChartSubtitle(new ChartSubtitle().setText(stringMessages.clickAndDragToZoomIn()));
        chart.setStyleName(chartsCss.chartStyle());
        ChartUtil.useCheckboxesToShowAndHide(chart);

        if (allowTimeAdjust) {
            chart.setClickEventHandler(new ChartClickEventHandler() {
                @Override
                public boolean onClick(ChartClickEvent chartClickEvent) {
                    return AbstractCompetitorRaceChart.this.onClick(chartClickEvent);
                }
            });
            chart.setSelectionEventHandler(new ChartSelectionEventHandler() {
                @Override
                public boolean onSelection(ChartSelectionEvent chartSelectionEvent) {
                    return AbstractCompetitorRaceChart.this.onXAxisSelectionChange(chartSelectionEvent);
                }
            });
        }

        if (hasSecondYAxis()) {
            chart.getYAxis(0).setStartOnTick(false).setShowFirstLabel(false);
            chart.getYAxis(1).setStartOnTick(false).setShowFirstLabel(false).setOpposite(true);
        } else {
            chart.getYAxis(0).setStartOnTick(false).setShowFirstLabel(false);
            chart.setLinePlotOptions(new LinePlotOptions()
                    .setLineWidth(LINE_WIDTH)
                    .setMarker(new Marker().setEnabled(false).setHoverState(new Marker().setEnabled(true).setRadius(4)))
                    .setShadow(false).setHoverStateLineWidth(LINE_WIDTH));
        }
        chart.getXAxis().setType(Axis.Type.DATE_TIME).setMaxZoom(60 * 1000); // 1 minute
        chart.getXAxis().setLabels(new XAxisLabels().setFormatter(new AxisLabelsFormatter() {
            @Override
            public String format(AxisLabelsData axisLabelsData) {
                return dateFormatHoursMinutes.format(new Date(axisLabelsData.getValueAsLong()));
            }
        }));
        timePlotLine = chart.getXAxis().createPlotLine().setColor("#656565").setWidth(1.5)
                .setDashStyle(DashStyle.SOLID);
        return chart;
    }

    protected abstract Component<SettingsType> getComponent();

    /**
     * Loads the needed data (data which isn't in the {@link #chartData cache}) for the
     * {@link #getSelectedCompetitors() visible competitors} via
     * {@link SailingServiceAsync#getCompetitorsRaceData(RaceIdentifier, List, long, DetailType, AsyncCallback)}. After
     * loading, the method {@link #drawChartData()} is called.
     * <p>
     * If no data needs to be {@link #needsDataLoading() loaded}, the "no competitors selected" label is displayed.
     */
    private void updateChart(Date from, Date to, boolean append) {
        if (hasSelectedCompetitors() && isVisible()) {
            remove(noCompetitorsSelectedLabel);
            if (!getChildren().contains(chart)) {
                add(chart);
            }
            ArrayList<CompetitorWithBoatDTO> competitorsToLoad = new ArrayList<CompetitorWithBoatDTO>();
            for (CompetitorWithBoatDTO competitorDTO : getSelectedCompetitors()) {
                competitorsToLoad.add(competitorDTO);
            }
            loadData(from, to, competitorsToLoad, append);
        } else {
            remove(chart);
            if (!getChildren().contains(noCompetitorsSelectedLabel)) {
                add(noCompetitorsSelectedLabel);
            }
        }
    }

    private void loadData(final Date from, final Date to, final List<CompetitorWithBoatDTO> competitors, final boolean append) {
        if (isVisible()) {
            // if no data is loaded yet, or if it is not playing and not live (append loading every second) show loading
            // indicator
            if (shouldShowLoading(primary.timeOfLatestRequestInMillis)) {
                showLoading(stringMessages.loadingCompetitorData());
            }
            ArrayList<CompetitorWithBoatDTO> competitorsToLoad = new ArrayList<CompetitorWithBoatDTO>();
            for (CompetitorWithBoatDTO competitorDTO : competitors) {
                competitorsToLoad.add(competitorDTO);
            }
            // If the time interval is too long and the step size too small, the number of fixes the query would have to
            // produce may exceed any reasonable limit. Therefore, we limit the number of fixes that such a query may ask
            // for:
            doLoadDataForCompetitorsAndDataType(from, to, append, competitorsToLoad, getSelectedFirstDetailType(), primary);
            if (getSelectedSecondDetailType() != null) {
                doLoadDataForCompetitorsAndDataType(from, to, append, competitorsToLoad, getSelectedSecondDetailType(), secondary);
            }
        }
    }

    @Override
    protected void changeMinMaxAndExtremesInterval(Date minTimepoint, Date maxTimepoint, boolean redraw) {
        super.changeMinMaxAndExtremesInterval(minTimepoint, maxTimepoint, redraw);
        updateChartIfEffectiveStepSizeChanged(minTimepoint, maxTimepoint);
    }

    protected void updateChartIfEffectiveStepSizeChanged(Date minTimepoint, Date maxTimepoint) {
        long effectiveStepSize = getEffectiveStepSize(minTimepoint, maxTimepoint);
        if (this.effectiveStepSize != -1 && this.effectiveStepSize != effectiveStepSize) {
            // the change has had an impact on the effective step size; trigger a re-load
            updateChart(minTimepoint, maxTimepoint, /* append==false means replace points in chart by the new ones */ false);
        }
    }

    private void doLoadDataForCompetitorsAndDataType(final Date from, final Date to, final boolean append,
            ArrayList<CompetitorWithBoatDTO> competitorsToLoad, final DetailType selectedDataTypeToRetrieve, final TimingHolder tholder) {
        long effectiveStepSize = getEffectiveStepSize(from, to);
        GetCompetitorsRaceDataAction getCompetitorsRaceDataAction = new GetCompetitorsRaceDataAction(sailingService,
                selectedRaceIdentifier, competitorsToLoad, from, to, effectiveStepSize, selectedDataTypeToRetrieve,
                leaderboardGroupName, leaderboardName);
        AsyncCallback<CompetitorsRaceDataDTO> dataLoadedCallback = new AsyncCallback<CompetitorsRaceDataDTO>() {
            @Override
            public void onSuccess(final CompetitorsRaceDataDTO result) {
                hideLoading();
                AbstractCompetitorRaceChart.this.effectiveStepSize = effectiveStepSize;
                if (result != null) {
                    if (result.isEmpty() && chartContainsNoData()) {
                        setWidget(noDataFoundLabel);
                    } else {
                        updateChartSeries(result, selectedDataTypeToRetrieve, append, tholder);
                    }
                } else {
                    if (!append) {
                        clearChart();
                    }
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                hideLoading();
                errorReporter.reportError(stringMessages.errorFetchingChartData(caught.getMessage()),
                        timer.getPlayMode() == PlayModes.Live);
            }
        };
        if (append) {
            // this call is repeated, allow it to be throttled and dropped
            asyncActionsExecutor.execute(getCompetitorsRaceDataAction, LOAD_COMPETITOR_CHART_DATA_CATEGORY,
                    dataLoadedCallback);
        } else {
            // ensure that non appending only once loading is reliable and cannot be dropped by not using
            // asyncActionExecutor
            getCompetitorsRaceDataAction.execute(dataLoadedCallback);
        }
    }

    /**
     * Based on a time range and a {@link #getStepSizeInMillis() desired step size} computes an effective
     * step size that observes a maximum number of fixes that may be queried in one request from the server
     * ({@link SailingServiceConstants#MAX_NUMBER_OF_FIXES_TO_QUERY}). If the time range and the desired step
     * size do not exceed this limit, the step size desired will be returned. Otherwise, the step size will be
     * extended such that {@link SailingServiceConstants#MAX_NUMBER_OF_FIXES_TO_QUERY} fixes will be requested
     * for the time range between {@code from} and {@code to}.
     * 
     * @return the effective step size in milliseconds
     */
    private long getEffectiveStepSize(final Date from, final Date to) {
        return Math.max(getStepSizeInMillis(),
                from==null||to==null ? 0 : Math.abs(to.getTime()-from.getTime())/SailingServiceConstants.MAX_NUMBER_OF_FIXES_TO_QUERY);
    }
    
    private boolean chartContainsNoData() {
        for (Series competitorSeries : dataSeriesForDetailTypeAndCompetitor.values()) {
            if (competitorSeries.getPoints().length != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addedToSelection(CompetitorWithBoatDTO competitor) {
        if (isVisible()) {
            showLoading(stringMessages.loadingCompetitorData());
            ArrayList<CompetitorWithBoatDTO> competitorsToLoad = new ArrayList<CompetitorWithBoatDTO>();
            competitorsToLoad.add(competitor);
            
            {
                Date fromDate = primary.timeOfEarliestRequestInMillis == null ? null : new Date(primary.timeOfEarliestRequestInMillis);
                Date toDate = primary.timeOfLatestRequestInMillis == null ? null : new Date(primary.timeOfLatestRequestInMillis);
                loadData(fromDate, toDate, competitorsToLoad, false);
            }
            {
                Date fromDate = secondary.timeOfEarliestRequestInMillis == null ? null : new Date(secondary.timeOfEarliestRequestInMillis);
                Date toDate = secondary.timeOfLatestRequestInMillis == null ? null : new Date(secondary.timeOfLatestRequestInMillis);
                loadData(fromDate, toDate, competitorsToLoad, false);
            }
        }
    }

    @Override
    public void removedFromSelection(CompetitorWithBoatDTO competitor) {
        for (Pair<CompetitorWithBoatDTO, DetailType> coDePair : dataSeriesForDetailTypeAndCompetitor.keySet()) {
            if (!coDePair.getA().equals(competitor)) {
                continue;
            }
            Series competitorSeries = dataSeriesForDetailTypeAndCompetitor.get(coDePair);
            if (competitorSeries != null) {
                chart.removeSeries(competitorSeries, false);
            }
            Series competitorMarkPassingSeries = markPassingSeriesByCompetitor.get(coDePair);
            if (competitorMarkPassingSeries != null) {
                chart.removeSeries(competitorMarkPassingSeries, false);
            }
            if (isVisible()) {
                if (hasSelectedCompetitors()) {
                    chart.redraw();
                } else {
                    setWidget(noCompetitorsSelectedLabel);
                }
            }
        }
    }

    /**
     * Creates the series for all selected competitors if these aren't created yet.<br />
     * Fills the series for the selected competitors with the data in {@link AbstractCompetitorRaceChart#chartData}.<br />
     */
    private synchronized void updateChartSeries(CompetitorsRaceDataDTO chartData, DetailType retrievedDataType,
            boolean append, TimingHolder tholder) {
        // Make sure the busy indicator is removed at this point, or plotting the data results in an exception
        setWidget(chart);
        for (CompetitorWithBoatDTO competitor : chartData.getCompetitors()) {
            Series competitorDataSeries = getOrCreateCompetitorDataSeries(retrievedDataType, competitor);
            Series markPassingSeries = getOrCreateCompetitorMarkPassingSeries(competitorDataSeries, retrievedDataType, competitor);
            CompetitorRaceDataDTO competitorData = chartData.getCompetitorData(competitor);
            if (competitorData != null) {
                Date toDate = timer.getLiveTimePointAsDate();
                List<com.sap.sse.common.Util.Triple<String, Date, Double>> markPassingsData = competitorData.getMarkPassingsData();
                List<Point> markPassingPoints = new ArrayList<Point>();
                for (com.sap.sse.common.Util.Triple<String, Date, Double> markPassingData : markPassingsData) {
                    if (markPassingData.getB() != null && markPassingData.getC() != null) {
                        if (markPassingData.getB().before(toDate)) {
                            Point markPassingPoint = new Point(markPassingData.getB().getTime(),
                                    markPassingData.getC());
                            markPassingPoint.setName(markPassingData.getA());
                            markPassingPoints.add(markPassingPoint);
                        }
                    }
                }
                markPassingSeries.setPoints(markPassingPoints.toArray(new Point[0]), false);
                Point[] oldRaceDataPoints = competitorDataSeries.getPoints();
                List<com.sap.sse.common.Util.Pair<Date, Double>> raceData = competitorData.getRaceData();
                Point[] raceDataPointsToAdd = new Point[raceData.size()];
                int currentPointIndex = 0;
                for (com.sap.sse.common.Util.Pair<Date, Double> raceDataPoint : raceData) {
                    Double dataPointValue = raceDataPoint.getB();
                    if (dataPointValue != null) {
                        long dataPointTimeAsMillis = raceDataPoint.getA().getTime();
                        if (append == false || (tholder.timeOfEarliestRequestInMillis == null || dataPointTimeAsMillis < tholder.timeOfEarliestRequestInMillis) || 
                                tholder.timeOfLatestRequestInMillis == null || dataPointTimeAsMillis > tholder.timeOfLatestRequestInMillis) {
                            raceDataPointsToAdd[currentPointIndex++] = new Point(dataPointTimeAsMillis, dataPointValue);
                        }
                    }
                }
                Point[] newRaceDataPoints;
                if (append) {
                    newRaceDataPoints = new Point[oldRaceDataPoints.length + currentPointIndex];
                    System.arraycopy(oldRaceDataPoints, 0, newRaceDataPoints, 0, oldRaceDataPoints.length);
                    System.arraycopy(raceDataPointsToAdd, 0, newRaceDataPoints, oldRaceDataPoints.length, currentPointIndex);
                } else {
                    newRaceDataPoints = new Point[currentPointIndex];
                    System.arraycopy(raceDataPointsToAdd, 0, newRaceDataPoints, 0, currentPointIndex);
                }
                if (retrievedDataType.isDegreeTypeWithRecalculation()) {
                    for (int i = 1; i < newRaceDataPoints.length; i++) {
                        final Point previous = newRaceDataPoints[i - 1], current = newRaceDataPoints[1];
                        newRaceDataPoints[i] = ChartPointRecalculator.stayClosestToPreviousPoint(previous, current);
                    }
                }
                setSeriesPoints(competitorDataSeries, newRaceDataPoints, /* manageZoom */ append);
                // Adding the series if chart doesn't contain it
                List<Series> chartSeries = Arrays.asList(chart.getSeries());
                if (!chartSeries.contains(competitorDataSeries)) {
                    chart.addSeries(competitorDataSeries);
                    chart.addSeries(markPassingSeries);
                    // this is to prevent a bug, that will prohibit the initial rendering
                    chart.setSizeToMatchContainer();
                }
            }
        }
        if (tholder.timeOfEarliestRequestInMillis == null || tholder.timeOfEarliestRequestInMillis > chartData.getRequestedFromTime().getTime()) {
            tholder.timeOfEarliestRequestInMillis = chartData.getRequestedFromTime().getTime();
        }
        if (tholder.timeOfLatestRequestInMillis == null || tholder.timeOfLatestRequestInMillis < chartData.getRequestedToTime().getTime()) {
            tholder.timeOfLatestRequestInMillis = chartData.getRequestedToTime().getTime();
        }
        chart.redraw();
    }

    private Iterable<CompetitorWithBoatDTO> getSelectedCompetitors() {
        return competitorSelectionProvider.getSelectedCompetitors();
    }

    private boolean hasSelectedCompetitors() {
        return Util.size(getSelectedCompetitors()) > 0;
    }

    /**
     * 
     * @param competitor
     * @return A series in the chart, that can be used to show the data of a specific competitor.
     */
    protected Series getOrCreateCompetitorDataSeries(DetailType seriesDetailType, final CompetitorWithBoatDTO competitor) {
        final Pair<CompetitorWithBoatDTO, DetailType> coDePair = new Pair<CompetitorWithBoatDTO, DetailType>(competitor,
                seriesDetailType);
        final int yAxisIndex = yAxisIndex(seriesDetailType);
        Series result = this.dataSeriesForDetailTypeAndCompetitor.get(coDePair);
        if (result == null) {
            result = chart.createSeries().setType(Series.Type.LINE);

            if (hasSecondYAxis()) {
                result.setName(DetailTypeFormatter.format(seriesDetailType) + " " + competitor.getName());
            } else {
                result.setName(competitor.getName());
            }
            result.setPlotOptions(new LinePlotOptions()
                    .setLineWidth(LINE_WIDTH)
                    .setDashStyle(yAxisIndex == 0 ? PlotLine.DashStyle.SOLID : PlotLine.DashStyle.LONG_DASH)
                    .setMarker(new Marker().setEnabled(false).setHoverState(new Marker().setEnabled(true).setRadius(4)))
                    .setShadow(false).setHoverStateLineWidth(LINE_WIDTH)
                    .setColor(competitorSelectionProvider.getColor(competitor, selectedRaceIdentifier).getAsHtml())
                    .setSelected(true));
            result.setOption("turboThreshold", MAX_SERIES_POINTS);
            dataSeriesForDetailTypeAndCompetitor.put(coDePair, result);
        }

        result.setYAxis(yAxisIndex);
        return result;
    }

    /**
     * 
     * @param competitor
     * @return A series in the chart, that can be used to show the mark passings.
     */
    private Series getOrCreateCompetitorMarkPassingSeries(Series linkedCompetitorSeries, DetailType seriesDetailType,
            CompetitorWithBoatDTO competitor) {
        final Pair<CompetitorWithBoatDTO, DetailType> coDePair = new Pair<CompetitorWithBoatDTO, DetailType>(competitor,
                seriesDetailType);
        Series result = markPassingSeriesByCompetitor.get(coDePair);
        final int yAxisIndex = yAxisIndex(seriesDetailType);
        if (result == null) {
            result = chart.createSeries().setType(Series.Type.SCATTER)
                    .setName(stringMessages.markPassing() + " " + competitor.getName());
            result.setPlotOptions(new ScatterPlotOptions().setLinkedTo(linkedCompetitorSeries)
                    .setColor(
                    competitorSelectionProvider.getColor(competitor, selectedRaceIdentifier).getAsHtml())
                    .setSelected(true));
            markPassingSeriesByCompetitor.put(coDePair, result);
        }
        result.setYAxis(yAxisIndex);
        return result;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                @Override
                public void execute() {
                    // Workaround for a highcharts bug: Set a chart title, overwrite the title,
                    // switch chart to invisible and visible again -> the old title appears
                    if (compactChart) {
                        applyCompactTitle(chartTitleFromDetailTypes());
                    } else {
                        chart.setTitle(hasSecondYAxis() ? null : new ChartTitle().setText(chartTitleFromDetailTypes()),
                                null);
                    }

                    chart.setSizeToMatchContainer();
                    // it's important here to recall the redraw method, otherwise the bug fix for wrong checkbox
                    // positions
                    // (nativeAdjustCheckboxPosition)
                    // in the BaseChart class would not be called
                    chart.redraw();
                }
            });
        }

    }

    private void applyCompactTitle(String title) {
        chart.setSpacingLeft(10).setSpacingRight(10).setSpacingTop(20).setOption("legend/margin", 2)
                .setChartSubtitle(null).getXAxis().setAxisTitleText(null);
        chart.setTitle(hasSecondYAxis() ? null : new ChartTitle().setText(title).setOption("floating",true), null);
        chart.redraw();
    }

    private int yAxisIndex(DetailType seriesDetailType) {
        if (selectedSecondDetailType != null && seriesDetailType == selectedSecondDetailType) {
            return 1;
        } else {
            return 0;
        }
    }

    private String chartTitleFromDetailTypes() {
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(DetailTypeFormatter.format(selectedFirstDetailType));
        if (selectedSecondDetailType != null) {
            titleBuilder.append("/");
            titleBuilder.append(DetailTypeFormatter.format(selectedSecondDetailType));
        }
        return titleBuilder.toString();
    }

    /**
     * Clears the whole chart and empties cached data.
     */
    protected void clearChart() {
        primary.timeOfEarliestRequestInMillis = null;
        primary.timeOfLatestRequestInMillis = null;
        secondary.timeOfEarliestRequestInMillis = null;
        secondary.timeOfLatestRequestInMillis = null;
        dataSeriesForDetailTypeAndCompetitor.clear();
        markPassingSeriesByCompetitor.clear();
        chart.removeAllSeries();
    }

    public String getLocalizedShortName() {
        return chartTitleFromDetailTypes();
    }

    public Widget getEntryWidget() {
        return this;
    }

    public boolean hasSettings() {
        return true;
    }

    public ChartSettings getAbstractSettings() {
        return new ChartSettings(getStepSizeInMillis());
    }

    /**
     * Updates the settings known to be contained in {@link ChartSettings}. Subclasses have to update settings provided
     * by subclasses thereof. Subclasses also need to call {@link #clearChart()} and {@link #updateChart(boolean)}, if
     * this method returns <code>true</code>;
     * 
     * @return <code>true</code> if the settings had been changed and a clearing and loading is needed.
     */
    protected boolean updateSettingsOnly(ChartSettings newSettings) {
        boolean settingsChanged = false;
        if (getStepSizeInMillis() != newSettings.getStepSizeInMillis()) {
            setStepSizeInMillis(newSettings.getStepSizeInMillis());
            settingsChanged = true;
        }
        return settingsChanged;
    }

    protected StringMessages getStringMessages() {
        return stringMessages;
    }

    /**
     * The chart step size in milliseconds
     */
    protected long getStepSizeInMillis() {
        return stepSizeInMillis;
    }

    protected void setStepSizeInMillis(long stepSizeInMillis) {
        this.stepSizeInMillis = stepSizeInMillis;
    }

    protected DetailType getSelectedFirstDetailType() {
        return this.selectedFirstDetailType;
    }

    protected DetailType getSelectedSecondDetailType() {
        return this.selectedSecondDetailType;
    }

    /**
     * Updates the {@link #selectedFirstDetailType} field, clears the chart for the new <code>selectedDetailType</code>
     * and clears the {@link #chartData}.<br />
     * Doesn't {@link #updateChart(boolean) load the data}.
     * 
     * @return <code>true</code> if the detail type changed
     */
    protected boolean setSelectedDetailTypes(DetailType newSelectedFirstDetailType,
            DetailType newSelectedSecondDetailType) {
        boolean hasDetailTypeChanged = !Util.equalsWithNull(newSelectedFirstDetailType, this.selectedFirstDetailType)
                || !Util.equalsWithNull(newSelectedSecondDetailType, this.selectedSecondDetailType);
        if (hasDetailTypeChanged) {
            this.selectedFirstDetailType = newSelectedFirstDetailType;
            this.selectedSecondDetailType = newSelectedSecondDetailType;
            
            chart.getYAxis(0).setReversed(isY0AxisReversed());
            chart.getYAxis(1).setReversed(isY1AxisReversed());
            
            final String unitY0 = DetailTypeFormatter.getUnit(getSelectedFirstDetailType());
            final String labelY0 = unitY0.isEmpty() ? "" : "[" + unitY0 + "]";
            final String unitY1 = hasSecondYAxis() ? DetailTypeFormatter.getUnit(getSelectedSecondDetailType()) : null;
            final String labelY1 = hasSecondYAxis() ? (unitY1.isEmpty() ? "" : "[" + unitY1 + "]") : null;

            if (compactChart) {
                applyCompactTitle(chartTitleFromDetailTypes());
            } else {
                chart.setTitle(hasSecondYAxis() ? null : new ChartTitle().setText(chartTitleFromDetailTypes()), null);
            }
            if (hasSecondYAxis()) {
                chart.getYAxis(0).setAxisTitleText(DetailTypeFormatter.format(selectedFirstDetailType) + " " + labelY0);
                chart.getYAxis(1)
                        .setAxisTitleText(DetailTypeFormatter.format(selectedSecondDetailType) + " " + labelY1);
                chart.getYAxis(0).setOpposite(false)
                .setGridLineWidth(1)
                .setMinorGridLineWidth(0).setMinorGridLineColor("transparent");
                
                chart.getYAxis(1).setOpposite(true)
                .setGridLineWidth(1)
                .setGridLineDashStyle(DashStyle.LONG_DASH)
                .setMinorGridLineWidth(0).setMinorGridLineColor("transparent")
                .setMinorTickIntervalAuto();
            } else {
                chart.getYAxis(0).setAxisTitleText(labelY0);
                chart.getYAxis(1).setAxisTitle(null);
            }
            final NumberFormat numberFormatY0 = DetailTypeFormatter.getNumberFormat(selectedFirstDetailType);
            final NumberFormat numberFormatY1 = hasSecondYAxis() ? DetailTypeFormatter
                    .getNumberFormat(selectedSecondDetailType) : null;
            
            chart.setToolTip(new ToolTip().setEnabled(true).setFormatter(new ToolTipFormatter() {
                @Override
                public String format(ToolTipData toolTipData) {
                    String seriesName = toolTipData.getSeriesName();

                    StringBuilder ttb = new StringBuilder();
                    if (seriesName.equals(stringMessages.time())) {
                        ttb.append("<b>").append(seriesName).append(":</b> ")
                                .append(dateFormat.format(new Date(toolTipData.getXAsLong()))).append("<br/>(")
                                .append(stringMessages.clickChartToSetTime()).append(")");
                    } else {
                        ttb.append("<b>").append(seriesName);
                        if (toolTipData.getPointName() != null) {
                            ttb.append(" ").append(toolTipData.getPointName());
                        }
                        ttb.append("</b><br/>").append(dateFormat.format(new Date(toolTipData.getXAsLong())))
                                .append(": ");
                        if (isSecondYAxis(toolTipData.getSeriesId())) {
                            ttb.append(numberFormatY1.format(toolTipData.getYAsDouble())).append(" ").append(unitY1);
                        } else {
                            ttb.append(numberFormatY0.format(toolTipData.getYAsDouble())).append(" ").append(unitY0);
                        }
                    }
                    return ttb.toString();
                }
            }));
        }
        return hasDetailTypeChanged;
    }
    
    private boolean isSecondYAxis(String seriesId) {
        Series series = chart.getSeries(seriesId);
        for (Entry<Pair<CompetitorWithBoatDTO, DetailType>, Series> entry : dataSeriesForDetailTypeAndCompetitor.entrySet()) {
            if (entry.getValue().equals(series)) {
                return entry.getKey().getB().equals(selectedSecondDetailType);
            }
        }
        return false;
    }
    
    private boolean hasSecondYAxis() {
        return selectedSecondDetailType != null;
    }

    private boolean isY0AxisReversed() {
        return isYAxisReversed(selectedFirstDetailType);
    }

    private boolean isY1AxisReversed() {
        return isYAxisReversed(selectedSecondDetailType);
    }

    private boolean isYAxisReversed(DetailType detailType) {
        return detailType == DetailType.CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD
                || detailType == DetailType.LEG_GAP_TO_LEADER_IN_SECONDS || detailType == DetailType.RACE_RANK
                || detailType == DetailType.REGATTA_RANK || detailType == DetailType.OVERALL_RANK;
    }

    /**
     * Checks the relation of the mark passings to the selection range.
     * 
     * @param markPassingInRange
     *            A Boolean matrix filled by
     *            {@link AbstractCompetitorRaceChart#fillPotentialXValues(double, double, ArrayList, ArrayList, ArrayList)
     *            fillPotentialXValues(...)}
     * @return A pair of Booleans. Value A contains false if a passing is not in the selection (error), so that the
     *         selection range needs to be refactored. Value B returns true if two passings are in range before the
     *         error happened or false, if the error happens before two passings were in the selection. B can be
     *         <code>null</code>.
     */
    public com.sap.sse.common.Util.Pair<Boolean, Boolean> checkPassingRelationToSelection(ArrayList<ArrayList<Boolean>> markPassingInRange) {
        boolean everyPassingInRange = true;
        Boolean twoPassingsInRangeBeforeError = null;
        ArrayList<Boolean> competitorPassings = markPassingInRange.get(0);
        for (int i = 0; i < competitorPassings.size(); i++) {
            Boolean passingInRange = competitorPassings.get(i);
            for (int j = 1; j < markPassingInRange.size(); j++) {
                Boolean passingToCompare = markPassingInRange.get(j).get(i);
                if (passingInRange != null) {
                    if (passingToCompare != null && everyPassingInRange) {
                        everyPassingInRange = passingInRange.equals(passingToCompare);
                        if (passingInRange && passingToCompare) {
                            twoPassingsInRangeBeforeError = true;
                        }
                    } else if (passingToCompare != null) {
                        if (passingInRange && passingToCompare) {
                            twoPassingsInRangeBeforeError = false;
                        }
                    }
                } else {
                    passingInRange = passingToCompare;
                }
            }
        }

        return new com.sap.sse.common.Util.Pair<Boolean, Boolean>(everyPassingInRange, twoPassingsInRangeBeforeError);
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if (!isVisible()) {
            return;
        }
        if (allowTimeAdjust) {
            updateTimePlotLine(newTime);
        }
        switch (timer.getPlayMode()) {
        case Live: {
            // is date before first cache entry or is cache empty?
            if (primary.timeOfEarliestRequestInMillis == null
                    || newTime.getTime() < primary.timeOfEarliestRequestInMillis) {
                updateChart(timeRangeWithZoomProvider.getFromTime(), newTime, /* append */true);
            } else if (newTime.getTime() > primary.timeOfLatestRequestInMillis) {
                updateChart(new Date(primary.timeOfLatestRequestInMillis), timeRangeWithZoomProvider.getToTime(), /* append */true);
            }
            // otherwise the cache spans across date and so we don't need to load anything
            break;
        }
        case Replay: {
            if (primary.timeOfLatestRequestInMillis == null) {
                // pure replay mode
                updateChart(timeRangeWithZoomProvider.getFromTime(), timeRangeWithZoomProvider.getToTime(), /* append */false);
            } else {
                // replay mode during live play
                if (primary.timeOfEarliestRequestInMillis == null || newTime.getTime() < primary.timeOfEarliestRequestInMillis) {
                    updateChart(timeRangeWithZoomProvider.getFromTime(), newTime, /* append */true);
                } else if (newTime.getTime() > primary.timeOfLatestRequestInMillis) {
                    updateChart(new Date(primary.timeOfLatestRequestInMillis), timeRangeWithZoomProvider.getToTime(), /* append */true);
                }
            }
            break;
        }
        }
    }

    @Override
    public void onResize() {
        chart.setSizeToMatchContainer();
        // it's important here to recall the redraw method, otherwise the bug fix for wrong checkbox positions
        // (nativeAdjustCheckboxPosition)
        // in the BaseChart class would not be called
        chart.redraw();
    }

    @Override
    public void competitorsListChanged(Iterable<CompetitorWithBoatDTO> competitors) {
        timeChanged(timer.getTime(), null);
    }
    
    @Override
    public void filteredCompetitorsListChanged(Iterable<CompetitorWithBoatDTO> filteredCompetitors) {
        timeChanged(timer.getTime(), null);
    }

    @Override
    public void filterChanged(FilterSet<CompetitorWithBoatDTO, ? extends Filter<CompetitorWithBoatDTO>> oldFilterSet,
            FilterSet<CompetitorWithBoatDTO, ? extends Filter<CompetitorWithBoatDTO>> newFilterSet) {
        // nothing to do; if it changes the filtered competitor list, a separate call to filteredCompetitorsListChanged will occur
    }
    
    static class TimingHolder{
        private Long timeOfEarliestRequestInMillis;
        private Long timeOfLatestRequestInMillis;
    }

    
}
