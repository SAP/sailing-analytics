package com.sap.sailing.polars.datamining.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.polars.datamining.data.HasLeaderboardPolarContext;
import com.sap.sailing.polars.datamining.data.impl.LeaderboardWithPolarContext;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class PolarLeaderboardRetrievalProcessor extends AbstractRetrievalProcessor<HasLeaderboardGroupContext, HasLeaderboardPolarContext> {

    public PolarLeaderboardRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasLeaderboardPolarContext, ?>> resultReceivers, int retrievalLevel,
            String retrievedDataTypeMessageKey) {
        super(HasLeaderboardGroupContext.class, HasLeaderboardPolarContext.class, executor, resultReceivers,
                retrievalLevel, retrievedDataTypeMessageKey);
    }

    @Override
    protected Iterable<HasLeaderboardPolarContext> retrieveData(HasLeaderboardGroupContext element) {
        Set<HasLeaderboardPolarContext> leaderboardsWithContext = new HashSet<>();
        for (Leaderboard leaderboard : element.getLeaderboardGroup().getLeaderboards()) {
            if (isAborted()) {
                break;
            }
            final Subject subject = SecurityUtils.getSubject();
            if (subject.isPermitted(leaderboard.getIdentifier().getStringPermission(DefaultActions.READ))) {
                leaderboardsWithContext.add(new LeaderboardWithPolarContext(leaderboard, element));
            }
        }
        return leaderboardsWithContext;
    }

}
