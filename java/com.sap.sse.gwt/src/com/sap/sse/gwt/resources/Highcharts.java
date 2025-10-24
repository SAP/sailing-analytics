package com.sap.sse.gwt.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.ScriptInjector;

/**
 * Convenience class to inject common control css content
 * 
 * @author pgtaboada
 *
 */
public final class Highcharts {
    private static final HighChartsRessource HIGHCHARTS_RESSOURCES = GWT.create(HighChartsRessource.class);

    private static boolean isInjected = false;
    private static boolean isMoreInjected = false;
    private static boolean isExportInjected = false;

    protected Highcharts() {
    }

    /**
     * inject minimal js required for highcharts
     */
    public static void ensureInjected() {
        if (!isInjected) {
            ScriptInjector.fromString(HIGHCHARTS_RESSOURCES.highcharts().getText()).setWindow(ScriptInjector.TOP_WINDOW).inject();
            ScriptInjector.fromString(HIGHCHARTS_RESSOURCES.highchartsThemeGrid().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                    .inject();
            isInjected = true;
        }
    }

    /**
     * inject required js for polar charts, angular gauges and range series as well as radial gradients for pies
     */
    public static void ensureInjectedWithMore() {
        ensureInjected();
        if (!isMoreInjected) {
            ScriptInjector.fromString(HIGHCHARTS_RESSOURCES.highchartsMore().getText()).setWindow(ScriptInjector.TOP_WINDOW).inject();
            isMoreInjected = true;
        }
    }

    /**
     * inject required js that allows users to download the chart as pdf, png, jpeg or a svg vector image. It also
     * allows printing the chart directly without distracting elements from the web page.
     */
    public static void ensureInjectedWithExport() {
        ensureInjectedWithMore();
        if (!isExportInjected) {
            ScriptInjector.fromString(HIGHCHARTS_RESSOURCES.highchartsModuleExport().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                    .inject();
            isExportInjected = true;
        }
    }
}
