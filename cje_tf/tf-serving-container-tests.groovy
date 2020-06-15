static final String node_label = params.get('NODE_LABEL', 'skx' || 'bdw')

static final String tf_serving_repo_url = "https://github.com/tensorflow/serving.git"
static final String tf_serving_repo_branch = params.get('TF_SERVING_BRANCH', 'master')

static final String py_ver = params.get('PY_VERSION', '2')

static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"

static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

static final String docker_image_name = params.get('IMAGE_NAMESPACE', 'amr-registry.caas.intel.com/aipg-tf/tf-serving')
static final String docker_build_version = "intel-tf-serving-master"

// TODO: add -py3 options when that is available
// TODO: any way to combine this stuff and have helper functions?
// this is gonna be a sorta copy paste from tf-serving-container-build
static final String docker_py2 = "${docker_image_name}:${docker_build_version}-py2"
static final String docker_py2_mkl_devel = "${docker_image_name}:${docker_build_version}-py2-mkl-devel"
static final ArrayList docker_images = [ docker_py2, docker_py2_mkl_devel ]

static final String bazel_version = "0.20.0"

node(node_label) {
    try {
        stage('CleanUp') {
            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                docker stop $(docker ps -a -q)
                echo Y | docker system prune -a
            '''
        }

        // pull the cje-tf
        dir( 'cje-tf' ) {
            checkout scm
        }

        stage('Checkout') {

            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: tf_serving_repo_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-serving']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[url: "$tf_serving_repo_url"]]])


        }

        stage('Pull') {
            // Pull from internal repo
            docker.withRegistry(docker_registry_url) {
                for (dockerImage in docker_images) {
                    docker.image(dockerImage).pull()
                }
            }
        }

        stage('Test') {

            // install bazel if need be and go into tests folder
            sh """#!/bin/bash -x
            install_bazel=1

            # check if command exists
            command -v bazel
            bazel_exists=\$(echo \$?)
            # if command exists, check if right version
            if [[ \$bazel_exists == "0" ]]; then
                bazel version | grep ${bazel_version}
                install_bazel=\$(echo \$?)
            fi

            # if either command doesn't exist or wrong version, then install again
            if [[ \$install_bazel == "1" ]]; then
                pushd /tmp
                wget --quiet https://github.com/bazelbuild/bazel/releases/download/${bazel_version}/bazel-${bazel_version}-linux-x86_64
                chmod 755 bazel-${bazel_version}-linux-x86_64
                # remove any old version
                sudo rm -rf /usr/local/bin/bazel
                sudo ln -sf \$(pwd)/bazel-${bazel_version}-linux-x86_64 /usr/local/bin/bazel
                popd
            fi
            """

            // test things
            for (dockerImage in docker_images) {
                withEnv(["dockerImage=$dockerImage"]) {
                    sh """#!/bin/bash -x
                    cd tensorflow-serving/tensorflow_serving/tools/docker/tests
                    # check if mkl image, if so run mkl tests
                    if [[ \$dockerImage == *"mkl"* ]]; then
                        bazel test :unittest_dockerfile_devel_mkl --test_arg=${dockerImage} --test_output=streamed --verbose_failures
                    else
                        bazel test :unittest_dockerfile_devel --test_arg=${dockerImage} --test_output=streamed --verbose_failures
                    fi
                    """
                }
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {

    } // finally
}
