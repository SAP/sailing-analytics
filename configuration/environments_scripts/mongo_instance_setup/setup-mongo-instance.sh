#!/bin/bash
# Usage: Launch an Amazon EC2 instance from an Amazon Linux 2023 AMI with
# 8GB of root partition size and the "MongoDB Replica Set" security group
# using an SSH key for which you have a working private key available.
# Then, run this script on your local computer, using the external IP address
# of the instance you just launched in AWS as only argument. This will then
# turn the instance into a MongoDB replica set node.
# When the script is done you may log in to look around and check
# things. When done, shut down the instance (Stop, not Terminate) and create
# an image off of it, naming it, e.g., "MongoDB Live Replica Set NVMe 2.6" and
# also tagging its root volume snapshot as, e.g., "MongoDB Live Replica Set NVMe 2.6 (Root)".
# If you want to use the resulting image in production, also tag it with
# tag key "image-type" and tag value "mongodb-server".
if [ $# != 0 ]; then
  SERVER=$1
  scp "${0}" ec2-user@${SERVER}:
  ssh -A ec2-user@${SERVER} ./$( basename "${0}" )
else
  if ec2-metadata | grep -q instance-id; then
    echo "Running on an AWS EC2 instance as user ${USER} / $(whoami), starting setup..."
    # Install standard packages:
    sudo dnf -y --best --allowerasing --releasever=latest upgrade
    sudo dnf -y install nvme-cli chrony cronie cronie-anacron jq mailx whois iptables
    # Copy imageupgrade_function.sh
    scp -o StrictHostKeyChecking=no -p root@sapsailing.com:/home/wiki/gitwiki/configuration/environments_scripts/repo/usr/local/bin/imageupgrade_functions.sh .
    sudo mv imageupgrade_functions.sh /usr/local/bin
    # build-crontab
    . imageupgrade_functions.sh
    build_crontab_and_setup_files mongo_instance_setup
    # Install MongoDB 5.0 and configure as replica set "live"
    setup_mongo_7_0_on_AL2023
    setup_mail_sending
    setup_fail2ban
    # obtain root SSH key from key vault:
    setup_keys "mongo_instance_setup"
    scp root@sapsailing.com:ssh-key-reader.token /tmp
    sudo mv /tmp/ssh-key-reader.token /root
    sudo chown root:root /root/ssh-key-reader.token
    sudo chmod 600 /root/ssh-key-reader.token
    setup_sshd_resilience
  else
    echo "Not running on an AWS instance; refusing to run setup!" >&2
    echo "To prepare an instance running in AWS, provide its external IP as argument to this script." >&2
    exit 2
  fi
fi
