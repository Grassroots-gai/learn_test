#!/bin/bash
#set -x
export PATH="/opt/tensorflow/java/jdk1.8.0_131/bin:/opt/tensorflow/bazel/bin:/opt/tensorflow/gcc/gcc6.2/bin:$PATH"
export LD_LIBRARY_PATH="/opt/tensorflow/gcc/gcc6.2/lib64"
export JAVA_HOME="/opt/tensorflow/java/jdk1.8.0_131"
PYTHON_BIN_PATH="/usr/bin/python" 
PYTHON_LIB_PATH="/usr/lib/python2.7/site-packages"

intel_tensorflow_url="git@github.com:Intel-tensorflow/tensorflow.git"
public_tensorflow_url="git@github.com:tensorflow/tensorflow.git"
#private_tensorflow_url="git@github.com:NervanaSystems/private-tensorflow.git"
private_tensorflow_url="git@https://gitlab.devtools.intel.com/TensorFlow/Direct-Optimization/private-tensorflow.git"
private_tensorflow_dir="private-tensorflow"
tensorflow_dir="tensorflow"

validation_root="/mnt/aipg_tensorflow_shared/validation"
if [ ! -d ${validation_root} ]
then
  echo "Validation root directory doesn't exist: ${validation_root}"
  exit 1
fi
builddir="${validation_root}/build" 
scriptsloc="${validation_root}/scripts"
loglocation="${validation_root}/logs"

cd ${validation_root} 
#if this file exists, won't update the master
if [ -f ${validation_root}/_DONOTRUN_UPDATE_REPO ] 
then
  echo "Did not update repos today, quitting"
  echo "Remove the file ${validation_root}/_DONOTRUN_UPDATE_REPO to run the update script"
  exit 1
fi
date=`date "+%Y_%m_%d"`
### update the private tensorflow
rebasedir=${builddir}/repo_for_rebase

function update_private_tensorflow {
    echo "------>Updating Private Repo<-------"
    mkdir -p ${rebasedir}
    cd ${rebasedir}
    rm -rf ${private_tensorflow_dir}
    #git clone https://github.com/NervanaSystems/private-tensorflow.git 
    git clone ${private_tensorflow_url}
    if [ $? -ne 0 ]
    then
      echo "could not clone private repo"
      exit 1
    fi
    cd ${private_tensorflow_dir}
    #git remote add upstream https://github.com/tensorflow/tensorflow.git
    git remote add upstream ${public_tensorflow_url}
    if [ $? -ne 0 ]
    then
      echo "could not set remote public repo"
      exit 1
    fi
    git fetch upstream
    if [ $? -ne 0 ]
    then
      echo "could not fetch private repo"
      exit 1
    fi
    git checkout master
    if [ $? -ne 0 ]
    then
      echo "checkout failed  private repo"
      exit 1
    fi
    git rebase upstream/master
    if [ $? -ne 0 ]
    then
      echo "rebase failed private repo"
      exit 1
    fi
    git push -f origin master
    if [ $? -ne 0 ]
    then
      echo "push failed private repo"
      exit 1
    fi

}

function update_intel_tensorflow {
    ### update the public tensorflow
    echo "------>Updating Intel Public  Repo<-------"
    cd ${rebasedir}
    rm -rf ${tensorflow_dir}
    #git clone https://github.com/Intel-tensorflow/tensorflow.git
    git clone ${intel_tensorflow_url}
    if [ $? -ne 0 ]
    then
      echo "could not clone public repo"
      exit 1
    fi
    cd ${tensorflow_dir}
    #git remote add upstream https://github.com/tensorflow/tensorflow.git
    git remote add upstream ${public_tensorflow_url}
    if [ $? -ne 0 ]
    then
      echo "could set remote public repo"
      exit 1
    fi
    git fetch upstream
    if [ $? -ne 0 ]
    then
      echo "could not fetch  public repo"
      exit 1
    fi
    git checkout master
    if [ $? -ne 0 ]
    then
      echo "checkout failed public repo"
      exit 1
    fi
    git rebase upstream/master
    if [ $? -ne 0 ]
    then
      echo "rebase failed public repo"
      exit 1
    fi
    git push -f origin master
    if [ $? -ne 0 ]
    then
      echo "push failed public repo"
      exit 1
    fi
}

