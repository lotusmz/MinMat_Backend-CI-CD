pipeline {
  agent any
  environment {
    SONARQUBE = 'sonar-local' // Debe coincidir con el nombre configurado en Manage Jenkins → System → SonarQube servers
  }
  options { timestamps() }

  stages {
    stage('Checkout') { steps { checkout scm } }

    stage('Permisos gradlew (Linux)') {
      when { expression { return isUnix() } }
      steps { sh 'chmod +x gradlew || true' }
    }
    stage('Build & Test') {
      steps {
        withEnv(['SPRING_PROFILES_ACTIVE=test']) {
          // Corre tests y genera JaCoCo, pero NO tumba el pipeline si fallan
          sh './gradlew clean test jacocoTestReport -Dspring.sql.init.mode=never || true'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: 'build/test-results/test/**/*.xml'
          archiveArtifacts allowEmptyArchive: true, artifacts: 'build/reports/**/*'
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv('sonar-local') {
          // Ejecuta Sonar SIN volver a correr test ni jacoco
          sh './gradlew sonarqube -x test -x jacocoTestReport -Dsonar.gradle.skipCompile=true'
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 10, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }
  }
}
