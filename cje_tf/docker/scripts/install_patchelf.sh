#!/usr/bin/env bash

#install patchelf
echo "Installing patchelf..."
patchelf_location=$(which patchelf)
if [[ -z "$patchelf_location" ]]; then
  # Install patchelf from source (it does not come with trusty package)
  wget https://nixos.org/releases/patchelf/patchelf-0.9/patchelf-0.9.tar.bz2
  tar xfa patchelf-0.9.tar.bz2
  rm patchelf-0.9.tar.bz2
  cd patchelf-0.9
  ./configure --prefix=/usr/local
  make
  make install
fi
cd ..

