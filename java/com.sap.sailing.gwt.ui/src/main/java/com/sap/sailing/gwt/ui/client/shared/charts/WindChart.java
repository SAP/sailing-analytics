package com.sap.sailing.gwt.ui.client.shared.charts;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.moxieapps.gwt.highcharts.client.Style;
import org.moxieapps.gwt.highcharts.client.ToolTip;
import org.moxieapps.gwt.highcharts.client.ToolTipData;
import org.moxieapps.gwt.highcharts.client.ToolTipFormatter;
import org.moxieapps.gwt.highcharts.client.events.ChartClickEvent;
import org.moxieapps.gwt.highcharts.client.events.ChartClickEventHandler;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEvent;
import org.moxieapps.gwt.highcharts.client.events.ChartSelectionEventHandler;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.PlotLineLabel;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;
import org.moxieapps.gwt.highcharts.client.labels.YAxisLabels;
import org.moxieapps.gwt.highcharts.client.plotOptions.LinePlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.text.client.DateTimeFormatRenderer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.ColorMapImpl;
import com.sap.sailing.gwt.ui.actions.GetWindInfoAction;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.WindSourceTypeFormatter;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.DoublePair;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;
import com.sap.sse.common.scalablevalue.impl.ScalableSpeed;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.TimeRangeWithZoomProvider;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

public class WindChart extends AbstractRaceChart<WindChartSettings> implements RequiresResize {
    public static final String LOAD_WIND_CHART_DATA_CATEGORY = "loadWindChartData";
    
    private static final int LINE_WIDTH = 1;

    private final WindChartSettings settings;
    private final WindChartLifecycle windChartLifecycle;
    
    /**
     * Holds one series for each wind source for which data has been received.
     */
    private final Map<WindSource, Series> windSourceDirectionSeries;
    private final Map<WindSource, Series> windSourceSpeedSeries;
    private final Map<WindSource, Point[]> windSourceDirectionPoints;
    private final Map<WindSource, Point[]> windSourceSpeedPoints;
    private Point firstPointOfFirstSeries;
    private final Map<WindSource, PlotLine> directionAvgPlotLines = new HashMap<WindSource, PlotLine>();
    private final Map<WindSource, PlotLine> directionMinPlotLines = new HashMap<WindSource, PlotLine>();
    private final Map<WindSource, PlotLine> directionMaxPlotLines = new HashMap<WindSource, PlotLine>();
    private final Map<WindSource, PlotLine> speedAvgPlotLines = new HashMap<WindSource, PlotLine>();
    private final Map<WindSource, PlotLine> speedMinPlotLines = new HashMap<WindSource, PlotLine>();
    private final Map<WindSource, PlotLine> speedMaxPlotLines = new HashMap<WindSource, PlotLine>();
    private final Map<WindSource, DirectionStatAccumulator> directionAccumulators = new HashMap<WindSource, DirectionStatAccumulator>();
    private final Map<WindSource, SpeedStatAccumulator> speedAccumulators = new HashMap<WindSource, SpeedStatAccumulator>();
    
    private Long timeOfEarliestRequestInMillis;
    private Long timeOfLatestRequestInMillis;

    private final ColorMapImpl<WindSource> colorMap;

    private WindSource preselectFilter;

