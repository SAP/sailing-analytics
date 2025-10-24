#!/bin/bash

# PART 2
# Assumes to be run after or be invoked by setup-central-reverse-proxy.sh which
# is assumed to have prepared content for user home folders in /root/temporary_home_copy,
# has set up the software packages, and that the user has mounted the volumes with their
# original content to /home, /var/log, et cetera.
#
# Call as follows:
#    setup-central-reverse-proxy-part-2.sh {external-ip-of-new-instance} {image-type}
# where {image-type} identifies the "environment" from configuration/environments_scripts
# which usually would be "central_reverse_proxy" here, passed in from the previous stage's
# script.
if [[ "$#" -ne 2 ]]; then
    echo "2 arguments required. Check comment of scripts for more details."
    exit 2
fi
IP=$1
IMAGE_TYPE="$2"
TEMPORARY_HOME_COPY_LOCATION="/root/temporary_home_copy" # home nested within this.
ssh -A "root@${IP}" "bash -s" << EOF
sudo systemctl start crond.service
sudo systemctl start tmux-management-panel.service
. imageupgrade_functions.sh
cp -r "$TEMPORARY_HOME_COPY_LOCATION"/home /
rm -rf "$TEMPORARY_HOME_COPY_LOCATION"
# Localhost works here as we are logged on as root and are using ssh agent forwarding.
if ! build_crontab_and_setup_files -h localhost -f "${IMAGE_TYPE}"; then # files have already been copied so -f is used.
    exit 1
fi
setup_keys -p "${IMAGE_TYPE}"
chown trac:static /var/www/static
# setup nfs
systemctl enable nfs-server
echo "/var/log/old 172.31.0.0/16(rw,nohide,no_root_squash)
/home/scores 172.31.0.0/16(rw,nohide,no_root_squash)" >>/etc/exports
systemctl start nfs-server
scp -p -o StrictHostKeyChecking=no -r root@sapsailing.com:/etc/ssh /etc
# append hostname to sysconfig
echo "HOSTNAME=sapsailing.com" >> /etc/sysconfig/network
sed -i "s/\(127.0.0.1 *\)/\1 sapsailing.com /" /etc/hosts
hostname sapsailing.com
hostnamectl set-hostname sapsailing.com
if sshd -t; then
    systemctl restart sshd
fi
EOF
ssh -o StrictHostKeyChecking=no -A -f root@"$IP" "cd /var/log/old/cache/docker/registry && nohup docker-compose up &>/dev/null &" &> /dev/null
echo "PLEASE READ ALL OF THE FOLLOWING..."
echo "Remove the existing central reverse proxy from the SSH target groups."
echo "When ready, press a key to continue, which will run the script target-group-tag-route53-nfs-elasticIP-setup.sh."
echo "This configures the EC2 instance tags, adds to the "
echo "necessary target groups, modifies a few records in route53 (logs.internal.sapsailing.com"
echo "and smtp.internal.sapsailing.com), remounts those instances that are dependent on this, and sets the elastic IP."
echo "You will need to have the AWS CLI installed and have credentials that allow you to run all the commands in the script."
echo "Make sure you have an active session token in your shell's environment, e.g., obtained"
echo "through the awsmfalogon.sh script. The script requires that you have jq and host installed locally."
echo "If you haven't already authenticated, then stop this script; authenticate manually;"
echo "and then run the script named above with the remote IP as a parameter."
read -n 1 -p "Press a key to continue" key_pressed
"$(dirname $0)"/target-group-tag-route53-nfs-elasticIP-setup.sh