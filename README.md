# Mobile UI Test Automation Framework

Cross-platform mobile UI test automation framework built with **Java 21**, **JUnit 5**, **Appium (`java-client`)**, and **Maven** to validate the core user flows and paywall behavior of `com.zipo.interviewproject`.

---

## Tech Stack

* **Language & Build:** Java 21 (`maven.compiler.release=21`), Maven 3.9+
* **Test Runner & Assertions:** JUnit Jupiter `5.11.4` (`junit-jupiter`), Maven Surefire Plugin
* **Mobile Automation:** Appium 2.x, Appium Java Client (`AppiumDriver`, `UiAutomator2Options`, `XCUITestOptions`)
* **Failure Triage:** Custom non-blocking AI analyzer (`FailureAiAnalyzer`) built on JDK 21 `java.net.http.HttpClient` (zero external runtime dependencies)
* **CI/CD:** GitHub Actions (`ubuntu-latest` with KVM hardware acceleration and Android API 34 emulator)

---

## Automated Test Coverage

The suite contains **5 deterministic end-to-end tests** organized across 3 test classes:

| Test Class | Scenario | Key Assertions |
| :--- | :--- | :--- |
| **`PaywallSmokeTest`** | App launch → Main screen load → Open paywall → Dismiss via `NOT NOW` | Splash transitions cleanly; `Earth 1..11` list renders; `Premium` title and all 4 CTA controls are visible; modal closes cleanly without crash/ANR. |
| **`PaywallActionsTest`** | Tap `SUBSCRIBE` CTA on paywall | Paywall dismisses cleanly; app process stays foregrounded (`RUNNING`); `MainScreen` remains interactive. |
| **`PaywallActionsTest`** | Tap `PURCHASE` CTA on paywall | Paywall dismisses cleanly; no `FATAL EXCEPTION` or ANR in `logcat`; `MainScreen` list remains visible. |
| **`PaywallActionsTest`** | Tap `RESTORE PURCHASES` CTA on paywall | Paywall dismisses cleanly; app state remains healthy; `MainScreen` is restored. |
| **`PaywallNavigationTest`** | Dismiss paywall and reopen from a lower list item (`Earth 10`) | Modal closes on `NOT NOW` and re-renders properly with all controls upon subsequent `DOWNLOAD` tap. |

---

## Architecture & Project Structure

The framework is built around a unified `AppiumDriver` abstraction and dual-platform Page Object annotations (`@AndroidFindBy` / `@iOSXCUITFindBy`), avoiding brittle text-based XPath locators in favor of native resource IDs and accessibility identifiers.

```text
├── .github/workflows/
│   └── android-tests.yml          # GitHub Actions CI pipeline (API 34 emulator + Appium)
├── app/
│   └── app-debug.apk              # Target Android build (bundled for self-contained execution)
├── src/test/
│   ├── java/com/zipo/tests/
│   │   ├── config/                # Layered configuration resolver (CLI -> Env -> .env -> .properties)
│   │   ├── diagnostics/           # Non-blocking LLM failure triage (FailureAiAnalyzer)
│   │   ├── driver/                # ThreadLocal AppiumDriver factory (UiAutomator2 / XCUITest)
│   │   ├── extension/             # JUnit 5 lifecycle extension (Crash/ANR detection & artifacts)
│   │   ├── pages/                 # Cross-platform Page Objects (BaseScreen, Splash, Main, Paywall)
│   │   └── tests/                 # Smoke, Paywall CTA Actions, and Navigation test suites
│   └── resources/config/
│       ├── local.properties       # Default local emulator/device settings
│       └── ci.properties          # Headless CI emulator settings
├── .env.example                   # Template for local environment overrides & API keys
├── .gitignore                     # Excludes target/, .env, IDE files, and local logs
└── pom.xml                        # Maven build & dependency configuration
```

