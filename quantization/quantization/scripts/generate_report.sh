#!/bin/bash

function main {
    echo "summaryLog: ${summaryLog}"
    echo "lastFile: ${lastFile}"
    generate_html_head
    generate_html_body
    generate_results
    generate_html_footer

}

function generate_inference {

    awk -v model="${model}" -F ';' '
        BEGINE {
            fp32_ms_bs = nan;
            fp32_ms_value = nan;
            fp32_ms_url = nan;
            fp32_fps_bs = nan;
            fp32_fps_value = nan;
            fp32_fps_url = nan;
            fp32_acc_bs = nan;
            fp32_acc_value = nan;
            fp32_acc_url = nan;
            
            int8_ms_bs = nan;
            int8_ms_value = nan;
            int8_ms_url = nan;
            int8_fps_bs = nan;
            int8_fps_value = nan;
            int8_fps_url = nan;
            int8_acc_bs = nan;
            int8_acc_value = nan;
            int8_acc_url = nan;
        }{
            if($4 == model) {
                // FP32
                if($3 == "FP32") {
                    // Latency
                    if($6 == "Latency") {
                        if( $8 ~/[0-9]/) {
                            fp32_ms_bs = $7;
                            fp32_ms_value = $8;
                        }
                        fp32_ms_url = $9;
                    }
                    // Throughput
                    if($6 == "Throughput") {
                        if($8 ~/[0-9]/) {
                            fp32_fps_bs = $7;
                            fp32_fps_value = $8;
                        }
                        fp32_fps_url = $9;
                    }
                    // Accuracy
                    if($6 == "Accuracy") {
                        if($8 ~/[0-9]/) {
                            fp32_acc_bs = $7;
                            fp32_acc_value = $8;
                        }
                        fp32_acc_url = $9;
                    }
                }
                
                // INT8
                if($3 == "INT8") {
                    // Latency
                    if($6 == "Latency") {
                        if($8 ~/[0-9]/) {
                            int8_ms_bs = $7;
                            int8_ms_value = $8;
                        }
                        int8_ms_url = $9;
                    }
                    // Throughput
                    if($6 == "Throughput") {
                        if($8 ~/[0-9]/) {
                            int8_fps_bs = $7;
                            int8_fps_value = $8;
                        }
                        int8_fps_url = $9;
                    }
                    // Accuracy
                    if($6 == "Accuracy") {
                        if($8 ~/[0-9]/) {
                            int8_acc_bs = $7;
                            int8_acc_value = $8;
                        }
                        int8_acc_url = $9;
                    }
                }
            }
        }END {
            printf("%s;%.5f;%s;%.5f;%s;%s;", int8_ms_bs,int8_ms_value,int8_fps_bs,int8_fps_value,int8_acc_bs,int8_acc_value);
            printf("%s;%.5f;%s;%.5f;%s;%s;", fp32_ms_bs,fp32_ms_value,fp32_fps_bs,fp32_fps_value,fp32_acc_bs,fp32_acc_value);
            printf("%s;%s;%s;%s;%s;%s", int8_ms_url,int8_fps_url,int8_acc_url,fp32_ms_url,fp32_fps_url,fp32_acc_url);
        }
    ' $1
}

