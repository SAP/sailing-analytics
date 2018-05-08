package com.sap.sailing.gwt.ui.client.shared.charts;

import org.moxieapps.gwt.highcharts.client.Chart;
import org.moxieapps.gwt.highcharts.client.Legend;
import org.moxieapps.gwt.highcharts.client.Series;
import org.moxieapps.gwt.highcharts.client.events.SeriesCheckboxClickEvent;
import org.moxieapps.gwt.highcharts.client.events.SeriesCheckboxClickEventHandler;
import org.moxieapps.gwt.highcharts.client.plotOptions.PlotOptions;
import org.moxieapps.gwt.highcharts.client.plotOptions.SeriesPlotOptions;

public class ChartUtil {

    /**
     * When using this method to enable the use of checkboxes only for hiding / showing a series, callers need to ensure
     * that all series have {@link PlotOptions#setSelected(boolean)} set to <code>true</code> for all series that are
     * added to the chart and hence visible. Otherwise, the checkbox won't initially be in sync with the series'
     * visibility state.
     */
    static protected void useCheckboxesToShowAndHide(final Chart chart) {
        chart.setLegend(new Legend().setEnabled(true).setBorderWidth(0).setSymbolPadding(25)); // make room for checkbox
        chart.setSeriesPlotOptions(new SeriesPlotOptions().setShowCheckbox(true)
                .setSeriesCheckboxClickEventHandler(new SeriesCheckboxClickEventHandler() {
                    @Override
                    public boolean onClick(SeriesCheckboxClickEvent seriesCheckboxClickEvent) {
                        // we want to change the visiblity not the selection state, so own handler is necessary
                        Series series = chart.getSeries(seriesCheckboxClickEvent.getSeriesId());
                        series.setVisible(seriesCheckboxClickEvent.isChecked());
                        return false; // don't toggle the select state of the series
                    }
                }));
    }

}
