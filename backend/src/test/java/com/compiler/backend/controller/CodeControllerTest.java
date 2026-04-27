package com.compiler.backend.controller;

import com.compiler.backend.model.CodeRequest;
import com.compiler.backend.service.CodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CodeController.class)   // loads ONLY the controller layer
class CodeControllerTest {

    private static final MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.APPLICATION_JSON);

    @Autowired
    MockMvc mockMvc;                // performs HTTP calls without real server

    @MockitoBean
    CodeService service;            // fake CodeService — no real execution

    // ─────────────────────────────────────────
    // Helper: build fake service response
    // ─────────────────────────────────────────
    private Map<String, Object> fakeResult(String stdout, String stderr, int exitCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("stdout",   stdout);
        result.put("stderr",   stderr);
        result.put("exitCode", exitCode);
        return result;
    }

    // ══════════════════════════════════════════════════════════════
    //  ✅ SUCCESS CASES
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("Success Cases")
    class SuccessCases {

        @Test
        @DisplayName("POST /api/execute → returns stdout from service")
        void testExecuteReturnsMockedResponse() throws Exception {
            when(service.execute(any(CodeRequest.class)))
                    .thenReturn(fakeResult("Hello Python", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {
                                  "language": "python",
                                  "code": "print('Hello Python')",
                                  "stdin": ""
                                }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stdout").value("Hello Python"))
                    .andExpect(jsonPath("$.stderr").value(""))
                    .andExpect(jsonPath("$.exitCode").value(0));

            // verify service was called exactly once
            verify(service, times(1)).execute(any(CodeRequest.class));
        }

        @Test
        @DisplayName("Python execution success")
        void testPythonSuccess() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("Hello\n", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"print('Hello')","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(0))
                    .andExpect(jsonPath("$.stdout").value("Hello\n"));
        }

        @Test
        @DisplayName("Java execution success")
        void testJavaSuccess() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("Hello Java\n", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"java","code":"public class Main { public static void main(String[] args) { System.out.println(\\"Hello Java\\"); } }","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(0))
                    .andExpect(jsonPath("$.stdout").value("Hello Java\n"));
        }

        @Test
        @DisplayName("C execution success")
        void testCSuccess() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("Hello C\n", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"c","code":"#include<stdio.h>\\nint main(){printf(\\"Hello C\\\\n\\");return 0;}","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(0))
                    .andExpect(jsonPath("$.stdout").value("Hello C\n"));
        }

        @Test
        @DisplayName("Stdin is passed through to service")
        void testStdinPassedThrough() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("Hello World\n", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"print(input())","stdin":"World"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stdout").value("Hello World\n"));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ❌ ERROR CASES
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("Error Cases")
    class ErrorCases {

        @Test
        @DisplayName("Syntax error → stderr not empty, exitCode = 1")
        void testSyntaxError() throws Exception {
            when(service.execute(any()))
                    .thenReturn(fakeResult("", "SyntaxError: invalid syntax", 1));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"print(","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(1))
                    .andExpect(jsonPath("$.stderr").value("SyntaxError: invalid syntax"));
        }

        @Test
        @DisplayName("Compile error (Java) → stderr contains error")
        void testJavaCompileError() throws Exception {
            when(service.execute(any()))
                    .thenReturn(fakeResult("", "Main.java:1: error: ';' expected", 1));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"java","code":"public class Main { }","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(1))
                    .andExpect(jsonPath("$.stderr").value("Main.java:1: error: ';' expected"));
        }

        @Test
        @DisplayName("Unsupported language → exitCode = 1")
        void testUnsupportedLanguage() throws Exception {
            when(service.execute(any()))
                    .thenReturn(fakeResult("", "Unsupported language: ruby", 1));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"ruby","code":"puts 'Hi'","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(1))
                    .andExpect(jsonPath("$.stderr").value("Unsupported language: ruby"));
        }

        @Test
        @DisplayName("Timeout → exitCode = 124")
        void testTimeout() throws Exception {
            when(service.execute(any()))
                    .thenReturn(fakeResult("", "Execution timed out after 10 seconds.", 124));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"while True: pass","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(124))
                    .andExpect(jsonPath("$.stderr").value("Execution timed out after 10 seconds."));
        }

        @Test
        @DisplayName("Service throws exception → 500 with structured JSON body")
        void testServiceThrowsException() throws Exception {
            when(service.execute(any()))
                    .thenThrow(new RuntimeException("Unexpected server error"));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"print('hi')","stdin":""}
                            """))
                    .andExpect(status().isInternalServerError())       // 500
                    .andExpect(jsonPath("$.exitCode").value(1))        // structured body
                    .andExpect(jsonPath("$.stderr").value("Unexpected server error"))
                    .andExpect(jsonPath("$.stdout").value(""));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  🔑 EDGE CASES
    // ══════════════════════════════════════════════════════════════
    @Nested
    @SuppressWarnings("unused")
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Response always contains stdout, stderr, exitCode keys")
        void testResponseAlwaysHasAllKeys() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"x=1","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stdout").exists())
                    .andExpect(jsonPath("$.stderr").exists())
                    .andExpect(jsonPath("$.exitCode").exists());
        }

        @Test
        @DisplayName("Service called exactly once per request")
        void testServiceCalledOnce() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("ok", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"print('ok')","stdin":""}
                            """));

            verify(service, times(1)).execute(any(CodeRequest.class));
            verifyNoMoreInteractions(service);
        }

        @Test
        @DisplayName("Content-Type must be application/json")
        void testContentTypeJson() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("ok", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"python","code":"print('ok')","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
        }

        @Test
        @DisplayName("Case insensitive language (PYTHON) handled by service")
        void testCaseInsensitiveLanguage() throws Exception {
            when(service.execute(any())).thenReturn(fakeResult("ok", "", 0));

            mockMvc.perform(post("/api/execute")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                {"language":"PYTHON","code":"print('ok')","stdin":""}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(0));
        }
    }
}
