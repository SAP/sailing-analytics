package com.google.gwt.user.client.rpc.core.com.sap.sailing.landscape.common;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;

public final class ReplicaSetLiveContent_CustomFieldSerializer extends CustomFieldSerializer<ReplicaSetLiveContent> {
    public static void serialize(final SerializationStreamWriter writer, final ReplicaSetLiveContent instance)
            throws SerializationException {
        writer.writeString(instance.getReplicaSetName());
        writer.writeInt(instance.getEventsWithLiveContent().size());
        for (final EventLiveContent event : instance.getEventsWithLiveContent()) {
            writer.writeObject(event);
        }
    }

    public static ReplicaSetLiveContent instantiate(final SerializationStreamReader reader)
            throws SerializationException {
        final String replicaSetName = reader.readString();
        final int eventCount = reader.readInt();
        final List<EventLiveContent> events = new ArrayList<>(eventCount);
        for (int i = 0; i < eventCount; i++) {
            events.add((EventLiveContent) reader.readObject());
        }
        return new ReplicaSetLiveContent(replicaSetName, events);
    }

    public static void deserialize(final SerializationStreamReader reader, final ReplicaSetLiveContent instance) {
        // Fully initialized by instantiate(...)
    }

    @Override
    public void serializeInstance(final SerializationStreamWriter writer, final ReplicaSetLiveContent instance)
            throws SerializationException {
        serialize(writer, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public ReplicaSetLiveContent instantiateInstance(final SerializationStreamReader reader)
            throws SerializationException {
        return instantiate(reader);
    }

    @Override
    public void deserializeInstance(final SerializationStreamReader reader, final ReplicaSetLiveContent instance) {
        deserialize(reader, instance);
    }
}
