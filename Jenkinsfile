node {
    stage ('Retrieve sources') {
        checkout([
            $class: 'GitSCM',
            branches: [[name: 'refs/heads/'+env.BRANCH_NAME]],
            extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: '']],
            userRemoteConfigs: scm.userRemoteConfigs,
        ])
    }

    stage ('Clean') {
        sh 'rm -rf output'
        sh 'mkdir -p output'
    }

    stage ('Compute version number') {
        env.VERSION_NUMBER = sh(
            script: 'scripts/ci.sh version-number ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}',
            returnStdout: true
        )
        echo("Version number: ${env.VERSION_NUMBER}")

        stage ('Download and cache dependencies') {
            sh 'scripts/ci.sh dependencies ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}'
        }

        stage ('Build jar') {
            sh 'scripts/ci.sh build-jar ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}'
        }

        stage ('Publish jar') {
            sh 'scripts/ci.sh publish-jar ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}'

            stage ('Build Docker image') {
                sh 'scripts/ci.sh build-docker ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}'
            }
            stage ('Publish Docker image') {
                withCredentials(
                    [
                        usernamePassword(
                            credentialsId: 'DOCKERHUB_CREDENTIAL',
                            usernameVariable: 'DOCKERHUB_USER',
                            passwordVariable: 'DOCKERHUB_TOKEN'
                        )
                    ]
                ) {
                    docker.withRegistry('https://index.docker.io/v1/', 'DOCKERHUB_CREDENTIAL') {
                        sh 'scripts/ci.sh publish-docker ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}'
                    }
                }
            }
        }

        stage ('Clean Docker images') {
            sh 'scripts/ci.sh clean ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}'
        }
    }
}
