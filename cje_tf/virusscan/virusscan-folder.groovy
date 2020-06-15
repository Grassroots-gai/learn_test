public static String code_scan_tools_branch = params.get("CODE_SCAN_TOOLS_BRANCH", "master")
public static String node_label = params.get("NODE_LABEL", "bdw || skx || clx")
public static String file_filter = params.get("FILE_FILTER", "*.whl")

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

      stage('Download TF wheels') {
          // download wheels from artifactory from latest release containers location
          // TODO: should the `whl` stuff be dynamic? Will we ever virus scan a folder that isn't a wheel?
          // This could be a param to download whatever from the SCAN_TARGET
          downloadSpec = downloadLatestArtifacts("$SCAN_TARGET", file_filter)
      }

      stage('Virus Scan') {
          withEnv(["target=$WORKSPACE/build"]) {
              sh '''
              #!/bin/bash -xe

              cd code-scan-tools/virus
              ./virusscan.sh $target
              '''
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
