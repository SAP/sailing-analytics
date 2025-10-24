package com.sap.sailing.selenium.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <p>Java representation of the test environment configuration as defined in the XML file. Since there can only be one
 *   configuration per test run, this class is implemented as a singleton.</p>
 * 
 * @author
 *   D049941
 */
public class TestEnvironmentConfiguration {
    /**
     * <p>Java representation of a web driver as defined in the configuration file.</p>
     * 
     * @author
     *   D049941
     */
    protected static class DriverDefinition {
        private String driver;
        private Map<String, Object> capabilities;
        
        /**
         * <p>Creates a new definition of a web driver with the given class and the desired capabilities.</p>
         * 
         * @param driver
         *   The classname of the web driver.
         * @param capabilities
         *   The capabilities of the web driver.
         */
        public DriverDefinition(String driver, Map<String, Object> capabilities) {
            this.driver = driver;
            this.capabilities = capabilities;
        }
        
        /**
         * <p>Returns the classname of the web driver.</p>
         * 
         * @return
         *   The classname of the web driver.
         */
        public String getDriver() {
            return this.driver;
        }
        
        /**
         * <p>Returns the desired capabilities of the web driver.</p>
         * 
         * @return
         *   The desired capabilities of the web driver.
         */
        public Map<String, Object> getCapabilities() {
            return this.capabilities;
        }
    }
    
    /**
     * <p>The key of the system property for the specification of the configuration file.</p>
     */
    public static final String TEST_ENVIRONMENT_CONFIGURATION = "selenium.test.environment.configuration"; //$NON-NLS-1$
    
    private static final String TEST_ENVIRONMENT_SCHEMA = "test-environment-configuration.xsd"; //$NON-NLS-1$
    
    private static final String NAMESPACE_URI = "http://www.sapsailing.com/test-environment"; //$NON-NLS-1$
    
    private static final String CONTEXT_ROOT = "context-root"; //$NON-NLS-1$
    
    private static final String SCREENSHOTS_FOLDER = "screenshots-folder"; //$NON-NLS-1$
    
    private static final String SYSTEM_PROPERTIES = "system-properties"; //$NON-NLS-1$
    
    private static final String SYSTEM_PROPERTY = "system-property"; //$NON-NLS-1$
    
    private static final String DRIVER_DEFINITION = "driver-definition"; //$NON-NLS-1$
    
    private static final String DRIVER_CLASS = "class"; //$NON-NLS-1$
    
    private static final String DRIVER_CAPABILITIES = "capabilities"; //$NON-NLS-1$
    
    private static final String DRIVER_CAPABILITY = "capability"; //$NON-NLS-1$
    
    private static final String PARAMETER_NAME = "name"; //$NON-NLS-1$
    
    private static final String PARAMETER_VALUE = "value"; //$NON-NLS-1$
    
    private static TestEnvironmentConfiguration instance;
    
