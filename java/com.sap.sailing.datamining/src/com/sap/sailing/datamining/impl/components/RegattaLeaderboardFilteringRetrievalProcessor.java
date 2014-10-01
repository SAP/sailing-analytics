package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractSimpleFilteringRetrievalProcessor;
import com.sap.sse.datamining.shared.annotations.DataRetriever;

@DataRetriever(dataType=RegattaLeaderboard.class,
               groupName=Activator.dataRetrieverGroupName,
               level=1)
public class RegattaLeaderboardFilteringRetrievalProcessor extends
        AbstractSimpleFilteringRetrievalProcessor<LeaderboardGroup, RegattaLeaderboard> {

    public RegattaLeaderboardFilteringRetrievalProcessor(ExecutorService executor,
            Collection<Processor<RegattaLeaderboard, ?>> resultReceivers, FilterCriterion<RegattaLeaderboard> criteria) {
        super(LeaderboardGroup.class, RegattaLeaderboard.class, executor, resultReceivers, criteria);
    }

    @Override
    protected Iterable<RegattaLeaderboard> retrieveData(LeaderboardGroup element) {
        Collection<RegattaLeaderboard> regattaLeaderboards = new ArrayList<>();
        for (Leaderboard leaderboard : element.getLeaderboards()) {
            if (leaderboard instanceof RegattaLeaderboard) {
                regattaLeaderboards.add((RegattaLeaderboard) leaderboard);
            }
        }
        return regattaLeaderboards;
    }

}
