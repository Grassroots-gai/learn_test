from time import sleep
import subprocess
import os
import argparse
import configparser
import sys


class ReadConfig:
    def __init__(self):
        self.cf = configparser.ConfigParser()
        self.cf.read(config_path)

    def get_pb(self, name):
        value = self.cf.get("pb", name)
        return value

    def get_dataset(self, name):
        value = self.cf.get("dataset", name)
        return value

    def get_required(self, name):
        value = self.cf.get("required", name)
        return value

    def get_branch(self, name):
        value = self.cf.get("branch", name)
        return value

    def get_bs(self, name):
        value = self.cf.get("batch_size", name)
        return value


def list_of_groups(init_list, children_list_len):
    children_list_len = int(children_list_len)
    list_of_groups = zip(*(iter(init_list),) *children_list_len)
    end_list = [list(i) for i in list_of_groups]
    count = len(init_list) % children_list_len
    end_list.append(init_list[-count:]) if count !=0 else end_list
    return end_list

def cpu_info(cores_per_instance):
    '''
    sockets_num=$(lscpu |grep 'Socket(s):' |sed 's/[^0-9]//g')
    cores_per_socket=$(lscpu |grep 'Core(s) per socket:' |sed 's/[^0-9]//g')
    phsical_cores_num=$( echo "${sockets_num} * ${cores_per_socket}" |bc )
    numa_nodes_num=$(lscpu |grep 'NUMA node(s):' |sed 's/[^0-9]//g')
    cores_per_node=$( echo "${phsical_cores_num} / ${numa_nodes_num}" |bc )
    cores_per_instance=${cores_per_node}
    numa_nodes_use='  '
    '''

    get_sockets_num = subprocess.Popen("lscpu |grep 'Socket(s):' |sed 's/[^0-9]//g'", shell=True, stdout=subprocess.PIPE)
    sockets_num = get_sockets_num.stdout.readline()
    sockets_num = int(str(sockets_num).lstrip("b'").rstrip("\\n'"))

    get_cores_per_socket = subprocess.Popen("lscpu |grep 'Core(s) per socket:' |sed 's/[^0-9]//g'", shell=True, stdout=subprocess.PIPE)
    cores_per_socket = get_cores_per_socket.stdout.readline()
    cores_per_socket = int(str(cores_per_socket).lstrip("b'").rstrip("\\n'"))

    get_numa_nodes_num = subprocess.Popen("lscpu |grep 'NUMA node(s):' |sed 's/[^0-9]//g'", shell=True, stdout=subprocess.PIPE)
    numa_nodes_num = get_numa_nodes_num.stdout.readline()
    numa_nodes_num = int(str(numa_nodes_num).lstrip("b'").rstrip("\\n'"))

    phsical_cores_num = sockets_num * cores_per_socket
    cores_per_node = int(phsical_cores_num / numa_nodes_num)
    numa_nodes_use = numa_nodes_num

    cpu_array_shell = "numactl -H |grep 'node [0-9]* cpus:' |sed 's/.*node [0-9]* cpus: *//' | head -{0} |cut -f1-{1} -d' '".format(
        numa_nodes_use, int(cores_per_node))
    cpu_array = subprocess.Popen(cpu_array_shell, shell=True, stdout=subprocess.PIPE)
    cpu_array_output = cpu_array.stdout.readlines()
    cpu_cores_string = ''
    for one_core in cpu_array_output:
        new_one_core = str(one_core).lstrip("b'").replace("\\n'", " ")
        cpu_cores_string += new_one_core
    cpu_cores_list = cpu_cores_string.split(" ")
    new_cpu_cores_list = [x for x in cpu_cores_list if x != '']
    test_cores_list = list_of_groups(new_cpu_cores_list, cores_per_instance)
    cpu_info_list = [sockets_num, cores_per_socket, numa_nodes_num, phsical_cores_num, cores_per_node, cores_per_instance, numa_nodes_use]
    return cpu_info_list, test_cores_list

