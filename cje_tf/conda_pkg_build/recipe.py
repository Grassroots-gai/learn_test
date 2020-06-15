# Author: Aditya Kalari @ Intel Corporation
# Date: 25 June 2019

# TODO: Create loops for lots of ugly package code

import os
import subprocess
import sys
# Check if package info is installed
try:
    import pkginfo
except ImportError:
    print ('Requires pkginfo to read dependencies. Now installing through pip.')
    os.system('pip install pkginfo')

wheel_path = os.environ.get('TF_WHL')
deps = os.popen('pkginfo -f \'requires_dist\' ' + wheel_path).read()

# Clean up deps_list
deps = deps[16:]
deps = deps[:-2]
deps = deps.split('\'')
deps[:] = [x for x in deps if x != ', ']
deps = deps[1:]
deps = deps[:-1]
# Append pip, python
deps.insert(0, 'pip')
deps.insert(0, 'python')

# Gather METADATA into dictionary
metadata = {
    'name': '',
    'version': '',
    'summary': '',
    'home_page': '',
    'license': ''
}
for item in metadata:
    length = len(item)
    metadata[item] = os.popen('pkginfo -f ' + '\'' + item + '\' ' + wheel_path).read()
    metadata[item] = metadata[item][length+2:]
    metadata[item] = metadata[item][:-1]
metadata['home'] = metadata.pop('home_page')

# Create file meta.yaml in named directory
if not os.path.exists(metadata['name']):
    os.makedirs(metadata['name'])
try:
    os.remove(metadata['name']+'/metadata.yaml')
except OSError:
    pass
writeyaml = open(metadata['name'] + '/meta.yaml', 'w+')
# TODO: Modify length of tab escape sequence
tab = '  '
# Header
writeyaml.write('{% set name = \'' + metadata['name'] + '\' %}')
writeyaml.write('\n')
writeyaml.write('{% set version  = \'' + metadata['version'] + '\' %}')
writeyaml.write('\n')
writeyaml.write('{% set buildnumber = 0 %}')
writeyaml.write('\n\n')
# Package
writeyaml.write('package:')
writeyaml.write('\n')
writeyaml.write(tab + 'name: {{name}}')
writeyaml.write('\n')
writeyaml.write(tab + 'version: {{version}}')
writeyaml.write('\n\n')
# Build
writeyaml.write('build:')
writeyaml.write('\n')
writeyaml.write(tab + 'script_env:')
writeyaml.write('\n')
writeyaml.write(tab + tab + '- TF_WHL')
writeyaml.write('\n')
writeyaml.write(tab + 'number: {{buildnumber}}')
writeyaml.write('\n')
writeyaml.write(tab + 'script: pip install --nodeps {{TF_WHL}}')
writeyaml.write('\n\n')
# Requirements
writeyaml.write('requirements:')
writeyaml.write('\n')
writeyaml.write(tab + 'build:')
writeyaml.write('\n')
writeyaml.write(tab + tab + '- python')
writeyaml.write('\n')
writeyaml.write(tab + tab + '- pip')
writeyaml.write('\n')
writeyaml.write(tab + 'run:')
writeyaml.write('\n')
for dependency in deps:
    writeyaml.write(tab + tab + '- ' + dependency)
    writeyaml.write('\n')
writeyaml.write('\n')
writeyaml.write(tab + 'test:')
writeyaml.write('\n')
writeyaml.write(tab + 'imports:')
writeyaml.write('\n')
writeyaml.write(tab + tab + '- ' + metadata['name'])
writeyaml.write('\n\n')
# Summary : TODO: Loop this ugly code
writeyaml.write('about:')
writeyaml.write('\n')
writeyaml.write(tab + 'home: ' + metadata['home'])
writeyaml.write('\n')
writeyaml.write(tab + 'license: ' + metadata['license'])
writeyaml.write('\n')
family = metadata['license'].split(' ')
writeyaml.write(tab + 'license_family: ' + family[0])
writeyaml.write('\n')
writeyaml.write(tab + 'summary: ' + metadata['summary'])
writeyaml.write('\n')
# TODO: Parameterize doc_url, dev_url, description, doc_source_url
description = 'TensorFlow provides multiple APIs.The lowest level API, TensorFlow Core provides you with complete programming control.'
writeyaml.write(tab + 'description: ' + description)
writeyaml.write('\n')
dev_url = 'https://github.com/tensorflow/tensorflow'
writeyaml.write(tab + 'dev_url: ' + dev_url)
writeyaml.write('\n')
doc_url = 'https://www.tensorflow.org/get_started/get_started'
writeyaml.write(tab + 'doc_url: ' + doc_url)
writeyaml.write('\n')
doc_source_url = 'https://github.com/tensorflow/tensorflow/tree/master/tensorflow/docs_src'
writeyaml.write(tab + 'doc_source_url: ' + doc_source_url)