package com.google.gwt.user.client.rpc.core.com.sap.sailing.domain.common.tracking.impl;

import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactWindImpl.VeryCompactPosition;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;

public final class CompactWindImpl {
    private static final Logger logger = Logger.getLogger(CompactWindImpl.class.getName());
    
    public static final class CompactPosition_CustomFieldSerializer extends CustomFieldSerializer<VeryCompactPosition> {
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
                return (VeryCompactPosition) new com.sap.sailing.domain.common.tracking.impl.VeryCompactWindImpl(new WindImpl(
                        new DegreePosition(latDeg, lngDeg), /* timePoint */null, new KnotSpeedWithBearingImpl(0,
                                new DegreeBearingImpl(0)))).getPosition();
            } catch (CompactionNotPossibleException e) {
                logger.severe("Internal error: Cannot de-serialize compact position: "+e.getMessage()+"; throwing runtime exception");
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
}