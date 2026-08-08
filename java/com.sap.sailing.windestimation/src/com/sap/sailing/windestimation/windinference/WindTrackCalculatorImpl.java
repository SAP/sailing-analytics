package com.sap.sailing.windestimation.windinference;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedType;
import com.sap.sailing.windestimation.util.WindUtil;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

/**
 * Uses provided {@link #twdCalculator} and {@link #twsCalculator} to derive wind fixes. TWS of all wind fixes is set to
 * the median TWS which is determined by {@link #twsCalculator} for each maneuver.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindTrackCalculatorImpl implements WindTrackCalculator {

    private final TwdFromManeuverCalculator twdCalculator;
    private final TwsFromManeuverCalculator twsCalculator;

    public WindTrackCalculatorImpl(TwdFromManeuverCalculator twdCalculator, TwsFromManeuverCalculator twsCalculator) {
        this.twdCalculator = twdCalculator;
        this.twsCalculator = twsCalculator;
    }

    @Override
    public List<WindWithConfidence<Pair<Position, TimePoint>>> getWindTrackFromManeuverClassifications(
            List<? extends SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> improvedManeuverClassifications) {
        List<WindWithConfidence<Pair<Position, TimePoint>>> windFixes = new ArrayList<>();
        for (SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation> maneuverWithEstimatedType : improvedManeuverClassifications) {
            Bearing windFrom = twdCalculator.getTwd(maneuverWithEstimatedType);
            if (windFrom != null) {
                final Bearing windTo = windFrom.reverse();
                SimpleManeuverForEstimation maneuver = maneuverWithEstimatedType.getManeuver();
                Speed avgWindSpeed = twsCalculator.getWindSpeed(maneuver, windTo);
                Wind wind = new WindImpl(maneuver.getManeuverPosition(), maneuver.getManeuverTimePoint(),
                        new KnotSpeedWithBearingImpl(avgWindSpeed.getKnots(), windTo));
                windFixes.add(new WindWithConfidenceImpl<>(wind, maneuverWithEstimatedType.getConfidence(),
                        new Pair<>(wind.getPosition(), wind.getTimePoint()), avgWindSpeed.getKnots() > 0));
            }
        }
        windFixes = WindUtil.getWindFixesWithMedianTws(windFixes);
        return windFixes;
    }

}
