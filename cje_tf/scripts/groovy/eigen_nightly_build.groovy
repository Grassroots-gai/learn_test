git_credential="lab_tfbot"
git_credential_lab="lab_tfbot"
repo_master_branch="master"

private_tensorflow_url="https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
private_tensorflow_name="private-tensorflow"

cje_tf_url="https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"
cje_tf_name="cje-tf"

def notifyBuild(String buildStatus = 'STARTED', String msg) {

    // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'
    // msg of null means no additional messages to report
    msg = msg ?: ''

    String summary = "Job <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> on ${env.NODE_NAME} : ${buildStatus} \n ${msg}"

    Integer SLACK_RETRY_TIMES = 2
    String SLACK_CHANNEL = '#tensorflow-jenkins'
    Map SLACK_COLOR_STRINGS = [
            'SUCCESS': 'good',
            'UNSTABLE': 'warning',
            'FAILURE': 'danger'
    ]

    // Send notifications
    retry(SLACK_RETRY_TIMES) {
        slackSend channel: SLACK_CHANNEL, message: summary, color: SLACK_COLOR_STRINGS[currentBuild.currentResult]
    }

}

node('nervana-bdw27.fm.intel.com') {

    try {

        notifyBuild('STARTED', '')
        deleteDir()

        stage('checkout') {

            checkout([$class: 'GitSCM',
                      branches: [[name: "*/$repo_master_branch"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$private_tensorflow_name"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$git_credential_lab",
                                           name: "$private_tensorflow_name",
                                           url: "$private_tensorflow_url"]]])

            checkout([$class: 'GitSCM',
                      branches: [[name: "*/$repo_master_branch"]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: "$cje_tf_name"]],
                      submoduleCfg: [],
                      userRemoteConfigs: [[credentialsId: "$git_credential",
                                           url: "$cje_tf_url"]]])
        }

        stage('Install dependencies') {
            sh '''
        #!/bin/bash -x
        virtualenv -p /usr/bin/python $WORKSPACE/venv
        source $WORKSPACE/venv/bin/activate
        pip install --upgrade autograd wheel backports.weakref bleach enum enum34 funcsigs future futures grpc gevent grpcio html5lib Markdown mock msgpack-python numpy pbr portpicker protobuf scikit-learn scipy setuptools six tensorflow-tensorboard Werkzeug
        '''
        }

        stage('Configure') {

            dir("$private_tensorflow_name") {
                sh '''#!/bin/bash -x

                cje_tf_dir=$WORKSPACE/cje-tf
                if [ ! -d ${cje_tf_dir} ]
                then
                echo "ERROR: cje-tf repo was not cloned correctly."
                exit 1
                fi
                
                script_dir="${cje_tf_dir}/scripts"
                if [ ! -d ${script_dir} ]
                then
                echo "ERROR: cje-tf scripts directory was not created correctly."
                exit 1
                fi
                
                patch_file="${script_dir}/patchfiles.sh"
                if [ ! -f ${patch_file} ]
                then
                echo "ERROR!!! Unable to find the patch file: ${patch_file} "
                exit 1
                fi
                cp ${patch_file} .
                ./patchfiles.sh .
    
                export PATH="/opt/tensorflow/java/jdk1.8.0_131/bin:/opt/tensorflow/bazel/bin:/opt/tensorflow/gcc/gcc6.2/bin:$PATH"
                export LD_LIBRARY_PATH="/opt/tensorflow/gcc/gcc6.2/lib64"
                export JAVA_HOME="/opt/tensorflow/java/jdk1.8.0_131"
                PYTHON_BIN_PATH="/usr/bin/python"
                PYTHON_LIB_PATH="/usr/lib/python2.7/site-packages"
    
                export PYTHON_BIN_PATH=${PYTHON_BIN_PATH} \\
                    PYTHON_LIB_PATH=${PYTHON_LIB_PATH} 
    
                source $WORKSPACE/venv/bin/activate
                yes "" | python configure.py
    
                if [ $? -ne 0 ]
                then
                  echo "configure failed"
                  exit 1
                fi
            '''
            }
        }

        stage('Build') {

            dir('private-tensorflow') {
                //print out build information
                gitCommit = sh (
                        script: 'git log --pretty=format:"%H" -n 1',
                        returnStdout: true
                ).trim()

                date = sh (
                        script: 'date +"%Y-%m-%d %H:%M"',
                        returnStdout: true
                ).trim()
                model_name = sh (script: 'lscpu | grep "Model name:"', returnStdout: true).trim()
                os_version = sh (script: "cat /etc/os-release | grep PRETTY_NAME | sed 's/PRETTY_NAME=/OS Version: /'", returnStdout: true).trim()

                echo "NODE_NAME = ${env.NODE_NAME}"
                withEnv(["date=$date","gitCommit=$gitCommit", "model_name=$model_name","os_version=$os_version",
                         "nodeName=${env.NODE_NAME}", "repoBranch=${repo_master_branch}"]) {
                    sh '''#!/bin/bash -x
                echo "*********************************************************************" > ../summary.log
                echo "Tensorflow Nightly Eigen Build ${date}" >> ../summary.log
                echo "Repository: private-tensorflow" >> ../summary.log
                echo "Branch: ${repoBranch}" >> ../summary.log
                echo "Running on: ${nodeName}" >> ../summary.log
                echo "Git Revision: ${gitCommit}" >> ../summary.log
                echo "${model_name}" >> ../summary.log
                echo "${os_version}" >> ../summary.log
                echo "*********************************************************************\n" >> ../summary.log
                echo "\n" >> ../summary.log
                '''
                }

                //build and test
                sh '''#!/bin/bash -x
                export PATH="/opt/tensorflow/java/jdk1.8.0_131/bin:/opt/tensorflow/bazel/bin:/opt/tensorflow/gcc/gcc6.2/bin:$PATH"
                export LD_LIBRARY_PATH="/opt/tensorflow/gcc/gcc6.2/lib64"
                export JAVA_HOME="/opt/tensorflow/java/jdk1.8.0_131"
                PYTHON_BIN_PATH="/usr/bin/python"
                PYTHON_LIB_PATH="/usr/lib/python2.7/site-packages"

                mkdir -p $WORKSPACE/eigendepend
                mkdir -p $WORKSPACE/logs
                log_location=$WORKSPACE/logs
                mkdir -p ${log_location}/eigentest
                eigentest_logdir="${log_location}/eigentest"
                current_build_logfile="${eigentest_logdir}/current_build.txt"

                source $WORKSPACE/venv/bin/activate
                bazel --output_base=$WORKSPACE/eigendepend test --copt="-mfma" --copt="-mavx2" --copt="-march=broadwell" --test_timeout 300,450,1200,3600 --copt="-O3" -s --cache_test_results=no -c opt  -- //tensorflow/... -//tensorflow/compiler/xla/...  -//tensorflow/compiler/jit/... -//tensorflow/contrib/tpu/... >& ${current_build_logfile}
                
                fgrep Executed ${current_build_logfile}
                if [ $? -ne 0 ]
                then
                    echo "Test failed to execute" >> ../summary.log
                    exit 1
                fi
                fgrep "were skipped" ${current_build_logfile}
                if [ $? -eq 0 ]
                then
                    echo "Some tests skipped, unsure of results, leaving eigen.failures as is" >> ../summary.log
                    exit 0
                fi

                tail -2 ${current_build_logfile} >> ../summary.log

                mkdir -p ${eigentest_logdir}/eigen-fails
                fgrep FAILED ${current_build_logfile}  | sed 's/[ ][ ]*.*//' > ${log_location}/eigen.failures

                get_failure_script=$WORKSPACE/cje-tf/scripts/get_my_failures.sh
                if [ ! -f ${get_failure_script} ]
                then
                echo "ERROR!!! Unable to find the patch file: ${get_failure_script} "
                exit 1
                fi
                
                cp ${get_failure_script} .
                ./get_my_failures.sh ${current_build_logfile} all
                cp logs.tar.gz  ${log_location}/eigen.logs.tar.gz
                cp ${current_build_logfile} ${log_location}/current_build.log
                
                eigen_build_results_shared_dir="/mnt/aipg_tensorflow_shared/validation/logs"
                if [ ! -d ${eigen_build_results_shared_dir} ]
                then
                mkdir -p ${eigen_build_results_shared_dir}
                fi
                
                date=`date "+%Y_%m_%d"`
                eigen_failure_file=${eigen_build_results_shared_dir}/eigen.failures
                eigen_logs_zip=${eigen_build_results_shared_dir}/eigen.logs.tar.gz
                
                if [ -f ${eigen_failure_file} ]
                then
                    if [-f ${eigen_failure_file}_${date} ]
                    then
                    rm -rf ${eigen_failure_file}_${date}
                    fi
                cp ${eigen_failure_file} ${eigen_failure_file}_${date}
                fi
                
                if [ -f ${eigen_logs_zip} ]
                then
                    if [-f ${eigen_logs_zip}_${date} ]
                    then
                    rm -rf ${eigen_logs_zip}_${date}
                    fi
                cp ${eigen_logs_zip} ${eigen_logs_zip}_${date}
                fi
                
                cp ${log_location}/eigen.failures ${eigen_build_results_shared_dir}/eigen.failures
                cp ${log_location}/eigen.logs.tar.gz ${eigen_build_results_shared_dir}/eigen.logs.tar.gz
                
            '''
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
        // Success or failure, always send notifications
        def msg = readFile('./summary.log')
        notifyBuild(currentBuild.result, msg)

        // Success or failure, always do artifacts
        stage('Archive Artifacts / Test Results') {
            archiveArtifacts artifacts: 'summary.log,logs/current_build.log,logs/eigen.failures,logs/eigen.logs.tar.gz', excludes: null
            fingerprint: true
        }
    }

}
