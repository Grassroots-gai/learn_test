**1. Structure introduction**
*  **LZ_jenkins_main.groovy**  : Jenkins job main script  
*  **2020_shell_LZ.py**        : For testing models and collecting test data
*  **2020_LZ_write_html.py**   : Generate data report  
*  **collect_best_data.py**    : Collect the best data  
*  **get_machine_info.sh**     : Get the information of testing machine  
*  **CLX_config.ini**          : Configuration CLX machine required for testing
*  **CPX_config.ini**          : Configuration CPX machine required for testing


**2.  How to add a model to the test**

1.   Add model name in jenkins job configure ALL_MODELS   
2.   Add the model all required in CLX_config.ini or CPX_config.ini(Depends on the model of the machine you are using)
3.   Add the model test command in 2020_shell_LZ.py  
4.   Add the shell commands for analyzing test data in 2020_shell_LZ.py(function collectMain)
5.   Add new model dict in 2020_LZ_write_html.py(function collect_results)