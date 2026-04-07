package com.compiler.online_code_compiler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.compiler.online_code_compiler.model.CodeRequest;
import com.compiler.online_code_compiler.service.CodeService;

@RestController
public class CodeController {
    @Autowired
    private CodeService codeService;

    @PostMapping("/run")
    public String runCode(@RequestBody CodeRequest request) {
        return codeService.executeCode(request.getCode(), request.getLanguage());
    }

    @GetMapping("/run")
public String testRun() {
    return "Run API working!";
}
}
