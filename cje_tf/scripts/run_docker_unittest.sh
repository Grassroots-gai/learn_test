#!/bin/bash -x

env
echo "http_proxy is $http_proxy"
echo "https_proxy is $https_proxy"
echo "HTTP_PROXY is $HTTP_PROXY"
echo "$HTTPS_PROXY is$HTTPS_PROXY"

if [ -z ${WORKSPACE} ] ; then
    WORKSPACE=/workspace
fi

if [ -z ${TENSORFLOW_DIR} ] ; then
    TENSORFLOW_DIR="tensorflow"
fi

if [ -z ${TENSORFLOW_BRANCH} ] ; then
    TENSORFLOW_BRANCH="v1.8.0"
fi
TENSORFLOW_BRANCH="${TENSORFLOW_BRANCH//\//_}"

if [ -z ${TARGET_PLATFORM} ] ; then
    TARGET_PLATFORM="avx"
fi

if [ -z ${PYTHON} ] ; then
    PYTHON="2.7"
fi

if [ -z ${RUN_EIGEN} ] ; then
    RUN_EIGEN=false
fi

if [ -z ${UNITTESTLOG} ] ; then
    UNITTESTLOG="${WORKSPACE}/unit_test_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
fi

if [ -z ${DISABLE_MKL} ] ; then
    DISABLE_MKL=false
fi

if [ -z ${DISTR} ] ; then
    DISTR="ubuntu"
fi

if [ -z ${OPTIONAL_BAZEL_TEST_OPTIONS} ] ; then
    OPTIONAL_BAZEL_TEST_OPTIONS=""
fi

if [ -z ${MR_NUMBER} ] ; then
    MR_NUMBER=""
fi

echo WORKSPACE=$WORKSPACE
echo TENSORFLOW_DIR=$TENSORFLOW_DIR
echo TARGET_PLATFORM=$TARGET_PLATFORM
echo TENSORFLOW_BRANCH=$TENSORFLOW_BRANCH
echo PYTHON=$PYTHON
echo RUN_EIGEN=$RUN_EIGEN
echo UNITTESTLOG=$UNITTESTLOG
echo DATASET_LOCATION=$DATASET_LOCATION
echo DISTR=$DISTR
echo TEST_TO_SKIP=${TEST_TO_SKIP}
echo BAZEL_VERSION=${BAZEL_VERSION}
echo OPTIONAL_BAZEL_TEST_OPTIONS=${OPTIONAL_BAZEL_TEST_OPTIONS}
echo http_proxy=${http_proxy}
echo https_proxy=${https_proxy}
echo MR_NUMBER=${MR_NUMBER}

# debug
env | grep "JAVA_HOME"
unset JAVA_HOME
env | grep "JAVA_HOME"
env | grep "LD_LIBRARY_PATH"
unset LD_LIBRARY_PATH
env | grep "LD_LIBRARY_PATH"
env | grep -i proxy

# setting proxy again just to ensure....
export http_proxy=http://proxy-chain.intel.com:911
export https_proxy=http://proxy-chain.intel.com:911 
echo "http_proxy is $http_proxy"
echo "https_proxy is $https_proxy"

# Convert the list of tests to skip to a space separated list
IFS=';' read -ra TEST <<< "$TEST_TO_SKIP"
TEST_TO_SKIP=""
for i in "${TEST[@]}"; do
    TEST_TO_SKIP="${TEST_TO_SKIP} ${i}"
done
echo TEST_TO_SKIP=${TEST_TO_SKIP}

# check python 
p_version=$(python -V 2>&1)
case "$p_version" in
*3.4*)
    PIP="python3.4 -m pip"
    ;;
*3.5*)
    PIP="python3.5 -m pip"
    ;;
*3.6*)
    PIP="python3.6 -m pip"
    ;;
*3.7*)
    PIP="python3.7 -m pip"
    ;;
*)
    PIP="pip"
    ;;
esac
echo ${PIP}

# check if bazel is installed
`which bazel`
if [ $? == 1 ] || [ ! -z ${BAZEL_VERSION} ]; then
    echo "bazel not found or installing a bazel version ${BAZEL_VERSION}"

    apt-get clean; apt-get update -y || yum clean all; yum update -y
    apt-get install wget unzip zip openjdk-8-jdk -y || yum install wget unzip zip openjdk-8-djk -y
    wget https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh
    export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
    echo $JAVA_HOME
    chmod 775 bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh
    ./bazel-${BAZEL_VERSION}-installer-linux-x86_64.sh --prefix=/usr/local/
    ${PIP} install future
