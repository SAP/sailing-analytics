package com.sap.sailing.selenium.pages.adminconsole;

import java.util.function.Function;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import com.sap.sailing.selenium.pages.PageObject;

public class ActionsHelper {
    private static final String ACTION_XPATH = ".//descendant::div[@name='%s']/img";
    
    public static final String EDIT_ACTION = "ACTION_EDIT";
    public static final String REMOVE_ACTION = "ACTION_REMOVE";
    public static final String EDIT_SCORES_ACTION = "ACTION_EDIT_SCORES";
    public static final String CONFIGURE_URL_ACTION = "ACTION_CONFIGURE_URL";
    public static final String UNLINK_RACE_ACTION = "ACTION_UNLINK";
    public static final String REFRESH_RACE_ACTION = "ACTION_REFRESH_RACE";
    public static final String SET_START_TIME_ACTION = "ACTION_SET_STARTTIME";

    public static final String UPDATE_ACTION = "UPDATE";
    public static final String UNLOCK_ACTION = "MANAGE_LOCK";
    public static final String DELETE_ACTION = "DELETE";
    public static final String CHANGE_OWNERSHIP_ACTION = "CHANGE_OWNERSHIP";
    
    public static WebElement findEditAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, EDIT_ACTION)));
    }
    
    public static WebElement findRemoveAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, REMOVE_ACTION)));
    }
    
    public static WebElement findEditScoresAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, EDIT_SCORES_ACTION)));
    }
    
    public static WebElement findConfigureUrlAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, CONFIGURE_URL_ACTION)));
    }
    
    public static WebElement findUnlinkRaceAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, UNLINK_RACE_ACTION)));
    }
    
    public static WebElement findRefreshAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, REFRESH_RACE_ACTION)));
    }
    
    public static WebElement findSetStartTimeAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, SET_START_TIME_ACTION)));
    }
    
    public static WebElement findUpdateAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, UPDATE_ACTION)));
    }
    
    public static WebElement findUnlockAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, UNLOCK_ACTION)));
    }

    public static WebElement findDeleteAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, DELETE_ACTION)));
    }

    public static WebElement findChangeOwnershipAction(SearchContext context) {
        return context.findElement(By.xpath(String.format(ACTION_XPATH, CHANGE_OWNERSHIP_ACTION)));
    }

    public static void acceptAlert(WebDriver driver) {
        Alert alert = getAlert(driver);
        alert.accept();
    }
    
    public static void dismissAlert(WebDriver driver) {
        Alert alert = getAlert(driver);
        alert.dismiss();
    }
    
    public static Alert getAlert(WebDriver driver) {
        FluentWait<WebDriver> wait = PageObject.createFluentWait(driver, NoAlertPresentException.class);
        
        Alert alert = wait.until(new Function<WebDriver, Alert>() {
            @Override
            public Alert apply(WebDriver driver) {
                TargetLocator locator = driver.switchTo();
                
                return locator.alert();
            }
        });
        
        return alert;
    }
}
