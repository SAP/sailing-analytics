package com.google.gwt.user.client.rpc.core.com.sap.sse.landscape.aws.common.shared;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.aws.common.shared.SecuredAwsLandscapeType;

public class SecuredAwsLandscapeType_CustomFieldSerializer extends CustomFieldSerializer<SecuredAwsLandscapeType> {
    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, SecuredAwsLandscapeType instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }

    public static void serialize(SerializationStreamWriter streamWriter, SecuredAwsLandscapeType instance)
            throws SerializationException {
        streamWriter.writeString(instance.getName());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public SecuredAwsLandscapeType instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
        return instantiate(streamReader);
    }

    public static SecuredAwsLandscapeType instantiate(SerializationStreamReader streamReader) throws SerializationException {
        final String name = streamReader.readString();
        return Util.filter(SecuredAwsLandscapeType.getAllInstances(), sst->sst.getName().equals(name)).iterator().next();
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, SecuredAwsLandscapeType instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, SecuredAwsLandscapeType instance) {
        // Done by instantiateInstance
    }
}