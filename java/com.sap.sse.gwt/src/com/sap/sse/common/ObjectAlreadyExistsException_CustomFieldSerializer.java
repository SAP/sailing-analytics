package com.sap.sse.common;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.ObjectAlreadyExistsException.ObjectType;

public class ObjectAlreadyExistsException_CustomFieldSerializer extends CustomFieldSerializer<ObjectAlreadyExistsException> {
    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public ObjectAlreadyExistsException instantiateInstance(final SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static ObjectAlreadyExistsException instantiate(final SerializationStreamReader streamReader)
            throws SerializationException {
        return new ObjectAlreadyExistsException((ObjectType) streamReader.readObject(), streamReader.readString());
    }

    public static void deserialize(final SerializationStreamReader streamReader, final ObjectAlreadyExistsException instance)
            throws SerializationException {
        // All fields are read by instantiate.
    }

    @Override
    public void deserializeInstance(final SerializationStreamReader streamReader, final ObjectAlreadyExistsException instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    @Override
    public void serializeInstance(final SerializationStreamWriter streamWriter, final ObjectAlreadyExistsException instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }

    public static void serialize(final SerializationStreamWriter streamWriter, final ObjectAlreadyExistsException instance)
            throws SerializationException {
        streamWriter.writeObject(instance.getObjectType());
        streamWriter.writeString(instance.getIdentifier());
    }
}
