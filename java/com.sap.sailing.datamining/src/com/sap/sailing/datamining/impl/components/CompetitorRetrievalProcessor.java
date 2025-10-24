package com.sap.sailing.datamining.impl.components;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.datamining.data.HasCompetitorContext;
import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.impl.data.CompetitorWithContext;
import com.sap.sse.common.Util;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class CompetitorRetrievalProcessor extends AbstractRetrievalProcessor<HasLeaderboardContext, HasCompetitorContext> {
    public CompetitorRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasCompetitorContext, ?>> resultReceivers, int retrievalLevel,
            String retrievedDataTypeMessageKey) {
        super(HasLeaderboardContext.class, HasCompetitorContext.class, executor, resultReceivers, retrievalLevel,
                retrievedDataTypeMessageKey);
    }

    @Override
    protected Iterable<HasCompetitorContext> retrieveData(HasLeaderboardContext element) {
        final Subject subject = SecurityUtils.getSubject();
        return Util.map(
                Util.filter(element.getLeaderboard().getCompetitors(),
                            c->subject.isPermitted(c.getIdentifier().getStringPermission(DefaultActions.READ))),
                        c->new CompetitorWithContext(c, element));
    }
}
