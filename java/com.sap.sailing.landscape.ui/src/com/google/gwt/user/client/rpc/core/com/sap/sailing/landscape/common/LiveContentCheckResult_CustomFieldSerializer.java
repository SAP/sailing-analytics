package com.google.gwt.user.client.rpc.core.com.sap.sailing.landscape.common;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.landscape.common.LiveContentCheckResult;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;

public final class LiveContentCheckResult_CustomFieldSerializer extends CustomFieldSerializer<LiveContentCheckResult> {
    public static void serialize(final SerializationStreamWriter writer, final LiveContentCheckResult instance)
            throws SerializationException {
        writer.writeLong(instance.getCheckedAtMillis());
        writer.writeInt(instance.getReplicaSetsWithLiveContent().size());
        for (final ReplicaSetLiveContent replicaSet : instance.getReplicaSetsWithLiveContent()) {
            writer.writeObject(replicaSet);
        }
    }

    public static LiveContentCheckResult instantiate(final SerializationStreamReader reader)
            throws SerializationException {
        final long checkedAtMillis = reader.readLong();
        final int replicaSetCount = reader.readInt();
        final List<ReplicaSetLiveContent> replicaSets = new ArrayList<>(replicaSetCount);
        for (int i = 0; i < replicaSetCount; i++) {
            replicaSets.add((ReplicaSetLiveContent) reader.readObject());
        }
        return new LiveContentCheckResult(checkedAtMillis, replicaSets);
    }

    public static void deserialize(final SerializationStreamReader reader, final LiveContentCheckResult instance) {
        // Fully initialized by instantiate(...)
    }

    @Override
    public void serializeInstance(final SerializationStreamWriter writer, final LiveContentCheckResult instance)
            throws SerializationException {
        serialize(writer, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public LiveContentCheckResult instantiateInstance(final SerializationStreamReader reader)
            throws SerializationException {
        return instantiate(reader);
    }

    @Override
    public void deserializeInstance(final SerializationStreamReader reader, final LiveContentCheckResult instance) {
        deserialize(reader, instance);
    }
}
