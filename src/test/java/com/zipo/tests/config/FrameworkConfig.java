package com.zipo.tests.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class FrameworkConfig {

    private static final FrameworkConfig INSTANCE = new FrameworkConfig();

    private final Properties profileProperties = new Properties();
    private final Map<String, String> dotEnvValues = new HashMap<>();

    private FrameworkConfig() {
        loadDotEnv();
        String env = resolve("env", "APP_ENV", "local");
        loadProfile(env);
    }

    public static FrameworkConfig getInstance() {
        return INSTANCE;
    }

    public String appiumUrl() {
        return resolve("appiumUrl", "APPIUM_URL", "appium.url", "http://127.0.0.1:4723");
    }

    public String platformName() {
        return resolve("platformName", "PLATFORM_NAME", "platform.name", "Android");
    }

    public String platformVersion() {
        return resolve("platformVersion", "PLATFORM_VERSION", "platform.version", "14");
    }

    public String deviceName() {
        return resolve("deviceName", "DEVICE_NAME", "device.name", "pixel_7_api34_play");
    }

    public String udid() {
        return resolve("udid", "UDID", "udid", "");
    }

    public String avdName() {
        return resolve("avdName", "AVD_NAME", "avd.name", "");
    }

    public String appPath() {
        return resolve("appPath", "APP_PATH", "app.path", "src/test/resources/app/app-debug.apk");
    }

    public String appPackage() {
        return resolve("appPackage", "APP_PACKAGE", "app.package", "com.zipo.interviewproject");
    }

    public String appActivity() {
        return resolve("appActivity", "APP_ACTIVITY", "app.activity", "com.zipo.interviewproject.SplashActivity");
    }

    public String appWaitActivity() {
        return resolve("appWaitActivity", "APP_WAIT_ACTIVITY", "app.wait.activity", "com.zipo.interviewproject.*");
    }

    public Duration explicitWait() {
        String seconds = resolve("explicitWaitSeconds", "EXPLICIT_WAIT_SECONDS", "explicit.wait.seconds", "15");
        return Duration.ofSeconds(Long.parseLong(seconds));
    }

    public boolean noReset() {
        String value = resolve("noReset", "NO_RESET", "no.reset", "false");
        return Boolean.parseBoolean(value);
    }

    private void loadProfile(String env) {
        String resourcePath = "config/" + env + ".properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Environment config not found on classpath: " + resourcePath);
            }
            profileProperties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load environment config: " + resourcePath, e);
        }
    }

    private void loadDotEnv() {
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return;
        }
        try {
            for (String rawLine : Files.readAllLines(envFile)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int idx = line.indexOf('=');
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                dotEnvValues.put(key, val);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env file", e);
        }
    }

    private String resolve(String sysProp, String envVar, String defaultValue) {
        return resolve(sysProp, envVar, null, defaultValue);
    }

    private String resolve(String sysProp, String envVar, String propKey, String defaultValue) {
        String sysVal = System.getProperty(sysProp);
        if (isPresent(sysVal)) {
            return sysVal.trim();
        }
        String osEnvVal = System.getenv(envVar);
        if (isPresent(osEnvVal)) {
            return osEnvVal.trim();
        }
        String dotEnvVal = dotEnvValues.get(envVar);
        if (isPresent(dotEnvVal)) {
            return dotEnvVal.trim();
        }
        if (propKey != null) {
            String fileVal = profileProperties.getProperty(propKey);
            if (isPresent(fileVal)) {
                return fileVal.trim();
            }
        }
        return defaultValue;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}