package com.sap.sailing.gwt.ui.leaderboard;

import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class ScoringSchemeTypeFormatter {
    public static String format(ScoringSchemeType scoringSchemeType, StringMessages stringMessages) {
        switch (scoringSchemeType) {
        case LOW_POINT:
            return stringMessages.scoringSchemeLowPointSystem();
        case HIGH_POINT:
            return stringMessages.scoringSchemeHighPointSystem();
        case HIGH_POINT_ESS_OVERALL:
            return stringMessages.scoringSchemeHighPointEssOverall();
        case HIGH_POINT_LAST_BREAKS_TIE:
            return stringMessages.scoringSchemeHighPointLastBreaksTie();
        case HIGH_POINT_FIRST_GETS_ONE:
            return stringMessages.scoringSchemeHighPointFirstGetsOne();
        case HIGH_POINT_FIRST_GETS_TEN:
            return stringMessages.scoringSchemeHighPointFirstGetsTen();
        case LOW_POINT_WINNER_GETS_ZERO:
            return stringMessages.scoringSchemeLowPointWinnerGetsZero();
        }
        return null;
    }
}
