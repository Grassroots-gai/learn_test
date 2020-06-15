def call(String branchName, String name, String runType, String summaryLog, String summaryTitle) {
    echo "summaryTitle: ${summaryTitle}"

    gitCommit = ""
    if (fileExists('get_tf_sourceinfo.out')) {
        gitCommit=readFile('get_tf_sourceinfo.out').trim()
        echo "gitCommit: ${gitCommit}"
    }
    else {
        if (name.trim()) {
            dir("${name}") {
                gitCommit = sh (
                    script: 'git log --pretty=format:"%H" -n 1',
                    returnStdout: true
                ).trim()
            }
        }
    }
    date = sh (
        script: 'date +"%Y-%m-%d %H:%M"',
        returnStdout: true
    ).trim()
    model_name = sh (script: 'lscpu | grep "Model name:"', returnStdout: true).trim()
    os_version = sh (script: "cat /etc/os-release | grep PRETTY_NAME | sed 's/PRETTY_NAME=/OS Version:      /'", returnStdout: true).trim()

    SERVERNAME = sh (script:"echo ${env.NODE_NAME} | cut -f1 -d.",
                              returnStdout: true).trim()
    echo "SERVERNAME = ${SERVERNAME}"

    if (summaryLog == '') {
    	summaryLog = "summary_${SERVERNAME}.log"
    }
    echo "summaryLog = ${summaryLog}"

    String summary = "${summaryTitle} ${date}"
    withEnv(["name=$name", "gitCommit=$gitCommit", "model_name=$model_name","os_version=$os_version", "nodeName=${env.NODE_NAME}", "repoBranch=${branchName}", "summaryLog=${summaryLog}", "summary=${summary}" ]) {
        sh '''#!/bin/bash -x
        set -e

        echo "$summaryLog"
        echo "$summary"
        echo "*************************************************************************" > ${summaryLog}
        echo "${summary}" >> ${summaryLog}
        if [[ "$name" != "" ]]; then
            echo "Repository: ${name}" >> ${summaryLog}
        fi
        if [[ "$branchName" != "" ]]; then
            echo "Branch: ${repoBranch}" >> ${summaryLog}
        fi
        if [[ "$gitCommit" != "" ]]; then
            echo "Git Revision: ${gitCommit}" >> ${summaryLog}
        fi
        echo "Running on: ${nodeName}" >> ${summaryLog}
        echo "${model_name}" >> ${summaryLog}
        echo "${os_version}" >> ${summaryLog}
        echo "*************************************************************************\n" >> ${summaryLog}
        echo "\n" >> ${summaryLog}
        '''
    }

}

return this;
