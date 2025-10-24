package com.google.gwt.user.client.rpc.core.com.sap.sse.landscape.common.shared;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.Util;
import com.sap.sse.landscape.common.shared.SecuredLandscapeTypes;

public class SecuredLandscapeTypes_CustomFieldSerializer extends CustomFieldSerializer<SecuredLandscapeTypes> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, SecuredLandscapeTypes instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }

    public static void serialize(SerializationStreamWriter streamWriter, SecuredLandscapeTypes instance)
            throws SerializationException {
        streamWriter.writeString(instance.getName());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public SecuredLandscapeTypes instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
        return instantiate(streamReader);
    }

    public static SecuredLandscapeTypes instantiate(SerializationStreamReader streamReader) throws SerializationException {
        final String name = streamReader.readString();
        return Util.filter(SecuredLandscapeTypes.getAllInstances(), sst->sst.getName().equals(name)).iterator().next();
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, SecuredLandscapeTypes instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, SecuredLandscapeTypes instance) {
        // Done by instantiateInstance
    }

}
