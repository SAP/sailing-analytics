package com.sap.sailing.selenium.core;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.sap.sailing.selenium.core.TestEnvironmentConfiguration.DriverDefinition;
import com.sap.sailing.selenium.core.impl.TestEnvironmentImpl;

public class TestEnvironmentFactory {
    public static TestEnvironmentImpl create(DriverDefinition def) throws Exception {
        String driverClassname = def.getDriver();
        Map<String, Object> capabilities = def.getCapabilities();

        @SuppressWarnings("unchecked")
        Class<WebDriver> clazz = (Class<WebDriver>) Class.forName(driverClassname);
        Constructor<WebDriver> constructor = clazz.getConstructor(Capabilities.class);

        WebDriver driver = constructor.newInstance(new DesiredCapabilities(capabilities));
        return new TestEnvironmentImpl(() -> driver, TestEnvironmentConfiguration.getInstance().getContextRoot(),
                new File(TestEnvironmentConfiguration.getInstance().getScreenshotsFolder()));
    }
}
