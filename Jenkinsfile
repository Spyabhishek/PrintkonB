pipeline {
    agent any

    environment {
        // Updated to your backend repo and credentials
        DOCKER_IMAGE = "iamabhshek/PrintkonBackend"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/Spyabhishek/PrintkonB.git'
            }
        }
      
        stage('Build Docker Image') {
            steps {
                script {
                    // This builds the JAR inside the container if using a multi-stage Dockerfile
                    sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    sh "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    // Uses the same credential ID you set for the frontend
                    docker.withRegistry('https://index.docker.io/v1/', 'Docker_cred') {
                        docker.image("${DOCKER_IMAGE}").push("${DOCKER_TAG}")
                        docker.image("${DOCKER_IMAGE}").push("latest")
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    // Using port 8081 to avoid conflict with your frontend on 8080
                    sh "docker stop spring-app || true && docker rm spring-app || true"
                    sh "docker run -d --name spring-app -p 8443:8443 ${DOCKER_IMAGE}:latest"
                }
            }
        }
    }

    post {
        always {
            sh "docker logout"
            cleanWs()
        }
    }
}