function generate_html_core {
    echo "<tr><td rowspan=3>${model}</td>" >> ${WORKSPACE}/report.html
    echo |awk -v current_values=${current_values} -v last_values=${last_values} -v model=${model} -F ';' '

        function abs(x) { return x < 0 ? -x : x }

        function show_new_last(a,b,c,d) {
            if(c ~/[1-9]/) {
                if (d == "ms") {
                    printf("<td>%s</td> <td><a href=%s>%.2f</a></td>\n",a,b,c);
                }else if(d == "fps") {
                    printf("<td>%s</td> <td><a href=%s>%.2f</a></td>\n",a,b,c);
                }else {
                    printf("<td>%s</td> <td><a href=%s>%.4f</a></td>\n",a,b,c);
                }
            }else {
                if(b == "") {
                    printf("<td></td> <td></td>\n");
                }else
                {
                    printf("<td>%s</td> <td><a href=%s>Failure</a></td>\n",a,b);
                }
            }
        }

        function compare_result(a,b,c) {

            if(a ~/[1-9]/ && b ~/[1-9]/) {
                if(c == "acc") {
                    target = b - a;
                    if(target <= 0.01) {
                      status_png = check_png;
                    }else {
                      status_png = cross_png;
                    }
                }else if(c == "ms"){
                    target = a / b;
                    if(target >= 0.95) {
                      status_png = check_png;
                    }else {
                      status_png = cross_png;
                      }
                }else {
                    target = b / a;
                    if(target >= 0.95) {
                      status_png = check_png;
                    }else {
                      status_png = cross_png;
                        }
                    }
                    printf("<td colspan=2 class=\"col-cell col-cell1\">%.4f %s</td>", target,status_png);
                }else {
                    status_png = cross_png;
                    if( b ~ /[1-9]/) {
                      status_png = ""
                    }
                    printf("<td colspan=2 class=\"col-cell col-cell1\">%s</td>",status_png);
                }
            }

        function compare_current(a,b,c) {

            if(a ~/[1-9]/ && b ~/[1-9]/) {
                if(c == "acc") {
                    target = a - b;
                    if(target >= 0.01) {
                      status_png = check_png;
                    }else {
                      status_png = cross_png;
                    }
                    printf("<td class=\"col-cell col-cell1\" rowspan=3>%.4f</td>", target);
                }else {
                    target = a / b;
                    if(target >= 2) {
                      status_png = check_png;
                    }else {
                      status_png = cross_png;
                    }
                    printf("<td class=\"col-cell col-cell1\" rowspan=3>%.2f</td>", target);
                }

                }else {
                    status_png = cross_png;
                printf("<td class=\"col-cell col-cell1\" rowspan=3>%s</td>", status_png);
                }
            }

        BEGIN {
            check_png = "<img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/check.png></img>";
            cross_png = "<img src=http://heims.sh.intel.com/static/doc/tensorflow/images/16x16/cross.png></img>";

            // issue list
            jira_mobilenet = "https://jira01.devtools.intel.com/browse/PADDLEQ-384";
            jira_resnext = "https://jira01.devtools.intel.com/browse/PADDLEQ-387";
            jira_ssdmobilenet = "https://jira01.devtools.intel.com/browse/PADDLEQ-406";
        }{
            // Current values
            split(current_values,current_value,";");

            // Last values
            split(last_values,   last_value,   ";");

            // current
            print("<td>Cur</td>");
            show_new_last(current_value[1],current_value[13],current_value[2],"ms");
            show_new_last(current_value[3],current_value[14],current_value[4],"fps");
            show_new_last(current_value[5],current_value[15],current_value[6],"acc");
            show_new_last(current_value[7],current_value[16],current_value[8],"ms");
            show_new_last(current_value[9],current_value[17],current_value[10],"fps");
            show_new_last(current_value[11],current_value[18],current_value[12],"acc");

            // Compare Current
            compare_current(current_value[8],current_value[2],"ms");
            compare_current(current_value[4],current_value[10],"fps");
            compare_current(current_value[6],current_value[12],"acc");

            // last
            print "</tr><tr><td>Last</td>"
            show_new_last(last_value[1],last_value[13],last_value[2],"ms");
            show_new_last(last_value[3],last_value[14],last_value[4],"fps");
            show_new_last(last_value[5],last_value[15],last_value[6],"acc");
            show_new_last(last_value[7],last_value[16],last_value[8],"ms");
            show_new_last(last_value[9],last_value[17],last_value[10],"fps");
            show_new_last(last_value[11],last_value[18],last_value[12],"acc");

            // current vs last
            print "</tr><tr><td>Cur vs Last</td>"
            compare_result(last_value[2],current_value[2],"ms");
            compare_result(last_value[4],current_value[4],"fps");
            compare_result(last_value[6],current_value[6],"acc");
            compare_result(last_value[8],current_value[8],"ms");
            compare_result(last_value[10],current_value[10],"fps");
            compare_result(last_value[12],current_value[12],"acc");

        }
    ' >> ${WORKSPACE}/report.html
}

function generate_results {

    models=$(sed '1d' ${summaryLog} |cut -d';' -f4 | uniq)
    
    for model in ${models[@]}
    do
        current_values=$(generate_inference ${summaryLog})
        last_values=$(generate_inference ${lastFile})
        
        generate_html_core
    done
}

