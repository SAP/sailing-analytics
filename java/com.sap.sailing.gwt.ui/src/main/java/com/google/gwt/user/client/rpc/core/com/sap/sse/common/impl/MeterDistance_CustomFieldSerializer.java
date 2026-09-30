package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.MeterDistance;

public class MeterDistance_CustomFieldSerializer extends CustomFieldSerializer<MeterDistance> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, MeterDistance instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, MeterDistance instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getMeters());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public MeterDistance instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static MeterDistance instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new MeterDistance(streamReader.readDouble());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, MeterDistance instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, MeterDistance instance) {
        // Done by instantiateInstance
    }

}
