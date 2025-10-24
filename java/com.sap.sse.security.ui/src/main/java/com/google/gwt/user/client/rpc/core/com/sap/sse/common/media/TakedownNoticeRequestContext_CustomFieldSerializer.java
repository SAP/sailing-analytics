package com.google.gwt.user.client.rpc.core.com.sap.sse.common.media;

import java.util.Arrays;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.Util;
import com.sap.sse.common.media.NatureOfClaim;
import com.sap.sse.common.media.TakedownNoticeRequestContext;

public class TakedownNoticeRequestContext_CustomFieldSerializer extends CustomFieldSerializer<TakedownNoticeRequestContext> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, TakedownNoticeRequestContext instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, TakedownNoticeRequestContext instance)
            throws SerializationException {
        streamWriter.writeString(instance.getContextDescriptionMessageKey());
        streamWriter.writeString(instance.getContextDescriptionMessageParameter());
        streamWriter.writeString(instance.getContentUrl());
        streamWriter.writeString(instance.getPageUrl());
        streamWriter.writeString(instance.getNatureOfClaim().name());
        streamWriter.writeString(instance.getReportingUserComment());
        streamWriter.writeObject(Util.toArray(instance.getSupportingURLs(), new String[0]));
        streamWriter.writeString(instance.getUsername());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public TakedownNoticeRequestContext instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static TakedownNoticeRequestContext instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new TakedownNoticeRequestContext(streamReader.readString(), streamReader.readString(),
                streamReader.readString(), streamReader.readString(), NatureOfClaim.valueOf(streamReader.readString()),
                streamReader.readString(), Arrays.asList((String[]) streamReader.readObject()), streamReader.readString());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, TakedownNoticeRequestContext instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, TakedownNoticeRequestContext instance) {
        // Done by instantiateInstance
    }

}
