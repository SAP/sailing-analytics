package com.sap.sailing.gwt.ui.leaderboard;

import java.util.Comparator;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sse.common.impl.MeterDistance;

public class RideHeightQualityMinMaxRender extends MinMaxRenderer<LeaderboardRowDTO> {

    public RideHeightQualityMinMaxRender(HasStringAndDoubleValue<LeaderboardRowDTO> valueProvider, Comparator<LeaderboardRowDTO> comparator) {
        super(valueProvider, comparator);
    }
    
    /**
     * Renders the value of a {@link LeaderboardRowDTO}. Values greater than
     * {@link BravoFix#MIN_FOILING_HEIGHT_THRESHOLD} are considered OK, values greater than that plus 0.2m are considered good,
     * values below {@link BravoFix#MIN_FOILING_HEIGHT_THRESHOLD} are considered bad.
     */
    @Override
    protected void render(LeaderboardRowDTO row, String nullSafeValue, String nullSafeTitle, SafeHtmlBuilder sb) {
        Double doubleValue = getValueProvider().getDoubleValue(row);
        if (doubleValue != null) {
            final String barStyle;
            if (doubleValue > BravoFix.MIN_FOILING_HEIGHT_THRESHOLD.add(new MeterDistance(0.2)).getMeters()) {
                barStyle = BACKGROUND_BAR_STYLE_GOOD;
            } else if (doubleValue >= BravoFix.MIN_FOILING_HEIGHT_THRESHOLD.getMeters()) {
                barStyle = BACKGROUND_BAR_STYLE_OK;
            } else {
                barStyle = BACKGROUND_BAR_STYLE_BAD;
            }
            sb.append(TEMPLATES.render(nullSafeValue, barStyle, nullSafeTitle, 100));
        }
    }
}
