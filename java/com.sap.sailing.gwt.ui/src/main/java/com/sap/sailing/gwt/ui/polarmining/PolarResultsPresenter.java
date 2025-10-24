package com.sap.sailing.gwt.ui.polarmining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.events.PointSelectEvent;
import org.moxieapps.gwt.highcharts.client.events.PointSelectEventHandler;
import org.moxieapps.gwt.highcharts.client.events.PointUnselectEvent;
import org.moxieapps.gwt.highcharts.client.events.PointUnselectEventHandler;
import org.moxieapps.gwt.highcharts.client.events.SeriesHideEvent;
import org.moxieapps.gwt.highcharts.client.events.SeriesHideEventHandler;
import org.moxieapps.gwt.highcharts.client.events.SeriesShowEvent;
import org.moxieapps.gwt.highcharts.client.events.SeriesShowEventHandler;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.datamining.presentation.AbstractSailingResultsPresenter;
import com.sap.sailing.gwt.ui.datamining.presentation.ChartFactory;
import com.sap.sailing.polars.datamining.shared.PolarAggregation;
import com.sap.sailing.polars.datamining.shared.PolarDataMiningSettings;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;
import com.sap.sse.datamining.ui.client.ChartToCsvExporter;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

/**
 * Allows presentation of {@link PolarAggregation} data.
 * 
 * </br>
 * </br>
 * 
 * Contains a polar chart on the left displaying the actual polar diagram and two histograms on the right. The upper
 * histogram shows datacount over angle and the second one shows datacount over windrange upon clicking a point in the
 * polar chart.
 * 
 * </br>
 * </br>
 * 
 * Used in conjunction with the datamining framework.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarResultsPresenter extends AbstractSailingResultsPresenter<Settings> {

    private final DockLayoutPanel dockLayoutPanel;

    private final Chart polarChart;
    private final SimpleLayoutPanel polarChartWrapperPanel;

    private final Chart dataCountHistogramChart;
    private final Chart dataCountPerAngleHistogramChart;
    private final DockLayoutPanel histogramChartsWrapperPanel;

    private final Map<Series, Series> histogramSeriesForPolarSeries = new HashMap<>();
    private final Map<Series, Map<Long, Series>> perAngleHistogramSeriesForAngle = new HashMap<>();

    public PolarResultsPresenter(Component<?> parent, ComponentContext<?> context, StringMessages stringMessages) {
        super(parent, context, stringMessages);
        polarChart = ChartFactory.createPolarChart();
        polarChart.getYAxis().setMin(0);
        polarChartWrapperPanel = new SimpleLayoutPanel() {
            @Override
            public void onResize() {
                polarChart.setSizeToMatchContainer();
                polarChart.redraw();
            }
        };
        polarChartWrapperPanel.add(polarChart);
        dataCountHistogramChart = ChartFactory.createDataCountHistogramChart(
                stringMessages.TWA() + " (" + stringMessages.degreesShort() + ")", stringMessages);
        dataCountPerAngleHistogramChart = ChartFactory.createDataCountHistogramChart(stringMessages.windSpeed(), stringMessages);
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
        final ChartToCsvExporter chartToCsvExporter = new ChartToCsvExporter(stringMessages.csvCopiedToClipboard());
        final Button exportStatisticsCurveToCsvButton = new Button(stringMessages.exportStatisticsCurveToCsv(),
                (ClickHandler) e->chartToCsvExporter.exportChartAsCsvToClipboard(polarChart));
        addControl(exportStatisticsCurveToCsvButton);
        final Button exportTWAHistogramToCsvButton = new Button(stringMessages.exportTWAHistogramToCsv(),
                (ClickHandler) e->chartToCsvExporter.exportChartAsCsvToClipboard(dataCountHistogramChart));
        addControl(exportTWAHistogramToCsvButton);
        final Button exportWindSpeedHistogramToCsvButton = new Button(stringMessages.exportWindSpeedHistogramToCsv(),
                (ClickHandler) e->chartToCsvExporter.exportChartAsCsvToClipboard(dataCountPerAngleHistogramChart));
        addControl(exportWindSpeedHistogramToCsvButton);
        setSeriesShowAndHideHandler();
    }

    private void setSeriesShowAndHideHandler() {
        SeriesPlotOptions seriesPlotOptions = new SeriesPlotOptions();
        seriesPlotOptions.setSeriesShowEventHandler(createSeriesShowEventHandler());
        seriesPlotOptions.setSeriesHideEventHandler(createSeriesHideEventHandler());
        seriesPlotOptions.setPointSelectEventHandler(createPointSelectEventHandler());
        seriesPlotOptions.setPointUnselectEventHandler(createPointUnselectEventHandler());
        seriesPlotOptions.setAllowPointSelect(true);
        polarChart.setSeriesPlotOptions(seriesPlotOptions);
    }

    private PointUnselectEventHandler createPointUnselectEventHandler() {
        return new PointUnselectEventHandler() {
            @Override
            public boolean onUnselect(PointUnselectEvent pointUnselectEvent) {
                long angle = pointUnselectEvent.getXAsLong();
                Series series = polarChart.getSeries(pointUnselectEvent.getSeriesId());
                Series seriesToHide = perAngleHistogramSeriesForAngle.get(series).get(angle);
                if (seriesToHide != null) {
                    seriesToHide.setVisible(false, true);
                }
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
                Series seriesToShow = perAngleHistogramSeriesForAngle.get(series).get(angle);
                if (seriesToShow != null) {
                    seriesToShow.setVisible(true, true);
                }
                return true;
            }
        };
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

    @Override
    protected Widget getPresentationWidget() {
        return dockLayoutPanel;
    }

    @Override
    protected void internalShowResults(StatisticQueryDefinitionDTO queryDefinition, QueryResultDTO<?> result) {
        polarChart.removeAllSeries(false);
        dataCountHistogramChart.removeAllSeries(false);
        dataCountPerAngleHistogramChart.removeAllSeries(false);
        histogramSeriesForPolarSeries.clear();
        perAngleHistogramSeriesForAngle.clear();
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
            if (settings.getMinimumDataCountPerGraph() <= count) {
                Series polarSeries = polarChart.createSeries();
                Series histogramSeries = dataCountHistogramChart.createSeries();
                Map<Integer, Map<Double, Integer>> histogramData = aggregation.getCountHistogramPerAngle();
                Map<Long, Series> seriesPerAngle = new HashMap<>();
                perAngleHistogramSeriesForAngle.put(polarSeries, seriesPerAngle);
                // Ensure that the points are added with ascending x coordinates to prevent Highcharts error 15
                for (int convertedAngle = -179; convertedAngle <= 180; convertedAngle++) {
                    int index = convertedAngle < 0 ? convertedAngle + 360 : convertedAngle;
                    double speed = speedsPerAngle[index];
                    Point point = null;
                    if (countPerAngle[index] >= settings.getMinimumDataCountPerAngle() && speed != 0) {
                        point = new Point(convertedAngle, speed);
                        polarSeries.addPoint(point, false, false, false);
                    } else {
                        polarSeries.addPoint(convertedAngle, 0, false, false, false);
                    }
                    histogramSeries.addPoint(convertedAngle, countPerAngle[index], false, false, false);
                    if (point != null) {
                        Map<Double, Integer> histogramDataForAngle = histogramData.get(index);
                        Series dataCountPerAngleSeries = dataCountPerAngleHistogramChart.createSeries();
                        dataCountPerAngleSeries.setName(key.asString() + " - " + convertedAngle + stringMessages.degreesShort());
                        // Iterating over the histogram data without sorting the x coordinates ascending leads
                        // to a massive occurrence of Highcharts error 15, freezing the complete UI
                        List<Double> sortedAngles = new ArrayList<>(histogramDataForAngle.keySet());
                        Collections.sort(sortedAngles);
                        for (Double angle : sortedAngles) {
                            dataCountPerAngleSeries.addPoint(angle, histogramDataForAngle.get(angle));
                        }
                        dataCountPerAngleHistogramChart.addSeries(dataCountPerAngleSeries, false, false);
                        dataCountPerAngleSeries.setVisible(false, false);
                        seriesPerAngle.put((long) convertedAngle, dataCountPerAngleSeries);
                    }
                }
                polarSeries.setName(key.asString());
                histogramSeries.setName(key.asString());
                histogramSeries.setVisible(false, false);
                histogramSeriesForPolarSeries.put(polarSeries, histogramSeries);
                polarChart.addSeries(polarSeries, false, false);
                dataCountHistogramChart.addSeries(histogramSeries);
                for (Series seriesToHide : seriesPerAngle.values()) {
                    seriesToHide.setVisible(false, false);
                }
            }
        }
        // Initially resize the chart. Otherwise it's too big. FIXME with a better solution
        Scheduler.get().scheduleDeferred(()->{
                polarChart.setSizeToMatchContainer();
                dataCountHistogramChart.setSizeToMatchContainer();
                dataCountPerAngleHistogramChart.setSizeToMatchContainer();
                dataCountPerAngleHistogramChart.redraw();
            });
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.polarResultsPresenter();
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    @Override
    public SettingsDialogComponent<Settings> getSettingsDialogComponent(Settings settings) {
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
