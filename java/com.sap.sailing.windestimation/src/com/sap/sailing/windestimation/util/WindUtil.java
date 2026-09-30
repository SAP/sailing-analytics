package com.sap.sailing.windestimation.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

import smile.sort.QuickSelect;

/**
 * Util class for diverse adjustments of lists with wind fixes
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindUtil {

    private WindUtil() {
    }

    public static List<WindWithConfidence<Void>> getWindFixesWithFixedConfidence(
            List<WindWithConfidence<Void>> windFixes, double fixedConfidence) {
        List<WindWithConfidence<Void>> result = new ArrayList<>();
        for (WindWithConfidence<Void> windWithConfidence : windFixes) {
            WindWithConfidence<Void> newWindWithConfidence = new WindWithConfidenceImpl<>(
                    windWithConfidence.getObject(), fixedConfidence, windWithConfidence.getRelativeTo(),
                    windWithConfidence.useSpeed());
            result.add(newWindWithConfidence);
        }
        return result;
    }

    public static List<WindWithConfidence<Pair<Position, TimePoint>>> getWindFixesWithMedianTws(
            List<WindWithConfidence<Pair<Position, TimePoint>>> windFixes) {
        if (windFixes.size() <= 1) {
            return windFixes;
        }
        double[] windSpeedsInKnots = new double[windFixes.size()];
        int i = 0;
        int zerosCount = 0;
        for (WindWithConfidence<Pair<Position, TimePoint>> windFix : windFixes) {
            double windSpeedInKnots = windFix.getObject().getKnots();
            if (windSpeedInKnots > 0) {
                windSpeedsInKnots[i++] = windFix.getObject().getKnots();
            } else {
                zerosCount++;
            }
        }
        if (zerosCount == windSpeedsInKnots.length) {
            return windFixes;
        }
        if (zerosCount > 0) {
            windSpeedsInKnots = Arrays.copyOfRange(windSpeedsInKnots, 0, windSpeedsInKnots.length - zerosCount);
        }
        double avgWindSpeedInKnots = windSpeedsInKnots.length == 1 ? windSpeedsInKnots[0]
                : QuickSelect.median(windSpeedsInKnots);
        List<WindWithConfidence<Pair<Position, TimePoint>>> result = new ArrayList<>();
        for (WindWithConfidence<Pair<Position, TimePoint>> windFix : windFixes) {
            Wind wind = windFix.getObject();
            WindWithConfidence<Pair<Position, TimePoint>> newWindFix = new WindWithConfidenceImpl<>(
                    new WindImpl(wind.getPosition(), wind.getTimePoint(),
                            new KnotSpeedWithBearingImpl(avgWindSpeedInKnots, wind.getBearing())),
                    windFix.getConfidence(), windFix.getRelativeTo(), avgWindSpeedInKnots > 0);
            result.add(newWindFix);
        }
        return result;
    }

}
