package com.compiler.history.controller;

import com.compiler.history.model.Submission;
import com.compiler.history.service.HistoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HistoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    HistoryService historyService;

    // ══════════════════════════════════════════════════════════════
    //  💾 SAVE SUBMISSION TESTS  (POST /api/history)
    // ══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/history — Save Submission")
    class SaveSubmissionTests {

        @Test
        @DisplayName("✅ Save a valid Java submission")
        void testSaveJavaSubmission() throws Exception {
            String body = """
                {
                    "language": "java",
                    "code": "public class Main { public static void main(String[] args) { System.out.println(\\"Hi\\"); } }",
                    "stdin": "",
                    "output": "Hi\\n",
                    "stderr": "",
                    "exitCode": 0
                }
                """;

            mockMvc.perform(post("/api/history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.language").value("java"))
                    .andExpect(jsonPath("$.exitCode").value(0))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("✅ Save a valid Python submission")
        void testSavePythonSubmission() throws Exception {
            String body = """
                {
                    "language": "python",
                    "code": "print('hello')",
                    "stdin": "",
                    "output": "hello\\n",
                    "stderr": "",
                    "exitCode": 0
                }
                """;

            mockMvc.perform(post("/api/history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.language").value("python"))
                    .andExpect(jsonPath("$.output").value("hello\\n"));
        }

        @Test
        @DisplayName("✅ Save a failed submission (exitCode=1, has stderr)")
        void testSaveFailedSubmission() throws Exception {
            String body = """
                {
                    "language": "python",
                    "code": "print(",
                    "stdin": "",
                    "output": "",
                    "stderr": "SyntaxError: unexpected EOF",
                    "exitCode": 1
                }
                """;

            mockMvc.perform(post("/api/history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(1))
                    .andExpect(jsonPath("$.stderr").value("SyntaxError: unexpected EOF"));
        }

        @Test
        @DisplayName("✅ Save a timeout submission (exitCode=124)")
        void testSaveTimeoutSubmission() throws Exception {
            String body = """
                {
                    "language": "java",
                    "code": "public class Main { public static void main(String[] args) { while(true){} } }",
                    "stdin": "",
                    "output": "",
                    "stderr": "Execution timed out after 10 seconds.",
                    "exitCode": 124
                }
                """;

            mockMvc.perform(post("/api/history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitCode").value(124));
        }

        @Test
        @DisplayName("✅ stdin field is optional — defaults to empty string")
        void testSaveWithoutStdin() throws Exception {
            String body = """
                {
                    "language": "c",
                    "code": "#include<stdio.h>\\nint main(){printf(\\"Hi\\");return 0;}",
                    "output": "Hi",
                    "exitCode": 0
                }
                """;

            mockMvc.perform(post("/api/history")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stdin").value(""));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  📋 GET ALL HISTORY TESTS  (GET /api/history)
    // ══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/history — Fetch All")
    class GetAllHistoryTests {

        @Test
        @DisplayName("✅ Returns empty list when no submissions saved")
        void testGetAllEmpty() throws Exception {
            mockMvc.perform(get("/api/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("✅ Returns all saved submissions")
        void testGetAllReturnsSaved() {
            historyService.saveSubmission("java",   "code1", "", "out1", "", 0);
            historyService.saveSubmission("python", "code2", "", "out2", "", 0);
            historyService.saveSubmission("c",      "code3", "", "out3", "", 0);

            List<Submission> all = historyService.getAllSubmissions();
            assertEquals(3, all.size());
        }

        @Test
        @DisplayName("✅ Results are ordered most recent first")
        void testGetAllOrderedByTimestamp() {
            historyService.saveSubmission("java",   "first",  "", "", "", 0);
            historyService.saveSubmission("python", "second", "", "", "", 0);

            List<Submission> all = historyService.getAllSubmissions();
            assertEquals("python", all.get(0).getLanguage()); // most recent first
            assertEquals("java",   all.get(1).getLanguage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  🔍 FILTER BY LANGUAGE TESTS  (GET /api/history/language/{lang})
    // ══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/history/language/{lang} — Filter by Language")
    class FilterByLanguageTests {

        @Test
        @DisplayName("✅ Returns only Java submissions")
        void testFilterJava() throws Exception {
            historyService.saveSubmission("java",   "java code",   "", "out", "", 0);
            historyService.saveSubmission("python", "python code", "", "out", "", 0);

            mockMvc.perform(get("/api/history/language/java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].language").value("java"));
        }

        @Test
        @DisplayName("✅ Language filter is case-insensitive (JAVA == java)")
        void testFilterCaseInsensitive() {
            historyService.saveSubmission("java", "code", "", "", "", 0);

            List<Submission> result = historyService.getSubmissionsByLanguage("JAVA");
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("✅ Returns empty list for language with no submissions")
        void testFilterNoMatch() throws Exception {
            historyService.saveSubmission("python", "code", "", "", "", 0);

            mockMvc.perform(get("/api/history/language/c"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("✅ Returns multiple submissions for same language")
        void testFilterMultiple() {
            historyService.saveSubmission("python", "code1", "", "", "", 0);
            historyService.saveSubmission("python", "code2", "", "", "", 0);
            historyService.saveSubmission("java",   "code3", "", "", "", 0);

            List<Submission> result = historyService.getSubmissionsByLanguage("python");
            assertEquals(2, result.size());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  📊 LANGUAGE STATS TESTS  (GET /api/history/stats)
    // ══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/history/stats — Language Statistics")
    class LanguageStatsTests {

        @Test
        @DisplayName("✅ Returns empty map when no submissions")
        void testStatsEmpty() throws Exception {
            mockMvc.perform(get("/api/history/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isMap());
        }

        @Test
        @DisplayName("✅ Correctly counts each language")
        void testStatsCorrectCounts() {
            historyService.saveSubmission("java",   "c1", "", "", "", 0);
            historyService.saveSubmission("java",   "c2", "", "", "", 0);
            historyService.saveSubmission("python", "c3", "", "", "", 0);
            historyService.saveSubmission("c",      "c4", "", "", "", 0);
            historyService.saveSubmission("c",      "c5", "", "", "", 0);
            historyService.saveSubmission("c",      "c6", "", "", "", 0);

            Map<String, Long> stats = historyService.getLanguageStats();
            assertEquals(2L, stats.get("java"));
            assertEquals(1L, stats.get("python"));
            assertEquals(3L, stats.get("c"));
        }

        @Test
        @DisplayName("✅ Stats endpoint returns 200 and JSON map")
        void testStatsEndpoint() throws Exception {
            historyService.saveSubmission("java", "code", "", "", "", 0);

            mockMvc.perform(get("/api/history/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.java").value(1));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  🔢 COUNT TESTS  (GET /api/history/count)
    // ══════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/history/count — Total Count")
    class CountTests {

        @Test
        @DisplayName("✅ Returns 0 when no submissions")
        void testCountEmpty() throws Exception {
            mockMvc.perform(get("/api/history/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("✅ Count increases with each submission")
        void testCountIncreases() {
            assertEquals(0L, historyService.getTotalCount());

            historyService.saveSubmission("java",   "c1", "", "", "", 0);
            assertEquals(1L, historyService.getTotalCount());

            historyService.saveSubmission("python", "c2", "", "", "", 0);
            assertEquals(2L, historyService.getTotalCount());
        }

        @Test
        @DisplayName("✅ Count endpoint returns correct total")
        void testCountEndpoint() throws Exception {
            historyService.saveSubmission("java",   "c1", "", "", "", 0);
            historyService.saveSubmission("python", "c2", "", "", "", 0);
            historyService.saveSubmission("c",      "c3", "", "", "", 0);

            mockMvc.perform(get("/api/history/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(3));
        }
    }
}
