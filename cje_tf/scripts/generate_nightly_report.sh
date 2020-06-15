#!/bin/bash

function main {
    
    overview_log="${WORKSPACE}/summary_overview.log"
    reference_overview="${WORKSPACE}/reference_log/summary_overview.log"
    unittest_log="${WORKSPACE}/summary_unittest.log"
    reference_unittest="${WORKSPACE}/reference_log/summary_unittest.log"
    benchmark_dir="${WORKSPACE}/benchmark"
    summary_report="${WORKSPACE}/tensorflow_nightly_report.html"
    png_path="http://heims.sh.intel.com/static/doc/tensorflow/images/24x24"
    
    # create report
    createHead
    heims_url="http://heims.sh.intel.com/static/doc/tensorflow/nightly/${1}/"
    echo "<body> <div id=\"main\"> <h1 align=\"center\">TensorFlow $1 Nightly Tests Report [ <a href=\"${heims_url}${test_start_date}-detail.html\">${test_start_date}</a> ]</h1>" >> ${summary_report}
    createSummary $1 $2
    createOverview $1
    createUnitTest $1 $2
    createCheckTime $1 $2
    createBenchmark $1 $2
    createFooter
}

function createHead {

    cat > ${summary_report} <<  eof
    
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html lang="en">
<head>
    <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">  
    <title>TensorFlow Nightly Tests - Jenkins</title> 
    <link rel="stylesheet" type="text/css" href="http://heims.sh.intel.com/static/doc/tensorflow/css/daily_report.css">
</head>
eof
}

function createFooter {
    
    cat >> ${summary_report} <<  eof
		    <tr>
			    <td colspan="18"><font color="#d6776f">Note: </font>All data tested on AIPG server.</td>
	
		    </tr>
	    </table>
	</div>
</body>
</html>
eof
}

function createSummary {

    if [ "$2" != "all" ];then 
        echo "<p align="center">[ Please click <a href="${heims_url}${test_start_date}-detail.html">here</a> to see the detail report ]</p>" >> ${summary_report}
    fi 

    cat >> ${summary_report} <<  eof

    <h2>Summary</h2>
    <table class="features-table" style="width: 60%;margin: 0 auto 0 0;">
        <tr>
            <th>Nightly</th>
            <th>TensorFlow</th>
            <th>Branch</th>
            <th>Commit</th>
            <th>Revision URL</th>
        </tr>
        <tr>
        <td>New</td><td>private tensorflow</td>
        $(
            branch=""
            commit=""
            url=""
            for logfile in $(find ${WORKSPACE}/benchmark/ -name 'summary_aipg*.log')
            do 
                if [ "${branch}" == "" ] || [ "${commit}" == "" ] || [ "${url}" == "" ];then 
                    branch=$(grep 'branch:' ${logfile} |sed 's/.*branch://')
                    commit=$(grep 'commit:' ${logfile} |sed 's/.*commit://')
                    url=$(grep 'url:' ${logfile} |sed 's/.*url://')
                fi
            done 

            if [ "${commit}" == "" ];then 
                url=$(grep 'tensorflow_repo=' ${WORKSPACE}/tensorflow_info.log |sed 's/.*=//')
                branch=$(grep 'tensorflow_branch=' ${WORKSPACE}/tensorflow_info.log |sed 's/.*=//')
                commit=$(grep 'tensorflow_commit=' ${WORKSPACE}/tensorflow_info.log |sed 's/.*=//')
            fi

            echo "<td>${branch}</td><td>${commit}</td><td>$(echo ${url} |sed 's+//.*@+//+')</td>"
        )
        </tr>
         <tr>
        <td>Last</td><td>private tensorflow</td>
        $(
            branch=""
            commit=""
            url=""
            for logfile in $(find ${WORKSPACE}/reference_log/benchmark/ -name 'summary_aipg*.log')
            do 
                if [ "${branch}" == "" ] || [ "${commit}" == "" ] || [ "${url}" == "" ];then 
                    branch=$(grep 'branch:' ${logfile} |sed 's/.*branch://')
                    commit=$(grep 'commit:' ${logfile} |sed 's/.*commit://')
                    url=$(grep 'url:' ${logfile} |sed 's/.*url://')
                fi
            done 

            if [ "${commit}" == "" ];then 
                url=$(grep 'tensorflow_repo=' ${WORKSPACE}/reference_log/tensorflow_info.log |sed 's/.*=//')
                branch=$(grep 'tensorflow_branch=' ${WORKSPACE}/reference_log/tensorflow_info.log |sed 's/.*=//')
                commit=$(grep 'tensorflow_commit=' ${WORKSPACE}/reference_log/tensorflow_info.log |sed 's/.*=//')
            fi

            echo "<td>${branch}</td><td>${commit}</td><td>$(echo ${url} |sed 's+//.*@+//+')</td>"
        )
        </tr>
    </table>
eof
}