    /**
     * @param selectedRaceIdentifier
     *            if <code>null</code>, this chart won't update its contents automatically upon race selection change;
     *            otherwise, whenever the selection changes, the wind data of the race selected now is loaded from the
     *            server and displayed in this chart. If no race is selected, the chart is cleared. The caller of this
     *            constructor must ensure to trigger {@link RaceSelectionChangeListener#onRaceSelectionChange(List)} at
     *            least once to ensure that this chart sets its {@link AbstractRaceChart#selectedRaceIdentifier} field.
     */
    public WindChart(Component<?> parent, ComponentContext<?> context, WindChartLifecycle windChartLifecycle,
            SailingServiceAsync sailingService,
            RegattaAndRaceIdentifier selectedRaceIdentifier, Timer timer,
            TimeRangeWithZoomProvider timeRangeWithZoomProvider, WindChartSettings settings, final StringMessages stringMessages, 
            AsyncActionsExecutor asyncActionsExecutor, ErrorReporter errorReporter, boolean compactChart) {
        super(parent, context, sailingService, selectedRaceIdentifier, timer, timeRangeWithZoomProvider, stringMessages,
                asyncActionsExecutor, errorReporter);
        this.windChartLifecycle = windChartLifecycle;
        this.settings = settings;
        windSourceDirectionSeries = new HashMap<WindSource, Series>();
        windSourceSpeedSeries = new HashMap<WindSource, Series>();
        windSourceDirectionPoints = new HashMap<WindSource, Point[]>();
        windSourceSpeedPoints = new HashMap<WindSource, Point[]>();
        firstPointOfFirstSeries = null;
        colorMap = new ColorMapImpl<WindSource>();
        chart = new Chart()
                .setPersistent(true)
                .setReflow(false)
                .setZoomType(BaseChart.ZoomType.X)
                .setMarginLeft(65)
                .setMarginRight(65)
                .setWidth100()
                .setHeight100()
                .setBorderColor(new Color("#CACACA"))
                .setBorderWidth(0)
                .setBorderRadius(0)
                .setBackgroundColor(new Color("#FFFFFF"))
                .setPlotBackgroundColor("#f8f8f8")
                .setPlotBorderWidth(0)
                .setAlignTicks(false)
                .setCredits(new Credits().setEnabled(false))
                .setChartTitle(new ChartTitle().setText(stringMessages.wind()).setOption("floating",true))
                .setChartSubtitle(new ChartSubtitle().setText(stringMessages.clickAndDragToZoomIn()))
                .setLinePlotOptions(new LinePlotOptions().setLineWidth(LINE_WIDTH).setMarker(
                        new Marker().setEnabled(false).setHoverState(
                                new Marker().setEnabled(true).setRadius(4))).setShadow(false)
                                    .setHoverStateLineWidth(LINE_WIDTH));
        chart.setStyleName(chartsCss.chartStyle());
        ChartUtil.useCheckboxesToShowAndHide(chart, this::updateStatPlotLines);
        final NumberFormat numberFormat = NumberFormat.getFormat("0.0");
        chart.setToolTip(new ToolTip().setEnabled(true).setFormatter(new ToolTipFormatter() {
            @Override
            public String format(ToolTipData toolTipData) {
                final String result;
                final String seriesName = toolTipData.getSeriesName();
                if (seriesName.equals(WindChart.this.stringMessages.time())) {
                    result = "<b>" + seriesName + ":</b> " + dateFormat.format(new Date(toolTipData.getXAsLong()))
                            + "<br/>(" + stringMessages.clickChartToSetTime() + ")";
                } else if (seriesName.startsWith(stringMessages.fromDeg()+" ")) {
                    double value = toolTipData.getYAsDouble() % 360;
                    result = "<b>" + seriesName + (toolTipData.getPointName() != null ? " "+toolTipData.getPointName() : "")
                            + "</b><br/>" +  
                            dateFormat.format(new Date(toolTipData.getXAsLong())) + ": " +
                            numberFormat.format(value < 0 ? value + 360 : value) + stringMessages.degreesShort();
                } else {
                    result = "<b>" + seriesName + (toolTipData.getPointName() != null ? " "+toolTipData.getPointName() : "")
                            + "</b><br/>" +  
                            dateFormat.format(new Date(toolTipData.getXAsLong())) + ": " +
                            numberFormat.format(toolTipData.getYAsDouble()) + stringMessages.knotsUnit();
                }
                return result;
            }
        }));
        
        chart.setClickEventHandler(new ChartClickEventHandler() {
            @Override
            public boolean onClick(ChartClickEvent chartClickEvent) {
                return WindChart.this.onClick(chartClickEvent);
            }
        });
       
        chart.setSelectionEventHandler(new ChartSelectionEventHandler() {
            @Override
            public boolean onSelection(ChartSelectionEvent chartSelectionEvent) {
                return WindChart.this.onXAxisSelectionChange(chartSelectionEvent);
            }
        });

        chart.getXAxis().setType(Axis.Type.DATE_TIME)
                        .setMaxZoom(60 * 1000) // 1 minute
                        .setAxisTitleText(stringMessages.time());
        chart.getXAxis().setLabels(new XAxisLabels().setFormatter(new AxisLabelsFormatter() {
            @Override
            public String format(AxisLabelsData axisLabelsData) {
                return dateFormatHoursMinutes.format(new Date(axisLabelsData.getValueAsLong()));
            }
        }));
        timePlotLine = chart.getXAxis().createPlotLine().setColor("#656565").setWidth(1.5).setDashStyle(DashStyle.SOLID);
        chart.getYAxis(0).setAxisTitleText(stringMessages.fromDeg()).setStartOnTick(false)
                .setLabels(new YAxisLabels().setFormatter(new AxisLabelsFormatter() {
                    @Override
                    public String format(AxisLabelsData axisLabelsData) {
                        final double directionInDegreesYAxis = axisLabelsData.getValueAsDouble();
                        final double normalized = normalizeYAxisDirectionValue(directionInDegreesYAxis);
                        return Long.valueOf(Double.valueOf(normalized).longValue()).toString();
                    }
                }));
        chart.getYAxis(1).setOpposite(true).setAxisTitleText(stringMessages.speed()+" ("+stringMessages.knotsUnit()+")").setMin(0)
            .setMaxPadding(0.05).setStartOnTick(false).setGridLineWidth(0).setMinorGridLineWidth(0);
        if (compactChart) {
            chart.setSpacingBottom(10).setSpacingLeft(10).setSpacingRight(10).setSpacingTop(20)
                 .setOption("legend/margin", 2)
                 .setOption("title/margin", 5)
                 .setChartSubtitle(null)
                 .getXAxis().setAxisTitle(null);
        }
        setSize("100%", "100%");
        if (selectedRaceIdentifier != null) {
            clearCacheAndReload();
            if (isVisible()) {
                updateVisibleSeries();
            }
        } else {
            clearChart();
        }
    }

    private static double normalizeYAxisDirectionValue(final double directionInDegreesYAxis) {
        double value = directionInDegreesYAxis % 360;
        final double normalized = value < 0 ? value + 360 : value;
        return normalized;
    }
    
    @Override
    protected Button createSettingsButton() {
        Button settingsButton = SettingsDialog.createSettingsButton(this, stringMessages);
        return settingsButton;
    }

    @Override
    public String getLocalizedShortName() {
        return windChartLifecycle.getLocalizedShortName();
    }

    @Override
    public Widget getEntryWidget() {
        return this;
    }

    private void updateVisibleSeries() {
        final Set<Series> visibleSeries = new HashSet<Series>(Arrays.asList(chart.getSeries()));
        if (preselectFilter != null) {
            forceSeriesSelection(visibleSeries, windSourceDirectionSeries);
            forceSeriesSelection(visibleSeries, windSourceSpeedSeries);
        } else {
            final boolean showDirectionSeries = settings.isShowWindDirectionsSeries();
            final Set<WindSourceType> directionSourceTypesToDisplay = settings.getWindDirectionSourcesToDisplay();
            updateSeries(visibleSeries, windSourceDirectionSeries, showDirectionSeries, directionSourceTypesToDisplay);
            final boolean showSpeedSeries = settings.isShowWindSpeedSeries();
            final Set<WindSourceType> speedSourceTypesToDisplay = settings.getWindSpeedSourcesToDisplay();
            updateSeries(visibleSeries, windSourceSpeedSeries, showSpeedSeries, speedSourceTypesToDisplay);
        }
        onResize();
    }

    private void updateSeries(final Set<Series> visibleSeries, final Map<WindSource, Series> windSourceToSeriesMap,
            final boolean showSeries, final Set<WindSourceType> windSourceTypesToDisplay) {
        if (showSeries) {
            for (Entry<WindSource, Series> entry : windSourceToSeriesMap.entrySet()) {
                final Series series = entry.getValue();
                if (windSourceTypesToDisplay.contains(entry.getKey().getType())) {
                    if (!visibleSeries.contains(series)) {
                        chart.addSeries(series, false, false);
                    }
                } else if (visibleSeries.contains(series)) {
                    chart.removeSeries(series, false);
                }
            }
        } else {
            for (Entry<WindSource, Series> entry : windSourceToSeriesMap.entrySet()) {
                final Series series = entry.getValue();
                if (visibleSeries.contains(series)) {
                    chart.removeSeries(series, false);
                }
            }
        }
    }

