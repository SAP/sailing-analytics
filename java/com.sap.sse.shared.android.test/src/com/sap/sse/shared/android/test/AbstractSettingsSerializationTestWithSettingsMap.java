package com.sap.sse.shared.android.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.filter.TextOperator;
import com.sap.sse.common.filter.TextOperator.Operators;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.DecimalSetting;
import com.sap.sse.common.settings.generic.EnumSetSetting;
import com.sap.sse.common.settings.generic.EnumSetting;
import com.sap.sse.common.settings.generic.GenericSerializableSettings;
import com.sap.sse.common.settings.generic.SettingsMap;
import com.sap.sse.common.settings.generic.StringSetSetting;
import com.sap.sse.common.settings.generic.StringSetting;

public abstract class AbstractSettingsSerializationTestWithSettingsMap<SOT> extends AbstractSettingsSerializationTest<SOT> {
    private static class UntypedSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = -2265305217290147424L;
        private transient StringSetting enumSetting;
        private transient StringSetSetting enumSetSetting;
        private transient StringSetting numberSetting;
        private transient StringSetting booleanSetting;
        
        public UntypedSettings(String enumValue, Collection<String> enumValues, String numberValue, String booleanValue) {
            super();
            enumSetting.setValue(enumValue);
            enumSetSetting.setValues(enumValues);
            numberSetting.setValue(numberValue);
            booleanSetting.setValue(booleanValue);
        }
        
