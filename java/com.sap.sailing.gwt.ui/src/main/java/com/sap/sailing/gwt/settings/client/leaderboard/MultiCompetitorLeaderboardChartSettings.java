package com.sap.sailing.gwt.settings.client.leaderboard;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.EnumSetting;
import com.sap.sse.common.settings.generic.StringToEnumConverter;

public class MultiCompetitorLeaderboardChartSettings extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = 2456555424331709738L;

    private EnumSetting<DetailType> detailType;

    private MultiCompetitorLeaderboardChartSettings() {
        super();
    }
    
    public MultiCompetitorLeaderboardChartSettings(DetailType detailType) {
        this();
        this.detailType.setValue(detailType);
    }
    
    @Override
    protected void addChildSettings() {
        detailType = new EnumSetting<>("detailType", this, new StringToEnumConverter<DetailType>() {
            @Override
            public DetailType fromString(String stringValue) {
                return DetailType.valueOf(stringValue);
            }
        });
    }

    public DetailType getDetailType() {
        return detailType.getValue();
    }
    
    public static MultiCompetitorLeaderboardChartSettings createWithDefaultDetailType(boolean isOverall, DetailType detailType) {
        MultiCompetitorLeaderboardChartSettings result = createWithDefaultDetailType(isOverall);
        result.detailType.setValue(detailType);
        return result;
    }
    
    public static MultiCompetitorLeaderboardChartSettings createWithDefaultDetailType(boolean isOverall) {
        MultiCompetitorLeaderboardChartSettings result = new MultiCompetitorLeaderboardChartSettings();
        result.detailType.setDefaultValue(getDefaultDetailType(isOverall));
        return result;
    }
    
    public static DetailType getDefaultDetailType(boolean isOverall) {
        return isOverall ? DetailType.OVERALL_RANK : DetailType.REGATTA_RANK;
    }
}