    private boolean forceSeriesSelection(Set<Series> visibleSeries, Map<WindSource, Series> toProcess) {
        boolean wasInResult = false;
        for (Map.Entry<WindSource, Series> e : toProcess.entrySet()) {
            Series series = e.getValue();
            WindSource seriesSource = e.getKey();
            // add all of type, remove non matching ones
            if (seriesSource.getType().equals(preselectFilter.getType())) {
                if (!visibleSeries.contains(series)) {
                    chart.addSeries(series, true, false);
                }
                // preselet the matching one
                if (preselectFilter.equals(e.getKey())) {
                    wasInResult = true;
                    series.select(true); // ensures that the checkbox will be ticked
                    series.setVisible(true, true);
                } else {
                    series.select(false);
                    series.setVisible(false, true);
                }
            } else {
                if (visibleSeries.contains(series)) {
                    chart.removeSeries(series, false);
                }
            }
        }
        return wasInResult;
    }

    /**
     * Creates the series for the <code>windSource</code> specified. If the series is created and needs to be visible
     * based on the {@link #windDirectionSourcesToDisplay}, it is added to the chart.
     */
    private Series getOrCreateSpeedSeries(WindSource windSource) {
        Series result = windSourceSpeedSeries.get(windSource);
        if (result == null) {
            result = createSpeedSeries(windSource);
            windSourceSpeedSeries.put(windSource, result);
        }
        return result;
    }

    /**
     * Creates the series for the <code>windSource</code> specified. If the series is created and needs to be visible
     * based on the {@link #windDirectionSourcesToDisplay}, it is added to the chart.
     */
    private Series getOrCreateDirectionSeries(WindSource windSource) {
        Series result = windSourceDirectionSeries.get(windSource);
        if (result == null) {
            result = createDirectionSeries(windSource);
            windSourceDirectionSeries.put(windSource, result);
        }
        return result;
    }


    /**
     * Only creates the series but doesn't add it to the chart. See also {@link #getOrCreateDirectionSeries(WindSource)} and
     * {@link #updateVisibleSeries()}
     */
    private Series createDirectionSeries(WindSource windSource) {
        Series newSeries = chart
                .createSeries()
                .setType(Series.Type.LINE)
                .setName(stringMessages.fromDeg()+" "+WindSourceTypeFormatter.format(windSource, stringMessages))
                .setYAxis(0)
                .setOption("turboThreshold", MAX_SERIES_POINTS)
                .setPlotOptions(new LinePlotOptions().setColor(colorMap.getColorByID(windSource).getAsHtml()).setSelected(true));
        return newSeries;
    }

    /**
     * Only creates the series but doesn't add it to the chart. See also {@link #getOrCreateSpeedSeries(WindSource)} and
     * {@link #updateVisibleSeries()}
     */
    private Series createSpeedSeries(WindSource windSource) {
        Series newSeries = chart
                .createSeries()
                .setType(Series.Type.LINE)
                .setName(stringMessages.windSpeed()+" "+WindSourceTypeFormatter.format(windSource, stringMessages))
                .setYAxis(1) // use the second Y-axis
                .setOption("turboThreshold", MAX_SERIES_POINTS)
                .setPlotOptions(new LinePlotOptions().setDashStyle(PlotLine.DashStyle.SHORT_DOT)
                        .setLineWidth(3).setHoverStateLineWidth(3)
                        .setColor(colorMap.getColorByID(windSource).getAsHtml()).setSelected(true)); // show only the markers, not the connecting lines
        return newSeries;
    }

