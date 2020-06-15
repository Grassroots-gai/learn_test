def call() {

        sh '''#!/bin/bash -x
        sudo sh -c 'sync; echo 1 > /proc/sys/vm/compact_memory; echo 1 > /proc/sys/vm/drop_caches' || true
        export PATH="/nfs/fm/disks/aipg_tensorflow_tools/gcc6.3/bin/:/nfs/fm/disks/aipg_tensorflow_tools/bazel/bin:$PATH"
        virtualenv -p /usr/bin/python $WORKSPACE/venv
        source $WORKSPACE/venv/bin/activate
        sudo touch /usr/include/stropts.h
        pip install -I subprocess32 'backports.weakref >= 1.0rc1' 'enum34 >= 1.1.6' autograd 'bleach >= 1.5.0' enum funcsigs 'future >= 0.17.1' futures grpc gevent 'grpcio >= 1.8.6' html5lib Markdown 'mock >= 2.0.0' msgpack-python 'numpy >= 1.14.5, < 2.0' pbr pip portpicker 'protobuf >= 3.6.1' scikit-learn 'scipy >= 0.15.1' setuptools 'six >= 1.10.0' 'tensorboard >= 1.13.0, < 1.14.0' Werkzeug 'wheel >= 0.26' h5py matplotlib opencv-python 'keras_preprocessing >= 1.0.5'
        '''
}

return this;
