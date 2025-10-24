#!/bin/bash

# Setup script for Amazon Linux 2023. Use 50g root partition.
# May need to update macro definitions for the archive IP.
# Parameter 1 is the IP and parameter 2 is the bearer token to be installed in the root home dir.
# Ensure that the security for requesting the metadata uses IMDSv1
if [[ "$#" -ne 2 ]]; then
    echo "Incorrect number of args (2 required). See script header for further details."
    exit 2
fi
IP=$1
BEARER_TOKEN=$2
IMAGE_TYPE="reverse_proxy"
HTTP_LOGROTATE_ABSOLUTE=/etc/logrotate.d/httpd
ssh -A "ec2-user@${IP}" "bash -s" << FIRSTEOF 
# Correct authorized keys. May not be necessary if update_authorized_keys is running.
sudo su - -c "cat ~ec2-user/.ssh/authorized_keys > /root/.ssh/authorized_keys"
FIRSTEOF
# writes std error to local text file
ssh -A "root@${IP}" "bash -s" << SECONDEOF  >stdoutLog.txt
# fstab setup
mkdir /var/log/old
echo "logfiles.internal.sapsailing.com:/var/log/old   /var/log/old    nfs     tcp,intr,timeo=100,retry=0" >> /etc/fstab
mount -a
# update instance
dnf upgrade -y --best --allowerasing --releasever=latest
dnf install -y httpd mod_proxy_html tmux nfs-utils git whois jq cronie iptables nmap
sudo systemctl enable crond.service
# setup other users and crontabs to keep repo updated
cd /root
scp -o StrictHostKeyChecking=no -p "root@sapsailing.com:/home/wiki/gitwiki/configuration/environments_scripts/repo/usr/local/bin/imageupgrade_functions.sh" /usr/local/bin
# Setup root user and apache user with the right keys.
. imageupgrade_functions.sh
setup_keys "${IMAGE_TYPE}"
setup_cloud_cfg_and_root_login
setup_swap 5000
# setup files and crontab for the required users, both dependent on the environment type.
build_crontab_and_setup_files "${IMAGE_TYPE}"
# setup mail
setup_mail_sending
# setup sshd config
setup_sshd_resilience
systemctl reload sshd.service
cd /usr/local/bin
echo $BEARER_TOKEN > /root/ssh-key-reader.token
# add basic test page which won't cause redirect error code if used as a health check.
cat <<EOF > /var/www/html/index.html
<!DOCTYPE html><html lang="en"><head><title>Health check</title><meta charset="UTF-8"></head><body><h1>Test page</h1></body></html>
EOF
# ensure httpd starts on startup
systemctl enable httpd
echo "net.ipv4.ip_conntrac_max = 131072" >> /etc/sysctl.conf
# setup fail2ban
setup_fail2ban
# goaccess and apachetop
setup_goaccess
setup_apachetop
# setup logrotate.d/httpd 
mkdir /var/log/logrotate-target
echo "Patching $HTTP_LOGROTATE_ABSOLUTE so that old logs go to /var/log/old/$IP" >>/var/log/sailing.out
mkdir --parents "/var/log/old/REVERSE_PROXIES/${IP}"
sed -i  "s|/var/log/old|/var/log/old/REVERSE_PROXIES/${IP}|" $HTTP_LOGROTATE_ABSOLUTE 
# logrotate.conf setup
sed -i 's/rotate 4/rotate 20 \n\nolddir \/var\/log\/logrotate-target/' /etc/logrotate.conf
sed -i "s/^#compress/compress/" /etc/logrotate.conf
# setup git
setupHttpdGitLocal.sh "httpdConf@sapsailing.com:repo.git" disposable "Disposable Reverse Proxy"
# Final enabling and starting of services.
systemctl start httpd
sudo systemctl start crond.service
systemctl enable imageupgrade.service
SECONDEOF
