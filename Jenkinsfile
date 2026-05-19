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
        TAG        = "${BUILD_NUMBER}"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
        NAMESPACE  = "gestion-projet"

        SONAR_PROJECT_KEY = "rouissinour464_micro-service-auth"
        SONAR_ORG = "rouissinour464"
    }

    stages {

        /* ======================= */
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        /* ✅ TESTS UNIQUEMENT */
        stage('Unit Tests') {
            steps {
                sh '''
                    set -eux
                    chmod +x mvnw
                    ./mvnw test
                '''
            }
        }

        /* ✅ INTEGRATION TEST */
        stage('Integration Tests') {
            steps {
                sh '''
                    set -eux
                    ./mvnw verify
                '''
            }
        }

        /* ✅ SONAR */
        stage('SonarCloud Analysis') {
            steps {
                withSonarQubeEnv('SonarCloud') {
                    withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            set -eux

                            ./mvnw sonar:sonar \
                              -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                              -Dsonar.organization=${SONAR_ORG} \
                              -Dsonar.host.url=https://sonarcloud.io \
                              -Dsonar.token=${SONAR_TOKEN}
                        '''
                    }
                }
            }
        }

        /* ✅ QUALITY */
        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        /* ✅ DOCKER = build réel */
        stage('Docker Build') {
            steps {
                sh '''
                    set -eux
                    docker build -t ${IMAGE}:${TAG} .
                '''
            }
        }

        /* ✅ PUSH */
        stage('Docker Push') {
            steps {
                withCredentials([
                    string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')
                ]) {
                    sh '''
                        set -eux

                        echo "$DOCKER_PASSWORD" | docker login -u ${REGISTRY} --password-stdin
                        docker push ${IMAGE}:${TAG}
                        docker tag ${IMAGE}:${TAG} ${IMAGE}:latest
                        docker push ${IMAGE}:latest
                        docker logout
                    '''
                }
            }
        }

        /* ✅ CHECK CLUSTER */
        stage('Check Cluster Nodes') {
            steps {
                sh '''
                    set -eux
                    kubectl get nodes
                '''
            }
        }

        /* ✅ DEPLOY */
        stage('Deploy Application (K3s)') {
            steps {
                sh '''
                    set -eux
                    kubectl apply -k k8s/app
                '''
            }
        }

        /* ✅ RESTART */
        stage('Restart Auth Service') {
            steps {
                sh '''
                    set -eux
                    kubectl rollout restart deployment auth-deployment -n ${NAMESPACE}
                    kubectl rollout status deployment auth-deployment -n ${NAMESPACE}
                '''
            }
        }

        /* ✅ MONITORING */
        stage('Deploy Monitoring') {
            steps {
                sh '''
                    set -eux
                    kubectl apply -k k8s/monitoring
                '''
            }
        }

        stage('Restart Monitoring') {
            steps {
                sh '''
                    set -eux
                    kubectl rollout restart deployment prometheus -n monitoring
                    kubectl rollout restart deployment alertmanager -n monitoring
                    kubectl rollout restart deployment grafana -n monitoring
                '''
            }
        }

        /* ✅ CHECK FINAL */
        stage('Check Pods Final') {
            steps {
                sh '''
                    kubectl get pods -n ${NAMESPACE}
                '''
            }
        }
    }

    post {
        success {
            echo "✅ PIPELINE SUCCESS 🚀"
        }

        failure {
            echo "❌ PIPELINE FAILED"

            sh '''
                kubectl describe pods -n ${NAMESPACE} || true
                kubectl logs -l app=auth-service -n ${NAMESPACE} --tail=80 || true
            '''
        }

        always {
            cleanWs()
        }
    }
}