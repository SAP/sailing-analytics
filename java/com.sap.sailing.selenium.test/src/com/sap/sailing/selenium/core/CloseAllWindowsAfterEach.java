package com.sap.sailing.selenium.core;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class CloseAllWindowsAfterEach implements TestWatcher, AfterEachCallback {
    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (context.getRequiredTestInstance() instanceof AbstractSeleniumTest) {
            AbstractSeleniumTest testInstance = (AbstractSeleniumTest) context.getRequiredTestInstance();
            testInstance.getEnvironment().getWindowManager().closeAllWindows();
        } else {
            throw new IllegalStateException("The test instance must be of type AbstractSeleniumTest to use the ScreenShotRule.");
        }
    }
}