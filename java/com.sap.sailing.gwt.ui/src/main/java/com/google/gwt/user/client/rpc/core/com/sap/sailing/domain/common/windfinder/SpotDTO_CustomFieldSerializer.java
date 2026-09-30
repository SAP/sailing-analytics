package com.google.gwt.user.client.rpc.core.com.sap.sailing.domain.common.windfinder;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.windfinder.SpotDTO;
import com.sap.sse.common.Position;

public class SpotDTO_CustomFieldSerializer extends CustomFieldSerializer<SpotDTO> {
    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public SpotDTO instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
        return instantiate(streamReader);
    }

    public static SpotDTO instantiate(SerializationStreamReader streamReader) throws SerializationException {
        final String name = streamReader.readString();
        final String id = streamReader.readString();
        final String keyword = streamReader.readString();
        final String englishCountryName = streamReader.readString();
        final Position position = (Position) streamReader.readObject();
        return new SpotDTO(name, id, keyword, englishCountryName, position);
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, SpotDTO instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, SpotDTO instance) {
        // handled by instantiate
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, SpotDTO instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }

    public static void serialize(SerializationStreamWriter streamWriter, SpotDTO instance)
            throws SerializationException {
        streamWriter.writeString(instance.getName());
        streamWriter.writeString(instance.getId());
        streamWriter.writeString(instance.getKeyword());
        streamWriter.writeString(instance.getEnglishCountryName());
        streamWriter.writeObject(instance.getPosition());
    }
}
