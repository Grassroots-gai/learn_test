###################################################################################################################################################
#
# this script goes to the private-tensorflow clone repo directory and get information about the repo's url, branch, commit, and mkldnn version,etc.
# it will then generated an output file get_tf_sourceinfo.out under the <workspace_dir> which can be used when posting to the dashboard.
# the script takes 2 input:
#     tensorflow_dir: where the private-tensorflow is cloned
#     workspace_dir: workspace for the running job, output file will be generated under this directory
# the output file have the following info:
#     url: <url>
#     branch: <branch>
#     commit: <commit>
#     mkldnn_version: <mkldnn_version>
#
###################################################################################################################################################


import sys
import os
import subprocess
import re
import argparse

def main(): 

    # get and parse command line options
    parser = argparse.ArgumentParser()
    parser.add_argument("--tensorflow_dir", type=str, default="/private-tensorflow",
                    help="The tensorflow cloned directory.")
    parser.add_argument("--workspace_dir", type=str, default="/workspace",
                    help="The output directory when outputfile is generated.")
    args = parser.parse_args()

    tensorflow_dir = args.tensorflow_dir
    workspace_dir = args.workspace_dir

    if os.path.exists(tensorflow_dir):
        # get branch info
        branches = subprocess.check_output(["cd " + tensorflow_dir + " && git describe --all"],shell=True).decode('utf-8').rstrip()
        branch = branches.split("/")[-1]

        # get commit info
        commit = subprocess.check_output(["cd " + tensorflow_dir + " && git rev-parse HEAD"],shell=True).decode('utf-8').rstrip()

        # get url info
        url = subprocess.check_output(["cd " + tensorflow_dir + " && git config --get remote.origin.url"],shell=True).decode('utf-8').rstrip()

        # get the MKLDNN version for private-tensorflow, the version can be found inside the workspace.bzl file, eg.
        # mkl_repository(
        #     name = "mkl_linux",
        #     build_file = clean_dep("//third_party/mkl:mkl.BUILD"),
        #     sha256 = "e2233534a9d15c387e22260997af4312a39e9f86f791768409be273b5453c4e6",
        #     strip_prefix = "mklml_lnx_2019.0.20180710",
        #     urls = [
        #         "https://mirror.bazel.build/github.com/intel/mkl-dnn/releases/download/v0.16/mklml_lnx_2019.0.20180710.tgz",
        #         "https://github.com/intel/mkl-dnn/releases/download/v0.16/mklml_lnx_2019.0.20180710.tgz",
        #     ],
        # )
        f_mkldnn_version = os.path.abspath(tensorflow_dir + "/tensorflow")
        if os.path.exists(f_mkldnn_version):
            mkldnn_process = subprocess.check_output(["cd " + f_mkldnn_version + " && grep -A 6 'mkl.linux' ./workspace.bzl | grep download | tail -1 | cut -d/ -f2- | awk -F'/' '{print $7}'"],
                                                     shell=True)
            mkldnn_version = mkldnn_process.decode('utf-8').rstrip()

        with open(workspace_dir + "/get_tf_sourceinfo.out", 'w') as foutput:
            foutput.write("url: " + url + "\n")
            foutput.write("branch: " + branch + "\n")
            foutput.write("commit: " + commit + "\n")
            foutput.write("mkldnn_version: " + mkldnn_version + "\n")

if __name__ == "__main__":
    main()

