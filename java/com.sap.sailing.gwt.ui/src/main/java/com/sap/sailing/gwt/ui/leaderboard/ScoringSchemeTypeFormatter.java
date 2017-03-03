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
        case HIGH_POINT_ESS_OVERALL_12:
            return stringMessages.scoringSchemeHighPointEssOverall12();
        case HIGH_POINT_LAST_BREAKS_TIE:
            return stringMessages.scoringSchemeHighPointLastBreaksTie();
        case HIGH_POINT_FIRST_GETS_ONE:
            return stringMessages.scoringSchemeHighPointFirstGetsOne();
        case HIGH_POINT_FIRST_GETS_TEN:
            return stringMessages.scoringSchemeHighPointFirstGetsTen();
        case LOW_POINT_WINNER_GETS_ZERO:
            return stringMessages.scoringSchemeLowPointWinnerGetsZero();
        case HIGH_POINT_WINNER_GETS_FIVE:
            return stringMessages.scoringSchemeWinnerGetsFive();
        case HIGH_POINT_WINNER_GETS_SIX:
            return stringMessages.scoringSchemeWinnerGetsSix();
        case HIGH_POINT_WINNER_GETS_EIGHT:
            return stringMessages.scoringSchemeWinnerGetsEight();
        case HIGH_POINT_MATCH_RACING:
            return stringMessages.scoringSchemeHighPointMatchRacing();
        case HIGH_POINT_WINNER_GETS_EIGHT_AND_INTERPOLATION:
            return stringMessages.scoringSchemeWinnerGetsEightAndInterpolation();
        case HIGH_POINT_FIRST_GETS_TEN_OR_EIGHT:
            return stringMessages.scoringSchemeHighPointFirstGetsTenOrEight();
        case HIGH_POINT_FIRST_GETS_TWELVE_OR_EIGHT:
            return stringMessages.scoringSchemeHighPointFirstGetsTwelveOrEight();
        case HIGH_POINT_FIRST_GETS_TWELVE_OR_EIGHT_2017:
            return stringMessages.scoringSchemeHighPointFirstGetsTwelveOrEight();
        case LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07:
            return stringMessages.scoringSchemeLowPointWithEliminationsAndRoundsWinnerGets07();
        case LOW_POINT_LEAGUE_OVERALL:
            return stringMessages.scoringSchemeLowPointForLeagueOverallLeaderboard();
        case LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY:
            return stringMessages.scoringSchemeLowPointTieBreakBasedOnLastSeriesOnly();
        }
        return null;
    }
    
    public static String getDescription(ScoringSchemeType scoringSchemeType, StringMessages stringMessages) {
        switch(scoringSchemeType) {
        case HIGH_POINT_FIRST_GETS_TEN_OR_EIGHT:
            return stringMessages.scoringSchemeHighPointFirstGetsTenOrEightDescription();
        case HIGH_POINT_FIRST_GETS_TWELVE_OR_EIGHT:
            return stringMessages.scoringSchemeHighPointFirstGetsTwelveOrEightDescription();
        case HIGH_POINT_ESS_OVERALL:
            return stringMessages.scoringSchemeHighPointEssOverallDescription();
        case HIGH_POINT_ESS_OVERALL_12:
            return stringMessages.scoringSchemeHighPointEssOverall12Description();
        case LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07:
            return stringMessages.scoringSchemeLowPointWithEliminationsAndRoundsWinnerGets07Description();
        case LOW_POINT_LEAGUE_OVERALL:
            return stringMessages.scoringSchemeLowPointForLeagueOverallLeaderboardDescription();
        case HIGH_POINT_MATCH_RACING:
            return stringMessages.scoringSchemeHighPointMatchRacingDescription();
        case LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY:
            return stringMessages.scoringSchemeLowPointTieBreakBasedOnLastSeriesOnlyDescription();
        default:
            return format(scoringSchemeType, stringMessages);
        }
    }
}
