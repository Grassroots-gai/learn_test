// Dynamic function post2AIBTdashboard(channel, buildStatus, msg)
//
// Parameters:
//
//     runtype       posting data type, currently tfdo-inference 
//
//     framework     framework name, use tensorflow-do for now
//
//     frontend      framework name, use tensorflow-do for now
//
//     target        target server: expr server or production server 
//
//     logsdir       the log directory 
//
//     logfile       the log file name 
//    
//     network       the network model name
//
// Returns: nothing
//
// External dependencies: None

def call(String runtype, \
         String framework, \
         String frontend, \
         String target = 'default', 
         String logsdir, 
         String logfile, 
         String network, \
         String datatype) {

    sh '''
        ls -al ${logsdir}
        python $WORKSPACE/cje-algo/post2dashboard/util/hardware_info.py --output ./hw_info.json
    '''

    String workdir = "${env.WORKSPACE}/cje-algo/post2dashboard"
    String setupScript = "${workdir}/setup.sh"

    withEnv(["workdir=${workdir}", "setupScript=${setupScript}"]) {

        sh '''#!/bin/bash -x
            export WORKDIR=$workdir
            source $setupScript
            deactivate
        '''
    }

    upload2dashboard(runtype, framework, frontend, logsdir, logfile, workdir, target, network, datatype )

}

def upload2dashboard(String runtype, \
                     String framework, \
                     String frontend, \
                     String logsdir, \
                     String logfile, \
                     String workdir, \
                     String target, \
                     String network, \
                     String datatype) {

    try {

        echo "uploading to dashboard results for $runtype"

        // model names from Jenkin jobs map to "NETWORK MODEL" on the dashboard
        Map network_map = [
            "3DGAN":                   "3DGAN",                    // Q2 fp32
            "3DUNet":                  "3D_UNet",                  // Q2 fp32
            "A3C":                     "A3C",                      // Q2 fp32
            "deepSpeech":              "deepspeech_mozilla",       // Q2 fp32
            "DenseNet":                "DenseNet",                 // Q2 fp32
            "DenseNet169":             "densenet_169",             // Q4 fp32
            "DCGAN":                   "DCGAN",                    // Q2 fp32
            "DRAW":                    "DRAW",                     // Q2 fp32
            "FaceNet":                 "FaceNet",                  // Q4 fp32
            "fastrcnn":                "FasterRCNN",               // Q2 fp32
            "FaceNet":                 "FaceNet",                  // Q4 fp32
            "FastRCNN":                "FasterRCNN",               // Q3 int8
            "gnmt":                    "gnmt_8",                   // Q2 fp32
            "inception_resnet_v2":     "Inception_ResNet_v2",      // Q2 fp32, only posting intel-models fp32/int8
            "InceptionResNetV2":       "Inception_ResNet_v2",      // Q3 int8
            "inceptionv3":             "inception_v3",             // Q2 fp32, intel-models
            "InceptionV3":             "inception_v3",             // Q3 int8
            "inception_v4":            "inception_v4",             // Q2 fp32
            "InceptionV4":             "inception_v4",             // Q4 int8
            "inceptionv4":             "inception_v4",             // intel-models 
            "MaskRCNN":                "MaskRCNN",                 // Q2 fp32
            "maskrcnn":                "MaskRCNN",                 // intel-models
            "mobilenet_v1":            "mobilenet_v1",             // Q2 fp32
            "mobilenetv1":             "mobilenet_v1",             // Q3 int8
            "MTCC":                    "MTCC",                     // Q4 fp32
            "NCF":                     "ncf",                      // Q3 fp32
            "ncf":                     "ncf",                      // intel-models 
            "resnet50":                "resnet_50_v1",             // Q2 fp32, intel-models
            "resnet101":               "resnet_101_v1",            // intel-models 
            "ResNet50":                "resnet_50_v1",             // Q3 int8
            "ResNet101":               "resnet_101_v1",            // Q4 fp32, Q4 int8
            "rfcn":                    "RFCN",                     // Q2 fp32
            "RFCN":                    "RFCN",                     // Q3 int8
            "SqueezeNet":              "SqueezeNet",               // Q2 fp32
            "SSDMobilenet":            "SSD_Mobilenet_v1",         // Q3 int8
            "ssd-mobilenet":           "SSD_Mobilenet_v1",         // intel-models 
            "SSDvgg16":                "ssd_vgg16",                // Q2 fp32
            "SSD-VGG16":               "ssd_vgg16",                // Q3 int8
            "ssd_vgg16":               "ssd_vgg16",                // intel-models 
            "transformerLanguage":     "transformer_language",     // Q2 fp32
            "transformerSpeech":       "transformer_speech",       // Q2 fp32
            "transformer_language":    "transformer_language",     // intel-models 
            "UNet":                    "UNet",                     // Q2 fp32
            "vgg16":                   "vgg16",                    // Q2 fp32
            "YoloV2":                  "Yolo_v2",                  // Q2 fp32, Q3 int8
            "wideDeep":                "wide_and_deep",            // Q2 fp32
            "wide_deep":               "wide_and_deep",            // intel-models 
            "WaveNet":                 "WaveNet",                  // Q2 fp32
            "WaveNet_Magenta":         "WaveNet_Magenta"           // Q2 fp32
        ]

        // model names from Jenkin jobs map to "DATASET" names on the dashboard
        Map dataset_map = [
            "3DGAN":                   "ele",
            "3DUNet":                  "brats",
            "A3C":                     "realtime_pong",
            "deepSpeech":              "LibriSpeech",
            "DenseNet":                "NoDataLayer",
            "DenseNet169":             "Imagenet",
            "DCGAN":                   "Cifar10Data",
            "DRAW":                    "MnistData",
            "FaceNet":                 "LFW",
            "fastrcnn":                "coco",
            "FastRCNN":                "coco",
            "gnmt":                    "wmt16_ger_eng",
            "inception_resnet_v2":     "Imagenet",
            "InceptionResNetV2":       "Imagenet",
            "inceptionv3":             "NoDataLayer",
            "InceptionV3":             "NoDataLayer",
            "inception_v4":            "Imagenet",
            "inceptionv4":             "Imagenet",
            "InceptionV4":             "Imagenet",
            "MaskRCNN":                "coco",
            "maskrcnn":                "coco",
            "mobilenet_v1":            "Imagenet",
            "mobilenetv1":             "Imagenet",
            "MTCC":                    "LFW",                     
            "NCF":                     "NoDataLayer",
            "ncf":                     "NoDataLayer",
            "resnet50":                "NoDataLayer",
            "ResNet50":                "NoDataLayer",
            "resnet101":               "Imagenet",
            "ResNet101":               "Imagenet",
            "rfcn":                    "coco",
            "RFCN":                    "NoDataLayer",
            "SqueezeNet":              "Imagenet",
            "SSDMobilenet":            "coco", 
            "ssd-mobilenet":           "coco", 
            "SSDvgg16":                "VOC2007",
            "SSD-VGG16":               "VOC2007",
            "ssd_vgg16":               "coco",
            "transformerLanguage":     "wmt16_ger_eng",
            "transformerSpeech":       "LibriSpeech",
            "transformer_language":    "wmt16_ger_eng",
            "UNet":                    "NoDataLayer",
            "vgg16":                   "NoDataLayer",
            "YoloV2":                  "VOC2007",
            "wideDeep":                "census_income",
            "wide_deep":               "census_income",
            "WaveNet":                 "NoDataLayer",
            "WaveNet_Magenta":         "NoDataLayer",

        ]

        String hwinfofile = "${env.WORKSPACE}/hw_info.json"
        String summaryfile = "None"
        //String dataset = "unknown"

        postmodeldata(hwinfofile, logfile, summaryfile, network_map.get(network), dataset_map.get(network), frontend, framework, target, logsdir, workdir, runtype, datatype)

    } catch(e) {

        echo "======================================================================================="
        echo "ERROR: Exception caught in module which uploads results to dashboard - upload2dashboard"
        echo "ERROR: ${e}"
        echo "======================================================================================="

    }  // catch

}  //  def

