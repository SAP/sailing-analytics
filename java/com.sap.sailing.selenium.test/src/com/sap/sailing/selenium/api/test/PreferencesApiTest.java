package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.PreferencesApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class PreferencesApiTest extends AbstractSeleniumTest {

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
    }

    @SeleniumTestCase
    public void createAndGetPreferencesTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), ApiContext.SECURITY_CONTEXT);

        Map<String, String> prefObjectAttr = new HashMap<String, String>();
        prefObjectAttr.put("key1", "value1");
        prefObjectAttr.put("key2", "value2");

        PreferencesApi preferencesApi = new PreferencesApi();
        preferencesApi.createPreference(ctx, "pref1", prefObjectAttr);

        JSONObject foundPreference = preferencesApi.getPreference(ctx, "pref1");
        assertEquals("value1", foundPreference.get("key1"), "restored preference is different");
        assertEquals("value2", foundPreference.get("key2"), "restored preference is different");
    }

    @SeleniumTestCase
    public void createAndDeletePreferencesTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), ApiContext.SECURITY_CONTEXT);

        Map<String, String> prefObjectAttr = new HashMap<String, String>();
        prefObjectAttr.put("key1", "value1");
        prefObjectAttr.put("key2", "value2");

        PreferencesApi preferencesApi = new PreferencesApi();
        preferencesApi.createPreference(ctx, "pref2", prefObjectAttr);
        preferencesApi.deletePreference(ctx, "pref2");

        JSONObject foundPreference = preferencesApi.getPreference(ctx, "pref2");
        assertNull(foundPreference, "should not find a deleted preference");
    }

}
