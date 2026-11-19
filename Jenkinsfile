pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        // Docker Desktop CLI: system-wide install path first, then the per-user install path
        PATH = "C:\\Program Files\\Docker\\Docker\\resources\\bin;C:\\Users\\skype\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin;${env.PATH}"
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-21'
        SONARQUBE_SERVER = 'SonarQubeServer'
        DOCKERHUB_CREDENTIALS_ID = 'Docker_Hub'
        // Docker Hub repository names must be lowercase
        DOCKERHUB_REPO = 'marakusa/bmi_3013_markus'
        DOCKER_IMAGE_TAG = 'latest'
    }

    stages {
        stage('Checkout') {
            steps { git branch: 'master', url: 'https://github.com/Marakusa/RTL_BMI_Solution_Instructions.git' }
        }
        stage('Build') {
            steps { bat 'mvn clean install' }
        }
        stage('Test') {
            steps { bat 'mvn test' }
        }
        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    withSonarQubeEnv("${env.SONARQUBE_SERVER}") {
                        // Project key, sources, tests and coverage paths come from sonar-project.properties
                        bat """
                            sonar-scanner ^
                            -Dsonar.host.url=http://localhost:9000 ^
                            -Dsonar.token=%SONAR_TOKEN%
                        """
                    }
                }
            }
        }
        stage('Check Dockerfile') {
            steps { bat 'dir' }
        }
        stage('Build Docker Image') {
            steps {
                script { docker.build("${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG}") }
            }
        }
        stage('Push Docker Image to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', DOCKERHUB_CREDENTIALS_ID) {
                        docker.image("${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG}").push()
                    }
                }
            }
        }
    }
}
