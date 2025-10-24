package com.sap.sse.common.settings.generic.base;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.ValueConverter;
import com.sap.sse.common.settings.generic.ValueSetting;
import com.sap.sse.common.settings.value.Value;

public abstract class AbstractValueSetting<T> extends AbstractHasValueSetting<T> implements ValueSetting<T> {
    
    private T defaultValue;

    protected AbstractValueSetting(String name, AbstractGenericSerializableSettings settings, T defaultValue, ValueConverter<T> valueConverter) {
        super(name, settings, valueConverter);
        this.defaultValue = defaultValue;
        if (settings.getValue(settingName) == null) {
            resetToDefault();
        }
    }

    @Override
    public T getValue() {
        T result = null;
        Value value = settings.getValue(settingName);
        if (value != null) {
            result = getValueConverter().fromValue(value);
        }
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    @Override
    public void setValue(T value) {
        setValueWithoutPermittionCheck(value);
    }

    private void setValueWithoutPermittionCheck(T value) {
        settings.setValue(settingName, getValueConverter().toValue(value));
    }

    @Override
    public boolean isDefaultValue() {
        T value = getValue();
        if (value == defaultValue) {
            return true;
        }
        if (value != null) {
            return value.equals(defaultValue);
        }
        // value == null && defaultValue != null => value isn't default
        return false;
    }
    
    @Override
    public void resetToDefault() {
        //setValue(defaultValue);
        setValueWithoutPermittionCheck(defaultValue);
    }
    
    @Override
    public final void setDefaultValue(T defaultValue) {
        boolean wasDefault = isDefaultValue();
        this.defaultValue = defaultValue;
        if (wasDefault) {
            resetToDefault();
        }
    }
    
    @Override
    public final T getDefaultValue() {
        return defaultValue;
    }
    
    @Override
    public String toString() {
        return "" +  getValue();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        T value = getValue();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("rawtypes")
        AbstractValueSetting other = (AbstractValueSetting) obj;
        T value = getValue();
        Object otherValue = other.getValue();
        if (value == null) {
            if (otherValue != null)
                return false;
        } else if (!value.equals(otherValue))
            return false;
        return true;
    }
}
