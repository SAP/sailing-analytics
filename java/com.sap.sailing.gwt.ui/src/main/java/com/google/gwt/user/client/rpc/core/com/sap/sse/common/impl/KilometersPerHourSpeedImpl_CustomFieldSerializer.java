package com.google.gwt.user.client.rpc.core.com.sap.sse.common.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.impl.KilometersPerHourSpeedImpl;

public class KilometersPerHourSpeedImpl_CustomFieldSerializer extends CustomFieldSerializer<KilometersPerHourSpeedImpl> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, KilometersPerHourSpeedImpl instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, KilometersPerHourSpeedImpl instance)
            throws SerializationException {
        streamWriter.writeDouble(instance.getKilometersPerHour());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public KilometersPerHourSpeedImpl instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static KilometersPerHourSpeedImpl instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new KilometersPerHourSpeedImpl(streamReader.readDouble());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, KilometersPerHourSpeedImpl instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, KilometersPerHourSpeedImpl instance) {
        // Done by instantiateInstance
    }

}
