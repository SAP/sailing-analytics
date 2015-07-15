package com.sap.sailing.dashboards.gwt.client.startlineadvantage;

import java.util.Iterator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.dashboards.gwt.client.windchart.VerticalWindChartClickListener;
import com.sap.sailing.dashboards.gwt.client.windchart.WindBotComponent;
import com.sap.sailing.gwt.ui.client.StringMessages;

/**
 * The purpose of the class is to display a live and an average value of a continuously updating data source.
 * {@link StartLineAdvantageComponent} extends from it and the class {@link WindBotComponent} uses it to display wind
 * data.
 * 
 * @author Alexander Ries (D062114)
 *
 */
public class LiveAverageComponent extends Composite implements HasWidgets, VerticalWindChartClickListener {

    private static LiveAverageComponentUiBinder uiBinder = GWT.create(LiveAverageComponentUiBinder.class);

    interface LiveAverageComponentUiBinder extends UiBinder<Widget, LiveAverageComponent> {
    }
    
    interface LiveAverageComponentStyle extends CssResource {
        
        String liveAverageCompoment_panel_value_unit_degrees();
    }

    @UiField
    HTMLPanel liveAveragePanel;

    @UiField
    DivElement header;

    @UiField
    SpanElement liveNumber;

    @UiField
    SpanElement liveUnit;

    @UiField
    SpanElement averageNumber;

    @UiField
    SpanElement averageUnit;

    @UiField
    DivElement liveLabel;

    @UiField
    DivElement averageLabel;

    @UiField
    HTMLPanel livePanel;

    @UiField
    HTMLPanel middleLine;

    @UiField
    HTMLPanel averagePanel;
    
    @UiField
    LiveAverageComponentStyle style;

    private StringMessages stringConstants;
    public LiveAverageComponent() {
        initWidget(uiBinder.createAndBindUi(this));
    }

    /**
     * @param unit
     *            is used for a unit label next to each value displayed.
     * @param unitFontSize
     *            sets a special size
     * 
     * */
    public LiveAverageComponent(String header, String unit) {
        stringConstants = StringMessages.INSTANCE;
        initWidget(uiBinder.createAndBindUi(this));
        liveAveragePanel.getElement().getStyle().setProperty("backgroundColor", "white");
        this.header.setInnerText(header);
        this.liveUnit.setInnerText(unit);
        this.averageUnit.setInnerText(unit);
        // Lifts the degrees unit string "°". Otherwise it would be too small.
        if (unit.equals("°")) {
            this.liveUnit.addClassName(style.liveAverageCompoment_panel_value_unit_degrees());
            this.averageUnit.setClassName(style.liveAverageCompoment_panel_value_unit_degrees());
        }
        this.liveLabel.setInnerHTML(stringConstants.dashboardLiveWind());
        this.averageLabel.setInnerHTML(stringConstants.dashboardAverageWind()+"<br>"+stringConstants.dashboardAverageWindMinutes(15));
    }

    public void setLiveValue(String liveValue) {
        this.liveNumber.setInnerText(liveValue);
    }
    
    public void setAverageValue(String averageValue) {
        this.averageNumber.setInnerText(averageValue);
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

    @Override
    public void clickedWindChartWithNewIntervalChangeInMillis(int windChartIntervallInMillis) {
        this.averageLabel.setInnerHTML(stringConstants.dashboardAverageWind()+"<br>"+stringConstants.dashboardAverageWindMinutes(windChartIntervallInMillis));
    }
}
