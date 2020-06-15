#!/usr/bin/python
from argparse import ArgumentParser
import os
import subprocess
import sys
from multiprocessing import Process

# This utility will run all of the TensorFlow docker builds

class DockerBuildUtil:
  docker_script_path = ''
  docker_script_name = "parameterized_docker_build.sh"
  log_file_name = "docker_build.log"
  DOCKER_PUSH_CMD = "'docker push'"
  default_build_options = "--copt=\"-mavx2\" --copt=\"-mfma\" --copt=\"-march=broadwell\" --copt=\"-O3\""
  #container_prefix = "nervana-dockrepo01.fm.intel.com:5001/intelai/tensorflow"
  container_prefix = "amr-registry.caas.intel.com/aipg-tf-private/intel-tensorflow"
  proxy_url = "http://proxy-us.intel.com:911"
  DOCKER_PROXY_BUILD_ARGS = (
        " --build-arg http_proxy=" + proxy_url + "" 
        " --build-arg https_proxy={}"
        " --build-arg HTTP_PROXY={}"
        " --build-arg HTTPS_PROXY={}".format(proxy_url,proxy_url,proxy_url))
  DOCKER_NO_CACHE_BUILD_ARGS = " --pull --no-cache"
  DOCKER_BUILD_EXTRA_PARAMS_KEY="TF_DOCKER_BUILD_ARGS"
  CONTAINER_BUILDS = {
     "1.8-public_whl": {
        "TF_DOCKER_BUILD_TYPE": "'MKL'",
        "TF_DOCKER_BUILD_IS_DEVEL": "'no'",
        "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
        "TF_DOCKER_BUILD_VERSION": "'1.8'",
        "TF_DOCKER_BUILD_CENTRAL_PIP": "'https://storage.googleapis.com/"
		"intelai-tensorflow/whl/avx/tensorflow-1.8.0-cp27-cp27mu-linux_x86_64.whl'"
     }, 
     "1.8-devel-mkl": {
         "TF_DOCKER_BUILD_TYPE": "'MKL'",
         "TF_DOCKER_BUILD_IS_DEVEL": "'yes'",
         "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
         "TF_DOCKER_BUILD_VERSION": "'1.8'",
         "TF_DOCKER_BUILD_DEVEL_BRANCH": "'r1.8'",
         "TF_BAZEL_BUILD_OPTIONS": "'{}'"
     },
     "latest-devel-mkl": {
         "TF_DOCKER_BUILD_TYPE": "'MKL'",
         "TF_DOCKER_BUILD_IS_DEVEL": "'yes'",
         "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
         "TF_DOCKER_BUILD_VERSION": "",
         "TF_DOCKER_BUILD_DEVEL_BRANCH": "'master'",
         "TF_BAZEL_BUILD_OPTIONS": "'{}'"
     },
     "1.6-public_whl": {
        "TF_DOCKER_BUILD_TYPE": "'MKL'",
        "TF_DOCKER_BUILD_IS_DEVEL": "'no'",
        "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
        "TF_DOCKER_BUILD_VERSION": "'1.6'",
        "TF_DOCKER_BUILD_CENTRAL_PIP": "'https://anaconda.org/intel/tensorflow/"
                "1.6.0/download/tensorflow-1.6.0-cp27-cp27mu-linux_x86_64.whl'"
    }, 
    "1.6-public_whl-python3": {
        "TF_DOCKER_BUILD_TYPE": "'MKL'",
        "TF_DOCKER_BUILD_IS_DEVEL": "'no'",
        "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
        "TF_DOCKER_BUILD_VERSION": "'1.6'",
        "TF_DOCKER_BUILD_PYTHON_VERSION": "PYTHON3",
        "TF_DOCKER_BUILD_CENTRAL_PIP": "'https://anaconda.org/intel/tensorflow/"
                "1.6.0/download/tensorflow-1.6.0-cp35-cp35m-linux_x86_64.whl'"
    },
     "1.7-devel-mkl": {
         "TF_DOCKER_BUILD_TYPE": "'MKL'",
         "TF_DOCKER_BUILD_IS_DEVEL": "'yes'",
         "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
         "TF_DOCKER_BUILD_VERSION": "'1.7'",
         "TF_DOCKER_BUILD_DEVEL_BRANCH": "'r1.7'",
         "TF_BAZEL_BUILD_OPTIONS": "'{}'"
    },
     "1.6-devel-mkl": {
         "TF_DOCKER_BUILD_TYPE": "'MKL'",
         "TF_DOCKER_BUILD_IS_DEVEL": "'yes'",
         "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
         "TF_DOCKER_BUILD_VERSION": "'1.6'",
         "TF_DOCKER_BUILD_DEVEL_BRANCH": "'r1.6'",
         "TF_BAZEL_BUILD_OPTIONS": "'{}'"
     },
     "1.5-devel-mkl": {
         "TF_DOCKER_BUILD_TYPE": "'MKL'",
         "TF_DOCKER_BUILD_IS_DEVEL": "'yes'",
         "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
         "TF_DOCKER_BUILD_VERSION": "'1.5'",
         "TF_DOCKER_BUILD_DEVEL_BRANCH": "'r1.5'",
         "TF_BAZEL_BUILD_OPTIONS": "'{}'"
     },
     "latest-devel-mklml": {
         "TF_DOCKER_BUILD_TYPE": "'MKL'",
         "TF_DOCKER_BUILD_IS_DEVEL": "'yes'",
         "TF_DOCKER_BUILD_IMAGE_NAME": "'{}'".format(container_prefix),
         "TF_DOCKER_BUILD_VERSION": "'latest-ml'",
         "TF_DOCKER_BUILD_DEVEL_BRANCH": "'master'",
         "TF_BAZEL_BUILD_OPTIONS": "'{} --copt=-DINTEL_MKL_ML'"
     }
  }

  def generate_logfile_path(self, prefix=None):
    return "{}/{}-{}".format(self.args.log_path, prefix, self.args.log_file_name) 

  def validate_params(self):
    retVal = True 
    #if we are running parallel builds, we have to output to log files
    if self.args.no_log:
      if not self.args.serialize_build and not self.args.single_image:
        print "Parallel builds require output to log files. --no-log option " \
          "requires --serialize_build or --single-image. Quitting."
        return False
    if not os.path.exists(self.args.workspace_path) or \
      not os.path.isdir(self.args.workspace_path) or \
      not os.path.isfile(self.args.workspace_path+'/WORKSPACE') :
      print "Workspace at {} is invalid. Must be the root of a tensorflow source tree. Quitting.".format(
          self.args.workspace_path)
      return False
    else:    
      docker_root = "{}/tensorflow/tools/docker".format(self.args.workspace_path)
      self.docker_script_path = "{}/{}".format(docker_root, self.docker_script_name)
    
    if os.path.exists(self.docker_script_path) == False:
      print "Docker script does not exist at {}. Quitting.".format(
          self.docker_script_path)
      return False
    if os.path.exists(self.args.log_path) == False:
      print "Log directory {} does not exist. Quitting.".format(
          self.args.log_path)
      return False
    return retVal

  def build_command(self):
    command = self.docker_script_path
    return command

  def build_params_to_string(self, params):
    retval = ''
    if not self.args.no_push:
        params["TF_DOCKER_BUILD_PUSH_CMD"] = "{}".format(self.DOCKER_PUSH_CMD)
    
    #set the compiler flags
    if params["TF_DOCKER_BUILD_IS_DEVEL"] == "'yes'":
      if self.args.debug: print "compiler flags={}".format(self.args.build_flags)
      params["TF_BAZEL_BUILD_OPTIONS"] = params["TF_BAZEL_BUILD_OPTIONS"].format(self.args.build_flags)

    if self.args.debug: print "Params={}".format(params)

    params["WORKSPACE"] = "\"{}\"".format(self.args.workspace_path)

    if self.args.dry_run:
      params["TF_BUILD_DRY_RUN"] = "1"
    if not self.args.no_proxy:#use proxy
      if self.args.no_cache:#no cache
        params[self.DOCKER_BUILD_EXTRA_PARAMS_KEY] = "'{} {}'".format(
                          self.DOCKER_PROXY_BUILD_ARGS,
                          self.DOCKER_NO_CACHE_BUILD_ARGS)
      else:
        params[self.DOCKER_BUILD_EXTRA_PARAMS_KEY] = "'{}'".format(
          self.DOCKER_PROXY_BUILD_ARGS)
    else:#no proxy
      if self.args.no_cache:
        params[self.DOCKER_BUILD_EXTRA_PARAMS_KEY] = "'{}'".format(
                          self.DOCKER_NO_CACHE_BUILD_ARGS)

    for param in params:
      retval += "export {}={} && ".format(param,params.get(param))
    if  self.args.debug: print "retVal = {}".format(retval)
    return retval

  def run_build(self, command, name, params):
    full_command = "{} {}".format(self.build_params_to_string(params),command)
    log_file_path = self.generate_logfile_path(prefix=name)
    if not self.args.serialize_build and self.args.debug:
      if hasattr(os, 'getppid'):
        print 'Parent Process: ', os.getppid()
      print "Current Process ID: ", os.getpid()
    if  self.args.debug: print "Running:", full_command
    retCode = 0
    if not self.args.no_log  :
        print "Check output in ", log_file_path
        log_file = file(log_file_path, mode='w')
        retCode = subprocess.call(full_command, shell=True, stdout=log_file, 
          stderr=subprocess.STDOUT)
    else:
        print "Not Generating a log file. Good luck!"
        retCode = subprocess.call(full_command, shell=True, 
          stderr=subprocess.STDOUT)
    
    if retCode != 0:
      print "Build {} FAILED with error Code {}".format(name, retCode)
    else: 
      print "Build {} SUCCEEDED.".format(name)



  def main(self):
    arg_parser = ArgumentParser(description='Launchpad for Parameterized Docker'
        ' builds')

    arg_parser.add_argument('-d', "--dry-run", 
                            help="Don't actually build, just dry run", 
                            dest="dry_run", action="store_true")
    arg_parser.add_argument('-n', "--logfile-base-name", 
                            help='Specify the base name for the log file', 
                            dest="log_file_name", default=self.log_file_name)
    arg_parser.add_argument('-p', "--log-path", 
                            help='Specify the path for the log file', 
                            dest="log_path", default=os.getcwd())
    arg_parser.add_argument("workspace_path", 
                            help='Specify the path for the workspace.  Must be the root of a tensorflow source tree.')
    arg_parser.add_argument('-s', "--single-image", 
                            help='Specify the name of the image to build.', 
                            dest="single_image", choices=self.CONTAINER_BUILDS,
                            default=None)
    arg_parser.add_argument("--no-log", help="Don't output to logfile."
                            " Requires --serialize-build option.", 
                            dest="no_log", action ='store_true')
    arg_parser.add_argument("--no-cache", help="Don't use cached Docker layers",
                            dest="no_cache", action ='store_true')
    arg_parser.add_argument("--serialize-build",
                    help="Don't do parallel builds. ", 
                    dest="serialize_build", action="store_true")
    arg_parser.add_argument("--no-proxy",
                    help="Don't use the proxy info for the build", 
                    dest="no_proxy", action="store_true")
    arg_parser.add_argument("--no-push-container", 
                    help="Don't publish the docker container to the registry.", 
                    dest="no_push", action="store_true")
    arg_parser.add_argument("--latest-only", 
                    help="Only build the latest builds from master branch.", 
                    dest="latest_only", action="store_true")
    arg_parser.add_argument("--debug", 
                    help="Print debug information for {}".format(__file__), 
                    dest="debug", action="store_true")
    arg_parser.add_argument("-f", "--build-flags", 
                    help="Compiler build options. HINT: use ${BAZEL_BUILD_OPTS}", 
                    dest="build_flags", default=self.default_build_options)

    self.args = arg_parser.parse_args()

    if self.validate_params() == False:
        print("Invalid parameters. Exiting.")

    command = self.build_command()
    processes = {}
    for build in self.CONTAINER_BUILDS:
      if self.args.latest_only and build.find("latest") == -1 :
        continue
      if self.args.single_image and build.find(self.args.single_image) == -1:
        continue
      if self.args.serialize_build:
        self.run_build(command, build, self.CONTAINER_BUILDS.get(build))
      else: #parallelize the builds
        p = Process(target=self.run_build, 
          args=(command, build, self.CONTAINER_BUILDS.get(build)))
        p.name = build
        processes[build] = p
        p.start()

if __name__ == "__main__":
  builder = DockerBuildUtil()
  builder.main()
