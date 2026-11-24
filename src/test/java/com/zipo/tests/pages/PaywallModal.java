package com.zipo.tests.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class PaywallModal extends BaseScreen {

    @AndroidFindBy(id = "com.zipo.interviewproject:id/paywall_title")
    @iOSXCUITFindBy(accessibility = "paywall_title")
    private WebElement titleView;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/paywall_message")
    @iOSXCUITFindBy(accessibility = "paywall_message")
    private WebElement messageView;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/paywall_btn_subscribe")
    @iOSXCUITFindBy(accessibility = "paywall_btn_subscribe")
    private WebElement subscribeButton;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/paywall_btn_purchase")
    @iOSXCUITFindBy(accessibility = "paywall_btn_purchase")
    private WebElement purchaseButton;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/paywall_btn_restore")
    @iOSXCUITFindBy(accessibility = "paywall_btn_restore")
    private WebElement restoreButton;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/paywall_btn_not_now")
    @iOSXCUITFindBy(accessibility = "paywall_btn_not_now")
    private WebElement notNowButton;

    public PaywallModal waitUntilLoaded() {
        waitForVisible(titleView);
        waitForVisible(notNowButton);
        return this;
    }

    public boolean isDisplayed() {
        return isVisible(titleView, Duration.ofSeconds(3));
    }

    public String getTitleText() {
        return waitForVisible(titleView).getText();
    }

    public String getMessageText() {
        return waitForVisible(messageView).getText();
    }

    public boolean areAllActionsVisible() {
        Duration shortWait = Duration.ofSeconds(3);
        return isVisible(subscribeButton, shortWait)
                && isVisible(purchaseButton, shortWait)
                && isVisible(restoreButton, shortWait)
                && isVisible(notNowButton, shortWait);
    }

    public MainScreen tapSubscribe() {
        return tapAndClose(subscribeButton);
    }

    public MainScreen tapPurchase() {
        return tapAndClose(purchaseButton);
    }

    public MainScreen tapRestorePurchases() {
        return tapAndClose(restoreButton);
    }

    public MainScreen tapNotNow() {
        return tapAndClose(notNowButton);
    }

    private MainScreen tapAndClose(WebElement button) {
        tap(button);
        waitForInvisible(titleView);
        return new MainScreen().waitUntilLoaded();
    }
}