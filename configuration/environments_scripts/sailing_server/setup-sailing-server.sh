#!/bin/bash
# Usage: Launch an Amazon EC2 instance from an Amazon Linux 2023 AMI with
# 100GB of root partition size and the "Sailing Analytics App" security group
# using an SSH key for which you have a working private key available.
# Then, run this script on your local computer, using the external IP address
# of the instance you just launched in AWS as only argument. This will then
# turn the instance into an application server for the SAP Sailing Analytics
# application. When the script is done you may log in to look around and check
# things. When done, shut down the instance (Stop, not Terminate) and create
# an image off of it, naming it, e.g., "SAP Sailing Analytics 2.0" and
# also tagging its root volume snapshot as, e.g., "SAP Sailing Analytics 2.23 (Root)".
# If you want to use the resulting image in production, also tag it with
# tag key "image-type" and tag value "sailing-analytics-server".
if [ $# != 0 ]; then
  SERVER=$1
  scp "${0}" ec2-user@${SERVER}:
  ssh -A ec2-user@${SERVER} ./$( basename "${0}" )
else
  if ec2-metadata | grep -q instance-id; then
    echo "Running on an AWS EC2 instance as user ${USER} / $(whoami), starting setup..."
    # Install standard packages:
    sudo dnf -y --best --allowerasing --releasever=latest upgrade
    sudo dnf -y install git tmux nvme-cli chrony cronie cronie-anacron jq telnet mailx
    # Allow root ssh login with the same key used for the ec2-user for now;
    # later, a cron job will be installed that keeps the /root/authorized_keys file
    # up to date with all landscape managers' public SSH keys
    sudo cp /home/ec2-user/.ssh/authorized_keys /root/.ssh
    sudo chown root /root/.ssh/authorized_keys
    sudo chgrp root /root/.ssh/authorized_keys
    # Copy imageupgrade_function.sh
    scp -o StrictHostKeyChecking=no -p root@sapsailing.com:/home/wiki/gitwiki/configuration/environments_scripts/repo/usr/local/bin/imageupgrade_functions.sh .
    sudo mv imageupgrade_functions.sh /usr/local/bin
    # build-crontab
    . imageupgrade_functions.sh
    # Install MongoDB 5.0 and configure as replica set "replica"
    setup_mongo_7_0_on_AL2023
    sudo su - -c "cat << EOF >>/etc/mongod.conf
replication:
  replSetName: replica
EOF
"
    sudo sed -i -e 's/bindIp: *[0-9]\+\.[0-9]\+\.[0-9]\+\.[0-9]\+/bindIp: 0.0.0.0/' /etc/mongod.conf
    build_crontab_and_setup_files sailing_server
    # Create an SSH key pair with empty passphrase for ec2-user, deploy it to trac@sapsailing.com
    # and then move it to the sailing user's .ssh directory
    setup_keys "sailing_server"
    setup_mail_sending
    setup_fail2ban
    sudo su - sailing -c "mkdir servers"
    # Force acceptance of sapsailing.com's host key:
    sudo su - sailing -c "ssh -o StrictHostKeyChecking=false trac@sapsailing.com ls" >/dev/null
    # Keep Amazon Linux from patching root's authorized_keys file and setup root login:
    setup_cloud_cfg_and_root_login
    # Install SAP JVM 8:
    sudo mkdir -p /opt
    sudo su - -c "source /usr/local/bin/imageupgrade_functions.sh; download_and_install_latest_sap_jvm_8"
    # Increase limits
    sudo su - -c "cat << EOF >>/etc/sysctl.conf
# number of connections the firewall can track
net.ipv4.ip_conntrac_max = 131072
EOF
"
    sudo systemctl daemon-reload
    sudo systemctl enable mountnvmeswap.service
    scp root@sapsailing.com:ssh-key-reader.token /tmp
    sudo mv /tmp/ssh-key-reader.token /root
    sudo chown root /root/ssh-key-reader.token
    sudo chgrp root /root/ssh-key-reader.token
    sudo chmod 600 /root/ssh-key-reader.token
    # Install /etc/init.d/sailing start-up / shut-down service
    sudo systemctl daemon-reload
    sudo systemctl enable sailing.service
    # Install secrets
    scp root@sapsailing.com:secrets /tmp
    scp root@sapsailing.com:mail.properties /tmp
    sudo mv /tmp/secrets /root
    sudo mv /tmp/mail.properties /root
    sudo chown root /root/secrets
    sudo chgrp root /root/secrets
    sudo chmod 600 /root/secrets
    sudo chown root /root/mail.properties
    sudo chgrp root /root/mail.properties
    sudo chmod 600 /root/mail.properties
    # Create some swap space for the case mountnvmeswap hasn't created any
    setup_swap 6000
    setup_sshd_resilience
    # Add the NFS mount of /home/scores to /etc/fstab:
    sudo mkdir /home/scores
    sudo su - -c 'echo "logfiles.internal.sapsailing.com:/home/scores       /home/scores    nfs     tcp,intr,timeo=100,retry=0" >>/etc/fstab'
    sudo swapon -a
  else
    echo "Not running on an AWS instance; refusing to run setup!" >&2
    echo "To prepare an instance running in AWS, provide its external IP as argument to this script." >&2
    exit 2
  fi
fi
