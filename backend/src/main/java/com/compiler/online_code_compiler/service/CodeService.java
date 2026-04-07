package com.compiler.online_code_compiler.service;

import org.springframework.stereotype.Service;

@Service
public class CodeService {
    public String executeCode(String code, String language) {

        // For now, just simulate execution
        return "Code received for " + language + ":\n" + code;
    }
}
