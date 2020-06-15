#!/bin/bash
# collect best reulst

for file in $(find ${WORKSPACE}/reference_log/benchmark/ -type f -name "summary_nightly.log")
do
    reference_file=${file}
    target_file=$(echo ${file} |sed -e 's/reference_log/target_log/')
    
    # init target file
    mkdir -p ${target_file%/*}
    if [ ! -f ${target_file} ];
    then
        echo "Target is not found, copy the reference as target."
        cp -r ${reference_file%/*} ${target_file%/*}
    fi 
    
    # save the best result
    while read line 
    do 
        model=$(echo ${line} |cut -f1 -d';')
        mode=$(echo ${line} |cut -f2 -d';')
        machine_type=$(echo ${line} |cut -f3 -d';')
        data_type=$(echo ${line} |cut -f4 -d';')
        performance=$(echo ${line} |cut -f5 -d';')
        batch_size=$(echo ${line} |cut -f6 -d';')
        values=$(echo ${line} |cut -f7 -d';')
        log_file=$(echo ${line} |cut -f8 -d';')
        job_name=$(echo ${line} |cut -f9 -d';')
        build_id=$(echo ${line} |cut -f10 -d';')
        
        if [ $(grep "${model};${mode};${machine_type};${data_type};${performance};" ${target_file} |wc -l) -ne 0 ];
        then 
            target_values=$(
                grep "${model};${mode};${machine_type};${data_type};${performance};" ${target_file} |tail -1 |cut -f7 -d';'
            )
            
            new_or_old=$(echo |awk -v performance=${performance} -v new="${values}" -v old="${target_values}" '
                BEGIN { new_or_old = "old"; }
                {
                    if(performance == "Latency") {
                        if(new ~/[1-9]/) {
                            if(old ~/[1-9]/) {
                                if(new < old) {
                                    new_or_old = "new";
                                }
                            }else {
                                new_or_old = "new";
                            }
                        }
                    }
                    if(performance == "Throughput") {
                        if(new ~/[1-9]/) {
                            if(old ~/[1-9]/) {
                                if(new > old) {
                                    new_or_old = "new";
                                }
                            }else {
                                new_or_old = "new";
                            }
                        }
                    }
                    if(performance == "Accuracy") {
                        if(new ~/[1-9]/) {
                            split(new,a1,",")
                            if(old ~/[1-9]/) {
                                split(old,a2,",")
                                if(a1[1] > a2[1]) {
                                    new_or_old = "new";
                                }
                            }else {
                                new_or_old = "new";
                            }
                        }
                    }
                }
                END { print new_or_old; }
            ')

            if [ "${new_or_old}" == "new" ];
            then 
                sed -i "s+${model};${mode};${machine_type};${data_type};${performance};.*+NEW+" ${target_file}
                sed -i "s+NEW+${line}+" ${target_file}

                reference_log_file="${WORKSPACE}/reference_log/benchmark/${job_name}/${log_file}"
                target_log_file="${WORKSPACE}/target_log/benchmark/${job_name}/${log_file}"

                mkdir -p ${target_log_file%/*}
                echo "${performance} -- |new: ${values},old: ${target_values}| -- ${new_or_old}"
                if [ -f ${reference_log_file} ];then 
                    cp  ${reference_log_file} ${target_log_file}
                fi 
            fi 
        else 
            if [ $(echo ${line} |grep 'Model,Mode' |wc -l) -eq 0 ];
            then 
                echo "${line}" >> ${target_file}
            fi 
        fi 

    done < ${reference_file}
done 
