package com.sap.sailing.racecommittee.app.domain.impl;

import java.util.List;
import java.util.Map;

import com.sap.sse.common.Util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LeaderboardResult {

    private Map<String, List<Util.Pair<Long, String>>> mResult;

    public LeaderboardResult(@NonNull Map<String, List<Util.Pair<Long, String>>> result) {
        mResult = result;
    }

    @Nullable
    public List<Util.Pair<Long, String>> getResult(@NonNull String raceName) {
        return mResult.get(raceName);
    }

}
