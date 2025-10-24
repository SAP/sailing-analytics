package com.sap.sailing.gwt.autoplay.client.utils;

import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreLiveRaceLeaderboardWithImageViewImpl.ImageProvider;

public class CompetitorImageProvider implements ImageProvider {

    @Override
    public String getImageUrl(CompetitorDTO marked) {
        if (marked.getImageURL() != null) {
            return marked.getImageURL();
        }
        return "";
    }

    @Override
    public String getTakedownNoticeContextKey() {
        return "takedownRequestForCompetitorImage";
    }
}
