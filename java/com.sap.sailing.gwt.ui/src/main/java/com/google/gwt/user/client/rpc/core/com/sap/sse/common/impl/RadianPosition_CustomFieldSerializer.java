package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.RadianPosition;

public final class RadianPosition_CustomFieldSerializer extends CustomFieldSerializer<RadianPosition> {
    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public RadianPosition instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
        return instantiate(streamReader);
    }

    public static RadianPosition instantiate(SerializationStreamReader streamReader) throws SerializationException {
        final double latRad = streamReader.readDouble();
        final double lngRad = streamReader.readDouble();
        return new RadianPosition(latRad, lngRad);
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, RadianPosition instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, RadianPosition instance) {
        // handled by instantiate
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, RadianPosition instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }

    public static void serialize(SerializationStreamWriter streamWriter, RadianPosition instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getLatRad());
        streamWriter.writeDouble(instance.getLngRad());
    }
}
