package com.google.gwt.user.client.rpc.core.com.sap.sailing.domain.common.security;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.common.Util;

public class SecuredDomainType_CustomFieldSerializer extends CustomFieldSerializer<SecuredDomainType> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, SecuredDomainType instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }

    public static void serialize(SerializationStreamWriter streamWriter, SecuredDomainType instance)
            throws SerializationException {
        streamWriter.writeString(instance.getName());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }

    @Override
    public SecuredDomainType instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
        return instantiate(streamReader);
    }

    public static SecuredDomainType instantiate(SerializationStreamReader streamReader) throws SerializationException {
        final String name = streamReader.readString();
        return Util.filter(SecuredDomainType.getAllInstances(), sst->sst.getName().equals(name)).iterator().next();
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, SecuredDomainType instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, SecuredDomainType instance) {
        // Done by instantiateInstance
    }

}
