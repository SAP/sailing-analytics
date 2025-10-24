package com.sap.sse.common.test.settings.generic.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.DecimalSetting;
import com.sap.sse.common.settings.generic.StringSetSetting;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.support.SettingsUtil;

public class SettingsUtilTest {
    
    private static final BigDecimal someNumber = new BigDecimal(54321);
    private static final String someString = "xyz";
    private static final HashSet<String> someStrings = new HashSet<>(Arrays.asList(someString, "uvw"));
    
    private static final BigDecimal defaultNumber = new BigDecimal(1337);
    private static final String defaultString = "abc";
    private static final HashSet<String> defaultStrings = new HashSet<>(Arrays.asList(defaultString, "def"));

    private static class OuterSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = -7379232503773525915L;
        private transient TestSettings nested;

        public OuterSettings() {
            super();
        }
        
        @Override
        protected void addChildSettings() {
            nested = new TestSettings("nested", this);
        }
    }

    private static class TestSettings extends AbstractGenericSerializableSettings {
        private static final long serialVersionUID = -4134836978411240572L;
        private transient StringSetting string;
        private transient DecimalSetting number;
        private transient StringSetSetting strings;

        public TestSettings(String name, AbstractGenericSerializableSettings parent) {
            super(name, parent);
        }
        
        @Override
        protected void addChildSettings() {
            string = new StringSetting("string", this);
            number = new DecimalSetting("number", this);
            strings = new StringSetSetting("strings", this);
        }
    }
    
    @Test
    public void testDefaults() {
        final OuterSettings outerSettingsWithDefaults = new OuterSettings();
        final TestSettings innerSettingsWithDefaults = outerSettingsWithDefaults.nested;
        
        innerSettingsWithDefaults.string.setDefaultValue(defaultString);
        innerSettingsWithDefaults.number.setDefaultValue(defaultNumber);
        innerSettingsWithDefaults.strings.setDefaultValues(defaultStrings);
        
        innerSettingsWithDefaults.string.setValue("x");
        innerSettingsWithDefaults.number.setValue(BigDecimal.TEN);
        innerSettingsWithDefaults.strings.setValues(new HashSet<>(Arrays.asList("y", "b")));
        
        final OuterSettings outerSettingsToSetDefaults = new OuterSettings();
        final TestSettings innerSettingsToSetDefaults = outerSettingsToSetDefaults.nested;
        innerSettingsToSetDefaults.string.setValue(someString);
        innerSettingsToSetDefaults.number.setValue(someNumber);
        innerSettingsToSetDefaults.strings.setValues(someStrings);
        
        SettingsUtil.copyDefaults(outerSettingsWithDefaults, outerSettingsToSetDefaults);
        
        assertEquals(someString, innerSettingsToSetDefaults.string.getValue());
        assertEquals(someNumber, innerSettingsToSetDefaults.number.getValue());
        assertEquals(someStrings, new HashSet<String>((Collection<String>)innerSettingsToSetDefaults.strings.getValues()));
        
        assertEquals(defaultString, innerSettingsToSetDefaults.string.getDefaultValue());
        assertEquals(defaultNumber, innerSettingsToSetDefaults.number.getDefaultValue());
        assertEquals(defaultStrings, new HashSet<String>((Collection<String>)innerSettingsToSetDefaults.strings.getDefaultValues()));
    }
    
    @Test
    public void testDefaults2() {
        final OuterSettings outerSettingsWithDefaults = new OuterSettings();
        final TestSettings innerSettingsWithDefaults = outerSettingsWithDefaults.nested;
        
        innerSettingsWithDefaults.string.setDefaultValue("x");
        innerSettingsWithDefaults.number.setDefaultValue(BigDecimal.TEN);
        innerSettingsWithDefaults.strings.setDefaultValues(new HashSet<>(Arrays.asList("y", "b")));
        
        innerSettingsWithDefaults.string.setValue(defaultString);
        innerSettingsWithDefaults.number.setValue(defaultNumber);
        innerSettingsWithDefaults.strings.setValues(defaultStrings);
        
        final OuterSettings outerSettingsToSetDefaults = new OuterSettings();
        final TestSettings innerSettingsToSetDefaults = outerSettingsToSetDefaults.nested;
        innerSettingsToSetDefaults.string.setValue(someString);
        innerSettingsToSetDefaults.number.setValue(someNumber);
        innerSettingsToSetDefaults.strings.setValues(someStrings);
        
        SettingsUtil.copyDefaultsFromValues(outerSettingsWithDefaults, outerSettingsToSetDefaults);
        
        assertEquals(someString, innerSettingsToSetDefaults.string.getValue());
        assertEquals(someNumber, innerSettingsToSetDefaults.number.getValue());
        assertEquals(someStrings, new HashSet<String>((Collection<String>)innerSettingsToSetDefaults.strings.getValues()));
        
        assertEquals(defaultString, innerSettingsToSetDefaults.string.getDefaultValue());
        assertEquals(defaultNumber, innerSettingsToSetDefaults.number.getDefaultValue());
        assertEquals(defaultStrings, new HashSet<String>((Collection<String>)innerSettingsToSetDefaults.strings.getDefaultValues()));
    }

}
