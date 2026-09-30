package com.sap.sailing.polars.datamining.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.WindSpeedSteppingWithMaxDistance;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.polars.datamining.data.HasCompetitorPolarContext;
import com.sap.sailing.polars.datamining.data.HasGPSFixPolarContext;
import com.sap.sailing.polars.datamining.data.impl.GPSFixWithPolarContext;
import com.sap.sailing.polars.datamining.data.impl.PolarStatisticImpl;
import com.sap.sailing.polars.datamining.data.impl.SpeedClusterGroup;
import com.sap.sailing.polars.datamining.shared.PolarDataMiningSettings;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.impl.components.AbstractRetrievalProcessor;

/**
 * Essential retriever in the polar datamining pipeline that also handles some standard filtering.
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarGPSFixRetrievalProcessor extends AbstractRetrievalProcessor<HasCompetitorPolarContext, HasGPSFixPolarContext> {

    private final PolarDataMiningSettings settings;

    public PolarGPSFixRetrievalProcessor(ExecutorService executor, Collection<Processor<HasGPSFixPolarContext, ?>> resultReceivers,
            PolarDataMiningSettings settings, int retrievalLevel, String retrievedDataTypeMessageKey) {
        super(HasCompetitorPolarContext.class, HasGPSFixPolarContext.class, executor, resultReceivers, retrievalLevel,
                retrievedDataTypeMessageKey);
        this.settings = settings;
    }

    @Override
    protected Iterable<HasGPSFixPolarContext> retrieveData(HasCompetitorPolarContext element) {
        ClusterGroup<Speed> windSpeedRangeGroup = toClusterGroup(settings.getWindSpeedStepping());
        TrackedRace trackedRace = element.getTrackedRace();
        Competitor competitor = element.getCompetitor();
        Leg leg = element.getLeg();
        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        TrackedLegOfCompetitor trackedLeg = trackedRace.getTrackedLeg(competitor, leg);
        TimePoint startTime = trackedLeg.getStartTime();
        TimePoint finishTime = trackedLeg.getFinishTime();
        Set<HasGPSFixPolarContext> result = new HashSet<>();
        if (startTime != null && finishTime != null) {
            final Set<WindSource> windSourcesToIgnoreForSpeed = PolarStatisticImpl.collectWindSourcesToIgnoreForSpeed(trackedRace, settings.useOnlyWindGaugesForWindSpeed());
            track.lockForRead();
            try {
                Iterable<GPSFixMoving> fixes = track.getFixes(startTime, true, finishTime, false);
                for (GPSFixMoving fix : fixes) {
                    if (isAborted()) {
                        break;
                    }
                    WindWithConfidence<Pair<Position, TimePoint>> wind = trackedRace.getWindWithConfidence(
                            fix.getPosition(),
                            fix.getTimePoint(),
                            windSourcesToIgnoreForSpeed);
                    if (wind != null && (settings.applyMinimumWindConfidence() ?  wind.getConfidence() >= settings.getMinimumWindConfidence() : true)) {
                        GPSFixWithPolarContext potentialResult = new GPSFixWithPolarContext(fix, trackedRace, windSpeedRangeGroup, competitor,
                                settings, wind, element);
                        if (!potentialResult.getWindSpeedRange().getSignifier().equals("null") && !track.hasDirectionChange(fix.getTimePoint(), 5)) {
                            result.add(potentialResult);
                        }
                    }
                }
            } finally {
                track.unlockAfterRead();
            }
        }
        return result;
    }

    private ClusterGroup<Speed> toClusterGroup(WindSpeedSteppingWithMaxDistance windSpeedStepping) {
        return SpeedClusterGroup.createSpeedClusterGroupFrom(windSpeedStepping);
    }
}
