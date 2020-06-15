#!/bin/bash

TARGET_TAG="1.9"
TMP_DIR="/tmp/tf-tag-scan"

function usage_exit {
	echo "Usage: $@ [tag-pattern] [branch|tag]"
	exit 1
}

if [[ $# -ne 2 ]]; then
	usage_exit
elif [[ "$2" != "branch" ]] && \
     [[ "$2" != "tag" ]]; then
	usage_exit
fi


TARGET_TAG=$1
REF_TYPE=$2

#if we are looking for branches, we have to add --all 
if [[ "${REF_TYPE}" == "branch" ]]; then
	REF_TYPE="branch --all"
fi


#clone tensorflow
mkdir -p ${TMP_DIR}
cd ${TMP_DIR}
rm -rf tensorflow

git clone https://github.com/tensorflow/tensorflow.git
if [[ $? -gt 0 ]]; then
	echo "Error cloning tensorflow."
	exit 1
fi

cd tensorflow

#pull all of the tags
git fetch --tag

git ${REF_TYPE} | fgrep ${TARGET_TAG}

if [[ $? -gt 0 ]]; then
	echo "Target ${REF_TYPE} ${TARGET_TAG} does not exist. Ret code=1"
	exit 1
else
	echo "Target ${REF_TYPE} ${TARGET_TAG} exists!! Ret code=0"
fi
