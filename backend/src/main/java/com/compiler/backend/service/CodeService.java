package com.compiler.backend.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.compiler.backend.model.CodeRequest;

@Service
public class CodeService {

    public Map<String, Object> execute(CodeRequest req) {
        Map<String, Object> result = new HashMap<>();
        File file = null;

        try {
            // 1. Create temp Python file
            file = File.createTempFile("code", ".py");

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(req.getCode());
            }

            // 2. Detect OS and choose python command
            String command = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "python "
                    : "python3 ";

            Process process = Runtime.getRuntime()
                    .exec(command + file.getAbsolutePath());

            // 3. Send stdin if provided
            if (req.getStdin() != null && !req.getStdin().isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(req.getStdin().getBytes());
                    os.flush();
                }
            }

            // 4. Capture output and error
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();

            try (
                BufferedReader output = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                BufferedReader error = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))
            ) {
                String line;

                while ((line = output.readLine()) != null) {
                    out.append(line).append("\n");
                }

                while ((line = error.readLine()) != null) {
                    err.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            // 5. Prepare response
            result.put("stdout", out.toString());
            result.put("stderr", err.toString());
            result.put("exitCode", exitCode);

        } catch (IOException | InterruptedException e) {
            result.put("stdout", "");
            result.put("stderr", e.getMessage());
            result.put("exitCode", 1);
        } finally {
            // 6. Cleanup temp file
            if (file != null && file.exists()) {
                file.delete();
            }
        }

        return result;
    }
}