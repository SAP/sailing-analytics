package com.sap.sailing.dashboards.gwt.client.widgets.windbot.charts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.moxieapps.gwt.highcharts.client.Axis.Type;
import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Credits;
import org.moxieapps.gwt.highcharts.client.DateTimeLabelFormats;
import org.moxieapps.gwt.highcharts.client.Highcharts;
import org.moxieapps.gwt.highcharts.client.Legend;
import org.moxieapps.gwt.highcharts.client.PlotLine.DashStyle;
import org.moxieapps.gwt.highcharts.client.Point;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.Style;
import org.moxieapps.gwt.highcharts.client.Time;
import org.moxieapps.gwt.highcharts.client.ToolTip;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsData;
import org.moxieapps.gwt.highcharts.client.labels.AxisLabelsFormatter;
import org.moxieapps.gwt.highcharts.client.labels.DataLabels;
import org.moxieapps.gwt.highcharts.client.labels.XAxisLabels;
import org.moxieapps.gwt.highcharts.client.labels.YAxisLabels;
import org.moxieapps.gwt.highcharts.client.plotOptions.AreaPlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.BarPlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.Marker;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.dashboards.gwt.client.theme.Colors;
import com.sap.sailing.dashboards.gwt.client.theme.Fonts;
import com.sap.sailing.dashboards.gwt.client.theme.Sizes;
import com.sap.sailing.dashboards.gwt.client.util.HighchartsUtil;
import com.sap.sailing.dashboards.gwt.shared.WindType;
import com.sap.sailing.gwt.ui.client.shared.charts.ChartPointRecalculator;
import com.sap.sailing.gwt.ui.shared.WindDTO;

/**
 * The class represents a wind chart that is displayed vertically. Because it is meant to be used to display wind fixes,
 * it shows also changes its threshold to indicate the average wind value over a certain amount of time. The charts
 * x-axis can show the values in a small and big time interval that the user can change by tabing somewhere on the whole
 * chart.
 * 
 * @author Alexander Ries (D062114)
 *
 */
public class VerticalWindChart extends Composite implements HasWidgets {

    private Series verticalWindChartSeries;
    private Point lastPoint;
    private List<VerticalWindChartClickListener> verticalWindChartClickListeners;

    /**
     * The field is used to cache the current selected display interval. It is either
     * {@link #SMALL_DISPLAY_INTERVALL_IN_MINUTES} or {@link #LARGE_DISPLAY_INTERVALL_IN_MINUTES}.
     * */
    private int chartIntervallinMinutes = SMALL_DISPLAY_INTERVALL_IN_MINUTES;

    /**
     * LARGE_DISPLAY_INTERVALL_IN_MINUTES and SMALL_DISPLAY_INTERVALL_IN_MINUTES define two possible intervals that the
     * user can select by tabing on the chart.
     * */
    private static final int LARGE_DISPLAY_INTERVALL_IN_MINUTES = 60;
    private static final int SMALL_DISPLAY_INTERVALL_IN_MINUTES = 15;
    
    private static final int MAX_SERIES_POINTS = 100000000;

    private static VerticalWindChartUiBinder uiBinder = GWT.create(VerticalWindChartUiBinder.class);
    
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(VerticalWindChart.class.getName());

    interface VerticalWindChartUiBinder extends UiBinder<Widget, VerticalWindChart> {
    }

    interface VerticalWindChartStyle extends CssResource {
    }

    /**
     * An invisible clickable edge to edge area that changes onClick the charts x-axis extreme values. See
     * {@link #verticalWindChartClickAreaClicked(ClickEvent)}
     * */
    @UiField
    FocusPanel verticalWindChartClickArea;

    @UiField(provided = true)
    Chart verticalWindChart;

    public VerticalWindChart() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    public @UiConstructor VerticalWindChart(String positiveFillColorAsHex, String negativeFillColorAsHex) {
        VerticalWindChartResources.INSTANCE.gss().ensureInjected();
        initVerticalWindChartWithColors(positiveFillColorAsHex, negativeFillColorAsHex);
        initWidget(uiBinder.createAndBindUi(this));
    }

    private void initVerticalWindChartWithColors(String positiveFillColor, String negativeFillColor) {
        Highcharts.setOptions(
                new Highcharts.Options().setTime(
                    new Time()
                      .setUseUTC(false)
              ));
        verticalWindChart = new Chart();
        setChartOptions();
        setXAxisOptions();
        setYAxisOptions();
        initVerticalWindChartSeries(positiveFillColor, negativeFillColor);
        verticalWindChartClickListeners = new ArrayList<VerticalWindChartClickListener>();
        HighchartsUtil.setSizeToMatchContainerDelayed(verticalWindChart);
    }

