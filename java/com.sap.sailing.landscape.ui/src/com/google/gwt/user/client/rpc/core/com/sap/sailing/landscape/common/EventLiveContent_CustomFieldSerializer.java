package com.google.gwt.user.client.rpc.core.com.sap.sailing.landscape.common;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.RaceLiveContent;

public final class EventLiveContent_CustomFieldSerializer extends CustomFieldSerializer<EventLiveContent> {
    public static void serialize(final SerializationStreamWriter writer, final EventLiveContent instance)
            throws SerializationException {
        writer.writeString(instance.getEventId());
        writer.writeString(instance.getEventName());
        writer.writeObject(instance.getEventStartMillis());
        writer.writeObject(instance.getEventEndMillis());
        writer.writeInt(instance.getRacesWithLiveContent().size());
        for (final RaceLiveContent race : instance.getRacesWithLiveContent()) {
            writer.writeObject(race);
        }
    }

    public static EventLiveContent instantiate(final SerializationStreamReader reader) throws SerializationException {
        final String eventId = reader.readString();
        final String eventName = reader.readString();
        final Long eventStartMillis = (Long) reader.readObject();
        final Long eventEndMillis = (Long) reader.readObject();
        final int raceCount = reader.readInt();
        final List<RaceLiveContent> races = new ArrayList<>(raceCount);
        for (int i = 0; i < raceCount; i++) {
            races.add((RaceLiveContent) reader.readObject());
        }
        return new EventLiveContent(eventId, eventName, eventStartMillis, eventEndMillis, races);
    }

    public static void deserialize(final SerializationStreamReader reader, final EventLiveContent instance) {
        // Fully initialized by instantiate(...)
    }

    @Override
    public void serializeInstance(final SerializationStreamWriter writer, final EventLiveContent instance)
            throws SerializationException {
        serialize(writer, instance);
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public EventLiveContent instantiateInstance(final SerializationStreamReader reader) throws SerializationException {
        return instantiate(reader);
    }

    @Override
    public void deserializeInstance(final SerializationStreamReader reader, final EventLiveContent instance) {
        deserialize(reader, instance);
    }
}
