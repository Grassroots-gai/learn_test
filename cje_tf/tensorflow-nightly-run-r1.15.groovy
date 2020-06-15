// tensorflow nightly run

cje_tf = 'cje-tf'

node_label = 'skx'
if( 'node_label' in params && params.node_label != '' ) {
    node_label = params.node_label
}
echo "Task is running on ${node_label}"

// tensorflow update node label
tensorflow_update_node_label = 'skx'
if( 'tensorflow_update_node_label' in params && params.tensorflow_update_node_label != '' ) {
    tensorflow_update_node_label = params.tensorflow_update_node_label
}
echo "Tensorflow update will run on ${tensorflow_update_node_label}"

// container build node label
container_build_node_label = 'skx'
if( 'container_build_node_label' in params && params.container_build_node_label != '' ) {
    container_build_node_label = params.container_build_node_label
}
echo "Container build will run on ${container_build_node_label}"

// task
task = 'Benchmark'
if( 'task' in params && params.task != '' ) {
    task = params.task
}
task = task.split(',')
echo "Task: ${task}"

// benchmark
benchmark_job_list = ''
if( 'benchmark_job_list' in params && params.benchmark_job_list != '' ) {
    benchmark_job_list = params.benchmark_job_list
}
job_list = benchmark_job_list.split(',')
echo "Benchmark: ${benchmark_job_list}"

// set reference build
refer_build = 'lastSuccessfulBuild'
if( 'refer_build' in params && params.refer_build != '' ) {
    refer_build = params.refer_build
}
echo "refer_build: $refer_build"

// set send email
send_email = 'false'
if( 'send_email' in params && params.send_email != '' ) {
    send_email = params.send_email
}
echo "send_email: $send_email"

// retry number
retry_num = 2
if( 'retry_num' in params && params.retry_num != '' ) {
    retry_num = params.retry_num
}
echo "retry_num: $retry_num"

// set Post2Heims
Post2Heims = 'false'
if( 'Post2Heims' in params && params.Post2Heims != '' ) {
    Post2Heims = params.Post2Heims
}
echo "Post2Heims: $Post2Heims"

