import webbrowser
import time
import argparse


def write_html(worksapce, title_part, summary_part, results_part):
    now = int(time.time())
    timeArray = time.localtime(now)
    otherStyleTime = time.strftime("%Y-%m-%d %H:%M:%S", timeArray)
    report_html = "{0}/Tensorflow_MR_Test_report.html".format(worksapce)
    with open(report_html, 'w') as f:
        message = """
       <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Tensorflow MR Test Report</title>
            <link rel="stylesheet" type="text/css" href="http://mlpc.intel.com/static/doc/tensorflow/css/LZ_jenkins.css">
        </head>
        <body>
            <table width="100%" border="0" cellspacing="0" cellpadding="0" align="center">
              <tr>
                    <td align="center" class="biaoti" height="60">{1}</td>
              </tr>
              <tr>
                    <td align="right" height="20">{0}</td>
              </tr>
            </table> 
              {2}
              {3}
        </body>
        </html>
        """ \
            .format(otherStyleTime, title_part, summary_part, results_part)
        f.write(message)
    webbrowser.open(report_html)


def getMain(worksapce, test_status, build_url, mr_branch, tools_repo, target_branch, commit, tensorflow_version, merge_id, user_email, merge_title, repo_page, build_number):
    title_part = """Tensorflow MR Test Report [<a href='{0}/merge_requests/{1}' style="color: #255e95">MR-{1}</a>][<a href='{3}' style="color: #255e95">JOB-{2}</a>]""".format(repo_page, merge_id, build_number, build_url)
    summary_part = """
           <table  width="100%" border="0" cellspacing="0" cellpadding="0" align="center">
               <tr>
                   <td class="biaoti2" style="padding-top: 10px">Summary: </td>
               </tr>
           </table>
           <table width="100%" border="1" cellspacing="0" cellpadding="0"  bgcolor="#cccccc" class="tabtop13">
            <tr>
                <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 10%">Tensorflow version</th>
                <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 10%">Repo</th>
                <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 10%">Source Branch</th>
                <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 10%">Target Branch</th>
                <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 20%">Commit ID</th>
                <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 20%">Author</th>
                <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 20%">Message</th>
            </tr>
            <tr>
                <td class="  font_center  " align="center">{0}</td>
                <td class="  font_center  " align="center"><a href='{1}'>perf_projection_tool</a></td>
                <td class="  font_center  " align="center">{2}</td>
                <td class="  font_center  " align="center">{3}</td>
                <td class="  font_center  " align="center">{4}</td>
                <td class="  font_center  " align="center">{5}</td>
                <td class="  font_center  " align="center">{6}</td>
            </tr>
        </table>
           """.format(tensorflow_version, tools_repo, mr_branch, target_branch, commit, user_email, merge_title)
    if test_status == "FAILED":
        a_link = """<a href='{1}artifact/ 'style="color: #FF8888;">{0}</a>""".format(test_status, build_url)
    else:
        a_link = """<a href='{1}artifact/ ' style="color: green;">{0}</a>""".format(test_status, build_url)
    results_part = """
        <table  width="100%" border="0" cellspacing="0" cellpadding="0" align="left">
           <tr>
                <td class="biaoti2" style="padding-top: 10px;paddind-right"  >Test Results:{0}</td>
           </tr>
        </table>
        """.format(a_link)
    write_html(worksapce=worksapce, title_part=title_part, summary_part=summary_part, results_part=results_part)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="send report")
    parser.add_argument("--worksapce", "-w",
                        help="worksapce", required=True)
    parser.add_argument("--test_status", "-ts",
                        help="test results status", required=True)
    parser.add_argument("--build_url", "-bu",
                        help="job build url", required=True)
    parser.add_argument("--mr_branch", "-mb",
                        help="gitlabSourceBranch", required=True)
    parser.add_argument("--target_branch", "-tb",
                        help="gitlabTargetBranch", required=True)
    parser.add_argument("--commit", "-c",
                        help="gitlabMergeRequestLastCommit", required=True)
    parser.add_argument("--tensorflow_version", "-tv",
                        help="test tensorflow version", required=True)
    parser.add_argument("--merge_id", "-mi",
                        help="gitlabMergeRequestIid", required=True)
    parser.add_argument("--user_email", "-ue",
                        help="gitlabUserEmail", required=True)
    parser.add_argument("--merge_title", "-mt",
                        help="gitlabMergeRequestTitle", required=True)
    parser.add_argument("--repo_page", "-rp",
                        help="gitlabSourceRepoHomepage", required=True)
    parser.add_argument("--tools_repo", "-tr",
                        help="perf_projection_tool repo", required=True)
    parser.add_argument("--build_number", "-bn",
                        help="job build number", required=True)


    args = parser.parse_args()
    worksapce = args.worksapce
    test_status = args.test_status
    build_url = args.build_url
    mr_branch = args.mr_branch
    target_branch = args.target_branch
    commit = args.commit
    tensorflow_version = args.tensorflow_version
    merge_id = args.merge_id
    user_email = args.user_email
    merge_title = args.merge_title
    repo_page = args.repo_page
    tools_repo = args.tools_repo
    build_number = args.build_number

    getMain(worksapce=worksapce, test_status=test_status, build_url=build_url, mr_branch=mr_branch,tools_repo=tools_repo,
            target_branch=target_branch, commit=commit, tensorflow_version=tensorflow_version,
            merge_id=merge_id, user_email=user_email, merge_title=merge_title, repo_page=repo_page, build_number=build_number)


