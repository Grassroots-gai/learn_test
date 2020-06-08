git_credential = "f4bb679e-e69f-4738-9a4d-a67630e89aaf"

node(BENCHMARK_MAIN_NODE){
	deleteDir()
	if(DOUBLE_CHECK == "False"){
		stage("build tensorflow wheel"){
			TF_ARR = TF_SOURCES.split(',')
			def build_jobs = [:]
			TF_ARR.each{ONE_TF ->
				build_jobs["${ONE_TF}"] = {
					node(BUILD_NODE){
						if (BUILD_NODE == TEST_NODE){
							withEnv(["ONE_TF=${ONE_TF}","INTERNAL_BAZEL_VERSION=${INTERNAL_BAZEL_VERSION}", "UPSTREAM_BAZEL_VERSION=${UPSTREAM_BAZEL_VERSION}"]){
								sh '''#!/bin/bash
								shopt -s extglob
								rm -rf !(${ONE_TF}_tensorflow|bazel-${INTERNAL_BAZEL_VERSION}-installer-linux-x86_64.sh|bazel-${UPSTREAM_BAZEL_VERSION}-installer-linux-x86_64.sh|mask_rcnn|MobileNet-v1_models|SSD-ResNet34_models|ssd-resnet-benchmarks|SSD-ResNet34_training_models|bert)
								shopt -u extglob
							'''
							}
						}else{
							withEnv(["ONE_TF=${ONE_TF}","INTERNAL_BAZEL_VERSION=${INTERNAL_BAZEL_VERSION}", "UPSTREAM_BAZEL_VERSION=${UPSTREAM_BAZEL_VERSION}"]){
								sh '''#!/bin/bash
								shopt -s extglob
								rm -rf !(${ONE_TF}_tensorflow|bazel-${INTERNAL_BAZEL_VERSION}-installer-linux-x86_64.sh|bazel-${UPSTREAM_BAZEL_VERSION}-installer-linux-x86_64.sh)
								shopt -u extglob
							'''
							}
						}
						RETRY_NUM=3
						retry(RETRY_NUM) {
							if (ONE_TF == "internal") {
								if (INTERNAL_TF_BRANCH == "") {
									INTERNAL_TF_BRANCH = "cpx-launch-unified"
								}
								checkout([
										$class                           : 'GitSCM',
										branches                         : [[name: "${INTERNAL_TF_BRANCH}"]],
										browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
										doGenerateSubmoduleConfigurations: false,
										extensions                       : [
												[$class: 'RelativeTargetDirectory', relativeTargetDir: "internal_tensorflow"],
												[$class: 'CloneOption', timeout: 3600]
										],
										submoduleCfg                     : [],
										userRemoteConfigs                : [
												[credentialsId: git_credential,
												 url          : "${INTERNAL_TF}"]
										]
								])
							} else {
								if (UPSTREAM_TF_BRANCH == "") {
									UPSTREAM_TF_BRANCH = "master"
								}
								withEnv(["UPSTREAM_TF=${UPSTREAM_TF}",
										 "UPSTREAM_TF_BRANCH=${UPSTREAM_TF_BRANCH}",
										 "UPSTREAM_TF_COMMIT_ID=${UPSTREAM_TF_COMMIT_ID}"]) {
									sh '''#!/bin/bash -x
										export ftp_proxy=http://child-prc.intel.com:913
										export http_proxy=http://child-prc.intel.com:913
										export https_proxy=http://child-prc.intel.com:913
										cd ${WORKSPACE}
                                        git clone ${UPSTREAM_TF} upstream_tensorflow
                                        cd upstream_tensorflow
                                        git reset --hard
                                        git clean -fd
                                        git pull
                                        git checkout ${UPSTREAM_TF_BRANCH}
                                        git reset ${UPSTREAM_TF_COMMIT_ID}  --hard
                                    '''
								}
							}
						}

						if(USE_MACHINE_GCC=="False"){
							build_gcc_version=GCC_VERSION
						}else{
							build_gcc_version=sh(script:'gcc -v 2>&1 |grep "gcc version" | sed -e s"/gcc version //;s/(.*//"', returnStdout: true).trim()
						}

						withEnv(["ONE_TF=${ONE_TF}","INTERNAL_TF_COMMIT_ID=${INTERNAL_TF_COMMIT_ID}","UPSTREAM_TF_COMMIT_ID=${UPSTREAM_TF_COMMIT_ID}",
								 "INTERNAL_TF=${INTERNAL_TF}","INTERNAL_TF_BRANCH=${INTERNAL_TF_BRANCH}",
								 "UPSTREAM_TF=${UPSTREAM_TF}","UPSTREAM_TF_BRANCH=${UPSTREAM_TF_BRANCH}",
								 "BUILD_COMMAND=${BUILD_COMMAND}","INTERNAL_BAZEL_VERSION=${INTERNAL_BAZEL_VERSION}",
                                 "UPSTREAM_BAZEL_VERSION=${UPSTREAM_BAZEL_VERSION}", "GCC_PATH=${GCC_PATH}",
                                 "CONDA_BIN_PATH=${CONDA_BIN_PATH}", "USE_MACHINE_GCC=${USE_MACHINE_GCC}"]){
							sh'''#!/bin/bash -x
								export ftp_proxy=http://child-prc.intel.com:913
								export http_proxy=http://child-prc.intel.com:913
								export https_proxy=http://child-prc.intel.com:913
								COMMIT_ID=""
								TF_BRANCH=""
								BAZEL_VERSION=""
								if [ "${ONE_TF}" == "internal" ];
								then
									TF_BRANCH=${INTERNAL_TF_BRANCH}
									BAZEL_VERSION=${INTERNAL_BAZEL_VERSION}
									if [ "${INTERNAL_TF_COMMIT_ID}" == "" ];
									then
										cd ${WORKSPACE}/internal_tensorflow
										git reset --hard
										git checkout ${TF_BRANCH}
										git pull
										INTERNAL_TF_COMMIT_ID=$(git rev-parse HEAD)
									fi
									COMMIT_ID=${INTERNAL_TF_COMMIT_ID}
									echo "internal url: ${INTERNAL_TF}" >> ${WORKSPACE}/internal_tf_info.log
									echo "internal branch: ${INTERNAL_TF_BRANCH}" >> ${WORKSPACE}/internal_tf_info.log
									echo "internal commit id: ${INTERNAL_TF_COMMIT_ID}" >> ${WORKSPACE}/internal_tf_info.log
								elif [ "${ONE_TF}" == "upstream" ];
								then
									TF_BRANCH=${UPSTREAM_TF_BRANCH}
									BAZEL_VERSION=${UPSTREAM_BAZEL_VERSION}
									if [ "${UPSTREAM_TF_COMMIT_ID}" == "" ];
									then
										cd ${WORKSPACE}/upstream_tensorflow
										git reset --hard
										git checkout ${TF_BRANCH}
										git pull
										UPSTREAM_TF_COMMIT_ID=$(git rev-parse HEAD)
									fi
									COMMIT_ID=${UPSTREAM_TF_COMMIT_ID}
									echo "upstream url: ${UPSTREAM_TF}" >> ${WORKSPACE}/upstream_tf_info.log
									echo "upstream branch: ${UPSTREAM_TF_BRANCH}" >> ${WORKSPACE}/upstream_tf_info.log
									echo "upstream commit id: ${UPSTREAM_TF_COMMIT_ID}" >> ${WORKSPACE}/upstream_tf_info.log
								fi
								
								echo "======================================================="
								echo "build with parameters:"
								echo "	  tensorflow source:                 ${ONE_TF}"
								echo "    bazel version:                     ${BAZEL_VERSION}"
								echo "    build command:                     ${BUILD_COMMAND}"
								echo "    tensorflow branch:                 ${TF_BRANCH}"
								echo "    tensorflow commit id:              ${COMMIT_ID}"
								echo "======================================================="
		
								cd ${WORKSPACE}
								if [ "${USE_MACHINE_GCC}" == "False" ]; then
									export PATH="${GCC_PATH}/bin:$PATH"
									export LD_LIBRARY_PATH="${GCC_PATH}/lib64:$LD_LIBRARY_PATH"
								fi
								export PATH="${CONDA_BIN_PATH}:$PATH"
								source ${CONDA_BIN_PATH}/activate ${HOSTNAME}
								if [ $? -ne 0 ]; then
									attempts=0
									until [[ "$attempts" -ge 3 ]]
									do
										conda create python=3.6.9 -y -n ${HOSTNAME} && break
										attempts=$[$attempts+1]
										sleep 5
									done																	
									source activate ${HOSTNAME}
									attempts1=0
									until [[ "$attempts1" -ge 3 ]]
									do
										pip install numpy tensorflow Keras && break
										attempts1=$[$attempts1+1]
										sleep 5
									done
									attempts2=0
									until [[ "$attempts2" -ge 3 ]]
									do
										conda install -c anaconda git -y && break
										attempts2=$[$attempts2+1]
										sleep 5
									done							
								fi
								ls bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh
								if [ $? -ne 0 ]; then
									attempts3=0
									until [[ "$attempts3" -ge 3 ]]
									do
										wget https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh && break
										attempts3=$[$attempts3+1]
										sleep 5
									done
									chmod 777 bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh
								fi
								./bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh --prefix=${WORKSPACE}/bazel_${BAZEL_VERSION}
								export PATH=${WORKSPACE}/bazel_${BAZEL_VERSION}/bin:$PATH
								cd ${WORKSPACE}/${ONE_TF}_tensorflow							
								git checkout ${TF_BRANCH}
								git pull
								git reset ${COMMIT_ID} --hard
								sed -i "/executable = ctx.executable._swig/a\\        use_default_shell_env = True," tensorflow/tensorflow.bzl
								sed -i "s/f14df2173370a4ff495886bd2faee764d8580431f154132e0ce18fe57ed45f03/657785e96dd3fc716e0b511c77d429c95e3ef96445507847f549abcf58dc3171/"  tensorflow/workspace.bzl
								bazel clean --async --expunge
								yes "" | python configure.py
								${BUILD_COMMAND} > /dev/null 2>&1
								wheel_path=${WORKSPACE}/tensorflow_${ONE_TF}_wheel/
								mkdir -p ${wheel_path}
								./bazel-bin/tensorflow/tools/pip_package/build_pip_package  ${wheel_path}
							'''
						}
						archiveArtifacts "tensorflow_*_wheel/**,*tf_info.log"
					}
				}
			}
			parallel build_jobs
		}
	}else{
		node(BUILD_NODE){
			if(USE_MACHINE_GCC=="False"){
				build_gcc_version=GCC_VERSION
			}else{
				build_gcc_version=sh(script:'gcc -v 2>&1 |grep "gcc version" | sed -e s"/gcc version //;s/(.*//"', returnStdout: true).trim()
			}
		}
	}
	stage('run_models') {
		MODEL_ARR = ALL_MODELS.split(',')
		TF_ARR = TF_SOURCES.split(',')
		PRE_ARR = PRECISION.split(',')
		TEST_MODE_ARR = TEST_MODE.split(',')
		def test_jobs = [:]
		TF_ARR.each { ONE_TF ->
			MODEL_ARR.each { ONE_NAME ->
				PRE_ARR.each { ONE_PRE ->
					TEST_MODE_ARR.each { ONE_MODE ->
						test_jobs["${ONE_TF}--${ONE_NAME}--${ONE_PRE}--${ONE_MODE}--${SERVER_TYPE}"] = {
							node(TEST_NODE) {
								dir(WORKSPACE){
									sh '''#!/bin/bash -x
											shopt -s extglob
											rm -rf !(mask_rcnn|MobileNet-v1_models|SSD-ResNet34_models|ssd-resnet-benchmarks|SSD-ResNet34_training_models|bert)
											shopt -u extglob
											CPUFREQ=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor)
											if [ $CPUFREQ == "powersave" ]; then
												sudo cpupower frequency-set -g performance
											fi
										'''

									inference_dict = [
											"ResNet-50-v1.5": ['fp32', 'int8', 'bfloat16'],
											"MobileNet-v1": ['fp32', 'int8'],
											"SSD-MobileNet-v1": ['fp32', 'int8'],
											"SSD-ResNet34": ['fp32', 'int8'],
											"Bert_base": ['fp32'],
											"Bert_tcnt": ['fp32', 'int8', 'bfloat16'],
											'Bert_large': ['fp32', 'bfloat16']
									]
									training_dict = [
											'ResNet-50-v1.5': ['fp32', 'bfloat16'],
											'SSD-ResNet34': ['fp32', 'bfloat16'],
											'MaskRCNN': ['fp32', 'bfloat16'],
											'Transformer_mlperf': ['fp32', 'bfloat16'],
											'Bert_large': ['fp32', 'bfloat16']
									]
									accuracy_dict = [
											"ResNet-50-v1.5": ['fp32', 'int8', 'bfloat16'],
											"MobileNet-v1": ['fp32', 'int8'],
											"SSD-MobileNet-v1": ['fp32', 'int8'],
											"SSD-ResNet34": ['fp32', 'int8'],
											"Bert_base": ['fp32'],
											'Bert_large': ['fp32', 'bfloat16']
									]
									all_dict = [
											"inference": inference_dict,
											"training": training_dict,
											"accuracy": accuracy_dict
									]
									one_test_mode_dict = all_dict[ONE_MODE]
									if(ONE_NAME in one_test_mode_dict.keySet() && ONE_PRE in one_test_mode_dict[ONE_NAME]){
										RETRY_NUM=3
										retry(RETRY_NUM){
											checkout scm
											checkout([
													$class: 'GitSCM',
													branches: [[name: "*/master"]],
													browser: [$class : 'AssemblaWeb',repoUrl: ''],
													doGenerateSubmoduleConfigurations: false,
													extensions: [
															[$class: 'RelativeTargetDirectory', relativeTargetDir: "intelai-models"],
															[$class: 'CloneOption', timeout: 3600]
													],
													submoduleCfg : [],
													userRemoteConfigs: [[credentialsId: git_credential,
																		 url: "https://gitlab.devtools.intel.com/intelai/models.git"
																		]]
											])
											if (ONE_NAME == "MobileNet-v1" && ONE_MODE == "inference") {
												model_name = 'MobileNet-v1_models'
												model_url = "https://github.com/tensorflow/models.git"
												checkout([
														$class: 'GitSCM',
														branches: [[name: "*/master"]],
														browser: [
																$class : 'AssemblaWeb',
																repoUrl: ''
														],
														doGenerateSubmoduleConfigurations: false,
														extensions: [[
																			 $class: 'RelativeTargetDirectory', relativeTargetDir: model_name],
																	 [$class: 'CloneOption', timeout: 3600]
														],
														submoduleCfg: [],
														userRemoteConfigs: [[credentialsId: git_credential,
																			 url: model_url]]
												])
											}
											if (ONE_NAME == "MaskRCNN" && ONE_MODE == "training") {
												model_name = 'mask_rcnn'
												model_url = "https://gitlab.devtools.intel.com/yangshe1/mask_rcnn.git"
												checkout([
														$class: 'GitSCM',
														branches: [[name: "eb38bc21d4b899326dbb6ca4146c6ccc2ea372a1"]],
														browser: [$class : 'AssemblaWeb',repoUrl: ''],
														doGenerateSubmoduleConfigurations: false,
														extensions: [
																[$class: 'RelativeTargetDirectory',relativeTargetDir: model_name],
																[$class: 'CloneOption', timeout: 3600]
														],
														submoduleCfg: [],
														userRemoteConfigs: [[credentialsId: git_credential,
																			 url: model_url
																			]]
												])
											}
											if (ONE_NAME == "SSD-ResNet34" && ONE_MODE == "training") {
												model_name = 'SSD-ResNet34_training_models'
												model_url = "https://github.com/tensorflow/models.git"
												checkout([
														$class: 'GitSCM',
														branches: [[name: "8110bb64ca63c48d0caee9d565e5b4274db2220a"]],
														browser: [$class : 'AssemblaWeb',repoUrl: ''],
														doGenerateSubmoduleConfigurations: false,
														extensions: [
																[$class: 'RelativeTargetDirectory',relativeTargetDir: model_name],
																[$class: 'CloneOption', timeout: 3600]
														],
														submoduleCfg: [],
														userRemoteConfigs: [[credentialsId: git_credential,
																			 url: model_url
																			]]
												])
											}
											if (ONE_NAME == "SSD-ResNet34" && ONE_MODE != "training") {
												model_name = 'SSD-ResNet34_models'
												model_url = "https://github.com/tensorflow/models.git"
												checkout([
														$class: 'GitSCM',
														branches: [[name: "f505cecde2d8ebf6fe15f40fb8bc350b2b1ed5dc"]],
														browser: [$class : 'AssemblaWeb',repoUrl: ''],
														doGenerateSubmoduleConfigurations: false,
														extensions: [
																[$class: 'RelativeTargetDirectory',relativeTargetDir: model_name],
																[$class: 'CloneOption', timeout: 3600]
														],
														submoduleCfg: [],
														userRemoteConfigs: [[credentialsId: git_credential,
																			 url: model_url
																			]]
												])
												model_name = 'ssd-resnet-benchmarks'
												model_url = "https://github.com/tensorflow/benchmarks.git"
												checkout([
														$class: 'GitSCM',
														branches: [[name: "509b9d288937216ca7069f31cfb22aaa7db6a4a7"]],
														browser: [$class : 'AssemblaWeb',repoUrl: ''],
														doGenerateSubmoduleConfigurations: false,
														extensions: [
																[$class: 'RelativeTargetDirectory',relativeTargetDir: model_name],
																[$class: 'CloneOption', timeout: 3600]
														],
														submoduleCfg: [],
														userRemoteConfigs: [[credentialsId: git_credential,
																			 url: model_url
																			]]
												])
											}
											if (ONE_NAME == "Bert_base") {
												model_name = 'bert'
												model_url = "https://github.com/google-research/bert.git"
												checkout([
														$class: 'GitSCM',
														branches: [[name: "88a817c37f788702a363ff935fd173b6dc6ac0d6"]],
														browser: [$class : 'AssemblaWeb',repoUrl: ''],
														doGenerateSubmoduleConfigurations: false,
														extensions: [
																[$class: 'RelativeTargetDirectory',relativeTargetDir: model_name],
																[$class: 'CloneOption', timeout: 3600]
														],
														submoduleCfg: [],
														userRemoteConfigs: [[credentialsId: git_credential,
																			 url: model_url
																			]]
												])
											}
										}
										if(DOUBLE_CHECK == "False"){
											copyArtifacts(
													projectName: "$JOB_NAME",
													selector: specific("${currentBuild.number}"),
													filter: 'tensorflow_**/**',
													fingerprintArtifacts: true,
													target: "$WORKSPACE/")
										}else{
											echo "WHEEL_JOB_NUM ${WHEEL_JOB_NUM}"
											copyArtifacts(
													projectName: "$JOB_NAME",
													selector: specific("${WHEEL_JOB_NUM}"),
													filter: 'tensorflow_**/**',
													fingerprintArtifacts: true,
													target: "$WORKSPACE/")
											copyArtifacts(
													projectName: "$JOB_NAME",
													selector: specific("${WHEEL_JOB_NUM}"),
													filter: '*tf_info.log',
													fingerprintArtifacts: true,
													target: "$WORKSPACE/")
										}
										if(USE_CUSTOM_CONFIG=="True"){
											CONFIG_FILE=CUSTOM_CONFIG_NAME
										}else{
											CONFIG_FILE="${SERVER}_config.ini"
										}
										withEnv(["TF=${ONE_TF}", "MODEL_NAME=${ONE_NAME}", "JOBNAME=${JOB_NAME}", "PRECISION=${ONE_PRE}","TEST_MODE=${ONE_MODE}","CPI=${CORES_PER_INSTANCE}","BUILDNUM=${currentBuild.number}", "SERVER=${SERVER_TYPE}", "CONDA_BIN_PATH=${CONDA_BIN_PATH}", "CONFIG_FILE=${CONFIG_FILE}"]) {
											sh '''#!/bin/bash -x
												# use the jenkins configure node CONDA_BIN_PATH
												export PATH=$CONDA_BIN_PATH:$PATH
												python3 $WORKSPACE/2020_shell_LZ.py -t $TF -m $MODEL_NAME  -p $PRECISION  -tm $TEST_MODE -cpi $CPI -w $WORKSPACE  -jn $JOBNAME -bn $BUILDNUM -st $SERVER -cp $CONDA_BIN_PATH -cfp $WORKSPACE/${CONFIG_FILE}
												bash $WORKSPACE/get_machine_info.sh  |& tee  $WORKSPACE/machine_info.log
											'''
										}
										if(DOUBLE_CHECK == "False"){
											archiveArtifacts 'all_test_log/**,*summary_one.log,machine_info.log'
										}else{
											archiveArtifacts 'all_test_log/**,*summary_one.log,machine_info.log,tensorflow_*_wheel/**,*tf_info.log'
										}
									}
									else{
										println(ONE_NAME + "--" + ONE_MODE + "--" + ONE_PRE + " not able")
									}
								}
							}
						}
					}
				}
			}
		}
		parallel test_jobs
	}
	stage('collect_all_info'){
		checkout scm
		upstream_benchmark_log = "${WORKSPACE}/benchmark/upstream_summary_benchmark.log"
		writeFile file: upstream_benchmark_log,
				text: "Model, Mode, Server, Data_Type, Use_Case, Batch_Size, Result"

		internal_benchmark_log = "${WORKSPACE}/benchmark/internal_summary_benchmark.log"
		writeFile file: internal_benchmark_log,
				text: "Model, Mode, Server, Data_Type, Use_Case, Batch_Size, Result"
		env.task_number = currentBuild.number
		copyArtifacts(
				projectName: currentBuild.projectName,
				selector: specific("${task_number}"),
				filter: 'all_test_log/**,*.log',
				fingerprintArtifacts: true,
				target: "benchmark/")
		withEnv(["internal_benchmark_log=${internal_benchmark_log}","upstream_benchmark_log=${upstream_benchmark_log}"]){
			sh '''#!/bin/bash
				benchmark_log_path=${WORKSPACE}/benchmark
				for file in $(find ${WORKSPACE}/benchmark/ -type f -name '*summary_one.log' | awk -F '/' '{print $(NF)}' )
				do
					if [ ${file:0:8} == "upstream" ]
					then
						echo "" >> ${upstream_benchmark_log}
						cat ${benchmark_log_path}/${file} >> ${upstream_benchmark_log}
					elif [ ${file:0:8} == "internal" ]
					then
						echo "" >> ${internal_benchmark_log}
						cat ${benchmark_log_path}/${file} >> ${internal_benchmark_log}
					fi
				done
				echo "" >> ${upstream_benchmark_log}
				echo "" >> ${internal_benchmark_log}
				cat $benchmark_log_path/*_tf_info.log >> $benchmark_log_path/all_tf.txt
			'''
		}
		if(FIRST_TEST == 'False') {
			if(DOUBLE_CHECK == "False"){
				copyArtifacts(
						projectName: currentBuild.projectName,
						selector: lastSuccessful(),
						filter: 'benchmark/**',
						fingerprintArtifacts: true,
						target: "reference_log/")
				copyArtifacts(
						projectName: currentBuild.projectName,
						selector: lastSuccessful(),
						filter: 'target_log/**',
						fingerprintArtifacts: true,
						target: "./")
			}else{
				copyArtifacts(
						projectName: currentBuild.projectName,
						selector: specific("${WHEEL_JOB_NUM}"),
						filter: 'benchmark/**',
						fingerprintArtifacts: true,
						target: "reference_log/")
				copyArtifacts(
						projectName: currentBuild.projectName,
						selector: lastSuccessful(),
						filter: 'target_log/**',
						fingerprintArtifacts: true,
						target: "./")
			}
		}else{
			copyArtifacts(
					projectName: currentBuild.projectName,
					selector: specific("${task_number}"),
					filter: 'all_test_log/**,*.log',
					fingerprintArtifacts: true,
					target: "reference_log/benchmark/")
			copyArtifacts(
					projectName: currentBuild.projectName,
					selector: specific("${task_number}"),
					filter: 'all_test_log/**,*.log',
					fingerprintArtifacts: true,
					target: "target_log/benchmark/")
			sh '''#!/bin/bash
				cp ${WORKSPACE}/benchmark/upstream_summary_benchmark.log  ${WORKSPACE}/reference_log/benchmark/upstream_summary_benchmark.log
				cp ${WORKSPACE}/benchmark/internal_summary_benchmark.log  ${WORKSPACE}/reference_log/benchmark/internal_summary_benchmark.log
				cp ${WORKSPACE}/benchmark/upstream_summary_benchmark.log  ${WORKSPACE}/target_log/benchmark/upstream_summary_benchmark.log
				cp ${WORKSPACE}/benchmark/internal_summary_benchmark.log  ${WORKSPACE}/target_log/benchmark/internal_summary_benchmark.log
				cp ${WORKSPACE}/benchmark/all_tf.txt  ${WORKSPACE}/reference_log/benchmark/all_tf.txt
				cp ${WORKSPACE}/benchmark/all_tf.txt  ${WORKSPACE}/target_log/benchmark/all_tf.txt
			'''
		}
		TF_ARR = TF_SOURCES.split(',')
		TF_ARR.each{ONE_TF ->
			withEnv(["TF=${ONE_TF}"]){
				sh '''#!/bin/bash -x
					NEW_DATA="$WORKSPACE/benchmark/${TF}_summary_benchmark.log"
					BEST_DATA="$WORKSPACE/target_log/benchmark/${TF}_summary_benchmark.log"
					python3 $WORKSPACE/collect_best_data.py -t ${TF} -w $WORKSPACE/target_log/benchmark  -nd ${NEW_DATA}  -bd ${BEST_DATA}
				'''
			}
		}
		withEnv(["SERVER=${SERVER_TYPE}", "BUILD_CMD=${BUILD_COMMAND}", "build_gcc_version=${build_gcc_version}"]){
			sh '''#!/bin/bash -x
				MACHINE_FILE_NAME='machine_info.log'
				STR_BUILD_CMD="$BUILD_CMD"
				python3 $WORKSPACE/2020_LZ_write_html.py -ul upstream_summary_benchmark.log -il  internal_summary_benchmark.log -ti all_tf.txt -nd $WORKSPACE/benchmark  -rd $WORKSPACE/reference_log/benchmark  \
				-bd  $WORKSPACE/target_log/benchmark  -bu $BUILD_URL  -st $SERVER  -bc "$STR_BUILD_CMD" -gv $build_gcc_version -mi $MACHINE_FILE_NAME
        	'''
		}
	}
	stage('send_report'){
		emailext subject: "Tensorflow LandingZone Models Weekly Report",
				to: "$Recipient",
				replyTo: "$Recipient",
				body: '''${FILE,path="Tensorflow_LandingZone_models_report.html"}''',
				attachmentsPattern: "Tensorflow_LandingZone_models_report.html",
				mimeType: 'text/html'
	}
	stage('upload_all_info'){
		archiveArtifacts 'benchmark/**,reference_log/**,target_log/**'
	}
}