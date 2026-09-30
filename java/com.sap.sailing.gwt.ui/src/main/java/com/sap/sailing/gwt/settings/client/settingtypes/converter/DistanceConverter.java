package com.sap.sailing.gwt.settings.client.settingtypes.converter;

import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.settings.generic.ValueConverter;
import com.sap.sse.common.settings.value.DoubleValue;
import com.sap.sse.common.settings.value.Value;

public class DistanceConverter implements ValueConverter<Distance> {

    public static final DistanceConverter INSTANCE = new DistanceConverter();

    private DistanceConverter() {
    }

    @Override
    public Object toJSONValue(Distance value) {
        return value.getMeters();
    }

    @Override
    public Distance fromJSONValue(Object jsonValue) {
        return new MeterDistance(((Number) jsonValue).doubleValue());
    }

    @Override
    public String toStringValue(Distance value) {
        return value == null ? null : Double.toString(value.getMeters());
    }

    @Override
    public Distance fromStringValue(String stringValue) {
        return stringValue == null ? null : new MeterDistance(Double.parseDouble(stringValue));
    }

    @Override
    public Distance fromValue(Value value) {
        DoubleValue doubleValue = (DoubleValue) value;
        return doubleValue.getValue() == null ? null : new MeterDistance(doubleValue.getValue());
    }

    @Override
    public Value toValue(Distance value) {
        return value == null ? null : new DoubleValue(value.getMeters());
    }
}
