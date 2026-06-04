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

    environment {
        REGISTRY   = "nour292"
        IMAGE      = "${REGISTRY}/auth-service"
        TAG        = "${BUILD_NUMBER}"
        KUBECONFIG = "/var/lib/jenkins/.kube/config"
        NAMESPACE  = "gestion-projet"

        SONAR_PROJECT_KEY  = "rouissinour464_micro-service-auth"
        SONAR_ORG          = "rouissinour464"

        GIT_CREDENTIALS_ID = "github-creds"
        GIT_USER_EMAIL     = "jenkins@ci.local"
        GIT_USER_NAME      = "Jenkins CI"
    }

    stages {

        // ============================================================
        stage('Checkout') {
        // ============================================================
            steps { checkout scm }
        }

        // ============================================================
        stage('Unit Tests') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    chmod +x mvnw
                    ./mvnw test
                '''
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ============================================================
        stage('Integration Tests') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    ./mvnw verify -DskipUnitTests
                '''
            }
        }

        // ============================================================
        stage('SonarCloud Analysis') {
        // ============================================================
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

        // ============================================================
        stage('Quality Gate') {
        // ============================================================
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ============================================================
        stage('Docker Build') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    docker build -t ${IMAGE}:${TAG} .
                    docker tag ${IMAGE}:${TAG} ${IMAGE}:latest
                '''
            }
        }

        // ============================================================
        stage('Docker Push') {
        // ============================================================
            steps {
                withCredentials([string(credentialsId: 'dockerhub-pass', variable: 'DOCKER_PASSWORD')]) {
                    sh '''
                        set -eux
                        echo "$DOCKER_PASSWORD" | docker login -u ${REGISTRY} --password-stdin
                        docker push ${IMAGE}:${TAG}
                        docker push ${IMAGE}:latest
                        docker logout

                        echo "🧹 Cleanup images locales..."
                        docker rmi ${IMAGE}:${TAG} ${IMAGE}:latest || true
                    '''
                }
            }
        }

        // ============================================================
        stage('Check Cluster Nodes') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    kubectl get nodes

                    NOT_READY=$(kubectl get nodes --no-headers | grep -v " Ready" || true)
                    if [ -n "$NOT_READY" ]; then
                        echo "❌ Some nodes NOT READY"
                        exit 1
                    fi

                    echo "✅ ALL NODES READY"
                '''
            }
        }

        // ============================================================
        stage('Update Image Tag') {
        // ============================================================
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

                        sed -i "/name: nour292\\/auth-service/{n;s/newTag:.*/newTag: \\"${TAG}\\"/}" \
                            k8s/app/kustomization.yaml

                        git add k8s/app/kustomization.yaml
                        git diff --cached --quiet && echo "⏭️ Pas de changement — skip commit" && exit 0

                        git commit -m "ci: update auth-service image tag to ${TAG} [skip ci]"

                        REMOTE=$(git remote get-url origin \
                            | sed "s|https://|https://${GIT_USER}:${GIT_TOKEN}@|")
                        git push "$REMOTE" HEAD:v1

                        echo "✅ Tag ${TAG} pushé sur branche v1"
                    '''
                }
            }
        }

        // ============================================================
        stage('Deploy via Kustomize') {
        // ============================================================
            steps {
                sh '''
                    set -eux

                    echo "📂 Contenu de k8s/app :"
                    ls -la k8s/app/

                    echo "🔍 Manifestes générés par Kustomize :"
                    kubectl kustomize k8s/app

                    kubectl create namespace ${NAMESPACE} \
                        --dry-run=client -o yaml | kubectl apply -f -

                    echo "🚀 Déploiement via Kustomize..."
                    kubectl apply -k k8s/app

                    echo "⏳ Attente du rollout..."
                    kubectl rollout status deployment/auth-service \
                        -n ${NAMESPACE} --timeout=120s

                    echo "🔄 Restart forcé pour prendre la nouvelle image..."
                    kubectl rollout restart deployment/auth-service \
                        -n ${NAMESPACE}

                    kubectl rollout status deployment/auth-service \
                        -n ${NAMESPACE} --timeout=120s

                    echo "✅ Déploiement auth-service terminé"
                '''
            }
        }

        // ============================================================
        stage('Apply ArgoCD Apps') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    kubectl apply -f k8s/argocd/ -n argocd || true
                    argocd app list --grpc-web || true
                '''
            }
        }

        // ============================================================
        stage('Refresh ArgoCD Cache') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    kubectl rollout restart deployment argocd-repo-server -n argocd
                    kubectl rollout status deployment argocd-repo-server \
                        -n argocd --timeout=60s
                '''
            }
        }

        // ============================================================
        stage('Force Sync ArgoCD') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    argocd app sync auth-service --grpc-web || true
                '''
            }
        }

        // ============================================================
        stage('Debug Kustomize') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    echo "🔍 Debug Kustomize"
                    kustomize build k8s/app || true
                '''
            }
        }

        // ============================================================
        stage('Wait ArgoCD Sync') {
        // ============================================================
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

        // ============================================================
        stage('Deploy Monitoring') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    kubectl apply -k k8s/monitoring
                '''
            }
        }

        // ============================================================
        stage('Restart Monitoring') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    kubectl rollout restart deployment prometheus -n monitoring
                    kubectl rollout status deployment prometheus \
                        -n monitoring --timeout=60s

                    kubectl rollout restart deployment alertmanager -n monitoring
                    kubectl rollout status deployment alertmanager \
                        -n monitoring --timeout=60s

                    kubectl rollout restart deployment grafana -n monitoring
                    kubectl rollout status deployment grafana \
                        -n monitoring --timeout=60s
                '''
            }
        }

        // ============================================================
        stage('Check Pods Final') {
        // ============================================================
            steps {
                sh '''
                    set -eux
                    echo "📦 Pods gestion-projet :"
                    kubectl get pods -n ${NAMESPACE}

                    echo "📊 ArgoCD Applications :"
                    kubectl get applications -n argocd || true

                    echo "📊 Pods monitoring :"
                    kubectl get pods -n monitoring || true
                '''
            }
        }

    } // end stages

    post {
        success {
            echo "✅ PIPELINE SUCCESS 🚀"
        }
        failure {
            echo "❌ PIPELINE FAILED"
            sh '''
                echo "=== Pods ==="
                kubectl get pods -n ${NAMESPACE} || true

                echo "=== Describe Pods ==="
                kubectl describe pods -n ${NAMESPACE} || true

                echo "=== Logs auth-service ==="
                kubectl logs -l app=auth-service \
                    -n ${NAMESPACE} --tail=80 || true

                echo "=== Events ==="
                kubectl get events -n ${NAMESPACE} \
                    --sort-by='.lastTimestamp' || true

                echo "=== ArgoCD status ==="
                argocd app get auth-service --grpc-web || true
            '''
        }
        always {
            cleanWs()
        }
    }
}