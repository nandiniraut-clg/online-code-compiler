# Online Code Compiler – Backend

## Features
- REST API using Spring Boot
- Supports Python, Java, C execution
- Returns stdout, stderr, exitCode
- Unit testing using JUnit + Mockito
- CI/CD using Jenkins

## API Endpoint
POST /api/execute

## Request
{
  "language": "python",
  "code": "print('Hello')",
  "stdin": ""
}

## Response
{
  "stdout": "Hello",
  "stderr": "",
  "exitCode": 0
}

## Tech Stack
- Java (Spring Boot)
- Maven
- JUnit + Mockito
- Jenkins
