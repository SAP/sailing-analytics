package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.KnotSpeedImpl;

public class KnotSpeedImpl_CustomFieldSerializer extends CustomFieldSerializer<KnotSpeedImpl> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, KnotSpeedImpl instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, KnotSpeedImpl instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getKnots());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public KnotSpeedImpl instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static KnotSpeedImpl instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new KnotSpeedImpl(streamReader.readDouble());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, KnotSpeedImpl instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, KnotSpeedImpl instance) {
        // Done by instantiateInstance
    }

}
