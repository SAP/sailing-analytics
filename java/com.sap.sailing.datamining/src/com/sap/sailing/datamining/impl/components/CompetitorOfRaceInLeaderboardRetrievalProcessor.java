package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasRaceResultOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.impl.data.RaceResultOfCompetitorWithContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class CompetitorOfRaceInLeaderboardRetrievalProcessor
        extends AbstractRetrievalProcessor<HasTrackedRaceContext, HasRaceResultOfCompetitorContext> {

    public CompetitorOfRaceInLeaderboardRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasRaceResultOfCompetitorContext, ?>> resultReceivers, int retrievalLevel,
            String retrievedDataTypeMessageKey) {
        super(HasTrackedRaceContext.class, HasRaceResultOfCompetitorContext.class, executor, resultReceivers,
                retrievalLevel, retrievedDataTypeMessageKey);
    }

    @Override
    protected Iterable<HasRaceResultOfCompetitorContext> retrieveData(HasTrackedRaceContext element) {
        Collection<HasRaceResultOfCompetitorContext> raceResultsOfCompetitor = new ArrayList<>();
        HasLeaderboardContext leaderboardContext = element.getLeaderboardContext();
        for (Competitor competitor : element.getRace().getCompetitors()) {
            if (isAborted()) {
                break;
            }
            final Subject subject = SecurityUtils.getSubject();
            if (subject.isPermitted(competitor.getIdentifier().getStringPermission(DefaultActions.READ))) {
                HasRaceResultOfCompetitorContext raceResultOfCompetitorContext = new RaceResultOfCompetitorWithContext(
                        leaderboardContext, element.getRaceColumn(), competitor,
                        leaderboardContext.getLeaderboardGroupContext().getPolarDataService(), element);
                raceResultsOfCompetitor.add(raceResultOfCompetitorContext);
            }
        }
        return raceResultsOfCompetitor;
    }

}
