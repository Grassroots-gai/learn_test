import webbrowser
import csv
from itertools import islice
import os
import time
import argparse
import subprocess
import glob


def write_html(new_data, benchmark_html, summary_html):
    now = int(time.time())
    timeArray = time.localtime(now)
    otherStyleTime = time.strftime("%Y-%m-%d %H:%M:%S", timeArray)
    report_html = "{0}/Tensorflow_LandingZone_models_report.html".format(new_data + '/..')
    with open(report_html, 'w') as f:
        message = """
       <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Tensorflow LandingZone Models Weekly Report</title>
            <link rel="stylesheet" type="text/css" href="http://mlpc.intel.com/static/doc/tensorflow/css/LZ_jenkins.css">
        </head>
        <body>
            <table width="100%" border="0" cellspacing="0" cellpadding="0" align="center">
              <tr>
                    <td align="center" class="biaoti" height="60">Tensorflow LandingZone Models Weekly Report</td>
              </tr>
              <tr>
                    <td align="right" height="20">{0}</td>
              </tr>
            </table> 
              {1}
              {2}
              {3}
        </body>
        </html>
        """ \
            .format(otherStyleTime, summary_html[0], benchmark_html, summary_html[1])
        f.write(message)
    webbrowser.open(report_html)


def collect_results(results_list, tensorflow_source):
    Resnet50_v15_inference_dict = {"model_name": "ResNet50-v15#inference", "new_list": [], "old_list": [],
                                   "best_list": []}
    MobileNet_v1_inference_dict = {"model_name": "MobileNet-v1#inference", "new_list": [], "old_list": [],
                                   "best_list": []}
    SSD_MobileNet_v1_inference_dict = {"model_name": "SSD-Mobilenet-v1#inference", "new_list": [], "old_list": [], "best_list": []}
    SSD_Resnet34_inference_dict = {"model_name": "SSD-ResNet34#inference", "new_list": [], "old_list": [],
                                   "best_list": []}
    Resnet50_v15_training_dict = {"model_name": "ResNet50-v15#training", "new_list": [], "old_list": [],
                                  "best_list": []}
    SSD_Resnet34_training_dict = {"model_name": "SSD-ResNet34#training", "new_list": [], "old_list": [],
                                  "best_list": []}
    MaskRCNN_training_dict = {"model_name": "MaskRCNN#training", "new_list": [], "old_list": [],
                              "best_list": []}
    Transformer_mlperf_training_dict = {"model_name": "Transformer-mlperf#training", "new_list": [], "old_list": [],
                              "best_list": []}
    Bert_base_inference_dict = {"model_name": "Bert_base#inference", "new_list": [], "old_list": [],
                                        "best_list": []}
    Bert_large_training_dict = {"model_name": "Bert_large#training", "new_list": [], "old_list": [],
                                "best_list": []}
    Bert_tcnt_inference_dict = {"model_name": "Bert_tcnt#inference", "new_list": [], "old_list": [],
                                "best_list": []}
    Bert_large_inference_dict = {"model_name": "Bert_large#inference", "new_list": [], "old_list": [],
                                "best_list": []}

    all_models_info_list = []
    # print("tensorflow_source:  " + tensorflow_source)
    for one_results in results_list:
        if results_list.index(one_results) == 0:
            results_data = 'new_list'
        elif results_list.index(one_results) == 1:
            results_data = 'old_list'
        else:
            results_data = 'best_list'
        with open(one_results, 'r+') as of:
            one_data = of.readlines()
            for one_data_row in one_data[1:]:
                one_data_list = one_data_row.split(';')
                if one_data_list[0] == 'ResNet50_v1.5' and one_data_list[1] == "inference":
                    Resnet50_v15_inference_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'ResNet50_v1.5' and one_data_list[1] == "training":
                    Resnet50_v15_training_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'SSD_ResNet34' and one_data_list[1] == "inference":
                    SSD_Resnet34_inference_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'SSD_ResNet34' and one_data_list[1] == "training":
                    SSD_Resnet34_training_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'SSD_Mobilenet_v1' and one_data_list[1] == "inference":
                    SSD_MobileNet_v1_inference_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'Mobilenet_v1' and one_data_list[1] == "inference":
                    MobileNet_v1_inference_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'MaskRCNN' and one_data_list[1] == "training":
                    MaskRCNN_training_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'Transformer-mlperf' and one_data_list[1] == "training":
                    Transformer_mlperf_training_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'Bert_base' and one_data_list[1] == "inference":
                    Bert_base_inference_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'Bert_large' and one_data_list[1] == "training":
                    Bert_large_training_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'Bert_tcnt' and one_data_list[1] == "inference":
                    Bert_tcnt_inference_dict['{0}'.format(results_data)].append(one_data_list)
                elif one_data_list[0] == 'Bert_large' and one_data_list[1] == "inference":
                    Bert_large_inference_dict['{0}'.format(results_data)].append(one_data_list)


    all_dict = [Resnet50_v15_inference_dict, MobileNet_v1_inference_dict, SSD_MobileNet_v1_inference_dict,
                SSD_Resnet34_inference_dict, Resnet50_v15_training_dict, SSD_Resnet34_training_dict,
                MaskRCNN_training_dict, Transformer_mlperf_training_dict, Bert_base_inference_dict,
                Bert_large_training_dict, Bert_tcnt_inference_dict, Bert_large_inference_dict]

    for one_model_dict in all_dict:
        if len(one_model_dict['new_list']) == 0:
            print('{0} is not have new data'.format(one_model_dict['model_name']))
        else:
            one_model_info_list = collect_one_model_result(one_model_dict=one_model_dict)
            all_models_info_list.append(one_model_info_list)
    all_models_info_list.append(tensorflow_source)
    return all_models_info_list


