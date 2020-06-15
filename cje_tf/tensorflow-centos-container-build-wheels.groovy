static final String node_label = params.get('NODE_LABEL', 'skx')
static final String tf_common_branch = params.get('TF_COMMON_BRANCH', 'master')
static final String tf_build_version = params.get('TF_BUILD_VERSION', 'master')
static final String tf_build_repo = params.get('TF_BUILD_REPO', 'https://github.com/tensorflow/tensorflow')
static final String bazel_version = params.get('BAZEL_VERSION', '0.24.1')
static final String tf_bazel_options = params.get('OPTIONAL_BAZEL_BUILD_OPTIONS', '')

// TODO: For now just check for first char in the string until we have the Character.isDigit whitelisted
// static final String docker_tag_version = (tf_build_version.startsWith("v") && Character.isDigit(tf_build_version.charAt(1)) ? tf_build_version.substring(1) : tf_build_version) + "-devel-mkl"
static final String docker_tag_version = (tf_build_version.startsWith("v") ? tf_build_version.substring(1) : tf_build_version) + "-devel-mkl"
static final String tf_common_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/Shared/tensorflow-common.git"
static final String cje_tf_repo_url = "https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git"
static final String jenkins_git_credentials = "lab_tfbot"
static final String jenkins_git_credentials_lab = "lab_tfbot"


static final String docker_registry_url =  'https://amr-registry.caas.intel.com'
static final String docker_registry_credentials = 'lab_tfbot'

// Check if building secure wheels is requested
String enable_secure_build = 'no'
if (ENABLE_SECURE_BUILD == "true") {
    enable_secure_build='yes'
}

// set to 0 if Python3.7 is not supported
static int is_py37_supported = 1

static ArrayList docker_images = [
    "amr-registry.caas.intel.com/aipg-tf/dev/centos7:"+docker_tag_version,
    "amr-registry.caas.intel.com/aipg-tf/dev/centos7:"+docker_tag_version+"-py3",
    "amr-registry.caas.intel.com/aipg-tf/dev/centos7:"+docker_tag_version+"-py3.4",
    "amr-registry.caas.intel.com/aipg-tf/dev/centos7:"+docker_tag_version+"-py3.5",
    "amr-registry.caas.intel.com/aipg-tf/dev/centos7:"+docker_tag_version+"-py3.6",
    "amr-registry.caas.intel.com/aipg-tf/dev/centos7:"+docker_tag_version+"-py3.7"
]

// Python3.7 is only supported for TensorFlow v1.13.1 and newer
if (
    (tf_build_version.startsWith("v1.") && tf_build_version < "v1.13.1") ||
    (tf_build_version.startsWith("r1.") && tf_build_version < "r1.13")) {
    if (docker_images.size() > 0) {
        docker_images.remove(docker_images.size() - 1)
        is_py37_supported = 0
    }
}

