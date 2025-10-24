package com.sap.sailing.gwt.home.communication.event.minileaderboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import com.sap.sse.gwt.shared.DTO;

public class GetMiniLeaderboardDTO implements DTO {
    private ArrayList<MiniLeaderboardItemDTO> items = new ArrayList<MiniLeaderboardItemDTO>();
    private String scoreCorrectionText;
    private Date lastScoreUpdate;
    private boolean live;
    private int totalCompetitorCount;

    public GetMiniLeaderboardDTO() {
    }

    public ArrayList<MiniLeaderboardItemDTO> getItems() {
        return items;
    }

    public void addItem(MiniLeaderboardItemDTO item) {
        items.add(item);
    }

    public String getScoreCorrectionText() {
        return scoreCorrectionText;
    }

    public void setScoreCorrectionText(String scoreCorrectionText) {
        this.scoreCorrectionText = scoreCorrectionText;
    }

    public Date getLastScoreUpdate() {
        return lastScoreUpdate;
    }

    public void setLastScoreUpdate(Date lastScoreUpdate) {
        this.lastScoreUpdate = lastScoreUpdate;
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public int getTotalCompetitorCount() {
        return totalCompetitorCount;
    }

    public void setTotalCompetitorCount(int totalCompetitorCount) {
        this.totalCompetitorCount = totalCompetitorCount;
    }

    public boolean hasDifferentRaceCounts() {
        HashSet<Integer> uniqueRaceCounts = new HashSet<>();
        for (MiniLeaderboardItemDTO item : items) {
            uniqueRaceCounts.add(item.getRaceCount());
        }
        return uniqueRaceCounts.size() > 1;
    }
}
