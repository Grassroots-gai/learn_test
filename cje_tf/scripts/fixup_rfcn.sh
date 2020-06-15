#!/bin/bash

if [ -z ${DATASET_LOCATION} ] ; then
    DATASET_LOCATION="/tf_dataset"
fi

DATASET_DIR="${DATASET_LOCATION}/dataset"
WORKDIR=`pwd`
CJE_TF_SCRIPTS_DIR=${WORKDIR}/cje-tf/scripts
cd $CJE_TF_SCRIPTS_DIR 
sed "s|\/dataset|${DATASET_DIR}|" rfcn_pipeline.config > rfcn_pipeline.config.tmp
cp rfcn_pipeline.config.tmp rfcn_pipeline.config
