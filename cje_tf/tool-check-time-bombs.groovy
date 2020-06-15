// check time bombs

node_label = 'clx'
if( 'node_label' in params && params.node_label != '' ) {
    node_label = params.node_label
}
echo "Task is running on ${node_label}"

tensorflow_repo = 'https://github.com/tensorflow/tensorflow.git'
if( 'tensorflow_repo' in params && params.tensorflow_repo != '' ) {
    tensorflow_repo = params.tensorflow_repo
}
echo "tensorflow ${tensorflow_repo}"

tensorflow_branch = 'master'
if( 'tensorflow_branch' in params && params.tensorflow_branch != '' ) {
    tensorflow_branch = params.tensorflow_branch
}
echo "tensorflow branch ${tensorflow_branch}"

refer_build = currentBuild.number - 1
if( 'refer_build' in params && params.refer_build != 'lastBuild' ) {
    refer_build = params.refer_build
}
echo "refer build: ${refer_build}"


// start
node(node_label) {

    deleteDir()
    
    stage("downloadRepo") {
        // checkout cje-tf
        checkout([
            $class: 'GitSCM',
            branches: [[name: "*/master"]],
            browser: [$class: 'AssemblaWeb', repoUrl: ''],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'RelativeTargetDirectory',relativeTargetDir: "cje-tf"],
                [$class: 'CloneOption', timeout: 30, depth: 1]
            ],
            submoduleCfg: [],
            userRemoteConfigs: [
                [url: "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"]
            ]
        ])
        
        // checkout tensorflow
        checkout([
            $class: 'GitSCM',
            branches: [[name: tensorflow_branch]],
            browser: [$class: 'AssemblaWeb', repoUrl: ''],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'RelativeTargetDirectory',relativeTargetDir: "tensorflow"],
                [$class: 'CloneOption', timeout: 30, depth: 1]
            ],
            submoduleCfg: [],
            userRemoteConfigs: [
                [credentialsId: 'lab_tfbot', url: tensorflow_repo]
            ]
        ])
    }
    
    try {
        // copy reference log
        if(refer_build != '0') {
            copyArtifacts(
                projectName: currentBuild.projectName,
                selector: specific("${refer_build}"),
                filter: '*.json',
                fingerprintArtifacts: true,
                target: "reference_log/"
            )
            archiveArtifacts artifacts: "reference_log/**"
        }

        stage("checkTimeBombs") {
            
            withEnv(["tensorflow_repo=${tensorflow_repo}","tensorflow_branch=${tensorflow_branch}"]) {
                
                sh '''#!/bin/bash -x
                    
                    flags=" -o ${WORKSPACE}/tensorflow-check-time-bombs.json -s ${WORKSPACE}/tensorflow -d ${WORKSPACE}/tensorflow.xml "
                    
                    if [ -f ${WORKSPACE}/reference_log/tensorflow-check-time-bombs.json ];then 
                        flags+=" -i ${WORKSPACE}/reference_log/tensorflow-check-time-bombs.json "
                    fi 
                    
                    python -V
                    
                    python ${WORKSPACE}/cje-tf/scripts/checkTimeBombs.py ${flags}
                    printf $? |tee ${WORKSPACE}/check_time_bombs.status
                    
                    python ${WORKSPACE}/cje-tf/scripts/get_ctb_result.py -u $(echo ${tensorflow_repo} |sed 's+.git$++') -b ${tensorflow_branch} -i ${WORKSPACE}/tensorflow-check-time-bombs.json
                '''
            }

            String build_status = readFile file: "${WORKSPACE}/check_time_bombs.status"
            
            if(build_status == "1") {
                currentBuild.result = 'UNSTABLE'
            }
            if(build_status != "1" && build_status != "0") {
                currentBuild.result = 'FAILURE'
            }
        }

    }catch(e) {
        throw e
    }finally {
        archiveArtifacts artifacts: '*.json,*.xml,*.html', excludes: null
        deleteDir()
    }
}
