package com.zipo.tests.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class SplashScreen extends BaseScreen {

    @AndroidFindBy(id = "com.zipo.interviewproject:id/splash_app_name")
    @iOSXCUITFindBy(accessibility = "splash_app_name")
    private WebElement appTitle;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/splash_progress")
    @iOSXCUITFindBy(accessibility = "splash_progress")
    private WebElement progressSpinner;

    public boolean isSplashDisplayed() {
        return isVisible(appTitle, Duration.ofSeconds(3))
                && isVisible(progressSpinner, Duration.ofSeconds(3));
    }

    public MainScreen waitUntilMainScreenLoads() {
        waitForInvisible(progressSpinner);
        return new MainScreen().waitUntilLoaded();
    }
}