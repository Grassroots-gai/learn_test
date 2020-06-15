#!/bin/bash

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
    file="${1}"
    echo "${1}" >> ../summary.log
    /opt/tensorflow/google-code-compliance/clang+llvm-3.9.0-x86_64-fedora23/bin/clang-format -style=Google ${1} > ${1}.clang.txt
    cat ${1}.clang.txt
    if ! diff -q "${1}" "${1}.clang.txt" &>/dev/null; then
        echo "${1}" >> ../clang_failed.txt
        diff "${1}" "${1}.clang.txt" >> ../clang_failed.txt
    else
        rm "${1}.clang.txt"
    fi
}


# Run clang-format -i on all of the things
export PATH=/opt/tensorflow/gcc/gcc6.3/bin:$PATH;
export LD_LIBRARY_PATH=/opt/tensorflow/gcc/gcc6.3/lib64:$LD_LIBRARY_PATH
export -f format_file
for dir in "$@"; do
    pushd "${dir}" &>/dev/null
#    if ! find-dominating-file . .clang-format; then
#        echo "Failed to find dominating .clang-format starting at $PWD"
#        continue
#    fi
    find . \
         \( -name '*.c' \
         -o -name '*.cc' \
         -o -name '*.cpp' \
         -o -name '*.h' \
         -o -name '*.hh' \
         -o -name '*.hpp' \)\
         -exec bash -c 'format_file "$0"' {} \;
    popd &>/dev/null
done
