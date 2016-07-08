package com.sap.sse.shared.android.test;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Test;

import com.sap.sse.common.filter.TextOperator;
import com.sap.sse.common.filter.TextOperator.Operators;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.DecimalListSetting;
import com.sap.sse.common.settings.generic.DecimalSetting;
import com.sap.sse.common.settings.generic.EnumListSetting;
import com.sap.sse.common.settings.generic.EnumSetting;
import com.sap.sse.common.settings.generic.GenericSerializableSettings;
import com.sap.sse.common.settings.generic.SettingsList;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.StringToEnumConverter;

public abstract class AbstractSettingsSerializationTest<SOT> {

    private static class TestOuterSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = -7379232503773525915L;
        private transient SimpleTestSettings nested;

        public TestOuterSettings() {
        }
        
        @Override
        protected void addChildSettings() {
            nested = new SimpleTestSettings("nested", this);
        }
    }

    private static class TestListSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = 6919127895749914961L;
        private transient SettingsList<SimpleTestSettings> l;

        public TestListSettings() {
        }
        
        @Override
        protected void addChildSettings() {
            l = new SettingsList<>("l", this, SimpleTestSettings::new);
        }
    }

    private static class SimpleTestSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = -4134836978411240572L;
        private transient StringSetting string;
        private transient DecimalSetting num;

        public SimpleTestSettings() {
        }

        public SimpleTestSettings(String string, BigDecimal num) {
            this.string.setValue(string);
            this.num.setValue(num);
        }

        public SimpleTestSettings(String name, AbstractGenericSerializableSettings parent) {
            super(name, parent);
        }
        
        @Override
        protected void addChildSettings() {
            string = new StringSetting("string", this);
            num = new DecimalSetting("num", this);
        }
    }

    private static class TestSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = -611806711715538293L;
        private transient StringSetting humba;
        private transient BooleanSetting bumpa;
        private transient EnumSetting<TextOperator.Operators> trala;
        private transient DecimalSetting num;
        private transient DecimalListSetting l;

        public TestSettings() {
        }
        
        @Override
        protected void addChildSettings() {
            humba = new StringSetting("humba", this);
            bumpa = new BooleanSetting("bumpa", this);
            trala = new EnumSetting<>("trala", this, TextOperator.Operators::valueOf);
            num = new DecimalSetting("num", this);
            l = new DecimalListSetting("l", this);
        }
    }

    private static class TestEnumListSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = 93688955681544920L;
        private transient EnumListSetting<TextOperator.Operators> l;
        
        public TestEnumListSettings() {
        }

        @Override
        protected void addChildSettings() {
            l = new EnumListSetting<>("l", this,
                    new StringToEnumConverter<TextOperator.Operators>() {
                @Override
                public Operators fromString(String stringValue) {
                    return TextOperator.Operators.valueOf(stringValue);
                }
            });
        }
    }

    private static class DuplicateFieldSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = 4058775568295038177L;
        @SuppressWarnings("unused")
        private transient StringSetting humba;
        @SuppressWarnings("unused")
        private transient DecimalSetting bumba;
        
        @Override
        protected void addChildSettings() {
            humba = new StringSetting("humba", this);
            bumba = new DecimalSetting("humba", this);
        }
    }
    
    private static class DisallowedKeySettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = -2265305217290147424L;
        @SuppressWarnings("unused")
        private transient StringSetting disallowedKey;
        
        @Override
        protected void addChildSettings() {
            disallowedKey= new StringSetting("disallowed.key", this);
        }
    }

    protected abstract <T extends GenericSerializableSettings> SOT serialize(T settings) throws Exception;

    protected <T extends GenericSerializableSettings> T deserialize(SOT serializedObject, Class<T> settingsClass) throws Exception {
        T deserializedInstance = settingsClass.newInstance();

        return deserialize(serializedObject, deserializedInstance);
    }
    
    protected <T extends GenericSerializableSettings> T deserialize(SOT serializedObject, T settings) throws Exception {
        return null;
    };

    @SuppressWarnings("unchecked")
    private <T extends GenericSerializableSettings> T serializeAndDeserialize(T objectToSerialize) {
        try {
            SOT serialized = serialize(objectToSerialize);
            return deserialize(serialized, (Class<T>) objectToSerialize.getClass());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFlatJsonSerialization() {
        final TestSettings settings = new TestSettings();
        settings.humba.setValue("trala");
        settings.bumpa.setValue(true);
        settings.trala.setValue(TextOperator.Operators.Contains);
        settings.num.setValue(BigDecimal.TEN);
        settings.l.setValues(Arrays.asList(BigDecimal.ONE, BigDecimal.valueOf(2), BigDecimal.valueOf(3)));

        final TestSettings deserializedSettings = serializeAndDeserialize(settings);
        assertEquals(settings, deserializedSettings);
    }

    @Test
    public void testNestedJsonSerialization() {
        final TestOuterSettings outerSettings = new TestOuterSettings();
        final SimpleTestSettings settings = outerSettings.nested;
        settings.string.setValue("trala");
        settings.num.setValue(BigDecimal.TEN);

        final TestOuterSettings deserializedSettings = serializeAndDeserialize(outerSettings);
        assertEquals(outerSettings, deserializedSettings);
    }

    /**
     * Verifies that it is not possible to have two equally named child settings that would cause conflicts on
     * serialization.
     */
    @Test(expected = RuntimeException.class)
    public void testDuplicateSetting() {
        new DuplicateFieldSettings();
    }
    
    /**
     * Verifies that it is not possible to have setting names with ".".
     */
    @Test(expected = RuntimeException.class)
    public void testDisallowedSettingNameSetting() {
        new DisallowedKeySettings();
    }

    /**
     * A white-box test that tests property name escaping in case the property name conflicts with how type names (used
     * particularly for enum settings) are represented in JSON.
     */
    @Test
    public void testJsonSerializationWithEnumTypedList() {
        final TestEnumListSettings settings = new TestEnumListSettings();
        settings.l.setValues(Arrays.asList(Operators.Contains, Operators.EndsWith));

        final TestEnumListSettings deserializedSettings = serializeAndDeserialize(settings);
        assertEquals(settings, deserializedSettings);
    }

    @Test
    public void testJsonSerializationWithNestedSettingsList() {
        final TestListSettings settings = new TestListSettings();
        settings.l.setValues(Arrays.asList(new SimpleTestSettings("blubb", BigDecimal.TEN), new SimpleTestSettings(
                "bla", BigDecimal.ONE)));

        final TestListSettings deserializedSettings = serializeAndDeserialize(settings);
        assertEquals(settings, deserializedSettings);
    }
}
