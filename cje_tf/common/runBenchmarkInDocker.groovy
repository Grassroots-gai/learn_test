// Dynamic function runBenchmarkInDocker(docker_image, dataset_location, run_benchmark, target_platform, tensorflow_branch, python_version, single_socket, servername, models, modes)
//
// Parameters:
//
//     docker_image        where to get the docker image 
//
//     dataset_location    dataset directory 
//
//     run_benchmark       whether to run benchmark or convergence 
//
//     target_platform     the target platform: avx, avx2, avx512 
//
//     tensorflow_branch   tensorflow branch name 
//
//     python_version      the python version: can be 2.7, 3.5 or 3.6 
//    
//     single_socket       whether to run single socket or not 
//
//     servername          the host name the docker is running on
// 
//     models              list of models to run, separated by comma
//
//     modes               list of modes to run, training and/or inference, separated by comma
//
// Returns: nothing
//
// External dependencies: None

def call(String docker_image, \
         String dataset_location, \
         Boolean run_benchmark, \
         String target_platform, \
         String tensorflow_branch, \
         String python_version, \
         Boolean single_socket, \
         String servername, \
         String models, \
         String modes, \
         Boolean run_q1models, \
         Boolean run_q2models) {

    workspace_volumn="${WORKSPACE}:/workspace"
    dataset_volume="${dataset_location}:${dataset_location}"

    docker.image("$docker_image").inside("--env \"http_proxy=${http_proxy}\" \
                                          --env \"https_proxy=${https_proxy}\" \
                                          --volume ${workspace_volumn} \
                                          --volume ${dataset_volume} \
                                          --env DATASET_LOCATION=$dataset_location \
                                          --env RUN_BENCHMARK=$run_benchmark \
                                          --env TARGET_PLATFORM=$target_platform \
                                          --env TENSORFLOW_BRANCH=$tensorflow_branch \
                                          --env PYTHON=$python_version \
                                          --env SINGLE_SOCKET=$single_socket \
                                          --env SERVERNAME=$servername \
                                          --env MODELS=$models \
                                          --env MODES=$modes \
                                          --env RUN_Q1MODELS=$run_q1models \
                                          --env RUN_Q2MODELS=$run_q2models \
                                          --privileged \
                                          -u root:root") {

        sh '''#!/bin/bash -x
        env | grep -i proxy
        python --version
        pip list
        
        if [ -d "/private-tensorflow" ]; then
            python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=/private-tensorflow --workspace_dir=/workspace
        elif [ -d "/tensorflow" ]; then
            python /workspace/cje-tf/scripts/get_tf_sourceinfo.py --tensorflow_dir=/tensorflow --workspace_dir=/workspace
        fi

        chmod 775 /workspace/cje-tf/scripts/run_docker_benchmark_py2.sh
        /workspace/cje-tf/scripts/run_docker_benchmark_py2.sh
        '''
    }

}

return this;
