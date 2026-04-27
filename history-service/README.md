# CodeRunner — History Service Module
### DevOps Mini Project | Person 4

---

## 📁 Module Structure

```
history-service/
├── src/
│   ├── main/
│   │   ├── java/com/compiler/history/
│   │   │   ├── HistoryServiceApplication.java
│   │   │   ├── model/Submission.java
│   │   │   ├── repository/SubmissionRepository.java
│   │   │   ├── service/HistoryService.java
│   │   │   └── controller/HistoryController.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/compiler/history/
│           ├── HistoryServiceApplicationTests.java
│           └── controller/HistoryControllerTest.java
├── pom.xml
├── Jenkinsfile
└── README.md
```

---

## ⚙️ Description

Stores and serves all past code submissions from the online compiler.

**Features:**
- Log every submission (language, code, stdin, output, stderr, exitCode, timestamp)
- Fetch full submission history
- Filter history by language
- Language usage statistics
- H2 in-memory database (no setup required)

---

## 🔗 REST API

| Method | Endpoint                         | Description                        |
|--------|----------------------------------|------------------------------------|
| POST   | `/api/history`                   | Save a submission (called by Person 2) |
| GET    | `/api/history`                   | Get all submissions (recent first) |
| GET    | `/api/history/language/{lang}`   | Filter by language                 |
| GET    | `/api/history/stats`             | Submission count per language      |
| GET    | `/api/history/count`             | Total submission count             |

### POST /api/history — Request Body
```json
{
  "language": "java",
  "code":     "public class Main { ... }",
  "stdin":    "",
  "output":   "Hello World\n",
  "stderr":   "",
  "exitCode": 0
}
```

---

## 🚀 Running Locally

```bash
cd history-service
mvn spring-boot:run
```

Service starts on **http://localhost:8082**

H2 Console (for debugging): **http://localhost:8082/h2-console**
- JDBC URL: `jdbc:h2:mem:historydb`
- Username: `sa` | Password: *(empty)*

---

## 🧪 Running Tests

```bash
mvn test
```

---

## 🔧 Maven Commands

```bash
mvn clean          # Clean build artifacts
mvn test           # Run all JUnit tests
mvn package        # Build JAR
mvn spring-boot:run # Start the service
```

---

## 🔁 Integration with Other Modules

Person 2 (API Gateway) should call `POST /api/history` after every execution:

```java
// In Person 2's CodeController, after calling the execution engine:
restTemplate.postForObject("http://localhost:8082/api/history", requestBody, String.class);
```