def inferenceMain(tensorflow_source, test_model_name, work_path, job_name, build_number, server_type, conda_path, precision, cores_per_instance_args, config_list, test_env_name, pip_source, trusted_host, model_name, log_model_name):
    sys.stdout.flush()
    print('=======start {0} {1} inference========='.format(tensorflow_source, test_model_name))
    model_path = work_path + '/intelai-models'
    test_mode = "inference"
    source_env = "source {1}/activate {0};".format(test_env_name, conda_path)
    # config_list = [config_dataset, config_pb, config_required, config_branch, config_bs]
    graph_path = config_list[1]
    config_required = config_list[2]
    config_branch = config_list[3]
    config_bs = config_list[4]
    config_dataset = config_list[0]
    batch_size_list = config_bs.split(',')
    checkout_branch = 'git checkout {0};'.format(config_branch)
    config_required_list = config_required.split(',')
    required_wheel = ''
    if config_required_list != [""]:
        for required_one in config_required_list:
            required_wheel += 'pip install {0} -i {1}  --trusted-host {2};'.format(required_one, pip_source, trusted_host)

    run_env = '{0}{1}git branch;git log -1;{2}'.format(source_env, checkout_branch, required_wheel)
    latency_cpu_info_list, latency_test_cores_list = cpu_info(cores_per_instance=int(cores_per_instance_args))
    throughput_cpu_info_list, throughput_test_cores_list = cpu_info(cores_per_instance=latency_cpu_info_list[1])

    if test_model_name == "ResNet-50-v1.5":
        for use_case in ["Latency", "Throughput"]:
            batch_size = batch_size_list[0] if use_case == "Latency" else batch_size_list[1]
            cores_list = latency_test_cores_list if use_case == 'Latency' else throughput_test_cores_list
            per_instance_cores = cores_per_instance_args if use_case == 'Latency' else throughput_cpu_info_list[1]
            log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_cores{7}".format(work_path, tensorflow_source, log_model_name,
                                                          precision, batch_size, use_case, len(cores_list), per_instance_cores)
            os.makedirs(log_file)
            test_command = ""
            for test_cores in cores_list:
                instance_num = cores_list.index(test_cores)
                one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                                  "numactl --localalloc --physcpubind={1}  " \
                                  "python ./benchmarks/launch_benchmark.py --benchmark-only --framework tensorflow " \
                                  "--model-name {2} --mode {3} " \
                                  "--precision {4} --batch-size {5} --in-graph {6} " \
                                  "--num-intra-threads {7} --num-inter-threads 1 -- warmup_steps=50 steps=200 " \
                                  "> {12}/{8}_{11}_{4}_bs{5}_{10}_{3}_instance{9}.log 2>&1 & "\
                    .format(len(test_cores), ','.join(test_cores), model_name, test_mode, precision, batch_size, graph_path, len(test_cores),
                            tensorflow_source, instance_num, use_case, log_model_name, log_file)

                # one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                #                    "taskset -c {1}-{2},{3}-{4}  " \
                #                    "python ./benchmarks/launch_benchmark.py --benchmark-only --framework tensorflow " \
                #                    "--model-name {5} --mode {6} " \
                #                    "--precision {7} --batch-size {8} --in-graph {9} " \
                #                    "--num-intra-threads {10} --num-inter-threads 1 -- warmup_steps=50 steps=200 " \
                #                    "> {15}/{11}_{14}_{7}_bs{8}_{13}_{6}_instance{12}.log 2>&1 & " \
                #     .format(len(test_cores), test_cores[0], test_cores[-1],
                #             int(test_cores[0])+latency_cpu_info_list[0] * latency_cpu_info_list[1],
                #             int(test_cores[1]) + latency_cpu_info_list[0] * latency_cpu_info_list[1],
                #             model_name, test_mode, precision, batch_size,
                #             graph_path, len(test_cores),
                #             tensorflow_source, instance_num, use_case, log_model_name, log_file)

                test_command += one_test_command

            test_command = test_command + "\n wait"
            sys.stdout.flush()
            print(test_command)
            test_command_file = "{6}/{0}_{1}_{2}_bs{3}_{7}_instance{4}_cores{5}.sh"\
                .format(tensorflow_source, log_model_name, precision, batch_size, len(cores_list), per_instance_cores, log_file, use_case)
            with open(test_command_file, 'w+') as tf:
                tf.write(test_command)
            sleep(10)
            os.chdir(model_path)
            all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
            sys.stdout.flush()
            print(all_test_command)
            subprocess.call(all_test_command, shell=True, executable="/bin/bash")
            log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name, precision, batch_size, use_case, log_file, test_mode)
            collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name, build_number=build_number, server_type=server_type)

    elif test_model_name == "MobileNet-v1":
        tf_models_dir = " " if precision == "int8" else " --model-source-dir {0}/{1}_models ".format(work_path, test_model_name)
        for use_case in ["Latency", "Throughput"]:
            batch_size = batch_size_list[0] if use_case == "Latency" else batch_size_list[1]
            cores_list = latency_test_cores_list if use_case == 'Latency' else throughput_test_cores_list
            per_instance_cores = cores_per_instance_args if use_case == 'Latency' else throughput_cpu_info_list[1]
            log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_cores{7}".format(work_path, tensorflow_source,
                                                                               log_model_name,
                                                                               precision, batch_size, use_case,
                                                                               len(cores_list), per_instance_cores)
            test_command = ""
            os.makedirs(log_file)
            for test_cores in cores_list:
                instance_num = cores_list.index(test_cores)
                one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                                   "numactl --localalloc --physcpubind={1}  " \
                                   "python ./benchmarks/launch_benchmark.py --benchmark-only --framework tensorflow " \
                                   "--model-name {2} --mode {3} " \
                                   "--precision {4} --batch-size {5} --in-graph {6}  {11} " \
                                   "--num-intra-threads {7} --num-inter-threads 1 " \
                                   "-- input_height=224 input_width=224 warmup_steps=500 steps=1000 " \
                                   "input_layer='input' output_layer='MobilenetV1/Predictions/Reshape_1' " \
                                   "> {13}/{8}_{12}_{4}_bs{5}_{10}_{3}_instance{9}.log 2>&1 & " \
                    .format(len(test_cores), ','.join(test_cores), model_name, test_mode, precision, batch_size,
                            graph_path, len(test_cores), tensorflow_source, instance_num, use_case, tf_models_dir, log_model_name, log_file)

                test_command += one_test_command
            test_command = test_command + "\n wait"
            sys.stdout.flush()
            print(test_command)
            test_command_file = "{6}/{0}_{1}_{2}_bs{3}_{7}_instance{4}_cores{5}.sh" \
                .format(tensorflow_source, log_model_name, precision, batch_size, len(cores_list), per_instance_cores,
                        log_file, use_case)
            with open(test_command_file, 'w+') as tf:
                tf.write(test_command)
            os.chdir(model_path)
            sleep(10)
            all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
            sys.stdout.flush()
            print(all_test_command)
            subprocess.call(all_test_command, shell=True, executable="/bin/bash")
            log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                           precision, batch_size, use_case,
                                                                           log_file, test_mode)
            collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                        build_number=build_number, server_type=server_type)

    elif test_model_name == "SSD-MobileNet-v1":
        for use_case in ["Latency", "Throughput"]:
            batch_size = batch_size_list[0] if use_case == "Latency" else batch_size_list[1]
            cores_list = latency_test_cores_list if use_case == 'Latency' else throughput_test_cores_list
            per_instance_cores = cores_per_instance_args if use_case == 'Latency' else throughput_cpu_info_list[1]
            log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_cores{7}".format(work_path, tensorflow_source,
                                                                               log_model_name,
                                                                               precision, batch_size, use_case,
                                                                               len(cores_list), per_instance_cores)
            os.makedirs(log_file)
            test_command = ""
            for test_cores in cores_list:
                instance_num = cores_list.index(test_cores)
                one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                                   "numactl --localalloc --physcpubind={1}  " \
                                   "python ./benchmarks/launch_benchmark.py --benchmark-only --framework tensorflow " \
                                   "--model-name {2} --mode {3} " \
                                   "--precision {4} --batch-size {5} --in-graph {6} " \
                                   "--num-intra-threads {7} --num-inter-threads 1 --verbose " \
                                   "> {12}/{8}_{11}_{4}_bs{5}_{10}_{3}_instance{9}.log 2>&1 & " \
                    .format(len(test_cores), ','.join(test_cores), model_name, test_mode, precision, batch_size,
                            graph_path, len(test_cores),
                            tensorflow_source, instance_num, use_case, log_model_name, log_file)

                test_command += one_test_command
            test_command = test_command + "\n wait"
            sys.stdout.flush()
            print(test_command)
            per_instance_cores = '4' if batch_size == '1' else throughput_cpu_info_list[1]
            test_command_file = "{6}/{0}_{1}_{2}_bs{3}_{7}_instance{4}_cores{5}.sh" \
                .format(tensorflow_source, log_model_name, precision, batch_size, len(cores_list), per_instance_cores,
                        log_file, use_case)
            with open(test_command_file, 'w+') as tf:
                tf.write(test_command)
            sleep(10)
            os.chdir(model_path)
            all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
            sys.stdout.flush()
            print(all_test_command)
            subprocess.call(all_test_command, shell=True, executable="/bin/bash")
            log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                           precision, batch_size, use_case,
                                                                           log_file, test_mode)
            collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                        build_number=build_number, server_type=server_type)

    elif test_model_name == "SSD-ResNet34":
        tf_models_dir = " --model-source-dir {0}/{1}_models ".format(work_path, test_model_name)
        for use_case in ['Latency', 'Throughput']:
            batch_size = batch_size_list[0]
            cores_list = latency_test_cores_list if use_case == 'Latency' else throughput_test_cores_list
            per_instance_cores = cores_per_instance_args if use_case == 'Latency' else throughput_cpu_info_list[1]
            log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_cores{7}".format(work_path, tensorflow_source,
                                                                               log_model_name,
                                                                               precision, batch_size, use_case,
                                                                               len(cores_list), per_instance_cores)
            os.makedirs(log_file)
            test_command = ""
            for test_cores in cores_list:
                instance_num = cores_list.index(test_cores)
                one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                                   "numactl --localalloc --physcpubind={1}  " \
                                   "python ./benchmarks/launch_benchmark.py --benchmark-only --framework tensorflow " \
                                   "--model-name {2} --mode {3}  {11}  " \
                                   "--precision {4} --batch-size {5} --in-graph {6} " \
                                   "--num-intra-threads {7} --num-inter-threads 1 -- input-size=1200 " \
                                   "> {13}/{8}_{12}_{4}_bs{5}_{10}_{3}_instance{9}.log 2>&1 & " \
                    .format(len(test_cores), ','.join(test_cores), model_name, test_mode, precision, batch_size,
                            graph_path, len(test_cores),
                            tensorflow_source, instance_num, use_case, tf_models_dir, log_model_name, log_file)

                test_command += one_test_command
            test_command = test_command + "\n wait"
            sys.stdout.flush()
            print(test_command)
            per_instance_cores = '4' if use_case == 'Latency' else throughput_cpu_info_list[1]
            test_command_file = "{6}/{0}_{1}_{2}_bs{3}_{7}_instance{4}_cores{5}.sh" \
                .format(tensorflow_source, log_model_name, precision, batch_size, len(cores_list), per_instance_cores,
                        log_file, use_case)
            with open(test_command_file, 'w+') as tf:
                tf.write(test_command)
            sleep(10)
            os.chdir(model_path)
            all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
            sys.stdout.flush()
            print(all_test_command)
            subprocess.call(all_test_command, shell=True, executable="/bin/bash")
            log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                           precision, batch_size, use_case,
                                                                           log_file, test_mode)
            collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                        build_number=build_number, server_type=server_type)

    elif test_model_name == "Bert_base":
        tf_models_dir = " --model-source-dir {0} ".format(work_path + "/bert")
        for use_case in ["Latency", "Throughput"]:
            batch_size = batch_size_list[0] if use_case == "Latency" else batch_size_list[1]
            cores_list = latency_test_cores_list if use_case == 'Latency' else throughput_test_cores_list
            per_instance_cores = cores_per_instance_args if use_case == 'Latency' else throughput_cpu_info_list[1]
            log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_cores{7}".format(work_path, tensorflow_source,
                                                                               log_model_name,
                                                                               precision, batch_size, use_case,
                                                                               len(cores_list), per_instance_cores)
            os.makedirs(log_file)
            test_command = ""
            for test_cores in cores_list:
                instance_num = cores_list.index(test_cores)
                one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                                   "numactl --localalloc --physcpubind={1}  " \
                                   " python ./benchmarks/launch_benchmark.py  --model-name {2}  " \
                                   "--mode {3}  --precision {4} --framework tensorflow  " \
                                   "--batch-size {5}  --benchmark-only  " \
                                   "--checkpoint {6} " \
                                   "--data-location {13}  " \
                                   "{14}  " \
                                   "--num-inter-threads 1  --num-intra-threads {7} --verbose  " \
                                   "-- task-name=MRPC max-seq-length=128 learning-rate=5e-5  " \
                                   "> {12}/{8}_{11}_{4}_bs{5}_{10}_{3}_instance{9}.log 2>&1 & " \
                    .format(len(test_cores), ','.join(test_cores), model_name, test_mode, precision, batch_size,
                            graph_path, len(test_cores), tensorflow_source, instance_num, use_case,
                            log_model_name, log_file, config_dataset, tf_models_dir)

                test_command += one_test_command
            test_command = test_command + "\n wait"
            sys.stdout.flush()
            print(test_command)
            per_instance_cores = '4' if batch_size == '1' else throughput_cpu_info_list[1]
            test_command_file = "{6}/{0}_{1}_{2}_bs{3}_{7}_instance{4}_cores{5}.sh" \
                .format(tensorflow_source, log_model_name, precision, batch_size, len(cores_list), per_instance_cores,
                        log_file, use_case)
            with open(test_command_file, 'w+') as tf:
                tf.write(test_command)
            sleep(10)
            os.chdir(model_path)
            all_test_command = "{0}{1};sleep 10;rm -fr {2}/benchmarks/common/tensorflow/logs/*".format(run_env, "bash {0}".format(test_command_file), model_path)
            sys.stdout.flush()
            print(all_test_command)
            subprocess.call(all_test_command, shell=True, executable="/bin/bash")
            log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                           precision, batch_size, use_case,
                                                                           log_file, test_mode)
            collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                        build_number=build_number, server_type=server_type)

    elif test_model_name == "Bert_tcnt":
        for use_case in ['Latency', 'Throughput']:
            batch_size = batch_size_list[0]
            cores_list = latency_test_cores_list if use_case == 'Latency' else throughput_test_cores_list
            per_instance_cores = cores_per_instance_args if use_case == 'Latency' else throughput_cpu_info_list[1]
            log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_cores{7}".format(work_path,
                                                                                            tensorflow_source,
                                                                                            log_model_name,
                                                                                            precision, batch_size,
                                                                                            use_case,
                                                                                            len(cores_list),
                                                                                            per_instance_cores)
            os.makedirs(log_file)
            test_command = ""
            for test_cores in cores_list:
                instance_num = cores_list.index(test_cores)
                one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                                   "numactl --localalloc --physcpubind={1}  " \
                                   " python ./benchmarks/launch_benchmark.py  --model-name {2} " \
                                   "--mode {3}  --precision {4} --framework tensorflow  " \
                                   "--batch-size {5} --in-graph  {6} --benchmark-only  --num-inter-threads 1 " \
                                   " --num-intra-threads {0} -- num-cores={0} " \
                                   "> {7}/{8}_{9}_{4}_bs{5}_{10}_{3}_instance{11}.log 2>&1 & "\
                    .format(len(test_cores), ','.join(test_cores), model_name, test_mode, precision, batch_size, graph_path,
                            log_file, tensorflow_source, log_model_name, use_case, instance_num)

                test_command += one_test_command
            test_command = test_command + "\n wait"
            sys.stdout.flush()
            print(test_command)
            per_instance_cores = '4' if use_case == 'Latency' else throughput_cpu_info_list[1]
            test_command_file = "{6}/{0}_{1}_{2}_bs{3}_{7}_instance{4}_cores{5}.sh" \
                .format(tensorflow_source, log_model_name, precision, batch_size, len(cores_list), per_instance_cores,
                        log_file, use_case)
            with open(test_command_file, 'w+') as tf:
                tf.write(test_command)
            sleep(10)
            os.chdir(model_path)
            all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
            sys.stdout.flush()
            print(all_test_command)
            subprocess.call(all_test_command, shell=True, executable="/bin/bash")
            log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                           precision, batch_size, use_case,
                                                                           log_file, test_mode)
            collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                        build_number=build_number, server_type=server_type)

    elif test_model_name == "Bert_large":
        # mkldnn_ops = "experimental-mkldnn-ops=False" if tensorflow_source == "upstream" else ""
        for use_case in ['Latency', 'Throughput']:
            batch_size = batch_size_list[0] if use_case == "Latency" else batch_size_list[1]
            cores_list = latency_test_cores_list if use_case == 'Latency' else throughput_test_cores_list
            per_instance_cores = cores_per_instance_args if use_case == 'Latency' else throughput_cpu_info_list[1]
            log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_cores{7}".format(work_path,
                                                                                            tensorflow_source,
                                                                                            log_model_name,
                                                                                            precision, batch_size,
                                                                                            use_case,
                                                                                            len(cores_list),
                                                                                            per_instance_cores)
            os.makedirs(log_file)
            test_command = ""
            for test_cores in cores_list:
                instance_num = cores_list.index(test_cores)
                one_test_command = "OMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 " \
                                   "numactl --localalloc --physcpubind={1}  " \
                                   " python ./benchmarks/launch_benchmark.py  --model-name={2} " \
                                   "--num-inter-threads 1 --num-intra-threads {0}  " \
                                   "--mode={3}  --precision={4} --framework=tensorflow  " \
                                   "--batch-size={5}  --data-location {6}  --checkpoint  {7}    " \
                                   "--output-dir  /tmp/bert-squad-output/   --benchmark-only  --verbose  " \
                                   "-- init_checkpoint=model.ckpt-7299 " \
                                   "> {8}/{9}_{10}_{4}_bs{5}_{11}_{3}_instance{12}.log 2>&1 & "\
                    .format(len(test_cores), ','.join(test_cores), model_name, test_mode, precision, batch_size,
                            config_dataset, graph_path,
                            log_file, tensorflow_source, log_model_name, use_case, instance_num)

                test_command += one_test_command
            test_command = test_command + "\n wait"
            sys.stdout.flush()
            print(test_command)
            per_instance_cores = '4' if use_case == 'Latency' else throughput_cpu_info_list[1]
            test_command_file = "{6}/{0}_{1}_{2}_bs{3}_{7}_instance{4}_cores{5}.sh" \
                .format(tensorflow_source, log_model_name, precision, batch_size, len(cores_list), per_instance_cores,
                        log_file, use_case)
            with open(test_command_file, 'w+') as tf:
                tf.write(test_command)
            sleep(10)
            os.chdir(model_path)
            all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
            sys.stdout.flush()
            print(all_test_command)
            subprocess.call(all_test_command, shell=True, executable="/bin/bash")
            log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                           precision, batch_size, use_case,
                                                                           log_file, test_mode)
            collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                        build_number=build_number, server_type=server_type)

