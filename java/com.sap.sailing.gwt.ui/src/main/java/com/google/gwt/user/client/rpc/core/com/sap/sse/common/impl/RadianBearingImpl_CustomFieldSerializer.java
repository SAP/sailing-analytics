package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.RadianBearingImpl;

public class RadianBearingImpl_CustomFieldSerializer extends CustomFieldSerializer<RadianBearingImpl> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, RadianBearingImpl instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, RadianBearingImpl instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getRadians());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public RadianBearingImpl instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static RadianBearingImpl instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new RadianBearingImpl(streamReader.readDouble());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, RadianBearingImpl instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, RadianBearingImpl instance) {
        // Done by instantiateInstance
    }

}
