package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.Date;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.LatLngBounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;

/**
 * A field of vectors to display in a streamlet {@link Swarm}. The {@link Vector}s returned by {@link #getVector(Position, Date)}
 * are used to initialize {@link Particle}s. Their "speed" is the {@link Vector#length() length} of the vector.<p>
 * 
 * @author Christopher Ronnewinkel (D036654)
 * @author Axel Uhl (D043530)
 *
 */
public interface VectorField {
    /**
     * Tells whether the real-world position <code>p</code> is within the bounds of this field.
     */
    boolean inBounds(Position p);
    
    /**
     * Tells whether the map position <code>p</code> which may have undergone rotation and translation
     * is within the bounds of this field.
     */
    boolean inBounds(LatLng p);

    /**
     * The vector field's value at position <code>p</code>.
     * 
     * @param p
     *            position in the map's coordinate system; needs to be mapped backwards through a
     *            {@link CoordinateSystem} to produce a real-world {@link Position}
     * @param at
     *            the time at which to query the vector field. This vector field and its client(s) may agree on how to
     *            handle the time ranges. For example, a vector field implementation may buffer data for a larger time
     *            range, and clients can then query this vector field for different time points successfully. Or
     *            this vector field and its client(s) agree what a valid time range or even single time point may be,
     *            and this vector field then buffers only the data for a very short time range or even a single time
     *            point and then basically ignores this argument, knowing that its client(s) already make an assumption
     *            about the time for which this vector field will hold and deliver data.
     * 
     * @return the speed/direction vector that tells how a particle will fly at this position in the vector field, or
     *         <code>null</code> if there should not be a flying particle, e.g., because the field does not know how a
     *         particle would fly at this position. An implementation that does not cleverly extrapolate outside the
     *         field's {@link #inBounds(Position) bounds} should return <code>null</code> for out-of-bounds positions.
     */
    Vector getVector(LatLng p, Date at);

    /**
     * @param zoomLevel the zoom level as returned by {@link MapWidget#getZoom()}
     */
    double getMotionScale(double zoomLevel);

    /**
     * A weight between 0.0 and 1.0 (inclusive) that tells the probability at which a particle at position
     * <code>p</code> and with speed vector <code>v</code> will be shown. 1.0 means it will certainly be shown; 0.0
     * means it will certainly not be shown.
     * 
     * @param p
     *            the particle's position in the map's coordinate system; needs to be mapped backwards through a
     *            {@link CoordinateSystem} to produce a real-world {@link Position}
     * @param v
     *            the particle's speed/direction vector which may be <code>null</code>; in this case, an implementation
     *            should return 0.0 as the particle's weight.
     */
    double getParticleWeight(LatLng p, Vector v);

    /**
     * Computes a line width for a particle flying at a certain speed.
     * 
     * @param speed
     *            a speed as obtained by computing a {@link Vector}'s {@link Vector#length() length}.
     */
    double getLineWidth(double speed);

    /**
     * @return the north-east and the south-west corner of the rectangular area on a Mercator projection that fully
     *         contains this field's {@link #inBounds(Position) bounds}. Note that the field's
     *         {@link #inBounds(Position) bounds} are not necessarily defined as a rectangle on a Mercator map. There
     *         may be a rotation or some other shape in place.
     */
    LatLngBounds getFieldCorners();

    double getParticleFactor();
}