    /**
     * <p>Returns the Java representation of the test environment configuration as defined in the XML file.</p>
     * 
     * @return
     *   The Java representation of the test environment configuration as defined in the XML file.
     */
    public static TestEnvironmentConfiguration getInstance() {
        if (TestEnvironmentConfiguration.instance == null) {
            try {
                TestEnvironmentConfiguration.instance = createTestEnvironmentConfiguration();
            } catch(Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return TestEnvironmentConfiguration.instance;
    }
    
    private static synchronized TestEnvironmentConfiguration createTestEnvironmentConfiguration() throws
            ParserConfigurationException, SAXException, IOException {
        if (TestEnvironmentConfiguration.instance != null) {
            return TestEnvironmentConfiguration.instance;
        }
        Document document = readTestConfiguration();
        Element testEnvironmentNode = document.getDocumentElement();
        testEnvironmentNode.normalize();
        String contextRoot = XMLHelper.getContentTextNS(testEnvironmentNode, CONTEXT_ROOT, NAMESPACE_URI);
        String screenshotsFolder = XMLHelper.getContentTextNS(testEnvironmentNode, SCREENSHOTS_FOLDER, NAMESPACE_URI);
        Map<String, String> systemProperties = createSystemProperties(testEnvironmentNode);
        List<DriverDefinition> driverDefinitions = createDriverDefinitions(testEnvironmentNode);
        return new TestEnvironmentConfiguration(contextRoot, screenshotsFolder, systemProperties, driverDefinitions);
    }
    
    private static synchronized Document readTestConfiguration() throws ParserConfigurationException,
            SAXException, IOException {
        String path = System.getProperty(TEST_ENVIRONMENT_CONFIGURATION, "local-test-environment.xml");
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(TEST_ENVIRONMENT_SCHEMA));
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        builderFactory.setSchema(schema);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        InputSource source = new InputSource(new FileInputStream(path));
        return builder.parse(source);
    }
    
    private static Map<String, String> createSystemProperties(Element testEnvironmentNode) {
        Element systemPropertiesNode = XMLHelper.getElementNS(testEnvironmentNode, SYSTEM_PROPERTIES, NAMESPACE_URI);
        if (systemPropertiesNode == null) {
            return Collections.emptyMap();
        }
        Map<String, String> properties = new HashMap<>();
        List<Element> systemPropertyNodes = XMLHelper.getElementsNS(systemPropertiesNode, SYSTEM_PROPERTY,
                NAMESPACE_URI);
        for(Element systemPropertyNode : systemPropertyNodes) {
            String propertyName = XMLHelper.getContentTextNS(systemPropertyNode, PARAMETER_NAME, NAMESPACE_URI);
            String propertyValue = XMLHelper.getContentTextNS(systemPropertyNode, PARAMETER_VALUE, NAMESPACE_URI);
            properties.put(propertyName, propertyValue);
        }
        return properties;
    }
    
    private static List<DriverDefinition> createDriverDefinitions(Element testEnvironmentNode) {
        List<DriverDefinition> definitions = new LinkedList<>();
        List<Element> driverDefinitionNodes = XMLHelper.getElementsNS(testEnvironmentNode, DRIVER_DEFINITION, NAMESPACE_URI);
        for (Element driverDefinitionNode : driverDefinitionNodes) {
            definitions.add(createDriverDefinition(driverDefinitionNode));
        }
        return definitions;
    }
    
    private static DriverDefinition createDriverDefinition(Element driverDefinitionNode) {
        String driverClass = XMLHelper.getAttributeValueNS(driverDefinitionNode, DRIVER_CLASS, null);;
        Element capabilitiesNode = XMLHelper.getElementNS(driverDefinitionNode, DRIVER_CAPABILITIES, NAMESPACE_URI);
        
        if (capabilitiesNode == null) {
            return new DriverDefinition(driverClass, Collections.<String, Object>emptyMap());
        }
        
        Map<String, Object> capabilities = new HashMap<>();
        
        for(Element capabilityNode : XMLHelper.getElementsNS(capabilitiesNode, DRIVER_CAPABILITY, NAMESPACE_URI)) {
            String capabilityName = XMLHelper.getContentTextNS(capabilityNode, PARAMETER_NAME, NAMESPACE_URI);
            String capabilityValue = XMLHelper.getContentTextNS(capabilityNode, PARAMETER_VALUE, NAMESPACE_URI);
            if(capabilityValue != null && !capabilityValue.isEmpty()) {
                if(capabilityValue.equalsIgnoreCase("true") || capabilityValue.equalsIgnoreCase("false")) {
                    capabilities.put(capabilityName, Boolean.valueOf(capabilityValue));
                } else {
                    capabilities.put(capabilityName, capabilityValue);
                }
            }
        }
        
        return new DriverDefinition(driverClass, capabilities);
    }
    
    private String root;
    
    private String screenshotsFolder;
    
    private Map<String, String> properties;
    
    private List<DriverDefinition> definitions;
        
    private TestEnvironmentConfiguration(String root, String screenshots, Map<String, String> properties,
            List<DriverDefinition> definitions) {
        this.root = root;
        this.screenshotsFolder = screenshots;
        this.properties = properties;
        this.definitions = definitions;
    }
    
    /**
     * <p>Returns the context root (base URL) for the execution of the tests as defined in the configuration. The
     *   context root identifies the web application and usually consists of a protocol definition, the host and a
     *   path.</p>
     * 
     * @return
     *   The context root for the execution of the tests.
     */
    public String getContextRoot() {
        return this.root;
    }
    
    /**
     * <p>Returns {@code true} if screenshots should be captured during the execution of tests and {@code false}
     *   otherwise. The capturing of screenshots can be disabled by not defining a screenshot folder in the
     *   configuration file.</p>
     * 
     * @return
     *   {@code true} if screenshots should be captured and {@code false} otherwise.
     */
    public boolean screenshotsEnabled() {
        return (this.screenshotsFolder != null && !this.screenshotsFolder.isEmpty());
    }
    
    /**
     * <p>Returns the screenshot folder as defined in the configuration or {@code null} if it is not defined. The
     *   screenshot folder is used for storing screenshots captured during the execution of a test.</p>
     * 
     * @return
     *   The screenshot folder as defined in the configuration or {@code null} if it is not defined.
     */
    public String getScreenshotsFolder() {
        return this.screenshotsFolder;
    }
    
    /**
     * <p>Returns the system properties as defined in the configuration.</p>
     * 
     * @return
     *   The system properties as defined in the configuration.
     */
    public Map<String, String> getSystemProperties() {
        return this.properties;
    }
    
    /**
     * <p>Returns the list of web drivers as defined in the configuration.</p>
     * 
     * @return
     *   The list of web drivers as defined in the configuration.
     */
    public List<DriverDefinition> getDriverDefinitions() {
        return this.definitions;
    }
}
