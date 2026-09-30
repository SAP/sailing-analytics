package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class KnotSpeedWithBearingImpl_CustomFieldSerializer extends CustomFieldSerializer<KnotSpeedWithBearingImpl> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, KnotSpeedWithBearingImpl instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, KnotSpeedWithBearingImpl instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getKnots());
        streamWriter.writeDouble(instance.getBearing().getDegrees());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public KnotSpeedWithBearingImpl instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static KnotSpeedWithBearingImpl instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new KnotSpeedWithBearingImpl(streamReader.readDouble(), new DegreeBearingImpl(streamReader.readDouble()));
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, KnotSpeedWithBearingImpl instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, KnotSpeedWithBearingImpl instance) {
        // Done by instantiateInstance
    }

}