    /**
     * Updates the wind charts with the wind data from <code>result</code>. If <code>append</code> is <code>true</code>, previously
     * existing points in the chart are left unchanged. Otherwise, the existing wind series are replaced.
     */
    public void updateChartSeries(WindInfoForRaceDTO result, boolean append) {
        Long newMinTimepoint = timeOfEarliestRequestInMillis;
        Long newMaxTimepoint = timeOfLatestRequestInMillis;
        for (final WindSource windSource: result.windTrackInfoByWindSource.keySet()) {
            final WindTrackInfoDTO windTrackInfo = result.windTrackInfoByWindSource.get(windSource);
            final Series directionSeries = getOrCreateDirectionSeries(windSource);
            final Series speedSeries = windSource.getType().useSpeed() ? getOrCreateSpeedSeries(windSource) : null;
            Point previousDirectionPoint = null;
            if (append && windSourceDirectionPoints.get(windSource) != null
                       && windSourceDirectionPoints.get(windSource).length != 0) {
                previousDirectionPoint = windSourceDirectionPoints.get(windSource)[windSourceDirectionPoints.get(windSource).length - 1];
            }
            final Point[] directionPoints = new Point[windTrackInfo.windFixes.size()];
            final Point[] speedPoints = new Point[windTrackInfo.windFixes.size()];
            int currentPointIndex = 0;
            if (!append) {
                directionAccumulators.put(windSource, new DirectionStatAccumulator());
                if (windSource.getType().useSpeed()) {
                    speedAccumulators.put(windSource, new SpeedStatAccumulator());
                }
            }
            final DirectionStatAccumulator dirAccumulator = directionAccumulators.computeIfAbsent(windSource, s -> new DirectionStatAccumulator());
            final SpeedStatAccumulator spdAccumulator = windSource.getType().useSpeed() ? speedAccumulators.computeIfAbsent(windSource, s -> new SpeedStatAccumulator()) : null;
            for (final WindDTO wind : windTrackInfo.windFixes) {
                if (newMinTimepoint == null || wind.requestTimepoint < newMinTimepoint) {
                    newMinTimepoint = wind.requestTimepoint;
                }
                if (newMaxTimepoint == null || wind.requestTimepoint > newMaxTimepoint) {
                    newMaxTimepoint = wind.requestTimepoint;
                }
                // if we are in non appending mode, the data is the truth, use all of it without filtering
                if (!append || ((timeOfEarliestRequestInMillis == null || wind.requestTimepoint < timeOfEarliestRequestInMillis) || 
                    timeOfLatestRequestInMillis == null || wind.requestTimepoint > timeOfLatestRequestInMillis)) {
                    Point newDirectionPoint = new Point(wind.requestTimepoint, wind.dampenedTrueWindFromDeg);
                    if (previousDirectionPoint != null) {
                        newDirectionPoint = ChartPointRecalculator.stayClosestToPreviousPoint(previousDirectionPoint,
                                newDirectionPoint);
                    } else if (firstPointOfFirstSeries != null && windSourceDirectionPoints.get(windSource) == null) {
                        //This Point is the first point of a new series
                        newDirectionPoint = ChartPointRecalculator.stayClosestToPreviousPoint(firstPointOfFirstSeries, newDirectionPoint);
                    }
                    directionPoints[currentPointIndex] = newDirectionPoint;
                    previousDirectionPoint = newDirectionPoint;
                    if (wind.dampenedTrueWindFromDeg != null) {
                        dirAccumulator.add(newDirectionPoint.getY().doubleValue());
                    }
                    final Point newSpeedPoint = new Point(wind.requestTimepoint, wind.dampenedTrueWindSpeedInKnots);
                    speedPoints[currentPointIndex++] = newSpeedPoint;
                    if (spdAccumulator != null && wind.dampenedTrueWindSpeedInKnots != null) {
                        spdAccumulator.add(wind.dampenedTrueWindSpeedInKnots);
                    }
                }
            }
            Point[] newDirectionPoints;
            Point[] newSpeedPoints = null;
            if (append) {
                Point[] oldDirectionPoints = windSourceDirectionPoints.get(windSource) != null ? windSourceDirectionPoints.get(windSource) : new Point[0];
                newDirectionPoints = new Point[oldDirectionPoints.length + currentPointIndex];
                System.arraycopy(oldDirectionPoints, 0, newDirectionPoints, 0, oldDirectionPoints.length);
                System.arraycopy(directionPoints, 0, newDirectionPoints, oldDirectionPoints.length, currentPointIndex);
                if (windSource.getType().useSpeed()) {
                    Point[] oldSpeedPoints =  windSourceSpeedPoints.get(windSource) != null ? windSourceSpeedPoints.get(windSource) : new Point[0];
                    newSpeedPoints = new Point[oldSpeedPoints.length + currentPointIndex];
                    System.arraycopy(oldSpeedPoints, 0, newSpeedPoints, 0, oldSpeedPoints.length);
                    System.arraycopy(speedPoints, 0, newSpeedPoints, oldSpeedPoints.length, currentPointIndex);
                }
            } else {
                newDirectionPoints = directionPoints;
                newSpeedPoints = speedPoints;
            }
            setSeriesPoints(directionSeries, newDirectionPoints, /* manageZoom */ true);
            windSourceDirectionPoints.put(windSource, newDirectionPoints);
            if (windSource.getType().useSpeed()) {
                setSeriesPoints(speedSeries, newSpeedPoints, /* manageZoom */ true);
                windSourceSpeedPoints.put(windSource, newSpeedPoints);
            }
            if (firstPointOfFirstSeries == null && newDirectionPoints.length != 0) { //If firstPointOfFirstSeries is null, than this series is the first
                firstPointOfFirstSeries = newDirectionPoints[0];
            }
        }
        
        timeOfEarliestRequestInMillis = newMinTimepoint;
        timeOfLatestRequestInMillis = newMaxTimepoint;
    }

    @Override
    public boolean hasSettings() {
        return windChartLifecycle.hasSettings();
    }

    @Override
    public SettingsDialogComponent<WindChartSettings> getSettingsDialogComponent(WindChartSettings settings) {
        return windChartLifecycle.getSettingsDialogComponent(settings);
    }

