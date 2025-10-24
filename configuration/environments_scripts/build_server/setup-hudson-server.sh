#!/bin/bash

# Usage: Launch an Amazon EC2 t3.medium instance from an Amazon Linux 2023 AMI with
# 30GB of root partition size and the "Sailing Analytics App" security group
# using an SSH key for which you have a working private key available.
# Launch it in the same Available Zone (AZ) as the current Build/DEV server
# because you want to move the /home/hudson volume from the old to the new
# instance.
# Then, run this script on your local computer, using the external IP address
# of the instance you just launched in AWS as only argument. This will then
# turn the instance into an application server for the SAP Sailing Analytics
# application. When the script is done you may log in to look around and check
# things, such as the landscape managers keys, aws credentials, and that 
# the necessary scripts are in /usr/local/bin.
if [ $# != 0 ]; then
  SERVER=$1
  $(dirname $0)/../sailing_server/setup-sailing-server.sh ${SERVER}
  scp "${0}" ec2-user@${SERVER}:
  ssh -A ec2-user@${SERVER} ./$( basename "${0}" )
else
  if ec2-metadata | grep -q instance-id; then
    echo "Running on an AWS EC2 instance as user ${USER} / $(whoami), starting setup..."
    # Install secrets
    scp root@sapsailing.com:dev-secrets /tmp
    sudo mv /tmp/dev-secrets /root/secrets
    sudo chown root:root /root/secrets
    sudo chmod 600 /root/secrets
    . imageupgrade_functions.sh
    if ! build_crontab_and_setup_files build_server; then
        exit 1
    fi
    # Install "xq" which is used by a hudson cron job to clean up workspaces of disabled jobs:
    curl -sSL https://bit.ly/install-xq | sudo bash
    # Make eu-west-1 the default region for any aws CLI interaction:
    sudo su - -c "aws configure set default.region eu-west-1"
    # Clear "hudson" user's directory again which is to become a mount point
    sudo su - hudson -c "rm -rf /home/hudson/* /home/hudson/.* 2>/dev/null"
    sudo mkdir /usr/lib/hudson
    sudo chown hudson /usr/lib/hudson
    sudo mkdir /var/log/hudson
    sudo chgrp hudson /var/log/hudson
    sudo chmod g+w /var/log/hudson
    sudo wget -O /usr/lib/hudson/hudson.war "https://static.sapsailing.com/hudson.war.patched-with-mail-1.6.2"
    # Enable NFS server
    sudo systemctl enable nfs-server.service
    sudo systemctl start nfs-server.service
    # Enable the mongod.service on which the sailing service depends:
    sudo systemctl enable mongod.service
    sudo systemctl start mongod.service
    # Enable the service:
    sudo systemctl daemon-reload
    sudo systemctl enable hudson.service
    # NFS-export Android SDK
    sudo su - -c "cat <<EOF >>/etc/exports
/home/hudson/android-sdk-linux 172.31.0.0/16(rw,nohide,no_root_squash)
EOF
"
    # Install DEV server
    sudo su - sailing -c "mkdir /home/sailing/servers/DEV
cd /home/sailing/servers/DEV
cat <<EOF | /usr/local/bin/refreshInstance.sh auto-install-from-stdin
USE_ENVIRONMENT=dev-server
EOF
"
    sudo cp /root/secrets /home/sailing/servers/DEV/configuration
    sudo chown sailing /home/sailing/servers/DEV/configuration/secrets
    sudo chgrp sailing /home/sailing/servers/DEV/configuration/secrets
    sudo cp /root/mail.properties /home/sailing/servers/DEV/configuration
    sudo chown sailing /home/sailing/servers/DEV/configuration/mail.properties
    sudo chgrp sailing /home/sailing/servers/DEV/configuration/mail.properties
    # Start the sailing.service with empty/no user data, so the next boot is recognized as a re-boot
    sudo systemctl start sailing.service
    sudo mount -a
    echo "Now follow the instructions on the wiki for creating-ec2-image-for-hudson-from-scratch.md. It explains how to mount"
    echo "the hudson volume and setup the aws + ssh keys."
    echo "See https://wiki.sapsailing.com/wiki/info/landscape/creating-ec2-image-for-hudson-from-scratch.md"
  else
    echo "Not running on an AWS instance; refusing to run setup!" >&2
    echo "To prepare an instance running in AWS, provide its external IP as argument to this script." >&2
    exit 2
  fi
fi