function generate_html_body {
MR_TITLE=''
Test_Info_Title=''
Test_Info=''
if [ "$MR_branch" != "" ];
then
  MR_TITLE="[ <a href='${gitlabSourceRepoHomepage}/merge_requests/${gitlabMergeRequestIid}'>MR-${gitlabMergeRequestIid}</a> ]"
  Test_Info_Title="<th colspan="2">Source Branch</th> <th colspan="4">Target Branch</th> <th colspan="4">Commit ID</th> <th colspan="4">Author</th> <th colspan="4">Message</th>"
  Test_Info="<td colspan="2">${gitlabSourceBranch}</td> <td colspan="4">${gitlabTargetBranch}</td> <td colspan="4">${gitlabMergeRequestLastCommit}</td> <td colspan="4">${gitlabUserEmail}</td> <td colspan="4">${gitlabMergeRequestTitle}</td>"
else
  Test_Info_Title="<th colspan="4">Qtools Branch</th> <th colspan="4">Commit ID</th> "
  Test_Info="<th colspan="4">${qtools_branch}</th> <th colspan="4">${qtools_commit}</th> "
fi
cat >> ${WORKSPACE}/report.html << eof

<body>
    <div id="main">
	    <h1 align="center">TensorFlow Quantization Tests ${MR_TITLE}
        [ <a href="${BUILD_URL}">Job-${BUILD_NUMBER}</a> ]</h1>
	    <table class="features-table">
	        <tr>
              <th>Platform</th>
              <th>TensorFlow Version</th>
              <th>Models Branch</th>
              <th>Repo</th>
              ${Test_Info_Title}
		      </tr>
		      <tr>
			        <td>CLX8280</td>
              <th>${tensorflow_version}</th>
			        <th>${models_branch}</th>
			        <td><a href="https://gitlab.devtools.intel.com/intelai/tools">IntelAI-Tools</a></td>
              ${Test_Info}
			    </tr>
	    </table>
	    <br>
		  <table class="features-table">
            <tr>
                <th rowspan="2">Model</th>
                <th rowspan="2">Change</th>
			          <th colspan="6">INT8</th>
			          <th colspan="6">FP32</th>
			          <th colspan="3" class="col-cell col-cell1 col-cellh">Ratio</th>
		        </tr>
		        <tr>
                <th>bs</th>
                <th>ms</th>
                <th>bs</th>
                <th>imgs/s</th>
                <th>bs</th>
                <th>top1</th>
                <th>bs</th>
                <th>ms</th>
                <th>bs</th>
                <th>imgs/s</th>
                <th>bs</th>
                <th>top1</th>
                <th class="col-cell col-cell1">Latency<br><font size="2px">FP32/INT8>=2</font></th>
                <th class="col-cell col-cell1">Throughput<br><font size="2px">INT8/FP32>=2</font></th>
                <th class="col-cell col-cell1">Accuracy<br><font size="2px">INT8-FP32>=0.01</font></th>
		        </tr>
eof
}

function generate_html_footer {

    cat >> ${WORKSPACE}/report.html << eof
		    <tr>
			    <td colspan="14"><font color="#d6776f">Note: </font>All data tested on TensorFlow Dedicated Server.</td>
			    <td colspan="3" class="col-cell col-cell1 col-cellf"></td>
		    </tr>
	    </table>
	</div>
</body>
</html>
eof
}

function generate_html_head {

cat > ${WORKSPACE}/report.html << eof

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html lang="en">
<head>
    <meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">  
    <title>Daily Tests - TensorFlow - Jenkins</title> 
    <style type="text/css">    
        body
        {
	        margin: 0;
	        padding: 0;
	        background: white no-repeat left top;
        }
        #main
        {
	        // width: 100%;
	        margin: 20px auto 10px auto;
	        background: white;
	        -moz-border-radius: 8px;
	        -webkit-border-radius: 8px;
	        padding: 0 30px 30px 30px;
	        border: 1px solid #adaa9f;
	        -moz-box-shadow: 0 2px 2px #9c9c9c;
	        -webkit-box-shadow: 0 2px 2px #9c9c9c;
        }
        .features-table
        {
          width: 100%;
          margin: 0 auto;
          border-collapse: separate;
          border-spacing: 0;
          text-shadow: 0 1px 0 #fff;
          color: #2a2a2a;
          background: #fafafa;  
          background-image: -moz-linear-gradient(top, #fff, #eaeaea, #fff); /* Firefox 3.6 */
          background-image: -webkit-gradient(linear,center bottom,center top,from(#fff),color-stop(0.5, #eaeaea),to(#fff)); 
          font-family: Verdana,Arial,Helvetica
        }
        .features-table th,td
        {
          text-align: center;
          height: 25px;
          line-height: 25px;
          padding: 0 8px;
          border: 1px solid #cdcdcd;
          box-shadow: 0 1px 0 white;
          -moz-box-shadow: 0 1px 0 white;
          -webkit-box-shadow: 0 1px 0 white;
          white-space: nowrap;
        }
        .no-border th
        {
          box-shadow: none;
          -moz-box-shadow: none;
          -webkit-box-shadow: none;     
        }
        .col-cell
        {
          text-align: center;
          width: 150px;
          font: normal 1em Verdana, Arial, Helvetica;  
        }
        .col-cell3
        {
          background: #efefef;
          background: rgba(144,144,144,0.15);
        }
        .col-cell1, .col-cell2
        {
          background: #B0C4DE;  
          background: rgba(176,196,222,0.3);
        }
        .col-cellh
        {
          font: bold 1.3em 'trebuchet MS', 'Lucida Sans', Arial;  
          -moz-border-radius-topright: 10px;
          -moz-border-radius-topleft: 10px; 
          border-top-right-radius: 10px;
          border-top-left-radius: 10px;
          border-top: 1px solid #eaeaea !important; 
        }
        .col-cellf
        {
          font: bold 1.4em Georgia;   
          -moz-border-radius-bottomright: 10px;
          -moz-border-radius-bottomleft: 10px; 
          border-bottom-right-radius: 10px;
          border-bottom-left-radius: 10px;
          border-bottom: 1px solid #dadada !important;
        }
    </style>
</head>

eof

}

main