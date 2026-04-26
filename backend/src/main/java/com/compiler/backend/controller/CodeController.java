package com.compiler.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compiler.backend.model.CodeRequest;
import com.compiler.backend.service.CodeService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CodeController {

    @Autowired
    private CodeService service;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody CodeRequest request) {
        try {
            Map<String, Object> result = service.execute(request);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("stdout", "");
            error.put("stderr", e.getMessage());
            error.put("exitCode", 1);
            return ResponseEntity.status(500).body(error);
        }
    }
}