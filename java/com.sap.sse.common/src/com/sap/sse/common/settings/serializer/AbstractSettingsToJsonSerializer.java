package com.sap.sse.common.settings.serializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.sse.common.settings.Settings;
import com.sap.sse.common.settings.generic.CollectionSetting;
import com.sap.sse.common.settings.generic.GenericSerializableSettings;
import com.sap.sse.common.settings.generic.Setting;
import com.sap.sse.common.settings.generic.SettingsListSetting;
import com.sap.sse.common.settings.generic.SettingsMap;
import com.sap.sse.common.settings.generic.ValueCollectionSetting;
import com.sap.sse.common.settings.generic.ValueConverter;
import com.sap.sse.common.settings.generic.ValueSetting;

/**
 * Base class for {@link Settings} serializers that produce JSON objects using some generic object model. This base
 * class ensures that the produced structure is equivalent for different JSON APIs so the setrialized JSON Strings
 * should be compatible and sharable between all those implementations.
 *
 * @param <OBJECT>
 *            JSON object type
 * @param <ARRAY>
 *            JSON Array type
 */
public abstract class AbstractSettingsToJsonSerializer<OBJECT, ARRAY> {

    protected abstract OBJECT newOBJECT();

    protected abstract void set(OBJECT jsonObject, String property, Object value);

    protected abstract Object get(OBJECT jsonObject, String property);
    
    protected abstract boolean hasProperty(OBJECT jsonObject, String property);

    protected abstract ARRAY ToJsonArray(Iterable<Object> values);

    protected abstract Iterable<Object> fromJsonArray(ARRAY jsonArray);
    
    protected abstract OBJECT parseStringToJsonObject(String jsonString) throws Exception;
    
    protected abstract String jsonObjectToString(OBJECT jsonObject);
    
    protected OBJECT parseStringToJsonObjectWithExceptionHandling(String jsonString) {
        try {
            return parseStringToJsonObject(jsonString);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse settings as JSON: " + jsonString, e);
        }
    }
    
    public String serializeToString(SettingsMap settings) {
        return jsonObjectToString(serialize(settings));
    }
    
    public <T extends SettingsMap> T deserialize(T settings, String jsonString) {
        if (jsonString != null && !jsonString.isEmpty()) {
            OBJECT jsonObject = parseStringToJsonObjectWithExceptionHandling(jsonString);
            if(jsonObject != null) {
                deserialize(settings, jsonObject);
            }
        }
        return settings;
    }
    
    public String serializeToString(GenericSerializableSettings settings) {
        return jsonObjectToString(serialize(settings));
    }

    public <T extends GenericSerializableSettings> T deserialize(T settings, String jsonString) {
        if (jsonString != null && !jsonString.isEmpty()) {
            OBJECT jsonObject = parseStringToJsonObjectWithExceptionHandling(jsonString);
            if(jsonObject != null) {
                deserialize(settings, jsonObject);
            }
        }
        return settings;
    }
    
    public OBJECT serialize(SettingsMap settingsMap) {
        final OBJECT jsonObject = newOBJECT();
        for (Map.Entry<String, Settings> entry : settingsMap.getSettingsByKey().entrySet()) {
            Settings settings = entry.getValue();
            final Object serializedObject;
            if(settings instanceof SettingsMap) {
                serializedObject = serialize((SettingsMap)settings);
            } else if(settings instanceof GenericSerializableSettings) {
                serializedObject = serialize((GenericSerializableSettings)settings);
            } else {
                serializedObject = null;
            }
            if(serializedObject != null) {
                set(jsonObject, entry.getKey().toString(), serializedObject);
            }
        }
        return jsonObject;
    }

    public OBJECT serialize(GenericSerializableSettings settings) {
        final OBJECT jsonObject = newOBJECT();
        for (Map.Entry<String, Setting> entry : settings.getChildSettings().entrySet()) {
            Setting setting = entry.getValue();
            if (!setting.isDefaultValue()) {
                set(jsonObject, entry.getKey(), serialize(setting));
            }
        }
        return jsonObject;
    }

    private Object serialize(Setting setting) {
        if (setting instanceof ValueSetting) {
            return serializeValueSetting((ValueSetting<?>) setting);
        } else if (setting instanceof CollectionSetting) {
            return serializeListSetting((CollectionSetting<?>) setting);
        } else if (setting instanceof GenericSerializableSettings) {
            return serialize((GenericSerializableSettings) setting);
        } else {
            throw new IllegalStateException("Unknown Setting type");
        }
    }