        @Override
        protected void addChildSettings() {
            enumSetting = new StringSetting("enumSetting", this);
            enumSetSetting = new StringSetSetting("enumSetSetting", this);
            numberSetting = new StringSetting("numberSetting", this);
            booleanSetting = new StringSetting("booleanSetting", this);
        }
    }
    
    private static class TypedSettings extends AbstractGenericSerializableSettings {
        public TypedSettings() {
            super();
        }

        private static final long serialVersionUID = -2265305217290147424L;
        @SuppressWarnings("unused")
        private transient EnumSetting<TextOperator.Operators> enumSetting;
        private transient EnumSetSetting<TextOperator.Operators> enumSetSetting;
        @SuppressWarnings("unused")
        private transient DecimalSetting numberSetting;
        @SuppressWarnings("unused")
        private transient BooleanSetting booleanSetting;
        
        @Override
        protected void addChildSettings() {
            enumSetting = new EnumSetting<>("enumSetting", this, TextOperator.Operators::valueOf);
            enumSetSetting = new EnumSetSetting<>("enumSetSetting", this, TextOperator.Operators::valueOf);
            numberSetting = new DecimalSetting("numberSetting", this);
            booleanSetting = new BooleanSetting("booleanSetting", this);
        }
    }
    
    @Test
    public void testSettingsMapWithOneSerializableSettings() throws Exception {
        final GenericSerializableSettings settingsValues = createTestSettingsWithValues();
        
        SettingsMap settingsMapToSave = new SettingsMapImpl("aaa", settingsValues);

        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        Settings deserializedSettingsValues = deserializedSettingsMap.getSettingsPerComponentId().get("aaa");
        assertEquals(deserializedSettingsValues, createTestSettingsWithValues());
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 1);
    }
    
    @Test
    public void testSettingsMapWithMultipleSerializableSettings() throws Exception {
        final GenericSerializableSettings settingsValues1 = createTestSettingsWithValues();
        final GenericSerializableSettings settingsValues2 = createTestSettingsWithValues2();
        Map<String, Settings> settings = new HashMap<>();
        settings.put("aaa", settingsValues1);
        settings.put("bbb", settingsValues2);
        
        SettingsMap settingsMapToSave = new SettingsMapImpl(settings);

        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        Settings deserializedSettingsValues1 = deserializedSettingsMap.getSettingsPerComponentId().get("aaa");
        Settings deserializedSettingsValues2 = deserializedSettingsMap.getSettingsPerComponentId().get("bbb");
        assertEquals(deserializedSettingsValues1, createTestSettingsWithValues());
        assertEquals(deserializedSettingsValues2, createTestSettingsWithValues2());
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 2);
    }
    
    @Test
    public void testSettingsMapWithNullKey() throws Exception {
        final GenericSerializableSettings settingsValues1 = createTestSettingsWithValues();
        final GenericSerializableSettings settingsValues2 = createTestSettingsWithValues2();
        Map<String, Settings> settings = new HashMap<>();
        settings.put("aaa", settingsValues1);
        settings.put(null, settingsValues2);
        
        SettingsMap settingsMapToSave = new SettingsMapImpl(settings);
        
        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        Settings deserializedSettingsValues1 = deserializedSettingsMap.getSettingsPerComponentId().get("aaa");
        Settings deserializedSettingsValues2 = deserializedSettingsMap.getSettingsPerComponentId().get(null);
        assertEquals(deserializedSettingsValues1, createTestSettingsWithValues());
        assertEquals(deserializedSettingsValues2, createTestSettingsWithValues2());
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 2);
    }
    
    @Test
    public void testSettingsMapWithNestedSettingsMapWithOneSerializableSettingsChild() throws Exception {
        final GenericSerializableSettings childSettingsValues = createTestSettingsWithValues();
        SettingsMap childSettingsMap = new SettingsMapImpl("aaa", childSettingsValues);
        SettingsMap settingsMapToSave = new SettingsMapImpl("bbb", childSettingsMap);

        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        SettingsMap deserializedChildSettingsMap = (SettingsMap) deserializedSettingsMap.getSettingsPerComponentId()
                .get("bbb");
        Settings deserializedChildSettingsValues = deserializedChildSettingsMap.getSettingsPerComponentId().get("aaa");
        assertEquals(deserializedChildSettingsValues, createTestSettingsWithValues());
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 1);
        assertEquals(deserializedChildSettingsMap.getSettingsPerComponentId().size(), 1);
    }
    
    @Test
    public void testSettingsMapWithOneSerializableSettingsAndNestedSettingsMapWithOneSerializableSettings() throws Exception {
        final GenericSerializableSettings childSettingsValues = createTestSettingsWithValues2();
        SettingsMap childSettingsMap = new SettingsMapImpl("aaa", childSettingsValues);
        
        final GenericSerializableSettings settingsValues = createTestSettingsWithValues();
        Map<String, Settings> settings = new HashMap<>();
        settings.put("aaa", settingsValues);
        settings.put("bbb", childSettingsMap);
        final SettingsMap settingsMapToSave = new SettingsMapImpl(settings);
        
        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        SettingsMap deserializedChildSettingsMap = (SettingsMap) deserializedSettingsMap.getSettingsPerComponentId()
                .get("bbb");
        Settings deserializedSettingsValues = deserializedSettingsMap.getSettingsPerComponentId().get("aaa");
        Settings deserializedChildSettingsValues = deserializedChildSettingsMap.getSettingsPerComponentId().get("aaa");
        
        assertEquals(deserializedSettingsValues, createTestSettingsWithValues());
        assertEquals(deserializedChildSettingsValues, createTestSettingsWithValues2());
        
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 2);
        assertEquals(deserializedChildSettingsMap.getSettingsPerComponentId().size(), 1);
    }
    
    @Test
    public void testSettingsMapWithOneNonSerializableSettings() throws Exception {
        final Settings settingsValuesNonSerializable = createNonSerializableTestSettingsWithChangedValues();
        
        SettingsMap settingsMapToSave = new SettingsMapImpl("aaa", settingsValuesNonSerializable);

        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        
        Settings deserializedSettingsValues = deserializedSettingsMap.getSettingsPerComponentId().get("aaa");
        assertEquals(deserializedSettingsValues, createNonSerializableTestSettingsWithDefaultValues());
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 1);
    }
    
    @Test
    public void testSettingsMapWithOneSerializableAndOneNonSerializableSettings() throws Exception {
        
        final GenericSerializableSettings childSettingsValues = createTestSettingsWithValues();
        final Settings childSettingsValuesNonSerializable = createNonSerializableTestSettingsWithChangedValues();
        Map<String, Settings> settings = new HashMap<>();
        settings.put("aaa", childSettingsValues);
        settings.put("bbb", childSettingsValuesNonSerializable);
        
        SettingsMap settingsMapToSave = new SettingsMapImpl(settings);
        
        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        Settings deserializedSettingsValues = deserializedSettingsMap.getSettingsPerComponentId().get("aaa");
        Settings deserializedSettingsValuesNonSerializable = deserializedSettingsMap.getSettingsPerComponentId()
                .get("bbb");
        assertEquals(deserializedSettingsValues, createTestSettingsWithValues());
        assertEquals(deserializedSettingsValuesNonSerializable, createNonSerializableTestSettingsWithDefaultValues());
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 2);
    }
    
    @Test
    public void testSettingsMapWithNestedSerializableAndNonSerializableSettings() throws Exception {
        
        final GenericSerializableSettings childSettingsValues = createTestSettingsWithValues();
        final Settings childSettingsNonSerializable = createNonSerializableTestSettingsWithChangedValues();
        Map<String, Settings> childSettings = new HashMap<>();
        childSettings.put("aaa", childSettingsValues);
        childSettings.put("bbb", childSettingsNonSerializable);
        
        SettingsMap childSettingsMap = new SettingsMapImpl(childSettings);
        
        final GenericSerializableSettings settingsValues = createTestSettingsWithValues();
        final Settings settingsValuesNonSerializable = createNonSerializableTestSettingsWithChangedValues();
        Map<String, Settings> settings = new HashMap<>();
        settings.put("aaa", childSettingsMap);
        settings.put("bbb", settingsValuesNonSerializable);
        settings.put("ccc", settingsValues);
        
        SettingsMap settingsMapToSave = new SettingsMapImpl(settings);
        
        final SettingsMap deserializedSettingsMap = serializeAndDeserialize(settingsMapToSave);
        SettingsMap deserializedChildSettingsMap = (SettingsMap) deserializedSettingsMap.getSettingsPerComponentId()
                .get("aaa");
        Settings deserializedSettingsValuesNonSerializable = deserializedSettingsMap.getSettingsPerComponentId()
                .get("bbb");
        Settings deserializedSettingsValues = deserializedSettingsMap.getSettingsPerComponentId().get("ccc");
        
        assertEquals(deserializedSettingsValues, createTestSettingsWithValues());
        assertEquals(deserializedSettingsValuesNonSerializable, createNonSerializableTestSettingsWithDefaultValues());
        assertEquals(deserializedSettingsMap.getSettingsPerComponentId().size(), 3);
        
        Settings deserializedChildSettingsValues = deserializedChildSettingsMap.getSettingsPerComponentId().get("aaa");
        Settings deserializedChildSettingsValuesNonSerializable = deserializedChildSettingsMap
                .getSettingsPerComponentId().get("bbb");
        
        assertEquals(deserializedChildSettingsValues, createTestSettingsWithValues());
        assertEquals(deserializedChildSettingsValuesNonSerializable, createNonSerializableTestSettingsWithDefaultValues());
        assertEquals(deserializedChildSettingsMap.getSettingsPerComponentId().size(), 2);
    }
    
    private static class SettingsMapImpl implements SettingsMap {
        private final Map<String, Settings> innerSettings;
        
        public SettingsMapImpl(String key, Settings settings) {
            innerSettings = new HashMap<>();
            innerSettings.put(key, settings);
        }
        
        public SettingsMapImpl(Map<String, Settings> settings) {
            innerSettings = new HashMap<>(settings);
        }

        @Override
        public Map<String, Settings> getSettingsPerComponentId() {
            return innerSettings;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((innerSettings == null) ? 0 : innerSettings.hashCode());
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
            SettingsMapImpl other = (SettingsMapImpl) obj;
            if (innerSettings == null) {
                if (other.innerSettings != null)
                    return false;
            } else if (!innerSettings.equals(other.innerSettings))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "SettingsMapImpl [innerSettings=" + innerSettings + "]";
        }
    }
    
    protected abstract SettingsMap deserialize(SOT serializedObject, SettingsMap settingsMap) throws Exception;
    
    protected abstract SOT serialize(SettingsMap settings) throws Exception;
    
    private SettingsMap serializeAndDeserialize(SettingsMap settingsMap) throws Exception {
        SOT serialized = serialize(settingsMap);
        SettingsMap reconstructedSettingsMap = reconstructSettingsMap(settingsMap);
        return deserialize(serialized, reconstructedSettingsMap);
    }

    private SettingsMap reconstructSettingsMap(SettingsMap settingsMap) throws Exception {
        Map<String, Settings> innerMap = new HashMap<>();
        for (Map.Entry<String, Settings> entry : settingsMap.getSettingsPerComponentId().entrySet()) {
            Settings innerSettings = entry.getValue();
            Settings copiedSettings;
            if(innerSettings instanceof SettingsMap) {
                copiedSettings = reconstructSettingsMap((SettingsMap)innerSettings);
            } else {
                copiedSettings = innerSettings.getClass().getDeclaredConstructor().newInstance();
            }
            innerMap.put(entry.getKey(), copiedSettings);
        }
        return new SettingsMapImpl(innerMap);
    }
    
    /**
     * Regression test for Bug 4383.
     * When the serialized settings contain values that can't be converted to the setting
     * type, those values may not break the deserialization. If a user e.g. manipulates the URL, the page may not stay
     * empty because of an internal exception. In this case, the wrong values should be ignored instead but all other
     * values must be deserialized.
     */
    @Test
    public void testIllegalValuesInSerializedSettings() throws Exception {
        UntypedSettings untypedSettings = new UntypedSettings("WrongEnumValue", Arrays.asList("Contains", "WrongEnumValue"), "noNumber", "noBoolean");
        SOT serialized = serialize(untypedSettings);
        TypedSettings deserialized = deserialize(serialized, new TypedSettings());
        TypedSettings expected = new TypedSettings();
        // this is the only value that will be correctly deserialized
        expected.enumSetSetting.addValue(Operators.Contains);
        assertEquals(expected, deserialized);
    }
}
