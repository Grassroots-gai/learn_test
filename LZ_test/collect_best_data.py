import subprocess
import os
import argparse


def collect_best_data(tensorflow_source, new_data, best_data, work_path):
    add_best_data_list = []
    new_best_data_file = "{0}_summary_target_new.log".format(tensorflow_source)
    with open(best_data, 'r+') as tf, open(new_data, 'r+') as tf1:
        all_best_content = tf.readlines()
        all_new_content = tf1.readlines()
        for one_new_data in all_new_content[1:]:
            one_new_list = one_new_data.split(";")
            flag = 0
            if len(all_best_content[1:]) == 0:
                all_best_content.append(one_new_data)
            else:
                for one_best_data in all_best_content[1:]:
                    one_best_list = one_best_data.split(";")
                    one_best_data_index = all_best_content.index(one_best_data)
                    if one_new_list[:5] == one_best_list[:5]:
                        if one_new_list[6] != "FAILED":
                            if one_best_list[6] == 'FAILED':
                                all_best_content[one_best_data_index] = one_new_data
                            elif one_best_list[6] != 'FAILED':
                                if float(one_new_list[6]) > float(one_best_list[6]):
                                    all_best_content[one_best_data_index] = one_new_data
                                elif float(one_new_list[6]) <= float(one_best_list[6]):
                                    all_best_content[one_best_data_index] = one_best_data
                        elif one_new_list[6] == "FAILED":
                            all_best_content[one_best_data_index] = one_best_data
                        flag = 0
                        break
                    elif one_new_list[:5] != one_best_list[:5]:
                        flag = 1
                if flag == 1:
                    add_best_data_list.append(one_new_data)
    all_best_content.extend(add_best_data_list)
    new_best_data_file_name = work_path + '/' + new_best_data_file
    with open(new_best_data_file_name, 'w+') as tf2:
        tf2.writelines(all_best_content)

    subprocess.call('cd {0};rm {1};mv {2} {1}'.format(work_path, best_data, new_best_data_file_name), shell=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="run LZ models jenkins")
    parser.add_argument("--tensorflow_source", "-t",
                        help="select upstream or internal", required=True)
    parser.add_argument("--work_path", "-w", help="work path", required=True)
    parser.add_argument("--new_data", "-nd", help="new data file", required=True)
    parser.add_argument("--best_data", "-bd", help="best data file", required=True)
    args = parser.parse_args()
    tensorflow_source = args.tensorflow_source
    work_path = args.work_path
    new_data = args.new_data
    best_data = args.best_data
    collect_best_data(tensorflow_source=tensorflow_source, new_data=new_data, best_data=best_data, work_path=work_path)