    /**
     * Sets the visibilities of the wind source series based on the new settings. Note that this does not
     * re-load any wind data. This has to happen by calling {@link #updateChartSeries(WindInfoForRaceDTO, boolean)}.
     */
    @Override
    public void updateSettings(WindChartSettings newSettings) {
        preselectFilter = null;
        boolean clearCacheAndReload = false;
        final Set<String> oldWindSourceTypesToRequest = getNamesOfWindSourceTypesOfWhichToDisplaySpeedOrDirection();
        if (newSettings.getResolutionInMilliseconds() != settings.getResolutionInMilliseconds()) {
            settings.setResolutionInMilliseconds(newSettings.getResolutionInMilliseconds());
            clearCacheAndReload = true;
        }
        settings.setShowWindDirectionsSeries(newSettings.isShowWindDirectionsSeries());
        settings.setWindDirectionSourcesToDisplay(newSettings.getWindDirectionSourcesToDisplay());
        settings.setShowWindSpeedSeries(newSettings.isShowWindSpeedSeries());
        settings.setWindSpeedSourcesToDisplay(newSettings.getWindSpeedSourcesToDisplay());
        if (!oldWindSourceTypesToRequest.equals(getNamesOfWindSourceTypesOfWhichToDisplaySpeedOrDirection())) {
            clearCacheAndReload = true;
        }
        final boolean dirAvgChanged = !settings.getDirectionAvgSources().equals(newSettings.getDirectionAvgSources());
        final boolean dirMinChanged = !settings.getDirectionMinSources().equals(newSettings.getDirectionMinSources());
        final boolean dirMaxChanged = !settings.getDirectionMaxSources().equals(newSettings.getDirectionMaxSources());
        final boolean spdAvgChanged = !settings.getSpeedAvgSources().equals(newSettings.getSpeedAvgSources());
        final boolean spdMinChanged = !settings.getSpeedMinSources().equals(newSettings.getSpeedMinSources());
        final boolean spdMaxChanged = !settings.getSpeedMaxSources().equals(newSettings.getSpeedMaxSources());
        settings.setDirectionAvgSources(newSettings.getDirectionAvgSources());
        settings.setDirectionMinSources(newSettings.getDirectionMinSources());
        settings.setDirectionMaxSources(newSettings.getDirectionMaxSources());
        settings.setDirectionAvgBulk(newSettings.isDirectionAvgBulk());
        settings.setDirectionMinBulk(newSettings.isDirectionMinBulk());
        settings.setDirectionMaxBulk(newSettings.isDirectionMaxBulk());
        settings.setSpeedAvgSources(newSettings.getSpeedAvgSources());
        settings.setSpeedMinSources(newSettings.getSpeedMinSources());
        settings.setSpeedMaxSources(newSettings.getSpeedMaxSources());
        settings.setSpeedAvgBulk(newSettings.isSpeedAvgBulk());
        settings.setSpeedMinBulk(newSettings.isSpeedMinBulk());
        settings.setSpeedMaxBulk(newSettings.isSpeedMaxBulk());
        if (clearCacheAndReload) {
            clearCacheAndReload();
        }
        updateVisibleSeries();
        if (!clearCacheAndReload) {
            if (dirAvgChanged) {
                updateStatPlotLinesForStat(windSourceDirectionPoints, windSourceDirectionSeries, 0,
                        settings.getDirectionAvgSources(), directionAvgPlotLines, directionAccumulators, StatKind.AVG, /* isDirection */ true);
            }
            if (dirMinChanged) {
                updateStatPlotLinesForStat(windSourceDirectionPoints, windSourceDirectionSeries, 0,
                        settings.getDirectionMinSources(), directionMinPlotLines, directionAccumulators, StatKind.MIN, /* isDirection */ true);
            }
            if (dirMaxChanged) {
                updateStatPlotLinesForStat(windSourceDirectionPoints, windSourceDirectionSeries, 0,
                        settings.getDirectionMaxSources(), directionMaxPlotLines, directionAccumulators, StatKind.MAX, /* isDirection */ true);
            }
            if (spdAvgChanged) {
                updateStatPlotLinesForStat(windSourceSpeedPoints, windSourceSpeedSeries, 1,
                        settings.getSpeedAvgSources(), speedAvgPlotLines, speedAccumulators, StatKind.AVG, /* isDirection */ false);
            }
            if (spdMinChanged) {
                updateStatPlotLinesForStat(windSourceSpeedPoints, windSourceSpeedSeries, 1,
                        settings.getSpeedMinSources(), speedMinPlotLines, speedAccumulators, StatKind.MIN, /* isDirection */ false);
            }
            if (spdMaxChanged) {
                updateStatPlotLinesForStat(windSourceSpeedPoints, windSourceSpeedSeries, 1,
                        settings.getSpeedMaxSources(), speedMaxPlotLines, speedAccumulators, StatKind.MAX, /* isDirection */ false);
            }
        }
    }

    private void clearCacheAndReload() {
        timeOfEarliestRequestInMillis = null;
        timeOfLatestRequestInMillis = null;
        windSourceDirectionPoints.clear();
        windSourceSpeedPoints.clear();
        directionAccumulators.clear();
        speedAccumulators.clear();
        firstPointOfFirstSeries = null;
        loadData(timeRangeWithZoomProvider.getFromTime(), timeRangeWithZoomProvider.getToTime(), /* append */false);
    }

