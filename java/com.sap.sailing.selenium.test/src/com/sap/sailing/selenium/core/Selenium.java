package com.sap.sailing.selenium.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import java.net.URL;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.runner.Description;

import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.ParentRunner;

import org.junit.runners.model.InitializationError;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import org.openqa.selenium.remote.DesiredCapabilities;

import com.sap.sailing.selenium.core.Selenium.SeleniumJUnit4ClassRunner;
import com.sap.sailing.selenium.core.TestEnvironmentConfiguration.DriverDefinition;

import com.sap.sailing.selenium.core.impl.TestEnvironmentImpl;

/**
 * <p>The Selenium runner is a JUnit runner that runs a test case, using the WebDriver API of Selenium, as a suite of
 *   tests. The runner takes a configuration file and runs the test for each defined browser.</p>
 * 
 * @author
 *   D049941
 */
public class Selenium extends ParentRunner<SeleniumJUnit4ClassRunner> {
    /**
     * <p>A test runner that will run the tests for a specific browser instance using a Selenium web driver. The web
     *   driver is provided to the tests by injecting it to annotated fields.</p>
     * 
     * @author
     *   D049941
     */
    protected class SeleniumJUnit4ClassRunner extends BlockJUnit4ClassRunner {
        private String root;
        private URL screenshots;
        private DriverDefinition definition;

        private TestEnvironmentImpl environment;

        /**
         * <p>Creates a new runner to run the test cases, encapsulated within the given class, for a specific browser
         *   instance using a Selenium web driver.</p>
         * 
         * @param klass
         *   The class containing the tests.
         * @param root
         *   The context root (base URL) against the tests should be executed.
         * @param screenshots
         *   The folder where screenshots should be stored.
         * @param definition
         *   Definition of the web driver to use.
         * @throws InitializationError
         *   
         */
        public SeleniumJUnit4ClassRunner(Class<?> klass, String root, URL screenshots, DriverDefinition definition)
                throws InitializationError {
            super(klass);

            this.root = root;
            this.screenshots = screenshots;
            this.definition = definition;
        }

        /**
         * <p>Creates the test object and injects the test environment to use into fields that were annotated with
         *   {@link Managed}.</p>
         * 
         * @return
         *   The test object.
         * @throws Exception
         *   If there was an error creating the test object.
         */
        @Override
        protected Object createTest() throws Exception {
            final Object test = super.createTest();
            final TestClass testClass = getTestClass();

            List<FrameworkField> fields = testClass.getAnnotatedFields(Managed.class);

            for (final FrameworkField field : fields) {
                setValueForField(test, field.getField(), this.environment);
            }

            return test;
        }

        @Override
        public void run(final RunNotifier notifier) {
            try {
                this.environment = createTestEnvironment();

                try {
                    super.run(notifier);
                } finally {
                    this.environment.close();
                }
            } catch (Exception exception) {
                notifier.fireTestFailure(new Failure(getDescription(), exception));
            }
        }

        @Override
        protected void collectInitializationErrors(java.util.List<java.lang.Throwable> errors) {
            super.collectInitializationErrors(errors);

            // QUESTION: Do we need addional checks here?
        }

        @Override
        public Description getDescription() {
            String name = String.format("%s(%s)", createDriverDescription(), getTestClass().getName()); //$NON-NLS-1$
            Description description = Description.createSuiteDescription(name);

            for (Description child : super.getDescription().getChildren())
                description.addChild(child);

            return description;
        }

        @Override
        protected Description describeChild(FrameworkMethod method) {
            return Description.createTestDescription(getTestClass().getJavaClass(), testName(method) + ':'
                    + createDriverDescription(), method.getAnnotations());
        }

        private String createDriverDescription() {
            return descripeDriver() + descripeCapabilities();
        }

        private String descripeDriver() {
            String driver = this.definition.getDriver();
            int index = driver.lastIndexOf('.');

            return (index != -1 ? driver.substring(index + 1) : driver);
        }

