#!/bin/bash
# Apply this to an instance launched from a barebones Debian 12 image with ~16GB of root volume size.
# As a result, you'll get a ready-to-use RabbitMQ server that is running on the default port and accepts
# connections with the guest user log-in also from non-localhost addresses.
if [ $# != 0 ]; then
  SERVER=$1
  scp -o StrictHostKeyChecking=false "${0}" admin@${SERVER}:
  ssh -o StrictHostKeyChecking=false -A admin@${SERVER} ./$( basename "${0}" )
else
  # Fix the non-sensical use of "dash" as the default shell:
  sudo rm /usr/bin/sh
  sudo ln -s /usr/bin/bash /usr/bin/sh
  scp -o StrictHostKeyChecking=false root@sapsailing.com:ssh-key-reader.token /home/admin
  sudo chown admin /home/admin/ssh-key-reader.token
  sudo chgrp admin /home/admin/ssh-key-reader.token
  sudo chmod 600 /home/admin/ssh-key-reader.token
  # Install packages for MariaDB and cron/anacron/crontab:
  sudo apt-get -y update
  sudo DEBIAN_FRONTEND=noninteractive apt-get -yq -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confnew upgrade
  # Note that the fail2ban installation on Debian automatically configures a jail for sshd based on auth.log;
  # therefore, the setup_fail2ban function from imageupgrade_functions.sh does not need to be invoked in this case.
  sudo DEBIAN_FRONTEND=noninteractive apt-get -yq -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confnew install rabbitmq-server systemd-cron jq syslog-ng fail2ban
  sudo touch /var/run/last_change_aws_landscape_managers_ssh_keys__home_admin
  sudo chown admin:admin /var/run/last_change_aws_landscape_managers_ssh_keys__home_admin
  scp -o StrictHostKeyChecking=false -r root@sapsailing.com:/home/wiki/gitwiki/configuration/environments_scripts/repo/usr/local/bin/imageupgrade_functions.sh /home/admin
  sudo mv imageupgrade_functions.sh /usr/local/bin
  . imageupgrade_functions.sh
  if ! build_crontab_and_setup_files 'rabbitmq_instance_setup'; then
    exit 1
  fi
  setup_sshd_resilience
  sudo chown root:root /usr/local/bin/imageupgrade_functions.sh
  # Wait for RabbitMQ to become available; note that install under apt also means start...
  sleep 10
  sudo rabbitmq-plugins enable rabbitmq_management
  # Allow guest login from non-localhost IPs:
  sudo su - -c "cat <<EOF >>/etc/rabbitmq/rabbitmq.conf
loopback_users = none
EOF
"
  sudo systemctl restart rabbitmq-server.service
fi
