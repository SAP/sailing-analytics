package com.sap.sailing.selenium.pages.authentication;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.common.AttributeHelper;

public class AuthenticationMenuPO extends PageArea {

    public AuthenticationMenuPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public boolean isOpen() {
        return AttributeHelper.isEnabled(getWebElement(), "data-open");
    }

    public boolean isLoggedIn() {
        return AttributeHelper.isEnabled(getWebElement(), "data-auth");
    }

    public boolean attemptLogin(final String username, final String password) {
        final AuthenticationViewPO authenticationView = showAuthenticationView();
        authenticationView.getSignInView().doLogin(username, password);
        waitForAjaxRequests();
        return isLoggedIn();
    }

    public boolean attemptLoginExpectingAlertContainingMessage(final String username, final String password,
            final String expectedAlertMessage) {
        final AuthenticationViewPO authenticationView = showAuthenticationView();
        authenticationView.getSignInView().doLogin(username, password);
        waitForAlertContainingMessageAndAccept(expectedAlertMessage);
        waitForAjaxRequests();
        return isLoggedIn();
    }

    private AuthenticationViewPO showAuthenticationView() {
        if (!this.isOpen()) {
            getWebElement().click();
        }
        waitUntil(this::isOpen);
        return getPO(AuthenticationViewPO::new, "authenticationView");
    }

}
