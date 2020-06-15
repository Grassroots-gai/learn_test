#!/usr/bin/env bash
# install python version passed in as a parameter

set -e

if [[ $# -ne 1 ]]; then
  echo "num params = $#"
  echo "Usage: $0 <python version suffix (e.g. 3.5)>"
  exit 1
fi

PYTHON_VERSION="${1}"
PIP="pip${1}"
echo "Installing python ${PYTHON_VERSION}..."

if [[ "${PYTHON_VERSION}" == "3" || "${PYTHON_VERSION}" == "3.5" ]]; then 
    /install/install_pip_packages.sh
    ln -sf /usr/bin/python3.5 /usr/bin/python
elif [[ "${PYTHON_VERSION}" == "3.6" || "${PYTHON_VERSION}" == "3.7" || "${PYTHON_VERSION}" == "3.8" ]]; then
    apt-get update -y && apt-get install -y build-essential checkinstall \
    git libfreetype6-dev libhdf5-dev openjdk-8-jdk \
    libcurl4-openssl-dev libpng12-dev libssl-dev unzip wget libzmq-dev \
    zlib1g-dev libbz2-dev gcc libffi-dev tar
    mkdir -p /usr/src && cd /usr/src
    if [[ "${PYTHON_VERSION}" == "3.6" ]]; then
      wget --quiet https://www.python.org/ftp/python/3.6.8/Python-3.6.8.tgz
      tar xzf Python-3.6.8.tgz
      cd Python-3.6.8
      rm /usr/src/Python-3.6.8.tgz
      ln -sf /usr/src/Python-3.6.8/python /usr/bin/python
    elif [[ "${PYTHON_VERSION}" == "3.7" ]]; then
      wget --quiet https://www.python.org/ftp/python/3.7.4/Python-3.7.4.tgz
      tar xzf Python-3.7.4.tgz
      cd Python-3.7.4
      rm /usr/src/Python-3.7.4.tgz
      # force create the link before the target exists
      ln -sf /usr/src/Python-3.7.4/python /usr/bin/python
    else
      wget --quiet https://www.python.org/ftp/python/3.8.2/Python-3.8.2.tgz
      tar xzf Python-3.8.2.tgz
      cd Python-3.8.2
      rm /usr/src/Python-3.8.2.tgz
      ln -sf /usr/src/Python-3.8.2/python /usr/bin/python
    fi
    ./configure --enable-optimizations
    make altinstall
    wget --quiet https://bootstrap.pypa.io/get-pip.py
    python${PYTHON_VERSION} get-pip.py
    rm get-pip.py
    ${PIP} --no-cache-dir install --upgrade matplotlib numpy
    ln -s "/usr/include/x86_64-linux-gnu/python${PYTHON_VERSION}m" "/dt7/usr/include/x86_64-linux-gnu/python${PYTHON_VERSION}m"
    ln -s "/usr/include/x86_64-linux-gnu/python${PYTHON_VERSION}m" "/dt8/usr/include/x86_64-linux-gnu/python${PYTHON_VERSION}m"
else
    echo "Unsupported Python version requested: ${PYTHON_VERSION} Exiting."
    exit 1
fi
