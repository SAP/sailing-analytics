package com.sap.sailing.gwt.ui.client.shared.charts;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sse.common.settings.generic.EnumSetting;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class MultiCompetitorRaceChartSettings extends ChartSettings {
    private static final long serialVersionUID = 2885108745814848018L;
    
    private EnumSetting<DetailType> firstDetailType;
    private EnumSetting<DetailType> secondDetailType;
    
    @Override
    protected void addChildSettings(SecurityChildSettingsContext context) {
        super.addChildSettings(context);
        firstDetailType = new EnumSetting<>("firstDetailType", this, DetailType.CHART_WINDWARD_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD, DetailType::valueOfString);
        secondDetailType = new EnumSetting<>("secondDetailType", this, null, DetailType::valueOfString);
    }
    
    public MultiCompetitorRaceChartSettings() {
    }

    /**
     * @param firstDetailType must not be {@code null}
     * @param secondDetailType may be {@code null} if only one detail is to be displayed per competitor in the chart
     */
    public MultiCompetitorRaceChartSettings(ChartSettings settings, DetailType firstDetailType,
            DetailType secondDetailType) {
        super(settings.getStepSizeInMillis());
        this.firstDetailType.setValue(firstDetailType);
        this.secondDetailType.setValue(secondDetailType);
    }

    public DetailType getFirstDetailType() {
        return firstDetailType.getValue();
    }

    public DetailType getSecondDetailType() {
        return secondDetailType.getValue();
    }
}
