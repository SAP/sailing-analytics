package com.sap.sailing.datamining.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.datamining.i18n.DataMiningResourceBundleManager;

public class TestI18N {

    private static final String SIMPLE_TEST_MESSAGE_KEY = "SimpleTestMessage";
    private static final String TEST_MESSAGE_WITH_PARAMETERS = "TestMessageWithParameters";
    
    private OpenResourceBundleManager bundleManager;
    
    @Before
    public void initializeBundleManager() {
        bundleManager = new OpenResourceBundleManager(Locale.ENGLISH);
    }

    @Test
    public void testGettingASimpleMessage() {
        assertThat(bundleManager.get("default", SIMPLE_TEST_MESSAGE_KEY), is("English"));
        assertThat(bundleManager.get("en", SIMPLE_TEST_MESSAGE_KEY), is("English"));
        assertThat(bundleManager.get("de", SIMPLE_TEST_MESSAGE_KEY), is("Deutsch"));
        
        assertThat(bundleManager.get(Locale.ENGLISH, SIMPLE_TEST_MESSAGE_KEY), is("English"));
        assertThat(bundleManager.get(Locale.GERMAN, SIMPLE_TEST_MESSAGE_KEY), is("Deutsch"));
    }
    
    @Test
    public void testGettingAMessageWithParameters() {
        assertThat(bundleManager.get("default", TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("English Param0 - Param1"));
        assertThat(bundleManager.get("en", TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("English Param0 - Param1"));
        assertThat(bundleManager.get("de", TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("Deutsch Param0 - Param1"));
        
        assertThat(bundleManager.get(Locale.ENGLISH, TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("English Param0 - Param1"));
        assertThat(bundleManager.get(Locale.GERMAN, TEST_MESSAGE_WITH_PARAMETERS, "Param0", "Param1"), is("Deutsch Param0 - Param1"));
    }
    
    private class OpenResourceBundleManager extends DataMiningResourceBundleManager {

        public OpenResourceBundleManager(Locale defaultLocale) {
            super(defaultLocale);
        }
        
        public String get(Locale locale, String message, String... parameters) {
            return super.get(locale, message, parameters);
        }
        
        public String get(Locale locale, String message) {
            return super.get(locale, message, new String[0]);
        }
        
        public String get(String localeName, String message, String... parameters) {
            return this.get(getLocaleFrom(localeName), message, parameters);
        }
        
        public String get(String localeName, String message) {
            return this.get(getLocaleFrom(localeName), message);
        }
        
    }

}
