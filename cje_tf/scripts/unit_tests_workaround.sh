#/bin/sh
set -x

rm -rf tensorflow/lite/

sed 's/.*lite.*/#&/' tensorflow/BUILD > xtemp.$$
mv xtemp.$$ tensorflow/BUILD 

sed 's/.*contrib\/lite.*/#&/' tensorflow/contrib/BUILD > xtemp.$$
mv xtemp.$$ tensorflow/contrib/BUILD 

sed 's/.*lite.*/#&/' tensorflow/tools/pip_package/BUILD > xtemp.$$
mv xtemp.$$ tensorflow/tools/pip_package/BUILD

sed -e 's/0x1.0P-20/0.000001/' -e 's/0x1.0P-5/0.031250/' tensorflow/compiler/xla/service/hlo_evaluator_test.cc > xtemp.$$
mv xtemp.$$ tensorflow/compiler/xla/service/hlo_evaluator_test.cc
 

