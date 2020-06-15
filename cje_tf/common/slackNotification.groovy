// Dynamic function slackNotification(channel, buildStatus, msg)
//
// Parameters:
//
//     channel       Channel to post to
//
//     buildStatus   default to SUCCESSFUL if passing null 
//
//     msg           The message to post
//
//     msgLevel  Danger level to use when posting.  Can be Slack style
//               ('good', 'warning', 'danger') or Jenkins style ('FAILURE',
//               'UNSTABLE', 'SUCCESS', 'success').  Note that Jenkins 'success'
//               level should be used when the Jenkins result is null (which is
//               often), and the caller needs to translate null to 'success'.
//
// Returns: nothing
//
// External dependencies: None

def call(String channel, String buildStatus = 'STARTED', String msg) {

  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  // msg of null means no additional messages to report
  msg = msg ?: ''
  String summary = "Job <${env.BUILD_URL}|${env.JOB_NAME} #${env.BUILD_NUMBER}> on ${env.NODE_NAME} : ${buildStatus} \n ${msg}"
  Integer SLACK_RETRY_TIMES = 2
  Map SLACK_COLOR_STRINGS = [
     'SUCCESS': 'good',
     'UNSTABLE': 'warning',
     'FAILURE': 'danger'
  ]

  // Send notifications
  retry(SLACK_RETRY_TIMES) {
    slackSend channel: channel, message: summary, color: SLACK_COLOR_STRINGS[currentBuild.currentResult]
  }

}

return this;