function createOverview {
    
    # update tensorflow
    # update_repo=($(grep 'update-repo' ${overview_log} |sed 's/,/ /'))
    # container_build=($(grep 'Container-Master' ${overview_log} |sed 's/,/ /'))
    jenkins_job_url="https://aipg-jenkins-tf.intel.com/job/"
    
    # update tensorflow
    update_tensorflow=($(grep 'update-repo' ${overview_log} |sed 's/,/ /g'))
    if [[ "${update_tensorflow[1]}" == *"FAIL"* ]];then 
        update_tensorflow_status="<img src=${png_path}/red.png></img>"
    elif [[ "${update_tensorflow[1]}" == *"SUCC"* ]];then 
        update_tensorflow_status="<img src=${png_path}/blue.png></img>"
    else 
        update_tensorflow_status="<img src=${png_path}/yellow.png></img>"
    fi 
    
    if [ "$1" == "V1" ];then 
        # check time bombs
        check_time_bombs=($(grep 'check-time-bombs' ${overview_log} |sed 's/,/ /g'))
        if [[ "${check_time_bombs[1]}" == *"FAIL"* ]];then 
            check_time_bombs_status="<img src=${png_path}/red.png></img>"
        elif [[ "${check_time_bombs[1]}" == *"SUCC"* ]];then 
            check_time_bombs_status="<img src=${png_path}/blue.png></img>"
        else 
            check_time_bombs_status="<img src=${png_path}/yellow.png></img>"
        fi 
    fi 
    
    # build wheel
    build_wheel=($(grep 'tensorflow-build-trigger' ${overview_log} |sed 's/,/ /g'))
    if [[ "${build_wheel[1]}" == *"FAIL"* ]];then 
        build_wheel_status="<img src=${png_path}/red.png></img>"
    elif [[ "${build_wheel[1]}" == *"SUCC"* ]];then 
        build_wheel_status="<img src=${png_path}/blue.png></img>"
    else 
        build_wheel_status="<img src=${png_path}/yellow.png></img>"
    fi 
    
    # container build
    container_build=($(grep 'Nightly-Container' ${overview_log} |sed 's/,/ /g'))
    if [[ "${container_build[1]}" == *"FAIL"* ]];then 
        container_build_status_0="<img src=${png_path}/red.png></img>"
    elif [[ "${container_build[1]}" == *"SUCC"* ]];then 
        container_build_status_0="<img src=${png_path}/blue.png></img>"
    else 
        container_build_status_0="<img src=${png_path}/yellow.png></img>"
    fi
    if [[ "${container_build[4]}" == *"FAIL"* ]];then 
        container_build_status_1="<img src=${png_path}/red.png></img>"
    elif [[ "${container_build[4]}" == *"SUCC"* ]];then 
        container_build_status_1="<img src=${png_path}/blue.png></img>"
    else 
        container_build_status_1="<img src=${png_path}/yellow.png></img>"
    fi 
    
    # unit test
    eigen_test=($(grep 'Tensorflow-Eigen' ${overview_log} |sed 's/,/ /g'))
    if [[ "${eigen_test[1]}" == *"FAIL"* ]];then 
        eigen_test_status="<img src=${png_path}/red.png></img>"
    elif [[ "${eigen_test[1]}" == *"SUCC"* ]];then 
        eigen_test_status="<img src=${png_path}/blue.png></img>"
    else 
        eigen_test_status="<img src=${png_path}/yellow.png></img>"
    fi 
    
    unit_test=($(grep 'Tensorflow-Unittest' ${overview_log} |sed 's/,/ /g'))
    if [[ "${unit_test[1]}" == *"FAIL"* ]];then 
        unit_test_status="<img src=${png_path}/red.png></img>"
    elif [[ "${unit_test[1]}" == *"SUCC"* ]];then 
        unit_test_status="<img src=${png_path}/blue.png></img>"
    else 
        unit_test_status="<img src=${png_path}/yellow.png></img>"
    fi  
    
    # benchmark
    benchmark_num=$(grep -i 'benchmark' ${overview_log} |wc -l)
    
    cat >> ${summary_report} <<  eof
    
    <h2>Overview</h2>
    <table class="features-table" style="width: 60%;margin: 0 auto 0 0;">
        <tr>
            <th>Task</th>
            <th>Job</th>
            <th>Status</th>
        </tr>
        $(
            if [[ "${task}" == *"TensorFlow Update"* ]];then 
                echo "<tr><td>TensorFlow Update</td>"
                echo "<td style=\"text-align:left\"><a href=\"${jenkins_job_url}${update_tensorflow[0]}/${update_tensorflow[2]}\">${update_tensorflow[0]}#${update_tensorflow[2]}</a></td>"
                echo "<td>${update_tensorflow_status}</td></tr>"
            fi
            
            if [[ "${1}" == *"V1"* ]];then 
                echo "<tr><td>Check Time Bombs</td>"
                echo "<td style=\"text-align:left\"><a href=\"${jenkins_job_url}${check_time_bombs[0]}/${check_time_bombs[2]}\">${check_time_bombs[0]}#${check_time_bombs[2]}</a></td>"
                echo "<td>${check_time_bombs_status}</td></tr>"
            fi
            
            if [[ "${task}" == *"Wheel Build"* ]];then 
                echo "<tr><td>Wheel Build</td>"
                echo "<td style=\"text-align:left\"><a href=\"${jenkins_job_url}${build_wheel[0]}/${build_wheel[2]}\">${build_wheel[0]}#${build_wheel[2]}</a></td>"
                echo "<td>${build_wheel_status}</td></tr>"
            fi
            
            if [[ "${task}" == *"Container Build"* ]];then 

                if [ "${container_build[3]}" != "" ];then 

                    echo "<tr><td rowspan=2>Container Build</td>"
                    echo "<td style=\"text-align:left\"><a href=\"${jenkins_job_url}${container_build[0]}/${container_build[2]}\">${container_build[0]}#${container_build[2]}</a></td>"
                    echo "<td>${container_build_status_0}</td></tr>"

                    echo "<tr><td style=\"text-align:left\"><a href=\"${jenkins_job_url}${container_build[3]}/${container_build[5]}\">${container_build[3]}#${container_build[5]}</a></td>"
                    echo "<td>${container_build_status_1}</td></tr>"
                else 
                    echo "<tr><td>Container Build</td>"
                    echo "<td style=\"text-align:left\"><a href=\"${jenkins_job_url}${container_build[0]}/${container_build[2]}\">${container_build[0]}#${container_build[2]}</a></td>"
                    echo "<td>${container_build_status_0}</td></tr>"
                fi
            fi
            
            if [[ "${task}" == *"Unit Test"* ]];then 
                echo "<tr><td rowspan=2>Unit Test</td>"
                echo "<td style=\"text-align:left\"><a href=\"${jenkins_job_url}${eigen_test[0]}/${eigen_test[2]}\">${eigen_test[0]}#${eigen_test[2]}</a></td>"
                echo "<td>${eigen_test_status}</td></tr>"

                echo "<tr><td style=\"text-align:left\"><a href=\"${jenkins_job_url}${unit_test[0]}/${unit_test[2]}\">${unit_test[0]}#${unit_test[2]}</a></td>"
                echo "<td>${unit_test_status}</td></tr>"
            fi

            if [[ "${task}" == *"Benchmark"* ]];then 
                grep -i 'benchmark' ${overview_log} |awk -F, -v benchmark_num=${benchmark_num} -v png_path=${png_path} -v jenkins_job_url=${jenkins_job_url} '
                    {
                        job_status = "<img src="png_path"/aborted.png></img>";
                        if($2 ~/SUCCESS/) {
                            job_status = "<img src="png_path"/blue.png></img>";
                        }
                        if($2 ~/FAIL/) {
                            job_status = "<img src="png_path"/red.png></img>";
                        }
                        if($2 ~/UNSTABLE/) {
                            job_status = "<img src="png_path"/yellow.png></img>";
                        }
                        
                        if(NR == 1) {
                            printf("<tr><td rowspan=%s>Benchmark</td>", benchmark_num);
                        }else {
                            printf("<tr>");
                        }
                        printf("<td style=\"text-align:left\"><a href=\"%s%s/%s\">%s#%s</a></td>", jenkins_job_url,$1,$3,$1,$3);
                        printf("<td HERE_IS%sCOLOR>%s</td></tr>", $1,job_status);
                    }
                '
            fi 
        )
    </table>
eof
}

