package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.LatLngBounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.confidence.BearingWithConfidence;
import com.sap.sailing.domain.common.confidence.BearingWithConfidenceCluster;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sailing.gwt.ui.simulator.streamlets.PositionDTOWeigher.AverageLatitudeProvider;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Implements the {@link VectorField} interface by providing real wind data from a <code>TrackedRace</code> which has
 * been received in the form of one or more {@link WindInfoForRaceDTO} objects. The field uses spatial distances to
 * weigh the wind measurements provided, assuming there is always only one fix contained in each wind track, leading to
 * a spatially-resolved wind field that can be visualized. Its bounds are infinite as time/space-weighed averages can
 * generally be computed anywhere.
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
public class WindInfoForRaceVectorField implements VectorField, AverageLatitudeProvider {
    private static final double MAX_WIND_SPEED_IN_KNOTS = 40;
    private final LatLngBounds infiniteBounds = LatLngBounds.newInstance(LatLng.newInstance(-90, -180), LatLng.newInstance(90, 180));
    private final Weigher<Position> weigher;
    private double averageLatitudeCosine;
    private double knotsInDegreePerFrame;
    private long latitudeCount;
    private double latitudeSum;
    private final CoordinateSystem coordinateSystem;
    private final BearingWithConfidenceCluster<Position> bearingCluster;
    private final WindInfoForRaceDTO windInfoForRace;
    
    /**
     * @param windInfoForRace
     *            the underlying wind data; updated outside of this class, but this instance expects to be
     *            {@link #updateWindInfo(WindInfoForRaceDTO) notified} about such updates because it will take the
     *            opportunity to update the {@link BearingWithConfidenceCluster cluster of wind directions} only once
     *            per update so that it doesn't need to be re-constructed for each particle that is to be animated.
     */
    public WindInfoForRaceVectorField(WindInfoForRaceDTO windInfoForRace, double framesPerSecond, CoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
        this.windInfoForRace = windInfoForRace;
        this.knotsInDegreePerFrame = 1.0 / (60*3600) / framesPerSecond; // 1kn = 1/60 deg/h = 1/(60*3600) deg/s
        weigher = new PositionDTOWeigher(/* halfConfidenceDistance */new MeterDistance(100), this);
        bearingCluster = new BearingWithConfidenceCluster<>(weigher);
    }
    
    /**
     * Replaces the internal structures of the final {@link #windInfoForRace} based on the contents of {@code newWindInfo} and
     * updates the {@link #bearingCluster} to reflect the new wind fixes with their wind directions and confidences.
     */
    public void updateWindInfo(WindInfoForRaceDTO newWindInfo) {
        // merge the new wind fixes into the existing WindInfoForRaceDTO structure, updating min/max
        // confidences
        windInfoForRace.windTrackInfoByWindSource = newWindInfo.windTrackInfoByWindSource;
        bearingCluster.clear();
        for (final Entry<WindSource, WindTrackInfoDTO> windSourceAndWindTrack : windInfoForRace.windTrackInfoByWindSource.entrySet()) {
            if (!Util.contains(windInfoForRace.windSourcesToExclude, windSourceAndWindTrack.getKey())) {
                 final List<WindDTO> windFixes = windSourceAndWindTrack.getValue().windFixes;
                 if (windFixes != null && !windFixes.isEmpty()) {
                     WindDTO timewiseClosestFixForWindSource = windFixes.get(0);
                     if (timewiseClosestFixForWindSource != null) {
                         final double confidence = timewiseClosestFixForWindSource.confidence == null ? 1 : timewiseClosestFixForWindSource.confidence;
                         bearingCluster.add(new BearingWithConfidenceImpl<Position>(
                                 new DegreeBearingImpl(timewiseClosestFixForWindSource.dampenedTrueWindBearingDeg), confidence, timewiseClosestFixForWindSource.position));
                     }
                 }
             }
         }
        updateAverageLatitudeDeg(newWindInfo);
    }
    
