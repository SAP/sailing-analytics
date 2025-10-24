package com.sap.sse.common.settings.generic;

import com.sap.sse.common.settings.generic.base.AbstractValueSetSetting;
import com.sap.sse.common.settings.generic.converter.EnumConverter;

public class EnumSetSetting<T extends Enum<T>> extends AbstractValueSetSetting<T> {
    public EnumSetSetting(String name, AbstractGenericSerializableSettings settings, StringToEnumConverter<T> stringToEnumConverter) {
        super(name, settings, new EnumConverter<>(stringToEnumConverter));
    }

    public EnumSetSetting(String name, AbstractGenericSerializableSettings settings, Iterable<T> defaultValues, StringToEnumConverter<T> stringToEnumConverter) {
        super(name, settings, defaultValues, new EnumConverter<>(stringToEnumConverter));
    }
}
