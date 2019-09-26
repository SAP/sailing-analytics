package com.google.gwt.user.client.rpc.core.com.sap.sailing.domain.common.orc.impl;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class ORCPerformanceCurveLegImpl_CustomFieldSerializer extends CustomFieldSerializer<ORCPerformanceCurveLegImpl> {

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, ORCPerformanceCurveLegImpl instance)
            throws SerializationException {
        serialize(streamWriter, instance);
    }
    
    public static void serialize(SerializationStreamWriter streamWriter, ORCPerformanceCurveLegImpl instance)
            throws SerializationException {
        streamWriter.writeString(instance.getType().name());
        streamWriter.writeObject((Double) instance.getTwa().getDegrees());
        streamWriter.writeObject((Double) instance.getLength().getNauticalMiles());
    }

    @Override
    public boolean hasCustomInstantiateInstance() {
        return true;
    }
    
    @Override
    public ORCPerformanceCurveLegImpl instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        return instantiate(streamReader);
    }

    public static ORCPerformanceCurveLegImpl instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        final ORCPerformanceCurveLegTypes type = ORCPerformanceCurveLegTypes.valueOf(streamReader.readString());
        final Double twaInDegrees = (Double) streamReader.readObject();
        final Double distanceInNauticalMiles = (Double) streamReader.readObject();
        return type == ORCPerformanceCurveLegTypes.TWA ? new ORCPerformanceCurveLegImpl(distanceInNauticalMiles==null?null:new NauticalMileDistance(distanceInNauticalMiles),
                twaInDegrees==null?null:new DegreeBearingImpl(twaInDegrees)) :
                    new ORCPerformanceCurveLegImpl(distanceInNauticalMiles==null?null:new NauticalMileDistance(distanceInNauticalMiles), type);
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, ORCPerformanceCurveLegImpl instance)
            throws SerializationException {
        deserialize(streamReader, instance);
    }

    public static void deserialize(SerializationStreamReader streamReader, ORCPerformanceCurveLegImpl instance) {
        // Done by instantiateInstance
    }

}
