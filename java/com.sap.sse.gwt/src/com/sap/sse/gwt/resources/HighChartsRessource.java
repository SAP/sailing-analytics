package com.sap.sse.gwt.resources;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface HighChartsRessource extends ClientBundle {
    /**
     * Package common controls css files as text resource so it can be injected when required. We need to move away from
     * this css file.
     */
    @Source("css/CommonControls.css")
    TextResource commonControls();

    @Source("highcharts/highcharts.js")
    TextResource highcharts();
    
    @Source("highcharts/themes/grid.js")
    TextResource highchartsThemeGrid();

    @Source("highcharts/modules/exporting.js")
    TextResource highchartsModuleExport();

    @Source("highcharts/highcharts-more.js")
    TextResource highchartsMore();
}
