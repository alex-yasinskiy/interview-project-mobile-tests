# Test Plan — Interview Project (`com.zipo.interviewproject`)

## 1. Application Overview
**Interview Project** (v1.0) is a 3-screen Android application gated by a monetization paywall:
1. **Splash Screen (`SplashActivity`)**: Launches on cold start, displays the `"Interview Project"` title and an indeterminate loading spinner, then transitions automatically to the Main Screen.
2. **Main Screen**: Displays a scrollable list of items (`Earth 1`, `Earth 2`, ..., `Earth 11+`), each paired with a `DOWNLOAD` button.
3. **Paywall Modal (`Premium`)**: Opens as an overlay dialog when tapping `DOWNLOAD` on any list item. Contains `SUBSCRIBE`, `PURCHASE`, `RESTORE PURCHASES`, and `NOT NOW` actions. In test/unsigned builds (or without an active Google Play Billing session), billing is disabled and action buttons display a short feedback message without starting a download.

---

## 2. What Are the Most Important Things This App Must Get Right?
1. **Payment & Subscription Gating (Core Business Value)**: The payment and subscription flows are the most critical journeys in the application. Content must remain strictly locked behind the `Premium` paywall without an active entitlement, and must become immediately accessible right after a successful purchase, subscription, or purchase restoration.
2. **Clean Launch & Splash Transition**: The app must launch reliably without crashing or getting stuck on the Splash screen, transitioning smoothly to the Main Screen within an acceptable timeout.
3. **Core Conversion Funnel (`Main` → `Paywall`)**: Every item's `DOWNLOAD` button on the Main Screen must reliably trigger the `Premium` paywall modal, including items further down the scrollable list (e.g., `Earth 10+`).
4. **Resilient Paywall Behavior (Graceful Degradation)**: Tapping `SUBSCRIBE`, `PURCHASE`, or `RESTORE PURCHASES` when Google Play Billing is unavailable or disabled must handle the state gracefully—displaying the expected short message without throwing unhandled exceptions, freezing (ANR), or bypassing the paywall to trigger an unauthorized download.
5. **Reversible Navigation & Modal Dismissal**: Tapping `NOT NOW` (or system `Back`) must cleanly dismiss the `Premium` modal and restore full interactivity to the Main Screen so the user can browse or re-open the paywall on subsequent taps.
6. **Runtime Stability**: Zero crashes (`FATAL EXCEPTION`) and zero Application Not Responding (`ANR`) events across lifecycle transitions (background/foreground, configuration changes, repeated taps).

---

## 3. Risk Analysis: What Could Break and What Is the Impact?

| Risk / Failure Mode | Potential Cause | User & Business Impact | Severity |
| :--- | :--- | :--- | :--- |
| **App hangs indefinitely on Splash Screen** | Unhandled initialization error, blocking main-thread operation, or missing network timeout | **Critical** — Complete blocker; user cannot reach core functionality | Critical |
| **Crash (`FATAL EXCEPTION`) on `DOWNLOAD` or Paywall CTA tap** | Null/uninitialized Google Play Billing client or missing Play Store services on device/CI | **Critical** — App terminates during primary monetization flow | Critical |
| **Paywall bypass (download starts without entitlement)** | Incorrect state flag after dismissing modal or tapping disabled billing buttons | **High** — Revenue leakage / broken paywall gating | High |
| **Paywall modal cannot be dismissed or freezes UI (ANR)** | Broken `NOT NOW` click listener or leaked dialog window on rapid double-tap | **High** — User is trapped on the modal and forced to force-close the app | High |
| **Recycled list items (`Earth 10+`) lose click handlers after scrolling** | Improper `RecyclerView` view-holder binding on scroll | **Medium** — Users cannot interact with items below the fold | Medium |
| **UI misalignment / clipping on different screen densities** | Fixed margins or unweighted horizontal rows (e.g., `DOWNLOAD` button shifting right on two-digit items like `Earth 10`) | **Low** — Visual polish degradation; potential button clipping on narrow screens | Low |

---

## 4. Automation vs. Manual / Exploratory Testing Strategy

### Automated Scope (Appium + JUnit 5)
We automate deterministic, high-priority functional paths and regression checks that run reliably in headless CI:
* **App Launch & Splash Transition**: Verify cold start lands on the Main Screen within an explicit wait threshold and the list of `Earth` items renders.
* **Primary Paywall Flow**: Verify tapping `DOWNLOAD` on a visible item (`Earth 1`) opens the `Premium` modal with all expected controls (`SUBSCRIBE`, `PURCHASE`, `RESTORE PURCHASES`, `NOT NOW`).
* **Scroll & Below-the-Fold Interaction**: Scroll down the list (`Earth 10` / `Earth 11`) and verify `DOWNLOAD` still opens the `Premium` modal.
* **Paywall Actions & Feedback**: Tap `SUBSCRIBE`, `PURCHASE`, and `RESTORE PURCHASES`, verify the expected short notification/toast message appears, the paywall remains intact without unlocking content prematurely, and no crash/ANR occurs.
* **Modal Dismissal & Re-entry**: Tap `NOT NOW` (and test system `Back`), verify return to the Main Screen, and confirm tapping `DOWNLOAD` again re-opens the modal cleanly.
* **Process Health Checks**: Automated logcat assertions after every test ensuring zero `FATAL EXCEPTION` or `ANR in` entries.

### Manual & Exploratory Scope
We keep exploratory, visual, and real-billing-dependent checks manual (or out of CI scope):
* **Real Google Play Billing End-to-End Flow**: Testing actual SKU price localization, subscription trials, payment sheet rendering, purchase restoration, and immediate content unlock after payment using a signed-in Google Play License Tester account (not available on unsigned CI emulators).
* **Visual & Layout Inspection Across Devices**: Checking subtle alignment issues (such as button horizontal alignment shifts between single-digit `Earth 1` and double-digit `Earth 10`), font scaling, and dark/light system themes.
* **Ad-Hoc Interruptions & Stress Exploration**: Rapid multi-finger tapping, mid-transition screen rotation, airplane mode toggling mid-splash, and incoming call/notification interruptions.

---

## 5. Exit Criteria: How Will We Know When We Have Enough Coverage?
We consider test coverage sufficient for this release when:
1. **100% of Critical & High-risk user journeys** (`Cold Start` → `Main List` → `Scroll` → `Paywall Open` → `CTA Feedback` → `Dismiss & Re-open`) are automated and passing reliably across consecutive CI runs without flakiness or fixed sleeps.
2. **Negative / Unauthenticated Billing State** is explicitly verified both functionally (UI feedback message and locked content) and at the OS log level (zero crashes or ANRs).
3. **Diagnostic Readiness**: Every test failure automatically captures a screenshot, page source/logcat dump, and crash/ANR check so any regression can be triaged without manual reproduction.
4. **Known UI/Functional Anomalies** discovered during exploratory testing are documented with reproducible steps, expected vs. actual behavior, and severity ratings.