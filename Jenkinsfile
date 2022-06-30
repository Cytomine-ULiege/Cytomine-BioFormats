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

            stage ('Check official release status') {
                env.OFFICIAL_RELEASE = sh(
                    script: 'scripts/ci.sh is-official-release ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}',
                    returnStdout: true
                )
                echo("Official Release to publish on Github ? ${env.OFFICIAL_RELEASE}")

                if (env.OFFICIAL_RELEASE && env.OFFICIAL_RELEASE.toBoolean()) {
                    echo("official")
                    stage ('Publish official release on Github') {
                        env.GITHUB_REPO = scm.getUserRemoteConfigs()[0].getUrl().replaceFirst(/^.*?(?::\/\/.*?\/|:)(.*).git$/, '$1')
                        withCredentials(
                            [
                                usernamePassword(
                                    credentialsId: 'GITHUB_RELEASE_CREDENTIAL',
                                    usernameVariable: 'GITHUB_RELEASE_USER',
                                    passwordVariable: 'GITHUB_RELEASE_TOKEN'
                                )
                            ]
                        ) {
                            sh 'scripts/ci.sh publish-github'
                        }
                    }
                }
            }
        }

        stage ('Clean Docker images') {
            sh 'scripts/ci.sh clean-docker ${NAMESPACE} docker.io bioformat ${BRANCH_NAME}'
        }
    }
}
