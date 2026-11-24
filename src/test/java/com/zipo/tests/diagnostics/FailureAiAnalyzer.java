package com.zipo.tests.diagnostics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class FailureAiAnalyzer {

    private static final String DEFAULT_MODEL = "gemini-4-hydrogen";
    private static final String GEMINI_ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final int MAX_ERROR_CHARS = 2500;
    private static final int MAX_LOG_LINES = 60;
    private static final int MAX_PAGE_SOURCE_CHARS = 4500;

    private FailureAiAnalyzer() {
    }

    public static void analyzeAndSave(String testId, Path diagnosticsDir) {
        String apiKey = resolveSetting("GEMINI_API_KEY", "gemini.api.key");
        if (apiKey.isBlank()) {
            System.out.println("[AI Triage] Skipped: GEMINI_API_KEY is not configured.");
            return;
        }

        String model = resolveSetting("GEMINI_MODEL", "gemini.model");
        if (model.isBlank()) {
            model = DEFAULT_MODEL;
        }

        try {
            String errorTrace = readTruncatedText(diagnosticsDir.resolve("error.txt"), MAX_ERROR_CHARS);
            String logTail = readLogTail(diagnosticsDir.resolve("logcat.txt"), MAX_LOG_LINES);
            String pageSource = readTruncatedText(diagnosticsDir.resolve("page_source.xml"), MAX_PAGE_SOURCE_CHARS);

            String prompt = buildPrompt(testId, errorTrace, logTail, pageSource);
            String analysis = callGeminiApi(apiKey, model, prompt);

            if (analysis != null && !analysis.isBlank()) {
                Path reportPath = diagnosticsDir.resolve("ai_analysis.md");
                String markdown = "# AI Failure Analysis: " + testId + System.lineSeparator()
                        + "_Model: " + model + "_" + System.lineSeparator() + System.lineSeparator()
                        + analysis.trim() + System.lineSeparator();

                Files.writeString(reportPath, markdown, StandardCharsets.UTF_8);
                System.err.println("[AI Triage] Saved analysis to: " + reportPath.toAbsolutePath());
                System.err.println("--- AI Failure Summary ---" + System.lineSeparator()
                        + analysis.trim() + System.lineSeparator()
                        + "--------------------------");
            }
        } catch (Exception e) {
            System.err.println("[AI Triage] Analysis failed (non-blocking): " + e.getMessage());
        }
    }

    private static String buildPrompt(String testId, String errorTrace, String logTail, String pageSource) {
        return """
                You are a Senior Mobile QA Automation Engineer triaging a failed Appium test run.
                Analyze the failure artifacts below and provide a concise, actionable root-cause report in Markdown.

                Include strictly these 3 sections:
                1. **Category**: Choose one (`Locator / UI Mismatch`, `Timing / Synchronization`, `Application Crash / ANR`, `Test Assertion / Logic`, or `Environment / Appium Setup`).
                2. **Root Cause**: 2-3 sentences explaining exact cause based on stack trace, logs, and UI hierarchy.
                3. **Recommended Fix**: Concrete code or configuration fix.

                ### Test Identifier
                %s

                ### Exception & Stack Trace
                ```text
                %s
                ```

                ### Device Logs (Tail)
                ```text
                %s
                ```

                ### UI Hierarchy Snapshot (Truncated XML)
                ```xml
                %s
                ```
                """.formatted(
                testId,
                errorTrace.isBlank() ? "<unavailable>" : errorTrace,
                logTail.isBlank() ? "<unavailable>" : logTail,
                pageSource.isBlank() ? "<unavailable>" : pageSource
        );
    }

    private static String callGeminiApi(String apiKey, String model, String prompt)
            throws IOException, InterruptedException {
        String requestBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        { "text": %s }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.2,
                    "maxOutputTokens": 700
                  }
                }
                """.formatted(toJsonString(prompt));

        URI endpoint = URI.create(GEMINI_ENDPOINT_TEMPLATE.formatted(model, apiKey));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Gemini API returned HTTP " + response.statusCode() + ": " + response.body());
            }
            return extractTextFromGeminiJson(response.body());
        }
    }

    private static String resolveSetting(String envKey, String sysPropKey) {
        String sysProp = System.getProperty(sysPropKey);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        return readFromDotEnv(envKey).orElse("");
    }

    private static Optional<String> readFromDotEnv(String key) {
        Path dotEnv = Path.of(".env");
        if (!Files.exists(dotEnv)) {
            return Optional.empty();
        }
        try {
            for (String rawLine : Files.readAllLines(dotEnv, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int idx = line.indexOf('=');
                String k = line.substring(0, idx).trim();
                if (k.equals(key)) {
                    String v = line.substring(idx + 1).trim();
                    if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                        v = v.substring(1, v.length() - 1);
                    }
                    return Optional.of(v);
                }
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    private static String readTruncatedText(Path file, int maxChars) {
        if (!Files.exists(file)) {
            return "";
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.length() <= maxChars) {
                return content;
            }
            return content.substring(0, maxChars) + System.lineSeparator() + "... [truncated]";
        } catch (IOException e) {
            return "";
        }
    }

    private static String readLogTail(Path file, int maxLines) {
        if (!Files.exists(file)) {
            return "";
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int start = Math.max(0, lines.size() - maxLines);
            return String.join(System.lineSeparator(), lines.subList(start, lines.size()));
        } catch (IOException e) {
            return "";
        }
    }

    private static String toJsonString(String raw) {
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        sb.append('"');
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static String extractTextFromGeminiJson(String json) {
        String marker = "\"text\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return "";
        }
        int quoteStart = json.indexOf('"', colonIndex + 1);
        if (quoteStart < 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean escaping = false;
        for (int i = quoteStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            result.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                    }
                    default -> result.append(c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}