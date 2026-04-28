# CodeRunner — Frontend Module
**DevOps Mini Project**

---

## Module Structure

frontend/
├── src/
│   └── main/
│       └── webapp/
│           ├── index.html
│           ├── css/
│           └── js/
├── test/
├── pom.xml
├── Jenkinsfile
└── README.md

---

## Description

This module provides the **frontend UI** for an online code compiler.

### Features:
- Select programming language (Java, Python, C, C++)
- Write code in browser
- Execute code via backend API
- Display output and errors

---

## API Used

POST `/api/execute`

### Request:
```json
{
  "language": "java",
  "code": "public class Main {...}",
  "stdin": ""
}
