sed -i "s/\s\+nfs\s\+rsize=32768,wsize=32768,async,auto/ nfs sync,vers=4.0,auto/" /etc/fstab
umount -fl /home /opt
reboot
