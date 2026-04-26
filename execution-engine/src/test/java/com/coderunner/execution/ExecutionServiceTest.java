package com.coderunner.execution;

import com.coderunner.execution.model.CodeRequest;
import com.coderunner.execution.model.ExecutionResult;
import com.coderunner.execution.service.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionServiceTest {

    private ExecutionService executionService;

    @BeforeEach
    void setUp() {
        executionService = new ExecutionService();
    }

    // ─────────────────────────────────────────────
    // Java Tests
    // ─────────────────────────────────────────────

    @Test
    void testJavaHelloWorld() {
        String code = """
                public class HelloWorld {
                    public static void main(String[] args) {
                        System.out.println("Hello, World!");
                    }
                }
                """;
        ExecutionResult result = executionService.execute(new CodeRequest("java", code));

        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getStdout().contains("Hello, World!"));
        assertTrue(result.getStderr().isEmpty());
    }

    @Test
    void testJavaCompilationError() {
        String code = """
                public class Broken {
                    public static void main(String[] args) {
                        System.out.println("missing semicolon")
                    }
                }
                """;
        ExecutionResult result = executionService.execute(new CodeRequest("java", code));

        assertEquals("ERROR", result.getStatus());
        assertFalse(result.getStderr().isEmpty()); // should have compiler error
    }

    @Test
    void testJavaRuntimeException() {
        String code = """
                public class Crasher {
                    public static void main(String[] args) {
                        int[] arr = new int[5];
                        System.out.println(arr[10]); // ArrayIndexOutOfBounds
                    }
                }
                """;
        ExecutionResult result = executionService.execute(new CodeRequest("java", code));

        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getStderr().contains("ArrayIndexOutOfBoundsException"));
    }

    @Test
    void testJavaMultiLineOutput() {
        String code = """
                public class Counter {
                    public static void main(String[] args) {
                        for (int i = 1; i <= 5; i++) {
                            System.out.println("Line " + i);
                        }
                    }
                }
                """;
        ExecutionResult result = executionService.execute(new CodeRequest("java", code));

        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getStdout().contains("Line 1"));
        assertTrue(result.getStdout().contains("Line 5"));
    }

    // ─────────────────────────────────────────────
    // Python Tests
    // ─────────────────────────────────────────────

    @Test
    void testPythonHelloWorld() {
        String code = "print('Hello from Python!')";
        ExecutionResult result = executionService.execute(new CodeRequest("python", code));

        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getStdout().contains("Hello from Python!"));
    }

    @Test
    void testPythonSyntaxError() {
        String code = "print('unclosed string)";
        ExecutionResult result = executionService.execute(new CodeRequest("python", code));

        assertEquals("ERROR", result.getStatus());
        assertFalse(result.getStderr().isEmpty());
    }

    @Test
    void testPythonArithmetic() {
        String code = "print(2 + 3 * 4)";
        ExecutionResult result = executionService.execute(new CodeRequest("python", code));

        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getStdout().trim().equals("14"));
    }

    // ─────────────────────────────────────────────
    // General / Edge Case Tests
    // ─────────────────────────────────────────────

    @Test
    void testUnsupportedLanguage() {
        ExecutionResult result = executionService.execute(new CodeRequest("cobol", "some code"));

        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getStderr().contains("Unsupported language"));
    }

    @Test
    void testExtractJavaClassName_simple() {
        String code = "public class MyApp { public static void main(String[] args){} }";
        String name = executionService.extractJavaClassName(code);
        assertEquals("MyApp", name);
    }

    @Test
    void testExtractJavaClassName_withBrace() {
        String code = "public class Solution{\n  public static void main(String[] args){}\n}";
        String name = executionService.extractJavaClassName(code);
        assertEquals("Solution", name);
    }

    @Test
    void testExtractJavaClassName_notFound() {
        String code = "class notPublic { }";
        String name = executionService.extractJavaClassName(code);
        assertNull(name);
    }

    @Test
    void testExecutionTimeIsRecorded() {
        String code = "print('fast')";
        ExecutionResult result = executionService.execute(new CodeRequest("python", code));

        assertTrue(result.getExecutionTimeMs() >= 0);
    }
}