function prep_build {
    echo "----------->Preparing to build<---------------"
    mkdir -p ${builddir}/eigentest
    cd ${builddir}/eigentest
    rm -rf ${private_tensorflow_dir}
    #git clone https://github.com/NervanaSystems/private-tensorflow.git
    git clone ${private_tensorflow_url}
    if [ $? -ne 0 ]
    then
      echo "git clone failed"
      exit 1
    fi
    cd ${private_tensorflow_dir}
    if [ ! -f ${scriptsloc}/patchfiles.sh ] 
    then
      echo "ERROR!!! Unable to find the patch file: ${scriptsloc}/patchfiles.sh"
      exit 1
    fi
    ${scriptsloc}/patchfiles.sh

    echo "----------->Setting Config Environment Variables<---------------"
    export PYTHON_BIN_PATH=${PYTHON_BIN_PATH} \
        PYTHON_LIB_PATH=${PYTHON_LIB_PATH} \
        CC_OPT_FLAGS='-march=native' \
        TF_NEED_JEMALLOC=1 \
        TF_NEED_GCP=1 \
        TF_NEED_HDFS=1 \
        TF_NEED_S3=1 \
        TF_NEED_KAFKA=0 \
        TF_ENABLE_XLA=0 \
        TF_NEED_GDR=0 \
        TF_NEED_VERBS=0 \
        TF_NEED_OPENCL=0 \
        TF_NEED_OPENCL_SYCL=0 \
        TF_NEED_CUDA=0 \
        TF_NEED_MPI=0 \
        TF_SET_ANDROID_WORKSPACE=0
    echo $(env | grep "TF_")

    echo "-------->Running Configure<----------"
    ./configure

    if [ $? -ne 0 ]
    then
      echo "configure failed"
      exit 1
    fi
}

############## EIGEN #######################
function build_eigen {
    echo "----------->Building EIGEN version<---------------"
    mkdir -p ${builddir}/eigendepend 
    eigentest_logdir="${loglocation}/eigentest"
    current_build_logfile="${eigentest_logdir}/current_build.txt"
    if [ ! -d "${builddir}/eigentest/${private_tensorflow_dir}" ]
    then
      echo "Eigen build directory doesn't exist: ${builddir}/eigentest/${private_tensorflow_dir}"
      exit 1
    fi
    cd "${builddir}/eigentest/${private_tensorflow_dir}"
    #now in ${builddir}/eigentest/${private_tensorflow_dir}
    bazel --output_base=${builddir}/eigendepend test --copt="-mfma" --copt="-mavx2" --copt="-march=broadwell" --test_timeout 300,450,1200,3600 --copt="-O3" -s --cache_test_results=no -c opt  -- //tensorflow/... -//tensorflow/compiler/xla/... > ${current_build_logfile} 2>&1
    fgrep Executed ${current_build_logfile} > /dev/null 2>&1
    if [ $? -ne 0 ]
    then
      echo "Test failed to execute"
      exit 1
    fi
    fgrep "were skipped" ${current_build_logfile} > /dev/null 2>&1
    if [ $? -eq 0 ]
    then
      echo "Some tests skipped, unsure of results, leaving eigen.failures as is"
      exit 0
    fi
    mkdir - p ${eigentest_logdir}/eigen-fails
    cp ${loglocation}/eigen.failures ${eigentest_logdir}/eigen-fails/eigen.failures_$date
    fgrep FAILED ${current_build_logfile}  | sed 's/[ ][ ]*.*//' > ${loglocation}/eigen.failures

    cp ${loglocation}/eigen.logs.tar.gz  ${eigentest_logdir}/eigen-fails/eigen.logs.tar.gz_$date
    $scriptsloc/get_my_failures.sh ${current_build_logfile} all
    cp logs.tar.gz  ${loglocation}/eigen.logs.tar.gz

}