    private void updateAverageLatitudeDeg(WindInfoForRaceDTO windInfoForRace) {
        for (Entry<WindSource, WindTrackInfoDTO> windSourceAndTrack : windInfoForRace.windTrackInfoByWindSource.entrySet()) {
            for (WindDTO wind : windSourceAndTrack.getValue().windFixes) {
                if (wind.position != null) {
                    latitudeSum += wind.position.getLatDeg();
                    latitudeCount++;
                }
            }
        }
        if (latitudeCount > 0) {
            setAverageLatitudeDeg(latitudeSum / latitudeCount);
        }
    }

    /**
     * Sets the average latitude used for the simplified approximating distance calculation. Until called, 0.0 is
     * assumed.
     */
    public void setAverageLatitudeDeg(double averageLatitudeDeg) {
        this.averageLatitudeCosine = Math.cos(averageLatitudeDeg/180.0*Math.PI);
    }
    
    @Override
    public double getCosineOfAverageLatitude() {
        return averageLatitudeCosine;
    }

    @Override
    public boolean inBounds(Position p) {
        // all positions are always considered in bounds as we'll always try to interpolate/extrapolate
        return true;
    }

    @Override
    public boolean inBounds(LatLng p) {
        // all positions are always considered in bounds as we'll always try to interpolate/extrapolate
        return true;
    }

    @Override
    public Vector getVector(final LatLng mappedPosition, final Date at) {
        final Position p = coordinateSystem.getPosition(mappedPosition);
        double speedConfidenceSum = 0;
        double knotSpeedSumScaledByConfidence = 0;
        for (final Entry<WindSource, WindTrackInfoDTO> windSourceAndWindTrack : windInfoForRace.windTrackInfoByWindSource.entrySet()) {
           if (!Util.contains(windInfoForRace.windSourcesToExclude, windSourceAndWindTrack.getKey())) {
                final List<WindDTO> windFixes = windSourceAndWindTrack.getValue().windFixes;
                if (windFixes != null && !windFixes.isEmpty()) {
                    WindDTO timewiseClosestFixForWindSource = windFixes.get(0);
                    if (timewiseClosestFixForWindSource != null) {
                        final double confidence = (timewiseClosestFixForWindSource.confidence == null ? 1 : timewiseClosestFixForWindSource.confidence) *
                                weigher.getConfidence(timewiseClosestFixForWindSource.position, p);
                        if (windSourceAndWindTrack.getKey().getType().useSpeed()) {
                            speedConfidenceSum += confidence;
                            knotSpeedSumScaledByConfidence += confidence * timewiseClosestFixForWindSource.dampenedTrueWindSpeedInKnots;
                        }
                    }
                }
            }
        }
        final BearingWithConfidence<Position> bearing = bearingCluster.getAverage(p);
        final Vector result;
        if (bearing != null && bearing.getObject() != null) {
            final double mappedBearingRad = coordinateSystem.map(bearing.getObject()).getRadians();
            final double speedInKnots = knotSpeedSumScaledByConfidence / speedConfidenceSum;
            result = new Vector(speedInKnots * Math.sin(mappedBearingRad), speedInKnots * Math.cos(mappedBearingRad));
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public double getMotionScale(double zoomLevel) {
        // This implementation is copied from SimulatorField, hoping it does something useful in combination with
        // the Swarm implementation.
        return 2.0 * knotsInDegreePerFrame * Math.pow(1.8, Math.min(15.0, 20.0 - zoomLevel)); 
    }
    
    @Override
    public double getParticleWeight(LatLng p, Vector v) {
        return v == null ? 0 : (v.length() / MAX_WIND_SPEED_IN_KNOTS);
    }

    /**
     * The implementation uses 3 coordinate space units (usually mapping to pixels if no zoom or other transformation is
     * in place for the Context2d canvas) for {@link #MAX_WIND_SPEED_IN_KNOTS}, decreasing linearly to 0 for zero speeds.
     */
    @Override
    public double getLineWidth(double speed) {
        return 1.2;
    }

    @Override
    public LatLngBounds getFieldCorners() {
        return infiniteBounds;
    }

    @Override
    public double getParticleFactor() {
        return 0.5;
    }
}
