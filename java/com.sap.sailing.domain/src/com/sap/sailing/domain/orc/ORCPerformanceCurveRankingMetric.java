package com.sap.sailing.domain.orc;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Marker interface for all ORC Polar Curve Scoring (PCS) ranking
 * metrics ({@link RankingMetric}).
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ORCPerformanceCurveRankingMetric {
    Speed getImpliedWind(Competitor competitor, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache)
            throws FunctionEvaluationException, MaxIterationsExceededException;

    ORCCertificate getCertificate(Boat boat);
}
