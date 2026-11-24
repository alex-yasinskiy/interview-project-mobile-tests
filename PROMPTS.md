# Key Engineering Prompts — Mobile Test Automation Framework

A curated log of the core architectural and implementation prompts used to design, build, and document this cross-platform mobile test automation framework.

---

### 1. Foundation & Cross-Platform Architecture
> "Let's bootstrap a clean, cross-platform mobile test automation framework using Java 21, JUnit 5, Appium 2.x, and Maven. Design a unified `AppiumDriver` architecture (`DriverManager` and `BaseScreen`) that supports both Android (`UiAutomator2`) and iOS (`XCUITest`) out of the box without hardcoding device or app parameters. Build a layered configuration loader where CLI flags (`-D...`), OS environment variables, a local `.env` file, and environment-specific `.properties` profiles (`local` vs. `ci`) cleanly override one another."

### 2. JUnit 5 Lifecycle Extension & Automated Crash/ANR Detection
> "Create a custom JUnit 5 extension (`MobileTestExtension`) to own the entire test lifecycle. Beyond initializing and tearing down the driver cleanly, have it drain pre-test device logs and inspect `logcat` after every test execution so any fatal application crash or ANR automatically fails the run—even if all UI assertions passed. Whenever a test fails, capture a full diagnostic bundle (`error.txt`, screenshot, `page_source.xml`, and `logcat.txt`) in a timestamped folder before closing the session."

### 3. Cross-Platform Page Objects & Deterministic Paywall Suite
> "Using the element IDs inspected from the target APK, implement cross-platform Page Objects (`SplashScreen`, `MainScreen`, and `PaywallModal`) with `@AndroidFindBy` and `@iOSXCUITFindBy` annotations, avoiding fragile text-based XPath locators. Then build a focused test suite covering app launch, paywall content validation, CTA button clicks (`SUBSCRIBE`, `PURCHASE`, `RESTORE PURCHASES`), and modal reopening. Skip polling transient Android system `Toast` messages—instead, assert deterministic UI outcomes: clean modal dismissal, process stability, and return to an interactive main list."

### 4. Zero-Dependency AI Failure Triage (`FailureAiAnalyzer`)
> "Let's add an automated AI failure triage module (`FailureAiAnalyzer`) using only Java 21's standard `HttpClient`—no extra external libraries in `pom.xml`. When a test fails, package the stack trace, the tail of `logcat.txt`, and a truncated `page_source.xml` snapshot into a structured prompt for the Gemini API. Have it classify the failure, pinpoint the root cause, suggest a concrete fix, and save the report as `ai_analysis.md` alongside the failure artifacts—while remaining completely non-blocking if the API key is missing or the service is unreachable."

### 5. Resilient Model Fallback for High Availability
> "Single LLM endpoints can hit temporary overload (`HTTP 503`) or deprecation errors. Upgrade `FailureAiAnalyzer` with a cascading fallback chain (`gemini-4-hydrogen` down through lightweight Flash variants, plus dynamic model discovery if needed) so that if one model is unavailable or overloaded, the analyzer automatically fails over to the next working model without interrupting test cleanup."

### 6. Headless GitHub Actions CI/CD Pipeline
> "Set up a complete GitHub Actions workflow (`.github/workflows/android-tests.yml`) to run the suite automatically on every push and pull request to `main`. Configure an Ubuntu runner with KVM hardware acceleration, JDK 21, Node.js, Appium with `uiautomator2`, and a headless Android API 34 emulator. Run the suite against the `ci` profile (`-Denv=ci`) and always upload Surefire reports, Appium server logs, and failure diagnostics as build artifacts."

### 7. Comprehensive Documentation & Property-Driven Scaling (~20 Apps)
> "Pull everything together into a comprehensive, well-structured English `README.md`. Clearly explain how `.env` overrides work, document the full parameter priority hierarchy with practical Maven CLI examples, and note the technical trade-offs (such as excluding flaky system Toasts and verifying the AI analyzer's fault tolerance during upstream `503` outages). For the scaling section (~20 mobile apps), skip heavyweight shared library dependencies—focus instead on how the framework scales effortlessly across different apps and OS platforms through isolated `.properties` profiles, JUnit tags, and dynamic binary injection."