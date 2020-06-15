/*
This job:
1. Clones tensorflow/tensorflow at a given commit
2. Calls the 'tensorflow/tools/ci_build/linux/mkl/basic-mkl-test.sh' script

This job accepts params:
- String Type :: CHECKOUT_BRANCH :: A string branch name to clone tensorflow/tensorflow from
*/

static final ArrayList buildLabels = [ 'bdw' /*, 'skx'*/ ]
static final String buildScript = 'tensorflow/tools/ci_build/linux/mkl/basic-mkl-test.sh'
static final String branchName = params.get('CHECKOUT_BRANCH', 'master')

Map parallelBuild = [:]
buildLabels.each { l ->
    parallelBuild += [
        "${l}": {
            node(l) {
                stage('Checkout') {
                    deleteDir()
                    checkout([$class: 'GitSCM', branches: [[name: branchName]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '5d094940-c7a0-42a8-8417-1ecc9a7bd947', url: 'https://github.com/tensorflow/tensorflow']]])
                }
                stage('Build') {
                    sh buildScript
                }
            }
        }
    ]
}
parallel parallelBuild

