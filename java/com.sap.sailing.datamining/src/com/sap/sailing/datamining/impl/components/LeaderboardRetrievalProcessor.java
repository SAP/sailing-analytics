package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.impl.data.LeaderboardWithContext;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class LeaderboardRetrievalProcessor extends AbstractRetrievalProcessor<HasLeaderboardGroupContext, HasLeaderboardContext> {

    public LeaderboardRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasLeaderboardContext, ?>> resultReceivers, int retrievalLevel,
            String retrievedDataTypeMessageKey) {
        super(HasLeaderboardGroupContext.class, HasLeaderboardContext.class, executor, resultReceivers, retrievalLevel,
                retrievedDataTypeMessageKey);
    }

    @Override
    protected Iterable<HasLeaderboardContext> retrieveData(HasLeaderboardGroupContext element) {
        Collection<HasLeaderboardContext> leaderboardsWithContext = new ArrayList<>();
        for (Leaderboard leaderboard : element.getLeaderboardGroup().getLeaderboards()) {
            if (isAborted()) {
                break;
            }
            final Subject subject = SecurityUtils.getSubject();
            if (subject.isPermitted(leaderboard.getIdentifier().getStringPermission(DefaultActions.READ))) {
                leaderboardsWithContext.add(new LeaderboardWithContext(leaderboard, element));
            }
        }
        return leaderboardsWithContext;
    }

}
