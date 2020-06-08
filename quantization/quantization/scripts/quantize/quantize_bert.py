import os
import argparse

from tensorflow.python.platform import app

import intel_quantization.graph_converter as converter

def model_callback_cmds():
    script = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                          'tensorflow-intelai-models/benchmarks/launch_benchmark.py')
    flags = " --model-name bert" + \
            " --precision fp32" + \
            " --mode inference" + \
            " --framework tensorflow" + \
            " --benchmark-only" + \
            " --input-graph={}" + \
            " --batch-size 1" + \
            " --socket-id 0" + \
            " --num-cores 28" + \
            " --num-intra-threads 28" + \
            " --num-inter-threads 1 "

    return 'python ' + script + flags

def main(_):
    c = None
    if args.model == 'bert_tencent':
        input_shape = 'Placeholder'
        output_shape = 'strided_slice'
    elif args.model == 'bert_official':
        input_shape = ''
        output_shape = ''

    c = converter.GraphConverter(args.model_location, args.out_graph, [input_shape], [output_shape],
                                     per_channel=True)
    c.debug = True
    c.gen_calib_data_cmds = model_callback_cmds()
    c.convert()

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', type=str, default='resnet50', help='The model name')
    parser.add_argument('--model_location', type=str, default=None, help='The original fp32 frozen graph')
    parser.add_argument('--out_graph', type=str, default=None, help='The path to generated output int8 frozen graph.')

    args = parser.parse_args()

    app.run()