def createParallelJobs() {

    def parallel_jobs = [:]

    parallel_jobs["Unit_Test"] = {
    
        if('Unit Test' in task) {
            // eigen
            stage("Eigen Test") {
                eigen_job_name = 'Nightly-Private-Tensorflow-Eigen-r1.15'

                retry(retry_num) {
                    def eigen_test = build job: eigen_job_name, propagate: false

                    task_status = eigen_test.result
                    task_number = eigen_test.number

                    if(task_status == "FAILURE") {
                        sh '''
                            echo "Eigen got FAILED!!!"
                            exit 1
                        '''
                    }
                }

                withEnv(["eigen_job_name=${eigen_job_name}","task_status=${task_status}","task_number=${task_number}","overview_log=${overview_log}"]) {
                    sh '''#!/bin/bash
                        echo "${eigen_job_name},${task_status},${task_number}" >> ${overview_log}
                    '''
                }
                if( task_status != 'FAILURE' ) {
    
                    catchError {
                        copyArtifacts(
                                projectName: eigen_job_name,
                                selector: specific("${task_number}"),
                                filter: '**/*',
                                fingerprintArtifacts: true,
                                target: "eigen_test/")
    
                        archiveArtifacts artifacts: "eigen_test/**"
                    }
                }else {
                    writeFile file: "${WORKSPACE}/nightly_status.failure", text: "Failure"
                }
            }
            
            // unit test
            stage("Unit Test") {
                unit_job_name = 'Nightly-Private-Tensorflow-Unittest-r1.15'

                retry(retry_num) {
                    def unit_test = build job: unit_job_name, propagate: false

                    task_status = unit_test.result
                    task_number = unit_test.number

                    if(task_status == "FAILURE") {
                        sh '''
                            echo "Unit Test got FAILED!!!"
                            exit 1
                        '''
                    }
                }

                withEnv(["unit_job_name=${unit_job_name}","task_status=${task_status}","task_number=${task_number}","overview_log=${overview_log}"]) {
                    sh '''#!/bin/bash
                        echo "${unit_job_name},${task_status},${task_number}" >> ${overview_log}
                    '''
                }
                if( task_status != 'FAILURE' ) {
    
                    catchError {
                        copyArtifacts(
                                projectName: unit_job_name,
                                selector: specific("${task_number}"),
                                filter: '**/*',
                                fingerprintArtifacts: true,
                                target: "unit_test/")
    
                        archiveArtifacts artifacts: "unit_test/**"
                    }
                }else {
                    writeFile file: "${WORKSPACE}/nightly_status.failure", text: "Failure"
                }
            }
        }
    }

    parallel_jobs["Container_Build"] = {

        def container_jobs_run = [:]

        container_jobs = [ 'Intel-Models-Benchmark-fp32-py3-Trigger-r1.15','Intel-Models-Benchmark-int8-py3-Trigger-r1.15','Intel-Models-Benchmark-fp32-Trigger-r1.15','Intel-Models-Benchmark-int8-Trigger-r1.15','Private-TensorFlow-Benchmark-Q2-Py2-r1.15-CLX','Private-TensorFlow-Benchmark-Q2-Py3-r1.15-CLX','Private-TensorFlow-Benchmark-Q3-FP32-Models-r1.15-CLX','Private-TensorFlow-Benchmark-Q3-Int8-Models-r1.15-CLX','Private-TensorFlow-Benchmark-Q4-FP32-Models-r1.15-CLX','Private-TensorFlow-Benchmark-Q4-Int8-Models-r1.15-CLX','tensorflow-skylake-benchmark-nightly-r1.15' ]
        container_jobs.each {benchmark_job_name ->
            if(benchmark_job_name in job_list) {
                container_jobs_run["${benchmark_job_name}"] = {
                    catchError {
                        stage("${benchmark_job_name}") {
                            def benchmark_qa = build job: benchmark_job_name, propagate: false
                        
                            task_status = benchmark_qa.result
                            task_number = benchmark_qa.number
                            withEnv(["benchmark_job_name=${benchmark_job_name}","task_status=${task_status}","task_number=${task_number}","overview_log=${overview_log}"]) {
                                sh '''#!/bin/bash
                                    echo "${benchmark_job_name},${task_status},${task_number}" >> ${overview_log}
                                '''
                            }
                            
                            if( benchmark_qa.result != 'FAILURE' ) {
                                catchError {
                                    copyArtifacts(
                                            projectName: benchmark_job_name,
                                            selector: specific("${benchmark_qa.getNumber()}"),
                                            filter: '**/*',
                                            fingerprintArtifacts: true,
                                            target: "benchmark/${benchmark_job_name}/")
            
                                    archiveArtifacts artifacts: "benchmark/${benchmark_job_name}/**"
                                }
                            }else {
                                writeFile file: "${WORKSPACE}/nightly_status.failure", text: "Failure"
                            }
                        } //stage
                    }
                }
            }
        } // end each
        
        if('Container Build' in task) {
            // container
            stage("Container Build") {
                container_job_name = 'Private-TensorFlow-Nightly-Container-R1.15-MKLDNN'
            
                retry(retry_num) {
                    def container_build = build job: container_job_name, propagate: false

                    task_status = container_build.result
                    task_number = container_build.number

                    if(task_status == "FAILURE") {
                        sh '''
                            echo "Container Build got FAILED!!!"
                            exit 1
                        '''
                    }
                }

                withEnv(["container_job_name=${container_job_name}","task_status=${task_status}","task_number=${task_number}","overview_log=${overview_log}"]) {
                    sh '''#!/bin/bash
                        echo "${container_job_name},${task_status},${task_number}" >> ${overview_log}
                    '''
                }
                
                if(task_status == "FAILURE") {
                    writeFile file: "${WORKSPACE}/nightly_status.failure", text: "Failure"
                    echo "${container_job_name} got FAILURE !!!"
                }

                if('Benchmark' in task && task_status == "SUCCESS") {
                    parallel container_jobs_run
                }
            }
            
        }else {
            if('Benchmark' in task) {
                parallel container_jobs_run
            }
        }
    }

    if('Check Time Bombs' in task) {
        parallel_jobs["Check_Time_Bombs"] = {

            // check time bombs
            stage("Check Time Bombs") {

                tool_job_name = "tool-check-time-bombs-r1.15"
                def check_time = build job: tool_job_name, propagate: false

                task_status = check_time.result
                task_number = check_time.number
                withEnv(["tool_job_name=${tool_job_name}","task_status=${task_status}","task_number=${task_number}","overview_log=${overview_log}"]) {
                    sh '''#!/bin/bash
                        echo "${tool_job_name},${task_status},${task_number}" >> ${overview_log}
                    '''
                }

                catchError {
                    copyArtifacts(
                            projectName: tool_job_name,
                            selector: specific("${task_number}"),
                            filter: '**/*',
                            fingerprintArtifacts: true,
                            target: "tools/${tool_job_name}/")

                    archiveArtifacts artifacts: "tools/${tool_job_name}/**"
                }
            }
        }
    }

    parallel parallel_jobs
}

