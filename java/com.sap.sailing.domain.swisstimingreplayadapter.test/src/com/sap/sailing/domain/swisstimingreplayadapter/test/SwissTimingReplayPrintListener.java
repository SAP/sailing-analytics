package com.sap.sailing.domain.swisstimingreplayadapter.test;

import java.util.Date;
import java.util.logging.Logger;

import com.sap.sailing.domain.swisstimingreplayadapter.CompetitorStatus;
import com.sap.sailing.domain.swisstimingreplayadapter.SwissTimingReplayListener;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.BoatType;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.MarkType;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.RaceStatus;
import com.sap.sailing.domain.swisstimingreplayadapter.impl.Weather;

public class SwissTimingReplayPrintListener implements SwissTimingReplayListener {
    private static final Logger logger = Logger.getLogger(SwissTimingReplayPrintListener.class.getName());

    private long referenceTimestamp;
    private int referenceLatitude;
    private int referenceLongitude;
    
    @Override
    public void raceID(String text) {
        logger.fine(()->"rsc_cid - text: " + text);
    }
    
    @Override
    public void referenceTimestamp(long referenceTimestamp) {
        this.referenceTimestamp = referenceTimestamp;
        logger.fine(()->"referenceTimestamp - referenceTimestamp: " + new Date(this.referenceTimestamp));
    }
    
    @Override
    public void referenceLocation(int latitude, int longitude) {
        this.referenceLatitude = latitude;
        this.referenceLongitude = longitude;
        logger.fine(()->"referenceLocation - latitude: " + (double) this.referenceLatitude / 1E7 + ", longitude: "
                + (double) this.referenceLongitude / 1E7);
    }

    @Override
    public void keyFrameIndexPosition(int keyFrameIndexPosition) {
        logger.fine(()->"keyFrameIndexPosition - keyFrameIndexPosition: " + keyFrameIndexPosition);
    }
    
    @Override
    public void keyFrameIndex(int keyFrameIndex) {
        logger.fine(()->"keyFrameIndex - keyFrameIndex:" + keyFrameIndex); 
    }
    
    @Override
    public void competitorsCount(short competitorsCount) {
        logger.fine(()->"competitorsCount - competitorsCount: " + competitorsCount);
    }
    
    @Override
    public void competitor(int hashValue, String nation, String sailNumber, String name, CompetitorStatus competitorStatus,
            BoatType boatType, short cRank_Bracket, short cnPoints_x10_Bracket,
            short ctPoints_x10_Winner) {
        logger.fine(()->"competitor - hashValue: " + hashValue + ", nation: " + nation + ", sailNumber: "
                + sailNumber + ", name: " + name + ", competitorStatus: " + competitorStatus + ", boatType: "
                + boatType + ", cRank_Bracket: " + cRank_Bracket + ", cnPoints_x10_Bracket: " + cnPoints_x10_Bracket
                + ", ctPoints_x10_Winner: " + ctPoints_x10_Winner);
    }

    @Override
    public void mark(MarkType markType, String identifier, byte index, String id1, String id2, short windSpeed,
            short windDirection) {
        logger.fine(()->"mark - markType: " + markType + ", identifier: " + identifier + ", index: " + index
                        + ", id1: " + id1 + ", id2: " + id2 + ", windSpeed: " + windSpeed + ", windDirection: "
                        + windDirection);
    }

    @Override
    public void frameMetaData(byte cid, int raceTime, int startTime, int estimatedStartTime, RaceStatus raceStatus,
            short distanceToNextMark, Weather weather, short humidity, short temperature, String messageText,
            byte cFlag, byte rFlag, byte duration, short nm) {
        logger.fine(()->"frame - cid: " + cid + ", raceTime: " + raceTime + ", startTime: " + startTime
                + ", estimatedStartTime: " + estimatedStartTime + ", raceStatus: " + raceStatus
                + ", distanceToNextMark: " + distanceToNextMark + ", weather: " + weather + ", humidity: " + humidity
                + ", temperature: " + temperature + ", messageText: " + messageText + ", cFlag: " + cFlag + ", rFlag: "
                + rFlag + ", duration: " + duration + ", nm: " + nm);
    }

    @Override
    public void ranking(int hashValue, short rank, short rankIndex, short racePoints, CompetitorStatus competitorStatus,
            short finishRank, short finishRankIndex, int gap, int raceTime) {
        logger.fine(()->"ranking - hashValue: " + hashValue + ", rank: " + rank + ", rankIndex: " + rankIndex
                + ", racePoints: " + racePoints + ", competitorStatus: " + competitorStatus + ", finishRank: "
                + finishRank + ", finishRankIndex: " + finishRankIndex + ", gap: " + gap + ", raceTime: " + raceTime);

    }

    @Override
    public void rankingsCount(short entriesCount) {
        logger.fine(()->"rankingsCount - entriesCount: " + entriesCount);
    }

    @Override
    public void rankingMark(short marksRank, short marksRankIndex, int marksGap, int marksRaceTime) {
        logger.fine(()->"rankingMark - marksRank: " + marksRank + ", marksRankIndex: " + marksRankIndex
                + ", marksGap: " + marksGap + ", marksRaceTime: " + marksRaceTime);

    }

    @Override
    public void trackersCount(short trackersCount) {
        logger.fine(()->"trackersCount - trackersCount: " + trackersCount);
        
    }

    @Override
    public void trackers(int hashValue, int latitude, int longitude, short cog, short sog, short average_sog,
            short vmg, CompetitorStatus competitorStatus, short rank, short dtl, short dtnm, short nm, short pRank, short ptPoints,
            short pnPoints) {
        final int computedLatitude = referenceLatitude - latitude;
        final int computedLongitude = referenceLongitude - longitude;
        logger.fine(()->"trackers - hashValue: " + hashValue + ", latitude: " + (double) computedLatitude / 1E7
                + ", longitude: " + (double) computedLongitude / 1E7 + ", cog: " + cog + ", sog: " + sog + ", average_sog: "
                + average_sog + ", vmg: " + vmg + ", competitorStatus: " + competitorStatus + ", rank: " + rank
                + ", dtl: " + dtl + ", dtnm: " + dtnm + ", nm: " + nm + ", pRank: " + pRank + ", ptPoints: " + ptPoints
                + ", pnPoints: " + pnPoints);
    }

    @Override
    public void eot() {
        logger.fine(()->"EOT");
    }

    @Override
    public void progress(double progress) {
        logger.fine(()->"Progress: "+progress);
    }
}
