package com.sap.sailing.gwt.ui.polarmining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.moxieapps.gwt.highcharts.client.AxisTitle;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.ChartSubtitle;
import org.moxieapps.gwt.highcharts.client.ChartTitle;
import org.moxieapps.gwt.highcharts.client.Exporting;
import org.moxieapps.gwt.highcharts.client.Legend;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.Series.Type;
import org.moxieapps.gwt.highcharts.client.events.PointSelectEvent;
import org.moxieapps.gwt.highcharts.client.events.PointSelectEventHandler;
import org.moxieapps.gwt.highcharts.client.events.PointUnselectEvent;
import org.moxieapps.gwt.highcharts.client.events.PointUnselectEventHandler;
import org.moxieapps.gwt.highcharts.client.events.SeriesHideEvent;
import org.moxieapps.gwt.highcharts.client.events.SeriesHideEventHandler;
import org.moxieapps.gwt.highcharts.client.events.SeriesShowEvent;
import org.moxieapps.gwt.highcharts.client.events.SeriesShowEventHandler;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;
import org.moxieapps.gwt.highcharts.client.plotOptions.LinePlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.datamining.presentation.AbstractResultsPresenter;
import com.sap.sailing.polars.datamining.shared.PolarAggregation;
import com.sap.sailing.polars.datamining.shared.PolarDataMiningSettings;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

