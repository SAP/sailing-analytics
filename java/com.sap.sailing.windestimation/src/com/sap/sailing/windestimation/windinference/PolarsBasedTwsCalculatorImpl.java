package com.sap.sailing.windestimation.windinference;

import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.windestimation.aggregator.polarsfitting.PolarsFittingWindEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;

/**
 * Determines TWS by matching SOG before and SOG after maneuver with boat class polars.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class PolarsBasedTwsCalculatorImpl implements TwsFromManeuverCalculator {

    private final PolarsFittingWindEstimation polarsFittingWindEstimation;

    public PolarsBasedTwsCalculatorImpl(PolarDataService polarService) {
        polarsFittingWindEstimation = new PolarsFittingWindEstimation(polarService);
    }

    @Override
    public Speed getWindSpeed(SimpleManeuverForEstimation maneuver, Bearing windCourse) {
        return polarsFittingWindEstimation.getWindSpeed(maneuver, windCourse);
    }

}
