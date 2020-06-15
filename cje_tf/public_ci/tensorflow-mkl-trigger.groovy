/*
* Tensorflow/Tensorflow MKL Pipeline
*/

static final ArrayList jobs = [ 'tensorflow-mkl-linux-cpu'/*, 'tensorflow-mkl-linux-gpu'*/ ] //Deactivating gpu for now
static final ArrayList prJobs = [ 'tensorflow-mkl-linux-cpu-pr' ]
static final String branchName = env.BRANCH_NAME
static final String prLabel = 'ready to pull'

stage('Trigger') {
  if (env.CHANGE_ID) {
    // This is a PR Build
       if (pullRequest.labels.contains(prLabel)) {
      prJobs.each{ j ->
        build job: j, parameters: [string(name: 'CHECKOUT_BRANCH', value: "origin/pull/${pullRequest.number}")], wait: false
      }
    }
    else {
      println('PR Missing magic label') 
    }
  }
  else {
    // This is a branch build
    jobs.each { j ->
        build job: j, parameters: [string(name: 'CHECKOUT_BRANCH', value: branchName)], wait: false
    }
    }
}

