# CodeRunner – Execution Engine
**Person 3 | Branch: `execution-engine`**

This module is the core of CodeRunner. It accepts source code + a language identifier, runs the code in a secure subprocess, and returns the output (stdout/stderr).

---

## 📁 Project Structure

```
execution-engine/
├── src/
│   ├── main/java/com/coderunner/execution/
│   │   ├── ExecutionEngineApplication.java   ← Spring Boot entry point
│   │   ├── controller/
│   │   │   └── ExecutionController.java      ← REST API endpoint
│   │   ├── model/
│   │   │   ├── CodeRequest.java              ← Input: language + code
│   │   │   └── ExecutionResult.java          ← Output: stdout/stderr/status
│   │   └── service/
│   │       └── ExecutionService.java         ← Core execution logic
│   └── test/java/com/coderunner/execution/
│       └── ExecutionServiceTest.java         ← JUnit 5 tests
├── pom.xml
├── Jenkinsfile
└── README.md
```

---

## 🚀 Running Locally

```bash
cd execution-engine
mvn spring-boot:run
```

Service starts on **http://localhost:8082**

---

## 🔌 API Reference

### POST `/api/execute`
Run a piece of code.

**Request Body:**
```json
{
  "language": "java",
  "code": "public class Hello { public static void main(String[] args) { System.out.println(\"Hi!\"); } }"
}
```

**Response:**
```json
{
  "stdout": "Hi!\n",
  "stderr": "",
  "status": "SUCCESS",
  "executionTimeMs": 412
}
```

Supported languages: `java`, `python`

Status values: `SUCCESS`, `ERROR`, `TIMEOUT`

---

### GET `/api/execute/health`
Returns `200 OK` if the service is running.

---

## 🧪 Running Tests

```bash
mvn test
```

Tests cover:
- Java Hello World
- Java compilation error
- Java runtime exception
- Python Hello World
- Python syntax error
- Unsupported language
- Timeout handling
- Class name extraction

---

## 🔧 DevOps Tools Used

| Tool | Usage |
|------|-------|
| **Git/GitHub** | Branch `execution-engine`, commits, PR to `main` |
| **Maven** | `pom.xml`, `mvn compile`, `mvn test`, `mvn package` |
| **JUnit 5** | 11 unit tests in `ExecutionServiceTest.java` |
| **Jenkins** | Pipeline: Checkout → Build → Test → Package → Email notify |

---

## ⚙️ Configuration

In `application.properties`:
```
execution.timeout.seconds=10   # kill process after 10s
```
