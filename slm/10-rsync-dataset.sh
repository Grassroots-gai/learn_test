#!/bin/bash

IP=$1
if [[ -z $IP ]]; then 
	IP=1
fi

echo $IP

mkdir -p /mnt/local_disk/dataset/imagenet/
#rsync -av --progress root@192.169.30.${IP}:/mnt/local_disk/dataset/imagenet/lmdb /mnt/local_disk/dataset/imagenet/
#rsync -av --progress root@192.169.30.${IP}:/mnt/local_disk/dataset/imagenet/lmdb_compressed /mnt/local_disk/dataset/imagenet/
#rsync -av --progress root@192.169.30.${IP}:/mnt/local_disk/dataset/imagenet/lmdb_320_compressed /mnt/local_disk/dataset/imagenet/

#rsync -a root@192.169.30.1:/mnt/local_disk/dataset/imagenet/img /mnt/local_disk/dataset/imagenet/
#rsync -a root@192.169.30.${IP}:/mnt/local_disk/dataset/imagenet/img_256 /mnt/local_disk/dataset/imagenet/

#rsync -a root@192.169.30.${IP}:/mnt/local_disk/dataset/VOC /mnt/local_disk/dataset/


rsync -av --progress root@192.169.10.${IP}:/mnt/local_disk/dataset/imagenet/lmdb_compressed /mnt/local_disk/dataset/imagenet/
rsync -a root@192.169.10.${IP}:/mnt/local_disk/dataset/imagenet/img_256 /mnt/local_disk/dataset/imagenet/
rsync -a root@192.169.10.${IP}:/mnt/local_disk/dataset/VOC /mnt/local_disk/dataset/
