package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.moxieapps.gwt.highcharts.client.Axis;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.ChartTitle;
import org.moxieapps.gwt.highcharts.client.Color;
import org.moxieapps.gwt.highcharts.client.Credits;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.ToolTip;
import org.moxieapps.gwt.highcharts.client.ToolTipData;
import org.moxieapps.gwt.highcharts.client.ToolTipFormatter;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;
import org.moxieapps.gwt.highcharts.client.plotOptions.LinePlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker.Symbol;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.actions.GetLeaderboardDataEntriesAction;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionChangeListener;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.common.filter.FilterSet;
import com.sap.sse.common.settings.generic.GenericSerializableSettings;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.components.AbstractLazyComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

/**
 * A base class for a leaderboard chart showing competitor data for all race columns of a leaderboard.
 */
public abstract class AbstractCompetitorLeaderboardChart<SettingsType extends GenericSerializableSettings>
        extends AbstractLazyComponent<SettingsType>
        implements Component<SettingsType>, 
    CompetitorSelectionChangeListener, RequiresResize, TimeListener {
    public static final String LODA_LEADERBOARD_CHART_DATA_CATEGORY = "loadLeaderboradChartData";
    
    private static final int LINE_WIDTH = 1;
    protected final CompetitorSelectionProvider competitorSelectionProvider;
    protected final Map<CompetitorDTO, Series> competitorSeries;
    protected Chart chart;
    protected final Timer timer;
    private DetailType selectedDetailType;
    private final SailingServiceAsync sailingService;
    private final AsyncActionsExecutor asyncActionsExecutor;
    private final ErrorReporter errorReporter;

    private final List<String> raceColumnNames;
    private final List<String> raceColumnNamesWithData;
    protected final String leaderboardName;
    protected final StringMessages stringMessages;
    
    private final List<Runnable> chartDataUpdateCallbacks = new ArrayList<>();

    public AbstractCompetitorLeaderboardChart(Component<?> parent, ComponentContext<?> context,
            SailingServiceAsync sailingService,
            AsyncActionsExecutor asyncActionsExecutor, String leaderboardName,
            DetailType detailType, CompetitorSelectionProvider competitorSelectionProvider, Timer timer,
            final StringMessages stringMessages, ErrorReporter errorReporter) {
        super(parent, context);
        this.sailingService = sailingService;
        this.asyncActionsExecutor = asyncActionsExecutor;
        this.competitorSelectionProvider = competitorSelectionProvider;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.timer = timer;
        this.leaderboardName = leaderboardName;
        this.selectedDetailType = detailType;
        competitorSeries = new HashMap<>();
        raceColumnNames = new ArrayList<String>();
        raceColumnNamesWithData = new ArrayList<String>();
    }

    @Override
    public Widget createWidget() {
        setSize("100%", "100%");
        chart = createChart();
        setSelectedDetailType(selectedDetailType);
        
        timer.addTimeListener(this);
        competitorSelectionProvider.addCompetitorSelectionChangeListener(this);
        
        return chart;
    }
    
    /**
     * Add a {@link Runnable callback} to get notified when the chart data has been updated successfully.
     * 
     * @param callback {@link Runnable} to call on chart data update
     * @return {@link HandlerRegistration} to remove the callback
     */
    public HandlerRegistration addChartDataUpdatedHandler(final Runnable callback) {
        this.chartDataUpdateCallbacks.add(callback);
        return () -> this.chartDataUpdateCallbacks.remove(callback);
    }


    protected boolean isCompetitorVisible(CompetitorDTO competitor) {
        return Util.isEmpty(competitorSelectionProvider.getSelectedCompetitors()) || competitorSelectionProvider.isSelected(competitor);
    }
    
    private Chart createChart() {
        Chart chart = new Chart()
            .setPersistent(true)
            .setMarginLeft(40)
            .setMarginRight(40)
            .setWidth100()
            .setBorderColor(new Color("#CACACA"))
            .setBorderWidth(1)
            .setBorderRadius(0)
            .setBackgroundColor(new Color("#EBEBEB"))
            .setPlotBorderWidth(0)
            .setCredits(new Credits().setEnabled(false))
            .setChartTitle(new ChartTitle().setText(stringMessages.rank()))
            .setLinePlotOptions(new LinePlotOptions().setLineWidth(LINE_WIDTH).setMarker(
                    new Marker().setEnabled(true).setHoverState(
                            new Marker().setEnabled(true).setRadius(4))).setShadow(false)
                                .setHoverStateLineWidth(LINE_WIDTH));
        ChartUtil.useCheckboxesToShowAndHide(chart, () -> {});
        chart.getXAxis().setType(Axis.Type.LINEAR);
        chart.getXAxis().setLabels(new XAxisLabels().setFormatter(new AxisLabelsFormatter() {
            @Override
            public String format(AxisLabelsData axisLabelsData) {
                Double valueAsDouble = axisLabelsData.getValueAsDouble();
                return raceColumnNamesWithData.get(valueAsDouble.intValue());
            }
        }));
        chart.getYAxis().setAllowDecimals(false).setStartOnTick(true).setEndOnTick(true);
        chart.getYAxis().setShowFirstLabel(false).setShowLastLabel(false);
        chart.getYAxis().setMinorTickInterval(null);
        chart.getYAxis().setTickInterval(1);
        chart.getXAxis().setTickInterval(1);
        
        return chart;
    }
    
    protected void setSelectedDetailType(DetailType newSelectedDetailType) {
        this.selectedDetailType = newSelectedDetailType;
        
        // TODO: There are some problems with the highcharts library to change all values of a chart dynamically.
        // Therefore we need to recreate the chart each time the detail type changes
        chart = createChart();
      
        switch(selectedDetailType) {
            case REGATTA_RANK:
            case OVERALL_RANK:
                chart.getYAxis().setReversed(true);
                chart.getYAxis().setTickInterval(1.0);
                break;
            case REGATTA_NET_POINTS_SUM:
                chart.getYAxis().setTickInterval(5.0);
                chart.getYAxis().setReversed(false);
                break;
            default:
                break;
        }
        chart.setTitle(new ChartTitle().setText(DetailTypeFormatter.format(selectedDetailType)), null);
        chart.ensureDebugId("CompetitorChart");
        final String unit = DetailTypeFormatter.getUnit(getSelectedDetailType());
        final String label = unit.isEmpty() ? "" : "[" + unit + "]";
        chart.getYAxis().setAxisTitleText(label);

        chart.setToolTip(new ToolTip().setEnabled(true).setFormatter(new ToolTipFormatter() {
            @Override
            public String format(ToolTipData toolTipData) {
                String seriesName = toolTipData.getSeriesName();
                Double xValue = toolTipData.getXAsDouble();
                Double yValue = toolTipData.getYAsDouble();
                String raceColumnName = raceColumnNamesWithData.get(xValue.intValue());
                NumberFormat numberFormat = DetailTypeFormatter.getNumberFormat(selectedDetailType);
                
                return "<b>" + seriesName + ":</b> " + " " + 
                    stringMessages.competitorRegattaDataAfterRaceN(numberFormat.format(yValue), raceColumnName);
            }
        }));
    }

    public void clearChart() {
        competitorSeries.clear();
        chart.removeAllSeries();
    }
    
    protected Series getOrCreateSeries(CompetitorDTO competitor) {
        Series result = competitorSeries.get(competitor);
        if (result == null) {
            result = chart.createSeries().setType(Series.Type.LINE).setName(competitor.getName());
            result.setPlotOptions(new LinePlotOptions()
                    .setLineWidth(LINE_WIDTH)
                    .setMarker(new Marker().setEnabled(true).setRadius(4).setSymbol(Symbol.DIAMOND))
                    .setShadow(true)
                    .setHoverStateLineWidth(LINE_WIDTH)
                    .setColor(competitorSelectionProvider.getColor(competitor).getAsHtml())
                    .setSelected(true)
                );
            competitorSeries.put(competitor, result);
            chart.addSeries(result, false, true);
        }
        return result;
    }

    @Override
    public void onResize() {
        chart.setSizeToMatchContainer();
        // it's important here to recall the redraw method, otherwise the bug fix for wrong checkbox positions (nativeAdjustCheckboxPosition)
        // in the BaseChart class would not be called 
        chart.redraw();
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if(!isVisible()) {
            return;
        }
        
        loadChartData(timer.getPlayMode() == PlayModes.Live ? null : newTime);
    }
    
    protected void loadChartData(Date date) {
        if(timer.getPlayMode() != PlayModes.Live) {
            chart.showLoading(stringMessages.loadingCompetitorData());
        }
        
        GetLeaderboardDataEntriesAction getLeaderboardDataEntriesAction = new GetLeaderboardDataEntriesAction(
                sailingService, leaderboardName, /* date */ null, selectedDetailType);
        
        asyncActionsExecutor.execute(getLeaderboardDataEntriesAction, LODA_LEADERBOARD_CHART_DATA_CATEGORY,
                new AsyncCallback<List<Triple<String, List<CompetitorDTO>, List<Double>>>>() {
                    @Override
                    public void onSuccess(List<Triple<String, List<CompetitorDTO>, List<Double>>> result) {
                        chart.hideLoading();
                        setWidget(chart);
                        raceColumnNames.clear();
                        fillSeries(result);
                        // TODO will removing the following line do harm on any usage of this abstract base class?
                        // chart.setSizeToMatchContainer();
                        // it's important here to recall the redraw method, otherwise the bug fix for wrong checkbox
                        // positions (nativeAdjustCheckboxPosition) in the BaseChart class would not be called
                        chart.redraw();
                        chartDataUpdateCallbacks.forEach(Runnable::run);
                    }
                    @Override
                    public void onFailure(Throwable caught) {
                        chart.hideLoading();
                        errorReporter.reportError(stringMessages.errorFetchingChartData(caught.getMessage()),
                                timer.getPlayMode() == PlayModes.Live);
                    }
                });
    }

    private void fillSeries(List<Triple<String, List<CompetitorDTO>, List<Double>>> result) {
        int raceColumnNumber = 0;
        final Map<CompetitorDTO, Point[]> competitorWithCoordinatePoints = new ConcurrentHashMap<>();
        for (Triple<String, List<CompetitorDTO>, List<Double>> entry : result) {
            final List<Double> dataValues = entry.getC();
            raceColumnNames.add(entry.getA());
            if (hasValidValues(dataValues)) {
                raceColumnNamesWithData.add(entry.getA());
                int index = 0;
                for (CompetitorDTO competitor : entry.getB()) {
                    if (isCompetitorVisible(competitor)) {
                        Double dataValue = dataValues.get(index);
                        if (dataValue != null) {
                            final Point[] points = competitorWithCoordinatePoints
                                    .computeIfAbsent(competitor, c -> new Point[result.size()]);
                            points[raceColumnNumber] = new Point(raceColumnNumber, dataValue);
                        }
                    }
                    index++;
                }
                raceColumnNumber++;
            }
        }
        competitorWithCoordinatePoints
                .forEach((competitor, points) -> getOrCreateSeries(competitor).setPoints(points, false));
        setHeight();
    }
    
    private void setHeight() {
        chart.setSize(chart.getOffsetWidth(), Window.getClientHeight());
    }
    
    private boolean hasValidValues(List<Double> values) {
        boolean result = false;
        for(Double value: values) {
            if(value != null) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    @Override
    public Widget getEntryWidget() {
        return this;
    }

    @Override
    public void competitorsListChanged(Iterable<CompetitorDTO> competitors) {
        timeChanged(timer.getTime(), null);
    }

    @Override
    public void addedToSelection(CompetitorDTO competitor) {
        timeChanged(timer.getTime(), null);
    }

    @Override
    public void removedFromSelection(CompetitorDTO competitor) {
        timeChanged(timer.getTime(), null);
    }

    protected DetailType getSelectedDetailType() {
        return selectedDetailType;
    }

    @Override
    public void filteredCompetitorsListChanged(Iterable<CompetitorDTO> filteredCompetitors) {
    }

    @Override
    public void filterChanged(FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> oldFilterSet,
            FilterSet<CompetitorDTO, ? extends Filter<CompetitorDTO>> newFilterSet) {
    }
}