    private void setChartOptions() {
        verticalWindChart.setType(Series.Type.AREA_SPLINE).setInverted(true).setChartTitleText(" ").setMarginTop(10)
                .setMarginBottom(25).setMarginRight(20)
                        .setBackgroundColor("#FFFFFF")
        .setBorderColor("#FFFFFF")
        .setPlotShadow(false)
        .setPlotBorderColor("#FFFFFF")
        .setOption("/exporting/enabled", false)
        .setShadow(false)
                .setBarPlotOptions(new BarPlotOptions().setDataLabels(new DataLabels().setEnabled(true)))
                .setLegend(new Legend().setEnabled(false)).setCredits(new Credits().setEnabled(false))
                .setToolTip(new ToolTip().setEnabled(false));
    }

    private void setXAxisOptions() {
        verticalWindChart
                .getXAxis()
                .setType(Type.DATE_TIME)
                .setReversed(false)
                .setLabels(
                        new XAxisLabels().setStyle(new Style().setFontFamily(Fonts.DASHBOARD_FONT_FAMILY).setFontSize(Sizes.DASHBOARD_FONT_SIZE_SMALL).setColor(Colors.LIGHT_TEXT_COLOR))).setLineColor(Colors.LIGHT_TEXT_COLOR).setTickWidth(0.5).setLineWidth(1)
                .setTickWidth(1).setTickColor(Colors.LIGHT_TEXT_COLOR).setOffset(0).setMinPadding(20).setGridLineColor(Colors.LIGHT_GREY)
                .setGridLineWidth(0)
                .setDateTimeLabelFormats(new DateTimeLabelFormats().setMonth("%e. %b").setYear("%b"));
    }

    private void setYAxisOptions() {
        verticalWindChart
                .getYAxis()
                .setLineColor(Colors.LIGHT_TEXT_COLOR)
                .setTickWidth(1)
                .setTickColor(Colors.LIGHT_TEXT_COLOR)
                .setAxisTitleText(null)
                .setGridLineWidth(0)
                .setAlternateGridColor(Colors.LIGHT_GREY)
                .setLineWidth(1)
                .setEndOnTick(true)
                .setStartOnTick(true)
                .setGridLineColor("transparent")
                .setGridLineWidth(0)
                .setOffset(0)
                .setMaxPadding(0)
                .setLabels(
                        new YAxisLabels().setStyle(
                                new Style().setFontFamily(Fonts.DASHBOARD_FONT_FAMILY).setFontSize(Sizes.DASHBOARD_FONT_SIZE_SMALL).setColor(Colors.LIGHT_TEXT_COLOR)).setFormatter(
                                new AxisLabelsFormatter() {
                                    @Override
                                    public String format(AxisLabelsData axisLabelsData) {
                                        long value = axisLabelsData.getValueAsLong() % 360;
                                        return Long.valueOf(value < 0 ? value + 360 : value).toString();
                                    }
                                }));
    }

    private void initVerticalWindChartSeries(String positivSeriesColorAsHex, String negativeSeriesColorAsHex) {
        verticalWindChartSeries = verticalWindChart.createSeries();
        AreaPlotOptions areaPlotOptions = new AreaPlotOptions();
        verticalWindChart.addSeries(verticalWindChartSeries.setName(null).setPlotOptions(
                areaPlotOptions.setDashStyle(DashStyle.SOLID).setLineWidth(0.1)
                        .setMarker(new Marker().setEnabled(false)).setShadow(false).setHoverStateEnabled(false)
                        .setLineColor("#FFFFFF").setFillColor(positivSeriesColorAsHex)
                        .setOption("negativeFillColor", negativeSeriesColorAsHex)).setOption("turboThreshold", MAX_SERIES_POINTS));
    }

    /**
     * The methods adds one point to the charts series and updates the threshold with a wind average value. When the
     * series point contains wind direction values the y values gets adapted to stay close to the previous one.
     * Depending on the selected chart display interval {@link #SMALL_DISPLAY_INTERVALL_IN_MINUTES} or
     * {@link #LARGE_DISPLAY_INTERVALL_IN_MINUTES}, the extreme values of the charts x-axis get adapted. See
     * {@link #adaptVerticalWindChartExtemes()},
     * {@link #setXAxisExtremesForSeriesPointRangeIsSmallerThanChartDisplayIntervall()} and
     * {@link #setXAxisExtremesForSeriesPointRangeIsBiggerThanChartDisplayIntervall()}.
     * */
    public void addPointsToSeriesWithAverageAndWindType(List<WindDTO> windFixes, final double average, WindType windType) {
        if(windType.equals(WindType.SPEED)) {
            addPointsToSeriesOfTypeSpeed(windFixes);
        } else {
            addPointsToSeriesOfTypeDirection(windFixes);
        }
        verticalWindChartSeries.updateThreshold(""+average);
        adaptVerticalWindChartExtemes(lastPoint.getX().longValue());
    }
    
