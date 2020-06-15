/*
This job:
1. Clones tensorflow/tensorflow at a given commit
2. Calls the 'tensorflow/tools/ci_build/linux/mkl/build-dev-container.sh' script

This job accepts params:
- String Type :: CHECKOUT_BRANCH :: A string branch name to clone tensorflow/tensorflow from
*/

static final String buildLabel = 'bdw'
static final String buildScript = 'tensorflow/tools/ci_build/linux/mkl/build-dev-container.sh'
static final String branchName = params.get('CHECKOUT_BRANCH', 'master')
static final ArrayList dockerImages = [ 'intel-mkl/tensorflow:nightly-devel-mkl' , 'intel-mkl/tensorflow:nightly-devel-mkl-py3' ]

node(buildLabel) {
    stage('Cleanup') {
        sh '''#!/bin/bash -x
            cd $WORKSPACE
            sudo rm -rf *
            docker stop $(docker ps -a -q)
            echo Y | docker system prune -a
        '''
    }
    stage('Checkout') {
        deleteDir()
        checkout([$class: 'GitSCM', branches: [[name: branchName]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '5d094940-c7a0-42a8-8417-1ecc9a7bd947', url: 'https://github.com/tensorflow/tensorflow']]])
    }
    stage('Build') {
        sh buildScript
    }
    stage('Archive') {
        for (dockerImage in dockerImages) {
            docker.image(dockerImage).inside('-v $WORKSPACE:/output -u root') {
                sh '''
                ls /output
                touch /tmp/pip*/*.whl && ls /tmp/pip*
                cp /tmp/pip*/*.whl /output
                '''
                //archiveArtifacts allowEmptyArchive: false, artifacts: '/tmp/pip*/*.whl'
                archiveArtifacts allowEmptyArchive: false, artifacts: '*.whl'
            }
        }
    }
}

