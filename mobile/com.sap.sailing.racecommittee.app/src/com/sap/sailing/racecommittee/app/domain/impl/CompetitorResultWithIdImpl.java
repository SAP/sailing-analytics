package com.sap.sailing.racecommittee.app.domain.impl;

import androidx.annotation.Nullable;

import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sse.common.TimePoint;

import java.io.Serializable;

public class CompetitorResultWithIdImpl extends CompetitorResultImpl {
    private static final long serialVersionUID = 4373611281142932067L;

    private long mId;
    private Boat mBoat;

    public CompetitorResultWithIdImpl(long id, CompetitorResult result) {
        this(id, result.getCompetitorId(), result.getName(), result.getShortName(), result.getBoatName(),
                result.getBoatSailId(), result.getOneBasedRank(), result.getMaxPointsReason(), result.getScore(),
                result.getFinishingTime(), result.getComment(), result.getMergeState());
    }

    public CompetitorResultWithIdImpl(long id, CompetitorResult result, MaxPointsReason maxPointsReason) {
        this(id, result.getCompetitorId(), result.getName(), result.getShortName(), result.getBoatName(),
                result.getBoatSailId(), result.getOneBasedRank(), maxPointsReason, result.getScore(),
                result.getFinishingTime(), result.getComment(), result.getMergeState());
    }

    public CompetitorResultWithIdImpl(long id, Serializable competitorId, String competitorName, String shortName,
                                      @Nullable Boat boat, int oneBasedRank, MaxPointsReason maxPointsReason,
                                      Double score, TimePoint finishingTime, String comment, MergeState mergeState) {
        this(id, competitorId, competitorName, shortName,
                boat != null ? boat.getName() : null,
                boat != null ? boat.getSailID() : null,
                oneBasedRank, maxPointsReason, score, finishingTime, comment, mergeState);
        mBoat = boat;
    }

    public CompetitorResultWithIdImpl(long id, Serializable competitorId, String competitorName, String shortName,
                                      String boatName, String sailId, int oneBasedRank, MaxPointsReason maxPointsReason,
                                      Double score, TimePoint finishingTime, String comment, MergeState mergeState) {
        super(competitorId, competitorName, shortName, boatName, sailId, oneBasedRank, maxPointsReason, score,
                finishingTime, comment, mergeState);
        mId = id;
    }

    public long getId() {
        return mId;
    }

    public Boat getBoat() {
        return mBoat;
    }

    public void setBoat(Boat boat) {
        mBoat = boat;
    }
}
