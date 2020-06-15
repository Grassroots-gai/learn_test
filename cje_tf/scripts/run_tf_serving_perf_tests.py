#
# Copyright (c) 2019 Intel Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: EPL-2.0
#

import csv
import json
import os
import re
import sys
import time
import glob
from argparse import ArgumentParser
from subprocess import check_output, CalledProcessError
from tempfile import TemporaryFile

arg_parser = ArgumentParser(description="Parse args for performance tests")


def parse_args():
    arg_parser.add_argument("--intel_models_repo_dir",
                            help="intel-models repo directory",
                            dest="intel_models_repo_dir", default=None, type=str, required=True)
    arg_parser.add_argument("--baseline_file",
                            help="Json file that contains baseline metrics for all models",
                            dest="baseline_file", type=str, required=True)
    arg_parser.add_argument("--intel_trained_models_dir",
                            help="This directory contains trained model files",
                            dest="intel_trained_models_dir", default=None, type=str, required=True)
    arg_parser.add_argument("--output_dir",
                            help="This directory where you want to get performance results",
                            dest="output_dir", default=os.getcwd(), type=str)
    arg_parser.add_argument("--tolerance",
                            help="This is percentage tolerance compared to baseline metric",
                            dest="tolerance", default=5, type=str)

    args = arg_parser.parse_args()

    # validate arguments
    for path in [args.intel_models_repo_dir, args.baseline_file,
                 args.intel_trained_models_dir, args.output_dir]:
        if not os.path.exists(path):
            sys.exit("{} file does not exists.".format(path))

    return args


def get_model_path(intel_trained_models_dir, model_file_name):
    files = glob.glob("{}/*/*/{}".format(intel_trained_models_dir,
                                         model_file_name))
    if len(files) != 0 and os.path.isfile(files[0]):
        return files[0]
    else:
        return None


class Model:
    def __init__(self, name, file_path, mode, precision,
                 platform, batch_size, metric_name, baseline_metric_value,
                 tolerance, intel_models_repo_dir):
        self.name = name
        self.file_path = file_path
        self.mode = mode
        self.precision = precision
        self.platform = platform
        self.batch_size = batch_size
        self.metric_name = metric_name
        self.baseline_metric_value = baseline_metric_value
        self.test_result = None
        self.metric_value = None
        self.metric_performance = None
        self.tolerance = int(tolerance)
        self.log_file = None
        self.intel_models_repo_dir = intel_models_repo_dir

    def run_cmd(self, cmd):
        with TemporaryFile() as t:
            try:
                out = check_output(cmd, stderr=t, shell=True)
                return 0, out
            except CalledProcessError as e:
                t.seek(0)
                return e.returncode, t.read()

    def parse_logs(self, output):
        # latency & throughput metrics values in different models log,
        # files are not consistent, hence we have to parse value of these
        # metrics separately for each model.
        try:
            if self.name == "inceptionv3":
                str = re.findall(r'Log output location: .*', output)
                self.log_file = str[0][20:].strip()
                with open(self.log_file, "r") as logfile:
                    logs_text = logfile.read()
                print("LOG OUTPUT")
                print(logs_text)
                if self.metric_name == "latency":
                    str = re.findall(r'Latency: \d+\.\d+', logs_text)
                    self.metric_value = float(str[0][8:])
                if self.metric_name == "throughput" and self.name == "inceptionv3":
                    str = re.findall(r'Throughput: \d+\.\d+', logs_text)
                    self.metric_value = float(str[0][12:])
        except Exception as e:
            print("Exception occurred - ", e.message)
            self.metric_value = None
        except OSError as err:
            print("Error occurred : ", err.message)
            self.metric_value = None

    def run_test(self):
        cwd = os.getcwd()
        os.chdir(self.intel_models_repo_dir + "/benchmarks")
        cmd = "\n python launch_benchmark.py --in-graph " + self.file_path + \
              " --model-name " + self.name + \
              " --framework tensorflow_serving " \
              " --precision " + self.precision + \
              " --mode " + self.mode +\
              " --batch-size=" + str(self.batch_size) + \
              " --benchmark-only"
        print("Running model {} for {}".format(self.name, self.metric_name))
        return_code, output = self.run_cmd(cmd)
        if return_code:
            self.test_result = "Fail"
        else:
            # get latency & throughput from log files. write separate function
            self.parse_logs(output)
            if self.metric_value is None:
                self.test_result = "Fail"
                self.metric_performance = None
            else:
                self.metric_performance = float("{:.2f}".format(
                    100 * (self.metric_value - self.baseline_metric_value) / self.baseline_metric_value))
                if self.metric_name == "latency":
                    self.test_result = "Pass" if self.metric_performance < self.tolerance else "Fail"
                if self.metric_name == "throughput":
                    self.test_result = "Pass" if self.metric_performance > -self.tolerance else "Fail"
        os.chdir(cwd)

    def get_results(self):
        result = dict()
        result['model'] = self.name
        result['model_file'] = self.file_path
        result['mode'] = self.mode
        result['precision'] = self.precision
        result['platform'] = self.platform
        result['batch_size'] = self.batch_size
        result['metric_name'] = self.metric_name
        result['baseline_metric_value'] = self.baseline_metric_value
        result['metric_value'] = self.metric_value
        result['percentage_change'] = self.metric_performance
        result['test_result'] = self.test_result
        result['logfile'] = self.log_file
        return result


def main(args):
    success = True
    models = []
    with open(args.baseline_file) as f:
        baseline_metrics = json.load(f)

    if len(baseline_metrics) == 0:
        print("No experiments to run. Please update baseline cofig "
              "file with experiments to run.")
        exit(-1)
    for m in baseline_metrics:
        model_file_path = get_model_path(args.intel_trained_models_dir, m['model_file'])
        models.append(Model(m['model'],
                            model_file_path,
                            m['mode'],
                            m['precision'],
                            m['platform'],
                            m['batch_size'],
                            m['metric_name'],
                            m['baseline_metric_value'],
                            args.tolerance,
                            args.intel_models_repo_dir))

    results = []
    for model in models:
        model.run_test()
        test_result = model.get_results()
        results.append(test_result)
        if test_result['test_result'] == "Fail":
            success = False

    # Write results to file
    keys = results[0].keys()
    output_csv = "{}/results_{}.csv".format(args.output_dir, time.strftime("%Y%m%d-%H%M%S"))
    with open(output_csv, "w") as outfile:
        dict_writer = csv.DictWriter(outfile, keys)
        dict_writer.writeheader()
        dict_writer.writerows(results)
    print("Performance tests completed. Result stored in: " + output_csv)
    exit(0 if success else -1)

if __name__ == "__main__":
    args = parse_args()
    main(args)
