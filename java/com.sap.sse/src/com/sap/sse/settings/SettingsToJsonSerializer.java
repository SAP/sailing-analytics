package com.sap.sse.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.settings.AbstractSettings;
import com.sap.sse.common.settings.EnumSetting;
import com.sap.sse.common.settings.ListSetting;
import com.sap.sse.common.settings.MapSettings;
import com.sap.sse.common.settings.NumberSetting;
import com.sap.sse.common.settings.Setting;
import com.sap.sse.common.settings.SettingType;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.common.settings.StringSetting;

/**
 * Serializes a {@link Settings} object to a {@link JSONObject}. All setting types are supported as top-level entities.
 * Nesting of {@link Settings} is generally supported. For example, a {@link Settings} object can be contains as one
 * setting within another {@link Settings} object. However, when nesting {@link EnumSetting} objects within a
 * {@link ListSetting} then it is mandatory to put this {@link ListSetting} object into a {@link Settings} object's
 * {@link Settings#getNonDefaultSettings()}, such as into a {@link MapSettings} object. The technical reason for this
 * limitation is that the {@link Class} object for the enumeration type must be stored with the serialized format in
 * order to re-construct the enumeration literals of the correct type. For readability of the produced JSON output,
 * this implementation chooses to store the enumeration class's name in a special property that is sibling of the
 * enumeration property. A {@link ListSetting} object does not contain named properties and therefore does not allow
 * us to add such a property easily.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class SettingsToJsonSerializer {
    private final static String TYPE_PROPERTY_SUFFIX = "___TYPE";
    private final static char TYPE_PROPERTY_SUFFIX_ESCAPE = ':';
    
    public JSONObject serialize(Settings settings) {
        final Map<String, Setting> settingsToSerialize = settings.getNonDefaultSettings();
        JSONObject jsonObject = new JSONObject();
        for (Entry<String, Setting> e : settingsToSerialize.entrySet()) {
            Object settingAsJson = serialize(e.getValue());
            jsonObject.put(escapePropertyName(e.getKey()), settingAsJson);
            final String typePropertyName = getTypePropertyName(e.getKey());
            if (e.getValue() instanceof EnumSetting<?>) {
                jsonObject.put(typePropertyName, getEnumTypeName((EnumSetting<?>) e.getValue()));
            } else if (e.getValue() instanceof ListSetting<?>) {
                Iterator<?> i = ((ListSetting<?>) e.getValue()).iterator();
                if (i.hasNext()) {
                    Object o = i.next();
                    if (o instanceof EnumSetting<?>) {
                        jsonObject.put(typePropertyName, getEnumListTypeName(((EnumSetting<?>) o).getValue()));
                    }
                }
            }
        }
        return jsonObject;
    }

    /**
     * A list containing enum literals shall be represented as simply holding these literals as strings. However,
     * when de-serializing, the list elements need to be constructed as the proper enum's literals again, so we need
     * to store the enumeration class name together with the information that it's a list.
     */
    private Object getEnumListTypeName(Enum<?> literal) {
        return SettingType.LIST.name()+"/"+literal.getClass().getName();
    }

    private Object getEnumTypeName(EnumSetting<?> value) {
        return SettingType.ENUM.name()+"/"+value.getValue().getClass().getName();
    }

    private String getTypePropertyName(String unescapedKey) {
        return unescapedKey+TYPE_PROPERTY_SUFFIX;
    }

    private boolean isTypePropertyName(String unescapedKey) {
        return unescapedKey.endsWith(TYPE_PROPERTY_SUFFIX) && !unescapedKey.endsWith(TYPE_PROPERTY_SUFFIX_ESCAPE+TYPE_PROPERTY_SUFFIX);
    }
    
    /**
     * If <code>key</code> looks like a result of {@link #getTypePropertyName(String)}, the string is escaped such that
     * it does not more and that {@link #unescapePropertyName} will return <code>key</code>.<p>
     */
    String escapePropertyName(String key) {
        return key.replaceAll(TYPE_PROPERTY_SUFFIX, TYPE_PROPERTY_SUFFIX_ESCAPE+TYPE_PROPERTY_SUFFIX);
    }
    
    String unescapePropertyName(String escapedKey) {
        return escapedKey.replaceAll(TYPE_PROPERTY_SUFFIX_ESCAPE+TYPE_PROPERTY_SUFFIX, TYPE_PROPERTY_SUFFIX);
    }
    
    private Object serialize(Setting setting) {
        switch (setting.getType()) {
        case ENUM:
            return serialize((EnumSetting<?>) setting);
        case LIST:
            return serialize((ListSetting<?>) setting);
        case MAP:
            return serialize((Settings) setting);
        case NUMBER:
            return serialize((NumberSetting) setting);
        case STRING:
            return serialize((StringSetting) setting);
        default:
            throw new IllegalArgumentException("Don't know setting of type "+setting.getType());
        }
    }
    
    private <T extends Enum<T>> String serialize(EnumSetting<T> enumSetting) {
        return enumSetting.getValue().name();
    }

    private <T extends Setting> JSONArray serialize(ListSetting<T> listSetting) {
        JSONArray result = new JSONArray();
        for (T t : listSetting) {
            result.add(serialize(t));
        }
        return result;
    }

    private Number serialize(NumberSetting numberSetting) {
        return numberSetting.getNumber();
    }

    private String serialize(StringSetting stringSetting) {
        return stringSetting.getString();
    }

    public Settings deserialize(JSONObject json) throws ClassNotFoundException {
        final Map<String, Setting> settings = new HashMap<>();
        for (Entry<Object, Object> e : json.entrySet()) {
            String escapedKey = (String) e.getKey();
            String unescapedKey = unescapePropertyName(escapedKey);
            if (!isTypePropertyName(escapedKey)) {
                final Object value = e.getValue();
                String typePropertyName = getTypePropertyName(unescapedKey);
                // all properties must specify their type unless they are of type String, JSONArray or Number;
                // however, properties of type enum will be represented as strings
                final SettingType type;
                final Class<?> clazz;
                if (json.containsKey(typePropertyName)) {
                    String[] typeNameAndClassName = ((String) json.get(typePropertyName)).split("/");
                    type = SettingType.valueOf(typeNameAndClassName[0]);
                     if (typeNameAndClassName.length > 1) {
                         clazz = Class.forName(typeNameAndClassName[1]);
                     } else {
                        clazz = null;
                     }
                } else {
                    type = getSettingType(value);
                    clazz = null;
                }
                Setting setting = createSettingFromObjectAndType(value, type, clazz);
                settings.put(unescapePropertyName(escapedKey), setting);
            }
        }
        return new AbstractSettings() {
            @Override
            public Map<String, Setting> getNonDefaultSettings() {
                return settings;
            }
        };
    }

    private SettingType getSettingType(final Object value) {
        final SettingType type;
        if (value instanceof String) {
            type = SettingType.STRING;
        } else if (value instanceof Number){
            type = SettingType.NUMBER;
        } else if (value instanceof JSONArray) {
            type = SettingType.LIST;
        } else if (value instanceof JSONObject) {
            type = SettingType.MAP;
        } else {
            throw new IllegalArgumentException("Don't know how to de-serialize setting "+value+" of type "+value.getClass());
        }
        return type;
    }

    private Setting createSettingFromObjectAndType(Object obj, final SettingType type, Class<?> clazz)
            throws ClassNotFoundException {
        final Setting setting;
        switch (type) {
        case ENUM:
            Enum<?> value = null;
            @SuppressWarnings("unchecked")
            Class<Enum<?>> enumClass = (Class<Enum<?>>) clazz;
            for (Enum<?> literal : enumClass.getEnumConstants()) {
                if (literal.name().equals((String) obj)) {
                    value = literal;
                }
            }
            setting = new EnumSetting<>(value);
            break;
        case LIST:
            JSONArray array = (JSONArray) obj;
            List<Setting> settingList = new ArrayList<>(); 
            for (Object o : array) {
                settingList.add(createSettingFromObjectAndType(o, clazz==null?getSettingType(o):SettingType.ENUM, clazz));
            }
            setting = new ListSetting<>(settingList);
            break;
        case MAP:
            setting = deserialize((JSONObject) obj);
            break;
        case NUMBER:
            setting = new NumberSetting((Number) obj);
            break;
        case STRING:
            setting = new StringSetting((String) obj);
            break;
        default:
            throw new IllegalArgumentException("Don't know how to de-serialize setting "+obj+" of type "+obj.getClass());
        }
        return setting;
    }
}
