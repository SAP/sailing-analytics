package com.google.gwt.user.client.rpc.core.com.sap.sailing.landscape.common;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.landscape.common.LiveContentAwareOperationResult;
import com.sap.sailing.landscape.common.LiveContentCheckResult;

public final class LiveContentAwareOperationResult_CustomFieldSerializer
        extends CustomFieldSerializer<LiveContentAwareOperationResult<?>> {
    public static void serialize(final SerializationStreamWriter writer,
            final LiveContentAwareOperationResult<?> instance) throws SerializationException {
        writer.writeBoolean(instance.isSuccessful());
        writer.writeObject(instance.getSuccessfulResult());
        writer.writeObject(instance.getLiveContentCheckResult());
    }

    public static LiveContentAwareOperationResult<?> instantiate(final SerializationStreamReader reader)
            throws SerializationException {
        final boolean successful = reader.readBoolean();
        final Object successfulResult = reader.readObject();
        final LiveContentCheckResult liveContentCheckResult = (LiveContentCheckResult) reader.readObject();
        final LiveContentAwareOperationResult<?> result;
        if (successful) {
            result = LiveContentAwareOperationResult.success(successfulResult);
        } else {
            result = LiveContentAwareOperationResult.liveContentConflict(liveContentCheckResult);
        }
        return result;
    }

    public static void deserialize(final SerializationStreamReader reader,
            final LiveContentAwareOperationResult<?> instance) {
        // Fully initialized by instantiate(...)
    }

    @Override
    public void serializeInstance(final SerializationStreamWriter writer,
            final LiveContentAwareOperationResult<?> instance) throws SerializationException {
        serialize(writer, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public LiveContentAwareOperationResult<?> instantiateInstance(final SerializationStreamReader reader)
            throws SerializationException {
        return instantiate(reader);
    }

    @Override
    public void deserializeInstance(final SerializationStreamReader reader,
            final LiveContentAwareOperationResult<?> instance) {
        deserialize(reader, instance);
    }
}
