// Groovy scripts
updateGitlabCommitStatus state: 'pending'
gitLabConnection('gitlab.devtools.intel.com')
teamforge_credential = "5da0b320-00b8-4312-b653-36d4cf980fcb"

// setting test_title
test_title = "TensorFlow Tests"
if ('test_title' in params && params.test_title != '') {
    test_title = params.test_title
}
echo "Running named ${test_title}"

// setting node_label
node_label = "limengfx-clx8280"
if ('node_label' in params && params.node_label != '') {
    node_label = params.node_label
}
echo "Running on node ${node_label}"

// display and description
display_info = ""
if ('display_info' in params) {
    if (params.display_info != '') {
        display_info = params.display_info
        display_info = display_info.split(';')
        currentBuild.displayName = display_info[0]
        currentBuild.description = display_info[1]
    }
}

// setting refer_build
refer_build = "x0"
if ('refer_build' in params && params.refer_build != '') {
    refer_build = params.refer_build
}
echo "Running ${refer_build}"

// setting models
models = "ResNet-50v1.0"
if ('models' in params && params.models != '') {
    models = params.models
}
echo "Running ${models}"

// setting tensorflow_version
tensorflow_version = '1.15.2'
if ('tensorflow_version' in params && params.tensorflow_version != '') {
    tensorflow_version = params.tensorflow_version
}
echo "Running ${tensorflow_version}"

// setting models_branch
models_branch = 'develop-tf-2.1'
if ('models_branch' in params && params.models_branch != '') {
    models_branch = params.models_branch
}
echo "models_branch: ${models_branch}"

// validation-tensorflow branch to get test groovy
validation_branch = 'suyue/test_mr'
if ('validation_branch' in params && params.validation_branch != '') {
    validation_branch = params.validation_branch
}
echo "validation_branch: $validation_branch"

nigthly_test_branch = ''
MR_branch = ''
if ('nigthly_test_branch' in params && params.nigthly_test_branch != '') {
    nigthly_test_branch = params.nigthly_test_branch
}else{
    if ("${gitlabSourceBranch}" != '') {
        MR_branch = "${gitlabSourceBranch}"
        echo MR_branch
    }
}
echo "nigthly_test_branch: $nigthly_test_branch"
echo "MR_branch: $MR_branch"

email_subject="${test_title}"
if ( MR_branch != ''){
   email_subject="MR${gitlabMergeRequestIid}: ${test_title}"
}else {
   email_subject="Nightly: ${test_title}"
}

echo "email_subject: $email_subject"

Flake8_require='True'
if ('Flake8_require' in params && params.Flake8_require != '') {
    Flake8_require = params.Flake8_require
}
echo "Flake8_require: $Flake8_require"

data_type='fp32,int8'
if ('data_type' in params && params.data_type != '') {
    data_type = params.data_type
}
echo "Running ${data_type}"

//select running performance
performance='accuracy,latency,throughput'
if ('performance' in params && params.performance != '') {
    performance = params.performance
}
echo "Running ${performance}"

local_test=false
if ('local_test' in params && params.local_test){
    echo "local_test is true"
    local_test=params.local_test
}
echo "local_test = ${local_test}"

// model list of intel model zoo models == 9
intel_model_zoo_model_list=['ResNet-50v1.0', 'ResNet-50v1.5', 'ResNet-101', 'SSD-ResNet34', 'MobileNetv1', 'SSD-MobileNet', 'Faster-RCNN', 'R-FCN', 'Inception_v3']

// model list of oob classification models == 13
oob_classification_model_list=['inception_v1', 'inception_v2', 'inception_v4', 'vgg_16', 'vgg_19', 'mobilenet_v2', 'mobilenet_v1', 'resnet_v1_152', 'resnet_v1_50', 'resnet_v2_101', 'resnet_v2_152', 'resnet_v2_200', 'resnet_v2_50']

// model list of oob object detection models == 15
oob_object_detection_model_list=['ssd_mobilenet_v1', 'ssd_resnet50_v1', 'ssd_mobilenet_v2', 'ssdlite_mobilenet_v2', 'ssd_inception_v2', 'faster_rcnn_inception_v2', 'faster_rcnn_resnet50', 'rfcn_resnet101', 'faster_rcnn_resnet101', 'faster_rcnn_inception_resnet_v2', 'faster_rcnn_nas', 'mask_rcnn_inception_resnet_v2', 'mask_rcnn_inception_v2', 'mask_rcnn_resnet101', 'mask_rcnn_resnet50']