def generateReport(String test_start_date) {
    stage("Generate Report") {
    
        if(refer_build != 'Not') {
            // copy reference log
            copyArtifacts(
                projectName: currentBuild.projectName,
                selector: specific("${refer_build}"),
                filter: 'benchmark/**,eigen_test/**,unit_test/**,*.log,target_log/**',
                fingerprintArtifacts: true,
                target: "reference_log/")
        archiveArtifacts artifacts: "reference_log/**"
        }

        dir( WORKSPACE ) {

            // benchmark results
            withEnv(["benchmark_log=${benchmark_log}"]) {
                sh '''#!/bin/bash 
                    for file in $(find ${WORKSPACE}/benchmark/ -type f -name 'summary_nightly.log')
                    do 
                        sed '1d' ${file} >> ${benchmark_log}
                    done 
                '''
            }
            
            // unit test results
            withEnv(["unit_test_log=${unit_test_log}"]) {
                sh '''#!/bin/bash
                    # test case
                    unit_test_failed=$(grep '//.*FAILED' eigen_test/eigen_build_*.log unit_test/unit_test_*.log |sed -e 's+://+u_t_is+;s/.*u_t_is//' |cut -f1 -d' ' |sort |uniq)
                    for unit in ${unit_test_failed[@]}
                    do 
                        if [ $(grep "${unit}.*FAILED" eigen_test/eigen_build_*.log |wc -l) -ne 0 ];then 
                            eigen_test_case='FAILED'
                        else 
                            eigen_test_case='SUCCESS'
                        fi 
                        if [ $(grep "${unit}.*FAILED" unit_test/unit_test_*.log |wc -l) -ne 0 ];then 
                            unit_test_case='FAILED'
                        else 
                            unit_test_case='SUCCESS'
                        fi
                        if [ "${unit_test_case}" == "FAILED" ] && [ "${eigen_test_case}" == "SUCCESS" ];then 
                            unit_final_result='FAILED'
                        else 
                            unit_final_result='SUCCESS'
                        fi 
                        echo "//${unit},${eigen_test_case},${unit_test_case},${unit_final_result}" >> ${unit_test_log}
                    done 
                '''
            }

            // send benchmark_log to heims
            withEnv(["benchmark_log=${benchmark_log}","Post2Heims=${Post2Heims}","test_start_date=${test_start_date}"]) {
                sh '''#!/bin/bash 
                    cd ${WORKSPACE}
                    if [ "${Post2Heims}" != "false" ];then 
                        python ./cje-tf/scripts/post2heims.py -p ${benchmark_log} -t ${test_start_date} -b "r1.15" -v "r1.15" || true
                    fi
                '''
            }
           
            // generate report
            withEnv(["send_email=${send_email}","task=${task}","test_start_date=${test_start_date}"]) {
                sh '''
                    set +x
                    
                    # generate the best 
                    mv ${WORKSPACE}/reference_log/target_log ${WORKSPACE}/ || true
                    source ${WORKSPACE}/cje-tf/scripts/collect_logs_best_result.sh || true

                    # detail
                    source ${WORKSPACE}/cje-tf/scripts/generate_nightly_report.sh "R1.15" "all"
                    mv tensorflow_nightly_report.html detail.html
                    curl -X POST -H "Content-Type: application/html" -d @${WORKSPACE}/detail.html http://heims.sh.intel.com/api/storeFile/tensorflow/nightly:R1.15/${test_start_date}-detail.html/false
                    
                    source ${WORKSPACE}/cje-tf/scripts/generate_nightly_report.sh "R1.15"
                    mv tensorflow_nightly_report.html email.html
                    if [ "${send_email}" != "false" ];then 
                        curl -X POST -H "Content-Type: application/html" -d @${WORKSPACE}/email.html http://heims.sh.intel.com/api/storeFile/tensorflow/nightly:R1.15/${test_start_date}.html/true
                    fi 
                '''
            }

            archiveArtifacts artifacts: '*.html,target_log/**', excludes: null
            fingerprint: true
        }
    }
}

