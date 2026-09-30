package com.google.gwt.user.client.rpc.core.com.sap.sailing.domain.common.tracking.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl.VeryCompactEstimatedSpeed;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl.VeryCompactPosition;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl.VeryCompactSpeedWithBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public final class VeryCompactGPSFixMovingImpl {
    private static final Logger logger = Logger.getLogger(VeryCompactGPSFixMovingImpl.class.getName());
    
    public static final class VeryCompactPosition_CustomFieldSerializer extends CustomFieldSerializer<VeryCompactPosition> {
        @Override
        public boolean hasCustomInstantiateInstance() {
            return true;
        }

        @Override
        public VeryCompactPosition instantiateInstance(SerializationStreamReader streamReader)
                throws SerializationException {
            return instantiate(streamReader);
        }

        public static VeryCompactPosition instantiate(SerializationStreamReader streamReader) throws SerializationException {
            final double latDeg = streamReader.readDouble();
            final double lngDeg = streamReader.readDouble();
            try {
                return (VeryCompactPosition) new com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl(
                        new DegreePosition(latDeg, lngDeg), /* timePoint */null, /* speed with bearing */ null, /* optionalTrueHeading */ null).getPosition();
            } catch (CompactionNotPossibleException e) {
                logger.log(Level.SEVERE, "Internal error: an object that was a very compact position and was serialized "+
                        "couldn't be de-serialized again as such an object", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void deserializeInstance(SerializationStreamReader streamReader, VeryCompactPosition instance)
                throws SerializationException {
            deserialize(streamReader, instance);
        }

        public static void deserialize(SerializationStreamReader streamReader, VeryCompactPosition instance) {
            // handled by instantiate
        }

        @Override
        public void serializeInstance(SerializationStreamWriter streamWriter, VeryCompactPosition instance)
                throws SerializationException {
            serialize(streamWriter, instance);
        }

        public static void serialize(SerializationStreamWriter streamWriter, VeryCompactPosition instance)
                throws SerializationException {
            streamWriter.writeDouble(instance.getLatDeg());
            streamWriter.writeDouble(instance.getLngDeg());
        }
    }

    public static final class VeryCompactSpeedWithBearing_CustomFieldSerializer extends CustomFieldSerializer<VeryCompactSpeedWithBearing> {
        @Override
        public boolean hasCustomInstantiateInstance() {
            return true;
        }

        @Override
        public VeryCompactSpeedWithBearing instantiateInstance(SerializationStreamReader streamReader)
                throws SerializationException {
            return instantiate(streamReader);
        }

        public static VeryCompactSpeedWithBearing instantiate(SerializationStreamReader streamReader) throws SerializationException {
            final double knotSpeed = streamReader.readDouble();
            final double bearingDeg = streamReader.readDouble();
            try {
                return (VeryCompactSpeedWithBearing) new com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl(
                        /* position */ null, /* timePoint */null, new KnotSpeedWithBearingImpl(knotSpeed, new DegreeBearingImpl(bearingDeg)), /* optionalTrueHeading */ null).getSpeed();
            } catch (CompactionNotPossibleException e) {
                logger.log(Level.SEVERE, "Internal error: an object that was a very compact position and was serialized "+
                        "couldn't be de-serialized again as such an object", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void deserializeInstance(SerializationStreamReader streamReader, VeryCompactSpeedWithBearing instance)
                throws SerializationException {
            deserialize(streamReader, instance);
        }

        public static void deserialize(SerializationStreamReader streamReader, VeryCompactSpeedWithBearing instance) {
            // handled by instantiate
        }

        @Override
        public void serializeInstance(SerializationStreamWriter streamWriter, VeryCompactSpeedWithBearing instance)
                throws SerializationException {
            serialize(streamWriter, instance);
        }

        public static void serialize(SerializationStreamWriter streamWriter, VeryCompactSpeedWithBearing instance)
                throws SerializationException {
            streamWriter.writeDouble(instance.getKnots());
            streamWriter.writeDouble(instance.getBearing().getDegrees());
        }
    }

    public static final class VeryCompactEstimatedSpeed_CustomFieldSerializer extends CustomFieldSerializer<VeryCompactEstimatedSpeed> {
        @Override
        public boolean hasCustomInstantiateInstance() {
            return true;
        }

        @Override
        public VeryCompactEstimatedSpeed instantiateInstance(SerializationStreamReader streamReader)
                throws SerializationException {
            return instantiate(streamReader);
        }

        public static VeryCompactEstimatedSpeed instantiate(SerializationStreamReader streamReader) throws SerializationException {
            final double knotSpeed = streamReader.readDouble();
            final double bearingDeg = streamReader.readDouble();
            try {
                final KnotSpeedWithBearingImpl speed = new KnotSpeedWithBearingImpl(knotSpeed, new DegreeBearingImpl(bearingDeg));
                com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl fix = new com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl(
                        /* dummy position */ new DegreePosition(0, 0), /* timePoint */null, speed, /* optionalTrueHeading */ null);
                fix.cacheEstimatedSpeed(speed);
                return (VeryCompactEstimatedSpeed) fix.getCachedEstimatedSpeed();
            } catch (CompactionNotPossibleException e) {
                logger.log(Level.SEVERE, "Internal error: an object that was a very compact position and was serialized "+
                        "couldn't be de-serialized again as such an object", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void deserializeInstance(SerializationStreamReader streamReader, VeryCompactEstimatedSpeed instance)
                throws SerializationException {
            deserialize(streamReader, instance);
        }

        public static void deserialize(SerializationStreamReader streamReader, VeryCompactEstimatedSpeed instance) {
            // handled by instantiate
        }

        @Override
        public void serializeInstance(SerializationStreamWriter streamWriter, VeryCompactEstimatedSpeed instance)
                throws SerializationException {
            serialize(streamWriter, instance);
        }

        public static void serialize(SerializationStreamWriter streamWriter, VeryCompactEstimatedSpeed instance)
                throws SerializationException {
            streamWriter.writeDouble(instance.getKnots());
            streamWriter.writeDouble(instance.getBearing().getDegrees());
        }
    }
}