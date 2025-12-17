package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.SecondsDurationImpl;

public class SecondsDurationImpl_CustomFieldSerializer extends CustomFieldSerializer<SecondsDurationImpl> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, SecondsDurationImpl instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, SecondsDurationImpl instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.asSeconds());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public SecondsDurationImpl instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static SecondsDurationImpl instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new SecondsDurationImpl(streamReader.readDouble());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, SecondsDurationImpl instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, SecondsDurationImpl instance) {
        // Done by instantiateInstance
    }

}
