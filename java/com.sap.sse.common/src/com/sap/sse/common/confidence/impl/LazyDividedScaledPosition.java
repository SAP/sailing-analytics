package com.sap.sse.common.confidence.impl;

import com.sap.sse.common.AbstractPosition;
import com.sap.sse.common.Position;
import com.sap.sse.common.scalablevalue.impl.ScalablePosition;

/**
 * A wind object's position is currently not very frequently used. However, during averaging, computing the average
 * position of a number of wind fixes costs quite a few CPU cycles. By making this calculation lazy / on-demand we may
 * save some of them.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class LazyDividedScaledPosition extends AbstractPosition {
    private static final long serialVersionUID = -4755705843467806809L;
    private final ScalablePosition scalablePosition;
    private final double divisor;
    private Position position;
    
    public LazyDividedScaledPosition(ScalablePosition scalablePosition, double divisor) {
        super();
        this.scalablePosition = scalablePosition;
        this.divisor = divisor;
        this.position = null;
    }

    @Override
    public synchronized double getLatRad() {
        if (position == null) {
            resolve();
        }
        return position.getLatRad();
    }

    @Override
    public synchronized double getLngRad() {
        if (position == null) {
            resolve();
        }
        return position.getLngRad();
    }
    
    private synchronized void resolve() {
        position = scalablePosition.divide(divisor);
    }
}