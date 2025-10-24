package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.impl.data.TrackedLegOfCompetitorWithContext;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

public class TrackedLegOfCompetitorRetrievalProcessor extends AbstractRetrievalProcessor<HasTrackedLegContext, HasTrackedLegOfCompetitorContext> {
    /**
     * Settings will be used to control the retrieval of tack type segments for the
     * corresponding statistics such as distance/duration relation between long and
     * short tack for the leg of the competitor
     */
    private final TackTypeSegmentsDataMiningSettings settings;

    public TrackedLegOfCompetitorRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasTrackedLegOfCompetitorContext, ?>> resultReceivers,
            TackTypeSegmentsDataMiningSettings settings, int retrievalLevel, String retrievedDataTypeMessageKey) {
        super(HasTrackedLegContext.class, HasTrackedLegOfCompetitorContext.class, executor, resultReceivers,
                retrievalLevel, retrievedDataTypeMessageKey);
        this.settings = settings;
    }

    @Override
    protected Iterable<HasTrackedLegOfCompetitorContext> retrieveData(HasTrackedLegContext element) {
        Collection<HasTrackedLegOfCompetitorContext> trackedLegOfCompetitorsWithContext = new ArrayList<>();
        for (Competitor competitor : element.getTrackedRaceContext().getTrackedRace().getRace().getCompetitors()) {
            if (isAborted()) {
                break;
            }
            final Subject subject = SecurityUtils.getSubject();
            if (subject.isPermitted(competitor.getIdentifier().getStringPermission(SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC))) {
                HasTrackedLegOfCompetitorContext trackedLegOfCompetitorWithContext = new TrackedLegOfCompetitorWithContext(element, element.getTrackedLeg().getTrackedLeg(competitor), settings);
                trackedLegOfCompetitorsWithContext.add(trackedLegOfCompetitorWithContext);
            }
        }
        return trackedLegOfCompetitorsWithContext;
    }

}
