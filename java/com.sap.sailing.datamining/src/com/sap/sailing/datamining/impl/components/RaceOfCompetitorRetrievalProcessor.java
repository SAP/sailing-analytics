package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.datamining.data.HasRaceOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.impl.data.RaceOfCompetitorWithContext;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

public class RaceOfCompetitorRetrievalProcessor extends AbstractRetrievalProcessor<HasTrackedRaceContext, HasRaceOfCompetitorContext> {
    /**
     * Settings will be used to control the retrieval of tack type segments for the
     * corresponding statistics such as distance/duration relation between long and
     * short tack for the race of the competitor
     */
    private final TackTypeSegmentsDataMiningSettings settings;

    public RaceOfCompetitorRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasRaceOfCompetitorContext, ?>> resultReceivers,
            TackTypeSegmentsDataMiningSettings settings, int retrievalLevel, String retrievedDataTypeMessageKey) {
        super(HasTrackedRaceContext.class,
                HasRaceOfCompetitorContext.class, executor, resultReceivers, retrievalLevel,
                retrievedDataTypeMessageKey);
        this.settings = settings;
    }

    @Override
    protected Iterable<HasRaceOfCompetitorContext> retrieveData(HasTrackedRaceContext element) {
        Collection<HasRaceOfCompetitorContext> raceOfCompetitorsWithContext = new ArrayList<>();
        if (element.getTrackedRace() != null) {
            for (Competitor competitor : element.getTrackedRace().getRace().getCompetitors()) {
                if (isAborted()) {
                    break;
                }
                final Subject subject = SecurityUtils.getSubject();
                if (subject.isPermitted(competitor.getIdentifier().getStringPermission(SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC))) {
                    HasRaceOfCompetitorContext raceOfCompetitorWithContext = new RaceOfCompetitorWithContext(element, competitor, settings);
                    raceOfCompetitorsWithContext.add(raceOfCompetitorWithContext);
                }
            }
        }
        return raceOfCompetitorsWithContext;
    }
}
