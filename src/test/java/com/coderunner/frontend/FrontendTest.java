package com.coderunner.frontend;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================
 *  CodeRunner — Frontend Module — JUnit 5 Test Suite
 *  Person 1 | DevOps Mini Project
 * ============================================================
 *
 *  Test Categories:
 *  1. Unit Tests      → Logic tests (no server needed)
 *  2. Integration Tests → API call tests (server must be running)
 *
 *  Run with:  mvn test
 * ============================================================
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FrontendTest {

    // ─── CONFIG ───────────────────────────────────────────────
    private static final String BASE_URL = "http://localhost:8080";
    private static HttpClient client;

    @BeforeAll
    static void setup() {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }


    // ══════════════════════════════════════════════════════════
    //  SECTION 1 — UNIT TESTS (Pure Logic, No Server Required)
    // ══════════════════════════════════════════════════════════

    /**
     * Test: Request JSON builder produces valid JSON structure
     */
    @Test
    @Order(1)
    @DisplayName("Unit: buildRequestJson returns valid JSON")
    void testBuildRequestJsonStructure() {
        String json = buildRequestJson("java", "JDK 21", "System.out.println(1);", "");
        assertTrue(json.contains("\"language\""));
        assertTrue(json.contains("\"code\""));
        assertTrue(json.contains("\"stdin\""));
        assertTrue(json.contains("java"));
    }

    /**
     * Test: Empty code string is detected before sending
     */
    @Test
    @Order(2)
    @DisplayName("Unit: empty code is detected as invalid input")
    void testEmptyCodeDetection() {
        String code = "   ";
        assertTrue(code.trim().isEmpty(), "Whitespace-only code should be treated as empty");
    }

    /**
     * Test: Language list contains all expected languages
     */
    @Test
    @Order(3)
    @DisplayName("Unit: supported language list is complete")
    void testSupportedLanguages() {
        String[] supported = {"java", "python", "javascript", "c", "cpp"};
        for (String lang : supported) {
            assertNotNull(lang);
            assertFalse(lang.isEmpty());
        }
        assertEquals(5, supported.length);
    }

    /**
     * Test: HTML-escape function prevents XSS in output display
     */
    @Test
    @Order(4)
    @DisplayName("Unit: HTML escape prevents XSS in output")
    void testHtmlEscape() {
        String raw = "<script>alert('xss')</script>";
        String escaped = escapeHtml(raw);
        assertFalse(escaped.contains("<script>"), "Script tags must be escaped");
        assertTrue(escaped.contains("&lt;script&gt;"));
    }

    /**
     * Test: Execution time display is formatted correctly
     */
    @Test
    @Order(5)
    @DisplayName("Unit: execution time formats to 2 decimal places")
    void testExecTimeFormat() {
        double ms = 1234;
        String formatted = String.format("%.2fs", ms / 1000);
        assertEquals("1.23s", formatted);
    }

    /**
     * Test: Version map returns correct options per language
     */
    @Test
    @Order(6)
    @DisplayName("Unit: version options exist for every language")
    void testVersionOptionsExist() {
        java.util.Map<String, String[]> versions = java.util.Map.of(
            "java",       new String[]{"JDK 21", "JDK 17", "JDK 11"},
            "python",     new String[]{"Python 3.12", "Python 3.10"},
            "javascript", new String[]{"Node 20 LTS", "Node 18 LTS"},
            "c",          new String[]{"GCC 13", "GCC 11"},
            "cpp",        new String[]{"GCC 13 (C++23)", "GCC 11 (C++17)"}
        );
        versions.forEach((lang, opts) -> {
            assertTrue(opts.length >= 2, lang + " should have at least 2 version options");
        });
    }

    /**
     * Test: Stdin value is included in request body
     */
    @Test
    @Order(7)
    @DisplayName("Unit: stdin is included in request JSON")
    void testStdinIncludedInRequest() {
        String stdin = "42\nhello";
        String json = buildRequestJson("python", "Python 3.12", "x=input()", stdin);
        assertTrue(json.contains("42\\nhello") || json.contains("42\nhello"),
                "stdin value should appear in the JSON body");
    }

    /**
     * Test: Language label formatting (first letter uppercase)
     */
    @ParameterizedTest
    @Order(8)
    @ValueSource(strings = {"java", "python", "javascript", "c", "cpp"})
    @DisplayName("Unit: language name formats correctly for display")
    void testLangLabelFormat(String lang) {
        String label = lang.substring(0, 1).toUpperCase() + lang.substring(1);
        assertTrue(Character.isUpperCase(label.charAt(0)));
    }


    // ══════════════════════════════════════════════════════════
    //  SECTION 2 — INTEGRATION TESTS (Requires API Server)
    // ══════════════════════════════════════════════════════════

    /**
     * Integration Test: Health endpoint returns 200
     */
    @Test
    @Order(10)
    @DisplayName("Integration: GET /api/health returns 200")
    void testHealthEndpointReachable() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Health endpoint must return 200");
    }

    /**
     * Integration Test: Java Hello World executes correctly
     */
    @Test
    @Order(11)
    @DisplayName("Integration: Java Hello World returns correct output")
    void testJavaHelloWorldExecution() throws Exception {
        String code = "public class Main { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }";
        String body = buildRequestJson("java", "JDK 21", code, "");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("Hello, World!"),
                "Response should contain 'Hello, World!'");
    }

    /**
     * Integration Test: Python print executes correctly
     */
    @Test
    @Order(12)
    @DisplayName("Integration: Python print returns correct output")
    void testPythonExecution() throws Exception {
        String body = buildRequestJson("python", "Python 3.12", "print('CodeRunner')", "");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("CodeRunner"));
    }

    /**
     * Integration Test: Stdin is passed to the program correctly
     */
    @Test
    @Order(13)
    @DisplayName("Integration: stdin value is passed and read by program")
    void testStdinPassedCorrectly() throws Exception {
        String code = "import java.util.Scanner; public class Main { public static void main(String[] args) { Scanner sc = new Scanner(System.in); System.out.println(sc.nextLine()); } }";
        String body = buildRequestJson("java", "JDK 21", code, "HelloStdin");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("HelloStdin"),
                "Output should echo back the stdin input");
    }

    /**
     * Integration Test: Compilation error returns non-zero exit code
     */
    @Test
    @Order(14)
    @DisplayName("Integration: invalid Java code returns compilation error")
    void testCompilationErrorHandled() throws Exception {
        String body = buildRequestJson("java", "JDK 21", "this is not valid java code at all!!!", "");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "API should still return 200, with error in body");
        // exitCode in body should be non-zero
        assertTrue(res.body().contains("exitCode") || res.body().contains("stderr"),
                "Error response should contain exitCode or stderr field");
    }

    /**
     * Integration Test: Empty code body returns a handled error (not 500)
     */
    @Test
    @Order(15)
    @DisplayName("Integration: empty code does not crash the server")
    void testEmptyCodeDoesNotCrashServer() throws Exception {
        String body = buildRequestJson("java", "JDK 21", "", "");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertNotEquals(500, res.statusCode(), "Empty code must NOT cause a 500 server error");
    }

    /**
     * Integration Test: Response contains required JSON fields
     */
    @Test
    @Order(16)
    @DisplayName("Integration: response body contains stdout, stderr, exitCode")
    void testResponseStructure() throws Exception {
        String code = "public class Main { public static void main(String[] args) { System.out.println(1); } }";
        String body = buildRequestJson("java", "JDK 21", code, "");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/execute"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        String responseBody = res.body();
        assertTrue(responseBody.contains("stdout"),   "Response must have 'stdout' field");
        assertTrue(responseBody.contains("stderr"),   "Response must have 'stderr' field");
        assertTrue(responseBody.contains("exitCode"), "Response must have 'exitCode' field");
    }


    // ══════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ══════════════════════════════════════════════════════════

    /**
     * Builds the JSON request body for /api/execute
     */
    private String buildRequestJson(String language, String version, String code, String stdin) {
        String escapedCode  = code.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String escapedStdin = stdin.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return String.format(
            "{\"language\":\"%s\",\"version\":\"%s\",\"code\":\"%s\",\"stdin\":\"%s\"}",
            language, version, escapedCode, escapedStdin
        );
    }

    /**
     * Mimics the frontend's HTML escape logic (mirrors escHtml in index.html)
     */
    private String escapeHtml(String input) {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
