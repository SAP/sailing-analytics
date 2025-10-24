package com.sap.sailing.selenium.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import com.sap.sailing.selenium.core.TestEnvironmentConfiguration.DriverDefinition;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * Used to extend the {@link SeleniumTestCase} annotation which in turn is used to mark the test methods of all Selenium
 * tests declared in subclasses of {@link AbstractSeleniumTest}. This provider produces test invocation contexts, one
 * for each {@link TestEnvironmentConfiguration#getDriverDefinitions() driver definition} found in the test environment
 * configuration. These contexts provide a test instance-specific extension of type {@link SeleniumTestEnvironmentInjector}
 * which is in particular a {@link TestInstancePostProcessor} that creates and injects a {@link TestEnvironment} created
 * for the driver definition known by the parameter resolver.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SeleniumTestInvocationProvider implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        TestEnvironmentConfiguration config = TestEnvironmentConfiguration.getInstance();
        for (Entry<String, String> e : config.getSystemProperties().entrySet()) {
            System.setProperty(e.getKey(), e.getValue());
        }
        return config.getDriverDefinitions().stream().map(driverDef -> {
            return new SeleniumTestContext(driverDef);
        });
    }

    static class SeleniumTestContext implements TestTemplateInvocationContext {
        private final DriverDefinition driverDefinition;

        SeleniumTestContext(DriverDefinition driverDefinition) {
            this.driverDefinition = driverDefinition;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            return "Selenium Test - " + driverDefinition.getDriver();
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return Arrays.asList(new SeleniumTestEnvironmentInjector(driverDefinition));
        }
    }
}
