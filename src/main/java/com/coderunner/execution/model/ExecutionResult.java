package com.coderunner.execution.model;
public class ExecutionResult {
    private String stdout, stderr, status;
    private long executionTimeMs;
    public ExecutionResult() {}
    public ExecutionResult(String stdout, String stderr, String status, long executionTimeMs) {
        this.stdout = stdout; this.stderr = stderr; this.status = status; this.executionTimeMs = executionTimeMs;
    }
    public String getStdout() { return stdout; }
    public void setStdout(String s) { this.stdout = s; }
    public String getStderr() { return stderr; }
    public void setStderr(String s) { this.stderr = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long ms) { this.executionTimeMs = ms; }
}
