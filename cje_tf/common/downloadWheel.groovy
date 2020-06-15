// Dynamic function downloadWheel(from_job, wheel_name)
//     download the wheel specified by the parameters from_job and wheel_name to the workspace's build directory
//
// Parameters:
//
//     from_job:     jenkin job name
//                   download the wheel from the specified jenkin job's last successful artifact,
//                   if this parameter is empty, use wheel_name to download
//                   if this parameter is not empty, download the wheel based on the pattern of the wheel specified in the  wheel_name
//
//     wheel_name:   full path name of the wheel, or the pattern of the wheel's name
//                   if from_job is empty, this parameter specify the absolute path of the wheel including the wheel's name to download, e.g.
//                   /mnt/aipg_tensorflow_shared/validation/releases/v1.14.0/intel_tensorflow-1.14.0-cp37-cp37m-manylinux1_x86_64.whl
//                   if from_job is not empty, use this parameter to specify the pattern of the wheel's name, e.g. *35*.whl
//
// Returns: nothing
//
// External dependencies: None

def call(String from_job, \
         String wheel_name) {

    echo "---------------------------------------------------------"
    echo "---------------   running downloadWheel     -------------"
    echo "---------------------------------------------------------"

    echo "DBG from_job: $from_job"
    echo "DBG wheel_name: $wheel_name"

    sh '''#!/bin/bash -x
        if [ ! -d "${WORKSPACE}/build" ]; then
            sudo mkdir ${WORKSPACE}/build
            sudo chmod 775 ${WORKSPACE}/build
            output=`stat -c "%U %G" $WORKSPACE`
            user=`echo $output | awk -F' ' '{print $1}'`
            echo $user
            group=`echo $output | awk -F' ' '{print $2}'`
            echo $group
            sudo chown ${user}:${group} ${WORKSPACE}/build
        fi
    '''
      
    if ( from_job == '') {
        if ( wheel_name == '' )  {
            echo "nothing to download" 
        }
        else {
            echo "wheel_name is $wheel_name"
            withEnv([ "name=$wheel_name" ]) {
                sh '''#!/bin/bash -x
                    cp $name ${WORKSPACE}/build/
                '''
            }
        }
    }
    else  {
        def server = Artifactory.server 'ubit-artifactory-or'

        def downloadSpec = """{
            "files": [
                {
                    "pattern": "aipg-local/aipg-tf/${from_job}/${wheel_name}",
                    "target": "build/",
                    "build": "${from_job}/LATEST",
                    "flat": "true"
                }
            ]
        }"""
        def buildInfo = server.download(downloadSpec)
    }
    
    echo "done running downloadWheel ......."
}

return this;

