package com.google.gwt.user.client.rpc.core.com.sap.sse.security.ui.shared;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sse.common.TimedLock;
import com.sap.sse.security.ui.shared.IpToTimedLockDTO;

public class IpToTimedLockDTO_CustomFieldSerializer  extends CustomFieldSerializer<IpToTimedLockDTO> {
    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, IpToTimedLockDTO instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, IpToTimedLockDTO instance)
            throws SerializationException {
        streamWriter.writeString(instance.getIp());
        streamWriter.writeObject(instance.getTimedLock());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public IpToTimedLockDTO instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static IpToTimedLockDTO instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        return new IpToTimedLockDTO(streamReader.readString(), (TimedLock) streamReader.readObject());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, IpToTimedLockDTO instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, IpToTimedLockDTO instance) {
        // Done by instantiateInstance
    }
}
