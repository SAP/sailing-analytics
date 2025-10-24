package com.sap.sailing.gwt.ui.polarmining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Series;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.datamining.presentation.AbstractSailingResultsPresenter;
import com.sap.sailing.gwt.ui.datamining.presentation.ChartFactory;
import com.sap.sailing.polars.datamining.shared.PolarBackendData;
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
 * Is able to present {@link PolarBackendData}.</br>
 * Has one polar chart showing the perAngle regression data and two x-y-linecharts that show speed and angle over
 * windspeed regressions.
 * 
 * </br>
 * </br>
 * Used in conjunction with the datamining framework.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarBackendResultsPresenter extends AbstractSailingResultsPresenter<Settings> {

    private final DockLayoutPanel dockLayoutPanel;

    private final Chart polarChart;
    private final SimpleLayoutPanel polarChartWrapperPanel;

    private final Chart speedChart;
    private final Chart angleChart;
    private final DockLayoutPanel speedAndAngleChart;

    public PolarBackendResultsPresenter(Component<?> parent, ComponentContext<?> context,
            StringMessages stringMessages) {
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
        speedChart = ChartFactory.createSpeedChart(stringMessages);
        angleChart = ChartFactory.createAngleChart(stringMessages);
        speedAndAngleChart = new DockLayoutPanel(Unit.PCT) {
            @Override
            public void onResize() {
                speedChart.setSizeToMatchContainer();
                speedChart.redraw();
                angleChart.setSizeToMatchContainer();
                angleChart.redraw();
            }
        };
        speedAndAngleChart.addNorth(speedChart, 50);
        speedAndAngleChart.addSouth(angleChart, 50);
        dockLayoutPanel = new DockLayoutPanel(Unit.PCT);
        dockLayoutPanel.addWest(polarChartWrapperPanel, 40);
        dockLayoutPanel.addEast(speedAndAngleChart, 60);
        ChartToCsvExporter chartToCsvExporter = new ChartToCsvExporter(stringMessages.csvCopiedToClipboard());
        Button exportStatisticsCurveToCsvButton = new Button(stringMessages.exportStatisticsCurveToCsv(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        chartToCsvExporter.exportChartAsCsvToClipboard(polarChart);
                    }
                });
        addControl(exportStatisticsCurveToCsvButton);
    }

    @Override
    protected Widget getPresentationWidget() {
        return dockLayoutPanel;
    }

    @Override
    protected void internalShowResults(StatisticQueryDefinitionDTO queryDefinition, QueryResultDTO<?> result) {
        polarChart.removeAllSeries(false);
        speedChart.removeAllSeries(false);
        angleChart.removeAllSeries(false);
        final Set<Series> seriesToHideAfterRendering = new HashSet<>();
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
            PolarBackendData aggregation = (PolarBackendData) results.get(key);
            if (aggregation.hasUpwindSpeedData()) {
                Series upwindSpeedSeries = speedChart.createSeries();
                upwindSpeedSeries.setName(key.asString() + "-" + stringMessages.upWind());
                double[] upwindSpeedOverWindSpeed = aggregation.getUpwindSpeedOverWindSpeed();
                for (int i = 0; i < 30; i++) {
                    upwindSpeedSeries.addPoint(i, upwindSpeedOverWindSpeed[i], false, false, false);
                }
                speedChart.addSeries(upwindSpeedSeries, false, false);
            }
            if (aggregation.hasDownwindSpeedData()) {
                Series downwindSpeedSeries = speedChart.createSeries();
                downwindSpeedSeries.setName(key.asString() + "-" + stringMessages.downWind());
                double[] downwindSpeedOverWindSpeed = aggregation.getDownwindSpeedOverWindSpeed();
                for (int i = 0; i < 30; i++) {
                    downwindSpeedSeries.addPoint(i, downwindSpeedOverWindSpeed[i], false, false, false);

                }
                speedChart.addSeries(downwindSpeedSeries, false, false);
            }
            if (aggregation.hasUpwindAngleData()) {
                Series upwindAngleSeries = speedChart.createSeries();
                upwindAngleSeries.setName(key.asString() + "-" + stringMessages.upWind());
                double[] upwindAngleOverWindSpeed = aggregation.getUpwindAngleOverWindSpeed();
                for (int i = 0; i < 30; i++) {
                    upwindAngleSeries.addPoint(i, upwindAngleOverWindSpeed[i], false, false, false);
                }
                angleChart.addSeries(upwindAngleSeries, false, false);
            }
            if (aggregation.hasDownwindAngleData()) {
                Series downwindAngleSeries = speedChart.createSeries();
                downwindAngleSeries.setName(key.asString() + "-" + stringMessages.downWind());
                double[] downwindAngleOverWindSpeed = aggregation.getDownwindAngleOverWindSpeed();
                for (int i = 0; i < 30; i++) {
                    downwindAngleSeries.addPoint(i, downwindAngleOverWindSpeed[i], false, false, false);
                }
                angleChart.addSeries(downwindAngleSeries, false, false);
            }
            boolean[] hasDataForAngle = aggregation.getDataForAngleBooleanArray();
            for (int i = 5; i < 30; i = i + 3) {
                Series polarSeries = polarChart.createSeries();
                polarSeries.setName(key.asString() + "-" + i + "kn");
                double[][] data = aggregation.getPolarDataPerWindspeedAndAngle();
                // Ensure that the points are added with ascending x coordinates to prevent Highcharts error 15
                for (int convertedAngle = -179; convertedAngle <= 180; convertedAngle++) {
                    int j = convertedAngle < 0 ? convertedAngle + 360 : convertedAngle;
                    polarSeries.addPoint(convertedAngle, hasDataForAngle[j] ? data[j][i] : 0, false, false, false);
                }
                if (i != 11) {
                    seriesToHideAfterRendering.add(polarSeries);
                }
                polarChart.addSeries(polarSeries, false, false);
            }
        }
        // Initially resize the chart. Otherwise it's too big. FIXME with a better solution
        Scheduler.get().scheduleDeferred(()->{
                polarChart.setSizeToMatchContainer();
                speedChart.setSizeToMatchContainer();
                angleChart.setSizeToMatchContainer();
                for (Series seriesToHide : seriesToHideAfterRendering) {
                    seriesToHide.setVisible(false, false);
                }
                polarChart.redraw();
                angleChart.redraw();
                speedChart.redraw();
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
        return "PolarBackendResultsPresenter";
    }
}
