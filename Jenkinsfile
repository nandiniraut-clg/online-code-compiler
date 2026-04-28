pipeline {
    agent any

    tools {
        maven 'Maven'   // must match the name configured in Jenkins → Global Tool Configuration
        jdk   'JDK17'  // must match the name configured in Jenkins → Global Tool Configuration
    }

    environment {
        MODULE_NAME = 'execution-engine'
        EMAIL_RECIPIENT = 'your-email@example.com'   // ← change this to your email
    }

    stages {

        stage('Checkout') {
            steps {
                echo "Checking out branch: ${env.GIT_BRANCH}"
                checkout scm
            }
        }

        stage('Build') {
            steps {
                dir('execution-engine') {
                    echo 'Building the Execution Engine...'
                    sh 'mvn clean compile -DskipTests'
                }
            }
        }

        stage('Test') {
            steps {
                dir('execution-engine') {
                    echo 'Running JUnit tests...'
                    sh 'mvn test'
                }
            }
            post {
                always {
                    // Publish test results in Jenkins UI
                    junit 'execution-engine/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                dir('execution-engine') {
                    echo 'Packaging into JAR...'
                    sh 'mvn package -DskipTests'
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'execution-engine/target/*.jar', fingerprint: true
                echo 'JAR archived successfully.'
            }
        }
    }

    post {
        success {
            mail to: "${env.EMAIL_RECIPIENT}",
                 subject: "✅ BUILD SUCCESS: ${env.MODULE_NAME} [${env.BUILD_NUMBER}]",
                 body: """Build #${env.BUILD_NUMBER} passed!

Module : ${env.MODULE_NAME}
Branch : ${env.GIT_BRANCH}
Status : SUCCESS

View build: ${env.BUILD_URL}"""
        }

        failure {
            mail to: "${env.EMAIL_RECIPIENT}",
                 subject: "❌ BUILD FAILED: ${env.MODULE_NAME} [${env.BUILD_NUMBER}]",
                 body: """Build #${env.BUILD_NUMBER} FAILED.

Module : ${env.MODULE_NAME}
Branch : ${env.GIT_BRANCH}
Status : FAILURE

View build: ${env.BUILD_URL}
Check the console output for details."""
        }
    }
}