// daily start
node( node_label ) {

    deleteDir()

    test_start_date = sh (
        script: 'date -d @$[$(date -d "$(curl -v --silent https://google.com/ 2>&1 |grep -i "Date" |tail -1 |sed -e "s/.*ate: *//;s/ GMT//")" +%s)+3600*8] +%F',
        returnStdout: true
    ).trim()

    // over view log
    overview_log = "${WORKSPACE}/summary_overview.log"
    writeFile file: overview_log,
        text: "Jenkins Job, Build Status, Build ID\n"

    // unit test log
    unit_test_log = "${WORKSPACE}/summary_unittest.log"
    writeFile file: unit_test_log,
        text: "Test Case, Eigen, Unit Test, Result\n"

    // benchmark log
    benchmark_log = "${WORKSPACE}/summary_benchmark.log"
    writeFile file: benchmark_log,
        text: "Model, Mode,Server, Data_Type, Use_Case, Batch_Size, Result\n"
    
    // pull the cje-tf
    dir( cje_tf ) {
        checkout scm
    }

    try {
        
        // trigger tensorflow update
        if('TensorFlow Update' in task) {
            stage("Tensorflow Update") {
                tensorflow_job_name = 'tensorflow-update-repositories-R15'
                
                def tensorflow_update = build job: tensorflow_job_name, propagate: false, parameters: [
                    [$class: 'StringParameterValue', name: 'NODE_LABEL', value: tensorflow_update_node_label],
                ]
                
                task_status = tensorflow_update.result
                task_number = tensorflow_update.number
                withEnv(["tensorflow_job_name=${tensorflow_job_name}","task_status=${task_status}","task_number=${task_number}","overview_log=${overview_log}"]) {
                    sh '''#!/bin/bash
                        echo "${tensorflow_job_name},${task_status},${task_number}" >> ${overview_log}
                    '''
                }
                
                if(task_status == "SUCCESS") {
                    createParallelJobs()
                }else {
                    echo "${tensorflow_job_name} got FAILED !!!"
                    writeFile file: "${WORKSPACE}/nightly_status.failure", text: "Failure"
                }
            }
        }else {
            createParallelJobs()
        }
        
    }catch (e) {

        currentBuild.result = "FAILURE"
        throw e

    }finally {

        stage("Generate TF Info") {
            
            // generate tensorflow info
            checkout([
                $class: 'GitSCM',
                branches: [[name: "*/r1.15" ]],
                browser: [$class: 'AssemblaWeb', repoUrl: ''],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                    [$class: 'RelativeTargetDirectory', relativeTargetDir: "tensorflow-tensorflow"]
                ],
                submoduleCfg: [],
                userRemoteConfigs: [
                    [ credentialsId: "lab_tfbot", 
                        url: "https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git" ]
                ]
            ])
            
            dir("${WORKSPACE}/tensorflow-tensorflow") {
                
                sh '''
                    echo "tensorflow_repo=https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git" > ${WORKSPACE}/tensorflow_info.log
                    echo "tensorflow_branch=r1.15" >> ${WORKSPACE}/tensorflow_info.log
                    echo "tensorflow_commit=$(git rev-parse HEAD)" >> ${WORKSPACE}/tensorflow_info.log
                '''
            }
        }

        generateReport(test_start_date)

        stage('Archive Artifacts ') {
            dir( WORKSPACE ) {
                archiveArtifacts artifacts: '*.log', excludes: null
                fingerprint: true

            }
        }

        dir("${WORKSPACE}") {
            sh '''#!/bin/bash
                if [ -f "${WORKSPACE}/nightly_status.failure" ];then 
                    echo "There is some jobs got Failed! Please check it in the report."
                    exit 1
                fi
            '''
        }
    }
}
