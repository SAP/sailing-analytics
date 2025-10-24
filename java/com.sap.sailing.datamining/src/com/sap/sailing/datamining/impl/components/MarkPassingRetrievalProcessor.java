package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.datamining.data.HasMarkPassingContext;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.impl.data.MarkPassingWithContext;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sse.common.TimePoint;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;

public class MarkPassingRetrievalProcessor extends AbstractRetrievalProcessor<HasTrackedLegOfCompetitorContext, HasMarkPassingContext> {

    public MarkPassingRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasMarkPassingContext, ?>> resultReceivers, int retrievalLevel,
            String retrievedDataTypeMessageKey) {
        super(HasTrackedLegOfCompetitorContext.class, HasMarkPassingContext.class, executor, resultReceivers,
                retrievalLevel, retrievedDataTypeMessageKey);
    }

    @Override
    protected Iterable<HasMarkPassingContext> retrieveData(HasTrackedLegOfCompetitorContext element) {
        Collection<HasMarkPassingContext> maneuversWithContext = new ArrayList<>();
        TimePoint finishTime = element.getTrackedLegOfCompetitor().getFinishTime();
        if (finishTime != null) {
            try {
                Iterable<Maneuver> maneuvers = element.getTrackedLegOfCompetitor().getManeuvers(finishTime, false);
                for (Maneuver maneuver : maneuvers) {
                    if (isAborted()) {
                        break;
                    }
                    if (maneuver.isMarkPassing()) {
                        maneuversWithContext.add(new MarkPassingWithContext(element, maneuver));
                    }
                }
            } catch (NoWindException e) {
                throw new IllegalStateException("No wind retrieving the mark passings", e);
            }
        }
        return maneuversWithContext;
    }

}