/**
 * Allows presentation of {@link PolarAggregation} data.
 * 
 * </br></br>
 * 
 * Contains a polar chart on the left displaying the actual polar diagram and two histograms on the right. The upper
 * histogram shows datacount over angle and the second one shows datacount over windrange upon clicking a point in the
 * polar chart.
 * 
 * </br></br>
 * 
 * Used in conjunction with the datamining framework.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarResultsPresenter extends AbstractResultsPresenter<Settings> {

    private final DockLayoutPanel dockLayoutPanel;
    
    private final Chart polarChart;
    private final SimpleLayoutPanel polarChartWrapperPanel;
    
    private final Chart dataCountHistogramChart;
    private final Chart dataCountPerAngleHistogramChart;
    private final DockLayoutPanel histogramChartsWrapperPanel;
    
    private final Map<Series, Series> histogramSeriesForPolarSeries= new HashMap<>();
    private final Map<Series, Map<Long, Series>> perAngleHistogramSeriesForAngle = new HashMap<>();

    public PolarResultsPresenter(Component<?> parent, ComponentContext<?> context, StringMessages stringMessages) {
        super(parent, context, stringMessages);
        
        polarChart = createPolarChart();
        polarChartWrapperPanel = new SimpleLayoutPanel() {
            @Override
            public void onResize() {
                polarChart.setSizeToMatchContainer();
                polarChart.redraw();
            }
        };
        polarChartWrapperPanel.add(polarChart);

        dataCountHistogramChart = createDataCountHistogramChart(stringMessages.beatAngle() + " ("
                + stringMessages.degreesShort() + ")", true);
        dataCountPerAngleHistogramChart = createDataCountHistogramChart(stringMessages.windSpeed(), true);
        histogramChartsWrapperPanel = new DockLayoutPanel(Unit.PCT) {
            @Override
            public void onResize() {
                dataCountHistogramChart.setSizeToMatchContainer();
                dataCountHistogramChart.redraw();
                dataCountPerAngleHistogramChart.setSizeToMatchContainer();
                dataCountPerAngleHistogramChart.redraw();
            }
        };
        histogramChartsWrapperPanel.addNorth(dataCountHistogramChart, 50);
        histogramChartsWrapperPanel.addSouth(dataCountPerAngleHistogramChart, 50);
        
        dockLayoutPanel = new DockLayoutPanel(Unit.PCT);
        dockLayoutPanel.addWest(polarChartWrapperPanel, 40);
        dockLayoutPanel.addEast(histogramChartsWrapperPanel, 60);
        
        setSeriesShowAndHideHandler();
    }

    private PointUnselectEventHandler createPointUnselectEventHandler() {
        return new PointUnselectEventHandler() {
            
            @Override
            public boolean onUnselect(PointUnselectEvent pointUnselectEvent) {
                long angle = pointUnselectEvent.getXAsLong();
                Series series = polarChart.getSeries(pointUnselectEvent.getSeriesId());
                Series seriesToHide = perAngleHistogramSeriesForAngle.get(series).get(angle);
                seriesToHide.setVisible(false, true);
                return true;
            }
        };
    }

    private PointSelectEventHandler createPointSelectEventHandler() {
        return new PointSelectEventHandler() {

            @Override
            public boolean onSelect(PointSelectEvent pointSelectEvent) {
                long angle = pointSelectEvent.getXAsLong();
                Series series = polarChart.getSeries(pointSelectEvent.getSeriesId());
                Series seriesToHide = perAngleHistogramSeriesForAngle.get(series).get(angle);
                seriesToHide.setVisible(true, true);
                return true;
            }
        };
    }

    private void setSeriesShowAndHideHandler() {
        SeriesPlotOptions seriesPlotOptions = new SeriesPlotOptions();
        seriesPlotOptions.setSeriesShowEventHandler(createSeriesShowEventHandler());
        seriesPlotOptions.setSeriesHideEventHandler(createSeriesHideEventHandler());
        seriesPlotOptions.setPointSelectEventHandler(createPointSelectEventHandler());
        seriesPlotOptions.setPointUnselectEventHandler(createPointUnselectEventHandler());
        seriesPlotOptions.setAllowPointSelect(true);
        polarChart.setSeriesPlotOptions(seriesPlotOptions );
    }

    private SeriesShowEventHandler createSeriesShowEventHandler() {
        return new SeriesShowEventHandler() {
            
            @Override
            public boolean onShow(SeriesShowEvent seriesShowEvent) {
                String id = seriesShowEvent.getSeriesId();
                Series shownSeries = polarChart.getSeries(id);
                Series histogramSeries = histogramSeriesForPolarSeries.get(shownSeries);
                histogramSeries.setVisible(true, true);
                return true;
            }
        };
    }
    
    private SeriesHideEventHandler createSeriesHideEventHandler() {
        return new SeriesHideEventHandler() {
            
            @Override
            public boolean onHide(SeriesHideEvent seriesHideEvent) {
                String id = seriesHideEvent.getSeriesId();
                Series hiddenSeries = polarChart.getSeries(id);
                Series histogramSeries = histogramSeriesForPolarSeries.get(hiddenSeries);
                histogramSeries.setVisible(false, true);
                return true;
            }
        };
    }

    private Chart createDataCountHistogramChart(String xAxisLabel, boolean showXLabels) {
        Chart histogramChart = new Chart().setType(Type.COLUMN).setHeight100().setWidth100();
        histogramChart.setTitle(new ChartTitle().setText(""), new ChartSubtitle().setText(""));
        histogramChart.getYAxis().setMin(0).setAxisTitle(new AxisTitle().setText(stringMessages.numberOfDataPoints()));
        histogramChart.getXAxis().setLabels(new XAxisLabels().setRotation(-90f).setY(30).setEnabled(showXLabels))
                .setAxisTitle(new AxisTitle().setText(xAxisLabel));
        histogramChart.setLegend(new Legend().setEnabled(false));
        histogramChart.setExporting(new Exporting().setEnabled(false));
        return histogramChart;
    }

    private Chart createPolarChart() {
        LinePlotOptions linePlotOptions = new LinePlotOptions().setLineWidth(1).setMarker(new Marker().setEnabled(false));
        Chart polarSheetChart = new Chart().setType(Series.Type.LINE)
                .setLinePlotOptions(linePlotOptions)
                .setPolar(true).setHeight100().setWidth100();
        polarSheetChart.setTitle(new ChartTitle().setText(""), new ChartSubtitle().setText(""));
        polarSheetChart.getYAxis().setMin(0);
        polarSheetChart.getXAxis().setMin(-179).setMax(180).setTickInterval(45);
        polarSheetChart.setOption("/pane/startAngle", 180);
        polarSheetChart.setExporting(new Exporting().setEnabled(false));
        return polarSheetChart;
    }

    @Override
    protected Widget getPresentationWidget() {
        return dockLayoutPanel;
    }

    @Override
    protected void internalShowResults(QueryResultDTO<?> result) {
        Map<GroupKey, ?> results = result.getResults();
        List<GroupKey> sortedNaturally = new ArrayList<GroupKey>(results.keySet());
        Collections.sort(sortedNaturally, new Comparator<GroupKey>() {
            @Override
            public int compare(GroupKey o1, GroupKey o2) {
                Comparator<String> naturalComparator = new NaturalComparator();
                return naturalComparator.compare(o1.asString(), o2.asString());
            }
        });
        for (GroupKey key : sortedNaturally) {
            PolarAggregation aggregation = (PolarAggregation) results.get(key);
            double[] speedsPerAngle = aggregation.getAverageSpeedsPerAngle();
            int count = aggregation.getCount();
            int[] countPerAngle = aggregation.getCountPerAngle();
            PolarDataMiningSettings settings = aggregation.getSettings();
            if (settings.getMinimumDataCountPerGraph() < count) {
                Series polarSeries = polarChart.createSeries();
                Series histogramSeries = dataCountHistogramChart.createSeries();
                Map<Integer, Map<Double, Integer>> histogramData = aggregation.getCountHistogramPerAngle();
                Map<Long, Series> seriesPerAngle = new HashMap<>();
                perAngleHistogramSeriesForAngle.put(polarSeries, seriesPerAngle);
                for (int i = 0; i < 360; i++) {
                    int convertedAngle = i > 180 ? i - 360 : i;
                    double speed = speedsPerAngle[i];
                    Point point = null;
                    if (countPerAngle[i] >= settings.getMinimumDataCountPerAngle() && speed > 0) {
                        point = new Point(convertedAngle, speed);
                        polarSeries.addPoint(point, false, false, false);
                    }  else {
                        polarSeries.addPoint(convertedAngle, 0, false, false, false);
                    }
                    histogramSeries.addPoint(convertedAngle, countPerAngle[i], false, false, false);
                    Series dataCountPerAngleSeries = dataCountPerAngleHistogramChart.createSeries();
                    Map<Double, Integer> histogramDataForAngle = histogramData.get(i);
                    for (Entry<Double, Integer> entry : histogramDataForAngle.entrySet()) {
                        dataCountPerAngleSeries.addPoint(entry.getKey(), entry.getValue());
                    }
                    dataCountPerAngleHistogramChart.addSeries(dataCountPerAngleSeries, false, false);
                    dataCountPerAngleSeries.setVisible(false, false);
                    if (point != null) {
                        seriesPerAngle.put((long) convertedAngle, dataCountPerAngleSeries);
                    }
                }
                polarSeries.setName(key.asString());
                histogramSeries.setName(key.asString());
                polarChart.addSeries(polarSeries, false, false);
                histogramSeries.setVisible(false, false);
                histogramSeriesForPolarSeries.put(polarSeries, histogramSeries);
                dataCountHistogramChart.addSeries(histogramSeries);
                for (Series seriesToHide : seriesPerAngle.values()) {
                    seriesToHide.setVisible(false, false);
                }
                dataCountPerAngleHistogramChart.redraw();
            }
        }
        //Initially resize the chart. Otherwise it's too big. FIXME with a better solution
        Timer timer = new Timer() {

            @Override
            public void run() {
                polarChart.setSizeToMatchContainer();
                dataCountHistogramChart.setSizeToMatchContainer();
                dataCountPerAngleHistogramChart.setSizeToMatchContainer();
            }
        };
        timer.schedule(200);
    }

    @Override
    public String getLocalizedShortName() {
        return getStringMessages().polarResultsPresenter();
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public SettingsDialogComponent<Settings> getSettingsDialogComponent() {
        return null;
    }

    @Override
    public void updateSettings(Settings newSettings) {
        // no-op
    }

    @Override
    public String getDependentCssClassName() {
        return "polarResultsPresenter";
    }

    @Override
    public Settings getSettings() {
        return null;
    }

    @Override
    public String getId() {
        return "PolarResultsPresenter";
    }
}
