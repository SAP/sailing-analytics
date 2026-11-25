package com.sap.sailing.racecommittee.app.ui.utils;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;

public class CompetitorUtils {

    public static final String DELIMITER_SHORT_NAME = " / ";
    public static final String DELIMITER_SAIL_ID = " - ";

    public static String getDisplayName(Competitor competitor) {
        String shortName = competitor.getShortName();
        String sailIdOrBoatName = null;
        if (competitor.hasBoat() && competitor instanceof CompetitorWithBoat) {
            CompetitorWithBoat competitorWithBoat = (CompetitorWithBoat) competitor;
            String sailId = competitorWithBoat.getBoat().getSailID();
            if (TextUtils.isEmpty(sailId)) {
                sailIdOrBoatName = competitorWithBoat.getBoat().getName();
            } else {
                sailIdOrBoatName = sailId;
            }
        }
        return getDisplayName(shortName, sailIdOrBoatName, competitor.getName());
    }

    public static String getDisplayName(CompetitorResult competitor) {
        String shortName = competitor.getShortName();
        String sailIdOrBoatName;
        String sailId = competitor.getBoatSailId();
        if (TextUtils.isEmpty(sailId)) {
            sailIdOrBoatName = competitor.getBoatName();
        } else {
            sailIdOrBoatName = sailId;
        }
        return getDisplayName(shortName, sailIdOrBoatName, competitor.getName());
    }

    private static String getDisplayName(@Nullable String shortName,
                                         @Nullable String sailIdOrBoatName, String name) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(shortName)) {
            builder.append(shortName);
            builder.append(DELIMITER_SHORT_NAME);
        }
        if (!TextUtils.isEmpty(sailIdOrBoatName)) {
            builder.append(sailIdOrBoatName);
            builder.append(DELIMITER_SAIL_ID);
        }
        builder.append(name);
        return builder.toString();
    }
}
