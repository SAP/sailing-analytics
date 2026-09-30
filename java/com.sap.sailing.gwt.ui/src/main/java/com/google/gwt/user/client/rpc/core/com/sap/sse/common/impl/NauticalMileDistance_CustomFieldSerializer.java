package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.NauticalMileDistance;

public class NauticalMileDistance_CustomFieldSerializer extends CustomFieldSerializer<NauticalMileDistance> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, NauticalMileDistance instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, NauticalMileDistance instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getNauticalMiles());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public NauticalMileDistance instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static NauticalMileDistance instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new NauticalMileDistance(streamReader.readDouble());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, NauticalMileDistance instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, NauticalMileDistance instance) {
        // Done by instantiateInstance
    }

}
