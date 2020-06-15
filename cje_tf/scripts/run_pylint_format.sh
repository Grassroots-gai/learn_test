#!/bin/bash
set -x

wget https://raw.githubusercontent.com/tensorflow/tensorflow/master/tensorflow/tools/ci_build/pylintrc

function usage {
    echo "Usage: $0 DIR..."
    exit 1
}

if [ $# -eq 0 ]; then
    usage
fi

# Check all of the arguments first to make sure they're all directories
for dir in "$@"; do
    if [ ! -d "${dir}" ]; then
        echo "${dir} is not a directory"
        usage
    fi
done

format_file(){
    echo "in format_file, checking ${1}..."
    file="${1}"
    echo "${1}" >> $WORKSPACE/summary.log
    rcfile=../pylintrc
    pylint --rcfile=${rcfile} ${1} > ${1}.pylint.txt 2>&1
    ret=$?
    echo $ret
    cat ${1}.pylint
    #checking for return code
    if [ "$ret" -ne 0 ]; then
       echo "${1}" >> ../pylint_failed.txt
    else
         rm "${1}.pylint.txt"
    fi

}
pyv=$(python -V 2>&1)
case "$pyv" in
    *2.7*)
        PIP="pip"
        ;;
    *3.5*)
        PIP="python3.5 -m pip"
        ;;
    *3.6*)
        PIP="python3.6 -m pip"
        ;;
    *)
        echo "Error: unknown"
        ;;
    esac
echo $PIP
${PIP} install virtualenv
virtualenv --version
virtualenv venv_for_pylint
source venv_for_pylint/bin/activate
${PIP} install pylint

export PATH=/opt/tensorflow/python:$PATH;
echo "$PATH"

export -f format_file
for dir in "$@"; do
    echo "dir is $dir"
    pushd $dir
    find . \
         \( -name '*.py' \
         -o -name '*.py3' \
         -o -name '*.pyo' \
         -o -name '*.pyc' \
         -o -name '*.pyw' \
         -o -name '*.pyx' \)\
         -exec bash -c 'format_file "$0"' {} \;
    popd &>/dev/null
done