function createCheckTime {

    if [ "$1" == "V1" ];then
        if [ "$2" == "all" ];then
            html_result="${WORKSPACE}/tools/tool-check-time-bombs/detail.html"
        else
            html_result="${WORKSPACE}/tools/tool-check-time-bombs/new.html"
        fi

        echo '<h2>Check Time Bombs</h2>' >> ${summary_report}
        if [ -f ${html_result} ];then
            cat ${html_result} >> ${summary_report}
        fi
    fi
    if [ "$1" == "R1.15" ];then
        if [ "$2" == "all" ];then
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-r1.15/detail.html"
        else
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-r1.15/new.html"
        fi

        echo '<h2>Check Time Bombs</h2>' >> ${summary_report}
        if [ -f ${html_result} ];then
            cat ${html_result} >> ${summary_report}
        fi
    fi
    if [ "$1" == "R2" ];then
        if [ "$2" == "all" ];then
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-r2.0/detail.html"
        else
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-r2.0/new.html"
        fi

        echo '<h2>Check Time Bombs</h2>' >> ${summary_report}
        if [ -f ${html_result} ];then
            cat ${html_result} >> ${summary_report}
        fi
    fi
    if [ "$1" == "V2" ];then
        if [ "$2" == "all" ];then
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-v2/detail.html"
        else
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-v2/new.html"
        fi

        echo '<h2>Check Time Bombs</h2>' >> ${summary_report}
        if [ -f ${html_result} ];then
            cat ${html_result} >> ${summary_report}
        fi
    fi
    if [ "$1" == "R2.1" ];then
        if [ "$2" == "all" ];then
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-r2.1/detail.html"
        else
            html_result="${WORKSPACE}/tools/tool-check-time-bombs-r2.1/new.html"
        fi

        echo '<h2>Check Time Bombs</h2>' >> ${summary_report}
        if [ -f ${html_result} ];then
            cat ${html_result} >> ${summary_report}
        fi
    fi
}