#### RUN ML ####################
function test_mkl_ml {
    echo "------------>Running MKL_ML tests<-----------------"
    bazel --output_base=${builddir}/eigendepend test --config=mkl --copt="-mfma" --copt="-mavx2" --copt="-march=broadwell" --test_timeout 300,450,1200,3600 --copt="-O3" -s --cache_test_results=no -c opt  -- //tensorflow/... -//tensorflow/compiler/xla/... > ${current_build_logfile}.ml 2>&1
    fgrep Executed ${current_build_logfile}.ml > /dev/null 2>&1
    if [ $? -ne 0 ]
    then
      echo "ML Test failed to execute"
    else
      fgrep "were skipped"  ${current_build_logfile}.ml  > /dev/null 2>&1
      if [ $? -eq 0 ]
      then
        echo "Some tests skipped, unsure of results, leaving ML failures as is"
      else
        cp ${loglocation}/ml.failures ${loglocation}/ml/ml-fails/ml.failures_$date
        fgrep FAILED ${current_build_logfile}.ml  | sed 's/[ ][ ]*.*//' > ${loglocation}/ml.failures
        cp ${loglocation}/ml.logs.tar.gz  ${loglocation}/ml/ml-fails/ml.logs.tar.gz_$date
        $scriptsloc/get_my_failures.sh ${current_build_logfile}.ml all
        cp logs.tar.gz  ${loglocation}/ml.logs.tar.gz

        cp ${loglocation}/new-ml.logs.tar.gz  ${loglocation}/ml/ml-fails/new-ml.logs.tar.gz_$date
        $scriptsloc/get_my_failures.sh ${current_build_logfile}.ml ml
        cp logs.tar.gz  ${loglocation}/new-ml.logs.tar.gz

        echo "ML failures have been logged"
      fi
    fi
}

#### RUN DNN ####################
function test_mkl_dnn {
    bazel --output_base=${builddir}/eigendepend test --config=mkl --copt="-mfma" --copt="-mavx2" --copt="-march=broadwell" --copt=-DINTEL_MKL_DNN --test_timeout 300,450,1200,3600 --copt="-O3" -s --cache_test_results=no -c opt  -- //tensorflow/... -//tensorflow/compiler/xla/... > ${current_build_logfile}.dnn 2>&1
    fgrep Executed ${current_build_logfile}.dnn > /dev/null 2>&1
    if [ $? -ne 0 ]
    then
      echo "DNN Test failed to execute"
    else
      fgrep "were skipped"  ${current_build_logfile}.dnn  > /dev/null 2>&1
      if [ $? -eq 0 ]
      then
        echo "Some tests skipped, unsure of results, leaving DNN failures as is"
      else
        cp ${loglocation}/dnn.failures ${loglocation}/dnn/dnn-fails/dnn.failures_$date
        fgrep FAILED ${current_build_logfile}.dnn  | sed 's/[ ][ ]*.*//' > ${loglocation}/dnn.failures
        cp ${loglocation}/dnn.logs.tar.gz  ${loglocation}/dnn/dnn-fails/dnn.logs.tar.gz_$date
        $scriptsloc/get_my_failures.sh ${current_build_logfile}.dnn all
        cp logs.tar.gz  ${loglocation}/dnn.logs.tar.gz

        cp ${loglocation}/new-dnn.logs.tar.gz  ${loglocation}/dnn/dnn-fails/new-dnn.logs.tar.gz_$date
        $scriptsloc/get_my_failures.sh ${current_build_logfile}.dnn dnn
        cp logs.tar.gz  ${loglocation}/new-dnn.logs.tar.gz

        echo "DNN failures have been logged"
      fi
    fi
}

update_private_tensorflow
update_intel_tensorflow
prep_build
build_eigen
echo "All repos sucessfully updated and eigen failures have been logged"
exit 0

test_mkl_ml
test_mkl_dnn
