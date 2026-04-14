pipeline {
    agent any

    tools {
        jdk 'JDK21'
    }

    environment {
        REGISTRY   = "nour292"
        IMAGE      = "${REGISTRY}/auth-service"
        TAG        = "build-${BUILD_NUMBER}"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                dir('auth') {
                    sh './mvnw clean verify'
                }
            }
        }

        stage('Docker Build') {
            steps {
                dir('auth') {
                    sh "docker build -t ${IMAGE}:${TAG} ."
                }
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([
                    string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')
                ]) {
                    sh """
                        echo \$DOCKER_PASSWORD | docker login -u ${REGISTRY} --password-stdin
                        docker push ${IMAGE}:${TAG}
                    """
                }
            }
        }

        stage('Deploy Application (K3s)') {
            steps {
                sh '''
                    echo "🚀 Deploying application via Kustomize..."
                    kubectl apply -k k8s/app
                '''
            }
        }

        stage('Rollout Restart') {
            steps {
                sh '''
                    echo "🔄 Restarting deployment..."
                    kubectl rollout restart deployment auth-deployment -n gestion-projet
                    kubectl rollout status deployment auth-deployment -n gestion-projet --timeout=90s
                '''
            }
        }

        /* ✅ STAGE MONITORING */
        stage('Deploy Monitoring') {
            steps {
                sh '''
                    echo "📊 Deploying Monitoring stack..."
                    kubectl apply -k k8s/monitoring

                    echo "✅ Monitoring pods status"
                    kubectl get pods -n monitoring
                '''
            }
        }
    }

    post {
        success {
            echo "✅ APPLICATION + MONITORING DEPLOYED SUCCESSFULLY 🎉"
        }
        failure {
            echo "❌ PIPELINE FAILED"
        }
    }
}