    private <T> Object serializeValueSetting(ValueSetting<T> valueSetting) {
        return valueSetting.getValueConverter().toJSONValue(valueSetting.getValue());
    }

    private <T> Object serializeListSetting(CollectionSetting<T> listSetting) {
        List<Object> jsonValues = new ArrayList<>();
        if (listSetting instanceof ValueCollectionSetting) {
            ValueCollectionSetting<T> valueListSetting = (ValueCollectionSetting<T>) listSetting;
            ValueConverter<T> converter = valueListSetting.getValueConverter();
            for (T value : valueListSetting.getValues()) {
                jsonValues.add(converter.toJSONValue(value));
            }
        } else if (listSetting instanceof SettingsListSetting) {
            SettingsListSetting<?> settingsListSetting = (SettingsListSetting<?>) listSetting;
            for (GenericSerializableSettings value : settingsListSetting.getValues()) {
                jsonValues.add(serialize(value));
            }
        } else {
            throw new IllegalStateException("Unknown ListSetting type");
        }
        return ToJsonArray(jsonValues);
    }
    
    public <T extends SettingsMap> T deserialize(T settingsMap, OBJECT json) {
        if (json != null) {
            for (Map.Entry<String, Settings> entry : settingsMap.getSettingsByKey().entrySet()) {
                String key = entry.getKey().toString();
                if(hasProperty(json, key)) {
                    Settings settings = entry.getValue();
                    Object serializedObject = get(json, key);
                    if(settings instanceof SettingsMap) {
                        @SuppressWarnings("unchecked")
                        OBJECT serializedChildObject = (OBJECT) serializedObject;
                        deserialize((SettingsMap)settings, serializedChildObject);
                    } else if(settings instanceof GenericSerializableSettings) {
                        deserializeObject((GenericSerializableSettings)settings, serializedObject);
                    }
                }
            }
        }
        return settingsMap;
    }

    public <T extends GenericSerializableSettings> T deserialize(T settings, OBJECT json) {
        if (json != null) {
            for (Map.Entry<String, Setting> entry : settings.getChildSettings().entrySet()) {
                Setting setting = entry.getValue();
                if(hasProperty(json, entry.getKey())) {
                    Object jsonValue = get(json, entry.getKey());
                    deserializeSetting(setting, jsonValue);
                }
            }
        }
        return settings;
    }

    private void deserializeSetting(Setting setting, Object jsonValue) {
        if (setting instanceof ValueSetting) {
            deserializeValueSetting(jsonValue, (ValueSetting<?>) setting);
        } else if (setting instanceof CollectionSetting) {
            deserializeListSetting(jsonValue, (CollectionSetting<?>) setting);
        } else if (setting instanceof GenericSerializableSettings) {
            deserializeObject((GenericSerializableSettings) setting, jsonValue);
        } else {
            throw new IllegalStateException("Unknown Setting type");
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends GenericSerializableSettings> T deserializeObject(T setting, Object jsonValue) {
        return deserialize(setting, (OBJECT) jsonValue);
    }

    private <T> void deserializeValueSetting(Object jsonValue, ValueSetting<T> valueSetting) {
        valueSetting.setValue(valueSetting.getValueConverter().fromJSONValue(jsonValue));
    }

    private <T> void deserializeListSetting(Object jsonValue, CollectionSetting<T> listSetting) {
        @SuppressWarnings("unchecked")
        ARRAY jsonArray = (ARRAY) jsonValue;
        if (listSetting instanceof ValueCollectionSetting) {
            ValueCollectionSetting<T> valueListSetting = (ValueCollectionSetting<T>) listSetting;
            ValueConverter<T> converter = valueListSetting.getValueConverter();
            List<T> values = new ArrayList<>();
            for (Object value : fromJsonArray(jsonArray)) {
                values.add(converter.fromJSONValue(value));
            }
            valueListSetting.setValues(values);
        } else if (listSetting instanceof SettingsListSetting) {
            deserializeSettingsListSetting(jsonArray, (SettingsListSetting<?>) listSetting);
        } else {
            throw new IllegalStateException("Unknown ListSetting type");
        }
    }

    private <T extends GenericSerializableSettings> void deserializeSettingsListSetting(ARRAY jsonArray, SettingsListSetting<T> settingsListSetting) {
        List<T> values = new ArrayList<>();
        for (Object value : fromJsonArray(jsonArray)) {
            values.add(deserializeObject(settingsListSetting.getSettingsFactory().newInstance(), value));
        }
        settingsListSetting.setValues(values);
    }
}
