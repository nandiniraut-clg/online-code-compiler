package com.compiler.backend;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.compiler.backend.model.CodeRequest;
import com.compiler.backend.service.CodeService;

class CodeServiceTest {

    CodeService service;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        service = new CodeService();
    }

    // ══════════════════════════════════════════════════════════════
    //  🐍 PYTHON TESTS
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("Python Tests")
    class PythonTests {

        @Test
        @DisplayName("✅ Python - Basic print")
        void testPythonBasicPrint() {
            CodeRequest req = build("python", "print('Hello')", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("Hello"));
            assertTrue(result.get("stderr").toString().isEmpty());
        }

        @Test
        @DisplayName("✅ Python - Read from stdin")
        void testPythonStdin() {
            CodeRequest req = build("python",
                "name = input()\nprint('Hello ' + name)", "World");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("Hello World"));
        }

        @Test
        @DisplayName("✅ Python - Multi-line output")
        void testPythonMultiLineOutput() {
            CodeRequest req = build("python",
                "for i in range(1, 4):\n    print(i)", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            String out = result.get("stdout").toString();
            assertTrue(out.contains("1"));
            assertTrue(out.contains("2"));
            assertTrue(out.contains("3"));
        }

        @Test
        @DisplayName("✅ Python - Arithmetic output")
        void testPythonArithmetic() {
            CodeRequest req = build("python", "print(2 + 3)", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("5"));
        }

        @Test
        @DisplayName("✅ Python - Multiple stdin lines")
        void testPythonMultipleStdinLines() {
            CodeRequest req = build("python",
                "a = int(input())\nb = int(input())\nprint(a + b)",
                "10\n20");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("30"));
        }

        @Test
        @DisplayName("❌ Python - Syntax error")
        void testPythonSyntaxError() {
            CodeRequest req = build("python", "print(", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(1, result.get("exitCode"));
            assertFalse(result.get("stderr").toString().isEmpty());
        }

        @Test
        @DisplayName("❌ Python - Runtime error (ZeroDivisionError)")
        void testPythonRuntimeError() {
            CodeRequest req = build("python", "print(1 / 0)", "");
            Map<String, Object> result = service.execute(req);

            assertNotEquals(0, result.get("exitCode"));
            assertTrue(result.get("stderr").toString().contains("ZeroDivisionError"));
        }

        @Test
        @DisplayName("❌ Python - Undefined variable")
        void testPythonUndefinedVariable() {
            CodeRequest req = build("python", "print(x)", "");
            Map<String, Object> result = service.execute(req);

            assertNotEquals(0, result.get("exitCode"));
            assertTrue(result.get("stderr").toString().contains("NameError"));
        }

        @Test
        @DisplayName("⏱️ Python - Infinite loop timeout")
        void testPythonTimeout() {
            CodeRequest req = build("python", "while True:\n    pass", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(124, result.get("exitCode"));
            assertTrue(result.get("stderr").toString().contains("timed out"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ☕ JAVA TESTS
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("Java Tests")
    class JavaTests {

        @Test
        @DisplayName("✅ Java - Basic print")
        void testJavaBasicPrint() {
            String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello Java");
                    }
                }
                """;
            CodeRequest req = build("java", code, "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("Hello Java"));
        }

        @Test
        @DisplayName("✅ Java - Read from stdin")
        void testJavaStdin() {
            String code = """
                import java.util.Scanner;
                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        String name = sc.nextLine();
                        System.out.println("Hello " + name);
                    }
                }
                """;
            CodeRequest req = build("java", code, "World");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("Hello World"));
        }

        @Test
        @DisplayName("✅ Java - Arithmetic output")
        void testJavaArithmetic() {
            String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(10 + 20);
                    }
                }
                """;
            CodeRequest req = build("java", code, "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("30"));
        }

        @Test
        @DisplayName("✅ Java - Multiple stdin lines")
        void testJavaMultipleStdin() {
            String code = """
                import java.util.Scanner;
                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println(a + b);
                    }
                }
                """;
            CodeRequest req = build("java", code, "15\n25");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("40"));
        }

        @Test
        @DisplayName("❌ Java - Compile error (missing semicolon)")
        void testJavaCompileError() {
            String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello")
                    }
                }
                """;
            CodeRequest req = build("java", code, "");
            Map<String, Object> result = service.execute(req);

            assertNotEquals(0, result.get("exitCode"));
            assertFalse(result.get("stderr").toString().isEmpty());
        }

        @Test
        @DisplayName("❌ Java - Runtime exception (ArrayIndexOutOfBounds)")
        void testJavaRuntimeException() {
            String code = """
                public class Main {
                    public static void main(String[] args) {
                        int[] arr = new int[3];
                        System.out.println(arr[10]);
                    }
                }
                """;
            CodeRequest req = build("java", code, "");
            Map<String, Object> result = service.execute(req);

            assertNotEquals(0, result.get("exitCode"));
            assertTrue(result.get("stderr").toString()
                .contains("ArrayIndexOutOfBoundsException"));
        }

        @Test
        @DisplayName("❌ Java - NullPointerException")
        void testJavaNullPointer() {
            String code = """
                public class Main {
                    public static void main(String[] args) {
                        String s = null;
                        System.out.println(s.length());
                    }
                }
                """;
            CodeRequest req = build("java", code, "");
            Map<String, Object> result = service.execute(req);

            assertNotEquals(0, result.get("exitCode"));
            assertTrue(result.get("stderr").toString()
                .contains("NullPointerException"));
        }

        @Test
        @DisplayName("⏱️ Java - Infinite loop timeout")
        void testJavaTimeout() {
            String code = """
                public class Main {
                    public static void main(String[] args) {
                        while (true) {}
                    }
                }
                """;
            CodeRequest req = build("java", code, "");
            Map<String, Object> result = service.execute(req);

            assertEquals(124, result.get("exitCode"));
            assertTrue(result.get("stderr").toString().contains("timed out"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  🔵 C LANGUAGE TESTS
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("C Language Tests")
    class CTests {

        @Test
        @DisplayName("✅ C - Basic print")
        void testCBasicPrint() {
            String code = """
                #include <stdio.h>
                int main() {
                    printf("Hello C\\n");
                    return 0;
                }
                """;
            CodeRequest req = build("c", code, "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("Hello C"));
        }

        @Test
        @DisplayName("✅ C - Read from stdin")
        void testCStdin() {
            String code = """
                #include <stdio.h>
                int main() {
                    char name[50];
                    scanf("%s", name);
                    printf("Hello %s\\n", name);
                    return 0;
                }
                """;
            CodeRequest req = build("c", code, "World");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("Hello World"));
        }

        @Test
        @DisplayName("✅ C - Arithmetic output")
        void testCArithmetic() {
            String code = """
                #include <stdio.h>
                int main() {
                    printf("%d\\n", 10 + 20);
                    return 0;
                }
                """;
            CodeRequest req = build("c", code, "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("30"));
        }

        @Test
        @DisplayName("✅ C - Multiple stdin values")
        void testCMultipleStdin() {
            String code = """
                #include <stdio.h>
                int main() {
                    int a, b;
                    scanf("%d %d", &a, &b);
                    printf("%d\\n", a + b);
                    return 0;
                }
                """;
            CodeRequest req = build("c", code, "15 25");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("40"));
        }

        @Test
        @DisplayName("❌ C - Compile error (missing semicolon)")
        void testCCompileError() {
            String code = """
                #include <stdio.h>
                int main() {
                    printf("Hello")
                    return 0;
                }
                """;
            CodeRequest req = build("c", code, "");
            Map<String, Object> result = service.execute(req);

            assertNotEquals(0, result.get("exitCode"));
            assertFalse(result.get("stderr").toString().isEmpty());
        }

        @Test
        @DisplayName("❌ C - Undeclared variable")
        void testCUndeclaredVariable() {
            String code = """
                #include <stdio.h>
                int main() {
                    printf("%d\\n", x);
                    return 0;
                }
                """;
            CodeRequest req = build("c", code, "");
            Map<String, Object> result = service.execute(req);

            assertNotEquals(0, result.get("exitCode"));
            assertFalse(result.get("stderr").toString().isEmpty());
        }

        @Test
        @DisplayName("⏱️ C - Infinite loop timeout")
        void testCTimeout() {
            String code = """
                #include <stdio.h>
                int main() {
                    while(1) {}
                    return 0;
                }
                """;
            CodeRequest req = build("c", code, "");
            Map<String, Object> result = service.execute(req);

            assertEquals(124, result.get("exitCode"));
            assertTrue(result.get("stderr").toString().contains("timed out"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ❌ UNSUPPORTED LANGUAGE TESTS
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("Unsupported Language Tests")
    class UnsupportedLanguageTests {

        @Test
        @DisplayName("❌ Ruby - Not supported")
        void testRubyUnsupported() {
            CodeRequest req = build("ruby", "puts 'Hello'", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(1, result.get("exitCode"));
            assertTrue(result.get("stderr").toString().contains("Unsupported language"));
        }

        @Test
        @DisplayName("❌ JavaScript - Not supported")
        void testJavaScriptUnsupported() {
            CodeRequest req = build("javascript", "console.log('Hi')", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(1, result.get("exitCode"));
            assertTrue(result.get("stderr").toString().contains("Unsupported language"));
        }

        @Test
        @DisplayName("❌ Empty language string")
        void testEmptyLanguage() {
            CodeRequest req = build("", "print('Hello')", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(1, result.get("exitCode"));
        }

        @Test
        @DisplayName("❌ Random/garbage language")
        void testGarbageLanguage() {
            CodeRequest req = build("xyz123", "some code", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(1, result.get("exitCode"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  🔑 EDGE CASE TESTS
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("✅ Empty output (no print statement)")
        void testEmptyOutput() {
            CodeRequest req = build("python", "x = 1 + 1", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().isEmpty()
                || result.get("stdout").toString().isBlank());
        }

        @Test
        @DisplayName("✅ Python - Language case insensitive (PYTHON)")
        void testCaseInsensitiveLanguage() {
            CodeRequest req = build("PYTHON", "print('case test')", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("case test"));
        }

        @Test
        @DisplayName("✅ Python - Mixed case language (Python)")
        void testMixedCaseLanguage() {
            CodeRequest req = build("Python", "print('mixed')", "");
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("mixed"));
        }

        @Test
        @DisplayName("✅ Result map always contains all 3 keys")
        void testResultMapAlwaysHasKeys() {
            CodeRequest req = build("python", "print('keys test')", "");
            Map<String, Object> result = service.execute(req);

            assertTrue(result.containsKey("stdout"));
            assertTrue(result.containsKey("stderr"));
            assertTrue(result.containsKey("exitCode"));
        }

        @Test
        @DisplayName("✅ Result map keys present even on unsupported language")
        void testResultMapKeysOnUnsupported() {
            CodeRequest req = build("cobol", "DISPLAY 'HI'", "");
            Map<String, Object> result = service.execute(req);

            assertTrue(result.containsKey("stdout"));
            assertTrue(result.containsKey("stderr"));
            assertTrue(result.containsKey("exitCode"));
        }

        @Test
        @DisplayName("✅ Null stdin treated as empty")
        void testNullStdin() {
            CodeRequest req = build("python", "print('no stdin')", null);
            Map<String, Object> result = service.execute(req);

            assertEquals(0, result.get("exitCode"));
            assertTrue(result.get("stdout").toString().contains("no stdin"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  🛠️ HELPER
    // ══════════════════════════════════════════════════════════════
    private CodeRequest build(String lang, String code, String stdin) {
        CodeRequest req = new CodeRequest();
        req.setLanguage(lang);
        req.setCode(code);
        req.setStdin(stdin);
        return req;
    }
}