def trainingMain(tensorflow_source, test_model_name, work_path, job_name, build_number, server_type, conda_path, precision, config_list, test_env_name, pip_source, trusted_host, model_name, log_model_name):
    print('=======start {0} {1} training========='.format(tensorflow_source, test_model_name))
    model_path = work_path + '/intelai-models'
    maskrcnn_path = work_path + '/mask_rcnn'
    test_mode = "training"
    use_case = "Throughput"
    source_env = "source {1}/activate {0};".format(test_env_name, conda_path)
    # this cores_per_instance, you can use any value here.
    cpu_info_list, cores_list = cpu_info(cores_per_instance=28)
    get_crti = 'find /usr/ -name crti* | head -n 1'
    crti_path_shell = subprocess.Popen(get_crti, shell=True, stdout=subprocess.PIPE)
    crti_path = crti_path_shell.stdout.readline()
    crti_path = str(crti_path).lstrip("b'").rstrip("\\n'")
    crti_path_env = '/'.join(crti_path.split('/')[:-1])
    # config_list = [config_dataset, config_pb, config_required, config_branch, config_bs]
    data_location = config_list[0]
    config_required = config_list[2]
    config_branch = config_list[3]
    config_bs = config_list[4]
    batch_size_list = config_bs.split(',')
    checkout_branch = 'git checkout {0};'.format(config_branch)
    config_required_list = config_required.split(',')
    required_wheel = 'export LIBRARY_PATH={0}:$LIBRARY_PATH;'.format(crti_path_env) if test_model_name in ["ResNet-50-v1.5", "SSD-ResNet34"] else ""
    if config_required_list != [""]:
        for required_one in config_required_list:
            if 'openmpi' in required_one:
                required_openmpi = 'export PATH={0}/bin:$PATH;'.format(required_one)
                required_wheel += required_openmpi
            elif 'horovod' == required_one:
                required_wheel += 'pip install --no-cache-dir horovod -i {0}  --trusted-host {1};'.format(pip_source, trusted_host)
            elif 'compilervars.sh' in required_one:
                required_intelmpi = 'source {0} intel64;'.format(required_one)
                required_wheel += required_intelmpi
            else:
                required_wheel += 'pip install {0} -i {1}  --trusted-host {2};'.format(required_one, pip_source, trusted_host)

    run_env = '{0}{1}git branch;git log -1;{2}'.format(source_env, checkout_branch, required_wheel)
    batch_size = batch_size_list[0]

    if test_model_name == "ResNet-50-v1.5":
        rm_tmp_ckp = 'rm -fr /tmp/tmp*;rm -fr /tmp/main;'
        run_env = run_env + rm_tmp_ckp
        log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_training".format(work_path, tensorflow_source,
                                                            log_model_name, precision, batch_size, use_case, cpu_info_list[0])
        os.makedirs(log_file)
        training_command = "python benchmarks/launch_benchmark.py --model-name={0} --precision={1} " \
                                "--mode={2} --framework tensorflow --data-location={3} " \
                                "--mpi_num_processes={4} --mpi_num_processes_per_socket=1 -b {5} -a {6} -e 2 " \
                                "-- steps=300 train_epochs=1 epochs_between_evals=1 " \
                                " 2>&1 | tee {10}/{7}_{9}_{1}_bs{5}_{8}_{2}_instance_{4}.log " \
            .format(model_name, precision, test_mode, data_location, cpu_info_list[0], batch_size, int(cpu_info_list[1])-4,
                    tensorflow_source, use_case, log_model_name, log_file)

        sys.stdout.flush()
        print(training_command)
        test_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance{5}_training.sh"\
            .format(tensorflow_source, log_model_name, precision, batch_size, log_file, cpu_info_list[0])
        with open(test_command_file, 'w+') as tf:
            tf.write(training_command)
        sleep(10)
        os.chdir(model_path)
        all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
        sys.stdout.flush()
        print(all_test_command)
        subprocess.call(all_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, batch_size, use_case,
                                                                       log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "MaskRCNN":
        # git_reset = 'git reset eb38bc21d4b899326dbb6ca4146c6ccc2ea372a1 --hard;'
        use_bfloat16 = "true" if precision == "bfloat16" else "false"
        log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_training".format(work_path, tensorflow_source,
                                                log_model_name, precision, batch_size, use_case, cpu_info_list[0])

        os.makedirs(log_file)
        maskrcnn_instance_number=1
        training_command = "export TF_CPP_MIN_LOG_LEVEL=0\nOMP_NUM_THREADS={0} KMP_AFFINITY=granularity=fine,verbose,compact,1,0 KMP_BLOCKTIME=0 " \
                           "KMP_SETTINGS=1 TF_MKL_OPTIMIZE_PRIMITVE_MEMUSE=false " \
                            "numactl --physcpubind={1} --membind=0 python mask_rcnn_main.py " \
                            "--hparams=first_lr_drop_step=9750,second_lr_drop_step=13000,total_steps=40," \
                            "train_batch_size={7},iterations_per_loop=500," \
                            "train_use_tpu_estimator=true,eval_use_tpu_estimator=true,cores_per_worker=8,use_bfloat16={2} " \
                            "--inter_op_parallelism_threads={11} --intra_op_parallelism_threads={3} " \
                            "--training_file_pattern={4} --model_dir=./model --use_tpu=false --num_cores=8 " \
                            "--warmup=20 2>&1 | tee {9}/{5}_MaskRCNN_{6}_bs{7}_{8}_{10}_instance_{11}.log " \
            .format(cpu_info_list[1], "0-{0}".format(int(cpu_info_list[1])-1), use_bfloat16, cpu_info_list[1], data_location,
                    tensorflow_source, precision, batch_size, use_case, log_file, test_mode, maskrcnn_instance_number)
        sys.stdout.flush()
        print(training_command)
        test_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance{5}_training.sh" \
            .format(tensorflow_source, log_model_name, precision, batch_size, log_file, cpu_info_list[0])
        with open(test_command_file, 'w+') as tf:
            tf.write(training_command)
        sleep(15)
        os.chdir(maskrcnn_path)
        all_test_command = "{0}{1};rm -fr ./model".format(run_env, "bash {0}".format(test_command_file))
        sys.stdout.flush()
        print(all_test_command)
        subprocess.call(all_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name, precision, batch_size,
                                                               use_case, log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "SSD-ResNet34":
        tf_models_dir = "{0}/{1}_{2}_models ".format(work_path, test_model_name, test_mode)
        log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_training".format(work_path, tensorflow_source,
                                                               log_model_name, precision, batch_size, use_case, cpu_info_list[0])
        os.makedirs(log_file)
        # git apply this branch patch
        subprocess.call('cd {0};{1}'.format(model_path, checkout_branch), shell=True)
        subprocess.call('cd {0};git reset --hard;git clean -fd;git apply {1}/models/object_detection/tensorflow/ssd-resnet34/training/fp32/tf-2.0.diff'.format(tf_models_dir, model_path), shell=True)

        training_command = "python ./benchmarks/launch_benchmark.py  --data-location {0}  " \
                           "--model-source-dir {1}  --model-name {2}  " \
                           "--framework tensorflow  --precision {3}  --mode {4} --num-cores {5}  " \
                           "--num-inter-threads 1 --num-intra-threads {5} --num-train-steps 100  " \
                           "--batch-size={6}  --weight_decay=1e-4 " \
                           "--mpi_num_processes={11}  --mpi_num_processes_per_socket=1  " \
                           "2>&1 | tee {10}/{7}_{8}_{3}_bs{6}_{9}_{4}_instance_{11}.log " \
            .format(data_location, tf_models_dir, model_name, precision, test_mode,
                    int(cpu_info_list[1])*2-2, batch_size,
                    tensorflow_source, log_model_name, use_case, log_file, cpu_info_list[0])

        sys.stdout.flush()
        print(training_command)
        test_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance{5}_training.sh" \
            .format(tensorflow_source, log_model_name, precision, batch_size, log_file, cpu_info_list[0])
        with open(test_command_file, 'w+') as tf:
            tf.write(training_command)
        sleep(10)
        os.chdir(model_path)
        all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
        sys.stdout.flush()
        print(all_test_command)
        subprocess.call(all_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name, precision, batch_size,
                                                               use_case, log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "Transformer_mlperf":
        log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_training".format(work_path, tensorflow_source,
                                                                                        log_model_name, precision,
                                                                                        batch_size, use_case,
                                                                                        cpu_info_list[0])
        os.makedirs(log_file)
        training_command = "python benchmarks/launch_benchmark.py --model-name {0} --precision {1} " \
                           "--mode {2} --framework tensorflow --data-location {3} " \
                           "--mpi_num_processes={4} --verbose " \
                           "-- random_seed=11 train_steps=100 steps_between_eval=100  " \
                           "params=big save_checkpoints='No' do_eval='No' print_iter=10  " \
                           " 2>&1 | tee {8}/{5}_{7}_{1}_bs{9}_{6}_{2}_instance_{4}.log  " \
            .format(model_name, precision, test_mode, data_location, cpu_info_list[0],
                    tensorflow_source, use_case, log_model_name, log_file, batch_size)

        sys.stdout.flush()
        print(training_command)
        test_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance{5}_training.sh" \
            .format(tensorflow_source, log_model_name, precision, batch_size, log_file, cpu_info_list[0])
        with open(test_command_file, 'w+') as tf:
            tf.write(training_command)
        sleep(10)
        os.chdir(model_path)
        all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file))
        sys.stdout.flush()
        print(all_test_command)
        subprocess.call(all_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, batch_size, use_case,
                                                                       log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "Bert_large":
        mkldnn_ops = "True" if tensorflow_source == "internal" else "False"
        log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_training".format(work_path, tensorflow_source,
                                                               log_model_name, precision, batch_size, use_case, cpu_info_list[0])
        os.makedirs(log_file)
        training_command = "OUTPUT=/tmp/bert_large_{2}_training\nrm -fr $OUTPUT\nmkdir -p $OUTPUT\n" \
                           "python ./benchmarks/launch_benchmark.py --verbose --mpi_num_processes={10}  " \
                           "--mpi_num_processes_per_socket=1  --num-intra-threads={0} " \
                           "--num-inter-threads=1 --model-name={1} --precision={2} --mode={3} --framework=tensorflow  " \
                           "--output-dir=$OUTPUT --batch-size={4} " \
                           "-- train-option=Pretraining config-file={5}/bert_config.json  " \
                           "input-file={5}/tf_examples.tfrecord  do-eval=False  do-train=True learning-rate=4e-5  " \
                           "init-checkpoint={5}/bert_model.ckpt  max-predictions=76 max-seq-length=512 num-train-steps=20 " \
                           "warmup-steps=0  save-checkpoints_steps=1000  profile=False  experimental-mkldnn-ops={11} " \
                           " 2>&1 | tee {6}/{7}_{8}_{2}_bs{4}_{9}_{3}_instance_{10}.log ".format(cpu_info_list[1]-2,
                                            model_name, precision, test_mode, batch_size, data_location, log_file,
                                            tensorflow_source, log_model_name, use_case, cpu_info_list[0], mkldnn_ops)

        sys.stdout.flush()
        print(training_command)
        test_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance{5}_training.sh" \
            .format(tensorflow_source, log_model_name, precision, batch_size, log_file, cpu_info_list[0])
        with open(test_command_file, 'w+') as tf:
            tf.write(training_command)
        sleep(10)
        os.chdir(model_path)
        all_test_command = "{0}{1}".format(run_env, "bash {0}".format(test_command_file), precision)
        sys.stdout.flush()
        print(all_test_command)
        subprocess.call(all_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name, precision, batch_size,
                                                               use_case, log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

def accuracyMain(tensorflow_source, test_model_name, work_path, job_name, build_number, server_type, conda_path, precision, cores_per_instance_args, config_list, test_env_name, pip_source, trusted_host, model_name, log_model_name):
    sys.stdout.flush()
    print('=======start {0} {1} accuracy========='.format(tensorflow_source, test_model_name))
    model_path = work_path + '/intelai-models'
    test_mode = "inference"
    use_case = 'Accuracy'
    source_env = "source {1}/activate {0};".format(test_env_name, conda_path)
    # config_list = [config_dataset, config_pb, config_required, config_branch, config_bs]
    graph_path = config_list[1]
    data_location = config_list[0]
    config_required = config_list[2]
    config_branch = config_list[3]
    config_bs = config_list[4]
    batch_size_list = config_bs.split(',')
    checkout_branch = 'git checkout {0};'.format(config_branch)
    config_required_list = config_required.split(',')
    required_wheel = ''
    if config_required_list != [""]:
        for required_one in config_required_list:
            required_wheel += 'pip install {0} -i {1}  --trusted-host {2};'.format(required_one, pip_source, trusted_host)
    run_env = '{0}{1}git branch;git log -1;{2}'.format(source_env, checkout_branch, required_wheel)
    accuracy_batch_size = batch_size_list[0]
    latency_cpu_info_list, latency_test_cores_list = cpu_info(cores_per_instance=int(cores_per_instance_args))

    if test_model_name == "ResNet-50-v1.5":
        accuracy_log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_accuracy".format(work_path,
                tensorflow_source, log_model_name, precision, accuracy_batch_size, use_case, latency_cpu_info_list[0])
        os.makedirs(accuracy_log_file)
        accuracy_command = 'python ./benchmarks/launch_benchmark.py --in-graph {0} --data-location {1}' \
                           ' --model-name {2}  --framework tensorflow  --precision {3} --mode inference ' \
                           ' --socket-id 0 --batch-size={4}  --accuracy-only ' \
                           '> {5}/{6}_{7}_{3}_bs{4}_{8}_{9}_instance_all.log 2>&1 ' \
            .format(graph_path, data_location, model_name, precision, accuracy_batch_size, accuracy_log_file,
                    tensorflow_source, log_model_name, use_case, test_mode)
        accuracy_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance1_accuracy.sh" \
            .format(tensorflow_source, log_model_name, precision, accuracy_batch_size, accuracy_log_file)
        sys.stdout.flush()
        print(accuracy_command)
        with open(accuracy_command_file, 'w+') as tf:
            tf.write(accuracy_command)
        sleep(10)
        os.chdir(model_path)
        all_accuracy_test_command = "{0}{1}".format(run_env, "bash {0}".format(accuracy_command_file))
        sys.stdout.flush()
        print(all_accuracy_test_command)
        subprocess.call(all_accuracy_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, accuracy_batch_size, use_case,
                                                                       accuracy_log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "MobileNet-v1":
        accuracy_log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_accuracy".format(work_path,
            tensorflow_source, log_model_name, precision, accuracy_batch_size, use_case, latency_cpu_info_list[0])
        os.makedirs(accuracy_log_file)
        accuracy_command = 'python ./benchmarks/launch_benchmark.py --in-graph {0} --data-location {1}' \
                           ' --model-name {2}  --framework tensorflow  --precision {3} --mode inference ' \
                           '--socket-id 0  --batch-size {4} ' \
                           '--accuracy-only -- input_height=224 input_width=224 ' \
                           'input_layer="input" output_layer="MobilenetV1/Predictions/Reshape_1"  ' \
                           '> {5}/{6}_{7}_{3}_bs{4}_{8}_{9}_instance_all.log 2>&1 ' \
            .format(graph_path, data_location, model_name, precision, accuracy_batch_size, accuracy_log_file,
                    tensorflow_source, log_model_name, use_case, test_mode)
        accuracy_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance1_accuracy.sh" \
            .format(tensorflow_source, log_model_name, precision, accuracy_batch_size, accuracy_log_file)
        sys.stdout.flush()
        print(accuracy_command)
        with open(accuracy_command_file, 'w+') as tf:
            tf.write(accuracy_command)
        sleep(10)
        os.chdir(model_path)
        all_accuracy_test_command = "{0}{1}".format(run_env, "bash {0}".format(accuracy_command_file))
        sys.stdout.flush()
        print(all_accuracy_test_command)
        subprocess.call(all_accuracy_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, accuracy_batch_size, use_case,
                                                                       accuracy_log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "SSD-MobileNet-v1":
        accuracy_log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_accuracy".format(work_path,
                            tensorflow_source, log_model_name, precision, accuracy_batch_size,
                            use_case, latency_cpu_info_list[0])
        os.makedirs(accuracy_log_file)
        accuracy_command = 'python ./benchmarks/launch_benchmark.py --in-graph {0} --data-location {1}' \
                           ' --model-name {2}  --framework tensorflow  --precision {3} --mode inference ' \
                           '--socket-id 0  --batch-size {4} --num-intra-threads {9}  --num-inter-threads 1  ' \
                           '--accuracy-only ' \
                           '> {5}/{6}_{7}_{3}_bs{4}_{8}_{10}_instance_all.log 2>&1 '\
            .format(graph_path, data_location, model_name, precision, accuracy_batch_size, accuracy_log_file,
                    tensorflow_source, log_model_name, use_case, latency_cpu_info_list[1], test_mode)
        sys.stdout.flush()
        print(accuracy_command)
        accuracy_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance1_accuracy.sh" \
            .format(tensorflow_source, log_model_name, precision, accuracy_batch_size, accuracy_log_file)
        with open(accuracy_command_file, 'w+') as tf:
            tf.write(accuracy_command)
        sleep(10)
        os.chdir(model_path)
        all_accuracy_test_command = "{0}{1}".format(run_env, "bash {0}".format(accuracy_command_file))
        sys.stdout.flush()
        print(all_accuracy_test_command)
        subprocess.call(all_accuracy_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, accuracy_batch_size, use_case,
                                                                       accuracy_log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "SSD-ResNet34":
        tf_models_dir = " --model-source-dir {0}/{1}_models ".format(work_path, test_model_name)
        accuracy_log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_accuracy".format(work_path,
                             tensorflow_source, log_model_name, precision, accuracy_batch_size,
                            use_case, latency_cpu_info_list[0])
        os.makedirs(accuracy_log_file)
        accuracy_command = 'python ./benchmarks/launch_benchmark.py --in-graph {0}  {1} ' \
                           '--data-location  {10}  ' \
                           ' --model-name {2}  --framework tensorflow  --precision {3} --mode inference ' \
                           '--socket-id 0  --batch-size={4} --accuracy-only -- input-size=1200  ' \
                           '> {5}/{6}_{7}_{3}_bs{4}_{8}_{9}_instance_all.log 2>&1 '.format(graph_path, tf_models_dir,
                                                      model_name, precision, accuracy_batch_size,
                                                      accuracy_log_file, tensorflow_source,
                                                      log_model_name, use_case, test_mode, data_location)
        accuracy_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance1_accuracy.sh" \
            .format(tensorflow_source, log_model_name, precision, accuracy_batch_size, accuracy_log_file)
        sys.stdout.flush()
        print(accuracy_command)
        with open(accuracy_command_file, 'w+') as tf:
            tf.write(accuracy_command)
        sleep(10)
        os.chdir(model_path)
        all_accuracy_test_command = "{0}{1}".format(run_env, "bash {0}".format(accuracy_command_file))
        sys.stdout.flush()
        print(all_accuracy_test_command)
        subprocess.call(all_accuracy_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, accuracy_batch_size, use_case,
                                                                       accuracy_log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "Bert_base":
        tf_models_dir = " --model-source-dir {0} ".format(work_path + "/bert ")
        accuracy_log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_accuracy".format(work_path,
                             tensorflow_source, log_model_name, precision, accuracy_batch_size,
                            use_case, latency_cpu_info_list[0])
        os.makedirs(accuracy_log_file)
        accuracy_command = 'python ./benchmarks/launch_benchmark.py --model-name {2}  --precision {3}  --mode inference ' \
                           ' --framework tensorflow  --batch-size {4} --socket-id 0  --accuracy-only  ' \
                           '--checkpoint  {0}  --data-location {10}  {1}  --num-inter-threads 1 ' \
                           '--num-intra-threads {11}  --verbose --output-dir {10}/tmp  ' \
                           '-- task-name=MRPC max-seq-length=128 learning-rate=2e-5 num_train_epochs=3.0 ' \
                           '> {5}/{6}_{7}_{3}_bs{4}_{8}_{9}_instance_all.log 2>&1 '.format(graph_path, tf_models_dir,
                                                      model_name, precision, accuracy_batch_size,
                                                      accuracy_log_file, tensorflow_source,
                                                      log_model_name, use_case, test_mode,
                                                      data_location, latency_cpu_info_list[1])

        accuracy_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance1_accuracy.sh" \
            .format(tensorflow_source, log_model_name, precision, accuracy_batch_size, accuracy_log_file)
        sys.stdout.flush()
        print(accuracy_command)
        with open(accuracy_command_file, 'w+') as tf:
            tf.write(accuracy_command)
        sleep(10)
        os.chdir(model_path)
        all_accuracy_test_command = "{0}{1}{2}".format(run_env, "rm -fr {0}/tmp/;".format(data_location), "bash {0}".format(accuracy_command_file))
        sys.stdout.flush()
        print(all_accuracy_test_command)
        subprocess.call(all_accuracy_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, accuracy_batch_size, use_case,
                                                                       accuracy_log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

    elif test_model_name == "Bert_large":
        accuracy_log_file = "{0}/all_test_log/{1}_{2}_{3}_bs{4}_{5}_instance{6}_accuracy".format(work_path,
                             tensorflow_source, log_model_name, precision, accuracy_batch_size,
                            use_case, latency_cpu_info_list[0])
        os.makedirs(accuracy_log_file)

        accuracy_command = 'python ./benchmarks/launch_benchmark.py --model-name={0} ' \
                           ' --precision={1}  --mode=inference ' \
                           ' --framework=tensorflow  --batch-size={2} ' \
                           ' --data-location {3} --checkpoint  {4}  ' \
                           ' --output-dir /tmp/bert-squad-output/  ' \
                           ' --accuracy-only  --verbose  ' \
                           ' -- init_checkpoint=model.ckpt-7299 ' \
                           ' > {5}/{6}_{7}_{1}_bs{2}_{8}_{9}_instance_all.log 2>&1 '.format(model_name,
                                                      precision, accuracy_batch_size, data_location,
                                                      graph_path, accuracy_log_file, tensorflow_source,
                                                      log_model_name, use_case, test_mode)

        accuracy_command_file = "{4}/{0}_{1}_{2}_bs{3}_instance1_accuracy.sh" \
            .format(tensorflow_source, log_model_name, precision, accuracy_batch_size, accuracy_log_file)
        sys.stdout.flush()
        print(accuracy_command)
        with open(accuracy_command_file, 'w+') as tf:
            tf.write(accuracy_command)
        sleep(10)
        os.chdir(model_path)
        all_accuracy_test_command = "{0}{1}{2}".format(run_env, "rm -fr {0}/tmp/;".format(data_location), "bash {0}".format(accuracy_command_file))
        sys.stdout.flush()
        print(all_accuracy_test_command)
        subprocess.call(all_accuracy_test_command, shell=True, executable="/bin/bash")
        log_dir = '{5}/{0}_{1}_{2}_bs{3}_{4}_{6}_instance*.log'.format(tensorflow_source, log_model_name,
                                                                       precision, accuracy_batch_size, use_case,
                                                                       accuracy_log_file, test_mode)
        collectMain(log_dir=log_dir, test_mode=test_mode, work_path=work_path, job_name=job_name,
                    build_number=build_number, server_type=server_type)

def collectMain(log_dir, test_mode, work_path, job_name, build_number, server_type):
    print('log dir:******{0}********'.format(log_dir))
    sleep(20)
    os.chdir(work_path)
    log_dir_list = log_dir.split('/')
    log_dir_path = '/'.join(log_dir_list[:-1])
    log_dir_path_2 = log_dir_list[-2]
    instance_cores_info = log_dir_list[-2]
    instance_cores_info_list = instance_cores_info.split('_')
    test_instance = instance_cores_info_list[-2].strip('instance')
    log_name = log_dir_list[-1].split('.')[0]
    info_list = log_name.split('_')
    tensorflow_source = info_list[0]
    model_name = info_list[1]
    precision = info_list[2]
    batch_size = info_list[3].strip('bs')
    use_case = info_list[4]
    result = ''
    model_name_dict = {'SSDMobilenetv1': 'SSD_Mobilenet_v1', 'Mobilenetv1': 'Mobilenet_v1',
                      'SSDResnet34': 'SSD_ResNet34', 'Resnet50v15': 'ResNet50_v1.5',
                      'MaskRCNN': "MaskRCNN", 'Transformer-mlperf': 'Transformer-mlperf',
                      "Bert-base": "Bert_base", "Bert-large": "Bert_large", "Bert-tcnt": "Bert_tcnt"}
    model_name_summary = model_name_dict[model_name]
    # all_log_name = '_'.join(info_list[0:6]) + '_all_instance.log'
    # link_log_name = log_dir_path_2 + '/' + all_log_name
    link_log_name = log_dir_path_2
    # subprocess.call('cat {0} > {2}/{1}'.format(log_dir, all_log_name, log_dir_path), shell=True)

    try:
        if test_mode == 'inference':
            if model_name_summary == 'SSD_Mobilenet_v1':
                get_throughput = 'cat {0} | grep "Total samples/sec:" | sed -e s"/.*: *//;s/samples\/s//"'.format(log_dir)
                if use_case == 'Latency' or use_case == 'Throughput':
                    run_throughput = subprocess.Popen(get_throughput, shell=True, stdout=subprocess.PIPE)
                    throughput_list = run_throughput.stdout.readlines()
                    throughput_sum = 0
                    for one_throughput in throughput_list:
                        throughput_sum += float(str(one_throughput).lstrip("b'").rstrip("\\n'"))
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'

                elif use_case == 'Accuracy':
                    get_accuracy = 'cat {0} | grep "Average Precision" | head -n 1 | sed -e "s/.*] = //" '.format(log_dir)
                    run_accuracy = subprocess.Popen(get_accuracy, shell=True, stdout=subprocess.PIPE)
                    accuracy_btye = run_accuracy.stdout.readline()
                    accuracy = float(str(accuracy_btye).lstrip("b'").rstrip("\\n'"))
                    result = accuracy

            elif model_name_summary == 'Mobilenet_v1':
                if use_case == 'Latency':
                    throughput_sum = 0
                    all_latency = 0
                    for instance_num in range(0, int(test_instance)):
                        one_instance_file_name = '{3}/{0}_Mobilenetv1_{1}_bs{4}_Latency_inference_instance{2}.log' \
                            .format(tensorflow_source, precision, str(instance_num), log_dir_path, batch_size)
                        throughput_command = 'cat {0} | grep -a "steps = 1000" | sed -e "s/.*, //;s/ images\/sec//"'.format(
                            one_instance_file_name)
                        get_results = subprocess.Popen(throughput_command, shell=True, stdout=subprocess.PIPE)
                        throughput_list = get_results.stdout.readlines()
                        one_throughput = float(str(throughput_list[0]).lstrip("b'").rstrip("\\n'"))
                        throughput_sum += one_throughput
                        one_latency = 1000 / one_throughput
                        all_latency += one_latency
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'
                    # latency = all_latency / int(test_instance)

                elif use_case == 'Throughput':
                    throughput_sum = 0
                    for instance_num in range(0, int(test_instance)):
                        one_instance_file_name = '{3}/{0}_Mobilenetv1_{1}_bs{4}_Throughput_inference_instance{2}.log' \
                            .format(tensorflow_source, precision, str(instance_num), log_dir_path, batch_size)
                        throughput_command = 'cat {0} | grep -a "steps = 1000" | sed -e "s/.*, //;s/ images\/sec//"'.format(
                            one_instance_file_name)
                        get_results = subprocess.Popen(throughput_command, shell=True, stdout=subprocess.PIPE)
                        throughput_list = get_results.stdout.readlines()
                        one_throughput = float(str(throughput_list[0]).lstrip("b'").rstrip("\\n'"))
                        throughput_sum += one_throughput
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'

                elif use_case == 'Accuracy':
                    # (0.7089, 0.8920)
                    get_accuracy = 'cat {0} | grep "Processed 50000 images" | sed -e "s/.* = //" '.format(log_dir)
                    run_accuracy = subprocess.Popen(get_accuracy, shell=True, stdout=subprocess.PIPE)
                    accuracy_btye = run_accuracy.stdout.readline()
                    accuracy_str = str(accuracy_btye).lstrip("b'").rstrip("\\n'")
                    accuracy_list = accuracy_str.split(',')
                    accuracy_top1 = accuracy_list[0].strip('(')
                    accuracy = float(accuracy_top1)
                    result = accuracy

            elif model_name_summary == 'ResNet50_v1.5':
                get_results = 'cat {0} | grep Throughput: | sed -e s"/.*: //"'.format(log_dir)
                if use_case == 'Latency':
                    run_throughput = subprocess.Popen(get_results, shell=True, stdout=subprocess.PIPE)
                    throughput_list = run_throughput.stdout.readlines()
                    throughput_sum = 0
                    for one_throughput in throughput_list:
                        one_throughput = float(str(one_throughput).lstrip("b'").rstrip("\\n'").rstrip(' images/sec'))
                        throughput_sum += one_throughput
                    # throughput = throughput_sum / int(test_instance)
                    # latency = 1000 / float(throughput)
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'

                elif use_case == 'Throughput':
                    run_throughput = subprocess.Popen(get_results, shell=True, stdout=subprocess.PIPE)
                    throughput_list = run_throughput.stdout.readlines()
                    throughput_sum = 0
                    for one_throughput in throughput_list:
                        one_throughput = float(
                            str(one_throughput).lstrip("b'").rstrip("\\n'").rstrip(' images/sec'))
                        throughput_sum += one_throughput
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'
                    # latency = (1000 * int(batch_size)) / float(throughput)

                elif use_case == 'Accuracy':
                    # (0.7594, 0.9286)
                    get_accuracy = 'cat {0} | grep "Processed 50000 images" | sed -e "s/.* = //" '.format(log_dir)
                    run_accuracy = subprocess.Popen(get_accuracy, shell=True, stdout=subprocess.PIPE)
                    accuracy_btye = run_accuracy.stdout.readline()
                    accuracy_str = str(accuracy_btye).lstrip("b'").rstrip("\\n'")
                    accuracy_list = accuracy_str.split(',')
                    accuracy_top1 = accuracy_list[0].strip('(')
                    accuracy = float(accuracy_top1)
                    result = accuracy

            elif model_name_summary == 'SSD_ResNet34':
                get_throughput = 'cat {0} | grep "Total samples/sec:" | sed -e s"/.*: *//;s/samples\/s//"'.format(
                    log_dir)
                if use_case == 'Latency' or use_case == 'Throughput':
                    run_throughput = subprocess.Popen(get_throughput, shell=True, stdout=subprocess.PIPE)
                    throughput_list = run_throughput.stdout.readlines()
                    throughput_sum = 0
                    for one_throughput in throughput_list:
                        throughput_sum += float(str(one_throughput).lstrip("b'").rstrip("\\n'"))
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'

                elif use_case == 'Accuracy':
                    get_accuracy = 'cat {0} | grep "Average Precision" | head -n 1 | sed -e "s/.*] = //" '.format(
                        log_dir)
                    run_accuracy = subprocess.Popen(get_accuracy, shell=True, stdout=subprocess.PIPE)
                    accuracy_btye = run_accuracy.stdout.readline()
                    accuracy = float(str(accuracy_btye).lstrip("b'").rstrip("\\n'"))
                    result = accuracy

            elif model_name_summary == 'Bert_base':
                get_latency = 'cat {0}  | grep "Time spent per iteration" | sed -e "s/.*: *//;s/ms//"'.format(log_dir)
                if use_case == 'Latency' or use_case == 'Throughput':
                    run_latency = subprocess.Popen(get_latency, shell=True, stdout=subprocess.PIPE)
                    latency_list = run_latency.stdout.readlines()
                    throughput_sum = 0
                    for one_latency in latency_list:
                        throughput_sum += (int(batch_size)*1000)/float(str(one_latency).lstrip("b'").rstrip("\\n'"))
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'

                elif use_case == 'Accuracy':
                    get_accuracy = 'cat {0} | grep "eval_accuracy = " | tail -n 1 | sed -e "s/.* = //" '.format(
                        log_dir)
                    run_accuracy = subprocess.Popen(get_accuracy, shell=True, stdout=subprocess.PIPE)
                    accuracy_btye = run_accuracy.stdout.readline()
                    accuracy = float(str(accuracy_btye).lstrip("b'").rstrip("\\n'"))
                    result = accuracy

            elif model_name_summary == 'Bert_tcnt':
                get_throughput = 'cat {0}  | grep "Throughput =" | sed -e "s/.*= //"'.format(log_dir)
                if use_case == 'Latency' or use_case == 'Throughput':
                    run_throughput = subprocess.Popen(get_throughput, shell=True, stdout=subprocess.PIPE)
                    throughput_list = run_throughput.stdout.readlines()
                    throughput_sum = 0
                    for one_throughput in throughput_list:
                        throughput_sum += float(str(one_throughput).lstrip("b'").rstrip("\\n'"))
                    result = throughput_sum if throughput_sum != 0 else 'FAILED'

            elif model_name_summary == 'Bert_large':
                # this models is get all time not latency
                get_all_time = 'cat {0}  | grep "Elapsed time:" | sed -e "s/.*: *//" '.format(log_dir)
                if use_case == 'Latency' or use_case == 'Throughput':
                    run_all_time = subprocess.Popen(get_all_time, shell=True, stdout=subprocess.PIPE)
                    all_time_list = run_all_time.stdout.readlines()
                    all_time_sum = 0
                    for one_time in all_time_list:
                        all_time_sum += float(str(one_time).lstrip("b'").rstrip("\\n'"))
                    mean_time = all_time_sum/len(all_time_list)
                    result = mean_time if mean_time != 0 else 'FAILED'

                elif use_case == 'Accuracy':
                    get_accuracy = 'cat {0} | grep "f1" | sed -e "s/.*: *//;s/}//" '.format(
                        log_dir)
                    run_accuracy = subprocess.Popen(get_accuracy, shell=True, stdout=subprocess.PIPE)
                    accuracy_btye = run_accuracy.stdout.readline()
                    accuracy = float(str(accuracy_btye).lstrip("b'").rstrip("\\n'"))
                    result = accuracy

        elif test_mode == 'training':
            if model_name_summary == 'SSD_ResNet34':
                throughput_command = 'cat {0} | grep "total images/sec:" | sed -e s"/.*: //" | tail -n 1 '.format(log_dir)
                run_throughput = subprocess.Popen(throughput_command, shell=True, stdout=subprocess.PIPE)
                throughput_byte = run_throughput.stdout.readline()
                throughput = float(str(throughput_byte).lstrip("b'").rstrip("\\n'"))
                result = throughput

            elif model_name_summary == 'ResNet50_v1.5':
                throughput_command = 'cat {0} | grep "global_step/sec:" | sed -e s"/.*: //" | tail -n 1'.format(log_dir)
                get_global_step = subprocess.Popen(throughput_command, shell=True, stdout=subprocess.PIPE)
                global_step_byte = get_global_step.stdout.readline()
                global_step = float(str(global_step_byte).lstrip("b'").rstrip("\\n'"))
                throughput = global_step * int(batch_size) * int(test_instance)
                latency = ''
                result = throughput

            elif model_name_summary == "MaskRCNN":
                throughput_command = 'cat {0} | grep "Total samples/sec: " | sed -e s"/.*: //;s/samples\/s//" '.format(log_dir)
                run_throughput = subprocess.Popen(throughput_command, shell=True, stdout=subprocess.PIPE)
                throughput_byte = run_throughput.stdout.readline()
                throughput = float(str(throughput_byte).lstrip("b'").rstrip("\\n'"))
                throughput = throughput * int(test_instance)
                result = throughput

            elif model_name_summary == "Transformer-mlperf":
                throughput_command = 'cat {0} | grep "INFO:tensorflow:Batch \[90\]" | tail -n {1} | sed -e "s/.* average exp\/sec = *//" '.format(log_dir, test_instance)
                run_throughput = subprocess.Popen(throughput_command, shell=True, stdout=subprocess.PIPE)
                throughput_byte = run_throughput.stdout.readlines()
                throughput_sum = 0
                for one_throughput_byte in throughput_byte:
                    throughput_sum += float(str(one_throughput_byte).lstrip("b'").rstrip("\\n'"))
                result = throughput_sum if throughput_sum != 0 else 'FAILED'

            elif model_name_summary == "Bert_large":
                throughput_command = 'cat {0} | grep "INFO:tensorflow:examples/sec:" | tail -n {1} | sed -e "s/.*: //"'.format(
                    log_dir, test_instance)
                run_throughput = subprocess.Popen(throughput_command, shell=True, stdout=subprocess.PIPE)
                throughput_byte = run_throughput.stdout.readlines()
                throughput_sum = 0
                for one_throughput_byte in throughput_byte:
                    throughput_sum += float(str(one_throughput_byte).lstrip("b'").rstrip("\\n'"))
                result = throughput_sum if throughput_sum != 0 else 'FAILED'


    except Exception as ex:
        print("===========COLLECT {0}_{1}_{2}_{3}_{4}_{5} FAILED============".format(tensorflow_source, model_name,
                                                                                     test_mode, precision, batch_size, use_case))
        result = "FAILED"
        log_info = "{0};{1};{2};{3};{4};{5};{6};{7};{8};{9}".format(model_name_summary, test_mode, server_type,
                                                                    precision, use_case, batch_size, result,
                                                                    link_log_name, job_name, build_number)
        with open("{0}_{1}_{2}_{3}_{4}_{5}_{6}_summary_one.log".format(tensorflow_source, model_name, test_mode,
                                                                       precision, batch_size, use_case, server_type),
                  "w+") as tf:
            tf.write(log_info)
    else:
        log_info = "{0};{1};{2};{3};{4};{5};{6};{7};{8};{9}".format(model_name_summary, test_mode, server_type,
                                                                    precision, use_case, batch_size, result,
                                                                    link_log_name, job_name, build_number)

        with open("{0}_{1}_{2}_{3}_{4}_{5}_{6}_summary_one.log".format(tensorflow_source, model_name, test_mode,
                                                    precision, batch_size, use_case, server_type), "w+") as tf:
            tf.write(log_info)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="run LZ models jenkins")
    parser.add_argument("--tensorflow_source", "-t",
                        help="select upstream or internal", required=True)
    parser.add_argument("--model_name", "-m", help="select test model name", required=True)
    parser.add_argument("--test_mode", "-tm", help="select test_mode (inference, accuracy, training)", required=True)
    parser.add_argument("--precision", "-p", help="select test precision", required=True)
    parser.add_argument("--cores_per_instance", "-cpi", help="select realtime every instance used cores number", default=4)
    parser.add_argument("--config_path", "-cfp", help="config file path", required=True)
    parser.add_argument("--work_path", "-w", help="jenkins project workspace", required=True)
    parser.add_argument("--job_name", "-jn", help="jenkins project job name", required=True)
    parser.add_argument("--build_number", "-bn", help="jenkins project job build number", required=True)
    parser.add_argument("--server_type", "-st", help="test used server type", required=True)
    parser.add_argument("--conda_path", "-cp", help="conda install path", required=True)

    args = parser.parse_args()
    tensorflow_source = args.tensorflow_source
    test_model_name = args.model_name
    test_mode = args.test_mode
    precision = args.precision
    cores_per_instance_args = args.cores_per_instance
    config_path = args.config_path
    work_path = args.work_path
    job_name = args.job_name
    build_number = args.build_number
    server_type = args.server_type
    conda_path = args.conda_path
    model_path = work_path + '/intelai-models'

    pip_source = "http://pypi.douban.com/simple/"
    trusted_host = pip_source.split("/")[2]
    pip_wheel = "pip install {1}/tensorflow_{0}_wheel/tensorflow-*.*.*-cp36-cp36m-linux_x86_64.whl -i {2} --trusted-host {3}".format(
        tensorflow_source, work_path, pip_source, trusted_host)
    env_test_mode = test_mode if test_mode == 'training' else 'inference'
    test_env_name = '{0}_{1}'.format(test_model_name, env_test_mode)

    attempts = 0
    success = False
    while attempts < 5 and not success:
        prepare_env = "conda remove --all -y -n {0};conda create python=3.6.9 -y -n {0};source {2}/activate {0};{1}".format(test_env_name, pip_wheel, conda_path)
        create_env = subprocess.call(prepare_env, shell=True, executable="/bin/bash")
        if create_env == 0:
            success = True
        else:
            attempts += 1
            print('create conda env failed, retry {0}'.format(attempts))
    config_file = ReadConfig()
    if test_model_name in ["Bert_base", "Bert_large"] and test_mode != 'training':
        config_dataset = config_file.get_dataset('{0}_{1}'.format(test_model_name, 'accuracy'))
    else:
        config_dataset = '' if test_mode == 'inference' else config_file.get_dataset('{0}_{1}'.format(test_model_name, test_mode))

    config_pb = config_file.get_pb('{0}_{1}'.format(test_model_name, precision)) if test_mode != 'training' else ''
    config_test_mode = 'inference' if test_mode == 'accuracy' else test_mode
    config_required = config_file.get_required('{0}_{1}'.format(test_model_name, config_test_mode))
    config_branch = config_file.get_branch('{0}_{1}'.format(test_model_name, config_test_mode))
    if test_model_name == 'ResNet-50-v1.5' and test_mode == 'training':
        config_bs = config_file.get_bs('{0}_{1}_{2}'.format(test_model_name, test_mode, precision))
    else:
        config_bs = config_file.get_bs('{0}_{1}'.format(test_model_name, test_mode))
    config_list = [config_dataset, config_pb, config_required, config_branch, config_bs]

    all_models_name = {
        "ResNet-50-v1.5": ["resnet50v1_5", "Resnet50v15"],
        "MaskRCNN": ["MaskRCNN", "MaskRCNN"],
        "Transformer_mlperf": ["transformer_mlperf", "Transformer-mlperf"],
        "SSD-ResNet34": ["ssd-resnet34", "SSDResnet34"],
        "Bert_large": ["bert_large", "Bert-large"],
        "MobileNet-v1": ["mobilenet_v1", "Mobilenetv1"],
        "SSD-MobileNet-v1": ["ssd-mobilenet", "SSDMobilenetv1"],
        "Bert_base": ["bert", "Bert-base"],
        "Bert_tcnt": ["bert", "Bert-tcnt"]
    }
    model_name = all_models_name[test_model_name][0]
    log_model_name = all_models_name[test_model_name][1]
    if test_mode == 'inference':
        inferenceMain(tensorflow_source=tensorflow_source, test_model_name=test_model_name, work_path=work_path,
                      job_name=job_name, build_number=build_number, server_type=server_type,
                      conda_path=conda_path, precision=precision, cores_per_instance_args=cores_per_instance_args,
                      config_list=config_list, test_env_name=test_env_name,
                      pip_source=pip_source, trusted_host=trusted_host, model_name=model_name, log_model_name=log_model_name)
    elif test_mode == 'accuracy':
        accuracyMain(tensorflow_source=tensorflow_source, test_model_name=test_model_name, work_path=work_path,
                     job_name=job_name, build_number=build_number, server_type=server_type,
                     conda_path=conda_path, precision=precision, cores_per_instance_args=cores_per_instance_args,
                     config_list=config_list, test_env_name=test_env_name,
                     pip_source=pip_source, trusted_host=trusted_host, model_name=model_name, log_model_name=log_model_name)
    elif test_mode == 'training':
        trainingMain(tensorflow_source=tensorflow_source, test_model_name=test_model_name, work_path=work_path,
                     job_name=job_name, build_number=build_number, server_type=server_type,
                     conda_path=conda_path, precision=precision, config_list=config_list,
                     test_env_name=test_env_name, pip_source=pip_source, trusted_host=trusted_host,
                     model_name=model_name, log_model_name=log_model_name)