    private Point createSpeedPoint(WindDTO windDTO) {
        Point result = new Point(windDTO.measureTimepoint, windDTO.trueWindSpeedInKnots);
        if (lastPoint != null) {
            adaptWindDirectionPointToStayCloseToLastPoint(lastPoint, result);
        }
        lastPoint = result;
        return result;
    }
    
    private Point createDirectionPoint(WindDTO windDTO) {
        Point result = new Point(windDTO.measureTimepoint, windDTO.trueWindFromDeg);
        if (lastPoint != null) {
            adaptWindDirectionPointToStayCloseToLastPoint(lastPoint, result);
        }
        lastPoint = result;
        return result;
    }
    
    private void addPointsToSeriesOfTypeSpeed(List<WindDTO> windFixes) {
        for (WindDTO windDTO : windFixes) {
            verticalWindChartSeries.addPoint(createSpeedPoint(windDTO), false, false, false);
        }
    }
    
    private void addPointsToSeriesOfTypeDirection(List<WindDTO> windFixes) {
        for (WindDTO windDTO : windFixes) {
            verticalWindChartSeries.addPoint(createDirectionPoint(windDTO), false, false, false);
        }
    }

    /**
     * Depending on the series points time range and the selected display interval, the series fits into the display
     * interval or not. So there are two cases where the extreme points of the x-axis get calculated differently.
     * */
    private void adaptVerticalWindChartExtemes(Long lastTimePoint) {
            setXAxisExtremesForSeriesPointRangeIsBiggerThanChartDisplayIntervall(lastTimePoint.longValue());
    }

    /**
     * When the time range of the series points is bigger than the current selected display interval represented by
     * either LARGE_DISPLAY_INTERVALL_IN_MINUTES or SMALL_DISPLAY_INTERVALL_IN_MINUTES the x-axis extremes min value is
     * the the max value minus the selected display interval and the max value the last points time value. The chart
     * gets other extremes, because the values do not fit anymore into the selected display interval.
     * */
    private void setXAxisExtremesForSeriesPointRangeIsBiggerThanChartDisplayIntervall(long lastTimePoint) {
                long maximumExtremeValueInMillis = lastTimePoint;
                long minimumExtremeValueInMillis = maximumExtremeValueInMillis - chartIntervallinMinutes * 60 * 1000;
                verticalWindChart.getXAxis().setExtremes(minimumExtremeValueInMillis, maximumExtremeValueInMillis, true, false);                
    }

    public void addVerticalWindChartClickListener(VerticalWindChartClickListener verticalWindChartClickListener) {
        verticalWindChartClickListeners.add(verticalWindChartClickListener);
    }
    
    public void removeVerticalWindChartClickListener(VerticalWindChartClickListener verticalWindChartClickListener) {
        verticalWindChartClickListeners.remove(verticalWindChartClickListener);
    }
    
    public void notifyVerticalWindChartClickListeners(int intervalInMillis) {
        for (VerticalWindChartClickListener verticalWindChartClickListener : verticalWindChartClickListeners) {
            verticalWindChartClickListener.clickedWindChartWithNewIntervalChangeInMillis(intervalInMillis);
        }
    }
    
    private Point adaptWindDirectionPointToStayCloseToLastPoint(Point previousPoint, Point point) {
        return ChartPointRecalculator.stayClosestToPreviousPoint(previousPoint, point);
    }

    /**
     * The method handles a click on the vertical wind charts focus panel. It changes the display interval of the x-axis
     * and adapts the click hint at the bottom of the chart.
     * */
    @UiHandler("verticalWindChartClickArea")
    public void verticalWindChartClickAreaClicked(ClickEvent e) {
        if (lastPoint != null) {
            if (chartIntervallinMinutes == SMALL_DISPLAY_INTERVALL_IN_MINUTES) {
                chartIntervallinMinutes = LARGE_DISPLAY_INTERVALL_IN_MINUTES;
            } else {
                chartIntervallinMinutes = SMALL_DISPLAY_INTERVALL_IN_MINUTES;
            }
            adaptVerticalWindChartExtemes(lastPoint.getX().longValue());
            notifyVerticalWindChartClickListeners(chartIntervallinMinutes);
        }
    }

    @Override
    public void add(Widget w) {
        throw new UnsupportedOperationException("The method add(Widget w) is not supported.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("The method clear() is not supported.");
    }

    @Override
    public Iterator<Widget> iterator() {
        return null;
    }

    @Override
    public boolean remove(Widget w) {
        return false;
    }
}
