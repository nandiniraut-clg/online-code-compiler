package com.coderunner.execution.controller;
import com.coderunner.execution.model.CodeRequest;
import com.coderunner.execution.model.ExecutionResult;
import com.coderunner.execution.service.ExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/execute")
@CrossOrigin(origins = "*")
public class ExecutionController {
    private final ExecutionService executionService;
    public ExecutionController(ExecutionService executionService) { this.executionService = executionService; }
    @PostMapping
    public ResponseEntity<ExecutionResult> executeCode(@RequestBody CodeRequest request) {
        if (request.getCode() == null || request.getCode().isBlank())
            return ResponseEntity.badRequest().body(new ExecutionResult("", "Code cannot be empty.", "ERROR", 0));
        if (request.getLanguage() == null || request.getLanguage().isBlank())
            return ResponseEntity.badRequest().body(new ExecutionResult("", "Language cannot be empty.", "ERROR", 0));
        return ResponseEntity.ok(executionService.execute(request));
    }
    @GetMapping("/health")
    public ResponseEntity<String> health() { return ResponseEntity.ok("Execution Engine is up and running!"); }
}