### Core Components
* **`DriverManager`**: Isolates `AppiumDriver` sessions via `ThreadLocal` and constructs platform-specific capabilities (`UiAutomator2Options` for Android, `XCUITestOptions` for iOS) based on `platformName`.
* **`MobileTestExtension`**: Custom JUnit 5 extension implementing `BeforeEachCallback`, `AfterTestExecutionCallback`, `TestWatcher`, and `ParameterResolver`. It drains pre-test device logs, automatically fails tests if an application crash (`FATAL EXCEPTION`, `AndroidRuntime`) or ANR (`ANR in ...`) appears in `logcat`—even if UI assertions pass—and captures failure artifacts before quitting the session.
* **Page Objects (`BaseScreen`, `SplashScreen`, `MainScreen`, `PaywallModal`)**: Encapsulate explicit `WebDriverWait` synchronization and screen transitions using `@AndroidFindBy` and `@iOSXCUITFindBy`.

---

## Configuration & Parameter Precedence

All environment, device, server, and API parameters are resolved dynamically at runtime without hardcoding values in Java code.

### How the `.env` File Works
1. Create a `.env` file in the repository root by copying the template:
   ```bash
   cp .env.example .env
   ```
2. Add your local overrides or secrets (such as `GEMINI_API_KEY`, `UDID`, or `APP_PATH`).
3. `.env` is excluded in `.gitignore`, ensuring local device IDs and personal API keys are never committed to Git.
4. At test startup, both `FrameworkConfig` and `FailureAiAnalyzer` read `KEY=VALUE` pairs from `.env` automatically (ignoring blank lines and `#` comments, and stripping surrounding single/double quotes) whenever a parameter has not already been supplied via CLI flags or OS environment variables.

### Parameter Priority Hierarchy
Configuration values are resolved in strict descending order of priority (highest precedence wins):

| Priority | Source | Format Example | Typical Use Case |
| :--- | :--- | :--- | :--- |
| **1 (Highest)** | **JVM System Properties (CLI)** | `-Denv=ci -Dudid=emulator-5554` | One-off overrides in terminal or IntelliJ IDEA Run Configurations |
| **2** | **OS Environment Variables** | `APP_PATH=/builds/app.apk` | GitHub Actions Secrets/Vars and CI/CD environment injection |
| **3** | **Local `.env` File** | `GEMINI_API_KEY=AIza...` | Local developer secrets and machine-specific overrides |
| **4 (Lowest)** | **Profile Properties File** | `src/test/resources/config/<env>.properties` | Baseline defaults for `local` (`local.properties`) or `ci` (`ci.properties`) |

### Configuration Reference

| JVM / `.properties` Key | Env / `.env` Key | Default (`local`) | Description |
| :--- | :--- | :--- | :--- |
| `env` | `ENV` | `local` | Active configuration profile (`local` or `ci`) |
| `platformName` | `PLATFORM_NAME` | `Android` | Target mobile OS (`Android` or `iOS`) |
| `platformVersion` | `PLATFORM_VERSION` | `14` | OS version on target emulator/device |
| `deviceName` | `DEVICE_NAME` | `emulator-5554` | Target device or emulator name |
| `udid` | `UDID` | _(empty)_ | Device UDID (optional when a single device/emulator is attached) |
| `avdName` | `AVD_NAME` | _(empty)_ | AVD name for Appium auto-launch (leave empty if emulator is already booted) |
| `appiumUrl` | `APPIUM_URL` | `http://127.0.0.1:4723` | Running Appium 2.x server URL |
| `appPath` | `APP_PATH` | `app/app-debug.apk` | Relative or absolute path to `.apk` / `.ipa` / `.app` binary |
| `appPackage` | `APP_PACKAGE` | `com.zipo.interviewproject` | Android `appPackage` or iOS `bundleId` |
| `appActivity` | `APP_ACTIVITY` | `.MainActivity` | Android launch activity |
| `appWaitActivity` | `APP_WAIT_ACTIVITY` | `*` | Activity wildcard to wait for on app launch |
| `explicitWaitSeconds` | `EXPLICIT_WAIT_SECONDS` | `15` | Explicit `WebDriverWait` timeout in seconds |
| `noReset` | `NO_RESET` | `false` | Preserve app state between tests (`true` / `false`) |
| `gemini.api.key` | `GEMINI_API_KEY` | _(empty)_ | Optional Gemini API key for automated failure root-cause triage |
| `gemini.model` | `GEMINI_MODEL` | `gemini-4-hydrogen` | Preferred Gemini model (supports comma-separated fallback list) |

