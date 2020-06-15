#!/bin/bash

echo "Testing ${DOCKER_IMAGE}"

echo "DOCKER_TEST_DIR=${DOCKER_TEST_DIR}"
echo "TEST_SCRIPT=${TEST_SCRIPT}"

test_output=$(docker run -v ${DOCKER_TEST_DIR}:/root/scripts ${DOCKER_IMAGE} /bin/bash /root/scripts/${TEST_SCRIPT})

echo ${test_output}

num_tests_failed=$(echo $test_output | grep "FAILED" | wc -l)
num_tests_passed=$(echo $test_output | grep "PASS" | wc -l)

echo "Tests failed: $num_tests_failed"
echo "Tests passed: $num_tests_passed"

# Error if any tests failed
if [ ${num_tests_failed} -ge 1 ]
then
    exit 1
fi