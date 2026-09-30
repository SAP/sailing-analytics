package com.google.gwt.user.client.rpc.core.com.sap.sailing.domain.common.tracking.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixImpl.CompactPosition;
import com.sap.sse.common.impl.DegreePosition;

public final class VeryCompactGPSFixImpl {
    public static final class CompactPosition_CustomFieldSerializer extends CustomFieldSerializer<CompactPosition> {
        @Override
        public boolean hasCustomInstantiateInstance() {
            return true;
        }

        @Override
        public CompactPosition instantiateInstance(SerializationStreamReader streamReader)
                throws SerializationException {
            return instantiate(streamReader);
        }

        public static CompactPosition instantiate(SerializationStreamReader streamReader) throws SerializationException {
            final double latDeg = streamReader.readDouble();
            final double lngDeg = streamReader.readDouble();
            return (CompactPosition) new com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixImpl(
                    new DegreePosition(latDeg, lngDeg), /* timePoint */null).getPosition();
        }

        @Override
        public void deserializeInstance(SerializationStreamReader streamReader, CompactPosition instance)
                throws SerializationException {
            deserialize(streamReader, instance);
        }

        public static void deserialize(SerializationStreamReader streamReader, CompactPosition instance) {
            // handled by instantiate
        }

        @Override
        public void serializeInstance(SerializationStreamWriter streamWriter, CompactPosition instance)
                throws SerializationException {
            serialize(streamWriter, instance);
        }

        public static void serialize(SerializationStreamWriter streamWriter, CompactPosition instance)
                throws SerializationException {
            streamWriter.writeDouble(instance.getLatDeg());
            streamWriter.writeDouble(instance.getLngDeg());
        }
    }
}