        private String descripeCapabilities() {
            Map<String, String> capabilities = this.definition.getCapabilities();

            if (capabilities.isEmpty())
                return ""; //$NON-NLS-1$

            StringBuilder builder = new StringBuilder(capabilities.size() * 10);

            builder.append('[');

            for (Entry<String, String> capability : capabilities.entrySet()) {
                builder.append(capability.getKey());
                builder.append('=');
                builder.append(capability.getValue());
                builder.append(',');
            }

            builder.setCharAt(builder.length() - 1, ']');

            return builder.toString();
        }

        private TestEnvironmentImpl createTestEnvironment() throws Exception {
            try {
                String driverClassname = this.definition.getDriver();
                Map<String, String> capabilityDefenitions = this.definition.getCapabilities();

                @SuppressWarnings("unchecked")
                Class<WebDriver> clazz = (Class<WebDriver>) Class.forName(driverClassname);
                Capabilities capabilities = new DesiredCapabilities(capabilityDefenitions);

                Constructor<WebDriver> constructor = clazz.getConstructor(Capabilities.class);
                WebDriver driver = constructor.newInstance(capabilities);
                
                URL screenshots = resolveScreenshotFolder();
                
                return new TestEnvironmentImpl(driver, this.root, screenshots);
            } catch (Exception exception) {
                throw exception;
            }
        }

        // TODO: Implement in a way that we construct a unique folder which is a subfolder of the defined one!
        private URL resolveScreenshotFolder() {
            return this.screenshots;
        }
        
        private void setValueForField(Object instance, Field field, Object value) throws IllegalArgumentException,
                IllegalAccessException {
            field.setAccessible(true);
            field.set(instance, value);
        }
    }
    
    private final List<SeleniumJUnit4ClassRunner> children;

    /**
     * <p>Creates a runner to run the test cases encapsulated within the given class.</p>
     * 
     * @param klass
     *   The class that encapsulates the test cases.
     * @throws InitializationError
     *   If there was an error during the initialization of the test runner.
     */
    public Selenium(Class<?> klass) throws InitializationError {
        super(klass);

        this.children = new LinkedList<>();

        initializeRunners();
    }

    @Override
    protected List<SeleniumJUnit4ClassRunner> getChildren() {
        return this.children;
    }

    @Override
    protected Description describeChild(SeleniumJUnit4ClassRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(SeleniumJUnit4ClassRunner child, RunNotifier notifier) {
        child.run(notifier);
    }

    @Override
    protected void collectInitializationErrors(java.util.List<java.lang.Throwable> errors) {
        super.collectInitializationErrors(errors);

        // QUESTION: Do we need addional checks here?
    }

    /**
     * <p>Builds the test runners for each browser for test cases. A test runner is created for each web driver
     *   specified in the used configuration.</p>
     */
    private void initializeRunners() throws InitializationError {
        try {
            TestEnvironmentConfiguration configuration = TestEnvironmentConfiguration.getInstance();
            String contextRoot = configuration.getContextRoot();
            String screenshotsFolder = configuration.getScreenshotsFolder();
            Map<String, String> systemProperties = configuration.getSystemProperties();

            for (Entry<String, String> property : systemProperties.entrySet()) {
                System.setProperty(property.getKey(), property.getValue());
            }

            TestClass test = getTestClass();
            URL screenshotsStorage = (configuration.screenshotsEnabled() ? new URL(screenshotsFolder) : null);
            
            for (DriverDefinition defenition : configuration.getDriverDefinitions()) {
                SeleniumJUnit4ClassRunner child = new SeleniumJUnit4ClassRunner(test.getJavaClass(), contextRoot,
                        screenshotsStorage, defenition);
                
                this.children.add(child);
            }
        } catch (Exception exception) {
            throw new InitializationError(exception);
        }
    }
}
