package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.CentralAngleDistance;

public class CentralAngleDistance_CustomFieldSerializer extends CustomFieldSerializer<CentralAngleDistance> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, CentralAngleDistance instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, CentralAngleDistance instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getCentralAngleRad());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public CentralAngleDistance instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static CentralAngleDistance instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new CentralAngleDistance(streamReader.readDouble());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, CentralAngleDistance instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, CentralAngleDistance instance) {
        // Done by instantiateInstance
    }

}
