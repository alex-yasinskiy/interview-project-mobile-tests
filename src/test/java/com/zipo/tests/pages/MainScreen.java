package com.zipo.tests.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

public class MainScreen extends BaseScreen {

    @AndroidFindBy(id = "com.zipo.interviewproject:id/root")
    @iOSXCUITFindBy(accessibility = "main_root")
    private WebElement rootContainer;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/list_rv")
    @iOSXCUITFindBy(accessibility = "earth_list")
    private WebElement itemsRecyclerView;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/my_adapter_item_tv")
    @iOSXCUITFindBy(accessibility = "earth_item_title")
    private List<WebElement> itemTitles;

    @AndroidFindBy(id = "com.zipo.interviewproject:id/my_adapter_item_btn")
    @iOSXCUITFindBy(accessibility = "earth_item_download_btn")
    private List<WebElement> downloadButtons;

    public MainScreen waitUntilLoaded() {
        waitForVisible(rootContainer);
        waitForVisible(itemsRecyclerView);
        wait.until(d -> !downloadButtons.isEmpty() && downloadButtons.getFirst().isDisplayed());
        return this;
    }

    public boolean isDisplayed() {
        return isVisible(rootContainer, Duration.ofSeconds(3))
                && isVisible(itemsRecyclerView, Duration.ofSeconds(3));
    }

    public int getVisibleItemsCount() {
        return downloadButtons.size();
    }

    public List<String> getVisibleItemTitles() {
        return itemTitles.stream()
                .map(WebElement::getText)
                .toList();
    }

    public PaywallModal tapDownloadAt(int index) {
        if (index < 0 || index >= downloadButtons.size()) {
            throw new IndexOutOfBoundsException(
                    "Requested item index " + index + ", but only " + downloadButtons.size() + " items are visible."
            );
        }
        tap(downloadButtons.get(index));
        return new PaywallModal().waitUntilLoaded();
    }
}