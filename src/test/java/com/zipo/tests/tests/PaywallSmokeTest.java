package com.zipo.tests.tests;

import com.zipo.tests.extension.MobileTestExtension;
import com.zipo.tests.pages.MainScreen;
import com.zipo.tests.pages.PaywallModal;
import com.zipo.tests.pages.SplashScreen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MobileTestExtension.class)
class PaywallSmokeTest {

    @Test
    @DisplayName("Splash transitions to Main list, opens Premium paywall on Download tap, and closes on Not Now")
    void shouldLoadMainListOpenPaywallAndDismissWithNotNow() {
        MainScreen mainScreen = new SplashScreen().waitUntilMainScreenLoads();

        List<String> visibleTitles = mainScreen.getVisibleItemTitles();
        assertTrue(visibleTitles.containsAll(List.of("Earth 1", "Earth 2", "Earth 3")),
                "Expected initial Earth items to be displayed, actual: " + visibleTitles);

        PaywallModal paywallModal = mainScreen.tapDownloadAt(0);

        assertAll("Verify Premium paywall modal content and action buttons",
                () -> assertTrue(paywallModal.isDisplayed(), "Paywall modal should be visible"),
                () -> assertEquals("Premium", paywallModal.getTitleText()),
                () -> assertFalse(paywallModal.getMessageText().isBlank(), "Paywall notice should not be empty"),
                () -> assertTrue(paywallModal.areAllActionsVisible(),
                        "SUBSCRIBE, PURCHASE, RESTORE PURCHASES, and NOT NOW buttons should be visible")
        );

        MainScreen returnedMainScreen = paywallModal.tapNotNow();

        assertAll("Verify return to Main screen after dismissing paywall",
                () -> assertFalse(paywallModal.isDisplayed(), "Paywall modal should be closed"),
                () -> assertTrue(returnedMainScreen.isDisplayed(), "Main screen should be active"),
                () -> assertTrue(returnedMainScreen.getVisibleItemsCount() >= 3, "Earth list should remain visible")
        );
    }
}