def checkout_intelai_models(String model_name){
    //checkout tensorflow-intelai-models branch for intel model zoo models
    branch = models_branch
    if (model_name=="bert_tencent"){
        branch = "tencent/bert_tf2"
    }
    if (model_name=="bert_official") {
        branch = "develop"
    }
    // download intelai models
    if(!fileExists("${WORKSPACE}/tensorflow-intelai-models")) {
        checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: "${branch}"]],
                browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [
                        [$class: 'RelativeTargetDirectory', relativeTargetDir: "tensorflow-intelai-models"],
                        [$class: 'CloneOption', timeout: 60, depth: 1]
                ],
                submoduleCfg                     : [],
                userRemoteConfigs                : [
                        [credentialsId: "${teamforge_credential}",
                         url          : "https://gitlab.devtools.intel.com/intelai/models.git"]
                ]
        ])
    }else{
        dir("${WORKSPACE}/tensorflow-intelai-models") {
            withEnv(["current_branch=${branch}"]){
                sh '''#!/bin/bash
                    set -x
                    echo "branch of intel_models--->"
                    git reset --hard
                    git clean -df
                    git checkout ${current_branch}
                    git branch
                    git rev-parse HEAD
                '''
            }
        }
    }
}


// -- Start -- //
node(node_label) {
    
    // clean up
    dir(WORKSPACE) {
        checkout scm
        //deleteDir()
        keep_dir=""
        if (local_test){
            keep_dir="dlft_oob_performance|tf_models|tensorflow-intelai-models|tensorflow-intelai-tools"
        }else{
            keep_dir="dlft_oob_performance|tf_models|tensorflow-intelai-models"
        }
        withEnv(["keep_dir=${keep_dir}"]){
            sh '''#!/bin/bash
            shopt -s extglob
            # Don't need to upgrade dlft_oob_performance every time, this process cost a lot of time. 
            rm -rf !(${keep_dir})
            shopt -u extglob
            git config --global user.email "mengfeix.li@intel.com"
            git config --global user.name "limengfx"
            '''
        }
    }

    stage("Downloads") {
        // download validation scripts
        checkout([
            $class: 'GitSCM',
            branches: [[name: validation_branch]],
            browser: [$class: 'AssemblaWeb', repoUrl: ''],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'RelativeTargetDirectory',relativeTargetDir: "validation-tensorflow"],
                [$class: 'CloneOption', timeout: 60]
            ],
            submoduleCfg: [],
            userRemoteConfigs: [
                [credentialsId: "${teamforge_credential}",
                url: "https://gitlab.devtools.intel.com/limengfx/validation-tensorflow.git"]
            ]
        ])
        
        // download tools
        if(MR_branch != ''){
            if(!fileExists("${WORKSPACE}/tensorflow-intelai-tools")) {
                checkout changelog: true, poll: true, scm:[
                        $class: 'GitSCM',
                        branches: [[name: "origin/${MR_branch}"]],
                        browser: [$class: 'AssemblaWeb', repoUrl: ''],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [
                                [$class: 'RelativeTargetDirectory',relativeTargetDir: "tensorflow-intelai-tools"],
                                [$class: 'CloneOption', timeout: 60],
                                [$class: 'PreBuildMerge', options: [fastForwardMode: 'FF', mergeRemote: 'origin', mergeStrategy: 'DEFAULT', mergeTarget: "${env.gitlabTargetBranch}"]]
                        ],
                        submoduleCfg: [],
                        userRemoteConfigs: [
                                [credentialsId: "${teamforge_credential}",
                                 url: "https://gitlab.devtools.intel.com/intelai/tools.git"]
                        ]
                ]
            }
        }else{
            checkout changelog: true, poll: true, scm:[
                    $class: 'GitSCM',
                    branches: [[name: "${nigthly_test_branch}"]],
                    browser: [$class: 'AssemblaWeb', repoUrl: ''],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                            [$class: 'RelativeTargetDirectory',relativeTargetDir: "tensorflow-intelai-tools"],
                            [$class: 'CloneOption', timeout: 60]
                    ],
                    submoduleCfg: [],
                    userRemoteConfigs: [
                            [credentialsId: "${teamforge_credential}",
                             url: "https://gitlab.devtools.intel.com/intelai/tools.git"]
                    ]
            ]
        }
    }

    // format check autopep8
    if(Flake8_require=='True'){
        stage('Flake8') {
            echo "+---------------- Flake8 ----------------+"

            sh '''#!/bin/bash
            # python
            export PATH=${HOME}/miniconda3/bin/:$PATH
            conda remove --all -y -n ${HOSTNAME}
            conda create python=3.6.9 -y -n ${HOSTNAME}
            source activate ${HOSTNAME}
            python -V
            
            set -x
            pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
            pip install flake8
            flake8 --max-line-length 120 ${WORKSPACE}/tensorflow-intelai-tools > ${WORKSPACE}/flake8-intelai-tools-$(date +%s).log 2>&1 || true
        '''
        }
    }

    // benchmark
    stage("Benchmarks") {
        echo "+---------------- Benchmarks ----------------+"

        // copy quantize file
        withEnv(["tensorflow_version=${tensorflow_version}"]){
            sh'''#!/bin/bash
            set -x
            from_path=${WORKSPACE}/validation-tensorflow/quantization/scripts/quantize
            to_path=${WORKSPACE}/tensorflow-intelai-tools/api
            cp ${from_path}/quantize_model.py ${to_path} 
            cp ${from_path}/quantize_model_oob.py ${to_path}
            cp ${from_path}/quantize_model_oob_obj.py ${to_path}
            cp ${from_path}/quantize_bert.py ${to_path}
            cp ${from_path}/config/models-tf2x.json ${to_path}/config/models.json
            #if [[ ${tensorflow_version} == 2.* ]]; then
            #    cp ${from_path}/config/models-tf2x.json ${to_path}/config/models.json
            #else
            #    cp ${from_path}/config/models-tf1x.json ${to_path}/config/models.json
            #fi
        '''
        }

        summary_log = "${WORKSPACE}/summary_tests.log"
        writeFile file: summary_log, text: "Framework;Platform;Precision;Model;Mode;Type;BS;Value\n"

        models = models.split(',')
        model=""
        for(per_model in models) {
            model="${per_model}"
            echo "outside model is -------${model}---------"
            if (oob_object_detection_model_list.contains(per_model)) {
                echo "-----model in oob_object_detection_model_list-----"
                generate_benchmark = load("${WORKSPACE}/validation-tensorflow/quantization/run_oob_object_detection_multi.groovy")
            }else if(oob_classification_model_list.contains(per_model)){
                echo "-----model in oob_classification_model_list-----"
                generate_benchmark = load("${WORKSPACE}/validation-tensorflow/quantization/run_oob_classification_multi.groovy")
            }else if(intel_model_zoo_model_list.contains(per_model)){
                echo "-----model in intel_model_zoo_model_list-----"
                checkout_intelai_models(per_model)
                generate_benchmark = load("${WORKSPACE}/validation-tensorflow/quantization/${models_branch}/run-${models_branch}-all.groovy")
            }else {
                echo "-----model is bert------"
                checkout_intelai_models(per_model)
                generate_benchmark = load("${WORKSPACE}/validation-tensorflow/quantization/run_${model}.groovy")
            }
            generate_benchmark()
        }

    }
    
    // report
    stage("Report") {
        // download reference
        if(refer_build != 'x0') {
            copyArtifacts(
                projectName: currentBuild.projectName,
                selector: specific("${refer_build}"),
                filter: 'summary_tests.log',
                fingerprintArtifacts: true,
                target: "reference")
        }

        dir(WORKSPACE) {
            withEnv(["tensorflow_version=${tensorflow_version}","models_branch=${models_branch}","MR_branch=${MR_branch}","qtools_branch=${nigthly_test_branch}"]) {
                sh'''#!/bin/bash
                    set -x
                    cd ${WORKSPACE}/tensorflow-intelai-tools
                    qtools_commit=$(git rev-parse HEAD)
                    cd ${WORKSPACE}
                    summaryLog="${WORKSPACE}/summary_tests.log"
                    lastFile="${WORKSPACE}/reference/summary_tests.log"
                    chmod 775 ${WORKSPACE}/validation-tensorflow/quantization/scripts/generate_report.sh
                    qtools_commit=${qtools_commit} summaryLog=${summaryLog} lastFile=${lastFile} ${WORKSPACE}/validation-tensorflow/quantization/scripts/generate_report.sh 
                '''
            }
        }
    }
    // archive artifacts
    stage("Artifacts") {
        dir("$WORKSPACE") {
            if(MR_branch != ''){
                recipient_list = 'suyue.chen@intel.com,' + "${gitlabUserEmail}"
                if ('recipient_list' in params && params.recipient_list != '') {
                    recipient_list = params.recipient_list + ',' + gitlabUserEmail
                }
            }else{
                recipient_list = 'suyue.chen@intel.com'
                if ('recipient_list' in params && params.recipient_list != '') {
                    recipient_list = params.recipient_list
                }
            }

            echo "Running ${models}"
            emailext subject: "${email_subject}",
                to: "${recipient_list}",
                replyTo: "${recipient_list}",
                body: '''${FILE,path="report.html"}''',
                attachmentsPattern: "",
                mimeType: 'text/html'
            
            archiveArtifacts artifacts: '*.log,*.html,*/*.log', excludes: null
            fingerprint: true
        }
        if (MR_branch != ''){
            updateGitlabCommitStatus state: 'success'
        }else{
            sh'''
                echo 'nightly test success'
            '''
        }
    }

}
