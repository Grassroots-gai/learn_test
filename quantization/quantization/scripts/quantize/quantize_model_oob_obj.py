#
#  -*- coding: utf-8 -*-
#
#  Copyright (c) 2019 Intel Corporation
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

import os
import argparse
import re

from tensorflow.python.platform import app

import intel_quantization.graph_converter as converter
from intel_quantization.util import split_shared_inputs

def model_callback_cmds(data_location,model_location):
    script = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                          'dlft_oob_performance/tensorflow/models/research/object_detection/inference/infer_detections.py')
    flags = " --input_tfrecord_paths={}".format(data_location) + \
            " --output_tfrecord_path=./validation_detections.tfrecord-00000-of-00001" + \
            " --inference_graph={}".format(model_location) + \
            " --discard_image_pixels true" + \
            " --iterations 100"

    return 'python ' + script + flags

def main(_):
    c = None
    output_shape = args.model + "/predictions/Reshape_1"
    image_size=224
    output_dir = args.out_graph

    if args.model == "faster_rcnn_inception_resnet_v2" or args.model == 'mask_rcnn_inception_resnet_v2' or args.model == 'faster_rcnn_nas' or args.model == 'ssd_mobilenet_v2' or args.model == 'ssd_mobilenet_v1':
        excluded_ops=['ConcatV2']
        output_shape=['num_detections', 'detection_boxes', 'detection_scores', 'detection_classes']
        c = converter.GraphConverter(args.model_location, output_dir, ['image_tensor'], output_shape, excluded_ops, per_channel=True)

    elif re.match('ssd', args.model) or re.match('mask', args.model) or re.match('fast', args.model) or re.match('rfcn', args.model):
        output_shape=['num_detections', 'detection_boxes', 'detection_scores', 'detection_classes']
        c = converter.GraphConverter(args.model_location, output_dir, ['image_tensor'], output_shape, per_channel=True)

    else:
        c = converter.GraphConverter(args.model_location, output_dir, ['image_tensor'], output_shape, per_channel=True)
    

    c.debug = True
    c.gen_calib_data_cmds = model_callback_cmds(args.data_location, args.model_location)
    c.convert()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', type=str, default='resnet50', help='The model name')
    parser.add_argument('--model_location', type=str, default=None, help='The original fp32 frozen graph')
    parser.add_argument('--data_location', type=str, default=None, help='The dataset in tfrecord format')
    parser.add_argument('--out_graph', type=str, default=None, help='The path to generated output int8 frozen graph.')
    args = parser.parse_args()

    app.run()

