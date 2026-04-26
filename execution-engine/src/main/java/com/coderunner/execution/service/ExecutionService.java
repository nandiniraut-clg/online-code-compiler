package com.coderunner.execution.service;

import com.coderunner.execution.model.CodeRequest;
import com.coderunner.execution.model.ExecutionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

@Service
public class ExecutionService {

    @Value("${execution.timeout.seconds:10}")
    private int timeoutSeconds;

    /**
     * Main entry point. Routes to the right executor based on language.
     */
    public ExecutionResult execute(CodeRequest request) {
        String lang = request.getLanguage().toLowerCase().trim();

        return switch (lang) {
            case "java"   -> executeJava(request.getCode());
            case "python" -> executePython(request.getCode());
            default       -> new ExecutionResult("", "Unsupported language: " + lang, "ERROR", 0);
        };
    }

    // ─────────────────────────────────────────────
    // JAVA EXECUTION
    // ─────────────────────────────────────────────

    private ExecutionResult executeJava(String code) {
        Path tempDir = null;
        try {
            // 1. Create a temp directory to hold the .java file
            tempDir = Files.createTempDirectory("coderunner_java_");

            // 2. Extract class name from code (must match filename)
            String className = extractJavaClassName(code);
            if (className == null) {
                return new ExecutionResult("", "Could not find public class name in your Java code.", "ERROR", 0);
            }

            // 3. Write code to ClassName.java
            Path sourceFile = tempDir.resolve(className + ".java");
            Files.writeString(sourceFile, code);

            // 4. Compile using javac
            ExecutionResult compileResult = runProcess(
                new String[]{"javac", sourceFile.toString()},
                tempDir, timeoutSeconds
            );
            if (!compileResult.getStderr().isEmpty()) {
                // Compilation failed
                return new ExecutionResult("", compileResult.getStderr(), "ERROR", compileResult.getExecutionTimeMs());
            }

            // 5. Run the compiled class
            ExecutionResult runResult = runProcess(
                new String[]{"java", "-cp", tempDir.toString(), className},
                tempDir, timeoutSeconds
            );
            return runResult;

        } catch (Exception e) {
            return new ExecutionResult("", "Internal error: " + e.getMessage(), "ERROR", 0);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    /**
     * Extracts the public class name from Java source code.
     * Looks for: public class SomeName
     */
    String extractJavaClassName(String code) {
        for (String line : code.split("\n")) {
            line = line.trim();
            if (line.startsWith("public class ")) {
                String[] parts = line.split("\\s+");
                // parts[0]="public", parts[1]="class", parts[2]="ClassName"
                if (parts.length >= 3) {
                    // Remove any trailing { or other chars
                    return parts[2].replace("{", "").trim();
                }
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // PYTHON EXECUTION
    // ─────────────────────────────────────────────

    private ExecutionResult executePython(String code) {
        Path tempDir = null;
        try {
            // 1. Write code to a temp .py file
            tempDir = Files.createTempDirectory("coderunner_python_");
            Path sourceFile = tempDir.resolve("main.py");
            Files.writeString(sourceFile, code);

            // 2. Run python3 directly
            return runProcess(
                new String[]{"python3", sourceFile.toString()},
                tempDir, timeoutSeconds
            );

        } catch (Exception e) {
            return new ExecutionResult("", "Internal error: " + e.getMessage(), "ERROR", 0);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    // ─────────────────────────────────────────────
    // SHARED PROCESS RUNNER
    // ─────────────────────────────────────────────

    /**
     * Runs any command as a subprocess, captures stdout/stderr,
     * enforces a timeout, returns an ExecutionResult.
     */
    ExecutionResult runProcess(String[] command, Path workingDir, int timeoutSec) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(false); // keep stdout and stderr separate

        Process process = pb.start();

        // Read stdout and stderr in parallel to avoid blocking
        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdoutBuilder.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderrBuilder.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        stdoutReader.start();
        stderrReader.start();

        // Wait with timeout
        boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        if (!finished) {
            process.destroyForcibly();
            return new ExecutionResult("", "Execution timed out after " + timeoutSec + " seconds.", "TIMEOUT", elapsed);
        }

        stdoutReader.join();
        stderrReader.join();

        String stdout = stdoutBuilder.toString();
        String stderr = stderrBuilder.toString();
        String status = stderr.isEmpty() ? "SUCCESS" : "ERROR";

        return new ExecutionResult(stdout, stderr, status, elapsed);
    }

    // ─────────────────────────────────────────────
    // CLEANUP
    // ─────────────────────────────────────────────

    private void deleteTempDir(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                .sorted((a, b) -> -a.compareTo(b)) // delete files before directories
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
    }
}
