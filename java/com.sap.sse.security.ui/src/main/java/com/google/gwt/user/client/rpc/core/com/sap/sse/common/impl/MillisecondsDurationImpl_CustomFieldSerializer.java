package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.MillisecondsDurationImpl;

public class MillisecondsDurationImpl_CustomFieldSerializer extends CustomFieldSerializer<MillisecondsDurationImpl> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, MillisecondsDurationImpl instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, MillisecondsDurationImpl instance)
            throws SerializationException {
        streamWriter.writeLong(instance.asMillis());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public MillisecondsDurationImpl instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static MillisecondsDurationImpl instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new MillisecondsDurationImpl(streamReader.readLong());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, MillisecondsDurationImpl instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, MillisecondsDurationImpl instance) {
        // Done by instantiateInstance
    }

}
