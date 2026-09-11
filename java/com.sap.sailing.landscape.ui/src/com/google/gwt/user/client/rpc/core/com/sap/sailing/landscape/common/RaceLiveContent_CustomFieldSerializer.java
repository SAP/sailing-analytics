package com.google.gwt.user.client.rpc.core.com.sap.sailing.landscape.common;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.landscape.common.RaceLiveContent;

public final class RaceLiveContent_CustomFieldSerializer extends CustomFieldSerializer<RaceLiveContent> {
    public static void serialize(final SerializationStreamWriter writer, final RaceLiveContent instance)
            throws SerializationException {
        writer.writeString(instance.getRegattaName());
        writer.writeString(instance.getRaceName());
        writer.writeLong(instance.getTrackingStartMillis());
        writer.writeObject(instance.getTrackingEndMillis());
    }

    public static RaceLiveContent instantiate(final SerializationStreamReader reader) throws SerializationException {
        return new RaceLiveContent(reader.readString(), reader.readString(), reader.readLong(),
                (Long) reader.readObject());
    }

    public static void deserialize(final SerializationStreamReader reader, final RaceLiveContent instance) {
        // Fully initialized by instantiate(...)
    }

    @Override
    public void serializeInstance(final SerializationStreamWriter writer, final RaceLiveContent instance)
            throws SerializationException {
        serialize(writer, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public RaceLiveContent instantiateInstance(final SerializationStreamReader reader) throws SerializationException {
        return instantiate(reader);
    }

    @Override
    public void deserializeInstance(final SerializationStreamReader reader, final RaceLiveContent instance) {
        deserialize(reader, instance);
    }
}
