package com.compiler.backend.service;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.compiler.backend.model.CodeRequest;

@Service
public class CodeService {

    private static final int TIMEOUT_SECONDS = 10;

    public Map<String, Object> execute(CodeRequest req) {
        Map<String, Object> result = new HashMap<>();
        File tempFile = null;

        try {
            String lang = req.getLanguage().toLowerCase();
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            Process process;

            // ─────────────────────────────────────────
            // 🐍 PYTHON
            // ─────────────────────────────────────────
            if (lang.equals("python")) {
                tempFile = File.createTempFile("code", ".py");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(req.getCode());
                }

                String pythonCmd = isWindows ? "python" : "python3";
                process = new ProcessBuilder(pythonCmd, tempFile.getAbsolutePath())
                        .start();
            }

            // ─────────────────────────────────────────
            // ☕ JAVA
            // ─────────────────────────────────────────
            else if (lang.equals("java")) {
                File dir = new File(System.getProperty("java.io.tmpdir"), "javacode_" + Thread.currentThread().getId());
                dir.mkdirs();
                tempFile = new File(dir, "Main.java");

                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(req.getCode());
                }

                // Compile
                Process compile = new ProcessBuilder("javac", "Main.java")
                        .directory(dir)
                        .start();

                String compileErr = new String(compile.getErrorStream().readAllBytes());
                compile.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (compile.exitValue() != 0) {
                    result.put("stdout", "");
                    result.put("stderr", compileErr);
                    result.put("exitCode", compile.exitValue());
                    return result;
                }

                // Run
                process = new ProcessBuilder("java", "Main")
                        .directory(dir)
                        .start();
            }

            // ─────────────────────────────────────────
            // 🔵 C LANGUAGE
            // ─────────────────────────────────────────
            else if (lang.equals("c")) {
                // Timestamp-based dir avoids collision during parallel tests
                File dir = new File(System.getProperty("java.io.tmpdir"),
                        "ccode_" + System.currentTimeMillis());
                dir.mkdirs();
                tempFile = new File(dir, "code.c");

                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(req.getCode());
                }

                // Use absolute paths for both source and output binary
                String exeName = isWindows ? "code.exe" : "code.out";
                File   exeFile = new File(dir, exeName);

                // Resolve gcc: GCC_PATH env var → MSYS2 default → plain "gcc"
                String gccPath = System.getenv("GCC_PATH");
                if (gccPath == null || gccPath.isBlank()) {
                    // Common MSYS2 install locations on Windows
                    String[] msys2Candidates = {
                        "E:\\mysys\\mingw64\\bin\\gcc.exe",  // your install path
                        "C:\\msys64\\mingw64\\bin\\gcc.exe",
                        "C:\\msys64\\ucrt64\\bin\\gcc.exe",
                        "C:\\msys2\\mingw64\\bin\\gcc.exe",
                        "C:\\mingw64\\bin\\gcc.exe"
                    };
                    gccPath = "gcc"; // default fallback
                    if (isWindows) {
                        for (String candidate : msys2Candidates) {
                            if (new File(candidate).exists()) {
                                gccPath = candidate;
                                break;
                            }
                        }
                    }
                }

                // Compile: absolute paths eliminate any cwd/PATH ambiguity
                Process compile = new ProcessBuilder(
                            gccPath,
                            tempFile.getAbsolutePath(),  // absolute source path
                            "-o",
                            exeFile.getAbsolutePath()    // absolute output path
                        )
                        .directory(dir)
                        .start();

                String compileErr = new String(compile.getErrorStream().readAllBytes());
                boolean compileFinished = compile.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (!compileFinished) {
                    compile.destroyForcibly();
                    result.put("stdout", "");
                    result.put("stderr", "Compilation timed out.");
                    result.put("exitCode", 1);
                    return result;
                }

                if (compile.exitValue() != 0) {
                    result.put("stdout", "");
                    result.put("stderr", "Compilation Failed:\n" + compileErr);
                    result.put("exitCode", compile.exitValue());
                    return result;
                }

                // Run using the absolute path to binary — no PATH lookup, no VS Code conflict
                process = new ProcessBuilder(exeFile.getAbsolutePath())
                        .directory(dir)
                        .start();
            }

            // ─────────────────────────────────────────
            // ❌ Unsupported language
            // ─────────────────────────────────────────
            else {
                result.put("stdout", "");
                result.put("stderr", "Unsupported language: " + req.getLanguage());
                result.put("exitCode", 1);
                return result;
            }

            // ─────────────────────────────────────────
            // 📥 Handle stdin
            // ─────────────────────────────────────────
            if (req.getStdin() != null && !req.getStdin().isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(req.getStdin().getBytes());
                    os.flush();
                }
            }

            // ─────────────────────────────────────────
            // ⏱️ Timeout guard FIRST
            // ─────────────────────────────────────────
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.put("stdout", "");
                result.put("stderr", "Execution timed out after " + TIMEOUT_SECONDS + " seconds.");
                result.put("exitCode", 124);
                return result;
            }

            // ─────────────────────────────────────────
            // 📤 Read output AFTER process has exited
            //    Process is already dead so buffers are
            //    small — readAllBytes() is safe here
            // ─────────────────────────────────────────
            String stdout = new String(process.getInputStream().readAllBytes());
            String stderr = new String(process.getErrorStream().readAllBytes());

            result.put("stdout", stdout);
            result.put("stderr", stderr);
            result.put("exitCode", process.exitValue());

        } catch (Exception e) {
            result.put("stdout", "");
            result.put("stderr", e.getMessage());
            result.put("exitCode", 1);

        } finally {
            // ─────────────────────────────────────────
            // 🧹 Clean up temp files
            // ─────────────────────────────────────────
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }

        return result;
    }
}