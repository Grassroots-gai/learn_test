#!/usr/bin/env bash

echo "WORKSPACE: $WORKSPACE"
echo "NODE_NAME: $NODE_NAME"

WORKSPACE=$PWD
LOCAL_MODELS_LOCATION=/tf_dataset/pre-trained-models
BUCKET_NAME=intel-optimized-tensorflow/models

# clean up if exists:
rm -f $WORKSPACE/published_models.txt
rm -f $WORKSPACE/approved_models_names.txt
rm -rf $WORKSPACE/temp
mkdir temp

# Upload the model to GCP:
function upload_model() {
        echo "***** Uploading the $published_name model.."
        {
            gsutil cp ${WORKSPACE}/temp/${published_name} gs://${BUCKET_NAME} && echo "***** ${published_name} is successfully updated!"
        } || {  echo "=========================================================================="
        		echo " Failed to upload the model: ${published_name}!"
                echo "=========================================================================="
                exit 1
        }
}

# Find the model checkpoint dir, archive and upload to GCP:
function archive_upload_checkpoint() {
    local_checkpoint_dir=$(basename "$(ls ${LOCAL_MODELS_LOCATION}/${model}/${precision})")
    model_local_path=${LOCAL_MODELS_LOCATION}/${model}/${precision}/$local_checkpoint_dir
    
    pushd "${WORKSPACE}/temp"
    if [ "${model}" == "rfcn" ]; then
        published_name="rfcn_resnet101_fp32_coco_pretrained_model.tar.gz"
        # use the existing rfcn_pipeline.config file that has the updated dataset and annotation file paths.
        wget --quiet https://storage.googleapis.com/intel-optimized-tensorflow/models/rfcn_resnet101_fp32_coco_pretrained_model.tar.gz
        tar -xvf rfcn_resnet101_fp32_coco_pretrained_model.tar.gz
        cp rfcn_resnet101_fp32_coco/rfcn_pipeline.config .
    else
        if [[ ${local_checkpoint_dir} == *[.]* ]]; then
            local_checkpoint_dir=${precision}
            model_local_path=${LOCAL_MODELS_LOCATION}/${model}/${precision}
        fi
        cp -r ${model_local_path} .
        if [ "${model}" == "rfcn" ]; then
            cp rfcn_pipeline.config ${local_checkpoint_dir}
        fi
        tar -czf ${published_name} ${local_checkpoint_dir}
        rm -rf ${local_checkpoint_dir}
        upload_model
    fi
    popd
}

# Get the model name from the public buckets and the approved models locally.
gsutil ls gs://"$BUCKET_NAME"/* >> $WORKSPACE/published_models.txt
ls $LOCAL_MODELS_LOCATION >> $WORKSPACE/approved_models_names.txt


#sync up if the model exists in the public bucket:
while read model; do
    if [ ${model} == "transformerLanguage" ]; then
        precision="fp32"
        published_name="transformer_lt_fp32_pretrained_model.tar.gz" archive_upload_checkpoint
    elif [ ${model} == "transformerSpeech" ]; then
        precision="fp32"
        published_name="transformer_lt_official_fp32_pretrained_model.tar.gz" archive_upload_checkpoint
    else
        # check if it's a published checkpoint or .pb file
        for precision in int8 fp32; do
            # match the local model name and precision with the published models list and return the model public path.
            model_public_path=$(egrep -i "${model}+_+${precision}+?(_unconditional_cifar10)+_+pretrained*|${model}+_+${precision}+_+pretrained*|${model}+_+?[a-z.0-9]+_+${precision}*|f+${model:1:6}+_+${precision}*|${model:0:3}?[a-z._+-]+${model:${#model}-2:${#model}-1}+_+${precision}+_pretrained*" $WORKSPACE/published_models.txt)
            if [ ! -z "${model_public_path}" ]; then
                published_name=$(basename ${model_public_path})
                if [ "${published_name##*.}" == "pb" ] && [ ! -z $(basename "$(ls ${LOCAL_MODELS_LOCATION}/${model}/${precision}/*.pb)") ]; then
                    # specify the file name when more than one .pb files found locally
                    if [ "${model}" == "inceptionv3" ] || [ "${model}" == "resnet50" ] && [ ${precision} == "fp32" ]; then
                        model_graph=optimized_${model}.pb
                    elif [ ${model} == "resnet101" ] && [ ${precision} == "int8" ]; then
                        model_graph=resnet101_pad_fused.pb
                    elif [ ${model} == "rfcn" ] && [ ${precision} == "int8" ]; then
                        model_graph=per-channel-int8-rfcn-graph.pb
                    elif [ ${model} == "SSDMobilenet" ] && [ ${precision} == "int8" ]; then
                        model_graph=int8_final.pb
                    else
                        model_graph=$(basename "$(ls ${LOCAL_MODELS_LOCATION}/${model}/${precision}/*.pb)")
                    fi
                    model_local_path=${LOCAL_MODELS_LOCATION}/${model}/${precision}/${model_graph}
                    cp ${model_local_path} ${WORKSPACE}/temp/${published_name}
                    upload_model

                elif [ "${published_name##*.}" == "gz" ] && [ ! -z "$(basename "$(ls ${LOCAL_MODELS_LOCATION}/${model}/${precision})")" ]; then
                    # for checkpoints
                    archive_upload_checkpoint
                else
                     echo "==================================================================="
                     echo " The model ${published_name} is not found locally!"
                     echo "==================================================================="
                fi

            else
                echo "=========================================================================="
                echo " The model ${model} with precision ${precision} is not published yet!"
                echo "=========================================================================="
            fi

        done
    fi
done <${WORKSPACE}/approved_models_names.txt
