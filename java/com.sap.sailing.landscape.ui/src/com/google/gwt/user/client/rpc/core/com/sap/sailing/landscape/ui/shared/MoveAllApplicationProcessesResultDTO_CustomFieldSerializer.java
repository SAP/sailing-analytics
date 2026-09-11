package com.google.gwt.user.client.rpc.core.com.sap.sailing.landscape.ui.shared;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.landscape.ui.shared.MoveAllApplicationProcessesResultDTO;

public final class MoveAllApplicationProcessesResultDTO_CustomFieldSerializer
        extends CustomFieldSerializer<MoveAllApplicationProcessesResultDTO> {
    public static void serialize(final SerializationStreamWriter writer,
            final MoveAllApplicationProcessesResultDTO instance) throws SerializationException {
        writer.writeString(instance.getNewHostId());
    }

    public static MoveAllApplicationProcessesResultDTO instantiate(final SerializationStreamReader reader)
            throws SerializationException {
        return new MoveAllApplicationProcessesResultDTO(reader.readString());
    }

    public static void deserialize(final SerializationStreamReader reader,
            final MoveAllApplicationProcessesResultDTO instance) {
        // Fully initialized by instantiate(...)
    }

    @Override
    public void serializeInstance(final SerializationStreamWriter writer,
            final MoveAllApplicationProcessesResultDTO instance) throws SerializationException {
        serialize(writer, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public MoveAllApplicationProcessesResultDTO instantiateInstance(final SerializationStreamReader reader)
            throws SerializationException {
        return instantiate(reader);
    }

    @Override
    public void deserializeInstance(final SerializationStreamReader reader,
            final MoveAllApplicationProcessesResultDTO instance) {
        deserialize(reader, instance);
    }
}
