package com.compiler.history.repository;

import com.compiler.history.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // Filter by language (e.g. GET /api/history?language=java)
    List<Submission> findByLanguageIgnoreCase(String language);

    // For stats: get distinct language names
    // (we'll count in the service layer — simpler and testable)
}
