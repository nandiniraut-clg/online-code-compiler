/*
 * ============================================================
 *  CodeRunner — Frontend Module — Jenkins CI/CD Pipeline
 *  Person 1 | DevOps Mini Project
 * ============================================================
 *
 *  Pipeline Stages:
 *  1. Checkout       → Pull code from GitHub branch
 *  2. Build          → Compile with Maven
 *  3. Unit Tests     → Run JUnit tests, publibat results
 *  4. Code Quality   → Check for empty files / basic lint
 *  5. Integration Tests → API-level tests (server must be running)
 *  6. Package        → Build final JAR artifact
 *  7. Archive        → Save JAR + HTML as Jenkins artifacts
 *
 *  Post Actions:
 *  - Always  → Publibat JUnit XML report
 *  - Success → Email success notification
 *  - Failure → Email failure notification with log link
 *
 *  Setup in Jenkins:
 *  1. New Item → Pipeline
 *  2. Pipeline Definition → "Pipeline script from SCM"
 *  3. SCM: Git | Repo URL: your GitHub URL
 *  4. Branch: feature/frontend
 *  5. Script Path: frontend/Jenkinsfile
 *  6. Save → Build Now
 * ============================================================
 */

pipeline {

    // Run on any available Jenkins agent
    agent any

    // ── Environment Variables ────────────────────────────────
    environment {
        MODULE_NAME     = 'frontend'
        MODULE_DIR      = 'frontend'
        JAVA_VERSION    = '17'
        NOTIFY_EMAIL    = 'nandini.raut@cumminscollege.in'   
        GITHUB_REPO     = 'https://github.com/nandiniraut-clg/online-code-compiler'  
        BRANCH_NAME_VAL = 'frontend'
    }

    // ── Build Triggers ───────────────────────────────────────
    triggers {
        // Poll GitHub every 5 minutes for new commits
        pollSCM('H/5 * * * *')
    }

    // ── Global Options ───────────────────────────────────────
    options {
        // Keep only last 10 builds to save disk space
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Fail the build if it runs for more than 20 minutes
        timeout(time: 20, unit: 'MINUTES')

        // Add timestamps to console output
        timestamps()

        // Do not run concurrent builds for the same branch
        disableConcurrentBuilds()
    }

    // ════════════════════════════════════════════════════════
    //  STAGES
    // ════════════════════════════════════════════════════════
    stages {

        // ── Stage 1: Checkout ────────────────────────────────
        stage('Checkout') {
            steps {
                echo '──────────────────────────────────────'
                echo " Stage 1: Using existing checkout"
                echo '──────────────────────────────────────'

                checkout scm

                bat 'git log -1 --pretty=format:"Commit: %H%nAuthor: %an%nMessage: %s"'
            }
        }

        // ── Stage 2: Build ───────────────────────────────────
        stage('Build') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 2: Build — mvn clean compile'
                echo '──────────────────────────────────────'
                dir("${env.MODULE_DIR}") {
                    bat 'mvn clean compile -B'
                    // -B = batch mode (no progress bars, cleaner Jenkins logs)
                }
            }
        }

        // ── Stage 3: Unit Tests ──────────────────────────────
        stage('Unit Tests') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 3: Unit Tests — mvn test'
                echo '──────────────────────────────────────'
                dir("${env.MODULE_DIR}") {
                    // Run tests and continue even if some fail
                    // (so we can always publibat the report)
                    bat 'mvn test -B || true'
                }
            }
            post {
                always {
                    // Publibat JUnit XML results to Jenkins dabatboard
                    junit(
                        testResults: "${env.MODULE_DIR}/target/surefire-reports/*.xml",
                        allowEmptyResults: true,
                        skipPublishingChecks: false
                    )
                    echo 'JUnit report publibated to Jenkins.'
                }
            }
        }

        // ── Stage 4: Code Quality Check ──────────────────────
        stage('Code Quality') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 4: Code Quality Checks'
                echo '──────────────────────────────────────'
                dir("${env.MODULE_DIR}") {
                    script {
                        // Check index.html exists and is non-empty
                        def htmlFile = 'src/main/webapp/index.html'
                        if (fileExists(htmlFile)) {
                            def size = bat(
                                script: "wc -c < ${htmlFile}",
                                returnStdout: true
                            ).trim().toInteger()
                            if (size < 100) {
                                error("index.html appears to be empty or too small (${size} bytes)")
                            }
                            echo "index.html exists and is ${size} bytes — OK"
                        } else {
                            echo "WARNING: ${htmlFile} not found — skipping HTML check"
                        }

                        // Check pom.xml exists
                        if (!fileExists('pom.xml')) {
                            error('pom.xml is missing!')
                        }
                        echo 'pom.xml exists — OK'

                        // Check test file exists
                        if (!fileExists('src/test/java/com/coderunner/frontend/FrontendTest.java')) {
                            echo 'WARNING: FrontendTest.java not found at expected path'
                        } else {
                            echo 'FrontendTest.java exists — OK'
                        }
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
                dir("${env.MODULE_DIR}") {
                    script {
                        // Check if API server is reachable before running tests
                        def serverUp = bat(
                            script: 'curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/health || echo "000"',
                            returnStdout: true
                        ).trim()

                        if (serverUp == '200') {
                            echo "API server is UP (HTTP ${serverUp}) — running integration tests"
                            bat 'mvn verify -P integration-tests -B || true'

                            // Publibat failsafe integration test results
                            junit(
                                testResults: 'target/failsafe-reports/*.xml',
                                allowEmptyResults: true
                            )
                        } else {
                            echo "WARNING: API server not reachable (HTTP ${serverUp})"
                            echo "Skipping integration tests — start the API server and re-run."
                            // Mark as unstable instead of failed
                            currentBuild.result = 'UNSTABLE'
                        }
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
                dir("${env.MODULE_DIR}") {
                    bat 'mvn package -DskipTests -B'
                    echo 'JAR built at: target/coderunner-frontend-1.0.0.jar'
                }
            }
        }

        // ── Stage 7: Archive Artifacts ───────────────────────
        stage('Archive') {
            steps {
                echo '──────────────────────────────────────'
                echo ' Stage 7: Archiving Artifacts'
                echo '──────────────────────────────────────'
                dir("${env.MODULE_DIR}") {
                    // Save JAR to Jenkins build artifacts
                    archiveArtifacts(
                        artifacts: 'target/*.jar',
                        fingerprint: true,
                        allowEmptyArchive: true
                    )
                    // Save the frontend HTML file too
                    archiveArtifacts(
                        artifacts: 'src/main/webapp/index.html',
                        fingerprint: true,
                        allowEmptyArchive: true
                    )
                }
                echo 'Artifacts archived — visible in Jenkins build page.'
            }
        }

    } // end stages

    // ════════════════════════════════════════════════════════
    //  POST-BUILD ACTIONS
    // ════════════════════════════════════════════════════════
    post {

        // Always runs — regardless of pass/fail
        always {
            echo '══════════════════════════════════════'
            echo " Build #${env.BUILD_NUMBER} finibated: ${currentBuild.currentResult}"
            echo '══════════════════════════════════════'
        }

        // On SUCCESS
        success {
            mail(
                to: "${env.NOTIFY_EMAIL}",
                subject: "✅ [CodeRunner] Frontend Build #${env.BUILD_NUMBER} — PASSED",
                body: """
Hi,

The Frontend module build has PASSED successfully.

─────────────────────────────────
 Project   : CodeRunner — Frontend
 Branch    : ${env.BRANCH_NAME_VAL}
 Build No  : #${env.BUILD_NUMBER}
 Status    : SUCCESS ✅
 Duration  : ${currentBuild.durationString}
─────────────────────────────────

Build URL  : ${env.BUILD_URL}
Console Log: ${env.BUILD_URL}console

All unit tests passed. Artifact archived.

— Jenkins CI
                """.stripIndent()
            )
        }

        // On FAILURE
        failure {
            mail(
                to: "${env.NOTIFY_EMAIL}",
                subject: "❌ [CodeRunner] Frontend Build #${env.BUILD_NUMBER} — FAILED",
                body: """
Hi,

The Frontend module build has FAILED.

─────────────────────────────────
 Project   : CodeRunner — Frontend
 Branch    : ${env.BRANCH_NAME_VAL}
 Build No  : #${env.BUILD_NUMBER}
 Status    : FAILED ❌
 Duration  : ${currentBuild.durationString}
─────────────────────────────────

Build URL  : ${env.BUILD_URL}
Console Log: ${env.BUILD_URL}console

Please check the console output for the error details.

— Jenkins CI
                """.stripIndent()
            )
        }

        // On UNSTABLE (tests passed but integration tests skipped)
        unstable {
            mail(
                to: "${env.NOTIFY_EMAIL}",
                subject: "⚠️ [CodeRunner] Frontend Build #${env.BUILD_NUMBER} — UNSTABLE",
                body: """
Hi,

The Frontend module build is UNSTABLE.

This usually means integration tests were skipped because
the API server (localhost:8080) was not reachable during the build.

─────────────────────────────────
 Project   : CodeRunner — Frontend
 Branch    : ${env.BRANCH_NAME_VAL}
 Build No  : #${env.BUILD_NUMBER}
 Status    : UNSTABLE ⚠️
─────────────────────────────────

Build URL  : ${env.BUILD_URL}

Action: Start the API server and re-trigger this build.

— Jenkins CI
                """.stripIndent()
            )
        }

    } // end post

} // end pipeline
