package com.sap.sailing.windestimation.windinference;

import java.util.List;

import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedType;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Converts maneuver classifications to wind fixes.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface WindTrackCalculator {

    List<WindWithConfidence<Pair<Position, TimePoint>>> getWindTrackFromManeuverClassifications(
            List<? extends SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> aggregatedManeuverClassifications);

}