function createUnitTest {
    
    cat >> ${summary_report} <<  eof

    <h2>$1 Unit Test <img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/cross.png></img> HERE_IS_FAILED_NUM</h2>
    <table class="features-table" style="width: 60%;margin: 0 auto 0 0;">
        <tr>
            <th>Test Case</th>
            <th><a href="${BUILD_URL}artifact/reference_log/eigen_test">Last Eigen</a></th>
            <th><a href="${BUILD_URL}artifact/reference_log/unit_test">Last Unit Test</a></th>
            <th><a href="${BUILD_URL}artifact/eigen_test">New Eigen</a></th>
            <th><a href="${BUILD_URL}artifact/unit_test">New Unit Test</a></th>
            <th>Result</th>
        </tr>
        $(
            if [ $(grep "Unittest" ${overview_log} |grep -E "UNSTABLE|SUCCESS" |wc -l) -ne 0 ];then 
                all_ut_case=($(cat ${unittest_log} ${reference_unittest} |grep '^//' |cut -f1 -d, |sort |uniq))
                ut_failed_num=0
                for ut_case in ${all_ut_case[@]}
                do 
                    new_unit_status=$(grep "^${ut_case}," ${unittest_log} |cut -f3 -d,)
                    
                    if [ $(grep "Eigen" ${overview_log} |grep -E "UNSTABLE|SUCCESS" |wc -l) -ne 0 ];then 
                        new_eigen_status=$(grep "^${ut_case}," ${unittest_log} |cut -f2 -d,)
                    else
                        new_eigen_status=""
                    fi
                    
                    if [ $(grep "Eigen" ${reference_overview} |grep -E "UNSTABLE|SUCCESS" |wc -l) -ne 0 ];then 
                        last_eigen_status=$(grep "^${ut_case}," ${reference_unittest} |cut -f2 -d,)
                    else
                        last_eigen_status=""
                    fi
                    
                    if [ $(grep "Unittest" ${reference_overview} |grep -E "UNSTABLE|SUCCESS" |wc -l) -ne 0 ];then 
                        last_unit_status=$(grep "^${ut_case}," ${reference_unittest} |cut -f3 -d,)
                    else
                        last_unit_status=""
                    fi

                    status_png="<img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/check.png></img>"
                    ut_color=""

                    if [[ "${new_unit_status}" == *"FAIL"* ]];then 

                        if [ "${new_eigen_status}" == "" ] || [ "${last_unit_status}" == "" ];then 
                            ut_color=" bgcolor=yellow "
                        fi 

                        if [[ "${new_eigen_status}" != *"FAIL"* ]] || [[ "${last_unit_status}" == *"SUCCESS"* ]];then 
                            
                            status_png="<img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/cross.png></img>"
                            ut_failed_num=$[${ut_failed_num}+1]
                            echo "<tr><td style=\"text-align:left\">${ut_case}</td> <td>${last_eigen_status}</td><td>${last_unit_status}</td>"
                            echo "    <td>${new_eigen_status}</td> <td>${new_unit_status}</td> <td ${ut_color}>${status_png}</td></tr>"
                        # else
                        # 
                        #     if [ "$2" == "all" ];then
                        #         echo "<tr><td style=\"text-align:left\">${ut_case}</td> <td>${last_eigen_status}</td><td>${last_unit_status}</td>"
                        #         echo "    <td>${new_eigen_status}</td> <td>${new_unit_status}</td> <td ${ut_color}>${status_png}</td></tr>"
                        #     fi
                        fi 
                    fi 
                done
                echo "UT_FAILED_NUMBER: ${ut_failed_num}"
            fi 
        )
    </table>
eof

ut_failed_num=$(grep 'UT_FAILED_NUMBER' ${summary_report} |cut -f2 -d' ')
sed -ie "s/UT_FAILED_NUMBER.*//;s/HERE_IS_FAILED_NUM/${ut_failed_num}/"  ${summary_report}
}

