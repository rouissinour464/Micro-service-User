pipeline {
    agent any

    triggers {
        cron('H */6 * * *')
    }

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
            steps { checkout scm }
        }

        stage('Build & Test') {
            steps {
                sh '''
                    chmod +x mvnw
                    ./mvnw clean verify
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t ${IMAGE}:${TAG} .'
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([
                    string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')
                ]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login -u nour292 --password-stdin
                        docker push ${IMAGE}:${TAG}
                    '''
                }
            }
        }

        stage('Deploy Application (K3s)') {
            steps {
                sh '''
                    kubectl apply -k k8s/app
                    kubectl get pods -n gestion-projet
                '''
            }
        }

        stage('Restart Auth Service') {
            steps {
                sh '''
                    kubectl rollout restart deployment auth-deployment -n gestion-projet
                    kubectl rollout status deployment auth-deployment -n gestion-projet --timeout=180s
                '''
            }
        }

        /* =======================
           MONITORING STACK (FIXED ✅)
        ======================= */
        stage('Deploy Monitoring') {
            steps {
                sh '''
                    echo "📊 Deploying monitoring stack (safe apply)..."

                    # ❌ NO DELETE
                    # ✅ Keep Grafana PVC & data
                    kubectl apply -k k8s/monitoring

                    kubectl get pods -n monitoring
                    kubectl get pvc -n monitoring
                '''
            }
        }

        stage('Restart Monitoring') {
            steps {
                sh '''
                    kubectl rollout restart deployment prometheus -n monitoring
                    kubectl rollout status deployment prometheus -n monitoring --timeout=180s

                    kubectl rollout restart deployment alertmanager -n monitoring
                    kubectl rollout status deployment alertmanager -n monitoring --timeout=180s

                    kubectl rollout restart deployment grafana -n monitoring
                    kubectl rollout status deployment grafana -n monitoring --timeout=180s
                '''
            }
        }

        /* =======================
           LOGGING STACK (OpenSearch)
        ======================= */
        stage('Deploy Logging (OpenSearch Stack)') {
            steps {
                sh '''
                    kubectl delete namespace logging --ignore-not-found=true || true
                    sleep 10

                    kubectl apply -f k8s/logging/namespace.yaml
                    kubectl apply -f k8s/logging/opensearch.yaml
                    kubectl apply -f k8s/logging/opensearch-dashboards.yaml
                    kubectl apply -f k8s/logging/fluent-bit.yaml

                    kubectl get pods -n logging
                '''
            }
        }
    }

    post {
        success {
            echo "✅ APPLICATION + MONITORING + LOGGING DEPLOYED SUCCESSFULLY 🎉"
        }
        failure {
            echo "❌ PIPELINE FAILED"
        }
    }
}
