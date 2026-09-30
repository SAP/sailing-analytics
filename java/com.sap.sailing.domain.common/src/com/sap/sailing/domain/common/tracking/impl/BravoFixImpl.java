package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.sensordata.BravoSensorDataMetadata;
import com.sap.sailing.domain.common.sensordata.ColumnMetadata;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MeterDistance;

/**
 * Implementation of {@link BravoFix} that wraps a {@link DoubleVectorFix} which holds the actual sensor data. The
 * mapping metadata is stored in the {@link BravoSensorDataMetadata} enum.
 */
public class BravoFixImpl extends SensorFixImpl implements BravoFix {
    private static final long serialVersionUID = 2033254212013221160L;

    public BravoFixImpl(DoubleVectorFix fix) {
        super(fix);
    }
    
    @Override
    protected ColumnMetadata resolveMetadataFromValueName(String valueName) {
        return BravoSensorDataMetadata.byColumnName(valueName);
    }

    @Override
    public Distance getRideHeight() {
        Double rideHeightPortHullAsDouble = fix.get(BravoSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex());
        Double rideHeightStarboardHullAsDouble = fix
                .get(BravoSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex());
        return rideHeightPortHullAsDouble == null ?
                rideHeightStarboardHullAsDouble == null ? null :
                    new MeterDistance(rideHeightStarboardHullAsDouble) :
                rideHeightStarboardHullAsDouble == null ?
                        new MeterDistance(rideHeightPortHullAsDouble) :
                            new MeterDistance(Math.min(rideHeightPortHullAsDouble, rideHeightStarboardHullAsDouble));
    }

    @Override
    public Distance getRideHeightPortHull() {
        double rideHeightPortHullasDouble = fix.get(BravoSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex());
        return new MeterDistance(rideHeightPortHullasDouble);
    }

    @Override
    public Distance getRideHeightStarboardHull() {
        double rideHeightStarboardHullasDouble = fix
                .get(BravoSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex());
        return new MeterDistance(rideHeightStarboardHullasDouble);
    }

    @Override
    public boolean isFoiling() {
        return isFoiling(MIN_FOILING_HEIGHT_THRESHOLD);
    }
    
    @Override
    public boolean isFoiling(Distance minimumRideHeight) {
        final Distance rideHeight = getRideHeight();
        return rideHeight != null && rideHeight.compareTo(minimumRideHeight) >= 0;
    }

    @Override
    public Bearing getPitch() {
        final Double bearingDeg = fix.get(BravoSensorDataMetadata.PITCH.getColumnIndex());
        return bearingDeg == null ? null : new DegreeBearingImpl(bearingDeg);
    }

    @Override
    public Bearing getHeel() {
        final Double bearingDeg = fix.get(BravoSensorDataMetadata.HEEL.getColumnIndex());
        return bearingDeg == null ? null : new DegreeBearingImpl(bearingDeg);
    }
}