def collect_one_model_result(one_model_dict):
    [fp32_latency, fp32_bs1_file_name, fp32_latency_bs,
     fp32_throughput, fp32_bsdefault_file_name, fp32_throughput_bs,
     fp32_accuracy, fp32_accuracy_file_name, fp32_accuracy_bs,
     int8_latency, int8_bs1_file_name, int8_latency_bs,
     int8_throughput, int8_bsdefault_file_name, int8_throughput_bs,
     int8_accuracy, int8_accuracy_file_name, int8_accuracy_bs,
     bf16_latency, bf16_bs1_file_name, bf16_latency_bs,
     bf16_throughput, bf16_bsdefault_file_name, bf16_throughput_bs,
     bf16_accuracy, bf16_accuracy_file_name, bf16_accuracy_bs] = [""] * 27

    one_model_all_data = []
    one_model_all_file = []
    one_model_all_bs = []
    model_name = one_model_dict['model_name'].split('#')[0]
    test_mode = one_model_dict['model_name'].split('#')[1]
    for one_date in ['new_list', 'old_list', 'best_list']:
        for one_data in one_model_dict[one_date]:
            if one_data[4] == 'Latency' and one_data[3] == 'fp32':
                fp32_latency = one_data[6]
                fp32_bs1_file_name = one_data[7]
                fp32_latency_bs = one_data[5]
            elif one_data[4] == 'Throughput' and one_data[3] == 'fp32':
                fp32_throughput = one_data[6]
                fp32_bsdefault_file_name = one_data[7]
                fp32_throughput_bs = one_data[5]
            elif one_data[4] == 'Accuracy' and one_data[3] == 'fp32':
                fp32_accuracy = one_data[6]
                fp32_accuracy_file_name = one_data[7]
                fp32_accuracy_bs = one_data[5]

            elif one_data[4] == 'Latency' and one_data[3] == 'int8':
                int8_latency = one_data[6]
                int8_bs1_file_name = one_data[7]
                int8_latency_bs = one_data[5]
            elif one_data[4] == 'Throughput' and one_data[3] == 'int8':
                int8_throughput = one_data[6]
                int8_bsdefault_file_name = one_data[7]
                int8_throughput_bs = one_data[5]
            elif one_data[4] == 'Accuracy' and one_data[3] == 'int8':
                int8_accuracy = one_data[6]
                int8_accuracy_file_name = one_data[7]
                int8_accuracy_bs = one_data[5]

            elif one_data[4] == 'Latency' and one_data[3] == 'bfloat16':
                bf16_latency = one_data[6]
                bf16_bs1_file_name = one_data[7]
                bf16_latency_bs = one_data[5]
            elif one_data[4] == 'Throughput' and one_data[3] == 'bfloat16':
                bf16_throughput = one_data[6]
                bf16_bsdefault_file_name = one_data[7]
                bf16_throughput_bs = one_data[5]
            elif one_data[4] == 'Accuracy' and one_data[3] == 'bfloat16':
                bf16_accuracy = one_data[6]
                bf16_accuracy_file_name = one_data[7]
                bf16_accuracy_bs = one_data[5]
        data_list = [fp32_latency, fp32_throughput, fp32_accuracy,
                     int8_latency, int8_throughput, int8_accuracy,
                     bf16_latency, bf16_throughput, bf16_accuracy
                     ]
        file_name_list = [
            fp32_bs1_file_name, fp32_bsdefault_file_name, fp32_accuracy_file_name,
            int8_bs1_file_name, int8_bsdefault_file_name, int8_accuracy_file_name,
            bf16_bs1_file_name, bf16_bsdefault_file_name, bf16_accuracy_file_name
        ]
        bs_list = [
            fp32_latency_bs, fp32_throughput_bs, fp32_accuracy_bs,
            int8_latency_bs, int8_throughput_bs, int8_accuracy_bs,
            bf16_latency_bs, bf16_throughput_bs, bf16_accuracy_bs
        ]

        one_model_all_data.append(data_list)
        one_model_all_file.append(file_name_list)
        one_model_all_bs.append(bs_list)

    all_file_name_list = [one_model_all_file[0][0], one_model_all_file[0][1], one_model_all_file[0][2],
                          one_model_all_file[1][0], one_model_all_file[1][1], one_model_all_file[1][2],
                          one_model_all_file[2][0], one_model_all_file[2][1], one_model_all_file[2][2],
                          one_model_all_file[0][3], one_model_all_file[0][4], one_model_all_file[0][5],
                          one_model_all_file[1][3], one_model_all_file[1][4], one_model_all_file[1][5],
                          one_model_all_file[2][3], one_model_all_file[2][4], one_model_all_file[2][5],
                          one_model_all_file[0][6], one_model_all_file[0][7], one_model_all_file[0][8],
                          one_model_all_file[1][6], one_model_all_file[1][7], one_model_all_file[1][8],
                          one_model_all_file[2][6], one_model_all_file[2][7], one_model_all_file[2][8]]

    bs_list = [one_model_all_bs[0], one_model_all_bs[1], one_model_all_bs[2]]

    one_model_info_list = [model_name, test_mode, bs_list,
                     one_model_all_data[0][0], one_model_all_data[0][1], one_model_all_data[0][2],
                     one_model_all_data[0][3], one_model_all_data[0][4], one_model_all_data[0][5],
                     one_model_all_data[0][6], one_model_all_data[0][7], one_model_all_data[0][8],
                     one_model_all_data[1][0], one_model_all_data[1][1], one_model_all_data[1][2],
                     one_model_all_data[1][3], one_model_all_data[1][4], one_model_all_data[1][5],
                     one_model_all_data[1][6], one_model_all_data[1][7], one_model_all_data[1][8],
                     one_model_all_data[2][0], one_model_all_data[2][1], one_model_all_data[2][2],
                     one_model_all_data[2][3], one_model_all_data[2][4], one_model_all_data[2][5],
                     one_model_all_data[2][6], one_model_all_data[2][7], one_model_all_data[2][8],
                     all_file_name_list]
    return one_model_info_list


def isFloat(x):
    try:
        float(x)
        if str(x) in ['inf', 'infinity', 'INF', 'INFINITY', 'True', 'NAN', 'nan', 'False', '-inf', '-INF', '-INFINITY',
                      '-infinity', 'NaN', 'Nan']:
            return False
        else:
            return True
    except:
        return False


def get_info_summary(summary):
    summary_list = []
    upstream_commit_command = 'cat {0} |  grep "upstream commit id"  | sed -e "s/.*: //" '.format(summary)
    upstream_branch_command = 'cat {0} |  grep "upstream branch" | sed -e "s/.*: //" '.format(summary)
    upstream_repo_command = 'cat {0} | grep "upstream url" | sed -e "s/.*: //" '.format(summary)

    internal_commit_command = 'cat {0} |  grep "internal commit id"  | sed -e "s/.*: //" '.format(summary)
    internal_branch_command = 'cat {0} |  grep "internal branch" | sed -e "s/.*: //" '.format(summary)
    internal_repo_command = 'cat {0} | grep "internal url" | sed -e "s/.*: //" '.format(summary)

    uc = subprocess.Popen(upstream_commit_command, shell=True, stdout=subprocess.PIPE)
    ub = subprocess.Popen(upstream_branch_command, shell=True, stdout=subprocess.PIPE)
    ur = subprocess.Popen(upstream_repo_command, shell=True, stdout=subprocess.PIPE)

    ic = subprocess.Popen(internal_commit_command, shell=True, stdout=subprocess.PIPE)
    ib = subprocess.Popen(internal_branch_command, shell=True, stdout=subprocess.PIPE)
    ir = subprocess.Popen(internal_repo_command, shell=True, stdout=subprocess.PIPE)

    upstream_commit = str(uc.stdout.readline()).lstrip("b'").rstrip("\\n'")
    upstream_branch = str(ub.stdout.readline()).lstrip("b'").rstrip("\\n'")
    upstream_repo = str(ur.stdout.readline()).lstrip("b'").rstrip("\\n'")
    internal_commit = str(ic.stdout.readline()).lstrip("b'").rstrip("\\n'")
    internal_branch = str(ib.stdout.readline()).lstrip("b'").rstrip("\\n'")
    internal_repo = str(ir.stdout.readline()).lstrip("b'").rstrip("\\n'")

    summary_list = [upstream_commit, upstream_branch, upstream_repo,
                    internal_commit, internal_branch, internal_repo]
    return summary_list


