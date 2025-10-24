package com.sap.sailing.selenium.core;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import com.sap.sailing.selenium.core.TestEnvironmentConfiguration.DriverDefinition;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * Creates the {@link TestEnvironment} based on the {@link DriverDefinition} passed to its constructor, and if the test
 * being executed is an instance of {@link AbstractSeleniumTest}, sets the environment on the test instance using its
 * {@link AbstractSeleniumTest#setEnvironment(TestEnvironment)} method.
 * 
 * @author Axel Uhl (d043530)
 *
 */
class SeleniumTestEnvironmentInjector implements TestInstancePostProcessor {
    private final DriverDefinition driverDef;
    private TestEnvironment environment;

    SeleniumTestEnvironmentInjector(DriverDefinition driverDef) {
        this.driverDef = driverDef;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        environment = TestEnvironmentFactory.create(driverDef);
        if (testInstance instanceof AbstractSeleniumTest) {
            ((AbstractSeleniumTest) testInstance).setEnvironment(environment);
        }
    }
}