---

## Running Tests via Maven

### Prerequisites
1. **JDK 21** (`java -version`)
2. **Maven 3.9+** (`mvn -version`)
3. **Appium 2.x** with the `uiautomator2` driver installed:
   ```bash
   npm install -g appium
   appium driver install uiautomator2
   ```
4. A booted Android Emulator or connected physical device (`adb devices`) and a running Appium server (`appium`).

### Maven Execution Commands

```bash
# 1. Run the full test suite with default 'local' profile (src/test/resources/config/local.properties)
mvn test

# 2. Run using the CI profile (src/test/resources/config/ci.properties)
mvn test -Denv=ci

# 3. Override target device UDID and APK binary path from the command line
mvn test -Dudid=emulator-5554 -DappPath=app/app-debug.apk

# 4. Point tests to a remote Appium server with a custom explicit wait timeout
mvn test -DappiumUrl=[http://192.168.1.50:4723](http://192.168.1.50:4723) -DexplicitWaitSeconds=25

# 5. Enable AI failure triage via CLI system properties
mvn test -Dgemini.api.key=YOUR_API_KEY -Dgemini.model=gemini-4-hydrogen

# 6. Run a single test class or an individual test method
mvn test -Dtest=PaywallSmokeTest
mvn test -Dtest=PaywallNavigationTest#shouldReopenPaywallOnSubsequentDownloadTap
```

> **IntelliJ IDEA Tip:** `pom.xml` targets Java 21 and JUnit `5.11.4`. To run and debug tests directly via the native IDEA JUnit runner, keep **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → Delegate IDE build/run actions to Maven** disabled.

---

## Failure Diagnostics & AI Triage

Whenever a test fails, `MobileTestExtension` automatically creates a timestamped folder under `target/diagnostics/<TestClass>_<testMethod>_<timestamp>/` containing:
* `error.txt` — complete Java exception and stack trace
* `failure.png` — device screenshot captured at the moment of failure
* `page_source.xml` — full UI hierarchy XML dump
* `logcat.txt` — device logs collected during the test execution window

### Automated AI Root-Cause Analysis (`FailureAiAnalyzer`)
Immediately after writing the diagnostic files, `FailureAiAnalyzer` packages the stack trace, the tail of `logcat.txt`, and a truncated `page_source.xml` snapshot into a structured triage prompt:
* **If `GEMINI_API_KEY` is set:** Sends the diagnostic context to the Gemini API, writes a 3-section Markdown report (`Category`, `Root Cause`, `Recommended Fix`) to `ai_analysis.md`, and prints the summary to the console.
* **If `GEMINI_API_KEY` is omitted or unreachable:** Logs a non-blocking message and exits cleanly without masking the original JUnit assertion failure.
* **Testing Note on Live LLM Response:** During local fault-injection testing, configuration resolution (`.env` / CLI), diagnostic payload truncation, JSON serialization, model fallback iteration, and non-blocking error handling were verified end-to-end; however, live report generation could not be completed at test time because the upstream Gemini API endpoints returned `HTTP 503 (UNAVAILABLE)` due to temporary server overload (`high demand`).

---

## CI/CD Pipeline (GitHub Actions)

