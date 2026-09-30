package com.sap.sailing.windestimation.aggregator.outlierremoval;

import java.util.List;

import com.sap.sailing.windestimation.data.ManeuverWithEstimatedType;
import com.sap.sailing.windestimation.windinference.TwdFromManeuverCalculator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class MeanBasedOutlierRemovalWindEstimator extends AbstractOutlierRemovalWindEstimator {

    public MeanBasedOutlierRemovalWindEstimator(TwdFromManeuverCalculator twdCalculator) {
        super(twdCalculator);
    }

    @Override
    protected OutlierAnalysisResult analyzeOutlier(List<Pair<Bearing, ManeuverWithEstimatedType>> twdsWithManeuvers) {
        ScalableBearing twdSum = null;
        double likelihoodSum = 0;
        OutlierAnalysisResult outlierAnalysisResult = new OutlierAnalysisResult();
        for (Pair<Bearing, ManeuverWithEstimatedType> twdWithManeuvers : twdsWithManeuvers) {
            Bearing twd = twdWithManeuvers.getA();
            double confidence = twdWithManeuvers.getB().getConfidence();
            ScalableBearing scalableTwd = new ScalableBearing(twd).multiply(confidence);
            likelihoodSum += confidence;
            twdSum = twdSum == null ? scalableTwd : twdSum.add(scalableTwd);
        }
        if (twdSum != null) {
            Bearing avgTwd = twdSum.divide(likelihoodSum);
            for (Pair<Bearing, ManeuverWithEstimatedType> twdWithManeuver : twdsWithManeuvers) {
                Bearing twd = twdWithManeuver.getA();
                ManeuverWithEstimatedType maneuver = twdWithManeuver.getB();
                if (Math.abs(avgTwd.getDifferenceTo(twd).getDegrees()) <= MAX_DEVIATON_FROM_AVG_WIND_COURSE) {
                    outlierAnalysisResult.addIncludedManeuver(maneuver);
                } else {
                    outlierAnalysisResult.addExcludedManeuver(maneuver);
                }
            }
        }
        return outlierAnalysisResult;
    }

}