node(node_label) {

    try {
        stage('CleanUp') {

            // TODO: put back
            sh '''#!/bin/bash -x
                cd $WORKSPACE
                sudo rm -rf *
                # docker stop $(docker ps -a -q)
                # echo Y | docker system prune -a
            '''
        }

        // pull the cje-tf
        dir( 'cje-tf' ) {
            checkout scm
        }

        stage('Checkout') {

            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: tf_common_branch]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                           relativeTargetDir: 'tensorflow-common']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials_lab",
                                                           url          : "$tf_common_repo_url"]]])


        }

        stage('Build') {
            // build dev containers, will contain wheels
            sh """#!/bin/bash -x
            set -e

            pushd tensorflow-common/build-env
            source set_avx_build-gcc4.8
            popd
            pushd $WORKSPACE/cje-tf/docker
            if [ "${enable_secure_build}" == "yes" ]; then
              export TF_BAZEL_BUILD_OPTIONS=\$BAZEL_SECURE_MKL_BUILD_OPTS
            else
              export TF_BAZEL_BUILD_OPTIONS=\$BAZEL_MKL_BUILD_OPTS_BASIC
            fi
            TF_OPTIONAL_BAZEL_BUILD_OPTIONS=${tf_bazel_options} TF_BUILD_REPO=${tf_build_repo} TF_BUILD_VERSION="${tf_build_version}" BAZEL_VERSION="${bazel_version}" IS_PY37_SUPPORTED="${is_py37_supported}" ./intel-build-dev-container-centos.sh

            # extract tf created wheels from internal images
            # puts the wheels into $WORKSPACE/publish
            OUTPUT_DIR=$WORKSPACE/publish
            mkdir -p \$OUTPUT_DIR

            TF_VER=${tf_build_version}

            # remove any old containers if hanging around, can't force-rm running container for some reason
            docker stop temp-centos || true
            docker rm temp-centos || true

            # copy wheel files into output_dir
            cp $WORKSPACE/cje-tf/docker/scripts/*.whl $WORKSPACE/publish

            OUTPUT_DIR=$WORKSPACE/publish
            pushd \$OUTPUT_DIR

            # convert to intel-tensorflow wheels
            # need centos7 image because of kernel version containing right lib files
            docker run -d --name temp-centos \
                --env "http_proxy=${http_proxy}" --env "https_proxy=${https_proxy}" \
                --mount src=\$(pwd),dst=/wheels,type=bind amr-registry.caas.intel.com/aipg-tf/dev/centos7:py3 \
                /bin/bash -c "sleep infinity & wait"
            docker exec temp-centos bash -c '
                yum install -y patchelf &&
                pip install auditwheel==1.5 virtualenv wheel==0.31.1 &&
                for file in /wheels/*.whl; do
                    auditwheel repair \$file
                done
                # copy new wheels to wheel dir
                cp /wheelhouse/*.whl /wheels
            ' || true
            docker stop temp-centos
            docker rm temp-centos

            # delete old tensorflow wheels so as to not make them new packages
            find . -name "tensorflow-*" -not -path "*manylinux1*" -exec rm {} \\;

            # this wheel version needed for making new packages
            python -m pip install virtualenv
            virtualenv -p python3 venv
            . venv/bin/activate
            pip install wheel==0.32.2
            $WORKSPACE/cje-tf/scripts/make_new_package_name.py tensorflow intel_tensorflow
            deactivate

            # delete tensorflow manylinux1 wheels since we have intel-tensorflow ones
            find . -name "tensorflow-*" -exec rm {} \\;

            # create shasums
            for file in intel_tensorflow*; do
                sha256sum \$file > \$file.sha256sum
            done

            popd
            """
        }

        stage('Push') {
            // push to public internal registry, getting latest CA chain from Intel's PKI
            // to fix this issue, we do below. It works already on some nodes but not all, see https://soco.intel.com/groups/caas-evaluation-workgroup/blog/2018/11/13/how-to-using-container-registry-and-its-various-features
            // -- docker push amr-registry.caas.intel.com/aipg-tf/dev:centos7
            //      The push refers to repository [amr-registry.caas.intel.com/aipg-tf/dev]
            //      Get https://amr-registry.caas.intel.com/v2/: x509: certificate signed by unknown authority
            sh """
            # depending on machine, regardless we want unzip
            sudo apt-get -y -qq install unzip || sudo yum install -y unzip
            http_proxy='' &&\
              curl http://certificates.intel.com/repository/certificates/IntelSHA2RootChain-Base64.zip > /tmp/IntelSHA2RootChain-Base64.zip
            yes | sudo unzip -o /tmp/IntelSHA2RootChain-Base64.zip -d /usr/local/share/ca-certificates/
            rm /tmp/IntelSHA2RootChain-Base64.zip
            sudo update-ca-certificates || sudo update-ca-trust
            sudo service docker restart
            """
            docker.withRegistry(docker_registry_url, docker_registry_credentials) {
                for (dockerImage in docker_images) {
                    docker.image(dockerImage).push()
                }
            }
        }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
        stage('Archive Tensorflow Wheels') {
            dir("$WORKSPACE" + "/publish") {
                archiveArtifacts artifacts: '*.whl', excludes: null
                fingerprint: true

                def server = Artifactory.server 'ubit-artifactory-or'
                def uploadSpec = """{
              "files": [
               {
                   "pattern": "*.whl",
                   "target": "aipg-local/aipg-tf/${env.JOB_NAME}/${env.BUILD_NUMBER}/"
               }
               ]
            }"""
                def buildInfo = server.upload(uploadSpec)
                server.publishBuildInfo(buildInfo)

            }
        }
    } // finally
}
