package com.compiler.backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.compiler.backend.model.CodeRequest;
import com.compiler.backend.service.CodeService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // IMPORTANT for frontend
public class CodeController {

    @Autowired
    private CodeService service;

    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody CodeRequest request) {
        return service.execute(request);
    }
}