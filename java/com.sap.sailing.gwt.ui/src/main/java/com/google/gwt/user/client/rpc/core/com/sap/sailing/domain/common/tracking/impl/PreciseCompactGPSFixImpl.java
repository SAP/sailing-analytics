package com.google.gwt.user.client.rpc.core.com.sap.sailing.domain.common.tracking.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.tracking.impl.PreciseCompactGPSFixImpl.PreciseCompactPosition;
import com.sap.sse.common.impl.DegreePosition;

public final class PreciseCompactGPSFixImpl {
    public static final class PreciseCompactPosition_CustomFieldSerializer extends CustomFieldSerializer<PreciseCompactPosition> {
        @Override
        public boolean hasCustomInstantiateInstance() {
            return true;
        }

        @Override
        public PreciseCompactPosition instantiateInstance(SerializationStreamReader streamReader)
                throws SerializationException {
            return instantiate(streamReader);
        }

        public static PreciseCompactPosition instantiate(SerializationStreamReader streamReader) throws SerializationException {
            final double latDeg = streamReader.readDouble();
            final double lngDeg = streamReader.readDouble();
            return (PreciseCompactPosition) new com.sap.sailing.domain.common.tracking.impl.PreciseCompactGPSFixImpl(
                    new DegreePosition(latDeg, lngDeg), /* timePoint */null).getPosition();
        }

        @Override
        public void deserializeInstance(SerializationStreamReader streamReader, PreciseCompactPosition instance)
                throws SerializationException {
            deserialize(streamReader, instance);
        }

        public static void deserialize(SerializationStreamReader streamReader, PreciseCompactPosition instance) {
            // handled by instantiate
        }

        @Override
        public void serializeInstance(SerializationStreamWriter streamWriter, PreciseCompactPosition instance)
                throws SerializationException {
            serialize(streamWriter, instance);
        }

        public static void serialize(SerializationStreamWriter streamWriter, PreciseCompactPosition instance)
                throws SerializationException {
            streamWriter.writeDouble(instance.getLatDeg());
            streamWriter.writeDouble(instance.getLngDeg());
        }
    }
}