    /**
     * @param append
     *            if <code>true</code>, the results retrieved from the server will be appended to the wind chart instead
     *            of overwriting the existing series.
     */
    private void loadData(final Date from, final Date to, final boolean append) {
        if (isVisible()) {
            if (selectedRaceIdentifier == null) {
                clearChart();
            } else if (from != null && to != null) {
                setWidget(chart);
                // if not playing or empty show loading message
                if (shouldShowLoading(timeOfLatestRequestInMillis)) {
                    showLoading(stringMessages.windChartLoading());
                }
                GetWindInfoAction getWindInfoAction = new GetWindInfoAction(sailingService, selectedRaceIdentifier,
                        from, to, settings.getResolutionInMilliseconds(), getNamesOfWindSourceTypesOfWhichToDisplaySpeedOrDirection(),
                        /* onlyUpToNewestEvent==true because we don't want
                        to overshoot the evidence so far */ true);
                asyncActionsExecutor.execute(getWindInfoAction, LOAD_WIND_CHART_DATA_CATEGORY,
                        new AsyncCallback<WindInfoForRaceDTO>() {
                            @Override
                            public void onSuccess(WindInfoForRaceDTO result) {
                                if (result != null) {
                                    updateChartSeries(result, append);
                                    updateVisibleSeries();
                                    updateStatPlotLines();
                                } else {
                                    if (!append) {
                                        clearChart(); // no wind known for untracked race
                                    }
                                }
                                hideLoading();
                            }
            
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError(stringMessages.errorFetchingWindInformationForRace() + " "
                                        + selectedRaceIdentifier + ": " + caught.getMessage(), timer.getPlayMode() == PlayModes.Live);
                                hideLoading();
                            }
                        });
            }
        }
    }
    
    private Set<String> getNamesOfWindSourceTypesOfWhichToDisplaySpeedOrDirection() {
        final Set<String> result = new HashSet<>();
        for (WindSourceType speedType : getSettings().getWindSpeedSourcesToDisplay()) {
            result.add(speedType.name());
        }
        for (WindSourceType speedType : getSettings().getWindDirectionSourcesToDisplay()) {
            result.add(speedType.name());
        }
        return result;
    }

    private void clearChart() {
        chart.removeAllSeries();
        removeStatPlotLines();
        directionAccumulators.clear();
        speedAccumulators.clear();
    }

    /** Removes all six stat plot lines (avg/min/max for direction and speed) from the chart and clears the maps. */
    private void removeStatPlotLines() {
        for (final PlotLine pl : directionAvgPlotLines.values()) { chart.getYAxis(0).removePlotLine(pl); }
        for (final PlotLine pl : directionMinPlotLines.values()) { chart.getYAxis(0).removePlotLine(pl); }
        for (final PlotLine pl : directionMaxPlotLines.values()) { chart.getYAxis(0).removePlotLine(pl); }
        for (final PlotLine pl : speedAvgPlotLines.values()) { chart.getYAxis(1).removePlotLine(pl); }
        for (final PlotLine pl : speedMinPlotLines.values()) { chart.getYAxis(1).removePlotLine(pl); }
        for (final PlotLine pl : speedMaxPlotLines.values()) { chart.getYAxis(1).removePlotLine(pl); }
        directionAvgPlotLines.clear();
        directionMinPlotLines.clear();
        directionMaxPlotLines.clear();
        speedAvgPlotLines.clear();
        speedMinPlotLines.clear();
        speedMaxPlotLines.clear();
    }

    /** Which statistic a plot line represents. */
    private enum StatKind { AVG, MIN, MAX }
    
    /**
     * Base class for running stat accumulators. Subclasses handle avg computation differently. The accumulator uses a
     * {@link ScalableValue} sub-type for computing averages. It assumes that values of this type will be displayed in a
     * chart, using a {@code double} value for the Y-axis display. As such, it supports converting {@code double} values
     * into a {@link ScalableValue} of the respective sub-type and adding it to the accumulator as the corresponding
     * {@link ScalableValue}, and vice-versa it supports converting a {@link ScalableValue} or whatever it averages to
     * ({@code AveragesTo}) to a {@code double} value for us in the chart's Y-axis.
     */
    private abstract static class StatAccumulator<T extends ScalableValue<ValueType, AveragesTo>, ValueType, AveragesTo extends Comparable<AveragesTo>> {
        protected double min;
        protected double max;
        protected int count = 0;
        private ScalableValue<ValueType, AveragesTo> sum;

        public StatAccumulator() {
            min = Double.MAX_VALUE;
            max = Double.MIN_VALUE;
            sum = null;
        }
        
        public abstract double getDoubleValue(AveragesTo t);
        
        protected abstract T fromDoubleValue(double d);
        
        public void add(double v) {
            final T scalableValue = fromDoubleValue(v);
            sum = sum == null ? scalableValue : sum.add(scalableValue);
            if (v > max) {
                max = v;
            }
            if (v < min) {
                min = v;
            }
            count++;
        }

        private AveragesTo computeAvg() {
            return sum.divide(count);
        }

        Map<StatKind, Double> toStatMap() {
            final Map<StatKind, Double> result;
            if (count == 0) {
                result = null;
            } else {
                result = new EnumMap<StatKind, Double>(StatKind.class);
                result.put(StatKind.AVG, getDoubleValue(computeAvg()));
                result.put(StatKind.MIN, min);
                result.put(StatKind.MAX, max);
            }
            return result;
        }
    }

    /**
     * Accumulates wind direction values using circular (sin/cos) averaging via {@link ScalableBearing}. The {@code double}
     * representation is the {@link Bearing#getDegrees() degree} value.
     */
    private static final class DirectionStatAccumulator extends StatAccumulator<ScalableBearing, DoublePair, Bearing> {
        @Override
        protected ScalableBearing fromDoubleValue(double bearingInDegrees) {
            return new ScalableBearing(new DegreeBearingImpl(bearingInDegrees));
        }

        @Override
        public double getDoubleValue(Bearing bearing) {
            return bearing.getDegrees();
        }
    }
    
    /**
     * Accumulates wind speed values using arithmetic averaging via {@link ScalableSpeed}. The {@code double}
     * representation is the {@link Speed#getKnots() speed in knots}.
     */
    private static final class SpeedStatAccumulator extends StatAccumulator<ScalableSpeed, Double, Speed> {
        @Override
        protected ScalableSpeed fromDoubleValue(double knots) {
            return new ScalableSpeed(new KnotSpeedImpl(knots));
        }

        @Override
        public double getDoubleValue(Speed speed) {
            return speed.getKnots();
        }
    }

    /**
     * Computes avg/min/max over a point array for zoom. Returns null if there are no valid points. Direction values are
     * normalized to 0-360 before accumulation to undo the shift applied by stayClosestToPreviousPoint.
     */
    private static Map<StatKind, Double> computeStatValues(final Point[] points, final Long fromMillis, final Long toMillis,
            final boolean isDirection) {
        final StatAccumulator<?, ?, ?> acc = isDirection ? new DirectionStatAccumulator() : new SpeedStatAccumulator();
        for (final Point p : points) {
            if (p != null && p.getY() != null) {
                if ((fromMillis == null || p.getX().longValue() >= fromMillis)
                 && (toMillis == null || p.getX().longValue() <= toMillis)) {
                    final double v = p.getY().doubleValue();
                    acc.add(v);
                }
            }
        }
        return acc.toStatMap();
    }

    /**
     * Creates a single stat plot line. Direction lines are dashed, speed lines are dotted.
     * Avg is drawn thicker than min/max. The label sits on the left for direction, right for speed.
     * The line color is a darkened version of the series color so it stands out from the main line.
     */
    private PlotLine buildStatPlotLine(final int yAxisIndex, final String color, final double value,
            final StatKind kind, final boolean isDirection, final String labelText) {
        final DashStyle dashStyle = isDirection ? DashStyle.DASH : DashStyle.DOT;
        final double width = isDirection ? (kind == StatKind.AVG ? 2 : 1.5) : (kind == StatKind.AVG ? 3 : 2);
        final PlotLineLabel.Align align = isDirection ? PlotLineLabel.Align.LEFT : PlotLineLabel.Align.RIGHT;
        final int labelX = isDirection ? 4 : -4;
        final String lineColor = darkenColor(color, 0.65);
        final PlotLine pl = chart.getYAxis(yAxisIndex).createPlotLine()
                .setColor(lineColor)
                .setWidth(width)
                .setDashStyle(dashStyle)
                .setZIndex(5)
                .setLabel(new PlotLineLabel()
                        .setText(labelText)
                        .setAlign(align)
                        .setX(labelX)
                        .setY(-4)
                        .setStyle(new Style().setOption("fontSize", "11px").setOption("color", lineColor)));
        pl.setValue(value);
        return pl;
    }

    /** Multiplies each RGB channel by factor to produce a darker shade of the given hex color. */
    private static String darkenColor(final String hex, final double factor) {
        final String h = hex.startsWith("#") ? hex.substring(1) : hex;
        final int r = (int) (Integer.parseInt(h.substring(0, 2), 16) * factor);
        final int g = (int) (Integer.parseInt(h.substring(2, 4), 16) * factor);
        final int b = (int) (Integer.parseInt(h.substring(4, 6), 16) * factor);
        return "#" + toHex2(r) + toHex2(g) + toHex2(b);
    }

    private static String toHex2(final int v) {
        final String s = Integer.toHexString(v);
        return s.length() < 2 ? "0" + s : s;
    }

    /**
     * Redraws the plot lines for one stat type (avg, min, or max) on one axis.
     * Removes the old lines first, then adds a new line for each visible source whose type
     * appears in enabledTypes. Called individually per stat so only the changed ones are redrawn.
     */
    private void updateStatPlotLinesForStat(
            final Map<WindSource, Point[]> pointsMap,
            final Map<WindSource, Series> seriesMap,
            final int yAxisIndex,
            final Set<WindSourceType> enabledTypes,
            final Map<WindSource, PlotLine> plotLineMap,
            final Map<WindSource, ? extends StatAccumulator<?, ?, ?>> accumulators,
            final StatKind kind,
            final boolean isDirection) {
        for (final PlotLine pl : plotLineMap.values()) {
            chart.getYAxis(yAxisIndex).removePlotLine(pl);
        }
        plotLineMap.clear();
        final NumberFormat fmt = NumberFormat.getFormat("0.#");
        final Util.Pair<Date, Date> zoom = timeRangeWithZoomProvider.isZoomed() ? timeRangeWithZoomProvider.getTimeZoom() : null;
        for (final Map.Entry<WindSource, Point[]> entry : pointsMap.entrySet()) {
            final WindSource source = entry.getKey();
            final Series series = seriesMap.get(source);
            if (series != null && series.isVisible() && entry.getValue() != null
                    && enabledTypes.contains(source.getType())) {
                final Map<StatKind, Double> stats;
                if (zoom == null) {
                    final StatAccumulator<?, ?, ?> acc = accumulators.get(source);
                    stats = acc == null ? null : acc.toStatMap();
                } else {
                    stats = computeStatValues(entry.getValue(), zoom.getA().getTime(), zoom.getB().getTime(), isDirection);
                }
                if (stats != null) {
                    final String color = colorMap.getColorByID(source).getAsHtml();
                    final String sourceName = WindSourceTypeFormatter.format(source, stringMessages);
                    final double statValue = stats.get(kind);
                    final String kindLabel = kind == StatKind.AVG ? stringMessages.windStatAvg()
                            : (kind == StatKind.MIN ? stringMessages.windStatMin() : stringMessages.windStatMax());
                    final String unit = isDirection ? stringMessages.degreesShort() : stringMessages.knotsUnit();
                    final String labelText = kindLabel + " " + sourceName + ": " + fmt.format(isDirection ? normalizeYAxisDirectionValue(statValue) : statValue) + unit;
                    final PlotLine pl = buildStatPlotLine(yAxisIndex, color, statValue, kind, isDirection, labelText);
                    chart.getYAxis(yAxisIndex).addPlotLines(pl);
                    plotLineMap.put(source, pl);
                }
            }
        }
    }

    /** Redraws all six stat plot lines at once. Used when all of them need to refresh together —
     *  after new data loads or when a legend series is toggled. When only one stat setting changes
     *  in the dialog, updateStatPlotLinesForStat is called directly for just that one. */
    private void updateStatPlotLines() {
        updateStatPlotLinesForStat(windSourceDirectionPoints, windSourceDirectionSeries, 0,
                settings.getDirectionAvgSources(), directionAvgPlotLines, directionAccumulators, StatKind.AVG, /* isDirection */ true);
        updateStatPlotLinesForStat(windSourceDirectionPoints, windSourceDirectionSeries, 0,
                settings.getDirectionMinSources(), directionMinPlotLines, directionAccumulators, StatKind.MIN, /* isDirection */ true);
        updateStatPlotLinesForStat(windSourceDirectionPoints, windSourceDirectionSeries, 0,
                settings.getDirectionMaxSources(), directionMaxPlotLines, directionAccumulators, StatKind.MAX, /* isDirection */ true);
        updateStatPlotLinesForStat(windSourceSpeedPoints, windSourceSpeedSeries, 1,
                settings.getSpeedAvgSources(), speedAvgPlotLines, speedAccumulators, StatKind.AVG, /* isDirection */ false);
        updateStatPlotLinesForStat(windSourceSpeedPoints, windSourceSpeedSeries, 1,
                settings.getSpeedMinSources(), speedMinPlotLines, speedAccumulators, StatKind.MIN, /* isDirection */ false);
        updateStatPlotLinesForStat(windSourceSpeedPoints, windSourceSpeedSeries, 1,
                settings.getSpeedMaxSources(), speedMaxPlotLines, speedAccumulators, StatKind.MAX, /* isDirection */ false);
    }

    /**
     * If in live mode, fetches what's missing since the last fix and <code>date</code>. If nothing has been loaded yet,
     * loads from the beginning up to <code>date</code>. If in replay mode, checks if anything has been loaded at all. If not,
     * everything for the currently selected race is loaded; otherwise, no-op.
     */
    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        if (isVisible()) {
            updateTimePlotLine(newTime);
            switch (timer.getPlayMode()) {
                case Live:
                {
                    // is date before first cache entry or is cache empty?
                    if (timeOfEarliestRequestInMillis == null || newTime.getTime() < timeOfEarliestRequestInMillis) {
                        loadData(timeRangeWithZoomProvider.getFromTime(), newTime, /* append */ true);
                    } else if (newTime.getTime() > timeOfLatestRequestInMillis) {
                        loadData(new Date(timeOfLatestRequestInMillis), timeRangeWithZoomProvider.getToTime(), /* append */true);
                    }
                    // otherwise the cache spans across date and so we don't need to load anything
                    break;
                }
                case Replay:
                {
                    if (timeOfLatestRequestInMillis == null) {
                        // pure replay mode
                        loadData(timeRangeWithZoomProvider.getFromTime(), timeRangeWithZoomProvider.getToTime(), /* append */false);
                    } else {
                        // replay mode during live play
                        if (timeOfEarliestRequestInMillis == null || newTime.getTime() < timeOfEarliestRequestInMillis) {
                            loadData(timeRangeWithZoomProvider.getFromTime(), newTime, /* append */ true);
                        } else if (newTime.getTime() > timeOfLatestRequestInMillis) {
                            loadData(new Date(timeOfLatestRequestInMillis), newTime, /* append */true);
                        }                    
                    }
                    break;
                }
            }
        }
     }

    @Override
    public void onResize() {
        chart.setSizeToMatchContainer();
        // it's important here to recall the redraw method, otherwise the bug fix for wrong checkbox positions (nativeAdjustCheckboxPosition)
        // in the BaseChart class would not be called 
        chart.redraw();
    }

    @Override
    public void onTimeZoomChanged(final Date zoomStartTimepoint, final Date zoomEndTimepoint) {
        super.onTimeZoomChanged(zoomStartTimepoint, zoomEndTimepoint);
        updateStatPlotLines();
    }

    @Override
    public void onTimeZoomReset() {
        super.onTimeZoomReset();
        updateStatPlotLines();
    }

    /**
     * Prints basic data and points of a WindInfoForRaceDTO object to a formatted string.
     * Can be used for debugging
     */
    @SuppressWarnings("unused")
    private String printWindInfoForRace(final Date from, final Date to, WindInfoForRaceDTO result, boolean printFixDetails) {
        DateTimeFormatRenderer timeFormatter = new DateTimeFormatRenderer(DateTimeFormat.getFormat("HH:mm:ss:SSS"));
        StringBuffer buffer = new StringBuffer();
        buffer.append("\n");
        buffer.append("Loaded wind data..." + "\n");
        buffer.append("From: " + timeFormatter.render(from) + "\n");
        buffer.append("To: " + timeFormatter.render(to) + "\n");
        buffer.append("With resolution: " + settings.getResolutionInMilliseconds() + "\n");
        
        for(WindSource windSource: result.windTrackInfoByWindSource.keySet()) {
            WindTrackInfoDTO windTrackInfoDTO = result.windTrackInfoByWindSource.get(windSource);
            int i = 1;
            buffer.append("Data of windsource: " + windSource.name() + "\n");
            if(printFixDetails) {
                for(WindDTO windDTO: windTrackInfoDTO.windFixes) {
                    String windFix = "P" + i++ + ": " + timeFormatter.render(new Date(windDTO.requestTimepoint));
                    if(windDTO.measureTimepoint != null) {
                        windFix += " ," + timeFormatter.render(new Date(windDTO.measureTimepoint));
                    }
                    buffer.append(windFix + "\n");
                }
            } else {
                buffer.append(Util.size(windTrackInfoDTO.windFixes) + " Fixes" + "\n");
            }
        }
        buffer.append("\n");
        return buffer.toString();
    }    

    /**
     * Prints basic data and all points of a windSource to a formatted string.
     * Can be used for debugging
     */
    @SuppressWarnings("unused")
    private String printPoints(WindSource windSource, String whatIsIt, Point[] points, boolean printFixDetails) {
        StringBuffer buffer = new StringBuffer();
        DateTimeFormatRenderer timeFormatter = new DateTimeFormatRenderer(DateTimeFormat.getFormat("HH:mm:ss:SSS"));
        buffer.append("\n");
        buffer.append("WindSource: " + windSource.name() + ": " + whatIsIt + "\n");
        buffer.append("Resolution in ms: " + settings.getResolutionInMilliseconds() + "\n");
        buffer.append("timeOfEarliestRequest: " + (timeOfEarliestRequestInMillis != null ? timeFormatter.render(new Date(timeOfEarliestRequestInMillis)) : "") + "\n");
        buffer.append("timeOfLatestRequest: " + (timeOfLatestRequestInMillis != null ? timeFormatter.render(new Date(timeOfLatestRequestInMillis)) : "") + "\n");
        if (points == null) {
            buffer.append("Points is null" + "\n");
        } else {
            buffer.append("Point count: " + points.length + "\n");
            if(printFixDetails) {
                Date xAsDate = new Date();
                for (int i = 0; i < points.length; i++) {
                    Point point = points[i];
                    xAsDate.setTime(point.getX().longValue());
                    buffer.append("P" + (i + 1) + ": " + timeFormatter.render(xAsDate) + ", V: " + point.getY() + "\n");
                }
            }
        }
        buffer.append("\n");
        return buffer.toString();
    }

    @Override
    public String getDependentCssClassName() {
        return "windChart";
    }

    @Override
    public WindChartSettings getSettings() {
        return settings;
    }

    @Override
    public String getId() {
        return windChartLifecycle.getComponentId();
    }

    /**
     * Forces the display of a specific windProvider preselected, and all of same type unselected. Will be disabled by
     * either updateSettings or once the provider was properly found and shown
     */
    public void showProvider(WindSource windprovider) {
        final WindSourceType type = windprovider.getType();
        final Set<WindSourceType> windSpeedSourcesToDisplay = new HashSet<>();
        final Set<WindSourceType> windDirectionSourcesToDisplay = new HashSet<>();
        windSpeedSourcesToDisplay.add(type);
        windDirectionSourcesToDisplay.add(type);
        final WindChartSettings patched = new WindChartSettings(true, windSpeedSourcesToDisplay, true,
                windDirectionSourcesToDisplay, settings.getResolutionInMilliseconds(),
                settings.getDirectionAvgSources(), settings.getDirectionMinSources(), settings.getDirectionMaxSources(),
                settings.isDirectionAvgBulk(), settings.isDirectionMinBulk(), settings.isDirectionMaxBulk(),
                settings.getSpeedAvgSources(), settings.getSpeedMinSources(), settings.getSpeedMaxSources(),
                settings.isSpeedAvgBulk(), settings.isSpeedMinBulk(), settings.isSpeedMaxBulk());
        updateSettings(patched);
        preselectFilter = windprovider;
        updateVisibleSeries();
    }
}