def get_summary_part(summary, new_data_path, old_data_path, blank_table, build_command, gcc_version, machine_info):
    new_summary = new_data_path + '/' + summary
    regerence_summary = old_data_path + '/' + summary
    new_info_list = get_info_summary(summary=new_summary)
    reference_info_list = get_info_summary(summary=regerence_summary)
    new_upstream_html = ''
    new_internal_html = ''
    with open('{0}/{1}'.format(new_data_path, machine_info), 'r+') as mi:
        machine_info_content = mi.readlines()
        all_line_html = ''
        for one_line in machine_info_content:
            one_content = one_line.split(':')[1].strip('\n')
            one_line_html = '<td class="font_center" align="center">{0}</td>'.format(one_content)
            all_line_html += one_line_html
    machine_info = '<tr>' \
                   '{0}' \
                   '</tr>'.format(all_line_html)

    machine_info_html = """
                       <table  width="100%" border="0" cellspacing="0" cellpadding="0" align="center">
                           <tr>
                               <td class="biaoti2" >Server info: </td>
                           </tr>
                       </table>
                       <table width="100%" border="1" cellspacing="0" cellpadding="0" bgcolor="#cccccc" class="tabtop13" align="center">
                         <tr>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">OS</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">OS version</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">Kernel version</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">CPU model</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">CPU freq</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">Total of physical CPU</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">Number of CPU cores</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">Number of logical CPUs</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">Present Mode Of CPU</th>
                               <th class="btbg  font_center titfont"  align="center" style="color: white;font-weight: bold">Total Memory</th>
                           </tr>
                            {0}
                       </table>
                       """.format(machine_info)

    if new_info_list[3] != '':
        new_internal_html = """
        <tr>
            <td class="font_center" rowspan="2" align="center">Internal</td>
            <td class="font_center" align="center">Cur</td>
            <td class="font_center" align="center" style="line-height:120%" >{0}</td>
            <td class="font_center" align="center">{1}</td>
            <td class="font_center" align="center">{2}</td>
        </tr>
        """.format(new_info_list[5], new_info_list[4], new_info_list[3])

    if new_info_list[0] != '':
        new_upstream_html = """
        <tr>
            <td class="font_center" rowspan="2" align="center">Upstream</td>
            <td class=" font_center" align="center">Cur</td>
            <td class=" font_center" align="center">{0}</td>
            <td class=" font_center" align="center">{1}</td>
            <td class=" font_center" align="center">{2}</td>
        </tr>
        """.format(new_info_list[2], new_info_list[1], new_info_list[0])

    if reference_info_list[3] != '':
        reference_internal_html = """
        <tr>
            <td class=" font_center" align="center">Last</td>
            <td class=" font_center" align="center" style="line-height:120%" >{0}</td>
            <td class=" font_center" align="center">{1}</td>
            <td class=" font_center" align="center">{2}</td>
        </tr>
        """.format(reference_info_list[5], reference_info_list[4], reference_info_list[3])
    else:
        reference_internal_html = """
                <tr>
                    <td class=" font_center" align="center">Last</td>
                    <td class=" font_center" align="center" style="line-height:120%" ></td>
                    <td class=" font_center" align="center"></td>
                    <td class=" font_center" align="center"></td>
                </tr>
                 """

    if reference_info_list[0] != '':
        reference_upstream_html = """
        <tr>
            <td class=" font_center" align="center">Last</td>
            <td class=" font_center" align="center">{0}</td>
            <td class=" font_center" align="center">{1}</td>
            <td class=" font_center" align="center">{2}</td>
        </tr>
        """.format(reference_info_list[2], reference_info_list[1], reference_info_list[0])
    else:
        reference_upstream_html = """
               <tr>
                   <td class=" font_center" align="center">Last</td>
                   <td class=" font_center" align="center"></td>
                   <td class=" font_center" align="center"></td>
                   <td class=" font_center" align="center"></td>
               </tr>
               """

    internal_html = ''
    upstream_html = ''
    if new_info_list[3] != '':
        internal_html = """
                <table width="80%" border="1" cellspacing="0" cellpadding="0"  bgcolor="#cccccc" class="tabtop13">
                    <tr>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 10%">tensorflow source</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 5%">change</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 40%">repo</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 15%">branch</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 30%">commit id</th>
                    </tr>
                    {0}
                    {1}
                </table>
        """.format(new_internal_html, reference_internal_html)
    if new_info_list[0] != '':
        upstream_html = """
                <table width="80%" border="1" cellspacing="0" cellpadding="0"  bgcolor="#cccccc" class="tabtop13">
                    <tr>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 10%">tensorflow source</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 5%">change</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 40%">repo</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 15%">branch</th>
                        <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 30%">commit id</th>
                    </tr>
                    {0}
                    {1}
                </table>
        """.format(new_upstream_html, reference_upstream_html)

    summary_part = """ """
    blank_table_part = blank_table if internal_html != '' and upstream_html != '' else """ """
    blank_table_part_2 = blank_table
    build_info = """
            <tr>
                <td class=" font_center" align="center">{0}</td>
                <td class=" font_center" align="center" style="line-height:120%" >{1}</td>
            </tr>
            """.format(gcc_version, build_command)

    build_command_html = """
             <table width="80%" border="1" cellspacing="0" cellpadding="0"  bgcolor="#cccccc" class="tabtop13">
                        <tr>
                            <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 10%">gcc version</th>
                            <th class="btbg  font_center titfont " align="center" style="color: white;font-weight: bold;width: 90%">build commmand</th>
                        </tr>
                        {0}
                    </table>    
                    """.format(build_info)

    if internal_html != '' or upstream_html != '':
        summary_part = """
                   <table  width="100%" border="0" cellspacing="0" cellpadding="0" align="center">
                       <tr>
                           <td class="biaoti2" style="padding-top: 10px">Summary: </td>
                       </tr>
                   </table>
                   {0}
                   {2}
                   {1}
                   {4}
                   {3}
                   """.format(internal_html, upstream_html, blank_table_part, build_command_html, blank_table_part_2)

    summary_list = [summary_part, machine_info_html]
    return summary_list


def getMain(upstream_log, internal_log, new_data, reference_data, best_data, tf_info, build_url, server_type, build_command, gcc_version, machine_info):
    test_upstream = subprocess.Popen('cat {0}/{1} | grep "upstream"'.format(new_data, tf_info), shell=True,
                                     stdout=subprocess.PIPE)
    test_upstream_list = test_upstream.stdout.readlines()
    upstream_test = "True" if len(test_upstream_list) == 3 else "False"

    test_internal = subprocess.Popen('cat {0}/{1} | grep "internal"'.format(new_data, tf_info), shell=True,
                                     stdout=subprocess.PIPE)
    test_internal_list = test_internal.stdout.readlines()
    internal_test = "True" if len(test_internal_list) == 3 else "False"
    upstream_info_list = []
    internal_info_list = []
    if upstream_test == "True":
        upstream_log_list = [new_data + "/" + upstream_log, reference_data + "/" + upstream_log,
                             best_data + "/" + upstream_log]
        upstream_info_list = collect_results(results_list=upstream_log_list, tensorflow_source='Upstream')
    else:
        print("new test not have upstream data")

    if internal_test == "True":
        internal_log_list = [new_data + "/" + internal_log, reference_data + "/" + internal_log,
                             best_data + "/" + internal_log]
        internal_info_list = collect_results(results_list=internal_log_list, tensorflow_source='Internal')
    else:
        print("new test not have internal data")
    if len(upstream_info_list) != 0 or len(internal_info_list) != 0:
        blank_table = """
                           <table width="100" class="blank_table">
                           <tr>
                               <td>&nbsp;</td>
                           </tr>
                           <tr>
                               <td>&nbsp;</td>
                           </tr>
                       </table>
                        """
        all_tf_list = [upstream_info_list, internal_info_list]

        benchmark_part = get_benchmark_part(all_tf_list=all_tf_list, build_url=build_url, server_type=server_type)
        summary_part = get_summary_part(summary=tf_info, new_data_path=new_data, old_data_path=reference_data,
                                blank_table=blank_table, build_command=build_command,
                                gcc_version=gcc_version, machine_info=machine_info)
        write_html(new_data=new_data, benchmark_html=benchmark_part, summary_html=summary_part)
    else:
        print('all data not have')


