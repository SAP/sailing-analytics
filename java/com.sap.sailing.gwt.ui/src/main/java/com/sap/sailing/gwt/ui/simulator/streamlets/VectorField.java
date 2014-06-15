package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.Date;

import com.google.gwt.maps.client.MapWidget;
import com.sap.sailing.domain.common.Bounds;
import com.sap.sailing.domain.common.Position;

/**
 * A field of vectors to display in a streamlet {@link Swarm}. The {@link Vector}s returned by {@link #getVector(Position, Date)}
 * are used to initialize {@link Particle}s. Their "speed" is the {@link Vector#length() length} of the vector.<p>
 * 
 * This field provides color strings for each possible speed which are then used to style the particles as they fly through
 * the swarm.
 * 
 * @author Christopher Ronnewinkel (D036654)
 * @author Axel Uhl (D043530)
 *
 */
public interface VectorField {
    /**
     * Tells whether <code>p</code> is within the bounds of this field.
     */
    boolean inBounds(Position p);

    /**
     * The vector field's value at position <code>p</code>.
     * 
     * @param at
     *            the time at which to query the vector field
     * 
     * @return the speed/direction vector that tells how a particle will fly at this position in the vector field, or
     *         <code>null</code> if there should not be a flying particle, e.g., because the field does not know how a
     *         particle would fly at this position. An implementation that does not cleverly extrapolate outside the
     *         field's {@link #inBounds(Position) bounds} should return <code>null</code> for out-of-bounds positions.
     */
    Vector getVector(Position p, Date at);

    /**
     * @param zoomLevel the zoom level as returned by {@link MapWidget#getZoom()}
     */
    double getMotionScale(int zoomLevel);

    /**
     * A weight between 0.0 and 1.0 (inclusive) that tells the probability at which a particle at position
     * <code>p</code> and with speed vector <code>v</code> will be shown. 1.0 means it will certainly be shown; 0.0
     * means it will certainly not be shown.
     * 
     * @param p
     *            the particle's position
     * @param v
     *            the particle's speed/direction vector which may be <code>null</code>; in this case, an implementation
     *            should return 0.0 as the particle's weight.
     */
    double getParticleWeight(Position p, Vector v);

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
    Bounds getFieldCorners();

    double getParticleFactor();

    /**
     * Computes a color for a particle flying at a certain speed.
     * 
     * @param speed
     *            a speed as obtained by computing a {@link Vector}'s {@link Vector#length() length}.
     * @return an RGB color with optional transparency that is a legal color in CSS. Example:
     *         <code>"rgba(1, 2, 3, 0.4)"</code>
     */
    String getColor(double speed);
}
