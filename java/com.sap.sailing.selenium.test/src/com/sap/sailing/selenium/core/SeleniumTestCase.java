package com.sap.sailing.selenium.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * Use this annotation on test methods in subclasses of {@link AbstractSeleniumTest} to indicate that they are Selenium
 * test cases. This will set the {@link SeleniumTestInvocationProvider} as the test invocation provider for the test
 * method, ensuring it will get executed for each web driver definition, with the {@link TestEnvironment} being injected
 * into the test by the additional extension {@link TestEnvironmentInjector} provided by the test contexts produced by
 * the invocation provider.
 * 
 * @author Axel Uhl (d043530)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(SeleniumTestInvocationProvider.class)
public @interface SeleniumTestCase {}
