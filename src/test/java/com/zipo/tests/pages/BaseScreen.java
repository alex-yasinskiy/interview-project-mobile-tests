package com.zipo.tests.pages;

import com.zipo.tests.config.FrameworkConfig;
import com.zipo.tests.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BaseScreen {

    protected final AppiumDriver driver;
    protected final WebDriverWait wait;

    protected BaseScreen() {
        this.driver = DriverManager.getDriver();
        this.wait = new WebDriverWait(driver, FrameworkConfig.getInstance().explicitWait());
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ZERO), this);
    }

    protected WebElement waitForVisible(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    protected void waitForInvisible(WebElement element) {
        wait.until(ExpectedConditions.invisibilityOf(element));
    }

    protected void tap(WebElement element) {
        wait.until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    protected boolean isVisible(WebElement element, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .ignoring(NoSuchElementException.class, StaleElementReferenceException.class)
                    .until(d -> element.isDisplayed());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}