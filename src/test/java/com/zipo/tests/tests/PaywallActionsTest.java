package com.zipo.tests.tests;

import com.zipo.tests.extension.MobileTestExtension;
import com.zipo.tests.pages.MainScreen;
import com.zipo.tests.pages.PaywallModal;
import com.zipo.tests.pages.SplashScreen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MobileTestExtension.class)
class PaywallActionsTest {

    enum PaywallCta {
        SUBSCRIBE(PaywallModal::tapSubscribe),
        PURCHASE(PaywallModal::tapPurchase),
        RESTORE_PURCHASES(PaywallModal::tapRestorePurchases);

        private final Function<PaywallModal, MainScreen> action;

        PaywallCta(Function<PaywallModal, MainScreen> action) {
            this.action = action;
        }
    }

    @ParameterizedTest(name = "Tapping {0} dismisses paywall and returns to Main screen")
    @EnumSource(PaywallCta.class)
    @DisplayName("Paywall CTA buttons dismiss modal without crashing and return to Main screen")
    void shouldDismissPaywallAndReturnToMainScreenOnActionTap(PaywallCta cta) {
        MainScreen mainScreen = new SplashScreen().waitUntilMainScreenLoads();
        PaywallModal paywallModal = mainScreen.tapDownloadAt(0);

        MainScreen returnedMainScreen = cta.action.apply(paywallModal);

        assertAll("Verify post-action state for " + cta.name(),
                () -> assertFalse(paywallModal.isDisplayed(), "Paywall modal should close after CTA tap"),
                () -> assertTrue(returnedMainScreen.isDisplayed(), "User should return to Main screen"),
                () -> assertTrue(returnedMainScreen.getVisibleItemsCount() >= 3,
                        "Earth list should remain visible and interactive")
        );
    }
}