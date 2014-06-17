package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.sap.sailing.domain.common.Bounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.confidence.BearingWithConfidence;
import com.sap.sailing.domain.common.confidence.BearingWithConfidenceCluster;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.common.dto.PositionDTO;
import com.sap.sailing.domain.common.impl.BoundsImpl;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.gwt.ui.simulator.streamlets.PositionDTOAndDateWeigher.AverageLatitudeProvider;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

/**
 * Implements the {@link VectorField} interface by providing real wind data from a <code>TrackedRace</code> which has
 * been received in the form of one or more {@link WindInfoForRaceDTO} objects. The field uses time and space distances
 * to weigh the wind measurements provided, leading to a spatially-resolved wind field that can be visualized. Its
 * bounds are infinite as time/space-weighed averages can generally be computed anywhere.
 * <p>
 * 
 * The vectors produced by this field are sized such that their x/y values represent 1/60th of a longitude/latitude
 * degree per hour which resembles knots (nautical miles per hour).
 * <p>
 * 
 * The maximum wind speed assumed by this wind field is 40kts which is used to calculate line widths and particle
 * speeds.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class WindInfoForRaceVectorField implements VectorField {
    private static final double MAX_WIND_SPEED_IN_KNOTS = 40;
    private final Bounds infiniteBounds = new BoundsImpl(new DegreePosition(-90, -180), new DegreePosition(90, 180));
    private final Weigher<Pair<PositionDTO, Date>> weigher;
    private final Comparator<WindDTO> windByMeasureTimePointComparator = new Comparator<WindDTO>() {
        @Override
        public int compare(WindDTO o1, WindDTO o2) {
            return o1.measureTimepoint > o2.measureTimepoint ? 1 : o1.measureTimepoint == o2.measureTimepoint ? 0 : -1;
        }
    };
    private final WindInfoForRaceDTO windInfoForRace;
    
    public WindInfoForRaceVectorField(WindInfoForRaceDTO windInfoForRace, AverageLatitudeProvider averageLatitudeDeg) {
        this.windInfoForRace = windInfoForRace;
        weigher = new PositionDTOAndDateWeigher(/* half confidence after milliseconds */3000,
                /* halfConfidenceDistance */new MeterDistance(1000), averageLatitudeDeg);
    }
    
    @Override
    public boolean inBounds(Position p) {
        // all positions are always considered in bounds as we'll always try to interpolate/extrapolate
        return true;
    }

    @Override
    public Vector getVector(Position p, Date at) {
        final Pair<PositionDTO, Date> request = new Pair<>(new PositionDTO(p.getLatDeg(), p.getLngDeg()), at);
        int speedCount = 0;
        double knotSpeedSum = 0;
        final BearingWithConfidenceCluster<Pair<PositionDTO, Date>> bearingCluster = new BearingWithConfidenceCluster<>(weigher);
        for (final Entry<WindSource, WindTrackInfoDTO> windSourceAndWindTrack : windInfoForRace.windTrackInfoByWindSource.entrySet()) {
            if (!Util.contains(windInfoForRace.windSourcesToExclude, windSourceAndWindTrack.getKey())) {
                WindDTO timewiseClosestFixForWindSource = getTimewiseClosestFix(windSourceAndWindTrack.getValue().windFixes, at);
                if (timewiseClosestFixForWindSource != null) {
                    if (windSourceAndWindTrack.getKey().getType().useSpeed()) {
                        speedCount++;
                        knotSpeedSum += timewiseClosestFixForWindSource.dampenedTrueWindSpeedInKnots;
                    }
                    Pair<PositionDTO, Date> fix = new Pair<>(timewiseClosestFixForWindSource.position, new Date(
                            timewiseClosestFixForWindSource.measureTimepoint));
                    bearingCluster.add(new BearingWithConfidenceImpl<Util.Pair<PositionDTO, Date>>(
                            new DegreeBearingImpl(timewiseClosestFixForWindSource.dampenedTrueWindBearingDeg), weigher
                                    .getConfidence(fix, request), request));
                }
            }
        }
        final BearingWithConfidence<Pair<PositionDTO, Date>> bearing = bearingCluster.getAverage(request);
        final Vector result;
        if (bearing != null && bearing.getObject() != null) {
            final double bearingRad = bearing.getObject().getRadians();
            final double speedInKnots = knotSpeedSum / speedCount;
            result = new Vector(speedInKnots * Math.sin(bearingRad), speedInKnots * Math.cos(bearingRad));
        } else {
            result = null;
        }
        return result;
    }

    private WindDTO getTimewiseClosestFix(List<WindDTO> windFixes, Date at) {
        final WindDTO result;
        if (windFixes == null || windFixes.isEmpty()) {
            result = null;
        } else {
            final WindDTO atDummy = new WindDTO();
            atDummy.measureTimepoint = at.getTime();
            int pos = Collections.binarySearch(windFixes, atDummy, windByMeasureTimePointComparator);
            if (pos < 0) {
                pos = (-pos) - 1; // now pos points at the insertion point
                if (pos == 0
                        || (pos < windFixes.size() &&
                            Math.abs(windFixes.get(pos).measureTimepoint - at.getTime()) < Math.abs(windFixes
                                .get(pos - 1).measureTimepoint - at.getTime()))) {
                    result = windFixes.get(pos);
                } else {
                    // pos doesn't point to the first element, nor is the element at pos time-wise closer than the element at pos-1
                    // or pos points beyond the end of the list
                    result = windFixes.get(pos-1);
                }
            } else {
                result = windFixes.get(pos);
            }
        }
        return result;
    }

    @Override
    public double getMotionScale(int zoomLevel) {
        // This implementation is copied from SimulatorField, hoping it does something useful in combination with
        // the Swarm implementation.
        return 0.07 * Math.pow(1.6, Math.min(1.0, 6.0 - zoomLevel));
    }
    
    @Override
    public double getParticleWeight(Position p, Vector v) {
        return v == null ? 0 : (v.length() / MAX_WIND_SPEED_IN_KNOTS);
    }

    /**
     * The implementation uses 3 coordinate space units (usually mapping to pixels if no zoom or other transformation is
     * in place for the Context2d canvas) for {@link #MAX_WIND_SPEED_IN_KNOTS}, decreasing linearly to 0 for zero speeds.
     */
    @Override
    public double getLineWidth(double speed) {
        return 3.*speed/MAX_WIND_SPEED_IN_KNOTS;
    }

    @Override
    public Bounds getFieldCorners() {
        return infiniteBounds;
    }

    @Override
    public double getParticleFactor() {
        return 2.0;
    }

    /**
     * {@link #MAX_WIND_SPEED_IN_KNOTS maximum wind speed} is fully opaque; zero speed is fully transparent
     */
    @Override
    public String getColor(double speed) {
        return "rgba(255,255,255,"+Math.min(255, Math.round(255.*speed/MAX_WIND_SPEED_IN_KNOTS))+")";
    }

}
