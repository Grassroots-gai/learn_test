static final String wheel_job = params.get("WHEEL_JOB", "Tensorflow-Centos-Container-Wheels")
static final String py_vers = params.get("PY_VERS", "2.7 3.6 3.7")
// TODO: this won't work, we are automating this now so pulling from latest wheels released, don't know version ahead of time
static final String tf_whls_ver = params.get('TF_WHLS_VER', 'latest')
echo "${tf_whls_ver}"
// set default value for NODE_LABEL
NODE_LABEL = 'clx||skx||bdw'
if ('NODE_LABEL' in params) {
    echo "NODE_LABEL in params"
    if (params.NODE_LABEL != '') {
        NODE_LABEL = params.NODE_LABEL
    }
}
echo "NODE_LABEL: $NODE_LABEL"

def downloadLatestArtifacts = { jobName, fileSpec ->
    def server = Artifactory.server 'ubit-artifactory-or'
    def downloadSpec = """{
        "files": [
            {
                "pattern": "aipg-local/aipg-tf/${jobName}/(*)/${fileSpec}",
                "target": "build/",
                "build": "${jobName}/LATEST",
                "flat": "true"
            }
        ]
    }"""
    def buildInfo = server.download(downloadSpec)
}

node( NODE_LABEL ) {
    try {
        stage('CleanUp') {
            // TODO: put back in cleanup
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



        stage('Build') {
            // download wheels from artifactory from latest release containers location
            //downloadSpec = downloadLatestArtifacts(wheel_job, "*tensorflow*manylinux*.whl")

            copyArtifacts(
                    projectName: wheel_job,
                    selector: specific("lastSuccessfulBuild"),
                    filter: "*tensorflow*manylinux*.whl",
                    fingerprintArtifacts: true,
                    target: "build/")

            sh """#!/bin/bash -x
            set -e
            BUILD_DIR="$WORKSPACE/cje-tf/conda_pkg_build"
            pushd \$BUILD_DIR
            TAG=`git describe --tags --always --dirty`

            # copy wheels dir to dockerfile location
            TF_WHLS_DIR=$WORKSPACE/build
            cp -R \$TF_WHLS_DIR .

            # get first file in TF_WHLS_DIR to detect version
            # NOTE: this will only work if we have a recipe for this version in cje-tf
            #for file in \$TF_WHLS_DIR/*"*tensorflow*.whl"; do break 1; done
            ## detect version of wheels
            #arrFile=(\${file//tensorflow-/ })             
            #versionPart=\${arrFile[1]}
            #version=(\${versionPart//-/ })
            #tf_whls_ver=v\${version[0]}

            # split up all py versions
            PY_VERS="${py_vers}"
            tf_whls_ver="${tf_whls_ver}"
            PY_VERS=`echo \$PY_VERS | tr ' ' '\n' | sort -nu`

            # copy recipes for making conda packages to wheels dir if we have a wheel for that
            for PY_VER in ${py_vers}; do \
                ORIG_PY_VER=\$PY_VER
                # make version without '.' and then check if we have a wheel that matches that
                PY_VER=\${PY_VER//[.]/}
                for wheel in `find . -name "*\$PY_VER*"`; do
                    cp \$wheel \$BUILD_DIR/recipes/\$tf_whls_ver/py\${ORIG_PY_VER}_avx/
                done
            done

            TAG=\$TAG PY_VERS="${py_vers}" TF_WHLS_VER=\$TF_WHLS_VER TF_WHLS_DIR=`basename \$TF_WHLS_DIR` \
            TF_CONDA_DIR=$WORKSPACE/publish CHANNEL=intel \
            $WORKSPACE/cje-tf/conda_pkg_build/build_packages.sh
            popd
            """

        }

        // NOTE: not pushing now but we could someday!
        // stage('Push') {
            // push to public internal registry, getting latest CA chain from Intel's PKI
            // to fix this issue, we do below. It works already on some nodes but not all, see https://soco.intel.com/groups/caas-evaluation-workgroup/blog/2018/11/13/how-to-using-container-registry-and-its-various-features
            // -- docker push amr-registry.caas.intel.com/aipg-tf/dev:centos7
            //      The push refers to repository [amr-registry.caas.intel.com/aipg-tf/dev]
            //      Get https://amr-registry.caas.intel.com/v2/: x509: certificate signed by unknown authority
        //    sh """
        //    # depending on machine, regardless we want unzip
        //    sudo apt-get -y -qq install unzip || sudo yum install -y unzip
        //    http_proxy='' &&\
        //      curl http://certificates.intel.com/repository/certificates/IntelSHA2RootChain-Base64.zip > /tmp/IntelSHA2RootChain-Base64.zip
        //    sudo unzip -o /tmp/IntelSHA2RootChain-Base64.zip -d /usr/local/share/ca-certificates/
        //    rm /tmp/IntelSHA2RootChain-Base64.zip
        //    sudo update-ca-certificates || sudo update-ca-trust
        //    sudo service docker restart
        //    """
        //     docker.withRegistry(docker_registry_url, docker_registry_credentials) {
        //         for (dockerImage in docker_images) {
        //             docker.image(dockerImage).push()
        //         }
        //     }
        // }
    } catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
        // Success or failure, always do artifacts
        stage('Archive Tensorflow Conda Packages') {
            dir("$WORKSPACE" + "/publish") {
                def server = Artifactory.server 'ubit-artifactory-or'
                def uploadSpec = """{
              "files": [
               {
                   "pattern": "*",
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
