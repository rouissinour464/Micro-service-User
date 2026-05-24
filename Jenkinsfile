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
        SONAR_ORG         = "rouissinour464"

        GIT_CREDENTIALS_ID = "github-creds"
        GIT_USER_EMAIL     = "jenkins@ci.local"
        GIT_USER_NAME      = "Jenkins CI"
    }

    stages {

        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Unit Tests') {
            steps {
                sh '''
                    set -eux
                    chmod +x mvnw
                    ./mvnw test
                '''
            }
        }

        stage('Integration Tests') {
            steps {
                sh '''
                    set -eux
                    ./mvnw verify
                '''
            }
        }

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

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -eux
                    docker build -t ${IMAGE}:${TAG} .
                '''
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')]) {
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

        stage('Check Cluster Nodes') {
            steps {
                sh '''
                    set -eux
                    kubectl get nodes
                '''
            }
        }

        stage('Update Image Tag') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "${GIT_CREDENTIALS_ID}",
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_TOKEN'
                )]) {
                    sh '''
                        set -eux
                        git config user.email "${GIT_USER_EMAIL}"
                        git config user.name  "${GIT_USER_NAME}"

                        git checkout -B v1

                        # ✅ ciblé uniquement auth-service
                        sed -i "/name: nour292\\/auth-service/{n;s/newTag:.*/newTag: \\"${TAG}\\"/}" k8s/app/kustomization.yaml

                        git add k8s/app/kustomization.yaml
                        git commit -m "ci: update auth-service image tag to ${TAG} [skip ci]" || true

                        REMOTE=$(git remote get-url origin | sed "s|https://|https://${GIT_USER}:${GIT_TOKEN}@|")
                        git push "$REMOTE" HEAD:v1
                    '''
                }
            }
        }

        stage('Apply ArgoCD Apps') {
            steps {
                sh '''
                    set -eux
                    kubectl apply -f k8s/argocd/ -n argocd || true
                    argocd app list --grpc-web || true
                '''
            }
        }

        stage('Refresh ArgoCD Cache') {
            steps {
                sh '''
                    set -eux
                    kubectl rollout restart deployment argocd-repo-server -n argocd
                '''
            }
        }

        stage('Force Sync ArgoCD') {
            steps {
                sh '''
                    set -eux
                    argocd app sync auth-service --grpc-web || true
                '''
            }
        }

        stage('Debug Kustomize') {
            steps {
                sh '''
                    set -eux
                    echo "🔍 Debug Kustomize"
                    kustomize build k8s/app || true
                '''
            }
        }

        stage('Wait ArgoCD Sync') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    sh '''
                        set -eux
                        argocd app wait auth-service \
                            --sync --health --timeout 240 --grpc-web || true
                        argocd app get auth-service --grpc-web || true
                    '''
                }
            }
        }

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

        stage('Check Pods Final') {
            steps {
                sh '''
                    set -eux
                    kubectl get pods -n ${NAMESPACE}
                    kubectl get applications -n argocd || true
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
                argocd app get auth-service --grpc-web || true
            '''
        }
        always {
            cleanWs()
        }
    }
}