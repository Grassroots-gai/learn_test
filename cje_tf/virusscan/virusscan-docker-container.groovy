public static String code_scan_tools_branch = params.get("CODE_SCAN_TOOLS_BRANCH", "master")
public static String node_label = params.get("NODE_LABEL", "skx || bdw || clx-8280")
public static String release_version = params.get("RELEASE_VERSION", "v1.14.0")
public static Boolean scan_devel_container=params.SCAN_DEVEL_CONTAINER
public static Boolean scan_nondevel_container=params.SCAN_NONDEVEL_CONTAINER
public static Boolean scan_py2_container=params.SCAN_PY2_CONTAINER
public static Boolean scan_py3_container=params.SCAN_PY3_CONTAINER

node( node_label ) {
  try {

      deleteDir()

      stage('Checkout') {
          // where code to run dynamic scans lives
            checkout([$class: 'GitSCM',
                      branches: [[name: code_scan_tools_branch]],
                      browser: [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions: [[$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'code-scan-tools']],
                      submoduleCfg: [],
                      userRemoteConfigs: [[url: "https://gitlab.devtools.intel.com/Intel-Common/QA/code-scan-tools.git"]]])
      }

      stage('Virus Scan') {
          dir ( "$WORKSPACE/code-scan-tools/virus" ) {
              withEnv(["TF_VERSION=$release_version", "SCAN_DEV=$scan_devel_container", "SCAN_NON_DEV=$scan_nondevel_container", "SCAN_PY2=$scan_py2_container", "SCAN_PY3=$scan_py3_container"]) {

                  sh """
                  #!/bin/bash -xe

                  if [ ${SCAN_DEV} == "true" ]; then
                      #CONTAINERS=(
                      #  "amr-registry.caas.intel.com/aipg-tf/manylinux2010:${TF_VERSION}-devel-mkl"
                      #  "amr-registry.caas.intel.com/aipg-tf/manylinux2010:${TF_VERSION}-devel-mkl-py3"
                      #)
                      #./virusscan.sh "\${CONTAINERS[@]}"
                      
                      if [ ${SCAN_PY2} == "true" ]; then
                          CONTAINERS=(
                            "amr-registry.caas.intel.com/aipg-tf/manylinux2010:${TF_VERSION}-devel-mkl"
                          )
                          ./virusscan.sh "\${CONTAINERS[@]}"                        
                      fi

                      if [ ${SCAN_PY3} == "true" ]; then
                          CONTAINERS=(
                            "amr-registry.caas.intel.com/aipg-tf/manylinux2010:${TF_VERSION}-devel-mkl-py3"
                          )
                          ./virusscan.sh "\${CONTAINERS[@]}"                        
                      fi

                      
                  fi

                  if [ ${SCAN_NON_DEV} == "true" ]; then
                      #CONTAINERS=(
                      #  "amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:${TF_VERSION}-mkl"
                      #  "amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:${TF_VERSION}-mkl-py3"
                      #)
                      #./virusscan.sh "\${CONTAINERS[@]}"

                      if [ ${SCAN_PY2} == "true" ]; then
                          CONTAINERS=(
                            "amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:${TF_VERSION}-mkl"
                          )
                          ./virusscan.sh "\${CONTAINERS[@]}"                        
                      fi

                      if [ ${SCAN_PY3} == "true" ]; then
                          CONTAINERS=(
                            "amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:${TF_VERSION}-mkl-py3"
                            "amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow:${TF_VERSION}-mkl-py3-jupyter"
                          )
                          ./virusscan.sh "\${CONTAINERS[@]}"                        
                      fi

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
        stage('Archive Artifacts ') {
            dir("$WORKSPACE/code-scan-tools/virus") {
              archiveArtifacts artifacts: '*.log', excludes: null
            }
        }
    }
}