The workflow `.github/workflows/android-tests.yml` triggers automatically on every `push` and `pull_request` to `main` (and supports manual `workflow_dispatch` runs):
1. Provisions an `ubuntu-latest` runner with **Temurin JDK 21** (with Maven cache) and **Node.js 20**.
2. Installs **Appium** and the **`uiautomator2`** driver, and configures **KVM** hardware acceleration.
3. Boots a headless **Android API 34 (`x86_64`)** emulator via `reactivecircus/android-emulator-runner@v2` with system animations disabled.
4. Starts Appium in the background, polls `/status` until ready, and executes `mvn test -B -Denv=ci`.
5. Uploads `target/surefire-reports/`, `target/diagnostics/`, and `target/appium.log` as the `test-reports-and-diagnostics` workflow artifact (`if: always()`).

To enable AI failure reports in CI, configure `GEMINI_API_KEY` under **Repository Settings → Secrets and variables → Actions**.

---

## Scaling Strategy (~20 Mobile Applications)

The framework is designed to be completely agnostic to a specific application or mobile OS, allowing seamless scaling from a single assessment app to a portfolio of ~20 Android and iOS applications purely through configuration:

1. **Property-Driven Multi-App & Multi-OS Setup:**
    * Core engine components (`DriverManager`, `MobileTestExtension`, `FrameworkConfig`, `BaseScreen`, and `FailureAiAnalyzer`) contain zero hardcoded app identifiers or platform assumptions.
    * Onboarding a new application or switching between Android and iOS requires only adding a dedicated profile file under `src/test/resources/config/<profile>.properties` (e.g., `app1-android.properties`, `app1-ios.properties`, `app2-ci.properties`) with its own `platformName`, `appPackage` / `bundleId`, `appActivity`, `appPath`, and `explicitWaitSeconds`, activated at runtime via `-Denv=<profile>`.
    * Screen objects and test suites are organized cleanly into app-specific packages (`pages.<appName>` and `tests.<appName>`) and executed selectively using JUnit 5 `@Tag` filtering (`mvn test -Denv=app2-android -Dgroups=app2`).
2. **Dynamic Binary Delivery (`appPath` vs. Repository Storage):**
    * *Assessment Trade-off:* `app/app-debug.apk` is committed directly to this repository so reviewers can clone and run `mvn test` immediately with zero external setup.
    * *Production Setup:* Application binaries should not live in Git. CI pipelines fetch the latest signed `.apk`/`.ipa` from build artifacts, Amazon S3, or Firebase App Distribution into a temporary workspace and inject its path dynamically via `-DappPath` or `APP_PATH`.
3. **Parallel Execution & Cloud Device Farms:**
    * Because `DriverManager` isolates sessions per thread via `ThreadLocal<AppiumDriver>`, execution scales horizontally across multiple local emulators or cloud device farms (BrowserStack, Sauce Labs, AWS Device Farm) simply by configuring `appiumUrl`, `udid`, and target capabilities in a `.properties` profile or CLI arguments without modifying test code.

---

## Technical Trade-offs & Known Limitations

* **System `Toast` Verification Excluded:** Tapping `SUBSCRIBE`, `PURCHASE`, or `RESTORE PURCHASES` displays a transient Android system `Toast` (`Toast.LENGTH_SHORT`, ~2 seconds) rendered in a separate system overlay window (`TYPE_TOAST`) outside the application's view hierarchy. Polling short-lived system toasts in `UiAutomator2` introduces timing flakiness across varying emulator CPU loads. To keep the test suite 100% deterministic, CTA tests verify the observable UI contract instead: the paywall modal dismisses cleanly, the app process remains alive without crashes/ANRs, and the interactive `MainScreen` list is restored.
* **List Scrolling:** On standard viewport heights (e.g., Pixel 6), all 11 list items (`Earth 1` through `Earth 11`) fit completely inside the visible `RecyclerView` bounds without vertical scrolling. Accordingly, `PaywallNavigationTest` directly exercises lower-list interaction (`Earth 10`) and modal reopening after dismissal rather than performing a no-op swipe gesture.