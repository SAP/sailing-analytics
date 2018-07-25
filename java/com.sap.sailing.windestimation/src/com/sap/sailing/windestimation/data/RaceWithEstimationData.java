package com.sap.sailing.windestimation.data;

import java.util.List;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class RaceWithEstimationData<T> {

    private final String dbId;
    private final String regattaName;
    private final String raceName;
    private final List<CompetitorTrackWithEstimationData<T>> competitorTracks;

    public RaceWithEstimationData(String dbId, String regattaName, String raceName,
            List<CompetitorTrackWithEstimationData<T>> competitorTracks) {
        this.dbId = dbId;
        this.regattaName = regattaName;
        this.raceName = raceName;
        this.competitorTracks = competitorTracks;
    }

    public String getDbId() {
        return dbId;
    }

    public String getRaceName() {
        return raceName;
    }

    public List<CompetitorTrackWithEstimationData<T>> getCompetitorTracks() {
        return competitorTracks;
    }

    public String getRegattaName() {
        return regattaName;
    }

}