def get_benchmark_part(all_tf_list, build_url, server_type):
    all_info_string = ''
    for one_source_list in all_tf_list:
        if len(one_source_list) != 0:
            one_file_row = ''
            tensorflow_source = one_source_list[-1]

            one_model_list = one_source_list[:-1]
            for one_list in one_model_list:
                model_name = one_list[0]
                test_mode = one_list[1]
                cur_bs_list = one_list[2][0]
                last_bs_list = one_list[2][1]
                best_bs_list = one_list[2][2]

                [compare_latency_fp32_cur_last_td, compare_throughput_fp32_cur_last_td, compare_accuracy_fp32_cur_last_td,
                 compare_latency_int8_cur_last_td, compare_throughput_int8_cur_last_td, compare_accuracy_int8_cur_last_td,
                 compare_latency_bf16_cur_last_td, compare_throughput_bf16_cur_last_td, compare_accuracy_bf16_cur_last_td,
                 compare_latency_fp32_cur_best_td, compare_throughput_fp32_cur_best_td, compare_accuracy_fp32_cur_best_td,
                 compare_latency_int8_cur_best_td, compare_throughput_int8_cur_best_td, compare_accuracy_int8_cur_best_td,
                 compare_latency_bf16_cur_best_td, compare_throughput_bf16_cur_best_td, compare_accuracy_bf16_cur_best_td] \
                    = ["""<td class=" font_center" align="center"></td>"""] * 18

                fp32_cur_latency = '%.2f' % float(one_list[3]) if isFloat(one_list[3]) else one_list[3]
                if one_list[3] != 'FAILED' and one_list[3] != '':
                    fp32_cur_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][0], fp32_cur_latency)
                elif one_list[3] == 'FAILED':
                    fp32_cur_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][0], fp32_cur_latency)
                else:
                    fp32_cur_latency_url = ''

                fp32_cur_throughput = '%.2f' % float(one_list[4]) if isFloat(one_list[4]) else one_list[4]
                if one_list[4] != 'FAILED' and one_list[4] != '':
                    fp32_cur_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][1], fp32_cur_throughput)
                elif one_list[4] == 'FAILED':
                    fp32_cur_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][1], fp32_cur_throughput)
                else:
                    fp32_cur_throughput_url = ''

                fp32_cur_accuracy = float(one_list[5]) if isFloat(one_list[5]) else one_list[5]
                if one_list[5] != 'FAILED' and one_list[5] != '':
                    fp32_cur_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][2], fp32_cur_accuracy)
                elif one_list[5] == 'FAILED':
                    fp32_cur_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][2], fp32_cur_accuracy)
                else:
                    fp32_cur_accuracy_url = ''

                fp32_last_latency = '%.2f' % float(one_list[12]) if isFloat(one_list[12]) else one_list[12]
                if one_list[12] != 'FAILED' and one_list[12] != '':
                    fp32_last_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][3], fp32_last_latency)
                elif one_list[12] == 'FAILED':
                    fp32_last_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][3], fp32_last_latency)
                else:
                    fp32_last_latency_url = ''


                fp32_last_throughput = '%.2f' % float(one_list[13]) if isFloat(one_list[13]) else one_list[13]
                if one_list[13] != 'FAILED' and one_list[13] != '':
                    fp32_last_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][4], fp32_last_throughput)
                elif one_list[13] == 'FAILED':
                    fp32_last_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][4], fp32_last_throughput)
                else:
                    fp32_last_throughput_url = ''

                fp32_last_accuracy = float(one_list[14]) if isFloat(one_list[14]) else one_list[14]
                if one_list[14] != 'FAILED' and one_list[14] != '':
                    fp32_last_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][5], fp32_last_accuracy)
                elif one_list[14] == 'FAILED':
                    fp32_last_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][5], fp32_last_accuracy)
                else:
                    fp32_last_accuracy_url = ''

                fp32_best_latency = '%.2f' % float(one_list[21]) if isFloat(one_list[21]) else one_list[21]
                if one_list[21] != 'FAILED' and one_list[21] != '':
                    fp32_best_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][6], fp32_best_latency)
                elif one_list[21] == 'FAILED':
                    fp32_best_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][6], fp32_best_latency)
                else:
                    fp32_best_latency_url = ''

                fp32_best_throughput = '%.2f' % float(one_list[22]) if isFloat(one_list[22]) else one_list[22]
                if one_list[22] != 'FAILED' and one_list[22] != '':
                    fp32_best_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][7], fp32_best_throughput)
                elif one_list[22] == 'FAILED':
                    fp32_best_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][7], fp32_best_throughput)
                else:
                    fp32_best_throughput_url = ''

                fp32_best_accuracy = float(one_list[23]) if isFloat(one_list[23]) else one_list[23]
                if one_list[23] != 'FAILED' and one_list[23] != '':
                    fp32_best_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][8], fp32_best_accuracy)
                elif one_list[23] == 'FAILED':
                    fp32_best_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][8], fp32_best_accuracy)
                else:
                    fp32_best_accuracy_url = ''

                int8_cur_latency = '%.2f' % float(one_list[6]) if isFloat(one_list[6]) else one_list[6]
                if one_list[6] != 'FAILED' and one_list[6] != '':
                    int8_cur_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][9], int8_cur_latency)
                elif one_list[6] == 'FAILED':
                    int8_cur_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][9], int8_cur_latency)
                else:
                    int8_cur_latency_url = ''

                int8_cur_throughput = '%.2f' % float(one_list[7]) if isFloat(one_list[7]) else one_list[7]
                if one_list[7] != 'FAILED' and one_list[7] != '':
                    int8_cur_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][10], int8_cur_throughput)
                elif one_list[7] == 'FAILED':
                    int8_cur_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][10], int8_cur_throughput)
                else:
                    int8_cur_throughput_url = ''

                int8_cur_accuracy = float(one_list[8]) if isFloat(one_list[8]) else one_list[8]
                if one_list[8] != 'FAILED' and one_list[8] != '':
                    int8_cur_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][11], int8_cur_accuracy)
                elif one_list[8] == 'FAILED':
                    int8_cur_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][11], int8_cur_accuracy)
                else:
                    int8_cur_accuracy_url = ''

                int8_last_latency = '%.2f' % float(one_list[15]) if isFloat(one_list[15]) else one_list[15]
                if one_list[15] != 'FAILED' and one_list[15] != '':
                    int8_last_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][12], int8_last_latency)
                elif one_list[15] == 'FAILED':
                    int8_last_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][12], int8_last_latency)
                else:
                    int8_last_latency_url = ''

                int8_last_throughput = '%.2f' % float(one_list[16]) if isFloat(one_list[16]) else one_list[16]
                if one_list[16] != 'FAILED' and one_list[16] != '':
                    int8_last_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][13], int8_last_throughput)
                elif one_list[16] == 'FAILED':
                    int8_last_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][13], int8_last_throughput)
                else:
                    int8_last_throughput_url = ''

                int8_last_accuracy = float(one_list[17]) if isFloat(one_list[17]) else one_list[17]
                if one_list[17] != 'FAILED' and one_list[17] != '':
                    int8_last_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][14], int8_last_accuracy)
                elif one_list[17] == 'FAILED':
                    int8_last_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][14], int8_last_accuracy)
                else:
                    int8_last_accuracy_url = ''

                int8_best_latency = '%.2f' % float(one_list[24]) if isFloat(one_list[24]) else one_list[24]
                if one_list[24] != 'FAILED' and one_list[24] != '':
                    int8_best_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][15], int8_best_latency)
                elif one_list[24] == 'FAILED':
                    int8_best_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][15], int8_best_latency)
                else:
                    int8_best_latency_url = ''

                int8_best_throughput = '%.2f' % float(one_list[25]) if isFloat(one_list[25]) else one_list[25]
                if one_list[25] != 'FAILED' and one_list[25] != '':
                    int8_best_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][16], int8_best_throughput)
                elif one_list[25] == 'FAILED':
                    int8_best_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][16], int8_best_throughput)
                else:
                    int8_best_throughput_url = ''

                int8_best_accuracy = float(one_list[26]) if isFloat(one_list[26]) else one_list[26]
                if one_list[26] != 'FAILED' and one_list[26] != '':
                    int8_best_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][17], int8_best_accuracy)
                elif one_list[26] == 'FAILED':
                    int8_best_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][17], int8_best_accuracy)
                else:
                    int8_best_accuracy_url = ''


                bf16_cur_latency = '%.2f' % float(one_list[9]) if isFloat(one_list[9]) else one_list[9]
                if one_list[9] != 'FAILED' and one_list[9] != '':
                    bf16_cur_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][18], bf16_cur_latency)
                elif one_list[9] == 'FAILED':
                    bf16_cur_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][18], bf16_cur_latency)
                else:
                    bf16_cur_latency_url = ''

                bf16_cur_throughput = '%.2f' % float(one_list[10]) if isFloat(one_list[10]) else one_list[10]
                if one_list[10] != 'FAILED' and one_list[10] != '':
                    bf16_cur_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][19], bf16_cur_throughput)
                elif one_list[10] == 'FAILED':
                    bf16_cur_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][19], bf16_cur_throughput)
                else:
                    bf16_cur_throughput_url = ''

                bf16_cur_accuracy = float(one_list[11]) if isFloat(one_list[11]) else one_list[11]
                if one_list[11] != 'FAILED' and one_list[11] != '':
                    bf16_cur_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][20], bf16_cur_accuracy)
                elif one_list[11] == 'FAILED':
                    bf16_cur_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/benchmark/all_test_log/' + one_list[-1][20], bf16_cur_accuracy)
                else:
                    bf16_cur_accuracy_url = ''

                bf16_last_latency = '%.2f' % float(one_list[18]) if isFloat(one_list[18]) else one_list[18]
                if one_list[18] != 'FAILED' and one_list[18] != '':
                    bf16_last_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][21], bf16_last_latency)
                elif one_list[18] == 'FAILED':
                    bf16_last_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][21], bf16_last_latency)
                else:
                    bf16_last_latency_url = ''

                bf16_last_throughput = '%.2f' % float(one_list[19]) if isFloat(one_list[19]) else one_list[19]
                if one_list[19] != 'FAILED' and one_list[19] != '':
                    bf16_last_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][22], bf16_last_throughput)
                elif one_list[19] == 'FAILED':
                    bf16_last_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][22], bf16_last_throughput)
                else:
                    bf16_last_throughput_url = ''

                bf16_last_accuracy = float(one_list[20]) if isFloat(one_list[20]) else one_list[20]
                if one_list[20] != 'FAILED' and one_list[20] != '':
                    bf16_last_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][23], bf16_last_accuracy)
                elif one_list[20] == 'FAILED':
                    bf16_last_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/reference_log/benchmark/all_test_log/' + one_list[-1][23], bf16_last_accuracy)
                else:
                    bf16_last_accuracy_url = ''

                bf16_best_latency = '%.2f' % float(one_list[27]) if isFloat(one_list[27]) else one_list[27]
                if one_list[27] != 'FAILED' and one_list[27] != '':
                    bf16_best_latency_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][24], bf16_best_latency)
                elif one_list[27] == 'FAILED':
                    bf16_best_latency_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][24], bf16_best_latency)
                else:
                    bf16_best_latency_url = ''

                bf16_best_throughput = '%.2f' % float(one_list[28]) if isFloat(one_list[28]) else one_list[28]
                if one_list[28] != 'FAILED' and one_list[28] != '':
                    bf16_best_throughput_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][25], bf16_best_throughput)
                elif one_list[28] == 'FAILED':
                    bf16_best_throughput_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][25], bf16_best_throughput)
                else:
                    bf16_best_throughput_url = ''

                bf16_best_accuracy = float(one_list[29]) if isFloat(one_list[29]) else one_list[29]
                if one_list[29] != 'FAILED' and one_list[29] != '':
                    bf16_best_accuracy_url = """<a href="{0}">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][26], bf16_best_accuracy)
                elif one_list[29] == 'FAILED':
                    bf16_best_accuracy_url = """<a href="{0}" style="color: red;font-weight: bold;">{1}</a>""" \
                        .format(build_url + 'artifact/target_log/benchmark/all_test_log/' + one_list[-1][26], bf16_best_accuracy)
                else:
                    bf16_best_accuracy_url = ''

                # compare td
                # fp32 latency cur&last
                if isFloat(fp32_cur_latency) and isFloat(fp32_last_latency):
                    compare_latency_fp32_cur_last = (float(fp32_cur_latency) - float(fp32_last_latency)) / float(
                        fp32_last_latency)
                    if compare_latency_fp32_cur_last > 0.05:
                        compare_latency_fp32_cur_last_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_fp32_cur_last * 100) + '%')
                    elif compare_latency_fp32_cur_last < -0.05:
                        compare_latency_fp32_cur_last_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_fp32_cur_last * 100) + '%')

                    else:
                        compare_latency_fp32_cur_last_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_fp32_cur_last * 100) + '%')

                # fp32 latency cur&best
                if isFloat(fp32_cur_latency) and isFloat(fp32_best_latency):
                    compare_latency_fp32_cur_best = (float(fp32_cur_latency) - float(fp32_best_latency)) / float(
                        fp32_best_latency)
                    if compare_latency_fp32_cur_best > 0.05:
                        compare_latency_fp32_cur_best_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_fp32_cur_best * 100) + '%')
                    elif compare_latency_fp32_cur_best < -0.05:
                        compare_latency_fp32_cur_best_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_fp32_cur_best * 100) + '%')

                    else:
                        compare_latency_fp32_cur_best_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_fp32_cur_best * 100) + '%')

                # fp32 throughput cur&last
                if isFloat(fp32_cur_throughput) and isFloat(fp32_last_throughput):
                    compare_throughput_fp32_cur_last = (float(fp32_cur_throughput) - float(fp32_last_throughput)) / float(
                        fp32_last_throughput)
                    if compare_throughput_fp32_cur_last > 0.05:
                        compare_throughput_fp32_cur_last_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_fp32_cur_last * 100) + '%')
                    elif compare_throughput_fp32_cur_last < -0.05:
                        compare_throughput_fp32_cur_last_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_fp32_cur_last * 100) + '%')
                    else:
                        compare_throughput_fp32_cur_last_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_fp32_cur_last * 100) + '%')
                # fp32 throughput cur&best
                if isFloat(fp32_cur_throughput) and isFloat(fp32_best_throughput):
                    compare_throughput_fp32_cur_best = (float(fp32_cur_throughput) - float(fp32_best_throughput)) / float(
                        fp32_best_throughput)
                    if compare_throughput_fp32_cur_best > 0.05:
                        compare_throughput_fp32_cur_best_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_fp32_cur_best * 100) + '%')
                    elif compare_throughput_fp32_cur_best < -0.05:
                        compare_throughput_fp32_cur_best_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_fp32_cur_best * 100) + '%')
                    else:
                        compare_throughput_fp32_cur_best_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_fp32_cur_best * 100) + '%')

                # fp32 accuracy cur&last
                if isFloat(fp32_cur_accuracy) and isFloat(fp32_last_accuracy):
                    compare_accuracy_fp32_cur_last = (float(fp32_cur_accuracy) - float(fp32_last_accuracy))
                    if compare_accuracy_fp32_cur_last < 0.00:
                        compare_accuracy_fp32_cur_last_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/cross.png align="center" ></img></td>"""
                    else:
                        compare_accuracy_fp32_cur_last_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/check.png align="center"></img></td>"""

                # fp32 accuracy cur&best
                if isFloat(fp32_cur_accuracy) and isFloat(fp32_best_accuracy):
                    compare_accuracy_fp32_cur_best = (float(fp32_cur_accuracy) - float(fp32_best_accuracy))
                    if compare_accuracy_fp32_cur_best < 0.00:
                        compare_accuracy_fp32_cur_best_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/cross.png align="center" ></img></td>"""
                    else:
                        compare_accuracy_fp32_cur_best_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/check.png align="center"></img></td>"""

                # int8 latency cur&last
                if isFloat(int8_cur_latency) and isFloat(int8_last_latency):
                    compare_latency_int8_cur_last = (float(int8_cur_latency) - float(int8_last_latency)) / float(
                        int8_last_latency)
                    if compare_latency_int8_cur_last > 0.05:
                        compare_latency_int8_cur_last_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_int8_cur_last * 100) + '%')
                    elif compare_latency_int8_cur_last < -0.05:
                        compare_latency_int8_cur_last_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_int8_cur_last * 100) + '%')
                    else:
                        compare_latency_int8_cur_last_td = """<td class=" font_center" align="center" >{0}</td>""" \
                            .format('%.2f' % (compare_latency_int8_cur_last * 100) + '%')
                # int8 latency cur&best
                if isFloat(int8_cur_latency) and isFloat(int8_best_latency):
                    compare_latency_int8_cur_best = (float(int8_cur_latency) - float(int8_best_latency)) / float(
                        int8_best_latency)

                    if compare_latency_int8_cur_best > 0.05:
                        compare_latency_int8_cur_best_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_int8_cur_best * 100) + '%')
                    elif compare_latency_int8_cur_best < -0.05:
                        compare_latency_int8_cur_best_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_int8_cur_best * 100) + '%')
                    else:
                        compare_latency_int8_cur_best_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_int8_cur_best * 100) + '%')
                # int8 throughput cur&last
                if isFloat(int8_cur_throughput) and isFloat(int8_last_throughput):
                    compare_throughput_int8_cur_last = (float(int8_cur_throughput) - float(int8_last_throughput)) / float(
                        int8_last_throughput)
                    if compare_throughput_int8_cur_last > 0.05:
                        compare_throughput_int8_cur_last_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_int8_cur_last * 100) + '%')
                    elif compare_throughput_int8_cur_last < -0.05:
                        compare_throughput_int8_cur_last_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_int8_cur_last * 100) + '%')
                    else:
                        compare_throughput_int8_cur_last_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_int8_cur_last * 100) + '%')
                # int8 throughput cur&best
                if isFloat(int8_cur_throughput) and isFloat(int8_best_throughput):
                    compare_throughput_int8_cur_best = (float(int8_cur_throughput) - float(int8_best_throughput)) / float(
                        int8_best_throughput)
                    if compare_throughput_int8_cur_best > 0.05:
                        compare_throughput_int8_cur_best_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_int8_cur_best * 100) + '%')
                    elif compare_throughput_int8_cur_best < -0.05:
                        compare_throughput_int8_cur_best_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_int8_cur_best * 100) + '%')
                    else:
                        compare_throughput_int8_cur_best_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_int8_cur_best * 100) + '%')

                # int8 accuracy cur&last
                if isFloat(int8_cur_accuracy) and isFloat(int8_last_accuracy):
                    compare_accuracy_int8_cur_last = (float(int8_cur_accuracy) - float(int8_last_accuracy))
                    if compare_accuracy_int8_cur_last < 0.00:
                        compare_accuracy_int8_cur_last_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/cross.png align="center"></img></td>"""
                    else:
                        compare_accuracy_int8_cur_last_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/check.png align="center"></img></td>"""

                # int8 accuracy cur&best
                if isFloat(int8_cur_accuracy) and isFloat(int8_best_accuracy):
                    compare_accuracy_int8_cur_best = (float(int8_cur_accuracy) - float(int8_best_accuracy))
                    if compare_accuracy_int8_cur_best < 0.00:
                        compare_accuracy_int8_cur_best_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/cross.png align="center"></img></td>"""
                    else:
                        compare_accuracy_int8_cur_best_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/check.png align="center"></img></td>"""

                # bf16 latency cur&last
                if isFloat(bf16_cur_latency) and isFloat(bf16_last_latency):
                    compare_latency_bf16_cur_last = (float(bf16_cur_latency) - float(bf16_last_latency)) / float(
                        bf16_last_latency)
                    if compare_latency_bf16_cur_last > 0.05:
                        compare_latency_bf16_cur_last_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_bf16_cur_last * 100) + '%')
                    elif compare_latency_bf16_cur_last < -0.05:
                        compare_latency_bf16_cur_last_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_bf16_cur_last * 100) + '%')
                    else:
                        compare_latency_bf16_cur_last_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_bf16_cur_last * 100) + '%')

                # bf16 latency cur&best
                if isFloat(bf16_cur_latency) and isFloat(bf16_best_latency):
                    compare_latency_bf16_cur_best = (float(bf16_cur_latency) - float(bf16_best_latency)) / float(
                        bf16_best_latency)
                    if compare_latency_bf16_cur_best > 0.05:
                        compare_latency_bf16_cur_best_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_bf16_cur_best * 100) + '%')
                    elif compare_latency_bf16_cur_best < -0.05:
                        compare_latency_bf16_cur_best_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_bf16_cur_best * 100) + '%')
                    else:
                        compare_latency_bf16_cur_best_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_latency_bf16_cur_best * 100) + '%')
                # bf16 throughput cur&last
                if isFloat(bf16_cur_throughput) and isFloat(bf16_last_throughput):
                    compare_throughput_bf16_cur_last = (float(bf16_cur_throughput) - float(bf16_last_throughput)) / float(
                        bf16_last_throughput)
                    if compare_throughput_bf16_cur_last > 0.05:
                        compare_throughput_bf16_cur_last_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_bf16_cur_last * 100) + '%')
                    elif compare_throughput_bf16_cur_last < -0.05:
                        compare_throughput_bf16_cur_last_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_bf16_cur_last * 100) + '%')
                    else:
                        compare_throughput_bf16_cur_last_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_bf16_cur_last * 100) + '%')
                # bf16 throughput cur&best
                if isFloat(bf16_cur_throughput) and isFloat(bf16_best_throughput):
                    compare_throughput_bf16_cur_best = (float(bf16_cur_throughput) - float(bf16_best_throughput)) / float(
                        bf16_best_throughput)
                    if compare_throughput_bf16_cur_best > 0.05:
                        compare_throughput_bf16_cur_best_td = """<td class=" font_center" style="color: green;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_bf16_cur_best * 100) + '%')
                    elif compare_throughput_bf16_cur_best < -0.05:
                        compare_throughput_bf16_cur_best_td = """<td class=" font_center" style="color: red;font-weight: bold;" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_bf16_cur_best * 100) + '%')
                    else:
                        compare_throughput_bf16_cur_best_td = """<td class=" font_center" align="center">{0}</td>""" \
                            .format('%.2f' % (compare_throughput_bf16_cur_best * 100) + '%')

                # bf16 accuracy cur&last
                if isFloat(bf16_cur_accuracy) and isFloat(bf16_last_accuracy):
                    compare_accuracy_bf16_cur_last = (float(bf16_cur_accuracy) - float(bf16_last_accuracy))
                    if compare_accuracy_bf16_cur_last < 0.00:
                        compare_accuracy_bf16_cur_last_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/cross.png align="center"></img></td>"""
                    else:
                        compare_accuracy_bf16_cur_last_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/check.png align="center"></img></td>"""

                # bf16 accuracy cur&best
                if isFloat(bf16_cur_accuracy) and isFloat(bf16_best_accuracy):
                    compare_accuracy_bf16_cur_best = (float(bf16_cur_accuracy) - float(bf16_best_accuracy))
                    if compare_accuracy_bf16_cur_best < 0.00:
                        compare_accuracy_bf16_cur_best_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/cross.png align="center"></img></td>"""
                    else:
                        compare_accuracy_bf16_cur_best_td = """<td class=" font_center" align="center"><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/check.png align="center"></img></td>"""

                cur_bs_fp32_1 = cur_bs_list[0] if fp32_cur_latency_url != '' else ''
                cur_bs_fp32_default = cur_bs_list[1] if fp32_cur_throughput_url != '' else ''
                cur_bs_fp32_accuracy = cur_bs_list[2] if fp32_cur_accuracy_url != '' else ''
                cur_bs_int8_1 = cur_bs_list[3] if int8_cur_latency_url != '' else ''
                cur_bs_int8_default = cur_bs_list[4] if int8_cur_throughput_url != '' else ''
                cur_bs_int8_accuracy = cur_bs_list[5] if int8_cur_accuracy_url != '' else ''
                cur_bs_bf16_1 = cur_bs_list[6] if bf16_cur_latency_url != '' else ''
                cur_bs_bf16_default = cur_bs_list[7] if bf16_cur_throughput_url != '' else ''
                cur_bs_bf16_accuracy = cur_bs_list[8] if bf16_cur_accuracy_url != '' else ''

                cur_list = [cur_bs_fp32_1, fp32_cur_latency_url, cur_bs_fp32_default, fp32_cur_throughput_url, cur_bs_fp32_accuracy,
                            fp32_cur_accuracy_url,
                            cur_bs_int8_1, int8_cur_latency_url, cur_bs_int8_default, int8_cur_throughput_url, cur_bs_int8_accuracy,
                            int8_cur_accuracy_url,
                            cur_bs_bf16_1, bf16_cur_latency_url, cur_bs_bf16_default, bf16_cur_throughput_url, cur_bs_bf16_accuracy,
                            bf16_cur_accuracy_url]
                cur_row = ''
                for cur_one in cur_list:
                    cur_one_row = '<td class=" font_center" align="center">{0}</td>'.format(cur_one)
                    cur_row += cur_one_row

                last_bs_fp32_1 = last_bs_list[0] if fp32_last_latency_url != '' else ''
                last_bs_fp32_default = last_bs_list[1] if fp32_last_throughput_url != '' else ''
                last_bs_fp32_accuracy = last_bs_list[2] if fp32_last_accuracy_url != '' else ''
                last_bs_int8_1 = last_bs_list[3] if int8_last_latency_url != '' else ''
                last_bs_int8_default = last_bs_list[4] if int8_last_throughput_url != '' else ''
                last_bs_int8_accuracy = last_bs_list[5] if int8_last_accuracy_url != '' else ''
                last_bs_bf16_1 = last_bs_list[6] if bf16_last_latency_url != '' else ''
                last_bs_bf16_default = last_bs_list[7] if bf16_last_throughput_url != '' else ''
                last_bs_bf16_accuracy = last_bs_list[8] if bf16_last_accuracy_url != '' else ''

                last_list = [last_bs_fp32_1, fp32_last_latency_url, last_bs_fp32_default, fp32_last_throughput_url, last_bs_fp32_accuracy,
                             fp32_last_accuracy_url,
                             last_bs_int8_1, int8_last_latency_url, last_bs_int8_default, int8_last_throughput_url, last_bs_int8_accuracy,
                             int8_last_accuracy_url,
                             last_bs_bf16_1, bf16_last_latency_url, last_bs_bf16_default, bf16_last_throughput_url, last_bs_bf16_accuracy,
                             bf16_last_accuracy_url]
                last_row = ''
                for last_one in last_list:
                    last_one_row = '<td class=" font_center" align="center">{0}</td>'.format(last_one)
                    last_row += last_one_row

                best_bs_fp32_1 = best_bs_list[0] if fp32_best_latency_url != '' else ''
                best_bs_fp32_default = best_bs_list[1] if fp32_best_throughput_url != '' else ''
                best_bs_fp32_accuracy = best_bs_list[2] if fp32_best_accuracy_url != '' else ''
                best_bs_int8_1 = best_bs_list[3] if int8_best_latency_url != '' else ''
                best_bs_int8_default = best_bs_list[4] if int8_best_throughput_url != '' else ''
                best_bs_int8_accuracy = best_bs_list[5] if int8_best_accuracy_url != '' else ''
                best_bs_bf16_1 = best_bs_list[6] if bf16_best_latency_url != '' else ''
                best_bs_bf16_default = best_bs_list[7] if bf16_best_throughput_url != '' else ''
                best_bs_bf16_accuracy = best_bs_list[8] if bf16_best_accuracy_url != '' else ''


                best_list = [best_bs_fp32_1, fp32_best_latency_url, best_bs_fp32_default, fp32_best_throughput_url, best_bs_fp32_accuracy,
                             fp32_best_accuracy_url,
                             best_bs_int8_1, int8_best_latency_url, best_bs_int8_default, int8_best_throughput_url, best_bs_int8_accuracy,
                             int8_best_accuracy_url,
                             best_bs_bf16_1, bf16_best_latency_url, best_bs_bf16_default, bf16_best_throughput_url, best_bs_bf16_accuracy,
                             bf16_best_accuracy_url]
                best_row = ''
                for best_one in best_list:
                    best_one_row = '<td class=" font_center" align="center">{0}</td>'.format(best_one)
                    best_row += best_one_row

                compare_latency_fp32_cur_last_td_bs = cur_bs_list[0] if cur_bs_fp32_1 != '' and last_bs_fp32_1 != '' else ''
                compare_throughput_fp32_cur_last_td_bs = cur_bs_list[1] if cur_bs_fp32_default != '' and last_bs_fp32_default != '' else ''
                compare_accuracy_fp32_cur_last_td_bs = cur_bs_list[2] if cur_bs_fp32_accuracy != '' and last_bs_fp32_accuracy != '' else ''

                compare_latency_int8_cur_last_td_bs = cur_bs_list[3] if cur_bs_int8_1 != '' and last_bs_int8_1 != '' else ''
                compare_throughput_int8_cur_last_td_bs = cur_bs_list[4] if cur_bs_int8_default != '' and last_bs_int8_default != '' else ''
                compare_accuracy_int8_cur_last_td_bs = cur_bs_list[5] if cur_bs_int8_accuracy != '' and last_bs_int8_accuracy != '' else ''

                compare_latency_bf16_cur_last_td_bs = cur_bs_list[6] if cur_bs_bf16_1 != '' and last_bs_bf16_1 != '' else ''
                compare_throughput_bf16_cur_last_td_bs = cur_bs_list[7] if cur_bs_bf16_default != '' and last_bs_bf16_default != '' else ''
                compare_accuracy_bf16_cur_last_td_bs = cur_bs_list[8] if cur_bs_bf16_accuracy != '' and last_bs_bf16_accuracy != '' else ''

                compare_cur_last = [compare_latency_fp32_cur_last_td_bs, compare_latency_fp32_cur_last_td,
                                    compare_throughput_fp32_cur_last_td_bs, compare_throughput_fp32_cur_last_td,
                                    compare_accuracy_fp32_cur_last_td_bs, compare_accuracy_fp32_cur_last_td,
                                    compare_latency_int8_cur_last_td_bs, compare_latency_int8_cur_last_td,
                                    compare_throughput_int8_cur_last_td_bs, compare_throughput_int8_cur_last_td,
                                    compare_accuracy_int8_cur_last_td_bs, compare_accuracy_int8_cur_last_td,
                                    compare_latency_bf16_cur_last_td_bs, compare_latency_bf16_cur_last_td,
                                    compare_throughput_bf16_cur_last_td_bs, compare_throughput_bf16_cur_last_td,
                                    compare_accuracy_bf16_cur_last_td_bs, compare_accuracy_bf16_cur_last_td]

                compare_cur_last_row = ''
                for compare_cur_last_one in compare_cur_last:
                    if compare_cur_last.index(compare_cur_last_one) % 2 == 0:
                        compare_cur_last_row += '<td class=" font_center" align="center">{0}</td>'.format(compare_cur_last_one)
                    else:
                        compare_cur_last_row += compare_cur_last_one

                compare_latency_fp32_cur_best_td_bs = cur_bs_list[0] if cur_bs_fp32_1 != '' and best_bs_fp32_1 != '' else ''
                compare_throughput_fp32_cur_best_td_bs = cur_bs_list[1] if cur_bs_fp32_default != '' and best_bs_fp32_default != '' else ''
                compare_accuracy_fp32_cur_best_td_bs = cur_bs_list[2] if cur_bs_fp32_accuracy != '' and best_bs_fp32_accuracy != '' else ''

                compare_latency_int8_cur_best_td_bs = cur_bs_list[3] if cur_bs_int8_1 != '' and best_bs_int8_1 != '' else ''
                compare_throughput_int8_cur_best_td_bs = cur_bs_list[4] if cur_bs_int8_default != '' and best_bs_int8_default != '' else ''
                compare_accuracy_int8_cur_best_td_bs = cur_bs_list[5] if cur_bs_int8_accuracy != '' and best_bs_int8_accuracy != '' else ''

                compare_latency_bf16_cur_best_td_bs = cur_bs_list[6] if cur_bs_bf16_1 != '' and best_bs_bf16_1 != '' else ''
                compare_throughput_bf16_cur_best_td_bs = cur_bs_list[7] if cur_bs_bf16_default != '' and best_bs_bf16_default != '' else ''
                compare_accuracy_bf16_cur_best_td_bs = cur_bs_list[8] if cur_bs_bf16_accuracy != '' and best_bs_bf16_accuracy != '' else ''

                compare_cur_best = [compare_latency_fp32_cur_best_td_bs, compare_latency_fp32_cur_best_td,
                                    compare_throughput_fp32_cur_best_td_bs, compare_throughput_fp32_cur_best_td,
                                    compare_accuracy_fp32_cur_best_td_bs, compare_accuracy_fp32_cur_best_td,
                                    compare_latency_int8_cur_best_td_bs, compare_latency_int8_cur_best_td,
                                    compare_throughput_int8_cur_best_td_bs, compare_throughput_int8_cur_best_td,
                                    compare_accuracy_int8_cur_best_td_bs, compare_accuracy_int8_cur_best_td,
                                    compare_latency_bf16_cur_best_td_bs, compare_latency_bf16_cur_best_td,
                                    compare_throughput_bf16_cur_best_td_bs, compare_throughput_bf16_cur_best_td,
                                    compare_accuracy_bf16_cur_best_td_bs, compare_accuracy_bf16_cur_best_td]

                compare_cur_best_row = ''
                for compare_cur_best_one in compare_cur_best:
                    if compare_cur_best.index(compare_cur_best_one) % 2 == 0:
                        compare_cur_best_row += '<td class=" font_center" align="center">{0}</td>'.format(
                            compare_cur_best_one)
                    else:
                        compare_cur_best_row += compare_cur_best_one

                one_row = '<tr>' \
                          '<td class="{0} font_center" rowspan="5" align="center">{8}</td>' \
                          '<td class="{0} font_center" rowspan="5" align="center">{0}</td>' \
                          '<td class="{0} font_center" rowspan="5" align="center">{1}</td>' \
                          '<td class="{0} font_center" rowspan="5" align="center">{2}</td>' \
                          '<td class="{0} font_center" align="center">New</td>' \
                          '{3}' \
                          '</tr>' \
                          '<tr>' \
                          '<td class="{0} font_center" align="center">Last</td>' \
                          '{4}' \
                          '</tr>' \
                          '<tr>' \
                          '<td class="{0} font_center " align="center">Best</td>' \
                          '{5}' \
                          '</tr>' \
                          '<tr>' \
                          '<td class="{0} font_center" align="center">|(Cur-Last)/Last|<0.05</td>' \
                          '{6}' \
                          '</tr>' \
                          '<tr>' \
                          '<td class="{0} font_center" align="center">|(Cur-Best)/Best|<0.05</td>' \
                          '{7}' \
                          '</tr>'.format(tensorflow_source, model_name, test_mode,  cur_row, last_row,  best_row,
                                         compare_cur_last_row, compare_cur_best_row, server_type)

                one_file_row += one_row
            all_info_string += one_file_row
    benchmark_part = """
                    <table  width="100%" border="0" cellspacing="0" cellpadding="0" align="center">
                        <tr>
                            <td class="biaoti2" >Performance Test: </td>
                        </tr>
                        <tr>
                            <td class="biaoti3" ><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/check.png align="center" ></img>: accuracy constant or improved</td>
                        </tr>
                        <tr>
                            <td class="biaoti3" ><img src=http://mlpc.intel.com/static/doc/tensorflow/images/16x16/cross.png align="center" >: accuracy regression</img></td>
                        </tr>
                    </table>
                    <table width="100%" border="1" cellspacing="0" cellpadding="0" bgcolor="#cccccc" class="tabtop13" align="center">
                      <tr>
                            <th class="btbg  font_center titfont" rowspan="2" align="center" style="color: white;font-weight: bold">platform</th>
                            <th class="btbg  font_center titfont" rowspan="2" align="center" style="color: white;font-weight: bold">tensorflow source</th>
                            <th class="btbg  font_center titfont" rowspan="2" align="center" style="color: white;font-weight: bold">model name</th>
                            <th class="btbg  font_center titfont" rowspan="2" align="center" style="color: white;font-weight: bold">mode</th>
                            <th class="btbg  font_center titfont" rowspan="2" align="center" style="color: white;font-weight: bold">Change</th>
                            <th class="btbg  font_center titfont" colspan="6" align="center" style="color: white;font-weight: bold">FP32</th>
                            <th class="btbg  font_center titfont" colspan="6" align="center" style="color: white;font-weight: bold">INT8</th>
                            <th class="btbg  font_center titfont" colspan="6" align="center" style="color: white;font-weight: bold">BF16</th>
                        </tr>
                        <tr>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">realtime</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">throughput</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">accuracy</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">realtime</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">throughput</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">accuracy</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">realtime</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">throughput</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">BS</th>
                            <th class="btbg font_center titfont" align="center" style="color: white;font-weight: bold">accuracy</th>
                        </tr>
                      {0}
                    </table>
                    """.format(all_info_string)
    return benchmark_part


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="send report")
    parser.add_argument("--upstream_log", "-ul",
                        help="upstream_log name", required=True)
    parser.add_argument("--internal_log", "-il",
                        help="internal_log name", required=True)
    parser.add_argument("--tf_info", "-ti",
                        help="tf_info name", required=True)

    parser.add_argument("--new_data", "-nd",
                        help="new_data path", required=True)
    parser.add_argument("--reference_data", "-rd",
                        help="reference_data path", required=True)
    parser.add_argument("--best_data", "-bd",
                        help="best_data path", required=True)
    parser.add_argument("--build_url", "-bu",
                        help="build_url ", required=True)
    parser.add_argument("--server_type", "-st",
                        help="server_type ", required=True)
    parser.add_argument("--build_command", "-bc",
                        help="build_command ", required=True)
    parser.add_argument("--gcc_version", "-gv",
                        help="gcc_version ", required=True)
    parser.add_argument("--machine_info", "-mi",
                        help="machine_info ", required=True)


    args = parser.parse_args()
    upstream_log = args.upstream_log
    internal_log = args.internal_log
    tf_info = args.tf_info
    new_data = args.new_data
    reference_data = args.reference_data
    best_data = args.best_data
    build_url = args.build_url
    server_type = args.server_type
    build_command = args.build_command
    gcc_version = args.gcc_version
    machine_info = args.machine_info
    getMain(upstream_log=upstream_log, internal_log=internal_log, new_data=new_data,
                  reference_data=reference_data, best_data=best_data,
                  tf_info=tf_info, build_url=build_url, server_type=server_type,
                  build_command=build_command, gcc_version=gcc_version, machine_info=machine_info)

