#!/bin/bash

cd /mnt/local_disk/dataset/imagenet

ls -l

mv img img_raw
mv img_256 img_256_resize
ln -s img_256_resize img_256
ln -s img_256 img

mv lmdb_compressed lmdb_256_resize_compressed
ln -s lmdb_256_resize_compressed lmdb_compressed
ln -s lmdb_compressed lmdb
mv lmdb_320_compressed lmdb_320_resize_compressed

echo "======="
ls -l 
