package com.zipo.tests.driver;

import com.zipo.tests.config.FrameworkConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class DriverManager {

    private static final ThreadLocal<AppiumDriver> DRIVER_HOLDER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static void initDriver() {
        FrameworkConfig config = FrameworkConfig.getInstance();
        Path appFile = Path.of(config.appPath()).toAbsolutePath();

        if (!Files.exists(appFile)) {
            throw new IllegalStateException("Application binary not found at: " + appFile);
        }

        URL serverUrl = parseUrl(config.appiumUrl());
        String platform = config.platformName().trim().toLowerCase();

        AppiumDriver driver = switch (platform) {
            case "android" -> createAndroidDriver(serverUrl, config, appFile);
            case "ios" -> createIosDriver(serverUrl, config, appFile);
            default -> throw new IllegalArgumentException(
                    "Unsupported platformName: '" + config.platformName() + "'. Expected 'Android' or 'iOS'."
            );
        };

        DRIVER_HOLDER.set(driver);
    }

    private static AndroidDriver createAndroidDriver(URL serverUrl, FrameworkConfig config, Path apkPath) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setPlatformVersion(config.platformVersion())
                .setDeviceName(config.deviceName())
                .setApp(apkPath.toString())
                .setAppPackage(config.appPackage())
                .setAppActivity(config.appActivity())
                .setAppWaitActivity(config.appWaitActivity())
                .setAppWaitDuration(Duration.ofSeconds(20))
                .setAutoGrantPermissions(true)
                .setNoReset(config.noReset())
                .setNewCommandTimeout(Duration.ofSeconds(60));

        // Required so UiAutomator2 can locate system Toast windows outside the main app window
        options.setCapability("appium:enableMultiWindows", true);

        if (!config.udid().isBlank()) {
            options.setUdid(config.udid());
        }
        if (!config.avdName().isBlank()) {
            options.setAvd(config.avdName());
        }

        return new AndroidDriver(serverUrl, options);
    }

    private static IOSDriver createIosDriver(URL serverUrl, FrameworkConfig config, Path ipaOrAppPath) {
        XCUITestOptions options = new XCUITestOptions()
                .setPlatformName("iOS")
                .setPlatformVersion(config.platformVersion())
                .setDeviceName(config.deviceName())
                .setApp(ipaOrAppPath.toString())
                .setBundleId(config.appPackage())
                .setAutoAcceptAlerts(true)
                .setNoReset(config.noReset())
                .setNewCommandTimeout(Duration.ofSeconds(60));

        if (!config.udid().isBlank()) {
            options.setUdid(config.udid());
        }

        return new IOSDriver(serverUrl, options);
    }

    private static URL parseUrl(String rawUrl) {
        try {
            return URI.create(rawUrl).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Appium server URL: " + rawUrl, e);
        }
    }

    public static AppiumDriver getDriver() {
        AppiumDriver driver = DRIVER_HOLDER.get();
        if (driver == null) {
            throw new IllegalStateException("AppiumDriver is not initialized for the current thread.");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER_HOLDER.get() != null;
    }

    public static void quitDriver() {
        AppiumDriver driver = DRIVER_HOLDER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER_HOLDER.remove();
            }
        }
    }
}