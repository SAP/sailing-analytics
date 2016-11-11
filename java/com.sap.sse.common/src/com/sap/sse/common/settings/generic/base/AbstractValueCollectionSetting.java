package com.sap.sse.common.settings.generic.base;

import java.util.Collection;
import java.util.Collections;

import com.sap.sse.common.Util;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.ValueCollectionSetting;
import com.sap.sse.common.settings.generic.ValueConverter;
import com.sap.sse.common.settings.value.Value;
import com.sap.sse.common.settings.value.ValueCollectionValue;

public abstract class AbstractValueCollectionSetting<T, C extends Collection<Value>, D extends Collection<T>> extends AbstractHasValueSetting<T> implements ValueCollectionSetting<T> {

    private final D defaultValues = createDefaultValuesCollection();
    private final boolean emptyIsDefault;
    
    public AbstractValueCollectionSetting(String name, AbstractGenericSerializableSettings settings, ValueConverter<T> valueConverter, boolean emptyIsDefault) {
        super(name, settings, valueConverter);
        this.emptyIsDefault = emptyIsDefault;
    }
    
    @SuppressWarnings("unchecked")
    protected ValueCollectionValue<C> getValue() {
        return (ValueCollectionValue<C>) settings.getValue(settingName);
    }
    
    protected abstract ValueCollectionValue<C> createValue();
    
    private ValueCollectionValue<C> ensureValue() {
        ValueCollectionValue<C> result = getValue();
        if(result == null) {
            result = createValue();
            settings.setValue(settingName, result);
        }
        return result;
    }
    
    protected abstract D createDefaultValuesCollection();
    
    @Override
    public Iterable<T> getValues() {
        ValueCollectionValue<C> value = getValue();
        if(emptyIsDefault && (value == null || value.isEmpty())) {
            return Collections.unmodifiableCollection(defaultValues);
        }
        if(value == null) {
            return Collections.emptyList();
        }
        return value.getValues(getValueConverter());
    }

    @Override
    public final void setValues(Iterable<T> values) {
        ensureValue().setValues(values, getValueConverter());
    }
    
    public void addValue(T value) {
        ensureValue().addValue(value, getValueConverter());
    }

    public void clear() {
        ensureValue().clear();
    }
    @Override
    public void resetToDefault() {
        setValues(defaultValues);
    }
    
    @Override
    public boolean isDefaultValue() {
        ValueCollectionValue<C> value = getValue();
        return (emptyIsDefault && (value == null || value.isEmpty()))
                || (value.size() == defaultValues.size() && defaultValues.containsAll(value.getValues(getValueConverter())));
    }
    
    @Override
    public final void setDefaultValues(Iterable<T> defaultValues) {
        boolean wasDefault = isDefaultValue();
        this.defaultValues.clear();
        if(defaultValues != null) {
            Util.addAll(defaultValues, this.defaultValues);
        }
        if(wasDefault) {
            resetToDefault();
        }
    }

    @Override
    public String toString() {
        ValueCollectionValue<C> value = getValue();
        if(value == null) {
            return "[]";
        }
        return value.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        ValueCollectionValue<C> value = getValue();
        result = prime * result + ((value == null) ? 0 : getValue().hashCode());
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
        AbstractValueCollectionSetting other = (AbstractValueCollectionSetting) obj;
        ValueCollectionValue<C> value = getValue();
        ValueCollectionValue<?> otherValue = other.getValue();
        if (value == null) {
            if (otherValue != null)
                return false;
        } else if (!value.equals(otherValue))
            return false;
        return true;
    }
}
