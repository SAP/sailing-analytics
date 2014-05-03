package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.impl.data.TrackedLegOfCompetitorWithContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sse.datamining.components.FilterCriteria;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractSimpleFilteringRetrievalProcessor;

public class TrackedLegOfCompetitorFilteringRetrievalProcessor extends
        AbstractSimpleFilteringRetrievalProcessor<HasTrackedLegContext, HasTrackedLegOfCompetitorContext> {

    public TrackedLegOfCompetitorFilteringRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasTrackedLegOfCompetitorContext>> resultReceivers,
            FilterCriteria<HasTrackedLegOfCompetitorContext> criteria) {
        super(executor, resultReceivers, criteria);
    }

    @Override
    protected Iterable<HasTrackedLegOfCompetitorContext> retrieveData(HasTrackedLegContext element) {
        Collection<HasTrackedLegOfCompetitorContext> trackedLegOfCompetitorsWithContext = new ArrayList<>();
        for (Competitor competitor : element.getRace().getCompetitors()) {
            HasTrackedLegOfCompetitorContext trackedLegOfCompetitorWithContext = new TrackedLegOfCompetitorWithContext(element, element.getTrackedLeg().getTrackedLeg(competitor));
            trackedLegOfCompetitorsWithContext.add(trackedLegOfCompetitorWithContext);
        }
        return trackedLegOfCompetitorsWithContext;
    }

}