function createBenchmark {
    
    cat >> ${summary_report} <<  eof
    
        <h2>$1 Benchmark <img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/cross.png></img> HERE_IS_benchmark_FAILED_NUM</h2>
        <table class="features-table">
            <tr>
                <th rowspan="2">Job</th>
                <th rowspan="2">Model</th>
                <th rowspan="2">Mode</th>
                <th rowspan="2">Platform</th>
                <th rowspan="2">Cores</th>
			    <th rowspan="2">Change</th>
			    <th colspan="6"> FP32</th>
			    <th colspan="6"> INT8</th>
		    </tr>
		    <tr>
			    <th>BS</th>
                <th>Latency</th>
                <th>BS</th>
			    <th>Throughput</th>
                <th>BS</th>
			    <th>Accuracy</th>
                <th>BS</th>
			    <th>Latency</th>
                <th>BS</th>
			    <th>Throughput</th>
                <th>BS</th>
			    <th>Accuracy</th>
		    </tr>
eof
    
    benchmark_total_failed_num=0
    
    for file in $(find ${WORKSPACE}/benchmark -type f -name 'summary_nightly.log' |sort)
    do
        benchmark_log=${file}
        reference_log=${file/\/benchmark\///reference_log/benchmark/}
        target_log=${file/\/benchmark\///target_log/benchmark/}

        job_name=$(echo ${file} |awk -F '/' '{ print $(NF-1) }')
        job_models_num=$[$(sed '1d' ${file} |cut -f1,2,3 -d';' |sort |uniq |wc -l)*5]

        tmp_row_num=0
        job_red=0
        job_yellow=0
        job_blue=0
        
        generateValues $2

        if [ ${job_red} -ne 0 ];then
            sed -i "s+HERE_IS${job_name}COLOR+bgcolor=red+g" ${summary_report}
        elif [ ${job_yellow} -ne 0 ];then 
            sed -i "s+HERE_IS${job_name}COLOR+bgcolor=yellow+g" ${summary_report}
        else 
            sed -i "s+HERE_IS${job_name}COLOR+bgcolor=blue+g" ${summary_report}
        fi 
        
        benchmark_total_failed_num=$[ ${benchmark_total_failed_num} + ${job_red} + ${job_yellow} ]

    done
    
    sed -i "s/HERE_IS_benchmark_FAILED_NUM/${benchmark_total_failed_num}/" ${summary_report}
}

function generateValues {
    
    job_fail_num=0
    
    models=($(sed '1d' ${benchmark_log} |cut -f1 -d ';' |sort |uniq))
    for model in ${models[@]}
    do
    
        modes=($(grep "${model}" ${benchmark_log} |cut -f2 -d ';' |sort |uniq))
        for mode in ${modes[@]}
        do
            
            servers=($(grep "${model};${mode}" ${benchmark_log} |cut -f3 -d ';' |sort |uniq))
            for server in ${servers[@]}
            do
            
                current_values=($(grep "${model};${mode};${server}" ${benchmark_log} |sed 's/ //g' |sort |awk -F ';' '
                    BEGIN {
                        fp32_acc = "nan;nan;nan";
                        fp32_ms = "nan;nan;nan";
                        fp32_fps = "nan;nan;nan";
                        
                        int8_acc = "nan;nan;nan";
                        int8_ms = "nan;nan;nan";
                        int8_fps = "nan;nan;nan";
                    }{
                        if( $4 == "fp32" && $5 == "Accuracy" ) {
                            fp32_acc = $6";"$7";"$8;
                        }
                        if( $4 == "fp32" && $5 == "Latency" ) {
                            fp32_ms = $6";"$7";"$8;
                        }
                        if( $4 == "fp32" && $5 == "Throughput" ) {
                            fp32_fps = $6";"$7";"$8;
                        }
                        
                        if( $4 == "int8" && $5 == "Accuracy" ) {
                            int8_acc = $6";"$7";"$8;
                        }
                        if( $4 == "int8" && $5 == "Latency" ) {
                            int8_ms = $6";"$7";"$8;
                        }
                        if( $4 == "int8" && $5 == "Throughput" ) {
                            int8_fps = $6";"$7";"$8;
                        }
                    }END {
                        printf("%s|%s|%s|%s|%s|%s", fp32_acc,fp32_ms,fp32_fps,int8_acc,int8_ms,int8_fps);
                    }
                '))
                last_values=($(grep "${model};${mode};${server}" ${reference_log} |sed 's/ //g' |sort |awk -F ';' '
                    BEGIN {
                        fp32_acc = "nan;nan;nan";
                        fp32_ms = "nan;nan;nan";
                        fp32_fps = "nan;nan;nan";
                        
                        int8_acc = "nan;nan;nan";
                        int8_ms = "nan;nan;nan";
                        int8_fps = "nan;nan;nan";
                    }{
                        if( $4 == "fp32" && $5 == "Accuracy" ) {
                            fp32_acc = $6";"$7";"$8;
                        }
                        if( $4 == "fp32" && $5 == "Latency" ) {
                            fp32_ms = $6";"$7";"$8;
                        }
                        if( $4 == "fp32" && $5 == "Throughput" ) {
                            fp32_fps = $6";"$7";"$8;
                        }
                        
                        if( $4 == "int8" && $5 == "Accuracy" ) {
                            int8_acc = $6";"$7";"$8;
                        }
                        if( $4 == "int8" && $5 == "Latency" ) {
                            int8_ms = $6";"$7";"$8;
                        }
                        if( $4 == "int8" && $5 == "Throughput" ) {
                            int8_fps = $6";"$7";"$8;
                        }
                    }END {
                        printf("%s|%s|%s|%s|%s|%s", fp32_acc,fp32_ms,fp32_fps,int8_acc,int8_ms,int8_fps);
                    }
                '))
                goal_values=($(grep "${model};${mode};${server}" ${target_log} |sed 's/ //g' |sort |awk -F ';' '
                    BEGIN {
                        fp32_acc = "nan;nan;nan";
                        fp32_ms = "nan;nan;nan";
                        fp32_fps = "nan;nan;nan";
                        
                        int8_acc = "nan;nan;nan";
                        int8_ms = "nan;nan;nan";
                        int8_fps = "nan;nan;nan";
                    }{
                        if( $4 == "fp32" && $5 == "Accuracy" ) {
                            fp32_acc = $6";"$7";"$8;
                        }
                        if( $4 == "fp32" && $5 == "Latency" ) {
                            fp32_ms = $6";"$7";"$8;
                        }
                        if( $4 == "fp32" && $5 == "Throughput" ) {
                            fp32_fps = $6";"$7";"$8;
                        }
                        
                        if( $4 == "int8" && $5 == "Accuracy" ) {
                            int8_acc = $6";"$7";"$8;
                        }
                        if( $4 == "int8" && $5 == "Latency" ) {
                            int8_ms = $6";"$7";"$8;
                        }
                        if( $4 == "int8" && $5 == "Throughput" ) {
                            int8_fps = $6";"$7";"$8;
                        }
                    }END {
                        printf("%s|%s|%s|%s|%s|%s", fp32_acc,fp32_ms,fp32_fps,int8_acc,int8_ms,int8_fps);
                    }
                '))
                
                generateTable $1
                
                tmp_row_num=$[${tmp_row_num}+1]
                
                job_red=$[${job_red}+${job_result[0]}]
                job_yellow=$[${job_yellow}+${job_result[1]}]
                job_blue=$[${job_blue}+${job_result[2]}]

            done 
        done 
    done 
}

function generateTable {

        echo |awk -v current_values=${current_values[@]} -v last_values=${last_values[@]} -v goal_values=${goal_values[@]} -v model=${model} -v mode=${mode} -v server=${server} -v job_name=${job_name} -v tmp_row_num=${tmp_row_num} -v job_models_num=${job_models_num} -v build_url=${BUILD_URL} -v all_or_fail=$1 '
            function abs(x) { return x < 0 ? -x : x }
            function show_new_last(a,b,c,d,e,f,g,h) {
                
                if(a == "new") {
                    c = c"artifact/benchmark/"d"/"e;
                }else if(a == "last") {
                    c = c"artifact/reference_log/benchmark/"d"/"e;
                }else if(a == "best") {
                    c = c"artifact/target_log/benchmark/"d"/"e;
                }else {
                    c = "";
                }
                
                if(f ~/[1-9]/) {
                    if (g == "ms") {
                        printf("<td>%s</td> <td><a href=%s>%.2f</a></td>\n",b,c,f);
                    }else if(g == "fps") {
                        printf("<td>%s</td> <td><a href=%s>%.1f</a></td>\n",b,c,f);
                    }else {
                        printf("<td>%s</td> <td><a href=%s>%s/%s</a></td>\n",b,c,f,h);
                    }
                }else {
                    if(f == "nan") {
                        printf("<td></td> <td></td>\n");
                    }else {
                        printf("<td>%s</td> <td><a href=%s>Failure</a></td>\n",b,c);
                    }
                }
            }
            function compare_result(a,b,c) {
                
                if(a ~/[1-9]/ && b ~/[1-9]/) {
                    if(c == "acc") {
                        target = a - b;
                        if(target >= -0.0001 && target <= 0.0001) {
                            status_png = check_png;
                            job_blue++;
                        }else {
                            status_png = cross_png;
                            job_yellow++;
                        }
                    }else {
                        target = a / b;
                        if(target >= 0.95) {
                            status_png = check_png;
                            job_blue++;
                        }else {
                            status_png = cross_png;
                            job_yellow++;
                        }
                    }
                    printf("<td class=\"col-cell col-cell1\" colspan=2>%.4f%s</td>", target,status_png);
                }else {
                    if(a == "nan" || b == "nan") {
                        printf("<td class=\"col-cell col-cell3\" colspan=2></td>");
                    }else {
                        printf("<td class=\"col-cell col-cell1\" colspan=2>NaN%s</td>", cross_png);
                        job_red++;
                    }
                }
            }
            function show_all() {

                // current
                printf("<td>New</td>");
                show_new_last("new",cur_fp32_ms[1],build_url,job_name,cur_fp32_ms[3],cur_fp32_ms[2],"ms");
                show_new_last("new",cur_fp32_fps[1],build_url,job_name,cur_fp32_fps[3],cur_fp32_fps[2],"fps");
                show_new_last("new",cur_fp32_acc[1],build_url,job_name,cur_fp32_acc[3],cur_fp32_acc_values[1],"acc",cur_fp32_acc_values[2]);
                show_new_last("new",cur_int8_ms[1],build_url,job_name,cur_int8_ms[3],cur_int8_ms[2],"ms");
                show_new_last("new",cur_int8_fps[1],build_url,job_name,cur_int8_fps[3],cur_int8_fps[2],"fps");
                show_new_last("new",cur_int8_acc[1],build_url,job_name,cur_int8_acc[3],cur_int8_acc_values[1],"acc",cur_int8_acc_values[2]);

                // last
                printf("</tr>\n<tr><td>Last</td>");
                show_new_last("last",lst_fp32_ms[1],build_url,job_name,lst_fp32_ms[3],lst_fp32_ms[2],"ms");
                show_new_last("last",lst_fp32_fps[1],build_url,job_name,lst_fp32_fps[3],lst_fp32_fps[2],"fps");
                show_new_last("last",lst_fp32_acc[1],build_url,job_name,lst_fp32_acc[3],lst_fp32_acc_values[1],"acc",lst_fp32_acc_values[2]);
                show_new_last("last",lst_int8_ms[1],build_url,job_name,lst_int8_ms[3],lst_int8_ms[2],"ms");
                show_new_last("last",lst_int8_fps[1],build_url,job_name,lst_int8_fps[3],lst_int8_fps[2],"fps");
                show_new_last("last",lst_int8_acc[1],build_url,job_name,lst_int8_acc[3],lst_int8_acc_values[1],"acc",lst_int8_acc_values[2]);
                
                // best
                printf("</tr>\n<tr><td>Best</td>");
                show_new_last("best",goa_fp32_ms[1],build_url,job_name,goa_fp32_ms[3],goa_fp32_ms[2],"ms");
                show_new_last("best",goa_fp32_fps[1],build_url,job_name,goa_fp32_fps[3],goa_fp32_fps[2],"fps");
                show_new_last("best",goa_fp32_acc[1],build_url,job_name,goa_fp32_acc[3],goa_fp32_acc_values[1],"acc",goa_fp32_acc_values[2]);
                show_new_last("best",goa_int8_ms[1],build_url,job_name,goa_int8_ms[3],goa_int8_ms[2],"ms");
                show_new_last("best",goa_int8_fps[1],build_url,job_name,goa_int8_fps[3],goa_int8_fps[2],"fps");
                show_new_last("best",goa_int8_acc[1],build_url,job_name,goa_int8_acc[3],goa_int8_acc_values[1],"acc",goa_int8_acc_values[2]);

                // current vs last
                printf("</tr>\n<tr><td>New/Last</td>");
                compare_result(lst_fp32_ms[2],cur_fp32_ms[2],"ms");
                compare_result(cur_fp32_fps[2],lst_fp32_fps[2],"fps");
                compare_result(cur_fp32_acc_values[1],lst_fp32_acc_values[1],"acc");
                compare_result(lst_int8_ms[2],cur_int8_ms[2],"ms");
                compare_result(cur_int8_fps[2],lst_int8_fps[2],"fps");
                compare_result(cur_int8_acc_values[1],lst_int8_acc_values[1],"acc");
                printf("</tr>\n");

                // current vs best
                printf("</tr>\n<tr><td>New/Best</td>");
                compare_result(goa_fp32_ms[2],cur_fp32_ms[2],"ms");
                compare_result(cur_fp32_fps[2],goa_fp32_fps[2],"fps");
                compare_result(cur_fp32_acc_values[1],goa_fp32_acc_values[1],"acc");
                compare_result(goa_int8_ms[2],cur_int8_ms[2],"ms");
                compare_result(cur_int8_fps[2],goa_int8_fps[2],"fps");
                compare_result(cur_int8_acc_values[1],goa_int8_acc_values[1],"acc");
                printf("</tr>\n");
            }
            
            function show(a,b,c,d,e,f,g,h,i,j,k,l) {
                
                show_status = "no";

                if(a ~/[1-9]/ && b ~/[1-9]/) {
                    if(a/b < 0.945) {
                        show_status = "yes";
                    }
                }else if(a != "nan" && b != "nan") {
                    show_status = "yes";
                }
                if(c ~/[1-9]/ && d ~/[1-9]/) {
                    if(c/d < 0.945) {
                        show_status = "yes";
                    }
                }else if(c != "nan" && d != "nan") {
                    show_status = "yes";
                }
                if(e ~/[1-9]/ && f ~/[1-9]/) {
                    if(abs(e-f) > 0.0001) {
                        show_status = "yes";
                    }
                }else if(e != "nan" && f != "nan") {
                    show_status = "yes";
                }
                
                if(g ~/[1-9]/ && h ~/[1-9]/) {
                    if(g/h < 0.945) {
                        show_status = "yes";
                    }
                }else if(g != "nan" && h != "nan") {
                    show_status = "yes";
                }
                if(i ~/[1-9]/ && j ~/[1-9]/) {
                    if(i/j < 0.945) {
                        show_status = "yes";
                    }
                }else if(i != "nan" && j != "nan") {
                    show_status = "yes";
                }
                if(k ~/[1-9]/ && l ~/[1-9]/) {
                    if(abs(k-l) > 0.0001) {
                        show_status = "yes";
                    }
                }else if(k != "nan" && l != "nan") {
                    show_status = "yes";
                }
                
                if(show_status == "yes") {
                    printf("<tr><td rowspan=5>%s</td><td rowspan=5>%s</td><td rowspan=5>%s</td><td rowspan=5>%s</td><td rowspan=5>28</td>",job_name,model,mode,server);
                    show_all();
                }
            }

            BEGIN {
                check_png = "<img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/check.png></img>";
                cross_png = "<img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/cross.png></img>";
                
                job_red = 0;
                job_yellow = 0;
                job_blue = 0;
            }{
                // Current values
                split(current_values,cur_values,"|");
                
                split(cur_values[1],cur_fp32_acc,";");
                split(cur_fp32_acc[2],cur_fp32_acc_values,",")
                split(cur_values[2],cur_fp32_ms,";");
                split(cur_values[3],cur_fp32_fps,";");
                
                split(cur_values[4],cur_int8_acc,";");
                split(cur_int8_acc[2],cur_int8_acc_values,",")
                split(cur_values[5],cur_int8_ms,";");
                split(cur_values[6],cur_int8_fps,";");
                
                // Last values
                split(last_values,lst_values,"|");
                
                split(lst_values[1],lst_fp32_acc,";");
                split(lst_fp32_acc[2],lst_fp32_acc_values,",")
                split(lst_values[2],lst_fp32_ms,";");
                split(lst_values[3],lst_fp32_fps,";");
                
                split(lst_values[4],lst_int8_acc,";");
                split(lst_int8_acc[2],lst_int8_acc_values,",")
                split(lst_values[5],lst_int8_ms,";");
                split(lst_values[6],lst_int8_fps,";");
                
                // goal values
                split(goal_values,goa_values,"|");
                
                split(goa_values[1],goa_fp32_acc,";");
                split(goa_fp32_acc[2],goa_fp32_acc_values,",")
                split(goa_values[2],goa_fp32_ms,";");
                split(goa_values[3],goa_fp32_fps,";");
                
                split(goa_values[4],goa_int8_acc,";");
                split(goa_int8_acc[2],goa_int8_acc_values,",")
                split(goa_values[5],goa_int8_ms,";");
                split(goa_values[6],goa_int8_fps,";");
                
                // current vs last
                if(all_or_fail == "all") {
                    if(tmp_row_num == 0) {
                        printf("<tr><td rowspan=%d>%s</td><td rowspan=5>%s</td><td rowspan=5>%s</td><td rowspan=5>%s</td><td rowspan=5>28</td>", job_models_num,job_name,model,mode,server);
                    }else {
                        printf("<tr><td rowspan=5>%s</td><td rowspan=5>%s</td><td rowspan=5>%s</td><td rowspan=5>28</td>", model,mode,server);
                    }
                    show_all()
                }else {
                    show(lst_fp32_ms[2],cur_fp32_ms[2],cur_fp32_fps[2],lst_fp32_fps[2],cur_fp32_acc_values[1],lst_fp32_acc_values[1],lst_int8_ms[2],cur_int8_ms[2],cur_int8_fps[2],lst_int8_fps[2],cur_int8_acc_values[1],lst_int8_acc_values[1]);
                }
            }
            END {
                print "";
                printf("%d  %d  %d", job_red,job_yellow,job_blue);
            }
        ' >> ${summary_report}
        
        job_result=($(tail -1 ${summary_report}))
        sed -i '$s/.*//' ${summary_report}
}

main $1 $2
