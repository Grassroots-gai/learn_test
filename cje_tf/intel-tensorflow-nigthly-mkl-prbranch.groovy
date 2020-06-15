static final String node_label = params.get('NODE_LABEL', 'skx')
static final String jenkins_git_credentials = "aipgbot-tfdo"
static final String public_intel_tf_credential = "b8b085e0-f9ed-4550-b829-35f0bf5187ff"

CJE_TF_COMMON_DIR = "cje-tf/common"
SLACK_CHANNEL = '#test-jenkin-notify'

node(node_label) {
    
    try {
        
        stage("Clean up") {
            sh '''
            cd $WORKSPACE
            sudo rm -rf *
		    sudo rm -rf *.log*
		    sudo rm -rf /tmp/patch.*
            '''
        
        }
        stage("Clone repository for Building PR containers") {
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "$CJE_TF_BRANCH"]],
                      browser                          : [$class: 'AssemblaWeb', repoUrl: ''],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                        relativeTargetDir: 'cje-tf']],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[credentialsId: "$jenkins_git_credentials",
                      url          : 'https://gitlab.devtools.intel.com/TensorFlow/QA/cje-tf.git']]])
        }
        
        stage('Build PR branch ......') {
            
            withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                              credentialsId   : "$public_intel_tf_credential",
                              usernameVariable: 'UserName', passwordVariable: 'Password']]) {
            sh '''#!/bin/bash
            set -xe
            sudo apt-get install jq -y || sudo yum install jq -y
            git config --global user.name $UserName
            git clone https://$UserName:$Password@github.com/Intel-tensorflow/tensorflow.git
            cd $WORKSPACE/tensorflow
            if [[ $(git branch --list $MKL_PR_BRANCH) ]]  
	    then
               git branch -d $MKL_PR_BRANCH
            else
               git checkout -b $MKL_PR_BRANCH
            fi
            branch_name="$(git symbolic-ref HEAD --short)"
            failed_prs=()
            merged_prs=()
            for row in $(curl -s https://api.github.com/search/issues?q=is%3Apr+is%3Aopen+MKL+in%3Atitle%20repo:tensorflow/tensorflow | jq '.items[] | select(.title | contains("MKL") ) | select(.state | contains("open") )' | jq '.html_url'
) ; do
            {
              patch_url=${row:1:${#row}-2}'.diff'
              rm -rf /tmp/patch.$$
              curl -o /tmp/patch.$$ -L $patch_url
              git clean -f
              git apply --ignore-space-change --ignore-whitespace --3way /tmp/patch.$$
              if [[ $? -ne 0 ]]; 
              then
                      failed_prs=("${failed_prs[@]}" $row)
                      continue
              else
                      merged_prs+=($row)
                      echo ${merged_prs[@]}
              fi
             }
             done
             echo "Creating PR Status log file"
             SUMMARYLOG = "$WORKSPACE/PRStatus.log"
             //Prepare PR Status log
             echo "*******PR Status: ****************" |& tee -a SUMMARYLOG
             echo "*******Failed PRs*************" |& tee -a SUMMARYLOG
             for i in "${failed_prs[@]}"; do echo "$i" |& tee -a  SUMMARYLOG; done
                echo "*************Successful PRs*************" |& tee -a  SUMMARYLOG
                for i in "${merged_prs[@]}"; do echo "$i"  |& tee -a  SUMMARYLOG ; done
                cp  SUMMARYLOG "$WORKSPACE/PRStatus.log"
                cp  SUMMARYLOG ../SUMMARYLOG
                //When no conflicts push to the latest_prs branch
                git add -A
                git commit -m "Merging into intel tf  PRs from public tf"
    
             if [[ $branch_name != "master" ]] 
             then
                    git pull origin latest_prs
                    git push origin $MKL_PR_BRANCH
             else
                    echo "Master branch is protected, cannot push PRs here !"
                    exit 1
             fi
            set +xe
        '''
            }
        }
}
     catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
        throw e
    } finally {
        def mesg = "The pipeline ${currentBuild.fullDisplayName} status ${currentBuild.result}"
        def notifyBuild = load("${CJE_TF_COMMON_DIR}/slackNotification.groovy")
        notifyBuild(SLACK_CHANNEL, currentBuild.result, mesg)
    }
     stage('Archive Artifacts') {
            dir("$WORKSPACE") {
            archiveArtifacts artifacts: '*.log*', excludes: null
            fingerprint: true }
        
     }
    
}
