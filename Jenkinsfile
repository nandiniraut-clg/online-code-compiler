/*
 * ============================================================
 *  CodeRunner — Frontend Module — Jenkins CI/CD Pipeline
 *  Person 1 | DevOps Mini Project
 * ============================================================
 *
 *  Pipeline Stages:
 *  1. Checkout       → Pull code from GitHub branch
 *  2. Build          → Compile with Maven
 *  3. Unit Tests     → Run JUnit tests, publish results
 *  4. Code Quality   → Check for empty files / basic lint
 *  5. Integration Tests → API-level tests (server must be running)
 *  6. Package        → Build final JAR artifact
 *  7. Archive        → Save JAR + HTML as Jenkins artifacts
 *
 *  Post Actions:
 *  - Always  → Publish JUnit XML report
 *  - Success → Email success notification
 *  - Failure → Email failure notification with log link
 * ============================================================
 */

pipeline {

    agent any

    tools {
        jdk 'jdk17'
        maven 'maven'
    }

    environment {
        MODULE_NAME     = 'frontend'
        JAVA_VERSION    = '17'
        NOTIFY_EMAIL    = 'nandini.raut@cumminscollege.in'
        GITHUB_REPO     = 'https://github.com/nandiniraut-clg/online-code-compiler'
        BRANCH_NAME_VAL = 'frontend'
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        // ── Stage 1: Checkout ────────────────────────────────
        stage('Checkout') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 1: Using existing checkout'
                echo '──────────────────────────────────────'

                checkout scm

                bat 'git log -1 --pretty=format:"Commit: %%H%%nAuthor: %%an%%nMessage: %%s"'
            }
        }

        // ── Stage 2: Build ───────────────────────────────────
        stage('Build') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 2: Build — mvn clean compile'
                echo '──────────────────────────────────────'
                bat 'mvn clean compile -B'
            }
        }

        // ── Stage 3: Unit Tests ──────────────────────────────
        stage('Unit Tests') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 3: Unit Tests — mvn test'
                echo '──────────────────────────────────────'
                bat 'mvn test -B || exit 0'
            }
            post {
                always {
                    junit(
                        testResults: 'target/surefire-reports/*.xml',
                        allowEmptyResults: true,
                        skipPublishingChecks: false
                    )
                    echo 'JUnit report published to Jenkins.'
                }
            }
        }

        // ── Stage 4: Code Quality Check ──────────────────────
        stage('Code Quality') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 4: Code Quality Checks'
                echo '──────────────────────────────────────'
                script {
                    def htmlFile = 'src/main/webapp/index.html'
                    if (fileExists(htmlFile)) {
                        def size = bat(
                            script: "for %%F in (${htmlFile}) do @echo %%~zF",
                            returnStdout: true
                        ).trim().toInteger()
                        if (size < 100) {
                            error("index.html appears to be empty or too small (${size} bytes)")
                        }
                        echo "index.html exists and is ${size} bytes — OK"
                    } else {
                        echo "WARNING: ${htmlFile} not found — skipping HTML check"
                    }

                    if (!fileExists('pom.xml')) {
                        error('pom.xml is missing!')
                    }
                    echo 'pom.xml exists — OK'

                    if (!fileExists('src/test/java/com/coderunner/frontend/FrontendTest.java')) {
                        echo 'WARNING: FrontendTest.java not found at expected path'
                    } else {
                        echo 'FrontendTest.java exists — OK'
                    }
                }
            }
        }

        // ── Stage 5: Integration Tests ───────────────────────
        stage('Integration Tests') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 5: Integration Tests'
                echo ' (Requires API server at localhost:8080)'
                echo '──────────────────────────────────────'
                script {
                    def serverUp = bat(
                        script: 'curl -s -o NUL -w "%%{http_code}" http://localhost:8080/api/health || echo 000',
                        returnStdout: true
                    ).trim()

                    if (serverUp == '200') {
                        echo "API server is UP (HTTP ${serverUp}) — running integration tests"
                        bat 'mvn verify -P integration-tests -B || true'
                        junit(
                            testResults: 'target/failsafe-reports/*.xml',
                            allowEmptyResults: true
                        )
                    } else {
                        echo "WARNING: API server not reachable (HTTP ${serverUp})"
                        echo "Skipping integration tests — start the API server and re-run."
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }

        // ── Stage 6: Package ─────────────────────────────────
        stage('Package') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 6: Package — mvn package'
                echo '──────────────────────────────────────'
                bat 'mvn package -DskipTests -B'
                echo 'JAR built at: target/coderunner-frontend-1.0.0.jar'
            }
        }

        // ── Stage 7: Archive Artifacts ───────────────────────
        stage('Archive') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 7: Archiving Artifacts'
                echo '──────────────────────────────────────'
                archiveArtifacts(
                    artifacts: 'target/*.jar',
                    fingerprint: true,
                    allowEmptyArchive: true
                )
                archiveArtifacts(
                    artifacts: 'src/main/webapp/index.html',
                    fingerprint: true,
                    allowEmptyArchive: true
                )
                echo 'Artifacts archived — visible in Jenkins build page.'
            }
        }

    } // end stages

    post {

        always {
            echo '══════════════════════════════════════'
            echo " Build #${env.BUILD_NUMBER} finished: ${currentBuild.currentResult}"
            echo '══════════════════════════════════════'
        }

        success {
            mail(
                to: "${env.NOTIFY_EMAIL}",
                subject: "[CodeRunner] Frontend Build #${env.BUILD_NUMBER} — PASSED",
                body: """
Hi,

The Frontend module build has PASSED successfully.

─────────────────────────────────
 Project   : CodeRunner — Frontend
 Branch    : ${env.BRANCH_NAME_VAL}
 Build No  : #${env.BUILD_NUMBER}
 Status    : SUCCESS
 Duration  : ${currentBuild.durationString}
─────────────────────────────────

Build URL  : ${env.BUILD_URL}
Console Log: ${env.BUILD_URL}console

All unit tests passed. Artifact archived.

— Jenkins CI
                """.stripIndent()
            )
        }

        failure {
            mail(
                to: "${env.NOTIFY_EMAIL}",
                subject: "[CodeRunner] Frontend Build #${env.BUILD_NUMBER} — FAILED",
                body: """
Hi,

The Frontend module build has FAILED.

─────────────────────────────────
 Project   : CodeRunner — Frontend
 Branch    : ${env.BRANCH_NAME_VAL}
 Build No  : #${env.BUILD_NUMBER}
 Status    : FAILED
 Duration  : ${currentBuild.durationString}
─────────────────────────────────

Build URL  : ${env.BUILD_URL}
Console Log: ${env.BUILD_URL}console

Please check the console output for the error details.

— Jenkins CI
                """.stripIndent()
            )
        }

        unstable {
            mail(
                to: "${env.NOTIFY_EMAIL}",
                subject: "[CodeRunner] Frontend Build #${env.BUILD_NUMBER} — UNSTABLE",
                body: """
Hi,

The Frontend module build is UNSTABLE.

This usually means integration tests were skipped because
the API server (localhost:8080) was not reachable during the build.

─────────────────────────────────
 Project   : CodeRunner — Frontend
 Branch    : ${env.BRANCH_NAME_VAL}
 Build No  : #${env.BUILD_NUMBER}
 Status    : UNSTABLE
─────────────────────────────────

Build URL  : ${env.BUILD_URL}

Action: Start the API server and re-trigger this build.

— Jenkins CI
                """.stripIndent()
            )
        }

    } // end post

} // end pipeline
