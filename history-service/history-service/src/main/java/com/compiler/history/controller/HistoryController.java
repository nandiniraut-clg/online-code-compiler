package com.compiler.history.controller;

import com.compiler.history.model.Submission;
import com.compiler.history.service.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*") // Allow calls from frontend (Person 1) and API Gateway (Person 2)
public class HistoryController {

    private static final Logger log = LoggerFactory.getLogger(HistoryController.class);

    @Autowired
    private HistoryService historyService;

    // ──────────────────────────────────────────────────────────
    //  POST /api/history
    //  Called by Person 2 (API Gateway) after every execution.
    //
    //  Request body:
    //  {
    //    "language": "java",
    //    "code":     "public class Main { ... }",
    //    "stdin":    "",
    //    "output":   "Hello World\n",
    //    "stderr":   "",
    //    "exitCode": 0
    //  }
    // ──────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<Submission> saveSubmission(@RequestBody Map<String, Object> request) {
        String  language = (String)  request.get("language");
        String  code     = (String)  request.get("code");
        String  stdin    = (String)  request.getOrDefault("stdin",   "");
        String  output   = (String)  request.getOrDefault("output",  "");
        String  stderr   = (String)  request.getOrDefault("stderr",  "");
        Integer exitCode = request.get("exitCode") != null
                ? Integer.parseInt(request.get("exitCode").toString()) : 0;

        log.info("Received submission: language={} exitCode={}", language, exitCode);
        Submission saved = historyService.saveSubmission(language, code, stdin, output, stderr, exitCode);
        return ResponseEntity.ok(saved);
    }

    // ──────────────────────────────────────────────────────────
    //  GET /api/history
    //  Returns all past submissions (most recent first).
    // ──────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Submission>> getAllSubmissions() {
        log.info("Fetching all submissions");
        return ResponseEntity.ok(historyService.getAllSubmissions());
    }

    // ──────────────────────────────────────────────────────────
    //  GET /api/history/language/{lang}
    //  Returns submissions for a specific language.
    //  e.g. GET /api/history/language/java
    // ──────────────────────────────────────────────────────────
    @GetMapping("/language/{lang}")
    public ResponseEntity<List<Submission>> getByLanguage(@PathVariable String lang) {
        log.info("Fetching submissions for language={}", lang);
        return ResponseEntity.ok(historyService.getSubmissionsByLanguage(lang));
    }

    // ──────────────────────────────────────────────────────────
    //  GET /api/history/stats
    //  Returns submission count per language.
    //  e.g. { "java": 5, "python": 3, "c": 2 }
    // ──────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getLanguageStats() {
        log.info("Fetching language stats");
        return ResponseEntity.ok(historyService.getLanguageStats());
    }

    // ──────────────────────────────────────────────────────────
    //  GET /api/history/count
    //  Returns total number of submissions ever logged.
    // ──────────────────────────────────────────────────────────
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalCount() {
        return ResponseEntity.ok(Map.of("total", historyService.getTotalCount()));
    }
}
