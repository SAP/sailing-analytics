package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.DegreePosition;

public class DegreePosition_CustomFieldSerializer extends CustomFieldSerializer<DegreePosition> {
    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public DegreePosition instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
        return instantiate(streamReader);
    }

    public static DegreePosition instantiate(SerializationStreamReader streamReader) throws SerializationException {
        return new DegreePosition(streamReader.readDouble(), streamReader.readDouble());
    }

    public static void deserialize(SerializationStreamReader streamReader, DegreePosition instance)
            throws SerializationException {
        // handled by instantiateInstance
    }
    
    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, DegreePosition instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, DegreePosition instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }

    public static void serialize(SerializationStreamWriter streamWriter, DegreePosition instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getLatDeg());
        streamWriter.writeDouble(instance.getLngDeg());
    }

}
