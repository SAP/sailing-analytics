package com.sap.sailing.gwt.ui.leaderboard;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.LeaderboardRowDTO;

/**
 * Shows the {@link LeaderboardRowDTO#totalTimeSailedDownwindInSeconds} attribute.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MaxSpeedOverallColumn extends FormattedDoubleLegDetailColumn {
    
    private static class MaxSpeedOverallField implements LegDetailField<Double> {
        @Override
        public Double get(LeaderboardRowDTO row) {
            return row.maximumSpeedOverGroundInKnots;
        }
    }

    public MaxSpeedOverallColumn(StringMessages stringMessages, String headerStyle, String columnStyle) {
        super(stringMessages.maximumSpeedOverGroundInKnots(), "[" + stringMessages.currentSpeedOverGroundInKnotsUnit() + "]",
                new MaxSpeedOverallField(),
                DetailType.MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS.getPrecision(),
                DetailType.MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS.getDefaultSortingOrder(),
                headerStyle, columnStyle);
    }

    /**
     * Adds the time point when the maximum speed was achieved as tool tip / title
     */
    @Override
    protected String getTitle(LeaderboardRowDTO row) {
        String result = null;
        if (row.whenMaximumSpeedOverGroundWasAchieved != null) {
            result = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(row.whenMaximumSpeedOverGroundWasAchieved);
        }
        return result;
    }
}
