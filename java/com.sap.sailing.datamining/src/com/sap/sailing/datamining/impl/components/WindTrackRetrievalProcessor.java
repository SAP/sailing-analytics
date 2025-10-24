package com.sap.sailing.datamining.impl.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.data.HasWindTrackContext;
import com.sap.sailing.datamining.impl.data.WindTrackWithContext;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;

/**
 * Retrieves all wind fixes from a {@link TrackedRace} that are provided by wind sources that are not currently
 * {@link TrackedRace#getWindSourcesToExclude() excluded}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class WindTrackRetrievalProcessor extends AbstractRetrievalProcessor<HasTrackedRaceContext, HasWindTrackContext> {

    public WindTrackRetrievalProcessor(ExecutorService executor,
            Collection<Processor<HasWindTrackContext, ?>> resultReceivers, int retrievalLevel,
            String retrievedDataTypeMessageKey) {
        super(HasTrackedRaceContext.class, HasWindTrackContext.class, executor, resultReceivers, retrievalLevel,
                retrievedDataTypeMessageKey);
    }

    @Override
    protected Iterable<HasWindTrackContext> retrieveData(HasTrackedRaceContext element) {
        Collection<HasWindTrackContext> windTracksWithContext = new ArrayList<>();
        if(element != null && element.getTrackedRace() != null) {
            final TrackedRace trackedRace = element.getTrackedRace();
            for (final WindSource windSource : trackedRace.getWindSources()) {
                if (isAborted()) {
                    break;
                }
                // see bug 6084: only use "real" wind source types, not those estimated
                if ((windSource.getType() == WindSourceType.RACECOMMITTEE || windSource.getType().canBeStored())
                        && !trackedRace.getWindSourcesToExclude().contains(windSource)) {
                    final WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
                    windTracksWithContext.add(new WindTrackWithContext(element, windTrack, windSource));
                }
            }
        }
        return windTracksWithContext;
    }

}
