package com.compiler.history.service;

import com.compiler.history.model.Submission;
import com.compiler.history.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * Save a submission sent by the API Gateway (Person 2) after code execution.
     * Fields mirror CodeRequest (language, code, stdin) + ExecutionResult (output, stderr, exitCode).
     */
    public Submission saveSubmission(String language, String code, String stdin,
                                     String output, String stderr, Integer exitCode) {
        Submission submission = new Submission(
                null,
                language,
                code,
                stdin   != null ? stdin  : "",
                output  != null ? output : "",
                stderr  != null ? stderr : "",
                exitCode != null ? exitCode : -1,
                LocalDateTime.now()
        );
        Submission saved = submissionRepository.save(submission);
        log.info("Saved submission id={} language={} exitCode={}", saved.getId(), language, exitCode);
        return saved;
    }

    /**
     * Return all past submissions, most recent first.
     */
    public List<Submission> getAllSubmissions() {
        return submissionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Submission::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Return submissions filtered by language (case-insensitive).
     */
    public List<Submission> getSubmissionsByLanguage(String language) {
        return submissionRepository.findByLanguageIgnoreCase(language);
    }

    /**
     * Return count of submissions per language — for stats/charts on the frontend.
     * Example: { "java": 5, "python": 3, "c": 2 }
     */
    public Map<String, Long> getLanguageStats() {
        return submissionRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        s -> s.getLanguage().toLowerCase(),
                        Collectors.counting()
                ));
    }

    /**
     * Return total number of submissions logged.
     */
    public long getTotalCount() {
        return submissionRepository.count();
    }
}
