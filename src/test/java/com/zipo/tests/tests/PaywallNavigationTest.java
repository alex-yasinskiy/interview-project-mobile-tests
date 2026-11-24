package com.zipo.tests.tests;

import com.zipo.tests.extension.MobileTestExtension;
import com.zipo.tests.pages.MainScreen;
import com.zipo.tests.pages.PaywallModal;
import com.zipo.tests.pages.SplashScreen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MobileTestExtension.class)
class PaywallNavigationTest {

    @Test
    @DisplayName("Paywall can be reopened on subsequent Download tap after being dismissed with Not Now")
    void shouldReopenPaywallOnSubsequentDownloadTap() {
        MainScreen mainScreen = new SplashScreen().waitUntilMainScreenLoads();

        // First open and dismissal (Earth 1)
        PaywallModal firstPaywall = mainScreen.tapDownloadAt(0);
        assertTrue(firstPaywall.isDisplayed(), "Paywall should open on first Download tap");
        MainScreen afterFirstDismiss = firstPaywall.tapNotNow();
        assertFalse(firstPaywall.isDisplayed(), "Paywall should close after Not Now");

        // Second open and dismissal on a lower list item (Earth 10, index 9)
        int lowerItemIndex = Math.min(9, afterFirstDismiss.getVisibleItemsCount() - 1);
        PaywallModal secondPaywall = afterFirstDismiss.tapDownloadAt(lowerItemIndex);
        assertAll("Verify paywall reopens cleanly on subsequent Download tap",
                () -> assertTrue(secondPaywall.isDisplayed(), "Paywall should reopen on subsequent Download tap"),
                () -> assertEquals("Premium", secondPaywall.getTitleText()),
                () -> assertTrue(secondPaywall.areAllActionsVisible(), "All paywall controls should be rendered")
        );

        MainScreen afterSecondDismiss = secondPaywall.tapNotNow();
        assertTrue(afterSecondDismiss.isDisplayed(), "Main screen should remain interactive after second dismissal");
    }
}