// The following will post results to the dashboard
def postmodeldata(String hwinfofile, \
                  String logfile, \
                  String summaryfile, \
                  String network, \
                  String dataset, \
                  String frontend, \
                  String framework, \
                  String target, \
                  String logsdir, \
                  String workdir, \
                  String runtype, \
                  String datatype) {

    try {

        //collect all the logs required to run
        withEnv(["HWINFOFILE=$hwinfofile", \
                 "LOGFILE=$logfile", \
                 "SUMMARYFILE=$summaryfile", \
                 "NETWORK=$network", \
                 "DATASET=$dataset", \
                 "FRONTEND=$frontend", \
                 "FRAMEWORK=$framework", \
                 "TARGET=$target", \
                 "LOGSDIR=$logsdir", \
                 "WORKDIR=$workdir", \
                 "RUNTYPE=$runtype", \
                 "DATATYPE=$datatype"]) {

            sh '''#!/bin/bash -x

                export WORKDIR=$WORKDIR
                . $WORKDIR/.venv3/bin/activate
                export PYTHONPATH=$WORKDIR/parser/tfdo_inference:$WORKDIR/parser/tfdo:$WORKDIR/util:$WORKDIR/benchmarks-src:$WORKDIR/parser:$WORKDIR/sender:$WORKDIR

                if [ $SUMMARYFILE == "None" ]; then
                    python $WORKDIR/postJson.py --hwinfofile $HWINFOFILE \
                                            --runtype $RUNTYPE \
                                            --logfile $LOGFILE \
                                            --network $NETWORK \
                                            --dataset $DATASET \
                                            --frontend $FRONTEND \
                                            --framework $FRAMEWORK \
                                            --target $TARGET \
                                            --keep-temp \
                                            --datatype $DATATYPE
                else
                    python3 $WORKDIR/postJson.py --hwinfofile $HWINFOFILE \
                                            --runtype $RUNTYPE \
                                            --logfile $LOGFILE \
                                            --summaryfile $SUMMARYFILE \
                                            --network $NETWORK \
                                            --dataset $DATASET \
                                            --frontend $FRONTEND \
                                            --framework $FRAMEWORK \
                                            --target $TARGET \
                                            --keep-temp
                                            --datatype $DATATYPE
                fi

                echo 'check the dashboard for the data that is posted:'
                echo 'expr: https://exp-avus.apps1-fm-int.icloud.intel.com'
                echo 'prod: https://avus.apps1-fm-int.icloud.intel.com'
                deactivate

                '''
        }

    } catch (Exception e) {
        echo 'ERROR: Post to dashboard failed'
        echo 'exception occurred e.getMessage()'
    }

} //end

return this;

