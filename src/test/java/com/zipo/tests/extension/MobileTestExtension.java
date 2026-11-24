package com.zipo.tests.extension;

import com.zipo.tests.config.FrameworkConfig;
import com.zipo.tests.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;
import io.appium.java_client.appmanagement.ApplicationState;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.logging.LogEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MobileTestExtension implements
        BeforeEachCallback,
        AfterTestExecutionCallback,
        TestWatcher,
        ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MobileTestExtension.class);
    private static final String LOGS_KEY = "test_device_logs";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void beforeEach(ExtensionContext context) {
        DriverManager.initDriver();
        // Drain pre-test log buffer so captured logs belong strictly to this test
        fetchDeviceLogsSafely(DriverManager.getDriver());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (!DriverManager.hasDriver()) {
            return;
        }
        AppiumDriver driver = DriverManager.getDriver();
        String appId = FrameworkConfig.getInstance().appPackage();

        List<String> logLines = fetchDeviceLogsSafely(driver);
        context.getStore(NAMESPACE).put(LOGS_KEY, logLines);

        List<String> fatalLines = logLines.stream()
                .filter(line -> isAppCrashOrAnr(line, appId))
                .toList();

        if (!fatalLines.isEmpty()) {
            String preview = fatalLines.stream().limit(15).collect(Collectors.joining(System.lineSeparator()));
            throw new AssertionError("Application Crash or ANR detected during test execution:\n" + preview);
        }

        if (driver instanceof InteractsWithApps appDriver) {
            try {
                ApplicationState state = appDriver.queryAppState(appId);
                if (state == ApplicationState.NOT_RUNNING) {
                    throw new AssertionError("Application process (" + appId + ") terminated unexpectedly (NOT_RUNNING).");
                }
            } catch (AssertionError ae) {
                throw ae;
            } catch (Exception ignored) {
                // Ignore state query errors if session already closed
            }
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testId = sanitize(context.getRequiredTestClass().getSimpleName()
                + "_" + context.getRequiredTestMethod().getName()
                + "_" + LocalDateTime.now().format(TIMESTAMP_FMT));

        Path outputDir = Path.of("target", "diagnostics", testId);

        try {
            Files.createDirectories(outputDir);
            saveErrorTrace(cause, outputDir.resolve("error.txt"));

            if (DriverManager.hasDriver()) {
                AppiumDriver driver = DriverManager.getDriver();
                saveScreenshot(driver, outputDir.resolve("failure.png"));
                savePageSource(driver, outputDir.resolve("page_source.xml"));
                saveDeviceLogs(context, driver, outputDir.resolve("logcat.txt"));
            } else {
                System.err.println("[Diagnostics] Driver was not initialized; skipped screenshot and device logs.");
            }

            System.err.println("[Diagnostics] Saved failure artifacts to: " + outputDir.toAbsolutePath());
            if (cause != null) {
                System.err.println("[Diagnostics] Failure reason: " + cause);
            }
        } catch (IOException e) {
            System.err.println("[Diagnostics] Failed to write failure artifacts: " + e.getMessage());
        } finally {
            DriverManager.quitDriver();
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        DriverManager.quitDriver();
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        DriverManager.quitDriver();
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        DriverManager.quitDriver();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return AppiumDriver.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return DriverManager.getDriver();
    }

    private boolean isAppCrashOrAnr(String line, String appId) {
        boolean isCrash = line.contains("FATAL EXCEPTION")
                || (line.contains("AndroidRuntime") && line.contains(appId));
        boolean isAnr = line.contains("ANR in " + appId)
                || (line.contains("Application Not Responding") && line.contains(appId));
        return isCrash || isAnr;
    }

    private List<String> fetchDeviceLogsSafely(AppiumDriver driver) {
        String logType = "ios".equalsIgnoreCase(FrameworkConfig.getInstance().platformName())
                ? "syslog"
                : "logcat";
        try {
            return driver.manage().logs().get(logType).getAll().stream()
                    .map(LogEntry::getMessage)
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void saveScreenshot(AppiumDriver driver, Path targetFile) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(targetFile, png);
        } catch (Exception e) {
            System.err.println("[Diagnostics] Unable to capture screenshot: " + e.getMessage());
        }
    }

    private void savePageSource(AppiumDriver driver, Path targetFile) {
        try {
            String xml = driver.getPageSource();
            if (xml != null) {
                Files.writeString(targetFile, xml, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            System.err.println("[Diagnostics] Unable to capture page source: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void saveDeviceLogs(ExtensionContext context, AppiumDriver driver, Path targetFile) {
        try {
            List<String> storedLogs = Optional.ofNullable(
                    (List<String>) context.getStore(NAMESPACE).get(LOGS_KEY)
            ).orElseGet(() -> fetchDeviceLogsSafely(driver));

            Files.write(targetFile, storedLogs, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[Diagnostics] Unable to write device logs: " + e.getMessage());
        }
    }

    private void saveErrorTrace(Throwable cause, Path targetFile) throws IOException {
        if (cause == null) {
            return;
        }
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        Files.writeString(targetFile, sw.toString(), StandardCharsets.UTF_8);
    }

    private String sanitize(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}