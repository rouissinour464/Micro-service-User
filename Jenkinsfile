pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        timestamps()
    }

    triggers {
        githubPush()
        cron('H */6 * * *')
    }

    tools {
        jdk 'JDK21'
    }

    environment {
        REGISTRY   = "nour292"
        IMAGE      = "${REGISTRY}/auth-service"
        TAG        = "latest"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
    }

    stages {

        /* =======================
           SOURCE CODE
        ======================= */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* =======================
           BUILD & TEST
        ======================= */
        stage('Build & Test') {
            steps {
                sh '''
                    set -euxo pipefail
                    chmod +x mvnw
                    ./mvnw clean verify
                '''
            }
        }

        /* =======================
           DOCKER
        ======================= */
        stage('Docker Build') {
            steps {
                sh '''
                    set -euxo pipefail
                    docker build -t ${IMAGE}:${TAG} .
                '''
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([
                    string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')
                ]) {
                    sh '''
                        set -euxo pipefail
                        echo "$DOCKER_PASSWORD" | docker login -u nour292 --password-stdin
                        docker push ${IMAGE}:${TAG}
                        docker logout
                    '''
                }
            }
        }

        /* =======================
           APPLICATION DEPLOY
        ======================= */
        stage('Deploy Application (K3s)') {
            steps {
                sh '''
                    set -euxo pipefail
                    kubectl apply -k k8s/app
                    kubectl get pods -n gestion-projet
                '''
            }
        }

        stage('Restart Auth Service') {
            steps {
                sh '''
                    set -euxo pipefail
                    kubectl rollout restart deployment auth-deployment -n gestion-projet
                    kubectl rollout status deployment auth-deployment -n gestion-projet --timeout=180s
                '''
            }
        }

        /* =======================
           MONITORING STACK
        ======================= */
        stage('Deploy Monitoring') {
            steps {
                sh '''
                    set -euxo pipefail
                    kubectl apply -k k8s/monitoring
                    kubectl get pods -n monitoring
                    kubectl get pvc -n monitoring
                '''
            }
        }

        stage('Restart Monitoring') {
            steps {
                sh '''
                    set -euxo pipefail

                    kubectl rollout restart deployment prometheus -n monitoring
                    kubectl rollout status deployment prometheus -n monitoring --timeout=180s

                    kubectl rollout restart deployment alertmanager -n monitoring
                    kubectl rollout status deployment alertmanager -n monitoring --timeout=180s

                    kubectl rollout restart deployment grafana -n monitoring
                    kubectl rollout status deployment grafana -n monitoring --timeout=180s
                '''
            }
        }
    }

    post {
        success {
            echo "✅ APPLICATION + MONITORING DEPLOYED SUCCESSFULLY 🎉"
        }
        failure {
            echo "❌ PIPELINE FAILED ❌"
        }
        always {
            cleanWs()
        }
    }
}