fi

# now we always checkout tensorflow under workspace
WORK_DIR="${WORKSPACE}/${TENSORFLOW_DIR}"

GCC_VERSION=""
if [ "${DISTR}" == "centos" ]; then 
	GCC_VERSION="-gcc4.8"
	yum install -y java-1.8.0-openjdk-devel.x86_64
	export JAVA_HOME=/usr/lib/jvm/java-1.8.0
	echo $JAVA_HOME

	if [ "${PYTHON}" == "3.5" ]; then    
		PIP="python3.5 -m pip"
	elif [ "${PYTHON}" == "3.6" ]; then    
		PIP="python3.6 -m pip"
	fi
	echo ${PIP}
	${PIP} install tensorflow_estimator
fi

${PIP} install portpicker
${PIP} install future>=0.17.1

if [ "${TENSORFLOW_BRANCH}" == "master" ] || [ "${MR_NUMBER}" != "" ] ; then
    ${PIP} install --upgrade  tf-estimator-nightly
fi

${PIP} list

echo WORK_DIR=${WORK_DIR}
cd ${WORK_DIR}
echo "--------> Running Configure <----------"
yes "" | python configure.py

if [ $? -ne 0 ]; then
    echo "configure failed"
    exit 1
else
    echo "configured complete"
fi

cp $WORKSPACE/tensorflow-common/build-env/set_${TARGET_PLATFORM}_build${GCC_VERSION} ./build_option
source build_option

if [ "$TENSORFLOW_BRANCH" == "master" ]; then
    build_test="-- //tensorflow/...  -//tensorflow/compiler/...  -//tensorflow/lite/... -//tensorflow/stream_executor/cuda/... ${TEST_TO_SKIP}"
else
	if [ "${DISTR}" == "centos" ]; then 
		build_test="-- //tensorflow/...  -//tensorflow/compiler/...  -//tensorflow/lite/... -//tensorflow/core:example_protos -//tensorflow/core:example_protos_closure -//tensorflow/core:example_java_proto ${TEST_TO_SKIP}"
	else
	    build_test="-- //tensorflow/...  -//tensorflow/compiler/...  -//tensorflow/lite/... -//tensorflow/stream_executor/cuda/... ${TEST_TO_SKIP}"
	fi
fi

if [ "$DISABLE_MKL" == true ];  then
    export TF_DISABLE_MKL=1
    export TF_CPP_MIN_VLOG_LEVEL=1
fi

cd $WORK_DIR
if [ "$RUN_EIGEN" == true ];  then

    mkl=""
    eigen_log="eigen_build_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
    test_log="${WORKSPACE}/${eigen_log}"
    summary_log="$WORKSPACE/eigen_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}_summary.log"
    output_user_root="$WORKSPACE/eigen_build"
    bazel --nosystem_rc --nohome_rc --output_user_root=${output_user_root} test ${BAZEL_SECURE_BUILD_OPTS} ${OPTIONAL_BAZEL_TEST_OPTIONS} --test_timeout 300,450,1200,3600 --test_env=KMP_BLOCKTIME=0 -s --cache_test_results=no  --test_size_filters=small,medium,large,enormous -c opt ${build_test} >& ${test_log}
    eigen_failures="${WORKSPACE}/eigen.failures"
    fgrep "FAILED in" ${test_log}  | sed 's/[ ][ ]*.*//' > ${eigen_failures}

else

    mkl="--config=mkl"
    test_log=$UNITTESTLOG
    output_user_root="$WORKSPACE/test"
    OMP_PARAM="--action_env=OMP_NUM_THREADS=10"

    bazel --nosystem_rc --nohome_rc --output_user_root=${output_user_root} test ${OMP_PARAM} ${OMP_LIB_PATH} ${BAZEL_SECURE_MKL_BUILD_OPTS} ${OPTIONAL_BAZEL_TEST_OPTIONS} --verbose_failures --test_verbose_timeout_warnings --flaky_test_attempts 3 --test_timeout 300,450,1200,3600 --test_size_filters=small,medium,large,enormous  -c opt  ${build_test}  >& ${test_log}
    ut_failures="${WORKSPACE}/ut.failures"
    fgrep "FAILED in" ${test_log}  | sed 's/[ ][ ]*.*//' > ${ut_failures}

fi

exit 0

