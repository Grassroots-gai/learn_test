#!/bin/bash -x

if [ -z ${WORKSPACE} ] ; then
    WORKSPACE=/workspace
fi

if [ -z ${TENSORFLOW_DIR} ] ; then
    TENSORFLOW_DIR="tensorflow"
fi

if [ -z ${TENSORFLOW_BRANCH} ] ; then
    TENSORFLOW_BRANCH="v1.8.0"
fi

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

echo $WORKSPACE
echo $TENSORFLOW_DIR
echo $TARGET_PLATFORM
echo $TENSORFLOW_BRANCH
echo $PYTHON
echo $RUN_EIGEN
echo $UNITTESTLOG
echo $DATASET_LOCATION

echo $(env)
echo $(env | grep "JAVA_HOME")
unset JAVA_HOME
echo $(JAVA_HOME)
echo $(env | grep "LD_LIBRARY_PATH")
unset LD_LIBRARY_PATH
echo $(LD_LIBRARY_PATH)

cd $WORKSPACE/$TENSORFLOW_DIR
echo "--------> Running Configure with python${PYTHON} <----------"
yes "" | python${PYTHON} configure.py

if [ $? -ne 0 ]; then
    echo "configure failed"
    exit 1
else
    echo "configured complete"
fi

GCC_VERSION="-gcc4.8"
cp $WORKSPACE/tensorflow-common/build-env/set_${TARGET_PLATFORM}_build${GCC_VERSION} ./build_option
source build_option

if [ "$TENSORFLOW_BRANCH" == "master" ]; then
    build_test="-- //tensorflow/...  -//tensorflow/compiler/...  -//tensorflow/lite/... -//tensorflow/core:test_lite_main -//tensorflow/contrib/tpu/profiler/... -//tensorflow/contrib/tensorrt/... -//tensorflow/contrib/gdr/... -//tensorflow/stream_executor/cuda/..."
else
    build_test="-- //tensorflow/...  -//tensorflow/compiler/...  -//tensorflow/contrib/lite/... -//tensorflow/core:test_lite_main -//tensorflow/contrib/tpu/profiler/...  -//tensorflow/contrib/tensorrt/... -//tensorflow/contrib/tpu/... -//tensorflow/stream_executor/cuda/..."
fi

cd $WORKSPACE/$TENSORFLOW_DIR
if [ "$RUN_EIGEN" == true ];  then

    mkl=""
    eigen_log="eigen_build_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}.log"
    test_log="${WORKSPACE}/${eigen_log}"
    summary_log="$WORKSPACE/eigen_${TENSORFLOW_BRANCH}_${TARGET_PLATFORM}_Python_${PYTHON}_summary.log"
    output_user_root="$WORKSPACE/eigen_build"
    bazel --nosystem_rc --nohome_rc --output_user_root=${output_user_root} test ${BAZEL_SECURE_BUILD_OPTS} --test_timeout 300,450,1200,3600 --test_env=KMP_BLOCKTIME=0 -s --cache_test_results=no -c opt ${build_test} >& ${test_log}

    eigen_failures="${WORKSPACE}/eigen.failures"
    fgrep FAILED ${test_log}  | sed 's/[ ][ ]*.*//' > ${eigen_failures}

else

    mkl="--config=mkl"
    test_log=$UNITTESTLOG
    output_user_root="$WORKSPACE/test"
    bazel --nosystem_rc --nohome_rc --output_user_root=${output_user_root} test ${BAZEL_SECURE_MKL_BUILD_OPTS} --verbose_failures --test_verbose_timeout_warnings --local_test_jobs 4 --flaky_test_attempts 3 --test_timeout 300,450,1200,3600  -c opt  ${build_test}  >& ${test_log}

fi

exit 0

