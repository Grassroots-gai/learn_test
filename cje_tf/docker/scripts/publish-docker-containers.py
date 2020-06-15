#!/usr/bin/python

import fnmatch
import os
import subprocess
from argparse import ArgumentParser
from os.path import expanduser
from shutil import copyfile

MKL_TAG = "mkl"

INTERNAL_REPO = "amr-registry.caas.intel.com/aipg-tf/intel-optimized-tensorflow"
STAGING_REPO = "docker.io/intelaipg/staging"
PUBLIC_REPO = "docker.io/intelaipg/intel-optimized-tensorflow"


def set_docker_env_vars(tag=None):
    # find and copy all notary keys to ~/.docker/private
    #notary_dir = os.path.join(expanduser("~"), ".docker/trust/private/")
    # for root, dirnames, filenames in os.walk(os.getcwd()):
    #    for notary_key in fnmatch.filter(filenames, '*.key'):
    #        # create docker notoray directory is not created yet
    #       if not os.path.exists(notary_dir):
    #            os.makedirs(notary_dir)
    #        copyfile(os.path.join(root, notary_key), notary_dir)
    #        command = "chmod -R 600 {}".format(notary_dir)
    #        ret_code = run_command(command)

    if STAGING_REPO in tag:
        os.environ["DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE"] = \
            os.environ["PUBLIC_DOCKER_STAGING_CONTENT_TRUST_REPOSITORY_PASSPHRASE"]
    elif PUBLIC_REPO in tag:
        os.environ["DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE"] = \
            os.environ["PUBLIC_DOCKER_PROD_CONTENT_TRUST_REPOSITORY_PASSPHRASE"]
    elif INTERNAL_REPO not in tag:
        raise Exception("{} is an invalid docker repository".format(tag))
    # Set the root passphrase as well if we made it here
    os.environ["DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE"] = \
        os.environ["PUBLIC_DOCKER_CONTENT_TRUST_ROOT_PASSPHRASE"]


def run_command(command):
    ret_code = subprocess.call(command, shell=True, stderr=subprocess.STDOUT)
    # if not self.args.no_log  :
    # print "Check output in ", log_file_path
    # log_file = file(log_file_path, mode='w')
    # retCode = subprocess.call(full_command, shell=True, stdout=log_file,
    # else:
    if ret_code != 0:
        print("Command {} FAILED with error Code {}".format(command, ret_code))
    else:
        print("Commnad {} SUCCEEDED.".format(command))

    return ret_code


def tag_container(id, new_tag, REPO=None):
    new_tag = "{}:{}".format(REPO, new_tag)
    print("Tagging container id {} as {}".format(id, new_tag))
    command = "docker tag {} {}".format(id, new_tag)
    ret_code = run_command(command)
    return new_tag


def push_container(tag):
    command = "docker push --disable-content-trust=false {}".format(tag)
    run_command(command)


def publish():
    parser = ArgumentParser()
    parser.add_argument('-v', '--tensorflow-version', help="The tensorflow tag number (e.g. '1.11.0')",
                        dest='tf_version', required=True)
    parser.add_argument('-l', '--latest-container-id', help="The 'latest' container id",
                        dest='latest_id', required=False)
    parser.add_argument('-p', '--latest-py3-container-id', help="The 'latest py3' container id",
                        dest='latest_py3_id', required=False)
    parser.add_argument('-d', '--latest-devel-container-id', help="The 'latest devel' container id",
                        dest='latest_devel_id', required=False)
    parser.add_argument('-y', '--latest-devel-py3-container-id', help="The 'latest devel py3' container id",
                        dest='latest_devel_py3_id', required=False)
    parser.add_argument('-j', '--latest_jupyter_container_id', help="The 'latest jupyter' container id",
                        dest='latest_jupyter_container_id', required=False)
    parser.add_argument('-k', '--latest_jupyter_py3_container_id', help="The 'latest py3 jupyter' container id",
                        dest='latest_jupyter_py3_container_id', required=False)
    args = parser.parse_args()

    CONTAINER_BUILDS = {
        "{}".format(args.latest_id): [
            "{}".format(args.tf_version),
            "{}-mkl".format(args.tf_version)],
        "{}".format(args.latest_devel_id): [
            "{}-devel".format(args.tf_version),
            "{}-devel-mkl".format(args.tf_version)],
        "{}".format(args.latest_py3_id): [
            "{}-py3".format(args.tf_version),
            "{}-mkl-py3".format(args.tf_version)],
        "{}".format(args.latest_devel_py3_id): [
            "{}-devel-py3".format(args.tf_version),
            "{}-devel-mkl-py3".format(args.tf_version)],
        "{}".format(args.latest_jupyter_container_id): [
            "{}-jupyter".format(args.tf_version),
            "{}-mkl-jupyter".format(args.tf_version)],
        "{}".format(args.latest_jupyter_py3_container_id): [
            "{}-py3-jupyter".format(args.tf_version),
            "{}-mkl-py3-jupyter".format(args.tf_version)]}

    # Delete tags not provided by the args
    del CONTAINER_BUILDS['None']

    for id in CONTAINER_BUILDS:
        for tag in CONTAINER_BUILDS.get(id):
            # Don's update released containers unless it's desired
            if tag.split('.')[0].isdigit() and \
               os.environ.has_key("OVERWRITE_RELEASED_IMAGES") and \
               os.environ["OVERWRITE_RELEASED_IMAGES"] is not None and \
               os.environ["OVERWRITE_RELEASED_IMAGES"].strip().lower() \
               != "yes":
                continue

            # Publish nightly images
            new_tag = tag_container(id, tag, REPO=PUBLIC_REPO)
            set_docker_env_vars(new_tag)
            push_container(new_tag)


if __name__ == "__main__":
    publish()
